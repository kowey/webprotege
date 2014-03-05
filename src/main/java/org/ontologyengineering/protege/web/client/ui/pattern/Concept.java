package org.ontologyengineering.protege.web.client.ui.pattern;

import com.google.common.base.Optional;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.layout.client.Layout;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.*;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import lombok.*;
import org.ontologyengineering.protege.web.client.ConceptManager;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableRect;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableShape;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.TemplateHandler;
import org.semanticweb.owlapi.model.IRI;

import java.lang.Math;

/**
 * Created by kowey on 2014-02-03.
 */
public
// We would just use @Data but @EqualsAndHashCode is incompatible with GWT
// https://code.google.com/p/projectlombok/issues/detail?id=414
// because the GWT compiler does not support '$' in variable names
@Getter @Setter @RequiredArgsConstructor @ToString
class Concept extends Pattern implements Cloneable,
        MouseOverHandler, MouseOutHandler, MouseUpHandler, MouseDownHandler, MouseMoveHandler {

    @RequiredArgsConstructor
    class RenameHandler implements KeyUpHandler {
        @NonNull private final TextBox textbox;

        @Override
        public void onKeyUp(KeyUpEvent event) {
            // we respond to key up events up updating a provisional label
            // only on Enter key or mouse-out do we actually commit the label
            final String text = textbox.getText().trim();
            final Optional<String> label = (text.isEmpty() || text.equals(""))
                    ? Optional.<String>absent()
                    : Optional.of(text);
            setTempLabel(label);
            // once the user has begun to type things (even a single char)
            // we no longer consider it annoying to lose focus on mouse-out
            canvasState.setRenaming(false);
            if (event.getNativeKeyCode() ==  KeyCodes.KEY_ENTER) {
                onMouseOut(null);
            }
        }
    }

    @RequiredArgsConstructor
    class DeleteHandler implements ClickHandler {
        @Override
        public void onClick(ClickEvent event) {
            if (iri.isPresent()) {
                conceptManager.deleteClass(iri.get());
            }
            Concept.this.delete();
        }
    }

    @Getter private String idPrefix = "concept";

    @NonNull final String id;
    @NonNull final ConceptManager conceptManager;

    @Setter(AccessLevel.PRIVATE) @NonNull Optional<String> tempLabel = Optional.absent();
    @NonNull Optional<String> label = Optional.absent();

    /**
     * FIXME: You should probably not use the setter for this method
     */
    @Setter @NonNull Optional<IRI> iri = Optional.absent();

    @Data class ResizeScale {
        private final float x;
        private final float y;

        public String toString() {
            return x + "×" + y;
        }
    }

    @Data class CanvasState {
        private boolean isMoving = false;
        private boolean isRenaming = false;

        private boolean isAboutToResize = false;
        @Setter(AccessLevel.NONE) boolean isResizing = false;
        @Setter(AccessLevel.NONE) private int resizePointX = 0;
        @Setter(AccessLevel.NONE) private int resizePointY = 0;

        /**
         * Signal to this canvas state that we're ready to switch to resize mode.
         * Triggered on some non-resize event, like a button press.
         * Haven't actually started yet, though
         */
        public void prepareForResizing() {
            isAboutToResize = true;
            makeUndraggable();
        }

        public void startResizing(MouseEvent event) {
            isAboutToResize = false;
            isResizing = true;
            final Element elm = Concept.this.getElement();
            resizePointX = event.getRelativeX(elm);
            resizePointY = event.getRelativeY(elm);
            GWT.log("Started resizing at " + resizePointX + " and " + resizePointY);
        }

        /**
         * How much to scale the thing we are currently resizing.
         * The idea here is to be give live feedback by resizing as the
         * user drags their mouse. (If we're not currently resizing,
         * this would just be 1:1).
         *
         * @param event
         * @return
         */
        public ResizeScale resizingScale(MouseEvent event) {
            if (isResizing) {
                final Element elm = Concept.this.getElement();
                final int currentX = event.getRelativeX(elm);
                final int currentY = event.getRelativeY(elm);
                final float scaleX = (resizePointX > 0) ? (currentX / (float)resizePointX) : 1;
                final float scaleY = (resizePointY > 0) ? (currentY / (float)resizePointY) : 1;
                resizePointX = currentX;
                resizePointY = currentY;
                return new ResizeScale(scaleX, scaleY);
            } else {
                return new ResizeScale(1,1);
            }
        }

        public void stopResizing() {
            isResizing = false;
            makeDraggable();
        }
    }

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    CanvasState canvasState = new CanvasState();

    private int rounding = 20;
    final DraggableShape wCurve = new DraggableRect(this.width, this.height, this.rounding);
    ButtonBar buttonBar = new ButtonBar();

    private Concept thisConcept() {
        return this;
    }

    public String getCurveId() {
        return this.id + "_curve";
    }

    @Data class ButtonBar extends DockPanel {
        final private TextBox wLabel = new TextBox();
        final Panel wButtons = new HorizontalPanel();
        final Button wDelete = new Button("X");
        final Button wResize = new Button("⇲");

        private void activate() {
            this.wLabel.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    canvasState.setRenaming(true);
                }
            });
            this.wLabel.addKeyUpHandler(new RenameHandler(wLabel));
            this.wLabel.setReadOnly(true);

            wDelete.addClickHandler(new DeleteHandler());
            wResize.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    canvasState.prepareForResizing();
                }
            });
        }

        private void reposition(int curveWidth, int curveHeight) {
            setHeight(curveHeight + 10 + "px");
        }

        public ButtonBar() {
            wLabel.setWidth("6em");
            wButtons.getElement().setClassName("concept-button");
            wButtons.add(wDelete);
            wButtons.add(wResize);
            add(wLabel, NORTH);
            add(wButtons, SOUTH);
            setCellHorizontalAlignment(wButtons, ALIGN_RIGHT);
            setCellVerticalAlignment(wButtons, ALIGN_BOTTOM);
            reposition(Concept.this.width, Concept.this.height);
            activate();
        }
    }


    @Override
    public void onLoad() {
        this.getElement().setId(this.id);
        super.onLoad();
        wCurve.getElement().setId(getCurveId());
        this.add(wCurve, 1, 1);
        setSize(this.width, this.height);
    }

    /**
     * Resize this concept and reposition its helper widgets
     * accordingly
     */
    private void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        wCurve.setSize(width, height);
        buttonBar.removeFromParent(); // no-op if not there
        this.add(buttonBar, width + 5, 0);
        buttonBar.reposition(width, height);
        this.setPixelSize(width + buttonBar.getOffsetWidth() + 5, height + 5);
    }


    public Concept copyTemplate(@NonNull final AbsolutePanel container,
                                final int counter) {



        Concept copy  = new Concept(idPrefix + counter, conceptManager);
        copy.setLabel(this.getLabel());
        container.add(copy, container.getWidgetLeft(this), container.getWidgetTop(this));
        copy.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
        copy.getElement().setClassName("template");
        TemplateHandler.addHandler(container, this, copy, counter);
        copy.makeDraggable();
        return copy;
    }

    public void mouseOverHighlight() {
        this.getElement().setClassName("concept-over");
    }

    @Override
    public void onMouseOver(MouseOverEvent event) {
        mouseOverHighlight();
        if (this.iri.isPresent()) {
            this.conceptManager.selectClass(this.iri.get());
        }
        buttonBar.wLabel.setReadOnly(false);
    }

    @Override
    public void onMouseOut(MouseOutEvent event) {
        if (! this.canvasState.isRenaming()) {
            this.getElement().setClassName("concept");
            buttonBar.wLabel.setReadOnly(true);
            handleLabelChanges(this.label, this.tempLabel);
            this.label = this.tempLabel;
        }
    }

    protected void handleLabelChanges(@NonNull final Optional<String> before,
                                      @NonNull final Optional<String> after) {
        if (before == after) {
            return;
        } else if (!before.isPresent()) {
            this.conceptManager.createClass(this, after.get());
        } else if (!after.isPresent()) {
            if (this.iri.isPresent()) {
                this.conceptManager.deleteClass(this.iri.get());
            } else {
                GWT.log("ERROR: no IRI set even though delete was triggered");
            }
        } else {
            if (this.iri.isPresent()) {
                this.conceptManager.renameClass(this.iri.get(), before.get(), after.get());
            }
        }
    }

    @Override
    public void onMouseUp(MouseUpEvent event) {
        this.canvasState.setMoving(false);
        this.canvasState.stopResizing();
    }

    @Override
    public void onMouseDown(MouseDownEvent event) {
        if (canvasState.isAboutToResize()) {
            canvasState.startResizing(event);
        } else {
            canvasState.setMoving(true);
        }
    }

    @Override
    public void onMouseMove(MouseMoveEvent event) {
        if (canvasState.isMoving()) {
            onMouseOut(null);
        } else if (canvasState.isResizing) {
            ResizeScale scale = canvasState.resizingScale(event);
            int newWidth = Math.round(width * scale.getX());
            int newHeight = Math.round(height * scale.getY());
            setSize(newWidth, newHeight);
            GWT.log("Desired scale = " + scale + " would be " + newWidth + "x" + newHeight);
        }
    }

    public void setLabel(@NonNull Optional<String> label) {
        GWT.log("[CONCEPT] setLabel" + label + "(was " + this.label + "," + this.tempLabel + ")");
        setTempLabel(label);
        this.label = label;
        buttonBar.wLabel.setText(label.or(""));
    }

    /**
     * Remove and unregister this concept
     */
    public void delete() {
        if (iri.isPresent()) {
            conceptManager.deleteClass(iri.get());
        }
        removeFromParent();
    }

    public void switchToInstanceMode() {
        addDomHandler(this, MouseOverEvent.getType());
        addDomHandler(this, MouseOutEvent.getType());
        addDomHandler(this, MouseUpEvent.getType());
        addDomHandler(this, MouseDownEvent.getType());
        addDomHandler(this, MouseMoveEvent.getType());
        mouseOverHighlight();
        setLabel(Optional.<String>absent());
    }

    /**
     * Note: you should only ever call this once
     */
    public void startTemplateMode() {
        this.setLabel(Optional.of("CLASS"));
        this.getElement().getStyle().setVisibility(Style.Visibility.HIDDEN);
        this.getElement().setClassName("template");
        buttonBar.wLabel.setReadOnly(true);
    }

    protected void makeDraggable() {
        _makeDraggable("#" + getId());
    }

    protected void makeUndraggable() {
        _makeUndraggable("#" + getId());
    }

    private native void _makeDraggable(String draggableId) /*-{
        $wnd.make_draggable(draggableId);
        }-*/;

    private native void _makeUndraggable(String draggableId) /*-{
        $wnd.make_undraggable(draggableId);
        }-*/;
}
