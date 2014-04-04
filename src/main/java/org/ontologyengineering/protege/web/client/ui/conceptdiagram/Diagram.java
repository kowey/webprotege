package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import lombok.NonNull;
import org.ontologyengineering.protege.web.client.ui.pattern.CurveCore;
import org.semanticweb.owlapi.model.IRI;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Just a thin GWT-serializable wrapper around RealDiagram
 * By rights, RealDiagram should be GWT-serializable, but at
 * the time of this writing, I get runtime errors
 **/
public class Diagram implements Serializable {
    private HashMap<IRI, IRI> immediateParents;
    private Map<IRI, ArrayList<CurveCore>> namedCurves;
    private Map<IRI, String> names;

    public Diagram() {
        this.immediateParents = new HashMap<IRI, IRI>();
        this.namedCurves = new HashMap<IRI, ArrayList<CurveCore>>();
        this.names = new HashMap<IRI, String>();
    }

    public Diagram(RealDiagram diagram) {
        this.immediateParents = diagram.immediateParents;
        this.namedCurves =  new HashMap<IRI, ArrayList<CurveCore>>();
        for (Map.Entry<IRI, Collection<CurveCore>> entry : diagram.namedCurves.asMap().entrySet()) {
            namedCurves.put(entry.getKey(), new ArrayList<CurveCore>(entry.getValue()));
        }
        this.names = new HashMap<IRI, String>(diagram.names);
    }

    public static RealDiagram unpack(@NonNull final Diagram diagram) {
        RealDiagram result = new RealDiagram();
        result.immediateParents = diagram.immediateParents;
        result.names = HashBiMap.create(diagram.names);
        for (Map.Entry<IRI, ArrayList<CurveCore>> entry : diagram.namedCurves.entrySet()) {
            result.namedCurves.putAll(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
