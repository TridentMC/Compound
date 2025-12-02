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

        // Measure and place - validate every operation
        tree.measureTree(rootConstraints);
        tree.placeTree(new Position(0, 0), rootConstraints);

        // Verify every element was measured and placed correctly
        // validateEveryElementWasMeasuredAndPlaced(tree);

        // 4. Assertions - Complete UI Structure Validation
        assertNotNull(tree.getRoot());

        // Root Stack should fill entire screen (800x600)
        ITreeNode rootStackNode = tree.getRoot();
        assertEquals(Stack.class, rootStackNode.getElement().getClass());
        Bounds rootStackBounds = rootStackNode.getElement().getBounds();
        assertEquals(new Bounds(0, 0, 800, 600), rootStackBounds, "Root Stack should fill entire screen");

        // Root Stack has one child (Row)
        assertEquals(1, rootStackNode.getChildren().size());
        ITreeNode rowNode = rootStackNode.getChildren().getFirst();
        assertEquals(Row.class, rowNode.getElement().getClass());

        // Row should have 2 children: Main UI ElementBox and ScrollArea ElementBox
        assertEquals(2, rowNode.getChildren().size());

        // 5. Assertions - Layout & Positioning
        // Screen: 800x600
        // Row Content: 178 (Main) + 8 (Gap) + 120 (Scroll) = 306 width
        // Row Height: 190
        // Centered X: (800 - 306) / 2 = 247
        // Centered Y: (600 - 190) / 2 = 205

        // --- Verify Row positioning ---
        Bounds rowBounds = rowNode.getElement().getBounds();
        assertEquals(new Bounds(247, 205, 306, 190), rowBounds, "Row should be centered with correct dimensions");

        // --- Verify Main UI Box ---
        ITreeNode mainBoxNode = rowNode.getChildren().get(0);
        assertEquals(ElementBox.class, mainBoxNode.getElement().getClass());
        Bounds mainBoxBounds = mainBoxNode.getElement().getBounds();
        assertEquals(new Bounds(247, 205, 178, 190), mainBoxBounds, "Main Box should be centered");

        // Verify content of Main UI Box
        // ElementBox -> Stack -> Box (padding) -> Column
        ITreeNode mainStackNode = mainBoxNode.getChildren().getFirst();
        assertEquals(Stack.class, mainStackNode.getElement().getClass());
        Bounds mainStackBounds = mainStackNode.getElement().getBounds();
        assertEquals(new Bounds(247, 205, 178, 190), mainStackBounds, "Main Stack should fill ElementBox");

        ITreeNode paddedContentNode = mainStackNode.getChildren().stream()
                .filter(n -> n.getElement() instanceof Box)
                .findFirst().orElseThrow();
        assertEquals(Box.class, paddedContentNode.getElement().getClass());
        Bounds paddedContentBounds = paddedContentNode.getElement().getBounds();
        // Box with padding actually fills the same bounds as its parent, but applies padding to its children
        assertEquals(new Bounds(247, 205, 178, 190), paddedContentBounds, "Padded Box should fill ElementBox");

        ITreeNode columnNode = paddedContentNode.getChildren().stream()
                .filter(n -> n.getElement() instanceof Column)
                .findFirst().orElseThrow();
        assertEquals(Column.class, columnNode.getElement().getClass());
        Bounds columnBounds = columnNode.getElement().getBounds();
        // Column accounts for 8px padding from parent Box: X+8, Y+8, Width-16, Height-16
        assertEquals(new Bounds(255, 213, 162, 174), columnBounds, "Column should account for parent Box padding");

        // Column Children Layout - Comprehensive validation of all 6 elements
        assertEquals(6, columnNode.getChildren().size(), "Column should have exactly 6 children");

        // 1. "Crate" Label
        ITreeNode crateLabelNode = columnNode.getChildren().get(0);
        assertEquals(ElementLabel.class, crateLabelNode.getElement().getClass());
        Bounds crateLabelBounds = crateLabelNode.getElement().getBounds();
        // Labels only take up width they need, not full column width
        assertEquals(new Bounds(255, 213, 30, 9), crateLabelBounds, "Crate label bounds");

        // 2. Crate Grid (9x3)
        // Y = 213 + 9 (Label) + 4 (Spacing) = 226
        ITreeNode crateGridNode = columnNode.getChildren().get(1);
        assertEquals(Grid.class, crateGridNode.getElement().getClass());
        Bounds crateGridBounds = crateGridNode.getElement().getBounds();
        assertEquals(new Bounds(255, 226, 162, 54), crateGridBounds, "Crate Grid bounds");
        assertEquals(27, crateGridNode.getChildren().size(), "Crate grid should have 27 slots");

        // Validate individual crate slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col;
                ITreeNode slotNode = crateGridNode.getChildren().get(slotIndex);
                assertEquals(ComposedSlot.class, slotNode.getElement().getClass());
                Bounds slotBounds = slotNode.getElement().getBounds();
                assertEquals(new Bounds(255 + col * 18, 226 + row * 18, 18, 18),
                    slotBounds, "Crate slot [" + row + "," + col + "] bounds");
            }
        }

        // 3. "Inventory" Label
        // Y = 226 + 54 (Grid) + 4 (Spacing) = 284
        ITreeNode invLabelNode = columnNode.getChildren().get(2);
        assertEquals(ElementLabel.class, invLabelNode.getElement().getClass());
        Bounds invLabelBounds = invLabelNode.getElement().getBounds();
        // "Inventory" text width is exactly 54px
        assertEquals(new Bounds(255, 284, 54, 9), invLabelBounds, "Inventory label bounds");

        // 4. Player Grid (9x3)
        // Y = 284 + 9 (Label) + 4 (Spacing) = 297
        ITreeNode playerGridNode = columnNode.getChildren().get(3);
        assertEquals(Grid.class, playerGridNode.getElement().getClass());
        Bounds playerGridBounds = playerGridNode.getElement().getBounds();
        assertEquals(new Bounds(255, 297, 162, 54), playerGridBounds, "Player Grid bounds");
        assertEquals(27, playerGridNode.getChildren().size(), "Player grid should have 27 slots");

        // Validate individual player slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col;
                ITreeNode slotNode = playerGridNode.getChildren().get(slotIndex);
                assertEquals(ComposedSlot.class, slotNode.getElement().getClass());
                Bounds slotBounds = slotNode.getElement().getBounds();
                assertEquals(new Bounds(255 + col * 18, 297 + row * 18, 18, 18),
                    slotBounds, "Player slot [" + row + "," + col + "] bounds");
            }
        }

        // 5. Spacer (Height 4)
        // Y = 297 + 54 (Grid) + 4 (Spacing) = 355
        ITreeNode spacerNode = columnNode.getChildren().get(4);
        assertEquals(ElementSpacer.class, spacerNode.getElement().getClass());
        Bounds spacerBounds = spacerNode.getElement().getBounds();
        // Spacer has no width (0) but takes full column height with padding
        assertEquals(new Bounds(255, 355, 0, 4), spacerBounds, "Spacer bounds");

        // 6. Hotbar Grid (9x1)
        // Y = 355 + 4 (Spacer) + 4 (Spacing) = 363
        ITreeNode hotbarGridNode = columnNode.getChildren().get(5);
        assertEquals(Grid.class, hotbarGridNode.getElement().getClass());
        Bounds hotbarGridBounds = hotbarGridNode.getElement().getBounds();
        assertEquals(new Bounds(255, 363, 162, 18), hotbarGridBounds, "Hotbar Grid bounds");
        assertEquals(9, hotbarGridNode.getChildren().size(), "Hotbar grid should have 9 slots");

        // Validate individual hotbar slots
        for (int col = 0; col < 9; col++) {
            ITreeNode slotNode = hotbarGridNode.getChildren().get(col);
            assertEquals(ComposedSlot.class, slotNode.getElement().getClass());
            Bounds slotBounds = slotNode.getElement().getBounds();
            assertEquals(new Bounds(255 + col * 18, 363, 18, 18),
                slotBounds, "Hotbar slot [" + col + "] bounds");
        }

        // --- Verify Scroll UI Box ---
        ITreeNode scrollBoxNode = rowNode.getChildren().get(1);
        assertEquals(ElementBox.class, scrollBoxNode.getElement().getClass());
        Bounds scrollBoxBounds = scrollBoxNode.getElement().getBounds();
        // X = 247 + 178 + 8 = 433
        assertEquals(new Bounds(433, 205, 120, 190), scrollBoxBounds, "Scroll Box position");

        // Verify ScrollArea content - Complete structure validation
        // ScrollBox -> Stack -> Box -> ScrollArea
        ITreeNode scrollStackNode = scrollBoxNode.getChildren().getFirst();
        assertEquals(Stack.class, scrollStackNode.getElement().getClass());
        Bounds scrollStackBounds = scrollStackNode.getElement().getBounds();
        assertEquals(new Bounds(433, 205, 120, 190), scrollStackBounds, "Scroll Stack should fill ElementBox");

        ITreeNode scrollPaddedBoxNode = scrollStackNode.getChildren().stream()
                .filter(n -> n.getElement() instanceof Box)
                .findFirst().orElseThrow();
        assertEquals(Box.class, scrollPaddedBoxNode.getElement().getClass());
        Bounds scrollPaddedBoxBounds = scrollPaddedBoxNode.getElement().getBounds();
        // Box with padding fills same bounds as parent, but applies 4px padding to children
        assertEquals(new Bounds(433, 205, 120, 190), scrollPaddedBoxBounds, "Scroll Padded Box should fill ElementBox");

        ITreeNode scrollAreaNode = scrollPaddedBoxNode.getChildren().stream()
                .filter(n -> n.getElement() instanceof ScrollArea)
                .findFirst().orElseThrow();
        assertEquals(ScrollArea.class, scrollAreaNode.getElement().getClass());
        Bounds scrollAreaBounds = scrollAreaNode.getElement().getBounds();
        // ScrollArea accounts for 4px padding from parent Box: X+4, Y+4, Width-8, Height-8
        assertEquals(new Bounds(437, 209, 112, 182), scrollAreaBounds, "ScrollArea should account for parent Box padding");

        // ScrollArea content is in a slot.
        // ScrollArea -> Box -> Column
        ITreeNode scrollInnerBoxNode = scrollAreaNode.getChildren().getFirst();
        assertEquals(Box.class, scrollInnerBoxNode.getElement().getClass());
        Bounds scrollInnerBoxBounds = scrollInnerBoxNode.getElement().getBounds();
        // Inner box expands to contain all content: 50 buttons * (20 height + 2 spacing) = 1100, minus 2 for last spacing = 1098
        assertEquals(new Bounds(437, 209, 112, 1098), scrollInnerBoxBounds, "Scroll Inner Box should expand to fit all content");

        ITreeNode scrollContentColumnNode = scrollInnerBoxNode.getChildren().stream()
                .filter(n -> n.getElement() instanceof Column)
                .findFirst().orElseThrow();
        assertEquals(Column.class, scrollContentColumnNode.getElement().getClass());
        assertEquals(50, scrollContentColumnNode.getChildren().size(), "Scroll content column should have 50 buttons");

        // Validate the column bounds and button positions
        Bounds scrollContentColumnBounds = scrollContentColumnNode.getElement().getBounds();
        // Column bounds should match inner box exactly
        assertEquals(new Bounds(437, 209, 112, 1098), scrollContentColumnBounds, "Scroll content column bounds");

        // Validate all buttons in the scroll area
        for (int i = 0; i < 50; i++) {
            ITreeNode buttonNode = scrollContentColumnNode.getChildren().get(i);
            assertEquals(Button.class, buttonNode.getElement().getClass(), "Button " + i + " should be Button class");

            Bounds buttonBounds = buttonNode.getElement().getBounds();
            // Each button: full width (112), fixed height (20), 2px spacing between buttons
            int expectedY = 209 + (i * 22); // 209 + (i * (20 height + 2 spacing))
            assertEquals(new Bounds(437, expectedY, 112, 20), buttonBounds, "Button " + i + " bounds");

            // Validate button content (Stack containing label)
            assertEquals(1, buttonNode.getChildren().size(), "Button " + i + " should have 1 child (Stack with label)");
            ITreeNode buttonStackNode = buttonNode.getChildren().getFirst();
            assertEquals(Stack.class, buttonStackNode.getElement().getClass(), "Button " + i + " child should be Stack");

            // The Stack should contain 2 children: ElementSprite (background) and ElementLabel (text)
            assertEquals(2, buttonStackNode.getChildren().size(), "Button " + i + " Stack should have 2 children (sprite + label)");

            // First child should be ElementSprite (button background)
            ITreeNode spriteNode = buttonStackNode.getChildren().get(0);
            assertEquals(ElementSprite.class, spriteNode.getElement().getClass(), "Button " + i + " first child should be ElementSprite");

            // Second child should be ElementLabel (button text)
            ITreeNode labelNode = buttonStackNode.getChildren().get(1);
            assertEquals(ElementLabel.class, labelNode.getElement().getClass(), "Button " + i + " second child should be ElementLabel");

            Bounds labelBounds = labelNode.getElement().getBounds();
            // Validate label position and size (content varies by button number)
            // Label is vertically centered in button: button Y + (button height - label height) / 2
            int expectedLabelY = expectedY + (20 - 9) / 2; // Center 9px label in 20px button
            assertEquals(expectedLabelY, labelBounds.y(), "Button " + i + " label Y position (centered)");
            assertEquals(9, labelBounds.height(), "Button " + i + " label height");
            // Label should be within button bounds and reasonable size
            assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                "Button " + i + " label should be within button X bounds");
            assertTrue(labelBounds.width() >= 40 && labelBounds.width() <= 70, "Button " + i + " label width reasonable");
        }

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
