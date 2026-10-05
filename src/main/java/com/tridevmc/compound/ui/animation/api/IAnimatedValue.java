package com.tridevmc.compound.ui.animation.api;

import com.tridevmc.compound.ui.animation.Easing;
import com.tridevmc.compound.ui.animation.Interpolator;
import com.tridevmc.compound.ui.animation.internal.AnimatedValue;
import java.time.Duration;

/** Typed tween sampled without observable-state notifications or layout invalidation. */
public interface IAnimatedValue<T> {
    static <T> IAnimatedValue<T> create(IAnimationTimeline timeline, T initial,
            Duration duration, Interpolator<T> interpolator, Easing easing) {
        return new AnimatedValue<>(timeline, initial, duration, interpolator, easing);
    }

    T value();
    T target();

    /** Retargets continuously from the currently sampled value. */
    void target(T value);
    void immediate(T value);

    /** Changes both current value and the value restored by timeline reset. */
    void reset(T value);
    void timing(Duration duration, Easing easing);
    boolean isAnimating();
}
