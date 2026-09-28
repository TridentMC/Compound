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
 * Measurement constraints passed down during the measure phase.
 */
public record Constraints(int minWidth, int maxWidth, int minHeight, int maxHeight) {

    public Constraints {
        minWidth = Math.max(0, minWidth);
        maxWidth = Math.max(minWidth, maxWidth);
        minHeight = Math.max(0, minHeight);
        maxHeight = Math.max(minHeight, maxHeight);
    }

    // Static factory methods
    public static Constraints unbounded() {
        return new Constraints(0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
    }

    public static Constraints fixed(int width, int height) {
        return new Constraints(width, width, height, height);
    }

    public static Constraints loose(int maxWidth, int maxHeight) {
        return new Constraints(0, maxWidth, 0, maxHeight);
    }

    public static Constraints tight(int width, int height) {
        return fixed(width, height);
    }

    // Validation methods
    public boolean hasFixedWidth() {
        return this.minWidth == this.maxWidth;
    }

    public boolean hasFixedHeight() {
        return this.minHeight == this.maxHeight;
    }

    public boolean hasBoundedWidth() {
        return this.maxWidth < Integer.MAX_VALUE;
    }

    public boolean hasBoundedHeight() {
        return this.maxHeight < Integer.MAX_VALUE;
    }

    public boolean isUnbounded() {
        return !this.hasBoundedWidth() && !this.hasBoundedHeight();
    }

    // Constrain a size to fit within these constraints
    public Size constrain(Size size) {
        int width = Math.max(this.minWidth, Math.min(this.maxWidth, size.width()));
        int height = Math.max(this.minHeight, Math.min(this.maxHeight, size.height()));
        return new Size(width, height);
    }

    // Constrain individual dimensions
    public int constrainWidth(int width) {
        return Math.max(this.minWidth, Math.min(this.maxWidth, width));
    }

    public int constrainHeight(int height) {
        return Math.max(this.minHeight, Math.min(this.maxHeight, height));
    }

    // Create variations
    public Constraints withMaxWidth(int maxWidth) {
        return new Constraints(this.minWidth, maxWidth, this.minHeight, this.maxHeight);
    }

    public Constraints withMaxHeight(int maxHeight) {
        return new Constraints(this.minWidth, this.maxWidth, this.minHeight, maxHeight);
    }

    public Constraints withMinWidth(int minWidth) {
        return new Constraints(minWidth, this.maxWidth, this.minHeight, this.maxHeight);
    }

    public Constraints withMinHeight(int minHeight) {
        return new Constraints(this.minWidth, this.maxWidth, minHeight, this.maxHeight);
    }

    public Constraints withFixedWidth(int width) {
        return new Constraints(width, width, this.minHeight, this.maxHeight);
    }

    public Constraints withFixedHeight(int height) {
        return new Constraints(this.minWidth, this.maxWidth, height, height);
    }

    public Constraints withFixedSize(int width, int height) {
        return new Constraints(width, width, height, height);
    }

    /** Removes total axis insets while preserving unbounded maximum constraints. */
    public Constraints inset(long horizontal, long vertical) {
        return new Constraints(
                subtractInset(this.minWidth, horizontal),
                this.hasBoundedWidth() ? subtractInset(this.maxWidth, horizontal) : Integer.MAX_VALUE,
                subtractInset(this.minHeight, vertical),
                this.hasBoundedHeight() ? subtractInset(this.maxHeight, vertical) : Integer.MAX_VALUE
        );
    }

    /** Subtracts an inset without overflowing or producing a negative size. */
    public static int subtractInset(int size, long inset) {
        if (inset >= size) return 0;
        if (inset <= (long) size - Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) (size - inset);
    }

    public Constraints deflate(int horizontal, int vertical) {
        return this.inset((long) horizontal * 2, (long) vertical * 2);
    }
}
