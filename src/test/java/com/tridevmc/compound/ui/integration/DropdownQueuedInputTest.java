package com.tridevmc.compound.ui.integration;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Dropdown;
import com.tridevmc.compound.ui.element.ListItem;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class DropdownQueuedInputTest {
    @Test
    void disablingAnOpenDropdownImmediatelyRejectsQueuedOptionClicks() {
        var dropdown = new Dropdown<>(List.of("A", "B"), "A");
        var changes = new AtomicInteger();
        dropdown.setOnSelectionChanged(value -> changes.incrementAndGet());
        var tree = new UITree();
        try {
            ICompositionScope.root(tree).e(dropdown, scope -> scope.layout().fixedSize(100, 20));
            var context = mock(IScreenContext.class);
            tree.prepareFrame(400, 300, context);
            click(tree, 5, 5);
            tree.prepareFrame(400, 300, context);
            var option = tree.findNodesByElementType(ListItem.class).getLast().getFrameGeometry();
            dropdown.setEnabled(false);
            assertFalse(dropdown.isOpen());
            click(tree, (int) option.left() + 4, (int) option.top() + 4);
            assertEquals("A", dropdown.getSelected());
            assertEquals(0, changes.get());
        } finally {
            tree.reset();
        }
    }

    @Test
    void closingAfterSelectionRejectsASecondQueuedOptionClick() {
        var dropdown = new Dropdown<>(List.of("A", "B"), "A");
        var changes = new AtomicInteger();
        dropdown.setOnSelectionChanged(value -> changes.incrementAndGet());
        var tree = new UITree();
        try {
            ICompositionScope.root(tree).e(dropdown, scope -> scope.layout().fixedSize(100, 20));
            var context = mock(IScreenContext.class);
            tree.prepareFrame(400, 300, context);
            click(tree, 5, 5);
            tree.prepareFrame(400, 300, context);
            var first = tree.findNodesByElementType(ListItem.class).getFirst().getFrameGeometry();
            var last = tree.findNodesByElementType(ListItem.class).getLast().getFrameGeometry();
            click(tree, (int) last.left() + 4, (int) last.top() + 4);
            assertFalse(dropdown.isOpen());
            click(tree, (int) first.left() + 4, (int) first.top() + 4);
            assertEquals("B", dropdown.getSelected());
            assertEquals(1, changes.get());
        } finally {
            tree.reset();
        }
    }

    private static void click(UITree tree, int x, int y) {
        tree.dispatchClick(x, y, new MouseClickEvent(x, y,
                InputConstants.MOUSE_BUTTON_LEFT, false, false, false));
    }
}
