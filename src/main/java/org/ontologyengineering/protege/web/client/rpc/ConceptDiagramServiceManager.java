package org.ontologyengineering.protege.web.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.Diagram;

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

    public void saveDiagram(ProjectId projectId,
                            Diagram curve,
                            AsyncCallback<Void> callback) {
        proxy.saveDiagram(projectId, curve, callback);
    }

    public void loadDiagram(ProjectId projectId,
                            AsyncCallback<Diagram> callback) {
        proxy.loadDiagram(projectId, callback);
    }
}
