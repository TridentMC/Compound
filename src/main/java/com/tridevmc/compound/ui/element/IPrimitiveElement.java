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

import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * Leaf element that draws content without creating child nodes.
 * Use {@link PrimitiveElement} when implementing a custom primitive.
 */
public interface IPrimitiveElement extends IElement {

    /**
     * Draws the placed primitive during visible frames. The tree applies clipping and layer order.
     * Read live values from suppliers here when changes do not require composition or layout.
     *
     * @param context the screen context for drawing
     */
    void draw(IScreenContext context);
}
