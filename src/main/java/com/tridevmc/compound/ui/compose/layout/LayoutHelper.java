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

package com.tridevmc.compound.ui.compose.layout;

import com.tridevmc.compound.ui.compose.element.IElement;

/**
 * Helper methods for applying layout properties to child elements.
 */
public class LayoutHelper {

    /**
     * Measures a child element with its layout properties applied.
     * Handles margin, fixed size, and min/max constraints.
     *
     * @param child       the child element to measure
     * @param constraints the parent's constraints
     * @return the measured size including margin
     */
    public static Size measureChild(IElement child, Constraints constraints) {
        var props = child.getLayoutProperties();
        if (props == null) {
            return child.measure(constraints);
        }

        // Account for margin in available space
        int marginHorizontal = props.getMarginLeft() + props.getMarginRight();
        int marginVertical = props.getMarginTop() + props.getMarginBottom();

        int availableWidth = Math.max(0, constraints.maxWidth() - marginHorizontal);
        int availableHeight = Math.max(0, constraints.maxHeight() - marginVertical);

        // Apply min/max from layout properties
        if (props.getMinWidth() != null) {
            availableWidth = Math.max(availableWidth, props.getMinWidth());
        }
        if (props.getMaxWidth() != null) {
            availableWidth = Math.min(availableWidth, props.getMaxWidth());
        }
        if (props.getMinHeight() != null) {
            availableHeight = Math.max(availableHeight, props.getMinHeight());
        }
        if (props.getMaxHeight() != null) {
            availableHeight = Math.min(availableHeight, props.getMaxHeight());
        }

        // Create constraints for child
        var childConstraints = new Constraints(0, availableWidth, 0, availableHeight);

        Size childSize;
        // Fixed size overrides measurement
        if (props.getFixedWidth() != null && props.getFixedHeight() != null) {
            childSize = new Size(props.getFixedWidth(), props.getFixedHeight());
        } else if (props.getFixedWidth() != null) {
            var measured = child.measure(childConstraints);
            childSize = new Size(props.getFixedWidth(), measured.height());
        } else if (props.getFixedHeight() != null) {
            var measured = child.measure(childConstraints);
            childSize = new Size(measured.width(), props.getFixedHeight());
        } else {
            childSize = child.measure(childConstraints);
        }

        // Apply fillMax flags
        if (props.isFillMaxWidth()) {
            childSize = new Size(availableWidth, childSize.height());
        }
        if (props.isFillMaxHeight()) {
            childSize = new Size(childSize.width(), availableHeight);
        }

        // Return size including margin
        return new Size(
                childSize.width() + marginHorizontal,
                childSize.height() + marginVertical
        );
    }

    /**
     * Places a child element with its layout properties applied.
     * Handles margin and alignment.
     *
     * @param child          the child element to place
     * @param allocatedBounds the bounds allocated by the parent (including margin)
     */
    public static void placeChild(IElement child, Bounds allocatedBounds) {
        var props = child.getLayoutProperties();
        if (props == null) {
            child.place(allocatedBounds);
            return;
        }

        // Apply margin
        int x = allocatedBounds.x() + props.getMarginLeft();
        int y = allocatedBounds.y() + props.getMarginTop();
        int width = allocatedBounds.width() - props.getMarginLeft() - props.getMarginRight();
        int height = allocatedBounds.height() - props.getMarginTop() - props.getMarginBottom();

        // Get child's actual size (without margin)
        var childBounds = child.getBounds();
        Size childSize;
        if (childBounds != null) {
            childSize = new Size(
                    childBounds.width() - props.getMarginLeft() - props.getMarginRight(),
                    childBounds.height() - props.getMarginTop() - props.getMarginBottom()
            );
        } else {
            childSize = new Size(width, height);
        }

        // Place child
        // Note: Alignment is handled by specific container types (Box, Column, Row, Stack)
        // not as a general property here
        child.place(new Bounds(new Position(x, y), childSize));
    }

    /**
     * Gets the total margin (horizontal or vertical) for a child.
     */
    public static int getHorizontalMargin(IElement child) {
        var props = child.getLayoutProperties();
        if (props == null) return 0;
        return props.getMarginLeft() + props.getMarginRight();
    }

    public static int getVerticalMargin(IElement child) {
        var props = child.getLayoutProperties();
        if (props == null) return 0;
        return props.getMarginTop() + props.getMarginBottom();
    }
}
