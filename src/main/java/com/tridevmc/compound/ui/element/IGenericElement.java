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
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * The base interface for all UI elements in the compose system.
 */
public interface IGenericElement {

    /**
     * Layout Phase 1: Measure (bottom-up).
     * Returns the intrinsic size given constraints, layout properties, and measured child sizes.
     * The tree has already measured children with their layout properties applied.
     * Element calculates its intrinsic size based on measured children (for containers)
     * or its own content (for primitives).
     *
     * @param constraints      the constraints for measuring this element's content
     * @param ownProperties    this element's layout properties (padding, spacing, alignment, etc.)
     * @param measuredChildren sizes of children (INCLUDING their margins) already measured by tree
     * @return the intrinsic size of this element (tree will add padding and apply other properties)
     */
    Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren);

    /**
     * Layout Phase 2: Place (top-down).
     * Calculates bounds for children based on this element's layout algorithm.
     * Element is a pure function - all inputs provided as parameters.
     * Tree will apply child margins and recursively place children.
     *
     * @param bounds           this element's final bounds (set via setBounds before this is called)
     * @param ownProperties    this element's layout properties
     * @param measuredChildren sizes of children (INCLUDING their margins)
     * @return bounds for each child (INCLUDING space for margins), tree will offset by child's margin
     */
    List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties ownProperties, List<Size> measuredChildren);

    /**
     * Lifecycle: Called when element is attached to the tree.
     */
    void onAttached();

    /**
     * Lifecycle: Called when element is detached from the tree.
     */
    void onDetached();

    /**
     * Gets the current bounds after placement.
     *
     * @return the current bounds
     */
    Bounds getBounds();
}
