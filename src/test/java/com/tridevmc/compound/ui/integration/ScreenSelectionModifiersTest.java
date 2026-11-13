package com.tridevmc.compound.ui.integration;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.container.CompoundContainerMenu;
import com.tridevmc.compound.ui.element.EnumSelectionMode;
import com.tridevmc.compound.ui.element.ListView;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.ComposedUI;
import com.tridevmc.compound.ui.screen.ComposedUIContainer;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MinecraftMockExtension.class)
class ScreenSelectionModifiersTest {
    @Test
    void ordinaryScreenUsesClickModifiersForToggleAndAdditiveRanges() throws Exception {
        var screen = new ComposedUI() {
            @Override
            protected void compose(ICompositionScope scope) {
            }
        };
        assertGestures(screen, ComposedUI.class);
    }

    @Test
    void containerScreenUsesClickModifiersForToggleAndAdditiveRanges() throws Exception {
        var screen = new ComposedUIContainer<>(mock(CompoundContainerMenu.class),
                mock(Inventory.class), Component.empty()) {
            @Override
            protected void compose(ICompositionScope scope) {
            }
        };
        assertGestures(screen, ComposedUIContainer.class);
    }

    private static void assertGestures(Screen screen, Class<?> owner) throws Exception {
        var field = owner.getDeclaredField("tree");
        field.setAccessible(true);
        var tree = (UITree) field.get(screen);
        var list = new ListView<Integer>();
        list.setItems(IntStream.range(0, 8).boxed().toList());
        list.setSelectionMode(EnumSelectionMode.MULTIPLE);
        ICompositionScope.root(tree).e(list, scope -> scope.layout().fixedSize(128, 128));
        tree.prepareFrame(128, 128, mock(IScreenContext.class));
        try {
            // Native event modifiers remain correct even after the live keyboard state changes.
            assertFalse(Minecraft.getInstance().hasControlDown());
            click(screen, 0, 0);
            click(screen, 2, InputConstants.MOD_CONTROL);
            assertEquals(List.of(0, 2), list.getSelectedIndices());
            click(screen, 4, InputConstants.MOD_CONTROL | InputConstants.MOD_SHIFT);
            assertEquals(List.of(0, 2, 3, 4), list.getSelectedIndices());
            click(screen, 2, InputConstants.MOD_CONTROL);
            assertEquals(List.of(0, 3, 4), list.getSelectedIndices());
            click(screen, 6, InputConstants.MOD_SHIFT);
            assertEquals(List.of(2, 3, 4, 5, 6), list.getSelectedIndices());
        } finally {
            tree.reset();
        }
    }

    private static void click(Screen screen, int row, int modifiers) {
        var event = new MouseButtonEvent(5, row * 16 + 5,
                new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, modifiers));
        assertTrue(screen.mouseClicked(event, false));
    }
}
