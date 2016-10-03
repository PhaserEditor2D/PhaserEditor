/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.codeassist;

import java.util.Locale;
import java.util.Map;

import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.internal.codeassist.impl.AssistParser;
import org.eclipse.wst.jsdt.internal.codeassist.impl.Engine;
import org.eclipse.wst.jsdt.internal.codeassist.select.SelectionNodeFound;
import org.eclipse.wst.jsdt.internal.codeassist.select.SelectionOnImportReference;
import org.eclipse.wst.jsdt.internal.codeassist.select.SelectionOnPackageReference;
import org.eclipse.wst.jsdt.internal.codeassist.select.SelectionOnQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.codeassist.select.SelectionOnSingleTypeReference;
import org.eclipse.wst.jsdt.internal.codeassist.select.SelectionParser;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceType;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalFunctionBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MemberTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.parser.Scanner;
import org.eclipse.wst.jsdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.wst.jsdt.internal.compiler.parser.SourceTypeConverter;
import org.eclipse.wst.jsdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.core.SearchableEnvironment;
import org.eclipse.wst.jsdt.internal.core.SelectionRequestor;
import org.eclipse.wst.jsdt.internal.core.SourceType;
import org.eclipse.wst.jsdt.internal.core.SourceTypeElementInfo;
import org.eclipse.wst.jsdt.internal.core.util.ASTNodeFinder;

/**
 * The selection engine is intended to infer the nature of a selected name in some
 * source code. This name can be qualified.
 *
 * Selection is resolving context using a name environment (no need to search), assuming
 * the source where selection occurred is correct and will not perform any completion
 * attempt. If this was the desired behavior, a call to the CompletionEngine should be
 * performed instead.
 */
public final class SelectionEngine extends Engine implements ISearchRequestor {

	public static boolean DEBUG = false;
	public static boolean PERF = false;

	SelectionParser parser;
	ISelectionRequestor requestor;

	boolean acceptedAnswer;

	private int actualSelectionStart;
	private int actualSelectionEnd;
	private char[] selectedIdentifier;

	private char[][][] acceptedClasses;
	private int[] acceptedClassesModifiers;
	private char[][][] acceptedInterfaces;
	private int[] acceptedInterfacesModifiers;
	private char[][][] acceptedEnums;
	private int[] acceptedEnumsModifiers;
	private char[][][] acceptedAnnotations;
	private int[] acceptedAnnotationsModifiers;
	int acceptedClassesCount;
	int acceptedInterfacesCount;
	int acceptedEnumsCount;
	int acceptedAnnotationsCount;

	boolean noProposal = true;
	CategorizedProblem problem = null;

	/**
	 * The SelectionEngine is responsible for computing the selected object.
	 *
	 * It requires a searchable name environment, which supports some
	 * specific search APIs, and a requestor to feed back the results to a UI.
	 *
	 *  @param nameEnvironment org.eclipse.wst.jsdt.internal.core.SearchableEnvironment
	 *      used to resolve type/package references and search for types/packages
	 *      based on partial names.
	 *
	 *  @param requestor org.eclipse.wst.jsdt.internal.codeassist.ISelectionRequestor
	 *      since the engine might produce answers of various forms, the engine
	 *      is associated with a requestor able to accept all possible completions.
	 *
	 *  @param settings java.util.Map
	 *		set of options used to configure the code assist engine.
	 */
	public SelectionEngine(
		SearchableEnvironment nameEnvironment,
		ISelectionRequestor requestor,
		Map settings) {

		super(settings);

		this.requestor = requestor;
		this.nameEnvironment = nameEnvironment;

		ProblemReporter problemReporter =
			new ProblemReporter(
				DefaultErrorHandlingPolicies.proceedWithAllProblems(),
				this.compilerOptions,
				new DefaultProblemFactory(Locale.getDefault())) {

			public CategorizedProblem createProblem(
				char[] fileName,
				int problemId,
				String[] problemArguments,
				String[] messageArguments,
				int severity,
				int problemStartPosition,
				int problemEndPosition,
				int lineNumber,
				int columnNumber) {
				CategorizedProblem pb =  super.createProblem(
					fileName,
					problemId,
					problemArguments,
					messageArguments,
					severity,
					problemStartPosition,
					problemEndPosition,
					lineNumber,
					columnNumber);
					if(SelectionEngine.this.problem == null && pb.isError() && (pb.getID() & IProblem.Syntax) == 0) {
						SelectionEngine.this.problem = pb;
					}

					return pb;
			}
		};
		this.lookupEnvironment =
			new LookupEnvironment(this, this.compilerOptions, problemReporter, nameEnvironment);
		this.parser = new SelectionParser(problemReporter);
	}

	public void acceptType(char[] packageName, char [] fileName,  char[] simpleTypeName, char[][] enclosingTypeNames, int modifiers, AccessRestriction accessRestriction) {
		char[] typeName = enclosingTypeNames == null ?
				simpleTypeName :
					CharOperation.concat(
						CharOperation.concatWith(enclosingTypeNames, '.'),
						simpleTypeName,
						'.');

		if (CharOperation.equals(simpleTypeName, this.selectedIdentifier)) {
			char[] flatEnclosingTypeNames =
				enclosingTypeNames == null || enclosingTypeNames.length == 0 ?
						null :
							CharOperation.concatWith(enclosingTypeNames, '.');
			if(mustQualifyType(packageName, simpleTypeName, flatEnclosingTypeNames, modifiers)) {
				int length = 0;
				int kind = 0;
				switch (kind) {
					default:
						char[][] acceptedClass = new char[3][];
						acceptedClass[0] = packageName;
						acceptedClass[1] = typeName;
						acceptedClass[2] = fileName;

						if(this.acceptedClasses == null) {
							this.acceptedClasses = new char[10][][];
							this.acceptedClassesModifiers = new int[10];
							this.acceptedClassesCount = 0;
						}
						length = this.acceptedClasses.length;
						if(length == this.acceptedClassesCount) {
							int newLength = (length + 1)* 2;
							System.arraycopy(this.acceptedClasses, 0, this.acceptedClasses = new char[newLength][][], 0, length);
							System.arraycopy(this.acceptedClassesModifiers, 0, this.acceptedClassesModifiers = new int[newLength], 0, length);
						}
						this.acceptedClassesModifiers[this.acceptedClassesCount] = modifiers;
						this.acceptedClasses[this.acceptedClassesCount++] = acceptedClass;
						break;
				}
			} else {
				this.noProposal = false;
				this.requestor.acceptType(
					packageName,
					fileName,
					typeName,
					modifiers,
					false,
					null,
					this.actualSelectionStart,
					this.actualSelectionEnd);
				this.acceptedAnswer = true;
			}
		}
	}

