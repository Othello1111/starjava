#!/usr/bin/perl -w

#+
#  Name:
#     AstException.pl

#  Purpose:
#     Write java/C source for identifying error status codes.

#  Usage:
#     AstException.pl type-flag name messgenfile

#  Description:
#     This script writes either the java source code for an Exception
#     class, or the C source code for a corresponding function.
#     Together these allow error codes to be accessed by name from 
#     Java source code.

#  Arguments:
#     type-flag
#        Must be either '-c' to write the C source code of a function
#        called 'name' or '-java' to write the java source code of
#        a class called 'name'.
#     name
#        The name of the C function or java class.
#     messgenfile
#        The messgen input file for the error facility in question.

#  Author:
#     MBT: Mark Taylor (Starlink)

#  History:
#     25-SEP-2001 (MBT):
#        Original version.
#-

use strict;

#  Constants.
my( $PackageName ) = "uk.ac.starlink.ast";
my( $ClassName ) = "AstException";
my( $MethodName ) = "getErrConst";
my( $ErrHeader ) = "ast.h";

#  Set usage string.
my( $self ) = $0;
$self =~ s%.*/%%;
my( $usage ) = "Usage: $self (-c|-java) messgenfile\n";

#  Validate arguments.
if ( @ARGV != 2 ) {
   die( $usage );
}
my( $Typeflag, $Messgenfile ) = @ARGV;
if ( $Typeflag ne "-c" && $Typeflag ne "-java" ) {
   die( $usage );
}

if ( $Typeflag eq "-c" ) {
   my( $phname ) = $PackageName;
   $phname =~ s/\./_/g;
   my( $funcname ) = "Java_${phname}_${ClassName}_${MethodName}";
   print <<__EOT__;
#include <stdlib.h>
#include <string.h>
#include "jni.h"
#include "$ErrHeader"
#include "sae_par.h"
#include "${phname}_${ClassName}.h"

#define TRY_CONST(Xident) \\
   if ( strcmp( #Xident, ident ) == 0 ) { \\
      result = (jint) Xident; \\
      success = 1; \\
   }

JNIEXPORT jint JNICALL $funcname(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* The class */
   jstring jIdent        /* Name identifying the error constant */
) {
   jint result = (jint) SAI__OK;
   int success = 0;
   const char *ident = (*env)->GetStringUTFChars( env, jIdent, NULL );
   if ( ident != NULL ) {
      TRY_CONST(SAI__OK) 
      else TRY_CONST(SAI__ERROR)
__EOT__

}
else {
   print <<__EOT__;

package uk.ac.starlink.ast;

/**
 * Thrown to indicate that there has been an AST error of some description.
 * If a call to the underlying AST library occurs results in a non-zero
 * status value, an AstException is thrown.  By calling <code>getStatus</code>
 * on this exception and comparing it with one of the 
 * <code>static final int</code> fields (constants) defined by this class, 
 * it is possible to find out exactly what went wrong.
 * In a few cases an AstException may be thrown by parts of
 * the Java uk.ac.starlink.ast system not provided by the underlying AST 
 * library - in this case the status will be equal to <code>SAI__ERROR</code>.
 *
 * \@author  Mark Taylor (Starlink)
 * \@version \$Id\$
 */
public class $ClassName extends RuntimeException {
    private int status;

    /**
     * Construct an $ClassName.
     *
     * \@param  message  an explanatory message
     */
    public $ClassName( String message ) {
        this( message, SAI__ERROR );
    }

    /**
     * Construct an $ClassName with a given status value.
     *
     * \@param  message  an explanatory message
     * \@param  status   the numerical status value
     */
    public AstException( String message, int status ) {
        super( message );
        this.status = status;
    }

    /**
     * Get the status value corresponding to the exception.  This ought
     * to correspond to one of this class's static final member fields.
     *
     * \@return  the error status value
     */
    public int getStatus() {
        return status;
    }

    private native static int $MethodName( String ident );

    /**
     * Status constant for no error.  
     * This value should never be used as the status of an AstException.
     */
    public static final int SAI__OK = ${MethodName}( "SAI__OK" );

    /**
     * Status constant for unknown error. 
     * This value may be used as the status of an AstException where no
     * value defined in the underlying library is appropriate.
     */
    public static final int SAI__ERROR = ${MethodName}( "SAI__ERROR" );

__EOT__
}

#  Open the messgen file.
open( MESSGEN, $Messgenfile ) or die( "Failed to open file $Messgenfile\n" );

#  Get the facility name.
my( $line );
$line = <MESSGEN>;
$line =~ /FACILITY *(\S*)/;
my( $fac ) = $1;

#  Read the lines corresponding to error codes.
while ( <MESSGEN> ) {
   chomp;
   my( $num, $label, $text ) = split( /,/, $_ );
   if ( $Typeflag eq "-c" ) {
      print( "      else TRY_CONST(${fac}__${label})\n" );
   }
   else {
      print( "   /** Status constant for error \"$text\" */\n" );
      print( "   public static final int ${fac}__${label} = " .
             "${MethodName}( \"${fac}__${label}\" );\n" );
   }
}

#  Write footers.
if ( $Typeflag eq "-c" ) {
   print <<__EOT__;
   }
   if ( ! success ) printf( "no such constant %s\\n", ident );
   (*env)->ReleaseStringUTFChars( env, jIdent, ident );
   if ( ! success ) {
      (*env)->ThrowNew( env, (*env)->FindClass( env, "java/lang/Error" ),
                        "No such constant" );
   }
   return result;
}
__EOT__

}
else {
   print( "}\n" );
}


# $Id$
