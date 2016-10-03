/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.ui.text;

/**
 * Definition of JavaScript partitioning and its partitions.
 *
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves. */
public interface IJavaScriptPartitions {

	/**
	 * The identifier of the JavaScript partitioning.
	 */
	String JAVA_PARTITIONING= "___java_partitioning";  //$NON-NLS-1$

	/**
	 * The identifier of the single-line (JLS2: EndOfLineComment) end comment partition content type.
	 */
	String JAVA_SINGLE_LINE_COMMENT= "__java_singleline_comment"; //$NON-NLS-1$

	/**
	 * The identifier multi-line (JLS2: TraditionalComment) comment partition content type.
	 */
	String JAVA_MULTI_LINE_COMMENT= "__java_multiline_comment"; //$NON-NLS-1$

	/**
	 * The identifier of the Javadoc (JLS2: DocumentationComment) partition content type.
	 */
	String JAVA_DOC= "__java_javadoc"; //$NON-NLS-1$

	/**
	 * The identifier of the JavaScript string partition content type.
	 */
	String JAVA_STRING= "__java_string"; //$NON-NLS-1$

	/**
	 * The identifier of the JavaScript character partition content type.
	 */
	String JAVA_CHARACTER= "__java_character";  //$NON-NLS-1$
	
	/**
	 * The shebang line
	 */
	String JAVA_SHEBANG_LINE= "__java_shebang";
}
