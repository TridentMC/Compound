package com.tridevmc.compound.ui.element;

import com.google.common.collect.ImmutableList;

public interface IElementContainer extends IElement {

    /**
     * Gets an immutable list of all the elements currently in this container.
     *
     * @return an immutable list of all the UI elements in this container.
     */
    ImmutableList<IElement> getElements();

    /**
     * Adds an element to the container.
     *
     * @param element the element to add to the container.
     */
    void addElement(IElement element);

    /**
     * Removes an element from the container.
     *
     * @param element the element to remove from the container.
     * @return true if the element was found and removed, false otherwise.
     */
    boolean removeElement(IElement element);

}
