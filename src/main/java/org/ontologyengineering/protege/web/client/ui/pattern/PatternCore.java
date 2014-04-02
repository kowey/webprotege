package org.ontologyengineering.protege.web.client.ui.pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Serializable heart of {@link org.ontologyengineering.protege.web.client.ui.pattern.Pattern}
 */
@Getter
@Setter
@AllArgsConstructor
public class PatternCore implements Serializable {
    protected int height;
    protected int width;

    /**
     * Do not use this constructor; it is for serialization purposes only
     */
    protected PatternCore() {}
}