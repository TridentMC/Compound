package com.tridevmc.compound.ui.integration;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.element.Accordion;
import com.tridevmc.compound.ui.element.Button;
import com.tridevmc.compound.ui.element.Checkbox;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.CycleButton;
import com.tridevmc.compound.ui.element.Divider;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.ProgressBar;
import com.tridevmc.compound.ui.element.RadioButton;
import com.tridevmc.compound.ui.element.RadioButtonGroup;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.ToggleSwitch;
import com.tridevmc.compound.ui.event.KeyInputEvent;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import com.tridevmc.compound.ui.visual.BufferedImageScreenContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftMockExtension.class)
class BasicControlInteractionTest {
    @Test
    void checkboxesAndSwitchesDistinguishUserInputFromSilentUpdates() {
        var checkbox = new Checkbox(Component.literal("Enabled"));
        var toggle = new ToggleSwitch();
        var checks = new ArrayList<Boolean>();
        var toggles = new ArrayList<Boolean>();
        checkbox.setOnCheckedChanged(checks::add);
        toggle.setOnChanged(toggles::add);
        try (var mounted = new Mounted(root -> {
            root.e(checkbox, control -> control.layout().fixedSize(120, 20));
            root.e(toggle, control -> control.layout().fixedSize(50, 20));
        })) {
            assertTrue(mounted.click(checkbox));
            assertTrue(checkbox.isChecked());
            assertTrue(mounted.key(InputConstants.KEY_SPACE, false));
            checkbox.setChecked(true);
            checkbox.setEnabled(false);
            assertFalse(mounted.key(InputConstants.KEY_SPACE, false));
            assertFalse(mounted.click(checkbox));
            assertTrue(checkbox.isChecked());
            assertEquals(List.of(true, false), checks);

            assertTrue(mounted.click(toggle));
            assertTrue(toggle.isOn());
            assertTrue(mounted.key(InputConstants.KEY_RETURN, false));
            toggle.setOn(true);
            toggle.setEnabled(false);
            assertFalse(mounted.key(InputConstants.KEY_SPACE, false));
            assertFalse(mounted.click(toggle));
            assertTrue(toggle.isOn());
            assertEquals(List.of(true, false), toggles);
        }
    }

    @Test
    void cyclingAndRadioSelectionUsePointerKeyboardAndDisabledGuards() {
        var cycle = new CycleButton<>(Component.literal("Mode"), List.of("A", "B", "C"),
                Component::literal);
        var radio = new RadioButtonGroup();
        radio.addOption("First");
        radio.addOption("Second");
        radio.addOption("Third");
        radio.setSelectedIndex(0);
        var modes = new ArrayList<String>();
        var choices = new ArrayList<Integer>();
        cycle.setOnValueChanged(modes::add);
        radio.setOnSelectionChanged(choices::add);
        when(Minecraft.getInstance().hasShiftDown()).thenReturn(false);
        try (var mounted = new Mounted(root -> {
            root.e(cycle, control -> control.layout().fixedSize(120, 20));
            root.e(radio, control -> control.layout().fixedWidth(120));
        })) {
            assertTrue(mounted.click(cycle));
            assertEquals("B", cycle.getValue());
            when(Minecraft.getInstance().hasShiftDown()).thenReturn(true);
            assertTrue(mounted.key(InputConstants.KEY_RETURN, true));
            assertEquals("A", cycle.getValue());
            when(Minecraft.getInstance().hasShiftDown()).thenReturn(false);
            assertTrue(mounted.key(InputConstants.KEY_SPACE, false));
            cycle.setEnabled(false);
            assertFalse(mounted.key(InputConstants.KEY_RETURN, false));
            assertFalse(mounted.click(cycle));
            assertEquals("B", cycle.getValue());
            assertEquals(List.of("B", "A", "B"), modes);

            assertTrue(mounted.click(mounted.label("Second")));
            assertEquals(1, radio.getSelectedIndex());
            assertTrue(mounted.key(InputConstants.KEY_DOWN, false));
            assertEquals(2, radio.getSelectedIndex());
            radio.removeOption(1);
            mounted.prepare();
            assertEquals(1, radio.getSelectedIndex());
            assertEquals(List.of(1, 2), choices);
            radio.setEnabled(false);
            assertFalse(mounted.key(InputConstants.KEY_UP, false));
            assertFalse(mounted.click(mounted.label("First")));
            assertEquals(1, radio.getSelectedIndex());
        } finally {
            when(Minecraft.getInstance().hasShiftDown()).thenReturn(false);
        }
    }

