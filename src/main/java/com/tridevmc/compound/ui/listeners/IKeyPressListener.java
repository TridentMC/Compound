package com.tridevmc.compound.ui.listeners;

@FunctionalInterface
public interface IKeyPressListener {

    /**
     * Called when a key is pressed.
     *
     * @param key       the key that was pressed.
     * @param scanCode  the scan code.
     * @param modifiers the modifiers for the key.
     */
    void listen(int key, int scanCode, int modifiers);

}
