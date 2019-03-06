package com.tridevmc.compound.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisteredConfigObjectSerializer {
    /**
     * The mod id of the config that this object serializer should be registered to.
     *
     * @return the mod id associated with the config that this serializer should be registered to.
     */
    String value();
}
