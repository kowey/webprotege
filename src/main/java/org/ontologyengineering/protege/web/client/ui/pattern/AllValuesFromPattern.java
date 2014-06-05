package org.ontologyengineering.protege.web.client.ui.pattern;

import com.google.common.base.Optional;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.*;
import com.hydro4ge.raphaelgwt.client.Raphael;
import lombok.*;
import org.ontologyengineering.protege.web.client.effect.VisualEffect;
import org.ontologyengineering.protege.web.client.ui.curve.Curve;
import org.ontologyengineering.protege.web.client.ui.dragsnap.DragSnapEndpoint;
import org.ontologyengineering.protege.web.client.ui.dragsnap.Effects;
import org.ontologyengineering.protege.web.client.ui.dragsnap.Endpoint;
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
 * Pattern to express that two classes are related by some property.
 * We indicate this with a labeled arrow between the two classes.
 * See the concept diagram manual for implied restrictions on the
 * domain and range.
 */
public
// We would just use @Data but @EqualsAndHashCode is incompatible with GWT
// https://code.google.com/p/projectlombok/issues/detail?id=414
// because the GWT compiler does not support '$' in variable names
@ToString
class AllValuesFromPattern extends Pattern implements Cloneable {

    // pertaining to either the src or to the subset endpoint
    enum Role { SOURCE, TARGET };

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------

    static int labelCounter = 0;


    private Effects visualEffects;

    // Widgets

    ButtonBar buttonBar;
    final private DragSnapEndpoint srcPoint;
    final private TargetPropertyEndpoint tgtPoint;

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
    final Collection<DragSnapEndpoint> endpoints;



    public AllValuesFromPattern(@NonNull final String id,
                                @NonNull final CurveRegistry registry,
                                @NonNull final SearchManager searchManager,
                                @NonNull final AbsolutePanel parentPanel) {
        super(id, registry, searchManager, parentPanel);
        this.setSize(new Size(60, 40));

        final int width = this.getWidth();
        final int height = this.getHeight();

        this.buttonBar = new ButtonBar();
        this.alreadyChosen = Optional.absent();
        this.firstSnapped = Optional.absent();
        this.endpoints = new HashSet<DragSnapEndpoint>();

        final Position srcTopLeft = new Position(3, 12);
        final Position tgtTopLeft = new Position(3 + width + 60, 12);

        final DraggableRect wCurveSource =
                new DraggableRect(width, height, rounding);
        final DraggableRect wCurveTarget =
                new DraggableRect(width, height, rounding);

        final DraggableRect wGhostSource =
                new DraggableRect(width, height, rounding);
        final DraggableRect wGhostTarget =
                new DraggableRect(width, height, rounding);


        visualEffects = new PropertyEffects(Arrays.<Widget>asList(
                wCurveSource, wCurveTarget,
                wGhostSource, wGhostTarget));

        srcPoint = new PropertyEndpoint(Role.SOURCE, "_curve_source",
                wCurveSource, wGhostSource, "pink",
                buttonBar.wSource, "red",
                srcTopLeft);
        tgtPoint = new TargetPropertyEndpoint(Role.TARGET, "_curve_target",
                wCurveTarget, wGhostTarget, "yellow",
                buttonBar.wTarget, "orange",
                tgtTopLeft);

        endpoints.add(srcPoint);
        endpoints.add(tgtPoint);
    }

    // ----------------------------------------------------------------
    // Handlers
    // ----------------------------------------------------------------

    class PropertyEndpoint extends DragSnapEndpoint {
        final private Role role;

        public PropertyEndpoint(@NonNull Role role,
                                @NonNull String idSuffix,
                                @NonNull DraggableShape curve,
                                @NonNull DraggableShape ghost,
                                @NonNull String color,
                                @NonNull TextBox searchBox,
                                @NonNull String searchColor,
                                @NonNull Position topLeft) {
            super(AllValuesFromPattern.this.searchManager,
                  AllValuesFromPattern.this.visualEffects,
                  AllValuesFromPattern.this.getId() + idSuffix,
                  curve, ghost, color, searchBox, searchColor,
                  topLeft);
            this.role = role;
        }

