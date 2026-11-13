package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.animation.Easing;
import com.tridevmc.compound.ui.animation.Interpolators;
import com.tridevmc.compound.ui.animation.api.IAnimatedValue;
import com.tridevmc.compound.ui.animation.api.IAnimationTimeline;
import java.time.Duration;

final class AnimationModel {
    final IAnimationTimeline timeline = IAnimationTimeline.create();
    final IAnimationTimeline marquee = IAnimationTimeline.create();
    final IAnimatedValue<Integer> palette = IAnimatedValue.create(this.timeline, 0xFF4285F4,
            Duration.ofMillis(750), Interpolators.COLOR, Easing.EASE_IN_OUT);
    final DvdMotion dvd = new DvdMotion();
    boolean block;
    boolean hovered;
    private boolean warm;
    private static final double SPIN_SECONDS = 3;
    private static final double MARQUEE_SECONDS = 8;
    private int spinDirection = 1;
    private int marqueeDirection = 1;
    private double spinOrigin;
    private double spinTime;
    private double marqueeOrigin;
    private double marqueeTime;

    double spinTurns() {
        return this.spinOrigin + this.spinDirection
                * (this.timeline.elapsedSeconds() - this.spinTime) / SPIN_SECONDS;
    }

    double marqueeProgress() {
        double progress = this.marqueeOrigin + this.marqueeDirection
                * (this.marquee.elapsedSeconds() - this.marqueeTime) / MARQUEE_SECONDS;
        return progress - Math.floor(progress);
    }

    void reverseSpin() {
        this.spinOrigin = this.spinTurns();
        this.spinTime = this.timeline.elapsedSeconds();
        this.spinDirection *= -1;
    }

    void reverseMarquee() {
        this.marqueeOrigin = this.marqueeProgress();
        this.marqueeTime = this.marquee.elapsedSeconds();
        this.marqueeDirection *= -1;
    }

    void reverse() {
        this.reverseSpin();
        this.reverseMarquee();
    }

    void advance(double width, double height, double objectWidth, double objectHeight) {
        this.dvd.advance(this.timeline.elapsedSeconds(), width, height, objectWidth, objectHeight);
    }

    void togglePause() {
        if (this.timeline.isPaused()) {
            this.timeline.resume();
            if (!this.hovered) this.marquee.resume();
        } else {
            this.timeline.pause();
            this.marquee.pause();
        }
    }

    void hover(boolean hovered) {
        this.hovered = hovered;
        if (hovered || this.timeline.isPaused()) this.marquee.pause();
        else this.marquee.resume();
    }

    void speed() {
        double speed = this.timeline.speed() == 2 ? .5 : this.timeline.speed() * 2;
        this.timeline.speed(speed);
        this.marquee.speed(speed);
    }

    void changePalette() {
        this.warm = !this.warm;
        this.palette.target(this.warm ? 0xFFFF7043 : 0xFF4285F4);
    }

    void reset() {
        this.timeline.reset();
        this.marquee.reset();
        this.resetMotion();
        this.warm = false;
        this.spinDirection = 1;
        this.marqueeDirection = 1;
        this.spinOrigin = 0;
        this.spinTime = 0;
        this.marqueeOrigin = 0;
        this.marqueeTime = 0;
    }

    private void resetMotion() {
        this.dvd.reset();
    }
}
