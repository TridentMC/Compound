package com.tridevmc.compound.ui.animation.internal;

import com.tridevmc.compound.ui.animation.Easing;
import com.tridevmc.compound.ui.animation.Interpolator;
import com.tridevmc.compound.ui.animation.api.IAnimatedValue;
import com.tridevmc.compound.ui.animation.api.IAnimationTimeline;
import java.time.Duration;
import java.util.Objects;

public final class AnimatedValue<T> implements IAnimatedValue<T> {
    private final AnimationTimeline timeline;
    private final Interpolator<T> interpolator;
    private T initial;
    private T start;
    private T target;
    private double started;
    private double duration;
    private Easing easing;
    private long generation;

    public AnimatedValue(IAnimationTimeline timeline, T initial, Duration duration,
            Interpolator<T> interpolator, Easing easing) {
        if (!(timeline instanceof AnimationTimeline clock)) {
            throw new IllegalArgumentException("Use IAnimationTimeline.create()");
        }
        this.timeline = clock;
        this.interpolator = Objects.requireNonNull(interpolator);
        this.timing(duration, easing);
        this.reset(initial);
    }

    private void synchronizeReset() {
        if (this.generation != this.timeline.generation()) this.immediate(this.initial);
    }

    @Override
    public T value() {
        this.synchronizeReset();
        double progress = Math.clamp((this.timeline.elapsedSeconds() - this.started) / this.duration, 0, 1);
        return progress >= 1 ? this.target : this.interpolator.interpolate(this.start,
                this.target, this.easing.apply((float) progress));
    }

    @Override public T target() { this.synchronizeReset(); return this.target; }

    @Override
    public void target(T value) {
        Objects.requireNonNull(value);
        T current = this.value();
        if (value.equals(this.target)) return;
        this.start = current;
        this.target = value;
        this.started = this.timeline.elapsedSeconds();
    }

    @Override
    public void immediate(T value) {
        this.start = Objects.requireNonNull(value);
        this.target = value;
        this.started = this.timeline.elapsedSeconds();
        this.generation = this.timeline.generation();
    }

    @Override public void reset(T value) { this.initial = Objects.requireNonNull(value); this.immediate(value); }
    @Override public void timing(Duration duration, Easing easing) {
        this.duration = AnimationTimeline.seconds(duration);
        this.easing = Objects.requireNonNull(easing);
    }
    @Override public boolean isAnimating() {
        this.synchronizeReset();
        return !this.start.equals(this.target) && this.timeline.elapsedSeconds() - this.started < this.duration;
    }
}
