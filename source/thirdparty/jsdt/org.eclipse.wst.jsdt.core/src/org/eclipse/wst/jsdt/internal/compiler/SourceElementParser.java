/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     bug 242694 -  Michael Spector <spektom@gmail.com>     
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredAttribute;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Assignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.FunctionExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Initializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocFieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocMessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocSingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.NameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteralField;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ThisReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceType;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.compiler.parser.SourceTypeConverter;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfInt;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObjectToInt;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;
import org.eclipse.wst.jsdt.internal.core.util.CommentRecorderParser;

/**
 * A source element parser extracts structural and reference information
 * from a piece of source.
 *
 * also see @ISourceElementRequestor
 *
 * The structural investigation includes:
 * - the package statement
 * - import statements
 * - top-level types: package member, member types (member types of member types...)
 * - fields
 * - methods
 *
 * If reference information is requested, then all source constructs are
 * investigated and type, field & method references are provided as well.
 *
 * Any (parsing) problem encountered is also provided.
 */
public class SourceElementParser extends CommentRecorderParser {

	ISourceElementRequestor requestor;
	ISourceType sourceType;
	boolean reportReferenceInfo;
	char[][] typeNames;
	char[][] superTypeNames;
	int nestedTypeIndex;
	int nestedMethodIndex;
	LocalDeclarationVisitor localDeclarationVisitor = null;
	CompilerOptions options;
	HashtableOfObjectToInt sourceEnds = new HashtableOfObjectToInt();
	HashMap nodesToCategories = new HashMap(); // a map from ASTNode to char[][]
	boolean useSourceJavadocParser = true;
	HashtableOfObject notifiedTypes=new HashtableOfObject();
	
	
	public static final boolean NOTIFY_LOCALS=false;
/**
 * An ast visitor that visits local type declarations.
 */
public class LocalDeclarationVisitor extends ASTVisitor {
	ArrayList declaringTypes;
	public void pushDeclaringType(TypeDeclaration declaringType) {
		if (this.declaringTypes == null) {
			this.declaringTypes = new ArrayList();
		}
		this.declaringTypes.add(declaringType);
	}
	public void popDeclaringType() {
		this.declaringTypes.remove(this.declaringTypes.size()-1);
	}
	public TypeDeclaration peekDeclaringType() {
		if (this.declaringTypes == null) return null;
		int size = this.declaringTypes.size();
		if (size == 0) return null;
		return (TypeDeclaration) this.declaringTypes.get(size-1);
	}
	public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
		notifySourceElementRequestor(typeDeclaration, sourceType == null, peekDeclaringType());
		return false; // don't visit members as this was done during notifySourceElementRequestor(...)
	}
	public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
		notifySourceElementRequestor(typeDeclaration, sourceType == null, peekDeclaringType());
		return false; // don't visit members as this was done during notifySourceElementRequestor(...)
	}

	public boolean visit(MethodDeclaration methodDeclaration, Scope scope) {
		notifySourceElementRequestor(methodDeclaration);
		return false;
	}
}


	/*
	 * Visitor for current context declaration.
	 *
	 * A context is defined by either the top level or a closure (function)
	 */
	protected ASTVisitor contextDeclarationNotifier = new ASTVisitor(){
		
		/**
		 * Key: int - function depth
		 * <br/>
		 * Value: <code>ArrayList&ltString></code> locals defined at key depth
		 */
		private HashtableOfInt locals = new HashtableOfInt();

		public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
			if (NOTIFY_LOCALS || nestedMethodIndex==1) {
				notifySourceElementRequestor( localDeclaration, null );
				
				//visit method declaration on RHS of declaration if at nest level 0
				AbstractMethodDeclaration methodDecl = AbstractMethodDeclaration.findMethodDeclaration(localDeclaration);
				if(methodDecl != null) {
					notifySourceElementRequestor(methodDecl);
				}
			}
			
			//store the local declaration at the current nest level
			storeLocal(localDeclaration.name, nestedMethodIndex);
			
			return true;
		}

		public boolean visit(MethodDeclaration methodDeclaration, Scope scope) {

			/* only non anonymous functions are notified here
			 * other functions will be notified from assignment and local declaration visits
			 */
			char[] name = methodDeclaration.getName();
			if( !methodDeclaration.isConstructor() && !methodDeclaration.isAnonymous() && name != null && name.length > 0 && (nestedMethodIndex == 1 || NOTIFY_LOCALS)) {
				notifySourceElementRequestor( methodDeclaration );
			}
			
			nestedMethodIndex++;
			return true;
		}
		
		//visit closures and add them as a source method in the model
		public boolean visit(MessageSend messageSend, BlockScope scope) {
			if (messageSend.receiver instanceof FunctionExpression) {
				MethodDeclaration methodDecl = ((FunctionExpression) messageSend.receiver).methodDeclaration;
				if (methodDecl != null && nestedMethodIndex == 1) {
					notifySourceElementRequestor(methodDecl);
				}
			}
			return true;
		}

		/**
		 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.Assignment, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
		 */
		public boolean visit(Assignment assignment, BlockScope scope) {
			boolean keepVisiting = true;

			IExpression leftHandSide = assignment.getLeftHandSide();
			
			if(leftHandSide instanceof SingleNameReference && !containsLocal(((SingleNameReference)leftHandSide).token, nestedMethodIndex)) {
				notifySourceElementRequestor(assignment, null);
				
				//visit method declaration as long as local scope does not contain the single name reference
				AbstractMethodDeclaration methodDecl = AbstractMethodDeclaration.findMethodDeclaration(assignment);
				if(methodDecl != null) {
					notifySourceElementRequestor(methodDecl);
				}
			} else {
				/* this is to prevent visiting expressions like:
				 * MyType.prototype.myFunc = ...
				 * They will be visited when visiting the type
				 */
				keepVisiting = false;
			}

			return keepVisiting;
		}
		
		public boolean visit(Argument argument, BlockScope scope) {
			boolean keepVisiting = true;
			storeLocal(argument.name, nestedMethodIndex);
			return keepVisiting;
		}

		/**
		 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteral, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
		 */
		public boolean visit(ObjectLiteral literal, BlockScope scope) {
			//it is considered a nest level when inside an object literal
			nestedMethodIndex++;
			return true;
		}
		
		/**
		 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#endVisit(org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteral, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
		 */
		public void endVisit(ObjectLiteral literal, BlockScope scope) {
			//it is considered a nest level when inside an object literal
			nestedMethodIndex--;
		}
		
		public boolean visit(ObjectLiteralField field, BlockScope scope) {
			//store the local declaration at the current nest level
			if(field.getFieldName() instanceof SingleNameReference)
				storeLocal(((SingleNameReference)field.getFieldName()).token, nestedMethodIndex);
			return true;
		}
		
		public void endVisit(MethodDeclaration methodDeclaration, Scope scope) {
			clearLocals(nestedMethodIndex);
			nestedMethodIndex--;
		}
		
		private void storeLocal(char[] localName, int methodDepth) {
			ArrayList localList = (ArrayList) locals.get(methodDepth);
			if(localList == null) {
				localList = new ArrayList();
				locals.put(methodDepth, localList);
			}
			
			//store as string so that contains works correctly
			String local = new String(localName);
			if(!localList.contains(local))
				localList.add(local);
		}
		
		private boolean containsLocal(char[] localName, int methodDepth) {
			while(methodDepth >= 1) {
				ArrayList localList = (ArrayList) locals.get(methodDepth);
				if(localList == null) {
					methodDepth--;
					continue;
				}

				//list contains strings so that #contains call works as expected
				if(localList.contains(new String(localName)))
					return true;
				methodDepth--;
			}
			return false;
		}
		
		private void clearLocals(int methodDepth) {
			ArrayList localList = (ArrayList) locals.get(methodDepth);
			if(localList != null) {
				localList.clear();
			}
		}

	};

