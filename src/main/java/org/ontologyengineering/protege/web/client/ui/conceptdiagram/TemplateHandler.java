package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.AbsolutePanel;
import lombok.Data;
import lombok.NonNull;

/**
 * Created by kowey on 2014-02-03.
 */
public @Data
class TemplateHandler implements MouseMoveHandler {

    private @NonNull final AbsolutePanel container;
    // gwt compiler gets confused if we call this template
    // no kidding! (this is due to lombok's name mangling)
    private @NonNull final Concept _template;
    private @NonNull final Concept copy;
    private @NonNull final String prefix;
    private final int counter;
    private HandlerRegistration registration;

    private final static int GAP = 5;

    @Override
    public void onMouseMove(MouseMoveEvent event) {
        if (this.isFarFromTemplate()) {
            _template.copyTemplate(container,
                    prefix,
                    counter + 1);
            if (registration != null) {
                registration.removeHandler();
                copy.switchToConceptMode();
            }
        }
    }

    public static void addHandler(final AbsolutePanel container,
                                  final Concept template,
                                  final Concept copy,
                                  final String  idPrefix,
                                  final int counter) {
        TemplateHandler handler = new TemplateHandler(container, template, copy, idPrefix, counter);
        HandlerRegistration reg =
                copy.addDomHandler(handler, MouseMoveEvent.getType());
        handler.setRegistration(reg);
    }

    private boolean isFarFromTemplate() {
        return  Math.abs(copy.getAbsoluteLeft() - _template.getAbsoluteLeft()) > GAP ||
                Math.abs(copy.getAbsoluteTop()  - _template.getAbsoluteTop()) > GAP;
    }
}