package org.ontologyengineering.protege.web.client.ui.curve;

import com.google.common.base.Optional;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.*;
import lombok.*;
import org.ontologyengineering.protege.web.client.effect.AttributeLayers;
import org.ontologyengineering.protege.web.client.effect.Key;
import org.ontologyengineering.protege.web.client.effect.Painter;
import org.ontologyengineering.protege.web.client.effect.VisualEffect;
import org.ontologyengineering.protege.web.client.ui.pattern.Pattern;
import org.ontologyengineering.protege.web.client.util.Position;
import org.ontologyengineering.protege.web.client.util.Scale;
import org.ontologyengineering.protege.web.client.util.Size;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.CurveRegistry;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.SearchManager;
import org.ontologyengineering.protege.web.client.ui.conceptdiagram.SearchManager.SearchHandler;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableRect;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableShape;
import org.semanticweb.owlapi.model.IRI;

import java.util.IdentityHashMap;
import java.util.Map;

public
// We would just use @Data but @EqualsAndHashCode is incompatible with GWT
// https://code.google.com/p/projectlombok/issues/detail?id=414
// because the GWT compiler does not support '$' in variable names
@ToString
class Curve implements
        MouseOverHandler, MouseOutHandler, MouseUpHandler, MouseDownHandler, MouseMoveHandler {

    // relative to its inner box
    private final static Position curveOffset = new Position(1, 1);

    @Getter @NonNull final private String id;

    /**
     * Heart of the curve, everything we need to be able to reconstruct this
     * curve
     */
    @Getter private CurveCore core;

    @Getter final private CurvePanel canvasState;
    @Getter private String idPrefix = "curve";

    @NonNull final CurveRegistry curveRegistry;
    @NonNull final SearchManager searchManager;

    @Setter(AccessLevel.PRIVATE) @NonNull Optional<String> tempLabel;

    final private TextBox wLabel;
    @Getter final private DraggableShape wCurve;
    final private ButtonBar buttonBar;
    @Getter final private Effects effects;

    // used to ensure that all curves created have a unique identifier
    static private int idCounter = 0;

    public Curve(@NonNull final CurveRegistry curveRegistry,
                 @NonNull final SearchManager searchManager) {
        this(new CurveCore(), curveRegistry, searchManager);
    }

    public Curve(@NonNull final CurveCore core,
                 @NonNull final CurveRegistry curveRegistry,
                 @NonNull final SearchManager searchManager) {
        super();
        idCounter++;
        this.id = getIdPrefix() + idCounter;
        this.curveRegistry = curveRegistry;
        this.searchManager = searchManager;
        this.core = core;

        this.canvasState = new CurvePanel();

        this.tempLabel = Optional.absent();

        this.wLabel = new TextBox();
        this.buttonBar = new ButtonBar(wLabel);

        this.wCurve = new DraggableRect(
                this.core.getWidth(),
                this.core.getHeight(),
                this.core.getRounding());
        this.effects = new Effects(wCurve, wLabel);
        this.effects.initialize();

        this.setLabel(core.getLabel());
        this.mouseOutHighlight();
    }

    public Optional<String> getLabel() {
        return core.getLabel();
    }

    /**
     * Note that setIri should probably only be used by the concept manager
     */
    public void setIri(@NonNull final Optional<IRI> iri) {
        core.setIri(iri);
    }

    public Optional<IRI> getIri() {
        return core.getIri();
    }

    @RequiredArgsConstructor @Getter
    public class Effects extends AttributeLayers {
        @NonNull final private DraggableShape curve;
        @NonNull final private TextBox label;

        // here we model the possibility of there being more than one search box
        final private Map<SearchHandler, VisualEffect> searchEffects =
                new IdentityHashMap<SearchHandler, VisualEffect>();
        final private Map<DraggableShape, VisualEffect> dragSnapEffects =
                new IdentityHashMap<DraggableShape, VisualEffect>();

        final String DEFAULT_CURVE_FILL_COLOR = "white";
        final String DEFAULT_CURVE_STROKE_COLOR = "black";
        final String DEFAULT_CURVE_FILL_OPACITY = "0";
        final String DEFAULT_CURVE_STROKE_OPACITY = "1";

        @NonNull public VisualEffect searchBoxPartial(String color) {
            VisualEffect effect = new VisualEffect("searchbox partial (" + color + ")");
            effect.setAttribute(label, "color", color, "black");
            effect.setAttribute(curve, "stroke", color, DEFAULT_CURVE_STROKE_COLOR);
            effect.setAttribute(curve, "stroke-width", "2", "1");
            effect.setAttribute(curve, "stroke-dasharray", "-", "");
            return effect;
        }

        @NonNull public VisualEffect searchBoxUnique(String color) {
            VisualEffect effect = new VisualEffect("searchbox unique (" + color + ")");
            effect.setAttribute(label, "color", color, "black");
            effect.setAttribute(label, "fontWeight", "bold", "normal");
            effect.setAttribute(curve, "stroke", color, DEFAULT_CURVE_STROKE_COLOR);
            effect.setAttribute(curve, "stroke-width", "5", "1");
            effect.setAttribute(curve, "stroke-dasharray", "- . .", "");
            return effect;
        }

        @NonNull public VisualEffect dragSnapPartial(String color) {
            VisualEffect effect = new VisualEffect("dragsnap partial (" + color + ")");
            effect.setAttribute(curve, "fill", color, DEFAULT_CURVE_FILL_COLOR);
            effect.setAttribute(curve, "fill-opacity", "0.25", DEFAULT_CURVE_FILL_OPACITY);
            return effect;
        }

        @NonNull public VisualEffect dragSnapUnique(String color) {
            VisualEffect effect = new VisualEffect("dragsnap unique (" + color + ")");
            effect.setAttribute(curve, "fill", color, DEFAULT_CURVE_FILL_COLOR);
            effect.setAttribute(curve, "fill-opacity", "0.5", DEFAULT_CURVE_FILL_OPACITY);
            return effect;
        }

        @NonNull public void initialize() {
            VisualEffect effect = new VisualEffect("curve defaults");
            effect.setAttribute(curve, "fill", "", DEFAULT_CURVE_FILL_COLOR);
            effect.setAttribute(curve, "fill-opacity", "", DEFAULT_CURVE_FILL_OPACITY);
            effect.setAttribute(curve, "stroke-opacity", "", DEFAULT_CURVE_STROKE_OPACITY);
            addDefaultEffect(effect);
            applyAttributes(new Painter() {
                @Override
                public void apply(Key key, String value) {
                    final String attr = key.getAttribute();
                    curve.attr(attr, value);
                }
            });
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

    @Getter @Setter
    @RequiredArgsConstructor class CurvePanel extends AbsolutePanel {
        private boolean isMoving = false;
        private boolean isRenaming = false;

        private boolean isAboutToResize = false;
        @Setter(AccessLevel.NONE) boolean isResizing = false;
        @Setter(AccessLevel.NONE) private int resizePointX = 0;
        @Setter(AccessLevel.NONE) private int resizePointY = 0;

        @Override
        public void onLoad() {
            this.getElement().setId(Curve.this.getId());
            wCurve.getElement().setId(getCurveId());
            this.add(wCurve, curveOffset.getX(), curveOffset.getY());
            redraw();
        }


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
            final Element elm = Curve.this.canvasState.getElement();
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
        public Scale resizingScale(MouseEvent event) {
            if (isResizing) {
                final Element elm = canvasState.getElement();
                final int currentX = event.getRelativeX(elm);
                final int currentY = event.getRelativeY(elm);
                final float scaleX = (resizePointX > 0) ? (currentX / (float)resizePointX) : 1;
                final float scaleY = (resizePointY > 0) ? (currentY / (float)resizePointY) : 1;
                resizePointX = currentX;
                resizePointY = currentY;
                return new Scale(scaleX, scaleY);
            } else {
                return new Scale(1,1);
            }
        }

        public void stopResizing() {
            if (isResizing) {
                isResizing = false;
                makeDraggable();
                Curve.this.wCurve.removeStyleName("resizing");
            }
        }

        public boolean isAcceptable(@NonNull final Size sz) {
            return sz.getWidth() > 50 && sz.getHeight() > 50;
        }
    }

    public String getCurveId() {
        return this.getId() + "_curve";
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

        private void reposition(@NonNull final Size sz) {
            setHeight(sz.getHeight() + 10 + "px");
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
            reposition(Curve.this.getSize());
            activate();
        }
    }

    /*
    @Override
    public void onLoad() {
        this.canvasState.getElement().setId(this.id);
        wCurve.getElement().setId(getCurveId());
        this.canvasState.add(wCurve, 1, 1);
        setSize(this.width, this.height);
    }*/

    public Widget getWidget() {
        return canvasState;
    }

    public Element getElement() {
        return canvasState.getElement();
    }

    private void redraw() {
        final Size sz = getSize();
        final int width = sz.getWidth();
        final int height = sz.getHeight();
        wCurve.setSize(width, height);
        buttonBar.removeFromParent(); // no-op if not there
        this.canvasState.add(buttonBar, width + 5, 0);
        buttonBar.reposition(sz);
        this.canvasState.setPixelSize(width + buttonBar.getOffsetWidth() + 5, height + 5);
    }

    /**
     * Resize this concept and reposition its helper widgets
     * accordingly
     */
    public void setSize(final Size sz) {
        this.core.setSize(sz);
        redraw();
    }

    public Size getSize() {
        return this.core.getSize();
    }

    /**
     * Get the top-left coordinates of the curve with respect to the parent
     * @return
     */
    public Position getPosition() {
        Widget widget = getWidget();
        Widget parent = widget.getParent();
        return new Position(
                widget.getAbsoluteLeft() - parent.getAbsoluteLeft(),
                widget.getAbsoluteTop() - parent.getAbsoluteTop());
    }

    /**
     * Given the position of the curve itself, return the position of the
     * surrounding panel (that contains the various widgets, labels, etc)
     *
     * Basically the curve is going to be offset down and to the right a
     * bit, and we want to reverse this offset by inching left/up
     */
    public static Position curveToPanelPosition(@NonNull final Position curvePosition) {
        return curvePosition.subtract(curveOffset);
    }



    /**
     * Create a whole new curve, with the given left/top coordinates and dimensions
     */
    public Curve copyCurve() {
        Curve curve = new Curve(curveRegistry, searchManager);
        curve.setLabel(this.getLabel());
        curve.setIri(this.getIri());
        return curve;
    }

    /**
     * Place a newly-created curve in its parent container and activate it.
     *
     * @param container
     * @param topLeft
     */
    public void placeCurve(@NonNull final AbsolutePanel container,
                           @NonNull final Position topLeft) {
        container.add(this.canvasState, topLeft.getX(), topLeft.getY());
        activate();
    }

    /**
     * Reposition and resize this curve so that it surrounds the other curve.
     */
    public void placeOutside(@NonNull final AbsolutePanel container,
                             @NonNull final Curve other) {
        final Position anonTopLeft = new Position(
                container.getWidgetLeft(other.getWidget()) - 10,
                container.getWidgetTop(other.getWidget()) - 10);
        final Scale anonScale = new Scale((float) 1.2, (float) 1.2);
        placeCurve(container, anonTopLeft);
        setSize(anonScale.transform(other.getSize()));
    }

    /**
     * Reposition and resize this curve so that it is surrounded by the other curve
     */
    public void placeInside(@NonNull final AbsolutePanel container,
                            @NonNull final Curve other) {
        final Position anonTopLeft = new Position(
                container.getWidgetLeft(other.getWidget()) + 10,
                container.getWidgetTop(other.getWidget()) + 10);
        final Scale anonScale = new Scale((float) 0.8, (float) 0.8);
        placeCurve(container, anonTopLeft);
        setSize(anonScale.transform(other.getSize()));
    }

    public void mouseOverHighlight() {
        this.getElement().setClassName("concept-over");
    }

    public void mouseOutHighlight() {
        this.getElement().setClassName("concept");
    }

    @Override
    public void onMouseOver(MouseOverEvent event) {
        mouseOverHighlight();
        if (this.getIri().isPresent()) {
            this.curveRegistry.selectClass(this.getIri().get());
        }
        buttonBar.wLabel.setReadOnly(false);
    }

    @Override
    public void onMouseOut(MouseOutEvent event) {
        if (! this.canvasState.isRenaming()) {
            mouseOutHighlight();
            buttonBar.wLabel.setReadOnly(true);
            if (! this.getLabel().equals(this.tempLabel)) {
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
        this.core.setPosition(getPosition());
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
            final Scale scale = canvasState.resizingScale(event);
            final Size sz = scale.transform(getSize());
            GWT.log("Desired scale = " + scale + " would be " + sz);
            if (canvasState.isAcceptable(sz)) {
                setSize(sz);
            }
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
        this.core.setLabel(label);
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
        canvasState.removeFromParent();
    }

    /**
     * Make this curve respond to user input
     */
    public void activate() {
        canvasState.addDomHandler(this, MouseOverEvent.getType());
        canvasState.addDomHandler(this, MouseOutEvent.getType());
        canvasState.addDomHandler(this, MouseUpEvent.getType());
        canvasState.addDomHandler(this, MouseDownEvent.getType());
        canvasState.addDomHandler(this, MouseMoveEvent.getType());
        makeDraggable(); // gratuitious in the general but needed
        // when creating curves from whole cloth
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
