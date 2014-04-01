package org.ontologyengineering.protege.web.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;

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

    public void logHello(Dummy msg, AsyncCallback<Void> callback) {
        proxy.logHello(msg, callback);
    }

    public void fetchDummy(ProjectId projectId, AsyncCallback<Dummy> callback) {
        proxy.fetchDummy(projectId, callback);
    }
}
