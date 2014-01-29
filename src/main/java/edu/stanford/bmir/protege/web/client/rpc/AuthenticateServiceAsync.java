/**
 * 
 */
package edu.stanford.bmir.protege.web.client.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.stanford.bmir.protege.web.client.rpc.data.UserData;


/**
 * @author z.khan
 *
 */
public interface AuthenticateServiceAsync {
    
    void validateUserAndAddInSession(String name, String password, AsyncCallback<UserData> callback);

    void validateUser(String name, String password, AsyncCallback<UserData> cb);
    
    void changePassword(String userName, String password, AsyncCallback<Void> callback);

    void sendPasswordReminder(String userName, AsyncCallback<Void> callback);

    void registerUser(String name, String password, String email, AsyncCallback<UserData> callback);

}