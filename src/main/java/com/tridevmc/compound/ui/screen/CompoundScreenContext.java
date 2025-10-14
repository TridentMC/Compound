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
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.IInternalCompoundUI;
import com.tridevmc.compound.ui.render.CompoundRenderable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix3x2f;
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
        this.ui.getActiveGuiGraphics().drawString(this.getFont(), processor, (int) x, (int) y, 0xFF404040, false);
    }

    @Override
    public void drawCenteredFormattedCharSequence(FormattedCharSequence processor, float x, float y) {
        int stringWidth = this.getFont().width(processor);
        this.drawFormattedCharSequence(processor, x - (stringWidth / 2F), y);
    }

    @Override
    public void drawFormattedCharSequenceWithShadow(FormattedCharSequence processor, float x, float y) {
        this.ui.getActiveGuiGraphics().drawString(this.getFont(), processor, (int) x, (int) y, 0xFF404040, true);
    }

    @Override
    public void drawCenteredFormattedCharSequenceWithShadow(FormattedCharSequence processor, float x, float y) {
        int stringWidth = this.getFont().width(processor);
        this.drawFormattedCharSequenceWithShadow(processor, x - (stringWidth / 2F), y);
    }

    @Override
    public void drawTexturedRect(ResourceLocation texture, float x, float y, float width, float height, float minU, float minV, float maxU, float maxV, int zLevel) {
        var pose = new Matrix3x2f(this.getActiveStack());

        var textureView = getMc().getTextureManager().getTexture(texture).getTextureView();
        var textureSetup = TextureSetup.singleTexture(textureView);

        var bounds = new ScreenRectangle((int) x, (int) y, (int) width, (int) height).transformMaxBounds(pose);

        this.getGuiRenderState().submitGuiElement(
            new CompoundRenderable(
                RenderPipelines.GUI_TEXTURED,
                textureSetup,
                pose,
                null,
                bounds,
                consumer -> {
                    // Emit vertices in correct winding order: top-left, bottom-left, bottom-right, top-right
                    int color = -1;
                    consumer.addVertexWith2DPose(pose, x, y).setUv(minU, minV).setColor(color);
                    consumer.addVertexWith2DPose(pose, x, y + height).setUv(minU, maxV).setColor(color);
                    consumer.addVertexWith2DPose(pose, x + width, y + height).setUv(maxU, maxV).setColor(color);
                    consumer.addVertexWith2DPose(pose, x + width, y).setUv(maxU, minV).setColor(color);
                }
            )
        );
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
    }

    @Override
    public void drawGradientRect(float x, float y, float width, float height, int startColour, int endColour, int zLevel) {
        var pose = new Matrix3x2f(this.getActiveStack());

        // Calculate bounds for culling and debug rendering
        var bounds = new ScreenRectangle((int) x, (int) y, (int) width, (int) height).transformMaxBounds(pose);

        this.getGuiRenderState().submitGuiElement(
            new CompoundRenderable(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                pose,
                null,
                bounds,
                consumer -> {
                    // Emit vertices in correct winding order: top-left, bottom-left, bottom-right, top-right
                    // Top two vertices use startColour, bottom two use endColour
                    consumer.addVertexWith2DPose(pose, x, y).setColor(startColour);
                    consumer.addVertexWith2DPose(pose, x, y + height).setColor(endColour);
                    consumer.addVertexWith2DPose(pose, x + width, y + height).setColor(endColour);
                    consumer.addVertexWith2DPose(pose, x + width, y).setColor(startColour);
                }
            )
        );
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