public SourceElementParser(
		final ISourceElementRequestor requestor,
		IProblemFactory problemFactory,
		CompilerOptions options,
		boolean reportLocalDeclarations,
		boolean optimizeStringLiterals) {
	this(requestor, problemFactory, options, reportLocalDeclarations, optimizeStringLiterals, true/* use SourceJavadocParser */);
}

public SourceElementParser(
		ISourceElementRequestor requestor,
		IProblemFactory problemFactory,
		CompilerOptions options,
		boolean reportLocalDeclarations,
		boolean optimizeStringLiterals,
		boolean useSourceJavadocParser) {

	super(
		new ProblemReporter(
			DefaultErrorHandlingPolicies.exitAfterAllProblems(),
			options,
			problemFactory),
		optimizeStringLiterals);

	// we want to notify all syntax error with the acceptProblem API
	// To do so, we define the record method of the ProblemReporter
	this.problemReporter = new ProblemReporter(
		DefaultErrorHandlingPolicies.exitAfterAllProblems(),
		options,
		problemFactory) {
		public void record(CategorizedProblem problem, CompilationResult unitResult, ReferenceContext context) {
			unitResult.record(problem, context); // TODO (jerome) clients are trapping problems either through factory or requestor... is result storing needed?
			SourceElementParser.this.requestor.acceptProblem(problem);
		}
	};
	this.requestor = requestor;
	typeNames = new char[4][];
	superTypeNames = new char[4][];
	nestedTypeIndex = 0;
	this.options = options;
	if (reportLocalDeclarations) {
		this.localDeclarationVisitor = new LocalDeclarationVisitor();
	}
	// set specific javadoc parser
	this.useSourceJavadocParser = useSourceJavadocParser;
	if (useSourceJavadocParser) {
		this.javadocParser = new SourceJavadocParser(this);
	}
	
	// must start at 1 because HashtableOfInt can not contain 0
	this.nestedMethodIndex = 1;
}

public void setRequestor(ISourceElementRequestor requestor) {
	this.requestor = requestor;
	notifiedTypes.clear();
}

private void acceptJavadocTypeReference(Expression expression) {
	if (expression instanceof JavadocSingleTypeReference) {
		JavadocSingleTypeReference singleRef = (JavadocSingleTypeReference) expression;
		this.requestor.acceptTypeReference(singleRef.token, singleRef.sourceStart);
	} else if (expression instanceof JavadocQualifiedTypeReference) {
		JavadocQualifiedTypeReference qualifiedRef = (JavadocQualifiedTypeReference) expression;
		this.requestor.acceptTypeReference(qualifiedRef.tokens, qualifiedRef.sourceStart, qualifiedRef.sourceEnd);
	}
}
public void addUnknownRef(NameReference nameRef) {
	// Note that:
	// - the only requestor interested in references is the SourceIndexerRequestor
	// - a name reference can become a type reference only during the cast case, it is then tagged later with the Binding.TYPE bit
	// However since the indexer doesn't make the distinction between name reference and type reference, there is no need
	// to report a type reference in the SourceElementParser.
	// This gained 3.7% in the indexing performance test.
	if (nameRef instanceof SingleNameReference) {
		requestor.acceptUnknownReference(((SingleNameReference) nameRef).token, nameRef.sourceStart);
	} else {
		//QualifiedNameReference
		requestor.acceptUnknownReference(((QualifiedNameReference) nameRef).tokens, nameRef.sourceStart, nameRef.sourceEnd);
	}
}
public void checkComment() {
	// discard obsolete comments while inside methods or fields initializer (see bug 74369)
	// don't discard if the expression being worked on is an ObjectLiteral (see bug 322412 )
	if (!(this.diet && this.dietInt == 0) && this.scanner.commentPtr >= 0 && !(expressionPtr >= 0 && expressionStack[expressionPtr] instanceof ObjectLiteral)) {
		flushCommentsDefinedPriorTo(this.endStatementPosition);
	}

	int lastComment = this.scanner.commentPtr;

	if (this.modifiersSourceStart >= 0) {
		// eliminate comments located after modifierSourceStart if positionned
		while (lastComment >= 0 && Math.abs(this.scanner.commentStarts[lastComment]) > this.modifiersSourceStart) lastComment--;
	}
	if (lastComment >= 0) {
		// consider all remaining leading comments to be part of current declaration
		this.modifiersSourceStart = Math.abs(this.scanner.commentStarts[0]);

		// check deprecation in last comment if javadoc (can be followed by non-javadoc comments which are simply ignored)
		while (lastComment >= 0 && this.scanner.commentStops[lastComment] < 0) lastComment--; // non javadoc comment have negative end positions
		if (lastComment >= 0 && this.javadocParser != null) {
			int commentEnd = this.scanner.commentStops[lastComment] - 1; //stop is one over,
			// do not report problem before last parsed comment while recovering code...
			this.javadocParser.reportProblems = this.currentElement == null || commentEnd > this.lastJavadocEnd;
			if (this.javadocParser.checkDeprecation(lastComment)) {
				checkAndSetModifiers(ClassFileConstants.AccDeprecated);
			}
			this.javadoc = this.javadocParser.docComment;	// null if check javadoc is not activated
			if (currentElement == null) this.lastJavadocEnd = commentEnd;
		}
	}

	if (this.reportReferenceInfo && this.javadocParser.checkDocComment && this.javadoc != null) {
		// Report reference info in javadoc comment @throws/@exception tags
		TypeReference[] thrownExceptions = this.javadoc.exceptionReferences;
		if (thrownExceptions != null) {
			for (int i = 0, max=thrownExceptions.length; i < max; i++) {
				TypeReference typeRef = thrownExceptions[i];
				if (typeRef instanceof JavadocSingleTypeReference) {
					JavadocSingleTypeReference singleRef = (JavadocSingleTypeReference) typeRef;
					this.requestor.acceptTypeReference(singleRef.token, singleRef.sourceStart);
				} else if (typeRef instanceof JavadocQualifiedTypeReference) {
					JavadocQualifiedTypeReference qualifiedRef = (JavadocQualifiedTypeReference) typeRef;
					this.requestor.acceptTypeReference(qualifiedRef.tokens, qualifiedRef.sourceStart, qualifiedRef.sourceEnd);
				}
			}
		}

		// Report reference info in javadoc comment @see tags
		Expression[] references = this.javadoc.seeReferences;
		if (references != null) {
			for (int i = 0, max=references.length; i < max; i++) {
				Expression reference = references[i];
				acceptJavadocTypeReference(reference);
				if (reference instanceof JavadocFieldReference) {
					JavadocFieldReference fieldRef = (JavadocFieldReference) reference;
					this.requestor.acceptFieldReference(fieldRef.token, fieldRef.sourceStart);
					if (fieldRef.receiver != null && !fieldRef.receiver.isThis()) {
						acceptJavadocTypeReference(fieldRef.receiver);
					}
				} else if (reference instanceof JavadocMessageSend) {
					JavadocMessageSend messageSend = (JavadocMessageSend) reference;
					int argCount = messageSend.arguments == null ? 0 : messageSend.arguments.length;
					this.requestor.acceptMethodReference(messageSend.selector, messageSend.sourceStart);
					this.requestor.acceptConstructorReference(messageSend.selector, argCount, messageSend.sourceStart);
					if (messageSend.receiver != null && !messageSend.receiver.isThis()) {
						acceptJavadocTypeReference(messageSend.receiver);
					}
				} else if (reference instanceof JavadocAllocationExpression) {
					JavadocAllocationExpression constructor = (JavadocAllocationExpression) reference;
					int argCount = constructor.arguments == null ? 0 : constructor.arguments.length;
					if (constructor.type != null) {
						char[][] compoundName = constructor.type.getTypeName();
						this.requestor.acceptConstructorReference(compoundName[compoundName.length-1], argCount, constructor.sourceStart);
						if (!constructor.type.isThis()) {
							acceptJavadocTypeReference(constructor.type);
						}
					}
				}
			}
		}
	}
}
protected void classInstanceCreation(boolean alwaysQualified, boolean isShort) {

	boolean previousFlag = reportReferenceInfo;
	reportReferenceInfo = false; // not to see the type reference reported in super call to getTypeReference(...)
	super.classInstanceCreation(alwaysQualified, isShort);
	reportReferenceInfo = previousFlag;
	if (reportReferenceInfo){
		AllocationExpression alloc = (AllocationExpression)expressionStack[expressionPtr];
//		TypeReference typeRef = alloc.type;
		char [] name={};
		if (alloc.member !=null)
		{
			name=Util.getTypeName(alloc.member);
		}
		else if (alloc.type!=null)
			name= CharOperation.concatWith(alloc.type.getTypeName(), '.');

		if (name!=null && name.length>0)
			requestor.acceptConstructorReference(name,
//			typeRef instanceof SingleTypeReference
//				? ((SingleTypeReference) typeRef).token
//				: CharOperation.concatWith(alloc.type.getParameterizedTypeName(), '.'),
			alloc.arguments == null ? 0 : alloc.arguments.length,
			alloc.sourceStart);
	}
}

