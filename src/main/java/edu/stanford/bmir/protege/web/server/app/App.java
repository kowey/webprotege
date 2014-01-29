package edu.stanford.bmir.protege.web.server.app;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 05/11/2013
 * <p>
 *     A singleton instance that manages per app functionality on the server side.
 * </p>
 */
public class App {

    private static final App instance = new App();

    private App() {
    }

    public static App get() {
        return instance;
    }

}
