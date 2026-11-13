package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.animation.Easing;
import com.tridevmc.compound.ui.element.Button;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.CycleButton;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.TextArea;
import com.tridevmc.compound.ui.element.TextInput;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.state.State;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

import java.util.List;

final class GalleryText extends GalleryModule {
    private final TextArea notes = new TextArea();
    private final State<Boolean> narrowNotes = State.of(false);
    private final CycleButton<TextArea.CursorAnimationMode> cursorStyle = new CycleButton<>(
            Component.literal("Cursor"), List.of(TextArea.CursorAnimationMode.values()),
            mode -> Component.literal(mode == TextArea.CursorAnimationMode.INSTANT ? "Blink" : "Ease in/out"));

    GalleryText(TextInput name) {
        this.notes.setHint(Component.literal("Write a note..."));
        this.notes.setMaxLength(4000);
        this.cursorStyle.setOnValueChanged(mode -> {
            Easing easing = mode == TextArea.CursorAnimationMode.INSTANT ? Easing.STEP : Easing.EASE_IN_OUT;
            name.setCursorAnimation(600, easing);
            this.notes.setCursorAnimation(600, easing);
        });
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.e(new Column(), column -> {
            column.layout().fillMax().spacing(6);
            column.onKeyPress(event -> {
                if (event.keyCode() != InputConstants.KEY_F6) return false;
                this.narrowNotes.set(!this.narrowNotes.get());
                return true;
            });
            column.e(this.cursorStyle, control -> {
                control.layout().fillMaxWidth().fixedHeight(20);
                control.fillSlot(Button.CONTENT_SLOT, content -> content.e(new Label(
                        () -> Component.literal(this.cursorStyle.getValue() == TextArea.CursorAnimationMode.INSTANT
                                ? "Caret animation: Blink" : "Caret animation: Ease in/out"), () -> 0xFFFFFF)));
            });
            column.e(new Label(Component.literal("F6: resize notes | Type, select, paste, and scroll."),
                    0x404040, false));
            column.e(this.notes, area -> {
                area.layout().fillMaxWidth().weight(1).deferred(layout -> {
                    layout.bind(this.narrowNotes);
                    layout.layout().maxWidth(this.narrowNotes.get() ? 190 : 440);
                });
            });
        });
    }
}
