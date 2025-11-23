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
import com.tridevmc.compound.ui.compose.state.StateObserver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Implementation of ITreeNode that wraps an element and stores tree metadata.
 */
public class TreeNode implements ITreeNode {
    private final IElement element;
    private UITree tree;
    private ITreeNode parent;
    private final List<ITreeNode> children = new ArrayList<>();
    private final List<State<?>> boundStates = new ArrayList<>();
    private final Set<State<?>> compositionStates = new HashSet<>();
    private final Set<State<?>> layoutStates = new HashSet<>();
    private Runnable compositionFunction;
    private SlotMap slotMap;
    private Size measuredSize;

    // Two dedicated observers for different state types
    private final StateObserver compositionObserver = state -> {
        if (this.tree != null) {
            this.tree.requestRecompose(this);
        }
    };

    private final StateObserver layoutObserver = state -> {
        if (this.tree != null) {
            this.tree.requestRemeasure(this);
        }
    };

    private final List<Consumer<MouseClickEvent>> clickHandlers = new ArrayList<>();
    private final List<Runnable> mouseEnterHandlers = new ArrayList<>();
    private final List<Runnable> mouseExitHandlers = new ArrayList<>();
    private final List<Consumer<MouseScrollEvent>> scrollHandlers = new ArrayList<>();
    private final List<Consumer<KeyInputEvent>> keyPressHandlers = new ArrayList<>();
    private final List<Consumer<KeyInputEvent>> keyReleaseHandlers = new ArrayList<>();
    private final List<Consumer<CharEvent>> charTypedHandlers = new ArrayList<>();
    private final List<Consumer<MouseReleaseEvent>> mouseReleaseHandlers = new ArrayList<>();
    private final List<Consumer<MouseDragEvent>> mouseDragHandlers = new ArrayList<>();
    private final List<Consumer<MouseMoveEvent>> mouseMoveHandlers = new ArrayList<>();

    public TreeNode(IElement element, UITree tree) {
        this.element = element;
        this.tree = tree;
    }

    @Override
    public IElement getElement() {
        return this.element;
    }

    @Override
    public ITreeNode getParent() {
        return this.parent;
    }

    void setParent(ITreeNode parent) {
        this.parent = parent;
    }

    @Override
    public List<ITreeNode> getChildren() {
        return new ArrayList<>(this.children);
    }

    @Override
    public void addChild(ITreeNode child) {
        this.children.add(child);
        if (child instanceof TreeNode treeNode) {
            treeNode.setParent(this);
        }
    }

    @Override
    public void removeChild(ITreeNode child) {
        this.children.remove(child);
        if (child instanceof TreeNode treeNode) {
            treeNode.setParent(null);
        }
    }

    @Override
    public void clearChildren() {
        for (ITreeNode child : this.children) {
            if (child instanceof TreeNode treeNode) {
                treeNode.setParent(null);
            }
        }
        this.children.clear();
    }

