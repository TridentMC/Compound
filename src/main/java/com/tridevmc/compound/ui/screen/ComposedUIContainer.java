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

package com.tridevmc.compound.ui.screen;

import com.tridevmc.compound.core.reflect.WrappedField;
import com.tridevmc.compound.core.reflect.WrappedMethod;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.IInternalCompoundUI;
import com.tridevmc.compound.ui.container.CompoundContainerMenu;
import com.tridevmc.compound.ui.debug.DebugOverlayConfig;
import com.tridevmc.compound.ui.element.InventorySlot;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.element.Panel;
import com.tridevmc.compound.ui.element.Surface;
import com.tridevmc.compound.ui.event.KeyInputEvent;
import com.tridevmc.compound.ui.event.MouseReleaseEvent;
import com.tridevmc.compound.ui.event.MouseDragEvent;
import com.tridevmc.compound.ui.event.CharEvent;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseMoveEvent;
import com.tridevmc.compound.ui.event.MouseScrollEvent;
import com.tridevmc.compound.ui.scope.RootScope;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.tree.UITree;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;
import com.mojang.blaze3d.platform.InputConstants;

public abstract class ComposedUIContainer<T extends CompoundContainerMenu> extends AbstractContainerScreen<T> implements IInternalCompoundUI {

    private static final WrappedField<Integer> quickCraftingType = WrappedField.create(AbstractContainerScreen.class, "quickCraftingType", "field_146987_F");
    private static final WrappedMethod<Void> RECALCULATE_QUICK_CRAFT_REMAINING =
            WrappedMethod.create(AbstractContainerScreen.class, "recalculateQuickCraftRemaining");
    private final CompoundScreenContext screenContext;
    private final UITree tree;
    private GuiGraphicsExtractor activeGuiGraphics;
    private long ticks;
    private float mouseX, mouseY;
    private float prevMouseX, prevMouseY;

    public ComposedUIContainer(T container, Inventory inventory, Component title) {
        super(container, inventory, title);

        this.screenContext = new CompoundScreenContext(this);
        this.tree = new UITree();
    }

    @Override
    protected void init() {
        super.init();
        var viewport = this.tree.getViewportSize();
        if (this.tree.hasRoot() && viewport.width() == this.width && viewport.height() == this.height) {
            this.tree.requestRemeasure(this.tree.getRoot());
            return;
        }
        var previousFocus = this.tree.getFocusedNode();
        this.tree.reset();
        this.tree.setViewportSize(this.width, this.height);
        this.compose(new RootScope(this.tree));
        if (previousFocus != null) {
            var restored = this.tree.getNodeForElement(previousFocus.getElement());
            if (restored != null) this.tree.requestFocus(restored);
        }
    }

    @Override
    public void removed() {
        this.tree.reset();
        super.removed();
    }

    /**
     * Override this method to define the UI composition.
     *
     * @param scope the root composition scope
     */
    protected abstract void compose(ICompositionScope scope);

