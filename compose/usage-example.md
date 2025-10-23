# **Declarative UI Usage Examples**

This document provides concrete examples of how to build UIs using the declarative framework, based on the API from the api-consumer-guide.md.

## **Example 1: Static Minecraft Chest UI**

This example demonstrates how to use layout primitives (Stack, Column, Grid) and visual primitives (ElementBox, ElementLabel, ElementSlot) to create a complex, static UI that replicates the layout of a double chest.  
This UI is "static" because its structure *never* changes. It's composed once and that's it.
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
    protected void composeUI(CompositionContext context) {  
          
        // 1. Root: Use a Stack to center the entire UI  
        context.e(new Stack(), stack -> {  
            stack.layoutProperties(LayoutProperties.create()  
                .withAlignment(Alignment.CENTER));

            // 2. Main Panel: The dark gray background for the chest  
            stack.e(new ElementBox(), mainPanel -> {  
                // This is the standard 9-slice box primitive  
                mainPanel.setSprite(CommonSprites.INVENTORY_PANEL);   
                mainPanel.layoutProperties(LayoutProperties.create()  
                    .withFixedSize(176, 222) // Standard double chest size  
                    .withPadding(7)); // Standard 7px padding

                // 3. Main Layout: A Column to stack all content vertically  
                mainPanel.e(new Column(), contentColumn -> {  
                    contentColumn.layoutProperties(LayoutProperties.create()  
                        .withSpacing(4)); // 4px spacing between elements

                    // --- CHEST INVENTORY ---  
                    contentColumn.e(new ElementLabel(), label -> {  
                        label.setText("Chest"); // Or pass a Component  
                        label.setColor(0x404040); // Dark gray text color  
                    });

                    contentColumn.e(new Grid(), chestGrid -> {  
                        chestGrid.layoutProperties(LayoutProperties.create()  
                            .withGridSize(9, 6)   // 9 columns, 6 rows  
                            .withSpacing(0));     // No spacing between slots  
                          
                        // Add 54 slots (9x6)  
                        for (int i = 0; i < 54; i++) {  
                            // Assume ElementSlot is a primitive that  
                            // takes a slot index or a Slot object.  
                            chestGrid.e(new ElementSlot(this.menu.getSlot(i)), slot -> {  
                                // Slot is 18x18, which is its default size  
                            });  
                        }  
                    });

                    // --- PLAYER INVENTORY ---  
                    contentColumn.e(new ElementLabel(), label -> {  
                        label.setText("Player Inventory");  
                        label.setColor(0x404040);  
                    });

                    contentColumn.e(new Grid(), playerGrid -> {  
                        playerGrid.layoutProperties(LayoutProperties.create()  
                            .withGridSize(9, 3) // 9 columns, 3 rows  
                            .withSpacing(0));  
                          
                        // Add 27 slots (9x3)  
                        for (int i = 0; i < 27; i++) {  
                            // We map these to the correct slot indices  
                            int slotIndex = 54 + i;  
                            playerGrid.e(new ElementSlot(this.menu.getSlot(slotIndex)), slot -> {});  
                        }  
                    });

                    // --- PLAYER HOTBAR ---  
                    // We add extra space before the hotbar  
                    contentColumn.e(new ElementSpacer(), spacer -> {  
                        spacer.layoutProperties(LayoutProperties.create().withFixedSize(0, 4));  
                    });

                    contentColumn.e(new Grid(), hotbarGrid -> {  
                        hotbarGrid.layoutProperties(LayoutProperties.create()  
                            .withGridSize(9, 1) // 9 columns, 1 row  
                            .withSpacing(0));  
                          
                        // Add 9 slots (9x1)  
                        for (int i = 0; i < 9; i++) {  
                            int slotIndex = 54 + 27 + i;  
                            hotbarGrid.e(new ElementSlot(this.menu.getSlot(slotIndex)), slot -> {});  
                        }  
                    });  
                });  
            });  
        });  
    }  
}
```

## **Example 2: Dynamic Enchantment List**

This example demonstrates how to build a dynamic UI using **both** types of reactivity:

1. **Reactive Structure (bind()):** A scrollable list of enchantments is built from a State<List<Enchantment>>.  
2. **Reactive Properties (State<T>):** A "details" panel shows information about the *selected* enchantment, and its text updates automatically when the selection changes.

```java
import java.util.Random;

