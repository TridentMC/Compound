package com.tridevmc.compound.config;

import net.minecraftforge.fml.config.ModConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on config classes to determine what type of configuration they are.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigType {

    /**
     * Determines what type of configuration the annotated class is.
     *
     * @return the type of configuration.
     */
    ModConfig.Type value() default ModConfig.Type.COMMON;

}
