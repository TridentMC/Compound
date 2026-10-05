/*
 * Copyright 2018 - 2024 TridentMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridevmc.compound.ui.element;

import com.mojang.blaze3d.platform.InputConstants;

import com.tridevmc.compound.ui.CompoundCursors;
import com.tridevmc.compound.ui.cursor.UICursor;
import com.tridevmc.compound.ui.animation.Easing;
import com.tridevmc.compound.ui.event.CharEvent;
import com.tridevmc.compound.ui.event.KeyInputEvent;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.text.api.TextInsets;
import com.tridevmc.compound.ui.text.core.api.ITextEditor;
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;


import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A multiline text editor with soft wrapping, selection, clipboard support, and vertical
 * scrolling. Length limits and absolute selection offsets use UTF-16 code units.
 */
public class TextArea extends Element implements IComposableElement {

    private static final int DEFAULT_TEXT_COLOR = 0xE0E0E0;
    private static final int DEFAULT_BACKGROUND_COLOR = 0xFF000000;

    /** Presets for caret opacity changes. Custom timing is configured separately. */
    public enum CursorAnimationMode {
        /** Switches between visible and hidden without a fade. */
        INSTANT,
        /** Fades opacity using ease-in-out timing. */
        FADE
    }

    private final ITextEditor editor =
            ITextEditor.create(
                    ITextEditor.Mode.MULTILINE, 500,
                    ITextEditor.ReplacementCaret.START, "");
    private final EditorState<String> text = new EditorState<>(
            () -> this.editor.snapshot().text(), this::setValue);
    private final State<Integer> cursorLine = State.of(0);
    private final State<Integer> cursorColumn = State.of(0);
    private final EditorState<Integer> selectionStart = new EditorState<>(
            () -> this.editor.snapshot().selection().anchor(),
            pos -> this.editor.select(pos, this.editor.snapshot().selection().caret()));
    private final EditorState<Integer> selectionEnd = new EditorState<>(
            () -> this.editor.snapshot().selection().caret(),
            pos -> this.editor.select(this.editor.snapshot().selection().anchor(), pos));
    private final State<Integer> scrollLine = State.of(0);
    private final State<Boolean> focused = State.of(false);

    private static final int SELECTION_COLOR = 0x800000FF;

    private int maxLength = 500;
    private boolean editable = true;
    private int textColor = DEFAULT_TEXT_COLOR;
    private int backgroundColor = DEFAULT_BACKGROUND_COLOR;
    private TextInsets contentInsets =
            new TextInsets(4, 4, 4, 4);
    private Alignment textAlignment = Alignment.TOP_LEFT;
    private boolean backgroundVisible = true;
    private Consumer<ICompositionScope> background = scope -> scope.e(
            new Surface(() -> this.backgroundColor), bg -> bg.layout().fillMax());
    private Component hint;
    private int lineHeight;
    private record DisplayLine(int start, String text) { }

    private final List<DisplayLine> lines = new ArrayList<>();
    private int wrappedWidth = -1;
    private String wrappedText = "";

    private final CursorBlink cursorBlink = new CursorBlink();
    private CursorAnimationMode cursorAnimationMode = CursorAnimationMode.INSTANT;
    private int composedVisibleLines;
    private int placedHeight = -1;

