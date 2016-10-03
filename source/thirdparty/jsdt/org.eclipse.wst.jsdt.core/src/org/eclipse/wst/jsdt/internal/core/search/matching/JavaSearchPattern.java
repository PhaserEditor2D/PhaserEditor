/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search.matching;

import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;


public class JavaSearchPattern extends SearchPattern implements IIndexConstants {

	/*
	 * Whether this pattern is case sensitive.
	 */
	boolean isCaseSensitive;

	/*
	 * Whether this pattern is camel case.
	 */
	boolean isCamelCase;

	/**
	 * One of following pattern value:
	 * <ul>
	 * 	<li>{@link #R_EXACT_MATCH}</li>
	 *		<li>{@link #R_PREFIX_MATCH}</li>
	 *		<li>{@link #R_PATTERN_MATCH}</li>
	 *		<li>{@link #R_REGEXP_MATCH}</li>
	 *		<li>{@link #R_CAMELCASE_MATCH}</li>
	 * </ul>
	 */
	int matchMode;

	/**
	 * One of {@link #R_ERASURE_MATCH}, {@link #R_EQUIVALENT_MATCH}, {@link #R_FULL_MATCH}.
	 */
	int matchCompatibility;

	/**
	 * Mask used on match rule for match mode.
	 */
	public static final int MATCH_MODE_MASK = R_EXACT_MATCH | R_PREFIX_MATCH | R_PATTERN_MATCH | R_REGEXP_MATCH;

	/**
	 * Mask used on match rule for generic relevance.
	 */
	public static final int MATCH_COMPATIBILITY_MASK = R_ERASURE_MATCH | R_EQUIVALENT_MATCH | R_FULL_MATCH;

	// Signatures and arguments for parameterized types search
	char[][] typeSignatures;
	private char[][][] typeArguments;
	private int flags = 0;
	static final int HAS_TYPE_ARGUMENTS = 1;

	protected JavaSearchPattern(int patternKind, int matchRule) {
		super(matchRule);
		((InternalSearchPattern)this).kind = patternKind;
		// Use getMatchRule() instead of matchRule as super constructor may modify its value
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=81377
		int rule = getMatchRule();
		this.isCaseSensitive = (rule & R_CASE_SENSITIVE) != 0;
		this.isCamelCase = (rule & R_CAMELCASE_MATCH) != 0;
		this.matchCompatibility = rule & MATCH_COMPATIBILITY_MASK;
		this.matchMode = rule & MATCH_MODE_MASK;
	}

	public SearchPattern getBlankPattern() {
		return null;
	}

	int getMatchMode() {
		return this.matchMode;
	}

	boolean isCamelCase() {
		return this.isCamelCase;
	}

	boolean isCaseSensitive () {
		return this.isCaseSensitive;
	}

	boolean isErasureMatch() {
		return (this.matchCompatibility & R_ERASURE_MATCH) != 0;
	}

	boolean isEquivalentMatch() {
		return (this.matchCompatibility & R_EQUIVALENT_MATCH) != 0;
	}

	/*
	 * Extract method arguments using unique key for parameterized methods
	 * and type parameters for non-generic ones.
	 */
	char[][] extractMethodArguments(IFunction method) {
		return null;
	}

	/**
	 * @return Returns the typeArguments.
	 */
	final char[][][] getTypeArguments() {
		return typeArguments;
	}

	/**
	 * Returns whether the pattern has signatures or not.
	 * If pattern {@link #typeArguments} field, this field shows that it was built
	 * on a generic source type.
	 * @return true if {@link #typeSignatures} field is not null and has a length greater than 0.
	 */
	public final boolean hasSignatures() {
		return this.typeSignatures != null && this.typeSignatures.length > 0;
	}

	/**
	 * Returns whether the pattern includes type arguments information or not.
	 * @return default is false
	 */
	public final boolean hasTypeArguments() {
		return (this.flags & HAS_TYPE_ARGUMENTS) != 0;
	}

