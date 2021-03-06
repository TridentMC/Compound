package com.tridevmc.compound.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;

/**
 * Only for internal use, exposes methods and variables to screen context.
 */
public interface IInternalCompoundUI {
    int getBlitOffset();

    void setBlitOffset(int blitOffset);

    int getWidth();

    int getHeight();

    float getMouseX();

    float getMouseY();

    Minecraft getMc();

    long getTicks();

    Screen asGuiScreen();

    void drawTextComponent(ITextComponent component, int x, int y);

    EnumUILayer getCurrentLayer();

}
