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

import com.tridevmc.compound.ui.CompoundCursors;
import com.tridevmc.compound.ui.cursor.UICursor;
import com.tridevmc.compound.ui.animation.Easing;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A multi-line text input field for entering larger amounts of text.
 * Supports scrolling, line wrapping, and basic text editing.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new TextArea(), area -> {
 *     area.layout().fixedSize(200, 100);
 *     area.getElement().setMaxLength(500);
 *     area.getElement().setResponder(text -> System.out.println("Text: " + text));
 * });
 * </pre>
 */
public class TextArea extends BaseElement implements IComposableElement {

    private static final int DEFAULT_TEXT_COLOR = 0xE0E0E0;
    private static final int DEFAULT_BACKGROUND_COLOR = 0xFF000000;

    /**
     * Defines how the cursor blink animation should behave.
     */
    public enum CursorAnimationMode {
        /**
         * Cursor instantly toggles between fully visible and invisible.
         */
        INSTANT,
        /**
         * Cursor smoothly fades in and out using opacity interpolation.
         */
        FADE
    }

    private final State<String> text = new StateImpl<>("");
    private final State<Integer> cursorLine = new StateImpl<>(0);
    private final State<Integer> cursorColumn = new StateImpl<>(0);
    private final State<Integer> selectionStart = new StateImpl<>(0);
    private final State<Integer> selectionEnd = new StateImpl<>(0);
    private final State<Integer> scrollLine = new StateImpl<>(0);
    private final State<Boolean> focused = new StateImpl<>(false);

    private static final int SELECTION_COLOR = 0x800000FF;

    private int maxLength = 500;
    private boolean editable = true;
    private int textColor = DEFAULT_TEXT_COLOR;
    private int backgroundColor = DEFAULT_BACKGROUND_COLOR;
    private Predicate<String> filter = s -> s != null;
    private Consumer<String> responder;
    private Component hint;
    private int lineHeight;
    private record DisplayLine(int start, String text) { }

    private final List<DisplayLine> lines = new ArrayList<>();
    private int wrappedWidth = -1;

    // Cursor animation state
    private final CursorBlink cursorBlink = new CursorBlink();
    private CursorAnimationMode cursorAnimationMode = CursorAnimationMode.INSTANT;
    private int composedVisibleLines;

    public TextArea() {
        this.updateLines();
    }

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

        // Initialize cursor blink animation based on selected mode
        this.cursorBlink.compose(scope);

        scope.onClick(event -> {
            if (!this.editable || event.button() != 0) return false;
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
            this.cursorBlink.reset();
        });
        scope.onFocusLost(() -> this.focused.set(false));

        scope.onScrollWhenFocused(event -> {
            if (this.lines.size() <= this.getVisibleLines()) return false;
            int newScroll = this.scrollLine.get() - (int) event.scrollDelta();
            newScroll = Math.max(0, Math.min(newScroll, Math.max(0, this.lines.size() - this.getVisibleLines())));
            this.scrollLine.set(newScroll);
            return true;
        });

