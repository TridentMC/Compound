package com.tridevmc.compound.ui.compose.integration;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.compose.element.*;
import com.tridevmc.compound.ui.compose.layout.*;
import com.tridevmc.compound.ui.compose.scope.RootScope;
import com.tridevmc.compound.ui.compose.tree.ITreeNode;
import com.tridevmc.compound.ui.compose.tree.UITree;
import com.tridevmc.compound.ui.screen.IPrimitiveScreenContext;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MinecraftMockExtension.class, MockitoExtension.class})
public class ChestScreenIntegrationTest {

    @Mock
    private AbstractContainerMenu menu;

    @Mock
    private IScreenContext screenContext;

    @Test
    void testChestScreenComposition() {
        // 1. Setup
        UITree tree = new UITree();
        RootScope rootScope = new RootScope(tree);

        // Mock slots
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < 90; i++) {
            Slot slot = mock(Slot.class);
            when(this.menu.getSlot(i)).thenReturn(slot);
            slots.add(slot);
        }

        // Mock sprite
        IScreenSprite inventoryPanelSprite = mock(IScreenSprite.class);

        // 2. Composition
        rootScope.e(new Stack(), stack -> {
            stack.layout().contentAlignment(Alignment.CENTER);

            stack.e(new ElementBox(inventoryPanelSprite), mainPanel -> {
                mainPanel.layout()
                        .fixedSize(176, 222);

                mainPanel.fillSlot(ElementBox.CONTENT_SLOT, contentScope -> {
                    contentScope.e(new Column(), contentColumn -> {
                        contentColumn.layout().padding(7).spacing(4);

                        // --- CHEST INVENTORY ---
                        contentColumn.e(new ElementLabel(Component.literal("Chest"), 0x404040));

                        contentColumn.e(new Grid(9, 0), chestGrid -> {
                            for (int i = 0; i < 54; i++) {
                                chestGrid.e(new ComposedSlot(this.menu.getSlot(i)));
                            }
                        });

                        // --- PLAYER INVENTORY ---
                        contentColumn.e(new ElementLabel(Component.literal("Player Inventory"), 0x404040));

                        contentColumn.e(new Grid(9, 0), playerGrid -> {
                            for (int i = 0; i < 27; i++) {
                                int slotIndex = 54 + i;
                                playerGrid.e(new ComposedSlot(this.menu.getSlot(slotIndex)));
                            }
                        });

                        // --- PLAYER HOTBAR ---
                        contentColumn.e(new ElementSpacer(), spacer -> {
                            spacer.layout().fixedSize(0, 4);
                        });

                        contentColumn.e(new Grid(9, 0), hotbarGrid -> {
                            for (int i = 0; i < 9; i++) {
                                int slotIndex = 54 + 27 + i;
                                hotbarGrid.e(new ComposedSlot(this.menu.getSlot(slotIndex)));
                            }
                        });
                    });
                });
            });
        });

        // 3. Layout
        // We assume a screen size of 400x300 for the test
        var rootConstraints = new Constraints(0, 400, 0, 300);
        tree.measureTree(rootConstraints);
        tree.placeTree(new Position(0, 0), rootConstraints);

        // 4. Assertions
        assertNotNull(tree.getRoot());
        assertEquals(1, tree.getRoot().getChildren().size()); // Root Stack has one child (ElementBox)

        ITreeNode mainPanelNode = tree.getRoot().getChildren().getFirst();
        IElement mainPanelElement = mainPanelNode.getElement();
        assertEquals(ElementBox.class, mainPanelElement.getClass());
        Bounds mainPanelBounds = mainPanelElement.getBounds();
        // Centered in 400x300 screen
        assertEquals(new Bounds((400 - 176) / 2, (300 - 222) / 2, 176, 222), mainPanelBounds);

        // Get the main column from the content of the ElementBox
        // ElementBox -> Stack -> Column
        ITreeNode stackNode = mainPanelNode.getChildren().getFirst();
        ITreeNode columnNode = stackNode.getChildren().get(1); // 0 is the sprite, 1 is the content
        IElement columnElement = columnNode.getElement();
        assertEquals(Column.class, columnElement.getClass());
        Bounds columnBounds = columnElement.getBounds();
        assertNotNull(columnBounds, "Column should have bounds after placement");
        // Column itself is at panel position; padding applies to Column's children
        assertEquals(mainPanelBounds.x(), columnBounds.x());
        assertEquals(mainPanelBounds.y(), columnBounds.y());

        // Verify that Column's children respect the padding
        if (!columnNode.getChildren().isEmpty()) {
            ITreeNode firstChildNode = columnNode.getChildren().getFirst(); // "Chest" label
            Bounds firstChildBounds = firstChildNode.getElement().getBounds();
            assertNotNull(firstChildBounds, "Column's first child should have bounds");
            // Children should be offset by padding
            assertEquals(mainPanelBounds.x() + 7, firstChildBounds.x(), "Column children should respect left padding");
            assertEquals(mainPanelBounds.y() + 7, firstChildBounds.y(), "Column children should respect top padding");
        }

        // 5. Rendering
        tree.renderTree(this.screenContext);

        // 6. Verify render calls
        // Verify the main panel sprite was drawn
        verify(this.screenContext).drawSprite(eq(inventoryPanelSprite), anyFloat(), anyFloat(), anyFloat(), anyFloat());

        // Verify the "Chest" label was drawn
        ArgumentCaptor<Component> textCaptor = ArgumentCaptor.forClass(Component.class);
        ArgumentCaptor<Float> xCaptor = ArgumentCaptor.forClass(Float.class);
        ArgumentCaptor<Float> yCaptor = ArgumentCaptor.forClass(Float.class);

        // We expect 2 labels, drawn with shadow by default
        verify((IPrimitiveScreenContext) this.screenContext, times(2)).drawTextWithShadow(textCaptor.capture(), xCaptor.capture(), yCaptor.capture());

        List<Component> capturedTexts = textCaptor.getAllValues();
        List<Float> capturedX = xCaptor.getAllValues();
        List<Float> capturedY = yCaptor.getAllValues();

        // "Chest" label
        assertEquals("Chest", capturedTexts.getFirst().getString());
        assertNotNull(capturedTexts.getFirst().getStyle().getColor());
        assertEquals(0x404040, capturedTexts.getFirst().getStyle().getColor().getValue());
        // Position should be inside the column
        assertTrue(capturedX.getFirst() > columnBounds.x());
        assertTrue(capturedY.getFirst() > columnBounds.y());

        // "Player Inventory" label
        assertEquals("Player Inventory", capturedTexts.get(1).getString());
        assertNotNull(capturedTexts.get(1).getStyle().getColor());
        assertEquals(0x404040, capturedTexts.get(1).getStyle().getColor().getValue());
        // Position should be below the first label and chest grid
        assertTrue(capturedX.get(1) > columnBounds.x());
        assertTrue(capturedY.get(1) > capturedY.get(0));

        // Verify that slots were rendered. Each slot renders a background sprite.
        // 9*6 + 9*3 + 9*1 = 54 + 27 + 9 = 90 slots
        // This is a bit complex to verify precisely without knowing the slot rendering logic.
        // A simple verification could be to check if the slot's draw method was called.
        // However, ComposedSlot is a composable, so we'd check its internal elements.
        // For now, we'll assume the presence in the tree is sufficient for this test.
        // A more detailed test would mock the ComposedSlot's internal composition.
    }
}
