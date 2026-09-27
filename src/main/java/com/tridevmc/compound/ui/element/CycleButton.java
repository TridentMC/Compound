package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.slot.SlotContent;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class CycleButton<T> extends Button {
    private final Component title;
    private final List<T> options;
    private final Function<T, Component> formatter;
    private final State<Integer> selectedIndex = new StateImpl<>(0);
    private Consumer<T> onValueChanged = value -> {};

    public CycleButton(Component title, List<T> options, Function<T, Component> formatter) {
        this.title = Objects.requireNonNull(title);
        this.options = List.copyOf(options);
        this.formatter = Objects.requireNonNull(formatter);
        if (this.options.isEmpty()) {
            throw new IllegalArgumentException("Cycle button requires at least one option");
        }
        this.addPressListener((x, y) -> {
            int direction = Minecraft.getInstance().hasShiftDown() ? -1 : 1;
            this.setValue(this.options.get(Math.floorMod(this.selectedIndex.get() + direction,
                    this.options.size())));
        });
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bindLayout(this.selectedIndex);
        if (!scope.getSlotMap().has(CONTENT_SLOT)) {
            scope.getSlotMap().put(CONTENT_SLOT, new SlotContent(content -> content.e(new Label(
                    () -> this.title.copy().append(": ").append(this.formatter.apply(this.getValue())),
                    () -> this.isEnabled() ? 0xFFFFFF : 0xA0A0A0))));
        }
        super.compose(scope);
    }

    public T getValue() {
        return this.options.get(this.selectedIndex.get());
    }

    public void setValue(T value) {
        int index = this.options.indexOf(value);
        if (index < 0) {
            throw new IllegalArgumentException("Cycle button value must belong to its options");
        }
        if (index != this.selectedIndex.get()) {
            this.selectedIndex.set(index);
            this.onValueChanged.accept(this.getValue());
        }
    }

    public void setOnValueChanged(Consumer<T> listener) {
        this.onValueChanged = Objects.requireNonNull(listener);
    }
}
