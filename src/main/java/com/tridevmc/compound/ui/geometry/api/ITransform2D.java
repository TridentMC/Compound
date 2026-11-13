package com.tridevmc.compound.ui.geometry.api;

import com.tridevmc.compound.ui.geometry.internal.Transform2D;

/** Visual-only transform: scale, then rotate around a local pixel pivot, then translate. */
public interface ITransform2D {
    ITransform2D IDENTITY = of(0, 0, 0, 1, 1, 0, 0);

    static ITransform2D translation(double x, double y) {
        return of(x, y, 0, 1, 1, 0, 0);
    }

    static ITransform2D rotation(double radians, double pivotX, double pivotY) {
        return of(0, 0, radians, 1, 1, pivotX, pivotY);
    }

    static ITransform2D of(double x, double y, double radians, double scaleX,
            double scaleY, double pivotX, double pivotY) {
        return new Transform2D(x, y, radians, scaleX, scaleY, pivotX, pivotY);
    }

    double x();
    double y();
    double radians();
    double scaleX();
    double scaleY();
    double pivotX();
    double pivotY();
}
