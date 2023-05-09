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

package com.tridevmc.compound.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.IInternalCompoundUI;
import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.UVData;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.net.URI;
import java.util.List;

public class CompoundScreenContext implements IScreenContext {

    private final IInternalCompoundUI ui;

    public CompoundScreenContext(IInternalCompoundUI ui) {
        this.ui = ui;
    }

    @Override
    public PoseStack getActiveStack() {
        return this.ui.getActiveStack();
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
        return this.getMc().getDeltaFrameTime();
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
    public void bindTexture(ResourceLocation texture) {
        RenderSystem.setShaderTexture(0, texture);
    }

    @Override
    public void drawGradientRect(Rect2F rect, int startColour, int endColour, int zLevel) {
        float[] startColourUnpacked = this.getRGBA(startColour);
        float r1 = startColourUnpacked[0];
        float g1 = startColourUnpacked[1];
        float b1 = startColourUnpacked[2];
        float a1 = startColourUnpacked[3];

        float[] endColourUnpacked = this.getRGBA(endColour);
        float r2 = endColourUnpacked[0];
        float g2 = endColourUnpacked[1];
        float b2 = endColourUnpacked[2];
        float a2 = endColourUnpacked[3];

        RenderSystem.enableBlend();
        //RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var pose = this.getActiveStack().last().pose();
        var tessellator = Tesselator.getInstance();
        var bufferbuilder = tessellator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.vertex(pose, rect.getX() + rect.getWidth(), rect.getY(), zLevel)
                .color(r1, g1, b1, a1)
                .endVertex();
        bufferbuilder.vertex(pose, rect.getX(), rect.getY(), zLevel)
                .color(r1, g1, b1, a1)
                .endVertex();
        bufferbuilder.vertex(pose, rect.getX(), rect.getY() + rect.getHeight(), zLevel)
                .color(r2, g2, b2, a2)
                .endVertex();
        bufferbuilder.vertex(pose, rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), zLevel)
                .color(r2, g2, b2, a2)
                .endVertex();
        tessellator.end();
        //RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    @Override
    public void drawFormattedCharSequence(PoseStack matrix, FormattedCharSequence processor, float x, float y) {
        this.getFont().draw(matrix, processor, x, y, 16777215);
    }

    @Override
    public void drawCenteredFormattedCharSequence(PoseStack matrix, FormattedCharSequence processor, float x, float y) {
        int stringWidth = this.getFont().width(processor);
        this.drawFormattedCharSequence(matrix, processor, x - (stringWidth / 2F), y);
    }

    @Override
    public void drawFormattedCharSequenceWithShadow(PoseStack matrix, FormattedCharSequence processor, float x, float y) {
        this.getFont().drawShadow(matrix, processor, x, y, 16777215);
    }

    @Override
    public void drawCenteredFormattedCharSequenceWithShadow(PoseStack matrix, FormattedCharSequence processor, float x, float y) {
        int stringWidth = this.getFont().width(processor);
        this.drawFormattedCharSequenceWithShadow(matrix, processor, x - (stringWidth / 2F), y);
    }

    @Override
    public void drawTexturedRect(Rect2F rect, UVData uvs, int zLevel) {
        this.drawTexturedRect(rect, uvs, new UVData(uvs.getU() + rect.getWidth(), uvs.getV() + rect.getHeight()));
    }

    @Override
    public void drawTexturedRect(float x, float y, UVData minUvs, UVData maxUvs, int zLevel) {
        this.drawTexturedRect(new Rect2F(x, y, maxUvs.getU() - minUvs.getU(), maxUvs.getV() - minUvs.getV()), minUvs, maxUvs);
    }

    @Override
    public void drawTexturedRect(Rect2F rect, UVData minUvs, UVData maxUvs, int zLevel) {
        double x = rect.getX();
        double y = rect.getY();
        double width = rect.getWidth();
        double height = rect.getHeight();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        var pose = this.getActiveStack().last().pose();
        var tessellator = Tesselator.getInstance();
        var bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(pose, (float) x, (float) (y + height), zLevel).uv(minUvs.getU() * 0.00390625F, maxUvs.getV() * 0.00390625F).endVertex();
        bufferbuilder.vertex(pose, (float) (x + width), (float) (y + height), zLevel).uv(maxUvs.getU() * 0.00390625F, maxUvs.getV() * 0.00390625F).endVertex();
        bufferbuilder.vertex(pose, (float) (x + width), (float) y, zLevel).uv(maxUvs.getU() * 0.00390625F, minUvs.getV() * 0.00390625F).endVertex();
        bufferbuilder.vertex(pose, (float) x, (float) y, zLevel).uv(minUvs.getU() * 0.00390625F, minUvs.getV() * 0.00390625F).endVertex();
        tessellator.end();
    }

    @Override
    public void drawTexturedRect(Rect2F rect, TextureAtlasSprite sprite, int zLevel) {
        RenderSystem.setShaderTexture(0, sprite.contents().name());
        this.drawTexturedRect(rect, new UVData(sprite.getU0(), sprite.getV0()), new UVData(sprite.getU1(), sprite.getV1()));
    }

    @Override
    public void drawTexturedRect(Rect2F rect, UVData uvs, float textureWidth, float textureHeight, int zLevel) {
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        var pose = this.getActiveStack().last().pose();
        var tessellator = Tesselator.getInstance();
        var bufferbuilder = tessellator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(pose, rect.getX(), rect.getY() + rect.getHeight(), zLevel).uv((uvs.getU() * f), (uvs.getV() + rect.getHeight()) * f1).endVertex();
        bufferbuilder.vertex(pose, rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), zLevel).uv(((uvs.getU() + rect.getWidth()) * f), ((uvs.getV() + rect.getHeight()) * f1)).endVertex();
        bufferbuilder.vertex(pose, rect.getX() + rect.getWidth(), rect.getY(), zLevel).uv((uvs.getU() + rect.getWidth()) * f, (uvs.getV() * f1)).endVertex();
        bufferbuilder.vertex(pose, rect.getX(), rect.getY(), zLevel).uv((uvs.getU() * f), (uvs.getV() * f1)).endVertex();
        tessellator.end();
    }

