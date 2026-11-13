package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.ILazyCollection;
import com.tridevmc.compound.ui.element.ILazySource;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.visual.BufferedImageScreenContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MinecraftMockExtension.class)
class OverlayOrderingTest {
    private static final int RED = 0xFFCC0000;
    private static final int GREEN = 0xFF00CC00;

    @Test
    void nestedHigherLayerRendersAndHitsAboveLowerLayerSiblingOnce() {
        var tree = new UITree();
        var draws = new AtomicInteger();
        var high = countedRect(RED, draws);
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fillMax();
            root.e(new Stack(), parent -> {
                parent.layout().fixedSize(50, 50).layer(1);
                parent.e(high, child -> child.layout().fixedSize(50, 50).layer(100));
            });
            root.e(new Rect(GREEN), sibling -> sibling.layout().fixedSize(50, 50).layer(2));
        });
        try (var context = new BufferedImageScreenContext(100, 100)) {
            tree.layoutAndRender(100, 100, context);
            assertEquals(RED, context.getImage().getRGB(5, 5));
            assertSame(high, tree.findNodeAt(5, 5).getElement());
            assertEquals(1, draws.get());
        } finally {
            tree.reset();
        }
    }

    @Test
    void equalLayersRetainPreorderAndLaterSiblingWins() {
        var tree = new UITree();
        var later = new Rect(GREEN);
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fillMax();
            root.e(new Stack(), parent -> {
                parent.layout().fixedSize(50, 50).layer(1);
                parent.e(new Rect(RED), child -> child.layout().fixedSize(50, 50).layer(5));
            });
            root.e(later, sibling -> sibling.layout().fixedSize(50, 50).layer(5));
        });
        try (var context = new BufferedImageScreenContext(100, 100)) {
            tree.layoutAndRender(100, 100, context);
            assertEquals(GREEN, context.getImage().getRGB(5, 5));
            assertSame(later, tree.findNodeAt(5, 5).getElement());
        } finally {
            tree.reset();
        }
    }

    @Test
    void nestedOverlayEscapesAncestorClipButHiddenAncestorStillSuppressesIt() {
        for (boolean visible : new boolean[]{true, false}) {
            var tree = new UITree();
            var overlay = new Rect(RED);
            ICompositionScope.root(tree).e(new Stack(), root -> {
                root.layout().fillMax().clip();
                root.e(new Stack() {
                    @Override
                    public boolean isVisible() {
                        return visible;
                    }
                }, parent -> {
                    parent.layout().fixedSize(10, 10).clip().layer(1);
                    parent.e(overlay, child -> child.layout().fixedSize(10, 10)
                            .margin(30, 0, 0, 0).layer(100));
                });
            });
            try (var context = new BufferedImageScreenContext(100, 100)) {
                tree.layoutAndRender(100, 100, context);
                assertEquals(visible ? RED : 0, context.getImage().getRGB(35, 5));
                if (visible) assertSame(overlay, tree.findNodeAt(35, 5).getElement());
                else assertNotSame(overlay, tree.findNodeAt(35, 5).getElement());
            } finally {
                tree.reset();
            }
        }
    }

    @Test
    void layeredLazyCellContentRemainsInsideItsCellClip() {
        var tree = new UITree();
        var overflow = new Rect(RED);
        var collection = ILazyCollection.list(ILazySource.immutable(1, index -> index, index -> index),
                20, 20, 0, (item, cell) -> cell.e(new Stack(), layered -> {
                    layered.layout().fixedSize(20, 20).layer(1);
                    layered.e(overflow, rect -> rect.layout().fixedSize(60, 20).layer(100));
                }));
        ICompositionScope.root(tree).e(new ScrollArea().showScrollbar(false), root -> {
            root.layout().fillMax();
            root.fillSlot(ScrollArea.CONTENT_SLOT, body -> body.e(collection));
        });
        try (var context = new BufferedImageScreenContext(100, 100)) {
            tree.layoutAndRender(100, 100, context);
            assertEquals(RED, context.getImage().getRGB(5, 5));
            assertEquals(0, context.getImage().getRGB(25, 5));
            assertSame(overflow, tree.findNodeAt(5, 5).getElement());
            assertNotSame(overflow, tree.findNodeAt(25, 5).getElement());
        } finally {
            tree.reset();
        }
    }

    private static Rect countedRect(int colour, AtomicInteger draws) {
        return new Rect(colour) {
            @Override
            protected void drawElement(IScreenContext context, Bounds bounds) {
                draws.incrementAndGet();
                super.drawElement(context, bounds);
            }
        };
    }
}
