package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.slot.SlotKey;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

public class Accordion extends BaseElement implements IComposableElement {
    public static final SlotKey CONTENT_SLOT = new SlotKey("content");

    private final Component title;
    private final State<Boolean> expanded;
    private Consumer<Boolean> onExpandedChanged = value -> {};

    public Accordion(Component title) {
        this(title, false);
    }

    public Accordion(Component title, boolean expanded) {
        this.title = title;
        this.expanded = new StateImpl<>(expanded);
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bindComposition(this.expanded);
        scope.e(new Column(), column -> {
            column.layout().fillMaxWidth().spacing(4);
            column.e(new Button(), button -> {
                button.layout().fillMaxWidth().fixedHeight(20);
                button.fillSlot(Button.CONTENT_SLOT, content -> content.e(new Label(
                        Component.literal(this.expanded.get() ? "- " : "+ ").append(this.title))));
                button.getElement().addPressListener((x, y) -> this.setExpanded(!this.isExpanded()));
            });
            if (this.expanded.get()) {
                column.e(new Column(), content -> {
                    content.layout().fillMaxWidth().spacing(4);
                    scope.slotInto(CONTENT_SLOT, content);
                });
            }
        });
    }

    public boolean isExpanded() {
        return this.expanded.get();
    }

    public void setExpanded(boolean expanded) {
        if (expanded != this.expanded.get()) {
            this.expanded.set(expanded);
            this.onExpandedChanged.accept(expanded);
        }
    }

    public void setOnExpandedChanged(Consumer<Boolean> listener) {
        this.onExpandedChanged = listener;
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> children) {
        return children.isEmpty() ? new Size(0, 0) : children.getFirst();
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> children) {
        return children.isEmpty() ? List.of() : List.of(bounds);
    }
}
