package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.common.base.Optional;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;

/**
 * A grouping of concept diagram objects (eg. curves, arrows, spiders) that
 * idiomatically encode things we want to express in ontologies
 */
public interface Pattern {

    /**
     * Instantiate the pattern (needed when it's added to the diagram).
     *
     * A pattern may have a different appearance and behaviour when it is a template,
     * and when it's instantiated. By rights, this should only be called once for a
     * given template. To create multiple instances of a pattern, we copy the template
     * 
     *
     * {@see startTemplateMode}
     */
    public void switchToInstanceMode();
    public void startTemplateMode(final String label);
}
