package com.tridevmc.compound.ui.layout;

import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * A layout that makes an element travel across the screen horizontally and then wraps it around the beginning after completion.
 * <p>
 * There's literally no reason anyone should ever use this, it's just a useful test of the layout system.
 */
public class LayoutMarquee implements ILayout {
    private long lastTick = -1;
    private double lastPosition, currentPosition;
    private double movementSpeed = 1D;

    public LayoutMarquee() {
    }

    public LayoutMarquee(double movementSpeed) {
        this.movementSpeed = movementSpeed;
    }

    @Override
    public Rect2D getTransformedRect(IScreenContext screen, IElement element, Rect2D rect) {
        if (screen.getTicks() != this.lastTick) {
            this.lastTick = screen.getTicks();
            this.updatePosition(this.currentPosition + this.movementSpeed);
        }

        if (this.currentPosition > screen.getWidth()) {
            this.currentPosition = -rect.getWidth();
            this.lastPosition = this.currentPosition - this.movementSpeed;
        }

        double x = this.lastPosition + ((this.currentPosition - this.lastPosition) * screen.getPartialTicks());
        return rect.setPosition(x, rect.getY());
    }

    private void updatePosition(double newPosition) {
        this.lastPosition = this.currentPosition;
        this.currentPosition = newPosition;
    }
}
