package com.tridevmc.molecule.ui;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.animation.Easing;
import com.tridevmc.compound.ui.debug.DebugOverlayConfig;
import com.tridevmc.compound.ui.element.ContextMenu;
import com.tridevmc.compound.ui.element.CycleButton;
import com.tridevmc.compound.ui.element.Label;
import com.tridevmc.compound.ui.element.ScrollArea;
import com.tridevmc.compound.ui.element.Tabs;
import com.tridevmc.compound.ui.element.TextArea;
import com.tridevmc.compound.ui.element.TextInput;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.Position;
import com.tridevmc.compound.ui.screen.ComposedUI;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.tree.ITreeNode;
import com.tridevmc.compound.ui.tree.UITree;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftMockExtension.class)
class UIGalleryInputTest {
    private UIGallery screen;
    private UITree tree;
    private GalleryActions actions;
    private List<GalleryModule> modules;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        when(Minecraft.getInstance().font.getSplitter())
                .thenReturn(new StringSplitter((codePoint, style) -> 6F));
        clearInvocations(Minecraft.getInstance().textInputManager());
        when(Minecraft.getInstance().font.plainSubstrByWidth(anyString(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var keyboardField = Minecraft.class.getDeclaredField("keyboardHandler");
        keyboardField.setAccessible(true);
        keyboardField.set(Minecraft.getInstance(), mock(net.minecraft.client.KeyboardHandler.class));
        this.actions = new GalleryActions();
        var name = spy(new TextInput());
        this.modules = List.of(spy(new GalleryControls(this.actions, name)), spy(new GalleryLists()),
                spy(new GalleryText(name)), spy(new GalleryMore(this.actions)),
                spy(new GalleryScrollbars()), spy(new UIRegressionGallery()), spy(new GalleryAnimations()));
        this.screen = new UIGallery(null, this.actions, this.modules);
        this.screen.width = 800;
        this.screen.height = 600;
        var initMethod = ComposedUI.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(this.screen);
        var treeField = ComposedUI.class.getDeclaredField("tree");
        treeField.setAccessible(true);
        this.tree = (UITree) treeField.get(this.screen);
        this.layout();
    }

    @Test
    void rightClickOpensActions() {
        var target = this.actionTarget();
        assertTrue(this.screen.mouseClicked(this.click(target, InputConstants.MOUSE_BUTTON_RIGHT), false));
        assertTrue(this.contextMenu().isVisible());
    }

    @Test
    void leftClickDoesNotOpenActions() {
        var target = this.actionTarget();
        this.screen.mouseClicked(this.click(target, InputConstants.MOUSE_BUTTON_LEFT), false);
        assertFalse(this.contextMenu().isVisible());
    }

    @Test
    void ordinaryTypingReachesTextInput() {
        var node = this.find(nodeToFind -> nodeToFind.getElement() instanceof TextInput);
        assertTrue(this.screen.mouseClicked(this.click(node, InputConstants.MOUSE_BUTTON_LEFT), false));
        verify(Minecraft.getInstance().textInputManager())
                .onTextInputFocusChange(node.getElement(), true);
        this.layout();
        assertTrue(this.screen.charTyped(new CharacterEvent('a')));
        assertTrue(this.screen.charTyped(new CharacterEvent('Z')));
        var input = (TextInput) node.getElement();
        assertEquals("aZ", input.getValue());
        input.setEditable(false);
        verify(Minecraft.getInstance().textInputManager())
                .onTextInputFocusChange(input, false);
        input.setEditable(true);
        this.screen.mouseClicked(new MouseButtonEvent(0, 0,
                new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
        verify(Minecraft.getInstance().textInputManager(), times(2))
                .onTextInputFocusChange(input, false);
    }

    @Test
    void ordinaryTypingReachesTextArea() {
        this.selectTextTab();
        var node = this.find(nodeToFind -> nodeToFind.getElement() instanceof TextArea);
        assertTrue(this.screen.mouseClicked(this.click(node, InputConstants.MOUSE_BUTTON_LEFT), false));
        verify(Minecraft.getInstance().textInputManager())
                .onTextInputFocusChange(node.getElement(), true);
        this.layout();
        assertTrue(this.screen.charTyped(new CharacterEvent('a')));
        assertTrue(this.screen.charTyped(new CharacterEvent(0x1F642)));
        var area = (TextArea) node.getElement();
        assertEquals("a\uD83D\uDE42", area.getValue());
        area.setEditable(false);
        verify(Minecraft.getInstance().textInputManager())
                .onTextInputFocusChange(area, false);
        area.setEditable(true);
        this.screen.removed();
        verify(Minecraft.getInstance().textInputManager(), times(2))
                .onTextInputFocusChange(area, false);
    }

    @Test
    void pasteStillReachesTextInputAndTextArea() {
        when(Minecraft.getInstance().keyboardHandler.getClipboard()).thenReturn("pasted");
        var input = this.find(node -> node.getElement() instanceof TextInput);
        this.screen.mouseClicked(this.click(input, InputConstants.MOUSE_BUTTON_LEFT), false);
        assertTrue(this.screen.keyPressed(this.paste()));
        assertEquals("pasted", ((TextInput) input.getElement()).getValue());
        this.selectTextTab();
        var area = this.find(node -> node.getElement() instanceof TextArea);
        this.screen.mouseClicked(this.click(area, InputConstants.MOUSE_BUTTON_LEFT), false);
        assertTrue(this.screen.keyPressed(this.paste()));
        assertEquals("pasted", ((TextArea) area.getElement()).getValue());
    }

    @Test
    void modulesComposeOnlyOnMountAndExplicitInvalidation() {
        this.assertCompositions(1, 0, 0, 0, 0, 0);
        for (int frame = 0; frame < 5; frame++) this.layout();
        this.tree.requestRemeasure(this.modules.getFirst().getNode());
        this.layout();
        this.assertCompositions(1, 0, 0, 0, 0, 0);
        this.selectTab(0);
        this.assertCompositions(1, 0, 0, 0, 0, 0);
        this.selectTab(1);
        this.assertCompositions(1, 1, 0, 0, 0, 0);
        this.tree.requestRecompose(this.modules.get(1).getNode());
        this.layout();
        this.assertCompositions(1, 2, 0, 0, 0, 0);
        this.selectTab(0);
        this.assertCompositions(2, 2, 0, 0, 0, 0);
        for (int index = 2; index < 6; index++) this.selectTab(index);
        this.assertCompositions(2, 2, 1, 1, 1, 1);
        for (int frame = 0; frame < 5; frame++) this.layout();
        this.assertCompositions(2, 2, 1, 1, 1, 1);
    }

    @Test
    void widgetChangesAndActionSuppliersDoNotReplayControls() {
        var input = (TextInput) this.find(node -> node.getElement() instanceof TextInput).getElement();
        input.setValue("Retained name");
        this.actions.count();
        this.actions.dialog.show();
        this.layout();
        this.actions.dialog.close();
        this.layout();
        var counter = (Label) this.find(node -> node.getElement() instanceof Label label
                && label.getText().getString().equals("Actions: 1")).getElement();
        assertEquals("Actions: 1", counter.getText().getString());
        this.assertCompositions(1, 0, 0, 0, 0, 0);
        this.selectTab(1);
        this.selectTab(0);
        assertSame(input, this.find(node -> node.getElement() instanceof TextInput).getElement());
        assertEquals("Retained name", input.getValue());
        assertEquals(1, this.actions.countValue());
    }

    @Test
    void notesResizeIsLayoutOnlyAndValuesSurviveRemount() {
        this.selectTextTab();
        var node = this.find(candidate -> candidate.getElement() instanceof TextArea);
        var notes = (TextArea) node.getElement();
        this.screen.mouseClicked(this.click(node, InputConstants.MOUSE_BUTTON_LEFT), false);
        notes.setValue("Retained notes");
        this.layout();
        int wide = notes.getBounds().width();
        assertTrue(this.screen.keyPressed(new KeyEvent(InputConstants.KEY_F6, 0, 0)));
        this.layout();
        assertTrue(notes.getBounds().width() < wide);
        this.assertCompositions(1, 0, 1, 0, 0, 0);
        this.selectTab(0);
        this.selectTextTab();
        assertSame(notes, this.find(candidate -> candidate.getElement() instanceof TextArea).getElement());
        assertEquals("Retained notes", notes.getValue());
        assertEquals(190, notes.getBounds().width());
        this.assertCompositions(2, 0, 2, 0, 0, 0);
    }

    @Test
    void scrollbarSamplesRetainScrollPositionsAcrossTabs() {
        this.selectTab(4);
        var scroll = (ScrollArea) this.find(node -> node.getElement() instanceof ScrollArea area
                && area.getMaxScrollY() > 0).getElement();
        scroll.scrollTo(0, 70);
        this.layout();
        int position = scroll.getScrollYState().get();
        assertTrue(position > 0);
        this.selectTab(0);
        this.selectTab(4);
        assertSame(scroll, this.find(node -> node.getElement() instanceof ScrollArea area
                && area.getMaxScrollY() > 0).getElement());
        assertEquals(position, scroll.getScrollYState().get());
        this.assertCompositions(2, 0, 0, 0, 2, 0);
    }

    @Test
    void caretStyleStillUpdatesTheNameOnAnotherTab() {
        var name = (TextInput) this.find(node -> node.getElement() instanceof TextInput).getElement();
        this.selectTextTab();
        var cursor = (CycleButton<?>) this.find(node -> node.getElement() instanceof CycleButton).getElement();
        @SuppressWarnings("unchecked")
        var styles = (CycleButton<TextArea.CursorAnimationMode>) cursor;
        styles.setValue(TextArea.CursorAnimationMode.FADE);
        this.layout();
        verify(name).setCursorAnimation(600, Easing.EASE_IN_OUT);
        this.assertCompositions(1, 0, 1, 0, 0, 0);
        this.selectTab(0);
        this.selectTextTab();
        styles.setValue(TextArea.CursorAnimationMode.INSTANT);
        this.layout();
        verify(name).setCursorAnimation(600, Easing.STEP);
        this.assertCompositions(2, 0, 2, 0, 0, 0);
    }

    @Test
    void animationTabIsAppendedAndRetainsItsModelAcrossSwitching() {
        var tabs = (Tabs) this.find(node -> node.getElement() instanceof Tabs).getElement();
        assertEquals(7, tabs.getTabCount());
        this.selectTab(6);
        var module = (GalleryAnimations) this.modules.get(6);
        assertTrue(this.tree.hasNode(module.canvas));
        module.model.timeline.pause();
        module.model.palette.immediate(0xFF123456);
        this.selectTab(0);
        assertFalse(this.tree.hasNode(module.canvas));
        this.selectTab(6);
        assertTrue(this.tree.hasNode(module.canvas));
        assertTrue(module.model.timeline.isPaused());
        assertEquals(0xFF123456, module.model.palette.value());
        this.assertCompositions(2, 0, 0, 0, 0, 0, 2);
    }

    @Test
    void debugShortcutDoesNotRecomposeModules() {
        var config = DebugOverlayConfig.get();
        boolean enabled = config.isEnabled();
        try {
            assertTrue(this.screen.keyPressed(new KeyEvent(InputConstants.KEY_F7, 0, 0)));
            assertEquals(!enabled, config.isEnabled());
            this.layout();
            this.assertCompositions(1, 0, 0, 0, 0, 0);
        } finally {
            config.setEnabled(enabled);
        }
    }

    @Test
    void regressionStructuralChangesStayInsideTheirReplayNodes() throws ReflectiveOperationException {
        this.selectTab(5);
        var field = UIRegressionGallery.class.getDeclaredField("revision");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var state = (State<Integer>) field.get(this.modules.get(5));
        for (int revision = 1; revision <= 2; revision++) {
            state.update(value -> value + 1);
            this.layout();
            var expected = "Container revision: " + revision;
            assertTrue(this.find(node -> node.getElement() instanceof Label label
                    && label.getText().getString().equals(expected)) != null);
            this.assertCompositions(1, 0, 0, 0, 0, 1);
        }
        this.selectTab(0);
        this.selectTab(5);
        state.update(value -> value + 1);
        this.layout();
        assertTrue(this.find(node -> node.getElement() instanceof Label label
                && label.getText().getString().equals("Container revision: 3")) != null);
        this.assertCompositions(2, 0, 0, 0, 0, 2);
    }

    private void assertCompositions(int... counts) {
        for (int index = 0; index < counts.length; index++) {
            verify(this.modules.get(index), times(counts[index])).compose(any());
        }
    }

    private void selectTab(int index) {
        ((Tabs) this.find(node -> node.getElement() instanceof Tabs).getElement()).setSelectedTab(index);
        this.layout();
    }

    private KeyEvent paste() {
        return new KeyEvent(InputConstants.KEY_V, InputConstants.KEYCODE_V, InputConstants.MOD_CONTROL);
    }

    private void selectTextTab() {
        ((Tabs) this.find(node -> node.getElement() instanceof Tabs).getElement()).setSelectedTab(2);
        this.layout();
    }

    private ITreeNode actionTarget() {
        var scroll = (ScrollArea) this.find(node -> node.getElement() instanceof ScrollArea area
                && area.getMaxScrollY() > 0).getElement();
        scroll.scrollTo(0, 10000);
        this.layout();
        return this.find(node -> node.getElement() instanceof Label label
                && label.getText().getString().equals("Right-click here for actions"));
    }

    private ContextMenu contextMenu() {
        return (ContextMenu) this.find(node -> node.getElement() instanceof ContextMenu).getElement();
    }

    private MouseButtonEvent click(ITreeNode node, int button) {
        var bounds = node.getElement().getBounds();
        return new MouseButtonEvent(bounds.x() + 2, bounds.y() + 2, new MouseButtonInfo(button, 0));
    }

    private void layout() {
        this.tree.prepareFrame(800, 600, this.screen.getScreenContext());
        var constraints = new Constraints(0, 800, 0, 600);
        this.tree.measureTree(constraints);
        this.tree.placeTree(new Position(0, 0), constraints);
    }

    private ITreeNode find(Predicate<ITreeNode> predicate) {
        return this.find(this.tree.getRoot(), predicate);
    }

    private ITreeNode find(ITreeNode node, Predicate<ITreeNode> predicate) {
        if (predicate.test(node)) return node;
        for (var child : node.getChildren()) {
            var found = this.find(child, predicate);
            if (found != null) return found;
        }
        return null;
    }
}
