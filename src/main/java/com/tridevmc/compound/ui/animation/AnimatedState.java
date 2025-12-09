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
 * Unlike StateImpl, this state updates itself over time via the AnimationScheduler.
 * Set a new target value and it smoothly animates from current to target.
 * <p>
 * Uses Minecraft's tick system (20 TPS) with partial tick interpolation for smooth rendering.
 */
public class AnimatedState<T> implements State<T> {
    private final long durationTicks;  // Animation duration in ticks
    private final Interpolator<T> interpolator;
    private final Easing easing;
    private final AnimationScheduler scheduler;
    private final List<StateObserver> observers = new ArrayList<>();
    private T lastTickValue;      // Value at start of current tick (for partial tick interpolation)
    private T currentTickValue;   // Target value for current tick
    private T targetValue;        // Final target value
    private T animationStartValue; // Value when animation started
    private long startTick;       // Tick when animation started
    private long lastUpdateTick = -1;  // Last tick we updated on
    private boolean isAnimating = false;
    private boolean looping = false;  // Whether to loop when animation completes
    private T loopStartValue;     // Value to return to when looping
    private T loopEndValue;       // Value to animate to when looping
    private final T originalLoopStart;  // Original loop start (for reset)
    private final T originalLoopEnd;    // Original loop end (for reset)

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

        // Start looping animation immediately if requested
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
        // Interpolate between last tick and current tick values using partial ticks
        return this.interpolator.interpolate(this.lastTickValue, this.currentTickValue, partialTicks);
    }

    @Override
    public void set(T value) {
        if (Objects.equals(this.targetValue, value)) {
            return;  // Already animating to this value
        }

        // Start new animation (will begin on next tick update)
        this.animationStartValue = this.currentTickValue;
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

        if (this.looping) {
            // Reset loop to original state and restart from the given value
            this.loopStartValue = this.originalLoopStart;
            this.loopEndValue = this.originalLoopEnd;
            this.animationStartValue = value;
            this.targetValue = this.originalLoopEnd;  // Always animate toward the end value first
            this.lastUpdateTick = -1;  // Reset timing so next update starts fresh
        } else {
            // Stop animation (existing behavior for non-looping)
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

        // Start animation if this is the first tick
        if (this.lastUpdateTick == -1) {
            this.startTick = currentTick;
            this.lastUpdateTick = currentTick;
        }

        // Only update once per tick (like LayoutMarquee pattern)
        if (currentTick == this.lastUpdateTick) {
            return;
        }

        // Store last tick's value for partial tick interpolation
        this.lastTickValue = this.currentTickValue;
        this.lastUpdateTick = currentTick;

        long elapsedTicks = currentTick - this.startTick;
        float progress = Math.min(1.0f, elapsedTicks / (float) this.durationTicks);

        T newValue;
        if (progress >= 1.0f) {
            // Animation complete
            newValue = this.targetValue;

            if (this.looping) {
                // Restart animation with swapped start/end values
                this.animationStartValue = this.loopEndValue;
                this.targetValue = this.loopStartValue;
                // Swap for next iteration
                T temp = this.loopStartValue;
                this.loopStartValue = this.loopEndValue;
                this.loopEndValue = temp;
                // Reset timing
                this.startTick = currentTick;
                this.lastUpdateTick = currentTick;
            } else {
                this.isAnimating = false;
                this.scheduler.unregisterAnimation(this);
                this.lastUpdateTick = -1;
            }
        } else {
            // Interpolate from animation start to target
            float easedProgress = this.easing.apply(progress);
            newValue = this.interpolator.interpolate(this.animationStartValue, this.targetValue, easedProgress);
        }

        // Update current tick value
        if (!Objects.equals(this.currentTickValue, newValue)) {
            this.currentTickValue = newValue;

            // Only notify if there are observers (i.e., someone called bindLayout())
            if (!this.observers.isEmpty()) {
                this.notifyObservers();
            }
        }
    }

    public boolean isAnimating() {
        return this.isAnimating;
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
