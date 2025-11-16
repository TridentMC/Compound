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

import com.tridevmc.compound.ui.compose.layout.*;
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
    private static boolean warnedMultipleChildren = false;

    public Box() {
    }

    @Override
    public Size measure(Constraints constraints, List<IElement> children) {
        var props = this.getLayoutProperties();
        int horizontalPadding = props.getPaddingLeft() + props.getPaddingRight();
        int verticalPadding = props.getPaddingTop() + props.getPaddingBottom();

        if (children.isEmpty()) {
            return new Size(horizontalPadding, verticalPadding);
        }

        // Warn if multiple children are present (Box is designed for single child)
        if (children.size() > 1 && !warnedMultipleChildren) {
            LOGGER.warn("Box element contains {} children, but Box is designed for a single child. " +
                    "Only the first child will be rendered. Consider using Stack for multiple children.",
                    children.size());
            warnedMultipleChildren = true;
        }

        // Single child - use standard container pattern like Stack/Column/Row
        var contentConstraints = LayoutMath.calculateContentConstraints(constraints, props);
        var childSize = LayoutHelper.measureChild(children.getFirst(), contentConstraints);

        return new Size(
                Math.min(childSize.width() + horizontalPadding, constraints.maxWidth()),
                Math.min(childSize.height() + verticalPadding, constraints.maxHeight())
        );
    }

    @Override
    public void place(Bounds bounds, List<IElement> children) {
        this.setBounds(bounds);

        if (!children.isEmpty()) {
            var child = children.getFirst();
            var childSize = child.getBounds() != null ? child.getBounds().size() : new Size(0, 0);

            // Calculate available space after padding
            int availableWidth = bounds.width() - this.getLayoutProperties().getPaddingLeft() - this.getLayoutProperties().getPaddingRight();
            int availableHeight = bounds.height() - this.getLayoutProperties().getPaddingTop() - this.getLayoutProperties().getPaddingBottom();
            var availableSize = new Size(availableWidth, availableHeight);

            // Apply contentAlignment to position child within available space
            var alignment = this.getLayoutProperties().getContentAlignment();
            Position childOffset;
            if (alignment != null) {
                childOffset = alignment.align(childSize, availableSize);
            } else {
                // Default to top-left if no alignment specified
                childOffset = new Position(0, 0);
            }

            // Place child with padding and alignment offset
            var childBounds = new Bounds(
                    new Position(
                            bounds.x() + this.getLayoutProperties().getPaddingLeft() + childOffset.x(),
                            bounds.y() + this.getLayoutProperties().getPaddingTop() + childOffset.y()
                    ),
                    childSize
            );

            LayoutHelper.placeChild(child, childBounds);
        }
    }
}
