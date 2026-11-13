package com.tridevmc.compound.ui.text.core.api;

import com.tridevmc.compound.ui.text.core.internal.TextEditor;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Java-only text editing with absolute UTF-16 offsets snapped to code-point boundaries. */
public interface ITextEditor {
    enum Mode { SINGLE_LINE, MULTILINE }
    enum ReplacementCaret { START, END }
    record Selection(int anchor, int caret) {
        public int start() { return Math.min(anchor, caret); }
        public int end() { return Math.max(anchor, caret); }
    }
    record Snapshot(String text, Selection selection) { }

    static ITextEditor create(Mode mode, int limit, ReplacementCaret replacementCaret,
                              String initialValue) {
        return new TextEditor(mode, limit, replacementCaret, initialValue);
    }

    /** Returns immutable text and selection; offsets are UTF-16 code units. */
    Snapshot snapshot();
    /** Sanitizes before validation, truncates to the limit, and applies the replacement caret policy. */
    void value(String value);
    void insert(String value);
    void delete(int direction, boolean byWord);
    void select(int anchor, int caret);
    void moveTo(int position, boolean select);
    void move(int codePoints, boolean select);
    int wordPosition(int direction);
    String selectedText();
    void limit(int limit);
    void filter(Predicate<String> filter);
    void responder(Consumer<String> responder);
    void observe(Runnable observer);
}
