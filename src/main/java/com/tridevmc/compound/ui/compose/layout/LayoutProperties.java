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
 * Configuration for how an element should be laid out by its parent.
 */
public class LayoutProperties {
    // Fixed size
    private Integer fixedWidth;
    private Integer fixedHeight;

    // Weight (for flex layouts)
    private Float weight;

    // Min/Max size
    private Integer minWidth;
    private Integer minHeight;
    private Integer maxWidth;
    private Integer maxHeight;

    // Padding (space inside the element's bounds)
    private int paddingLeft;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;

    // Margin (space outside the element's bounds)
    private int marginLeft;
    private int marginTop;
    private int marginRight;
    private int marginBottom;

    // Alignment (for containers)
    private Alignment alignment;

    // Spacing (between children in containers)
    private Integer spacing;

    // Grid-specific
    private Integer gridColumns;
    private Integer gridRows;

    private LayoutProperties() {
    }

    public static LayoutProperties create() {
        return new LayoutProperties();
    }

    // Fixed size
    public LayoutProperties withFixedWidth(int width) {
        this.fixedWidth = width;
        return this;
    }

    public LayoutProperties withFixedHeight(int height) {
        this.fixedHeight = height;
        return this;
    }

    public LayoutProperties withFixedSize(int width, int height) {
        this.fixedWidth = width;
        this.fixedHeight = height;
        return this;
    }

    public Integer getFixedWidth() {
        return this.fixedWidth;
    }

    public Integer getFixedHeight() {
        return this.fixedHeight;
    }

    // Weight
    public LayoutProperties withWeight(float weight) {
        this.weight = weight;
        return this;
    }

    public Float getWeight() {
        return this.weight;
    }

    // Min/Max size
    public LayoutProperties withMinWidth(int minWidth) {
        this.minWidth = minWidth;
        return this;
    }

    public LayoutProperties withMinHeight(int minHeight) {
        this.minHeight = minHeight;
        return this;
    }

    public LayoutProperties withMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    public LayoutProperties withMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        return this;
    }

    public Integer getMinWidth() {
        return this.minWidth;
    }

    public Integer getMinHeight() {
        return this.minHeight;
    }

    public Integer getMaxWidth() {
        return this.maxWidth;
    }

    public Integer getMaxHeight() {
        return this.maxHeight;
    }

    // Padding
    public LayoutProperties withPadding(int padding) {
        return this.withPadding(padding, padding, padding, padding);
    }

    public LayoutProperties withPadding(int horizontal, int vertical) {
        return this.withPadding(horizontal, vertical, horizontal, vertical);
    }

    public LayoutProperties withPadding(int left, int top, int right, int bottom) {
        this.paddingLeft = left;
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        return this;
    }

    public int getPaddingLeft() {
        return this.paddingLeft;
    }

    public int getPaddingTop() {
        return this.paddingTop;
    }

    public int getPaddingRight() {
        return this.paddingRight;
    }

    public int getPaddingBottom() {
        return this.paddingBottom;
    }

    // Margin
    public LayoutProperties withMargin(int margin) {
        return this.withMargin(margin, margin, margin, margin);
    }

    public LayoutProperties withMargin(int horizontal, int vertical) {
        return this.withMargin(horizontal, vertical, horizontal, vertical);
    }

    public LayoutProperties withMargin(int left, int top, int right, int bottom) {
        this.marginLeft = left;
        this.marginTop = top;
        this.marginRight = right;
        this.marginBottom = bottom;
        return this;
    }

    public int getMarginLeft() {
        return this.marginLeft;
    }

    public int getMarginTop() {
        return this.marginTop;
    }

    public int getMarginRight() {
        return this.marginRight;
    }

    public int getMarginBottom() {
        return this.marginBottom;
    }

    // Alignment
    public LayoutProperties withAlignment(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    public Alignment getAlignment() {
        return this.alignment;
    }

    // Spacing
    public LayoutProperties withSpacing(int spacing) {
        this.spacing = spacing;
        return this;
    }

    public Integer getSpacing() {
        return this.spacing;
    }

    // Grid-specific
    public LayoutProperties withGridSize(int columns, int rows) {
        this.gridColumns = columns;
        this.gridRows = rows;
        return this;
    }

    public Integer getGridColumns() {
        return this.gridColumns;
    }

    public Integer getGridRows() {
        return this.gridRows;
    }
}
