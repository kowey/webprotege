package org.ontologyengineering.protege.web.shared.persistence;

import edu.stanford.bmir.protege.web.client.dispatch.VoidResult;
import edu.stanford.bmir.protege.web.shared.HasProjectId;
import edu.stanford.bmir.protege.web.shared.dispatch.Action;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import lombok.Getter;
import lombok.ToString;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.DiagramNub;

@Getter @ToString
public class SaveDiagramAction implements Action<VoidResult>, HasProjectId {
    private ProjectId projectId;
    private DiagramNub diagram;

    private SaveDiagramAction() {}

    public SaveDiagramAction(final ProjectId projectId,
                             final DiagramNub diagram) {
        this.projectId = projectId;
        this.diagram = diagram;
    }
}
