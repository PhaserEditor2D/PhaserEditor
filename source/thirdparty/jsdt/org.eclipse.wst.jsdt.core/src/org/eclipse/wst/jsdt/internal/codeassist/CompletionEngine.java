/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Michael Spector <spektom@gmail.com> Bug 242989 
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.codeassist;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import org.eclipse.wst.jsdt.core.CompletionContext;
import org.eclipse.wst.jsdt.core.CompletionFlags;
import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.core.CompletionRequestor;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IAccessRule;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.infer.IInferEngine;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionNodeFound;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnArgumentName;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnBrankStatementLabel;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnClassLiteralAccess;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnExplicitConstructorCall;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnFieldName;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnFieldType;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnImportReference;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnJavadoc;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnJavadocAllocationExpression;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnJavadocFieldReference;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnJavadocMessageSend;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnJavadocParamNameReference;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnJavadocQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnJavadocSingleTypeReference;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnJavadocTag;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnKeyword;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnLocalName;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnMemberAccess;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnMessageSend;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnMessageSendName;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnMethodName;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnQualifiedAllocationExpression;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnQualifiedNameReference;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnQualifiedType;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnSingleNameReference;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnSingleTypeName;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnSingleTypeReference;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionOnStringLiteral;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionParser;
import org.eclipse.wst.jsdt.internal.codeassist.complete.CompletionScanner;
import org.eclipse.wst.jsdt.internal.codeassist.complete.InvalidCursorLocation;
import org.eclipse.wst.jsdt.internal.codeassist.impl.AssistParser;
import org.eclipse.wst.jsdt.internal.codeassist.impl.Engine;
import org.eclipse.wst.jsdt.internal.codeassist.impl.Keywords;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Assignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Initializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocImplicitTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocSingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.OperatorExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds;
import org.eclipse.wst.jsdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SuperReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TryStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceType;
import org.eclipse.wst.jsdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.wst.jsdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FunctionTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ImportBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalFunctionBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MetatdataTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.compiler.lookup.VariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.parser.JavadocTagConstants;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.parser.Scanner;
import org.eclipse.wst.jsdt.internal.compiler.parser.SourceTypeConverter;
import org.eclipse.wst.jsdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.wst.jsdt.internal.compiler.util.ObjectVector;
import org.eclipse.wst.jsdt.internal.compiler.util.SuffixConstants;
import org.eclipse.wst.jsdt.internal.core.BasicCompilationUnit;
import org.eclipse.wst.jsdt.internal.core.BinaryTypeConverter;
import org.eclipse.wst.jsdt.internal.core.INamingRequestor;
import org.eclipse.wst.jsdt.internal.core.InternalNamingConventions;
import org.eclipse.wst.jsdt.internal.core.SearchableEnvironment;
import org.eclipse.wst.jsdt.internal.core.SourceMethod;
import org.eclipse.wst.jsdt.internal.core.SourceMethodElementInfo;
import org.eclipse.wst.jsdt.internal.core.SourceType;
import org.eclipse.wst.jsdt.internal.core.SourceTypeElementInfo;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;
import org.eclipse.wst.jsdt.internal.core.util.QualificationHelpers;
import org.eclipse.wst.jsdt.internal.oaametadata.ClassData;
import org.eclipse.wst.jsdt.internal.oaametadata.Method;

/**
 * This class is the entry point for source completions.
 * It contains two public APIs used to call CodeAssist on a given source with
 * a given environment, assisting position and storage (and possibly options).
 */
