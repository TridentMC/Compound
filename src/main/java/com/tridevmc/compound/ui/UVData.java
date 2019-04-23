package com.tridevmc.compound.ui;

import com.google.common.base.MoreObjects;

public class UVData {

    private final float u, v;

    public UVData(float u, float v) {
        this.u = u;
        this.v = v;
    }

    public float getU() {
        return this.u;
    }

    public float getV() {
        return this.v;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("u", this.u)
                .add("v", this.v)
                .toString();
    }
}
