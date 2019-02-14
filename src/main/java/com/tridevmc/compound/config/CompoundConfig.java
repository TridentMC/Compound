package com.tridevmc.compound.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tridevmc.compound.core.reflect.WrappedField;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

/**
 * Used for the management and creation of Compound configs, create and load a configuration using {@link #of(Class, String)}
 *
 * @param <T> the class of the configuration object.
 */
public class CompoundConfig<T> {

    private static final Logger LOG = LogManager.getLogger("CompoundConfig");
    static final Map<Object, CompoundConfig> KNOWN_CONFIGS = Maps.newHashMap();

    private final String modId;
    private final Configuration forgeConfig;

    private final Class<T> configClass;
    private final T configInstance;

    private final ArrayList<ConfigField> fields = Lists.newArrayList();

    private CompoundConfig(@Nonnull Class<T> type, String modId, File configFile, String configVersion) throws IllegalAccessException, InstantiationException {
        this.modId = modId;
        this.forgeConfig = new Configuration(configFile, configVersion);

        this.configClass = type;
        this.configInstance = type.newInstance();

        this.loadConfig();

        KNOWN_CONFIGS.put(this.configInstance, this);
    }

    private void loadConfig() {
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

        this.fields.forEach(ConfigField::loadFromProperty);
        this.forgeConfig.getCategoryNames().forEach(this::updateCategory);
        this.forgeConfig.save();
    }

    private void updateCategory(String categoryName) {
        this.forgeConfig.getCategory(categoryName).setLanguageKey(getModId() + ".compoundconfig.gui.category." + categoryName);
    }

    public String getModId() {
        return modId;
    }

    public Configuration getForgeConfig() {
        return forgeConfig;
    }

    public Class<T> getConfigClass() {
        return configClass;
    }

    public T getConfigInstance() {
        return configInstance;
    }

    @SubscribeEvent
    public void onConfigChange(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(this.getModId())) {
            this.loadConfig();
        }
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
        File configDir = new File(Loader.instance().getConfigDir(), modId);
        return CompoundConfig.of(type, modId, new File(configDir, modId + ".cfg"), null);
    }

    /**
     * Create a configuration object managed by a Compound config.
     *
     * @param type       the type of class the Compound config will generate.
     * @param modId      the id of the mod the configuration is from.
     * @param configName the name of the configuration file to manage.
     * @param <C>        the class of the configuration object the Compound config will manage.
     * @return the configuration object that will be managed by the Compound config.
     */
    @Nullable
    public static <C> C of(@Nonnull Class<C> type, @Nonnull String modId, @Nullable String configName) {
        File configDir = new File(Loader.instance().getConfigDir(), modId);
        return CompoundConfig.of(type, modId, new File(configDir, configName + ".cfg"), "");
    }

    /**
     * Create a configuration object managed by a Compound config.
     *
     * @param type          the type of class the Compound config will generate.
     * @param modId         the id of the mod the configuration is from.
     * @param configName    the name of the configuration file to manage.
     * @param configVersion the version of the configuration file to manage.
     * @param <C>           the class of the configuration object the Compound config will manage.
     * @return the configuration object that will be managed by the Compound config.
     */
    @Nullable
    public static <C> C of(@Nonnull Class<C> type, @Nonnull String modId, @Nonnull String configName, @Nullable String configVersion) {
        File configDir = new File(Loader.instance().getConfigDir(), modId);
        return CompoundConfig.of(type, modId, new File(configDir, configName + ".cfg"), configVersion);
    }

    /**
     * Create a configuration object managed by a Compound config.
     *
     * @param type       the type of class the Compound config will generate.
     * @param modId      the id of the mod the configuration is from.
     * @param configFile the file of the configuration file to manage.
     * @param <C>        the class of the configuration object the Compound config will manage.
     * @return the configuration object that will be managed by the Compound config.
     */
    @Nullable
    public static <C> C of(@Nonnull Class<C> type, @Nonnull String modId, @Nonnull File configFile) {
        return CompoundConfig.of(type, modId, configFile, null);
    }

    /**
     * Create a configuration object managed by a Compound config.
     *
     * @param type          the type of class the Compound config will generate.
     * @param modId         the id of the mod the configuration is from.
     * @param configFile    the file of the configuration file to manage.
     * @param configVersion the version of the configuration file to manage.
     * @param <C>           the class of the configuration object the Compound config will manage.
     * @return the configuration object that will be managed by the Compound config.
     */
    @Nullable
    public static <C> C of(@Nonnull Class<C> type, @Nonnull String modId, @Nonnull File configFile, @Nullable String configVersion) {
        CompoundConfig<C> cCompoundConfig = null;
        try {
            cCompoundConfig = new CompoundConfig<C>(type, modId, configFile, configVersion);
            MinecraftForge.EVENT_BUS.register(cCompoundConfig);
        } catch (Exception e) {
            LOG.error("Failed to create compound config of type {}", type.getName());
            e.printStackTrace();
        }

        return cCompoundConfig == null ? null : cCompoundConfig.configInstance;
    }

}
