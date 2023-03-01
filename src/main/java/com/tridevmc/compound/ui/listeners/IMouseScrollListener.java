/*
 * Copyright 2018 - 2022 TridentMC
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

package com.tridevmc.compound.ui.listeners;

import com.tridevmc.compound.ui.screen.IScreenContext;

@FunctionalInterface
public interface IMouseScrollListener {

    /**
     * Called when the scroll wheel is moved.
     *
     * @param screen   the screen context where the event took place.
     * @param x        the current x position of the mouse cursor.
     * @param y        the current y position of the mouse cursor.
     * @param distance the distance that the scroll wheel moved.
     */
    void listen(IScreenContext screen, double x, double y, double distance);

}
