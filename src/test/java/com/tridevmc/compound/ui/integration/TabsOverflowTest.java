package com.tridevmc.compound.ui.integration;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Button;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.element.Tabs;
import com.tridevmc.compound.ui.event.KeyInputEvent;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseScrollEvent;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.ITreeNode;
import com.tridevmc.compound.ui.tree.UITree;
import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class TabsOverflowTest {
    @Test
    void lastTabIsRevealedOnFirstLayoutAndProgrammaticSelection() {
        var tabs = tabs();
        tabs.setSelectedTab(7);
        var tree = tree(tabs);
        try {
            prepare(tree);
            var headers = tree.findNodesByElementType(Button.class);
            assertVisible(tree, headers.getLast());
            assertFalse(headers.getFirst().getFrameGeometry().contains(2, 2));
            tabs.setSelectedTab(0);
            prepare(tree);
            assertVisible(tree, headers.getFirst());
            tabs.setSelectedTab(7);
            prepare(tree);
            assertVisible(tree, headers.getLast());
            var bounds = headers.getLast().getBounds();
            int x = bounds.x() + bounds.width() / 2;
            int y = bounds.y() + 5;
            assertTrue(tree.dispatchClick(x, y, new MouseClickEvent(x, y,
                    InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
            assertEquals(7, tabs.getSelectedTab());
        } finally {
            tree.reset();
        }
    }

    @Test
    void keyboardRevealsHeadersSkipsDisabledTabsAndKeepsHeaderNodes() {
        var tabs = tabs();
        tabs.setTabEnabled(6, false);
        var tree = tree(tabs);
        try {
            prepare(tree);
            var headers = tree.findNodesByElementType(Button.class);
            tree.requestFocus(headers.getFirst());
            assertTrue(tree.dispatchKeyPress(key(InputConstants.KEY_END)));
            prepare(tree);
            assertEquals(7, tabs.getSelectedTab());
            assertSame(headers.getLast(), tree.getFocusedNode());
            assertVisible(tree, headers.getLast());
            assertTrue(tree.dispatchKeyPress(key(InputConstants.KEY_LEFT)));
            prepare(tree);
            assertEquals(5, tabs.getSelectedTab());
            assertSame(headers.get(5), tree.getFocusedNode());
            assertVisible(tree, headers.get(5));
            assertTrue(tree.dispatchKeyPress(key(InputConstants.KEY_HOME)));
            prepare(tree);
            assertEquals(0, tabs.getSelectedTab());
            assertSame(headers.getFirst(), tree.getFocusedNode());
            assertSame(headers.getFirst(), tree.findNodesByElementType(Button.class).getFirst());
            assertVisible(tree, headers.getFirst());
        } finally {
            tree.reset();
        }
    }

    @Test
    void wheelScrollRemainsAvailableWithoutChangingSelection() {
        var tabs = tabs();
        var tree = tree(tabs);
        try {
            prepare(tree);
            var viewport = (ScrollArea) tree.findNodesByElementType(ScrollArea.class).getFirst().getElement();
            assertTrue(tree.dispatchScroll(5, 5, new MouseScrollEvent(5, 5, -1)));
            prepare(tree);
            assertEquals(20, viewport.getScrollXState().get());
            assertEquals(0, tabs.getSelectedTab());
        } finally {
            tree.reset();
        }
    }

    private static Tabs tabs() {
        var tabs = new Tabs();
        for (int index = 0; index < 8; index++) {
            tabs.addTab("Tab " + index, scope -> scope.e(new Rect(0)));
        }
        return tabs;
    }

    private static UITree tree(Tabs tabs) {
        var tree = new UITree();
        ICompositionScope.root(tree).e(tabs, scope -> scope.layout().fixedSize(160, 120));
        return tree;
    }

    private static void prepare(UITree tree) {
        tree.prepareFrame(160, 120, mock(IScreenContext.class));
    }

    private static KeyInputEvent key(int code) {
        return new KeyInputEvent(code, code, false, false, false);
    }

    private static void assertVisible(UITree tree, ITreeNode header) {
        var viewport = tree.findNodesByElementType(ScrollArea.class).getFirst().getBounds();
        var bounds = header.getBounds();
        assertTrue(bounds.left() >= viewport.left());
        assertTrue(bounds.right() <= viewport.right());
        assertSame(header, ancestorButton(tree.findNodeAt(bounds.x() + 2, bounds.y() + 2)));
    }

    private static ITreeNode ancestorButton(ITreeNode node) {
        for (var current = node; current != null; current = current.getParent()) {
            if (current.getElement() instanceof Button) return current;
        }
        throw new AssertionError("Visible header must be hit-testable");
    }
}
