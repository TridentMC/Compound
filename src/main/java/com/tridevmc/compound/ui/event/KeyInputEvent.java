/*
 * Copyright 2018 - 2026 TridentMC
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

package com.tridevmc.compound.ui.event;

/**
 * Keyboard input with a physical key for navigation and a layout-dependent logical key for shortcuts.
 * ctrlDown represents the platform editing modifier: Control on Windows/Linux, Command on macOS.
 */
public record KeyInputEvent(int keyCode, int logicalKeyCode, boolean shiftDown, boolean ctrlDown, boolean altDown) {

    /** Creates an event from the legacy character-based signature. */
    public KeyInputEvent(int keyCode, char character, boolean shiftDown, boolean ctrlDown, boolean altDown) {
        this(keyCode, (int) character, shiftDown, ctrlDown, altDown);
    }

    /** Legacy character view; use logicalKeyCode for keyboard shortcuts and CharEvent for text. */
    @Deprecated
    public char character() {
        return Character.isBmpCodePoint(this.logicalKeyCode) ? (char) this.logicalKeyCode : '\0';
    }

    /** Matches an editing shortcut with the platform modifier and no Shift or Alt modifiers. */
    public boolean isShortcut(int logicalKeyCode) {
        return this.ctrlDown && !this.shiftDown && !this.altDown && this.logicalKeyCode == logicalKeyCode;
    }
}
