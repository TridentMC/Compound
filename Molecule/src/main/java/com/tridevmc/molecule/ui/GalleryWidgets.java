package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.element.Button;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import net.minecraft.network.chat.Component;

final class GalleryWidgets {
    private GalleryWidgets() {
    }

    static void button(ICompositionScope scope, String label, Runnable action) {
        scope.e(new Button(), button -> {
            button.layout().fillMaxWidth().fixedHeight(20);
            button.fillSlot(Button.CONTENT_SLOT, content -> content.e(new Label(Component.literal(label))));
            button.getElement().addPressListener((x, y) -> action.run());
        });
    }
}
