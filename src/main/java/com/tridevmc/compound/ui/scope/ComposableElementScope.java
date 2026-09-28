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
import com.tridevmc.compound.ui.slot.SlotContent;
import com.tridevmc.compound.ui.slot.SlotKey;
import com.tridevmc.compound.ui.slot.SlotMap;
import com.tridevmc.compound.ui.tree.ITreeNode;
import com.tridevmc.compound.ui.tree.UITree;
import java.util.function.Consumer;

final class ComposableElementScope<T extends IComposableElement> extends CompositionScope<T>
        implements IComposableElementScope<T> {
    private final SlotMap slotMap;

    ComposableElementScope(UITree tree, ITreeNode node, T element, SlotMap slots) {
        super(tree, node, element);
        this.slotMap = slots;
    }

    @Override
    public void fillSlot(SlotKey key, Consumer<ICompositionScope> content) {
        this.slotMap.put(key, new SlotContent(content));
        if (this.scopeNode().hasCompositionFunction()) this.getTree().requestRecompose(this.scopeNode());
    }

    @Override
    public SlotMap getSlotMap() {
        return this.slotMap;
    }
}
