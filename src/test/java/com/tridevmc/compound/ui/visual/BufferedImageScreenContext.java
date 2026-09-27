package com.tridevmc.compound.ui.visual;

import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.sprite.IScreenSpriteWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.*;
import java.util.List;

/**
 * A headless {@link com.tridevmc.compound.ui.screen.IScreenContext} that renders
 * all draw calls into a {@link BufferedImage} using Java2D.
 *
 * <p>This is the core of the visual harness: it lets you compose a UI tree,
 * measure and place it, then "render" it into an image you can save as a PNG
 * for visual debugging — all without launching Minecraft.</p>
 *
 * <p>Rendering semantics:</p>
 * <ul>
 *   <li>{@code drawGradientRect} → filled rectangle (gradient if two colours differ)</li>
 *   <li>{@code drawSprite} / {@code drawTexturedRect} / {@code drawRectUsingSprite} →
 *       tinted rectangle showing the sprite's position and size</li>
 *   <li>{@code drawFormattedCharSequence} → text rendered via Java2D</li>
 *   <li>{@code drawItemStack} → dark-grey placeholder with item name</li>
 *   <li>{@code enableScissor}/{@code disableScissor} → clip region management</li>
 * </ul>
 */
public class BufferedImageScreenContext implements com.tridevmc.compound.ui.screen.IScreenContext {

    private final BufferedImage image;
    private final Graphics2D g2d;
    private final int screenWidth;
    private final int screenHeight;
    private final java.awt.Font font;
    private final Deque<Rectangle> scissorStack = new ArrayDeque<>();

    private static final Color SLOT_BG = new Color(55, 55, 55, 200);
    private static final Color PANEL_BG = new Color(24, 24, 36, 240);
    private static final Color TEXT_COLOR = new Color(0x40, 0x40, 0x40);
    private static final Color DEFAULT_SPRITE_TINT = new Color(139, 139, 139, 180);
    private static final Color ITEM_PLACEHOLDER = new Color(80, 80, 80);

    public BufferedImageScreenContext(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.g2d = this.image.createGraphics();
        this.g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        this.g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        this.font = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 9);

