package com.tridevmc.compound.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to define a field as referencing a value in a Compound config file.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigValue {

    /**
     * Defines the name of the field in the configuration file, defaults to the field name if not set.
     *
     * @return the name of the field in the configuration file.
     */
    String name() default "";

    /**
     * Defines the comment associated with the field in the configuration file.
     *
     * @return the comment associated with the field in the configuration file.
     */
    String comment() default "";

    /**
     * Defines the language key associated with the field for use in the config gui.
     * <p>
     * Defaults to "modid.compoundconfig.gui.property.{field}" if no value is set.
     *
     * @return the language key to use in the config gui.
     */
    String langKey() default "";

    /**
     * Defines the category of the field in the configuration file, defaults to "general" if not set.
     *
     * @return the category associated with the field in the configuration file.
     */
    String category() default "";

    /**
     * Used to determine whether any changes to this field requires the game to restart.
     *
     * @return true if changes to this field require the game to restart, false otherwise.
     */
    boolean requiresGameRestart() default false;

    /**
     * Used to determine whether any changes to this field requires the world to restart.
     *
     * @return true if changes to this field require the world to restart, false otherwise.
     */
    boolean requiresWorldRestart() default false;

    /**
     * Used to determine whether this field should be visible in the config gui.
     *
     * @return true if the field should be visible in the config gui, false otherwise.
     */
    boolean visibleInGui() default true;

}
