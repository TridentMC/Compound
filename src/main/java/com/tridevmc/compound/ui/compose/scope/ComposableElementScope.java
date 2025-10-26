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

import com.tridevmc.compound.ui.compose.element.IComposableElement;
import com.tridevmc.compound.ui.compose.slot.SlotContent;
import com.tridevmc.compound.ui.compose.slot.SlotKey;
import com.tridevmc.compound.ui.compose.slot.SlotMap;
import com.tridevmc.compound.ui.compose.tree.ITreeNode;
import com.tridevmc.compound.ui.compose.tree.UITree;

import java.util.function.Consumer;

/**
 * Scope for configuring a composable element.
 * Provides composition methods and slot management.
 */
public class ComposableElementScope<T extends IComposableElement> extends ContainerScope<T> implements IComposableElementScope<T> {
    private final SlotMap slotMap;

    public ComposableElementScope(UITree tree, ITreeNode parentNode, T element, SlotMap slotMap) {
        super(tree, parentNode, element);
        this.slotMap = slotMap;
    }

    @Override
    public void fillSlot(SlotKey key, Consumer<IContainerScope<?>> content) {
        this.slotMap.put(key, new SlotContent(content));
    }

    @Override
    public void slot(SlotKey key, Consumer<IContainerScope<?>> defaultContent) {
        SlotContent content = this.slotMap.get(key);
        if (content != null) {
            // Render user-provided content
            content.render(this);
        } else if (defaultContent != null) {
            // Render default content
            defaultContent.accept(this);
        }
    }
}
