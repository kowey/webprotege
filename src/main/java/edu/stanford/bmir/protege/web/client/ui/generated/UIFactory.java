package edu.stanford.bmir.protege.web.client.ui.generated;

import com.google.gwt.core.client.GWT;
import edu.stanford.bmir.protege.web.client.project.Project;
import edu.stanford.bmir.protege.web.client.ui.editor.EditorPortlet;
import edu.stanford.bmir.protege.web.client.ui.ontology.annotations.OntologyAnnotationsPortlet;
import edu.stanford.bmir.protege.web.client.ui.ontology.changes.ChangeSummaryPortlet;
import edu.stanford.bmir.protege.web.client.ui.ontology.changes.ChangesPortlet;
import edu.stanford.bmir.protege.web.client.ui.ontology.classes.*;
import edu.stanford.bmir.protege.web.client.ui.ontology.hierarchy.ManageHierarchyPortlet;
import edu.stanford.bmir.protege.web.client.ui.ontology.hierarchy.ManageHierarchyTab;
import edu.stanford.bmir.protege.web.client.ui.ontology.home.OntologiesPortlet;
import edu.stanford.bmir.protege.web.client.ui.ontology.id.OntologyIdPortlet;
import edu.stanford.bmir.protege.web.client.ui.ontology.individuals.IndividualsListPortlet;
import edu.stanford.bmir.protege.web.client.ui.ontology.individuals.IndividualsTab;
import edu.stanford.bmir.protege.web.client.ui.ontology.metadata.AnnotationsPortlet;
import edu.stanford.bmir.protege.web.client.ui.ontology.metadata.ImportsTreePortlet;
import edu.stanford.bmir.protege.web.client.ui.ontology.metadata.MetadataTab;
import edu.stanford.bmir.protege.web.client.ui.ontology.properties.PropertiesTab;
import edu.stanford.bmir.protege.web.client.ui.ontology.properties.PropertiesTreePortlet;
import edu.stanford.bmir.protege.web.client.ui.ontology.restrictions.ConditionsPortlet;
import edu.stanford.bmir.protege.web.client.ui.ontology.revisions.RevisionsPortlet;
import edu.stanford.bmir.protege.web.client.ui.ontology.search.OtherTerminologiesTab;
import edu.stanford.bmir.protege.web.client.ui.portlet.EntityPortlet;
import edu.stanford.bmir.protege.web.client.ui.portlet.html.HtmlMessagePortlet;
import edu.stanford.bmir.protege.web.client.ui.portlet.propertyForm.PropertyFieldPortlet;
import edu.stanford.bmir.protege.web.client.ui.projectfeed.ProjectFeedPortlet;
import edu.stanford.bmir.protege.web.client.ui.tab.AbstractTab;
import edu.stanford.bmir.protege.web.client.ui.tab.UserDefinedTab;
import edu.stanford.bmir.protege.web.client.usage.UsagePortlet;

import java.util.*;

/**
 * This class is supposed to be automatically generated by the GWT generator at
 * compile time. In the current version it is not.
 * <p/>
 * This is a factory class that will provide create methods for all tabs and
 * portlets. The UI configurator will use this class to layout the UI for a
 * particular project and user.
 * <p/>
 * This factory tries to compensate for the lack of reflection support in GWT
 * and JS.
 * @author Tania Tudorache <tudorache@stanford.edu>
 */
public class UIFactory {

    public static final String PORTLET_SUFFIX = "Portlet";

    /*
     * Tab factory
     */

    public static AbstractTab createTab(Project project, String tabJavaClassName) {
        if (tabJavaClassName.equals(ClassesTab.class.getName())) {
            return new ClassesTab(project);
        }
        else if (tabJavaClassName.equals(PropertiesTab.class.getName())) {
            return new PropertiesTab(project);
        }
        else if (tabJavaClassName.equals(IndividualsTab.class.getName())) {
            return new IndividualsTab(project);
        }
        else if (tabJavaClassName.equals(MetadataTab.class.getName())) {
            return new MetadataTab(project);
        }
        else if (tabJavaClassName.equals(OtherTerminologiesTab.class.getName())) {
            return new OtherTerminologiesTab(project);
        }
        else if (tabJavaClassName.equals(UserDefinedTab.class.getName())) {
            return new UserDefinedTab(project);
        }
        else if (tabJavaClassName.equals(ManageHierarchyTab.class.getName())) {
            return new ManageHierarchyTab(project);
        }
        return null;
    }

