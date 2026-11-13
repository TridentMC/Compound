package com.tridevmc.compound.ui.integration;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.Text;
import com.tridevmc.compound.ui.element.TextInput;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftMockExtension.class)
class FormattedInputMetricsTest {
    @Test
    void styledGlyphAdvancesDriveCaretAndPointerGeometry() throws Exception {
        var input = bold("abcd");
        try (var mounted = new Mounted(input, 120)) {
            input.moveCursorTo(2, false);
            mounted.prepare();
            assertEquals(mounted.text().x() + 16, mounted.caret().x());
            mounted.click(mounted.text().x() + 16);
            assertEquals(2, input.getCursorPosState().get());
        }
    }

    @Test
    void styledSelectionUsesTheSameRunAsDrawingAndPointerGeometry() throws Exception {
        var calls = new AtomicInteger();
        var input = new TextInput("abcd").setBordered(false);
        input.addFormatter((value, offset) -> {
            calls.incrementAndGet();
            return FormattedCharSequence.forward(value, Style.EMPTY.withBold(true));
        });
        try (var mounted = new Mounted(input, 120)) {
            input.moveCursorTo(2, false);
            mounted.prepare();
            int before = calls.get();
            mounted.tree.renderTree(mounted.context);
            assertEquals(before, calls.get());
            mounted.click(mounted.text().x() + 16);
            assertEquals(2, input.getCursorPosState().get());
            input.moveCursorTo(4, true);
            mounted.prepare();
            var selection = mounted.tree.findNodesByElementType(Rect.class).getFirst().getBounds();
            assertEquals(mounted.text().x() + 16, selection.x());
            assertEquals(16, selection.width());
            assertEquals("cd", input.getSelectedText());
        }
    }

    @Test
    void compositeStyledSequencesKeepLogicalUtf16Offsets() throws Exception {
        var input = new TextInput("abcd").setBordered(false);
        input.addFormatter((value, offset) -> {
            int split = Math.min(2, value.length());
            return FormattedCharSequence.composite(
                    FormattedCharSequence.forward(value.substring(0, split), Style.EMPTY),
                    FormattedCharSequence.forward(value.substring(split), Style.EMPTY.withBold(true)));
        });
        try (var mounted = new Mounted(input, 120)) {
            input.moveCursorTo(1, false);
            mounted.prepare();
            assertEquals(mounted.text().x() + 6, mounted.caret().x());
            mounted.click(mounted.text().x() + 12);
            assertEquals(2, input.getCursorPosState().get());
            input.moveCursorTo(4, true);
            mounted.prepare();
            var selection = mounted.tree.findNodesByElementType(Rect.class).getFirst().getBounds();
            assertEquals(16, selection.width());
        }
    }

    @Test
    void styledHorizontalRevealKeepsSurrogatePairsWhole() throws Exception {
        var offsets = new ArrayList<Integer>();
        var input = new TextInput("ab😀cd").setBordered(false);
        input.addFormatter((value, offset) -> {
            offsets.add(offset);
            return FormattedCharSequence.forward(value, Style.EMPTY.withBold(true));
        });
        try (var mounted = new Mounted(input, 32)) {
            input.moveCursorTo(input.getValue().length(), false);
            mounted.prepare();
            assertTrue(mounted.caret().right() <= input.getContentBounds().right());
            mounted.click(mounted.text().x());
            assertEquals(4, input.getCursorPosState().get());
            assertFalse(offsets.contains(3));
            input.moveCursorTo(0, false);
            mounted.prepare();
            mounted.click(mounted.text().x() + 16);
            assertEquals(2, input.getCursorPosState().get());
            assertEquals("ab😀cd", input.getValue());
        }
    }

    @Test
    void addingAndRemovingFormattersRefreshesGeometryWithoutEditingText() throws Exception {
        var input = new TextInput("abc").setBordered(false);
        var edits = new AtomicInteger();
        input.setResponder(value -> edits.incrementAndGet());
        try (var mounted = new Mounted(input, 120)) {
            input.moveCursorTo(3, false);
            mounted.prepare();
            assertEquals(mounted.text().x() + 18, mounted.caret().x());
            input.addFormatter((value, offset) -> FormattedCharSequence.forward(
                    value, Style.EMPTY.withBold(true)));
            mounted.prepare();
            assertEquals(mounted.text().x() + 24, mounted.caret().x());
            input.clearFormatters();
            mounted.prepare();
            assertEquals(mounted.text().x() + 18, mounted.caret().x());
            assertEquals("abc", input.getValue());
            assertEquals(0, edits.get());
        }
    }

    @Test
    void fractionalGlyphAdvancesRoundOnlyAfterAccumulation() throws Exception {
        var input = bold("abcd");
        try (var mounted = new Mounted(input, 120, 6.25F)) {
            input.moveCursorTo(4, false);
            mounted.prepare();
            assertEquals(mounted.text().x() + 25, mounted.caret().x());
            mounted.click(mounted.text().x() + 13);
            assertEquals(2, input.getCursorPosState().get());
        }
    }

    private static TextInput bold(String value) {
        return new TextInput(value).setBordered(false).addFormatter((text, offset) ->
                FormattedCharSequence.forward(text, Style.EMPTY.withBold(true)));
    }

    private static final class Mounted implements AutoCloseable {
        private final UITree tree = new UITree();
        private final IScreenContext context = mock(IScreenContext.class);
        private final Font previousFont = Minecraft.getInstance().font;
        private final int width;

        Mounted(TextInput input, int width) throws Exception {
            this(input, width, 8F);
        }

        Mounted(TextInput input, int width, float boldAdvance) throws Exception {
            this.width = width;
            var font = mock(Font.class);
            var splitter = new StringSplitter((codepoint, style) -> style.isBold() ? boldAdvance : 6F);
            when(font.getSplitter()).thenReturn(splitter);
            when(font.width(anyString())).thenAnswer(call ->
                    (int) Math.ceil(splitter.stringWidth((String) call.getArgument(0))));
            when(font.width(any(FormattedText.class))).thenAnswer(call ->
                    (int) Math.ceil(splitter.stringWidth((FormattedText) call.getArgument(0))));
            when(font.width(any(FormattedCharSequence.class))).thenAnswer(call ->
                    (int) Math.ceil(splitter.stringWidth((FormattedCharSequence) call.getArgument(0))));
            var lineHeight = Font.class.getDeclaredField("lineHeight");
            lineHeight.setAccessible(true);
            lineHeight.setInt(font, 9);
            setFont(font);
            ICompositionScope.root(this.tree).e(input, scope -> scope.layout().fixedSize(width, 20));
            this.prepare();
            this.tree.requestFocus(input.getNode());
            this.prepare();
        }

        void prepare() { this.tree.prepareFrame(this.width, 20, this.context); }
        Bounds text() { return this.tree.findNodesByElementType(Text.class).getFirst().getBounds(); }
        Bounds caret() { return this.tree.findNodesByElementType(Rect.class).getLast().getBounds(); }

        void click(int x) {
            this.tree.dispatchClick(x, this.text().y() + 1, new MouseClickEvent(
                    x, this.text().y() + 1, InputConstants.MOUSE_BUTTON_LEFT,
                    false, false, false));
        }

        private static void setFont(Font font) throws Exception {
            var field = Minecraft.class.getDeclaredField("font");
            field.setAccessible(true);
            field.set(Minecraft.getInstance(), font);
        }

        @Override
        public void close() throws Exception {
            try {
                this.tree.reset();
            } finally {
                setFont(this.previousFont);
            }
        }
    }
}
