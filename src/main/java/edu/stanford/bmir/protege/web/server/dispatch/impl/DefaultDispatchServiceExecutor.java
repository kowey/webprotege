package edu.stanford.bmir.protege.web.server.dispatch.impl;

import edu.stanford.bmir.protege.web.client.dispatch.ActionExecutionException;
import edu.stanford.bmir.protege.web.server.dispatch.*;
import edu.stanford.bmir.protege.web.server.dispatch.validators.UserHasProjectWritePermissionValidator;
import edu.stanford.bmir.protege.web.shared.dispatch.Action;
import edu.stanford.bmir.protege.web.shared.dispatch.DispatchServiceResultContainer;
import edu.stanford.bmir.protege.web.shared.dispatch.Result;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 19/02/2013
 */
public class DefaultDispatchServiceExecutor implements DispatchServiceHandler {

    // TODO: Injection of registry with GIN
    private ActionHandlerRegistry handlerRegistry = new DefaultActionHandlerRegistry();


    @Override
    public <A extends Action<R>, R extends Result> DispatchServiceResultContainer execute(A action, RequestContext requestContext, ExecutionContext executionContext) throws ActionExecutionException {
        ActionHandler<A, R> actionHandler = handlerRegistry.getActionHandler(action);
        R result = actionHandler.execute(action, executionContext);
        return new DispatchServiceResultContainer(result);
    }
}
