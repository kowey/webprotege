package org.ontologyengineering.protege.web.client.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter

/**
 * Width/height tuple
 */
public class Size {
    private final int width;
    private final int height;

    public String toString() {
        return width + "w × " + height + "h";
    }
}
