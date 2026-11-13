package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.geometry.api.FrameGeometry;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Size;

record PopupPlacement(Bounds viewport, Bounds popup) {
    static PopupPlacement of(FrameGeometry anchor, Size viewport, int wantedHeight) {
        if (Math.abs(anchor.b()) > 1e-9 || Math.abs(anchor.c()) > 1e-9) {
            throw new UnsupportedOperationException(
                    "Rotated popup clipping requires a stencil backend; scissors are axis-aligned");
        }
        if (Math.abs(anchor.a()) < 1e-12 || Math.abs(anchor.d()) < 1e-12) {
            var empty = new Bounds(0, 0, 0, 0);
            return new PopupPlacement(empty, empty);
        }
        double scaleY = Math.abs(anchor.d());
        double below = Math.max(0, viewport.height() - anchor.bottom() - 2 * scaleY);
        double above = Math.max(0, anchor.top() - 2 * scaleY);
        double wanted = wantedHeight * scaleY;
        boolean upwards = below < wanted && above > below;
        double height = Math.min(viewport.height(), Math.min(wanted, upwards ? above : below));
        double width = Math.min(viewport.width(), anchor.right() - anchor.left());
        double x = Math.clamp(anchor.left(), 0, viewport.width() - width);
        double y = Math.clamp(upwards ? anchor.top() - height : anchor.bottom(),
                0, viewport.height() - height);
        return new PopupPlacement(toLocal(anchor, 0, 0, viewport.width(), viewport.height(),
                Rounding.OUTWARD), toLocal(anchor, x, y, x + width, y + height, Rounding.INWARD));
    }

    private static Bounds toLocal(FrameGeometry geometry, double left, double top,
            double right, double bottom, Rounding rounding) {
        double x1 = (left - geometry.tx()) / geometry.a();
        double x2 = (right - geometry.tx()) / geometry.a();
        double y1 = (top - geometry.ty()) / geometry.d();
        double y2 = (bottom - geometry.ty()) / geometry.d();
        // Content stays inside the viewport; the invisible dismissal backdrop covers its edges.
        int x = rounding.lower(Math.min(x1, x2));
        int y = rounding.lower(Math.min(y1, y2));
        int maxX = rounding.upper(Math.max(x1, x2));
        int maxY = rounding.upper(Math.max(y1, y2));
        return new Bounds(x, y, Math.max(0, maxX - x), Math.max(0, maxY - y));
    }

    private enum Rounding {
        INWARD {
            @Override int lower(double value) { return (int) Math.ceil(value - 1e-9); }
            @Override int upper(double value) { return (int) Math.floor(value + 1e-9); }
        },
        OUTWARD {
            @Override int lower(double value) { return (int) Math.floor(value + 1e-9); }
            @Override int upper(double value) { return (int) Math.ceil(value - 1e-9); }
        };

        abstract int lower(double value);
        abstract int upper(double value);
    }
}
