package com.tridevmc.compound.ui.listeners;

import com.tridevmc.compound.ui.screen.IScreenContext;

@FunctionalInterface
public interface IMouseScrollListener {
    /**
     * Called when the scroll wheel is moved.
     *
     * @param screen   the screen context where the event took place.
     * @param distance the distance that the scroll wheel moved.
     */
    void listen(IScreenContext screen, double distance);
}
