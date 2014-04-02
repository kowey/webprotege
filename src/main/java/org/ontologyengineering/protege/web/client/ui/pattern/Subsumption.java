package org.ontologyengineering.protege.web.client.ui.pattern;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.*;
import lombok.*;
import org.ontologyengineering.protege.web.client.effect.AttributeLayers;
import org.ontologyengineering.protege.web.client.effect.Key;
import org.ontologyengineering.protege.web.client.effect.Painter;
import org.ontologyengineering.protege.web.client.effect.VisualEffect;
import org.ontologyengineering.protege.web.client.util.Position;
import org.ontologyengineering.protege.web.client.util.Scale;
import org.ontologyengineering.protege.web.client.util.Size;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.CurveRegistry;
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
@ToString
class Subsumption extends Pattern implements Cloneable {

    // pertaining to either the superset or to the subset endpoint
    enum Role { SUPER, SUB };

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------


    @RequiredArgsConstructor
    @Getter
    class Core extends PatternCore {
        @NonNull final private String id;
        private int rounding = 20;
    }

    @NonNull final Core core;
    @NonNull final CurveRegistry registry;
    @NonNull final SearchManager searchManager;
    @NonNull final AbsolutePanel panel = new SubsumptionPanel();
    @NonNull final AbsolutePanel parentPanel;

    @Getter private String idPrefix = "subsumes";

    // State tracking fields
    private boolean isMoving = false;
    private boolean isRenaming = false;
    private Effects visualEffects = new Effects();


    // Widgets

    ButtonBar buttonBar;
    final private Endpoint superset;
    final private Endpoint subset;

    private Optional<Curve> alreadyChosen;
    private Optional<Role> firstSnapped;

    // widgets that are not contained in the template frame necessarily
    final Collection<Widget> freeWidgets;
    final Collection<Endpoint> endpoints;

    private final Position supersetTopLeft;
    private final Position subsetTopLeft;

    public Subsumption(@NonNull final String id,
                       @NonNull final CurveRegistry registry,
                       @NonNull final SearchManager searchManager,
                       @NonNull final AbsolutePanel parentPanel) {
        this.core = new Core(id);

        final int width = this.core.getWidth();
        final int height = this.core.getHeight();

        this.registry = registry;
        this.searchManager = searchManager;
        this.parentPanel = parentPanel;

        this.buttonBar = new ButtonBar();
        this.alreadyChosen = Optional.absent();
        this.firstSnapped = Optional.absent();
        this.endpoints = new HashSet<Endpoint>();

        this.supersetTopLeft = new Position(1, 1);
        this.subsetTopLeft = new Position(1 + width / 3, 1 + height / 3);

        final DraggableShape wCurveOuter =
                new DraggableRect(width, height, this.core.rounding);
        final DraggableShape wCurveInner =
                new DraggableRect(width / 2, height / 2, this.core.rounding);

        superset = new Endpoint(Role.SUPER, "_curve_outer",
                wCurveOuter, "green",
                buttonBar.wSuperset, "darkgreen",
                supersetTopLeft);
        subset = new Endpoint(Role.SUB, "_curve_inner",
                wCurveInner, "blue",
                buttonBar.wSubset, "darkblue",
                subsetTopLeft);

        endpoints.add(superset);
        endpoints.add(subset);
        getElement().setClassName("template");

        freeWidgets = Arrays.<Widget>asList(wCurveInner, wCurveOuter);
    }

    // ----------------------------------------------------------------
    // Handlers
    // ----------------------------------------------------------------

