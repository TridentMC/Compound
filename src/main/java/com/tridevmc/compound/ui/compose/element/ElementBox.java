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

package com.tridevmc.compound.ui.compose.element;

import com.tridevmc.compound.ui.compose.layout.Bounds;
import com.tridevmc.compound.ui.compose.layout.Constraints;
import com.tridevmc.compound.ui.compose.layout.Size;
import com.tridevmc.compound.ui.compose.scope.IComposableElementScope;
import com.tridevmc.compound.ui.compose.scope.ICompositionScope;
import com.tridevmc.compound.ui.compose.slot.SlotKey;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.sprite.ScreenSpriteWriterNineSlice;
import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * A composable box element that renders a nineslice background with content on top.
 * By default uses the inventory container sprite.
 */
public class ElementBox extends BaseElement implements IComposableElement {

    public static final SlotKey CONTENT_SLOT = new SlotKey("content");

    private static IScreenSprite DEFAULT_SPRITE = null;

    private Supplier<IScreenSprite> spriteSupplier;

    public ElementBox() {
        this(ElementBox::getDefaultSprite);
    }

    public ElementBox(IScreenSprite sprite) {
        this(() -> sprite);
    }

    public ElementBox(Supplier<IScreenSprite> spriteSupplier) {
        this.spriteSupplier = spriteSupplier;
    }

    private static IScreenSprite getDefaultSprite() {
        if (DEFAULT_SPRITE == null) {
            DEFAULT_SPRITE = IScreenSprite.of(
                    Minecraft.getInstance()
                            .getAtlasManager()
                            .getAtlasOrThrow(AtlasIds.GUI)
                            .getSprite(ResourceLocation.withDefaultNamespace("container/inventory")),
                    new ScreenSpriteWriterNineSlice(4, 4, 4, 4)
            );
        }
        return DEFAULT_SPRITE;
    }

    @Override
    public void compose(ICompositionScope scope) {
        IComposableElementScope<?> elementScope = (IComposableElementScope<?>) scope;

        // Stack the background sprite and content
        scope.e(new Stack(), stack -> {
            // Background nineslice sprite (ElementSprite already supports Supplier)
            // Sprite will fill whatever bounds are set during placement
            stack.e(new ElementSprite(this.spriteSupplier));

            // Content slot
            elementScope.slot(CONTENT_SLOT);
        });
    }

    @Override
    public Size measure(Constraints constraints) {
        // Check if we have a fixed size from layout properties
        var props = this.getLayoutProperties();
        if (props != null && props.getFixedWidth() != null && props.getFixedHeight() != null) {
            return new Size(props.getFixedWidth(), props.getFixedHeight());
        }

        var tree = this.getTree();
        if (tree == null) {
            return new Size(0, 0);
        }
        var node = tree.getNodeForElement(this);
        if (node == null) {
            return new Size(0, 0);
        }
        var children = node.getChildren();
        if (children.isEmpty()) {
            return new Size(0, 0);
        }
        return children.get(0).getElement().measure(constraints);
    }

    @Override
    public void place(Bounds bounds) {
        this.setBounds(bounds);

        var tree = this.getTree();
        if (tree == null) {
            return;
        }
        var node = tree.getNodeForElement(this);
        if (node == null) {
            return;
        }
        var children = node.getChildren();
        if (!children.isEmpty()) {
            com.tridevmc.compound.ui.compose.layout.LayoutHelper.placeChild(children.get(0).getElement(), bounds);
        }
    }

    public IScreenSprite getSprite() {
        return this.spriteSupplier.get();
    }

    public void setSprite(IScreenSprite sprite) {
        this.spriteSupplier = () -> sprite;
    }

    public Supplier<IScreenSprite> getSpriteSupplier() {
        return this.spriteSupplier;
    }

    public void setSpriteSupplier(Supplier<IScreenSprite> spriteSupplier) {
        this.spriteSupplier = spriteSupplier;
    }
}
