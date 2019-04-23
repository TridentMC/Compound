package com.tridevmc.compound.ui.listeners;

@FunctionalInterface
public interface IMouseScrollListener {
    /**
     * Called when the scroll wheel is moved.
     *
     * @param distance the distance that the scroll wheel moved.
     */
    void listen(double distance);
}