protected void consumeExitVariableWithInitialization() {
	// ExitVariableWithInitialization ::= $empty
	// the scanner is located after the comma or the semi-colon.
	// we want to include the comma or the semi-colon
	super.consumeExitVariableWithInitialization();
	if ((currentToken == TokenNameCOMMA || currentToken == TokenNameSEMICOLON)
			&& this.astStack[this.astPtr] instanceof FieldDeclaration) {
		this.sourceEnds.put(this.astStack[this.astPtr], this.scanner.currentPosition - 1);
		rememberCategories();
	}
}
protected void consumeExitVariableWithoutInitialization() {
	// ExitVariableWithoutInitialization ::= $empty
	// do nothing by default
	super.consumeExitVariableWithoutInitialization();
	if ((currentToken == TokenNameCOMMA || currentToken == TokenNameSEMICOLON)
			&& astStack[astPtr] instanceof FieldDeclaration) {
		this.sourceEnds.put(this.astStack[this.astPtr], this.scanner.currentPosition - 1);
		rememberCategories();
	}
}
protected void consumeCallExpressionWithSimpleName() {
	super.consumeCallExpressionWithSimpleName();
	FieldReference fr = (FieldReference) expressionStack[expressionPtr];
	if (reportReferenceInfo) {
		requestor.acceptFieldReference(fr.token, fr.sourceStart);
	}

}
protected void consumeMemberExpressionWithSimpleName() {
	super.consumeMemberExpressionWithSimpleName();
	FieldReference fr = (FieldReference) expressionStack[expressionPtr];
	if (reportReferenceInfo) {
		requestor.acceptFieldReference(fr.token, fr.sourceStart);
	}

}
protected void consumeFormalParameter(boolean isVarArgs) {
	super.consumeFormalParameter(isVarArgs);

	// Flush comments prior to this formal parameter so the declarationSourceStart of the following parameter
	// is correctly set (see bug 80904)
	// Note that this could be done in the Parser itself, but this would slow down all parsers, when they don't need
	// the declarationSourceStart to be set
	flushCommentsDefinedPriorTo(this.scanner.currentPosition);
}
protected void consumeMethodHeaderName(boolean isAnonymousMethod) {
	long selectorSourcePositions = (isAnonymousMethod) ? this.lParenPos
			:this.identifierPositionStack[this.identifierPtr];
	int selectorSourceEnd = (int) selectorSourcePositions;
	int currentAstPtr = this.astPtr;
	super.consumeMethodHeaderName(isAnonymousMethod);
	if (this.astPtr > currentAstPtr) { // if ast node was pushed on the ast stack
		this.sourceEnds.put(this.astStack[this.astPtr], selectorSourceEnd);
		rememberCategories();
	}
}
protected void consumeCallExpressionWithArguments() {
	super.consumeCallExpressionWithArguments();
	MessageSend messageSend = (MessageSend) expressionStack[expressionPtr];
	Expression[] args = messageSend.arguments;
	if (reportReferenceInfo) {
		requestor.acceptMethodReference(
			messageSend.selector,
			(int)(messageSend.nameSourcePosition >>> 32));
	}
}
public MethodDeclaration convertToMethodDeclaration(ConstructorDeclaration c, CompilationResult compilationResult) {
	MethodDeclaration methodDeclaration = super.convertToMethodDeclaration(c, compilationResult);
	int selectorSourceEnd = this.sourceEnds.removeKey(c);
	if (selectorSourceEnd != -1)
		this.sourceEnds.put(methodDeclaration, selectorSourceEnd);
	char[][] categories =  (char[][]) this.nodesToCategories.remove(c);
	if (categories != null)
		this.nodesToCategories.put(methodDeclaration, categories);

	return methodDeclaration;
}
protected CompilationUnitDeclaration endParse(int act) {
	if (compilationUnit != null) {
		CompilationUnitDeclaration result = super.endParse(act);
		return result;
	} else {
		return null;
	}
}
public TypeReference getTypeReference(int dim) {
	/* build a Reference on a variable that may be qualified or not
	 * This variable is a type reference and dim will be its dimensions
	 */
	int length = identifierLengthStack[identifierLengthPtr--];
	if (length < 0) { //flag for precompiled type reference on base types
		TypeReference ref = TypeReference.baseTypeReference(-length, dim);
		ref.sourceStart = intStack[intPtr--];
		if (dim == 0) {
			ref.sourceEnd = intStack[intPtr--];
		} else {
			intPtr--; // no need to use this position as it is an array
			ref.sourceEnd = endPosition;
		}
		if (reportReferenceInfo){
				requestor.acceptTypeReference(ref.getTypeName(), ref.sourceStart, ref.sourceEnd);
		}
		return ref;
	} else {
		int numberOfIdentifiers = this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr--];
		if (length != numberOfIdentifiers || this.genericsLengthStack[this.genericsLengthPtr] != 0) {
			// generic type
			TypeReference ref = null;
			return ref;
		} else if (length == 1) {
			// single variable reference
			this.genericsLengthPtr--; // pop the 0
			if (dim == 0) {
				SingleTypeReference ref =
					new SingleTypeReference(
						identifierStack[identifierPtr],
						identifierPositionStack[identifierPtr--]);
				if (reportReferenceInfo) {
					requestor.acceptTypeReference(ref.token, ref.sourceStart);
				}
				return ref;
			} else {
				ArrayTypeReference ref =
					new ArrayTypeReference(
						identifierStack[identifierPtr],
						dim,
						identifierPositionStack[identifierPtr--]);
				ref.sourceEnd = endPosition;
				if (reportReferenceInfo) {
					requestor.acceptTypeReference(ref.token, ref.sourceStart);
				}
				return ref;
			}
		} else {//Qualified variable reference
			this.genericsLengthPtr--;
			char[][] tokens = new char[length][];
			identifierPtr -= length;
			long[] positions = new long[length];
			System.arraycopy(identifierStack, identifierPtr + 1, tokens, 0, length);
			System.arraycopy(
				identifierPositionStack,
				identifierPtr + 1,
				positions,
				0,
				length);
			if (dim == 0) {
				QualifiedTypeReference ref = new QualifiedTypeReference(tokens, positions);
				if (reportReferenceInfo) {
					requestor.acceptTypeReference(ref.tokens, ref.sourceStart, ref.sourceEnd);
				}
				return ref;
			} else {
				ArrayQualifiedTypeReference ref =
					new ArrayQualifiedTypeReference(tokens, dim, positions);
				ref.sourceEnd = endPosition;
				if (reportReferenceInfo) {
					requestor.acceptTypeReference(ref.tokens, ref.sourceStart, ref.sourceEnd);
				}
				return ref;
			}
		}
	}
}
public NameReference getUnspecifiedReference() {
	/* build a (unspecified) NameReference which may be qualified*/

	int length;
	if ((length = identifierLengthStack[identifierLengthPtr--]) == 1) {
		// single variable reference
		SingleNameReference ref =
			newSingleNameReference(
				identifierStack[identifierPtr],
				identifierPositionStack[identifierPtr--]);
		if (reportReferenceInfo) {
			this.addUnknownRef(ref);
		}
		return ref;
	} else {
		//Qualified variable reference
		char[][] tokens = new char[length][];
		identifierPtr -= length;
		System.arraycopy(identifierStack, identifierPtr + 1, tokens, 0, length);
		long[] positions = new long[length];
		System.arraycopy(identifierPositionStack, identifierPtr + 1, positions, 0, length);
		QualifiedNameReference ref =
			newQualifiedNameReference(
				tokens,
				positions,
				(int) (identifierPositionStack[identifierPtr + 1] >> 32), // sourceStart
				(int) identifierPositionStack[identifierPtr + length]); // sourceEnd
		if (reportReferenceInfo) {
			this.addUnknownRef(ref);
		}
		return ref;
	}
}
public NameReference getUnspecifiedReferenceOptimized() {
	/* build a (unspecified) NameReference which may be qualified
	The optimization occurs for qualified reference while we are
	certain in this case the last item of the qualified name is
	a field access. This optimization is IMPORTANT while it results
	that when a NameReference is build, the type checker should always
	look for that it is not a type reference */

	int length;
	if ((length = identifierLengthStack[identifierLengthPtr--]) == 1) {
		// single variable reference
		SingleNameReference ref =
			newSingleNameReference(
				identifierStack[identifierPtr],
				identifierPositionStack[identifierPtr--]);
		ref.bits &= ~ASTNode.RestrictiveFlagMASK;
		ref.bits |= Binding.LOCAL | Binding.FIELD;
		if (reportReferenceInfo) {
			this.addUnknownRef(ref);
		}
		return ref;
	}

	//Qualified-variable-reference
	//In fact it is variable-reference DOT field-ref , but it would result in a type
	//conflict tha can be only reduce by making a superclass (or inetrface ) between
	//nameReference and FiledReference or putting FieldReference under NameReference
	//or else..........This optimisation is not really relevant so just leave as it is

	char[][] tokens = new char[length][];
	identifierPtr -= length;
	System.arraycopy(identifierStack, identifierPtr + 1, tokens, 0, length);
	long[] positions = new long[length];
	System.arraycopy(identifierPositionStack, identifierPtr + 1, positions, 0, length);
	QualifiedNameReference ref =
		newQualifiedNameReference(
			tokens,
			positions,
			(int) (identifierPositionStack[identifierPtr + 1] >> 32),
	// sourceStart
	 (int) identifierPositionStack[identifierPtr + length]); // sourceEnd
	ref.bits &= ~ASTNode.RestrictiveFlagMASK;
	ref.bits |= Binding.LOCAL | Binding.FIELD;
	if (reportReferenceInfo) {
		this.addUnknownRef(ref);
	}
	return ref;
}

