/*
 * Copyright 2018 - 2024 TridentMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridevmc.compound.ui.animation;

import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A state that automatically animates to new target values using interpolation.
 * <p>
 * The AnimationScheduler advances this state over time.
 * Set a new target value and it smoothly animates from current to target.
 * <p>
 * Uses Minecraft's tick system (20 TPS) with partial tick interpolation for smooth rendering.
 */
public class AnimatedState<T> implements State<T> {
    private long durationTicks;
    private final Interpolator<T> interpolator;
    private Easing easing;
    private final AnimationScheduler scheduler;
    private final List<StateObserver> observers = new ArrayList<>();
    private T lastTickValue;
    private T currentTickValue;
    private T targetValue;
    private T animationStartValue;
    private long startTick;
    private long lastUpdateTick = -1;
    private boolean isAnimating = false;
    private boolean looping = false;
    private T loopStartValue;
    private T loopEndValue;
    private final T originalLoopStart;
    private final T originalLoopEnd;

    public AnimatedState(T initialValue, long durationMillis,
                         Interpolator<T> interpolator, Easing easing,
                         AnimationScheduler scheduler) {
        this(initialValue, durationMillis, interpolator, easing, scheduler, false, null, null);
    }

    /**
     * Public constructor for looping animations.
     * Used by composition scope factory methods.
     */
    public AnimatedState(T initialValue, long durationMillis,
                         Interpolator<T> interpolator, Easing easing,
                         AnimationScheduler scheduler,
                         boolean looping, T loopStartValue, T loopEndValue) {
        this.lastTickValue = initialValue;
        this.currentTickValue = initialValue;
        this.targetValue = looping ? loopEndValue : initialValue;
        this.animationStartValue = initialValue;
        // Convert milliseconds to ticks (20 TPS = 50ms per tick)
        this.durationTicks = Math.max(1, durationMillis / 50);
        this.interpolator = interpolator;
        this.easing = easing;
        this.scheduler = scheduler;
        this.looping = looping;
        this.loopStartValue = loopStartValue;
        this.loopEndValue = loopEndValue;
        this.originalLoopStart = loopStartValue;
        this.originalLoopEnd = loopEndValue;
        if (looping) {
            this.isAnimating = true;
            this.scheduler.registerAnimation(this);
        }
    }

    @Override
    public T get() {
        return this.currentTickValue;
    }

    /**
     * Get interpolated value for rendering (uses partial ticks for smooth animation).
     * Should be called during render phase with partial tick value from screen context.
     *
     * @param partialTicks interpolation value 0.0-1.0 within current tick
     * @return interpolated value for smooth rendering
     */
    public T get(float partialTicks) {
        if (!this.isAnimating || Objects.equals(this.lastTickValue, this.currentTickValue)) {
            return this.currentTickValue;
        }
        return this.interpolator.interpolate(this.lastTickValue, this.currentTickValue, partialTicks);
    }

    @Override
    public void set(T value) {
        if (Objects.equals(this.targetValue, value)) {
            return;
        }
        this.animationStartValue = this.currentTickValue;
        this.lastTickValue = this.currentTickValue;
        this.lastUpdateTick = -1;
        this.targetValue = value;
        this.isAnimating = true;

        this.scheduler.registerAnimation(this);
    }

    @Override
    public void update(Function<T, T> updater) {
        this.set(updater.apply(this.currentTickValue));
    }

    /**
     * Set value immediately without animation.
     * For looping animations, jumps to the value and fully resets the loop cycle.
     */
    public void setImmediate(T value) {
        this.lastTickValue = value;
        this.currentTickValue = value;
        this.lastUpdateTick = -1;

        if (this.looping) {
            this.loopStartValue = this.originalLoopStart;
            this.loopEndValue = this.originalLoopEnd;
            this.animationStartValue = value;
            this.targetValue = this.originalLoopEnd;
            this.lastUpdateTick = -1;
        } else {
            this.targetValue = value;
            this.animationStartValue = value;
            this.isAnimating = false;
            this.scheduler.unregisterAnimation(this);
        }

        this.notifyObservers();
    }

    /**
     * Called by AnimationScheduler each tick.
     * Updates the current value based on animation progress.
     * Package-private.
     */
    void updateAnimation(long currentTick) {
        if (!this.isAnimating) {
            return;
        }
        if (this.lastUpdateTick == -1) {
            this.startTick = currentTick;
            this.lastUpdateTick = currentTick;
        }
        if (currentTick == this.lastUpdateTick) {
            return;
        }
        this.lastTickValue = this.currentTickValue;
        this.lastUpdateTick = currentTick;

        long elapsedTicks = currentTick - this.startTick;
        float progress = Math.min(1.0f, elapsedTicks / (float) this.durationTicks);

        T newValue;
        if (progress >= 1.0f) {
            newValue = this.targetValue;

            if (this.looping) {
                this.animationStartValue = this.loopEndValue;
                this.targetValue = this.loopStartValue;
                T temp = this.loopStartValue;
                this.loopStartValue = this.loopEndValue;
                this.loopEndValue = temp;
                this.startTick = currentTick;
                this.lastUpdateTick = currentTick;
            } else {
                this.isAnimating = false;
                this.scheduler.unregisterAnimation(this);
                this.lastUpdateTick = -1;
            }
        } else {
            float easedProgress = this.easing.apply(progress);
            newValue = this.interpolator.interpolate(this.animationStartValue, this.targetValue, easedProgress);
        }
        if (!Objects.equals(this.currentTickValue, newValue)) {
            this.currentTickValue = newValue;
            if (!this.observers.isEmpty()) {
                this.notifyObservers();
            }
        }
    }

    public boolean isAnimating() {
        return this.isAnimating;
    }

    public void setTiming(long durationMillis, Easing easing) {
        if (durationMillis <= 0) throw new IllegalArgumentException("Animation duration must be positive");
        this.easing = Objects.requireNonNull(easing, "easing");
        this.durationTicks = Math.max(1, durationMillis / 50);
        this.animationStartValue = this.currentTickValue;
        this.lastUpdateTick = -1;
    }

    /**
     * Stop looping and complete the current animation.
     * The animation will finish its current cycle and then stop.
     */
    public void stopLooping() {
        this.looping = false;
    }

    @Override
    public void addObserver(StateObserver observer) {
        if (!this.observers.contains(observer)) {
            this.observers.add(observer);
        }
    }

    @Override
    public void removeObserver(StateObserver observer) {
        this.observers.remove(observer);
    }

    @Override
    public void dispose() {
        this.observers.clear();
        this.scheduler.unregisterAnimation(this);
    }

    private void notifyObservers() {
        // Create copy to avoid concurrent modification
        List<StateObserver> observersCopy = new ArrayList<>(this.observers);
        for (StateObserver observer : observersCopy) {
            observer.onStateChanged(this);
        }
    }
}
