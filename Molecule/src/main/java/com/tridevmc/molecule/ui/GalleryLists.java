package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.element.ListView;
import com.tridevmc.compound.ui.element.EnumSelectionMode;
import com.tridevmc.compound.ui.scope.ICompositionScope;

import java.util.stream.IntStream;

final class GalleryLists extends GalleryModule {
    private final ListView<String> entries = new ListView<>();

    GalleryLists() {
        this.entries.setSelectionMode(EnumSelectionMode.MULTIPLE);
        this.entries.setItems(IntStream.rangeClosed(1, 100)
                .mapToObj(index -> "Storage entry " + index).toList());
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.e(this.entries, list -> list.layout().fillMax());
    }
}
