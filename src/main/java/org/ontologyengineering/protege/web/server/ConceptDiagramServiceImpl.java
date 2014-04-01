package org.ontologyengineering.protege.web.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import edu.stanford.bmir.protege.web.server.owlapi.OWLAPIProjectFileStore;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.smi.protege.util.Log;
import org.ontologyengineering.protege.web.client.rpc.ConceptDiagramService;
import org.ontologyengineering.protege.web.client.rpc.Dummy;
import org.ontologyengineering.protege.web.client.ui.pattern.CurveCore;

import java.io.*;

public class ConceptDiagramServiceImpl extends RemoteServiceServlet implements ConceptDiagramService {
    static int counter = 0;
    private static final String DATA_DIRECTORY_NAME = "conceptdiagram-data";
    private static final String DIAGRAM_STATE_FILE_NAME = "conceptdiagram-data.binary";

    public void saveCurve(ProjectId projectId, CurveCore curve) throws IOException {
        Log.getLogger().info("{CONCEPT DIAGRAM HELLO} " + counter + ":" + curve.getIri());
        counter++;

        final File dataFile = getDataFile(projectId);
        Log.getLogger().info("{CONCEPT DIAGRAM...}" + dataFile);

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile));
        oos.writeObject(curve);
        oos.close();
    }

    public void saveDummy(ProjectId projectId, Dummy dummy) throws IOException {
        Log.getLogger().info("{CONCEPT DIAGRAM HELLO} " + counter + ":" + dummy);
        counter++;

        final File dataFile = getDataFile(projectId);
        Log.getLogger().info("{CONCEPT DIAGRAM...}" + dataFile);

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile));
        oos.writeObject(dummy);
        oos.close();
    }

    public CurveCore fetchDummy(ProjectId projectId) throws IOException {
        final File dataFile = getDataFile(projectId);
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile));
        try {
            CurveCore dummy = (CurveCore) ois.readObject();
            return dummy;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            ois.close();
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
