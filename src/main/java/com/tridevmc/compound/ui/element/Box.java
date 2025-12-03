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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A container that wraps a single child with padding and alignment.
 * Uses contentAlignment and padding properties from LayoutProperties.
 *
 * <p><strong>Important:</strong> Box is designed for a single child only. If multiple children
 * are added, only the first child will be rendered and a warning will be logged.</p>
 */
public class Box extends BaseContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Box.class);

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return new Size(0, 0);
        }

        if (measuredChildren.size() > 1) {
            LOGGER.debug("Box element contains {} children, but Box is designed for a single child. " +
                            "Only the first child will be rendered.",
                    measuredChildren.size());
        }

        Size childSize = measuredChildren.get(0);
        // Box behavior depends on whether it has padding:
        // - With padding: fill available space to properly apply padding and alignment
        // - Without padding: wrap content (allow child to determine size, including overflow for scrolling)
        boolean hasPadding = props.getHorizontalPadding() > 0 || props.getVerticalPadding() > 0;

        int width, height;
        if (hasPadding) {
            // Fill available space when padding is present
            width = Math.max(constraints.maxWidth(), childSize.width());
            height = Math.max(constraints.maxHeight(), childSize.height());
        } else {
            // Wrap content when no padding - just return child size
            width = childSize.width();
            height = childSize.height();
        }
        return new Size(width, height);
    }

    @Override
    public List<Bounds> place(Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }

        var contentArea = LayoutMath.calculateContentArea(bounds, props);
        var alignment = props.getContentAlignment();

        Size childSize = measuredChildren.get(0);
        Bounds childBound;

        if (alignment != null) {
            var alignedPos = alignment.align(childSize, contentArea.size());
            childBound = new Bounds(
                    new Position(contentArea.x() + alignedPos.x(), contentArea.y() + alignedPos.y()),
                    childSize
            );
        } else {
            // When no alignment is specified, child should fill the full content area
            childBound = new Bounds(contentArea.position(), contentArea.size());
        }

        return List.of(childBound);
    }
}
