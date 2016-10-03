/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.search;

import java.util.regex.Pattern;

import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IImportDeclaration;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.parser.Scanner;
import org.eclipse.wst.jsdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.wst.jsdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.wst.jsdt.internal.core.LocalVariable;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;
import org.eclipse.wst.jsdt.internal.core.search.matching.ConstructorPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.FieldPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.InternalSearchPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.LocalVariablePattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.MatchLocator;
import org.eclipse.wst.jsdt.internal.core.search.matching.MethodPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.OrPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.PackageDeclarationPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.PackageReferencePattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.SuperTypeReferencePattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.TypeDeclarationPattern;
import org.eclipse.wst.jsdt.internal.core.search.matching.TypeReferencePattern;
import org.eclipse.wst.jsdt.internal.core.util.QualificationHelpers;


/**
 * A search pattern defines how search results are found. Use <code>SearchPattern.createPattern</code>
 * to create a search pattern.
 * <p>
 * Search patterns are used during the search phase to decode index entries that were added during the indexing phase
 * (see {@link SearchDocument#addIndexEntry(char[], char[])}). When an index is queried, the
 * index categories and keys to consider are retrieved from the search pattern using {@link #getIndexCategories()} and
 * {@link #getIndexKey()}, as well as the match rule (see {@link #getMatchRule()}). A blank pattern is
 * then created (see {@link #getBlankPattern()}). This blank pattern is used as a record as follows.
 * For each index entry in the given index categories and that starts with the given key, the blank pattern is fed using
 * {@link #decodeIndexKey(char[])}. The original pattern is then asked if it matches the decoded key using
 * {@link #matchesDecodedKey(SearchPattern)}. If it matches, a search doument is created for this index entry
 * using {@link SearchParticipant#getDocument(String)}.
 *
 * </p><p>
 * This class is intended to be subclassed by clients. A default behavior is provided for each of the methods above, that
 * clients can ovveride if they wish.
 * </p>
 * @see #createPattern(org.eclipse.wst.jsdt.core.IJavaScriptElement, int)
 * @see #createPattern(String, int, int, int)
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public abstract class SearchPattern extends InternalSearchPattern {

	// Rules for pattern matching: (exact, prefix, pattern) [ | case sensitive]
	/**
	 * Match rule: The search pattern matches exactly the search result,
	 * that is, the source of the search result equals the search pattern.
	 */
	public static final int R_EXACT_MATCH = 0;

	/**
	 * Match rule: The search pattern is a prefix of the search result.
	 */
	public static final int R_PREFIX_MATCH = 0x0001;

	/**
	 * Match rule: The search pattern contains one or more wild cards ('*' or '?').
	 * A '*' wild-card can replace 0 or more characters in the search result.
	 * A '?' wild-card replaces exactly 1 character in the search result.
	 */
	public static final int R_PATTERN_MATCH = 0x0002;

	/**
	 * Match rule: The search pattern contains a regular expression.
	 */
	public static final int R_REGEXP_MATCH = 0x0004;

	/**
	 * Match rule: The search pattern matches the search result only if cases are the same.
	 * Can be combined to previous rules, e.g. {@link #R_EXACT_MATCH} | {@link #R_CASE_SENSITIVE}
	 */
	public static final int R_CASE_SENSITIVE = 0x0008;

	/**
	 * Match rule: The search pattern matches search results as raw/parameterized types/methods with same erasure.
	 * This mode has no effect on other javascript elements search.<br>
	 * Type search example:
	 * 	<ul>
	 * 	<li>pattern: <code>List&lt;Exception&gt;</code></li>
	 * 	<li>match: <code>List&lt;Object&gt;</code></li>
	 * 	</ul>
	 * Method search example:
	 * 	<ul>
	 * 	<li>declaration: <code>&lt;T&gt;foo(T t)</code></li>
	 * 	<li>pattern: <code>&lt;Exception&gt;foo(new Exception())</code></li>
	 * 	<li>match: <code>&lt;Object&gt;foo(new Object())</code></li>
	 * 	</ul>
	 * Can be combined to all other match rules, e.g. {@link #R_CASE_SENSITIVE} | {@link #R_ERASURE_MATCH}
	 * This rule is not activated by default, so raw types or parameterized types with same erasure will not be found
	 * for pattern List&lt;String&gt;,
	 * Note that with this pattern, the match selection will be only on the erasure even for parameterized types.
	 *  
	 */
	public static final int R_ERASURE_MATCH = 0x0010;

	/**
	 * Match rule: The search pattern matches search results as raw/parameterized types/methods with equivalent type parameters.
	 * This mode has no effect on other javascript elements search.<br>
	 * Type search example:
	 * <ul>
	 * 	<li>pattern: <code>List&lt;Exception&gt;</code></li>
	 * 	<li>match:
	 * 		<ul>
	 * 		<li><code>List&lt;? extends Throwable&gt;</code></li>
	 * 		<li><code>List&lt;? super RuntimeException&gt;</code></li>
	 * 		<li><code>List&lt;?&gt;</code></li>
	 *			</ul>
	 * 	</li>
	 * 	</ul>
	 * Method search example:
	 * 	<ul>
	 * 	<li>declaration: <code>&lt;T&gt;foo(T t)</code></li>
	 * 	<li>pattern: <code>&lt;Exception&gt;foo(new Exception())</code></li>
	 * 	<li>match:
	 * 		<ul>
	 * 		<li><code>&lt;? extends Throwable&gt;foo(new Exception())</code></li>
	 * 		<li><code>&lt;? super RuntimeException&gt;foo(new Exception())</code></li>
	 * 		<li><code>foo(new Exception())</code></li>
	 *			</ul>
	 * 	</ul>
	 * Can be combined to all other match rules, e.g. {@link #R_CASE_SENSITIVE} | {@link #R_EQUIVALENT_MATCH}
	 * This rule is not activated by default, so raw types or equivalent parameterized types will not be found
	 * for pattern List&lt;String&gt;,
	 * This mode is overridden by {@link  #R_ERASURE_MATCH} as erasure matches obviously include equivalent ones.
	 * That means that pattern with rule set to {@link #R_EQUIVALENT_MATCH} | {@link  #R_ERASURE_MATCH}
	 * will return same results than rule only set with {@link  #R_ERASURE_MATCH}.
	 *  
	 */
	public static final int R_EQUIVALENT_MATCH = 0x0020;

	/**
	 * Match rule: The search pattern matches exactly the search result,
	 * that is, the source of the search result equals the search pattern.
	 *  
	 */
	public static final int R_FULL_MATCH = 0x0040;

	/**
	 * Match rule: The search pattern contains a Camel Case expression.
	 * <br>
	 * Examples:
	 * <ul>
	 * 	<li><code>NPE</code> type string pattern will match
	 * 		<code>NullPointerException</code> and <code>NpPermissionException</code> types,</li>
	 * 	<li><code>NuPoEx</code> type string pattern will only match
	 * 		<code>NullPointerException</code> type.</li>
	 * </ul>
	 * @see CharOperation#camelCaseMatch(char[], char[]) for a detailed explanation
	 * of Camel Case matching.
	 *<br>
	 * Can be combined to {@link #R_PREFIX_MATCH} match rule. For example,
	 * when prefix match rule is combined with Camel Case match rule,
	 * <code>"nPE"</code> pattern will match <code>nPException</code>.
	 *<br>
	 * Match rule {@link #R_PATTERN_MATCH} may also be combined but both rules
	 * will not be used simultaneously as they are mutually exclusive.
	 * Used match rule depends on whether string pattern contains specific pattern
	 * characters (e.g. '*' or '?') or not. If it does, then only Pattern match rule
	 * will be used, otherwise only Camel Case match will be used.
	 * For example, with <code>"NPE"</code> string pattern, search will only use
	 * Camel Case match rule, but with <code>N*P*E*</code> string pattern, it will
	 * use only Pattern match rule.
	 *
	 *  
	 */
	public static final int R_CAMELCASE_MATCH = 0x0080;

	private static final int MODE_MASK = R_EXACT_MATCH | R_PREFIX_MATCH | R_PATTERN_MATCH | R_REGEXP_MATCH;

	private int matchRule;

/**
 * Creates a search pattern with the rule to apply for matching index keys.
 * It can be exact match, prefix match, pattern match or regexp match.
 * Rule can also be combined with a case sensitivity flag.
 *
 * @param matchRule one of {@link #R_EXACT_MATCH}, {@link #R_PREFIX_MATCH}, {@link #R_PATTERN_MATCH},
 * 	{@link #R_REGEXP_MATCH}, {@link #R_CAMELCASE_MATCH} combined with one of following values:
 * 	{@link #R_CASE_SENSITIVE}, {@link #R_ERASURE_MATCH} or {@link #R_EQUIVALENT_MATCH}.
 *		e.g. {@link #R_EXACT_MATCH} | {@link #R_CASE_SENSITIVE} if an exact and case sensitive match is requested,
 *		{@link #R_PREFIX_MATCH} if a prefix non case sensitive match is requested or {@link #R_EXACT_MATCH} | {@link #R_ERASURE_MATCH}
 *		if a non case sensitive and erasure match is requested.<br>
 * 	Note that {@link #R_ERASURE_MATCH} or {@link #R_EQUIVALENT_MATCH} have no effect
 * 	on non-generic types/methods search.<br>
 * 	Note also that default behavior for generic types/methods search is to find exact matches.
 */
public SearchPattern(int matchRule) {
	this.matchRule = matchRule;
	// Set full match implicit mode
	if ((matchRule & (R_EQUIVALENT_MATCH | R_ERASURE_MATCH )) == 0) {
		this.matchRule |= R_FULL_MATCH;
	}
}

