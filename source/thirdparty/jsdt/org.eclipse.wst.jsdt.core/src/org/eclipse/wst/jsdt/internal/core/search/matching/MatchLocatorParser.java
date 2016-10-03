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

import org.eclipse.wst.jsdt.core.infer.InferredAttribute;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Initializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocArgumentExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocFieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocMessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocSingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.NameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement;
import org.eclipse.wst.jsdt.internal.compiler.ast.Reference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;

/**
 * A parser that locates ast nodes that match a given search pattern.
 */
public class MatchLocatorParser extends Parser {

MatchingNodeSet nodeSet;
PatternLocator patternLocator;
private ASTVisitor localDeclarationVisitor;

public static MatchLocatorParser createParser(ProblemReporter problemReporter, MatchLocator locator) {
	return new MatchLocatorParser(problemReporter, locator);
}

/**
 * An ast visitor that visits local type declarations.
 */
public class NoClassNoMethodDeclarationVisitor extends ASTVisitor {
	public boolean visit(ConstructorDeclaration constructorDeclaration, Scope scope) {
		return (constructorDeclaration.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type
	}
	public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		return (fieldDeclaration.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type;
	}
	public boolean visit(Initializer initializer, MethodScope scope) {
		return (initializer.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type
	}
	public boolean visit(MethodDeclaration methodDeclaration, Scope scope) {
		return (methodDeclaration.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type
	}
}
public class MethodButNoClassDeclarationVisitor extends NoClassNoMethodDeclarationVisitor {
	public boolean visit(TypeDeclaration localTypeDeclaration, Scope scope) {
		patternLocator.match(localTypeDeclaration, nodeSet);
		return true;
	}
	public boolean visit(InferredType localTypeDeclaration, BlockScope scope) {
		patternLocator.match(localTypeDeclaration, nodeSet);
		return true;
	}
}
public class ClassButNoMethodDeclarationVisitor extends ASTVisitor {
	public boolean visit(ConstructorDeclaration constructorDeclaration, Scope scope) {
		patternLocator.match(constructorDeclaration, nodeSet);
		return (constructorDeclaration.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type
	}
	public boolean visit(FieldDeclaration fieldDeclaration, Scope scope) {
		patternLocator.match(fieldDeclaration, nodeSet);
		return (fieldDeclaration.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type;
	}
	public boolean visit(InferredAttribute field, BlockScope scope) {
		patternLocator.match(field, nodeSet);
		return false; // continue only if it has local type;
	}
	public boolean visit(Initializer initializer, MethodScope scope) {
		patternLocator.match(initializer, nodeSet);
		return (initializer.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type
	}
	public boolean visit(TypeDeclaration memberTypeDeclaration, Scope scope) {
		patternLocator.match(memberTypeDeclaration, nodeSet);
		return true;
	}
	public boolean visit(MethodDeclaration methodDeclaration, Scope scope) {
		patternLocator.match(methodDeclaration, nodeSet);
		return (methodDeclaration.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type
	}
	public boolean visit(InferredMethod inferredMethod, BlockScope scope) {
		patternLocator.match(inferredMethod, nodeSet);
		return false;
	}
}
public class ClassAndMethodDeclarationVisitor extends ClassButNoMethodDeclarationVisitor {
	public boolean visit(TypeDeclaration localTypeDeclaration, Scope scope) {
		patternLocator.match(localTypeDeclaration, nodeSet);
		return true;
	}
	public boolean visit(InferredType localTypeDeclaration, BlockScope scope) {
		patternLocator.match(localTypeDeclaration, nodeSet);
		return true;
	}
}

protected MatchLocatorParser(ProblemReporter problemReporter, MatchLocator locator) {
	super(problemReporter, true);
	this.reportOnlyOneSyntaxError = true;
	this.patternLocator = locator.patternLocator;
	if ((locator.matchContainer & PatternLocator.CLASS_CONTAINER) != 0) {
		this.localDeclarationVisitor = (locator.matchContainer & PatternLocator.METHOD_CONTAINER) != 0
			? new ClassAndMethodDeclarationVisitor()
			: new ClassButNoMethodDeclarationVisitor();
	} else {
		this.localDeclarationVisitor = (locator.matchContainer & PatternLocator.METHOD_CONTAINER) != 0
			? new MethodButNoClassDeclarationVisitor()
			: new NoClassNoMethodDeclarationVisitor();
	}
}
public void checkComment() {
	super.checkComment();
	if (this.javadocParser.checkDocComment && this.javadoc != null) {

		// Search for pattern locator matches in javadoc comment parameters @param tags
		JavadocSingleNameReference[] paramReferences = this.javadoc.paramReferences;
		if (paramReferences != null) {
			for (int i=0, length=paramReferences.length; i < length; i++) {
				this.patternLocator.match(paramReferences[i], this.nodeSet);
			}
		}

		// Search for pattern locator matches in javadoc comment @throws/@exception tags
		TypeReference[] thrownExceptions = this.javadoc.exceptionReferences;
		if (thrownExceptions != null) {
			for (int i=0, length=thrownExceptions.length; i < length; i++) {
				this.patternLocator.match(thrownExceptions[i], this.nodeSet);
			}
		}

		// Search for pattern locator matches in javadoc comment @see tags
		Expression[] references = this.javadoc.seeReferences;
		if (references != null) {
			for (int i=0, length=references.length; i < length; i++) {
				Expression reference = references[i];
				if (reference instanceof TypeReference) {
					TypeReference typeRef = (TypeReference) reference;
					this.patternLocator.match(typeRef, this.nodeSet);
				} else if (reference instanceof JavadocFieldReference) {
					JavadocFieldReference fieldRef = (JavadocFieldReference) reference;
					this.patternLocator.match(fieldRef, this.nodeSet);
					if (fieldRef.receiver instanceof TypeReference && !fieldRef.receiver.isThis()) {
						TypeReference typeRef = (TypeReference) fieldRef.receiver;
						this.patternLocator.match(typeRef, this.nodeSet);
					}
				} else if (reference instanceof JavadocMessageSend) {
					JavadocMessageSend messageSend = (JavadocMessageSend) reference;
					this.patternLocator.match(messageSend, this.nodeSet);
					if (messageSend.receiver instanceof TypeReference && !messageSend.receiver.isThis()) {
						TypeReference typeRef = (TypeReference) messageSend.receiver;
						this.patternLocator.match(typeRef, this.nodeSet);
					}
					if (messageSend.arguments != null) {
						for (int a=0,al=messageSend.arguments.length; a<al; a++) {
							JavadocArgumentExpression argument = (JavadocArgumentExpression) messageSend.arguments[a];
							if (argument.argument != null && argument.argument.type != null) {
								this.patternLocator.match(argument.argument.type, this.nodeSet);
							}
						}
					}
				} else if (reference instanceof JavadocAllocationExpression) {
					JavadocAllocationExpression constructor = (JavadocAllocationExpression) reference;
					this.patternLocator.match(constructor, this.nodeSet);
					if (constructor.type != null && !constructor.type.isThis()) {
						this.patternLocator.match(constructor.type, this.nodeSet);
					}
					if (constructor.arguments != null) {
						for (int a=0,al=constructor.arguments.length; a<al; a++) {
							this.patternLocator.match(constructor.arguments[a], this.nodeSet);
							JavadocArgumentExpression argument = (JavadocArgumentExpression) constructor.arguments[a];
							if (argument.argument != null && argument.argument.type != null) {
								this.patternLocator.match(argument.argument.type, this.nodeSet);
							}
						}
					}
				}
			}
		}
	}
}
protected void classInstanceCreation(boolean alwaysQualified, boolean isShort) {
	super.classInstanceCreation(alwaysQualified, isShort);
	this.patternLocator.match(this.expressionStack[this.expressionPtr], this.nodeSet);
}
protected void consumeAssignment() {
	super.consumeAssignment();
	this.patternLocator.match(this.expressionStack[this.expressionPtr], this.nodeSet);
}
protected void consumeCallExpressionWithSimpleName() {
	super.consumeCallExpressionWithSimpleName();

	// this is always a Reference
	this.patternLocator.match((Reference) this.expressionStack[this.expressionPtr], this.nodeSet);
}
protected void consumeMemberExpressionWithSimpleName() {
	super.consumeMemberExpressionWithSimpleName();

	// this is always a Reference
	this.patternLocator.match((Reference) this.expressionStack[this.expressionPtr], this.nodeSet);
}
protected void consumeFormalParameter(boolean isVarArgs) {
	super.consumeFormalParameter(isVarArgs);

	// this is always a LocalDeclaration
	this.patternLocator.match((LocalDeclaration) this.astStack[this.astPtr], this.nodeSet);
}
protected void consumeLocalVariableDeclaration() {
	super.consumeLocalVariableDeclaration();

	// this is always a LocalDeclaration
	this.patternLocator.match((LocalDeclaration) this.astStack[this.astPtr], this.nodeSet);
}
protected void consumeCallExpressionWithArguments() {
	super.consumeCallExpressionWithArguments();

	// this is always a MessageSend
	this.patternLocator.match((MessageSend) this.expressionStack[this.expressionPtr], this.nodeSet);
}
protected void consumePrimaryNoNewArray() {
	// pop parenthesis positions (and don't update expression positions
	// (see http://bugs.eclipse.org/bugs/show_bug.cgi?id=23329)
	intPtr--;
	intPtr--;
}

protected void consumeUnaryExpression(int op, boolean post) {
	super.consumeUnaryExpression(op, post);
	this.patternLocator.match(this.expressionStack[this.expressionPtr], this.nodeSet);
}
protected TypeReference copyDims(TypeReference typeRef, int dim) {
	TypeReference result = super.copyDims(typeRef, dim);
	 if (this.nodeSet.removePossibleMatch(typeRef) != null)
		this.nodeSet.addPossibleMatch(result);
	 else if (this.nodeSet.removeTrustedMatch(typeRef) != null)
		this.nodeSet.addTrustedMatch(result, true);
	return result;
}
protected TypeReference getTypeReference(int dim) {
	TypeReference typeRef = super.getTypeReference(dim);
	this.patternLocator.match(typeRef, this.nodeSet); // NB: Don't check container since type reference can happen anywhere
	return typeRef;
}
protected NameReference getUnspecifiedReference() {
	NameReference nameRef = super.getUnspecifiedReference();
	this.patternLocator.match(nameRef, this.nodeSet); // NB: Don't check container since unspecified reference can happen anywhere
	return nameRef;
}
protected NameReference getUnspecifiedReferenceOptimized() {
	NameReference nameRef = super.getUnspecifiedReferenceOptimized();
	this.patternLocator.match(nameRef, this.nodeSet); // NB: Don't check container since unspecified reference can happen anywhere
	return nameRef;
}
/**
 * Parses the method bodies in the given compilation unit
 * @param unit CompilationUnitDeclaration
 */
public void parseBodies(CompilationUnitDeclaration unit) {
	TypeDeclaration[] types = unit.types;
	if (types != null)
	  for (int i = 0; i < types.length; i++) {
		TypeDeclaration type = types[i];
		this.patternLocator.match(type, this.nodeSet);
		this.parseBodies(type, unit);
	}


	ProgramElement[] statements = unit.statements;
	if (statements != null)
 	  for (int i = 0; i < statements.length; i++) {
		if (statements[i] instanceof LocalDeclaration){
			((LocalDeclaration)statements[i]).traverse(localDeclarationVisitor, null);
			if (patternLocator instanceof FieldLocator) {
				((FieldLocator)patternLocator).matchLocalDeclaration((LocalDeclaration)statements[i], this.nodeSet);
			}
		}
		
		//check if the statement contains a method declaration
		AbstractMethodDeclaration methodDeclaration = AbstractMethodDeclaration.findMethodDeclaration(statements[i]);
		if (methodDeclaration != null && methodDeclaration instanceof MethodDeclaration) {
			methodDeclaration.traverse(localDeclarationVisitor, (Scope) null);
			if (this.patternLocator instanceof MethodLocator) {
				((MethodLocator)this.patternLocator).match((MethodDeclaration)methodDeclaration, this.nodeSet);
			}
		}
	}

	unit.traverseInferredTypes(localDeclarationVisitor,  null);
}
/**
 * Parses the member bodies in the given type.
 * @param type TypeDeclaration
 * @param unit CompilationUnitDeclaration
 */
protected void parseBodies(TypeDeclaration type, CompilationUnitDeclaration unit) {
	FieldDeclaration[] fields = type.fields;
	if (fields != null) {
		for (int i = 0; i < fields.length; i++) {
			FieldDeclaration field = fields[i];
			if (field instanceof Initializer)
				this.parse((Initializer) field, type, unit);
			field.traverse(localDeclarationVisitor, null);
		}
	}

	AbstractMethodDeclaration[] methods = type.methods;
	if (methods != null) {
		for (int i = 0; i < methods.length; i++) {
			AbstractMethodDeclaration method = methods[i];
			if (method.sourceStart >= type.bodyStart) { // if not synthetic
				if (method instanceof MethodDeclaration) {
					MethodDeclaration methodDeclaration = (MethodDeclaration) method;
					this.parse(methodDeclaration, unit);
					methodDeclaration.traverse(localDeclarationVisitor, (Scope) null);
				} else if (method instanceof ConstructorDeclaration) {
					ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) method;
					this.parse(constructorDeclaration, unit);
					constructorDeclaration.traverse(localDeclarationVisitor, (Scope) null);
				}
			} else if (method.isDefaultConstructor()) {
				method.parseStatements(this, unit);
			}
		}
	}

//	TypeDeclaration[] memberTypes = type.memberTypes;
//	if (memberTypes != null) {
//		for (int i = 0; i < memberTypes.length; i++) {
//			TypeDeclaration memberType = memberTypes[i];
//			this.parseBodies(memberType, unit);
//			memberType.traverse(localDeclarationVisitor, (Scope) null);
//		}
//	}
}



}

