/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search.indexing;


public interface IIndexConstants {

	/* index encoding */
	final char[] REF= "ref".toCharArray(); //$NON-NLS-1$
	final char[] METHOD_REF= "methodRef".toCharArray(); //$NON-NLS-1$
	final char[] CONSTRUCTOR_REF= "constructorRef".toCharArray(); //$NON-NLS-1$
	final char[] SUPER_REF = "superRef".toCharArray(); //$NON-NLS-1$
	final char[] TYPE_DECL = "typeDecl".toCharArray(); //$NON-NLS-1$
	final char[] METHOD_DECL= "methodDecl".toCharArray(); //$NON-NLS-1$
	final char[] FUNCTION_DECL= "functionDecl".toCharArray(); //$NON-NLS-1$
	final char[] CONSTRUCTOR_DECL= "constructorDecl".toCharArray(); //$NON-NLS-1$
	final char[] FIELD_DECL= "fieldDecl".toCharArray(); //$NON-NLS-1$
	final char[] VAR_DECL= "varDecl".toCharArray(); //$NON-NLS-1$
	final char[] TYPE_SYNONYMS = "typeSynonyms".toCharArray(); //$NON-NLS-1$
	final char[] OBJECT = "Object".toCharArray(); //$NON-NLS-1$
	final char [] WINDOW = "Window".toCharArray(); //$NON-NLS-1$
	final char[] GLOBAL = "Global".toCharArray(); //$NON-NLS-1$
	final char[] GLOBAL_SYMBOL = "@G".toCharArray(); //$NON-NLS-1$
	final char[][] COUNTS=
		new char[][] { new char[] {'/', '0'}, new char[] {'/', '1'}, new char[] {'/', '2'}, new char[] {'/', '3'}, new char[] {'/', '4'},
			new char[] {'/', '5'}, new char[] {'/', '6'}, new char[] {'/', '7'}, new char[] {'/', '8'}, new char[] {'/', '9'}
	};
	final char CLASS_SUFFIX = 'C';
	final char TYPE_SUFFIX = 0;
	final char SEPARATOR= '/';
	final char PARAMETER_SEPARATOR= ',';
	final char SECONDARY_SUFFIX = 'S';
	final char DOT = '.';

	final char[] ONE_STAR = new char[] {'*'};
	final char[][] ONE_STAR_CHAR = new char[][] {ONE_STAR};

	// used as special marker for enclosing type name of local and anonymous classes
	final char ZERO_CHAR = '0';
	final char[] ONE_ZERO = new char[] { ZERO_CHAR };
	final char[][] ONE_ZERO_CHAR = new char[][] {ONE_ZERO};

	final int PKG_REF_PATTERN = 0x0001;
	final int PKG_DECL_PATTERN = 0x0002;
	final int TYPE_REF_PATTERN = 0x0004;
	final int TYPE_DECL_PATTERN = 0x0008;
	final int SUPER_REF_PATTERN = 0x0010;
	final int CONSTRUCTOR_PATTERN = 0x0020;
	final int FIELD_PATTERN = 0x0040;
	final int METHOD_PATTERN = 0x0080;
	final int OR_PATTERN = 0x0100;
	final int LOCAL_VAR_PATTERN = 0x0200;
	final int TYPE_SYNONYMS_PATTERN = 0x0300;
}
