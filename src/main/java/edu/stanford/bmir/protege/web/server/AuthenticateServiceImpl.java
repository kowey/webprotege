package edu.stanford.bmir.protege.web.server;

import edu.stanford.bmir.protege.web.client.rpc.AuthenticateService;
import edu.stanford.bmir.protege.web.client.rpc.data.UserData;
import edu.stanford.bmir.protege.web.client.ui.login.constants.AuthenticationConstants;
import edu.stanford.bmir.protege.web.server.app.App;
import edu.stanford.bmir.protege.web.server.app.WebProtegeProperties;
import edu.stanford.bmir.protege.web.shared.user.UserId;
import edu.stanford.smi.protege.server.metaproject.User;
import edu.stanford.smi.protege.util.Log;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.logging.Level;

/**
 * Service for Authenticate module for authenticating user.
 *
 * @author z.khan
 *
 */
public class AuthenticateServiceImpl extends WebProtegeRemoteServiceServlet implements AuthenticateService {

    private static final long serialVersionUID = 5326582825556868383L;

    private boolean isAuthenticateWithOpenId() {
        return WebProtegeProperties.get().isOpenIdAuthenticationEnabled();
    }

    public UserData validateUserAndAddInSession(String name, String password) {
        UserId userId = UserId.getUserId(name);

        HttpServletRequest request = this.getThreadLocalRequest();
        HttpSession session = request.getSession();
        session.setAttribute(AuthenticationConstants.LOGIN_METHOD, AuthenticationConstants.LOGIN_METHOD_WEBPROTEGE_ACCOUNT);

        if (!MetaProjectManager.getManager().hasValidCredentials(name, password)) {
            SessionConstants.removeAttribute(SessionConstants.USER_ID, session);
            return null;
        }

        UserData userData = AuthenticationUtil.createUserData(userId);
        SessionConstants.setAttribute(SessionConstants.USER_ID, userId, session);
        return userData;
    }

    public UserData validateUser(String name, String password) {
        if (!MetaProjectManager.getManager().hasValidCredentials(name, password)) {
            return null;
        }
        Log.getLogger().info("User " + name + " logged in at: " + new Date());
        return AuthenticationUtil.createUserData(UserId.getUserId(name));
    }

    public void changePassword(String userName, String password) {
        MetaProjectManager.getManager().changePassword(userName, password);
    }

    public void sendPasswordReminder(String userName) {
            throw new IllegalArgumentException("Email disabled, so can't send password reminder to  " + userName);
    }

    public UserData registerUser(String userName, String password, String email) {
        return MetaProjectManager.getManager().registerUser(userName, email, password);
    }

}
