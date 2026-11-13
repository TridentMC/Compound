package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.*;
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.Position;
import com.tridevmc.compound.ui.tree.ITreeNode;
import com.tridevmc.compound.ui.tree.UITree;
import com.tridevmc.compound.ui.visual.BufferedImageScreenContext;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Visual harness that composes the CrateUI, renders it to a {@link BufferedImageScreenContext},
 * and saves the result as a PNG for visual debugging.
 *
 * <p>Run with: {@code gradle test --tests "CrateUIVisualHarness"}</p>
 *
 * <p>Screenshots are saved to {@code build/screenshots/} relative to the project root.</p>
 */
@ExtendWith({MinecraftMockExtension.class, MockitoExtension.class})
public class CrateUIVisualHarness {

    @Mock
    private AbstractContainerMenu menu;

    private static final String OUTPUT_DIR = "build/screenshots";

    @BeforeEach
    void configureTextWrapping() {
        lenient().when(Minecraft.getInstance().font.getSplitter())
                .thenReturn(new StringSplitter((codePoint, style) -> 6F));
    }

    /**
     * Composes the current CrateUI layout and renders it at 800x600 (standard Minecraft GUI scale).
     * Saves the result as {@code crate_ui_800x600.png}.
     */
    @Test
    void renderCrateUI() {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < 63; i++) {
            Slot slot = mock(Slot.class);
            when(this.menu.getSlot(i)).thenReturn(slot);
            slots.add(slot);
        }

