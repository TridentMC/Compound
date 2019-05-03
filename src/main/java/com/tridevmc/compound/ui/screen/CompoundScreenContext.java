package com.tridevmc.compound.ui.screen;

import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.IInternalCompoundUI;
import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.UVData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
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
        return this.ui.getZLevel();
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
    public float getPartialTicks() {
        return this.ui.getPartialTicks();
    }

    @Override
    public GuiScreen getActiveGui() {
        return this.ui.asGuiScreen();
    }

    @Override
    public void bindTexture(ResourceLocation texture) {
        this.getMc().textureManager.bindTexture(texture);
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

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlphaTest();
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(7425);
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
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture2D();
    }

    @Override
    public void drawString(String text, int x, int y, int colour) {
        this.getMc().fontRenderer.drawString(text, x, y, colour);
    }

    @Override
    public void drawCenteredString(String text, int x, int y, int colour) {
        int stringWidth = this.getMc().fontRenderer.getStringWidth(text);
        this.getMc().fontRenderer.drawString(text, x - (stringWidth / 2), y, colour);
    }

    @Override
    public void drawStringWithShadow(String text, int x, int y, int colour) {
        this.getMc().fontRenderer.drawStringWithShadow(text, x, y, colour);
    }

    @Override
    public void drawCenteredStringWithShadow(String text, int x, int y, int colour) {
        int stringWidth = this.getMc().fontRenderer.getStringWidth(text);
        this.getMc().fontRenderer.drawStringWithShadow(text, x - (stringWidth / 2), y, colour);
    }

    @Override
    public void drawTexturedRect(Rect2D rect, UVData uvs) {
        this.drawTexturedRect(rect, uvs, new UVData(uvs.getU() + (float) rect.getWidth(), uvs.getV() + (float) rect.getHeight()));
    }

    @Override
    public void drawTexturedRect(float x, float y, UVData minUvs, UVData maxUvs) {
        this.ui.asGuiScreen().drawTexturedModalRect(x, y, (int) minUvs.getU(), (int) minUvs.getV(), (int) maxUvs.getU(), (int) maxUvs.getV());
    }

    @Override
    public void drawTexturedRect(Rect2D rect, UVData minUvs, UVData maxUvs) {
        double x = rect.getX();
        double y = rect.getY();
        double width = rect.getWidth();
        double height = rect.getHeight();
        float zLevel = this.ui.getZLevel();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x + 0, y + height, (double) zLevel).tex((double) ((minUvs.getU() + 0) * 0.00390625F), (double) (maxUvs.getV() * 0.00390625F)).endVertex();
        bufferbuilder.pos(x + width, y + height, (double) zLevel).tex((double) (maxUvs.getU() * 0.00390625F), (double) (maxUvs.getV() * 0.00390625F)).endVertex();
        bufferbuilder.pos(x + width, y + 0, (double) zLevel).tex((double) (maxUvs.getU() * 0.00390625F), (double) ((minUvs.getV() + 0) * 0.00390625F)).endVertex();
        bufferbuilder.pos(x + 0, y + 0, (double) zLevel).tex((double) ((minUvs.getU() + 0) * 0.00390625F), (double) ((minUvs.getV() + 0) * 0.00390625F)).endVertex();
        tessellator.draw();
    }


    @Override
    public void drawTexturedRect(Rect2D rect, TextureAtlasSprite sprite) {
        this.drawTexturedRect(rect, new UVData(sprite.getMinU(), sprite.getMinV()), new UVData(sprite.getMaxU(), sprite.getMaxV()));
    }

    @Override
    public void drawTexturedRect(Rect2D rect, UVData uvs, float textureWidth, float textureHeight) {
        Gui.drawModalRectWithCustomSizedTexture((int) rect.getX(), (int) rect.getY(), uvs.getU(), uvs.getV(), (int) rect.getWidth(), (int) rect.getHeight(), textureWidth, textureHeight);
    }

    @Override
    public void drawScaledTiledTexturedRect(Rect2D rect, UVData uvs, int uWidth, int vHeight, float tileWidth, float tileHeight) {
        Gui.drawScaledCustomSizeModalRect((int) rect.getX(), (int) rect.getY(), uvs.getU(), uvs.getV(), uWidth, vHeight, (int) rect.getWidth(), (int) rect.getHeight(), tileWidth, tileHeight);
    }

    @Override
    public void drawTooltip(ItemStack stack, int x, int y) {
        net.minecraftforge.fml.client.config.GuiUtils.preItemToolTip(stack);
        FontRenderer font = stack.getItem().getFontRenderer(stack);
        font = font == null ? this.getMc().fontRenderer : font;
        this.drawTooltip(this.ui.asGuiScreen().getItemToolTip(stack), x, y, font);
        net.minecraftforge.fml.client.config.GuiUtils.postItemToolTip();
    }

    @Override
    public void drawTooltip(String text, int x, int y) {
        this.drawTooltip(text, x, y, this.getMc().fontRenderer);
    }

    @Override
    public void drawTooltip(String text, int x, int y, FontRenderer fontRenderer) {
        this.drawTooltip(Collections.singletonList(text), x, y, fontRenderer);
    }

    @Override
    public void drawTooltip(List<String> lines, int x, int y) {
        this.drawTooltip(lines, x, y, this.getMc().fontRenderer);
    }

    @Override
    public void drawTooltip(List<String> lines, int x, int y, FontRenderer fontRenderer) {
        this.ui.asGuiScreen().drawHoveringText(lines, x, y, fontRenderer);
    }

    @Override
    public void drawTooltip(ITextComponent component, int x, int y) {
        this.ui.drawTextComponent(component, x, y);
    }

    @Override
    public void drawItemStack(ItemStack stack, Rect2D dimensions, String altText) {
        float oZLevel = this.ui.getZLevel();
        this.ui.setZLevel(200F);
        this.getMc().getItemRenderer().zLevel = 200.0F;
        net.minecraft.client.gui.FontRenderer font = stack.getItem().getFontRenderer(stack);
        if (font == null) font = this.getMc().fontRenderer;
        GlStateManager.pushMatrix();
        GlStateManager.translated(dimensions.getX(), dimensions.getY(), 0);
        GlStateManager.scaled(1D / 16D, 1D / 16D, 1);
        GlStateManager.scaled(dimensions.getWidth(), dimensions.getHeight(), 1);
        RenderHelper.enableGUIStandardItemLighting();
        this.getMc().getItemRenderer().renderItemAndEffectIntoGUI(stack, 0, 0);
        this.getMc().getItemRenderer().renderItemOverlayIntoGUI(font, stack, 0, 0, altText);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
        this.ui.setZLevel(oZLevel);
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
        this.ui.asGuiScreen().sendChatMessage(message, addToChat);
    }

    @Override
    public void openWebLink(URI url) {
        Util.getOSType().openURI(url);
    }

    @Override
    public boolean isShiftDown() {
        return GuiScreen.isShiftKeyDown();
    }

    @Override
    public boolean isAltDown() {
        return GuiScreen.isAltKeyDown();
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
        return ui.getCurrentLayer();
    }
}
