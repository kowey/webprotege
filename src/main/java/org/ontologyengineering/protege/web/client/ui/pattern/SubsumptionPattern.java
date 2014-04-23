package org.ontologyengineering.protege.web.client.ui.pattern;

import com.google.common.base.Optional;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;
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
class SubsumptionPattern extends Pattern {

    // pertaining to either the superset or to the subset endpoint
    enum Role { SUPER, SUB };

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------

    private Effects visualEffects;

    // State tracking fields
    private Optional<Curve> alreadyChosen;
    private Optional<Role> firstSnapped;

    // Widgets
    ButtonBar buttonBar;
    final private DragSnapEndpoint superset;
    final private DragSnapEndpoint subset;


    // widgets that are not contained in the template frame necessarily
    final Collection<DragSnapEndpoint> endpoints;

    public SubsumptionPattern(@NonNull final String id,
                              @NonNull final CurveRegistry registry,
                              @NonNull final SearchManager searchManager,
                              @NonNull final AbsolutePanel parentPanel) {
        super(id, registry, searchManager, parentPanel);
        this.buttonBar = new ButtonBar();
        this.alreadyChosen = Optional.absent();
        this.firstSnapped = Optional.absent();
        this.endpoints = new HashSet<DragSnapEndpoint>();

        final Position supersetTopLeft = new Position(1, 1);
        final Position subsetTopLeft = new Position(1 + width / 3, 1 + height / 3);

        final DraggableShape wCurveOuter =
                new DraggableRect(width, height, rounding);
        final DraggableShape wCurveInner =
                new DraggableRect(width / 2, height / 2, rounding);
        final DraggableShape wGhostOuter =
                new DraggableRect(width, height, rounding);
        final DraggableShape wGhostInner =
                new DraggableRect(width / 2, height / 2, rounding);

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
    class SubsumptionEndpoint extends DragSnapEndpoint {
        @NonNull final private Role role;

        public SubsumptionEndpoint(@NonNull Role role,
                        @NonNull String idSuffix,
                        @NonNull DraggableShape curve,
                        @NonNull DraggableShape ghost,
                        @NonNull String color,
                        @NonNull TextBox searchBox,
                        @NonNull String searchColor,
                        @NonNull Position topLeft) {
            super(SubsumptionPattern.this.searchManager,
                  SubsumptionPattern.this.visualEffects,
                  SubsumptionPattern.this.getId() + idSuffix,
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
            if (!SubsumptionPattern.this.firstSnapped.isPresent()) {
                SubsumptionPattern.this.alreadyChosen = Optional.of(match);
                SubsumptionPattern.this.firstSnapped = Optional.of(this.role);
            } else {
                final AbsolutePanel parentPanel = SubsumptionPattern.this.getParentPanel();
                final Role firstRole = firstSnapped.get();
                final Curve chosen = SubsumptionPattern.this.alreadyChosen.get();

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
                SubsumptionPattern.this.maybeFinish();
            }
        }

        protected Collection<Curve> getAlreadyChosen() {
            return SubsumptionPattern.this.alreadyChosen.asSet();
        }

        protected void resetSnapChoices() {
            SubsumptionPattern.this.alreadyChosen = Optional.absent();
            SubsumptionPattern.this.firstSnapped = Optional.absent();
        }

        protected void withdrawCurve() {
            final Position topLeft = relativeToParent(getHome());
            getParentPanel().setWidgetPosition(getCurve(), topLeft.getX(), topLeft.getY());
            getParentPanel().setWidgetPosition(getGhost(), topLeft.getX(), topLeft.getY());
        }
    }

    @Override
    public void onLoad() {
        this.getElement().setId(getId());
        this.setPixelSize(Pattern.DEFAULT_TEMPLATE_WIDTH,
                Pattern.DEFAULT_TEMPLATE_HEIGHT);
        super.onLoad();

        for (DragSnapEndpoint endpoint : endpoints) {
            getParentPanel().add(endpoint.getCurve());
            getParentPanel().add(endpoint.getGhost());
            visualEffects.addDefaultEffect(visualEffects.ghostPattern(endpoint.getGhost()));
            endpoint.onLoad();
        }
        visualEffects.applyAttributes();
        this.add(buttonBar, getSize().getWidth() + 5, 0);
        buttonBar.reposition(getSize());
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
                    for (SearchHandler handler : SubsumptionPattern.this.endpoints) {
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
            reposition(SubsumptionPattern.this.getSize());
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

        public Optional<DragSnapEndpoint> getActiveCurve() {
            if (activeCurves.contains(subset)) {
                return Optional.of(subset);
            } else if (activeCurves.contains(superset)) {
                return Optional.of(superset);
            } else {
                return Optional.absent();
            }
        }

        private void highlightActiveCurves() {
            for (DragSnapEndpoint endpoint : endpoints) {
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
            IRI oldParent = getCurveRegistry().getImmediateParent(cls);
            getCurveRegistry().moveClass(cls, oldParent, newParent);
            for (Endpoint endpoint : endpoints) {
                endpoint.reset();
            }
        }
    }
}
