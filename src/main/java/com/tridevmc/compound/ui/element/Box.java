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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A container that wraps a single child with padding and alignment.
 * Uses contentAlignment and padding properties from LayoutProperties.
 *
 * <p><strong>Important:</strong> Box is designed for a single child only. If multiple children
 * are added, only the first child will be rendered and a warning will be logged.</p>
 */
public class Box extends BaseContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Box.class);

    @Override
    public Size measure(Constraints constraints, List<IElement> children) {
        var props = this.getOwnLayoutProperties();
        int horizontalPadding = props.getPaddingLeft() + props.getPaddingRight();
        int verticalPadding = props.getPaddingTop() + props.getPaddingBottom();

        if (children.isEmpty()) {
            return constraints.constrain(new Size(horizontalPadding, verticalPadding));
        }

        if (children.size() > 1) {
            LOGGER.debug("Box element contains {} children, but Box is designed for a single child. " +
                            "Only the first child will be rendered. Consider using Stack for multiple children.",
                    children.size());
        }

        var contentConstraints = LayoutMath.calculateContentConstraints(constraints, props);
        var childSize = LayoutHelper.measureChild(children.getFirst(), contentConstraints);

        return constraints.constrain(new Size(
                childSize.width() + horizontalPadding,
                childSize.height() + verticalPadding
        ));
    }

    @Override
    public void place(Bounds bounds, List<IElement> children) {
        this.setBounds(bounds);

        if (children.isEmpty()) {
            return;
        }

        var child = children.getFirst();
        var childSize = LayoutHelper.getMeasuredSize(child);
        if (childSize == null) {
            return;
        }

        var props = this.getOwnLayoutProperties();
        var contentArea = LayoutMath.calculateContentArea(bounds, props);

        var alignment = props.getContentAlignment();
        Bounds childBounds;
        if (alignment != null) {
            var alignedPos = alignment.align(childSize, contentArea.size());
            childBounds = new Bounds(
                    new Position(contentArea.x() + alignedPos.x(), contentArea.y() + alignedPos.y()),
                    childSize
            );
        } else {
            childBounds = new Bounds(contentArea.position(), childSize);
        }

        LayoutHelper.placeChild(child, childBounds);
    }
}
