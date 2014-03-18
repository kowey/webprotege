package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.google.common.base.Optional;
import com.google.gwt.user.client.ui.TextBox;
import org.ontologyengineering.protege.web.client.ui.pattern.Concept;
import org.ontologyengineering.protege.web.client.ui.shape.DraggableShape;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by eykk10 on 3/12/14.
 */
public interface SearchManager {

    public interface SearchHandler {
        public void bind();
        public String getColor();
        public Optional<Collection<Concept>> getMatching();
        public Optional<Collection<Concept>> getNonMatching();
    }

    public enum MatchStatus {
        NO_MATCH,
        PARTIAL_MATCH,
        UNIQUE_MATCH;

        public MatchStatus getNext() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public SearchHandler makeSearchHandler(TextBox textbox, String color);

    /**
     * Given a curve that we're trying to drag out onto the canvas,
     * what are the possible matches?
     *
     * @param dragged
     * @return possible matches in preference order
     */
    public List<Concept> getSnapCandidates(DraggableShape dragged);

}