/**
 * Answers true if the pattern matches the given name using CamelCase rules, or false otherwise.
 * CamelCase matching does NOT accept explicit wild-cards '*' and '?' and is inherently case sensitive.
 * <br>
 * CamelCase denotes the convention of writing compound names without spaces, and capitalizing every term.
 * This function recognizes both upper and lower CamelCase, depending whether the leading character is capitalized
 * or not. The leading part of an upper CamelCase pattern is assumed to contain a sequence of capitals which are appearing
 * in the matching name; e.g. 'NPE' will match 'NullPointerException', but not 'NewPerfData'. A lower CamelCase pattern
 * uses a lowercase first character. In Java, type names follow the upper CamelCase convention, whereas method or field
 * names follow the lower CamelCase convention.
 * <br>
 * The pattern may contain lowercase characters, which will be match in a case sensitive way. These characters must
 * appear in sequence in the name. For instance, 'NPExcep' will match 'NullPointerException', but not 'NullPointerExCEPTION'
 * or 'NuPoEx' will match 'NullPointerException', but not 'NoPointerException'.
 * <br><br>
 * Examples:
 * <ol>
 * <li><pre>
 *    pattern = "NPE"
 *    name = NullPointerException / NoPermissionException
 *    result => true
 * </pre>
 * </li>
 * <li><pre>
 *    pattern = "NuPoEx"
 *    name = NullPointerException
 *    result => true
 * </pre>
 * </li>
 * <li><pre>
 *    pattern = "npe"
 *    name = NullPointerException
 *    result => false
 * </pre>
 * </li>
 * </ol>
 * @see CharOperation#camelCaseMatch(char[], char[])
 * 	Implementation has been entirely copied from this method except for array lengthes
 * 	which were obviously replaced with calls to {@link String#length()}.
 *
 * @param pattern the given pattern
 * @param name the given name
 * @return true if the pattern matches the given name, false otherwise
 *  
 */
public static final boolean camelCaseMatch(String pattern, String name) {
	if (pattern == null)
		return true; // null pattern is equivalent to '*'
	if (name == null)
		return false; // null name cannot match

	return camelCaseMatch(pattern, 0, pattern.length(), name, 0, name.length());
}

/**
 * Answers true if a sub-pattern matches the subpart of the given name using CamelCase rules, or false otherwise.
 * CamelCase matching does NOT accept explicit wild-cards '*' and '?' and is inherently case sensitive.
 * Can match only subset of name/pattern, considering end positions as non-inclusive.
 * The subpattern is defined by the patternStart and patternEnd positions.
 * <br>
 * CamelCase denotes the convention of writing compound names without spaces, and capitalizing every term.
 * This function recognizes both upper and lower CamelCase, depending whether the leading character is capitalized
 * or not. The leading part of an upper CamelCase pattern is assumed to contain a sequence of capitals which are appearing
 * in the matching name; e.g. 'NPE' will match 'NullPointerException', but not 'NewPerfData'. A lower CamelCase pattern
 * uses a lowercase first character. In Java, type names follow the upper CamelCase convention, whereas method or field
 * names follow the lower CamelCase convention.
 * <br>
 * The pattern may contain lowercase characters, which will be match in a case sensitive way. These characters must
 * appear in sequence in the name. For instance, 'NPExcep' will match 'NullPointerException', but not 'NullPointerExCEPTION'
 * or 'NuPoEx' will match 'NullPointerException', but not 'NoPointerException'.
 * <br><br>
 * Examples:
 * <ol>
 * <li><pre>
 *    pattern = "NPE"
 *    patternStart = 0
 *    patternEnd = 3
 *    name = NullPointerException
 *    nameStart = 0
 *    nameEnd = 20
 *    result => true
 * </pre>
 * </li>
 * <li><pre>
 *    pattern = "NPE"
 *    patternStart = 0
 *    patternEnd = 3
 *    name = NoPermissionException
 *    nameStart = 0
 *    nameEnd = 21
 *    result => true
 * </pre>
 * </li>
 * <li><pre>
 *    pattern = "NuPoEx"
 *    patternStart = 0
 *    patternEnd = 6
 *    name = NullPointerException
 *    nameStart = 0
 *    nameEnd = 20
 *    result => true
 * </pre>
 * </li>
 * <li><pre>
 *    pattern = "NuPoEx"
 *    patternStart = 0
 *    patternEnd = 6
 *    name = NoPermissionException
 *    nameStart = 0
 *    nameEnd = 21
 *    result => false
 * </pre>
 * </li>
 * <li><pre>
 *    pattern = "npe"
 *    patternStart = 0
 *    patternEnd = 3
 *    name = NullPointerException
 *    nameStart = 0
 *    nameEnd = 20
 *    result => false
 * </pre>
 * </li>
 * </ol>
 * @see CharOperation#camelCaseMatch(char[], int, int, char[], int, int)
 * 	Implementation has been entirely copied from this method except for array lengthes
 * 	which were obviously replaced with calls to {@link String#length()} and
 * 	for array direct access which were replaced with calls to {@link String#charAt(int)}.
 *
 * @param pattern the given pattern
 * @param patternStart the start index of the pattern, inclusive
 * @param patternEnd the end index of the pattern, exclusive
 * @param name the given name
 * @param nameStart the start index of the name, inclusive
 * @param nameEnd the end index of the name, exclusive
 * @return true if a sub-pattern matches the subpart of the given name, false otherwise
 *  
 */
public static final boolean camelCaseMatch(String pattern, int patternStart, int patternEnd, String name, int nameStart, int nameEnd) {
	if (name == null)
		return false; // null name cannot match
	if (pattern == null)
		return true; // null pattern is equivalent to '*'
	if (patternEnd < 0) 	patternEnd = pattern.length();
	if (nameEnd < 0) nameEnd = name.length();

	if (patternEnd <= patternStart) return nameEnd <= nameStart;
	if (nameEnd <= nameStart) return false;
	// check first pattern char
	if (name.charAt(nameStart) != pattern.charAt(patternStart)) {
		// first char must strictly match (upper/lower)
		return false;
	}

	char patternChar, nameChar;
	int iPattern = patternStart;
	int iName = nameStart;

	// Main loop is on pattern characters
	while (true) {

		iPattern++;
		iName++;

		if (iPattern == patternEnd) {
			// We have exhausted pattern, so it's a match
			return true;
		}

		if (iName == nameEnd){
			// We have exhausted name (and not pattern), so it's not a match
			return false;
		}

		// For as long as we're exactly matching, bring it on (even if it's a lower case character)
		if ((patternChar = pattern.charAt(iPattern)) == name.charAt(iName)) {
			continue;
		}

		// If characters are not equals, then it's not a match if patternChar is lowercase
		if (patternChar < ScannerHelper.MAX_OBVIOUS) {
			if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[patternChar] & ScannerHelper.C_UPPER_LETTER) == 0) {
				return false;
			}
		}
		else if (Character.isJavaIdentifierPart(patternChar) && !Character.isUpperCase(patternChar)) {
			return false;
		}

		// patternChar is uppercase, so let's find the next uppercase in name
		while (true) {
			if (iName == nameEnd){
	            //	We have exhausted name (and not pattern), so it's not a match
				return false;
			}

			nameChar = name.charAt(iName);

			if (nameChar < ScannerHelper.MAX_OBVIOUS) {
				if ((ScannerHelper.OBVIOUS_IDENT_CHAR_NATURES[nameChar] & (ScannerHelper.C_LOWER_LETTER | ScannerHelper.C_SPECIAL | ScannerHelper.C_DIGIT)) != 0) {
					// nameChar is lowercase
					iName++;
				// nameChar is uppercase...
				} else  if (patternChar != nameChar) {
					//.. and it does not match patternChar, so it's not a match
					return false;
				} else {
					//.. and it matched patternChar. Back to the big loop
					break;
				}
			}
			else if (Character.isJavaIdentifierPart(nameChar) && !Character.isUpperCase(nameChar)) {
				// nameChar is lowercase
				iName++;
			// nameChar is uppercase...
			} else  if (patternChar != nameChar) {
				//.. and it does not match patternChar, so it's not a match
				return false;
			} else {
				//.. and it matched patternChar. Back to the big loop
				break;
			}
		}
		// At this point, either name has been exhausted, or it is at an uppercase letter.
		// Since pattern is also at an uppercase letter
	}
}

/**
 * Returns a search pattern that combines the given two patterns into an
 * "and" pattern. The search result will match both the left pattern and
 * the right pattern.
 *
 * @param leftPattern the left pattern
 * @param rightPattern the right pattern
 * @return an "and" pattern
 */
public static SearchPattern createAndPattern(SearchPattern leftPattern, SearchPattern rightPattern) {
	return MatchLocator.createAndPattern(leftPattern, rightPattern);
}

/**
 * Field pattern are formed by [declaringType.]name[ type]
 * e.g. java.lang.String.serialVersionUID long
 *		field*
 */
