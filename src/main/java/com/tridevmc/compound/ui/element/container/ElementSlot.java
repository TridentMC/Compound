/*
 * Copyright 2018 - 2021 TridentMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridevmc.compound.ui.element.container;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tridevmc.compound.ui.ICompoundUI;
import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.UVData;
import com.tridevmc.compound.ui.element.Element;
import com.tridevmc.compound.ui.layout.ILayout;
import com.tridevmc.compound.ui.layout.LayoutNone;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Objects;


/**
 * A resizable slot element to add to Container UIs, must be used in conjunction with CompoundUIContainer and CompoundContainer
 */
public class ElementSlot extends Element {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/inventory.png");
    private Slot slot;
    private boolean drawOverlay;
    private boolean drawUnderlay;
    private ItemStack displayStack;
    private String displayString;

    public ElementSlot(@Nonnull Rect2F dimensions, @Nonnull Slot slot) {
        this(dimensions, new LayoutNone(), slot);
    }

    public ElementSlot(@Nonnull Rect2F dimensions, @Nonnull ILayout layout, @Nonnull Slot slot) {
        super(dimensions, layout);
        this.slot = slot;
    }

    @Override
    public void drawBackground(ICompoundUI ui) {
        IScreenContext screen = ui.getScreenContext();
        Rect2F rect = this.getTransformedDimensions(screen);
        screen.bindTexture(TEXTURE);
        screen.drawTexturedRect(rect, new UVData(8, 8), new UVData(8, 8));
        screen.drawTexturedRect(new Rect2F(rect.getX(), rect.getY(), rect.getWidth() - 1, 1),
                new UVData(7, 7), new UVData(7, 7));
        screen.drawTexturedRect(new Rect2F(rect.getX(), rect.getY(), 1, rect.getHeight() - 1),
                new UVData(7, 7), new UVData(7, 7));
        screen.drawTexturedRect(new Rect2F(rect.getX() + rect.getWidth() - 1, rect.getY() + 1, 1, rect.getHeight() - 1),
                new UVData(24, 8), new UVData(24, 8));
        screen.drawTexturedRect(new Rect2F(rect.getX() + 1, rect.getY() + rect.getHeight() - 1, rect.getWidth() - 1, 1),
                new UVData(24, 8), new UVData(24, 8));
    }

    @Override
    public void drawForeground(ICompoundUI ui) {
        if (this.drawUnderlay) {
            this.drawHighlight(ui.getScreenContext());
        }

        Rect2F rect = this.getTransformedDimensions(ui.getScreenContext());
        rect = new Rect2F(rect.getX() + 1, rect.getY() + 1, rect.getWidth() - 2, rect.getHeight() - 2);
        ui.getScreenContext().drawItemStack(this.displayStack, rect, this.displayString, 100);

        if (this.drawOverlay) {
            this.drawHighlight(ui.getScreenContext());
        }
    }

    @Override
    public void drawOverlay(ICompoundUI ui) {
        var screenContext = ui.getScreenContext();
        if (this.drawOverlay && Objects.requireNonNull(screenContext.getMc().player).containerMenu.getCarried().isEmpty() && this.slot.hasItem()) {
            this.drawTooltip(ui.getScreenContext());
        }

        this.reset();
    }

    private void drawHighlight(IScreenContext screen) {
        Rect2F highlightArea = this.getTransformedDimensions(screen).offset(new Rect2F(1, 1, -1, -1));
        RenderSystem.disableDepthTest();
        RenderSystem.colorMask(true, true, true, false);
        int slotColor = -2130706433;
        screen.drawRect(highlightArea, slotColor);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
    }

    private void drawTooltip(IScreenContext screen) {
        screen.drawTooltip(screen.getActiveStack(), this.displayStack, (int) screen.getMouseX(), (int) screen.getMouseY());
    }

    private void reset() {
        this.drawOverlay = false;
        this.drawUnderlay = false;
        this.displayStack = this.slot.getItem();
        this.displayString = null;
    }

    public boolean isMouseOverSlot(IScreenContext screen) {
        Rect2F rect = this.getTransformedDimensions(screen).offsetSize(-1, -1);
        return rect.isPointInRect(screen.getMouseX(), screen.getMouseY());
    }

    public void setDrawOverlay(boolean drawOverlay) {
        this.drawOverlay = drawOverlay;
    }

    public void setDisplayStack(ItemStack displayStack) {
        this.displayStack = displayStack;
    }

    public void setDisplayString(String displayString) {
        this.displayString = displayString;
    }

    public void setDrawUnderlay(boolean drawUnderlay) {
        this.drawUnderlay = drawUnderlay;
    }
}
