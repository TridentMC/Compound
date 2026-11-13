package com.tridevmc.compound.ui.integration;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.container.CompoundContainerMenu;
import com.tridevmc.compound.ui.element.InventorySlot;
import com.tridevmc.compound.ui.element.Row;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.ComposedUIContainer;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftMockExtension.class)
class NativeSlotHoverTest {
    @Test
    void changingSlotsAndLeavingNotifiesEachPreviousItemExactlyOnce() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.point(5, 5);
            assertSame(fixture.first, fixture.screen.getHoveredSlot());
            fixture.point(5, 5);
            verify(fixture.action, never()).onStopHovering(fixture.first);

            fixture.point(23, 5);
            assertSame(fixture.second, fixture.screen.getHoveredSlot());
            verify(fixture.action).onStopHovering(fixture.first);
            fixture.point(100, 100);
            fixture.point(100, 100);
            assertNull(fixture.screen.getHoveredSlot());
            verify(fixture.action, times(1)).onStopHovering(fixture.first);
            verify(fixture.action, times(1)).onStopHovering(fixture.second);
        }
    }

    @Test
    void deactivatingHoveredSlotRunsNativeCleanup() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.point(5, 5);
            when(fixture.first.isActive()).thenReturn(false);
            fixture.point(5, 5);
            assertNull(fixture.screen.getHoveredSlot());
            verify(fixture.action).onStopHovering(fixture.first);
        }
    }

    @Test
    void vanillaFiltersEmptySlotsAndNonmatchingActions() throws Exception {
        try (var fixture = new Fixture()) {
            when(fixture.first.hasItem()).thenReturn(false);
            when(fixture.action.matches(fixture.second)).thenReturn(false);
            fixture.point(5, 5);
            fixture.point(23, 5);
            fixture.point(100, 100);
            verify(fixture.action, never()).onStopHovering(fixture.first);
            verify(fixture.action, never()).onStopHovering(fixture.second);
        }
    }

    private static Slot slot() {
        var slot = mock(Slot.class);
        when(slot.isActive()).thenReturn(true);
        when(slot.hasItem()).thenReturn(true);
        when(slot.getItem()).thenReturn(mock(ItemStack.class));
        return slot;
    }

    private static final class TestScreen extends ComposedUIContainer<CompoundContainerMenu> {
        TestScreen() {
            super(mock(CompoundContainerMenu.class), mock(Inventory.class), Component.literal("Hover"));
        }

        @Override protected void compose(ICompositionScope scope) { }
        void addAction(ItemSlotMouseAction action) { this.addItemSlotMouseAction(action); }
    }

    private static final class Fixture implements AutoCloseable {
        private final TestScreen screen = new TestScreen();
        private final Slot first = slot();
        private final Slot second = slot();
        private final ItemSlotMouseAction action = mock(ItemSlotMouseAction.class);
        private final UITree tree;

        Fixture() throws Exception {
            var treeField = ComposedUIContainer.class.getDeclaredField("tree");
            treeField.setAccessible(true);
            this.tree = (UITree) treeField.get(this.screen);
            when(this.screen.getMenu().getCarried()).thenReturn(ItemStack.EMPTY);
            when(this.action.matches(this.first)).thenReturn(true);
            when(this.action.matches(this.second)).thenReturn(true);
            this.screen.addAction(this.action);
            ICompositionScope.root(this.tree).e(new Row(), root -> {
                root.e(new InventorySlot(this.first));
                root.e(new InventorySlot(this.second));
            });
            this.tree.prepareFrame(400, 300, mock(IScreenContext.class));
        }

        void point(float x, float y) throws Exception {
            var mouseX = ComposedUIContainer.class.getDeclaredField("mouseX");
            var mouseY = ComposedUIContainer.class.getDeclaredField("mouseY");
            mouseX.setAccessible(true);
            mouseY.setAccessible(true);
            mouseX.setFloat(this.screen, x);
            mouseY.setFloat(this.screen, y);
            var update = ComposedUIContainer.class.getDeclaredMethod("updateSlotStates");
            update.setAccessible(true);
            update.invoke(this.screen);
        }

        @Override public void close() { this.tree.reset(); }
    }
}
