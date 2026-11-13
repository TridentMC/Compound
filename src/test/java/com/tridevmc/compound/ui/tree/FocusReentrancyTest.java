package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.ILazyCollection;
import com.tridevmc.compound.ui.element.ILazySource;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.Row;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MinecraftMockExtension.class)
class FocusReentrancyTest {
    @Test
    void lostCallbackRedirectsFocusWithoutAnUnbalancedIntermediateGain() {
        try (var fixture = new Fixture()) {
            fixture.tree.requestFocus(fixture.first.getNode());
            fixture.events.clear();
            fixture.first.getNode().addFocusLostHandler(() ->
                    fixture.tree.requestFocus(fixture.redirected.getNode()));

            fixture.tree.requestFocus(fixture.requested.getNode());

            assertSame(fixture.redirected.getNode(), fixture.tree.getFocusedNode());
            assertEquals(List.of("first lost", "redirected gained"), fixture.events);
        }
    }

    @Test
    void lostCallbackCanClearTheFocusRequestWhileNoNodeIsFocused() {
        try (var fixture = new Fixture()) {
            fixture.tree.requestFocus(fixture.first.getNode());
            fixture.events.clear();
            fixture.first.getNode().addFocusLostHandler(fixture.tree::clearFocus);

            fixture.tree.requestFocus(fixture.requested.getNode());

            assertNull(fixture.tree.getFocusedNode());
            assertEquals(List.of("first lost"), fixture.events);
        }
    }

    @Test
    void gainedCallbackRedirectsAfterTheCurrentNotificationBatch() {
        try (var fixture = new Fixture()) {
            fixture.requested.getNode().addFocusGainedHandler(() ->
                    fixture.tree.requestFocus(fixture.redirected.getNode()));
            fixture.requested.getNode().addFocusGainedHandler(() ->
                    fixture.events.add("requested second gained"));

            fixture.tree.requestFocus(fixture.requested.getNode());

            assertSame(fixture.redirected.getNode(), fixture.tree.getFocusedNode());
            assertEquals(List.of("requested gained", "requested second gained",
                    "requested lost", "redirected gained"), fixture.events);
        }
    }

    @Test
    void idempotentRequestFromGainedCallbackRetainsNarration() {
        try (var fixture = new Fixture()) {
            var requested = fixture.requested.getNode();
            requested.addFocusGainedHandler(() -> fixture.tree.requestFocus(requested));
            var narrator = Minecraft.getInstance().getNarrator();
            clearInvocations(narrator);

            fixture.tree.requestFocus(requested);

            assertSame(requested, fixture.tree.getFocusedNode());
            assertEquals(List.of("requested gained"), fixture.events);
            verify(narrator).saySystemNow(any(Component.class));
        }
    }

    @Test
    void resetFromLostCallbackCancelsTheOuterTarget() {
        try (var fixture = new Fixture()) {
            fixture.tree.requestFocus(fixture.first.getNode());
            fixture.events.clear();
            fixture.first.getNode().addFocusLostHandler(fixture.tree::reset);

            fixture.tree.requestFocus(fixture.requested.getNode());

            assertNull(fixture.tree.getRoot());
            assertNull(fixture.tree.getFocusedNode());
            assertEquals(List.of("first lost"), fixture.events);
        }
    }

    @Test
    void resetFromGainedCallbackStopsStaleGainsNarrationAndDisposalRequests() {
        try (var fixture = new Fixture()) {
            var requested = fixture.requested.getNode();
            var redirected = fixture.redirected.getNode();
            requested.onDispose(() -> fixture.tree.requestFocus(redirected));
            requested.addFocusGainedHandler(fixture.tree::reset);
            requested.addFocusGainedHandler(() -> fixture.events.add("stale gained"));
            var narrator = Minecraft.getInstance().getNarrator();
            clearInvocations(narrator);

            fixture.tree.requestFocus(requested);

            assertNull(fixture.tree.getRoot());
            assertNull(fixture.tree.getFocusedNode());
            assertEquals(List.of("requested gained", "requested lost"), fixture.events);
            verify(narrator, never()).saySystemNow(any(Component.class));
        }
    }

