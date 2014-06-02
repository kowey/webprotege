package org.ontologyengineering.protege.web.client.ui.dragsnap;

import com.google.gwt.user.client.ui.AbsolutePanel;
import lombok.Getter;
import lombok.NonNull;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableShape;
import org.ontologyengineering.protege.web.client.util.Position;

/**
 * Created by eykk10 on 4/23/14.
 */
@Getter
public abstract class Endpoint {

    @NonNull
    protected final DraggableShape curve;
    // not meant to be selectable, just provides a visual
    // hint to the existence of this object
    @NonNull
    protected final DraggableShape ghost;

    @NonNull
    protected final String idSuffix;

    // initial x/y coordinates for the curve (absolute)
    protected final Position home;

    public Endpoint(@NonNull DraggableShape curve,
                    @NonNull DraggableShape ghost,
                    @NonNull String idSuffix,
                    @NonNull Position topLeft) {
        this.home = topLeft;
        this.curve = curve;
        this.ghost = ghost;
        this.idSuffix = idSuffix;
    }

    public void onLoad(@NonNull AbsolutePanel container) {
        container.add(curve);
        container.add(ghost);
        ghost.getElement().setId(getGhostId());
        curve.getElement().setId(getCurveId());
        bind();
        reset();
    }

    public String getCurveId() {
        return this.idSuffix;
    }

    public String getGhostId() {
        return "ghost_" + this.idSuffix;
    }

    public abstract void update();

    /**
     * Bring the curve back into its template
     * (this is also used for curve initialisation)
     */
    abstract protected void withdrawCurve();

    public abstract void reset();

    public abstract void bind();
}
