package com.tridevmc.molecule.ui;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Button;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.ListView;
import com.tridevmc.compound.ui.element.Panel;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.element.Tabs;
import com.tridevmc.compound.ui.element.TextInput;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.tree.UITree;
import com.tridevmc.compound.ui.visual.BufferedImageScreenContext;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class UIGalleryVisualTest {
    private static final Path OUTPUT = Path.of("build", "screenshots", "gallery");

    @BeforeEach
    void configureText() {
        var font = Minecraft.getInstance().font;
        when(font.getSplitter()).thenReturn(new StringSplitter((codePoint, style) -> 6F));
        when(font.plainSubstrByWidth(anyString(), anyInt())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0);
            int width = invocation.getArgument(1);
            int count = Math.min(text.codePointCount(0, text.length()), Math.max(0, width / 6));
            return text.substring(0, text.offsetByCodePoints(0, count));
        });
    }

    @Test
    void productionGalleryRendersEveryTabWithoutAClientWindow() {
        var tree = gallery(800, 600);
        try {
            var tabs = (Tabs) tree.findNodesByElementType(Tabs.class).getFirst().getElement();
            assertEquals(8, tabs.getTabCount());
            String[] names = {"controls", "lists", "text", "more", "scrollbars", "checks", "animations", "large-grid"};
            for (int index = 0; index < names.length; index++) {
                tabs.setSelectedTab(index);
                snapshot(tree, 800, 600, names[index]);
            }
        } finally {
            tree.reset();
        }
    }

    @Test
    void listSelectionSurvivesViewportResize() {
        var tree = gallery(640, 360);
        try {
            var tabs = (Tabs) tree.findNodesByElementType(Tabs.class).getFirst().getElement();
            tabs.setSelectedTab(1);
            snapshot(tree, 640, 360, "lists-compact-before");
            var label = tree.findNodesByElementType(Label.class).stream()
                    .filter(node -> ((Label) node.getElement()).getText().getString().equals("Storage entry 3"))
                    .findFirst().orElseThrow();
            var bounds = label.getElement().getBounds();
            int x = bounds.x() + 2;
            int y = bounds.y() + 2;
            assertTrue(tree.dispatchClick(x, y, new MouseClickEvent(x, y,
                    InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
            var list = (ListView<?>) tree.findNodesByElementType(ListView.class).getFirst().getElement();
            assertEquals("Storage entry 3", list.getSelected());
            snapshot(tree, 640, 360, "lists-compact-selected");
            snapshot(tree, 800, 600, "lists-selected-resized");
            assertEquals("Storage entry 3", list.getSelected());
            var fifth = tree.findNodesByElementType(Label.class).stream()
                    .filter(node -> ((Label) node.getElement()).getText().getString().equals("Storage entry 5"))
                    .findFirst().orElseThrow().getBounds();
            int fifthX = fifth.x() + 2;
            int fifthY = fifth.y() + 2;
            assertTrue(tree.dispatchClick(fifthX, fifthY,
                    new MouseClickEvent(fifthX, fifthY, InputConstants.MOUSE_BUTTON_LEFT, false, true, false)));
            assertEquals(List.of("Storage entry 3", "Storage entry 5"), list.getSelectedItems());
            snapshot(tree, 800, 600, "lists-multiple-selected");
        } finally {
            tree.reset();
        }
    }

    private static UITree gallery(int width, int height) {
        var actions = new GalleryActions();
        var name = new TextInput();
        // Item data is a game boundary; the production grid and gallery composition stay real.
        var stack = mock(ItemStack.class);
        var gallery = new UIGallery(null, actions, List.of(
                new GalleryControls(actions, name), new GalleryLists(), new GalleryText(name),
                new GalleryMore(actions), new GalleryScrollbars(), new UIRegressionGallery(),
                new GalleryAnimations(), new GalleryLargeCollections(index -> stack)));
        gallery.width = width;
        gallery.height = height;
        var tree = new UITree();
        gallery.compose(ICompositionScope.root(tree));
        snapshot(tree, width, height, "initial-" + width + "x" + height);
        return tree;
    }

    private static void snapshot(UITree tree, int width, int height, String name) {
        try (var context = new BufferedImageScreenContext(width, height)) {
            context.setFrameNanos(1_000_000_000L);
            context.drawRect(0, 0, width, height, 0xFF30343B);
            tree.layoutAndRender(width, height, context);
            var panel = tree.findNodesByElementType(Panel.class).getFirst().getElement().getBounds();
            assertTrue(panel.x() >= 0 && panel.y() >= 0);
            assertTrue(panel.right() <= width && panel.bottom() <= height);
            assertTrue(panel.width() > 0 && panel.height() > 0);
            var tabs = (Tabs) tree.findNodesByElementType(Tabs.class).getFirst().getElement();
            var selectedHeader = tree.findNodesByElementType(Button.class).get(tabs.getSelectedTab()).getBounds();
            var headerViewport = tree.findNodesByElementType(ScrollArea.class).getFirst().getBounds();
            assertTrue(selectedHeader.left() >= headerViewport.left());
            assertTrue(selectedHeader.right() <= headerViewport.right());
            context.saveTo(OUTPUT.resolve(name + ".png").toString());
        }
    }
}
