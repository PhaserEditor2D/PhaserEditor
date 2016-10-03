/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.dom;

import org.eclipse.wst.jsdt.core.IJavaScriptUnit;

/**
 * An AST requestor handles ASTs for javaScript units passed to
 * <code>ASTParser.createASTs</code>.
 * <p>
 * <code>ASTRequestor.acceptAST</code> is called for each of the
 * javaScript units passed to <code>ASTParser.createASTs</code>.
 * After all the javaScript units have been processed,
 * <code>ASTRequestor.acceptBindings</code> is called for each
 * of the binding keys passed to <code>ASTParser.createASTs</code>.
 * </p>
 * <p>
 * This class is intended to be subclassed by clients.
 * AST requestors are serially reusable, but neither reentrant nor
 * thread-safe.
 * </p>
 *
 * @see ASTParser#createASTs(IJavaScriptUnit[], String[], ASTRequestor, org.eclipse.core.runtime.IProgressMonitor)
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public abstract class ASTRequestor {

	/**
	 * The javaScript unit resolver used to resolve bindings, or
	 * <code>null</code> if none. Note that this field is non-null
	 * only within the dynamic scope of a call to
	 * <code>ASTParser.createASTs</code>.
	 */
	JavaScriptUnitResolver compilationUnitResolver = null;

	/**
	 * Creates a new instance.
	 */
	protected ASTRequestor() {
		// do nothing
	}

	/**
	 * Accepts an AST corresponding to the javaScript unit.
	 * That is, <code>ast</code> is an AST for <code>source</code>.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Clients should override to process the resulting AST.
	 * </p>
	 *
	 * @param source the javaScript unit the ast is coming from
	 * @param ast the requested abtract syntax tree
	 */
	public void acceptAST(IJavaScriptUnit source, JavaScriptUnit ast) {
		// do nothing
	}

	/**
	 * Accepts a binding corresponding to the binding key.
	 * That is, <code>binding</code> is the binding for
	 * <code>bindingKey</code>; <code>binding</code> is <code>null</code>
	 * if the key cannot be resolved.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Clients should override to process the resulting binding.
	 * </p>
	 *
	 * @param bindingKey the key of the requested binding
	 * @param binding the requested binding, or <code>null</code> if none
	 */
	public void acceptBinding(String bindingKey, IBinding binding) {
		// do nothing
	}

	/**
	 * Resolves bindings for the given binding keys.
	 * The given binding keys must have been obtained earlier
	 * using {@link IBinding#getKey()}.
	 * <p>
	 * If a binding key cannot be resolved, <code>null</code> is put in the resulting array.
	 * Bindings can only be resolved in the dynamic scope of a <code>ASTParser.createASTs</code>,
	 * and only if <code>ASTParser.resolveBindings(true)</code> was specified.
	 * </p>
	 * <p>
	 * Caveat: During an <code>acceptAST</code> callback, there are implementation
	 * limitations concerning the look up of binding keys representing local elements.
	 * In some cases, the binding is unavailable, and <code>null</code> will be returned.
	 * This is only an issue during an <code>acceptAST</code> callback, and only
	 * when the binding key represents a local element (e.g., local variable,
	 * local class, method declared in anonymous class). There is no such limitation
	 * outside of <code>acceptAST</code> callbacks, or for top-level types and their
	 * members even within <code>acceptAST</code> callbacks.
	 * </p>
	 *
	 * @param bindingKeys the binding keys to look up
	 * @return a list of bindings paralleling the <code>bindingKeys</code> parameter,
	 * with <code>null</code> entries for keys that could not be resolved
	 */
	public final IBinding[] createBindings(String[] bindingKeys) {
		int length = bindingKeys.length;
		IBinding[] result = new IBinding[length];
		for (int i = 0; i < length; i++) {
			result[i] = null;
			if (this.compilationUnitResolver != null) {
				result[i] = this.compilationUnitResolver.createBinding(bindingKeys[i]);
			}
		}
		return result;
	}
}
