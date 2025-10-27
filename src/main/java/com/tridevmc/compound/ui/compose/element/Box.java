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

package com.tridevmc.compound.ui.compose.element;

import com.tridevmc.compound.ui.compose.layout.Bounds;
import com.tridevmc.compound.ui.compose.layout.Constraints;
import com.tridevmc.compound.ui.compose.layout.LayoutHelper;
import com.tridevmc.compound.ui.compose.layout.Position;
import com.tridevmc.compound.ui.compose.layout.Size;

/**
 * A container that wraps a single child with padding.
 */
public class Box extends BaseContainer {

    private int paddingLeft;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;

    public Box() {
        this(0, 0, 0, 0);
    }

    public Box(int padding) {
        this(padding, padding, padding, padding);
    }

    public Box(int horizontal, int vertical) {
        this(horizontal, vertical, horizontal, vertical);
    }

    public Box(int paddingLeft, int paddingTop, int paddingRight, int paddingBottom) {
        this.paddingLeft = paddingLeft;
        this.paddingTop = paddingTop;
        this.paddingRight = paddingRight;
        this.paddingBottom = paddingBottom;
    }

    @Override
    public Size measure(Constraints constraints) {
        var children = this.getChildren();
        if (children.isEmpty()) {
            return new Size(
                    this.paddingLeft + this.paddingRight,
                    this.paddingTop + this.paddingBottom
            );
        }

        // Single child - measure with reduced constraints
        var child = children.getFirst();
        int horizontalPadding = this.paddingLeft + this.paddingRight;
        int verticalPadding = this.paddingTop + this.paddingBottom;

        var childConstraints = new Constraints(
                Math.max(0, constraints.minWidth() - horizontalPadding),
                Math.max(0, constraints.maxWidth() - horizontalPadding),
                Math.max(0, constraints.minHeight() - verticalPadding),
                Math.max(0, constraints.maxHeight() - verticalPadding)
        );

        var childSize = LayoutHelper.measureChild(child, childConstraints);

        return new Size(
                Math.min(childSize.width() + horizontalPadding, constraints.maxWidth()),
                Math.min(childSize.height() + verticalPadding, constraints.maxHeight())
        );
    }

    @Override
    public void place(Bounds bounds) {
        this.setBounds(bounds);

        var children = this.getChildren();
        if (!children.isEmpty()) {
            var child = children.get(0);
            var childSize = child.getBounds() != null ? child.getBounds().size() : new Size(0, 0);

            // Place child with padding offset
            var childBounds = new Bounds(
                    new Position(bounds.x() + this.paddingLeft, bounds.y() + this.paddingTop),
                    childSize
            );

            LayoutHelper.placeChild(child, childBounds);
        }
    }

    public int getPaddingLeft() {
        return this.paddingLeft;
    }

    public void setPaddingLeft(int paddingLeft) {
        this.paddingLeft = paddingLeft;
    }

    public int getPaddingTop() {
        return this.paddingTop;
    }

    public void setPaddingTop(int paddingTop) {
        this.paddingTop = paddingTop;
    }

    public int getPaddingRight() {
        return this.paddingRight;
    }

    public void setPaddingRight(int paddingRight) {
        this.paddingRight = paddingRight;
    }

    public int getPaddingBottom() {
        return this.paddingBottom;
    }

    public void setPaddingBottom(int paddingBottom) {
        this.paddingBottom = paddingBottom;
    }
}
