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
    private Optional<Leg<T>> playback = Optional.empty();
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
        if (this.playback.isEmpty() || this.sampledTick == UNSTARTED) {
            return this.value;
        }
        var leg = this.playback.get();
        return this.sample(leg, this.sampledTick - leg.startTick() + Math.clamp(partialTicks, 0F, 1F));
    }

    @Override
    public void set(T target) {
        boolean sameTarget = this.loop.isEmpty()
                && Objects.equals(this.playback.isPresent() ? this.playback.get().to() : this.value, target);
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
        this.playback = Optional.of(new Leg<>(this.value, target, UNSTARTED));
        this.sampledTick = UNSTARTED;
        this.scheduler.registerAnimation(this);
    }

    void updateAnimation(long tick) {
        if (this.playback.isEmpty() || tick == this.sampledTick) {
            return;
        }
        var leg = this.playback.get();
        if (leg.startTick() == UNSTARTED) {
            this.playback = Optional.of(new Leg<>(leg.from(), leg.to(), tick));
            this.sampledTick = tick;
            return;
        }
        this.sampledTick = tick;
        long elapsed = Math.max(0, tick - leg.startTick());
        if (elapsed >= this.durationTicks && this.loop.isPresent()) {
            leg = this.advanceLoop(leg, elapsed);
            this.playback = Optional.of(leg);
            elapsed = tick - leg.startTick();
        }
        T previous = this.value;
        this.value = this.sample(leg, elapsed);
        if (elapsed >= this.durationTicks) {
            this.stop();
        }
        this.notifyChange(previous);
    }

    private Leg<T> advanceLoop(Leg<T> leg, long elapsed) {
        var endpoints = this.loop.orElseThrow();
        long completedLegs = elapsed / this.durationTicks;
        T opposite = Objects.equals(leg.to(), endpoints.end()) ? endpoints.start() : endpoints.end();
        T from = completedLegs % 2 == 1 ? leg.to() : opposite;
        T to = completedLegs % 2 == 1 ? opposite : leg.to();
        return new Leg<>(from, to, leg.startTick() + completedLegs * this.durationTicks);
    }

    private T sample(Leg<T> leg, float elapsed) {
        if (elapsed >= this.durationTicks) {
            return leg.to();
        }
        return this.interpolator.interpolate(leg.from(), leg.to(), this.easing.apply(elapsed / this.durationTicks));
    }

    @Override
    public boolean isAnimating() {
        return this.playback.isPresent();
    }

    @Override
    public void setTiming(long durationMillis, Easing easing) {
        if (durationMillis <= 0) {
            throw new IllegalArgumentException("Animation duration must be positive");
        }
        this.easing = Objects.requireNonNull(easing, "easing");
        this.durationTicks = Math.max(1, durationMillis / 50);
        this.playback.ifPresent(leg -> this.start(leg.to()));
    }

    @Override
    public void stopLooping() {
        this.loop = Optional.empty();
    }

    private void stop() {
        this.playback = Optional.empty();
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
        if (!Objects.equals(previous, this.value)) {
            for (var observer : new ArrayList<>(this.observers)) {
                observer.onStateChanged(this);
            }
        }
    }

    private record Leg<T>(T from, T to, long startTick) { }

    private record Endpoints<T>(T start, T end) { }
}

