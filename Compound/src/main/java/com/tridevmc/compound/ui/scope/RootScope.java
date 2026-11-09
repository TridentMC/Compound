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

package com.tridevmc.compound.ui.scope;

import com.tridevmc.compound.ui.tree.ITreeNode;
import com.tridevmc.compound.ui.tree.UITree;

final class RootScope implements NodeCompositionScope {
    private final UITree tree;
    private ITreeNode rootNode;

    RootScope(UITree tree) {
        this.tree = tree;
    }

    @Override
    public ITreeNode scopeNode() {
        return this.rootNode;
    }

    @Override
    public UITree getTree() {
        return this.tree;
    }

    @Override
    public void attach(ITreeNode node) {
        if (this.rootNode == null) {
            this.tree.setRoot(node);
            this.rootNode = node;
        } else {
            this.tree.attachNode(this.rootNode, node);
        }
    }
}
