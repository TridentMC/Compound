package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.debug.DebugOverlayConfig;
import com.tridevmc.compound.ui.element.Box;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.Panel;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.element.Tabs;
import com.tridevmc.compound.ui.element.TextInput;
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.ComposedUI;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

public class UIGallery extends ComposedUI {
    private final Screen previousScreen;
    private final Tabs tabs = new Tabs();
    private final GalleryActions actions;

    public UIGallery(Screen previousScreen) {
        this(previousScreen, new GalleryActions());
    }

    private UIGallery(Screen previousScreen, GalleryActions actions) {
        this(previousScreen, actions, modules(actions));
    }

    UIGallery(Screen previousScreen, GalleryActions actions, List<GalleryModule> modules) {
        this.previousScreen = previousScreen;
        this.actions = actions;
        var titles = List.of("Controls", "Lists", "Text", "More", "Scrollbars", "Checks", "Animations", "Large grids");
        for (int index = 0; index < modules.size(); index++) {
            var module = modules.get(index);
            this.tabs.addTab(titles.get(index), scope ->
                    scope.e(module, body -> body.layout().fillMax()));
        }
    }

    private static List<GalleryModule> modules(GalleryActions actions) {
        var name = new TextInput();
        return List.of(new GalleryControls(actions, name), new GalleryLists(), new GalleryText(name),
                new GalleryMore(actions), new GalleryScrollbars(), new UIRegressionGallery(),
                new GalleryAnimations(), new GalleryLargeCollections());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_F7) {
            DebugOverlayConfig.get().toggle();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void compose(ICompositionScope scope) {
        scope.e(new Stack(), root -> {
            root.layout().fillMax().contentAlignment(Alignment.CENTER);
            root.e(new Panel(), panel -> {
                panel.layout().fixedSize(Math.min(570, this.width - 16),
                        Math.min(330, this.height - 16));
                panel.fillSlot(Panel.CONTENT_SLOT, this::composeBody);
            });
            root.e(this.actions.contextMenu, menu -> menu.layout().fillMax().layer(100));
            root.e(this.actions.dialog, modal -> modal.layout().fillMax());
        });
    }

    private void composeBody(ICompositionScope scope) {
        scope.e(new Box(), padding -> {
            padding.layout().fillMax().padding(10);
            padding.e(new Column(), column -> {
                column.layout().fillMax().spacing(6);
                column.e(new Label(Component.literal("Compound UI Gallery"), 0x404040, false));
                column.e(new Label(Component.literal("Tab: focus | Enter: activate | Wheel: scroll | F7: layout"),
                        0x404040, false));
                column.e(this.tabs, tabScope -> tabScope.layout().fillMaxWidth().weight(1));
                GalleryWidgets.button(column, "Done", this::onClose);
            });
        });
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.previousScreen);
    }
}
