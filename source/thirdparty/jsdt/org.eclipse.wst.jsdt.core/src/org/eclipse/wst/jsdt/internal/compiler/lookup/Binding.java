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
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;

public abstract class Binding {

	// binding kinds
	public static final int FIELD = ASTNode.Bit1;
	public static final int LOCAL = ASTNode.Bit2;
	public static final int VARIABLE = FIELD | LOCAL;
	public static final int TYPE = ASTNode.Bit3;
	public static final int METHOD = ASTNode.Bit4;
	public static final int PACKAGE = ASTNode.Bit15;
	public static final int IMPORT = ASTNode.Bit6;
	public static final int ARRAY_TYPE = TYPE | ASTNode.Bit7;
	public static final int BASE_TYPE = TYPE | ASTNode.Bit8;
	public static final int GLOBAL = ASTNode.Bit9;
	public static final int COMPILATION_UNIT = ASTNode.Bit14;

	public static final int BASIC_BINDINGS_MASK= METHOD|TYPE|VARIABLE;
	public static final int NUMBER_BASIC_BINDING= (METHOD+TYPE+VARIABLE)+1;

	// Shared binding collections
	public static final TypeBinding[] NO_TYPES = new TypeBinding[0];
	public static final TypeBinding[] NO_PARAMETERS = new TypeBinding[0];
	public static final ReferenceBinding[] NO_EXCEPTIONS = new ReferenceBinding[0];
	public static final ReferenceBinding[] ANY_EXCEPTION = new ReferenceBinding[] { null }; // special handler for all exceptions
	public static final FieldBinding[] NO_FIELDS = new FieldBinding[0];
	public static final MethodBinding[] NO_METHODS = new MethodBinding[0];
	public static final ReferenceBinding[] NO_MIXINS = new ReferenceBinding[0];
	public static final ReferenceBinding[] NO_MEMBER_TYPES = new ReferenceBinding[0];

	/*
	* Answer the receiver's binding type from Binding.BindingID.
	*/
	public abstract int kind();
	/*
	 * Computes a key that uniquely identifies this binding.
	 * Returns null if binding is not a TypeBinding, a FunctionBinding, a FieldBinding or a PackageBinding.
	 */
	public char[] computeUniqueKey() {
		return computeUniqueKey(true/*leaf*/);
	}
	/*
	 * Computes a key that uniquely identifies this binding. Optinaly include access flags.
	 * Returns null if binding is not a TypeBinding, a FunctionBinding, a FieldBinding or a PackageBinding.
	 */
	public char[] computeUniqueKey(boolean isLeaf) {
		return null;
	}

	/* API
	* Answer true if the receiver is not a problem binding
	*/
	public final boolean isValidBinding() {
		return problemId() == ProblemReasons.NoError;
	}
	/* API
	* Answer the problem id associated with the receiver.
	* NoError if the receiver is a valid binding.
	*/
	// TODO (philippe) should rename into problemReason()
	public int problemId() {
		return ProblemReasons.NoError;
	}
	/* Answer a printable representation of the receiver.
	*/
	public abstract char[] readableName();
	/* Shorter printable representation of the receiver (no qualified type)
	 */
	public char[] shortReadableName(){
		return readableName();
	}
}
