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

import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutMath;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Position;
import com.tridevmc.compound.ui.layout.Size;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

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
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty() || this.columns <= 0) {
            return new Size(0, 0);
        }

        int rows = calculateRowCount(measuredChildren.size());
        int[] columnWidths = new int[this.columns];
        int[] rowHeights = new int[rows];

        for (int i = 0; i < measuredChildren.size(); i++) {
            Size childSize = measuredChildren.get(i);
            int col = i % this.columns;
            int row = i / this.columns;

            columnWidths[col] = Math.max(columnWidths[col], childSize.width());
            rowHeights[row] = Math.max(rowHeights[row], childSize.height());
        }

        int totalWidth = 0;
        for (int width : columnWidths) {
            totalWidth += width;
        }
        totalWidth += (this.columns - 1) * this.horizontalSpacing;

        int totalHeight = 0;
        for (int height : rowHeights) {
            totalHeight += height;
        }
        totalHeight += (rows - 1) * this.verticalSpacing;

        return new Size(totalWidth, totalHeight);
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty() || this.columns <= 0) {
            return List.of();
        }

        var contentArea = LayoutMath.calculateContentArea(bounds, props);

        // Calculate column widths and row heights from measured children
        int rows = calculateRowCount(measuredChildren.size());
        int[] columnWidths = new int[this.columns];
        int[] rowHeights = new int[rows];

        for (int i = 0; i < measuredChildren.size(); i++) {
            Size childSize = measuredChildren.get(i);
            int col = i % this.columns;
            int row = i / this.columns;

            columnWidths[col] = Math.max(columnWidths[col], childSize.width());
            rowHeights[row] = Math.max(rowHeights[row], childSize.height());
        }

        // Calculate cumulative positions for each column/row
        int[] columnPositions = new int[this.columns];
        int currentX = contentArea.x();
        for (int col = 0; col < this.columns; col++) {
            columnPositions[col] = currentX;
            currentX += columnWidths[col] + this.horizontalSpacing;
        }

        int[] rowPositions = new int[rows];
        int currentY = contentArea.y();
        for (int row = 0; row < rows; row++) {
            rowPositions[row] = currentY;
            currentY += rowHeights[row] + this.verticalSpacing;
        }

        // Calculate bounds for each child
        List<Bounds> childBounds = new ArrayList<>();
        for (int i = 0; i < measuredChildren.size(); i++) {
            Size childSize = measuredChildren.get(i);
            int col = i % this.columns;
            int row = i / this.columns;

            int x = columnPositions[col];
            int y = rowPositions[row];

            childBounds.add(new Bounds(new Position(x, y), childSize));
        }

        return childBounds;
    }

    private int calculateRowCount(int childCount) {
        return (int) Math.ceil((double) childCount / this.columns);
    }

    public int getColumns() {
        return this.columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
        this.invalidateLayout();
    }

    public int getHorizontalSpacing() {
        return this.horizontalSpacing;
    }

    public void setHorizontalSpacing(int horizontalSpacing) {
        this.horizontalSpacing = horizontalSpacing;
        this.invalidateLayout();
    }

    public int getVerticalSpacing() {
        return this.verticalSpacing;
    }

    public void setVerticalSpacing(int verticalSpacing) {
        this.verticalSpacing = verticalSpacing;
        this.invalidateLayout();
    }
}
