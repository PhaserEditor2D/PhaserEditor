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

import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.core.Logger;
import org.eclipse.wst.jsdt.internal.core.index.EntryResult;
import org.eclipse.wst.jsdt.internal.core.index.Index;
import org.eclipse.wst.jsdt.internal.core.util.QualificationHelpers;

/**
 * <p>Pattern used when adding functions to an index or searching an index for functions.</p>
 */
public class MethodPattern extends JavaSearchPattern  {

	protected static final char[][] REF_CATEGORIES = { METHOD_REF };
	protected static final char[][] REF_AND_DECL_CATEGORIES = { METHOD_REF, METHOD_DECL };
	protected static final char[][] DECL_CATEGORIES = { METHOD_DECL };
	protected static final char[][] FUNCTION_REF_AND_DECL_CATEGORIES = { METHOD_REF, FUNCTION_DECL, METHOD_DECL };
	protected static final char[][] FUNCTION_DECL_CATEGORIES = { FUNCTION_DECL, METHOD_DECL };
	
	/**
	 * <p><b>Required</b></p>
	 * 
	 * <p>Name of the function</p>
	 */
	public char[] selector;
	
	/**
	 * <p><b>Optional</b></p>
	 * 
	 * <p>Qualifications of the parameter types for this function.</p>
	 * <p>This should have the same length as {@link #parameterCount}, or
	 * <code>null</code> if {@link #parameterCount} is 0</p>
	 * 
	 * <p><b>Note:</b> If this field is defined then the {@link #parameterSimpleNames} must
	 * also be defined.</p>
	 * 
	 * @see #parameterSimpleNames
	 */
	public char[][] parameterQualifications;
	
	/**
	 * <p><b>Optional</b></p>
	 * 
	 * <p>Simple names of the parameter types for this function.</p>
	 * <p>This should have the same length as {@link #parameterCount}, or
	 * <code>null</code> if {@link #parameterCount} is 0</p>
	 * 
	 * <p><b>Note:</b> If this field is defined then the {@link #parameterQualifications}
	 * filed can be defined, but does not have to be.</p>
	 * 
	 * @see #parameterQualifications
	 */
	public char[][] parameterSimpleNames;
	
	/**
	 * <p><b>Required</b> if {@link #parameterCount} is greater then 0</p>
	 * 
	 * <p>Names of the defined parameters for this function.</p>
	 * <p>This should have the same length as {@link #parameterCount}, or
	 * <code>null</code> if {@link #parameterCount} is 0</p>
	 */
	public char[][] parameterNames;
	
	/**
	 * <p><b>Optional</b></p>
	 * 
	 * <p>Qualification of the return type of this function.</p>
	 * 
	 * <p><b>Note:</b> If this field is defined then the {@link #returnSimpleName} must
	 * also be defined.</p>
	 */
	public char[] returnQualification;
	
	/**
	 * <p><b>Optional</b></p>
	 * 
	 * <p>Simple name of the return type of this function.</p>
	 * 
	 * <p>This can either be a single simple name if this pattern is representing a specific function
	 * defined on a specific type, or it can be a list of simple names if it is a pattern for finding
	 * a function that could be defined on many different types.</p>
	 * 
	 * <p><b>Note:</b> If this field is defined then the {@link #returnQualification}
	 * filed can be defined, but does not have to be.</p>
	 */
	public char[] returnSimpleName;
	
	/**
	 * <p><b>Optional</b></p>
	 * 
	 * <p>Qualification of the declaring type containing this function.</p>
	 * 
	 * <p>This can either be a single qualification if this pattern is representing a specific function
	 * defined on a specific type, or it can be a list of qualifications if it is a pattern for finding
	 * a function that could be defined on many different types.</p>
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
	 * <p>Simple name of the declaring type containing this function.</p>
	 * 
	 * <p>This can either be a single simple name if this pattern is representing a specific function
	 * defined on a specific type, or it can be a list of simple names if it is a pattern for finding
	 * a function that could be defined on many different types.</p>
	 * 
	 * <p><b>Note:</b> If this field is defined then the {@link #declaringQualification}
	 * can be defined, but does not have to be.</p>
	 * 
	 * @see #declaringQualification
	 */
	private char[][] declaringSimpleName;
	
	/**
	 * <p><b>Optional</b></p>
	 * 
	 * <p>Any modifiers for this function.</p>
	 * 
	 * @see ClassFileConstants
	 */
	public int modifiers;
	
	/**
	 * <p>When using this pattern to do a search <code>true</code> to
	 * find function declarations that match this pattern, <code>false</code> otherwise.</p>
	 */
	protected boolean findDeclarations;
	