	public void acceptBinding(char[] packageName, char [] filename, char[] simpleTypeName, int bindingType, int modifiers, AccessRestriction accessRestriction) {
		char[] typeName = 		simpleTypeName ;

		if (CharOperation.equals(simpleTypeName, this.selectedIdentifier)) {
			char[] flatEnclosingTypeNames = null;
			if(mustQualifyType(packageName, simpleTypeName, flatEnclosingTypeNames, modifiers)) {
				int length = 0;
				int kind = 0;
				switch (kind) {
					default:
						char[][] acceptedClass = new char[2][];
						acceptedClass[0] = packageName;
						acceptedClass[1] = typeName;

						if(this.acceptedClasses == null) {
							this.acceptedClasses = new char[10][][];
							this.acceptedClassesModifiers = new int[10];
							this.acceptedClassesCount = 0;
						}
						length = this.acceptedClasses.length;
						if(length == this.acceptedClassesCount) {
							int newLength = (length + 1)* 2;
							System.arraycopy(this.acceptedClasses, 0, this.acceptedClasses = new char[newLength][][], 0, length);
							System.arraycopy(this.acceptedClassesModifiers, 0, this.acceptedClassesModifiers = new int[newLength], 0, length);
						}
						this.acceptedClassesModifiers[this.acceptedClassesCount] = modifiers;
						this.acceptedClasses[this.acceptedClassesCount++] = acceptedClass;
						break;
				}
			} else {
				this.noProposal = false;
				this.requestor.acceptType(
					packageName,
					filename,
					typeName,
					modifiers,
					false,
					null,
					this.actualSelectionStart,
					this.actualSelectionEnd);
				this.acceptedAnswer = true;
			}
		}
	}

