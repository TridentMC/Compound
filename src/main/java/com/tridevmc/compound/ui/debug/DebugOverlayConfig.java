/*
 * Copyright 2018 - 2026 TridentMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridevmc.compound.ui.debug;

/**
 * Configuration for the debug overlay visualization.
 * Provides browser DevTools-style visualization of element bounds, padding, and margins.
 *
 * <p>This feature is entirely handled by the framework - individual elements do not need
 * to implement any debug rendering logic.</p>
 */
public class DebugOverlayConfig {

    private static final DebugOverlayConfig INSTANCE = new DebugOverlayConfig();

    // Semi-transparent colors inspired by browser DevTools
    // CSS Box Model Colors
    public static final int COLOR_CONTENT = 0x802196F3; // Blue
    public static final int COLOR_PADDING = 0x804CAF50; // Green
    public static final int COLOR_MARGIN = 0x80FF9800;  // Orange
    public static final int COLOR_SPACING = 0x80E91E63; // Pink
    public static final int COLOR_ALIGNMENT = 0x60FFEB3B; // Yellow (Transparent)
    
    // Dimmed colors for ancestors
    public static final int COLOR_ANCESTOR_CONTENT = 0x202196F3;
    public static final int COLOR_ANCESTOR_PADDING = 0x204CAF50;
    public static final int COLOR_ANCESTOR_MARGIN = 0x20FF9800;
    
    // Outlines
    public static final int COLOR_BOUNDS_OUTLINE = 0xFF2196F3; // Blue
    public static final int COLOR_ANCESTOR_OUTLINE = 0x409E9E9E; // Gray

    // Badges/Info
    public static final int COLOR_INFO_BACKGROUND = 0xE0000000;
    public static final int COLOR_INFO_TEXT = 0xFFFFFFFF;
    
    // Layout Type Badge Backgrounds (Legacy/Fallback)
    public static final int COLOR_BADGE_COLUMN = 0xFF4CAF50;
    public static final int COLOR_BADGE_ROW = 0xFF2196F3;
    public static final int COLOR_BADGE_BOX = 0xFF9C27B0;
    public static final int COLOR_BADGE_STACK = 0xFFFF9800;
    public static final int COLOR_BADGE_GRID = 0xFFE91E63;
    public static final int COLOR_BADGE_PANEL = 0xFF607D8B;
    public static final int COLOR_BADGE_SCROLL = 0xFF795548;
    public static final int COLOR_BADGE_DEFAULT = 0xFF9E9E9E; // Blue-gray

    // Layout info text colors
    public static final int COLOR_ALIGNMENT_TEXT = 0xFFFFEB3B;   // Yellow for alignment
    public static final int COLOR_SPACING_TEXT = 0xFF81C784;     // Light green for spacing
    public static final int COLOR_SIZE_CONSTRAINT = 0xFFCE93D8;  // Light purple for size constraints

    // Allocated bounds visualization (gap between parent allocation and actual element size)
    public static final int COLOR_ALLOCATED_OUTLINE = 0x80FFFFFF; // White dashed outline for parent allocation
    public static final int COLOR_UNUSED_SPACE = 0x50FFFF00;      // Yellow for unused allocation space

    private boolean enabled = false;
    private boolean showDimensions = true;

    private DebugOverlayConfig() {
    }

    /**
     * Gets the singleton configuration instance.
     *
     * @return the debug overlay configuration
     */
    public static DebugOverlayConfig get() {
        return INSTANCE;
    }

    /**
     * Checks if debug overlay is enabled.
     *
     * @return true if debug overlay should be rendered
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Sets whether debug overlay is enabled.
     *
     * @param enabled true to enable debug overlay
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Toggles debug overlay on/off.
     *
     * @return the new enabled state after toggling
     */
    public boolean toggle() {
        this.enabled = !this.enabled;
        return this.enabled;
    }

    /**
     * Checks if dimension labels should be shown on hovered elements.
     *
     * @return true if dimensions should be displayed
     */
    public boolean shouldShowDimensions() {
        return this.showDimensions;
    }

    /**
     * Sets whether dimension labels should be shown.
     *
     * @param showDimensions true to show dimension labels
     */
    public void setShowDimensions(boolean showDimensions) {
        this.showDimensions = showDimensions;
    }

    /**
     * Gets the color for content area visualization.
     *
     * @return ARGB color for content area
     */
    public int getContentColor() {
        return COLOR_CONTENT;
    }

    /**
     * Gets the color for padding area visualization.
     *
     * @return ARGB color for padding area
     */
    public int getPaddingColor() {
        return COLOR_PADDING;
    }

    /**
     * Gets the color for margin area visualization.
     *
     * @return ARGB color for margin area
     */
    public int getMarginColor() {
        return COLOR_MARGIN;
    }

    /**
     * Gets the color for bounds outline.
     *
     * @return ARGB color for bounds outline
     */
    public int getBoundsOutlineColor() {
        return COLOR_BOUNDS_OUTLINE;
    }
}
