package org.ontologyengineering.protege.web.client.ui.pattern;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.*;
import lombok.*;
import org.ontologyengineering.protege.web.client.effect.AttributeLayers;
import org.ontologyengineering.protege.web.client.effect.Key;
import org.ontologyengineering.protege.web.client.effect.Painter;
import org.ontologyengineering.protege.web.client.effect.VisualEffect;
import org.ontologyengineering.protege.web.client.util.Position;
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
class Property extends Pattern implements Cloneable {

    // pertaining to either the src or to the subset endpoint
    enum Role { SOURCE, TARGET };

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
    @NonNull final AbsolutePanel panel = new PropertyPanel();
    @NonNull final AbsolutePanel parentPanel;

    @Getter private String idPrefix = "property";

    // State tracking fields
    private boolean isMoving = false;
    private boolean isRenaming = false;
    private Effects visualEffects = new Effects();


    // Widgets

    ButtonBar buttonBar;
    final private Endpoint srcPoint;
    final private Endpoint tgtPoint;

    // a visual hint connecting objects in the property template
    // if you snap to an object, the connection moves.
    // if you complete a connection this should be reset
    //
    // * connection within template
    // * connection between template/canvas
    //
    // this should never be null once onLoad is called
    private Optional<JavaScriptObject> connectionHint = Optional.absent();

    private Optional<Curve> alreadyChosen;
    private Optional<Role> firstSnapped;

    // widgets that are not contained in the template frame necessarily
    final Collection<Widget> freeWidgets;
    // widgets that serve to provide a hint what this template is about
    final Collection<Widget> ghosts;

    final Collection<Endpoint> endpoints;

    private final Position srcTopLeft;
    private final Position tgtTopLeft;

    public Property(@NonNull final String id,
                    @NonNull final CurveRegistry registry,
                    @NonNull final SearchManager searchManager,
                    @NonNull final AbsolutePanel parentPanel) {
        this.core = new Core(id);
        this.core.setSize(new Size(60, 40));

        final int width = this.core.getWidth();
        final int height = this.core.getHeight();

        this.registry = registry;
        this.searchManager = searchManager;
        this.parentPanel = parentPanel;

        this.buttonBar = new ButtonBar();
        this.alreadyChosen = Optional.absent();
        this.firstSnapped = Optional.absent();
        this.endpoints = new HashSet<Endpoint>();

        this.srcTopLeft = new Position(1, 1);
        this.tgtTopLeft = new Position(1 + width + 40, 1);

        final DraggableShape wCurveSource =
                new DraggableRect(width, height, this.core.rounding);
        final DraggableShape wCurveTarget =
                new DraggableRect(width, height, this.core.rounding);
        final DraggableShape wGhostSource =
                new DraggableRect(width, height, this.core.rounding);
        final DraggableShape wGhostTarget =
                new DraggableRect(width, height, this.core.rounding);

        srcPoint = new Endpoint(Role.SOURCE, "_curve_source",
                wCurveSource, wGhostSource, "pink",
                buttonBar.wSource, "red",
                srcTopLeft);
        tgtPoint = new Endpoint(Role.TARGET, "_curve_target",
                wCurveTarget, wGhostTarget, "yellow",
                buttonBar.wTarget, "orange",
                tgtTopLeft);

        endpoints.add(srcPoint);
        endpoints.add(tgtPoint);
        getElement().setClassName("template");

        freeWidgets = Arrays.<Widget>asList(wCurveSource, wCurveTarget);
        ghosts = Arrays.<Widget>asList(wGhostSource, wGhostTarget);
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
        // not meant to be selectable, just provides a visual
        // hint to the existence of this object
        @NonNull final private DraggableShape ghost;

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
                        @NonNull DraggableShape ghost,
                        @NonNull String color,
                        @NonNull TextBox searchBox,
                        @NonNull String searchColor,
                        @NonNull Position topLeft) {
            this.role = role;
            this.idSuffix = idSuffix;
            this.curve = curve;
            this.ghost = ghost;
            this.color = color;
            this.searchBox = searchBox;
            this.searchColor = searchColor;
            this.searchHandler = searchManager.makeSearchHandler(searchBox, searchColor);
            this.home = topLeft;

        }

