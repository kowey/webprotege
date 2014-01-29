package edu.stanford.bmir.protege.web.client.dispatch.actions;

import edu.stanford.bmir.protege.web.shared.HasProjectId;
import edu.stanford.bmir.protege.web.shared.HasUserId;
import edu.stanford.bmir.protege.web.shared.dispatch.Result;
import edu.stanford.bmir.protege.web.shared.project.ProjectDetails;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.user.UserId;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 05/04/2013
 */
public class LoadProjectResult implements Result, HasUserId, HasProjectId {

    private UserId userId;

    private ProjectDetails projectDetails;

    /**
     * For serialization purposes only
     */
    private LoadProjectResult() {

    }

    public LoadProjectResult(UserId loadedBy, ProjectDetails projectDetails) {
        this.userId = loadedBy;
        this.projectDetails = projectDetails;
    }

    @Override
    public UserId getUserId() {
        return userId;
    }

    public ProjectId getProjectId() {
        return projectDetails.getProjectId();
    }

    public ProjectDetails getProjectDetails() {
        return projectDetails;
    }

}
