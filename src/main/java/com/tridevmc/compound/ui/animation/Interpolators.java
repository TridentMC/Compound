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
 * Common interpolators for basic types.
 */
public class Interpolators {

    public static final Interpolator<Float> FLOAT = (start, end, progress) -> {
        return start + (end - start) * progress;
    };

    public static final Interpolator<Integer> INT = (start, end, progress) -> {
        return Math.round(start + (end - start) * progress);
    };

    /**
     * Interpolates ARGB color values, handling each channel independently.
     */
    public static final Interpolator<Integer> COLOR = (start, end, progress) -> {
        int startA = (start >> 24) & 0xFF;
        int startR = (start >> 16) & 0xFF;
        int startG = (start >> 8) & 0xFF;
        int startB = start & 0xFF;

        int endA = (end >> 24) & 0xFF;
        int endR = (end >> 16) & 0xFF;
        int endG = (end >> 8) & 0xFF;
        int endB = end & 0xFF;

        int a = (int) (startA + (endA - startA) * progress);
        int r = (int) (startR + (endR - startR) * progress);
        int g = (int) (startG + (endG - startG) * progress);
        int b = (int) (startB + (endB - startB) * progress);

        return (a << 24) | (r << 16) | (g << 8) | b;
    };

    private Interpolators() {
    }
}
