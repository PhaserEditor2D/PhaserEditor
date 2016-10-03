/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.internal.core.Logger;
import org.eclipse.wst.jsdt.internal.core.index.EntryResult;
import org.eclipse.wst.jsdt.internal.core.index.Index;
import org.eclipse.wst.jsdt.internal.core.util.QualificationHelpers;

/**
 * <p>Pattern used to find and store constructor declarations.</p>
 */
public class ConstructorPattern extends JavaSearchPattern {
	private final static char[][] REF_CATEGORIES = { CONSTRUCTOR_REF };
	private final static char[][] REF_AND_DECL_CATEGORIES = { CONSTRUCTOR_REF, CONSTRUCTOR_DECL };
	private final static char[][] DECL_CATEGORIES = { CONSTRUCTOR_DECL };
	
	/**
	 * <p><code>true</code> if this pattern should match on constructor declarations, <code>false</code> otherwise.</p>
	 */
	protected boolean findDeclarations;
	
	/**
	 * <p><code>true</code> if this pattern should match on constructor references, <code>false</code> otherwise.</p>
	 */
	protected boolean findReferences;

	/**
	 * <p><b>Optional</b></p>
	 * 
	 * <p>Qualification of the declaring type for this constructor.</p>
	 */
	public char[] declaringQualification;
	
	/**
	 * <p>Simple name of the declaring type for this constructor.</p>
	 */
	public char[] declaringSimpleName;
	
	/**
	 * <p><b>Optional</b></p>
	 * 
	 * <p>Qualifications of the parameter types for this function.</p>
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
	 * <p>Simple names of the parameter types for this constructor.</p>
	 * 
	 * <p><b>Note:</b> If this field is defined then the {@link #parameterQualifications}
	 * filed can be defined, but does not have to be.</p>
	 * 
	 * @see #parameterQualifications
	 */
	public char[][] parameterSimpleNames;
	
	/** <p>names of the parameters</p> */
	public char[][] parameterNames;
	
	/** <p> Modifiers for the constructor</p> */
	public int modifiers;
	
	/**
	 * <p>Used when searing for constructors using a given prefix.
	 * This prefix will be used to match on either the {@link ConstructorPattern#declaringQualification}
	 * or the {@link ConstructorPattern#declaringSimpleName}.</p>
	 * 
	 * @see #ConstructorDeclarationPattern(char[], int)
	 * 
	 * @see ConstructorPattern#declaringQualification
	 * @see ConstructorPattern#declaringSimpleName
	 */
	private char[] fSearchPrefix;

	/**
	 * 
	 * @param matchRule
	 */
	private ConstructorPattern(int matchRule) {
		super(CONSTRUCTOR_PATTERN, matchRule);
		
		this.findDeclarations = true;
		this.findReferences = false;
	}
	
	/**
	 * <p>Constructor to use when the constructor declarations qualification and simple name
	 * are both known.</p>
	 *
	 * @param declaringQualification
	 * @param declaringSimpleName
	 * @param matchRule
	 */
	public ConstructorPattern(char[] declaringQualification, char[] declaringSimpleName, int matchRule) {
		this(matchRule);
		
		this.declaringQualification = (this.isCaseSensitive() || this.isCamelCase()) ?
				declaringQualification : CharOperation.toLowerCase(declaringQualification);
		this.declaringSimpleName = (this.isCaseSensitive() || this.isCamelCase()) ?
				declaringSimpleName : CharOperation.toLowerCase(declaringSimpleName);
	}
	
	/**
	 * <p>Constructor to use when searching for a constructor declaration based on a given prefix.</p>
	 *
	 * @param searchPrefix to match against either the fully qualified name or simple name of
	 * constructor declarations
	 * 
	 * @param matchRule
	 * @deprecated
	 */
	public ConstructorPattern(char[] searchPrefix, int matchRule) {
		this(matchRule);
		
		this.fSearchPrefix = searchPrefix;
	}

	/**
	 * <p>Constructor to use when searching for a constructor declaration based on a given prefix.</p>
	 *
	 * @param searchPrefix to match against either the fully qualified name or simple name of
	 * constructor declarations
	 * @param findDeclarations return matches for declarations
	 * @param findReferences return matches for references
	 * @param matchRule one or more of the rule constants found in org.eclipse.wst.jsdt.core.search.SearchPattern 
	 */
	public ConstructorPattern(char[] searchPrefix, int matchRule, boolean findDeclarations, boolean findReferences) {
		this(matchRule);
		
		this.fSearchPrefix = (isCaseSensitive() || isCamelCase()) ? searchPrefix : CharOperation.toLowerCase(searchPrefix);
		this.findDeclarations = findDeclarations;
		this.findReferences = findReferences;
	}
	
