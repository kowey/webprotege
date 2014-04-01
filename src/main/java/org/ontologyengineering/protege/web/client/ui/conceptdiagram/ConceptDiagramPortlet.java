package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
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
import org.ontologyengineering.protege.web.client.ConceptDiagram;
import org.ontologyengineering.protege.web.client.rpc.ConceptDiagramService;
import org.ontologyengineering.protege.web.client.rpc.ConceptDiagramServiceManager;
import org.ontologyengineering.protege.web.client.rpc.Dummy;
import org.ontologyengineering.protege.web.client.ui.pattern.Curve;
import org.ontologyengineering.protege.web.client.ui.pattern.Pattern;
import org.ontologyengineering.protege.web.client.ui.pattern.Subsumption;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableShape;
import org.ontologyengineering.protege.web.client.util.Rectangle;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplNoCompression;

import java.util.*;

public class ConceptDiagramPortlet extends AbstractOWLEntityPortlet implements CurveRegistry, SearchManager {
    private boolean registeredEventHandlers = false;

    final private HashMap<IRI, IRI> immediateParents = new HashMap();
    final private Multimap<IRI, Curve> namedCurves = HashMultimap.create();
    final private BiMap<IRI, String> names = HashBiMap.create();

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
        final HorizontalPanel buttonBar = new HorizontalPanel();

        final Button btn = new Button("Start");
        final Button btnLoad = new Button("Load");
        final Button btnSave = new Button("Save");

        final TextBox searchBox = new TextBox();
        final Label searchBoxCaption = new Label("search:");

        buttonBar.add(btn);
        buttonBar.add(btnLoad);
        buttonBar.add(btnSave);
        vPanel.add(buttonBar);
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

