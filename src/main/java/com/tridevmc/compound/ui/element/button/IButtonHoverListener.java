package com.tridevmc.compound.ui.element.button;

@FunctionalInterface
public interface IButtonHoverListener {
    /**
     * Called when the mouse cursor enters or exits the button area.
     *
     * @param x       the current x position of the mouse cursor.
     * @param y       the current y position of the mouse cursor.
     * @param entered whether the cursor entered or exited the button area. True if entered, false if exited.
     */
    void onButtonHover(double x, double y, boolean entered);
}
