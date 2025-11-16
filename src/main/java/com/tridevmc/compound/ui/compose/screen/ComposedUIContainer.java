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

package com.tridevmc.compound.ui.compose.screen;

import com.google.common.collect.Maps;
import com.tridevmc.compound.core.reflect.WrappedField;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.IInternalCompoundUI;
import com.tridevmc.compound.ui.compose.element.ComposedSlot;
import com.tridevmc.compound.ui.compose.event.KeyEvent;
import com.tridevmc.compound.ui.compose.event.MouseClickEvent;
import com.tridevmc.compound.ui.compose.event.MouseScrollEvent;
import com.tridevmc.compound.ui.compose.layout.Constraints;
import com.tridevmc.compound.ui.compose.layout.Position;
import com.tridevmc.compound.ui.compose.scope.RootScope;
import com.tridevmc.compound.ui.compose.state.State;
import com.tridevmc.compound.ui.compose.tree.UITree;
import com.tridevmc.compound.ui.container.CompoundContainerMenu;
import com.tridevmc.compound.ui.screen.CompoundScreenContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;

import java.util.Map;
import java.util.Optional;

/**
 * Simple record to hold window dimensions for state tracking.
 */
record WindowSize(int width, int height) {}


public abstract class ComposedUIContainer<T extends CompoundContainerMenu> extends AbstractContainerScreen<T> implements IInternalCompoundUI {

    private static final WrappedField<Slot> clickedSlot = WrappedField.create(AbstractContainerScreen.class, "clickedSlot", "field_147005_v");
    private static final WrappedField<Boolean> isSplittingStack = WrappedField.create(AbstractContainerScreen.class, "isSplittingStack", "field_147004_w");
    private static final WrappedField<ItemStack> draggingItem = WrappedField.create(AbstractContainerScreen.class, "draggingItem", "field_147012_x");
    private static final WrappedField<Integer> quickCraftingType = WrappedField.create(AbstractContainerScreen.class, "quickCraftingType", "field_146987_F");
    private static final WrappedField<GuiRenderState> guiRenderState = WrappedField.create(GuiGraphics.class, "guiRenderState", "f_399111_");

    private GuiGraphics activeGuiGraphics;
    private long ticks;
    private float mouseX, mouseY;

    private CompoundScreenContext screenContext;
    private UITree tree;
    private Map<Slot, ComposedSlot> slotElements;

    // Window size state - changes will trigger recomposition automatically
    private final State<WindowSize> windowSizeState = State.of(new WindowSize(0, 0));

    
    public ComposedUIContainer(T container) {
        super(container, Minecraft.getInstance().player.getInventory(), Component.empty());

        this.screenContext = new CompoundScreenContext(this);
        this.tree = new UITree();
        this.slotElements = Maps.newHashMap();

        var mc = Minecraft.getInstance();
        this.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());

        // Bootstrap composition with window size state bound to root
        RootScope scope = new RootScope(this.tree);

        // Update window size state and bind it to root - changes trigger recomposition
        this.windowSizeState.set(new WindowSize(this.width, this.height));
        scope.bind(this.windowSizeState);

        this.compose(scope);