    @Test
    void detachmentFromGainedCallbackNotifiesLossBeforeOwnedResourcesDispose() {
        try (var fixture = new Fixture()) {
            var requested = fixture.requested.getNode();
            var disposed = new AtomicInteger();
            requested.onDispose(() -> {
                assertNull(fixture.tree.getFocusedNode());
                disposed.incrementAndGet();
            });
            requested.addFocusGainedHandler(() -> fixture.tree.detachNode(requested));
            requested.addFocusGainedHandler(() -> fixture.events.add("stale gained"));

            fixture.tree.requestFocus(requested);

            assertNull(fixture.tree.getFocusedNode());
            assertEquals(1, disposed.get());
            assertEquals(List.of("requested gained", "requested lost"), fixture.events);
        }
    }

    @Test
    void cyclicConsumerFocusCallbacksFailWithABoundedDiagnostic() {
        try (var fixture = new Fixture()) {
            fixture.first.getNode().addFocusGainedHandler(() ->
                    fixture.tree.requestFocus(fixture.requested.getNode()));
            fixture.requested.getNode().addFocusGainedHandler(() ->
                    fixture.tree.requestFocus(fixture.first.getNode()));

            var error = assertThrows(IllegalStateException.class, () ->
                    fixture.tree.requestFocus(fixture.first.getNode()));

            assertEquals("Focus requests did not settle in 64 transitions", error.getMessage());
        }
    }

    @Test
    void reentrantFocusTransferReleasesTheOldOffscreenLazyCell() {
        var tree = new UITree();
        var area = new ScrollArea().showScrollbar(false);
        var source = ILazySource.mutable(IntStream.range(0, 20).boxed().toList(), item -> item);
        var collection = ILazyCollection.list(source, 20, 20, 0, (item, cell) ->
                cell.e(new Rect(0), rect -> rect.layout().fixedSize(20, 20)));
        var context = mock(IScreenContext.class);
        try {
            ICompositionScope.root(tree).e(area, scroll -> {
                scroll.layout().fixedSize(100, 60);
                scroll.fillSlot(ScrollArea.CONTENT_SLOT, body -> body.e(collection));
            });
            tree.prepareFrame(100, 60, context);
            var host = tree.getNodeForElement(collection);
            var old = host.getChildren().getFirst();
            tree.requestFocus(old);
            area.scrollTo(0, 200);
            tree.prepareFrame(100, 60, context);
            assertEquals(4, host.getChildCount());
            var redirected = host.getChildren().get(1);
            var requested = host.getChildren().get(2);
            old.addFocusLostHandler(() -> tree.requestFocus(redirected));

            tree.requestFocus(requested);
            tree.prepareFrame(100, 60, context);

            assertSame(redirected, tree.getFocusedNode());
            assertEquals(3, host.getChildCount());
            assertFalse(host.getChildren().contains(old));
        } finally {
            tree.reset();
        }
    }

    private static Rect named(String name) {
        return new Rect(0) {
            @Override public Component getNarrationMessage() { return Component.literal(name); }
        };
    }

    private static final class Fixture implements AutoCloseable {
        private final UITree tree = new UITree();
        private final Rect first = named("first");
        private final Rect requested = named("requested");
        private final Rect redirected = named("redirected");
        private final List<String> events = new ArrayList<>();

        Fixture() {
            ICompositionScope.root(this.tree).e(new Row(), row -> {
                row.e(this.first, child -> child.layout().fixedSize(20, 20));
                row.e(this.requested, child -> child.layout().fixedSize(20, 20));
                row.e(this.redirected, child -> child.layout().fixedSize(20, 20));
            });
            this.tree.prepareFrame(100, 100, mock(IScreenContext.class));
            for (var element : List.of(this.first, this.requested, this.redirected)) {
                var name = element.getNarrationMessage().getString();
                element.getNode().addFocusGainedHandler(() -> this.events.add(name + " gained"));
                element.getNode().addFocusLostHandler(() -> this.events.add(name + " lost"));
            }
        }

        @Override public void close() { this.tree.reset(); }
    }
}
