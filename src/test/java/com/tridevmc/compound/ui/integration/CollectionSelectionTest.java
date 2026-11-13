package com.tridevmc.compound.ui.integration;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.EnumSelectionMode;
import com.tridevmc.compound.ui.element.IComposableElement;
import com.tridevmc.compound.ui.element.ListView;
import com.tridevmc.compound.ui.element.TreeView;
import com.tridevmc.compound.ui.event.KeyInputEvent;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import com.tridevmc.compound.ui.visual.BufferedImageScreenContext;
import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class CollectionSelectionTest {
    @Test
    void controlShiftRangesPreserveDisconnectedListAndTreeSelections() {
        var list = new ListView<Integer>();
        list.setItems(List.of(0, 1, 2, 3, 4));
        list.setSelectionMode(EnumSelectionMode.MULTIPLE);
        var listTree = mount(list);
        try {
            click(listTree, 0, false, false);
            click(listTree, 2, false, true);
            click(listTree, 4, true, true);
            assertEquals(List.of(0, 2, 3, 4), list.getSelectedIndices());
        } finally {
            listTree.reset();
        }

        var view = new TreeView<Integer>();
        for (var item : List.of(0, 1, 2, 3, 4)) view.getRoot().addChild(item);
        view.setSelectionMode(EnumSelectionMode.MULTIPLE);
        var viewTree = mount(view);
        try {
            click(viewTree, 0, false, false);
            click(viewTree, 2, false, true);
            click(viewTree, 4, true, true);
            assertEquals(List.of(0, 2, 3, 4), view.getSelectedNodes().stream()
                    .map(TreeView.TreeNode::getValue).toList());
        } finally {
            viewTree.reset();
        }
    }

    @Test
    void keyboardFocusOutlineAppearsOnlyWhileCollectionHasFocus() {
        var list = new ListView<String>();
        list.setItems(List.of("zero", "one"));
        list.setSelectionMode(EnumSelectionMode.MULTIPLE);
        this.assertFocusOutline(list);

        var view = new TreeView<String>();
        view.getRoot().addChild("zero");
        view.getRoot().addChild("one");
        view.setSelectionMode(EnumSelectionMode.MULTIPLE);
        this.assertFocusOutline(view);
    }

    private void assertFocusOutline(IComposableElement collection) {
        var tree = mount(collection);
        click(tree, 0, false, false);
        key(tree, InputConstants.KEY_DOWN, false, true);
        try (var context = new BufferedImageScreenContext(120, 96)) {
            tree.renderTree(context);
            assertEquals(0xFF808080, context.getImage().getRGB(0, 16));
            assertEquals(0xFFFFFFFF, context.getImage().getRGB(0, 0));
        }
        tree.clearFocus();
        try (var context = new BufferedImageScreenContext(120, 96)) {
            tree.renderTree(context);
            assertEquals(0xFF181818, context.getImage().getRGB(0, 16));
            assertEquals(0xFFFFFFFF, context.getImage().getRGB(0, 0));
        }
    }

    @Test
    void listSupportsToggleRangeKeyboardAndImmutableCallbacks() {
        var list = new ListView<String>();
        list.setItems(List.of("zero", "one", "two", "three", "four"));
        list.setSelectionMode(EnumSelectionMode.MULTIPLE);
        var changes = new ArrayList<List<String>>();
        list.setOnSelectionsChanged(changes::add);
        var tree = mount(list);

        click(tree, 0, false, false);
        click(tree, 2, false, true);
        assertEquals(List.of(0, 2), list.getSelectedIndices());
        click(tree, 4, true, false);
        assertEquals(List.of(2, 3, 4), list.getSelectedIndices());
        key(tree, InputConstants.KEY_UP, true, false);
        assertEquals(List.of(2, 3), list.getSelectedIndices());
        key(tree, InputConstants.KEY_HOME, false, true);
        assertEquals(List.of(2, 3), list.getSelectedIndices());
        key(tree, InputConstants.KEY_SPACE, false, true);
        assertEquals(List.of(0, 2, 3), list.getSelectedIndices());
        key(tree, InputConstants.KEY_A, false, true);
        assertEquals(List.of(0, 1, 2, 3, 4), list.getSelectedIndices());
        assertEquals(List.of("zero"), changes.getFirst());
        assertThrows(UnsupportedOperationException.class, () -> changes.getFirst().add("changed"));
        list.clearSelection();
        assertTrue(changes.getLast().isEmpty());
    }

    @Test
    void listDefaultsToSingleAndPreservesSelectedItemsWhenEarlierRowsAreRemoved() {
        var list = new ListView<String>();
        list.setItems(List.of("zero", "one", "two", "three"));
        var tree = mount(list);
        click(tree, 0, false, false);
        click(tree, 2, false, true);
        assertEquals(List.of(2), list.getSelectedIndices());
        list.setSelectionMode(EnumSelectionMode.MULTIPLE);
        list.setSelectedIndices(List.of(1, 3));
        list.removeItem(0);
        assertEquals(List.of(0, 2), list.getSelectedIndices());
        assertEquals(List.of("one", "three"), list.getSelectedItems());
        list.removeItem(2);
        assertEquals(List.of("one"), list.getSelectedItems());
        assertThrows(IllegalArgumentException.class, () -> list.setSelectedIndices(List.of(0, 9)));
        assertEquals(List.of("one"), list.getSelectedItems());
        list.setItems(List.of("replacement"));
        assertTrue(list.getSelectedItems().isEmpty());
    }

    @Test
    void disabledCollectionIgnoresSelectionInputAndSingleModeRetainsOneRow() {
        var list = new ListView<String>();
        list.setItems(List.of("zero", "one", "two"));
        list.setSelectionMode(EnumSelectionMode.MULTIPLE);
        var tree = mount(list);
        click(tree, 0, false, false);
        key(tree, InputConstants.KEY_A, false, true);
        list.setSelectionMode(EnumSelectionMode.SINGLE);
        assertEquals(List.of(2), list.getSelectedIndices());
        list.setEnabled(false);
        tree.dispatchClick(5, 5, new MouseClickEvent(5, 5, InputConstants.MOUSE_BUTTON_LEFT, false, true, false));
        tree.dispatchKeyPress(new KeyInputEvent(InputConstants.KEY_HOME,
                InputConstants.KEY_HOME, false, false, false));
        assertEquals(List.of(2), list.getSelectedIndices());
    }

    @Test
    void treeUsesVisibleRangesWithoutCollapsingOnModifierSelection() {
        var view = new TreeView<String>();
        var parent = view.getRoot().addChild("parent");
        var child = parent.addChild("child");
        var sibling = view.getRoot().addChild("sibling");
        view.setSelectionMode(EnumSelectionMode.MULTIPLE);
        var tree = mount(view);
        click(tree, 0, false, true);
        assertTrue(parent.isExpanded());
        click(tree, 2, true, false);
        assertEquals(List.of(parent, child, sibling), view.getSelectedNodes());
        parent.setExpanded(false);
        tree.prepareFrame(120, 96, mock(IScreenContext.class));
        assertEquals(List.of(parent, child, sibling), view.getSelectedNodes());
        key(tree, InputConstants.KEY_HOME, false, false);
        key(tree, InputConstants.KEY_END, true, false);
        assertEquals(List.of(parent, sibling), view.getSelectedNodes());
        key(tree, InputConstants.KEY_A, false, true);
        assertEquals(List.of(parent, sibling), view.getSelectedNodes());
    }

    @Test
    void detachedTreeSubtreesCannotRemainSelectedOrBeSelectedAgain() {
        var view = new TreeView<String>();
        var parent = view.getRoot().addChild("parent");
        var child = parent.addChild("child");
        var sibling = view.getRoot().addChild("sibling");
        view.setSelectionMode(EnumSelectionMode.MULTIPLE);
        var changes = new ArrayList<List<TreeView.TreeNode<String>>>();
        view.setOnSelectionsChanged(changes::add);
        view.setSelectedNodes(List.of(child, sibling));
        view.getRoot().removeChild(parent);
        assertEquals(List.of(sibling), view.getSelectedNodes());
        assertEquals(List.of(sibling), changes.getLast());
        assertFalse(child.isSelected());
        child.setSelected(true);
        assertFalse(child.isSelected());
        assertThrows(IllegalArgumentException.class, () -> view.setSelectedNodes(List.of(child)));
        var foreign = new TreeView<String>().getRoot().addChild("foreign");
        assertThrows(IllegalArgumentException.class, () -> view.setSelectedNodes(List.of(foreign)));
        assertEquals(List.of(sibling), view.getSelectedNodes());
        view.getRoot().removeChild(sibling);
        assertTrue(changes.getLast().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> changes.getFirst().clear());
    }

    private static UITree mount(IComposableElement element) {
        var tree = new UITree();
        ICompositionScope.root(tree).e(element, scope -> scope.layout().fixedSize(120, 96));
        tree.prepareFrame(120, 96, mock(IScreenContext.class));
        return tree;
    }

    private static void click(UITree tree, int row, boolean shift, boolean ctrl) {
        var y = row * 16 + 5;
        assertTrue(tree.dispatchClick(5, y, new MouseClickEvent(5, y, InputConstants.MOUSE_BUTTON_LEFT, shift, ctrl, false)), () -> {
            var path = new StringBuilder("Click row ").append(row).append(" root bounds ")
                    .append(tree.getRoot().getBounds()).append(" hit path: ");
            for (var node = tree.findNodeAt(5, y); node != null; node = node.getParent()) {
                path.append(node.getElement().getClass().getSimpleName()).append(node.getBounds())
                        .append(" handlers=").append(node.getClickHandlers().size()).append("; ");
            }
            return path.toString();
        });
    }

    private static void key(UITree tree, int key, boolean shift, boolean ctrl) {
        var logical = key == InputConstants.KEY_A ? InputConstants.KEYCODE_A : key;
        assertTrue(tree.dispatchKeyPress(new KeyInputEvent(key, logical, shift, ctrl, false)));
    }
}
