/*
 * Copyright 2018 - 2022 TridentMC
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

package com.tridevmc.compound.ui.element.button;

import com.google.common.collect.Lists;
import com.tridevmc.compound.ui.ICompoundUI;
import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.element.Element;
import com.tridevmc.compound.ui.layout.ILayout;
import com.tridevmc.compound.ui.listeners.IMousePressListener;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.util.List;


/**
 * A resizable button element to add to UIs, add hover and press listeners to add functionality.
 */
public class ElementButton extends Element {

    private static final ResourceLocation ENABLED_TEXTURE_LOCATION = new ResourceLocation("widget/button");
    private static final ResourceLocation DISABLED_TEXTURE_LOCATION = new ResourceLocation("widget/button_disabled");
    private static final ResourceLocation HIGHLIGHTED_TEXTURE_LOCATION = new ResourceLocation("widget/button_highlighted");
    private boolean isEnabled;
    private boolean isVisible;
    private boolean isHovered;
    private final List<IButtonPressListener> pressListeners;
    private final List<IButtonHoverListener> hoverListeners;

    public ElementButton(Rect2F dimensions, ILayout layout) {
        super(dimensions, layout);
        this.isEnabled = true;
        this.isVisible = true;
        this.isHovered = false;
        this.pressListeners = Lists.newArrayList();
        this.hoverListeners = Lists.newArrayList();
    }

    @Override
    public void drawBackground(ICompoundUI ui) {
        IScreenContext screen = ui.getScreenContext();
        Rect2F dimensions = this.getScreenspaceDimensions(screen);
        double mouseX = screen.getMouseX();
        double mouseY = screen.getMouseY();
        if (dimensions.isPointInRect(mouseX, mouseY) && this.canPress()) {
            if (!this.isHovered) {
                this.hoverListeners.forEach((l) -> l.onButtonHover(mouseX, mouseY, true));
                this.isHovered = true;
            }
        } else {
            if (this.isHovered) {
                this.hoverListeners.forEach((l) -> l.onButtonHover(mouseX, mouseY, false));
                this.isHovered = false;
            }
        }

        if (this.isVisible()) {
            var sprite = switch (this.getButtonState()) {
                case ENABLED -> IScreenSprite.of(ENABLED_TEXTURE_LOCATION);
                case DISABLED -> IScreenSprite.of(DISABLED_TEXTURE_LOCATION);
                case HIGHLIGHTED -> IScreenSprite.of(HIGHLIGHTED_TEXTURE_LOCATION);
            };
            screen.drawSprite(sprite, this.getDrawnDimensions(screen));
        }
    }

    @Override
    public void initElement(ICompoundUI ui) {
        ui.addListener((IMousePressListener) this::onMousePress);
    }

    public void addPressListener(IButtonPressListener listener) {
        this.pressListeners.add(listener);
    }

    public void addHoverListener(IButtonHoverListener listener) {
        this.hoverListeners.add(listener);
    }

    public void disable() {
        this.isEnabled = false;
    }

    public void enable() {
        this.isEnabled = true;
    }

    public void hide() {
        this.isVisible = false;
    }

    public void show() {
        this.isVisible = true;
    }

    public boolean isVisible() {
        return this.isVisible;
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    private void onMousePress(IScreenContext screen, double x, double y, int button) {
        if (button != 0)
            return;

        Rect2F dimensions = this.getDrawnDimensions(screen);
        if (dimensions.isPointInRect(x, y) && this.canPress()) {
            if (!this.isHovered) {
                this.hoverListeners.forEach((l) -> l.onButtonHover(x, y, true));
                this.isHovered = true;
            }
            screen.getMc().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.pressListeners.forEach((l) -> l.onButtonPress(x, y));
        }
    }

    private boolean canPress() {
        return this.isVisible && this.isEnabled;
    }

    private ButtonState getButtonState() {
        if (!this.isEnabled) {
            return ButtonState.DISABLED;
        } else if (this.isHovered) {
            return ButtonState.HIGHLIGHTED;
        } else {
            return ButtonState.ENABLED;
        }
    }

    private enum ButtonState {
        ENABLED,
        DISABLED,
        HIGHLIGHTED
    }

}