    /**
     * What happens when we manipulate one of the template curves (for
     * example, by dragging them out onto the canvas), or their associated
     * search boxes (for example, by typing into them)
     */
    @Getter @Setter
    class Endpoint implements SearchHandler,
            MouseOverHandler, MouseOutHandler,
            MouseUpHandler, MouseDownHandler, MouseMoveHandler,
            KeyUpHandler {

        @NonNull final private Role role;
        @NonNull final private String idSuffix;
        @NonNull final private DraggableShape curve;
        @NonNull final private String color;
        @NonNull final private TextBox searchBox;
        @NonNull final private String searchColor;
        @NonNull final private SearchHandler searchHandler;

        // initial x/y coordinates for the curve (relative to parent)
        final private Position home;

        @NonNull Optional<IRI> iri = Optional.absent();

        // we need to keep track of pre-existing candidates so that
        // we can remove any visual effects we've applied on them once
        // they are no longer candidates
        Collection<Curve> candidates = Collections.emptyList();
        private boolean dragging = false;

        public Endpoint(@NonNull Role role,
                        @NonNull String idSuffix,
                        @NonNull DraggableShape curve,
                        @NonNull String color,
                        @NonNull TextBox searchBox,
                        @NonNull String searchColor,
                        @NonNull Position topLeft) {
            this.role = role;
            this.idSuffix = idSuffix;
            this.curve = curve;
            this.color = color;
            this.searchBox = searchBox;
            this.searchColor = searchColor;
            this.searchHandler = searchManager.makeSearchHandler(searchBox, searchColor);
            this.home = topLeft;
        }

        public void onLoad() {
            curve.getElement().setId(getCurveId());
            curve.addStyleName("snap-to-drag-curve");
            bind();
            reset();
            makeDraggable("#" + getCurveId());
        }

        public String getCurveId() {
            return Subsumption.this.core.id + this.idSuffix;
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
            Subsumption.this.visualEffects.clear();
            Subsumption.this.visualEffects.addActiveCurve(this);
        }

        /**
         * Snap the curve to its (unique) match if there is one
         */
        public void snapIfUniqueMatch() {
            if (candidates.size() == 1) {
                Curve match = candidates.iterator().next();
                if (match.getIri().isPresent()) {
                    curve.setVisible(false);
                    searchBox.setText(match.getLabel().or("<UNNAMED>"));
                    searchBox.setEnabled(false);
                    iri = match.getIri();
                    if (! Subsumption.this.firstSnapped.isPresent()) {
                        Subsumption.this.alreadyChosen = Optional.of(match);
                        Subsumption.this.firstSnapped = Optional.of(this.role);
                    } else {
                        final AbsolutePanel parentPanel = Subsumption.this.parentPanel;
                        final Role firstRole = firstSnapped.get();
                        final Curve chosen = Subsumption.this.alreadyChosen.get();

                        Position topleft = new Position(
                                parentPanel.getWidgetLeft(chosen.getWidget()),
                                parentPanel.getWidgetTop(chosen.getWidget()));

                        Scale scale = new Scale(1, 1);
                        switch (firstRole) {
                            case SUB:
                                scale = new Scale((float)1.2, (float)1.2);
                                topleft = new Position(topleft.getX() - 10 , topleft.getY() - 10);
                                break;
                            case SUPER:
                                scale = new Scale((float)0.8, (float)0.8);
                                topleft = new Position(topleft.getX() + 10, topleft.getY() + 10);
                                break;
                        }
                        Curve other = match.createCurve(parentPanel, topleft.getX(), topleft.getY());
                        other.setSize(scale.transform(chosen.getSize()));
                        Subsumption.this.maybeFinish();
                    }
                }
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
            Position topleft = relativeToParent(home);
            Subsumption.this.parentPanel.setWidgetPosition(curve, topleft.getX(), topleft.getY());
            update();
            iri = Optional.absent();
            Subsumption.this.alreadyChosen = Optional.absent();
            Subsumption.this.firstSnapped = Optional.absent();
            curve.setVisible(true);
            searchBox.setEnabled(true);
            searchHandler.reset();
        }

        @Override
        public void onMouseOver(MouseOverEvent event) {
            Subsumption.this.visualEffects.addActiveCurve(this);
            searchBox.setFocus(true);
        }

        @Override
        public void onMouseOut(MouseOutEvent event) {
            Subsumption.this.visualEffects.removeActiveCurve(this);
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

    class SubsumptionPanel extends AbsolutePanel {

        @Override
        public void onLoad() {
            Subsumption subsumption = Subsumption.this;
            this.getElement().setId(subsumption.core.getId());
            this.setWidth((subsumption.core.getWidth() + 120) + "px");
            this.setHeight((subsumption.core.getHeight() + 10) + "px");
            super.onLoad();

            for (Endpoint endpoint : endpoints) {
                parentPanel.add(endpoint.curve);
                endpoint.onLoad();
            }

            final Size sz = subsumption.core.getSize();
            this.add(buttonBar, sz.getWidth() + 5, 0);
            buttonBar.reposition(sz);
        }
    }

    public Widget getWidget() {
        return panel;
    }

    public Element getElement() {
        return panel.getElement();
    }



    @Getter
    class ButtonBar extends DockPanel {
        final private TextBox wSuperset = new TextBox();
        final private TextBox wSubset = new TextBox();
        final private Label wSubsumes = new Label("SUBSUMES");

        final Panel wButtons = new HorizontalPanel();
        final Button wReset = new Button("X");

        private void activate() {
            wReset.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    for (SearchHandler handler : Subsumption.this.endpoints) {
                        handler.reset();
                    }
                }
            });
        }

        private void reposition(@NonNull final Size sz) {
            setHeight(sz.getHeight() + 10 + "px");
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
            reposition(Subsumption.this.core.getSize());
            activate();
        }
    }