// A simple data class for our example, using a record  
record Enchantment(String name, String description) {}

/**  
 * A screen that shows a dynamic, scrollable list of enchantments.  
 */  
public class MyEnchantingScreen extends CompoundScreenWithDeclarativeUI {

    // --- 1. APPLICATION STATE ---  
    private State<List<Enchantment>> enchantments;  
    private State<Enchantment> selectedEnchantment;  
    private final Random random = new Random();  
      
    @Override  
    public void init() {  
        super.init();  
          
        // Initialize our state objects  
        this.enchantments = useState(generateRandomEnchantments());  
        this.selectedEnchantment = useState(null); // Nothing selected initially  
    }

    @Override  
    protected void composeUI(CompositionContext context) {  
          
        // Root: A Row to split the screen into [List | Details]  
        context.e(new Row(), root -> {  
            root.layoutProperties(LayoutProperties.create()  
                .withFixedSize(300, 200) // Fixed size for our UI  
                .withPadding(10)  
                .withSpacing(10));

            // --- 2. LEFT PANEL (DYNAMIC LIST) ---  
            root.e(new ScrollContainer(), scrollBox -> {  
                scrollBox.layoutProperties(LayoutProperties.create()  
                    .withWeight(1) // Takes 50% of the width  
                    .withPadding(4));  
                scrollBox.setBackground(CommonSprites.INVENTORY_PANEL);  
                  
                // This Column will hold our dynamic children  
                scrollBox.e(new Column(), listContainer -> {  
                    listContainer.layoutProperties(LayoutProperties.create().withSpacing(2));

                    // *** REACTIVE STRUCTURE ***  
                    // Bind the 'enchantments' state to this container.  
                    // This lambda will re-run, rebuilding all children,  
                    // whenever 'enchantments' is updated.  
                    listContainer.bind(this.enchantments);
                    for (Enchantment enchant : currentEnchants) {
                        scope.e(new ElementButton(), button -> {
                            button.setText(enchant.name());

                            // On click, update the 'selectedEnchantment' state  
                            button.onClick(e -> this.selectedEnchantment.set(enchant));
                        });
                    }
                });  
            });

            // --- 3. RIGHT PANEL (DETAILS) ---  
            root.e(new Column(), detailsPanel -> {  
                detailsPanel.layoutProperties(LayoutProperties.create()  
                    .withWeight(1) // Takes 50% of the width  
                    .withSpacing(5));

                // *** REACTIVE PROPERTY ***  
                // This label's text is provided by a Supplier (lambda)  
                // that reads the 'selectedEnchantment' state.  
                // It will re-render its text every frame.  
                detailsPanel.e(new ElementLabel(), title -> {  
                    title.setText(() -> {  
                        Enchantment selected = this.selectedEnchantment.get();  
                        return selected != null ? selected.name() : "---";  
                    });  
                });  
                  
                // *** REACTIVE PROPERTY ***  
                detailsPanel.e(new ElementLabel(), description -> {  
                    description.setWraps(true);  
                    description.setText(() -> {  
                        Enchantment selected = this.selectedEnchantment.get();  
                        return selected != null ? selected.description() : "Select an enchantment to see details.";  
                    });  
                });

                // --- 4. REROLL BUTTON ---  
                detailsPanel.e(new ElementButton(), rerollButton -> {  
                    rerollButton.setText("Reroll (Costs 3 Lapis)");  
                    rerollButton.onClick(e -> {  
                        // This updates the 'enchantments' state,  
                        // which triggers the 'listContainer.bind()' to re-run.  
                        this.enchantments.set(generateRandomEnchantments());  
                          
                        // Clear the selection  
                        this.selectedEnchantment.set(null);  
                    });  
                });  
            });  
        });  
    }

    private List<Enchantment> generateRandomEnchantments() {  
        String[] types = {"Protection", "Sharpness", "Efficiency", "Unbreaking", "Fortune"};  
        String[] levels = {"I", "II", "III", "IV", "V"};  
        List<Enchantment> list = new ArrayList<>();  
        int count = 3 + random.nextInt(5); // Generate 3-7 enchantments  
        for (int i = 0; i < count; i++) {  
            String name = types[random.nextInt(types.length)] + " " + levels[random.nextInt(levels.length)];  
            list.add(new Enchantment(name, "A description for " + name));  
        }  
        return list;  
    }  
}  
```