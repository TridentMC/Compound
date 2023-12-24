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

package com.tridevmc.compound.ui.sprite;

import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * Implementation of {@link IScreenSpriteWriter} that stretches the sprite to fit the given dimensions.
 */
public class ScreenSpriteWriterStretch implements IScreenSpriteWriter {

    public static final ScreenSpriteWriterStretch INSTANCE = new ScreenSpriteWriterStretch();

    @Override
    public void drawSprite(IScreenContext screen, IScreenSprite sprite, float x, float y, float width, float height, int zLevel) {
        screen.drawRectUsingSprite(
                sprite,
                x, y,
                width, height,
                sprite.getMinU(), sprite.getMinV(),
                sprite.getMaxU(), sprite.getMaxV(),
                zLevel
        );
    }
}
