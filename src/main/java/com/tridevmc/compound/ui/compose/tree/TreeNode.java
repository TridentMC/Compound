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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Implementation of ITreeNode that wraps an element and stores tree metadata.
 */
public class TreeNode implements ITreeNode {
    private final IElement element;
    private ITreeNode parent;
    private final List<ITreeNode> children = new ArrayList<>();
    private final List<State<?>> boundStates = new ArrayList<>();
    private Runnable compositionFunction;
    private SlotMap slotMap;

    // Event handlers - elements can have multiple listeners for each event type
    private final List<Consumer<MouseClickEvent>> clickHandlers = new ArrayList<>();
    private final List<Runnable> mouseEnterHandlers = new ArrayList<>();
    private final List<Runnable> mouseExitHandlers = new ArrayList<>();
    private final List<Consumer<MouseScrollEvent>> scrollHandlers = new ArrayList<>();
    private final List<Consumer<KeyEvent>> keyPressHandlers = new ArrayList<>();
    private final List<Consumer<KeyEvent>> keyReleaseHandlers = new ArrayList<>();

    public TreeNode(IElement element) {
        this.element = element;
    }

    @Override
    public IElement getElement() {
        return this.element;
    }

    // Parent-child relationships
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

    // State bindings
    @Override
    public List<State<?>> getBoundStates() {
        return new ArrayList<>(this.boundStates);
    }

    @Override
    public void bindState(State<?> state) {
        if (!this.boundStates.contains(state)) {
            this.boundStates.add(state);
        }
    }

    @Override
    public void unbindState(State<?> state) {
        this.boundStates.remove(state);
    }

    @Override
    public boolean isBoundToState(State<?> state) {
        return this.boundStates.contains(state);
    }

    // Composition function
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

    // Event handlers
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
    public void addKeyPressHandler(Consumer<KeyEvent> handler) {
        this.keyPressHandlers.add(handler);
    }

    @Override
    public void addKeyReleaseHandler(Consumer<KeyEvent> handler) {
        this.keyReleaseHandlers.add(handler);
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
    public List<Consumer<KeyEvent>> getKeyPressHandlers() {
        return new ArrayList<>(this.keyPressHandlers);
    }

    @Override
    public List<Consumer<KeyEvent>> getKeyReleaseHandlers() {
        return new ArrayList<>(this.keyReleaseHandlers);
    }

    @Override
    public void clearHandlers() {
        this.clickHandlers.clear();
        this.mouseEnterHandlers.clear();
        this.mouseExitHandlers.clear();
        this.scrollHandlers.clear();
        this.keyPressHandlers.clear();
        this.keyReleaseHandlers.clear();
    }

    // Slot map
    @Override
    public SlotMap getSlotMap() {
        return this.slotMap;
    }

    @Override
    public void setSlotMap(SlotMap slotMap) {
        this.slotMap = slotMap;
    }

    // Tree queries
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
}
