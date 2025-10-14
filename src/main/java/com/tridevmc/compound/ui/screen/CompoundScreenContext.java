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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.IInternalCompoundUI;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
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
    public VertexConsumer getBuffer(RenderType renderType) {
        return ui.getBufferSource().getBuffer(renderType);
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
        this.ui.getActiveGuiGraphics().drawString(this.getFont(), processor, (int) x, (int) y, 16777215, false);
    }

    @Override
    public void drawCenteredFormattedCharSequence(FormattedCharSequence processor, float x, float y) {
        int stringWidth = this.getFont().width(processor);
        this.drawFormattedCharSequence(processor, x - (stringWidth / 2F), y);
    }

    @Override
    public void drawFormattedCharSequenceWithShadow(FormattedCharSequence processor, float x, float y) {
        this.ui.getActiveGuiGraphics().drawString(this.getFont(), processor, (int) x, (int) y, -1, true);
    }

    @Override
    public void drawCenteredFormattedCharSequenceWithShadow(FormattedCharSequence processor, float x, float y) {
        int stringWidth = this.getFont().width(processor);
        this.drawFormattedCharSequenceWithShadow(processor, x - (stringWidth / 2F), y);
    }

    @Override
    public void drawTexturedRect(float x, float y, float width, float height, float minU, float minV, float maxU, float maxV, int zLevel) {
        // TODO: Delegating to GuiGraphics for now, but we don't want to rely on them as the API changes way too frequently.
        // RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
        // RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // var pose = this.getActiveStack().last().pose();
        // var bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        // bb.addVertex(pose, x, y + height, zLevel).setUv(minU, maxV).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        // bb.addVertex(pose, x + width, y + height, zLevel).setUv(maxU, maxV).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        // bb.addVertex(pose, x + width, y, zLevel).setUv(maxU, minV).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        // bb.addVertex(pose, x, y, zLevel).setUv(minU, minV).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        // BufferUploader.drawWithShader(bb.buildOrThrow());
        this.ui.getActiveGuiGraphics().fill((int) x, (int) y, (int) (x + width), (int) (y + height), -1);
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
    public void drawItemStack(ItemStack stack, float x, float y, float width, float height, String altText, int zLevel) {
        var font = IClientItemExtensions.of(stack).getFont(stack, IClientItemExtensions.FontContext.TOOLTIP);
        if (font == null) font = this.getFont();
        var poseStack = this.getActiveStack();
        poseStack.pushMatrix();
        poseStack.translate(x, y);
        poseStack.scale(width / 16F, height / 16F);

        this.ui.getActiveGuiGraphics().renderItem(stack, 0, 0);
        this.ui.getActiveGuiGraphics().renderItemDecorations(font, stack, 0, 0, altText);

        poseStack.popMatrix();
        this.ui.getBufferSource().endLastBatch();
    }

    @Override
    public void drawGradientRect(float x, float y, float width, float height, int startColour, int endColour, int zLevel) {
        // TODO: Do not delegate to guigraphics as its a changing target.
        // float[] startColourUnpacked = this.getRGBA(startColour);
        // float r1 = startColourUnpacked[0];
        // float g1 = startColourUnpacked[1];
        // float b1 = startColourUnpacked[2];
        // float a1 = startColourUnpacked[3];
        //
        // float[] endColourUnpacked = this.getRGBA(endColour);
        // float r2 = endColourUnpacked[0];
        // float g2 = endColourUnpacked[1];
        // float b2 = endColourUnpacked[2];
        // float a2 = endColourUnpacked[3];
        //
        // RenderSystem.enableBlend();
        // RenderSystem.defaultBlendFunc();
        // RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        // var pose = this.getActiveStack().last().pose();
        // var bb = getBuffer(RenderType.guiOverlay());
        // bb.addVertex(pose, x + width, y, zLevel).setColor(r1, g1, b1, a1);
        // bb.addVertex(pose, x, y, zLevel).setColor(r1, g1, b1, a1);
        // bb.addVertex(pose, x, y + height, zLevel).setColor(r2, g2, b2, a2);
        // bb.addVertex(pose, x + width, y + height, zLevel).setColor(r2, g2, b2, a2);
        // RenderSystem.disableBlend();
        // this.ui.getBufferSource().endLastBatch();
        this.ui.getActiveGuiGraphics().fillGradient((int) x, (int) y, (int) (x + width), (int) (y + height), startColour, endColour);
    }

    @Override
    public void sendChatMessage(String message) {
        this.sendChatMessage(message, true);
    }

    @Override
    public void sendChatMessage(String message, boolean addToChat) {
        var player = this.getMc().player;
        if (player != null) {
            this.getMc().player.displayClientMessage(Component.translatable(message), !addToChat);
        }
    }

    @Override
    public void openWebLink(URI url) {
        Util.getPlatform().openUri(url);
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