protected ImportReference newImportReference(char[][] tokens, long[] positions, boolean onDemand) {
	return new ImportReference(tokens, positions, onDemand);
}
protected QualifiedNameReference newQualifiedNameReference(char[][] tokens, long[] positions, int sourceStart, int sourceEnd) {
	return new QualifiedNameReference(tokens, positions, sourceStart, sourceEnd);
}
protected SingleNameReference newSingleNameReference(char[] source, long positions) {
	return new SingleNameReference(source, positions);
}



/*
 * Update the bodyStart of the corresponding parse node
 */
public void notifySourceElementRequestor(CompilationUnitDeclaration parsedUnit) {
	if (parsedUnit == null) {
		// when we parse a single type member declaration the compilation unit is null, but we still
		// want to be able to notify the requestor on the created ast node
		if (astStack[0] instanceof AbstractMethodDeclaration) {
			notifySourceElementRequestor((AbstractMethodDeclaration) astStack[0]);
			return;
		}
		return;
	}

	inferTypes(parsedUnit,this.options);

	// range check
	boolean isInRange =
				scanner.initialPosition <= parsedUnit.sourceStart
				&& scanner.eofPosition >= parsedUnit.sourceEnd;

	// collect the top level ast nodes
	if (sourceType == null){
		if (isInRange) {
			requestor.enterCompilationUnit();
		}
	}

	//visit each statement to notify context declarations
	if( parsedUnit.statements != null ){

		for( int i=0; i<parsedUnit.statements.length; i++ ){
			parsedUnit.statements[i].traverse( contextDeclarationNotifier, parsedUnit.scope );
		}

	}

	for (int inx=0;inx<parsedUnit.numberInferredTypes;inx++) {
			InferredType type = parsedUnit.inferredTypes[inx];

		notifySourceElementRequestor(type);
	}

	if (sourceType == null){
		if (isInRange) {
			requestor.exitCompilationUnit(parsedUnit.sourceEnd);
		}
	}
}