    /**
     * Creates an empty editable field with a 500-code-unit length limit.
     */
    public TextArea() {
        this.updateLines();
        this.editor.observe(() -> {
            if (!this.text.get().equals(this.wrappedText)) this.updateLines();
            this.text.refresh();
            this.projectCursor(this.editor.snapshot().selection().caret());
            this.selectionStart.refresh();
            this.selectionEnd.refresh();
            this.scrollToCursor();
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compose(ICompositionScope scope) {
        this.composedVisibleLines = this.getVisibleLines();
        scope.bind(this.text);
        scope.bind(this.cursorLine);
        scope.bind(this.cursorColumn);
        scope.bind(this.selectionStart);
        scope.bind(this.selectionEnd);
        scope.bind(this.scrollLine);
        scope.bind(this.focused);

        this.cursorBlink.compose(scope);

        scope.onClick(event -> {
            if (!this.editable || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
            scope.requestFocus();
            this.updateCursorFromPosition(event.x(), event.y(), event.shiftDown());
            return true;
        });

        scope.onKeyPress(this::handleKeyPress);
        scope.onCharTyped(this::handleCharTyped);

        scope.onMouseDrag(event -> {
            if (!this.editable || !this.focused.get()) return false;
            this.updateCursorFromPosition(event.x(), event.y(), true);
            return true;
        });

        scope.onFocusGained(() -> {
            this.focused.set(true);
            Minecraft.getInstance().textInputManager().onTextInputFocusChange(this, this.editable);
            this.cursorBlink.reset();
        });
        scope.onFocusLost(() -> {
            this.focused.set(false);
            Minecraft.getInstance().textInputManager().onTextInputFocusChange(this, false);
        });

        scope.onScrollWhenFocused(event -> {
            if (this.lines.size() <= this.getVisibleLines()) return false;
            int newScroll = this.scrollLine.get() - (int) event.scrollY();
            newScroll = Math.max(0, Math.min(newScroll, Math.max(0, this.lines.size() - this.getVisibleLines())));
            if (newScroll == this.scrollLine.get()) return false;
            this.scrollLine.set(newScroll);
            return true;
        });

        scope.e(new Stack(), stack -> {
            stack.layout().fillMax();

            if (this.backgroundVisible) this.background.accept(stack);

            stack.e(new Box(), contentBox -> {
                contentBox.layout().padding(this.contentInsets.left(), this.contentInsets.top(),
                        this.contentInsets.right(), this.contentInsets.bottom()).fillMax().clip();

                contentBox.e(new Stack(), textStack -> {
                    textStack.layout().fillMax();

                    var font = Minecraft.getInstance().font;
                    this.lineHeight = font.lineHeight;

                    int startLine = this.scrollLine.get();
                    int visibleLines = this.getVisibleLines();
                    if (this.text.get().isEmpty() && this.hint != null && !this.focused.get()) {
                        textStack.e(new Label(this.hint, 0x808080, false).setWrap(true),
                                label -> label.layout().fillMaxWidth());
                    }

                    this.composeSelection(textStack, startLine, visibleLines);

                    for (int i = 0; i < visibleLines && startLine + i < this.lines.size(); i++) {
                        final int lineIndex = startLine + i;
                        final int lineOffset = i;
                        String line = this.lines.get(lineIndex).text();

                        textStack.e(new Label(
                                Component.literal(line),
                                () -> this.editable ? this.textColor : 0x808080,
                                () -> false
                        ), label -> label.layout().margin(this.lineOrigin(lineIndex).x(),
                                this.lineOrigin(lineIndex).y() + lineOffset * this.lineHeight, 0, 0));
                    }

                    if (this.focused.get() && this.editable) {
                        int cursorX = this.getCursorX();
                        int cursorY = (this.cursorLine.get() - startLine) * this.lineHeight;

                        if (cursorY >= 0 && cursorY < visibleLines * this.lineHeight) {
                            textStack.e(new Rect(this.cursorBlink::color), cursor -> cursor.layout()
                                    .fixedSize(1, this.lineHeight).margin(
                                            cursorX + this.lineOrigin(this.cursorLine.get()).x(),
                                            cursorY + this.lineOrigin(this.cursorLine.get()).y(), 0, 0));
                        }
                    }
                });
            });
        });
    }

    private void composeSelection(ICompositionScope scope, int startLine, int visibleLines) {
        if (!this.hasSelection()) return;
        var font = Minecraft.getInstance().font;
        for (int row = 0; row < visibleLines && startLine + row < this.lines.size(); row++) {
            var line = this.lines.get(startLine + row);
            int start = Math.max(this.getSelectionMin(), line.start()) - line.start();
            int end = Math.min(this.getSelectionMax() - line.start(), line.text().length());
            if (start >= end) continue;
            int x = this.lineOrigin(startLine + row).x() + font.width(line.text().substring(0, start));
            int width = font.width(line.text().substring(start, end));
            int y = this.lineOrigin(startLine + row).y() + row * this.lineHeight;
            scope.e(new Rect(SELECTION_COLOR), selection -> selection.layout()
                    .fixedSize(width, this.lineHeight).margin(x, y, 0, 0));
        }
    }

    private boolean handleKeyPress(KeyInputEvent event) {
        if (!this.focused.get() || !this.editable) return false;

        int keyCode = event.keyCode();
        boolean shift = event.shiftDown();
        boolean ctrl = event.ctrlDown();

        if (event.isShortcut(InputConstants.KEYCODE_A)) {
            this.selectAll();
            return true;
        }
        if (event.isShortcut(InputConstants.KEYCODE_C)) {
            this.copyToClipboard();
            return true;
        }
        if (event.isShortcut(InputConstants.KEYCODE_V)) {
            this.pasteFromClipboard();
            return true;
        }
        if (event.isShortcut(InputConstants.KEYCODE_X)) {
            this.copyToClipboard();
            if (this.hasSelection()) this.insertText("");
            return true;
        }


        if (keyCode == InputConstants.KEY_LEFT || keyCode == InputConstants.KEY_RIGHT
                || keyCode == InputConstants.KEY_UP || keyCode == InputConstants.KEY_DOWN
                || keyCode == InputConstants.KEY_HOME || keyCode == InputConstants.KEY_END
                || keyCode == InputConstants.KEY_PAGEUP || keyCode == InputConstants.KEY_PAGEDOWN) {
            int anchor = this.hasSelection() ? this.selectionStart.get() : this.getCursorPosition();
            switch (keyCode) {
                case InputConstants.KEY_LEFT -> {
                    if (!shift && this.hasSelection()) this.setCursorPosition(this.getSelectionMin());
                    else if (ctrl) this.setCursorPosition(this.wordPosition(-1));
                    else this.moveCursor(-1, 0);
                }
                case InputConstants.KEY_RIGHT -> {
                    if (!shift && this.hasSelection()) this.setCursorPosition(this.getSelectionMax());
                    else if (ctrl) this.setCursorPosition(this.wordPosition(1));
                    else this.moveCursor(1, 0);
                }
                case InputConstants.KEY_UP -> this.moveCursor(0, -1);
                case InputConstants.KEY_DOWN -> this.moveCursor(0, 1);
                case InputConstants.KEY_PAGEUP -> this.moveCursor(0, -this.getVisibleLines());
                case InputConstants.KEY_PAGEDOWN -> this.moveCursor(0, this.getVisibleLines());
                case InputConstants.KEY_HOME -> this.setCursorPosition(ctrl ? 0 : this.getLineStart(this.cursorLine.get()));
                case InputConstants.KEY_END -> this.setCursorPosition(ctrl ? this.text.get().length()
                        : this.getLineStart(this.cursorLine.get()) + this.lines.get(this.cursorLine.get()).text().length());
                default -> { }
            }
            if (shift) this.setSelectionRange(anchor, this.getCursorPosition());
            else this.clearSelection();
            this.scrollToCursor();
            return true;
        }

        return switch (keyCode) {
            case InputConstants.KEY_BACKSPACE -> {
                this.editor.delete(-1, ctrl);
                yield true;
            }
            case InputConstants.KEY_DELETE -> {
                this.editor.delete(1, ctrl);
                yield true;
            }
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                this.insertText("\n");
                yield true;
            }
            default -> false;
        };
    }

    private boolean handleCharTyped(CharEvent event) {
        if (!this.focused.get() || !this.editable) return false;
        if (!TextEditing.isPrintable(event.codePoint())) return false;
        this.insertText(new String(Character.toChars(event.codePoint())));
        return true;
    }

    public void insertText(String textToInsert) {
        this.editor.select(this.hasSelection() ? this.selectionStart.get() : this.getCursorPosition(),
                this.hasSelection() ? this.selectionEnd.get() : this.getCursorPosition());
        this.editor.insert(textToInsert);
    }

    private void setCursorPosition(int position) {
        this.editor.moveTo(position, true);
    }

    private void projectCursor(int position) {
        int pos = Math.clamp(position, 0, this.text.get().length());
        int line = Math.clamp(this.cursorLine.get(), 0, this.lines.size() - 1);
        var currentLine = this.lines.get(line);
        // Adjacent soft-wrapped rows share a character offset; preserve the current row there.
        if (pos < currentLine.start() || pos > currentLine.start() + currentLine.text().length()) {
            line = this.lines.size() - 1;
            for (int i = 0; i < this.lines.size(); i++) {
                var view = this.lines.get(i);
                if (pos <= view.start() + view.text().length()) {
                    line = i;
                    break;
                }
            }
        }
        this.cursorLine.set(line);
        this.cursorColumn.set(Math.clamp(pos - this.lines.get(line).start(), 0, this.lines.get(line).text().length()));
        this.cursorBlink.reset();
    }

    private void moveCursor(int deltaX, int deltaY) {
        if (deltaY == 0) {
            this.editor.move(deltaX, true);
        } else {
            int line = Math.clamp(this.cursorLine.get() + deltaY, 0, this.lines.size() - 1);
            var font = Minecraft.getInstance().font;
            int column = font.plainSubstrByWidth(this.lines.get(line).text(), this.getCursorX()).length();
            this.cursorLine.set(line);
            this.setCursorPosition(this.getPositionForLineColumn(line, column));
        }
        this.scrollToCursor();
        this.cursorBlink.reset();
    }

    private int wordPosition(int direction) {
        return this.editor.wordPosition(direction);
    }

    private int getCursorPosition() {
        return this.editor.snapshot().selection().caret();
    }

    private int getPositionForLineColumn(int line, int column) {
        var view = this.lines.get(Math.clamp(line, 0, this.lines.size() - 1));
        return view.start() + Math.clamp(column, 0, view.text().length());
    }

    private int getLineStart(int lineIndex) {
        return this.lines.get(lineIndex).start();
    }

    private int getCursorX() {
        var font = Minecraft.getInstance().font;
        if (this.cursorLine.get() >= this.lines.size()) return 0;
        String line = this.lines.get(this.cursorLine.get()).text();
        int col = Math.min(this.cursorColumn.get(), line.length());
        return font.width(line.substring(0, col));
    }

    private void updateCursorFromPosition(int mouseX, int mouseY, boolean shiftDown) {
        Bounds bounds = this.getBounds();
        if (bounds == null) return;

        Bounds content = this.getContentBounds();
        int relativeY = mouseY - content.y() - this.lineOrigin(this.scrollLine.get()).y();
        int line = this.scrollLine.get() + Math.floorDiv(relativeY, Math.max(1, this.lineHeight));
        line = Mth.clamp(line, 0, Math.max(0, this.lines.size() - 1));
        int relativeX = mouseX - content.x() - this.lineOrigin(line).x();

        var font = Minecraft.getInstance().font;
        String textLine = this.lines.get(line).text();
        int column = font.plainSubstrByWidth(textLine, Math.max(0, relativeX)).length();

        this.cursorLine.set(line);
        this.cursorColumn.set(column);
        this.scrollToCursor();
        this.cursorBlink.reset();

        int cursorPos = this.getPositionForLineColumn(line, column);
        this.editor.select(shiftDown ? this.selectionStart.get() : cursorPos, cursorPos);
    }

    private void setSelectionRange(int start, int end) {
        this.editor.select(start, end);
    }

    private void clearSelection() {
        int pos = this.getCursorPosition();
        this.editor.select(pos, pos);
    }

    private boolean hasSelection() {
        return !this.selectionStart.get().equals(this.selectionEnd.get());
    }

    private int getSelectionMin() {
        return Math.min(this.selectionStart.get(), this.selectionEnd.get());
    }

    private int getSelectionMax() {
        return Math.max(this.selectionStart.get(), this.selectionEnd.get());
    }

    private void scrollToCursor() {
        int visibleLines = this.getVisibleLines();
        int currentScroll = this.scrollLine.get();
        int cursorLine = this.cursorLine.get();

        if (cursorLine < currentScroll) {
            this.scrollLine.set(cursorLine);
        } else if (cursorLine >= currentScroll + visibleLines) {
            this.scrollLine.set(cursorLine - visibleLines + 1);
        }
    }

    private int getVisibleLines() {
        Bounds bounds = this.getBounds();
        if (bounds == null || this.lineHeight == 0) return 1;
        return Math.max(1, this.getContentBounds().height() / this.lineHeight);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFocusable() {
        return this.editable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDetached() {
        this.cursorBlink.detach();
        this.focused.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getNarrationMessage() {
        return Component.translatable("gui.narrate.editBox", this.hint != null ? this.hint : Component.empty(), this.text.get());
    }

    private void updateLines() {
        this.lines.clear();
        String currentText = this.text.get();
        this.wrappedText = currentText;
        int width = this.getContentBounds().width();
        this.wrappedWidth = width;
        if (currentText.isEmpty()) {
            this.lines.add(new DisplayLine(0, ""));
            return;
        }
        // Vanilla's splitter supplies original-string offsets, including skipped wrap spaces/newlines.
        Minecraft.getInstance().font.getSplitter().splitLines(currentText,
                width > 0 ? width : Integer.MAX_VALUE, Style.EMPTY, false,
                (style, start, end) -> this.lines.add(new DisplayLine(start, currentText.substring(start, end))));
        if (currentText.endsWith("\n")) {
            this.lines.add(new DisplayLine(currentText.length(), ""));
        }
    }

    private void copyToClipboard() {
        String selected = this.getSelectedText();
        if (!selected.isEmpty()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(selected);
        }
    }

    private void pasteFromClipboard() {
        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clipboard != null && !clipboard.isEmpty()) {
            this.insertText(clipboard);
        }
    }

    public String getSelectedText() {
        return this.editor.selectedText();
    }

    private void selectAll() {
        this.editor.select(0, this.text.get().length());
    }

    /**
     * Returns the current unformatted text.
     *
     * @return the current text
     */
    public String getValue() {
        return this.text.get();
    }

    /**
     * Replaces accepted text, applies the length limit, and resets the caret, selection, and scroll to the start.
     *
     * @param value the proposed non-null text; rejected values leave the field unchanged
     */
    public void setValue(String value) {
        this.editor.value(value);
    }

    /**
     * Sets the UTF-16 length limit. Existing text is truncated without splitting a surrogate pair and the responder is notified.
     *
     * @param maxLength the nonnegative maximum number of UTF-16 code units
     * @throws IllegalArgumentException if maxLength is negative
     */
    public void setMaxLength(int maxLength) {
        if (maxLength < 0) throw new IllegalArgumentException("Maximum text length must be nonnegative");
        this.maxLength = maxLength;
        this.editor.limit(maxLength);
    }

    /**
     * Controls editing and keyboard focus eligibility; programmatic value changes remain available.
     *
     * @param editable whether to accept user edits
     */
    public void setEditable(boolean editable) {
        if (this.focused.get()) {
            Minecraft.getInstance().textInputManager().onTextInputFocusChange(this, editable);
        }
        this.editable = editable;
        this.invalidateComposition();
    }

    /**
     * Sets the text color.
     *
     * @param color the drawing color
     */
    public void setTextColor(int color) {
        this.textColor = color;
    }

    /**
     * Sets the background ARGB color.
     *
     * @param color the drawing color
     */
    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    public Bounds getContentBounds() {
        Bounds bounds = this.getBounds();
        return this.contentInsets.apply(bounds == null ? new Bounds(0, 0, 200, 100) : bounds);
    }

    public TextArea setContentInsets(TextInsets insets) {
        this.contentInsets = Objects.requireNonNull(insets);
        this.updateLines();
        this.projectCursor(this.getCursorPosition());
        this.scrollLine.set(Math.clamp(this.scrollLine.get(), 0,
                Math.max(0, this.lines.size() - this.getVisibleLines())));
        this.scrollToCursor();
        this.invalidateComposition();
        return this;
    }

    public TextArea setTextAlignment(Alignment alignment) {
        this.textAlignment = Objects.requireNonNull(alignment);
        this.invalidateComposition();
        return this;
    }

    public TextArea setBackground(Consumer<ICompositionScope> background) {
        this.background = Objects.requireNonNull(background);
        this.backgroundVisible = true;
        this.invalidateComposition();
        return this;
    }

    public TextArea setBackgroundVisible(boolean visible) {
        this.backgroundVisible = visible;
        this.invalidateComposition();
        return this;
    }

    private Position lineOrigin(int line) {
        int count = Math.min(this.getVisibleLines(), this.lines.size() - this.scrollLine.get());
        int width = Minecraft.getInstance().font.width(this.lines.get(line).text());
        return this.textAlignment.align(new Size(width, count * this.lineHeight),
                new Size(this.getContentBounds().width(), this.getContentBounds().height()));
    }

    /**
     * Sets the predicate that accepts or rejects proposed complete text values.
     *
     * @param filter the non-null predicate for proposed complete values
     */
    public void setFilter(Predicate<String> filter) {
        this.editor.filter(filter);
    }

    /**
     * Sets the callback notified after accepted value changes.
     *
     * @param responder the callback receiving accepted values, or null to disable notifications
     */
    public void setResponder(Consumer<String> responder) {
        this.editor.responder(responder);
    }

    /**
     * Sets the placeholder displayed while the field is empty.
     *
     * @param hint the placeholder, or null to clear it
     */
    public void setHint(Component hint) {
        this.hint = hint;
        this.invalidateComposition();
    }

    /**
     * Applies a 300-millisecond caret animation using step or ease-in-out timing.
     *
     * @param mode the non-null caret animation preset
     * @return this element
     */
    public TextArea setCursorAnimationMode(CursorAnimationMode mode) {
        this.cursorAnimationMode = Objects.requireNonNull(mode);
        return this.setCursorAnimation(300, mode == CursorAnimationMode.INSTANT ? Easing.STEP : Easing.EASE_IN_OUT);
    }

    /**
     * Configures the looping caret opacity animation and restarts it when already mounted.
     *
     * @param intervalMillis the positive duration of each fade direction in milliseconds
     * @param easing the non-null easing function
     * @return this element
     * @throws IllegalArgumentException if intervalMillis is not positive
     */
    public TextArea setCursorAnimation(long intervalMillis, Easing easing) {
        this.cursorBlink.configure(intervalMillis, easing);
        this.cursorAnimationMode = easing == Easing.STEP ? CursorAnimationMode.INSTANT : CursorAnimationMode.FADE;
        this.invalidateComposition();
        return this;
    }

    /**
     * Returns the selected caret animation preset.
     *
     * @return the current preset
     */
    public CursorAnimationMode getCursorAnimationMode() {
        return this.cursorAnimationMode;
    }

    /**
     * Returns whether user editing is enabled.
     *
     * @return true when user editing is enabled
     */
    public boolean isEditable() {
        return this.editable;
    }

    /**
     * Returns whether the tree has focused this editor.
     *
     * @return true when this editor has focus
     */
    public boolean isFocused() {
        return this.focused.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(
                Math.min(200, constraints.maxWidth()),
                Math.min(100, constraints.maxHeight())
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        boolean widthChanged = this.wrappedWidth != this.contentInsets.apply(bounds).width();
        boolean heightChanged = this.placedHeight != bounds.height();
        this.placedHeight = bounds.height();
        if (widthChanged) {
            int cursor = this.getCursorPosition();
            this.updateLines();
            this.setCursorPosition(cursor);
        }
        this.scrollLine.set(Math.clamp(this.scrollLine.get(), 0, Math.max(0, this.lines.size() - this.getVisibleLines())));
        // Ordinary layout passes must preserve deliberate wheel scrolling away from the caret.
        if (this.focused.get() && (widthChanged || heightChanged)) {
            this.scrollToCursor();
        }
        if (widthChanged || this.composedVisibleLines != this.getVisibleLines()) {
            this.invalidateComposition();
        }
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UICursor getCursor(int x, int y) {
        return this.editable ? CompoundCursors.IBEAM : null;
    }
}
