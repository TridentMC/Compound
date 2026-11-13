package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.ILazyCollection;
import com.tridevmc.compound.ui.element.ILazySource;
import com.tridevmc.compound.ui.element.ItemDisplay;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class GalleryLargeCollections extends GalleryModule {
    private final ScrollArea scroll = new ScrollArea();
    private final ILazyCollection<ItemStack, Integer> grid;

    GalleryLargeCollections() {
        this(index -> new ItemStack(index % 2 == 0 ? Items.DIAMOND : Items.EMERALD,
                index % 64 + 1));
    }

    GalleryLargeCollections(java.util.function.IntFunction<ItemStack> items) {
        this.grid = ILazyCollection.grid(ILazySource.immutable(50_000, index -> index, items),
                20, 20, 20, 2, (item, cell) -> cell.e(new ItemDisplay(item)));
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.e(new Column(), column -> {
            column.layout().fillMax().spacing(4);
            column.e(new Label(Component.literal("50,000 items: wheel or drag the scrollbar")));
            column.e(this.scroll, viewport -> {
                viewport.layout().fillMaxWidth().weight(1);
                viewport.fillSlot(ScrollArea.CONTENT_SLOT, content -> content.e(this.grid));
            });
        });
    }
}
