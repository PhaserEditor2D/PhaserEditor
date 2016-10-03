/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     bug 227489 - Etienne Pfister <epfister@hsr.ch>
 *******************************************************************************/

package org.eclipse.wst.jsdt.core.dom;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocArgumentExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocFieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocMessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectGetterSetterField;
import org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds;
import org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement;
import org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteralConcatenation;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.parser.RecoveryScanner;
import org.eclipse.wst.jsdt.internal.compiler.parser.Scanner;
import org.eclipse.wst.jsdt.internal.compiler.parser.TerminalTokens;

/**
 * Internal class for converting internal compiler ASTs into public ASTs.
 */
class ASTConverter {

	protected AST ast;
	protected Comment[] commentsTable;
	char[] compilationUnitSource;
	int compilationUnitSourceLength;
	protected DocCommentParser docParser;
	// comments
	protected boolean insideComments;
	protected IProgressMonitor monitor;
	protected Set pendingNameScopeResolution;
	protected Set pendingThisExpressionScopeResolution;
	protected boolean resolveBindings;
	Scanner scanner;
	private DefaultCommentMapper commentMapper;

	public ASTConverter(Map options, boolean resolveBindings, IProgressMonitor monitor) {
		this.resolveBindings = resolveBindings;
		Object sourceModeSetting = options.get(JavaScriptCore.COMPILER_SOURCE);
		long sourceLevel = ClassFileConstants.JDK1_3;
		if (JavaScriptCore.VERSION_1_4.equals(sourceModeSetting)) {
			sourceLevel = ClassFileConstants.JDK1_4;
		} else if (JavaScriptCore.VERSION_1_5.equals(sourceModeSetting)) {
			sourceLevel = ClassFileConstants.JDK1_5;
		}

		this.scanner = new Scanner(
			true /*comment*/,
			false /*whitespace*/,
			false /*nls*/,
			sourceLevel /*sourceLevel*/,
			null /*taskTags*/,
			null/*taskPriorities*/,
			true/*taskCaseSensitive*/);
		this.monitor = monitor;
		this.insideComments = JavaScriptCore.ENABLED.equals(options.get(JavaScriptCore.COMPILER_DOC_COMMENT_SUPPORT));
	}

	protected void adjustSourcePositionsForParent(org.eclipse.wst.jsdt.internal.compiler.ast.Expression expression) {
		int start = expression.sourceStart;
		int end = expression.sourceEnd;
		int leftParentCount = 1;
		int rightParentCount = 0;
		this.scanner.resetTo(start, end);
		try {
			int token = this.scanner.getNextToken();
			expression.sourceStart = this.scanner.currentPosition;
			boolean stop = false;
			while (!stop && ((token  = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF)) {
				switch(token) {
					case TerminalTokens.TokenNameLPAREN:
						leftParentCount++;
						break;
					case TerminalTokens.TokenNameRPAREN:
						rightParentCount++;
						if (rightParentCount == leftParentCount) {
							// we found the matching parenthesis
							stop = true;
						}
				}
			}
			expression.sourceEnd = this.scanner.startPosition - 1;
		} catch(InvalidInputException e) {
			// ignore
		}
	}

	protected void buildBodyDeclarations(org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration typeDeclaration, AbstractTypeDeclaration typeDecl) {
		// add body declaration in the lexical order
		org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration[] members = typeDeclaration.memberTypes;
		org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration[] fields = typeDeclaration.fields;
		org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration[] methods = typeDeclaration.methods;

		int fieldsLength = fields == null? 0 : fields.length;
		int methodsLength = methods == null? 0 : methods.length;
		int membersLength = members == null ? 0 : members.length;
		int fieldsIndex = 0;
		int methodsIndex = 0;
		int membersIndex = 0;

		while ((fieldsIndex < fieldsLength)
			|| (membersIndex < membersLength)
			|| (methodsIndex < methodsLength)) {
			org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration nextFieldDeclaration = null;
			org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration nextMethodDeclaration = null;
			org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration nextMemberDeclaration = null;

			int position = Integer.MAX_VALUE;
			int nextDeclarationType = -1;
			if (fieldsIndex < fieldsLength) {
				nextFieldDeclaration = fields[fieldsIndex];
				if (nextFieldDeclaration.declarationSourceStart < position) {
					position = nextFieldDeclaration.declarationSourceStart;
					nextDeclarationType = 0; // FIELD
				}
			}
			if (methodsIndex < methodsLength) {
				nextMethodDeclaration = methods[methodsIndex];
				if (nextMethodDeclaration.declarationSourceStart < position) {
					position = nextMethodDeclaration.declarationSourceStart;
					nextDeclarationType = 1; // METHOD
				}
 			}
			if (membersIndex < membersLength) {
				nextMemberDeclaration = members[membersIndex];
				if (nextMemberDeclaration.declarationSourceStart < position) {
					position = nextMemberDeclaration.declarationSourceStart;
					nextDeclarationType = 2; // MEMBER
				}
			}
			switch (nextDeclarationType) {
				case 0 :
						checkAndAddMultipleFieldDeclaration(fields, fieldsIndex, typeDecl.bodyDeclarations());
					fieldsIndex++;
					break;
				case 1 :
					methodsIndex++;
					if (!nextMethodDeclaration.isDefaultConstructor() && !nextMethodDeclaration.isClinit()) {
						typeDecl.bodyDeclarations().add(convert(nextMethodDeclaration));
					}
					break;
				case 2 :
					membersIndex++;
					ASTNode node = convert(nextMemberDeclaration);
					if (node == null) {
						typeDecl.setFlags(typeDecl.getFlags() | ASTNode.MALFORMED);
					} else {
						typeDecl.bodyDeclarations().add(node);
					}
			}
		}
		// Convert javadoc
		convert(typeDeclaration.javadoc, typeDecl);
	}


	protected void buildBodyDeclarations(org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration expression, AnonymousClassDeclaration anonymousClassDeclaration) {
		// add body declaration in the lexical order
		org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration[] members = expression.memberTypes;
		org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration[] fields = expression.fields;
		org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration[] methods = expression.methods;

		int fieldsLength = fields == null? 0 : fields.length;
		int methodsLength = methods == null? 0 : methods.length;
		int membersLength = members == null ? 0 : members.length;
		int fieldsIndex = 0;
		int methodsIndex = 0;
		int membersIndex = 0;

		while ((fieldsIndex < fieldsLength)
			|| (membersIndex < membersLength)
			|| (methodsIndex < methodsLength)) {
			org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration nextFieldDeclaration = null;
			org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration nextMethodDeclaration = null;
			org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration nextMemberDeclaration = null;

			int position = Integer.MAX_VALUE;
			int nextDeclarationType = -1;
			if (fieldsIndex < fieldsLength) {
				nextFieldDeclaration = fields[fieldsIndex];
				if (nextFieldDeclaration.declarationSourceStart < position) {
					position = nextFieldDeclaration.declarationSourceStart;
					nextDeclarationType = 0; // FIELD
				}
			}
			if (methodsIndex < methodsLength) {
				nextMethodDeclaration = methods[methodsIndex];
				if (nextMethodDeclaration.declarationSourceStart < position) {
					position = nextMethodDeclaration.declarationSourceStart;
					nextDeclarationType = 1; // METHOD
				}
			}
			if (membersIndex < membersLength) {
				nextMemberDeclaration = members[membersIndex];
				if (nextMemberDeclaration.declarationSourceStart < position) {
					position = nextMemberDeclaration.declarationSourceStart;
					nextDeclarationType = 2; // MEMBER
				}
			}
			switch (nextDeclarationType) {
				case 0 :
						checkAndAddMultipleFieldDeclaration(fields, fieldsIndex, anonymousClassDeclaration.bodyDeclarations());
					fieldsIndex++;
					break;
				case 1 :
					methodsIndex++;
					if (!nextMethodDeclaration.isDefaultConstructor() && !nextMethodDeclaration.isClinit()) {
						anonymousClassDeclaration.bodyDeclarations().add(convert(nextMethodDeclaration));
					}
					break;
				case 2 :
					membersIndex++;
					ASTNode node = convert(nextMemberDeclaration);
					if (node == null) {
						anonymousClassDeclaration.setFlags(anonymousClassDeclaration.getFlags() | ASTNode.MALFORMED);
					} else {
						anonymousClassDeclaration.bodyDeclarations().add(node);
					}
			}
		}
	}

	/**
	 * @param compilationUnit
	 * @param comments
	 */
	void buildCommentsTable(JavaScriptUnit compilationUnit, int[][] comments) {
		// Build comment table
		this.commentsTable = new Comment[comments.length];
		int nbr = 0;
		for (int i = 0; i < comments.length; i++) {
			Comment comment = createComment(comments[i]);
			if (comment != null) {
				comment.setAlternateRoot(compilationUnit);
				this.commentsTable[nbr++] = comment;
			}
		}
		// Resize table if  necessary
		if (nbr<comments.length) {
			Comment[] newCommentsTable = new Comment[nbr];
			System.arraycopy(this.commentsTable, 0, newCommentsTable, 0, nbr);
			this.commentsTable = newCommentsTable;
		}
		compilationUnit.setCommentTable(this.commentsTable);
	}

	protected void checkAndAddMultipleFieldDeclaration(org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration[] fields, int index, List bodyDeclarations) {
		if (fields[index] instanceof org.eclipse.wst.jsdt.internal.compiler.ast.Initializer) {
			org.eclipse.wst.jsdt.internal.compiler.ast.Initializer oldInitializer = (org.eclipse.wst.jsdt.internal.compiler.ast.Initializer) fields[index];
			Initializer initializer = new Initializer(this.ast);
			initializer.setBody(convert(oldInitializer.block));
			setModifiers(initializer, oldInitializer);
			initializer.setSourceRange(oldInitializer.declarationSourceStart, oldInitializer.sourceEnd - oldInitializer.declarationSourceStart + 1);
			// The jsdoc comment is now got from list store in javaScript unit declaration
			convert(oldInitializer.javadoc, initializer);
			bodyDeclarations.add(initializer);
			return;
		}
		if (index > 0 && fields[index - 1].declarationSourceStart == fields[index].declarationSourceStart) {
			// we have a multiple field declaration
			// We retrieve the existing fieldDeclaration to add the new VariableDeclarationFragment
			FieldDeclaration fieldDeclaration = (FieldDeclaration) bodyDeclarations.get(bodyDeclarations.size() - 1);
			fieldDeclaration.fragments().add(convertToVariableDeclarationFragment(fields[index]));
		} else {
			// we can create a new FieldDeclaration
			bodyDeclarations.add(convertToFieldDeclaration(fields[index]));
		}
	}

	protected void checkAndAddMultipleLocalDeclaration(org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement[] stmts, int index, List blockStatements) {
//		if (index > 0
//		    && stmts[index - 1] instanceof org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration) {
//		    	org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration local1 = (org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration) stmts[index - 1];
//		    	org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration local2 = (org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration) stmts[index];
//			   if (local1.declarationSourceStart == local2.declarationSourceStart) {
//					// we have a multiple local declarations
//					// We retrieve the existing VariableDeclarationStatement to add the new VariableDeclarationFragment
//					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) blockStatements.get(blockStatements.size() - 1);
//					variableDeclarationStatement.fragments().add(convertToVariableDeclarationFragment((org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration)stmts[index]));
//			   } else {
//					// we can create a new FieldDeclaration
//					blockStatements.add(convertToVariableDeclarationStatement((org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration)stmts[index]));
//			   }
//		} else {
//			// we can create a new FieldDeclaration
//			blockStatements.add(convertToVariableDeclarationStatement((org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration)stmts[index]));
//		}
		VariableDeclarationStatement variableDeclarationStatement = convertToVariableDeclarationStatement((org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration)stmts[index]);
		blockStatements.add(variableDeclarationStatement);
	}

	protected void checkCanceled() {
		if (this.monitor != null && this.monitor.isCanceled())
			throw new OperationCanceledException();
	}

	protected void completeRecord(ArrayType arrayType, org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode astNode) {
		ArrayType array = arrayType;
		int dimensions = array.getDimensions();
		for (int i = 0; i < dimensions; i++) {
			Type componentType = array.getComponentType();
			this.recordNodes(componentType, astNode);
			if (componentType.isArrayType()) {
				array = (ArrayType) componentType;
			}
		}
	}


	public ASTNode convert(org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration methodDeclaration) {
		checkCanceled();
		trimWhiteSpacesAndComments(methodDeclaration);
		FunctionDeclaration methodDecl = new FunctionDeclaration(this.ast);
		setModifiers(methodDecl, methodDeclaration);
		boolean isConstructor = methodDeclaration.isConstructor();
		methodDecl.setConstructor(isConstructor);
		int start = methodDeclaration.sourceStart;
		int end;
		 SimpleName methodName =null;
		if (methodDeclaration.selector != null) // We couldn't use inferred method name here, so 
												// do not use methodDeclaration.getName() here!
		{
			  methodName = new SimpleName(this.ast);
			methodName.internalSetIdentifier(new String(methodDeclaration.selector));
			end = retrieveIdentifierEndPosition(start, methodDeclaration.sourceEnd);

			methodName.setSourceRange(start, end == -1 ? 0 : end - start + 1);
			methodDecl.setName(methodName);
		}
		else
			end= methodDeclaration.sourceStart;
		org.eclipse.wst.jsdt.internal.compiler.ast.Argument[] parameters = methodDeclaration.arguments;
		if (parameters != null) {
			int parametersLength = parameters.length;
			for (int i = 0; i < parametersLength; i++) {
				methodDecl.parameters().add(convert(parameters[i]));
			}
		}
		org.eclipse.wst.jsdt.internal.compiler.ast.ExplicitConstructorCall explicitConstructorCall = null;
		/* need this check because a constructor could have been made a constructor after the
		 * method declaration was created, and thus it is not a ConstructorDeclaration
		 */
		if (isConstructor  && methodDeclaration instanceof ConstructorDeclaration) {
			ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) methodDeclaration;
			explicitConstructorCall = constructorDeclaration.constructorCall;
			switch(this.ast.apiLevel) {
				case AST.JLS2_INTERNAL :
					// set the return type to VOID
					PrimitiveType returnType = new PrimitiveType(this.ast);
					returnType.setPrimitiveTypeCode(PrimitiveType.VOID);
					returnType.setSourceRange(methodDeclaration.sourceStart, 0);
					methodDecl.internalSetReturnType(returnType);
					break;
				case AST.JLS3 :
					methodDecl.setReturnType2(null);
			}
		} else if (methodDeclaration instanceof org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration) {
			org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration method = (org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration) methodDeclaration;
			org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference typeReference = method.returnType;
			if (typeReference != null) {
				Type returnType = convertType(typeReference,method.inferredType);
				// get the positions of the right parenthesis
				int rightParenthesisPosition = retrieveEndOfRightParenthesisPosition(end, method.bodyEnd);
				int extraDimensions = retrieveExtraDimension(rightParenthesisPosition, method.bodyEnd);
				methodDecl.setExtraDimensions(extraDimensions);
				setTypeForMethodDeclaration(methodDecl, returnType, extraDimensions);
			} else {
				switch(this.ast.apiLevel) {
					case AST.JLS2_INTERNAL :
						methodDecl.setFlags(methodDecl.getFlags() | ASTNode.MALFORMED);
						break;
					case AST.JLS3 :
						methodDecl.setReturnType2(null);
				}
			}
		}
		int declarationSourceStart = methodDeclaration.declarationSourceStart;
		int declarationSourceEnd = methodDeclaration.bodyEnd;
		methodDecl.setSourceRange(declarationSourceStart, declarationSourceEnd - declarationSourceStart + 1);
		int closingPosition = retrieveRightBraceOrSemiColonPosition(methodDeclaration.bodyEnd + 1, methodDeclaration.declarationSourceEnd);
		if (closingPosition != -1) {
			int startPosition = methodDecl.getStartPosition();
			methodDecl.setSourceRange(startPosition, closingPosition - startPosition + 1);

			org.eclipse.wst.jsdt.internal.compiler.ast.Statement[] statements = methodDeclaration.statements;

			start = retrieveStartBlockPosition(methodDeclaration.sourceStart, declarationSourceEnd);
			end = retrieveEndBlockPosition(methodDeclaration.sourceStart, methodDeclaration.declarationSourceEnd);
			Block block = null;
			if (start != -1 && end != -1) {
				/*
				 * start or end can be equal to -1 if we have an interface's method.
				 */
				block = new Block(this.ast);
				block.setSourceRange(start, end - start + 1);
				methodDecl.setBody(block);
			}
			if (block != null && (statements != null || explicitConstructorCall != null)) {
				if (explicitConstructorCall != null && explicitConstructorCall.accessMode != org.eclipse.wst.jsdt.internal.compiler.ast.ExplicitConstructorCall.ImplicitSuper) {
					block.statements().add(convert(explicitConstructorCall));
				}
				int statementsLength = statements == null ? 0 : statements.length;
				for (int i = 0; i < statementsLength; i++) {
					if (statements[i] instanceof org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration) {
						checkAndAddMultipleLocalDeclaration(statements, i, block.statements());
					} else if (statements[i] instanceof org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration) { // fix for inner function handling, Etienne Pfister
					   	org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration method = (org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration) statements[i];
					   	block.statements().add(convert(method));
					}
					else {
						final Statement statement = convert(statements[i]);
						if (statement != null) {
							block.statements().add(statement);
						}
					}
				}
			}
			if (block != null && (Modifier.isAbstract(methodDecl.getModifiers()) || Modifier.isNative(methodDecl.getModifiers()))) {
				methodDecl.setFlags(methodDecl.getFlags() | ASTNode.MALFORMED);
			}
		} else {
			// syntax error in this method declaration
			if (!methodDeclaration.isAbstract()) {
				start = retrieveStartBlockPosition(methodDeclaration.sourceStart, declarationSourceEnd);
				end = methodDeclaration.bodyEnd;
				// try to get the best end position
				CategorizedProblem[] problems = methodDeclaration.compilationResult().problems;
				if (problems != null) {
					for (int i = 0, max = methodDeclaration.compilationResult().problemCount; i < max; i++) {
						CategorizedProblem currentProblem = problems[i];
						if (currentProblem.getSourceStart() == start && currentProblem.getID() == IProblem.ParsingErrorInsertToComplete) {
							end = currentProblem.getSourceEnd();
							break;
						}
					}
				}
				int startPosition = methodDecl.getStartPosition();
				methodDecl.setSourceRange(startPosition, end - startPosition + 1);
				if (start != -1 && end != -1) {
					/*
					 * start or end can be equal to -1 if we have an interface's method.
					 */
					Block block = new Block(this.ast);
					block.setSourceRange(start, end - start + 1);
					methodDecl.setBody(block);
				}
			}
		}

		// The jsdoc comment is now got from list store in javaScript unit declaration
		convert(methodDeclaration.javadoc, methodDecl);
		if (this.resolveBindings) {
			recordNodes(methodDecl, methodDeclaration);
			if (methodName!=null)
			  recordNodes(methodName, methodDeclaration);
			methodDecl.resolveBinding();
		}
		return methodDecl;
	}

	public ClassInstanceCreation convert(org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression expression) {
		ClassInstanceCreation classInstanceCreation = new ClassInstanceCreation(this.ast);
		if (this.resolveBindings) {
			recordNodes(classInstanceCreation, expression);
		}
		if (expression.type!=null) {
			switch (this.ast.apiLevel) {
			case AST.JLS2_INTERNAL:
				classInstanceCreation.internalSetName(convert(expression.type));
				break;
			case AST.JLS3:
				classInstanceCreation.setType(convertType(expression.type));
			}
		}
		classInstanceCreation.setMember(convert(expression.member));
		classInstanceCreation.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		org.eclipse.wst.jsdt.internal.compiler.ast.Expression[] arguments = expression.arguments;
		if (arguments != null) {
			int length = arguments.length;
			for (int i = 0; i < length; i++) {
				classInstanceCreation.arguments().add(convert(arguments[i]));
			}
		}
		removeTrailingCommentFromExpressionEndingWithAParen(classInstanceCreation);
		return classInstanceCreation;
	}

