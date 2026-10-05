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

package com.tridevmc.compound.ui.scope;

import com.tridevmc.compound.ui.element.IGenericElement;
import com.tridevmc.compound.ui.geometry.api.ITransform2D;
import com.tridevmc.compound.ui.geometry.api.LocalPoint;
import com.tridevmc.compound.ui.geometry.api.WorldPoint;
import com.tridevmc.compound.ui.layout.LayoutProperties;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Base scope for configuring an element.
 *
 * @param <T> the element type
 */
public interface IGenericElementScope<T extends IGenericElement> {

    /**
     * Get the element being configured.
     *
     * @return the element
     */
    T getElement();

    /** Samples once after layout each frame, without composition or measurement invalidation. */
    default void transform(Supplier<ITransform2D> supplier) {
        this.layout().getBoundNode().transform(supplier);
    }

    /** Runs after layout and before any transform suppliers are sampled. */
    default void beforeGeometry(Runnable action) {
        this.layout().getBoundNode().beforeGeometry(action);
    }

    default Optional<LocalPoint> toLocal(WorldPoint point) {
        return this.toLocal(point.x(), point.y());
    }

    /** Converts world callbacks using the same geometry as drawing and hit testing. */
    default Optional<LocalPoint> toLocal(double x, double y) {
        var geometry = this.layout().getBoundNode().getFrameGeometry();
        return geometry == null ? Optional.empty() : geometry.toLocal(x, y);
    }

    /**
     * Get the element's layout properties for configuration.
     * This provides fluent access to layout configuration methods.
     *
     * @return the element's layout properties
     */
    LayoutProperties layout();
}