    @Override
    public void drawTexturedRect(Rect2F rect, UVData uvs, int uWidth, int vHeight, float tileWidth, float tileHeight, int zLevel) {
        float f = 1.0F / tileWidth;
        float f1 = 1.0F / tileHeight;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        var pose = this.getActiveStack().last().pose();
        var tessellator = Tesselator.getInstance();
        var bufferbuilder = tessellator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(pose, rect.getX(), rect.getY() + rect.getHeight(), zLevel).uv((uvs.getU() * f), ((uvs.getV() + vHeight) * f1)).endVertex();
        bufferbuilder.vertex(pose, rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), zLevel).uv(((uvs.getU() + uWidth) * f), ((uvs.getV() + vHeight) * f1)).endVertex();
        bufferbuilder.vertex(pose, rect.getX() + rect.getWidth(), rect.getY(), zLevel).uv(((uvs.getU() + uWidth) * f), (uvs.getV() * f1)).endVertex();
        bufferbuilder.vertex(pose, rect.getX(), rect.getY(), zLevel).uv((uvs.getU() * f), (uvs.getV() * f1)).endVertex();
        tessellator.end();
    }

    @Override
    public void drawTiledTexturedRect(Rect2F rect, UVData uvMin, UVData uvMax, int zLevel) {
        float uvWidth = uvMax.getU() - uvMin.getU();
        float uvHeight = uvMax.getV() - uvMin.getV();

        for (int x = 0; x < rect.getWidth(); x += uvWidth) {
            for (int y = 0; y < rect.getHeight(); y += uvHeight) {
                float width = Math.min(uvWidth, rect.getWidth() - x);
                float height = Math.min(uvHeight, rect.getHeight() - y);
                this.drawTexturedRect(rect.offsetPosition(x, y).setSize(width, height), uvMin, new UVData(uvMin.getU() + width, uvMin.getV() + height));
            }
        }
    }

    @Override
    public void drawProcessorAsTooltip(PoseStack poseStack, List<FormattedCharSequence> processors, int x, int y, Font font) {
        this.ui.asGuiScreen().renderTooltip(poseStack, processors, x, y, font);
    }

    @Override
    public void drawItemStack(ItemStack stack, Rect2F dimensions, String altText) {
        this.drawItemStack(stack, dimensions, altText, 232);
    }

    @Override
    public void drawItemStack(ItemStack stack, Rect2F dimensions, String altText, int blitOffset) {
        Font font = IClientItemExtensions.of(stack).getFont(stack, IClientItemExtensions.FontContext.TOOLTIP);
        if (font == null) font = this.getFont();
        PoseStack poseStack = this.getActiveStack();
        poseStack.pushPose();
        poseStack.translate(dimensions.getX(), dimensions.getY(), 0);
        poseStack.scale(1F / 16F, 1F / 16F, 1);
        poseStack.scale(dimensions.getWidth(), dimensions.getHeight(), 1);
        poseStack.pushPose();
        poseStack.translate(0, 0, blitOffset);
        RenderSystem.applyModelViewMatrix();
        this.getMc().getItemRenderer().renderAndDecorateItem(poseStack, stack, 0, 0);
        this.getMc().getItemRenderer().renderGuiItemDecorations(poseStack, font, stack, 0, 0, altText);
        poseStack.popPose();
        poseStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    @Override
    public void drawItemStack(ItemStack stack, int x, int y, String altText) {
        this.drawItemStack(stack, new Rect2F(x, y, 16, 16), altText);
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
        return Screen.hasShiftDown();
    }

    @Override
    public boolean isAltDown() {
        return Screen.hasAltDown();
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