        public void onLoad() {
            panel.add(ghost, home.getX(), home.getY());
            ghost.getElement().setId(getGhostId());
            curve.getElement().setId(getCurveId());
            ghost.addStyleName("snap-to-drag-ghost");
            curve.addStyleName("snap-to-drag-curve");
            bind();
            reset();
            makeDraggable("#" + getCurveId());
        }

        public String getCurveId() {
            return Property.this.core.id + this.idSuffix;
        }

        public String getGhostId() {
            return "ghost_" + Property.this.core.id + this.idSuffix;
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
            Property.this.visualEffects.clear();
            Property.this.visualEffects.addActiveCurve(this);
        }

        // helper for snapIfUniqueMatch
        private void snapToMatch(@NonNull final Curve match) {
            final Property prop = Property.this;
            curve.setVisible(false);
            searchBox.setText(match.getLabel().or("<UNNAMED>"));
            searchBox.setEnabled(false);
            this.iri = match.getIri();
            if (! prop.firstSnapped.isPresent()) {
                prop.alreadyChosen = Optional.of(match);
                prop.firstSnapped = Optional.of(this.role);
                switch (this.role) {
                    case TARGET:
                        prop.setConnectionHintTarget(match.getCurveId());
                        break;
                    case SOURCE:
                        prop.setConnectionHintSource(match.getCurveId());
                        break;
                }
            } else {
                final Role firstRole = firstSnapped.get();
                final Curve chosen = prop.alreadyChosen.get();
                switch (firstRole) {
                    case TARGET:
                        connectPair(match.getCurveId(), chosen.getCurveId());
                        break;
                    case SOURCE:
                        connectPair(chosen.getCurveId(), match.getCurveId());
                        break;
                }
                prop.maybeFinish();
            }
        }


        /**
         * Snap the curve to its (unique) match if there is one
         */
        public void snapIfUniqueMatch() {
            if (candidates.size() == 1) {
                Curve match = candidates.iterator().next();
                if (match.getIri().isPresent()) {
                    snapToMatch(match);
                }
            }
        }

        private Collection<Curve> getSnapCandidates() {
            Collection<Curve> candidates = searchManager.getSnapCandidates(curve);
            // avoid chosing a shape that was already chosen for the other role,
            // ie. srcPoint if we are tgtPoint or vice-versa
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
            Property.this.parentPanel.setWidgetPosition(curve, topleft.getX(), topleft.getY());
            update();
            iri = Optional.absent();
            Property.this.alreadyChosen = Optional.absent();
            Property.this.firstSnapped = Optional.absent();
            curve.setVisible(true);
            searchBox.setEnabled(true);
            searchHandler.reset();

        }

        @Override
        public void onMouseOver(MouseOverEvent event) {
            Property.this.visualEffects.addActiveCurve(this);
            searchBox.setFocus(true);
        }

        @Override
        public void onMouseOut(MouseOutEvent event) {
            Property.this.visualEffects.removeActiveCurve(this);
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
            if (!Property.this.connectionHint.isPresent()) {
                Property.this.resetConnectionHint();
            }
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

    class PropertyPanel extends AbsolutePanel {
        @Override
        public void onLoad() {
            Property property = Property.this;
            this.getElement().setId(property.core.getId());
            super.onLoad();
            this.setPixelSize(Pattern.DEFAULT_TEMPLATE_WIDTH,
                    Pattern.DEFAULT_TEMPLATE_HEIGHT + 40);

            for (Endpoint endpoint : endpoints) {
                parentPanel.add(endpoint.curve);
                endpoint.onLoad();
            }

            final Size sz = property.core.getSize();
            this.add(buttonBar, 1, sz.getHeight() + 10);
            buttonBar.reposition(sz);
            visualEffects.addDefaultEffect(visualEffects.ghostPattern(srcPoint.getGhost()));
            visualEffects.addDefaultEffect(visualEffects.ghostPattern(tgtPoint.getGhost()));
            visualEffects.applyAttributes();
            connectPair(srcPoint.getGhostId(), tgtPoint.getGhostId());
            resetConnectionHint();
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
        final private TextBox wSource = new TextBox();
        final private TextBox wTarget = new TextBox();
        final private Label wSubsumes = new Label("<PROPERTY>");

        final Panel wButtons = new HorizontalPanel();
        final Button wReset = new Button("X");

        private void activate() {
            wReset.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    for (SearchHandler handler : Property.this.endpoints) {
                        handler.reset();
                    }
                }
            });
        }

