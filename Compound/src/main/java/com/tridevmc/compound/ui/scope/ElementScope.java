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

import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseDragEvent;
import com.tridevmc.compound.ui.event.MouseReleaseEvent;
import com.tridevmc.compound.ui.geometry.api.ITransform2D;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.tree.ITreeNode;

import java.util.function.Function;

/**
 * Scope for configuring a basic element (no children, no composition).
 */
class ElementScope<T extends IElement> implements IElementScope<T> {
    protected final T element;
    private final ITreeNode node;
    private LayoutProperties layoutProperties = LayoutProperties.create();

    ElementScope(T element, ITreeNode node) {
        this.element = element;
        this.node = node;
        this.layoutProperties.setBoundNode(node);
    }

    @Override
    public void onClick(Function<MouseClickEvent, Boolean> handler) {
        this.node.addClickHandler(handler);
    }

    @Override
    public void onMouseDrag(Function<MouseDragEvent, Boolean> handler) {
        this.node.addMouseDragHandler(handler);
    }

    @Override
    public void onMouseRelease(Function<MouseReleaseEvent, Boolean> handler) {
        this.node.addMouseReleaseHandler(handler);
    }

    @Override
    public void onMouseEnter(Runnable handler) {
        this.node.addMouseEnterHandler(handler);
    }

    @Override
    public void onMouseExit(Runnable handler) {
        this.node.addMouseExitHandler(handler);
    }

    void resetLayoutProperties() {
        this.node.transform(() -> ITransform2D.IDENTITY);
        this.node.beforeGeometry(() -> { });
        this.layoutProperties = LayoutProperties.create();
        this.layoutProperties.setBoundNode(this.node);
    }

    @Override
    public T getElement() {
        return this.element;
    }

    @Override
    public LayoutProperties layout() {
        return this.layoutProperties;
    }

    public LayoutProperties getLayoutProperties() {
        return this.layoutProperties;
    }
}
