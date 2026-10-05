package com.tridevmc.compound.ui.text.api;

import com.tridevmc.compound.ui.layout.Bounds;

public record TextInsets(int left, int top, int right, int bottom) {
    public static final TextInsets NONE = new TextInsets(0, 0, 0, 0);
    public TextInsets {
        if (left < 0 || top < 0 || right < 0 || bottom < 0) {
            throw new IllegalArgumentException("Text insets must be nonnegative");
        }
    }
    public Bounds apply(Bounds bounds) {
        return new Bounds(bounds.x() + left, bounds.y() + top,
                Math.max(0, bounds.width() - left - right),
                Math.max(0, bounds.height() - top - bottom));
    }
}
