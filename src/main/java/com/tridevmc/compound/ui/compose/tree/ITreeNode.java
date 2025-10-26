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

package com.tridevmc.compound.ui.compose.tree;

import com.tridevmc.compound.ui.compose.element.IElement;
import com.tridevmc.compound.ui.compose.event.KeyEvent;
import com.tridevmc.compound.ui.compose.event.MouseClickEvent;
import com.tridevmc.compound.ui.compose.event.MouseScrollEvent;
import com.tridevmc.compound.ui.compose.slot.SlotMap;
import com.tridevmc.compound.ui.compose.state.State;

import java.util.List;
import java.util.function.Consumer;

/**
 * Wraps an element and stores tree metadata (like DOM nodes wrap elements).
 * NOTE: This is internal to the framework and not exposed to elements or composition code.
 */
public interface ITreeNode {

    // Element reference
    IElement getElement();

    // Parent-child relationships (stored in the node, not the element)
    ITreeNode getParent();

    List<ITreeNode> getChildren();

    void addChild(ITreeNode child);

    void removeChild(ITreeNode child);

    void clearChildren();

    boolean hasChildren();

    int getChildCount();

    // State bindings (for re-composition)
    List<State<?>> getBoundStates();

    void bindState(State<?> state);

    void unbindState(State<?> state);

    boolean isBoundToState(State<?> state);

    // Composition function (stored for re-composition)
    Runnable getCompositionFunction();

    void setCompositionFunction(Runnable compositionFn);

    boolean hasCompositionFunction();

    // Event handlers (tied to this node, cleaned up when node is removed)
    // Elements can have multiple listeners for each event type
    void addClickHandler(Consumer<MouseClickEvent> handler);

    void addMouseEnterHandler(Runnable handler);

    void addMouseExitHandler(Runnable handler);

    void addScrollHandler(Consumer<MouseScrollEvent> handler);

    void addKeyPressHandler(Consumer<KeyEvent> handler);

    void addKeyReleaseHandler(Consumer<KeyEvent> handler);

    List<Consumer<MouseClickEvent>> getClickHandlers();

    List<Runnable> getMouseEnterHandlers();

    List<Runnable> getMouseExitHandlers();

    List<Consumer<MouseScrollEvent>> getScrollHandlers();

    List<Consumer<KeyEvent>> getKeyPressHandlers();

    List<Consumer<KeyEvent>> getKeyReleaseHandlers();

    void clearHandlers();

    // Slot map (for composable elements)
    SlotMap getSlotMap();

    void setSlotMap(SlotMap slotMap);

    // Tree queries
    int getDepth();

    boolean isAncestorOf(ITreeNode other);

    boolean isDescendantOf(ITreeNode other);
}
