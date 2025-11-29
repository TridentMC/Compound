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

package com.tridevmc.compound.ui;

import com.mojang.blaze3d.platform.cursor.CursorType;
import org.lwjgl.glfw.GLFW;

/**
 * Holds standard cursor types for use in the UI.
 */
public class CompoundCursors {
    public static final CursorType HAND = CursorType.createStandardCursor(GLFW.GLFW_HAND_CURSOR, "hand", CursorType.DEFAULT);
    public static final CursorType IBEAM = CursorType.createStandardCursor(GLFW.GLFW_IBEAM_CURSOR, "ibeam", CursorType.DEFAULT);
    public static final CursorType CROSSHAIR = CursorType.createStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR, "crosshair", CursorType.DEFAULT);
    public static final CursorType HRESIZE = CursorType.createStandardCursor(GLFW.GLFW_HRESIZE_CURSOR, "hresize", CursorType.DEFAULT);
    public static final CursorType VRESIZE = CursorType.createStandardCursor(GLFW.GLFW_VRESIZE_CURSOR, "vresize", CursorType.DEFAULT);
}
