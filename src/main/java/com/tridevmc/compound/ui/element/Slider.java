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

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.tridevmc.compound.ui.CompoundCursors;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A draggable slider component for selecting numeric values from a continuous or discrete range.
 * Essential for volume controls, brightness, ranges, etc.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new Slider(0.0, 1.0, 0.01), slider -> {
 *     slider.getElement().setValue(0.75);
 *     slider.getElement().setFormatter(v -> String.format("%.0f%%", v * 100));
 *     slider.getElement().setOnValueChanged((old, newVal) -> {
 *         audioManager.setVolume(newVal);
 *     });
 * });
 * </pre>
 */
public class Slider extends BaseElement implements IComposableElement {

    private static final int DEFAULT_TRACK_HEIGHT = 4;
    private static final int DEFAULT_THUMB_SIZE = 12;
    private static final int DEFAULT_MIN_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 20;

    private static final IScreenSprite TRACK_SPRITE = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/slider"));
    private static final IScreenSprite TRACK_HIGHLIGHTED_SPRITE = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/slider_highlighted"));
    private static final IScreenSprite THUMB_SPRITE = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/slider_thumb"));
    private static final IScreenSprite THUMB_HIGHLIGHTED_SPRITE = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/slider_thumb_highlighted"));

    private final State<Double> value = new StateImpl<>(0.0);
    private final State<Boolean> enabled = new StateImpl<>(true);
    private final State<Boolean> hovered = new StateImpl<>(false);
    private final State<Boolean> dragging = new StateImpl<>(false);

    private double minValue;
    private double maxValue;
    private double step;
    private boolean showValue = false;
    private Function<Double, String> formatter = v -> String.valueOf(v);
    private Consumer<Double> onValueChanged;
    private Consumer<Double> onDragStart;
    private Consumer<Double> onDragEnd;
    private int trackColor = 0xFF808080;
    private int fillColor = 0xFF3366CC;
    private int thumbColor = 0xFFFFFFFF;

