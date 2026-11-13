package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateObserver;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class EditorState<T> implements State<T> {
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final State<T> notifications;

    EditorState(Supplier<T> getter, Consumer<T> setter) {
        this.getter = getter;
        this.setter = setter;
        this.notifications = State.of(getter.get());
    }

    void refresh() { this.notifications.set(this.get()); }
    @Override
    public T get() { return this.getter.get(); }
    @Override
    public void set(T value) { this.setter.accept(value); }
    @Override
    public void update(Function<T, T> updater) { this.set(updater.apply(this.get())); }
    @Override
    public void addObserver(StateObserver observer) { this.notifications.addObserver(observer); }
    @Override
    public void removeObserver(StateObserver observer) { this.notifications.removeObserver(observer); }
    @Override
    public void dispose() { this.notifications.dispose(); }
}
