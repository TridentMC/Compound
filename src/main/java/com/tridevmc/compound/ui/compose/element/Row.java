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
 * A container that lays out children horizontally in a row.
 * Uses verticalAlignment and spacing properties from LayoutProperties.
 */
public class Row extends BaseContainer {

    public Row() {
    }

    @Override
    public Size measure(Constraints constraints) {
        int totalWidth = 0;
        int maxHeight = 0;
        var children = this.getChildren();
        int spacing = this.getLayoutProperties().getSpacing();

        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);

            // Measure child with available width
            var childConstraints = new Constraints(
                    0,
                    Math.max(0, constraints.maxWidth() - totalWidth),
                    constraints.minHeight(),
                    constraints.maxHeight()
            );

            var childSize = LayoutHelper.measureChild(child, childConstraints);
            totalWidth += childSize.width();
            if (i < children.size() - 1) {
                totalWidth += spacing;
            }
            maxHeight = Math.max(maxHeight, childSize.height());
        }

        return new Size(
                Math.min(totalWidth, constraints.maxWidth()),
                Math.min(maxHeight, constraints.maxHeight())
        );
    }

    @Override
    public void place(Bounds bounds) {
        this.setBounds(bounds);

        int x = bounds.x();
        var children = this.getChildren();
        int spacing = this.getLayoutProperties().getSpacing();
        var verticalAlignment = this.getLayoutProperties().getVerticalAlignment();

        for (var child : children) {
            var childSize = child.getBounds() != null ? child.getBounds().size() : new Size(0, 0);

            // Apply vertical alignment
            int y;
            if (verticalAlignment != null) {
                // Use alignment to calculate y offset
                var alignedPos = verticalAlignment.align(childSize, bounds.size());
                y = bounds.y() + alignedPos.y();
            } else {
                // Default to top
                y = bounds.y();
            }

            // Place child at current x position with vertical alignment
            var childBounds = new Bounds(
                    new Position(x, y),
                    childSize
            );

            LayoutHelper.placeChild(child, childBounds);
            x += childSize.width() + spacing;
        }
    }
}
