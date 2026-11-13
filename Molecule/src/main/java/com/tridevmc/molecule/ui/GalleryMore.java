package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.element.Button;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.ProgressBar;
import com.tridevmc.compound.ui.element.RadioButtonGroup;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.element.ToggleSwitch;
import com.tridevmc.compound.ui.element.TreeView;
import com.tridevmc.compound.ui.element.EnumSelectionMode;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import net.minecraft.network.chat.Component;

final class GalleryMore extends GalleryModule {
    private final GalleryActions actions;
    private final ScrollArea scroll = new ScrollArea();
    private final TreeView<String> storageTree = new TreeView<>();
    private final RadioButtonGroup sorting = new RadioButtonGroup();
    private final ToggleSwitch automatic = new ToggleSwitch(true);
    private final ProgressBar progress = new ProgressBar();
    private final ProgressBar loading = new ProgressBar();

    GalleryMore(GalleryActions actions) {
        this.actions = actions;
        this.storageTree.setSelectionMode(EnumSelectionMode.MULTIPLE);
        this.sorting.addOption("By name");
        this.sorting.addOption("By quantity");
        this.sorting.addOption("By item type");
        this.sorting.setSelectedIndex(0);
        this.sorting.setLabelColor(0x404040);
        this.progress.setProgress(65, 100);
        this.progress.setShowPercentage(true);
        this.loading.setIndeterminate(true);
        this.loading.setLabel(Component.literal("Loading..."));
        var building = this.storageTree.getRoot().addChild("Building materials");
        building.addChild("Stone");
        building.addChild("Wood", wood -> {
            wood.addChild("Oak");
            wood.addChild("Spruce");
        });
        building.setExpanded(true);
        var tools = this.storageTree.getRoot().addChild("Tools");
        tools.addChild("Pickaxe");
        tools.addChild("Axe");
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.e(this.scroll, scroll -> {
            scroll.layout().fillMax();
            scroll.fillSlot(ScrollArea.CONTENT_SLOT, content -> content.e(new Column(), column -> {
                column.layout().fillMaxWidth().spacing(6);
                column.e(new Label(Component.literal("Ctrl: toggle selection | Shift: select range"), 0x404040, false));
                column.e(this.storageTree, tree -> tree.layout().fillMaxWidth().fixedHeight(80));
                column.e(this.sorting, radio -> radio.layout().fillMaxWidth());
                column.e(new Label(Component.literal("Automatic transfer"), 0x404040, false));
                column.e(this.automatic, toggle -> toggle.layout().fixedSize(50, 20));
                column.e(this.progress, bar -> bar.layout().fillMaxWidth().fixedHeight(16));
                GalleryWidgets.button(column, "Advance progress", () -> this.progress.setProgress(
                        this.progress.getProgress() >= 100 ? 0 : this.progress.getProgress() + 5));
                column.e(this.loading, bar -> bar.layout().fillMaxWidth().fixedHeight(16));
                column.e(new Button(false), button -> {
                    button.layout().fillMaxWidth().fixedHeight(20);
                    button.fillSlot(Button.CONTENT_SLOT, body -> body.e(new Label(
                            Component.literal("Unavailable action"), 0xA0A0A0)));
                    button.getElement().addPressListener((x, y) -> this.actions.count());
                });
            }));
        });
    }
}
