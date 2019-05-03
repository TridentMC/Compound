package com.tridevmc.compound.ui;

/**
 * Defines a 2D area on the screen, used for UI element positioning.
 */
public class Rect2D {

    private final double x, y;
    private final double width, height;

    public Rect2D(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rect2D(Rect2D rect2D) {
        this.x = rect2D.x;
        this.y = rect2D.y;
        this.width = rect2D.width;
        this.height = rect2D.height;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getWidth() {
        return this.width;
    }

    public double getHeight() {
        return this.height;
    }

    public Rect2D offset(Rect2D by) {
        return new Rect2D(this.getX() + by.getX(),
                this.getY() + by.getY(),
                this.getWidth() + by.getWidth(),
                this.getHeight() + by.getHeight());
    }

    public Rect2D offsetPosition(double x, double y) {
        return this.offset(new Rect2D(x, y, 0, 0));
    }

    public Rect2D offsetSize(double width, double height) {
        return this.offset(new Rect2D(0, 0, width, height));
    }

    public Rect2D setPosition(double x, double y) {
        return new Rect2D(x, y, this.getWidth(), this.getHeight());
    }

    public Rect2D setSize(double width, double height) {
        return new Rect2D(this.getX(), this.getY(), width, height);
    }

    public boolean isPointInRect(double x, double y) {
        return x <= this.getX() + this.getWidth() && x >= this.getX() &&
                y <= this.getY() + this.getHeight() && y >= this.getY();
    }
}
