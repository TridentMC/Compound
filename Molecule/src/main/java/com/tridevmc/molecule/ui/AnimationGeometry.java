package com.tridevmc.molecule.ui;

final class AnimationGeometry {
    static final int GRADIENT_HEIGHT = 28;
    static final int MARQUEE_TOP = 80;
    static final int DVD_TOP = 93;
    static final double SPIN_FRACTION = .22;
    static final int SPIN_Y = 53;
    static final int SPIN_HALF_SIZE = 12;
    static final double BOUNCE_FRACTION = .65;
    static final int BOUNCE_HALF_WIDTH = 15;
    static final int BOUNCE_HALF_HEIGHT = 7;
    static final int BOUNCE_BASE = 43;
    static final int BOUNCE_TRAVEL = 22;

    private AnimationGeometry() { }

    static boolean isMarquee(double y) {
        return y >= MARQUEE_TOP && y < DVD_TOP;
    }
}
