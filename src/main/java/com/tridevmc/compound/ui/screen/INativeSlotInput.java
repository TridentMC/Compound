package com.tridevmc.compound.ui.screen;

import net.minecraft.world.inventory.Slot;

import java.util.List;

interface INativeSlotInput extends AutoCloseable {
    INativeSlotInput UNMAPPED = (slot, x, y) -> slot.x == x && slot.y == y;

    static INativeSlotInput capture(List<Slot> slots) {
        return new NativeSlotInput(slots);
    }

    boolean matches(Slot slot, int x, int y);

    @Override
    default void close() {
    }
}
