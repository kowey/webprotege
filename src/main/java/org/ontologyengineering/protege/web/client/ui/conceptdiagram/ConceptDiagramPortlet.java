package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import edu.stanford.bmir.protege.web.client.project.Project;
import edu.stanford.bmir.protege.web.client.rpc.data.EntityData;
import edu.stanford.bmir.protege.web.client.rpc.data.layout.PortletConfiguration;
import edu.stanford.bmir.protege.web.client.ui.portlet.AbstractOWLEntityPortlet;

import java.util.ArrayList;

public class ConceptDiagramPortlet extends AbstractOWLEntityPortlet {

    public ConceptDiagramPortlet(Project project) {
        super(project);
    }

    @Override
    public void initialize() {
        setTitle("You don't suppose? Search");
        reload();
    }

    @Override
    public void setPortletConfiguration(PortletConfiguration portletConfiguration) {
        super.setPortletConfiguration(portletConfiguration);
    }

    @Override
    public void reload() {
        if (_currentEntity == null) {
            return;
        }
        setTitle("HelloWorld results for " + _currentEntity.getBrowserText());
    }

    public ArrayList<EntityData> getSelection() {
        return null;
    }

}

