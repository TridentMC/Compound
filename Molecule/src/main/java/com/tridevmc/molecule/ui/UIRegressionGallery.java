package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.animation.Easing;
import com.tridevmc.compound.ui.element.Element;
import com.tridevmc.compound.ui.element.Checkbox;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.Divider;
import com.tridevmc.compound.ui.element.IComposableElement;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.ProgressBar;
import com.tridevmc.compound.ui.element.RadioButtonGroup;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.element.TextInput;
import com.tridevmc.compound.ui.element.ToggleSwitch;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.state.State;
import net.minecraft.network.chat.Component;

import java.util.List;

final class UIRegressionGallery extends GalleryModule {
    private final ScrollArea scroll = new ScrollArea();
    private final State<Integer> revision = State.of(0);
    private final Checkbox checkbox = new Checkbox(Component.literal("Original label"));
    private final RadioButtonGroup options = new RadioButtonGroup();
    private final ProgressBar progress = new ProgressBar();
    private final TextInput text = new TextInput().setCentered(true);
    private final ToggleSwitch toggle = new ToggleSwitch();
    private final Divider divider = new Divider(0xFF606060, 3);
    private boolean changed;

    UIRegressionGallery() {
        this.checkbox.setLabelColor(0x404040);
        this.options.addOption("Keep this option");
        this.options.addOption("Remove this option");
        this.options.setLabelColor(0x404040);
        this.progress.setProgress(50, 100);
        this.progress.setShowPercentage(true);
        this.text.setValue("ABCDE");
        this.toggle.setUseTextures(false);
        this.toggle.setThumbColor(0xFF00AAFF);
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.e(this.scroll, scroll -> {
            scroll.layout().fillMax();
            scroll.fillSlot(ScrollArea.CONTENT_SLOT, content -> content.e(new Column(), column -> {
                column.layout().fillMaxWidth().spacing(6);
                GalleryWidgets.button(column, "Recompose container", () -> this.revision.update(value -> value + 1));
                column.e(new Column(), bound -> {
                    bound.layout().fillMaxWidth().spacing(4);
                    bound.bindComposition(this.revision);
                    bound.e(new Label(Component.literal("Container revision: " + this.revision.get()), 0x404040, false));
                    if (this.revision.get() % 2 == 0) {
                        bound.e(new Label(Component.literal("Conditional child is visible"), 0x404040, false));
                    }
                });
                column.e(new Pulse(), pulse -> pulse.layout().fillMaxWidth().fixedHeight(6));
                column.e(new Label(() -> Component.literal("Active animations: "
                        + scope.getTree().getAnimationScheduler().getActiveAnimationCount()), () -> 0x404040, () -> false));
                GalleryWidgets.button(column, "Change mounted widgets", this::changeWidgets);
                column.e(this.checkbox);
                column.e(this.options);
                column.e(this.progress, bar -> bar.layout().fillMaxWidth().fixedHeight(14));
                column.e(this.toggle, control -> control.layout().fixedSize(50, 20));
                column.e(this.divider, line -> line.layout().fillMaxWidth());
                column.e(this.text, field -> field.layout().fillMaxWidth().fixedHeight(20));
                GalleryWidgets.button(column, "Long text then short text", () -> {
                    this.text.setValue("A long value that scrolls the field far beyond the end of the replacement text ".repeat(4));
                    this.text.setValue("ABCDE");
                });
            }));
        });
    }

    private void changeWidgets() {
        this.changed = !this.changed;
        this.checkbox.setLabel(Component.literal(this.changed ? "Updated label on the left" : "Original label"));
        this.checkbox.setLabelRight(!this.changed);
        this.checkbox.setSpacing(this.changed ? 10 : 4);
        this.progress.setProgress(50, this.changed ? 200 : 100);
        if (this.changed) this.options.removeOption(1);
        else this.options.addOption("Remove this option");
        this.divider.setColor(this.changed ? 0xFF008800 : 0xFF606060);
        this.divider.setThickness(this.changed ? 5 : 3);
        this.toggle.setThumbColor(this.changed ? 0xFFFFAA00 : 0xFF00AAFF);
    }

    private class Pulse extends Element implements IComposableElement {
        @Override
        public Size measure(Constraints constraints, LayoutProperties properties, List<Size> children) {
            return new Size(constraints.maxWidth(), 6);
        }

        @Override
        public List<Bounds> place(Bounds bounds, LayoutProperties properties, List<Size> children) {
            return List.of(bounds);
        }

        @Override
        public void compose(ICompositionScope scope) {
            scope.bindComposition(UIRegressionGallery.this.revision);
            var alpha = scope.animateFloatLooping(0.2F, 1F, 1000, Easing.LINEAR);
            scope.e(new Rect(() -> (Math.round(alpha.get() * 255) << 24) | 0x008800), fill -> fill.layout().fillMax());
        }
    }
}
