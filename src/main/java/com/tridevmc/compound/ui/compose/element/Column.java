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
 * A container that lays out children vertically from top to bottom.
 * Uses horizontalAlignment and spacing properties from LayoutProperties.
 */
public class Column extends BaseContainer {

    // Store measured child sizes between measure and place phases
    private final List<Size> measuredChildSizes = new ArrayList<>();

    public Column() {
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
        int maxWidth = 0;
        int totalHeight = 0;

        for (var child : children) {
            var childSize = LayoutHelper.measureChild(child, contentConstraints);
            this.measuredChildSizes.add(childSize);

            maxWidth = Math.max(maxWidth, childSize.width());
            totalHeight += childSize.height();
        }

        // Add spacing between children
        int totalSpacing = LayoutMath.calculateTotalSpacing(children.size(), props.getSpacing());
        totalHeight += totalSpacing;

        // Add padding back to total size
        int horizontalPadding = props.getPaddingLeft() + props.getPaddingRight();
        int verticalPadding = props.getPaddingTop() + props.getPaddingBottom();

        int totalWidth = maxWidth + horizontalPadding;
        totalHeight += verticalPadding;

        return constraints.constrain(new Size(totalWidth, totalHeight));
    }

    @Override
    public void place(Bounds bounds, List<IElement> children) {
        this.setBounds(bounds);

        var props = this.getLayoutProperties();
        var horizontalAlignment = props.getHorizontalAlignment();

        if (this.measuredChildSizes.isEmpty()) {
            return;
        }

        // Calculate content area (excluding padding)
        var contentArea = LayoutMath.calculateContentArea(bounds, props);

        // Place children vertically with spacing
        int currentY = contentArea.y();

        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);
            Size childSize = this.measuredChildSizes.get(i);

            // Apply horizontal alignment within content area
            Bounds childBounds;
            if (horizontalAlignment != null) {
                var alignedPos = LayoutMath.applyAlignment(
                    new Bounds(contentArea.x(), currentY, contentArea.width(), childSize.height()),
                    childSize, horizontalAlignment, true
                );
                childBounds = new Bounds(alignedPos, childSize);
            } else {
                // Default to top-left
                childBounds = new Bounds(new Position(contentArea.x(), currentY), childSize);
            }

            LayoutHelper.placeChild(child, childBounds);
            currentY += childSize.height() + props.getSpacing();
        }
    }
}