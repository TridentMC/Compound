package com.tridevmc.compound.ui.integration;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.IComposableElement;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.NumberInput;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.Text;
import com.tridevmc.compound.ui.element.TextArea;
import com.tridevmc.compound.ui.element.TextInput;
import com.tridevmc.compound.ui.event.CharEvent;
import com.tridevmc.compound.ui.event.KeyInputEvent;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseScrollEvent;
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.Position;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.text.api.TextInsets;
import com.tridevmc.compound.ui.tree.UITree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MinecraftMockExtension.class)
class TextEditorGeometryTest {
    private final IScreenContext context = mock(IScreenContext.class);
    private UITree tree;

    @BeforeEach
    void font() throws ReflectiveOperationException {
        var font = Minecraft.getInstance().font;
        when(font.getSplitter()).thenReturn(new StringSplitter((cp, style) -> 6F));
        when(font.plainSubstrByWidth(anyString(), anyInt())).thenAnswer(call -> {
            String value = call.getArgument(0);
            int width = call.getArgument(1);
            return value.substring(0, Math.clamp(width / 6, 0, value.length()));
        });
        when(font.plainSubstrByWidth(anyString(), anyInt(), eq(true))).thenAnswer(call -> {
            String value = call.getArgument(0);
            int width = call.getArgument(1);
            return value.substring(Math.max(0, value.length() - width / 6));
        });
        var field = Minecraft.class.getDeclaredField("keyboardHandler");
        field.setAccessible(true);
        field.set(Minecraft.getInstance(), mock(net.minecraft.client.KeyboardHandler.class));
    }

    private void mount(IComposableElement element, int width, int height) {
        this.tree = new UITree();
        ICompositionScope.root(this.tree).e(element, scope -> scope.layout().fixedSize(width, height));
        this.frame(width, height);
    }

    private void frame(int width, int height) {
        for (int pass = 0; pass < 3; pass++) {
            this.tree.prepareFrame(width, height, this.context);
            var constraints = new Constraints(0, width, 0, height);
            this.tree.measureTree(constraints);
            this.tree.placeTree(new Position(0, 0), constraints);
        }
        this.tree.renderTree(this.context);
    }

    private void click(int x, int y, boolean shift) {
        assertTrue(this.tree.dispatchClick(x, y,
                new MouseClickEvent(x, y, InputConstants.MOUSE_BUTTON_LEFT, shift, false, false)));
    }

    private void key(int key, boolean shift, boolean ctrl) {
        assertTrue(this.tree.dispatchKeyPress(new KeyInputEvent(key, key, shift, ctrl, false)));
    }

    @Test
    void inputDecorationDoesNotAlterCustomGeometryAndPointerMatchesCaret() {
        var input = new TextInput("abcd").setContentInsets(new TextInsets(11, 7, 5, 3))
                .setTextAlignment(Alignment.BOTTOM_RIGHT).setCentered(true).setBordered(false);
        this.mount(input, 100, 40);
        assertEquals(new Bounds(11, 7, 84, 30), input.getContentBounds());
        Bounds text = this.tree.findNodesByElementType(Text.class).getFirst().getElement().getBounds();
        assertEquals(71, text.x());
        assertEquals(28, text.y());
        this.click(text.x() + 6, text.y() + 1, false);
        assertEquals(1, input.getCursorPosState().get());
        this.frame(100, 40);
        var caret = this.tree.findNodesByElementType(Rect.class).getFirst().getElement().getBounds();
        assertEquals(text.x() + 6, caret.x());
        this.click(text.x() + 18, text.y() + 1, true);
        assertEquals("bc", input.getSelectedText());
        this.frame(100, 40);
        var selection = this.tree.findNodesByElementType(Rect.class).getFirst().getElement().getBounds();
        assertEquals(text.x() + 6, selection.x());
        assertEquals(12, selection.width());
        input.setBackground(scope -> scope.e(new Rect(0xFF112233), bg -> bg.layout().fillMax()));
        this.frame(100, 40);
        assertEquals(text, this.tree.findNodesByElementType(Text.class).getFirst().getElement().getBounds());
        input.setBordered(false);
        assertTrue(this.tree.dispatchCharTyped(new CharEvent('Z', 0)));
        assertEquals("aZd", input.getValue());
    }

