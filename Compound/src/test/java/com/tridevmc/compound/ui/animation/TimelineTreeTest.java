package com.tridevmc.compound.ui.animation;

import com.tridevmc.compound.test.MinecraftMockExtension;
import com.tridevmc.compound.ui.animation.api.IAnimationTimeline;
import com.tridevmc.compound.ui.element.Box;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.UITree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftMockExtension.class)
class TimelineTreeTest {
    @Test void droppedBindingFreezesAndReadditionUsesCurrentFrame() {
        var tree = new UITree();
        var clock = IAnimationTimeline.create();
        var context = mock(IScreenContext.class);
        boolean[] enabled = {true};
        ICompositionScope.root(tree).e(new Box(), scope -> {
            if (enabled[0]) {
                scope.useAnimationTimeline(clock);
                scope.useAnimationTimeline(clock);
            }
        });
        when(context.frameNanos()).thenReturn(0L);
        tree.prepareFrame(100, 100, context);
        when(context.frameNanos()).thenReturn(500_000_000L);
        tree.prepareFrame(100, 100, context);
        assertEquals(.5, clock.elapsedSeconds());
        enabled[0] = false;
        tree.requestRecompose(tree.getRoot());
        when(context.frameNanos()).thenReturn(1_000_000_000L);
        tree.prepareFrame(100, 100, context);
        when(context.frameNanos()).thenReturn(10_000_000_000L);
        tree.prepareFrame(100, 100, context);
        assertEquals(1, clock.elapsedSeconds());
        enabled[0] = true;
        tree.requestRecompose(tree.getRoot());
        when(context.frameNanos()).thenReturn(20_000_000_000L);
        tree.prepareFrame(100, 100, context);
        assertEquals(1, clock.elapsedSeconds());
        when(context.frameNanos()).thenReturn(20_500_000_000L);
        tree.prepareFrame(100, 100, context);
        assertEquals(1.5, clock.elapsedSeconds());
    }

    @Test void otherTreeCannotOwnMountedClockAndDetachReleasesOwnership() {
        var clock = IAnimationTimeline.create();
        var first = new UITree();
        var second = new UITree();
        ICompositionScope.root(first).e(new Box(), scope -> scope.useAnimationTimeline(clock));
        assertThrows(IllegalStateException.class, () -> ICompositionScope.root(second)
                .e(new Box(), scope -> scope.useAnimationTimeline(clock)));
        first.reset();
        second.reset();
        ICompositionScope.root(second).e(new Box(), scope -> scope.useAnimationTimeline(clock));
        second.reset();
    }
}
