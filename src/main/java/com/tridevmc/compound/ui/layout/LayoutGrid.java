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

package com.tridevmc.compound.ui.layout;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.screen.IScreenContext;

import java.util.List;
import java.util.Map;

/**
 * Organizes a list of elements in a grid arrangement, useful for a grid of slots for example.
 */
public class LayoutGrid implements ILayout {

    private Rect2F area;
    private boolean isFlexible;
    private float xPadding, yPadding;

    private List<IElement> gridElements;
    private Map<IElement, Rect2F> cachedElementDimensions;
    private EnumUILayer lastLayer;
    private IElement lastElement;

    public LayoutGrid(Rect2F area, boolean isFlexible, float xPadding, float yPadding) {
        this.area = area;
        this.isFlexible = isFlexible;
        this.xPadding = xPadding;
        this.yPadding = yPadding;

        this.gridElements = Lists.newArrayList();
        this.cachedElementDimensions = Maps.newHashMap();
    }

    public LayoutGrid(Rect2F area, boolean isFlexible) {
        this(area, isFlexible, 0, 0);
    }

    public LayoutGrid(Rect2F area) {
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

    public void setArea(Rect2F area) {
        this.area = area;
    }

    public Rect2F getArea() {
        return this.area;
    }

    private void resizeForElement(IElement element) {
        float w = Math.max(this.area.getWidth(), element.getDimensions().getWidth() + (this.xPadding * 2));
        float h = Math.max(this.area.getHeight(), element.getDimensions().getHeight() + (this.yPadding * 2));
        this.setArea(this.area.setSize(w, h));
    }

    private void recalculateSizes() {
        if (this.isFlexible) {
            this.gridElements.forEach(this::resizeForElement);
        }
        Rect2F rect = this.area;
        this.cachedElementDimensions.clear();

        float currentX = rect.getX() + this.xPadding;
        float currentY = rect.getY() + this.yPadding;
        float rowHeight = 0;
        for (IElement element : this.gridElements) {
            float sizeIncrease = element.getDimensions().getWidth() + this.xPadding;
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
    public Rect2F getTransformedRect(IScreenContext screen, IElement element, Rect2F rect) {
        if (this.lastElement == element || this.lastLayer != screen.getCurrentLayer()) {
            this.recalculateSizes();
        } else {
            this.lastElement = element;
            this.lastLayer = screen.getCurrentLayer();
        }
        return this.cachedElementDimensions.getOrDefault(element, element.getDimensions());
    }

}