    /*
     * Portlet factory
     */

    public static EntityPortlet createPortlet(Project project, String portletJavaClassName) {
        try {
            if (portletJavaClassName.equals(AllPropertiesPortlet.class.getName())) {
                return new AllPropertiesPortlet(project);
            }
            else if (portletJavaClassName.equals(AnnotationsPortlet.class.getName())) {
                return new AnnotationsPortlet(project);
            }
            else if (portletJavaClassName.equals(ClassTreePortlet.class.getName())) {
                return new ClassTreePortlet(project);
            }
            else if (portletJavaClassName.equals(ImportsTreePortlet.class.getName())) {
                return new ImportsTreePortlet(project);
            }
            else if (portletJavaClassName.equals(IndividualsListPortlet.class.getName())) {
                return new IndividualsListPortlet(project);
            }
            else if (portletJavaClassName.equals(PropertiesTreePortlet.class.getName())) {
                return new PropertiesTreePortlet(project);
            }
            else if (portletJavaClassName.equals(PropertyFieldPortlet.class.getName())) {
                return new PropertyFieldPortlet(project);
            }
            else if (portletJavaClassName.equals(ChangeSummaryPortlet.class.getName())) {
                return new ChangeSummaryPortlet(project);
            }
            else if (portletJavaClassName.equals(ManageHierarchyPortlet.class.getName())) {
                return new ManageHierarchyPortlet(project);
            }
            else if (portletJavaClassName.equals(HtmlMessagePortlet.class.getName())) {
                return new HtmlMessagePortlet(project);
            }
            else if (portletJavaClassName.equals(SuperclassesPortlet.class.getName())) {
                return new SuperclassesPortlet(project);
            }
            else if (portletJavaClassName.equals(ChangesPortlet.class.getName())) {
                return new ChangesPortlet(project);
            }
            else if (portletJavaClassName.equals(PropertiesViewPortlet.class.getName())) {
                return new PropertiesViewPortlet(project);
            }
            else if (portletJavaClassName.equals(ConditionsPortlet.class.getName())) {
                return new ConditionsPortlet(project);
            }
            else if (portletJavaClassName.equals(RevisionsPortlet.class.getName())) {
                return new RevisionsPortlet(project);
            }
            else if (portletJavaClassName.equals(OntologyIdPortlet.class.getName())) {
                return new OntologyIdPortlet(project);
            }
            else if (portletJavaClassName.equals(OntologyAnnotationsPortlet.class.getName())) {
                return new OntologyAnnotationsPortlet(project);
            }
            else if (portletJavaClassName.equals(ProjectFeedPortlet.class.getName())) {
                return new ProjectFeedPortlet(project);
            }
            else if(portletJavaClassName.equals(EditorPortlet.class.getName())) {
                return new EditorPortlet(project);
            }
            else if(portletJavaClassName.equals(UsagePortlet.class.getName())) {
                return new UsagePortlet(project);
            }
        }
        catch (Exception e) {
            GWT.log("Error when creating portlet", e);
        }
        return null;
    }

    //TODO: taking out ManageHierarchyTab.class.getName() - must be de-icd-ezed
    public static List<String> getAvailableTabNames() {
        /*
         * Removed tabs:
         * OtherTerminologiesTab.class.getName(),
         *
         */
        String[] tabs = {ClassesTab.class.getName(), PropertiesTab.class.getName(), IndividualsTab.class.getName(), MetadataTab.class.getName()};
        return Arrays.asList(tabs);
    }

