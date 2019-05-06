package com.tridevmc.compound.ui.element.button;

@FunctionalInterface
public interface IButtonPressListener {
    /**
     * Called when a button is pressed.
     *
     * @param x the current x position of the mouse cursor.
     * @param y the current y position of the mouse cursor.
     */
    void onButtonPress(double x, double y);
}
