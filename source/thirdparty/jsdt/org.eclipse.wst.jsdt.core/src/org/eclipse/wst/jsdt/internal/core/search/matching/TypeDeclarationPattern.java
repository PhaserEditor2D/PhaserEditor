/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Michael Spector <spektom@gmail.com>  Bug 242987
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
 * <p>Search pattern used to find and create type declaration index entries.</p>
 */
public class TypeDeclarationPattern extends JavaSearchPattern {	
	/**
	 * <p>Qualification of the type declaration.</p>
	 */
	public char[] qualification;
	
	/**
	 * <p>Simple type name of the type declaration .</p>
	 */
	public char[] simpleName;
	
	/**
	 * <p>Super types of the type declaration.</p>
	 */
	public char[][] superTypes;
	
	/**
	 * @deprecated this will be removed at some point because it does not apply to JavaScript and is not longer used internally
	 */
	public char[][] enclosingTypeNames;
	
	/**
	 * <p><b>Optional</b></p>
	 * 
	 * <p>Any modifiers for this type declaration.</p>
	 * 
	 * @see ClassFileConstants
	 */
	public int modifiers;
	
	/**
	 * <p>Used when searing for type declarations using a given prefix.
	 * This prefix will be used to match on either the {@link TypeDeclarationPattern#qualification}
	 * or the {@link TypeDeclarationPattern#simpleName}.</p>
	 * 
	 * @see #TypeDeclarationPattern(char[], int)
	 * 
	 * @see TypeDeclarationPattern#qualification
	 * @see TypeDeclarationPattern#simpleName
	 */
	private char[] fSearchPrefix;

	/**
	 * <p>Categories to search in for matches in the index.</p>
	 */
	private static char[][] CATEGORIES = { TYPE_DECL };
	
	
	/**
	 * <p>Internal constructor for creating plank patterns</p>
	 *
	 * @param matchRule match rule used when comparing this pattern to search results
	 */
	TypeDeclarationPattern(int matchRule) {
		super(TYPE_DECL_PATTERN, matchRule);
	}
	
	/**
	 * <p>Constructor to use when searching for a type with a specific simple name and qualification.</p>
	 *
	 * @param qualification optional qualification of the type, if not specified then the type
	 * does not have a qualification.
	 * @param simpleName simple type name of the type
	 * @param matchRule match rule used when comparing this pattern to search results
	 */
	public TypeDeclarationPattern(
			char[] qualification,
			char[] simpleName,
			int matchRule) {
	
		this(qualification, simpleName, null, matchRule);
	}
	
	/**
	 * <p>Constructor to create a pattern using qualification, simple name, and super types.</p>
	 *
	 * @param qualification optional qualification of the type, if not specified then the type
	 * does not have a qualification.
	 * @param simpleName simple type name of the type
	 * @param superTypes
	 * @param matchRule match rule used when comparing this pattern to search results
	 */
	public TypeDeclarationPattern(
			char[] qualification,
			char[] simpleName,
			char[][] superTypes,
			int matchRule) {
	
		this(matchRule);
		
		/* if someone past a fully qualified name as the simple name break it up, should not have to do this
		 * else initialize normally
		 */
		if((qualification == null || qualification.length == 0) && CharOperation.contains(DOT, simpleName)) {
			char[][] seperated = QualificationHelpers.seperateFullyQualifedName(simpleName);
			this.qualification = seperated[QualificationHelpers.QULIFIERS_INDEX];
			this.simpleName = seperated[QualificationHelpers.SIMPLE_NAMES_INDEX];
		} else {
			if(qualification != null && qualification.length > 0) {
				this.qualification = qualification;
			} else {
				this.qualification = null;
			}
			this.simpleName = simpleName;
		}
		
		//deal with case sensitive and camel case
		this.qualification = (isCaseSensitive() || isCamelCase()) ? this.qualification : CharOperation.toLowerCase(this.qualification);
		this.simpleName = (isCaseSensitive() || isCamelCase()) ? this.simpleName : CharOperation.toLowerCase(this.simpleName);
		
		
		this.superTypes = superTypes;
	}
	
