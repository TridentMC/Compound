package com.tridevmc.compound.ui.tree;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.event.KeyInputEvent;
import com.tridevmc.compound.ui.geometry.api.ITransform2D;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.Position;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class TabFocusGeometryTest {
    @Test
    void tabSkipsAControlTransformedEntirelyOutsideItsClip() {
        var tree = new UITree();
        var clipped = focusable();
        var visible = focusable();
        try {
            ICompositionScope.root(tree).e(new Stack(), root -> {
                root.layout().fillMax().clip();
                root.e(clipped, child -> {
                    child.layout().fixedSize(20, 20);
                    child.transform(() -> ITransform2D.translation(200, 0));
                });
                root.e(visible, child -> child.layout().fixedSize(20, 20));
            });
            prepare(tree);
            tab(tree, false);
            assertSame(visible.getNode(), tree.getFocusedNode());
        } finally {
            tree.reset();
        }
    }

    @Test
    void controlTranslatedIntoTheClipRemainsEligible() {
        var tree = new UITree();
        var visible = focusable();
        try {
            ICompositionScope.root(tree).e(new Column(), root -> {
                root.layout().fillMax().clip();
                root.e(new Rect(0), spacer -> spacer.layout().fixedSize(20, 120));
                root.e(visible, child -> {
                    child.layout().fixedSize(20, 20);
                    child.transform(() -> ITransform2D.translation(0, -120));
                });
            });
            prepare(tree);
            tab(tree, false);
            assertSame(visible.getNode(), tree.getFocusedNode());
        } finally {
            tree.reset();
        }
    }

    @Test
    void singularControlIsSkippedAndReflectedVisibleControlIsEligible() {
        var tree = new UITree();
        var singular = focusable();
        var reflected = focusable();
        try {
            ICompositionScope.root(tree).e(new Stack(), root -> {
                root.layout().fillMax().clip();
                root.e(singular, child -> {
                    child.layout().fixedSize(20, 20);
                    child.transform(() -> ITransform2D.of(10, 0, 0, 0, 1, 0, 0));
                });
                root.e(reflected, child -> {
                    child.layout().fixedSize(20, 20);
                    child.transform(() -> ITransform2D.of(50, 0, 0, -1, 1, 0, 0));
                });
            });
            prepare(tree);
            tab(tree, false);
            assertSame(reflected.getNode(), tree.getFocusedNode());
        } finally {
            tree.reset();
        }
    }

    @Test
    void traversalUsesTheLatestFrameWhenAnimationMovesAControlOffscreen() {
        var tree = new UITree();
        var first = focusable();
        var second = focusable();
        var transform = new AtomicReference<>(ITransform2D.translation(0, 0));
        try {
            ICompositionScope.root(tree).e(new Stack(), root -> {
                root.layout().fillMax();
                root.e(first, child -> {
                    child.layout().fixedSize(20, 20);
                    child.transform(transform::get);
                });
                root.e(second, child -> child.layout().fixedSize(20, 20));
            });
            prepare(tree);
            tab(tree, false);
            assertSame(first.getNode(), tree.getFocusedNode());

            transform.set(ITransform2D.translation(200, 0));
            prepare(tree);
            tab(tree, false);
            assertSame(second.getNode(), tree.getFocusedNode());
            tab(tree, true);
            assertSame(second.getNode(), tree.getFocusedNode());
        } finally {
            tree.reset();
        }
    }

    @Test
    void modalOverlayUsesItsEffectiveClipInsteadOfItsClippedAncestors() {
        var tree = new UITree();
        var overlay = new Stack();
        var visible = focusable();
        try {
            ICompositionScope.root(tree).e(new Stack(), root -> {
                root.layout().fillMax().clip();
                root.e(new Stack(), host -> {
                    host.layout().fixedSize(20, 20).clip();
                    host.e(overlay, modal -> {
                        modal.layout().fixedSize(20, 20).layer(10);
                        modal.e(visible, child -> {
                            child.layout().fixedSize(20, 20);
                            child.transform(() -> ITransform2D.translation(50, 0));
                        });
                    });
                });
            });
            prepare(tree);
            tree.setInputRoot(overlay.getNode());
            tab(tree, false);
            assertSame(visible.getNode(), tree.getFocusedNode());
        } finally {
            tree.reset();
        }
    }

    @Test
    void manualLayoutBeforeTheFirstGeometrySnapshotRetainsFocusTraversal() {
        var tree = new UITree();
        var visible = focusable();
        try {
            ICompositionScope.root(tree).e(new Stack(), root -> {
                root.layout().fixedSize(100, 100).clip();
                root.e(visible, child -> child.layout().fixedSize(20, 20));
            });
            tree.measureTree(Constraints.loose(100, 100));
            tree.placeTree(Position.ORIGIN);
            tab(tree, false);
            assertSame(visible.getNode(), tree.getFocusedNode());
        } finally {
            tree.reset();
        }
    }

    private static Rect focusable() {
        return new Rect(0) {
            @Override public boolean isFocusable() { return true; }
        };
    }

    private static void prepare(UITree tree) {
        tree.prepareFrame(100, 100, mock(IScreenContext.class));
    }

    private static void tab(UITree tree, boolean backwards) {
        assertTrue(tree.dispatchKeyPress(new KeyInputEvent(
                InputConstants.KEY_TAB, 0, backwards, false, false)));
    }
}
