package edu.stanford.bmir.protege.web.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.stanford.bmir.protege.web.client.rpc.data.UserData;
import edu.stanford.bmir.protege.web.shared.user.UserId;

/**
 * @author z.khan
 */
public class AuthenticateServiceManager {

    private static AuthenticateServiceAsync proxy;
    static AuthenticateServiceManager instance;

    private AuthenticateServiceManager() {
        proxy = (AuthenticateServiceAsync) GWT.create(AuthenticateService.class);
    }

    public static AuthenticateServiceManager getInstance() {
        if (instance == null) {
            instance = new AuthenticateServiceManager();
        }
        return instance;
    }

    //log in with https
    public void validateUserAndAddInSession(String name, String password, AsyncCallback<UserData> cb) {
        proxy.validateUserAndAddInSession(name, password, cb);
    }

    //change password
    public void validateUser(UserId userId, String password, AsyncCallback<UserData> cb) {
        proxy.validateUser(userId.getUserName(), password, cb);
    }

    //change password with https
    public void changePassword(UserId userId, String password, AsyncCallback<Void> cb) {
        proxy.changePassword(userId.getUserName(), password, cb);
    }

    public void sendPasswordReminder(UserId userId, AsyncCallback<Void> cb) {
        proxy.sendPasswordReminder(userId.getUserName(), cb);
    }

    //create new user via https
    public void registerUser(UserId userId, String password, String email, AsyncCallback<UserData> cb) {
        proxy.registerUser(userId.getUserName(), password, email, cb);
    }
}
