package org.ontologyengineering.protege.web.client.ui.pattern;

import com.google.common.base.Optional;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;
import lombok.*;
import org.ontologyengineering.protege.web.client.effect.VisualEffect;
import org.ontologyengineering.protege.web.client.ui.curve.Curve;
import org.ontologyengineering.protege.web.client.ui.dragsnap.Effects;
import org.ontologyengineering.protege.web.client.ui.dragsnap.Endpoint;
import org.ontologyengineering.protege.web.client.util.Position;
import org.ontologyengineering.protege.web.client.util.Rectangle;
import org.ontologyengineering.protege.web.client.util.Size;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.CurveRegistry;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.SearchManager;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableRect;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableShape;
import org.openrdf.query.algebra.evaluation.function.numeric.Abs;
import org.semanticweb.owlapi.model.IRI;

import java.util.Arrays;
import java.util.Collection;

public
// We would just use @Data but @EqualsAndHashCode is incompatible with GWT
// https://code.google.com/p/projectlombok/issues/detail?id=414
// because the GWT compiler does not support '$' in variable names
@ToString
class CurvePattern extends Pattern {

    @Getter private final CurveEndpoint endpoint;

    final private ButtonBar buttonBar;
    @Getter final private Effects visualEffects;

    // widgets that are not contained in the template frame necessarily
    final Collection<Endpoint> endpoints;

    /**
     * What happens when we manipulate one of the template curves (for
     * example, by dragging them out onto the canvas), or their associated
     * search boxes (for example, by typing into them)
     */
    @Getter
    class CurveEndpoint extends Endpoint implements MouseUpHandler, MouseOverHandler, MouseOutHandler {
        public CurveEndpoint(@NonNull DraggableShape curve,
                             @NonNull DraggableShape ghost,
                             @NonNull Position topLeft) {
            super(curve, ghost,
                  CurvePattern.this.getId() + "_new",
                  topLeft);
        }

        public void onLoad(@NonNull AbsolutePanel container) {
            super.onLoad(container);
            curve.getElement().setId(getCurveId());
            curve.addStyleName("dragsnap-curve");
            ghost.addStyleName("dragsnap-ghost");
            makeDraggable("#" + getCurveId());
        }

        protected void withdrawCurve() {
            final Position topLeft = relativeToParent(getHome());
            getParentPanel().setWidgetPosition(getCurve(), topLeft.getX(), topLeft.getY());
            getParentPanel().setWidgetPosition(getGhost(), topLeft.getX(), topLeft.getY());
        }

        /**
         * Relative to its canvas
         */
        public Position getCurveTopLeft() {
            final AbsolutePanel parent = getParentPanel();
            return new Position(
                    parent.getWidgetLeft(curve),
                    parent.getWidgetTop(curve));
        }

        public void reset() {
            withdrawCurve();
        }

        @Override
        public void onMouseOver(MouseOverEvent event) {
            getVisualEffects().addActiveCurve(this);
        }

        @Override
        public void onMouseOut(MouseOutEvent event) {
            getVisualEffects().removeActiveCurve(this);
        }

        @Override
        public void onMouseUp(MouseUpEvent event) {
            final Rectangle dbox = curve.getAbsoluteBBox();
            final Rectangle pbox = CurvePattern.this.getAbsoluteBBox();
            final boolean isFarEnough = (!dbox.intersects(pbox)) &&
                    dbox.getLeft() > pbox.getRight();
            if (isFarEnough) {
                final Position createTopLeft = getCurveTopLeft();
                CurvePattern.this.createCurve(createTopLeft);
                reset();
            }
        }

        public void bind() {
            curve.addDomHandler(this, MouseUpEvent.getType());
            curve.addDomHandler(this, MouseOverEvent.getType());
            curve.addDomHandler(this, MouseOutEvent.getType());
        }

        public void update() {

        }
    }

    public CurvePattern(@NonNull final String id,
                        @NonNull final CurveRegistry curveRegistry,
                        @NonNull final SearchManager searchManager,
                        @NonNull final AbsolutePanel parentPanel) {
        super(id, curveRegistry, searchManager, parentPanel);

        this.buttonBar = new ButtonBar();

        final DraggableShape wCurve = new DraggableRect(width, height, rounding);
        final DraggableShape wGhost = new DraggableRect(width, height, rounding);
        this.endpoint = new CurveEndpoint(wCurve, wGhost, new Position(1,1));
        this.endpoints = Arrays.<Endpoint>asList(endpoint);
        this.visualEffects = new CurveEffects(Arrays.<Widget>asList(wCurve, wGhost));
    }

    @Getter
    public class CurveEffects extends Effects {
        public CurveEffects(@NonNull final Collection<Widget> widgets) {
            super(widgets);
        }

        public void clear() {}
        public void addActiveCurve(Endpoint endpoint) {
            VisualEffect effect = activePattern(endpoint.getCurve(), "black");
            setEffect(endpoint.getCurve(), Optional.of(effect));
            applyAttributes();
        }

        public void removeActiveCurve(Endpoint endpoint) {
            setEffect(endpoint.getCurve(), Optional.<VisualEffect>absent());
            applyAttributes();
        }


    }

    @Override
    public void onLoad() {
        this.getElement().setId(getId());
        this.setPixelSize(Pattern.DEFAULT_TEMPLATE_WIDTH,
                Pattern.DEFAULT_TEMPLATE_HEIGHT);
        super.onLoad();
        for (Endpoint endpoint : endpoints) {
            endpoint.onLoad(getParentPanel());
        }
        visualEffects.applyAttributes();
        this.add(buttonBar, getSize().getWidth() + 5, 0);
        buttonBar.reposition(getSize());
    }

    public String getCurveId() {
        return this.getId() + "_curve";
    }

    @Getter
    class ButtonBar extends DockPanel {
        final private Label wLabel = new Label("CLASS");
        final Panel wButtons = new HorizontalPanel();

        private void activate() {
        }

        private void reposition(@NonNull final Size sz) {
            setHeight(sz.getHeight() + 10 + "px");
        }

        public ButtonBar() {
            wLabel.setWidth("6em");
            wButtons.getElement().setClassName("concept-button");
            add(wLabel, NORTH);
            add(wButtons, SOUTH);
            setCellHorizontalAlignment(wButtons, ALIGN_RIGHT);
            setCellVerticalAlignment(wButtons, ALIGN_BOTTOM);
            reposition(CurvePattern.this.getSize());
            activate();
        }
    }

    /**
     * Bounding box for this panel
     */
    public Rectangle getAbsoluteBBox() {
        int x1 = getAbsoluteLeft();
        int x2 = x1 + getOffsetWidth();
        int y1 = getAbsoluteTop();
        int y2 = y1 + getOffsetHeight();
        return new Rectangle(x1, y1, x2, y2);
    }

    public void createCurve(@NonNull final Position topLeft) {
        final Curve curve = new Curve(this.curveRegistry, this.searchManager);
        final Position realTopLeft = Curve.curveToPanelPosition(topLeft);
        getParentPanel().add(curve.getCanvasState(),
                realTopLeft.getX(), realTopLeft.getY());
        curve.activate();
    }
}
