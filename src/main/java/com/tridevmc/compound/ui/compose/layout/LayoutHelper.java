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
import com.tridevmc.compound.ui.compose.tree.ITreeNode;

import java.util.List;

/**
 * Helper methods for applying layout properties to child elements.
 */
public class LayoutHelper {

    /**
     * Gets the children of an element from the tree.
     * Used when a parent needs to measure/place a child and needs to provide grandchildren.
     */
    private static List<IElement> getChildrenFromTree(IElement element) {
        var tree = element.getTree();
        if (tree == null) {
            return List.of();
        }
        var node = tree.getNodeForElement(element);
        if (node == null) {
            return List.of();
        }
        return node.getChildren().stream()
                .map(ITreeNode::getElement)
                .toList();
    }

    /**
     * Calculates an element's final size based on its intrinsic size, layout properties, and parent constraints.
     * This is a common helper for elements to determine their own size.
     *
     * @param intrinsicWidth  the element's preferred/natural width
     * @param intrinsicHeight the element's preferred/natural height
     * @param props           the element's layout properties (can be null)
     * @param constraints     the parent's constraints
     * @return the final size respecting properties and constraints
     */
    public static Size calculateSizeWithProperties(
            int intrinsicWidth,
            int intrinsicHeight,
            LayoutProperties props,
            Constraints constraints
    ) {
        int finalWidth;
        int finalHeight;

        if (props != null) {
            // Apply min/max constraints first
            int minWidth = props.getMinWidth() != null ? props.getMinWidth() : 0;
            int minHeight = props.getMinHeight() != null ? props.getMinHeight() : 0;
            int maxWidth = props.getMaxWidth() != null ? Math.min(props.getMaxWidth(), constraints.maxWidth()) : constraints.maxWidth();
            int maxHeight = props.getMaxHeight() != null ? Math.min(props.getMaxHeight(), constraints.maxHeight()) : constraints.maxHeight();

            // Fixed size takes precedence, but respects min/max
            if (props.getFixedWidth() != null && props.getFixedHeight() != null) {
                finalWidth = Math.max(minWidth, Math.min(maxWidth, props.getFixedWidth()));
                finalHeight = Math.max(minHeight, Math.min(maxHeight, props.getFixedHeight()));
            } else if (props.getFixedWidth() != null) {
                finalWidth = Math.max(minWidth, Math.min(maxWidth, props.getFixedWidth()));
                finalHeight = props.isFillMaxHeight() ? maxHeight : Math.max(minHeight, Math.min(maxHeight, constraints.constrainHeight(intrinsicHeight)));
            } else if (props.getFixedHeight() != null) {
                finalWidth = props.isFillMaxWidth() ? maxWidth : Math.max(minWidth, Math.min(maxWidth, constraints.constrainWidth(intrinsicWidth)));
                finalHeight = Math.max(minHeight, Math.min(maxHeight, props.getFixedHeight()));
            } else {
                // No fixed size - check fill max flags, respecting min/max
                finalWidth = props.isFillMaxWidth() ? maxWidth : Math.max(minWidth, Math.min(maxWidth, constraints.constrainWidth(intrinsicHeight)));
                finalHeight = props.isFillMaxHeight() ? maxHeight : Math.max(minHeight, Math.min(maxHeight, constraints.constrainHeight(intrinsicHeight)));
            }
        } else {
            // No properties - use intrinsic size constrained to parent
            finalWidth = constraints.constrainWidth(intrinsicWidth);
            finalHeight = constraints.constrainHeight(intrinsicHeight);
        }

        return new Size(finalWidth, finalHeight);
    }

