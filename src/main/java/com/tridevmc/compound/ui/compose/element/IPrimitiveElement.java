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

package com.tridevmc.compound.ui.compose.element;

import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * Elements that cannot have children - they draw pixels.
 * Primitives are "terminators" in the tree that actually render to the screen.
 */
public interface IPrimitiveElement extends IElement {

    /**
     * Draw this primitive element (called every frame).
     * Only primitive elements draw - containers and composables do not.
     * Mouse position and partial ticks can be queried from the context if needed.
     *
     * @param context the screen context for drawing
     */
    void draw(IScreenContext context);
}
