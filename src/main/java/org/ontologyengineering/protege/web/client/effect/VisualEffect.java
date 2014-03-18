package org.ontologyengineering.protege.web.client.effect;

import lombok.Data;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * See {@link org.ontologyengineering.protege.web.client.effect.AttributeLayers}
 */
@Data
public class VisualEffect {


    final private Map<Key, String> attributes = new HashMap();
    final private Map<Key, String> defaults = new HashMap();

    public void setAttribute(@NonNull final Key attribute,
                             @NonNull final String value,
                             @NonNull final String defaultValue) {
        attributes.put(attribute, value);
        defaults.put(attribute, defaultValue);
    }

    public void setAttribute(@NonNull final Object object,
                             @NonNull final String attribute,
                             @NonNull final String value,
                             @NonNull final String defaultValue) {
        setAttribute(new Key(object, attribute), value, defaultValue);
    }



}