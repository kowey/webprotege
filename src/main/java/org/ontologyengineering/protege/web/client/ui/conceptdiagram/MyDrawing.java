package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

/*
 * Copyright 2010 Hydro4GE, Incorporated. http://www.hydro4ge.com/
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

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.hydro4ge.raphaelgwt.client.PathBuilder;
import com.hydro4ge.raphaelgwt.client.Raphael;

import java.util.ArrayList;
import java.util.List;

/**
 * simple drawing demonstration
 */
public class MyDrawing extends Raphael {

    /* center point of the drawing */
    private final int cx;
    private final int cy;
    private final AbsolutePanel panel;

    /**
     * MyDrawing constructor
     */
    public MyDrawing(final AbsolutePanel panel, final int width, final int height) {
        super(width, height);
        this.cx = width/2;
        this.cy = height/2;
        this.panel = panel;
    }

    /**
     * Raphael Text and the Firefox 3.6 SVG implementation do not
     * work together when the text is appended to the drawing
     * before the drawing is appended to the document.  Therefore,
     * we defer the layout to onLoad() here instead of doing it in
     * the constructor.
     */
    @Override
    public void onLoad() {
        super.onLoad();

        final int box1_x = (int)Math.round(cx * 0.5);
        final int box1_y = (int)Math.round(cy * 0.5);

        final int box2_x = (int)Math.round(cx * 0.8);
        final int box2_y = (int)Math.round(cy * 0.5);

      /*
        final Rect box1 = new Rect(box1_x, box1_y, 50,  100, 20);
        final Rect box2 = new Rect(box2_x, box2_y, 150, 100, 20);
        */

        final DraggableShape dbox1 = new DraggableRect(55, 105, 20);
        final DraggableShape dbox2 = new DraggableRect(155, 105, 20);
        this.panel.add(dbox1, box1_x, box1_y);
        this.panel.add(dbox2, box2_x, box2_y);

        List<Widget> shapes = new ArrayList<Widget>();
        //shapes.add(box1);
        //shapes.add(box2);
        shapes.add(dbox1);
        shapes.add(dbox2);

        dbox1.attr("fill", "red");
        dbox2.attr("fill", "#d1b48c");

        dbox1.getElement().setId("koweybox1");
        dbox2.getElement().setId("koweybox2");

        for (Widget s : shapes) {
            s.getElement().setClassName("koweybox");
        }

    }




}