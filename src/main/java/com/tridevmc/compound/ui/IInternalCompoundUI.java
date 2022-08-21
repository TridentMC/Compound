/*
 * Copyright 2018 - 2022 TridentMC
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

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Only for internal use, exposes methods and variables to screen context.
 */
public interface IInternalCompoundUI {

    PoseStack getActiveStack();

    int getBlitOffset();

    void setBlitOffset(int blitOffset);

    int getWidth();

    int getHeight();

    double getMouseX();

    double getMouseY();

    Minecraft getMc();

    long getTicks();

    Screen asGuiScreen();

    EnumUILayer getCurrentLayer();
}
