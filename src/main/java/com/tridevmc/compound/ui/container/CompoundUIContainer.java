/*
 * Copyright 2018 - 2022 TridentMC
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

package com.tridevmc.compound.ui.container;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tridevmc.compound.core.reflect.WrappedField;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.ICompoundUI;
import com.tridevmc.compound.ui.IInternalCompoundUI;
import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.element.container.ElementSlot;
import com.tridevmc.compound.ui.layout.ILayout;
import com.tridevmc.compound.ui.listeners.*;
import com.tridevmc.compound.ui.screen.CompoundScreenContext;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class CompoundUIContainer<T extends CompoundContainerMenu> extends AbstractContainerScreen<T> implements ICompoundUI, IInternalCompoundUI {
    private static final WrappedField<Slot> clickedSlot = WrappedField.create(AbstractContainerScreen.class, "clickedSlot", "field_147005_v");
    private static final WrappedField<Boolean> isSplittingStack = WrappedField.create(AbstractContainerScreen.class, "isSplittingStack", "field_147004_w");
    private static final WrappedField<ItemStack> draggingItem = WrappedField.create(AbstractContainerScreen.class, "draggingItem", "field_147012_x");
    private static final WrappedField<Integer> quickCraftingType = WrappedField.create(AbstractContainerScreen.class, "quickCraftingType", "field_146987_F");

    private PoseStack activeStack;
    private long ticks;
    private float mouseX, mouseY;
    private EnumUILayer currentLayer;

    private CompoundScreenContext screenContext;
    private List<IElement> elements;
    private Map<Slot, ElementSlot> slotElements;

    private List<IKeyPressListener> keyPressListeners;
    private List<IKeyReleaseListener> keyReleaseListeners;
    private List<ICharTypeListener> charTypeListeners;
    private List<IMouseDraggedListener> mouseDragListeners;
    private List<IMousePressListener> mousePressListeners;
    private List<IMouseReleaseListener> mouseReleaseListeners;
    private List<IMouseScrollListener> mouseScrollListeners;

    public CompoundUIContainer(T container) {
        super(container, Minecraft.getInstance().player.getInventory(), Component.empty());

        this.screenContext = new CompoundScreenContext(this);
        this.elements = Lists.newArrayList();
        this.slotElements = Maps.newHashMap();
        this.keyPressListeners = Lists.newArrayList();
        this.keyReleaseListeners = Lists.newArrayList();
        this.charTypeListeners = Lists.newArrayList();
        this.mouseDragListeners = Lists.newArrayList();
        this.mousePressListeners = Lists.newArrayList();
        this.mouseReleaseListeners = Lists.newArrayList();
        this.mouseScrollListeners = Lists.newArrayList();

        Minecraft mc = Minecraft.getInstance();
        this.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        this.initElements();
        this.elements.forEach((e) -> e.initElement(this));
    }

    @Override
    protected void renderBg(PoseStack stack, float partialTicks, int mouseX, int mouseY) {
        this.currentLayer = EnumUILayer.BACKGROUND;
        this.elements.forEach((e) -> e.drawLayer(this, EnumUILayer.BACKGROUND));
    }

    @Override
    protected void renderLabels(PoseStack stack, int mouseX, int mouseY) {
        PoseStack modelStack = RenderSystem.getModelViewStack();
        modelStack.pushPose();
        modelStack.translate(-this.leftPos, -this.topPos, 0);
        this.currentLayer = EnumUILayer.FOREGROUND;
        this.elements.forEach((e) -> e.drawLayer(this, EnumUILayer.FOREGROUND));
        this.currentLayer = EnumUILayer.OVERLAY;
        this.elements.forEach((e) -> e.drawLayer(this, EnumUILayer.OVERLAY));
        modelStack.popPose();
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        this.activeStack = stack;
        this.renderBackground(stack);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.updateSlotStates();
        super.render(stack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.ticks++;
    }

    /**
     * Updates the state of all the slot elements so match the user input.
     * <p>
     * For internal use only.
     */
    private void updateSlotStates() {
        // Load some common variables using our wrapped fields.
        Slot clickSlot = clickedSlot.get(this);
        ItemStack dragItem = draggingItem.get(this);
        Integer quickCraftType = quickCraftingType.get(this);
        Boolean splittingStack = isSplittingStack.get(this);
        for (int i1 = 0; i1 < this.getMenu().slots.size(); ++i1) {
            Slot slot = this.getMenu().slots.get(i1);
            ElementSlot slotElement = this.slotElements.get(slot);
            if (slotElement == null)
                continue;

            ItemStack slotStack = slot.getItem();
            ItemStack playerStack = this.getMc().player.containerMenu.getCarried();
            if (slot == clickSlot && !dragItem.isEmpty() && splittingStack && !slotStack.isEmpty()) {
                slotStack = slotStack.copy();
                slotStack.setCount(slotStack.getCount() / 2);
            } else if (this.isQuickCrafting && this.quickCraftSlots.contains(slot) && !playerStack.isEmpty()) {
                if (this.quickCraftSlots.size() == 1) {
                    return;
                }

                if (AbstractContainerMenu.canItemQuickReplace(slot, playerStack, true) && this.getMenu().canDragTo(slot)) {
                    slotStack = playerStack.copy();
                    slotElement.setDrawUnderlay(true);
                    AbstractContainerMenu.getQuickCraftSlotCount(this.quickCraftSlots, quickCraftType, slotStack, slot.getItem().isEmpty() ? 0 : slot.getItem().getCount());
                    int size = Math.min(slotStack.getMaxStackSize(), slot.getMaxStackSize(slotStack));
                    if (slotStack.getCount() > size) {
                        slotElement.setDisplayString(ChatFormatting.YELLOW.toString() + size);
                        slotStack.setCount(size);
                    }
                }
            }
            slotElement.setDisplayStack(slotStack);
            slotElement.setDrawOverlay(slot.isActive() && slotElement.isMouseOverSlot(this.screenContext));
        }
    }

    /**
     * Adds a slot element to the UI, uses the real positioning defined by the compound container.
     *
     * @param layout    the layout to apply to the slot element.
     * @param slotIndex the slot index in the container that holds this slot.
     * @return the newly created slot element.
     */
    public ElementSlot addSlotElement(ILayout layout, int slotIndex) {
        Slot slot = this.getMenu().getSlot(slotIndex);
        return this.addSlotElement(new Rect2F(slot.x, slot.y - Integer.MIN_VALUE, 18, 18), layout, slotIndex);
    }

    /**
     * Adds a slot element to the UI with the given dimensions.
     *
     * @param dimensions the dimensions of the slot to add.
     * @param layout     the layout to apply to the slot element.
     * @param slotIndex  the slot index in the container that holds this slot.
     * @return the newly created slot element.
     */
    public ElementSlot addSlotElement(Rect2F dimensions, ILayout layout, int slotIndex) {
        Slot slot = this.getMenu().getSlot(slotIndex);
        ElementSlot element = new ElementSlot(dimensions, layout, slot);
        this.addElement(element);
        this.slotElements.put(slot, element);
        return element;
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        // A hack, not a clever one. Just a hack.
        Optional<Slot> matchingSlot = this.slotElements.keySet().stream()
                .filter((s) -> s.x == x && s.y == y)
                .findFirst();

        return matchingSlot.map(slot -> this.slotElements.get(slot)
                        .getTransformedDimensions(this.screenContext)
                        .isPointInRect(mouseX, mouseY))
                .orElseGet(() -> super.isHovering(x, y, width, height, mouseX, mouseY));
    }

    @Override
    public double getMouseX() {
        return this.mouseX;
    }

    @Override
    public double getMouseY() {
        return this.mouseY;
    }

    @Override
    public PoseStack getActiveStack() {
        return this.activeStack;
    }

    @Override
    public int getBlitOffset() {
        return super.getBlitOffset();
    }

    @Override
    public void setBlitOffset(int blitOffset) {
        super.setBlitOffset(blitOffset);
    }

    @Override
    public long getTicks() {
        return this.ticks;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public Minecraft getMc() {
        return this.minecraft;
    }

    @Override
    public Screen asGuiScreen() {
        return this;
    }

    @Override
    public EnumUILayer getCurrentLayer() {
        return this.currentLayer;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        this.keyPressListeners.forEach((l) -> l.listen(this.screenContext, key, scanCode, modifiers));
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        this.keyReleaseListeners.forEach((l) -> l.listen(this.screenContext, key, scanCode, modifiers));
        return super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char typedChar, int modifiers) {
        this.charTypeListeners.forEach((l) -> l.listen(this.screenContext, typedChar, modifiers));
        return super.charTyped(typedChar, modifiers);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        this.mousePressListeners.forEach((l) -> l.listen(this.screenContext, x, y, button));
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double pX, double pY) {
        this.mouseDragListeners.forEach((l) -> l.listen(this.screenContext, x, y, button, pX, pY));
        return super.mouseDragged(x, y, button, pX, pY);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        this.mouseReleaseListeners.forEach((l) -> l.listen(this.screenContext, x, y, button));
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double distance) {
        this.mouseScrollListeners.forEach((l) -> l.listen(this.screenContext, x, y, distance));
        return super.mouseScrolled(x, y, distance);
    }

    @Override
    public IScreenContext getScreenContext() {
        return this.screenContext;
    }

    @Override
    public ImmutableList<IElement> getElements() {
        return ImmutableList.copyOf(this.elements);
    }

    @Override
    public void addElement(IElement element) {
        this.elements.add(element);
    }

    @Override
    public boolean removeElement(IElement element) {
        Optional<Map.Entry<Slot, ElementSlot>> entry = this.slotElements.entrySet().stream().filter(e -> e.getValue().equals(element)).findFirst();
        entry.ifPresent(e -> this.slotElements.remove(e.getKey()));
        return this.elements.remove(element);
    }

    @Override
    public void addListener(IKeyPressListener listener) {
        this.keyPressListeners.add(listener);
    }

    @Override
    public void addListener(IKeyReleaseListener listener) {
        this.keyReleaseListeners.add(listener);
    }

    @Override
    public void addListener(ICharTypeListener listener) {
        this.charTypeListeners.add(listener);
    }

    @Override
    public void addListener(IMouseDraggedListener listener) {
        this.mouseDragListeners.add(listener);
    }

    @Override
    public void addListener(IMousePressListener listener) {
        this.mousePressListeners.add(listener);
    }

    @Override
    public void addListener(IMouseReleaseListener listener) {
        this.mouseReleaseListeners.add(listener);
    }

    @Override
    public void addListener(IMouseScrollListener listener) {
        this.mouseScrollListeners.add(listener);
    }

}
