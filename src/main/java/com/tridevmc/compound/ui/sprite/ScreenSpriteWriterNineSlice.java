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
 * Implementation of {@link IScreenSpriteWriter} that draws a nine-slice sprite within the given dimensions.
 */
public class ScreenSpriteWriterNineSlice implements IScreenSpriteWriter {

    public ScreenSpriteWriterNineSlice(int leftBorder, int rightBorder, int topBorder, int bottomBorder) {
        this.leftBorder = leftBorder;
        this.rightBorder = rightBorder;
        this.topBorder = topBorder;
        this.bottomBorder = bottomBorder;
    }

    private final int leftBorder, rightBorder, topBorder, bottomBorder;

    @Override
    public void drawSprite(IScreenContext screen, IScreenSprite sprite, float x, float y, float width, float height, int zLevel) {
        // Draw all the corners first
        // Top left
        screen.drawRectUsingSprite(
                sprite,
                x, y,
                leftBorder, topBorder,
                0, 0,
                leftBorder, topBorder,
                zLevel
        );

        // Top right
        screen.drawRectUsingSprite(
                sprite,
                x + width - rightBorder, y,
                rightBorder, topBorder,
                sprite.getWidthInPixels() - rightBorder, 0,
                sprite.getWidthInPixels(), topBorder,
                zLevel
        );

        // Bottom left
        screen.drawRectUsingSprite(
                sprite,
                x, y + height - bottomBorder,
                leftBorder, bottomBorder,
                0, sprite.getHeightInPixels() - bottomBorder,
                leftBorder, sprite.getHeightInPixels(),
                zLevel
        );

        // Bottom right
        screen.drawRectUsingSprite(
                sprite,
                x + width - rightBorder, y + height - bottomBorder,
                rightBorder, bottomBorder,
                sprite.getWidthInPixels() - rightBorder, sprite.getHeightInPixels() - bottomBorder,
                sprite.getWidthInPixels(), sprite.getHeightInPixels(),
                zLevel
        );

        // Draw the top and bottom edges
        // Top
        var maxSegmentWidth = sprite.getWidthInPixels() - leftBorder - rightBorder;
        var effectiveWidth = width - leftBorder - rightBorder;
        var segmentCount = (int) Math.ceil(effectiveWidth / maxSegmentWidth);
        for (int i = 0; i < segmentCount; i++) {
            var segmentWidth = Math.min(effectiveWidth - (i * maxSegmentWidth), maxSegmentWidth);
            screen.drawRectUsingSprite(
                    sprite,
                    x + leftBorder + (i * maxSegmentWidth), y,
                    segmentWidth, topBorder,
                    leftBorder, 0,
                    leftBorder + segmentWidth, topBorder,
                    zLevel
            );
        }

        // Bottom
        for (int i = 0; i < segmentCount; i++) {
            var segmentWidth = Math.min(effectiveWidth - (i * maxSegmentWidth), maxSegmentWidth);
            screen.drawRectUsingSprite(
                    sprite,
                    x + leftBorder + (i * maxSegmentWidth), y + height - bottomBorder,
                    segmentWidth, bottomBorder,
                    leftBorder, sprite.getHeightInPixels() - bottomBorder,
                    leftBorder + segmentWidth, sprite.getHeightInPixels(),
                    zLevel
            );
        }

        // Draw the left and right edges
        // Left
        var maxSegmentHeight = sprite.getHeightInPixels() - topBorder - bottomBorder;
        var effectiveHeight = height - topBorder - bottomBorder;
        var segmentHeightCount = (int) Math.ceil(effectiveHeight / maxSegmentHeight);
        for (int i = 0; i < segmentHeightCount; i++) {
            var segmentHeight = Math.min(effectiveHeight - (i * maxSegmentHeight), maxSegmentHeight);
            screen.drawRectUsingSprite(
                    sprite,
                    x, y + topBorder + (i * maxSegmentHeight),
                    leftBorder, segmentHeight,
                    0, topBorder,
                    leftBorder, topBorder + segmentHeight,
                    zLevel
            );
        }

        // Right
        for (int i = 0; i < segmentHeightCount; i++) {
            var segmentHeight = Math.min(effectiveHeight - (i * maxSegmentHeight), maxSegmentHeight);
            screen.drawRectUsingSprite(
                    sprite,
                    x + width - rightBorder, y + topBorder + (i * maxSegmentHeight),
                    rightBorder, segmentHeight,
                    sprite.getWidthInPixels() - rightBorder, topBorder,
                    sprite.getWidthInPixels(), topBorder + segmentHeight,
                    zLevel
            );
        }

        // Draw the center
        var centerWidth = width - leftBorder - rightBorder;
        var centerHeight = height - topBorder - bottomBorder;
        var horizontalSegmentCount = (int) Math.ceil(centerWidth / maxSegmentWidth);
        var verticalSegmentCount = (int) Math.ceil(centerHeight / maxSegmentHeight);
        for (int i = 0; i < horizontalSegmentCount; i++) {
            var segmentWidth = Math.min(centerWidth - (i * maxSegmentWidth), maxSegmentWidth);
            for (int j = 0; j < verticalSegmentCount; j++) {
                var segmentHeight = Math.min(centerHeight - (j * maxSegmentHeight), maxSegmentHeight);
                screen.drawRectUsingSprite(
                        sprite,
                        x + leftBorder + (i * maxSegmentWidth), y + topBorder + (j * maxSegmentHeight),
                        segmentWidth, segmentHeight,
                        leftBorder, topBorder,
                        leftBorder + segmentWidth, topBorder + segmentHeight,
                        zLevel
                );
            }
        }
    }

}
