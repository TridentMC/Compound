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
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith({MinecraftMockExtension.class, MockitoExtension.class})
public class CrateUIIntegrationTest {

    @Mock
    private AbstractContainerMenu menu;

    @Mock
    private IScreenContext screenContext;

    // Gold bounds for regression testing - captured from working implementation
    // These represent expected positions/dimensions that should remain stable
    private static final Bounds GOLD_ROOT_BOUNDS = new Bounds(0, 0, 800, 600);
    private static final Bounds GOLD_ROW_BOUNDS = new Bounds(247, 205, 306, 190);
    private static final Bounds GOLD_MAIN_BOX_BOUNDS = new Bounds(247, 205, 178, 190);
    private static final Bounds GOLD_MAIN_STACK_BOUNDS = new Bounds(247, 205, 178, 190);
    private static final Bounds GOLD_PADDED_CONTENT_BOUNDS = new Bounds(247, 205, 178, 190);
    private static final Bounds GOLD_COLUMN_BOUNDS = new Bounds(255, 213, 162, 174);
    private static final Bounds GOLD_CRATE_LABEL_BOUNDS = new Bounds(255, 213, 30, 9);
    private static final Bounds GOLD_CRATE_GRID_BOUNDS = new Bounds(255, 226, 162, 54);
    private static final Bounds GOLD_INVENTORY_LABEL_BOUNDS = new Bounds(255, 284, 54, 9);
    private static final Bounds GOLD_PLAYER_GRID_BOUNDS = new Bounds(255, 297, 162, 54);
    private static final Bounds GOLD_SPACER_BOUNDS = new Bounds(255, 355, 0, 4);
    private static final Bounds GOLD_HOTBAR_GRID_BOUNDS = new Bounds(255, 363, 162, 18);
    private static final Bounds GOLD_SCROLL_BOX_BOUNDS = new Bounds(433, 205, 120, 190);
    private static final Bounds GOLD_SCROLL_STACK_BOUNDS = new Bounds(433, 205, 120, 190);
    private static final Bounds GOLD_SCROLL_PADDED_BOUNDS = new Bounds(433, 205, 120, 190);
    private static final Bounds GOLD_SCROLL_AREA_BOUNDS = new Bounds(437, 209, 112, 182);
    private static final Bounds GOLD_SCROLL_INNER_BOX_BOUNDS = new Bounds(437, 209, 112, 1098);
    private static final Bounds GOLD_SCROLL_COLUMN_BOUNDS = new Bounds(437, 209, 112, 1098);

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
                switch (row * 9 + col) {
                    case 0 -> assertEquals(new Bounds(255, 226, 18, 18), slotBounds, "Crate slot [0,0] bounds");
                    case 1 -> assertEquals(new Bounds(273, 226, 18, 18), slotBounds, "Crate slot [0,1] bounds");
                    case 2 -> assertEquals(new Bounds(291, 226, 18, 18), slotBounds, "Crate slot [0,2] bounds");
                    case 3 -> assertEquals(new Bounds(309, 226, 18, 18), slotBounds, "Crate slot [0,3] bounds");
                    case 4 -> assertEquals(new Bounds(327, 226, 18, 18), slotBounds, "Crate slot [0,4] bounds");
                    case 5 -> assertEquals(new Bounds(345, 226, 18, 18), slotBounds, "Crate slot [0,5] bounds");
                    case 6 -> assertEquals(new Bounds(363, 226, 18, 18), slotBounds, "Crate slot [0,6] bounds");
                    case 7 -> assertEquals(new Bounds(381, 226, 18, 18), slotBounds, "Crate slot [0,7] bounds");
                    case 8 -> assertEquals(new Bounds(399, 226, 18, 18), slotBounds, "Crate slot [0,8] bounds");
                    case 9 -> assertEquals(new Bounds(255, 244, 18, 18), slotBounds, "Crate slot [1,0] bounds");
                    case 10 -> assertEquals(new Bounds(273, 244, 18, 18), slotBounds, "Crate slot [1,1] bounds");
                    case 11 -> assertEquals(new Bounds(291, 244, 18, 18), slotBounds, "Crate slot [1,2] bounds");
                    case 12 -> assertEquals(new Bounds(309, 244, 18, 18), slotBounds, "Crate slot [1,3] bounds");
                    case 13 -> assertEquals(new Bounds(327, 244, 18, 18), slotBounds, "Crate slot [1,4] bounds");
                    case 14 -> assertEquals(new Bounds(345, 244, 18, 18), slotBounds, "Crate slot [1,5] bounds");
                    case 15 -> assertEquals(new Bounds(363, 244, 18, 18), slotBounds, "Crate slot [1,6] bounds");
                    case 16 -> assertEquals(new Bounds(381, 244, 18, 18), slotBounds, "Crate slot [1,7] bounds");
                    case 17 -> assertEquals(new Bounds(399, 244, 18, 18), slotBounds, "Crate slot [1,8] bounds");
                    case 18 -> assertEquals(new Bounds(255, 262, 18, 18), slotBounds, "Crate slot [2,0] bounds");
                    case 19 -> assertEquals(new Bounds(273, 262, 18, 18), slotBounds, "Crate slot [2,1] bounds");
                    case 20 -> assertEquals(new Bounds(291, 262, 18, 18), slotBounds, "Crate slot [2,2] bounds");
                    case 21 -> assertEquals(new Bounds(309, 262, 18, 18), slotBounds, "Crate slot [2,3] bounds");
                    case 22 -> assertEquals(new Bounds(327, 262, 18, 18), slotBounds, "Crate slot [2,4] bounds");
                    case 23 -> assertEquals(new Bounds(345, 262, 18, 18), slotBounds, "Crate slot [2,5] bounds");
                    case 24 -> assertEquals(new Bounds(363, 262, 18, 18), slotBounds, "Crate slot [2,6] bounds");
                    case 25 -> assertEquals(new Bounds(381, 262, 18, 18), slotBounds, "Crate slot [2,7] bounds");
                    case 26 -> assertEquals(new Bounds(399, 262, 18, 18), slotBounds, "Crate slot [2,8] bounds");
                    default -> fail("Unexpected slot index: " + (row * 9 + col));
                }
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
                switch (row * 9 + col) {
                    case 0 -> assertEquals(new Bounds(255, 297, 18, 18), slotBounds, "Player slot [0,0] bounds");
                    case 1 -> assertEquals(new Bounds(273, 297, 18, 18), slotBounds, "Player slot [0,1] bounds");
                    case 2 -> assertEquals(new Bounds(291, 297, 18, 18), slotBounds, "Player slot [0,2] bounds");
                    case 3 -> assertEquals(new Bounds(309, 297, 18, 18), slotBounds, "Player slot [0,3] bounds");
                    case 4 -> assertEquals(new Bounds(327, 297, 18, 18), slotBounds, "Player slot [0,4] bounds");
                    case 5 -> assertEquals(new Bounds(345, 297, 18, 18), slotBounds, "Player slot [0,5] bounds");
                    case 6 -> assertEquals(new Bounds(363, 297, 18, 18), slotBounds, "Player slot [0,6] bounds");
                    case 7 -> assertEquals(new Bounds(381, 297, 18, 18), slotBounds, "Player slot [0,7] bounds");
                    case 8 -> assertEquals(new Bounds(399, 297, 18, 18), slotBounds, "Player slot [0,8] bounds");
                    case 9 -> assertEquals(new Bounds(255, 315, 18, 18), slotBounds, "Player slot [1,0] bounds");
                    case 10 -> assertEquals(new Bounds(273, 315, 18, 18), slotBounds, "Player slot [1,1] bounds");
                    case 11 -> assertEquals(new Bounds(291, 315, 18, 18), slotBounds, "Player slot [1,2] bounds");
                    case 12 -> assertEquals(new Bounds(309, 315, 18, 18), slotBounds, "Player slot [1,3] bounds");
                    case 13 -> assertEquals(new Bounds(327, 315, 18, 18), slotBounds, "Player slot [1,4] bounds");
                    case 14 -> assertEquals(new Bounds(345, 315, 18, 18), slotBounds, "Player slot [1,5] bounds");
                    case 15 -> assertEquals(new Bounds(363, 315, 18, 18), slotBounds, "Player slot [1,6] bounds");
                    case 16 -> assertEquals(new Bounds(381, 315, 18, 18), slotBounds, "Player slot [1,7] bounds");
                    case 17 -> assertEquals(new Bounds(399, 315, 18, 18), slotBounds, "Player slot [1,8] bounds");
                    case 18 -> assertEquals(new Bounds(255, 333, 18, 18), slotBounds, "Player slot [2,0] bounds");
                    case 19 -> assertEquals(new Bounds(273, 333, 18, 18), slotBounds, "Player slot [2,1] bounds");
                    case 20 -> assertEquals(new Bounds(291, 333, 18, 18), slotBounds, "Player slot [2,2] bounds");
                    case 21 -> assertEquals(new Bounds(309, 333, 18, 18), slotBounds, "Player slot [2,3] bounds");
                    case 22 -> assertEquals(new Bounds(327, 333, 18, 18), slotBounds, "Player slot [2,4] bounds");
                    case 23 -> assertEquals(new Bounds(345, 333, 18, 18), slotBounds, "Player slot [2,5] bounds");
                    case 24 -> assertEquals(new Bounds(363, 333, 18, 18), slotBounds, "Player slot [2,6] bounds");
                    case 25 -> assertEquals(new Bounds(381, 333, 18, 18), slotBounds, "Player slot [2,7] bounds");
                    case 26 -> assertEquals(new Bounds(399, 333, 18, 18), slotBounds, "Player slot [2,8] bounds");
                    default -> fail("Unexpected slot index: " + (row * 9 + col));
                }
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
            switch (col) {
                case 0 -> assertEquals(new Bounds(255, 363, 18, 18), slotBounds, "Hotbar slot [0] bounds");
                case 1 -> assertEquals(new Bounds(273, 363, 18, 18), slotBounds, "Hotbar slot [1] bounds");
                case 2 -> assertEquals(new Bounds(291, 363, 18, 18), slotBounds, "Hotbar slot [2] bounds");
                case 3 -> assertEquals(new Bounds(309, 363, 18, 18), slotBounds, "Hotbar slot [3] bounds");
                case 4 -> assertEquals(new Bounds(327, 363, 18, 18), slotBounds, "Hotbar slot [4] bounds");
                case 5 -> assertEquals(new Bounds(345, 363, 18, 18), slotBounds, "Hotbar slot [5] bounds");
                case 6 -> assertEquals(new Bounds(363, 363, 18, 18), slotBounds, "Hotbar slot [6] bounds");
                case 7 -> assertEquals(new Bounds(381, 363, 18, 18), slotBounds, "Hotbar slot [7] bounds");
                case 8 -> assertEquals(new Bounds(399, 363, 18, 18), slotBounds, "Hotbar slot [8] bounds");
                default -> fail("Unexpected slot index: " + col);
            }
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
            switch (i) {
                case 0 -> assertEquals(new Bounds(437, 209, 112, 20), buttonBounds, "Button 0 bounds");
                case 1 -> assertEquals(new Bounds(437, 231, 112, 20), buttonBounds, "Button 1 bounds");
                case 2 -> assertEquals(new Bounds(437, 253, 112, 20), buttonBounds, "Button 2 bounds");
                case 3 -> assertEquals(new Bounds(437, 275, 112, 20), buttonBounds, "Button 3 bounds");
                case 4 -> assertEquals(new Bounds(437, 297, 112, 20), buttonBounds, "Button 4 bounds");
                case 5 -> assertEquals(new Bounds(437, 319, 112, 20), buttonBounds, "Button 5 bounds");
                case 6 -> assertEquals(new Bounds(437, 341, 112, 20), buttonBounds, "Button 6 bounds");
                case 7 -> assertEquals(new Bounds(437, 363, 112, 20), buttonBounds, "Button 7 bounds");
                case 8 -> assertEquals(new Bounds(437, 385, 112, 20), buttonBounds, "Button 8 bounds");
                case 9 -> assertEquals(new Bounds(437, 407, 112, 20), buttonBounds, "Button 9 bounds");
                case 10 -> assertEquals(new Bounds(437, 429, 112, 20), buttonBounds, "Button 10 bounds");
                case 11 -> assertEquals(new Bounds(437, 451, 112, 20), buttonBounds, "Button 11 bounds");
                case 12 -> assertEquals(new Bounds(437, 473, 112, 20), buttonBounds, "Button 12 bounds");
                case 13 -> assertEquals(new Bounds(437, 495, 112, 20), buttonBounds, "Button 13 bounds");
                case 14 -> assertEquals(new Bounds(437, 517, 112, 20), buttonBounds, "Button 14 bounds");
                case 15 -> assertEquals(new Bounds(437, 539, 112, 20), buttonBounds, "Button 15 bounds");
                case 16 -> assertEquals(new Bounds(437, 561, 112, 20), buttonBounds, "Button 16 bounds");
                case 17 -> assertEquals(new Bounds(437, 583, 112, 20), buttonBounds, "Button 17 bounds");
                case 18 -> assertEquals(new Bounds(437, 605, 112, 20), buttonBounds, "Button 18 bounds");
                case 19 -> assertEquals(new Bounds(437, 627, 112, 20), buttonBounds, "Button 19 bounds");
                case 20 -> assertEquals(new Bounds(437, 649, 112, 20), buttonBounds, "Button 20 bounds");
                case 21 -> assertEquals(new Bounds(437, 671, 112, 20), buttonBounds, "Button 21 bounds");
                case 22 -> assertEquals(new Bounds(437, 693, 112, 20), buttonBounds, "Button 22 bounds");
                case 23 -> assertEquals(new Bounds(437, 715, 112, 20), buttonBounds, "Button 23 bounds");
                case 24 -> assertEquals(new Bounds(437, 737, 112, 20), buttonBounds, "Button 24 bounds");
                case 25 -> assertEquals(new Bounds(437, 759, 112, 20), buttonBounds, "Button 25 bounds");
                case 26 -> assertEquals(new Bounds(437, 781, 112, 20), buttonBounds, "Button 26 bounds");
                case 27 -> assertEquals(new Bounds(437, 803, 112, 20), buttonBounds, "Button 27 bounds");
                case 28 -> assertEquals(new Bounds(437, 825, 112, 20), buttonBounds, "Button 28 bounds");
                case 29 -> assertEquals(new Bounds(437, 847, 112, 20), buttonBounds, "Button 29 bounds");
                case 30 -> assertEquals(new Bounds(437, 869, 112, 20), buttonBounds, "Button 30 bounds");
                case 31 -> assertEquals(new Bounds(437, 891, 112, 20), buttonBounds, "Button 31 bounds");
                case 32 -> assertEquals(new Bounds(437, 913, 112, 20), buttonBounds, "Button 32 bounds");
                case 33 -> assertEquals(new Bounds(437, 935, 112, 20), buttonBounds, "Button 33 bounds");
                case 34 -> assertEquals(new Bounds(437, 957, 112, 20), buttonBounds, "Button 34 bounds");
                case 35 -> assertEquals(new Bounds(437, 979, 112, 20), buttonBounds, "Button 35 bounds");
                case 36 -> assertEquals(new Bounds(437, 1001, 112, 20), buttonBounds, "Button 36 bounds");
                case 37 -> assertEquals(new Bounds(437, 1023, 112, 20), buttonBounds, "Button 37 bounds");
                case 38 -> assertEquals(new Bounds(437, 1045, 112, 20), buttonBounds, "Button 38 bounds");
                case 39 -> assertEquals(new Bounds(437, 1067, 112, 20), buttonBounds, "Button 39 bounds");
                case 40 -> assertEquals(new Bounds(437, 1089, 112, 20), buttonBounds, "Button 40 bounds");
                case 41 -> assertEquals(new Bounds(437, 1111, 112, 20), buttonBounds, "Button 41 bounds");
                case 42 -> assertEquals(new Bounds(437, 1133, 112, 20), buttonBounds, "Button 42 bounds");
                case 43 -> assertEquals(new Bounds(437, 1155, 112, 20), buttonBounds, "Button 43 bounds");
                case 44 -> assertEquals(new Bounds(437, 1177, 112, 20), buttonBounds, "Button 44 bounds");
                case 45 -> assertEquals(new Bounds(437, 1199, 112, 20), buttonBounds, "Button 45 bounds");
                case 46 -> assertEquals(new Bounds(437, 1221, 112, 20), buttonBounds, "Button 46 bounds");
                case 47 -> assertEquals(new Bounds(437, 1243, 112, 20), buttonBounds, "Button 47 bounds");
                case 48 -> assertEquals(new Bounds(437, 1265, 112, 20), buttonBounds, "Button 48 bounds");
                case 49 -> assertEquals(new Bounds(437, 1287, 112, 20), buttonBounds, "Button 49 bounds");
                default -> fail("Unexpected button index: " + i);
            }

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
            // Label is vertically centered in button
            switch (i) {
                case 0 -> assertEquals(214, labelBounds.y(), "Button 0 label Y position (centered)");
                case 1 -> assertEquals(236, labelBounds.y(), "Button 1 label Y position (centered)");
                case 2 -> assertEquals(258, labelBounds.y(), "Button 2 label Y position (centered)");
                case 3 -> assertEquals(280, labelBounds.y(), "Button 3 label Y position (centered)");
                case 4 -> assertEquals(302, labelBounds.y(), "Button 4 label Y position (centered)");
                case 5 -> assertEquals(324, labelBounds.y(), "Button 5 label Y position (centered)");
                case 6 -> assertEquals(346, labelBounds.y(), "Button 6 label Y position (centered)");
                case 7 -> assertEquals(368, labelBounds.y(), "Button 7 label Y position (centered)");
                case 8 -> assertEquals(390, labelBounds.y(), "Button 8 label Y position (centered)");
                case 9 -> assertEquals(412, labelBounds.y(), "Button 9 label Y position (centered)");
                case 10 -> assertEquals(434, labelBounds.y(), "Button 10 label Y position (centered)");
                case 11 -> assertEquals(456, labelBounds.y(), "Button 11 label Y position (centered)");
                case 12 -> assertEquals(478, labelBounds.y(), "Button 12 label Y position (centered)");
                case 13 -> assertEquals(500, labelBounds.y(), "Button 13 label Y position (centered)");
                case 14 -> assertEquals(522, labelBounds.y(), "Button 14 label Y position (centered)");
                case 15 -> assertEquals(544, labelBounds.y(), "Button 15 label Y position (centered)");
                case 16 -> assertEquals(566, labelBounds.y(), "Button 16 label Y position (centered)");
                case 17 -> assertEquals(588, labelBounds.y(), "Button 17 label Y position (centered)");
                case 18 -> assertEquals(610, labelBounds.y(), "Button 18 label Y position (centered)");
                case 19 -> assertEquals(632, labelBounds.y(), "Button 19 label Y position (centered)");
                case 20 -> assertEquals(654, labelBounds.y(), "Button 20 label Y position (centered)");
                case 21 -> assertEquals(676, labelBounds.y(), "Button 21 label Y position (centered)");
                case 22 -> assertEquals(698, labelBounds.y(), "Button 22 label Y position (centered)");
                case 23 -> assertEquals(720, labelBounds.y(), "Button 23 label Y position (centered)");
                case 24 -> assertEquals(742, labelBounds.y(), "Button 24 label Y position (centered)");
                case 25 -> assertEquals(764, labelBounds.y(), "Button 25 label Y position (centered)");
                case 26 -> assertEquals(786, labelBounds.y(), "Button 26 label Y position (centered)");
                case 27 -> assertEquals(808, labelBounds.y(), "Button 27 label Y position (centered)");
                case 28 -> assertEquals(830, labelBounds.y(), "Button 28 label Y position (centered)");
                case 29 -> assertEquals(852, labelBounds.y(), "Button 29 label Y position (centered)");
                case 30 -> assertEquals(874, labelBounds.y(), "Button 30 label Y position (centered)");
                case 31 -> assertEquals(896, labelBounds.y(), "Button 31 label Y position (centered)");
                case 32 -> assertEquals(918, labelBounds.y(), "Button 32 label Y position (centered)");
                case 33 -> assertEquals(940, labelBounds.y(), "Button 33 label Y position (centered)");
                case 34 -> assertEquals(962, labelBounds.y(), "Button 34 label Y position (centered)");
                case 35 -> assertEquals(984, labelBounds.y(), "Button 35 label Y position (centered)");
                case 36 -> assertEquals(1006, labelBounds.y(), "Button 36 label Y position (centered)");
                case 37 -> assertEquals(1028, labelBounds.y(), "Button 37 label Y position (centered)");
                case 38 -> assertEquals(1050, labelBounds.y(), "Button 38 label Y position (centered)");
                case 39 -> assertEquals(1072, labelBounds.y(), "Button 39 label Y position (centered)");
                case 40 -> assertEquals(1094, labelBounds.y(), "Button 40 label Y position (centered)");
                case 41 -> assertEquals(1116, labelBounds.y(), "Button 41 label Y position (centered)");
                case 42 -> assertEquals(1138, labelBounds.y(), "Button 42 label Y position (centered)");
                case 43 -> assertEquals(1160, labelBounds.y(), "Button 43 label Y position (centered)");
                case 44 -> assertEquals(1182, labelBounds.y(), "Button 44 label Y position (centered)");
                case 45 -> assertEquals(1204, labelBounds.y(), "Button 45 label Y position (centered)");
                case 46 -> assertEquals(1226, labelBounds.y(), "Button 46 label Y position (centered)");
                case 47 -> assertEquals(1248, labelBounds.y(), "Button 47 label Y position (centered)");
                case 48 -> assertEquals(1270, labelBounds.y(), "Button 48 label Y position (centered)");
                case 49 -> assertEquals(1292, labelBounds.y(), "Button 49 label Y position (centered)");
                default -> fail("Unexpected button index: " + i);
            }
            assertEquals(9, labelBounds.height(), "Button " + i + " label height");
            // Label should be within button bounds and reasonable size
            switch (i) {
                // All buttons seem to have the same label width
                case 0 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 0 label should be within button X bounds");
                    assertEquals(48, labelBounds.width(), "Button 0 label width");
                }
                case 1 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 1 label should be within button X bounds");
                    assertEquals(48, labelBounds.width(), "Button 1 label width");
                }
                case 2 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 2 label should be within button X bounds");
                    assertEquals(48, labelBounds.width(), "Button 2 label width");
                }
                case 3 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 3 label should be within button X bounds");
                    assertEquals(48, labelBounds.width(), "Button 3 label width");
                }
                case 4 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 4 label should be within button X bounds");
                    assertEquals(48, labelBounds.width(), "Button 4 label width");
                }
                case 5 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 5 label should be within button X bounds");
                    assertEquals(48, labelBounds.width(), "Button 5 label width");
                }
                case 6 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 6 label should be within button X bounds");
                    assertEquals(48, labelBounds.width(), "Button 6 label width");
                }
                case 7 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 7 label should be within button X bounds");
                    assertEquals(48, labelBounds.width(), "Button 7 label width");
                }
                case 8 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 8 label should be within button X bounds");
                    assertEquals(48, labelBounds.width(), "Button 8 label width");
                }
                case 9 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 9 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 9 label width");
                }
                // Buttons 10-99 (double digits): width around 50-60
                case 10 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 10 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 10 label width");
                }
                case 11 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 11 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 11 label width");
                }
                case 12 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 12 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 12 label width");
                }
                case 13 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 13 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 13 label width");
                }
                case 14 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 14 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 14 label width");
                }
                case 15 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 15 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 15 label width");
                }
                case 16 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 16 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 16 label width");
                }
                case 17 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 17 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 17 label width");
                }
                case 18 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 18 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 18 label width");
                }
                case 19 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 19 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 19 label width");
                }
                case 20 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 20 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 20 label width");
                }
                case 21 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 21 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 21 label width");
                }
                case 22 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 22 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 22 label width");
                }
                case 23 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 23 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 23 label width");
                }
                case 24 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 24 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 24 label width");
                }
                case 25 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 25 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 25 label width");
                }
                case 26 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 26 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 26 label width");
                }
                case 27 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 27 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 27 label width");
                }
                case 28 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 28 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 28 label width");
                }
                case 29 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 29 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 29 label width");
                }
                case 30 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 30 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 30 label width");
                }
                case 31 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 31 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 31 label width");
                }
                case 32 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 32 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 32 label width");
                }
                case 33 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 33 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 33 label width");
                }
                case 34 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 34 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 34 label width");
                }
                case 35 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 35 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 35 label width");
                }
                case 36 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 36 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 36 label width");
                }
                case 37 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 37 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 37 label width");
                }
                case 38 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 38 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 38 label width");
                }
                case 39 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 39 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 39 label width");
                }
                case 40 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 40 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 40 label width");
                }
                case 41 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 41 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 41 label width");
                }
                case 42 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 42 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 42 label width");
                }
                case 43 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 43 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 43 label width");
                }
                case 44 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 44 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 44 label width");
                }
                case 45 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 45 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 45 label width");
                }
                case 46 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 46 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 46 label width");
                }
                case 47 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 47 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 47 label width");
                }
                case 48 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 48 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 48 label width");
                }
                case 49 -> {
                    assertTrue(labelBounds.x() >= 437 && labelBounds.x() + labelBounds.width() <= 549,
                        "Button 49 label should be within button X bounds");
                    assertEquals(54, labelBounds.width(), "Button 49 label width");
                }
                default -> fail("Unexpected button index: " + i);
            }
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
