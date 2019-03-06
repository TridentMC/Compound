package com.tridevmc.compound.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tridevmc.compound.core.reflect.WrappedField;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import net.minecraftforge.forgespi.language.ModFileScanData;
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
    private final ForgeConfigSpec forgeConfig;
    private final Set<IConfigObjectSerializer> objectSerializers;

    private final Class<T> configClass;
    private final T configInstance;

    private final ArrayList<ConfigField> fields = Lists.newArrayList();

    private CompoundConfig(@Nonnull Class<T> type, String modId, String configFile) throws IllegalAccessException, InstantiationException {
        this.modId = modId;
        this.configClass = type;
        this.configInstance = type.newInstance();
        this.configType = this.genConfigType();
        this.objectSerializers = Sets.newHashSet();
        this.objectSerializers.add(new ForgeRegistryEntrySerializer());

        Pair<Object, ForgeConfigSpec> configure = new ForgeConfigSpec.Builder().configure(this::loadConfig);
        this.forgeConfig = configure.getRight();

        ModContainer modContainer = ModLoadingContext.get().getActiveContainer();
        if (modContainer instanceof FMLModContainer) {
            ((FMLModContainer) modContainer).getEventBus().register(this);
        }
        this.modConfig = new CompoundModConfig(this, modContainer, configFile);
        // TODO: Add config guis...
        //modContainer.registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, parent) -> {});
        KNOWN_CONFIGS.put(this.configInstance, this);
    }

    /**
     * Create a configuration object managed by a Compound config.
     *
     * @param type  the type of class the Compound config will generate.
     * @param modId the id of the mod the configuration is from.
     * @param <C>   the class of the configuration object the Compound config will manage.
     * @return the configuration object that will be managed by the Compound config.
     */
    @Nullable
    public static <C> C of(@Nonnull Class<C> type, @Nonnull String modId) {
        return CompoundConfig.of(type, modId, modId + ".toml");
    }

    /**
     * Create a configuration object managed by a Compound config.
     *
     * @param type  the type of class the Compound config will generate.
     * @param modId the id of the mod the configuration is from.
     * @param <C>   the class of the configuration object the Compound config will manage.
     * @return the configuration object that will be managed by the Compound config.
     */
    @Nullable
    public static <C> C of(@Nonnull Class<C> type, @Nonnull String modId, @Nonnull String fileName) {
        CompoundConfig<C> cCompoundConfig = null;
        try {
            cCompoundConfig = new CompoundConfig<C>(type, modId, fileName);
        } catch (Exception e) {
            LOG.error("Failed to create compound config of type {}", type.getName());
            e.printStackTrace();
        }

        return cCompoundConfig == null ? null : cCompoundConfig.configInstance;
    }

    private ModConfig.Type genConfigType() {
        if (configClass.isAnnotationPresent(ConfigType.class)) {
            ConfigType annotation = configClass.getAnnotation(ConfigType.class);
            return annotation.value();
        } else {
            return ModConfig.Type.COMMON;
        }
    }

    private CompoundConfig loadConfig(ForgeConfigSpec.Builder builder) {
        if (this.objectSerializers.isEmpty()) {
            List<ModFileScanData> modScanData = ModList.get().getAllScanData();
            ArrayList<ModFileScanData.AnnotationData> annotationData = Lists.newArrayList();
            String annotationName = RegisteredConfigObjectSerializer.class.getName();

            modScanData.forEach((m) -> m.getAnnotations().stream().filter(a -> Objects.equals(a.getAnnotationType().getClassName(), annotationName)).forEach(a -> {
                Map<String, Object> annotationInfo = a.getAnnotationData();
                String modId = (String) annotationInfo.get("value");
                if (Objects.equals(modId, this.getModId())) {
                    annotationData.add(a);
                }
            }));

            this.objectSerializers.addAll(annotationData.stream().map(data -> {
                try {
                    return (IConfigObjectSerializer) Class.forName(data.getClassType().getClassName()).newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException(String.format(
                            "Failed to instantiate %s, is there an empty constructor?",
                            data.getMemberName()),
                            e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(String.format(
                            "Failed to instantiate %s, is there a public empty constructor?",
                            data.getMemberName()),
                            e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(String.format(
                            "Unable to find class: \"%s\" for registered marshaller.",
                            data.getMemberName()),
                            e);
                }
            }).filter(Objects::nonNull).collect(Collectors.toSet()));
        }

        if (this.fields.isEmpty()) {
            // Load any fields if we haven't already.
            for (Field field : configClass.getDeclaredFields()) {
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
    public void onConfigReloading(ModConfig.ConfigReloading e) {
        if (Objects.equals(e.getConfig().getModId(), this.modId)) {
            this.loadFields();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onConfigLoading(ModConfig.Loading e) {
        if (Objects.equals(e.getConfig().getModId(), this.modId)) {
            this.loadFields();
        }
    }

    protected void loadFields() {
        this.fields.forEach(ConfigField::loadField);
    }

    protected String getModId() {
        return modId;
    }

    protected ForgeConfigSpec getForgeConfig() {
        return forgeConfig;
    }

    protected Class<T> getConfigClass() {
        return configClass;
    }

    protected T getConfigInstance() {
        return configInstance;
    }

    protected ModConfig.Type getConfigType() {
        return configType;
    }

    @Nullable
    protected IConfigObjectSerializer getSerializerFor(Class fieldType) {
        Optional<IConfigObjectSerializer> serializer = this.objectSerializers.stream()
                .filter((s) -> s.accepts(fieldType))
                .findFirst();
        return serializer.orElse(null);
    }
}
