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
     * Measures content after the tree has measured the children.
     * Return the intrinsic content size; the tree adds this element's padding and margins
     * and applies fixed, minimum, maximum, and fill constraints.
     *
     * @param constraints      the constraints for measuring this element's content
     * @param ownProperties    this element's layout properties (padding, spacing, alignment, etc.)
     * @param measuredChildren sizes of children including their margins already measured by tree
     * @return the intrinsic size of this element (tree will add padding and apply other properties)
     */
    Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren);

    /**
     * Allocates child bounds in screen coordinates after this element has been placed.
     * Return one entry per child, in composition order, including each child's margins.
     * The tree removes those margins before recursively placing each child.
     *
     * @param bounds           this element's final bounds, including padding but excluding its own margins
     * @param ownProperties    this element's layout properties
     * @param measuredChildren sizes of children including their margins
     * @return one allocated bounds entry per child, including space for its margins
     */
    List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties ownProperties, List<Size> measuredChildren);

    /**
     * Called after the element is attached, before its children are composed and measured.
     */
    void onAttached();

    /**
     * Called when the element leaves the tree, including removal during parent recomposition.
     * Release element-owned external resources here; tree-owned subscriptions are cleaned up by the tree.
     */
    void onDetached();

    /**
     * Gets the current bounds after placement.
     *
     * @return the placed screen bounds, possibly null before the first layout;
     *         {@link Element} returns empty bounds while detached
     */
    Bounds getBounds();
}