	/**
	 * <p>Constructor to create a pattern that accepts all possible information about a constructor.</p>
	 *
	 * @param findDeclarations
	 * @param findReferences
	 * @param declaringQualification
	 * @param declaringSimpleName
	 * @param parameterQualifications
	 * @param parameterSimpleNames
	 * @param matchRule
	 */
	public ConstructorPattern(
		boolean findDeclarations,
		boolean findReferences,
		char[][] parameterQualifications,
		char[][] parameterSimpleNames,
		char[] declaringQualification,
		char[] declaringSimpleName,
		int matchRule) {

		this(matchRule);

		this.findDeclarations = findDeclarations;
		this.findReferences = findReferences;

		this.declaringQualification = isCaseSensitive() ? declaringQualification : CharOperation.toLowerCase(declaringQualification);
		this.declaringSimpleName =isCaseSensitive() ? declaringSimpleName : CharOperation.toLowerCase(declaringSimpleName);
		
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
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.JavaSearchPattern#getBlankPattern()
	 */
	public SearchPattern getBlankPattern() {
		return new ConstructorPattern(R_EXACT_MATCH | R_CASE_SENSITIVE);
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchPattern#getIndexCategories()
	 */
	public char[][] getIndexCategories() {
		if (this.findReferences)
			return this.findDeclarations ? REF_AND_DECL_CATEGORIES : REF_CATEGORIES;
		if (this.findDeclarations)
			return DECL_CATEGORIES;
		return CharOperation.NO_CHAR_CHAR;
	}
	
	/**
	 * <p>Matches this pattern against another pattern using the following logic:<ul>
	 * 	<li>OR<ul>
	 * 		<li>AND<ul>
	 * 			<li>this pattern has a defined search prefix</li>
	 * 			<li>OR<ul>
	 * 				<li>this pattern's prefix matches the other patterns qualified name</li>
	 * 				<li>this pattern's prefix matches the other patterns simple name</li>
	 * 				<li>AND if after separating this pattern's prefix into a qualifier and simple name<ul>
	 * 					<li>this pattern's prefix qualifier matches the other patterns qualified name</li>
	 * 					<li>this pattern's prefix simple name matches the other patterns simple name</li></ul></li></ul></li></ul></li>
	 * 		<li>AND<ul>
	 * 			<li>this pattern does not have a defined search prefix</li>
	 * 			<li>this pattern's qualified name equals the other patterns qualified name</li>
	 * 			<li>this pattern's simple name equals the other patterns simple name</li></ul></li></ul></li></ul></li></ul></p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.ConstructorPattern#matchesDecodedKey(org.eclipse.wst.jsdt.core.search.SearchPattern)
	 */
	public boolean matchesDecodedKey(SearchPattern decodedPattern) {
		ConstructorPattern pattern = (ConstructorPattern) decodedPattern;
		char[][] seperatedSearchPrefix = QualificationHelpers.seperateFullyQualifedName(this.fSearchPrefix);
		
		return 
			(
				this.fSearchPrefix != null &&
				(
					matchesName(this.fSearchPrefix, pattern.declaringQualification) ||
					matchesName(this.fSearchPrefix, pattern.declaringSimpleName) ||
					(
						(
							CharOperation.equals(seperatedSearchPrefix[QualificationHelpers.QULIFIERS_INDEX], pattern.declaringQualification, isCaseSensitive) ||
							matchesQualificationPattern(seperatedSearchPrefix[QualificationHelpers.QULIFIERS_INDEX], pattern.declaringQualification, isCaseSensitive)
						) &&	
						matchesName(seperatedSearchPrefix[QualificationHelpers.SIMPLE_NAMES_INDEX], pattern.declaringSimpleName)
					)
				)
			) ||
			(
				this.fSearchPrefix == null &&
				matchesName(this.declaringQualification, pattern.declaringQualification) &&
				matchesName(this.declaringSimpleName, pattern.declaringSimpleName)
			);
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.ConstructorPattern#queryIn(org.eclipse.wst.jsdt.internal.core.index.Index)
	 */
	EntryResult[] queryIn(Index index) throws IOException {
		EntryResult[] results = null;
		
		//determine the qualification and simple name patterns to use
		char[] qualificationPattern;
		char[] simpleNamePattern;
		if(this.fSearchPrefix != null) {
			char[][] seperatedSearchPrefix = QualificationHelpers.seperateFullyQualifedName(this.fSearchPrefix);
			qualificationPattern = seperatedSearchPrefix[QualificationHelpers.QULIFIERS_INDEX];
			simpleNamePattern = seperatedSearchPrefix[QualificationHelpers.SIMPLE_NAMES_INDEX];
		} else {
			qualificationPattern = this.declaringQualification;
			simpleNamePattern = this.declaringSimpleName;
		}
		
		//might have to do multiple searches
		char[][] keys = null;
		int[] matchRules = null;

		switch(getMatchMode()) {
			case R_EXACT_MATCH :
			
				/* doing an exact match on the type, but really doing a prefix match in the index for
				 * 		simpleName// or simpleName/qualification/
				 */
				keys = new char[1][];
				matchRules = new int[1];
				//can not do an exact match with camel case
				if (this.isCamelCase) break;
				
				if(qualificationPattern == null || qualificationPattern.length == 0) {
					keys[0] = CharOperation.append(simpleNamePattern, SEPARATOR);
				} else {
					keys[0] = CharOperation.concat(simpleNamePattern, qualificationPattern, SEPARATOR);
				}
				
				keys[0] = CharOperation.append(keys[0], SEPARATOR);
				matchRules[0] = this.getMatchRule();
				matchRules[0] &= ~R_EXACT_MATCH;
				matchRules[0] |= R_PREFIX_MATCH;
				break;
			case R_PREFIX_MATCH :
				if(qualificationPattern != null && qualificationPattern.length > 0) {
					if(simpleNamePattern == null || simpleNamePattern.length == 0) {
						keys = new char[1][];
						matchRules = new int[1];
					} else {
						keys = new char[2][];
						matchRules = new int[2];
						
						/* search just simple name because can not search camel case simple name with qualification:
						 * 		simpleNamePattern
						 */
						keys[1] = simpleNamePattern;
						matchRules[1] = this.getMatchRule();
					}
					
					/* do a pattern search using the entire pattern as the qualification:
					 * 		* /fSearchPrefix*
					 */
					char[] trimmedPrefix = this.fSearchPrefix;
					if(this.fSearchPrefix != null && this.fSearchPrefix[this.fSearchPrefix.length - 1] == DOT) {
						trimmedPrefix = CharOperation.subarray(this.fSearchPrefix, 0, this.fSearchPrefix.length - 1);
					}
					keys[0] = CharOperation.concat(ONE_STAR, trimmedPrefix, SEPARATOR);
					keys[0] = CharOperation.concat(keys[0], ONE_STAR);
					matchRules[0] = this.getMatchRule();
					matchRules[0] &= ~R_PREFIX_MATCH;
					matchRules[0] |= R_PATTERN_MATCH;
				} else {
					if(simpleNamePattern == null || simpleNamePattern.length == 0) {
						keys = new char[1][];
						matchRules = new int[1];
					} else {
						keys = new char[2][];
						matchRules = new int[2];
						
						/* first key to search for is using the simple name as the simple name using prefix match:
						 * 		simpleNamePattern
						 */
						keys[1] = simpleNamePattern;
						matchRules[1] = this.getMatchRule();
					}
					
					/* second key to search for is using the simple name as the qualifier using a pattern match
					 * 		* /simpleNamePattern*
					 */
					keys[0] = CharOperation.concat(ONE_STAR, simpleNamePattern, SEPARATOR);
					keys[0] = CharOperation.concat(keys[0], ONE_STAR);
					matchRules[0] = this.getMatchRule();
					matchRules[0] &= ~R_PREFIX_MATCH;
					matchRules[0] |= R_PATTERN_MATCH;
				}
				
				break;
			case R_PATTERN_MATCH :
				/* create the pattern:
				 * 		simpleNamePattern/qualificationPattern/*
				 */
				if (this.fSearchPrefix != null) {
					keys = new char[2][];
					matchRules = new int[2];
					
					/* Key to search for is using the entire pattern as the qualification:
					 * 		* /fSearchPrefix/*
					 */
					keys[1] = CharOperation.concat(ONE_STAR, this.fSearchPrefix, SEPARATOR);
					keys[1] = CharOperation.concat(keys[1], ONE_STAR, SEPARATOR);
					matchRules[1] = this.getMatchRule();
				} else {
					keys = new char[1][];
					matchRules = new int[1];
				}

				//if no simple name use *
				if (simpleNamePattern == null || simpleNamePattern.length == 0) {
					simpleNamePattern = ONE_STAR;
				}

				//if no qualification use *
				if (qualificationPattern == null || qualificationPattern.length == 0) {
					qualificationPattern = ONE_STAR;
				}

				/* Key to search for is using the simple name of the pattern as the simple name
				 * 		simpleNamePattern/qualificationPattern/*
				 */
				keys[0] = CharOperation.concat(simpleNamePattern, qualificationPattern, SEPARATOR);
				keys[0] = CharOperation.concat(keys[0], ONE_STAR, SEPARATOR);
				matchRules[0] = this.getMatchRule();
				
				break;
			case R_REGEXP_MATCH :
				Logger.log(Logger.WARNING, "Regular expression matching is not implimented by ConstructorPattern");
				break;
		}
		
		//run a search for each search key
		for(int i = 0; i < keys.length; ++i) {
			//run search
			EntryResult[] additionalResults = index.query(getIndexCategories(), keys[i], matchRules[i]);
			
			//collect results
			if(additionalResults != null && additionalResults.length > 0) {
				if(results == null) {
					results = additionalResults;
				} else {
					EntryResult[] existingResults = results;
					
					results = new EntryResult[existingResults.length + additionalResults.length];
					
					System.arraycopy(existingResults, 0, results, 0, existingResults.length);
					System.arraycopy(additionalResults, 0, results, existingResults.length, additionalResults.length);
				}
			}
		}
		
		// remove duplicates
		int duplicateCount = 0;
		for(int i = 0; results != null && i < results.length - 1; i++) {
			for(int j = i + 1; j < results.length; j++) {
				if(results [i] != null && results[j] != null && CharOperation.equals(results[i].getWord(), results[j].getWord())) {
					results[j] = null;
					duplicateCount++;
				}
			}
		}
		
		EntryResult[] uniqueResults = null;
		if(duplicateCount > 0) {
			uniqueResults = new EntryResult[results.length - duplicateCount];
			int uniqueIndex = 0;
			for(int i = 0; i < results.length; i++) {
				if(results [i] != null) {
					uniqueResults[uniqueIndex] = results[i];
					uniqueIndex++;
				}
			}	
			results = uniqueResults;
		}
		
		return results;
	}
	
	/**
	 * <p>Decodes an index key made with {@link #createDeclarationIndexKey(char[], int, char[][], char[][], int)} into the
	 * parameters of this pattern.</p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.ConstructorPattern#decodeIndexKey(char[])
	 * 
	 * @see #createDeclarationIndexKey(char[], int, char[][], char[][], int)
	 */
	public void decodeIndexKey(char[] key) {
		char[][] seperated = CharOperation.splitOn(SEPARATOR, key);
		
		//decode type name
		this.declaringSimpleName = seperated[0];
		this.declaringQualification = seperated[1];
		
		//get parameter names
		this.parameterNames  = CharOperation.splitOn(PARAMETER_SEPARATOR, seperated[3]);
		
		// decode parameter types
		char[][][] seperatedParamTypeNames = QualificationHelpers.seperateFullyQualifiedNames(seperated[2], parameterNames.length);
		this.parameterQualifications = seperatedParamTypeNames[QualificationHelpers.QULIFIERS_INDEX];
		this.parameterSimpleNames = seperatedParamTypeNames[QualificationHelpers.SIMPLE_NAMES_INDEX];
		
		// decode function modifiers
		this.modifiers = seperated[4][0] + seperated[4][1];
	}
	
	/**
	 * @return the selector for the constructor
	 */
	public char[] getSelector() {
		return QualificationHelpers.createFullyQualifiedName(this.declaringQualification, this.declaringSimpleName);
	}
	
	/**
	 * @return the fully qualified type names for the parameters
	 */
	public char[][] getFullyQualifiedParameterTypeNames() {
		return QualificationHelpers.createFullyQualifiedNames(this.parameterQualifications, this.parameterSimpleNames);
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.JavaSearchPattern#print(java.lang.StringBuffer)
	 */
	protected StringBuffer print(StringBuffer output) {
		if (this.findDeclarations) {
			output.append(this.findReferences
				? "ConstructorCombinedPattern: " //$NON-NLS-1$
				: "ConstructorDeclarationPattern: "); //$NON-NLS-1$
		} else {
			output.append("ConstructorReferencePattern: "); //$NON-NLS-1$
		}
		if (declaringQualification != null)
			output.append(declaringQualification).append('.');
		if (declaringSimpleName != null)
			output.append(declaringSimpleName);
		else if (declaringQualification != null)
			output.append("*"); //$NON-NLS-1$

		output.append('(');
		char[][] parameterTypeNames = this.getFullyQualifiedParameterTypeNames();
		if (parameterTypeNames == null) {
			output.append("..."); //$NON-NLS-1$
		} else {
			for (int i = 0, max = parameterTypeNames.length; i < max; i++) {
				if (i > 0) output.append(", "); //$NON-NLS-1$
				if (parameterTypeNames[i] != null) output.append(parameterTypeNames[i]).append('.');
			}
		}
		output.append(')');
		return super.print(output);
	}
	
	/**
	 * <p>Creates a constructor index key based on the given information to be placed in the index.</p>
	 * 
	 * <p><b>Key Syntax</b>:
	 * <code>typeSimpleName/typeQualification/parameterFullTypeNames/paramaterNames/modifiers</code></p>
	 * 
	 * @param typeName Name of the type the constructor is for
	 * @param parameterTypes Type names of the parameters, should be same length as <code>parameterCount</code>
	 * @param parameterNames Names of the parameters, should be same length as <code>parameterCount</code>
	 * @param modifiers Modifiers to the constructor such as public/private
	 * 
	 * @return Constructor index key based on the given information to be used in an index
	 */
	public static char[] createIndexKey(
			char[] typeName,
			char[][] parameterTypes,
			char[][] parameterNames,
			int modifiers) {
		
		char[] parameterTypesChars = null;
		char[] parameterNamesChars = null;
		
		//separate type name
		char[][] seperatedTypeName = QualificationHelpers.seperateFullyQualifedName(typeName);
		char[] qualification = seperatedTypeName[QualificationHelpers.QULIFIERS_INDEX];
		char[] simpleName = seperatedTypeName[QualificationHelpers.SIMPLE_NAMES_INDEX];
		
		//get param types
		if (parameterTypes != null) {
			parameterTypesChars = CharOperation.concatWith(parameterTypes, PARAMETER_SEPARATOR, false);
		}
		
		//get param names
		if (parameterNames != null) {
			parameterNamesChars = CharOperation.concatWith(parameterNames, PARAMETER_SEPARATOR);
		}
		
		//get lengths
		int simpleNameLength = simpleName == null ? 0 : simpleName.length;
		int qualificationLength = qualification == null ? 0 : qualification.length;
		int parameterTypesLength = (parameterTypesChars == null ? 0 : parameterTypesChars.length);
		int parameterNamesLength = (parameterNamesChars == null ? 0 : parameterNamesChars.length);
		
		int resultLength = simpleNameLength
				+ 1 + qualificationLength
				+ 1 + parameterTypesLength
				+ 1 + parameterNamesLength
				+ 3; //modifiers
		
		//create result char array
		char[] result = new char[resultLength];
		
		//add simple type name to result
		int pos = 0;
		if (simpleNameLength > 0) {
			System.arraycopy(simpleName, 0, result, pos, simpleNameLength);
			pos += simpleNameLength;
		}
		
		//add qualification to result
		result[pos++] = SEPARATOR;
		if (qualificationLength > 0) {
			System.arraycopy(qualification, 0, result, pos, qualificationLength);
			pos += qualificationLength;
		}
		
		//add param types
		result[pos++] = SEPARATOR;
		if (parameterTypesLength > 0) {
			System.arraycopy(parameterTypesChars, 0, result, pos, parameterTypesLength);
			
			pos += parameterTypesLength;
		}
		
		//add param names
		result[pos++] = SEPARATOR;
		if (parameterNamesLength > 0) {
			System.arraycopy(parameterNamesChars, 0, result, pos, parameterNamesLength);
			pos += parameterNamesLength;
		}
		
		//add modifiers
		result[pos++] = SEPARATOR;
		result[pos++] = (char) modifiers;
		result[pos++] = (char) (modifiers>>16);
		
		return result;
	}

	public char[] getSearchPrefix() {
		return fSearchPrefix;
	}
}
