/*
 * $Id: ShapeUtilities.java,v 1.3 2002/08/20 09:57:58 brighton Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

// (Note: Taken from the Diva sources to fix a bug when flipping shapes - Allan)

//package diva.util.java2d;

package jsky.util.java2d;

import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

import diva.util.java2d.Polygon2D;
import diva.util.java2d.Polyline2D;

/** A set of utilities on Java2D shapes.
 *
 * @version $Revision: 1.3 $
 * @author  John Reekie (johnr@eecs.berkeley.edu)
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 */
public final class ShapeUtilities {

    /** Clone a shape. This method is needed because Shape
     * does not define clone(), although many shape classes do.
     */
    public static Shape cloneShape(Shape s) {
        // FIXME Add more specific shapes
        if (s instanceof RectangularShape) {
            return (RectangularShape) ((RectangularShape) s).clone();
        }
        else {
            return new GeneralPath(s);
        }
    }

    /** Create the "cloud" shape. If running in JDK1.2 beta4,
     * generate the shape on-the-fly using the Area
     * class and return it. If running in JDK1.2 final, which
     * has the screwed-up Area code, just generate a hardwired
     * path (using code that was generated by running in beta4!!!)
     * The shape is bounded by the rectangle (0,0,100,100).
     */
    public static Shape createCloudShape() {
	GeneralPath cloud = new GeneralPath();
	cloud.moveTo(25.53f, 25.00f);
	cloud.curveTo(25.18f, 23.42f, 25.0f, 21.75f, 25.0f, 20.0f);
	cloud.curveTo(25.0f, 8.28f, 33.28f, 0.0f, 45.0f, 0.0f);
	cloud.curveTo(54.65f, 0.0f, 61.97f, 5.62f, 64.25f, 14.15f);
	cloud.curveTo(66.90f, 11.53f, 70.63f, 10.0f, 75.0f, 10.0f);
	cloud.curveTo(83.78f, 10.0f, 90.0f, 16.21f, 90.0f, 25.0f);
	cloud.curveTo(90.0f, 27.46f, 89.51f, 29.72f, 88.61f, 31.70f);
	cloud.curveTo(95.57f, 34.70f, 100.0f, 41.43f, 100.0f, 50.0f);
	cloud.curveTo(100.0f, 61.21f, 92.40f, 69.29f, 81.47f, 69.95f);
	cloud.curveTo(76.58f, 79.21f, 66.99f, 85.0f, 55.0f, 85.0f);
	cloud.curveTo(45.01f, 85.0f, 36.69f, 80.98f, 31.36f, 74.30f);
	cloud.curveTo(29.35f, 74.76f, 27.22f, 75.0f, 25.0f, 75.0f);
	cloud.curveTo(10.35f, 75.0f, 0.0f, 64.64f, 0.0f, 50.0f);
	cloud.curveTo(0.0f, 35.35f, 10.35f, 25.0f, 25.0f, 25.0f);
	cloud.curveTo(25.18f, 25.0f, 25.35f, 25.00f, 25.53f, 25.00f);
	cloud.closePath();
	return cloud;
    }

    /** Create the "swatch" shape. If running in JDK1.2 beta4,
     * generate the shape on-the-fly using the Area
     * class and return it. If running in JDK1.2 final, which
     * has the screwed-up Area code, just generate a hardwired
     * path (using code that was generated by running in beta4!!!)
     * The shape is bounded by the rectangle (0,0,100,100).
     */
    public static Shape createSwatchShape() {
	GeneralPath swatch = new GeneralPath();
	swatch.moveTo(32.37f, 92.37f);
	swatch.curveTo(24.12f, 84.12f, 15.87f, 75.87f, 7.62f, 67.62f);
	swatch.curveTo(10.93f, 64.32f, 14.69f, 61.41f, 18.88f, 58.88f);
	swatch.curveTo(13.46f, 53.46f, 8.04f, 48.04f, 2.62f, 42.62f);
	swatch.curveTo(5.93f, 39.32f, 9.69f, 36.41f, 13.88f, 33.88f);
	swatch.curveTo(8.46f, 28.46f, 3.04f, 23.04f, -2.37f, 17.62f);
	swatch.curveTo(6.80f, 8.47f, 19.36f, 2.29f, 35.30f, -0.90f);
	swatch.curveTo(43.93f, -2.63f, 53.64f, -3.49f, 64.43f, -3.49f);
	swatch.curveTo(71.37f, -3.49f, 78.75f, -3.13f, 86.58f, -2.42f);
	swatch.curveTo(85.84f, 5.71f, 85.10f, 13.86f, 84.36f, 22.00f);
	swatch.curveTo(86.72f, 22.16f, 89.13f, 22.34f, 91.58f, 22.57f);
	swatch.curveTo(90.84f, 30.71f, 90.10f, 38.86f, 89.36f, 47.00f);
	swatch.curveTo(91.72f, 47.16f, 94.13f, 47.34f, 96.58f, 47.57f);
	swatch.curveTo(95.52f, 59.19f, 94.47f, 70.80f, 93.41f, 82.42f);
	swatch.curveTo(86.57f, 81.80f, 80.21f, 81.49f, 74.35f, 81.49f);
	swatch.curveTo(65.95f, 81.49f, 58.57f, 82.13f, 52.19f, 83.40f);
	swatch.curveTo(43.14f, 85.19f, 36.54f, 88.18f, 32.37f, 92.37f);
	swatch.closePath();
	return swatch;
    }

