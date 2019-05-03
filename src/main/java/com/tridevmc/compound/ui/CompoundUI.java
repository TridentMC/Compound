package com.tridevmc.compound.ui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.listeners.*;
import com.tridevmc.compound.ui.screen.CompoundScreenContext;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

public abstract class CompoundUI extends GuiScreen implements ICompoundUI, IInternalCompoundUI {

    private float partialTicks;
    private float mouseX, mouseY;
    private EnumUILayer currentLayer;

    private CompoundScreenContext screenContext;
    private List<IElement> elements;

    private List<IKeyPressListener> keyPressListeners;
    private List<IKeyReleaseListener> keyReleaseListeners;
    private List<ICharTypeListener> charTypeListeners;
    private List<IMouseDraggedListener> mouseDragListeners;
    private List<IMousePressListener> mousePressListeners;
    private List<IMouseReleaseListener> mouseReleaseListeners;
    private List<IMouseScrollListener> mouseScrollListeners;

    public CompoundUI() {
        this.screenContext = new CompoundScreenContext(this);
        this.elements = Lists.newArrayList();
        this.keyPressListeners = Lists.newArrayList();
        this.keyReleaseListeners = Lists.newArrayList();
        this.charTypeListeners = Lists.newArrayList();
        this.mouseDragListeners = Lists.newArrayList();
        this.mousePressListeners = Lists.newArrayList();
        this.mouseReleaseListeners = Lists.newArrayList();
        this.mouseScrollListeners = Lists.newArrayList();

        Minecraft mc = Minecraft.getInstance();
        this.setWorldAndResolution(mc, mc.mainWindow.getScaledWidth(), mc.mainWindow.getScaledHeight());
        this.initElements();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.partialTicks = partialTicks;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        for (EnumUILayer layer : EnumUILayer.values()) {
            this.currentLayer = layer;
            this.elements.forEach((e) -> e.drawLayer(this, layer));
        }

        super.render(mouseX, mouseY, partialTicks);
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
    public float getZLevel() {
        return this.zLevel;
    }

    @Override
    public void setZLevel(float zLevel) {
        this.zLevel = zLevel;
    }

    @Override
    public float getPartialTicks() {
        return this.partialTicks;
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
        return this.mc;
    }

    @Override
    public GuiScreen asGuiScreen() {
        return this;
    }

    @Override
    public EnumUILayer getCurrentLayer() {
        return this.currentLayer;
    }

    @Override
    public void drawTextComponent(ITextComponent component, int x, int y) {
        this.handleComponentHover(component, x, y);
    }


    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        this.keyPressListeners.forEach((l) -> l.listen(key, scanCode, modifiers));
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        this.keyReleaseListeners.forEach((l) -> l.listen(key, scanCode, modifiers));
        return super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char typedChar, int modifiers) {
        this.charTypeListeners.forEach((l) -> l.listen(typedChar, modifiers));
        return super.charTyped(typedChar, modifiers);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        this.mousePressListeners.forEach((l) -> l.listen(x, y, button));
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double pX, double pY) {
        this.mouseDragListeners.forEach((l) -> l.listen(x, y, button, pX, pY));
        return super.mouseDragged(x, y, button, pX, pY);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        this.mouseReleaseListeners.forEach((l) -> l.listen(x, y, button));
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double distance) {
        this.mouseScrollListeners.forEach((l) -> l.listen(distance));
        return super.mouseScrolled(distance);
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
