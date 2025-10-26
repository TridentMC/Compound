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

import java.util.List;

/**
 * Elements that can have children.
 */
public interface IContainer extends IElement {

    /**
     * Get children (queries the internal tree).
     *
     * @return list of child elements
     */
    List<IElement> getChildren();

    /**
     * Check if this container has any children.
     *
     * @return true if has children, false otherwise
     */
    boolean hasChildren();

    /**
     * Get child count.
     *
     * @return number of children
     */
    int getChildCount();
}
