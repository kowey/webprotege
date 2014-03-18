package org.ontologyengineering.protege.web.client.effect;

/**
 * Basically a lambda term for applying effects
 */
abstract public class Painter {
    abstract public void apply(Key key, String value);
}
