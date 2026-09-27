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
import java.util.Locale;
import java.util.function.Consumer;

/**
 * A numeric input field with validation for integer and decimal values.
 * Extends TextInput with numeric-specific features.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new NumberInput(), input -> {
 *     input.getElement().setRange(0, 100);
 *     input.getElement().setValue(50);
 *     input.getElement().setOnValueChanged(value -> {
 *         System.out.println("Value: " + value);
 *     });
 * });
 * </pre>
 */
public class NumberInput extends BaseElement implements IComposableElement {

    private final TextInput textInput;
    private final State<Double> value = new StateImpl<>(0.0);
    private double minValue = Double.NEGATIVE_INFINITY;
    private double maxValue = Double.POSITIVE_INFINITY;
    private boolean allowDecimals = true;
    private Consumer<Double> onValueChanged;

    public NumberInput() {
        this.textInput = new TextInput("0");
        this.setupFilter();
    }

    public NumberInput(double initialValue) {
        this.textInput = new TextInput();
        this.setupFilter();
        this.setValue(initialValue);
    }

    private void setupFilter() {
        this.textInput.setFilter(text -> {
            if (text == null || text.isEmpty()) return true;
            if (text.equals("-") && this.minValue < 0) return true;
            if (this.allowDecimals && text.equals(".")) return true;
            if (this.allowDecimals && this.minValue < 0 && text.equals("-.")) return true;

            try {
                double val = Double.parseDouble(text);
                return Double.isFinite(val) && (this.allowDecimals || val == Math.rint(val));
            } catch (NumberFormatException e) {
                return false;
            }
        });

        this.textInput.setResponder(text -> {
            if (text == null || text.isEmpty() || text.equals("-") || text.equals(".") ||
                    text.equals("-.")) {
                return;
            }

            try {
                double val = Double.parseDouble(text);
                double clamped = this.clamp(val);
                if (Double.compare(this.value.get(), clamped) != 0) {
                    this.value.set(clamped);
                    if (this.onValueChanged != null) this.onValueChanged.accept(clamped);
                }
            } catch (NumberFormatException ignored) {
            }
        });
        this.textInput.setOnCommit(text -> {
            try {
                this.setValue(Double.parseDouble(text));
            } catch (NumberFormatException ignored) {
                this.setValue(this.value.get());
            }
        });
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.e(this.textInput);
    }

    public void setValue(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Numeric input value must be finite");
        double clamped = this.clamp(value);
        this.textInput.setValue(this.allowDecimals ? String.valueOf(clamped) : String.format(Locale.ROOT, "%.0f", clamped));
    }

    private double clamp(double value) {
        double clamped = Math.clamp(value, this.minValue, this.maxValue);
        if (!this.allowDecimals) {
            clamped = Math.clamp(Math.rint(clamped), Math.ceil(this.minValue), Math.floor(this.maxValue));
        }
        return clamped;
    }

    public double getValue() {
        return this.value.get();
    }

    public int getIntValue() {
        return (int) Math.round(this.value.get());
    }

    public void setRange(double min, double max) {
        if (Double.isNaN(min) || Double.isNaN(max) || min > max || min == Double.POSITIVE_INFINITY || max == Double.NEGATIVE_INFINITY
                || (!this.allowDecimals && Math.ceil(min) > Math.floor(max))) {
            throw new IllegalArgumentException("Numeric input range must be ordered and contain a permitted value");
        }
        this.minValue = min;
        this.maxValue = max;
        this.setValue(this.value.get());
    }

    public void setAllowDecimals(boolean allow) {
        if (!allow && Math.ceil(this.minValue) > Math.floor(this.maxValue)) {
            throw new IllegalArgumentException("Numeric input range contains no integer");
        }
        this.allowDecimals = allow;
        this.setValue(this.value.get());
    }

    public void setOnValueChanged(Consumer<Double> onValueChanged) {
        this.onValueChanged = onValueChanged;
    }

    public TextInput getTextInput() {
        return this.textInput;
    }

    public void setMaxLength(int maxLength) {
        this.textInput.setMaxLength(maxLength);
    }

    public void setHint(Component hint) {
        this.textInput.setHint(hint);
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(
                Math.min(200, constraints.maxWidth()),
                Math.min(20, constraints.maxHeight())
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
