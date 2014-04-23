package org.ontologyengineering.protege.web.client.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
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


    public Position add(@NonNull final Position other) {
        return new Position(this.x + other.getX(), this.y + other.getY());
    }

    public Position subtract(@NonNull final Position other) {
        return new Position(this.x - other.getX(), this.y - other.getY());
    }
}
