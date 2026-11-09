package com.tridevmc.compound.ui.geometry.internal;

import com.tridevmc.compound.ui.geometry.api.ITransform2D;

public record Transform2D(double x, double y, double radians, double scaleX,
        double scaleY, double pivotX, double pivotY) implements ITransform2D {
    public Transform2D {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(radians)
                || !Double.isFinite(scaleX) || !Double.isFinite(scaleY)
                || !Double.isFinite(pivotX) || !Double.isFinite(pivotY)) {
            throw new IllegalArgumentException("Transform components must be finite");
        }
    }
}
