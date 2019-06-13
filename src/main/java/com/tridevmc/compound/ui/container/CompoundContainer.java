package com.tridevmc.compound.ui.container;

import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;

import javax.annotation.Nullable;

/**
 * Base class for any containers that will be used with a Compound UI.
 * Includes basic functionality to locate slots off the screen so they can be rendered as elements instead.
 */
public abstract class CompoundContainer extends Container {

    protected CompoundContainer(@Nullable ContainerType<?> type, int id) {
        super(type, id);
    }

    @Override
    protected Slot addSlot(Slot slotIn) {
        // Move the slot way off screen, we don't want to actually see it.
        slotIn.yPos = Integer.MIN_VALUE + slotIn.yPos;
        return super.addSlot(slotIn);
    }
}