	/**
	 * One result of the search consists of a new package.
	 * @param packageName char[]
	 *
	 * NOTE - All package names are presented in their readable form:
	 *    Package names are in the form "a.b.c".
	 *    The default package is represented by an empty array.
	 */
	public void acceptPackage(char[] packageName) {
		// implementation of interface method
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.codeassist.ISearchRequestor#acceptConstructor(
	 * 		int, char[], char[][], char[][], java.lang.String, org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction)
	 */
	public void acceptConstructor(int modifiers, char[] typeName,
			char[][] parameterTypes,
			char[][] parameterNames, String path,
			AccessRestriction access) {
		
		// do nothing
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.codeassist.ISearchRequestor#acceptFunction(char[], char[][], char[][], char[], char[], char[], char[], int, java.lang.String)
	 */
	public void acceptFunction(char[] signature, char[][] parameterFullyQualifedTypeNames,
				char[][] parameterNames, char[] returnQualification, char[] returnSimpleName,
				char[] declaringQualification, char[] declaringSimpleName, int modifiers, String path) {
		
		//do nothing
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.codeassist.ISearchRequestor#acceptVariable(char[], char[], char[], char[], char[], int, java.lang.String)
	 */
	public void acceptVariable(char[] signature,
			char[] typeQualification, char[] typeSimpleName,
			char[] declaringQualification, char[] declaringSimpleName,
			int modifiers, String path) {
		
		//do nothing
	}

	private void acceptQualifiedTypes() {
		if(this.acceptedClasses != null){
			this.acceptedAnswer = true;
			for (int i = 0; i < this.acceptedClassesCount; i++) {
				this.noProposal = false;
				this.requestor.acceptType(
					this.acceptedClasses[i][0],
					this.acceptedClasses[i][2],
					this.acceptedClasses[i][1],
					this.acceptedClassesModifiers[i],
					false,
					null,
					this.actualSelectionStart,
					this.actualSelectionEnd);
			}
			this.acceptedClasses = null;
			this.acceptedClassesModifiers = null;
			this.acceptedClassesCount = 0;
		}
		if(this.acceptedInterfaces != null){
			this.acceptedAnswer = true;
			for (int i = 0; i < this.acceptedInterfacesCount; i++) {
				this.noProposal = false;
				this.requestor.acceptType(
					this.acceptedInterfaces[i][0],
					null,
					this.acceptedInterfaces[i][1],
					this.acceptedInterfacesModifiers[i],
					false,
					null,
					this.actualSelectionStart,
					this.actualSelectionEnd);
			}
			this.acceptedInterfaces = null;
			this.acceptedInterfacesModifiers = null;
			this.acceptedInterfacesCount = 0;
		}
		if(this.acceptedAnnotations != null){
			this.acceptedAnswer = true;
			for (int i = 0; i < this.acceptedAnnotationsCount; i++) {
				this.noProposal = false;
				this.requestor.acceptType(
					this.acceptedAnnotations[i][0],
					null,
					this.acceptedAnnotations[i][1],
					this.acceptedAnnotationsModifiers[i],
					false,
					null,
					this.actualSelectionStart,
					this.actualSelectionEnd);
			}
			this.acceptedAnnotations = null;
			this.acceptedAnnotationsModifiers = null;
			this.acceptedAnnotationsCount = 0;
		}
		if(this.acceptedEnums != null){
			this.acceptedAnswer = true;
			for (int i = 0; i < this.acceptedEnumsCount; i++) {
				this.noProposal = false;
				this.requestor.acceptType(
					this.acceptedEnums[i][0],
					null,
					this.acceptedEnums[i][1],
					this.acceptedEnumsModifiers[i],
					false,
					null,
					this.actualSelectionStart,
					this.actualSelectionEnd);
			}
			this.acceptedEnums = null;
			this.acceptedEnumsModifiers = null;
			this.acceptedEnumsCount = 0;
		}
	}
	private boolean checkSelection(
		char[] source,
		int selectionStart,
		int selectionEnd) {

		Scanner scanner = new Scanner();
		scanner.setSource(source);

		int lastIdentifierStart = -1;
		int lastIdentifierEnd = -1;
		char[] lastIdentifier = null;
		int token;

		if(selectionStart > selectionEnd){

			// compute end position of the selection
			int end = selectionEnd + 1 == source.length ? selectionEnd : selectionEnd + 1;
			// compute start position of current line
			int currentPosition = selectionStart - 1;
			int nextCharacterPosition = selectionStart;
			char currentCharacter = ' ';
			boolean brokeLoop=false;
			try {
				lineLoop: while(currentPosition > 0){

					if(source[currentPosition] == '\\' && source[currentPosition+1] == 'u') {
						int pos = currentPosition + 2;
						int c1 = 0, c2 = 0, c3 = 0, c4 = 0;
						while (source[pos] == 'u') {
							pos++;
						}
						int endOfUnicode = pos + 3;
						if (end < endOfUnicode) {
							if (endOfUnicode < source.length) {
								end = endOfUnicode;
							} else {
								return false; // not enough characters to decode an unicode
							}
						}

						if ((c1 = ScannerHelper.getNumericValue(source[pos++])) > 15
							|| c1 < 0
							|| (c2 = ScannerHelper.getNumericValue(source[pos++])) > 15
							|| c2 < 0
							|| (c3 = ScannerHelper.getNumericValue(source[pos++])) > 15
							|| c3 < 0
							|| (c4 = ScannerHelper.getNumericValue(source[pos++])) > 15
							|| c4 < 0) {
							return false;
						} else {
							currentCharacter = (char) (((c1 * 16 + c2) * 16 + c3) * 16 + c4);
							nextCharacterPosition = pos;
						}
					} else {
						currentCharacter = source[currentPosition];
						nextCharacterPosition = currentPosition+1;
					}

					switch(currentCharacter) {
						case '\r':
						case '\n':
						case '/':
						case '"':
						case '\'':
							brokeLoop=true;
							break lineLoop;
					}
					currentPosition--;
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				return false;
			}
			if (!brokeLoop)
				nextCharacterPosition=currentPosition;
			
			// compute start and end of the last token
			scanner.resetTo(nextCharacterPosition, end);
			do {
				try {
					token = scanner.getNextToken();
				} catch (InvalidInputException e) {
					return false;
				}
				switch (token) {
					case TerminalTokens.TokenNamethis:
					case TerminalTokens.TokenNamesuper:
					case TerminalTokens.TokenNameIdentifier:
						if (scanner.startPosition <= selectionStart && selectionStart <= scanner.currentPosition) {
							if (scanner.currentPosition == scanner.eofPosition) {
								int temp = scanner.eofPosition;
								scanner.eofPosition = scanner.source.length;
							 	while(scanner.getNextCharAsJavaIdentifierPart()){/*empty*/}
							 	scanner.eofPosition = temp;
							}
							lastIdentifierStart = scanner.startPosition;
							lastIdentifierEnd = scanner.currentPosition - 1;
							lastIdentifier = scanner.getCurrentTokenSource();
						}
						break;
				}
			} while (token != TerminalTokens.TokenNameEOF);
		} else {
			scanner.resetTo(selectionStart, selectionEnd);

			boolean expectingIdentifier = true;
			try {
				do {
					token = scanner.getNextToken();

					switch (token) {
						case TerminalTokens.TokenNamethis :
						case TerminalTokens.TokenNamesuper :
						case TerminalTokens.TokenNameIdentifier :
							if (!expectingIdentifier)
								return false;
							lastIdentifier = scanner.getCurrentTokenSource();
							lastIdentifierStart = scanner.startPosition;
							lastIdentifierEnd = scanner.currentPosition - 1;
							if(lastIdentifierEnd > selectionEnd) {
								lastIdentifierEnd = selectionEnd;
								lastIdentifier = CharOperation.subarray(lastIdentifier, 0,lastIdentifierEnd - lastIdentifierStart + 1);
							}

							expectingIdentifier = false;
							break;
						case TerminalTokens.TokenNameDOT :
							if (expectingIdentifier)
								return false;
							expectingIdentifier = true;
							break;
						case TerminalTokens.TokenNameEOF :
							if (expectingIdentifier)
								return false;
							break;
						case TerminalTokens.TokenNameLESS :
							if(!checkTypeArgument(scanner))
								return false;
							break;
						default :
							return false;
					}
				} while (token != TerminalTokens.TokenNameEOF);
			} catch (InvalidInputException e) {
				return false;
			}
		}
		if (lastIdentifierStart >= 0) {
			this.actualSelectionStart = lastIdentifierStart;
			this.actualSelectionEnd = lastIdentifierEnd;
			this.selectedIdentifier = lastIdentifier;
			return true;
		}
		return false;
	}
	private boolean checkTypeArgument(Scanner scanner) throws InvalidInputException {
		int depth = 1;
		int token;
		StringBuffer buffer = new StringBuffer();
		do {
			token = scanner.getNextToken();

			switch(token) {
				case TerminalTokens.TokenNameLESS :
					depth++;
					buffer.append(scanner.getCurrentTokenSource());
					break;
				case TerminalTokens.TokenNameGREATER :
					depth--;
					buffer.append(scanner.getCurrentTokenSource());
					break;
				case TerminalTokens.TokenNameRIGHT_SHIFT :
					depth-=2;
					buffer.append(scanner.getCurrentTokenSource());
					break;
				case TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT :
					depth-=3;
					buffer.append(scanner.getCurrentTokenSource());
					break;
				case TerminalTokens.TokenNameextends :
				case TerminalTokens.TokenNamesuper :
					buffer.append(' ');
					buffer.append(scanner.getCurrentTokenSource());
					buffer.append(' ');
					break;
				case TerminalTokens.TokenNameCOMMA :
					if(depth == 1) {
						int length = buffer.length();
						char[] typeRef = new char[length];
						buffer.getChars(0, length, typeRef, 0);
						try {
							Signature.createTypeSignature(typeRef, true);
							buffer = new StringBuffer();
						} catch(IllegalArgumentException e) {
							return false;
						}
					}
					break;
				default :
					buffer.append(scanner.getCurrentTokenSource());
					break;

			}
			if(depth < 0) {
				return false;
			}
		} while (depth != 0 && token != TerminalTokens.TokenNameEOF);

		if(depth == 0) {
			int length = buffer.length() - 1;
			char[] typeRef = new char[length];
			buffer.getChars(0, length, typeRef, 0);
			try {
				Signature.createTypeSignature(typeRef, true);
				return true;
			} catch(IllegalArgumentException e) {
				return false;
			}
		}

		return false;
	}

	public AssistParser getParser() {
		return this.parser;
	}

	/*
	 * Returns whether the given binding is a local/anonymous reference binding, or if its declaring class is
	 * local.
	 */
	private boolean isLocal(ReferenceBinding binding) {
		if (!(binding instanceof SourceTypeBinding)) return false;
		if (binding instanceof LocalTypeBinding) return true;
		if (binding instanceof MemberTypeBinding) {
			return isLocal(((MemberTypeBinding)binding).enclosingType);
		}
		return false;
	}

	/**
	 * Ask the engine to compute the selection at the specified position
	 * of the given compilation unit.

	 *  @param sourceUnit org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit
	 *      the source of the current compilation unit.
	 *
	 *  @param selectionSourceStart int
	 *  @param selectionSourceEnd int
	 *      a range in the source where the selection is.
	 */
	public void select(
		ICompilationUnit sourceUnit,
		int selectionSourceStart,
		int selectionSourceEnd) {

		char[] source = sourceUnit.getContents();

		if(DEBUG) {
			System.out.print("SELECTION IN "); //$NON-NLS-1$
			System.out.print(sourceUnit.getFileName());
			System.out.print(" FROM "); //$NON-NLS-1$
			System.out.print(selectionSourceStart);
			System.out.print(" TO "); //$NON-NLS-1$
			System.out.println(selectionSourceEnd);
			System.out.println("SELECTION - Source :"); //$NON-NLS-1$
			System.out.println(source);
		}
		if (!checkSelection(source, selectionSourceStart, selectionSourceEnd)) {
			return;
		}
		if (DEBUG) {
			System.out.print("SELECTION - Checked : \""); //$NON-NLS-1$
			System.out.print(new String(source, actualSelectionStart, actualSelectionEnd-actualSelectionStart+1));
			System.out.println('"');
		}
		try {
			this.acceptedAnswer = false;
			CompilationResult result = new CompilationResult(sourceUnit, 1, 1, this.compilerOptions.maxProblemsPerUnit);
			CompilationUnitDeclaration parsedUnit =
				this.parser.dietParse(sourceUnit, result, this.actualSelectionStart, this.actualSelectionEnd);

			if (parsedUnit != null) {
				if(DEBUG) {
					System.out.println("SELECTION - Diet AST :"); //$NON-NLS-1$
					System.out.println(parsedUnit.toString());
				}

				// check for inferred types declared with their names in the selection
				this.parser.inferTypes(parsedUnit, this.compilerOptions);
				for (int i = 0; i < parsedUnit.numberInferredTypes; i++) {
					if (parsedUnit.inferredTypes[i] != null && parsedUnit.inferredTypes[i].isDefinition() && parsedUnit.inferredTypes[i].getNameStart() <= selectionSourceEnd && selectionSourceStart <= parsedUnit.inferredTypes[i].getNameStart() +  parsedUnit.inferredTypes[i].getName().length) {
						this.requestor.acceptType(CharOperation.NO_CHAR, sourceUnit.getFileName(), parsedUnit.inferredTypes[i].getName(), 0, parsedUnit.inferredTypes[i].isDefinition(), CharOperation.NO_CHAR, parsedUnit.inferredTypes[i].sourceStart, parsedUnit.inferredTypes[i].sourceEnd);
					}
				}
				// scan the package & import statements first
				if (parsedUnit.currentPackage instanceof SelectionOnPackageReference) {
					char[][] tokens =
						((SelectionOnPackageReference) parsedUnit.currentPackage).tokens;
					this.noProposal = false;
					this.requestor.acceptPackage(CharOperation.concatWith(tokens, '.'));
					return;
				}
				ImportReference[] imports = parsedUnit.imports;
				if (imports != null) {
					for (int i = 0, length = imports.length; i < length; i++) {
						ImportReference importReference = imports[i];
						if (importReference instanceof SelectionOnImportReference) {
							char[][] tokens = ((SelectionOnImportReference) importReference).tokens;
							this.noProposal = false;
							this.requestor.acceptPackage(CharOperation.concatWith(tokens, '.'));
							this.nameEnvironment.findTypes(CharOperation.concatWith(tokens, '.'), false, false, IJavaScriptSearchConstants.TYPE, this);

							this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
							if ((this.unitScope = parsedUnit.scope) != null) {
								int tokenCount = tokens.length;
								char[] lastToken = tokens[tokenCount - 1];
								char[][] qualifierTokens = CharOperation.subarray(tokens, 0, tokenCount - 1);

								if(qualifierTokens != null && qualifierTokens.length > 0) {
									Binding binding = this.unitScope.getTypeOrPackage(qualifierTokens);
									if(binding != null && binding instanceof ReferenceBinding) {
										ReferenceBinding ref = (ReferenceBinding) binding;
										selectMemberTypeFromImport(parsedUnit, lastToken, ref);
									}
								}
							}

							// accept qualified types only if no unqualified type was accepted
							if(!this.acceptedAnswer) {
								acceptQualifiedTypes();
								if (!this.acceptedAnswer) {
									this.nameEnvironment.findTypes(this.selectedIdentifier, false, false, IJavaScriptSearchConstants.TYPE, this);
									// try with simple type name
									if(!this.acceptedAnswer) {
										acceptQualifiedTypes();
									}
								}
							}
							if(this.noProposal && this.problem != null) {
								this.requestor.acceptError(this.problem);
							}
							return;
						}
					}
				}
				if (parsedUnit.statements != null || parsedUnit.isPackageInfo()) {
					if(selectDeclaration(parsedUnit))
						return;
					try {
						/* We must build bindings to be able to resolve a reference (might not
						 * require completing), however this may itself cause SelectionNodeFound
						 * to be thrown */
						this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/, true);
						if ((this.unitScope = parsedUnit.scope)  != null) {
							this.lookupEnvironment.completeTypeBindings(parsedUnit, true);
							parsedUnit.scope.faultInTypes();
							ASTNode node = null;
							if (parsedUnit.types != null)
								node = parseBlockStatements(parsedUnit, selectionSourceStart);
							if(DEBUG) {
								System.out.println("SELECTION - AST :"); //$NON-NLS-1$
								System.out.println(parsedUnit.toString());
							}
							parsedUnit.resolve();
							if (node != null) {
								selectLocalDeclaration(node);
							}
						}
					} catch (SelectionNodeFound e) {
						if (e.binding != null) {
							if(DEBUG) {
								System.out.println("SELECTION - Selection binding:"); //$NON-NLS-1$
								System.out.println(e.binding.toString());
							}
							// if null then we found a problem in the selection node
							selectFrom(e.binding, parsedUnit, e.isDeclaration);
						}
					}
				}
			}
			// only reaches here if no selection could be derived from the parsed tree
			// thus use the selected source and perform a textual type search
			if (!this.acceptedAnswer) {
				this.nameEnvironment.findTypes(this.selectedIdentifier, false, false, IJavaScriptSearchConstants.TYPE, this);

				// accept qualified types only if no unqualified type was accepted
				if(!this.acceptedAnswer) {
					acceptQualifiedTypes();
				}
			}
			if(this.noProposal && this.problem != null) {
				this.requestor.acceptError(this.problem);
			}
		} catch (IndexOutOfBoundsException e) { // work-around internal failure - 1GEMF6D
			if(DEBUG) {
				System.out.println("Exception caught by SelectionEngine:"); //$NON-NLS-1$
				e.printStackTrace(System.out);
			}
		} catch (AbortCompilation e) { // ignore this exception for now since it typically means we cannot find java.lang.Object
			if(DEBUG) {
				System.out.println("Exception caught by SelectionEngine:"); //$NON-NLS-1$
				e.printStackTrace(System.out);
			}
		} finally {
			reset();
		}
	}

	private void selectMemberTypeFromImport(CompilationUnitDeclaration parsedUnit, char[] lastToken, ReferenceBinding ref) {
		int fieldLength = lastToken.length;
		ReferenceBinding[] memberTypes = ref.memberTypes();
		next : for (int j = 0; j < memberTypes.length; j++) {
			ReferenceBinding memberType = memberTypes[j];

			if (fieldLength > memberType.sourceName.length)
				continue next;

			if (!CharOperation.equals(lastToken, memberType.sourceName, true))
				continue next;

			this.selectFrom(memberType, parsedUnit, false);
		}
	}

	private void selectFrom(Binding binding, CompilationUnitDeclaration parsedUnit, boolean isDeclaration) {
		if (binding instanceof ReferenceBinding) {
			ReferenceBinding typeBinding = (ReferenceBinding) binding;
			if(typeBinding instanceof ProblemReferenceBinding) {
				typeBinding = typeBinding.closestMatch();
			}
			if (typeBinding == null) return;
			if (isLocal(typeBinding) && this.requestor instanceof SelectionRequestor) {
				this.noProposal = false;
				((SelectionRequestor)this.requestor).acceptLocalType(typeBinding);
			} else {
				this.noProposal = false;

				this.requestor.acceptType(
					typeBinding.qualifiedPackageName(),
					typeBinding.getFileName(),
					typeBinding.qualifiedSourceName(),
					typeBinding.modifiers,
					false,
					typeBinding.computeUniqueKey(),
					this.actualSelectionStart,
					this.actualSelectionEnd);
			}
			this.acceptedAnswer = true;
		} else
			if (binding instanceof MethodBinding) {
				MethodBinding methodBinding = (MethodBinding) binding;
				this.noProposal = false;

				boolean isValuesOrValueOf = false;

				if(!isValuesOrValueOf) {
					TypeBinding[] parameterTypes = methodBinding.original().parameters;
					int length = parameterTypes.length;
					char[][] parameterPackageNames = new char[length][];
					char[][] parameterTypeNames = new char[length][];
					String[] parameterSignatures = new String[length];
					for (int i = 0; i < length; i++) {
						parameterPackageNames[i] = parameterTypes[i].qualifiedPackageName();
						parameterTypeNames[i] = parameterTypes[i].qualifiedSourceName();
						parameterSignatures[i] = new String(getSignature(parameterTypes[i])).replace('/', '.');
					}

					char[][] typeParameterNames = new char[length][];
					char[][][] typeParameterBoundNames = new char[length][][];

					ReferenceBinding declaringClass = methodBinding.declaringClass;
					if (	( ( methodBinding instanceof LocalFunctionBinding || isLocal(declaringClass))
									&& this.requestor instanceof SelectionRequestor)
							|| declaringClass.qualifiedSourceName()==null) {
						((SelectionRequestor)this.requestor).acceptLocalMethod(methodBinding);
					} else {
						this.requestor.acceptMethod(
							/*declaringClass.qualifiedPackageName()*/ CharOperation.NO_CHAR,
							declaringClass.getFileName(),
							declaringClass.qualifiedSourceName(),
							declaringClass.enclosingType() == null ? null : new String(getSignature(declaringClass.enclosingType())),
							methodBinding.isConstructor()
								? declaringClass.sourceName()
								: methodBinding.selector,
							parameterPackageNames,
							parameterTypeNames,
							parameterSignatures,
							typeParameterNames,
							typeParameterBoundNames,
							methodBinding.isConstructor(),
							isDeclaration,
							methodBinding.computeUniqueKey(),
							this.actualSelectionStart,
							this.actualSelectionEnd);
					}
				}
				this.acceptedAnswer = true;
			} else
				if (binding instanceof FieldBinding) {
					FieldBinding fieldBinding = (FieldBinding) binding;
					ReferenceBinding declaringClass = fieldBinding.declaringClass;
					if (declaringClass != null) { // arraylength
						this.noProposal = false;
						if (isLocal(declaringClass) && this.requestor instanceof SelectionRequestor) {
							((SelectionRequestor)this.requestor).acceptLocalField(fieldBinding);
						} else {
							this.requestor.acceptField(
								declaringClass.qualifiedPackageName(),
								declaringClass.getFileName(),
								declaringClass.qualifiedSourceName(),
								fieldBinding.name,
								false,
								fieldBinding.computeUniqueKey(),
								this.actualSelectionStart,
								this.actualSelectionEnd);
						}
						this.acceptedAnswer = true;
					}
				} else
					if (binding instanceof LocalVariableBinding) {
						this.noProposal = false;
						if (this.requestor instanceof SelectionRequestor) {
							((SelectionRequestor)this.requestor).acceptLocalVariable((LocalVariableBinding)binding);
							this.acceptedAnswer = true;
						} else {
							// open on the type of the variable
							selectFrom(((LocalVariableBinding) binding).type, parsedUnit, false);
						}
					} else
						if (binding instanceof ArrayBinding) {
							selectFrom(((ArrayBinding) binding).leafComponentType, parsedUnit, false);
							// open on the type of the array
						} else
							if (binding instanceof PackageBinding) {
								PackageBinding packageBinding = (PackageBinding) binding;
								this.noProposal = false;
								this.requestor.acceptPackage(packageBinding.readableName());
								this.acceptedAnswer = true;
							} else
								if(binding instanceof BaseTypeBinding) {
									this.acceptedAnswer = true;
								}
	}

	/*
	 * Checks if a local declaration got selected in this method/initializer/field.
	 */
	private void selectLocalDeclaration(ASTNode node) {
		// the selected identifier is not identical to the parser one (equals but not identical),
		// for traversing the parse tree, the parser assist identifier is necessary for identitiy checks
		final char[] assistIdentifier = this.getParser().assistIdentifier();
		if (assistIdentifier == null) return;

		class Visitor extends ASTVisitor {
			public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
				if (constructorDeclaration.getName() == assistIdentifier){
					if (constructorDeclaration.getBinding() != null) {
						throw new SelectionNodeFound(constructorDeclaration.getBinding());
					} else {
						if (constructorDeclaration.getScope() != null) {
							throw new SelectionNodeFound(new MethodBinding(constructorDeclaration.modifiers, constructorDeclaration.getName(), null, null, constructorDeclaration.getScope().referenceType().binding));
						}
					}
				}
				return true;
			}
			public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
				if (fieldDeclaration.name == assistIdentifier){
					throw new SelectionNodeFound(fieldDeclaration.binding);
				}
				return true;
			}
			public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
				if (localTypeDeclaration.name == assistIdentifier) {
					throw new SelectionNodeFound(localTypeDeclaration.binding);
				}
				return true;
			}
			public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
				if (memberTypeDeclaration.name == assistIdentifier) {
					throw new SelectionNodeFound(memberTypeDeclaration.binding);
				}
				return true;
			}
			public boolean visit(MethodDeclaration methodDeclaration, Scope scope) {
				if (methodDeclaration.getName() == assistIdentifier){
					if (methodDeclaration.getBinding() != null) {
						throw new SelectionNodeFound(methodDeclaration.getBinding());
					} else {
						if (methodDeclaration.getScope() != null) {
							throw new SelectionNodeFound(new MethodBinding(methodDeclaration.modifiers, methodDeclaration.getName(), null, null, methodDeclaration.getScope().referenceType().binding));
						}
					}
				}
				return true;
			}
			public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
				if (typeDeclaration.name == assistIdentifier) {
					throw new SelectionNodeFound(typeDeclaration.binding);
				}
				return true;
			}
		}

		if (node instanceof AbstractMethodDeclaration) {
			((AbstractMethodDeclaration)node).traverse(new Visitor(), (ClassScope)null);
		} else {
			((FieldDeclaration)node).traverse(new Visitor(), (MethodScope)null);
		}
	}

	/**
	 * Asks the engine to compute the selection of the given type
	 * from the source type.
	 *
	 *  @param sourceType org.eclipse.wst.jsdt.internal.compiler.env.ISourceType
	 *      a source form of the current type in which code assist is invoked.
	 *
	 *  @param typeName char[]
	 *      a type name which is to be resolved in the context of a compilation unit.
	 *		NOTE: the type name is supposed to be correctly reduced (no whitespaces, no unicodes left)
	 *
	 * @param topLevelTypes SourceTypeElementInfo[]
	 *      a source form of the top level types of the compilation unit in which code assist is invoked.

	 *  @param searchInEnvironment
	 * 	if <code>true</code> and no selection could be found in context then search type in environment.
	 */
	public void selectType(ISourceType sourceType, char[] typeName, SourceTypeElementInfo[] topLevelTypes, boolean searchInEnvironment) {
		try {
			this.acceptedAnswer = false;

			// only the type erasure are returned by IType.resolvedType(...)
			if (CharOperation.indexOf('<', typeName) != -1) {
				char[] typeSig = Signature.createCharArrayTypeSignature(typeName, false/*not resolved*/);
				typeName = Signature.toCharArray(typeSig);
			}

			// find the outer most type
			ISourceType outerType = sourceType;
			ISourceType parent = sourceType.getEnclosingType();
			while (parent != null) {
				outerType = parent;
				parent = parent.getEnclosingType();
			}
			// compute parse tree for this most outer type
			CompilationResult result = new CompilationResult(outerType.getFileName(), outerType.getPackageName(), 1, 1, this.compilerOptions.maxProblemsPerUnit);
			if (!(sourceType instanceof SourceTypeElementInfo)) return;
			SourceType typeHandle = (SourceType) ((SourceTypeElementInfo)sourceType).getHandle();
			int flags = SourceTypeConverter.FIELD_AND_METHOD | SourceTypeConverter.MEMBER_TYPE;
			if (typeHandle.isAnonymous() || typeHandle.isLocal())
				flags |= SourceTypeConverter.LOCAL_TYPE;
			CompilationUnitDeclaration parsedUnit =
				SourceTypeConverter.buildCompilationUnit(
						topLevelTypes,
						flags,
						this.parser.problemReporter(),
						result);

			if (parsedUnit != null && parsedUnit.types != null) {
				if(DEBUG) {
					System.out.println("SELECTION - Diet AST :"); //$NON-NLS-1$
					System.out.println(parsedUnit.toString());
				}
				// find the type declaration that corresponds to the original source type
				TypeDeclaration typeDecl = new ASTNodeFinder(parsedUnit).findType(typeHandle);

				if (typeDecl != null) {

					// add fake field with the type we're looking for
					// note: since we didn't ask for fields above, there is no field defined yet
					FieldDeclaration field = new FieldDeclaration();
					int dot;
					if ((dot = CharOperation.lastIndexOf('.', typeName)) == -1) {
						this.selectedIdentifier = typeName;
						field.type = new SelectionOnSingleTypeReference(typeName, -1);
						// position not used
					} else {
						char[][] previousIdentifiers = CharOperation.splitOn('.', typeName, 0, dot);
						char[] selectionIdentifier =
							CharOperation.subarray(typeName, dot + 1, typeName.length);
						this.selectedIdentifier = selectionIdentifier;
						field.type =
							new SelectionOnQualifiedTypeReference(
								previousIdentifiers,
								selectionIdentifier,
								new long[previousIdentifiers.length + 1]);
					}
					field.name = "<fakeField>".toCharArray(); //$NON-NLS-1$
					typeDecl.fields = new FieldDeclaration[] { field };

					// build bindings
					this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
					if ((this.unitScope = parsedUnit.scope) != null) {
						try {
							// build fields
							// note: this builds fields only in the parsed unit (the buildFieldsAndMethods flag is not passed along)
							this.lookupEnvironment.completeTypeBindings(parsedUnit, true);

							// resolve
							parsedUnit.scope.faultInTypes();
							parsedUnit.resolve();
						} catch (SelectionNodeFound e) {
							if (e.binding != null) {
								if(DEBUG) {
									System.out.println("SELECTION - Selection binding :"); //$NON-NLS-1$
									System.out.println(e.binding.toString());
								}
								// if null then we found a problem in the selection node
								selectFrom(e.binding, parsedUnit, e.isDeclaration);
							}
						}
					}
				}
			}
			// only reaches here if no selection could be derived from the parsed tree
			// thus use the selected source and perform a textual type search
			if (!this.acceptedAnswer && searchInEnvironment) {
				if (this.selectedIdentifier != null) {
					this.nameEnvironment.findTypes(typeName, false, false, IJavaScriptSearchConstants.TYPE, this);

					// accept qualified types only if no unqualified type was accepted
					if(!this.acceptedAnswer) {
						acceptQualifiedTypes();
					}
				}
			}
			if(this.noProposal && this.problem != null) {
				this.requestor.acceptError(this.problem);
			}
		} catch (AbortCompilation e) { // ignore this exception for now since it typically means we cannot find java.lang.Object
		} finally {
			reset();
		}
	}

	// Check if a declaration got selected in this unit
	private boolean selectDeclaration(CompilationUnitDeclaration compilationUnit){

		// the selected identifier is not identical to the parser one (equals but not identical),
		// for traversing the parse tree, the parser assist identifier is necessary for identity checks
		char[] assistIdentifier = this.getParser().assistIdentifier();
		if (assistIdentifier == null) return false;

		ImportReference currentPackage = compilationUnit.currentPackage;
		char[] packageName = currentPackage == null ? CharOperation.NO_CHAR : CharOperation.concatWith(currentPackage.tokens, '.');
		// iterate over the types
		TypeDeclaration[] types = compilationUnit.types;
		for (int i = 0, length = types == null ? 0 : types.length; i < length; i++){
			if(selectDeclaration(types[i], assistIdentifier, packageName))
				return true;
		}
		ProgramElement[] statements = compilationUnit.statements;
		for (int i = 0, length = statements == null ? 0 : statements.length; i < length; i++){
			if (statements[i] instanceof FieldDeclaration)
			{
				FieldDeclaration field = (FieldDeclaration)statements[i];

				if (field.name == assistIdentifier){
					char[] qualifiedSourceName = null;

					this.requestor.acceptField(
						packageName,
						compilationUnit.getFileName(),
						qualifiedSourceName,
						field.name,
						true,
						field.binding != null ? field.binding.computeUniqueKey() : null,
						this.actualSelectionStart,
						this.actualSelectionEnd);

					this.noProposal = false;
					return true;
				}
			}
			else if (statements[i] instanceof AbstractMethodDeclaration)
			{
			   AbstractMethodDeclaration  method  = (AbstractMethodDeclaration)statements[i];

				if (method.getName() == assistIdentifier){
					char[] qualifiedSourceName = compilationUnit.getFileName();

					this.requestor.acceptMethod(
						packageName,
						compilationUnit.getFileName(),
						qualifiedSourceName,
						null, // SelectionRequestor does not need of declaring type signature for method declaration
						method.getName(),
						null, // SelectionRequestor does not need of parameters type for method declaration
						null, // SelectionRequestor does not need of parameters type for method declaration
						null, // SelectionRequestor does not need of parameters type for method declaration
						null,null,
						method.isConstructor(),
						true,
						method.hasBinding() ? method.getBinding().computeUniqueKey() : null,
						this.actualSelectionStart,
						this.actualSelectionEnd);

					this.noProposal = false;
					return true;
				}

			}

		}
		return false;
	}

	// Check if a declaration got selected in this type
	private boolean selectDeclaration(TypeDeclaration typeDeclaration, char[] assistIdentifier, char[] packageName){

		if (typeDeclaration.name == assistIdentifier){
			char[] qualifiedSourceName = null;

			TypeDeclaration enclosingType = typeDeclaration;
			while(enclosingType != null) {
				qualifiedSourceName = CharOperation.concat(enclosingType.name, qualifiedSourceName, '.');
				enclosingType = enclosingType.enclosingType;
			}
			char[] uniqueKey = typeDeclaration.binding != null ? typeDeclaration.binding.computeUniqueKey() : null;

			this.requestor.acceptType(
				packageName,
				null,
				qualifiedSourceName,
				typeDeclaration.modifiers,
				true,
				uniqueKey,
				this.actualSelectionStart,
				this.actualSelectionEnd);

			this.noProposal = false;
			return true;
		}
		TypeDeclaration[] memberTypes = typeDeclaration.memberTypes;
		for (int i = 0, length = memberTypes == null ? 0 : memberTypes.length; i < length; i++){
			if(selectDeclaration(memberTypes[i], assistIdentifier, packageName))
				return true;
		}
		FieldDeclaration[] fields = typeDeclaration.fields;
		for (int i = 0, length = fields == null ? 0 : fields.length; i < length; i++){
			if (fields[i].name == assistIdentifier){
				char[] qualifiedSourceName = null;

				TypeDeclaration enclosingType = typeDeclaration;
				while(enclosingType != null) {
					qualifiedSourceName = CharOperation.concat(enclosingType.name, qualifiedSourceName, '.');
					enclosingType = enclosingType.enclosingType;
				}
				FieldDeclaration field = fields[i];
				this.requestor.acceptField(
					packageName,
					null,
					qualifiedSourceName,
					field.name,
					true,
					field.binding != null ? field.binding.computeUniqueKey() : null,
					this.actualSelectionStart,
					this.actualSelectionEnd);

				this.noProposal = false;
				return true;
			}
		}
		AbstractMethodDeclaration[] methods = typeDeclaration.methods;
		for (int i = 0, length = methods == null ? 0 : methods.length; i < length; i++){
			AbstractMethodDeclaration method = methods[i];

			if (method.getName() == assistIdentifier){
				char[] qualifiedSourceName = null;

				TypeDeclaration enclosingType = typeDeclaration;
				while(enclosingType != null) {
					qualifiedSourceName = CharOperation.concat(enclosingType.name, qualifiedSourceName, '.');
					enclosingType = enclosingType.enclosingType;
				}

				this.requestor.acceptMethod(
					packageName,
					null,
					qualifiedSourceName,
					null, // SelectionRequestor does not need of declaring type signature for method declaration
					method.getName(),
					null, // SelectionRequestor does not need of parameters type for method declaration
					null, // SelectionRequestor does not need of parameters type for method declaration
					null, // SelectionRequestor does not need of parameters type for method declaration
					null, // SelectionRequestor does not need of type parameters name for method declaration
					null, // SelectionRequestor does not need of type parameters bounds for method declaration
					method.isConstructor(),
					true,
					method.hasBinding() ? method.getBinding().computeUniqueKey() : null,
					this.actualSelectionStart,
					this.actualSelectionEnd);

				this.noProposal = false;
				return true;
			}

		}

		return false;
	}
	public CompilationUnitDeclaration doParse(ICompilationUnit unit, AccessRestriction accessRestriction) {
		CompilationResult unitResult =
			new CompilationResult(unit, 1, 1, this.compilerOptions.maxProblemsPerUnit);
		try {
			Parser localParser = new Parser(this.parser.problemReporter(), this.compilerOptions.parseLiteralExpressionsAsConstants);

			CompilationUnitDeclaration parsedUnit = localParser.parse(unit, unitResult);
			localParser.inferTypes(parsedUnit,this.compilerOptions);
			return parsedUnit;
		} catch (AbortCompilationUnit e) {
			throw e; // want to abort enclosing request to compile
		}

	}
}
