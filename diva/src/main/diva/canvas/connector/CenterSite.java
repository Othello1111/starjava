/*
 * $Id: CenterSite.java,v 1.2 2000/05/02 00:43:18 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas.connector;

import diva.canvas.AbstractSite;
import diva.canvas.Site;
import diva.canvas.Figure;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** A concrete implementation of Site that is located in the
 * center of the bounding box of a figure. This is a utility class
 * provided for convenience of figures that need to make their
 * center points connectible.
 *
 * @version	$Revision: 1.2 $
 * @author 	John Reekie
 */
public class CenterSite extends AbstractSite {

    /** The id
     */
    private int _id;

    /** The parent figure
     */
    private Figure _parentFigure;

    /** Create a new site on the given figure. The site will have 
     * the ID zero.
     *
     * @ FIXME deprecated Use the constructor that takes an ID.
     */ 
    public CenterSite (Figure figure) {
        this(figure,0);
    }

    /** Create a new site on the given figure and with the given ID
     */ 
    public CenterSite (Figure figure, int id) {
        this._id = id;
        this._parentFigure = figure;
    }

    /** Get the figure to which this site is attached.
     */
    public Figure getFigure () {
        return _parentFigure;
    }

    /** Get the ID of this site.
     */
    public int getID () {
        return _id;
    }

    /** Get the x-coordinate of the site. The site
     * is located in the center of the parent figure's bounding
     * box.
     */
    public double getX () {
        Rectangle2D bounds = _parentFigure.getBounds();
        return bounds.getX() + bounds.getWidth()/2;
    }

    /** Get the y-coordinate of the site.  The site
     * is located in the center of the parent figure's bounding
     * box.
     */
    public double getY () {
        Rectangle2D bounds = _parentFigure.getBounds();
        return bounds.getY() + bounds.getHeight()/2;
    } 
}

