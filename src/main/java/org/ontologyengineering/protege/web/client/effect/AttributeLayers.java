package org.ontologyengineering.protege.web.client.effect;

import com.google.common.base.Optional;
import lombok.NonNull;

import java.util.*;

/**
 * Layered SVG attributes: in the concept diagram plugin, we want to provide
 * visual feedback by changing certain attributes on an object
 *
 * For example, we might indicate that an object is part of a search result
 * by highlighting it with a certain colour.
 *
 * We also want to account for the fact that:
 *
 * 1. these attributes are in some sense transient.
 * If you change the search term, the object may no longer match
 * and so must stop being highlighted
 *
 * 2. effects can compose (and be transient).
 * For example, we might have an idiom where you can use a
 * combination of typing in a class name to search for it,
 * and dragging something over it. These orthogonal
 * mechanisms have visual effects of their own, and must
 * revert to previous state when no longer applicable.
 *
 * 3. not all effects touch the same attributes, but some of
 * may touch overlapping sets of attributes
 *
 * 4. effects can be added or removed in any order, although perhaps
 * one can be considered primary in case of ambiguity
 *
 * We're going to introduce some provisional bits of vocabulary here:
 *
 * * an <b>attribute</b> is some string representing an SVG property,
 *   for example, "stroke" (we may make this more abstract
 *   later on)
 * * an <b>effect</b> is a set of attribute value pairs (for example,
 *   set the stroke to green, and the stroke-width to 1)
 * * an <b>idiom</b> is some mechanism that affects the visual appearance
 *   of an object.  When an idiom becomes active, it has an effect.
 *   When it becomes inactive, the effect must removed
 *
 * Possible examples of idioms: different type of matching indicators,
 * highlighting indicators, shading in a concept diagram
 *
 */
public class AttributeLayers {
    // position in stack should only be used to express which attribute wins in case
    // of ambiguity

    // attributes should have defaults - should they declared ahead of time? what if
    // defaults collide? I guess we ignore them

    final private VisualEffect defaultEffect = new VisualEffect("default effect");

    // front of the list is the top of the stack
    // note that we have to search the whole list to find effects
    final private List<VisualEffect> activeEffects = new LinkedList();

    public AttributeLayers() {
        activeEffects.add(defaultEffect);
    }

    /**
     * Add the attributes from the source dictionary to the target
     * dictionary; target dictionary wins in case of collision
     */
    private void mergeAttributes(@NonNull final Map<Key, String> source,
                                 @NonNull final Map<Key, String> target) {

        // set any defaults we don't already know about
        final Iterator it = source.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<Key, String> pair = (Map.Entry)it.next();
            final Key key = pair.getKey();
            final String value = pair.getValue();
            if (! target.containsKey(key)) {
                target.put(key, value);
            }
        }
    }

    /**
     * Add an effect to the top of the list
     *
     * @param effect
     */
    public void addEffect(@NonNull final VisualEffect effect) {
        activeEffects.add(0, effect);
        mergeAttributes(effect.getDefaults(), defaultEffect.getAttributes());
    }

    /**
     * Remove an effect (not necessarily one that is on top)
     *
     * @param effect
     */
    public void removeEffect(@NonNull final VisualEffect effect) {
        activeEffects.remove(effect);
    }

    /**
     * Current set of visual attributes to apply
     *
     * @return attributes grouped by the object they apply on
     */
    public Map<Key, String> getAttributes() {
        final Map<Key, String> attributes = new HashMap();
        for (VisualEffect effect : activeEffects) {
            mergeAttributes(effect.getAttributes(), attributes);
        }
        return attributes;
    }

    public void applyAttributes(Painter painter) {
        final Iterator it = getAttributes().entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<Key, String> pair = (Map.Entry)it.next();
            painter.apply(pair.getKey(), pair.getValue());
        }
    }

    /**
     * This is mainly intended as a helper function to attribute layers
     * of your own design.
     *
     * The idea is that you might be keeping track of attributes coming
     * in from multiple sources (for example, search boxes), each called
     * a context; and that you want to either set/unset the effect
     * coming out of that context.
     *
     * This helper deletes whatever effect (if present) is set for that
     * context, and then add whatever effect (if present) you specify
     *
     * @param contextEffects
     * @param context
     * @param newEffect
     * @param <K> the context type
     */
    public <K> void setContextEffect(Map<K, VisualEffect>contextEffects,
                                     K context,
                                     Optional<VisualEffect> newEffect) {

        if (contextEffects.containsKey(context)) {
            removeEffect(contextEffects.get(context));
        }
        if (newEffect.isPresent()) {
            VisualEffect effect = newEffect.get();
            contextEffects.put(context, effect);
            addEffect(effect);
        }
    }
}
