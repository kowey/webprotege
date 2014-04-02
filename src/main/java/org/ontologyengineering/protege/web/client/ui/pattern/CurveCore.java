package org.ontologyengineering.protege.web.client.ui.pattern;

import com.google.common.base.Optional;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.semanticweb.owlapi.model.IRI;

import java.io.Serializable;

/**
 * Serializable heart of {@link Curve}
 */
@Getter
@Setter
public class CurveCore extends PatternCore implements Serializable {
    private int rounding;
    /*gwtnofinal*/ @NonNull protected String id;

    @NonNull Optional<String> label = Optional.absent();
    @NonNull Optional<IRI> iri = Optional.absent();

    public CurveCore(final String id) {
        this(id, 20);
    }

    public CurveCore(final String id,
                     final int rounding) {
        super();
        this.id = id;
        this.rounding = rounding;
    }
    /**
     * Do not use this constructor; it is for serialization purposes only
     */
    private CurveCore() {
        super();
    }


}