    public static List<String> getAvailablePortletNames() {
        /*
         * Removed portlets:
         * PropertyFieldPortlet.class.getName()
         * ChangeTreePortlet.class.getName(), ChangeSummaryPortlet.class.getName(), ChangeTablePortlet.class.getName(),
         * NotesPortlet.class.getName(),
         */
        String[] portlets = {UsagePortlet.class.getName(), OntologyIdPortlet.class.getName(), OntologyAnnotationsPortlet.class.getName(), EditorPortlet.class.getName(),
                ChangeSummaryPortlet.class.getName(), ProjectFeedPortlet.class.getName(), AnnotationsPortlet.class.getName(), AllPropertiesPortlet.class.getName(), PropertiesViewPortlet.class.getName(), ClassTreePortlet.class.getName(), ImportsTreePortlet.class.getName(), IndividualsListPortlet.class.getName(),
                OntologiesPortlet.class.getName(), PropertiesTreePortlet.class.getName(), ConditionsPortlet.class.getName(),
                SuperclassesPortlet.class.getName(), ChangesPortlet.class.getName(),
                RevisionsPortlet.class.getName()};

        List<String> portletsList = Arrays.asList(portlets);
        Collections.sort(portletsList, new Comparator<String>() {
            public int compare(String p1, String p2) {
                String n1 = p1.substring(p1.lastIndexOf(".") + 1);
                String n2 = p2.substring(p2.lastIndexOf(".") + 1);
                return n1.compareTo(n2);
            }
        });
        return portletsList;
    }

    /*
     * Generic utils
     */

    public static List<String> getAvailableTabShortNames() {
        List<String> tabs = getAvailableTabNames();
        List<String> shortNames = new ArrayList<String>();
        for (String tab : tabs) {
            shortNames.add(tab.substring(tab.lastIndexOf(".") + 1));
        }
        return shortNames;
    }

    /**
     * @return A map from the short names of the portlets to the full java class
     *         name of the portlets.
     */
    public static Map<String, String> getAvailablePortletNameMap() {
        List<String> portlets = getAvailablePortletNames();
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (String portlet : portlets) {
            map.put(getPortletShortName(portlet), portlet);
        }
        return map;
    }

    /**
     * Extracts a short human readable name from a portlet java class name.
     * @param portletClassName The fully qualified class name for the portlet
     * @return The short name for the portlet (not null).
     */
    private static String getPortletShortName(String portletClassName) {
        int simpleNameSeparator = portletClassName.lastIndexOf(".");
        if(simpleNameSeparator == -1) {
            return portletClassName;
        }
        String simpleName = portletClassName.substring(simpleNameSeparator + 1);
        String strippedName;
        if(simpleName.endsWith(PORTLET_SUFFIX)) {
            strippedName = simpleName.substring(0, simpleName.length() - PORTLET_SUFFIX.length());
        }
        else {
            strippedName = simpleName;
        }
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < strippedName.length(); i++) {
            char ch = strippedName.charAt(i);
            boolean upperCase = Character.isUpperCase(ch);
            if(upperCase) {
                if (isNextCharacterLowerCase(strippedName, i) || isPreviousCharacterLowerCase(strippedName, i)) {
                    sb.append(" ");
                }
            }
            sb.append(ch);
        }
        return sb.toString().trim();
    }

    private static boolean isPreviousCharacterLowerCase(String strippedName, int i) {
        return i > 0 && Character.isLowerCase(strippedName.charAt(i - 1));
    }

    private static boolean isNextCharacterLowerCase(String s, int currentIndex) {
        return currentIndex < s.length() - 1 && Character.isLowerCase(s.charAt(currentIndex + 1));
    }
    

    /**
     * @return A map from the short names of tabs (label) to the full java class
     *         name of the tabs.
     */
    public static Map<String, String> getAvailableTabNameMap() {
        List<String> tabs = getAvailableTabNames();
        Collections.sort(tabs);
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (String tab : tabs) {
            map.put(tab.substring(tab.lastIndexOf(".") + 1), tab);
        }
        return map;
    }

}