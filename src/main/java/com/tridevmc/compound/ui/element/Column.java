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

import java.util.ArrayList;
import java.util.List;

/**
 * A container that lays out children vertically from top to bottom.
 * Uses horizontalAlignment and spacing properties from LayoutProperties.
 */
public class Column extends BaseContainer {

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return new Size(0, 0);
        }

        int maxWidth = 0;
        int totalHeight = 0;

        for (Size childSize : measuredChildren) {
            maxWidth = Math.max(maxWidth, childSize.width());
            totalHeight += childSize.height();
        }

        totalHeight += LayoutMath.calculateTotalSpacing(measuredChildren.size(), props.getSpacing());

        return new Size(maxWidth, totalHeight);
    }

    @Override
    public List<Bounds> place(Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }

        var contentArea = LayoutMath.calculateContentArea(bounds, props);
        var horizontalAlignment = props.getHorizontalAlignment();

        List<Bounds> childBounds = new ArrayList<>();
        int currentY = contentArea.y();

        for (Size childSize : measuredChildren) {
            Bounds childBound;
            if (horizontalAlignment != null) {
                var alignedPos = LayoutMath.applyAlignment(
                        new Bounds(contentArea.x(), currentY, contentArea.width(), childSize.height()),
                        childSize, horizontalAlignment, true
                );
                childBound = new Bounds(alignedPos, childSize);
            } else {
                childBound = new Bounds(new Position(contentArea.x(), currentY), childSize);
            }

            childBounds.add(childBound);
            currentY += childSize.height() + props.getSpacing();
        }

        return childBounds;
    }
}