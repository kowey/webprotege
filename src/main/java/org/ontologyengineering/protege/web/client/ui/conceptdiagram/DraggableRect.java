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

package org.ontologyengineering.protege.web.client.ui.conceptdiagram;

import com.hydro4ge.raphaelgwt.client.Raphael;

public class DraggableRect extends DraggableShape
{
  private final int height;
  private final int width;
  private final int rounding;

  /**
   *
   * @param parent can be null
   * @param width
   * @param height
   * @param rounding
   */
  public DraggableRect(Raphael parent, int width,  int height, int rounding) {
    super(parent, width, height);
    this.width = width;
    this.height = height;
    this.rounding = rounding;
  }

  public DraggableRect(int width, int height, int rounding) {
     this(null, width, height, rounding);
  }

  protected Shape createShape() {
    return new Rect(0, 0, this.width, this.height, this.rounding);
  }

  protected int relativeShapeTop(int cx, int cy) {
    return cy;
  }

  protected int relativeShapeLeft(int cx, int cy) {
    return cx;
  }
}


