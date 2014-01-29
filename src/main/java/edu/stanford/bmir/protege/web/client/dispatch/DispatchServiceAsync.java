package edu.stanford.bmir.protege.web.client.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.stanford.bmir.protege.web.shared.dispatch.Action;
import edu.stanford.bmir.protege.web.shared.dispatch.DispatchServiceResultContainer;

public interface DispatchServiceAsync {

    void executeAction(Action action, AsyncCallback<DispatchServiceResultContainer> async) throws ActionExecutionException;
}
