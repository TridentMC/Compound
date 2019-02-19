package com.tridevmc.compound.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to define a field as referencing a type in a Compound config file.
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
     * Defaults to "modid.compoundconfig.gui.property.{field}" if no type is set.
     *
     * @return the language key to use in the config gui.
     */
    String langKey() default "";

    /**
     * Used to determine whether any changes to this field requires the world to restart.
     *
     * @return true if changes to this field require the world to restart, false otherwise.
     */
    boolean requiresWorldRestart() default false;


}
