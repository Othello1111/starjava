/*
 * $Id: RelativeBundle.java,v 1.3 2000/09/08 21:06:00 neuendor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.resource;

import java.awt.Toolkit;
import java.awt.Image;

import java.io.InputStream;
import java.net.URL;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;

/**
 * A class for managing resources. This class is an extension to the
 * standard ResourceBundle that allows you to construct
 * ResourceBundles that
 * 
 * <ul>
 * <li> are relative to a specified class -- this means that you can
 * let the class-loaders find the resource for you.
 * <li> "override" other resource bundles. Thus, an abstract application
 * could have a set of default resources, and a particular application
 * could add its own resources to override the defaults where appropriate.
 * </ul>
 * 
 * @author John Reekie (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 */
public class RelativeBundle extends ResourceBundle {
    // FIXME If the parent is another RelativeBundle, it may have a different
    // loader argument.  This doesn't work properly because that loader
    // argument is ignored.
    /** The class that is to be used to look up URL resources
     * from this bundle.
     */
    private Class _loader;

    /** Icons that have already been loaded
     */
    private HashMap _imageIcons = new HashMap();

    /** The actual ResourceBundle
     */
    private ResourceBundle _delegate;

    /** Create a new RelativeBundle using the given basename, with the
     * given class as the loader for URL-based resources, and with the
     * given ResourceBundle as the one that gets overridden. The loader
     * must be such that the resource paths in the properties file
     * ("baseName.properties") are relative to the package containing
     * that class. 
     * 
     * For example, you could create a default Bundle and override
     * it as follows:
     * <pre>
     *   RelativeBundle default = new diva.resource.Default();
     *   RelativeBundle resource = new RelativeBundle(
     *       "mypackage.resource.MyResources", getClass(), default);
     * <pre>
     *
     * In this example, the file mypackage/resource/MyResource.properties
     * would contain resource such as
     * <pre>
     *    LoadImage = resources/load.gif
     *    SaveImage = resources/save.gif
     * </pre>
     *
     * <P>If you don't mind cluttering up your source directory
     * with resource files, then it's probably better to put the
     * resource file in the same directory as the application classes,
     * so the properties files doesn't need the "resources/" strings.
     */
    public RelativeBundle (String baseName, Class loader,
            ResourceBundle overrides) {
        try {
            _delegate = ResourceBundle.getBundle(baseName, Locale.getDefault());
        } catch (MissingResourceException e) {
            System.err.println( baseName + ".properties not found");
            System.exit(1);
        }
        if (loader == null) {
            _loader = getClass();
        } else {
            _loader = loader;
        }
        if (overrides != null) {
            setParent(overrides);
        }
    }

    /** Get a resource as an absolute URL.
     */
    public URL getResource (String key) {
        String s = getString(key);
        return _loader.getResource(s);        
    }

    /** Get a resource as an input stream.
     */
    public InputStream getResourceAsStream (String key) {
        String s = getString(key);
        return _loader.getResourceAsStream(s);        
    }

    /** Get a resource as an image icon. Return null if not found.
     * (Or should this throw an exception?)
     */
    public ImageIcon getImageIcon (String key) {
        ImageIcon icon = (ImageIcon)_imageIcons.get(key);
        if (icon == null) {
            URL url = getResource(key);
            if (url != null) {
                icon = new ImageIcon(url);
                _imageIcons.put(key, icon);
            }
        }
        return icon;
    }

    /** Get a resource as an image. Return null if not found.
     * (Or should this throw an exception?)
     */
    public Image getImage (String key) {
        URL url = getResource(key);
        
        if (url != null) {
            Toolkit tk = Toolkit.getDefaultToolkit();
            Image img = tk.getImage(url);
            return img;
        }
        return null;
    }
    
    /** Get an object from a ResourceBundle.
     */
    protected Object handleGetObject(String key)
            throws MissingResourceException {
	try {
	    return _delegate.getObject(key);
	} catch (MissingResourceException ex) {
	    return null;
	}
    }

    /** Get an enumeration over the keys
     */
    public Enumeration getKeys () {
        return _delegate.getKeys();
    }
}

