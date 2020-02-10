package com.tridevmc.compound.ui.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.IInternalCompoundUI;
import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.UVData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;

import java.net.URI;
import java.util.Collections;
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
    public int getWidth() {
        return this.ui.getWidth();
    }

    @Override
    public int getHeight() {
        return this.ui.getHeight();
    }

    @Override
    public float getMouseX() {
        return this.ui.getMouseX();
    }

    @Override
    public float getMouseY() {
        return this.ui.getMouseY();
    }

    @Override
    public Minecraft getMc() {
        return this.ui.getMc();
    }

    @Override
    public FontRenderer getFontRenderer() {
        return this.getMc().fontRenderer;
    }

    @Override
    public float getPartialTicks() {
        return this.getMc().getRenderPartialTicks();
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
        this.getMc().getTextureManager().bindTexture(texture);
    }

    @Override
    public void drawRect(Rect2D rect, int colour) {
        this.drawGradientRect(rect, colour, colour);
    }

    @Override
    public void drawGradientRect(Rect2D rect, int startColour, int endColour) {
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
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(rect.getX() + rect.getWidth(), rect.getY(), this.getZLevel())
                .color(r1, g1, b1, a1)
                .endVertex();
        bufferbuilder.pos(rect.getX(), rect.getY(), this.getZLevel())
                .color(r1, g1, b1, a1)
                .endVertex();
        bufferbuilder.pos(rect.getX(), rect.getY() + rect.getHeight(), this.getZLevel())
                .color(r2, g2, b2, a2)
                .endVertex();
        bufferbuilder.pos(rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), this.getZLevel())
                .color(r2, g2, b2, a2)
                .endVertex();
        tessellator.draw();
        RenderSystem.shadeModel(7424);
        RenderSystem.disableBlend();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
    }

    @Override
    public void drawString(String text, double x, double y, int colour) {
        this.getMc().fontRenderer.drawString(text, (float) x, (float) y, colour);
    }

    @Override
    public void drawCenteredString(String text, double x, double y, int colour) {
        int stringWidth = this.getFontRenderer().getStringWidth(text);
        this.getFontRenderer().drawString(text, (float) x - (stringWidth / 2F), (float) y, colour);
    }

    @Override
    public void drawStringWithShadow(String text, double x, double y, int colour) {
        this.getFontRenderer().drawStringWithShadow(text, (float) x, (float) y, colour);
    }

    @Override
    public void drawCenteredStringWithShadow(String text, double x, double y, int colour) {
        int stringWidth = this.getFontRenderer().getStringWidth(text);
        this.getFontRenderer().drawStringWithShadow(text, (float) x - (stringWidth / 2F), (float) y, colour);
    }

    @Override
    public void drawTexturedRect(Rect2D rect, UVData uvs) {
        this.drawTexturedRect(rect, uvs, new UVData(uvs.getU() + (float) rect.getWidth(), uvs.getV() + (float) rect.getHeight()));
    }

    @Override
    public void drawTexturedRect(float x, float y, UVData minUvs, UVData maxUvs) {
        this.drawTexturedRect(new Rect2D(x, y, maxUvs.getU() - minUvs.getU(), maxUvs.getV() - minUvs.getV()), minUvs, maxUvs);
    }

    @Override
    public void drawTexturedRect(Rect2D rect, UVData minUvs, UVData maxUvs) {
        double x = rect.getX();
        double y = rect.getY();
        double width = rect.getWidth();
        double height = rect.getHeight();
        double zLevel = this.ui.getBlitOffset();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, y + height, zLevel).tex(minUvs.getU() * 0.00390625F, maxUvs.getV() * 0.00390625F).endVertex();
        bufferbuilder.pos(x + width, y + height, zLevel).tex(maxUvs.getU() * 0.00390625F, maxUvs.getV() * 0.00390625F).endVertex();
        bufferbuilder.pos(x + width, y, zLevel).tex(maxUvs.getU() * 0.00390625F, minUvs.getV() * 0.00390625F).endVertex();
        bufferbuilder.pos(x, y, zLevel).tex(minUvs.getU() * 0.00390625F, minUvs.getV() * 0.00390625F).endVertex();
        tessellator.draw();
    }

    @Override
    public void drawTexturedRect(Rect2D rect, TextureAtlasSprite sprite) {
        this.drawTexturedRect(rect, new UVData(sprite.getMinU(), sprite.getMinV()), new UVData(sprite.getMaxU(), sprite.getMaxV()));
    }

    @Override
    public void drawTexturedRect(Rect2D rect, UVData uvs, float textureWidth, float textureHeight) {
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(rect.getX(), rect.getY() + rect.getHeight(), 0.0D).tex((uvs.getU() * f), (float) ((uvs.getV() + rect.getHeight()) * f1)).endVertex();
        bufferbuilder.pos(rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), 0.0D).tex(((uvs.getU() + rect.getWidthF()) * f), ((uvs.getV() + rect.getHeightF()) * f1)).endVertex();
        bufferbuilder.pos(rect.getX() + rect.getWidth(), rect.getY(), 0.0D).tex((float) ((uvs.getU() + rect.getWidth()) * f), (uvs.getV() * f1)).endVertex();
        bufferbuilder.pos(rect.getX(), rect.getY(), 0.0D).tex((uvs.getU() * f), (uvs.getV() * f1)).endVertex();
        tessellator.draw();
    }

    @Override
    public void drawTexturedRect(Rect2D rect, UVData uvs, int uWidth, int vHeight, float tileWidth, float tileHeight) {
        float f = 1.0F / tileWidth;
        float f1 = 1.0F / tileHeight;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(rect.getX(), rect.getY() + rect.getHeight(), 0.0D).tex((uvs.getU() * f), ((uvs.getV() + vHeight) * f1)).endVertex();
        bufferbuilder.pos(rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), 0.0D).tex(((uvs.getU() + uWidth) * f), ((uvs.getV() + vHeight) * f1)).endVertex();
        bufferbuilder.pos(rect.getX() + rect.getWidth(), rect.getY(), 0.0D).tex(((uvs.getU() + uWidth) * f), (uvs.getV() * f1)).endVertex();
        bufferbuilder.pos(rect.getX(), rect.getY(), 0.0D).tex((uvs.getU() * f), (uvs.getV() * f1)).endVertex();
        tessellator.draw();
    }

    @Override
    public void drawTiledTexturedRect(Rect2D rect, UVData uvMin, UVData uvMax) {
        float uvWidth = uvMax.getU() - uvMin.getU();
        float uvHeight = uvMax.getV() - uvMin.getV();

        for (int x = 0; x < rect.getWidth(); x += uvWidth) {
            for (int y = 0; y < rect.getHeight(); y += uvHeight) {
                double width = Math.min(uvWidth, rect.getWidth() - x);
                double height = Math.min(uvHeight, rect.getHeight() - y);
                this.drawTexturedRect(rect.offsetPosition(x, y).setSize(width, height), uvMin, new UVData(uvMin.getU() + (float) width, uvMin.getV() + (float) height));
            }
        }
    }

    @Override
    public void drawTooltip(ItemStack stack, int x, int y) {
        net.minecraftforge.fml.client.gui.GuiUtils.preItemToolTip(stack);
        FontRenderer font = stack.getItem().getFontRenderer(stack);
        font = font == null ? this.getFontRenderer() : font;
        this.drawTooltip(this.ui.asGuiScreen().getTooltipFromItem(stack), x, y, font);
        net.minecraftforge.fml.client.gui.GuiUtils.postItemToolTip();
    }

    @Override
    public void drawTooltip(String text, int x, int y) {
        this.drawTooltip(text, x, y, this.getFontRenderer());
    }

    @Override
    public void drawTooltip(String text, int x, int y, FontRenderer fontRenderer) {
        this.drawTooltip(Collections.singletonList(text), x, y, fontRenderer);
    }

    @Override
    public void drawTooltip(List<String> lines, int x, int y) {
        this.drawTooltip(lines, x, y, this.getFontRenderer());
    }

    @Override
    public void drawTooltip(List<String> lines, int x, int y, FontRenderer fontRenderer) {
        this.ui.asGuiScreen().renderTooltip(lines, x, y, fontRenderer);
    }

    @Override
    public void drawTooltip(ITextComponent component, int x, int y) {
        this.ui.drawTextComponent(component, x, y);
    }

    @Override
    public void drawItemStack(ItemStack stack, Rect2D dimensions, String altText) {
        int oBlitOffset = this.ui.getBlitOffset();
        this.ui.setBlitOffset(200);
        this.getMc().getItemRenderer().zLevel = 200.0F;
        net.minecraft.client.gui.FontRenderer font = stack.getItem().getFontRenderer(stack);
        if (font == null) font = this.getFontRenderer();
        RenderSystem.pushMatrix();
        RenderSystem.translated(dimensions.getX(), dimensions.getY(), 0);
        RenderSystem.scaled(1D / 16D, 1D / 16D, 1);
        RenderSystem.scaled(dimensions.getWidth(), dimensions.getHeight(), 1);
        RenderHelper.enableStandardItemLighting();
        this.getMc().getItemRenderer().renderItemAndEffectIntoGUI(stack, 0, 0);
        this.getMc().getItemRenderer().renderItemOverlayIntoGUI(font, stack, 0, 0, altText);
        RenderHelper.disableStandardItemLighting();
        RenderSystem.popMatrix();
        this.ui.setBlitOffset(oBlitOffset);
        this.getMc().getItemRenderer().zLevel = 0.0F;
    }

    @Override
    public void drawItemStack(ItemStack stack, int x, int y, String altText) {
        this.drawItemStack(stack, new Rect2D(x, y, 16, 16), altText);
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
        Util.getOSType().openURI(url);
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
