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

public class Label extends BaseElement implements IComposableElement {

    private Supplier<Component> textSupplier;
    private Supplier<Integer> colorSupplier;
    private Supplier<Boolean> shadowSupplier;
    private boolean wrap;

    public Label(Component text) {
        this(() -> text, () -> 0xFFFFFF, () -> true);
    }

    public Label(Component text, int color) {
        this(() -> text, () -> color, () -> true);
    }

    public Label(Component text, int color, boolean shadow) {
        this(() -> text, () -> color, () -> shadow);
    }

    public Label(Component text, Supplier<Integer> colorSupplier) {
        this(() -> text, colorSupplier, () -> true);
    }

    public Label(Component text, Supplier<Integer> colorSupplier, Supplier<Boolean> shadowSupplier) {
        this(() -> text, colorSupplier, shadowSupplier);
    }

    public Label(Supplier<Component> textSupplier, Supplier<Integer> colorSupplier) {
        this(textSupplier, colorSupplier, () -> true);
    }

    public Label(Supplier<Component> textSupplier, Supplier<Integer> colorSupplier, Supplier<Boolean> shadowSupplier) {
        this.textSupplier = textSupplier;
        this.colorSupplier = colorSupplier;
        this.shadowSupplier = shadowSupplier;
    }

    @Override
    public Component getNarrationMessage() {
        return this.textSupplier.get();
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.e(new Text(() -> this.textSupplier.get(), () -> this.colorSupplier.get(), () -> this.shadowSupplier.get()).setWrap(this.wrap));
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(0, 0);
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties ownProperties, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return List.of(bounds);
        }
        return List.of();
    }

    public Component getText() {
        return this.textSupplier.get();
    }

    public void setText(Component text) {
        this.textSupplier = () -> text;
        this.invalidateSize();
    }

    public Supplier<Component> getTextSupplier() {
        return this.textSupplier;
    }

    public void setTextSupplier(Supplier<Component> textSupplier) {
        this.textSupplier = textSupplier;
        this.invalidateSize();
    }

    public int getColor() {
        return this.colorSupplier.get();
    }

    public void setColor(int color) {
        this.colorSupplier = () -> color;
    }

    public Supplier<Integer> getColorSupplier() {
        return this.colorSupplier;
    }

    public void setColorSupplier(Supplier<Integer> colorSupplier) {
        this.colorSupplier = colorSupplier;
    }

    public boolean isShadow() {
        return this.shadowSupplier.get();
    }

    public void setShadow(boolean shadow) {
        this.shadowSupplier = () -> shadow;
    }

    public Supplier<Boolean> getShadowSupplier() {
        return this.shadowSupplier;
    }

    public void setShadowSupplier(Supplier<Boolean> shadowSupplier) {
        this.shadowSupplier = shadowSupplier;
    }

    private void invalidateSize() {
        var node = this.getNode();
        if (node != null) node.getTree().requestRemeasure(node);
    }

    public Label setWrap(boolean wrap) {
        this.wrap = wrap;
        var node = this.getNode();
        if (node != null) node.getTree().requestRecompose(node);
        return this;
    }
}
