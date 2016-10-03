/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfBinding;

public class GlobalBinding extends Binding implements TypeConstants {
	public long tagBits = 0; // See values in the interface TagBits below

	public LookupEnvironment environment;
	HashtableOfBinding[] knownBindings = new HashtableOfBinding[NUMBER_BASIC_BINDING];

	protected GlobalBinding() {
		// for creating problem package
	}

	public GlobalBinding(LookupEnvironment environment) {
		this.environment = environment;
	}

	private void addNotFoundBinding1(char[] simpleName, int mask) {
		if (knownBindings[mask] == null)
			knownBindings[mask] = new HashtableOfBinding(25);
		knownBindings[mask].put(simpleName, LookupEnvironment.TheNotFoundType);
	}

	private void addNotFoundBinding(char[] simpleName, int mask) {
		if (((Binding.VARIABLE | Binding.FIELD) & mask) != 0)
			addNotFoundBinding1(simpleName, Binding.VARIABLE | Binding.FIELD);
		if ((Binding.METHOD & mask) != 0)
			addNotFoundBinding1(simpleName, Binding.METHOD);
		if ((Binding.TYPE & mask) != 0)
			addNotFoundBinding1(simpleName, Binding.TYPE);
	}

	void addType(ReferenceBinding element) {
		if (knownBindings[Binding.TYPE] == null)
			knownBindings[Binding.TYPE] = new HashtableOfBinding(25);
		knownBindings[Binding.TYPE].put(
				element.compoundName[element.compoundName.length - 1], element);
	}

	public void addBinding(Binding element, char[] name, int mask) {
		if (mask < knownBindings.length) {
			if (knownBindings[mask] == null)
				knownBindings[mask] = new HashtableOfBinding(25);
			knownBindings[mask].put(name, element);
		}
	}

	/*
	 * API Answer the receiver's binding type from Binding.BindingID.
	 */
	public final int kind() {
		return Binding.GLOBAL;
	}

	/*
	 * slash separated name org.eclipse.wst.wst.jsdt.core -->
	 * org/eclipse/jdt/core
	 */
	public char[] computeUniqueKey(boolean isLeaf) {
		return new char[]{'G','L','O','B','A','L','/'};
	}

	/*
	 * Answer the type named name; ask the oracle for the type if its not in the
	 * cache. Answer a NotVisible problem type if the type is not visible from
	 * the invocationPackage. Answer null if it could not be resolved.
	 * 
	 * NOTE: This should only be used by source types/scopes which know there is
	 * NOT a package with the same name.
	 */
	ReferenceBinding getType(char[] name) {
		return (ReferenceBinding) getBinding(name, Binding.TYPE);
	}

	public Binding getBinding(char[] name, int mask) {
		Binding typeBinding = getBinding0(name, mask);
		if (typeBinding == null) {
			if ((typeBinding = environment.askForBinding(this, name, mask)) == null) {
				// not found so remember a problem type binding in the cache for
				// future lookups
				addNotFoundBinding(name, mask);
				return null;
			}
		}

		if (typeBinding == LookupEnvironment.TheNotFoundType)
			return null;

		// typeBinding = BinaryTypeBinding.resolveType(typeBinding, environment,
		// false); // no raw conversion for now
		// if (typeBinding.isNestedType())
		// return new ProblemReferenceBinding(name, typeBinding,
		// ProblemReasons.InternalNameProvided);
		return typeBinding;
	}

	/*
	 * Answer the type named name if it exists in the cache. Answer
	 * theNotFoundType if it could not be resolved the first time it was looked
	 * up, otherwise answer null.
	 * 
	 * NOTE: Senders must convert theNotFoundType into a real problem reference
	 * type if its to returned.
	 */

	ReferenceBinding getType0(char[] name) {
		if (knownBindings[Binding.TYPE] == null)
			return null;
		return (ReferenceBinding) knownBindings[Binding.TYPE].get(name);
	}

	Binding getBinding1(char[] name, int mask) {
		if (knownBindings[mask] == null)
			return null;
		return knownBindings[mask].get(name);
	}

	Binding getBinding0(char[] name, int mask) {
		Binding binding;
		if ((mask & (Binding.VARIABLE | Binding.FIELD)) != 0) {
			binding = getBinding1(name, Binding.VARIABLE | Binding.FIELD);
			if (binding != null)
				return binding;
		}
		if ((mask & (Binding.TYPE)) != 0) {
			binding = getBinding1(name, Binding.TYPE);
			if (binding != null)
				return binding;
		}
		if ((mask & (Binding.METHOD)) != 0) {
			binding = getBinding1(name, Binding.METHOD);
			if (binding != null)
				return binding;
		}
		return null;
	}

	/*
	 * Answer the package or type named name; ask the oracle if it is not in the
	 * cache. Answer null if it could not be resolved.
	 * 
	 * When collisions exist between a type name & a package name, answer the
	 * type. Treat the package as if it does not exist... a problem was already
	 * reported when the type was defined.
	 * 
	 * NOTE: no visibility checks are performed. THIS SHOULD ONLY BE USED BY
	 * SOURCE TYPES/SCOPES.
	 */

	public Binding getType(char[] name, int mask) {
		Binding typeBinding = getBinding0(name, mask);
		// if (typeBinding != null && typeBinding !=
		// LookupEnvironment.TheNotFoundType) {
		// typeBinding = BinaryTypeBinding.resolveType(typeBinding, environment,
		// false); // no raw conversion for now
		// if (typeBinding.isNestedType())
		// return new ProblemReferenceBinding(name, typeBinding,
		// ProblemReasons.InternalNameProvided);
		// return typeBinding;
		// }
		if (typeBinding != null)
			return typeBinding;

		if (mask != Binding.PACKAGE) { // have not looked
																// for it before
			if ((typeBinding = environment.askForBinding(this, name, mask)) != null) {
				// if (typeBinding.isNestedType())
				// return new ProblemReferenceBinding(name, typeBinding,
				// ProblemReasons.InternalNameProvided);
				return typeBinding;
			}

			// Since name could not be found, add a problem binding
			// to the collections so it will be reported as an error next time.
			addNotFoundBinding(name, mask);
		}

		return null;
	}

	public char[] readableName() /* java.lang */{
		return new char[]{'G','L','O','B','A','L','/'};
	}

	public String toString() {
		return "Global Namespace"; //$NON-NLS-1$
	}
}
