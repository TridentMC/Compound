package com.tridevmc.compound.ui.integration;

import com.tridevmc.compound.ui.element.EnumSelectionMode;
import com.tridevmc.compound.ui.element.ISelection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionModelTest {
    @Test
    void controlShiftAddsAnAnchoredRangeWithoutDiscardingOtherSelections() {
        var model = ISelection.<String>create(EnumSelectionMode.MULTIPLE);
        model.setKeys(List.of("a", "b", "c", "d", "e"));
        var changes = new ArrayList<List<String>>();
        model.setOnSelectionChanged(changes::add);
        model.select("a", false, false);
        model.select("c", false, true);
        model.select("e", true, true);
        assertEquals(List.of("a", "c", "d", "e"), model.getSelectedKeys());
        model.select("d", false, true);
        assertEquals(List.of("a", "c", "e"), model.getSelectedKeys());
        assertThrows(UnsupportedOperationException.class, () -> changes.getFirst().add("changed"));
        model.clear();
        assertTrue(changes.getLast().isEmpty());
    }

    @Test
    void reorderedStableKeysRetainSelectionAndRemovedAnchorsAreRepaired() {
        var model = ISelection.<Integer>create(EnumSelectionMode.MULTIPLE);
        model.setKeys(List.of(0, 1, 2, 3, 4));
        model.select(0, false, false);
        model.select(2, false, true);
        model.setKeys(List.of(4, 3, 2, 1, 0));
        assertEquals(List.of(2, 0), model.getSelectedKeys());
        model.setKeys(List.of(4, 3, 1, 0));
        assertEquals(List.of(0), model.getSelectedKeys());
        assertTrue(model.getFocusedKey().isEmpty());
        model.select(3, true, true);
        assertEquals(List.of(3, 0), model.getSelectedKeys());
    }

    @Test
    void invalidUpdatesAreAtomicAndSingleModeHasOneSelection() {
        var model = ISelection.<Integer>create(EnumSelectionMode.SINGLE);
        model.setKeys(List.of(0, 1, 2));
        model.select(0, false, false);
        assertThrows(IllegalArgumentException.class, () -> model.setKeys(List.of(0, 0)));
        assertThrows(NullPointerException.class, () -> model.setKeys(Arrays.asList(0, null)));
        assertThrows(IllegalArgumentException.class, () -> model.setSelectedKeys(List.of(0, 1)));
        assertThrows(IllegalArgumentException.class, () -> model.select(3, false, false));
        assertEquals(List.of(0), model.getSelectedKeys());
        assertEquals(List.of(0, 1, 2), model.getKeys());
        model.select(2, true, true);
        assertEquals(List.of(2), model.getSelectedKeys());
        model.setMode(EnumSelectionMode.MULTIPLE);
        model.selectAll();
        model.focus(1);
        model.setMode(EnumSelectionMode.SINGLE);
        assertEquals(List.of(1), model.getSelectedKeys());
        assertFalse(model.isSelected(0));
    }
}
