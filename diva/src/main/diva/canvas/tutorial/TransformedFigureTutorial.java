/*
 * $Id: TransformedFigureTutorial.java,v 1.7 2000/05/22 17:07:25 neuendor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas.tutorial;

import diva.canvas.AbstractFigure;
import diva.canvas.CanvasPane;
import diva.canvas.CanvasUtilities;
import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.JCanvas;
import diva.canvas.TransformContext;

import diva.canvas.event.MouseFilter;

import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.DragInteractor;

import diva.canvas.toolbox.BasicFigure;
import diva.canvas.toolbox.BasicEllipse;
import diva.canvas.toolbox.BasicRectangle;

import diva.gui.BasicFrame;

import diva.util.java2d.ShapeUtilities;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;


/** An example showing how to make custom figures that contain
 * their own AffineTransform. In the FigureTutorial class, we
 * showed how to make a custom figure, and how to transform the
 * various 2D shapes in the paint() method. Here, we will use
 * an AffineTransform to do the same thing. This technique is
 * a little more work to figure out how to do, but it probably
 * better if your figure has more than a couple of Shapes in it.
 *
 * <p> Transforms are a little tricky to get right, so the Diva
 * Canvas provides a class, TransformContext, that you need to use
 * to give a figure its own transform. Each instance of TransformContext
 * contains a single AffineTransform, and a bunch of methods that
 * deal with it. See the class documentation for
 * TransformedFigureTutorial.CloudFigure for more details.
 *
 * @author John Reekie
 * @version $Revision: 1.7 $
 */
public class TransformedFigureTutorial {

    // The JCanvas
    private JCanvas canvas;

    // The GraphicsPane
    private GraphicsPane graphicsPane;

    /** Create a JCanvas and put it into a window.
     */
    public TransformedFigureTutorial () {
      canvas = new JCanvas();
      graphicsPane = (GraphicsPane)canvas.getCanvasPane();

      BasicFrame frame = new BasicFrame("Figure tutorial", canvas);
      frame.setSize(600,400);
      frame.setVisible(true);
    }

    /** Create instances of the class defined
     * in this file. To make the demo a little more interesting,
     * make them draggable.
     */
    public void createFigures () {
        FigureLayer layer = graphicsPane.getForegroundLayer();

        // Create the interaction role and an interactor to do the work.
        Interactor dragger = new DragInteractor();
      
        // Create the figure
        Figure one = new CloudFigure(10.0,10.0,80.0,80.0);
        layer.add(one);
        one.setInteractor(dragger);

        Figure two = new CloudFigure(150, 150, 200, 180);
        layer.add(two);
        two.setInteractor(dragger);
    }

    /** Main function
     */
    public static void main (String argv[]) {
        TransformedFigureTutorial ex = new TransformedFigureTutorial();
        ex.createFigures();
    }

    //////////////////////////////////////////////////////////////////////
    //// CloudFigure

    /** CloudFigure is a class that paints itself as a
     * translucent "cloud."
     * This example figure class illustrates the use of different
     * paints and strokes to create the required image, and the use
     * of TransformContext to position that image.
     */
    public class CloudFigure extends AbstractFigure {
        // The cloud shape
        private Shape _shape;

        // Little cloud 1
        private Shape _cloud1;

        // Little cloud 2
        private Shape _cloud2;

        // The transform
        private TransformContext _transformContext;

        // The cached bounding box
        private Rectangle2D _cachedBounds = null;

        // The cached shape, in the external transform context
        private Shape _cachedShape = null;

        /** Create a new instance of this figure. The cloud is initially
         * created at coordinates (0,0) and then transformed to the requested
         * coordinates. 
         * To create the cloud shape, use the Area class in Java2D, which
         * implements constructive area geometry, and join a bunch
         * of circles into a single shape.
         */
        public CloudFigure (
                double x, double y,
                double width, double height ) {

            // Create the transform context and initialize it
            // so that the figure is drawn at the requested coordinates
            _transformContext = new TransformContext(this);
            AffineTransform at = _transformContext.getTransform();
            at.translate(x,y);
            at.scale(width/100, height/100);
            _transformContext.invalidateCache();

            // Create the shape we will use to draw the figure
            //              Area area = new Area();
            //              Ellipse2D c = new Ellipse2D.Double();
            //              c.setFrame(0,25,50,50);
            //              area.add(new Area(c));
            //              c.setFrame(25,0,40,40);
            //              area.add(new Area(c));
            //              c.setFrame(25,25,60,60);
            //              area.add(new Area(c));
            //              c.setFrame(60,30,40,40);
            //              area.add(new Area(c));
            //              c.setFrame(60,10,30,30);
            //              area.add(new Area(c));
            //            _shape = area;
            _shape = ShapeUtilities.createSwatchShape();

            // Create the shapes for the little clouds. This could
            // also be done in the paint() method, but since it's
            // rather slow, we do it one in the constructor.
            // Watch it -- don't modify the main transform!
            Shape c = ShapeUtilities.createCloudShape();
            at = new AffineTransform();
            at.setToTranslation(20,20);
            at.scale(0.25,0.25);
            _cloud1 = at.createTransformedShape(c);
           
            at.setToTranslation(50,40);
            at.scale(0.4,0.4);
            _cloud2 = at.createTransformedShape(c);
        }

        /** Get the bounds of this figure. Because this figure has
         * its own transform, we need to transform the internal bounds
         * into the enclosing context. To make this more efficient,
         * we use a previously-cached copy of the transformed bounds
         * if there is one.
         */
        public Rectangle2D getBounds () {
            if (_cachedBounds == null) {
                _cachedBounds = getShape().getBounds2D();
            }
            return _cachedBounds;
        }

        /** Get the shape of this figure. Because this figure has
         * its own transform, we need to transform the internal shape
         * into the enclosing context. To make this more efficient,
         * we use a previously-cached copy of the transformed shape
         * if there is one.
         */
        public Shape getShape () {
            if (_cachedShape == null) {
                AffineTransform at = _transformContext.getTransform();
                _cachedShape = at.createTransformedShape(_shape);
            }
            return _cachedShape;
        }

        /** Get the transform context. This method must be overridden
         * since this figure defined its own context.
         */
        public TransformContext getTransformContext () {
            return _transformContext;
        }

        /** Paint this figure onto the given graphics context.
         * First we "push" the transform context onto the transform
         * stack, so that the graphics port has the correct transform.
         * Then we paint the cloud a translucent magenta (yum!),
         * and then we make a couple of little clouds and paint them
         * opaque. (The way this is done in this example is horrendously
         * inefficient.) Finally, we "pop" the transform context off
         * the stack.
         */
        public void paint (Graphics2D g) {
            // Push the context
            _transformContext.push(g);

            // Paint the big cloud
            AlphaComposite c = AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER,0.3f);
            g.setComposite(c);
            g.setPaint(Color.magenta);
            g.fill(_shape);

            // Paint the little clouds
            g.setComposite(AlphaComposite.SrcOver);
            g.setPaint(Color.red);
            g.fill(_cloud1);
           
            g.setPaint(Color.green);
            g.fill(_cloud2);

            // Pop the context
            _transformContext.pop(g);
         }

        /** Transform the object.
         * In this example, we pre-concatenate the given transform with
         * the transform in the transform context. When the figure is
         * repainted, it will be redrawn in the right place.
         * We also must be sure to invalidate the cached
         * geometry objects that depend on the transform.
         */
        public void transform (AffineTransform at) {
            repaint();
            _cachedShape = null;
            _cachedBounds = null;
            _transformContext.preConcatenate(at);
            repaint();
        }
    }
}

