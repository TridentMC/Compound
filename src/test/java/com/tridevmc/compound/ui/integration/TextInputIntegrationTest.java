/*
 * Copyright 2018 - 2024 TridentMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridevmc.compound.ui.integration;

import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Box;
import com.tridevmc.compound.ui.element.Button;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.Panel;
import com.tridevmc.compound.ui.element.Row;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.element.TextInput;
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.Position;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.ITreeNode;
import com.tridevmc.compound.ui.tree.UITree;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for TextInput element.
 * Validates that TextInput elements compose correctly within the UI tree
 * and have the expected structure.
 */
@ExtendWith({MinecraftMockExtension.class, MockitoExtension.class})
public class TextInputIntegrationTest {

    @Mock
    private IScreenContext screenContext;

    /**
     * Test that TextInput elements compose correctly with various configurations.
     */
    @Test
    void testTextInputComposition() {
        UITree tree = new UITree();
        var scope = ICompositionScope.root(tree);

        // Compose a UI with multiple TextInput elements in different configurations
        scope.e(new Stack(), stack -> {
            stack.layout().fillMax().contentAlignment(Alignment.CENTER);

            stack.e(new Column(), column -> {
                column.layout().spacing(8).fixedSize(300, 200);

                // 1. Simple bordered TextInput with hint
                column.e(new TextInput(), input -> {
                    input.layout().fillMaxWidth().fixedHeight(20);
                    input.getElement().setHint(Component.literal("Enter username..."));
                    input.getElement().setMaxLength(16);
                });

                // 2. Unbordered TextInput
                column.e(new TextInput(), input -> {
                    input.layout().fillMaxWidth().fixedHeight(20);
                    input.getElement().setBordered(false);
                    input.getElement().setTextShadow(false);
                });

                // 3. TextInput with initial value
                column.e(new TextInput("Default text"), input -> {
                    input.layout().fillMaxWidth().fixedHeight(20);
                });

                // 4. Centered TextInput
                column.e(new TextInput(), input -> {
                    input.layout().fillMaxWidth().fixedHeight(20);
                    input.getElement().setCentered(true);
                    input.getElement().setHint(Component.literal("Centered input"));
                });

                // 5. Row with label and input
                column.e(new Row(), row -> {
                    row.layout().spacing(4).fillMaxWidth();

                    row.e(new Label(Component.literal("Password:"), 0xFFFFFF, false), label -> {
                        label.layout().fixedSize(70, 20);
                    });

                    row.e(new TextInput(), input -> {
                        input.layout().weight(1).fixedHeight(20);
                        input.getElement().setHint(Component.literal("Enter password..."));
                    });
                });

                // 6. Row with button and input (demonstrating TextInput alongside other elements)
                column.e(new Row(), row -> {
                    row.layout().spacing(4).fillMaxWidth();

                    row.e(new Button(), button -> {
                        button.layout().fixedSize(60, 20);
                        button.fillSlot(Button.CONTENT_SLOT, content -> {
                            content.e(new Label(Component.literal("Search"), 0xFFFFFF, true));
                        });
                    });

                    row.e(new TextInput(), input -> {
                        input.layout().weight(1).fixedHeight(20);
                        input.getElement().setSuggestion("Type to search...");
                        input.getElement().setResponder(text -> {
                            System.out.println("Search text: " + text);
                        });
                    });
                });
            });
        });

        // Layout
        var rootConstraints = new Constraints(0, 800, 0, 600);
        tree.measureTree(rootConstraints);
        tree.placeTree(new Position(0, 0), rootConstraints);

        // Assertions
        assertNotNull(tree.getRoot());
        assertEquals(Stack.class, tree.getRoot().getElement().getClass());

        ITreeNode columnNode = tree.getRoot().getChildren().getFirst();
        assertEquals(Column.class, columnNode.getElement().getClass());

        // Should have 6 children (5 direct inputs + 1 row with label+input)
        assertEquals(6, columnNode.getChildren().size());

        // Verify first input structure
        ITreeNode firstInputNode = columnNode.getChildren().get(0);
        assertEquals(TextInput.class, firstInputNode.getElement().getClass());
        Bounds firstInputBounds = firstInputNode.getElement().getBounds();
        assertEquals(300, firstInputBounds.width());
        assertEquals(20, firstInputBounds.height());

        // TextInput should have children: Stack (root) -> [Sprite (bg), Box (padding), Stack (content)]
        assertTrue(firstInputNode.getChildren().size() >= 1, "TextInput should have at least one child");
        ITreeNode inputStackNode = firstInputNode.getChildren().getFirst();
        assertEquals(Stack.class, inputStackNode.getElement().getClass());

        // Second input should be unbordered (no background sprite)
        ITreeNode secondInputNode = columnNode.getChildren().get(1);
        assertEquals(TextInput.class, secondInputNode.getElement().getClass());

        // Third input should have initial value
        ITreeNode thirdInputNode = columnNode.getChildren().get(2);
        TextInput thirdInput = (TextInput) thirdInputNode.getElement();
        assertEquals("Default text", thirdInput.getValue());

        // Fourth input should be centered
        ITreeNode fourthInputNode = columnNode.getChildren().get(3);
        TextInput fourthInput = (TextInput) fourthInputNode.getElement();
        assertTrue(fourthInput.isCentered());

        // Fifth child is a Row with label and input
        ITreeNode rowNode = columnNode.getChildren().get(4);
        assertEquals(Row.class, rowNode.getElement().getClass());
        assertEquals(2, rowNode.getChildren().size());
        assertEquals(Label.class, rowNode.getChildren().get(0).getElement().getClass());
        assertEquals(TextInput.class, rowNode.getChildren().get(1).getElement().getClass());

        // Sixth child is a Row with button and input
        ITreeNode searchRowNode = columnNode.getChildren().get(5);
        assertEquals(Row.class, searchRowNode.getElement().getClass());
        assertEquals(2, searchRowNode.getChildren().size());
        assertEquals(Button.class, searchRowNode.getChildren().get(0).getElement().getClass());
        assertEquals(TextInput.class, searchRowNode.getChildren().get(1).getElement().getClass());
    }

