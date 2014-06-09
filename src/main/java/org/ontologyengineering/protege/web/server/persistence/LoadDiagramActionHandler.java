package org.ontologyengineering.protege.web.server.persistence;

import edu.stanford.bmir.protege.web.server.dispatch.ActionHandler;
import edu.stanford.bmir.protege.web.server.dispatch.ExecutionContext;
import edu.stanford.bmir.protege.web.server.dispatch.RequestContext;
import edu.stanford.bmir.protege.web.server.dispatch.RequestValidator;
import edu.stanford.bmir.protege.web.server.dispatch.validators.UserHasProjectWritePermissionValidator;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.DiagramNub;
import org.ontologyengineering.protege.web.shared.persistence.LoadDiagramAction;
import org.ontologyengineering.protege.web.shared.persistence.LoadDiagramResult;

import java.io.*;

public class LoadDiagramActionHandler implements ActionHandler<LoadDiagramAction, LoadDiagramResult> {
    public RequestValidator<LoadDiagramAction> getRequestValidator(LoadDiagramAction action, RequestContext requestContext) {
        return UserHasProjectWritePermissionValidator.get();
    }

    @Override
    public LoadDiagramResult execute(LoadDiagramAction action,
                                     ExecutionContext executionContext) {
        final File dataFile = Util.getDataFile(action.getProjectId());
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile));
            try {
                DiagramNub diagram = (DiagramNub) ois.readObject();
                return new LoadDiagramResult(diagram);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } finally {
                ois.close();
            }
        } catch (FileNotFoundException e) {
            return new LoadDiagramResult(new DiagramNub());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Class<LoadDiagramAction> getActionClass() {
        return LoadDiagramAction.class;
    }
}
