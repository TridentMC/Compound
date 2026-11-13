package com.tridevmc.compound.ui.element;

import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

final class CollectionSelection<T> {
    private final Set<T> selected = new LinkedHashSet<>();
    private Optional<T> anchor = Optional.empty();
    private Optional<T> focus = Optional.empty();
    private EnumSelectionMode mode = EnumSelectionMode.SINGLE;

    boolean select(List<T> order, T item, boolean extend, boolean toggle) {
        var index = order.indexOf(item);
        if (index < 0) return false;
        var previous = Set.copyOf(this.selected);
        if (this.mode == EnumSelectionMode.MULTIPLE && extend) {
            var start = this.anchor.map(order::indexOf).filter(i -> i >= 0).orElse(index);
            if (!toggle) this.selected.clear();
            for (var i = Math.min(start, index); i <= Math.max(start, index); i++) {
                this.selected.add(order.get(i));
            }
            this.anchor = Optional.of(order.get(start));
        } else {
            if (this.mode == EnumSelectionMode.MULTIPLE && toggle) {
                if (!this.selected.remove(item)) this.selected.add(item);
            } else {
                this.selected.clear();
                this.selected.add(item);
            }
            this.anchor = Optional.of(item);
        }
        this.focus = Optional.of(item);
        return !previous.equals(this.selected);
    }

    boolean replace(List<T> order, List<T> values) {
        if (!new HashSet<>(order).containsAll(values)) {
            throw new IllegalArgumentException("Selection contains an unknown row");
        }
        var replacement = new LinkedHashSet<>(values);
        if (this.mode == EnumSelectionMode.SINGLE && replacement.size() > 1) {
            throw new IllegalArgumentException("Multiple rows require MULTIPLE selection mode");
        }
        var changed = !this.selected.equals(replacement);
        this.selected.clear();
        this.selected.addAll(replacement);
        this.focus = values.isEmpty() ? Optional.empty() : Optional.of(values.getLast());
        this.anchor = this.focus;
        return changed;
    }

    boolean selectAll(List<T> order) {
        return this.mode == EnumSelectionMode.MULTIPLE && this.replace(order, order);
    }

    boolean clear() {
        var changed = !this.selected.isEmpty();
        this.selected.clear();
        this.anchor = Optional.empty();
        this.focus = Optional.empty();
        return changed;
    }

    boolean retain(List<T> order) {
        var retained = new HashSet<>(order);
        var changed = this.selected.retainAll(retained);
        this.anchor = this.anchor.filter(retained::contains);
        this.focus = this.focus.filter(retained::contains);
        return changed;
    }

    void remap(Function<T, Optional<T>> mapper) {
        var replacement = this.selected.stream().flatMap(value -> mapper.apply(value).stream()).toList();
        this.selected.clear();
        this.selected.addAll(replacement);
        this.anchor = this.anchor.flatMap(mapper);
        this.focus = this.focus.flatMap(mapper);
    }

    boolean mode(EnumSelectionMode mode, List<T> order) {
        this.mode = Objects.requireNonNull(mode);
        if (mode == EnumSelectionMode.SINGLE && this.selected.size() > 1) {
            var retained = this.focus.filter(this.selected::contains)
                    .orElseGet(() -> this.snapshot(order).getFirst());
            return this.replace(order, List.of(retained));
        }
        return false;
    }

    EnumSelectionMode mode() {
        return this.mode;
    }

    void focus(T item) {
        this.focus = Optional.of(item);
    }

    int focusedIndex(List<T> order) {
        return this.focus.map(order::indexOf).orElse(-1);
    }

    boolean contains(T item) {
        return this.selected.contains(item);
    }

    boolean isFocused(T item) {
        return this.focus.isPresent() && this.focus.get().equals(item);
    }

    List<T> snapshot(List<T> order) {
        return order.stream().filter(this.selected::contains).toList();
    }

    Optional<T> primary(List<T> order) {
        return this.focus.filter(this.selected::contains).or(() -> this.snapshot(order).stream().findFirst());
    }
}
