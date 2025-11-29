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
 * A container that stacks children in z-order (layering).
 * Uses contentAlignment for positioning children within the available space.
 */
public class Stack extends BaseContainer {

    @Override
    public Size measure(Constraints constraints, List<IElement> children) {
        var props = this.getLayoutProperties();

        if (children.isEmpty()) {
            return constraints.constrain(new Size(props.getHorizontalPadding(), props.getVerticalPadding()));
        }

        var contentConstraints = LayoutMath.calculateContentConstraints(constraints, props);

        int maxWidth = 0;
        int maxHeight = 0;

        for (var child : children) {
            var childSize = LayoutHelper.measureChild(child, contentConstraints);
            maxWidth = Math.max(maxWidth, childSize.width());
            maxHeight = Math.max(maxHeight, childSize.height());
        }

        int totalWidth = maxWidth + props.getHorizontalPadding();
        int totalHeight = maxHeight + props.getVerticalPadding();

        return constraints.constrain(new Size(totalWidth, totalHeight));
    }

    @Override
    public void place(Bounds bounds, List<IElement> children) {
        this.setBounds(bounds);

        if (children.isEmpty()) {
            return;
        }

        var props = this.getLayoutProperties();
        var contentAlignment = props.getContentAlignment();
        var contentArea = LayoutMath.calculateContentArea(bounds, props);

        for (var child : children) {
            var childSize = LayoutHelper.getMeasuredSize(child);
            if (childSize == null) {
                continue;
            }

            Bounds childBounds;
            if (contentAlignment != null) {
                var alignedPos = contentAlignment.align(childSize, contentArea.size());
                childBounds = new Bounds(
                        new Position(contentArea.x() + alignedPos.x(), contentArea.y() + alignedPos.y()),
                        childSize
                );
            } else {
                childBounds = new Bounds(contentArea.position(), childSize);
            }

            LayoutHelper.placeChild(child, childBounds);
        }
    }
}