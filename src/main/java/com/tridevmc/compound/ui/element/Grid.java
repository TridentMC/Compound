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

        if (children.isEmpty() || this.columns <= 0) {
            return new Size(props.getHorizontalPadding(), props.getVerticalPadding());
        }

        var contentConstraints = LayoutMath.calculateContentConstraints(constraints, props);
        int rows = calculateRowCount(children.size());

        Size cellSize = calculateCellSize(contentConstraints.maxWidth(), contentConstraints.maxHeight(), rows);
        var cellConstraints = new Constraints(0, cellSize.width(), 0, cellSize.height());

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

        int totalWidth = props.getHorizontalPadding();
        for (int width : columnWidths) {
            totalWidth += width;
        }
        totalWidth += (this.columns - 1) * this.horizontalSpacing;

        int totalHeight = props.getVerticalPadding();
        for (int height : rowHeights) {
            totalHeight += height;
        }
        totalHeight += (rows - 1) * this.verticalSpacing;

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

        var props = this.getLayoutProperties();
        var contentArea = LayoutMath.calculateContentArea(bounds, props);

        int rows = calculateRowCount(children.size());
        Size cellSize = calculateCellSize(contentArea.width(), contentArea.height(), rows);

        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);
            int col = i % this.columns;
            int row = i / this.columns;

            int x = contentArea.x() + (col * (cellSize.width() + this.horizontalSpacing));
            int y = contentArea.y() + (row * (cellSize.height() + this.verticalSpacing));

            var childSize = LayoutHelper.getMeasuredSize(child);
            if (childSize == null) {
                continue;
            }

            var childBounds = new Bounds(new Position(x, y), childSize);
            LayoutHelper.placeChild(child, childBounds);
        }
    }

    private int calculateRowCount(int childCount) {
        return (int) Math.ceil((double) childCount / this.columns);
    }

    private Size calculateCellSize(int availableWidth, int availableHeight, int rows) {
        int totalHorizontalSpacing = (this.columns - 1) * this.horizontalSpacing;
        int totalVerticalSpacing = (rows - 1) * this.verticalSpacing;
        int cellWidth = Math.max(0, (availableWidth - totalHorizontalSpacing) / this.columns);
        int cellHeight = Math.max(0, (availableHeight - totalVerticalSpacing) / rows);
        return new Size(cellWidth, cellHeight);
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
