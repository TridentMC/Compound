package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.element.ContextMenu;
import com.tridevmc.compound.ui.element.Modal;
import net.minecraft.network.chat.Component;

final class GalleryActions {
    final ContextMenu contextMenu = new ContextMenu();
    final Modal dialog = new Modal(Component.literal("Confirm action"),
            Component.literal("Controls behind this dialog should be blocked. "
                    + "This deliberately long message must scroll while the action buttons stay visible. ".repeat(12)));
    private int clicks;

    GalleryActions() {
        this.dialog.addButton("Cancel", this.dialog::close);
        this.dialog.addButton("Confirm", () -> {
            this.count();
            this.dialog.close();
        });
        this.dialog.close();
        this.contextMenu.addItem("Count action", this::count);
        this.contextMenu.addItem("Reset actions", () -> this.clicks = 0);
        this.contextMenu.addSeparator();
        this.contextMenu.addItem("Cancel", this.contextMenu::hide);
    }

    void count() {
        this.clicks++;
    }

    int countValue() {
        return this.clicks;
    }
}