private static SearchPattern createFieldPattern(String patternString, int limitTo, int matchRule,boolean isVar) {

	Scanner scanner = new Scanner(false /*comment*/, true /*whitespace*/, false /*nls*/, ClassFileConstants.JDK1_3/*sourceLevel*/, null /*taskTags*/, null/*taskPriorities*/, true/*taskCaseSensitive*/);
	scanner.setSource(patternString.toCharArray());
	final int InsideDeclaringPart = 1;
	final int InsideType = 2;
	int lastToken = -1;

	String declaringType = null, fieldName = null;
	String type = null;
	int mode = InsideDeclaringPart;
	int token;
	try {
		token = scanner.getNextToken();
	} catch (InvalidInputException e) {
		return null;
	}
	while (token != TerminalTokens.TokenNameEOF) {
		switch(mode) {
			// read declaring type and fieldName
			case InsideDeclaringPart :
				switch (token) {
					case TerminalTokens.TokenNameDOT:
						if (declaringType == null) {
							if (fieldName == null) return null;
							declaringType = fieldName;
						} else {
							String tokenSource = scanner.getCurrentTokenString();
							declaringType += tokenSource + fieldName;
						}
						fieldName = null;
						break;
					case TerminalTokens.TokenNameWHITESPACE:
						if (!(TerminalTokens.TokenNameWHITESPACE == lastToken || TerminalTokens.TokenNameDOT == lastToken))
							mode = InsideType;
						break;
					default: // all other tokens are considered identifiers (see bug 21763 Problem in JavaScript search [search])
						if (fieldName == null)
							fieldName = scanner.getCurrentTokenString();
						else
							fieldName += scanner.getCurrentTokenString();
				}
				break;
			// read type
			case InsideType:
				switch (token) {
					case TerminalTokens.TokenNameWHITESPACE:
						break;
					default: // all other tokens are considered identifiers (see bug 21763 Problem in JavaScript search [search])
						if (type == null)
							type = scanner.getCurrentTokenString();
						else
							type += scanner.getCurrentTokenString();
				}
				break;
		}
		lastToken = token;
		try {
			token = scanner.getNextToken();
		} catch (InvalidInputException e) {
			return null;
		}
	}
	if (fieldName == null) return null;

	char[] fieldNameChars = fieldName.toCharArray();
	if (fieldNameChars.length == 1 && fieldNameChars[0] == '*') fieldNameChars = null;

	char[] declaringTypeQualification = null;
	char[] declaringTypeSimpleName = null;
	char[] typeQualification = null;
	char[] typeSimpleName = null;

	// extract declaring type infos
	if (declaringType != null) {
		char[] declaringTypePart = declaringType.toCharArray();
		int lastDotPosition = CharOperation.lastIndexOf('.', declaringTypePart);
		if (lastDotPosition >= 0) {
			declaringTypeQualification = CharOperation.subarray(declaringTypePart, 0, lastDotPosition);
			if (declaringTypeQualification.length == 1 && declaringTypeQualification[0] == '*')
				declaringTypeQualification = null;
			declaringTypeSimpleName = CharOperation.subarray(declaringTypePart, lastDotPosition+1, declaringTypePart.length);
		} else {
			declaringTypeSimpleName = declaringTypePart;
		}
		if (declaringTypeSimpleName.length == 1 && declaringTypeSimpleName[0] == '*') {
			declaringTypeSimpleName = null;
		}
	} 
	// extract type infos
	if (type != null) {
		char[] typePart = type.toCharArray();
		int lastDotPosition = CharOperation.lastIndexOf('.', typePart);
		if (lastDotPosition >= 0) {
			typeQualification = CharOperation.subarray(typePart, 0, lastDotPosition);
			if (typeQualification.length == 1 && typeQualification[0] == '*') {
				typeQualification = null;
			} else {
				// prefix with a '*' as the full qualification could be bigger (because of an import)
				typeQualification = CharOperation.concat(IIndexConstants.ONE_STAR, typeQualification);
			}
			typeSimpleName = CharOperation.subarray(typePart, lastDotPosition+1, typePart.length);
		} else {
			typeSimpleName = typePart;
		}
		if (typeSimpleName.length == 1 && typeSimpleName[0] == '*')
			typeSimpleName = null;
	}
	// Create field pattern
	boolean findDeclarations = false;
	boolean readAccess = false;
	boolean writeAccess = false;
	switch (limitTo) {
		case IJavaScriptSearchConstants.DECLARATIONS :
			findDeclarations = true;
			break;
		case IJavaScriptSearchConstants.REFERENCES :
		case IJavaScriptSearchConstants.READ_ACCESSES :
			readAccess = true;
			break;
		case IJavaScriptSearchConstants.WRITE_ACCESSES :
			writeAccess = true;
			break;
		case IJavaScriptSearchConstants.ALL_OCCURRENCES :
			findDeclarations = true;
			readAccess = true;
			writeAccess = true;
			break;
	}
	return new FieldPattern(
			findDeclarations,
			readAccess,
			writeAccess,
			isVar,
			fieldNameChars,
			declaringTypeQualification,
			declaringTypeSimpleName,
			typeQualification,
			typeSimpleName,
			matchRule,null);
}

/**
 * Method pattern are formed by:<br>
 * 	[declaringType '.'] ['&lt;' typeArguments '&gt;'] selector ['(' parameterTypes ')'] [returnType]
 *		<br>e.g.<ul>
 *			<li>java.lang.Runnable.run() void</li>
 *			<li>main(*)</li>
 *			<li>&lt;String&gt;toArray(String[])</li>
 *		</ul>
 * Constructor pattern are formed by:<br>
 *		[declaringQualification '.'] ['&lt;' typeArguments '&gt;'] type ['(' parameterTypes ')']
 *		<br>e.g.<ul>
 *			<li>java.lang.Object()</li>
 *			<li>Main(*)</li>
 *			<li>&lt;Exception&gt;Sample(Exception)</li>
 *		</ul>
 * Type arguments have the same pattern that for type patterns
 * @see #createTypePattern(String,int,int,char)
 */