	/**
	 * Returns whether the pattern includes type parameters information or not.
	 * @return true if {@link #typeArguments} contains type parameters instead
	 * 	type arguments signatures.
	 */
	public final boolean hasTypeParameters() {
		return !hasSignatures() && hasTypeArguments();
	}

	/**
	 * Return whether two suffixes are compatible.
	 *
	 * Note that obvious compatibility values as equals and {@link IIndexConstants#TYPE_SUFFIX}
	 * has to be tested by caller to avoid unnecessary method call...
	 *
	 * @param typeSuffix
	 * @param patternSuffix
	 * @return true if suffixes are compatible, false otherwise
	 */
	boolean matchDifferentTypeSuffixes(int typeSuffix, int patternSuffix) {
		switch(typeSuffix) {
			case CLASS_SUFFIX :
				return false;
		}

		// Default behavior is to match suffixes
		return true;
	}

	protected StringBuffer print(StringBuffer output) {
		output.append(", "); //$NON-NLS-1$
		if (hasTypeArguments() && hasSignatures()) {
			output.append("signature:\""); //$NON-NLS-1$
			output.append(this.typeSignatures[0]);
			output.append("\", "); //$NON-NLS-1$
		}
		if (this.isCamelCase) {
			output.append("camel case + "); //$NON-NLS-1$
		}
		switch(getMatchMode()) {
			case R_EXACT_MATCH :
				output.append("exact match,"); //$NON-NLS-1$
				break;
			case R_PREFIX_MATCH :
				output.append("prefix match,"); //$NON-NLS-1$
				break;
			case R_PATTERN_MATCH :
				output.append("pattern match,"); //$NON-NLS-1$
				break;
			case R_REGEXP_MATCH :
				output.append("regexp match, "); //$NON-NLS-1$
				break;
		}
		if (isCaseSensitive())
			output.append(" case sensitive"); //$NON-NLS-1$
		else
			output.append(" case insensitive"); //$NON-NLS-1$
		if ((this.matchCompatibility & R_ERASURE_MATCH) != 0) {
			output.append(", erasure only"); //$NON-NLS-1$
		}
		if ((this.matchCompatibility & R_EQUIVALENT_MATCH) != 0) {
			output.append(", equivalent oronly"); //$NON-NLS-1$
		}
		return output;
	}
	/**
	 * @param typeArguments The typeArguments to set.
	 */
	final void setTypeArguments(char[][][] typeArguments) {
		this.typeArguments = typeArguments;
		// update flags
		if (this.typeArguments != null) {
			int length = this.typeArguments.length;
			for (int i=0; i<length; i++) {
				if (this.typeArguments[i] != null && this.typeArguments[i].length > 0) {
					this.flags |= HAS_TYPE_ARGUMENTS;
					break;
				}
			}
		}
	}

	/*
	 * Extract and store type signatures and arguments using unique key for parameterized types
	 * and type parameters for non-generic ones
	 */
	void storeTypeSignaturesAndArguments(IType type) {
		// Scan hierachy to store type arguments at each level
		char[][][] typeParameters = new char[10][][];
		int ptr = -1;
		boolean hasParameters = false;
		IJavaScriptElement parent = type;
		while (parent != null && parent.getElementType() == IJavaScriptElement.TYPE) {
			if (++ptr > typeParameters.length) {
				System.arraycopy(typeParameters, 0, typeParameters = new char[typeParameters.length+10][][], 0, ptr);
			}
			parent = parent.getParent();
		}
		
		// Store type arguments if any
		if (hasParameters) {
			if (++ptr < typeParameters.length)
				System.arraycopy(typeParameters, 0, typeParameters = new char[ptr][][], 0, ptr);
			setTypeArguments(typeParameters);
		}
	
	}
	
	public final String toString() {
		return print(new StringBuffer(30)).toString();
	}
}
