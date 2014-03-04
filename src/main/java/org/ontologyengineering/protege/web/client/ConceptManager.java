package org.ontologyengineering.protege.web.client;

import org.ontologyengineering.protege.web.client.ui.pattern.Concept;
import org.semanticweb.owlapi.model.IRI;

public interface ConceptManager {
    public void selectClass(final IRI iri);

    public void createClass(final Concept concept,
                            final String name);

    public void deleteClass(final IRI iri);

    public void onDeleteClass(final IRI iri);

    public void renameClass(final IRI iri,
                            final String oldName,
                            final String newName);

    public void checkClassName(final Concept concept);

}
