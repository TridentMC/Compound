# Declarative UI Usage Examples

This document provides concrete examples of how to build UIs using the declarative framework.

## Example 1: Static Minecraft Chest UI

This example demonstrates how to use layout primitives (Stack, Column, Grid) and visual primitives (ElementBox, ElementLabel, ElementSlot) to create a complex, static UI that replicates the layout of a double chest.

This UI is "static" because its structure never changes. It's composed once and that's it.

```java
/**
 * A screen that replicates the layout of a Minecraft double chest inventory.
 * This demonstrates complex, static layout.
 */
public class MyChestScreen extends CompoundScreenWithDeclarativeUI {
    // (Constructor)
    public MyChestScreen(Component title) {
        super(title);
    }

    @Override
    protected void compose(ICompositionContext context) {
        // 1. Root: Use a Stack to center the entire UI
        context.e(new Stack(), stack -> {
            stack.active.setLayoutProperties(
                    LayoutProperties.create().withAlignment(Alignment.CENTER));

            // 2. Main Panel: The dark gray background for the chest
            stack.e(new ElementBox(), mainPanel -> {
                // This is the standard 9-slice box primitive
                mainPanel.active.setSprite(CommonSprites.INVENTORY_PANEL);
                mainPanel.active.setLayoutProperties(
                        LayoutProperties.create()
                                .withFixedSize(
                                        176, 222) // Standard double chest size
                                .withPadding(7)); // Standard 7px padding

                // 3. Main Layout: A Column to stack all content vertically
                mainPanel.e(new Column(), contentColumn -> {
                    contentColumn.active.setLayoutProperties(
                            LayoutProperties.create().withSpacing(
                                    4)); // 4px spacing between elements

                    // --- CHEST INVENTORY ---
                    contentColumn.e(new ElementLabel(), label -> {
                        label.active.setText("Chest"); // Or pass a Component
                        label.active.setColor(0x404040); // Dark gray text color
                    });

                    contentColumn.e(new Grid(), chestGrid -> {
                        chestGrid.active.setLayoutProperties(
                                LayoutProperties.create()
                                        .withGridSize(9, 6) // 9 columns, 6 rows
                                        .withSpacing(
                                                0)); // No spacing between slots

                        // Add 54 slots (9x6)
                        for (int i = 0; i < 54; i++) {
                            // Assume ElementSlot is a primitive that
                            // takes a slot index or a Slot object.
                            chestGrid.e(new ElementSlot(this.menu.getSlot(i)),
                                    slot
                                            -> {
                                        // Slot is 18x18, which is its
                                        // default size
                                    });
                        }
                    });

                    // --- PLAYER INVENTORY ---
                    contentColumn.e(new ElementLabel(), label -> {
                        label.active.setText("Player Inventory");
                        label.active.setColor(0x404040);
                    });

                    contentColumn.e(new Grid(), playerGrid -> {
                        playerGrid.active.setLayoutProperties(
                                LayoutProperties.create()
                                        .withGridSize(9, 3) // 9 columns, 3 rows
                                        .withSpacing(0));

                        // Add 27 slots (9x3)
                        for (int i = 0; i < 27; i++) {
                            // We map these to the correct slot indices
                            int slotIndex = 54 + i;
                            playerGrid.e(new ElementSlot(
                                            this.menu.getSlot(slotIndex)),
                                    slot -> {});
                        }
                    });

                    // --- PLAYER HOTBAR ---
                    // We add extra space before the hotbar
                    contentColumn.e(new ElementSpacer(), spacer -> {
                        spacer.active.setLayoutProperties(
                                LayoutProperties.create().withFixedSize(0, 4));
                    });

                    contentColumn.e(new Grid(), hotbarGrid -> {
                        hotbarGrid.active.setLayoutProperties(
                                LayoutProperties.create()
                                        .withGridSize(9, 1) // 9 columns, 1 row
                                        .withSpacing(0));

                        // Add 9 slots (9x1)
                        for (int i = 0; i < 9; i++) {
                            int slotIndex = 54 + 27 + i;
                            hotbarGrid.e(new ElementSlot(
                                            this.menu.getSlot(slotIndex)),
                                    slot -> {});
                        }
                    });
                });
            });
        });
    }
}
```

