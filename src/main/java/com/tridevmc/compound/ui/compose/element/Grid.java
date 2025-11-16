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

import com.tridevmc.compound.ui.compose.layout.Bounds;
import com.tridevmc.compound.ui.compose.layout.Constraints;
import com.tridevmc.compound.ui.compose.layout.LayoutHelper;
import com.tridevmc.compound.ui.compose.layout.LayoutMath;
import com.tridevmc.compound.ui.compose.layout.Position;
import com.tridevmc.compound.ui.compose.layout.Size;

import java.util.List;

/**
 * A container that lays out children in a grid with a fixed number of columns.
 * Rows are determined automatically based on the number of children.
 * Respects padding from LayoutProperties.
 */
public class Grid extends BaseContainer {

    private int columns;
    private int horizontalSpacing;
    private int verticalSpacing;

    public Grid(int columns) {
        this(columns, 0, 0);
    }

    public Grid(int columns, int spacing) {
        this(columns, spacing, spacing);
    }

    public Grid(int columns, int horizontalSpacing, int verticalSpacing) {
        this.columns = columns;
        this.horizontalSpacing = horizontalSpacing;
        this.verticalSpacing = verticalSpacing;
    }

    @Override
    public Size measure(Constraints constraints, List<IElement> children) {
        var props = this.getLayoutProperties();
        int horizontalPadding = props.getPaddingLeft() + props.getPaddingRight();
        int verticalPadding = props.getPaddingTop() + props.getPaddingBottom();

        if (children.isEmpty() || this.columns <= 0) {
            return new Size(horizontalPadding, verticalPadding);
        }

        // Calculate content constraints (excluding padding)
        var contentConstraints = LayoutMath.calculateContentConstraints(constraints, props);
        int rows = (int) Math.ceil((double) children.size() / this.columns);

        // Calculate available space per cell
        int totalHorizontalSpacing = (this.columns - 1) * this.horizontalSpacing;
        int totalVerticalSpacing = (rows - 1) * this.verticalSpacing;
        int cellWidth = Math.max(0, (contentConstraints.maxWidth() - totalHorizontalSpacing) / this.columns);
        int cellHeight = Math.max(0, (contentConstraints.maxHeight() - totalVerticalSpacing) / rows);

        // Create constraints for each cell
        var cellConstraints = new Constraints(0, cellWidth, 0, cellHeight);

        // Track maximum dimensions actually used
        int[] columnWidths = new int[this.columns];
        int[] rowHeights = new int[rows];

        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);
            int col = i % this.columns;
            int row = i / this.columns;

            var childSize = LayoutHelper.measureChild(child, cellConstraints);
            columnWidths[col] = Math.max(columnWidths[col], childSize.width());
            rowHeights[row] = Math.max(rowHeights[row], childSize.height());
        }

        // Calculate total size (content + padding)
        int totalWidth = horizontalPadding;
        for (int width : columnWidths) {
            totalWidth += width;
        }
        totalWidth += totalHorizontalSpacing;

        int totalHeight = verticalPadding;
        for (int height : rowHeights) {
            totalHeight += height;
        }
        totalHeight += totalVerticalSpacing;

        return new Size(
                Math.min(totalWidth, constraints.maxWidth()),
                Math.min(totalHeight, constraints.maxHeight())
        );
    }

    @Override
    public void place(Bounds bounds, List<IElement> children) {
        this.setBounds(bounds);

        if (children.isEmpty() || this.columns <= 0) {
            return;
        }

        // Calculate content area (excluding padding)
        var props = this.getLayoutProperties();
        var contentArea = LayoutMath.calculateContentArea(bounds, props);

        int rows = (int) Math.ceil((double) children.size() / this.columns);

        // Calculate available space per cell
        int totalHorizontalSpacing = (this.columns - 1) * this.horizontalSpacing;
        int totalVerticalSpacing = (rows - 1) * this.verticalSpacing;
        int cellWidth = Math.max(0, (contentArea.width() - totalHorizontalSpacing) / this.columns);
        int cellHeight = Math.max(0, (contentArea.height() - totalVerticalSpacing) / rows);

        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);
            int col = i % this.columns;
            int row = i / this.columns;

            // Calculate position for this cell (within content area)
            int x = contentArea.x() + (col * (cellWidth + this.horizontalSpacing));
            int y = contentArea.y() + (row * (cellHeight + this.verticalSpacing));

            // Get child's measured size
            var childSize = child.getBounds() != null ? child.getBounds().size() : new Size(0, 0);

            // Place child in its cell
            var childBounds = new Bounds(
                    new Position(x, y),
                    childSize
            );

            LayoutHelper.placeChild(child, childBounds);
        }
    }

    public int getColumns() {
        return this.columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public int getHorizontalSpacing() {
        return this.horizontalSpacing;
    }

    public void setHorizontalSpacing(int horizontalSpacing) {
        this.horizontalSpacing = horizontalSpacing;
    }

    public int getVerticalSpacing() {
        return this.verticalSpacing;
    }

    public void setVerticalSpacing(int verticalSpacing) {
        this.verticalSpacing = verticalSpacing;
    }
}
