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
@Getter @Setter @ToString
class Pattern extends AbsolutePanel {

    // used to ensure that all curves created have a unique identifier
    static private int globalPatternCounter = 0;

    protected int height;
    protected int width;

    public Pattern() {
        this(80, 120);
    }

    public Pattern(final int height,
                   final int width) {
        this.height = height;
        this.width = width;
        globalPatternCounter++;
    }

    /**
     * Return the current DOM identifier
     *
     * Note that every call to the Pattern constructor causes this to change
     */
    public String makeId() {
        return getIdPrefix() + globalPatternCounter;
    }

    /**
     * Return some string that can be used as a prefix to a DOM identifier
     * for objects within this pattern.
     *
     * (can be static)
     */
    public abstract String getIdPrefix();
}
