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

import com.google.common.collect.Maps;
import com.tridevmc.compound.core.reflect.WrappedField;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.IInternalCompoundUI;
import com.tridevmc.compound.ui.container.CompoundContainerMenu;
import com.tridevmc.compound.ui.debug.DebugOverlayConfig;
import com.tridevmc.compound.ui.element.InventorySlot;
import com.tridevmc.compound.ui.event.KeyInputEvent;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseMoveEvent;
import com.tridevmc.compound.ui.event.MouseScrollEvent;
import com.tridevmc.compound.ui.scope.RootScope;
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

import java.util.Map;


public abstract class ComposedUIContainer<T extends CompoundContainerMenu> extends AbstractContainerScreen<T> implements IInternalCompoundUI {

    private static final WrappedField<Slot> clickedSlot = WrappedField.create(AbstractContainerScreen.class, "clickedSlot", "field_147005_v");
    private static final WrappedField<Boolean> isSplittingStack = WrappedField.create(AbstractContainerScreen.class, "isSplittingStack", "field_147004_w");
    private static final WrappedField<ItemStack> draggingItem = WrappedField.create(AbstractContainerScreen.class, "draggingItem", "field_147012_x");
    private static final WrappedField<Integer> quickCraftingType = WrappedField.create(AbstractContainerScreen.class, "quickCraftingType", "field_146987_F");
    private final CompoundScreenContext screenContext;
    private final UITree tree;
    private final Map<Slot, InventorySlot> slotElements;
    private GuiGraphicsExtractor activeGuiGraphics;
    private long ticks;
    private float mouseX, mouseY;
    private float prevMouseX, prevMouseY;

    public ComposedUIContainer(T container, Inventory inventory, Component title) {
        super(container, inventory, title);

        this.screenContext = new CompoundScreenContext(this);
        this.tree = new UITree();
        this.slotElements = Maps.newHashMap();
    }

    @Override
    protected void init() {
        var viewport = this.tree.getViewportSize();
        if (this.tree.hasRoot() && viewport.width() == this.width && viewport.height() == this.height) {
            this.tree.requestRemeasure(this.tree.getRoot());
            return;
        }
        var previousFocus = this.tree.getFocusedNode();
        this.tree.reset();
        this.tree.setViewportSize(this.width, this.height);
        this.slotElements.clear();
        this.compose(new RootScope(this.tree));
        this.discoverSlotElements();
        if (previousFocus != null) {
            var restored = this.tree.getNodeForElement(previousFocus.getElement());
            if (restored != null) this.tree.requestFocus(restored);
        }
    }

    /**
     * Traverses the tree to find all InventorySlot instances and register them.
     */
    private void discoverSlotElements() {
        this.tree.walkDepthFirst(this.tree.getRoot(), node -> {
            if (node.getElement() instanceof InventorySlot InventorySlot) {
                this.slotElements.put(InventorySlot.getVanillaSlot(), InventorySlot);
            }
        });
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
    protected abstract void compose(RootScope scope);

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

        if (this.slotElements.isEmpty() && this.tree.hasRoot()) {
            this.discoverSlotElements();
        }

        this.tree.layoutAndRender(this.width, this.height, this.screenContext);
        this.updateSlotStates();

        this.tree.getRequestedCursor().select(this.minecraft.getWindow());
        super.extractCarriedItem(gg, mouseX, mouseY);
        super.extractSnapbackItem(gg);
        super.extractTooltip(gg, mouseX, mouseY);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.ticks++;
    }

