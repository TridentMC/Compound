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

/** Receives transitions into and out of a button's hovered state. */
@FunctionalInterface
public interface IButtonHoverListener {
    /**
     * Handles a hover transition.
     *
     * @param x the button centre x coordinate, in screen GUI pixels
     * @param y the button centre y coordinate, in screen GUI pixels
     * @param hovered true when the pointer enters; false when it leaves
     */
    void onButtonHover(double x, double y, boolean hovered);
}
