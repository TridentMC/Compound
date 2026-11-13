package com.tridevmc.compound.ui.integration;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Button;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.element.Slider;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.element.TextArea;
import com.tridevmc.compound.ui.element.TextInput;
import com.tridevmc.compound.ui.event.CharEvent;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseDragEvent;
import com.tridevmc.compound.ui.geometry.api.ITransform2D;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftMockExtension.class)
class TransformedControlInputTest {
    private final IScreenContext context = mock(IScreenContext.class);

    @BeforeEach
    void font() {
        var font = Minecraft.getInstance().font;
        when(font.getSplitter()).thenReturn(new StringSplitter((codePoint, style) -> 6F));
        when(font.plainSubstrByWidth(anyString(), anyInt())).thenAnswer(call -> {
            String text = call.getArgument(0);
            int width = call.getArgument(1);
            return text.substring(0, Math.clamp(width / 6, 0, text.length()));
        });
    }

    private UITree mount(Consumer<ICompositionScope> content) {
        var tree = new UITree();
        ICompositionScope.root(tree).e(new Stack(), screen -> {
            screen.layout().fillMax();
            screen.e(new Stack(), transformed -> {
                transformed.layout().fixedSize(100, 100);
                transformed.transform(() -> ITransform2D.of(80, 30, 0, 2, 2, 0, 0));
                content.accept(transformed);
            });
        });
        for (int pass = 0; pass < 3; pass++) tree.prepareFrame(400, 300, this.context);
        return tree;
    }

    private void click(UITree tree, int localX, int localY) {
        int x = 80 + localX * 2;
        int y = 30 + localY * 2;
        assertTrue(tree.dispatchClick(x, y, new MouseClickEvent(
                x, y, InputConstants.MOUSE_BUTTON_LEFT, false, false, false)));
    }

    @Test
    void transformedButtonActivatesAndPreservesWorldCoordinatesForListeners() {
        var button = new Button();
        var presses = new AtomicInteger();
        var pointer = new double[2];
        button.addPressListener((x, y) -> {
            presses.incrementAndGet();
            pointer[0] = x;
            pointer[1] = y;
        });
        var tree = this.mount(scope -> scope.e(button, element -> element.layout().fixedSize(100, 20)));
        this.click(tree, 50, 10);
        assertEquals(1, presses.get());
        assertEquals(180, pointer[0]);
        assertEquals(50, pointer[1]);
        tree.reset();
    }

    @Test
    void translatedScaledSliderUsesVisualPositionForClickAndCapturedDrag() {
        var slider = new Slider(0, 1, 0);
        var tree = this.mount(scope -> scope.e(slider, element -> element.layout().fixedSize(100, 20)));
        this.click(tree, 50, 10);
        assertEquals(.5, slider.getValue(), 1e-8);
        assertTrue(tree.dispatchMouseDrag(280, 50, new MouseDragEvent(
                InputConstants.MOUSE_BUTTON_LEFT, 280, 50, 100, 0)));
        assertEquals(1, slider.getValue(), 1e-8);
        tree.reset();
    }

    @Test
    void transformedSingleLineEditorPlacesCaretAtClickedCharacter() {
        var input = new TextInput("abcd");
        var tree = this.mount(scope -> scope.e(input, element -> element.layout().fixedSize(100, 20)));
        this.click(tree, 10, 10);
        assertEquals(1, input.getCursorPosState().get());
        tree.reset();
    }

    @Test
    void transformedMultilineEditorUsesBothPointerAxes() {
        var input = new TextArea();
        input.setValue("abcd\nefgh");
        var tree = this.mount(scope -> scope.e(input, element -> element.layout().fixedSize(100, 40)));
        this.click(tree, 10, 15);
        assertTrue(tree.dispatchCharTyped(new CharEvent('X', 0)));
        assertEquals("abcd\neXfgh", input.getValue());
        tree.reset();
    }

    @Test
    void transformedScrollbarUsesVisualTrackPosition() {
        var area = new ScrollArea();
        var tree = this.mount(scope -> scope.e(area, element -> {
            element.layout().fixedSize(100, 100);
            element.fillSlot(ScrollArea.CONTENT_SLOT, body ->
                    body.e(new Rect(0), rect -> rect.layout().fixedSize(20, 300)));
        }));
        this.click(tree, 98, 50);
        assertTrue(Math.abs(area.getScrollYState().get() - area.getMaxScrollY() / 2) <= 2);
        tree.reset();
    }
}
