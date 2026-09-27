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

package com.tridevmc.compound.ui.debug;

import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.Row;
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.ITreeNode;

import java.util.ArrayList;
import java.util.List;

/** Draws the optional layout inspector without participating in tree layout or input. */
public final class LayoutDebugRenderer {
    private record DebugLine(String text, int colour) {}

    /**
     * Renders debug overlay showing bounds, margins, and padding for the hovered element
     * and its ancestors. Includes layout driver visualization (alignment springs, spacing bars).
     *
     * @param context the screen context for drawing
     * @param hoveredNode the element whose layout is inspected
     */
    public void render(IScreenContext context, ITreeNode hoveredNode) {
        if (hoveredNode == null) {
            return;
        }
        var ancestorChain = new ArrayList<ITreeNode>();
        ITreeNode current = hoveredNode;
        while (current != null) {
            ancestorChain.add(0, current); // Add at beginning to get root-to-node order
            current = current.getParent();
        }
        for (int i = 0; i < ancestorChain.size() - 1; i++) {
            ITreeNode node = ancestorChain.get(i);
            drawBoxModel(context, node, false);
        }
        drawAllocatedVsActual(context, hoveredNode);
        drawBoxModel(context, hoveredNode, true);
        for (int i = 0; i < ancestorChain.size() - 1; i++) {
            ITreeNode ancestor = ancestorChain.get(i);
            ITreeNode childInAncestor = ancestorChain.get(i + 1);
            if (ancestor.getElement() instanceof Column || ancestor.getElement() instanceof Row) {
                drawLayoutDrivers(context, childInAncestor, ancestor, childInAncestor == hoveredNode);
            }
            var ancestorProps = ancestor.getLayoutProperties();
            if (ancestorProps != null && ancestorProps.getContentAlignment() != null) {
                drawAlignmentDrivers(context, childInAncestor, ancestor);
            }
        }
        drawDebugInfoPanel(context, hoveredNode, ancestorChain);
    }

