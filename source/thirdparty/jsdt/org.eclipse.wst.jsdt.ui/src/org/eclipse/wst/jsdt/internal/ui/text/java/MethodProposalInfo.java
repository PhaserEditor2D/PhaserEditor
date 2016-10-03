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
package org.eclipse.wst.jsdt.internal.ui.text.java;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.core.search.SearchRequestor;
import org.eclipse.wst.jsdt.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.wst.jsdt.internal.core.search.matching.MethodPattern;
import org.eclipse.wst.jsdt.internal.corext.template.java.SignatureUtil;
import org.eclipse.wst.jsdt.internal.ui.Logger;


/**
 * Proposal info that computes the javadoc lazily when it is queried.
 *
 * 
 */
public final class MethodProposalInfo extends MemberProposalInfo {

	/**
	 * Fallback in case we can't match a generic method. The fall back is only based
	 * on method name and number of parameters.
	 */
	private IFunction fFallbackMatch;

	/**
	 * Creates a new proposal info.
	 *
	 * @param project the java project to reference when resolving types
	 * @param proposal the proposal to generate information for
	 */
	public MethodProposalInfo(IJavaScriptProject project, CompletionProposal proposal) {
		super(project, proposal);
	}

	/**
	 * Resolves the member described by the receiver and returns it if found.
	 * Returns <code>null</code> if no corresponding member can be found.
	 *
	 * @return the resolved member or <code>null</code> if none is found
	 * @throws JavaScriptModelException if accessing the java model fails
	 */
	protected IMember resolveMember() throws JavaScriptModelException {
		//get the type name
		char[] typeNameChars = fProposal.getDeclarationTypeName();
		String declaringTypeName = null;
		if(typeNameChars != null) {
			declaringTypeName = String.valueOf(typeNameChars);
		}
		
		/* try using the signature if type name not set
		 * NOTE: old way of doing things, should be removed at some point
		 */
		if(declaringTypeName == null) {
			char[] declarationSignature= fProposal.getDeclarationSignature();
			if(declarationSignature != null) {
				declaringTypeName = SignatureUtil.stripSignatureToFQN(String.valueOf(declarationSignature));
			}
		}
		
		IFunction func = null;
		if (declaringTypeName!=null) {
			String functionName = String.valueOf(fProposal.getName());
			
			//get the parameter type names
			String[] paramTypeNameStrings = null;
			char[][] paramTypeNameChars = this.fProposal.getParameterTypeNames();
			if(paramTypeNameChars != null) {
				paramTypeNameStrings = new String[paramTypeNameChars.length];
				for(int i = 0; i < paramTypeNameChars.length; ++i) {
					paramTypeNameStrings[i] = paramTypeNameChars[i] != null ? String.valueOf(paramTypeNameChars[i]) : null;
				}
			} else {
				char[] signature = fProposal.getSignature();
				if(signature != null && signature.length > 0) {
					paramTypeNameStrings = Signature.getParameterTypes(String.valueOf(fProposal.getSignature()));
				} else {
					paramTypeNameStrings = new String[0];
				}
			}
			
			//search all the possible types until a match is found
			IType[] types = fJavaProject.findTypes(declaringTypeName);
			if(types != null && types.length >0) {
				for(int i = 0; i < types.length && func == null; ++i) {
					IType type = types[i];
					if (type != null) {
						boolean isConstructor = fProposal.isConstructor();
						try {
							func = findMethod(functionName, paramTypeNameStrings, isConstructor, type);
						} catch(JavaScriptModelException e) {
							//ignore, could not find method
						}
					}
				}
			} else {
				//search the index for a match
				MethodPattern methodPattern = new MethodPattern(true, false,
							functionName.toCharArray(),
							new char[][] {declaringTypeName.toCharArray()},
							SearchPattern.R_EXACT_MATCH);
				
				SearchEngine searchEngine = new SearchEngine(DefaultWorkingCopyOwner.PRIMARY);
				IJavaScriptSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaScriptElement[] {this.fJavaProject});
				final List matches = new ArrayList();
				try {
					searchEngine.search(methodPattern,
							new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
							scope,
							new SearchRequestor() {
								public void acceptSearchMatch(SearchMatch match) throws CoreException {
									if(match.getElement() instanceof IFunction) {
										matches.add(match.getElement());
									}
								}
							},
							new NullProgressMonitor());  //using a NPM here maybe a bad idea, but nothing better to do right now
				}
				catch (CoreException e) {
					Logger.logException("Failed index search for function: " + functionName, e); //$NON-NLS-1$
				}
				
				// just use the first match found
				if(!matches.isEmpty()) {
					func = (IFunction)matches.get(0);
				}
			}
		}
		