	public Expression convert(org.eclipse.wst.jsdt.internal.compiler.ast.AND_AND_Expression expression) {
		InfixExpression infixExpression = new InfixExpression(this.ast);
		infixExpression.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
		if (this.resolveBindings) {
			this.recordNodes(infixExpression, expression);
		}
		final int expressionOperatorID = (expression.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorMASK) >> org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorSHIFT;
		if (expression.left instanceof org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression
				&& ((expression.left.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) == 0)) {
			// create an extended string literal equivalent => use the extended operands list
			infixExpression.extendedOperands().add(convert(expression.right));
			org.eclipse.wst.jsdt.internal.compiler.ast.Expression leftOperand = expression.left;
			org.eclipse.wst.jsdt.internal.compiler.ast.Expression rightOperand = null;
			do {
				rightOperand = ((org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression) leftOperand).right;
				if ((((leftOperand.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorMASK) >> org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorSHIFT) != expressionOperatorID
							&& ((leftOperand.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) == 0))
					 || ((rightOperand instanceof org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression
				 			&& ((rightOperand.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorMASK) >> org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorSHIFT) != expressionOperatorID)
							&& ((rightOperand.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) == 0))) {
				 	List extendedOperands = infixExpression.extendedOperands();
				 	InfixExpression temp = new InfixExpression(this.ast);
					if (this.resolveBindings) {
						this.recordNodes(temp, expression);
					}
				 	temp.setOperator(getOperatorFor(expressionOperatorID));
				 	Expression leftSide = convert(leftOperand);
					temp.setLeftOperand(leftSide);
					temp.setSourceRange(leftSide.getStartPosition(), leftSide.getLength());
					int size = extendedOperands.size();
				 	for (int i = 0; i < size - 1; i++) {
				 		Expression expr = temp;
				 		temp = new InfixExpression(this.ast);

						if (this.resolveBindings) {
							this.recordNodes(temp, expression);
						}
				 		temp.setLeftOperand(expr);
					 	temp.setOperator(getOperatorFor(expressionOperatorID));
						temp.setSourceRange(expr.getStartPosition(), expr.getLength());
				 	}
				 	infixExpression = temp;
				 	for (int i = 0; i < size; i++) {
				 		Expression extendedOperand = (Expression) extendedOperands.remove(size - 1 - i);
				 		temp.setRightOperand(extendedOperand);
				 		int startPosition = temp.getLeftOperand().getStartPosition();
				 		temp.setSourceRange(startPosition, extendedOperand.getStartPosition() + extendedOperand.getLength() - startPosition);
				 		if (temp.getLeftOperand().getNodeType() == ASTNode.INFIX_EXPRESSION) {
				 			temp = (InfixExpression) temp.getLeftOperand();
				 		}
				 	}
					int startPosition = infixExpression.getLeftOperand().getStartPosition();
					infixExpression.setSourceRange(startPosition, expression.sourceEnd - startPosition + 1);
					if (this.resolveBindings) {
						this.recordNodes(infixExpression, expression);
					}
					return infixExpression;
				}
				infixExpression.extendedOperands().add(0, convert(rightOperand));
				leftOperand = ((org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression) leftOperand).left;
			} while (leftOperand instanceof org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression && ((leftOperand.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) == 0));
			Expression leftExpression = convert(leftOperand);
			infixExpression.setLeftOperand(leftExpression);
			infixExpression.setRightOperand((Expression)infixExpression.extendedOperands().remove(0));
			int startPosition = leftExpression.getStartPosition();
			infixExpression.setSourceRange(startPosition, expression.sourceEnd - startPosition + 1);
			return infixExpression;
		}
		Expression leftExpression = convert(expression.left);
		infixExpression.setLeftOperand(leftExpression);
		infixExpression.setRightOperand(convert(expression.right));
		infixExpression.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
		int startPosition = leftExpression.getStartPosition();
		infixExpression.setSourceRange(startPosition, expression.sourceEnd - startPosition + 1);
		return infixExpression;
	}



	public SingleVariableDeclaration convert(org.eclipse.wst.jsdt.internal.compiler.ast.Argument argument) {
		SingleVariableDeclaration variableDecl = new SingleVariableDeclaration(this.ast);
		setModifiers(variableDecl, argument);
		final SimpleName name = new SimpleName(this.ast);
		name.internalSetIdentifier(new String(argument.name));
		int start = argument.sourceStart;
		int nameEnd = argument.sourceEnd;
		name.setSourceRange(start, nameEnd - start + 1);
		variableDecl.setName(name);
//		final int typeSourceEnd = argument.type.sourceEnd;
		final int extraDimensions = 0;
//		final int extraDimensions = retrieveExtraDimension(nameEnd + 1, typeSourceEnd);
//		variableDecl.setExtraDimensions(extraDimensions);
//		final boolean isVarArgs = argument.isVarArgs();
//		if (isVarArgs && extraDimensions == 0) {
//			// remove the ellipsis from the type source end
//			argument.type.sourceEnd = retrieveEllipsisStartPosition(argument.type.sourceStart, typeSourceEnd);
//		}
		Type type = convertType(argument.type,argument.inferredType);
//		int typeEnd = type.getStartPosition() + type.getLength() - 1;
//		int rightEnd = Math.max(typeEnd, argument.declarationSourceEnd);
		int rightEnd = argument.declarationSourceEnd;
		/*
		 * There is extra work to do to set the proper type positions
		 * See PR http://bugs.eclipse.org/bugs/show_bug.cgi?id=23284
		 */
//		if (isVarArgs) {
//			setTypeForSingleVariableDeclaration(variableDecl, type, extraDimensions + 1);
//			if (extraDimensions != 0) {
//				variableDecl.setFlags(variableDecl.getFlags() | ASTNode.MALFORMED);
//			}
//		} else {
			setTypeForSingleVariableDeclaration(variableDecl, type, extraDimensions);
//		}
		variableDecl.setSourceRange(argument.declarationSourceStart, rightEnd - argument.declarationSourceStart + 1);

//		if (isVarArgs) {
//			switch(this.ast.apiLevel) {
//				case AST.JLS2_INTERNAL :
//					variableDecl.setFlags(variableDecl.getFlags() | ASTNode.MALFORMED);
//					break;
//				case AST.JLS3 :
//					variableDecl.setVarargs(true);
//			}
//		}
		if (this.resolveBindings) {
			recordNodes(name, argument);
			recordNodes(variableDecl, argument);
			variableDecl.resolveBinding();
		}
		return variableDecl;
	}


	public ArrayCreation convert(org.eclipse.wst.jsdt.internal.compiler.ast.ArrayAllocationExpression expression) {
		ArrayCreation arrayCreation = new ArrayCreation(this.ast);
		if (this.resolveBindings) {
			recordNodes(arrayCreation, expression);
		}
		arrayCreation.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		org.eclipse.wst.jsdt.internal.compiler.ast.Expression[] dimensions = expression.dimensions;

		int dimensionsLength = dimensions.length;
		for (int i = 0; i < dimensionsLength; i++) {
			if (dimensions[i] != null) {
				Expression dimension = convert(dimensions[i]);
				if (this.resolveBindings) {
					recordNodes(dimension, dimensions[i]);
				}
				arrayCreation.dimensions().add(dimension);
			}
		}
		Type type = convertType(expression.type);
		if (this.resolveBindings) {
			recordNodes(type, expression.type);
		}
		ArrayType arrayType = null;
		if (type.isArrayType()) {
			arrayType = (ArrayType) type;
		} else {
			arrayType = this.ast.newArrayType(type, dimensionsLength);
			if (this.resolveBindings) {
				completeRecord(arrayType, expression);
			}
			int start = type.getStartPosition();
			int end = type.getStartPosition() + type.getLength();
			int previousSearchStart = end;
			ArrayType componentType = (ArrayType) type.getParent();
			for (int i = 0; i < dimensionsLength; i++) {
				previousSearchStart = retrieveRightBracketPosition(previousSearchStart + 1, this.compilationUnitSourceLength);
				componentType.setSourceRange(start, previousSearchStart - start + 1);
				componentType = (ArrayType) componentType.getParent();
			}
		}
		arrayCreation.setType(arrayType);
		if (this.resolveBindings) {
			recordNodes(arrayType, expression);
		}
		if (expression.initializer != null) {
			arrayCreation.setInitializer(convert(expression.initializer));
		}
		return arrayCreation;
	}

	public ArrayInitializer convert(org.eclipse.wst.jsdt.internal.compiler.ast.ArrayInitializer expression) {
		ArrayInitializer arrayInitializer = new ArrayInitializer(this.ast);
		if (this.resolveBindings) {
			recordNodes(arrayInitializer, expression);
		}
		arrayInitializer.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		org.eclipse.wst.jsdt.internal.compiler.ast.Expression[] expressions = expression.expressions;
		if (expressions != null) {
			int length = expressions.length;
			for (int i = 0; i < length; i++) {
				Expression expr = convert(expressions[i]);
				if (this.resolveBindings) {
					recordNodes(expr, expressions[i]);
				}
				arrayInitializer.expressions().add(expr);
			}
		}
		return arrayInitializer;
	}

	public ArrayAccess convert(org.eclipse.wst.jsdt.internal.compiler.ast.ArrayReference reference) {
		ArrayAccess arrayAccess = new ArrayAccess(this.ast);
		if (this.resolveBindings) {
			recordNodes(arrayAccess, reference);
		}
		arrayAccess.setSourceRange(reference.sourceStart, reference.sourceEnd - reference.sourceStart + 1);
		arrayAccess.setArray(convert(reference.receiver));
		arrayAccess.setIndex(convert(reference.position));
		return arrayAccess;
	}

	public Assignment convert(org.eclipse.wst.jsdt.internal.compiler.ast.Assignment expression) {
		Assignment assignment = new Assignment(this.ast);
		if (this.resolveBindings) {
			recordNodes(assignment, expression);
		}
		Expression lhs = convert(expression.lhs);
		assignment.setLeftHandSide(lhs);
		assignment.setOperator(Assignment.Operator.ASSIGN);
		assignment.setRightHandSide(convert(expression.expression));
		int start = lhs.getStartPosition();
		assignment.setSourceRange(start, expression.sourceEnd - start + 1);
		return assignment;
	}

	/*
	 * Internal use only
	 * Used to convert class body declarations
	 */
	public JavaScriptUnit convert(org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode[] nodes, JavaScriptUnit compilationUnit) {
//		typeDecl.setInterface(false);
		int nodesLength = nodes.length;
		for (int i = 0; i < nodesLength; i++) {
			org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode node = nodes[i];
			if (node instanceof org.eclipse.wst.jsdt.internal.compiler.ast.Initializer) {
				org.eclipse.wst.jsdt.internal.compiler.ast.Initializer oldInitializer = (org.eclipse.wst.jsdt.internal.compiler.ast.Initializer) node;
				Initializer initializer = new Initializer(this.ast);
				initializer.setBody(convert(oldInitializer.block));
				setModifiers(initializer, oldInitializer);
				initializer.setSourceRange(oldInitializer.declarationSourceStart, oldInitializer.sourceEnd - oldInitializer.declarationSourceStart + 1);
//				setJavaDocComment(initializer);
//				initializer.setJavadoc(convert(oldInitializer.javadoc));
				convert(oldInitializer.javadoc, initializer);
				compilationUnit.statements().add(initializer);
			} else if (node instanceof org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration) {
				org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration fieldDeclaration = (org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration) node;
				if (i > 0
					&& (nodes[i - 1] instanceof org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration)
					&& ((org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration)nodes[i - 1]).declarationSourceStart == fieldDeclaration.declarationSourceStart) {
					// we have a multiple field declaration
					// We retrieve the existing fieldDeclaration to add the new VariableDeclarationFragment
					FieldDeclaration currentFieldDeclaration = (FieldDeclaration) compilationUnit.statements().get(compilationUnit.statements().size() - 1);
					currentFieldDeclaration.fragments().add(convertToVariableDeclarationFragment(fieldDeclaration));
				} else {
					// we can create a new FieldDeclaration
					compilationUnit.statements().add(convertToFieldDeclaration(fieldDeclaration));
				}
			} else if(node instanceof org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration) {
				AbstractMethodDeclaration nextMethodDeclaration = (AbstractMethodDeclaration) node;
				if (!nextMethodDeclaration.isDefaultConstructor() && !nextMethodDeclaration.isClinit()) {
					compilationUnit.statements().add(convert(nextMethodDeclaration));
				}
			} else if (node instanceof org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration) {
			org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration localDeclaration = (org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration) node;
			if (i > 0
				&& (nodes[i - 1] instanceof org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration)
				&& ((org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration)nodes[i - 1]).declarationSourceStart == localDeclaration.declarationSourceStart) {
				// we have a multiple field declaration
				// We retrieve the existing fieldDeclaration to add the new VariableDeclarationFragment
				FieldDeclaration currentFieldDeclaration = (FieldDeclaration) compilationUnit.statements().get(compilationUnit.statements().size() - 1);
				currentFieldDeclaration.fragments().add(convertToVariableDeclarationFragment(localDeclaration));
			} else {
				// we can create a new FieldDeclaration
				compilationUnit.statements().add(convertToFieldDeclaration(localDeclaration));
			}
		}
//			else if(node instanceof org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) {
//				org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration nextMemberDeclaration = (org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) node;
//				ASTNode nextMemberDeclarationNode = convert(nextMemberDeclaration);
//				if (nextMemberDeclarationNode == null) {
//					typeDecl.setFlags(typeDecl.getFlags() | ASTNode.MALFORMED);
//				} else {
//					typeDecl.bodyDeclarations().add(nextMemberDeclarationNode);
//				}
//			}
		}
		return compilationUnit;
	}

