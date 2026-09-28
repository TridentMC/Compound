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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Advances animations using the owning screen's tick counter on the client thread.
 * Repeated frame updates within a tick do not advance an animation twice.
 */
public class AnimationScheduler {
    private final Set<TickAnimation<?>> activeAnimations = new HashSet<>();

    /** Creates an empty scheduler. */
    public AnimationScheduler() {
    }

    void registerAnimation(TickAnimation<?> animation) {
        this.activeAnimations.add(animation);
    }

    void unregisterAnimation(TickAnimation<?> animation) {
        this.activeAnimations.remove(animation);
    }

    /**
     * Update all active animations.
     * Called by UITree each frame with current tick count from screen context.
     *
     * @param currentTick the current game tick (from screen.getTicks())
     */
    public void updateAnimations(long currentTick) {
        if (this.activeAnimations.isEmpty()) {
            return;
        }

        for (TickAnimation<?> animation : new ArrayList<>(this.activeAnimations)) {
            if (this.activeAnimations.contains(animation)) {
                animation.updateAnimation(currentTick);
            }
        }
    }

    /**
     * Checks whether any animation is scheduled.
     *
     * @return whether at least one animation is active
     */
    public boolean hasActiveAnimations() {
        return !this.activeAnimations.isEmpty();
    }

    /**
     * Disposes all scheduled animations and clears their observers.
     */
    public void dispose() {
        for (var animation : new ArrayList<>(this.activeAnimations)) {
            animation.dispose();
        }
    }

    /**
     * Counts scheduled animations.
     *
     * @return the number of active animations
     */
    public int getActiveAnimationCount() {
        return this.activeAnimations.size();
    }
}
