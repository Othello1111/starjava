/*
 * $Id: ModelWriter.java,v 1.1 2000/05/10 22:02:15 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;

import java.io.IOException;
import java.io.Writer;

/**
 * ModelWriter is an interface that should be extended by application
 * specified model writers to write out data structures to an output
 * stream.  For example, a SketchWriter extends this interface and
 * writes out a sketch model to a stream.
 *
 * @author Heloise Hse  (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 */
public interface ModelWriter {

    /**
     * Write the given model to the character stream.
     */
    public void writeModel(Object model, Writer writer) throws IOException;
}






