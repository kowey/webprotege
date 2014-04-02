package org.ontologyengineering.protege.web.client.ui.pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.ontologyengineering.protege.web.client.util.Size;

import java.io.Serializable;

/**
 * Serializable heart of {@link org.ontologyengineering.protege.web.client.ui.pattern.Pattern}
 */
@Getter
@Setter
@AllArgsConstructor
public class PatternCore implements Serializable {
    protected int width = 120;
    protected int height = 80;

    /**
     * Do not use this constructor; it is for serialization purposes only
     */
    protected PatternCore() {}

    public Size getSize() {
        return new Size(width, height);
    }

    public void setSize(@NonNull final Size sz) {
        this.width = sz.getWidth();
        this.height = sz.getHeight();
    }
}