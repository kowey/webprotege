package edu.stanford.bmir.protege.web.client.banner;

import edu.stanford.bmir.protege.web.client.Application;
import edu.stanford.bmir.protege.web.client.actionbar.application.*;
import edu.stanford.bmir.protege.web.client.actionbar.project.ProjectActionBar;
import edu.stanford.bmir.protege.web.client.actionbar.project.ShowFreshEntitySettingsHandlerImpl;
import edu.stanford.bmir.protege.web.client.actionbar.project.ShowProjectDetailsHandlerImpl;
import edu.stanford.bmir.protege.web.client.events.UserLoggedInEvent;
import edu.stanford.bmir.protege.web.client.events.UserLoggedInHandler;
import edu.stanford.bmir.protege.web.client.events.UserLoggedOutEvent;
import edu.stanford.bmir.protege.web.client.events.UserLoggedOutHandler;
import edu.stanford.bmir.protege.web.client.project.ActiveProjectChangedEvent;
import edu.stanford.bmir.protege.web.client.project.ActiveProjectChangedHandler;
import edu.stanford.bmir.protege.web.shared.event.EventBusManager;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 23/08/2013
 */
public class BannerPresenter {

    private BannerView bannerView = new BannerViewImpl();

    public BannerPresenter() {
        final ProjectActionBar projectActionBar = bannerView.getProjectActionBar();
        projectActionBar.setProjectId(Application.get().getActiveProject());
        projectActionBar.setShowFreshEntitySettingsHandler(new ShowFreshEntitySettingsHandlerImpl());
        projectActionBar.setShowProjectDetailsHandler(new ShowProjectDetailsHandlerImpl());
        final ApplicationActionBar w = bannerView.getApplicationActionBar();


        EventBusManager.getManager().registerHandler(ActiveProjectChangedEvent.TYPE, new ActiveProjectChangedHandler() {
            @Override
            public void handleActiveProjectChanged(ActiveProjectChangedEvent event) {
                projectActionBar.setProjectId(event.getProjectId());
            }
        });
        EventBusManager.getManager().registerHandler(UserLoggedInEvent.TYPE, new UserLoggedInHandler() {
            @Override
            public void handleUserLoggedIn(UserLoggedInEvent event) {
                projectActionBar.setProjectId(Application.get().getActiveProject());
            }
        });
        EventBusManager.getManager().registerHandler(UserLoggedOutEvent.TYPE, new UserLoggedOutHandler() {
            @Override
            public void handleUserLoggedOut(UserLoggedOutEvent event) {
                projectActionBar.setProjectId(Application.get().getActiveProject());
            }
        });
    }

    public BannerView getView() {
        return bannerView;
    }
}