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

import com.tridevmc.compound.ui.cursor.UICursor;
import net.minecraft.network.chat.Component;

/**
 * The core interface for all UI elements in the compose system.
 * All elements implement this interface directly or through sub-interfaces.
 *
 * <p>Element types:</p>
 * <ul>
 *   <li>{@link IPrimitiveElement} - Leaf nodes that draw pixels (no children)</li>
 *   <li>{@link IComposableElement} - Elements with internal composition</li>
 *   <li>{@link IContainer} - Layout containers that manage children</li>
 * </ul>
 */
public interface IElement extends IGenericElement {
    /** Text announced when this element receives focus; empty uses composed child labels. */
    default Component getNarrationMessage() {
        return Component.empty();
    }

    /** Whether this element participates in rendering and input. */
    default boolean isVisible() {
        return true;
    }

    /** Whether this element can receive keyboard focus in its current state. */
    default boolean isFocusable() {
        return false;
    }

    // Core element contract is defined in IGenericElement
    // This interface serves as the common type for all UI elements

    /**
     * Gets the cursor to display when the mouse is at the given coordinates relative to the element.
     *
     * @param x the x coordinate relative to the element
     * @param y the y coordinate relative to the element
     * @return the cursor to display, or null to use the default
     */
    default UICursor getCursor(int x, int y) {
        return null;
    }
}
