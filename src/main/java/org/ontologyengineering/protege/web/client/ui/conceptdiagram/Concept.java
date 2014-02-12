package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.common.base.Optional;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;

import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceManager;
import edu.stanford.bmir.protege.web.client.dispatch.actions.CreateClassAction;
import lombok.*;
import org.ontologyengineering.protege.web.client.ConceptManager;
import org.semanticweb.owlapi.model.IRI;

/**
 * Created by kowey on 2014-02-03.
 */
public
// We would just use @Data but @EqualsAndHashCode is incompatible with GWT
// https://code.google.com/p/projectlombok/issues/detail?id=414
// because the GWT compiler does not support '$' in variable names
@Getter @Setter @RequiredArgsConstructor @ToString
class Concept extends AbsolutePanel implements Cloneable,
        MouseOverHandler, MouseOutHandler, MouseUpHandler, MouseDownHandler, MouseMoveHandler {

    @RequiredArgsConstructor
    class RenameHandler implements KeyUpHandler {
        @NonNull private final Concept concept;
        @NonNull private final TextBox textbox;

        @Override
        public void onKeyUp(KeyUpEvent event) {
            // we respond to key up events up updating a provisional label
            // only on Enter key or mouse-out do we actually commit the label
            final String text = textbox.getText().trim();
            final Optional<String> label = (text.isEmpty() || text.equals(""))
                    ? Optional.<String>absent()
                    : Optional.of(text);
            concept.setTempLabel(label);
            // once the user has begun to type things (even a single char)
            // we no longer consider it annoying to lose focus on mouse-out
            concept.setRenaming(false);
            if (event.getNativeKeyCode() ==  KeyCodes.KEY_ENTER) {
                concept.onMouseOut(null);
            }
        }
    }

    @RequiredArgsConstructor
    class DeleteHandler implements ClickHandler {
        @NonNull private final Concept concept;

        @Override
        public void onClick(ClickEvent event) {
            if (iri.isPresent()) {
                conceptManager.deleteClass(iri.get());
            }
            concept.delete();
        }
    }

    @NonNull final String id;
    @NonNull final ConceptManager conceptManager;

    @Setter(AccessLevel.PRIVATE) @NonNull Optional<String> tempLabel = Optional.absent();
    @NonNull Optional<String> label = Optional.absent();
    @Setter(AccessLevel.PACKAGE) @NonNull Optional<IRI> iri = Optional.absent();

    private boolean isMoving = false;
    private boolean isRenaming = false;

    private int width  = 120;
    private int height = 80;
    private int rounding = 20;
    final private TextBox wLabel = new TextBox();
    final private Label wQueryResult = new Label("???");

    private Concept thisConcept() {
        return this;
    }

    public String getCurveId() {
        return this.id + "_curve";
    }

    @Override
    public void onLoad() {
        this.getElement().setId(this.id);
        this.setWidth((this.width + 90) + "px");
        this.setHeight((this.height + 10) + "px");
        super.onLoad();

        final DraggableShape wCurve = new DraggableRect(this.width, this.height, this.rounding);
        wCurve.getElement().setId(getCurveId());

        this.add(this.wLabel, this.width + 5, 5);
        this.wLabel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                isRenaming = true;
            }
        });
        this.wLabel.addKeyUpHandler(new RenameHandler(this, wLabel));
        this.wLabel.setReadOnly(true);

        this.add(this.wQueryResult, this.width + 5, 25);

        final Panel wButtons = new HorizontalPanel();
        wButtons.getElement().setClassName("concept-button");

        final Button wTempQuery = new Button("?");
        wTempQuery.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                wQueryResult.setText("!!!");
                conceptManager.checkClassName(thisConcept());
            }
        });
        final Button wDelete = new Button("X");
        wDelete.addClickHandler(new DeleteHandler(this));
        wButtons.add(wTempQuery);
        wButtons.add(wDelete);
        this.add(wButtons, this.width + 5, this.height - 10);
        this.add(wCurve, 1, 1);
    }


    public Concept copyTemplate(@NonNull final AbsolutePanel container,
                                @NonNull final String idPrefix,
                                final int counter) {

        Concept copy  = new Concept(idPrefix + counter, conceptManager);
        copy.setLabel(this.getLabel());
        container.add(copy, container.getWidgetLeft(this), container.getWidgetTop(this));
        copy.getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);
        copy.getElement().setClassName("template");
        TemplateHandler.addHandler(container, this, copy, idPrefix, counter);
        makeDraggable("#" + copy.getId());
        return copy;
    }

    public void mouseOverHighlight() {
        this.getElement().setClassName("concept-over");
    }

    @Override
    public void onMouseOver(MouseOverEvent event) {
        mouseOverHighlight();
        wLabel.setReadOnly(false);
    }

    @Override
    public void onMouseOut(MouseOutEvent event) {
        if (! this.isRenaming) {
            this.getElement().setClassName("concept");
            wLabel.setReadOnly(true);
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

    public void setLabel(@NonNull Optional<String> label) {
        GWT.log("[CONCEPT] setLabel" + label + "(was " + this.label + "," + this.tempLabel + ")");
        setTempLabel(label);
        this.label = label;
        wLabel.setText(label.or(""));
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

    public void switchToConceptMode() {
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
    public void startTemplateMode(final String label) {
        this.setLabel(Optional.of("CONCEPT"));
        this.getElement().getStyle().setVisibility(Style.Visibility.HIDDEN);
        this.getElement().setClassName("template");
        this.wLabel.setReadOnly(true);
    }

    private native void makeDraggable(String draggableId) /*-{
        $wnd.make_draggable(draggableId);
        }-*/;
}
