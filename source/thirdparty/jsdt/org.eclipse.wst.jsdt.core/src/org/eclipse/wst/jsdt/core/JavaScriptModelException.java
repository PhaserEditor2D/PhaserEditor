/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.jsdt.internal.core.JavaModelStatus;

/**
 * A checked exception representing a failure in the JavaScript model.
 * JavaScript model exceptions contain a JavaScript-specific status object describing the
 * cause of the exception.
 * <p>
 * This class is not intended to be subclassed by clients. Instances of this
 * class are automatically created by the JavaScript model when problems arise, so
 * there is generally no need for clients to create instances.
 * </p>
 *
 * @see IJavaScriptModelStatus
 * @see IJavaScriptModelStatusConstants
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class JavaScriptModelException extends CoreException {

	private static final long serialVersionUID = -760398656505871287L; // backward compatible

	CoreException nestedCoreException;
/**
 * Creates a JavaScript model exception that wrappers the given <code>Throwable</code>.
 * The exception contains a JavaScript-specific status object with severity
 * <code>IStatus.ERROR</code> and the given status code.
 *
 * @param e the <code>Throwable</code>
 * @param code one of the JavaScript-specific status codes declared in
 *   <code>IJavaScriptModelStatusConstants</code>
 * @see IJavaScriptModelStatusConstants
 * @see org.eclipse.core.runtime.IStatus#ERROR
 */
public JavaScriptModelException(Throwable e, int code) {
	this(new JavaModelStatus(code, e));
}
/**
 * Creates a JavaScript model exception for the given <code>CoreException</code>.
 * Equivalent to
 * <code>JavaScriptModelException(exception,IJavaScriptModelStatusConstants.CORE_EXCEPTION</code>.
 *
 * @param exception the <code>CoreException</code>
 */
public JavaScriptModelException(CoreException exception) {
	super(exception.getStatus());
	this.nestedCoreException = exception;
}
/**
 * Creates a JavaScript model exception for the given JavaScript-specific status object.
 *
 * @param status the JavaScript-specific status object
 */
public JavaScriptModelException(IJavaScriptModelStatus status) {
	super(status);
}
/**
 * Returns the underlying <code>Throwable</code> that caused the failure.
 *
 * @return the wrappered <code>Throwable</code>, or <code>null</code> if the
 *   direct case of the failure was at the JavaScript model layer
 */
public Throwable getException() {
	if (this.nestedCoreException == null) {
		return getStatus().getException();
	} else {
		return this.nestedCoreException;
	}
}
/**
 * Returns the JavaScript model status object for this exception.
 * Equivalent to <code>(IJavaScriptModelStatus) getStatus()</code>.
 *
 * @return a status object
 */
public IJavaScriptModelStatus getJavaScriptModelStatus() {
	IStatus status = this.getStatus();
	if (status instanceof IJavaScriptModelStatus) {
		return (IJavaScriptModelStatus)status;
	} else {
		// A regular IStatus is created only in the case of a CoreException.
		// See bug 13492 Should handle JavaModelExceptions that contains CoreException more gracefully
		return new JavaModelStatus(this.nestedCoreException);
	}
}
/**
 * Returns whether this exception indicates that a JavaScript model element does not
 * exist. Such exceptions have a status with a code of
 * <code>IJavaScriptModelStatusConstants.ELEMENT_DOES_NOT_EXIST</code> or
 * <code>IJavaScriptModelStatusConstants.ELEMENT_NOT_ON_CLASSPATH</code>.
 * This is a convenience method.
 *
 * @return <code>true</code> if this exception indicates that a JavaScript model
 *   element does not exist
 * @see IJavaScriptModelStatus#isDoesNotExist()
 * @see IJavaScriptModelStatusConstants#ELEMENT_DOES_NOT_EXIST
 * @see IJavaScriptModelStatusConstants#ELEMENT_NOT_ON_CLASSPATH
 */
public boolean isDoesNotExist() {
	IJavaScriptModelStatus javaModelStatus = getJavaScriptModelStatus();
	return javaModelStatus != null && javaModelStatus.isDoesNotExist();
}

/**
 * Prints this exception's stack trace to the given print stream.
 *
 * @param output the print stream
 */
public void printStackTrace(PrintStream output) {
	synchronized(output) {
		super.printStackTrace(output);
		Throwable throwable = getException();
		if (throwable != null) {
			output.print("Caused by: "); //$NON-NLS-1$
			throwable.printStackTrace(output);
		}
	}
}

/**
 * Prints this exception's stack trace to the given print writer.
 *
 * @param output the print writer
 */
public void printStackTrace(PrintWriter output) {
	synchronized(output) {
		super.printStackTrace(output);
		Throwable throwable = getException();
		if (throwable != null) {
			output.print("Caused by: "); //$NON-NLS-1$
			throwable.printStackTrace(output);
		}
	}
}
/*
 * Returns a printable representation of this exception suitable for debugging
 * purposes only.
 */
public String toString() {
	StringBuffer buffer= new StringBuffer();
	buffer.append("JavaScript Model Exception: "); //$NON-NLS-1$
	if (getException() != null) {
		if (getException() instanceof CoreException) {
			CoreException c= (CoreException)getException();
			buffer.append("Core Exception [code "); //$NON-NLS-1$
			buffer.append(c.getStatus().getCode());
			buffer.append("] "); //$NON-NLS-1$
			buffer.append(c.getStatus().getMessage());
		} else {
			buffer.append(getException().toString());
		}
	} else {
		buffer.append(getStatus().toString());
	}
	return buffer.toString();
}
}
