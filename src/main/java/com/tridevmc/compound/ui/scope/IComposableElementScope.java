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

package com.tridevmc.compound.ui.scope;

import com.tridevmc.compound.ui.element.IComposableElement;
import com.tridevmc.compound.ui.slot.SlotKey;

import java.util.function.Consumer;

/**
 * Scope for configuring a composable element.
 * Extends IElementScope and adds slot filling capabilities.
 * Composable elements are not containers from the outside, so this doesn't extend IContainerScope.
 */
public interface IComposableElementScope<T extends IComposableElement> extends IElementScope<T> {

    /**
     * Fill a slot with custom content.
     * This stores the content to be rendered by the composable element during its internal composition.
     *
     * @param key     the slot key
     * @param content the content to render in the slot
     */
    void fillSlot(SlotKey key, Consumer<ICompositionScope> content);
}
