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

package com.tridevmc.compound.ui.layout;

/**
 * Represents position and size together.
 */
public record Bounds(Position position, Size size) {

    public Bounds(int x, int y, int width, int height) {
        this(new Position(x, y), new Size(width, height));
    }

    public int x() {
        return this.position.x();
    }

    public int y() {
        return this.position.y();
    }

    public int width() {
        return this.size.width();
    }

    public int height() {
        return this.size.height();
    }

    public int left() {
        return this.x();
    }

    public int top() {
        return this.y();
    }

    public int right() {
        return this.x() + this.width();
    }

    public int bottom() {
        return this.y() + this.height();
    }

    // Hit testing
    public boolean contains(int x, int y) {
        return x >= this.left() && x < this.right() &&
                y >= this.top() && y < this.bottom();
    }

    public boolean contains(Position position) {
        return this.contains(position.x(), position.y());
    }

    public boolean intersects(Bounds other) {
        return this.left() < other.right() &&
                this.right() > other.left() &&
                this.top() < other.bottom() &&
                this.bottom() > other.top();
    }

    /**
     * Calculates the intersection of this bounds with another bounds.
     * Returns the overlapping rectangle, or an empty bounds if no overlap.
     *
     * @param other the other bounds to intersect with
     * @return the intersection rectangle
     */
    public Bounds intersection(Bounds other) {
        int left = Math.max(this.left(), other.left());
        int top = Math.max(this.top(), other.top());
        int right = Math.min(this.right(), other.right());
        int bottom = Math.min(this.bottom(), other.bottom());

        // No intersection - return empty bounds
        if (left >= right || top >= bottom) {
            return new Bounds(0, 0, 0, 0);
        }

        return new Bounds(left, top, right - left, bottom - top);
    }

    // Utility methods
    public Bounds offset(int dx, int dy) {
        return new Bounds(this.position.offset(dx, dy), this.size);
    }

    public Bounds shrink(int amount) {
        return this.shrink(amount, amount);
    }

    public Bounds shrink(int horizontal, int vertical) {
        return new Bounds(
                this.x() + horizontal,
                this.y() + vertical,
                Math.max(0, this.width() - horizontal * 2),
                Math.max(0, this.height() - vertical * 2)
        );
    }

    public Bounds expand(int amount) {
        return new Bounds(
                this.x() - amount,
                this.y() - amount,
                this.width() + amount * 2,
                this.height() + amount * 2
        );
    }
}
