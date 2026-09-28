package com.tridevmc.compound.ui.animation;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimatedStateTest {
    private static final float EPSILON = 0.000001F;

    @Test
    void duplicateSamplesDoNotAdvanceOrNotifyTwice() {
        var scheduler = new AnimationScheduler();
        var state = AnimatedState.of(0F, 200, Interpolators.FLOAT, Easing.LINEAR, scheduler);
        var notifications = new AtomicInteger();
        state.addObserver(ignored -> notifications.incrementAndGet());
        state.set(1F);

        scheduler.updateAnimations(100);
        scheduler.updateAnimations(100);
        assertEquals(0F, state.get(), EPSILON);
        assertEquals(0, notifications.get());

        scheduler.updateAnimations(101);
        scheduler.updateAnimations(101);
        assertEquals(0.25F, state.get(), EPSILON);
        assertEquals(1, notifications.get());

        scheduler.updateAnimations(104);
        assertEquals(1F, state.get(), EPSILON);
        assertFalse(state.isAnimating());
        assertFalse(scheduler.hasActiveAnimations());
    }

    @Test
    void skippedSamplesPreserveLoopPhaseInBothDirections() {
        var scheduler = new AnimationScheduler();
        var state = AnimatedState.looping(0F, 1F, 200, Interpolators.FLOAT, Easing.LINEAR, scheduler);
        scheduler.updateAnimations(10);

        scheduler.updateAnimations(20);
        assertEquals(0.5F, state.get(), EPSILON);
        scheduler.updateAnimations(23);
        assertEquals(0.75F, state.get(), EPSILON);
        scheduler.updateAnimations(26);
        assertEquals(0F, state.get(), EPSILON);
        assertTrue(state.isAnimating());
        assertEquals(1, scheduler.getActiveAnimationCount());
    }

    @Test
    void retargetingStartsFromTheCurrentValueAndEndsLooping() {
        var scheduler = new AnimationScheduler();
        var state = AnimatedState.looping(0F, 1F, 200, Interpolators.FLOAT, Easing.LINEAR, scheduler);
        scheduler.updateAnimations(0);
        scheduler.updateAnimations(1);
        state.set(0.75F);
        assertEquals(0.25F, state.get(), EPSILON);

        scheduler.updateAnimations(1);
        scheduler.updateAnimations(3);
        assertEquals(0.5F, state.get(), EPSILON);
        scheduler.updateAnimations(5);
        scheduler.updateAnimations(20);
        assertEquals(0.75F, state.get(), EPSILON);
        assertFalse(state.isAnimating());
    }

    @Test
    void requestingTheExistingLoopTargetStillEndsLooping() {
        var scheduler = new AnimationScheduler();
        var state = AnimatedState.looping(0F, 1F, 200, Interpolators.FLOAT, Easing.LINEAR, scheduler);
        scheduler.updateAnimations(0);
        scheduler.updateAnimations(1);
        state.set(1F);
        scheduler.updateAnimations(1);
        scheduler.updateAnimations(5);
        scheduler.updateAnimations(9);

        assertEquals(1F, state.get(), EPSILON);
        assertFalse(state.isAnimating());
    }

    @Test
    void immediateLoopResetRestoresTheOriginalDestination() {
        var scheduler = new AnimationScheduler();
        var state = AnimatedState.looping(0F, 1F, 200, Interpolators.FLOAT, Easing.LINEAR, scheduler);
        scheduler.updateAnimations(0);
        scheduler.updateAnimations(6);
        assertEquals(0.5F, state.get(), EPSILON);

        state.setImmediate(0.25F);
        assertEquals(0.25F, state.get(), EPSILON);
        scheduler.updateAnimations(6);
        scheduler.updateAnimations(8);
        assertEquals(0.625F, state.get(), EPSILON);
        scheduler.updateAnimations(10);
        assertEquals(1F, state.get(), EPSILON);
        scheduler.updateAnimations(12);
        assertEquals(0.5F, state.get(), EPSILON);
    }

    @Test
    void changingTimingRestartsFromTheCurrentValue() {
        var scheduler = new AnimationScheduler();
        var state = AnimatedState.of(0F, 200, Interpolators.FLOAT, Easing.LINEAR, scheduler);
        state.set(1F);
        scheduler.updateAnimations(100);
        scheduler.updateAnimations(101);
        state.setTiming(100, Easing.LINEAR);
        assertEquals(0.25F, state.get(), EPSILON);

        scheduler.updateAnimations(101);
        scheduler.updateAnimations(102);
        assertEquals(0.625F, state.get(), EPSILON);
        scheduler.updateAnimations(103);
        assertEquals(1F, state.get(), EPSILON);
        assertFalse(state.isAnimating());
    }

    @Test
    void partialSamplesEvaluateTheEasingCurveWithoutChangingState() {
        var scheduler = new AnimationScheduler();
        var state = AnimatedState.of(0F, 200, Interpolators.FLOAT, Easing.EASE_IN, scheduler);
        state.set(1F);
        scheduler.updateAnimations(0);
        scheduler.updateAnimations(1);

        assertEquals(0.0625F, state.get(), EPSILON);
        assertEquals(0.140625F, state.get(0.5F), EPSILON);
        assertEquals(0.0625F, state.get(), EPSILON);

        scheduler.updateAnimations(3);
        assertEquals(0.765625F, state.get(0.5F), EPSILON);
        scheduler.updateAnimations(4);
        assertEquals(1F, state.get(0.5F), EPSILON);
    }

    @Test
    void partialSamplesDoNotFadeStepTransitions() {
        var scheduler = new AnimationScheduler();
        var state = AnimatedState.looping(0F, 1F, 200, Interpolators.FLOAT, Easing.STEP, scheduler);
        scheduler.updateAnimations(0);
        scheduler.updateAnimations(3);
        assertEquals(0F, state.get(0.9F), EPSILON);
        scheduler.updateAnimations(4);
        assertEquals(1F, state.get(0.5F), EPSILON);
        scheduler.updateAnimations(8);
        assertEquals(0F, state.get(0.5F), EPSILON);
    }

    @Test
    void immediateOneShotValueStopsScheduling() {
        var scheduler = new AnimationScheduler();
        var state = AnimatedState.of(0F, 200, Interpolators.FLOAT, Easing.LINEAR, scheduler);
        state.set(1F);
        state.setImmediate(0.75F);
        scheduler.updateAnimations(100);

        assertEquals(0.75F, state.get(), EPSILON);
        assertFalse(state.isAnimating());
        assertFalse(scheduler.hasActiveAnimations());
    }

    @Test
    void disposalDuringAnObserverSkipsTheOtherQueuedAnimation() {
        var scheduler = new AnimationScheduler();
        var first = AnimatedState.looping(0F, 1F, 200, Interpolators.FLOAT, Easing.LINEAR, scheduler);
        var second = AnimatedState.looping(0F, 1F, 200, Interpolators.FLOAT, Easing.LINEAR, scheduler);
        var notifications = new AtomicInteger();
        first.addObserver(ignored -> {
            notifications.incrementAndGet();
            first.dispose();
            second.dispose();
        });
        second.addObserver(ignored -> {
            notifications.incrementAndGet();
            first.dispose();
            second.dispose();
        });
        scheduler.updateAnimations(0);
        scheduler.updateAnimations(1);

        assertEquals(1, notifications.get());
        assertEquals(0.25F, first.get() + second.get(), EPSILON);
        assertFalse(first.isAnimating());
        assertFalse(second.isAnimating());
        assertFalse(scheduler.hasActiveAnimations());
    }

    @Test
    void disposingSchedulerDuringAnObserverStopsTheRemainingSnapshot() {
        var scheduler = new AnimationScheduler();
        var first = AnimatedState.looping(0F, 1F, 200, Interpolators.FLOAT, Easing.LINEAR, scheduler);
        var second = AnimatedState.looping(0F, 1F, 200, Interpolators.FLOAT, Easing.LINEAR, scheduler);
        var notifications = new AtomicInteger();
        first.addObserver(ignored -> {
            notifications.incrementAndGet();
            scheduler.dispose();
        });
        second.addObserver(ignored -> {
            notifications.incrementAndGet();
            scheduler.dispose();
        });
        scheduler.updateAnimations(0);
        scheduler.updateAnimations(1);

        assertEquals(1, notifications.get());
        assertEquals(0.25F, first.get() + second.get(), EPSILON);
        assertFalse(scheduler.hasActiveAnimations());
    }

    @Test
    void durationsMustBePositive() {
        var scheduler = new AnimationScheduler();
        var state = AnimatedState.of(0F, 200, Interpolators.FLOAT, Easing.LINEAR, scheduler);
        for (long duration : new long[]{0, -1}) {
            assertThrows(IllegalArgumentException.class, () ->
                    AnimatedState.of(0F, duration, Interpolators.FLOAT, Easing.LINEAR, scheduler));
            assertThrows(IllegalArgumentException.class, () ->
                    AnimatedState.looping(0F, 1F, duration, Interpolators.FLOAT, Easing.LINEAR, scheduler));
            assertThrows(IllegalArgumentException.class, () -> state.setTiming(duration, Easing.LINEAR));
        }
    }
}
