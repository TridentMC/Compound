package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.test.MinecraftMockExtension;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftMockExtension.class)
class StyledInputTextReloadTest {
    @Test
    void sameFontReloadKeepsCaretAndPointerAdvancesAlignedWithDrawing() {
        var glyphWidth = new AtomicInteger(6);
        var splitter = new StringSplitter((codePoint, style) -> glyphWidth.get());
        var font = mock(Font.class);
        when(font.getSplitter()).thenReturn(splitter);
        when(font.width(any(FormattedText.class))).thenAnswer(call ->
                (int) Math.ceil(splitter.stringWidth((FormattedText) call.getArgument(0))));
        var text = StyledInputText.of("ab", FormattedCharSequence.forward("ab", Style.EMPTY));
        assertEquals(12, text.width(font));
        assertEquals(12, text.advance(font, 2));
        assertEquals(1, text.positionAt(font, 6));

        glyphWidth.set(8);

        assertEquals(16, text.width(font));
        assertAll(
                () -> assertEquals(16, text.advance(font, 2)),
                () -> assertEquals(1, text.positionAt(font, 8)));
    }
}
