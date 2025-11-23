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

package com.tridevmc.compound.ui.compose.state;

import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;

/**
 * Simple collector for render elements that maintains composition order.
 * This is a clean, idiomatic approach that doesn't rely on inheritance or reflection.
 *
 * Usage:
 * 1. Create instance before rendering
 * 2. Collect elements during tree traversal
 * 3. Flush to original GuiRenderState to submit in correct order
 */
public class GuiRenderStateAdapter {

    private final GuiRenderState original;
    private final CompositionOrderGuiRenderState collector;
    private boolean collecting = false;

    public GuiRenderStateAdapter(GuiRenderState original) {
        this.original = original;
        this.collector = new CompositionOrderGuiRenderState();
    }

    /**
     * Start collecting render elements in composition order.
     */
    public void startCollecting() {
        this.collecting = true;
        this.collector.reset();
    }

    /**
     * Submit all collected elements to the original GuiRenderState in the exact
     * order they were collected, ensuring perfect layering.
     */
    public void flush() {
        this.collecting = false;
        this.collector.flushTo(this.original);
    }

    /**
     * Get the current GuiRenderState to use for rendering.
     * When collecting, returns a facade that collects elements.
     * When not collecting, returns the original.
     */
    public GuiRenderState getCurrent() {
        return collecting ? new GuiRenderStateFacade() : original;
    }

    /**
     * Simple facade that redirects calls to our collector during the collection phase.
     * This avoids complex inheritance hierarchies and maintains clean separation.
     */
    private class GuiRenderStateFacade extends GuiRenderState {
        @Override
        public void submitGuiElement(GuiElementRenderState renderState) {
            collector.addElement(CompositionOrderGuiRenderState.RenderElement.guiElement(renderState));
        }

        @Override
        public void submitItem(GuiItemRenderState renderState) {
            collector.addElement(CompositionOrderGuiRenderState.RenderElement.item(renderState));
        }

        @Override
        public void submitText(GuiTextRenderState renderState) {
            collector.addElement(CompositionOrderGuiRenderState.RenderElement.text(renderState));
        }

        @Override
        public void submitPicturesInPictureState(PictureInPictureRenderState renderState) {
            collector.addElement(CompositionOrderGuiRenderState.RenderElement.pictureInPicture(renderState));
        }

        // Other methods are no-ops during collection since we control layering ourselves
        @Override
        public void nextStratum() {
            // No-op - we handle layering through collection order
        }

        @Override
        public void blurBeforeThisStratum() {
            // No-op - not needed for our use case
        }

        @Override
        public void up() {
            // No-op - not needed for our use case
        }
    }
}