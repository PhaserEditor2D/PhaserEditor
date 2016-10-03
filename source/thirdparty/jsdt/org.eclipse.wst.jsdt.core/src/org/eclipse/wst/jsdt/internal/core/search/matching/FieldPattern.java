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
package org.eclipse.wst.jsdt.internal.core.search.matching;

import java.io.IOException;

import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.core.Logger;
import org.eclipse.wst.jsdt.internal.core.index.EntryResult;
import org.eclipse.wst.jsdt.internal.core.index.Index;
import org.eclipse.wst.jsdt.internal.core.util.QualificationHelpers;

public class FieldPattern extends VariablePattern {
	/**
	 * <p><b>Optional</b></p>
	 * 
	 * <p>Qualification of the declaring type containing this field.</p>
	 * 
	 * <p>This can either be a single qualification if this pattern is representing a specific field
	 * defined on a specific type, or it can be a list of qualifications if it is a pattern for finding
	 * a field that could be defined on many different types.</p>
	 * 
	 * <p><b>Note:</b> If this field is defined then the {@link #declaringSimpleName} must
	 * also be defined.</p>
	 * 
	 * @see #declaringSimpleName
	 */
	private char[][] declaringQualification;
	
	/**
	 * <p><b>Optional</b></p>
	 * 
	 * <p>Simple name of the declaring type containing this field.</p>
	 * 
	 * <p>This can either be a single simple name if this pattern is representing a specific field
	 * defined on a specific type, or it can be a list of simple names if it is a pattern for finding
	 * a field that could be defined on many different types.</p>
	 * 
	 * <p><b>Note:</b> If this field is defined then the {@link #declaringQualification}
	 * can be defined, but does not have to be.</p>
	 * 
	 * @see #declaringQualification
	 */
	private char[][] declaringSimpleName;
	
	// type
	public char[] typeQualification;
	public char[] typeSimpleName;
	
	// modifiers
	public int modifiers;
	
	protected static char[][] REF_CATEGORIES = { REF };
	protected static char[][] REF_AND_DECL_CATEGORIES = { REF, FIELD_DECL, VAR_DECL };
	protected static char[][] DECL_CATEGORIES = { FIELD_DECL, VAR_DECL };
	
	/**
	 * @deprecated this will be removed at some point
	 */
	protected boolean isVar;
	
	/**
	 * <p>Creates a field pattern index key based on the given information.</p>
	 * 
	 * @param fieldName
	 * @param typeName
	 * @param declaringType
	 * @param modifiers
	 * @return
	 */
	public static char[] createIndexKey(char[] fieldName, char[] typeName, char[] declaringType, int modifiers) {
		char[] indexKey = null;
		
		if(fieldName != null  && fieldName.length > 0) {
			//get lengths
			int typeNameLength= (typeName == null ? 0 : typeName.length);
			int declaringTypeLength = (declaringType == null ? 0 : declaringType.length);
			
			int resultLength = fieldName.length
					+ 1 + typeNameLength
					+ 1 + declaringTypeLength
					+ 3; //modifiers
			
			//create result char array
			indexKey = new char[resultLength];
			
			//add type name to result
			int pos = 0;
			System.arraycopy(fieldName, 0, indexKey, pos, fieldName.length);
			pos += fieldName.length;
		
			//add declaring type
			indexKey[pos++] = SEPARATOR;
			if(declaringTypeLength > 0) {
				System.arraycopy(declaringType, 0, indexKey, pos, declaringTypeLength);
				pos += declaringTypeLength;
			}
			
			//add type
			indexKey[pos++] = SEPARATOR;
			if(typeNameLength > 0) {
				System.arraycopy(typeName, 0, indexKey, pos, typeNameLength);
				pos += typeNameLength;
			}
			
			//add modifiers
			indexKey[pos++] = SEPARATOR;
			indexKey[pos++] = (char) modifiers;
			indexKey[pos++] = (char) (modifiers>>16);
		}
			
		return indexKey;
	}
	
