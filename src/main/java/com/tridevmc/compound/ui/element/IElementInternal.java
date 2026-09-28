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

package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.tree.ITreeNode;

/**
 * Internal interface for UITree operations on elements.
 * <p>
 * WARNING: This interface is for framework-internal use only.
 * Element implementations should extend Element instead of implementing this directly.
 * <p>
 * Methods in this interface are called by UITree during tree lifecycle and layout.
 * Elements should not call these methods directly - doing so may break the layout system.
 */
public interface IElementInternal extends IGenericElement {

    /**
     * Gets the tree node reference for this element.
     *
     * @return the associated tree node, or null if not attached
     */
    ITreeNode getNode();

    /**
     * Sets the tree node reference for this element.
     * Called by UITree when element is attached to the tree.
     * Bounds are stored in the node, not duplicated in the element.
     *
     * @param node the owning tree node, or null when detaching
     */
    void setNode(ITreeNode node);
}
