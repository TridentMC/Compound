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

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.tridevmc.compound.ui.CompoundCursors;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
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
    private static final int CURSOR_BLINK_INTERVAL_MS = 300;

    private final State<String> text = new StateImpl<>("");
    private final State<Integer> cursorLine = new StateImpl<>(0);
    private final State<Integer> cursorColumn = new StateImpl<>(0);
    private final State<Integer> scrollLine = new StateImpl<>(0);
    private final State<Boolean> focused = new StateImpl<>(false);

    private int maxLength = 500;
    private boolean editable = true;
    private int textColor = DEFAULT_TEXT_COLOR;
    private int backgroundColor = DEFAULT_BACKGROUND_COLOR;
    private Predicate<String> filter = s -> s != null;
    private Consumer<String> responder;
    private Component hint;
    private int lineHeight;
    private List<String> lines = new ArrayList<>();

    public TextArea() {
        this.updateLines();
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.text);
        scope.bind(this.cursorLine);
        scope.bind(this.cursorColumn);
        scope.bind(this.scrollLine);
        scope.bind(this.focused);

        scope.onClick(event -> {
            if (!this.editable) return false;
            scope.requestFocus();
            this.updateCursorFromPosition(event.x(), event.y());
            return true;
        });

        scope.onKeyPress(this::handleKeyPress);
        scope.onCharTyped(this::handleCharTyped);

        scope.onFocusGained(() -> this.focused.set(true));
        scope.onFocusLost(() -> this.focused.set(false));

        scope.onScroll(event -> {
            int newScroll = this.scrollLine.get() - (int) event.scrollDelta();
            newScroll = Math.max(0, Math.min(newScroll, Math.max(0, this.lines.size() - 1)));
            this.scrollLine.set(newScroll);
            return true;
        });

        scope.e(new Stack(), stack -> {
            stack.layout().fillMax();

            stack.e(new Rect(() -> this.backgroundColor), bg -> bg.layout().fillMax());

            stack.e(new Box(), contentBox -> {
                contentBox.layout().padding(4).fillMax();

                contentBox.e(new Stack(), textStack -> {
                    textStack.layout().fillMax();

                    var font = Minecraft.getInstance().font;
                    this.lineHeight = font.lineHeight;

                    int startLine = this.scrollLine.get();
                    int visibleLines = this.getVisibleLines();

                    for (int i = 0; i < visibleLines && startLine + i < this.lines.size(); i++) {
                        final int lineIndex = startLine + i;
                        final int lineOffset = i;
                        String line = this.lines.get(lineIndex);

                        textStack.e(new Label(
                                Component.literal(line),
                                () -> this.editable ? this.textColor : 0x808080,
                                () -> false
                        ), label -> label.layout().margin(0, lineOffset * this.lineHeight, 0, 0));
                    }

                    if (this.focused.get() && this.editable) {
                        int cursorX = this.getCursorX();
                        int cursorY = (this.cursorLine.get() - startLine) * this.lineHeight;

                        if (cursorY >= 0 && cursorY < visibleLines * this.lineHeight) {
                            textStack.e(new Rect(0xFFFFFFFF), cursor ->
                                    cursor.layout().fixedSize(1, this.lineHeight)
                                            .margin(cursorX, cursorY, 0, 0));
                        }
                    }
                });
            });
        });
    }

    private boolean handleKeyPress(com.tridevmc.compound.ui.event.KeyInputEvent event) {
        if (!this.focused.get() || !this.editable) return false;

        int keyCode = event.keyCode();
        boolean shift = event.shiftDown();
        boolean ctrl = event.ctrlDown();

        return switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                this.deleteChar(-1);
                yield true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                this.deleteChar(1);
                yield true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                this.moveCursor(-1, 0);
                yield true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                this.moveCursor(1, 0);
                yield true;
            }
            case GLFW.GLFW_KEY_UP -> {
                this.moveCursor(0, -1);
                yield true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                this.moveCursor(0, 1);
                yield true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                this.cursorColumn.set(0);
                yield true;
            }
            case GLFW.GLFW_KEY_END -> {
                if (this.cursorLine.get() < this.lines.size()) {
                    this.cursorColumn.set(this.lines.get(this.cursorLine.get()).length());
                }
                yield true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                this.insertText("\n");
                yield true;
            }
            case GLFW.GLFW_KEY_A -> {
                if (ctrl) {
                    this.cursorLine.set(0);
                    this.cursorColumn.set(0);
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
        if (currentText.length() + textToInsert.length() > this.maxLength) {
            textToInsert = textToInsert.substring(0, this.maxLength - currentText.length());
        }

        int cursorPos = this.getCursorPosition();
        String newText = currentText.substring(0, cursorPos) + textToInsert + currentText.substring(cursorPos);

        if (this.filter.test(newText)) {
            this.text.set(newText);
            this.updateLines();

            for (int i = 0; i < textToInsert.length(); i++) {
                if (textToInsert.charAt(i) == '\n') {
                    this.cursorLine.set(this.cursorLine.get() + 1);
                    this.cursorColumn.set(0);
                } else {
                    this.cursorColumn.set(this.cursorColumn.get() + 1);
                }
            }

            this.scrollToCursor();
            this.onValueChanged();
        }
    }

    private void deleteChar(int direction) {
        String currentText = this.text.get();
        int cursorPos = this.getCursorPosition();

        if (direction < 0 && cursorPos > 0) {
            String newText = currentText.substring(0, cursorPos - 1) + currentText.substring(cursorPos);
            if (this.filter.test(newText)) {
                this.text.set(newText);
                this.moveCursor(-1, 0);
                this.updateLines();
                this.onValueChanged();
            }
        } else if (direction > 0 && cursorPos < currentText.length()) {
            String newText = currentText.substring(0, cursorPos) + currentText.substring(cursorPos + 1);
            if (this.filter.test(newText)) {
                this.text.set(newText);
                this.updateLines();
                this.onValueChanged();
            }
        }
    }

    private void moveCursor(int deltaX, int deltaY) {
        int newLine = this.cursorLine.get() + deltaY;
        int newColumn = this.cursorColumn.get() + deltaX;

        if (newLine < 0) newLine = 0;
        if (newLine >= this.lines.size()) newLine = Math.max(0, this.lines.size() - 1);

        String line = this.lines.get(newLine);
        if (newColumn < 0) {
            if (newLine > 0) {
                newLine--;
                newColumn = this.lines.get(newLine).length();
            } else {
                newColumn = 0;
            }
        }
        if (newColumn > line.length()) {
            if (newLine < this.lines.size() - 1) {
                newLine++;
                newColumn = 0;
            } else {
                newColumn = line.length();
            }
        }

        this.cursorLine.set(newLine);
        this.cursorColumn.set(newColumn);
        this.scrollToCursor();
    }

    private int getCursorPosition() {
        int pos = 0;
        for (int i = 0; i < this.cursorLine.get() && i < this.lines.size(); i++) {
            pos += this.lines.get(i).length() + 1;
        }
        pos += this.cursorColumn.get();
        return pos;
    }

    private int getCursorX() {
        var font = Minecraft.getInstance().font;
        if (this.cursorLine.get() >= this.lines.size()) return 0;
        String line = this.lines.get(this.cursorLine.get());
        int col = Math.min(this.cursorColumn.get(), line.length());
        return font.width(line.substring(0, col));
    }

    private void updateCursorFromPosition(int mouseX, int mouseY) {
        Bounds bounds = this.getBounds();
        if (bounds == null) return;

        int relativeX = mouseX - bounds.x() - 4;
        int relativeY = mouseY - bounds.y() - 4;

        int line = this.scrollLine.get() + relativeY / this.lineHeight;
        line = Mth.clamp(line, 0, Math.max(0, this.lines.size() - 1));

        var font = Minecraft.getInstance().font;
        String textLine = line < this.lines.size() ? this.lines.get(line) : "";
        int column = 0;
        for (int i = 0; i <= textLine.length(); i++) {
            if (font.width(textLine.substring(0, i)) >= relativeX) {
                column = i;
                break;
            }
            column = i;
        }

        this.cursorLine.set(line);
        this.cursorColumn.set(column);
        this.scrollToCursor();
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

    private void updateLines() {
        this.lines.clear();
        String currentText = this.text.get();
        if (currentText.isEmpty()) {
            this.lines.add("");
            return;
        }

        StringBuilder currentLine = new StringBuilder();
        for (char c : currentText.toCharArray()) {
            if (c == '\n') {
                this.lines.add(currentLine.toString());
                currentLine = new StringBuilder();
            } else {
                currentLine.append(c);
            }
        }
        this.lines.add(currentLine.toString());
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
        return "";
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
            this.onValueChanged();
        }
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        String current = this.text.get();
        if (current.length() > maxLength) {
            this.setValue(current.substring(0, maxLength));
        }
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
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
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }

    @Override
    public CursorType getCursor(int x, int y) {
        return this.editable ? CompoundCursors.IBEAM : null;
    }
}
