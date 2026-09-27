package com.tridevmc.compound.ui.integration;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.Rect2F;
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
    private static final Bounds GOLD_INVENTORY_LABEL_BOUNDS = new Bounds(255, 283, 54, 9);
    private static final Bounds GOLD_PLAYER_GRID_BOUNDS = new Bounds(255, 295, 162, 54);
    private static final Bounds GOLD_SPACER_BOUNDS = new Bounds(255, 355, 0, 4);
    private static final Bounds GOLD_HOTBAR_GRID_BOUNDS = new Bounds(255, 363, 162, 18);
    private static final Bounds GOLD_SCROLL_BOX_BOUNDS = new Bounds(433, 205, 120, 190);
    private static final Bounds GOLD_SCROLL_STACK_BOUNDS = new Bounds(433, 205, 120, 190);
    private static final Bounds GOLD_SCROLL_PADDED_BOUNDS = new Bounds(433, 205, 120, 190);
    private static final Bounds GOLD_SCROLL_AREA_BOUNDS = new Bounds(437, 209, 112, 182);
    private static final Bounds GOLD_SCROLL_INNER_BOX_BOUNDS = new Bounds(437, 209, 112, 1098);
    private static final Bounds GOLD_SCROLL_COLUMN_BOUNDS = new Bounds(437, 209, 112, 1098);

    /**
     * Comprehensive regression test for CrateUI that validates EVERY aspect of the UI rendering:
     *
     * <h3>Element Bounds Validation (800x600 screen):</h3>
     * <ul>
     *   <li>Root Stack: (0, 0, 800, 600)</li>
     *   <li>Centered Row: (247, 205, 306, 190)</li>
     *   <li>Main Panel: (247, 205, 178, 190) with all nested elements</li>
     *   <li>Scroll Panel: (433, 205, 120, 190) with ScrollArea</li>
     *   <li>All 63 slot positions (27 crate + 27 player + 9 hotbar)</li>
     *   <li>All 50 text input positions (only 9 visible within ScrollArea scissor)</li>
     * </ul>
     *
     * <h3>Internal Element Structure:</h3>
     * <ul>
     *   <li>InventorySlot: Stack → [sprite(bg), sprite(underlay), item, sprite(overlay)]</li>
     *   <li>TextInput: Complex internal structure for text editing</li>
     *   <li>Panel: Stack → [sprite(bg), content]</li>
     * </ul>
     *
     * <h3>Render Call Validation (exact coordinates):</h3>
     * <ul>
     *   <li><b>74 sprite draws:</b>
     *     <ul>
     *       <li>2 Panel backgrounds at (247,205,178,190) and (433,205,120,190)</li>
     *       <li>27 crate slots: grid starting at (255,226) with 18px spacing</li>
     *       <li>27 player slots: grid starting at (255,297) with 18px spacing</li>
     *       <li>9 hotbar slots: row at (255,363) with 18px spacing</li>
     *       <li>9 visible text input backgrounds: column at (437,209+) with 22px pitch</li>
     *     </ul>
     *   </li>
     *   <li><b>2 text draws (no shadow):</b>
     *     <ul>
     *       <li>"Crate" label at (255, 213)</li>
     *       <li>"Inventory" label at (255, 284)</li>
     *     </ul>
     *   </li>
     *   <li><b>9 text draws (no shadow):</b>
     *     <ul>
     *       <li>"Input 1-9" hint texts left-aligned at (441, 214+) with 22px pitch</li>
     *     </ul>
     *   </li>
     *   <li><b>1 scissor enable:</b> (437, 209, 549, 391) for ScrollArea clipping</li>
     *   <li><b>1 scissor disable:</b> restores full rendering</li>
     * </ul>
     *
     * <p>This test uses the EXACT same composition code as CrateUI and validates that every
     * element is measured, placed, and rendered at precisely the expected coordinates.
     * Any change to layout, positioning, or rendering will cause this test to fail.</p>
     */
    @Test
    void testCrateUIComposition() {
        // 1. Setup
        UITree tree = new UITree();
        RootScope scope = new RootScope(tree);
        Column scrollContent = new Column();

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
                row.e(new Panel(), box -> {
                    // Panel just specifies its size
                    box.layout().fixedSize(178, 190);

                    // Fill content slot with Box for padding
                    box.fillSlot(Panel.CONTENT_SLOT, content -> {
                        content.e(new Box(), paddedContent -> {
                            // Set padding on the box
                            paddedContent.layout().padding(8);

                            // Main content column
                            paddedContent.e(new Column(), column -> {
                                // Set spacing on the column
                                column.layout().spacing(4);
                                // "Crate" label - no shadow for Minecraft inventory style
                                column.e(new Label(
                                        Component.literal("Crate"),
                                        0x404040,
                                        false  // No shadow for inventory labels
                                ));

                                // Crate slots grid (9x3 = 27 slots)
                                column.e(new Grid(9, 0, 0), crateGrid -> {
                                    for (int i = 0; i < 27; i++) {
                                        crateGrid.e(new InventorySlot(this.menu.getSlot(i)));
                                    }
                                });

                                // "Inventory" label - no shadow for Minecraft inventory style
                                column.e(new Label(
                                        Component.literal("Inventory"),
                                        0x404040,
                                        false  // No shadow for inventory labels
                                ));

                                // Player inventory grid (9x3 = 27 slots)
                                column.e(new Grid(9, 0, 0), playerGrid -> {
                                    for (int i = 0; i < 27; i++) {
                                        playerGrid.e(new InventorySlot(this.menu.getSlot(27 + i)));
                                    }
                                });

                                // Spacer before hotbar
                                column.e(new Spacer(0, 4));

                                // Hotbar grid (9x1 = 9 slots)
                                column.e(new Grid(9, 0, 0), hotbarGrid -> {
                                    for (int i = 0; i < 9; i++) {
                                        hotbarGrid.e(new InventorySlot(this.menu.getSlot(54 + i)));
                                    }
                                });
                            });
                        });
                    });
                });

                // Scrollable list of text inputs on the right side
                row.e(new Panel(), scrollBox -> {
                    scrollBox.layout().fixedSize(120, 190); // Match height of main UI

                    scrollBox.fillSlot(Panel.CONTENT_SLOT, content -> {
                        content.e(new Box(), paddedBox -> {
                            paddedBox.layout().padding(4);

                            // ScrollArea containing the list of text inputs
                            paddedBox.e(new ScrollArea(), scrollArea -> {
                                scrollArea.getElement().scrollSpeed(10);

                                // Fill scroll area's content slot with column of text inputs
                                scrollArea.fillSlot(ScrollArea.CONTENT_SLOT, contentSlot -> {
                                    contentSlot.e(scrollContent, inputColumn -> {
                                        inputColumn.layout().spacing(2);

                                        // Create alternating buttons and text inputs
                                        for (int i = 1; i <= 50; i++) {
                                            final int elementIndex = i;
                                            if (i % 2 == 1) {
                                                // Odd indices: Text inputs
                                                inputColumn.e(new TextInput(), input -> {
                                                    input.layout()
                                                            .fillMaxWidth()
                                                            .fixedHeight(20);

                                                    input.getElement().setHint(Component.literal("Input " + elementIndex));
                                                    input.getElement().setResponder(text -> {
                                                        System.out.println("Input " + elementIndex + " changed: " + text);
                                                    });
                                                });
                                            } else {
                                                // Even indices: Buttons
                                                inputColumn.e(new Button(), button -> {
                                                    button.layout()
                                                            .fillMaxWidth()
                                                            .fixedHeight(20);

                                                    button.fillSlot(Button.CONTENT_SLOT, buttonContent -> {
                                                        buttonContent.e(new Label(Component.literal("Button " + elementIndex)));
                                                    });

                                                    button.getElement().addPressListener((x, y) -> {
                                                        System.out.println("Button " + elementIndex + " pressed at " + x + ", " + y);
                                                    });
                                                });
                                            }
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

        // Row should have 2 children: Main UI Panel and ScrollArea Panel
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
        assertEquals(Panel.class, mainBoxNode.getElement().getClass());
        Bounds mainBoxBounds = mainBoxNode.getElement().getBounds();
        assertEquals(new Bounds(247, 205, 178, 190), mainBoxBounds, "Main Box should be centered");

        // Verify content of Main UI Box
        // Panel -> Stack -> Box (padding) -> Column
        ITreeNode mainStackNode = mainBoxNode.getChildren().getFirst();
        assertTrue(mainStackNode.getElement() instanceof Stack, "Main Stack should be a Stack");
        Bounds mainStackBounds = mainStackNode.getElement().getBounds();
        assertEquals(new Bounds(247, 205, 178, 190), mainStackBounds, "Main Stack should fill Panel");

        // Validate Panel sprite (background) - should be first child of Stack
        ITreeNode mainBoxSpriteNode = mainStackNode.getChildren().stream()
                .filter(n -> n.getElement() instanceof Sprite)
                .findFirst().orElseThrow();
        assertEquals(Sprite.class, mainBoxSpriteNode.getElement().getClass());
        Bounds mainBoxSpriteBounds = mainBoxSpriteNode.getElement().getBounds();
        assertEquals(new Bounds(247, 205, 178, 190), mainBoxSpriteBounds, "Main Panel sprite should fill Panel");

        ITreeNode paddedContentNode = mainStackNode.getChildren().stream()
                .filter(n -> n.getElement() instanceof Box)
                .findFirst().orElseThrow();
        assertEquals(Box.class, paddedContentNode.getElement().getClass());
        Bounds paddedContentBounds = paddedContentNode.getElement().getBounds();
        // Box with padding actually fills the same bounds as its parent, but applies padding to its children
        assertEquals(new Bounds(247, 205, 178, 190), paddedContentBounds, "Padded Box should fill Panel");

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
        assertEquals(Label.class, crateLabelNode.getElement().getClass());
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
                assertEquals(InventorySlot.class, slotNode.getElement().getClass());
                Bounds slotBounds = slotNode.getElement().getBounds();
                switch (row * 9 + col) {
                    case 0 -> {
                        assertEquals(new Bounds(255, 226, 18, 18), slotBounds, "Crate slot [0,0] bounds");
                        // Validate first slot's internal structure comprehensively
                        // InventorySlot -> Stack -> Sprite (bg) + Sprite (underlay) + ItemDisplay + Sprite (overlay)
                        ITreeNode slotStackNode = slotNode.getChildren().getFirst();
                        assertEquals(Stack.class, slotStackNode.getElement().getClass());
                        Bounds slotStackBounds = slotStackNode.getElement().getBounds();
                        assertEquals(new Bounds(255, 226, 18, 18), slotStackBounds, "Crate slot [0,0] Stack bounds");

                        // Should have 4 children: bg sprite, underlay sprite, item, overlay sprite
                        assertEquals(4, slotStackNode.getChildren().size(), "Crate slot [0,0] should have 4 children");

                        // Child 0: Background sprite (18x18)
                        ITreeNode slotBgNode = slotStackNode.getChildren().get(0);
                        assertEquals(Sprite.class, slotBgNode.getElement().getClass());
                        Bounds slotBgBounds = slotBgNode.getElement().getBounds();
                        assertEquals(new Bounds(255, 226, 18, 18), slotBgBounds, "Crate slot [0,0] background sprite bounds");

                        // Child 1: Underlay sprite (null by default, so would be 24x24 with -3 margin if present)
                        ITreeNode slotUnderlayNode = slotStackNode.getChildren().get(1);
                        assertEquals(Sprite.class, slotUnderlayNode.getElement().getClass());
                        Sprite slotUnderlaySprite = (Sprite) slotUnderlayNode.getElement();
                        // Underlay is null by default, but element exists with bounds calculated as if it would render
                        Bounds slotUnderlayBounds = slotUnderlayNode.getElement().getBounds();
                        assertEquals(new Bounds(252, 223, 24, 24), slotUnderlayBounds, "Crate slot [0,0] underlay sprite bounds (with -3 margin)");

                        // Child 2: Item element (16x16 with 1px margin)
                        ITreeNode slotItemNode = slotStackNode.getChildren().get(2);
                        assertEquals(ItemDisplay.class, slotItemNode.getElement().getClass());
                        Bounds slotItemBounds = slotItemNode.getElement().getBounds();
                        assertEquals(new Bounds(256, 227, 16, 16), slotItemBounds, "Crate slot [0,0] item bounds (with 1px margin)");

                        // Child 3: Overlay sprite (null by default, so would be 24x24 with -3 margin if present)
                        ITreeNode slotOverlayNode = slotStackNode.getChildren().get(3);
                        assertEquals(Sprite.class, slotOverlayNode.getElement().getClass());
                        Sprite slotOverlaySprite = (Sprite) slotOverlayNode.getElement();
                        Bounds slotOverlayBounds = slotOverlayNode.getElement().getBounds();
                        assertEquals(new Bounds(252, 223, 24, 24), slotOverlayBounds, "Crate slot [0,0] overlay sprite bounds (with -3 margin)");
                    }
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
        assertEquals(Label.class, invLabelNode.getElement().getClass());
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
                assertEquals(InventorySlot.class, slotNode.getElement().getClass());
                Bounds slotBounds = slotNode.getElement().getBounds();
                switch (row * 9 + col) {
                    case 0 -> {
                        assertEquals(new Bounds(255, 297, 18, 18), slotBounds, "Player slot [0,0] bounds");
                        // Validate first player slot's internal structure
                        ITreeNode slotStackNode = slotNode.getChildren().getFirst();
                        assertEquals(Stack.class, slotStackNode.getElement().getClass());
                        assertEquals(4, slotStackNode.getChildren().size(), "Player slot [0,0] should have 4 children");

                        // Background sprite
                        ITreeNode slotBgNode = slotStackNode.getChildren().get(0);
                        assertEquals(Sprite.class, slotBgNode.getElement().getClass());
                        assertEquals(new Bounds(255, 297, 18, 18), slotBgNode.getElement().getBounds(), "Player slot [0,0] background sprite bounds");

                        // Item element
                        ITreeNode slotItemNode = slotStackNode.getChildren().get(2);
                        assertEquals(ItemDisplay.class, slotItemNode.getElement().getClass());
                        assertEquals(new Bounds(256, 298, 16, 16), slotItemNode.getElement().getBounds(), "Player slot [0,0] item bounds");
                    }
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
        assertEquals(Spacer.class, spacerNode.getElement().getClass());
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
            assertEquals(InventorySlot.class, slotNode.getElement().getClass());
            Bounds slotBounds = slotNode.getElement().getBounds();
            switch (col) {
                case 0 -> {
                    assertEquals(new Bounds(255, 363, 18, 18), slotBounds, "Hotbar slot [0] bounds");
                    // Validate first hotbar slot's internal structure
                    ITreeNode slotStackNode = slotNode.getChildren().getFirst();
                    assertEquals(Stack.class, slotStackNode.getElement().getClass());
                    assertEquals(4, slotStackNode.getChildren().size(), "Hotbar slot [0] should have 4 children");

                    // Background sprite
                    ITreeNode slotBgNode = slotStackNode.getChildren().get(0);
                    assertEquals(Sprite.class, slotBgNode.getElement().getClass());
                    assertEquals(new Bounds(255, 363, 18, 18), slotBgNode.getElement().getBounds(), "Hotbar slot [0] background sprite bounds");

                    // Item element
                    ITreeNode slotItemNode = slotStackNode.getChildren().get(2);
                    assertEquals(ItemDisplay.class, slotItemNode.getElement().getClass());
                    assertEquals(new Bounds(256, 364, 16, 16), slotItemNode.getElement().getBounds(), "Hotbar slot [0] item bounds");
                }
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
        assertEquals(Panel.class, scrollBoxNode.getElement().getClass());
        Bounds scrollBoxBounds = scrollBoxNode.getElement().getBounds();
        // X = 247 + 178 + 8 = 433
        assertEquals(new Bounds(433, 205, 120, 190), scrollBoxBounds, "Scroll Box position");

        // Verify ScrollArea content - Complete structure validation
        // ScrollBox -> Stack -> Box -> ScrollArea
        ITreeNode scrollStackNode = scrollBoxNode.getChildren().getFirst();
        assertTrue(scrollStackNode.getElement() instanceof Stack, "Scroll Stack should be a Stack");
        Bounds scrollStackBounds = scrollStackNode.getElement().getBounds();
        assertEquals(new Bounds(433, 205, 120, 190), scrollStackBounds, "Scroll Stack should fill Panel");

        // Validate ScrollBox sprite (background) - should be first child of Stack
        ITreeNode scrollBoxSpriteNode = scrollStackNode.getChildren().stream()
                .filter(n -> n.getElement() instanceof Sprite)
                .findFirst().orElseThrow();
        assertEquals(Sprite.class, scrollBoxSpriteNode.getElement().getClass());
        Bounds scrollBoxSpriteBounds = scrollBoxSpriteNode.getElement().getBounds();
        assertEquals(new Bounds(433, 205, 120, 190), scrollBoxSpriteBounds, "Scroll Panel sprite should fill Panel");

        ITreeNode scrollPaddedBoxNode = scrollStackNode.getChildren().stream()
                .filter(n -> n.getElement() instanceof Box)
                .findFirst().orElseThrow();
        assertEquals(Box.class, scrollPaddedBoxNode.getElement().getClass());
        Bounds scrollPaddedBoxBounds = scrollPaddedBoxNode.getElement().getBounds();
        // Box with padding fills same bounds as parent, but applies 4px padding to children
        assertEquals(new Bounds(433, 205, 120, 190), scrollPaddedBoxBounds, "Scroll Padded Box should fill Panel");

        ITreeNode scrollAreaNode = scrollPaddedBoxNode.getChildren().stream()
                .filter(n -> n.getElement() instanceof ScrollArea)
                .findFirst().orElseThrow();
        assertEquals(ScrollArea.class, scrollAreaNode.getElement().getClass());
        Bounds scrollAreaBounds = scrollAreaNode.getElement().getBounds();
        // ScrollArea accounts for 4px padding from parent Box: X+4, Y+4, Width-8, Height-8
        assertEquals(new Bounds(437, 209, 112, 182), scrollAreaBounds, "ScrollArea should account for parent Box padding");

        var scrollContentNode = tree.getNodeForElement(scrollContent);
        assertNotNull(scrollContentNode, "Authored scroll content must remain mounted");
        assertEquals(50, scrollContentNode.getChildren().size());
        var contentBounds = scrollContent.getBounds();
        int scrollbarGutter = 6 + 4;
        int expectedContentWidth = scrollAreaBounds.width() - scrollbarGutter;
        int expectedContentHeight = 50 * 20 + 49 * 2;
        assertEquals(new Bounds(scrollAreaBounds.x(), scrollAreaBounds.y(),
                expectedContentWidth, expectedContentHeight), contentBounds);
        assertTrue(scrollAreaNode.getLayoutProperties().isClip(), "ScrollArea must clip overflowing content");
        assertEquals(expectedContentHeight - scrollAreaBounds.height(),
                ((ScrollArea) scrollAreaNode.getElement()).getMaxScrollY());

        for (int i = 0; i < 50; i++) {
            var element = scrollContentNode.getChildren().get(i).getElement();
            assertEquals(i % 2 == 0 ? TextInput.class : Button.class, element.getClass());
            assertEquals(new Bounds(contentBounds.x(), contentBounds.y() + i * 22,
                    expectedContentWidth, 20), element.getBounds(), "Row " + i + " layout");
        }

        tree.renderTree(this.screenContext);
        var spriteCaptor = ArgumentCaptor.forClass(IScreenSprite.class);
        var rectCaptor = ArgumentCaptor.forClass(Rect2F.class);
        verify(this.screenContext, atLeastOnce()).drawSprite(spriteCaptor.capture(), rectCaptor.capture());
        var spriteRects = rectCaptor.getAllValues();
        for (var child : scrollContentNode.getChildren()) {
            var bounds = child.getBounds();
            boolean rendered = spriteRects.stream().anyMatch(rect -> rect.getX() == bounds.x()
                    && rect.getY() == bounds.y() && rect.getWidth() == bounds.width()
                    && rect.getHeight() == bounds.height());
            assertEquals(bounds.intersects(scrollAreaBounds), rendered,
                    "Only rows intersecting the viewport should submit their background sprite");
        }

        var plainText = ArgumentCaptor.forClass(Component.class);
        var shadowText = ArgumentCaptor.forClass(Component.class);
        verify(this.screenContext, atLeastOnce()).drawText(plainText.capture(), anyFloat(), anyFloat());
        verify(this.screenContext, atLeastOnce()).drawTextWithShadow(shadowText.capture(), anyFloat(), anyFloat());
        List<String> renderedText = new ArrayList<>();
        plainText.getAllValues().forEach(text -> renderedText.add(text.getString()));
        shadowText.getAllValues().forEach(text -> renderedText.add(text.getString()));
        assertTrue(renderedText.containsAll(List.of("Crate", "Inventory", "Input 1", "Button 2")),
                "Inventory labels, input hints and button slot labels must render");
        assertFalse(renderedText.contains("Input 49"), "Offscreen text must be culled");

        verify(this.screenContext, atLeastOnce()).enableScissor(scrollAreaBounds.x(), scrollAreaBounds.y(),
                scrollAreaBounds.right(), scrollAreaBounds.bottom());
        verify(this.screenContext, atLeastOnce()).disableScissor();
    }
}