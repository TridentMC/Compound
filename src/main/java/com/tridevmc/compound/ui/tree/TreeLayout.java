/*
 * Copyright 2018 - 2026 TridentMC
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

package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.element.Row;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutMath;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Position;
import com.tridevmc.compound.ui.layout.Size;
import java.util.ArrayList;
import java.util.List;

final class TreeLayout {
    Size measureNode(ITreeNode node, Constraints constraints) {
        LayoutProperties props = node.getLayoutProperties();
        if (props == null) {
            props = LayoutProperties.create();
        }

        long marginHorizontal = (long) props.getMarginLeft() + props.getMarginRight();
        long marginVertical = (long) props.getMarginTop() + props.getMarginBottom();
        Constraints constraintsWithoutMargin = constraints.inset(marginHorizontal, marginVertical);

        Constraints elementConstraints = this.applyLayoutPropertiesToConstraints(constraintsWithoutMargin, props);

        // Children should not be forced to be as large as the parent's minimum size
        // They are constrained by the parent's maximum size
        Constraints childConstraints = new Constraints(
                0,
                elementConstraints.maxWidth(),
                0,
                elementConstraints.maxHeight()
        );

        Constraints contentConstraints = LayoutMath.calculateContentConstraints(childConstraints, props);

        List<Size> measuredChildren = this.measureChildren(node, contentConstraints);

        Size intrinsicSize = node.getElement().measure(contentConstraints, props, measuredChildren);

        int withPaddingWidth = Constraints.subtractInset(intrinsicSize.width(),
                -((long) props.getPaddingLeft() + props.getPaddingRight()));
        int withPaddingHeight = Constraints.subtractInset(intrinsicSize.height(),
                -((long) props.getPaddingTop() + props.getPaddingBottom()));
        Size elementSize = new Size(withPaddingWidth, withPaddingHeight);

        // Apply fillMax after measurement (not before, so children get loose constraints)
        int finalWidth = elementSize.width();
        int finalHeight = elementSize.height();
        if (props.isFillMaxWidth()) {
            finalWidth = constraintsWithoutMargin.maxWidth();
        }
        if (props.isFillMaxHeight()) {
            finalHeight = constraintsWithoutMargin.maxHeight();
        }

        // Constrain to element constraints (respects fixedSize, min/max)
        // Scrolling content can explicitly opt out of the parent limit.
        int constrainedWidth = finalWidth;
        int constrainedHeight = finalHeight;

        if (props.isWidthUnbounded()) {
            // Allow exceeding parent maxWidth - only apply minWidth constraint
            constrainedWidth = Math.max(elementConstraints.minWidth(), finalWidth);
        } else {
            constrainedWidth = elementConstraints.constrainWidth(finalWidth);
        }

        if (props.isHeightUnbounded()) {
            // Allow exceeding parent maxHeight - only apply minHeight constraint
            constrainedHeight = Math.max(elementConstraints.minHeight(), finalHeight);
        } else {
            constrainedHeight = elementConstraints.constrainHeight(finalHeight);
        }

        Size finalSize = new Size(constrainedWidth, constrainedHeight);

        int totalWidth = Constraints.subtractInset(finalSize.width(), -marginHorizontal);
        int totalHeight = Constraints.subtractInset(finalSize.height(), -marginVertical);
        Size sizeWithMargin = new Size(totalWidth, totalHeight);

        node.setMeasuredSize(sizeWithMargin);

        return sizeWithMargin;
    }

    private List<Size> measureChildren(ITreeNode node, Constraints constraints) {
        var children = node.getChildren();
        var measured = new ArrayList<Size>(children.size());
        var horizontal = node.getElement() instanceof Row;
        var weightedLayout = horizontal ? constraints.hasBoundedWidth()
                : node.getElement() instanceof Column && constraints.hasBoundedHeight();
        var available = horizontal ? constraints.maxWidth() : constraints.maxHeight();
        long occupied = LayoutMath.calculateTotalSpacing(children.size(), node.getLayoutProperties().getSpacing());
        double remainingWeight = 0;
        for (var child : children) {
            var weight = child.getLayoutProperties().getWeight();
            if (weightedLayout && weight != null && weight > 0) {
                remainingWeight += weight;
                measured.add(new Size(0, 0));
            } else {
                var size = this.measureNode(child, constraints);
                occupied += horizontal ? size.width() : size.height();
                measured.add(size);
            }
        }
        var remaining = (int) Math.max(0L, available - occupied);
        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);
            var weight = child.getLayoutProperties().getWeight();
            if (weightedLayout && weight != null && weight > 0) {
                var allocation = (int) Math.round((double) remaining * weight / remainingWeight);
                remaining -= allocation;
                remainingWeight -= weight;
                var childConstraints = horizontal ? constraints.withFixedWidth(allocation)
                        : constraints.withFixedHeight(allocation);
                measured.set(i, this.measureNode(child, childConstraints));
            }
        }
        return measured;
    }

    private Constraints applyLayoutPropertiesToConstraints(Constraints constraints, LayoutProperties props) {
        int minWidth = constraints.minWidth();
        int maxWidth = constraints.maxWidth();
        int minHeight = constraints.minHeight();
        int maxHeight = constraints.maxHeight();

        // Apply fixed size (tightest constraints)
        if (props.getFixedWidth() != null) {
            minWidth = props.getFixedWidth();
            maxWidth = props.getFixedWidth();
        }
        if (props.getFixedHeight() != null) {
            minHeight = props.getFixedHeight();
            maxHeight = props.getFixedHeight();
        }

        if (props.getMinWidth() != null) {
            minWidth = Math.max(minWidth, props.getMinWidth());
        }
        if (props.isWidthUnbounded()) {
            maxWidth = Integer.MAX_VALUE;
        } else if (props.getMaxWidth() != null) {
            maxWidth = Math.min(maxWidth, props.getMaxWidth());
        }

        if (props.getMinHeight() != null) {
            minHeight = Math.max(minHeight, props.getMinHeight());
        }
        if (props.isHeightUnbounded()) {
            maxHeight = Integer.MAX_VALUE;
        } else if (props.getMaxHeight() != null) {
            maxHeight = Math.min(maxHeight, props.getMaxHeight());
        }

        maxWidth = Math.max(minWidth, maxWidth);
        maxHeight = Math.max(minHeight, maxHeight);

        return new Constraints(minWidth, maxWidth, minHeight, maxHeight);
    }

    void placeNode(ITreeNode node, Bounds bounds) {
        IElement element = node.getElement();
        LayoutProperties props = node.getLayoutProperties();
        if (props == null) {
            props = LayoutProperties.create();
        }

        node.setBounds(bounds);

        List<ITreeNode> children = node.getChildren();
        if (children.isEmpty()) {
            return;
        }

        List<Size> measuredChildren = children.stream()
                .map(ITreeNode::getMeasuredSize)
                .toList();

        List<Bounds> childBounds = element.place(bounds, props, measuredChildren);

        if (childBounds.size() != children.size()) {
            throw new IllegalStateException(
                    "Element " + element.getClass().getSimpleName() + " returned " +
                            childBounds.size() + " bounds but has " + children.size() + " children"
            );
        }

        for (int i = 0; i < children.size(); i++) {
            ITreeNode child = children.get(i);
            Bounds allocatedBounds = childBounds.get(i);
            LayoutProperties childProps = child.getLayoutProperties();
            if (childProps == null) {
                childProps = LayoutProperties.create();
            }

            // Store what parent allocated (for debug overlay)
            child.setAllocatedBounds(allocatedBounds);

            int contentX = allocatedBounds.x() + childProps.getMarginLeft();
            int contentY = allocatedBounds.y() + childProps.getMarginTop();
            int contentWidth = Constraints.subtractInset(allocatedBounds.width(),
                    (long) childProps.getMarginLeft() + childProps.getMarginRight());
            int contentHeight = Constraints.subtractInset(allocatedBounds.height(),
                    (long) childProps.getMarginTop() + childProps.getMarginBottom());

            Bounds contentBounds = new Bounds(
                    new Position(contentX, contentY),
                    new Size(contentWidth, contentHeight)
            );

            this.placeNode(child, contentBounds);
        }
    }

}
