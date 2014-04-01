package org.ontologyengineering.protege.web.client.ui.pattern;

import com.google.common.base.Optional;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.ontologyengineering.protege.web.client.util.Position;
import org.semanticweb.owlapi.model.IRI;

import java.io.Serializable;

/**
 * Serializable heart of {@link Curve}
 */
@Getter
@Setter
public class CurveCore extends PatternCore implements Serializable {
    protected int rounding;

    @NonNull protected Position position;
    @NonNull Optional<String> label = Optional.absent();
    @NonNull Optional<IRI> iri = Optional.absent();

    public CurveCore() {
        this(20, new Position(0,0));
    }

    public CurveCore(final int rounding,
                     @NonNull final Position position) {
        super();
        this.rounding = rounding;
        this.position = position;
    }
}