package org.ontologyengineering.protege.web.client.rpc;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import org.ontologyengineering.protege.web.client.ui.pattern.Curve;

import java.io.IOException;

/**
 *
 */
@RemoteServiceRelativePath("conceptdiagram")
public interface ConceptDiagramService extends RemoteService {
    void saveCurve(ProjectId projectId, Curve curve) throws IOException;
    void saveDummy(ProjectId projectId, Dummy dummy) throws IOException;

    Curve fetchDummy(ProjectId projectId) throws IOException;
}
