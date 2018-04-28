package com.tridevmc.compound.network.marshallers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to specify a marshaller for a given field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface SetMarshaller {

    /**
     * Used to determine the marshaller to be used for the annotated field.
     *
     * @return the id of the marshaller to use.
     */
    String marshallerId();
}
