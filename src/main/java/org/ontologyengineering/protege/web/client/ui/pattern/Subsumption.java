package org.ontologyengineering.protege.web.client.ui.pattern;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;
import lombok.*;
import org.ontologyengineering.protege.web.client.effect.AttributeLayers;
import org.ontologyengineering.protege.web.client.effect.Key;
import org.ontologyengineering.protege.web.client.effect.Painter;
import org.ontologyengineering.protege.web.client.effect.VisualEffect;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.SearchManager;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.SearchManager.SearchHandler;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableRect;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableShape;
import org.semanticweb.owlapi.model.IRI;

import java.util.*;

/**
 * Pattern to express that one class subsumes another.
 * This is represented as one curve enclosing another.
 * The basic idea to drag a curve out from the pattern until it uniquely
 * snaps to one of the curves on the canvas. You can speed the process
 * along by typing in a search box to narrow the possible matching curves.
 *
 * You can then select the remaining curve in the same way
 */
public
// We would just use @Data but @EqualsAndHashCode is incompatible with GWT
// https://code.google.com/p/projectlombok/issues/detail?id=414
// because the GWT compiler does not support '$' in variable names
@Getter @Setter @RequiredArgsConstructor @ToString
class Subsumption extends Pattern implements Cloneable {

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------

    @NonNull final String id;
    @NonNull final SearchManager searchManager;
    @NonNull final AbsolutePanel parentPanel;

    @NonNull final String COLOR_SUBSET = "blue";
    @NonNull final String COLOR_SUPERSET = "green";
    @NonNull final String COLOR_SUBSET_SEARCH = "darkblue";
    @NonNull final String COLOR_SUPERSET_SEARCH = "darkgreen";


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


    // which template curves are currently being moused over
    // we need to track this because mouse-over events are only
    // propagated to the uppermost curve on the z axis; so if
    // we have one curve inside of another one, it can be
    // tricky to mark the inner curve as having been selected
    // (on second thought, another approach might have been just
    // to raise the z index on that one?)
    @Getter(AccessLevel.NONE)  @Setter(AccessLevel.NONE)
    private Set<DraggableShape> activeCurves = new HashSet();

    //
    private Optional<Curve> alreadyChosen = Optional.absent();

    final DraggableShape wCurveOuter = new DraggableRect(this.width, this.height, this.rounding);
    final DraggableShape wCurveInner = new DraggableRect(this.width / 2, this.height / 2, this.rounding);
    // widgets that are not contained in the template frame necessarily
    final Collection<Widget> freeWidgets = Arrays.<Widget>asList(wCurveInner, wCurveOuter);

    // ----------------------------------------------------------------
    // Handlers
    // ----------------------------------------------------------------

