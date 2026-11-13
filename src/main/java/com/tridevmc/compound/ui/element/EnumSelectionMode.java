package com.tridevmc.compound.ui.element;

/** Controls whether a collection accepts one selected row or multiple selected rows. */
public enum EnumSelectionMode {
    /** Each selection replaces the previous row. */
    SINGLE,
    /**
     * Control-click toggles a row; Shift-click or Shift-navigation selects an anchored range
     * in visible display order. Control+Shift adds the range to the existing selection.
     * Control-navigation moves focus without changing selection; Control+Space toggles that row.
     * Control+A replaces selection with all visible rows. Plain selection replaces other rows.
     * Control refers to the platform editing modifier, including Command on macOS.
     */
    MULTIPLE
}
