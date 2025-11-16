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

/**
 * Utility class for common layout calculations.
 * Provides reusable methods to eliminate code duplication across containers.
 */
public final class LayoutMath {

    private LayoutMath() {
        // Utility class - no instances
    }

    /**
     * Calculates content area after removing padding from bounds.
     *
     * @param bounds the outer bounds
     * @param props  the layout properties containing padding
     * @return the content area bounds (same position, reduced size)
     */
    public static Bounds calculateContentArea(Bounds bounds, LayoutProperties props) {
        int contentX = bounds.x() + props.getPaddingLeft();
        int contentY = bounds.y() + props.getPaddingTop();
        int contentWidth = bounds.width() - props.getPaddingLeft() - props.getPaddingRight();
        int contentHeight = bounds.height() - props.getPaddingTop() - props.getPaddingBottom();

        return new Bounds(new Position(contentX, contentY), new Size(contentWidth, contentHeight));
    }

    /**
     * Calculates content area size after removing padding from constraints.
     *
     * @param constraints the original constraints
     * @param props       the layout properties containing padding
     * @return the content constraints (reduced for padding)
     */
    public static Constraints calculateContentConstraints(Constraints constraints, LayoutProperties props) {
        int horizontalPadding = props.getPaddingLeft() + props.getPaddingRight();
        int verticalPadding = props.getPaddingTop() + props.getPaddingBottom();

        return new Constraints(
                Math.max(0, constraints.minWidth() - horizontalPadding),
                Math.max(0, constraints.maxWidth() - horizontalPadding),
                Math.max(0, constraints.minHeight() - verticalPadding),
                Math.max(0, constraints.maxHeight() - verticalPadding)
        );
    }

    /**
     * Aggregates sizes for linear layout (Column/Row) with spacing.
     *
     * @param sizes   the list of child sizes
     * @param spacing the spacing between elements
     * @param isStack if true, uses max size; if false, accumulates size
     * @return the aggregated size
     */
    public static Size aggregateLinearSizes(java.util.List<Size> sizes, int spacing, boolean isStack) {
        if (sizes.isEmpty()) {
            return new Size(0, 0);
        }

        if (isStack) {
            // Stack uses the maximum size in each dimension
            int maxWidth = 0;
            int maxHeight = 0;
            for (Size size : sizes) {
                maxWidth = Math.max(maxWidth, size.width());
                maxHeight = Math.max(maxHeight, size.height());
            }
            return new Size(maxWidth, maxHeight);
        } else {
            // Linear layout accumulates size along one axis
            int totalWidth = 0;
            int totalHeight = 0;
            int maxCrossSize = 0;

            for (Size size : sizes) {
                totalWidth += size.width();
                totalHeight += size.height();
                // This assumes horizontal layout - for vertical layout, swap dimensions
                maxCrossSize = Math.max(maxCrossSize, size.height());
            }

            return new Size(totalWidth, maxCrossSize);
        }
    }

    /**
     * Calculates total spacing for a linear layout.
     *
     * @param childCount the number of children
     * @param spacing    the spacing between elements
     * @return total spacing needed
     */
    public static int calculateTotalSpacing(int childCount, int spacing) {
        return Math.max(0, (childCount - 1) * spacing);
    }

    /**
     * Applies alignment to position a child within a content area.
     *
     * @param contentArea     the content area bounds
     * @param childSize       the child's size
     * @param alignment       the alignment to apply
     * @param isHorizontal    if true, align horizontally; if false, align vertically
     * @return the position for the child
     */
    public static Position applyAlignment(Bounds contentArea, Size childSize, Alignment alignment, boolean isHorizontal) {
        if (alignment == null) {
            // Default to top-left
            return contentArea.position();
        }

        Size contentSize = contentArea.size();
        var alignedPos = alignment.align(childSize, contentSize);

        if (isHorizontal) {
            return new Position(contentArea.x() + alignedPos.x(), contentArea.y());
        } else {
            return new Position(contentArea.x(), contentArea.y() + alignedPos.y());
        }
    }

    /**
     * Calculates constraints for a child in a linear layout, accounting for remaining space.
     *
     * @param availableConstraints the available constraints
     * @param usedSpace           the space already used
     * @param remainingChildren   the number of children still to be placed
     * @param spacing            the spacing between elements
     * @param isHorizontal       if true, layout is horizontal; if false, vertical
     * @return the constraints for the child
     */
    public static Constraints calculateLinearChildConstraints(
            Constraints availableConstraints,
            int usedSpace,
            int remainingChildren,
            int spacing,
            boolean isHorizontal) {

        int remainingSpacing = calculateTotalSpacing(remainingChildren, spacing);
        int availableSpace;

        if (isHorizontal) {
            availableSpace = Math.max(0, availableConstraints.maxWidth() - usedSpace - remainingSpacing);
            return new Constraints(
                    0,
                    availableSpace,
                    availableConstraints.minHeight(),
                    availableConstraints.maxHeight()
            );
        } else {
            availableSpace = Math.max(0, availableConstraints.maxHeight() - usedSpace - remainingSpacing);
            return new Constraints(
                    availableConstraints.minWidth(),
                    availableConstraints.maxWidth(),
                    0,
                    availableSpace
            );
        }
    }
}