    /**
     * Updates the state of all the slot elements to match the user input.
     * <p>
     * For internal use only.
     */
    private void updateSlotStates() {
        var clickSlot = clickedSlot.get(this);
        var dragItem = draggingItem.get(this);
        var quickCraftType = quickCraftingType.get(this);
        var splittingStack = isSplittingStack.get(this);

        Slot newHoveredSlot = this.findHoveredSlot((int) this.mouseX, (int) this.mouseY);

        for (int i1 = 0; i1 < this.getMenu().slots.size(); ++i1) {
            var slot = this.getMenu().slots.get(i1);
            var slotElement = this.slotElements.get(slot);
            if (slotElement == null)
                continue;

            var displayStack = slot.getItem();
            var playerStack = this.getMc().player.containerMenu.getCarried();
            if (slot == clickSlot && !dragItem.isEmpty() && splittingStack && !displayStack.isEmpty()) {
                displayStack = displayStack.copy();
                displayStack.setCount(displayStack.getCount() / 2);
            } else if (this.isQuickCrafting && this.quickCraftSlots.contains(slot) && !playerStack.isEmpty()) {
                if (this.quickCraftSlots.size() == 1) {
                    return;
                }

                if (AbstractContainerMenu.canItemQuickReplace(slot, playerStack, true) && this.getMenu().canDragTo(slot)) {
                    displayStack = playerStack.copy();
                    slotElement.setDrawUnderlay(true);
                    var maxSize = Math.min(playerStack.getMaxStackSize(), slot.getMaxStackSize(playerStack));
                    var existingSlotContent = slot.getItem().isEmpty() ? 0 : slot.getItem().getCount();
                    int quickCraftPlaceCount;
                    if (quickCraftType == 0) {
                        // DISTRIBUTE_EVENLY
                        quickCraftPlaceCount = (playerStack.getCount() + this.quickCraftSlots.size() - 1) / this.quickCraftSlots.size();
                    } else if (quickCraftType == 1) {
                        // SINGLE_ITEM
                        quickCraftPlaceCount = 1;
                    } else {
                        // CLONE
                        quickCraftPlaceCount = playerStack.getCount();
                    }
                    quickCraftPlaceCount += existingSlotContent;
                    if (quickCraftPlaceCount > maxSize) {
                        slotElement.setDisplayString(ChatFormatting.YELLOW.toString() + maxSize);
                    }
                    displayStack = displayStack.copyWithCount(quickCraftPlaceCount);
                }
            }
            slotElement.setDisplayStack(displayStack);

            boolean isHovered = slot == newHoveredSlot;

            slotElement.setDrawOverlay(isHovered);
            if (!this.isQuickCrafting || !this.quickCraftSlots.contains(slot)) {
                slotElement.setDrawUnderlay(isHovered);
            }
        }

        this.hoveredSlot = newHoveredSlot;
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
        for (var entry : this.slotElements.entrySet()) {
            var slot = entry.getKey();
            if (slot.x == x && slot.y == y) {
                var bounds = entry.getValue().getBounds();
                return bounds != null && bounds.contains((int) mouseX, (int) mouseY)
                        && this.findHoveredSlot((int) mouseX, (int) mouseY) == slot;
            }
        }
        return false;
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

    public CompoundScreenContext getScreenContext() {
        return this.screenContext;
    }

    @Override
    public EnumUILayer getCurrentLayer() {
        return EnumUILayer.FOREGROUND;
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        // F3+B toggles debug overlay (matches Minecraft's hitbox debug pattern)
        long windowHandle = this.minecraft.getWindow().handle();
        boolean f3Down = org.lwjgl.glfw.GLFW.glfwGetKey(windowHandle, org.lwjgl.glfw.GLFW.GLFW_KEY_F3) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_B && f3Down) {
            DebugOverlayConfig.get().toggle();
            return true;
        }

        KeyInputEvent keyEvent = new KeyInputEvent(
                event.key(),
                (char) event.scancode(),
                (event.modifiers() & 1) != 0,
                (event.modifiers() & 2) != 0,
                (event.modifiers() & 4) != 0
        );
        boolean consumed = this.tree.dispatchKeyPress(keyEvent);
        return consumed || super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(@NotNull KeyEvent event) {
        KeyInputEvent keyEvent = new KeyInputEvent(
                event.key(), (char) event.scancode(),
                (event.modifiers() & 1) != 0,
                (event.modifiers() & 2) != 0,
                (event.modifiers() & 4) != 0
        );
        boolean consumed = this.tree.dispatchKeyRelease(keyEvent);
        return consumed || super.keyReleased(event);
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent event) {
        int modifiers = 0;
        if (this.minecraft.hasShiftDown()) modifiers |= 1;
        if (this.minecraft.hasControlDown()) modifiers |= 2;
        if (this.minecraft.hasAltDown()) modifiers |= 4;
        com.tridevmc.compound.ui.event.CharEvent charEvent = new com.tridevmc.compound.ui.event.CharEvent((char) event.codepoint(), modifiers);
        boolean consumed = this.tree.dispatchCharTyped(charEvent);
        return consumed || super.charTyped(event);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double pX, double pY) {
        com.tridevmc.compound.ui.event.MouseDragEvent dragEvent = new com.tridevmc.compound.ui.event.MouseDragEvent(
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
        com.tridevmc.compound.ui.event.MouseReleaseEvent releaseEvent = new com.tridevmc.compound.ui.event.MouseReleaseEvent(
                (int) event.x(), (int) event.y(), event.button()
        );
        boolean consumed = this.tree.dispatchMouseRelease((int) event.x(), (int) event.y(), releaseEvent);
        return consumed || super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        MouseScrollEvent scrollEvent = new MouseScrollEvent((int) x, (int) y, scrollY);
        boolean handled = this.tree.dispatchScroll((int) x, (int) y, scrollEvent);
        return handled || super.mouseScrolled(x, y, scrollX, scrollY);
    }
}