        protected Collection<Curve> getAlreadyChosen() {
            return AllValuesFromPattern.this.alreadyChosen.asSet();
        }

        protected void resetSnapChoices() {
            AllValuesFromPattern.this.alreadyChosen = Optional.absent();
            AllValuesFromPattern.this.firstSnapped = Optional.absent();
            AllValuesFromPattern.this.buttonBar.getWProperty().setText("");
            removeConnectionHint();
        }

        protected void withdrawCurve() {
            final Position topLeft = relativeToParent(getHome());
            getParentPanel().setWidgetPosition(getCurve(), topLeft.getX(), topLeft.getY());
            getParentPanel().setWidgetPosition(getGhost(), topLeft.getX(), topLeft.getY());
        }

        @Override
        public void onLoad(@NonNull final AbsolutePanel container) {
            // put this endpoint in its own universe (box)
            // to avoid implying disjunction
            final int boxWidth = Math.max(width, height) + 8;
            final DraggableRect wUniverse =
                    new DraggableRect(boxWidth, boxWidth, 0);
            final Position boxPosition =
                    relativeToParent(home).add(new Position(-5, -10));
            container.add(wUniverse, boxPosition.getX(), boxPosition.getY());
            //
            getGhost().addStyleName("dragsnap-ghost");
            getCurve().addStyleName("dragsnap-curve");
            super.onLoad(container);
            makeDraggable("#" + getCurveId());
        }

        @Override
        public void onMouseDown(MouseDownEvent event) {
            super.onMouseDown(event);
            if (!AllValuesFromPattern.this.connectionHint.isPresent()) {
                AllValuesFromPattern.this.resetConnectionHint();
            }
        }

        // helper for snapIfUniqueMatch
        protected void snapToMatch(@NonNull final Curve match) {
            final AllValuesFromPattern prop = AllValuesFromPattern.this;
            getCurve().setVisible(false);
            getSearchBox().setText(match.getLabel().or("<UNNAMED>"));
            getSearchBox().setEnabled(false);
            setIri(match.getIri());
            if (! prop.firstSnapped.isPresent()) {
                prop.alreadyChosen = Optional.of(match);
                prop.firstSnapped = Optional.of(this.role);
                final boolean isSourceFirst = firstSnapped.get().equals(Role.SOURCE);
                final Optional<String> source = isSourceFirst
                        ? Optional.of(match.getCurveId()) : Optional.<String>absent();
                // there is no connection hint target because we're always
                // pointing to the anonymous curve anyway
                addConnectionHint(source, Optional.<String>absent());
            } else {
                final Curve chosen = prop.alreadyChosen.get();
                final boolean isSourceFirst = firstSnapped.get().equals(Role.SOURCE);

                final Curve source = isSourceFirst ? chosen : match;
                final Curve target = isSourceFirst ? match : chosen;

                // create a new anonymous curve that we actually want to point to
                final AbsolutePanel parentPanel = AllValuesFromPattern.this.getParentPanel();
                final Curve targetAnon = new Curve(curveRegistry, searchManager);
                targetAnon.placeInside(parentPanel, target);

                // we always expect there to be some element, so non-null
                final Element hintTextBox = DOM.getElementById(getConnectionHintId());
                final String hintText = hintTextBox.getAttribute("value");

                final String connectionId = makeConnectionId();
                final TextBox propertyLabelBox = createArrowLabel(connectionId, hintText);
                connectPair(source.getCurveId(), targetAnon.getCurveId(), connectionId);
                // FIXME update (replace) condition whenever this value changes
                propertyLabelBox.addValueChangeHandler(new ValueChangeHandler<String>() {
                    @Override
                    public void onValueChange(ValueChangeEvent<String> event) {
                        createCondition(event.getValue(), source, target);
                    }
                });

                // now the back end elements
                createCondition(hintText, source, target);

                // reset this pattern
                prop.maybeFinish();
            }
        }
    }

    /**
     * Has an anonymous inner curve inside of it that follows it wherever it goes
     */
    class TargetPropertyEndpoint extends PropertyEndpoint {

        @NonNull private final DraggableShape anon;
        @NonNull private final DraggableShape anonGhost;

        final Position ANON_OFFSET = new Position(5, 5);
        final Scale ANON_SCALE = new Scale(0.8, 0.8);

