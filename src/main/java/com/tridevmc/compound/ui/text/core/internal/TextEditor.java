package com.tridevmc.compound.ui.text.core.internal;

import com.tridevmc.compound.ui.text.core.api.ITextEditor;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class TextEditor implements ITextEditor {
    private final Mode mode;
    private final ReplacementCaret replacementCaret;
    private String text;
    private Selection selection;
    private int limit;
    private Predicate<String> filter = value -> true;
    private Consumer<String> responder = value -> { };
    private Runnable observer = () -> { };

    public TextEditor(Mode mode, int limit, ReplacementCaret replacementCaret, String initial) {
        this.mode = Objects.requireNonNull(mode);
        this.replacementCaret = Objects.requireNonNull(replacementCaret);
        if (limit < 0) throw new IllegalArgumentException("Maximum text length must be nonnegative");
        this.limit = limit;
        this.text = this.sanitize(initial == null ? "" : initial, false);
        int caret = replacementCaret == ReplacementCaret.END ? this.text.length() : 0;
        this.selection = new Selection(caret, caret);
    }

    @Override
    public Snapshot snapshot() { return new Snapshot(this.text, this.selection); }

    private String sanitize(String value, boolean insertion) {
        Objects.requireNonNull(value);
        StringBuilder result = new StringBuilder();
        value.codePoints().forEach(cp -> {
            if (cp >= 0xD800 && cp <= 0xDFFF) return;
            if (this.mode == Mode.SINGLE_LINE && (cp == '\r' || cp == '\n')) return;
            if (insertion && cp != '\n' && (cp < 32 || cp == 127 || cp == 167)) return;
            result.appendCodePoint(cp);
        });
        return result.toString();
    }

    private int boundary(int offset) {
        int end = Math.clamp(offset, 0, this.text.length());
        if (end > 0 && end < this.text.length() && Character.isLowSurrogate(this.text.charAt(end))) end--;
        return end;
    }

    private static String truncate(String value, int limit) {
        int end = Math.clamp(limit, 0, value.length());
        if (end > 0 && end < value.length() && Character.isLowSurrogate(value.charAt(end))) end--;
        return value.substring(0, end);
    }

    private void apply(String value, int caret) {
        this.text = value;
        int position = this.boundary(caret);
        this.selection = new Selection(position, position);
        this.observer.run();
        this.responder.accept(value);
    }

    @Override
    public void value(String value) {
        String clean = this.sanitize(value, false);
        if (!this.filter.test(clean)) return;
        clean = truncate(clean, this.limit);
        this.apply(clean, this.replacementCaret == ReplacementCaret.END ? clean.length() : 0);
    }

    @Override
    public void insert(String value) {
        int start = this.selection.start();
        int end = this.selection.end();
        String clean = truncate(this.sanitize(value, true), Math.max(0, this.limit - this.text.length() + end - start));
        String proposed = this.text.substring(0, start) + clean + this.text.substring(end);
        if (this.filter.test(proposed)) this.apply(proposed, start + clean.length());
    }

    @Override
    public void delete(int direction, boolean byWord) {
        if (this.selection.start() != this.selection.end()) { this.insert(""); return; }
        int caret = this.selection.caret();
        int end = byWord ? this.wordPosition(direction) : this.offset(direction);
        if (end == caret) return;
        String proposed = this.text.substring(0, Math.min(caret, end)) + this.text.substring(Math.max(caret, end));
        if (this.filter.test(proposed)) this.apply(proposed, Math.min(caret, end));
    }

    private int offset(int delta) {
        int caret = this.selection.caret();
        int count = delta < 0 ? this.text.codePointCount(0, caret) : this.text.codePointCount(caret, this.text.length());
        return this.text.offsetByCodePoints(caret, Math.clamp(delta, -count, count));
    }

    @Override
    public void select(int anchor, int caret) {
        this.selection = new Selection(this.boundary(anchor), this.boundary(caret));
        this.observer.run();
    }

    @Override
    public void moveTo(int position, boolean select) {
        this.select(select ? this.selection.anchor() : position, position);
    }

    @Override
    public void move(int codePoints, boolean select) { this.moveTo(this.offset(codePoints), select); }

    @Override
    public int wordPosition(int direction) {
        int position = this.selection.caret();
        for (int word = 0; word < Math.abs(direction); word++) {
            if (direction < 0) {
                while (position > 0 && Character.isWhitespace(this.text.codePointBefore(position))) {
                    position = this.text.offsetByCodePoints(position, -1);
                }
                while (position > 0 && !Character.isWhitespace(this.text.codePointBefore(position))) {
                    position = this.text.offsetByCodePoints(position, -1);
                }
            } else {
                while (position < this.text.length()
                        && !Character.isWhitespace(this.text.codePointAt(position))) {
                    position = this.text.offsetByCodePoints(position, 1);
                }
                while (position < this.text.length()
                        && Character.isWhitespace(this.text.codePointAt(position))) {
                    position = this.text.offsetByCodePoints(position, 1);
                }
            }
        }
        return position;
    }

    @Override
    public String selectedText() { return this.text.substring(this.selection.start(), this.selection.end()); }

    @Override
    public void limit(int limit) {
        if (limit < 0) throw new IllegalArgumentException("Maximum text length must be nonnegative");
        this.limit = limit;
        if (this.text.length() > limit) {
            String truncated = truncate(this.text, limit);
            this.apply(truncated, truncated.length());
        }
    }

    @Override
    public void filter(Predicate<String> filter) { this.filter = Objects.requireNonNull(filter); }
    @Override
    public void responder(Consumer<String> responder) { this.responder = responder == null ? value -> { } : responder; }
    @Override
    public void observe(Runnable observer) { this.observer = Objects.requireNonNull(observer); }
}
