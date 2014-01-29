package edu.stanford.bmir.protege.web.client;


import com.google.common.base.Optional;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.gwtext.client.widgets.MessageBox;
import edu.stanford.bmir.protege.web.client.rpc.AbstractAsyncHandler;
import edu.stanford.bmir.protege.web.client.rpc.AdminServiceManager;
import edu.stanford.bmir.protege.web.client.rpc.data.LoginChallengeData;
import edu.stanford.bmir.protege.web.client.rpc.data.SignupInfo;
import edu.stanford.bmir.protege.web.client.rpc.data.UserData;
import edu.stanford.bmir.protege.web.client.ui.library.dlg.WebProtegeDialogCloser;
import edu.stanford.bmir.protege.web.client.ui.login.HashAlgorithm;
import edu.stanford.bmir.protege.web.client.ui.login.constants.AuthenticationConstants;
import edu.stanford.bmir.protege.web.client.ui.ontology.accesspolicy.InvitationConstants;
import edu.stanford.bmir.protege.web.client.ui.ontology.accesspolicy.InviteUserUtil;
import edu.stanford.bmir.protege.web.client.ui.verification.NullHumanVerificationServiceProvider;
import edu.stanford.bmir.protege.web.client.workspace.WorkspaceViewImpl;
import edu.stanford.bmir.protege.web.shared.app.WebProtegePropertyName;
import edu.stanford.bmir.protege.web.shared.user.EmailAddress;
import edu.stanford.bmir.protege.web.shared.user.UserEmailAlreadyExistsException;
import edu.stanford.bmir.protege.web.shared.user.UserId;
import edu.stanford.bmir.protege.web.shared.user.UserNameAlreadyExistsException;


/**
 * @author Jennifer Vendetti <vendetti@stanford.edu>
 * @author Tania Tudorache <tudorache@stanford.edu>
 */
public class WebProtege implements EntryPoint {

    public void onModuleLoad() {
        Application.init(new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                GWT.log("There was a problem initializing WebProtege", caught);
            }

            @Override
            public void onSuccess(Void result) {
                GWT.log("Application initialization complete.  Starting UI Initialization.");
                handleUIInitialization();
            }
        });
    }

  private class GetSaltAndChallengeForLoginHandler extends AbstractAsyncHandler<LoginChallengeData> {

    private UserId userName;

    private String password;

    private com.gwtext.client.widgets.Window win;

    public GetSaltAndChallengeForLoginHandler(final UserId userName, final String password) {
      this.userName = userName;
      this.password = password;
    }

    @Override
    public void handleSuccess(LoginChallengeData result) {

      if (result != null) {
        HashAlgorithm hAlgorithm = new HashAlgorithm();
        String saltedHashedPass = hAlgorithm.md5(result.getSalt() + password);
        String response = hAlgorithm.md5(result.getChallenge() + saltedHashedPass);
        AdminServiceManager.getInstance().authenticateToLogin(userName, response, new AsyncCallback<UserId>() {

          public void onSuccess(UserId userId) {
            win.getEl().unmask();
            if (!userId.isGuest()) {
              Application.get().setCurrentUser(userId);
            }
          }

          public void onFailure(Throwable caught) {
            MessageBox.alert(AuthenticationConstants.ASYNCHRONOUS_CALL_FAILURE_MESSAGE);
          }
        });
      }
    }

    @Override
    public void handleFailure(Throwable caught) {
      MessageBox.alert(AuthenticationConstants.ASYNCHRONOUS_CALL_FAILURE_MESSAGE);
    }
  }

    private void handleUIInitialization() {
        // This doesn't feel like it belongs here
        if (isInvitation()) {
            InviteUserUtil inviteUserUtil = new InviteUserUtil();
            inviteUserUtil.ProcessInvitation();
        }

        buildUI();
        doFakeLogin();

    }

    private final static String FAKE_USERNAME = "foo";
    private final static String FAKE_PASSWORD = "blop";
    private final static String FAKE_EMAILADDRESS = "me@example.com";

    /**
     * Temporary bypass for the authentication system.
     * This is partly to enable faster logging in, and partly to enable faster
     * building by allowing us to remove the client-side authentication code
     */
    private void doFakeLogin() {
        final SignupInfo data = new SignupInfo(
                new EmailAddress(FAKE_EMAILADDRESS),
                FAKE_USERNAME,
                FAKE_PASSWORD,
                new NullHumanVerificationServiceProvider());

        final AdminServiceManager adminServiceManager = AdminServiceManager.getInstance();
        adminServiceManager.getNewSalt(new AsyncCallback<String>() {
            public void onFailure(Throwable caught) {
                edu.stanford.bmir.protege.web.client.ui.library.msgbox.MessageBox.showAlert("Error", "There was a problem registering the specified user account. Please contact admin. (Problem " + caught.getMessage() + ")");
            }

            public void onSuccess(String salt) {
                HashAlgorithm hashAlgorithm = new HashAlgorithm();
                final String userName = data.getUserName();
                String email = data.getEmailAddress().getEmailAddress();
                String hashedPassword = hashAlgorithm.md5(salt + data.getPassword());
                adminServiceManager.registerUserViaEncrption(userName, hashedPassword, email, new AsyncCallback<UserData>() {
                    public void onFailure(Throwable caught) {
                        if(caught instanceof UserNameAlreadyExistsException) {
                            final UserId fakeUserName = UserId.getUserId(FAKE_USERNAME);
                            final String fakeUserPass = FAKE_PASSWORD;
                            AdminServiceManager.getInstance().getUserSaltAndChallenge(fakeUserName,
                                    new GetSaltAndChallengeForLoginHandler(fakeUserName, fakeUserPass));
                        } else {
                            edu.stanford.bmir.protege.web.client.ui.library.msgbox.MessageBox.showAlert("Error registering account", "There was a problem registering the specified user account.  Please contact administrator.");
                        }
                    }

                    public void onSuccess(UserData result) {
                        edu.stanford.bmir.protege.web.client.ui.library.msgbox.MessageBox.showMessage("Fake User Registration complete for " + userName);
                    }
                });
            }
        });
    }

    /**
     * Checks whether the URL is an invitation URL, by analyzing the parameter
     * <code>InvitationConstants.INVITATION_URL_PARAMETER_IS_INVITATION</code>
     * in URL.
     * @return
     */
    private boolean isInvitation() {
        String isInvitationURL = Window.Location.getParameter(InvitationConstants.INVITATION_URL_PARAMETER_IS_INVITATION);
        return isInvitationURL != null && isInvitationURL.trim().contains("true");
    }


    protected void buildUI() {
        RootPanel.get().add(new WorkspaceViewImpl());

        final Optional<String> appName = Application.get().getClientApplicationProperty(WebProtegePropertyName.APPLICATION_NAME);
        if (appName.isPresent()) {
            Window.setTitle(appName.get());
        }

    }

}