    /**
     * Measures a child element with its layout properties applied.
     * Handles margin, fixed size, and min/max constraints.
     * Automatically gets the child's children from the tree.
     *
     * @param child       the child element to measure
     * @param constraints the parent's constraints
     * @return the measured size including margin
     */
    public static Size measureChild(IElement child, Constraints constraints) {
        var props = child.getLayoutProperties();
        var grandchildren = getChildrenFromTree(child);

        if (props == null) {
            return child.measure(constraints, grandchildren);
        }

        // Account for margin in available space
        int marginHorizontal = props.getMarginLeft() + props.getMarginRight();
        int marginVertical = props.getMarginTop() + props.getMarginBottom();

        int availableWidth = Math.max(0, constraints.maxWidth() - marginHorizontal);
        int availableHeight = Math.max(0, constraints.maxHeight() - marginVertical);

        // Calculate effective minimum constraints, accounting for margin
        int effectiveMinWidth = Math.max(0, constraints.minWidth() - marginHorizontal);
        int effectiveMinHeight = Math.max(0, constraints.minHeight() - marginVertical);

        // Apply min/max from layout properties (these are content constraints, not including margin)
        int contentMinWidth = effectiveMinWidth;
        int contentMaxWidth = availableWidth;
        int contentMinHeight = effectiveMinHeight;
        int contentMaxHeight = availableHeight;

        if (props.getMinWidth() != null) {
            contentMinWidth = Math.max(contentMinWidth, props.getMinWidth());
        }
        if (props.getMaxWidth() != null) {
            contentMaxWidth = Math.min(contentMaxWidth, props.getMaxWidth());
        }
        if (props.getMinHeight() != null) {
            contentMinHeight = Math.max(contentMinHeight, props.getMinHeight());
        }
        if (props.getMaxHeight() != null) {
            contentMaxHeight = Math.min(contentMaxHeight, props.getMaxHeight());
        }

        // Create constraints for child's content area
        var childConstraints = new Constraints(
                contentMinWidth,
                contentMaxWidth,
                contentMinHeight,
                contentMaxHeight
        );

        Size childSize;
        // Always measure children first to ensure they're properly set up
        var measuredSize = child.measure(childConstraints, grandchildren);

        // Fixed size overrides measurement (these are content sizes, not including margin)
        if (props.getFixedWidth() != null && props.getFixedHeight() != null) {
            childSize = new Size(props.getFixedWidth(), props.getFixedHeight());
        } else if (props.getFixedWidth() != null) {
            childSize = new Size(props.getFixedWidth(), measuredSize.height());
        } else if (props.getFixedHeight() != null) {
            childSize = new Size(measuredSize.width(), props.getFixedHeight());
        } else {
            childSize = measuredSize;
        }

        // Apply fillMax flags (to content area, not including margin)
        if (props.isFillMaxWidth()) {
            childSize = new Size(contentMaxWidth, childSize.height());
        }
        if (props.isFillMaxHeight()) {
            childSize = new Size(childSize.width(), contentMaxHeight);
        }

        // Return total size including margin
        return new Size(
                childSize.width() + marginHorizontal,
                childSize.height() + marginVertical
        );
    }

    /**
     * Places a child element with its layout properties applied.
     * Handles margin and alignment.
     * Automatically gets the child's children from the tree.
     *
     * @param child          the child element to place
     * @param allocatedBounds the bounds allocated by the parent (including margin)
     */
    public static void placeChild(IElement child, Bounds allocatedBounds) {
        var props = child.getLayoutProperties();
        var grandchildren = getChildrenFromTree(child);

        if (props == null) {
            child.place(allocatedBounds, grandchildren);
            return;
        }

        // Calculate margin values
        int marginLeft = props.getMarginLeft();
        int marginTop = props.getMarginTop();
        int marginRight = props.getMarginRight();
        int marginBottom = props.getMarginBottom();

        // Calculate content area (total bounds minus margins)
        int contentX = allocatedBounds.x() + marginLeft;
        int contentY = allocatedBounds.y() + marginTop;
        int contentWidth = allocatedBounds.width() - marginLeft - marginRight;
        int contentHeight = allocatedBounds.height() - marginTop - marginBottom;

        // Get the child's actual measured content size
        // Note: The child's bounds should already be set to content size, not total size including margin
        var childBounds = child.getBounds();
        Size childSize;
        if (childBounds != null) {
            // Child bounds should already be the content size
            childSize = childBounds.size();
        } else {
            // Fallback: use available content area
            childSize = new Size(contentWidth, contentHeight);
        }

        // Place child in content area with its measured size
        // Note: Alignment is handled by specific container types (Column, Row, Stack)
        // not as a general property here
        child.place(new Bounds(new Position(contentX, contentY), childSize), grandchildren);
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
