package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import edu.stanford.bmir.protege.web.client.project.Project;
import edu.stanford.bmir.protege.web.client.rpc.data.EntityData;
import edu.stanford.bmir.protege.web.client.rpc.data.layout.PortletConfiguration;
import edu.stanford.bmir.protege.web.client.ui.portlet.AbstractOWLEntityPortlet;

import java.util.ArrayList;
import java.util.List;

public class ConceptDiagramPortlet extends AbstractOWLEntityPortlet {

    public ConceptDiagramPortlet(Project project) {
        super(project);
    }

    @Override
    public void initialize() {
        setTitle("You don't suppose? Search");
        koweySetup();
        reload();
    }

    @Override
    public void setPortletConfiguration(PortletConfiguration portletConfiguration) {
        super.setPortletConfiguration(portletConfiguration);
    }

    class CreateDiagramHandler implements ClickHandler {

      private final AbsolutePanel vPanel;

      public CreateDiagramHandler(AbsolutePanel vp) {
        this.vPanel = vp;
      }

      @Override
      public void onClick(ClickEvent clickEvent) {
        try {
          MyDrawing d = new MyDrawing(vPanel,
                  300,
                  300);
          vPanel.add(d);
          gwtjsPlumbConnect(jsPlumbToConnect());
        } catch (Exception e) {
          GWT.log("buh?", e);

        }

    }}

    public void koweySetup() {

      AbsolutePanel vPanel = new AbsolutePanel();
      vPanel.getElement().getStyle().setProperty("height", "100%");
      vPanel.getElement().getStyle().setProperty("width", "100%");
      Button btn = new Button("Start diagram");
      vPanel.add(btn);
      btn.addClickHandler(new CreateDiagramHandler(vPanel));
      this.add(vPanel);
      gwtjsPlumbDemo();

    }

    @Override
    public void reload() {

        if (_currentEntity == null) {
            return;
        }

      setTitle("HelloWorld results for [13] " + _currentEntity.getBrowserText());



    }

    public ArrayList<EntityData> getSelection() {
        return null;
    }


  public static native void gwtjsPlumbDemo() /*-{
            $wnd.gwtjsplumbinit();

        }-*/;

  public static native void gwtjsPlumbConnect(JavaScriptObject pairs) /*-{
            $wnd.gwtjsconnect(pairs);

        }-*/;

  // TODO would love to have a function that I could pass in a list of Java pairs/objs to
  public static native JavaScriptObject jsPlumbToConnect() /*-{
      return [{source:"koweybox1", target:"koweybox2"},
              {source:"koweybox3", target:"koweybox4"}];
  }-*/;
}


