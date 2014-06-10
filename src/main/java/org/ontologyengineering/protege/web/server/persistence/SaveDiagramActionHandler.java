package org.ontologyengineering.protege.web.server.persistence;

import edu.stanford.bmir.protege.web.client.dispatch.VoidResult;
import edu.stanford.bmir.protege.web.server.dispatch.ActionHandler;
import edu.stanford.bmir.protege.web.server.dispatch.ExecutionContext;
import edu.stanford.bmir.protege.web.server.dispatch.RequestContext;
import edu.stanford.bmir.protege.web.server.dispatch.RequestValidator;
import edu.stanford.bmir.protege.web.server.dispatch.validators.UserHasProjectWritePermissionValidator;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.DiagramNub;
import org.ontologyengineering.protege.web.shared.persistence.SaveDiagramAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class SaveDiagramActionHandler implements ActionHandler<SaveDiagramAction, VoidResult> {
    public RequestValidator<SaveDiagramAction> getRequestValidator(SaveDiagramAction action, RequestContext requestContext) {
        return UserHasProjectWritePermissionValidator.get();
    }

    @Override
    public VoidResult execute(SaveDiagramAction action, ExecutionContext executionContext) {
        final DiagramNub diagram = action.getDiagram();
        final File dataFile = Util.getDataFile(action.getProjectId());

        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile));
            oos.writeObject(diagram);
            oos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return VoidResult.get();
    }

    @Override
    public Class<SaveDiagramAction> getActionClass() {
        return SaveDiagramAction.class;
    }
}
