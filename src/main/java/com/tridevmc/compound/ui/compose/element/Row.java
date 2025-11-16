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

import com.tridevmc.compound.ui.compose.layout.Alignment;
import com.tridevmc.compound.ui.compose.layout.Bounds;
import com.tridevmc.compound.ui.compose.layout.Constraints;
import com.tridevmc.compound.ui.compose.layout.LayoutHelper;
import com.tridevmc.compound.ui.compose.layout.LayoutMath;
import com.tridevmc.compound.ui.compose.layout.LayoutProperties;
import com.tridevmc.compound.ui.compose.layout.Position;
import com.tridevmc.compound.ui.compose.layout.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * A container that lays out children horizontally in a row.
 * Uses verticalAlignment and spacing properties from LayoutProperties.
 */
public class Row extends BaseContainer {

    // Store measured child sizes between measure and place phases
    private final List<Size> measuredChildSizes = new ArrayList<>();

    public Row() {
    }

    @Override
    public Size measure(Constraints constraints, List<IElement> children) {
        this.measuredChildSizes.clear();

        var props = this.getLayoutProperties();

        if (children.isEmpty()) {
            int horizontalPadding = props.getPaddingLeft() + props.getPaddingRight();
            int verticalPadding = props.getPaddingTop() + props.getPaddingBottom();
            return constraints.constrain(new Size(horizontalPadding, verticalPadding));
        }

        // Calculate content constraints (excluding padding)
        var contentConstraints = LayoutMath.calculateContentConstraints(constraints, props);

        // Measure all children
        int totalWidth = 0;
        int maxHeight = 0;

        for (var child : children) {
            var childSize = LayoutHelper.measureChild(child, contentConstraints);
            this.measuredChildSizes.add(childSize);

            totalWidth += childSize.width();
            maxHeight = Math.max(maxHeight, childSize.height());
        }

        // Add spacing between children
        int totalSpacing = LayoutMath.calculateTotalSpacing(children.size(), props.getSpacing());
        totalWidth += totalSpacing;

        // Add padding back to total size
        int horizontalPadding = props.getPaddingLeft() + props.getPaddingRight();
        int verticalPadding = props.getPaddingTop() + props.getPaddingBottom();

        totalWidth += horizontalPadding;
        int totalHeight = maxHeight + verticalPadding;

        return constraints.constrain(new Size(totalWidth, totalHeight));
    }

    @Override
    public void place(Bounds bounds, List<IElement> children) {
        this.setBounds(bounds);

        var props = this.getLayoutProperties();
        var verticalAlignment = props.getVerticalAlignment();

        if (this.measuredChildSizes.isEmpty()) {
            return;
        }

        // Calculate content area (excluding padding)
        var contentArea = LayoutMath.calculateContentArea(bounds, props);

        // Place children horizontally with spacing
        int currentX = contentArea.x();

        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);
            Size childSize = this.measuredChildSizes.get(i);

            // Apply vertical alignment within content area
            Bounds childBounds;
            if (verticalAlignment != null) {
                var alignedPos = LayoutMath.applyAlignment(
                    new Bounds(currentX, contentArea.y(), childSize.width(), contentArea.height()),
                    childSize, verticalAlignment, false
                );
                childBounds = new Bounds(alignedPos, childSize);
            } else {
                // Default to top-left
                childBounds = new Bounds(new Position(currentX, contentArea.y()), childSize);
            }

            LayoutHelper.placeChild(child, childBounds);
            currentX += childSize.width() + props.getSpacing();
        }
    }
}