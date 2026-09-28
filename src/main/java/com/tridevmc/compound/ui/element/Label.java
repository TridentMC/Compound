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

import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Supplier;

/**
 * Composable read-only text with optional wrapping. Suppliers are read during measurement and
 * drawing; bind layout state when a supplier change can alter the measured size.
 */
public class Label extends BaseElement implements IComposableElement {

    private Supplier<Component> textSupplier;
    private Supplier<Integer> colorSupplier;
    private Supplier<Boolean> shadowSupplier;
    private boolean wrap;

    /**
     * Creates a label. Omitted color and shadow settings use white text with a shadow.
     *
     * @param text the non-null styled text
     */
    public Label(Component text) {
        this(() -> text, () -> 0xFFFFFF, () -> true);
    }

    /**
     * Creates a label. Omitted color and shadow settings use white text with a shadow.
     *
     * @param text the non-null styled text
     * @param color the drawing color
     */
    public Label(Component text, int color) {
        this(() -> text, () -> color, () -> true);
    }

    /**
     * Creates a label. Omitted color and shadow settings use white text with a shadow.
     *
     * @param text the non-null styled text
     * @param color the drawing color
     * @param shadow whether to draw a text shadow
     */
    public Label(Component text, int color, boolean shadow) {
        this(() -> text, () -> color, () -> shadow);
    }

    /**
     * Creates a label. Omitted color and shadow settings use white text with a shadow.
     *
     * @param text the non-null styled text
     * @param colorSupplier the non-null supplier of drawing colors
     */
    public Label(Component text, Supplier<Integer> colorSupplier) {
        this(() -> text, colorSupplier, () -> true);
    }

    /**
     * Creates a label. Omitted color and shadow settings use white text with a shadow.
     *
     * @param text the non-null styled text
     * @param colorSupplier the non-null supplier of drawing colors
     * @param shadowSupplier the non-null supplier controlling text shadows
     */
    public Label(Component text, Supplier<Integer> colorSupplier, Supplier<Boolean> shadowSupplier) {
        this(() -> text, colorSupplier, shadowSupplier);
    }

    /**
     * Creates a label. Omitted color and shadow settings use white text with a shadow.
     *
     * @param textSupplier the non-null supplier of non-null styled text
     * @param colorSupplier the non-null supplier of drawing colors
     */
    public Label(Supplier<Component> textSupplier, Supplier<Integer> colorSupplier) {
        this(textSupplier, colorSupplier, () -> true);
    }

    /**
     * Creates a label. Omitted color and shadow settings use white text with a shadow.
     *
     * @param textSupplier the non-null supplier of non-null styled text
     * @param colorSupplier the non-null supplier of drawing colors
     * @param shadowSupplier the non-null supplier controlling text shadows
     */
    public Label(Supplier<Component> textSupplier, Supplier<Integer> colorSupplier, Supplier<Boolean> shadowSupplier) {
        this.textSupplier = textSupplier;
        this.colorSupplier = colorSupplier;
        this.shadowSupplier = shadowSupplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getNarrationMessage() {
        return this.textSupplier.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compose(ICompositionScope scope) {
        scope.e(new Text(() -> this.textSupplier.get(), () -> this.colorSupplier.get(), () -> this.shadowSupplier.get()).setWrap(this.wrap));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(0, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties ownProperties, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return List.of(bounds);
        }
        return List.of();
    }

    /**
     * Evaluates the current text supplier.
     *
     * @return the current styled text
     */
    public Component getText() {
        return this.textSupplier.get();
    }

    /**
     * Replaces the text supplier with a constant and requests layout.
     *
     * @param text the non-null styled text
     */
    public void setText(Component text) {
        this.textSupplier = () -> text;
        this.invalidateLayout();
    }

    /**
     * Returns the supplier used for text measurement and drawing.
     *
     * @return the configured supplier
     */
    public Supplier<Component> getTextSupplier() {
        return this.textSupplier;
    }

    /**
     * Replaces the text supplier and requests layout. Bind layout state for later size-changing supplier updates.
     *
     * @param textSupplier the non-null supplier of non-null styled text
     */
    public void setTextSupplier(Supplier<Component> textSupplier) {
        this.textSupplier = textSupplier;
        this.invalidateLayout();
    }

    /**
     * Evaluates the current text color supplier.
     *
     * @return the current drawing color
     */
    public int getColor() {
        return this.colorSupplier.get();
    }

    /**
     * Sets the drawing color without changing layout.
     *
     * @param color the drawing color
     */
    public void setColor(int color) {
        this.colorSupplier = () -> color;
    }

    /**
     * Returns the color supplier sampled during drawing.
     *
     * @return the configured supplier
     */
    public Supplier<Integer> getColorSupplier() {
        return this.colorSupplier;
    }

    /**
     * Replaces the color supplier sampled during drawing.
     *
     * @param colorSupplier the non-null supplier of drawing colors
     */
    public void setColorSupplier(Supplier<Integer> colorSupplier) {
        this.colorSupplier = colorSupplier;
    }

    /**
     * Evaluates whether text is drawn with a shadow.
     *
     * @return true when enabled
     */
    public boolean isShadow() {
        return this.shadowSupplier.get();
    }

    /**
     * Sets whether text is drawn with a shadow.
     *
     * @param shadow whether to draw a text shadow
     */
    public void setShadow(boolean shadow) {
        this.shadowSupplier = () -> shadow;
    }

    /**
     * Returns the supplier controlling text shadows.
     *
     * @return the configured supplier
     */
    public Supplier<Boolean> getShadowSupplier() {
        return this.shadowSupplier;
    }

    /**
     * Replaces the supplier controlling text shadows.
     *
     * @param shadowSupplier the non-null supplier controlling text shadows
     */
    public void setShadowSupplier(Supplier<Boolean> shadowSupplier) {
        this.shadowSupplier = shadowSupplier;
    }

    /**
     * Sets whether text wraps to the available width.
     *
     * @param wrap whether to wrap text to the available width
     * @return this element
     */
    public Label setWrap(boolean wrap) {
        this.wrap = wrap;
        this.invalidateComposition();
        return this;
    }
}
