package com.tridevmc.compound.ui.element;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.ICompoundUI;
import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.layout.ILayout;
import com.tridevmc.compound.ui.layout.LayoutNone;

import javax.annotation.Nonnull;
import java.util.List;

public class ElementContainerGrid implements IElementContainer {

    private Rect2D dimensions;
    private ILayout layout;
    private float xPadding, yPadding;
    private boolean isFlexible;
    private List<IElement> childElements;

    public ElementContainerGrid(Rect2D dimensions) {
        this(dimensions, false);
    }

    public ElementContainerGrid(Rect2D dimensions, boolean isFlexible) {
        this(dimensions, isFlexible, 0, 0);
    }

    public ElementContainerGrid(Rect2D dimensions, boolean isFlexible, float xPadding, float yPadding) {
        this.dimensions = dimensions;
        this.isFlexible = isFlexible;
        this.xPadding = xPadding;
        this.yPadding = yPadding;
        this.childElements = Lists.newArrayList();
        this.layout = new LayoutNone();
    }

    @Override
    public void drawLayer(ICompoundUI ui, EnumUILayer layer) {
        if (this.isFlexible) {
            this.getElements().forEach((e) -> {
                        this.dimensions.setWidth(Math.max(this.dimensions.getWidth(),
                                e.getDimensions().getWidth() + (this.xPadding * 2)));
                        this.dimensions.setHeight(Math.max(this.dimensions.getHeight(),
                                e.getDimensions().getHeight() + (this.yPadding * 2)));
                    }
            );
        }
        Rect2D rect = this.layout.getTransformedRect(ui.getScreenContext(), this.getDimensions());

        float currentX = this.xPadding;
        float currentY = this.yPadding;
        float rowHeight = 0;
        for (IElement element : this.getElements()) {
            double sizeIncrease = element.getDimensions().getWidth() + this.xPadding;
            rowHeight = Math.max(rowHeight, (float) element.getDimensions().getHeight());
            if (currentX + sizeIncrease > this.getDimensions().getWidth()) {
                currentY += rowHeight + this.yPadding;
                currentX = this.xPadding;
                rowHeight = 0;

                if (currentX + sizeIncrease > this.getDimensions().getWidth()) {
                    // Not sure how an element got in here if its that big, but we'll just pretend it doesn't exist.
                    continue;
                }
            }

            element.getDimensions().setX(rect.getX() + currentX);
            element.getDimensions().setY(rect.getY() + currentY);
            element.drawLayer(ui, layer);

            currentX += this.xPadding + element.getDimensions().getWidth();
        }
    }

    @Override
    public ImmutableList<IElement> getElements() {
        return ImmutableList.copyOf(this.childElements);
    }

    @Override
    public void addElement(IElement element) {
        if (this.isFlexible) {
            this.dimensions.setWidth(Math.max(this.dimensions.getWidth(),
                    element.getDimensions().getWidth() + (this.xPadding * 2)));
            this.dimensions.setHeight(Math.max(this.dimensions.getHeight(),
                    element.getDimensions().getHeight() + (this.yPadding * 2)));
        } else {
            if (element.getDimensions().getWidth() > this.dimensions.getWidth() + this.xPadding
                    || element.getDimensions().getHeight() > this.dimensions.getHeight() + this.yPadding) {
                throw new IllegalArgumentException("Unable to add child element to grid, the size of the element is larger than the grid.");
            }
        }
        this.childElements.add(element);
    }

    @Override
    public boolean removeElement(IElement element) {
        return this.childElements.remove(element);
    }

    @Nonnull
    @Override
    public Rect2D getDimensions() {
        return this.dimensions;
    }

    @Override
    public void setDimensions(@Nonnull Rect2D dimensions) {
        this.dimensions = dimensions;
    }

    @Nonnull
    @Override
    public ILayout getLayout() {
        return this.layout;
    }

    @Override
    public void setLayout(@Nonnull ILayout layout) {
        this.layout = layout;
    }
}
