package org.ontologyengineering.protege.web.client;

import org.ontologyengineering.protege.web.client.ui.pattern.Curve;
import org.semanticweb.owlapi.model.IRI;

public interface ConceptManager {
    public void selectClass(final IRI iri);

    public void createClass(final Curve curve,
                            final String name);

    public void deleteClass(final IRI iri);

    public void onDeleteClass(final IRI iri);

    public void renameClass(final IRI iri,
                            final String oldName,
                            final String newName);

    /**
     * Rename just the current curve; if the curve is the only one representing
     * its class, rename the whole class
     *
     * @param curve
     * @param newName
     */
    public void renameCurveOnly(final Curve curve,
                                final String newName);

    public void checkClassName(final Curve curve);

}
