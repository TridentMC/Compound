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

import com.tridevmc.compound.ui.animation.AnimatedState;
import com.tridevmc.compound.ui.animation.Easing;
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.state.State;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.DoubleSupplier;

/**
 * A progress indicator with optional labels and an animated indeterminate mode.
 * Progress uses a configurable maximum, initially one.
 */
public class ProgressBar extends Element implements IComposableElement {

    private static final int DEFAULT_MIN_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 12;

    private final State<Double> progress = State.of(0.0);
    private final State<Boolean> indeterminate = State.of(false);

    private int backgroundColor = 0x00000000;
    private int fillColor = 0xFFFFFFFF;
    private int textColor = 0xFFFFFFFF;
    private boolean showPercentage = false;
    private boolean showFraction = false;
    private Function<Double, String> labelFormatter;
    private Component label;
    private Consumer<Void> onCompleted;
    private double maxProgress = 1.0;

    /**
     * Creates an empty progress bar with a maximum of one.
     */
    public ProgressBar() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compose(ICompositionScope scope) {
        scope.bindLayout(this.progress);
        scope.bind(this.indeterminate);

        scope.e(new Stack(), stack -> {
            stack.layout().fillMax();

            stack.e(new Surface(() -> this.backgroundColor, () -> this.fillColor, 1), frame -> {
                frame.layout().fillMax();
                frame.fillSlot(Surface.CONTENT_SLOT, content -> content.e(new Stack(), interior -> {
                    interior.layout().fillMax().margin(2).clip();
                    interior.e(new ProgressFill(
                            () -> this.fillColor, this::getNormalizedProgress, this.indeterminate.get()
                    ), fill -> fill.layout().fillMaxHeight().clip());
                }));
            });

            if (this.showPercentage || this.showFraction || this.label != null || this.labelFormatter != null) {
                stack.e(new Box(), labelBox -> {
                    labelBox.layout().fillMax().contentAlignment(Alignment.CENTER);
                    labelBox.e(new Label(
                        () -> {
                            if (this.label != null) return this.label;
                            if (this.indeterminate.get()) return Component.literal("");
                            if (this.showPercentage) {
                                double pct = this.maxProgress > 0
                                        ? (this.progress.get() / this.maxProgress) * 100.0
                                        : 0.0;
                                return Component.literal(String.format("%.0f%%", pct));
                            }
                            if (this.showFraction) {
                                return Component.literal(String.format("%.0f / %.0f",
                                        this.progress.get(), this.maxProgress));
                            }
                            if (this.labelFormatter != null) {
                                return Component.literal(this.labelFormatter.apply(this.progress.get()));
                            }
                            return Component.literal("");
                        },
                        () -> this.textColor,
                        () -> true
                    ));
                });
            }
        });
    }

    /**
     * Clamps progress to zero through the maximum and notifies the completion callback on an upward crossing.
     *
     * @param progress the requested progress in the current maximum's units
     */
    public void setProgress(double progress) {
        double oldProgress = this.progress.get();
        double clamped = Math.clamp(progress, 0.0, this.maxProgress);
        this.progress.set(clamped);

        if (oldProgress < this.maxProgress && clamped >= this.maxProgress && this.onCompleted != null) {
            this.onCompleted.accept(null);
        }
    }

    /**
     * Clamps progress to zero through the maximum and notifies the completion callback on an upward crossing.
     *
     * @param current the requested progress in the given maximum's units
     * @param max the finite, nonnegative maximum
     */
    public void setProgress(double current, double max) {
        this.maxProgress = max;
        this.setProgress(current);
        this.invalidateLayout();
    }

    /**
     * Returns progress in the configured maximum's units.
     *
     * @return the current absolute progress
     */
    public double getProgress() {
        return this.progress.get();
    }

    /**
     * Returns progress divided by the maximum, or zero when the maximum is not positive.
     *
     * @return the normalized progress
     */
    public double getNormalizedProgress() {
        return this.maxProgress > 0 ? this.progress.get() / this.maxProgress : 0.0;
    }

