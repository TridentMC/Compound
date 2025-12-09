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
import com.tridevmc.compound.ui.animation.AnimatedState;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A single-line text input field, inspired by Minecraft's EditBox.
 * <p>
 * Supports text editing, cursor navigation, text selection, copy/paste,
 * maximum length constraints, input filtering, and visual customization.
 * </p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new TextInput(), input -&gt; {
 *     input.layout().fixedSize(200, 20);
 *     input.getElement().setMaxLength(50);
 *     input.getElement().setHint(Component.literal("Enter text..."));
 *     input.getElement().setResponder(text -&gt; System.out.println("Text: " + text));
 * });
 * </pre>
 */
public class TextInput extends BaseElement implements IComposableElement {

    // Sprites for bordered mode
    private static final IScreenSprite SPRITE_NORMAL = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/text_field"));
    private static final IScreenSprite SPRITE_FOCUSED = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/text_field_highlighted"));

    // Constants
    private static final int CURSOR_BLINK_INTERVAL_MS = 300;
    private static final int DEFAULT_TEXT_COLOR = 0xE0E0E0;
    private static final int DEFAULT_DISABLED_TEXT_COLOR = 0x707070;
    private static final int DEFAULT_HINT_COLOR = 0x808080;
    private static final int SELECTION_COLOR = 0x800000FF;
    private static final int CURSOR_COLOR = 0xFFD0D0D0;
    private static final int BORDER_PADDING = 4;

    // State
    private final State<String> text = new StateImpl<>("");
    private final State<Integer> cursorPos = new StateImpl<>(0);
    private final State<Integer> highlightPos = new StateImpl<>(0);
    private final State<Integer> displayPos = new StateImpl<>(0);
    private final State<Boolean> focused = new StateImpl<>(false);

    // Configuration
    private int maxLength = 32;
    private boolean bordered = true;
    private boolean editable = true;
    private Predicate<String> filter = Objects::nonNull;
    @Nullable
    private Consumer<String> responder;
    @Nullable
    private Component hint;
    private int textColor = DEFAULT_TEXT_COLOR;
    private int disabledTextColor = DEFAULT_DISABLED_TEXT_COLOR;

    // Animation state (initialized in compose)
    private AnimatedState<Float> cursorBlink;

    /**
     * Creates a new TextInput with default settings.
     */
    public TextInput() {
    }

    /**
     * Creates a new TextInput with an initial value.
     *
     * @param initialValue the initial text value
     */
    public TextInput(String initialValue) {
        this.text.set(initialValue != null ? initialValue : "");
        this.cursorPos.set(this.text.get().length());
        this.highlightPos.set(this.cursorPos.get());
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.text);
        scope.bind(this.cursorPos);
        scope.bind(this.highlightPos);
        scope.bind(this.displayPos);
        scope.bind(this.focused);

        // Initialize cursor blink animation (only visible when focused)
        if (this.cursorBlink == null) {
            this.cursorBlink = scope.animateFloatLooping(1f, 0f, CURSOR_BLINK_INTERVAL_MS);
        }

        // Event handlers
        scope.onClick(event -> {
            if (!this.editable) return false;
            scope.requestFocus();
            int clickPos = this.findClickPosition(event.x());
            this.moveCursorTo(clickPos, event.shiftDown());
            return true;
        });

        scope.onMouseDrag(event -> {
            if (!this.editable || !this.focused.get()) return false;
            int dragPos = this.findClickPosition(event.x());
            this.moveCursorTo(dragPos, true);
            return true;
        });

        scope.onKeyPress(this::handleKeyPress);
        scope.onCharTyped(this::handleCharTyped);

        scope.onFocusGained(() -> {
            this.focused.set(true);
            // Reset blink animation on focus gain
            if (this.cursorBlink != null) {
                this.cursorBlink.setImmediate(1f);
            }
        });

        scope.onFocusLost(() -> this.focused.set(false));

