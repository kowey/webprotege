/*
 * Copyright 2010-2011 Hydro4GE, Incorporated. http://www.hydro4ge.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ontologyengineering.protege.web.client.ui.shape;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.hydro4ge.raphaelgwt.client.Raphael;
import org.ontologyengineering.protege.web.client.util.Rectangle;

import java.util.HashMap;
import java.util.Map;

public abstract class DraggableShape extends Raphael
        implements HasMouseDownHandlers, HasMouseUpHandlers, HasMouseMoveHandlers, HasMouseOutHandlers
{
    /*gwtnofinal*/ private Map<String, String> attrs;

    private Raphael parent;
    private Shape shape;

    class DragEndHandler implements MouseUpHandler {
        private DraggableShape dshape;

        public DragEndHandler(DraggableShape dshape) {
            super();
            this.dshape = dshape;
        }

        @Override
        public void onMouseUp(MouseUpEvent event) {
        }
    }

    /**
     * @param parent can be null
     */
    public DraggableShape(Raphael parent, int width, int height) {
        super(width, height);
        this.parent = parent;
        this.attrs  = new HashMap<String, String>();
        this.addMouseUpHandler(new DragEndHandler(this));
    }

  /* use this version if RootPanel is set to position relative */
  /*
  private int parentRelativeTop(AbsolutePanel p) {
    return (this.parent == null)
            ? 0
            : this.parent.getAbsoluteTop() - p.getAbsoluteTop();
  }

  private int parentRelativeLeft(AbsolutePanel p) {
    return (this.parent == null)
            ? 0
            : this.parent.getAbsoluteLeft() - p.getAbsoluteLeft();
  }
  */

    // use this version if RootPanel is absolute
    private int parentRelativeTop(AbsolutePanel p) {
        return (this.parent == null)
                ? 0
                : this.parent.getAbsoluteTop();
    }

    private int parentRelativeLeft(AbsolutePanel p) {
        return (this.parent == null)
                ? 0
                : this.parent.getAbsoluteLeft();
    }

    protected abstract Shape createShape();
    protected abstract int relativeShapeTop(int cx, int cy);
    protected abstract int relativeShapeLeft(int cx, int cy);

    /**
     * Bounding box for this shape
     */
    public Rectangle getAbsoluteBBox() {
        int x1 = getAbsoluteLeft();
        int x2 = x1 + getOffsetWidth();
        int y1 = getAbsoluteTop();
        int y2 = y1 + getOffsetHeight();
        return new Rectangle(x1, y1, x2, y2);
    }

    /**
     *
     * @param p
     * @param cx
     * @param cy
     */
    public void addToPanel(AbsolutePanel p, int cx, int cy) {
        int top_x = this.relativeShapeLeft(cx, cy);// + parentRelativeLeft(p);
        int top_y = this.relativeShapeTop(cx, cy);//  + parentRelativeTop(p);
        p.add(this, top_x, top_y);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.shape = createShape();
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            this.shape.attr(entry.getKey(), entry.getValue());
        }
    }

    public void attr(String key, String value) {
        attrs.put(key, value);
        if (this.shape != null) {
            this.shape.attr(key, value);
        }
    }

    public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
        return this.addDomHandler(handler, MouseDownEvent.getType());
    }

    public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
        return this.addDomHandler(handler, MouseUpEvent.getType());
    }

    public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
        return this.addDomHandler(handler, MouseMoveEvent.getType());
    }

    public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
        return this.addDomHandler(handler, MouseOutEvent.getType());
    }

}

