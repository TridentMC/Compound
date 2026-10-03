package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.Row;
import com.tridevmc.compound.ui.element.Spacer;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TreeLayoutTest {
    @Test
    void largeFiniteWeightsDivideRowsAndColumnsNormally() {
        for (var container : List.of(new Row(), new Column())) {
            for (float weight : new float[]{1, Float.MAX_VALUE}) {
                var parent = new TreeNode(container, null);
                for (int i = 0; i < 2; i++) {
                    var child = new TreeNode(new Spacer(10, 10), null);
                    child.setLayoutProperties(LayoutProperties.create().weight(weight));
                    parent.addChild(child);
                }

                new TreeLayout().measureNode(parent, Constraints.loose(100, 100));

                var expected = container instanceof Row ? new Size(50, 10) : new Size(10, 50);
                for (var child : parent.getChildren()) {
                    assertEquals(expected, child.getMeasuredSize());
                }
            }
        }
    }

    @Test
    void paddedWeightedScrollContentKeepsIntrinsicHeight() {
        var column = new TreeNode(new Column(), null);
        column.setLayoutProperties(LayoutProperties.create().padding(4));
        var child = new TreeNode(new Spacer(10, 10), null);
        child.setLayoutProperties(LayoutProperties.create().weight(1));
        column.addChild(child);

        var size = new TreeLayout().measureNode(column, Constraints.loose(100, Integer.MAX_VALUE));

        assertEquals(new Size(18, 18), size);
        assertEquals(new Size(10, 10), child.getMeasuredSize());
    }

    @Test
    void negativeMarginsDoNotOverflowUnboundedConstraints() {
        var constraints = Constraints.unbounded().inset(-6, -6);
        assertFalse(constraints.hasBoundedWidth());
        assertFalse(constraints.hasBoundedHeight());
        assertEquals(Integer.MAX_VALUE, Constraints.subtractInset(Integer.MAX_VALUE, -6));
        assertEquals(0, Constraints.subtractInset(2, 6));
    }
}
