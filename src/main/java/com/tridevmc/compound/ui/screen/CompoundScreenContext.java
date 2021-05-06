/*
 * Copyright 2018 - 2021 TridentMC
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

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.IInternalCompoundUI;
import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.UVData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.List;

public class CompoundScreenContext implements IScreenContext {

    private IInternalCompoundUI ui;

    public CompoundScreenContext(IInternalCompoundUI ui) {
        this.ui = ui;
    }

    private float getZLevel() {
        return this.ui.getBlitOffset();
    }

    @Override
    public MatrixStack getActiveStack() {
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
    public FontRenderer getFontRenderer() {
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
        this.getMc().getTextureManager().bind(texture);
    }

    @Override
    public void drawRect(Rect2F rect, int colour) {
        this.drawGradientRect(rect, colour, colour);
    }

    @Override
    public void drawGradientRect(Rect2F rect, int startColour, int endColour) {
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

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.vertex(rect.getX() + rect.getWidth(), rect.getY(), this.getZLevel())
                .color(r1, g1, b1, a1)
                .endVertex();
        bufferbuilder.vertex(rect.getX(), rect.getY(), this.getZLevel())
                .color(r1, g1, b1, a1)
                .endVertex();
        bufferbuilder.vertex(rect.getX(), rect.getY() + rect.getHeight(), this.getZLevel())
                .color(r2, g2, b2, a2)
                .endVertex();
        bufferbuilder.vertex(rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), this.getZLevel())
                .color(r2, g2, b2, a2)
                .endVertex();
        tessellator.end();
        RenderSystem.shadeModel(7424);
        RenderSystem.disableBlend();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
    }

    @Override
    public void drawReorderingProcessor(MatrixStack matrix, IReorderingProcessor processor, float x, float y) {
        this.getFontRenderer().draw(matrix, processor, x, y, 16777215);
    }

    @Override
    public void drawCenteredReorderingProcessor(MatrixStack matrix, IReorderingProcessor processor, float x, float y) {
        int stringWidth = this.getFontRenderer().width(processor);
        this.drawReorderingProcessor(matrix, processor, x - (stringWidth / 2F), y);
    }

    @Override
    public void drawReorderingProcessorWithShadow(MatrixStack matrix, IReorderingProcessor processor, float x, float y) {
        this.getFontRenderer().drawShadow(matrix, processor, x, y, 16777215);
    }

    @Override
    public void drawCenteredReorderingProcessorWithShadow(MatrixStack matrix, IReorderingProcessor processor, float x, float y) {
        int stringWidth = this.getFontRenderer().width(processor);
        this.drawReorderingProcessorWithShadow(matrix, processor, x - (stringWidth / 2F), y);
    }

    @Override
    public void drawTexturedRect(Rect2F rect, UVData uvs) {
        this.drawTexturedRect(rect, uvs, new UVData(uvs.getU() + (float) rect.getWidth(), uvs.getV() + (float) rect.getHeight()));
    }

    @Override
    public void drawTexturedRect(float x, float y, UVData minUvs, UVData maxUvs) {
        this.drawTexturedRect(new Rect2F(x, y, maxUvs.getU() - minUvs.getU(), maxUvs.getV() - minUvs.getV()), minUvs, maxUvs);
    }

    @Override
    public void drawTexturedRect(Rect2F rect, UVData minUvs, UVData maxUvs) {
        double x = rect.getX();
        double y = rect.getY();
        double width = rect.getWidth();
        double height = rect.getHeight();
        double zLevel = this.ui.getBlitOffset();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.vertex(x, y + height, zLevel).uv(minUvs.getU() * 0.00390625F, maxUvs.getV() * 0.00390625F).endVertex();
        bufferbuilder.vertex(x + width, y + height, zLevel).uv(maxUvs.getU() * 0.00390625F, maxUvs.getV() * 0.00390625F).endVertex();
        bufferbuilder.vertex(x + width, y, zLevel).uv(maxUvs.getU() * 0.00390625F, minUvs.getV() * 0.00390625F).endVertex();
        bufferbuilder.vertex(x, y, zLevel).uv(minUvs.getU() * 0.00390625F, minUvs.getV() * 0.00390625F).endVertex();
        tessellator.end();
    }

    @Override
    public void drawTexturedRect(Rect2F rect, TextureAtlasSprite sprite) {
        this.drawTexturedRect(rect, new UVData(sprite.getU0(), sprite.getV0()), new UVData(sprite.getU1(), sprite.getV1()));
    }

    @Override
    public void drawTexturedRect(Rect2F rect, UVData uvs, float textureWidth, float textureHeight) {
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.vertex(rect.getX(), rect.getY() + rect.getHeight(), 0.0D).uv((uvs.getU() * f), (float) ((uvs.getV() + rect.getHeight()) * f1)).endVertex();
        bufferbuilder.vertex(rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), 0.0D).uv(((uvs.getU() + rect.getWidth()) * f), ((uvs.getV() + rect.getHeight()) * f1)).endVertex();
        bufferbuilder.vertex(rect.getX() + rect.getWidth(), rect.getY(), 0.0D).uv((float) ((uvs.getU() + rect.getWidth()) * f), (uvs.getV() * f1)).endVertex();
        bufferbuilder.vertex(rect.getX(), rect.getY(), 0.0D).uv((uvs.getU() * f), (uvs.getV() * f1)).endVertex();
        tessellator.end();
    }

    @Override
    public void drawTexturedRect(Rect2F rect, UVData uvs, int uWidth, int vHeight, float tileWidth, float tileHeight) {
        float f = 1.0F / tileWidth;
        float f1 = 1.0F / tileHeight;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.vertex(rect.getX(), rect.getY() + rect.getHeight(), 0.0D).uv((uvs.getU() * f), ((uvs.getV() + vHeight) * f1)).endVertex();
        bufferbuilder.vertex(rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), 0.0D).uv(((uvs.getU() + uWidth) * f), ((uvs.getV() + vHeight) * f1)).endVertex();
        bufferbuilder.vertex(rect.getX() + rect.getWidth(), rect.getY(), 0.0D).uv(((uvs.getU() + uWidth) * f), (uvs.getV() * f1)).endVertex();
        bufferbuilder.vertex(rect.getX(), rect.getY(), 0.0D).uv((uvs.getU() * f), (uvs.getV() * f1)).endVertex();
        tessellator.end();
    }

    @Override
    public void drawTiledTexturedRect(Rect2F rect, UVData uvMin, UVData uvMax) {
        float uvWidth = uvMax.getU() - uvMin.getU();
        float uvHeight = uvMax.getV() - uvMin.getV();

        for (int x = 0; x < rect.getWidth(); x += uvWidth) {
            for (int y = 0; y < rect.getHeight(); y += uvHeight) {
                float width = Math.min(uvWidth, rect.getWidth() - x);
                float height = Math.min(uvHeight, rect.getHeight() - y);
                this.drawTexturedRect(rect.offsetPosition(x, y).setSize(width, height), uvMin, new UVData(uvMin.getU() + (float) width, uvMin.getV() + (float) height));
            }
        }
    }

    @Override
    public void drawProcessorAsTooltip(MatrixStack matrix, List<IReorderingProcessor> processors, int x, int y, FontRenderer font) {
        this.ui.asGuiScreen().renderToolTip(matrix, processors, x, y, font);
    }

    @Override
    public void drawItemStack(ItemStack stack, Rect2F dimensions, String altText) {
        this.drawItemStack(stack, dimensions, altText, 200);
    }

    @Override
    public void drawItemStack(ItemStack stack, Rect2F dimensions, String altText, int blitOffset) {
        int oBlitOffset = this.ui.getBlitOffset();
        this.ui.setBlitOffset(blitOffset);
        this.getMc().getItemRenderer().blitOffset = blitOffset;
        net.minecraft.client.gui.FontRenderer font = stack.getItem().getFontRenderer(stack);
        if (font == null) font = this.getFontRenderer();
        RenderSystem.pushMatrix();
        RenderSystem.translated(dimensions.getX(), dimensions.getY(), 0);
        RenderSystem.scaled(1D / 16D, 1D / 16D, 1);
        RenderSystem.scaled(dimensions.getWidth(), dimensions.getHeight(), 1);
        this.getMc().getItemRenderer().renderAndDecorateItem(stack, 0, 0);
        this.getMc().getItemRenderer().renderGuiItemDecorations(font, stack, 0, 0, altText);
        RenderSystem.popMatrix();
        this.ui.setBlitOffset(oBlitOffset);
        this.getMc().getItemRenderer().blitOffset = 0.0F;
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
        this.ui.asGuiScreen().sendMessage(message, addToChat);
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