    @Override
    public boolean hasChildren() {
        return !this.children.isEmpty();
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public List<State<?>> getBoundStates() {
        // Merge all state types for backward compatibility
        Set<State<?>> all = new HashSet<>();
        all.addAll(this.boundStates);
        all.addAll(this.compositionStates);
        all.addAll(this.layoutStates);
        return new ArrayList<>(all);
    }

    @Override
    @Deprecated
    public void bindState(State<?> state) {
        // Default to composition binding for backward compatibility
        this.bindCompositionState(state);
        if (!this.boundStates.contains(state)) {
            this.boundStates.add(state);
        }
    }

    @Override
    public void unbindState(State<?> state) {
        this.boundStates.remove(state);
        // Try both types
        this.unbindCompositionState(state);
        this.unbindLayoutState(state);
    }

    @Override
    public boolean isBoundToState(State<?> state) {
        return this.boundStates.contains(state) ||
               this.compositionStates.contains(state) ||
               this.layoutStates.contains(state);
    }

    @Override
    public void bindCompositionState(State<?> state) {
        if (this.compositionStates.add(state)) {  // Set.add returns true if added
            state.addObserver(this.compositionObserver);
        }
    }

    @Override
    public void bindLayoutState(State<?> state) {
        if (this.layoutStates.add(state)) {
            state.addObserver(this.layoutObserver);
        }
    }

    @Override
    public void unbindCompositionState(State<?> state) {
        if (this.compositionStates.remove(state)) {
            state.removeObserver(this.compositionObserver);
        }
    }

    @Override
    public void unbindLayoutState(State<?> state) {
        if (this.layoutStates.remove(state)) {
            state.removeObserver(this.layoutObserver);
        }
    }

    @Override
    public Set<State<?>> getCompositionStates() {
        return new HashSet<>(this.compositionStates);
    }

    @Override
    public Set<State<?>> getLayoutStates() {
        return new HashSet<>(this.layoutStates);
    }

    @Override
    public void dispose() {
        // Remove all state observers
        for (State<?> state : this.compositionStates) {
            state.removeObserver(this.compositionObserver);
        }
        for (State<?> state : this.layoutStates) {
            state.removeObserver(this.layoutObserver);
        }
        this.compositionStates.clear();
        this.layoutStates.clear();
        this.boundStates.clear();
    }

    @Override
    public Runnable getCompositionFunction() {
        return this.compositionFunction;
    }

    @Override
    public void setCompositionFunction(Runnable compositionFn) {
        this.compositionFunction = compositionFn;
    }

    @Override
    public boolean hasCompositionFunction() {
        return this.compositionFunction != null;
    }

    @Override
    public void addClickHandler(Consumer<MouseClickEvent> handler) {
        this.clickHandlers.add(handler);
    }

    @Override
    public void addMouseEnterHandler(Runnable handler) {
        this.mouseEnterHandlers.add(handler);
    }

    @Override
    public void addMouseExitHandler(Runnable handler) {
        this.mouseExitHandlers.add(handler);
    }

    @Override
    public void addScrollHandler(Consumer<MouseScrollEvent> handler) {
        this.scrollHandlers.add(handler);
    }

    @Override
    public void addKeyPressHandler(Consumer<KeyInputEvent> handler) {
        this.keyPressHandlers.add(handler);
    }

    @Override
    public void addKeyReleaseHandler(Consumer<KeyInputEvent> handler) {
        this.keyReleaseHandlers.add(handler);
    }

    @Override
    public void addCharTypedHandler(Consumer<CharEvent> handler) {
        this.charTypedHandlers.add(handler);
    }

    @Override
    public void addMouseReleaseHandler(Consumer<MouseReleaseEvent> handler) {
        this.mouseReleaseHandlers.add(handler);
    }

    @Override
    public void addMouseDragHandler(Consumer<MouseDragEvent> handler) {
        this.mouseDragHandlers.add(handler);
    }

    @Override
    public void addMouseMoveHandler(Consumer<MouseMoveEvent> handler) {
        this.mouseMoveHandlers.add(handler);
    }

    @Override
    public List<Consumer<MouseClickEvent>> getClickHandlers() {
        return new ArrayList<>(this.clickHandlers);
    }

    @Override
    public List<Runnable> getMouseEnterHandlers() {
        return new ArrayList<>(this.mouseEnterHandlers);
    }

    @Override
    public List<Runnable> getMouseExitHandlers() {
        return new ArrayList<>(this.mouseExitHandlers);
    }

    @Override
    public List<Consumer<MouseScrollEvent>> getScrollHandlers() {
        return new ArrayList<>(this.scrollHandlers);
    }

    @Override
    public List<Consumer<KeyInputEvent>> getKeyPressHandlers() {
        return new ArrayList<>(this.keyPressHandlers);
    }

    @Override
    public List<Consumer<KeyInputEvent>> getKeyReleaseHandlers() {
        return new ArrayList<>(this.keyReleaseHandlers);
    }

    @Override
    public List<Consumer<CharEvent>> getCharTypedHandlers() {
        return new ArrayList<>(this.charTypedHandlers);
    }

    @Override
    public List<Consumer<MouseReleaseEvent>> getMouseReleaseHandlers() {
        return new ArrayList<>(this.mouseReleaseHandlers);
    }

    @Override
    public List<Consumer<MouseDragEvent>> getMouseDragHandlers() {
        return new ArrayList<>(this.mouseDragHandlers);
    }

    @Override
    public List<Consumer<MouseMoveEvent>> getMouseMoveHandlers() {
        return new ArrayList<>(this.mouseMoveHandlers);
    }

    @Override
    public void clearHandlers() {
        this.clickHandlers.clear();
        this.mouseEnterHandlers.clear();
        this.mouseExitHandlers.clear();
        this.scrollHandlers.clear();
        this.keyPressHandlers.clear();
        this.keyReleaseHandlers.clear();
        this.charTypedHandlers.clear();
        this.mouseReleaseHandlers.clear();
        this.mouseDragHandlers.clear();
        this.mouseMoveHandlers.clear();
    }

    @Override
    public SlotMap getSlotMap() {
        return this.slotMap;
    }

    @Override
    public void setSlotMap(SlotMap slotMap) {
        this.slotMap = slotMap;
    }

    @Override
    public int getDepth() {
        int depth = 0;
        ITreeNode current = this.parent;
        while (current != null) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    @Override
    public boolean isAncestorOf(ITreeNode other) {
        if (other == null) {
            return false;
        }
        ITreeNode current = other.getParent();
        while (current != null) {
            if (current == this) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    @Override
    public boolean isDescendantOf(ITreeNode other) {
        return other != null && other.isAncestorOf(this);
    }

    @Override
    public Size getMeasuredSize() {
        return this.measuredSize;
    }

    @Override
    public void setMeasuredSize(Size size) {
        this.measuredSize = size;
    }
}
