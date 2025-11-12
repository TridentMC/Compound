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
 * A container that stacks children on top of each other (z-layering).
 * Uses contentAlignment property from LayoutProperties to align stacked children.
 */
public class Stack extends BaseContainer {

    private final java.util.List<Size> measuredChildSizes = new java.util.ArrayList<>();

    public Stack() {
    }

    @Override
    public Size measure(Constraints constraints) {
        measuredChildSizes.clear();

        int maxWidth = 0;
        int maxHeight = 0;

        // Measure all children with the same constraints and store their sizes
        for (int i = 0; i < this.getChildren().size(); i++) {
            var child = this.getChildren().get(i);
            var childSize = LayoutHelper.measureChild(child, constraints);
            measuredChildSizes.add(childSize);

            maxWidth = Math.max(maxWidth, childSize.width());
            maxHeight = Math.max(maxHeight, childSize.height());
        }

        return new Size(
                Math.min(maxWidth, constraints.maxWidth()),
                Math.min(maxHeight, constraints.maxHeight())
        );
    }

    @Override
    public void place(Bounds bounds) {
        this.setBounds(bounds);

        var contentAlignment = this.getLayoutProperties().getContentAlignment();

        // Place all children with alignment using measured sizes
        for (int i = 0; i < this.getChildren().size(); i++) {
            var child = this.getChildren().get(i);
            Size childSize = i < measuredChildSizes.size() ? measuredChildSizes.get(i) : new Size(0, 0);

            Bounds childBounds;
            if (contentAlignment != null) {
                // Apply contentAlignment to position child within available space
                var alignedPos = contentAlignment.align(childSize, bounds.size());
                childBounds = new Bounds(
                        new Position(bounds.x() + alignedPos.x(), bounds.y() + alignedPos.y()),
                        childSize
                );
            } else {
                // Default: fill entire bounds
                childBounds = bounds;
            }
            LayoutHelper.placeChild(child, childBounds);
        }
    }
}
