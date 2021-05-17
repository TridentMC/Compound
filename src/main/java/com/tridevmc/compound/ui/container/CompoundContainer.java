/*
 * Copyright 2018 - 2021 TridentMC
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

package com.tridevmc.compound.ui.container;

import com.tridevmc.compound.core.reflect.WrappedField;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

/**
 * Base class for any containers that will be used with a Compound UI.
 * Includes basic functionality to locate slots off the screen so they can be rendered as elements instead.
 */
public abstract class CompoundContainer extends Container {

    private static final WrappedField<Integer> SLOT_X = WrappedField.create(Slot.class, "x", "field_75223_e");
    private static final WrappedField<Integer> SLOT_Y = WrappedField.create(Slot.class, "y", "field_75221_f");

    protected CompoundContainer(@Nullable ContainerType<?> type, int id) {
        super(type, id);
    }

    @Override
    protected Slot addSlot(Slot slot) {
        Slot out = super.addSlot(slot);
        SLOT_X.set(out, Integer.MIN_VALUE + (out.index * 16));
        SLOT_Y.set(out, Integer.MIN_VALUE);
        return out;
    }

    /**
     * Adds a slot for the given item handler at the given slot index - this can then be referenced in your UI as an element.
     *
     * @param handler the item handler that the slot is for.
     * @param slot    the index of the slot on the item handler.
     * @return the slot that was added to the container.
     */
    protected Slot addSlotFor(IItemHandler handler, int slot) {
        return this.addSlot(new SlotItemHandler(handler, slot, 0, 0));
    }

    /**
     * Adds a slot for the given inventory at the given slot index - this can then be referenced in your UI as an element.
     *
     * @param inventory the inventory that the slot is for.
     * @param slot      the index of the slot on the inventory.
     * @return the slot that was added to the container.
     */
    protected Slot addSlotFor(IInventory inventory, int slot) {
        return this.addSlot(new Slot(inventory, slot, 0, 0));
    }
}
