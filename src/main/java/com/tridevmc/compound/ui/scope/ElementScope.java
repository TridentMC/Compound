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
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.tree.ITreeNode;

/**
 * Scope for configuring a basic element (no children, no composition).
 */
public class ElementScope<T extends IElement> implements IElementScope<T> {
    protected final T element;
    private final ITreeNode node;
    private LayoutProperties layoutProperties = LayoutProperties.create();

    public ElementScope(T element) {
        this(element, null);
    }

    public ElementScope(T element, ITreeNode node) {
        this.element = element;
        this.node = node;
        this.layoutProperties.setBoundNode(node);
    }

    void resetLayoutProperties() {
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
