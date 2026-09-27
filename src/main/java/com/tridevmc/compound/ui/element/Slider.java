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

import com.mojang.blaze3d.platform.InputConstants;

import com.tridevmc.compound.ui.CompoundCursors;
import com.tridevmc.compound.ui.cursor.UICursor;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

    private static final int DEFAULT_MIN_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 20;

    private static final int HANDLE_WIDTH = 8;
    private static final int TEXT_MARGIN = 2;

    private static final IScreenSprite TRACK_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/slider"));
    private static final IScreenSprite TRACK_HIGHLIGHTED_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/slider_highlighted"));
    private static final IScreenSprite HANDLE_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/slider_handle"));
    private static final IScreenSprite HANDLE_HIGHLIGHTED_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/slider_handle_highlighted"));

    /**
     * Composed slider handle element. Measures as the full track size but places a
     * child sprite at the position derived from the normalized value supplier.
     */
    private class SliderThumb extends BaseElement implements IComposableElement {
        private final java.util.function.Supplier<IScreenSprite> spriteSupplier;
        private final java.util.function.Supplier<Double> normalizedSupplier;

        SliderThumb(java.util.function.Supplier<IScreenSprite> spriteSupplier,
                    java.util.function.Supplier<Double> normalizedSupplier) {
            this.spriteSupplier = spriteSupplier;
            this.normalizedSupplier = normalizedSupplier;
        }

        @Override
        public void compose(ICompositionScope scope) {
            scope.e(new Sprite(this.spriteSupplier), sprite -> sprite.layout().fixedSize(HANDLE_WIDTH, DEFAULT_HEIGHT));
        }

        @Override
        public Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren) {
            return new Size(constraints.maxWidth(), DEFAULT_HEIGHT);
        }

        @Override
        public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
            double normalized = this.normalizedSupplier.get();
            int trackWidth = bounds.width();
            int handleX = (int) (normalized * (trackWidth - HANDLE_WIDTH));
            return List.of(new Bounds(
                    new Position(bounds.x() + handleX, bounds.y()),
                    new Size(HANDLE_WIDTH, bounds.height())
            ));
        }
    }

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

    public Slider(double minValue, double maxValue, double step) {
        if (!Double.isFinite(minValue) || !Double.isFinite(maxValue) || minValue > maxValue) {
            throw new IllegalArgumentException("Slider range must be finite and ordered");
        }
        if (!Double.isFinite(step) || step < 0) throw new IllegalArgumentException("Slider step must be nonnegative");
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;
        this.value.set(minValue);
    }

    public Slider() {
        this(0.0, 1.0, 0.01);
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bindLayout(this.value);

        scope.onMouseEnter(() -> this.hovered.set(true));
        scope.onMouseExit(() -> this.hovered.set(false));

        scope.onClick(event -> {
            if (!this.enabled.get() || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
            scope.requestFocus();
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
                this.playClickSound();
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
                case InputConstants.KEY_LEFT, InputConstants.KEY_DOWN -> {
                    this.setValueInternal(this.value.get() - stepSize);
                    this.playClickSound();
                    yield true;
                }
                case InputConstants.KEY_RIGHT, InputConstants.KEY_UP -> {
                    this.setValueInternal(this.value.get() + stepSize);
                    this.playClickSound();
                    yield true;
                }
                case InputConstants.KEY_HOME -> {
                    this.setValueInternal(this.minValue);
                    this.playClickSound();
                    yield true;
                }
                case InputConstants.KEY_END -> {
                    this.setValueInternal(this.maxValue);
                    this.playClickSound();
                    yield true;
                }
                default -> false;
            };
        });

        scope.e(new Stack(), stack -> {
            stack.layout().fillMax();

            stack.e(new Sprite(() -> !this.enabled.get()
                    ? TRACK_SPRITE
                    : (this.hovered.get() || this.dragging.get()) ? TRACK_HIGHLIGHTED_SPRITE
                    : TRACK_SPRITE), bg -> bg.layout().fillMax());

            stack.e(new SliderThumb(
                    () -> !this.enabled.get() || (!this.hovered.get() && !this.dragging.get())
                            ? HANDLE_SPRITE
                            : HANDLE_HIGHLIGHTED_SPRITE,
                    () -> this.getNormalizedValue()
            ), handle -> handle.layout().fillMax());

            if (this.showValue) {
                stack.e(new Box(), labelBox -> {
                    // ActiveTextCollector centres the text and adds one pixel to its baseline.
                    labelBox.layout().fillMax().padding(TEXT_MARGIN, 2, TEXT_MARGIN, 0).contentAlignment(Alignment.CENTER);
                    labelBox.e(new Label(
                            () -> Component.literal(this.formatter.apply(this.value.get())),
                            () -> this.enabled.get() ? 0xFFFFFF : 0x808080,
                            () -> true));
                });
            }
        });
    }

    private double getNormalizedValue() {
        double range = this.maxValue - this.minValue;
        return range > 0 ? (this.value.get() - this.minValue) / range : 0;
    }

    private void updateValueFromPosition(int mouseX) {
        Bounds bounds = this.getBounds();
        if (bounds == null) return;

        int usableWidth = bounds.width() - HANDLE_WIDTH;
        if (usableWidth <= 0) return;
        double normalized = (mouseX - (bounds.x() + HANDLE_WIDTH / 2.0)) / usableWidth;
        normalized = Mth.clamp(normalized, 0.0, 1.0);

        double newValue = this.minValue + normalized * (this.maxValue - this.minValue);
        this.setValueInternal(newValue);
    }

    public void setValue(double value) {
        this.setValueInternal(value);
    }

    private void setValueInternal(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Slider value must be finite");
        double clamped = Mth.clamp(value, this.minValue, this.maxValue);
        if (this.step > 0) {
            clamped = this.minValue + Math.round((clamped - this.minValue) / this.step) * this.step;
            clamped = Mth.clamp(clamped, this.minValue, this.maxValue);
        }

        double oldValue = this.value.get();
        if (oldValue != clamped) {
            this.value.set(clamped);

            if (this.onValueChanged != null) {
                this.onValueChanged.accept(clamped);
            }
        }
    }

    private void playClickSound() {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
        if (!Double.isFinite(min) || !Double.isFinite(max) || min > max) {
            throw new IllegalArgumentException("Slider range must be finite and ordered");
        }
        this.minValue = min;
        this.maxValue = max;
        this.setValue(this.value.get());
        this.invalidateLayout();
    }

    public void setStep(double step) {
        if (!Double.isFinite(step) || step < 0) throw new IllegalArgumentException("Slider step must be nonnegative");
        this.step = step;
        this.setValue(this.value.get());
    }

    public boolean isShowValue() {
        return this.showValue;
    }

    public void setShowValue(boolean show) {
        this.showValue = show;
        this.invalidateComposition();
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
    public boolean isFocusable() {
        return this.enabled.get();
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
    public UICursor getCursor(int x, int y) {
        return this.enabled.get() ? CompoundCursors.HAND : null;
    }
}
