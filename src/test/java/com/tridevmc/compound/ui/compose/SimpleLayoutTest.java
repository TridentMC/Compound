package com.tridevmc.compound.ui.compose;

import com.tridevmc.compound.ui.compose.element.Column;
import com.tridevmc.compound.ui.compose.element.ElementSpacer;
import com.tridevmc.compound.ui.compose.element.Stack;
import com.tridevmc.compound.ui.compose.layout.Alignment;
import com.tridevmc.compound.ui.compose.layout.Constraints;
import com.tridevmc.compound.ui.compose.layout.Position;
import com.tridevmc.compound.ui.compose.scope.RootScope;
import com.tridevmc.compound.ui.compose.tree.UITree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit test to verify the compose UI layout system works correctly.
 * This test avoids classes with Minecraft static dependencies like ComposedSlot.
 */
public class SimpleLayoutTest {

    @Test
    void testBasicStackLayout() {
        // Create a UI tree
        UITree tree = new UITree();
        RootScope rootScope = new RootScope(tree);

        // Build a simple layout: Stack with centered Column containing spacers
        rootScope.e(new Stack(), stack -> {
            stack.layout().contentAlignment(Alignment.CENTER);

            stack.e(new Column(), column -> {
                column.layout().padding(10).spacing(5);

                column.e(new ElementSpacer(), spacer -> {
                    spacer.layout().fixedSize(100, 20);
                });

                column.e(new ElementSpacer(), spacer -> {
                    spacer.layout().fixedSize(100, 20);
                });

                column.e(new ElementSpacer(), spacer -> {
                    spacer.layout().fixedSize(100, 20);
                });
            });
        });

        // Measure and place the tree
        tree.measureTree(new Constraints(0, 400, 0, 300));
        tree.placeTree(new Position(0, 0));

        // Verify the tree structure
        assertNotNull(tree.getRoot());
        // Stack is the root node itself
        assertEquals(Stack.class, tree.getRoot().getElement().getClass());
        assertEquals(1, tree.getRoot().getChildren().size(), "Stack should have one child (Column)");

        // Verify Stack contains Column
        var stackNode = tree.getRoot();
        var columnNode = stackNode.getChildren().getFirst();
        assertEquals(Column.class, columnNode.getElement().getClass());
        assertEquals(3, columnNode.getChildren().size(), "Column should have 3 spacers");

        // Verify spacer sizes
        for (int i = 0; i < 3; i++) {
            var spacerNode = columnNode.getChildren().get(i);
            assertEquals(ElementSpacer.class, spacerNode.getElement().getClass());
            var bounds = spacerNode.getElement().getBounds();
            assertEquals(100, bounds.width(), "Spacer width should be 100");
            assertEquals(20, bounds.height(), "Spacer height should be 20");
        }

        // Verify column has proper spacing between elements
        var spacer0Bounds = columnNode.getChildren().get(0).getElement().getBounds();
        var spacer1Bounds = columnNode.getChildren().get(1).getElement().getBounds();
        var spacer2Bounds = columnNode.getChildren().get(2).getElement().getBounds();

        // Check vertical spacing
        assertEquals(spacer0Bounds.y() + 20 + 5, spacer1Bounds.y(), "Should have 5px spacing between spacer 0 and 1");
        assertEquals(spacer1Bounds.y() + 20 + 5, spacer2Bounds.y(), "Should have 5px spacing between spacer 1 and 2");
    }

    @Test
    void testConstraints() {
        Constraints constraints = new Constraints(0, 100, 0, 200);

        assertEquals(0, constraints.minWidth());
        assertEquals(100, constraints.maxWidth());
        assertEquals(0, constraints.minHeight());
        assertEquals(200, constraints.maxHeight());

        assertTrue(constraints.hasBoundedWidth());
        assertTrue(constraints.hasBoundedHeight());
    }
}
