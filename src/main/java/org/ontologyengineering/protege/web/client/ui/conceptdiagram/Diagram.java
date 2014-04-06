package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.gwt.core.client.GWT;
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
 *
 * Note that the structures here are somewhat reduced with respect to
 * the original. This is with duplicate sources of information removed
 **/
public class Diagram implements Serializable {
    private HashMap<IRI, IRI> immediateParents;
    private ArrayList<CurveCore> namedCurves;

    public Diagram() {
        this.immediateParents = new HashMap<IRI, IRI>();
        this.namedCurves = new ArrayList<CurveCore>();
    }

    public Diagram(RealDiagram diagram) {
        this.immediateParents = diagram.immediateParents;
        this.namedCurves = new ArrayList<CurveCore>(diagram.namedCurves.values());
    }

    public static RealDiagram unpack(@NonNull final Diagram diagram) {
        RealDiagram result = new RealDiagram();
        result.immediateParents = diagram.immediateParents;
        result.names = HashBiMap.create();
        for (CurveCore curveCore : diagram.namedCurves) {
            if ((! curveCore.getIri().isPresent()) || (! curveCore.getLabel().isPresent())) {
                final String msg = "Don't know how to unpack diagrams with anonymous curves";
                throw new IllegalArgumentException(msg);
            }
            final IRI iri = curveCore.getIri().get();
            final String name = curveCore.getLabel().get();
            result.names.put(iri, name);
            result.namedCurves.put(iri, curveCore);
        }
        return result;
    }
}
