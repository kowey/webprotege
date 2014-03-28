package org.ontologyengineering.protege.web.client.effect;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * VisualEffect key: tuple of object it applies on, and the attribute
 */
@RequiredArgsConstructor
@Getter
public class Key {
    final private Object object;
    final private String attribute;

    public String toString() {
        return ("[" + object.getClass() + "@" + object.hashCode() + "] " + attribute);
    }

}