        scope.e(new Stack(), stack -> {
            stack.layout().fillMax();

            stack.e(new Surface(() -> this.backgroundColor), bg -> bg.layout().fillMax());

            stack.e(new Box(), contentBox -> {
                contentBox.layout().padding(4).fillMax().clip();

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
                        ), label -> label.layout().margin(0, lineOffset * this.lineHeight, 0, 0));
                    }

                    // Cursor bar with blink animation - composed whenever focused so the
                    // color supplier can make it blink without requiring recomposition.
                    if (this.focused.get() && this.editable) {
                        int cursorX = this.getCursorX();
                        int cursorY = (this.cursorLine.get() - startLine) * this.lineHeight;

                        if (cursorY >= 0 && cursorY < visibleLines * this.lineHeight) {
                            textStack.e(new Rect(this.cursorBlink::color), cursor -> cursor.layout()
                                    .fixedSize(1, this.lineHeight).margin(cursorX, cursorY, 0, 0));
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
            int x = font.width(line.text().substring(0, start));
            int width = font.width(line.text().substring(start, end));
            int y = row * this.lineHeight;
            scope.e(new Rect(SELECTION_COLOR), selection -> selection.layout()
                    .fixedSize(width, this.lineHeight).margin(x, y, 0, 0));
        }
    }

    private boolean handleKeyPress(com.tridevmc.compound.ui.event.KeyInputEvent event) {
        if (!this.focused.get() || !this.editable) return false;

        int keyCode = event.keyCode();
        boolean shift = event.shiftDown();
        boolean ctrl = event.ctrlDown();

        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT
                || keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN
                || keyCode == GLFW.GLFW_KEY_HOME || keyCode == GLFW.GLFW_KEY_END
                || keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            int anchor = this.hasSelection() ? this.selectionStart.get() : this.getCursorPosition();
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> {
                    if (!shift && this.hasSelection()) this.setCursorPosition(this.getSelectionMin());
                    else if (ctrl) this.setCursorPosition(this.wordPosition(-1));
                    else this.moveCursor(-1, 0);
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    if (!shift && this.hasSelection()) this.setCursorPosition(this.getSelectionMax());
                    else if (ctrl) this.setCursorPosition(this.wordPosition(1));
                    else this.moveCursor(1, 0);
                }
                case GLFW.GLFW_KEY_UP -> this.moveCursor(0, -1);
                case GLFW.GLFW_KEY_DOWN -> this.moveCursor(0, 1);
                case GLFW.GLFW_KEY_PAGE_UP -> this.moveCursor(0, -this.getVisibleLines());
                case GLFW.GLFW_KEY_PAGE_DOWN -> this.moveCursor(0, this.getVisibleLines());
                case GLFW.GLFW_KEY_HOME -> this.setCursorPosition(ctrl ? 0 : this.getLineStart(this.cursorLine.get()));
                case GLFW.GLFW_KEY_END -> this.setCursorPosition(ctrl ? this.text.get().length()
                        : this.getLineStart(this.cursorLine.get()) + this.lines.get(this.cursorLine.get()).text().length());
                default -> { }
            }
            if (shift) this.setSelectionRange(anchor, this.getCursorPosition());
            else this.clearSelection();
            this.scrollToCursor();
            return true;
        }

