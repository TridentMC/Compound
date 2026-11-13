package com.tridevmc.compound.ui.integration;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.ILazyCollection;
import com.tridevmc.compound.ui.element.ILazySource;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseReleaseEvent;
import com.tridevmc.compound.ui.event.MouseScrollEvent;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class LazyCollectionTest {
    @Test
    void sourceNotificationDuringCompositionSettlesWithoutMutationDuringIteration() {
        var source = ILazySource.mutable(java.util.List.of(0, 1, 2, 3, 4, 5), i -> i);
        var tree = new UITree();
        var compositions = new AtomicInteger();
        var once = new java.util.concurrent.atomic.AtomicBoolean();
        var collection = ILazyCollection.list(source, 20, 20, 0, (item, cell) -> {
            compositions.incrementAndGet();
            cell.e(new Rect(0));
            if (once.compareAndSet(false, true)) source.remove(0);
        });
        ICompositionScope.root(tree).e(collection);
        tree.prepareFrame(100, 100, mock(IScreenContext.class));
        assertEquals(5, tree.getNodeForElement(collection).getChildCount());
        assertEquals(6, compositions.get());
    }

    @Test
    void idleAndNotificationCallbacksReadNothingAndDetachUnsubscribes() {
        var fixture = new Fixture(50_000, 5);
        fixture.frame(100);
        assertEquals(1, fixture.source.sizeReads);
        assertEquals(1, fixture.source.subscribers.size());
        var retained = fixture.host().getChildren().getFirst();
        for (int frame = 0; frame < 10; frame++) fixture.frame(100);
        assertEquals(1, fixture.source.sizeReads);
        assertEquals(25, fixture.source.reads);
        assertEquals(25, fixture.source.keyReads);
        fixture.source.publish(new ILazySource.Change(ILazySource.Kind.UPDATE, 2, 1));
        fixture.source.publish(new ILazySource.Change(ILazySource.Kind.UPDATE, 3, 1));
        assertEquals(1, fixture.source.sizeReads);
        assertEquals(25, fixture.source.reads);
        assertEquals(25, fixture.source.keyReads);
        assertEquals(25, fixture.compositions.get());
        fixture.frame(100);
        assertEquals(2, fixture.source.sizeReads);
        assertEquals(27, fixture.source.reads);
        assertEquals(27, fixture.compositions.get());
        assertSame(retained, fixture.host().getChildren().getFirst());
        fixture.tree.reset();
        assertTrue(fixture.source.subscribers.isEmpty());
        fixture.source.publish(ILazySource.Change.reset());
        assertEquals(2, fixture.source.sizeReads);
    }

    @Test
    void mutableSourceChangesPreserveCellsAndReentrantDeliveryOrder() {
        var source = ILazySource.mutable(java.util.List.of(0, 1, 2, 3, 4, 5, 6), i -> i);
        var events = new java.util.ArrayList<ILazySource.Kind>();
        var subscription = source.subscribe(change -> {
            events.add(change.kind());
            if (change.kind() == ILazySource.Kind.INSERT) source.update(0, 99);
        });
        source.insert(0, 7);
        assertEquals(java.util.List.of(ILazySource.Kind.INSERT, ILazySource.Kind.UPDATE), events);
        subscription.dispose();
        subscription.dispose();
        source.remove(0);
        assertEquals(2, events.size());
        var tree = new UITree();
        var compositions = new AtomicInteger();
        var collection = ILazyCollection.list(source, 20, 20, 0, (item, cell) -> {
            compositions.incrementAndGet();
            cell.e(new Rect(0));
        });
        ICompositionScope.root(tree).e(collection);
        tree.prepareFrame(100, 100, mock(IScreenContext.class));
        var retained = tree.getNodeForElement(collection).getChildren().get(1);
        source.insert(0, 100);
        source.remove(0);
        source.update(0, 101);
        assertEquals(5, compositions.get());
        tree.prepareFrame(100, 100, mock(IScreenContext.class));
        assertEquals(6, compositions.get());
        assertSame(retained, tree.getNodeForElement(collection).getChildren().get(1));
    }

    @Test
    void gridsHaveIdenticalBoundedWorkAtTenAndFiftyThousand() {
        var mountedCounts = new ArrayList<Integer>();
        for (int size : new int[]{10_000, 50_000}) {
            var fixture = new Fixture(size, 5);
            fixture.frame(100);
            assertEquals(25, fixture.source.reads);
            assertEquals(25, fixture.compositions.get());
            assertEquals(25, fixture.host().getChildCount());
            assertEquals(25, fixture.draws.get());
            var mountedNodes = new AtomicInteger();
            fixture.tree.walkDepthFirst(fixture.tree.getRoot(), node -> mountedNodes.incrementAndGet());
            mountedCounts.add(mountedNodes.get());
            var overlap = fixture.host().getChildren().get(5);
            fixture.frame(100);
            assertEquals(25, fixture.source.reads);
            assertEquals(25, fixture.compositions.get());
            assertEquals(25, fixture.source.keyReads);
            assertTrue(fixture.tree.dispatchScroll(5, 5, new MouseScrollEvent(5, 5, -1)));
            fixture.frame(100);
            assertEquals(30, fixture.source.reads);
            assertEquals(30, fixture.compositions.get());
            assertEquals(50, fixture.source.keyReads);
            assertTrue(fixture.host().getChildren().contains(overlap));
            assertEquals(25, fixture.host().getChildCount());
            assertEquals(25, fixture.draws.get());
            fixture.area.scrollTo(0, Integer.MAX_VALUE);
            fixture.frame(100);
            assertEquals(55, fixture.source.reads);
            assertEquals(25, fixture.host().getChildCount());
            assertEquals(size / 5 * 20 - 100, fixture.area.getScrollYState().get());
            fixture.frame(60);
            assertEquals(15, fixture.host().getChildCount());
            fixture.source.size = 7;
            fixture.source.publish(ILazySource.Change.reset());
            fixture.frame(100);
            assertEquals(7, fixture.host().getChildCount());
            assertEquals(0, fixture.area.getScrollYState().get());
            fixture.source.size = 0;
            fixture.source.publish(ILazySource.Change.reset());
            fixture.frame(100);
            assertEquals(0, fixture.host().getChildCount());
            assertEquals(0, fixture.draws.get());
            fixture.tree.reset();
            assertNull(fixture.tree.getRoot());
        }
        assertEquals(mountedCounts.getFirst(), mountedCounts.getLast());
    }

    @Test
    void listPinsFocusAndCaptureThenReleasesAndResets() {
        var fixture = new Fixture(50_000, 1);
        fixture.frame(100);
        var cell = fixture.host().getChildren().getFirst();
        assertTrue(fixture.tree.dispatchClick(5, 5, new MouseClickEvent(5, 5, 1, false, false, false)));
        fixture.tree.requestFocus(cell);
        fixture.area.scrollTo(0, 2000);
        fixture.frame(100);
        assertEquals(6, fixture.host().getChildCount());
        assertSame(cell, fixture.tree.getFocusedNode());
        assertSame(cell, fixture.tree.getCapturedNode());
        assertEquals(5, fixture.draws.get());
        fixture.tree.clearFocus();
        fixture.frame(100);
        assertEquals(6, fixture.host().getChildCount());
        int sizeReads = fixture.source.sizeReads;
        int itemReads = fixture.source.reads;
        fixture.tree.dispatchMouseRelease(5, 5, new MouseReleaseEvent(5, 5, 1));
        fixture.frame(100);
        assertEquals(sizeReads, fixture.source.sizeReads);
        assertEquals(itemReads, fixture.source.reads);
        assertEquals(5, fixture.host().getChildCount());
        assertFalse(fixture.tree.hasNode(cell.getElement()));
        fixture.tree.requestFocus(fixture.host().getChildren().getFirst());
        fixture.source.publish(ILazySource.Change.reset());
        fixture.frame(100);
        assertNull(fixture.tree.getFocusedNode());
        assertNull(fixture.tree.getCapturedNode());
    }

    @Test
    void listFactoryHasBoundedWorkAndTwoIndependentPins() {
        for (int size : new int[]{10_000, 50_000}) {
            var fixture = new Fixture(size, 1);
            fixture.frame(100);
            assertEquals(5, fixture.source.reads);
            assertEquals(5, fixture.compositions.get());
            var capture = fixture.host().getChildren().getFirst();
            var focus = fixture.host().getChildren().get(1);
            fixture.tree.dispatchClick(5, 5, new MouseClickEvent(5, 5, 1, false, false, false));
            fixture.tree.requestFocus(focus);
            fixture.area.scrollTo(0, 2000);
            fixture.frame(100);
            assertEquals(7, fixture.host().getChildCount());
            assertEquals(10, fixture.source.reads);
            assertSame(capture, fixture.tree.getCapturedNode());
            assertSame(focus, fixture.tree.getFocusedNode());
            assertEquals(5, fixture.draws.get());
            fixture.source.size = 0;
            fixture.source.publish(ILazySource.Change.reset());
            fixture.frame(100);
            assertNull(fixture.tree.getCapturedNode());
            assertNull(fixture.tree.getFocusedNode());
            assertEquals(0, fixture.host().getChildCount());
        }
    }

    @Test
    void tabUsesLogicalOrderAfterScrollingBackAndSkipsOffscreenPins() {
        var fixture = new Fixture(10_000, 1);
        fixture.frame(100);
        var retained = fixture.host().getChildren().get(1);
        fixture.area.scrollTo(0, 20);
        fixture.frame(100);
        fixture.area.scrollTo(0, 0);
        fixture.frame(100);
        assertSame(retained, fixture.host().getChildren().get(1));
        fixture.tree.clearFocus();
        for (int index = 0; index < 5; index++) {
            assertTrue(fixture.tree.dispatchKeyPress(new com.tridevmc.compound.ui.event.KeyInputEvent(
                    com.mojang.blaze3d.platform.InputConstants.KEY_TAB, 0, false, false, false)));
            assertEquals(index * 20, fixture.tree.getFocusedNode().getBounds().y());
        }
        fixture.area.scrollTo(0, 200);
        fixture.frame(100);
        assertEquals(6, fixture.host().getChildCount());
        assertTrue(fixture.tree.dispatchKeyPress(new com.tridevmc.compound.ui.event.KeyInputEvent(
                com.mojang.blaze3d.platform.InputConstants.KEY_TAB, 0, false, false, false)));
        assertEquals(0, fixture.tree.getFocusedNode().getBounds().y());
    }

    @Test
    void partiallyVisibleCellDoesNotDrawPrimitiveOutsideScreen() {
        var tree = new UITree();
        var draws = new AtomicInteger();
        var collection = ILazyCollection.list(new Source(100), 200, 20, 0, (item, cell) ->
                cell.e(new Rect(0) {
                    @Override protected void drawElement(IScreenContext context,
                            com.tridevmc.compound.ui.layout.Bounds bounds) { draws.incrementAndGet(); }
                }, rect -> rect.layout().fixedSize(10, 10).margin(120, 0, 0, 0)));
        ICompositionScope.root(tree).e(new ScrollArea().showScrollbar(false), scroll -> {
            scroll.layout().fixedSize(200, 100).margin(50, 0, 0, 0);
            scroll.fillSlot(ScrollArea.CONTENT_SLOT, body -> body.e(collection));
        });
        tree.layoutAndRender(100, 100, mock(IScreenContext.class));
        assertEquals(5, tree.getNodeForElement(collection).getChildCount());
        assertEquals(0, draws.get());
        assertNull(tree.findNodeAt(125, 5));
    }

    @Test
    void horizontalScrollWindowsColumnsInsideNestedVerticalViewport() {
        var tree = new UITree();
        var source = new Source(50_000);
        var outer = new ScrollArea().showScrollbar(false);
        var horizontal = new ScrollArea(ScrollArea.Direction.HORIZONTAL).showScrollbar(false);
        var collection = ILazyCollection.grid(source, 20, 20, 20, 0,
                (item, cell) -> cell.e(new Rect(0), rect -> rect.layout().fixedSize(20, 20)));
        ICompositionScope.root(tree).e(outer, scroll -> {
            scroll.layout().fillMax();
            scroll.fillSlot(ScrollArea.CONTENT_SLOT, body -> body.e(new com.tridevmc.compound.ui.element.Column(), column -> {
                column.e(new Rect(0), spacer -> spacer.layout().fixedSize(100, 20));
                column.e(horizontal, nested -> {
                    nested.layout().fixedSize(100, 80);
                    nested.fillSlot(ScrollArea.CONTENT_SLOT, content -> content.e(collection));
                });
            }));
        });
        var context = mock(IScreenContext.class);
        tree.layoutAndRender(100, 100, context);
        var host = tree.getNodeForElement(collection);
        assertEquals(20, host.getChildCount());
        assertEquals(20, source.reads);
        var retained = host.getChildren().get(1);
        assertTrue(tree.dispatchScroll(5, 25, new MouseScrollEvent(5, 25, -1, 0)));
        tree.layoutAndRender(100, 100, context);
        assertEquals(20, horizontal.getScrollXState().get());
        assertEquals(24, source.reads);
        assertTrue(host.getChildren().contains(retained));
        assertEquals(20, host.getChildCount());
    }

    @Test
    void changedAndReorderedRevisionRecomposesValuesAndClearsPins() {
        var fixture = new Fixture(100, 1);
        fixture.frame(100);
        var old = fixture.host().getChildren().getFirst();
        fixture.tree.dispatchClick(5, 5, new MouseClickEvent(5, 5, 1, false, false, false));
        fixture.tree.requestFocus(old);
        fixture.source.reversed = true;
        fixture.source.valueOffset = 1000;
        fixture.source.publish(ILazySource.Change.reset());
        fixture.frame(100);
        assertEquals(java.util.List.of(1099, 1098, 1097, 1096, 1095),
                fixture.values.subList(5, 10));
        assertEquals(10, fixture.compositions.get());
        assertFalse(fixture.tree.hasNode(old.getElement()));
        assertNull(fixture.tree.getFocusedNode());
        assertNull(fixture.tree.getCapturedNode());
        fixture.frame(100);
        assertEquals(10, fixture.source.reads);
    }

    @Test
    void rejectsDuplicateKeysAndOverflow() {
        var fixture = new Fixture(100, 5);
        fixture.source.duplicates = true;
        assertThrows(IllegalArgumentException.class, () -> fixture.frame(100));
        assertThrows(IllegalArgumentException.class, () -> ILazyCollection.grid(new Source(1),
                Integer.MAX_VALUE, 20, 20, 0, (item, scope) -> {}));
        var huge = new Fixture(Integer.MAX_VALUE, 1);
        assertThrows(IllegalArgumentException.class, () -> huge.frame(100));
    }

    private static final class Source implements ILazySource<Integer, Integer> {
        int size;
        int sizeReads;
        int reads;
        int keyReads;
        final java.util.List<java.util.function.Consumer<Change>> subscribers = new java.util.ArrayList<>();
        boolean duplicates;
        boolean reversed;
        int valueOffset;
        Source(int size) { this.size = size; }
        public int size() { this.sizeReads++; return this.size; }
        public Subscription subscribe(java.util.function.Consumer<Change> subscriber) {
            this.subscribers.add(subscriber);
            return () -> this.subscribers.remove(subscriber);
        }
        void publish(Change change) {
            for (var subscriber : java.util.List.copyOf(this.subscribers)) subscriber.accept(change);
        }
        public Integer key(int index) {
            this.keyReads++;
            return this.duplicates ? 0 : this.reversed ? this.size - index - 1 : index;
        }
        public Integer item(int index) {
            this.reads++;
            return (this.reversed ? this.size - index - 1 : index) + this.valueOffset;
        }
    }

    private static final class Fixture {
        final UITree tree = new UITree();
        final ScrollArea area = new ScrollArea().showScrollbar(false);
        final Source source;
        final AtomicInteger compositions = new AtomicInteger();
        final AtomicInteger draws = new AtomicInteger();
        final IScreenContext context = mock(IScreenContext.class);
        final ILazyCollection<Integer, Integer> collection;
        final java.util.List<Integer> values = new java.util.ArrayList<>();
        Fixture(int count, int columns) {
            this.source = new Source(count);
            java.util.function.BiConsumer<Integer, ICompositionScope> composer = (item, scope) -> {
                this.compositions.incrementAndGet();
                this.values.add(item);
                scope.onClick(event -> true);
                scope.e(new Rect(0) {
                    @Override public boolean isFocusable() { return true; }
                    @Override protected void drawElement(IScreenContext context,
                            com.tridevmc.compound.ui.layout.Bounds bounds) { draws.incrementAndGet(); }
                }, rect -> rect.layout().fixedSize(20, 20).layer(10));
            };
            this.collection = columns == 1
                    ? ILazyCollection.list(this.source, 20, 20, 0, composer)
                    : ILazyCollection.grid(this.source, columns, 20, 20, 0, composer);
            ICompositionScope.root(this.tree).e(this.area, scroll -> {
                scroll.layout().fillMax();
                scroll.fillSlot(ScrollArea.CONTENT_SLOT, body -> body.e(this.collection));
            });
        }
        com.tridevmc.compound.ui.tree.ITreeNode host() {
            return this.tree.getNodeForElement(this.collection);
        }
        void frame(int height) {
            this.draws.set(0);
            this.tree.layoutAndRender(100, height, this.context);
        }
    }
}
