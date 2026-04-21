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

import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.network.chat.Component;

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

    private final State<Double> progress = new StateImpl<>(0.0);
    private final State<Boolean> indeterminate = new StateImpl<>(false);

    private int backgroundColor = 0xFF404040;
    private int fillColor = 0xFF3366CC;
    private int textColor = 0xFFFFFFFF;
    private boolean showPercentage = false;
    private boolean showFraction = false;
    private Function<Double, String> labelFormatter;
    private Component label;
    private Consumer<Void> onCompleted;
    private double maxProgress = 1.0;
    private double currentProgress = 0.0;

    public ProgressBar() {
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bindLayout(this.progress);
        scope.bind(this.indeterminate);

        scope.e(new Stack(), stack -> {
            stack.layout().fillMax();

            stack.e(new Rect(() -> this.backgroundColor), bg -> bg.layout().fillMax());

            stack.e(new Stack(), fillStack -> {
                var bounds = this.getBounds();
                double normalizedProgress = this.maxProgress > 0
                        ? this.progress.get() / this.maxProgress
                        : 0.0;
                int fillWidth = bounds != null ? (int) (bounds.width() * normalizedProgress) : 0;

                fillStack.layout().fixedWidth(fillWidth).fillMaxHeight();
                fillStack.e(new Rect(() -> this.fillColor), r -> r.layout().fillMax());
            });

            if (this.showPercentage || this.showFraction || this.label != null) {
                stack.e(new Label(
                        () -> {
                            if (this.label != null) return this.label;
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
                        () -> false
                ), label -> label.layout().contentAlignment(Alignment.CENTER));
            }
        });
    }

    public void setProgress(double progress) {
        double oldProgress = this.progress.get();
        double clamped = Math.clamp(progress, 0.0, this.maxProgress);
        this.progress.set(clamped);
        this.currentProgress = clamped;

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
    }

    public void setShowFraction(boolean show) {
        this.showFraction = show;
    }

    public void setLabel(Component label) {
        this.label = label;
    }

    public void setLabelFormatter(Function<Double, String> formatter) {
        this.labelFormatter = formatter;
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
}
