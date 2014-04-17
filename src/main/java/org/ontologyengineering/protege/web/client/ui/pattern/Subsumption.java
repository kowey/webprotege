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
    private Effects visualEffects;


    // Widgets

    ButtonBar buttonBar;
    final private Endpoint superset;
    final private Endpoint subset;

    private Optional<Curve> alreadyChosen;
    private Optional<Role> firstSnapped;

    // widgets that are not contained in the template frame necessarily
    final Collection<Endpoint> endpoints;

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

        final Position supersetTopLeft = new Position(1, 1);
        final Position subsetTopLeft = new Position(1 + width / 3, 1 + height / 3);

        final DraggableShape wCurveOuter =
                new DraggableRect(width, height, this.core.rounding);
        final DraggableShape wCurveInner =
                new DraggableRect(width / 2, height / 2, this.core.rounding);
        final DraggableShape wGhostOuter =
                new DraggableRect(width, height, this.core.rounding);
        final DraggableShape wGhostInner =
                new DraggableRect(width / 2, height / 2, this.core.rounding);

        this.visualEffects = new SubsumptionEffects(Arrays.<Widget>asList(
                wCurveInner, wGhostInner,
                wCurveOuter, wGhostOuter));

        superset = new SubsumptionEndpoint(Role.SUPER, "_curve_outer",
                wCurveOuter, wGhostOuter, "green",
                buttonBar.wSuperset, "darkgreen",
                supersetTopLeft);
        subset = new SubsumptionEndpoint(Role.SUB, "_curve_inner",
                wCurveInner, wGhostInner, "blue",
                buttonBar.wSubset, "darkblue",
                subsetTopLeft);

        endpoints.add(superset);
        endpoints.add(subset);
        getElement().setClassName("template");

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
    class SubsumptionEndpoint extends Endpoint {
        @NonNull final private Role role;

        public SubsumptionEndpoint(@NonNull Role role,
                        @NonNull String idSuffix,
                        @NonNull DraggableShape curve,
                        @NonNull DraggableShape ghost,
                        @NonNull String color,
                        @NonNull TextBox searchBox,
                        @NonNull String searchColor,
                        @NonNull Position topLeft) {
            super(Subsumption.this.searchManager,
                  Subsumption.this.visualEffects,
                  Subsumption.this.core.id + idSuffix,
                  curve, ghost, color, searchBox, searchColor,
                  topLeft);
            this.role = role;
        }

        public void onLoad() {
            super.onLoad();
            if (this.role == Role.SUB) {
                getCurve().addStyleName("snap-to-drag-inner-curve");
            } else {
                getCurve().addStyleName("snap-to-drag-outer-curve");
            }
            makeDraggable("#" + getCurveId());
        }

        protected void snapToMatch(@NonNull final Curve match) {
            getCurve().setVisible(false);
            getSearchBox().setText(match.getLabel().or("<UNNAMED>"));
            getSearchBox().setEnabled(false);
            setIri(match.getIri());
            if (!Subsumption.this.firstSnapped.isPresent()) {
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
                        scale = new Scale((float) 1.2, (float) 1.2);
                        topleft = new Position(topleft.getX() - 10, topleft.getY() - 10);
                        break;
                    case SUPER:
                        scale = new Scale((float) 0.8, (float) 0.8);
                        topleft = new Position(topleft.getX() + 10, topleft.getY() + 10);
                        break;
                }
                Curve other = match.createCurve(parentPanel, topleft.getX(), topleft.getY());
                other.setSize(scale.transform(chosen.getSize()));
                Subsumption.this.maybeFinish();
            }
        }

        protected Collection<Curve> getAlreadyChosen() {
            return Subsumption.this.alreadyChosen.asSet();
        }

        protected void resetSnapChoices() {
            Subsumption.this.alreadyChosen = Optional.absent();
            Subsumption.this.firstSnapped = Optional.absent();
        }

        protected void withdrawCurve() {
            final Position topLeft = relativeToParent(getHome());
            Subsumption.this.parentPanel.setWidgetPosition(getCurve(), topLeft.getX(), topLeft.getY());
            Subsumption.this.parentPanel.setWidgetPosition(getGhost(), topLeft.getX(), topLeft.getY());
        }
    }

    class SubsumptionPanel extends AbsolutePanel {

        @Override
        public void onLoad() {
            Subsumption subsumption = Subsumption.this;
            this.getElement().setId(subsumption.core.getId());
            this.setPixelSize(Pattern.DEFAULT_TEMPLATE_WIDTH,
                    Pattern.DEFAULT_TEMPLATE_HEIGHT);
            super.onLoad();

            for (Endpoint endpoint : endpoints) {
                parentPanel.add(endpoint.getCurve());
                parentPanel.add(endpoint.getGhost());
                visualEffects.addDefaultEffect(visualEffects.ghostPattern(endpoint.getGhost()));
                endpoint.onLoad();
            }
            visualEffects.applyAttributes();
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
    class SubsumptionEffects extends Effects {
        public SubsumptionEffects(@NonNull final Collection<Widget> widgets) {
            super(widgets);
        }

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
                Optional<VisualEffect> effect = (getActiveCurve().equals(Optional.of(endpoint)))
                        ? Optional.of(activePattern(endpoint.getCurve(), endpoint.getSearchColor()))
                        : Optional.<VisualEffect>absent();
                setEffect(endpoint.getCurve(), effect);
            }
            applyAttributes();
        }
    }

    public void maybeFinish() {
        if (superset.getIri().isPresent() && subset.getIri().isPresent()) {
            // do the actual move
            IRI cls = subset.getIri().get();
            IRI newParent = superset.getIri().get();
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
