package com.tridevmc.compound.ui.element;

import com.google.common.collect.Lists;
import com.tridevmc.compound.ui.ICompoundUI;
import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.layout.ILayout;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import java.util.List;

/**
 * A simple text label element to add to UIs, contains auto resize and text wrapping functionality.
 */
public class ElementLabel extends Element {

    private final FontRenderer fontRenderer;

    private ITextComponent text;
    private boolean drawShadow, wrapText, autoSize;
    private int maxWidth, maxHeight;

    private List<ITextComponent> lines = Lists.newArrayList();
    private int longestLineWidth;

    public ElementLabel(Rect2D dimensions, ILayout layout, FontRenderer fontRenderer, boolean drawShadow, boolean wrapText, boolean autoSize, int maxWidth, int maxHeight) {
        super(dimensions, layout);
        this.fontRenderer = fontRenderer;
        this.drawShadow = drawShadow;
        this.wrapText = wrapText;
        this.autoSize = autoSize;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    public ElementLabel(Rect2D dimensions, ILayout layout, FontRenderer fontRenderer, boolean drawShadow, boolean wrapText, boolean autoSize) {
        this(dimensions, layout, fontRenderer, drawShadow, wrapText, autoSize, -1, -1);
    }

    public ElementLabel(Rect2D dimensions, ILayout layout, FontRenderer fontRenderer) {
        this(dimensions, layout, fontRenderer, true, true, true, -1, -1);
    }

    public ElementLabel(Rect2D dimensions, ILayout layout) {
        this(dimensions, layout, Minecraft.getInstance().fontRenderer);
    }

    @Override
    public void drawForeground(ICompoundUI ui) {
        IScreenContext screen = ui.getScreenContext();
        Rect2D dimensions = this.getTransformedDimensions(screen);

        double nextYLevel = dimensions.getY();
        for (ITextComponent line : this.lines) {
            if (nextYLevel + this.fontRenderer.FONT_HEIGHT > dimensions.getY() + dimensions.getHeight()) {
                nextYLevel = Math.min(dimensions.getY(), dimensions.getHeight() - this.fontRenderer.FONT_HEIGHT);
            }

            if (this.drawShadow) {
                screen.drawStringWithShadow(line.getFormattedText(), dimensions.getX(), nextYLevel, 16777215);
            } else {
                screen.drawString(line.getFormattedText(), dimensions.getX(), nextYLevel, 16777215);
            }

            nextYLevel += this.fontRenderer.FONT_HEIGHT;
        }
    }

    private void resize() {
        if (this.autoSize) {
            int newWidth = Math.min(this.getMaxWidth(), this.longestLineWidth);
            int newHeight = Math.min(this.getMaxHeight(), this.lines.size() * (this.fontRenderer.FONT_HEIGHT + 2));
            this.setDimensions(this.getDimensions().setSize(newWidth, newHeight));
        }
    }

    public void setText(String text) {
        this.setText(new TextComponentString(text));
    }

    public void setText(ITextComponent text) {
        this.text = text;

        this.lines = this.wrapText ? GuiUtilRenderComponents.splitText(this.text, this.getMaxWidth(), this.fontRenderer,
                false, false) : Lists.newArrayList(text);
        this.longestLineWidth = this.lines.stream()
                .mapToInt((c) -> this.fontRenderer.getStringWidth(c.getFormattedText()))
                .max()
                .orElseGet(this::getMaxWidth);
        this.resize();
    }

    public ITextComponent getText() {
        return this.text;
    }

    private int getMaxWidth() {
        if (this.autoSize) {
            return this.maxWidth == -1 ? Integer.MAX_VALUE : this.maxWidth;
        } else {
            return (int) this.getDimensions().getWidth();
        }
    }

    private int getMaxHeight() {
        if (this.autoSize) {
            return this.maxHeight == -1 ? Integer.MAX_VALUE : this.maxHeight;
        } else {
            return (int) this.getDimensions().getHeight();
        }
    }
}
