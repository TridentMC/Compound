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
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.tree.UITree;

import java.util.List;

/**
 * The base interface for all UI elements in the compose system.
 */
public interface IGenericElement {

    /**
     * Layout Phase 1: Measure (bottom-up).
     * Returns the desired size given the constraints.
     * Children are provided by the tree - elements don't query for them.
     *
     * @param constraints the constraints for measuring this element
     * @param children    the child elements (empty for primitives)
     * @return the desired size of this element
     */
    Size measure(Constraints constraints, List<IElement> children);

    /**
     * Layout Phase 2: Place (top-down).
     * Sets the final bounds for this element.
     * Children are provided by the tree - elements don't query for them.
     *
     * @param bounds   the final bounds for this element
     * @param children the child elements (empty for primitives)
     */
    void place(Bounds bounds, List<IElement> children);

    /**
     * Gets the tree this element belongs to.
     *
     * @return the UI tree
     */
    UITree getTree();

    /**
     * Sets the tree this element belongs to (called by framework).
     *
     * @param tree the UI tree
     */
    void setTree(UITree tree);

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

    /**
     * Checks if this element is visible.
     *
     * @return true if visible, false otherwise
     */
    boolean isVisible();

    /**
     * Sets the visibility of this element.
     *
     * @param visible true to make visible, false to hide
     */
    void setVisible(boolean visible);
}
