package org.ontologyengineering.protege.web.shared.persistence;

import edu.stanford.bmir.protege.web.shared.dispatch.Result;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.DiagramNub;

@Getter
public class LoadDiagramResult implements Result {
    private DiagramNub diagram;

    private LoadDiagramResult() {} // for serialization only

    public LoadDiagramResult(@NonNull final DiagramNub diagram) {
        this.diagram = diagram;
    }
}