        return switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (ctrl && !this.hasSelection()) this.setSelectionRange(this.wordPosition(-1), this.getCursorPosition());
                this.deleteChar(-1);
                yield true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (ctrl && !this.hasSelection()) this.setSelectionRange(this.getCursorPosition(), this.wordPosition(1));
                this.deleteChar(1);
                yield true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                this.insertText("\n");
                yield true;
            }
            case GLFW.GLFW_KEY_A -> {
                if (ctrl) {
                    this.selectAll();
                    yield true;
                }
                yield false;
            }
            case GLFW.GLFW_KEY_C -> {
                if (ctrl) {
                    this.copyToClipboard();
                    yield true;
                }
                yield false;
            }
            case GLFW.GLFW_KEY_V -> {
                if (ctrl) {
                    this.pasteFromClipboard();
                    yield true;
                }
                yield false;
            }
            case GLFW.GLFW_KEY_X -> {
                if (!ctrl) yield false;
                this.copyToClipboard();
                if (this.hasSelection()) this.insertText("");
                yield true;
            }
            default -> false;
        };
    }

    private boolean handleCharTyped(com.tridevmc.compound.ui.event.CharEvent event) {
        if (!this.focused.get() || !this.editable) return false;
        this.insertText(String.valueOf(event.character()));
        return true;
    }

    private void insertText(String textToInsert) {
        String currentText = this.text.get();

        int insertPos;
        if (this.hasSelection()) {
            int selMin = this.getSelectionMin();
            int selMax = this.getSelectionMax();
            currentText = currentText.substring(0, selMin) + currentText.substring(selMax);
            insertPos = selMin;
        } else {
            insertPos = this.getCursorPosition();
        }

        if (currentText.length() + textToInsert.length() > this.maxLength) {
            textToInsert = textToInsert.substring(0, this.maxLength - currentText.length());
        }

        String newText = currentText.substring(0, insertPos) + textToInsert + currentText.substring(insertPos);

        if (this.filter.test(newText)) {
            this.text.set(newText);
            this.updateLines();
            this.setCursorPosition(insertPos + textToInsert.length());
            this.clearSelection();
            this.scrollToCursor();
            this.onValueChanged();
        }
    }

    private void setCursorPosition(int position) {
        int pos = Math.clamp(position, 0, this.text.get().length());
        int line = this.lines.size() - 1;
        for (int i = 0; i < this.lines.size(); i++) {
            var view = this.lines.get(i);
            if (pos <= view.start() + view.text().length()) {
                line = i;
                break;
            }
        }
        this.cursorLine.set(line);
        this.cursorColumn.set(Math.clamp(pos - this.lines.get(line).start(), 0, this.lines.get(line).text().length()));
        this.cursorBlink.reset();
    }

    private void deleteChar(int direction) {
        String currentText = this.text.get();

        if (this.hasSelection()) {
            int selMin = this.getSelectionMin();
            int selMax = this.getSelectionMax();
            String newText = currentText.substring(0, selMin) + currentText.substring(selMax);
            if (this.filter.test(newText)) {
                this.text.set(newText);
                this.updateLines();
                this.setCursorPosition(selMin);
                this.clearSelection();
                this.onValueChanged();
            }
            return;
        }

        int cursorPos = this.getCursorPosition();

        int next = Util.offsetByCodepoints(currentText, cursorPos, direction);
        if (next == cursorPos) return;
        this.setSelectionRange(Math.min(cursorPos, next), Math.max(cursorPos, next));
        this.insertText("");
    }

    private void moveCursor(int deltaX, int deltaY) {
        if (deltaY == 0) {
            this.setCursorPosition(Util.offsetByCodepoints(this.text.get(), this.getCursorPosition(), deltaX));
        } else {
            int line = Math.clamp(this.cursorLine.get() + deltaY, 0, this.lines.size() - 1);
            var font = Minecraft.getInstance().font;
            int column = font.plainSubstrByWidth(this.lines.get(line).text(), this.getCursorX()).length();
            this.cursorLine.set(line);
            this.cursorColumn.set(column);
        }
        this.scrollToCursor();
        this.cursorBlink.reset();
    }

    private int wordPosition(int direction) {
        String value = this.text.get();
        int cursor = this.getCursorPosition();
        if (direction < 0) {
            while (cursor > 0 && Character.isWhitespace(value.codePointBefore(cursor))) {
                cursor = Util.offsetByCodepoints(value, cursor, -1);
            }
            while (cursor > 0 && !Character.isWhitespace(value.codePointBefore(cursor))) {
                cursor = Util.offsetByCodepoints(value, cursor, -1);
            }
        } else {
            while (cursor < value.length() && !Character.isWhitespace(value.codePointAt(cursor))) {
                cursor = Util.offsetByCodepoints(value, cursor, 1);
            }
            while (cursor < value.length() && Character.isWhitespace(value.codePointAt(cursor))) {
                cursor = Util.offsetByCodepoints(value, cursor, 1);
            }
        }
        return cursor;
    }

    private int getCursorPosition() {
        return this.getPositionForLineColumn(this.cursorLine.get(), this.cursorColumn.get());
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

        int relativeX = mouseX - bounds.x() - 4;
        int relativeY = mouseY - bounds.y() - 4;

        int line = this.scrollLine.get() + relativeY / this.lineHeight;
        line = Mth.clamp(line, 0, Math.max(0, this.lines.size() - 1));

        var font = Minecraft.getInstance().font;
        String textLine = this.lines.get(line).text();
        int column = font.plainSubstrByWidth(textLine, Math.max(0, relativeX)).length();

        this.cursorLine.set(line);
        this.cursorColumn.set(column);
        this.scrollToCursor();
        this.cursorBlink.reset();

        int cursorPos = this.getCursorPosition();
        if (shiftDown) {
            this.selectionEnd.set(cursorPos);
        } else {
            this.selectionStart.set(cursorPos);
            this.selectionEnd.set(cursorPos);
        }
    }

    private void setSelectionRange(int start, int end) {
        this.selectionStart.set(start);
        this.selectionEnd.set(end);
    }

    private void clearSelection() {
        int pos = this.getCursorPosition();
        this.selectionStart.set(pos);
        this.selectionEnd.set(pos);
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
        return Math.max(1, (bounds.height() - 8) / this.lineHeight);
    }

    @Override
    public boolean isFocusable() {
        return this.editable;
    }

    @Override
    public void onDetached() {
        this.cursorBlink.detach();
        this.focused.set(false);
    }

    @Override
    public Component getNarrationMessage() {
        return Component.translatable("gui.narrate.editBox", this.hint != null ? this.hint : Component.empty(), this.text.get());
    }

    private void updateLines() {
        this.lines.clear();
        String currentText = this.text.get();
        int width = this.getBounds().width() - 8;
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

    private String getSelectedText() {
        if (!this.hasSelection()) return "";
        String currentText = this.text.get();
        int selMin = this.getSelectionMin();
        int selMax = this.getSelectionMax();
        return currentText.substring(selMin, selMax);
    }

    private void selectAll() {
        this.selectionStart.set(0);
        this.selectionEnd.set(this.text.get().length());
        this.setCursorPosition(this.text.get().length());
        this.scrollToCursor();
    }

    private void onValueChanged() {
        if (this.responder != null) {
            this.responder.accept(this.text.get());
        }
    }

    public String getValue() {
        return this.text.get();
    }

    public void setValue(String value) {
        if (this.filter.test(value)) {
            String clamped = value.length() > this.maxLength ? value.substring(0, this.maxLength) : value;
            this.text.set(clamped);
            this.updateLines();
            this.cursorLine.set(0);
            this.cursorColumn.set(0);
            this.selectionStart.set(0);
            this.selectionEnd.set(0);
            this.scrollLine.set(0);
            this.onValueChanged();
        }
    }

    public void setMaxLength(int maxLength) {
        if (maxLength < 0) throw new IllegalArgumentException("Maximum text length must be nonnegative");
        this.maxLength = maxLength;
        String current = this.text.get();
        if (current.length() > maxLength) {
            this.setValue(current.substring(0, maxLength));
        }
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        this.invalidate();
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    public void setFilter(Predicate<String> filter) {
        this.filter = filter;
    }

    public void setResponder(Consumer<String> responder) {
        this.responder = responder;
    }

    public void setHint(Component hint) {
        this.hint = hint;
        this.invalidate();
    }

    /**
     * Sets the cursor animation mode.
     * {@link CursorAnimationMode#INSTANT} toggles the cursor on/off instantly.
     * {@link CursorAnimationMode#FADE} smoothly fades the cursor in and out.
     *
     * @param mode the animation mode
     * @return this for chaining
     */
    public TextArea setCursorAnimationMode(CursorAnimationMode mode) {
        this.cursorAnimationMode = java.util.Objects.requireNonNull(mode);
        return this.setCursorAnimation(300, mode == CursorAnimationMode.INSTANT ? Easing.STEP : Easing.EASE_IN_OUT);
    }

    public TextArea setCursorAnimation(long intervalMillis, Easing easing) {
        this.cursorBlink.configure(intervalMillis, easing);
        this.cursorAnimationMode = easing == Easing.STEP ? CursorAnimationMode.INSTANT : CursorAnimationMode.FADE;
        this.invalidate();
        return this;
    }

    private void invalidate() {
        var node = this.getNode();
        if (node != null) node.getTree().requestRecompose(node);
    }

    public CursorAnimationMode getCursorAnimationMode() {
        return this.cursorAnimationMode;
    }

    public boolean isEditable() {
        return this.editable;
    }

    public boolean isFocused() {
        return this.focused.get();
    }

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

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        boolean widthChanged = this.wrappedWidth != bounds.width() - 8;
        if (widthChanged) {
            int cursor = this.getCursorPosition();
            this.updateLines();
            this.setCursorPosition(cursor);
        }
        this.scrollLine.set(Math.clamp(this.scrollLine.get(), 0, Math.max(0, this.lines.size() - this.getVisibleLines())));
        if ((widthChanged || this.composedVisibleLines != this.getVisibleLines()) && this.getNode() != null) {
            this.getNode().getTree().requestRecompose(this.getNode());
        }
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }

    @Override
    public UICursor getCursor(int x, int y) {
        return this.editable ? CompoundCursors.IBEAM : null;
    }
}
