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
import com.tridevmc.compound.ui.compose.layout.Size;
import com.tridevmc.compound.ui.compose.event.CharEvent;
import com.tridevmc.compound.ui.compose.event.KeyInputEvent;
import com.tridevmc.compound.ui.compose.event.MouseClickEvent;
import com.tridevmc.compound.ui.compose.event.MouseDragEvent;
import com.tridevmc.compound.ui.compose.event.MouseMoveEvent;
import com.tridevmc.compound.ui.compose.event.MouseReleaseEvent;
import com.tridevmc.compound.ui.compose.event.MouseScrollEvent;
import com.tridevmc.compound.ui.compose.slot.SlotMap;
import com.tridevmc.compound.ui.compose.state.State;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

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

    // New typed binding methods
    void bindCompositionState(State<?> state);

    void bindLayoutState(State<?> state);

    void unbindCompositionState(State<?> state);

    void unbindLayoutState(State<?> state);

    Set<State<?>> getCompositionStates();

    Set<State<?>> getLayoutStates();

    void dispose();

    // Composition function (stored for re-composition)
    Runnable getCompositionFunction();

    void setCompositionFunction(Runnable compositionFn);

    boolean hasCompositionFunction();

    // Event handlers (tied to this node, cleaned up when node is removed)
    // Elements can have multiple listeners for each event type
    void addClickHandler(Consumer<MouseClickEvent> handler);

    void addMouseEnterHandler(Runnable handler);

    void addMouseExitHandler(Runnable handler);

    void addScrollHandler(Function<MouseScrollEvent, Boolean> handler);

    void addKeyPressHandler(Consumer<KeyInputEvent> handler);

    void addKeyReleaseHandler(Consumer<KeyInputEvent> handler);

    void addCharTypedHandler(Consumer<CharEvent> handler);

    void addMouseReleaseHandler(Consumer<MouseReleaseEvent> handler);

    void addMouseDragHandler(Consumer<MouseDragEvent> handler);

    void addMouseMoveHandler(Consumer<MouseMoveEvent> handler);

    List<Consumer<MouseClickEvent>> getClickHandlers();

    List<Runnable> getMouseEnterHandlers();

    List<Runnable> getMouseExitHandlers();

    List<Function<MouseScrollEvent, Boolean>> getScrollHandlers();

    List<Consumer<KeyInputEvent>> getKeyPressHandlers();

    List<Consumer<KeyInputEvent>> getKeyReleaseHandlers();

    List<Consumer<CharEvent>> getCharTypedHandlers();

    List<Consumer<MouseReleaseEvent>> getMouseReleaseHandlers();

    List<Consumer<MouseDragEvent>> getMouseDragHandlers();

    List<Consumer<MouseMoveEvent>> getMouseMoveHandlers();

    void clearHandlers();

    // Slot map (for composable elements)
    SlotMap getSlotMap();

    void setSlotMap(SlotMap slotMap);

    // Tree queries
    int getDepth();

    boolean isAncestorOf(ITreeNode other);

    boolean isDescendantOf(ITreeNode other);

    // Measured size cache (for connecting measurement to placement)
    Size getMeasuredSize();

    void setMeasuredSize(Size size);
}
