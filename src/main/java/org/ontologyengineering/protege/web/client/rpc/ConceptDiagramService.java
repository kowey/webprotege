package org.ontologyengineering.protege.web.client.rpc;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.Diagram;

import java.io.IOException;

/**
 *
 */
@RemoteServiceRelativePath("conceptdiagram")
public interface ConceptDiagramService extends RemoteService {

    void saveDiagram(ProjectId projectId, Diagram diagram) throws IOException;

    Diagram loadDiagram(ProjectId projectId) throws IOException;
}
