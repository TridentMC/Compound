package com.tridevmc.compound.ui.integration;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.ContextMenu;
import com.tridevmc.compound.ui.element.Dropdown;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.Modal;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.element.Tooltip;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import com.tridevmc.compound.ui.visual.BufferedImageScreenContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class NestedPopupLayerTest {
    @Test
    void dropdownInsideModalDrawsAboveBackdropAndSelectsVisibleOption() {
        var tree = new UITree();
        tree.setViewportSize(400, 300);
        var dropdown = new Dropdown<>(List.of("A", "B", "C"));
        var modal = new Modal(null);
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fillMax();
            root.e(modal, dialog -> dialog.fillSlot(Modal.CONTENT_SLOT,
                    content -> content.e(dropdown, control -> control.layout().fixedSize(100, 20))));
        });
        var colours = new ArrayList<Integer>();
        try (var context = new BufferedImageScreenContext(400, 300) {
            @Override
            public void drawGradientRect(float x, float y, float width, float height,
                    int startColour, int endColour) {
                colours.add(startColour);
                super.drawGradientRect(x, y, width, height, startColour, endColour);
            }
        }) {
            tree.prepareFrame(400, 300, context);
            var bounds = dropdown.getBounds();
            click(tree, bounds.x() + 4, bounds.y() + 4);
            assertTrue(dropdown.isOpen());
            tree.prepareFrame(400, 300, context);
            colours.clear();
            tree.renderTree(context);
            int backdrop = colours.indexOf(0x90000000);
            int popup = colours.indexOf(0xFF202020);
            assertTrue(backdrop >= 0 && popup > backdrop);
            var option = tree.findNodesByElementType(Label.class).stream()
                    .filter(node -> ((Label) node.getElement()).getText().getString().equals("B"))
                    .findFirst().orElseThrow().getBounds();
            click(tree, option.x() + 2, option.y() + 2);
            assertEquals("B", dropdown.getSelected());
            assertFalse(dropdown.isOpen());
        } finally {
            tree.reset();
        }
    }

    @Test
    void builtInOverlayLayersRespectAncestorsAndStayStableOnRecomposition() {
        for (int ancestor : new int[]{1000, Integer.MAX_VALUE}) {
            var tree = new UITree();
            tree.setViewportSize(200, 180);
            var menu = new ContextMenu();
            menu.addItem("Action", () -> { });
            menu.show(0, 0);
            var modal = new Modal(null);
            var tooltip = new Tooltip(() -> null);
            ICompositionScope.root(tree).e(new Stack(), root -> {
                root.layout().fillMax().layer(ancestor);
                root.e(menu);
                root.e(modal);
                root.e(tooltip, owner -> owner.fillSlot(Tooltip.CONTENT_SLOT,
                        content -> content.e(new Rect(0), rect -> rect.layout().fixedSize(20, 20))));
            });
            try {
                var context = mock(IScreenContext.class);
                int expected = ancestor == Integer.MAX_VALUE ? Integer.MAX_VALUE : ancestor + 1;
                for (int frame = 0; frame < 2; frame++) {
                    tree.prepareFrame(200, 180, context);
                    assertEquals(expected, menu.getNode().getLayoutProperties().getLayer());
                    assertEquals(expected, modal.getNode().getLayoutProperties().getLayer());
                    assertEquals(expected, tooltip.getNode().getChildren().getLast()
                            .getLayoutProperties().getLayer());
                    tree.requestRecompose(menu.getNode());
                    tree.requestRecompose(modal.getNode());
                    tree.requestRecompose(tooltip.getNode());
                }
            } finally {
                tree.reset();
            }
        }
    }

    private static void click(UITree tree, int x, int y) {
        assertTrue(tree.dispatchClick(x, y,
                new MouseClickEvent(x, y, InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
    }
}
