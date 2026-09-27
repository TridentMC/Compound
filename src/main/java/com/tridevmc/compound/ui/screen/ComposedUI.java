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

import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.IInternalCompoundUI;
import com.tridevmc.compound.ui.debug.DebugOverlayConfig;
import com.tridevmc.compound.ui.event.CharEvent;
import com.tridevmc.compound.ui.event.KeyInputEvent;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseDragEvent;
import com.tridevmc.compound.ui.event.MouseMoveEvent;
import com.tridevmc.compound.ui.event.MouseReleaseEvent;
import com.tridevmc.compound.ui.event.MouseScrollEvent;
import com.tridevmc.compound.ui.scope.RootScope;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.tree.UITree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;
import com.mojang.blaze3d.platform.InputConstants;

public abstract class ComposedUI extends Screen implements IInternalCompoundUI {

    private final CompoundScreenContext screenContext;
    private final UITree tree;
    private GuiGraphicsExtractor activeGuiGraphics;
    private Matrix3x2fStack activeStack;
    private long ticks;
    private double mouseX, mouseY;
    private double prevMouseX, prevMouseY;

    public ComposedUI() {
        super(Component.literal(""));
        this.screenContext = new CompoundScreenContext(this);
        this.tree = new UITree();
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        this.activeGuiGraphics = graphics;
        this.activeStack = graphics.pose();

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

        this.tree.layoutAndRender(this.width, this.height, this.screenContext);

        this.tree.getRequestedCursor().select();
    }

    @Override
    public void tick() {
        super.tick();
        this.ticks++;
    }

    public double getMouseX() {
        return this.mouseX;
    }

    public double getMouseY() {
        return this.mouseY;
    }

    public Matrix3x2fStack getActiveStack() {
        return this.activeStack;
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

    public GuiGraphicsExtractor getActiveGuiGraphics() {
        return this.activeGuiGraphics;
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
                event.key(),
                event.shortcutKey(),
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
