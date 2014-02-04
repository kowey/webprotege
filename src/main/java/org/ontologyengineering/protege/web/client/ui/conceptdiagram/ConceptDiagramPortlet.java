package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.common.base.Optional;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import edu.stanford.bmir.protege.web.client.Application;
import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceManager;
import edu.stanford.bmir.protege.web.client.dispatch.actions.CreateClassAction;
import edu.stanford.bmir.protege.web.client.dispatch.actions.CreateClassResult;
import edu.stanford.bmir.protege.web.client.dispatch.actions.DeleteEntityAction;
import edu.stanford.bmir.protege.web.client.dispatch.actions.DeleteEntityResult;
import edu.stanford.bmir.protege.web.client.model.PropertyValueUtil;
import edu.stanford.bmir.protege.web.client.project.Project;
import edu.stanford.bmir.protege.web.client.rpc.AbstractAsyncHandler;
import edu.stanford.bmir.protege.web.client.rpc.OntologyServiceManager;
import edu.stanford.bmir.protege.web.client.rpc.data.EntityData;
import edu.stanford.bmir.protege.web.client.rpc.data.ValueType;
import edu.stanford.bmir.protege.web.client.rpc.data.layout.PortletConfiguration;
import edu.stanford.bmir.protege.web.client.ui.library.msgbox.MessageBox;
import edu.stanford.bmir.protege.web.client.ui.portlet.AbstractOWLEntityPortlet;
import edu.stanford.bmir.protege.web.shared.DataFactory;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.user.UserId;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.ontologyengineering.protege.web.client.ConceptDiagram;
import org.ontologyengineering.protege.web.client.ConceptManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;

import java.util.ArrayList;


public class ConceptDiagramPortlet extends AbstractOWLEntityPortlet implements ConceptManager {

    public ConceptDiagramPortlet(Project project) {
        super(project);
    }

    @Override
    public void initialize() {
        setTitle("ConceptDiagram");
        koweySetup();
        reload();
    }

    @Override
    public void setPortletConfiguration(PortletConfiguration portletConfiguration) {
        super.setPortletConfiguration(portletConfiguration);
    }

    @RequiredArgsConstructor
    class CreateDiagramHandler implements ClickHandler {

        private final AbsolutePanel vPanel;
        private final ConceptManager conceptManager;

        @Override
        public void onClick(ClickEvent clickEvent) {
            try {
                final Concept conceptTemplate = new Concept("template", conceptManager);
                conceptTemplate.startTemplateMode("CONCEPT");
                final int templateX = 0;
                final int templateY = 20;
                vPanel.add(new Label("Drag one of these templates out to instantiate it"));
                vPanel.add(conceptTemplate, templateX, templateY);
                final Concept concept = conceptTemplate.copyTemplate(vPanel, "concept", 0);
            } catch (Exception e) {
                GWT.log("buh?", e);

            }

        }}

    public void koweySetup() {
        AbsolutePanel vPanel = new AbsolutePanel();
        vPanel.getElement().getStyle().setProperty("height", "100%");
        vPanel.getElement().getStyle().setProperty("width", "100%");
        final Button btn = new Button("Start");
        vPanel.add(btn);
        btn.addClickHandler(new CreateDiagramHandler(vPanel, this));
        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                btn.removeFromParent();
            }
        });
        this.add(vPanel);
    }

    @Override
    public void reload() {
        if (_currentEntity == null) {
            return;
        }
    }

    public ArrayList<EntityData> getSelection() {
        return null;
    }

    public void createClass(@NonNull final Concept concept,
                            @NonNull final String name) {
        DispatchServiceManager.get().execute(
                new CreateClassAction(getProjectId(), name, DataFactory.getOWLThing()),
                getCreateClassAsyncHandler(concept));
    }

    public void deleteClass(@NonNull final IRI iri) {
        GWT.log("[CM] Should delete class " + iri.toString());
        OWLClass entity = DataFactory.getOWLClass(iri.toString());
        DispatchServiceManager.get().execute(
                new DeleteEntityAction(entity, getProjectId()),
                new DeleteClassHandler());

        refreshFromServer(500);
    }

    public void renameClass(@NonNull final IRI iri,
                            @NonNull final String oldName,
                            @NonNull final String newName) {
        GWT.log("[CM] Don't yet know how to rename " + newName, null);
        if (oldName.equals(newName) || newName == null || newName.length() == 0) {
            return;
        }
    }

    /*
     * ************ Remote procedure calls *****************
     */

    protected AbstractAsyncHandler<CreateClassResult> getCreateClassAsyncHandler(Concept concept) {
        return new CreateClassHandler(this, concept);
    }

    @RequiredArgsConstructor
    class CreateClassHandler extends AbstractAsyncHandler<CreateClassResult> {

        final ConceptDiagramPortlet portlet;
        final Concept concept;

        @Override
        public void handleFailure(final Throwable caught) {
            GWT.log("[CM] Error at creating class", caught);
            MessageBox.showErrorMessage("Class not created", caught);
        }

        @Override
        public void handleSuccess(final CreateClassResult result) {
            GWT.log("[CM] created object: " + result.getObject().getIRI());
            concept.setIri(Optional.of(result.getObject().getIRI()));
            // we don't need to react to this IMHO
        }
    }

    class DeleteClassHandler extends AbstractAsyncHandler<DeleteEntityResult> {

        @Override
        public void handleFailure(final Throwable caught) {
            GWT.log("[CM] Error at deleting class", caught);
            MessageBox.showErrorMessage("Class not deleted", caught);
        }

        @Override
        public void handleSuccess(final DeleteEntityResult result) {
            GWT.log("[CM] Delete successfully class ", null);

        }
    }

    public static native void gwtjsPlumbConnect(JavaScriptObject pairs) /*-{
            $wnd.gwtjsconnect(pairs);

        }-*/;


}


