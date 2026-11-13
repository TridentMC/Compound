package com.tridevmc.compound.ui.tree;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.event.CharEvent;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class InputTargetLifecycleTest {
    @Test
    void hidingAncestorReleasesFocusAndCaptureBeforeDeliveringInput() {
        var visible = new AtomicBoolean(true);
        var typed = new AtomicInteger();
        var lost = new AtomicInteger();
        var leaf = new Rect(0) {
            @Override
            public boolean isFocusable() {
                return true;
            }
        };
        var tree = new UITree();
        ICompositionScope.root(tree).e(new Stack() {
            @Override
            public boolean isVisible() {
                return visible.get();
            }
        }, root -> {
            root.layout().fillMax();
            root.e(leaf, child -> {
                child.layout().fixedSize(20, 20);
                child.onClick(event -> true);
                child.layout().getBoundNode().addFocusLostHandler(lost::incrementAndGet);
                child.layout().getBoundNode().addCharTypedHandler(event -> {
                    typed.incrementAndGet();
                    return true;
                });
            });
        });
        tree.prepareFrame(100, 100, mock(IScreenContext.class));
        tree.requestFocus(leaf.getNode());
        assertTrue(tree.dispatchClick(5, 5, new MouseClickEvent(
                5, 5, InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
        tree.requestFocus(leaf.getNode());
        visible.set(false);

        assertFalse(tree.dispatchCharTyped(new CharEvent('X', 0)));
        assertEquals(0, typed.get());
        assertNull(tree.getFocusedNode());
        assertNull(tree.getCapturedNode());
        assertEquals(1, lost.get());
        tree.requestFocus(leaf.getNode());
        assertNull(tree.getFocusedNode());
        tree.reset();
    }

    @Test
    void clickHandlerThatDetachesItselfCannotAcquireCapture() {
        var tree = new UITree();
        var leaf = new Rect(0);
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fillMax();
            root.e(leaf, child -> {
                child.layout().fixedSize(20, 20);
                child.onClick(event -> {
                    tree.detachNode(leaf.getNode());
                    return true;
                });
            });
        });
        tree.prepareFrame(100, 100, mock(IScreenContext.class));
        assertTrue(tree.dispatchClick(5, 5, new MouseClickEvent(
                5, 5, InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
        assertNull(tree.getCapturedNode());
        tree.reset();
    }
}
