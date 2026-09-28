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

import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.tree.ITreeNode;
import com.tridevmc.compound.ui.tree.UITree;

abstract class CompositionScope<T extends IElement> extends ElementScope<T> implements NodeCompositionScope {
    private final UITree tree;
    private final ITreeNode node;

    CompositionScope(UITree tree, ITreeNode node, T element) {
        super(element, node);
        this.tree = tree;
        this.node = node;
    }

    @Override
    public final ITreeNode scopeNode() {
        return this.node;
    }

    @Override
    public final UITree getTree() {
        return this.tree;
    }

    @Override
    public final void attach(ITreeNode child) {
        this.tree.attachNode(this.node, child);
    }
}
