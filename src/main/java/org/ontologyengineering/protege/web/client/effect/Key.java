package org.ontologyengineering.protege.web.client.effect;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static java.lang.System.identityHashCode;

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

    public boolean equals(Object obj) {
        if (obj.getClass() != this.getClass()) {
            return false;
        } else {
            // for keys, the notion of equality used means "points to the same object"
            // (plus attribute equality as normal)
            Key other = (Key)obj;
            return (this.object == other.object && this.attribute.equals(other.attribute));
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + identityHashCode(object);
        result = prime * result + attribute.hashCode();
        return result;
    }

}