public final class CompletionEngine
	extends Engine
	implements ISearchRequestor, TypeConstants , TerminalTokens , RelevanceConstants, SuffixConstants {
	
	public class CompletionProblemFactory extends DefaultProblemFactory {
		private int lastErrorStart;

		private boolean checkProblems = false;
		public boolean hasForbiddenProblems = false;
		public boolean hasAllowedProblems = false;

		public CompletionProblemFactory(Locale loc) {
			super(loc);
		}

		public CategorizedProblem createProblem(
			char[] originatingFileName,
			int problemId,
			String[] problemArguments,
			String[] messageArguments,
			int severity,
			int start,
			int end,
			int lineNumber,
			int columnNumber) {

			CategorizedProblem pb = super.createProblem(
				originatingFileName,
				problemId,
				problemArguments,
				messageArguments,
				severity,
				start,
				end,
				lineNumber,
				columnNumber);
			int id = pb.getID();
			if (CompletionEngine.this.actualCompletionPosition > start
				&& this.lastErrorStart < start
				&& pb.isError()
				&& (id & IProblem.Syntax) == 0
				&& (CompletionEngine.this.fileName == null || CharOperation.equals(CompletionEngine.this.fileName, originatingFileName))) {

				CompletionEngine.this.problem = pb;
				this.lastErrorStart = start;
			}
			if (this.checkProblems && !this.hasForbiddenProblems) {
				switch (id) {
					case IProblem.UsingDeprecatedType:
						this.hasForbiddenProblems =
							CompletionEngine.this.options.checkDeprecation;
						break;
					case IProblem.NotVisibleType:
						this.hasForbiddenProblems =
							CompletionEngine.this.options.checkVisibility;
						break;
					case IProblem.ForbiddenReference:
						this.hasForbiddenProblems =
							CompletionEngine.this.options.checkForbiddenReference;
						break;
					case IProblem.DiscouragedReference:
						this.hasForbiddenProblems =
							CompletionEngine.this.options.checkDiscouragedReference;
						break;
					default:
						if ((severity & ProblemSeverities.Optional) != 0) {
							this.hasAllowedProblems = true;
						} else {
							this.hasForbiddenProblems = true;
						}

						break;
				}
			}

			return pb;
		}

		public void startCheckingProblems() {
			this.checkProblems = true;
			this.hasForbiddenProblems = false;
			this.hasAllowedProblems = false;
		}

		public void stopCheckingProblems() {
			this.checkProblems = false;
		}
	}

	private static class AcceptedBinding {
		public AcceptedBinding(
			char[] packageName,
			char[] simpleTypeName,
			char[][] enclosingTypeNames,
			int modifiers,
			int accessibility) {
			this.packageName = packageName;
			this.simpleTypeName = simpleTypeName;
			this.enclosingTypeNames = enclosingTypeNames;
			this.modifiers = modifiers;
			this.accessibility = accessibility;
		    this.bindingType=Binding.TYPE;
		}
		public AcceptedBinding(
				int bindingType,
				char[] packageName,
				char[] simpleTypeName,
				int modifiers,
				int accessibility) {
			    this.bindingType=bindingType;
				this.packageName = packageName;
				this.simpleTypeName = simpleTypeName;
				this.modifiers = modifiers;
				this.accessibility = accessibility;
			}
		public char[] packageName;
		public char[] simpleTypeName;
		public char[][] enclosingTypeNames;
		public int modifiers;
		public int accessibility;

		public boolean mustBeQualified = false;
		public char[] fullyQualifiedName = null;
		public char[] qualifiedTypeName = null;
		public int bindingType;

		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append('{');
			buffer.append(packageName);
			buffer.append(',');
			buffer.append(simpleTypeName);
			buffer.append(',');
			buffer.append(CharOperation.concatWith(enclosingTypeNames, '.'));
			buffer.append('}');
			return buffer.toString();
		}
	}

	public HashtableOfObject typeCache;

	public static boolean DEBUG = false;
	public static boolean PERF = false;

	// temporary constants to quickly disabled polish features if necessary
	public final static boolean NO_TYPE_COMPLETION_ON_EMPTY_TOKEN = false;

	private final static char[] ERROR_PATTERN = "*error*".toCharArray();  //$NON-NLS-1$
	private final static char[] EXCEPTION_PATTERN = "*exception*".toCharArray();  //$NON-NLS-1$
	private final static char[] SEMICOLON = new char[] { ';' };

	private final static char[] CLASS = "Class".toCharArray();  //$NON-NLS-1$
	private final static char[] VOID = "void".toCharArray();  //$NON-NLS-1$

	private final static char[] VARARGS = "...".toCharArray();  //$NON-NLS-1$

	private final static char[] IMPORT = "import".toCharArray();  //$NON-NLS-1$
	private final static char[] STATIC = "static".toCharArray();  //$NON-NLS-1$
	private final static char[] ON_DEMAND = ".*".toCharArray();  //$NON-NLS-1$
	private final static char[] IMPORT_END = ";\n".toCharArray();  //$NON-NLS-1$

	private final static char[] JAVA_LANG_OBJECT_SIGNATURE =
		createTypeSignature(
				new char[]{},
				OBJECT);
	private final static char[] JAVA_LANG_NAME =
		CharOperation.concatWith(JAVA_LANG, '.');


	private final static int NONE = 0;
	private final static int SUPERTYPE = 1;
	private final static int SUBTYPE = 2;

	private final static int FIELD = 0;
	private final static int LOCAL = 1;
	private final static int ARGUMENT = 2;

	int expectedTypesPtr = -1;
	TypeBinding[] expectedTypes = new TypeBinding[1];
	int expectedTypesFilter;
	boolean hasJavaLangObjectAsExpectedType = false;
	int uninterestingBindingsPtr = -1;
	Binding[] uninterestingBindings = new Binding[1];
	int forbbidenBindingsPtr = -1;
	Binding[] forbbidenBindings = new Binding[1];
	int forbbidenBindingsFilter;

	ImportBinding[] favoriteReferenceBindings;

	boolean assistNodeIsClass;
	boolean assistNodeIsException;
	boolean assistNodeIsConstructor;
	boolean assistNodeIsSuperType;
	int  assistNodeInJavadoc = 0;
	boolean assistNodeCanBeSingleMemberAnnotation = false;

	long targetedElement;

	IJavaScriptProject javaProject;
	CompletionParser parser;
	CompletionRequestor requestor;
	CompletionProblemFactory problemFactory;
	ProblemReporter problemReporter;
	char[] source;
	char[] completionToken;
	char[] qualifiedCompletionToken;
	boolean resolvingImports = false;
	boolean insideQualifiedReference = false;
	boolean noProposal = true;
	CategorizedProblem problem = null;
	char[] fileName = null;
	char [][]packageName;
	int startPosition, actualCompletionPosition, endPosition, offset;
	int javadocTagPosition; // Position of previous tag while completing in javadoc
	HashtableOfObject knownPkgs = new HashtableOfObject(10);
	HashtableOfObject knownTypes = new HashtableOfObject(10);
	Scanner nameScanner;

	static final char[] classField = "class".toCharArray();  //$NON-NLS-1$
	static final char[] lengthField = "length".toCharArray();  //$NON-NLS-1$
	static final char[] cloneMethod = "clone".toCharArray();  //$NON-NLS-1$
	static final char[] THIS = "this".toCharArray();  //$NON-NLS-1$
	static final char[] THROWS = "throws".toCharArray();  //$NON-NLS-1$

	static InvocationSite FakeInvocationSite = new InvocationSite(){
		public boolean isSuperAccess(){ return false; }
		public boolean isTypeAccess(){ return false; }
		public void setActualReceiverType(ReferenceBinding receiverType) {/* empty */}
		public void setDepth(int depth){/* empty */}
		public void setFieldIndex(int depth){/* empty */}
		public int sourceStart() { return 0; 	}
		public int sourceEnd() { return 0; 	}
	};
	
	/**
	 * When performing an index search for binding matches this is
	 * where the results will be stored
	 */
	private ObjectVector acceptedBindings;
	
	/**
	 * The CompletionEngine is responsible for computing source completions.
	 *
	 * It requires a searchable name environment, which supports some
	 * specific search APIs, and a requestor to feed back the results to a UI.
	 *
	 *  @param nameEnvironment org.eclipse.wst.jsdt.internal.codeassist.ISearchableNameEnvironment
	 *      used to resolve type/package references and search for types/packages
	 *      based on partial names.
	 *
	 *  @param requestor org.eclipse.wst.jsdt.internal.codeassist.ICompletionRequestor
	 *      since the engine might produce answers of various forms, the engine
	 *      is associated with a requestor able to accept all possible completions.
	 *
	 *  @param settings java.util.Map
	 *		set of options used to configure the code assist engine.
	 */
	public CompletionEngine(
			SearchableEnvironment nameEnvironment,
			CompletionRequestor requestor,
			Map settings,
			IJavaScriptProject javaProject) {
		super(settings);
		this.javaProject = javaProject;
		this.requestor = requestor;
		this.nameEnvironment = nameEnvironment;
		this.typeCache = new HashtableOfObject(5);

		this.problemFactory = new CompletionProblemFactory(Locale.getDefault());
		this.problemReporter = new ProblemReporter(
				DefaultErrorHandlingPolicies.proceedWithAllProblems(),
				this.compilerOptions,
				problemFactory);
		this.lookupEnvironment =
			new LookupEnvironment(this, this.compilerOptions, this.problemReporter, nameEnvironment);
		this.parser =
			new CompletionParser(this.problemReporter);
		this.nameScanner =
			new Scanner(
				false /*comment*/,
				false /*whitespace*/,
				false /*nls*/,
				this.compilerOptions.sourceLevel,
				null /*taskTags*/,
				null/*taskPriorities*/,
				true/*taskCaseSensitive*/);
	}

	/**
	 * One result of the search consists of a new type.
	 *
	 * NOTE - All package and type names are presented in their readable form:
	 *    Package names are in the form "a.b.c".
	 *    Nested type names are in the qualified form "A.I".
	 *    The default package is represented by an empty array.
	 */
	public void acceptType(
		char[] packageName,
		char[] fileName,
		char[] simpleTypeName,
		char[][] enclosingTypeNames,
		int modifiers,
		AccessRestriction accessRestriction) {

		if (this.options.checkDeprecation && (modifiers & ClassFileConstants.AccDeprecated) != 0) {
			return;
		}
		
		//ignore types with no simple name and anonymous types 
		if(simpleTypeName == null ||
					CharOperation.indexOf(IInferEngine.ANONYMOUS_PREFIX, simpleTypeName, false) == 0) {
			return;
		}

		if (this.options.checkVisibility) {
			if((modifiers & ClassFileConstants.AccPublic) == 0) {
				if((modifiers & ClassFileConstants.AccPrivate) != 0)
					return;
			}
		}

		int accessibility = IAccessRule.K_ACCESSIBLE;
		if(accessRestriction != null) {
			switch (accessRestriction.getProblemId()) {
				case IProblem.ForbiddenReference:
					if (this.options.checkForbiddenReference) {
						return;
					}
					accessibility = IAccessRule.K_NON_ACCESSIBLE;
					break;
				case IProblem.DiscouragedReference:
					if (this.options.checkDiscouragedReference) {
						return;
					}
					accessibility = IAccessRule.K_DISCOURAGED;
					break;
			}
		}

		if(this.acceptedBindings == null) {
			this.acceptedBindings = new ObjectVector();
		}
		char[] fullyQualifiedName = simpleTypeName;
		if(CharOperation.indexOf('.', simpleTypeName) < 0) {
			fullyQualifiedName = CharOperation.concat(packageName, simpleTypeName, '.');
		}
		this.acceptedBindings.add(new AcceptedBinding(packageName, fullyQualifiedName, enclosingTypeNames, modifiers, accessibility));
	}

	public void acceptBinding(
			char[] packageName,
			char[] fileName,
			char[] simpleTypeName,
			int bindingType,
			int modifiers,
			AccessRestriction accessRestriction) {

		if (this.options.checkDeprecation && (modifiers & ClassFileConstants.AccDeprecated) != 0) return;

		if (this.options.checkVisibility) {
			if((modifiers & ClassFileConstants.AccPublic) == 0) {
				if((modifiers & ClassFileConstants.AccPrivate) != 0) return;

				char[] currentPackage = CharOperation.concatWith(this.unitScope.getDefaultPackage().compoundName, '.');
				if(!CharOperation.equals(packageName, currentPackage)) return;
			}
		}

		int accessibility = IAccessRule.K_ACCESSIBLE;
		if(accessRestriction != null) {
			switch (accessRestriction.getProblemId()) {
				case IProblem.ForbiddenReference:
					if (this.options.checkForbiddenReference) {
						return;
					}
					accessibility = IAccessRule.K_NON_ACCESSIBLE;
					break;
				case IProblem.DiscouragedReference:
					if (this.options.checkDiscouragedReference) {
						return;
					}
					accessibility = IAccessRule.K_DISCOURAGED;
					break;
			}
		}

		if(acceptedBindings == null) {
			acceptedBindings = new ObjectVector();
		}
		acceptedBindings.add(new AcceptedBinding(bindingType,packageName, simpleTypeName, modifiers, accessibility));
	}

	private void acceptTypes(Scope scope) {
		if(this.acceptedBindings == null) return;

		int length = this.acceptedBindings.size();

		if(length == 0) return;

		HashtableOfObject onDemandFound = new HashtableOfObject();

		next : for (int i = 0; i < length; i++) {
			AcceptedBinding acceptedType = (AcceptedBinding)this.acceptedBindings.elementAt(i);
			if (acceptedType.bindingType!=Binding.TYPE)
				continue;
			char[] packageName = acceptedType.packageName;
			char[] simpleTypeName = acceptedType.simpleTypeName;
			char[][] enclosingTypeNames = acceptedType.enclosingTypeNames;
			int modifiers = acceptedType.modifiers;
			int accessibility = acceptedType.accessibility;

			char[] typeName;
			char[] flatEnclosingTypeNames;
			if(enclosingTypeNames == null || enclosingTypeNames.length == 0) {
				flatEnclosingTypeNames = null;
				typeName = simpleTypeName;
			} else {
				flatEnclosingTypeNames = CharOperation.concatWith(acceptedType.enclosingTypeNames, '.');
				typeName = CharOperation.concat(flatEnclosingTypeNames, simpleTypeName, '.');
			}
			
			// only need to combine package and type name if the name is not already qualified
			// in most cases, if not all, it will already be in qualified state
			char[] fullyQualifiedName = typeName;
			if(CharOperation.indexOf('.', typeName) < 0) {
				fullyQualifiedName = CharOperation.concat(packageName, typeName, '.');
			}

			if (this.knownTypes.containsKey(fullyQualifiedName)) continue next;

			this.knownTypes.put(fullyQualifiedName, this);

			if (this.resolvingImports) {
				char[] completionName = CharOperation.concat(fullyQualifiedName, new char[] { ';' });
				
				int relevance = computeBaseRelevance();
				relevance += computeRelevanceForResolution();
				relevance += computeRelevanceForInterestingProposal();
				relevance += computeRelevanceForRestrictions(accessibility);
				if(insideQualifiedReference) {
					relevance += computeRelevanceForCaseMatching(this.completionToken, fullyQualifiedName);
				} else {
					relevance += computeRelevanceForCaseMatching(this.completionToken, simpleTypeName);
				}

				this.noProposal = false;
				if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
					createTypeProposal(packageName, typeName, modifiers, accessibility, completionName, relevance);
				}
			} else {
				if(!this.importCachesInitialized) {
					this.initializeImportCaches();
				}

				for (int j = 0; j < this.importCacheCount; j++) {
					char[][] importName = this.importsCache[j];
					if(CharOperation.equals(typeName, importName[0])) {
						proposeType(
								packageName,
								simpleTypeName,
								modifiers,
								accessibility,
								typeName,
								fullyQualifiedName,
								!CharOperation.equals(fullyQualifiedName, importName[1]),
								scope);
						continue next;
					}
				}


				//if ((enclosingTypeNames == null || enclosingTypeNames.length == 0 ) && CharOperation.equals(this.currentPackageName, packageName)) {
					proposeType(
							packageName,
							simpleTypeName,
							modifiers,
							accessibility,
							typeName,
							fullyQualifiedName,
							true,scope);
					continue next;
			}
		}
		char[][] keys = onDemandFound.keyTable;
		Object[] values = onDemandFound.valueTable;
		int max = keys.length;
		for (int i = 0; i < max; i++) {
			if(keys[i] != null) {
				AcceptedBinding value = (AcceptedBinding) values[i];
				if(value != null) {
					proposeType(
							value.packageName,
							value.simpleTypeName,
							value.modifiers,
							value.accessibility,
							value.qualifiedTypeName,
							value.fullyQualifiedName,
							value.mustBeQualified, scope);
				}
			}
		}
		this.acceptedBindings = null; // reset
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.codeassist.ISearchRequestor#acceptFunction(char[], char[][], char[][], char[], char[], char[], char[], int, java.lang.String)
	 */
	public void acceptFunction(char[] signature,
			char[][] parameterFullyQualifiedTypeNames,
			char[][] parameterNames, char[] returnQualification,
			char[] returnSimpleName, char[] declaringQualification,
			char[] declaringSimpleName, int modifiers, String path) {
		
		if(!this.requestor.isIgnored(CompletionProposal.METHOD_REF) && (this.assistNodeInJavadoc & CompletionOnJavadoc.ONLY_INLINE_TAG) == 0) {
			
			this.proposeFunction(signature, parameterFullyQualifiedTypeNames, parameterNames,
					returnQualification, returnSimpleName, declaringQualification, declaringSimpleName, modifiers);
		}
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.codeassist.ISearchRequestor#acceptVariable(char[], char[], char[], char[], char[], int, java.lang.String)
	 */
	public void acceptVariable(char[] signature,
			char[] typeQualification,
			char[] typeSimpleName, char[] declaringQualification,
			char[] declaringSimpleName, int modifiers, String path) {
		
		this.proposeField(signature, typeQualification, typeSimpleName, declaringQualification, declaringSimpleName, modifiers);
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.codeassist.ISearchRequestor#acceptConstructor(int, char[], char[][], char[][], java.lang.String, org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction)
	 */
	public void acceptConstructor(
			int modifiers,
			char[] typeName,
			char[][] parameterTypes,
			char[][] parameterNames,
			String path,
			AccessRestriction accessRestriction) {

		//ignore constructors with no type name and anonymous types 
		if(typeName == null ||
					CharOperation.indexOf(IInferEngine.ANONYMOUS_PREFIX, typeName, false) == 0) {
			return;
		}
		
		int accessibility = IAccessRule.K_ACCESSIBLE;
		if(accessRestriction != null) {
			switch (accessRestriction.getProblemId()) {
				case IProblem.ForbiddenReference:
					if (this.options.checkForbiddenReference) {
						return;
					}
					accessibility = IAccessRule.K_NON_ACCESSIBLE;
					break;
				case IProblem.DiscouragedReference:
					if (this.options.checkDiscouragedReference) {
						return;
					}
					accessibility = IAccessRule.K_DISCOURAGED;
					break;
			}
		}
				
		//decide if constructor should be proposed based on visibility rules
		boolean proposeConstructor = true;
		if (this.options.checkVisibility) {
			proposeConstructor = !(((modifiers & ClassFileConstants.AccPublic) == 0) &&
					((modifiers & ClassFileConstants.AccPrivate) != 0));
		}
		
		if (proposeConstructor) {
			proposeConstructor(typeName, parameterTypes, parameterNames,
					modifiers, accessibility);
		}
	}

	public void acceptUnresolvedName(char[] name) {
		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForResolution(false);
		relevance += computeRelevanceForInterestingProposal();
		relevance += computeRelevanceForCaseMatching(completionToken, name);
		relevance += computeRelevanceForQualification(false);
		relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for local variable
		CompletionEngine.this.noProposal = false;
		if(!CompletionEngine.this.requestor.isIgnored(CompletionProposal.LOCAL_VARIABLE_REF)) {
			CompletionProposal proposal = CompletionEngine.this.createProposal(CompletionProposal.LOCAL_VARIABLE_REF, CompletionEngine.this.actualCompletionPosition);
			proposal.setSignature(JAVA_LANG_OBJECT_SIGNATURE);
			proposal.setReturnQualification(JAVA_LANG_NAME);
			proposal.setReturnSimpleName(OBJECT);
			proposal.setName(name);
			proposal.setCompletion(name);
			proposal.setFlags(Flags.AccDefault);
			proposal.setReplaceRange(CompletionEngine.this.startPosition - CompletionEngine.this.offset, CompletionEngine.this.endPosition - CompletionEngine.this.offset);
			proposal.setRelevance(relevance);
			CompletionEngine.this.requestor.accept(proposal);
			if(DEBUG) {
				CompletionEngine.this.printDebug(proposal);
			}
		}
	}

	/**
	 * One result of the search consists of a new package.
	 *
	 * NOTE - All package names are presented in their readable form:
	 *    Package names are in the form "a.b.c".
	 *    The default package is represented by an empty array.
	 */
	public void acceptPackage(char[] packageName) {

		if (this.knownPkgs.containsKey(packageName)) return;

		this.knownPkgs.put(packageName, this);

		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForResolution();
		relevance += computeRelevanceForInterestingProposal();
		relevance += computeRelevanceForCaseMatching(this.qualifiedCompletionToken == null ? this.completionToken : this.qualifiedCompletionToken, packageName);
		if(!this.resolvingImports) {
			relevance += computeRelevanceForQualification(true);
		}
		relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

		this.noProposal = false;
	}

	private void buildContext(
			ASTNode astNode,
			ASTNode astNodeParent,
			Binding qualifiedBinding,
			Scope scope) {
		CompletionContext context = new CompletionContext();

		// build expected types context
		if (this.expectedTypesPtr > -1) {
			int length = this.expectedTypesPtr + 1;
			char[][] expTypes = new char[length][];
			char[][] expKeys = new char[length][];
			for (int i = 0; i < length; i++) {
				expTypes[i] = getSignature(this.expectedTypes[i]);
				expKeys[i] = this.expectedTypes[i].computeUniqueKey();
			}
			context.setExpectedTypesSignatures(expTypes);
			context.setExpectedTypesKeys(expKeys);
		}

		context.setOffset(this.actualCompletionPosition + 1 - this.offset);

		// Set javadoc info
		if (astNode instanceof CompletionOnJavadoc) {
			this.assistNodeInJavadoc = ((CompletionOnJavadoc)astNode).getCompletionFlags();
			context.setJavadoc(this.assistNodeInJavadoc);
		}

		if (!(astNode instanceof CompletionOnJavadoc)) {
			CompletionScanner scanner = (CompletionScanner)this.parser.scanner;
			context.setToken(scanner.completionIdentifier);
			context.setTokenRange(
					scanner.completedIdentifierStart - this.offset,
					scanner.completedIdentifierEnd - this.offset,
					scanner.endOfEmptyToken - this.offset);
		} else if(astNode instanceof CompletionOnJavadocTag) {
			CompletionOnJavadocTag javadocTag = (CompletionOnJavadocTag) astNode;
			context.setToken(CharOperation.concat(new char[]{'@'}, javadocTag.token));
			context.setTokenRange(
					javadocTag.tagSourceStart - this.offset,
					javadocTag.tagSourceEnd - this.offset,
					((CompletionScanner)this.parser.javadocParser.scanner).endOfEmptyToken - this.offset);
		} else {
			CompletionScanner scanner = (CompletionScanner)this.parser.javadocParser.scanner;
			context.setToken(scanner.completionIdentifier);
			context.setTokenRange(
					scanner.completedIdentifierStart - this.offset,
					scanner.completedIdentifierEnd - this.offset,
					scanner.endOfEmptyToken - this.offset);
		}

		if(astNode instanceof CompletionOnStringLiteral) {
			context.setTokenKind(CompletionContext.TOKEN_KIND_STRING_LITERAL);
		} else {
			context.setTokenKind(CompletionContext.TOKEN_KIND_NAME);
		}

		if(DEBUG) {
			System.out.println(context.toString());
		}
		this.requestor.acceptContext(context);
	}

	private boolean complete(ASTNode astNode, ASTNode astNodeParent, Binding qualifiedBinding, Scope scope, boolean insideTypeAnnotation) {

		setSourceRange(astNode.sourceStart, astNode.sourceEnd);

		scope = computeForbiddenBindings(astNode, astNodeParent, scope);
		computeUninterestingBindings(astNodeParent, scope);
		if(astNodeParent != null) {
			if(!isValidParent(astNodeParent, astNode, scope)) return false;
			computeExpectedTypes(astNodeParent, astNode, scope);
		}

		buildContext(astNode, astNodeParent, qualifiedBinding, scope);

		if (astNode instanceof CompletionOnFieldType) {

			CompletionOnFieldType field = (CompletionOnFieldType) astNode;
			CompletionOnSingleTypeReference type = (CompletionOnSingleTypeReference) field.type;
			this.completionToken = type.token;
			setSourceRange(type.sourceStart, type.sourceEnd);

			findTypesAndPackages(this.completionToken, scope, new ObjectVector());
			if (!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
				findKeywordsForMember(this.completionToken, field.modifiers);
			}

			if (!field.isLocalVariable && field.modifiers == ClassFileConstants.AccDefault) {
				SourceTypeBinding enclosingType = scope.enclosingSourceType();
			
				if (!this.requestor.isIgnored(CompletionProposal.METHOD_DECLARATION)) {
					findMethods(this.completionToken,null,null,enclosingType,scope,new ObjectVector(),false,false,true,null,null,false,false,true,null, null, null, false);
				}
				if (!this.requestor.isIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION)) {
					proposeNewMethod(this.completionToken, enclosingType);
				}
			}
		} else if (astNode instanceof CompletionOnSingleNameReference) {

			CompletionOnSingleNameReference singleNameReference = (CompletionOnSingleNameReference) astNode;
			this.completionToken = singleNameReference.token;
			
			findVariablesAndMethods(
				this.completionToken,
				scope,
				singleNameReference,
				scope,
				insideTypeAnnotation,
				singleNameReference.isInsideAnnotationAttribute);
			// can be the start of a qualified type name
			findTypesAndPackages(this.completionToken, scope, new ObjectVector());
			if (!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
				if (this.completionToken != null && this.completionToken.length != 0) {
					findKeywords(this.completionToken, singleNameReference.possibleKeywords, false, false);
				} else {
					findTrueOrFalseKeywords(singleNameReference.possibleKeywords);
				}
			}
			if (singleNameReference.canBeExplicitConstructor && !this.requestor.isIgnored(CompletionProposal.METHOD_REF)){
				if (CharOperation.prefixEquals(this.completionToken, Keywords.THIS, false)) {
					ReferenceBinding ref = scope.enclosingSourceType();
					findExplicitConstructors(Keywords.THIS, ref, (MethodScope)scope, singleNameReference);
				} else if (CharOperation.prefixEquals(this.completionToken, Keywords.SUPER, false)) {
					ReferenceBinding ref = scope.enclosingSourceType();
					findExplicitConstructors(Keywords.SUPER, ref.getSuperBinding(), (MethodScope)scope, singleNameReference);
				}
			}
			
		} else if (astNode instanceof CompletionOnSingleTypeReference) {

			CompletionOnSingleTypeReference singleRef = (CompletionOnSingleTypeReference) astNode;

			this.completionToken = singleRef.token;

			this.assistNodeIsClass = singleRef.isClass();
			this.assistNodeIsException = singleRef.isException();
			this.assistNodeIsConstructor = singleRef.isConstructorType;
			this.assistNodeIsSuperType = singleRef.isSuperType();


			// can be the start of a qualified type name
			if (qualifiedBinding == null) {
				ObjectVector typesFound = new ObjectVector();
				if (this.assistNodeIsException && astNodeParent instanceof TryStatement) {
					findExceptionFromTryStatement(
							this.completionToken,
							null,
							scope.enclosingSourceType(),
							(BlockScope)scope,
							typesFound);
				}
				findTypesAndPackages(this.completionToken, scope, typesFound);
				
			} else if (!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
				findMemberTypes(
					this.completionToken,
					(ReferenceBinding) qualifiedBinding,
					scope,
					scope.enclosingSourceType(),
					false,
					false,
					false,
					false,
					!this.assistNodeIsConstructor,
					null,
					new ObjectVector());
			}
		} else if (astNode instanceof CompletionOnSingleTypeName) {

			CompletionOnSingleTypeName singleRef = (CompletionOnSingleTypeName) astNode;
			this.completionToken = singleRef.token;

			this.assistNodeIsClass = true;
			this.assistNodeIsConstructor = true;

			// can be the start of a qualified type name
			if (qualifiedBinding == null) {
					ObjectVector typesFound = new ObjectVector();
					findTypesAndPackages(this.completionToken, scope, typesFound);
			} else if (!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
				findMemberTypes(
					this.completionToken,
					(ReferenceBinding) qualifiedBinding,
					scope,
					scope.enclosingSourceType(),
					false,
					false,
					false,
					false,
					!this.assistNodeIsConstructor,
					null,
					new ObjectVector());
			}
		}
		else if (astNode instanceof CompletionOnQualifiedNameReference) {

			this.insideQualifiedReference = true;
			CompletionOnQualifiedNameReference ref =
				(CompletionOnQualifiedNameReference) astNode;
			this.completionToken = ref.completionIdentifier;
			long completionPosition = ref.sourcePositions[ref.sourcePositions.length - 1];

			if (qualifiedBinding.problemId() == ProblemReasons.NotFound) {
				setSourceRange((int) (completionPosition >>> 32), (int) completionPosition);
				// complete field members with missing fields type
				// class X {
				//   Missing f;
				//   void foo() {
				//     f.|
				//   }
				// }
				if (this.assistNodeInJavadoc == 0 &&
						(this.requestor.isAllowingRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_REF) ||
								this.requestor.isAllowingRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_REF))) {
					if(ref.tokens.length == 1) {
						findFieldsAndMethodsFromMissingFieldType(ref.tokens[0], scope, ref, insideTypeAnnotation);
					}
				}
			} else if (qualifiedBinding instanceof VariableBinding) {
				setSourceRange((int) (completionPosition >>> 32), (int) completionPosition);
				TypeBinding receiverType = ((VariableBinding) qualifiedBinding).type;
				if (receiverType != null) {
					findFieldsAndMethods(this.completionToken, receiverType, scope, ref, scope,false,false,false, null, null, null, false);
				} else if (this.assistNodeInJavadoc == 0 &&
						(this.requestor.isAllowingRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_REF) ||
								this.requestor.isAllowingRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_REF))) {
					boolean proposeField = !this.requestor.isIgnored(CompletionProposal.FIELD_REF);
					boolean proposeMethod = !this.requestor.isIgnored(CompletionProposal.METHOD_REF);
					if (proposeField || proposeMethod) {
						if (qualifiedBinding instanceof LocalVariableBinding) {
							// complete local variable members with missing variables type
							// class X {
							//   void foo() {
							//     Missing f;
							//     f.|
							//   }
							// }
							LocalVariableBinding localVariableBinding = (LocalVariableBinding) qualifiedBinding;

							findFieldsAndMethodsFromMissingType(
									this.completionToken,
									localVariableBinding.declaration.type,
									localVariableBinding.declaringScope,
									ref,
									scope);
						}
					}
				}

			} else if (qualifiedBinding instanceof ReferenceBinding) {
				boolean isInsideAnnotationAttribute = ref.isInsideAnnotationAttribute;
				ReferenceBinding receiverType = (ReferenceBinding) qualifiedBinding;
				setSourceRange((int) (completionPosition >>> 32), (int) completionPosition);

				if (!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
					findMemberTypes(
							this.completionToken,
							receiverType,
							scope,
							scope.enclosingSourceType(),
							false,
							true,
							new ObjectVector());
				}
				if (!this.requestor.isIgnored(CompletionProposal.FIELD_REF)) {
					findClassField(this.completionToken, (TypeBinding) qualifiedBinding, scope);
				}

				MethodScope methodScope = null;
				if (!isInsideAnnotationAttribute &&
						!this.requestor.isIgnored(CompletionProposal.KEYWORD) &&
						((scope instanceof MethodScope && !((MethodScope)scope).isStatic)
						|| ((methodScope = scope.enclosingMethodScope()) != null && !methodScope.isStatic))) {
					if (this.completionToken.length > 0) {
						findKeywords(this.completionToken, new char[][]{Keywords.THIS}, false, true);
					} else {
						int relevance = computeBaseRelevance();
						relevance += computeRelevanceForResolution();
						relevance += computeRelevanceForInterestingProposal();
						relevance += computeRelevanceForCaseMatching(this.completionToken, Keywords.THIS);
						relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for keywords
						relevance += R_NON_INHERITED;

						this.noProposal = false;
						if (!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
							CompletionProposal proposal = this.createProposal(CompletionProposal.KEYWORD, this.actualCompletionPosition);
							proposal.setName(Keywords.THIS);
							proposal.setCompletion(Keywords.THIS);
							proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
							proposal.setRelevance(relevance);
							this.requestor.accept(proposal);
							if (DEBUG) {
								this.printDebug(proposal);
							}
						}
					}
				}

				if (!this.requestor.isIgnored(CompletionProposal.FIELD_REF)) {
					findFields(
						this.completionToken,
						receiverType,
						scope,
						new ObjectVector(),
						new ObjectVector(),
						true,
						ref,
						scope,
						false,
						false,
						null,
						null,
						null,
						false);
				}

				if (!isInsideAnnotationAttribute && !this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
					findMethods(
						this.completionToken,
						null,
						null,
						receiverType,
						scope,
						new ObjectVector(),
						true,
						false,
						false,
						ref,
						scope,
						false,
						false,
						false,
						null,
						null,
						null,
						false);
				}

			} else if (qualifiedBinding instanceof PackageBinding) {

				setSourceRange(astNode.sourceStart, (int) completionPosition);
				// replace to the end of the completion identifier
				findTypesAndSubpackages(this.completionToken, (PackageBinding) qualifiedBinding, scope);
			}
		} else if (astNode instanceof CompletionOnQualifiedTypeReference) {

			this.insideQualifiedReference = true;

			CompletionOnQualifiedTypeReference ref =
				(CompletionOnQualifiedTypeReference) astNode;

			this.assistNodeIsClass = ref.isClass();
			this.assistNodeIsException = ref.isException();
			this.assistNodeIsSuperType = ref.isSuperType();

			this.completionToken = ref.completionIdentifier;
			long completionPosition = ref.sourcePositions[ref.tokens.length];

			// get the source positions of the completion identifier
			if (qualifiedBinding instanceof ReferenceBinding) {
				if (!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
					setSourceRange((int) (completionPosition >>> 32), (int) completionPosition);

					ObjectVector typesFound = new ObjectVector();

					if (this.assistNodeIsException && astNodeParent instanceof TryStatement) {
						findExceptionFromTryStatement(
								this.completionToken,
								(ReferenceBinding)qualifiedBinding,
								scope.enclosingSourceType(),
								(BlockScope)scope,
								typesFound);
					}

					findMemberTypes(
						this.completionToken,
						(ReferenceBinding) qualifiedBinding,
						scope,
						scope.enclosingSourceType(),
						false,
						false,
						typesFound);
				}
			} else if (qualifiedBinding instanceof PackageBinding) {

				setSourceRange(astNode.sourceStart, (int) completionPosition);
				// replace to the end of the completion identifier
				findTypesAndSubpackages(this.completionToken, (PackageBinding) qualifiedBinding, scope);
			}
		} else if (astNode instanceof CompletionOnQualifiedType) {

			this.insideQualifiedReference = true;

			CompletionOnQualifiedType ref =
				(CompletionOnQualifiedType) astNode;

			this.assistNodeIsClass = true;

			this.completionToken = ref.completionIdentifier;
			long completionPosition = ref.sourcePositions[ref.tokens.length];

			// get the source positions of the completion identifier
			if (qualifiedBinding instanceof ReferenceBinding) {
				if (!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
					setSourceRange((int) (completionPosition >>> 32), (int) completionPosition);

					ObjectVector typesFound = new ObjectVector();


					findMemberTypes(
						this.completionToken,
						(ReferenceBinding) qualifiedBinding,
						scope,
						scope.enclosingSourceType(),
						false,
						false,
						typesFound);
				}
			}
		} else if (astNode instanceof CompletionOnMemberAccess) {
			this.insideQualifiedReference = true;
			CompletionOnMemberAccess access = (CompletionOnMemberAccess) astNode;
			long completionPosition = access.nameSourcePosition;
			setSourceRange((int) (completionPosition >>> 32), (int) completionPosition);

			// can be the start of a qualified type name
			if (qualifiedBinding == null) {
			
					this.completionToken = computeToken(access);
					setSourceRange((int) (completionPosition >>> 32), (int) completionPosition);
				
					ObjectVector typesFound = new ObjectVector();
					findTypesAndPackages(this.completionToken, scope, typesFound);
			} else {
				this.completionToken = access.token;
				if (qualifiedBinding.problemId() == ProblemReasons.NotFound) {
					/* complete method members with missing return type
					 * class X {
					 *   Missing f() {return null;}
					 *   void foo() {
					 *     f().|
					 *   }
					 * }
					 */
					if (this.assistNodeInJavadoc == 0 &&
							(this.requestor.isAllowingRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_REF) ||
									this.requestor.isAllowingRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_REF))) {
						ProblemMethodBinding problemMethodBinding = (ProblemMethodBinding) qualifiedBinding;
						findFieldsAndMethodsFromMissingReturnType(
								problemMethodBinding.selector,
								problemMethodBinding.parameters,
								scope,
								access,
								insideTypeAnnotation);
					}
				} else {
					if (!access.isInsideAnnotation) {
						if (!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
							findKeywords(this.completionToken, new char[][]{Keywords.NEW}, false, false);
						}
	
						findFieldsAndMethods(
							this.completionToken,
							((TypeBinding) qualifiedBinding),
							scope,
							access,
							scope,
							access.isStatic,
							false,
							access.receiver instanceof SuperReference,
							null,
							null,
							null,
							false);
						// reset completion token to find types
						this.completionToken = computeToken(access);
						setSourceRange((int) (completionPosition >>> 32) - (this.completionToken.length - access.token.length), (int) completionPosition);
						findTypesAndPackages(this.completionToken, scope, new ObjectVector());
						// after looking for types set the completion token back to original value
						this.completionToken = access.token;
						setSourceRange((int) (completionPosition >>> 32), (int) completionPosition);
						if (qualifiedBinding instanceof FunctionTypeBinding) {
							FunctionTypeBinding functionTypeBinding = (FunctionTypeBinding) qualifiedBinding;
							if (functionTypeBinding.functionBinding!=null && functionTypeBinding.functionBinding.isConstructor())
							{
								ReferenceBinding declaringClass = (ReferenceBinding)functionTypeBinding.functionBinding.returnType;
								findFieldsAndMethods(
										this.completionToken,
										declaringClass,
										scope,
										access,
										scope,
										true,
										false,
										access.receiver instanceof SuperReference,
										null,
										null,
										null,
										false);
								
							}
							
						}
					}
				}
			}

		} else if (astNode instanceof CompletionOnMessageSend) {
			setSourceRange(astNode.sourceStart, astNode.sourceEnd, false);

			CompletionOnMessageSend messageSend = (CompletionOnMessageSend) astNode;
			TypeBinding[] argTypes = computeTypes(messageSend.arguments);
			this.completionToken = messageSend.selector;
			if (qualifiedBinding == null) {
				if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
					findImplicitMessageSends(this.completionToken, argTypes, scope, messageSend, scope);
				}
			} else  if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
				findMethods(
					this.completionToken,
					null,
					argTypes,
					(messageSend.receiver!=null)?
									(ReferenceBinding)((ReferenceBinding) qualifiedBinding)
									:null,
					scope,
					new ObjectVector(),
					false,
					false,
					false,
					messageSend,
					scope,
					false,
					messageSend.receiver instanceof SuperReference,
					false,
					null,
					null,
					null,
					false);
			}
		} else if (astNode instanceof CompletionOnExplicitConstructorCall) {
			if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
				setSourceRange(astNode.sourceStart, astNode.sourceEnd, false);

				CompletionOnExplicitConstructorCall constructorCall =
					(CompletionOnExplicitConstructorCall) astNode;
				TypeBinding[] argTypes = computeTypes(constructorCall.arguments);
				findConstructors(
					(ReferenceBinding) qualifiedBinding,
					argTypes,
					scope,
					constructorCall,
					false);
									}
		} else if (astNode instanceof CompletionOnQualifiedAllocationExpression) {
			setSourceRange(astNode.sourceStart, astNode.sourceEnd, false);

			CompletionOnQualifiedAllocationExpression allocExpression =
				(CompletionOnQualifiedAllocationExpression) astNode;
			TypeBinding[] argTypes = computeTypes(allocExpression.arguments);

			ReferenceBinding ref = (ReferenceBinding) qualifiedBinding;
			if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)
					&& ref.isClass()) {
					findConstructors(
						ref,
						argTypes,
						scope,
						allocExpression,
						false);
			}
			if (!this.requestor.isIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION)){
				findAnonymousType(
					ref,
					argTypes,
					scope,
					allocExpression);
			}
		} else if (astNode instanceof CompletionOnClassLiteralAccess) {
			if (!this.requestor.isIgnored(CompletionProposal.FIELD_REF)) {
				CompletionOnClassLiteralAccess access = (CompletionOnClassLiteralAccess) astNode;
				setSourceRange(access.classStart, access.sourceEnd);

				this.completionToken = access.completionIdentifier;

				findClassField(this.completionToken, (TypeBinding) qualifiedBinding, scope);
			}
		} else if (astNode instanceof CompletionOnMethodName) {
			if (!this.requestor.isIgnored(CompletionProposal.VARIABLE_DECLARATION)) {
				CompletionOnMethodName method = (CompletionOnMethodName) astNode;

				setSourceRange(method.sourceStart, method.selectorEnd);

				FieldBinding[] fields = scope.enclosingSourceType().fields();
				char[][] excludeNames = new char[fields.length][];
				for(int i = 0 ; i < fields.length ; i++){
					excludeNames[i] = fields[i].name;
				}

				this.completionToken = method.getName();

				findVariableNames(this.completionToken, method.returnType, excludeNames, null, FIELD, method.modifiers);
			}
		} else if (astNode instanceof CompletionOnFieldName) {
			if (!this.requestor.isIgnored(CompletionProposal.VARIABLE_DECLARATION)) {
				CompletionOnFieldName field = (CompletionOnFieldName) astNode;

				FieldBinding[] fields = scope.enclosingSourceType().fields();
				char[][] excludeNames = new char[fields.length][];
				for(int i = 0 ; i < fields.length ; i++){
					excludeNames[i] = fields[i].name;
				}

				this.completionToken = field.realName;

				findVariableNames(field.realName, field.type, excludeNames, null, FIELD, field.modifiers);
			}
		} else if (astNode instanceof CompletionOnLocalName || astNode instanceof CompletionOnArgumentName) {
			if (!this.requestor.isIgnored(CompletionProposal.VARIABLE_DECLARATION)) {
				LocalDeclaration variable = (LocalDeclaration) astNode;

				int kind;
				if (variable instanceof CompletionOnLocalName){
					this.completionToken = ((CompletionOnLocalName) variable).realName;
					kind = LOCAL;
				} else {
					CompletionOnArgumentName arg = (CompletionOnArgumentName) variable;
					this.completionToken = arg.realName;
					kind = arg.isCatchArgument ? LOCAL : ARGUMENT;
				}

				char[][] alreadyDefinedName = computeAlreadyDefinedName((BlockScope)scope, variable);

				char[][] forbiddenNames = findVariableFromUnresolvedReference(variable, (BlockScope)scope, alreadyDefinedName);

				LocalVariableBinding[] locals = ((BlockScope)scope).locals;
				char[][] discouragedNames = new char[locals.length][];
				int localCount = 0;
				for(int i = 0 ; i < locals.length ; i++){
					if (locals[i] != null) {
						discouragedNames[localCount++] = locals[i].name;
					}
				}

				System.arraycopy(discouragedNames, 0, discouragedNames = new char[localCount][], 0, localCount);

				findVariableNames(this.completionToken, variable.type, discouragedNames, forbiddenNames, kind, variable.modifiers);
			}
		} else if (astNode instanceof CompletionOnKeyword) {
			if (!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
				CompletionOnKeyword keyword = (CompletionOnKeyword)astNode;
				findKeywords(keyword.getToken(), keyword.getPossibleKeywords(), keyword.canCompleteEmptyToken(), false);
			}
		} else if(astNode instanceof CompletionOnBrankStatementLabel) {
			if (!this.requestor.isIgnored(CompletionProposal.LABEL_REF)) {
				CompletionOnBrankStatementLabel label = (CompletionOnBrankStatementLabel) astNode;

				this.completionToken = label.label;

				this.findLabels(this.completionToken, label.possibleLabels);
			}
		} else if(astNode instanceof CompletionOnMessageSendName) {
			if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
				CompletionOnMessageSendName messageSend = (CompletionOnMessageSendName) astNode;

				this.insideQualifiedReference = true;
				this.completionToken = messageSend.selector;
			}
		// Completion on Javadoc nodes
		} else if ((astNode.bits & ASTNode.InsideJavadoc) != 0) {
			if (astNode instanceof CompletionOnJavadocSingleTypeReference) {

				CompletionOnJavadocSingleTypeReference typeRef = (CompletionOnJavadocSingleTypeReference) astNode;
				this.completionToken = typeRef.token;
				this.javadocTagPosition = typeRef.tagSourceStart;
				setSourceRange(typeRef.sourceStart, typeRef.sourceEnd);
				findTypesAndPackages(this.completionToken, scope, new ObjectVector());

			} else if (astNode instanceof CompletionOnJavadocQualifiedTypeReference) {

				this.insideQualifiedReference = true;

				CompletionOnJavadocQualifiedTypeReference typeRef = (CompletionOnJavadocQualifiedTypeReference) astNode;
				this.completionToken = typeRef.completionIdentifier;
				long completionPosition = typeRef.sourcePositions[typeRef.tokens.length];
				this.javadocTagPosition = typeRef.tagSourceStart;

				// get the source positions of the completion identifier
				if (qualifiedBinding instanceof ReferenceBinding) {
					if (!this.requestor.isIgnored(CompletionProposal.TYPE_REF) ||
							((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JSDOC_TYPE_REF))) {
						int rangeStart = typeRef.completeInText() ? typeRef.sourceStart : (int) (completionPosition >>> 32);
						setSourceRange(rangeStart, (int) completionPosition);
						findMemberTypes(this.completionToken,
							(ReferenceBinding) qualifiedBinding,
							scope,
							scope.enclosingSourceType(),
							false,
							false,
							new ObjectVector());
					}
				} else if (qualifiedBinding instanceof PackageBinding) {

					setSourceRange(astNode.sourceStart, (int) completionPosition);
					// replace to the end of the completion identifier
					findTypesAndSubpackages(this.completionToken, (PackageBinding) qualifiedBinding, scope);
				}
			} else if (astNode instanceof CompletionOnJavadocFieldReference) {

				this.insideQualifiedReference = true;
				CompletionOnJavadocFieldReference fieldRef = (CompletionOnJavadocFieldReference) astNode;
				this.completionToken = fieldRef.token;
				long completionPosition = fieldRef.nameSourcePosition;
				this.javadocTagPosition = fieldRef.tagSourceStart;

				if (fieldRef.receiverType != null && fieldRef.receiverType.isValidBinding()) {
					ReferenceBinding receiverType = (ReferenceBinding) fieldRef.receiverType;
					int rangeStart = (int) (completionPosition >>> 32);
					if (fieldRef.receiver.isThis()) {
						if (fieldRef.completeInText()) {
							rangeStart = fieldRef.separatorPosition;
						}
					} else if (fieldRef.completeInText()) {
						rangeStart = fieldRef.receiver.sourceStart;
					}
					setSourceRange(rangeStart, (int) completionPosition);

					if (!this.requestor.isIgnored(CompletionProposal.FIELD_REF)
							|| !this.requestor.isIgnored(CompletionProposal.JSDOC_FIELD_REF)) {
						findFields(this.completionToken,
							receiverType,
							scope,
							new ObjectVector(),
							new ObjectVector(),
							false, /*not only static */
							fieldRef,
							scope,
							false,
							true,
							null,
							null,
							null,
							false);
					}

					if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)
							|| !this.requestor.isIgnored(CompletionProposal.JSDOC_METHOD_REF)) {
						findMethods(this.completionToken,
							null,
							null,
							receiverType,
							scope,
							new ObjectVector(),
							false, /*not only static */
							false,
							false,
							fieldRef,
							scope,
							false,
							false,
							true,
							null,
							null,
							null,
							false);
						if (fieldRef.receiverType instanceof ReferenceBinding) {
							ReferenceBinding refBinding = (ReferenceBinding)fieldRef.receiverType;
							if (this.completionToken == null
									|| CharOperation.prefixEquals(this.completionToken, refBinding.sourceName)
									|| (this.options.camelCaseMatch && CharOperation.camelCaseMatch(this.completionToken, refBinding.sourceName))) {
								findConstructors(refBinding, null, scope, fieldRef, false);
							}
						}
					}
				}
			} else if (astNode instanceof CompletionOnJavadocMessageSend) {

				CompletionOnJavadocMessageSend messageSend = (CompletionOnJavadocMessageSend) astNode;
				TypeBinding[] argTypes = null; //computeTypes(messageSend.arguments);
				this.completionToken = messageSend.selector;
				this.javadocTagPosition = messageSend.tagSourceStart;

				// Set source range
				int rangeStart = astNode.sourceStart;
				if (messageSend.receiver!=null && messageSend.receiver.isThis()) {
					if (messageSend.completeInText()) {
						rangeStart = messageSend.separatorPosition;
					}
				} else if (messageSend.completeInText()) {
					rangeStart = messageSend.receiver.sourceStart;
				}
				setSourceRange(rangeStart, astNode.sourceEnd, false);

				if (qualifiedBinding == null) {
					if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
						findImplicitMessageSends(this.completionToken, argTypes, scope, messageSend, scope);
					}
				} else if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
					findMethods(
						this.completionToken,
						null,
						argTypes,
						((ReferenceBinding) qualifiedBinding),
						scope,
						new ObjectVector(),
						false,
						false/* prefix match */,
						false,
						messageSend,
						scope,
						false,
						messageSend.receiver instanceof SuperReference,
						true,
						null,
						null,
						null,
						false);
				}
			} else if (astNode instanceof CompletionOnJavadocAllocationExpression) {
//				setSourceRange(astNode.sourceStart, astNode.sourceEnd, false);

				CompletionOnJavadocAllocationExpression allocExpression = (CompletionOnJavadocAllocationExpression) astNode;
				this.javadocTagPosition = allocExpression.tagSourceStart;
				int rangeStart = astNode.sourceStart;
				if (allocExpression.type.isThis()) {
					if (allocExpression.completeInText()) {
						rangeStart = allocExpression.separatorPosition;
					}
				} else if (allocExpression.completeInText()) {
					rangeStart = allocExpression.type.sourceStart;
				}
				setSourceRange(rangeStart, astNode.sourceEnd, false);
				TypeBinding[] argTypes = computeTypes(allocExpression.arguments);

				ReferenceBinding ref = (ReferenceBinding) qualifiedBinding;
				if (!this.requestor.isIgnored(CompletionProposal.METHOD_REF) && ref.isClass()) {
					findConstructors(ref, argTypes, scope, allocExpression, false);
				}
			} else if (astNode instanceof CompletionOnJavadocParamNameReference) {
				if (!this.requestor.isIgnored(CompletionProposal.JSDOC_PARAM_REF)) {
					CompletionOnJavadocParamNameReference paramRef = (CompletionOnJavadocParamNameReference) astNode;
					setSourceRange(paramRef.tagSourceStart, paramRef.tagSourceEnd);
					findJavadocParamNames(paramRef.token, paramRef.missingParams, false);
				}
			} else if (astNode instanceof CompletionOnJavadocTag) {
				CompletionOnJavadocTag javadocTag = (CompletionOnJavadocTag) astNode;
				setSourceRange(javadocTag.tagSourceStart, javadocTag.sourceEnd);
				findJavadocBlockTags(javadocTag);
				findJavadocInlineTags(javadocTag);
			}
		}
		return true;
	}

	public void complete(IType type, char[] snippet, int position, char[][] localVariableTypeNames, char[][] localVariableNames, int[] localVariableModifiers, boolean isStatic){
		if(this.requestor != null){
			this.requestor.beginReporting();
		}
		boolean contextAccepted = false;
		IType topLevelType = type;
		while(topLevelType.getDeclaringType() != null) {
			topLevelType = topLevelType.getDeclaringType();
		}

		this.fileName = topLevelType.getParent().getElementName().toCharArray();
		CompilationResult compilationResult = new CompilationResult(this.fileName, this.packageName, 1, 1, this.compilerOptions.maxProblemsPerUnit);

		CompilationUnitDeclaration compilationUnit = null;

		try {
			// TypeConverter is used instead of SourceTypeConverter because the type
			// to convert can be a binary type or a source type
			TypeDeclaration typeDeclaration = null;
			if (type instanceof SourceType) {
				SourceType sourceType = (SourceType) type;
				ISourceType info = (ISourceType) sourceType.getElementInfo();
				compilationUnit = SourceTypeConverter.buildCompilationUnit(
					new ISourceType[] {info},//sourceTypes[0] is always toplevel here
					SourceTypeConverter.FIELD_AND_METHOD // need field and methods
					| SourceTypeConverter.MEMBER_TYPE, // need member types
					// no need for field initialization
					this.problemReporter,
					compilationResult);
				if (compilationUnit.types != null)
					typeDeclaration = compilationUnit.types[0];
			} else {
				compilationUnit = new CompilationUnitDeclaration(this.problemReporter, compilationResult, 0);
				typeDeclaration = BinaryTypeConverter.buildTypeDeclaration(type, compilationUnit, compilationResult);
			}

			if(typeDeclaration != null) {
				// build AST from snippet
				Initializer fakeInitializer = parseSnippeInitializer(snippet, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic);

				// merge AST
				FieldDeclaration[] oldFields = typeDeclaration.fields;
				FieldDeclaration[] newFields = null;
				if (oldFields != null) {
					newFields = new FieldDeclaration[oldFields.length + 1];
					System.arraycopy(oldFields, 0, newFields, 0, oldFields.length);
					newFields[oldFields.length] = fakeInitializer;
				} else {
					newFields = new FieldDeclaration[] {fakeInitializer};
				}
				typeDeclaration.fields = newFields;

				if(DEBUG) {
					System.out.println("SNIPPET COMPLETION AST :"); //$NON-NLS-1$
					System.out.println(compilationUnit.toString());
				}

				if (compilationUnit.types != null) {
					try {
						this.lookupEnvironment.buildTypeBindings(compilationUnit, null /*no access restriction*/);

						if ((this.unitScope = compilationUnit.scope) != null) {
							this.lookupEnvironment.completeTypeBindings(compilationUnit, true);
							compilationUnit.scope.faultInTypes();
							compilationUnit.resolve();
						}
					} catch (CompletionNodeFound e) {
						//					completionNodeFound = true;
						if (e.astNode != null) {
							// if null then we found a problem in the completion node
							contextAccepted = complete(e.astNode, this.parser.assistNodeParent, e.qualifiedBinding, e.scope, e.insideTypeAnnotation);
						}
					}
				}
				if(this.noProposal && this.problem != null) {
					if(!contextAccepted) {
						contextAccepted = true;
						this.requestor.acceptContext(new CompletionContext());
					}
					this.requestor.completionFailure(this.problem);
					if(DEBUG) {
						this.printDebug(this.problem);
					}
				}
			}
		}  catch (IndexOutOfBoundsException e) { // work-around internal failure - 1GEMF6D (added with fix of 99629)
			if(DEBUG) {
				System.out.println("Exception caught by CompletionEngine:"); //$NON-NLS-1$
				e.printStackTrace(System.out);
			}
		} catch (InvalidCursorLocation e) { // may eventually report a usefull error (added to fix 99629)
			if(DEBUG) {
				System.out.println("Exception caught by CompletionEngine:"); //$NON-NLS-1$
				e.printStackTrace(System.out);
			}
		} catch (AbortCompilation e) { // ignore this exception for now since it typically means we cannot find java.lang.Object (added with fix of 99629)
			if(DEBUG) {
				System.out.println("Exception caught by CompletionEngine:"); //$NON-NLS-1$
				e.printStackTrace(System.out);
			}
		} catch (CompletionNodeFound e){ // internal failure - bugs 5618 (added with fix of 99629)
			if(DEBUG) {
				System.out.println("Exception caught by CompletionEngine:"); //$NON-NLS-1$
				e.printStackTrace(System.out);
			}
		} catch(JavaScriptModelException e) {
			// Do nothing
		}
		if(!contextAccepted) {
			contextAccepted = true;
			this.requestor.acceptContext(new CompletionContext());
		}
		if(this.requestor != null){
			this.requestor.endReporting();
		}
	}

	private Initializer parseSnippeInitializer(char[] snippet, int position, char[][] localVariableTypeNames, char[][] localVariableNames, int[] localVariableModifiers, boolean isStatic){
		StringBuffer prefix = new StringBuffer();
		prefix.append("public class FakeType {\n "); //$NON-NLS-1$
		if(isStatic) {
			prefix.append("static "); //$NON-NLS-1$
		}
		prefix.append("{\n"); //$NON-NLS-1$
		for (int i = 0; i < localVariableTypeNames.length; i++) {
			ASTNode.printModifiers(localVariableModifiers[i], prefix);
			prefix.append(' ');
			prefix.append(localVariableTypeNames[i]);
			prefix.append(' ');
			prefix.append(localVariableNames[i]);
			prefix.append(';');
		}

		char[] fakeSource = CharOperation.concat(prefix.toString().toCharArray(), snippet, "}}".toCharArray());//$NON-NLS-1$
		this.offset = prefix.length();

		String encoding = this.compilerOptions.defaultEncoding;
		BasicCompilationUnit fakeUnit = new BasicCompilationUnit(
			fakeSource,
			null,
			"FakeType.java", //$NON-NLS-1$
			encoding);

		this.actualCompletionPosition = prefix.length() + position - 1;

		CompilationResult fakeResult = new CompilationResult(fakeUnit, 1, 1, this.compilerOptions.maxProblemsPerUnit);
		CompilationUnitDeclaration fakeAST = this.parser.dietParse(fakeUnit, fakeResult, this.actualCompletionPosition);

		parseBlockStatements(fakeAST, this.actualCompletionPosition);

		return (Initializer)fakeAST.types[0].fields[0];
	}

	/**
	 * Ask the engine to compute a completion at the specified position
	 * of the given compilation unit.
	 *
	 *  No return
	 *      completion results are answered through a requestor.
	 *
	 *  @param sourceUnit org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit
	 *      the source of the current compilation unit.
	 *
	 *  @param completionPosition int
	 *      a position in the source where the completion is taking place.
	 *      This position is relative to the source provided.
	 */
	public void complete(ICompilationUnit sourceUnit, int completionPosition, int pos) {

		if(DEBUG) {
			System.out.print("COMPLETION IN "); //$NON-NLS-1$
			System.out.print(sourceUnit.getFileName());
			System.out.print(" AT POSITION "); //$NON-NLS-1$
			System.out.println(completionPosition);
			System.out.println("COMPLETION - Source :"); //$NON-NLS-1$
			System.out.println(sourceUnit.getContents());
		}
		this.requestor.beginReporting();
		boolean contextAccepted = false;
		try {
			this.fileName = sourceUnit.getFileName();
			this.packageName= CharOperation.NO_CHAR_CHAR;
			this.actualCompletionPosition = completionPosition - 1;
			this.offset = pos;
			// for now until we can change the UI.
			CompilationResult result = new CompilationResult(sourceUnit, 1, 1, this.compilerOptions.maxProblemsPerUnit);
			CompilationUnitDeclaration parsedUnit = this.parser.dietParse(sourceUnit, result, this.actualCompletionPosition);
			//		boolean completionNodeFound = false;
			if (parsedUnit != null) {
				if(DEBUG) {
					System.out.println("COMPLETION - Diet AST :"); //$NON-NLS-1$
					System.out.println(parsedUnit.toString());
				}

				this.parser.inferTypes(parsedUnit, this.compilerOptions);

				ImportReference[] imports = parsedUnit.imports;
				if (imports != null) {
					for (int i = 0, length = imports.length; i < length; i++) {
						ImportReference importReference = imports[i];
						if (importReference instanceof CompletionOnImportReference) {
							this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
							if ((this.unitScope = parsedUnit.scope) != null) {
								contextAccepted = true;
								this.buildContext(importReference, null, null, null);

								setSourceRange(
									importReference.sourceStart,
									importReference.declarationSourceEnd);

								char[][] oldTokens = importReference.tokens;
								int tokenCount = oldTokens.length;
								if (tokenCount == 1) {
									findImports((CompletionOnImportReference)importReference, true);
								} else if(tokenCount > 1){
									this.insideQualifiedReference = true;

									char[] lastToken = oldTokens[tokenCount - 1];
									char[][] qualifierTokens = CharOperation.subarray(oldTokens, 0, tokenCount - 1);

									Binding binding = this.unitScope.getTypeOrPackage(qualifierTokens);
									if(binding != null) {
										if(binding instanceof PackageBinding) {
											findImports((CompletionOnImportReference)importReference, false);
										} else {
											ReferenceBinding ref = (ReferenceBinding) binding;
											if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
												this.findImportsOfMemberTypes(lastToken, ref);
											}
										}
									}
								}

								if(this.noProposal && this.problem != null) {
									this.requestor.completionFailure(this.problem);
									if(DEBUG) {
										this.printDebug(this.problem);
									}
								}
							}
							return;
						} else if(importReference instanceof CompletionOnKeyword) {
							contextAccepted = true;
							this.buildContext(importReference, null, null, null);
							if(!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
								setSourceRange(importReference.sourceStart, importReference.sourceEnd);
								CompletionOnKeyword keyword = (CompletionOnKeyword)importReference;
								findKeywords(keyword.getToken(), keyword.getPossibleKeywords(), false, false);
							}
							if(this.noProposal && this.problem != null) {
								this.requestor.completionFailure(this.problem);
								if(DEBUG) {
									this.printDebug(this.problem);
								}
							}
							return;
						}
					}
				}

				if (parsedUnit.statements  != null) {
					try {
						this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/, true);

						if ((this.unitScope = parsedUnit.scope) != null) {
							this.source = sourceUnit.getContents();
							this.lookupEnvironment.completeTypeBindings(parsedUnit, true);
							parsedUnit.scope.faultInTypes();
							if (Parser.DO_DIET_PARSE)
							parseBlockStatements(parsedUnit, this.actualCompletionPosition);
							if(DEBUG) {
								System.out.println("COMPLETION - AST :"); //$NON-NLS-1$
								System.out.println(parsedUnit.toString());
							}
							parsedUnit.resolve();
						}
					} catch (CompletionNodeFound e) {
						if (e.astNode != null) {
							if(DEBUG) {
								System.out.print("COMPLETION - Completion node : "); //$NON-NLS-1$
								System.out.println(e.astNode.toString());
								if(this.parser.assistNodeParent != null) {
									System.out.print("COMPLETION - Parent Node : ");  //$NON-NLS-1$
									System.out.println(this.parser.assistNodeParent);
								}
							}
							
							//in case completion node found before unit scope set
							if(this.unitScope == null && parsedUnit.scope != null) {
								this.unitScope = parsedUnit.scope;
							}
							
							// if null then we found a problem in the completion node
							contextAccepted = complete(e.astNode, this.parser.assistNodeParent, e.qualifiedBinding, e.scope, e.insideTypeAnnotation);
						}
					}
				}
				parsedUnit.cleanUp();
			}

			if(this.noProposal && this.problem != null) {
				if(!contextAccepted) {
					contextAccepted = true;
					CompletionContext context = new CompletionContext();
					context.setOffset(completionPosition - this.offset);
					context.setTokenKind(CompletionContext.TOKEN_KIND_UNKNOWN);
					this.requestor.acceptContext(context);
				}
				this.requestor.completionFailure(this.problem);
				if(DEBUG) {
					this.printDebug(this.problem);
				}
			}
			/* Ignore package, import, class & interface keywords for now...
					if (!completionNodeFound) {
						if (parsedUnit == null || parsedUnit.types == null) {
							// this is not good enough... can still be trying to define a second type
							CompletionScanner scanner = (CompletionScanner) this.parser.scanner;
							setSourceRange(scanner.completedIdentifierStart, scanner.completedIdentifierEnd);
							findKeywords(scanner.completionIdentifier, mainDeclarations, null);
						}
						// currently have no way to know if extends/implements are possible keywords
					}
			*/
		} catch (IndexOutOfBoundsException e) { // work-around internal failure - 1GEMF6D
			if(DEBUG) {
				System.out.println("Exception caught by CompletionEngine:"); //$NON-NLS-1$
				e.printStackTrace(System.out);
			}
		} catch (InvalidCursorLocation e) { // may eventually report a usefull error
			if(DEBUG) {
				System.out.println("Exception caught by CompletionEngine:"); //$NON-NLS-1$
				e.printStackTrace(System.out);
			}
		} catch (AbortCompilation e) { // ignore this exception for now since it typically means we cannot find java.lang.Object
			if(DEBUG) {
				System.out.println("Exception caught by CompletionEngine:"); //$NON-NLS-1$
				e.printStackTrace(System.out);
			}
		} catch (CompletionNodeFound e){ // internal failure - bugs 5618
			if(DEBUG) {
				System.out.println("Exception caught by CompletionEngine:"); //$NON-NLS-1$
				e.printStackTrace(System.out);
			}
		} finally {
			this.parser=null;
			reset();
			if(!contextAccepted) {
				contextAccepted = true;
				CompletionContext context = new CompletionContext();
				context.setTokenKind(CompletionContext.TOKEN_KIND_UNKNOWN);
				context.setOffset(completionPosition - this.offset);
				this.requestor.acceptContext(context);
			}
			this.requestor.endReporting();
		}
	}

	private TypeBinding[] computeTypes(Expression[] arguments) {
		if (arguments == null) return null;
		int argsLength = arguments.length;
		TypeBinding[] argTypes = new TypeBinding[argsLength];
		for (int a = argsLength; --a >= 0;) {
			argTypes[a] = arguments[a].resolvedType;
		}
		return argTypes;
	}
	
	private void findAnonymousType(
		ReferenceBinding currentType,
		TypeBinding[] argTypes,
		Scope scope,
		InvocationSite invocationSite) {

		findConstructors(
			currentType,
			argTypes,
			scope,
			invocationSite,
			true);
	}

	private void findClassField(char[] token, TypeBinding receiverType, Scope scope) {

		if (token == null) return;

		if (token.length <= classField.length
			&& CharOperation.prefixEquals(token, classField, false /* ignore case */
		)) {
			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForCaseMatching(token, classField);
			relevance += computeRelevanceForExpectingType(scope.getJavaLangClass());
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); //no access restriction for class field
			relevance += R_NON_INHERITED;

			this.noProposal = false;
			if(!this.requestor.isIgnored(CompletionProposal.FIELD_REF)) {
				CompletionProposal proposal = this.createProposal(CompletionProposal.FIELD_REF, this.actualCompletionPosition);
				//proposal.setDeclarationSignature(null);
				char[] signature =
					createNonGenericTypeSignature(CLASS);
				if (this.compilerOptions.sourceLevel > ClassFileConstants.JDK1_4) {
					// add type argument
					char[] typeArgument = getTypeSignature(receiverType);
					int oldLength = signature.length;
					int argumentLength = typeArgument.length;
					int newLength = oldLength + argumentLength + 2;
					System.arraycopy(signature, 0, signature = new char[newLength], 0, oldLength - 1);
					signature[oldLength - 1] = '<';
					System.arraycopy(typeArgument, 0, signature, oldLength , argumentLength);
					signature[newLength - 2] = '>';
					signature[newLength - 1] = ';';
				}
				proposal.setSignature(signature);
				//proposal.setDeclarationPackageName(null);
				//proposal.setDeclarationTypeName(null);
				proposal.setReturnQualification(CharOperation.concatWith(JAVA_LANG, '.'));
				proposal.setReturnSimpleName(CLASS);
				proposal.setName(classField);
				proposal.setCompletion(classField);
				proposal.setFlags(Flags.AccStatic | Flags.AccPublic);
				proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
				proposal.setRelevance(relevance);
				this.requestor.accept(proposal);
				if(DEBUG) {
					this.printDebug(proposal);
				}
			}
		}
	}

	private void findExceptionFromTryStatement(
			char[] typeName,
			ReferenceBinding exceptionType,
			ReferenceBinding receiverType,
			SourceTypeBinding invocationType,
			BlockScope scope,
			ObjectVector typesFound,
			boolean searchSuperClasses) {


		if (searchSuperClasses) {
			ReferenceBinding javaLangThrowable = scope.getJavaLangThrowable();
			if (exceptionType != javaLangThrowable) {
				ReferenceBinding superClass = exceptionType.getSuperBinding();
				while(superClass != null && superClass != javaLangThrowable) {
					findExceptionFromTryStatement(typeName, superClass, receiverType, invocationType, scope, typesFound, false);
					superClass = superClass.getSuperBinding();
				}
			}
		}

		if (typeName.length > exceptionType.sourceName.length)
			return;

		if (!CharOperation.prefixEquals(typeName, exceptionType.sourceName, false/* ignore case */)
				&& !(this.options.camelCaseMatch && CharOperation.camelCaseMatch(typeName, exceptionType.sourceName)))
			return;

		if (this.options.checkDeprecation &&
				exceptionType.isViewedAsDeprecated() &&
				!scope.isDefinedInSameUnit(exceptionType))
			return;

		if (this.options.checkVisibility) {
			if (invocationType != null) {
				if (receiverType != null) {
					if (!exceptionType.canBeSeenBy(receiverType, invocationType)) return;
				} else {
					if (!exceptionType.canBeSeenBy(exceptionType, invocationType)) return;
				}
			} else if(!exceptionType.canBeSeenBy(this.unitScope.getDefaultPackage())) {
				return;
			}
		}

		for (int j = typesFound.size; --j >= 0;) {
			ReferenceBinding otherType = (ReferenceBinding) typesFound.elementAt(j);

			if (exceptionType == otherType)
				return;

			if (CharOperation.equals(exceptionType.sourceName, otherType.sourceName, true)) {

				if (exceptionType.enclosingType().isSuperclassOf(otherType.enclosingType()))
					return;
					
			}
		}

		typesFound.add(exceptionType);

		char[] completionName = exceptionType.sourceName();

		boolean isQualified = false;

		if(!this.insideQualifiedReference) {
			isQualified = true;

			char[] memberPackageName = exceptionType.qualifiedPackageName();
			char[] memberTypeName = exceptionType.sourceName();
			char[] memberEnclosingTypeNames = null;

			ReferenceBinding enclosingType = exceptionType.enclosingType();
			if (enclosingType != null) {
				memberEnclosingTypeNames = exceptionType.enclosingType().qualifiedSourceName();
			}

			Scope currentScope = scope;
			done : while (currentScope != null) { // done when a COMPILATION_UNIT_SCOPE is found

				switch (currentScope.kind) {

					case Scope.METHOD_SCOPE :
					case Scope.BLOCK_SCOPE :
						BlockScope blockScope = (BlockScope) currentScope;

						for (int j = 0, length = blockScope.subscopeCount; j < length; j++) {

							if (blockScope.subscopes[j] instanceof ClassScope) {
								SourceTypeBinding localType =
									((ClassScope) blockScope.subscopes[j]).referenceContext.binding;

								if (localType == exceptionType) {
									isQualified = false;
									break done;
								}
							}
						}
						break;

					case Scope.CLASS_SCOPE :
						SourceTypeBinding type = ((ClassScope)currentScope).referenceContext.binding;
						ReferenceBinding[] memberTypes = type.memberTypes();
						if (memberTypes != null) {
							for (int j = 0; j < memberTypes.length; j++) {
								if (memberTypes[j] == exceptionType) {
									isQualified = false;
									break done;
								}
							}
						}


						break;

					case Scope.COMPILATION_UNIT_SCOPE :
						SourceTypeBinding[] types = ((CompilationUnitScope)currentScope).topLevelTypes;
						if (types != null) {
							for (int j = 0; j < types.length; j++) {
								if (types[j] == exceptionType) {
									isQualified = false;
									break done;
								}
							}
						}
						break done;
				}
				currentScope = currentScope.parent;
			}

			if (isQualified && mustQualifyType(memberPackageName, memberTypeName, memberEnclosingTypeNames, exceptionType.modifiers)) {
				if (memberPackageName == null || memberPackageName.length == 0)
					if (this.unitScope != null && this.unitScope.getDefaultPackage().compoundName != CharOperation.NO_CHAR_CHAR)
						return; // ignore types from the default package from outside it
			} else {
				isQualified = false;
			}

			if (isQualified) {
				completionName =
					CharOperation.concat(
							memberPackageName,
							CharOperation.concat(
									memberEnclosingTypeNames,
									memberTypeName,
									'.'),
							'.');
			}
		}

		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForResolution();
		relevance += computeRelevanceForInterestingProposal();
		relevance += computeRelevanceForCaseMatching(typeName, exceptionType.sourceName);
		relevance += computeRelevanceForExpectingType(exceptionType);
		relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);
		if(!insideQualifiedReference) {
			relevance += computeRelevanceForQualification(isQualified);
		}
		relevance += computeRelevanceForClass();
		relevance += computeRelevanceForException();

		this.noProposal = false;
		if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
			createTypeProposal(exceptionType, exceptionType.qualifiedSourceName(), IAccessRule.K_ACCESSIBLE, completionName, relevance);
		}
	}

	private void findExceptionFromTryStatement(
			char[] typeName,
			ReferenceBinding receiverType,
			SourceTypeBinding invocationType,
			BlockScope scope,
			ObjectVector typesFound) {

		for (int i = 0; i <= this.expectedTypesPtr; i++) {
			ReferenceBinding exceptionType = (ReferenceBinding)this.expectedTypes[i];

			findExceptionFromTryStatement(typeName, exceptionType, receiverType, invocationType, scope, typesFound, true);
		}
	}

	private void findExplicitConstructors(
		char[] name,
		ReferenceBinding currentType,
		MethodScope scope,
		InvocationSite invocationSite) {

		ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration)scope.referenceContext;
		MethodBinding enclosingConstructor = constructorDeclaration.getBinding();

		// No visibility checks can be performed without the scope & invocationSite
		MethodBinding[] methods = currentType.availableMethods();
		if(methods != null) {
			next : for (int f = methods.length; --f >= 0;) {
				MethodBinding constructor = methods[f];
				if (constructor != enclosingConstructor && constructor.isConstructor()) {

					if (this.options.checkDeprecation &&
							constructor.isViewedAsDeprecated() &&
							!scope.isDefinedInSameUnit(constructor.declaringClass))
						continue next;

					if (this.options.checkVisibility
						&& !constructor.canBeSeenBy(invocationSite, scope))	continue next;

					TypeBinding[] parameters = constructor.parameters;
					int paramLength = parameters.length;

					char[][] parameterTypeNames = new char[paramLength][];
					for (int i = 0; i < paramLength; i++) {
						TypeBinding type = parameters[i];
						parameterTypeNames[i] = type.qualifiedSourceName();
					}
					char[][] parameterNames = findMethodParameterNames(constructor,parameterTypeNames);

					char[] completion = CharOperation.NO_CHAR;
					if (this.source != null
						&& this.source.length > this.endPosition
						&& this.source[this.endPosition] == '(')
						completion = name;
					else
						completion = CharOperation.concat(name, new char[] { '(', ')' });

					int relevance = computeBaseRelevance();
					relevance += computeRelevanceForResolution();
					relevance += computeRelevanceForInterestingProposal();
					relevance += computeRelevanceForCaseMatching(this.completionToken, name);
					relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

					this.noProposal = false;
					if(!this.requestor.isIgnored(CompletionProposal.METHOD_REF)) {
						CompletionProposal proposal = this.createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
						proposal.setDeclarationSignature(getSignature(currentType));
						proposal.setSignature(getSignature(constructor));
						MethodBinding original = constructor.original();
						if(original != constructor) {
							proposal.setOriginalSignature(getSignature(original));
						}
						proposal.setDeclarationPackageName(currentType.qualifiedPackageName());
						proposal.setDeclarationTypeName(currentType.qualifiedSourceName());
						proposal.setParameterTypeNames(parameterTypeNames);
						proposal.setName(name);
						proposal.setIsContructor(true);
						proposal.setCompletion(completion);
						proposal.setFlags(constructor.modifiers);
						proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
						proposal.setRelevance(relevance);
						if(parameterNames != null) proposal.setParameterNames(parameterNames);
						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				}
			}
		}
	}


	private void findConstructors(
		ReferenceBinding currentType,
		TypeBinding[] argTypes,
		Scope scope,
		InvocationSite invocationSite,
		boolean forAnonymousType) {

		// No visibility checks can be performed without the scope & invocationSite
		MethodBinding[] methods = currentType.availableMethods();
		if(methods != null) {
			int minArgLength = argTypes == null ? 0 : argTypes.length;
			next : for (int f = methods.length; --f >= 0;) {
				MethodBinding constructor = methods[f];
				if (constructor.isConstructor()) {

					if (this.options.checkDeprecation &&
							constructor.isViewedAsDeprecated() &&
							!scope.isDefinedInSameUnit(constructor.declaringClass))
						continue next;

					if (this.options.checkVisibility
						&& !constructor.canBeSeenBy(invocationSite, scope)) {
						if(!forAnonymousType || !constructor.isProtected())
							continue next;
					}

					TypeBinding[] parameters = constructor.parameters;
					int paramLength = parameters.length;
					if (minArgLength > paramLength)
						continue next;
					for (int a = minArgLength; --a >= 0;)
						if (argTypes[a] != null) { // can be null if it could not be resolved properly
							if (!argTypes[a].isCompatibleWith(constructor.parameters[a]))
								continue next;
						}

					char[][] parameterTypeNames = new char[paramLength][];
					for (int i = 0; i < paramLength; i++) {
						TypeBinding type = parameters[i];
						parameterTypeNames[i] = type.qualifiedSourceName();
					}
					char[][] parameterNames = findMethodParameterNames(constructor,parameterTypeNames);

					char[] bindingName = constructor.selector;
					char[] completion = bindingName;
					if (this.source != null
						&& this.source.length > this.endPosition
						&& this.source[this.endPosition] == '(') {
						completion = bindingName;
					} else {
						completion = CharOperation.concat(bindingName, new char[] { '(', ')' });
					}
					
					if(forAnonymousType){
						int relevance = computeBaseRelevance();
						relevance += computeRelevanceForResolution();
						relevance += computeRelevanceForInterestingProposal();
						relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

						this.noProposal = false;
						if(!this.requestor.isIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION)) {
							CompletionProposal proposal = this.createProposal(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, this.actualCompletionPosition);
							proposal.setDeclarationSignature(getSignature(currentType));
							proposal.setDeclarationKey(currentType.computeUniqueKey());
							proposal.setSignature(getSignature(constructor));
							MethodBinding original = constructor.original();
							if(original != constructor) {
								proposal.setOriginalSignature(getSignature(original));
							}
							proposal.setKey(constructor.computeUniqueKey());
							proposal.setDeclarationPackageName(currentType.qualifiedPackageName());
							proposal.setDeclarationTypeName(currentType.qualifiedSourceName());
							proposal.setParameterTypeNames(parameterTypeNames);
							proposal.setCompletion(completion);
							proposal.setFlags(constructor.modifiers);
							proposal.setReplaceRange(this.endPosition - this.offset, this.endPosition - this.offset);
							proposal.setRelevance(relevance);
							if(parameterNames != null) proposal.setParameterNames(parameterNames);
							this.requestor.accept(proposal);
							if(DEBUG) {
								this.printDebug(proposal);
							}
						}
					} else {
						int relevance = computeBaseRelevance();
						relevance += computeRelevanceForResolution();
						relevance += computeRelevanceForInterestingProposal();
						relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

						// Special case for completion in javadoc
						if (this.assistNodeInJavadoc > 0) {
							Expression receiver = null;
							char[] selector = null;
							if (invocationSite instanceof CompletionOnJavadocAllocationExpression) {
								CompletionOnJavadocAllocationExpression alloc = (CompletionOnJavadocAllocationExpression) invocationSite;
								receiver = alloc.type;
							} else if (invocationSite instanceof CompletionOnJavadocFieldReference) {
								CompletionOnJavadocFieldReference fieldRef = (CompletionOnJavadocFieldReference) invocationSite;
								receiver = fieldRef.receiver;
							}
							if (receiver != null) {
								StringBuffer javadocCompletion = new StringBuffer();
								if (receiver.isThis()) {
									selector = (((JavadocImplicitTypeReference)receiver).token);
									if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0) {
										javadocCompletion.append('#');
									}
								} else if (receiver instanceof JavadocSingleTypeReference) {
									JavadocSingleTypeReference typeRef = (JavadocSingleTypeReference) receiver;
									selector = typeRef.token;
									if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0) {
										javadocCompletion.append(typeRef.token);
										javadocCompletion.append('#');
									}
								} else if (receiver instanceof JavadocQualifiedTypeReference) {
									JavadocQualifiedTypeReference typeRef = (JavadocQualifiedTypeReference) receiver;
									selector = typeRef.tokens[typeRef.tokens.length-1];
									if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0) {
										javadocCompletion.append(CharOperation.concatWith(typeRef.tokens, '.'));
										javadocCompletion.append('#');
									}
								}
								// Append parameters types
								javadocCompletion.append(selector);
								javadocCompletion.append('(');
								if (constructor.parameters != null) {
									boolean isVarargs = constructor.isVarargs();
									for (int p=0, ln=constructor.parameters.length; p<ln; p++) {
										if (p>0) javadocCompletion.append(", "); //$NON-NLS-1$
										TypeBinding argTypeBinding = constructor.parameters[p];
										if (isVarargs && p == ln - 1)  {
											createVargsType(argTypeBinding, javadocCompletion);
										} else {
											createType(argTypeBinding, javadocCompletion);
										}
									}
								}
								javadocCompletion.append(')');
								completion = javadocCompletion.toString().toCharArray();
							}
						}

						// Create standard proposal
						this.noProposal = false;
						if(!this.requestor.isIgnored(CompletionProposal.METHOD_REF) && (this.assistNodeInJavadoc & CompletionOnJavadoc.ONLY_INLINE_TAG) == 0) {
							CompletionProposal proposal = this.createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
							proposal.setDeclarationSignature(getSignature(currentType));
							proposal.setSignature(getSignature(constructor));
							MethodBinding original = constructor.original();
							if(original != constructor) {
								proposal.setOriginalSignature(getSignature(original));
							}
							proposal.setDeclarationPackageName(currentType.qualifiedPackageName());
							proposal.setDeclarationTypeName(currentType.qualifiedSourceName());
							proposal.setParameterTypeNames(parameterTypeNames);
							proposal.setName(currentType.sourceName());
							proposal.setIsContructor(true);
							proposal.setCompletion(completion);
							proposal.setFlags(constructor.modifiers);
							proposal.setReplaceRange(this.startPosition  - this.offset, this.endPosition - this.offset);
							proposal.setRelevance(relevance);
							if(parameterNames != null) proposal.setParameterNames(parameterNames);
							this.requestor.accept(proposal);
							if(DEBUG) {
								this.printDebug(proposal);
							}
						}
						if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JSDOC_METHOD_REF)) {
							char[] javadocCompletion = inlineTagCompletion(completion, JavadocTagConstants.TAG_LINK);
							CompletionProposal proposal = this.createProposal(CompletionProposal.JSDOC_METHOD_REF, this.actualCompletionPosition);
							proposal.setDeclarationSignature(getSignature(currentType));
							proposal.setSignature(getSignature(constructor));
							MethodBinding original = constructor.original();
							if(original != constructor) {
								proposal.setOriginalSignature(getSignature(original));
							}
							proposal.setDeclarationPackageName(currentType.qualifiedPackageName());
							proposal.setDeclarationTypeName(currentType.qualifiedSourceName());
							proposal.setParameterTypeNames(parameterTypeNames);
							proposal.setName(currentType.sourceName());
							proposal.setIsContructor(true);
							proposal.setCompletion(javadocCompletion);
							proposal.setFlags(constructor.modifiers);
							int start = (this.assistNodeInJavadoc > 0) ? this.startPosition : this.endPosition;
							if ((this.assistNodeInJavadoc & CompletionOnJavadoc.REPLACE_TAG) != 0) start = this.javadocTagPosition;
							proposal.setReplaceRange(start - this.offset, this.endPosition - this.offset);
							proposal.setRelevance(relevance+R_INLINE_TAG);
							if(parameterNames != null) proposal.setParameterNames(parameterNames);
							this.requestor.accept(proposal);
							if(DEBUG) {
								this.printDebug(proposal);
							}
						}
					}
				}
			}
		}
	}

	// Helper method for findFields(char[], ReferenceBinding, Scope, ObjectVector, boolean)
	private void findFields(
		char[] fieldName,
		FieldBinding[] fields,
		Scope scope,
		ObjectVector fieldsFound,
		ObjectVector localsFound,
		boolean onlyStaticFields,
		ReferenceBinding receiverType,
		InvocationSite invocationSite,
		Scope invocationScope,
		boolean implicitCall,
		boolean canBePrefixed,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems) {

		ObjectVector newFieldsFound = new ObjectVector();
		// Inherited fields which are hidden by subclasses are filtered out
		// No visibility checks can be performed without the scope & invocationSite

		int fieldLength = fieldName.length;
		next : for (int f = fields.length; --f >= 0;) {
			FieldBinding field = fields[f];

			if (onlyStaticFields && !field.isStatic()) continue next;

			if (fieldLength > field.name.length) continue next;

			if (!CharOperation.prefixEquals(fieldName, field.name, false /* ignore case */)
					&& !(this.options.camelCaseMatch && CharOperation.camelCaseMatch(fieldName, field.name)))	continue next;

			if (this.options.checkDeprecation &&
					field.isViewedAsDeprecated() &&
					!scope.isDefinedInSameUnit(field.declaringClass))
				continue next;

			if (this.options.checkVisibility
				&& !field.canBeSeenBy(receiverType, invocationSite, scope))	continue next;

			boolean prefixRequired = false;

			for (int i = fieldsFound.size; --i >= 0;) {
				Object[] other = (Object[])fieldsFound.elementAt(i);
				FieldBinding otherField = (FieldBinding) other[0];
				ReferenceBinding otherReceiverType = (ReferenceBinding) other[1];
				if (field == otherField && receiverType == otherReceiverType)
					continue next;
				if (CharOperation.equals(field.name, otherField.name, true)) {
					if (field.declaringClass.isSuperclassOf(otherField.declaringClass))
						continue next;
					if(canBePrefixed) {
						prefixRequired = true;
					} else {
						continue next;
					}
				}
			}

			for (int l = localsFound.size; --l >= 0;) {
				LocalVariableBinding local = (LocalVariableBinding) localsFound.elementAt(l);

				if (CharOperation.equals(field.name, local.name, true)) {
					SourceTypeBinding declarationType = scope.enclosingSourceType();
					if (declarationType.isAnonymousType() && declarationType != invocationScope.enclosingSourceType()) {
						continue next;
					}
					if(canBePrefixed) {
						prefixRequired = true;
					} else {
						continue next;
					}
					break;
				}
			}

			newFieldsFound.add(new Object[]{field, receiverType});
			
			this.proposeField(field);
		}

		fieldsFound.addAll(newFieldsFound);
	}

	private void findFields(
		char[] fieldName,
		ReferenceBinding receiverType,
		Scope scope,
		ObjectVector fieldsFound,
		ObjectVector localsFound,
		boolean onlyStaticFields,
		InvocationSite invocationSite,
		Scope invocationScope,
		boolean implicitCall,
		boolean canBePrefixed,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems) {

		boolean notInJavadoc = this.assistNodeInJavadoc == 0;
		if (fieldName == null && notInJavadoc)
			return;

		ReferenceBinding currentType = receiverType;
		do {

			FieldBinding[] fields = currentType.availableFields();
			if(fields != null && fields.length > 0) {
				findFields(
					fieldName,
					fields,
					scope,
					fieldsFound,
					localsFound,
					onlyStaticFields,
					receiverType,
					invocationSite,
					invocationScope,
					implicitCall,
					canBePrefixed,
					missingElements,
					missingElementsStarts,
					missingElementsEnds,
					missingElementsHaveProblems);
			}
			
			currentType = currentType.getSuperBinding();
		} while (notInJavadoc && currentType != null);
	}

	protected void findFieldsAndMethods(
		char[] token,
		TypeBinding receiverType,
		Scope scope,
		InvocationSite invocationSite,
		Scope invocationScope,
		boolean staticsOnly,
		boolean implicitCall,
		boolean superCall,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems) {

		if (token == null)
			return;

		if (receiverType.isBaseType())
			return; // nothing else is possible with base types

		boolean proposeField = !this.isIgnored(CompletionProposal.FIELD_REF, missingElements != null);
		boolean proposeMethod = !this.isIgnored(CompletionProposal.METHOD_REF, missingElements != null);

		ObjectVector methodsFound = new ObjectVector();
		ObjectVector fieldsFound = new ObjectVector();
		
		if(proposeField) {
			findFields(
				token,
				(ReferenceBinding) receiverType,
				scope,
				fieldsFound,
				new ObjectVector(),
				staticsOnly,
				invocationSite,
				invocationScope,
				implicitCall,
				false,
				missingElements,
				missingElementsStarts,
				missingElementsEnds,
				missingElementsHaveProblems);
		}

		if(proposeMethod) {
			findMethods(
				token,
				null,
				null,
				(ReferenceBinding) receiverType,
				scope,
				methodsFound,
				staticsOnly,
				false,
				false,
				invocationSite,
				invocationScope,
				implicitCall,
				superCall,
				false,
				missingElements,
				missingElementsStarts,
				missingElementsEnds,
				missingElementsHaveProblems);
		}
	}

	private void findFieldsAndMethodsFromFavorites(
			char[] token,
			Scope scope,
			InvocationSite invocationSite,
			Scope invocationScope,
			ObjectVector localsFound,
			ObjectVector fieldsFound,
			ObjectVector methodsFound) {

		ImportBinding[] favoriteBindings = getFavoriteReferenceBindings(invocationScope);

		if (favoriteBindings != null && favoriteBindings.length > 0) {
			for (int i = 0; i < favoriteBindings.length; i++) {
				ImportBinding favoriteBinding = favoriteBindings[i];
				switch (favoriteBinding.resolvedImport.kind()) {
					case Binding.FIELD:
						FieldBinding fieldBinding = (FieldBinding) favoriteBinding.resolvedImport;
						findFieldsFromFavorites(
								token,
								new FieldBinding[]{fieldBinding},
								scope,
								fieldsFound,
								localsFound,
								fieldBinding.declaringClass,
								invocationSite,
								invocationScope);
						break;
					case Binding.METHOD:
						MethodBinding methodBinding = (MethodBinding) favoriteBinding.resolvedImport;
						MethodBinding[] methods = methodBinding.declaringClass.availableMethods();
						long range;
						if ((range = ReferenceBinding.binarySearch(methodBinding.selector, methods)) >= 0) {
							int start = (int) range, end = (int) (range >> 32);
							int length = end - start + 1;
							System.arraycopy(methods, start, methods = new MethodBinding[length], 0, length);
						} else {
							methods = Binding.NO_METHODS;
						}
						findLocalMethodsFromFavorites(
								token,
								methods,
								scope,
								methodsFound,
								methodBinding.declaringClass,
								invocationSite,
								invocationScope);
						break;
					case Binding.TYPE:
						ReferenceBinding referenceBinding = (ReferenceBinding) favoriteBinding.resolvedImport;
						if(favoriteBinding.onDemand) {
							findFieldsFromFavorites(
									token,
									referenceBinding.availableFields(),
									scope,
									fieldsFound,
									localsFound,
									referenceBinding,
									invocationSite,
									invocationScope);

							findLocalMethodsFromFavorites(
									token,
									referenceBinding.availableMethods(),
									scope,
									methodsFound,
									referenceBinding,
									invocationSite,
									invocationScope);
						}
						break;
				}
			}
		}
	}

	private void findFieldsAndMethodsFromMissingFieldType(
		char[] token,
		Scope scope,
		InvocationSite invocationSite,
		boolean insideTypeAnnotation) {

		boolean staticsOnly = false;
		Scope currentScope = scope;

		done : while (true) { // done when a COMPILATION_UNIT_SCOPE is found

			switch (currentScope.kind) {

				case Scope.METHOD_SCOPE :
					// handle the error case inside an explicit constructor call (see MethodScope>>findField)
					MethodScope methodScope = (MethodScope) currentScope;
					staticsOnly |= methodScope.isStatic | methodScope.isConstructorCall;

				//$FALL-THROUGH$ - fall through is done on purpose
				case Scope.BLOCK_SCOPE :
					break;

				case Scope.CLASS_SCOPE :
					ClassScope classScope = (ClassScope) currentScope;
					SourceTypeBinding enclosingType = classScope.referenceContext.binding;
					if(!insideTypeAnnotation) {

						FieldDeclaration[] fields = classScope.referenceContext.fields;

						int fieldsCount = fields == null ? 0 : fields.length;
						for (int i = 0; i < fieldsCount; i++) {
							FieldDeclaration fieldDeclaration = fields[i];
							if (CharOperation.equals(fieldDeclaration.name, token)) {
								if (fieldDeclaration.binding == null) {
									findFieldsAndMethodsFromMissingType(
											this.completionToken,
											fieldDeclaration.type,
											currentScope,
											invocationSite,
											scope);
								}
								break done;
							}
						}
					}
					staticsOnly |= enclosingType.isStatic();
					insideTypeAnnotation = false;
					break;
				case Scope.COMPILATION_UNIT_SCOPE :
					break done;
			}
			currentScope = currentScope.parent;
		}
	}

	private void findFieldsAndMethodsFromMissingReturnType(
		char[] token,
		TypeBinding[] arguments,
		Scope scope,
		InvocationSite invocationSite,
		boolean insideTypeAnnotation) {

		boolean staticsOnly = false;
		Scope currentScope = scope;

		done : while (true) { // done when a COMPILATION_UNIT_SCOPE is found

			switch (currentScope.kind) {

				case Scope.METHOD_SCOPE :
					// handle the error case inside an explicit constructor call (see MethodScope>>findField)
					MethodScope methodScope = (MethodScope) currentScope;
					staticsOnly |= methodScope.isStatic | methodScope.isConstructorCall;

				//$FALL-THROUGH$ - fall through is done on purpose
				case Scope.BLOCK_SCOPE :
					break;

				case Scope.CLASS_SCOPE :
					ClassScope classScope = (ClassScope) currentScope;
					SourceTypeBinding enclosingType = classScope.referenceContext.binding;
					if(!insideTypeAnnotation) {

						AbstractMethodDeclaration[] methods = classScope.referenceContext.methods;

						int methodsCount = methods == null ? 0 : methods.length;
						for (int i = 0; i < methodsCount; i++) {
							AbstractMethodDeclaration methodDeclaration = methods[i];
							if (methodDeclaration instanceof MethodDeclaration &&
									CharOperation.equals(methodDeclaration.getName(), token)) {
								MethodDeclaration method = (MethodDeclaration) methodDeclaration;
								if (methodDeclaration.getBinding() == null) {
									Argument[] parameters = method.arguments;
									int parametersLength = parameters == null ? 0 : parameters.length;
									int argumentsLength = arguments == null ? 0 : arguments.length;

									if (parametersLength == 0) {
										if (argumentsLength == 0) {
											findFieldsAndMethodsFromMissingType(
													this.completionToken,
													method.returnType,
													currentScope,
													invocationSite,
													scope);
											break done;
										}
									} else {
										TypeBinding[] parametersBindings = new TypeBinding[parametersLength];
										for (int j = 0; j < parametersLength; j++) {
											parametersBindings[j] = parameters[j].type.resolvedType;
										}
										if(areParametersCompatibleWith(parametersBindings, arguments, parameters[parametersLength - 1].isVarArgs())) {
											findFieldsAndMethodsFromMissingType(
													this.completionToken,
													method.returnType,
													currentScope,
													invocationSite,
													scope);
											break done;
										}
									}
								}

							}
						}
					}
					staticsOnly |= enclosingType.isStatic();
					insideTypeAnnotation = false;
					break;
				case Scope.COMPILATION_UNIT_SCOPE :
					break done;
			}
			currentScope = currentScope.parent;
		}
	}

	private void findFieldsAndMethodsFromMissingType(
			final char[] token,
			TypeReference typeRef,
			final Scope scope,
			final InvocationSite invocationSite,
			final Scope invocationScope) {
		MissingTypesGuesser missingTypesConverter = new MissingTypesGuesser(this);
		MissingTypesGuesser.GuessedTypeRequestor substitutionRequestor =
			new MissingTypesGuesser.GuessedTypeRequestor() {
				public void accept(
						TypeBinding guessedType,
						Binding[] missingElements,
						int[] missingElementsStarts,
						int[] missingElementsEnds,
						boolean hasProblems) {
					findFieldsAndMethods(
						CompletionEngine.this.completionToken,
						guessedType,
						scope,
						invocationSite,
						invocationScope,
						false,
						false,
						false,
						missingElements,
						missingElementsStarts,
						missingElementsEnds,
						hasProblems);

				}
			};
		missingTypesConverter.guess(typeRef, scope, substitutionRequestor);
	}

	private void findFieldsFromFavorites(
			char[] fieldName,
			FieldBinding[] fields,
			Scope scope,
			ObjectVector fieldsFound,
			ObjectVector localsFound,
			ReferenceBinding receiverType,
			InvocationSite invocationSite,
			Scope invocationScope) {

		char[] typeName = CharOperation.concatWith(receiverType.compoundName, '.');

		int fieldLength = fieldName.length;
		next : for (int f = fields.length; --f >= 0;) {
			FieldBinding field = fields[f];

			// only static fields must be proposed
			if (!field.isStatic()) continue next;

			if (fieldLength > field.name.length) continue next;

			if (!CharOperation.prefixEquals(fieldName, field.name, false /* ignore case */)
					&& !(this.options.camelCaseMatch && CharOperation.camelCaseMatch(fieldName, field.name)))	continue next;

			if (this.options.checkDeprecation &&
					field.isViewedAsDeprecated() &&
					!scope.isDefinedInSameUnit(field.declaringClass))
				continue next;

			if (this.options.checkVisibility
				&& !field.canBeSeenBy(receiverType, invocationSite, scope))	continue next;

			for (int i = fieldsFound.size; --i >= 0;) {
				Object[] other = (Object[])fieldsFound.elementAt(i);
				FieldBinding otherField = (FieldBinding) other[0];

				if (field == otherField) continue next;
			}

			fieldsFound.add(new Object[]{field, receiverType});

			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal(field);
			relevance += computeRelevanceForCaseMatching(fieldName, field.name);
			relevance += computeRelevanceForExpectingType(field.type);
			relevance += computeRelevanceForStatic(true, true);
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

			CompilationUnitDeclaration cu = this.unitScope.referenceContext;
			int importStart = cu.types[0].declarationSourceStart;
			int importEnd = importStart;

			this.noProposal = false;

			if (this.compilerOptions.complianceLevel < ClassFileConstants.JDK1_5 ||
					!this.options.suggestStaticImport) {
				if (!this.isIgnored(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_IMPORT)) {
					char[] completion = CharOperation.concat(receiverType.sourceName, field.name, '.');

					CompletionProposal proposal = this.createProposal(CompletionProposal.FIELD_REF, this.actualCompletionPosition);
					proposal.setDeclarationSignature(getSignature(field.declaringClass));
					proposal.setSignature(getSignature(field.type));
					proposal.setDeclarationPackageName(field.declaringClass.qualifiedPackageName());
					proposal.setDeclarationTypeName(field.declaringClass.qualifiedSourceName());
					proposal.setReturnQualification(field.type.qualifiedPackageName());
					proposal.setReturnSimpleName(field.type.qualifiedSourceName());
					proposal.setName(field.name);
					proposal.setCompletion(completion);
					proposal.setFlags(field.modifiers);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setRelevance(relevance);

					char[] typeImportCompletion = createImportCharArray(typeName, false, false);

					CompletionProposal typeImportProposal = this.createProposal(CompletionProposal.TYPE_IMPORT, this.actualCompletionPosition);
					typeImportProposal.nameLookup = this.nameEnvironment.nameLookup;
					typeImportProposal.completionEngine = this;
					char[] packageName = receiverType.qualifiedPackageName();
					typeImportProposal.setDeclarationSignature(packageName);
					typeImportProposal.setSignature(getSignature(receiverType));
					typeImportProposal.setReturnQualification(packageName);
					typeImportProposal.setReturnSimpleName(receiverType.qualifiedSourceName());
					typeImportProposal.setCompletion(typeImportCompletion);
					typeImportProposal.setFlags(receiverType.modifiers);
					typeImportProposal.setAdditionalFlags(CompletionFlags.Default);
					typeImportProposal.setReplaceRange(importStart - this.offset, importEnd - this.offset);
					typeImportProposal.setRelevance(relevance);

					proposal.setRequiredProposals(new CompletionProposal[]{typeImportProposal});

					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}
			} else {
				if (!this.isIgnored(CompletionProposal.FIELD_REF, CompletionProposal.FIELD_IMPORT)) {
					char[] completion = field.name;

					CompletionProposal proposal = this.createProposal(CompletionProposal.FIELD_REF, this.actualCompletionPosition);
					proposal.setDeclarationSignature(getSignature(field.declaringClass));
					proposal.setSignature(getSignature(field.type));
					proposal.setDeclarationPackageName(field.declaringClass.qualifiedPackageName());
					proposal.setDeclarationTypeName(field.declaringClass.qualifiedSourceName());
					proposal.setReturnQualification(field.type.qualifiedPackageName());
					proposal.setReturnSimpleName(field.type.qualifiedSourceName());
					proposal.setName(field.name);
					proposal.setCompletion(completion);
					proposal.setFlags(field.modifiers);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setRelevance(relevance);

					char[] fieldImportCompletion = createImportCharArray(CharOperation.concat(typeName, field.name, '.'), true, false);

					CompletionProposal fieldImportProposal = this.createProposal(CompletionProposal.FIELD_IMPORT, this.actualCompletionPosition);
					fieldImportProposal.setDeclarationSignature(getSignature(field.declaringClass));
					fieldImportProposal.setSignature(getSignature(field.type));
					fieldImportProposal.setDeclarationPackageName(field.declaringClass.qualifiedPackageName());
					fieldImportProposal.setDeclarationTypeName(field.declaringClass.qualifiedSourceName());
					fieldImportProposal.setReturnQualification(field.type.qualifiedPackageName());
					fieldImportProposal.setReturnSimpleName(field.type.qualifiedSourceName());
					fieldImportProposal.setName(field.name);
					fieldImportProposal.setCompletion(fieldImportCompletion);
					fieldImportProposal.setFlags(field.modifiers);
					fieldImportProposal.setReplaceRange(importStart - this.offset, importEnd - this.offset);
					fieldImportProposal.setRelevance(relevance);

					proposal.setRequiredProposals(new CompletionProposal[]{fieldImportProposal});

					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}
			}
		}
	}

	private void findImports(CompletionOnImportReference importReference, boolean findMembers) {
		char[][] tokens = importReference.tokens;

		char[] importName = CharOperation.concatWith(tokens, '.');

		if (importName.length == 0)
			return;

		char[] lastToken = tokens[tokens.length - 1];
		if(lastToken != null && lastToken.length == 0)
			importName = CharOperation.concat(importName, new char[]{'.'});

		this.resolvingImports = true;

		this.completionToken =  importName;
		// want to replace the existing .*;
//		if(!this.requestor.isIgnored(CompletionProposal.PACKAGE_REF)) {
//			this.nameEnvironment.findPackages(importName, this);
//		}
		if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
			this.nameEnvironment.findTypes(
					importName,
					findMembers,
					this.options.camelCaseMatch,
					IJavaScriptSearchConstants.TYPE,
					this);
			acceptTypes(null);
		}
	}

	private void findImportsOfMemberTypes(char[] typeName,	ReferenceBinding ref) {
		ReferenceBinding[] memberTypes = ref.memberTypes();

		int typeLength = typeName.length;
		next : for (int m = memberTypes.length; --m >= 0;) {
			ReferenceBinding memberType = memberTypes[m];
			//		if (!wantClasses && memberType.isClass()) continue next;
			//		if (!wantInterfaces && memberType.isInterface()) continue next;

			if (typeLength > memberType.sourceName.length)
				continue next;

			if (!CharOperation.prefixEquals(typeName, memberType.sourceName, false/* ignore case */)
					&& !(this.options.camelCaseMatch && CharOperation.camelCaseMatch(typeName, memberType.sourceName)))
				continue next;

			if (this.options.checkDeprecation && memberType.isViewedAsDeprecated()) continue next;

			if (this.options.checkVisibility
				&& !memberType.canBeSeenBy(this.unitScope.getDefaultPackage()))
				continue next;

			char[] completionName = CharOperation.concat(
					memberType.qualifiedPackageName(),
					memberType.qualifiedSourceName(),
					'.');

			completionName = CharOperation.concat(completionName, SEMICOLON);

			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
		relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForCaseMatching(typeName, memberType.sourceName);
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

			if (memberType.isClass()) {
				relevance += computeRelevanceForClass();
			}
			this.noProposal = false;
			if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
				createTypeProposal(memberType, memberType.qualifiedSourceName(), IAccessRule.K_ACCESSIBLE, completionName, relevance);
			}
		}
	}

	/*
	 * Find javadoc block tags for a given completion javadoc tag node
	 */
	private void findJavadocBlockTags(CompletionOnJavadocTag javadocTag) {
		char[][] possibleTags = javadocTag.getPossibleBlockTags();
		if (possibleTags == null) return;
		int length = possibleTags.length;
		for (int i=0; i<length; i++) {
			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for keywors

			this.noProposal = false;
			if (!this.requestor.isIgnored(CompletionProposal.JSDOC_BLOCK_TAG)) {
				char[] possibleTag = possibleTags[i];
				CompletionProposal proposal = this.createProposal(CompletionProposal.JSDOC_BLOCK_TAG, this.actualCompletionPosition);
				proposal.setName(possibleTag);
				int tagLength = possibleTag.length;
				char[] completion = new char[1+tagLength];
				completion[0] = '@';
				System.arraycopy(possibleTag, 0, completion, 1, tagLength);
				proposal.setCompletion(completion);
				proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
				proposal.setRelevance(relevance);
				this.requestor.accept(proposal);
				if (DEBUG) {
					this.printDebug(proposal);
				}
			}
		}
	}

	/*
	 * Find javadoc inline tags for a given completion javadoc tag node
	 */
	private void findJavadocInlineTags(CompletionOnJavadocTag javadocTag) {
		char[][] possibleTags = javadocTag.getPossibleInlineTags();
		if (possibleTags == null) return;
		int length = possibleTags.length;
		for (int i=0; i<length; i++) {
			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for keywors

			this.noProposal = false;
			if (!this.requestor.isIgnored(CompletionProposal.JSDOC_INLINE_TAG)) {
				char[] possibleTag = possibleTags[i];
				CompletionProposal proposal = this.createProposal(CompletionProposal.JSDOC_INLINE_TAG, this.actualCompletionPosition);
				proposal.setName(possibleTag);
				int tagLength = possibleTag.length;
//				boolean inlineTagStarted = javadocTag.completeInlineTagStarted();
				char[] completion = new char[2+tagLength+1];
				completion[0] = '{';
				completion[1] = '@';
				System.arraycopy(possibleTag, 0, completion, 2, tagLength);
				// do not add space at end of inline tag (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=121026)
				//completion[tagLength+2] = ' ';
				completion[tagLength+2] = '}';
				proposal.setCompletion(completion);
				proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
				proposal.setRelevance(relevance);
				this.requestor.accept(proposal);
				if (DEBUG) {
					this.printDebug(proposal);
				}
			}
		}
	}

	// what about onDemand types? Ignore them since it does not happen!
	// import p1.p2.A.*;
	private void findKeywords(char[] keyword, char[][] choices, boolean canCompleteEmptyToken, boolean staticFieldsAndMethodOnly) {
		if(choices == null || choices.length == 0) return;

		int length = keyword.length;
		if (canCompleteEmptyToken || length > 0)
			for (int i = 0; i < choices.length; i++)
				if (length <= choices[i].length
					&& CharOperation.prefixEquals(keyword, choices[i], false /* ignore case */
				)){
					int relevance = computeBaseRelevance();
					relevance += computeRelevanceForResolution();
					relevance += computeRelevanceForInterestingProposal();
					relevance += computeRelevanceForCaseMatching(keyword, choices[i]);
					relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for keywors
					if (staticFieldsAndMethodOnly && this.insideQualifiedReference) relevance += R_NON_INHERITED;

					if(CharOperation.equals(choices[i], Keywords.TRUE) || CharOperation.equals(choices[i], Keywords.FALSE)) {
						relevance += computeRelevanceForExpectingType(TypeBinding.BOOLEAN);
						relevance += computeRelevanceForQualification(false);
					}
					this.noProposal = false;
					if(!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
						CompletionProposal proposal = this.createProposal(CompletionProposal.KEYWORD, this.actualCompletionPosition);
						proposal.setName(choices[i]);
						proposal.setCompletion(choices[i]);
						proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
						proposal.setRelevance(relevance);
						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				}
	}
	private void findTrueOrFalseKeywords(char[][] choices) {
		if(choices == null || choices.length == 0) return;

		if(this.expectedTypesPtr != 0 || this.expectedTypes[0] != TypeBinding.BOOLEAN) return;

		for (int i = 0; i < choices.length; i++) {
			if (CharOperation.equals(choices[i], Keywords.TRUE) ||
					CharOperation.equals(choices[i], Keywords.FALSE)
			){
				int relevance = computeBaseRelevance();
				relevance += computeRelevanceForResolution();
				relevance += computeRelevanceForInterestingProposal();
				relevance += computeRelevanceForCaseMatching(CharOperation.NO_CHAR, choices[i]);
				relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for keywors
				relevance += computeRelevanceForExpectingType(TypeBinding.BOOLEAN);
				relevance += computeRelevanceForQualification(false);
				relevance += R_TRUE_OR_FALSE;

				this.noProposal = false;
				if(!this.requestor.isIgnored(CompletionProposal.KEYWORD)) {
					CompletionProposal proposal = this.createProposal(CompletionProposal.KEYWORD, this.actualCompletionPosition);
					proposal.setName(choices[i]);
					proposal.setCompletion(choices[i]);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setRelevance(relevance);
					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}
			}
		}
	}

	private void findKeywordsForMember(char[] token, int modifiers) {
		char[][] keywords = new char[Keywords.COUNT][];
		int count = 0;

		// visibility
		if((modifiers & ClassFileConstants.AccPrivate) == 0
			&& (modifiers & ClassFileConstants.AccProtected) == 0
			&& (modifiers & ClassFileConstants.AccPublic) == 0) {
			keywords[count++] = Keywords.PROTECTED;
			keywords[count++] = Keywords.PUBLIC;
			if((modifiers & ClassFileConstants.AccAbstract) == 0) {
				keywords[count++] = Keywords.PRIVATE;
			}
		}

		if((modifiers & ClassFileConstants.AccAbstract) == 0) {
			// abtract
			if((modifiers & ~(ExtraCompilerModifiers.AccVisibilityMASK | ClassFileConstants.AccStatic)) == 0) {
				keywords[count++] = Keywords.ABSTRACT;
			}

			// final
			if((modifiers & ClassFileConstants.AccFinal) == 0) {
				keywords[count++] = Keywords.FINAL;
			}

			// static
			if((modifiers & ClassFileConstants.AccStatic) == 0) {
				keywords[count++] = Keywords.STATIC;
			}

			boolean canBeField = true;
			boolean canBeMethod = true;
			boolean canBeType = true;
			if((modifiers & ClassFileConstants.AccNative) != 0
				|| (modifiers & ClassFileConstants.AccStrictfp) != 0) {
				canBeField = false;
				canBeType = false;
			}


			if(canBeField) {
				// transient
				keywords[count++] = Keywords.TRANSIENT;
				

				// volatile
				keywords[count++] = Keywords.VOLATILE;
			}

			if(canBeMethod) {
				// native
				if((modifiers & ClassFileConstants.AccNative) == 0) {
					keywords[count++] = Keywords.NATIVE;
				}

				// strictfp
				if((modifiers & ClassFileConstants.AccStrictfp) == 0) {
					keywords[count++] = Keywords.STRICTFP;
				}

				// synchronized
				keywords[count++] = Keywords.SYNCHRONIZED;
				
			}

			if(canBeType) {
				keywords[count++] = Keywords.CLASS;
				keywords[count++] = Keywords.INTERFACE;
			}
		} else {
			// class
			keywords[count++] = Keywords.CLASS;
			keywords[count++] = Keywords.INTERFACE;
		}
		System.arraycopy(keywords, 0, keywords = new char[count][], 0, count);

		findKeywords(token, keywords, false, false);
	}

	private void findMemberTypes(
		char[] typeName,
		ReferenceBinding receiverType,
		Scope scope,
		SourceTypeBinding typeInvocation,
		boolean staticOnly,
		boolean staticFieldsAndMethodOnly,
		ObjectVector typesFound)  {
		findMemberTypes(
				typeName,
				receiverType,
				scope,
				typeInvocation,
				staticOnly,
				staticFieldsAndMethodOnly,
				false,
				false,
				false,
				null,
				typesFound);
	}
	private void findMemberTypes(
		char[] typeName,
		ReferenceBinding receiverType,
		Scope scope,
		SourceTypeBinding typeInvocation,
		boolean staticOnly,
		boolean staticFieldsAndMethodOnly,
		boolean fromStaticImport,
		boolean checkQualification,
		boolean proposeAllMemberTypes,
		SourceTypeBinding typeToIgnore,
		ObjectVector typesFound) {

		if (typeName == null)
			return;

		if (this.assistNodeIsSuperType && !this.insideQualifiedReference) return; // we're trying to find a supertype

		return; // we're trying to find a supertype
	}

	/*
	 * Find javadoc parameter names.
	 */
	private void findJavadocParamNames(char[] token, char[][] missingParams, boolean isTypeParam) {

		if (missingParams == null) return;

		// Get relevance
		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForInterestingProposal();
		relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for param name
		if (!isTypeParam) relevance += R_INTERESTING;

		// Propose missing param
		int length = missingParams.length;
		relevance += length;
		for (int i=0; i<length; i++) {
			char[] argName = missingParams[i];
			if (token == null || CharOperation.prefixEquals(token, argName)) {

				this.noProposal = false;
				if (!this.requestor.isIgnored(CompletionProposal.JSDOC_PARAM_REF)) {
					CompletionProposal proposal = this.createProposal(CompletionProposal.JSDOC_PARAM_REF, this.actualCompletionPosition);
					proposal.setName(argName);
					char[] completion = isTypeParam ? CharOperation.concat('<', argName, '>') : argName;
					proposal.setCompletion(completion);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setRelevance(--relevance);
					this.requestor.accept(proposal);
					if (DEBUG) {
						this.printDebug(proposal);
					}
				}
			}
		}
	}

	private void findSubMemberTypes(
		char[] typeName,
		ReferenceBinding receiverType,
		Scope scope,
		SourceTypeBinding typeInvocation,
		boolean staticOnly,
		boolean staticFieldsAndMethodOnly,
		boolean fromStaticImport,
		ObjectVector typesFound) {

		if (typeName == null || typeName.length == 0)
			return;

		return; // we're trying to find a supertype
	}

	private void findImplicitMessageSends(
		char[] token,
		TypeBinding[] argTypes,
		Scope scope,
		InvocationSite invocationSite,
		Scope invocationScope) {

		if (token == null)
			return;

		boolean staticsOnly = false;
		// need to know if we're in a static context (or inside a constructor)
		ObjectVector methodsFound = new ObjectVector();

		done : while (true) { // done when a COMPILATION_UNIT_SCOPE is found

			switch (scope.kind) {

				case Scope.METHOD_SCOPE :
					// handle the error case inside an explicit constructor call (see MethodScope>>findField)
					MethodScope methodScope = (MethodScope) scope;
					staticsOnly |= methodScope.isStatic | methodScope.isConstructorCall;
					break;

				case Scope.CLASS_SCOPE :
					ClassScope classScope = (ClassScope) scope;
					SourceTypeBinding enclosingType = classScope.getReferenceBinding();
					findMethods(
						token,
						null,
						argTypes,
						enclosingType,
						classScope,
						methodsFound,
						staticsOnly,
						true,
						false,
						invocationSite,
						invocationScope,
						true,
						false,
						true,
						null,
						null,
						null,
						false);
					staticsOnly |= enclosingType.isStatic();
					break;

				case Scope.COMPILATION_UNIT_SCOPE :
					CompilationUnitScope compScope = (CompilationUnitScope) scope;
					CompilationUnitBinding compBinding = compScope.enclosingCompilationUnit();
					findMethods(
						token,
						null,
						argTypes,
						compBinding,
						compScope,
						methodsFound,
						staticsOnly,
						false,
						false,
						invocationSite,
						invocationScope,
						true,
						false,
						true,
						null,
						null,
						null,
						false);
					break done;
			}
			scope = scope.parent;
		}
	}

	// Helper method for findMethods(char[], TypeBinding[], ReferenceBinding, Scope, ObjectVector, boolean, boolean, boolean)
	private void findLocalMethods(
		char[] methodName,
		TypeBinding[] argTypes,
		MethodBinding[] methods,
		int numberMethods,
		Scope scope,
		ObjectVector methodsFound,
		boolean onlyStaticMethods,
		boolean exactMatch,
		ReferenceBinding receiverType,
		InvocationSite invocationSite,
		Scope invocationScope,
		boolean implicitCall,
		boolean superCall,
		boolean canBePrefixed,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems) {

		ObjectVector newMethodsFound =  new ObjectVector();
		// Inherited methods which are hidden by subclasses are filtered out
		// No visibility checks can be performed without the scope & invocationSite

		int methodLength = methodName.length;
		int minArgLength = argTypes == null ? 0 : argTypes.length;

		next : for (int f = numberMethods; --f >= 0;) {
			MethodBinding method = methods[f];

			if (method.isDefaultAbstract())	continue next;

			if (method.isConstructor()) continue next;

			if (this.options.checkDeprecation &&
					method.isViewedAsDeprecated() &&
					!scope.isDefinedInSameUnit(method.declaringClass))
				continue next;

			if (onlyStaticMethods && !method.isStatic()) continue next;

			if (this.options.checkVisibility
				&& !method.canBeSeenBy(receiverType, invocationSite, scope)) continue next;

			if(superCall && method.isAbstract()) {
				methodsFound.add(new Object[]{method, receiverType});
				continue next;
			}

			if (exactMatch) {
				if (!CharOperation.equals(methodName, method.selector, false /* ignore case */)) {
					continue next;
				}
			} else {
				if (methodLength > method.selector.length) continue next;
				if (!CharOperation.prefixEquals(methodName, method.selector, false /* ignore case */)
						&& !(this.options.camelCaseMatch && CharOperation.camelCaseMatch(methodName, method.selector))) {
					continue next;
				}
			}

			if (minArgLength > method.parameters.length)
				continue next;

			for (int a = minArgLength; --a >= 0;){
				if (argTypes[a] != null) { // can be null if it could not be resolved properly
					if (!argTypes[a].isCompatibleWith(method.parameters[a])) {
						continue next;
					}
				}
			}

			boolean prefixRequired = false;

			for (int i = methodsFound.size; --i >= 0;) {
				Object[] other = (Object[]) methodsFound.elementAt(i);
				MethodBinding otherMethod = (MethodBinding) other[0];
				ReferenceBinding otherReceiverType = (ReferenceBinding) other[1];
				if (method == otherMethod && receiverType == otherReceiverType)
					continue next;

				if (CharOperation.equals(method.selector, otherMethod.selector, true)) {
					continue next;
				}
			}

			newMethodsFound.add(new Object[]{method, receiverType});

			// Standard proposal
			if(!this.isIgnored(CompletionProposal.METHOD_REF, missingElements != null) && (this.assistNodeInJavadoc & CompletionOnJavadoc.ONLY_INLINE_TAG) == 0) {
				this.proposeFunction(method);
			}

			// Javadoc proposal
			int previousStartPosition = this.startPosition;
			if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JSDOC_METHOD_REF)) {
				int length = method.parameters.length;
				char[][] parameterTypeNames = new char[length][];

				for (int i = 0; i < length; i++) {
					//find first none-anonymous parent type
					TypeBinding type = method.original().parameters[i];
					while(type != null && type.isAnonymousType() && type instanceof ReferenceBinding) {
						type = ((ReferenceBinding)type).getSuperBinding();
					}
					
					//if ended up with null type, use original
					if(type == null) {
						type = method.original().parameters[i];
					}

					parameterTypeNames[i] = type.qualifiedSourceName();
				}
				
				char[] completion = CharOperation.NO_CHAR;
				char[][] parameterNames = findMethodParameterNames(method,parameterTypeNames);
				
				// Special case for completion in javadoc
				if (this.assistNodeInJavadoc > 0) {
					Expression receiver = null;
					if (invocationSite instanceof CompletionOnJavadocMessageSend) {
						CompletionOnJavadocMessageSend msg = (CompletionOnJavadocMessageSend) invocationSite;
						receiver = msg.receiver;
					} else if (invocationSite instanceof CompletionOnJavadocFieldReference) {
						CompletionOnJavadocFieldReference fieldRef = (CompletionOnJavadocFieldReference) invocationSite;
						receiver = fieldRef.receiver;
					}
					if (receiver != null) {
						StringBuffer javadocCompletion = new StringBuffer();
						if (receiver.isThis()) {
							if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0) {
								javadocCompletion.append('#');
							}
						} else if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0) {
							if (receiver instanceof JavadocSingleTypeReference) {
								JavadocSingleTypeReference typeRef = (JavadocSingleTypeReference) receiver;
								javadocCompletion.append(typeRef.token);
								javadocCompletion.append('#');
							} else if (receiver instanceof JavadocQualifiedTypeReference) {
								JavadocQualifiedTypeReference typeRef = (JavadocQualifiedTypeReference) receiver;
								completion = CharOperation.concat(CharOperation.concatWith(typeRef.tokens, '.'), method.selector, '#');
								for (int t=0,nt =typeRef.tokens.length; t<nt; t++) {
									if (t>0) javadocCompletion.append('.');
									javadocCompletion.append(typeRef.tokens[t]);
								}
								javadocCompletion.append('#');
							}
						}
						javadocCompletion.append(method.selector);
						// Append parameters types
						javadocCompletion.append('(');
						if (method.parameters != null) {
							boolean isVarargs = method.isVarargs();
							for (int p=0, ln=method.parameters.length; p<ln; p++) {
								if (p>0) javadocCompletion.append(", "); //$NON-NLS-1$
								TypeBinding argTypeBinding = method.parameters[p];
								if (isVarargs && p == ln - 1)  {
									createVargsType(argTypeBinding, javadocCompletion);
								} else {
									createType(argTypeBinding, javadocCompletion);
								}
							}
						}
						javadocCompletion.append(')');
						completion = javadocCompletion.toString().toCharArray();
					}
				} else {
					// nothing to insert - do not want to replace the existing selector & arguments
					if (!exactMatch) {
						if (this.source != null
							&& this.source.length > this.endPosition
							&& this.source[this.endPosition] == '(')
							completion = method.selector;
						else
							completion = CharOperation.concat(method.selector, new char[] { '(', ')' });
					} else {
						if(prefixRequired && (this.source != null)) {
							completion = CharOperation.subarray(this.source, this.startPosition, this.endPosition);
						} else {
							this.startPosition = this.endPosition;
						}
					}

					if(prefixRequired || this.options.forceImplicitQualification){
						char[] prefix = computePrefix(scope.enclosingSourceType(), invocationScope.enclosingSourceType(), method.isStatic());
						completion = CharOperation.concat(prefix,completion,'.');
					}
				}
				
				int relevance = computeBaseRelevance();
				relevance += computeRelevanceForResolution();
				relevance += computeRelevanceForInterestingProposal();
				relevance += computeRelevanceForCaseMatching(methodName, method.selector);
				relevance += computeRelevanceForExpectingType(method.returnType);
				relevance += computeRelevanceForStatic(onlyStaticMethods, method.isStatic());
				relevance += computeRelevanceForQualification(prefixRequired);
				relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);
				if (onlyStaticMethods && this.insideQualifiedReference) {
					relevance += computeRelevanceForInheritance(receiverType, method.declaringClass);
				}
				if (missingElements != null) {
					relevance += computeRelevanceForMissingElements(missingElementsHaveProblems);
				}
				
				char[] javadocCompletion = inlineTagCompletion(completion, JavadocTagConstants.TAG_LINK);
				CompletionProposal proposal = this.createProposal(CompletionProposal.JSDOC_METHOD_REF, this.actualCompletionPosition);
				proposal.setDeclarationSignature(getSignature(method.declaringClass));
				proposal.setSignature(getSignature(method));
				MethodBinding original = method.original();
				if(original != method) {
					proposal.setOriginalSignature(getSignature(original));
				}
				proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
				proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
				proposal.setParameterTypeNames(parameterTypeNames);
				proposal.setReturnQualification(method.returnType.qualifiedPackageName());
				proposal.setReturnSimpleName(method.returnType.qualifiedSourceName());
				proposal.setName(method.selector);
				proposal.setCompletion(javadocCompletion);
				proposal.setFlags(method.modifiers);
				int start = (this.assistNodeInJavadoc & CompletionOnJavadoc.REPLACE_TAG) != 0 ? this.javadocTagPosition : this.startPosition;
				proposal.setReplaceRange(start - this.offset, this.endPosition - this.offset);
				proposal.setRelevance(relevance+R_INLINE_TAG);
				if(parameterNames != null) proposal.setParameterNames(parameterNames);
				this.requestor.accept(proposal);
				if(DEBUG) {
					this.printDebug(proposal);
				}
			}
			this.startPosition = previousStartPosition;
		}

		methodsFound.addAll(newMethodsFound);
	}

	private void findLocalMethodsFromFavorites(
			char[] methodName,
			MethodBinding[] methods,
			Scope scope,
			ObjectVector methodsFound,
			ReferenceBinding receiverType,
			InvocationSite invocationSite,
			Scope invocationScope) {

			char[] typeName = CharOperation.concatWith(receiverType.compoundName, '.');

			int methodLength = methodName.length;

			next : for (int f = methods.length; --f >= 0;) {
				MethodBinding method = methods[f];

				if (method.isDefaultAbstract())	continue next;

				if (method.isConstructor()) continue next;

				if (this.options.checkDeprecation &&
						method.isViewedAsDeprecated() &&
						!scope.isDefinedInSameUnit(method.declaringClass))
					continue next;

				if (!method.isStatic()) continue next;

				if (this.options.checkVisibility
					&& !method.canBeSeenBy(receiverType, invocationSite, scope)) continue next;

				if (methodLength > method.selector.length) continue next;

				if (!CharOperation.prefixEquals(methodName, method.selector, false /* ignore case */)
						&& !(this.options.camelCaseMatch && CharOperation.camelCaseMatch(methodName, method.selector))) {
					continue next;
				}

				for (int i = methodsFound.size; --i >= 0;) {
					Object[] other = (Object[]) methodsFound.elementAt(i);
					MethodBinding otherMethod = (MethodBinding) other[0];

					if (method == otherMethod) continue next;

					if (CharOperation.equals(method.selector, otherMethod.selector, true)) {
						if (lookupEnvironment.methodVerifier().doesMethodOverride(otherMethod, method)) {
							continue next;
						}
					}
				}

				boolean proposeStaticImport = !(this.compilerOptions.complianceLevel < ClassFileConstants.JDK1_5) &&
					this.options.suggestStaticImport;

				boolean isAlreadyImported = false;
				if (!proposeStaticImport) {
					if(!this.importCachesInitialized) {
						this.initializeImportCaches();
					}
					for (int j = 0; j < this.importCacheCount; j++) {
						char[][] importName = this.importsCache[j];
						if(CharOperation.equals(receiverType.sourceName, importName[0])) {
							if (!CharOperation.equals(typeName, importName[1])) {
								continue next;
							} else {
								isAlreadyImported = true;
							}
						}
					}
				}

				methodsFound.add(new Object[]{method, receiverType});

				ReferenceBinding superTypeWithSameErasure = (ReferenceBinding)receiverType.findSuperTypeWithSameErasure(method.declaringClass);
				if (method.declaringClass != superTypeWithSameErasure) {
					MethodBinding[] otherMethods = superTypeWithSameErasure.getMethods(method.selector);
					for (int i = 0; i < otherMethods.length; i++) {
						if(otherMethods[i].original() == method.original()) {
							method = otherMethods[i];
						}
					}
				}

				int length = method.parameters.length;
				char[][] parameterTypeNames = new char[length][];

				for (int i = 0; i < length; i++) {
					TypeBinding type = method.original().parameters[i];
					parameterTypeNames[i] = type.qualifiedSourceName();
				}
				char[][] parameterNames = findMethodParameterNames(method,parameterTypeNames);

				char[] completion = CharOperation.NO_CHAR;

				int previousStartPosition = this.startPosition;

				if (this.source != null
					&& this.source.length > this.endPosition
					&& this.source[this.endPosition] == '(') {
					completion = method.selector;
				} else {
					completion = CharOperation.concat(method.selector, new char[] { '(', ')' });
				}

				int relevance = computeBaseRelevance();
				relevance += computeRelevanceForResolution();
				relevance += computeRelevanceForInterestingProposal();
				relevance += computeRelevanceForCaseMatching(methodName, method.selector);
				relevance += computeRelevanceForExpectingType(method.returnType);
				relevance += computeRelevanceForStatic(true, method.isStatic());
				relevance += computeRelevanceForQualification(true);
				relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);

				CompilationUnitDeclaration cu = this.unitScope.referenceContext;
				int importStart = cu.types[0].declarationSourceStart;
				int importEnd = importStart;

				this.noProposal = false;

				if (!proposeStaticImport) {
					if (isAlreadyImported) {
						if (!isIgnored(CompletionProposal.METHOD_REF)) {
							completion = CharOperation.concat(receiverType.sourceName, completion, '.');

							CompletionProposal proposal = this.createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
							proposal.setDeclarationSignature(getSignature(method.declaringClass));
							proposal.setSignature(getSignature(method));
							MethodBinding original = method.original();
							if(original != method) {
								proposal.setOriginalSignature(getSignature(original));
							}
							proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
							proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
							proposal.setParameterTypeNames(parameterTypeNames);
							proposal.setReturnQualification(method.returnType.qualifiedPackageName());
							proposal.setReturnSimpleName(method.returnType.qualifiedSourceName());
							proposal.setName(method.selector);
							proposal.setCompletion(completion);
							proposal.setFlags(method.modifiers);
							proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
							proposal.setRelevance(relevance);
							if(parameterNames != null) proposal.setParameterNames(parameterNames);

							this.requestor.accept(proposal);
							if(DEBUG) {
								this.printDebug(proposal);
							}
						}
					} else if (!this.isIgnored(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_IMPORT)) {
						completion = CharOperation.concat(receiverType.sourceName, completion, '.');

						CompletionProposal proposal = this.createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
						proposal.setDeclarationSignature(getSignature(method.declaringClass));
						proposal.setSignature(getSignature(method));
						MethodBinding original = method.original();
						if(original != method) {
							proposal.setOriginalSignature(getSignature(original));
						}
						proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
						proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
						proposal.setParameterTypeNames(parameterTypeNames);
						proposal.setReturnQualification(method.returnType.qualifiedPackageName());
						proposal.setReturnSimpleName(method.returnType.qualifiedSourceName());
						proposal.setName(method.selector);
						proposal.setCompletion(completion);
						proposal.setFlags(method.modifiers);
						proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
						proposal.setRelevance(relevance);
						if(parameterNames != null) proposal.setParameterNames(parameterNames);

						char[] typeImportCompletion = createImportCharArray(typeName, false, false);

						CompletionProposal typeImportProposal = this.createProposal(CompletionProposal.TYPE_IMPORT, this.actualCompletionPosition);
						typeImportProposal.nameLookup = this.nameEnvironment.nameLookup;
						typeImportProposal.completionEngine = this;
						char[] packageName = receiverType.qualifiedPackageName();
						typeImportProposal.setDeclarationSignature(packageName);
						typeImportProposal.setSignature(getSignature(receiverType));
						typeImportProposal.setReturnQualification(packageName);
						typeImportProposal.setReturnSimpleName(receiverType.qualifiedSourceName());
						typeImportProposal.setCompletion(typeImportCompletion);
						typeImportProposal.setFlags(receiverType.modifiers);
						typeImportProposal.setAdditionalFlags(CompletionFlags.Default);
						typeImportProposal.setReplaceRange(importStart - this.offset, importEnd - this.offset);
						typeImportProposal.setRelevance(relevance);

						proposal.setRequiredProposals(new CompletionProposal[]{typeImportProposal});

						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				} else {
					if (!this.isIgnored(CompletionProposal.METHOD_REF, CompletionProposal.METHOD_IMPORT)) {
						CompletionProposal proposal = this.createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
						proposal.setDeclarationSignature(getSignature(method.declaringClass));
						proposal.setSignature(getSignature(method));
						MethodBinding original = method.original();
						if(original != method) {
							proposal.setOriginalSignature(getSignature(original));
						}
						proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
						proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
						proposal.setParameterTypeNames(parameterTypeNames);
						proposal.setReturnQualification(method.returnType.qualifiedPackageName());
						proposal.setReturnSimpleName(method.returnType.qualifiedSourceName());
						proposal.setName(method.selector);
						proposal.setCompletion(completion);
						proposal.setFlags(method.modifiers);
						proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
						proposal.setRelevance(relevance);
						if(parameterNames != null) proposal.setParameterNames(parameterNames);

						char[] methodImportCompletion = createImportCharArray(CharOperation.concat(typeName, method.selector, '.'), true, false);

						CompletionProposal methodImportProposal = this.createProposal(CompletionProposal.METHOD_IMPORT, this.actualCompletionPosition);
						methodImportProposal.setDeclarationSignature(getSignature(method.declaringClass));
						methodImportProposal.setSignature(getSignature(method));
						if(original != method) {
							proposal.setOriginalSignature(getSignature(original));
						}
						methodImportProposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
						methodImportProposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
						methodImportProposal.setParameterTypeNames(parameterTypeNames);
						methodImportProposal.setReturnQualification(method.returnType.qualifiedPackageName());
						methodImportProposal.setReturnSimpleName(method.returnType.qualifiedSourceName());
						methodImportProposal.setName(method.selector);
						methodImportProposal.setCompletion(methodImportCompletion);
						methodImportProposal.setFlags(method.modifiers);
						methodImportProposal.setReplaceRange(importStart - this.offset, importEnd - this.offset);
						methodImportProposal.setRelevance(relevance);
						if(parameterNames != null) methodImportProposal.setParameterNames(parameterNames);

						proposal.setRequiredProposals(new CompletionProposal[]{methodImportProposal});

						this.requestor.accept(proposal);
						if(DEBUG) {
							this.printDebug(proposal);
						}
					}
				}

				this.startPosition = previousStartPosition;
			}
		}

	private CompletionProposal createRequiredTypeProposal(Binding binding, int start, int end, int relevance) {
		CompletionProposal proposal = null;
		if (binding instanceof ReferenceBinding) {
			ReferenceBinding typeBinding = (ReferenceBinding) binding;

			char[] packageName = typeBinding.qualifiedPackageName();
			char[] typeName = typeBinding.qualifiedSourceName();
			char[] fullyQualifiedName = CharOperation.concat(packageName, typeName, '.');

			proposal = this.createProposal(CompletionProposal.TYPE_REF, this.actualCompletionPosition);
			proposal.nameLookup = this.nameEnvironment.nameLookup;
			proposal.completionEngine = this;
			proposal.setDeclarationSignature(packageName);
			proposal.setSignature(getSignature(typeBinding));
			proposal.setReturnQualification(packageName);
			proposal.setReturnSimpleName(typeName);
			proposal.setCompletion(fullyQualifiedName);
			proposal.setFlags(typeBinding.modifiers);
			proposal.setReplaceRange(start - this.offset, end - this.offset);
			proposal.setRelevance(relevance);
		} else if (binding instanceof PackageBinding) {
			PackageBinding packageBinding = (PackageBinding) binding;

			char[] packageName = CharOperation.concatWith(packageBinding.compoundName, '.');

			proposal = this.createProposal(CompletionProposal.PACKAGE_REF, this.actualCompletionPosition);
			proposal.setDeclarationSignature(packageName);
			proposal.setReturnQualification(packageName);
			proposal.setCompletion(packageName);
			proposal.setReplaceRange(start - this.offset, end - this.offset);
			proposal.setRelevance(relevance);
		}
		return proposal;
	}

	int computeRelevanceForCaseMatching(char[] token, char[] proposalName){
		if (this.options.camelCaseMatch) {
			if(CharOperation.equals(token, proposalName, true /* do not ignore case */)) {
				return R_CASE + R_EXACT_NAME;
			} else if (CharOperation.prefixEquals(token, proposalName, true /* do not ignore case */)) {
				return R_CASE;
			} else if (CharOperation.camelCaseMatch(token, proposalName)){
				return R_CAMEL_CASE;
			} else if(CharOperation.equals(token, proposalName, false /* ignore case */)) {
				return R_EXACT_NAME;
			}
		} else if (CharOperation.prefixEquals(token, proposalName, true /* do not ignore case */)) {
			if(CharOperation.equals(token, proposalName, true /* do not ignore case */)) {
				return R_CASE + R_EXACT_NAME;
			} else {
				return R_CASE;
			}
		} else if(CharOperation.equals(token, proposalName, false /* ignore case */)) {
			return R_EXACT_NAME;
		}
		return 0;
	}
	private int computeRelevanceForClass(){
		if(this.assistNodeIsClass) {
			return R_CLASS;
		}
		return 0;
	}
	private int computeRelevanceForMissingElements(boolean hasProblems) {
		if (!hasProblems) {
			return R_NO_PROBLEMS;
		}
		return 0;
	}
	 int computeRelevanceForQualification(boolean prefixRequired) {
		if(!prefixRequired && !this.insideQualifiedReference) {
			return R_UNQUALIFIED;
		}

		if(prefixRequired && this.insideQualifiedReference) {
			return R_QUALIFIED;
		}
		return 0;
	}
	int computeRelevanceForRestrictions(int accessRuleKind) {
		if(accessRuleKind == IAccessRule.K_ACCESSIBLE) {
			return R_NON_RESTRICTED;
		}
		return 0;
	}
	private int computeRelevanceForStatic(boolean onlyStatic, boolean isStatic) {
		if(this.insideQualifiedReference && !onlyStatic && !isStatic) {
			return R_NON_STATIC;
		}
		return 0;
	}
	private int computeRelevanceForException(){
		if (this.assistNodeIsException) {
			return R_EXCEPTION;
		}
		return 0;
	}
	private int computeRelevanceForException(char[] proposalName){

		if((this.assistNodeIsException || (this.assistNodeInJavadoc & CompletionOnJavadoc.EXCEPTION) != 0 )&&
			(CharOperation.match(EXCEPTION_PATTERN, proposalName, false) ||
			CharOperation.match(ERROR_PATTERN, proposalName, false))) {
			return R_EXCEPTION;
		}
		return 0;
	}
	private int computeRelevanceForExpectingType(TypeBinding proposalType){
		if(this.expectedTypes != null && proposalType != null) {
			for (int i = 0; i <= this.expectedTypesPtr; i++) {
                int relevance = R_EXPECTED_TYPE;
				if(CharOperation.equals(this.expectedTypes[i].qualifiedPackageName(), proposalType.qualifiedPackageName()) &&
					CharOperation.equals(this.expectedTypes[i].qualifiedSourceName(), proposalType.qualifiedSourceName())) {
                    relevance = R_EXACT_EXPECTED_TYPE;
				}
				if((this.expectedTypesFilter & SUBTYPE) != 0
					&& proposalType.isCompatibleWith(this.expectedTypes[i])) {
						return relevance;
				}
				if((this.expectedTypesFilter & SUPERTYPE) != 0
					&& this.expectedTypes[i].isCompatibleWith(proposalType)) {
					return relevance;
				}
			}
		}
		return 0;
	}
	private int computeRelevanceForExpectingType(char[] packageName, char[] typeName){
		if(this.expectedTypes != null) {
			for (int i = 0; i <= this.expectedTypesPtr; i++) {
				if(CharOperation.equals(this.expectedTypes[i].qualifiedPackageName(), packageName) &&
					CharOperation.equals(this.expectedTypes[i].qualifiedSourceName(), typeName)) {
					return R_EXACT_EXPECTED_TYPE;
				}
			}
			if(this.hasJavaLangObjectAsExpectedType) {
				return R_EXPECTED_TYPE;
			}
		}
		return 0;
	}

	private int computeRelevanceForInheritance(ReferenceBinding receiverType, ReferenceBinding declaringClass) {
		if (receiverType == declaringClass) return R_NON_INHERITED;
		return 0;
	}

	int computeRelevanceForInterestingProposal(){
		return computeRelevanceForInterestingProposal(null);
	}
	private int computeRelevanceForInterestingProposal(Binding binding){
		if(this.uninterestingBindings != null) {
			for (int i = 0; i <= this.uninterestingBindingsPtr; i++) {
				if(this.uninterestingBindings[i] == binding) {
					return 0;
				}
			}
		}
		return R_INTERESTING;
	}
	private void computeUninterestingBindings(ASTNode parent, Scope scope){
		if(parent instanceof LocalDeclaration) {
			addUninterestingBindings(((LocalDeclaration)parent).binding);
		} else if (parent instanceof FieldDeclaration) {
			addUninterestingBindings(((FieldDeclaration)parent).binding);
		}
	}

	private void findLabels(char[] label, char[][] choices) {
		if(choices == null || choices.length == 0) return;

		int length = label.length;
		for (int i = 0; i < choices.length; i++) {
			if (length <= choices[i].length
				&& CharOperation.prefixEquals(label, choices[i], false /* ignore case */
			)){
				int relevance = computeBaseRelevance();
				relevance += computeRelevanceForResolution();
				relevance += computeRelevanceForInterestingProposal();
				relevance += computeRelevanceForCaseMatching(label, choices[i]);
				relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for keywors

				this.noProposal = false;
				if(!this.requestor.isIgnored(CompletionProposal.LABEL_REF)) {
					CompletionProposal proposal = this.createProposal(CompletionProposal.LABEL_REF, this.actualCompletionPosition);
					proposal.setName(choices[i]);
					proposal.setCompletion(choices[i]);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setRelevance(relevance);
					this.requestor.accept(proposal);
					if(DEBUG) {
						this.printDebug(proposal);
					}
				}
			}
		}
	}

	// Helper method for findMethods(char[], FunctionBinding[], Scope, ObjectVector, boolean, boolean, boolean, TypeBinding)
	private void findLocalMethodDeclarations(
		char[] methodName,
		MethodBinding[] methods,
		Scope scope,
		ObjectVector methodsFound,
		//	boolean noVoidReturnType, how do you know?
		boolean exactMatch,
		ReferenceBinding receiverType) {

		ObjectVector newMethodsFound =  new ObjectVector();
		// Inherited methods which are hidden by subclasses are filtered out
		// No visibility checks can be performed without the scope & invocationSite
		int methodLength = methodName.length;
		next : for (int f = methods.length; --f >= 0;) {

			MethodBinding method = methods[f];

			if (method.isDefaultAbstract()) continue next;

			if (method.isConstructor()) continue next;

			if (method.isFinal()) {
                newMethodsFound.add(method);
                continue next;
            }

			if (this.options.checkDeprecation &&
					method.isViewedAsDeprecated() &&
					!scope.isDefinedInSameUnit(method.declaringClass))
				continue next;

			//		if (noVoidReturnType && method.returnType == BaseTypes.VoidBinding) continue next;
			if(method.isStatic()) continue next;

			if (!method.canBeSeenBy(receiverType, FakeInvocationSite , scope)) continue next;

			if (exactMatch) {
				if (!CharOperation.equals(methodName, method.selector, false /* ignore case */
					))
					continue next;

			} else {

				if (methodLength > method.selector.length)
					continue next;

				if (!CharOperation.prefixEquals(methodName, method.selector, false/* ignore case */)
						&& !(this.options.camelCaseMatch && CharOperation.camelCaseMatch(methodName, method.selector)))
					continue next;
			}

			for (int i = methodsFound.size; --i >= 0;) {
				MethodBinding otherMethod = (MethodBinding) methodsFound.elementAt(i);
				if (method == otherMethod)
					continue next;

				if (CharOperation.equals(method.selector, otherMethod.selector, true)
						&& lookupEnvironment.methodVerifier().doesMethodOverride(otherMethod, method)) {
					continue next;
				}
			}

			newMethodsFound.add(method);
			
			//propose method
			if(!this.requestor.isIgnored(CompletionProposal.METHOD_DECLARATION)) {
				this.proposeFunction(method);
			}
		}
		methodsFound.addAll(newMethodsFound);
	}

	private void createType(TypeBinding type, StringBuffer completion) {
		if (type.isBaseType()) {
			completion.append(type.sourceName());
		} else if (type.isArrayType()) {
			createType(type.leafComponentType(), completion);
			int dim = type.dimensions();
			for (int i = 0; i < dim; i++) {
				completion.append('[');
				completion.append(']');
			}
		} else {
			char[] packageName = type.qualifiedPackageName();
			char[] typeName = type.qualifiedSourceName();
			if(mustQualifyType(
					packageName,
					type.sourceName(),
					type.isMemberType() ? type.enclosingType().qualifiedSourceName() : null,
					((ReferenceBinding)type).modifiers)) {
				completion.append(CharOperation.concat(packageName, typeName,'.'));
			} else {
				completion.append(type.sourceName());
			}
		}
	}

	private void createVargsType(TypeBinding type, StringBuffer completion) {
		if (type.isArrayType()) {
			createType(type.leafComponentType(), completion);
			int dim = type.dimensions() - 1;
			for (int i = 0; i < dim; i++) {
				completion.append('[');
				completion.append(']');
			}
			completion.append(VARARGS);
		} else {
			createType(type, completion);
		}
	}
	private char[] createImportCharArray(char[] importedElement, boolean isStatic, boolean onDemand) {
		char[] result = IMPORT;
		if (isStatic) {
			result = CharOperation.concat(result, STATIC, ' ');
		}
		result = CharOperation.concat(result, importedElement, ' ');
		if (onDemand) {
			result = CharOperation.concat(result, ON_DEMAND);
		}
		return CharOperation.concat(result, IMPORT_END);
	}
	private void createMethod(MethodBinding method, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames, StringBuffer completion) {
		//// Modifiers
		// flush uninteresting modifiers
		int insertedModifiers = method.modifiers & ~(ClassFileConstants.AccNative | ClassFileConstants.AccAbstract);
		if(insertedModifiers != ClassFileConstants.AccDefault){
			ASTNode.printModifiers(insertedModifiers, completion);
		}

		//// Return type
		createType(method.returnType, completion);
		completion.append(' ');

		//// Selector
		completion.append(method.selector);

		completion.append('(');

		////Parameters
		TypeBinding[] parameterTypes = method.parameters;
		int length = parameterTypes.length;
		for (int i = 0; i < length; i++) {
			if(i != 0) {
				completion.append(',');
				completion.append(' ');
			}
			createType(parameterTypes[i], completion);
			completion.append(' ');
			if(parameterNames != null){
				completion.append(parameterNames[i]);
			} else {
				completion.append('%');
			}
		}

		completion.append(')');
	}

	private boolean isIgnored(int kind, boolean missingTypes) {
		return this.requestor.isIgnored(kind) ||
			(missingTypes && !this.requestor.isAllowingRequiredProposals(kind, CompletionProposal.TYPE_REF));
	}

	private boolean isIgnored(int kind) {
		return this.requestor.isIgnored(kind);
	}

	private boolean isIgnored(int kind, int requiredProposalKind) {
		return this.requestor.isIgnored(kind) ||
			!this.requestor.isAllowingRequiredProposals(kind, requiredProposalKind);
	}

	private void findMethods(
		char[] selector,
		TypeBinding[] typeArgTypes,
		TypeBinding[] argTypes,
		ReferenceBinding receiverType,
		Scope scope,
		ObjectVector methodsFound,
		boolean onlyStaticMethods,
		boolean exactMatch,
		boolean isCompletingDeclaration,
		InvocationSite invocationSite,
		Scope invocationScope,
		boolean implicitCall,
		boolean superCall,
		boolean canBePrefixed,
		Binding[] missingElements,
		int[] missingElementsStarts,
		int[] missingElementsEnds,
		boolean missingElementsHaveProblems) {

		boolean notInJavadoc = this.assistNodeInJavadoc == 0;
		if (selector == null && notInJavadoc) {
			return;
		}

		if(isCompletingDeclaration) {
			MethodBinding[] methods = receiverType.availableMethods();
			if (methods != null){
				for (int i = 0; i < methods.length; i++) {
					if(!methods[i].isDefaultAbstract()) {
						methodsFound.add(methods[i]);
					}
				}
			}
		}

		ReferenceBinding currentType = receiverType;
		if (notInJavadoc) {
			if (isCompletingDeclaration){

				currentType = receiverType.getSuperBinding();
			}
		}
		while (currentType != null) {

			MethodBinding[] methods = currentType.availableMethods();
			if (methods != null) {
				if (isCompletingDeclaration){
					findLocalMethodDeclarations(
						selector,
						methods,
						scope,
						methodsFound,
						exactMatch,
						receiverType);
				} else{
					findLocalMethods(
						selector,
						argTypes,
						methods,
						methods.length,
						scope,
						methodsFound,
						onlyStaticMethods,
						exactMatch,
						receiverType,
						invocationSite,
						invocationScope,
						implicitCall,
						superCall,
						canBePrefixed,
						missingElements,
						missingElementsStarts,
						missingElementsEnds,
						missingElementsHaveProblems);
				}
			}
			
			currentType = currentType.getSuperBinding();
			
		}
	}
	private char[][] findMethodParameterNames(MethodBinding method, char[][] parameterTypeNames){
		TypeBinding erasure =  method.original().declaringClass;
		if(!(erasure instanceof ReferenceBinding)) return null;

		char[][] parameterNames = null;

		int length = parameterTypeNames.length;

		if (length == 0){
			return CharOperation.NO_CHAR_CHAR;
		}
		// look into the corresponding unit if it is available
		if (erasure instanceof SourceTypeBinding){
			SourceTypeBinding sourceType = (SourceTypeBinding) erasure;

			if (sourceType  instanceof CompilationUnitBinding){
				CompilationUnitDeclaration parsedType;

				if ((parsedType = ((CompilationUnitScope)sourceType.scope).referenceContext) != null){
					AbstractMethodDeclaration methodDecl = parsedType.declarationOf(method.original());

					if(methodDecl == null && method.isConstructor()) {
						//if its a constructor we know the return type is the type the method is defined on
						InferredType type = parsedType.findInferredType(method.returnType.qualifiedSourceName());
						if(type != null) {
							InferredMethod infMethod = type.findMethod(method.selector, null);
							
							if(infMethod.getFunctionDeclaration() instanceof AbstractMethodDeclaration) {
								methodDecl = (AbstractMethodDeclaration)infMethod.getFunctionDeclaration();
							}
						}
					}
					
					if (methodDecl != null){
						Argument[] arguments = methodDecl.arguments;
						parameterNames = new char[length][];

						for(int i = 0 ; i < length ; i++){
							parameterNames[i] = arguments[i].name;
						}
					}
				}
			}
			else if (sourceType  instanceof MetatdataTypeBinding){
				MetatdataTypeBinding metatdataTypeBinding=(MetatdataTypeBinding)sourceType;
				ClassData classData = metatdataTypeBinding.getClassData();
				Method meth = classData.getMethod(new String (method.selector));
					if (meth != null){
						int argLength=meth.parameters!=null ? meth.parameters.length : 0;
						parameterNames = new char[argLength][];

						for(int i = 0 ; i < argLength ; i++){
							parameterNames[i] = meth.parameters[i].name.toCharArray();
						}
					}
			}
			else if (sourceType.scope != null) {
				TypeDeclaration parsedType;
				AbstractMethodDeclaration methodDecl = null;
				if ((parsedType = ((ClassScope)sourceType.scope).referenceContext) != null) {
					methodDecl = parsedType.declarationOf(method.original());
				} else if ( ((ClassScope)sourceType.scope).inferredType  != null) {
					methodDecl = (AbstractMethodDeclaration) ((ClassScope)sourceType.scope).inferredType.declarationOf(method.original());
				}

				if (methodDecl != null){
					Argument[] arguments = methodDecl.arguments;
					parameterNames = new char[length][];

					for(int i = 0 ; i < length ; i++){
						parameterNames[i] = arguments[i].name;
					}
				}
			}
		}
		// look into the model
		if(parameterNames == null){

			ReferenceBinding bindingType = (ReferenceBinding)erasure;

			char[] compoundName = CharOperation.concatWith(bindingType.compoundName, '.');
			Object type = this.typeCache.get(compoundName);

			ISourceType sourceType = null;
			if(type != null) {
				if(type instanceof ISourceType) {
					sourceType = (ISourceType) type;
				}
			} else {
				NameEnvironmentAnswer answer = this.nameEnvironment.findType(bindingType.compoundName,this);
				if(answer != null && answer.isSourceType()) {
					sourceType = answer.getSourceTypes()[0];
					this.typeCache.put(compoundName, sourceType);
				}
			}

			if(sourceType != null) {
				IType typeHandle = ((SourceTypeElementInfo) sourceType).getHandle();

				String[] parameterTypeSignatures = new String[length];
				for (int i = 0; i < length; i++) {
					parameterTypeSignatures[i] = Signature.createTypeSignature(parameterTypeNames[i], false);
				}
				IFunction searchedMethod = typeHandle.getFunction(String.valueOf(method.selector), parameterTypeSignatures);
				IFunction[] foundMethods = typeHandle.findMethods(searchedMethod);

				if(foundMethods != null) {
					int len = foundMethods.length;
					if(len == 1) {
						try {
							SourceMethod sourceMethod = (SourceMethod) foundMethods[0];
							parameterNames = ((SourceMethodElementInfo) sourceMethod.getElementInfo()).getArgumentNames();
						} catch (JavaScriptModelException e) {
							// method doesn't exist: ignore
						}
					}
				}
			}
		}
		return parameterNames;
	}

	private void findNestedTypes(
		char[] typeName,
		SourceTypeBinding currentType,
		Scope scope,
		boolean proposeAllMemberTypes,
		ObjectVector typesFound) {
		if (typeName == null)
			return;

		int typeLength = typeName.length;

		SourceTypeBinding nextTypeToIgnore = null;
		while (scope != null) { // done when a COMPILATION_UNIT_SCOPE is found

			switch (scope.kind) {

				case Scope.METHOD_SCOPE :
				case Scope.BLOCK_SCOPE :
					BlockScope blockScope = (BlockScope) scope;

					next : for (int i = 0, length = blockScope.subscopeCount; i < length; i++) {

						if (blockScope.subscopes[i] instanceof ClassScope) {
							SourceTypeBinding localType =
								((ClassScope) blockScope.subscopes[i]).getReferenceBinding();

							if (!localType.isAnonymousType()) {
								if (this.isForbidden(localType))
									continue next;

								if (typeLength > localType.sourceName.length)
									continue next;
								if (!CharOperation.prefixEquals(typeName, localType.sourceName, false/* ignore case */)
										&& !(this.options.camelCaseMatch && CharOperation.camelCaseMatch(typeName, localType.sourceName)))
									continue next;

								for (int j = typesFound.size; --j >= 0;) {
									ReferenceBinding otherType = (ReferenceBinding) typesFound.elementAt(j);

									if (localType == otherType)
										continue next;
								}

								if(this.assistNodeIsClass) {
									if(!localType.isClass()) continue next;
								}

								int relevance = computeBaseRelevance();
								relevance += computeRelevanceForResolution();
								relevance += computeRelevanceForInterestingProposal();
								relevance += computeRelevanceForCaseMatching(typeName, localType.sourceName);
								relevance += computeRelevanceForExpectingType(localType);
								relevance += computeRelevanceForException(localType.sourceName);
								relevance += computeRelevanceForClass();
								relevance += computeRelevanceForQualification(false);
								relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for nested type

								this.noProposal = false;
								if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
									createTypeProposal(localType, localType.sourceName, IAccessRule.K_ACCESSIBLE, localType.sourceName, relevance);
								}
							}
						}
					}
					break;

				case Scope.CLASS_SCOPE :
					SourceTypeBinding enclosingSourceType = scope.enclosingSourceType();
					findMemberTypes(typeName, enclosingSourceType, scope, currentType, false, false, false, false, proposeAllMemberTypes, nextTypeToIgnore, typesFound);
					nextTypeToIgnore = enclosingSourceType;
					if (typeLength == 0)
						return; // do not search outside the class scope if no prefix was provided
					break;

				case Scope.COMPILATION_UNIT_SCOPE :
					return;
			}
			scope = scope.parent;
		}
	}
	
	private void findTypesAndPackages(char[] token, Scope scope, ObjectVector typesFound) {

		if (token == null)
			return;
		
		boolean proposeType =
			!this.requestor.isIgnored(CompletionProposal.TYPE_REF) ||
			((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0);

		boolean proposeAllMemberTypes = !this.assistNodeIsConstructor;
		
		boolean proposeConstructor =
			this.assistNodeIsConstructor &&
			(!isIgnored(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF));
		

		if ((proposeType || proposeConstructor) && scope.enclosingSourceType() != null) {
			findNestedTypes(token, scope.enclosingSourceType(), scope, proposeAllMemberTypes, typesFound);
		}

		boolean isEmptyPrefix = token.length == 0;

		if ((proposeType || proposeConstructor) && this.unitScope != null) {
			
			ReferenceBinding outerInvocationType = scope.enclosingSourceType();
			if(outerInvocationType != null) {
				ReferenceBinding temp = outerInvocationType.enclosingType();
				while(temp != null) {
					outerInvocationType = temp;
					temp = temp.enclosingType();
				}
			}

			int typeLength = token.length;
			SourceTypeBinding[] types = this.unitScope.topLevelTypes;

			next : for (int i = 0, length = types.length; i < length; i++) {
				SourceTypeBinding sourceType = types[i];

				if(isForbidden(sourceType)) continue next;

				//hide anonymous types
				if( sourceType.isAnonymousType() ) {
					continue next;
				}

				if(proposeAllMemberTypes &&
					sourceType != outerInvocationType) {
					findSubMemberTypes(
							token,
							sourceType,
							scope,
							scope.enclosingSourceType(),
							false,
							false,
							false,
							typesFound);
				}

				if (sourceType.sourceName == CompletionParser.FAKE_TYPE_NAME) continue next;
				if (sourceType.sourceName == TypeConstants.PACKAGE_INFO_NAME) continue next;

				if (typeLength > sourceType.sourceName.length) continue next;

				int index = CharOperation.lastIndexOf('.', sourceType.sourceName);
				if (index > 0) {
//					char[] pkg = CharOperation.subarray(sourceType.sourceName, 0, index);
					char[] simpleName = CharOperation.subarray(sourceType.sourceName, index+1, sourceType.sourceName.length);
					
					if (!CharOperation.prefixEquals(token, simpleName, false) && !CharOperation.prefixEquals(token, sourceType.sourceName, false)
							&& !(this.options.camelCaseMatch && CharOperation.camelCaseMatch(token, simpleName))) {
						
						continue;
					}
					
				} else if (!CharOperation.prefixEquals(token, sourceType.sourceName, false)
						&& !(this.options.camelCaseMatch && CharOperation.camelCaseMatch(token, sourceType.sourceName))) {
						
					continue;
				}

				for (int j = typesFound.size; --j >= 0;) {
					ReferenceBinding otherType = (ReferenceBinding) typesFound.elementAt(j);

					if (sourceType == otherType) continue next;
				}

				this.knownTypes.put(CharOperation.concat(sourceType.qualifiedPackageName(), sourceType.sourceName(), '.'), this);

				if(this.assistNodeIsClass) {
					if(!sourceType.isClass()) continue next;
				}

				int relevance = computeBaseRelevance();
				relevance += computeRelevanceForResolution();
				relevance += computeRelevanceForInterestingProposal();
				relevance += computeRelevanceForCaseMatching(token, sourceType.sourceName);
				relevance += computeRelevanceForExpectingType(sourceType);
				relevance += computeRelevanceForQualification(false);
				relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for type in the current unit

				if(sourceType.isClass()){
					relevance += computeRelevanceForClass();
					relevance += computeRelevanceForException(sourceType.sourceName);
				}
				this.noProposal = false;
				if(proposeType && !this.assistNodeIsConstructor) {
					char[] typeName = sourceType.sourceName();
					createTypeProposal(
							sourceType,
							typeName,
							IAccessRule.K_ACCESSIBLE,
							typeName,
							relevance);
				}
				
				if (proposeConstructor) {
					findConstructors(
							sourceType,
							null,
							scope,
							FakeInvocationSite,
							false);
				}
			}
		}

		if (isEmptyPrefix) {
			if (!proposeConstructor) {
				findTypesFromExpectedTypes(token, scope, typesFound, proposeType, proposeConstructor);
			}
		} else {			
			if (proposeConstructor) {
				//search index for constructors that match
				this.nameEnvironment.findConstructorDeclarations(
						token,
						this.options.camelCaseMatch,
						this);
			} else if (proposeType) {
				int searchFor = IJavaScriptSearchConstants.TYPE;
				if(this.assistNodeIsClass) {
					searchFor = IJavaScriptSearchConstants.CLASS;
				}
				
				this.nameEnvironment.findTypes(
						token,
						proposeAllMemberTypes,
						this.options.camelCaseMatch,
						searchFor,
						this);
				acceptTypes(scope);
			}
			if(!isEmptyPrefix && !this.requestor.isIgnored(CompletionProposal.PACKAGE_REF)) {
				this.nameEnvironment.findPackages(token, this);
			}
		}
	}

	private void findTypesAndSubpackages(
		char[] token,
		PackageBinding packageBinding,
		Scope scope) {

		boolean proposeType =
			!this.requestor.isIgnored(CompletionProposal.TYPE_REF) ||
			((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JSDOC_TYPE_REF));

		char[] qualifiedName =
			CharOperation.concatWith(packageBinding.compoundName, token, '.');

		if (token == null || token.length == 0) {
			int length = qualifiedName.length;
			System.arraycopy(
				qualifiedName,
				0,
				qualifiedName = new char[length + 1],
				0,
				length);
			qualifiedName[length] = '.';
		}

		this.qualifiedCompletionToken = qualifiedName;

		if (proposeType && this.unitScope != null) {
			int typeLength = qualifiedName.length;
			SourceTypeBinding[] types = this.unitScope.topLevelTypes;

			for (int i = 0, length = types.length; i < length; i++) {
				SourceTypeBinding sourceType = types[i];

				char[] qualifiedSourceTypeName = CharOperation.concatWith(sourceType.compoundName, '.');

				if (sourceType.sourceName == CompletionParser.FAKE_TYPE_NAME) continue;
				if (sourceType.sourceName == TypeConstants.PACKAGE_INFO_NAME) continue;
				if (typeLength > qualifiedSourceTypeName.length) continue;
				if (!(packageBinding == sourceType.getPackage())) continue;

				if (!CharOperation.prefixEquals(qualifiedName, qualifiedSourceTypeName, false)
						&& !(this.options.camelCaseMatch && CharOperation.camelCaseMatch(token, sourceType.sourceName)))	continue;

				if (this.options.checkDeprecation &&
						sourceType.isViewedAsDeprecated() &&
						!scope.isDefinedInSameUnit(sourceType))
					continue;

				int accessibility = IAccessRule.K_ACCESSIBLE;
				if(sourceType.hasRestrictedAccess()) {
					AccessRestriction accessRestriction = lookupEnvironment.getAccessRestriction(sourceType);
					if(accessRestriction != null) {
						switch (accessRestriction.getProblemId()) {
							case IProblem.ForbiddenReference:
								if (this.options.checkForbiddenReference) {
									continue;
								}
								accessibility = IAccessRule.K_NON_ACCESSIBLE;
								break;
							case IProblem.DiscouragedReference:
								if (this.options.checkDiscouragedReference) {
									continue;
								}
								accessibility = IAccessRule.K_DISCOURAGED;
								break;
						}
					}
				}

				this.knownTypes.put(CharOperation.concat(sourceType.qualifiedPackageName(), sourceType.sourceName(), '.'), this);

				int relevance = computeBaseRelevance();
				relevance += computeRelevanceForResolution();
				relevance += computeRelevanceForInterestingProposal();
				relevance += computeRelevanceForCaseMatching(qualifiedName, qualifiedSourceTypeName);
				relevance += computeRelevanceForExpectingType(sourceType);
				relevance += computeRelevanceForQualification(false);
				relevance += computeRelevanceForRestrictions(accessibility);

				if (sourceType.isClass()) {
					relevance += computeRelevanceForClass();
					relevance += computeRelevanceForException(sourceType.sourceName);
				}
				this.noProposal = false;
				if(proposeType) {
					char[] typeName = sourceType.sourceName();
					createTypeProposal(sourceType, typeName, IAccessRule.K_ACCESSIBLE, typeName, relevance);
				}
			}
		}

		if(proposeType) {
			int searchFor = IJavaScriptSearchConstants.TYPE;
			if(this.assistNodeIsClass) {
				searchFor = IJavaScriptSearchConstants.CLASS;
			}
			this.nameEnvironment.findTypes(
					qualifiedName,
					false,
					this.options.camelCaseMatch,
					searchFor,
					this);
			acceptTypes(scope);
		}
	}

	private void findVariablesAndMethods(
		char[] token,
		Scope scope,
		InvocationSite invocationSite,
		Scope invocationScope,
		boolean insideTypeAnnotation,
		boolean insideAnnotationAttribute) {

		if (token == null) {
			return;
		}

		// Should local variables hide fields from the receiver type or any of its enclosing types?
		// we know its an implicit field/method access... see BlockScope getBinding/getImplicitMethod

		boolean staticsOnly = false;
		// need to know if we're in a static context (or inside a constructor)
		int tokenLength = token.length;

		ObjectVector localsFound = new ObjectVector();
		ObjectVector fieldsFound = new ObjectVector();
		ObjectVector methodsFound = new ObjectVector();

		Scope currentScope = scope;

		//loop up the scopes looking for locally defined variables
		done1 : while (true) { // done when a COMPILATION_UNIT_SCOPE is found
			LocalVariableBinding arguments = null;
			switch (currentScope.kind) {

				case Scope.METHOD_SCOPE :
					// handle the error case inside an explicit constructor call (see MethodScope>>findField)
					MethodScope methodScope = (MethodScope) currentScope;
					staticsOnly |= methodScope.isStatic | methodScope.isConstructorCall;
					arguments = methodScope.argumentsBinding;

				//$FALL-THROUGH$
				case Scope.BLOCK_SCOPE :
				case Scope.COMPILATION_UNIT_SCOPE : {
					BlockScope blockScope = (BlockScope) currentScope;
					LocalVariableBinding[] localBindings = null;
					if(arguments != null) {
						localBindings = new LocalVariableBinding[blockScope.locals.length + 1];
						System.arraycopy(blockScope.locals, 0, localBindings, 1, blockScope.locals.length);
						localBindings[0] = arguments;
					} else {
						localBindings = blockScope.locals;
					}
					
					next : for (int i = 0, length = localBindings.length; i < length; i++) {
						LocalVariableBinding local = localBindings[i];

						if (local == null)
							break next;
						

						if (tokenLength > local.name.length)
							continue next;

						if (!CharOperation.prefixEquals(token, local.name, false /* ignore case */)
								&& !(this.options.camelCaseMatch && CharOperation.camelCaseMatch(token, local.name)))
							continue next;

						if (local.isSecret())
							continue next;

						for (int f = 0; f < localsFound.size; f++) {
							LocalVariableBinding otherLocal =
								(LocalVariableBinding) localsFound.elementAt(f);
							if (CharOperation.equals(otherLocal.name, local.name, true))
								continue next;
						}
						localsFound.add(local);
						
						/* if completion unit then field completion
						 * else local completion
						 */
						int proposalKind;
						if(currentScope.kind == Scope.COMPILATION_UNIT_SCOPE) {
							proposalKind = CompletionProposal.FIELD_REF;
						} else {
							proposalKind = CompletionProposal.LOCAL_VARIABLE_REF;
						}
						
						//if kind is not ingored then make proposal
						if(!this.requestor.isIgnored(proposalKind)) {
							int relevance = computeBaseRelevance();
							relevance += computeRelevanceForResolution();
							relevance += computeRelevanceForInterestingProposal(local);
							relevance += computeRelevanceForCaseMatching(token, local.name);
							relevance += computeRelevanceForExpectingType(local.type);
							relevance += computeRelevanceForQualification(false);
							relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for local variable
							this.noProposal = false;
						
							CompletionProposal proposal = this.createProposal(proposalKind, this.actualCompletionPosition);
							proposal.setSignature(
								local.type == null
								? createTypeSignature(
										CharOperation.NO_CHAR,
										local.declaration.getTypeName().toCharArray())
								: local.type.qualifiedSourceName());
							if(local.type == null) {
								proposal.setReturnSimpleName(local.declaration.getTypeName().toCharArray());
							} else {
								proposal.setReturnQualification(local.type.qualifiedPackageName());
								proposal.setReturnSimpleName(local.type.qualifiedSourceName());
							}
							
							//only in the global scope if variable defined at compilation unit level
							if(currentScope.kind == Scope.COMPILATION_UNIT_SCOPE) {
								proposal.setDeclarationTypeName(IIndexConstants.GLOBAL_SYMBOL);
							}
							proposal.setName(local.name);
							proposal.setCompletion(local.name);
							proposal.setFlags(local.modifiers);
							proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
							proposal.setRelevance(relevance);
							this.requestor.accept(proposal);
							if(DEBUG) {
								this.printDebug(proposal);
							}
						}
					}
					
					if (currentScope.kind==Scope.COMPILATION_UNIT_SCOPE ) {
						break done1;
					} else {
						break;
					}
				}
			}
			currentScope = currentScope.parent;
		}

		boolean proposeField = !this.requestor.isIgnored(CompletionProposal.FIELD_REF);
		boolean proposeMethod = !this.requestor.isIgnored(CompletionProposal.METHOD_REF);

		staticsOnly = false;
		currentScope = scope;

		if(proposeField || proposeMethod) {
			done2 : while (true) { // done when a COMPILATION_UNIT_SCOPE is found

				switch (currentScope.kind) {
					case Scope.METHOD_SCOPE :
						// handle the error case inside an explicit constructor call (see MethodScope>>findField)
						MethodScope methodScope = (MethodScope) currentScope;
						staticsOnly |= methodScope.isStatic | methodScope.isConstructorCall;
						if (proposeMethod && methodScope.numberMethods>0)
						{
							findLocalMethods(
									token,
									null,
									methodScope.methods,
									methodScope.numberMethods,
									currentScope,
									methodsFound,
									false,
									false,
									null,
									invocationSite,
									invocationScope,
									true,
									false,
									true,
									null,
									null,
									null,
									false);
						}
						break;
					case Scope.COMPILATION_UNIT_SCOPE :
						CompilationUnitScope compilationUnitScope = (CompilationUnitScope) currentScope;
						SourceTypeBinding enclosingType = compilationUnitScope.enclosingCompilationUnit();
						if(!insideTypeAnnotation) {
							if(proposeField) {
								findFields(
									token,
									enclosingType,
									compilationUnitScope,
									fieldsFound,
									localsFound,
									staticsOnly,
									invocationSite,
									invocationScope,
									true,
									true,
									null,
									null,
									null,
									false);
							}
							if(proposeMethod && !insideAnnotationAttribute) {
								findMethods(
									token,
									null,
									null,
									enclosingType,
									compilationUnitScope,
									methodsFound,
									staticsOnly,
									false,
									false,
									invocationSite,
									invocationScope,
									true,
									false,
									true,
									null,
									null,
									null,
									false);
							}
						}
						staticsOnly |= enclosingType.isStatic();
						insideTypeAnnotation = false;
						break done2;
				}
				currentScope = currentScope.parent;
			}

			if (this.assistNodeInJavadoc == 0) {
				// search in favorites import
				findFieldsAndMethodsFromFavorites(
						token,
						scope,
						invocationSite,
						invocationScope,
						localsFound,
						fieldsFound,
						methodsFound);
			}
			
			//propose methods from environment if token length is not 0
			
			//String superTypeName = this.unitScope.referenceContext.compilationResult.compilationUnit.getCommonSuperType().getSuperTypeName();
			
			//ArrayList superTypes = this.nameEnvironment.findSuperTypes(superTypeName.toCharArray());
			//char[][] searchInTypes = new char[superTypes.size() + 2][];
			char[][] searchInTypes = new char[1][];
			//searchInTypes[0] = superTypeName.toCharArray();
			searchInTypes[0] = IIndexConstants.GLOBAL_SYMBOL;
//			for(int i = 0; i < superTypes.size(); i++) {
//				searchInTypes[i+2] = (char[]) superTypes.get(i);
//			}
			
			if (proposeMethod) {
				this.nameEnvironment.findFunctions(
						token,
						searchInTypes,
						this.options.camelCaseMatch,
						this);
			}
			
			//propose fields from environment if token length is not 0
			if (proposeField) {
				this.nameEnvironment.findVariables(
						token,
						searchInTypes,
						this.options.camelCaseMatch,
						this);
			}
		}
	}
	private char[][] findVariableFromUnresolvedReference(LocalDeclaration variable, BlockScope scope, final char[][] discouragedNames) {
		final TypeReference type = variable.type;
		if(type != null &&
				type.resolvedType != null &&
				type.resolvedType.problemId() == ProblemReasons.NoError){

			final ArrayList proposedNames = new ArrayList();

			UnresolvedReferenceNameFinder.UnresolvedReferenceNameRequestor nameRequestor =
				new UnresolvedReferenceNameFinder.UnresolvedReferenceNameRequestor() {
					public void acceptName(char[] name) {

						int relevance = computeBaseRelevance();
						relevance += computeRelevanceForInterestingProposal();
						relevance += computeRelevanceForCaseMatching(completionToken, name);
						relevance += R_NAME_FIRST_PREFIX;
						relevance += R_NAME_FIRST_SUFFIX;
						relevance += R_NAME_LESS_NEW_CHARACTERS;
						relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for variable name

						// accept result
						CompletionEngine.this.noProposal = false;
						if(!CompletionEngine.this.requestor.isIgnored(CompletionProposal.VARIABLE_DECLARATION)) {
							CompletionProposal proposal = CompletionEngine.this.createProposal(CompletionProposal.VARIABLE_DECLARATION, CompletionEngine.this.actualCompletionPosition);
							proposal.setSignature(getSignature(type.resolvedType));
							proposal.setReturnQualification(type.resolvedType.qualifiedPackageName());
							proposal.setReturnSimpleName(type.resolvedType.qualifiedSourceName());
							proposal.setName(name);
							proposal.setCompletion(name);
							//proposal.setFlags(Flags.AccDefault);
							proposal.setReplaceRange(CompletionEngine.this.startPosition - CompletionEngine.this.offset, CompletionEngine.this.endPosition - CompletionEngine.this.offset);
							proposal.setRelevance(relevance);
							CompletionEngine.this.requestor.accept(proposal);
							if(DEBUG) {
								CompletionEngine.this.printDebug(proposal);
							}
						}
						proposedNames.add(name);
					}
				};

			ReferenceContext referenceContext = scope.referenceContext();
			if (referenceContext instanceof AbstractMethodDeclaration) {
				AbstractMethodDeclaration md = (AbstractMethodDeclaration)referenceContext;

				UnresolvedReferenceNameFinder nameFinder = new UnresolvedReferenceNameFinder(this);
				nameFinder.find(
						completionToken,
						md,
						variable.declarationSourceEnd + 1,
						discouragedNames,
						nameRequestor);
			} else if (referenceContext instanceof TypeDeclaration) {
				TypeDeclaration typeDeclaration = (TypeDeclaration) referenceContext;
				FieldDeclaration[] fields = typeDeclaration.fields;
				if (fields != null) {
					done : for (int i = 0; i < fields.length; i++) {
						if (fields[i] instanceof Initializer) {
							Initializer initializer = (Initializer) fields[i];
							if (initializer.bodyStart <= variable.sourceStart &&
									variable.sourceStart < initializer.bodyEnd) {
								UnresolvedReferenceNameFinder nameFinder = new UnresolvedReferenceNameFinder(this);
								nameFinder.find(
										completionToken,
										initializer,
										typeDeclaration.scope,
										variable.declarationSourceEnd + 1,
										discouragedNames,
										nameRequestor);
								break done;
							}
						}
					}
				}
			}

			int proposedNamesCount = proposedNames.size();
			if (proposedNamesCount > 0) {
				return (char[][])proposedNames.toArray(new char[proposedNamesCount][]);
			}
		}

		return null;
	}

	/**
	 *  Helper method for private void findVariableNames(char[] name, TypeReference type )
	 * 
	 * @param token
	 * @param qualifiedPackageName
	 * @param qualifiedSourceName
	 * @param sourceName
	 * @param typeBinding
	 * @param discouragedNames
	 * @param forbiddenNames
	 * @param dim
	 * @param kind
	 * @param modifiers
	 */
	private void findVariableName(
		char[] token,
		char[] qualifiedPackageName,
		char[] qualifiedSourceName,
		char[] sourceName,
		final TypeBinding typeBinding,
		char[][] discouragedNames,
		final char[][] forbiddenNames,
		int dim,
		int kind,
		int modifiers){

		if(sourceName == null || sourceName.length == 0)
			return;

		// compute variable name for non base type
		final char[] displayName;
		if (dim > 0){
			int l = qualifiedSourceName.length;
			displayName = new char[l+(2*dim)];
			System.arraycopy(qualifiedSourceName, 0, displayName, 0, l);
			for(int i = 0; i < dim; i++){
				displayName[l+(i*2)] = '[';
				displayName[l+(i*2)+1] = ']';
			}
		} else {
			displayName = qualifiedSourceName;
		}

		final char[] t = token;
		final char[] q = qualifiedPackageName;
		INamingRequestor namingRequestor = new INamingRequestor() {
			public void acceptNameWithPrefixAndSuffix(char[] name, boolean isFirstPrefix, boolean isFirstSuffix, int reusedCharacters) {
				accept(
						name,
						(isFirstPrefix ? R_NAME_FIRST_PREFIX : R_NAME_PREFIX) + (isFirstSuffix ? R_NAME_FIRST_SUFFIX : R_NAME_SUFFIX),
						reusedCharacters);
			}

			public void acceptNameWithPrefix(char[] name, boolean isFirstPrefix, int reusedCharacters) {
				accept(name, isFirstPrefix ? R_NAME_FIRST_PREFIX :  R_NAME_PREFIX, reusedCharacters);
			}

			public void acceptNameWithSuffix(char[] name, boolean isFirstSuffix, int reusedCharacters) {
				accept(name, isFirstSuffix ? R_NAME_FIRST_SUFFIX : R_NAME_SUFFIX, reusedCharacters);
			}

			public void acceptNameWithoutPrefixAndSuffix(char[] name,int reusedCharacters) {
				accept(name, 0, reusedCharacters);
			}
			void accept(char[] name, int prefixAndSuffixRelevance, int reusedCharacters){
				int l = forbiddenNames == null ? 0 : forbiddenNames.length;
				for (int i = 0; i < l; i++) {
					if (CharOperation.equals(forbiddenNames[i], name, false)) return;
				}

				if (CharOperation.prefixEquals(t, name, false)) {
					int relevance = computeBaseRelevance();
					relevance += computeRelevanceForInterestingProposal();
					relevance += computeRelevanceForCaseMatching(t, name);
					relevance += prefixAndSuffixRelevance;
					if(reusedCharacters > 0) relevance += R_NAME_LESS_NEW_CHARACTERS;
					relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for variable name

					// accept result
					CompletionEngine.this.noProposal = false;
					if(!CompletionEngine.this.requestor.isIgnored(CompletionProposal.VARIABLE_DECLARATION)) {
						CompletionProposal proposal = CompletionEngine.this.createProposal(CompletionProposal.VARIABLE_DECLARATION, CompletionEngine.this.actualCompletionPosition);
						proposal.setSignature(getSignature(typeBinding));
						proposal.setReturnQualification(q);
						proposal.setReturnSimpleName(displayName);
						proposal.setName(name);
						proposal.setCompletion(name);
						//proposal.setFlags(Flags.AccDefault);
						proposal.setReplaceRange(CompletionEngine.this.startPosition - CompletionEngine.this.offset, CompletionEngine.this.endPosition - CompletionEngine.this.offset);
						proposal.setRelevance(relevance);
						CompletionEngine.this.requestor.accept(proposal);
						if(DEBUG) {
							CompletionEngine.this.printDebug(proposal);
						}
					}
				}
			}
		};

		switch (kind) {
			case FIELD :
				InternalNamingConventions.suggestFieldNames(
					this.javaProject,
					qualifiedPackageName,
					qualifiedSourceName,
					dim,
					modifiers,
					token,
					discouragedNames,
					namingRequestor);
				break;
			case LOCAL :
				InternalNamingConventions.suggestLocalVariableNames(
					this.javaProject,
					qualifiedPackageName,
					qualifiedSourceName,
					dim,
					token,
					discouragedNames,
					namingRequestor);
				break;
			case ARGUMENT :
				InternalNamingConventions.suggestArgumentNames(
					this.javaProject,
					qualifiedPackageName,
					qualifiedSourceName,
					dim,
					token,
					discouragedNames,
					namingRequestor);
				break;
		}
	}

	private void findVariableNames(char[] name, TypeReference type , char[][] discouragedNames, char[][] forbiddenNames, int kind, int modifiers){

		if(type != null &&
			type.resolvedType != null &&
			type.resolvedType.problemId() == ProblemReasons.NoError){
			TypeBinding tb = type.resolvedType;
			findVariableName(
				name,
				tb.leafComponentType().qualifiedPackageName(),
				tb.leafComponentType().qualifiedSourceName(),
				tb.leafComponentType().sourceName(),
				tb,
				discouragedNames,
				forbiddenNames,
				type.dimensions(),
				kind,
				modifiers);
		}/*	else {
			char[][] typeName = type.getTypeName();
			findVariableName(
				name,
				NoChar,
				CharOperation.concatWith(typeName, '.'),
				typeName[typeName.length - 1],
				excludeNames,
				type.dimensions());
		}*/
	}

	private ImportBinding[] getFavoriteReferenceBindings(Scope scope) {
		if (this.favoriteReferenceBindings != null) return this.favoriteReferenceBindings;

		String[] favoriteReferences = this.requestor.getFavoriteReferences();

		if (favoriteReferences == null || favoriteReferences.length == 0) return null;

		ImportBinding[] resolvedImports = new ImportBinding[favoriteReferences.length];

		int count = 0;
		next : for (int i = 0; i < favoriteReferences.length; i++) {
			String favoriteReference = favoriteReferences[i];

			int length;
			if (favoriteReference == null || (length = favoriteReference.length()) == 0) continue next;

			boolean onDemand = favoriteReference.charAt(length - 1) == '*';

			char[][] compoundName = CharOperation.splitOn('.', favoriteReference.toCharArray());
			if (onDemand) {
				compoundName = CharOperation.subarray(compoundName, 0, compoundName.length - 1);
			}

			// remove duplicate and conflicting
			for (int j = 0; j < count; j++) {
				ImportReference f = resolvedImports[j].reference;

				if (CharOperation.equals(f.tokens, compoundName)) continue next;

				if (!onDemand && ((f.bits & ASTNode.OnDemand) == 0)) {
					if (CharOperation.equals(f.tokens[f.tokens.length - 1], compoundName[compoundName.length - 1]))
						continue next;
				}
			}

			ImportReference importReference =
				new ImportReference(
						compoundName,
						new long[compoundName.length],
						onDemand);

			Binding importBinding = this.unitScope.findImport(compoundName, onDemand);

			if (!importBinding.isValidBinding()) {
				continue next;
			}

			if (importBinding instanceof PackageBinding) {
				continue next;
			}

			resolvedImports[count++] =
				new ImportBinding(compoundName, onDemand, importBinding, importReference);
		}

		if (resolvedImports.length > count)
			System.arraycopy(resolvedImports, 0, resolvedImports = new ImportBinding[count], 0, count);

		return this.favoriteReferenceBindings = resolvedImports;
	}

	public AssistParser getParser() {

		return this.parser;
	}

	protected void reset() {

		super.reset();
		this.knownPkgs = new HashtableOfObject(10);
		this.knownTypes = new HashtableOfObject(10);
	}

	private void setSourceRange(int start, int end) {
		this.setSourceRange(start, end, true);
	}

	private void setSourceRange(int start, int end, boolean emptyTokenAdjstment) {
		this.startPosition = start;
		if(emptyTokenAdjstment) {
			int endOfEmptyToken = ((CompletionScanner)this.parser.scanner).endOfEmptyToken;
			if (end == 0) {
				this.endPosition = 0;
			}
			else
				this.endPosition = endOfEmptyToken > end ? endOfEmptyToken + 1 : end + 1;
		} 
		
		else {
			this.endPosition = end + 1;
		}
	}
	private char[][] computeAlreadyDefinedName(
			BlockScope scope,
			InvocationSite invocationSite) {
		ArrayList result = new ArrayList();

		boolean staticsOnly = false;

		Scope currentScope = scope;

		done1 : while (true) { // done when a COMPILATION_UNIT_SCOPE is found

			switch (currentScope.kind) {

				case Scope.METHOD_SCOPE :
					// handle the error case inside an explicit constructor call (see MethodScope>>findField)
					MethodScope methodScope = (MethodScope) currentScope;
					staticsOnly |= methodScope.isStatic | methodScope.isConstructorCall;

				//$FALL-THROUGH$ - fall through is done on purpose
				case Scope.BLOCK_SCOPE :
					BlockScope blockScope = (BlockScope) currentScope;

					next : for (int i = 0, length = blockScope.locals.length; i < length; i++) {
						LocalVariableBinding local = blockScope.locals[i];

						if (local == null)
							break next;

						if (local.isSecret())
							continue next;

						result.add(local.name);
					}
					break;

				case Scope.CLASS_SCOPE :
					ClassScope classScope = (ClassScope) currentScope;
					SourceTypeBinding enclosingType = classScope.getReferenceBinding();
					computeAlreadyDefinedName(
							enclosingType,
							classScope,
							staticsOnly,
							invocationSite,
							result);
					staticsOnly |= enclosingType.isStatic();
					break;

				case Scope.COMPILATION_UNIT_SCOPE :
					break done1;
			}
			currentScope = currentScope.parent;
		}

		if (result.size() == 0) return CharOperation.NO_CHAR_CHAR;

		return (char[][])result.toArray(new char[result.size()][]);
	}

	private void computeAlreadyDefinedName(
			SourceTypeBinding receiverType,
			ClassScope scope,
			boolean onlyStaticFields,
			InvocationSite invocationSite,
			ArrayList result) {

		ReferenceBinding currentType = receiverType;
		do {
			FieldBinding[] fields = currentType.availableFields();
			if(fields != null && fields.length > 0) {
				computeAlreadyDefinedName(
					fields,
					scope,
					onlyStaticFields,
					receiverType,
					invocationSite,
					result);
			}
			currentType = currentType.getSuperBinding();
		} while ( currentType != null);
	}

	private void computeAlreadyDefinedName(
			FieldBinding[] fields,
			Scope scope,
			boolean onlyStaticFields,
			ReferenceBinding receiverType,
			InvocationSite invocationSite,
			ArrayList result) {

		next : for (int f = fields.length; --f >= 0;) {
			FieldBinding field = fields[f];

			if (onlyStaticFields && !field.isStatic()) continue next;

			if (!field.canBeSeenBy(receiverType, invocationSite, scope)) continue next;

			result.add(field.name);
		}
	}

	int computeBaseRelevance(){
		return R_DEFAULT;
	}
	int computeRelevanceForResolution(){
		return computeRelevanceForResolution(true);
	}
	int computeRelevanceForResolution(boolean isResolved){
		if (isResolved) {
			return R_RESOLVED;
		}
		return 0;
	}
	private void computeExpectedTypes(ASTNode parent, ASTNode node, Scope scope){

		// default filter
		this.expectedTypesFilter = SUBTYPE;
		this.hasJavaLangObjectAsExpectedType = false;

		// find types from parent
		if(parent instanceof AbstractVariableDeclaration) {
			AbstractVariableDeclaration variable = (AbstractVariableDeclaration)parent;
			TypeBinding binding = variable.getTypeBinding();
			if(binding != null) {
				if(!(variable.initialization instanceof ArrayInitializer)) {
					addExpectedType(binding, scope);
				}
			}
		} else if(parent instanceof Assignment) {
			TypeBinding binding = ((Assignment)parent).lhs.resolvedType;
			if(binding != null) {
				addExpectedType(binding, scope);
			}
		} else if(parent instanceof ReturnStatement) {
			if(scope.methodScope().referenceContext instanceof AbstractMethodDeclaration) {
				MethodBinding methodBinding = ((AbstractMethodDeclaration) scope.methodScope().referenceContext).getBinding();
				TypeBinding binding = methodBinding  == null ? null : methodBinding.returnType;
				if(binding != null) {
					addExpectedType(binding, scope);
				}
			}
		} else if(parent instanceof MessageSend) {
			MessageSend messageSend = (MessageSend) parent;

			if(messageSend.actualReceiverType instanceof ReferenceBinding) {
				ReferenceBinding binding = (ReferenceBinding)messageSend.actualReceiverType;
				boolean isStatic = messageSend.receiver!=null && messageSend.receiver.isTypeReference();

				while(binding != null) {
					computeExpectedTypesForMessageSend(
						binding,
						messageSend.selector,
						messageSend.arguments,
						(ReferenceBinding)messageSend.actualReceiverType,
						scope,
						messageSend,
						isStatic);
					binding = binding.getSuperBinding();
				}
			}
		} else if(parent instanceof AllocationExpression) {
			AllocationExpression allocationExpression = (AllocationExpression) parent;

			ReferenceBinding binding =null;
			if (allocationExpression.type!=null)
				binding=(ReferenceBinding)allocationExpression.type.resolvedType;
			else
				if (allocationExpression.member.resolvedType instanceof ReferenceBinding)
					binding=(ReferenceBinding)allocationExpression.member.resolvedType;


			if(binding != null) {
				computeExpectedTypesForAllocationExpression(
					binding,
					allocationExpression.arguments,
					scope,
					allocationExpression);
			}
		} else if(parent instanceof OperatorExpression) {
			int operator = (parent.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT;
			if(parent instanceof ConditionalExpression) {
				// for future use
			} else if(parent instanceof InstanceOfExpression) {
				InstanceOfExpression e = (InstanceOfExpression) parent;
				TypeBinding binding = e.expression.resolvedType;
				if(binding != null){
					addExpectedType(binding, scope);
					this.expectedTypesFilter = SUBTYPE | SUPERTYPE;
				}
			} else if(parent instanceof BinaryExpression) {
				switch(operator) {
					case OperatorIds.PLUS :
						addExpectedType(TypeBinding.ANY, scope);
						addExpectedType(scope.getJavaLangString(), scope);
						break;
					case OperatorIds.AND_AND :
					case OperatorIds.OR_OR :
					case OperatorIds.XOR :
						addExpectedType(TypeBinding.BOOLEAN, scope);
						break;
					default :
						addExpectedType(TypeBinding.ANY, scope);
						break;
				}
			} else if(parent instanceof UnaryExpression) {
				switch(operator) {
					case OperatorIds.NOT :
						addExpectedType(TypeBinding.BOOLEAN, scope);
						break;
					case OperatorIds.TWIDDLE :
						addExpectedType(TypeBinding.SHORT, scope);
						addExpectedType(TypeBinding.INT, scope);
						addExpectedType(TypeBinding.LONG, scope);
						addExpectedType(TypeBinding.CHAR, scope);
						break;
					case OperatorIds.PLUS :
					case OperatorIds.MINUS :
					case OperatorIds.PLUS_PLUS :
					case OperatorIds.MINUS_MINUS :
						addExpectedType(TypeBinding.SHORT, scope);
						addExpectedType(TypeBinding.INT, scope);
						addExpectedType(TypeBinding.LONG, scope);
						addExpectedType(TypeBinding.FLOAT, scope);
						addExpectedType(TypeBinding.DOUBLE, scope);
						addExpectedType(TypeBinding.CHAR, scope);
						break;
				}
			}
		} else if(parent instanceof ArrayReference) {
			addExpectedType(TypeBinding.SHORT, scope);
			addExpectedType(TypeBinding.INT, scope);
			addExpectedType(TypeBinding.LONG, scope);
		} else if (parent instanceof TryStatement) {
			boolean isException = false;
			if (node instanceof CompletionOnSingleTypeReference) {
				isException = ((CompletionOnSingleTypeReference)node).isException();
			} else if (node instanceof CompletionOnQualifiedTypeReference) {
				isException = ((CompletionOnQualifiedTypeReference)node).isException();
			}
			if (isException) {
				ThrownExceptionFinder thrownExceptionFinder = new ThrownExceptionFinder();
				ReferenceBinding[] bindings = thrownExceptionFinder.find((TryStatement) parent, (BlockScope)scope);
				if (bindings != null && bindings.length > 0) {
					for (int i = 0; i < bindings.length; i++) {
						addExpectedType(bindings[i], scope);
					}
					this.expectedTypesFilter = SUPERTYPE;
				}
			}

		// Expected types for javadoc
		}

		if(this.expectedTypesPtr + 1 != this.expectedTypes.length) {
			System.arraycopy(this.expectedTypes, 0, this.expectedTypes = new TypeBinding[this.expectedTypesPtr + 1], 0, this.expectedTypesPtr + 1);
		}
	}

	private void computeExpectedTypesForAllocationExpression(
		ReferenceBinding binding,
		Expression[] arguments,
		Scope scope,
		InvocationSite invocationSite) {

		MethodBinding[] methods = binding.availableMethods();
		nextMethod : for (int i = 0; i < methods.length; i++) {
			MethodBinding method = methods[i];

			if (!method.isConstructor()) continue nextMethod;

			if (this.options.checkVisibility && !method.canBeSeenBy(invocationSite, scope)) continue nextMethod;

			TypeBinding[] parameters = method.parameters;
			if(parameters.length < arguments.length)
				continue nextMethod;

			int length = arguments.length - 1;

			for (int j = 0; j < length; j++) {
				Expression argument = arguments[j];
				TypeBinding argType = argument.resolvedType;
				if(argType != null && !argType.isCompatibleWith(parameters[j]))
					continue nextMethod;
			}

			TypeBinding expectedType = method.parameters[arguments.length - 1];
			if(expectedType != null) {
				addExpectedType(expectedType, scope);
			}
		}
	}

	private void computeExpectedTypesForMessageSend(
		ReferenceBinding binding,
		char[] selector,
		Expression[] arguments,
		ReferenceBinding receiverType,
		Scope scope,
		InvocationSite invocationSite,
		boolean isStatic) {

		MethodBinding[] methods = binding.availableMethods();
		nextMethod : for (int i = 0; i < methods.length; i++) {
			MethodBinding method = methods[i];

			if (method.isDefaultAbstract())	continue nextMethod;

			if (method.isConstructor()) continue nextMethod;

			if (isStatic && !method.isStatic()) continue nextMethod;

			if (this.options.checkVisibility && !method.canBeSeenBy(receiverType, invocationSite, scope)) continue nextMethod;

			if(!CharOperation.equals(method.selector, selector)) continue nextMethod;

			TypeBinding[] parameters = method.parameters;
			if(parameters.length < arguments.length)
				continue nextMethod;

			int length = arguments.length - 1;

			for (int j = 0; j < length; j++) {
				Expression argument = arguments[j];
				TypeBinding argType = argument.resolvedType;
				if(argType != null && !argType.isCompatibleWith(parameters[j]))
					continue nextMethod;
			}

			TypeBinding expectedType = method.parameters[arguments.length - 1];
			if(expectedType != null) {
				addExpectedType(expectedType, scope);
			}
		}
	}
	private void addExpectedType(TypeBinding type, Scope scope){
		if (type == null || !type.isValidBinding()) return;

		int length = this.expectedTypes.length;
		if (++this.expectedTypesPtr >= length)
			System.arraycopy(this.expectedTypes, 0, this.expectedTypes = new TypeBinding[length * 2], 0, length);
		this.expectedTypes[this.expectedTypesPtr] = type;

		if(type == scope.getJavaLangObject()) {
			this.hasJavaLangObjectAsExpectedType = true;
		}
	}
	private void addForbiddenBindings(Binding binding){
		if (binding == null) return;

		int length = this.forbbidenBindings.length;
		if (++this.forbbidenBindingsPtr >= length)
			System.arraycopy(this.forbbidenBindings, 0, this.forbbidenBindings = new Binding[length * 2], 0, length);
		this.forbbidenBindings[this.forbbidenBindingsPtr] = binding;
	}
	private void addUninterestingBindings(Binding binding){
		if (binding == null) return;

		int length = this.uninterestingBindings.length;
		if (++this.uninterestingBindingsPtr >= length)
			System.arraycopy(this.uninterestingBindings, 0, this.uninterestingBindings = new Binding[length * 2], 0, length);
		this.uninterestingBindings[this.uninterestingBindingsPtr] = binding;
	}

	private Scope computeForbiddenBindings(ASTNode astNode, ASTNode astNodeParent, Scope scope) {
		this.forbbidenBindingsFilter = NONE;
		if(scope instanceof ClassScope) {
			TypeDeclaration typeDeclaration = ((ClassScope)scope).referenceContext;
			if(typeDeclaration.superclass == astNode) {
				this.addForbiddenBindings(typeDeclaration.binding);
				return scope.parent;
			}
		} else {
			if (astNodeParent != null && astNodeParent instanceof TryStatement) {
				boolean isException = false;
				if (astNode instanceof CompletionOnSingleTypeReference) {
					isException = ((CompletionOnSingleTypeReference)astNode).isException();
				} else if (astNode instanceof CompletionOnQualifiedTypeReference) {
					isException = ((CompletionOnQualifiedTypeReference)astNode).isException();
				}
				if (isException) {
					Argument[] catchArguments = ((TryStatement) astNodeParent).catchArguments;
					int length = catchArguments == null ? 0 : catchArguments.length;
					for (int i = 0; i < length; i++) {
						TypeBinding caughtException = catchArguments[i].type.resolvedType;
						if (caughtException != null) {
							this.addForbiddenBindings(caughtException);
							this.knownTypes.put(CharOperation.concat(caughtException.qualifiedPackageName(), caughtException.qualifiedSourceName(), '.'), this);
						}
					}
					this.forbbidenBindingsFilter = SUBTYPE;
				}
			}
		}
//		else if(scope instanceof MethodScope) {
//			MethodScope methodScope = (MethodScope) scope;
//			if(methodScope.insideTypeAnnotation) {
//				return methodScope.parent.parent;
//			}
//		}
		return scope;
	}
	private char[] computePrefix(SourceTypeBinding declarationType, SourceTypeBinding invocationType, boolean isStatic){

		StringBuffer completion = new StringBuffer(10);

		if (isStatic) {
			completion.append(declarationType.sourceName());

		} else if (declarationType == invocationType) {
			completion.append(THIS);

		} else {

			if (!declarationType.isNestedType()) {

				completion.append(declarationType.sourceName());
				completion.append('.');
				completion.append(THIS);

			} else if (!declarationType.isAnonymousType()) {

				completion.append(declarationType.sourceName());
				completion.append('.');
				completion.append(THIS);

			}
		}

		return completion.toString().toCharArray();
	}

	private void proposeNewMethod(char[] token, ReferenceBinding reference) {
		if(!this.requestor.isIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION)) {
			int relevance = computeBaseRelevance();
			relevance += computeRelevanceForResolution();
			relevance += computeRelevanceForInterestingProposal();
			relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE); // no access restriction for new method

			CompletionProposal proposal = this.createProposal(CompletionProposal.POTENTIAL_METHOD_DECLARATION, this.actualCompletionPosition);
			proposal.setDeclarationSignature(getSignature(reference));
			proposal.setSignature(
					createMethodSignature(
							CharOperation.NO_CHAR_CHAR,
							CharOperation.NO_CHAR_CHAR,
							CharOperation.NO_CHAR,
							VOID));
			proposal.setDeclarationPackageName(reference.qualifiedPackageName());
			proposal.setDeclarationTypeName(reference.qualifiedSourceName());

			//proposal.setPackageName(null);
			proposal.setReturnSimpleName(VOID);
			proposal.setName(token);
			//proposal.setParameterPackageNames(null);
			//proposal.setParameterTypeNames(null);
			//proposal.setPackageName(null);
			proposal.setCompletion(token);
			proposal.setFlags(Flags.AccPublic);
			proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
			proposal.setRelevance(relevance);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}
	}
	private boolean isForbidden(Binding binding) {
		for (int i = 0; i <= this.forbbidenBindingsPtr; i++) {
			if(this.forbbidenBindings[i] == binding) {
				return true;
			}
			if((this.forbbidenBindingsFilter & SUBTYPE) != 0) {
				if (binding instanceof TypeBinding &&
						this.forbbidenBindings[i] instanceof TypeBinding &&
						((TypeBinding)binding).isCompatibleWith((TypeBinding)this.forbbidenBindings[i])) {
					return true;
				}
			}
		}
		return false;
	}
	private boolean isValidParent(ASTNode parent, ASTNode node, Scope scope){
		return true;
	}

	public static char[] createNonGenericTypeSignature(char[] qualifiedTypeName) {
		return Signature.createCharArrayTypeSignature(qualifiedTypeName, true);
	}
	public static char[] createTypeSignature(char[] qualifiedPackageName, char[] qualifiedTypeName) {
		char[] name = new char[qualifiedTypeName.length];
		System.arraycopy(qualifiedTypeName, 0, name, 0, qualifiedTypeName.length);

		int depth = 0;
		int length = name.length;
		for (int i = length -1; i >= 0; i--) {
			switch (name[i]) {
				case '<':
					depth--;
					break;
				case '>':
					depth++;
					break;
			}
		}
		return Signature.createCharArrayTypeSignature(
				CharOperation.concat(
						qualifiedPackageName,
						name, '.'), true);
	}

	public static char[] createMethodSignature(char[][] parameterPackageNames, char[][] parameterTypeNames, char[] returnPackagename, char[] returnTypeName) {
		char[] returnTypeSignature =
			returnTypeName == null || returnTypeName.length == 0
			? Signature.createCharArrayTypeSignature(VOID, true)
			: Signature.createCharArrayTypeSignature(
					CharOperation.concat(
							returnPackagename,
							returnTypeName, '.'), true);

		return createMethodSignature(
				parameterPackageNames,
				parameterTypeNames,
				returnTypeSignature);
	}

	public static char[] createMethodSignature(char[][] parameterPackageNames, char[][] parameterTypeNames, char[] returnTypeSignature) {
		char[][] parameterTypeSignature = new char[parameterTypeNames.length][];
		for (int i = 0; i < parameterTypeSignature.length; i++) {
			parameterTypeSignature[i] =
				Signature.createCharArrayTypeSignature(
						CharOperation.concat(
								parameterPackageNames[i],
								parameterTypeNames[i], '.'), true);
		}

		return Signature.createMethodSignature(
				parameterTypeSignature,
				returnTypeSignature);
	}

	protected CompletionProposal createProposal(int kind, int completionOffset) {
		CompletionProposal proposal = CompletionProposal.create(kind, completionOffset - this.offset);
		proposal.nameLookup = this.nameEnvironment.nameLookup;
		proposal.completionEngine = this;
		return proposal;
	}

	/*
	 * Create a completion proposal for a type.
	 */
	private void createTypeProposal(char[] packageName, char[] typeName, int modifiers, int accessibility, char[] completionName, int relevance) {

		// Create standard type proposal
		if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF) && (this.assistNodeInJavadoc & CompletionOnJavadoc.ONLY_INLINE_TAG) == 0) {
			CompletionProposal proposal = CompletionProposal.create(CompletionProposal.TYPE_REF, this.actualCompletionPosition - this.offset);
			proposal.nameLookup = this.nameEnvironment.nameLookup;
			proposal.completionEngine = this;
			proposal.setDeclarationSignature(packageName);
			proposal.setSignature(createNonGenericTypeSignature(typeName));
			proposal.setReturnQualification(packageName);
			proposal.setReturnSimpleName(typeName);
			proposal.setCompletion(completionName);
			proposal.setFlags(modifiers);
			proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
			proposal.setRelevance(relevance);
			proposal.setAccessibility(accessibility);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}

		// Create javadoc text proposal if necessary
		if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JSDOC_TYPE_REF)) {
			char[] javadocCompletion= inlineTagCompletion(completionName, JavadocTagConstants.TAG_LINK);
			CompletionProposal proposal = CompletionProposal.create(CompletionProposal.JSDOC_TYPE_REF, this.actualCompletionPosition - this.offset);
			proposal.nameLookup = this.nameEnvironment.nameLookup;
			proposal.completionEngine = this;
			proposal.setDeclarationSignature(packageName);
			proposal.setSignature(createNonGenericTypeSignature(typeName));
			proposal.setReturnQualification(packageName);
			proposal.setReturnSimpleName(typeName);
			proposal.setCompletion(javadocCompletion);
			proposal.setFlags(modifiers);
			int start = (this.assistNodeInJavadoc & CompletionOnJavadoc.REPLACE_TAG) != 0 ? this.javadocTagPosition : this.startPosition;
			proposal.setReplaceRange(start - this.offset, this.endPosition - this.offset);
			proposal.setRelevance(relevance+R_INLINE_TAG);
			proposal.setAccessibility(accessibility);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}
	}

	/*
	 * Create a completion proposal for a member type.
	 */
	private void createTypeProposal(ReferenceBinding refBinding, char[] typeName, int accessibility, char[] completionName, int relevance) {

		// Create standard type proposal
		if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF) && (this.assistNodeInJavadoc & CompletionOnJavadoc.ONLY_INLINE_TAG) == 0) {
			CompletionProposal proposal = CompletionProposal.create(CompletionProposal.TYPE_REF, this.actualCompletionPosition - this.offset);
			proposal.nameLookup = this.nameEnvironment.nameLookup;
			proposal.completionEngine = this;
			proposal.setDeclarationSignature(refBinding.qualifiedPackageName());
			proposal.setSignature(getSignature(refBinding));
			proposal.setReturnQualification(refBinding.qualifiedPackageName());
			proposal.setReturnSimpleName(typeName);
			proposal.setCompletion(completionName);
			proposal.setFlags(refBinding.modifiers);
			proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
			proposal.setRelevance(relevance);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}

		// Create javadoc text proposal if necessary
		if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JSDOC_TYPE_REF)) {
			char[] javadocCompletion= inlineTagCompletion(completionName, JavadocTagConstants.TAG_LINK);
			CompletionProposal proposal = CompletionProposal.create(CompletionProposal.JSDOC_TYPE_REF, this.actualCompletionPosition - this.offset);
			proposal.nameLookup = this.nameEnvironment.nameLookup;
			proposal.completionEngine = this;
			proposal.setDeclarationSignature(refBinding.qualifiedPackageName());
			proposal.setSignature(getSignature(refBinding));
			proposal.setReturnQualification(refBinding.qualifiedPackageName());
			proposal.setReturnSimpleName(typeName);
			proposal.setCompletion(javadocCompletion);
			proposal.setFlags(refBinding.modifiers);
			int start = (this.assistNodeInJavadoc & CompletionOnJavadoc.REPLACE_TAG) != 0 ? this.javadocTagPosition : this.startPosition;
			proposal.setReplaceRange(start - this.offset, this.endPosition - this.offset);
			proposal.setRelevance(relevance+R_INLINE_TAG);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}
	}

	/**
	 * Returns completion string inserted inside a specified inline tag.
	 * @param completionName
	 * @return char[] Completion text inclunding specified inline tag
	 */
	private char[] inlineTagCompletion(char[] completionName, char[] inlineTag) {
		int tagLength= inlineTag.length;
		int completionLength = completionName.length;
		int inlineLength = 2+tagLength+1+completionLength+1;
		char[] inlineCompletion = new char[inlineLength];
		inlineCompletion[0] = '{';
		inlineCompletion[1] = '@';
		System.arraycopy(inlineTag, 0, inlineCompletion, 2, tagLength);
		inlineCompletion[tagLength+2] = ' ';
		System.arraycopy(completionName, 0, inlineCompletion, tagLength+3, completionLength);
		// do not add space at end of inline tag (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=121026)
		//inlineCompletion[inlineLength-2] = ' ';
		inlineCompletion[inlineLength-1] = '}';
		return inlineCompletion;
	}

	protected void printDebug(CategorizedProblem error) {
		if(CompletionEngine.DEBUG) {
			System.out.print("COMPLETION - completionFailure("); //$NON-NLS-1$
			System.out.print(error);
			System.out.println(")"); //$NON-NLS-1$
		}
	}

	private void printDebugTab(int tab, StringBuffer buffer) {
		for (int i = 0; i < tab; i++) {
			buffer.append('\t');
		}
	}

	protected void printDebug(CompletionProposal proposal){
		StringBuffer buffer = new StringBuffer();
		printDebug(proposal, 0, buffer);
		System.out.println(buffer.toString());
	}
	private void printDebug(CompletionProposal proposal, int tab, StringBuffer buffer){
		printDebugTab(tab, buffer);
		buffer.append("COMPLETION - "); //$NON-NLS-1$
		switch(proposal.getKind()) {
			case CompletionProposal.ANONYMOUS_CLASS_DECLARATION :
				buffer.append("ANONYMOUS_CLASS_DECLARATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.FIELD_REF :
				buffer.append("FIELD_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.KEYWORD :
				buffer.append("KEYWORD"); //$NON-NLS-1$
				break;
			case CompletionProposal.LABEL_REF :
				buffer.append("LABEL_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.LOCAL_VARIABLE_REF :
				buffer.append("LOCAL_VARIABLE_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.METHOD_DECLARATION :
				buffer.append("FUNCTION_DECLARATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.METHOD_REF :
				buffer.append("FUNCTION_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.PACKAGE_REF :
				buffer.append("PACKAGE_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.TYPE_REF :
				buffer.append("TYPE_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.VARIABLE_DECLARATION :
				buffer.append("VARIABLE_DECLARATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.POTENTIAL_METHOD_DECLARATION :
				buffer.append("POTENTIAL_METHOD_DECLARATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.METHOD_NAME_REFERENCE :
				buffer.append("METHOD_NAME_REFERENCE"); //$NON-NLS-1$
				break;
			case CompletionProposal.FIELD_IMPORT :
				buffer.append("FIELD_IMPORT"); //$NON-NLS-1$
				break;
			case CompletionProposal.METHOD_IMPORT :
				buffer.append("METHOD_IMPORT"); //$NON-NLS-1$
				break;
			case CompletionProposal.TYPE_IMPORT :
				buffer.append("TYPE_IMPORT"); //$NON-NLS-1$
				break;
			default :
				buffer.append("PROPOSAL"); //$NON-NLS-1$
				break;

		}

		buffer.append("{\n");//$NON-NLS-1$
		printDebugTab(tab, buffer);
		buffer.append("\tCompletion[").append(proposal.getCompletion() == null ? "null".toCharArray() : proposal.getCompletion()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		printDebugTab(tab, buffer);
		buffer.append("\tDeclarationSignature[").append(proposal.getDeclarationSignature() == null ? "null".toCharArray() : proposal.getDeclarationSignature()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		printDebugTab(tab, buffer);
		buffer.append("\tDeclarationKey[").append(proposal.getDeclarationKey() == null ? "null".toCharArray() : proposal.getDeclarationKey()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		printDebugTab(tab, buffer);
		buffer.append("\tSignature[").append(proposal.getSignature() == null ? "null".toCharArray() : proposal.getSignature()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		printDebugTab(tab, buffer);
		buffer.append("\tKey[").append(proposal.getKey() == null ? "null".toCharArray() : proposal.getKey()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		printDebugTab(tab, buffer);
		buffer.append("\tName[").append(proposal.getName() == null ? "null".toCharArray() : proposal.getName()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		printDebugTab(tab, buffer);
		buffer.append("\tFlags[");//$NON-NLS-1$
		int flags = proposal.getFlags();
		buffer.append(Flags.toString(flags));
		buffer.append("]\n"); //$NON-NLS-1$

		CompletionProposal[] proposals = proposal.getRequiredProposals();
		if(proposals != null) {
			printDebugTab(tab, buffer);
			buffer.append("\tRequiredProposals[");//$NON-NLS-1$
			for (int i = 0; i < proposals.length; i++) {
				buffer.append("\n"); //$NON-NLS-1$
				printDebug(proposals[i], tab + 2, buffer);
			}
			printDebugTab(tab, buffer);
			buffer.append("\n\t]\n"); //$NON-NLS-1$
		}

		printDebugTab(tab, buffer);
		buffer.append("\tCompletionLocation[").append(proposal.getCompletionLocation()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$
		int start = proposal.getReplaceStart();
		int end = proposal.getReplaceEnd();
		printDebugTab(tab, buffer);
		buffer.append("\tReplaceStart[").append(start).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("-ReplaceEnd[").append(end).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.source != null) {
			printDebugTab(tab, buffer);
			buffer.append("\tReplacedText[").append(this.source, start, end-start).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		printDebugTab(tab, buffer);
		buffer.append("\tTokenStart[").append(proposal.getTokenStart()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("-TokenEnd[").append(proposal.getTokenEnd()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$
		printDebugTab(tab, buffer);
		buffer.append("\tRelevance[").append(proposal.getRelevance()).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$

		printDebugTab(tab, buffer);
		buffer.append("}\n");//$NON-NLS-1$
	}

	public CompilationUnitDeclaration doParse(ICompilationUnit unit, AccessRestriction accessRestriction) {
		CompilationResult unitResult =
			new CompilationResult(unit, 1, 1, this.compilerOptions.maxProblemsPerUnit);
		try {
			Parser localParser = new Parser(this.problemReporter, this.compilerOptions.parseLiteralExpressionsAsConstants);
			// fix for 309133
			localParser.scanner.taskTags = null;

			CompilationUnitDeclaration parsedUnit = localParser.parse(unit, unitResult);
			localParser.inferTypes(parsedUnit,this.compilerOptions);
			return parsedUnit;
		} catch (AbortCompilationUnit e) {
//			// at this point, currentCompilationUnitResult may not be sourceUnit, but some other
//			// one requested further along to resolve sourceUnit.
//			if (unitResult.compilationUnit == sourceUnit) { // only report once
//				requestor.acceptResult(unitResult.tagAsAccepted());
//			} else {
				throw e; // want to abort enclosing request to compile
//			}
		}

	}
	
	public static char[] getSignature(Binding binding) {
		char[] result = null;
		if ((binding.kind() & Binding.TYPE) != 0 || (binding.kind() & Binding.COMPILATION_UNIT) != 0) {
			TypeBinding typeBinding = (TypeBinding)binding;
			result = typeBinding.signature();
			// walk the supertypes if the type is anonymous to get a useful name
			while (typeBinding != null && typeBinding.isAnonymousType()) {
				// why not just use ReferenceBinding here?
				if (typeBinding instanceof SourceTypeBinding)
					typeBinding = ((SourceTypeBinding) typeBinding).getSuperBinding();
				else if (typeBinding instanceof BinaryTypeBinding)
					typeBinding = ((BinaryTypeBinding) typeBinding).getSuperBinding();
				// must avoid endless loop
				else
					typeBinding = null;
			}
			if (typeBinding != null && typeBinding != binding)
				result = typeBinding.signature();
			
			if (result != null) {
				if ( (binding.kind() & Binding.TYPE) != 0 )
				result = CharOperation.replaceOnCopy(result, '/', '.');
			}
		} else {
			result = Engine.getSignature(binding);
		}
		
		return result;
	}
	
	private char[] computeToken(FieldReference field) {
		char[] currentToken = field.token;
		boolean addDot = false;
		if(currentToken != null && currentToken.length == 0)
			addDot = true;
		if(field.receiver != null) {
			if(field.receiver instanceof SingleNameReference) {
				currentToken = CharOperation.concat(((SingleNameReference)field.receiver).token, currentToken, '.');
			} else if(field.receiver instanceof FieldReference) {
				currentToken = CharOperation.concat(computeToken((FieldReference) field.receiver), currentToken, '.');
			}
		}
		if(addDot)
			currentToken = CharOperation.append(currentToken, '.');
		return currentToken;
	}

	
	private void findTypesFromExpectedTypes(char[] token, Scope scope, ObjectVector typesFound, boolean proposeType, boolean proposeConstructor) {
		if(this.expectedTypesPtr > -1) {
			int typeLength = token == null ? 0 : token.length;
			
			next : for (int i = 0; i <= this.expectedTypesPtr; i++) {
				
				if(this.expectedTypes[i] instanceof ReferenceBinding) {
					ReferenceBinding refBinding = (ReferenceBinding)this.expectedTypes[i];
					
					if (typeLength > 0) {
						if (typeLength > refBinding.sourceName.length) continue next;
	
						if (!CharOperation.prefixEquals(token, refBinding.sourceName, false)
								&& !(this.options.camelCaseMatch && CharOperation.camelCaseMatch(token, refBinding.sourceName))) continue next;
					}

					if (this.options.checkDeprecation &&
							refBinding.isViewedAsDeprecated() &&
							!scope.isDefinedInSameUnit(refBinding))
						continue next;

					int accessibility = IAccessRule.K_ACCESSIBLE;
					if(refBinding.hasRestrictedAccess()) {
						AccessRestriction accessRestriction = this.lookupEnvironment.getAccessRestriction(refBinding);
						if(accessRestriction != null) {
							switch (accessRestriction.getProblemId()) {
								case IProblem.ForbiddenReference:
									if (this.options.checkForbiddenReference) {
										continue next;
									}
									accessibility = IAccessRule.K_NON_ACCESSIBLE;
									break;
								case IProblem.DiscouragedReference:
									if (this.options.checkDiscouragedReference) {
										continue next;
									}
									accessibility = IAccessRule.K_DISCOURAGED;
									break;
							}
						}
					}

					for (int j = 0; j < typesFound.size(); j++) {
						ReferenceBinding typeFound = (ReferenceBinding)typesFound.elementAt(j);
						if (typeFound == refBinding) {
							continue next;
						}
					}
					
					typesFound.add(refBinding);

					boolean inSameUnit = this.unitScope.isDefinedInSameUnit(refBinding);

					// top level types of the current unit are already proposed.
					if(!inSameUnit || (inSameUnit && refBinding.isMemberType())) {
						char[] packageName = refBinding.qualifiedPackageName();
						char[] typeName = refBinding.sourceName();
						char[] completionName = typeName;

						boolean isQualified = false;
						if (!this.insideQualifiedReference && !refBinding.isMemberType()) {
							if (mustQualifyType(packageName, typeName, null, refBinding.modifiers)) {
								if (packageName == null || packageName.length == 0)
									if (this.unitScope != null && this.unitScope.fPackage.compoundName != CharOperation.NO_CHAR_CHAR)
										continue next; // ignore types from the default package from outside it
								completionName = CharOperation.concat(packageName, typeName, '.');
								isQualified = true;
							}
						}

						if(this.assistNodeIsClass) {
							if(!refBinding.isClass()) continue next;
						}

						int relevance = computeBaseRelevance();
						relevance += computeRelevanceForResolution();
						relevance += computeRelevanceForInterestingProposal();
						relevance += computeRelevanceForCaseMatching(token, typeName);
						relevance += computeRelevanceForExpectingType(refBinding);
						relevance += computeRelevanceForQualification(isQualified);
						relevance += computeRelevanceForRestrictions(accessibility);

						if(refBinding.isClass()) {
							relevance += computeRelevanceForClass();
							relevance += computeRelevanceForException(typeName);
						}
						
						if (proposeType && !this.assistNodeIsConstructor) {
							this.noProposal = false;
							if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
								CompletionProposal proposal =  createProposal(CompletionProposal.TYPE_REF, this.actualCompletionPosition);
								proposal.setDeclarationSignature(packageName);
								proposal.setSignature(getSignature(refBinding));
								proposal.setReturnQualification(packageName);
								proposal.setReturnSimpleName(typeName);
								proposal.setCompletion(completionName);
								proposal.setFlags(refBinding.modifiers);
								proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
								proposal.setRelevance(relevance);
								proposal.setAccessibility(accessibility);
								this.requestor.accept(proposal);
								if(DEBUG) {
									this.printDebug(proposal);
								}
							}
						}
						
						if (proposeConstructor) {
							findConstructors(
									refBinding,
									null,
									scope,
									FakeInvocationSite,
									isQualified);
						}
					}
				}
			}
		}
	}
	
	/**
	 * <p>Create a constructor proposal based on the given information.</p>
	 * 
	 * @param typeName Name of the type the constructor is for
	 * @param parameterTypes Type names of the parameters, should be same length as parameterCount
	 * @param parameterNames Type names of the parameters, should be same length as parameterCount
	 * @param modifiers Type names of the parameters, should be same length as parameterCount
	 * @param accessibility Accessibility of the constructor
	 * 
	 * @see Flags
	 * @see IAccessRule
	 */
	private void proposeConstructor(
			char[] typeName,
			char[][] parameterTypes,
			char[][] parameterNames,
			int modifiers,
			int accessibility) {

		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForResolution();
		relevance += computeRelevanceForInterestingProposal();
		relevance += computeRelevanceForRestrictions(accessibility);
		relevance += computeRelevanceForCaseMatching(this.completionToken, typeName);
		relevance += computeRelevanceForClass();
		relevance += computeRelevanceForException(typeName);

		char[] completion;
		if (this.source != null
					&& this.source.length > this.endPosition
					&& this.source[this.endPosition] == '(') {
			completion = CharOperation.NO_CHAR;
		} else {
			completion = new char[] { '(', ')' };
		}
		
		//NOTE: currently all constructors are assumed to be public
		int flags = modifiers;
		flags |= Flags.AccPublic;
		
		switch (parameterNames.length) {
			case -1: {// default constructor
				flags = Flags.AccPublic;
				this.noProposal = false;
				if(!isIgnored(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF)) {
					CompletionProposal proposal =  createProposal(CompletionProposal.CONSTRUCTOR_INVOCATION, this.actualCompletionPosition);
					proposal.setDeclarationSignature(createNonGenericTypeSignature(typeName));
					proposal.setDeclarationTypeName(typeName);
					proposal.setParameterTypeNames(CharOperation.NO_CHAR_CHAR);
					proposal.setParameterNames(CharOperation.NO_CHAR_CHAR);
					proposal.setName(typeName);
					proposal.setIsContructor(true);
					proposal.setCompletion(completion);
					proposal.setFlags(flags);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setRelevance(relevance);
					this.requestor.accept(proposal);
				}
				break;
			}
			case 0: {// constructor with no parameter
				this.noProposal = false;
				if(!isIgnored(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF)) {
					CompletionProposal proposal =  createProposal(CompletionProposal.CONSTRUCTOR_INVOCATION, this.actualCompletionPosition);
					proposal.setDeclarationSignature(createNonGenericTypeSignature(typeName));
					proposal.setDeclarationTypeName(typeName);
					proposal.setParameterTypeNames(CharOperation.NO_CHAR_CHAR);
					proposal.setParameterNames(CharOperation.NO_CHAR_CHAR);
					proposal.setName(typeName);
					proposal.setIsContructor(true);
					proposal.setCompletion(completion);
					proposal.setFlags(flags);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setRelevance(relevance);
					this.requestor.accept(proposal);
					
				}
				break;
			}
			default: {// constructor with parameter
				this.noProposal = false;
				if(!isIgnored(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF)) {
					CompletionProposal proposal =  createProposal(CompletionProposal.CONSTRUCTOR_INVOCATION, this.actualCompletionPosition);
					proposal.setDeclarationSignature(createNonGenericTypeSignature(typeName));
					proposal.setDeclarationTypeName(typeName);
					if(parameterTypes != null) {
						proposal.setParameterTypeNames(parameterTypes);
					}
					proposal.setParameterNames(parameterNames);
					proposal.setName(typeName);
					proposal.setIsContructor(true);
					proposal.setCompletion(completion);
					proposal.setFlags(flags);
					proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
					proposal.setRelevance(relevance);
					
					this.requestor.accept(proposal);
				}
				break;
			}
		}
	}

	private void proposeType(char[] packageName, char[] simpleTypeName, int modifiers, int accessibility, char[] typeName, char[] fullyQualifiedName, boolean isQualified, Scope scope) {
		char[] completionName = fullyQualifiedName;
		if(isQualified) {
			if (packageName == null || packageName.length == 0)
				if (this.unitScope != null && this.unitScope.getDefaultPackage().compoundName != CharOperation.NO_CHAR_CHAR)
					return; // ignore types from the default package from outside it
		} else {
			completionName = simpleTypeName;
		}
	
		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForResolution();
		relevance += computeRelevanceForInterestingProposal();
		relevance += computeRelevanceForRestrictions(accessibility);
		relevance += computeRelevanceForCaseMatching(this.completionToken, simpleTypeName);
		relevance += computeRelevanceForExpectingType(packageName, simpleTypeName);
		relevance += computeRelevanceForQualification(isQualified);
		relevance += computeRelevanceForClass();
		relevance += computeRelevanceForException(simpleTypeName);
		relevance += computeRelevanceForName(simpleTypeName);
		
		
		// put proposals that have '_' at the start of their final segment down the list
		char[] lastSegment = CharOperation.lastSegment(completionName, '.');
		if(CharOperation.indexOf('_', lastSegment) == 0) {
			relevance--;
		}
		this.noProposal = false;
		if(!this.requestor.isIgnored(CompletionProposal.TYPE_REF)) {
			createTypeProposal(packageName, typeName, modifiers, accessibility, completionName, relevance);
		}
	}
	
	/**
	 * <p>Creates a function proposal based on all of the given information</p>
	 * 
	 * @param name
	 * @param parameterQualifications
	 * @param parameterFullyQualifiedTypeNames
	 * @param parameterNames
	 * @param returnQualification
	 * @param returnSimpleName
	 * @param declaringQualification
	 * @param declaringSimpleName
	 * @param modifiers
	 */
	private void proposeFunction(char[] name,
			char[][] parameterFullyQualifiedTypeNames,
			char[][] parameterNames,
			char[] returnQualification,
			char[] returnSimpleName,
			char[] declaringQualification,
			char[] declaringSimpleName,
			int modifiers) {
		
		//compute completion
		char[] completion;
		if (this.source != null
				&& this.source.length > this.endPosition
				&& this.source[this.endPosition] == '(') {
			
			completion = name;
		} else {
			completion = CharOperation.concat(name, new char[] { '(', ')' });
		}
		
		//compute relevance
		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForInterestingProposal();
		if (this.completionToken != null) relevance += computeRelevanceForCaseMatching(this.completionToken, name);
		relevance += computeRelevanceForExpectingType(returnQualification, returnSimpleName);
		relevance += computeRelevanceForQualification(false);
		relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);
		relevance += RelevanceConstants.R_FUNCTION;
		relevance += computeRelevanceForName(name);
		
		this.noProposal = false;
		// create proposal
		CompletionProposal proposal = this.createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
		proposal.setDeclarationSignature(QualificationHelpers.createFullyQualifiedName(declaringQualification, declaringSimpleName));
		proposal.setDeclarationPackageName(declaringQualification);
		proposal.setDeclarationTypeName(declaringSimpleName);
		proposal.setParameterTypeNames(parameterFullyQualifiedTypeNames);
		proposal.setReturnQualification(returnQualification);
		proposal.setReturnSimpleName(returnSimpleName);
		proposal.setName(name);
		proposal.setCompletion(completion);
		proposal.setFlags(modifiers | Flags.AccPublic );
		proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
		proposal.setRelevance(relevance);
		if(parameterNames != null) proposal.setParameterNames(parameterNames);
		proposal.setIsContructor(false);
		this.requestor.accept(proposal);
		if(DEBUG) {
			this.printDebug(proposal);
		}
	}
	
	/**
	 * <p>Proposes a function content assist completion based on a {@link MethodBinding}.</p>
	 * 
	 * @param method {@link MethodBinding} to base a function content assist completion proposal on 
	 */
	private void proposeFunction(MethodBinding method) {
		if(method != null && method.selector != null) {
			//get parameter info
			int parametersLength = method.parameters != null ? method.parameters.length : 0;
			char[][] parameterFullyQualifiedTypeNames = new char[parametersLength][];

			//get parameter types
			for (int i = 0; i < parametersLength; i++) {
				//find first none-anonymous parent type and use that for argument type name
				TypeBinding parameterType = method.original().parameters[i];
				while(parameterType != null && parameterType.isAnonymousType() && parameterType instanceof ReferenceBinding) {
					parameterType = ((ReferenceBinding)parameterType).getSuperBinding();
				}
				
				//if ended up with null type, use original
				if(parameterType == null) {
					parameterType = method.original().parameters[i];
				}
				
				//do not display anonymous types
				if(!parameterType.isAnonymousType()) {
					parameterFullyQualifiedTypeNames[i] = parameterType.qualifiedSourceName();
				}
			}
			
			//get parameter names
			char[][] parameterNames = findMethodParameterNames(method, parameterFullyQualifiedTypeNames);
			
			//get return type info
			char[] returnQualification = null;
			char[] returnSimpleName = null;
			if(method.returnType != null) {
				char[][] seperatedReturnTypeName =
					QualificationHelpers.seperateFullyQualifedName(method.returnType.qualifiedSourceName());
				returnQualification = seperatedReturnTypeName[QualificationHelpers.QULIFIERS_INDEX];
				returnSimpleName = seperatedReturnTypeName[QualificationHelpers.SIMPLE_NAMES_INDEX];
			}
			
			//get declaring type info
			char[] declaringQualification = null;
			char[] declaringSimpleName = null;
			
			/* determine the declaring type name,
			 * local function bindings do not have declaring type unless their declaring class is not a compilation unit
			 * 
			 * IE:
			 * function() {
			 *   foo.bar = function() {};
			 * }*/
			if(method.declaringClass != null &&
						(!(method instanceof LocalFunctionBinding) || !(method.declaringClass instanceof CompilationUnitBinding)) ) {
				
				/* if declaring type is a compilation unit, then use global type
				 * else use function declaring type */
				if(method.declaringClass instanceof CompilationUnitBinding) {
					declaringSimpleName = IIndexConstants.GLOBAL_SYMBOL;
				} else if(method.declaringClass != null) {
					char[][] seperatedDeclaringTypeName =
						QualificationHelpers.seperateFullyQualifedName(method.declaringClass.qualifiedSourceName());
					declaringQualification = seperatedDeclaringTypeName[QualificationHelpers.QULIFIERS_INDEX];
					declaringSimpleName = seperatedDeclaringTypeName[QualificationHelpers.SIMPLE_NAMES_INDEX];
				}
			}
			
			this.proposeFunction(method.selector, parameterFullyQualifiedTypeNames, parameterNames,
					returnQualification, returnSimpleName,
					declaringQualification, declaringSimpleName,
					method.modifiers);
		}
	}
	
	/**
	 * <p>
	 * Creates a field proposal using the given {@link FieldBinding}.
	 * </p>
	 * 
	 * @param field
	 *            create a field proposal from this {@link FieldBinding}
	 */
	private void proposeField(FieldBinding field) {
		this.proposeField(field.name, field.type.qualifiedPackageName(), field.type.qualifiedSourceName(),
					field.declaringClass.qualifiedPackageName(), field.declaringClass.qualifiedSourceName(),
					field.modifiers);
	}
	
	/**
	 * <p>Creates a variable proposal based on all of the given information</p>
	 * 
	 * @param name
	 * @param typeQualification
	 * @param typeSimpleName
	 * @param declaringQualification
	 * @param declaringSimpleName
	 * @param modifiers
	 */
	private void proposeField(char[] name,
			char[] typeQualification,
			char[] typeSimpleName,
			char[] declaringQualification,
			char[] declaringSimpleName,
			int modifiers) {
		
		char[] completion = name;

		int relevance = computeBaseRelevance();
		relevance += computeRelevanceForInterestingProposal();
		if (this.completionToken != null) relevance += computeRelevanceForCaseMatching(this.completionToken, name);
		relevance += computeRelevanceForExpectingType(typeQualification, typeSimpleName);
		relevance += computeRelevanceForQualification(false);
		relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);
		relevance += RelevanceConstants.R_FIELD;
		relevance += computeRelevanceForName(name);

		this.noProposal = false;
		// Standard proposal
		if (!this.requestor.isIgnored(CompletionProposal.FIELD_REF) && (this.assistNodeInJavadoc & CompletionOnJavadoc.ONLY_INLINE_TAG) == 0) {
			CompletionProposal proposal = this.createProposal(CompletionProposal.FIELD_REF, this.actualCompletionPosition);
			proposal.setDeclarationPackageName(declaringQualification);
			proposal.setDeclarationTypeName(declaringSimpleName);
			proposal.setReturnQualification(typeQualification);
			proposal.setReturnSimpleName(typeSimpleName);
			proposal.setName(name);
			proposal.setCompletion(completion);
			proposal.setFlags(modifiers);
			proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
			proposal.setRelevance(relevance);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}
		
		// Javadoc completions
		if ((this.assistNodeInJavadoc & CompletionOnJavadoc.TEXT) != 0 && !this.requestor.isIgnored(CompletionProposal.JSDOC_FIELD_REF)) {
			char[] javadocCompletion = inlineTagCompletion(completion, JavadocTagConstants.TAG_LINK);
			CompletionProposal proposal = this.createProposal(CompletionProposal.JSDOC_FIELD_REF, this.actualCompletionPosition);
			proposal.setDeclarationPackageName(declaringQualification);
			proposal.setDeclarationTypeName(declaringSimpleName);
			proposal.setReturnQualification(typeQualification);
			proposal.setReturnSimpleName(typeSimpleName);
			proposal.setName(name);
			proposal.setCompletion(javadocCompletion);
			proposal.setFlags(modifiers);
			int start = (this.assistNodeInJavadoc & CompletionOnJavadoc.REPLACE_TAG) != 0 ? this.javadocTagPosition : this.startPosition;
			proposal.setReplaceRange(start - this.offset, this.endPosition - this.offset);
			proposal.setRelevance(relevance+R_INLINE_TAG);
			this.requestor.accept(proposal);
			if(DEBUG) {
				this.printDebug(proposal);
			}
		}
	}
	
	/**
	 * <p>
	 * Computes the relevance of a given name. The name could be for a type,
	 * field, or function.
	 * </p>
	 * 
	 * @param name
	 *            compute the relevance of this type, field, or function name
	 * 
	 * @return relevance of the given type, field, or function name
	 */
	private static int computeRelevanceForName(char[] name) {
		int relevance = 0;
		
		//higher relevance if name does not start with an underscore
		if(name != null && name.length > 0 && name[0] != '_') {
			relevance = RelevanceConstants.R_NOT_UNDERSCORE;
		}
		
		return relevance;
	}
	
	/**
	 * <p><b>NOTE:</b> This code is derived from FunctionBinding#areParametersCompatibleWith(TypeBinding[])</p>
	 * 
	 * @param parameters
	 * @param arguments
	 * @param isVarargs
	 * @return
	 */
	private static boolean areParametersCompatibleWith(TypeBinding[] parameters, TypeBinding[] arguments, boolean isVarargs) {
		int paramLength = parameters.length;
		int argLength = arguments.length;
		int lastIndex = argLength;
		if (isVarargs) {
			lastIndex = paramLength - 1;
			if (paramLength == argLength) { // accept X[] but not X or X[][]
				TypeBinding varArgType = parameters[lastIndex]; // is an ArrayBinding by definition
				TypeBinding lastArgument = arguments[lastIndex];
				if (varArgType != lastArgument && !lastArgument.isCompatibleWith(varArgType))
					return false;
			} else if (paramLength < argLength) { // all remainig argument types must be compatible with the elementsType of varArgType
				TypeBinding varArgType = ((ArrayBinding) parameters[lastIndex]).elementsType();
				for (int i = lastIndex; i < argLength; i++)
					if (varArgType != arguments[i] && !arguments[i].isCompatibleWith(varArgType))
						return false;
			} else if (lastIndex != argLength) { // can call foo(int i, X ... x) with foo(1) but NOT foo();
				return false;
			}
			// now compare standard arguments from 0 to lastIndex
		} else {
			if(paramLength != argLength)
				return false;
		}
		for (int i = 0; i < lastIndex; i++)
			if (parameters[i] != arguments[i] && !arguments[i].isCompatibleWith(parameters[i]))
				return false;
		return true;
	}
}
