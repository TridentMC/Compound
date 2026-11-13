package com.tridevmc.compound.ui.visual;

import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.sprite.IScreenSpriteWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public class BufferedImageScreenContext implements IScreenContext, AutoCloseable {
    private final BufferedImage image;
    private final Graphics2D graphics;
    private final Matrix3x2fStack pose = new Matrix3x2fStack(32);
    private final Deque<Rectangle> scissors = new ArrayDeque<>();
    private final java.awt.Font font = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 9);
    private long frameNanos;

    public BufferedImageScreenContext(int width, int height) {
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.graphics = this.image.createGraphics();
        this.graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        this.graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    }

    public BufferedImage getImage() {
        return this.image;
    }

    public void saveTo(String filename) {
        var path = Path.of(filename);
        try {
            var parent = path.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            if (!ImageIO.write(this.image, "PNG", path.toFile())) {
                throw new IOException("No PNG writer available");
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to save screenshot to " + path, exception);
        }
    }

    public void drawDebugGrid() {
        this.graphics.setColor(new Color(255, 255, 255, 40));
        for (int x = 0; x < this.getWidth(); x += 18) this.graphics.drawLine(x, 0, x, this.getHeight());
        for (int y = 0; y < this.getHeight(); y += 18) this.graphics.drawLine(0, y, this.getWidth(), y);
    }

    private Graphics2D drawing() {
        var drawing = (Graphics2D) this.graphics.create();
        drawing.transform(new AffineTransform(this.pose.m00(), this.pose.m01(),
                this.pose.m10(), this.pose.m11(), this.pose.m20(), this.pose.m21()));
        return drawing;
    }

    @Override
    public void close() {
        this.graphics.dispose();
    }

    @Override
    public Matrix3x2fStack getActiveStack() {
        return this.pose;
    }

    @Override
    public int getWidth() {
        return this.image.getWidth();
    }

    @Override
    public int getHeight() {
        return this.image.getHeight();
    }

    public void setFrameNanos(long frameNanos) {
        this.frameNanos = frameNanos;
    }

    @Override
    public long frameNanos() {
        return this.frameNanos;
    }

    @Override
    public double getMouseX() { return 0; }

    @Override
    public double getMouseY() { return 0; }

    @Override
    public float getPartialTicks() { return 0; }

    @Override
    public long getTicks() { return this.frameNanos / 50_000_000L; }

    @Override
    public Screen getActiveGui() { return null; }

    @Override
    public Minecraft getMc() { return Minecraft.getInstance(); }

    @Override
    public Font getFont() { return this.getMc().font; }

    @Override
    public void drawGradientRect(float x, float y, float width, float height, int startColour, int endColour) {
        if (width <= 0 || height <= 0) return;
        var drawing = this.drawing();
        try {
            drawing.setPaint(startColour == endColour ? new Color(startColour, true)
                    : new GradientPaint(x, y, new Color(startColour, true),
                            x, y + height, new Color(endColour, true)));
            drawing.fill(new Rectangle2D.Float(x, y, width, height));
        } finally {
            drawing.dispose();
        }
    }

    private static String plainText(FormattedCharSequence sequence) {
        var text = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            text.appendCodePoint(codePoint);
            return true;
        });
        return text.toString();
    }

    private void text(FormattedCharSequence sequence, float x, float y, boolean centred, boolean shadow) {
        var drawing = this.drawing();
        try {
            drawing.setFont(this.font);
            // Match the fixture's six-pixel advance while retaining each run's colour.
            var value = plainText(sequence);
            float start = centred ? x - value.codePointCount(0, value.length()) * 3F : x;
            int[] offset = {0};
            sequence.accept((index, style, codePoint) -> {
                var glyph = new String(Character.toChars(codePoint));
                int rgb = style.getColor() == null ? 0xE0E0E0 : style.getColor().getValue();
                float glyphX = start + offset[0] * 6;
                float baseline = y + 8;
                if (shadow) {
                    drawing.setColor(new Color((rgb & 0xFCFCFC) >> 2));
                    drawing.drawString(glyph, glyphX + 1, baseline + 1);
                }
                drawing.setColor(new Color(rgb));
                drawing.drawString(glyph, glyphX, baseline);
                offset[0]++;
                return true;
            });
        } finally {
            drawing.dispose();
        }
    }

    @Override
    public void drawFormattedCharSequence(FormattedCharSequence sequence, float x, float y) {
        this.text(sequence, x, y, false, false);
    }

    @Override
    public void drawCenteredFormattedCharSequence(FormattedCharSequence sequence, float x, float y) {
        this.text(sequence, x, y, true, false);
    }

    @Override
    public void drawFormattedCharSequenceWithShadow(FormattedCharSequence sequence, float x, float y) {
        this.text(sequence, x, y, false, true);
    }

    @Override
    public void drawCenteredFormattedCharSequenceWithShadow(FormattedCharSequence sequence, float x, float y) {
        this.text(sequence, x, y, true, true);
    }

    @Override
    public void drawTexturedRect(Identifier texture, float x, float y, float width, float height,
                                 float minU, float minV, float maxU, float maxV) {
        this.drawGradientRect(x, y, width, height, 0xB48B8B8B, 0xB48B8B8B);
    }

    @Override
    public void drawSprite(IScreenSprite sprite, float x, float y, float width, float height) {
        if (sprite == null) return;
        int colour = sprite.getWriter() == IScreenSpriteWriter.tiled() ? 0xC8373737
                : sprite.getWriter() == IScreenSpriteWriter.stretch() ? 0xB48B8B8B : 0xF0181824;
        this.drawGradientRect(x, y, width, height, colour, colour);
        this.drawRectOutline(x, y, width, height, 0x3C000000, 1);
    }

    @Override
    public void drawItemStack(ItemStack stack, float x, float y, float width, float height, String altText) {
        this.drawGradientRect(x, y, width, height, 0xFF505050, 0xFF505050);
        this.drawRectOutline(x, y, width, height, 0xFFAAAAAA, 1);
    }

    @Override
    public void enableScissor(int x, int y, int right, int bottom) {
        var clip = new Rectangle(x, y, Math.max(0, right - x), Math.max(0, bottom - y));
        if (!this.scissors.isEmpty()) clip = this.scissors.peek().intersection(clip);
        this.scissors.push(clip);
        this.graphics.setClip(clip);
    }

    @Override
    public void disableScissor() {
        if (this.scissors.isEmpty()) throw new IllegalStateException("Unbalanced scissor disable");
        this.scissors.pop();
        this.graphics.setClip(this.scissors.peek());
    }

    @Override
    public void sendChatMessage(String message) { }

    @Override
    public void sendChatMessage(String message, boolean addToChat) { }

    @Override
    public void openWebLink(URI url) { }

    @Override
    public boolean isShiftDown() { return false; }

    @Override
    public boolean isAltDown() { return false; }

    @Override
    public float[] getRGBA(int colour) {
        return new float[]{(colour >> 16 & 255) / 255F, (colour >> 8 & 255) / 255F,
                (colour & 255) / 255F, (colour >>> 24) / 255F};
    }

    @Override
    public EnumUILayer getCurrentLayer() { return EnumUILayer.FOREGROUND; }

    @Override
    public void drawTooltip(List<Component> lines, int x, int y, Optional<TooltipComponent> extra, Font font) {
        this.drawProcessorAsTooltip(lines.stream().map(Component::getVisualOrderText).toList(), x, y, font);
    }

    @Override
    public void drawProcessorAsTooltip(List<FormattedCharSequence> lines, int x, int y, Font font) {
        int width = lines.stream().mapToInt(line -> {
            var value = plainText(line);
            return value.codePointCount(0, value.length()) * 6;
        }).max().orElse(0);
        this.drawRect(x, y, width + 8, lines.size() * 10 + 6, 0xF0100010);
        this.drawRectOutline(x, y, width + 8, lines.size() * 10 + 6, 0xFF500050, 1);
        for (int index = 0; index < lines.size(); index++) {
            this.drawFormattedCharSequence(lines.get(index), x + 4, y + 3 + index * 10);
        }
    }
}