private static SearchPattern createMethodOrConstructorPattern(String patternString, int limitTo, int matchRule, boolean isConstructor, boolean isFunction) {

	Scanner scanner = new Scanner(false /*comment*/, true /*whitespace*/, false /*nls*/, ClassFileConstants.JDK1_3/*sourceLevel*/, null /*taskTags*/, null/*taskPriorities*/, true/*taskCaseSensitive*/);
	scanner.setSource(patternString.toCharArray());
	final int InsideSelector = 1;
	final int InsideTypeArguments = 2;
	final int InsideParameter = 3;
	final int InsideReturnType = 4;
	int lastToken = -1;

	String declaringType = null, selector = null, parameterType = null;
	String[] parameterTypes = null;
	String typeArgumentsString = null;
	int parameterCount = -1;
	String returnType = null;
	boolean foundClosingParenthesis = false;
	int mode = InsideSelector;
	int token, argCount = 0;
	try {
		token = scanner.getNextToken();
	} catch (InvalidInputException e) {
		return null;
	}
	while (token != TerminalTokens.TokenNameEOF) {
		switch(mode) {
			// read declaring type and selector
			case InsideSelector :
				if (argCount == 0) {
					switch (token) {
						case TerminalTokens.TokenNameLESS:
							argCount++;
							if (selector == null || lastToken == TerminalTokens.TokenNameDOT) {
								if (typeArgumentsString != null) return null; // invalid syntax
								typeArgumentsString = scanner.getCurrentTokenString();
								mode = InsideTypeArguments;
								break;
							}
							if (declaringType == null) {
								declaringType = selector;
							} else {
								declaringType += '.' + selector;
							}
							declaringType += scanner.getCurrentTokenString();
							selector = null;
							break;
						case TerminalTokens.TokenNameDOT:
							if (typeArgumentsString != null) return null; // invalid syntax
							if (declaringType == null) {
								if (selector == null) return null; // invalid syntax
								declaringType = selector;
							} else if (selector != null) {
								declaringType += scanner.getCurrentTokenString() + selector;
							}
							selector = null;
							break;
						case TerminalTokens.TokenNameLPAREN:
							parameterTypes = new String[5];
							parameterCount = 0;
							mode = InsideParameter;
							break;
						case TerminalTokens.TokenNameWHITESPACE:
							switch (lastToken) {
								case TerminalTokens.TokenNameWHITESPACE:
								case TerminalTokens.TokenNameDOT:
								case TerminalTokens.TokenNameGREATER:
								case TerminalTokens.TokenNameRIGHT_SHIFT:
								case TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT:
									break;
								default:
									mode = InsideReturnType;
									break;
							}
							break;
						default: // all other tokens are considered identifiers (see bug 21763 Problem in JavaScript search [search])
							if (selector == null)
								selector = scanner.getCurrentTokenString();
							else
								selector += scanner.getCurrentTokenString();
							break;
					}
				} else {
					if (declaringType == null) return null; // invalid syntax
					switch (token) {
						case TerminalTokens.TokenNameGREATER:
						case TerminalTokens.TokenNameRIGHT_SHIFT:
						case TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT:
							argCount--;
							break;
						case TerminalTokens.TokenNameLESS:
							argCount++;
							break;
					}
					declaringType += scanner.getCurrentTokenString();
				}
				break;
			// read type arguments
			case InsideTypeArguments:
				if (typeArgumentsString == null) return null; // invalid syntax
				typeArgumentsString += scanner.getCurrentTokenString();
				switch (token) {
					case TerminalTokens.TokenNameGREATER:
					case TerminalTokens.TokenNameRIGHT_SHIFT:
					case TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT:
						argCount--;
						if (argCount == 0) {
							mode = InsideSelector;
						}
						break;
					case TerminalTokens.TokenNameLESS:
						argCount++;
						break;
				}
				break;
			// read parameter types
			case InsideParameter :
				if (argCount == 0) {
					switch (token) {
						case TerminalTokens.TokenNameWHITESPACE:
							break;
						case TerminalTokens.TokenNameCOMMA:
							if (parameterType == null) return null;
							if (parameterTypes != null) {
								if (parameterTypes.length == parameterCount)
									System.arraycopy(parameterTypes, 0, parameterTypes = new String[parameterCount*2], 0, parameterCount);
								parameterTypes[parameterCount++] = parameterType;
							}
							parameterType = null;
							break;
						case TerminalTokens.TokenNameRPAREN:
							foundClosingParenthesis = true;
							if (parameterType != null && parameterTypes != null) {
								if (parameterTypes.length == parameterCount)
									System.arraycopy(parameterTypes, 0, parameterTypes = new String[parameterCount*2], 0, parameterCount);
								parameterTypes[parameterCount++] = parameterType;
							}
							mode = isConstructor ? InsideTypeArguments : InsideReturnType;
							break;
						case TerminalTokens.TokenNameLESS:
							argCount++;
							if (parameterType == null) return null; // invalid syntax
							//$FALL-THROUGH$ next case to add token
						default: // all other tokens are considered identifiers (see bug 21763 Problem in JavaScript search [search])
							if (parameterType == null)
								parameterType = scanner.getCurrentTokenString();
							else
								parameterType += scanner.getCurrentTokenString();
					}
				} else {
					if (parameterType == null) return null; // invalid syntax
					switch (token) {
						case TerminalTokens.TokenNameGREATER:
						case TerminalTokens.TokenNameRIGHT_SHIFT:
						case TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT:
							argCount--;
							break;
						case TerminalTokens.TokenNameLESS:
							argCount++;
							break;
					}
					parameterType += scanner.getCurrentTokenString();
				}
				break;
			// read return type
			case InsideReturnType:
				if (argCount == 0) {
					switch (token) {
						case TerminalTokens.TokenNameWHITESPACE:
							break;
						case TerminalTokens.TokenNameLPAREN:
							parameterTypes = new String[5];
							parameterCount = 0;
							mode = InsideParameter;
							break;
						case TerminalTokens.TokenNameLESS:
							argCount++;
							if (returnType == null) return null; // invalid syntax
							//$FALL-THROUGH$ next case to add token
						default: // all other tokens are considered identifiers (see bug 21763 Problem in JavaScript search [search])
							if (returnType == null)
								returnType = scanner.getCurrentTokenString();
							else
								returnType += scanner.getCurrentTokenString();
					}
				} else {
					if (returnType == null) return null; // invalid syntax
					switch (token) {
						case TerminalTokens.TokenNameGREATER:
						case TerminalTokens.TokenNameRIGHT_SHIFT:
						case TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT:
							argCount--;
							break;
						case TerminalTokens.TokenNameLESS:
							argCount++;
							break;
					}
					returnType += scanner.getCurrentTokenString();
				}
				break;
		}
		lastToken = token;
		try {
			token = scanner.getNextToken();
		} catch (InvalidInputException e) {
			return null;
		}
	}
	// parenthesis mismatch
	if (parameterCount>0 && !foundClosingParenthesis) return null;
	// type arguments mismatch
	if (argCount > 0) return null;

	char[] selectorChars = null;
	if (isConstructor) {
		// retrieve type for constructor patterns
		if (declaringType == null)
			declaringType = selector;
		else if (selector != null)
			declaringType += '.' + selector;
	} else {
		// get selector chars
		if (selector == null) return null;
		selectorChars = selector.toCharArray();
		if (selectorChars.length == 1 && selectorChars[0] == '*')
			selectorChars = null;
	}

	char[] declaringTypeQualification = null, declaringTypeSimpleName = null;
	char[] returnTypeQualification = null, returnTypeSimpleName = null;
	char[][] parameterTypeQualifications = null, parameterTypeSimpleNames = null;
	// Signatures
	String[] parameterTypeSignatures = null;

	// extract declaring type infos
	if (declaringType != null) {
		// get declaring type part and signature
		char[] declaringTypePart = null;
		try {
			declaringTypePart = declaringType.toCharArray();
		}
		catch (IllegalArgumentException iae) {
			// declaring type is invalid
			return null;
		}
		int lastDotPosition = CharOperation.lastIndexOf('.', declaringTypePart);
		if (lastDotPosition >= 0) {
			declaringTypeQualification = CharOperation.subarray(declaringTypePart, 0, lastDotPosition);
			if (declaringTypeQualification.length == 1 && declaringTypeQualification[0] == '*')
				declaringTypeQualification = null;
			declaringTypeSimpleName = CharOperation.subarray(declaringTypePart, lastDotPosition+1, declaringTypePart.length);
		} else {
			declaringTypeSimpleName = declaringTypePart;
		}
		if (declaringTypeSimpleName.length == 1 && declaringTypeSimpleName[0] == '*')
			declaringTypeSimpleName = null;
	} 
	// extract parameter types infos
	if (parameterCount >= 0) {
		parameterTypeQualifications = new char[parameterCount][];
		parameterTypeSimpleNames = new char[parameterCount][];
		parameterTypeSignatures = new String[parameterCount];
		for (int i = 0; i < parameterCount; i++) {
			// get parameter type part and signature
			char[] parameterTypePart = null;
			try {
				if (parameterTypes != null) {
					parameterTypeSignatures[i] = Signature.createTypeSignature(parameterTypes[i], false);
					parameterTypePart = parameterTypes[i].toCharArray();
				}
			}
			catch (IllegalArgumentException iae) {
				// string is not a valid type syntax
				return null;
			}
			int lastDotPosition = parameterTypePart==null ? -1 : CharOperation.lastIndexOf('.', parameterTypePart);
			if (parameterTypePart != null && lastDotPosition >= 0) {
				parameterTypeQualifications[i] = CharOperation.subarray(parameterTypePart, 0, lastDotPosition);
				if (parameterTypeQualifications[i].length == 1 && parameterTypeQualifications[i][0] == '*') {
					parameterTypeQualifications[i] = null;
				} else {
					// prefix with a '*' as the full qualification could be bigger (because of an import)
					parameterTypeQualifications[i] = CharOperation.concat(IIndexConstants.ONE_STAR, parameterTypeQualifications[i]);
				}
				parameterTypeSimpleNames[i] = CharOperation.subarray(parameterTypePart, lastDotPosition+1, parameterTypePart.length);
			} else {
				parameterTypeQualifications[i] = null;
				parameterTypeSimpleNames[i] = parameterTypePart;
			}
			if (parameterTypeSimpleNames[i].length == 1 && parameterTypeSimpleNames[i][0] == '*')
				parameterTypeSimpleNames[i] = null;
		}
	}
	// extract return type infos
	if (returnType != null) {
		// get return type part and signature
		char[] returnTypePart = null;
		try {
			returnTypePart = returnType.toCharArray();
		}
		catch (IllegalArgumentException iae) {
			// declaring type is invalid
			return null;
		}
		int lastDotPosition = CharOperation.lastIndexOf('.', returnTypePart);
		if (lastDotPosition >= 0) {
			returnTypeQualification = CharOperation.subarray(returnTypePart, 0, lastDotPosition);
			if (returnTypeQualification.length == 1 && returnTypeQualification[0] == '*') {
				returnTypeQualification = null;
			} else {
				// because of an import
				returnTypeQualification = CharOperation.concat(IIndexConstants.ONE_STAR, returnTypeQualification);
			}
			returnTypeSimpleName = CharOperation.subarray(returnTypePart, lastDotPosition+1, returnTypePart.length);
		} else {
			returnTypeSimpleName = returnTypePart;
		}
		if (returnTypeSimpleName.length == 1 && returnTypeSimpleName[0] == '*')
			returnTypeSimpleName = null;
	}
	// Create method/constructor pattern
	boolean findDeclarations = true;
	boolean findReferences = true;
	switch (limitTo) {
		case IJavaScriptSearchConstants.DECLARATIONS :
			findReferences = false;
			break;
		case IJavaScriptSearchConstants.REFERENCES :
			findDeclarations = false;
			break;
		case IJavaScriptSearchConstants.ALL_OCCURRENCES :
			break;
	}
	if (isConstructor) {
		return new ConstructorPattern(patternString.toCharArray(), matchRule, findDeclarations, findReferences);
	} else {
		return new MethodPattern(
				findDeclarations,
				findReferences,
				isFunction,
				selectorChars,
				parameterTypeQualifications,
				parameterTypeSimpleNames,
				returnTypeQualification,
				returnTypeSimpleName,
				declaringTypeQualification,
				declaringTypeSimpleName,
				matchRule);
	}
}

/**
 * Returns a search pattern that combines the given two patterns into an
 * "or" pattern. The search result will match either the left pattern or the
 * right pattern.
 *
 * @param leftPattern the left pattern
 * @param rightPattern the right pattern
 * @return an "or" pattern
 */
public static SearchPattern createOrPattern(SearchPattern leftPattern, SearchPattern rightPattern) {
	return new OrPattern(leftPattern, rightPattern);
}

private static SearchPattern createPackagePattern(String patternString, int limitTo, int matchRule) {
	switch (limitTo) {
		case IJavaScriptSearchConstants.DECLARATIONS :
			return new PackageDeclarationPattern(patternString.toCharArray(), matchRule);
		case IJavaScriptSearchConstants.REFERENCES :
			return new PackageReferencePattern(patternString.toCharArray(), matchRule);
		case IJavaScriptSearchConstants.ALL_OCCURRENCES :
			return new OrPattern(
				new PackageDeclarationPattern(patternString.toCharArray(), matchRule),
				new PackageReferencePattern(patternString.toCharArray(), matchRule)
			);
	}
	return null;
}

