package com.tridevmc.compound.ui.screen;

import com.tridevmc.compound.core.reflect.WrappedField;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.Objects;

final class NativeSlotInput implements INativeSlotInput {
    private static final WrappedField<Integer> SLOT_X = Objects.requireNonNull(
            WrappedField.create(Slot.class, "x", "field_75223_e"), "Slot x field");
    private static final WrappedField<Integer> SLOT_Y = Objects.requireNonNull(
            WrappedField.create(Slot.class, "y", "field_75221_f"), "Slot y field");
    private final List<Position> positions;

    NativeSlotInput(List<Slot> slots) {
        this.positions = slots.stream().map(slot -> new Position(slot, slot.x, slot.y)).toList();
        try {
            // Vanilla's private slot query passes coordinates instead of slot identity.
            for (int index = 0; index < this.positions.size(); index++) {
                var slot = this.positions.get(index).slot();
                SLOT_X.set(slot, index);
                SLOT_Y.set(slot, 0);
            }
        } catch (RuntimeException exception) {
            try {
                this.close();
            } catch (RuntimeException restoreException) {
                exception.addSuppressed(restoreException);
            }
            throw exception;
        }
    }

    @Override
    public boolean matches(Slot slot, int x, int y) {
        return y == 0 && x >= 0 && x < this.positions.size()
                && this.positions.get(x).slot() == slot;
    }

    @Override
    public void close() {
        for (var position : this.positions) {
            SLOT_X.set(position.slot(), position.x());
            SLOT_Y.set(position.slot(), position.y());
        }
    }

    private record Position(Slot slot, int x, int y) {
    }
}
