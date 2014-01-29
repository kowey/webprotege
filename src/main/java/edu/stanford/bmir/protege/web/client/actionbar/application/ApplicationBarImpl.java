package edu.stanford.bmir.protege.web.client.actionbar.application;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import edu.stanford.bmir.protege.web.client.Application;
import edu.stanford.bmir.protege.web.client.ui.library.msgbox.MessageBox;
import edu.stanford.bmir.protege.web.client.ui.library.popupmenu.PopupMenu;
import edu.stanford.bmir.protege.web.client.ui.res.WebProtegeClientBundle;
import edu.stanford.bmir.protege.web.shared.user.UserId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 22/08/2013
 */
public class ApplicationBarImpl extends Composite implements ApplicationActionBar {



    private ShowAboutBoxHandler showAboutBoxHandler = new ShowAboutBoxHandler() {
        @Override
        public void handleShowAboutBox() {
        }
    };

    private ShowUserGuideHandler showUserGuideHandler = new ShowUserGuideHandler() {
        @Override
        public void handleShowUserGuide() {
        }
    };

    interface ApplicationBarImplUiBinder extends UiBinder<HTMLPanel, ApplicationBarImpl> {

    }

    private static ApplicationBarImplUiBinder ourUiBinder = GWT.create(ApplicationBarImplUiBinder.class);

    @UiField
    protected ButtonBase helpItem;

    public ApplicationBarImpl() {
        HTMLPanel rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);
        helpItem.setHTML("Help&nbsp;&nbsp;&#x25BE");
    }

    @UiHandler("helpItem")
    protected void handleHelpItemClicked(ClickEvent clickEvent) {
        PopupMenu popupMenu = new PopupMenu();
        popupMenu.addItem("User guide", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                showUserGuideHandler.handleShowUserGuide();
            }
        });
        popupMenu.addItem("Send feedback", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                MessageBox.showMessage("Send us feedback", WebProtegeClientBundle.BUNDLE.feedbackBoxText().getText());
            }
        });
        popupMenu.addItem("About", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                showAboutBoxHandler.handleShowAboutBox();
            }
        });
        popupMenu.showRelativeTo(helpItem);
    }

    @Override
    public void setShowAboutBoxHandler(ShowAboutBoxHandler showAboutBoxHandler) {
        this.showAboutBoxHandler = checkNotNull(showAboutBoxHandler);
    }

    @Override
    public void setShowUserGuideHandler(ShowUserGuideHandler showUserGuideHandler) {
        this.showUserGuideHandler = checkNotNull(showUserGuideHandler);
    }
}