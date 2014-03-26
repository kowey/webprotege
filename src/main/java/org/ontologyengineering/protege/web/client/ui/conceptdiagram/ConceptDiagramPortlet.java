package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import edu.stanford.bmir.protege.web.client.Application;
import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceManager;
import edu.stanford.bmir.protege.web.client.dispatch.actions.CreateClassAction;
import edu.stanford.bmir.protege.web.client.dispatch.actions.CreateClassResult;
import edu.stanford.bmir.protege.web.client.dispatch.actions.DeleteEntityAction;
import edu.stanford.bmir.protege.web.client.dispatch.actions.DeleteEntityResult;
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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.ontologyengineering.protege.web.client.ConceptManager;
import org.ontologyengineering.protege.web.client.ui.pattern.Curve;
import org.ontologyengineering.protege.web.client.ui.pattern.Pattern;
import org.ontologyengineering.protege.web.client.ui.pattern.Subsumption;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableShape;
import org.ontologyengineering.protege.web.client.util.Rectangle;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplNoCompression;

import java.util.*;


public class ConceptDiagramPortlet extends AbstractOWLEntityPortlet implements ConceptManager, SearchManager {
    static final String DEFAULT_ENTITY_NAME_PREFIX = "http://ontologyengineering.org/uri#";

    private boolean registeredEventHandlers = false;
    final private Multimap<IRI, Curve> namedCurves = HashMultimap.create();
    private Collection<EntityData> selection = Collections.emptyList();

    public ConceptDiagramPortlet(Project project) {
        super(project);
    }

    /*
     * ************ Initialisation *****************
     */

    @Override
    public void initialize() {
        setTitle("ConceptDiagram");
        this.add(createMainPanel());
        registerEventHandlers();
        reload();
    }

    @Override
    public void reload() {
        if (_currentEntity == null) {
            return;
        }
    }

