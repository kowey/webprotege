package org.ontologyengineering.protege.web.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import edu.stanford.bmir.protege.web.server.owlapi.OWLAPIProjectFileStore;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.smi.protege.util.Log;
import org.ontologyengineering.protege.web.client.rpc.ConceptDiagramService;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.Diagram;

import java.io.*;

public class ConceptDiagramServiceImpl extends RemoteServiceServlet implements ConceptDiagramService {
    static int counter = 0;
    private static final String DATA_DIRECTORY_NAME = "conceptdiagram-data";
    private static final String DIAGRAM_STATE_FILE_NAME = "conceptdiagram-data.binary";

    public void saveDiagram(ProjectId projectId,
                            Diagram diagram) throws IOException {
        Log.getLogger().info("{CONCEPT DIAGRAM HELLO} " + counter + ":" + diagram);
        counter++;

        final File dataFile = getDataFile(projectId);
        Log.getLogger().info("{CONCEPT DIAGRAM...}" + dataFile);

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile));
        oos.writeObject(diagram);
        oos.close();
    }

    public Diagram loadDiagram(ProjectId projectId) throws IOException {
        final File dataFile = getDataFile(projectId);
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile));
            try {
                Diagram diagram = (Diagram) ois.readObject();
                return diagram;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } finally {
                ois.close();
            }
        } catch (FileNotFoundException e) {
            return new Diagram();
        }
    }

    private File getDataFile(ProjectId projectId) {
        OWLAPIProjectFileStore repository = OWLAPIProjectFileStore.getProjectFileStore(projectId);
        final File dataDirectory = new File(repository.getProjectDirectory(),
                DATA_DIRECTORY_NAME);
        dataDirectory.mkdirs();
        return new File(dataDirectory, DIAGRAM_STATE_FILE_NAME);
    }
}
