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

import com.hydro4ge.raphaelgwt.client.Raphael;

public class DraggableCircle extends DraggableShape
{
  static  final private int PADDING = 5;
  private final int radius;

  /**
   *
   * @param parent can be null
   * @param radius
   */
  public DraggableCircle(Raphael parent, int radius) {
    super(parent, (radius+PADDING)*2, (radius+PADDING)*2);
    this.radius = radius;
  }

  public DraggableCircle(int radius) {
     this(null, radius);
  }

  protected Shape createShape() {
    return new Circle(this.radius+PADDING, this.radius+PADDING, this.radius);
  }

  protected int relativeShapeTop(int cx, int cy) {
    return cy - (radius + PADDING);
  }

  protected int relativeShapeLeft(int cx, int cy) {
    return cx - (radius + PADDING);
  }
}


