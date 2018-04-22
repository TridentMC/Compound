package com.tridevmc.compound.gui;

public class MouseState {
	public final int x, y;
	public final int mouseButton;

	public MouseState(int x, int y) {
		this.x = x;
		this.y = y;
		this.mouseButton = -1;
	}

	public MouseState(int x, int y, int mouseButton) {
		this.x = x;
		this.y = y;
		this.mouseButton = mouseButton;
	}
}
