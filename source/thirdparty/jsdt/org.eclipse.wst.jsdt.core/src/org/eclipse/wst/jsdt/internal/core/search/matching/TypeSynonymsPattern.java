/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

/**
 * <p>
 * Pattern used to represent the synoyms of a type.
 * </p>
 */
public class TypeSynonymsPattern extends JavaSearchPattern {

	/**
	 * <p>
	 * Pattern prefix used when searching for type synonyms.
	 * </p>
	 */
	private static final char[] SEARCH_PATTERN_PREFIX = new char[]{'*', '/'};

	/**
	 * <p>
	 * Pattern suffix used when searching for type synonyms.
	 * </p>
	 */
	private static final char[] SEARCH_PATTERN_SUFFIX = new char[]{'/', '*'};

	/**
	 * <p>
	 * Categories useing this pattern.
	 * </p>
	 */
	private static char[][] CATEGORIES = {TYPE_SYNONYMS};

	/**
	 * <p>
	 * Type name to search for synonyms for.
	 * </p>
	 */
	private final char[] fSearchTypeName;

	/**
	 * <p>
	 * All of the type names in this array are synonyms.
	 * </p>
	 */
	private char[][] fSynonyms;
	
	/**
	 * <p>
	 * Constructor used to search for synonyms of a type with the given name.
	 * </p>
	 * 
	 * @param searchTypeName
	 *            create a pattern used to search for synonyms of a type with
	 *            this name
	 */
	public TypeSynonymsPattern(char[] searchTypeName) {
		super(TYPE_SYNONYMS_PATTERN, R_PATTERN_MATCH);
		
		this.fSearchTypeName = searchTypeName;
	}
	
	/**
	 * <p>
	 * Private internal constructor for creating a blank pattern to decode a
	 * result key into.
	 * </p>
	 */
	private TypeSynonymsPattern() {
		this(null);
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchPattern#decodeIndexKey(char[])
	 */
	public void decodeIndexKey(char[] key) {
		//min length for proper key is greater then 2 (//)
		if(key != null && key.length > 2) {
			this.fSynonyms = CharOperation.splitOn('/', key, 1, key.length-1);
		}
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.JavaSearchPattern#getBlankPattern()
	 */
	public SearchPattern getBlankPattern() {
		return new TypeSynonymsPattern();
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchPattern#getIndexCategories()
	 */
	public char[][] getIndexCategories() {
		return CATEGORIES;
	}
	
	/**
	 * @return synonyms of the type name being searched for, including the given type name
	 */
	public char[][] getSynonyms() {
		return this.fSynonyms;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.InternalSearchPattern#queryIn(org.eclipse.wst.jsdt.internal.core.index.Index)
	 */
	EntryResult[] queryIn(Index index) throws IOException {
		char[] key = CharOperation.concat(SEARCH_PATTERN_PREFIX, this.fSearchTypeName, SEARCH_PATTERN_SUFFIX);
		
		if(!this.isCaseSensitive) {
			key = CharOperation.toLowerCase(key);
		}
		
		return index.query(getIndexCategories(), key, this.getMatchRule());
	}
	
	/**
	 * <p>
	 * Creates an index key for the given type and its synonyms.
	 * </p>
	 * 
	 * <p>
	 * <b>Key Syntax</b>:
	 * <code>/type/synonymType0/synonymType1/synonymType2/</code>
	 * </p>
	 * 
	 * @param type
	 *            name of the type to create synonym index key for
	 * @param synonymTypes
	 *            synonyms of the given type name to create index key for
	 * 
	 * @return synonyms index key generated from the given type and synonyms
	 *         list
	 */
	public static char[] createIndexKey(char[] type, char[][] synonymTypes) {
		char[] key = null;
		
		if(type != null && type.length > 0 && synonymTypes != null && synonymTypes.length > 0) {
			key = CharOperation.concat(type, CharOperation.concatWith(synonymTypes, '/', true), '/');
			key = CharOperation.concat('/', key, '/');
		}
		
		return key;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.JavaSearchPattern#print(java.lang.StringBuffer)
	 */
	protected StringBuffer print(StringBuffer output) {
		if(this.fSearchTypeName != null) {
			output.append("SearchTypeName: "); //$NON-NLS-1$
			output.append(this.fSearchTypeName);
			output.append("\n"); //$NON-NLS-1$
		}
		
		if(this.fSynonyms != null) {
			output.append("Synonyms: "); //$NON-NLS-1$
			for(int i = 0; i < this.fSynonyms.length; ++i) {
				output.append(this.fSynonyms[i]);
				output.append(", "); //$NON-NLS-1$
			}
		}
		
		return super.print(output);
	}
}
