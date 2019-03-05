package com.tridevmc.compound.network.marshallers;

/**
 * Stores information about a marshaller, used for registration of default marshallers.
 * <p>
 * For internal use only.
 */
public class MarshallerMetadata {

    public String[] ids;
    public Marshaller marshaller;
    public Class[] acceptedTypes;

    public MarshallerMetadata(String[] ids, Marshaller marshaller, Class[] acceptedTypes) {
        this.ids = ids;
        this.marshaller = marshaller;
        this.acceptedTypes = acceptedTypes;
    }
}