    /** Return true if the outline of the given shape intersects with the
     *  given rectangle.
     */
    public static boolean intersectsOutline(Rectangle2D r, Shape s) {
        PathIterator i = s.getPathIterator(null, .01);
        double points[] = new double[6];
        double lastX = 0, lastY = 0;
        double firstX = 0, firstY = 0;
        while (!i.isDone()) {
            int type = i.currentSegment(points);
            if (type == PathIterator.SEG_MOVETO) {
                firstX = points[0];
                firstY = points[1];
            }
            else if (type == PathIterator.SEG_LINETO) {
                if (r.intersectsLine(lastX, lastY, points[0], points[1]))
                    return true;
            }
            else if (type == PathIterator.SEG_CLOSE) {
                if (r.intersectsLine(lastX, lastY, firstX, firstY))
                    return true;
            }
            lastX = points[0];
            lastY = points[1];
            i.next();
        }
        return false;
    }

    /** Return true if the given transform maps a rectangle
     * to a rectangle. If this method returns true, then the
     * transformRectangle and transformRectangularShape methods
     * will operate correctly.
     */
    public static boolean isOrthogonal(AffineTransform at) {
        int t = at.getType();
        return (t &
                (AffineTransform.TYPE_MASK_ROTATION
		 | AffineTransform.TYPE_GENERAL_TRANSFORM)) == 0;
    }

    /** Print a Shape to a String, as a code fragment that creates
     * a new GeneralPath.
     */
    public static String printShapeAsCode(String name, Shape shape) {
        StringBuffer s = new StringBuffer();
        PathIterator p = shape.getPathIterator(null);
        float data[] = new float[6];

        s.append("GeneralPath " + name + " = new GeneralPath();\n");
        while (!p.isDone()) {
            int type = p.currentSegment(data);
            switch (type) {
            case PathIterator.SEG_CLOSE:
                s.append(name + ".closePath();");
                break;
            case PathIterator.SEG_MOVETO:
                s.append(name + ".moveTo(");
                s.append(data[0] + "f, ");
                s.append(data[1] + "f);\n");
                break;
            case PathIterator.SEG_LINETO:
                s.append(name + ".lineTo(");
                s.append(data[0] + "f, ");
                s.append(data[1] + "f);\n");
                break;
            case PathIterator.SEG_QUADTO:
                s.append(name + ".quadTo(");
                s.append(data[0] + "f, ");
                s.append(data[1] + "f, ");
                s.append(data[2] + "f, ");
                s.append(data[3] + "f);\n");
                break;
            case PathIterator.SEG_CUBICTO:
                s.append(name + ".curveTo(");
                s.append(data[0] + "f, ");
                s.append(data[1] + "f, ");
                s.append(data[2] + "f, ");
                s.append(data[3] + "f, ");
                s.append(data[4] + "f, ");
                s.append(data[5] + "f);\n");
                break;
            }
            p.next();
        }
        s.append(name + ".closePath();");
        return s.toString();
    }

    /** Given a bounding-box rectangle, return a new rectangle
     * by transforming the argument rectangle and taking the bounding
     * box of the result. This method optimizes the calculation if the
     * transform is orthogonal. Note that the argument rectangle is
     * <i>not</i> modified, and the transform does not need to be
     * orthogonal.
     */
    public static Rectangle2D transformBounds(Rectangle2D rect, AffineTransform at) {
        if (!isOrthogonal(at)) {
            return at.createTransformedShape(rect).getBounds2D();

        }
        else if (rect instanceof Rectangle2D.Double) {
            Rectangle2D.Double r = (Rectangle2D.Double) rect;
            double coords[] = new double[4];
            coords[0] = r.x;
            coords[1] = r.y;
            coords[2] = r.x + r.width;
            coords[3] = r.y + r.height;
            at.transform(coords, 0, coords, 0, 2);
            return new Rectangle2D.Double(
					  coords[0],
					  coords[1],
					  coords[2] - coords[0],
					  coords[3] - coords[1]);

        }
        else if (rect instanceof Rectangle2D.Float) {
            Rectangle2D.Float r = (Rectangle2D.Float) rect;
            float coords[] = new float[4];
            coords[0] = r.x;
            coords[1] = r.y;
            coords[2] = r.x + r.width;
            coords[3] = r.y + r.height;
            at.transform(coords, 0, coords, 0, 2);
            return new Rectangle2D.Double(
					  coords[0],
					  coords[1],
					  coords[2] - coords[0],
					  coords[3] - coords[1]);

        }
        else {
            double coords[] = new double[4];
            coords[0] = rect.getX();
            coords[1] = rect.getY();
            coords[2] = coords[0] + rect.getWidth();
            coords[3] = coords[1] + rect.getHeight();
            at.transform(coords, 0, coords, 0, 2);
            return new Rectangle2D.Double(
					  coords[0], coords[1], coords[2], coords[3]);
        }
    }

