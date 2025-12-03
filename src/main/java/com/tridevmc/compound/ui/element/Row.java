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
 * A container that lays out children horizontally in a row.
 * Uses verticalAlignment and spacing properties from LayoutProperties.
 */
public class Row extends BaseContainer {

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return new Size(0, 0);
        }

        int totalWidth = 0;
        int maxHeight = 0;

        for (Size childSize : measuredChildren) {
            totalWidth += childSize.width();
            maxHeight = Math.max(maxHeight, childSize.height());
        }

        totalWidth += LayoutMath.calculateTotalSpacing(measuredChildren.size(), props.getSpacing());

        return new Size(totalWidth, maxHeight);
    }

    @Override
    public List<Bounds> place(Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }

        var contentArea = LayoutMath.calculateContentArea(bounds, props);
        var verticalAlignment = props.getVerticalAlignment();

        List<Bounds> childBounds = new ArrayList<>();
        int currentX = contentArea.x();

        for (Size childSize : measuredChildren) {
            Bounds childBound;
            if (verticalAlignment != null) {
                var alignedPos = LayoutMath.applyAlignment(
                        new Bounds(currentX, contentArea.y(), childSize.width(), contentArea.height()),
                        childSize, verticalAlignment, false
                );
                childBound = new Bounds(alignedPos, childSize);
            } else {
                childBound = new Bounds(new Position(currentX, contentArea.y()), childSize);
            }

            childBounds.add(childBound);
            currentX += childSize.width() + props.getSpacing();
        }

        return childBounds;
    }
}