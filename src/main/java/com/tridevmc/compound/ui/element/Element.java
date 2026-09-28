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
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.tree.ITreeNode;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Base class for custom elements with tree-managed bounds and lifecycle.
 * Extend this class and implement the appropriate element interface. An instance may be
 * mounted in one tree position at a time; keep consumer state in fields when it must survive recomposition.
 */
public abstract class Element implements IElementInternal {
    private ITreeNode node;

    /** Creates an unattached element; the tree assigns its node when mounted. */
    public Element() {
    }

    /** {@inheritDoc} */
    @Override
    public Bounds getBounds() {
        return this.node != null ? this.node.getBounds() : new Bounds(0, 0, 0, 0);
    }

    /** {@inheritDoc} */
    @Override
    public ITreeNode getNode() {
        return this.node;
    }

    /** {@inheritDoc} */
    @Override
    public void setNode(ITreeNode node) {
        this.node = node;
    }

    /** Schedules recomposition when this element is attached to a live tree. */
    protected final void invalidateComposition() {
        if (this.node != null && this.node.getTree() != null) {
            this.node.getTree().requestRecompose(this.node);
        }
    }

    /** Schedules measurement and placement without rebuilding this element's children. */
    protected final void invalidateLayout() {
        if (this.node != null && this.node.getTree() != null) {
            this.node.getTree().requestRemeasure(this.node);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onAttached() {
    }

    /** {@inheritDoc} */
    @Override
    public void onDetached() {
    }

    /** {@inheritDoc} */
    @Override
    public abstract Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren);

    /** {@inheritDoc} */
    @Override
    public abstract List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties ownProperties, List<Size> measuredChildren);
}
