package com.tridevmc.compound.ui.screen;

import com.tridevmc.compound.ui.IInternalCompoundUI;

public final class ScreenContextFixture {
    public static IScreenContext create(IInternalCompoundUI ui) {
        return new CompoundScreenContext(ui);
    }
}
