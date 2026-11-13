package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.element.Box;
import com.tridevmc.compound.ui.element.Column;
import com.tridevmc.compound.ui.element.Row;
import com.tridevmc.compound.ui.scope.ICompositionScope;

final class GalleryAnimations extends GalleryModule {
    final AnimationModel model = new AnimationModel();
    final AnimationCanvas canvas = new AnimationCanvas(this.model);

    @Override
    public void compose(ICompositionScope scope) {
        scope.useAnimationTimeline(this.model.timeline);
        scope.useAnimationTimeline(this.model.marquee);
        scope.e(new Column(), column -> {
            column.layout().fillMax().spacing(3);
            column.e(new Row(), row -> {
                row.layout().fillMaxWidth().spacing(3);
                control(row, "Pause / resume", this.model::togglePause);
                control(row, "Reset", this.model::reset);
                control(row, "Speed ½ / 1 / 2", this.model::speed);
            });
            column.e(new Row(), row -> {
                row.layout().fillMaxWidth().spacing(3);
                control(row, "Palette", this.model::changePalette);
                control(row, "Reverse", this.model::reverse);
                control(row, "DVD text / block", this.canvas::toggleBlock);
            });
            column.e(new Box(), stage -> {
                stage.layout().fillMaxWidth().weight(1);
                stage.e(this.canvas, canvas -> canvas.layout().fillMax());
            });
        });
    }

    private static void control(ICompositionScope scope, String text, Runnable action) {
        scope.e(new Box(), cell -> {
            cell.layout().weight(1);
            GalleryWidgets.button(cell, text, action);
        });
    }
}
