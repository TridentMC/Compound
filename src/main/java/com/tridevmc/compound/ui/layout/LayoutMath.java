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

package com.tridevmc.compound.ui.layout;

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
     * @param contentArea  the content area bounds
     * @param childSize    the child's size
     * @param alignment    the alignment to apply
     * @param isHorizontal if true, align horizontally; if false, align vertically
     * @return the position for the child
     */
    public static Position applyAlignment(Bounds contentArea, Size childSize, Alignment alignment, boolean isHorizontal) {
        if (alignment == null) {
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
}