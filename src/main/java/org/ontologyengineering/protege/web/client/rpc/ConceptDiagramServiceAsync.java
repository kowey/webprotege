package org.ontologyengineering.protege.web.client.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface ConceptDiagramServiceAsync extends RemoteService {
    void logHello(Dummy msg, AsyncCallback<Void> callback);
    void fetchDummy(ProjectId projectId, AsyncCallback<Dummy> callback);
}
