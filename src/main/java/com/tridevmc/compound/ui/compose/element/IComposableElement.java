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

import com.tridevmc.compound.ui.compose.scope.ICompositionScope;

/**
 * "Smart" components that encapsulate behavior and have internal composition.
 * Composable elements have internal children created via composition, but are not
 * containers from the external perspective.
 */
public interface IComposableElement extends IElement {

    /**
     * Internal composition method.
     * Called once during tree construction to build this element's internal UI.
     *
     * @param scope the composition scope for building internal UI
     */
    void compose(ICompositionScope scope);
}