        public TargetPropertyEndpoint(@NonNull Role role,
                                      @NonNull String idSuffix,
                                      @NonNull DraggableRect curve,
                                      @NonNull DraggableRect ghost,
                                      @NonNull String color,
                                      @NonNull TextBox searchBox,
                                      @NonNull String searchColor,
                                      @NonNull Position topLeft) {
            super(role, idSuffix, curve, ghost, color, searchBox, searchColor, topLeft);
            final Size size = ANON_SCALE.transform(
                    new Size(curve.getWidth(), curve.getHeight()));
            anon = new DraggableRect(size.getWidth(), size.getHeight(), rounding);
            anonGhost = new DraggableRect(size.getWidth(), size.getHeight(), rounding);

        }

        @Override
        public void onLoad(@NonNull final AbsolutePanel container) {
            container.add(anon);
            container.add(anonGhost);
            anon.getElement().setId(getAnonId());
            anonGhost.getElement().setId(getAnonGhostId());
            final Effects effects = getEffects();
            effects.addDefaultEffect(effects.ghostPattern(anonGhost));
            super.onLoad(container);
        }

        public DraggableShape getAnonCurve() {
            return anon;
        }

        public DraggableShape getAnonGhost() {
            return anonGhost;
        }

        public String getAnonId() {
            return "anon_" + this.idSuffix;
        }


        public String getAnonGhostId() {
            return "anon_ghost_" + this.idSuffix;
        }

        protected void withdrawCurve() {
            super.withdrawCurve();
            final Position topLeft = relativeToParent(getHome()).add(ANON_OFFSET);
            getParentPanel().setWidgetPosition(anon, topLeft.getX(), topLeft.getY());
            getParentPanel().setWidgetPosition(anonGhost, topLeft.getX(), topLeft.getY());
        }

        @Override
        public void onMouseMove(MouseMoveEvent event) {
            if (isDragging()) {
                final Position topLeft = curve.getRelativeTopLeft().add(ANON_OFFSET);
                getParentPanel().setWidgetPosition(anon, topLeft.getX(), topLeft.getY());
            }
            super.onMouseMove(event);
        }
    }

    class PropertyEffects extends Effects {

        public PropertyEffects(@NonNull final Collection<Widget> widgets) {
            super(widgets);
        }

        // which template curves are currently being moused over
        // we need to track this because mouse-over events are only
        // propagated to the uppermost curve on the z axis; so if
        // we have one curve inside of another one, it can be
        // tricky to mark the target curve as having been selected
        // (on second thought, another approach might have been just
        // to raise the z index on that one?)
        private Set<Endpoint> activeCurves = new HashSet<Endpoint>();

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

        public Optional<DragSnapEndpoint> getActiveCurve() {
            if (activeCurves.contains(tgtPoint)) {
                return Optional.<DragSnapEndpoint>of(tgtPoint);
            } else if (activeCurves.contains(srcPoint)) {
                return Optional.of(srcPoint);
            } else {
                return Optional.absent();
            }
        }

        public void highlightActiveCurves() {
            for (DragSnapEndpoint endpoint : endpoints) {
                Optional<VisualEffect> effect = (getActiveCurve().equals(Optional.of(endpoint)))
                        ? Optional.of(activePattern(endpoint.getCurve(), endpoint.getSearchColor()))
                        : Optional.<VisualEffect>absent();
                setEffect(endpoint.getCurve(), effect);
            }
            applyAttributes();
        }
    }

    @Override
    public void onLoad() {
        this.getElement().setId(getId());
        super.onLoad();
        this.setPixelSize(Pattern.DEFAULT_TEMPLATE_WIDTH,
                Pattern.DEFAULT_TEMPLATE_HEIGHT + 60);

        for (DragSnapEndpoint endpoint : endpoints) {
            endpoint.onLoad(getParentPanel());
        }

        final Size sz = getSize();
        this.add(buttonBar, 1, sz.getHeight() + 40);
        buttonBar.reposition(sz);
        visualEffects.applyAttributes();
        connectPair(srcPoint.getGhostId(), tgtPoint.getAnonGhostId(), null);
    }

