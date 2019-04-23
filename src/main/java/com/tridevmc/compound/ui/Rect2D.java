package com.tridevmc.compound.ui;

/**
 * Defines a 2D area on the screen, used for UI element positioning.
 */
public class Rect2D {

    private double x, y;
    private double width, height;

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

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public void setHeight(double height) {
        this.height = height;
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
}
