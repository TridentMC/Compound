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

package com.tridevmc.compound.ui.element;

/** Receives button activation from mouse or keyboard input. */
@FunctionalInterface
public interface IButtonPressListener {
    /**
     * Handles activation; keyboard activation supplies the button centre.
     *
     * @param x the activation x coordinate, in screen GUI pixels
     * @param y the activation y coordinate, in screen GUI pixels
     */
    void onButtonPress(double x, double y);
}