    /** Transform a rectangular shape with an orthogonal transform.
     * The passed rectangular shape will be modified according to the
     * transform.  By "orthogonal", it is meant any transform that
     * returns true to to the isOrthogonal() method.
     */
    public static void transformModifyRect(
					   RectangularShape s, AffineTransform at) {
        if (s instanceof Rectangle2D.Float) {
            Rectangle2D.Float r = (Rectangle2D.Float) s;
            float coords[] = new float[4];
            coords[0] = r.x;
            coords[1] = r.y;
            coords[2] = r.x + r.width;
            coords[3] = r.y + r.height;
            at.transform(coords, 0, coords, 0, 2);
            r.x = Math.min(coords[0], coords[2]);
            r.y = Math.min(coords[1], coords[3]);
            r.width = Math.abs(coords[2] - coords[0]);
            r.height = Math.abs(coords[3] - coords[1]);

        }
        else if (s instanceof Rectangle2D.Double) {
            Rectangle2D.Double r = (Rectangle2D.Double) s;
            double coords[] = new double[4];
            coords[0] = r.x;
            coords[1] = r.y;
            coords[2] = r.x + r.width;
            coords[3] = r.y + r.height;
            at.transform(coords, 0, coords, 0, 2);
            r.x = Math.min(coords[0], coords[2]);
            r.y = Math.min(coords[1], coords[3]);
            r.width = Math.abs(coords[2] - coords[0]);
            r.height = Math.abs(coords[3] - coords[1]);

        }
        else {
            double coords[] = new double[4];
            coords[0] = s.getX();
            coords[1] = s.getY();
            coords[2] = coords[0] + s.getWidth();
            coords[3] = coords[1] + s.getHeight();
            at.transform(coords, 0, coords, 0, 2);
            s.setFrameFromDiagonal(coords[0], coords[1], coords[2], coords[3]);
        }
    }

    /** Transform a shape with the supplied transform. If possible,
     * this method modifies the shape directly and returns a pointer
     * to that same shape. In particular, instances of Rectangle2D,
     * Polygon2D, Polyline2D, and GeneralPath will be modified
     * directly.  (In the case of RectangularShape, only if the
     * transform is orthogonal.)  Otherwise, a general transformation
     * is used that creates and returns a new instance of GeneralPath.
     */
    public static Shape transformModify(Shape s, AffineTransform at) {
        if (s instanceof RectangularShape && isOrthogonal(at)) {
            transformModifyRect((RectangularShape) s, at);
            return s;

        }
        else if (s instanceof Polygon2D) {
            ((Polygon2D) s).transform(at);
            return s;

        }
        else if (s instanceof Polyline2D) {
            ((Polyline2D) s).transform(at);
            return s;

        }
        else if (s instanceof GeneralPath) {
            ((GeneralPath) s).transform(at);
            return s;

        }
        else {
            return at.createTransformedShape(s);
        }
    }

    /** Translate a shape the given distance.  If possible, this
     * method modifies the shape directly and returns a pointer to
     * that same shape. In particular, instances of RectangularShape,
     * Polygon2D, Polyline2D, and GeneralPath are modified directly.
     * Otherwise, a general transformation is used that creates and
     * returns a new instance of GeneralPath.
     */
    public static Shape translateModify(Shape s, double x, double y) {
        if (s instanceof RectangularShape) {
            RectangularShape r = (RectangularShape) s;
            r.setFrame(x + r.getX(), y + r.getY(), r.getWidth(), r.getHeight());
            return r;

        }
        else if (s instanceof Polygon2D) {
            ((Polygon2D) s).translate(x, y);
            return s;

        }
        else if (s instanceof Polyline2D) {
            ((Polyline2D) s).translate(x, y);
            return s;

        }
        else if (s instanceof GeneralPath) {
            AffineTransform at = AffineTransform.getTranslateInstance(x, y);
            ((GeneralPath) s).transform(at);
            return s;

        }
        else {
            AffineTransform at1 = AffineTransform.getTranslateInstance(x, y);
            return (at1.createTransformedShape(s));
        }
    }
}

