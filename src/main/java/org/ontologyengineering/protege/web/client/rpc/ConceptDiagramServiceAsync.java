package org.ontologyengineering.protege.web.client.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.Diagram;

public interface ConceptDiagramServiceAsync extends RemoteService {
    void saveDiagram(ProjectId projectId,
                     Diagram diagram,
                     AsyncCallback<Void> callback);

    void loadDiagram(ProjectId projectId,
                     AsyncCallback<Diagram> callback);
}