	/**
	 * <p>Constructor to use when searching for a type declaration based on a given prefix.</p>
	 *
	 * @param searchPrefix to match against either the fully qualified name or simple name of
	 * type declarations
	 * @param matchRule match rule used when comparing this pattern to search results
	 */
	public TypeDeclarationPattern(char[] searchPrefix, int matchRule) {
		this(matchRule);
		
		this.fSearchPrefix = (isCaseSensitive() || isCamelCase()) ? searchPrefix : CharOperation.toLowerCase(searchPrefix);
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchPattern#decodeIndexKey(char[])
	 * @see #createIndexKey(char[], char[][], int)
	 */
	public void decodeIndexKey(char[] key) {
		char[][] seperated = CharOperation.splitOn(SEPARATOR, key);
		
		//decode type name
		this.simpleName = seperated[0];
		this.qualification = seperated[1];
		
		//get super types
		this.superTypes = CharOperation.splitOn(PARAMETER_SEPARATOR, seperated[2]);
		
		//get the modifiers
		this.modifiers = seperated[3][0] + seperated[3][1];
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.JavaSearchPattern#getBlankPattern()
	 */
	public SearchPattern getBlankPattern() {
		return new TypeDeclarationPattern(R_EXACT_MATCH | R_CASE_SENSITIVE);
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.search.SearchPattern#getIndexCategories()
	 */
	public char[][] getIndexCategories() {
		return CATEGORIES;
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
	 * @see org.eclipse.wst.jsdt.core.search.SearchPattern#matchesDecodedKey(org.eclipse.wst.jsdt.core.search.SearchPattern)
	 */
	public boolean matchesDecodedKey(SearchPattern decodedPattern) {
		TypeDeclarationPattern pattern = (TypeDeclarationPattern) decodedPattern;
		char[][] seperatedSearchPrefix = QualificationHelpers.seperateFullyQualifedName(this.fSearchPrefix);
		
		return 
			(
				this.fSearchPrefix != null &&
				(		
					matchesName(this.fSearchPrefix, CharOperation.append(pattern.qualification, '.')) ||
					matchesName(this.fSearchPrefix, pattern.simpleName) ||
					(
					    (
					    	CharOperation.equals(seperatedSearchPrefix[QualificationHelpers.QULIFIERS_INDEX], pattern.qualification, isCaseSensitive) ||
					    	matchesQualificationPattern(seperatedSearchPrefix[QualificationHelpers.QULIFIERS_INDEX], pattern.qualification, isCaseSensitive)
					    ) &&	
						matchesName(seperatedSearchPrefix[QualificationHelpers.SIMPLE_NAMES_INDEX], pattern.simpleName)
					)
				)
			)
			||
			(
				this.fSearchPrefix == null &&
				matchesName(this.qualification, pattern.qualification) &&
				matchesName(this.simpleName, pattern.simpleName)
			)
			||
			(
				this.fSearchPrefix == null && this.superTypes != null && 
				matchesName(this.superTypes, pattern.superTypes)
			);
	}

	/**
	 * @param superTypes
	 * @param patternSuperTypes
	 * @return
	 */
	private boolean matchesName(char[][] superTypes, char[][] patternSuperTypes) {
		for (int i = 0; i < superTypes.length; i++) {
			for (int j = 0; j < patternSuperTypes.length; j++) {
				if (matchesName(this.superTypes[i], patternSuperTypes[j]))
					return true;
			}
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.InternalSearchPattern#queryIn(org.eclipse.wst.jsdt.internal.core.index.Index)
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
			qualificationPattern = this.qualification;
			simpleNamePattern = this.simpleName;
		}
		
		//might have to do multiple searches
		char[][] keys = null;
		int[] matchRules = null;

		switch(getMatchMode()) {
			case R_EXACT_MATCH :
				keys = new char[1][];
				matchRules = new int[1];
				//can not do an exact match with camel case
				if (this.isCamelCase) break;

				/* doing an exact match on the type, but really doing a prefix match in the index for
				 * 		simpleName// or simpleName/qualification/
				 */
				
				if(qualificationPattern == null || qualificationPattern.length == 0) {
					keys[0] = CharOperation.append(simpleNamePattern, SEPARATOR);
				} else {
					keys[0] = CharOperation.concat(simpleNamePattern, qualificationPattern, SEPARATOR);
				}
				
				keys[0] = CharOperation.append(keys[0], SEPARATOR);
				if (superTypes != null)
					keys[0] = CharOperation.concat(keys[0], CharOperation.concatWith(superTypes, PARAMETER_SEPARATOR));

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
					if (superTypes != null)
						keys[0] = CharOperation.concat(keys[0], CharOperation.concatWith(superTypes, PARAMETER_SEPARATOR));
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
				if (superTypes != null) {
					// account for modifier values in #createIndexKey() by appending an extra SEPARATOR and ONE_STAR
					keys[0] = CharOperation.concat(keys[0], SEPARATOR, CharOperation.concatWith(superTypes, PARAMETER_SEPARATOR), SEPARATOR, ONE_STAR);
				}
				else {
					keys[0] = CharOperation.concat(keys[0], ONE_STAR, SEPARATOR);
				}
				matchRules[0] = this.getMatchRule();
				
				break;
			case R_REGEXP_MATCH :
				Logger.log(Logger.WARNING, "Regular expression matching is not implimented by ConstructorDeclarationPattern");
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
		
		return results;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.JavaSearchPattern#print(java.lang.StringBuffer)
	 */
	protected StringBuffer print(StringBuffer output) {
		if (qualification != null)
			output.append(qualification);
		else
			output.append("*"); //$NON-NLS-1$
		
		if (simpleName != null)
			output.append(simpleName);
		else
			output.append("*"); //$NON-NLS-1$
		output.append(">"); //$NON-NLS-1$
		return super.print(output);
	}
	
	/**
	 * <p>Creates a type declaration index key, based on the given information, to be placed in the index.</p>
	 * 
	 * <p><b>Key Syntax</b>:
	 * <code>simpleName/qualification/fullyQualifiedSuperTypeNames/modifiers</code></p>
	 * 
	 * @param qualification optional qualification of the type, if not specified then the type
	 * does not have a qualification.
	 * @param simpleName simple type name of the type
	 * @param fullyQualifiedSuperTypeNames list of fully qualified super type names
	 * @param modifiers Modifiers to the type declaration
	 * 
	 * @return Type declaration index key
	 */
	public static char[] createIndexKey(char[] qualification, char[] simpleName, char[][] fullyQualifiedSuperTypeNames, int modifiers) {
		char[] indexKey = null;
		
		if(simpleName != null) {
			//build list of super types
			char[] fullyQualifiedSuperTypeNamesList = null;
			if(fullyQualifiedSuperTypeNames != null) {
				fullyQualifiedSuperTypeNamesList = CharOperation.concatWith(fullyQualifiedSuperTypeNames, PARAMETER_SEPARATOR);
			} else {
				fullyQualifiedSuperTypeNamesList = CharOperation.NO_CHAR;
			}
			
			//get lengths
			int simpleNameLength = simpleName.length;
			int qualificationLength = qualification == null ? 0 : qualification.length;
			
			//get length
			int keyLength = simpleNameLength
					+ 1 + qualificationLength
					+ 1 + fullyQualifiedSuperTypeNamesList.length
					+ 3; //modifiers
			
			//create result char array
			indexKey = new char[keyLength];
			
			//add simple type name to result
			int pos = 0;
			if (simpleNameLength > 0) {
				System.arraycopy(simpleName, 0, indexKey, pos, simpleNameLength);
				pos += simpleNameLength;
			}
			
			//add qualification to result
			indexKey[pos++] = SEPARATOR;
			if (qualificationLength > 0) {
				System.arraycopy(qualification, 0, indexKey, pos, qualificationLength);
				pos += qualificationLength;
			}
			
			//add super types
			indexKey[pos++] = SEPARATOR;
			if (fullyQualifiedSuperTypeNamesList.length > 0) {
				System.arraycopy(fullyQualifiedSuperTypeNamesList, 0, indexKey, pos, fullyQualifiedSuperTypeNamesList.length);
				pos += fullyQualifiedSuperTypeNamesList.length;
			}
			
			//add modifiers
			indexKey[pos++] = SEPARATOR;
			indexKey[pos++] = (char) modifiers;
			indexKey[pos++] = (char) (modifiers>>16);
		}
		
		return indexKey;
	}
	
	public char[] getSearchPrefix() {
		return fSearchPrefix;
	}
	
}
