package org.ontologyengineering.protege.web.client.util;

import lombok.Data;

/**
 * Abstract representation of a 2D rectangle.
 * Used mainly for things like bounding boxes
 */
@Data
public class Rectangle {
    private final int left;
    private final int top;
    private final int right;
    private final int bottom;

    private boolean lineIntersects(int a1, int a2, int b1, int b2) {
        return (a1 <= b2 && a1 >= b1)  // left point inside
            || (a2 <= b2 && a2 >= b1)  // right point inside
            || (a1 <= b1 && a2 >= b2); // both points outside
    }

    public boolean intersects(Rectangle other) {
        return  lineIntersects(other.left, other.right, this.left, this.right) &&
                lineIntersects(other.top, other.bottom, this.top, this.bottom);

    }
}