		return func;
	}

	/* adapted from JavaModelUtil */

	/**
	 * Finds a method in a type. This searches for a method with the same name
	 * and signature. Parameter types are only compared by the simple name, no
	 * resolving for the fully qualified type name is done. Constructors are
	 * only compared by parameters, not the name.
	 *
	 * @param name The name of the method to find
	 * @param paramTypes The type signatures of the parameters e.g.
	 *        <code>{"QString;","I"}</code>
	 * @param isConstructor If the method is a constructor
	 * @return The first found method or <code>null</code>, if nothing found
	 */
	private IFunction findMethod(String name, String[] paramTypes, boolean isConstructor, IType type) throws JavaScriptModelException {
		Map typeVariables= computeTypeVariables(type);
		return findMethod(name, paramTypes, isConstructor, type.getFunctions(), typeVariables);
	}

	/**
	 * The type and method signatures received in
	 * <code>CompletionProposals</code> of type <code>FUNCTION_REF</code>
	 * contain concrete type bounds. When comparing parameters of the signature
	 * with an <code>IFunction</code>, we have to make sure that we match the
	 * case where the formal method declaration uses a type variable which in
	 * the signature is already substituted with a concrete type (bound).
	 * <p>
	 * This method creates a map from type variable names to type signatures
	 * based on the position they appear in the type declaration. The type
	 * signatures are filtered through
	 * {@link SignatureUtil#getLowerBound(char[])}.
	 * </p>
	 *
	 * @param type the type to get the variables from
	 * @return a map from type variables to concrete type signatures
	 * @throws JavaScriptModelException if accessing the java model fails
	 */
	private Map computeTypeVariables(IType type) throws JavaScriptModelException {
		Map map= new HashMap();
		char[] declarationSignature= fProposal.getDeclarationSignature();
		if (declarationSignature == null) // array methods don't contain a declaration signature
			return map;

		return map;
	}

	/**
	 * Finds a method by name. This searches for a method with a name and
	 * signature. Parameter types are only compared by the simple name, no
	 * resolving for the fully qualified type name is done. Constructors are
	 * only compared by parameters, not the name.
	 *
	 * @param name The name of the method to find
	 * @param paramTypes The type signatures of the parameters e.g.
	 *        <code>{"QString;","I"}</code>
	 * @param isConstructor If the method is a constructor
	 * @param methods The methods to search in
	 * @param typeVariables a map from type variables to concretely used types
	 * @return The found method or <code>null</code>, if nothing found
	 */
	private IFunction findMethod(String name, String[] paramTypes, boolean isConstructor, IFunction[] methods, Map typeVariables) throws JavaScriptModelException {
		for (int i= methods.length - 1; i >= 0; i--) {
			if (isSameMethodSignature(name, paramTypes, isConstructor, methods[i], typeVariables)) {
				return methods[i];
			}
		}
		return fFallbackMatch;
	}

	/**
	 * Tests if a method equals to the given signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type
	 * name is done. Constructors are only compared by parameters, not the name.
	 *
	 * @param name Name of the method
	 * @param paramTypes The type signatures of the parameters e.g.
	 *        <code>{"QString;","I"}</code>
	 * @param isConstructor Specifies if the method is a constructor
	 * @param method the method to be compared with this info's method
	 * @param typeVariables a map from type variables to types
	 * @return Returns <code>true</code> if the method has the given name and
	 *         parameter types and constructor state.
	 */
	private boolean isSameMethodSignature(String name, String[] paramTypes, boolean isConstructor, IFunction method, Map typeVariables) throws JavaScriptModelException {
		if (isConstructor || name.equals(method.getElementName())) {
			if (isConstructor == method.isConstructor()) {
				String[] otherParams= method.getParameterTypes(); // types may be type variables
				if (paramTypes.length == otherParams.length) {
					fFallbackMatch= method;
					String signature= method.getSignature();
					String[] otherParamsFromSignature= Signature.getParameterTypes(signature); // types are resolved / upper-bounded
					// no need to check method type variables since these are
					// not yet bound when proposing a method
					for (int i= 0; i < paramTypes.length; i++) {
						String ourParamName= computeSimpleTypeName(paramTypes[i], typeVariables);
						String otherParamName1= computeSimpleTypeName(otherParams[i], typeVariables);
						String otherParamName2= computeSimpleTypeName(otherParamsFromSignature[i], typeVariables);
						
						if (!ourParamName.equals(otherParamName1) && !ourParamName.equals(otherParamName2)) {
							return false;
						}
					}
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the simple erased name for a given type signature, possibly replacing type variables.
	 * 
	 * @param signature the type signature
	 * @param typeVariables the Map&lt;SimpleName, VariableName>
	 * @return the simple erased name for signature
	 */
	private String computeSimpleTypeName(String signature, Map typeVariables) {
		String simpleName = ""; //$NON-NLS-1$
		if(signature != null && signature.length() > 0) {
			// method equality uses erased types
			String erasure=signature;
			erasure= erasure.replaceAll("/", ".");  //$NON-NLS-1$//$NON-NLS-2$
			simpleName= Signature.getSimpleName(Signature.toString(erasure));
			char[] typeVar= (char[]) typeVariables.get(simpleName);
			if (typeVar != null) {
				simpleName= String.valueOf(Signature.getSignatureSimpleName(typeVar));
			}
		}
		return simpleName;
	}
}