    /**
     * What happens when we manipulate one of the template curves (for
     * example, by dragging them out onto the canvas), or their associated
     * search boxes (for example, by typing into them)
     */
    @Getter @Setter
    class CurveMatchHandler implements SearchHandler,
            MouseOverHandler, MouseOutHandler,
            MouseUpHandler, MouseDownHandler, MouseMoveHandler,
            KeyUpHandler {

        @NonNull final private DraggableShape curve;
        @NonNull final private String color;
        @NonNull final private TextBox searchBox;
        @NonNull final private String searchColor;
        @NonNull final private SearchHandler searchHandler;

        // initial x/y coordinates for the curve (relative to parent)
        final private int homeX;
        final private int homeY;

        // we need to keep track of pre-existing candidates so that
        // we can remove any visual effects we've applied on them once
        // they are no longer candidates
        Collection<Curve> candidates = Collections.emptyList();
        private boolean dragging = false;

        public CurveMatchHandler(@NonNull DraggableShape curve,
                                 @NonNull String color,
                                 @NonNull TextBox searchBox,
                                 @NonNull String searchColor) {
            this.curve = curve;
            this.color = color;
            this.searchBox = searchBox;
            this.searchColor = searchColor;
            this.searchHandler = searchManager.makeSearchHandler(searchBox, searchColor);
            this.homeX = Subsumption.this.parentPanel.getWidgetLeft(curve);
            this.homeY = Subsumption.this.parentPanel.getWidgetTop(curve);
        }

        public Optional<Collection<Curve>> getMatching() {
            return Optional.of(candidates);
        }

        /**
         * Forcibly "awaken" the current curve; normally a curve is activated by
         * hovering over it, but if we start typing things in the search box, it'd
         * be a good idea to light the curve up a form of feedback
         */
        public void forceActive() {
            Subsumption.this.activeCurves.clear();
            Subsumption.this.activeCurves.add(curve);
            Subsumption.this.highlightActiveCurves();
        }

        /**
         * Snap the curve to its (unique) match if there is one
         */
        public void snapIfUniqueMatch() {
            if (candidates.size() == 1) {
                Curve match = candidates.iterator().next();
                alreadyChosen = Optional.of(match);
                curve.setVisible(false);
                searchBox.setText(match.getLabel().or("<UNNAMED>"));
                searchBox.setEnabled(false);
            }
        }

        private Collection<Curve> getSnapCandidates() {
            Collection<Curve> candidates = searchManager.getSnapCandidates(curve);
            // avoid chosing a shape that was already chosen for the other role,
            // ie. superset if we are subset or vice-versa
            if (alreadyChosen.isPresent()) {
                candidates.remove(alreadyChosen.get());
            }
            // narrow the matching to things which have been preselected in the search
            // box (if applicable)
            if (searchHandler.getMatching().isPresent()) {
                final Collection<Curve> searchBoxMatching = searchHandler.getMatching().get();

                candidates = Collections2.filter(candidates, new Predicate<Curve>() {
                    @Override
                    public boolean apply(@NonNull Curve curve) {
                        return searchBoxMatching.contains(curve);
                    }
                });
            }
            return candidates;
        }

        /**
         * Stay abreast of changes to our snap candidates:
         * Clear any stale drag-to-snap visual effects from old candidates,
         * figure out the ones and apply effects accordingly
         */
        public void update() {
            Collection<Curve> newCandidates = getSnapCandidates();

            for (Curve oldCandidate : candidates) {
                oldCandidate.getEffects().applyDragSnapEffect(curve, Optional.<VisualEffect>absent());
            }

            if (newCandidates.size() == 1) {
                Curve.Effects effects = newCandidates.iterator().next().getEffects();
                effects.applyDragSnapEffect(curve, Optional.of(effects.dragSnapUnique(color)));
            } else {
                for (Curve newCandidate : newCandidates) {
                    Curve.Effects effects = newCandidate.getEffects();
                    effects.applyDragSnapEffect(curve, Optional.of(effects.dragSnapPartial(color)));
                }
            }
            candidates = newCandidates;
        }

        /**
         * Clear the current search and apply visual effects as appropriate
         */
        public void reset() {
            Subsumption.this.parentPanel.setWidgetPosition(curve, homeX, homeY);
            update();
            alreadyChosen = Optional.absent();
            curve.setVisible(true);
            searchBox.setEnabled(true);
            searchHandler.reset();
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
        public void onMouseMove(MouseMoveEvent event) {
            if (isDragging()) {
                update();
            }
        }

        @Override
        public void onMouseDown(MouseDownEvent event) {
            setDragging(true);
        }

        @Override
        public void onMouseUp(MouseUpEvent event) {
            setDragging(false);
            snapIfUniqueMatch();
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
            curve.addDomHandler(this, MouseMoveEvent.getType());
            searchHandler.bind();
            searchBox.addDomHandler(this, KeyUpEvent.getType());
        }
    }


    @Getter
    class ButtonBar extends DockPanel {
        final private TextBox wSuperset = new TextBox();
        final private TextBox wSubset = new TextBox();
        final private Label wSubsumes = new Label("SUBSUMES");

        final Panel wButtons = new HorizontalPanel();
        final Button wReset = new Button("X");

        final Collection<SearchHandler> searchHandlers = new LinkedList();

        private void activate() {
            wReset.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    for (SearchHandler handler : searchHandlers) {
                        handler.reset();
                    }
                }
            });
        }

        private void reposition(int curveWidth, int curveHeight) {
            setHeight(curveHeight + 10 + "px");
        }

        public ButtonBar() {
            wSubset.setWidth("6em");
            wSuperset.setWidth("6em");

            wButtons.getElement().setClassName("subsumption-button");
            wButtons.add(wReset);
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

    @Getter
    @RequiredArgsConstructor
    class Effects extends AttributeLayers {

        final Map<DraggableShape,VisualEffect> curveEffects = new HashMap();

        @NonNull private VisualEffect activePattern(@NonNull final DraggableShape curve,
                                                    @NonNull final String color) {
            VisualEffect effect = new VisualEffect("subsumption template hover (" + color + ")");
            effect.setAttribute(curve, "stroke", color, "black");
            effect.setAttribute(curve, "stroke-width", "3", "1");
            return effect;
        }

        public void setEffect(@NonNull final DraggableShape curve,
                              @NonNull final Optional<VisualEffect> newEffect) {
            setContextEffect(curveEffects, curve, newEffect);
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

        Collection<SearchHandler> searchHandlers = buttonBar.getSearchHandlers();
        searchHandlers.add(new CurveMatchHandler(
                wCurveOuter, COLOR_SUPERSET,
                buttonBar.wSuperset, COLOR_SUPERSET_SEARCH));
        searchHandlers.add(new CurveMatchHandler(
                wCurveInner, COLOR_SUBSET,
                buttonBar.wSubset, COLOR_SUBSET_SEARCH));
        for (SearchHandler handler : searchHandlers) {
            handler.bind();
        }
    }

    public Subsumption copyTemplate(@NonNull final AbsolutePanel container) {

        Subsumption copy  = new Subsumption(makeId(), searchManager, parentPanel);
        container.add(copy, container.getWidgetLeft(this), container.getWidgetTop(this));
        copy.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
        copy.getElement().setClassName("template");
        //TemplateHandler.addHandler(container, this, copy, counter);
        makeDraggable("#" + copy.getCurveIdOuter());
        makeDraggable("#" + copy.getCurveIdInner());

        return copy;
    }

    public void switchToInstanceMode() {
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
