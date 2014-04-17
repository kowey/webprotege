package org.ontologyengineering.protege.web.client.ui.dragsnap;

import com.google.common.base.Optional;
import com.google.gwt.user.client.ui.Widget;
import lombok.*;
import org.ontologyengineering.protege.web.client.effect.AttributeLayers;
import org.ontologyengineering.protege.web.client.effect.Key;
import org.ontologyengineering.protege.web.client.effect.Painter;
import org.ontologyengineering.protege.web.client.effect.VisualEffect;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableShape;

import java.util.*;

@Getter
@RequiredArgsConstructor
abstract public class Effects extends AttributeLayers {
    // widgets that are not contained in the template frame necessarily
    @NonNull final Collection<Widget> widgets;

    final Map<DraggableShape, VisualEffect> curveEffects = new HashMap();


    @NonNull
    protected VisualEffect activePattern(@NonNull final DraggableShape curve,
                                         @NonNull final String color) {
        VisualEffect effect = new VisualEffect("property template hover (" + color + ")");
        effect.setAttribute(curve, "stroke", color, "black");
        effect.setAttribute(curve, "stroke-width", "3", "1");
        return effect;
    }

    @NonNull
    public VisualEffect ghostPattern(@NonNull final DraggableShape curve) {
        VisualEffect effect = new VisualEffect("property template ghost");
        effect.setAttribute(curve, "stroke", "gray", "gray");
        effect.setAttribute(curve, "stroke-dasharray", ".", ".");
        return effect;
    }

    public void setEffect(@NonNull final DraggableShape curve,
                          @NonNull final Optional<VisualEffect> newEffect) {
        setContextEffect(curveEffects, curve, newEffect);
    }

    public void applyAttributes() {
        applyAttributes(new Painter() {
            @Override
            public void apply(Key key, String value) {
                final Object obj = key.getObject();
                final String attr = key.getAttribute();
                if (widgets.contains(obj)) {
                    DraggableShape curve = (DraggableShape) obj;
                    curve.attr(attr, value);
                }
            }
        });
    }

    abstract public void clear();
    abstract public void addActiveCurve(Endpoint endpoint);
    abstract public void removeActiveCurve(Endpoint endpoint);
}