    public Slider(double minValue, double maxValue, double step) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;
    }

    public Slider() {
        this(0.0, 1.0, 0.01);
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.enabled);
        scope.bind(this.hovered);
        scope.bind(this.dragging);
        scope.bindLayout(this.value);

        scope.onMouseEnter(() -> this.hovered.set(true));
        scope.onMouseExit(() -> {
            this.hovered.set(false);
            if (this.dragging.get()) {
                this.dragging.set(false);
                if (this.onDragEnd != null) {
                    this.onDragEnd.accept(this.value.get());
                }
            }
        });

        scope.onClick(event -> {
            if (!this.enabled.get()) return false;
            this.dragging.set(true);
            this.updateValueFromPosition(event.x());
            if (this.onDragStart != null) {
                this.onDragStart.accept(this.value.get());
            }
            return true;
        });

        scope.onMouseDrag(event -> {
            if (!this.enabled.get() || !this.dragging.get()) return false;
            this.updateValueFromPosition(event.x());
            return true;
        });

        scope.onMouseRelease(event -> {
            if (this.dragging.get()) {
                this.dragging.set(false);
                if (this.onDragEnd != null) {
                    this.onDragEnd.accept(this.value.get());
                }
            }
            return false;
        });

        scope.onKeyPress(event -> {
            if (!this.enabled.get()) return false;
            double stepSize = this.step > 0 ? this.step : (this.maxValue - this.minValue) / 20.0;
            return switch (event.keyCode()) {
                case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT, org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN -> {
                    this.setValue(this.value.get() - stepSize);
                    yield true;
                }
                case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, org.lwjgl.glfw.GLFW.GLFW_KEY_UP -> {
                    this.setValue(this.value.get() + stepSize);
                    yield true;
                }
                case org.lwjgl.glfw.GLFW.GLFW_KEY_HOME -> {
                    this.setValue(this.minValue);
                    yield true;
                }
                case org.lwjgl.glfw.GLFW.GLFW_KEY_END -> {
                    this.setValue(this.maxValue);
                    yield true;
                }
                default -> false;
            };
        });

        scope.e(new Stack(), stack -> {
            stack.layout().fillMax();

            if (this.showValue) {
                stack.e(new Row(), row -> {
                    row.layout().fillMax().spacing(4).verticalAlignment(Alignment.CENTER);

                    row.e(new Stack(), trackStack -> {
                        trackStack.layout().fillMax();

                        trackStack.e(new Rect(() -> this.enabled.get() ? this.trackColor : 0xFF404040),
                                r -> r.layout().fillMax());

                        double currentValue = this.value.get();
                        double range = this.maxValue - this.minValue;
                        double normalized = range > 0 ? (currentValue - this.minValue) / range : 0;

                        trackStack.e(new Rect(() -> this.enabled.get() ? this.fillColor : 0xFF202020),
                                r -> {
                                    var bounds = this.getBounds();
                                    int fillWidth = bounds != null ? (int) (bounds.width() * normalized) : 0;
                                    r.layout().fixedWidth(fillWidth).fillMaxHeight();
                                });
                    });

                    row.e(new Label(
                            () -> Component.literal(this.formatter.apply(this.value.get())),
                            () -> this.enabled.get() ? 0xFFFFFF : 0x808080,
                            () -> false
                    ));
                });
            } else {
                stack.e(new Stack(), trackStack -> {
                    trackStack.layout().fillMax();

                    trackStack.e(new Rect(() -> this.enabled.get() ? this.trackColor : 0xFF404040),
                            r -> r.layout().fillMax());

                    double currentValue = this.value.get();
                    double range = this.maxValue - this.minValue;
                    double normalized = range > 0 ? (currentValue - this.minValue) / range : 0;

                    trackStack.e(new Rect(() -> this.enabled.get() ? this.fillColor : 0xFF202020),
                            r -> {
                                var bounds = this.getBounds();
                                int fillWidth = bounds != null ? (int) (bounds.width() * normalized) : 0;
                                r.layout().fixedWidth(fillWidth).fillMaxHeight();
                            });
                });
            }
        });
    }

    private void updateValueFromPosition(int mouseX) {
        Bounds bounds = this.getBounds();
        if (bounds == null) return;

        int trackX = bounds.x();
        int trackWidth = bounds.width();

        double normalized = (double) (mouseX - trackX) / trackWidth;
        normalized = Mth.clamp(normalized, 0.0, 1.0);

        double newValue = this.minValue + normalized * (this.maxValue - this.minValue);
        this.setValue(newValue);
    }

    public void setValue(double value) {
        double clamped = Mth.clamp(value, this.minValue, this.maxValue);
        if (this.step > 0) {
            clamped = Math.round(clamped / this.step) * this.step;
            clamped = Mth.clamp(clamped, this.minValue, this.maxValue);
        }

        double oldValue = this.value.get();
        if (oldValue != clamped) {
            this.value.set(clamped);

            SoundManager soundManager = Minecraft.getInstance().getSoundManager();
            soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

            if (this.onValueChanged != null) {
                this.onValueChanged.accept(clamped);
            }
        }
    }

    public double getValue() {
        return this.value.get();
    }

    public State<Double> getValueState() {
        return this.value;
    }

    public double getMinValue() {
        return this.minValue;
    }

    public double getMaxValue() {
        return this.maxValue;
    }

    public void setRange(double min, double max) {
        this.minValue = min;
        this.maxValue = max;
        this.setValue(this.value.get());
    }

    public void setStep(double step) {
        this.step = step;
        this.setValue(this.value.get());
    }

    public boolean isShowValue() {
        return this.showValue;
    }

    public void setShowValue(boolean show) {
        this.showValue = show;
    }

    public void setFormatter(Function<Double, String> formatter) {
        this.formatter = formatter;
    }

    public void setOnValueChanged(Consumer<Double> onValueChanged) {
        this.onValueChanged = onValueChanged;
    }

    public void setOnDragStart(Consumer<Double> onDragStart) {
        this.onDragStart = onDragStart;
    }

    public void setOnDragEnd(Consumer<Double> onDragEnd) {
        this.onDragEnd = onDragEnd;
    }

    public boolean isEnabled() {
        return this.enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(
                Math.max(DEFAULT_MIN_WIDTH, constraints.maxWidth()),
                Math.min(DEFAULT_HEIGHT, constraints.maxHeight())
        );
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }

    @Override
    public CursorType getCursor(int x, int y) {
        return this.enabled.get() ? CompoundCursors.HAND : null;
    }
}
