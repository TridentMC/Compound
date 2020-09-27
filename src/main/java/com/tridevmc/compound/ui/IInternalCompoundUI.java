package com.tridevmc.compound.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

/**
 * Only for internal use, exposes methods and variables to screen context.
 */
public interface IInternalCompoundUI {

    MatrixStack getActiveStack();

    int getBlitOffset();

    void setBlitOffset(int blitOffset);

    int getWidth();

    int getHeight();

    double getMouseX();

    double getMouseY();

    Minecraft getMc();

    long getTicks();

    Screen asGuiScreen();

    EnumUILayer getCurrentLayer();

}
