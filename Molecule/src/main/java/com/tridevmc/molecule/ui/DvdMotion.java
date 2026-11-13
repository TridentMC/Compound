package com.tridevmc.molecule.ui;

final class DvdMotion {
    private double x;
    private double y;
    private double vx;
    private double vy;
    private double lastTime;
    private double maxX;
    private double maxY;
    private double dragX;
    private double dragY;
    private boolean dragging;
    private long collisions;
    private static final InitialMotion INITIAL = new InitialMotion(12, 8, 65, 43);

    DvdMotion() {
        this.reset();
    }

    void advance(double time, double width, double height, double objectWidth, double objectHeight) {
        double dx = Math.max(0, width - objectWidth);
        double dy = Math.max(0, height - objectHeight);
        if (time < this.lastTime) this.reset();
        if (dx != this.maxX || dy != this.maxY) {
            this.x = Math.clamp(this.x, 0, dx);
            this.y = Math.clamp(this.y, 0, dy);
            this.maxX = dx;
            this.maxY = dy;
            this.lastTime = time;
        }
        if (!this.dragging) {
            var horizontal = ReflectedMotion.advance(this.x, this.vx, time - this.lastTime, dx);
            var vertical = ReflectedMotion.advance(this.y, this.vy, time - this.lastTime, dy);
            this.x = horizontal.position();
            this.y = vertical.position();
            this.vx = horizontal.velocity();
            this.vy = vertical.velocity();
            this.collisions += horizontal.collisions() + vertical.collisions();
        }
        this.lastTime = time;
    }

    boolean beginDrag(double x, double y, double width, double height) {
        if (x < this.x || x > this.x + width || y < this.y || y > this.y + height) {
            return false;
        }
        this.dragging = true;
        this.dragX = x - this.x;
        this.dragY = y - this.y;
        return true;
    }

    void drag(double x, double y) {
        this.x = Math.clamp(x - this.dragX, 0, this.maxX);
        this.y = Math.clamp(y - this.dragY, 0, this.maxY);
    }

    void release(double time) {
        this.dragging = false;
        this.lastTime = time;
    }

    void reset() {
        this.x = INITIAL.x();
        this.y = INITIAL.y();
        this.vx = INITIAL.vx();
        this.vy = INITIAL.vy();
        this.lastTime = 0;
        this.maxX = -1;
        this.maxY = -1;
        this.collisions = 0;
        this.dragging = false;
    }

    double x() { return this.x; }
    double y() { return this.y; }
    double maxX() { return this.maxX; }
    double maxY() { return this.maxY; }
    boolean isDragging() { return this.dragging; }
    long collisions() { return this.collisions; }

    private record InitialMotion(double x, double y, double vx, double vy) { }
}
