package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.common.base.Optional;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceManager;
import edu.stanford.bmir.protege.web.client.dispatch.actions.*;
import edu.stanford.bmir.protege.web.client.project.Project;
import edu.stanford.bmir.protege.web.client.rpc.AbstractAsyncHandler;
import edu.stanford.bmir.protege.web.client.rpc.OntologyServiceManager;
import edu.stanford.bmir.protege.web.client.rpc.data.EntityData;
import edu.stanford.bmir.protege.web.client.rpc.data.Triple;
import edu.stanford.bmir.protege.web.client.rpc.data.layout.PortletConfiguration;
import edu.stanford.bmir.protege.web.client.ui.frame.LabelledFrame;
import edu.stanford.bmir.protege.web.client.ui.library.msgbox.MessageBox;
import edu.stanford.bmir.protege.web.client.ui.portlet.AbstractOWLEntityPortlet;
import edu.stanford.bmir.protege.web.shared.DataFactory;
import edu.stanford.bmir.protege.web.shared.dispatch.Result;
import edu.stanford.bmir.protege.web.shared.dispatch.UpdateObjectAction;
import edu.stanford.bmir.protege.web.shared.entity.OWLPrimitiveData;
import edu.stanford.bmir.protege.web.shared.event.BrowserTextChangedEvent;
import edu.stanford.bmir.protege.web.shared.event.BrowserTextChangedHandler;
import edu.stanford.bmir.protege.web.shared.frame.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.ontologyengineering.protege.web.client.ConceptManager;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplNoCompression;

import java.util.*;


public class ConceptDiagramPortlet extends AbstractOWLEntityPortlet implements ConceptManager {

    private boolean registeredEventHandlers = false;
    private Map<IRI, Concept> namedCurves = new HashMap();

    public ConceptDiagramPortlet(Project project) {
        super(project);
    }

    @Override
    public void initialize() {
        setTitle("ConceptDiagram");
        koweySetup();
        registerEventHandlers();
        reload();
    }

    @Override
    public void setPortletConfiguration(PortletConfiguration portletConfiguration) {
        super.setPortletConfiguration(portletConfiguration);
    }

