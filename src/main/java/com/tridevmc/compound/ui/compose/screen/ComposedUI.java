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

import com.tridevmc.compound.core.reflect.WrappedField;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.IInternalCompoundUI;
import com.tridevmc.compound.ui.compose.event.CharEvent;
import com.tridevmc.compound.ui.compose.event.KeyInputEvent;
import com.tridevmc.compound.ui.compose.event.MouseClickEvent;
import com.tridevmc.compound.ui.compose.event.MouseDragEvent;
import com.tridevmc.compound.ui.compose.event.MouseMoveEvent;
import com.tridevmc.compound.ui.compose.event.MouseReleaseEvent;
import com.tridevmc.compound.ui.compose.event.MouseScrollEvent;
import com.tridevmc.compound.ui.compose.scope.RootScope;
import com.tridevmc.compound.ui.compose.tree.UITree;
import com.tridevmc.compound.ui.screen.CompoundScreenContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;

public abstract class ComposedUI extends Screen implements IInternalCompoundUI {

    private static final WrappedField<GuiRenderState> guiRenderState = WrappedField.create(GuiGraphics.class, "guiRenderState", "f_399111_");

    private GuiGraphics activeGuiGraphics;
    private Matrix3x2fStack activeStack;
    private long ticks;
    private double mouseX, mouseY;
    private double prevMouseX, prevMouseY;

    private CompoundScreenContext screenContext;
    private UITree tree;

    public ComposedUI() {
        super(Component.literal(""));
        this.screenContext = new CompoundScreenContext(this);
        this.tree = new UITree();

        Minecraft mc = Minecraft.getInstance();
        this.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());

        // Bootstrap composition
        RootScope scope = new RootScope(this.tree);
        this.compose(scope);

        // Initial measurement will happen in first render() call when screen dimensions are available
    }

    /**
     * Override this method to define the UI composition.
     *
     * @param scope the root composition scope
     */
    protected abstract void compose(RootScope scope);

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
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
            MouseMoveEvent.resetConsumed();
            this.tree.dispatchMouseMove((int) this.mouseX, (int) this.mouseY, moveEvent);
        }

        this.tree.layoutAndRender(this.width, this.height, this.screenContext);

        super.render(graphics, mouseX, mouseY, partialTicks);
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

    public GuiGraphics getActiveGuiGraphics() {
        return this.activeGuiGraphics;
    }

    public GuiRenderState getGuiRenderState() {
        return guiRenderState.get(this.activeGuiGraphics);
    }

    public CompoundScreenContext getScreenContext() {
        return this.screenContext;
    }

    @Override
    public EnumUILayer getCurrentLayer() {
        return EnumUILayer.FOREGROUND;
    }

    @Override
    public boolean keyPressed(@NotNull net.minecraft.client.input.KeyEvent event) {
        KeyInputEvent keyEvent = new KeyInputEvent(
                event.key(),
                '\0',
                (event.modifiers() & 1) != 0,
                (event.modifiers() & 2) != 0,
                (event.modifiers() & 4) != 0
        );
        this.tree.dispatchKeyPress(keyEvent);
        return keyEvent.isConsumed() || super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(@NotNull net.minecraft.client.input.KeyEvent event) {
        KeyInputEvent keyEvent = new KeyInputEvent(
                event.key(),
                '\0',
                (event.modifiers() & 1) != 0,
                (event.modifiers() & 2) != 0,
                (event.modifiers() & 4) != 0
        );
        this.tree.dispatchKeyRelease(keyEvent);
        return keyEvent.isConsumed() || super.keyReleased(event);
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent event) {
        CharEvent charEvent = new CharEvent((char) event.codepoint(), event.modifiers());
        CharEvent.resetConsumed();
        this.tree.dispatchCharTyped(charEvent);
        return charEvent.isConsumed() || super.charTyped(event);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double pX, double pY) {
        MouseDragEvent dragEvent = new MouseDragEvent(
                event.button(),
                (int) event.x(), (int) event.y(),
                pX, pY
        );
        MouseDragEvent.resetConsumed();
        this.tree.dispatchMouseDrag((int) event.x(), (int) event.y(), dragEvent);
        return dragEvent.isConsumed() || super.mouseDragged(event, pX, pY);
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
        this.tree.dispatchClick((int) event.x(), (int) event.y(), clickEvent);
        return clickEvent.isConsumed() || super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        MouseReleaseEvent releaseEvent = new MouseReleaseEvent(
                (int) event.x(), (int) event.y(), event.button()
        );
        MouseReleaseEvent.resetConsumed();
        this.tree.dispatchMouseRelease((int) event.x(), (int) event.y(), releaseEvent);
        return releaseEvent.isConsumed() || super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        MouseScrollEvent scrollEvent = new MouseScrollEvent((int) x, (int) y, scrollY);
        this.tree.dispatchScroll((int) x, (int) y, scrollEvent);
        return scrollEvent.isConsumed() || super.mouseScrolled(x, y, scrollX, scrollY);
    }
}
