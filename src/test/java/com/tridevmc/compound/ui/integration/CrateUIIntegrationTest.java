package com.tridevmc.compound.ui.integration;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.element.Box;
import com.tridevmc.compound.ui.element.Button;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.Grid;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.element.InventorySlot;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.Panel;
import com.tridevmc.compound.ui.element.Row;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.element.Spacer;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.element.TextInput;
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.tree.UITree;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftMockExtension.class)
class CrateUIIntegrationTest {
    @Test
    void inventoryLayoutAndScrollableControlsRenderAtTheirPlacedBounds() {
        var tree = new UITree();
        var context = mock(IScreenContext.class);
        var mainPanel = new Panel();
        var scrollPanel = new Panel();
        var area = new ScrollArea().scrollSpeed(10);
        var content = new Column();
        var controls = new ArrayList<IElement>();
        var slots = new ArrayList<InventorySlot>();
        for (int index = 0; index < 63; index++) {
            var slot = mock(Slot.class);
            when(slot.getItem()).thenReturn(ItemStack.EMPTY);
            slots.add(new InventorySlot(slot));
        }
        try {
            ICompositionScope.root(tree).e(new Stack(), root -> {
                root.layout().fillMax().contentAlignment(Alignment.CENTER);
                root.e(new Row(), row -> {
                    row.layout().spacing(8);
                    inventoryPanel(row, mainPanel, slots);
                    row.e(scrollPanel, panel -> {
                        panel.layout().fixedSize(120, 190);
                        panel.fillSlot(Panel.CONTENT_SLOT, body -> body.e(new Box(), padding -> {
                            padding.layout().padding(4);
                            padding.e(area, scroll -> scroll.fillSlot(ScrollArea.CONTENT_SLOT,
                                    viewport -> viewport.e(content, column -> {
                                        column.layout().spacing(2);
                                        addControls(column, controls);
                                    })));
                        }));
                    });
                });
            });
            tree.prepareFrame(800, 600, context);

            assertEquals(new Bounds(247, 205, 178, 190), mainPanel.getBounds());
            assertEquals(new Bounds(433, 205, 120, 190), scrollPanel.getBounds());
            assertGridBounds(slots.subList(0, 27), 255, 226);
            assertGridBounds(slots.subList(27, 54), 255, 297);
            assertGridBounds(slots.subList(54, 63), 255, 363);
            var viewport = new Bounds(437, 209, 112, 182);
            assertEquals(viewport, area.getBounds());
            assertEquals(new Bounds(437, 209, 102, 1098), content.getBounds());
            assertEquals(1098 - viewport.height(), area.getMaxScrollY());
            for (int index = 0; index < controls.size(); index++) {
                assertEquals(new Bounds(437, 209 + index * 22, 102, 20),
                        controls.get(index).getBounds(), "Control " + index);
            }

            tree.renderTree(context);
            assertVisibleDrawing(context, controls, slots, viewport);
        } finally {
            tree.reset();
        }
    }

    private static void inventoryPanel(ICompositionScope row, Panel panel,
                                       List<InventorySlot> slots) {
        row.e(panel, main -> {
            main.layout().fixedSize(178, 190);
            main.fillSlot(Panel.CONTENT_SLOT, body -> body.e(new Box(), padding -> {
                padding.layout().padding(8);
                padding.e(new Column(), column -> {
                    column.layout().spacing(4);
                    column.e(new Label(Component.literal("Crate"), 0x404040, false));
                    column.e(new Grid(9, 0, 0), grid -> slots.subList(0, 27).forEach(grid::e));
                    column.e(new Label(Component.literal("Inventory"), 0x404040, false));
                    column.e(new Grid(9, 0, 0), grid -> slots.subList(27, 54).forEach(grid::e));
                    column.e(new Spacer(0, 4));
                    column.e(new Grid(9, 0, 0), grid -> slots.subList(54, 63).forEach(grid::e));
                });
            }));
        });
    }

    private static void addControls(ICompositionScope column, List<IElement> controls) {
        for (int index = 1; index <= 50; index++) {
            if (index % 2 == 1) {
                var input = new TextInput().setHint(Component.literal("Input " + index));
                controls.add(input);
                column.e(input, child -> child.layout().fillMaxWidth().fixedHeight(20));
            } else {
                var button = new Button();
                var label = Component.literal("Button " + index);
                controls.add(button);
                column.e(button, child -> {
                    child.layout().fillMaxWidth().fixedHeight(20);
                    child.fillSlot(Button.CONTENT_SLOT, body -> body.e(new Label(label)));
                });
            }
        }
    }

    private static void assertGridBounds(List<InventorySlot> slots, int x, int y) {
        for (int index = 0; index < slots.size(); index++) {
            assertEquals(new Bounds(x + index % 9 * 18, y + index / 9 * 18, 18, 18),
                    slots.get(index).getBounds(), "Inventory slot " + index);
        }
    }

    private static void assertVisibleDrawing(IScreenContext context, List<IElement> controls,
                                             List<InventorySlot> slots, Bounds viewport) {
        var sprites = ArgumentCaptor.forClass(IScreenSprite.class);
        var rectangles = ArgumentCaptor.forClass(Rect2F.class);
        verify(context, atLeastOnce()).drawSprite(sprites.capture(), rectangles.capture());
        for (var slot : slots) {
            assertTrue(hasDrawnBounds(rectangles.getAllValues(), slot.getBounds()),
                    "Inventory slot background should be submitted at its placed bounds");
        }
        for (var control : controls) {
            var bounds = control.getBounds();
            boolean drawn = hasDrawnBounds(rectangles.getAllValues(), bounds);
            assertEquals(bounds.intersects(viewport), drawn,
                    "Only visible controls should submit their background sprite");
        }
        var plainText = ArgumentCaptor.forClass(Component.class);
        var shadowText = ArgumentCaptor.forClass(Component.class);
        verify(context, atLeastOnce()).drawText(plainText.capture(), anyFloat(), anyFloat());
        verify(context, atLeastOnce()).drawTextWithShadow(shadowText.capture(), anyFloat(), anyFloat());
        var renderedText = new ArrayList<String>();
        plainText.getAllValues().forEach(text -> renderedText.add(text.getString()));
        shadowText.getAllValues().forEach(text -> renderedText.add(text.getString()));
        assertTrue(renderedText.containsAll(List.of("Crate", "Inventory", "Input 1", "Button 2")));
        assertFalse(renderedText.contains("Input 49"));
        verify(context, atLeastOnce()).enableScissor(viewport.x(), viewport.y(),
                viewport.right(), viewport.bottom());
        verify(context, atLeastOnce()).disableScissor();
    }

    private static boolean hasDrawnBounds(List<Rect2F> rectangles, Bounds bounds) {
        return rectangles.stream().anyMatch(rect -> rect.getX() == bounds.x()
                && rect.getY() == bounds.y() && rect.getWidth() == bounds.width()
                && rect.getHeight() == bounds.height());
    }
}
