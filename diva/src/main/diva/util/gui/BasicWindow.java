/*
 * $Id: BasicWindow.java,v 1.4 2000/05/02 00:45:27 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * BasicWindow is a JFrame that is used to display examples.
 * It contains a menubar with a quit entry and a few other
 * useful bits. Unlike BasicWindow, closing the window
 * calls dispose on the window but does not call System.exit().
 *
 * @author John Reekie (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @deprecated Use diva.gui.BasicFrame instead.
 */
public class BasicWindow extends JFrame {
    // My menubar
    transient JMenuBar menubar = null;

    // Constructor -- create the Frame and give it a title.
    public BasicWindow(String title) {
        super(title);

        // Create the menubar and set it
        setJMenuBar(createMenuBar());

        // Close the window on any window event
        addWindowListener(windowListener);
    }

    /** Create the menubar
     */
    public JMenuBar createMenuBar () {
        JMenuBar menubar;
        JMenu menuFile;
        JMenuItem itemClose;

        // Create the menubar and menus
        menubar = new JMenuBar();

        menuFile = new JMenu("File");
        menuFile.setMnemonic('F');

        // Create the menu items
        itemClose = menuFile.add(actionClose);
        itemClose.setMnemonic('C');
        itemClose.setToolTipText("Close this window");

        // Build the menus
        menubar.add(menuFile);

        return menubar;
    }

    /////////////////////////////////////////////////////////////////
    // The action classes
    //
    private transient Action actionClose = new AbstractAction ("Close") {
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    };

    /////////////////////////////////////////////////////////////////
    // The window listener
    //
    transient WindowListener windowListener = new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
            actionClose.actionPerformed(null);
        }
        public void windowIconified (WindowEvent e) {
            System.out.println(e);
        }
        public void windowDeiconified (WindowEvent e) {
            System.out.println(e);
        }
    };
}