    /**
     * Switches between a measured fill and a looping animated sweep.
     *
     * @param indeterminate whether to show an animated sweep
     */
    public void setIndeterminate(boolean indeterminate) {
        this.indeterminate.set(indeterminate);
    }

    /**
     * Returns whether the animated sweep is enabled.
     *
     * @return true when enabled
     */
    public boolean isIndeterminate() {
        return this.indeterminate.get();
    }

    /**
     * Controls the default percentage label and requests recomposition.
     *
     * @param show whether to display it
     */
    public void setShowPercentage(boolean show) {
        this.showPercentage = show;
        this.invalidateComposition();
    }

    /**
     * Controls the current/maximum label and requests recomposition.
     *
     * @param show whether to display it
     */
    public void setShowFraction(boolean show) {
        this.showFraction = show;
        this.invalidateComposition();
    }

    /**
     * Sets a fixed label that takes precedence over generated labels.
     *
     * @param label the fixed label, or null to use generated labels
     */
    public void setLabel(Component label) {
        this.label = label;
        this.invalidateComposition();
    }

    /**
     * Sets a formatter used when no fixed, percentage, or fraction label is enabled.
     *
     * @param formatter the formatter receiving absolute progress, or null to disable it
     */
    public void setLabelFormatter(Function<Double, String> formatter) {
        this.labelFormatter = formatter;
        this.invalidateComposition();
    }

    /**
     * Sets the callback invoked when setProgress moves from below the maximum to the maximum.
     *
     * @param onCompleted the callback receiving null on completion, or null to disable it
     */
    public void setOnCompleted(Consumer<Void> onCompleted) {
        this.onCompleted = onCompleted;
    }

    /**
     * Sets the background ARGB color.
     *
     * @param color the drawing color
     */
    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    /**
     * Sets the progress fill ARGB color.
     *
     * @param color the drawing color
     */
    public void setFillColor(int color) {
        this.fillColor = color;
    }

    /**
     * Sets the text color.
     *
     * @param color the drawing color
     */
    public void setTextColor(int color) {
        this.textColor = color;
    }

    /**
     * Returns the live progress state. Prefer setProgress for clamping and completion callbacks.
     *
     * @return the live state; changes notify its observers
     */
    public State<Double> getProgressState() {
        return this.progress;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }

    private static class ProgressFill extends Element implements IComposableElement {
        private final IntSupplier colorSupplier;
        private final DoubleSupplier normalizedSupplier;
        private final boolean indeterminate;
        private AnimatedState<Float> sweep;

        ProgressFill(IntSupplier colorSupplier,
                     DoubleSupplier normalizedSupplier,
                     boolean indeterminate) {
            this.colorSupplier = colorSupplier;
            this.normalizedSupplier = normalizedSupplier;
            this.indeterminate = indeterminate;
        }

        @Override
        public void compose(ICompositionScope scope) {
            if (this.indeterminate) {
                if (this.sweep == null) {
                    this.sweep = scope.animateFloatLooping(-0.25F, 1F, 1200, Easing.LINEAR);
                }
                scope.retainAnimation(this.sweep);
                scope.bindLayout(this.sweep);
            }
            scope.e(new Rect(this.colorSupplier), rect -> rect.layout().fillMax());
        }

        @Override
        public void onDetached() {
            this.sweep = null;
        }

        @Override
        public Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren) {
            double normalized = this.indeterminate ? 1.0 : this.normalizedSupplier.getAsDouble();
            int fillWidth = (int) (constraints.maxWidth() * normalized);
            return new Size(fillWidth, constraints.maxHeight());
        }

        @Override
        public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
            if (this.indeterminate && this.sweep != null) {
                return List.of(new Bounds(bounds.x() + Math.round(bounds.width() * this.sweep.get()),
                        bounds.y(), Math.max(1, bounds.width() / 4), bounds.height()));
            }
            return List.of(bounds);
        }
    }
}