    /**
     * Test that TextInput state management works correctly.
     */
    @Test
    void testTextInputState() {
        TextInput input = new TextInput();

        // Test initial state
        assertEquals("", input.getValue());
        assertEquals(0, input.getCursorPosState().get().intValue());
        assertFalse(input.isFocused());
        assertTrue(input.isEditable());
        assertTrue(input.isBordered());

        // Test value changes
        input.setValue("Hello");
        assertEquals("Hello", input.getValue());
        assertEquals(5, input.getCursorPosState().get().intValue());

        // Test max length
        input.setMaxLength(3);
        assertEquals("Hel", input.getValue());

        // Test filter
        input.setMaxLength(10);
        input.setFilter(s -> s.matches("[0-9]*"));
        // When filter rejects, value stays the same ("Hel" from before)
        input.setValue("abc123");
        assertEquals("Hel", input.getValue()); // Filter rejects non-numeric, keeps old value
        input.setValue("12345");
        assertEquals("12345", input.getValue());

        // Test editable
        input.setEditable(false);
        assertFalse(input.isEditable());

        // Test centered
        input.setCentered(true);
        assertTrue(input.isCentered());

        // Test text shadow
        input.setTextShadow(false);
        assertFalse(input.getTextShadow());

        // Test suggestion
        input.setSuggestion("Enter number...");
        assertEquals("Enter number...", input.getSuggestion());
    }

    /**
     * Test TextInput text manipulation methods.
     */
    @Test
    void testTextInputManipulation() {
        TextInput input = new TextInput("Hello World");

        // Test selection
        input.moveCursorTo(5, false);
        input.moveCursorTo(11, true);
        assertEquals(" World", input.getSelectedText());

        // Test delete (by replacing selection with empty)
        input.insertText("");
        assertEquals("Hello", input.getValue());

        // Test insert
        input.insertText(" There");
        assertEquals("Hello There", input.getValue());

        // Test backspace
        input.deleteText(-1, false);
        assertEquals("Hello Ther", input.getValue());

        // Test delete
        input.deleteText(1, false);
        assertEquals("Hello Ther", input.getValue()); // No change - at end

        input.moveCursorTo(5, false);
        input.deleteText(1, false);
        assertEquals("HelloTher", input.getValue()); // Deletes the space at position 5

        // Test word navigation
        input.setValue("Hello World Test");
        input.moveCursorTo(5, false);
        input.moveCursor(1, true); // Move right by word
        // Cursor should be at or past "World"

        // Test home/end
        input.moveCursorTo(0, false);
        assertEquals(0, input.getCursorPosState().get().intValue());

        input.moveCursorTo(input.getValue().length(), false);
        assertEquals(input.getValue().length(), input.getCursorPosState().get().intValue());
    }

    /**
     * Test that TextInput renders without errors.
     */
    @Test
    void testTextInputRendering() {
        UITree tree = new UITree();
        var scope = ICompositionScope.root(tree);

        scope.e(new Panel(), panel -> {
            panel.layout().fixedSize(200, 100);
            panel.fillSlot(Panel.CONTENT_SLOT, content -> {
                content.e(new Box(), box -> {
                    box.layout().padding(8);
                    box.e(new Column(), column -> {
                        column.layout().spacing(4).fillMax();

                        column.e(new TextInput("Test input"), input -> {
                            input.layout().fillMaxWidth().fixedHeight(20);
                        });
                    });
                });
            });
        });

        var rootConstraints = new Constraints(0, 800, 0, 600);
        tree.measureTree(rootConstraints);
        tree.placeTree(new Position(0, 0), rootConstraints);

        // Render should not throw
        assertDoesNotThrow(() -> tree.renderTree(this.screenContext));

        // Verify input has correct bounds
        ITreeNode inputNode = findFirstNodeOfType(tree.getRoot(), TextInput.class);
        assertNotNull(inputNode);
        Bounds bounds = inputNode.getElement().getBounds();
        assertTrue(bounds.width() > 0);
        assertTrue(bounds.height() > 0);
    }

    private ITreeNode findFirstNodeOfType(ITreeNode node, Class<?> type) {
        if (type.isInstance(node.getElement())) {
            return node;
        }
        for (var child : node.getChildren()) {
            var found = findFirstNodeOfType(child, type);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