/**
 * Returns a search pattern based on a given string pattern. The string patterns support '*' wild-cards.
 * The remaining parameters are used to narrow down the type of expected results.
 *
 * <br>
 *	Examples:
 *	<ul>
 * 		<li>search for case insensitive references to <code>Object</code>:
 *			<code>createSearchPattern("Object", TYPE, REFERENCES, false);</code></li>
 *  	<li>search for case sensitive references to exact <code>Object()</code> constructor:
 *			<code>createSearchPattern("java.lang.Object()", CONSTRUCTOR, REFERENCES, true);</code></li>
 *  	<li>search for implementers of <code>java.lang.Runnable</code>:
 *			<code>createSearchPattern("java.lang.Runnable", TYPE, IMPLEMENTORS, true);</code></li>
 *  </ul>
 * @param stringPattern the given pattern
 * @param searchFor determines the nature of the searched elements
 *	<ul>
 * 	<li>{@link IJavaScriptSearchConstants#CLASS}: only look for classes</li>
 *		<li>{@link IJavaScriptSearchConstants#INTERFACE}: only look for interfaces</li>
 * 	<li>{@link IJavaScriptSearchConstants#ENUM}: only look for enumeration</li>
 *		<li>{@link IJavaScriptSearchConstants#ANNOTATION_TYPE}: only look for annotation type</li>
 * 	<li>{@link IJavaScriptSearchConstants#CLASS_AND_ENUM}: only look for classes and enumerations</li>
 *		<li>{@link IJavaScriptSearchConstants#CLASS_AND_INTERFACE}: only look for classes and interfaces</li>
 * 	<li>{@link IJavaScriptSearchConstants#TYPE}: look for all types (ie. classes, interfaces, enum and annotation types)</li>
 *		<li>{@link IJavaScriptSearchConstants#FIELD}: look for fields</li>
 *		<li>{@link IJavaScriptSearchConstants#METHOD}: look for methods</li>
 *		<li>{@link IJavaScriptSearchConstants#CONSTRUCTOR}: look for constructors</li>
 *		<li>{@link IJavaScriptSearchConstants#PACKAGE}: look for packages</li>
 *	</ul>
 * @param limitTo determines the nature of the expected matches
 *	<ul>
 * 	<li>{@link IJavaScriptSearchConstants#DECLARATIONS}: will search declarations matching
 * 			with the corresponding element. In case the element is a method, declarations of matching
 * 			methods in subtypes will also be found, allowing to find declarations of abstract methods, etc.<br>
 * 			Note that additional flags {@link IJavaScriptSearchConstants#IGNORE_DECLARING_TYPE} and
 * 			{@link IJavaScriptSearchConstants#IGNORE_RETURN_TYPE} are ignored for string patterns.
 * 			This is due to the fact that client may omit to define them in string pattern to have same behavior.
 * 	</li>
 *		 <li>{@link IJavaScriptSearchConstants#REFERENCES}: will search references to the given element.</li>
 *		 <li>{@link IJavaScriptSearchConstants#ALL_OCCURRENCES}: will search for either declarations or
 *				references as specified above.
 *		</li>
 *		 <li>{@link IJavaScriptSearchConstants#IMPLEMENTORS}: for types, will find all types
 *				which directly implement/extend a given interface.
 *				Note that types may be only classes or only interfaces if {@link IJavaScriptSearchConstants#CLASS } or
 *				{@link IJavaScriptSearchConstants#INTERFACE} is respectively used instead of {@link IJavaScriptSearchConstants#TYPE}.
 *		</li>
 *	</ul>
 * @param matchRule one of {@link #R_EXACT_MATCH}, {@link #R_PREFIX_MATCH}, {@link #R_PATTERN_MATCH},
 * 	{@link #R_REGEXP_MATCH}, {@link #R_CAMELCASE_MATCH} combined with one of following values:
 * 	{@link #R_CASE_SENSITIVE}, {@link #R_ERASURE_MATCH} or {@link #R_EQUIVALENT_MATCH}.
 *		e.g. {@link #R_EXACT_MATCH} | {@link #R_CASE_SENSITIVE} if an exact and case sensitive match is requested,
 *		{@link #R_PREFIX_MATCH} if a prefix non case sensitive match is requested or {@link #R_EXACT_MATCH} | {@link #R_ERASURE_MATCH}
 *		if a non case sensitive and erasure match is requested.<br>
 * 	Note that {@link #R_ERASURE_MATCH} or {@link #R_EQUIVALENT_MATCH} have no effect
 * 	on non-generic types/methods search.<br>
 * 	Note also that default behavior for generic types/methods search is to find exact matches.
 * @return a search pattern on the given string pattern, or <code>null</code> if the string pattern is ill-formed
 */
public static SearchPattern createPattern(String stringPattern, int searchFor, int limitTo, int matchRule) {
	if (stringPattern == null || stringPattern.length() == 0) return null;

	if ((matchRule = validateMatchRule(stringPattern, matchRule)) == -1) {
		return null;
	}

	// Ignore additional nature flags
	limitTo &= ~(IJavaScriptSearchConstants.IGNORE_DECLARING_TYPE+IJavaScriptSearchConstants.IGNORE_RETURN_TYPE);

	switch (searchFor) {
		case IJavaScriptSearchConstants.CLASS:
			return createTypePattern(stringPattern, limitTo, matchRule, IIndexConstants.CLASS_SUFFIX);
		case IJavaScriptSearchConstants.TYPE:
			return createTypePattern(stringPattern, limitTo, matchRule, IIndexConstants.TYPE_SUFFIX);
		case IJavaScriptSearchConstants.FUNCTION:
			return createMethodOrConstructorPattern(stringPattern, limitTo, matchRule, false/*not a constructor*/,true);
		case IJavaScriptSearchConstants.METHOD:
			return createMethodOrConstructorPattern(stringPattern, limitTo, matchRule, false/*not a constructor*/,false);
		case IJavaScriptSearchConstants.CONSTRUCTOR:
			return createMethodOrConstructorPattern(stringPattern, limitTo, matchRule, true/*constructor*/,false);
		case IJavaScriptSearchConstants.FIELD:
			return createFieldPattern(stringPattern, limitTo, matchRule,false);
		case IJavaScriptSearchConstants.VAR:
			return createFieldPattern(stringPattern, limitTo, matchRule,true);
		case IJavaScriptSearchConstants.PACKAGE:
			return createPackagePattern(stringPattern, limitTo, matchRule);
	}
	return null;
}

/**
 * Returns a search pattern based on a given JavaScript element.
 * The pattern is used to trigger the appropriate search.
 * <br>
 * Note that for generic searches, the returned pattern consider {@link #R_ERASURE_MATCH} matches.
 * If other kind of generic matches (ie. {@link #R_EXACT_MATCH} or {@link #R_EQUIVALENT_MATCH})
 * are expected, {@link #createPattern(IJavaScriptElement, int, int)} method need to be used instead with
 * the explicit match rule specified.
 * <br>
 * The pattern can be parameterized as follows:
 *
 * @param element the JavaScript element the search pattern is based on
 * @param limitTo determines the nature of the expected matches
 *	<ul>
 * 	<li>{@link IJavaScriptSearchConstants#DECLARATIONS}: will search declarations matching
 * 			with the corresponding element. In case the element is a method, declarations of matching
 * 			methods in subtypes will also be found, allowing to find declarations of abstract methods, etc.
 *				Some additional flags may be specified while searching declaration:
 *				<ul>
 *					<li>{@link IJavaScriptSearchConstants#IGNORE_DECLARING_TYPE}: declaring type will be ignored
 *							during the search.<br>
 *							For example using following test case:
 *					<pre>
 *                  class A { A method() { return null; } }
 *                  class B extends A { B method() { return null; } }
 *                  class C { A method() { return null; } }
 *					</pre>
 *							search for <code>method</code> declaration with this flag
 *							will return 2 matches: in A and in C
 *					</li>
 *					<li>{@link IJavaScriptSearchConstants#IGNORE_RETURN_TYPE}: return type will be ignored
 *							during the search.<br>
 *							Using same example, search for <code>method</code> declaration with this flag
 *							will return 2 matches: in A and in B.
 *					</li>
 *				</ul>
 *				Note that these two flags may be combined and both declaring and return types can be ignored
 *				during the search. Then, using same example, search for <code>method</code> declaration
 *				with these 2 flags will return 3 matches: in A, in B  and in C
 * 	</li>
 *		 <li>{@link IJavaScriptSearchConstants#REFERENCES}: will search references to the given element.</li>
 *		 <li>{@link IJavaScriptSearchConstants#ALL_OCCURRENCES}: will search for either declarations or
 *				references as specified above.
 *		</li>
 *		 <li>{@link IJavaScriptSearchConstants#IMPLEMENTORS}: for types, will find all types
 *				which directly implement/extend a given interface.
 *		</li>
 *	</ul>
 * @return a search pattern for a JavaScript element or <code>null</code> if the given element is ill-formed
 */
public static SearchPattern createPattern(IJavaScriptElement element, int limitTo) {
	return createPattern(element, limitTo, R_EXACT_MATCH | R_CASE_SENSITIVE | R_ERASURE_MATCH);
}

