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
 * A container that lays out children vertically in a column.
 * Uses horizontalAlignment and spacing properties from LayoutProperties.
 */
public class Column extends BaseContainer {

    public Column() {
    }

    @Override
    public Size measure(Constraints constraints) {
        int maxWidth = 0;
        int totalHeight = 0;
        var children = this.getChildren();
        int spacing = this.getLayoutProperties().getSpacing();

        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);

            // Measure child with available height
            var childConstraints = new Constraints(
                    constraints.minWidth(),
                    constraints.maxWidth(),
                    0,
                    Math.max(0, constraints.maxHeight() - totalHeight)
            );

            var childSize = LayoutHelper.measureChild(child, childConstraints);
            maxWidth = Math.max(maxWidth, childSize.width());
            totalHeight += childSize.height();
            if (i < children.size() - 1) {
                totalHeight += spacing;
            }
        }

        return new Size(
                Math.min(maxWidth, constraints.maxWidth()),
                Math.min(totalHeight, constraints.maxHeight())
        );
    }

    @Override
    public void place(Bounds bounds) {
        this.setBounds(bounds);

        int y = bounds.y();
        var children = this.getChildren();
        int spacing = this.getLayoutProperties().getSpacing();
        var horizontalAlignment = this.getLayoutProperties().getHorizontalAlignment();

        for (var child : children) {
            var childSize = child.getBounds() != null ? child.getBounds().size() : new Size(0, 0);

            // Apply horizontal alignment
            int x;
            if (horizontalAlignment != null) {
                // Use alignment to calculate x offset
                var alignedPos = horizontalAlignment.align(childSize, bounds.size());
                x = bounds.x() + alignedPos.x();
            } else {
                // Default to left
                x = bounds.x();
            }

            // Place child at current y position with horizontal alignment
            var childBounds = new Bounds(
                    new Position(x, y),
                    childSize
            );

            LayoutHelper.placeChild(child, childBounds);
            y += childSize.height() + spacing;
        }
    }
}
