/*
 * $Id: ConnectorManipulator.java,v 1.11 2000/05/02 00:43:23 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.canvas.connector;

import diva.canvas.Site;
import diva.canvas.Figure;
import diva.canvas.FigureContainer;
import diva.canvas.FigureDecorator;

import diva.canvas.event.LayerEvent;
import diva.canvas.event.LayerMotionListener;
import diva.canvas.event.LayerEventMulticaster;
import diva.canvas.event.LayerListener;

import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.DragInteractor;
import diva.canvas.interactor.Manipulator;
import diva.canvas.interactor.GrabHandle;
import diva.canvas.interactor.GrabHandleFactory;
import diva.canvas.interactor.BasicGrabHandleFactory;

import diva.canvas.toolbox.BasicHighlighter;

/**
 * A manipulator which attaches grab handles to the ends of
 * a connector. The role given to the grab-handles determines
 * the behaviour of the grab-handles.
 *
 * @author John Reekie      (johnr@eecs.berkeley.edu)
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.11 $
 */
public class ConnectorManipulator extends Manipulator {

    /** Layer motion listeners
     */
    LayerMotionListener _layerMotionListener;

    /** The thing that we use to find sites
     */
    ConnectorTarget _connectorTarget;

    /** The snap halo
     */
    private double _snapHalo = 2.0;

    /** The highlighter listener
     */
    LayerMotionListener _targetHighlighter;

    /** The property name that is tested to see if a
     * figure is a possible drop target.
     */
    String _targetProperty;

    /** The value of the property that is tested to see if a
     * figure is a possible drop target.
     */
    String _targetPropertyValue;

    /** The handle at the connector head
     */
    private GrabHandle _headHandle;

    /** The handle at the connector tail
     */
    private GrabHandle _tailHandle;

    /**
     * Construct a new manipulator that uses rectangular grab-handles.
     */
    public ConnectorManipulator() {
        this(new BasicGrabHandleFactory());
    }

    /**
     * Construct a new manipulator using the given grab-handle factory. 
     */
    public ConnectorManipulator(GrabHandleFactory f) {
        setGrabHandleFactory(f);
        setHandleInteractor(new ConnectorInteractor(this));
        setTargetHighlighter(new TargetHighlighter());
    }

    /** Add a connector listener to the interactor that
     * is attached to grab-handles.
     */
    public void addConnectorListener (ConnectorListener l) {
        ((ConnectorInteractor) getHandleInteractor()).addConnectorListener(l);
    }

    /** Add the given layer motion listener to this interactor.
     * The listener will be invoked when the mouse moves
     * into, around on, or out of a suitable target object.
     */
    public void addLayerMotionListener(LayerMotionListener l) {
        _layerMotionListener =
            LayerEventMulticaster.add(_layerMotionListener,l);
    }
   
    /** Get the connector target
     */
    public ConnectorTarget getConnectorTarget () {
        return _connectorTarget;
    }
 
    /** Get the head handle
     */
    public GrabHandle getHeadHandle () {
        return _headHandle;
    }
 
    /** Get the tail handle
     */
    public GrabHandle getTailHandle () {
        return _tailHandle;
    }
 
    /** Get the snap halo.
     */
    public double getSnapHalo () {
        return _snapHalo;
    }

    /** Get the listener that highlights target objects.
     */
    public LayerMotionListener getTargetHighlighter () {
        return _targetHighlighter;
    }
 
    /** Create a new instance of this manipulator. The new
     * instance will have the same grab handle, and interaction role
     * for grab-handles, as this one.
     */
    public FigureDecorator newInstance (Figure f) {
        ConnectorManipulator m = new ConnectorManipulator();
        m.setGrabHandleFactory(this.getGrabHandleFactory());
        m.setHandleInteractor(this.getHandleInteractor());
        return m;
    }

    /**
     * Remove a connector listener from the interactor that
     * is attached to grab-handles.
     */
    public void removeConnectorListener (ConnectorListener l) {
        ((ConnectorInteractor) getHandleInteractor()).removeConnectorListener(l);
    }

    /** Remove the given layer motion listener from this interactor.
     */
    public void removeLayerMotionListener(LayerMotionListener l) {
        _layerMotionListener =
            LayerEventMulticaster.remove(_layerMotionListener, l);
    }

    /** Refresh the geometry. Check that the sites that the handles
     * are attached to are the same as the sites at the ends of
     * the connector, and if not, fix them. This is needed because
     * some clients might unnecessarily over-write the sites that
     * a connector is already snapped to. If the grab-handles aren't
     * re-attached accordingly, grabbing and moving them will cause
     * unpredictable results.
     */
    public void refresh () {
        Connector c = (Connector) getChild();
        // Check sites
        if (c != null) {
            if (_headHandle.getSite() != c.getHeadSite()) {
                _headHandle.setSite(c.getHeadSite());
            }
            if (_tailHandle.getSite() != c.getTailSite()) {
                _tailHandle.setSite(c.getTailSite());
            }
        }
    }

    /** Set the child figure. If we have any grab-handles, lose them.
     * Then create the grab-handles on the ends of the connector.
     */
    public void setChild (Figure f) {
        if (f == null) {
            super.setChild(null);
            clearGrabHandles();
            return;
        }
        if (!(f instanceof Connector)) {
            throw new IllegalArgumentException(
                    "Connector required by ConnectorManipulator");
        }
        clearGrabHandles();
        Connector c = (Connector) f;

        // Create the grab handles and set them up
        GrabHandleFactory factory = getGrabHandleFactory();
        _headHandle = factory.createGrabHandle(c.getHeadSite());
        _tailHandle = factory.createGrabHandle(c.getTailSite());

        _headHandle.setParent(this);
        _tailHandle.setParent(this);

        _headHandle.setInteractor(getHandleInteractor());
        _tailHandle.setInteractor(getHandleInteractor());

        addGrabHandle(_headHandle);
        addGrabHandle(_tailHandle);

        // Finally call the superclass method, which will
        // make it repaint
        super.setChild(f);
    }

    /** Set the connector target object
     */
    public void setConnectorTarget (ConnectorTarget t) {
        _connectorTarget = t;
    }

    /** Set the snap halo. This is the distance from a target
     * object that the connector will "snap" to it.
     */
    public void setSnapHalo (double halo) {
        _snapHalo = halo;
    }

    /** Set the listener that highlights target objects.
     */
    public void setTargetHighlighter (LayerMotionListener l) {
        _targetHighlighter = l;
        /// FIXME doesn't work
        //// addLayerMotionListener(_targetHighlighter);
    }

    /** Set the drop target property and value. The interactor will
     * make callbacks to the layer motion listeners while the mouse is
     * over any figure which has an interaction role with matching
     * properties.
     */
    public void setTargetProperty (String key, String value) {
        _targetProperty = key;
        _targetPropertyValue = value;
    }

    ///////////////////////////////////////////////////////////////
    //// TargetHighlighter

    static class TargetHighlighter implements LayerMotionListener {
        BasicHighlighter _high;
        FigureContainer _parent;

        public void mouseEntered(LayerEvent e) {
            Figure f = e.getFigureSource();
            _parent = (FigureContainer)f.getParent();
            if(_parent != null) {
                _high = new BasicHighlighter(java.awt.Color.red, 2.0f);
                _parent.decorate(f,_high);
            }
        }
        public void mouseExited(LayerEvent e) {
            _parent.undecorate(_high);
            _high = null;
            _parent = null;
        }
        public void mouseMoved(LayerEvent e) {
            //XXX System.out.println(e);
        }
    }
}

