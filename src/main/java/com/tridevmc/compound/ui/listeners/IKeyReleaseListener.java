package com.tridevmc.compound.ui.listeners;

@FunctionalInterface
public interface IKeyReleaseListener {

    /**
     * Called when a key is released.
     *
     * @param key       the key that was released.
     * @param scanCode  the scan code.
     * @param modifiers the modifiers for the key.
     */
    void listen(int key, int scanCode, int modifiers);

}
