package com.tridevmc.molecule.ui;

final class ReflectedMotion {
    record Axis(double position, double velocity, long collisions) { }

    static Axis advance(double position, double velocity, double seconds, double extent) {
        if (extent <= 0) return new Axis(0, velocity, 0);
        position = Math.clamp(position, 0, extent);
        double unfolded = position + velocity * seconds;
        double wrapped = ((unfolded % (2 * extent)) + 2 * extent) % (2 * extent);
        double result = wrapped <= extent ? wrapped : 2 * extent - wrapped;
        long crossings = velocity >= 0
                ? (long) Math.floor(unfolded / extent) - (long) Math.floor(position / extent)
                : (long) Math.ceil(position / extent) - (long) Math.ceil(unfolded / extent);
        double outgoing = wrapped == 0 ? Math.abs(velocity)
                : wrapped == extent ? -Math.abs(velocity)
                : (wrapped < extent ? velocity : -velocity);
        return new Axis(result, outgoing, Math.max(0, crossings));
    }

    static boolean containsRotated(double x, double y, double centerX, double centerY,
            double angle, double halfSize) {
        double dx = x - centerX;
        double dy = y - centerY;
        double localX = dx * Math.cos(angle) + dy * Math.sin(angle);
        double localY = -dx * Math.sin(angle) + dy * Math.cos(angle);
        return Math.abs(localX) <= halfSize && Math.abs(localY) <= halfSize;
    }

    private ReflectedMotion() { }
}