    @Test
    void accordionMountsAndDisposesItsBodyWhileKeepingHeaderFocus() {
        var accordion = new Accordion(Component.literal("Advanced"));
        var changes = new ArrayList<Boolean>();
        var disposals = new AtomicInteger();
        var body = new Rect(0) {
            @Override public void onDetached() { disposals.incrementAndGet(); }
        };
        accordion.setOnExpandedChanged(changes::add);
        try (var mounted = new Mounted(root -> root.e(accordion, section -> {
            section.layout().fixedWidth(120);
            section.fillSlot(Accordion.CONTENT_SLOT,
                    content -> content.e(body, element -> element.layout().fixedSize(100, 20)));
        }))) {
            var header = mounted.tree.findNodesByElementType(Button.class).getFirst();
            assertNull(body.getNode());
            assertTrue(mounted.click(header.getElement()));
            mounted.prepare();
            assertTrue(mounted.tree.hasNode(body));
            assertSame(header, mounted.tree.getFocusedNode());
            assertTrue(mounted.key(InputConstants.KEY_SPACE, false));
            mounted.prepare();
            assertNull(body.getNode());
            assertEquals(1, disposals.get());
            assertSame(header, mounted.tree.getFocusedNode());
            assertTrue(mounted.key(InputConstants.KEY_SPACE, false));
            mounted.prepare();
            assertTrue(mounted.tree.hasNode(body));
            assertEquals(List.of(true, false, true), changes);
        }
    }

    @Test
    void progressAndPassiveIndicatorsDrawTheirLiveNormalizedState() {
        var progress = new ProgressBar();
        var completions = new AtomicInteger();
        progress.setBackgroundColor(0xFF112233);
        progress.setFillColor(0xFF00AA00);
        progress.setOnCompleted(ignored -> completions.incrementAndGet());
        progress.setProgress(25, 50);
        var indicator = new RadioButton(() -> progress.getNormalizedProgress() == 1,
                () -> 0xFF112233, () -> 0xFF00AA00);
        var divider = new Divider(0xFF445566, 2);
        try (var mounted = new Mounted(root -> {
            root.e(progress, bar -> bar.layout().fixedSize(100, 12));
            root.e(indicator);
            root.e(divider, line -> line.layout().fixedWidth(100));
        }); var image = new BufferedImageScreenContext(140, 180)) {
            mounted.tree.renderTree(image);
            assertEquals(0xFF00AA00, image.getImage().getRGB(10, 6));
            assertEquals(0xFF112233, image.getImage().getRGB(75, 6));
            assertEquals(0xFF112233, image.getImage().getRGB(6, 18));
            assertEquals(0xFF445566, image.getImage().getRGB(0, 24));

            progress.setProgress(100);
            progress.setProgress(75);
            mounted.prepare();
            mounted.tree.renderTree(image);
            assertEquals(50, progress.getProgress());
            assertEquals(1, completions.get());
            assertEquals(0xFF00AA00, image.getImage().getRGB(75, 6));
            assertEquals(0xFF00AA00, image.getImage().getRGB(6, 18));

            progress.setProgress(-10);
            mounted.prepare();
            mounted.tree.renderTree(image);
            assertEquals(0, progress.getProgress());
            assertEquals(0xFF112233, image.getImage().getRGB(10, 6));
            assertEquals(1, completions.get());
        }
    }

    private static final class Mounted implements AutoCloseable {
        private final UITree tree = new UITree();
        private final IScreenContext context = mock(IScreenContext.class);

        Mounted(Consumer<ICompositionScope> content) {
            ICompositionScope.root(this.tree).e(new Column(), root -> {
                root.layout().fixedWidth(140);
                content.accept(root);
            });
            this.prepare();
        }

        void prepare() { this.tree.prepareFrame(140, 180, this.context); }

        boolean click(IElement element) {
            var bounds = element.getBounds();
            return this.tree.dispatchClick(bounds.x() + 4, bounds.y() + 4, new MouseClickEvent(
                    bounds.x() + 4, bounds.y() + 4, InputConstants.MOUSE_BUTTON_LEFT,
                    false, false, false));
        }

        boolean key(int code, boolean shift) {
            return this.tree.dispatchKeyPress(new KeyInputEvent(code, code, shift, false, false));
        }

        IElement label(String text) {
            return this.tree.findNodesByElementType(Label.class).stream().map(node -> node.getElement())
                    .filter(element -> ((Label) element).getText().getString().equals(text))
                    .findFirst().orElseThrow();
        }

        @Override public void close() { this.tree.reset(); }
    }
}
