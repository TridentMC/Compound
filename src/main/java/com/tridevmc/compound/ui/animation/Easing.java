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

/**
 * Easing functions for animation curves.
 * Takes linear progress (0.0 to 1.0) and returns eased progress.
 */
@FunctionalInterface
public interface Easing {
    /**
     * Apply easing to linear progress.
     *
     * @param t linear progress from 0.0 to 1.0
     * @return eased progress from 0.0 to 1.0
     */
    float apply(float t);

    // Common easing functions
    Easing LINEAR = t -> t;

    Easing EASE_IN = t -> t * t;

    Easing EASE_OUT = t -> t * (2 - t);

    Easing EASE_IN_OUT = t -> t < 0.5f
        ? 2 * t * t
        : -1 + (4 - 2 * t) * t;

    Easing EASE_IN_CUBIC = t -> t * t * t;

    Easing EASE_OUT_CUBIC = t -> {
        float f = t - 1;
        return f * f * f + 1;
    };

    Easing EASE_IN_OUT_CUBIC = t -> t < 0.5f
        ? 4 * t * t * t
        : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;
}
