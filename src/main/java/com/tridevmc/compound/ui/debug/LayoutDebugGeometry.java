/*
 * Copyright 2018 - 2026 TridentMC
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

package com.tridevmc.compound.ui.debug;

import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.Row;
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.ITreeNode;

final class LayoutDebugGeometry {
    private LayoutDebugGeometry() {
    }

    static void drawAllocatedVsActual(IScreenContext context, ITreeNode node) {
        var allocatedBounds = node.getAllocatedBounds();
        var actualBounds = node.getBounds();

        if (allocatedBounds == null || actualBounds == null) {
            return;
        }
        if (allocatedBounds.equals(actualBounds)) {
            return;
        }

        int unusedColor = DebugOverlayConfig.COLOR_UNUSED_SPACE;
        if (actualBounds.y() > allocatedBounds.y()) {
            float gapHeight = actualBounds.y() - allocatedBounds.y();
            context.drawRect(allocatedBounds.x(), allocatedBounds.y(),
                    allocatedBounds.width(), gapHeight, unusedColor);
        }
        float actualBottom = actualBounds.y() + actualBounds.height();
        float allocBottom = allocatedBounds.y() + allocatedBounds.height();
        if (actualBottom < allocBottom) {
            float gapHeight = allocBottom - actualBottom;
            context.drawRect(allocatedBounds.x(), actualBottom,
                    allocatedBounds.width(), gapHeight, unusedColor);
        }
        if (actualBounds.x() > allocatedBounds.x()) {
            float gapWidth = actualBounds.x() - allocatedBounds.x();
            context.drawRect(allocatedBounds.x(), actualBounds.y(),
                    gapWidth, actualBounds.height(), unusedColor);
        }
        float actualRight = actualBounds.x() + actualBounds.width();
        float allocRight = allocatedBounds.x() + allocatedBounds.width();
        if (actualRight < allocRight) {
            float gapWidth = allocRight - actualRight;
            context.drawRect(actualRight, actualBounds.y(),
                    gapWidth, actualBounds.height(), unusedColor);
        }
        int allocOutlineColor = DebugOverlayConfig.COLOR_ALLOCATED_OUTLINE;
        drawDashedRect(context, allocatedBounds.x(), allocatedBounds.y(),
                allocatedBounds.width(), allocatedBounds.height(), allocOutlineColor, 4);
    }

    private static void drawDashedRect(IScreenContext context, float x, float y, float w, float h, int color, int dashLen) {
        for (float dx = 0; dx < w; dx += dashLen * 2) {
            float len = Math.min(dashLen, w - dx);
            context.drawRect(x + dx, y, len, 1, color);
        }
        for (float dx = 0; dx < w; dx += dashLen * 2) {
            float len = Math.min(dashLen, w - dx);
            context.drawRect(x + dx, y + h - 1, len, 1, color);
        }
        for (float dy = 0; dy < h; dy += dashLen * 2) {
            float len = Math.min(dashLen, h - dy);
            context.drawRect(x, y + dy, 1, len, color);
        }
        for (float dy = 0; dy < h; dy += dashLen * 2) {
            float len = Math.min(dashLen, h - dy);
            context.drawRect(x + w - 1, y + dy, 1, len, color);
        }
    }

    static void drawBoxModel(IScreenContext context, ITreeNode node, boolean isHovered) {
        var bounds = node.getBounds();
        if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) {
            return;
        }

        var props = node.getLayoutProperties();
        if (props == null) props = LayoutProperties.create();

        float x = bounds.x();
        float y = bounds.y();
        float w = bounds.width();
        float h = bounds.height();

        int marginColor = isHovered ? DebugOverlayConfig.COLOR_MARGIN : DebugOverlayConfig.COLOR_ANCESTOR_MARGIN;
        int paddingColor = isHovered ? DebugOverlayConfig.COLOR_PADDING : DebugOverlayConfig.COLOR_ANCESTOR_PADDING;
        int outlineColor = isHovered ? DebugOverlayConfig.COLOR_BOUNDS_OUTLINE : DebugOverlayConfig.COLOR_ANCESTOR_OUTLINE;
        int mL = props.getMarginLeft();
        int mT = props.getMarginTop();
        int mR = props.getMarginRight();
        int mB = props.getMarginBottom();

        if (mL > 0) context.drawRect(x - mL, y, mL, h, marginColor);
        if (mT > 0) context.drawRect(x, y - mT, w, mT, marginColor);
        if (mR > 0) context.drawRect(x + w, y, mR, h, marginColor);
        if (mB > 0) context.drawRect(x, y + h, w, mB, marginColor);
        int pL = props.getPaddingLeft();
        int pT = props.getPaddingTop();
        int pR = props.getPaddingRight();
        int pB = props.getPaddingBottom();

        if (pL > 0) context.drawRect(x, y, pL, h, paddingColor);
        if (pT > 0) context.drawRect(x + pL, y, w - pL - pR, pT, paddingColor);
        if (pR > 0) context.drawRect(x + w - pR, y, pR, h, paddingColor);
        if (pB > 0) context.drawRect(x + pL, y + h - pB, w - pL - pR, pB, paddingColor);
        context.drawRectOutline(x, y, w, h, outlineColor, 1);
        if (isHovered) {
             if (mL > 0) drawArrowLabel(context, x - mL/2f, y + h/2f, mL, true);
             if (mR > 0) drawArrowLabel(context, x + w + mR/2f, y + h/2f, mR, true);
             if (mT > 0) drawArrowLabel(context, x + w/2f, y - mT/2f, mT, false);
             if (mB > 0) drawArrowLabel(context, x + w/2f, y + h + mB/2f, mB, false);

             if (pL > 0) drawArrowLabel(context, x + pL/2f, y + h/2f, pL, true);
             if (pR > 0) drawArrowLabel(context, x + w - pR/2f, y + h/2f, pR, true);
             if (pT > 0) drawArrowLabel(context, x + w/2f, y + pT/2f, pT, false);
             if (pB > 0) drawArrowLabel(context, x + w/2f, y + h - pB/2f, pB, false);
        }
    }

    static void drawSpacing(IScreenContext context, ITreeNode child, ITreeNode parent) {
        var parentProps = parent.getLayoutProperties();
        var parentElement = parent.getElement();
        if (parentProps == null) return;

        var parentBounds = parent.getBounds();
        var childBounds = child.getBounds();
        if (parentBounds == null || childBounds == null) return;
        if (parentElement instanceof Column || parentElement instanceof Row) {
            boolean isColumn = parentElement instanceof Column;
            var siblings = parent.getChildren();
            int spacingColor = DebugOverlayConfig.COLOR_SPACING;
            int dimmedSpacingColor = 0x40E91E63;
            int childIndex = -1;
            for (int i = 0; i < siblings.size(); i++) {
                if (siblings.get(i) == child) {
                    childIndex = i;
                    break;
                }
            }
            for (int i = 0; i < siblings.size() - 1; i++) {
                var node1 = siblings.get(i);
                var node2 = siblings.get(i + 1);
                var b1 = node1.getBounds();
                var b2 = node2.getBounds();

                if (b1 != null && b2 != null) {
                    boolean isAdjacentGap = (i == childIndex - 1) || (i == childIndex);
                    int color = isAdjacentGap ? spacingColor : dimmedSpacingColor;

                    if (isColumn) {
                        float gapY = b1.y() + b1.height();
                        float gapH = b2.y() - gapY;
                        if (gapH > 0 && gapH < 100) {
                            float gapX = Math.max(b1.x(), b2.x());
                            float gapW = Math.min(b1.width(), b2.width());
                            context.drawRect(gapX, gapY, gapW, gapH, color);
                            if (isAdjacentGap && gapH >= 2) {
                                drawArrowLabel(context, gapX + gapW/2f, gapY + gapH/2f, (int)gapH, false);
                            }
                        }
                    } else {
                        float gapX = b1.x() + b1.width();
                        float gapW = b2.x() - gapX;
                        if (gapW > 0 && gapW < 100) {
                            float gapY = Math.max(b1.y(), b2.y());
                            float gapH = Math.min(b1.height(), b2.height());
                            context.drawRect(gapX, gapY, gapW, gapH, color);
                            if (isAdjacentGap && gapW >= 2) {
                                drawArrowLabel(context, gapX + gapW/2f, gapY + gapH/2f, (int)gapW, true);
                            }
                        }
                    }
                }
            }
        }
    }

    static void drawAlignmentDrivers(IScreenContext context, ITreeNode child, ITreeNode parent) {
        var parentProps = parent.getLayoutProperties();
        if (parentProps == null) return;

        var parentBounds = parent.getBounds();
        var childBounds = child.getBounds();
        if (parentBounds == null || childBounds == null) return;
        Alignment align = parentProps.getContentAlignment();

        if (align != null) {
            int springColor = DebugOverlayConfig.COLOR_ALIGNMENT;
            float px = parentBounds.x() + parentProps.getPaddingLeft();
            float py = parentBounds.y() + parentProps.getPaddingTop();
            float pw = parentBounds.width() - parentProps.getPaddingLeft() - parentProps.getPaddingRight();
            float ph = parentBounds.height() - parentProps.getPaddingTop() - parentProps.getPaddingBottom();

            float cx = childBounds.x();
            float cy = childBounds.y();
            float cw = childBounds.width();
            float ch = childBounds.height();
            if (align == Alignment.CENTER || align == Alignment.CENTER_LEFT || align == Alignment.CENTER_RIGHT ||
                align == Alignment.TOP_CENTER || align == Alignment.BOTTOM_CENTER) {
                if (cx > px) {
                    float w = cx - px;
                    context.drawRect(px, cy, w, ch, springColor);
                    context.drawRect(px, cy + ch/2f, w, 1, 0xFFFFFF00);
                }
                if (cx + cw < px + pw) {
                    float w = (px + pw) - (cx + cw);
                    context.drawRect(cx + cw, cy, w, ch, springColor);
                    context.drawRect(cx + cw, cy + ch/2f, w, 1, 0xFFFFFF00);
                }
            }
            if (align == Alignment.CENTER || align == Alignment.TOP_CENTER || align == Alignment.BOTTOM_CENTER ||
                align == Alignment.CENTER_LEFT || align == Alignment.CENTER_RIGHT) {
                if (cy > py) {
                    float h = cy - py;
                    context.drawRect(cx, py, cw, h, springColor);
                    context.drawRect(cx + cw/2f, py, 1, h, 0xFFFFFF00);
                }
                if (cy + ch < py + ph) {
                    float h = (py + ph) - (cy + ch);
                    context.drawRect(cx, cy + ch, cw, h, springColor);
                    context.drawRect(cx + cw/2f, cy + ch, 1, h, 0xFFFFFF00);
                }
            }
        }
    }

    private static void drawArrowLabel(IScreenContext context, float centerX, float centerY,
                                 int value, boolean horizontal) {
        if (!DebugOverlayConfig.get().shouldShowDimensions()) return;
        String text = String.valueOf(value);
        int textWidth = context.getFont().width(text);
        int textHeight = 7;

        float textX = centerX - textWidth / 2f;
        float textY = centerY - textHeight / 2f;
        context.drawRect(textX - 1, textY - 1, textWidth + 2, textHeight + 2, 0xAA000000);
        int arrowColor = 0xFFFFFFFF;
        if (horizontal) {
            float arrowLen = Math.max(4, (value - textWidth) / 2f - 4);
            if (arrowLen > 2) {
                context.drawRect(centerX - textWidth / 2f - arrowLen, centerY - 0.5f, arrowLen - 2, 1, arrowColor);
                context.drawRect(centerX + textWidth / 2f + 2, centerY - 0.5f, arrowLen - 2, 1, arrowColor);
            }
        } else {
            float arrowLen = Math.max(4, (value - textHeight) / 2f - 4);
            if (arrowLen > 2) {
                context.drawRect(centerX - 0.5f, centerY - textHeight / 2f - arrowLen, 1, arrowLen - 2, arrowColor);
                context.drawRect(centerX - 0.5f, centerY + textHeight / 2f + 2, 1, arrowLen - 2, arrowColor);
            }
        }
        context.drawStringWithShadow(text, textX, textY, 0xFFFFFF);
    }
}
