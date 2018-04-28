package com.tridevmc.compound.network.marshallers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used for marking a marshaller for registration, required for a marshaller to function.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RegisteredMarshaller {

    /**
     * The ids used for identifying the annotated marshaller, used in @SetMarshaller
     *
     * @return the ids of the annotated marshaller class.
     */
    String[] ids();

    /**
     * Used to determine the network channel the annotated marshaller is made to be registered on.
     *
     * @return the name of the network channel to use.
     */
    String networkChannel() default "";

    /**
     * Used to determine the priority at which this marshaller will be selected for a given field.
     *
     * @return The priority for this marshaller to be used.
     */
    EnumMarshallerPriority priority() default EnumMarshallerPriority.NORMAL;

    /**
     * An array of accepted object types that the annotated marshaller can marshall. Used to
     * automatically find a marshaller for a field in a message.
     *
     * @return an array of accepted classes that match the annotated marshaller.
     */
    Class[] acceptedTypes();
}
