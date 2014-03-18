package org.ontologyengineering.protege.web.client.ui.pattern;

import com.google.common.base.Optional;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;
import lombok.*;
import org.ontologyengineering.protege.web.client.effect.AttributeLayers;
import org.ontologyengineering.protege.web.client.effect.Key;
import org.ontologyengineering.protege.web.client.effect.Painter;
import org.ontologyengineering.protege.web.client.effect.VisualEffect;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.ConceptDiagramPortlet;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.SearchManager;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableRect;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableShape;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.TemplateHandler;
import org.openrdf.query.algebra.Sum;
import org.semanticweb.owlapi.model.IRI;

import java.util.*;

/**
 * Created by kowey on 2014-02-03.
 */
public
// We would just use @Data but @EqualsAndHashCode is incompatible with GWT
// https://code.google.com/p/projectlombok/issues/detail?id=414
// because the GWT compiler does not support '$' in variable names
@Getter @Setter @RequiredArgsConstructor @ToString
class Subsumption extends Pattern implements Cloneable,
        MouseOverHandler, MouseOutHandler, MouseUpHandler, MouseDownHandler, MouseMoveHandler {

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------

    @NonNull final String id;
    @NonNull final SearchManager searchManager;
    @NonNull final AbsolutePanel parentPanel;

    @NonNull final String COLOR_SUBSET = "blue";
    @NonNull final String COLOR_SUPERSET = "green";

    @Getter private String idPrefix = "subsumes";

    // State tracking fields

    @Setter(AccessLevel.PACKAGE) @NonNull Optional<IRI> iriSrc = Optional.absent();
    @Setter(AccessLevel.PACKAGE) @NonNull Optional<IRI> iriTgt = Optional.absent();

    private boolean isMoving = false;
    private boolean isRenaming = false;
    private Effects visualEffects = new Effects();

    private int rounding = 20;

    // Widgets

    ButtonBar buttonBar = new ButtonBar();

    @Getter(AccessLevel.NONE)  @Setter(AccessLevel.NONE)
    private Set<DraggableShape> activeCurves = new HashSet();

    final DraggableShape wCurveOuter = new DraggableRect(this.width, this.height, this.rounding);
    final DraggableShape wCurveInner = new DraggableRect(this.width / 2, this.height / 2, this.rounding);
    // widgets that are not contained in the template frame necessarily
    final Collection<Widget> freeWidgets = Arrays.<Widget>asList(wCurveInner, wCurveOuter);

    // ----------------------------------------------------------------
    // Handlers
    // ----------------------------------------------------------------

    @RequiredArgsConstructor
    class RenameHandler implements KeyUpHandler {
        @Override
        public void onKeyUp(KeyUpEvent event) {
        }
    }

    @RequiredArgsConstructor
    class DeleteHandler implements ClickHandler {
        @Override
        public void onClick(ClickEvent event) {
        }
    }

    @RequiredArgsConstructor
    class CurveActivationHandler implements MouseOverHandler, MouseOutHandler,
            MouseUpHandler, MouseDownHandler,
            KeyUpHandler {
        final DraggableShape curve;
        final TextBox searchBox;

        public void forceActive() {
            Subsumption.this.activeCurves.clear();
            Subsumption.this.activeCurves.add(curve);
            Subsumption.this.highlightActiveCurves();
        }

        @Override
        public void onMouseOver(MouseOverEvent event) {
            Subsumption.this.activeCurves.add(curve);
            Subsumption.this.highlightActiveCurves();
            searchBox.setFocus(true);
        }

        @Override
        public void onMouseOut(MouseOutEvent event) {
            if (Subsumption.this.activeCurves.contains(curve)) {
                Subsumption.this.activeCurves.remove(curve);
            }
            Subsumption.this.highlightActiveCurves();
        }

        @Override
        public void onMouseUp(MouseUpEvent event) {
            GWT.log("[SUBSUMPTION] done seeking " + curve.getElement().getId());
            List<Concept> candidates = searchManager.getSnapCandidates(curve);
            if (! candidates.isEmpty()) {
                Concept match = candidates.get(0);
                GWT.log("[SUBSUMPTION] match found! " + curve.getElement().getId() + " with " + match.getLabel());
            }

        }

        @Override
        public void onMouseDown(MouseDownEvent event) {
            GWT.log("[SUBSUMPTION] start seeking " + curve.getElement().getId());

        }

        @Override
        public void onKeyUp(KeyUpEvent event) {
            forceActive();
        }

        /**
         * Set as the mouse over/out handlers for the given curve
         */
        public void bind() {
            curve.addDomHandler(this, MouseOverEvent.getType());
            curve.addDomHandler(this, MouseOutEvent.getType());
            curve.addDomHandler(this, MouseUpEvent.getType());
            curve.addDomHandler(this, MouseDownEvent.getType());
            searchBox.addDomHandler(this, KeyUpEvent.getType());
        }
    }



    @Data class ButtonBar extends DockPanel {
        final private TextBox wSuperset = new TextBox();
        final private TextBox wSubset = new TextBox();
        final private Label wSubsumes = new Label("SUBSUMES");

        final Panel wButtons = new HorizontalPanel();
        final Button wDelete = new Button("X");

        private void activate() {
            //searchManager.makeSearchHandler(wSuperset, "green");
            //.bind();
            //searchManager.makeSearchHandler(wSubset, "orange").bind();

            /*
            this.wLabel.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    canvasState.setRenaming(true);
                }
            });
            this.wLabel.addKeyUpHandler(new RenameHandler(wLabel));
            this.wLabel.setReadOnly(true);

            wDelete.addClickHandler(new DeleteHandler());
            wResize.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    canvasState.prepareForResizing();
                }
            });
            wFun.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    setMatchStatus(matchStatus.getNext());
                }
            });
            */
        }

        private void reposition(int curveWidth, int curveHeight) {
            setHeight(curveHeight + 10 + "px");
        }

        public ButtonBar() {
            wSubset.setWidth("6em");
            wSuperset.setWidth("6em");

            wButtons.getElement().setClassName("subsumption-button");
            wButtons.add(wDelete);
            add(wSuperset, NORTH);
            add(wSubsumes, NORTH);
            add(wSubset, NORTH);
            add(wButtons, SOUTH);
            setCellHorizontalAlignment(wButtons, ALIGN_RIGHT);
            setCellVerticalAlignment(wButtons, ALIGN_BOTTOM);
            reposition(Subsumption.this.width, Subsumption.this.height);
            activate();
        }
    }

    @Data class Effects extends AttributeLayers {

        final Map<DraggableShape,VisualEffect> curveEffects = new HashMap();

        @NonNull private VisualEffect activePattern(@NonNull final DraggableShape curve,
                                                    @NonNull final String color) {
            VisualEffect effect = new VisualEffect();
            effect.setAttribute(curve, "stroke", color, "black");
            effect.setAttribute(curve, "fill", color, "white");
            effect.setAttribute(curve, "opacity", "0.5", "1");
            effect.setAttribute(curve, "stroke-width", "2", "1");
            return effect;
        }

        public void setEffect(@NonNull final DraggableShape curve,
                              @NonNull final Optional<VisualEffect> newEffect) {
            if (curveEffects.containsKey(curve)) {
                removeEffect(curveEffects.get(curve));
            }
            if (newEffect.isPresent()) {
                addEffect(newEffect.get());
                curveEffects.put(curve, newEffect.get());
            }
        }

        private void applyAttributes() {
            applyAttributes(new Painter() {
                @Override
                public void apply(Key key, String value) {
                    final Object obj = key.getObject();
                    final String attr = key.getAttribute();
                    if (curveEffects.containsKey(obj)) {
                        DraggableShape curve = (DraggableShape)obj;
                        curve.attr(attr, value);
                    }
                }
            });
        }

    }



    public String getCurveIdOuter() {
        return this.id + "_curve_outer";
    }

    public String getCurveIdInner() {
        return this.id + "_curve_inner";
    }

    private Optional<DraggableShape> getActiveCurve() {
        if (activeCurves.contains(wCurveInner)) {
            return Optional.of(wCurveInner);
        } else if (activeCurves.contains(wCurveOuter)) {
            return Optional.of(wCurveOuter);
        } else {
            return Optional.absent();
        }
    }

    private void highlightIfActive(DraggableShape curve, String color) {
        if (getActiveCurve().equals(Optional.of(curve))) {
            VisualEffect effect = visualEffects.activePattern(curve, color);
            visualEffects.setEffect(curve, Optional.of(effect));
        } else {
            visualEffects.setEffect(curve, Optional.<VisualEffect>absent());
        }
        visualEffects.applyAttributes();
    }

    private void highlightActiveCurves() {
        highlightIfActive(wCurveInner, COLOR_SUBSET);
        highlightIfActive(wCurveOuter, COLOR_SUPERSET);
    }

    private void addToParent(Widget widget, int relativeX, int relativeY) {
        int parentRelativeX = relativeX + parentPanel.getWidgetLeft(this);
        int parentRelativeY = relativeY + parentPanel.getWidgetTop(this);
        parentPanel.add(widget, parentRelativeX, parentRelativeY);
    }

    @Override
    public void onLoad() {
        this.getElement().setId(this.id);
        this.setWidth((this.width + 120) + "px");
        this.setHeight((this.height + 10) + "px");
        super.onLoad();


        wCurveInner.getElement().setId(getCurveIdInner());
        wCurveOuter.getElement().setId(getCurveIdOuter());

        addToParent(wCurveOuter, 1, 1);
        addToParent(wCurveInner, 1 + this.width / 3, 1 + this.height / 3);
        for (Widget widget : freeWidgets) {
            widget.addStyleName("snap-to-drag-curve");
        }

        this.add(buttonBar, width + 5, 0);
        buttonBar.reposition(width, height);

        new CurveActivationHandler(wCurveOuter, buttonBar.wSuperset).bind();
        new CurveActivationHandler(wCurveInner, buttonBar.wSubset).bind();

        searchManager.makeSearchHandler(buttonBar.wSuperset, COLOR_SUPERSET).bind();
        searchManager.makeSearchHandler(buttonBar.wSubset, COLOR_SUBSET).bind();
    }


    public Subsumption copyTemplate(@NonNull final AbsolutePanel container,
                                 final int counter) {

        Subsumption copy  = new Subsumption(idPrefix + counter, searchManager, parentPanel);
        container.add(copy, container.getWidgetLeft(this), container.getWidgetTop(this));
        copy.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
        copy.getElement().setClassName("template");
        //TemplateHandler.addHandler(container, this, copy, counter);
        makeDraggable("#" + copy.getCurveIdOuter());
        makeDraggable("#" + copy.getCurveIdInner());

        return copy;
    }

    public void mouseOverHighlight() {
    }

    @Override
    public void onMouseOver(MouseOverEvent event) {

    }

    @Override
    public void onMouseOut(MouseOutEvent event) {

    }



    @Override
    public void onMouseUp(MouseUpEvent event) {
        this.isMoving = false;
    }

    @Override
    public void onMouseDown(MouseDownEvent event) {
        this.isMoving = true;
    }

    @Override
    public void onMouseMove(MouseMoveEvent event) {
        if (this.isMoving) {
            this.onMouseOut(null);
        }
    }

    /**
     * Remove and unregister this concept
     */
    public void delete() {
        removeFromParent();
    }

    public void switchToInstanceMode() {


        addDomHandler(this, MouseOverEvent.getType());
        addDomHandler(this, MouseOutEvent.getType());
        addDomHandler(this, MouseUpEvent.getType());
        addDomHandler(this, MouseDownEvent.getType());
        addDomHandler(this, MouseMoveEvent.getType());
        mouseOverHighlight();
    }

    /**
     * Note: you should only ever call this once
     */
    public void startTemplateMode() {
        this.getElement().getStyle().setVisibility(Style.Visibility.HIDDEN);
        for (Widget widget : freeWidgets) {
            widget.getElement().getStyle().setVisibility(Style.Visibility.HIDDEN);
        }
        this.getElement().setClassName("template");
    }

    private native void makeDraggable(String draggableId) /*-{
        $wnd.make_draggable(draggableId);
        }-*/;
}
