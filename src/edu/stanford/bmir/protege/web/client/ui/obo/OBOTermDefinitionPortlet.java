package edu.stanford.bmir.protege.web.client.ui.obo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.gwtext.client.widgets.MessageBox;
import edu.stanford.bmir.protege.web.client.model.Project;
import edu.stanford.bmir.protege.web.client.rpc.data.EntityData;
import edu.stanford.bmir.protege.web.client.rpc.data.NotSignedInException;
import edu.stanford.bmir.protege.web.client.rpc.data.ProjectId;
import edu.stanford.bmir.protege.web.client.rpc.data.obo.OBOTermDefinition;
import edu.stanford.bmir.protege.web.client.rpc.data.primitive.Entity;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 20/05/2012
 */
public class OBOTermDefinitionPortlet extends AbstractOBOTermPortlet {

    private OBOTermDefinitionEditor editor;

    public OBOTermDefinitionPortlet(Project project) {
        super(project);
        OBOTermEditorView editorView = new OBOTermEditorView(Arrays.asList(new OBOTermEditorGroup(Arrays.<OBOTermEditor>asList(editor))));
        add(editorView);
        setHeight("200px");
        setAutoScroll(false);
    }

    @Override
    protected boolean isDirty() {
        return editor.isDirty();
    }

    @Override
    protected void clearDisplay() {
        editor.clearValue();
    }

    @Override
    protected void commitChangesForEntity(Entity entity) {
        getService().setDefinition(getProjectId(), entity, editor.getValue(), new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
                if(caught instanceof NotSignedInException) {
                    MessageBox.alert("Your changes to the term definition have not been saved.  You must be signed in to make changes.");
                }
                else {
                    MessageBox.alert(caught.getMessage());
                    GWT.log(caught.getMessage(), caught);
                }

            }

            public void onSuccess(Void result) {

            }
        });
    }

    @Override
    protected void displayEntity(Entity entity) {
        getService().getDefinition(new ProjectId(getProject().getProjectName()), entity, new AsyncCallback<OBOTermDefinition>() {
            public void onFailure(Throwable caught) {
                MessageBox.alert(caught.getMessage());
            }

            public void onSuccess(OBOTermDefinition result) {
                if (result == null) {
                    editor.clearValue();
                }
                else {
                    editor.setValue(result);
                }
                updateTitle();
            }
        });
    }

    protected void updateTitle() {
        EntityData currentEntity = getEntity();
        StringBuilder sb = new StringBuilder();
        sb.append("Definition");
        if(currentEntity != null) {
            sb.append(" for ");
            sb.append(currentEntity.getBrowserText());
        }
        setTitle(sb.toString());
    }

    @Override
    public void initialize() {
        editor = new OBOTermDefinitionEditor();
    }

    public Collection<EntityData> getSelection() {
        return Collections.emptySet();
    }
}