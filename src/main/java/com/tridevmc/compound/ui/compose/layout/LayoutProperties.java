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
 *
 * <p>Properties are grouped by usage context. Not all properties apply to all containers.
 * See individual property documentation for which containers respect each property.</p>
 *
 * <h2>Common Properties (All Elements)</h2>
 * <ul>
 *   <li>{@link #fixedSize(int, int)} - Forces a specific size</li>
 *   <li>{@link #fillMaxWidth()} / {@link #fillMaxHeight()} - Expands to fill available space</li>
 *   <li>{@link #margin(int)} - Space outside element bounds (handled by parent)</li>
 *   <li>{@link #minWidth(int)} / {@link #maxWidth(int)} etc. - Size constraints</li>
 * </ul>
 *
 * <h2>Container Properties (Set on Container Element)</h2>
 * <ul>
 *   <li>{@link #padding(int)} - Space inside container bounds (Box, Column, Row, Stack, Grid)</li>
 *   <li>{@link #contentAlignment(Alignment)} - Child alignment in Box, Stack</li>
 *   <li>{@link #horizontalAlignment(Alignment)} - Child alignment in Column</li>
 *   <li>{@link #verticalAlignment(Alignment)} - Child alignment in Row</li>
 *   <li>{@link #spacing(int)} - Gap between children in Column, Row</li>
 * </ul>
 */
public class LayoutProperties {
    // Fixed size - used by LayoutHelper.measureChild() and LayoutHelper.calculateSizeWithProperties()
    private Integer fixedWidth;
    private Integer fixedHeight;

    // Fill max size flags - used by LayoutHelper.measureChild() and LayoutHelper.calculateSizeWithProperties()
    private boolean fillMaxWidth;
    private boolean fillMaxHeight;

    // Weight (for flex layouts) - reserved for future weighted layouts
    private Float weight;

    // Min/Max size - used by LayoutHelper.measureChild()
    private Integer minWidth;
    private Integer minHeight;
    private Integer maxWidth;
    private Integer maxHeight;

    // Padding (space inside the element's bounds) - used by Box, Column, Row, Stack, Grid
    private int paddingLeft;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;

    // Margin (space outside the element's bounds) - used by LayoutHelper.measureChild() and placeChild()
    private int marginLeft;
    private int marginTop;
    private int marginRight;
    private int marginBottom;

    // Container alignment properties
    private Alignment contentAlignment;      // Used by Box, Stack
    private Alignment horizontalAlignment;  // Used by Column
    private Alignment verticalAlignment;    // Used by Row

    // Spacing (between children in Row/Column)
    private int spacing;

    // Grid-specific - reserved for future Grid enhancements
    private Integer gridColumns;
    private Integer gridRows;

    protected LayoutProperties() {
    }

    public static LayoutProperties create() {
        return new LayoutProperties();
    }

    // ========== Fixed Size Methods ==========

    /**
     * Sets a fixed width for this element.
     * Used by: All elements via LayoutHelper.measureChild()
     *
     * @param width the fixed width in pixels
     * @return this for chaining
     */
    public LayoutProperties fixedWidth(int width) {
        this.fixedWidth = width;
        return this;
    }

    /**
     * Sets a fixed height for this element.
     * Used by: All elements via LayoutHelper.measureChild()
     *
     * @param height the fixed height in pixels
     * @return this for chaining
     */
    public LayoutProperties fixedHeight(int height) {
        this.fixedHeight = height;
        return this;
    }

    /**
     * Sets both fixed width and height for this element.
     * Used by: All elements via LayoutHelper.measureChild()
     *
     * @param width  the fixed width in pixels
     * @param height the fixed height in pixels
     * @return this for chaining
     */
    public LayoutProperties fixedSize(int width, int height) {
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

    // ========== Fill Max Size Methods ==========

    /**
     * Makes this element expand to fill maximum available width.
     * Used by: All elements via LayoutHelper.measureChild() and calculateSizeWithProperties()
     *
     * @return this for chaining
     */
    public LayoutProperties fillMaxWidth() {
        this.fillMaxWidth = true;
        return this;
    }

    /**
     * Makes this element expand to fill maximum available height.
     * Used by: All elements via LayoutHelper.measureChild() and calculateSizeWithProperties()
     *
     * @return this for chaining
     */
    public LayoutProperties fillMaxHeight() {
        this.fillMaxHeight = true;
        return this;
    }

    /**
     * Makes this element expand to fill maximum available space in both dimensions.
     * Used by: All elements via LayoutHelper.measureChild() and calculateSizeWithProperties()
     *
     * @return this for chaining
     */
    public LayoutProperties fillMax() {
        this.fillMaxWidth = true;
        this.fillMaxHeight = true;
        return this;
    }

    public boolean isFillMaxWidth() {
        return this.fillMaxWidth;
    }

    public boolean isFillMaxHeight() {
        return this.fillMaxHeight;
    }

    // ========== Weight Methods ==========

    /**
     * Sets the weight for weighted layouts (e.g., flex layouts).
     * Reserved for future use.
     *
     * @param weight the weight value
     * @return this for chaining
     */
    public LayoutProperties weight(float weight) {
        this.weight = weight;
        return this;
    }

    public Float getWeight() {
        return this.weight;
    }

    // ========== Min/Max Size Methods ==========

    /**
     * Sets minimum width constraint.
     * Used by: All elements via LayoutHelper.measureChild()
     *
     * @param minWidth the minimum width in pixels
     * @return this for chaining
     */
    public LayoutProperties minWidth(int minWidth) {
        this.minWidth = minWidth;
        return this;
    }

    /**
     * Sets minimum height constraint.
     * Used by: All elements via LayoutHelper.measureChild()
     *
     * @param minHeight the minimum height in pixels
     * @return this for chaining
     */
    public LayoutProperties minHeight(int minHeight) {
        this.minHeight = minHeight;
        return this;
    }

    /**
     * Sets maximum width constraint.
     * Used by: All elements via LayoutHelper.measureChild()
     *
     * @param maxWidth the maximum width in pixels
     * @return this for chaining
     */
    public LayoutProperties maxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    /**
     * Sets maximum height constraint.
     * Used by: All elements via LayoutHelper.measureChild()
     *
     * @param maxHeight the maximum height in pixels
     * @return this for chaining
     */
    public LayoutProperties maxHeight(int maxHeight) {
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

    // ========== Padding Methods ==========

    /**
     * Sets equal padding on all sides.
     * Used by: Box, Column, Row, Stack, Grid (space inside container bounds)
     *
     * @param all padding in pixels for all sides
     * @return this for chaining
     */
    public LayoutProperties padding(int all) {
        return this.padding(all, all, all, all);
    }

    /**
     * Sets horizontal and vertical padding.
     * Used by: Box, Column, Row, Stack, Grid (space inside container bounds)
     *
     * @param horizontal left and right padding in pixels
     * @param vertical   top and bottom padding in pixels
     * @return this for chaining
     */
    public LayoutProperties padding(int horizontal, int vertical) {
        return this.padding(horizontal, vertical, horizontal, vertical);
    }

    /**
     * Sets individual padding for each side.
     * Used by: Box, Column, Row, Stack, Grid (space inside container bounds)
     *
     * @param left   left padding in pixels
     * @param top    top padding in pixels
     * @param right  right padding in pixels
     * @param bottom bottom padding in pixels
     * @return this for chaining
     */
    public LayoutProperties padding(int left, int top, int right, int bottom) {
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

    // ========== Margin Methods ==========

    /**
     * Sets equal margin on all sides.
     * Used by: All elements via LayoutHelper.measureChild() and placeChild() (space outside bounds)
     *
     * @param all margin in pixels for all sides
     * @return this for chaining
     */
    public LayoutProperties margin(int all) {
        return this.margin(all, all, all, all);
    }

    /**
     * Sets horizontal and vertical margin.
     * Used by: All elements via LayoutHelper.measureChild() and placeChild() (space outside bounds)
     *
     * @param horizontal left and right margin in pixels
     * @param vertical   top and bottom margin in pixels
     * @return this for chaining
     */
    public LayoutProperties margin(int horizontal, int vertical) {
        return this.margin(horizontal, vertical, horizontal, vertical);
    }

    /**
     * Sets individual margin for each side.
     * Used by: All elements via LayoutHelper.measureChild() and placeChild() (space outside bounds)
     *
     * @param left   left margin in pixels
     * @param top    top margin in pixels
     * @param right  right margin in pixels
     * @param bottom bottom margin in pixels
     * @return this for chaining
     */
    public LayoutProperties margin(int left, int top, int right, int bottom) {
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

    // ========== Container Alignment Methods ==========

    /**
     * Sets content alignment for Box and Stack containers.
     * Used by: Box, Stack (positions child(ren) within container bounds)
     *
     * <p><strong>Note:</strong> This property is ignored by Column (use horizontalAlignment)
     * and Row (use verticalAlignment).</p>
     *
     * @param alignment the alignment to apply to children
     * @return this for chaining
     */
    public LayoutProperties contentAlignment(Alignment alignment) {
        this.contentAlignment = alignment;
        return this;
    }

    public Alignment getContentAlignment() {
        return this.contentAlignment;
    }

    /**
     * Sets horizontal alignment for Column containers.
     * Used by: Column (aligns children horizontally within column width)
     *
     * <p><strong>Note:</strong> This property is ignored by Box, Stack, Row, and Grid.</p>
     *
     * @param alignment the horizontal alignment to apply
     * @return this for chaining
     */
    public LayoutProperties horizontalAlignment(Alignment alignment) {
        this.horizontalAlignment = alignment;
        return this;
    }

    public Alignment getHorizontalAlignment() {
        return this.horizontalAlignment;
    }

    /**
     * Sets vertical alignment for Row containers.
     * Used by: Row (aligns children vertically within row height)
     *
     * <p><strong>Note:</strong> This property is ignored by Box, Stack, Column, and Grid.</p>
     *
     * @param alignment the vertical alignment to apply
     * @return this for chaining
     */
    public LayoutProperties verticalAlignment(Alignment alignment) {
        this.verticalAlignment = alignment;
        return this;
    }

    public Alignment getVerticalAlignment() {
        return this.verticalAlignment;
    }

    // ========== Spacing Methods ==========

    /**
     * Sets spacing between children in linear layouts.
     * Used by: Column (vertical spacing), Row (horizontal spacing)
     *
     * <p><strong>Note:</strong> This property is ignored by Box, Stack, and Grid
     * (Grid uses constructor parameters for spacing).</p>
     *
     * @param spacing the spacing in pixels between children
     * @return this for chaining
     */
    public LayoutProperties spacing(int spacing) {
        this.spacing = spacing;
        return this;
    }

    public int getSpacing() {
        return this.spacing;
    }

    // ========== Grid-Specific Methods ==========

    /**
     * Sets grid dimensions.
     * Reserved for future Grid enhancements.
     *
     * @param columns number of columns
     * @param rows    number of rows
     * @return this for chaining
     */
    public LayoutProperties gridSize(int columns, int rows) {
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
