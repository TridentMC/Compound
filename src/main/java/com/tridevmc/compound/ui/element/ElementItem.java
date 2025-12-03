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

import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * A primitive element that renders an item stack.
 */
public class ElementItem extends BasePrimitiveElement {

    private Supplier<ItemStack> itemStackSupplier;
    private Supplier<String> countOverrideSupplier;

    public ElementItem(ItemStack itemStack) {
        this(() -> itemStack, () -> null);
    }

    public ElementItem(Supplier<ItemStack> itemStackSupplier) {
        this(itemStackSupplier, () -> null);
    }

    public ElementItem(Supplier<ItemStack> itemStackSupplier, Supplier<String> countOverrideSupplier) {
        this.itemStackSupplier = itemStackSupplier;
        this.countOverrideSupplier = countOverrideSupplier;
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren) {
        int size = Math.min(constraints.maxWidth(), constraints.maxHeight());
        size = Math.min(size, 16);
        return new Size(size, size);
    }

    @Override
    protected void drawElement(IScreenContext context, Bounds bounds) {
        ItemStack itemStack = this.itemStackSupplier.get();
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        String countOverride = this.countOverrideSupplier.get();
        var rect = new Rect2F(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        context.drawItemStack(itemStack, rect, countOverride);
    }

    public Supplier<ItemStack> getItemStackSupplier() {
        return this.itemStackSupplier;
    }

    public void setItemStackSupplier(Supplier<ItemStack> itemStackSupplier) {
        this.itemStackSupplier = itemStackSupplier;
    }

    public Supplier<String> getCountOverrideSupplier() {
        return this.countOverrideSupplier;
    }

    public void setCountOverrideSupplier(Supplier<String> countOverrideSupplier) {
        this.countOverrideSupplier = countOverrideSupplier;
    }
}
