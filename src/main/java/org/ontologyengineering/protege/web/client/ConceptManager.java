package org.ontologyengineering.protege.web.client;

import lombok.NonNull;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.Concept;
import org.semanticweb.owlapi.model.IRI;

public interface ConceptManager {
    public void createClass(@NonNull final Concept concept,
                            @NonNull final String name);

    public void deleteClass(@NonNull final IRI iri);

    public void onDeleteClass(@NonNull final IRI iri);

    public void renameClass(@NonNull final IRI iri,
                            @NonNull final String oldName,
                            @NonNull final String newName);

    public void checkClassName(@NonNull final Concept concept);

}
