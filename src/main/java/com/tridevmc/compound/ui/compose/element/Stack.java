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
import com.tridevmc.compound.ui.compose.layout.LayoutMath;
import com.tridevmc.compound.ui.compose.layout.LayoutProperties;
import com.tridevmc.compound.ui.compose.layout.Position;
import com.tridevmc.compound.ui.compose.layout.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * A container that stacks children in z-order (layering).
 * Uses contentAlignment for positioning children within the available space.
 */
public class Stack extends BaseContainer {

    // Store measured child sizes between measure and place phases
    private final List<Size> measuredChildSizes = new ArrayList<>();

    public Stack() {
    }

    @Override
    public Size measure(Constraints constraints, List<IElement> children) {
        this.measuredChildSizes.clear();

        var props = this.getLayoutProperties();

        if (children.isEmpty()) {
            return constraints.constrain(calculateEmptySize(props));
        }

        // Calculate content constraints (excluding padding)
        var contentConstraints = LayoutMath.calculateContentConstraints(constraints, props);

        // Measure all children with the same constraints
        int maxWidth = 0;
        int maxHeight = 0;

        for (var child : children) {
            var childSize = LayoutHelper.measureChild(child, contentConstraints);
            this.measuredChildSizes.add(childSize);

            maxWidth = Math.max(maxWidth, childSize.width());
            maxHeight = Math.max(maxHeight, childSize.height());
        }

        // Total size includes padding
        int horizontalPadding = props.getPaddingLeft() + props.getPaddingRight();
        int verticalPadding = props.getPaddingTop() + props.getPaddingBottom();
        int totalWidth = maxWidth + horizontalPadding;
        int totalHeight = maxHeight + verticalPadding;

        return constraints.constrain(new Size(totalWidth, totalHeight));
    }

    @Override
    public void place(Bounds bounds, List<IElement> children) {
        this.setBounds(bounds);

        var props = this.getLayoutProperties();
        var contentAlignment = props.getContentAlignment();

        if (this.measuredChildSizes.isEmpty()) {
            return;
        }

        // Calculate content area (excluding padding)
        var contentArea = LayoutMath.calculateContentArea(bounds, props);

        // Place all children with alignment using measured sizes
        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);
            Size childSize = this.measuredChildSizes.get(i);

            Bounds childBounds;
            if (contentAlignment != null) {
                // Apply contentAlignment to position child within content area
                var alignedPos = contentAlignment.align(childSize, contentArea.size());
                childBounds = new Bounds(
                        new Position(contentArea.x() + alignedPos.x(), contentArea.y() + alignedPos.y()),
                        childSize
                );
            } else {
                // Default: place at top-left of content area
                childBounds = new Bounds(new Position(contentArea.x(), contentArea.y()), childSize);
            }

            LayoutHelper.placeChild(child, childBounds);
        }
    }

    private Size calculateEmptySize(LayoutProperties props) {
        int horizontalPadding = props.getPaddingLeft() + props.getPaddingRight();
        int verticalPadding = props.getPaddingTop() + props.getPaddingBottom();
        return new Size(horizontalPadding, verticalPadding);
    }
}