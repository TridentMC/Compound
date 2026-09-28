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

import com.tridevmc.compound.ui.scope.ICompositionScope;

/**
 * Component that builds its own child tree and exposes customization through slots.
 * Consumers configure it through a composable-element scope; subclasses build children
 * in {@link #compose}.
 */
public interface IComposableElement extends IElement {

    /**
     * Builds this element's children on attachment and whenever its composition is invalidated.
     * Previous children are detached before replay. Keep persistent consumer state in fields;
     * callbacks and composition-local resources registered here are replaced on replay.
     *
     * @param scope the composition scope for building internal UI
     */
    void compose(ICompositionScope scope);
}
