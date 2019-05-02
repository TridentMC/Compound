package com.tridevmc.compound.ui.listeners;

@FunctionalInterface
public interface IMouseDraggedListener {

    /**
     * Called when the mouse cursor was dragged while holding a button.
     *
     * @param x      the current x position of the mouse cursor.
     * @param y      the current y position of the mouse cursor.
     * @param button the mouse button being pressed.
     * @param pX     the previous x position of the mouse cursor.
     * @param pY     the previous y position of the mouse cursor.
     */
    void listen(double x, double y, int button, double pX, double pY);

}
