/*
 * Copyright 2018 - 2026 TridentMC
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

package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.sprite.IScreenSpriteWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * An 18-pixel inventory slot backed by a vanilla {@link Slot}. Container screens coordinate
 * item interaction and refresh its display state; this element supplies the visual representation.
 */
public class InventorySlot extends Element implements IComposableElement {

    private static final int SLOT_SIZE = 18;

    private static final IScreenSprite SLOT_SPRITE = IScreenSprite.of(
            Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI).getSprite(
                    Identifier.withDefaultNamespace("container/slot")),
            IScreenSpriteWriter.nineSlice(
                    1, 1, 1, 1)
    );

    private static final IScreenSprite SLOT_HIGHLIGHT_BACK_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("container/slot_highlight_back")
    );
    private static final IScreenSprite SLOT_HIGHLIGHT_FRONT_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("container/slot_highlight_front")
    );

    private final Slot vanillaSlot;
    private boolean drawOverlay;
    private boolean drawUnderlay;
    private ItemStack displayStack;
    private String displayString;

    /**
     * Creates a slot element for the compose system.
     *
     * @param menu      the container menu
     * @param slotIndex the slot index in the menu
     */
    public InventorySlot(@Nonnull AbstractContainerMenu menu, int slotIndex) {
        this(menu.getSlot(slotIndex));
    }

    /**
     * Creates a slot element for the compose system.
     *
     * @param vanillaSlot the vanilla slot
     */
    public InventorySlot(@Nonnull Slot vanillaSlot) {
        this.vanillaSlot = vanillaSlot;
        this.displayStack = vanillaSlot.getItem();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        // InventorySlot has fixed intrinsic size (18x18)
        // If it has a child (Stack), ignore its size - slot is always 18x18
        return new Size(SLOT_SIZE, SLOT_SIZE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }

        // Place Stack child at full slot bounds
        return List.of(bounds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compose(ICompositionScope scope) {
        // Compose as a stack with proper layering
        scope.e(new Stack(), stack -> {
            stack.layout().fixedSize(SLOT_SIZE, SLOT_SIZE);
            // Bottom layer: slot background sprite (18x18)
            stack.e(new Sprite(SLOT_SPRITE));

            // Underlay layer: highlight sprite (24x24, positioned with -3 offset to center on slot)
            // Uses supplier so it updates dynamically without recomposition
            stack.e(new Sprite(() -> this.drawUnderlay ? SLOT_HIGHLIGHT_BACK_SPRITE : null),
                    sprite -> sprite.layout().margin(-3));

            // Item layer: the actual item with 1px margin so it doesn't touch slot edges
            stack.e(new ItemDisplay(
                            () -> this.displayStack != null ? this.displayStack : ItemStack.EMPTY,
                            () -> this.displayString
                    ), i -> i.layout().margin(1)
            );

            // Overlay layer: highlight sprite (24x24, positioned with -3 offset to center on slot)
            // Uses supplier so it updates dynamically without recomposition
            stack.e(new Sprite(() -> this.drawOverlay ? SLOT_HIGHLIGHT_FRONT_SPRITE : null),
                    sprite -> sprite.layout().margin(-3));
        });

        // ComposedUIContainer extracts vanilla item tooltips after the tree and carried items.
    }

    /**
     * Returns the backing vanilla slot without copying it.
     *
     * @return the backing slot
     */
    public Slot getVanillaSlot() {
        return this.vanillaSlot;
    }

    /**
     * Controls the highlight drawn above the slot item.
     *
     * @param drawOverlay whether to draw the front highlight
     */
    public void setDrawOverlay(boolean drawOverlay) {
        this.drawOverlay = drawOverlay;
    }

    /**
     * Controls the highlight drawn behind the slot item.
     *
     * @param drawUnderlay whether to draw the rear highlight
     */
    public void setDrawUnderlay(boolean drawUnderlay) {
        this.drawUnderlay = drawUnderlay;
    }

    /**
     * Overrides the rendered stack without changing the backing inventory.
     *
     * @param displayStack the stack to display, or null to draw no item
     */
    public void setDisplayStack(ItemStack displayStack) {
        this.displayStack = displayStack;
    }

    /**
     * Overrides the rendered item count without changing the backing inventory.
     *
     * @param displayString the count label, or null for the vanilla count
     */
    public void setDisplayString(String displayString) {
        this.displayString = displayString;
    }

    /**
     * Clears highlights and count overrides, then refreshes the displayed stack from the backing slot.
     */
    public void reset() {
        this.drawOverlay = false;
        this.drawUnderlay = false;
        this.displayStack = this.vanillaSlot.getItem();
        this.displayString = null;
    }
}

