package com.tridevmc.compound.ui.container;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tridevmc.compound.core.reflect.WrappedField;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.ICompoundUI;
import com.tridevmc.compound.ui.IInternalCompoundUI;
import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.element.container.ElementSlot;
import com.tridevmc.compound.ui.layout.ILayout;
import com.tridevmc.compound.ui.listeners.*;
import com.tridevmc.compound.ui.screen.CompoundScreenContext;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class CompoundUIContainer<T extends CompoundContainer> extends ContainerScreen<T> implements ICompoundUI, IInternalCompoundUI {
    private static final WrappedField<Slot> clickedSlot = WrappedField.create(ContainerScreen.class, "clickedSlot", "field_147005_v");
    private static final WrappedField<Boolean> isRightMouseClick = WrappedField.create(ContainerScreen.class, "isRightMouseClick", "field_147004_w");
    private static final WrappedField<ItemStack> draggedStack = WrappedField.create(ContainerScreen.class, "draggedStack", "field_147012_x");
    private static final WrappedField<Integer> dragSplittingLimit = WrappedField.create(ContainerScreen.class, "dragSplittingLimit", "field_146987_F");

    private MatrixStack activeStack;
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
        super(container, Minecraft.getInstance().player.inventory, new StringTextComponent(""));

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
        this.init(mc, mc.getMainWindow().getScaledWidth(), mc.getMainWindow().getScaledHeight());
        this.initElements();
        this.elements.forEach((e) -> e.initElement(this));
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack stack, float partialTicks, int mouseX, int mouseY) {
        this.currentLayer = EnumUILayer.BACKGROUND;
        this.elements.forEach((e) -> e.drawLayer(this, EnumUILayer.BACKGROUND));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack stack, int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(stack, mouseX, mouseY);
        RenderSystem.pushMatrix();
        RenderSystem.translatef(-this.guiLeft, -this.guiTop, 0);
        this.currentLayer = EnumUILayer.FOREGROUND;
        this.elements.forEach((e) -> e.drawLayer(this, EnumUILayer.FOREGROUND));
        this.currentLayer = EnumUILayer.OVERLAY;
        this.elements.forEach((e) -> e.drawLayer(this, EnumUILayer.OVERLAY));
        RenderSystem.popMatrix();
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        this.activeStack = stack;
        this.renderBackground(stack);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.updateSlotStates();
        super.render(stack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void tick() {
        super.tick();
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
        ItemStack dragStack = draggedStack.get(this);
        Integer dragLimit = dragSplittingLimit.get(this);
        Boolean rightClick = isRightMouseClick.get(this);
        for (int i1 = 0; i1 < this.getContainer().inventorySlots.size(); ++i1) {
            Slot slot = this.getContainer().inventorySlots.get(i1);
            ElementSlot slotElement = this.slotElements.get(slot);
            if (slotElement == null)
                continue;

            ItemStack slotStack = slot.getStack();
            ItemStack playerStack = this.getMc().player.inventory.getItemStack();
            if (slot == clickSlot && !dragStack.isEmpty() && rightClick && !slotStack.isEmpty()) {
                slotStack = slotStack.copy();
                slotStack.setCount(slotStack.getCount() / 2);
            } else if (this.dragSplitting && this.dragSplittingSlots.contains(slot) && !playerStack.isEmpty()) {
                if (this.dragSplittingSlots.size() == 1) {
                    return;
                }

                if (Container.canAddItemToSlot(slot, playerStack, true) && this.getContainer().canDragIntoSlot(slot)) {
                    slotStack = playerStack.copy();
                    slotElement.setDrawUnderlay(true);
                    Container.computeStackSize(this.dragSplittingSlots, dragLimit, slotStack, slot.getStack().isEmpty() ? 0 : slot.getStack().getCount());
                    int size = Math.min(slotStack.getMaxStackSize(), slot.getItemStackLimit(slotStack));
                    if (slotStack.getCount() > size) {
                        slotElement.setDisplayString(TextFormatting.YELLOW.toString() + size);
                        slotStack.setCount(size);
                    }
                }
            }
            slotElement.setDisplayStack(slotStack);
            slotElement.setDrawOverlay(slot.isEnabled() && slotElement.isMouseOverSlot(this.screenContext));
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
        Slot slot = this.getContainer().getSlot(slotIndex);
        return this.addSlotElement(new Rect2D(slot.xPos, slot.yPos - Integer.MIN_VALUE, 18, 18), layout, slotIndex);
    }

    /**
     * Adds a slot element to the UI with the given dimensions.
     *
     * @param dimensions the dimensions of the slot to add.
     * @param layout     the layout to apply to the slot element.
     * @param slotIndex  the slot index in the container that holds this slot.
     * @return the newly created slot element.
     */
    public ElementSlot addSlotElement(Rect2D dimensions, ILayout layout, int slotIndex) {
        Slot slot = this.getContainer().getSlot(slotIndex);
        ElementSlot element = new ElementSlot(dimensions, layout, slot);
        this.addElement(element);
        this.slotElements.put(slot, element);
        return element;
    }

    @Override
    protected boolean isPointInRegion(int x, int y, int width, int height, double mouseX, double mouseY) {
        // A hack, not a clever one. Just a hack.
        Optional<Slot> matchingSlot = this.slotElements.keySet().stream()
                .filter((s) -> s.xPos == x && s.yPos == y)
                .findFirst();

        return matchingSlot.map(slot -> this.slotElements.get(slot)
                .getTransformedDimensions(this.screenContext)
                .isPointInRect(mouseX, mouseY))
                .orElseGet(() -> super.isPointInRegion(x, y, width, height, mouseX, mouseY));
    }

    @Override
    public float getMouseX() {
        return this.mouseX;
    }

    @Override
    public float getMouseY() {
        return this.mouseY;
    }

    @Override
    public MatrixStack getActiveStack() {
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
    public void renderTooltip(MatrixStack stack, List<? extends ITextProperties> lines, int x, int y, FontRenderer font) {
        super.renderToolTip(stack, lines, x, y, font);
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