        UITree tree = composeCrateUI(slots);
        renderAndSave(tree, 800, 600, "crate_ui_800x600");
    }

    /**
     * Renders the CrateUI at different screen sizes to check responsive layout.
     * Saves results as {@code crate_ui_1024x768.png} and {@code crate_ui_1920x1080.png}.
     */
    @Test
    void renderCrateUIVariousSizes() {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < 63; i++) {
            Slot slot = mock(Slot.class);
            when(this.menu.getSlot(i)).thenReturn(slot);
            slots.add(slot);
        }

        UITree tree = composeCrateUI(slots);
        renderAndSave(tree, 1024, 768, "crate_ui_1024x768");

        tree = composeCrateUI(slots);
        renderAndSave(tree, 1920, 1080, "crate_ui_1920x1080");
    }

    /**
     * Renders the CrateUI with debug grid overlay showing 18px slot alignment.
     */
    @Test
    void renderCrateUIDebugGrid() {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < 63; i++) {
            Slot slot = mock(Slot.class);
            when(this.menu.getSlot(i)).thenReturn(slot);
            slots.add(slot);
        }

        UITree tree = composeCrateUI(slots);
        renderAndSave(tree, 800, 600, "crate_ui_debug_grid", true);
    }

    /**
     * Dumps the full element tree with bounds for debugging layout issues.
     */
    @Test
    void dumpCrateUITree() {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < 63; i++) {
            Slot slot = mock(Slot.class);
            when(this.menu.getSlot(i)).thenReturn(slot);
            slots.add(slot);
        }

        UITree tree = composeCrateUI(slots);
        var constraints = new Constraints(0, 800, 0, 600);
        tree.measureTree(constraints);
        tree.placeTree(new Position(0, 0), constraints);

        StringBuilder sb = new StringBuilder();
        sb.append("=== CrateUI Element Tree (800x600) ===\n\n");
        dumpTree(tree.getRoot(), 0, sb);

        System.out.println(sb);
    }

    /**
     * Composes the current CrateUI layout.
     * Mirrors the composition in {@code CrateUI.compose()} using mocked slots.
     */
    private UITree composeCrateUI(List<Slot> slots) {
        UITree tree = new UITree();
        var scope = ICompositionScope.root(tree);

        scope.e(new Stack(), stack -> {
            stack.layout()
                    .fillMax()
                    .contentAlignment(Alignment.CENTER);

            stack.e(new Row(), mainRow -> {
                mainRow.layout().spacing(8);

                mainRow.e(new Panel(), inventoryPanel -> {
                    inventoryPanel.layout().fixedSize(178, 220);

                    inventoryPanel.fillSlot(Panel.CONTENT_SLOT, content -> {
                        content.e(new Box(), padded -> {
                            padded.layout().padding(8);

                            padded.e(new Column(), column -> {
                                column.layout().spacing(4);

                                column.e(new Label(
                                        Component.literal("Storage Crate"),
                                        0x404040,
                                        false
                                ));

                                column.e(new Divider(0xFF808080, 1),
                                        div -> div.layout().fillMaxWidth().fixedHeight(1));

                                column.e(new Grid(9, 0, 0), crateGrid -> {
                                    for (int i = 0; i < 27; i++) {
                                        crateGrid.e(new InventorySlot(this.menu, i));
                                    }
                                });

                                column.e(new Spacer(0, 6));

                                column.e(new Label(
                                        Component.literal("Inventory"),
                                        0x404040,
                                        false
                                ));

                                column.e(new Grid(9, 0, 0), playerGrid -> {
                                    for (int i = 0; i < 27; i++) {
                                        playerGrid.e(new InventorySlot(this.menu, 27 + i));
                                    }
                                });

                                column.e(new Spacer(0, 4));

                                column.e(new Grid(9, 0, 0), hotbarGrid -> {
                                    for (int i = 0; i < 9; i++) {
                                        hotbarGrid.e(new InventorySlot(this.menu, 54 + i));
                                    }
                                });
                            });
                        });
                    });
                });

                mainRow.e(new Divider(0xFF606060, 2),
                        div -> div.layout().fixedWidth(2).fixedHeight(220));

                mainRow.e(new Panel(), controlsPanel -> {
                    controlsPanel.layout().fixedSize(200, 220);

                    controlsPanel.fillSlot(Panel.CONTENT_SLOT, content -> {
                        content.e(new Box(), padded -> {
                            padded.layout().padding(8);

                            padded.e(new ScrollArea(), scrollArea -> {
                                scrollArea.layout().fillMax();
                                scrollArea.getElement().scrollSpeed(8);

                                scrollArea.fillSlot(ScrollArea.CONTENT_SLOT, scrollContent -> {
                                    scrollContent.e(new Column(), controlsColumn -> {
                                        controlsColumn.layout().spacing(6).fillMaxWidth();

                                        controlsColumn.e(new Label(
                                                Component.literal("Basic Inputs"),
                                                0x404040,
                                                false
                                        ));

                                        controlsColumn.e(new Divider(0xFF808080, 1),
                                                div -> div.layout().fillMaxWidth().fixedHeight(1));

                                        controlsColumn.e(new TextInput(), input -> {
                                            input.layout().fillMaxWidth().fixedHeight(20);
                                            input.getElement().setHint(Component.literal("Enter item name..."));
                                        });

                                        controlsColumn.e(new NumberInput(50), numberInput -> {
                                            numberInput.layout().fillMaxWidth().fixedHeight(20);
                                            numberInput.getElement().setRange(0, 100);
                                            numberInput.getElement().setOnValueChanged(val -> {});
                                        });

                                        controlsColumn.e(new Spacer(0, 8));

                                        controlsColumn.e(new Label(
                                                Component.literal("Toggles"),
                                                0x404040,
                                                false
                                        ));

                                        controlsColumn.e(new Divider(0xFF808080, 1),
                                                div -> div.layout().fillMaxWidth().fixedHeight(1));

                                        controlsColumn.e(new Checkbox(
                                                Component.literal("Auto-sort items"),
                                                true
                                        ), checkbox -> {
                                            checkbox.layout().fillMaxWidth().fixedHeight(16);
                                        });

                                        controlsColumn.e(new ToggleSwitch(true), toggle -> {
                                            toggle.layout().fixedSize(32, 16);
                                        });

                                        controlsColumn.e(new Spacer(0, 8));

                                        controlsColumn.e(new Label(
                                                Component.literal("Selection"),
                                                0x404040,
                                                false
                                        ));

                                        controlsColumn.e(new Divider(0xFF808080, 1),
                                                div -> div.layout().fillMaxWidth().fixedHeight(1));

                                        controlsColumn.e(new Dropdown<>(
                                                List.of("Stack", "Single", "Half Stack")
                                        ), dropdown -> {
                                            dropdown.layout().fillMaxWidth().fixedHeight(20);
                                            dropdown.getElement().setSelected("Stack");
                                        });

                                        controlsColumn.e(new RadioButtonGroup(), radioGroup -> {
                                            radioGroup.layout().fillMaxWidth();
                                            radioGroup.getElement().addOption("Public");
                                            radioGroup.getElement().addOption("Private");
                                            radioGroup.getElement().addOption("Friends Only");
                                            radioGroup.getElement().setSelectedIndex(0);
                                        });

                                        controlsColumn.e(new Spacer(0, 8));

                                        controlsColumn.e(new Label(
                                                Component.literal("Sliders & Progress"),
                                                0x404040,
                                                false
                                        ));

                                        controlsColumn.e(new Divider(0xFF808080, 1),
                                                div -> div.layout().fillMaxWidth().fixedHeight(1));

                                        controlsColumn.e(new Slider(0, 100, 1), slider -> {
                                            slider.layout().fillMaxWidth().fixedHeight(20);
                                            slider.getElement().setValue(75);
                                            slider.getElement().setShowValue(true);
                                            slider.getElement().setFormatter(v -> String.format("%.0f%%", v));
                                        });

                                        controlsColumn.e(new ProgressBar(), progress -> {
                                            progress.layout().fillMaxWidth().fixedHeight(12);
                                            progress.getElement().setProgress(65, 100);
                                            progress.getElement().setShowPercentage(true);
                                            progress.getElement().setFillColor(0xFF4CAF50);
                                        });

                                        controlsColumn.e(new Spacer(0, 8));

                                        controlsColumn.e(new Label(
                                                Component.literal("Actions"),
                                                0x404040,
                                                false
                                        ));

                                        controlsColumn.e(new Divider(0xFF808080, 1),
                                                div -> div.layout().fillMaxWidth().fixedHeight(1));

                                        controlsColumn.e(new Row(), buttonRow -> {
                                            buttonRow.layout().spacing(4).fillMaxWidth();

                                            buttonRow.e(new Button(), sortBtn -> {
                                                sortBtn.layout().fixedHeight(20).weight(1);
                                                sortBtn.fillSlot(Button.CONTENT_SLOT, btnContent -> {
                                                    btnContent.e(new Label(Component.literal("Sort")));
                                                });
                                            });

                                            buttonRow.e(new Button(), dumpBtn -> {
                                                dumpBtn.layout().fixedHeight(20).weight(1);
                                                dumpBtn.fillSlot(Button.CONTENT_SLOT, btnContent -> {
                                                    btnContent.e(new Label(Component.literal("Dump")));
                                                });
                                            });

                                            buttonRow.e(new Button(), clearBtn -> {
                                                clearBtn.layout().fixedHeight(20).weight(1);
                                                clearBtn.fillSlot(Button.CONTENT_SLOT, btnContent -> {
                                                    btnContent.e(new Label(Component.literal("Clear")));
                                                });
                                            });
                                        });

                                        controlsColumn.e(new Spacer(0, 8));

                                        controlsColumn.e(new Label(
                                                Component.literal("Notes"),
                                                0x404040,
                                                false
                                        ));

                                        controlsColumn.e(new Divider(0xFF808080, 1),
                                                div -> div.layout().fillMaxWidth().fixedHeight(1));

                                        controlsColumn.e(new TextArea(), textArea -> {
                                            textArea.layout().fillMaxWidth().fixedHeight(60);
                                            textArea.getElement().setHint(Component.literal("Enter notes about this crate..."));
                                            textArea.getElement().setMaxLength(200);
                                        });

                                        controlsColumn.e(new Spacer(0, 8));

                                        controlsColumn.e(new Label(
                                                Component.literal("Tabs"),
                                                0x404040,
                                                false
                                        ));

                                        controlsColumn.e(new Divider(0xFF808080, 1),
                                                div -> div.layout().fillMaxWidth().fixedHeight(1));

                                        controlsColumn.e(new Tabs(), tabs -> {
                                            tabs.layout().fillMaxWidth().fixedHeight(80);

                                            tabs.getElement().addTab("Info", tabScope -> {
                                                tabScope.e(new Label(
                                                        Component.literal("Crate ID: #1234"),
                                                        0x808080,
                                                        false
                                                ));
                                                tabScope.e(new Label(
                                                        Component.literal("Owner: PlayerName"),
                                                        0x808080,
                                                        false
                                                ));
                                            });

                                            tabs.getElement().addTab("Settings", tabScope -> {
                                                tabScope.e(new Checkbox(
                                                        Component.literal("Lock crate"),
                                                        false
                                                ));
                                                tabScope.e(new Checkbox(
                                                        Component.literal("Show name"),
                                                        true
                                                ));
                                            });

                                            tabs.getElement().addTab("History", tabScope -> {
                                                tabScope.e(new Label(
                                                        Component.literal("Last opened: 2m ago"),
                                                        0x808080,
                                                        false
                                                ));
                                            });
                                        });

                                        controlsColumn.e(new Spacer(0, 8));

                                        controlsColumn.e(new Label(
                                                Component.literal("Recent Items"),
                                                0x404040,
                                                false
                                        ));

                                        controlsColumn.e(new Divider(0xFF808080, 1),
                                                div -> div.layout().fillMaxWidth().fixedHeight(1));

                                        controlsColumn.e(new ListView<String>(), listView -> {
                                            listView.layout().fillMaxWidth().fixedHeight(60);
                                            listView.getElement().setItems(List.of(
                                                    "Diamond x12",
                                                    "Iron Ingot x64",
                                                    "Oak Log x32",
                                                    "Redstone x48"
                                            ));
                                        });
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });

        return tree;
    }

    /**
     * Measures, places, and renders the tree to a BufferedImage and saves it as a PNG.
     */
    private void renderAndSave(UITree tree, int width, int height, String filename) {
        renderAndSave(tree, width, height, filename, false);
    }

    private void renderAndSave(UITree tree, int width, int height, String filename, boolean drawDebugGrid) {
        var constraints = new Constraints(0, width, 0, height);
        tree.measureTree(constraints);
        tree.placeTree(new Position(0, 0), constraints);

        try (var ctx = new BufferedImageScreenContext(width, height)) {
            ctx.drawGradientRect(0, 0, width, height, 0xFFC6C6C6, 0xFFC6C6C6);
            tree.layoutAndRender(width, height, ctx);
            if (drawDebugGrid) ctx.drawDebugGrid();
            ctx.saveTo(OUTPUT_DIR + "/" + filename + ".png");
        } finally {
            tree.reset();
        }
    }

    /**
     * Recursively dumps the tree structure with indentation and bounds info.
     */
    private void dumpTree(ITreeNode node, int depth, StringBuilder sb) {
        String indent = "  ".repeat(depth);
        Bounds bounds = node.getElement().getBounds();
        String elementName = node.getElement().getClass().getSimpleName();
        String boundsStr = bounds != null
                ? String.format("(%d, %d, %d, %d)", bounds.x(), bounds.y(), bounds.width(), bounds.height())
                : "(no bounds)";
        sb.append(indent).append(elementName).append(" ").append(boundsStr).append("\n");
        for (ITreeNode child : node.getChildren()) {
            dumpTree(child, depth + 1, sb);
        }
    }
}
