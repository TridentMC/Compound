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

package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.layout.*;

import java.util.List;

/**
 * A container that lays out children vertically from top to bottom.
 * Uses horizontalAlignment and spacing properties from LayoutProperties.
 */
public class Column extends BaseContainer {

    @Override
    public Size measure(Constraints constraints, List<IElement> children) {
        var props = this.getLayoutProperties();

        if (children.isEmpty()) {
            return constraints.constrain(new Size(props.getHorizontalPadding(), props.getVerticalPadding()));
        }

        var contentConstraints = LayoutMath.calculateContentConstraints(constraints, props);

        int maxWidth = 0;
        int totalHeight = 0;

        for (var child : children) {
            var childSize = LayoutHelper.measureChild(child, contentConstraints);
            maxWidth = Math.max(maxWidth, childSize.width());
            totalHeight += childSize.height();
        }

        int totalSpacing = LayoutMath.calculateTotalSpacing(children.size(), props.getSpacing());
        totalHeight += totalSpacing;

        int totalWidth = maxWidth + props.getHorizontalPadding();
        totalHeight += props.getVerticalPadding();

        return constraints.constrain(new Size(totalWidth, totalHeight));
    }

    @Override
    public void place(Bounds bounds, List<IElement> children) {
        this.setBounds(bounds);

        if (children.isEmpty()) {
            return;
        }

        var props = this.getLayoutProperties();
        var horizontalAlignment = props.getHorizontalAlignment();
        var contentArea = LayoutMath.calculateContentArea(bounds, props);

        int currentY = contentArea.y();

        for (var child : children) {
            var childSize = LayoutHelper.getMeasuredSize(child);
            if (childSize == null) {
                continue;
            }

            Bounds childBounds;
            if (horizontalAlignment != null) {
                var alignedPos = LayoutMath.applyAlignment(
                        new Bounds(contentArea.x(), currentY, contentArea.width(), childSize.height()),
                        childSize, horizontalAlignment, true
                );
                childBounds = new Bounds(alignedPos, childSize);
            } else {
                childBounds = new Bounds(new Position(contentArea.x(), currentY), childSize);
            }

            LayoutHelper.placeChild(child, childBounds);
            currentY += childSize.height() + props.getSpacing();
        }
    }
}