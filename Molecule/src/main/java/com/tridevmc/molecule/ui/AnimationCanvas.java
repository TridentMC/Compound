package com.tridevmc.molecule.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.tridevmc.compound.ui.animation.Easing;
import com.tridevmc.compound.ui.animation.Interpolators;
import com.tridevmc.compound.ui.element.Box;
import com.tridevmc.compound.ui.element.Element;
import com.tridevmc.compound.ui.element.GradientRect;
import com.tridevmc.compound.ui.element.IComposableElement;
import com.tridevmc.compound.ui.element.Rect;
import com.tridevmc.compound.ui.element.Stack;
import com.tridevmc.compound.ui.element.Text;
import com.tridevmc.compound.ui.geometry.api.ITransform2D;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.state.State;
import net.minecraft.network.chat.Component;
import java.time.Duration;
import java.util.List;

final class AnimationCanvas extends Element implements IComposableElement {
    private final AnimationModel model;
    private final State<Boolean> block = State.of(false);
    private Box dvdContent;
    private static final int DVD_BLOCK_WIDTH = 26;
    private static final int DVD_BLOCK_HEIGHT = 18;
    private static final int LABEL_Y = 69;
    private static final String MARQUEE = "Compound animations  •  hover to pause  •  click to reverse";

    AnimationCanvas(AnimationModel model) {
        this.model = model;
    }

