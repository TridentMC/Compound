package com.tridevmc.compound.ui.listeners;

@FunctionalInterface
public interface IMousePressListener {

    /**
     * Called when a mouse button is pressed.
     *
     * @param x      the current x position of the mouse cursor.
     * @param y      the current y position of the mouse cursor.
     * @param button the mouse button that was pressed.
     */
    void listen(double x, double y, int button);

}