        private void reposition(@NonNull final Size sz) {
            setHeight(sz.getHeight() + 10 + "px");
        }

        public ButtonBar() {
            wTarget.setWidth("6em");
            wSource.setWidth("6em");

            wButtons.getElement().setClassName("property-button");
            wButtons.add(wReset);
            add(wSource, NORTH);
            add(wSubsumes, NORTH);
            add(wTarget, NORTH);
            add(wButtons, SOUTH);
            setCellHorizontalAlignment(wButtons, ALIGN_RIGHT);
            setCellVerticalAlignment(wButtons, ALIGN_BOTTOM);
            reposition(Property.this.core.getSize());
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
        // tricky to mark the target curve as having been selected
        // (on second thought, another approach might have been just
        // to raise the z index on that one?)
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private Set<Endpoint> activeCurves = new HashSet();

        @NonNull
        private VisualEffect activePattern(@NonNull final DraggableShape curve,
                                           @NonNull final String color) {
            VisualEffect effect = new VisualEffect("property template hover (" + color + ")");
            effect.setAttribute(curve, "stroke", color, "black");
            effect.setAttribute(curve, "stroke-width", "3", "1");
            return effect;
        }

        @NonNull
        private VisualEffect ghostPattern(@NonNull final DraggableShape curve) {
            VisualEffect effect = new VisualEffect("property template ghost");
            effect.setAttribute(curve, "stroke", "gray", "gray");
            effect.setAttribute(curve, "stroke-dasharray", ".", ".");
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
                    if (freeWidgets.contains(obj) || ghosts.contains(obj)) {
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
            if (activeCurves.contains(tgtPoint)) {
                return Optional.of(tgtPoint);
            } else if (activeCurves.contains(srcPoint)) {
                return Optional.of(srcPoint);
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

    private void removeConnectionHint() {
        if (this.connectionHint.isPresent()) {
            disconnect(this.connectionHint.get());
            this.connectionHint = Optional.absent();
        }
    }

    private void resetConnectionHint() {
        removeConnectionHint();
        this.connectionHint = Optional.of(connectPair(
                this.srcPoint.getCurveId(),
                this.tgtPoint.getCurveId()));
        this.repaintEverything();
    }

    private void setConnectionHintSource(@NonNull final String source) {
        removeConnectionHint();
        this.connectionHint = Optional.of(connectPair(
                source,
                this.tgtPoint.getCurveId()));
    }

    private void setConnectionHintTarget(@NonNull final String target) {
        removeConnectionHint();
        this.connectionHint = Optional.of(connectPair(
                this.srcPoint.getCurveId(),
                target));
    }

    public void maybeFinish() {
        if (srcPoint.iri.isPresent() && tgtPoint.iri.isPresent()) {

            // TODO actually complete
            for (Endpoint endpoint : endpoints) {
                endpoint.reset();
            }
            // To be restored when the user starts to move the curve again
            // for some reason jsPlumb connects to the position that the
            // endpoint was at before being reset, which looks wrong
            //
            // Only when you start to move the endpoint around again does the
            // curve snap back into the right place. So for now, we make do
            // with removing the curve altogether and putting it back as soon
            // the user starts to move the endpoints again :-(
            Property.this.removeConnectionHint();
        }
    }

    private Position relativeToParent(Position local) {
        return new Position(
                1 + local.getX() + parentPanel.getWidgetLeft(this.panel),
                1 + local.getY() + parentPanel.getWidgetTop(this.panel));
    }



    private native void makeDraggable(String draggableId) /*-{
        $wnd.make_draggable(draggableId);
        }-*/;

    private native JavaScriptObject connectPair(String source, String target) /*-{
        return $wnd.connect_pair(source,target);
        }-*/;

    private native void disconnect(JavaScriptObject connection) /*-{
        $wnd.disconnect(connection);
        }-*/;

    private native void repaintEverything() /*-{
        $wnd.repaint_everything();
        }-*/;
}
