package com.tridevmc.compound.ui.integration;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.EnumSelectionMode;
import com.tridevmc.compound.ui.element.ListView;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class ListSelectionRevealTest {
    @Test
    void selectionBeforeMountIsRevealedByTheFirstLayout() {
        var list = list();
        list.setSelectedIndex(19);
        try (var mounted = mount(list, 96)) {
            mounted.prepare();
            assertEquals(224, mounted.area().getScrollYState().get());
            assertEquals(List.of(19), list.getSelectedIndices());
        }
    }

    @Test
    void multipleSelectionBetweenCompositionAndLayoutRevealsTheFocusedRow() {
        var list = list();
        list.setSelectionMode(EnumSelectionMode.MULTIPLE);
        try (var mounted = mount(list, 96)) {
            assertDoesNotThrow(() -> list.setSelectedIndices(List.of(1, 19)));
            mounted.prepare();
            assertEquals(224, mounted.area().getScrollYState().get());
            assertEquals(List.of(1, 19), list.getSelectedIndices());
        }
    }

    @Test
    void pendingRevealWaitsForANonzeroViewport() {
        var list = list();
        try (var mounted = mount(list, 0)) {
            assertDoesNotThrow(() -> list.setSelectedIndex(19));
            mounted.prepare();
            assertEquals(0, mounted.area().getScrollYState().get());
            mounted.tree().getRoot().getLayoutProperties().fixedHeight(96);
            mounted.tree().requestRemeasure(mounted.tree().getRoot());
            mounted.prepare();
            assertEquals(224, mounted.area().getScrollYState().get());
        }
    }

    @Test
    void pendingRevealUsesTheRecomposedRowHeightAndRemappedSelection() {
        var list = list();
        try (var mounted = mount(list, 96)) {
            mounted.prepare();
            list.setSelectedIndex(19);
            list.setItemHeight(24);
            list.removeItem(0);
            mounted.prepare();
            assertEquals(360, mounted.area().getScrollYState().get());
            assertEquals(List.of(18), list.getSelectedIndices());
        }
    }

    @Test
    void replacingItemsAndClearingSelectionCancelPendingReveal() {
        var list = list();
        try (var mounted = mount(list, 96)) {
            list.setSelectedIndex(19);
            list.setItems(IntStream.range(0, 20).boxed().toList());
            mounted.prepare();
            assertEquals(0, mounted.area().getScrollYState().get());
            assertEquals(List.of(), list.getSelectedIndices());
            list.setSelectedIndex(19);
            list.clearSelection();
            mounted.prepare();
            assertEquals(0, mounted.area().getScrollYState().get());
            assertEquals(List.of(), list.getSelectedIndices());
        }
    }

    private static ListView<Integer> list() {
        var list = new ListView<Integer>();
        list.setItems(IntStream.range(0, 20).boxed().toList());
        return list;
    }

    private static Mounted mount(ListView<Integer> list, int height) {
        var tree = new UITree();
        ICompositionScope.root(tree).e(list, scope -> scope.layout().fixedSize(120, height));
        var areas = new ArrayList<ScrollArea>();
        tree.walkDepthFirst(tree.getRoot(), node -> {
            if (node.getElement() instanceof ScrollArea area) areas.add(area);
        });
        assertEquals(1, areas.size());
        return new Mounted(tree, areas.getFirst());
    }

    private record Mounted(UITree tree, ScrollArea area) implements AutoCloseable {
        void prepare() {
            this.tree.prepareFrame(120, 96, mock(IScreenContext.class));
        }

        @Override
        public void close() {
            this.tree.reset();
        }
    }
}
