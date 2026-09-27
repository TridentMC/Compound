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

        // Build the ancestor chain from root to hovered node
        var ancestorChain = new ArrayList<ITreeNode>();
        ITreeNode current = hoveredNode;
        while (current != null) {
            ancestorChain.add(0, current); // Add at beginning to get root-to-node order
            current = current.getParent();
        }

        // 1. Render Ancestors (Dimmed Box Model)
        for (int i = 0; i < ancestorChain.size() - 1; i++) {
            ITreeNode node = ancestorChain.get(i);
            drawBoxModel(context, node, false);
        }

        // 2. Render Allocated Bounds (what parent gave us) vs Actual Bounds
        drawAllocatedVsActual(context, hoveredNode);

        // 3. Render Hovered Element (Full Box Model)
        drawBoxModel(context, hoveredNode, true);

        // 4. Render Layout Drivers for ALL Column/Row ancestors in the chain
        // This is critical: For composed elements like Label->Text, the Column
        // might be 2+ levels up, not the immediate parent!
        for (int i = 0; i < ancestorChain.size() - 1; i++) {
            ITreeNode ancestor = ancestorChain.get(i);
            ITreeNode childInAncestor = ancestorChain.get(i + 1);

            // Visualize spacing for Column/Row ancestors
            if (ancestor.getElement() instanceof Column || ancestor.getElement() instanceof Row) {
                drawLayoutDrivers(context, childInAncestor, ancestor, childInAncestor == hoveredNode);
            }

            // Visualize alignment springs for ancestors with contentAlignment (Stack, Box, etc.)
            // This is key for showing WHY text is centered in buttons!
            var ancestorProps = ancestor.getLayoutProperties();
            if (ancestorProps != null && ancestorProps.getContentAlignment() != null) {
                drawAlignmentDrivers(context, childInAncestor, ancestor);
            }
        }

        // 5. Render Enhanced Info Panel
        drawDebugInfoPanel(context, hoveredNode, ancestorChain);
    }

    /**
     * Visualizes the difference between allocated bounds (what parent gave) and actual bounds.
     * This shows WHY there are gaps around elements due to centering/alignment.
     */
    private void drawAllocatedVsActual(IScreenContext context, ITreeNode node) {
        var allocatedBounds = node.getAllocatedBounds();
        var actualBounds = node.getBounds();

        if (allocatedBounds == null || actualBounds == null) {
            return;
        }

        // Only draw if there's actually a difference
        if (allocatedBounds.equals(actualBounds)) {
            return;
        }

        int unusedColor = DebugOverlayConfig.COLOR_UNUSED_SPACE;

        // Draw unused space as yellow fill
        // Top gap
        if (actualBounds.y() > allocatedBounds.y()) {
            float gapHeight = actualBounds.y() - allocatedBounds.y();
            context.drawRect(allocatedBounds.x(), allocatedBounds.y(),
                    allocatedBounds.width(), gapHeight, unusedColor);
        }

        // Bottom gap
        float actualBottom = actualBounds.y() + actualBounds.height();
        float allocBottom = allocatedBounds.y() + allocatedBounds.height();
        if (actualBottom < allocBottom) {
            float gapHeight = allocBottom - actualBottom;
            context.drawRect(allocatedBounds.x(), actualBottom,
                    allocatedBounds.width(), gapHeight, unusedColor);
        }

        // Left gap
        if (actualBounds.x() > allocatedBounds.x()) {
            float gapWidth = actualBounds.x() - allocatedBounds.x();
            context.drawRect(allocatedBounds.x(), actualBounds.y(),
                    gapWidth, actualBounds.height(), unusedColor);
        }

        // Right gap
        float actualRight = actualBounds.x() + actualBounds.width();
        float allocRight = allocatedBounds.x() + allocatedBounds.width();
        if (actualRight < allocRight) {
            float gapWidth = allocRight - actualRight;
            context.drawRect(actualRight, actualBounds.y(),
                    gapWidth, actualBounds.height(), unusedColor);
        }

        // Draw dashed outline for allocated bounds
        int allocOutlineColor = DebugOverlayConfig.COLOR_ALLOCATED_OUTLINE;
        drawDashedRect(context, allocatedBounds.x(), allocatedBounds.y(),
                allocatedBounds.width(), allocatedBounds.height(), allocOutlineColor, 4);
    }

    /**
     * Draws a dashed rectangle outline.
     */
    private void drawDashedRect(IScreenContext context, float x, float y, float w, float h, int color, int dashLen) {
        // Top edge
        for (float dx = 0; dx < w; dx += dashLen * 2) {
            float len = Math.min(dashLen, w - dx);
            context.drawRect(x + dx, y, len, 1, color);
        }
        // Bottom edge
        for (float dx = 0; dx < w; dx += dashLen * 2) {
            float len = Math.min(dashLen, w - dx);
            context.drawRect(x + dx, y + h - 1, len, 1, color);
        }
        // Left edge
        for (float dy = 0; dy < h; dy += dashLen * 2) {
            float len = Math.min(dashLen, h - dy);
            context.drawRect(x, y + dy, 1, len, color);
        }
        // Right edge
        for (float dy = 0; dy < h; dy += dashLen * 2) {
            float len = Math.min(dashLen, h - dy);
            context.drawRect(x + w - 1, y + dy, 1, len, color);
        }
    }

    /**
     * Draws the CSS Box Model (Margin, Padding, Content, Outline) for a node.
     */
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

        // Draw Margin (Orange)
        int mL = props.getMarginLeft();
        int mT = props.getMarginTop();
        int mR = props.getMarginRight();
        int mB = props.getMarginBottom();

        if (mL > 0) context.drawRect(x - mL, y, mL, h, marginColor);
        if (mT > 0) context.drawRect(x, y - mT, w, mT, marginColor);
        if (mR > 0) context.drawRect(x + w, y, mR, h, marginColor);
        if (mB > 0) context.drawRect(x, y + h, w, mB, marginColor);

        // Draw Padding (Green)
        int pL = props.getPaddingLeft();
        int pT = props.getPaddingTop();
        int pR = props.getPaddingRight();
        int pB = props.getPaddingBottom();

        if (pL > 0) context.drawRect(x, y, pL, h, paddingColor);
        if (pT > 0) context.drawRect(x + pL, y, w - pL - pR, pT, paddingColor);
        if (pR > 0) context.drawRect(x + w - pR, y, pR, h, paddingColor);
        if (pB > 0) context.drawRect(x + pL, y + h - pB, w - pL - pR, pB, paddingColor);

        // Draw Outline
        context.drawRectOutline(x, y, w, h, outlineColor, 1);

        // If hovered, draw detailed margin/padding labels
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

    /**
     * Visualizes spacing bars for Column/Row parents.
     * @param isDirectChild if true, the child is the directly hovered element (shows bright labels)
     */
    private void drawLayoutDrivers(IScreenContext context, ITreeNode child, ITreeNode parent, boolean isDirectChild) {
        var parentProps = parent.getLayoutProperties();
        var parentElement = parent.getElement();
        if (parentProps == null) return;

        var parentBounds = parent.getBounds();
        var childBounds = child.getBounds();
        if (parentBounds == null || childBounds == null) return;

        // --- Spacing Bars for Column/Row ---
        if (parentElement instanceof Column || parentElement instanceof Row) {
            boolean isColumn = parentElement instanceof Column;
            var siblings = parent.getChildren();
            int spacingColor = DebugOverlayConfig.COLOR_SPACING;
            int dimmedSpacingColor = 0x40E91E63; // Dimmed pink for non-adjacent

            // Find index of target child
            int childIndex = -1;
            for (int i = 0; i < siblings.size(); i++) {
                if (siblings.get(i) == child) {
                    childIndex = i;
                    break;
                }
            }

            // Draw spacing gaps
            for (int i = 0; i < siblings.size() - 1; i++) {
                var node1 = siblings.get(i);
                var node2 = siblings.get(i + 1);
                var b1 = node1.getBounds();
                var b2 = node2.getBounds();

                if (b1 != null && b2 != null) {
                    // Is this gap adjacent to the target element?
                    boolean isAdjacentGap = (i == childIndex - 1) || (i == childIndex);
                    // Use bright color for adjacent, dimmed for others
                    int color = isAdjacentGap ? spacingColor : dimmedSpacingColor;

                    if (isColumn) {
                        float gapY = b1.y() + b1.height();
                        float gapH = b2.y() - gapY;
                        if (gapH > 0 && gapH < 100) {
                            float gapX = Math.max(b1.x(), b2.x());
                            float gapW = Math.min(b1.width(), b2.width());
                            context.drawRect(gapX, gapY, gapW, gapH, color);

                            // Draw pixel label on adjacent gaps
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

                            // Draw pixel label on adjacent gaps
                            if (isAdjacentGap && gapW >= 2) {
                                drawArrowLabel(context, gapX + gapW/2f, gapY + gapH/2f, (int)gapW, true, color);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Visualizes alignment springs for non-Column/Row parents (Stack, Box, etc.)
     */
    private void drawAlignmentDrivers(IScreenContext context, ITreeNode child, ITreeNode parent) {
        var parentProps = parent.getLayoutProperties();
        if (parentProps == null) return;

        var parentBounds = parent.getBounds();
        var childBounds = child.getBounds();
        if (parentBounds == null || childBounds == null) return;

        // Alignment Springs (Yellow)
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

            // Horizontal Springs
            if (align == Alignment.CENTER || align == Alignment.CENTER_LEFT || align == Alignment.CENTER_RIGHT ||
                align == Alignment.TOP_CENTER || align == Alignment.BOTTOM_CENTER) {

                // Left Spring
                if (cx > px) {
                    float w = cx - px;
                    context.drawRect(px, cy, w, ch, springColor);
                    context.drawRect(px, cy + ch/2f, w, 1, 0xFFFFFF00);
                }

                // Right Spring
                if (cx + cw < px + pw) {
                    float w = (px + pw) - (cx + cw);
                    context.drawRect(cx + cw, cy, w, ch, springColor);
                    context.drawRect(cx + cw, cy + ch/2f, w, 1, 0xFFFFFF00);
                }
            }

            // Vertical Springs (for vertical centering)
            if (align == Alignment.CENTER || align == Alignment.TOP_CENTER || align == Alignment.BOTTOM_CENTER ||
                align == Alignment.CENTER_LEFT || align == Alignment.CENTER_RIGHT) {

                // Top Spring
                if (cy > py) {
                    float h = cy - py;
                    context.drawRect(cx, py, cw, h, springColor);
                    context.drawRect(cx + cw/2f, py, 1, h, 0xFFFFFF00);
                }

                // Bottom Spring
                if (cy + ch < py + ph) {
                    float h = (py + ph) - (cy + ch);
                    context.drawRect(cx, cy + ch, cw, h, springColor);
                    context.drawRect(cx + cw/2f, cy + ch, 1, h, 0xFFFFFF00);
                }
            }
        }
    }

    /**
     * Draws the enhanced layout info panel with hierarchy, bounds analysis, and gap explanations.
     */
    private void drawDebugInfoPanel(IScreenContext context, ITreeNode node, List<ITreeNode> ancestorChain) {
        var element = node.getElement();
        var bounds = node.getBounds();
        var allocatedBounds = node.getAllocatedBounds();
        var measuredSize = node.getMeasuredSize();
        var parent = node.getParent();
        var props = node.getLayoutProperties();
        if (props == null) props = LayoutProperties.create();

        var lines = new ArrayList<String>();
        var colors = new ArrayList<Integer>();

        // Title with element type and size
        String title = element.getClass().getSimpleName() + " (" + (int)bounds.width() + "×" + (int)bounds.height() + ")";
        lines.add(title);
        colors.add(0xFFFFFFFF);

        // Hierarchy breadcrumb (last 4 ancestors max)
        if (ancestorChain.size() > 1) {
            var breadcrumb = new StringBuilder();
            int start = Math.max(0, ancestorChain.size() - 4);
            for (int i = start; i < ancestorChain.size(); i++) {
                if (i > start) breadcrumb.append(" > ");
                breadcrumb.append(ancestorChain.get(i).getElement().getClass().getSimpleName());
            }
            lines.add(breadcrumb.toString());
            colors.add(0xFF888888);
        }

        lines.add("");
        colors.add(0xFF888888);

        // Bounds Analysis Section
        lines.add("BOUNDS ANALYSIS");
        colors.add(0xFF4FC3F7); // Light blue header

        lines.add("  Position: (" + (int)bounds.x() + ", " + (int)bounds.y() + ")");
        colors.add(0xFFCCCCCC);

        if (measuredSize != null) {
            lines.add("  Measured: " + measuredSize.width() + "×" + measuredSize.height());
            colors.add(0xFFCCCCCC);
        }

        // Show allocated vs actual bounds if different
        if (allocatedBounds != null && !allocatedBounds.equals(bounds)) {
            lines.add("  Allocated: " + (int)allocatedBounds.width() + "×" + (int)allocatedBounds.height());
            colors.add(0xFFFFFF00); // Yellow - this is the key info!

            // Calculate and show gaps
            int gapTop = bounds.y() - allocatedBounds.y();
            int gapBottom = (allocatedBounds.y() + allocatedBounds.height()) - (bounds.y() + bounds.height());
            int gapLeft = bounds.x() - allocatedBounds.x();
            int gapRight = (allocatedBounds.x() + allocatedBounds.width()) - (bounds.x() + bounds.width());

            if (gapTop > 0 || gapBottom > 0) {
                lines.add("  Gap V: ↑" + gapTop + "px ↓" + gapBottom + "px");
                colors.add(0xFFFFFF00);
            }
            if (gapLeft > 0 || gapRight > 0) {
                lines.add("  Gap H: ←" + gapLeft + "px →" + gapRight + "px");
                colors.add(0xFFFFFF00);
            }
        }

        // Parent layout info
        if (parent != null) {
            lines.add("");
            colors.add(0xFF888888);

            lines.add("PARENT LAYOUT (" + parent.getElement().getClass().getSimpleName() + ")");
            colors.add(0xFF81C784); // Light green header

            var pProps = parent.getLayoutProperties();
            if (pProps != null) {
                // Show alignment that caused centering
                Alignment align = pProps.getContentAlignment();
                if (align == null && parent.getElement() instanceof Column) align = pProps.getHorizontalAlignment();
                if (align == null && parent.getElement() instanceof Row) align = pProps.getVerticalAlignment();

                if (align != null) {
                    lines.add("  align: " + align.name());
                    colors.add(0xFFFFEB3B); // Yellow
                }

                if (pProps.getSpacing() > 0) {
                    lines.add("  spacing: " + pProps.getSpacing() + "px");
                    colors.add(0xFFFF69B4); // Pink
                }

                int pPad = pProps.getPaddingLeft() + pProps.getPaddingRight() +
                           pProps.getPaddingTop() + pProps.getPaddingBottom();
                if (pPad > 0) {
                    lines.add("  padding: " + pProps.getPaddingTop() + " " + pProps.getPaddingRight() +
                             " " + pProps.getPaddingBottom() + " " + pProps.getPaddingLeft());
                    colors.add(0xFF4CAF50); // Green
                }
            }
        }

        // Sibling context - search ancestor chain for Column/Row containers
        // This is crucial for composed elements like Label->Text where Column is 2 levels up
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
                    lines.add("");
                    colors.add(0xFF888888);

                    String containerName = ancestor.getElement().getClass().getSimpleName();
                    var ancestorProps = ancestor.getLayoutProperties();
                    int spacing = ancestorProps != null ? ancestorProps.getSpacing() : 0;

                    lines.add("LAYOUT IN " + containerName + " (spacing=" + spacing + ")");
                    colors.add(0xFFFF69B4); // Pink header

                    // Previous sibling
                    if (childIndex > 0) {
                        var prevNode = siblings.get(childIndex - 1);
                        var prevBounds = prevNode.getBounds();
                        var prevElement = prevNode.getElement();
                        var childBounds = childInAncestor.getBounds();

                        String prevName = prevElement.getClass().getSimpleName();
                        if (prevBounds != null) {
                            prevName += " (" + (int)prevBounds.width() + "×" + (int)prevBounds.height() + ")";
                        }
                        lines.add("  prev: " + prevName);
                        colors.add(0xFFCCCCCC);

                        // Calculate gap between prev and child
                        if (prevBounds != null && childBounds != null) {
                            int gap = isColumn ?
                                childBounds.y() - (prevBounds.y() + prevBounds.height()) :
                                childBounds.x() - (prevBounds.x() + prevBounds.width());
                            if (gap > 0) {
                                lines.add("  ↕ gap above: " + gap + "px");
                                colors.add(0xFFFF69B4); // Pink
                            }
                        }
                    }

                    // Current element indicator
                    var childBounds = childInAncestor.getBounds();
                    String childName = childInAncestor.getElement().getClass().getSimpleName();
                    if (childBounds != null) {
                        childName += " (" + (int)childBounds.width() + "×" + (int)childBounds.height() + ")";
                    }
                    lines.add("  → this: " + childName);
                    colors.add(0xFFFFFFFF);

                    // Next sibling
                    if (childIndex < siblings.size() - 1) {
                        var nextNode = siblings.get(childIndex + 1);
                        var nextBounds = nextNode.getBounds();
                        var nextElement = nextNode.getElement();

                        // Calculate gap between child and next
                        if (nextBounds != null && childBounds != null) {
                            int gap = isColumn ?
                                nextBounds.y() - (childBounds.y() + childBounds.height()) :
                                nextBounds.x() - (childBounds.x() + childBounds.width());
                            if (gap > 0) {
                                lines.add("  ↕ gap below: " + gap + "px");
                                colors.add(0xFFFF69B4);
                            }
                        }

                        String nextName = nextElement.getClass().getSimpleName();
                        if (nextBounds != null) {
                            nextName += " (" + (int)nextBounds.width() + "×" + (int)nextBounds.height() + ")";
                        }
                        lines.add("  next: " + nextName);
                        colors.add(0xFFCCCCCC);
                    }

                    // Only show one Column/Row context (the innermost one)
                    break;
                }
            }
        }

        // Centering context - search ancestor chain for contentAlignment
        // Iterate in REVERSE so we find the INNERMOST ancestor (closest to hovered element)
        // This shows WHY text is centered in buttons (Stack with contentAlignment=CENTER)
        for (int i = ancestorChain.size() - 2; i >= 0; i--) {
            ITreeNode ancestor = ancestorChain.get(i);
            ITreeNode childInAncestor = ancestorChain.get(i + 1);

            var ancestorProps = ancestor.getLayoutProperties();
            if (ancestorProps != null && ancestorProps.getContentAlignment() != null) {
                Alignment align = ancestorProps.getContentAlignment();
                var ancestorBounds = ancestor.getBounds();
                var childBounds = childInAncestor.getBounds();

                lines.add("");
                colors.add(0xFF888888);

                String ancestorName = ancestor.getElement().getClass().getSimpleName();
                lines.add("CENTERING (" + ancestorName + ")");
                colors.add(0xFFFFEB3B); // Yellow header

                lines.add("  contentAlignment: " + align.name());
                colors.add(0xFFFFEB3B);

                if (ancestorBounds != null && childBounds != null) {
                    // Calculate centering gaps (accounting for padding)
                    float px = ancestorBounds.x() + ancestorProps.getPaddingLeft();
                    float py = ancestorBounds.y() + ancestorProps.getPaddingTop();
                    float pw = ancestorBounds.width() - ancestorProps.getPaddingLeft() - ancestorProps.getPaddingRight();
                    float ph = ancestorBounds.height() - ancestorProps.getPaddingTop() - ancestorProps.getPaddingBottom();

                    int leftGap = (int)(childBounds.x() - px);
                    int rightGap = (int)((px + pw) - (childBounds.x() + childBounds.width()));
                    int topGap = (int)(childBounds.y() - py);
                    int bottomGap = (int)((py + ph) - (childBounds.y() + childBounds.height()));

                    if (leftGap > 0 || rightGap > 0) {
                        lines.add("  H gaps: ←" + leftGap + "px  →" + rightGap + "px");
                        colors.add(0xFFFFEB3B);
                    }
                    if (topGap > 0 || bottomGap > 0) {
                        lines.add("  V gaps: ↑" + topGap + "px  ↓" + bottomGap + "px");
                        colors.add(0xFFFFEB3B);
                    }
                }

                // Only show innermost centering ancestor
                break;
            }
        }

        // This element's properties
        int mTot = props.getMarginLeft() + props.getMarginRight() + props.getMarginTop() + props.getMarginBottom();
        int pTot = props.getPaddingLeft() + props.getPaddingRight() + props.getPaddingTop() + props.getPaddingBottom();

        if (mTot > 0 || pTot > 0 || props.isFillMaxWidth() || props.isFillMaxHeight()) {
            lines.add("");
            colors.add(0xFF888888);

            lines.add("THIS ELEMENT");
            colors.add(0xFFFFAB40); // Orange header

            if (mTot > 0) {
                lines.add("  margin: " + props.getMarginTop() + " " + props.getMarginRight() +
                         " " + props.getMarginBottom() + " " + props.getMarginLeft());
                colors.add(0xFFFFA500); // Orange
            }

            if (pTot > 0) {
                lines.add("  padding: " + props.getPaddingTop() + " " + props.getPaddingRight() +
                         " " + props.getPaddingBottom() + " " + props.getPaddingLeft());
                colors.add(0xFF4CAF50); // Green
            }

            if (props.isFillMaxWidth() || props.isFillMaxHeight()) {
                String fill = "";
                if (props.isFillMaxWidth() && props.isFillMaxHeight()) fill = "fillMax";
                else if (props.isFillMaxWidth()) fill = "fillMaxWidth";
                else fill = "fillMaxHeight";
                lines.add("  " + fill);
                colors.add(0xFFCE93D8); // Purple
            }
        }

        // Calculate panel dimensions
        int lineHeight = 10;
        int panelPadding = 6;
        float panelW = 220;
        float panelH = lines.size() * lineHeight + panelPadding * 2;

        // Position panel in top-right, but ensure it's visible
        float px = context.getWidth() - panelW - 5;
        float py = 5;

        // Draw panel background
        context.drawRect(px, py, panelW, panelH, 0xE8000000);
        context.drawRectOutline(px, py, panelW, panelH, 0xFF333333, 1);

        // Draw lines
        for (int i = 0; i < lines.size(); i++) {
            context.drawString(lines.get(i), px + panelPadding, py + panelPadding + i * lineHeight, colors.get(i));
        }
    }

    /**
     * Draws a measurement arrow label at the specified position.
     */
    private void drawArrowLabel(IScreenContext context, float centerX, float centerY,
                                 int value, boolean horizontal, int color) {
        String text = String.valueOf(value);
        int textWidth = context.getFont().width(text);
        int textHeight = 7;

        float textX = centerX - textWidth / 2f;
        float textY = centerY - textHeight / 2f;

        // Draw background for readability
        context.drawRect(textX - 1, textY - 1, textWidth + 2, textHeight + 2, 0xAA000000);

        // Draw arrow lines
        int arrowColor = 0xFFFFFFFF;
        if (horizontal) {
            // Horizontal arrows
            float arrowLen = Math.max(4, (value - textWidth) / 2f - 4);
            if (arrowLen > 2) {
                context.drawRect(centerX - textWidth / 2f - arrowLen, centerY - 0.5f, arrowLen - 2, 1, arrowColor);
                context.drawRect(centerX + textWidth / 2f + 2, centerY - 0.5f, arrowLen - 2, 1, arrowColor);
            }
        } else {
            // Vertical arrows
            float arrowLen = Math.max(4, (value - textHeight) / 2f - 4);
            if (arrowLen > 2) {
                context.drawRect(centerX - 0.5f, centerY - textHeight / 2f - arrowLen, 1, arrowLen - 2, arrowColor);
                context.drawRect(centerX - 0.5f, centerY + textHeight / 2f + 2, 1, arrowLen - 2, arrowColor);
            }
        }

        // Draw value text
        context.drawStringWithShadow(text, textX, textY, 0xFFFFFF);
    }
}
