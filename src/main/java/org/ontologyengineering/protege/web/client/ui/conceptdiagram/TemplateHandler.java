package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Widget;
import lombok.Data;
import lombok.NonNull;
import org.ontologyengineering.protege.web.client.ui.pattern.Curve;
import org.ontologyengineering.protege.web.client.ui.pattern.Pattern;

/**
 * Created by kowey on 2014-02-03.
 */
public @Data
class TemplateHandler implements MouseMoveHandler {

    private @NonNull final AbsolutePanel container;
    // gwt compiler gets confused if we call this template
    // no kidding! (this is due to lombok's name mangling)
    private @NonNull final Curve _template;
    private @NonNull final Curve copy;
    private HandlerRegistration registration;

    private final static int GAP = 5;

    @Override
    public void onMouseMove(MouseMoveEvent event) {
        if (this.isFarFromTemplate()) {
            _template.copyTemplate(container);
            if (registration != null) {
                registration.removeHandler();
                copy.switchToInstanceMode();
            }
        }
    }

    public static void addHandler(final AbsolutePanel container,
                                  final Curve template,
                                  final Curve copy) {
        TemplateHandler handler = new TemplateHandler(container, template, copy);
        HandlerRegistration reg =
                copy.getWidget().addDomHandler(handler, MouseMoveEvent.getType());
        handler.setRegistration(reg);
    }

    private boolean isFarFromTemplate() {
        Widget copyWidget = copy.getWidget();
        Widget templateWidget = _template.getWidget();
        return  Math.abs(copyWidget.getAbsoluteLeft() - templateWidget.getAbsoluteLeft()) > GAP ||
                Math.abs(copyWidget.getAbsoluteTop()  - templateWidget.getAbsoluteTop()) > GAP;
    }
}