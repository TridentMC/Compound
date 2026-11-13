package com.tridevmc.compound.ui.integration;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Button;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.EnumSelectionMode;
import com.tridevmc.compound.ui.element.ILazyCollection;
import com.tridevmc.compound.ui.element.ILazySource;
import com.tridevmc.compound.ui.element.ISelection;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.event.KeyInputEvent;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseReleaseEvent;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import com.tridevmc.compound.ui.visual.BufferedImageScreenContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class LazyGridSelectionTest {
    private static final int CELL = 24;
    private static final int PITCH = 28;
    private static final int BACKGROUND = 0xFF305060;

    @Test
    void controlClickAndControlShiftSelectDisconnectedCellsAndRanges() {
        try (var view = view(source(12), 128, 100, LazyGridSelectionTest::cell)) {
            view.click(0, false, false);
            view.click(5, false, true);
            assertEquals(List.of(0, 5), view.selection.getSelectedKeys());
            view.click(7, true, true);
            assertEquals(List.of(0, 5, 6, 7), view.selection.getSelectedKeys());
            try (var context = view.render()) {
                assertEquals(0xFFFFFFFF, context.getImage().getRGB(0, 0));
                assertEquals(BACKGROUND, context.getImage().getRGB(PITCH, 0));
                assertEquals(0xFFFFFFFF, context.getImage().getRGB(PITCH, PITCH));
                assertEquals(0xFFFFFFFF, context.getImage().getRGB(2 * PITCH, PITCH));
                assertEquals(0xFFFFFFFF, context.getImage().getRGB(3 * PITCH, PITCH));
                context.saveTo("build/screenshots/grid-multiple-selection.png");
            }
            view.click(6, false, true);
            assertEquals(List.of(0, 5, 7), view.selection.getSelectedKeys());
        }
    }

    @Test
    void gridKeyboardNavigationUsesColumnsAndRevealsFocusedCells() {
        try (var view = view(source(20), 128, 50, LazyGridSelectionTest::cell)) {
            view.click(1, false, false);
            view.key(InputConstants.KEY_DOWN, false, true);
            assertEquals(5, view.selection.getFocusedKey().orElseThrow());
            assertEquals(List.of(1), view.selection.getSelectedKeys());
            view.key(InputConstants.KEY_SPACE, false, true);
            view.key(InputConstants.KEY_RIGHT, true, true);
            assertEquals(List.of(1, 5, 6), view.selection.getSelectedKeys());
            view.key(InputConstants.KEY_END, false, true);
            view.prepare();
            assertEquals(19, view.selection.getFocusedKey().orElseThrow());
            assertEquals(view.area.getMaxScrollY(), view.area.getScrollYState().get());
            assertEquals(List.of(1, 5, 6), view.selection.getSelectedKeys());
            view.key(InputConstants.KEY_A, false, true);
            assertEquals(20, view.selection.getSelectedKeys().size());
            view.key(InputConstants.KEY_ESCAPE, false, false);
            assertTrue(view.selection.getSelectedKeys().isEmpty());
        }
    }

    @Test
    void stableSelectionSurvivesVirtualUnmountReorderAndRemoval() {
        var source = source(40);
        try (var view = view(source, 128, 52, LazyGridSelectionTest::cell)) {
            view.click(0, false, false);
            view.click(5, false, true);
            var first = view.tree.getNodeForElement(view.grid).getChildren().getFirst().getElement();
            view.area.scrollTo(0, view.area.getMaxScrollY());
            view.prepare();
            assertNull(view.tree.getNodeForElement(first));
            assertEquals(List.of(0, 5), view.selection.getSelectedKeys());
            view.area.scrollTo(0, 0);
            view.prepare();
            try (var context = view.render()) {
                assertEquals(0xFFFFFFFF, context.getImage().getRGB(0, 0));
                assertEquals(0xFFFFFFFF, context.getImage().getRGB(PITCH, PITCH));
            }
            source.reorder(Comparator.reverseOrder());
            view.prepare();
            assertEquals(List.of(5, 0), view.selection.getSelectedKeys());
            assertEquals(5, view.selection.getFocusedKey().orElseThrow());
            source.remove(34);
            view.prepare();
            assertEquals(List.of(0), view.selection.getSelectedKeys());
            assertTrue(view.selection.getFocusedKey().isEmpty());
            view.selection.select(1, true, true);
            assertEquals(List.of(1, 0), view.selection.getSelectedKeys());
        }
    }

    @Test
    void consumingCellButtonKeepsItsOwnInput() {
        var presses = new AtomicInteger();
        try (var view = view(source(4), 128, 50, (item, cell) -> {
            cell(item, cell);
            cell.e(new Button(), button -> {
                button.layout().fixedSize(10, 10);
                button.getElement().addPressListener((x, y) -> presses.incrementAndGet());
            });
        })) {
            view.clickAt(5, 5, false, false);
            assertEquals(1, presses.get());
            assertTrue(view.selection.getSelectedKeys().isEmpty());
            view.clickAt(20, 20, false, false);
            assertEquals(List.of(0), view.selection.getSelectedKeys());
        }
    }

    @Test
    void ordinaryFramesDoNotRescanKeysOrReadOffscreenItems() {
        var keyReads = new AtomicInteger();
        var itemReads = new AtomicInteger();
        var source = ILazySource.immutable(1_000, index -> {
            keyReads.incrementAndGet();
            return index;
        }, index -> {
            itemReads.incrementAndGet();
            return index;
        });
        try (var view = view(source, 128, 52, LazyGridSelectionTest::cell)) {
            assertEquals(8, itemReads.get());
            int keys = keyReads.get();
            for (int frame = 0; frame < 5; frame++) view.prepare();
            view.click(0, false, false);
            view.click(5, true, true);
            view.prepare();
            assertEquals(keys, keyReads.get());
            assertEquals(8, itemReads.get());
            assertFalse(view.selection.getSelectedKeys().isEmpty());
        }
    }

    @Test
    void keyboardRevealTraversesNestedHorizontalAndVerticalViewports() {
        var model = ISelection.<Integer>create(EnumSelectionMode.MULTIPLE);
        var grid = ILazyCollection.grid(source(20), 4, CELL, CELL, PITCH - CELL,
                LazyGridSelectionTest::cell).selection(model);
        var tree = new UITree();
        var outer = new ScrollArea().showScrollbar(false);
        var horizontal = new ScrollArea(ScrollArea.Direction.HORIZONTAL).showScrollbar(false);
        ICompositionScope.root(tree).e(outer, scroll -> {
            scroll.layout().fillMax();
            scroll.fillSlot(ScrollArea.CONTENT_SLOT, body -> body.e(new Column(), column -> {
                column.e(new Rect(0), spacer -> spacer.layout().fixedSize(64, 20));
                column.e(horizontal, nested -> {
                    nested.layout().fixedSize(64, 80);
                    nested.fillSlot(ScrollArea.CONTENT_SLOT, content -> content.e(grid));
                });
            }));
        });
        try {
            tree.prepareFrame(64, 60, mock(IScreenContext.class));
            tree.requestFocus(tree.getNodeForElement(grid));
            assertTrue(tree.dispatchKeyPress(new KeyInputEvent(InputConstants.KEY_END,
                    InputConstants.KEY_END, false, true, false)));
            tree.prepareFrame(64, 60, mock(IScreenContext.class));
            assertEquals(19, model.getFocusedKey().orElseThrow());
            assertTrue(horizontal.getScrollXState().get() > 0);
            assertTrue(outer.getScrollYState().get() > 0);
            var hit = tree.findNodeAt(52, 48);
            assertTrue(hit != null && tree.getNodeForElement(grid).isAncestorOf(hit));
        } finally {
            tree.reset();
        }
    }

    private static ILazySource.Mutable<Integer, Integer> source(int count) {
        return ILazySource.mutable(IntStream.range(0, count).boxed().toList(), key -> key);
    }

    private static void cell(Integer item, ICompositionScope cell) {
        cell.e(new Rect(BACKGROUND), rect -> rect.layout().fillMax());
    }

    private static View view(ILazySource<Integer, Integer> source, int width, int height,
            BiConsumer<Integer, ICompositionScope> composer) {
        var model = ISelection.<Integer>create(EnumSelectionMode.MULTIPLE);
        var grid = ILazyCollection.grid(source, 4, CELL, CELL, PITCH - CELL, composer).selection(model);
        var tree = new UITree();
        var area = new ScrollArea().showScrollbar(false);
        ICompositionScope.root(tree).e(area, scroll -> {
            scroll.layout().fillMax();
            scroll.fillSlot(ScrollArea.CONTENT_SLOT, body -> body.e(grid));
        });
        var view = new View(tree, area, grid, model, width, height);
        view.prepare();
        return view;
    }

    private record View(UITree tree, ScrollArea area, ILazyCollection<Integer, Integer> grid,
            ISelection<Integer> selection, int width, int height) implements AutoCloseable {
        void prepare() {
            this.tree.prepareFrame(this.width, this.height, mock(IScreenContext.class));
        }

        void click(int index, boolean shift, boolean ctrl) {
            int x = index % 4 * PITCH + CELL / 2;
            int y = index / 4 * PITCH + CELL / 2 - this.area.getScrollYState().get();
            this.clickAt(x, y, shift, ctrl);
        }

        void clickAt(int x, int y, boolean shift, boolean ctrl) {
            assertTrue(this.tree.dispatchClick(x, y,
                    new MouseClickEvent(x, y, InputConstants.MOUSE_BUTTON_LEFT, shift, ctrl, false)));
            this.tree.dispatchMouseRelease(x, y, new MouseReleaseEvent(x, y, InputConstants.MOUSE_BUTTON_LEFT));
        }

        void key(int physical, boolean shift, boolean ctrl) {
            int logical = physical == InputConstants.KEY_A ? InputConstants.KEYCODE_A : physical;
            assertTrue(this.tree.dispatchKeyPress(new KeyInputEvent(physical, logical, shift, ctrl, false)));
        }

        BufferedImageScreenContext render() {
            var context = new BufferedImageScreenContext(this.width, this.height);
            context.drawRect(0, 0, this.width, this.height, 0xFF181818);
            this.tree.layoutAndRender(this.width, this.height, context);
            return context;
        }

        @Override public void close() { this.tree.reset(); }
    }
}