	/**
	 * <p>When using this pattern to do a search <code>true</code> to
	 * find function references that match this pattern, <code>false</code> otherwise.</p>
	 */
	protected boolean findReferences;
	
	/**
	 * <p><code>true</code> if this pattern represents a function,
	 * <code>false</code> otherwise.</p>
	 * 
	 * <p><b>NOTE:</b> this whole concept should be removed, a function is a function is a function.</p>
	 */
	protected boolean isFunction;

	/**
	 * <p>Internal constructor for creating plank patterns</p>
	 *
	 * @param matchRule match rule used when comparing this pattern to search results
	 * @param isFunction <code>true</code> if this pattern represents a function,
	 * <code>false</code> otherwise
	 */
	MethodPattern(int matchRule, boolean isFunction) {
		super(METHOD_PATTERN, matchRule);
		this.isFunction=isFunction;
	}
	
	/**
	 * <p>Useful constructor when creating a pattern to search for index matches
	 * while doing content assist.</p>
	 *
	 * @param findDeclarations when using this pattern to do a search <code>true</code> to
	 * find function declarations that match this pattern, <code>false</code> otherwise.
	 * @param findReferences hen using this pattern to do a search <code>true</code> to
	 * find function references that match this pattern, <code>false</code> otherwise
	 * @param isFunction <code>true</code> if this pattern represents a function,
	 * <code>false</code> otherwise
	 * @param selector pattern for the name of the function
	 * @param selectorMatchRule match rule used when comparing this pattern to search results.
	 * This dictates what type of pattern is present, if any, in the specified <code>selector</code>
	 * 
	 * @see SearchPattern
	 */
	public MethodPattern(boolean findDeclarations,
			boolean findReferences,
			boolean isFunction,
			char[] selector,
			int selectorMatchRule) {
		
		this(findDeclarations, findReferences, isFunction, selector,
				null, null, null, null, null, null,
				selectorMatchRule);
	}
	
