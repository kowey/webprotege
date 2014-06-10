package org.ontologyengineering.protege.web.server.restriction;

import edu.stanford.bmir.protege.web.client.dispatch.VoidResult;
import edu.stanford.bmir.protege.web.server.dispatch.ActionHandler;
import edu.stanford.bmir.protege.web.server.dispatch.ExecutionContext;
import edu.stanford.bmir.protege.web.server.dispatch.RequestContext;
import edu.stanford.bmir.protege.web.server.dispatch.RequestValidator;
import edu.stanford.bmir.protege.web.server.dispatch.validators.UserHasProjectWritePermissionValidator;
import edu.stanford.bmir.protege.web.server.owlapi.OWLAPIProjectManager;
import edu.stanford.smi.protege.util.Log;
import org.ontologyengineering.protege.web.shared.restriction.AllValuesFromAction;
import org.semanticweb.owlapi.model.*;

import java.util.logging.Logger;

public class AllValuesFromActionHandler implements ActionHandler<AllValuesFromAction, VoidResult> {
    public RequestValidator<AllValuesFromAction> getRequestValidator(AllValuesFromAction action, RequestContext requestContext) {
        return UserHasProjectWritePermissionValidator.get();
    }

    @Override
    public VoidResult execute(AllValuesFromAction action, ExecutionContext executionContext) {
        Log.getLogger().info("{CONCEPT DIAGRAM ADD AVF} " + action);
        final OWLOntology ontology = OWLAPIProjectManager.getProjectManager()
                        .getProject(action.getProjectId())
                        .getRootOntology();
        final OWLOntologyManager manager = ontology.getOWLOntologyManager();
        final OWLDataFactory factory = manager.getOWLDataFactory();
        final IRI propertyIri = null; // FIXME
        final OWLProperty property = factory.getOWLObjectProperty(propertyIri);
        final OWLClass sourceClass = factory.getOWLClass(action.getSourceIri());
        final OWLClass targetClass = factory.getOWLClass(action.getTargetIri());

        final OWLObjectPropertyExpression propertyExp =
                factory.getOWLObjectProperty(propertyIri);
        final OWLClassExpression targetClassExp =
                factory.getOWLObjectAllValuesFrom(propertyExp, targetClass);

        manager.addAxiom(ontology,
                factory.getOWLDeclarationAxiom(property));
        manager.addAxiom(ontology,
                factory.getOWLSubClassOfAxiom(sourceClass, targetClassExp));

        return VoidResult.get();
    }

    @Override
    public Class<AllValuesFromAction> getActionClass() {
        return AllValuesFromAction.class;
    }
}
