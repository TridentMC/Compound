package com.tridevmc.compound.ui.integration;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.*;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.RootScope;
import com.tridevmc.compound.ui.tree.ITreeNode;
import com.tridevmc.compound.ui.tree.UITree;
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
public class CrateUIIntegrationTest {

    @Mock
    private AbstractContainerMenu menu;

    @Mock
    private IScreenContext screenContext;

    @Test
    void testCrateUIComposition() {
        // 1. Setup
        UITree tree = new UITree();
        RootScope scope = new RootScope(tree);

        // Mock slots (27 crate + 27 player + 9 hotbar = 63 slots)
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < 63; i++) {
            Slot slot = mock(Slot.class);
            when(this.menu.getSlot(i)).thenReturn(slot);
            slots.add(slot);
        }

        // 2. Composition (Mimicking CrateUI.compose)
        // Center everything on screen using Stack that fills the screen
        scope.e(new Stack(), stack -> {
            // Make stack fill the entire screen and center its children
            stack.layout()
                    .fillMax()
                    .contentAlignment(Alignment.CENTER);

            // Row to hold main UI and scroll area side by side
            stack.e(new Row(), row -> {
                row.layout().spacing(8); // 8px gap between main UI and scroll area

                // Background box with default inventory sprite
                row.e(new ElementBox(), box -> {
                    // ElementBox just specifies its size
                    box.layout().fixedSize(178, 190);

                    // Fill content slot with Box for padding
                    box.fillSlot(ElementBox.CONTENT_SLOT, content -> {
                        content.e(new Box(), paddedContent -> {
                            // Set padding on the box
                            paddedContent.layout().padding(8);

                            // Main content column
                            paddedContent.e(new Column(), column -> {
                                // Set spacing on the column
                                column.layout().spacing(4);
                                // "Crate" label - no shadow for Minecraft inventory style
                                column.e(new ElementLabel(
                                        Component.literal("Crate"),
                                        0x404040,
                                        false  // No shadow for inventory labels
                                ));

                                // Crate slots grid (9x3 = 27 slots)
                                column.e(new Grid(9, 0, 0), crateGrid -> {
                                    for (int i = 0; i < 27; i++) {
                                        crateGrid.e(new ComposedSlot(this.menu.getSlot(i)));
                                    }
                                });

                                // "Inventory" label - no shadow for Minecraft inventory style
                                column.e(new ElementLabel(
                                        Component.literal("Inventory"),
                                        0x404040,
                                        false  // No shadow for inventory labels
                                ));

                                // Player inventory grid (9x3 = 27 slots)
                                column.e(new Grid(9, 0, 0), playerGrid -> {
                                    for (int i = 0; i < 27; i++) {
                                        playerGrid.e(new ComposedSlot(this.menu.getSlot(27 + i)));
                                    }
                                });

                                // Spacer before hotbar
                                column.e(new ElementSpacer(0, 4));

                                // Hotbar grid (9x1 = 9 slots)
                                column.e(new Grid(9, 0, 0), hotbarGrid -> {
                                    for (int i = 0; i < 9; i++) {
                                        hotbarGrid.e(new ComposedSlot(this.menu.getSlot(54 + i)));
                                    }
                                });
                            });
                        });
                    });
                });

                // Scrollable list of buttons on the right side
                row.e(new ElementBox(), scrollBox -> {
                    scrollBox.layout().fixedSize(120, 190); // Match height of main UI

                    scrollBox.fillSlot(ElementBox.CONTENT_SLOT, content -> {
                        content.e(new Box(), paddedBox -> {
                            paddedBox.layout().padding(4);

                            // ScrollArea containing the list of buttons
                            paddedBox.e(new ScrollArea(), scrollArea -> {
                                scrollArea.getElement().scrollSpeed(10);

                                // Fill scroll area's content slot with column of buttons
                                scrollArea.fillSlot(ScrollArea.CONTENT_SLOT, scrollContent -> {
                                    scrollContent.e(new Column(), buttonColumn -> {
                                        buttonColumn.layout().spacing(2);

                                        // Create 50 buttons with labels
                                        for (int i = 1; i <= 50; i++) {
                                            final int buttonIndex = i;
                                            buttonColumn.e(new Button(), button -> {
                                                button.layout()
                                                        .fillMaxWidth()
                                                        .fixedHeight(20);

                                                button.getElement().addPressListener((x, y) -> {
                                                    System.out.println("Clicked button " + buttonIndex);
                                                });

                                                // Add label to the button via slot
                                                button.fillSlot(Button.CONTENT_SLOT, buttonContent -> {
                                                    buttonContent.e(new ElementLabel(
                                                            Component.literal("Button " + buttonIndex),
                                                            0xFFFFFF,
                                                            true
                                                    ));
                                                });
                                            });
                                        }
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });

        // 3. Layout
        // We assume a screen size of 800x600 for the test
        var rootConstraints = new Constraints(0, 800, 0, 600);
        tree.measureTree(rootConstraints);
        tree.placeTree(new Position(0, 0), rootConstraints);

        // 4. Assertions
        assertNotNull(tree.getRoot());
        // Root Stack has one child (Row) because Stack wraps the Row
        assertEquals(1, tree.getRoot().getChildren().size());
        ITreeNode stackNode = tree.getRoot();
        ITreeNode rowNode = stackNode.getChildren().getFirst();
        assertEquals(Row.class, rowNode.getElement().getClass());

        // Row should have 2 children: Main UI ElementBox and ScrollArea ElementBox
        assertEquals(2, rowNode.getChildren().size());
        
        // 5. Assertions - Layout & Positioning
        // Screen: 800x600
        // Row Content: 178 (Main) + 8 (Gap) + 120 (Scroll) = 306 width
        // Row Height: 190
        // Centered X: (800 - 306) / 2 = 247
        // Centered Y: (600 - 190) / 2 = 205

        // --- Verify Main UI Box ---
        ITreeNode mainBoxNode = rowNode.getChildren().get(0);
        assertEquals(ElementBox.class, mainBoxNode.getElement().getClass());
        Bounds mainBoxBounds = mainBoxNode.getElement().getBounds();
        assertEquals(new Bounds(247, 205, 178, 190), mainBoxBounds, "Main Box should be centered");

        // Verify content of Main UI Box
        // ElementBox -> Stack -> Box (padding) -> Column
        ITreeNode mainStackNode = mainBoxNode.getChildren().getFirst();
        ITreeNode paddedContentNode = mainStackNode.getChildren().stream()
                .filter(n -> n.getElement() instanceof Box)
                .findFirst().orElseThrow();
        
        ITreeNode columnNode = paddedContentNode.getChildren().stream()
                .filter(n -> n.getElement() instanceof Column)
                .findFirst().orElseThrow();

        // Column Position: MainBox X + Padding (8), MainBox Y + Padding (8)
        // X = 247 + 8 = 255
        // Y = 205 + 8 = 213
        Bounds columnBounds = columnNode.getElement().getBounds();
        assertEquals(255, columnBounds.x(), "Column X position");
        assertEquals(213, columnBounds.y(), "Column Y position");

        // Column Children Layout
        // 1. "Crate" Label
        ITreeNode crateLabelNode = columnNode.getChildren().get(0);
        Bounds crateLabelBounds = crateLabelNode.getElement().getBounds();
        assertEquals(255, crateLabelBounds.x());
        assertEquals(213, crateLabelBounds.y());
        assertEquals(9, crateLabelBounds.height()); // Default font height

        // 2. Crate Grid (9x3)
        // Y = 213 + 9 (Label) + 4 (Spacing) = 226
        ITreeNode crateGridNode = columnNode.getChildren().get(1);
        Bounds crateGridBounds = crateGridNode.getElement().getBounds();
        assertEquals(255, crateGridBounds.x());
        assertEquals(226, crateGridBounds.y());
        assertEquals(162, crateGridBounds.width()); // 9 * 18
        assertEquals(54, crateGridBounds.height()); // 3 * 18

        // 3. "Inventory" Label
        // Y = 226 + 54 (Grid) + 4 (Spacing) = 284
        ITreeNode invLabelNode = columnNode.getChildren().get(2);
        Bounds invLabelBounds = invLabelNode.getElement().getBounds();
        assertEquals(255, invLabelBounds.x());
        assertEquals(284, invLabelBounds.y());

        // 4. Player Grid (9x3)
        // Y = 284 + 9 (Label) + 4 (Spacing) = 297
        ITreeNode playerGridNode = columnNode.getChildren().get(3);
        Bounds playerGridBounds = playerGridNode.getElement().getBounds();
        assertEquals(255, playerGridBounds.x());
        assertEquals(297, playerGridBounds.y());

        // 5. Spacer (Height 4)
        // Y = 297 + 54 (Grid) + 4 (Spacing) = 355
        ITreeNode spacerNode = columnNode.getChildren().get(4);
        Bounds spacerBounds = spacerNode.getElement().getBounds();
        assertEquals(255, spacerBounds.x());
        assertEquals(355, spacerBounds.y());
        assertEquals(4, spacerBounds.height());

        // 6. Hotbar Grid (9x1)
        // Y = 355 + 4 (Spacer) + 4 (Spacing) = 363
        ITreeNode hotbarGridNode = columnNode.getChildren().get(5);
        Bounds hotbarGridBounds = hotbarGridNode.getElement().getBounds();
        assertEquals(255, hotbarGridBounds.x());
        assertEquals(363, hotbarGridBounds.y());
        assertEquals(18, hotbarGridBounds.height()); // 1 * 18

        // --- Verify Scroll UI Box ---
        ITreeNode scrollBoxNode = rowNode.getChildren().get(1);
        Bounds scrollBoxBounds = scrollBoxNode.getElement().getBounds();
        // X = 247 + 178 + 8 = 433
        assertEquals(new Bounds(433, 205, 120, 190), scrollBoxBounds, "Scroll Box position");
        
        // Verify ScrollArea content
        // ScrollBox -> Stack -> Box -> ScrollArea
        ITreeNode scrollStackNode = scrollBoxNode.getChildren().getFirst();
        ITreeNode scrollPaddedBoxNode = scrollStackNode.getChildren().stream()
                .filter(n -> n.getElement() instanceof Box)
                .findFirst().orElseThrow();
        
        ITreeNode scrollAreaNode = scrollPaddedBoxNode.getChildren().stream()
                .filter(n -> n.getElement() instanceof ScrollArea)
                .findFirst().orElseThrow();
        
        // ScrollArea Position: ScrollBox X + Padding (4), ScrollBox Y + Padding (4)
        // X = 433 + 4 = 437
        // Y = 205 + 4 = 209
        // Size: 120 - 8 = 112 width, 190 - 8 = 182 height
        Bounds scrollAreaBounds = scrollAreaNode.getElement().getBounds();
        assertEquals(new Bounds(437, 209, 112, 182), scrollAreaBounds, "ScrollArea bounds");

        // ScrollArea content is in a slot.
        // ScrollArea -> Box -> Column
        ITreeNode scrollInnerBoxNode = scrollAreaNode.getChildren().getFirst();
        ITreeNode scrollContentColumnNode = scrollInnerBoxNode.getChildren().stream()
                .filter(n -> n.getElement() instanceof Column)
                .findFirst().orElseThrow();
        
        assertEquals(50, scrollContentColumnNode.getChildren().size());
        assertEquals(Button.class, scrollContentColumnNode.getChildren().get(0).getElement().getClass());

        // 5. Rendering
        tree.renderTree(this.screenContext);

        // 6. Verify render calls
        // Verify "Crate" label was drawn (no shadow)
        // Verify "Inventory" label was drawn (no shadow)
        // Verify buttons labels (shadow)
        
        ArgumentCaptor<Component> textCaptor = ArgumentCaptor.forClass(Component.class);
        ArgumentCaptor<Float> xCaptor = ArgumentCaptor.forClass(Float.class);
        ArgumentCaptor<Float> yCaptor = ArgumentCaptor.forClass(Float.class);

        // "Crate" and "Inventory" are drawn with drawText (no shadow)
        verify((IPrimitiveScreenContext) this.screenContext, atLeast(2)).drawText(textCaptor.capture(), xCaptor.capture(), yCaptor.capture());
        
        List<Component> capturedTexts = textCaptor.getAllValues();
        boolean foundCrate = capturedTexts.stream().anyMatch(c -> c.getString().equals("Crate"));
        boolean foundInventory = capturedTexts.stream().anyMatch(c -> c.getString().equals("Inventory"));
        assertTrue(foundCrate, "Should have rendered 'Crate' label");
        assertTrue(foundInventory, "Should have rendered 'Inventory' label");
        
        // Buttons use drawTextWithShadow
        // We have 50 buttons.
        // verify((IPrimitiveScreenContext) this.screenContext, atLeast(50)).drawTextWithShadow(any(), anyFloat(), anyFloat());
    }
}
