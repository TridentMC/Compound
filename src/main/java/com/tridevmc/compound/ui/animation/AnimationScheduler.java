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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages all active animations, updating them each frame.
 * Lives in UITree and is called during layoutAndRender.
 */
public class AnimationScheduler {
    // HashSet is sufficient - animations only updated on main thread
    private final Set<AnimatedState<?>> activeAnimations = new HashSet<>();

    /**
     * Register an animation to be updated each frame.
     * Package-private - only called by AnimatedState.
     */
    void registerAnimation(AnimatedState<?> animation) {
        this.activeAnimations.add(animation);
    }

    /**
     * Unregister an animation (called when animation completes).
     * Package-private - only called by AnimatedState.
     */
    void unregisterAnimation(AnimatedState<?> animation) {
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
            return;  // Fast path - no animations
        }

        // Copy to avoid concurrent modification during iteration
        for (AnimatedState<?> animation : new ArrayList<>(this.activeAnimations)) {
            animation.updateAnimation(currentTick);
        }
    }

    /**
     * Check if there are any active animations.
     */
    public boolean hasActiveAnimations() {
        return !this.activeAnimations.isEmpty();
    }

    /**
     * Clear all animations (called on cleanup).
     */
    public void dispose() {
        this.activeAnimations.clear();
    }

    /**
     * Get count of active animations (for debugging).
     */
    public int getActiveAnimationCount() {
        return this.activeAnimations.size();
    }
}
