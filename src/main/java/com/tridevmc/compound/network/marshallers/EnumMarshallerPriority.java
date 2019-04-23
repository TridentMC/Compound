package com.tridevmc.compound.network.marshallers;

/**
 * An enum to specify the priority for a marshaller to be used, HIGH means it will be more likely to
 * be used for a field, LOW means it is less likely to be used for a field.
 */
public enum EnumMarshallerPriority {
    HIGH(1),
    NORMAL(0),
    LOW(-1);

    int rank = 0;

    EnumMarshallerPriority(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return this.rank;
    }
}