    @Override
    public void extractRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTicks) {
        this.activeGuiGraphics = gg;

        if (mouseX != this.mouseX || mouseY != this.mouseY) {
            this.prevMouseX = this.mouseX;
            this.prevMouseY = this.mouseY;
            this.mouseX = mouseX;
            this.mouseY = mouseY;

            MouseMoveEvent moveEvent = new MouseMoveEvent(
                    (int) this.mouseX, (int) this.mouseY,
                    (int) this.prevMouseX, (int) this.prevMouseY
            );
            this.tree.dispatchMouseMove((int) this.mouseX, (int) this.mouseY, moveEvent);
        }

        this.tree.prepareFrame(this.width, this.height, this.screenContext);
        this.updateSlotStates();
        this.tree.renderTree(this.screenContext);

        this.tree.getRequestedCursor().select();
        super.extractCarriedItem(gg, mouseX, mouseY);
        super.extractTooltip(gg, mouseX, mouseY);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.ticks++;
    }

    private void updateSlotStates() {
        var quickCraftType = quickCraftingType.get(this);

        Slot newHoveredSlot = this.findHoveredSlot((int) this.mouseX, (int) this.mouseY);
        this.hoveredSlot = newHoveredSlot;
        var playerStack = this.getMenu().getCarried();
        if (this.isQuickCrafting && !playerStack.isEmpty()) {
            boolean removed = this.quickCraftSlots.removeIf(slot -> !slot.isActive()
                    || !AbstractContainerMenu.canItemQuickReplace(slot, playerStack, true)
                    || !this.getMenu().canDragTo(slot));
            if (removed) {
                // Vanilla keeps the carried-item preview count private; reuse its calculation.
                RECALCULATE_QUICK_CRAFT_REMAINING.invoke(this, true, new Object[0]);
            }
        }
        this.tree.walkDepthFirst(this.tree.getRoot(), node -> {
            if (!(node.getElement() instanceof InventorySlot slotElement)) return;
            var slot = slotElement.getVanillaSlot();
            slotElement.reset();
            boolean isHovered = slot == newHoveredSlot && slot.isHighlightable();
            slotElement.setDrawOverlay(isHovered);
            slotElement.setDrawUnderlay(isHovered);
            var displayStack = slot.getItem();
            if (!slot.isActive()) {
                slotElement.setDisplayStack(ItemStack.EMPTY);
                return;
            }
            if (this.isQuickCrafting && this.quickCraftSlots.contains(slot) && !playerStack.isEmpty()) {
                if (this.quickCraftSlots.size() == 1) {
                    slotElement.setDisplayStack(ItemStack.EMPTY);
                    return;
                }

                if (AbstractContainerMenu.canItemQuickReplace(slot, playerStack, true) && this.getMenu().canDragTo(slot)) {
                    slotElement.setDrawUnderlay(true);
                    var maxSize = Math.min(playerStack.getMaxStackSize(), slot.getMaxStackSize(playerStack));
                    var existingSlotContent = slot.getItem().isEmpty() ? 0 : slot.getItem().getCount();
                    int quickCraftPlaceCount = AbstractContainerMenu.getQuickCraftPlaceCount(
                            this.quickCraftSlots.size(), quickCraftType, playerStack) + existingSlotContent;
                    if (quickCraftPlaceCount > maxSize) {
                        slotElement.setDisplayString(ChatFormatting.YELLOW.toString() + maxSize);
                    }
                    displayStack = playerStack.copyWithCount(Math.min(quickCraftPlaceCount, maxSize));
                }
            }
            slotElement.setDisplayStack(displayStack);
        });
    }

    private Slot findHoveredSlot(int mouseX, int mouseY) {
        var node = this.tree.findNodeAt(mouseX, mouseY);
        while (node != null) {
            if (node.getElement() instanceof InventorySlot element) {
                var slot = element.getVanillaSlot();
                var bounds = element.getBounds();
                if (slot.isActive() && bounds != null && bounds.contains(mouseX, mouseY)) return slot;
                // Slot highlight sprites extend beyond their slot; their decorative halo must not steal a neighbour's hit.
                var parent = node.getParent();
                if (parent != null) {
                    for (var sibling : parent.getChildren()) {
                        if (sibling.getElement() instanceof InventorySlot neighbour) {
                            var neighbourBounds = neighbour.getBounds();
                            if (neighbour.getVanillaSlot().isActive() && neighbourBounds != null
                                    && neighbourBounds.contains(mouseX, mouseY)) return neighbour.getVanillaSlot();
                        }
                    }
                }
                return null;
            }
            node = node.getParent();
        }
        return null;
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        // Vanilla identifies slots by their menu coordinates; the composed tree owns their visible bounds.
        var slot = this.findHoveredSlot((int) mouseX, (int) mouseY);
        return slot != null && slot.x == x && slot.y == y;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        var node = this.tree.findNodeAt((int) mouseX, (int) mouseY);
        while (node != null) {
            // Full-screen layout containers are not inventory backgrounds.
            if ((this.isContainerBackground(node.getElement()) || node.getElement() instanceof InventorySlot)
                    && node.getBounds().contains((int) mouseX, (int) mouseY)) return false;
            node = node.getParent();
        }
        return super.hasClickedOutside(mouseX, mouseY, left, top);
    }

    /**
     * Identifies composed backgrounds whose bounds protect carried items from outside-click dropping.
     * Override for custom background composites; vanilla image bounds remain the fallback.
     */
    protected boolean isContainerBackground(IElement element) {
        return element instanceof Panel || element instanceof Surface;
    }

    public double getMouseX() {
        return this.mouseX;
    }

    public double getMouseY() {
        return this.mouseY;
    }

    public GuiGraphicsExtractor getActiveGuiGraphics() {
        return this.activeGuiGraphics;
    }

    public Matrix3x2fStack getActiveStack() {
        return this.activeGuiGraphics != null ? this.activeGuiGraphics.pose() : null;
    }

    public long getTicks() {
        return this.ticks;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public Minecraft getMc() {
        return this.minecraft;
    }

    public Screen asGuiScreen() {
        return this;
    }

    public IScreenContext getScreenContext() {
        return this.screenContext;
    }

    @Override
    public EnumUILayer getCurrentLayer() {
        return EnumUILayer.FOREGROUND;
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        // F3+B toggles debug overlay (matches Minecraft's hitbox debug pattern)
        boolean f3Down = InputConstants.isKeyDown(InputConstants.KEY_F3);
        if (event.key() == InputConstants.KEY_B && f3Down) {
            DebugOverlayConfig.get().toggle();
            return true;
        }

        KeyInputEvent keyEvent = new KeyInputEvent(
                event.key(),
                event.shortcutKey(),
                event.hasShiftDown(),
                event.hasControlDownWithQuirk(),
                event.hasAltDown()
        );
        boolean consumed = this.tree.dispatchKeyPress(keyEvent);
        return consumed || super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(@NotNull KeyEvent event) {
        KeyInputEvent keyEvent = new KeyInputEvent(
                event.key(), event.shortcutKey(),
                event.hasShiftDown(),
                event.hasControlDownWithQuirk(),
                event.hasAltDown()
        );
        boolean consumed = this.tree.dispatchKeyRelease(keyEvent);
        return consumed || super.keyReleased(event);
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent event) {
        int modifiers = 0;
        if (this.minecraft.hasShiftDown()) modifiers |= InputConstants.MOD_SHIFT;
        if (this.minecraft.hasControlDown()) modifiers |= InputConstants.MOD_CONTROL;
        if (this.minecraft.hasAltDown()) modifiers |= InputConstants.MOD_ALT;
        CharEvent charEvent = new CharEvent(event.codepoint(), modifiers);
        boolean consumed = this.tree.dispatchCharTyped(charEvent);
        return consumed || super.charTyped(event);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double pX, double pY) {
        MouseDragEvent dragEvent = new MouseDragEvent(
                event.button(),
                (int) event.x(), (int) event.y(),
                pX, pY
        );
        boolean consumed = this.tree.dispatchMouseDrag((int) event.x(), (int) event.y(), dragEvent);
        return consumed || super.mouseDragged(event, pX, pY);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean isDoubleClick) {
        boolean shiftDown = this.minecraft != null && this.minecraft.hasShiftDown();
        boolean ctrlDown = this.minecraft != null && this.minecraft.hasControlDown();
        boolean altDown = this.minecraft != null && this.minecraft.hasAltDown();
        MouseClickEvent clickEvent = new MouseClickEvent(
                (int) event.x(), (int) event.y(), event.button(),
                shiftDown, ctrlDown, altDown
        );
        boolean consumed = this.tree.dispatchClick((int) event.x(), (int) event.y(), clickEvent);
        return consumed || super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        MouseReleaseEvent releaseEvent = new MouseReleaseEvent(
                (int) event.x(), (int) event.y(), event.button()
        );
        boolean consumed = this.tree.dispatchMouseRelease((int) event.x(), (int) event.y(), releaseEvent);
        return consumed || super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        MouseScrollEvent scrollEvent = new MouseScrollEvent((int) x, (int) y, scrollX, scrollY);
        boolean handled = this.tree.dispatchScroll((int) x, (int) y, scrollEvent);
        return handled || super.mouseScrolled(x, y, scrollX, scrollY);
    }
}