public void notifySourceElementRequestor( InferredType type ) {
	//do not notify if type is not a definition or not global
	if ( !type.isDefinition() || !type.isIndexed()) {
		return;
	}
	
	// prevent possible recursion
	if (notifiedTypes.containsKey(type.getName())) {
		return;
	}
	
	//notify the requester of the type
	notifiedTypes.put(type.getName(), null);
	ISourceElementRequestor.TypeInfo typeInfo = new ISourceElementRequestor.TypeInfo();
	typeInfo.declarationStart = type.sourceStart;
	typeInfo.modifiers = type.getModifiers();

	typeInfo.name = type.getName();

	typeInfo.nameSourceStart = type.getNameStart();
	if(type.isObjectLiteral) {
		typeInfo.nameSourceEnd = type.sourceEnd;
	} else {
		typeInfo.nameSourceEnd = typeInfo.nameSourceStart+typeInfo.name.length-1;
	}
	typeInfo.superclass = type.getSuperClassName();
	typeInfo.secondary = false;

	typeInfo.anonymousMember = type.isAnonymous;
	typeInfo.isIndexed = type.isIndexed();
	
	//add synonym info
	InferredType[] synonyms = type.getSynonyms();
	if(synonyms != null) {
		char[][] synonymsNames = new char[synonyms.length][];
		for(int i = 0; i < synonyms.length; ++i) {
			synonymsNames[i] = synonyms[i].getName();
		}
		typeInfo.synonyms = synonymsNames;
	}
	
	requestor.enterType(typeInfo);
	
	//notify the requester of attributes on the type
	for (int attributeInx=0; attributeInx<type.numberAttributes; attributeInx++) {
		InferredAttribute field = type.attributes[attributeInx];
		ISourceElementRequestor.FieldInfo fieldInfo = new ISourceElementRequestor.FieldInfo();
		fieldInfo.declarationStart = field.sourceStart();
		fieldInfo.name = field.name;
		fieldInfo.modifiers = field.modifiers;

		if (field.isStatic)
			fieldInfo.modifiers |= ClassFileConstants.AccStatic;
		fieldInfo.nameSourceStart = field.nameStart;
		fieldInfo.nameSourceEnd = field.nameStart+field.name.length-1;

		fieldInfo.type = field.type!=null ? field.type.getName():null;
		fieldInfo.declaringType = type.getName();
		requestor.enterField(fieldInfo);

		int initializationStart=field.initializationStart;
		requestor.exitField(initializationStart,field.sourceEnd(),field.sourceEnd());
	}

	//notify the requester of functions on the type
	if (type.methods!=null) {
		for (Iterator iterator = type.methods.iterator(); iterator.hasNext();) {
			InferredMethod method = (InferredMethod) iterator.next();
	
			ISourceElementRequestor.MethodInfo methodInfo = new ISourceElementRequestor.MethodInfo();
			methodInfo.isConstructor = method.isConstructor;
			MethodDeclaration methodDeclaration=(MethodDeclaration)method.getFunctionDeclaration();
	
			char[][] argumentTypes = null;
			char[][] argumentNames = null;
			Argument[] arguments = methodDeclaration.arguments;
			if (arguments != null) {
				int argumentLength = arguments.length;
				argumentTypes = new char[argumentLength][];
				argumentNames = new char[argumentLength][];
				for (int i = 0; i < argumentLength; i++) {
					if (arguments[i].type!=null) {
						argumentTypes[i] = CharOperation.concatWith(arguments[i].type.getTypeName(), '.');
					} else if(arguments[i].inferredType != null) {
						/* find first not anonymous parent type and use that for argument type name
						 * 
						 * This is done because if the argument is assigned to inside the function
						 * then an anonymous child type of the original argument type is created,
						 * but that anonymous child type is not the type that should be displayed
						 * to the user, the original argument type is.  #isAnonymous is used rather
						 * then #isIndexed because their are anonymous types that are indexed and the
						 * type of the argument should never be anonymous. */
						InferredType argumentType = arguments[i].inferredType;
						while(argumentType != null && argumentType.isAnonymous) {
							argumentType = argumentType.getSuperType();
						}
						
						//if ended up with null type, use original
						if(argumentType == null) {
							argumentType = arguments[i].inferredType;
						}
						
						argumentTypes[i] = argumentType.getName();
					}
					argumentNames[i] = arguments[i].name;
				}
			}
			methodInfo.declarationStart = methodDeclaration.declarationSourceStart;
			methodInfo.modifiers = methodDeclaration.modifiers;
			if (method.isStatic) {
				methodInfo.modifiers |= ClassFileConstants.AccStatic;
			}
			methodInfo.returnType = methodDeclaration.inferredType == null ?
					null : methodDeclaration.inferredType.getName();
			methodInfo.name =method.name;
			methodInfo.nameSourceStart = method.nameStart;
			methodInfo.nameSourceEnd = method.nameStart+method.name.length-1;
			methodInfo.parameterTypes = argumentTypes;
			methodInfo.parameterNames = argumentNames;
			methodInfo.categories = (char[][]) this.nodesToCategories.get(methodDeclaration);
			InferredMethod inferredMeth = methodDeclaration.getInferredMethod();
			if (inferredMeth != null) {
				InferredType declaringType = inferredMeth.inType;
				if (declaringType != null) {
					if (declaringType.isAnonymous) {
						methodInfo.declaringType = type.getName();
					} else {
						methodInfo.declaringType = declaringType.getName();
					}
				}
			}
			
			//enter either constructor or method where appropriate
			if(methodInfo.isConstructor) {
				requestor.enterConstructor(methodInfo);
			} else {
				requestor.enterMethod(methodInfo);
			}
			
			//visitIfNeeded( (MethodDeclaration)method.getFunctionDeclaration() );
	
			requestor.exitMethod(methodDeclaration.declarationSourceEnd, -1, -1);
		}
	}

	requestor.exitType(type.sourceEnd);
}

/**
 * <p>Notifies the requester of a method declaration using the {@link AbstractMethodDeclaration#selector} as the 
 * selector to notify with.</p>
 * 
 * @param methodDeclaration to notify the requester of
 * 
 * @see #notifySourceElementRequestor(AbstractMethodDeclaration, char[])
 */
public void notifySourceElementRequestor(AbstractMethodDeclaration methodDeclaration) {
	this.notifySourceElementRequestor(methodDeclaration, methodDeclaration.getName());
}

/**
 * <p>Notifies the requester of a method declaration using the given selector rather then the selector set
 * on the declaration itself.</p>
 * 
 * <p>This is useful when the selector on the declaration is not set but it can be pre-determined some other way.</p>
 * 
 * @param methodDeclaration to notify the requester of
 * @param selector to use when notifying the requester of the given <code>methodDeclaration</code>
 */
