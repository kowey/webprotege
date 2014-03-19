package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceManager;
import edu.stanford.bmir.protege.web.client.dispatch.actions.*;
import edu.stanford.bmir.protege.web.client.project.Project;
import edu.stanford.bmir.protege.web.client.rpc.AbstractAsyncHandler;
import edu.stanford.bmir.protege.web.client.rpc.OntologyServiceManager;
import edu.stanford.bmir.protege.web.client.rpc.data.EntityData;
import edu.stanford.bmir.protege.web.client.rpc.data.SubclassEntityData;
import edu.stanford.bmir.protege.web.client.rpc.data.Triple;
import edu.stanford.bmir.protege.web.client.rpc.data.ValueType;
import edu.stanford.bmir.protege.web.client.rpc.data.layout.PortletConfiguration;
import edu.stanford.bmir.protege.web.client.ui.frame.LabelledFrame;
import edu.stanford.bmir.protege.web.client.ui.library.msgbox.MessageBox;
import edu.stanford.bmir.protege.web.client.ui.portlet.AbstractOWLEntityPortlet;
import edu.stanford.bmir.protege.web.client.ui.selection.SelectionEvent;
import edu.stanford.bmir.protege.web.shared.DataFactory;
import edu.stanford.bmir.protege.web.shared.dispatch.Result;
import edu.stanford.bmir.protege.web.shared.dispatch.UpdateObjectAction;
import edu.stanford.bmir.protege.web.shared.entity.OWLPrimitiveData;
import edu.stanford.bmir.protege.web.shared.event.BrowserTextChangedEvent;
import edu.stanford.bmir.protege.web.shared.event.BrowserTextChangedHandler;
import edu.stanford.bmir.protege.web.shared.frame.*;
import edu.stanford.bmir.protege.web.shared.hierarchy.ClassHierarchyParentRemovedEvent;
import edu.stanford.bmir.protege.web.shared.hierarchy.ClassHierarchyParentRemovedHandler;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.ontologyengineering.protege.web.client.ConceptManager;
import org.ontologyengineering.protege.web.client.ui.pattern.Concept;
import org.ontologyengineering.protege.web.client.ui.pattern.Pattern;
import org.ontologyengineering.protege.web.client.ui.pattern.Property;
import org.ontologyengineering.protege.web.client.ui.pattern.Subsumption;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableShape;
import org.ontologyengineering.protege.web.client.util.Rectangle;

import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplNoCompression;

import java.util.*;
import java.util.List;


public class ConceptDiagramPortlet extends AbstractOWLEntityPortlet implements ConceptManager, SearchManager {

    private boolean registeredEventHandlers = false;
    final private Map<IRI, Concept> namedCurves = new HashMap();
    private Collection<EntityData> selection = Collections.emptyList();
    @Getter private Optional<DraggableShape> snapSeeker = Optional.absent();
    final private ListMultimap<DraggableShape, Concept> snapCandidates = ArrayListMultimap.create();

    public ConceptDiagramPortlet(Project project) {
        super(project);
    }

