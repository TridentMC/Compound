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

package com.tridevmc.compound.ui.slot;

import com.tridevmc.compound.ui.scope.ICompositionScope;

import java.util.function.Consumer;

/**
 * Wrapper for user-provided slot content.
 */
public record SlotContent(Consumer<ICompositionScope> content) {

    /**
     * Execute the content in the given scope.
     *
     * @param scope the scope to execute in
     */
    public void render(ICompositionScope scope) {
        this.content.accept(scope);
    }
}
