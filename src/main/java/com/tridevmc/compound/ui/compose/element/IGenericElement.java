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
import com.tridevmc.compound.ui.compose.layout.LayoutProperties;
import com.tridevmc.compound.ui.compose.layout.Size;
import com.tridevmc.compound.ui.compose.tree.UITree;

/**
 * The base interface for all UI elements in the compose system.
 */
public interface IGenericElement {

    /**
     * Layout Phase 1: Measure (bottom-up).
     * Returns the desired size given the constraints.
     *
     * @param constraints the constraints for measuring this element
     * @return the desired size of this element
     */
    Size measure(Constraints constraints);

    /**
     * Layout Phase 2: Place (top-down).
     * Sets the final bounds for this element.
     *
     * @param bounds the final bounds for this element
     */
    void place(Bounds bounds);

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
     * Gets the layout properties for this element.
     *
     * @return the layout properties
     */
    LayoutProperties getLayoutProperties();

    /**
     * Sets the layout properties for this element.
     *
     * @param properties the layout properties to set
     */
    void setLayoutProperties(LayoutProperties properties);

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
