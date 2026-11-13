package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Box;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MinecraftMockExtension.class)
class DeferredLayoutLifecycleTest {
    @Test
    void lateDependenciesBelongToTheirOriginalCompositionUntilDetachment() {
        var tree = new UITree();
        var enabled = State.of(false);
        var extra = spy(State.of(0));
        var calls = new AtomicInteger();
        var box = new Box();
        var context = mock(IScreenContext.class);
        try {
            ICompositionScope.root(tree).e(new Column(), root -> root.e(box, child ->
                    child.layout().deferred(deferred -> {
                        deferred.bind(enabled);
                        if (enabled.get()) deferred.bind(extra);
                        calls.incrementAndGet();
                        deferred.layout().fixedSize(extra.get(), 10);
                    })));
            tree.prepareFrame(100, 100, context);
            for (int pass = 1; pass <= 3; pass++) {
                enabled.set(true);
                tree.requestRecompose(box.getNode());
                tree.prepareFrame(100, 100, context);
                calls.set(0);
                extra.set(pass);
                assertEquals(1, calls.get());
                tree.prepareFrame(100, 100, context);
                assertEquals(pass, box.getBounds().width());
                enabled.set(false);
                tree.requestRecompose(box.getNode());
                tree.prepareFrame(100, 100, context);
                calls.set(0);
                extra.set(10 + pass);
                assertEquals(0, calls.get());
                verify(extra, times(2 * pass)).removeObserver(any(StateObserver.class));
            }
            enabled.set(true);
            tree.detachNode(box.getNode());
            calls.set(0);
            enabled.set(false);
            extra.set(99);
            assertEquals(0, calls.get());
            verify(extra, times(7)).removeObserver(any(StateObserver.class));
        } finally {
            tree.reset();
        }
    }

    @Test
    void consumerScopesCreatedBeforeAndAfterCompositionSurviveRecomposition() {
        var tree = new UITree();
        var before = State.of(20);
        var after = State.of(30);
        var callsBefore = new AtomicInteger();
        var callsAfter = new AtomicInteger();
        var area = new ScrollArea().showScrollbar(false);
        var context = mock(IScreenContext.class);
        try {
            ICompositionScope.root(tree).e(new Column(), root -> root.e(area, scroll ->
                    scroll.layout().deferred(deferred -> {
                        deferred.bind(before);
                        callsBefore.incrementAndGet();
                        deferred.layout().fixedWidth(before.get());
                    })));
            area.getNode().getLayoutProperties().deferred(deferred -> {
                deferred.bind(after);
                callsAfter.incrementAndGet();
                deferred.layout().fixedHeight(after.get());
            });
            for (int pass = 1; pass <= 3; pass++) {
                tree.requestRecompose(area.getNode());
                tree.prepareFrame(100, 100, context);
                callsBefore.set(0);
                callsAfter.set(0);
                before.set(20 + pass);
                after.set(30 + pass);
                assertEquals(1, callsBefore.get());
                assertEquals(1, callsAfter.get());
                tree.prepareFrame(100, 100, context);
                assertEquals(20 + pass, area.getBounds().width());
                assertEquals(30 + pass, area.getBounds().height());
            }
            tree.detachNode(area.getNode());
            callsBefore.set(0);
            callsAfter.set(0);
            before.set(99);
            after.set(99);
            assertEquals(0, callsBefore.get());
            assertEquals(0, callsAfter.get());
        } finally {
            tree.reset();
        }
    }

    @Test
    void disposedScopeDoesNotRunFromAnInFlightObserverSnapshot() {
        var tree = new UITree();
        var state = State.of(0);
        var calls = new AtomicInteger();
        state.addObserver(ignored -> tree.recomposeNode(tree.getRoot()));
        try {
            ICompositionScope.root(tree).e(new Box(), box -> box.layout().deferred(deferred -> {
                deferred.bind(state);
                calls.incrementAndGet();
            }));
            for (int pass = 1; pass <= 3; pass++) {
                calls.set(0);
                state.set(pass);
                assertEquals(1, calls.get());
            }
        } finally {
            tree.reset();
        }
    }
}
