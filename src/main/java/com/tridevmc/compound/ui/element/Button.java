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

import com.google.common.collect.Lists;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.slot.SlotKey;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * A composable button container that can have children.
 * Manages hover state, enabled/disabled state, and handles click events.
 * Children render on top of the state-based background sprite.
 * <p>
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
                return false;
            }

            var bounds = this.getBounds();
            if (bounds == null) {
                return false;
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
                return true;
            } else {
                return false;
            }
        });

        // Create a Stack to layer background + user children
        scope.e(new WrappingStack(), stack -> {
            stack.layout().contentAlignment(Alignment.CENTER);

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
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        // Button has no fixed intrinsic size - flexible by default
        // Return size of composed Stack child, or fill available space
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(0, 0);
    }

    @Override
    public List<Bounds> place(Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }

        // Place Stack child at full bounds
        return List.of(bounds);
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

    public boolean isEnabled() {
        return this.enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public boolean isVisible() {
        return this.visible.get();
    }

    public void setVisible(boolean visible) {
        this.visible.set(visible);
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

    @Override
    public com.mojang.blaze3d.platform.cursor.CursorType getCursor(int x, int y) {
        return this.canPress() ? com.tridevmc.compound.ui.CompoundCursors.HAND : null;
    }

    /**
     * A specialized Stack that ignores the first child (background) for measurement
     * and forces the first child to match the stack's bounds during placement.
     */
    private static class WrappingStack extends Stack {
        @Override
        public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
            if (measuredChildren.isEmpty()) {
                return new Size(0, 0);
            }

            // If only background, return its size (likely max)
            if (measuredChildren.size() == 1) {
                return measuredChildren.get(0);
            }

            int maxWidth = 0;
            int maxHeight = 0;

            // Skip first child (background) for size calculation
            for (int i = 1; i < measuredChildren.size(); i++) {
                Size childSize = measuredChildren.get(i);
                maxWidth = Math.max(maxWidth, childSize.width());
                maxHeight = Math.max(maxHeight, childSize.height());
            }

            return new Size(maxWidth, maxHeight);
        }

        @Override
        public List<Bounds> place(Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
            List<Bounds> childBounds = super.place(bounds, props, measuredChildren);
            
            // Force background (index 0) to match stack bounds
            if (!childBounds.isEmpty()) {
                childBounds.set(0, new Bounds(bounds.position(), bounds.size()));
            }
            
            return childBounds;
        }
    }
}
