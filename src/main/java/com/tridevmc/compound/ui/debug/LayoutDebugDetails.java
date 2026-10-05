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
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.ITreeNode;

import java.util.ArrayList;
import java.util.List;

final class LayoutDebugDetails {
    private record DebugLine(String text, int colour) {}

    private LayoutDebugDetails() {
    }

    static void render(IScreenContext context, ITreeNode node, List<ITreeNode> ancestorChain) {
        if (!DebugOverlayConfig.get().shouldShowDimensions()) return;
        var element = node.getElement();
        var bounds = node.getBounds();

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

        addBounds(lines, node);
        addParentLayout(lines, node.getParent());
        addSpacing(lines, ancestorChain);
        addAlignment(lines, ancestorChain);
        addElementProperties(lines, node.getLayoutProperties());

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

    private static void addBounds(List<DebugLine> lines, ITreeNode node) {
        var bounds = node.getBounds();
        var allocatedBounds = node.getAllocatedBounds();
        var measuredSize = node.getMeasuredSize();
        lines.add(new DebugLine("", 0xFF888888));
        lines.add(new DebugLine("BOUNDS ANALYSIS", 0xFF4FC3F7));

        lines.add(new DebugLine("  Layout: (" + bounds.x() + ", " + bounds.y() + ")", 0xFFCCCCCC));
        var geometry = node.getFrameGeometry();
        if (geometry != null) {
            lines.add(new DebugLine(String.format("  Visual: (%.2f, %.2f) %.2f×%.2f",
                    geometry.left(), geometry.top(), geometry.right() - geometry.left(),
                    geometry.bottom() - geometry.top()), 0xFFFFEB3B));
        }

        if (measuredSize != null) {
            lines.add(new DebugLine("  Measured: " + measuredSize.width() + "×" + measuredSize.height(), 0xFFCCCCCC));
        }
        if (allocatedBounds != null && !allocatedBounds.equals(bounds)) {
            lines.add(new DebugLine("  Allocated: " + (int)allocatedBounds.width() + "×" + (int)allocatedBounds.height(), 0xFFFFFF00));
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
    }

    private static void addParentLayout(List<DebugLine> lines, ITreeNode parent) {
        if (parent != null) {
            lines.add(new DebugLine("", 0xFF888888));

            lines.add(new DebugLine("PARENT LAYOUT (" + parent.getElement().getClass().getSimpleName() + ")", 0xFF81C784));

            var pProps = parent.getLayoutProperties();
            if (pProps != null) {
                Alignment align = pProps.getContentAlignment();
                if (align == null && parent.getElement() instanceof Column) align = pProps.getHorizontalAlignment();
                if (align == null && parent.getElement() instanceof Row) align = pProps.getVerticalAlignment();

                if (align != null) {
                    lines.add(new DebugLine("  align: " + align.name(), 0xFFFFEB3B));
                }

                if (pProps.getSpacing() > 0) {
                    lines.add(new DebugLine("  spacing: " + pProps.getSpacing() + "px", 0xFFFF69B4));
                }

                int pPad = pProps.getPaddingLeft() + pProps.getPaddingRight() +
                           pProps.getPaddingTop() + pProps.getPaddingBottom();
                if (pPad > 0) {
                    lines.add(new DebugLine("  padding: " + pProps.getPaddingTop() + " " + pProps.getPaddingRight() +
                             " " + pProps.getPaddingBottom() + " " + pProps.getPaddingLeft(), 0xFF4CAF50));
                }
            }
        }
    }

    private static void addSpacing(List<DebugLine> lines, List<ITreeNode> ancestorChain) {
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

                    lines.add(new DebugLine("LAYOUT IN " + containerName + " (spacing=" + spacing + ")", 0xFFFF69B4));
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
                                lines.add(new DebugLine("  ↕ gap above: " + gap + "px", 0xFFFF69B4));
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
    }

    private static void addAlignment(List<DebugLine> lines, List<ITreeNode> ancestorChain) {
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
                lines.add(new DebugLine("CENTERING (" + ancestorName + ")", 0xFFFFEB3B));

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
    }

    private static void addElementProperties(List<DebugLine> lines, LayoutProperties props) {
        if (props == null) props = LayoutProperties.create();
        int mTot = props.getMarginLeft() + props.getMarginRight() + props.getMarginTop() + props.getMarginBottom();
        int pTot = props.getPaddingLeft() + props.getPaddingRight() + props.getPaddingTop() + props.getPaddingBottom();

        if (mTot > 0 || pTot > 0 || props.isFillMaxWidth() || props.isFillMaxHeight()) {
            lines.add(new DebugLine("", 0xFF888888));

            lines.add(new DebugLine("THIS ELEMENT", 0xFFFFAB40));

            if (mTot > 0) {
                lines.add(new DebugLine("  margin: " + props.getMarginTop() + " " + props.getMarginRight() +
                         " " + props.getMarginBottom() + " " + props.getMarginLeft(), 0xFFFFA500));
            }

            if (pTot > 0) {
                lines.add(new DebugLine("  padding: " + props.getPaddingTop() + " " + props.getPaddingRight() +
                         " " + props.getPaddingBottom() + " " + props.getPaddingLeft(), 0xFF4CAF50));
            }

            if (props.isFillMaxWidth() || props.isFillMaxHeight()) {
                String fill = "";
                if (props.isFillMaxWidth() && props.isFillMaxHeight()) fill = "fillMax";
                else if (props.isFillMaxWidth()) fill = "fillMaxWidth";
                else fill = "fillMaxHeight";
                lines.add(new DebugLine("  " + fill, 0xFFCE93D8));
            }
        }
    }
}
