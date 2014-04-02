package org.ontologyengineering.protege.web.client.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter

/**
 * Width/height tuple
 */
public class Position {
    private final int x;
    private final int y;

    public String toString() {
        return x + "x × " + y + "y";
    }
}