	/**
	 * <p>Constructor good for creating a pattern to find a field that could be defined
	 * on one of many different specified types.</p>
	 *
	 * @param findDeclarations
	 * @param readAccess
	 * @param writeAccess
	 * @param isVar
	 * @param name
	 * @param possibleDeclaringTypes optional list of possible declaring types that the given selector must be
	 * defined on one of to be a valid match, or <code>null</code> to specify the field is not
	 * defined on a type
	 * @param typeQualification
	 * @param typeSimpleName
	 * @param matchRule
	 * @param field
	 */
	public FieldPattern(
		boolean findDeclarations,
		boolean readAccess,
		boolean writeAccess,
		boolean isVar,
		char[] name,
		char[][] possibleDeclaringTypes,
		char[] typeQualification,
		char[] typeSimpleName,
		int matchRule, IField field) {
	
		super(FIELD_PATTERN, findDeclarations, readAccess, writeAccess, name, matchRule,field);
	
		this.isVar=isVar;
		if(possibleDeclaringTypes != null) {
			this.declaringQualification = new char[possibleDeclaringTypes.length][];
			this.declaringSimpleName = new char[possibleDeclaringTypes.length][];
			for(int i = 0; i < possibleDeclaringTypes.length; i++) {
				char[][] seperatedDeclaringType = QualificationHelpers.seperateFullyQualifedName(possibleDeclaringTypes[i]);
				this.declaringQualification[i] = isCaseSensitive() ?
						seperatedDeclaringType[QualificationHelpers.QULIFIERS_INDEX] : CharOperation.toLowerCase(seperatedDeclaringType[QualificationHelpers.QULIFIERS_INDEX]);
				this.declaringSimpleName[i] = isCaseSensitive() ?
						seperatedDeclaringType[QualificationHelpers.SIMPLE_NAMES_INDEX] : CharOperation.toLowerCase(seperatedDeclaringType[QualificationHelpers.SIMPLE_NAMES_INDEX]);
			}
		}
		this.typeQualification = isCaseSensitive() ? typeQualification : CharOperation.toLowerCase(typeQualification);
		this.typeSimpleName = (isCaseSensitive() || isCamelCase())  ? typeSimpleName : CharOperation.toLowerCase(typeSimpleName);
	
		((InternalSearchPattern)this).mustResolve = mustResolve();
	}
	
	/**
	 * 
	 * <p>Constructor useful for searching for a specific field on a specific type.</p>
	 *
	 * @param findDeclarations
	 * @param readAccess
	 * @param writeAccess
	 * @param name
	 * @param declaringQualification
	 * @param declaringSimpleName
	 * @param matchRule
	 */
	public FieldPattern(
			boolean findDeclarations,
			boolean readAccess,
			boolean writeAccess,
			char[] name,
			char[] declaringQualification,
			char[] declaringSimpleName,
			int matchRule) {
		
		this(findDeclarations, readAccess, writeAccess, false,
					name, declaringQualification, declaringSimpleName, null, null, matchRule, null);
	}
	
