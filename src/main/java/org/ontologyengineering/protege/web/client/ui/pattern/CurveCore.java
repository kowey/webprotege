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
@RequiredArgsConstructor
public class CurveCore implements Serializable {
    private int rounding = 20;
    /*gwtnofinal*/ @NonNull protected String id;

    @NonNull Optional<String> label = Optional.absent();
    @NonNull Optional<IRI> iri = Optional.absent();

    /**
     * Do not use this constructor; it is for serialization purposes only
     */
    private CurveCore() {}
}