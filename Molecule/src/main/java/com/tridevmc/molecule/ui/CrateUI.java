package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.element.*;
import com.tridevmc.compound.ui.layout.Alignment;

import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.ComposedUIContainer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Comprehensive demo UI showcasing the full Compound composable UI framework.
 * Demonstrates containers, scrolling, various inputs, styling, and layout systems.
 */
public class CrateUI extends ComposedUIContainer<CrateMenu> {

    public CrateUI(CrateMenu menu, Inventory inventory, Component name) {
        super(menu, inventory, name);
    }

    @Override
    protected void compose(ICompositionScope scope) {
        scope.e(new Stack(), stack -> {
            stack.layout()
                    .fillMax()
                    .contentAlignment(Alignment.CENTER);

            // Main horizontal layout: Inventory panel | Divider | Controls panel
            stack.e(new Row(), mainRow -> {
                mainRow.layout().spacing(8).verticalAlignment(Alignment.CENTER);

                // ===== LEFT PANEL: Classic Minecraft Inventory =====
                mainRow.e(new Panel(), inventoryPanel -> {
                    inventoryPanel.layout().fixedSize(176, 168);

                    inventoryPanel.fillSlot(Panel.CONTENT_SLOT, content -> {
                        content.e(new Box(), padded -> {
                            padded.layout().padding(7, 6, 7, 7);

                            padded.e(new Column(), column -> {
                                column.layout().spacing(0);

                                // Title with styling
                                column.e(new Label(
                                        Component.literal("Storage Crate"),
                                        0x404040,
                                        false
                                ));

                                column.e(new Spacer(0, 2));

                                // Crate slots grid (9x3)
                                column.e(new Grid(9, 0, 0), crateGrid -> {
                                    for (int i = 0; i < 27; i++) {
                                        crateGrid.e(new InventorySlot(this.getMenu(), i));
                                    }
                                });

                                // Spacer
                                column.e(new Spacer(0, 3));

                                // Inventory label
                                column.e(new Label(
                                        Component.literal("Inventory"),
                                        0x404040,
                                        false
                                ));

                                column.e(new Spacer(0, 2));

                                // Player inventory (9x3)
                                column.e(new Grid(9, 0, 0), playerGrid -> {
                                    for (int i = 0; i < 27; i++) {
                                        playerGrid.e(new InventorySlot(this.getMenu(), 27 + i));
                                    }
                                });

                                // Spacer
                                column.e(new Spacer(0, 4));

                                // Hotbar (9x1)
                                column.e(new Grid(9, 0, 0), hotbarGrid -> {
                                    for (int i = 0; i < 9; i++) {
                                        hotbarGrid.e(new InventorySlot(this.getMenu(), 54 + i));
                                    }
                                });
                            });
                        });
                    });
                });

                // ===== CENTER DIVIDER =====
                mainRow.e(new Divider(0xFF606060, 2),
                        div -> div.layout().fixedWidth(2).fixedHeight(220));

                // ===== RIGHT PANEL: Controls Demo =====
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
                                        controlsColumn.layout().spacing(4).fillMaxWidth();

                                        // Section: Basic Inputs
                                        controlsColumn.e(new Label(
                                                Component.literal("Basic Inputs"),
                                                0x404040,
                                                false
                                        ));

                                        controlsColumn.e(new Divider(0xFF808080, 1),
                                                div -> div.layout().fillMaxWidth().fixedHeight(1));

                                        // Text input with hint
                                        controlsColumn.e(new TextInput(), input -> {
                                            input.layout().fillMaxWidth().fixedHeight(20);
                                            input.getElement().setHint(Component.literal("Enter item name..."));
                                        });

                                        // Number input
                                        controlsColumn.e(new NumberInput(50), numberInput -> {
                                            numberInput.layout().fillMaxWidth().fixedHeight(20);
                                            numberInput.getElement().setRange(0, 100);
                                            numberInput.getElement().setOnValueChanged(val -> {
                                                System.out.println("Number changed: " + val);
                                            });
                                        });

                                        // Spacer
                                        controlsColumn.e(new Spacer(0, 8));

                                        // Section: Toggles
                                        controlsColumn.e(new Label(
                                                Component.literal("Toggles"),
                                                0x404040,
                                                false
                                        ));

                                        controlsColumn.e(new Divider(0xFF808080, 1),
                                                div -> div.layout().fillMaxWidth().fixedHeight(1));

                                        // Checkbox
                                        controlsColumn.e(new Checkbox(
                                                Component.literal("Auto-sort items"),
                                                true
                                        ), checkbox -> {
                                            checkbox.layout().fillMaxWidth().fixedHeight(20);
                                            checkbox.getElement().setLabelColor(0x404040);
                                            checkbox.getElement().setOnCheckedChanged(checked -> {
                                                System.out.println("Auto-sort: " + checked);
                                            });
                                        });

                                        // Toggle switch
                                        controlsColumn.e(new ToggleSwitch(true), toggle -> {
                                            toggle.layout().fixedSize(32, 16);
                                            toggle.getElement().setOnChanged(on -> {
                                                System.out.println("Toggle: " + on);
                                            });
                                        });

                                        // Spacer
                                        controlsColumn.e(new Spacer(0, 8));

                                        // Section: Selection
                                        controlsColumn.e(new Label(
                                                Component.literal("Selection"),
                                                0x404040,
                                                false
                                        ));

                                        controlsColumn.e(new Divider(0xFF808080, 1),
                                                div -> div.layout().fillMaxWidth().fixedHeight(1));

                                        // Dropdown
                                        controlsColumn.e(new Dropdown<>(
                                                List.of("Stack", "Single", "Half Stack")
                                        ), dropdown -> {
                                            dropdown.layout().fillMaxWidth().fixedHeight(20);
                                            dropdown.getElement().setSelected("Stack");
                                            dropdown.getElement().setOnSelectionChanged(selected -> {
                                                System.out.println("Transfer mode: " + selected);
                                            });
                                        });

                                        // Radio button group
                                        controlsColumn.e(new RadioButtonGroup(), radioGroup -> {
                                            radioGroup.layout().fillMaxWidth();
                                            radioGroup.getElement().setLabelColor(0x404040);
                                            radioGroup.getElement().addOption("Public");
                                            radioGroup.getElement().addOption("Private");
                                            radioGroup.getElement().addOption("Friends Only");
                                            radioGroup.getElement().setSelectedIndex(0);
                                        });

                                        // Spacer
                                        controlsColumn.e(new Spacer(0, 8));

                                        // Section: Sliders & Progress
                                        controlsColumn.e(new Label(
                                                Component.literal("Sliders & Progress"),
                                                0x404040,
                                                false
                                        ));

                                        controlsColumn.e(new Divider(0xFF808080, 1),
                                                div -> div.layout().fillMaxWidth().fixedHeight(1));

                                        // Slider
                                        controlsColumn.e(new Slider(0, 100, 1), slider -> {
                                            slider.layout().fillMaxWidth().fixedHeight(20);
                                            slider.getElement().setValue(75);
                                            slider.getElement().setShowValue(true);
                                            slider.getElement().setFormatter(v -> String.format("%.0f%%", v));
                                            slider.getElement().setOnValueChanged(val -> {
                                                System.out.println("Slider: " + val);
                                            });
                                        });

                                        // Progress bar
                                        controlsColumn.e(new ProgressBar(), progress -> {
                                            progress.layout().fillMaxWidth().fixedHeight(12);
                                            progress.getElement().setProgress(65, 100);
                                            progress.getElement().setShowPercentage(true);
                                        });

                                        // Spacer
                                        controlsColumn.e(new Spacer(0, 8));

                                        // Section: Actions
                                        controlsColumn.e(new Label(
                                                Component.literal("Actions"),
                                                0x404040,
                                                false
                                        ));

                                        controlsColumn.e(new Divider(0xFF808080, 1),
                                                div -> div.layout().fillMaxWidth().fixedHeight(1));

                                        // Button row
                                        controlsColumn.e(new Row(), buttonRow -> {
                                            buttonRow.layout().spacing(4).fillMaxWidth();

                                            buttonRow.e(new Button(), sortBtn -> {
                                                sortBtn.layout().fixedHeight(20).weight(1);
                                                sortBtn.fillSlot(Button.CONTENT_SLOT, btnContent -> {
                                                    btnContent.e(new Label(Component.literal("Sort")));
                                                });
                                                sortBtn.getElement().addPressListener((x, y) -> {
                                                    System.out.println("Sort button pressed");
                                                });
                                            });

                                            buttonRow.e(new Button(), dumpBtn -> {
                                                dumpBtn.layout().fixedHeight(20).weight(1);
                                                dumpBtn.fillSlot(Button.CONTENT_SLOT, btnContent -> {
                                                    btnContent.e(new Label(Component.literal("Dump")));
                                                });
                                                dumpBtn.getElement().addPressListener((x, y) -> {
                                                    System.out.println("Dump button pressed");
                                                });
                                            });

                                            buttonRow.e(new Button(), clearBtn -> {
                                                clearBtn.layout().fixedHeight(20).weight(1);
                                                clearBtn.fillSlot(Button.CONTENT_SLOT, btnContent -> {
                                                    btnContent.e(new Label(Component.literal("Clear")));
                                                });
                                                clearBtn.getElement().addPressListener((x, y) -> {
                                                    System.out.println("Clear button pressed");
                                                });
                                            });
                                        });

                                        // Spacer
                                        controlsColumn.e(new Spacer(0, 8));

                                        // Section: Text Area
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

                                        // Spacer
                                        controlsColumn.e(new Spacer(0, 8));

                                        // Section: Tabs Demo
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
                                                ), checkbox -> checkbox.getElement().setLabelColor(0x404040));
                                                tabScope.e(new Checkbox(
                                                        Component.literal("Show name"),
                                                        true
                                                ), checkbox -> checkbox.getElement().setLabelColor(0x404040));
                                            });

                                            tabs.getElement().addTab("History", tabScope -> {
                                                tabScope.e(new Label(
                                                        Component.literal("Last opened: 2m ago"),
                                                        0x808080,
                                                        false
                                                ));
                                            });
                                        });

                                        // Spacer
                                        controlsColumn.e(new Spacer(0, 8));

                                        // Section: List View
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
                                            listView.getElement().setOnSelectionChanged(item -> {
                                                System.out.println("Selected: " + item);
                                            });
                                        });
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    }
}
