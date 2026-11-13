package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.animation.Easing;
import com.tridevmc.compound.ui.animation.api.IAnimationTimeline;
import com.tridevmc.compound.ui.element.Box;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.ILazyCollection;
import com.tridevmc.compound.ui.element.ILazySource;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.geometry.api.ITransform2D;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MinecraftMockExtension.class)
class FrameResetLifecycleTest {
    @Test
    void completingAnimationMayCloseItsOwningTreeDuringFramePreparation() {
        var tree = new UITree();
        var context = mock(IScreenContext.class);
        ICompositionScope.root(tree).e(new Box(), scope -> {
            var animation = scope.animateFloat(0, 50, Easing.LINEAR);
            animation.addObserver(ignored -> tree.reset());
            animation.set(1F);
        });
        when(context.getTicks()).thenReturn(0L);
        tree.prepareFrame(100, 100, context);
        when(context.getTicks()).thenReturn(1L);
        assertDoesNotThrow(() -> tree.prepareFrame(100, 100, context));
        assertFalse(tree.hasRoot());
        assertFalse(tree.getAnimationScheduler().hasActiveAnimations());
    }

    @Test
    void geometryResetSkipsDisposedChildrenAndTheirTransformSuppliers() {
        var tree = new UITree();
        var callbacks = new AtomicInteger();
        ICompositionScope.root(tree).e(new Column(), scope -> {
            scope.beforeGeometry(tree::reset);
            scope.e(new Rect(0), child -> {
                child.beforeGeometry(callbacks::incrementAndGet);
                child.transform(() -> {
                    callbacks.incrementAndGet();
                    return ITransform2D.IDENTITY;
                });
            });
        });
        assertDoesNotThrow(() -> tree.prepareFrame(100, 100, mock(IScreenContext.class)));
        assertFalse(tree.hasRoot());
        assertEquals(0, callbacks.get());
    }

    @Test
    void geometryReplacementWaitsUntilTheNextFrameBeforePreparingAndRendering() {
        var tree = new UITree();
        var context = mock(IScreenContext.class);
        var callbacks = new AtomicInteger();
        var replacement = new Rect(0);
        ICompositionScope.root(tree).e(new Box(), scope -> scope.beforeGeometry(() -> {
            tree.reset();
            ICompositionScope.root(tree).e(replacement, next -> {
                next.layout().fixedSize(20, 30);
                next.beforeGeometry(callbacks::incrementAndGet);
            });
        }));
        try {
            assertDoesNotThrow(() -> tree.prepareFrame(100, 100, context));
            assertTrue(tree.hasRoot());
            assertNull(replacement.getBounds());
            assertEquals(0, callbacks.get());
            assertDoesNotThrow(() -> tree.renderTree(context));
            tree.prepareFrame(100, 100, context);
            assertEquals(new Bounds(0, 0, 20, 30), replacement.getBounds());
            assertEquals(1, callbacks.get());
            assertDoesNotThrow(() -> tree.renderTree(context));
        } finally {
            tree.reset();
        }
    }

    @Test
    void queuedCompositionMayCloseItsOwningTree() {
        var tree = new UITree();
        var close = State.of(false);
        var context = mock(IScreenContext.class);
        ICompositionScope.root(tree).e(new Box(), scope -> {
            scope.bind(close);
            if (close.get()) tree.reset();
        });
        tree.prepareFrame(100, 100, context);
        close.set(true);
        assertDoesNotThrow(() -> tree.prepareFrame(100, 100, context));
        assertFalse(tree.hasRoot());
    }

    @Test
    void virtualCellDisposalMayCloseItsOwningTree() {
        this.assertCellDisposalMayCloseTree(false);
    }

    @Test
    void removedSourceCellDisposalMayCloseItsOwningTree() {
        this.assertCellDisposalMayCloseTree(true);
    }

    @Test
    void replacementMountedByFocusLossCannotLeaveTheOriginalTreeUndisposed() {
        var tree = new UITree();
        var original = new Box();
        var replacement = new Box();
        var originalState = spy(State.of(0));
        var replacementState = spy(State.of(0));
        var timeline = IAnimationTimeline.create();
        ICompositionScope.root(tree).e(original, scope -> {
            scope.bind(originalState);
            scope.onFocusLost(() -> ICompositionScope.root(tree).e(replacement, next -> {
                next.bind(replacementState);
                next.useAnimationTimeline(timeline);
                next.animateFloatLooping(0, 1, 50);
            }));
        });
        tree.requestFocus(original.getNode());
        assertDoesNotThrow(tree::reset);
        assertFalse(tree.hasRoot());
        assertNull(original.getNode());
        assertNull(replacement.getNode());
        verify(originalState).removeObserver(any(StateObserver.class));
        verify(replacementState).removeObserver(any(StateObserver.class));
        assertFalse(tree.getAnimationScheduler().hasActiveAnimations());
        ICompositionScope.root(tree).e(new Box(), scope -> scope.useAnimationTimeline(timeline));
        tree.reset();
    }

    @Test
    void replacementMountedByDetachmentCannotSurviveResetWithoutOwnership() {
        var tree = new UITree();
        var replacement = new Box();
        var original = new Box() {
            @Override
            public void onDetached() {
                ICompositionScope.root(tree).e(replacement);
            }
        };
        ICompositionScope.root(tree).e(original);
        assertDoesNotThrow(tree::reset);
        assertFalse(tree.hasRoot());
        assertNull(original.getNode());
        assertNull(replacement.getNode());
    }

    private void assertCellDisposalMayCloseTree(boolean sourceRemoval) {
        var tree = new UITree();
        var context = mock(IScreenContext.class);
        var closed = new AtomicBoolean();
        var source = ILazySource.mutable(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), item -> item);
        var collection = ILazyCollection.list(source, 20, 20, 0, (item, cell) ->
                cell.e(new Rect(0) {
                    @Override
                    public void onDetached() {
                        if (item == 0 && closed.compareAndSet(false, true)) tree.reset();
                    }
                }, rect -> rect.layout().fixedSize(20, 20)));
        var area = new ScrollArea().showScrollbar(false);
        ICompositionScope.root(tree).e(area, scroll -> {
            scroll.layout().fillMax();
            scroll.fillSlot(ScrollArea.CONTENT_SLOT, body -> body.e(collection));
        });
        tree.prepareFrame(100, 100, context);
        if (sourceRemoval) source.remove(0);
        else area.scrollTo(0, 100);
        assertDoesNotThrow(() -> tree.prepareFrame(100, 100, context));
        assertTrue(closed.get());
        assertFalse(tree.hasRoot());
    }
}