    void toggleBlock() {
        this.model.block = !this.model.block;
        this.block.set(this.model.block);
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.e(new Stack(), stage -> {
            stage.layout().fillMax().clip();
            stage.beforeGeometry(this::synchronizeMotion);
            stage.e(new Rect(0xFF151B2B), background -> background.layout().fillMax());
            stage.e(new GradientRect(this.model.palette::value, () -> {
                float shift = (float) Math.sin(this.model.timeline.phase(Duration.ofSeconds(4)) * Math.PI * 2);
                return Interpolators.interpolateColor(this.model.palette.value(), 0xFFE879F9, (shift + 1) / 2);
            }), gradient -> {
                gradient.layout().fillMaxWidth().fixedHeight(AnimationGeometry.GRADIENT_HEIGHT);
                gradient.onClick(event -> {
                    if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
                    this.model.changePalette();
                    return true;
                });
            });
            stage.e(new Text(Component.literal("Gradient • click to retarget"), 0xFFFFFF), label -> {
                label.transform(() -> ITransform2D.translation(8, 9));
                label.onClick(event -> {
                    this.model.changePalette();
                    return true;
                });
            });
            stage.e(new GradientRect(this.model.palette::value, () -> 0xFFB6F0FF), spin -> {
                int halfSize = AnimationGeometry.SPIN_HALF_SIZE;
                spin.layout().fixedSize(halfSize * 2, halfSize * 2);
                spin.transform(() -> ITransform2D.of(
                        this.getBounds().width() * AnimationGeometry.SPIN_FRACTION - halfSize,
                        AnimationGeometry.SPIN_Y - halfSize,
                        this.model.spinTurns() * Math.PI * 2, 1, 1, halfSize, halfSize));
                spin.onClick(event -> {
                    if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
                    this.model.reverseSpin();
                    return true;
                });
            });
            stage.e(new Rect(this.model.palette::value), bounce -> {
                int halfWidth = AnimationGeometry.BOUNCE_HALF_WIDTH;
                int halfHeight = AnimationGeometry.BOUNCE_HALF_HEIGHT;
                bounce.layout().fixedSize(halfWidth * 2, halfHeight * 2);
                bounce.transform(() -> ITransform2D.translation(
                        this.getBounds().width() * AnimationGeometry.BOUNCE_FRACTION - halfWidth,
                        this.bounceY() - halfHeight));
                bounce.onClick(event -> {
                    if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
                    this.model.changePalette();
                    return true;
                });
            });
            stage.e(new Text(Component.literal("Spin: reverse"), 0xD8DEEE), label ->
                    label.transform(() -> ITransform2D.translation(4, LABEL_Y)));
            stage.e(new Text(Component.literal("Bounce: palette"), 0xD8DEEE), label ->
                    label.transform(() -> ITransform2D.translation(
                            this.getBounds().width() * .52, LABEL_Y)));
            stage.e(new Box(), marquee -> {
                int height = AnimationGeometry.DVD_TOP - AnimationGeometry.MARQUEE_TOP - 1;
                marquee.layout().fillMaxWidth().fixedHeight(height).clip();
                marquee.transform(() -> ITransform2D.translation(0, AnimationGeometry.MARQUEE_TOP));
                marquee.onClick(event -> {
                    if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
                    this.model.reverseMarquee();
                    return true;
                });
                marquee.onMouseEnter(() -> this.model.hover(true));
                marquee.onMouseExit(() -> this.model.hover(false));
                marquee.e(new Text(Component.literal(MARQUEE), 0xFFFFFF), text -> {
                    text.layout().unboundedWidth();
                    text.transform(() -> ITransform2D.translation(this.getBounds().width()
                            - this.model.marqueeProgress() * (this.getBounds().width()
                            + text.getElement().getBounds().width()), 1));
                });
            });
            stage.e(new Box(), dvd -> {
                this.dvdContent = dvd.getElement();
                dvd.bind(this.block);
                dvd.transform(() -> ITransform2D.translation(
                        this.model.dvd.x(), this.dvdTop() + this.model.dvd.y()));
                if (this.block.get()) {
                    dvd.e(new Rect(this::dvdColor), rect ->
                            rect.layout().fixedSize(DVD_BLOCK_WIDTH, DVD_BLOCK_HEIGHT));
                } else {
                    dvd.e(new Text(() -> Component.literal("DVD"), this::dvdColor, () -> false));
                }
                dvd.onClick(event -> {
                    if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
                    var local = stage.toLocal(event.x(), event.y());
                    return local.map(point -> this.model.dvd.beginDrag(point.x(), point.y() - this.dvdTop(),
                            this.dvdSize().width(), this.dvdSize().height())).orElse(false);
                });
                dvd.onMouseDrag(event -> {
                    if (!this.model.dvd.isDragging() || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
                    stage.toLocal(event.x(), event.y()).ifPresent(point ->
                            this.model.dvd.drag(point.x(), point.y() - this.dvdTop()));
                    return true;
                });
                dvd.onMouseRelease(event -> {
                    if (!this.model.dvd.isDragging() || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
                    this.model.dvd.release(this.model.timeline.elapsedSeconds());
                    return true;
                });
            });
        });
    }

    private int dvdColor() {
        return switch ((int) (this.model.dvd.collisions() % 4)) {
            case 0 -> 0xFF48E5C2;
            case 1 -> 0xFFFFD166;
            case 2 -> 0xFFF78CFF;
            default -> 0xFF78BFFF;
        };
    }

    private Size dvdSize() {
        return this.dvdContent.getBounds().size();
    }

    private double dvdTop() {
        return AnimationGeometry.DVD_TOP;
    }

    private double bounceY() {
        double phase = this.model.timeline.phase(Duration.ofSeconds(2));
        float triangle = (float) (phase < .5 ? phase * 2 : 2 - phase * 2);
        return AnimationGeometry.BOUNCE_BASE + Easing.EASE_IN_OUT.apply(triangle) * AnimationGeometry.BOUNCE_TRAVEL;
    }

    void synchronizeMotion() {
        var size = this.dvdSize();
        var bounds = this.getBounds();
        this.model.advance(bounds.width(), Math.max(0, bounds.height() - this.dvdTop()), size.width(), size.height());
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties properties, List<Size> children) {
        return constraints.constrain(new Size(380, 145));
    }

    @Override
    public List<Bounds> place(Bounds bounds, LayoutProperties properties, List<Size> children) {
        return children.stream().map(size -> bounds).toList();
    }

    @Override
    public void onDetached() {
        this.model.dvd.release(this.model.timeline.elapsedSeconds());
        this.model.hover(false);
    }
}
