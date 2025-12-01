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

package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.tree.UITree;

import java.util.List;

/**
 * Base implementation of IElement with common functionality.
 * Concrete elements should extend this class.
 */
public abstract class BaseElement implements IElement {
    private UITree tree;
    private Bounds bounds;
    private boolean visible = true;

    @Override
    public UITree getTree() {
        return this.tree;
    }

    @Override
    public void setTree(UITree tree) {
        this.tree = tree;
    }

    @Override
    public Bounds getBounds() {
        return this.bounds;
    }

    protected void setBounds(Bounds bounds) {
        this.bounds = bounds;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void onAttached() {
        // Default: no-op, subclasses can override
    }

    @Override
    public void onDetached() {
        // Default: no-op, subclasses can override
    }

    // Subclasses must implement layout
    @Override
    public abstract Size measure(Constraints constraints, List<IElement> children);

    @Override
    public abstract void place(Bounds bounds, List<IElement> children);
}
