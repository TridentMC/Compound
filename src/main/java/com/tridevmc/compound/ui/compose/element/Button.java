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
import com.tridevmc.compound.ui.compose.layout.Bounds;
import com.tridevmc.compound.ui.compose.layout.Constraints;
import com.tridevmc.compound.ui.compose.layout.LayoutHelper;
import com.tridevmc.compound.ui.compose.layout.Size;
import com.tridevmc.compound.ui.compose.scope.IComposableElementScope;
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
 * A composable button element with customizable content via slots for each state.
 * Manages hover state, enabled/disabled state, and handles click events.
 *
 * Usage:
 * <pre>
 * scope.e(new Button(), button -> {
 *     button.slot(Button.ENABLED_SLOT, content -> {
 *         content.e(new ElementSprite(BUTTON_ENABLED_SPRITE));
 *     });
 *     button.slot(Button.HIGHLIGHTED_SLOT, content -> {
 *         content.e(new ElementSprite(BUTTON_HIGHLIGHTED_SPRITE));
 *     });
 *     button.slot(Button.DISABLED_SLOT, content -> {
 *         content.e(new ElementSprite(BUTTON_DISABLED_SPRITE));
 *     });
 * });
 * </pre>
 */
public class Button extends BaseElement implements IComposableElement {

    public static final SlotKey ENABLED_SLOT = new SlotKey("enabled");
    public static final SlotKey HIGHLIGHTED_SLOT = new SlotKey("highlighted");
    public static final SlotKey DISABLED_SLOT = new SlotKey("disabled");

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
        // Cast to IComposableElementScope for slot access
        IComposableElementScope<?> elementScope = (IComposableElementScope<?>) scope;

        // Bind to state changes to trigger recomposition
        scope.bind(this.enabled);
        scope.bind(this.hovered);

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

        // Render the appropriate slot based on current state
        if (!this.enabled.get()) {
            // Disabled state
            elementScope.slot(DISABLED_SLOT, content -> {
                content.e(new ElementSprite(DEFAULT_DISABLED_SPRITE));
            });
        } else if (this.hovered.get()) {
            // Highlighted/hover state
            elementScope.slot(HIGHLIGHTED_SLOT, content -> {
                content.e(new ElementSprite(DEFAULT_HIGHLIGHTED_SPRITE));
            });
        } else {
            // Normal/enabled state
            elementScope.slot(ENABLED_SLOT, content -> {
                content.e(new ElementSprite(DEFAULT_ENABLED_SPRITE));
            });
        }
    }

    @Override
    public Size measure(Constraints constraints, List<IElement> children) {
        // Step 1: Calculate final size using layout system (consistent with other composables)
        // Use large intrinsic size since Button doesn't have a natural size preference
        var finalSize = LayoutHelper.calculateSizeWithProperties(
            Integer.MAX_VALUE, Integer.MAX_VALUE,  // No intrinsic size preference
            this.getLayoutProperties(),
            constraints
        );

        // Step 2: Measure children with constraints matching our decided size
        if (!children.isEmpty()) {
            // Give child exact constraints matching our final size
            var childConstraints = Constraints.fixed(finalSize.width(), finalSize.height());
            LayoutHelper.measureChild(children.getFirst(), childConstraints);
        }

        // Step 3: Return our final size to parent
        return finalSize;
    }

    @Override
    public void place(Bounds bounds, List<IElement> children) {
        this.setBounds(bounds);

        // Place the slot content
        if (!children.isEmpty()) {
            LayoutHelper.placeChild(children.getFirst(), bounds);
        }
    }

    /**
     * Updates hover state when mouse position changes.
     * Should be called from mouse move events.
     */
    public void updateHoverState(int mouseX, int mouseY) {
        if (!this.canPress()) {
            if (this.hovered.get()) {
                this.hovered.set(false);
                this.hoverListeners.forEach(listener -> listener.onButtonHover(mouseX, mouseY, false));
            }
            return;
        }

        var bounds = this.getBounds();
        if (bounds == null) {
            return;
        }

        boolean nowHovered = mouseX >= bounds.x() && mouseX < bounds.x() + bounds.width() &&
                             mouseY >= bounds.y() && mouseY < bounds.y() + bounds.height();

        if (nowHovered != this.hovered.get()) {
            this.hovered.set(nowHovered);
            this.hoverListeners.forEach(listener -> listener.onButtonHover(mouseX, mouseY, nowHovered));
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
