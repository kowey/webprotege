package org.ontologyengineering.protege.web.client.ui.pattern;

import com.google.gwt.user.client.ui.AbsolutePanel;
import lombok.*;

/**
 * A grouping of concept diagram objects (eg. curves, arrows, spiders) that
 * idiomatically encode things we want to express in ontologies
 *
 * For any given pattern there are two instances live in the sidebar: an invisible
 * mould, and a visible template. Dragging the visible template out far enough from
 * the sidebar causes it to flip to instance mode (something calls this function).
 * The invisible mould is then copied into a new visible template, which needs to
 * be started off.
 *
 * {@see TemplateHandler}
 */
public abstract
@Getter @Setter @RequiredArgsConstructor @ToString
class Pattern extends AbsolutePanel {
    protected int height = 80;
    protected int width  = 120;

    /**
     * Return some string that can be used as a prefix to a DOM identifier
     * for objects within this pattern.
     *
     * (can be static)
     */
    public abstract String getIdPrefix();

    /**
     * Instantiate the pattern (needed when it's added to the diagram).
     * See class description for some background on instance/template mode.
     *
     * {@see startTemplateMode}
     */
    public abstract void switchToInstanceMode();

    /**
     * Start a pattern off in its initial state, that of serving as a template.
     * See class description for some background on the instance/template mode.
     *
     * {@see switchToInstanceMode}
     */
    public abstract void startTemplateMode();

    /**
     * Assuming the current pattern is the invisible mould (see class description),
     * copy it to a new visible template pattern
     *
     * @param container panel to copy the mould into
     * @param counter a number that would be used to build the DOM identifier
     *                for this pattern (should be incremented by
     * @return a brand new template that can be used to instantiate yet another pattern
     */
    public abstract Pattern copyTemplate(final AbsolutePanel container,
                                         final int counter);

}
