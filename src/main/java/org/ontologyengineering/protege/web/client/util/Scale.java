package org.ontologyengineering.protege.web.client.util;

import lombok.Data;
import lombok.NonNull;

@Data
public class Scale {
    private final float x;
    private final float y;

    public String toString() {
        return x + "×" + y;
    }

    public Size transform(@NonNull final Size sz) {
        return new Size(Math.round(x * sz.getWidth()), Math.round(y * sz.getHeight()));
    }
}