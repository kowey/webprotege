package org.ontologyengineering.protege.web.client.ui.pattern;

import com.google.gwt.user.client.ui.AbsolutePanel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.CurveRegistry;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.SearchManager;
import org.ontologyengineering.protege.web.client.util.Position;
import org.ontologyengineering.protege.web.client.util.Size;

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
@ToString @Getter @Setter
class Pattern extends AbsolutePanel {

    public final static int DEFAULT_TEMPLATE_WIDTH = 200;
    public final static int DEFAULT_TEMPLATE_HEIGHT = 90;

    @NonNull final private String id;
    @NonNull final CurveRegistry curveRegistry;
    @NonNull final SearchManager searchManager;
    @NonNull final private AbsolutePanel parentPanel;

    protected int width = 120;
    protected int height = 80;
    protected int rounding = 20;

    public Pattern(@NonNull final String id,
                   @NonNull final CurveRegistry curveRegistry,
                   @NonNull final SearchManager searchManager,
                   @NonNull final AbsolutePanel parentPanel) {
        this.id = id;
        this.curveRegistry = curveRegistry;
        this.searchManager = searchManager;
        this.parentPanel = parentPanel;
        getElement().setClassName("template");
    }

    public Size getSize() {
        return new Size(width, height);
    }

    public void setSize(@NonNull final Size sz) {
        this.width = sz.getWidth();
        this.height = sz.getHeight();
    }

    protected Position relativeToParent(Position local) {
        return new Position(
                1 + local.getX() + parentPanel.getWidgetLeft(this),
                1 + local.getY() + parentPanel.getWidgetTop(this));
    }

    protected native void makeDraggable(String draggableId) /*-{
        $wnd.make_draggable(draggableId);
        }-*/;
}
