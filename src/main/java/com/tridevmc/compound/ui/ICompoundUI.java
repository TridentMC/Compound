/*
 * Copyright 2018 - 2022 TridentMC
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

package com.tridevmc.compound.ui;

import com.google.common.collect.ImmutableList;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.listeners.*;
import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * An interface defining the required functionality for a Compound UI. The default implementation is recommended.
 */
public interface ICompoundUI {

    /**
     * Gets the current screen context for the UI, used for drawing to the screen.
     *
     * @return the active screen context.
     */
    IScreenContext getScreenContext();

    /**
     * Gets an immutable list of all the elements currently in the UI.
     *
     * @return an immutable list of all the UI elements.
     */
    ImmutableList<IElement> getElements();

    /**
     * Called during init, used to add any UI elements.
     */
    void initElements();

    /**
     * Adds an element to the UI.
     *
     * @param element the element to add to the UI.
     */
    void addElement(IElement element);

    /**
     * Removes an element from the UI.
     *
     * @param element the element to remove from the UI.
     * @return true if the element was found and removed, false otherwise.
     */
    boolean removeElement(IElement element);

    /**
     * Adds a key press listener to the UI.
     *
     * @param listener the listener to add.
     */
    void addListener(IKeyPressListener listener);

    /**
     * Adds a key release listener to the UI.
     *
     * @param listener the listener to add.
     */
    void addListener(IKeyReleaseListener listener);

    /**
     * Adds a char type listener to the UI.
     *
     * @param listener the listener to add.
     */
    void addListener(ICharTypeListener listener);

    /**
     * Adds a mouse drag listener to the UI.
     *
     * @param listener the listener to add.
     */
    void addListener(IMouseDraggedListener listener);

    /**
     * Adds a mouse press listener to the UI.
     *
     * @param listener the listener to add.
     */
    void addListener(IMousePressListener listener);

    /**
     * Adds a mouse release listener to the UI.
     *
     * @param listener the listener to add.
     */
    void addListener(IMouseReleaseListener listener);

    /**
     * Adds a mouse scroll listener to the UI.
     *
     * @param listener the listener to add.
     */
    void addListener(IMouseScrollListener listener);


}
