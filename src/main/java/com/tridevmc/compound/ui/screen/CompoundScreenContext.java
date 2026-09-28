/*
 * Copyright 2018 - 2026 TridentMC
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
import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix3x2fStack;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class CompoundScreenContext implements IScreenContext {

    private final IInternalCompoundUI ui;

    public CompoundScreenContext(IInternalCompoundUI ui) {
        this.ui = ui;
    }

    @Override
    public Matrix3x2fStack getActiveStack() {
        return this.ui.getActiveStack();
    }

    @Override
    public GuiRenderState getGuiRenderState() {
        return this.ui.getGuiRenderState();
    }

    @Override
    public int getWidth() {
        return this.ui.getWidth();
    }

    @Override
    public int getHeight() {
        return this.ui.getHeight();
    }

    @Override
    public double getMouseX() {
        return this.ui.getMouseX();
    }

    @Override
    public double getMouseY() {
        return this.ui.getMouseY();
    }

    @Override
    public Minecraft getMc() {
        return this.ui.getMc();
    }

    @Override
    public Font getFont() {
        return this.getMc().font;
    }

    @Override
    public float getPartialTicks() {
        return this.getMc().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    @Override
    public long getTicks() {
        return this.ui.getTicks();
    }

    @Override
    public Screen getActiveGui() {
        return this.ui.asGuiScreen();
    }

    @Override
    public void drawFormattedCharSequence(FormattedCharSequence processor, float x, float y) {
        this.ui.getActiveGuiGraphics().text(this.getFont(), processor, (int) x, (int) y, 0xFF404040, false);
    }

    @Override
    public void drawCenteredFormattedCharSequence(FormattedCharSequence processor, float x, float y) {
        int stringWidth = this.getFont().width(processor);
        this.drawFormattedCharSequence(processor, x - (stringWidth / 2F), y);
    }

    @Override
    public void drawFormattedCharSequenceWithShadow(FormattedCharSequence processor, float x, float y) {
        this.ui.getActiveGuiGraphics().text(this.getFont(), processor, (int) x, (int) y, 0xFF404040, true);
    }

    @Override
    public void drawCenteredFormattedCharSequenceWithShadow(FormattedCharSequence processor, float x, float y) {
        int stringWidth = this.getFont().width(processor);
        this.drawFormattedCharSequenceWithShadow(processor, x - (stringWidth / 2F), y);
    }

    @Override
    public void drawTexturedRect(Identifier texture, float x, float y, float width, float height, float minU, float minV, float maxU, float maxV) {
        this.ui.getActiveGuiGraphics().blit(texture, (int) x, (int) y, (int) (x + width), (int) (y + height), minU, maxU, minV, maxV);
    }
    @Override
    public void drawTooltip(List<Component> tooltip, int x, int y, Optional<TooltipComponent> extraComponents, Font font) {
        this.ui.getActiveGuiGraphics().setTooltipForNextFrame(font, tooltip, extraComponents, x, y);
    }

    @Override
    public void drawProcessorAsTooltip(List<FormattedCharSequence> processors, int x, int y, Font font) {
        this.ui.getActiveGuiGraphics().setTooltipForNextFrame(font, processors, x, y);
    }

    @Override
    public void drawItemStack(ItemStack stack, float x, float y, float width, float height, String altText) {
        var font = IClientItemExtensions.of(stack).getFont(stack, IClientItemExtensions.FontContext.ITEM_COUNT);
        if (font == null) font = this.getFont();
        var poseStack = this.getActiveStack();
        poseStack.pushMatrix();
        poseStack.translate(x, y);
        poseStack.scale(width / 16F, height / 16F);

        this.ui.getActiveGuiGraphics().item(stack, 0, 0);
        this.ui.getActiveGuiGraphics().itemDecorations(font, stack, 0, 0, altText);

        poseStack.popMatrix();
    }

    @Override
    public void drawGradientRect(float x, float y, float width, float height, int startColour, int endColour) {
        this.ui.getActiveGuiGraphics().fillGradient((int) x, (int) y, (int) (x + width), (int) (y + height), startColour, endColour);
    }
    @Override
    public void sendChatMessage(String message) {
        this.sendChatMessage(message, true);
    }

    @Override
    public void sendChatMessage(String message, boolean addToChat) {
        if (addToChat) {
            this.getMc().gui.hud.getChat().addClientSystemMessage(Component.translatable(message));
        } else {
            this.getMc().gui.hud.setOverlayMessage(Component.translatable(message), false);
        }
    }
    @Override
    public void openWebLink(URI url) {
        Blaze3D.openUri(url);
    }

    @Override
    public boolean isShiftDown() {
        return this.getMc().hasShiftDown();
    }

    @Override
    public boolean isAltDown() {
        return this.getMc().hasAltDown();
    }

    @Override
    public float[] getRGBA(int colour) {
        float r = (float) (colour >> 16 & 255) / 255.0F;
        float g = (float) (colour >> 8 & 255) / 255.0F;
        float b = (float) (colour & 255) / 255.0F;
        float a = (float) (colour >> 24 & 255) / 255.0F;
        return new float[]{r, g, b, a};
    }

    @Override
    public EnumUILayer getCurrentLayer() {
        return this.ui.getCurrentLayer();
    }

}
