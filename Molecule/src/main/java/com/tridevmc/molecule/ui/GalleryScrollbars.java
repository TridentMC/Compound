package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.Row;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import net.minecraft.network.chat.Component;

final class GalleryScrollbars extends GalleryModule {
    private final Sample selection = new Sample("Selection list", ScrollArea.ScrollbarStyle.LIST);
    private final Sample grippy = new Sample("Grippy", ScrollArea.ScrollbarStyle.GRIPPY);

    @Override
    public void compose(ICompositionScope scope) {
        scope.e(new Row(), row -> {
            row.layout().fillMax().spacing(12);
            this.selection.compose(row);
            this.grippy.compose(row);
        });
    }

    private static final class Sample {
        private final String title;
        private final ScrollArea overflow;
        private final ScrollArea fitting;
        private final ScrollArea horizontal;

        private Sample(String title, ScrollArea.ScrollbarStyle style) {
            this.title = title;
            this.overflow = new ScrollArea().scrollbarStyle(style);
            this.fitting = new ScrollArea().scrollbarStyle(style);
            this.horizontal = new ScrollArea(ScrollArea.Direction.HORIZONTAL).scrollbarStyle(style);
        }

        private void compose(ICompositionScope scope) {
            scope.e(new Column(), column -> {
                column.layout().weight(1).fillMaxHeight().spacing(6);
                column.e(new Label(Component.literal(this.title), 0x404040, false));
                column.e(this.overflow, scroll -> {
                    scroll.layout().fillMaxWidth().weight(1);
                    scroll.fillSlot(ScrollArea.CONTENT_SLOT, content -> content.e(new Column(), entries -> {
                        entries.layout().fillMaxWidth().spacing(4);
                        for (int index = 1; index <= 40; index++) {
                            entries.e(new Label(Component.literal("Entry " + index), 0x404040, false));
                        }
                    }));
                });
                column.e(new Label(Component.literal("No overflow"), 0x404040, false));
                column.e(this.fitting, scroll -> {
                    scroll.layout().fillMaxWidth().fixedHeight(24);
                    scroll.fillSlot(ScrollArea.CONTENT_SLOT,
                            content -> content.e(new Label(Component.literal("All items fit"), 0x404040, false)));
                });
                column.e(this.horizontal, scroll -> {
                    scroll.layout().fillMaxWidth().fixedHeight(34);
                    scroll.fillSlot(ScrollArea.CONTENT_SLOT, content -> content.e(new Label(
                            Component.literal("Horizontal content: beginning, middle, and end."), 0x404040, false)));
                });
            });
        }
    }
}
