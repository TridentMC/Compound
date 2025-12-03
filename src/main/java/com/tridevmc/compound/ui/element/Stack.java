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
 * A container that stacks children in z-order (layering).
 * Uses contentAlignment for positioning children within the available space.
 */
public class Stack extends BaseContainer {

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return new Size(0, 0);
        }

        int maxWidth = 0;
        int maxHeight = 0;

        for (Size childSize : measuredChildren) {
            maxWidth = Math.max(maxWidth, childSize.width());
            maxHeight = Math.max(maxHeight, childSize.height());
        }

        return new Size(maxWidth, maxHeight);
    }

    @Override
    public List<Bounds> place(Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }

        var contentArea = LayoutMath.calculateContentArea(bounds, props);
        var contentAlignment = props.getContentAlignment();

        List<Bounds> childBounds = new ArrayList<>();

        for (Size childSize : measuredChildren) {
            Bounds childBound;
            if (contentAlignment != null) {
                var alignedPos = contentAlignment.align(childSize, contentArea.size());
                childBound = new Bounds(
                        new Position(contentArea.x() + alignedPos.x(), contentArea.y() + alignedPos.y()),
                        childSize
                );
            } else {
                childBound = new Bounds(contentArea.position(), childSize);
            }

            childBounds.add(childBound);
        }

        return childBounds;
    }
}