	public Expression convert(org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression expression) {
		InfixExpression infixExpression = new InfixExpression(this.ast);
		if (this.resolveBindings) {
			this.recordNodes(infixExpression, expression);
		}

		int expressionOperatorID = (expression.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorMASK) >> org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorSHIFT;
		switch (expressionOperatorID) {
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.EQUAL_EQUAL :
				infixExpression.setOperator(InfixExpression.Operator.EQUALS);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.LESS_EQUAL :
				infixExpression.setOperator(InfixExpression.Operator.LESS_EQUALS);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.GREATER_EQUAL :
				infixExpression.setOperator(InfixExpression.Operator.GREATER_EQUALS);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.NOT_EQUAL :
				infixExpression.setOperator(InfixExpression.Operator.NOT_EQUALS);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.LEFT_SHIFT :
				infixExpression.setOperator(InfixExpression.Operator.LEFT_SHIFT);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.RIGHT_SHIFT :
				infixExpression.setOperator(InfixExpression.Operator.RIGHT_SHIFT_SIGNED);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.UNSIGNED_RIGHT_SHIFT :
				infixExpression.setOperator(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.OR_OR :
				infixExpression.setOperator(InfixExpression.Operator.CONDITIONAL_OR);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.AND_AND :
				infixExpression.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.PLUS :
				infixExpression.setOperator(InfixExpression.Operator.PLUS);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.MINUS :
				infixExpression.setOperator(InfixExpression.Operator.MINUS);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.REMAINDER :
				infixExpression.setOperator(InfixExpression.Operator.REMAINDER);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.XOR :
				infixExpression.setOperator(InfixExpression.Operator.XOR);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.AND :
				infixExpression.setOperator(InfixExpression.Operator.AND);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.MULTIPLY :
				infixExpression.setOperator(InfixExpression.Operator.TIMES);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.OR :
				infixExpression.setOperator(InfixExpression.Operator.OR);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.DIVIDE :
				infixExpression.setOperator(InfixExpression.Operator.DIVIDE);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.GREATER :
				infixExpression.setOperator(InfixExpression.Operator.GREATER);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.LESS :
				infixExpression.setOperator(InfixExpression.Operator.LESS);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.INSTANCEOF :
				infixExpression.setOperator(InfixExpression.Operator.INSTANCEOF);
				break;
            case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.IN:
            	infixExpression.setOperator(InfixExpression.Operator.IN);
            	break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.EQUAL_EQUAL_EQUAL :
				infixExpression.setOperator(InfixExpression.Operator.EQUAL_EQUAL_EQUAL);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.NOT_EQUAL_EQUAL :
				infixExpression.setOperator(InfixExpression.Operator.NOT_EQUAL_EQUAL);
				break;
		}

		if (expression.left instanceof org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression
				&& ((expression.left.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) == 0)) {
			// create an extended string literal equivalent => use the extended operands list
			infixExpression.extendedOperands().add(convert(expression.right));
			org.eclipse.wst.jsdt.internal.compiler.ast.Expression leftOperand = expression.left;
			org.eclipse.wst.jsdt.internal.compiler.ast.Expression rightOperand = null;
			do {
				rightOperand = ((org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression) leftOperand).right;
				if ((((leftOperand.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorMASK) >> org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorSHIFT) != expressionOperatorID
							&& ((leftOperand.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) == 0))
					 || ((rightOperand instanceof org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression
				 			&& ((rightOperand.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorMASK) >> org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorSHIFT) != expressionOperatorID)
							&& ((rightOperand.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) == 0))) {
				 	List extendedOperands = infixExpression.extendedOperands();
				 	InfixExpression temp = new InfixExpression(this.ast);
					if (this.resolveBindings) {
						this.recordNodes(temp, expression);
					}
				 	temp.setOperator(getOperatorFor(expressionOperatorID));
				 	Expression leftSide = convert(leftOperand);
					temp.setLeftOperand(leftSide);
					temp.setSourceRange(leftSide.getStartPosition(), leftSide.getLength());
					int size = extendedOperands.size();
				 	for (int i = 0; i < size - 1; i++) {
				 		Expression expr = temp;
				 		temp = new InfixExpression(this.ast);

						if (this.resolveBindings) {
							this.recordNodes(temp, expression);
						}
				 		temp.setLeftOperand(expr);
					 	temp.setOperator(getOperatorFor(expressionOperatorID));
						temp.setSourceRange(expr.getStartPosition(), expr.getLength());
				 	}
				 	infixExpression = temp;
				 	for (int i = 0; i < size; i++) {
				 		Expression extendedOperand = (Expression) extendedOperands.remove(size - 1 - i);
				 		temp.setRightOperand(extendedOperand);
				 		int startPosition = temp.getLeftOperand().getStartPosition();
				 		temp.setSourceRange(startPosition, extendedOperand.getStartPosition() + extendedOperand.getLength() - startPosition);
				 		if (temp.getLeftOperand().getNodeType() == ASTNode.INFIX_EXPRESSION) {
				 			temp = (InfixExpression) temp.getLeftOperand();
				 		}
				 	}
					int startPosition = infixExpression.getLeftOperand().getStartPosition();
					infixExpression.setSourceRange(startPosition, expression.sourceEnd - startPosition + 1);
					if (this.resolveBindings) {
						this.recordNodes(infixExpression, expression);
					}
					return infixExpression;
				}
				infixExpression.extendedOperands().add(0, convert(rightOperand));
				leftOperand = ((org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression) leftOperand).left;
			} while (leftOperand instanceof org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression && ((leftOperand.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) == 0));
			Expression leftExpression = convert(leftOperand);
			infixExpression.setLeftOperand(leftExpression);
			infixExpression.setRightOperand((Expression)infixExpression.extendedOperands().remove(0));
			int startPosition = leftExpression.getStartPosition();
			infixExpression.setSourceRange(startPosition, expression.sourceEnd - startPosition + 1);
			return infixExpression;
		} else if (expression.left instanceof StringLiteralConcatenation
				&& ((expression.left.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) == 0)
				&& (OperatorIds.PLUS == expressionOperatorID)) {
			StringLiteralConcatenation literal = (StringLiteralConcatenation) expression.left;
			final org.eclipse.wst.jsdt.internal.compiler.ast.Expression[] stringLiterals = literal.literals;
			infixExpression.setLeftOperand(convert(stringLiterals[0]));
			infixExpression.setRightOperand(convert(stringLiterals[1]));
			for (int i = 2; i < literal.counter; i++) {
				infixExpression.extendedOperands().add(convert(stringLiterals[i]));
			}
			infixExpression.extendedOperands().add(convert(expression.right));
			int startPosition = literal.sourceStart;
			infixExpression.setSourceRange(startPosition, expression.sourceEnd - startPosition + 1);
			return infixExpression;
		}
		Expression leftExpression = convert(expression.left);
		infixExpression.setLeftOperand(leftExpression);
		infixExpression.setRightOperand(convert(expression.right));
		int startPosition = leftExpression.getStartPosition();
		infixExpression.setSourceRange(startPosition, expression.sourceEnd - startPosition + 1);
		return infixExpression;
	}

	public Block convert(org.eclipse.wst.jsdt.internal.compiler.ast.Block statement) {
		Block block = new Block(this.ast);
		if (statement.sourceEnd > 0) {
			block.setSourceRange(statement.sourceStart, statement.sourceEnd - statement.sourceStart + 1);
		}
		org.eclipse.wst.jsdt.internal.compiler.ast.Statement[] statements = statement.statements;
		if (statements != null) {
			int statementsLength = statements.length;
			for (int i = 0; i < statementsLength; i++) {
				if (statements[i] instanceof org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration) {
					checkAndAddMultipleLocalDeclaration(statements, i, block.statements());
				} else {
					Statement statement2 = convert(statements[i]);
					if (statement2 != null) {
						block.statements().add(statement2);
					}
				}
			}
		}
		return block;
	}

	public BreakStatement convert(org.eclipse.wst.jsdt.internal.compiler.ast.BreakStatement statement)  {
		BreakStatement breakStatement = new BreakStatement(this.ast);
		breakStatement.setSourceRange(statement.sourceStart, statement.sourceEnd - statement.sourceStart + 1);
		if (statement.label != null) {
			final SimpleName name = new SimpleName(this.ast);
			name.internalSetIdentifier(new String(statement.label));
			retrieveIdentifierAndSetPositions(statement.sourceStart, statement.sourceEnd, name);
			breakStatement.setLabel(name);
		}
		return breakStatement;
	}


	public SwitchCase convert(org.eclipse.wst.jsdt.internal.compiler.ast.CaseStatement statement) {
		SwitchCase switchCase = new SwitchCase(this.ast);
		org.eclipse.wst.jsdt.internal.compiler.ast.Expression constantExpression = statement.constantExpression;
		if (constantExpression == null) {
			switchCase.setExpression(null);
		} else {
			switchCase.setExpression(convert(constantExpression));
		}
		switchCase.setSourceRange(statement.sourceStart, statement.sourceEnd - statement.sourceStart + 1);
		retrieveColonPosition(switchCase);
		return switchCase;
	}

	public FunctionExpression convert(org.eclipse.wst.jsdt.internal.compiler.ast.FunctionExpression expression) {
		FunctionExpression functionExpression = new FunctionExpression(this.ast);
		int sourceEnd = expression.sourceEnd;
		if (sourceEnd==0)
			sourceEnd=expression.methodDeclaration.bodyEnd;
		functionExpression.setSourceRange(expression.sourceStart, sourceEnd - expression.sourceStart + 1);

		functionExpression.setMethod((FunctionDeclaration)convert(expression.methodDeclaration));
		if (this.resolveBindings) {
			recordNodes(functionExpression, expression);
		}
		return functionExpression;
	}

	public ObjectLiteral convert(org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteral objLiteral) {
		ObjectLiteral objectLiteral  = new ObjectLiteral(this.ast);
		objectLiteral.setSourceRange(objLiteral.sourceStart, objLiteral.sourceEnd - objLiteral.sourceStart + 1);

		org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteralField[] fields = objLiteral.fields;
		if (fields != null) {
			int fieldsLength = fields.length;
			for (int i = 0; i < fieldsLength; i++) {

				ObjectLiteralField objectLiteralField = null;
				if(fields[i] instanceof ObjectGetterSetterField) {
					objectLiteralField = convert((ObjectGetterSetterField)fields[i]);
				} else {
					objectLiteralField = convert(fields[i]);
				}
				objectLiteral.fields().add(objectLiteralField);
			}
		}

		if (this.resolveBindings) {
			recordNodes(objectLiteral, objLiteral);
		}
		return objectLiteral;
	}

	public ObjectLiteralField convert(org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteralField field) {
		ObjectLiteralField objectLiteralField = new ObjectLiteralField(this.ast);
		objectLiteralField.setSourceRange(field.sourceStart, field.sourceEnd - field.sourceStart + 1);

		objectLiteralField.setFieldName( convert(field.fieldName));
		objectLiteralField.setInitializer( convert(field.initializer));
		if (this.resolveBindings) {
			recordNodes(objectLiteralField, field);
		}
		return objectLiteralField;
	}
	
	public ObjectLiteralField convert(org.eclipse.wst.jsdt.internal.compiler.ast.ObjectGetterSetterField field) {
		ObjectLiteralField objectLiteralField = new ObjectLiteralField(this.ast);
		objectLiteralField.setSourceRange(field.sourceStart, field.sourceEnd - field.sourceStart + 1);
		
		// ignore get set properties
//		objectLiteralField.setFieldName( convert(field.fieldName));
//		objectLiteralField.setInitializer( convert(field.initializer));
//		if (this.resolveBindings) {
//			recordNodes(objectLiteralField, field);
//		}
		return objectLiteralField;
	}

	public RegularExpressionLiteral convert(org.eclipse.wst.jsdt.internal.compiler.ast.RegExLiteral expression) {
		int length = expression.sourceEnd - expression.sourceStart + 1;
		int sourceStart = expression.sourceStart;
		RegularExpressionLiteral literal = new RegularExpressionLiteral(this.ast);
		if (this.resolveBindings) {
			this.recordNodes(literal, expression);
		}
		literal.internalSetRegularExpression(new String(this.compilationUnitSource, sourceStart, length));
		literal.setSourceRange(sourceStart, length);
		removeLeadingAndTrailingCommentsFromLiteral(literal);
		return literal;
	}

	public Expression convert(org.eclipse.wst.jsdt.internal.compiler.ast.ClassLiteralAccess expression) {
		TypeLiteral typeLiteral = new TypeLiteral(this.ast);
		if (this.resolveBindings) {
			this.recordNodes(typeLiteral, expression);
		}
		typeLiteral.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		typeLiteral.setType(convertType(expression.type));
		return typeLiteral;
	}

	public JavaScriptUnit convert(org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration unit, char[] source) {
		if(unit.compilationResult.recoveryScannerData != null) {
			RecoveryScanner recoveryScanner = new RecoveryScanner(this.scanner, unit.compilationResult.recoveryScannerData.removeUnused());
			this.scanner = recoveryScanner;
			this.docParser.scanner = this.scanner;
		}
		this.compilationUnitSource = source;
		this.compilationUnitSourceLength = source.length;
		this.scanner.setSource(source, unit.compilationResult);
		JavaScriptUnit compilationUnit = new JavaScriptUnit(this.ast);

		// Parse comments
		int[][] comments = unit.comments;
		if (comments != null) {
			buildCommentsTable(compilationUnit, comments);
		}

		// handle the package declaration immediately
		// There is no node corresponding to the package declaration
		if (this.resolveBindings) {
			recordNodes(compilationUnit, unit);
		}
		if (unit.currentPackage != null) {
			PackageDeclaration packageDeclaration = convertPackage(unit);
			compilationUnit.setPackage(packageDeclaration);
		}
		org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference[] imports = unit.imports;
		if (imports != null) {
			int importLength = imports.length;
			for (int i = 0; i < importLength; i++) {
					compilationUnit.imports().add(convertImport(imports[i]));
			}
		}

//		org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration[] types = unit.types;
//		if (types != null) {
//			int typesLength = types.length;
//			for (int i = 0; i < typesLength; i++) {
//				org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration declaration = types[i];
//				if (CharOperation.equals(declaration.name, TypeConstants.PACKAGE_INFO_NAME)) {
//					continue;
//				}
//				ASTNode type = convert(declaration);
//				if (type == null) {
//					compilationUnit.setFlags(compilationUnit.getFlags() | ASTNode.MALFORMED);
//				} else {
//					compilationUnit.types().add(type);
//				}
//			}
//		}
		org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement[] statements = unit.statements;
		if (statements != null) {
			int statementsLength = statements.length;
			for (int i = 0; i < statementsLength; i++) {
//				org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration declaration = types[i];
//				if (CharOperation.equals(declaration.name, TypeConstants.PACKAGE_INFO_NAME)) {
//					continue;
//				}
				ProgramElement programElement=statements[i];
				ASTNode type = null;
				if (programElement instanceof LocalDeclaration )
				{
					  	checkAndAddMultipleLocalDeclaration(statements, i, compilationUnit.statements());

				}
				else if (programElement instanceof AbstractMethodDeclaration )
				{
					type = convert((AbstractMethodDeclaration)programElement);
				}
				else if (programElement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.Statement )
				{
					type = convert((org.eclipse.wst.jsdt.internal.compiler.ast.Statement )programElement);
				}
				else
					throw new RuntimeException(""); //$NON-NLS-1$


				if (type == null) {
//					compilationUnit.setFlags(compilationUnit.getFlags() | ASTNode.MALFORMED);
				} else {
					compilationUnit.statements().add(type);
				}
			}
		}
		compilationUnit.setSourceRange(unit.sourceStart, unit.sourceEnd - unit.sourceStart  + 1);

		int problemLength = unit.compilationResult.problemCount;
		if (problemLength != 0) {
			CategorizedProblem[] resizedProblems = null;
			final CategorizedProblem[] problems = unit.compilationResult.getProblems();
			final int realProblemLength=problems.length;
			if (realProblemLength == problemLength) {
				resizedProblems = problems;
			} else {
				System.arraycopy(problems, 0, (resizedProblems = new CategorizedProblem[realProblemLength]), 0, realProblemLength);
			}
			ASTSyntaxErrorPropagator syntaxErrorPropagator = new ASTSyntaxErrorPropagator(resizedProblems);
			compilationUnit.accept(syntaxErrorPropagator);
			ASTRecoveryPropagator recoveryPropagator =
				new ASTRecoveryPropagator(resizedProblems, unit.compilationResult.recoveryScannerData);
			compilationUnit.accept(recoveryPropagator);
			compilationUnit.setProblems(resizedProblems);
		}
		if (this.resolveBindings) {
			lookupForScopes();
		}
		compilationUnit.initCommentMapper(this.scanner);
		return compilationUnit;
	}

	public Assignment convert(org.eclipse.wst.jsdt.internal.compiler.ast.CompoundAssignment expression) {
		Assignment assignment = new Assignment(this.ast);
		Expression lhs = convert(expression.lhs);
		assignment.setLeftHandSide(lhs);
		int start = lhs.getStartPosition();
		assignment.setSourceRange(start, expression.sourceEnd - start + 1);
		switch (expression.operator) {
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.PLUS :
				assignment.setOperator(Assignment.Operator.PLUS_ASSIGN);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.MINUS :
				assignment.setOperator(Assignment.Operator.MINUS_ASSIGN);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.MULTIPLY :
				assignment.setOperator(Assignment.Operator.TIMES_ASSIGN);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.DIVIDE :
				assignment.setOperator(Assignment.Operator.DIVIDE_ASSIGN);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.AND :
				assignment.setOperator(Assignment.Operator.BIT_AND_ASSIGN);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.OR :
				assignment.setOperator(Assignment.Operator.BIT_OR_ASSIGN);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.XOR :
				assignment.setOperator(Assignment.Operator.BIT_XOR_ASSIGN);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.REMAINDER :
				assignment.setOperator(Assignment.Operator.REMAINDER_ASSIGN);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.LEFT_SHIFT :
				assignment.setOperator(Assignment.Operator.LEFT_SHIFT_ASSIGN);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.RIGHT_SHIFT :
				assignment.setOperator(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.UNSIGNED_RIGHT_SHIFT :
				assignment.setOperator(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN);
				break;
		}
		assignment.setRightHandSide(convert(expression.expression));
		if (this.resolveBindings) {
			recordNodes(assignment, expression);
		}
		return assignment;
	}

	public ConditionalExpression convert(org.eclipse.wst.jsdt.internal.compiler.ast.ConditionalExpression expression) {
		ConditionalExpression conditionalExpression = new ConditionalExpression(this.ast);
		if (this.resolveBindings) {
			recordNodes(conditionalExpression, expression);
		}
		conditionalExpression.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		conditionalExpression.setExpression(convert(expression.condition));
		conditionalExpression.setThenExpression(convert(expression.valueIfTrue));
		conditionalExpression.setElseExpression(convert(expression.valueIfFalse));
		return conditionalExpression;
	}

	public ContinueStatement convert(org.eclipse.wst.jsdt.internal.compiler.ast.ContinueStatement statement)  {
		ContinueStatement continueStatement = new ContinueStatement(this.ast);
		continueStatement.setSourceRange(statement.sourceStart, statement.sourceEnd - statement.sourceStart + 1);
		if (statement.label != null) {
			final SimpleName name = new SimpleName(this.ast);
			name.internalSetIdentifier(new String(statement.label));
			retrieveIdentifierAndSetPositions(statement.sourceStart, statement.sourceEnd, name);
			continueStatement.setLabel(name);
		}
		return continueStatement;
	}

	public DoStatement convert(org.eclipse.wst.jsdt.internal.compiler.ast.DoStatement statement) {
		DoStatement doStatement = new DoStatement(this.ast);
		doStatement.setSourceRange(statement.sourceStart, statement.sourceEnd - statement.sourceStart + 1);
		doStatement.setExpression(convert(statement.condition));
		final Statement action = convert(statement.action);
		if (action == null) return null;
		doStatement.setBody(action);
		return doStatement;
	}

	public NumberLiteral convert(org.eclipse.wst.jsdt.internal.compiler.ast.DoubleLiteral expression) {
		int length = expression.sourceEnd - expression.sourceStart + 1;
		int sourceStart = expression.sourceStart;
		NumberLiteral literal = new NumberLiteral(this.ast);
		literal.internalSetToken(new String(this.compilationUnitSource, sourceStart, length));
		if (this.resolveBindings) {
			this.recordNodes(literal, expression);
		}
		literal.setSourceRange(sourceStart, length);
		removeLeadingAndTrailingCommentsFromLiteral(literal);
		return literal;
	}

	public EmptyStatement convert(org.eclipse.wst.jsdt.internal.compiler.ast.EmptyStatement statement) {
		EmptyStatement emptyStatement = new EmptyStatement(this.ast);
		emptyStatement.setSourceRange(statement.sourceStart, statement.sourceEnd - statement.sourceStart + 1);
		return emptyStatement;
	}


	public EmptyExpression convert(org.eclipse.wst.jsdt.internal.compiler.ast.EmptyExpression expression) {
		EmptyExpression emptyExpression = new EmptyExpression(this.ast);
		emptyExpression.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		return emptyExpression;
	}


	public Expression convert(org.eclipse.wst.jsdt.internal.compiler.ast.EqualExpression expression) {
		InfixExpression infixExpression = new InfixExpression(this.ast);
		if (this.resolveBindings) {
			recordNodes(infixExpression, expression);
		}
		Expression leftExpression = convert(expression.left);
		infixExpression.setLeftOperand(leftExpression);
		infixExpression.setRightOperand(convert(expression.right));
		int startPosition = leftExpression.getStartPosition();
		infixExpression.setSourceRange(startPosition, expression.sourceEnd - startPosition + 1);
		switch ((expression.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorMASK) >> org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorSHIFT) {
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.EQUAL_EQUAL :
				infixExpression.setOperator(InfixExpression.Operator.EQUALS);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.NOT_EQUAL :
				infixExpression.setOperator(InfixExpression.Operator.NOT_EQUALS);
		}
		return infixExpression;

	}

	public Statement convert(org.eclipse.wst.jsdt.internal.compiler.ast.ExplicitConstructorCall statement) {
		Statement newStatement;
		int sourceStart = statement.sourceStart;
		if (statement.isSuperAccess() || statement.isSuper()) {
			SuperConstructorInvocation superConstructorInvocation = new SuperConstructorInvocation(this.ast);
			if (statement.qualification != null) {
				superConstructorInvocation.setExpression(convert(statement.qualification));
			}
			org.eclipse.wst.jsdt.internal.compiler.ast.Expression[] arguments = statement.arguments;
			if (arguments != null) {
				int length = arguments.length;
				for (int i = 0; i < length; i++) {
					superConstructorInvocation.arguments().add(convert(arguments[i]));
				}
			}
			if (statement.typeArguments != null) {
				if (sourceStart > statement.typeArgumentsSourceStart) {
					sourceStart = statement.typeArgumentsSourceStart;
				}
				switch(this.ast.apiLevel) {
					case AST.JLS2_INTERNAL :
						superConstructorInvocation.setFlags(superConstructorInvocation.getFlags() | ASTNode.MALFORMED);
						break;
					case AST.JLS3 :
						for (int i = 0, max = statement.typeArguments.length; i < max; i++) {
							superConstructorInvocation.typeArguments().add(convertType(statement.typeArguments[i]));
						}
						break;
				}
			}
			newStatement = superConstructorInvocation;
		} else {
			ConstructorInvocation constructorInvocation = new ConstructorInvocation(this.ast);
			org.eclipse.wst.jsdt.internal.compiler.ast.Expression[] arguments = statement.arguments;
			if (arguments != null) {
				int length = arguments.length;
				for (int i = 0; i < length; i++) {
					constructorInvocation.arguments().add(convert(arguments[i]));
				}
			}
			if (statement.typeArguments != null) {
				if (sourceStart > statement.typeArgumentsSourceStart) {
					sourceStart = statement.typeArgumentsSourceStart;
				}
				switch(this.ast.apiLevel) {
					case AST.JLS2_INTERNAL :
						constructorInvocation.setFlags(constructorInvocation.getFlags() | ASTNode.MALFORMED);
						break;
					case AST.JLS3 :
						for (int i = 0, max = statement.typeArguments.length; i < max; i++) {
							constructorInvocation.typeArguments().add(convertType(statement.typeArguments[i]));
						}
					break;
				}
			}
			if (statement.qualification != null) {
				// this is an error
				constructorInvocation.setFlags(constructorInvocation.getFlags() | ASTNode.MALFORMED);
			}
			newStatement = constructorInvocation;
		}
		newStatement.setSourceRange(sourceStart, statement.sourceEnd - sourceStart + 1);
		if (this.resolveBindings) {
			recordNodes(newStatement, statement);
		}
		return newStatement;
	}

	public Expression convert(org.eclipse.wst.jsdt.internal.compiler.ast.Expression expression) {
		if (expression==null)
			return null;
		if ((expression.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) != 0) {
			return convertToParenthesizedExpression(expression);
		}
		// switch between all types of expression
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ArrayAllocationExpression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ArrayAllocationExpression) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedAllocationExpression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedAllocationExpression) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ArrayInitializer) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ArrayInitializer) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.PrefixExpression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.PrefixExpression) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.PostfixExpression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.PostfixExpression) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.CompoundAssignment) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.CompoundAssignment) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.Assignment) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.Assignment) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ClassLiteralAccess) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ClassLiteralAccess) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.FalseLiteral) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.FalseLiteral) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.TrueLiteral) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.TrueLiteral) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.NullLiteral) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.NullLiteral) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.DoubleLiteral) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.DoubleLiteral) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.IntLiteralMinValue) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.IntLiteralMinValue) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.IntLiteral) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.IntLiteral) expression);
		}
		if (expression instanceof StringLiteralConcatenation) {
			return convert((StringLiteralConcatenation) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ExtendedStringLiteral) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ExtendedStringLiteral) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteral) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteral) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.AND_AND_Expression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.AND_AND_Expression) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.OR_OR_Expression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.OR_OR_Expression) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.EqualExpression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.EqualExpression) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.InstanceOfExpression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.InstanceOfExpression) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.UnaryExpression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.UnaryExpression) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ConditionalExpression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ConditionalExpression) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.Reference) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.Reference) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.FunctionExpression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.FunctionExpression) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteral) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteral) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteralField) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteralField) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.UndefinedLiteral) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.UndefinedLiteral) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.RegExLiteral) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.RegExLiteral) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ListExpression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ListExpression) expression);
		}
		if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.EmptyExpression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.EmptyExpression) expression);
		}
		return null;
	}

	public StringLiteral convert(org.eclipse.wst.jsdt.internal.compiler.ast.ExtendedStringLiteral expression) {
		expression.computeConstant();
		StringLiteral literal = new StringLiteral(this.ast);
		if (this.resolveBindings) {
			this.recordNodes(literal, expression);
		}
		literal.setLiteralValue(expression.constant.stringValue());
		literal.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		return literal;
	}

	public BooleanLiteral convert(org.eclipse.wst.jsdt.internal.compiler.ast.FalseLiteral expression) {
		final BooleanLiteral literal =  new BooleanLiteral(this.ast);
		literal.setBooleanValue(false);
		if (this.resolveBindings) {
			this.recordNodes(literal, expression);
		}
		literal.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		return literal;
	}

	public Expression convert(org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference reference) {
		if (reference.receiver.isSuper()) {
			final SuperFieldAccess superFieldAccess = new SuperFieldAccess(this.ast);
			if (this.resolveBindings) {
				recordNodes(superFieldAccess, reference);
			}
			final SimpleName simpleName = new SimpleName(this.ast);
			simpleName.internalSetIdentifier(new String(reference.token));
			int sourceStart = (int)(reference.nameSourcePosition>>>32);
			int length = (int)(reference.nameSourcePosition & 0xFFFFFFFF) - sourceStart + 1;
			simpleName.setSourceRange(sourceStart, length);
			superFieldAccess.setName(simpleName);
			if (this.resolveBindings) {
				recordNodes(simpleName, reference);
			}
			superFieldAccess.setSourceRange(reference.receiver.sourceStart, reference.sourceEnd - reference.receiver.sourceStart + 1);
			return superFieldAccess;
		} else {
			final FieldAccess fieldAccess = new FieldAccess(this.ast);
			if (this.resolveBindings) {
				recordNodes(fieldAccess, reference);
			}
			Expression receiver = convert(reference.receiver);
			fieldAccess.setExpression(receiver);
			final SimpleName simpleName = new SimpleName(this.ast);
			simpleName.internalSetIdentifier(new String(reference.token));
			int sourceStart = (int)(reference.nameSourcePosition>>>32);
			int length = (int)(reference.nameSourcePosition & 0xFFFFFFFF) - sourceStart + 1;
			simpleName.setSourceRange(sourceStart, length);
			fieldAccess.setName(simpleName);
			if (this.resolveBindings) {
				recordNodes(simpleName, reference);
			}
			fieldAccess.setSourceRange(receiver.getStartPosition(), reference.sourceEnd - receiver.getStartPosition() + 1);
			return fieldAccess;
		}
	}

	public Statement convert(ForeachStatement statement) {
		switch(this.ast.apiLevel) {
			case AST.JLS2_INTERNAL :
				return createFakeEmptyStatement(statement);
			case AST.JLS3 :
				EnhancedForStatement enhancedForStatement = new EnhancedForStatement(this.ast);
				enhancedForStatement.setParameter(convertToSingleVariableDeclaration(statement.elementVariable));
				org.eclipse.wst.jsdt.internal.compiler.ast.Expression collection = statement.collection;
				if (collection == null) return null;				enhancedForStatement.setExpression(convert(collection));
				final Statement action = convert(statement.action);
				if (action == null) return null;
				enhancedForStatement.setBody(action);
				int start = statement.sourceStart;
				int end = statement.sourceEnd;
				enhancedForStatement.setSourceRange(start, end - start + 1);
				return enhancedForStatement;
			default:
				return createFakeEmptyStatement(statement);
		}
	}

	public ForStatement convert(org.eclipse.wst.jsdt.internal.compiler.ast.ForStatement statement) {
		ForStatement forStatement = new ForStatement(this.ast);
		forStatement.setSourceRange(statement.sourceStart, statement.sourceEnd - statement.sourceStart + 1);
		org.eclipse.wst.jsdt.internal.compiler.ast.Statement[] initializations = statement.initializations;
		if (initializations != null) {
			// we know that we have at least one initialization
			if (initializations[0] instanceof org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration) {
				VariableDeclarationExpression variableDeclarationExpression = convertToVariableDeclarationExpression((org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration) initializations[0]);
				int initializationsLength = initializations.length;
				for (int i = 1; i < initializationsLength; i++) {
					variableDeclarationExpression.fragments().add(convertToVariableDeclarationFragment((org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration)initializations[i]));
				}
				if (initializationsLength != 1) {
					int start = variableDeclarationExpression.getStartPosition();
					int end = ((org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration) initializations[initializationsLength - 1]).declarationSourceEnd;
					variableDeclarationExpression.setSourceRange(start, end - start + 1);
				}
				forStatement.initializers().add(variableDeclarationExpression);
			} else {
				int initializationsLength = initializations.length;
				for (int i = 0; i < initializationsLength; i++) {
					Expression initializer = convertToExpression(initializations[i]);
					if (initializer != null) {
						forStatement.initializers().add(initializer);
					} else {
						forStatement.setFlags(forStatement.getFlags() | ASTNode.MALFORMED);
					}
				}
			}
		}
		if (statement.condition != null) {
			forStatement.setExpression(convert(statement.condition));
		}
		org.eclipse.wst.jsdt.internal.compiler.ast.Statement[] increments = statement.increments;
		if (increments != null) {
			int incrementsLength = increments.length;
			for (int i = 0; i < incrementsLength; i++) {
				forStatement.updaters().add(convertToExpression(increments[i]));
			}
		}
		final Statement action = convert(statement.action);
		if (action == null) return null;
		forStatement.setBody(action);
		return forStatement;
	}


	public ForInStatement convert(org.eclipse.wst.jsdt.internal.compiler.ast.ForInStatement statement) {
		ForInStatement forInStatement = new ForInStatement(this.ast);
		forInStatement.setSourceRange(statement.sourceStart, statement.sourceEnd - statement.sourceStart + 1);

		Statement iterationVariable = convert(statement.iterationVariable);
		forInStatement.setIterationVariable(iterationVariable);

		Expression collection  = convert(statement.collection);
		forInStatement.setCollection(collection);

		final Statement action = convert(statement.action);
		if (action == null) return null;
		forInStatement.setBody(action);
		return forInStatement;
	}
	public IfStatement convert(org.eclipse.wst.jsdt.internal.compiler.ast.IfStatement statement) {
		IfStatement ifStatement = new IfStatement(this.ast);
		ifStatement.setSourceRange(statement.sourceStart, statement.sourceEnd - statement.sourceStart + 1);
		ifStatement.setExpression(convert(statement.condition));
		final Statement thenStatement = convert(statement.thenStatement);
		if (thenStatement == null) return null;
		ifStatement.setThenStatement(thenStatement);
		org.eclipse.wst.jsdt.internal.compiler.ast.Statement statement2 = statement.elseStatement;
		if (statement2 != null) {
			final Statement elseStatement = convert(statement2);
			if (elseStatement != null) {
				ifStatement.setElseStatement(elseStatement);
			}
		}
		return ifStatement;
	}

	public InstanceofExpression convert(org.eclipse.wst.jsdt.internal.compiler.ast.InstanceOfExpression expression) {
		InstanceofExpression instanceOfExpression = new InstanceofExpression(this.ast);
		if (this.resolveBindings) {
			recordNodes(instanceOfExpression, expression);
		}
		Expression leftExpression = convert(expression.expression);
		instanceOfExpression.setLeftOperand(leftExpression);
		final Type convertType = convertType(expression.type);
		instanceOfExpression.setRightOperand(convertType);
		int startPosition = leftExpression.getStartPosition();
		int sourceEnd = convertType.getStartPosition() + convertType.getLength() - 1;
		instanceOfExpression.setSourceRange(startPosition, sourceEnd - startPosition + 1);
		return instanceOfExpression;
	}

	public NumberLiteral convert(org.eclipse.wst.jsdt.internal.compiler.ast.IntLiteral expression) {
		int length = expression.sourceEnd - expression.sourceStart + 1;
		int sourceStart = expression.sourceStart;
		final NumberLiteral literal = new NumberLiteral(this.ast);
		literal.internalSetToken(new String(this.compilationUnitSource, sourceStart, length));
		if (this.resolveBindings) {
			this.recordNodes(literal, expression);
		}
		literal.setSourceRange(sourceStart, length);
		removeLeadingAndTrailingCommentsFromLiteral(literal);
		return literal;
	}

	public NumberLiteral convert(org.eclipse.wst.jsdt.internal.compiler.ast.IntLiteralMinValue expression) {
		int length = expression.sourceEnd - expression.sourceStart + 1;
		int sourceStart = expression.sourceStart;
		NumberLiteral literal = new NumberLiteral(this.ast);
		literal.internalSetToken(new String(this.compilationUnitSource, sourceStart, length));
		if (this.resolveBindings) {
			this.recordNodes(literal, expression);
		}
		literal.setSourceRange(sourceStart, length);
		removeLeadingAndTrailingCommentsFromLiteral(literal);
		return literal;
	}

	public void convert(org.eclipse.wst.jsdt.internal.compiler.ast.Javadoc javadoc, BodyDeclaration bodyDeclaration) {
		if (bodyDeclaration.getJavadoc() == null) {
			if (javadoc != null) {
				if (this.commentMapper == null || !this.commentMapper.hasSameTable(this.commentsTable)) {
					this.commentMapper = new DefaultCommentMapper(this.commentsTable);
				}
				Comment comment = this.commentMapper.getComment(javadoc.sourceStart);
				if (comment != null && comment.isDocComment() && comment.getParent() == null) {
					JSdoc docComment = (JSdoc) comment;
					if (this.resolveBindings) {
						recordNodes(docComment, javadoc);
						// resolve member and method references binding
						Iterator tags = docComment.tags().listIterator();
						while (tags.hasNext()) {
							recordNodes(javadoc, (TagElement) tags.next());
						}
					}
					bodyDeclaration.setJavadoc(docComment);
				}
			}
		}
	}

	public void convert(org.eclipse.wst.jsdt.internal.compiler.ast.Javadoc javadoc, VariableDeclarationStatement variable) {
		if (variable.getJavadoc() == null) {
			if (javadoc != null) {
				if (this.commentMapper == null || !this.commentMapper.hasSameTable(this.commentsTable)) {
					this.commentMapper = new DefaultCommentMapper(this.commentsTable);
				}
				Comment comment = this.commentMapper.getComment(javadoc.sourceStart);
				if (comment != null && comment.isDocComment() && comment.getParent() == null) {
					JSdoc docComment = (JSdoc) comment;
					if (this.resolveBindings) {
						recordNodes(docComment, javadoc);
						// resolve member and method references binding
						Iterator tags = docComment.tags().listIterator();
						while (tags.hasNext()) {
							recordNodes(javadoc, (TagElement) tags.next());
						}
					}
					variable.setJavadoc(docComment);
				}
			}
		}
	}

	public void convert(org.eclipse.wst.jsdt.internal.compiler.ast.Javadoc javadoc, PackageDeclaration packageDeclaration) {
		if (ast.apiLevel == AST.JLS3 && packageDeclaration.getJavadoc() == null) {
			if (javadoc != null) {
				if (this.commentMapper == null || !this.commentMapper.hasSameTable(this.commentsTable)) {
					this.commentMapper = new DefaultCommentMapper(this.commentsTable);
				}
				Comment comment = this.commentMapper.getComment(javadoc.sourceStart);
				if (comment != null && comment.isDocComment() && comment.getParent() == null) {
					JSdoc docComment = (JSdoc) comment;
					if (this.resolveBindings) {
						recordNodes(docComment, javadoc);
						// resolve member and method references binding
						Iterator tags = docComment.tags().listIterator();
						while (tags.hasNext()) {
							recordNodes(javadoc, (TagElement) tags.next());
						}
					}
					packageDeclaration.setJavadoc(docComment);
				}
			}
		}
	}

	public LabeledStatement convert(org.eclipse.wst.jsdt.internal.compiler.ast.LabeledStatement statement) {
		LabeledStatement labeledStatement = new LabeledStatement(this.ast);
		final int sourceStart = statement.sourceStart;
		labeledStatement.setSourceRange(sourceStart, statement.sourceEnd - sourceStart + 1);
		Statement body = convert(statement.statement);
		if (body == null) return null;
		labeledStatement.setBody(body);
		final SimpleName name = new SimpleName(this.ast);
		name.internalSetIdentifier(new String(statement.label));
		name.setSourceRange(sourceStart, statement.labelEnd - sourceStart + 1);
		labeledStatement.setLabel(name);
		return labeledStatement;
	}

	public ListExpression convert(org.eclipse.wst.jsdt.internal.compiler.ast.ListExpression expression) {
		ListExpression listExpression = new ListExpression(this.ast);
		if (this.resolveBindings) {
			recordNodes(listExpression, expression);
		}
		listExpression.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		org.eclipse.wst.jsdt.internal.compiler.ast.Expression[] expressions = expression.expressions;
		if (expressions != null) {
			int length = expressions.length;
			for (int i = 0; i < length; i++) {
				Expression expr = convert(expressions[i]);
				if (this.resolveBindings) {
					recordNodes(expr, expressions[i]);
				}
				listExpression.expressions().add(expr);
			}
		}
		return listExpression;
	}

	public Expression convert(MessageSend expression) {
		// will return a FunctionInvocation or a SuperMethodInvocation or
		Expression expr;
		int sourceStart = expression.sourceStart;
		if (expression.isSuperAccess()) {
			// returns a SuperMethodInvocation
			final SuperMethodInvocation superMethodInvocation = new SuperMethodInvocation(this.ast);
			if (this.resolveBindings) {
				recordNodes(superMethodInvocation, expression);
			}
			final SimpleName name = new SimpleName(this.ast);
			name.internalSetIdentifier(new String(expression.selector));
			int nameSourceStart =  (int) (expression.nameSourcePosition >>> 32);
			int nameSourceLength = ((int) expression.nameSourcePosition) - nameSourceStart + 1;
			name.setSourceRange(nameSourceStart, nameSourceLength);
			if (this.resolveBindings) {
				recordNodes(name, expression);
			}
			superMethodInvocation.setName(name);
			
			org.eclipse.wst.jsdt.internal.compiler.ast.Expression[] arguments = expression.arguments;
			if (arguments != null) {
				int argumentsLength = arguments.length;
				for (int i = 0; i < argumentsLength; i++) {
					Expression expri = convert(arguments[i]);
					if (this.resolveBindings) {
						recordNodes(expri, arguments[i]);
					}
					superMethodInvocation.arguments().add(expri);
				}
			}
			expr = superMethodInvocation;
		} else {
			// returns a FunctionInvocation
			final FunctionInvocation methodInvocation = new FunctionInvocation(this.ast);
			if (this.resolveBindings) {
				recordNodes(methodInvocation, expression);
			}
			if (expression.selector!=null)
			{
				final SimpleName name = new SimpleName(this.ast);
				name.internalSetIdentifier(new String(expression.selector));
				int nameSourceStart =  (int) (expression.nameSourcePosition >>> 32);
				int nameSourceLength = ((int) expression.nameSourcePosition) - nameSourceStart + 1;
				name.setSourceRange(nameSourceStart, nameSourceLength);
				methodInvocation.setName(name);
				if (this.resolveBindings) {
					recordNodes(name, expression);
				}

			}
			org.eclipse.wst.jsdt.internal.compiler.ast.Expression[] arguments = expression.arguments;
			if (arguments != null) {
				int argumentsLength = arguments.length;
				for (int i = 0; i < argumentsLength; i++) {
					Expression expri = convert(arguments[i]);
					if (this.resolveBindings) {
						recordNodes(expri, arguments[i]);
					}
					methodInvocation.arguments().add(expri);
				}
			}
			Expression qualifier = null;
			org.eclipse.wst.jsdt.internal.compiler.ast.Expression receiver = expression.receiver;
			if (receiver instanceof MessageSend) {
				if ((receiver.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) != 0) {
					qualifier = convertToParenthesizedExpression(receiver);
				} else {
					qualifier = convert((MessageSend) receiver);
				}
			} else {
				qualifier = convert(receiver);
			}
			if (qualifier instanceof Name && this.resolveBindings) {
				recordNodes(qualifier, receiver);
			}
			methodInvocation.setExpression(qualifier);
			if (qualifier != null) {
				sourceStart = qualifier.getStartPosition();
			}
			expr = methodInvocation;
		}
		expr.setSourceRange(sourceStart, expression.sourceEnd - sourceStart + 1);
		removeTrailingCommentFromExpressionEndingWithAParen(expr);
		return expr;
	}


	public Name convert(org.eclipse.wst.jsdt.internal.compiler.ast.NameReference reference) {
		if (reference instanceof org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference) reference);
		} else {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference) reference);
		}
	}

	public InfixExpression convert(StringLiteralConcatenation expression) {
		expression.computeConstant();
		final InfixExpression infixExpression = new InfixExpression(this.ast);
		infixExpression.setOperator(InfixExpression.Operator.PLUS);
		org.eclipse.wst.jsdt.internal.compiler.ast.Expression[] stringLiterals = expression.literals;
		infixExpression.setLeftOperand(convert(stringLiterals[0]));
		infixExpression.setRightOperand(convert(stringLiterals[1]));
		for (int i = 2; i < expression.counter; i++) {
			infixExpression.extendedOperands().add(convert(stringLiterals[i]));
		}
		if (this.resolveBindings) {
			this.recordNodes(infixExpression, expression);
		}
		infixExpression.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		return infixExpression;
	}


	public NullLiteral convert(org.eclipse.wst.jsdt.internal.compiler.ast.NullLiteral expression) {
		final NullLiteral literal = new NullLiteral(this.ast);
		if (this.resolveBindings) {
			this.recordNodes(literal, expression);
		}
		literal.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		return literal;
	}

	public UndefinedLiteral convert(org.eclipse.wst.jsdt.internal.compiler.ast.UndefinedLiteral expression) {
		final UndefinedLiteral literal = new UndefinedLiteral(this.ast);
		if (this.resolveBindings) {
			this.recordNodes(literal, expression);
		}
		literal.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		return literal;
	}

	public Expression convert(org.eclipse.wst.jsdt.internal.compiler.ast.OR_OR_Expression expression) {
		InfixExpression infixExpression = new InfixExpression(this.ast);
		infixExpression.setOperator(InfixExpression.Operator.CONDITIONAL_OR);
		if (this.resolveBindings) {
			this.recordNodes(infixExpression, expression);
		}
		final int expressionOperatorID = (expression.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorMASK) >> org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorSHIFT;
		if (expression.left instanceof org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression
				&& ((expression.left.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) == 0)) {
			// create an extended string literal equivalent => use the extended operands list
			infixExpression.extendedOperands().add(convert(expression.right));
			org.eclipse.wst.jsdt.internal.compiler.ast.Expression leftOperand = expression.left;
			org.eclipse.wst.jsdt.internal.compiler.ast.Expression rightOperand = null;
			do {
				rightOperand = ((org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression) leftOperand).right;
				if ((((leftOperand.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorMASK) >> org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorSHIFT) != expressionOperatorID
							&& ((leftOperand.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) == 0))
					 || ((rightOperand instanceof org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression
				 			&& ((rightOperand.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorMASK) >> org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorSHIFT) != expressionOperatorID)
							&& ((rightOperand.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) == 0))) {
				 	List extendedOperands = infixExpression.extendedOperands();
				 	InfixExpression temp = new InfixExpression(this.ast);
					if (this.resolveBindings) {
						this.recordNodes(temp, expression);
					}
				 	temp.setOperator(getOperatorFor(expressionOperatorID));
				 	Expression leftSide = convert(leftOperand);
					temp.setLeftOperand(leftSide);
					temp.setSourceRange(leftSide.getStartPosition(), leftSide.getLength());
					int size = extendedOperands.size();
				 	for (int i = 0; i < size - 1; i++) {
				 		Expression expr = temp;
				 		temp = new InfixExpression(this.ast);

						if (this.resolveBindings) {
							this.recordNodes(temp, expression);
						}
				 		temp.setLeftOperand(expr);
					 	temp.setOperator(getOperatorFor(expressionOperatorID));
						temp.setSourceRange(expr.getStartPosition(), expr.getLength());
				 	}
				 	infixExpression = temp;
				 	for (int i = 0; i < size; i++) {
				 		Expression extendedOperand = (Expression) extendedOperands.remove(size - 1 - i);
				 		temp.setRightOperand(extendedOperand);
				 		int startPosition = temp.getLeftOperand().getStartPosition();
				 		temp.setSourceRange(startPosition, extendedOperand.getStartPosition() + extendedOperand.getLength() - startPosition);
				 		if (temp.getLeftOperand().getNodeType() == ASTNode.INFIX_EXPRESSION) {
				 			temp = (InfixExpression) temp.getLeftOperand();
				 		}
				 	}
					int startPosition = infixExpression.getLeftOperand().getStartPosition();
					infixExpression.setSourceRange(startPosition, expression.sourceEnd - startPosition + 1);
					if (this.resolveBindings) {
						this.recordNodes(infixExpression, expression);
					}
					return infixExpression;
				}
				infixExpression.extendedOperands().add(0, convert(rightOperand));
				leftOperand = ((org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression) leftOperand).left;
			} while (leftOperand instanceof org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression && ((leftOperand.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) == 0));
			Expression leftExpression = convert(leftOperand);
			infixExpression.setLeftOperand(leftExpression);
			infixExpression.setRightOperand((Expression)infixExpression.extendedOperands().remove(0));
			int startPosition = leftExpression.getStartPosition();
			infixExpression.setSourceRange(startPosition, expression.sourceEnd - startPosition + 1);
			return infixExpression;
		}
		Expression leftExpression = convert(expression.left);
		infixExpression.setLeftOperand(leftExpression);
		infixExpression.setRightOperand(convert(expression.right));
		infixExpression.setOperator(InfixExpression.Operator.CONDITIONAL_OR);
		int startPosition = leftExpression.getStartPosition();
		infixExpression.setSourceRange(startPosition, expression.sourceEnd - startPosition + 1);
		return infixExpression;
	}

	public PostfixExpression convert(org.eclipse.wst.jsdt.internal.compiler.ast.PostfixExpression expression) {
		final PostfixExpression postfixExpression = new PostfixExpression(this.ast);
		if (this.resolveBindings) {
			recordNodes(postfixExpression, expression);
		}
		postfixExpression.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		postfixExpression.setOperand(convert(expression.lhs));
		switch (expression.operator) {
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.PLUS :
				postfixExpression.setOperator(PostfixExpression.Operator.INCREMENT);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.MINUS :
				postfixExpression.setOperator(PostfixExpression.Operator.DECREMENT);
				break;
		}
		return postfixExpression;
	}

	public PrefixExpression convert(org.eclipse.wst.jsdt.internal.compiler.ast.PrefixExpression expression) {
		final PrefixExpression prefixExpression = new PrefixExpression(this.ast);
		if (this.resolveBindings) {
			recordNodes(prefixExpression, expression);
		}
		prefixExpression.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		prefixExpression.setOperand(convert(expression.lhs));
		switch (expression.operator) {
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.PLUS :
				prefixExpression.setOperator(PrefixExpression.Operator.INCREMENT);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.MINUS :
				prefixExpression.setOperator(PrefixExpression.Operator.DECREMENT);
				break;
		}
		return prefixExpression;
	}

	public Expression convert(org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedAllocationExpression allocation) {
		final ClassInstanceCreation classInstanceCreation = new ClassInstanceCreation(this.ast);
		if (allocation.enclosingInstance != null) {
			classInstanceCreation.setExpression(convert(allocation.enclosingInstance));
		}
		if (allocation.member != null) {
			classInstanceCreation.setMember(convert(allocation.member));
		}
		if (allocation.type!=null)
		{
			switch(this.ast.apiLevel) {
			case AST.JLS2_INTERNAL :
					classInstanceCreation.internalSetName(convert(allocation.type));
				break;
			case AST.JLS3 :
				classInstanceCreation.setType(convertType(allocation.type));
			}
		}
		org.eclipse.wst.jsdt.internal.compiler.ast.Expression[] arguments = allocation.arguments;
		if (arguments != null) {
			int length = arguments.length;
			for (int i = 0; i < length; i++) {
				Expression argument = convert(arguments[i]);
				if (this.resolveBindings) {
					recordNodes(argument, arguments[i]);
				}
				classInstanceCreation.arguments().add(argument);
			}
		}
		if (allocation.anonymousType != null) {
			int declarationSourceStart = allocation.sourceStart;
			classInstanceCreation.setSourceRange(declarationSourceStart, allocation.anonymousType.bodyEnd - declarationSourceStart + 1);
			final AnonymousClassDeclaration anonymousClassDeclaration = new AnonymousClassDeclaration(this.ast);
			int start = retrieveStartBlockPosition(allocation.anonymousType.sourceEnd, allocation.anonymousType.bodyEnd);
			anonymousClassDeclaration.setSourceRange(start, allocation.anonymousType.bodyEnd - start + 1);
			classInstanceCreation.setAnonymousClassDeclaration(anonymousClassDeclaration);
			buildBodyDeclarations(allocation.anonymousType, anonymousClassDeclaration);
			if (this.resolveBindings) {
				recordNodes(classInstanceCreation, allocation.anonymousType);
				recordNodes(anonymousClassDeclaration, allocation.anonymousType);
				anonymousClassDeclaration.resolveBinding();
			}
			return classInstanceCreation;
		} else {
			final int start = allocation.sourceStart;
			classInstanceCreation.setSourceRange(start, allocation.sourceEnd - start + 1);
			if (this.resolveBindings) {
				recordNodes(classInstanceCreation, allocation);
			}
			removeTrailingCommentFromExpressionEndingWithAParen(classInstanceCreation);
			return classInstanceCreation;
		}
	}

	public Name convert(org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference nameReference) {
		return setQualifiedNameNameAndSourceRanges(nameReference.tokens, nameReference.sourcePositions, nameReference);
	}

	public ThisExpression convert(org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedThisReference reference) {
		final ThisExpression thisExpression = new ThisExpression(this.ast);
		thisExpression.setSourceRange(reference.sourceStart, reference.sourceEnd - reference.sourceStart + 1);
		thisExpression.setQualifier(convert(reference.qualification));
		if (this.resolveBindings) {
			recordNodes(thisExpression, reference);
			recordPendingThisExpressionScopeResolution(thisExpression);
		}
		return thisExpression;
	}

	public Expression convert(org.eclipse.wst.jsdt.internal.compiler.ast.Reference reference) {
		if (reference instanceof org.eclipse.wst.jsdt.internal.compiler.ast.NameReference) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.NameReference) reference);
		}
		if (reference instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ThisReference) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ThisReference) reference);
		}
		if (reference instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ArrayReference) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ArrayReference) reference);
		}
		if (reference instanceof org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference) reference);
		}
		return null; // cannot be reached
	}

	public ReturnStatement convert(org.eclipse.wst.jsdt.internal.compiler.ast.ReturnStatement statement) {
		final ReturnStatement returnStatement = new ReturnStatement(this.ast);
		returnStatement.setSourceRange(statement.sourceStart, statement.sourceEnd - statement.sourceStart + 1);
		if (statement.expression != null) {
			returnStatement.setExpression(convert(statement.expression));
		}
		return returnStatement;
	}

	public SimpleName convert(org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference nameReference) {
		final SimpleName name = new SimpleName(this.ast);
		name.internalSetIdentifier(new String(nameReference.token));
		if (this.resolveBindings) {
			recordNodes(name, nameReference);
		}
		name.setSourceRange(nameReference.sourceStart, nameReference.sourceEnd - nameReference.sourceStart + 1);
		return name;
	}

	public Statement convert(org.eclipse.wst.jsdt.internal.compiler.ast.Statement statement) {
		if (statement instanceof ForeachStatement) {
			return convert((ForeachStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration) {
			return convertToVariableDeclarationStatement((org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration)statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.Block) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.Block) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.BreakStatement) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.BreakStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ContinueStatement) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ContinueStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.CaseStatement) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.CaseStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.DoStatement) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.DoStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.EmptyStatement) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.EmptyStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ExplicitConstructorCall) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ExplicitConstructorCall) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ForStatement) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ForStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ForInStatement) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ForInStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.IfStatement) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.IfStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.LabeledStatement) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.LabeledStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ReturnStatement) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ReturnStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.SwitchStatement) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.SwitchStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.ThrowStatement) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.ThrowStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.TryStatement) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.TryStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) {
			ASTNode result = convert((org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) statement);
			if (result == null) {
				return createFakeEmptyStatement(statement);
			}
					TypeDeclaration typeDeclaration = (TypeDeclaration) result;
 						TypeDeclarationStatement typeDeclarationStatement = new TypeDeclarationStatement(this.ast);
						typeDeclarationStatement.setDeclaration(typeDeclaration);
						switch(this.ast.apiLevel) {
							case AST.JLS2_INTERNAL :
								TypeDeclaration typeDecl = typeDeclarationStatement.internalGetTypeDeclaration();
								typeDeclarationStatement.setSourceRange(typeDecl.getStartPosition(), typeDecl.getLength());
								break;
							case AST.JLS3 :
								AbstractTypeDeclaration typeDeclAST3 = typeDeclarationStatement.getDeclaration();
								typeDeclarationStatement.setSourceRange(typeDeclAST3.getStartPosition(), typeDeclAST3.getLength());
								break;
						}
						return typeDeclarationStatement;

		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.WhileStatement) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.WhileStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.WithStatement) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.WithStatement) statement);
		}
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.Expression) {
			org.eclipse.wst.jsdt.internal.compiler.ast.Expression statement2 = (org.eclipse.wst.jsdt.internal.compiler.ast.Expression) statement;
			final Expression expr = convert(  statement2);
			final ExpressionStatement stmt = new ExpressionStatement(this.ast);
			stmt.setExpression(expr);
			int sourceStart = expr.getStartPosition();
			int sourceEnd = statement2.statementEnd;
			if (sourceEnd==-1)
				sourceEnd=statement2.sourceEnd;
			stmt.setSourceRange(sourceStart, sourceEnd - sourceStart + 1);
			return stmt;
		}
		return createFakeEmptyStatement(statement);
	}

	public Expression convert(org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteral expression) {
		if (expression instanceof StringLiteralConcatenation) {
			return convert((StringLiteralConcatenation) expression);
		}
		int length = expression.sourceEnd - expression.sourceStart + 1;
		int sourceStart = expression.sourceStart;
		StringLiteral literal = new StringLiteral(this.ast);
		if (this.resolveBindings) {
			this.recordNodes(literal, expression);
		}
		literal.internalSetEscapedValue(new String(this.compilationUnitSource, sourceStart, length));
		literal.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		return literal;
	}

	public SwitchStatement convert(org.eclipse.wst.jsdt.internal.compiler.ast.SwitchStatement statement) {
		SwitchStatement switchStatement = new SwitchStatement(this.ast);
		switchStatement.setSourceRange(statement.sourceStart, statement.sourceEnd - statement.sourceStart + 1);
		switchStatement.setExpression(convert(statement.expression));
		org.eclipse.wst.jsdt.internal.compiler.ast.Statement[] statements = statement.statements;
		if (statements != null) {
			int statementsLength = statements.length;
			for (int i = 0; i < statementsLength; i++) {
				if (statements[i] instanceof org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration) {
					checkAndAddMultipleLocalDeclaration(statements, i, switchStatement.statements());
				} else {
					final Statement currentStatement = convert(statements[i]);
					if (currentStatement != null) {
						switchStatement.statements().add(currentStatement);
					}
				}
			}
		}
		return switchStatement;
	}

	public Expression convert(org.eclipse.wst.jsdt.internal.compiler.ast.ThisReference reference) {
		if (reference.isImplicitThis()) {
			// There is no source associated with an implicit this
			return null;
		} else if (reference instanceof org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedThisReference) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedThisReference) reference);
		}  else {
			ThisExpression thisExpression = new ThisExpression(this.ast);
			thisExpression.setSourceRange(reference.sourceStart, reference.sourceEnd - reference.sourceStart + 1);
			if (this.resolveBindings) {
				recordNodes(thisExpression, reference);
				recordPendingThisExpressionScopeResolution(thisExpression);
			}
			return thisExpression;
		}
	}

	public ThrowStatement convert(org.eclipse.wst.jsdt.internal.compiler.ast.ThrowStatement statement) {
		final ThrowStatement throwStatement = new ThrowStatement(this.ast);
		throwStatement.setSourceRange(statement.sourceStart, statement.sourceEnd - statement.sourceStart + 1);
		throwStatement.setExpression(convert(statement.exception));
		return throwStatement;
	}

	public BooleanLiteral convert(org.eclipse.wst.jsdt.internal.compiler.ast.TrueLiteral expression) {
		final BooleanLiteral literal = new BooleanLiteral(this.ast);
		literal.setBooleanValue(true);
		if (this.resolveBindings) {
			this.recordNodes(literal, expression);
		}
		literal.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		return literal;
	}

	public TryStatement convert(org.eclipse.wst.jsdt.internal.compiler.ast.TryStatement statement) {
		final TryStatement tryStatement = new TryStatement(this.ast);
		tryStatement.setSourceRange(statement.sourceStart, statement.sourceEnd - statement.sourceStart + 1);

		tryStatement.setBody(convert(statement.tryBlock));
		org.eclipse.wst.jsdt.internal.compiler.ast.Argument[] catchArguments = statement.catchArguments;
		if (catchArguments != null) {
			int catchArgumentsLength = catchArguments.length;
			org.eclipse.wst.jsdt.internal.compiler.ast.Block[] catchBlocks = statement.catchBlocks;
			int start = statement.tryBlock.sourceEnd;
			for (int i = 0; i < catchArgumentsLength; i++) {
				CatchClause catchClause = new CatchClause(this.ast);
				int catchClauseSourceStart = retrieveStartingCatchPosition(start, catchArguments[i].sourceStart);
				catchClause.setSourceRange(catchClauseSourceStart, catchBlocks[i].sourceEnd - catchClauseSourceStart + 1);
				catchClause.setBody(convert(catchBlocks[i]));
				catchClause.setException(convert(catchArguments[i]));
				tryStatement.catchClauses().add(catchClause);
				start = catchBlocks[i].sourceEnd;
			}
		}
		if (statement.finallyBlock != null) {
			tryStatement.setFinally(convert(statement.finallyBlock));
		}
		return tryStatement;
	}

	public ASTNode convert(org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration typeDeclaration) {
		checkCanceled();
		TypeDeclaration typeDecl = new TypeDeclaration(this.ast);
		if (typeDeclaration.modifiersSourceStart != -1) {
			setModifiers(typeDecl, typeDeclaration);
		}
		final SimpleName typeName = new SimpleName(this.ast);
		typeName.internalSetIdentifier(new String(typeDeclaration.name));
		typeName.setSourceRange(typeDeclaration.sourceStart, typeDeclaration.sourceEnd - typeDeclaration.sourceStart + 1);
		typeDecl.setName(typeName);
		typeDecl.setSourceRange(typeDeclaration.declarationSourceStart, typeDeclaration.bodyEnd - typeDeclaration.declarationSourceStart + 1);

		// need to set the superclass and super interfaces here since we cannot distinguish them at
		// the type references level.
		if (typeDeclaration.superclass != null) {
			switch(this.ast.apiLevel) {
				case AST.JLS2_INTERNAL :
					typeDecl.internalSetSuperclass(convert(typeDeclaration.superclass));
					break;
				case AST.JLS3 :
					typeDecl.setSuperclassType(convertType(typeDeclaration.superclass));
					break;
			}
		}
		
		buildBodyDeclarations(typeDeclaration, typeDecl);
		if (this.resolveBindings) {
			recordNodes(typeDecl, typeDeclaration);
			recordNodes(typeName, typeDeclaration);
			typeDecl.resolveBinding();
		}
		return typeDecl;
	}

	public Name convert(org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference typeReference) {
		char[][] typeName = typeReference.getTypeName();
		int length = typeName.length;
		if (length > 1) {
			// QualifiedName
			org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference qualifiedTypeReference = (org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference) typeReference;
			final long[] positions = qualifiedTypeReference.sourcePositions;
			return setQualifiedNameNameAndSourceRanges(typeName, positions, typeReference);
		} else {
			final SimpleName name = new SimpleName(this.ast);
			name.internalSetIdentifier(new String(typeName[0]));
			name.setSourceRange(typeReference.sourceStart, typeReference.sourceEnd - typeReference.sourceStart + 1);
			name.index = 1;
			if (this.resolveBindings) {
				recordNodes(name, typeReference);
			}
			return name;
		}
	}

	public PrefixExpression convert(org.eclipse.wst.jsdt.internal.compiler.ast.UnaryExpression expression) {
		final PrefixExpression prefixExpression = new PrefixExpression(this.ast);
		if (this.resolveBindings) {
			this.recordNodes(prefixExpression, expression);
		}
		prefixExpression.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		prefixExpression.setOperand(convert(expression.expression));
		switch ((expression.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorMASK) >> org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OperatorSHIFT) {
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.PLUS :
				prefixExpression.setOperator(PrefixExpression.Operator.PLUS);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.MINUS :
				prefixExpression.setOperator(PrefixExpression.Operator.MINUS);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.NOT :
				prefixExpression.setOperator(PrefixExpression.Operator.NOT);
				break;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.TWIDDLE :
				prefixExpression.setOperator(PrefixExpression.Operator.COMPLEMENT);
		}
		return prefixExpression;
	}

	public WhileStatement convert(org.eclipse.wst.jsdt.internal.compiler.ast.WhileStatement statement) {
		final WhileStatement whileStatement = new WhileStatement(this.ast);
		whileStatement.setSourceRange(statement.sourceStart, statement.sourceEnd - statement.sourceStart + 1);
		whileStatement.setExpression(convert(statement.condition));
		final Statement action = convert(statement.action);
		if (action == null) return null;
		whileStatement.setBody(action);
		return whileStatement;
	}


	public WithStatement convert(org.eclipse.wst.jsdt.internal.compiler.ast.WithStatement statement) {
		final WithStatement withStatement = new WithStatement(this.ast);
		withStatement.setSourceRange(statement.sourceStart, statement.sourceEnd - statement.sourceStart + 1);
		withStatement.setExpression(convert(statement.condition));
		final Statement action = convert(statement.action);
		if (action == null) return null;
		withStatement.setBody(action);
		return withStatement;
	}

	public ImportDeclaration convertImport(org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference importReference) {
		final ImportDeclaration importDeclaration = new ImportDeclaration(this.ast);
		final boolean onDemand = (importReference.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.OnDemand) != 0;
		final char[][] tokens = importReference.tokens;
		int length = importReference.tokens.length;
		final long[] positions = importReference.sourcePositions;
		if (length > 1) {
			importDeclaration.setName(setQualifiedNameNameAndSourceRanges(tokens, positions, importReference));
		} else if(length == 1) {
			final SimpleName name = new SimpleName(this.ast);
			name.internalSetIdentifier(new String(tokens[0]));
			final int start = (int)(positions[0]>>>32);
			final int end = (int)(positions[0] & 0xFFFFFFFF);
			name.setSourceRange(start, end - start + 1);
			name.index = 1;
			importDeclaration.setName(name);
			if (this.resolveBindings) {
				recordNodes(name, importReference);
			}
		}
		boolean isFile=(importReference.bits&org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.IsFileImport)!=0;
		importDeclaration.setSourceRange(importReference.declarationSourceStart, importReference.declarationEnd - importReference.declarationSourceStart + 1);
		importDeclaration.setOnDemand(onDemand && !isFile);
		importDeclaration.setIsFileImport(isFile);
		
		if (this.resolveBindings) {
			recordNodes(importDeclaration, importReference);
		}
		return importDeclaration;
	}

	public PackageDeclaration convertPackage(org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration compilationUnitDeclaration) {
		org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference importReference = compilationUnitDeclaration.currentPackage;
		final PackageDeclaration packageDeclaration = new PackageDeclaration(this.ast);
		final char[][] tokens = importReference.tokens;
		final int length = importReference.tokens.length;
		long[] positions = importReference.sourcePositions;
		if (length > 1) {
			packageDeclaration.setName(setQualifiedNameNameAndSourceRanges(tokens, positions, importReference));
		} else {
			final SimpleName name = new SimpleName(this.ast);
			name.internalSetIdentifier(new String(tokens[0]));
			int start = (int)(positions[0]>>>32);
			int end = (int)(positions[length - 1] & 0xFFFFFFFF);
			name.setSourceRange(start, end - start + 1);
			name.index = 1;
			packageDeclaration.setName(name);
			if (this.resolveBindings) {
				recordNodes(name, compilationUnitDeclaration);
			}
		}
		packageDeclaration.setSourceRange(importReference.declarationSourceStart, importReference.declarationEnd - importReference.declarationSourceStart + 1);
		
		if (this.resolveBindings) {
			recordNodes(packageDeclaration, importReference);
		}
		// Set javadoc
		convert(compilationUnitDeclaration.javadoc, packageDeclaration);
		return packageDeclaration;
	}

	public Expression convertToExpression(org.eclipse.wst.jsdt.internal.compiler.ast.Statement statement) {
		if (statement instanceof org.eclipse.wst.jsdt.internal.compiler.ast.Expression) {
			return convert((org.eclipse.wst.jsdt.internal.compiler.ast.Expression) statement);
		} else {
			return null;
		}
	}

	protected FieldDeclaration convertToFieldDeclaration(org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration fieldDecl) {
		VariableDeclarationFragment variableDeclarationFragment = convertToVariableDeclarationFragment(fieldDecl);
		final FieldDeclaration fieldDeclaration = new FieldDeclaration(this.ast);
		fieldDeclaration.fragments().add(variableDeclarationFragment);
		if (this.resolveBindings) {
			recordNodes(variableDeclarationFragment, fieldDecl);
			variableDeclarationFragment.resolveBinding();
		}
		fieldDeclaration.setSourceRange(fieldDecl.declarationSourceStart, fieldDecl.declarationEnd - fieldDecl.declarationSourceStart + 1);
		Type type = convertType(fieldDecl.type,fieldDecl.inferredType);
		setTypeForField(fieldDeclaration, type, variableDeclarationFragment.getExtraDimensions());
//		setModifiers(fieldDeclaration, fieldDecl);
		convert(fieldDecl.javadoc, fieldDeclaration);
		return fieldDeclaration;
	}

	public ParenthesizedExpression convertToParenthesizedExpression(org.eclipse.wst.jsdt.internal.compiler.ast.Expression expression) {
		final ParenthesizedExpression parenthesizedExpression = new ParenthesizedExpression(this.ast);
		if (this.resolveBindings) {
			recordNodes(parenthesizedExpression, expression);
		}
		parenthesizedExpression.setSourceRange(expression.sourceStart, expression.sourceEnd - expression.sourceStart + 1);
		adjustSourcePositionsForParent(expression);
		trimWhiteSpacesAndComments(expression);
		// decrement the number of parenthesis
		int numberOfParenthesis = (expression.bits & org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK) >> org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedSHIFT;
		expression.bits &= ~org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedMASK;
		expression.bits |= (numberOfParenthesis - 1) << org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode.ParenthesizedSHIFT;
		parenthesizedExpression.setExpression(convert(expression));
		return parenthesizedExpression;
	}

	public Type convertToType(org.eclipse.wst.jsdt.internal.compiler.ast.NameReference reference) {
		Name name = convert(reference);
		final SimpleType type = new SimpleType(this.ast);
		type.setName(name);
		type.setSourceRange(name.getStartPosition(), name.getLength());
		if (this.resolveBindings) {
			this.recordNodes(type, reference);
		}
		return type;
	}

	protected VariableDeclarationExpression convertToVariableDeclarationExpression(org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration localDeclaration) {
		final VariableDeclarationFragment variableDeclarationFragment = convertToVariableDeclarationFragment(localDeclaration);
		final VariableDeclarationExpression variableDeclarationExpression = new VariableDeclarationExpression(this.ast);
		variableDeclarationExpression.fragments().add(variableDeclarationFragment);
		if (this.resolveBindings) {
			recordNodes(variableDeclarationFragment, localDeclaration);
		}
		variableDeclarationExpression.setSourceRange(localDeclaration.declarationSourceStart, localDeclaration.declarationSourceEnd - localDeclaration.declarationSourceStart + 1);
		Type type = convertType(localDeclaration.type,localDeclaration.inferredType);
		setTypeForVariableDeclarationExpression(variableDeclarationExpression, type, variableDeclarationFragment.getExtraDimensions());
		if (localDeclaration.modifiersSourceStart != -1) {
			setModifiers(variableDeclarationExpression, localDeclaration);
		}
		return variableDeclarationExpression;
	}

	protected SingleVariableDeclaration convertToSingleVariableDeclaration(LocalDeclaration localDeclaration) {
		final SingleVariableDeclaration variableDecl = new SingleVariableDeclaration(this.ast);
		setModifiers(variableDecl, localDeclaration);
		final SimpleName name = new SimpleName(this.ast);
		name.internalSetIdentifier(new String(localDeclaration.name));
		int start = localDeclaration.sourceStart;
		int nameEnd = localDeclaration.sourceEnd;
		name.setSourceRange(start, nameEnd - start + 1);
		variableDecl.setName(name);
		final int extraDimensions = retrieveExtraDimension(nameEnd + 1, localDeclaration.type.sourceEnd);
		variableDecl.setExtraDimensions(extraDimensions);
		Type type = convertType(localDeclaration.type,localDeclaration.inferredType);
		int typeEnd = type.getStartPosition() + type.getLength() - 1;
		int rightEnd = Math.max(typeEnd, localDeclaration.declarationSourceEnd);
		/*
		 * There is extra work to do to set the proper type positions
		 * See PR http://bugs.eclipse.org/bugs/show_bug.cgi?id=23284
		 */
		setTypeForSingleVariableDeclaration(variableDecl, type, extraDimensions);
		variableDecl.setSourceRange(localDeclaration.declarationSourceStart, rightEnd - localDeclaration.declarationSourceStart + 1);
		if (this.resolveBindings) {
			recordNodes(name, localDeclaration);
			recordNodes(variableDecl, localDeclaration);
			variableDecl.resolveBinding();
		}
		return variableDecl;
	}

	protected VariableDeclarationFragment convertToVariableDeclarationFragment(org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration fieldDeclaration) {
		final VariableDeclarationFragment variableDeclarationFragment = new VariableDeclarationFragment(this.ast);
		final SimpleName name = new SimpleName(this.ast);
		name.internalSetIdentifier(new String(fieldDeclaration.name));
		name.setSourceRange(fieldDeclaration.sourceStart, fieldDeclaration.sourceEnd - fieldDeclaration.sourceStart + 1);
		variableDeclarationFragment.setName(name);
		int start = fieldDeclaration.sourceEnd;
		if (fieldDeclaration.initialization != null) {
			final Expression expression = convert(fieldDeclaration.initialization);
			variableDeclarationFragment.setInitializer(expression);
			start = expression.getStartPosition() + expression.getLength();
		}
		int end = retrievePositionBeforeNextCommaOrSemiColon(start, fieldDeclaration.declarationSourceEnd);
		if (end == -1) {
			variableDeclarationFragment.setSourceRange(fieldDeclaration.sourceStart, fieldDeclaration.declarationSourceEnd - fieldDeclaration.sourceStart + 1);
			variableDeclarationFragment.setFlags(variableDeclarationFragment.getFlags() | ASTNode.MALFORMED);
		} else {
			variableDeclarationFragment.setSourceRange(fieldDeclaration.sourceStart, end - fieldDeclaration.sourceStart + 1);
		}
		variableDeclarationFragment.setExtraDimensions(retrieveExtraDimension(fieldDeclaration.sourceEnd + 1, fieldDeclaration.declarationSourceEnd ));
		if (this.resolveBindings) {
			recordNodes(name, fieldDeclaration);
			recordNodes(variableDeclarationFragment, fieldDeclaration);
			variableDeclarationFragment.resolveBinding();
		}
		return variableDeclarationFragment;
	}

	protected VariableDeclarationFragment convertToVariableDeclarationFragment(org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration localDeclaration) {
		final VariableDeclarationFragment variableDeclarationFragment = new VariableDeclarationFragment(this.ast);
		final SimpleName name = new SimpleName(this.ast);
		name.internalSetIdentifier(new String(localDeclaration.name));
		name.setSourceRange(localDeclaration.sourceStart, localDeclaration.sourceEnd - localDeclaration.sourceStart + 1);
		variableDeclarationFragment.setName(name);
		int start = localDeclaration.sourceEnd;
		org.eclipse.wst.jsdt.internal.compiler.ast.Expression initialization = localDeclaration.initialization;
		boolean hasInitialization = initialization != null;
		if (hasInitialization) {
			final Expression expression = convert(initialization);
			variableDeclarationFragment.setInitializer(expression);
			start = expression.getStartPosition() + expression.getLength();
		}
		int end = retrievePositionBeforeNextCommaOrSemiColon(start, localDeclaration.declarationSourceEnd);
		if (end == -1) {
			if (hasInitialization) {
				// the initiazation sourceEnd is modified during convert(initialization)
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=128961
				end = start - 1;
			} else {
				end = localDeclaration.sourceEnd;
			}
		}
		variableDeclarationFragment.setSourceRange(localDeclaration.sourceStart, end - localDeclaration.sourceStart + 1);
		variableDeclarationFragment.setExtraDimensions(retrieveExtraDimension(localDeclaration.sourceEnd + 1, this.compilationUnitSourceLength));
		if (this.resolveBindings) {
			recordNodes(variableDeclarationFragment, localDeclaration);
			recordNodes(name, localDeclaration);
			variableDeclarationFragment.resolveBinding();
		}
		return variableDeclarationFragment;
	}

	protected VariableDeclarationStatement convertToVariableDeclarationStatement(org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration localDeclaration) {
		final VariableDeclarationFragment variableDeclarationFragment = convertToVariableDeclarationFragment(localDeclaration);
		final VariableDeclarationStatement variableDeclarationStatement = new VariableDeclarationStatement(this.ast);
		variableDeclarationStatement.fragments().add(variableDeclarationFragment);
		if (this.resolveBindings) {
			recordNodes(variableDeclarationFragment, localDeclaration);
		}
		variableDeclarationStatement.setSourceRange(localDeclaration.declarationSourceStart, localDeclaration.declarationSourceEnd - localDeclaration.declarationSourceStart + 1);
		if (localDeclaration.type!=null)
		{
		  Type type = convertType(localDeclaration.type,localDeclaration.inferredType);
		  setTypeForVariableDeclarationStatement(variableDeclarationStatement, type, variableDeclarationFragment.getExtraDimensions());
		}
		org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration local = localDeclaration;
    	while (local.nextLocal!=null) {
 			variableDeclarationStatement.fragments().add(convertToVariableDeclarationFragment(local.nextLocal));
    		local=(org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration)local.nextLocal;
		}

		if (localDeclaration.modifiersSourceStart != -1) {
			setModifiers(variableDeclarationStatement, localDeclaration);
		}
		convert(localDeclaration.javadoc, variableDeclarationStatement);
		return variableDeclarationStatement;
	}

	public Type convertType(TypeReference typeReference) {
		return convertType(typeReference,null);
	}
	public Type convertType(TypeReference typeReference, org.eclipse.wst.jsdt.core.infer.InferredType inferredType) {
		if (typeReference==null)
		{
			InferredType newType=new InferredType(this.ast);
			newType.setSourceRange(-1,0);
			if (inferredType!=null)
			{
			  newType.type=new String(inferredType.getName());
				if (this.resolveBindings)
					recordNodes(newType, inferredType);
			}
			return newType;
		}
		Type type = null;
		int sourceStart = -1;
		int length = 0;
		int dimensions = typeReference.dimensions();
		if (typeReference instanceof org.eclipse.wst.jsdt.internal.compiler.ast.SingleTypeReference) {
			// this is either an ArrayTypeReference or a SingleTypeReference
			char[] name = ((org.eclipse.wst.jsdt.internal.compiler.ast.SingleTypeReference) typeReference).getTypeName()[0];
			sourceStart = typeReference.sourceStart;
			length = typeReference.sourceEnd - typeReference.sourceStart + 1;
			// need to find out if this is an array type of primitive types or not
			if (isPrimitiveType(name)) {
				int end = retrieveEndOfElementTypeNamePosition(sourceStart, sourceStart + length);
				if (end == -1) {
					end = sourceStart + length - 1;
				}
				final PrimitiveType primitiveType = new PrimitiveType(this.ast);
				primitiveType.setPrimitiveTypeCode(getPrimitiveTypeCode(name));
				primitiveType.setSourceRange(sourceStart, end - sourceStart + 1);
				type = primitiveType;
			} else {
				final SimpleName simpleName = new SimpleName(this.ast);
				simpleName.internalSetIdentifier(new String(name));
				// we need to search for the starting position of the first brace in order to set the proper length
				// PR http://dev.eclipse.org/bugs/show_bug.cgi?id=10759
				int end = retrieveEndOfElementTypeNamePosition(sourceStart, sourceStart + length);
				if (end == -1) {
					end = sourceStart + length - 1;
				}
				simpleName.setSourceRange(sourceStart, end - sourceStart + 1);
				final SimpleType simpleType = new SimpleType(this.ast);
				simpleType.setName(simpleName);
				type = simpleType;
				type.setSourceRange(sourceStart, end - sourceStart + 1);
				type = simpleType;
				if (this.resolveBindings) {
					this.recordNodes(simpleName, typeReference);
				}
			}
			if (dimensions != 0) {
				type = this.ast.newArrayType(type, dimensions);
				type.setSourceRange(sourceStart, length);
				ArrayType subarrayType = (ArrayType) type;
				int index = dimensions - 1;
				while (index > 0) {
					subarrayType = (ArrayType) subarrayType.getComponentType();
					int end = retrieveProperRightBracketPosition(index, sourceStart);
					subarrayType.setSourceRange(sourceStart, end - sourceStart + 1);
					index--;
				}
				if (this.resolveBindings) {
					// store keys for inner types
					completeRecord((ArrayType) type, typeReference);
				}
			}
		} else {
			char[][] name = ((org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference) typeReference).getTypeName();
			int nameLength = name.length;
			long[] positions = ((org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference) typeReference).sourcePositions;
			sourceStart = (int)(positions[0]>>>32);
			length = (int)(positions[nameLength - 1] & 0xFFFFFFFF) - sourceStart + 1;
			final Name qualifiedName = this.setQualifiedNameNameAndSourceRanges(name, positions, typeReference);
			final SimpleType simpleType = new SimpleType(this.ast);
			simpleType.setName(qualifiedName);
			type = simpleType;
			type.setSourceRange(sourceStart, length);
			
			length = typeReference.sourceEnd - sourceStart + 1;
			if (dimensions != 0) {
				type = this.ast.newArrayType(type, dimensions);
				if (this.resolveBindings) {
					completeRecord((ArrayType) type, typeReference);
				}
				int end = retrieveEndOfDimensionsPosition(sourceStart+length, this.compilationUnitSourceLength);
				if (end != -1) {
					type.setSourceRange(sourceStart, end - sourceStart + 1);
				} else {
					type.setSourceRange(sourceStart, length);
				}
				ArrayType subarrayType = (ArrayType) type;
				int index = dimensions - 1;
				while (index > 0) {
					subarrayType = (ArrayType) subarrayType.getComponentType();
					end = retrieveProperRightBracketPosition(index, sourceStart);
					subarrayType.setSourceRange(sourceStart, end - sourceStart + 1);
					index--;
				}
			}
		}
		if (this.resolveBindings) {
			this.recordNodes(type, typeReference);
		}
		return type;
	}

	protected Comment createComment(int[] positions) {
		// Create comment node
		Comment comment = null;
		int start = positions[0];
		int end = positions[1];
		if (positions[1]>0) { // jsdoc comments have positive end position
			JSdoc docComment = this.docParser.parse(positions);
			if (docComment == null) return null;
			comment = docComment;
		} else {
			end = -end;
			if (positions[0] == 0) { // we cannot know without testing chars again
				if (this.docParser.scanner.source[1] == '/') {
					comment = new LineComment(this.ast);
				} else {
					comment = new BlockComment(this.ast);
				}
			}
			else if (positions[0]>0) { // Block comment have positive start position
				comment = new BlockComment(this.ast);
			} else { // Line comment have negative start and end position
				start = -start;
				comment = new LineComment(this.ast);
			}
			comment.setSourceRange(start, end - start);
		}
		return comment;
	}

	protected Statement createFakeEmptyStatement(org.eclipse.wst.jsdt.internal.compiler.ast.Statement statement) {
		if (statement == null) return null;
		EmptyStatement emptyStatement = new EmptyStatement(this.ast);
		emptyStatement.setFlags(emptyStatement.getFlags() | ASTNode.MALFORMED);
		int start = statement.sourceStart;
		int end = statement.sourceEnd;
		emptyStatement.setSourceRange(start, end - start + 1);
		return emptyStatement;
	}
	/**
	 * @return a new modifier
	 */
	private Modifier createModifier(ModifierKeyword keyword) {
		final Modifier modifier = new Modifier(this.ast);
		modifier.setKeyword(keyword);
		int start = this.scanner.getCurrentTokenStartPosition();
		int end = this.scanner.getCurrentTokenEndPosition();
		modifier.setSourceRange(start, end - start + 1);
		return modifier;
	}

	protected InfixExpression.Operator getOperatorFor(int operatorID) {
		switch (operatorID) {
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.EQUAL_EQUAL :
				return InfixExpression.Operator.EQUALS;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.LESS_EQUAL :
				return InfixExpression.Operator.LESS_EQUALS;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.GREATER_EQUAL :
				return InfixExpression.Operator.GREATER_EQUALS;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.NOT_EQUAL :
				return InfixExpression.Operator.NOT_EQUALS;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.LEFT_SHIFT :
				return InfixExpression.Operator.LEFT_SHIFT;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.RIGHT_SHIFT :
				return InfixExpression.Operator.RIGHT_SHIFT_SIGNED;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.UNSIGNED_RIGHT_SHIFT :
				return InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.OR_OR :
				return InfixExpression.Operator.CONDITIONAL_OR;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.AND_AND :
				return InfixExpression.Operator.CONDITIONAL_AND;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.PLUS :
				return InfixExpression.Operator.PLUS;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.MINUS :
				return InfixExpression.Operator.MINUS;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.REMAINDER :
				return InfixExpression.Operator.REMAINDER;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.XOR :
				return InfixExpression.Operator.XOR;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.AND :
				return InfixExpression.Operator.AND;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.MULTIPLY :
				return InfixExpression.Operator.TIMES;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.OR :
				return InfixExpression.Operator.OR;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.DIVIDE :
				return InfixExpression.Operator.DIVIDE;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.GREATER :
				return InfixExpression.Operator.GREATER;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.LESS :
				return InfixExpression.Operator.LESS;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.INSTANCEOF :
				return InfixExpression.Operator.INSTANCEOF;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.IN :
				return InfixExpression.Operator.IN;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.EQUAL_EQUAL_EQUAL :
				return InfixExpression.Operator.EQUAL_EQUAL_EQUAL;
			case org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds.NOT_EQUAL_EQUAL :
				return InfixExpression.Operator.NOT_EQUAL_EQUAL;
		}
		return null;
	}

	protected PrimitiveType.Code getPrimitiveTypeCode(char[] name) {
		switch(name[0]) {
			case 'i' :
				if (name.length == 3 && name[1] == 'n' && name[2] == 't') {
					return PrimitiveType.INT;
				}
				break;
			case 'l' :
				if (name.length == 4 && name[1] == 'o' && name[2] == 'n' && name[3] == 'g') {
					return PrimitiveType.LONG;
				}
				break;
			case 'd' :
				if (name.length == 6
					 && name[1] == 'o'
					 && name[2] == 'u'
					 && name[3] == 'b'
					 && name[4] == 'l'
					 && name[5] == 'e') {
					return PrimitiveType.DOUBLE;
				}
				break;
			case 'f' :
				if (name.length == 5
					 && name[1] == 'l'
					 && name[2] == 'o'
					 && name[3] == 'a'
					 && name[4] == 't') {
					return PrimitiveType.FLOAT;
				}
				break;
			case 'b' :
				if (name.length == 4
					 && name[1] == 'y'
					 && name[2] == 't'
					 && name[3] == 'e') {
					return PrimitiveType.BYTE;
				} else
					if (name.length == 7
						 && name[1] == 'o'
						 && name[2] == 'o'
						 && name[3] == 'l'
						 && name[4] == 'e'
						 && name[5] == 'a'
						 && name[6] == 'n') {
					return PrimitiveType.BOOLEAN;
				}
				break;
			case 'c' :
				if (name.length == 4
					 && name[1] == 'h'
					 && name[2] == 'a'
					 && name[3] == 'r') {
					return PrimitiveType.CHAR;
				}
				break;
			case 's' :
				if (name.length == 5
					 && name[1] == 'h'
					 && name[2] == 'o'
					 && name[3] == 'r'
					 && name[4] == 't') {
					return PrimitiveType.SHORT;
				}
				break;
			case 'v' :
				if (name.length == 4
					 && name[1] == 'o'
					 && name[2] == 'i'
					 && name[3] == 'd') {
					return PrimitiveType.VOID;
				}
		}
		return null; // cannot be reached
	}

	protected boolean isPrimitiveType(char[] name) {
		switch(name[0]) {
			case 'i' :
				if (name.length == 3 && name[1] == 'n' && name[2] == 't') {
					return true;
				}
				return false;
			case 'l' :
				if (name.length == 4 && name[1] == 'o' && name[2] == 'n' && name[3] == 'g') {
					return true;
				}
				return false;
			case 'd' :
				if (name.length == 6
					 && name[1] == 'o'
					 && name[2] == 'u'
					 && name[3] == 'b'
					 && name[4] == 'l'
					 && name[5] == 'e') {
					return true;
				}
				return false;
			case 'f' :
				if (name.length == 5
					 && name[1] == 'l'
					 && name[2] == 'o'
					 && name[3] == 'a'
					 && name[4] == 't') {
					return true;
				}
				return false;
			case 'b' :
				if (name.length == 4
					 && name[1] == 'y'
					 && name[2] == 't'
					 && name[3] == 'e') {
					return true;
				} else
					if (name.length == 7
						 && name[1] == 'o'
						 && name[2] == 'o'
						 && name[3] == 'l'
						 && name[4] == 'e'
						 && name[5] == 'a'
						 && name[6] == 'n') {
					return true;
				}
				return false;
			case 'c' :
				if (name.length == 4
					 && name[1] == 'h'
					 && name[2] == 'a'
					 && name[3] == 'r') {
					return true;
				}
				return false;
			case 's' :
				if (name.length == 5
					 && name[1] == 'h'
					 && name[2] == 'o'
					 && name[3] == 'r'
					 && name[4] == 't') {
					return true;
				}
				return false;
			case 'v' :
				if (name.length == 4
					 && name[1] == 'o'
					 && name[2] == 'i'
					 && name[3] == 'd') {
					return true;
				}
				return false;
		}
		return false;
	}

	private void lookupForScopes() {
		if (this.pendingNameScopeResolution != null) {
			for (Iterator iterator = this.pendingNameScopeResolution.iterator(); iterator.hasNext(); ) {
				Name name = (Name) iterator.next();
				this.ast.getBindingResolver().recordScope(name, lookupScope(name));
			}
		}
		if (this.pendingThisExpressionScopeResolution != null) {
			for (Iterator iterator = this.pendingThisExpressionScopeResolution.iterator(); iterator.hasNext(); ) {
				ThisExpression thisExpression = (ThisExpression) iterator.next();
				this.ast.getBindingResolver().recordScope(thisExpression, lookupScope(thisExpression));
			}
		}

	}

	private BlockScope lookupScope(ASTNode node) {
		ASTNode currentNode = node;
		while(currentNode != null
			&&!(currentNode instanceof FunctionDeclaration)
			&& !(currentNode instanceof Initializer)
			&& !(currentNode instanceof FieldDeclaration)) {
			currentNode = currentNode.getParent();
		}
		if (currentNode == null) {
			return null;
		}
		if (currentNode instanceof Initializer) {
			Initializer initializer = (Initializer) currentNode;
			while(!(currentNode instanceof AbstractTypeDeclaration)) {
				currentNode = currentNode.getParent();
			}
			if (currentNode instanceof TypeDeclaration) {
				org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration typeDecl = (org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) this.ast.getBindingResolver().getCorrespondingNode(currentNode);
				if ((initializer.getModifiers() & Modifier.STATIC) != 0) {
					return typeDecl.staticInitializerScope;
				} else {
					return typeDecl.initializerScope;
				}
			}
		} else if (currentNode instanceof FieldDeclaration) {
			FieldDeclaration fieldDeclaration = (FieldDeclaration) currentNode;
			while(!(currentNode instanceof AbstractTypeDeclaration)) {
				currentNode = currentNode.getParent();
			}
			if (currentNode instanceof TypeDeclaration) {
				org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration typeDecl = (org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) this.ast.getBindingResolver().getCorrespondingNode(currentNode);
				if ((fieldDeclaration.getModifiers() & Modifier.STATIC) != 0) {
					return typeDecl.staticInitializerScope;
				} else {
					return typeDecl.initializerScope;
				}
			}
		}
		AbstractMethodDeclaration abstractMethodDeclaration = (AbstractMethodDeclaration) this.ast.getBindingResolver().getCorrespondingNode(currentNode);
		return abstractMethodDeclaration.getScope();
	}

	protected void recordName(Name name, org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode compilerNode) {
		if (compilerNode != null) {
			recordNodes(name, compilerNode);
			if (compilerNode instanceof org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference) {
				org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference typeRef = (org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference) compilerNode;
				if (name.isQualifiedName()) {
					SimpleName simpleName = null;
					while (name.isQualifiedName()) {
						simpleName = ((QualifiedName) name).getName();
						recordNodes(simpleName, typeRef);
						name = ((QualifiedName) name).getQualifier();
						recordNodes(name, typeRef);
					}
				}
			}
		}
	}

	protected void recordNodes(ASTNode node, org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode oldASTNode) {
		this.ast.getBindingResolver().store(node, oldASTNode);
	}

	protected void recordNodes(org.eclipse.wst.jsdt.internal.compiler.ast.Javadoc javadoc, TagElement tagElement) {
		Iterator fragments = tagElement.fragments().listIterator();
		while (fragments.hasNext()) {
			ASTNode node = (ASTNode) fragments.next();
			if (node.getNodeType() == ASTNode.MEMBER_REF) {
				MemberRef memberRef = (MemberRef) node;
				Name name = memberRef.getName();
				// get compiler node and record nodes
				int start = name.getStartPosition();
				org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode compilerNode = javadoc.getNodeStartingAt(start);
				if (compilerNode!= null) {
					recordNodes(name, compilerNode);
					recordNodes(node, compilerNode);
				}
				// Replace qualifier to have all nodes recorded
				if (memberRef.getQualifier() != null) {
					org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference typeRef = null;
					if (compilerNode instanceof JavadocFieldReference) {
						org.eclipse.wst.jsdt.internal.compiler.ast.Expression expression = ((JavadocFieldReference)compilerNode).receiver;
						if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference) {
							typeRef = (org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference) expression;
						}
					}
					else if (compilerNode instanceof JavadocMessageSend) {
						org.eclipse.wst.jsdt.internal.compiler.ast.Expression expression = ((JavadocMessageSend)compilerNode).receiver;
						if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference) {
							typeRef = (org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference) expression;
						}
					}
					if (typeRef != null) {
						recordName(memberRef.getQualifier(), typeRef);
					}
				}
			} else if (node.getNodeType() == ASTNode.FUNCTION_REF) {
				FunctionRef methodRef = (FunctionRef) node;
				Name name = methodRef.getName();
				// get method name start position
				int start = methodRef.getStartPosition();
				this.scanner.resetTo(start, start + name.getStartPosition()+name.getLength());
				int token;
				try {
					nextToken: while((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF && token != TerminalTokens.TokenNameLPAREN)  {
						if (token == TerminalTokens.TokenNameERROR && this.scanner.currentCharacter == '#') {
							start = this.scanner.getCurrentTokenEndPosition()+1;
							break nextToken;
						}
					}
				}
				catch(InvalidInputException e) {
					// ignore
				}
				// get compiler node and record nodes
				org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode compilerNode = javadoc.getNodeStartingAt(start);
				// record nodes
				if (compilerNode != null) {
					recordNodes(methodRef, compilerNode);
					// get type ref
					org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference typeRef = null;
					if (compilerNode instanceof org.eclipse.wst.jsdt.internal.compiler.ast.JavadocAllocationExpression) {
						typeRef = ((org.eclipse.wst.jsdt.internal.compiler.ast.JavadocAllocationExpression)compilerNode).type;
						if (typeRef != null) recordNodes(name, compilerNode);
					}
					else if (compilerNode instanceof org.eclipse.wst.jsdt.internal.compiler.ast.JavadocMessageSend) {
						org.eclipse.wst.jsdt.internal.compiler.ast.Expression expression = ((org.eclipse.wst.jsdt.internal.compiler.ast.JavadocMessageSend)compilerNode).receiver;
						if (expression instanceof org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference) {
							typeRef = (org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference) expression;
						}
						recordNodes(name, compilerNode);
					}
					// record name and qualifier
					if (typeRef != null && methodRef.getQualifier() != null) {
						recordName(methodRef.getQualifier(), typeRef);
					}
				}
				// Resolve parameters
				Iterator parameters = methodRef.parameters().listIterator();
				while (parameters.hasNext()) {
					FunctionRefParameter param = (FunctionRefParameter) parameters.next();
					org.eclipse.wst.jsdt.internal.compiler.ast.Expression expression = (org.eclipse.wst.jsdt.internal.compiler.ast.Expression) javadoc.getNodeStartingAt(param.getStartPosition());
					if (expression != null) {
						recordNodes(param, expression);
						if (expression instanceof JavadocArgumentExpression) {
							JavadocArgumentExpression argExpr = (JavadocArgumentExpression) expression;
							org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference typeRef = argExpr.argument.type;
							if (this.ast.apiLevel >= AST.JLS3) param.setVarargs(argExpr.argument.isVarArgs());
							recordNodes(param.getType(), typeRef);
							if (param.getType().isSimpleType()) {
								recordName(((SimpleType)param.getType()).getName(), typeRef);
							} else if (param.getType().isArrayType()) {
								Type type = ((ArrayType) param.getType()).getElementType();
								recordNodes(type, typeRef);
								if (type.isSimpleType()) {
									recordName(((SimpleType)type).getName(), typeRef);
								}
							}
						}
					}
				}
			} else if (node.getNodeType() == ASTNode.SIMPLE_NAME ||
					node.getNodeType() == ASTNode.QUALIFIED_NAME) {
				org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode compilerNode = javadoc.getNodeStartingAt(node.getStartPosition());
				recordName((Name) node, compilerNode);
			} else if (node.getNodeType() == ASTNode.TAG_ELEMENT) {
				// resolve member and method references binding
				recordNodes(javadoc, (TagElement) node);
			}
		}
	}
	protected void recordPendingNameScopeResolution(Name name) {
		if (this.pendingNameScopeResolution == null) {
			this.pendingNameScopeResolution = new HashSet();
		}
		this.pendingNameScopeResolution.add(name);
	}

	protected void recordPendingThisExpressionScopeResolution(ThisExpression thisExpression) {
		if (this.pendingThisExpressionScopeResolution == null) {
			this.pendingThisExpressionScopeResolution = new HashSet();
		}
		this.pendingThisExpressionScopeResolution.add(thisExpression);
	}

	/**
	 * Remove white spaces and comments before and after the expression.
	 */
	private void trimWhiteSpacesAndComments(org.eclipse.wst.jsdt.internal.compiler.ast.Statement statement) {
		int start = statement.sourceStart;
		int end = statement.sourceEnd;
		int token;
		int trimLeftPosition = statement.sourceStart;
		int trimRightPosition = statement.sourceEnd;
		boolean first = true;
		Scanner removeBlankScanner = this.ast.scanner;
		try {
			removeBlankScanner.setSource(this.compilationUnitSource);
			removeBlankScanner.resetTo(start, end);
			while (true) {
				token = removeBlankScanner.getNextToken();
				switch (token) {
					case TerminalTokens.TokenNameCOMMENT_JAVADOC :
					case TerminalTokens.TokenNameCOMMENT_LINE :
					case TerminalTokens.TokenNameCOMMENT_BLOCK :
						if (first) {
							trimLeftPosition = removeBlankScanner.currentPosition;
						}
						break;
					case TerminalTokens.TokenNameWHITESPACE :
						if (first) {
							trimLeftPosition = removeBlankScanner.currentPosition;
						}
						break;
					case TerminalTokens.TokenNameEOF :
						statement.sourceStart = trimLeftPosition;
						statement.sourceEnd = trimRightPosition;
						return;
					default :
						/*
						 * if we find something else than a whitespace or a comment,
						 * then we reset the trimRigthPosition to the expression
						 * source end.
						 */
						trimRightPosition = removeBlankScanner.currentPosition - 1;
						first = false;
				}
			}
		} catch (InvalidInputException e){
			// ignore
		}
	}

	/**
	 * Remove potential trailing comment by settings the source end on the closing parenthesis
	 */
	protected void removeLeadingAndTrailingCommentsFromLiteral(ASTNode node) {
		int start = node.getStartPosition();
		this.scanner.resetTo(start, start + node.getLength());
		int token;
		int startPosition = -1;
		try {
			while((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF)  {
				switch(token) {
					case TerminalTokens.TokenNameIntegerLiteral :
					case TerminalTokens.TokenNameFloatingPointLiteral :
					case TerminalTokens.TokenNameLongLiteral :
					case TerminalTokens.TokenNameDoubleLiteral :
					case TerminalTokens.TokenNameCharacterLiteral :
					case TerminalTokens.TokenNameRegExLiteral :
						if (startPosition == -1) {
							startPosition = this.scanner.startPosition;
						}
						int end = this.scanner.currentPosition;
						node.setSourceRange(startPosition, end - startPosition);
						return;
					case TerminalTokens.TokenNameMINUS :
						startPosition = this.scanner.startPosition;
						break;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
	}

	/**
	 * Remove potential trailing comment by settings the source end on the closing parenthesis
	 */
	protected void removeTrailingCommentFromExpressionEndingWithAParen(ASTNode node) {
		int start = node.getStartPosition();
		this.scanner.resetTo(start, start + node.getLength());
		int token;
		int parenCounter = 0;
		try {
			while((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF)  {
				switch(token) {
					case TerminalTokens.TokenNameLPAREN :
						parenCounter++;
						break;
					case TerminalTokens.TokenNameRPAREN :
						parenCounter--;
						if (parenCounter == 0) {
							int end = this.scanner.currentPosition - 1;
							node.setSourceRange(start, end - start + 1);
						}
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
	}

	/**
	 * This method is used to retrieve the end position of the block.
	 * @return int the dimension found, -1 if none
	 */
	protected int retrieveClosingAngleBracketPosition(int start) {
		this.scanner.resetTo(start, this.compilationUnitSourceLength);
		this.scanner.returnOnlyGreater = true;
		try {
			int token;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameGREATER:
						return this.scanner.currentPosition - 1;
					default:
						return start;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		this.scanner.returnOnlyGreater = false;
		return start;
	}

	/**
	 * This method is used to set the right end position for expression
	 * statement. The actual AST nodes don't include the trailing semicolon.
	 * This method fixes the length of the corresponding node.
	 */
	protected void retrieveColonPosition(ASTNode node) {
		int start = node.getStartPosition();
		int length = node.getLength();
		int end = start + length;
		this.scanner.resetTo(end, this.compilationUnitSourceLength);
		try {
			int token;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameCOLON:
						node.setSourceRange(start, this.scanner.currentPosition - start);
						return;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
	}
//	/**
//	 * This method is used to retrieve the start position of the Ellipsis
//	 */
//	protected int retrieveEllipsisStartPosition(int start, int end) {
//		this.scanner.resetTo(start, end);
//		try {
//			int token;
//			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
//				switch(token) {
//					case TerminalTokens.TokenNameELLIPSIS:
//						return this.scanner.startPosition - 1;
//				}
//			}
//		} catch(InvalidInputException e) {
//			// ignore
//		}
//		return -1;
//
//	}
	/**
	 * This method is used to retrieve the end position of the block.
	 * @return int the dimension found, -1 if none
	 */
	protected int retrieveEndBlockPosition(int start, int end) {
		this.scanner.resetTo(start, end);
		int count = 0;
		try {
			int token;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameLBRACE://110
						count++;
						break;
					case TerminalTokens.TokenNameRBRACE://95
						count--;
						if (count == 0) {
							return this.scanner.currentPosition - 1;
						}
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return -1;
	}

	protected int retrieveSemiColonPosition(Expression node) {
		int start = node.getStartPosition();
		int length = node.getLength();
		int end = start + length;
		this.scanner.resetTo(end, this.compilationUnitSourceLength);
		try {
			int token;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameSEMICOLON:
						return this.scanner.currentPosition - 1;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return -1;
	}

	/**
	 * This method is used to retrieve the ending position for a type declaration when the dimension is right after the type
	 * name.
	 * For example:
	 *    int[] i; => return 5, but int i[] => return -1;
	 * @return int the dimension found
	 */
	protected int retrieveEndOfDimensionsPosition(int start, int end) {
		this.scanner.resetTo(start, end);
		int foundPosition = -1;
		try {
			int token;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameLBRACKET:
					case TerminalTokens.TokenNameCOMMENT_BLOCK:
					case TerminalTokens.TokenNameCOMMENT_JAVADOC:
					case TerminalTokens.TokenNameCOMMENT_LINE:
						break;
					case TerminalTokens.TokenNameRBRACKET://166
						foundPosition = this.scanner.currentPosition - 1;
						break;
					default:
						return foundPosition;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return foundPosition;
	}

	/**
	 * This method is used to retrieve the position just before the left bracket.
	 * @return int the dimension found, -1 if none
	 */
	protected int retrieveEndOfElementTypeNamePosition(int start, int end) {
		this.scanner.resetTo(start, end);
		try {
			int token;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameIdentifier:
					case TerminalTokens.TokenNamebyte:
					case TerminalTokens.TokenNamechar:
					case TerminalTokens.TokenNamedouble:
					case TerminalTokens.TokenNamefloat:
					case TerminalTokens.TokenNameint:
					case TerminalTokens.TokenNamelong:
					case TerminalTokens.TokenNameshort:
					case TerminalTokens.TokenNameboolean:
						return this.scanner.currentPosition - 1;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return -1;
	}

	/**
	 * This method is used to retrieve the position after the right parenthesis.
	 * @return int the position found
	 */
	protected int retrieveEndOfRightParenthesisPosition(int start, int end) {
		this.scanner.resetTo(start, end);
		try {
			int token;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameRPAREN:
						return this.scanner.currentPosition;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return -1;
	}

	/**
	 * This method is used to retrieve the array dimension declared after the
	 * name of a local or a field declaration.
	 * For example:
	 *    int i, j[] = null, k[][] = {{}};
	 *    It should return 0 for i, 1 for j and 2 for k.
	 * @return int the dimension found
	 */
	protected int retrieveExtraDimension(int start, int end) {
		this.scanner.resetTo(start, end);
		int dimensions = 0;
		try {
			int token;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameLBRACKET:
					case TerminalTokens.TokenNameCOMMENT_BLOCK:
					case TerminalTokens.TokenNameCOMMENT_JAVADOC:
					case TerminalTokens.TokenNameCOMMENT_LINE:
						break;
					case TerminalTokens.TokenNameRBRACKET://166
						dimensions++;
						break;
					default:
						return dimensions;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return dimensions;
	}

	protected void retrieveIdentifierAndSetPositions(int start, int end, Name name) {
		this.scanner.resetTo(start, end);
		int token;
		try {
			while((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF)  {
				if (token == TerminalTokens.TokenNameIdentifier) {
					int startName = this.scanner.startPosition;
					int endName = this.scanner.currentPosition - 1;
					name.setSourceRange(startName, endName - startName + 1);
					return;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
	}

	/**
	 * This method is used to retrieve the start position of the block.
	 * @return int the dimension found, -1 if none
	 */
	protected int retrieveIdentifierEndPosition(int start, int end) {
		this.scanner.resetTo(start, end);
		try {
			int token;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameIdentifier://110
						return this.scanner.getCurrentTokenEndPosition();
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return -1;
	}

	/**
	 * This method is used to retrieve position before the next comma or semi-colon.
	 * @return int the position found.
	 */
	protected int retrievePositionBeforeNextCommaOrSemiColon(int start, int end) {
		this.scanner.resetTo(start, end);
		try {
			int token;
			int balance = 0;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameLBRACE :
						balance++;
						break;
					case TerminalTokens.TokenNameRBRACE :
						balance --;
						break;
					case TerminalTokens.TokenNameCOMMA :
						if (balance == 0) return this.scanner.startPosition - 1;
						break;
					case TerminalTokens.TokenNameSEMICOLON :
						return this.scanner.startPosition - 1;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return -1;
	}

	protected int retrieveProperRightBracketPosition(int bracketNumber, int start) {
		this.scanner.resetTo(start, this.compilationUnitSourceLength);
		try {
			int token, count = 0;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameRBRACKET:
						count++;
						if (count == bracketNumber) {
							return this.scanner.currentPosition - 1;
						}
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return -1;
	}

	/**
	 * This method is used to retrieve position before the next right brace or semi-colon.
	 * @return int the position found.
	 */
	protected int retrieveRightBraceOrSemiColonPosition(int start, int end) {
		this.scanner.resetTo(start, end);
		try {
			int token;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameRBRACE :
						return this.scanner.currentPosition - 1;
					case TerminalTokens.TokenNameSEMICOLON :
						return this.scanner.currentPosition - 1;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return -1;
	}

	/**
	 * This method is used to retrieve position before the next right brace or semi-colon.
	 * @return int the position found.
	 */
	protected int retrieveRightBrace(int start, int end) {
		this.scanner.resetTo(start, end);
		try {
			int token;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameRBRACE :
						return this.scanner.currentPosition - 1;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return -1;
	}

	/**
	 * This method is used to retrieve the position of the right bracket.
	 * @return int the dimension found, -1 if none
	 */
	protected int retrieveRightBracketPosition(int start, int end) {
		this.scanner.resetTo(start, end);
		try {
			int token;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameRBRACKET:
						return this.scanner.currentPosition - 1;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return -1;
	}



	/**
	 * This method is used to retrieve the start position of the block.
	 * @return int the dimension found, -1 if none
	 */
	protected int retrieveStartBlockPosition(int start, int end) {
		this.scanner.resetTo(start, end);
		try {
			int token;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameLBRACE://110
						return this.scanner.startPosition;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return -1;
	}

	/**
	 * This method is used to retrieve the starting position of the catch keyword.
	 * @return int the dimension found, -1 if none
	 */
	protected int retrieveStartingCatchPosition(int start, int end) {
		this.scanner.resetTo(start, end);
		try {
			int token;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNamecatch://225
						return this.scanner.startPosition;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return -1;
	}

	public void setAST(AST ast) {
		this.ast = ast;
		this.docParser = new DocCommentParser(this.ast, this.scanner, this.insideComments);
	}

	/**
	 * @param bodyDeclaration
	 */
	protected void setModifiers(BodyDeclaration bodyDeclaration) {
		try {
			int token;
			while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				IExtendedModifier modifier = null;
				switch(token) {
					case TerminalTokens.TokenNameabstract:
						modifier = createModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD);
						break;
					case TerminalTokens.TokenNamepublic:
						modifier = createModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
						break;
					case TerminalTokens.TokenNamestatic:
						modifier = createModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);
						break;
					case TerminalTokens.TokenNameprotected:
						modifier = createModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD);
						break;
					case TerminalTokens.TokenNameprivate:
						modifier = createModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
						break;
					case TerminalTokens.TokenNamefinal:
						modifier = createModifier(Modifier.ModifierKeyword.FINAL_KEYWORD);
						break;
					case TerminalTokens.TokenNamenative:
						modifier = createModifier(Modifier.ModifierKeyword.NATIVE_KEYWORD);
						break;
					case TerminalTokens.TokenNamesynchronized:
						modifier = createModifier(Modifier.ModifierKeyword.SYNCHRONIZED_KEYWORD);
						break;
					case TerminalTokens.TokenNametransient:
						modifier = createModifier(Modifier.ModifierKeyword.TRANSIENT_KEYWORD);
						break;
					case TerminalTokens.TokenNamevolatile:
						modifier = createModifier(Modifier.ModifierKeyword.VOLATILE_KEYWORD);
						break;
					case TerminalTokens.TokenNameCOMMENT_BLOCK :
					case TerminalTokens.TokenNameCOMMENT_LINE :
					case TerminalTokens.TokenNameCOMMENT_JAVADOC :
						break;
					default :
						return;
				}
				if (modifier != null) {
					bodyDeclaration.modifiers().add(modifier);
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
	}


	/**
	 * @param fieldDeclaration
	 * @param fieldDecl
	 */
	protected void setModifiers(FieldDeclaration fieldDeclaration, org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration fieldDecl) {
		switch(this.ast.apiLevel) {
			case AST.JLS2_INTERNAL :
				fieldDeclaration.internalSetModifiers(fieldDecl.modifiers & ExtraCompilerModifiers.AccJustFlag);
				break;
			case AST.JLS3 :
				this.scanner.resetTo(fieldDecl.declarationSourceStart, fieldDecl.sourceStart);
				this.setModifiers(fieldDeclaration);
		}
	}

	/**
	 * @param initializer
	 * @param oldInitializer
	 */
	protected void setModifiers(Initializer initializer, org.eclipse.wst.jsdt.internal.compiler.ast.Initializer oldInitializer) {
		switch(this.ast.apiLevel) {
			case AST.JLS2_INTERNAL:
				initializer.internalSetModifiers(oldInitializer.modifiers & ExtraCompilerModifiers.AccJustFlag);
				break;
			case AST.JLS3 :
				this.scanner.resetTo(oldInitializer.declarationSourceStart, oldInitializer.bodyStart);
				this.setModifiers(initializer);
		}
	}
	/**
	 * @param methodDecl
	 * @param methodDeclaration
	 */
	protected void setModifiers(FunctionDeclaration methodDecl, AbstractMethodDeclaration methodDeclaration) {
		switch(this.ast.apiLevel) {
			case AST.JLS2_INTERNAL :
				methodDecl.internalSetModifiers(methodDeclaration.modifiers & ExtraCompilerModifiers.AccJustFlag);
				break;
			case AST.JLS3 :
				if (methodDeclaration.sourceStart>methodDeclaration.declarationSourceStart)
				{
				  this.scanner.resetTo(methodDeclaration.declarationSourceStart, methodDeclaration.sourceStart);
				  this.setModifiers(methodDecl);
				}
		}
	}

	/**
	 * @param variableDecl
	 * @param argument
	 */
	protected void setModifiers(SingleVariableDeclaration variableDecl, Argument argument) {
		switch(this.ast.apiLevel) {
			case AST.JLS2_INTERNAL :
				variableDecl.internalSetModifiers(argument.modifiers & ExtraCompilerModifiers.AccJustFlag);
				break;
			case AST.JLS3 :
				this.scanner.resetTo(argument.declarationSourceStart, argument.sourceStart);
				try {
					int token;
					while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
						IExtendedModifier modifier = null;
						switch(token) {
							case TerminalTokens.TokenNameabstract:
								modifier = createModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD);
								break;
							case TerminalTokens.TokenNamepublic:
								modifier = createModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
								break;
							case TerminalTokens.TokenNamestatic:
								modifier = createModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);
								break;
							case TerminalTokens.TokenNameprotected:
								modifier = createModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD);
								break;
							case TerminalTokens.TokenNameprivate:
								modifier = createModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
								break;
							case TerminalTokens.TokenNamefinal:
								modifier = createModifier(Modifier.ModifierKeyword.FINAL_KEYWORD);
								break;
							case TerminalTokens.TokenNamenative:
								modifier = createModifier(Modifier.ModifierKeyword.NATIVE_KEYWORD);
								break;
							case TerminalTokens.TokenNamesynchronized:
								modifier = createModifier(Modifier.ModifierKeyword.SYNCHRONIZED_KEYWORD);
								break;
							case TerminalTokens.TokenNametransient:
								modifier = createModifier(Modifier.ModifierKeyword.TRANSIENT_KEYWORD);
								break;
							case TerminalTokens.TokenNamevolatile:
								modifier = createModifier(Modifier.ModifierKeyword.VOLATILE_KEYWORD);
								break;
							case TerminalTokens.TokenNameCOMMENT_BLOCK :
							case TerminalTokens.TokenNameCOMMENT_LINE :
							case TerminalTokens.TokenNameCOMMENT_JAVADOC :
								break;
							default :
								return;
						}
						if (modifier != null) {
							variableDecl.modifiers().add(modifier);
						}
					}
				} catch(InvalidInputException e) {
					// ignore
				}
		}
	}

	protected void setModifiers(SingleVariableDeclaration variableDecl, LocalDeclaration localDeclaration) {
		switch(this.ast.apiLevel) {
		case AST.JLS2_INTERNAL :
			variableDecl.internalSetModifiers(localDeclaration.modifiers & ExtraCompilerModifiers.AccJustFlag);
			break;
		case AST.JLS3 :
			this.scanner.resetTo(localDeclaration.declarationSourceStart, localDeclaration.sourceStart);
			try {
				int token;
				while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
					IExtendedModifier modifier = null;
					switch(token) {
						case TerminalTokens.TokenNameabstract:
							modifier = createModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD);
							break;
						case TerminalTokens.TokenNamepublic:
							modifier = createModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
							break;
						case TerminalTokens.TokenNamestatic:
							modifier = createModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);
							break;
						case TerminalTokens.TokenNameprotected:
							modifier = createModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD);
							break;
						case TerminalTokens.TokenNameprivate:
							modifier = createModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
							break;
						case TerminalTokens.TokenNamefinal:
							modifier = createModifier(Modifier.ModifierKeyword.FINAL_KEYWORD);
							break;
						case TerminalTokens.TokenNamenative:
							modifier = createModifier(Modifier.ModifierKeyword.NATIVE_KEYWORD);
							break;
						case TerminalTokens.TokenNamesynchronized:
							modifier = createModifier(Modifier.ModifierKeyword.SYNCHRONIZED_KEYWORD);
							break;
						case TerminalTokens.TokenNametransient:
							modifier = createModifier(Modifier.ModifierKeyword.TRANSIENT_KEYWORD);
							break;
						case TerminalTokens.TokenNamevolatile:
							modifier = createModifier(Modifier.ModifierKeyword.VOLATILE_KEYWORD);
							break;
						case TerminalTokens.TokenNameCOMMENT_BLOCK :
						case TerminalTokens.TokenNameCOMMENT_LINE :
						case TerminalTokens.TokenNameCOMMENT_JAVADOC :
							break;
						default :
							return;
					}
					if (modifier != null) {
						variableDecl.modifiers().add(modifier);
					}
				}
			} catch(InvalidInputException e) {
				// ignore
			}
		}
	}

	/**
	 * @param typeDecl
	 * @param typeDeclaration
	 */
	protected void setModifiers(TypeDeclaration typeDecl, org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration typeDeclaration) {
		switch(this.ast.apiLevel) {
			case AST.JLS2_INTERNAL :
				int modifiers = typeDeclaration.modifiers;
				modifiers &= ExtraCompilerModifiers.AccJustFlag;
				typeDecl.internalSetModifiers(modifiers);
				break;
			case AST.JLS3 :
				this.scanner.resetTo(typeDeclaration.declarationSourceStart, typeDeclaration.sourceStart);
				this.setModifiers(typeDecl);
		}
	}

	/**
	 * @param variableDeclarationExpression
	 * @param localDeclaration
	 */
	protected void setModifiers(VariableDeclarationExpression variableDeclarationExpression, LocalDeclaration localDeclaration) {
		switch(this.ast.apiLevel) {
			case AST.JLS2_INTERNAL :
				int modifiers = localDeclaration.modifiers & ExtraCompilerModifiers.AccJustFlag;
				modifiers &= ~ExtraCompilerModifiers.AccBlankFinal;
				variableDeclarationExpression.internalSetModifiers(modifiers);
				break;
			case AST.JLS3 :
				this.scanner.resetTo(localDeclaration.declarationSourceStart, localDeclaration.sourceStart);
				try {
					int token;
					while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
						IExtendedModifier modifier = null;
						switch(token) {
							case TerminalTokens.TokenNameabstract:
								modifier = createModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD);
								break;
							case TerminalTokens.TokenNamepublic:
								modifier = createModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
								break;
							case TerminalTokens.TokenNamestatic:
								modifier = createModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);
								break;
							case TerminalTokens.TokenNameprotected:
								modifier = createModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD);
								break;
							case TerminalTokens.TokenNameprivate:
								modifier = createModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
								break;
							case TerminalTokens.TokenNamefinal:
								modifier = createModifier(Modifier.ModifierKeyword.FINAL_KEYWORD);
								break;
							case TerminalTokens.TokenNamenative:
								modifier = createModifier(Modifier.ModifierKeyword.NATIVE_KEYWORD);
								break;
							case TerminalTokens.TokenNamesynchronized:
								modifier = createModifier(Modifier.ModifierKeyword.SYNCHRONIZED_KEYWORD);
								break;
							case TerminalTokens.TokenNametransient:
								modifier = createModifier(Modifier.ModifierKeyword.TRANSIENT_KEYWORD);
								break;
							case TerminalTokens.TokenNamevolatile:
								modifier = createModifier(Modifier.ModifierKeyword.VOLATILE_KEYWORD);
								break;
							case TerminalTokens.TokenNameCOMMENT_BLOCK :
							case TerminalTokens.TokenNameCOMMENT_LINE :
							case TerminalTokens.TokenNameCOMMENT_JAVADOC :
								break;
							default :
								return;
						}
						if (modifier != null) {
							variableDeclarationExpression.modifiers().add(modifier);
						}
					}
				} catch(InvalidInputException e) {
					// ignore
				}
		}
	}

	/**
	 * @param variableDeclarationStatement
	 * @param localDeclaration
	 */
	protected void setModifiers(VariableDeclarationStatement variableDeclarationStatement, LocalDeclaration localDeclaration) {
		switch(this.ast.apiLevel) {
			case AST.JLS2_INTERNAL :
				int modifiers = localDeclaration.modifiers & ExtraCompilerModifiers.AccJustFlag;
				modifiers &= ~ExtraCompilerModifiers.AccBlankFinal;
				variableDeclarationStatement.internalSetModifiers(modifiers);
				break;
			case AST.JLS3 :
				this.scanner.resetTo(localDeclaration.declarationSourceStart, localDeclaration.sourceStart);
				try {
					int token;
					while ((token = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
						IExtendedModifier modifier = null;
						switch(token) {
							case TerminalTokens.TokenNameabstract:
								modifier = createModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD);
								break;
							case TerminalTokens.TokenNamepublic:
								modifier = createModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
								break;
							case TerminalTokens.TokenNamestatic:
								modifier = createModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);
								break;
							case TerminalTokens.TokenNameprotected:
								modifier = createModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD);
								break;
							case TerminalTokens.TokenNameprivate:
								modifier = createModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
								break;
							case TerminalTokens.TokenNamefinal:
								modifier = createModifier(Modifier.ModifierKeyword.FINAL_KEYWORD);
								break;
							case TerminalTokens.TokenNamenative:
								modifier = createModifier(Modifier.ModifierKeyword.NATIVE_KEYWORD);
								break;
							case TerminalTokens.TokenNamesynchronized:
								modifier = createModifier(Modifier.ModifierKeyword.SYNCHRONIZED_KEYWORD);
								break;
							case TerminalTokens.TokenNametransient:
								modifier = createModifier(Modifier.ModifierKeyword.TRANSIENT_KEYWORD);
								break;
							case TerminalTokens.TokenNamevolatile:
								modifier = createModifier(Modifier.ModifierKeyword.VOLATILE_KEYWORD);
								break;
							case TerminalTokens.TokenNameCOMMENT_BLOCK :
							case TerminalTokens.TokenNameCOMMENT_LINE :
							case TerminalTokens.TokenNameCOMMENT_JAVADOC :
								break;
							default :
								return;
						}
						if (modifier != null) {
							variableDeclarationStatement.modifiers().add(modifier);
						}
					}
				} catch(InvalidInputException e) {
					// ignore
				}
		}
	}

	protected QualifiedName setQualifiedNameNameAndSourceRanges(char[][] typeName, long[] positions, org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode node) {
		int length = typeName.length;
		final SimpleName firstToken = new SimpleName(this.ast);
		firstToken.internalSetIdentifier(new String(typeName[0]));
		firstToken.index = 1;
		int start0 = (int)(positions[0]>>>32);
		int start = start0;
		int end = (int)(positions[0] & 0xFFFFFFFF);
		firstToken.setSourceRange(start, end - start + 1);
		final SimpleName secondToken = new SimpleName(this.ast);
		secondToken.internalSetIdentifier(new String(typeName[1]));
		secondToken.index = 2;
		start = (int)(positions[1]>>>32);
		end = (int)(positions[1] & 0xFFFFFFFF);
		secondToken.setSourceRange(start, end - start + 1);
		QualifiedName qualifiedName = new QualifiedName(this.ast);
		qualifiedName.setQualifier(firstToken);
		qualifiedName.setName(secondToken);
		if (this.resolveBindings) {
			recordNodes(qualifiedName, node);
			recordPendingNameScopeResolution(qualifiedName);
			recordNodes(firstToken, node);
			recordNodes(secondToken, node);
			recordPendingNameScopeResolution(firstToken);
			recordPendingNameScopeResolution(secondToken);
		}
		qualifiedName.index = 2;
		qualifiedName.setSourceRange(start0, end - start0 + 1);
		SimpleName newPart = null;
		for (int i = 2; i < length; i++) {
			newPart = new SimpleName(this.ast);
			newPart.internalSetIdentifier(new String(typeName[i]));
			newPart.index = i + 1;
			start = (int)(positions[i]>>>32);
			end = (int)(positions[i] & 0xFFFFFFFF);
			newPart.setSourceRange(start,  end - start + 1);
			QualifiedName qualifiedName2 = new QualifiedName(this.ast);
			qualifiedName2.setQualifier(qualifiedName);
			qualifiedName2.setName(newPart);
			qualifiedName = qualifiedName2;
			qualifiedName.index = newPart.index;
			qualifiedName.setSourceRange(start0, end - start0 + 1);
			if (this.resolveBindings) {
				recordNodes(qualifiedName, node);
				recordNodes(newPart, node);
				recordPendingNameScopeResolution(qualifiedName);
				recordPendingNameScopeResolution(newPart);
			}
		}
		QualifiedName name = qualifiedName;
		if (this.resolveBindings) {
			recordNodes(name, node);
			recordPendingNameScopeResolution(name);
		}
		return name;
	}

	protected QualifiedName setQualifiedNameNameAndSourceRanges(char[][] typeName, long[] positions, int endingIndex, org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode node) {
 		int length = endingIndex + 1;
		final SimpleName firstToken = new SimpleName(this.ast);
		firstToken.internalSetIdentifier(new String(typeName[0]));
		firstToken.index = 1;
		int start0 = (int)(positions[0]>>>32);
		int start = start0;
		int end = (int) positions[0];
		firstToken.setSourceRange(start, end - start + 1);
		final SimpleName secondToken = new SimpleName(this.ast);
		secondToken.internalSetIdentifier(new String(typeName[1]));
		secondToken.index = 2;
		start = (int)(positions[1]>>>32);
		end = (int) positions[1];
		secondToken.setSourceRange(start, end - start + 1);
		QualifiedName qualifiedName = new QualifiedName(this.ast);
		qualifiedName.setQualifier(firstToken);
		qualifiedName.setName(secondToken);
		if (this.resolveBindings) {
			recordNodes(qualifiedName, node);
			recordPendingNameScopeResolution(qualifiedName);
			recordNodes(firstToken, node);
			recordNodes(secondToken, node);
			recordPendingNameScopeResolution(firstToken);
			recordPendingNameScopeResolution(secondToken);
		}
		qualifiedName.index = 2;
		qualifiedName.setSourceRange(start0, end - start0 + 1);
		SimpleName newPart = null;
		for (int i = 2; i < length; i++) {
			newPart = new SimpleName(this.ast);
			newPart.internalSetIdentifier(new String(typeName[i]));
			newPart.index = i + 1;
			start = (int)(positions[i]>>>32);
			end = (int) positions[i];
			newPart.setSourceRange(start,  end - start + 1);
			QualifiedName qualifiedName2 = new QualifiedName(this.ast);
			qualifiedName2.setQualifier(qualifiedName);
			qualifiedName2.setName(newPart);
			qualifiedName = qualifiedName2;
			qualifiedName.index = newPart.index;
			qualifiedName.setSourceRange(start0, end - start0 + 1);
			if (this.resolveBindings) {
				recordNodes(qualifiedName, node);
				recordNodes(newPart, node);
				recordPendingNameScopeResolution(qualifiedName);
				recordPendingNameScopeResolution(newPart);
			}
		}
        if (newPart == null && this.resolveBindings) {
            recordNodes(qualifiedName, node);
            recordPendingNameScopeResolution(qualifiedName);
        }
		return qualifiedName;
	}


	protected void setTypeForField(FieldDeclaration fieldDeclaration, Type type, int extraDimension) {
		if (extraDimension != 0) {
			if (type.isArrayType()) {
				ArrayType arrayType = (ArrayType) type;
				int remainingDimensions = arrayType.getDimensions() - extraDimension;
				if (remainingDimensions == 0)  {
					// the dimensions are after the name so the type of the fieldDeclaration is a simpleType
					Type elementType = arrayType.getElementType();
					// cut the child loose from its parent (without creating garbage)
					elementType.setParent(null, null);
					this.ast.getBindingResolver().updateKey(type, elementType);
					fieldDeclaration.setType(elementType);
				} else {
					int start = type.getStartPosition();
					ArrayType subarrayType = arrayType;
					int index = extraDimension;
					while (index > 0) {
						subarrayType = (ArrayType) subarrayType.getComponentType();
						index--;
					}
					int end = retrieveProperRightBracketPosition(remainingDimensions, start);
					subarrayType.setSourceRange(start, end - start + 1);
					// cut the child loose from its parent (without creating garbage)
					subarrayType.setParent(null, null);
					fieldDeclaration.setType(subarrayType);
					updateInnerPositions(subarrayType, remainingDimensions);
					this.ast.getBindingResolver().updateKey(type, subarrayType);
				}
			} else {
				fieldDeclaration.setType(type);
			}
		} else {
			if (type.isArrayType()) {
				// update positions of the component types of the array type
				int dimensions = ((ArrayType) type).getDimensions();
				updateInnerPositions(type, dimensions);
			}
			fieldDeclaration.setType(type);
		}
	}

	protected void setTypeForMethodDeclaration(FunctionDeclaration methodDeclaration, Type type, int extraDimension) {
		if (extraDimension != 0) {
			if (type.isArrayType()) {
				ArrayType arrayType = (ArrayType) type;
				int remainingDimensions = arrayType.getDimensions() - extraDimension;
				if (remainingDimensions == 0)  {
					// the dimensions are after the name so the type of the fieldDeclaration is a simpleType
					Type elementType = arrayType.getElementType();
					// cut the child loose from its parent (without creating garbage)
					elementType.setParent(null, null);
					this.ast.getBindingResolver().updateKey(type, elementType);
					switch(this.ast.apiLevel) {
						case AST.JLS2_INTERNAL :
							methodDeclaration.internalSetReturnType(elementType);
							break;
						case AST.JLS3 :
							methodDeclaration.setReturnType2(elementType);
						break;
					}
				} else {
					int start = type.getStartPosition();
					ArrayType subarrayType = arrayType;
					int index = extraDimension;
					while (index > 0) {
						subarrayType = (ArrayType) subarrayType.getComponentType();
						index--;
					}
					int end = retrieveProperRightBracketPosition(remainingDimensions, start);
					subarrayType.setSourceRange(start, end - start + 1);
					// cut the child loose from its parent (without creating garbage)
					subarrayType.setParent(null, null);
					updateInnerPositions(subarrayType, remainingDimensions);
					switch(this.ast.apiLevel) {
						case AST.JLS2_INTERNAL :
							methodDeclaration.internalSetReturnType(subarrayType);
							break;
						case AST.JLS3 :
							methodDeclaration.setReturnType2(subarrayType);
						break;
					}
					this.ast.getBindingResolver().updateKey(type, subarrayType);
				}
			} else {
				switch(this.ast.apiLevel) {
					case AST.JLS2_INTERNAL :
						methodDeclaration.internalSetReturnType(type);
						break;
					case AST.JLS3 :
						methodDeclaration.setReturnType2(type);
					break;
				}
			}
		} else {
			switch(this.ast.apiLevel) {
				case AST.JLS2_INTERNAL :
					methodDeclaration.internalSetReturnType(type);
					break;
				case AST.JLS3 :
					methodDeclaration.setReturnType2(type);
				break;
			}
		}
	}

	protected void setTypeForSingleVariableDeclaration(SingleVariableDeclaration singleVariableDeclaration, Type type, int extraDimension) {
		if (extraDimension != 0) {
			if (type.isArrayType()) {
				ArrayType arrayType = (ArrayType) type;
				int remainingDimensions = arrayType.getDimensions() - extraDimension;
				if (remainingDimensions == 0)  {
					// the dimensions are after the name so the type of the fieldDeclaration is a simpleType
					Type elementType = arrayType.getElementType();
					// cut the child loose from its parent (without creating garbage)
					elementType.setParent(null, null);
					this.ast.getBindingResolver().updateKey(type, elementType);
					singleVariableDeclaration.setType(elementType);
				} else {
					int start = type.getStartPosition();
					ArrayType subarrayType = arrayType;
					int index = extraDimension;
					while (index > 0) {
						subarrayType = (ArrayType) subarrayType.getComponentType();
						index--;
					}
					int end = retrieveProperRightBracketPosition(remainingDimensions, start);
					subarrayType.setSourceRange(start, end - start + 1);
					// cut the child loose from its parent (without creating garbage)
					subarrayType.setParent(null, null);
					updateInnerPositions(subarrayType, remainingDimensions);
					singleVariableDeclaration.setType(subarrayType);
					this.ast.getBindingResolver().updateKey(type, subarrayType);
				}
			} else {
				singleVariableDeclaration.setType(type);
			}
		} else {
			singleVariableDeclaration.setType(type);
		}
	}

	protected void setTypeForVariableDeclarationExpression(VariableDeclarationExpression variableDeclarationExpression, Type type, int extraDimension) {
		if (extraDimension != 0) {
			if (type.isArrayType()) {
				ArrayType arrayType = (ArrayType) type;
				int remainingDimensions = arrayType.getDimensions() - extraDimension;
				if (remainingDimensions == 0)  {
					// the dimensions are after the name so the type of the fieldDeclaration is a simpleType
					Type elementType = arrayType.getElementType();
					// cut the child loose from its parent (without creating garbage)
					elementType.setParent(null, null);
					this.ast.getBindingResolver().updateKey(type, elementType);
					variableDeclarationExpression.setType(elementType);
				} else {
					int start = type.getStartPosition();
					ArrayType subarrayType = arrayType;
					int index = extraDimension;
					while (index > 0) {
						subarrayType = (ArrayType) subarrayType.getComponentType();
						index--;
					}
					int end = retrieveProperRightBracketPosition(remainingDimensions, start);
					subarrayType.setSourceRange(start, end - start + 1);
					// cut the child loose from its parent (without creating garbage)
					subarrayType.setParent(null, null);
					updateInnerPositions(subarrayType, remainingDimensions);
					variableDeclarationExpression.setType(subarrayType);
					this.ast.getBindingResolver().updateKey(type, subarrayType);
				}
			} else {
				variableDeclarationExpression.setType(type);
			}
		} else {
			variableDeclarationExpression.setType(type);
		}
	}

	protected void setTypeForVariableDeclarationStatement(VariableDeclarationStatement variableDeclarationStatement, Type type, int extraDimension) {
		if (extraDimension != 0) {
			if (type.isArrayType()) {
				ArrayType arrayType = (ArrayType) type;
				int remainingDimensions = arrayType.getDimensions() - extraDimension;
				if (remainingDimensions == 0)  {
					// the dimensions are after the name so the type of the fieldDeclaration is a simpleType
					Type elementType = arrayType.getElementType();
					// cut the child loose from its parent (without creating garbage)
					elementType.setParent(null, null);
					this.ast.getBindingResolver().updateKey(type, elementType);
					variableDeclarationStatement.setType(elementType);
				} else {
					int start = type.getStartPosition();
					ArrayType subarrayType = arrayType;
					int index = extraDimension;
					while (index > 0) {
						subarrayType = (ArrayType) subarrayType.getComponentType();
						index--;
					}
					int end = retrieveProperRightBracketPosition(remainingDimensions, start);
					subarrayType.setSourceRange(start, end - start + 1);
					// cut the child loose from its parent (without creating garbage)
					subarrayType.setParent(null, null);
					updateInnerPositions(subarrayType, remainingDimensions);
					variableDeclarationStatement.setType(subarrayType);
					this.ast.getBindingResolver().updateKey(type, subarrayType);
				}
			} else {
				variableDeclarationStatement.setType(type);
			}
		} else {
			variableDeclarationStatement.setType(type);
		}
	}

	protected void updateInnerPositions(Type type, int dimensions) {
		if (dimensions > 1) {
			// need to set positions for intermediate array type see 42839
			int start = type.getStartPosition();
			Type currentComponentType = ((ArrayType) type).getComponentType();
			int searchedDimension = dimensions - 1;
			int rightBracketEndPosition = start;
			while (currentComponentType.isArrayType()) {
				rightBracketEndPosition = retrieveProperRightBracketPosition(searchedDimension, start);
				currentComponentType.setSourceRange(start, rightBracketEndPosition - start + 1);
				currentComponentType = ((ArrayType) currentComponentType).getComponentType();
				searchedDimension--;
			}
		}
	}
}

