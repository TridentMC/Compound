package com.tridevmc.compound.ui;

import com.google.common.base.MoreObjects;

/**
 * Defines a 2D area on the screen, used for UI element positioning.
 */
public class Rect2F {

    private final float x, y;
    private final float width, height;

    public Rect2F(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rect2F(Rect2F rect2F) {
        this.x = rect2F.x;
        this.y = rect2F.y;
        this.width = rect2F.width;
        this.height = rect2F.height;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }

    /**
     * Creates a new Rect by offsetting this rect by the given rect.
     *
     * @param by the offset to apply to this rect.
     * @return the new offset rect.
     */
    public Rect2F offset(Rect2F by) {
        return new Rect2F(this.getX() + by.getX(),
                this.getY() + by.getY(),
                this.getWidth() + by.getWidth(),
                this.getHeight() + by.getHeight());
    }

    /**
     * Creates a new Rect by offsetting this rect by the given x, y coordinates.
     *
     * @param x the x coordinate to offset by.
     * @param y the y coordinate to offset by.
     * @return the new offset rect.
     */
    public Rect2F offsetPosition(float x, float y) {
        return this.offset(new Rect2F(x, y, 0, 0));
    }

    /**
     * Creates a new Rect by offsetting this rect by the given width and height.
     *
     * @param width  the width to offset by.
     * @param height the height to offset by.
     * @return the new offset rect.
     */
    public Rect2F offsetSize(float width, float height) {
        return this.offset(new Rect2F(0, 0, width, height));
    }

    /**
     * Creates a new rect with the same dimensions but located at the given coordinates.
     *
     * @param x the x position of the new rect.
     * @param y the y position of the new rect.
     * @return the new rect in the new position.
     */
    public Rect2F setPosition(float x, float y) {
        return new Rect2F(x, y, this.getWidth(), this.getHeight());
    }

    /**
     * Creates a new rect with the same position but with the given dimensions.
     *
     * @param width  the width of the new rect.
     * @param height the height of the new rect.
     * @return the new rect with the new dimensions.
     */
    public Rect2F setSize(float width, float height) {
        return new Rect2F(this.getX(), this.getY(), width, height);
    }

    /**
     * Checks if the given x, y coordinates are present in this rect.
     *
     * @param x the x position to check.
     * @param y the y position to check.
     * @return true if the given point is in this rect, false otherwise.
     */
    public boolean isPointInRect(double x, double y) {
        return x <= this.getX() + this.getWidth() && x >= this.getX() &&
                y <= this.getY() + this.getHeight() && y >= this.getY();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("x", this.x)
                .add("y", this.y)
                .add("width", this.width)
                .add("height", this.height)
                .toString();
    }
}
