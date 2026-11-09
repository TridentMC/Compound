package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.ui.element.Column;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeNodeLifecycleTest {
    @Test
    void preservesExternalHandlersAndTheirOrderAcrossRepeatedComposition() {
        var node = new TreeNode(new Column(), null);
        var calls = new ArrayList<String>();
        node.addMouseEnterHandler(() -> calls.add("before"));
        node.preserveHandlers();
        node.setCompositionFunction(() -> node.addMouseEnterHandler(() -> calls.add("composed")));
        node.runComposition();
        node.addMouseEnterHandler(() -> calls.add("after"));

        for (int pass = 0; pass < 3; pass++) {
            calls.clear();
            node.getMouseEnterHandlers().forEach(Runnable::run);
            assertEquals(List.of("before", "composed", "after"), calls);
            node.clearHandlers();
            node.runComposition();
        }
    }
}
