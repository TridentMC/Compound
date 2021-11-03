/*
 * Copyright 2018 - 2021 TridentMC
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

package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.ICompoundUI;
import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.UVData;
import com.tridevmc.compound.ui.layout.ILayout;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.resources.ResourceLocation;

public class ElementImage extends Element {

    private final ResourceLocation textureLocation;
    private final UVData min, max;
    private final EnumUILayer layer;

    public ElementImage(Rect2F dimensions, ILayout layout, ResourceLocation textureLocation, Rect2F textureDimensions, EnumUILayer layer) {
        super(dimensions, layout);
        this.textureLocation = textureLocation;
        this.min = new UVData((float) textureDimensions.getX(), (float) textureDimensions.getY());
        this.max = new UVData((float) (textureDimensions.getX() + textureDimensions.getWidth()),
                (float) (textureDimensions.getY() + textureDimensions.getHeight()));
        this.layer = layer;
    }

    @Override
    public void drawLayer(ICompoundUI ui, EnumUILayer layer) {
        if (this.layer == layer) {
            IScreenContext context = ui.getScreenContext();
            Rect2F dimensions = this.getTransformedDimensions(context);
            context.bindTexture(this.textureLocation);
            context.drawTexturedRect(dimensions, min, max);
        }
    }
}
