package com.tridevmc.compound.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.Style;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Only for internal use, exposes methods and variables to screen context.
 */
public interface IInternalCompoundUI {

    MatrixStack getActiveStack();

    int getBlitOffset();

    void setBlitOffset(int blitOffset);

    int getWidth();

    int getHeight();

    float getMouseX();

    float getMouseY();

    Minecraft getMc();

    long getTicks();

    Screen asGuiScreen();

    EnumUILayer getCurrentLayer();

    void renderTooltip(MatrixStack stack, List<? extends ITextProperties> lines, int x, int y, FontRenderer font);

}
