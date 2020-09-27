package com.tridevmc.compound.ui.layout;

import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * A layout that makes an element travel across the screen horizontally and then wraps it around the beginning after completion.
 * <p>
 * There's literally no reason anyone should ever use this, it's just a useful test of the layout system.
 */
public class LayoutMarquee implements ILayout {
    private long lastTick = -1;
    private float lastPosition, currentPosition;
    private float movementSpeed = 1F;

    public LayoutMarquee() {
    }

    public LayoutMarquee(float movementSpeed) {
        this.movementSpeed = movementSpeed;
    }

    @Override
    public Rect2F getTransformedRect(IScreenContext screen, IElement element, Rect2F rect) {
        if (screen.getTicks() != this.lastTick) {
            this.lastTick = screen.getTicks();
            this.updatePosition(this.currentPosition + this.movementSpeed);
        }

        if (this.currentPosition > screen.getWidth()) {
            this.currentPosition = -rect.getWidth();
            this.lastPosition = this.currentPosition - this.movementSpeed;
        }

        float x = this.lastPosition + ((this.currentPosition - this.lastPosition) * screen.getPartialTicks());
        return rect.setPosition(x, rect.getY());
    }

    private void updatePosition(float newPosition) {
        this.lastPosition = this.currentPosition;
        this.currentPosition = newPosition;
    }
}