    @Test
    void legacyStatesValidateAndPasteSanitizesSingleLine() {
        var input = new TextInput("a\r\nb");
        assertEquals("ab", input.getValue());
        input.getTextState().set("c\nd\re");
        assertEquals("cde", input.getValue());
        input.getCursorPosState().set(999);
        assertEquals(3, input.getCursorPosState().get());
        input.setFilter(value -> !value.contains("x"));
        input.getTextState().set("x");
        assertEquals("cde", input.getValue());
        this.mount(input, 100, 20);
        this.tree.requestFocus(input.getNode());
        when(Minecraft.getInstance().keyboardHandler.getClipboard()).thenReturn("f\r\ng");
        this.key(InputConstants.KEYCODE_V, false, true);
        assertEquals("cdefg", input.getValue());
        input.getCursorPosState().set(-10);
        assertEquals(0, input.getCursorPosState().get());
        this.frame(100, 20);
    }

    @Test
    void inputScrollUsesCustomWidthAndRevealsAfterResize() {
        var input = new TextInput("abcdefghij").setContentInsets(new TextInsets(10, 2, 10, 2))
                .setTextAlignment(Alignment.TOP_LEFT).setBordered(false);
        this.mount(input, 50, 20);
        this.tree.requestFocus(input.getNode());
        input.moveCursorTo(10, false);
        this.frame(50, 20);
        var caret = this.tree.findNodesByElementType(Rect.class).getFirst().getElement().getBounds();
        assertTrue(caret.right() <= input.getContentBounds().right());
        this.click(10, 3, false);
        assertTrue(input.getCursorPosState().get() > 0);
        input.moveCursorTo(0, false);
        this.frame(50, 20);
        assertEquals(10, this.tree.findNodesByElementType(Rect.class).getFirst().getElement().getBounds().x());
    }

    @Test
    void alignedOverflowKeepsHomeEndPointerAndSelectionInTheViewport() {
        for (var alignment : java.util.List.of(Alignment.TOP_CENTER, Alignment.TOP_RIGHT)) {
            var input = new TextInput("abcdefghij")
                    .setContentInsets(new TextInsets(10, 2, 10, 2))
                    .setTextAlignment(alignment).setBordered(false);
            this.mount(input, 50, 20);
            this.tree.requestFocus(input.getNode());
            this.key(InputConstants.KEY_HOME, false, false);
            this.frame(50, 20);
            var text = this.tree.findNodesByElementType(Text.class).getFirst().getElement().getBounds();
            var caret = this.tree.findNodesByElementType(Rect.class).getFirst().getElement().getBounds();
            assertEquals(10, text.x());
            assertEquals(10, caret.x());
            this.click(16, 3, false);
            assertEquals(1, input.getCursorPosState().get());
            this.click(28, 3, true);
            assertEquals("bc", input.getSelectedText());
            this.frame(50, 20);
            var selection = this.tree.findNodesByElementType(Rect.class).getFirst().getElement().getBounds();
            assertEquals(16, selection.x());
            assertEquals(12, selection.width());
            this.key(InputConstants.KEY_END, false, false);
            this.frame(50, 20);
            caret = this.tree.findNodesByElementType(Rect.class).getFirst().getElement().getBounds();
            assertTrue(caret.x() >= input.getContentBounds().x());
            assertTrue(caret.right() <= input.getContentBounds().right());
            this.click(10, 3, false);
            assertEquals(6, input.getCursorPosState().get());
            this.key(InputConstants.KEY_HOME, false, false);
            this.frame(50, 20);
            assertEquals(10, this.tree.findNodesByElementType(Rect.class).getFirst().getElement().getBounds().x());
            this.tree.reset();
        }
    }

