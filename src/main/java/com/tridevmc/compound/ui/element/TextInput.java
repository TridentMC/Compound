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

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.cursor.CursorType;
import net.minecraft.ChatFormatting;

import com.tridevmc.compound.ui.CompoundCursors;
import com.tridevmc.compound.ui.animation.AnimatedState;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A single-line text input field with full feature parity to Minecraft's EditBox.
 * <p>
 * This is a composable element that uses existing primitives (Text, Sprite, etc.)
 * with dynamic suppliers to render text, cursor, and selection.
 * </p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new TextInput(), input -> {
 *     input.layout().fixedSize(200, 20);
 *     input.setMaxLength(50);
 *     input.setHint(Component.literal("Enter text..."));
 *     input.setResponder(text -> System.out.println("Text: " + text));
 *     input.setSuggestion("Type here...");
 * });
 * </pre>
 */
public class TextInput extends BaseElement implements IComposableElement {

    // Sprites for bordered mode
    private static final IScreenSprite SPRITE_NORMAL = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/text_field"));
    private static final IScreenSprite SPRITE_FOCUSED = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/text_field_highlighted"));

    // Constants from vanilla EditBox
    private static final int CURSOR_BLINK_INTERVAL_MS = 300;
    private static final int CURSOR_INSERT_WIDTH = 1;
    private static final String CURSOR_APPEND_CHARACTER = "_";
    public static final int DEFAULT_TEXT_COLOR = -2039584;  // 0xE0E0E0
    public static final int DEFAULT_TEXT_COLOR_UNEDITABLE = -9408400;
    public static final Style DEFAULT_HINT_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);

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
    private boolean canLoseFocus = true;
    private boolean centered = false;
    private boolean textShadow = false;  // Default to false to match vanilla EditBox behavior
    private int textColor = DEFAULT_TEXT_COLOR;
    private int textColorUneditable = DEFAULT_TEXT_COLOR_UNEDITABLE;
    private Predicate<String> filter = Objects::nonNull;
    @Nullable
    private Consumer<String> responder;
    @Nullable
    private Component hint;
    @Nullable
    private String suggestion;
    private final List<TextFormatter> formatters = new ArrayList<>();

    // Animation state
    private AnimatedState<Integer> cursorBlink;
    private long focusedTime = System.currentTimeMillis();

    public TextInput() {
    }

    public TextInput(String initialValue) {
        this.text.set(initialValue != null ? initialValue : "");
        this.cursorPos.set(this.text.get().length());
        this.highlightPos.set(this.cursorPos.get());
    }

    @Override
    public void compose(ICompositionScope scope) {
        // Bind all state to trigger recomposition on changes
        scope.bind(this.text);
        scope.bind(this.cursorPos);
        scope.bind(this.highlightPos);
        scope.bind(this.displayPos);
        scope.bind(this.focused);

        // Initialize cursor blink animation (alternates between 0 and 1)
        if (this.cursorBlink == null) {
            this.cursorBlink = scope.animateIntLooping(0, 1, CURSOR_BLINK_INTERVAL_MS);
        }

        // Register event handlers
        this.registerEventHandlers(scope);

        // Compose the UI
        this.composeUI(scope);
    }

    private void registerEventHandlers(ICompositionScope scope) {
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
            this.focusedTime = System.currentTimeMillis();
            if (this.cursorBlink != null) {
                this.cursorBlink.setImmediate(1);
            }
        });

        scope.onFocusLost(() -> {
            if (this.canLoseFocus) {
                this.focused.set(false);
            }
        });
    }

    private void composeUI(ICompositionScope scope) {
        scope.e(new Stack(), stack -> {
            stack.layout().fillMax();

            // Background sprite (if bordered)
            if (this.bordered) {
                IScreenSprite bgSprite = this.focused.get() ? SPRITE_FOCUSED : SPRITE_NORMAL;
                stack.e(new Sprite(bgSprite), bg -> bg.layout().fillMax());
            }

            // Content area - use a Box for padding
            stack.e(new Box(), box -> {
                if (this.bordered) {
                    box.layout().padding(4).fillMax();
                } else {
                    box.layout().fillMax();
                }

                // Use Stack for centering if needed
                box.e(new Stack(), contentStack -> {
                    var alignment = this.centered ? Alignment.CENTER : Alignment.CENTER_LEFT;
                    contentStack.layout().fillMax().contentAlignment(alignment);

                    // Use Text primitive directly with dynamic suppliers
                    // Set up selection highlighting
                    contentStack.e(new Text(
                            () -> this.getRenderedText(),
                            () -> this.editable ? this.textColor : this.textColorUneditable,
                            () -> this.textShadow
                        ).setHighlight(
                            () -> this.getSelectionStart(),
                            () -> this.getSelectionEnd(),
                            () -> 0x800000FF  // Selection color (semi-transparent blue)
                        ),
                        textElem -> {
                            if (this.centered) {
                                // For centered text, let it wrap content naturally
                                // No fillMax() - the Stack will center the text
                            } else {
                                // For left-aligned, fill the available width
                                textElem.layout().fillMax();
                            }
                        }
                    );
                });
            });
        });
    }

    /**
     * Gets the rendered text as a Component, including cursor when focused and visible.
     * This supplier is evaluated every frame during rendering.
     */
    private Component getRenderedText() {
        String currentText = this.text.get();
        int cursor = this.cursorPos.get();
        boolean isFocused = this.focused.get();
        int display = this.displayPos.get();

        // Show hint if empty and not focused
        if (currentText.isEmpty() && !isFocused && this.hint != null) {
            return this.hint;
        }

        // Get visible portion of text
        String visibleText = currentText.substring(Math.min(display, currentText.length()));

        // Build the text component with cursor if appropriate
        MutableComponent result = Component.literal(visibleText);

        // Check if cursor should be visible using animation state
        boolean showCursor = isFocused && this.editable &&
                this.cursorBlink != null && this.cursorBlink.get() == 1;

        // Add cursor at the end if visible
        int cursorOffset = cursor - display;
        if (showCursor && cursor >= display) {
            // Cursor is within or at the end of visible text
            int cursorInVisible = Math.min(cursorOffset, visibleText.length());

            if (cursorInVisible == visibleText.length() && currentText.length() < this.maxLength) {
                // Cursor at end - append underscore
                result.append(Component.literal(CURSOR_APPEND_CHARACTER));
            }
        }

        // Apply text formatters
        FormattedCharSequence formatted = this.applyFormat(visibleText, display);
        if (formatted != null) {
            return Component.literal(formatted.toString());
        }

        return result;
    }

    /**
     * Gets the selection start position relative to visible text.
     */
    private int getSelectionStart() {
        String currentText = this.text.get();
        int cursor = this.cursorPos.get();
        int highlight = this.highlightPos.get();
        int display = this.displayPos.get();

        if (!this.focused.get() || cursor == highlight) return -1;

        int selStart = Math.min(cursor, highlight) - display;
        return Math.max(0, selStart);
    }

    /**
     * Gets the selection end position relative to visible text.
     */
    private int getSelectionEnd() {
        String currentText = this.text.get();
        int cursor = this.cursorPos.get();
        int highlight = this.highlightPos.get();
        int display = this.displayPos.get();

        if (!this.focused.get() || cursor == highlight) return -1;

        int visibleLength = currentText.length() - display;
        int selEnd = Math.max(cursor, highlight) - display;
        return Math.min(visibleLength, selEnd);
    }

    private FormattedCharSequence applyFormat(String text, int displayPos) {
        for (var formatter : this.formatters) {
            var result = formatter.format(text, displayPos);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    // =====================
    // Event Handlers
    // =====================

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

    private boolean handleCharTyped(com.tridevmc.compound.ui.event.CharEvent event) {
        if (!this.focused.get() || !this.editable) return false;
        this.insertText(String.valueOf(event.character()));
        return true;
    }

    // =====================
    // Text Manipulation
    // =====================

    public String getValue() {
        return this.text.get();
    }

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

        int deletePos = byWord ? this.getWordPosition(direction) : Mth.clamp(cursor + direction, 0, currentText.length());

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

    public String getHighlighted() {
        int cursor = this.cursorPos.get();
        int highlight = this.highlightPos.get();
        if (cursor == highlight) return "";

        String currentText = this.text.get();
        int start = Math.min(cursor, highlight);
        int end = Math.max(cursor, highlight);
        return currentText.substring(start, end);
    }

    /**
     * Gets the currently selected text (alias for getHighlighted()).
     *
     * @return the selected text, or empty string if no selection
     */
    public String getSelectedText() {
        return this.getHighlighted();
    }

    /**
     * Gets the suggestion text shown when the input is not at max length and not focused.
     *
     * @return the suggestion text, or null if not set
     */
    @Nullable
    public String getSuggestion() {
        return this.suggestion;
    }

    // =====================
    // Cursor Navigation
    // =====================

    public void moveCursor(int delta, boolean select) {
        int newPos = this.getCursorPos(delta);
        this.moveCursorTo(newPos, select);
    }

    private int getCursorPos(int delta) {
        String currentText = this.text.get();
        int cursor = this.cursorPos.get();
        int newPos = Character.offsetByCodePoints(currentText.toCharArray(), 0, currentText.length(), cursor, delta);
        return Mth.clamp(newPos, 0, currentText.length());
    }

    public void moveCursorTo(int position, boolean select) {
        int clampedPos = Mth.clamp(position, 0, this.text.get().length());
        this.cursorPos.set(clampedPos);
        if (!select) {
            this.highlightPos.set(clampedPos);
        }
        this.scrollToCursor();
        if (this.cursorBlink != null) {
            this.cursorBlink.setImmediate(1);
        }
    }

    private int getWordPosition(int direction) {
        return this.getWordPosition(direction, this.cursorPos.get(), true);
    }

    private int getWordPosition(int numWords, int pos, boolean skipConsecutiveSpaces) {
        String currentText = this.text.get();
        int i = pos;
        boolean movingBackward = numWords < 0;
        int wordCount = Math.abs(numWords);

        for (int k = 0; k < wordCount; k++) {
            if (!movingBackward) {
                int len = currentText.length();
                i = currentText.indexOf(32, i);
                if (i == -1) {
                    i = len;
                } else {
                    while (skipConsecutiveSpaces && i < len && currentText.charAt(i) == ' ') {
                        i++;
                    }
                }
            } else {
                while (skipConsecutiveSpaces && i > 0 && currentText.charAt(i - 1) == ' ') {
                    i--;
                }
                while (i > 0 && currentText.charAt(i - 1) != ' ') {
                    i--;
                }
            }
        }

        return i;
    }

    private void scrollToCursor() {
        var font = Minecraft.getInstance().font;
        if (font == null) return;

        String currentText = this.text.get();
        int cursor = this.cursorPos.get();
        int display = this.displayPos.get();
        int innerWidth = this.getInnerWidth();

        display = Math.min(display, currentText.length());

        if (cursor < display) {
            this.displayPos.set(cursor);
            return;
        }

        String visibleText = currentText.substring(display);
        int cursorOffset = cursor - display;

        if (cursorOffset <= visibleText.length()) {
            String textToCursor = visibleText.substring(0, cursorOffset);
            if (font.width(textToCursor) > innerWidth) {
                while (display < cursor) {
                    display++;
                    String newVisible = currentText.substring(display, cursor);
                    if (font.width(newVisible) <= innerWidth) break;
                }
                this.displayPos.set(display);
            }
        }
    }

    private int findClickPosition(int clickX) {
        var font = Minecraft.getInstance().font;
        Bounds bounds = this.getBounds();
        if (bounds == null || font == null) return 0;

        int textStartX = bounds.x() + (this.bordered ? 4 : 0);
        int relativeX = clickX - textStartX;

        if (relativeX < 0) return this.displayPos.get();

        String currentText = this.text.get();
        int display = this.displayPos.get();
        String visibleText = currentText.substring(Math.min(display, currentText.length()));

        for (int i = 0; i <= visibleText.length(); i++) {
            String sub = visibleText.substring(0, i);
            if (font.width(sub) >= relativeX) {
                return display + i;
            }
        }

        return display + visibleText.length();
    }

    private int getInnerWidth() {
        Bounds bounds = this.getBounds();
        if (bounds == null) return 100;
        return bounds.width() - (this.bordered ? 8 : 0);
    }

    // =====================
    // Clipboard Operations
    // =====================

    private void copyToClipboard() {
        String selected = this.getHighlighted();
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

    private void onValueChanged() {
        if (this.responder != null) {
            this.responder.accept(this.text.get());
        }
    }

    // =====================
    // Configuration
    // =====================

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

    public TextInput setBordered(boolean bordered) {
        this.bordered = bordered;
        return this;
    }

    public TextInput setEditable(boolean editable) {
        this.editable = editable;
        return this;
    }

    public TextInput setCentered(boolean centered) {
        this.centered = centered;
        return this;
    }

    public TextInput setTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }

    public TextInput setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    public TextInput setTextColorUneditable(int color) {
        this.textColorUneditable = color;
        return this;
    }

    public TextInput setFilter(Predicate<String> filter) {
        this.filter = filter;
        return this;
    }

    public TextInput setResponder(Consumer<String> responder) {
        this.responder = responder;
        return this;
    }

    public TextInput setHint(@Nullable Component hint) {
        this.hint = hint;
        return this;
    }

    public TextInput setSuggestion(@Nullable String suggestion) {
        this.suggestion = suggestion;
        return this;
    }

    public TextInput setCanLoseFocus(boolean canLoseFocus) {
        this.canLoseFocus = canLoseFocus;
        return this;
    }

    public TextInput addFormatter(TextFormatter formatter) {
        this.formatters.add(formatter);
        return this;
    }

    public TextInput clearFormatters() {
        this.formatters.clear();
        return this;
    }

    // =====================
    // Getters
    // =====================

    public State<String> getTextState() {
        return this.text;
    }

    public State<Boolean> getFocusedState() {
        return this.focused;
    }

    public State<Integer> getCursorPosState() {
        return this.cursorPos;
    }

    public boolean isEditable() {
        return this.editable;
    }

    public boolean isBordered() {
        return this.bordered;
    }

    public boolean isCentered() {
        return this.centered;
    }

    public boolean getTextShadow() {
        return this.textShadow;
    }

    public boolean isFocused() {
        return this.focused.get();
    }

    public boolean canLoseFocus() {
        return this.canLoseFocus;
    }

    // =====================
    // Layout
    // =====================

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
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
        return List.of(bounds);
    }

    @Override
    public CursorType getCursor(int x, int y) {
        return this.editable ? CompoundCursors.IBEAM : null;
    }

    // =====================
    // TextFormatter Interface
    // =====================

    @FunctionalInterface
    public interface TextFormatter {
        @Nullable
        FormattedCharSequence format(String text, int displayPos);
    }
}
