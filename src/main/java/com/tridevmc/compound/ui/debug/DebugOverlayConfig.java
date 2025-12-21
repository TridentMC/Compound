/*
 * Copyright 2018 - 2024 TridentMC
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
    private static final int COLOR_CONTENT = 0x4064B5F6;    // Light blue tint
    private static final int COLOR_PADDING = 0x4066BB6A;    // Green tint
    private static final int COLOR_MARGIN = 0x50FFA726;     // Orange tint
    private static final int COLOR_BOUNDS_OUTLINE = 0xAA2196F3; // Blue outline

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
