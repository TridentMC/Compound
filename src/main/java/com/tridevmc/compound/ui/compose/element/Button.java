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

package com.tridevmc.compound.ui.compose.element;

import com.google.common.collect.Lists;
import com.tridevmc.compound.ui.compose.layout.Alignment;
import com.tridevmc.compound.ui.compose.layout.Bounds;
import com.tridevmc.compound.ui.compose.layout.Constraints;
import com.tridevmc.compound.ui.compose.layout.LayoutHelper;
import com.tridevmc.compound.ui.compose.layout.Size;
import com.tridevmc.compound.ui.compose.scope.ICompositionScope;
import com.tridevmc.compound.ui.compose.slot.SlotKey;
import com.tridevmc.compound.ui.compose.state.State;
import com.tridevmc.compound.ui.compose.state.StateImpl;
import com.tridevmc.compound.ui.element.button.IButtonHoverListener;
import com.tridevmc.compound.ui.element.button.IButtonPressListener;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.util.List;
import java.util.function.Consumer;

/**
 * A composable button container that can have children.
 * Manages hover state, enabled/disabled state, and handles click events.
 * Children render on top of the state-based background sprite.
 *
 * Usage:
 * <pre>
 * scope.e(new Button(), button -> {
 *     button.fillSlot(Button.CONTENT_SLOT, content -> {
 *         content.e(new ElementLabel(Component.literal("Click me")));
 *     });
 * });
 * </pre>
 */
public class Button extends BaseElement implements IComposableElement {

    public static final SlotKey CONTENT_SLOT = new SlotKey("content");

    private static final IScreenSprite DEFAULT_ENABLED_SPRITE = IScreenSprite.of(ResourceLocation.withDefaultNamespace("widget/button"));
    private static final IScreenSprite DEFAULT_DISABLED_SPRITE = IScreenSprite.of(ResourceLocation.withDefaultNamespace("widget/button_disabled"));
    private static final IScreenSprite DEFAULT_HIGHLIGHTED_SPRITE = IScreenSprite.of(ResourceLocation.withDefaultNamespace("widget/button_highlighted"));

    private final State<Boolean> enabled;
    private final State<Boolean> visible;
    private final State<Boolean> hovered;
    private final List<IButtonPressListener> pressListeners;
    private final List<IButtonHoverListener> hoverListeners;

    public Button() {
        this(true, true);
    }

    public Button(boolean enabled) {
        this(enabled, true);
    }

    public Button(boolean enabled, boolean visible) {
        this.enabled = new StateImpl<>(enabled);
        this.visible = new StateImpl<>(visible);
        this.hovered = new StateImpl<>(false);
        this.pressListeners = Lists.newArrayList();
        this.hoverListeners = Lists.newArrayList();
    }

    @Override
    public void compose(ICompositionScope scope) {
        // Bind to state changes to trigger recomposition
        scope.bind(this.enabled);
        scope.bind(this.hovered);

        // Register mouse enter/exit handlers for hover state
        scope.onMouseEnter(() -> {
            this.hovered.set(true);
            var bounds = this.getBounds();
            if (bounds != null) {
                this.hoverListeners.forEach(listener -> listener.onButtonHover(
                    bounds.x() + bounds.width() / 2.0,
                    bounds.y() + bounds.height() / 2.0,
                    true
                ));
            }
        });

        scope.onMouseExit(() -> {
            this.hovered.set(false);
            var bounds = this.getBounds();
            if (bounds != null) {
                this.hoverListeners.forEach(listener -> listener.onButtonHover(
                    bounds.x() + bounds.width() / 2.0,
                    bounds.y() + bounds.height() / 2.0,
                    false
                ));
            }
        });

        // Register mouse click handler on button
        scope.onClick(event -> {
            if (!this.canPress()) {
                return;
            }

            var bounds = this.getBounds();
            if (bounds == null) {
                return;
            }

            int x = event.x();
            int y = event.y();

            // Check if click is within button bounds
            if (x >= bounds.x() && x < bounds.x() + bounds.width() &&
                y >= bounds.y() && y < bounds.y() + bounds.height()) {

                // Play click sound
                SoundManager soundManager = Minecraft.getInstance().getSoundManager();
                soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

                // Notify listeners
                this.pressListeners.forEach(listener -> listener.onButtonPress(x, y));
            }
        });

        // Create a Stack to layer background + user children
        scope.e(new Stack(), stack -> {
            stack.layout().fillMax().contentAlignment(Alignment.CENTER);

            // Background sprite based on state
            IScreenSprite backgroundSprite;
            if (!this.enabled.get()) {
                backgroundSprite = DEFAULT_DISABLED_SPRITE;
            } else if (this.hovered.get()) {
                backgroundSprite = DEFAULT_HIGHLIGHTED_SPRITE;
            } else {
                backgroundSprite = DEFAULT_ENABLED_SPRITE;
            }

            stack.e(new ElementSprite(backgroundSprite), sprite -> {
                sprite.layout().fillMax();
            });

            // Render user content slot into the stack (on top of background)
            scope.slotInto(CONTENT_SLOT, stack);
        });
    }

    @Override
    public Size measure(Constraints constraints, List<IElement> children) {
        // Button doesn't have a natural size preference, so use max intrinsic size
        var finalSize = LayoutHelper.calculateSizeWithProperties(
            Integer.MAX_VALUE, Integer.MAX_VALUE,
            this.getLayoutProperties(),
            constraints
        );

        if (!children.isEmpty()) {
            var childConstraints = Constraints.fixed(finalSize.width(), finalSize.height());
            LayoutHelper.measureChild(children.getFirst(), childConstraints);
        }

        return finalSize;
    }

    @Override
    public void place(Bounds bounds, List<IElement> children) {
        this.setBounds(bounds);

        if (!children.isEmpty()) {
            LayoutHelper.placeChild(children.getFirst(), bounds);
        }
    }

    private boolean canPress() {
        return this.visible.get() && this.enabled.get();
    }

    public void addPressListener(IButtonPressListener listener) {
        this.pressListeners.add(listener);
    }

    public void addHoverListener(IButtonHoverListener listener) {
        this.hoverListeners.add(listener);
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public boolean isEnabled() {
        return this.enabled.get();
    }

    public void setVisible(boolean visible) {
        this.visible.set(visible);
    }

    public boolean isVisible() {
        return this.visible.get();
    }

    public boolean isHovered() {
        return this.hovered.get();
    }

    public State<Boolean> getEnabledState() {
        return this.enabled;
    }

    public State<Boolean> getVisibleState() {
        return this.visible;
    }

    public State<Boolean> getHoveredState() {
        return this.hovered;
    }
}
