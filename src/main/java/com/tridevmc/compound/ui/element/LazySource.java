package com.tridevmc.compound.ui.element;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

final class LazySource<T, K> implements ILazySource.Mutable<T, K> {
    private final List<T> items;
    private final Function<T, K> keys;
    private final List<Listener> listeners = new ArrayList<>();
    private final ArrayDeque<Change> changes = new ArrayDeque<>();
    private boolean delivering;

    LazySource(List<T> items, Function<T, K> keys) {
        this.items = new ArrayList<>(List.copyOf(items));
        this.keys = Objects.requireNonNull(keys);
    }

    public int size() { return this.items.size(); }
    public K key(int index) { return Objects.requireNonNull(this.keys.apply(this.items.get(index))); }
    public T item(int index) { return this.items.get(index); }

    public Subscription subscribe(Consumer<Change> subscriber) {
        var listener = new Listener(Objects.requireNonNull(subscriber));
        this.listeners.add(listener);
        return listener;
    }

    public void insert(int index, T item) {
        this.items.add(index, Objects.requireNonNull(item));
        this.publish(new Change(Kind.INSERT, index, 1));
    }

    public void remove(int index) {
        this.items.remove(index);
        this.publish(new Change(Kind.REMOVE, index, 1));
    }

    public void update(int index, T item) {
        this.items.set(index, Objects.requireNonNull(item));
        this.publish(new Change(Kind.UPDATE, index, 1));
    }

    public void reset(List<T> items) {
        var copy = List.copyOf(items);
        this.items.clear();
        this.items.addAll(copy);
        this.publish(Change.reset());
    }

    public void reorder(Comparator<T> order) {
        this.items.sort(order);
        this.publish(Change.reorder());
    }

    private void publish(Change change) {
        this.changes.addLast(change);
        if (this.delivering) return;
        this.delivering = true;
        try {
            while (!this.changes.isEmpty()) {
                var next = this.changes.removeFirst();
                for (var listener : List.copyOf(this.listeners)) {
                    if (listener.active) listener.subscriber.accept(next);
                }
            }
        } finally {
            this.delivering = false;
        }
    }

    private final class Listener implements Subscription {
        private final Consumer<Change> subscriber;
        private boolean active = true;
        Listener(Consumer<Change> subscriber) { this.subscriber = subscriber; }
        public void dispose() {
            this.active = false;
            listeners.remove(this);
        }
    }
}
