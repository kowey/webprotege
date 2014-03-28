package org.ontologyengineering.protege.web.client.ui.pattern;

import com.google.common.base.Optional;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.*;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Panel;
import lombok.*;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.CurveRegistry;
import org.ontologyengineering.protege.web.client.effect.Key;
import org.ontologyengineering.protege.web.client.effect.Painter;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.SearchManager;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.SearchManager.SearchHandler;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableRect;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableShape;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.TemplateHandler;
import org.ontologyengineering.protege.web.client.effect.AttributeLayers;
import org.ontologyengineering.protege.web.client.effect.VisualEffect;
import org.semanticweb.owlapi.model.IRI;

import java.lang.Math;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Created by kowey on 2014-02-03.
 */
public
// We would just use @Data but @EqualsAndHashCode is incompatible with GWT
// https://code.google.com/p/projectlombok/issues/detail?id=414
// because the GWT compiler does not support '$' in variable names
@Getter @Setter
@RequiredArgsConstructor
@ToString
class Curve extends Pattern implements Cloneable,
        MouseOverHandler, MouseOutHandler, MouseUpHandler, MouseDownHandler, MouseMoveHandler {

    @NonNull protected final String id;
    @Getter private String idPrefix = "curve";
    private int rounding = 20;

    @NonNull final CurveRegistry curveRegistry;
    @NonNull final SearchManager searchManager;

    @NonNull Optional<String> label = Optional.absent();
    @Setter(AccessLevel.PRIVATE) @NonNull Optional<String> tempLabel = Optional.absent();

    /**
     * Note that setIri should probably only be used by the concept manager
     */
    @NonNull Optional<IRI> iri = Optional.absent();

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    CanvasState canvasState = new CanvasState();

    final private TextBox wLabel = new TextBox();
    final private DraggableShape wCurve = new DraggableRect(this.width, this.height, this.rounding);
    final private ButtonBar buttonBar = new ButtonBar(wLabel);
    final private Effects effects = new Effects(wCurve, wLabel);

    @RequiredArgsConstructor @Getter
    class Effects extends AttributeLayers {
        @NonNull final private DraggableShape curve;
        @NonNull final private TextBox label;

        // here we model the possibility of there being more than one search box
        final private Map<SearchHandler, VisualEffect> searchEffects = new IdentityHashMap();
        final private Map<DraggableShape, VisualEffect> dragSnapEffects = new IdentityHashMap();

        @NonNull public VisualEffect searchBoxPartial(String color) {
            VisualEffect effect = new VisualEffect("searchbox partial (" + color + ")");
            effect.setAttribute(label, "color", color, "black");
            effect.setAttribute(curve, "stroke", color, "black");
            effect.setAttribute(curve, "stroke-width", "2", "1");
            effect.setAttribute(curve, "stroke-dasharray", "-", "");
            return effect;
        }

        @NonNull public VisualEffect searchBoxUnique(String color) {
            VisualEffect effect = new VisualEffect("searchbox unique (" + color + ")");
            effect.setAttribute(label, "color", color, "black");
            effect.setAttribute(label, "fontWeight", "bold", "normal");
            effect.setAttribute(curve, "stroke", color, "black");
            effect.setAttribute(curve, "stroke-width", "5", "1");
            effect.setAttribute(curve, "stroke-dasharray", "- . .", "");
            return effect;
        }

        @NonNull public VisualEffect dragSnapPartial(String color) {
            VisualEffect effect = new VisualEffect("dragsnap partial (" + color + ")");
            effect.setAttribute(curve, "fill", color, "white");
            effect.setAttribute(curve, "opacity", "0.25", "1");
            return effect;
        }

        @NonNull public VisualEffect dragSnapUnique(String color) {
            VisualEffect effect = new VisualEffect("dragsnap unique (" + color + ")");
            effect.setAttribute(curve, "fill", color, "white");
            effect.setAttribute(curve, "opacity", "0.5", "1");
            return effect;
        }


        public void applySearchBoxEffect(SearchHandler searchBox,
                                         Optional<VisualEffect> newEffect) {
            setContextEffect(searchEffects, searchBox, newEffect);
            applyAttributes();
        }

        public void applyDragSnapEffect(DraggableShape dragged,
                                        Optional<VisualEffect> newEffect) {
            setContextEffect(dragSnapEffects, dragged, newEffect);
            applyAttributes();
        }

        private void applyAttributes() {
            applyAttributes(new Painter() {
                final Style labelStyle = label.getElement().getStyle();
                @Override
                public void apply(Key key, String value) {
                    final Object obj = key.getObject();
                    final String attr = key.getAttribute();
                    if (obj == label) {
                        labelStyle.setProperty(attr, value);
                    } else if (obj == curve) {
                        curve.attr(attr, value);
                    }
                }
            });
        }

    }


    /*
     * ************ Handlers *****************
     */

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
            Curve.this.delete();
        }
    }





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
            final Element elm = Curve.this.getElement();
            resizePointX = event.getRelativeX(elm);
            resizePointY = event.getRelativeY(elm);
            Curve.this.wCurve.addStyleName("resizing");
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
                final Element elm = Curve.this.getElement();
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
            Curve.this.wCurve.removeStyleName("resizing");
        }
    }

    public String getCurveId() {
        return this.id + "_curve";
    }

    @Getter
    class ButtonBar extends DockPanel {
        final private TextBox wLabel;
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

        public ButtonBar(@NonNull final TextBox wLabel) {
            this.wLabel = wLabel;
            wLabel.setWidth("6em");
            wButtons.getElement().setClassName("concept-button");
            wButtons.add(wDelete);
            wButtons.add(wResize);
            add(wLabel, NORTH);
            add(wButtons, SOUTH);
            setCellHorizontalAlignment(wButtons, ALIGN_RIGHT);
            setCellVerticalAlignment(wButtons, ALIGN_BOTTOM);
            reposition(Curve.this.width, Curve.this.height);
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

    /**
     * Create a whole new curve, with the given left/top coordinates and dimensions
     */
    public Curve createCurve(@NonNull final AbsolutePanel container,
                             final int relativeX,
                             final int relativeY) {
        Curve curve = new Curve(makeId(), curveRegistry, searchManager);
        int parentRelativeX = relativeX + container.getWidgetLeft(this);
        int parentRelativeY = relativeY + container.getWidgetTop(this);
        container.add(curve, parentRelativeX, parentRelativeY);
        return curve;
    }

    public Curve copyTemplate(@NonNull final AbsolutePanel container) {
        Curve copy  = new Curve(makeId(), curveRegistry, searchManager);
        String text = this.buttonBar.wLabel.getText();
        if (text.isEmpty()) {
            copy.setLabel(Optional.<String>absent());
        } else {
            copy.setLabel(Optional.of(text));
        }
        container.add(copy, container.getWidgetLeft(this), container.getWidgetTop(this));
        copy.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
        copy.getElement().setClassName("template");
        TemplateHandler.addHandler(container, this, copy);
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
            this.curveRegistry.selectClass(this.iri.get());
        }
        buttonBar.wLabel.setReadOnly(false);
    }

    @Override
    public void onMouseOut(MouseOutEvent event) {
        if (! this.canvasState.isRenaming()) {
            this.getElement().setClassName("concept");
            buttonBar.wLabel.setReadOnly(true);
            if (! this.label.equals(this.tempLabel)) {
                rename(this.tempLabel);
            }
        }
        // we can mouse out even if we're still holding the mouse button
        // down; this leads to some confusion where we start resizing and
        // then mouse out to another object due to our mouse movements
        // and when we get back to object
        this.canvasState.stopResizing();
    }





    @Override
    public void onMouseUp(MouseUpEvent event) {
        this.canvasState.setMoving(false);
        this.canvasState.stopResizing();
        //searchManager.getSearchIndex();
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

    /**
     * [visual] Change label text only
     *
     * You probably want {@link #rename}
     *
     *
     * @param label
     */
    private void setLabel(@NonNull Optional<String> label) {
        this.label = label;
        setTempLabel(label);
        buttonBar.wLabel.setText(label.or(""));
    }

    /**
     * Change the curve label (and reregister accordingly)
     *
     * @param name
     */
    public void rename(Optional<String> name) {
        curveRegistry.changeCurveName(this, this.getLabel(), name);
        setLabel(name);
    }

    /**
     * Remove and unregister this curve
     */
    public void delete() {
        curveRegistry.removeCurveName(this);
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

    public void setMatchStatus(SearchManager.SearchHandler searchBox,
                               SearchManager.MatchStatus status) {
        final String color = searchBox.getColor();

        Optional<VisualEffect> mEffect = Optional.<VisualEffect>absent();
        switch (status) {
            case PARTIAL_MATCH:
                mEffect = Optional.of(effects.searchBoxPartial(color));
                break;
            case UNIQUE_MATCH:
                mEffect = Optional.of(effects.searchBoxUnique(color));
                break;
        }
        effects.applySearchBoxEffect(searchBox, mEffect);
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
