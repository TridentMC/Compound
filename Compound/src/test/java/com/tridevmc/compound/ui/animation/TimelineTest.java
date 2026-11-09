package com.tridevmc.compound.ui.animation;

import com.tridevmc.compound.ui.animation.api.IAnimatedValue;
import com.tridevmc.compound.ui.animation.api.IAnimationTimeline;
import com.tridevmc.compound.ui.animation.internal.AnimationTimeline;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimelineTest {
    @Test void fractionalFramesPauseSpeedResetAndBadTimestamps() {
        var clock = (AnimationTimeline) IAnimationTimeline.create();
        clock.attach(this);
        clock.advance(100);
        clock.advance(250_000_100);
        assertEquals(.25, clock.elapsedSeconds());
        clock.advance(100);
        clock.advance(250_000_100);
        assertEquals(.25, clock.elapsedSeconds());
        clock.pause();
        clock.advance(1_250_000_100);
        clock.speed(2);
        clock.resume();
        clock.advance(1_500_000_100);
        assertEquals(.75, clock.elapsedSeconds());
        assertEquals(.75, clock.phase(Duration.ofSeconds(1)));
        clock.pause();
        clock.reset();
        assertEquals(0, clock.elapsedSeconds());
        assertTrue(clock.isPaused());
        assertEquals(2, clock.speed());
    }

    @Test void detachRebasesAndRejectsOtherOwner() {
        var clock = (AnimationTimeline) IAnimationTimeline.create();
        clock.attach(this);
        assertThrows(IllegalStateException.class, () -> clock.attach(new Object()));
        clock.advance(0);
        clock.advance(1_000_000_000);
        clock.detach(this);
        clock.advance(100_000_000_000L);
        clock.attach(this);
        clock.advance(200_000_000_000L);
        assertEquals(1, clock.elapsedSeconds());
        clock.advance(201_000_000_000L);
        assertEquals(2, clock.elapsedSeconds());
    }

    @Test void typedTweenRetargetAndResetContinuity() {
        var clock = (AnimationTimeline) IAnimationTimeline.create();
        var value = IAnimatedValue.create(clock, 0F, Duration.ofSeconds(1), Interpolators.FLOAT, Easing.LINEAR);
        clock.attach(this);
        clock.advance(0);
        value.target(10F);
        clock.advance(500_000_000);
        assertEquals(5F, value.value());
        value.target(20F);
        assertEquals(5F, value.value());
        clock.advance(1_000_000_000);
        assertEquals(12.5F, value.value());
        clock.advance(20_000_000_000L);
        assertEquals(20F, value.value());
        assertFalse(value.isAnimating());
        clock.reset();
        assertEquals(0F, value.value());
        value.reset(4F);
        value.immediate(9F);
        clock.reset();
        assertEquals(4F, value.value());
    }

    @Test void rejectsInvalidTimingAndValues() {
        var clock = IAnimationTimeline.create();
        for (double speed : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> clock.speed(speed));
        }
        assertThrows(IllegalArgumentException.class, () -> clock.phase(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> IAnimatedValue.create(clock, 0,
                Duration.ofNanos(-1), Interpolators.INT, Easing.LINEAR));
    }
}
