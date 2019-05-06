package com.tridevmc.compound.ui.listeners;

import com.tridevmc.compound.ui.screen.IScreenContext;

@FunctionalInterface
public interface IKeyPressListener {

    /**
     * Called when a key is pressed.
     *
     * @param screen    the screen context where the event took place.
     * @param key       the key that was pressed.
     * @param scanCode  the scan code.
     * @param modifiers the modifiers for the key.
     */
    void listen(IScreenContext screen, int key, int scanCode, int modifiers);

}
