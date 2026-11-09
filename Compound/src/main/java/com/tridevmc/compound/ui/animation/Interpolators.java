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
 * Interpolation for numeric values and packed ARGB colors. Primitive methods avoid boxing
 * when used directly; constants adapt the same arithmetic to generic animated states.
 */
public final class Interpolators {

    /** Linear float interpolation. Inputs and results are boxed by the generic contract. */
    public static final Interpolator<Float> FLOAT = Interpolators::interpolateFloat;

    /** Linear integer interpolation, rounded to the nearest integer with ties toward positive infinity. */
    public static final Interpolator<Integer> INT = Interpolators::interpolateInt;

    /** Linear interpolation of each ARGB channel independently, truncating fractional channel values. */
    public static final Interpolator<Integer> COLOR = Interpolators::interpolateColor;

    /**
     * Interpolates floats without boxing. Intermediate arithmetic uses double precision to
     * avoid overflowing when finite endpoints have opposite signs.
     *
     * @param start the starting value
     * @param end the ending value
     * @param progress eased progress from zero to one
     * @return the interpolated float
     */
    public static float interpolateFloat(float start, float end, float progress) {
        return (float) (start + ((double) end - start) * progress);
    }

    /**
     * Interpolates integers without boxing or overflowing the difference between endpoints.
     *
     * @param start the starting value
     * @param end the ending value
     * @param progress eased progress from zero to one
     * @return the nearest integer, with ties rounded toward positive infinity and overflow saturated
     */
    public static int interpolateInt(int start, int end, float progress) {
        long result = Math.round(start + ((double) end - start) * progress);
        return (int) Math.clamp(result, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Interpolates packed ARGB colors without boxing. Each channel is interpolated separately;
     * the result uses straight channel arithmetic rather than gamma-corrected color blending.
     *
     * @param start the starting ARGB color
     * @param end the ending ARGB color
     * @param progress eased progress from zero to one
     * @return the interpolated ARGB color with fractional channels truncated
     */
    public static int interpolateColor(int start, int end, float progress) {
        int startA = (start >>> 24) & 0xFF;
        int startR = (start >>> 16) & 0xFF;
        int startG = (start >>> 8) & 0xFF;
        int startB = start & 0xFF;
        int endA = (end >>> 24) & 0xFF;
        int endR = (end >>> 16) & 0xFF;
        int endG = (end >>> 8) & 0xFF;
        int endB = end & 0xFF;

        int a = (int) (startA + (endA - startA) * progress);
        int r = (int) (startR + (endR - startR) * progress);
        int g = (int) (startG + (endG - startG) * progress);
        int b = (int) (startB + (endB - startB) * progress);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private Interpolators() {
    }
}
