package com.tridevmc.compound.ui;

/**
 * Defines the layers drawn by a compound UI.
 */
public enum EnumUILayer {
    BACKGROUND("background"),
    FOREGROUND("foreground"),
    OVERLAY("overlay");

    final String name;

    EnumUILayer(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