## Example 2: Dynamic List with bind()

This example demonstrates how to use Type 2 Reactivity (bind()) to create a dynamic list of items from a State object.

```java
/**
 * A screen that shows a dynamic, scrollable list of enchantments.
 */
public class MyEnchantingScreen extends CompoundScreenWithDeclarativeUI {
    // --- 1. APPLICATION STATE ---
    private State<List<String>> enchantmentNames;
    private final Random random = new Random();

    @Override
    public void init() {
        super.init();

        // Initialize our state object
        this.enchantmentNames = useState(generateRandomEnchantments());
    }

    @Override
    protected void compose(ICompositionContext context) {
        context.e(new Column(), root -> {
            root.active.setLayoutProperties(LayoutProperties.create()
                    .withFixedSize(200, 250)
                    .withPadding(10)
                    .withSpacing(10));

            // --- 2. REROLL BUTTON ---
            root.e(new ElementButton(), rerollButton -> {
                rerollButton.active.setText("Reroll");
                rerollButton.active.onClick(e -> {
                    // This updates the state, which triggers
                    // the 'listContainer.bind()' to re-run.
                    this.enchantmentNames.set(generateRandomEnchantments());
                });
            });

            // --- 3. DYNAMIC LIST ---
            root.e(new ScrollContainer(), scrollBox -> {
                scrollBox.active.setLayoutProperties(
                        LayoutProperties.create()
                                .withWeight(
                                        1) // Takes all available vertical space
                                .withPadding(4));
                scrollBox.active.setBackground(CommonSprites.INVENTORY_PANEL);

                // This Column will hold our dynamic children
                scrollBox.e(new Column(), listContainer -> {
                    listContainer.active.setLayoutProperties(
                            LayoutProperties.create().withSpacing(2));

                    // *** REACTIVE STRUCTURE ***
                    // Bind the 'enchantmentNames' state to this container.
                    // This entire lambda will re-run, rebuilding all
                    // children, whenever 'enchantmentNames' is updated.
                    listContainer.bind(this.enchantmentNames);

                    for (String enchantName : this.enchantmentNames.get()) {
                        listContainer.e(new ElementLabel(), label -> {
                            label.active.setText(enchantName);
                        });
                    }
                });
            });
        });
    }

    private List<String> generateRandomEnchantments() {
        String[] types = {"Protection", "Sharpness", "Efficiency", "Unbreaking",
                "Fortune"};
        String[] levels = {"I", "II", "III", "IV", "V"};
        List<String> list = new ArrayList<>();
        int count = 3 + random.nextInt(5); // Generate 3-7 enchantments
        for (int i = 0; i < count; i++) {
            list.add(types[random.nextInt(types.length)] + " "
                    + levels[random.nextInt(levels.length)]);
        }
        return list;
    }
}
```

## Example 3: Composable Button & Slots

This is an advanced example showing the complete workflow for creating and using a "smart" composable element.

### Part A: The Component Author (Button.java)

This is the code for the Button component itself. It encapsulates its own state and layout.

