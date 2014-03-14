package org.ontologyengineering.protege.web.client.util;

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
}
