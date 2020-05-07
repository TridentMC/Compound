package com.tridevmc.compound.ui.layout;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.screen.IScreenContext;

import java.util.List;
import java.util.Map;

/**
 * Organizes a list of elements in a grid arrangement, useful for a grid of slots for example.
 */
public class LayoutGrid implements ILayout {

    private Rect2D area;
    private boolean isFlexible;
    private double xPadding, yPadding;

    private List<IElement> gridElements;
    private Map<IElement, Rect2D> cachedElementDimensions;
    private EnumUILayer lastLayer;
    private IElement lastElement;

    public LayoutGrid(Rect2D area, boolean isFlexible, double xPadding, double yPadding) {
        this.area = area;
        this.isFlexible = isFlexible;
        this.xPadding = xPadding;
        this.yPadding = yPadding;

        this.gridElements = Lists.newArrayList();
        this.cachedElementDimensions = Maps.newHashMap();
    }

    public LayoutGrid(Rect2D area, boolean isFlexible) {
        this(area, isFlexible, 0, 0);
    }

    public LayoutGrid(Rect2D area) {
        this(area, false);
    }

    public void registerElement(IElement element) {
        if (this.isFlexible) {
            this.resizeForElement(element);
        } else {
            if (element.getDimensions().getWidth() > this.area.getWidth() + this.xPadding
                    || element.getDimensions().getHeight() > this.area.getHeight() + this.yPadding) {
                throw new IllegalArgumentException("Unable to add child element to grid, the size of the element is larger than the grid.");
            }
        }
        this.gridElements.add(element);
    }

    public boolean unregisterElement(IElement element) {
        return this.gridElements.remove(element);
    }

    public void setArea(Rect2D area) {
        this.area = area;
    }

    public Rect2D getArea() {
        return this.area;
    }

    private void resizeForElement(IElement element) {
        double w = Math.max(this.area.getWidth(), element.getDimensions().getWidth() + (this.xPadding * 2));
        double h = Math.max(this.area.getHeight(), element.getDimensions().getHeight() + (this.yPadding * 2));
        this.setArea(this.area.setSize(w, h));
    }

    private void recalculateSizes() {
        if (this.isFlexible) {
            this.gridElements.forEach(this::resizeForElement);
        }
        Rect2D rect = this.area;
        this.cachedElementDimensions.clear();

        double currentX = rect.getX() + this.xPadding;
        double currentY = rect.getY() + this.yPadding;
        double rowHeight = 0;
        for (IElement element : this.gridElements) {
            double sizeIncrease = element.getDimensions().getWidth() + this.xPadding;
            rowHeight = Math.max(rowHeight, element.getDimensions().getHeight());
            if (currentX + sizeIncrease > (this.area.getWidth() + this.getArea().getX())) {
                currentY += rowHeight + this.yPadding;
                currentX = rect.getX() + this.xPadding;
                rowHeight = 0;

                if (currentX + sizeIncrease > (this.area.getWidth() + this.getArea().getX())) {
                    // Not sure how an element got in here if its that big, but we'll just pretend it doesn't exist.
                    continue;
                }
            }

            this.cachedElementDimensions.put(element, element.getDimensions().setPosition(currentX, currentY));
            currentX += this.xPadding + element.getDimensions().getWidth();
        }
    }

    @Override
    public Rect2D getTransformedRect(IScreenContext screen, IElement element, Rect2D rect) {
        if (this.lastElement == element || this.lastLayer != screen.getCurrentLayer()) {
            this.recalculateSizes();
        } else {
            this.lastElement = element;
            this.lastLayer = screen.getCurrentLayer();
        }
        return this.cachedElementDimensions.getOrDefault(element, element.getDimensions());
    }
}
