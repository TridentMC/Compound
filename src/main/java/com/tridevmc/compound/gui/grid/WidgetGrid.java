package com.tridevmc.compound.gui.grid;

import com.tridevmc.compound.gui.MouseState;
import com.tridevmc.compound.gui.widget.WidgetBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHelper;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Tuple;

import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class WidgetGrid extends WidgetBase {

    protected final NonNullList<WidgetBase> widgets;
    protected final NonNullList<Tuple<Predicate<MouseState>, Consumer<MouseState>>> mousePressedCallbacks;

    public WidgetGrid(int top, int left, int width, int height) {
        this.widgets = NonNullList.create();
        this.mousePressedCallbacks = NonNullList.create();
    }

    public void registerWidget(WidgetBase widget, int top, int left) {
        widget.setPosition(top, left);
        widget.onRegister(this.parent);
        this.widgets.add(widget);
    }

    public void registerMousePressedCallback(Predicate<MouseState> predicate,
                                             Consumer<MouseState> callback) {
        this.mousePressedCallbacks.add(new Tuple<>(predicate, callback));
    }

    @Override
    public void drawBackground(int mouseX, int mouseY) {
        for (WidgetBase widget : this.widgets) {
            widget.drawBackground(mouseX, mouseY);
        }
    }

    @Override
    public void drawForeground(int mouseX, int mouseY) {
        for (WidgetBase widget : this.widgets) {
            widget.drawForeground(mouseX, mouseY);
        }
    }

    public void handleMouseInput() {
        Minecraft mc = Minecraft.getInstance();
        MouseHelper mouseHelper = mc.mouseHelper;
        int normalizedX = (int) (mouseHelper.getMouseX() * this.getWidth() / Minecraft.getInstance().mainWindow.getWidth());
        int normalizedY = (int) (mouseHelper.getMouseY() * this.getHeight() / Minecraft.getInstance().mainWindow.getHeight());
        // TODO: this isnt a proper fix, we should probably setup a mouse callback with LWJGL instead.
        int buttonState = mouseHelper.isLeftDown() ? 0 : mouseHelper.isRightDown() ? 1 : -1;
        MouseState state = new MouseState(normalizedX, normalizedY, buttonState);

        for (Tuple<Predicate<MouseState>, Consumer<MouseState>> t : this.mousePressedCallbacks) {
            if (t.getA().test(state)) {
                t.getB().accept(state);
                return;
            }
        }
    }
}
