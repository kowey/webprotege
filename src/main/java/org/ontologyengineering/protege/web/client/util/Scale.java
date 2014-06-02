package org.ontologyengineering.protege.web.client.util;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter @Setter
public class Scale {
    private final float x;
    private final float y;

    @java.beans.ConstructorProperties({"x", "y"})
    public Scale(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Scale(double x, double y) {
        this((float)x, (float)y);
    }

    public String toString() {
        return x + "×" + y;
    }

    public Size transform(@NonNull final Size sz) {
        return new Size(Math.round(x * sz.getWidth()), Math.round(y * sz.getHeight()));
    }
}