package com.tridevmc.compound.gui.grid;

import com.tridevmc.compound.gui.MouseState;
import com.tridevmc.compound.gui.widget.WidgetBase;
import net.minecraft.client.Minecraft;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Tuple;
import org.lwjgl.input.Mouse;

import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class WidgetGrid extends WidgetBase {
	protected final NonNullList<WidgetBase> widgets;
	protected final NonNullList<Tuple<Predicate<MouseState>,Consumer<MouseState>>> mousePressedCallbacks;

	public WidgetGrid(int top, int left, int width, int height) {
		this.widgets = NonNullList.create();
		this.mousePressedCallbacks = NonNullList.create();
	}

	public void registerWidget(WidgetBase widget, int top, int left) {
		widget.setPosition(top, left);
		widget.onRegister(this.parent);
		widgets.add(widget);
	}

	public void registerMousePressedCallback(Predicate<MouseState> predicate, Consumer<MouseState> callback) {
		this.mousePressedCallbacks.add(new Tuple<>(predicate, callback));
	}

	@Override
	public void drawBackground(int mouseX, int mouseY) {
		for(WidgetBase widget : widgets) {
			widget.drawBackground(mouseX, mouseY);
		}
	}

	@Override
	public void drawForeground(int mouseX, int mouseY) {
		for(WidgetBase widget : widgets) {
			widget.drawForeground(mouseX, mouseY);
		}
	}

	public void handleMouseInput() {
		int normalizedX = Mouse.getEventX() * this.getWidth() / Minecraft.getMinecraft().displayWidth;
		int normalizedY = Mouse.getEventY() * this.getHeight() / Minecraft.getMinecraft().displayHeight;
		int buttonState = Mouse.getEventButton();
		MouseState state = new MouseState(normalizedX, normalizedY, buttonState);

		for(Tuple<Predicate<MouseState>, Consumer<MouseState>> t : mousePressedCallbacks) {
			if(t.getFirst().test(state)) {
				t.getSecond().accept(state);
				return;
			}
		}
	}
}
