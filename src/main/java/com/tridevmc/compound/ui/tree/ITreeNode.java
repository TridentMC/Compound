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

package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.event.*;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.slot.SlotMap;
import com.tridevmc.compound.ui.state.State;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Wraps an element and stores tree metadata (like DOM nodes wrap elements).
 * NOTE: This is internal to the framework and not exposed to elements or composition code.
 */
public interface ITreeNode {

    // Element reference
    IElement getElement();

    // Tree reference
    UITree getTree();

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
    void addClickHandler(Function<MouseClickEvent, Boolean> handler);

    void addMouseEnterHandler(Runnable handler);

    void addMouseExitHandler(Runnable handler);

    void addFocusGainedHandler(Runnable handler);

    void addFocusLostHandler(Runnable handler);

    void addScrollHandler(Function<MouseScrollEvent, Boolean> handler);

    void addKeyPressHandler(Function<KeyInputEvent, Boolean> handler);

    void addKeyReleaseHandler(Function<KeyInputEvent, Boolean> handler);

    void addCharTypedHandler(Function<CharEvent, Boolean> handler);

    void addMouseReleaseHandler(Function<MouseReleaseEvent, Boolean> handler);

    void addMouseDragHandler(Function<MouseDragEvent, Boolean> handler);

    void addMouseMoveHandler(Function<MouseMoveEvent, Boolean> handler);

    List<Function<MouseClickEvent, Boolean>> getClickHandlers();

    List<Runnable> getMouseEnterHandlers();

    List<Runnable> getMouseExitHandlers();

    List<Runnable> getFocusGainedHandlers();

    List<Runnable> getFocusLostHandlers();

    List<Function<MouseScrollEvent, Boolean>> getScrollHandlers();

    List<Function<KeyInputEvent, Boolean>> getKeyPressHandlers();

    List<Function<KeyInputEvent, Boolean>> getKeyReleaseHandlers();

    List<Function<CharEvent, Boolean>> getCharTypedHandlers();

    List<Function<MouseReleaseEvent, Boolean>> getMouseReleaseHandlers();

    List<Function<MouseDragEvent, Boolean>> getMouseDragHandlers();

    List<Function<MouseMoveEvent, Boolean>> getMouseMoveHandlers();

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

    // Placed bounds (stored on node, not element - single source of truth)
    Bounds getBounds();

    void setBounds(Bounds bounds);

    // Layout properties (stored on node, not element)
    LayoutProperties getLayoutProperties();

    void setLayoutProperties(LayoutProperties properties);
}
