package com.tridevmc.compound.ui.integration;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.container.CompoundContainerMenu;
import com.tridevmc.compound.ui.element.IComposableElement;
import com.tridevmc.compound.ui.element.InventorySlot;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.Row;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.element.Surface;
import com.tridevmc.compound.ui.element.TextArea;
import com.tridevmc.compound.ui.element.TextInput;
import com.tridevmc.compound.ui.geometry.api.ITransform2D;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.ComposedUIContainer;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftMockExtension.class)
class ContainerInputRegressionTest {
    @Test
    void singleLineEditorRetainsTypedInventoryHotbarAndDropKeys() throws Exception {
        var editor = new TextInput();
        this.assertEditorOwnsTypedKeys(editor, editor::getValue);
    }

    @Test
    void multilineEditorRetainsTypedInventoryHotbarAndDropKeys() throws Exception {
        var editor = new TextArea();
        this.assertEditorOwnsTypedKeys(editor, editor::getValue);
    }

    private void assertEditorOwnsTypedKeys(IComposableElement editor, Supplier<String> value) throws Exception {
        try (var fixture = new Fixture(); var input = mockStatic(InputConstants.class)) {
            ICompositionScope.root(fixture.tree).e(editor, scope -> scope.layout().fixedSize(120, 40));
            fixture.prepare();
            fixture.tree.requestFocus(fixture.tree.getNodeForElement(editor));
            fixture.screen.hover(fixture.slot(0, 0, 0));
            var inventory = new KeyEvent(InputConstants.KEY_E, InputConstants.KEYCODE_E, 0);
            var number = new KeyEvent(InputConstants.KEY_1, '1', 0);
            var drop = new KeyEvent(InputConstants.KEY_Q, 'q', 0);
            var mappedInventory = mock(InputConstants.Key.class);
            var mappedNumber = mock(InputConstants.Key.class);
            var mappedDrop = mock(InputConstants.Key.class);
            input.when(() -> InputConstants.getKey(inventory)).thenReturn(mappedInventory);
            input.when(() -> InputConstants.getKey(number)).thenReturn(mappedNumber);
            input.when(() -> InputConstants.getKey(drop)).thenReturn(mappedDrop);
            when(fixture.options.keyInventory.isActiveAndMatches(mappedInventory)).thenReturn(true);
            when(fixture.options.keyHotbarSlots[0].isActiveAndMatches(mappedNumber)).thenReturn(true);
            when(fixture.options.keyDrop.matches(drop)).thenReturn(true);

            assertTrue(fixture.screen.keyPressed(inventory));
            assertTrue(fixture.screen.keyPressed(number));
            assertTrue(fixture.screen.keyPressed(drop));
            assertEquals(0, fixture.screen.closes);
            assertTrue(fixture.screen.actions.isEmpty());
            assertTrue(fixture.screen.charTyped(new CharacterEvent('e')));
            assertEquals("e", value.get());
            assertTrue(fixture.screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0)));
            assertEquals(1, fixture.screen.closes);
        }
    }

    @Test
    void inventoryShortcutStillClosesWhenNoEditorOwnsFocus() throws Exception {
        try (var fixture = new Fixture(); var input = mockStatic(InputConstants.class)) {
            var event = new KeyEvent(InputConstants.KEY_E, InputConstants.KEYCODE_E, 0);
            var mappedKey = mock(InputConstants.Key.class);
            input.when(() -> InputConstants.getKey(event)).thenReturn(mappedKey);
            when(fixture.options.keyInventory.isActiveAndMatches(mappedKey)).thenReturn(true);
            assertTrue(fixture.screen.keyPressed(event));
            assertEquals(1, fixture.screen.closes);
        }
    }

    @Test
    void customKeyboardCaptureEndsWhenItsAncestorIsHidden() throws Exception {
        try (var fixture = new Fixture(); var input = mockStatic(InputConstants.class)) {
            var visible = new AtomicBoolean(true);
            var editor = new Rect(0) {
                @Override public boolean isFocusable() { return true; }
                @Override public boolean isKeyboardInputExclusive() { return true; }
            };
            ICompositionScope.root(fixture.tree).e(new Stack() {
                @Override public boolean isVisible() { return visible.get(); }
            }, root -> {
                root.layout().fillMax();
                root.e(editor, child -> child.layout().fixedSize(20, 20));
            });
            fixture.prepare();
            fixture.tree.requestFocus(editor.getNode());
            var event = new KeyEvent(InputConstants.KEY_E, InputConstants.KEYCODE_E, 0);
            var mapped = mock(InputConstants.Key.class);
            input.when(() -> InputConstants.getKey(event)).thenReturn(mapped);
            when(fixture.options.keyInventory.isActiveAndMatches(mapped)).thenReturn(true);
            assertTrue(fixture.screen.keyPressed(event));
            assertEquals(0, fixture.screen.closes);
            visible.set(false);
            assertTrue(fixture.screen.keyPressed(event));
            assertEquals(1, fixture.screen.closes);
        }
    }

    @Test
    void translatedScaledSlotAcceptsNativeClickAndProtectsCarriedItems() throws Exception {
        try (var fixture = new Fixture()) {
            var slot = fixture.slot(0, 4, 7);
            ICompositionScope.root(fixture.tree).e(new Stack(), root -> {
                root.layout().fillMax();
                root.e(new InventorySlot(slot), scope -> {
                    scope.transform(() -> ITransform2D.of(240, 180, 0, 2, 2, 0, 0));
                });
            });
            fixture.prepare();
            assertFalse(fixture.screen.outside(250, 190));
            assertTrue(fixture.screen.mouseClicked(pointer(250, 190, InputConstants.MOUSE_BUTTON_LEFT), false));
            assertSame(slot, fixture.screen.actions.getFirst().slot());
            assertEquals(4, slot.x);
            assertEquals(7, slot.y);
        }
    }

    @Test
    void transformedBackgroundProtectsCarriedItemsOutsideVanillaImageBounds() throws Exception {
        try (var fixture = new Fixture()) {
            ICompositionScope.root(fixture.tree).e(new Stack(), root -> {
                root.layout().fillMax();
                root.e(new Surface(0), background -> {
                    background.layout().fixedSize(40, 30);
                    background.transform(() -> ITransform2D.of(300, 200, 0, 1, 1, 0, 0));
                });
            });
            fixture.prepare();
            assertFalse(fixture.screen.outside(310, 210));
            assertTrue(fixture.screen.outside(350, 240));
        }
    }

    @Test
    void leftAndRightClicksKeepSlotsWithSharedCoordinatesDistinct() throws Exception {
        for (var button : List.of(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOUSE_BUTTON_RIGHT)) {
            try (var fixture = new Fixture()) {
                var first = fixture.slot(0, 0, 0);
                var second = fixture.slot(1, 0, 0);
                fixture.row(first, second);
                assertTrue(fixture.screen.mouseClicked(pointer(23, 5, button), false));
                assertSame(second, fixture.screen.actions.getFirst().slot());
                assertEquals(1, fixture.screen.actions.getFirst().index());
                assertEquals(0, first.x);
                assertEquals(0, second.x);
            }
        }
    }

    @Test
    void inactiveAndUncomposedSlotsCannotStealNativeClicks() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.slot(0, 0, 0);
            var inactive = fixture.slot(1, 0, 0);
            when(inactive.isActive()).thenReturn(false);
            var target = fixture.slot(2, 0, 0);
            fixture.row(inactive, target);
            fixture.screen.mouseClicked(pointer(23, 5, InputConstants.MOUSE_BUTTON_LEFT), false);
            assertSame(target, fixture.screen.actions.getFirst().slot());
        }
    }

    @Test
    void nativeReleaseAndQuickCraftDragResolveTheVisualSlot() throws Exception {
        try (var fixture = new Fixture()) {
            var first = fixture.slot(0, 0, 0);
            var second = fixture.slot(1, 0, 0);
            fixture.row(first, second);
            var carried = mock(ItemStack.class);
            when(carried.getCount()).thenReturn(64);
            when(fixture.screen.getMenu().getCarried()).thenReturn(carried);
            assertTrue(fixture.screen.mouseClicked(pointer(23, 5, InputConstants.MOUSE_BUTTON_RIGHT), false));
            fixture.screen.mouseReleased(pointer(23, 5, InputConstants.MOUSE_BUTTON_RIGHT));
            assertSame(second, fixture.screen.actions.getFirst().slot());
            fixture.screen.quickCraft();
            when(second.mayPlace(carried)).thenReturn(true);
            when(fixture.screen.getMenu().canDragTo(second)).thenReturn(true);
            try (var menu = mockStatic(AbstractContainerMenu.class)) {
                menu.when(() -> AbstractContainerMenu.canItemQuickReplace(any(), any(), anyBoolean())).thenReturn(true);
                assertTrue(fixture.screen.mouseDragged(pointer(23, 5, InputConstants.MOUSE_BUTTON_LEFT), 1, 1));
            }
            assertEquals(List.of(second), List.copyOf(fixture.screen.quickCrafted()));
            assertEquals(0, first.x);
            assertEquals(0, second.x);
        }
    }

    @Test
    void nativeCoordinatesRestoreAfterNestedDispatchAndExceptions() throws Exception {
        try (var fixture = new Fixture()) {
            var first = fixture.slot(0, 12, 3);
            var second = fixture.slot(1, 12, 3);
            fixture.row(first, second);
            var nested = new AtomicBoolean();
            fixture.screen.onAction = () -> {
                if (nested.compareAndSet(false, true)) {
                    fixture.screen.mouseClicked(pointer(23, 5, InputConstants.MOUSE_BUTTON_RIGHT), false);
                }
            };
            fixture.screen.mouseClicked(pointer(5, 5, InputConstants.MOUSE_BUTTON_LEFT), false);
            assertEquals(List.of(first, second), fixture.screen.actions.stream().map(Action::slot).toList());
            assertEquals(12, first.x);
            assertEquals(3, first.y);
            assertEquals(12, second.x);
            assertEquals(3, second.y);
            fixture.screen.onAction = () -> { throw new IllegalStateException("consumer failure"); };
            assertThrows(IllegalStateException.class, () ->
                    fixture.screen.mouseClicked(pointer(23, 5, InputConstants.MOUSE_BUTTON_LEFT), false));
            assertEquals(12, first.x);
            assertEquals(3, first.y);
            assertEquals(12, second.x);
            assertEquals(3, second.y);
        }
    }

    private static MouseButtonEvent pointer(int x, int y, int button) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
    }

    private static void field(Object target, Class<?> owner, String name, Object value) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private record Action(Slot slot, int index, ContainerInput input) { }

    private static final class TestScreen extends ComposedUIContainer<CompoundContainerMenu> {
        private final List<Action> actions = new ArrayList<>();
        private Runnable onAction = () -> { };
        private int closes;

        TestScreen() {
            super(mock(CompoundContainerMenu.class), mock(Inventory.class), Component.literal("Regression"));
        }

        @Override protected void compose(ICompositionScope scope) { }
        @Override public void onClose() { this.closes++; }
        @Override protected void slotClicked(Slot slot, int index, int button, ContainerInput input) {
            this.actions.add(new Action(slot, index, input));
            this.onAction.run();
        }

        void hover(Slot slot) { this.hoveredSlot = slot; }
        void quickCraft() { this.isQuickCrafting = true; }
        Set<Slot> quickCrafted() { return this.quickCraftSlots; }
        boolean outside(int x, int y) { return this.hasClickedOutside(x, y, 0, 0); }
    }

    private static final class Fixture implements AutoCloseable {
        private final TestScreen screen = new TestScreen();
        private final UITree tree;
        private final Options previousOptions = Minecraft.getInstance().options;
        private final Options options = mock(Options.class);
        private final NonNullList<Slot> slots = NonNullList.create();

        Fixture() throws Exception {
            var treeField = ComposedUIContainer.class.getDeclaredField("tree");
            treeField.setAccessible(true);
            this.tree = (UITree) treeField.get(this.screen);
            when(Minecraft.getInstance().font.getSplitter())
                    .thenReturn(new StringSplitter((codePoint, style) -> 6F));
            field(this.screen.getMenu(), AbstractContainerMenu.class, "slots", this.slots);
            when(this.screen.getMenu().getCarried()).thenReturn(ItemStack.EMPTY);
            field(this.options, Options.class, "keyInventory", mock(KeyMapping.class));
            field(this.options, Options.class, "keyPickItem", mock(KeyMapping.class));
            field(this.options, Options.class, "keyDrop", mock(KeyMapping.class));
            field(this.options, Options.class, "keySwapOffhand", mock(KeyMapping.class));
            var hotbar = new KeyMapping[9];
            for (int index = 0; index < hotbar.length; index++) hotbar[index] = mock(KeyMapping.class);
            field(this.options, Options.class, "keyHotbarSlots", hotbar);
            field(Minecraft.getInstance(), Minecraft.class, "options", this.options);
        }

        Slot slot(int index, int x, int y) throws Exception {
            var slot = mock(Slot.class);
            when(slot.isActive()).thenReturn(true);
            when(slot.getItem()).thenReturn(ItemStack.EMPTY);
            field(slot, Slot.class, "index", index);
            field(slot, Slot.class, "x", x);
            field(slot, Slot.class, "y", y);
            this.slots.add(slot);
            return slot;
        }

        void prepare() { this.tree.prepareFrame(400, 300, mock(IScreenContext.class)); }

        void row(Slot... slots) {
            ICompositionScope.root(this.tree).e(new Row(), row -> {
                row.layout().fixedSize(slots.length * 18, 18);
                for (var slot : slots) row.e(new InventorySlot(slot));
            });
            this.prepare();
        }

        @Override public void close() throws Exception {
            this.tree.reset();
            field(Minecraft.getInstance(), Minecraft.class, "options", this.previousOptions);
        }
    }
}
