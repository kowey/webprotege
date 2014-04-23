package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import edu.stanford.bmir.protege.web.shared.DataFactory;
import lombok.Getter;
import lombok.NonNull;
import org.ontologyengineering.protege.web.client.ui.curve.CurveCore;
import org.semanticweb.owlapi.model.IRI;

import java.util.Collection;
import java.util.HashMap;

/**
 *
 */
@Getter
public class Diagram {
    final private HashMap<IRI, IRI> immediateParents;
    final private Multimap<IRI, CurveCore> namedCurves;
    final private BiMap<IRI, String> names;

    public Diagram() {
        this.immediateParents =  new HashMap<IRI, IRI>();
        this.namedCurves =  HashMultimap.create();
        this.names = HashBiMap.create();
    }

    public Diagram(@NonNull final DiagramNub nub) {
        this.immediateParents = nub.getImmediateParents();
        this.namedCurves =  HashMultimap.create();
        this.names = HashBiMap.create();
        for (CurveCore curveCore : nub.getNamedCurves()) {
            if ((! curveCore.getIri().isPresent()) || (! curveCore.getLabel().isPresent())) {
                final String msg = "Don't know how to unpack diagrams with anonymous curves";
                throw new IllegalArgumentException(msg);
            }
            final IRI iri = curveCore.getIri().get();
            final String name = curveCore.getLabel().get();
            this.names.put(iri, name);
            this.namedCurves.put(iri, curveCore);
        }
    }
    /**
     * Completely replace the contents of this diagram with those of another
     *
     * @param that
     */
    public void replaceWith(@NonNull final Diagram that) {
        this.immediateParents.clear();
        this.namedCurves.clear();
        this.names.clear();
        this.immediateParents.putAll(that.immediateParents);
        this.namedCurves.putAll(that.namedCurves);
        this.names.putAll(that.names);
    }

    /**
     * Assumed to be owl:Thing if not known
     */
    public IRI getImmediateParent(@NonNull IRI iri) {
        if (immediateParents.containsKey(iri)) {
            return immediateParents.get(iri);
        } else {
            return DataFactory.getOWLThing().getIRI();
        }
    }

    public void setImmediateParent(@NonNull IRI iri,
                                   @NonNull IRI parent) {
        immediateParents.put(iri, parent);
    }

    public Collection<CurveCore> getCurves() {
        return namedCurves.values();
    }

    public Collection<CurveCore> getCurves(@NonNull final IRI iri) {
        return namedCurves.get(iri);
    }

    /**
     * Note the name is ignored if we already know a name for the given iri
     *
     * @param iri
     * @param curve
     */
    public void addCurve(@NonNull final IRI iri,
                         @NonNull final String name,
                         @NonNull final CurveCore curve) {
        namedCurves.put(iri, curve);
        if (! names.containsKey(iri)) {
            names.put(iri, name);
        }
    }

    public void addCurves(@NonNull final IRI iri,
                          @NonNull final String name,
                          @NonNull final Iterable<CurveCore> curves) {
        namedCurves.putAll(iri, curves);
        if (! names.containsKey(iri)) {
            names.put(iri, name);
        }
    }

    public void removeCurve(@NonNull final IRI iri,
                            @NonNull final CurveCore curve) {
        namedCurves.remove(iri, curve);
        if (!namedCurves.containsKey(iri)) {
            names.remove(iri);
        }
    }

    public void removeCurves(@NonNull final IRI iri) {
        namedCurves.removeAll(iri);
        names.remove(iri);
    }

    public boolean hasName(@NonNull final String name) {
        return names.inverse().containsKey(name);
    }

    /**
     * Attribute the given name to given IRI, regardless of
     * whether or not the name/iri are already associated
     * to something else
     *
     * @param iri
     * @param name
     */
    public void forceName(@NonNull final IRI iri,
                          @NonNull final String name) {
        names.forcePut(iri, name);
    }

    /**
     * Precondition: hasName(name)
     *
     * @param name
     * @return
     */
    public IRI getIRI(@NonNull final String name) {
        return names.inverse().get(name);
    }

    /**
     * Precondition: we know oldIri and don't already know newIri
     *
     * @param oldIri
     * @param newIri
     */
    public void renameIri(@NonNull final IRI oldIri,
                          @NonNull final IRI newIri) {

        if (names.containsKey(newIri)) {
            final String oops = "Can't rename " + oldIri.toString() +
                    " to " + newIri.toString() + " because the latter already exists";
            throw new IllegalArgumentException(oops);
        }

        Collection<CurveCore> affectedCurves = namedCurves.get(oldIri);
        final String name = names.get(oldIri);
        removeCurves(oldIri);
        addCurves(newIri, name, affectedCurves);
    }

}
