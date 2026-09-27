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

import com.tridevmc.compound.ui.animation.AnimatedState;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.event.*;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.slot.SlotMap;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateObserver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Implementation of ITreeNode that wraps an element and stores tree metadata.
 */
public class TreeNode implements ITreeNode {
    private final IElement element;
    private final List<ITreeNode> children = new ArrayList<>();
    private final List<State<?>> boundStates = new ArrayList<>();
    private final Set<State<?>> compositionStates = new HashSet<>();
    private final Set<State<?>> layoutStates = new HashSet<>();
    private final List<Function<MouseClickEvent, Boolean>> clickHandlers = new ArrayList<>();
    private final List<Runnable> mouseEnterHandlers = new ArrayList<>();
    private final List<Runnable> mouseExitHandlers = new ArrayList<>();
    private final List<Runnable> focusGainedHandlers = new ArrayList<>();
    private final List<Runnable> focusLostHandlers = new ArrayList<>();
    private final List<Function<MouseScrollEvent, Boolean>> scrollHandlers = new ArrayList<>();
    private final List<Function<KeyInputEvent, Boolean>> keyPressHandlers = new ArrayList<>();
    private final List<Function<KeyInputEvent, Boolean>> keyReleaseHandlers = new ArrayList<>();
    private final List<Function<CharEvent, Boolean>> charTypedHandlers = new ArrayList<>();
    private final List<Function<MouseReleaseEvent, Boolean>> mouseReleaseHandlers = new ArrayList<>();
    private final List<Function<MouseDragEvent, Boolean>> mouseDragHandlers = new ArrayList<>();
    private final List<Function<MouseMoveEvent, Boolean>> mouseMoveHandlers = new ArrayList<>();
    private UITree tree;
    private final List<Runnable> handlerResets = new ArrayList<>();
    private final List<Runnable> disposalActions = new ArrayList<>();
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
    private ITreeNode parent;
    private Runnable compositionFunction;
    private SlotMap slotMap;
    private Size measuredSize;
    private Bounds bounds;
    private LayoutProperties layoutProperties = LayoutProperties.create();
    private final List<AnimatedState<?>> registeredAnimations = new ArrayList<>();

    public TreeNode(IElement element, UITree tree) {
        this.element = element;
        this.tree = tree;
        this.preserveHandlers();
    }

    @Override
    public IElement getElement() {
        return this.element;
    }

