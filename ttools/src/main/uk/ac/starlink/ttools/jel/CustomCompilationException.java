package uk.ac.starlink.ttools.jel;

import gnu.jel.CompilationException;

/**
 * Specialised subclass of JEL's CompilationException.
 * This is provided so that new CompilationExceptions can be generated by 
 * code other than that in the JEL librarry - the constructors for the
 * superclass are rather opaque.
 *
 * @author   Mark Taylor
 * @since    1 Nov 2008
 */
public class CustomCompilationException extends CompilationException {

    private final String msg_;
    private final CompilationException cause_;

    /**
     * Constructor.
     *
     * @param   msg   message
     * @param   base    existing compilation exception on which this is based
     */
    public CustomCompilationException( String msg, CompilationException base ) {
        super( base.getType(), base.getParameters() );
        msg_ = msg;
        cause_ = base;
    }

    /**
     * Constructor.
     *
     * @param   msg  message
     */
    public CustomCompilationException( String msg ) {
        super( 1, new Object[ 0 ] );
        msg_ = msg;
        cause_ = null;
    }

    public String getMessage() {
        return msg_;
    }

    public Throwable getCause() {
        return cause_;
    }
}