/**
 * Returns a search pattern based on a given JavaScript element.
 * The pattern is used to trigger the appropriate search, and can be parameterized as follows:
 *
 * @param element the JavaScript element the search pattern is based on
 * @param limitTo determines the nature of the expected matches
 *	<ul>
 * 	<li>{@link IJavaScriptSearchConstants#DECLARATIONS}: will search declarations matching
 * 			with the corresponding element. In case the element is a method, declarations of matching
 * 			methods in subtypes will also be found, allowing to find declarations of abstract methods, etc.
 *				Some additional flags may be specified while searching declaration:
 *				<ul>
 *					<li>{@link IJavaScriptSearchConstants#IGNORE_DECLARING_TYPE}: declaring type will be ignored
 *							during the search.<br>
 *							For example using following test case:
 *					<pre>
 *                  class A { A method() { return null; } }
 *                  class B extends A { B method() { return null; } }
 *                  class C { A method() { return null; } }
 *					</pre>
 *							search for <code>method</code> declaration with this flag
 *							will return 2 matches: in A and in C
 *					</li>
 *					<li>{@link IJavaScriptSearchConstants#IGNORE_RETURN_TYPE}: return type will be ignored
 *							during the search.<br>
 *							Using same example, search for <code>method</code> declaration with this flag
 *							will return 2 matches: in A and in B.
 *					</li>
 *				</ul>
 *				Note that these two flags may be combined and both declaring and return types can be ignored
 *				during the search. Then, using same example, search for <code>method</code> declaration
 *				with these 2 flags will return 3 matches: in A, in B  and in C
 * 	</li>
 *		 <li>{@link IJavaScriptSearchConstants#REFERENCES}: will search references to the given element.</li>
 *		 <li>{@link IJavaScriptSearchConstants#ALL_OCCURRENCES}: will search for either declarations or
 *				references as specified above.
 *		</li>
 *		 <li>{@link IJavaScriptSearchConstants#IMPLEMENTORS}: for types, will find all types
 *				which directly implement/extend a given interface.
 *		</li>
 *	</ul>
 * @param matchRule one of {@link #R_EXACT_MATCH}, {@link #R_PREFIX_MATCH}, {@link #R_PATTERN_MATCH},
 * 	{@link #R_REGEXP_MATCH}, {@link #R_CAMELCASE_MATCH} combined with one of following values:
 * 	{@link #R_CASE_SENSITIVE}, {@link #R_ERASURE_MATCH} or {@link #R_EQUIVALENT_MATCH}.
 *		e.g. {@link #R_EXACT_MATCH} | {@link #R_CASE_SENSITIVE} if an exact and case sensitive match is requested,
 *		{@link #R_PREFIX_MATCH} if a prefix non case sensitive match is requested or {@link #R_EXACT_MATCH} |{@link #R_ERASURE_MATCH}
 *		if a non case sensitive and erasure match is requested.<br>
 * 	Note that {@link #R_ERASURE_MATCH} or {@link #R_EQUIVALENT_MATCH} have no effect on non-generic types
 * 	or methods search.<br>
 * 	Note also that default behavior for generic types or methods is to find exact matches.
 * @return a search pattern for a JavaScript element or <code>null</code> if the given element is ill-formed
 *  
 */
public static SearchPattern createPattern(IJavaScriptElement element, int limitTo, int matchRule) {
	SearchPattern searchPattern = null;
	int lastDot;
	boolean ignoreDeclaringType = false;
	boolean ignoreReturnType = false;
	int maskedLimitTo = limitTo & ~(IJavaScriptSearchConstants.IGNORE_DECLARING_TYPE+IJavaScriptSearchConstants.IGNORE_RETURN_TYPE);
	if (maskedLimitTo == IJavaScriptSearchConstants.DECLARATIONS || maskedLimitTo == IJavaScriptSearchConstants.ALL_OCCURRENCES) {
		ignoreDeclaringType = (limitTo & IJavaScriptSearchConstants.IGNORE_DECLARING_TYPE) != 0;
		ignoreReturnType = (limitTo & IJavaScriptSearchConstants.IGNORE_RETURN_TYPE) != 0;
	}
	char[] declaringSimpleName = null;
	char[] declaringQualification = null;
	boolean isVar=false;
	boolean isFunction=false;
	switch (element.getElementType()) {
		case IJavaScriptElement.FIELD :
			IField field = (IField) element;
			IType declaringClassForField = field.getDeclaringType();
			isVar=(declaringClassForField==null);
			if (!ignoreDeclaringType) {
				if (declaringClassForField!=null)
				{
					declaringSimpleName = declaringClassForField.getElementName().toCharArray();
					declaringQualification= declaringClassForField.getPackageFragment().getElementName().toCharArray();
					char[][] enclosingNames = enclosingTypeNames(declaringClassForField);
					if (enclosingNames.length > 0) {
						declaringQualification = CharOperation.concat(declaringQualification, CharOperation.concatWith(enclosingNames, '.'), '.');
					}
				}
			}
			char[] name = field.getElementName().toCharArray();
			char[] typeSimpleName = null;
			char[] typeQualification = null;
			String typeSignature = null;
			if (!ignoreReturnType) {
				try {
					typeSignature = field.getTypeSignature();
					char[] signature = typeSignature.toCharArray();
					char[] typeErasure = Signature.toCharArray(signature);
					if ((lastDot = CharOperation.lastIndexOf('.', typeErasure)) == -1) {
						typeSimpleName = typeErasure;
					} else {
						typeSimpleName = CharOperation.subarray(typeErasure, lastDot + 1, typeErasure.length);
						typeQualification = CharOperation.subarray(typeErasure, 0, lastDot);
						if (!field.isBinary()) {
							// prefix with a '*' as the full qualification could be bigger (because of an import)
							typeQualification = CharOperation.concat(IIndexConstants.ONE_STAR, typeQualification);
						}
					}
				} catch (JavaScriptModelException e) {
					return null;
				}
			}

			// Create field pattern
			boolean findDeclarations = false;
			boolean readAccess = false;
			boolean writeAccess = false;
			switch (maskedLimitTo) {
				case IJavaScriptSearchConstants.DECLARATIONS :
					findDeclarations = true;
					break;
				case IJavaScriptSearchConstants.REFERENCES :
				case IJavaScriptSearchConstants.READ_ACCESSES :
					readAccess = true;
					break;
				case IJavaScriptSearchConstants.WRITE_ACCESSES :
					writeAccess = true;
					break;
				case IJavaScriptSearchConstants.ALL_OCCURRENCES :
					findDeclarations = true;
					readAccess = true;
					writeAccess = true;
					break;
			}
			searchPattern =
				new FieldPattern(
					findDeclarations,
					readAccess,
					writeAccess,
					isVar,
					name,
					declaringQualification,
					declaringSimpleName,
					typeQualification,
					typeSimpleName,
					matchRule,field);
			break;
		case IJavaScriptElement.IMPORT_DECLARATION :
			String elementName = element.getElementName();
			lastDot = elementName.lastIndexOf('.');
			if (lastDot == -1) return null; // invalid import declaration
			IImportDeclaration importDecl = (IImportDeclaration)element;
			if (importDecl.isOnDemand()) {
				searchPattern = createPackagePattern(elementName.substring(0, lastDot), maskedLimitTo, matchRule);
			} else {
				searchPattern =
					createTypePattern(
						elementName.substring(lastDot+1).toCharArray(),
						elementName.substring(0, lastDot).toCharArray(),
						null,
						null,
						null,
						maskedLimitTo,
						matchRule);
			}
			break;
		case IJavaScriptElement.LOCAL_VARIABLE :
			LocalVariable localVar = (LocalVariable) element;
			boolean findVarDeclarations = false;
			boolean findVarReadAccess = false;
			boolean findVarWriteAccess = false;
			switch (maskedLimitTo) {
				case IJavaScriptSearchConstants.DECLARATIONS :
					findVarDeclarations = true;
					break;
				case IJavaScriptSearchConstants.REFERENCES :
					findVarReadAccess = true;
					findVarWriteAccess = true;
					break;
				case IJavaScriptSearchConstants.READ_ACCESSES :
					findVarReadAccess = true;
					break;
				case IJavaScriptSearchConstants.WRITE_ACCESSES :
					findVarWriteAccess = true;
					break;
				case IJavaScriptSearchConstants.ALL_OCCURRENCES :
					findVarDeclarations = true;
					findVarReadAccess = true;
					findVarWriteAccess = true;
					break;
			}
			searchPattern =
				new LocalVariablePattern(
					findVarDeclarations,
					findVarReadAccess,
					findVarWriteAccess,
					localVar,
					matchRule);
			break;
		case IJavaScriptElement.METHOD :
			IFunction method = (IFunction) element;
			boolean isConstructor;
			try {
				isConstructor = method.isConstructor();
			} catch (JavaScriptModelException e) {
				return null;
			}
			IType declaringClass = method.getDeclaringType();

			if (declaringClass!=null) {
				if (ignoreDeclaringType) {
					if (isConstructor)
						declaringSimpleName = declaringClass.getElementName().toCharArray();
				} else {
					declaringSimpleName = declaringClass.getElementName().toCharArray();
					declaringQualification = declaringClass.getPackageFragment().getElementName().toCharArray();
					char[][] enclosingNames = enclosingTypeNames(declaringClass);
					if (enclosingNames.length > 0) {
						declaringQualification = CharOperation.concat(
								declaringQualification, CharOperation.concatWith(enclosingNames, '.'), '.');
					}
				}
			}
			else
				isFunction=true;
			char[] selector = method.getElementName().toCharArray();
			char[] returnSimpleName = null;
			char[] returnQualification = null;
			String returnSignature = null;
			if (!ignoreReturnType) {
				try {
					returnSignature = method.getReturnType();
					char[] signature = returnSignature.toCharArray();
					char[] returnErasure = Signature.toCharArray(signature);
					if ((lastDot = CharOperation.lastIndexOf('.', returnErasure)) == -1) {
						returnSimpleName = returnErasure;
					} else {
						returnSimpleName = CharOperation.subarray(returnErasure, lastDot + 1, returnErasure.length);
						returnQualification = CharOperation.subarray(returnErasure, 0, lastDot);
						if (!method.isBinary()) {
							// prefix with a '*' as the full qualification could be bigger (because of an import)
							CharOperation.concat(IIndexConstants.ONE_STAR, returnQualification);
						}
					}
				} catch (JavaScriptModelException e) {
					return null;
				}
			}
			String[] parameterTypes = method.getParameterTypes();
			int paramCount = parameterTypes.length;
			char[][] parameterSimpleNames = new char[paramCount][];
			char[][] parameterQualifications = new char[paramCount][];
			String[] parameterSignatures = new String[paramCount];
			for (int i = 0; i < paramCount; i++) {
				parameterSignatures[i] = parameterTypes[i];
				char[] signature = parameterSignatures[i].toCharArray();
				char[] paramErasure = Signature.toCharArray(signature);
				if ((lastDot = CharOperation.lastIndexOf('.', paramErasure)) == -1) {
					parameterSimpleNames[i] = paramErasure;
					parameterQualifications[i] = null;
				} else {
					parameterSimpleNames[i] = CharOperation.subarray(paramErasure, lastDot + 1, paramErasure.length);
					parameterQualifications[i] = CharOperation.subarray(paramErasure, 0, lastDot);
					if (!method.isBinary()) {
						// prefix with a '*' as the full qualification could be bigger (because of an import)
						CharOperation.concat(IIndexConstants.ONE_STAR, parameterQualifications[i]);
					}
				}
			}

			// Create method/constructor pattern
			boolean findMethodDeclarations = true;
			boolean findMethodReferences = true;
			switch (maskedLimitTo) {
				case IJavaScriptSearchConstants.DECLARATIONS :
					findMethodReferences = false;
					break;
				case IJavaScriptSearchConstants.REFERENCES :
					findMethodDeclarations = false;
					break;
				case IJavaScriptSearchConstants.ALL_OCCURRENCES :
					break;
			}
			if (isConstructor) {
				searchPattern =
					new ConstructorPattern(
						findMethodDeclarations,
						findMethodReferences,
						parameterQualifications,
						parameterSimpleNames,
						declaringSimpleName,
						declaringQualification,
						matchRule);
			} else {
				searchPattern =
					new MethodPattern(
						findMethodDeclarations,
						findMethodReferences,
						isFunction,
						selector,
						parameterQualifications,
						parameterSimpleNames,
						returnQualification,
						returnSimpleName,
						declaringQualification,
						declaringSimpleName,
						matchRule);
			}
			break;
		case IJavaScriptElement.TYPE :
			IType type = (IType)element;
			searchPattern = 	createTypePattern(
						type.getElementName().toCharArray(),
						type.getPackageFragment().getElementName().toCharArray(),
						ignoreDeclaringType ? null : enclosingTypeNames(type),
						null,
						type,
						maskedLimitTo,
						matchRule);
			break;
		case IJavaScriptElement.PACKAGE_FRAGMENT :
			searchPattern = createPackagePattern(element.getElementName(), maskedLimitTo, matchRule);
			break;
	}
	if (searchPattern != null)
		MatchLocator.setFocus(searchPattern, element);
	return searchPattern;
}

