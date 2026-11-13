package com.tridevmc.molecule.ui;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.ILazyCollection;
import com.tridevmc.compound.ui.element.ItemDisplay;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.event.MouseScrollEvent;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import net.minecraft.client.Minecraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftMockExtension.class)
class GalleryLargeCollectionsTest {
    @Test
    void galleryMountsOnlyVisibleItemsAndScrollsToLastRow() {
        var tree = new UITree();
        var context = mock(IScreenContext.class);
        var font = Minecraft.getInstance().font;
        when(context.getFont()).thenReturn(font);
        var stack = mock(net.minecraft.world.item.ItemStack.class);
        var reads = new java.util.concurrent.atomic.AtomicInteger();
        ICompositionScope.root(tree).e(new GalleryLargeCollections(index -> {
            reads.incrementAndGet();
            return stack;
        }),
                body -> body.layout().fillMax());
        tree.layoutAndRender(450, 200, context);
        var host = tree.findNodesByElementType(ILazyCollection.class).getFirst();
        int mounted = host.getChildCount();
        assertTrue(mounted > 0 && mounted < 220);
        assertEquals(mounted, tree.findNodesByElementType(ItemDisplay.class).size());
        assertEquals(mounted, reads.get());
        tree.layoutAndRender(450, 200, context);
        assertEquals(mounted, reads.get());
        assertTrue(tree.dispatchScroll(5, 30, new MouseScrollEvent(5, 30, -1)));
        tree.layoutAndRender(450, 200, context);
        var area = (ScrollArea) tree.findNodesByElementType(ScrollArea.class).getFirst().getElement();
        area.scrollTo(0, Integer.MAX_VALUE);
        tree.layoutAndRender(450, 200, context);
        assertEquals(area.getMaxScrollY(), area.getScrollYState().get());
        assertTrue(host.getChildCount() < 220);
    }
}