public void notifySourceElementRequestor(AbstractMethodDeclaration methodDeclaration, char[] selector) {

	this.nestedMethodIndex++;
	// range check
	boolean isInRange =
				scanner.initialPosition <= methodDeclaration.declarationSourceStart
				&& scanner.eofPosition >= methodDeclaration.declarationSourceEnd;

	if (methodDeclaration.isClinit()) {
		this.visitIfNeeded(methodDeclaration);
		this.nestedMethodIndex--;
		return;
	}

	if (methodDeclaration.isDefaultConstructor()) {
		if (reportReferenceInfo) {
			ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) methodDeclaration;
			ExplicitConstructorCall constructorCall = constructorDeclaration.constructorCall;
			if (constructorCall != null) {
				switch(constructorCall.accessMode) {
					case ExplicitConstructorCall.This :
						requestor.acceptConstructorReference(
							typeNames[nestedTypeIndex-1],
							constructorCall.arguments == null ? 0 : constructorCall.arguments.length,
							constructorCall.sourceStart);
						break;
					case ExplicitConstructorCall.ImplicitSuper :
						requestor.acceptConstructorReference(
							superTypeNames[nestedTypeIndex-1],
							constructorCall.arguments == null ? 0 : constructorCall.arguments.length,
							constructorCall.sourceStart);
						break;
				}
			}
		}
		this.nestedMethodIndex--;
		return;
	}
	char[][] argumentTypes = null;
	char[][] argumentNames = null;
	boolean isVarArgs = false;
	Argument[] arguments = methodDeclaration.arguments;
	if (arguments != null) {
		int argumentLength = arguments.length;
		argumentTypes = new char[argumentLength][];
		argumentNames = new char[argumentLength][];
		for (int i = 0; i < argumentLength; i++) {
			if (arguments[i].type!=null) {
				argumentTypes[i] = CharOperation.concatWith(arguments[i].type.getTypeName(), '.');
			} else if(arguments[i].inferredType != null) {
				/* find first not anonymous parent type and use that for argument type name
				 * 
				 * This is done because if the argument is assigned to inside the function
				 * then an anonymous child type of the original argument type is created,
				 * but that anonymous child type is not the type that should be displayed
				 * to the user, the original argument type is.  #isAnonymous is used rather
				 * then #isIndexed because their are anonymous types that are indexed and the
				 * type of the argument should never be anonymous. */
				InferredType argumentType = arguments[i].inferredType;
				while(argumentType != null && argumentType.isAnonymous) {
					argumentType = argumentType.getSuperType();
				}
				
				//if ended up with null type, use original
				if(argumentType == null) {
					argumentType = arguments[i].inferredType;
				}
				
				argumentTypes[i] = argumentType.getName();
			}
			argumentNames[i] = arguments[i].name;
		}
		isVarArgs = arguments[argumentLength-1].isVarArgs();
	}
	// by default no selector end position
	int selectorSourceEnd = -1;
	if (methodDeclaration.isConstructor()) {
		selectorSourceEnd = this.sourceEnds.get(methodDeclaration);
		if (isInRange){
			int currentModifiers = methodDeclaration.modifiers;
			if (isVarArgs)
				currentModifiers |= ClassFileConstants.AccVarargs;

			// remember deprecation so as to not lose it below
			boolean deprecated = (currentModifiers & ClassFileConstants.AccDeprecated) != 0;

			ISourceElementRequestor.MethodInfo methodInfo = new ISourceElementRequestor.MethodInfo();
			methodInfo.isConstructor = true;
			methodInfo.declarationStart = methodDeclaration.declarationSourceStart;
			methodInfo.modifiers = deprecated ? (currentModifiers & ExtraCompilerModifiers.AccJustFlag) | ClassFileConstants.AccDeprecated : currentModifiers & ExtraCompilerModifiers.AccJustFlag;
			methodInfo.name = selector;
			methodInfo.nameSourceStart = methodDeclaration.sourceStart;
			methodInfo.nameSourceEnd = selectorSourceEnd;
			methodInfo.parameterTypes = argumentTypes;
			methodInfo.parameterNames = argumentNames;
			methodInfo.categories = (char[][]) this.nodesToCategories.get(methodDeclaration);
			InferredMethod inferredMeth = methodDeclaration.getInferredMethod();
			if(inferredMeth != null) {
				InferredType declaringType = inferredMeth.inType;
				if(declaringType != null) {
					methodInfo.declaringType = declaringType.getName();
				}
			}
			requestor.enterConstructor(methodInfo);
		}
		/* need this check because a constructor could have been made a constructor after the
		 * method declaration was created, and thus it is not a ConstructorDeclaration
		 */
		if (reportReferenceInfo && methodDeclaration instanceof ConstructorDeclaration) {
			ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) methodDeclaration;
			ExplicitConstructorCall constructorCall = constructorDeclaration.constructorCall;
			if (constructorCall != null) {
				switch(constructorCall.accessMode) {
					case ExplicitConstructorCall.This :
						requestor.acceptConstructorReference(
							typeNames[nestedTypeIndex-1],
							constructorCall.arguments == null ? 0 : constructorCall.arguments.length,
							constructorCall.sourceStart);
						break;
					case ExplicitConstructorCall.ImplicitSuper :
						requestor.acceptConstructorReference(
							superTypeNames[nestedTypeIndex-1],
							constructorCall.arguments == null ? 0 : constructorCall.arguments.length,
							constructorCall.sourceStart);
						break;
				}
			}
		}
		this.visitIfNeeded(methodDeclaration);
		if (isInRange){
			requestor.exitConstructor(methodDeclaration.declarationSourceEnd);
		}
		this.nestedMethodIndex--;
		return;
	}
	selectorSourceEnd = this.sourceEnds.get(methodDeclaration);
	if (isInRange) {
		int currentModifiers = methodDeclaration.modifiers;
		if (isVarArgs)
			currentModifiers |= ClassFileConstants.AccVarargs;

		// remember deprecation so as to not lose it below
		boolean deprecated = (currentModifiers & ClassFileConstants.AccDeprecated) != 0;

		InferredType returnType = methodDeclaration instanceof MethodDeclaration
			? ((MethodDeclaration) methodDeclaration).inferredType
			: null;
		ISourceElementRequestor.MethodInfo methodInfo = new ISourceElementRequestor.MethodInfo();
		methodInfo.declarationStart = methodDeclaration.declarationSourceStart;
		methodInfo.modifiers = deprecated ? (currentModifiers & ExtraCompilerModifiers.AccJustFlag) | ClassFileConstants.AccDeprecated : currentModifiers & ExtraCompilerModifiers.AccJustFlag;
		methodInfo.returnType = returnType == null ? null : returnType.getName();
		methodInfo.name = selector;
		methodInfo.nameSourceStart = methodDeclaration.sourceStart;
		methodInfo.nameSourceEnd = selector != null? methodInfo.nameSourceStart + selector.length - 1 : selectorSourceEnd;
		methodInfo.parameterTypes = argumentTypes;
		methodInfo.parameterNames = argumentNames;
		methodInfo.categories = (char[][]) this.nodesToCategories.get(methodDeclaration);
		InferredMethod inferredMeth = methodDeclaration.getInferredMethod();
		if(inferredMeth != null) {
			InferredType declaringType = inferredMeth.inType;
			if(declaringType != null) {
				methodInfo.declaringType = declaringType.getName();
			}
		}
		requestor.enterMethod(methodInfo);
	}

	this.visitIfNeeded(methodDeclaration);

	if (isInRange) {
		requestor.exitMethod(methodDeclaration.declarationSourceEnd, -1, -1);
	}
	this.nestedMethodIndex--;
}

/*
* Update the bodyStart of the corresponding parse node
*/
public void notifySourceElementRequestor(AbstractVariableDeclaration fieldDeclaration, TypeDeclaration declaringType) {

	// range check
	boolean isInRange =
				scanner.initialPosition <= fieldDeclaration.declarationSourceStart
				&& scanner.eofPosition >= fieldDeclaration.declarationSourceEnd;

	switch(fieldDeclaration.getKind()) {
		case AbstractVariableDeclaration.FIELD:
		case AbstractVariableDeclaration.LOCAL_VARIABLE:
			int fieldEndPosition = this.sourceEnds.get(fieldDeclaration);
			if (fieldEndPosition == -1) {
				// use the declaration source end by default
				fieldEndPosition = fieldDeclaration.declarationSourceEnd;
			}
			
			if (isInRange) {
				int currentModifiers = fieldDeclaration.modifiers;

				// remember deprecation so as to not lose it below
				boolean deprecated = (currentModifiers & ClassFileConstants.AccDeprecated) != 0;
				
				ISourceElementRequestor.FieldInfo fieldInfo = new ISourceElementRequestor.FieldInfo();
				fieldInfo.declarationStart = fieldDeclaration.declarationSourceStart;
				fieldInfo.name = fieldDeclaration.name;
				fieldInfo.modifiers = deprecated ? (currentModifiers & ExtraCompilerModifiers.AccJustFlag)
						| ClassFileConstants.AccDeprecated
						: currentModifiers & ExtraCompilerModifiers.AccJustFlag;
				fieldInfo.type = fieldDeclaration.inferredType != null ? fieldDeclaration.inferredType
						.getName()
						: null;
				fieldInfo.nameSourceStart = fieldDeclaration.sourceStart;
				fieldInfo.nameSourceEnd = fieldDeclaration.sourceEnd;
				fieldInfo.categories = (char[][]) this.nodesToCategories
						.get(fieldDeclaration);
				requestor.enterField(fieldInfo);
				//If this field is of an anonymous type, need to notify so that it shows as a child
				if (fieldDeclaration.inferredType != null
						&& fieldDeclaration.inferredType.isAnonymous) {
					notifySourceElementRequestor(fieldDeclaration.inferredType);
				}
			}
			this.visitIfNeeded(fieldDeclaration, declaringType);
			if (isInRange){
				requestor.exitField(
						// filter out initializations that are not a constant (simple check)
						(fieldDeclaration.initialization == null
								|| fieldDeclaration.initialization instanceof ArrayInitializer
								|| fieldDeclaration.initialization instanceof AllocationExpression
								|| fieldDeclaration.initialization instanceof ArrayAllocationExpression
								|| fieldDeclaration.initialization instanceof Assignment
								|| fieldDeclaration.initialization instanceof ClassLiteralAccess
								|| fieldDeclaration.initialization instanceof MessageSend
								|| fieldDeclaration.initialization instanceof ArrayReference
								|| fieldDeclaration.initialization instanceof ThisReference) ?
							-1 :
							fieldDeclaration.initialization.sourceStart,
						fieldEndPosition,
						fieldDeclaration.declarationSourceEnd);
			}
			break;
		case AbstractVariableDeclaration.INITIALIZER:
			if (isInRange){
				requestor.enterInitializer(
					fieldDeclaration.declarationSourceStart,
					fieldDeclaration.modifiers);
			}
			this.visitIfNeeded((Initializer)fieldDeclaration);
			if (isInRange){
				requestor.exitInitializer(fieldDeclaration.declarationSourceEnd);
			}
			break;
	}
}

