/*******************************************************************************
 * Copyright (c) 2000, 20112 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search.matching;

import java.io.IOException;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.core.index.EntryResult;
import org.eclipse.wst.jsdt.internal.core.index.Index;

public class SuperTypeReferencePattern extends JavaSearchPattern {

	public char[] superTypeName;
	public char[] typeName;

	protected static char[][] CATEGORIES = {SUPER_REF};

	public static char[] createIndexKey(char[] typeName, char[] superTypeName) {
		if (superTypeName == null) {
			superTypeName = OBJECT;
		}

		// superSimpleName / superQualification / simpleName /
		// enclosingTypeName / typeParameters / packageName / superClassOrInterface
		// classOrInterface modifiers
		// superTypeName / typeName / modifiers
		int superLength = superTypeName == null ? 0 : superTypeName.length;
		int typeLength = typeName == null ? 0 : typeName.length;
		char[] result = new char[superLength + typeLength + 1];
		int pos = 0;
		if (superLength > 0) {
			System.arraycopy(superTypeName, 0, result, pos, superLength);
			pos += superLength;
		}
		result[pos++] = SEPARATOR;
		if (typeLength > 0) {
			System.arraycopy(typeName, 0, result, pos, typeLength);
			pos += typeLength;
		}
		return result;
	}

	public SuperTypeReferencePattern(char[] superTypeName, int matchRule) {
		this(matchRule);

		this.superTypeName = isCaseSensitive() ? superTypeName : CharOperation.toLowerCase(superTypeName);
	}

	SuperTypeReferencePattern(int matchRule) {
		super(SUPER_REF_PATTERN, matchRule);
	}

	/**
	 * <p>
	 * superSimpleName / superQualification / simpleName / enclosingTypeName /
	 * typeParameters / pkgName / superClassOrInterface classOrInterface modifiers
	 * </p>
	 *
	 * @see org.eclipse.wst.jsdt.core.search.SearchPattern#decodeIndexKey(char[])
	 */
	public void decodeIndexKey(char[] key) {
		int slash = CharOperation.indexOf(SEPARATOR, key, 0);
		this.superTypeName = CharOperation.subarray(key, 0, slash);

		// some values may not have been know when indexed so decode as null
		int start = slash + 1;
		slash = CharOperation.indexOf(SEPARATOR, key, start);
		this.typeName = CharOperation.subarray(key, start, slash);
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.JavaSearchPattern#getBlankPattern()
	 */
	public SearchPattern getBlankPattern() {
		return new SuperTypeReferencePattern(R_EXACT_MATCH | R_CASE_SENSITIVE);
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchPattern#getIndexCategories()
	 */
	public char[][] getIndexCategories() {
		return CATEGORIES;
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchPattern#matchesDecodedKey(org.eclipse.wst.jsdt.core.search.SearchPattern)
	 */
	public boolean matchesDecodedKey(SearchPattern decodedPattern) {
		SuperTypeReferencePattern pattern = (SuperTypeReferencePattern) decodedPattern;

		if (pattern.superTypeName != null) {
			if (!matchesName(this.superTypeName, pattern.superTypeName)) {
				return false;
			}
		}

		return matchesName(this.typeName, pattern.typeName);
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.InternalSearchPattern#queryIn(org.eclipse.wst.jsdt.internal.core.index.Index)
	 */
	EntryResult[] queryIn(Index index) throws IOException {
		char[] key = this.superTypeName; // can be null
		int matchRule = getMatchRule();

		// cannot include the superQualification since it may not exist in the index
		switch (getMatchMode()) {
			case R_EXACT_MATCH :
				if (this.isCamelCase)
					break;
				// do a prefix query with the superSimpleName
				matchRule &= ~R_EXACT_MATCH;
				matchRule |= R_PREFIX_MATCH;
				if (this.superTypeName != null)
					key = CharOperation.append(this.superTypeName, SEPARATOR);
				break;
			case R_PREFIX_MATCH :
				// do a prefix query with the superSimpleName
				break;
			case R_PATTERN_MATCH :
				// do a pattern query with the superSimpleName
				break;
			case R_REGEXP_MATCH :
				// TODO (frederic) implement regular expression match
				break;
		}

		// match rule is irrelevant when the key is null
		return index.query(getIndexCategories(), key, matchRule); 
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.JavaSearchPattern#print(java.lang.StringBuffer)
	 */
	protected StringBuffer print(StringBuffer output) {
		output.append("SuperClassReferencePattern: <"); //$NON-NLS-1$

		if (superTypeName != null) {
			output.append(superTypeName);
		} else {
			output.append("*"); //$NON-NLS-1$
		}
		
		output.append(">"); //$NON-NLS-1$
		return super.print(output);
	}
}
