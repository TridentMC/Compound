package com.tridevmc.compound.ui.integration;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.event.MouseScrollEvent;
import com.tridevmc.compound.ui.scope.RootScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class ScrollInputTest {
    @Test
    void horizontalWheelMovesContent() {
        var tree = new UITree();
        var area = new ScrollArea(ScrollArea.Direction.HORIZONTAL);
        new RootScope(tree).e(area, scroll -> {
            scroll.layout().fixedSize(100, 40);
            scroll.fillSlot(ScrollArea.CONTENT_SLOT, content ->
                    content.e(new Rect(0), rect -> rect.layout().fixedSize(300, 10)));
        });
        tree.prepareFrame(100, 40, mock(IScreenContext.class));

        assertTrue(tree.dispatchScroll(10, 5, new MouseScrollEvent(10, 5, -1, 0)));
        assertEquals(20, area.getScrollXState().get());
    }

    @Test
    void nestedScrollAtItsLimitBubblesToParent() {
        var tree = new UITree();
        var outer = new ScrollArea();
        var inner = new ScrollArea();
        new RootScope(tree).e(outer, scroll -> {
            scroll.layout().fixedSize(100, 100);
            scroll.fillSlot(ScrollArea.CONTENT_SLOT, content -> content.e(new Column(), column -> {
                column.layout().fillMaxWidth();
                column.e(inner, nested -> {
                    nested.layout().fillMaxWidth().fixedHeight(40);
                    nested.fillSlot(ScrollArea.CONTENT_SLOT, body ->
                            body.e(new Rect(0), rect -> rect.layout().fixedSize(30, 120)));
                });
                column.e(new Rect(0), rect -> rect.layout().fixedSize(30, 200));
            }));
        });
        var context = mock(IScreenContext.class);
        tree.prepareFrame(100, 100, context);
        inner.scrollTo(0, inner.getMaxScrollY());
        tree.prepareFrame(100, 100, context);

        assertTrue(tree.dispatchScroll(10, 5, new MouseScrollEvent(10, 5, -1)));
        assertEquals(20, outer.getScrollYState().get());
        assertEquals(inner.getMaxScrollY(), inner.getScrollYState().get());
    }
}
