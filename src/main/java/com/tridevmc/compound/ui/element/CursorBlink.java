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

package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.animation.AnimatedState;
import com.tridevmc.compound.ui.animation.Easing;
import com.tridevmc.compound.ui.scope.ICompositionScope;

import java.util.Objects;

final class CursorBlink {
    private long intervalMillis = 300;
    private Easing easing = Easing.STEP;
    private AnimatedState<Float> opacity;

    void compose(ICompositionScope scope) {
        if (this.opacity == null) {
            this.opacity = scope.animateFloatLooping(1F, 0F, this.intervalMillis, this.easing);
        }
        scope.retainAnimation(this.opacity);
    }

    void configure(long intervalMillis, Easing easing) {
        if (intervalMillis <= 0) throw new IllegalArgumentException("Cursor interval must be positive");
        Objects.requireNonNull(easing, "easing");
        this.intervalMillis = intervalMillis;
        this.easing = easing;
        if (this.opacity != null) {
            this.opacity.setTiming(intervalMillis, easing);
            this.reset();
        }
    }

    void reset() {
        if (this.opacity != null) this.opacity.setImmediate(1F);
    }

    int color() {
        float alpha = this.opacity == null ? 1F : this.opacity.get();
        return Math.round(Math.clamp(alpha, 0F, 1F) * 255) << 24 | 0xFFFFFF;
    }

    void detach() {
        if (this.opacity != null) this.opacity.dispose();
        this.opacity = null;
    }
}
