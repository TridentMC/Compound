/*
 * Copyright 2018 - 2024 TridentMC
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
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.sprite.ScreenSpriteWriterNineSlice;
import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Objects;


/**
 * A resizable slot element to add to Container UIs, must be used in conjunction with CompoundUIContainer and CompoundContainer
 */
public class ElementSlot extends Element {

    private static final IScreenSprite SLOT_SPRITE = IScreenSprite.of(Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI).getSprite(ResourceLocation.withDefaultNamespace("container/slot")), new ScreenSpriteWriterNineSlice(
            1, 1, 1, 1
    ));

    private static final IScreenSprite SLOT_HIGHLIGHT_BACK_SPRITE = IScreenSprite.of(ResourceLocation.withDefaultNamespace("container/slot_highlight_back"));
    private static final IScreenSprite SLOT_HIGHLIGHT_FRONT_SPRITE = IScreenSprite.of(ResourceLocation.withDefaultNamespace("container/slot_highlight_front"));
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
        Rect2F rect = this.getDrawnDimensions(screen);
        screen.drawSprite(SLOT_SPRITE, rect);
    }

    @Override
    public void drawForeground(ICompoundUI ui) {
        if (this.drawUnderlay) {
            this.drawHighlightUnderlay(ui.getScreenContext());
        }

        Rect2F rect = this.getDrawnDimensions(ui.getScreenContext());
        rect = new Rect2F(rect.getX() + 1, rect.getY() + 1, rect.getWidth() - 2, rect.getHeight() - 2);
        ui.getScreenContext().drawItemStack(this.displayStack, rect, this.displayString, 100);

        if (this.drawOverlay) {
            this.drawHighlightOverlay(ui.getScreenContext());
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

    private void drawHighlightOverlay(IScreenContext screen) {
        Rect2F slotRect = this.getDrawnDimensions(screen);
        // Draw front highlight sprite at 24x24 size, centered on the slot (-4 offset)
        // Render at higher z-level to be in front of items
        Rect2F highlightArea = new Rect2F(slotRect.getX() - 3, slotRect.getY() - 3, 24, 24);
        screen.drawSprite(SLOT_HIGHLIGHT_FRONT_SPRITE, highlightArea, 200);
    }

    private void drawHighlightUnderlay(IScreenContext screen) {
        Rect2F slotRect = this.getDrawnDimensions(screen);
        // Draw back highlight sprite at 24x24 size, centered on the slot (-4 offset)
        // Render at lower z-level to be behind items (item renders at 100)
        Rect2F highlightArea = new Rect2F(slotRect.getX() - 3, slotRect.getY() - 3, 24, 24);
        screen.drawSprite(SLOT_HIGHLIGHT_BACK_SPRITE, highlightArea, 50);
    }

    private void drawTooltip(IScreenContext screen) {
        screen.drawTooltip(this.displayStack, (int) screen.getMouseX(), (int) screen.getMouseY());
    }

    private void reset() {
        this.drawOverlay = false;
        this.drawUnderlay = false;
        this.displayStack = this.slot.getItem();
        this.displayString = null;
    }

    public boolean isMouseOverSlot(IScreenContext screen) {
        Rect2F rect = this.getScreenspaceDimensions(screen).offsetSize(-1, -1);
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

    @Override
    public boolean useManagedMatrix() {
        // Slots can't use a managed matrix because they have to draw labels and tooltips directly to the screen using the mouse position.
        return false;
    }
}