    @Override
    public UITree getTree() {
        return this.tree;
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
    public void registerAnimation(AnimatedState<?> animation) {
        if (animation != null && !this.registeredAnimations.contains(animation)) {
            this.registeredAnimations.add(animation);
        }
    }

    @Override
    public List<AnimatedState<?>> getRegisteredAnimations() {
        return new ArrayList<>(this.registeredAnimations);
    }

    /** Registers cleanup for resources owned by this node. */
    public void onDispose(Runnable cleanup) {
        this.disposalActions.add(cleanup);
    }

    @Override
    public void dispose() {
        this.tree = null;
        for (var cleanup : this.disposalActions) {
            cleanup.run();
        }
        this.disposalActions.clear();
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

        // Dispose all registered animations
        for (AnimatedState<?> animation : this.registeredAnimations) {
            animation.dispose();
        }
        this.registeredAnimations.clear();
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
    public void addClickHandler(Function<MouseClickEvent, Boolean> handler) {
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
    public void addFocusGainedHandler(Runnable handler) {
        this.focusGainedHandlers.add(handler);
    }

    @Override
    public void addFocusLostHandler(Runnable handler) {
        this.focusLostHandlers.add(handler);
    }

    @Override
    public void addScrollHandler(Function<MouseScrollEvent, Boolean> handler) {
        this.scrollHandlers.add(handler);
    }

    @Override
    public void addKeyPressHandler(Function<KeyInputEvent, Boolean> handler) {
        this.keyPressHandlers.add(handler);
    }

    @Override
    public void addKeyReleaseHandler(Function<KeyInputEvent, Boolean> handler) {
        this.keyReleaseHandlers.add(handler);
    }

    @Override
    public void addCharTypedHandler(Function<CharEvent, Boolean> handler) {
        this.charTypedHandlers.add(handler);
    }

    @Override
    public void addMouseReleaseHandler(Function<MouseReleaseEvent, Boolean> handler) {
        this.mouseReleaseHandlers.add(handler);
    }

    @Override
    public void addMouseDragHandler(Function<MouseDragEvent, Boolean> handler) {
        this.mouseDragHandlers.add(handler);
    }

    @Override
    public void addMouseMoveHandler(Function<MouseMoveEvent, Boolean> handler) {
        this.mouseMoveHandlers.add(handler);
    }

    @Override
    public List<Function<MouseClickEvent, Boolean>> getClickHandlers() {
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
    public List<Runnable> getFocusGainedHandlers() {
        return new ArrayList<>(this.focusGainedHandlers);
    }

    @Override
    public List<Runnable> getFocusLostHandlers() {
        return new ArrayList<>(this.focusLostHandlers);
    }

    @Override
    public List<Function<MouseScrollEvent, Boolean>> getScrollHandlers() {
        return new ArrayList<>(this.scrollHandlers);
    }

    @Override
    public List<Function<KeyInputEvent, Boolean>> getKeyPressHandlers() {
        return new ArrayList<>(this.keyPressHandlers);
    }

    @Override
    public List<Function<KeyInputEvent, Boolean>> getKeyReleaseHandlers() {
        return new ArrayList<>(this.keyReleaseHandlers);
    }

    @Override
    public List<Function<CharEvent, Boolean>> getCharTypedHandlers() {
        return new ArrayList<>(this.charTypedHandlers);
    }

    @Override
    public List<Function<MouseReleaseEvent, Boolean>> getMouseReleaseHandlers() {
        return new ArrayList<>(this.mouseReleaseHandlers);
    }

    @Override
    public List<Function<MouseDragEvent, Boolean>> getMouseDragHandlers() {
        return new ArrayList<>(this.mouseDragHandlers);
    }

    @Override
    public List<Function<MouseMoveEvent, Boolean>> getMouseMoveHandlers() {
        return new ArrayList<>(this.mouseMoveHandlers);
    }

    @Override
    public void preserveHandlers() {
        this.handlerResets.clear();
        this.preserve(this.clickHandlers);
        this.preserve(this.mouseEnterHandlers);
        this.preserve(this.mouseExitHandlers);
        this.preserve(this.focusGainedHandlers);
        this.preserve(this.focusLostHandlers);
        this.preserve(this.scrollHandlers);
        this.preserve(this.keyPressHandlers);
        this.preserve(this.keyReleaseHandlers);
        this.preserve(this.charTypedHandlers);
        this.preserve(this.mouseReleaseHandlers);
        this.preserve(this.mouseDragHandlers);
        this.preserve(this.mouseMoveHandlers);
    }

    private <T> void preserve(List<T> handlers) {
        var count = handlers.size();
        this.handlerResets.add(() -> handlers.subList(count, handlers.size()).clear());
    }

    @Override
    public void clearHandlers() {
        for (var reset : this.handlerResets) {
            reset.run();
        }
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

    @Override
    public Bounds getBounds() {
        return this.bounds;
    }

    @Override
    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
    }

    @Override
    public LayoutProperties getLayoutProperties() {
        return this.layoutProperties;
    }

    @Override
    public void setLayoutProperties(LayoutProperties properties) {
        this.layoutProperties = properties;
    }

    // Debug: Tracks what the parent allocated before alignment/centering
    private Bounds allocatedBounds;

    @Override
    public Bounds getAllocatedBounds() {
        return this.allocatedBounds;
    }

    @Override
    public void setAllocatedBounds(Bounds bounds) {
        this.allocatedBounds = bounds;
    }
}

