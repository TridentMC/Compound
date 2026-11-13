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

package com.tridevmc.compound.ui.layout;

import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateObserver;
import com.tridevmc.compound.ui.tree.ITreeNode;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Scope for configuring deferred layout properties with reactive state binding.
 *
 * <p>Provides access to bind states and mutate layout properties. When any bound state
 * changes, the callback is re-executed and layout is triggered without recomposition.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * var width = scope.animateInt(0, 300);
 * var height = scope.animateInt(0, 200);
 *
 * box.layout()
 *     .fillMaxHeight()
 *     .deferred(deferred -&gt; {
 *         deferred.bind(width);
 *         deferred.bind(height);
 *         deferred.layout().fixedWidth(width.get());
 *         deferred.layout().fixedHeight(height.get());
 *     });
 * </pre>
 */
public class DeferredScope {
    private final LayoutProperties properties;
    private final ITreeNode boundNode;
    private final Consumer<DeferredScope> callback;
    private final Set<State<?>> boundStates = new HashSet<>();
    private final StateObserver observer;
    private boolean disposed;

    DeferredScope(LayoutProperties properties, ITreeNode node, Consumer<DeferredScope> callback) {
        this.properties = properties;
        this.boundNode = node;
        this.callback = callback;
        this.observer = ignored -> {
            if (this.disposed || node == null) return;
            var tree = node.getTree();
            if (tree != null) {
                this.callback.accept(this);
                tree.requestRemeasure(node);
            }
        };
        // Capture ownership now, even when later callbacks discover more dependencies.
        if (node != null) node.onDispose(this::dispose);
    }

    /**
     * Bind a state for layout-only updates.
     * When this state changes, the deferred callback will re-execute and layout will be triggered without recomposition.
     *
     * @param state the state to bind
     */
    public void bind(State<?> state) {
        if (!this.disposed && this.boundNode != null && this.boundStates.add(state)) {
            state.addObserver(this.observer);
        }
    }

    private void dispose() {
        this.disposed = true;
        for (var state : this.boundStates) state.removeObserver(this.observer);
        this.boundStates.clear();
    }

    /**
     * Get the layout properties to mutate.
     * Use this to set layout properties that should update when bound states change.
     *
     * @return the layout properties instance
     */
    public LayoutProperties layout() {
        return properties;
    }
}