private static SearchPattern createTypePattern(char[] simpleName, char[] packageName, char[][] enclosingTypeNames, String typeSignature, IType type, int limitTo, int matchRule) {
	switch (limitTo) {
		case IJavaScriptSearchConstants.DECLARATIONS :
			return new TypeDeclarationPattern(
				packageName,
				simpleName,
				enclosingTypeNames,
				matchRule);
		case IJavaScriptSearchConstants.REFERENCES :
			if (type != null) {
				return new TypeReferencePattern(
					CharOperation.concatWith(packageName, enclosingTypeNames, '.'),
					simpleName,
					type,
					matchRule);
			}
			return new TypeReferencePattern(
				CharOperation.concatWith(packageName, enclosingTypeNames, '.'),
				simpleName,
				typeSignature,
				matchRule);
		case IJavaScriptSearchConstants.IMPLEMENTORS :
			return new SuperTypeReferencePattern(
						QualificationHelpers.createFullyQualifiedName(packageName, simpleName),
				matchRule);
		case IJavaScriptSearchConstants.ALL_OCCURRENCES :
			return new OrPattern(
				new TypeDeclarationPattern(
					packageName,
					simpleName,
					enclosingTypeNames,
					matchRule),
				(type != null)
					? new TypeReferencePattern(
						CharOperation.concatWith(packageName, enclosingTypeNames, '.'),
						simpleName,
						type,
						matchRule)
					: new TypeReferencePattern(
						CharOperation.concatWith(packageName, enclosingTypeNames, '.'),
						simpleName,
						typeSignature,
						matchRule)
			);
	}
	return null;
}
/**
 * Type pattern are formed by [qualification '.']type [typeArguments].
 * e.g. java.lang.Object
 *		Runnable
 *		List&lt;String&gt;
 */
private static SearchPattern createTypePattern(String patternString, int limitTo, int matchRule, char indexSuffix) {

	Scanner scanner = new Scanner(false /*comment*/, true /*whitespace*/, false /*nls*/, ClassFileConstants.JDK1_3/*sourceLevel*/, null /*taskTags*/, null/*taskPriorities*/, true/*taskCaseSensitive*/);
	scanner.setSource(patternString.toCharArray());
	String type = null;
	int token;
	try {
		token = scanner.getNextToken();
	} catch (InvalidInputException e) {
		return null;
	}
	int argCount = 0;
	while (token != TerminalTokens.TokenNameEOF) {
		if (argCount == 0) {
			switch (token) {
				case TerminalTokens.TokenNameWHITESPACE:
					break;
				case TerminalTokens.TokenNameLESS:
					argCount++;
					// fall through default case to add token to type
				default: // all other tokens are considered identifiers (see bug 21763 Problem in JavaScript search [search])
					if (type == null)
						type = scanner.getCurrentTokenString();
					else
						type += scanner.getCurrentTokenString();
			}
		} else {
			switch (token) {
				case TerminalTokens.TokenNameGREATER:
				case TerminalTokens.TokenNameRIGHT_SHIFT:
				case TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT:
					argCount--;
					break;
				case TerminalTokens.TokenNameLESS:
					argCount++;
					break;
			}
			if (type == null) return null; // invalid syntax
			type += scanner.getCurrentTokenString();
		}
		try {
			token = scanner.getNextToken();
		} catch (InvalidInputException e) {
			return null;
		}
	}
	if (type == null) return null;
	String typeSignature = null;
	char[] qualificationChars = null, typeChars = null;

	// get type part and signature
	char[] typePart = null;
	try {
		typeSignature = Signature.createTypeSignature(type, false);
		typePart = type.toCharArray();
	}
	catch (IllegalArgumentException iae) {
		// string is not a valid type syntax
		return null;
	}

	// get qualification name
	int lastDotPosition = CharOperation.lastIndexOf('.', typePart);
	if (lastDotPosition >= 0) {
		qualificationChars = CharOperation.subarray(typePart, 0, lastDotPosition);
		if (qualificationChars.length == 1 && qualificationChars[0] == '*')
			qualificationChars = null;
		typeChars = CharOperation.subarray(typePart, lastDotPosition+1, typePart.length);
	} else {
		typeChars = typePart;
	}
	if (typeChars.length == 1 && typeChars[0] == '*') {
		typeChars = null;
	}
	switch (limitTo) {
		case IJavaScriptSearchConstants.DECLARATIONS : // cannot search for explicit member types
			return new TypeDeclarationPattern(typePart, matchRule);
		case IJavaScriptSearchConstants.REFERENCES :
			return new TypeReferencePattern(qualificationChars, typeChars, typeSignature, matchRule);
		case IJavaScriptSearchConstants.IMPLEMENTORS :
			return new SuperTypeReferencePattern(QualificationHelpers.createFullyQualifiedName(qualificationChars, typeChars), matchRule);
		case IJavaScriptSearchConstants.ALL_OCCURRENCES :
			return new OrPattern(
				new TypeDeclarationPattern(typePart, matchRule),// cannot search for explicit member types
				new TypeReferencePattern(qualificationChars, typeChars, matchRule));
	}
	return null;
}
/**
 * Returns the enclosing type names of the given type.
 */
private static char[][] enclosingTypeNames(IType type) {
	IJavaScriptElement parent = type.getParent();
	switch (parent.getElementType()) {
		case IJavaScriptElement.CLASS_FILE:
			// For a binary type, the parent is not the enclosing type, but the declaring type is.
			// (see bug 20532  Declaration of member binary type not found)
			IType declaringType = type.getDeclaringType();
			if (declaringType == null) return CharOperation.NO_CHAR_CHAR;
			return CharOperation.arrayConcat(
				enclosingTypeNames(declaringType),
				declaringType.getElementName().toCharArray());
		case IJavaScriptElement.JAVASCRIPT_UNIT:
			return CharOperation.NO_CHAR_CHAR;
		case IJavaScriptElement.FIELD:
		case IJavaScriptElement.INITIALIZER:
		case IJavaScriptElement.METHOD:
			IType declaringClass = ((IMember) parent).getDeclaringType();
			if (declaringClass!=null)
			return CharOperation.arrayConcat(
				enclosingTypeNames(declaringClass),
				new char[][] {declaringClass.getElementName().toCharArray(), IIndexConstants.ONE_STAR});
			else
				return CharOperation.NO_CHAR_CHAR;
		case IJavaScriptElement.TYPE:
			return CharOperation.arrayConcat(
				enclosingTypeNames((IType)parent),
				parent.getElementName().toCharArray());
		default:
			return null;
	}
}

/**
 * Decode the given index key in this pattern. The decoded index key is used by
 * {@link #matchesDecodedKey(SearchPattern)} to find out if the corresponding index entry
 * should be considered.
 * <p>
 * This method should be re-implemented in subclasses that need to decode an index key.
 * </p>
 *
 * @param key the given index key
 */
public void decodeIndexKey(char[] key) {
	// called from findIndexMatches(), override as necessary
}
/**
 * Returns a blank pattern that can be used as a record to decode an index key.
 * <p>
 * Implementors of this method should return a new search pattern that is going to be used
 * to decode index keys.
 * </p>
 *
 * @return a new blank pattern
 * @see #decodeIndexKey(char[])
 */
