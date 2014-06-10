package org.ontologyengineering.protege.web.shared.restriction;

import edu.stanford.bmir.protege.web.client.dispatch.VoidResult;
import edu.stanford.bmir.protege.web.shared.HasProjectId;
import edu.stanford.bmir.protege.web.shared.dispatch.Action;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.semanticweb.owlapi.model.IRI;

/**
 * Add an axiom that allow us to express things like:
 *
 * Lecturer ⊑ ∀ teaches . Module
 *
 * Creates properties if needed
 */
@Getter @ToString @AllArgsConstructor
public class AllValuesFromAction implements Action<VoidResult>, HasProjectId {
    private ProjectId projectId;
    private IRI sourceIri;
    private String property;
    private IRI targetIri;

    private AllValuesFromAction() {} // serialization only
}
