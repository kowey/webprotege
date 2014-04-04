package org.ontologyengineering.protege.web.client.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/**
 * Width/height tuple
 */
@AllArgsConstructor
@Getter
public class Position implements Serializable {
    /*gwtnofinal*/ private int x;
    /*gwtnofinal*/ private int y;

    public String toString() {
        return x + "x × " + y + "y";
    }

    private Position() {
        this(0,0);
    }
}
