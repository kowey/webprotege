package org.ontologyengineering.protege.web.server.persistence;

import edu.stanford.bmir.protege.web.server.owlapi.OWLAPIProjectFileStore;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;

import java.io.File;

/**
 * Helpers for saving and loading diagrams
 */
class Util {
    static final String DATA_DIRECTORY_NAME = "conceptdiagram-data";
    static final String DIAGRAM_STATE_FILE_NAME = "conceptdiagram-data.binary";

    static File getDataFile(ProjectId projectId) {
        OWLAPIProjectFileStore repository = OWLAPIProjectFileStore.getProjectFileStore(projectId);
        final File dataDirectory = new File(repository.getProjectDirectory(),
                DATA_DIRECTORY_NAME);
        dataDirectory.mkdirs();
        return new File(dataDirectory, DIAGRAM_STATE_FILE_NAME);
    }
}
