package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.EnumUILayer;
import com.tridevmc.compound.ui.ICompoundUI;
import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.UVData;
import com.tridevmc.compound.ui.layout.ILayout;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.util.ResourceLocation;

public class ElementImage extends Element {

    private final ResourceLocation textureLocation;
    private final UVData min, max;
    private final EnumUILayer layer;

    public ElementImage(Rect2D dimensions, ILayout layout, ResourceLocation textureLocation, Rect2D textureDimensions, EnumUILayer layer) {
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
            Rect2D dimensions = this.getTransformedDimensions(context);
            context.bindTexture(this.textureLocation);
            context.drawTexturedRect(dimensions, min, max);
        }
    }
}
