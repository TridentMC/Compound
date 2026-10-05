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

import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseDragEvent;
import com.tridevmc.compound.ui.event.MouseReleaseEvent;

import java.util.function.Function;

/**
 * Default scope for configuring an element.
 * Most elements use this interface.
 */
public interface IElementScope<T extends IElement> extends IGenericElementScope<T> {
    default void onClick(Function<MouseClickEvent, Boolean> handler) {
        this.layout().getBoundNode().addClickHandler(handler);
    }

    default void onMouseDrag(Function<MouseDragEvent, Boolean> handler) {
        this.layout().getBoundNode().addMouseDragHandler(handler);
    }

    default void onMouseRelease(Function<MouseReleaseEvent, Boolean> handler) {
        this.layout().getBoundNode().addMouseReleaseHandler(handler);
    }

    default void onMouseEnter(Runnable handler) {
        this.layout().getBoundNode().addMouseEnterHandler(handler);
    }

    default void onMouseExit(Runnable handler) {
        this.layout().getBoundNode().addMouseExitHandler(handler);
    }
}
