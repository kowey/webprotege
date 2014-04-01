package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import lombok.Getter;
import org.ontologyengineering.protege.web.client.ui.pattern.CurveCore;
import org.semanticweb.owlapi.model.IRI;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Just a thin GWT-serializable wrapper around Diagram
 * By rights, Diagram should be GWT-serializable, but at
 * the time of this writing, I get runtime errors
 *
 * Note that the structures here are somewhat reduced with respect to
 * the original. This is with duplicate sources of information removed
 **/
@Getter
public class DiagramNub implements Serializable {
    private HashMap<IRI, IRI> immediateParents;
    private ArrayList<CurveCore> namedCurves;

    public DiagramNub() {
        this.immediateParents = new HashMap<IRI, IRI>();
        this.namedCurves = new ArrayList<CurveCore>();
    }

    public DiagramNub(Diagram diagram) {
        this.immediateParents = diagram.getImmediateParents();
        this.namedCurves = new ArrayList<CurveCore>(diagram.getNamedCurves().values());
    }


}
