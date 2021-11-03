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
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;

/**
 * Base class for any containers that will be used with a Compound UI.
 * Includes basic functionality to locate slots off the screen so they can be rendered as elements instead.
 */
public abstract class CompoundContainerMenu extends AbstractContainerMenu {

    private static final WrappedField<Integer> SLOT_YPOS = WrappedField.create(Slot.class, "y", "field_75221_f");

    protected CompoundContainerMenu(@Nullable MenuType<?> type, int id) {
        super(type, id);
    }

    @Override
    protected Slot addSlot(Slot slotIn) {
        // Move the slot way off screen, we don't want to actually see it.
        SLOT_YPOS.set(slotIn, Integer.MIN_VALUE + slotIn.y);
        return super.addSlot(slotIn);
    }
}
