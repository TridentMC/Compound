package com.tridevmc.compound.gui.widget;

import com.tridevmc.compound.gui.CompoundGui;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class WidgetBase {

    protected int top, left, width, height;
    protected CompoundGui parent;

    public final int getTop() {
        return this.top;
    }

    public final int getLeft() {
        return this.left;
    }

    public final void setPosition(int top, int left) {
        this.top = top;
        this.left = left;
    }

    public final int getWidth() {
        return this.width;
    }

    public final void setWidth(int width) {
        this.width = width;
    }

    public final int getHeight() {
        return this.height;
    }

    public final void setHeight(int height) {
        this.height = height;
    }

    public void onRegister(CompoundGui gui) {
        this.parent = gui;
    }

    public void drawBackground(int mouseX, int mouseY) {
    }

    public void drawForeground(int mouseX, int mouseY) {

    }
}
