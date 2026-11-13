package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.ILazyCollection;
import com.tridevmc.compound.ui.element.ILazySource;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class StructuralLayoutLifecycleTest {
    @Test
    void attachingAndDetachingChildrenUpdatesLayoutWithTheSameViewport() {
        var tree = new UITree();
        var first = new Rect(0);
        var second = new Rect(0);
        var context = mock(IScreenContext.class);
        try {
            ICompositionScope.root(tree).e(new Column(), column -> {
                column.e(first, child -> child.layout().fixedSize(20, 20));
                column.e(second, child -> child.layout().fixedSize(20, 20));
            });
            tree.prepareFrame(100, 100, context);
            assertEquals(new Bounds(0, 20, 20, 20), second.getBounds());

            tree.detachNode(first.getNode());
            tree.prepareFrame(100, 100, context);
            assertEquals(new Bounds(0, 0, 20, 20), second.getBounds());
            assertEquals(20, tree.getRoot().getBounds().height());

            for (int pass = 0; pass < 3; pass++) {
                var added = tree.createNode(new Rect(0));
                added.getLayoutProperties().fixedSize(30, 10);
                tree.attachNode(tree.getRoot(), added);
                tree.prepareFrame(100, 100, context);
                assertEquals(new Bounds(0, 20, 30, 10), added.getBounds());
                assertEquals(new Bounds(0, 0, 30, 30), tree.getRoot().getBounds());

                tree.detachNode(added);
                tree.prepareFrame(100, 100, context);
                assertEquals(new Bounds(0, 0, 20, 20), tree.getRoot().getBounds());
            }
        } finally {
            tree.reset();
        }
    }

    @Test
    void recompositionAndLazyMountsSettleWithTheSameViewport() {
        var source = ILazySource.mutable(List.of(0, 1, 2, 3, 4, 5, 6, 7), item -> item);
        var collection = ILazyCollection.list(source, 20, 20, 0, (item, cell) ->
                cell.e(new Rect(0), rect -> rect.layout().fixedSize(20, 20)));
        var area = new ScrollArea().showScrollbar(false);
        var tree = new UITree();
        var context = mock(IScreenContext.class);
        try {
            ICompositionScope.root(tree).e(area, scroll -> {
                scroll.layout().fixedSize(100, 60);
                scroll.fillSlot(ScrollArea.CONTENT_SLOT, content -> content.e(collection));
            });
            tree.prepareFrame(100, 100, context);
            assertEquals(3, tree.getNodeForElement(collection).getChildCount());
            for (int pass = 0; pass < 3; pass++) {
                tree.requestRecompose(area.getNode());
                tree.prepareFrame(100, 100, context);
                assertEquals(3, tree.getNodeForElement(collection).getChildCount());
                area.scrollTo(0, 40);
                tree.prepareFrame(100, 100, context);
                assertEquals(3, tree.getNodeForElement(collection).getChildCount());
                area.scrollTo(0, 0);
                tree.prepareFrame(100, 100, context);
                assertEquals(3, tree.getNodeForElement(collection).getChildCount());
            }
        } finally {
            tree.reset();
        }
    }
}
