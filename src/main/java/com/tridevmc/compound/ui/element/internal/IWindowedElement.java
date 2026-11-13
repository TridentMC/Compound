package com.tridevmc.compound.ui.element.internal;

import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.layout.Bounds;

/** Internal queued source and viewport preparation phases. */
public interface IWindowedElement extends IElement {
    boolean reconcileSource();
    boolean prepareWindow(Bounds viewport);
}
