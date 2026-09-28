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
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.state.State;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A single-line editor with selection, clipboard support, formatting, and horizontal scrolling.
 * Absolute caret offsets and length limits use UTF-16 code units; relative movement uses code points.
 */
public class TextInput extends Element implements IComposableElement {

    private static final IScreenSprite SPRITE_NORMAL = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/text_field"));
    private static final IScreenSprite SPRITE_FOCUSED = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/text_field_highlighted"));

    private static final int CURSOR_INSERT_WIDTH = 1;
    /** Default editable text color. */
    public static final int DEFAULT_TEXT_COLOR = -2039584;  // 0xE0E0E0
    /** Default text color when editing is disabled. */
    public static final int DEFAULT_TEXT_COLOR_UNEDITABLE = -9408400;
    /** Default dark-gray style available for placeholder text. */
    public static final Style DEFAULT_HINT_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);

    private final State<String> text = State.of("");
    private final State<Integer> cursorPos = State.of(0);
    private final State<Integer> highlightPos = State.of(0);
    private final State<Integer> displayPos = State.of(0);
    private final State<Boolean> focused = State.of(false);

    private int maxLength = 32;
    private boolean bordered = true;
    private boolean editable = true;
    private boolean centered = false;
    private Text renderedText;
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

    private final CursorBlink cursorBlink = new CursorBlink();
    private Consumer<String> onCommit = text -> { };

    /**
     * Creates an editable single-line field with a 32-code-unit limit for subsequent edits.
     */
    public TextInput() {
    }

    /**
     * Creates an editable single-line field with a 32-code-unit limit for subsequent edits.
     *
     * @param initialValue the initial text, or null for empty; construction does not apply the length limit
     */
    public TextInput(String initialValue) {
        this.text.set(initialValue != null ? initialValue : "");
        this.cursorPos.set(this.text.get().length());
        this.highlightPos.set(this.cursorPos.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.text);
        scope.bind(this.cursorPos);
        scope.bind(this.highlightPos);
        scope.bind(this.displayPos);
        scope.bind(this.focused);

        this.cursorBlink.compose(scope);

        this.registerEventHandlers(scope);

        this.composeUI(scope);
    }

    private void registerEventHandlers(ICompositionScope scope) {
        scope.onClick(event -> {
            if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
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
            this.cursorBlink.reset();
        });

        scope.onFocusLost(() -> {
            this.focused.set(false);
            this.onCommit.accept(this.text.get());
        });
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
        this.renderedText = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getNarrationMessage() {
        return Component.translatable("gui.narrate.editBox", this.hint != null ? this.hint : Component.empty(), this.text.get());
    }

    /**
     * Sets the callback used when editing is committed by Enter or focus loss.
     *
     * @param onCommit the non-null callback receiving the committed value
     * @return this element
     */
    public TextInput setOnCommit(Consumer<String> onCommit) {
        this.onCommit = Objects.requireNonNull(onCommit);
        return this;
    }

    private void composeUI(ICompositionScope scope) {
        scope.e(new Stack(), stack -> {
            stack.layout().fillMax();

            if (this.bordered) {
                IScreenSprite bgSprite = this.focused.get() ? SPRITE_FOCUSED : SPRITE_NORMAL;
                stack.e(new Sprite(bgSprite), bg -> bg.layout().fillMax());
            }

            stack.e(new Box(), box -> {
                if (this.bordered) {
                    // A 9px line centred below a 1px top inset matches EditBox's (height - 8) / 2.
                    box.layout().padding(4, 1, 4, 0).fillMax().clip();
                } else {
                    box.layout().fillMax().clip();
                }

                box.e(new Stack(), contentStack -> {
                    var alignment = this.bordered
                            ? (this.centered ? Alignment.CENTER : Alignment.CENTER_LEFT)
                            : (this.centered ? Alignment.TOP_CENTER : Alignment.TOP_LEFT);
                    contentStack.layout().fillMax().contentAlignment(alignment);

                    // Text + cursor are overlaid in a Stack with no alignment so they share
                    // the same origin and the cursor stays aligned with the glyphs regardless
                    // of whether the parent centers the group.
                    contentStack.e(new Stack(), textGroup -> {
                        textGroup.layout().fixedHeight(Minecraft.getInstance().font.lineHeight);

                        this.composeSelection(textGroup);
                        this.renderedText = new Text(
                                () -> this.getRenderedText(),
                                () -> this.editable ? this.textColor : this.textColorUneditable,
                                () -> this.textShadow
                            );
                        textGroup.e(this.renderedText,
                            textElem -> {
                                if (!this.centered) {
                                    // Left-aligned text fills the available width so selection
                                    // highlighting extends across the content area.
                                    textElem.layout().fillMax();
                                }
                            }
                        );

                        if (this.focused.get() && this.editable) {
                            textGroup.e(new Rect(this.cursorBlink::color), cursor -> cursor.layout()
                                    .fixedSize(1, Minecraft.getInstance().font.lineHeight)
                                    .margin(this.getCursorXOffset(), 0, 0, 0));
                        }
                    });
                    if (this.suggestion != null && this.cursorPos.get() == this.text.get().length()
                            && this.text.get().length() < this.maxLength) {
                        contentStack.e(new Suggestion(), suggestion -> suggestion.layout().fillMaxWidth());
                    }
                });
            });
        });
    }

    private Component getRenderedText() {
        String currentText = this.text.get();
        boolean isFocused = this.focused.get();
        int display = this.displayPos.get();

        if (currentText.isEmpty() && !isFocused && this.hint != null) {
            return this.hint;
        }

        String visibleText = currentText.substring(Math.min(display, currentText.length()));

        FormattedCharSequence formatted = this.applyFormat(visibleText, display);
        if (formatted != null) {
            var component = Component.empty();
            formatted.accept((index, style, codePoint) -> {
                component.append(Component.literal(new String(Character.toChars(codePoint))).setStyle(style));
                return true;
            });
            return component;
        }

        return Component.literal(visibleText);
    }

    private class Suggestion extends Element implements IComposableElement {
        @Override
        public void compose(ICompositionScope scope) {
            scope.e(new Text(() -> Component.literal(TextInput.this.suggestion == null ? "" : TextInput.this.suggestion),
                    () -> 0xFF808080, () -> TextInput.this.textShadow));
        }

        @Override
        public Size measure(Constraints constraints, LayoutProperties props, List<Size> children) {
            return new Size(constraints.maxWidth(), Minecraft.getInstance().font.lineHeight);
        }

        @Override
        public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> children) {
            int x = TextInput.this.renderedText.getBounds().x() + TextInput.this.getCursorXOffset();
            return List.of(new Bounds(x, bounds.y(), Math.max(0, bounds.right() - x), bounds.height()));
        }
    }

    private int getCursorXOffset() {
        var font = Minecraft.getInstance().font;
        String currentText = this.text.get();
        int cursor = this.cursorPos.get();
        int display = this.displayPos.get();

        if (cursor < display) {
            return 0;
        }

        String visibleText = currentText.substring(Math.min(display, currentText.length()));
        int cursorOffset = cursor - display;
        int cursorInVisible = Math.min(cursorOffset, visibleText.length());

        return font.width(visibleText.substring(0, cursorInVisible));
    }

    /**
     * Configures the looping caret opacity animation and restarts it when already mounted.
     *
     * @param intervalMillis the positive duration of each fade direction in milliseconds
     * @param easing the non-null easing function
     * @return this element
     * @throws IllegalArgumentException if intervalMillis is not positive
     */
    public TextInput setCursorAnimation(long intervalMillis, Easing easing) {
        this.cursorBlink.configure(intervalMillis, easing);
        this.invalidateComposition();
        return this;
    }

    private void composeSelection(ICompositionScope scope) {
        int start = this.getSelectionStart();
        int end = this.getSelectionEnd();
        if (start < 0 || end <= start) return;
        String visible = this.text.get().substring(this.displayPos.get());
        var font = Minecraft.getInstance().font;
        int x = font.width(visible.substring(0, start));
        int width = font.width(visible.substring(start, end));
        scope.e(new Rect(0x800000FF), selection -> selection.layout()
                .fixedSize(width, font.lineHeight).margin(x, 0, 0, 0));
    }

    private int getSelectionStart() {
        String currentText = this.text.get();
        int cursor = this.cursorPos.get();
        int highlight = this.highlightPos.get();
        int display = this.displayPos.get();

        if (!this.focused.get() || cursor == highlight) return -1;

        int selStart = Math.min(cursor, highlight) - display;
        return Math.max(0, selStart);
    }

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

    private boolean handleKeyPress(KeyInputEvent event) {
        if (!this.focused.get() || !this.editable) return false;

        int keyCode = event.keyCode();
        boolean shift = event.shiftDown();
        boolean ctrl = event.ctrlDown();

        if (event.isShortcut(InputConstants.KEYCODE_A)) {
            this.moveCursorTo(this.text.get().length(), false);
            this.highlightPos.set(0);
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
            this.insertText("");
            return true;
        }


        return switch (keyCode) {
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                this.onCommit.accept(this.text.get());
                yield true;
            }
            case InputConstants.KEY_BACKSPACE -> {
                this.deleteText(-1, ctrl);
                yield true;
            }
            case InputConstants.KEY_DELETE -> {
                this.deleteText(1, ctrl);
                yield true;
            }
            case InputConstants.KEY_LEFT -> {
                if (ctrl) {
                    this.moveCursorTo(this.getWordPosition(-1), shift);
                } else {
                    this.moveCursor(-1, shift);
                }
                yield true;
            }
            case InputConstants.KEY_RIGHT -> {
                if (ctrl) {
                    this.moveCursorTo(this.getWordPosition(1), shift);
                } else {
                    this.moveCursor(1, shift);
                }
                yield true;
            }
            case InputConstants.KEY_HOME -> {
                this.moveCursorTo(0, shift);
                yield true;
            }
            case InputConstants.KEY_END -> {
                this.moveCursorTo(this.text.get().length(), shift);
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

    /**
     * Returns the current unformatted text.
     *
     * @return the current text
     */
    public String getValue() {
        return this.text.get();
    }

    /**
     * Replaces accepted text, applies the length limit, clears selection, and moves the caret to the end.
     *
     * @param newText the proposed non-null text; rejected values leave the field unchanged
     */
    public void setValue(String newText) {
        if (this.filter.test(newText)) {
            String filtered = TextEditing.truncate(newText, this.maxLength);
            this.text.set(filtered);
            this.moveCursorTo(filtered.length(), false);
            this.onValueChanged();
        }
    }

    /**
     * Replaces the selection with filtered text, limited to the available length. Rejected edits leave the value unchanged.
     *
     * @param textToInsert the text to insert; unsupported input characters are removed
     */
    public void insertText(String textToInsert) {
        String currentText = this.text.get();
        int cursor = this.cursorPos.get();
        int highlight = this.highlightPos.get();

        int start = Math.min(cursor, highlight);
        int end = Math.max(cursor, highlight);

        int availableSpace = this.maxLength - currentText.length() + (end - start);
        if (availableSpace <= 0) return;

        String toInsert = TextEditing.truncate(StringUtil.filterText(textToInsert), availableSpace);

        String newText = currentText.substring(0, start) + toInsert + currentText.substring(end);

        if (this.filter.test(newText)) {
            this.text.set(newText);
            int newCursor = start + toInsert.length();
            this.cursorPos.set(newCursor);
            this.highlightPos.set(newCursor);
            this.scrollToCursor();
            this.cursorBlink.reset();
            this.onValueChanged();
        }
    }

    /**
     * Deletes the selection, or removes code points or words relative to the caret. The filter may reject the resulting value.
     *
     * @param direction the signed number of words or code points to remove
     * @param byWord whether to delete words instead of code points
     */
    public void deleteText(int direction, boolean byWord) {
        String currentText = this.text.get();
        if (currentText.isEmpty()) return;

        int cursor = this.cursorPos.get();
        int highlight = this.highlightPos.get();

        if (cursor != highlight) {
            this.insertText("");
            return;
        }

        int deletePos = byWord ? this.getWordPosition(direction) : Util.offsetByCodepoints(currentText, cursor, direction);

        int start = Math.min(cursor, deletePos);
        int end = Math.max(cursor, deletePos);

        if (start == end) return;

        String newText = currentText.substring(0, start) + currentText.substring(end);

        if (this.filter.test(newText)) {
            this.text.set(newText);
            this.cursorPos.set(start);
            this.highlightPos.set(start);
            this.scrollToCursor();
            this.cursorBlink.reset();
            this.onValueChanged();
        }
    }

    /**
     * Returns the selected substring, or an empty string when there is no selection.
     *
     * @return the selected text
     */
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
     * Returns the selected substring, or an empty string when there is no selection.
     *
     * @return the selected text
     */
    public String getSelectedText() {
        return this.getHighlighted();
    }
    /**
     * Returns the optional suffix displayed after the text.
     *
     * @return the suffix, or null
     */
    @Nullable
    public String getSuggestion() {
        return this.suggestion;
    }

    /**
     * Moves relative to the caret by Unicode code points and reveals the new position.
     *
     * @param delta the signed number of code points to move
     * @param select whether to retain the selection anchor
     */
    public void moveCursor(int delta, boolean select) {
        int newPos = this.getCursorPos(delta);
        this.moveCursorTo(newPos, select);
    }

    private int getCursorPos(int delta) {
        String currentText = this.text.get();
        int cursor = this.cursorPos.get();
        return Util.offsetByCodepoints(currentText, cursor, delta);
    }

    /**
     * Moves to an absolute UTF-16 offset, clamped to the text range, and reveals the new position.
     *
     * @param position the absolute UTF-16 offset
     * @param select whether to retain the selection anchor
     */
    public void moveCursorTo(int position, boolean select) {
        int clampedPos = Mth.clamp(position, 0, this.text.get().length());
        this.cursorPos.set(clampedPos);
        if (!select) {
            this.highlightPos.set(clampedPos);
        }
        this.scrollToCursor();
        this.cursorBlink.reset();
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
        if (innerWidth <= 0) {
            this.displayPos.set(0);
            return;
        }

        display = Math.min(display, currentText.length());
        if (font.width(currentText) <= innerWidth) {
            this.displayPos.set(0);
            return;
        }
        this.displayPos.set(display);

        if (cursor <= display) {
            String beforeCursor = currentText.substring(0, cursor);
            this.displayPos.set(cursor - font.plainSubstrByWidth(beforeCursor, innerWidth, true).length());
            return;
        }

        String visibleText = currentText.substring(display);
        int cursorOffset = cursor - display;

        if (cursorOffset <= visibleText.length()) {
            String textToCursor = visibleText.substring(0, cursorOffset);
            if (font.width(textToCursor) > innerWidth) {
                while (display < cursor) {
                    display = Util.offsetByCodepoints(currentText, display, 1);
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

        int textStartX = this.renderedText != null
                ? this.renderedText.getBounds().x() : bounds.x() + (this.bordered ? 4 : 0);
        int relativeX = clickX - textStartX;

        if (relativeX < 0) return this.displayPos.get();

        String currentText = this.text.get();
        int display = this.displayPos.get();
        String visibleText = currentText.substring(Math.min(display, currentText.length()));

        for (int i = 0; i < visibleText.length(); i = Util.offsetByCodepoints(visibleText, i, 1)) {
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


    /**
     * Sets the UTF-16 length limit. Existing text is truncated without splitting a surrogate pair and the responder is notified.
     *
     * @param maxLength the nonnegative maximum number of UTF-16 code units
     * @return this element
     * @throws IllegalArgumentException if maxLength is negative
     */
    public TextInput setMaxLength(int maxLength) {
        if (maxLength < 0) throw new IllegalArgumentException("Maximum text length must be nonnegative");
        this.maxLength = maxLength;
        this.invalidateComposition();
        String current = this.text.get();
        if (current.length() > maxLength) {
            this.text.set(TextEditing.truncate(current, maxLength));
            this.moveCursorTo(this.text.get().length(), false);
            this.onValueChanged();
        }
        return this;
    }

    /**
     * Controls the vanilla edit-box background and its text insets.
     *
     * @param bordered whether to draw the vanilla edit-box background
     * @return this element
     */
    public TextInput setBordered(boolean bordered) {
        this.bordered = bordered;
        this.invalidateComposition();
        return this;
    }

    /**
     * Controls editing and keyboard focus eligibility; programmatic value changes remain available.
     *
     * @param editable whether to accept user edits
     * @return this element
     */
    public TextInput setEditable(boolean editable) {
        this.editable = editable;
        this.invalidateComposition();
        return this;
    }

    /**
     * Controls horizontal centering of the visible text and caret.
     *
     * @param centered whether to center the text
     * @return this element
     */
    public TextInput setCentered(boolean centered) {
        this.centered = centered;
        this.invalidateComposition();
        return this;
    }

    /**
     * Controls the text shadow.
     *
     * @param textShadow whether to draw text shadows
     * @return this element
     */
    public TextInput setTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }

    /**
     * Sets the text color.
     *
     * @param color the drawing color
     * @return this element
     */
    public TextInput setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    /**
     * Sets the text color used while editing is disabled.
     *
     * @param color the drawing color
     * @return this element
     */
    public TextInput setTextColorUneditable(int color) {
        this.textColorUneditable = color;
        return this;
    }

    /**
     * Sets the predicate that accepts or rejects proposed complete text values.
     *
     * @param filter the non-null predicate for proposed complete values
     * @return this element
     */
    public TextInput setFilter(Predicate<String> filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Sets the callback notified after accepted value changes.
     *
     * @param responder the callback receiving accepted values, or null to disable notifications
     * @return this element
     */
    public TextInput setResponder(Consumer<String> responder) {
        this.responder = responder;
        return this;
    }

    /**
     * Sets the placeholder displayed while the field is empty and unfocused.
     *
     * @param hint the placeholder, or null to clear it
     * @return this element
     */
    public TextInput setHint(@Nullable Component hint) {
        this.hint = hint;
        return this;
    }

    /**
     * Sets a display-only suffix shown when the caret is at the end and the length limit has not been reached.
     *
     * @param suggestion the display-only suffix, or null to clear it
     * @return this element
     */
    public TextInput setSuggestion(@Nullable String suggestion) {
        this.suggestion = suggestion;
        this.invalidateComposition();
        return this;
    }


    /**
     * Appends a formatter. The first formatter returning a non-null sequence supplies the rendered text.
     *
     * @param formatter the non-null formatter appended after existing formatters
     * @return this element
     */
    public TextInput addFormatter(TextFormatter formatter) {
        this.formatters.add(formatter);
        return this;
    }

    /**
     * Removes all custom formatters, restoring plain text rendering.
     *
     * @return this element
     */
    public TextInput clearFormatters() {
        this.formatters.clear();
        return this;
    }

    /**
     * Returns the live text state. Prefer setValue so validation, selection, and responder updates stay synchronized.
     *
     * @return the live state; changes notify its observers
     */
    public State<String> getTextState() {
        return this.text;
    }

    /**
     * Returns the live focus state maintained by tree focus events.
     *
     * @return the live state; changes notify its observers
     */
    public State<Boolean> getFocusedState() {
        return this.focused;
    }

    /**
     * Returns the live UTF-16 caret offset. Prefer moveCursorTo to keep selection and scrolling synchronized.
     *
     * @return the live state; changes notify its observers
     */
    public State<Integer> getCursorPosState() {
        return this.cursorPos;
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
     * Returns whether the vanilla edit-box background is enabled.
     *
     * @return true when the background is drawn
     */
    public boolean isBordered() {
        return this.bordered;
    }

    /**
     * Returns whether the visible text is centered.
     *
     * @return true when text is centered
     */
    public boolean isCentered() {
        return this.centered;
    }

    /**
     * Returns whether text shadows are enabled.
     *
     * @return true when text shadows are drawn
     */
    public boolean getTextShadow() {
        return this.textShadow;
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
                Math.min(20, constraints.maxHeight())
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
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

    /** Supplies styled rendering for a visible text suffix without changing the stored value. */
    @FunctionalInterface
    public interface TextFormatter {
        /**
         * Formats the visible suffix. Return null to let the next formatter handle it.
         *
         * @param text the visible text beginning at displayPos
         * @param displayPos the suffix's UTF-16 offset in the complete value
         * @return the styled sequence, or null to decline formatting
         */
        @Nullable
        FormattedCharSequence format(String text, int displayPos);
    }
}
