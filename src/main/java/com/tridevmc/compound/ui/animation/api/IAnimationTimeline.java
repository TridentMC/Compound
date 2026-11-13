package com.tridevmc.compound.ui.animation.api;

import com.tridevmc.compound.ui.animation.internal.AnimationTimeline;
import java.time.Duration;

/** A retained render-time clock. Attach with scope.useAnimationTimeline; detach freezes time. */
public interface IAnimationTimeline {
    static IAnimationTimeline create() {
        return new AnimationTimeline();
    }

    double elapsedSeconds();

    /** Periodic progress in [0, 1); period must be positive. */
    double phase(Duration period);

    void pause();
    void resume();
    boolean isPaused();

    /** Returns to zero and restores attached tweens, preserving speed and explicit pause. */
    void reset();

    /** Finite, strictly positive playback multiplier. */
    void speed(double speed);
    double speed();
}
