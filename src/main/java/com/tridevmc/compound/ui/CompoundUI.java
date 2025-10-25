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

package com.tridevmc.compound.ui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.tridevmc.compound.core.reflect.WrappedField;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.listeners.*;
import com.tridevmc.compound.ui.screen.CompoundScreenContext;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;

import java.util.List;

public abstract class CompoundUI extends Screen implements ICompoundUI, IInternalCompoundUI {

    private static final WrappedField<GuiRenderState> guiRenderState = WrappedField.create(GuiGraphics.class, "guiRenderState", "f_399111_");

    private GuiGraphics activeGuiGraphics;
    private Matrix3x2fStack activeStack;
    private long ticks;
    private double mouseX, mouseY;
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
        super(Component.literal(""));
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
        this.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        this.initElements();
        this.elements.forEach((e) -> e.initElement(this));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.activeGuiGraphics = graphics;
        this.activeStack = graphics.pose();
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        for (var layer : EnumUILayer.values()) {
            this.currentLayer = layer;
            this.elements.forEach((e) -> {
                if (e.useManagedMatrix()) {
                    this.activeStack.pushMatrix();
                    e.getLayout().applyToMatrix(this.screenContext, e);
                    e.drawLayer(this, layer);
                    this.activeStack.popMatrix();
                } else {
                    e.drawLayer(this, layer);
                }
            });
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void tick() {
        super.tick();
        this.ticks++;
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
    public Matrix3x2fStack getActiveStack() {
        return this.activeStack;
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
    public GuiGraphics getActiveGuiGraphics() {
        return this.activeGuiGraphics;
    }

    @Override
    public GuiRenderState getGuiRenderState() {
        return guiRenderState.get(this.activeGuiGraphics);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        this.keyPressListeners.forEach((l) -> l.listen(this.screenContext, event.key(), event.scancode(), event.modifiers()));
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(@NotNull KeyEvent event) {
        this.keyReleaseListeners.forEach((l) -> l.listen(this.screenContext, event.key(), event.scancode(), event.modifiers()));
        return super.keyReleased(event);
    }


    @Override
    public boolean charTyped(@NotNull CharacterEvent event) {
        this.charTypeListeners.forEach((l) -> l.listen(this.screenContext, (char) event.codepoint(), event.modifiers()));
        return super.charTyped(event);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double pX, double pY) {
        this.mouseDragListeners.forEach((l) -> l.listen(this.screenContext, event.x(), event.y(), event.button(), pX, pY));

        return super.mouseDragged(event, pX, pY);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean isDoubleClick) {
        this.mousePressListeners.forEach((l) -> l.listen(this.screenContext, event.x(), event.y(), event.button()));
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        this.mouseReleaseListeners.forEach((l) -> l.listen(this.screenContext, event.x(), event.y(), event.button()));
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        this.mouseScrollListeners.forEach((l) -> l.listen(this.screenContext, x, y, scrollX, scrollY));
        return super.mouseScrolled(x, y, scrollX, scrollY);
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
