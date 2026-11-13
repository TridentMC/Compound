package com.tridevmc.compound.ui.integration;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.ILazyCollection;
import com.tridevmc.compound.ui.element.ILazySource;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class LazyCollectionReentrancyTest {
    @Test
    void focusLostSourceInsertionSettlesBeforeNewCellsMount() {
        var source = ILazySource.mutable(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), item -> item);
        var once = new AtomicBoolean();
        var collection = ILazyCollection.list(source, 20, 20, 0, (item, cell) -> {
            if (item == 0) cell.onFocusLost(() -> {
                if (once.compareAndSet(false, true)) source.insert(0, 99);
            });
            cell.e(new Rect(0), rect -> rect.layout().fixedSize(20, 20));
        });
        var tree = new UITree();
        ICompositionScope.root(tree).e(new ScrollArea().showScrollbar(false), scroll -> {
            scroll.layout().fillMax();
            scroll.fillSlot(ScrollArea.CONTENT_SLOT, body -> body.e(collection));
        });
        try {
            tree.prepareFrame(100, 100, mock(IScreenContext.class));
            var host = tree.getNodeForElement(collection);
            assertEquals(5, host.getChildCount());
            tree.requestFocus(host.getChildren().getFirst());
            source.remove(0);

            assertDoesNotThrow(() -> tree.prepareFrame(100, 100, mock(IScreenContext.class)));
            assertTrue(once.get());
            assertEquals(99, source.item(0));
            assertEquals(5, host.getChildCount());
            assertEquals(0, host.getChildren().getFirst().getBounds().y());
        } finally {
            tree.reset();
        }
    }

    @Test
    void offscreenDisposalCanInsertIntoTheSource() {
        var source = ILazySource.mutable(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), item -> item);
        var once = new AtomicBoolean();
        var collection = ILazyCollection.list(source, 20, 20, 0, (item, cell) ->
                cell.e(new Rect(0) {
                    @Override
                    public void onDetached() {
                        if (item == 0 && once.compareAndSet(false, true)) source.insert(0, 99);
                    }
                }, rect -> rect.layout().fixedSize(20, 20)));
        var tree = new UITree();
        var area = new ScrollArea().showScrollbar(false);
        ICompositionScope.root(tree).e(area, scroll -> {
            scroll.layout().fillMax();
            scroll.fillSlot(ScrollArea.CONTENT_SLOT, body -> body.e(collection));
        });
        try {
            tree.prepareFrame(100, 100, mock(IScreenContext.class));
            area.scrollTo(0, 100);
            assertDoesNotThrow(() -> tree.prepareFrame(100, 100, mock(IScreenContext.class)));
            assertTrue(once.get());
            assertEquals(99, source.item(0));
            area.scrollTo(0, 0);
            assertDoesNotThrow(() -> tree.prepareFrame(100, 100, mock(IScreenContext.class)));
            assertEquals(5, tree.getNodeForElement(collection).getChildCount());
        } finally {
            tree.reset();
        }
    }
}
