/*
 * Copyright 2018 - 2026 TridentMC
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

/**
 * Observable animation driven by an {@link AnimationScheduler} on the client thread.
 * Composition scopes provide factories that also dispose animations when their element detaches.
 *
 * @param <T> the animated value type
 */
public interface AnimatedState<T> extends State<T> {
    /**
     * Creates an idle state that animates when {@link #set} supplies a target.
     *
     * @param initialValue the initial value
     * @param durationMillis transition duration, positive and rounded down to ticks (minimum one tick)
     * @param interpolator interpolation for the value type
     * @param easing progression within each transition
     * @param scheduler the scheduler that will advance this state
     * @param <T> the value type
     * @return a new animation; the caller owns its disposal
     */
    static <T> AnimatedState<T> of(T initialValue, long durationMillis, Interpolator<T> interpolator,
                                   Easing easing, AnimationScheduler scheduler) {
        return new TickAnimation<>(initialValue, durationMillis, interpolator, easing, scheduler);
    }

    /**
     * Creates an animation alternating between two endpoints. The first scheduler update
     * establishes its start tick; skipped ticks preserve the loop's elapsed phase.
     *
     * @param startValue the initial endpoint
     * @param endValue the other endpoint
     * @param durationMillis duration of each leg, positive and rounded down to ticks (minimum one tick)
     * @param interpolator interpolation for the value type
     * @param easing progression within each leg; {@link Easing#STEP} toggles instantly
     * @param scheduler the scheduler that will advance this state
     * @param <T> the value type
     * @return a running loop; the caller owns its disposal
     */
    static <T> AnimatedState<T> looping(T startValue, T endValue, long durationMillis,
                                        Interpolator<T> interpolator, Easing easing,
                                        AnimationScheduler scheduler) {
        var animation = new TickAnimation<>(startValue, durationMillis, interpolator, easing, scheduler);
        animation.startLoop(startValue, endValue);
        return animation;
    }

    /**
     * Samples the easing curve between the last scheduler tick and the next tick, without
     * changing state or notifying observers. STEP easing remains an instantaneous toggle.
     *
     * @param partialTicks fraction of the tick, from zero to one
     * @return the value at that render time
     */
    T get(float partialTicks);

    /**
     * Ends looping and animates from the current value to the requested target.
     * Repeating an existing one-shot target leaves its progress unchanged.
     *
     * @param value the target value
     */
    @Override
    void set(T value);

    /**
     * Applies a value immediately. A loop restarts toward its original end endpoint;
     * a one-shot animation stops. Observers are notified only if the value changes.
     *
     * @param value the new value
     */
    void setImmediate(T value);

    /**
     * Reports whether a transition is scheduled.
     *
     * @return whether this state has a scheduled transition
     */
    boolean isAnimating();

    /**
     * Changes timing and restarts the current leg from its current value.
     *
     * @param durationMillis positive transition duration, rounded down to ticks (minimum one tick)
     * @param easing the new easing curve
     */
    void setTiming(long durationMillis, Easing easing);

    /** Ends looping after the current leg reaches its target. */
    void stopLooping();
}
