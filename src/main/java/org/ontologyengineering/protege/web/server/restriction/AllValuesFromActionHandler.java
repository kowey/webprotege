package org.ontologyengineering.protege.web.server.restriction;

import edu.stanford.bmir.protege.web.client.dispatch.VoidResult;
import edu.stanford.bmir.protege.web.client.dispatch.actions.CreateClassAction;
import edu.stanford.bmir.protege.web.client.dispatch.actions.CreateClassResult;
import edu.stanford.bmir.protege.web.server.change.*;
import edu.stanford.bmir.protege.web.server.dispatch.*;
import edu.stanford.bmir.protege.web.server.dispatch.validators.UserHasProjectWritePermissionValidator;
import edu.stanford.bmir.protege.web.server.msg.OWLMessageFormatter;
import edu.stanford.bmir.protege.web.server.owlapi.OWLAPIProject;
import edu.stanford.bmir.protege.web.server.owlapi.OWLAPIProjectManager;
import edu.stanford.bmir.protege.web.server.owlapi.RenameMap;
import edu.stanford.bmir.protege.web.shared.DataFactory;
import edu.stanford.bmir.protege.web.shared.event.ProjectEvent;
import edu.stanford.bmir.protege.web.shared.events.EventList;
import edu.stanford.smi.protege.util.Log;
import lombok.NonNull;
import org.ontologyengineering.protege.web.shared.restriction.AllValuesFromAction;
import org.semanticweb.owlapi.model.*;

import java.util.logging.Logger;

public class AllValuesFromActionHandler extends AbstractProjectChangeHandler<OWLProperty, AllValuesFromAction, VoidResult> {

    @Override
    public Class<AllValuesFromAction> getActionClass() {
        return AllValuesFromAction.class;
    }

    @Override
    protected ChangeDescriptionGenerator<OWLProperty> getChangeDescription(AllValuesFromAction action,
                                                                        OWLAPIProject project,
                                                                        ExecutionContext executionContext) {
        return new FixedMessageChangeDescriptionGenerator<OWLProperty>(OWLMessageFormatter
                .formatMessage("Set {0} to subclass of all values from {1}.{2}",
                        project,
                        action.getSourceIri().getFragment(),
                        action.getProperty(),
                        action.getTargetIri().getFragment()));
    }


    @Override
    public RequestValidator<AllValuesFromAction> getAdditionalRequestValidator(AllValuesFromAction action, RequestContext requestContext) {
        return UserHasProjectWritePermissionValidator.get();
    }

    protected ChangeListGenerator<OWLProperty> getChangeListGenerator(@NonNull final AllValuesFromAction action,
                                                                      @NonNull final OWLAPIProject project,
                                                                      @NonNull final ExecutionContext executionContext) {
        return new ChangeListGenerator<OWLProperty>() {
            @Override
            public OntologyChangeList<OWLProperty> generateChanges(OWLAPIProject project, ChangeGenerationContext context) {
                final OWLOntology ontology = OWLAPIProjectManager.getProjectManager()
                        .getProject(action.getProjectId())
                        .getRootOntology();
                final OWLOntologyManager manager = ontology.getOWLOntologyManager();
                final OWLDataFactory factory = manager.getOWLDataFactory();
                final OWLProperty property = DataFactory.getFreshOWLEntity(EntityType.OBJECT_PROPERTY, action.getProperty());
                final OWLClass sourceClass = factory.getOWLClass(action.getSourceIri());
                final OWLClass targetClass = factory.getOWLClass(action.getTargetIri());

                final OWLObjectPropertyExpression propertyExp =
                        factory.getOWLObjectProperty(property.getIRI());
                final OWLClassExpression targetClassExp =
                        factory.getOWLObjectAllValuesFrom(propertyExp, targetClass);

                OntologyChangeList.Builder<OWLProperty> builder = new OntologyChangeList.Builder<OWLProperty>();

                builder.addAxiom(ontology,
                        factory.getOWLDeclarationAxiom(property));
                builder.addAxiom(ontology,
                        factory.getOWLSubClassOfAxiom(sourceClass, targetClassExp));

                return builder.build(property);
            }

            @Override
            public OWLProperty getRenamedResult(OWLProperty result, RenameMap renameMap) {
                // FIXME: not sure what this is for (copied from createclass)
                return renameMap.getRenamedEntity(result);
            }
        };
    }


    protected VoidResult createActionResult(@NonNull final ChangeApplicationResult<OWLProperty> changeApplicationResult,
                                            @NonNull final AllValuesFromAction action,
                                            @NonNull final OWLAPIProject project,
                                            @NonNull final ExecutionContext executionContext,
                                            @NonNull EventList<ProjectEvent<?>> eventList) {
        return VoidResult.get();
    }


}
