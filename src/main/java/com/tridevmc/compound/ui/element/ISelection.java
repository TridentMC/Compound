package com.tridevmc.compound.ui.element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Retained, game-independent selection over unique stable keys in display order.
 * Keep the model outside composition so virtual cell removal does not lose selection.
 * An attached lazy collection supplies its key order when source changes settle; custom
 * compositions can supply their own order through {@link #setKeys(List)}.
 *
 * <pre>
 * var selection = ISelection.&lt;Integer&gt;create(EnumSelectionMode.MULTIPLE);
 * var grid = ILazyCollection.grid(source, 4, 20, 20, 2, composer).selection(selection);
 * selection.setOnSelectionChanged(keys -&gt; updateActions(keys));
 * </pre>
 *
 * @param <K> the non-null stable key type
 */
public interface ISelection<K> {
    /** Creates an empty model using the given selection policy. */
    @Nonnull
    static <K> ISelection<K> create(@Nonnull EnumSelectionMode mode) {
        return new Selection<>(mode);
    }

    /**
     * Replaces display order, retaining surviving selections and pruning removed focus/anchors.
     * Duplicate or null keys are rejected before mutation. Reordering uses the new order for
     * subsequent ranges; callbacks receive immutable snapshots in that order.
     */
    void setKeys(@Nonnull List<K> keys);

    /** Returns the immutable display-order key snapshot. */
    @Nonnull List<K> getKeys();

    /** Returns selected keys in display order as an immutable snapshot. */
    @Nonnull List<K> getSelectedKeys();

    /** Returns the focused key, including a focused row that is not selected. */
    @Nonnull Optional<K> getFocusedKey();

    /** Returns whether the key is selected. */
    boolean isSelected(@Nonnull K key);

    /** Returns whether the key owns the model's keyboard focus. */
    boolean isFocused(@Nonnull K key);

    /**
     * Selects a known key. In MULTIPLE mode, toggle is Ctrl and extend is Shift:
     * Ctrl toggles, Shift replaces with an anchored range, and Ctrl+Shift adds that range.
     * Unknown keys are rejected before mutation. SINGLE always replaces the selection.
     */
    void select(@Nonnull K key, boolean extend, boolean toggle);

    /** Moves focus to a known key without changing selection or its range anchor. */
    void focus(@Nonnull K key);

    /** Replaces selection atomically; multiple keys require MULTIPLE mode. */
    void setSelectedKeys(@Nonnull List<K> keys);

    /** Selects all keys in MULTIPLE mode; SINGLE is unchanged. */
    void selectAll();

    /** Clears selected keys, focus, and anchor. */
    void clear();

    /** Returns the selection policy. */
    @Nonnull EnumSelectionMode getMode();

    /** Changes policy; SINGLE retains the focused selected key, or the first selected key. */
    void setMode(@Nonnull EnumSelectionMode mode);

    /** Receives immutable snapshots when selected keys/order change; null removes the callback. */
    void setOnSelectionChanged(@Nullable Consumer<List<K>> listener);
}
