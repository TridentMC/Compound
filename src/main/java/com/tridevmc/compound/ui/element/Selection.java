package com.tridevmc.compound.ui.element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

final class Selection<K> implements ISelection<K> {
    private final CollectionSelection<K> selection = new CollectionSelection<>();
    private List<K> keys = List.of();
    private Consumer<List<K>> listener = keys -> { };

    Selection(EnumSelectionMode mode) {
        this.selection.mode(mode, this.keys);
    }

    @Override
    public void setKeys(@Nonnull List<K> keys) {
        var replacement = List.copyOf(keys);
        if (new HashSet<>(replacement).size() != replacement.size()) {
            throw new IllegalArgumentException("Selection keys must be unique");
        }
        var previous = this.getSelectedKeys();
        this.keys = replacement;
        this.selection.retain(replacement);
        if (!previous.equals(this.getSelectedKeys())) this.notifySelection();
    }

    @Override public List<K> getKeys() { return this.keys; }
    @Override public List<K> getSelectedKeys() { return this.selection.snapshot(this.keys); }
    @Override public Optional<K> getFocusedKey() {
        var index = this.selection.focusedIndex(this.keys);
        return index < 0 ? Optional.empty() : Optional.of(this.keys.get(index));
    }
    @Override public boolean isSelected(@Nonnull K key) { return this.selection.contains(key); }
    @Override public boolean isFocused(@Nonnull K key) { return this.selection.isFocused(key); }

    @Override
    public void select(@Nonnull K key, boolean extend, boolean toggle) {
        this.requireKey(key);
        if (this.selection.select(this.keys, key, extend, toggle)) this.notifySelection();
    }

    @Override
    public void focus(@Nonnull K key) {
        this.requireKey(key);
        this.selection.focus(key);
    }

    @Override
    public void setSelectedKeys(@Nonnull List<K> keys) {
        if (this.selection.replace(this.keys, List.copyOf(keys))) this.notifySelection();
    }

    @Override
    public void selectAll() {
        if (this.selection.selectAll(this.keys)) this.notifySelection();
    }

    @Override
    public void clear() {
        if (this.selection.clear()) this.notifySelection();
    }

    @Override public EnumSelectionMode getMode() { return this.selection.mode(); }

    @Override
    public void setMode(@Nonnull EnumSelectionMode mode) {
        if (this.selection.mode(mode, this.keys)) this.notifySelection();
    }

    @Override
    public void setOnSelectionChanged(@Nullable Consumer<List<K>> listener) {
        this.listener = listener == null ? keys -> { } : listener;
    }

    private void requireKey(K key) {
        if (!this.keys.contains(Objects.requireNonNull(key))) {
            throw new IllegalArgumentException("Unknown selection key: " + key);
        }
    }

    private void notifySelection() {
        this.listener.accept(this.getSelectedKeys());
    }
}