    @Getter
    @RequiredArgsConstructor
    class Effects extends AttributeLayers {

        final Map<DraggableShape, VisualEffect> curveEffects = new HashMap();

        // which template curves are currently being moused over
        // we need to track this because mouse-over events are only
        // propagated to the uppermost curve on the z axis; so if
        // we have one curve inside of another one, it can be
        // tricky to mark the inner curve as having been selected
        // (on second thought, another approach might have been just
        // to raise the z index on that one?)
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private Set<Endpoint> activeCurves = new HashSet();

        @NonNull
        private VisualEffect activePattern(@NonNull final DraggableShape curve,
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
                    if (freeWidgets.contains(obj)) {
                        DraggableShape curve = (DraggableShape) obj;
                        curve.attr(attr, value);
                    }
                }
            });
        }

        public void clear() {
            activeCurves.clear();
        }

        public void addActiveCurve(Endpoint endpoint) {
            activeCurves.add(endpoint);
            highlightActiveCurves();
        }

        public void removeActiveCurve(Endpoint endpoint) {
            if (activeCurves.contains(endpoint)) {
                activeCurves.remove(endpoint);
            }
            highlightActiveCurves();
        }

        public Optional<Endpoint> getActiveCurve() {
            if (activeCurves.contains(subset)) {
                return Optional.of(subset);
            } else if (activeCurves.contains(superset)) {
                return Optional.of(superset);
            } else {
                return Optional.absent();
            }
        }

        private void highlightActiveCurves() {
            for (Endpoint endpoint : endpoints) {
                if (getActiveCurve().equals(Optional.of(endpoint))) {
                    VisualEffect effect = activePattern(endpoint.curve, endpoint.searchColor);
                    setEffect(endpoint.curve, Optional.of(effect));
                } else {
                    setEffect(endpoint.curve, Optional.<VisualEffect>absent());
                }
            }
            applyAttributes();
        }
    }

    public void maybeFinish() {
        if (superset.iri.isPresent() && subset.iri.isPresent()) {
            // do the actual move
            IRI cls = subset.iri.get();
            IRI newParent = superset.iri.get();
            IRI oldParent = registry.getImmediateParent(cls);
            registry.moveClass(cls, oldParent, newParent);
            for (Endpoint endpoint : endpoints) {
                endpoint.reset();
            }
        }
    }

    private Position relativeToParent(Position local) {
        return new Position(
                local.getX() + parentPanel.getWidgetLeft(this.panel),
                local.getY() + parentPanel.getWidgetTop(this.panel));
    }



    private native void makeDraggable(String draggableId) /*-{
        $wnd.make_draggable(draggableId);
        }-*/;
}
