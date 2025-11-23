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

import java.util.ArrayList;
import java.util.List;

/**
 * Simple collector that maintains render elements in strict submission order.
 * This provides guaranteed composition order = render order without relying on
 * Minecraft's bounds-based layering system.
 */
public class CompositionOrderGuiRenderState {

    private final List<RenderElement> elements = new ArrayList<>();

    /**
     * Add an element to be rendered, maintaining submission order.
     */
    public void addElement(RenderElement element) {
        this.elements.add(element);
    }

    /**
     * Submit all collected elements to the original GuiRenderState in the exact order
     * they were added, ensuring perfect composition order layering.
     */
    public void flushTo(GuiRenderState original) {
        for (RenderElement element : this.elements) {
            element.submitTo(original);
        }
    }

    public void reset() {
        this.elements.clear();
    }

    /**
     * Represents a render element that can be submitted to GuiRenderState.
     * This encapsulates the different submission methods while maintaining order.
     */
    public static final class RenderElement {
        private final Type type;
        private final Object renderState;

        private RenderElement(Type type, Object renderState) {
            this.type = type;
            this.renderState = renderState;
        }

        public static RenderElement guiElement(GuiElementRenderState renderState) {
            return new RenderElement(Type.GUI_ELEMENT, renderState);
        }

        public static RenderElement item(GuiItemRenderState renderState) {
            return new RenderElement(Type.ITEM, renderState);
        }

        public static RenderElement text(GuiTextRenderState renderState) {
            return new RenderElement(Type.TEXT, renderState);
        }

        public static RenderElement pictureInPicture(PictureInPictureRenderState renderState) {
            return new RenderElement(Type.PICTURE_IN_PICTURE, renderState);
        }

        public void submitTo(GuiRenderState target) {
            switch (type) {
                case GUI_ELEMENT -> target.submitGuiElement((GuiElementRenderState) renderState);
                case ITEM -> target.submitItem((GuiItemRenderState) renderState);
                case TEXT -> target.submitText((GuiTextRenderState) renderState);
                case PICTURE_IN_PICTURE -> target.submitPicturesInPictureState((PictureInPictureRenderState) renderState);
            }
        }

        private enum Type {
            GUI_ELEMENT, ITEM, TEXT, PICTURE_IN_PICTURE
        }
    }
}