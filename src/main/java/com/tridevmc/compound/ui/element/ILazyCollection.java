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
