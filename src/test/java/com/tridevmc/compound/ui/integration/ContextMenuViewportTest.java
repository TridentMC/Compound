package com.tridevmc.compound.ui.integration;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.ContextMenu;
import com.tridevmc.compound.ui.element.Modal;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.element.Surface;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.geometry.api.ITransform2D;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import com.tridevmc.compound.ui.visual.BufferedImageScreenContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class ContextMenuViewportTest {
    @Test
    void guiPositionIsNotTranslatedAgainAndMenuStaysInsideViewport() {
        var menu = new ContextMenu();
        menu.addItem("Action", () -> { });
        var tree = mount(menu);
        try {
            menu.show(350, 250);
            tree.prepareFrame(400, 300, mock(IScreenContext.class));
            var geometry = tree.findNodesByElementType(Surface.class).getFirst().getFrameGeometry();
            assertEquals(280, geometry.left(), 1e-8);
            assertEquals(250, geometry.top(), 1e-8);
            assertTrue(geometry.right() <= 400);
            assertTrue(geometry.bottom() <= 300);
            try (var image = new BufferedImageScreenContext(400, 300)) {
                tree.renderTree(image);
                assertEquals(0xFFA0A0A0, image.getImage().getRGB(280, 250));
                image.saveTo("build/screenshots/transformed-context-menu.png");
            }
        } finally {
            tree.reset();
        }
    }

    @Test
    void currentFrameScaleClampsTheMenuWithoutMovingItsRequestedGuiPosition() {
        var menu = new ContextMenu();
        menu.addItem("Action", () -> { });
        var transform = new AtomicReference<>(ITransform2D.translation(100, 50));
        var tree = mount(menu, transform::get);
        try {
            menu.show(350, 250);
            tree.prepareFrame(400, 300, mock(IScreenContext.class));
            transform.set(ITransform2D.of(50, 70, 0, 2, 2, 0, 0));
            tree.prepareFrame(400, 300, mock(IScreenContext.class));
            var geometry = tree.findNodesByElementType(Surface.class).getFirst().getFrameGeometry();
            assertEquals(160, geometry.left(), 1e-8);
            assertEquals(250, geometry.top(), 1e-8);
            assertEquals(400, geometry.right(), 1e-8);
            assertEquals(286, geometry.bottom(), 1e-8);
        } finally {
            tree.reset();
        }
    }

    @Test
    void fractionalReflectedBackdropStillDismissesAtTheViewportEdge() {
        var menu = new ContextMenu();
        menu.addItem("Action", () -> { });
        var tree = mount(menu, () -> ITransform2D.of(399.5, 299.5, 0, -1.5, -.75, 0, 0));
        try {
            menu.show(250, 100);
            tree.prepareFrame(400, 300, mock(IScreenContext.class));
            var geometry = tree.findNodesByElementType(Surface.class).getFirst().getFrameGeometry();
            assertTrue(geometry.left() >= 0 && geometry.right() <= 400);
            assertTrue(geometry.top() >= 0 && geometry.bottom() <= 300);
            tree.dispatchClick(0, 0, new MouseClickEvent(
                    0, 0, InputConstants.MOUSE_BUTTON_LEFT, false, false, false));
            assertFalse(menu.isVisible());
        } finally {
            tree.reset();
        }
    }

    @Test
    void translatedMenuInsideAModalRetainsGlobalPlacementAndActionInput() {
        var tree = new UITree();
        tree.setViewportSize(400, 300);
        var menu = new ContextMenu();
        var actions = new AtomicInteger();
        menu.addItem("Action", actions::incrementAndGet);
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fillMax();
            root.transform(() -> ITransform2D.translation(100, 50));
            root.e(new Modal(null), modal -> modal.fillSlot(Modal.CONTENT_SLOT,
                    content -> content.e(menu)));
        });
        try {
            tree.prepareFrame(400, 300, mock(IScreenContext.class));
            menu.show(350, 250);
            tree.prepareFrame(400, 300, mock(IScreenContext.class));
            var geometry = tree.findNodesByElementType(Surface.class).stream()
                    .filter(menu.getNode()::isAncestorOf).findFirst().orElseThrow().getFrameGeometry();
            assertEquals(280, geometry.left(), 1e-8);
            assertEquals(250, geometry.top(), 1e-8);
            assertTrue(tree.dispatchClick(285, 255, new MouseClickEvent(
                    285, 255, InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
            assertEquals(1, actions.get());
            assertFalse(menu.isVisible());
        } finally {
            tree.reset();
        }
    }

    @Test
    void outsideViewportEdgeClickDismissesTranslatedMenu() {
        var menu = new ContextMenu();
        menu.addItem("Action", () -> { });
        var tree = mount(menu);
        try {
            menu.show(200, 100);
            tree.prepareFrame(400, 300, mock(IScreenContext.class));
            tree.dispatchClick(0, 299, new MouseClickEvent(
                    0, 299, InputConstants.MOUSE_BUTTON_LEFT, false, false, false));
            assertFalse(menu.isVisible());
        } finally {
            tree.reset();
        }
    }

    private static UITree mount(ContextMenu menu) {
        return mount(menu, () -> ITransform2D.translation(100, 50));
    }

    private static UITree mount(ContextMenu menu, Supplier<ITransform2D> transform) {
        var tree = new UITree();
        tree.setViewportSize(400, 300);
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fillMax();
            root.e(new Stack(), translated -> {
                translated.layout().fillMax();
                translated.transform(transform);
                translated.e(menu);
            });
        });
        tree.prepareFrame(400, 300, mock(IScreenContext.class));
        return tree;
    }
}
