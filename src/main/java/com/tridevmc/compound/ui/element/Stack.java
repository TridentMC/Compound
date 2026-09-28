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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Overlays children in composition order, with later children drawn above earlier ones.
 * Children keep their measured size and share the content origin unless content alignment is set.
 * Explicit layers may change draw order.
 */
public class Stack extends BaseContainer {

    /** Creates an empty overlapping layout; configure content alignment through its scope. */
    public Stack() {
    }

    /** {@inheritDoc} */
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

        // When contentAlignment is set, expand to fill available constraints
        // so that centering is visible and meaningful
        if (props.getContentAlignment() != null) {
            maxWidth = Math.max(maxWidth, constraints.maxWidth());
            maxHeight = Math.max(maxHeight, constraints.maxHeight());
        }

        return new Size(maxWidth, maxHeight);
    }

    /** {@inheritDoc} */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
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