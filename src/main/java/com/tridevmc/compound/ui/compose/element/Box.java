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

import com.tridevmc.compound.ui.compose.layout.*;

/**
 * A container that wraps a single child with padding and alignment.
 * Uses contentAlignment and padding properties from LayoutProperties.
 */
public class Box extends BaseContainer {

    public Box() {
    }

    @Override
    public Size measure(Constraints constraints) {
        var children = this.getChildren();
        int horizontalPadding = this.getLayoutProperties().getPaddingLeft() + this.getLayoutProperties().getPaddingRight();
        int verticalPadding = this.getLayoutProperties().getPaddingTop() + this.getLayoutProperties().getPaddingBottom();

        if (children.isEmpty()) {
            return new Size(horizontalPadding, verticalPadding);
        }

        // Single child - measure with reduced constraints
        var child = children.getFirst();

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

            // Calculate available space after padding
            int availableWidth = bounds.width() - this.getLayoutProperties().getPaddingLeft() - this.getLayoutProperties().getPaddingRight();
            int availableHeight = bounds.height() - this.getLayoutProperties().getPaddingTop() - this.getLayoutProperties().getPaddingBottom();
            var availableSize = new Size(availableWidth, availableHeight);

            // Apply contentAlignment to position child within available space
            var alignment = this.getLayoutProperties().getContentAlignment();
            Position childOffset;
            if (alignment != null) {
                childOffset = alignment.align(childSize, availableSize);
            } else {
                // Default to top-left if no alignment specified
                childOffset = new Position(0, 0);
            }

            // Place child with padding and alignment offset
            var childBounds = new Bounds(
                    new Position(
                            bounds.x() + this.getLayoutProperties().getPaddingLeft() + childOffset.x(),
                            bounds.y() + this.getLayoutProperties().getPaddingTop() + childOffset.y()
                    ),
                    childSize
            );

            LayoutHelper.placeChild(child, childBounds);
        }
    }
}
