package com.tridevmc.compound.ui.text.core;

import com.tridevmc.compound.ui.text.core.api.ITextEditor;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class TextEditorTest {
    private ITextEditor editor(ITextEditor.Mode mode) {
        return ITextEditor.create(mode, 32, ITextEditor.ReplacementCaret.END, "");
    }

    @Test
    void singleLineSanitizesEveryIngress() {
        var editor = ITextEditor.create(ITextEditor.Mode.SINGLE_LINE, 32,
                ITextEditor.ReplacementCaret.END, "a\r\nb");
        assertEquals("ab", editor.snapshot().text());
        editor.value("c\rd\ne");
        assertEquals("cde", editor.snapshot().text());
        editor.insert("\r\nf\n");
        assertEquals("cdef", editor.snapshot().text());
        var multiline = this.editor(ITextEditor.Mode.MULTILINE);
        multiline.insert("a\nb");
        assertEquals("a\nb", multiline.snapshot().text());
    }

    @Test
    void reversedSelectionAndDeletionAtFullLimit() {
        var editor = this.editor(ITextEditor.Mode.SINGLE_LINE);
        editor.value("abcd");
        editor.limit(4);
        editor.select(4, 1);
        assertEquals("bcd", editor.selectedText());
        editor.insert("");
        assertEquals("a", editor.snapshot().text());
        assertEquals(new ITextEditor.Selection(1, 1), editor.snapshot().selection());
    }

    @Test
    void unicodeOffsetsLimitsAndMutationStayWhole() {
        var editor = this.editor(ITextEditor.Mode.SINGLE_LINE);
        editor.value("a\uD83D\uDE42b");
        editor.moveTo(2, false);
        assertEquals(1, editor.snapshot().selection().caret());
        editor.move(1, false);
        assertEquals(3, editor.snapshot().selection().caret());
        editor.delete(-1, false);
        assertEquals("ab", editor.snapshot().text());
        editor.value("a\uD83D\uDE42b");
        editor.limit(2);
        assertEquals("a", editor.snapshot().text());
        editor.insert("\uD83D\uDE42");
        assertEquals("a", editor.snapshot().text());
        editor.value("\uD83Dx\uDE42");
        assertEquals("x", editor.snapshot().text());
    }

    @Test
    void rejectedEditsPreserveSelectionAndNotificationsSeeFinalState() {
        var editor = this.editor(ITextEditor.Mode.MULTILINE);
        editor.value("123");
        editor.select(3, 1);
        var before = editor.snapshot();
        var values = new ArrayList<String>();
        editor.filter(value -> value.matches("[0-9]*"));
        editor.responder(value -> {
            assertEquals(value, editor.snapshot().text());
            values.add(value);
            if (value.equals("4")) editor.value("5");
        });
        editor.insert("x");
        assertEquals(before, editor.snapshot());
        editor.value("4");
        assertEquals("5", editor.snapshot().text());
        assertEquals(java.util.List.of("4", "5"), values);
    }

    @Test
    void wordMovementAndReplacementPolicy() {
        var editor = ITextEditor.create(ITextEditor.Mode.MULTILINE, 32,
                ITextEditor.ReplacementCaret.START, "one\n two");
        editor.moveTo(8, false);
        assertEquals(5, editor.wordPosition(-1));
        editor.delete(-1, true);
        assertEquals("one\n ", editor.snapshot().text());
        editor.value("reset");
        assertEquals(0, editor.snapshot().selection().caret());
        assertThrows(IllegalArgumentException.class, () -> editor.limit(-1));
    }
}
