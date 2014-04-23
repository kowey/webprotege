package org.ontologyengineering.protege.web.client.ui.dragsnap;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.TextBox;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.ontologyengineering.protege.web.client.effect.VisualEffect;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.SearchManager;
import org.ontologyengineering.protege.web.client.ui.curve.Curve;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableShape;
import org.ontologyengineering.protege.web.client.util.Position;
import org.semanticweb.owlapi.model.IRI;

import java.util.Collection;
import java.util.Collections;

/**
 * What happens when we manipulate one of the template curves (for
 * example, by dragging them out onto the canvas), or their associated
 * search boxes (for example, by typing into them)
 */
@Getter
@Setter
abstract public class DragSnapEndpoint extends Endpoint implements SearchManager.SearchHandler,
        MouseOverHandler, MouseOutHandler,
        MouseUpHandler, MouseDownHandler, MouseMoveHandler,
        KeyUpHandler {


    @NonNull final SearchManager searchManager;
    @NonNull final Effects effects;

    @NonNull final private String color;
    @NonNull final private TextBox searchBox;
    @NonNull final private String searchColor;
    @NonNull final private SearchManager.SearchHandler searchHandler;

    @NonNull
    Optional<IRI> iri = Optional.absent();

    // we need to keep track of pre-existing candidates so that
    // we can remove any visual effects we've applied on them once
    // they are no longer candidates
    Collection<Curve> candidates = Collections.emptyList();
    private boolean dragging = false;

    public DragSnapEndpoint(@NonNull SearchManager searchManager,
                            @NonNull Effects effects,
                            @NonNull String idSuffix,
                            @NonNull DraggableShape curve,
                            @NonNull DraggableShape ghost,
                            @NonNull String color,
                            @NonNull TextBox searchBox,
                            @NonNull String searchColor,
                            @NonNull Position topLeft) {
        super(ghost, curve, idSuffix, topLeft);
        this.searchManager = searchManager;
        this.effects = effects;
        this.color = color;
        this.searchBox = searchBox;
        this.searchColor = searchColor;
        this.searchHandler = searchManager.makeSearchHandler(searchBox, searchColor);
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
        effects.clear();
        effects.addActiveCurve(this);
    }

    abstract protected void snapToMatch(@NonNull final Curve match);

    /**
     * Return curves that have already been chosen in this dragSnap interaction
     * and which should therefore not be considered as a match candidate
     */
    abstract protected Collection<Curve> getAlreadyChosen();

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
        for (Curve curve : getAlreadyChosen()) {
            candidates.remove(curve);
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
    @Override
    public void update() {
        Collection<Curve> newCandidates = getSnapCandidates();

        for (Curve oldCandidate : candidates) {
            oldCandidate.getEffects().applyDragSnapEffect(curve, Optional.<VisualEffect>absent());
        }

        if (newCandidates.size() == 1) {
            Curve.Effects curveEffects = newCandidates.iterator().next().getEffects();
            curveEffects.applyDragSnapEffect(curve, Optional.of(curveEffects.dragSnapUnique(color)));
        } else {
            for (Curve newCandidate : newCandidates) {
                Curve.Effects effects = newCandidate.getEffects();
                effects.applyDragSnapEffect(curve, Optional.of(effects.dragSnapPartial(color)));
            }
        }
        candidates = newCandidates;
    }

    /**
     * Clear out whatever state-tracking is needed to keep track of
     * the drag/snap process
     */
    abstract protected void resetSnapChoices();


    /**
     * Clear the current search and apply visual effects as appropriate
     */
    @Override
    public void reset() {
        update();
        iri = Optional.absent();
        withdrawCurve();
        resetSnapChoices();
        curve.setVisible(true);
        searchBox.setEnabled(true);
        searchHandler.reset();
    }

    @Override
    public void onMouseOver(MouseOverEvent event) {
        effects.addActiveCurve(this);
        searchBox.setFocus(true);
    }

    @Override
    public void onMouseOut(MouseOutEvent event) {
        effects.removeActiveCurve(this);
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
    @Override
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