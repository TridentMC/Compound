package com.tridevmc.compound.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigValue {

    String name() default "";

    String comment() default "";

    String langKey() default "";

    String category() default "";

    boolean requiresGameRestart() default false;

    boolean requiresWorldRestart() default false;

    boolean visibleInGui() default true;

}
