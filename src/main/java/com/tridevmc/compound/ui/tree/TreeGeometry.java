package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.ui.geometry.api.FrameGeometry;
import com.tridevmc.compound.ui.geometry.api.WorldPoint;
import com.tridevmc.compound.ui.layout.Bounds;
import java.util.List;

final class TreeGeometry {
    static void prepare(ITreeNode node) {
        node.prepareGeometry();
        for (var child : node.getChildren()) {
            prepare(child);
        }
    }

    static void sample(ITreeNode node, FrameGeometry parent, Bounds inheritedClip) {
        var bounds = node.getBounds();
        if (bounds == null) return;
        var transform = node.sampleTransform();
        double cos = Math.cos(transform.radians());
        double sin = Math.sin(transform.radians());
        double a = cos * transform.scaleX();
        double b = sin * transform.scaleX();
        double c = -sin * transform.scaleY();
        double d = cos * transform.scaleY();
        double px = bounds.x() + transform.pivotX();
        double py = bounds.y() + transform.pivotY();
        double tx = px + transform.x() - a * px - c * py;
        double ty = py + transform.y() - b * px - d * py;
        if (parent != null) {
            double na = parent.a() * a + parent.c() * b;
            double nb = parent.b() * a + parent.d() * b;
            double nc = parent.a() * c + parent.c() * d;
            double nd = parent.b() * c + parent.d() * d;
            double nx = parent.a() * tx + parent.c() * ty + parent.tx();
            ty = parent.b() * tx + parent.d() * ty + parent.ty();
            a = na;
            b = nb;
            c = nc;
            d = nd;
            tx = nx;
        }
        var corners = List.of(point(a, b, c, d, tx, ty, bounds.left(), bounds.top()),
                point(a, b, c, d, tx, ty, bounds.right(), bounds.top()),
                point(a, b, c, d, tx, ty, bounds.right(), bounds.bottom()),
                point(a, b, c, d, tx, ty, bounds.left(), bounds.bottom()));
        double left = corners.stream().mapToDouble(WorldPoint::x).min().orElseThrow();
        double top = corners.stream().mapToDouble(WorldPoint::y).min().orElseThrow();
        double right = corners.stream().mapToDouble(WorldPoint::x).max().orElseThrow();
        double bottom = corners.stream().mapToDouble(WorldPoint::y).max().orElseThrow();
        Bounds clip = node.getLayoutProperties().getLayer() > 0 ? null : inheritedClip;
        if (node.getLayoutProperties().isClip()) {
            if (Math.abs(b) > 1e-9 || Math.abs(c) > 1e-9) {
                throw new UnsupportedOperationException("Rotated clipping requires a stencil backend; scissors are axis-aligned");
            }
            var ownClip = new Bounds((int) Math.ceil(left), (int) Math.ceil(top),
                    Math.max(0, (int) Math.floor(right) - (int) Math.ceil(left)),
                    Math.max(0, (int) Math.floor(bottom) - (int) Math.ceil(top)));
            clip = clip == null ? ownClip : clip.intersection(ownClip);
        }
        var geometry = new FrameGeometry(a, b, c, d, tx, ty, bounds, corners,
                left, top, right, bottom, clip);
        node.frameGeometry(geometry);
        for (var child : node.getChildren()) {
            sample(child, geometry, clip);
        }
    }

    private static WorldPoint point(double a, double b, double c, double d,
            double tx, double ty, double x, double y) {
        return new WorldPoint(a * x + c * y + tx, b * x + d * y + ty);
    }
}
