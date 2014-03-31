package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.common.base.Optional;
import org.ontologyengineering.protege.web.client.ui.pattern.Curve;
import org.semanticweb.owlapi.model.IRI;

public interface CurveRegistry {
    public void selectClass(final IRI iri);

    /**
     * Rename just the current curve; if the curve is the only one representing
     * its class, rename the whole class
     *
     * @param curve
     * @param oldName - absent if you are creating the label
     * @param newName - absent if you want to remove the label
     */
    public void changeCurveName(final Curve curve,
                                final Optional<String> oldName,
                                final Optional<String> newName);

    /**
     * For any curve, this should be equivalent to
     * changeCurveName(curve, curve.getLabel(), Optional.absent())
     *
     * @param curve
     */
    public void removeCurveName(final Curve curve);

    public IRI getImmediateParent(final IRI cls);

    public void moveClass(final IRI cls,
                          final IRI oldParent,
                          final IRI newParent);

}
