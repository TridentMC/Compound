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
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.tree.ITreeNode;

import java.util.ArrayList;

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
        var ancestorChain = new ArrayList<ITreeNode>();
        ITreeNode current = hoveredNode;
        while (current != null) {
            ancestorChain.add(0, current);
            current = current.getParent();
        }
        for (int i = 0; i < ancestorChain.size() - 1; i++) {
            ITreeNode node = ancestorChain.get(i);
            LayoutDebugGeometry.drawBoxModel(context, node, false);
        }
        LayoutDebugGeometry.drawAllocatedVsActual(context, hoveredNode);
        LayoutDebugGeometry.drawBoxModel(context, hoveredNode, true);
        for (int i = 0; i < ancestorChain.size() - 1; i++) {
            ITreeNode ancestor = ancestorChain.get(i);
            ITreeNode childInAncestor = ancestorChain.get(i + 1);
            if (ancestor.getElement() instanceof Column || ancestor.getElement() instanceof Row) {
                LayoutDebugGeometry.drawSpacing(context, childInAncestor, ancestor);
            }
            var ancestorProps = ancestor.getLayoutProperties();
            if (ancestorProps != null && ancestorProps.getContentAlignment() != null) {
                LayoutDebugGeometry.drawAlignmentDrivers(context, childInAncestor, ancestor);
            }
        }
        LayoutDebugDetails.render(context, hoveredNode, ancestorChain);
    }
}
