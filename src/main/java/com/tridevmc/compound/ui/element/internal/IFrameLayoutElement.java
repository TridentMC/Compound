package com.tridevmc.compound.ui.element.internal;

import com.tridevmc.compound.ui.geometry.api.FrameGeometry;
import com.tridevmc.compound.ui.layout.Bounds;

import javax.annotation.Nonnull;

/** Internal layout for an overlay whose allocation depends on this frame's parent transform. */
public interface IFrameLayoutElement {
    /**
     * Updates geometry-dependent layout properties and returns the local allocation. The tree
     * measures and places this subtree before sampling its geometry, after sampling its parent.
     */
    @Nonnull
    Bounds prepareFrameLayout(@Nonnull FrameGeometry parent);
}
