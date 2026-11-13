package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.scope.ICompositionScope;
import javax.annotation.Nonnull;
import java.util.function.BiConsumer;

/**
 * Opt-in vertical fixed-cell windowing inside a ScrollArea. Normal frame preparation mounts
 * visible cells. Source notifications queue reconciliation before layout, with automatic disposal.
 * Insert/remove preserve surviving cells; update remounts only changed cells. Reset/reorder
 * discard mounted cells and clear their focus/capture. Keep durable item state in the model.
 * At most two offscreen cells are retained for focus and pointer capture.
 * Cells clip their subtrees; layered descendants cannot escape. Put popups outside the cells.
 * Ancestor transforms are unsupported; cell contents may translate/scale within their clip.
 */
public interface ILazyCollection<T, K> extends IComposableElement {
    /**
     * Enables retained key-based selection with automatic outlines and input wiring.
     * Ctrl-click toggles cells; Shift selects a row-major range; Ctrl+Shift adds that range.
     * Arrow keys navigate rows/columns, Ctrl+arrows move focus, Ctrl+Space toggles, Ctrl+A
     * selects the entire source, and Escape clears selection. Consuming child controls keep
     * their own input. Source changes refresh key order and prune removed selections before
     * layout; values are still read only for mounted cells. Enabling this enumerates source
     * keys on source changes, not on ordinary frames. Keep one source order per model.
     *
     * @param selection the retained model, normally {@code ISelection.create(EnumSelectionMode.MULTIPLE)}
     * @return this collection
     */
    @Nonnull ILazyCollection<T, K> selection(@Nonnull ISelection<K> selection);

    @Nonnull
    static <T, K> ILazyCollection<T, K> grid(@Nonnull ILazySource<T, K> source,
            int columns, int cellWidth, int cellHeight, int spacing,
            @Nonnull BiConsumer<T, ICompositionScope> composer) {
        return new LazyCollection<>(source, columns, cellWidth, cellHeight, spacing, composer);
    }

    @Nonnull
    static <T, K> ILazyCollection<T, K> list(@Nonnull ILazySource<T, K> source,
            int rowWidth, int rowHeight, int spacing,
            @Nonnull BiConsumer<T, ICompositionScope> composer) {
        return grid(source, 1, rowWidth, rowHeight, spacing, composer);
    }
}
