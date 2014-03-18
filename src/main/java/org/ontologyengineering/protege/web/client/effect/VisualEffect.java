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

    final private String debugName;

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

    public String toString() {
        Map<Object, String> attrs = new HashMap<Object, String>();
        for (Map.Entry<Key, String> pair : attributes.entrySet()) {
            Object obj = pair.getKey().getObject();
            String attr = pair.getKey().getAttribute();
            String value = pair.getValue();
            String attrValue = attr + ":" + value;

            if (attrs.containsKey(obj)) {
                attrs.put(obj, attrs.get(obj) + " " + attrValue);
            } else {
                attrs.put(obj, attrValue);
            }
        }

        StringBuffer buf = new StringBuffer();
        buf.append(debugName + " :: ");
        for (Map.Entry<Object, String> pair : attrs.entrySet()) {
            Object obj = pair.getKey();
            String key = obj.getClass().getName() + "@" + obj.hashCode();
            String objAttrs = pair.getValue();
            buf.append("[" + key + "] " + objAttrs);
        }
        return buf.toString();
    }

}