        clear();
    }

    private void clear() {
        this.g2d.setComposite(AlphaComposite.Src);
        this.g2d.setColor(new Color(0, 0, 0, 0));
        this.g2d.fillRect(0, 0, this.screenWidth, this.screenHeight);
    }

    public BufferedImage getImage() {
        return this.image;
    }

    /**
     * Save the current image to a PNG file.
     */
    public void saveTo(String path) {
        try {
            javax.imageio.ImageIO.write(this.image, "PNG", new java.io.File(path));
            System.out.println("Screenshot saved to: " + new java.io.File(path).getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to save screenshot to " + path, e);
        }
    }

    /**
     * Draw debug overlay showing element bounds outlines on top of the rendered image.
     * Useful for seeing exact positioning when iterating on layout.
     */
    public void drawDebugGrid() {
        this.g2d.setColor(new Color(255, 255, 255, 40));
        for (int x = 0; x < this.screenWidth; x += 18) {
            this.g2d.drawLine(x, 0, x, this.screenHeight);
        }
        for (int y = 0; y < this.screenHeight; y += 18) {
            this.g2d.drawLine(0, y, this.screenWidth, y);
        }
    }

    private void applyClip() {
        if (!this.scissorStack.isEmpty()) {
            Rectangle clip = this.scissorStack.peek();
            this.g2d.setClip(clip.x, clip.y, clip.width, clip.height);
        } else {
            this.g2d.setClip(0, 0, this.screenWidth, this.screenHeight);
        }
    }

    private int unpackAlpha(int colour) {
        return (colour >> 24) & 0xFF;
    }

    private int unpackR(int colour) {
        return (colour >> 16) & 0xFF;
    }

    private int unpackG(int colour) {
        return (colour >> 8) & 0xFF;
    }

    private int unpackB(int colour) {
        return colour & 0xFF;
    }

    @Override
    public Matrix3x2fStack getActiveStack() {
        return new Matrix3x2fStack();
    }

    @Override
    public int getWidth() {
        return this.screenWidth;
    }

    @Override
    public int getHeight() {
        return this.screenHeight;
    }

    @Override
    public double getMouseX() {
        return 0;
    }

    @Override
    public double getMouseY() {
        return 0;
    }

    @Override
    public float getPartialTicks() {
        return 0;
    }

    @Override
    public long getTicks() {
        return 0;
    }

    @Override
    public net.minecraft.client.gui.screens.Screen getActiveGui() {
        return null;
    }

    @Override
    public Minecraft getMc() {
        return null;
    }

    @Override
    public Font getFont() {
        return null;
    }

    @Override
    public void drawGradientRect(float x, float y, float width, float height, int startColour, int endColour) {
        if (startColour == endColour) {
            int a = unpackAlpha(startColour);
            int r = unpackR(startColour);
            int g = unpackG(startColour);
            int b = unpackB(startColour);
            this.g2d.setComposite(AlphaComposite.SrcOver.derive(a / 255f));
            this.g2d.setColor(new Color(r, g, b));
            this.g2d.fillRect(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
            this.g2d.setComposite(AlphaComposite.SrcOver);
        } else {
            int a1 = unpackAlpha(startColour), r1 = unpackR(startColour), g1 = unpackG(startColour), b1 = unpackB(startColour);
            int a2 = unpackAlpha(endColour), r2 = unpackR(endColour), g2_ = unpackG(endColour), b2 = unpackB(endColour);
            int ix = Math.round(x), iy = Math.round(y);
            int iw = Math.round(width), ih = Math.round(height);
            GradientPaint gradient = new GradientPaint(ix, iy, new Color(r1, g1, b1, a1), ix, iy + ih, new Color(r2, g2_, b2, a2));
            this.g2d.setPaint(gradient);
            this.g2d.fillRect(ix, iy, iw, ih);
            this.g2d.setPaint(null);
        }
    }

    @Override
    public void drawFormattedCharSequence(FormattedCharSequence processor, float x, float y) {
        String text = processor.toString();
        if (text.isEmpty()) return;
        this.g2d.setComposite(AlphaComposite.SrcOver);
        this.g2d.setFont(this.font);
        this.g2d.setColor(TEXT_COLOR);
        this.g2d.drawString(text, Math.round(x), Math.round(y) + this.g2d.getFontMetrics().getAscent());
    }

    @Override
    public void drawCenteredFormattedCharSequence(FormattedCharSequence processor, float x, float y) {
        String text = processor.toString();
        if (text.isEmpty()) return;
        this.g2d.setComposite(AlphaComposite.SrcOver);
        this.g2d.setFont(this.font);
        FontMetrics fm = this.g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int drawX = Math.round(x) - textWidth / 2;
        this.g2d.setColor(TEXT_COLOR);
        this.g2d.drawString(text, drawX, Math.round(y) + fm.getAscent());
    }

    @Override
    public void drawFormattedCharSequenceWithShadow(FormattedCharSequence processor, float x, float y) {
        String text = processor.toString();
        if (text.isEmpty()) return;
        this.g2d.setComposite(AlphaComposite.SrcOver);
        this.g2d.setFont(this.font);
        FontMetrics fm = this.g2d.getFontMetrics();
        this.g2d.setColor(new Color(0x3F, 0x3F, 0x3F));
        this.g2d.drawString(text, Math.round(x) + 1, Math.round(y) + fm.getAscent() + 1);
        this.g2d.setColor(TEXT_COLOR);
        this.g2d.drawString(text, Math.round(x), Math.round(y) + fm.getAscent());
    }

    @Override
    public void drawCenteredFormattedCharSequenceWithShadow(FormattedCharSequence processor, float x, float y) {
        String text = processor.toString();
        if (text.isEmpty()) return;
        this.g2d.setComposite(AlphaComposite.SrcOver);
        this.g2d.setFont(this.font);
        FontMetrics fm = this.g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int drawX = Math.round(x) - textWidth / 2;
        this.g2d.setColor(new Color(0x3F, 0x3F, 0x3F));
        this.g2d.drawString(text, drawX + 1, Math.round(y) + fm.getAscent() + 1);
        this.g2d.setColor(TEXT_COLOR);
        this.g2d.drawString(text, drawX, Math.round(y) + fm.getAscent());
    }

    @Override
    public void drawTexturedRect(Identifier texture, float x, float y, float width, float height, float minU, float minV, float maxU, float maxV) {
        this.g2d.setComposite(AlphaComposite.SrcOver);
        this.g2d.setColor(DEFAULT_SPRITE_TINT);
        this.g2d.fillRect(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
    }

    @Override
    public void drawSprite(IScreenSprite sprite, float x, float y, float width, float height) {
        if (sprite == null) return;
        this.g2d.setComposite(AlphaComposite.SrcOver);

        IScreenSpriteWriter writer = sprite.getWriter();
        Color tint;
        String label;

        if (writer instanceof com.tridevmc.compound.ui.sprite.ScreenSpriteWriterNineSlice) {
            tint = PANEL_BG;
            label = null;
        } else if (writer instanceof com.tridevmc.compound.ui.sprite.ScreenSpriteWriterTile) {
            tint = SLOT_BG;
            label = null;
        } else {
            tint = DEFAULT_SPRITE_TINT;
            label = null;
        }

        this.g2d.setColor(tint);
        this.g2d.fillRect(Math.round(x), Math.round(y), Math.round(width), Math.round(height));

        this.g2d.setColor(new Color(0, 0, 0, 60));
        this.g2d.drawRect(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
    }

    @Override
    public void drawItemStack(ItemStack stack, float x, float y, float width, float height, String altText) {
        this.g2d.setComposite(AlphaComposite.SrcOver);
        this.g2d.setColor(ITEM_PLACEHOLDER);
        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.round(width);
        int ih = Math.round(height);
        this.g2d.fillRect(ix, iy, iw, ih);
        this.g2d.setColor(new Color(0xAA, 0xAA, 0xAA));
        this.g2d.drawRect(ix, iy, iw, ih);
    }

    @Override
    public void enableScissor(int x, int y, int right, int bottom) {
        Rectangle scissor;
        if (!this.scissorStack.isEmpty()) {
            Rectangle parent = this.scissorStack.peek();
            scissor = parent.intersection(new Rectangle(x, y, right - x, bottom - y));
        } else {
            scissor = new Rectangle(x, y, right - x, bottom - y);
        }
        this.scissorStack.push(scissor);
        applyClip();
    }

    @Override
    public void disableScissor() {
        if (!this.scissorStack.isEmpty()) {
            this.scissorStack.pop();
        }
        applyClip();
    }

    @Override
    public void sendChatMessage(String message) {
    }

    @Override
    public void sendChatMessage(String message, boolean addToChat) {
    }

    @Override
    public void openWebLink(URI url) {
    }

    @Override
    public boolean isShiftDown() {
        return false;
    }

    @Override
    public boolean isAltDown() {
        return false;
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
        return EnumUILayer.FOREGROUND;
    }

    @Override
    public void drawTooltip(List<Component> tooltip, int x, int y, Optional<TooltipComponent> extraComponents, Font font) {
        int lineY = y;
        this.g2d.setComposite(AlphaComposite.SrcOver);
        this.g2d.setFont(this.font);
        FontMetrics fm = this.g2d.getFontMetrics();
        int maxWidth = 0;
        for (Component line : tooltip) {
            int w = fm.stringWidth(line.getString());
            if (w > maxWidth) maxWidth = w;
        }
        this.g2d.setColor(new Color(16, 0, 16, 240));
        this.g2d.fillRect(x, y, maxWidth + 8, tooltip.size() * 10 + 6);
        this.g2d.setColor(new Color(80, 0, 80));
        this.g2d.drawRect(x, y, maxWidth + 8, tooltip.size() * 10 + 6);
        for (Component line : tooltip) {
            this.g2d.setColor(Color.WHITE);
            this.g2d.drawString(line.getString(), x + 4, lineY + fm.getAscent());
            lineY += 10;
        }
    }

    @Override
    public void drawProcessorAsTooltip(List<FormattedCharSequence> processors, int x, int y, Font font) {
        int lineY = y;
        this.g2d.setComposite(AlphaComposite.SrcOver);
        this.g2d.setFont(this.font);
        FontMetrics fm = this.g2d.getFontMetrics();
        int maxWidth = 0;
        for (FormattedCharSequence proc : processors) {
            int w = fm.stringWidth(proc.toString());
            if (w > maxWidth) maxWidth = w;
        }
        this.g2d.setColor(new Color(16, 0, 16, 240));
        this.g2d.fillRect(x, y, maxWidth + 8, processors.size() * 10 + 6);
        this.g2d.setColor(new Color(80, 0, 80));
        this.g2d.drawRect(x, y, maxWidth + 8, processors.size() * 10 + 6);
        for (FormattedCharSequence proc : processors) {
            this.g2d.setColor(Color.WHITE);
            this.g2d.drawString(proc.toString(), x + 4, lineY + fm.getAscent());
            lineY += 10;
        }
    }
}
