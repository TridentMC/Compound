/*
 * Copyright 2018 - 2026 TridentMC
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
import com.tridevmc.compound.ui.screen.IScreenContext;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * An invisible layout element with either a requested pixel size or flexible dimensions.
 * Flexible spacers consume the available constraints; use bounded layout or an explicit weight.
 */
public class Spacer extends Element implements IPrimitiveElement {

    private int width;
    private int height;
    private boolean flexible;
    /**
     * Creates a spacer; the no-argument form is flexible and the sized form uses fixed intrinsic dimensions.
     */
    public Spacer() {
        this(0, 0, true);
    }

    /**
     * Creates a spacer; the no-argument form is flexible and the sized form uses fixed intrinsic dimensions.
     *
     * @param width the requested width in GUI pixels
     * @param height the requested height in GUI pixels
     */
    public Spacer(int width, int height) {
        this(width, height, false);
    }

    private Spacer(int width, int height, boolean flexible) {
        this.width = width;
        this.height = height;
        this.flexible = flexible;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren) {
        if (this.flexible) {
            return new Size(constraints.maxWidth(), constraints.maxHeight());
        } else {
            return new Size(
                    Math.min(this.width, constraints.maxWidth()),
                    Math.min(this.height, constraints.maxHeight())
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties ownProperties, List<Size> measuredChildren) {
        return List.of();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void draw(IScreenContext context) {
    }

    /**
     * Returns the requested width used when this spacer is not flexible.
     *
     * @return the requested width in pixels
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Changes the requested width and requests layout.
     *
     * @param width the requested width in GUI pixels
     */
    public void setWidth(int width) {
        this.width = width;
        this.invalidateLayout();
    }

    /**
     * Returns the requested height used when this spacer is not flexible.
     *
     * @return the requested height in pixels
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Changes the requested height and requests layout.
     *
     * @param height the requested height in GUI pixels
     */
    public void setHeight(int height) {
        this.height = height;
        this.invalidateLayout();
    }

    /**
     * Returns whether this spacer uses the available dimensions instead of its requested size.
     *
     * @return true when enabled
     */
    public boolean isFlexible() {
        return this.flexible;
    }

    /**
     * Switches between available dimensions and requested size, then requests layout.
     *
     * @param flexible whether to use the available dimensions
     */
    public void setFlexible(boolean flexible) {
        this.flexible = flexible;
        this.invalidateLayout();
    }
}
