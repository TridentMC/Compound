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

import com.tridevmc.compound.ui.animation.AnimatedState;
import com.tridevmc.compound.ui.animation.Easing;
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProgressBar extends BaseElement implements IComposableElement {

    private static final int DEFAULT_MIN_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 12;

    private final State<Double> progress = new StateImpl<>(0.0);
    private final State<Boolean> indeterminate = new StateImpl<>(false);

    private int backgroundColor = 0x00000000;
    private int fillColor = 0xFFFFFFFF;
    private int textColor = 0xFFFFFFFF;
    private boolean showPercentage = false;
    private boolean showFraction = false;
    private Function<Double, String> labelFormatter;
    private Component label;
    private Consumer<Void> onCompleted;
    private double maxProgress = 1.0;

    public ProgressBar() {
    }

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

    public void setProgress(double progress) {
        double oldProgress = this.progress.get();
        double clamped = Math.clamp(progress, 0.0, this.maxProgress);
        this.progress.set(clamped);

        if (oldProgress < this.maxProgress && clamped >= this.maxProgress && this.onCompleted != null) {
            this.onCompleted.accept(null);
        }
    }

    public void setProgress(double current, double max) {
        this.maxProgress = max;
        this.setProgress(current);
        this.invalidateLayout();
    }

    public double getProgress() {
        return this.progress.get();
    }

    public double getNormalizedProgress() {
        return this.maxProgress > 0 ? this.progress.get() / this.maxProgress : 0.0;
    }

    public void setIndeterminate(boolean indeterminate) {
        this.indeterminate.set(indeterminate);
    }

    public boolean isIndeterminate() {
        return this.indeterminate.get();
    }

    public void setShowPercentage(boolean show) {
        this.showPercentage = show;
        this.invalidateComposition();
    }

    public void setShowFraction(boolean show) {
        this.showFraction = show;
        this.invalidateComposition();
    }

    public void setLabel(Component label) {
        this.label = label;
        this.invalidateComposition();
    }

    public void setLabelFormatter(Function<Double, String> formatter) {
        this.labelFormatter = formatter;
        this.invalidateComposition();
    }

    public void setOnCompleted(Consumer<Void> onCompleted) {
        this.onCompleted = onCompleted;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    public void setFillColor(int color) {
        this.fillColor = color;
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }

    public State<Double> getProgressState() {
        return this.progress;
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

    private static class ProgressFill extends BaseElement implements IComposableElement {
        private final Supplier<Integer> colorSupplier;
        private final Supplier<Double> normalizedSupplier;
        private final boolean indeterminate;
        private AnimatedState<Float> sweep;

        ProgressFill(Supplier<Integer> colorSupplier,
                     Supplier<Double> normalizedSupplier,
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
            double normalized = this.indeterminate ? 1.0 : this.normalizedSupplier.get();
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
