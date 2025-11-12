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

package com.tridevmc.compound.ui.compose.scope;

import com.tridevmc.compound.ui.compose.element.IGenericContainer;
import com.tridevmc.compound.ui.compose.layout.LayoutProperties;

/**
 * Scope for configuring a container element (can add children).
 * Extends IGenericElementScope for element access and ICompositionScope for adding children.
 *
 * @param <T> the container type
 */
public interface IGenericContainerScope<T extends IGenericContainer>
        extends IGenericElementScope<T>, ICompositionScope {
}