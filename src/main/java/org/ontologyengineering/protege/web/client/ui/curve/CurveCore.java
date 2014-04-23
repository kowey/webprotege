package org.ontologyengineering.protege.web.client.ui.curve;

import com.google.common.base.Optional;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.ontologyengineering.protege.web.client.util.Position;
import org.ontologyengineering.protege.web.client.util.Size;
import org.semanticweb.owlapi.model.IRI;

import java.io.Serializable;

/**
 * Serializable heart of {@link Curve}
 */
@Getter
@Setter
public class CurveCore implements Serializable {
    protected int width = 120;
    protected int height = 80;
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

    public Size getSize() {
        return new Size(width, height);
    }

    public void setSize(@NonNull final Size sz) {
        this.width = sz.getWidth();
        this.height = sz.getHeight();
    }
}