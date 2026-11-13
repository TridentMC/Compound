package com.tridevmc.compound.ui.integration;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Button;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.element.Tooltip;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.geometry.api.ITransform2D;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftMockExtension.class)
class TooltipRenderingTest {
    @Test
    void boundaryAnchorsInvokeNativeTooltipRenderer() {
        for (var offset : List.of(new int[]{0, 0}, new int[]{0, 20}, new int[]{20, 0})) {
            var tree = new UITree();
            var text = Component.literal("boundary tooltip");
            var tooltip = new Tooltip(text);
            tooltip.setDelay(0);
            var clicks = new AtomicInteger();
            ICompositionScope.root(tree).e(new Stack(), root -> {
                root.layout().fillMax();
                root.e(tooltip, anchor -> {
                    anchor.layout().fixedSize(20, 20).margin(offset[0], offset[1], 0, 0);
                    anchor.fillSlot(Tooltip.CONTENT_SLOT, content ->
                            content.e(new Button(), button -> {
                                button.layout().fixedSize(20, 20);
                                button.getElement().addPressListener((x, y) -> clicks.incrementAndGet());
                            }));
                });
            });
            var context = mock(IScreenContext.class);
            var font = Minecraft.getInstance().font;
            var lines = List.of(text.getVisualOrderText());
            when(context.getFont()).thenReturn(font);
            when(context.getMouseX()).thenReturn((double) offset[0] + 5);
            when(context.getMouseY()).thenReturn((double) offset[1] + 5);
            when(font.split(text, 200)).thenReturn(lines);
            try {
                tree.layoutAndRender(100, 100, context);
                verify(context).drawProcessorAsTooltip(lines, offset[0] + 5, offset[1] + 5, font);
                assertEquals(20, tooltip.getBounds().width());
                assertEquals(20, tooltip.getBounds().height());
                assertTrue(tree.dispatchClick(offset[0], offset[1], new MouseClickEvent(
                        offset[0], offset[1], InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
                assertEquals(1, clicks.get());
            } finally {
                tree.reset();
            }
        }
    }

    @Test
    void transformedAnchorHoverUsesRenderedGeometry() {
        var tree = new UITree();
        var text = Component.literal("transformed tooltip");
        var tooltip = new Tooltip(text);
        tooltip.setDelay(0);
        var shown = new AtomicInteger();
        var hidden = new AtomicInteger();
        tooltip.setOnShow(ignored -> shown.incrementAndGet());
        tooltip.setOnHide(ignored -> hidden.incrementAndGet());
        ICompositionScope.root(tree).e(new Stack(), root -> {
            root.layout().fillMax();
            root.e(tooltip, anchor -> {
                anchor.layout().fixedSize(20, 20);
                anchor.transform(() -> ITransform2D.translation(40, 20));
                anchor.fillSlot(Tooltip.CONTENT_SLOT, content ->
                        content.e(new Rect(0), rect -> rect.layout().fixedSize(20, 20)));
            });
        });
        var context = mock(IScreenContext.class);
        var font = Minecraft.getInstance().font;
        var lines = List.of(text.getVisualOrderText());
        when(context.getFont()).thenReturn(font);
        when(context.getMouseX()).thenReturn(45.0);
        when(context.getMouseY()).thenReturn(25.0);
        when(font.split(text, 200)).thenReturn(lines);
        try {
            tree.layoutAndRender(100, 100, context);
            verify(context).drawProcessorAsTooltip(lines, 45, 25, font);
            assertEquals(1, shown.get());

            when(context.getMouseX()).thenReturn(5.0);
            tree.getNodeForElement(tooltip).transform(() -> ITransform2D.translation(-10, 20));
            tree.layoutAndRender(100, 100, context);
            verify(context).drawProcessorAsTooltip(lines, 5, 25, font);

            when(context.getMouseY()).thenReturn(5.0);
            tree.renderTree(context);
            verify(context, times(1)).drawProcessorAsTooltip(lines, 45, 25, font);
            assertEquals(1, hidden.get());
        } finally {
            tree.reset();
        }
    }
}