	/**
	 * <p>Useful constructor for finding index matches based on content assist pattern.</p>
	 *
	 * @param findDeclarations when using this pattern to do a search <code>true</code> to
	 * find function declarations that match this pattern, <code>false</code> otherwise.
	 * @param findReferences hen using this pattern to do a search <code>true</code> to
	 * find function references that match this pattern, <code>false</code> otherwise
	 * @param selector pattern for the name of the function
	 * @param possibleDeclaringTypes optional list of possible declaring types that the given selector must be
	 * defined on one of to be a valid match, or <code>null</code> to specify the function is not
	 * defined on a type
	 * @param selectorMatchRule match rule used when comparing this pattern to search results.
	 * This dictates what type of pattern is present, if any, in the specified <code>selector</code>
	 */
	public MethodPattern(boolean findDeclarations, boolean findReferences,
			char[] selector, char[][] possibleDeclaringTypes,
			int selectorMatchRule){
		
		this(selectorMatchRule, true);
		
		this.findDeclarations = findDeclarations;
		this.findReferences = findReferences;
		this.selector = (isCaseSensitive() || isCamelCase())  ? selector : CharOperation.toLowerCase(selector);
		
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
		} else {
			this.declaringQualification = null;
			this.declaringSimpleName = null;
		}
	}
	
	/**
	 * <p>Constructor to create a pattern that accepts all possible information about a function.</p>
	 *
	 * @param findDeclarations
	 * @param findReferences
	 * @param isFunction
	 * @param selector
	 * @param declaringQualification
	 * @param declaringSimpleName
	 * @param returnQualification
	 * @param returnSimpleName
	 * @param parameterQualifications
	 * @param parameterSimpleNames
	 * @param matchRule
	 */
	public MethodPattern(
		boolean findDeclarations,
		boolean findReferences,
		boolean isFunction,
		char[] selector,
		char[][] parameterQualifications,
		char[][] parameterSimpleNames,
		char[] returnQualification,
		char[] returnSimpleName,
		char[] declaringQualification,
		char[] declaringSimpleName,
		int matchRule) {

		this(matchRule,isFunction);

		this.findDeclarations = findDeclarations;
		this.findReferences = findReferences;

		this.selector = (isCaseSensitive() || isCamelCase())  ? selector : CharOperation.toLowerCase(selector);
		this.setDeclaringQualification(isCaseSensitive() ? declaringQualification : CharOperation.toLowerCase(declaringQualification));
		this.setDeclaringSimpleName(isCaseSensitive() ? declaringSimpleName : CharOperation.toLowerCase(declaringSimpleName));
		this.returnQualification = isCaseSensitive() ? returnQualification : CharOperation.toLowerCase(returnQualification);
		this.returnSimpleName = isCaseSensitive() ? returnSimpleName : CharOperation.toLowerCase(returnSimpleName);
		
		if (parameterSimpleNames != null) {
			this.parameterQualifications = new char[parameterSimpleNames.length][];
			this.parameterSimpleNames = new char[parameterSimpleNames.length][];
			for (int i = 0; i < this.parameterSimpleNames.length; i++) {
				this.parameterQualifications[i] = isCaseSensitive() ? parameterQualifications[i] : CharOperation.toLowerCase(parameterQualifications[i]);
				this.parameterSimpleNames[i] = isCaseSensitive() ? parameterSimpleNames[i] : CharOperation.toLowerCase(parameterSimpleNames[i]);
			}
		}
		((InternalSearchPattern)this).mustResolve = false;
	}
	
	/**
	 * <p>Given an index key created by this class decodes that key into the
	 * various fields of this pattern.</p>
	 * 
	 * @param key to decode into the fields of this pattern
	 * 
	 * @see #createIndexKey(char[])
	 * @see #createIndexKey(char[], char[][], char[][], char[], char[], int)
	 * @see #createSearchIndexKey(char[], char[], char[])
	 */
	public void decodeIndexKey(char[] key) {
		char[][] seperated = CharOperation.splitOn(SEPARATOR, key);
		
		//get the selector
		this.selector = seperated[0];
		
		//get parameter names
		char[][] parameterNames = CharOperation.splitOn(PARAMETER_SEPARATOR, seperated[2]);
		if(parameterNames.length > 0) {
			this.parameterNames = parameterNames;
		} else {
			this.parameterNames = null;
		}
		
		//get parameter types
		char[][][] parameterTypes = QualificationHelpers.seperateFullyQualifiedNames(seperated[1], parameterNames.length);
		this.parameterQualifications = parameterTypes[QualificationHelpers.QULIFIERS_INDEX];
		this.parameterSimpleNames = parameterTypes[QualificationHelpers.SIMPLE_NAMES_INDEX];
		
		//get the return type
		char[][] returnType = QualificationHelpers.seperateFullyQualifedName(seperated[3]);
		this.returnQualification = returnType[QualificationHelpers.QULIFIERS_INDEX];
		this.returnSimpleName = returnType[QualificationHelpers.SIMPLE_NAMES_INDEX];
		
		//get the declaration type
		char[][] declaringType = QualificationHelpers.seperateFullyQualifedName(seperated[4]);
		this.setDeclaringQualification(declaringType[QualificationHelpers.QULIFIERS_INDEX]);
		this.setDeclaringSimpleName(declaringType[QualificationHelpers.SIMPLE_NAMES_INDEX]);
		
		//get the modifiers
		this.modifiers = seperated[5][0] + seperated[5][1];
	}
	
	public SearchPattern getBlankPattern() {
		return new MethodPattern(R_EXACT_MATCH | R_CASE_SENSITIVE,isFunction);
	}
	public char[][] getIndexCategories() {
		if (this.findReferences)
			return this.findDeclarations ?
					(isFunction ? FUNCTION_REF_AND_DECL_CATEGORIES : REF_AND_DECL_CATEGORIES)
					: REF_CATEGORIES;
		if (this.findDeclarations)
			return isFunction ? FUNCTION_DECL_CATEGORIES : DECL_CATEGORIES;
		return CharOperation.NO_CHAR_CHAR;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchPattern#matchesDecodedKey(org.eclipse.wst.jsdt.core.search.SearchPattern)
	 */
	public boolean matchesDecodedKey(SearchPattern decodedPattern) {
		boolean matches = false;
		if(decodedPattern instanceof MethodPattern) {
			MethodPattern pattern = (MethodPattern) decodedPattern;
			
			matches = matchesName(this.selector, pattern.selector);
			
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
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.InternalSearchPattern#isPolymorphicSearch()
	 */
	boolean isPolymorphicSearch() {
		return this.findReferences;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.InternalSearchPattern#queryIn(org.eclipse.wst.jsdt.internal.core.index.Index)
	 */
	EntryResult[] queryIn(Index index) throws IOException {
		char[] key = this.selector; // can be null
		int matchRule = getMatchRule();
	
		int matchRuleToUse = matchRule;
		switch(getMatchMode()) {
			case R_EXACT_MATCH :
				if (this.isCamelCase) break;
				key = createSearchIndexKey(this.selector,
						this.getDeclaringQualification(), this.getDeclaringSimpleName());
				matchRuleToUse &= ~R_EXACT_MATCH;
				matchRuleToUse |= R_PATTERN_MATCH;
				break;
			case R_PREFIX_MATCH :
				break;
			case R_PATTERN_MATCH :
				key = createSearchIndexKey(this.selector,
						this.getDeclaringQualification(), this.getDeclaringSimpleName());
				break;
			case R_REGEXP_MATCH :
				Logger.log(Logger.WARNING, "Regular expression matching is not yet implimented for MethodPattern");
				break;
		}
	
		return index.query(getIndexCategories(), key, matchRuleToUse); // match rule is irrelevant when the key is null
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.JavaSearchPattern#print(java.lang.StringBuffer)
	 */
	protected StringBuffer print(StringBuffer output) {
		if (this.findDeclarations) {
			output.append(this.findReferences
				? "MethodCombinedPattern: " //$NON-NLS-1$
				: "MethodDeclarationPattern: "); //$NON-NLS-1$
		} else {
			output.append("MethodReferencePattern: "); //$NON-NLS-1$
		}
		if (this.getDeclaringQualification() != null)
			output.append(this.getDeclaringQualification()).append('.');
		if (this.getDeclaringSimpleName() != null)
			output.append(this.getDeclaringSimpleName()).append('.');
		else if (this.getDeclaringQualification() != null)
			output.append("*."); //$NON-NLS-1$

		if (selector != null)
			output.append(selector);
		else
			output.append("*"); //$NON-NLS-1$
		output.append('(');
		if (parameterSimpleNames == null) {
			output.append("..."); //$NON-NLS-1$
		} else {
			for (int i = 0, max = parameterSimpleNames.length; i < max; i++) {
				if (i > 0) output.append(", "); //$NON-NLS-1$
				if (parameterQualifications[i] != null) output.append(parameterQualifications[i]).append('.');
				if (parameterSimpleNames[i] == null) output.append('*'); else output.append(parameterSimpleNames[i]);
			}
		}
		output.append(')');
		if (returnQualification != null)
			output.append(" --> ").append(returnQualification).append('.'); //$NON-NLS-1$
		else if (returnSimpleName != null)
			output.append(" --> "); //$NON-NLS-1$
		if (returnSimpleName != null)
			output.append(returnSimpleName);
		else if (returnQualification != null)
			output.append("*"); //$NON-NLS-1$
		return super.print(output);
	}
	
	/**
	 * <p>Create an index key from a selector and a parameter count.</p>
	 * 
	 * <p><b>Note</b> Currently used to index function references, but the
	 * validity of this use is questionable.</p>
	 * 
	 * @param selector
	 * 
	 * @return a function index key created from a selector and a parameter count
	 */
	public static char[] createIndexKey(char[] selector) {
		return createIndexKey(selector, null, null, null, null, 0);
	}
	
	/**
	 * <p>Creates an index key based on the given function definition information.</p>
	 * 
	 * <p><b>Key Syntax</b>:
	 * <code>selector/parameterFullTypeNames/paramaterNames/returnFulLTypeName/declaringFullTypeName/modifiers</code></p>
	 * 
	 * <p>
	 * <b>Examples:</b><ul>
	 * <li><code>myFunction////</code> - function with no parameters and no return type</li>
	 * <li><code>myFunction///String/</code> - function with no parameters with a return type</li>
	 * <li><code>myFunction////foo.bar.Type</code> - function on a type with no parameters and no return type</li>
	 * <li><code>myFunction///String/foo.bar.Type</code> - function on a type with no parameters with a return type </li>
	 * <li><code>myFunction//param1,param2//</code> - function with no parameter types, with parameter names with no return type</li>
	 * <li><code>myFunction//param1,param2/String/</code> - function with no parameter types, with parameter names with a return type</li>
	 * <li><code>myFunction//param1,param2//foo.bar.Type</code> - function on a type with no parameter types, with parameter  names with no return type</li>
	 * <li><code>myFunction//param1,param2/String/foo.bar.Type</code> - function on a type with no parameter types, with parameter names with a return type</li>
	 * <li><code>myFunction/String,Number/param1,param2//</code> - function with parameter types and names with no return type</li>
	 * <li><code>myFunction/String,Number/param1,param2/String/</code> - function with parameter types and names with a return type</li>
	 * <li><code>myFunction/String,Number/param1,param2//foo.bar.Type</code> - function on a type with parameter types and names with no return type</li>
	 * <li><code>myFunction/String,Number/param1,param2/String/foo.bar.Type</code> - function on a type with parameter types and names with a return type</li>
	 * <li><code>myFunction/,Number/param1,param2//</code> - function where only one of the parameters has a type</li>
	 * <li><code>myFunction/,Number/param1,param2/String/</code> - function where only one of the parameters has a type with a return type</li>
	 * <li><code>myFunction/,Number/param1,param2//foo.bar.Type</code> - function on a type where only one of the parameters has a type</li>
	 * <li><code>myFunction/,Number/param1,param2/String/foo.bar.Type</code> - function on a type where only one of the parameters has a type with a return type</li>
	 * </ul></p>
	 * 
	 * @param selector
	 * @param parameterFullTypeNames
	 * @param parameterNames
	 * @param declaringFullTypeName
	 * @param returnFullTypeName
	 * @param modifiers
	 * 
	 * @see #decodeIndexKey(char[])
	 * 
	 * @return a key that can be put in an index or used to search an index for functions
	 */
	public static char[] createIndexKey(char[] selector,
			char[][] parameterFullTypeNames,
			char[][] parameterNames,
			char[] declaringFullTypeName,
			char[] returnFullTypeName,
			int modifiers) {
		
		char[] indexKey = null;
		
		if(selector != null  && selector.length > 0) {
			char[] parameterTypesChars = CharOperation.NO_CHAR;
			char[] parameterNamesChars = CharOperation.NO_CHAR;
			
			
			//get param types
			if (parameterFullTypeNames != null) {
				parameterTypesChars = CharOperation.concatWith(parameterFullTypeNames, PARAMETER_SEPARATOR, false);
			}
			
			//get param names
			if (parameterNames != null) {
				parameterNamesChars = CharOperation.concatWith(parameterNames, PARAMETER_SEPARATOR);
			}
				
			//get lengths
			int parameterTypesLength = (parameterTypesChars == null ? 0 : parameterTypesChars.length);
			int parameterNamesLength = (parameterNamesChars == null ? 0 : parameterNamesChars.length);
			int returnTypeLength = (returnFullTypeName == null ? 0 : returnFullTypeName.length);
			int delaringTypeLength = declaringFullTypeName == null ? 0 : declaringFullTypeName.length;
			
			int resultLength = selector.length
					+ 1 + parameterTypesLength
					+ 1 + parameterNamesLength
					+ 1 + returnTypeLength
					+ 1 + delaringTypeLength
					+ 3; //modifiers
			
			//create result char array
			indexKey = new char[resultLength];
			
			//add type name to result
			int pos = 0;
			System.arraycopy(selector, 0, indexKey, pos, selector.length);
			pos += selector.length;
			
			//add param types
			indexKey[pos++] = SEPARATOR;
			if (parameterTypesLength > 0) {
				System.arraycopy(parameterTypesChars, 0, indexKey, pos, parameterTypesLength);
				pos += parameterTypesLength;
			}
			
			//add param names
			indexKey[pos++] = SEPARATOR;
			if (parameterNamesLength > 0) {
				System.arraycopy(parameterNamesChars, 0, indexKey, pos, parameterNamesLength);
				pos += parameterNamesLength;
			}
			
			//add return type
			indexKey[pos++] = SEPARATOR;
			if(returnTypeLength > 0) {
				System.arraycopy(returnFullTypeName, 0, indexKey, pos, returnTypeLength);
				pos += returnTypeLength;
			}
			
			//add declaring type
			indexKey[pos++] = SEPARATOR;
			if(delaringTypeLength > 0) {
				System.arraycopy(declaringFullTypeName, 0, indexKey, pos, delaringTypeLength);
				pos += delaringTypeLength;
			}
			
			//add modifiers
			indexKey[pos++] = SEPARATOR;
			indexKey[pos++] = (char) modifiers;
			indexKey[pos++] = (char) (modifiers>>16);
		}
			
		return indexKey;
	}
	
	/**
	 * <p>Create an index key for search the index for any function that matches the given selector,
	 * on the optionally defined declaring type.</p>
	 * 
	 * @param selector
	 * @param declaringQualification
	 * @param declaringSimpleName
	 * 
	 * @return
	 */
	private static char[] createSearchIndexKey(char[] selector,
			char[] declaringQualification, char[] declaringSimpleName) {
		
		char[] declaringFullTypeName = null;
		if(declaringSimpleName != null) {
			declaringFullTypeName = QualificationHelpers.createFullyQualifiedName(declaringQualification, declaringSimpleName);
		}
		
		return createIndexKey(selector,
				ONE_STAR_CHAR,
				ONE_STAR_CHAR,
				declaringFullTypeName != null ? declaringFullTypeName : ONE_STAR,
				ONE_STAR,
				0);
	}
}
