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

package com.tridevmc.compound.ui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

import java.util.function.Consumer;

/**
 * Internal record that implements GuiElementRenderState for Compound's rendering pipeline.
 * <p>
 * This record captures all necessary state for rendering a GUI element through Vanilla's
 * GuiRenderState system, including the render pipeline, texture setup, transformation matrix,
 * scissor area, and a vertex emission lambda.
 * <p>
 * The vertex emitter lambda allows flexible vertex generation while keeping the record generic
 * enough to handle any primitive type (textured quads, gradients, etc.).
 *
 * @param pipeline    The render pipeline to use (e.g., GUI, GUI_TEXTURED)
 * @param textureSetup The texture configuration for this renderable
 * @param pose        The transformation matrix to apply to vertices
 * @param scissorArea Optional scissor rectangle to clip rendering
 * @param bounds      The screen-space bounds of this element (for culling and debug)
 * @param emitter     Lambda that emits vertices to the provided VertexConsumer
 */
public record CompoundRenderable(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds,
        Consumer<VertexConsumer> emitter
) implements GuiElementRenderState {

    @Override
    public void buildVertices(@NotNull VertexConsumer consumer) {
        emitter.accept(consumer);
    }
}
