package org.ontologyengineering.protege.web.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import org.ontologyengineering.protege.web.client.ui.pattern.Curve;

import java.io.FileNotFoundException;
import java.io.IOException;

public class ConceptDiagramServiceManager implements ConceptDiagramServiceAsync {
    private static ConceptDiagramServiceAsync proxy;
    static ConceptDiagramServiceManager instance;

    private ConceptDiagramServiceManager() {
        proxy = GWT.create(ConceptDiagramService.class);
    }

    public static ConceptDiagramServiceManager getInstance() {
        if (instance == null) {
            instance = new ConceptDiagramServiceManager();
        }
        return instance;

    }

    public void saveCurve(ProjectId projectId, Curve curve, AsyncCallback<Void> callback) {
        proxy.saveCurve(projectId, curve, callback);
    }

    public void saveDummy(ProjectId projectId, Dummy dummy, AsyncCallback<Void> callback) {
        proxy.saveDummy(projectId, dummy, callback);
    }

    public void fetchDummy(ProjectId projectId, AsyncCallback<Curve> callback) {
        proxy.fetchDummy(projectId, callback);
    }
}