    // draw the pattern templates
    private void initTemplates(AbsolutePanel vPanel) {
        vPanel.add(new Label("Drag one of these templates out to instantiate it"));

        final List<Pattern> templates =
                Arrays.<Pattern>asList(
                new Concept("concept-template", this, this),
                new Subsumption("subsume-template", this, vPanel));

        final int yGap = 20;
        final int templateX = 0;
        final int templateY = 3 * yGap;

        int currentY = templateY;
        for (Pattern template : templates) {
            template.startTemplateMode();
            vPanel.add(template, templateX, currentY);
            currentY += template.getHeight() + yGap;
            template.copyTemplate(vPanel, 0);
        }
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

        addProjectEventHandler(ClassHierarchyParentRemovedEvent.TYPE, new ClassHierarchyParentRemovedHandler() {
            @Override
            public void handleClassHierarchyParentRemoved(ClassHierarchyParentRemovedEvent event) {
                if (isEventForThisProject(event)) {
                    GWT.log("[CM] parent removed " + event);
                    handleParentRemovedEvent(event);
                }
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
                initTemplates(vPanel);
            } catch (Exception e) {
                GWT.log("buh?", e);
            }
        }}

    /*
     * ************ Searching and snapping *****************
     */

    @Data
    public class SearchHandlerImpl implements KeyUpHandler, SearchHandler {
        @NonNull private final TextBox textbox;
        @NonNull private final String color;

        @NonNull private Collection<Concept> matching = Collections.emptyList();
        @NonNull private Collection<Concept> nonMatching = Collections.emptyList();

        @NonNull boolean hasSearch = false;

        public Optional<Collection<Concept>> getMatching() {
            if (hasSearch) {
                return Optional.of(matching);
            } else {
                return Optional.absent();
            }
        }

        public Optional<Collection<Concept>> getNonMatching() {
            if (hasSearch) {
                return Optional.of(nonMatching);
            } else {
                return Optional.absent();
            }
        }

        @Override
        public void onKeyUp(KeyUpEvent event) {
            // we respond to key up events up updating a provisional label
            // only on Enter key or mouse-out do we actually commit the label
            final String text = textbox.getText().trim();
            setHasSearch(!text.isEmpty());
            ImmutableListMultimap<Boolean, Concept> partitionedMap =
                    Multimaps.index(namedCurves.values(), new Function<Concept, Boolean>() {
                @Override
                public Boolean apply(Concept input) {
                    if (input.getLabel().isPresent() && !text.isEmpty()) {
                        String clabel = input.getLabel().get();
                        return clabel.contains(text);
                    } else {
                        return false;
                    }
                }
            });

            matching = partitionedMap.get(true);
            nonMatching = partitionedMap.get(false);

            for (Concept concept : nonMatching) {
                concept.setMatchStatus(this, MatchStatus.NO_MATCH);
            }
            if (matching.size() > 1) {
                for (Concept concept : matching) {
                    concept.setMatchStatus(this, MatchStatus.PARTIAL_MATCH);
                }
            } else {
                for (Concept concept : matching) {
                    concept.setMatchStatus(this, MatchStatus.UNIQUE_MATCH);
                }
            }

        }

        public void bind() {
            textbox.addKeyUpHandler(this);
        }
    }

    public SearchHandler makeSearchHandler(TextBox textbox, String color) {
        return new SearchHandlerImpl(textbox, color);
    }

    public List<Concept> getSnapCandidates(DraggableShape dragged) {
        List<Concept> matches = new LinkedList<Concept>();
        Rectangle dbox = dragged.getAbsoluteBBox();
        GWT.log("[CM] getSnapCandidates for " + dbox);
        for (Concept candidate : namedCurves.values()) {
            Rectangle cbox = candidate.getWCurve().getAbsoluteBBox();
            if (dbox.intersects(cbox)) {
                matches.add(candidate);
            }
            GWT.log("[CM] candidate " + candidate.getLabel() + cbox + dbox.intersects(cbox));
        }
        return matches;
    }

    /*
     * ************ Initialisation *****************
     */

    public void koweySetup() {
        AbsolutePanel vPanel = new AbsolutePanel();
        vPanel.getElement().getStyle().setProperty("height", "100%");
        vPanel.getElement().getStyle().setProperty("width", "100%");
        final Button btn = new Button("Start");
        final TextBox searchBox = new TextBox();
        final Label searchBoxCaption = new Label("search:");

        vPanel.add(btn);
        vPanel.add(searchBoxCaption);
        vPanel.add(searchBox);
        btn.addClickHandler(new CreateDiagramHandler(vPanel, this));
        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                btn.removeFromParent();
            }
        });
        makeSearchHandler(searchBox, "orange").bind();
        this.add(vPanel);
    }

    @Override
    public void reload() {
        if (_currentEntity == null) {
            return;
        }
    }

    public Collection<EntityData> getSelection() {
        GWT.log("[CM] get selection");
        return this.selection;
    }

    @Override
    public void setSelection(final Collection<EntityData> selection) {
        GWT.log("[CM] set selection " + selection);
        this.selection = selection;
        notifySelectionListeners(new SelectionEvent(ConceptDiagramPortlet.this));
    }


    /**
     * Called upon (external) concept deletion
     *
     * @param event An event identifying the concept to be removed and its parent
     */
    private void handleParentRemovedEvent(ClassHierarchyParentRemovedEvent event) {
        GWT.log("[CM] handling parent remove " + event.getParent() + ", child: " + event.getChild());
        IRI toRename = event.getChild().getIRI();
        Concept curve = namedCurves.get(toRename);
        if (curve != null) {
            curve.delete();
        }
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

    public void selectClass(@NonNull final IRI iri) {
        // FIXME: bit of cargo culting, not sure why it has to be a SubclassEntityData
        // in particular and not just any old EntityData, but selection doesn't
        // propagate otherwise
        final EntityData entityData =
            new SubclassEntityData(iri.toString(), "", Collections.<EntityData>emptySet(), 0);
        entityData.setValueType(ValueType.Cls);
        setSelection(Collections.singleton(entityData));
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
                new DeleteClassHandler(iri));

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
                new PropertyValueList(Collections.singleton(labelAnnoPair));

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

    @RequiredArgsConstructor
    class DeleteClassHandler extends AbstractAsyncHandler<DeleteEntityResult> {
        final IRI iri;

        @Override
        public void handleFailure(final Throwable caught) {
            GWT.log("[CM] Error at deleting class", caught);
            MessageBox.showErrorMessage("Class not deleted", caught);
        }

        @Override
        public void handleSuccess(final DeleteEntityResult result) {
            GWT.log("[CM] Delete successfully class ", null);;
            onDeleteClass(iri);
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
            }
        }

    }

    public static native void gwtjsPlumbConnect(JavaScriptObject pairs) /*-{
            $wnd.gwtjsconnect(pairs);

        }-*/;


}


