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

package com.tridevmc.compound.ui.compose.element;

import com.tridevmc.compound.ui.compose.layout.Bounds;
import com.tridevmc.compound.ui.compose.layout.Constraints;
import com.tridevmc.compound.ui.compose.layout.Size;
import com.tridevmc.compound.ui.compose.scope.ICompositionScope;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * A slot element for the compose UI system.
 * Integrates with vanilla inventory management while providing declarative composition.
 * Composes several primitives: background sprite, underlay rect, item, label, overlay rect.
 */
public class ComposedSlot extends BaseContainer implements IComposableElement {

    private static final IScreenSprite SLOT_SPRITE = IScreenSprite.of(
            Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI)
                    .getSprite(ResourceLocation.withDefaultNamespace("container/slot"))
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
    public ComposedSlot(@Nonnull AbstractContainerMenu menu, int slotIndex) {
        this(menu.getSlot(slotIndex));
    }

    /**
     * Creates a slot element for the compose system.
     *
     * @param vanillaSlot the vanilla slot
     */
    public ComposedSlot(@Nonnull Slot vanillaSlot) {
        this.vanillaSlot = vanillaSlot;
        this.displayStack = vanillaSlot.getItem();
    }

    @Override
    public Size measure(Constraints constraints) {
        // Slots are fixed size (18x18)
        return new Size(18, 18);
    }

    @Override
    public void place(Bounds bounds) {
        this.setBounds(bounds);
        // Note: Vanilla slot x/y are final and set during slot construction.
        // The Integer.MIN_VALUE offset must be applied at construction time.
    }

    @Override
    public void compose(ICompositionScope scope) {
        // Compose as a stack with proper layering
        scope.e(new Stack(), stack -> {
            // Bottom layer: slot background sprite
            stack.e(new ElementSprite(SLOT_SPRITE));

            // Middle layer: underlay highlight (for quick craft selection)
            // Uses supplier so it updates dynamically without recomposition
            stack.e(new ElementRect(() -> this.drawUnderlay ? 0x80FFFFFF : 0x00FFFFFF));

            // Item layer: the actual item with optional count override
            // Uses suppliers so displayStack and displayString can update dynamically
            stack.e(new ElementItem(
                    () -> this.displayStack != null ? this.displayStack : ItemStack.EMPTY,
                    () -> this.displayString
            ));

            // Top layer: overlay highlight (for hover)
            // Uses supplier so it updates dynamically without recomposition
            stack.e(new ElementRect(() -> this.drawOverlay ? 0x80FFFFFF : 0x00FFFFFF));
        });

        // TODO: Tooltip handling
        // Tooltips are typically rendered in a separate overlay pass after all normal rendering.
        // We may need a special event handler or post-render hook for this.
        // For now, the tooltip logic remains in ComposedUIContainer's render method.
    }

    public Slot getVanillaSlot() {
        return this.vanillaSlot;
    }

    public void setDrawOverlay(boolean drawOverlay) {
        this.drawOverlay = drawOverlay;
    }

    public void setDrawUnderlay(boolean drawUnderlay) {
        this.drawUnderlay = drawUnderlay;
    }

    public void setDisplayStack(ItemStack displayStack) {
        this.displayStack = displayStack;
    }

    public void setDisplayString(String displayString) {
        this.displayString = displayString;
    }

    public void reset() {
        this.drawOverlay = false;
        this.drawUnderlay = false;
        this.displayStack = this.vanillaSlot.getItem();
        this.displayString = null;
    }
}