    @Test
    void areaCustomGeometryAlignsPointerSelectionCaretAndScroll() {
        var area = new TextArea().setContentInsets(new TextInsets(9, 5, 7, 4))
                .setTextAlignment(Alignment.BOTTOM_RIGHT).setBackgroundVisible(false);
        area.setValue("abcd\nef");
        this.mount(area, 100, 40);
        assertEquals(new Bounds(9, 5, 84, 31), area.getContentBounds());
        var labels = this.tree.findNodesByElementType(Label.class);
        var text = labels.getFirst().getElement().getBounds();
        assertEquals(69, text.x());
        assertEquals(18, text.y());
        this.click(text.x() + 6, text.y() + 1, false);
        this.frame(100, 40);
        assertEquals(text.x() + 6, this.tree.findNodesByElementType(Rect.class).getFirst().getElement().getBounds().x());
        this.click(text.x() + 18, text.y() + 1, true);
        assertEquals("bc", area.getSelectedText());
        this.frame(100, 40);
        assertEquals(12, this.tree.findNodesByElementType(Rect.class).getFirst().getElement().getBounds().width());
        assertTrue(this.tree.dispatchCharTyped(new CharEvent('Z', 0)));
        assertEquals("aZd\nef", area.getValue());
        area.setValue("0\n1\n2\n3\n4\n5");
        this.frame(100, 40);
        this.key(InputConstants.KEY_END, false, true);
        this.frame(100, 40);
        assertEquals("3", ((Label) this.tree.findNodesByElementType(Label.class).getFirst().getElement()).getText().getString());
        assertTrue(this.tree.dispatchScroll(20, 20, new MouseScrollEvent(20, 20, 1)));
        this.frame(100, 40);
        assertEquals("2", ((Label) this.tree.findNodesByElementType(Label.class).getFirst().getElement()).getText().getString());
        this.frame(100, 40);
        assertEquals("2", ((Label) this.tree.findNodesByElementType(Label.class).getFirst().getElement()).getText().getString());
    }

    @Test
    void formattedTextAndSuggestionsShareAlignedOrigin() {
        var input = new TextInput("ab").setContentInsets(new TextInsets(8, 3, 8, 3))
                .setTextAlignment(Alignment.CENTER).setBordered(false).setSuggestion(" suffix");
        input.addFormatter((value, offset) -> net.minecraft.util.FormattedCharSequence.forward(
                value, net.minecraft.network.chat.Style.EMPTY.withColor(0xFF123456)));
        this.mount(input, 100, 30);
        var texts = this.tree.findNodesByElementType(Text.class);
        var value = texts.getFirst().getElement().getBounds();
        var suggestion = texts.get(1).getElement().getBounds();
        assertEquals(value.y(), suggestion.y());
        assertEquals(value.x() + 12, suggestion.x());
        this.tree.requestFocus(input.getNode());
        this.frame(100, 30);
        var caret = this.tree.findNodesByElementType(Rect.class).getFirst().getElement().getBounds();
        assertEquals(value.x() + 12, caret.x());
        assertEquals(value.y(), caret.y());
    }

    @Test
    void areaAbsoluteCaretSurvivesWrapResizeAndRejectedDeletion() {
        var area = new TextArea().setContentInsets(new TextInsets(6, 2, 6, 2));
        area.setValue("ab\uD83D\uDE42 cd ef gh");
        this.mount(area, 60, 40);
        this.tree.requestFocus(area.getNode());
        this.key(InputConstants.KEY_END, false, true);
        this.key(InputConstants.KEY_LEFT, true, false);
        assertEquals("h", area.getSelectedText());
        area.setContentInsets(new TextInsets(20, 2, 20, 2));
        this.frame(60, 40);
        assertEquals("h", area.getSelectedText());
        area.setFilter(value -> value.endsWith("h"));
        this.key(InputConstants.KEY_BACKSPACE, false, false);
        assertEquals("h", area.getSelectedText());
        area.setFilter(value -> true);
        this.key(InputConstants.KEY_BACKSPACE, false, false);
        assertEquals("ab\uD83D\uDE42 cd ef g", area.getValue());
        this.key(InputConstants.KEY_HOME, false, true);
        this.key(InputConstants.KEY_RIGHT, false, false);
        this.key(InputConstants.KEY_RIGHT, false, false);
        this.key(InputConstants.KEY_DELETE, false, false);
        assertEquals("ab cd ef g", area.getValue());
        this.frame(60, 40);
    }

    @Test
    void numericIncompleteEditsAndCommitsSurviveSharedModel() {
        var number = new NumberInput(2);
        number.setRange(-5, 5);
        this.mount(number, 100, 20);
        var input = number.getTextInput();
        this.tree.requestFocus(input.getNode());
        input.setValue("-");
        assertEquals(2, number.getValue());
        this.key(InputConstants.KEY_RETURN, false, false);
        assertEquals("2.0", input.getValue());
        input.setValue("99");
        assertEquals(5, number.getValue());
        this.tree.clearFocus();
        assertEquals("5.0", input.getValue());
        this.frame(100, 20);
    }
}