        // After composition, discover all slot elements from the tree
        this.discoverSlotElements();
    }

    /**
     * Traverses the tree to find all ComposedSlot instances and register them.
     */
    private void discoverSlotElements() {
        this.tree.walkDepthFirst(this.tree.getRoot(), node -> {
            if (node.getElement() instanceof ComposedSlot composedSlot) {
                this.slotElements.put(composedSlot.getVanillaSlot(), composedSlot);
            }
        });
    }

    /**
     * Override this method to define the UI composition.
     *
     * @param scope the root composition scope
     */
    protected abstract void compose(RootScope scope);

    @Override
    protected void renderBg(GuiGraphics gg, float partialTicks, int mouseX, int mouseY) {
        this.activeGuiGraphics = gg;
        // Background rendering handled by tree
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        this.activeGuiGraphics = gg;
        // Label rendering handled by tree
    }

    @Override
    public void render(@NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        this.activeGuiGraphics = gg;
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        // Update slot states BEFORE layout so recomposition can be triggered
        this.updateSlotStates();

        // Update window size state if changed - this will trigger recomposition automatically
        WindowSize currentSize = new WindowSize(this.width, this.height);
        if (!currentSize.equals(this.windowSizeState.get())) {
            this.windowSizeState.set(currentSize);
        }

        // Render background overlay (dark transparent background)
        this.renderBackground(gg, mouseX, mouseY, partialTicks);

        // Render our composed UI tree
        // This renders the panel background, slots, labels, etc.
        this.tree.renderTree(this.screenContext);

        // Render carried item (the one being dragged by mouse)
        this.renderFloatingItem(gg, mouseX, mouseY, partialTicks);

        // Render tooltips for hovered slots
        this.renderTooltip(gg, mouseX, mouseY);
    }

    /**
     * Renders the item being carried by the mouse cursor.
     */
    private void renderFloatingItem(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        ItemStack carried = this.getMenu().getCarried();
        if (!carried.isEmpty()) {
            // Render the carried item at mouse position
            // Standard offset is 8 pixels to center the item on the cursor
            int x = mouseX - 8;
            int y = mouseY - 8;

            // Use drag count if we're splitting stack
            int dragCount = draggingItem.get(this).getCount();
            String countText = null;

            if (dragCount > 0 && isSplittingStack.get(this)) {
                // Show the split count
                countText = String.valueOf(dragCount);
            }

            gg.renderItem(carried, x, y);
            gg.renderItemDecorations(this.font, carried, x, y, countText);
        }
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
        // Load some common variables using our wrapped fields.
        var clickSlot = clickedSlot.get(this);
        var dragItem = draggingItem.get(this);
        var quickCraftType = quickCraftingType.get(this);
        var splittingStack = isSplittingStack.get(this);
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
                    var quickCraftPlaceCount = AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots, quickCraftType, playerStack) + existingSlotContent;
                    if (quickCraftPlaceCount > maxSize) {
                        slotElement.setDisplayString(ChatFormatting.YELLOW.toString() + maxSize);
                    }
                    displayStack = displayStack.copyWithCount(quickCraftPlaceCount);
                }
            }
            slotElement.setDisplayStack(displayStack);

            // Check if mouse is over this slot
            boolean isHovered = false;
            if (slot.isActive()) {
                var bounds = slotElement.getBounds();
                if (bounds != null) {
                    double mouseX = this.screenContext.getMouseX();
                    double mouseY = this.screenContext.getMouseY();
                    isHovered = mouseX >= bounds.x() && mouseX < bounds.right() &&
                               mouseY >= bounds.y() && mouseY < bounds.bottom();
                }
            }

            // Vanilla renders both underlay and overlay under the same condition: hoveredSlot != null && slot.isHighlightable()
            slotElement.setDrawOverlay(isHovered);
            if (!this.isQuickCrafting || !this.quickCraftSlots.contains(slot)) {
                slotElement.setDrawUnderlay(isHovered);
            }
        }
    }


    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        // A hack, not a clever one. Just a hack.
        var matchingSlot = this.slotElements.keySet().stream()
                .filter((s) -> s.x == x && s.y == y)
                .findFirst();

        return matchingSlot.map(slot -> {
            var composedSlot = this.slotElements.get(slot);
            var bounds = composedSlot.getBounds();
            if (bounds == null) {
                return false;
            }
            return mouseX >= bounds.x() && mouseX < bounds.right() &&
                   mouseY >= bounds.y() && mouseY < bounds.bottom();
        }).orElse(super.isHovering(x, y, width, height, mouseX, mouseY));
    }

    public double getMouseX() {
        return this.mouseX;
    }

    public double getMouseY() {
        return this.mouseY;
    }

    public GuiGraphics getActiveGuiGraphics() {
        return this.activeGuiGraphics;
    }

    public GuiRenderState getGuiRenderState() {
        return guiRenderState.get(this.getActiveGuiGraphics());
    }

    public Matrix3x2fStack getActiveStack() {
        return this.getActiveGuiGraphics() != null ? this.getActiveGuiGraphics().pose() : null;
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
        // Compose system renders in a single pass, return foreground as default
        return EnumUILayer.FOREGROUND;
    }

    @Override
    public boolean keyPressed(@NotNull net.minecraft.client.input.KeyEvent event) {
        KeyEvent keyEvent = new KeyEvent(
                event.key(),
                '\0', // Key events don't always correspond to characters
                (event.modifiers() & 1) != 0,  // shift
                (event.modifiers() & 2) != 0,  // control
                (event.modifiers() & 4) != 0   // alt
        );
        this.tree.dispatchKeyPress(keyEvent);
        return keyEvent.isConsumed() || super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(@NotNull net.minecraft.client.input.KeyEvent event) {
        KeyEvent keyEvent = new KeyEvent(
                event.key(),
                '\0', // Key events don't always correspond to characters
                (event.modifiers() & 1) != 0,  // shift
                (event.modifiers() & 2) != 0,  // control
                (event.modifiers() & 4) != 0   // alt
        );
        this.tree.dispatchKeyRelease(keyEvent);
        return keyEvent.isConsumed() || super.keyReleased(event);
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent event) {
        // Character events aren't currently supported in compose system
        return super.charTyped(event);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double pX, double pY) {
        // Mouse drag events aren't currently supported in compose system
        return super.mouseDragged(event, pX, pY);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean isDoubleClick) {
        MouseClickEvent clickEvent = new MouseClickEvent(
                (int) event.x(), (int) event.y(), event.button(),
                false, false, false // TODO: get actual modifier states
        );
        this.tree.dispatchClick((int) event.x(), (int) event.y(), clickEvent);
        return clickEvent.isConsumed() || super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        // Mouse release events aren't currently supported in compose system
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        MouseScrollEvent scrollEvent = new MouseScrollEvent((int) x, (int) y, scrollY);
        this.tree.dispatchScroll((int) x, (int) y, scrollEvent);
        return scrollEvent.isConsumed() || super.mouseScrolled(x, y, scrollX, scrollY);
    }
}