/*
* Update the bodyStart of the corresponding parse node
*/
public void notifySourceElementRequestor(Assignment assignment, TypeDeclaration declaringType) {

	// range check
	boolean isInRange =
				scanner.initialPosition <= assignment.sourceStart
				&& scanner.eofPosition >= assignment.sourceEnd;

	int fieldEndPosition = this.sourceEnds.get(assignment);
	if (fieldEndPosition == -1) {
		// use the declaration source end by default
		fieldEndPosition = assignment.sourceEnd;
	}

	if (isInRange) {
		if(assignment.getLeftHandSide() instanceof SingleNameReference) {
			SingleNameReference lhs = (SingleNameReference) assignment.getLeftHandSide();
			ISourceElementRequestor.FieldInfo fieldInfo = new ISourceElementRequestor.FieldInfo();
			fieldInfo.declarationStart = assignment.sourceStart;
			fieldInfo.name = lhs.getToken();
			fieldInfo.type = assignment.inferredType != null ? assignment.inferredType
					.getName()
					: null;
			fieldInfo.nameSourceStart = assignment.sourceStart;
			fieldInfo.nameSourceEnd = assignment.sourceEnd;
			fieldInfo.categories = (char[][]) this.nodesToCategories
					.get(assignment);
			requestor.enterField(fieldInfo);
			//If this field is of an anonymous type, need to notify so that it shows as a child
			if (assignment.inferredType != null
					&& assignment.inferredType.isAnonymous) {
				notifySourceElementRequestor(assignment.inferredType);
			}
		}
	}
	if (isInRange){
		requestor.exitField(
				// filter out initializations that are not a constant (simple check)
				(assignment.getExpression() == null
						|| assignment.getExpression() instanceof ArrayInitializer
						|| assignment.getExpression() instanceof AllocationExpression
						|| assignment.getExpression() instanceof ArrayAllocationExpression
						|| assignment.getExpression() instanceof Assignment
						|| assignment.getExpression() instanceof ClassLiteralAccess
						|| assignment.getExpression() instanceof MessageSend
						|| assignment.getExpression() instanceof ArrayReference
						|| assignment.getExpression() instanceof ThisReference) ?
					-1 :
					assignment.sourceStart,
				fieldEndPosition,
				assignment.sourceEnd);
	}
}
public void notifySourceElementRequestor(
	ImportReference importReference,
	boolean isPackage) {
	
	requestor.acceptImport(
		importReference.declarationSourceStart,
		importReference.declarationSourceEnd,
		importReference.tokens,
		(importReference.bits & ASTNode.OnDemand) != 0);
	
}
public void notifySourceElementRequestor(TypeDeclaration typeDeclaration, boolean notifyTypePresence, TypeDeclaration declaringType) {

	if (CharOperation.equals(TypeConstants.PACKAGE_INFO_NAME, typeDeclaration.name)) return;

	// range check
	boolean isInRange =
		scanner.initialPosition <= typeDeclaration.declarationSourceStart
		&& scanner.eofPosition >= typeDeclaration.declarationSourceEnd;

	FieldDeclaration[] fields = typeDeclaration.fields;
	AbstractMethodDeclaration[] methods = typeDeclaration.methods;
	TypeDeclaration[] memberTypes = typeDeclaration.memberTypes;
	int fieldCounter = fields == null ? 0 : fields.length;
	int methodCounter = methods == null ? 0 : methods.length;
	int memberTypeCounter = memberTypes == null ? 0 : memberTypes.length;
	int fieldIndex = 0;
	int methodIndex = 0;
	int memberTypeIndex = 0;

	if (notifyTypePresence){
		
		int kind = TypeDeclaration.kind(typeDeclaration.modifiers);
		char[] implicitSuperclassName = TypeConstants.CharArray_JAVA_LANG_OBJECT;
		if (isInRange) {
			int currentModifiers = typeDeclaration.modifiers;

			// remember deprecation so as to not lose it below
			boolean deprecated = (currentModifiers & ClassFileConstants.AccDeprecated) != 0;

			char[] superclassName;
			
			TypeReference superclass = typeDeclaration.superclass;
			superclassName = superclass != null ? CharOperation.concatWith(superclass.getTypeName(), '.') : null;
			
			ISourceElementRequestor.TypeInfo typeInfo = new ISourceElementRequestor.TypeInfo();
			typeInfo.declarationStart = typeDeclaration.declarationSourceStart;
			typeInfo.modifiers = deprecated ? (currentModifiers & ExtraCompilerModifiers.AccJustFlag) | ClassFileConstants.AccDeprecated : currentModifiers & ExtraCompilerModifiers.AccJustFlag;
			typeInfo.name = typeDeclaration.name;
			typeInfo.nameSourceStart = typeDeclaration.sourceStart;
			typeInfo.nameSourceEnd = sourceEnd(typeDeclaration);
			typeInfo.superclass = superclassName;
			typeInfo.categories = (char[][]) this.nodesToCategories.get(typeDeclaration);
			typeInfo.secondary = typeDeclaration.isSecondary();
			typeInfo.anonymousMember = typeDeclaration.allocation != null && typeDeclaration.allocation.enclosingInstance != null;
			requestor.enterType(typeInfo);
			switch (kind) {
				case TypeDeclaration.CLASS_DECL :
					if (superclassName != null)
						implicitSuperclassName = superclassName;
					break;
			}
		}
		if (this.nestedTypeIndex == this.typeNames.length) {
			// need a resize
			System.arraycopy(this.typeNames, 0, (this.typeNames = new char[this.nestedTypeIndex * 2][]), 0, this.nestedTypeIndex);
			System.arraycopy(this.superTypeNames, 0, (this.superTypeNames = new char[this.nestedTypeIndex * 2][]), 0, this.nestedTypeIndex);
		}
		this.typeNames[this.nestedTypeIndex] = typeDeclaration.name;
		this.superTypeNames[this.nestedTypeIndex++] = implicitSuperclassName;
	}
	while ((fieldIndex < fieldCounter)
			|| (memberTypeIndex < memberTypeCounter)
			|| (methodIndex < methodCounter)) {
		FieldDeclaration nextFieldDeclaration = null;
		AbstractMethodDeclaration nextMethodDeclaration = null;
		TypeDeclaration nextMemberDeclaration = null;

		int position = Integer.MAX_VALUE;
		int nextDeclarationType = -1;
		if (fieldIndex < fieldCounter) {
			nextFieldDeclaration = fields[fieldIndex];
			if (nextFieldDeclaration.declarationSourceStart < position) {
				position = nextFieldDeclaration.declarationSourceStart;
				nextDeclarationType = 0; // FIELD
			}
		}
		if (methodIndex < methodCounter) {
			nextMethodDeclaration = methods[methodIndex];
			if (nextMethodDeclaration.declarationSourceStart < position) {
				position = nextMethodDeclaration.declarationSourceStart;
				nextDeclarationType = 1; // METHOD
			}
		}
		if (memberTypeIndex < memberTypeCounter) {
			nextMemberDeclaration = memberTypes[memberTypeIndex];
			if (nextMemberDeclaration.declarationSourceStart < position) {
				position = nextMemberDeclaration.declarationSourceStart;
				nextDeclarationType = 2; // MEMBER
			}
		}
		switch (nextDeclarationType) {
			case 0 :
				fieldIndex++;
				notifySourceElementRequestor(nextFieldDeclaration, typeDeclaration);
				break;
			case 1 :
				methodIndex++;
				notifySourceElementRequestor(nextMethodDeclaration);
				break;
			case 2 :
				memberTypeIndex++;
				notifySourceElementRequestor(nextMemberDeclaration, true, null);
		}
	}
	if (notifyTypePresence){
		if (isInRange){
			requestor.exitType(typeDeclaration.declarationSourceEnd);
		}
		nestedTypeIndex--;
	}
}
public void parseCompilationUnit(
	ICompilationUnit unit,
	int start,
	int end,
	boolean fullParse) {

	this.reportReferenceInfo = fullParse;
	boolean old = diet;

	try {
		diet = true;
		CompilationResult compilationUnitResult = new CompilationResult(unit, 0, 0, this.options.maxProblemsPerUnit);
		CompilationUnitDeclaration parsedUnit = parse(unit, compilationUnitResult, start, end);
		if (scanner.recordLineSeparator) {
			requestor.acceptLineSeparatorPositions(compilationUnitResult.getLineSeparatorPositions());
		}
		if (this.localDeclarationVisitor != null || fullParse){
			diet = false;
			this.getMethodBodies(parsedUnit);
		}
		this.scanner.resetTo(start, end);
		notifySourceElementRequestor(parsedUnit);
	} catch (AbortCompilation e) {
		// ignore this exception
	} finally {
		diet = old;
		reset();
	}
}
public CompilationUnitDeclaration parseCompilationUnit(
	ICompilationUnit unit,
	boolean fullParse) {

	boolean old = diet;

	try {
		diet = DO_DIET_PARSE;
		this.reportReferenceInfo = fullParse;
		CompilationResult compilationUnitResult = new CompilationResult(unit, 0, 0, this.options.maxProblemsPerUnit);
		CompilationUnitDeclaration parsedUnit = parse(unit, compilationUnitResult);
		if (scanner.recordLineSeparator) {
			requestor.acceptLineSeparatorPositions(compilationUnitResult.getLineSeparatorPositions());
		}
		int initialStart = this.scanner.initialPosition;
		int initialEnd = this.scanner.eofPosition;
		if (this.localDeclarationVisitor != null || fullParse){
			diet = false;
			this.getMethodBodies(parsedUnit);
		}
		this.scanner.resetTo(initialStart, initialEnd);

		notifySourceElementRequestor(parsedUnit);
		return parsedUnit;
	} catch (AbortCompilation e) {
		// ignore this exception
	} finally {
		diet = old;
		reset();
	}
	return null;
}
public void parseTypeMemberDeclarations(
	ISourceType type,
	ICompilationUnit sourceUnit,
	int start,
	int end,
	boolean needReferenceInfo) {
	boolean old = diet;

	CompilationResult compilationUnitResult =
		new CompilationResult(sourceUnit, 0, 0, this.options.maxProblemsPerUnit);
	try {
		diet = !needReferenceInfo;
		reportReferenceInfo = needReferenceInfo;
		CompilationUnitDeclaration unit =
			SourceTypeConverter.buildCompilationUnit(
				new ISourceType[]{type},
				// no need for field and methods
				// no need for member types
				// no need for field initialization
				SourceTypeConverter.NONE,
				problemReporter(),
				compilationUnitResult);
		if ((unit == null) || (unit.types == null) || (unit.types.length != 1))
			return;
		this.sourceType = type;
		try {
			/* automaton initialization */
			initialize();
			goForClassBodyDeclarations();
			/* scanner initialization */
			scanner.setSource(sourceUnit.getContents());
			scanner.resetTo(start, end);
			/* unit creation */
			referenceContext = compilationUnit = unit;
			/* initialize the astStacl */
			// the compilationUnitDeclaration should contain exactly one type
			pushOnAstStack(unit.types[0]);
			/* run automaton */
			parse();
			notifySourceElementRequestor(unit);
		} finally {
			unit = compilationUnit;
			compilationUnit = null; // reset parser
		}
	} catch (AbortCompilation e) {
		// ignore this exception
	} finally {
		if (scanner.recordLineSeparator) {
			requestor.acceptLineSeparatorPositions(compilationUnitResult.getLineSeparatorPositions());
		}
		diet = old;
		reset();
	}
}