    public AbsolutePanel createMainPanel() {
        final AbsolutePanel vPanel = new AbsolutePanel();
        vPanel.getElement().getStyle().setProperty("height", "100%");
        vPanel.getElement().getStyle().setProperty("width", "100%");
        final Button btn = new Button("Start");
        final TextBox searchBox = new TextBox();
        final Label searchBoxCaption = new Label("search:");

        vPanel.add(btn);
        vPanel.add(searchBoxCaption);
        vPanel.add(searchBox);
        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                try {
                    initTemplates(vPanel);
                } catch (Exception e) {
                    GWT.log("Template initialisation error:", e);
                }
            }
        });
        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                btn.removeFromParent();
            }
        });
        makeSearchHandler(searchBox, "orange").bind();
        return vPanel;
    }

    // draw the pattern templates
    private void initTemplates(AbsolutePanel vPanel) {
        vPanel.add(new Label("Drag one of these templates out to instantiate it"));

        final List<Pattern> templates =
                Arrays.<Pattern>asList(
                new Curve("curve-template", this, this),
                new Subsumption("subsume-template", this, vPanel));

        final int yGap = 20;
        final int templateX = 0;
        final int templateY = 3 * yGap;

        int currentY = templateY;
        for (Pattern template : templates) {
            template.startTemplateMode();
            vPanel.add(template, templateX, currentY);
            currentY += template.getHeight() + yGap;
            template.copyTemplate(vPanel);
        }
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

    /*
     * ************ Searching and snapping *****************
     */

    @Data
    public class SearchHandlerImpl implements KeyUpHandler, SearchHandler {
        @NonNull private final TextBox textbox;
        @NonNull private final String color;

        @NonNull private Collection<Curve> matching = Collections.emptyList();
        @NonNull private Collection<Curve> nonMatching = Collections.emptyList();

        @NonNull boolean hasSearch = false;

        public Optional<Collection<Curve>> getMatching() {
            if (hasSearch) {
                return Optional.of(matching);
            } else {
                return Optional.absent();
            }
        }

        /**
         * Track the latest changes to our search results
         * and propagate visual effects to matching/non-matching
         * results accordingly
         */
        public void update() {
            final String text = textbox.getText().trim();
            setHasSearch(!text.isEmpty());
            ImmutableListMultimap<Boolean, Curve> partitionedMap =
                    Multimaps.index(namedCurves.values(), new Function<Curve, Boolean>() {
                        @Override
                        public Boolean apply(Curve input) {
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

            for (Curve curve : nonMatching) {
                curve.setMatchStatus(this, MatchStatus.NO_MATCH);
            }
            if (matching.size() > 1) {
                for (Curve curve : matching) {
                    curve.setMatchStatus(this, MatchStatus.PARTIAL_MATCH);
                }
            } else {
                for (Curve curve : matching) {
                    curve.setMatchStatus(this, MatchStatus.UNIQUE_MATCH);
                }
            }
        }

        /**
         * Clear the current search and apply visual effects as appropriate
         */
        public void reset() {
            textbox.setText("");
            update();
        }

        @Override
        public void onKeyUp(KeyUpEvent event) {
            update();
        }

        public void bind() {
            textbox.addKeyUpHandler(this);
        }
    }

    public SearchHandler makeSearchHandler(TextBox textbox, String color) {
        return new SearchHandlerImpl(textbox, color);
    }

    public List<Curve> getSnapCandidates(DraggableShape dragged) {
        List<Curve> matches = new LinkedList<Curve>();
        Rectangle dbox = dragged.getAbsoluteBBox();
        GWT.log("[CM] getSnapCandidates for " + dbox);
        for (Curve candidate : namedCurves.values()) {
            Rectangle cbox = candidate.getWCurve().getAbsoluteBBox();
            if (dbox.intersects(cbox)) {
                matches.add(candidate);
            }
            GWT.log("[CM] candidate " + candidate.getLabel() + cbox + dbox.intersects(cbox));
        }
        return matches;
    }

    /*
     * ************ Curve selection *****************
     */

    public void selectClass(@NonNull final IRI iri) {
        // FIXME: bit of cargo culting, not sure why it has to be a SubclassEntityData
        // in particular and not just any old EntityData, but selection doesn't
        // propagate otherwise
        final EntityData entityData =
                new SubclassEntityData(iri.toString(), "", Collections.<EntityData>emptySet(), 0);
        entityData.setValueType(ValueType.Cls);
        setSelection(Collections.singleton(entityData));
    }

    public Collection<EntityData> getSelection() {
        return this.selection;
    }

    @Override
    public void setSelection(final Collection<EntityData> selection) {
        this.selection = selection;
        notifySelectionListeners(new SelectionEvent(ConceptDiagramPortlet.this));
    }

    /*
     * ************ [Passive] Curve (re)naming, deletion *****************
     * handling curve rename/delete events from the outside world
     * *******************************************************************
     */

    /**
     * Called upon (external) concept deletion
     *
     * @param event An event identifying the concept to be removed and its parent
     */
    private void handleParentRemovedEvent(ClassHierarchyParentRemovedEvent event) {
        GWT.log("[CM] handling parent remove " + event.getParent() + ", child: " + event.getChild());
        IRI toRename = event.getChild().getIRI();
        for (Curve curve : namedCurves.get(toRename)) {
            curve.delete();
        }
    }

    /**
     * Called to update the curve label
     * @param event The event that describes the browser text change that happened.
     */
    protected void onEntityBrowserTextChanged(BrowserTextChangedEvent event) {

        IRI toRename = event.getEntity().getIRI();
        Optional<String> newName = Optional.of(event.getNewBrowserText());
        GWT.log("[CM rename] received BrowserTextChangedEvent event " + event +
                " | " + toRename + " to " + newName +
                " | " + event.getSource());
        for (Curve curve : namedCurves.get(toRename)) {
            if (!curve.getLabel().equals(newName)) {
                curve.setLabel(newName);
            }
        }
    }

    /*
     * ************ [Active] curve (re)naming, deletion **************
     */

    public void createClass(@NonNull final Curve curve,
                            @NonNull final String name) {
        GWT.log("[CM] Asked to give the curve " + curve.getId() + " the name " + name);
        DispatchServiceManager.get().execute(
                new CreateClassAction(getProjectId(), name, DataFactory.getOWLThing()),
                getCreateClassAsyncHandler(curve, name));
    }

    public void checkClassName(@NonNull final Curve curve) {
        GWT.log("[CM] Asked to check class name for " + curve.getId() + " | " + curve.getId());
        if (curve.getIri().isPresent()) {
            OntologyServiceManager.getInstance().getRelatedProperties(getProject().getProjectId(), curve.getIri().get().toString(),
                    new GetTriplesHandler(curve));
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


    public void renameCurveOnly(@NonNull final Curve curve,
                                @NonNull final String newName) {

        final IRI newIri = IRI.create(DEFAULT_ENTITY_NAME_PREFIX, newName);
        if (curve.getIri().isPresent()) {
            final IRI oldIri = curve.getIri().get();
            final Collection<Curve> sameName = namedCurves.get(oldIri);
            if (sameName.size() == 1) {
                GWT.log("[CM] Need a full on rename");
                renameClass(oldIri, oldIri.getFragment(), newName);
            } else if (namedCurves.containsKey(newIri)) {
                GWT.log("[CM] Renaming to existing IRI; just a quick job");
                curve.setIri(Optional.of(newIri));
                curve.setLabel(Optional.of(newName));
                namedCurves.remove(oldIri,curve);
                namedCurves.put(newIri, curve);
            } else {
                GWT.log("[CM] Renaming to whole new IRI, bit like creating a class");
                namedCurves.remove(oldIri,curve);
                createClass(curve, newName);
            }
        }
    }

    public void renameClass(@NonNull final IRI iri,
                            @NonNull final String oldName,
                            @NonNull final String newName) {
        if (oldName.equals(newName) || newName == null || newName.length() == 0) {
            return;
        }
        GWT.log("[CM] Invoking rename " + iri + " from " + oldName + " to " + newName, null);
        changeClassEntityName(iri, IRI.create(DEFAULT_ENTITY_NAME_PREFIX, newName));

    }

    protected void changeClassEntityName(final IRI oldName, final IRI newName) {
        GWT.log("Should rename class from " + oldName + " to " + newName, null);
        if (oldName.equals(newName) || newName == null || newName.length() == 0) {
            return;
        }

        OntologyServiceManager.getInstance().renameEntity(getProjectId(),
                oldName.toString(),
                newName.toString(),
                Application.get().getUserId(),
                "Old name: " + oldName + ", New name: " + newName,
                new RenameClassHandler(oldName, newName, newName.getFragment()));
    }

    /**
     * Possible concrete implementation for renaming; change the rdfs:label
     *
     * @param iri
     * @param oldName
     * @param newName
     */
    private void changeClassLabel(@NonNull final IRI iri,
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
        namedCurves.removeAll(iri);
    }

    public void onCreateClass(@NonNull final IRI iri,
                              @NonNull final Curve curve) {
        namedCurves.put(iri, curve);
    }

    /*
     * ************ Remote procedure calls *****************
     */

    protected AbstractAsyncHandler<CreateClassResult> getCreateClassAsyncHandler(Curve curve, String name) {
        return new CreateClassHandler(curve, name);
    }

    @RequiredArgsConstructor
    class CreateClassHandler extends AbstractAsyncHandler<CreateClassResult> {
        final Curve curve;
        final String desiredName;

        @Override
        public void handleFailure(final Throwable caught) {
            GWT.log("[CM] Error at creating class", caught);
            MessageBox.showErrorMessage("Class not created", caught);
        }

        @Override
        public void handleSuccess(final CreateClassResult result) {
            IRI createdIri = result.getObject().getIRI();
            GWT.log("[CM] created object: " + createdIri);
            IRI newIri = IRI.create(DEFAULT_ENTITY_NAME_PREFIX, desiredName);
            if (createdIri.equals(newIri)) {
                onCreateClass(newIri, curve);
            } else {
                OntologyServiceManager.getInstance().renameEntity(getProjectId(),
                    createdIri.toString(),
                    newIri.toString(),
                    Application.get().getUserId(),
                    "Rename on create: " + createdIri + ", New name: " + newIri,
                    new RenameClassOnCreateHandler(curve, newIri));
            }
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

    @RequiredArgsConstructor
    class RenameClassOnCreateHandler extends AbstractAsyncHandler<EntityData> {
        final private Curve curve;
        final private IRI iri;

        @Override
        public void handleFailure(final Throwable caught) {
            GWT.log("[CM] Error renaming newly created class", caught);
        }

        @Override
        public void handleSuccess(final EntityData result) {
            GWT.log("[CM] Successfully renamed newly-created class");
            curve.setIri(Optional.of(iri));
            ConceptDiagramPortlet.this.onCreateClass(iri, curve);
        }

    }

    // we are keeping this around in the event that we want to change the IRI
    // of a curve, something we currently do not touch (2014-03-21)
    @RequiredArgsConstructor
    class RenameClassHandler extends AbstractAsyncHandler<EntityData> {
        final IRI oldIri;
        final IRI newIri;
        final String newLabel;

        @Override
        public void handleFailure(final Throwable caught) {
            GWT.log("[CM] Error renaming class", caught);
            MessageBox.showErrorMessage("Class not deleted", caught);
        }

        @Override
        public void handleSuccess(final EntityData result) {
            GWT.log("[CM] My rename invocation succeeded ", null);
            Collection<Curve> affectedCurves = namedCurves.get(oldIri);
            for (Curve curve : affectedCurves) {
                curve.setIri(Optional.of(newIri));
                curve.setLabel(Optional.of(newLabel));
            }
            namedCurves.removeAll(oldIri);
            namedCurves.putAll(newIri, affectedCurves);
        }
    }

    @RequiredArgsConstructor
    class GetTriplesHandler extends AbstractAsyncHandler<List<Triple>> {
        private final Curve curve;

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

}