public abstract SearchPattern getBlankPattern();
/**
 * Returns a key to find in relevant index categories, if null then all index entries are matched.
 * The key will be matched according to some match rule. These potential matches
 * will be further narrowed by the match locator, but precise match locating can be expensive,
 * and index query should be as accurate as possible so as to eliminate obvious false hits.
 * <p>
 * This method should be re-implemented in subclasses that need to narrow down the
 * index query.
 * </p>
 *
 * @return an index key from this pattern, or <code>null</code> if all index entries are matched.
 */
public char[] getIndexKey() {
	return null; // called from queryIn(), override as necessary
}
/**
 * Returns an array of index categories to consider for this index query.
 * These potential matches will be further narrowed by the match locator, but precise
 * match locating can be expensive, and index query should be as accurate as possible
 * so as to eliminate obvious false hits.
 * <p>
 * This method should be re-implemented in subclasses that need to narrow down the
 * index query.
 * </p>
 *
 * @return an array of index categories
 */
public char[][] getIndexCategories() {
	return CharOperation.NO_CHAR_CHAR; // called from queryIn(), override as necessary
}
/**
 * Returns the rule to apply for matching index keys. Can be exact match, prefix match, pattern match or regexp match.
 * Rule can also be combined with a case sensitivity flag.
 *
 * @return one of R_EXACT_MATCH, R_PREFIX_MATCH, R_PATTERN_MATCH, R_REGEXP_MATCH combined with R_CASE_SENSITIVE,
 *   e.g. R_EXACT_MATCH | R_CASE_SENSITIVE if an exact and case sensitive match is requested,
 *   or R_PREFIX_MATCH if a prefix non case sensitive match is requested.
 */
public final int getMatchRule() {
	return this.matchRule;
}
/**
 * Returns whether this pattern matches the given pattern (representing a decoded index key).
 * <p>
 * This method should be re-implemented in subclasses that need to narrow down the
 * index query.
 * </p>
 *
 * @param decodedPattern a pattern representing a decoded index key
 * @return whether this pattern matches the given pattern
 */
public boolean matchesDecodedKey(SearchPattern decodedPattern) {
	return true; // called from findIndexMatches(), override as necessary if index key is encoded
}

/**
 * Returns whether the given name matches the given pattern.
 * <p>
 * This method should be re-implemented in subclasses that need to define how
 * a name matches a pattern.
 * </p>
 *
 * @param pattern the given pattern, or <code>null</code> to represent "*"
 * @param name the given name
 * @return whether the given name matches the given pattern
 */
public boolean matchesName(char[] pattern, char[] name) {
	if (pattern == null) return true; // null is as if it was "*"
	if (name != null) {
		boolean isCaseSensitive = (this.matchRule & R_CASE_SENSITIVE) != 0;
		boolean isCamelCase = (this.matchRule & R_CAMELCASE_MATCH) != 0;
		int matchMode = this.matchRule & MODE_MASK;
		boolean emptyPattern = pattern.length == 0;
		if (matchMode == R_PREFIX_MATCH && emptyPattern) return true;
		boolean sameLength = pattern.length == name.length;
		boolean canBePrefix = name.length >= pattern.length;
		boolean matchFirstChar = !isCaseSensitive || emptyPattern || (name.length > 0 &&  pattern[0] == name[0]);
		if (isCamelCase && matchFirstChar && CharOperation.camelCaseMatch(pattern, name)) {
			return true;
		}
		switch (matchMode) {
			case R_EXACT_MATCH :
			case R_FULL_MATCH :
				if (!isCamelCase) {
					if (sameLength && matchFirstChar) {
						return CharOperation.equals(pattern, name, isCaseSensitive);
					}
					break;
				}
			//$FALL-THROUGH$ next case to match as prefix if camel case failed
			case R_PREFIX_MATCH :
				if (canBePrefix && matchFirstChar) {
					return CharOperation.prefixEquals(pattern, name, isCaseSensitive);
				}
				break;

			case R_PATTERN_MATCH :
				if (!isCaseSensitive)
					pattern = CharOperation.toLowerCase(pattern);
				return CharOperation.match(pattern, name, isCaseSensitive);

			case R_REGEXP_MATCH : {
				int flags = 0;
				if(!isCaseSensitive) {
					flags = Pattern.CASE_INSENSITIVE;
				}
				return Pattern.compile(new String(pattern), flags).matcher(new String(name)).matches();
			}
		}
	}
	return false;
}

/**
 * <p>Returns whether this pattern's prefix qualifier matches the other patterns qualified name.
 * This pattern's qualifier must have a match rule of pattern match for a match to occur.</p>
 * 
 * @param thisQualifiedName  this qualifier pattern to inspect
 * @param otherQualifiedName other qualifier pattern to inspect 
 * @param isCaseSensitive whether this qualifier pattern is case sensitive
 * @return whether this pattern's prefix qualifier matches the other patterns qualified name
 */
public static boolean matchesQualificationPattern(char[] thisQualifiedName, char[] otherQualifiedName, boolean isCaseSensitive) {
	if (thisQualifiedName != null && isPatternMatch(String.valueOf(thisQualifiedName))) {
		if (otherQualifiedName != null && otherQualifiedName.length > 0) {
			if (!isCaseSensitive)
				thisQualifiedName = CharOperation.toLowerCase(thisQualifiedName);
			return CharOperation.match(thisQualifiedName, otherQualifiedName, isCaseSensitive);
		}
	}
	return false;	
}

/**
 * Validate compatibility between given string pattern and match rule.
 *<br>
 * Optimized (ie. returned match rule is modified) combinations are:
 * <ul>
 * 	<li>{@link #R_PATTERN_MATCH} without any '*' or '?' in string pattern:
 * 		pattern match bit is unset,
 * 	</li>
 * 	<li>{@link #R_PATTERN_MATCH} and {@link #R_PREFIX_MATCH}  bits simultaneously set:
 * 		prefix match bit is unset,
 * 	</li>
 * 	<li>{@link #R_PATTERN_MATCH} and {@link #R_CAMELCASE_MATCH}  bits simultaneously set:
 * 		camel case match bit is unset,
 * 	</li>
 * 	<li>{@link #R_CAMELCASE_MATCH} with invalid combination of uppercase and lowercase characters:
 * 		camel case match bit is unset and replaced with prefix match pattern,
 * 	</li>
 * 	<li>{@link #R_CAMELCASE_MATCH} combined with {@link #R_PREFIX_MATCH} and {@link #R_CASE_SENSITIVE}
 * 		bits is reduced to only {@link #R_CAMELCASE_MATCH} as Camel Case search is already prefix and case sensitive,
 * 	</li>
 * </ul>
 *<br>
 * Rejected (ie. returned match rule -1) combinations are:
 * <ul>
 * 	<li>{@link #R_REGEXP_MATCH} with any other match mode bit set,
 * 	</li>
 * </ul>
 *
 * @param stringPattern The string pattern
 * @param matchRule The match rule
 * @return Optimized valid match rule or -1 if an incompatibility was detected.
 *  
 */
public static int validateMatchRule(String stringPattern, int matchRule) {

	// Verify Regexp match rule
	if ((matchRule & R_REGEXP_MATCH) != 0) {
		if ((matchRule & R_PATTERN_MATCH) != 0 || (matchRule & R_PREFIX_MATCH) != 0 || (matchRule & R_CAMELCASE_MATCH) != 0) {
			return -1;
		}
	}

	// Verify Pattern match rule
	int starIndex = stringPattern.indexOf('*');
	int questionIndex = stringPattern.indexOf('?');
	if (starIndex < 0 && questionIndex < 0) {
		// reset pattern match bit if any
		matchRule &= ~R_PATTERN_MATCH;
	} else {
		// force Pattern rule
		matchRule |= R_PATTERN_MATCH;
	}
	if ((matchRule & R_PATTERN_MATCH) != 0) {
		// remove Camel Case and Prefix match bits if any
		matchRule &= ~R_CAMELCASE_MATCH;
		matchRule &= ~R_PREFIX_MATCH;
	}

	// Verify Camel Case match rule
	if ((matchRule & R_CAMELCASE_MATCH) != 0) {
		// Verify sting pattern validity
		int length = stringPattern.length();
		boolean validCamelCase = true;
		boolean uppercase = false;
		for (int i=0; i<length && validCamelCase; i++) {
			char ch = stringPattern.charAt(i);
			validCamelCase = ScannerHelper.isJavaIdentifierStart(ch);
			// at least one uppercase character is need in CamelCase pattern
			// (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=136313)
			if (!uppercase) uppercase = ScannerHelper.isUpperCase(ch);
		}
		validCamelCase = validCamelCase && uppercase;
		// Verify bits compatibility
		if (validCamelCase) {
			if ((matchRule & R_PREFIX_MATCH) != 0) {
				if ((matchRule & R_CASE_SENSITIVE) != 0) {
					// This is equivalent to Camel Case match rule
					matchRule &= ~R_PREFIX_MATCH;
					matchRule &= ~R_CASE_SENSITIVE;
				}
			}
		} else {
			matchRule &= ~R_CAMELCASE_MATCH;
			if ((matchRule & R_PREFIX_MATCH) == 0) {
				matchRule |= R_PREFIX_MATCH;
				matchRule |= R_CASE_SENSITIVE;
			}
		}
	}
	return matchRule;
}

/**
 * Returns whether the given pattern is a pattern case or not.
 * 
 * @param pattern the pattern to inspect
 * @return whether it is a pattern case or not
 */
public static boolean isPatternMatch(String pattern) {
	return SearchPattern.validateMatchRule(
		pattern, 
		SearchPattern.R_PATTERN_MATCH) == SearchPattern.R_PATTERN_MATCH;
}

/**
 * @see java.lang.Object#toString()
 */
public String toString() {
	return "SearchPattern"; //$NON-NLS-1$
}
}
