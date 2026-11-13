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

import com.tridevmc.compound.ui.state.StateObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

final class TickAnimation<T> implements AnimatedState<T> {
    private static final long UNSTARTED = Long.MIN_VALUE;

    private final Interpolator<T> interpolator;
    private final AnimationScheduler scheduler;
    private final List<StateObserver> observers = new ArrayList<>();
    private long durationTicks;
    private Easing easing;
    private T value;
    private long sampledTick = UNSTARTED;
    private final Transition<T> transition = new Transition<>();
    private Optional<Endpoints<T>> loop = Optional.empty();

    TickAnimation(T initialValue, long durationMillis, Interpolator<T> interpolator,
                  Easing easing, AnimationScheduler scheduler) {
        this.interpolator = Objects.requireNonNull(interpolator, "interpolator");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.value = initialValue;
        this.setTiming(durationMillis, easing);
    }

    void startLoop(T start, T end) {
        this.loop = Optional.of(new Endpoints<>(start, end));
        this.start(end);
    }

    @Override
    public T get() {
        return this.value;
    }

    @Override
    public T get(float partialTicks) {
        if (!this.transition.active || this.sampledTick == UNSTARTED) {
            return this.value;
        }
        return this.sample(this.sampledTick - this.transition.startTick + Math.clamp(partialTicks, 0F, 1F));
    }

    @Override
    public void set(T target) {
        boolean sameTarget = this.loop.isEmpty()
                && Objects.equals(this.transition.active ? this.transition.to : this.value, target);
        this.loop = Optional.empty();
        if (!sameTarget) {
            this.start(target);
        }
    }

    @Override
    public void update(Function<T, T> updater) {
        this.set(updater.apply(this.value));
    }

    @Override
    public void setImmediate(T value) {
        T previous = this.value;
        this.value = value;
        if (this.loop.isPresent()) {
            this.start(this.loop.get().end());
        } else {
            this.stop();
        }
        this.notifyChange(previous);
    }

    private void start(T target) {
        this.transition.from = this.value;
        this.transition.to = target;
        this.transition.startTick = UNSTARTED;
        this.transition.active = true;
        this.sampledTick = UNSTARTED;
        this.scheduler.registerAnimation(this);
    }

    void updateAnimation(long tick) {
        if (!this.transition.active || tick == this.sampledTick) {
            return;
        }
        if (this.transition.startTick == UNSTARTED) {
            this.transition.startTick = tick;
            this.sampledTick = tick;
            return;
        }
        this.sampledTick = tick;
        long elapsed = Math.max(0, tick - this.transition.startTick);
        if (elapsed >= this.durationTicks && this.loop.isPresent()) {
            this.advanceLoop(elapsed);
            elapsed = tick - this.transition.startTick;
        }
        T previous = this.value;
        this.value = this.sample(elapsed);
        if (elapsed >= this.durationTicks) {
            this.stop();
        }
        this.notifyChange(previous);
    }

    private void advanceLoop(long elapsed) {
        var endpoints = this.loop.orElseThrow();
        long completedLegs = elapsed / this.durationTicks;
        T destination = this.transition.to;
        T opposite = Objects.equals(destination, endpoints.end()) ? endpoints.start() : endpoints.end();
        boolean reversed = completedLegs % 2 == 1;
        this.transition.from = reversed ? destination : opposite;
        this.transition.to = reversed ? opposite : destination;
        this.transition.startTick += completedLegs * this.durationTicks;
    }

    private T sample(float elapsed) {
        if (elapsed >= this.durationTicks) {
            return this.transition.to;
        }
        float progress = this.easing.apply(elapsed / this.durationTicks);
        if (progress == 0F) {
            return this.transition.from;
        }
        if (progress == 1F) {
            return this.transition.to;
        }
        return this.interpolator.interpolate(this.transition.from, this.transition.to, progress);
    }

    @Override
    public boolean isAnimating() {
        return this.transition.active;
    }

    @Override
    public void setTiming(long durationMillis, Easing easing) {
        if (durationMillis <= 0) {
            throw new IllegalArgumentException("Animation duration must be positive");
        }
        this.easing = Objects.requireNonNull(easing, "easing");
        this.durationTicks = Math.max(1, durationMillis / 50);
        if (this.transition.active) {
            this.start(this.transition.to);
        }
    }

    @Override
    public void stopLooping() {
        this.loop = Optional.empty();
    }

    private void stop() {
        this.transition.active = false;
        this.transition.from = null;
        this.transition.to = null;
        this.transition.startTick = UNSTARTED;
        this.sampledTick = UNSTARTED;
        this.scheduler.unregisterAnimation(this);
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
        this.stop();
        this.loop = Optional.empty();
        this.observers.clear();
    }

    private void notifyChange(T previous) {
        if (!this.observers.isEmpty() && !Objects.equals(previous, this.value)) {
            for (var observer : new ArrayList<>(this.observers)) {
                observer.onStateChanged(this);
            }
        }
    }

    private static final class Transition<T> {
        private T from;
        private T to;
        private long startTick = UNSTARTED;
        private boolean active;
    }

    private record Endpoints<T>(T start, T end) { }
}

