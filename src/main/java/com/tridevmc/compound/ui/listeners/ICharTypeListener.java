package com.tridevmc.compound.ui.listeners;

import com.tridevmc.compound.ui.screen.IScreenContext;

@FunctionalInterface
public interface ICharTypeListener {

    /**
     * Called when a character is typed.
     *
     * @param screen    the screen context where the event took place.
     * @param typedChar the character that was typed.
     * @param modifiers any modifiers that were applied to the key.
     */
    void listen(IScreenContext screen, char typedChar, int modifiers);

}