    private void registerEventHandlers() {
        if(registeredEventHandlers) {
            return;
        }
        GWT.log("Registering event handlers for ConceptDiagramPortlet " + this);
        registeredEventHandlers = true;
        ///////////////////////////////////////////////////////////////////////////
        //
        // Registration of event handlers that we are interested in
        //
        ///////////////////////////////////////////////////////////////////////////

        addProjectEventHandler(BrowserTextChangedEvent.TYPE, new BrowserTextChangedHandler() {
            @Override
            public void browserTextChanged(BrowserTextChangedEvent event) {
                onEntityBrowserTextChanged(event);
            }
        });
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
                conceptTemplate.copyTemplate(vPanel, "concept", 0);
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

    /**
     * Called to update the concept label
     * @param event The event that describes the browser text change that happened.
     */
    protected void onEntityBrowserTextChanged(BrowserTextChangedEvent event) {

        IRI toRename = event.getEntity().getIRI();
        Optional<String> newName = Optional.of(event.getNewBrowserText());
        GWT.log("[CM rename] received BrowserTextChangedEvent event " + event +
                " | " + toRename + " to " + newName +
                " | " + event.getSource());
        Concept curve = namedCurves.get(toRename);
        if (curve != null) {
            if (!curve.getLabel().equals(newName)) {
                curve.setLabel(newName);
            }
        }

    }

    public void createClass(@NonNull final Concept concept,
                            @NonNull final String name) {
        DispatchServiceManager.get().execute(
                new CreateClassAction(getProjectId(), name, DataFactory.getOWLThing()),
                getCreateClassAsyncHandler(concept));
    }

    public void checkClassName(@NonNull final Concept concept) {
        GWT.log("[CM] Want to know class name for " + concept + " | " + concept.getId());
        if (concept.getIri().isPresent()) {
            OntologyServiceManager.getInstance().getRelatedProperties(getProject().getProjectId(), concept.getIri().get().toString(),
                    new GetTriplesHandler(concept));
        }
     }

    public void deleteClass(@NonNull final IRI iri) {
        GWT.log("[CM] Should delete class " + iri.toString());
        OWLClass entity = DataFactory.getOWLClass(iri.toString());
        DispatchServiceManager.get().execute(
                new DeleteEntityAction(entity, getProjectId()),
                new DeleteClassHandler());

        refreshFromServer(500);
    }

    private LabelledFrame<AnnotationPropertyFrame>
    createNamingFrame(@NonNull final IRI iri,
                      @NonNull final String name) {
        // that which is being named
        OWLAnnotationProperty property = DataFactory.getOWLAnnotationProperty(iri.toString());

        // FIXME: don't know what to populate these with
        // is this the domain and range of rdfs:label triples?
        OWLPrimitiveDataList domains = new OWLPrimitiveDataList(new ArrayList<OWLPrimitiveData>());
        OWLPrimitiveDataList ranges  = new OWLPrimitiveDataList(new ArrayList<OWLPrimitiveData>());
        final Set<OWLEntity> domainsClasses = new HashSet<OWLEntity>(domains.getSignature());
        final Set<OWLEntity> rangeTypes = new HashSet<OWLEntity>(ranges.getSignature());

        // complicated way of saying [("rdfs:label", name)]
        OWLLiteral nameLiteral = new OWLLiteralImplNoCompression(name, "", DataFactory.getXSDString());
        PropertyAnnotationValue labelAnnoPair =
                new PropertyAnnotationValue(DataFactory.get().getRDFSLabel(), nameLiteral);
        PropertyValueList pvList =
                new PropertyValueList(Arrays.<PropertyValue>asList(labelAnnoPair));

        // packing everything up into a frame
        AnnotationPropertyFrame annoFrame =
            new AnnotationPropertyFrame(property,
                                        pvList.getAnnotationPropertyValues(),
                                        domainsClasses,
                                        rangeTypes);
        return new LabelledFrame(name, annoFrame);
    }

    public void renameClass(@NonNull final IRI iri,
                            @NonNull final String oldName,
                            @NonNull final String newName) {
        if (oldName.equals(newName) || newName == null || newName.length() == 0) {
            return;
        }
        GWT.log("[CM] Invoking rename " + iri + " from " + oldName + " to " + newName, null);
        LabelledFrame<AnnotationPropertyFrame> oldFrame = createNamingFrame(iri, oldName);
        LabelledFrame<AnnotationPropertyFrame> newFrame = createNamingFrame(iri, newName);

        UpdateObjectAction<LabelledFrame<AnnotationPropertyFrame>> updateAction =
            new UpdateAnnotationPropertyFrameAction(getProjectId(), oldFrame, newFrame);
        DispatchServiceManager.get().execute(updateAction, new AsyncCallback<Result>() {
            @Override
            public void onFailure(Throwable caught) {
                GWT.log("[CM] Updating object failed", caught);
            }

            @Override
            public void onSuccess(Result result) {
                GWT.log("[CM] Object successfully renamed");
            }
        });
    }

    public void onDeleteClass(@NonNull final IRI iri) {
        namedCurves.remove(iri);
    }

    public void onCreateClass(@NonNull final IRI iri,
                              @NonNull final Concept concept) {
        namedCurves.put(iri, concept);
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
            IRI iri = result.getObject().getIRI();
            GWT.log("[CM] created object: " + iri);
            concept.setIri(Optional.of(iri));
            portlet.onCreateClass(iri, concept);
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
            GWT.log("[CM] Delete successfully class ", null);;
        }
    }

    class RenameClassHandler extends AbstractAsyncHandler<Void> {
        @Override
        public void handleFailure(final Throwable caught) {
            GWT.log("[CM] Error renaming class", caught);
            MessageBox.showErrorMessage("Class not deleted", caught);
        }

        @Override
        public void handleSuccess(final Void result) {
            GWT.log("[CM] My rename invocation succeeded ", null);
        }
    }

    @RequiredArgsConstructor
    class GetTriplesHandler extends AbstractAsyncHandler<List<Triple>> {

        private final Concept concept;

        @Override
        public void handleFailure(Throwable caught) {
            GWT.log("Error at retrieving props in domain for " + _currentEntity, caught);
        }

        @Override
        public void handleSuccess(List<Triple> triples) {
            for (Triple triple : triples) {
                GWT.log("[CM] triple " + triple.getProperty() + " " + triple.getValue());
                if (triple.getProperty().toString().equals("rdfs:label")) {
                    concept.getWQueryResult().setText(">>" +  triple.getValue() + "<<");
                }
            }
        }

    }

    public static native void gwtjsPlumbConnect(JavaScriptObject pairs) /*-{
            $wnd.gwtjsconnect(pairs);

        }-*/;


}