    private void drawAllocatedVsActual(IScreenContext context, ITreeNode node) {
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

    private void drawDashedRect(IScreenContext context, float x, float y, float w, float h, int color, int dashLen) {
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

    private void drawBoxModel(IScreenContext context, ITreeNode node, boolean isHovered) {
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
             if (mL > 0) drawArrowLabel(context, x - mL/2f, y + h/2f, mL, true, marginColor);
             if (mR > 0) drawArrowLabel(context, x + w + mR/2f, y + h/2f, mR, true, marginColor);
             if (mT > 0) drawArrowLabel(context, x + w/2f, y - mT/2f, mT, false, marginColor);
             if (mB > 0) drawArrowLabel(context, x + w/2f, y + h + mB/2f, mB, false, marginColor);

             if (pL > 0) drawArrowLabel(context, x + pL/2f, y + h/2f, pL, true, paddingColor);
             if (pR > 0) drawArrowLabel(context, x + w - pR/2f, y + h/2f, pR, true, paddingColor);
             if (pT > 0) drawArrowLabel(context, x + w/2f, y + pT/2f, pT, false, paddingColor);
             if (pB > 0) drawArrowLabel(context, x + w/2f, y + h - pB/2f, pB, false, paddingColor);
        }
    }

    private void drawLayoutDrivers(IScreenContext context, ITreeNode child, ITreeNode parent, boolean isDirectChild) {
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
            int dimmedSpacingColor = 0x40E91E63; // Dimmed pink for non-adjacent
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
                                drawArrowLabel(context, gapX + gapW/2f, gapY + gapH/2f, (int)gapH, false, color);
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
                                drawArrowLabel(context, gapX + gapW/2f, gapY + gapH/2f, (int)gapW, true, color);
                            }
                        }
                    }
                }
            }
        }
    }

    private void drawAlignmentDrivers(IScreenContext context, ITreeNode child, ITreeNode parent) {
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

    private void drawDebugInfoPanel(IScreenContext context, ITreeNode node, List<ITreeNode> ancestorChain) {
        if (!DebugOverlayConfig.get().shouldShowDimensions()) return;
        var element = node.getElement();
        var bounds = node.getBounds();
        var allocatedBounds = node.getAllocatedBounds();
        var measuredSize = node.getMeasuredSize();
        var parent = node.getParent();
        var props = node.getLayoutProperties();
        if (props == null) props = LayoutProperties.create();

        var lines = new ArrayList<DebugLine>();
        String title = element.getClass().getSimpleName() + " (" + (int)bounds.width() + "×" + (int)bounds.height() + ")";
        lines.add(new DebugLine(title, 0xFFFFFFFF));
        if (ancestorChain.size() > 1) {
            var breadcrumb = new StringBuilder();
            int start = Math.max(0, ancestorChain.size() - 4);
            for (int i = start; i < ancestorChain.size(); i++) {
                if (i > start) breadcrumb.append(" > ");
                breadcrumb.append(ancestorChain.get(i).getElement().getClass().getSimpleName());
            }
            lines.add(new DebugLine(breadcrumb.toString(), 0xFF888888));
        }

        lines.add(new DebugLine("", 0xFF888888));
        lines.add(new DebugLine("BOUNDS ANALYSIS", 0xFF4FC3F7)); // Light blue header

        lines.add(new DebugLine("  Position: (" + (int)bounds.x() + ", " + (int)bounds.y() + ")", 0xFFCCCCCC));

        if (measuredSize != null) {
            lines.add(new DebugLine("  Measured: " + measuredSize.width() + "×" + measuredSize.height(), 0xFFCCCCCC));
        }
        if (allocatedBounds != null && !allocatedBounds.equals(bounds)) {
            lines.add(new DebugLine("  Allocated: " + (int)allocatedBounds.width() + "×" + (int)allocatedBounds.height(), 0xFFFFFF00)); // Yellow - this is the key info!
            int gapTop = bounds.y() - allocatedBounds.y();
            int gapBottom = (allocatedBounds.y() + allocatedBounds.height()) - (bounds.y() + bounds.height());
            int gapLeft = bounds.x() - allocatedBounds.x();
            int gapRight = (allocatedBounds.x() + allocatedBounds.width()) - (bounds.x() + bounds.width());

            if (gapTop > 0 || gapBottom > 0) {
                lines.add(new DebugLine("  Gap V: ↑" + gapTop + "px ↓" + gapBottom + "px", 0xFFFFFF00));
            }
            if (gapLeft > 0 || gapRight > 0) {
                lines.add(new DebugLine("  Gap H: ←" + gapLeft + "px →" + gapRight + "px", 0xFFFFFF00));
            }
        }
        if (parent != null) {
            lines.add(new DebugLine("", 0xFF888888));

            lines.add(new DebugLine("PARENT LAYOUT (" + parent.getElement().getClass().getSimpleName() + ")", 0xFF81C784)); // Light green header

            var pProps = parent.getLayoutProperties();
            if (pProps != null) {
                Alignment align = pProps.getContentAlignment();
                if (align == null && parent.getElement() instanceof Column) align = pProps.getHorizontalAlignment();
                if (align == null && parent.getElement() instanceof Row) align = pProps.getVerticalAlignment();

                if (align != null) {
                    lines.add(new DebugLine("  align: " + align.name(), 0xFFFFEB3B)); // Yellow
                }

                if (pProps.getSpacing() > 0) {
                    lines.add(new DebugLine("  spacing: " + pProps.getSpacing() + "px", 0xFFFF69B4)); // Pink
                }

                int pPad = pProps.getPaddingLeft() + pProps.getPaddingRight() +
                           pProps.getPaddingTop() + pProps.getPaddingBottom();
                if (pPad > 0) {
                    lines.add(new DebugLine("  padding: " + pProps.getPaddingTop() + " " + pProps.getPaddingRight() +
                             " " + pProps.getPaddingBottom() + " " + pProps.getPaddingLeft(), 0xFF4CAF50)); // Green
                }
            }
        }
        for (int i = 0; i < ancestorChain.size() - 1; i++) {
            ITreeNode ancestor = ancestorChain.get(i);
            ITreeNode childInAncestor = ancestorChain.get(i + 1);

            if (ancestor.getElement() instanceof Column || ancestor.getElement() instanceof Row) {
                boolean isColumn = ancestor.getElement() instanceof Column;
                var siblings = ancestor.getChildren();
                int childIndex = -1;

                for (int j = 0; j < siblings.size(); j++) {
                    if (siblings.get(j) == childInAncestor) {
                        childIndex = j;
                        break;
                    }
                }

                if (childIndex != -1 && siblings.size() > 1) {
                    lines.add(new DebugLine("", 0xFF888888));

                    String containerName = ancestor.getElement().getClass().getSimpleName();
                    var ancestorProps = ancestor.getLayoutProperties();
                    int spacing = ancestorProps != null ? ancestorProps.getSpacing() : 0;

                    lines.add(new DebugLine("LAYOUT IN " + containerName + " (spacing=" + spacing + ")", 0xFFFF69B4)); // Pink header
                    if (childIndex > 0) {
                        var prevNode = siblings.get(childIndex - 1);
                        var prevBounds = prevNode.getBounds();
                        var prevElement = prevNode.getElement();
                        var childBounds = childInAncestor.getBounds();

                        String prevName = prevElement.getClass().getSimpleName();
                        if (prevBounds != null) {
                            prevName += " (" + (int)prevBounds.width() + "×" + (int)prevBounds.height() + ")";
                        }
                        lines.add(new DebugLine("  prev: " + prevName, 0xFFCCCCCC));
                        if (prevBounds != null && childBounds != null) {
                            int gap = isColumn ?
                                childBounds.y() - (prevBounds.y() + prevBounds.height()) :
                                childBounds.x() - (prevBounds.x() + prevBounds.width());
                            if (gap > 0) {
                                lines.add(new DebugLine("  ↕ gap above: " + gap + "px", 0xFFFF69B4)); // Pink
                            }
                        }
                    }
                    var childBounds = childInAncestor.getBounds();
                    String childName = childInAncestor.getElement().getClass().getSimpleName();
                    if (childBounds != null) {
                        childName += " (" + (int)childBounds.width() + "×" + (int)childBounds.height() + ")";
                    }
                    lines.add(new DebugLine("  → this: " + childName, 0xFFFFFFFF));
                    if (childIndex < siblings.size() - 1) {
                        var nextNode = siblings.get(childIndex + 1);
                        var nextBounds = nextNode.getBounds();
                        var nextElement = nextNode.getElement();
                        if (nextBounds != null && childBounds != null) {
                            int gap = isColumn ?
                                nextBounds.y() - (childBounds.y() + childBounds.height()) :
                                nextBounds.x() - (childBounds.x() + childBounds.width());
                            if (gap > 0) {
                                lines.add(new DebugLine("  ↕ gap below: " + gap + "px", 0xFFFF69B4));
                            }
                        }

                        String nextName = nextElement.getClass().getSimpleName();
                        if (nextBounds != null) {
                            nextName += " (" + (int)nextBounds.width() + "×" + (int)nextBounds.height() + ")";
                        }
                        lines.add(new DebugLine("  next: " + nextName, 0xFFCCCCCC));
                    }
                    break;
                }
            }
        }
        for (int i = ancestorChain.size() - 2; i >= 0; i--) {
            ITreeNode ancestor = ancestorChain.get(i);
            ITreeNode childInAncestor = ancestorChain.get(i + 1);

            var ancestorProps = ancestor.getLayoutProperties();
            if (ancestorProps != null && ancestorProps.getContentAlignment() != null) {
                Alignment align = ancestorProps.getContentAlignment();
                var ancestorBounds = ancestor.getBounds();
                var childBounds = childInAncestor.getBounds();

                lines.add(new DebugLine("", 0xFF888888));

                String ancestorName = ancestor.getElement().getClass().getSimpleName();
                lines.add(new DebugLine("CENTERING (" + ancestorName + ")", 0xFFFFEB3B)); // Yellow header

                lines.add(new DebugLine("  contentAlignment: " + align.name(), 0xFFFFEB3B));

                if (ancestorBounds != null && childBounds != null) {
                    float px = ancestorBounds.x() + ancestorProps.getPaddingLeft();
                    float py = ancestorBounds.y() + ancestorProps.getPaddingTop();
                    float pw = ancestorBounds.width() - ancestorProps.getPaddingLeft() - ancestorProps.getPaddingRight();
                    float ph = ancestorBounds.height() - ancestorProps.getPaddingTop() - ancestorProps.getPaddingBottom();

                    int leftGap = (int)(childBounds.x() - px);
                    int rightGap = (int)((px + pw) - (childBounds.x() + childBounds.width()));
                    int topGap = (int)(childBounds.y() - py);
                    int bottomGap = (int)((py + ph) - (childBounds.y() + childBounds.height()));

                    if (leftGap > 0 || rightGap > 0) {
                        lines.add(new DebugLine("  H gaps: ←" + leftGap + "px  →" + rightGap + "px", 0xFFFFEB3B));
                    }
                    if (topGap > 0 || bottomGap > 0) {
                        lines.add(new DebugLine("  V gaps: ↑" + topGap + "px  ↓" + bottomGap + "px", 0xFFFFEB3B));
                    }
                }
                break;
            }
        }
        int mTot = props.getMarginLeft() + props.getMarginRight() + props.getMarginTop() + props.getMarginBottom();
        int pTot = props.getPaddingLeft() + props.getPaddingRight() + props.getPaddingTop() + props.getPaddingBottom();

        if (mTot > 0 || pTot > 0 || props.isFillMaxWidth() || props.isFillMaxHeight()) {
            lines.add(new DebugLine("", 0xFF888888));

            lines.add(new DebugLine("THIS ELEMENT", 0xFFFFAB40)); // Orange header

            if (mTot > 0) {
                lines.add(new DebugLine("  margin: " + props.getMarginTop() + " " + props.getMarginRight() +
                         " " + props.getMarginBottom() + " " + props.getMarginLeft(), 0xFFFFA500)); // Orange
            }

            if (pTot > 0) {
                lines.add(new DebugLine("  padding: " + props.getPaddingTop() + " " + props.getPaddingRight() +
                         " " + props.getPaddingBottom() + " " + props.getPaddingLeft(), 0xFF4CAF50)); // Green
            }

            if (props.isFillMaxWidth() || props.isFillMaxHeight()) {
                String fill = "";
                if (props.isFillMaxWidth() && props.isFillMaxHeight()) fill = "fillMax";
                else if (props.isFillMaxWidth()) fill = "fillMaxWidth";
                else fill = "fillMaxHeight";
                lines.add(new DebugLine("  " + fill, 0xFFCE93D8)); // Purple
            }
        }
        int lineHeight = 10;
        int panelPadding = 6;
        float panelW = 220;
        float panelH = lines.size() * lineHeight + panelPadding * 2;
        float px = context.getWidth() - panelW - 5;
        float py = 5;
        context.drawRect(px, py, panelW, panelH, 0xE8000000);
        context.drawRectOutline(px, py, panelW, panelH, 0xFF333333, 1);
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            context.drawString(line.text(), px + panelPadding, py + panelPadding + i * lineHeight, line.colour());
        }
    }

    private void drawArrowLabel(IScreenContext context, float centerX, float centerY,
                                 int value, boolean horizontal, int color) {
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
