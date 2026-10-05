package com.tridevmc.compound.ui.geometry.api;

import com.tridevmc.compound.ui.layout.Bounds;
import org.joml.Matrix3x2f;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

public record FrameGeometry(double a, double b, double c, double d, double tx, double ty,
        Bounds layoutBounds, List<WorldPoint> corners, double left, double top,
        double right, double bottom, @Nullable Bounds clip) {
    public FrameGeometry {
        corners = List.copyOf(corners);
    }

    public Optional<LocalPoint> toLocal(WorldPoint point) {
        return toLocal(point.x(), point.y());
    }

    public Optional<LocalPoint> toLocal(double worldX, double worldY) {
        double determinant = a * d - b * c;
        if (!Double.isFinite(determinant) || Math.abs(determinant) < 1e-12) return Optional.empty();
        double x = worldX - tx;
        double y = worldY - ty;
        return Optional.of(new LocalPoint((d * x - c * y) / determinant - layoutBounds.x(),
                (a * y - b * x) / determinant - layoutBounds.y()));
    }

    public boolean contains(double x, double y) {
        if (clip != null && !clip.contains((int) Math.floor(x), (int) Math.floor(y))) return false;
        return toLocal(x, y).map(point -> point.x() >= 0 && point.y() >= 0
                && point.x() < layoutBounds.width() && point.y() < layoutBounds.height()).orElse(false);
    }

    public Matrix3x2f matrix() {
        return new Matrix3x2f((float) a, (float) b, (float) c, (float) d, (float) tx, (float) ty);
    }
}
