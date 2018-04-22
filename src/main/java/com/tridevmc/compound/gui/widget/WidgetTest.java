package com.tridevmc.compound.gui.widget;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

public class WidgetTest extends WidgetBase {

	int current = 0;
	int currentTimer = 20;
	@Override
	public void drawForeground(int mouseX, int mouseY) {
//		this.parent.drawBeveledBoxes(24,24,18,18, 9, 4);

		for(int i =0;i < 4;i++) {
			for(int j = 0;j < 9;j++) {
				this.parent.drawBeveledBox(24 + j*(18), 24 + (i*18), 18, 18);
				if((i*9)+j == current) {
					//GlStateManager.disableLighting();
					//GlStateManager.disableDepth();
					//GlStateManager.colorMask(true, true, true, false);
					Gui.drawRect(24 + j*(18) + 1, 24 + i*(18), 24 + j*(18) + 17, 24 + i*(18) + 17, -2130706433);
					//GlStateManager.colorMask(true, true, true, true);
					//GlStateManager.enableLighting();
					//GlStateManager.enableDepth();
					currentTimer--;
					if(currentTimer == 0) {
						currentTimer = 5;

						if (++current == 4 * 9)
							current = 0;
					}
				}
			}
		}
	}
}
