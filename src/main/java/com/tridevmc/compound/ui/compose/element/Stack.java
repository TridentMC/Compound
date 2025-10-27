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
import com.tridevmc.compound.ui.compose.layout.Size;

/**
 * A container that stacks children on top of each other (z-layering).
 * All children are sized to fill the entire stack bounds.
 */
public class Stack extends BaseContainer {

    @Override
    public Size measure(Constraints constraints) {
        int maxWidth = 0;
        int maxHeight = 0;

        // Measure all children with the same constraints
        for (var child : this.getChildren()) {
            var childSize = LayoutHelper.measureChild(child, constraints);
            maxWidth = Math.max(maxWidth, childSize.width());
            maxHeight = Math.max(maxHeight, childSize.height());
        }

        // Stack size is the maximum of all children
        return new Size(
                Math.min(maxWidth, constraints.maxWidth()),
                Math.min(maxHeight, constraints.maxHeight())
        );
    }

    @Override
    public void place(Bounds bounds) {
        this.setBounds(bounds);

        // Place all children at the same position and size
        for (var child : this.getChildren()) {
            LayoutHelper.placeChild(child, bounds);
        }
    }
}