```java
/**
 * A "smart" composable button.
 * It encapsulates hover state, click logic, and a default layout
 * that can be customized via slots.
 */
public class Button
        extends BaseComposableElement { // Implements IComposableElement

    // --- 1. Public, discoverable SlotKeys for APPEARANCE ---
    public static final SlotKey BACKGROUND_NORMAL =
            new SlotKey("Button.Background.Normal");
    public static final SlotKey BACKGROUND_HOVERED =
            new SlotKey("Button.Background.Hovered");
    public static final SlotKey CONTENT = new SlotKey("Button.Content");

    // --- 2. Internal State ---
    // (useState() is a helper from BaseComposableElement that ties
    // the state's lifecycle to this element)
    private State<Boolean> isHovered = useState(false);
    private String defaultText = "";

    // --- 3. Simple API for the 90% use case ---
    public void setText(String text) {
        this.defaultText = text;
    }

    // --- 4. Encapsulated Behavior (Lifecycle) ---
    @Override
    public void onMouseEntered() {
        this.isHovered.set(true);
    }

    @Override
    public void onMouseExited() {
        this.isHovered.set(false);
    }

    @Override
    public void onClick(MouseClickEvent e) {
        // playSound(...);
        // ... fire onClick handler ...
    }

    // --- 5. Internal Composition ---
    @Override
    public void compose(ICompositionScope internalScope) {
        // The Button's internal layout is a Stack to center content
        internalScope.e(new Stack(), stack -> {
            stack.active.setLayoutProperties(
                    LayoutProperties.create()
                            .withAlignment(Alignment.CENTER)
                            .withPadding(4)); // Default padding

            // --- A. DYNAMIC BACKGROUND ---
            // This container will be re-composed when 'isHovered' changes.
            stack.e(new Stack(), backgroundContainer -> {
                // *** BIND TO STATE ***
                backgroundContainer.bind(this.isHovered);

                // The 'if' statement runs *during composition*
                // because this whole lambda is re-run.
                if (this.isHovered.get()) {
                    // Show the HOVERED slot
                    backgroundContainer.slot(
                            BACKGROUND_HOVERED, defaultHovered -> {
                                // Fallback: If user didn't fill HOVERED,
                                // we render the NORMAL slot instead.
                                defaultHovered.slot(
                                        BACKGROUND_NORMAL, defaultNormal -> {
                                            defaultNormal.e(new ElementBox(
                                                    CommonSprites
                                                            .BUTTON_NORMAL));
                                        });
                            });
                } else {
                    // Show the NORMAL slot
                    backgroundContainer.slot(
                            BACKGROUND_NORMAL, defaultNormal -> {
                                defaultNormal.e(new ElementBox(
                                        CommonSprites.BUTTON_NORMAL));
                            });
                }
            });

            // --- B. STATIC CONTENT ---
            // The content renders on top and is composed only once.
            // It's placed in its own centered Stack slot.
            stack.slot(CONTENT, defaultContent -> {
                defaultContent.e(new ElementLabel(this.defaultText));
            });
        });
    }
}
```

### Part B: The Component User (MyScreen.java)

This is the code for a screen that uses the new Button component in various ways.

```java
/**
 * A screen demonstrating how to *use* the composable Button.
 */
public class MyButtonScreen extends CompoundScreenWithDeclarativeUI {
    @Override
    protected void compose(ICompositionContext context) {
        context.e(new Column(), col -> {
            col.active.setLayoutProperties(
                    LayoutProperties.create()
                            .withSpacing(10)
                            .withAlignment(Alignment.CENTER)
                            .withPadding(20));

            // --- Usage 1: Simple Case ---
            // Just use the simple API. The Button handles all
            // defaults for background, hover, and content.
            col.e(new Button(),
                    button -> { button.active.setText("Simple Button"); });

            // --- Usage 2: Custom Appearance ---
            // Use slots to override the default appearance.
            // The Button *still* handles all hover state and logic.
            col.e(new Button(), button -> {
                // We don't use setText(); we provide custom content.

                // 1. Fill the NORMAL background slot
                button.fillSlot(Button.BACKGROUND_NORMAL, scope -> {
                    scope.e(new ElementBox(CommonSprites.INVENTORY_PANEL),
                            box -> {
                                box.active.setColor(
                                        0x88FFFFFF); // Blue-ish tint
                            });
                });

                // 2. Fill the HOVERED background slot
                button.fillSlot(Button.BACKGROUND_HOVERED, scope -> {
                    scope.e(new ElementBox(CommonSprites.INVENTORY_PANEL),
                            box -> {
                                box.active.setColor(0xFFFFFFFF); // White tint
                            });
                });

                // 3. Fill the CONTENT slot
                button.fillSlot(Button.CONTENT, scope -> {
                    // We can put a whole sub-tree here
                    scope.e(new Row(), row -> {
                        row.active.setLayoutProperties(
                                LayoutProperties.create().withSpacing(5));
                        row.e(new ElementImage(CommonSprites.DIAMOND_ICON),
                                img -> {});
                        row.e(new ElementLabel("Custom Button!"), label -> {});
                    });
                });
            });
        });
    }
}
```
