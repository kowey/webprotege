package org.ontologyengineering.protege.web.shared.persistence;

import edu.stanford.bmir.protege.web.shared.HasProjectId;
import edu.stanford.bmir.protege.web.shared.dispatch.Action;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class LoadDiagramAction implements Action<LoadDiagramResult>, HasProjectId {
    private ProjectId projectId;

    private LoadDiagramAction() {
    }

    public LoadDiagramAction(final ProjectId projectId) {
        this.projectId = projectId;
    }
}
