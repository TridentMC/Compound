package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.slot.SlotKey;
import com.tridevmc.compound.ui.state.State;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * A collapsible section with a button header and arbitrary content in {@link #CONTENT_SLOT}.
 * The same instance retains its expanded state; collapsing removes the body from the tree,
 * so retain body element instances separately when their state must survive expansion.
 */
public class Accordion extends BaseElement implements IComposableElement {
    /**
     * The body content, composed only while expanded.
     */
    public static final SlotKey CONTENT_SLOT = new SlotKey("content");

    private final Component title;
    private final State<Boolean> expanded;
    private Consumer<Boolean> onExpandedChanged = value -> {};

    /**
     * Creates a collapsed section.
     *
     * @param title the header label.
     */
    public Accordion(Component title) {
        this(title, false);
    }

    /**
     * Creates a section with the given initial expansion state.
     *
     * @param title the header label.
     * @param expanded whether to compose the body initially.
     */
    public Accordion(Component title, boolean expanded) {
        this.title = title;
        this.expanded = State.of(expanded);
    }

    /** {@inheritDoc} */
    @Override
    public void compose(ICompositionScope scope) {
        scope.bindLayout(this.expanded);
        scope.e(new Column(), column -> {
            column.layout().fillMaxWidth();
            column.e(new Button(), button -> {
                button.layout().fillMaxWidth().fixedHeight(20);
                button.fillSlot(Button.CONTENT_SLOT, content -> content.e(new Label(
                        () -> Component.literal(this.expanded.get() ? "- " : "+ ").append(this.title),
                        () -> 0xFFFFFF)));
                button.getElement().addPressListener((x, y) -> this.setExpanded(!this.isExpanded()));
            });
            column.e(new ExpandedContent(content -> scope.slotInto(CONTENT_SLOT, content)),
                    content -> content.layout().fillMaxWidth());
        });
    }

    /**
     * Returns whether the body is expanded.
     *
     * @return true when the body is expanded.
     */
    public boolean isExpanded() {
        return this.expanded.get();
    }

    /**
     * Changes expansion and invokes the listener only when the state changes.
     *
     * @param expanded whether to expand the body.
     */
    public void setExpanded(boolean expanded) {
        if (expanded != this.expanded.get()) {
            this.expanded.set(expanded);
            this.onExpandedChanged.accept(expanded);
        }
    }

    /**
     * Replaces the expansion listener for user and programmatic changes.
     *
     * @param listener the callback receiving the new expanded state.
     */
    public void setOnExpandedChanged(Consumer<Boolean> listener) {
        this.onExpandedChanged = listener;
    }

    /** {@inheritDoc} */
    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> children) {
        return children.isEmpty() ? new Size(0, 0) : children.getFirst();
    }

    /** {@inheritDoc} */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> children) {
        return children.isEmpty() ? List.of() : List.of(bounds);
    }

    // Only the body is rebuilt when toggled, preserving the header's keyboard focus.
    private class ExpandedContent extends BaseElement implements IComposableElement {
        private final Consumer<ICompositionScope> content;

        private ExpandedContent(Consumer<ICompositionScope> content) {
            this.content = content;
        }

        @Override
        public void compose(ICompositionScope scope) {
            scope.bindComposition(Accordion.this.expanded);
            if (!Accordion.this.isExpanded()) return;
            scope.e(new Column(), column -> {
                column.layout().fillMaxWidth().spacing(4).margin(0, 4, 0, 0);
                this.content.accept(column);
            });
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
}
