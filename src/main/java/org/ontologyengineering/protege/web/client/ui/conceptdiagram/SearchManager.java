package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.gwt.user.client.ui.TextBox;

/**
 * Created by eykk10 on 3/12/14.
 */
public interface SearchManager {

    public interface SearchHandler {
        public void bind();
    };
    public SearchHandler makeSearchHandler(TextBox textbox, String color);

}
