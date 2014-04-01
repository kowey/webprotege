package org.ontologyengineering.protege.web.client.rpc;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;

import java.io.IOException;

/**
 *
 */
@RemoteServiceRelativePath("conceptdiagram")
public interface ConceptDiagramService extends RemoteService {
    void logHello(Dummy msg) throws IOException;
    Dummy fetchDummy(ProjectId projectId) throws IOException;
}
