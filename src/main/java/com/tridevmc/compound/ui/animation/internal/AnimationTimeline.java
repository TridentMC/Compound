package com.tridevmc.compound.ui.animation.internal;

import com.tridevmc.compound.ui.animation.api.IAnimationTimeline;
import java.time.Duration;
import java.util.Objects;

public final class AnimationTimeline implements IAnimationTimeline {
    private double elapsed;
    private double speed = 1;
    private boolean paused;
    private Object owner;
    private long timestamp;
    private boolean hasTimestamp;
    private long generation;

    public void attach(Object owner) {
        Objects.requireNonNull(owner);
        if (this.owner != null && this.owner != owner) {
            throw new IllegalStateException("Timeline already attached to another node");
        }
        this.owner = owner;
    }

    public void detach(Object owner) {
        if (this.owner == owner) {
            this.owner = null;
            this.hasTimestamp = false;
        }
    }

    public void advance(long nanos) {
        if (this.owner == null) return;
        if (!this.hasTimestamp) {
            this.timestamp = nanos;
            this.hasTimestamp = true;
        } else if (nanos > this.timestamp) {
            if (!this.paused) this.elapsed += (nanos - this.timestamp) / 1_000_000_000.0 * this.speed;
            this.timestamp = nanos;
        }
    }

    public static double seconds(Duration duration) {
        Objects.requireNonNull(duration);
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Animation duration must be positive");
        }
        return duration.getSeconds() + duration.getNano() / 1_000_000_000.0;
    }

    public long generation() { return this.generation; }
    @Override public double elapsedSeconds() { return this.elapsed; }
    @Override public double phase(Duration period) { return (this.elapsed / seconds(period)) % 1; }
    @Override public void pause() { this.paused = true; }
    @Override public void resume() { this.paused = false; }
    @Override public boolean isPaused() { return this.paused; }
    @Override public void reset() { this.elapsed = 0; this.generation++; }
    @Override public double speed() { return this.speed; }
    @Override public void speed(double speed) {
        if (!Double.isFinite(speed) || speed <= 0) {
            throw new IllegalArgumentException("Animation speed must be finite and positive");
        }
        this.speed = speed;
    }
}
