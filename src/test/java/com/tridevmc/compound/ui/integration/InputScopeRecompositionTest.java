package com.tridevmc.compound.ui.integration;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Button;
import com.tridevmc.compound.ui.element.Modal;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.event.KeyInputEvent;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.CharEvent;
import com.tridevmc.compound.ui.event.MouseDragEvent;
import com.tridevmc.compound.ui.event.MouseReleaseEvent;
import com.tridevmc.compound.ui.event.MouseScrollEvent;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.ITreeNode;
import com.tridevmc.compound.ui.tree.UITree;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class InputScopeRecompositionTest {
    @Test
    void hiddenModalScopeDoesNotConsumeUnhandledBackgroundInput() {
        var tree = new UITree();
        var visible = new AtomicBoolean(true);
        var backgroundActions = new AtomicInteger();
        tree.setViewportSize(400, 300);
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fillMax();
            root.e(new Rect(0), background -> {
                background.layout().fixedSize(20, 20);
                background.onClick(event -> {
                    backgroundActions.incrementAndGet();
                    return true;
                });
            });
            root.e(new Stack() {
                @Override public boolean isVisible() { return visible.get(); }
            }, host -> {
                host.layout().fillMax();
                host.e(new Modal(Component.literal("Hidden through ancestor")));
            });
        });
        try {
            var context = mock(IScreenContext.class);
            tree.prepareFrame(400, 300, context);
            visible.set(false);
            tree.prepareFrame(400, 300, context);

            assertFalse(tree.dispatchKeyPress(new KeyInputEvent(
                    InputConstants.KEY_ESCAPE, 0, false, false, false)));
            assertFalse(tree.dispatchKeyRelease(new KeyInputEvent(
                    InputConstants.KEY_ESCAPE, 0, false, false, false)));
            assertFalse(tree.dispatchCharTyped(new CharEvent('x', 0)));
            assertFalse(tree.dispatchScroll(399, 299, new MouseScrollEvent(399, 299, -1)));
            assertFalse(tree.dispatchClick(399, 299, new MouseClickEvent(
                    399, 299, InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
            assertFalse(tree.dispatchMouseDrag(399, 299, new MouseDragEvent(
                    InputConstants.MOUSE_BUTTON_LEFT, 399, 299, 1, 1)));
            assertFalse(tree.dispatchMouseRelease(399, 299, new MouseReleaseEvent(
                    399, 299, InputConstants.MOUSE_BUTTON_LEFT)));
            assertTrue(tree.dispatchClick(5, 5, new MouseClickEvent(
                    5, 5, InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
            assertEquals(1, backgroundActions.get());
        } finally {
            tree.reset();
        }
    }

    @Test
    void recomposingLowerModalKeepsTopModalFocusedAndInteractive() {
        try (var fixture = new Fixture()) {
            fixture.open(fixture.first);
            fixture.open(fixture.second);
            var topAction = fixture.action(fixture.second);
            fixture.tree.requestFocus(topAction);
            for (var pass = 0; pass < 3; pass++) {
                fixture.first.setTitle(Component.literal("Lower updated " + pass));
                fixture.prepare();
                assertSame(topAction, fixture.tree.getFocusedNode());
                fixture.click(topAction);
            }
            assertEquals(0, fixture.firstActions.get());
            assertEquals(3, fixture.secondActions.get());
        }
    }

    @Test
    void showingLowerSiblingLaterDoesNotStealInputFromThePaintedTopModal() {
        try (var fixture = new Fixture()) {
            fixture.open(fixture.second);
            var topAction = fixture.action(fixture.second);
            fixture.tree.requestFocus(topAction);
            fixture.open(fixture.first);

            assertSame(topAction, fixture.tree.getFocusedNode());
            fixture.click(topAction);
            assertEquals(0, fixture.firstActions.get());
            assertEquals(1, fixture.secondActions.get());
        }
    }

    @Test
    void closingTopThenLowerRestoresTheirPreviousFocus() {
        try (var fixture = new Fixture()) {
            fixture.tree.requestFocus(fixture.background.getNode());
            fixture.open(fixture.first);
            fixture.tree.requestFocus(fixture.first.getNode());
            fixture.open(fixture.second);
            fixture.tree.requestFocus(fixture.action(fixture.second));

            fixture.second.close();
            assertSame(fixture.first.getNode(), fixture.tree.getFocusedNode());
            fixture.first.close();
            assertSame(fixture.background.getNode(), fixture.tree.getFocusedNode());
        }
    }

    @Test
    void closingLowerWhileTopIsOpenRepairsTheRemainingFocusHistory() {
        try (var fixture = new Fixture()) {
            fixture.tree.requestFocus(fixture.background.getNode());
            fixture.open(fixture.first);
            fixture.tree.requestFocus(fixture.first.getNode());
            fixture.open(fixture.second);
            var topAction = fixture.action(fixture.second);
            fixture.tree.requestFocus(topAction);
            for (var pass = 0; pass < 3; pass++) {
                fixture.first.setMessage(Component.literal("Updated lower body " + pass));
                fixture.prepare();
            }

            fixture.first.close();
            assertSame(topAction, fixture.tree.getFocusedNode());
            fixture.second.close();
            assertSame(fixture.background.getNode(), fixture.tree.getFocusedNode());
        }
    }

    @Test
    void directLayerChangeRefreshesScopePriorityOnTheNextFrame() {
        try (var fixture = new Fixture()) {
            fixture.open(fixture.first);
            fixture.open(fixture.second);
            fixture.tree.requestFocus(fixture.action(fixture.second));
            fixture.first.getNode().getLayoutProperties().layer(250);
            fixture.prepare();

            fixture.click(fixture.action(fixture.first));
            assertEquals(1, fixture.firstActions.get());
            assertEquals(0, fixture.secondActions.get());
        }
    }

    @Test
    void modalComposedDuringResetCannotLeaveADiscardedInputScope() {
        try (var fixture = new Fixture()) {
            fixture.tree.requestFocus(fixture.background.getNode());
            fixture.background.getNode().addFocusLostHandler(() ->
                    ICompositionScope.root(fixture.tree).e(new Modal(Component.literal("Discarded"))));

            fixture.tree.reset();

            assertNull(fixture.tree.getRoot());
            assertFalse(fixture.tree.dispatchKeyPress(new KeyInputEvent(
                    InputConstants.KEY_SPACE, 0, false, false, false)));
            assertFalse(fixture.tree.dispatchClick(5, 5, new MouseClickEvent(
                    5, 5, InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
        }
    }

    @Test
    void nonTargetableOverlayKeepsItsPaintedButtonEligibleForScopePriority() {
        var tree = new UITree();
        tree.setViewportSize(400, 300);
        var lower = new Modal(Component.literal("Lower"));
        var upper = new Modal(Component.literal("Upper"));
        var overlayButton = new Button();
        var actions = new AtomicInteger();
        overlayButton.addPressListener((x, y) -> actions.incrementAndGet());
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fillMax();
            root.e(lower, modal -> modal.fillSlot(Modal.CONTENT_SLOT, content ->
                    content.e(new Stack() {
                        @Override public boolean isHitTestable() { return false; }
                    }, overlay -> {
                        overlay.layout().fillMaxWidth().fixedHeight(20).layer(300);
                        overlay.e(overlayButton, button -> button.layout().fixedSize(40, 20));
                    })));
            root.e(upper);
        });
        try {
            tree.prepareFrame(400, 300, mock(IScreenContext.class));
            var geometry = overlayButton.getNode().getFrameGeometry();
            int x = (int) ((geometry.left() + geometry.right()) / 2);
            int y = (int) ((geometry.top() + geometry.bottom()) / 2);
            assertTrue(tree.dispatchClick(x, y, new MouseClickEvent(
                    x, y, InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
            assertEquals(1, actions.get());
        } finally {
            tree.reset();
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final UITree tree = new UITree();
        private final Modal first = new Modal(Component.literal("First"));
        private final Modal second = new Modal(Component.literal("Second"));
        private final Rect background = new Rect(0) {
            @Override public boolean isFocusable() { return true; }
        };
        private final AtomicInteger firstActions = new AtomicInteger();
        private final AtomicInteger secondActions = new AtomicInteger();
        private final IScreenContext context = mock(IScreenContext.class);

        Fixture() {
            this.first.close();
            this.second.close();
            this.first.addButton("First action", this.firstActions::incrementAndGet);
            this.second.addButton("Second action", this.secondActions::incrementAndGet);
            this.tree.setViewportSize(400, 300);
            ICompositionScope.root(this.tree).e(new Stack(), root -> {
                root.layout().fillMax();
                root.e(this.background, child -> child.layout().fixedSize(20, 20));
                root.e(this.first);
                root.e(this.second);
            });
            this.prepare();
        }

        void open(Modal modal) {
            modal.show();
            this.prepare();
        }

        void prepare() { this.tree.prepareFrame(400, 300, this.context); }

        ITreeNode action(Modal modal) {
            return this.tree.findNodesByElementType(Button.class).stream()
                    .filter(modal.getNode()::isAncestorOf).findFirst().orElseThrow();
        }

        void click(ITreeNode node) {
            var geometry = node.getFrameGeometry();
            int x = (int) ((geometry.left() + geometry.right()) / 2);
            int y = (int) ((geometry.top() + geometry.bottom()) / 2);
            assertTrue(this.tree.dispatchClick(x, y, new MouseClickEvent(
                    x, y, InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
        }

        @Override public void close() { this.tree.reset(); }
    }
}