        btnLoad.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                loadDiagram();
            }
        });

        btnSave.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                saveDiagram();
            }
        });
        makeSearchHandler(searchBox, "orange").bind();
        return vPanel;
    }

    private void saveDiagram() {
        ConceptDiagramServiceManager.getInstance().logHello(Dummy.of(getProjectId(), "blah", 32), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                GWT.log("[Concept diagram] logHello failed!");
            }

            @Override
            public void onSuccess(Void result) {
                GWT.log("[Concept diagram] logHello should have succeeded!");
            }
        });
    }

    private void loadDiagram() {
        ConceptDiagramServiceManager.getInstance().fetchDummy(getProjectId(), new AsyncCallback<Dummy>() {
            @Override
            public void onFailure(Throwable caught) {
                GWT.log("[Concept diagram] fetchDummy failed!");
            }

            @Override
            public void onSuccess(Dummy result) {
                GWT.log("[Concept diagram] fetchDummy got" + result);
            }
        });
    }


    // draw the pattern templates
    private void initTemplates(AbsolutePanel vPanel) {
        vPanel.add(new Label("Drag one of these templates out to instantiate it"));

        Curve curveTemplate = new Curve("curve-template", this, this);

        final List<Pattern> patterns =
                Arrays.<Pattern>asList(
                curveTemplate,
                new Subsumption("subsume-template", this, this, vPanel));

        final int yGap = 20;
        final int templateX = 0;
        final int templateY = 3 * yGap;

        int currentY = templateY;
        for (Pattern pattern : patterns) {
            vPanel.add(pattern, templateX, currentY);
            currentY += pattern.getOffsetHeight() + yGap;
        }
        curveTemplate.startTemplateMode();
        curveTemplate.copyTemplate(vPanel);

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

        boolean hasSearch = false;

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
     * TODO - if we handle this, we can handle Deletion events, but we also
     * catch move events (and in the current implementation, delete them,
     * which is not cool)
     *
     * @param event An event identifying the concept to be removed and its parent
     */
    private void handleParentRemovedEvent(ClassHierarchyParentRemovedEvent event) {
        /*
        GWT.log("[CM] handling parent remove " + event.getParent() + ", child: " + event.getChild());


        IRI toRename = event.getChild().getIRI();
        for (Curve curve : namedCurves.get(toRename)) {
            curve.delete();
        }
        */
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
                curve.rename(newName);
            }
        }
    }

    /*
     * ************ [Active] curve (re)naming, deletion **************
     */

    /**
     * Assumed to be owl:Thing if not known
     */
    public IRI getImmediateParent(@NonNull IRI iri) {
        if (immediateParents.containsKey(iri)) {
            return immediateParents.get(iri);
        } else {
            return DataFactory.getOWLThing().getIRI();
        }
    }

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

    public void moveClass(@NonNull final IRI cls,
                          @NonNull final IRI oldParent,
                          @NonNull final IRI newParent) {

        final String desc = "Moving " + cls + " from " + oldParent + " to " + newParent;
        OntologyServiceManager.getInstance().moveCls(getProjectId(),
                cls.toString(), oldParent.toString(), newParent.toString(),
                false, Application.get().getUserId(), desc,
                new MoveClassHandler(cls, newParent));
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


    public void changeCurveName(@NonNull final Curve curve,
                                @NonNull final Optional<String> before,
                                @NonNull final Optional<String> after) {
        GWT.log("[CurveRegistry] changeCurveName from " + before.or("NIL") + " to " + after.or("NIL"));
        if (before == after) {
            return;
        } else if (!before.isPresent()) {
            assignCurveName(curve, after.get());
        } else if (!after.isPresent()) {
            removeCurveName(curve);
        } else {
            renameCurveOnly(curve, before.get(), after.get());
        }

    }

    public void removeCurveName(@NonNull final Curve curve) {
        GWT.log("[CurveRegistry] removeCurveName " + curve.getLabel().or("NIL"));
        if (curve.getIri().isPresent()) {
            final IRI iri = curve.getIri().get();
            Collection<Curve> named = namedCurves.get(iri);
            GWT.log("[CurveRegistry] ...removeCurveName tracking " + named.size());
            // Preconditions not supported in GWT?
            //checkState(!named.isEmpty(),
            //        "Tried to delete a curve we have an IRI, yet don't know about");
            if (named.size() == 1) { // all by myself...
                deleteClass(iri);
            } else {
                namedCurves.remove(iri, curve);
            }
        }
    }


    /**
     * Precondition: curve must not already have a name
     *
     * @param curve
     * @param name
     */
    private void assignCurveName(@NonNull final Curve curve,
                                 @NonNull final String name) {
        // Preconditions not GWT-compatible?
        //checkArgument(! curve.getIri().isPresent(),
        //        "curve must not already have a name");
        boolean nameExists = names.inverse().containsKey(name);
        if (nameExists) {
            assignCurveIdentity(curve, names.inverse().get(name), name);
        } else {
            createClass(curve, name);
        }
    }

    /**
     * This should be functionally equivalent to removeCurveName
     * followed by assignCurveName, but it's separate because I'd
     * like to avoid making unneeded calls to the server
     *
     * @param curve
     * @param oldName
     * @param newName
     */
    private void renameCurveOnly(@NonNull final Curve curve,
                                @NonNull final String oldName,
                                @NonNull final String newName) {

        final IRI oldIri = names.inverse().get(oldName);
        final IRI newIri = names.inverse().get(newName);

        if (oldIri == null) {
            return; // TODO: should this be considered Exception-worthy?
        }

        final Collection<Curve> hasOldName = namedCurves.get(oldIri);

        boolean isAlone = hasOldName.size() == 1;
        boolean newNameExists = names.inverse().containsKey(newName);

        if (newNameExists) {
            namedCurves.remove(oldIri, curve);
            assignCurveIdentity(curve, newIri, newName);
            if (isAlone) {
                deleteClass(oldIri);
            }
        } else {
            if (isAlone) {
                renameClass(oldIri, oldName, newName);
            } else {
                createClass(curve, newName);
            }
        }
    }

    private void assignCurveIdentity(Curve curve, IRI iri, String name) {
        GWT.log("[CurveRegistry] assignIdentity " + name + " to curve " + curve.getId() + " [" + iri + "]");
        namedCurves.put(iri, curve);
        curve.setIri(Optional.of(iri));
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
        DispatchServiceManager.get().execute(updateAction,
                new RenameClassHandler(iri, oldName, newName));
    }

    /**
     * Culmination of a delete operation
     * @param iri
     */
    private void onDeleteSuccess(@NonNull final IRI iri) {
        namedCurves.removeAll(iri);
        names.remove(iri);
    }

    private void onCreateSuccess(@NonNull final IRI iri,
                                 @NonNull final String name,
                                 @NonNull final Curve curve) {
        assignCurveIdentity(curve, iri, name);
        names.put(iri, name);
    }

    private void onRenameSuccess(@NonNull final IRI iri,
                                 @NonNull final String oldName,
                                 @NonNull final String newName) {
        names.forcePut(iri, newName);
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
            onCreateSuccess(createdIri, desiredName, curve);
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
            GWT.log("[CM] Delete successfully class ", null);
            onDeleteSuccess(iri);
        }
    }

    @RequiredArgsConstructor
    public class MoveClassHandler extends AbstractAsyncHandler<List<EntityData>> {

        private final IRI cls;
        private final IRI newParent;

        @Override
        public void handleFailure(final Throwable caught) {
            GWT.log("[CM] Error at moving class", caught);
            MessageBox.showErrorMessage("Class not moved", caught);
            // TODO: refresh oldParent and newParent
        }

        @Override
        public void handleSuccess(final List<EntityData> result) {
            GWT.log("[CM] Moved successfully class " + cls, null);
            if (result == null) {
                //MessageBox.alert("Success", "Class moved successfully.");
                immediateParents.put(cls, newParent);
            }
            else {
                warnAboutCycles(result);
            }

        }

        private void warnAboutCycles(List<EntityData> classes) {
            GWT.log("Cycle warning after moving class " + cls + ": " + classes, null);

            String warningMsg = "<B>WARNING! There is a cycle in the hierarchy: </B><BR><BR>";
            for (EntityData p : classes) {
                warningMsg += "&nbsp;&nbsp;&nbsp;&nbsp;" + p.getBrowserText() + "<BR>";
            }
            warningMsg += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ...";
            MessageBox.showAlert("Cycles introduced during class move", "Class moved successfully.<BR>" +
                    "<BR>" +
                    warningMsg);
        }
    }

    @RequiredArgsConstructor
    class RenameClassHandler extends AbstractAsyncHandler<Result> {
        final IRI iri;
        final String oldName;
        final String newName;

        @Override
        public void handleFailure(Throwable caught) {
            GWT.log("[CM] Updating object failed", caught);
        }

        @Override
        public void handleSuccess(Result result) {
            GWT.log("[CM] Object successfully renamed");
            onRenameSuccess(iri, oldName, newName);
        }
    }

    // we are keeping this around in the event that we want to change the IRI
    // of a curve, something we currently do not touch (2014-03-21)
    @RequiredArgsConstructor
    class ChangeIriHandler extends AbstractAsyncHandler<EntityData> {
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