        // Build composition - Stack with background and content
        scope.e(new Stack(), stack -> {
            stack.layout().fillMax();

            // Background sprite (if bordered)
            if (this.bordered) {
                IScreenSprite bgSprite = this.focused.get() ? SPRITE_FOCUSED : SPRITE_NORMAL;
                stack.e(new ElementSprite(bgSprite), bg -> bg.layout().fillMax());
            }

            // Content area with padding
            stack.e(new Box(), box -> {
                if (this.bordered) {
                    box.layout().padding(BORDER_PADDING).fillMax();
                } else {
                    box.layout().fillMax();
                }

                // Use a Stack for layering: selection highlight, text, cursor
                box.e(new Stack(), contentStack -> {
                    contentStack.layout().fillMax().contentAlignment(Alignment.CENTER_LEFT);
                    this.composeTextContent(contentStack);
                });
            });
        });
    }

    /**
     * Composes the text content including selection highlight, text labels, and cursor.
     */
    private void composeTextContent(ICompositionScope scope) {
        var font = Minecraft.getInstance().font;
        String currentText = this.text.get();
        int cursor = this.cursorPos.get();
        int highlight = this.highlightPos.get();
        int display = this.displayPos.get();
        boolean isFocused = this.focused.get();

        // Show hint if empty and not focused
        if (currentText.isEmpty() && !isFocused && this.hint != null) {
            scope.e(new ElementLabel(this.hint, DEFAULT_HINT_COLOR, false));
            return;
        }

        // Calculate visible text based on display position
        String visibleText = currentText.substring(Math.min(display, currentText.length()));
        int color = this.editable ? this.textColor : this.disabledTextColor;

        // Selection highlight (if there's a selection and focused)
        if (isFocused && cursor != highlight) {
            int selStart = Math.min(cursor, highlight) - display;
            int selEnd = Math.max(cursor, highlight) - display;

            if (selEnd > 0 && selStart < visibleText.length()) {
                selStart = Math.max(0, selStart);
                selEnd = Math.min(visibleText.length(), selEnd);

                String beforeSel = visibleText.substring(0, selStart);
                String selected = visibleText.substring(selStart, selEnd);

                int xOffset = font != null ? font.width(beforeSel) : beforeSel.length() * 6;
                int selWidth = font != null ? font.width(selected) : selected.length() * 6;

                // Selection rectangle
                scope.e(new ElementRect(SELECTION_COLOR), rect -> {
                    rect.layout()
                            .fixedSize(selWidth, font != null ? font.lineHeight : 9)
                            .margin(xOffset, 0, 0, 0);
                });
            }
        }

        // Main text label
        if (!visibleText.isEmpty()) {
            scope.e(new ElementLabel(Component.literal(visibleText), color, true));
        }

        // Cursor (only when focused and visible based on blink)
        if (isFocused && this.editable) {
            int cursorOffset = cursor - display;
            if (cursorOffset >= 0) {
                String beforeCursor = visibleText.substring(0, Math.min(cursorOffset, visibleText.length()));
                int xOffset = font != null ? font.width(beforeCursor) : beforeCursor.length() * 6;

                // Cursor visibility based on blink animation
                float blinkVal = this.cursorBlink != null ? this.cursorBlink.get() : 1f;
                if (blinkVal > 0.5f) {
                    // Show cursor as a thin rectangle
                    scope.e(new ElementRect(CURSOR_COLOR), cursorRect -> {
                        cursorRect.layout()
                                .fixedSize(1, font != null ? font.lineHeight : 9)
                                .margin(xOffset, 0, 0, 0);
                    });
                }
            }
        }
    }

    /**
     * Handles key press events for navigation and editing.
     */
    private boolean handleKeyPress(com.tridevmc.compound.ui.event.KeyInputEvent event) {
        if (!this.focused.get() || !this.editable) return false;

        int keyCode = event.keyCode();
        boolean shift = event.shiftDown();
        boolean ctrl = event.ctrlDown();

        return switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                this.deleteText(-1, ctrl);
                yield true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                this.deleteText(1, ctrl);
                yield true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (ctrl) {
                    this.moveCursorTo(this.getWordPosition(-1), shift);
                } else {
                    this.moveCursor(-1, shift);
                }
                yield true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (ctrl) {
                    this.moveCursorTo(this.getWordPosition(1), shift);
                } else {
                    this.moveCursor(1, shift);
                }
                yield true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                this.moveCursorTo(0, shift);
                yield true;
            }
            case GLFW.GLFW_KEY_END -> {
                this.moveCursorTo(this.text.get().length(), shift);
                yield true;
            }
            case GLFW.GLFW_KEY_A -> {
                if (ctrl) {
                    // Select all
                    this.moveCursorTo(this.text.get().length(), false);
                    this.highlightPos.set(0);
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
                if (ctrl) {
                    this.copyToClipboard();
                    this.insertText("");
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    /**
     * Handles character typed events for text input.
     */
    private boolean handleCharTyped(com.tridevmc.compound.ui.event.CharEvent event) {
        if (!this.focused.get() || !this.editable) return false;

        char c = event.character();
        // Filter out control characters
        if (Character.isISOControl(c)) return false;

        this.insertText(String.valueOf(c));
        return true;
    }

    // =====================
    // Text Manipulation
    // =====================

    /**
     * Gets the current text value.
     *
     * @return the current text
     */
    public String getValue() {
        return this.text.get();
    }

    /**
     * Sets the text value, moving cursor to end.
     *
     * @param newText the new text value
     */
    public void setValue(String newText) {
        if (this.filter.test(newText)) {
            String filtered = newText.length() > this.maxLength
                    ? newText.substring(0, this.maxLength)
                    : newText;
            this.text.set(filtered);
            this.moveCursorTo(filtered.length(), false);
            this.onValueChanged();
        }
    }

    /**
     * Inserts text at the cursor position, replacing any selection.
     *
     * @param textToInsert the text to insert
     */
    public void insertText(String textToInsert) {
        String currentText = this.text.get();
        int cursor = this.cursorPos.get();
        int highlight = this.highlightPos.get();

        int start = Math.min(cursor, highlight);
        int end = Math.max(cursor, highlight);

        int availableSpace = this.maxLength - currentText.length() + (end - start);
        if (availableSpace <= 0) return;

        String toInsert = textToInsert.length() > availableSpace
                ? textToInsert.substring(0, availableSpace)
                : textToInsert;

        String newText = currentText.substring(0, start) + toInsert + currentText.substring(end);

        if (this.filter.test(newText)) {
            this.text.set(newText);
            int newCursor = start + toInsert.length();
            this.cursorPos.set(newCursor);
            this.highlightPos.set(newCursor);
            this.scrollToCursor();
            this.onValueChanged();
        }
    }

    /**
     * Deletes text in the specified direction.
     *
     * @param direction -1 for backspace, 1 for delete
     * @param byWord    true to delete entire word
     */
    public void deleteText(int direction, boolean byWord) {
        String currentText = this.text.get();
        if (currentText.isEmpty()) return;

        int cursor = this.cursorPos.get();
        int highlight = this.highlightPos.get();

        // If there's a selection, delete it
        if (cursor != highlight) {
            this.insertText("");
            return;
        }

        int deletePos;
        if (byWord) {
            deletePos = this.getWordPosition(direction);
        } else {
            deletePos = Mth.clamp(cursor + direction, 0, currentText.length());
        }

        int start = Math.min(cursor, deletePos);
        int end = Math.max(cursor, deletePos);

        if (start == end) return;

        String newText = currentText.substring(0, start) + currentText.substring(end);

        if (this.filter.test(newText)) {
            this.text.set(newText);
            this.cursorPos.set(start);
            this.highlightPos.set(start);
            this.scrollToCursor();
            this.onValueChanged();
        }
    }

    /**
     * Gets the currently selected text.
     *
     * @return the selected text, or empty string if no selection
     */
    public String getSelectedText() {
        int cursor = this.cursorPos.get();
        int highlight = this.highlightPos.get();
        if (cursor == highlight) return "";

        String currentText = this.text.get();
        int start = Math.min(cursor, highlight);
        int end = Math.max(cursor, highlight);
        return currentText.substring(start, end);
    }

    // =====================
    // Cursor Navigation
    // =====================

    /**
     * Moves the cursor by the specified delta.
     *
     * @param delta  the number of characters to move (negative = left, positive = right)
     * @param select true to extend selection
     */
    public void moveCursor(int delta, boolean select) {
        int newPos = Mth.clamp(this.cursorPos.get() + delta, 0, this.text.get().length());
        this.moveCursorTo(newPos, select);
    }

    /**
     * Moves the cursor to the specified position.
     *
     * @param position the new cursor position
     * @param select   true to extend selection
     */
    public void moveCursorTo(int position, boolean select) {
        int clampedPos = Mth.clamp(position, 0, this.text.get().length());
        this.cursorPos.set(clampedPos);
        if (!select) {
            this.highlightPos.set(clampedPos);
        }
        this.scrollToCursor();
        // Reset blink on cursor move
        if (this.cursorBlink != null) {
            this.cursorBlink.setImmediate(1f);
        }
    }

    /**
     * Finds the word boundary position in the specified direction.
     *
     * @param direction -1 for previous word, 1 for next word
     * @return the position of the word boundary
     */
    private int getWordPosition(int direction) {
        String currentText = this.text.get();
        int pos = this.cursorPos.get();

        if (direction < 0) {
            // Move backward
            while (pos > 0 && currentText.charAt(pos - 1) == ' ') pos--;
            while (pos > 0 && currentText.charAt(pos - 1) != ' ') pos--;
        } else {
            // Move forward
            int len = currentText.length();
            while (pos < len && currentText.charAt(pos) != ' ') pos++;
            while (pos < len && currentText.charAt(pos) == ' ') pos++;
        }

        return pos;
    }

    /**
     * Scrolls the display to keep the cursor visible.
     */
    private void scrollToCursor() {
        var font = Minecraft.getInstance().font;
        if (font == null) return;

        String currentText = this.text.get();
        int cursor = this.cursorPos.get();
        int display = this.displayPos.get();
        int innerWidth = this.getInnerWidth();

        // Ensure display position is valid
        display = Math.min(display, currentText.length());

        // Cursor is before visible area - scroll left
        if (cursor < display) {
            this.displayPos.set(cursor);
            return;
        }

        // Cursor is after visible area - scroll right
        String visibleText = currentText.substring(display);
        int cursorOffset = cursor - display;

        if (cursorOffset <= visibleText.length()) {
            String textToCursor = visibleText.substring(0, cursorOffset);
            if (font.width(textToCursor) > innerWidth) {
                // Need to scroll right until cursor is visible
                while (display < cursor) {
                    display++;
                    String newVisible = currentText.substring(display, cursor);
                    if (font.width(newVisible) <= innerWidth) break;
                }
                this.displayPos.set(display);
            }
        }
    }

    /**
     * Finds the character position for a click at the given x coordinate.
     */
    private int findClickPosition(int clickX) {
        var font = Minecraft.getInstance().font;
        Bounds bounds = this.getBounds();
        if (bounds == null || font == null) return 0;

        int textStartX = bounds.x() + (this.bordered ? BORDER_PADDING : 0);
        int relativeX = clickX - textStartX;

        if (relativeX < 0) return this.displayPos.get();

        String currentText = this.text.get();
        int display = this.displayPos.get();
        String visibleText = currentText.substring(Math.min(display, currentText.length()));

        // Find position by measuring text width
        for (int i = 0; i <= visibleText.length(); i++) {
            String sub = visibleText.substring(0, i);
            if (font.width(sub) >= relativeX) {
                return display + i;
            }
        }

        return display + visibleText.length();
    }

    /**
     * Gets the inner width available for text.
     */
    private int getInnerWidth() {
        Bounds bounds = this.getBounds();
        if (bounds == null) return 100;
        return bounds.width() - (this.bordered ? BORDER_PADDING * 2 : 0);
    }

    // =====================
    // Clipboard Operations
    // =====================

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

    // =====================
    // Value Change Notification
    // =====================

    private void onValueChanged() {
        if (this.responder != null) {
            this.responder.accept(this.text.get());
        }
    }

    // =====================
    // Configuration
    // =====================

    /**
     * Sets the maximum text length.
     *
     * @param maxLength the maximum number of characters
     * @return this TextInput for chaining
     */
    public TextInput setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        String current = this.text.get();
        if (current.length() > maxLength) {
            this.text.set(current.substring(0, maxLength));
            this.moveCursorTo(this.text.get().length(), false);
            this.onValueChanged();
        }
        return this;
    }

    /**
     * Sets whether the input has a border.
     *
     * @param bordered true for bordered appearance
     * @return this TextInput for chaining
     */
    public TextInput setBordered(boolean bordered) {
        this.bordered = bordered;
        return this;
    }

    /**
     * Sets the input filter predicate.
     *
     * @param filter predicate that returns true for valid input
     * @return this TextInput for chaining
     */
    public TextInput setFilter(Predicate<String> filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Sets the responder callback for text changes.
     *
     * @param responder consumer called when text changes
     * @return this TextInput for chaining
     */
    public TextInput setResponder(Consumer<String> responder) {
        this.responder = responder;
        return this;
    }

    /**
     * Sets the hint text shown when empty and unfocused.
     *
     * @param hint the hint component
     * @return this TextInput for chaining
     */
    public TextInput setHint(@Nullable Component hint) {
        this.hint = hint;
        return this;
    }

    /**
     * Sets the text color for editable state.
     *
     * @param color the text color (ARGB)
     * @return this TextInput for chaining
     */
    public TextInput setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    /**
     * Sets the text color for disabled state.
     *
     * @param color the disabled text color (ARGB)
     * @return this TextInput for chaining
     */
    public TextInput setDisabledTextColor(int color) {
        this.disabledTextColor = color;
        return this;
    }

    public State<String> getTextState() {
        return this.text;
    }

    // =====================
    // State Accessors
    // =====================

    public State<Boolean> getFocusedState() {
        return this.focused;
    }

    public boolean isEditable() {
        return this.editable;
    }

    /**
     * Sets whether the input is editable.
     *
     * @param editable true to allow editing
     * @return this TextInput for chaining
     */
    public TextInput setEditable(boolean editable) {
        this.editable = editable;
        return this;
    }

    public boolean isFocused() {
        return this.focused.get();
    }

    // =====================
    // Layout
    // =====================

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        // TextInput should use its first child (Stack) for sizing
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        // Default size if no children
        return new Size(
                Math.min(200, constraints.maxWidth()),
                Math.min(20, constraints.maxHeight())
        );
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        // Child fills the entire TextInput bounds
        return List.of(bounds);
    }

    @Override
    public CursorType getCursor(int x, int y) {
        return this.editable ? CompoundCursors.IBEAM : null;
    }
}
