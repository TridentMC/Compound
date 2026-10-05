package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.geometry.api.ITransform2D;
import com.tridevmc.compound.ui.geometry.api.WorldPoint;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseDragEvent;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import org.joml.Matrix3x2fStack;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TreeGeometryTest {
    private IScreenContext context() {
        var context = mock(IScreenContext.class);
        when(context.getActiveStack()).thenReturn(new Matrix3x2fStack(16));
        return context;
    }

    @Test
    void nestedRotationUsesOneSnapshotForRenderHitAndLocalConversion() {
        var tree = new UITree();
        var rect = new Rect(0xFFFFFFFF);
        var samples = new AtomicInteger();
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fillMax();
            root.transform(() -> ITransform2D.translation(50.25, 20.5));
            root.e(rect, leaf -> {
                leaf.layout().fixedSize(20, 20);
                leaf.transform(() -> {
                    samples.incrementAndGet();
                    return ITransform2D.rotation(Math.PI / 4, 10, 10);
                });
            });
        });
        var context = context();
        tree.prepareFrame(200, 200, context);
        var node = tree.getNodeForElement(rect);
        var geometry = node.getFrameGeometry();
        assertSame(node, tree.findNodeAt(60, 30));
        assertNotSame(node, tree.findNodeAt(46, 16));
        var local = geometry.toLocal(60.25, 30.5).orElseThrow();
        assertEquals(10, local.x(), 1e-8);
        assertEquals(10, local.y(), 1e-8);
        tree.renderTree(context);
        tree.findNodeAt(60, 30);
        assertEquals(1, samples.get());
        assertSame(geometry, node.getFrameGeometry());
    }

    @Test
    void stationaryHoverMovesWithoutLayoutAndSingularTransformsCannotHit() {
        var tree = new UITree();
        var rect = new Rect(0);
        var x = new double[]{0};
        var enter = new AtomicInteger();
        var exit = new AtomicInteger();
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fillMax();
            root.e(rect, leaf -> {
                leaf.layout().fixedSize(20, 20);
                leaf.transform(() -> ITransform2D.translation(x[0], 0));
                leaf.onMouseEnter(enter::incrementAndGet);
                leaf.onMouseExit(exit::incrementAndGet);
            });
        });
        var context = context();
        tree.prepareFrame(200, 200, context);
        var size = tree.getNodeForElement(rect).getMeasuredSize();
        assertEquals(1, enter.get());
        x[0] = 100;
        tree.prepareFrame(200, 200, context);
        assertEquals(1, exit.get());
        assertSame(size, tree.getNodeForElement(rect).getMeasuredSize());
        tree.getNodeForElement(rect).transform(() -> ITransform2D.of(0, 0, 0, 0, 1, 0, 0));
        tree.prepareFrame(200, 200, context);
        assertTrue(tree.getNodeForElement(rect).getFrameGeometry().toLocal(0, 0).isEmpty());
        assertNotSame(tree.getNodeForElement(rect), tree.findNodeAt(0, 0));
    }

    @Test
    void overlayEscapesClipButKeepsInheritedTransform() {
        var tree = new UITree();
        var normal = new Rect(0);
        var overlay = new Rect(0);
        ICompositionScope.root(tree).e(new Stack(), screen -> screen.e(new Stack(), root -> {
            root.layout().fixedSize(20, 20).clip();
            root.transform(() -> ITransform2D.translation(10, 10));
            root.e(normal, leaf -> {
                leaf.layout().fixedSize(10, 10);
                leaf.transform(() -> ITransform2D.translation(25, 0));
            });
            root.e(overlay, leaf -> {
                leaf.layout().fixedSize(10, 10).layer(1);
                leaf.transform(() -> ITransform2D.translation(25, 0));
            });
        }));
        tree.prepareFrame(100, 100, context());
        assertSame(tree.getNodeForElement(overlay), tree.findNodeAt(36, 11));
        assertNull(tree.getNodeForElement(overlay).getFrameGeometry().clip());
        assertFalse(tree.getNodeForElement(normal).getFrameGeometry().contains(36, 11));
        assertEquals(35, tree.getNodeForElement(overlay).getFrameGeometry().left());
    }

    @Test
    void capturedCallbacksStayWorldBasedAndTypedConversionUsesCurrentSnapshot() {
        var tree = new UITree();
        var rect = new Rect(0);
        var received = new double[3];
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fillMax();
            root.e(rect, leaf -> {
                leaf.layout().fixedSize(20, 20);
                leaf.transform(() -> ITransform2D.translation(40.5, 30));
                leaf.onClick(event -> true);
                leaf.onMouseDrag(event -> {
                    received[0] = event.x();
                    var local = leaf.toLocal(new WorldPoint(event.x(), event.y())).orElseThrow();
                    received[1] = local.x();
                    received[2] = local.y();
                    return true;
                });
            });
        });
        tree.prepareFrame(200, 200, context());
        assertTrue(tree.dispatchClick(45, 35, new MouseClickEvent(
                45, 35, 0, false, false, false)));
        assertTrue(tree.dispatchMouseDrag(190, 190, new MouseDragEvent(
                0, 190, 190, 145, 155)));
        assertEquals(190, received[0]);
        assertEquals(149.5, received[1]);
        assertEquals(160, received[2]);
    }

    @Test
    void rotatedScissorIsRejectedRatherThanApproximated() {
        var tree = new UITree();
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fillMax().clip();
            root.transform(() -> ITransform2D.rotation(.2, 0, 0));
        });
        assertThrows(UnsupportedOperationException.class, () -> tree.prepareFrame(100, 100, context()));
    }
}
