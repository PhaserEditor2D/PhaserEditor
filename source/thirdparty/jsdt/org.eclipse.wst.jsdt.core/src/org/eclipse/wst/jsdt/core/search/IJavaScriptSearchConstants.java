/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.search;

import org.eclipse.wst.jsdt.internal.core.search.processing.IJob;

/**
 * <p>
 * This interface defines the constants used by the search engine.
 * </p>
 * <p>
 * This interface declares constants only; it is not intended to be implemented.
 * </p>
 * @see org.eclipse.wst.jsdt.core.search.SearchEngine
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IJavaScriptSearchConstants {

	/**
	 * The nature of searched element or the nature
	 * of match in unknown.
	 */
	int UNKNOWN = -1;

	/* Nature of searched element */

	/**
	 * The searched element is a type, which may include classes, interfaces,
	 * enums, and annotation types.
	 */
	int TYPE= 0;

	/**
	 * The searched element is a method.
	 */
	int METHOD= 1;

	/**
	 * The searched element is a package.
	 */
	int PACKAGE= 2;

	/**
	 * The searched element is a constructor.
	 */
	int CONSTRUCTOR= 3;

	/**
	 * The searched element is a field.
	 */
	int FIELD= 4;

	/**
	 * The searched element is a class.
	 * More selective than using {@link #TYPE}.
	 */
	int CLASS= 5;
	
	/**
	 * The searched element is an enum.
	 * More selective than using {@link #TYPE}.
	 *  
	 */
	int ENUM= 7;

	/**
	 * The searched element is a field.
	 */
	int VAR= 12;
	int FUNCTION= 13;

	/* Nature of match */

	/**
	 * The search result is a declaration.
	 * Can be used in conjunction with any of the nature of searched elements
	 * so as to better narrow down the search.
	 */
	int DECLARATIONS= 0;

	/**
	 * The search result is a type that extends a class.
	 * Used in conjunction with either TYPE or CLASS, it will
	 * respectively search for any type extending a type,
	 * or rather exclusively search for classes extending the type.
	 */
	int IMPLEMENTORS= 1;

	/**
	 * The search result is a reference.
	 * Can be used in conjunction with any of the nature of searched elements
	 * so as to better narrow down the search.
	 * References can contain implementers since they are more generic kind
	 * of matches.
	 */
	int REFERENCES= 2;

	/**
	 * The search result is a declaration, a reference, or an implementer
	 * of an interface.
	 * Can be used in conjunction with any of the nature of searched elements
	 * so as to better narrow down the search.
	 */
	int ALL_OCCURRENCES= 3;

	/**
	 * When searching for field matches, it will exclusively find read accesses, as
	 * opposed to write accesses. Note that some expressions are considered both
	 * as field read/write accesses: for example, x++; x+= 1;
	 *
	 *  
	 */
	int READ_ACCESSES = 4;

	/**
	 * When searching for field matches, it will exclusively find write accesses, as
	 * opposed to read accesses. Note that some expressions are considered both
	 * as field read/write accesses: for example,  x++; x+= 1;
	 *
	 *  
	 */
	int WRITE_ACCESSES = 5;

	/**
	 * Ignore declaring type while searching result.
	 * Can be used in conjunction with any of the nature of match.
	 *  
	 */
	int IGNORE_DECLARING_TYPE = 0x10;

	/**
	 * Ignore return type while searching result.
	 * Can be used in conjunction with any of the nature of match.
	 * Note that:
	 * <ul>
	 * 	<li>for fields search, pattern will ignore field type</li>
	 * 	<li>this flag will have no effect for types search</li>
	 *	</ul>
	 *  
	 */
	int IGNORE_RETURN_TYPE = 0x20;

	/* Syntactic match modes */

	/**
	 * The search operation starts immediately, even if the underlying indexer
	 * has not finished indexing the workspace. Results will more likely
	 * not contain all the matches.
	 */
	int FORCE_IMMEDIATE_SEARCH = IJob.ForceImmediate;
	/**
	 * The search operation throws an <code>org.eclipse.core.runtime.OperationCanceledException</code>
	 * if the underlying indexer has not finished indexing the workspace.
	 */
	int CANCEL_IF_NOT_READY_TO_SEARCH = IJob.CancelIfNotReady;
	/**
	 * The search operation waits for the underlying indexer to finish indexing
	 * the workspace before starting the search.
	 */
	int WAIT_UNTIL_READY_TO_SEARCH = IJob.WaitUntilReady;


}
