package org.ontologyengineering.protege.web.client.ui.pattern;

import com.google.common.base.Optional;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.*;
import lombok.*;
import org.ontologyengineering.protege.web.client.effect.VisualEffect;
import org.ontologyengineering.protege.web.client.ui.dragsnap.Effects;
import org.ontologyengineering.protege.web.client.ui.dragsnap.Endpoint;
import org.ontologyengineering.protege.web.client.util.Position;
import org.ontologyengineering.protege.web.client.util.Size;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.CurveRegistry;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.SearchManager;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.SearchManager.SearchHandler;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableRect;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableShape;

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
    private Effects visualEffects;

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
    final Collection<Endpoint> endpoints;



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

        final Position srcTopLeft = new Position(3, 3);
        final Position tgtTopLeft = new Position(3 + width + 40, 3);

        final DraggableShape wCurveSource =
                new DraggableRect(width, height, this.core.rounding);
        final DraggableShape wCurveTarget =
                new DraggableRect(width, height, this.core.rounding);
        final DraggableShape wGhostSource =
                new DraggableRect(width, height, this.core.rounding);
        final DraggableShape wGhostTarget =
                new DraggableRect(width, height, this.core.rounding);

        visualEffects = new PropertyEffects(Arrays.<Widget>asList(
                wCurveSource, wCurveTarget,
                wGhostSource, wGhostTarget));

        srcPoint = new PropertyEndpoint(Role.SOURCE, "_curve_source",
                wCurveSource, wGhostSource, "pink",
                buttonBar.wSource, "red",
                srcTopLeft);
        tgtPoint = new PropertyEndpoint(Role.TARGET, "_curve_target",
                wCurveTarget, wGhostTarget, "yellow",
                buttonBar.wTarget, "orange",
                tgtTopLeft);

        endpoints.add(srcPoint);
        endpoints.add(tgtPoint);
        getElement().setClassName("template");
    }

    // ----------------------------------------------------------------
    // Handlers
    // ----------------------------------------------------------------

    class PropertyEndpoint extends Endpoint {
        final private Role role;

        public PropertyEndpoint(@NonNull Role role,
                                @NonNull String idSuffix,
                                @NonNull DraggableShape curve,
                                @NonNull DraggableShape ghost,
                                @NonNull String color,
                                @NonNull TextBox searchBox,
                                @NonNull String searchColor,
                                @NonNull Position topLeft) {
            super(Property.this.searchManager,
                  Property.this.visualEffects,
                  Property.this.core.id + idSuffix,
                  curve, ghost, color, searchBox, searchColor,
                  topLeft);
            this.role = role;
        }

        protected Collection<Curve> getAlreadyChosen() {
            return Property.this.alreadyChosen.asSet();
        }

        protected void resetSnapChoices() {
            Property.this.alreadyChosen = Optional.absent();
            Property.this.firstSnapped = Optional.absent();
        }

        protected void withdrawCurve() {
            final Position topLeft = relativeToParent(getHome());
            Property.this.parentPanel.setWidgetPosition(getCurve(), topLeft.getX(), topLeft.getY());
            Property.this.parentPanel.setWidgetPosition(getGhost(), topLeft.getX(), topLeft.getY());
        }

        @Override
        public void onLoad() {
            getGhost().addStyleName("snap-to-drag-ghost");
            getCurve().addStyleName("snap-to-drag-curve");
            super.onLoad();
            makeDraggable("#" + getCurveId());
        }

        @Override
        public void onMouseDown(MouseDownEvent event) {
            super.onMouseDown(event);
            if (!Property.this.connectionHint.isPresent()) {
                Property.this.resetConnectionHint();
            }
        }

        // helper for snapIfUniqueMatch
        protected void snapToMatch(@NonNull final Curve match) {
            final Property prop = Property.this;
            getCurve().setVisible(false);
            getSearchBox().setText(match.getLabel().or("<UNNAMED>"));
            getSearchBox().setEnabled(false);
            setIri(match.getIri());
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
        private Set<Endpoint> activeCurves = new HashSet();

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

        public void highlightActiveCurves() {
            for (Endpoint endpoint : endpoints) {
                Optional<VisualEffect> effect = (getActiveCurve().equals(Optional.of(endpoint)))
                        ? Optional.of(activePattern(endpoint.getCurve(), endpoint.getSearchColor()))
                        : Optional.<VisualEffect>absent();
                setEffect(endpoint.getCurve(), effect);
            }
            applyAttributes();
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
                parentPanel.add(endpoint.getCurve());
                parentPanel.add(endpoint.getGhost());
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