    public String getConnectionHintId() {
        return this.getId() + "_hint";
    }

    /**
     * Create and return a brand new connection id
     */
    public String makeConnectionId () {
        this.labelCounter++;
        return this.getId() + "_conn_" + this.labelCounter;
    }

    @Getter
    class ButtonBar extends DockPanel {
        final private TextBox wSource = new TextBox();
        final private TextBox wTarget = new TextBox();
        final private TextBox wProperty = new TextBox();

        final Panel wButtons = new HorizontalPanel();
        final Button wReset = new Button("X");

        private void activate() {
            wReset.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    for (SearchHandler handler : AllValuesFromPattern.this.endpoints) {
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

            wSource.getElement().setAttribute("placeholder", "source");
            wTarget.getElement().setAttribute("placeholder", "target");
            wProperty.getElement().setAttribute("placeholder", "PROPERTY");
            wButtons.getElement().setClassName("property-button");
            wButtons.add(wReset);
            final HorizontalPanel propertyPanel = new HorizontalPanel();
            propertyPanel.add(new Label("⊑ ∀ "));
            propertyPanel.add(wProperty);
            propertyPanel.add(new Label("."));
            add(wSource, NORTH);
            add(propertyPanel, NORTH);
            add(wTarget, NORTH);
            add(wButtons, SOUTH);
            setCellHorizontalAlignment(wButtons, ALIGN_RIGHT);
            setCellVerticalAlignment(wButtons, ALIGN_BOTTOM);
            reposition(AllValuesFromPattern.this.getSize());
            activate();
        }
    }



    private void removeConnectionHint() {
        if (this.connectionHint.isPresent()) {
            DOM.getElementById(getConnectionHintId()).removeFromParent();
            disconnect(this.connectionHint.get());
            this.connectionHint = Optional.absent();
        }
    }

    /**
     * Args default to srcPoint and tgtPoint identifiers if not present
     *
     * @param source
     * @param target
     */
    private void addConnectionHint(@NonNull final Optional<String> source,
                                   @NonNull final Optional<String> target) {
        removeConnectionHint();
        final String src = source.or(this.srcPoint.getCurveId());
        final String tgt = target.or(this.tgtPoint.getAnonId());
        final String labelId = getConnectionHintId();
        createArrowLabel(labelId, "");

        this.connectionHint = Optional.of(connectPair(src, tgt, labelId));
    }

    private void resetConnectionHint() {
        addConnectionHint(Optional.<String>absent(), Optional.<String>absent());
    }

    public void maybeFinish() {
        if (srcPoint.getIri().isPresent() &&
            tgtPoint.getIri().isPresent()) {

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
            AllValuesFromPattern.this.removeConnectionHint();
        }
    }

    protected void createCondition(@NonNull final String propertyName,
                                   @NonNull final Curve source,
                                   @NonNull final Curve target) {

        // now the back end elements
        if (source.getIri().isPresent() && target.getLabel().isPresent() && !propertyName.isEmpty()) {
            createConditionHelper(propertyName, source.getIri().get(), target.getLabel().get());
        }
    }

    private void createConditionHelper(@NonNull final String propertyName,
                                       @NonNull final IRI sourceIri,
                                       @NonNull final String targetLabel) {
        final String restrictionAndTarget  = " only " + targetLabel;
        curveRegistry.addCondition(sourceIri, false, propertyName, restrictionAndTarget);
    }

    /**
     * Generate an arrow label textbox and add it to the canvas.
     * You will later need to pass the id of this box to connectPair
     *
     * @param id
     * @param text
     * @return
     */
    private TextBox createArrowLabel(@NonNull final String id,
                                     @NonNull final String text) {
        final TextBox box = new TextBox();
        box.getElement().setId(id);
        box.setText(text);
        box.getElement().setAttribute("placeholder", "PROPERTY");
        box.getElement().addClassName("connection-label");
        getParentPanel().add(box);
        return box;
    }

    private native JavaScriptObject connectPair(String source, String target,
                                                String labelId) /*-{
        return $wnd.connect_pair(source, target, labelId);
        }-*/;

    private native void disconnect(JavaScriptObject connection) /*-{
        $wnd.disconnect(connection);
        }-*/;
}
