package com.tridevmc.compound.gui;

import com.tridevmc.compound.gui.grid.WidgetGrid;
import com.tridevmc.compound.gui.grid.WidgetGridNormal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class CompoundGui extends GuiScreen {
	public WidgetGrid getGrid() {
		return grid;
	}

	protected final WidgetGrid grid;

	protected FontRenderer fontRenderer;
	public CompoundGui() {
		super();
		this.fontRenderer = Minecraft.getMinecraft().fontRenderer;
		this.grid = new WidgetGridNormal(0,0,this.width,this.height);
		this.grid.onRegister(this);
	}

	public void drawScreen(int mouseX, int mouseY) {
		ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
		this.grid.setWidth(scaledresolution.getScaledWidth());
		this.grid.setHeight(scaledresolution.getScaledHeight());
		if(this.fontRenderer == null) {
			// TODO: Don't do this.
			throw new RuntimeException("Font renderer was null on render call...");
		}
		this.drawBackground(mouseX, mouseY);
		this.drawForeground(mouseX, mouseY);
	}

	protected void drawBackground(int mouseX, int mouseY) {
		this.grid.drawBackground(mouseX, mouseY);
	}

	protected void drawForeground(int mouseX, int mouseY) {
		this.grid.drawForeground(mouseX, mouseY);
	}

	public final FontRenderer getFontRenderer() {
		return this.fontRenderer;
	}

	public final void setFontRenderer(FontRenderer renderer) {
		this.fontRenderer = renderer;
	}

	public void handleMouseInput() {
		this.grid.handleMouseInput();
	}

	public void drawColoredRect(int x, int y, int width, int height, int color) {
		Gui.drawRect(x,y,x+width,y+height,color);
	}

	public void drawBeveledBoxes(int x, int y, int width, int height, int wide, int high) {
		for(int wY = 0;wY < high;wY++) {
			for(int wX = 0;wX < wide;wX++) {
				drawBeveledBox(x + (width*wX), y + (height * wY), width, height);
			}
		}
	}

	public void drawBeveledBox(int x, int y, int width, int height, int background, int topleft, int botright) {
		Gui.drawRect(x,y, x+width, y+height, background);
		Gui.drawRect(x,y, x+(width-1), y+(height-1), topleft);
		Gui.drawRect(x+1, y+1, x+width, y+height, botright);
		Gui.drawRect(x+1,y+1,x+(width-1), y+(height-1), background);
	}

	public void drawBeveledBox(int x, int y, int width, int height) {
		drawBeveledBox(x,y,width,height, 0xFF8b8b8b, 0xFF373737, 0xFFffffff);
	}


}