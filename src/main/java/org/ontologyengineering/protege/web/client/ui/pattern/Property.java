package org.ontologyengineering.protege.web.client.ui.pattern;

import com.google.common.base.Optional;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;
import lombok.*;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableRect;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableShape;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.TemplateHandler;
import org.semanticweb.owlapi.model.IRI;

/**
 * Created by kowey on 2014-02-03.
 */
public
// We would just use @Data but @EqualsAndHashCode is incompatible with GWT
// https://code.google.com/p/projectlombok/issues/detail?id=414
// because the GWT compiler does not support '$' in variable names
@Getter @Setter @RequiredArgsConstructor @ToString
class Property extends Pattern implements Cloneable,
        MouseOverHandler, MouseOutHandler, MouseUpHandler, MouseDownHandler, MouseMoveHandler {

    @RequiredArgsConstructor
    class RenameHandler implements KeyUpHandler {
        @Override
        public void onKeyUp(KeyUpEvent event) {
        }
    }

    @RequiredArgsConstructor
    class DeleteHandler implements ClickHandler {
        @Override
        public void onClick(ClickEvent event) {
        }
    }

    @Getter private String idPrefix = "property";
    @NonNull final String id;
    //@NonNull final ConceptManager conceptManager;

    @Setter(AccessLevel.PACKAGE) @NonNull Optional<IRI> iriSrc = Optional.absent();
    @Setter(AccessLevel.PACKAGE) @NonNull Optional<IRI> iriTgt = Optional.absent();

    private boolean isMoving = false;
    private boolean isRenaming = false;

    private int width  = 50;
    private int height = 40;
    private int rounding = 20;

    public String getCurveIdSrc() {
        return this.id + "_curvesrc";
    }

    public String getCurveIdTgt() {
        return this.id + "_curvetgt";
    }

    @Override
    public void onLoad() {
        this.getElement().setId(this.id);
        this.setWidth((this.width + 120) + "px");
        this.setHeight((this.height + 10) + "px");
        super.onLoad();

        final DraggableShape wCurveSrc = new DraggableRect(this.width, this.height, this.rounding);
        final DraggableShape wCurveTgt = new DraggableRect(this.width, this.height, this.rounding);
        wCurveSrc.getElement().setId(getCurveIdSrc());
        wCurveTgt.getElement().setId(getCurveIdTgt());

        final Panel wButtons = new HorizontalPanel();
        wButtons.getElement().setClassName("concept-button");

        final Button wDelete = new Button("X");
        wDelete.addClickHandler(new DeleteHandler());
        wButtons.add(wDelete);
        this.add(wButtons, this.width + 5, this.height - 10);
        this.add(wCurveSrc, 1, 1);
        this.add(wCurveTgt, 11+this.width, 1);
    }


    public Property copyTemplate(@NonNull final AbsolutePanel container,
                                 final int counter) {

        Property copy  = new Property(idPrefix + counter);
        container.add(copy, container.getWidgetLeft(this), container.getWidgetTop(this));
        copy.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
        copy.getElement().setClassName("template");
        TemplateHandler.addHandler(container, this, copy, counter);
        makeDraggable("#" + copy.getId());
        return copy;
    }

    public void mouseOverHighlight() {
    }

    @Override
    public void onMouseOver(MouseOverEvent event) {

    }

    @Override
    public void onMouseOut(MouseOutEvent event) {

    }

    @Override
    public void onMouseUp(MouseUpEvent event) {
        this.isMoving = false;
    }

    @Override
    public void onMouseDown(MouseDownEvent event) {
        this.isMoving = true;
    }

    @Override
    public void onMouseMove(MouseMoveEvent event) {
        if (this.isMoving) {
            this.onMouseOut(null);
        }
    }

    /**
     * Remove and unregister this concept
     */
    public void delete() {
        removeFromParent();
    }

    public void switchToInstanceMode() {
        addDomHandler(this, MouseOverEvent.getType());
        addDomHandler(this, MouseOutEvent.getType());
        addDomHandler(this, MouseUpEvent.getType());
        addDomHandler(this, MouseDownEvent.getType());
        addDomHandler(this, MouseMoveEvent.getType());
        mouseOverHighlight();
    }

    /**
     * Note: you should only ever call this once
     */
    public void startTemplateMode() {
        this.getElement().getStyle().setVisibility(Style.Visibility.HIDDEN);
        this.getElement().setClassName("template");
    }

    private native void makeDraggable(String draggableId) /*-{
        $wnd.make_draggable(draggableId);
        }-*/;
}
