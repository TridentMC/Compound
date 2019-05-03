package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.ICompoundUI;
import com.tridevmc.compound.ui.Rect2D;
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
            case BACKGROUND:
                this.drawBackground(ui);
                break;
            case FOREGROUND:
                this.drawForeground(ui);
                break;
            case OVERLAY:
                this.drawOverlay(ui);
                break;
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
     * Gets the dimensions of this element.
     *
     * @return the dimensions of this element.
     */
    @Nonnull
    Rect2D getDimensions();

    /**
     * Changes the dimensions of the element to the given rect.
     *
     * @param dimensions the new dimensions of the element.
     */
    void setDimensions(@Nonnull Rect2D dimensions);

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
     * Uses the layout of the element to get the actual position of the element on the screen.
     *
     * @param screen the screen to pass to the layout.
     * @return the transformed rect for this element.
     */
    default Rect2D getTransformedDimensions(IScreenContext screen) {
        return this.getLayout().getTransformedRect(screen, this, this.getDimensions());
    }

}
