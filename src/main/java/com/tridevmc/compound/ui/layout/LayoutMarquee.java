/*
 * Copyright 2018 - 2021 TridentMC
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

package com.tridevmc.compound.ui.layout;

import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * A layout that makes an element travel across the screen horizontally and then wraps it around the beginning after completion.
 * <p>
 * There's literally no reason anyone should ever use this, it's just a useful test of the layout system.
 */
public class LayoutMarquee implements ILayout {
    private long lastTick = -1;
    private float lastHorizontalPosition, currentHorizontalPosition;
    private float lastVerticalPosition, currentVerticalPosition;
    private float horizontalSpeed = 1F, verticalSpeed = 0F;

    public LayoutMarquee() {
    }

    public LayoutMarquee(float horizontalSpeed, float verticalSpeed) {
        this.horizontalSpeed = horizontalSpeed;
        this.verticalSpeed = verticalSpeed;
    }

    @Deprecated // Use the version with both horizontal and vertical speeds
    public LayoutMarquee(float horizontalSpeed) {
        this.horizontalSpeed = horizontalSpeed;
    }

    @Override
    public Rect2F getTransformedRect(IScreenContext screen, IElement element, Rect2F rect) {
        if (screen.getTicks() != this.lastTick) {
            this.lastTick = screen.getTicks();
            this.updatePosition();
        }

        if (this.currentHorizontalPosition > screen.getWidth()) {
            this.currentHorizontalPosition = -rect.getWidth();
            this.lastHorizontalPosition = this.currentHorizontalPosition - this.horizontalSpeed;
        }

        if (this.currentVerticalPosition > screen.getHeight()) {
            this.currentVerticalPosition = -rect.getHeight();
            this.lastVerticalPosition = this.currentVerticalPosition - this.verticalSpeed;
        }

        float x = this.lastHorizontalPosition + ((this.currentHorizontalPosition - this.lastHorizontalPosition) * screen.getPartialTicks());
        float y = this.lastVerticalPosition + ((this.currentVerticalPosition - this.lastVerticalPosition) * screen.getPartialTicks());
        return rect.setPosition(x, y);
    }

    private void updatePosition() {
        this.lastHorizontalPosition = this.currentHorizontalPosition;
        this.currentHorizontalPosition = this.currentHorizontalPosition + this.horizontalSpeed;
        this.lastVerticalPosition = this.currentVerticalPosition;
        this.currentVerticalPosition = this.currentVerticalPosition + this.verticalSpeed;
    }

}
