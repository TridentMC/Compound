package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Box;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.element.Spacer;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class LayoutBoundsRegressionTest {
    @Test
    void paddedBoxInsideEitherScrollAxisKeepsIntrinsicExtent() {
        for (var direction : ScrollArea.Direction.values()) {
            var tree = new UITree();
            var area = new ScrollArea(direction).showScrollbar(false);
            var box = new Box();
            try {
                ICompositionScope.root(tree).e(area, scroll -> {
                    scroll.layout().fixedSize(100, 100);
                    scroll.fillSlot(ScrollArea.CONTENT_SLOT, content -> content.e(box, body -> {
                        body.layout().padding(4);
                        body.e(new Spacer(10, 10));
                    }));
                });
                tree.prepareFrame(100, 100, mock(IScreenContext.class));
                assertEquals(0, area.getMaxScrollX());
                assertEquals(0, area.getMaxScrollY());
                assertEquals(18, direction == ScrollArea.Direction.VERTICAL
                        ? box.getBounds().height() : box.getBounds().width());
            } finally {
                tree.reset();
            }
        }
    }

    @Test
    void alignedStackInsideEitherScrollAxisKeepsIntrinsicExtent() {
        for (var direction : ScrollArea.Direction.values()) {
            var tree = new UITree();
            var area = new ScrollArea(direction).showScrollbar(false);
            var stack = new Stack();
            try {
                ICompositionScope.root(tree).e(area, scroll -> {
                    scroll.layout().fixedSize(100, 100);
                    scroll.fillSlot(ScrollArea.CONTENT_SLOT, content -> content.e(stack, body -> {
                        body.layout().contentAlignment(Alignment.CENTER);
                        body.e(new Spacer(10, 10));
                    }));
                });
                tree.prepareFrame(100, 100, mock(IScreenContext.class));
                assertEquals(0, area.getMaxScrollX());
                assertEquals(0, area.getMaxScrollY());
                assertEquals(10, direction == ScrollArea.Direction.VERTICAL
                        ? stack.getBounds().height() : stack.getBounds().width());
            } finally {
                tree.reset();
            }
        }
    }

    @Test
    void rootFixedSizeAndMarginsSurviveFramePreparation() {
        assertRootBounds(props -> props.fixedSize(10, 12).margin(3, 4, 5, 6),
                new Bounds(3, 4, 10, 12));
    }

    @Test
    void rootMaximumsAndExplicitFillRemainEffective() {
        assertRootBounds(props -> props.fillMax(), new Bounds(0, 0, 100, 80));
        assertRootBounds(props -> props.fillMax().margin(3, 4, 5, 6),
                new Bounds(3, 4, 92, 70));
        assertRootBounds(props -> props.fillMax().maxWidth(40).maxHeight(30),
                new Bounds(0, 0, 40, 30));
        assertRootBounds(props -> props.minWidth(25).minHeight(20).maxWidth(40).maxHeight(30),
                new Bounds(0, 0, 40, 30));
    }

    @Test
    void rootMinimumsExpandAnIntrinsicChild() {
        var tree = new UITree();
        var spacer = new Spacer(10, 10);
        try {
            ICompositionScope.root(tree).e(spacer,
                    root -> root.layout().minWidth(25).minHeight(20));
            tree.prepareFrame(100, 80, mock(IScreenContext.class));
            assertEquals(new Bounds(0, 0, 25, 20), spacer.getBounds());
        } finally {
            tree.reset();
        }
    }

    private static void assertRootBounds(Consumer<LayoutProperties> configure, Bounds expected) {
        var tree = new UITree();
        var rect = new Rect(0);
        try {
            ICompositionScope.root(tree).e(rect, root -> configure.accept(root.layout()));
            tree.prepareFrame(100, 80, mock(IScreenContext.class));
            assertEquals(expected, rect.getBounds());
            tree.prepareFrame(120, 90, mock(IScreenContext.class));
            if (!rect.getNode().getLayoutProperties().isFillMaxWidth()) {
                assertEquals(expected, rect.getBounds());
            }
        } finally {
            tree.reset();
        }
    }
}
