/*
 * Copyright 2018 - 2022 TridentMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridevmc.compound.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tridevmc.compound.core.reflect.WrappedField;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Used for the management and creation of Compound configs, create and load a configuration using {@link #of(Class, String)}
 *
 * @param <T> the class of the configuration object.
 */
public class CompoundConfig<T> {

    static final Map<Object, CompoundConfig> KNOWN_CONFIGS = Maps.newHashMap();
    private static final Logger LOG = LogManager.getLogger("CompoundConfig");
    private final CompoundModConfig modConfig;
    private final String modId;
    private final ModConfig.Type configType;
    private final ModConfigSpec forgeConfig;
    private final Set<IConfigFieldSerializer> objectSerializers;

    private final Class<T> configClass;
    private final T configInstance;

    private final ArrayList<ConfigField> fields = Lists.newArrayList();

    private CompoundConfig(@Nonnull Class<T> type, ModContainer container, String configFile) throws IllegalAccessException, InstantiationException {
        this.modId = container.getModId();
        this.configClass = type;
        this.configInstance = type.newInstance();
        this.configType = this.genConfigType();
        this.objectSerializers = Sets.newHashSet();
        this.objectSerializers.addAll(Arrays.stream(InternalRegistryEntrySerializer.DEFAULT_SERIALIZERS).toList());
        this.objectSerializers.add(new ForgeRegistryEntrySerializer());

        Pair<Object, ModConfigSpec> configure = new ModConfigSpec.Builder().configure(this::loadConfig);
        this.forgeConfig = configure.getRight();

        if (container instanceof FMLModContainer) {
            ((FMLModContainer) container).getEventBus().register(this);
        }
        this.modConfig = new CompoundModConfig(this, container, configFile);
        // TODO: Add config guis...
        //modContainer.registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, parent) -> {});
        KNOWN_CONFIGS.put(this.configInstance, this);
    }

    /**
     * Create a configuration object managed by a Compound config.
     *
     * @param type      the type of class the Compound config will generate.
     * @param container the container of the mod this config belongs to.
     * @param <C>       the class of the configuration object the Compound config will manage.
     * @return the configuration object that will be managed by the Compound config.
     */
    @Nullable
    public static <C> C of(@Nonnull Class<C> type, @Nonnull ModContainer container) {
        return CompoundConfig.of(type, container, container.getModId() + ".toml");
    }

    /**
     * Create a configuration object managed by a Compound config.
     *
     * @param type      the type of class the Compound config will generate.
     * @param container the container of the mod this config belongs to.
     * @param <C>       the class of the configuration object the Compound config will manage.
     * @return the configuration object that will be managed by the Compound config.
     */
    @Nullable
    public static <C> C of(@Nonnull Class<C> type, @Nonnull ModContainer container, @Nonnull String fileName) {
        CompoundConfig<C> cCompoundConfig = null;
        try {
            cCompoundConfig = new CompoundConfig<C>(type, container, fileName);
        } catch (Exception e) {
            LOG.error("Failed to create compound config of type {}", type.getName());
            e.printStackTrace();
        }

        return cCompoundConfig == null ? null : cCompoundConfig.configInstance;
    }

    private ModConfig.Type genConfigType() {
        if (this.configClass.isAnnotationPresent(ConfigType.class)) {
            ConfigType annotation = this.configClass.getAnnotation(ConfigType.class);
            return annotation.value();
        } else {
            return ModConfig.Type.COMMON;
        }
    }

    private CompoundConfig loadConfig(ModConfigSpec.Builder builder) {
        if (this.objectSerializers.isEmpty()) {
            List<ModFileScanData> modScanData = ModList.get().getAllScanData();
            ArrayList<ModFileScanData.AnnotationData> annotationData = Lists.newArrayList();
            String annotationName = RegisteredConfigObjectSerializer.class.getName();

            modScanData.forEach((m) -> m.getAnnotations().stream().filter(a -> Objects.equals(a.annotationType().getClassName(), annotationName)).forEach(a -> {
                Map<String, Object> annotationInfo = a.annotationData();
                String modId = (String) annotationInfo.get("value");
                if (Objects.equals(modId, this.getModId())) {
                    annotationData.add(a);
                }
            }));

            this.objectSerializers.addAll(annotationData.stream().map(data -> {
                try {
                    return (IConfigObjectSerializer) Class.forName(data.targetType().name()).newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException(String.format(
                            "Failed to instantiate %s, is there an empty constructor?",
                            data.memberName()),
                            e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(String.format(
                            "Failed to instantiate %s, is there a public empty constructor?",
                            data.memberName()),
                            e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(String.format(
                            "Unable to find class: \"%s\" for registered marshaller.",
                            data.memberName()),
                            e);
                }
            }).filter(Objects::nonNull).collect(Collectors.toSet()));
        }

        if (this.fields.isEmpty()) {
            // Load any fields if we haven't already.
            for (Field field : this.configClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(ConfigValue.class)) {
                    WrappedField wrappedField = WrappedField.create(field);
                    wrappedField.setAccessible(true);
                    try {
                        ConfigField configField = new ConfigField(this, wrappedField);
                        this.fields.add(configField);
                    } catch (IllegalArgumentException e) {
                        LOG.error("Failed to create ConfigField for field {}, caused by {}", wrappedField.getName(), e);
                    }
                }
            }
        }

        this.fields.forEach((f) -> f.addToSpec(builder));
        return this;
    }

    // We need this to run first so config implementations can load their data from the injected changes.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onConfigReloading(ModConfigEvent.Reloading e) {
        if (Objects.equals(e.getConfig(), this.modConfig)) {
            this.loadFields();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onConfigLoading(ModConfigEvent.Loading e) {
        if (Objects.equals(e.getConfig(), this.modConfig)) {
            this.loadFields();
        }
    }

    protected void loadFields() {
        this.fields.forEach(ConfigField::loadField);
    }

    protected String getModId() {
        return this.modId;
    }

    protected ModConfigSpec getForgeConfig() {
        return this.forgeConfig;
    }

    protected Class<T> getConfigClass() {
        return this.configClass;
    }

    protected T getConfigInstance() {
        return this.configInstance;
    }

    protected ModConfig.Type getConfigType() {
        return this.configType;
    }

    @Nullable
    protected <F> IConfigFieldSerializer<F> getSerializerFor(ConfigField<F> fieldType) {
        Optional<IConfigFieldSerializer> serializer = this.objectSerializers.stream()
                .filter((s) -> s.accepts(fieldType))
                .findFirst();
        return serializer.orElse(null);
    }

}
