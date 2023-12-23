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

package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.ICompoundUI;
import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.layout.ILayout;
import com.tridevmc.compound.ui.screen.IScreenContext;

import javax.annotation.Nonnull;

/**
 * Base class for all Compound UI elements.
 */
public interface IElement {

    /**
     * Called when drawing the specified ui layer of the element.
     *
     * @param ui    the ui that is drawing the element.
     * @param layer the layer that is being drawn.
     */
    default void drawLayer(ICompoundUI ui, EnumUILayer layer) {
        switch (layer) {
            case BACKGROUND -> this.drawBackground(ui);
            case FOREGROUND -> this.drawForeground(ui);
            case OVERLAY -> this.drawOverlay(ui);
        }
    }

    /**
     * Called when drawing the background layer of the element.
     *
     * @param ui the ui that is drawing the element.
     */
    default void drawBackground(ICompoundUI ui) {
    }

    /**
     * Called when drawing the foreground layer of the element.
     *
     * @param ui the ui that is drawing the element.
     */
    default void drawForeground(ICompoundUI ui) {
    }

    /**
     * Called when drawing the overlay layer of the element.
     *
     * @param ui the ui that is drawing the element.
     */
    default void drawOverlay(ICompoundUI ui) {
    }

    /**
     * Called after all elements have been added to the UI but before they've been drawn.
     * Used for adding listeners and initializing any cross-element interactions.
     *
     * @param ui the ui that contains the element.
     */
    default void initElement(ICompoundUI ui) {
    }

    /**
     * Called on each ui tick, useful for updating animation states.
     *
     * @param ui the ui that is ticking the element.
     */
    default void tick(ICompoundUI ui) {
    }

    /**
     * Gets the dimensions of this element.
     *
     * @return the dimensions of this element.
     */
    @Nonnull
    Rect2F getDimensions();

    /**
     * Changes the dimensions of the element to the given rect.
     *
     * @param dimensions the new dimensions of the element.
     */
    void setDimensions(@Nonnull Rect2F dimensions);

    /**
     * Gets the UI layout of this element.
     *
     * @return the UI layout of this element.
     */
    @Nonnull
    ILayout getLayout();

    /**
     * Sets the UI layout of this element to the given layout.
     *
     * @param layout the new layout of the element.
     */
    void setLayout(@Nonnull ILayout layout);

    /**
     * Determines if the element should rely on the layout to manipulate the matrix stack before rendering instead of
     * transforming our dimensions directly.
     *
     * @return true if the element should use the layout to manipulate the matrix stack.
     */
    default boolean useManagedMatrix() {
        return true;
    }

    /**
     * Gets the drawn dimensions of the element, this is the dimensions that are used to draw the element on the screen.
     * If the element is using a managed matrix, this will be the transformed dimensions of the element, otherwise it
     * will be the same as the elements dimensions.
     * <p>
     * If you need to check the actual rendered dimensions of the element, use {@link #getScreenspaceDimensions(IScreenContext)}.
     *
     * @param screen the screen to pass to the layout.
     * @return the transformed rect for this element.
     */
    default Rect2F getDrawnDimensions(IScreenContext screen) {
        if (!this.useManagedMatrix()) {
            return this.getLayout().getTransformedRect(screen, this, this.getDimensions());
        } else {
            return this.getDimensions();
        }
    }

    /**
     * Uses the layout of the element to get the actual position of the element on the screen.
     *
     * @param screen the screen to pass to the layout.
     * @return the transformed rect for this element.
     */
    default Rect2F getScreenspaceDimensions(IScreenContext screen) {
        return this.getLayout().getTransformedRect(screen, this, this.getDimensions());
    }

}