	/**
	 * <p>Constructor useful for searching for a specific field on a specific type.</p>
	 *
	 * @param findDeclarations
	 * @param readAccess
	 * @param writeAccess
	 * @param isVar
	 * @param name
	 * @param declaringQualification
	 * @param declaringSimpleName
	 * @param typeQualification
	 * @param typeSimpleName
	 * @param matchRule
	 * @param field
	 */
	public FieldPattern(
			boolean findDeclarations,
			boolean readAccess,
			boolean writeAccess,
			boolean isVar,
			char[] name,
			char[] declaringQualification,
			char[] declaringSimpleName,
			char[] typeQualification,
			char[] typeSimpleName,
			int matchRule, IField field) {
	
		super(FIELD_PATTERN, findDeclarations, readAccess, writeAccess, name, matchRule,field);
		
		this.isVar=isVar;
		
		this.setDeclaringQualification(declaringQualification);
		this.setDeclaringSimpleName(declaringSimpleName);
		this.typeQualification = isCaseSensitive() ? typeQualification : CharOperation.toLowerCase(typeQualification);
		this.typeSimpleName = (isCaseSensitive() || isCamelCase())  ? typeSimpleName : CharOperation.toLowerCase(typeSimpleName);
	
		((InternalSearchPattern)this).mustResolve = mustResolve();
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchPattern#decodeIndexKey(char[])
	 */
	public void decodeIndexKey(char[] key) {
		char[][] seperated = CharOperation.splitOn(SEPARATOR, key);
		
		//get the name
		this.name = seperated[0];
		
		if (seperated.length > 1) {
			// get the declaring type
			char[][] declaringType = QualificationHelpers.seperateFullyQualifedName(seperated[1]);
			this.setDeclaringQualification(declaringType[QualificationHelpers.QULIFIERS_INDEX]);
			this.setDeclaringSimpleName(declaringType[QualificationHelpers.SIMPLE_NAMES_INDEX]);

			// get the type of the field
			char[][] type = QualificationHelpers.seperateFullyQualifedName(seperated[2]);
			this.typeQualification = type[QualificationHelpers.QULIFIERS_INDEX];
			this.typeSimpleName = type[QualificationHelpers.SIMPLE_NAMES_INDEX];

			// get the modifiers
			this.modifiers = seperated[3][0] + seperated[3][1];
		}
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.JavaSearchPattern#getBlankPattern()
	 */
	public SearchPattern getBlankPattern() {
		return new FieldPattern(false, false, false, isVar, null, null, null, null, R_EXACT_MATCH | R_CASE_SENSITIVE,null);
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchPattern#getIndexKey()
	 */
	public char[] getIndexKey() {
		return this.name;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.InternalSearchPattern#queryIn(org.eclipse.wst.jsdt.internal.core.index.Index)
	 */
	EntryResult[] queryIn(Index index) throws IOException {
		//might have to do multiple searches
		EntryResult[] results = null;
		char[][] keys = null;
		int[] matchRules = null;
		
		if (this.findReferences && (this.findDeclarations || this.writeAccess)) {
			keys = new char[2][];
			matchRules = new int[2];
		} else {
			keys = new char[1][];
			matchRules = new int[1];
		}
			
		keys[0] = this.name; // can be null
		matchRules[0] = getMatchRule();
		
		if (this.findDeclarations || this.writeAccess) {
			switch (getMatchMode()) {
				case R_EXACT_MATCH :
					// can not do an exact match with camel case
					if (this.isCamelCase)
						break;

					/*
					 * do a prefix match on name/declaringType/
					 */
					keys[0] = CharOperation.concat(this.name, QualificationHelpers.createFullyQualifiedName(this.getDeclaringQualification(), this.getDeclaringSimpleName()), SEPARATOR);
					keys[0] = CharOperation.append(keys[0], SEPARATOR);
					matchRules[0] &= ~R_EXACT_MATCH;
					matchRules[0] |= R_PREFIX_MATCH;
					break;
				case R_PREFIX_MATCH :
					break;
				case R_PATTERN_MATCH :
					keys[0] = createSearchIndexKey(this.name, this.getDeclaringQualification(), this.getDeclaringSimpleName());
					break;
				case R_REGEXP_MATCH :
					Logger.log(Logger.WARNING, "Regular expression matching is not yet implimented for MethodPattern");
					break;
			}
		}
		
		if (this.findReferences && (this.findDeclarations || this.writeAccess)) {
			keys[1] = this.name; // can be null
			matchRules[1] = getMatchRule();
		}
	
		//run a search for each search key
		for (int i = 0; i < keys.length; ++i) {
			//run search
			EntryResult[] additionalResults = index.query(getIndexCategories(), keys[i], matchRules[i]);
			
			//collect results
			if (additionalResults != null && additionalResults.length > 0) {
				if (results == null) {
					results = additionalResults;
				} else {
					EntryResult[] existingResults = results;
					
					results = new EntryResult[existingResults.length + additionalResults.length];
					
					System.arraycopy(existingResults, 0, results, 0, existingResults.length);
					System.arraycopy(additionalResults, 0, results, existingResults.length, additionalResults.length);
				}
			}
		}		
		return results;
	}
	public char[][] getIndexCategories() {
		if (this.findReferences)
			return this.findDeclarations || this.writeAccess ? REF_AND_DECL_CATEGORIES
					: REF_CATEGORIES;
		if (this.findDeclarations)
			return DECL_CATEGORIES;
	
		return CharOperation.NO_CHAR_CHAR;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchPattern#matchesDecodedKey(org.eclipse.wst.jsdt.core.search.SearchPattern)
	 */
	public boolean matchesDecodedKey(SearchPattern decodedPattern) {
		boolean matches = false;
		if(decodedPattern instanceof FieldPattern) {
			FieldPattern pattern = (FieldPattern) decodedPattern;
			
			matches = matchesName(this.name, pattern.name);
			
			if(matches && this.declaringSimpleName != null) {
				boolean foundTypeMatch = false;
				for(int i = 0; i < this.declaringSimpleName.length; i++) {
					if(matchesName(this.declaringQualification[i], pattern.getDeclaringQualification())
								&& matchesName(this.declaringSimpleName[i], pattern.getDeclaringSimpleName())) {
						foundTypeMatch = true;
						break;
					}
				}
				if(!foundTypeMatch)
					matches = false;
			}
		}
		
		return matches;
	}
	
	/**
	 * @return the declaring qualification for this pattern, or <code>null</code> if no declaring qualification
	 */
	public char[] getDeclaringQualification() {
		return this.declaringQualification != null && this.declaringQualification.length > 0 ?
				this.declaringQualification[0] : null;
	}
	
	/**
	 * @return the declaring simple name for this pattern, or <code>null</code> if no declaring simple name
	 */
	public char[] getDeclaringSimpleName() {
		return this.declaringSimpleName != null && this.declaringSimpleName.length > 0 ?
				this.declaringSimpleName[0] : null;
	}
	
	/**
	 * <p>Sets the declaring qualification for this pattern.</p>
	 * <p>If the declaring qualification is set then the declaring simple name must also be set.</p>
	 * 
	 * @param declaringQualification declaring qualification for this pattern
	 * 
	 * @see #setDeclaringSimpleName(char[])
	 */
	private void setDeclaringQualification(char[] declaringQualification) {
		this.declaringQualification = new char[1][];
		this.declaringQualification[0] = declaringQualification;
	}
	
	/**
	 * <p>Sets the declaring simple name for this pattern</p>
	 * 
	 * @param declaringSimpleName declaring simple name for this pattern
	 */
	private void setDeclaringSimpleName(char[] declaringSimpleName) {
		this.declaringSimpleName = new char[1][];
		this.declaringSimpleName[0] = declaringSimpleName;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.VariablePattern#mustResolve()
	 */
	protected boolean mustResolve() {
		if (!isVar && this.findDeclarations)
			return true;
		
		return false;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.JavaSearchPattern#print(java.lang.StringBuffer)
	 */
	protected StringBuffer print(StringBuffer output) {
		if (this.findDeclarations) {
			output.append(this.findReferences
				? "FieldCombinedPattern: " //$NON-NLS-1$
				: "FieldDeclarationPattern: "); //$NON-NLS-1$
		} else {
			output.append("FieldReferencePattern: "); //$NON-NLS-1$
		}
		if (this.getDeclaringQualification() != null) output.append(this.getDeclaringQualification()).append('.');
		if (this.getDeclaringSimpleName() != null)
			output.append(this.getDeclaringSimpleName()).append('.');
		else if (this.getDeclaringSimpleName() != null) output.append("*."); //$NON-NLS-1$
		if (name == null) {
			output.append("*"); //$NON-NLS-1$
		} else {
			output.append(name);
		}
		if (typeQualification != null)
			output.append(" --> ").append(typeQualification).append('.'); //$NON-NLS-1$
		else if (typeSimpleName != null) output.append(" --> "); //$NON-NLS-1$
		if (typeSimpleName != null)
			output.append(typeSimpleName);
		else if (typeQualification != null) output.append("*"); //$NON-NLS-1$
		return super.print(output);
	}
	
	/**
	 * <p>Create an index key for search the index for any field that matches the given selector,
	 * on the optionally defined declaring type.</p>
	 * 
	 * @param name
	 * @param declaringQualification
	 * @param declaringSimpleName
	 * 
	 * @return
	 */
	private static char[] createSearchIndexKey(char[] name,
			char[] declaringQualification, char[] declaringSimpleName) {
		
		char[] declaringFullTypeName = null;
		if(declaringSimpleName != null) {
			declaringFullTypeName = QualificationHelpers.createFullyQualifiedName(declaringQualification, declaringSimpleName);
		}
		
		return createIndexKey(name,
				ONE_STAR,
				declaringFullTypeName != null ? declaringFullTypeName : ONE_STAR,
				0);
	}
}
