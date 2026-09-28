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

package com.tridevmc.compound.ui.layout;

/**
 * How to align children within available space.
 */
public enum Alignment {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT;

    /**
     * Calculate position for a child with given size in a parent with given size.
     *
     * @param childSize  the size of the child element
     * @param parentSize the size of the parent element
     * @return the position where the child should be placed
     */
    public Position align(Size childSize, Size parentSize) {
        int x = switch (this) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> 0;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> (parentSize.width() - childSize.width()) / 2;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> parentSize.width() - childSize.width();
        };

        int y = switch (this) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> (parentSize.height() - childSize.height()) / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> parentSize.height() - childSize.height();
        };

        return new Position(x, y);
    }
}
