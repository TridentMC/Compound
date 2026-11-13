package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.geometry.api.ITransform2D;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MinecraftMockExtension.class)
class RenderCullingTest {
    @Test
    void offscreenAndTouchingEdgeHaveZeroDrawsPartialHasScissor() {
        for (int offset : new int[]{100, 120, 95}) {
            var count = new AtomicInteger();
            var tree = new UITree();
            ICompositionScope.root(tree).e(new Stack(), root -> {
                root.layout().fixedSize(100, 100).clip();
                root.e(primitive(count), rect -> rect.layout().fixedSize(10, 10).margin(offset, 0, 0, 0));
            });
            var context = mock(IScreenContext.class);
            tree.layoutAndRender(100, 100, context);
            assertEquals(offset == 95 ? 1 : 0, count.get());
            if (offset == 95) {
                verify(context).enableScissor(0, 0, 100, 100);
                verify(context).disableScissor();
            } else verify(context, never()).disableScissor();
        }
    }

    @Test
    void descendantCanTransformIntoClipAndOrdinaryLayerCanEscape() {
        var transformed = new AtomicInteger();
        var overlay = new AtomicInteger();
        var tree = new UITree();
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fixedSize(100, 100).clip();
            root.e(new Stack(), parent -> {
                parent.layout().fixedSize(10, 10).margin(200, 0, 0, 0);
                parent.e(primitive(transformed), child -> {
                    child.layout().fixedSize(10, 10);
                    child.transform(() -> ITransform2D.translation(-200, 0));
                });
                parent.e(primitive(overlay), child -> child.layout().fixedSize(10, 10).layer(1));
            });
        });
        tree.layoutAndRender(100, 100, mock(IScreenContext.class));
        assertEquals(1, transformed.get());
        assertEquals(0, overlay.get());
        org.junit.jupiter.api.Assertions.assertNull(tree.findNodeAt(205, 5));
    }

    @Test
    void screenClipsUnclippedTreesAndOverlaysButAllowsAncestorClipEscape() {
        var offscreen = new AtomicInteger();
        var escape = new AtomicInteger();
        var tree = new UITree();
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fillMax();
            root.e(primitive(offscreen), child -> child.layout().fixedSize(10, 10).margin(110, 0, 0, 0));
            root.e(new Stack(), clipped -> {
                clipped.layout().fixedSize(10, 10).clip();
                clipped.e(primitive(escape), child -> child.layout().fixedSize(10, 10).margin(30, 0, 0, 0).layer(1));
                clipped.e(primitive(offscreen), child -> child.layout().fixedSize(10, 10).margin(110, 0, 0, 0).layer(2));
            });
        });
        tree.layoutAndRender(100, 100, mock(IScreenContext.class));
        assertEquals(0, offscreen.get());
        assertEquals(1, escape.get());
        org.junit.jupiter.api.Assertions.assertNull(tree.findNodeAt(115, 5));
        org.junit.jupiter.api.Assertions.assertNotNull(tree.findNodeAt(35, 5));
    }

    @Test
    void nestedDisjointClipsAreEmptyEvenWhenChildSpansBoth() {
        var count = new AtomicInteger();
        var tree = new UITree();
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fixedSize(100, 100).clip();
            root.e(new Stack(), nested -> {
                nested.layout().fixedSize(100, 100).margin(150, 0, 0, 0).clip();
                nested.e(primitive(count), rect -> rect.layout().fixedSize(300, 20).margin(-200, 0, 0, 0));
            });
        });
        tree.layoutAndRender(100, 100, mock(IScreenContext.class));
        assertEquals(0, count.get());
    }

    @Test
    void lazyAncestorTransformSamplesOnceAfterBeforeGeometry() {
        var tree = new UITree();
        var samples = new AtomicInteger();
        var prepared = new AtomicInteger();
        var collection = com.tridevmc.compound.ui.element.ILazyCollection.list(
                com.tridevmc.compound.ui.element.ILazySource.immutable(50_000, i -> i, i -> i),
                20, 20, 0, (item, cell) -> cell.e(new Rect(0)));
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fillMax();
            root.beforeGeometry(prepared::incrementAndGet);
            root.transform(() -> {
                assertEquals(prepared.get(), samples.incrementAndGet());
                return ITransform2D.translation(0, 0);
            });
            root.e(collection);
        });
        tree.prepareFrame(100, 100, mock(IScreenContext.class));
        tree.prepareFrame(100, 100, mock(IScreenContext.class));
        assertEquals(2, samples.get());
        assertEquals(2, prepared.get());
    }

    private static Rect primitive(AtomicInteger count) {
        return new Rect(0) {
            @Override protected void drawElement(IScreenContext context, Bounds bounds) {
                count.incrementAndGet();
            }
        };
    }
}
