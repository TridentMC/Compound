package com.tridevmc.compound.ui.element;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * UI-thread random access source with unique, non-null lifetime-stable keys.
 * Mutable sources publish changes after modifying data. Subscribers only enqueue work;
 * no source reads or tree mutations occur in the notification callback.
 */
public interface ILazySource<T, K> {
    int size();
    @Nonnull K key(int index);
    @Nonnull T item(int index);
    @Nonnull Subscription subscribe(@Nonnull Consumer<Change> subscriber);

    enum Kind { INSERT, REMOVE, UPDATE, RESET, REORDER }

    /** Ranges use indices at the time of the change; reset/reorder discard mounted cells. */
    record Change(Kind kind, int index, int count) {
        public Change {
            Objects.requireNonNull(kind);
            if (index < 0 || count < 0) throw new IllegalArgumentException("Negative change range");
        }
        public static Change reset() { return new Change(Kind.RESET, 0, 0); }
        public static Change reorder() { return new Change(Kind.REORDER, 0, 0); }
    }

    /** Disposal is idempotent and ends delivery, including queued subscriber delivery. */
    interface Subscription {
        Subscription NONE = () -> {};
        void dispose();
    }

    /** Data must remain unchanged for the source lifetime. Values are read only when mounted. */
    static <T, K> ILazySource<T, K> immutable(int size, IntFunction<K> keys,
            IntFunction<T> items) {
        if (size < 0) throw new IllegalArgumentException("Negative source size");
        Objects.requireNonNull(keys);
        Objects.requireNonNull(items);
        return new ILazySource<>() {
            public int size() { return size; }
            public K key(int index) { return keys.apply(index); }
            public T item(int index) { return items.apply(index); }
            public Subscription subscribe(Consumer<Change> subscriber) { return Subscription.NONE; }
        };
    }

    static <T, K> Mutable<T, K> mutable(List<T> items, Function<T, K> keys) {
        return new LazySource<>(items, keys);
    }

    /** Updates retain keys outside the changed range; reorder/reset intentionally remount. */
    interface Mutable<T, K> extends ILazySource<T, K> {
        void insert(int index, T item);
        void remove(int index);
        void update(int index, T item);
        void reset(List<T> items);
        void reorder(Comparator<T> order);
    }
}
