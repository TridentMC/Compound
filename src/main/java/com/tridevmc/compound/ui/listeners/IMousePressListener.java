package com.tridevmc.compound.ui.listeners;

import com.tridevmc.compound.ui.screen.IScreenContext;

@FunctionalInterface
public interface IMousePressListener {

    /**
     * Called when a mouse button is pressed.
     *
     * @param screen    the screen context where the event took place.
     * @param x      the current x position of the mouse cursor.
     * @param y      the current y position of the mouse cursor.
     * @param button the mouse button that was pressed.
     */
    void listen(IScreenContext screen, double x, double y, int button);

}
