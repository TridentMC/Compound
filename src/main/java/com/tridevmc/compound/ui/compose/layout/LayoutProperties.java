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

import java.util.function.Consumer;

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
 *   <li>{@link #clip()} - Enables clipping of children to element bounds</li>
 * </ul>
 */
public class LayoutProperties {
    private Integer fixedWidth;
    private Integer fixedHeight;
    private boolean fillMaxWidth;
    private boolean fillMaxHeight;
    private Float weight;
    private Integer minWidth;
    private Integer minHeight;
    private Integer maxWidth;
    private Integer maxHeight;
    private int paddingLeft;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;
    private int marginLeft;
    private int marginTop;
    private int marginRight;
    private int marginBottom;
    private Alignment contentAlignment;
    private Alignment horizontalAlignment;
    private Alignment verticalAlignment;
    private int spacing;
    private Integer gridColumns;
    private Integer gridRows;
    private boolean clip;

    protected LayoutProperties() {
    }

    /**
     * Enter deferred layout scope for reactive property updates.
     * Use this when layout properties need to respond to state changes without recomposition.
     *
     * <p>The callback will be re-executed whenever any bound state changes, allowing
     * layout properties to be updated reactively. This triggers layout but not recomposition.</p>
     *
     * <p>Example:</p>
     * <pre>
     * var width = scope.animateInt(0, 300);
     *
     * box.layout()
     *     .fillMaxHeight()
     *     .deferred(deferred -&gt; {
     *         deferred.bind(width);
     *         deferred.layout().fixedWidth(width.get());
     *     });
     * </pre>
     *
     * @param configurator the deferred configuration scope
     * @return this for continued chaining
     */
    public LayoutProperties deferred(Consumer<DeferredScope> configurator) {
        // Note: Node binding will be set up when this is called from an element context
        // For now, create scope without node (node will be injected by element)
        DeferredScope scope = new DeferredScope(this, null, configurator);
        configurator.accept(scope);
        return this;
    }

    public static LayoutProperties create() {
        return new LayoutProperties();
    }

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

    public int getHorizontalPadding() {
        return this.paddingLeft + this.paddingRight;
    }

    public int getVerticalPadding() {
        return this.paddingTop + this.paddingBottom;
    }

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

    /**
     * Enables clipping of children to this element's bounds.
     * Used by: All containers (controls whether children are clipped during rendering)
     *
     * <p>When enabled, children are clipped using GPU scissor test (sharp edges, no performance cost).
     * By default, clipping is disabled and children can render outside bounds.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * scope.e(new Box(), box -> {
     *     box.layoutProperties()
     *         .fixedSize(200, 100)
     *         .clip();
     *     // Content outside 200x100 will be clipped
     * });
     * </pre>
     *
     * @return this for chaining
     */
    public LayoutProperties clip() {
        this.clip = true;
        return this;
    }

    public boolean isClip() {
        return this.clip;
    }
}
