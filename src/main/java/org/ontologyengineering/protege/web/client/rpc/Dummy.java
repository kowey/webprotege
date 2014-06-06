package org.ontologyengineering.protege.web.client.rpc;

import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import lombok.*;

import java.io.Serializable;

/**
 * Just a demonstration class to illustrate serializability
 */
@AllArgsConstructor(staticName = "of")
@ToString
public class Dummy implements Serializable {
    @Getter @NonNull private ProjectId projectId;
    @NonNull private String msg;
    private int num;

    private Dummy() {} /* for GWT serialization */
}
