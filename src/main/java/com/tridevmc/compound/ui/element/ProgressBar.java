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
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A visual indicator showing the completion progress of a task or operation.
 * Supports determinate and indeterminate modes.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new ProgressBar(), bar -> {
 *     bar.getElement().setProgress(0.75);
 *     bar.getElement().setShowPercentage(true);
 *     bar.getElement().setOnCompleted(() -> System.out.println("Complete!"));
 * });
 * </pre>
 */
public class ProgressBar extends BaseElement implements IComposableElement {

    private static final int DEFAULT_MIN_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 12;
    private static final IScreenSprite BACKGROUND = IScreenSprite.of(
            Identifier.withDefaultNamespace("boss_bar/green_background"));
    private static final IScreenSprite FILL = IScreenSprite.of(
            Identifier.withDefaultNamespace("boss_bar/green_progress"));

    private final State<Double> progress = new StateImpl<>(0.0);
    private final State<Boolean> indeterminate = new StateImpl<>(false);

    private int backgroundColor = 0xFF404040;
    private int fillColor = 0xFF3366CC;
    private int textColor = 0xFFFFFFFF;
    private boolean customBackground;
    private boolean customFill;
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

            if (this.customBackground) {
                stack.e(new Rect(() -> this.backgroundColor), bg -> bg.layout().fillMax());
            } else {
                stack.e(new Box(), background -> {
                    background.layout().fillMax().contentAlignment(Alignment.CENTER);
                    background.e(new Sprite(BACKGROUND), sprite -> sprite.layout().fillMaxWidth().fixedHeight(5));
                });
            }

            stack.e(new ProgressFill(
                    () -> this.fillColor,
                    () -> this.maxProgress > 0 ? this.progress.get() / this.maxProgress : 0.0,
                    this.customFill, this.indeterminate.get()
            ), fillRect -> fillRect.layout().fillMaxHeight().clip());

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
        this.invalidate();
    }

    public void setShowFraction(boolean show) {
        this.showFraction = show;
        this.invalidate();
    }

    public void setLabel(Component label) {
        this.label = label;
        this.invalidate();
    }

    public void setLabelFormatter(Function<Double, String> formatter) {
        this.labelFormatter = formatter;
        this.invalidate();
    }

    public void setOnCompleted(Consumer<Void> onCompleted) {
        this.onCompleted = onCompleted;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        if (!this.customBackground) {
            this.customBackground = true;
            this.invalidate();
        }
    }

    public void setFillColor(int color) {
        this.fillColor = color;
        if (!this.customFill) {
            this.customFill = true;
            this.invalidate();
        }
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }

    public State<Double> getProgressState() {
        return this.progress;
    }

    private void invalidate() {
        if (this.getNode() != null) this.getNode().getTree().requestRecompose(this.getNode());
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

    /**
     * Composed fill element that measures its width dynamically based on a normalized progress value.
     * This allows the fill width to update during remeasure without requiring recomposition.
     */
    private static class ProgressFill extends BaseElement implements IComposableElement {
        private final java.util.function.Supplier<Integer> colorSupplier;
        private final java.util.function.Supplier<Double> normalizedSupplier;
        private final boolean customColor;
        private final boolean indeterminate;
        private AnimatedState<Float> sweep;

        ProgressFill(java.util.function.Supplier<Integer> colorSupplier,
                     java.util.function.Supplier<Double> normalizedSupplier, boolean customColor,
                     boolean indeterminate) {
            this.colorSupplier = colorSupplier;
            this.normalizedSupplier = normalizedSupplier;
            this.customColor = customColor;
            this.indeterminate = indeterminate;
        }

        @Override
        public void compose(ICompositionScope scope) {
            if (this.indeterminate) {
                if (this.sweep == null) {
                    this.sweep = scope.animateFloatLooping(-0.25F, 1F, 1200, Easing.LINEAR);
                }
                scope.bindLayout(this.sweep);
            }
            if (this.customColor) {
                scope.e(new Rect(this.colorSupplier), rect -> rect.layout().fillMax());
            } else {
                scope.e(new Sprite(FILL), sprite -> sprite.layout().fillMax());
            }
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
            int height = this.customColor ? bounds.height() : Math.min(5, bounds.height());
            int y = bounds.y() + (bounds.height() - height) / 2;
            if (this.indeterminate && this.sweep != null) {
                return List.of(new Bounds(bounds.x() + Math.round(bounds.width() * this.sweep.get()),
                        y, Math.max(1, bounds.width() / 4), height));
            }
            return List.of(new Bounds(bounds.x(), y, bounds.width(), height));
        }
    }
}
