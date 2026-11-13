package com.tridevmc.compound.ui.element;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

final class StyledInputText {
    private final String source;
    private final Component component;
    private final List<Glyph> glyphs;
    private final int[] advances;

    private StyledInputText(String source, Component component, List<Glyph> glyphs) {
        this.source = source;
        this.component = component;
        this.glyphs = List.copyOf(glyphs);
        this.advances = new int[glyphs.size() + 1];
    }

    static StyledInputText of(String source, FormattedCharSequence formatted) {
        var component = Component.empty();
        var glyphs = new ArrayList<Glyph>();
        formatted.accept((ignored, style, codePoint) -> {
            var part = glyph(codePoint, style);
            component.append(part);
            int previous = glyphs.isEmpty() ? 0 : glyphs.getLast().end();
            glyphs.add(new Glyph(previous + Character.charCount(codePoint), part));
            return true;
        });
        return new StyledInputText(source, component, glyphs);
    }

    Component component() {
        return this.component;
    }

    int width(Font font) {
        return font.width(this.component);
    }

    int advance(Font font, int offset) {
        this.measure(font);
        int low = 0;
        int high = this.glyphs.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (this.glyphs.get(middle).end() <= offset) low = middle + 1;
            else high = middle;
        }
        return this.advances[low];
    }

    int positionAt(Font font, int x) {
        this.measure(font);
        int low = 0;
        int high = this.glyphs.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (this.advances[middle] < x) low = middle + 1;
            else high = middle;
        }
        return low == 0 ? 0 : Math.min(this.source.length(), this.glyphs.get(low - 1).end());
    }

    private void measure(Font font) {
        // Resource reloads replace glyph providers without replacing the Font instance.
        float width = 0;
        var splitter = font.getSplitter();
        for (int index = 0; index < this.glyphs.size(); index++) {
            width += splitter.stringWidth(this.glyphs.get(index).text());
            this.advances[index + 1] = (int) Math.ceil(width);
        }
    }

    private static Component glyph(int codePoint, Style style) {
        return Component.literal(new String(Character.toChars(codePoint))).setStyle(style);
    }

    private record Glyph(int end, Component text) { }
}