public void parseTypeMemberDeclarations(
	char[] contents,
	int start,
	int end) {

	boolean old = diet;

	try {
		diet = true;

		/* automaton initialization */
		initialize();
		goForClassBodyDeclarations();
		/* scanner initialization */
		scanner.setSource(contents);
		scanner.recordLineSeparator = false;
		scanner.taskTags = null;
		scanner.taskPriorities = null;
		scanner.resetTo(start, end);

		/* unit creation */
		referenceContext = null;

		/* initialize the astStacl */
		// the compilationUnitDeclaration should contain exactly one type
		/* run automaton */
		parse();
		notifySourceElementRequestor((CompilationUnitDeclaration)null);
	} catch (AbortCompilation e) {
		// ignore this exception
	} finally {
		diet = old;
		reset();
	}
}
/*
 * Sort the given ast nodes by their positions.
 */
//private static void quickSort(ASTNode[] sortedCollection, int left, int right) {
//	int original_left = left;
//	int original_right = right;
//	ASTNode mid = sortedCollection[ left +  (right - left) / 2];
//	do {
//		while (sortedCollection[left].sourceStart < mid.sourceStart) {
//			left++;
//		}
//		while (mid.sourceStart < sortedCollection[right].sourceStart) {
//			right--;
//		}
//		if (left <= right) {
//			ASTNode tmp = sortedCollection[left];
//			sortedCollection[left] = sortedCollection[right];
//			sortedCollection[right] = tmp;
//			left++;
//			right--;
//		}
//	} while (left <= right);
//	if (original_left < right) {
//		quickSort(sortedCollection, original_left, right);
//	}
//	if (left < original_right) {
//		quickSort(sortedCollection, left, original_right);
//	}
//}
private void rememberCategories() {
	if (this.useSourceJavadocParser) {
		SourceJavadocParser sourceJavadocParser = (SourceJavadocParser) this.javadocParser;
		char[][] categories =  sourceJavadocParser.categories;
		if (categories.length > 0) {
			this.nodesToCategories.put(this.astStack[this.astPtr], categories);
			sourceJavadocParser.categories = CharOperation.NO_CHAR_CHAR;
		}
	}
}
private void reset() {
	this.sourceEnds = new HashtableOfObjectToInt();
	this.nodesToCategories = new HashMap();
	typeNames = new char[4][];
	superTypeNames = new char[4][];
	nestedTypeIndex = 0;
}
private int sourceEnd(TypeDeclaration typeDeclaration) {
	if ((typeDeclaration.bits & ASTNode.IsAnonymousType) != 0) {
		QualifiedAllocationExpression allocation = typeDeclaration.allocation;
		if (allocation.type == null) // case of enum constant body
			return typeDeclaration.sourceEnd;
		return allocation.type.sourceEnd;
	} else {
		return typeDeclaration.sourceEnd;
	}
}
	private void visitIfNeeded(AbstractMethodDeclaration method) {
		this.nestedMethodIndex++;
		
		if (this.localDeclarationVisitor != null){
			if (method instanceof ConstructorDeclaration) {
				ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) method;
				if (constructorDeclaration.constructorCall != null) {
					constructorDeclaration.constructorCall.traverse(this.localDeclarationVisitor, method.getScope());
				}
			}
			if (method.arguments != null) {
				int argumentLength = method.arguments.length;
				for (int i = 0; i < argumentLength; i++)
					method.arguments[i].traverse(contextDeclarationNotifier, method.getScope());
			}
			if (method.statements != null) {
				int statementsLength = method.statements.length;
				for (int i = 0; i < statementsLength; i++) {
					method.statements[i].traverse( contextDeclarationNotifier, method.getScope() );
				}
			}
		}
		
		this.nestedMethodIndex--;
	}

private void visitIfNeeded(AbstractVariableDeclaration field, TypeDeclaration declaringType) {
	if (this.localDeclarationVisitor != null
		&& (field.bits & ASTNode.HasLocalType) != 0) {
			if (field.initialization != null) {
				try {
					this.localDeclarationVisitor.pushDeclaringType(declaringType);
					field.initialization.traverse(this.localDeclarationVisitor, (MethodScope) null);
				} finally {
					this.localDeclarationVisitor.popDeclaringType();
				}
			}
	}
}

private void visitIfNeeded(Initializer initializer) {
	if (this.localDeclarationVisitor != null
		&& (initializer.bits & ASTNode.HasLocalType) != 0) {
			if (initializer.block != null) {
				initializer.block.traverse(this.localDeclarationVisitor, null);
			}
	}
}
}
