package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.element.Accordion;
import com.tridevmc.compound.ui.element.Box;
import com.tridevmc.compound.ui.element.Checkbox;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.CycleButton;
import com.tridevmc.compound.ui.element.Dropdown;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.NumberInput;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.element.Slider;
import com.tridevmc.compound.ui.element.TextInput;
import com.tridevmc.compound.ui.element.Tooltip;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.IntStream;

final class GalleryControls extends GalleryModule {
    private final GalleryActions actions;
    private final ScrollArea scroll = new ScrollArea();
    private final Dropdown<String> access = new Dropdown<>(List.of("Public", "Private", "Friends only"));
    private final TextInput name;
    private final NumberInput quantity = new NumberInput(50);
    private final Slider volume = new Slider(0, 100, 1);
    private final Checkbox enabled = new Checkbox(Component.literal("Auto-sort items"), true);
    private final CycleButton<String> mode = new CycleButton<>(Component.literal("Transfer"),
            List.of("Stack", "Single", "Half stack"), Component::literal);
    private final Accordion advanced = new Accordion(Component.literal("Advanced settings"));
    private final Dropdown<String> destinations = new Dropdown<>(IntStream.rangeClosed(1, 40)
            .mapToObj(index -> "Destination " + index).toList());

    GalleryControls(GalleryActions actions, TextInput name) {
        this.actions = actions;
        this.name = name;
        this.access.setSelected("Public");
        this.enabled.setLabelColor(0x404040);
        this.name.setHint(Component.literal("Item name"));
        this.quantity.setRange(0, 100);
        this.volume.setValue(75);
        this.volume.setShowValue(true);
        this.volume.setFormatter(value -> "Volume: " + Math.round(value) + "%");
        this.destinations.setSelected("Destination 30");
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.e(this.scroll, scroll -> {
            scroll.layout().fillMax();
            scroll.fillSlot(ScrollArea.CONTENT_SLOT, content -> content.e(new Column(), column -> {
                column.layout().fillMaxWidth().spacing(6);
                this.composeForm(column);
                this.composeActions(column);
                this.composeDestinations(column);
            }));
        });
    }

    private void composeForm(ICompositionScope scope) {
        scope.e(this.name, input -> input.layout().fillMaxWidth().fixedHeight(20));
        scope.e(this.quantity, input -> input.layout().fillMaxWidth().fixedHeight(20));
        scope.e(this.enabled, checkbox -> checkbox.layout().fillMaxWidth().fixedHeight(20));
        scope.e(this.mode, cycle -> cycle.layout().fillMaxWidth().fixedHeight(20));
        scope.e(new Label(Component.literal("Shift+click cycles backwards"), 0x404040, false));
        scope.e(this.volume, slider -> slider.layout().fillMaxWidth().fixedHeight(20));
        scope.e(this.access, dropdown -> dropdown.layout().fillMaxWidth().fixedHeight(20));
        scope.e(this.advanced, accordion -> {
            accordion.layout().fillMaxWidth();
            accordion.fillSlot(Accordion.CONTENT_SLOT, body -> {
                body.e(new Label(Component.literal("Custom content inside the accordion"), 0x404040, false));
                GalleryWidgets.button(body, "Open confirmation", this.actions.dialog::show);
            });
        });
    }

    private void composeActions(ICompositionScope scope) {
        GalleryWidgets.button(scope, "Count click", this.actions::count);
        scope.e(new Label(() -> Component.literal("Actions: " + this.actions.countValue()),
                () -> 0x404040, () -> false));
        GalleryWidgets.button(scope, "Open confirmation", this.actions.dialog::show);
        scope.e(new Tooltip(Component.literal("A vanilla tooltip, with wrapping for longer explanations.")), tooltip -> {
            tooltip.layout().fillMaxWidth().fixedHeight(20);
            tooltip.fillSlot(Tooltip.CONTENT_SLOT,
                    body -> GalleryWidgets.button(body, "Hover for help", this.actions::count));
        });
        scope.e(new Box(), target -> {
            target.layout().fillMaxWidth().fixedHeight(20);
            target.e(new Label(Component.literal("Right-click here for actions"), 0x404040, false));
            target.onClick(event -> {
                if (event.button() != InputConstants.MOUSE_BUTTON_RIGHT) return false;
                this.actions.contextMenu.show(event.x(), event.y());
                return true;
            });
        });
    }

    private void composeDestinations(ICompositionScope scope) {
        scope.e(new Label(Component.literal("40 destinations: open near the bottom edge"),
                0x404040, false));
        scope.e(this.destinations, dropdown -> dropdown.layout().fillMaxWidth().fixedHeight(20));
    }
}
