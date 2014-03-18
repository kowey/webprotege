package org.ontologyengineering.protege.web.client.effect;

import lombok.Data;

/**
 * VisualEffect key: tuple of object it applies on, and the attribute
 */
@Data public class Key {
    final private Object object;
    final private String attribute;
}
