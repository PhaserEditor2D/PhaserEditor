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
package org.eclipse.wst.jsdt.internal.compiler.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IFunctionExpression;
import org.eclipse.wst.jsdt.core.ast.IProgramElement;
import org.eclipse.wst.jsdt.core.ast.IScriptFileDeclaration;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.IInferenceFile;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.DelegateASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.compiler.parser.NLSTag;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortMethod;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortType;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;

public class CompilationUnitDeclaration
	extends ASTNode
	implements ProblemSeverities, ReferenceContext, IScriptFileDeclaration, IInferenceFile {

	private static final Comparator STRING_LITERAL_COMPARATOR = new Comparator() {
		public int compare(Object o1, Object o2) {
			StringLiteral literal1 = (StringLiteral) o1;
			StringLiteral literal2 = (StringLiteral) o2;
			return literal1.sourceStart - literal2.sourceStart;
		}
	};
	private static final int STRING_LITERALS_INCREMENT = 10;

	public ImportReference currentPackage;
	public ImportReference[] imports;
	public TypeDeclaration[] types;
	public ProgramElement[] statements;
	public int[][] comments;


	public InferredType [] inferredTypes = new InferredType[10];
	public int numberInferredTypes=0;
	public HashtableOfObject inferredTypesHash=new HashtableOfObject();
	public boolean typesHaveBeenInferred=false;

	public boolean ignoreFurtherInvestigation = false;	// once pointless to investigate due to errors
	public boolean ignoreMethodBodies = false;
	public CompilationUnitScope scope;
	public ProblemReporter problemReporter;
	public CompilationResult compilationResult;


	public LocalTypeBinding[] localTypes;
	public int localTypeCount = 0;

	public CompilationUnitBinding compilationUnitBinding;


	public boolean isPropagatingInnerClassEmulation;

	public Javadoc javadoc; // 1.5 addition for package-info.js

	public NLSTag[] nlsTags;
	private StringLiteral[] stringLiterals;
	private int stringLiteralsPtr;



	public CompilationUnitDeclaration(
		ProblemReporter problemReporter,
		CompilationResult compilationResult,
		int sourceLength) {

		this.problemReporter = problemReporter;
		this.compilationResult = compilationResult;

		//by definition of a compilation unit....
		sourceStart = 0;
		sourceEnd = sourceLength - 1;
//		System.out.println("create "+hashCode());
	}

	/*
	 *	We cause the compilation task to abort to a given extent.
	 */
	public void abort(int abortLevel, CategorizedProblem problem) {

		switch (abortLevel) {
			case AbortType :
				throw new AbortType(this.compilationResult, problem);
			case AbortMethod :
				throw new AbortMethod(this.compilationResult, problem);
			default :
				throw new AbortCompilationUnit(this.compilationResult, problem);
		}
	}

	/*
	 * Dispatch code analysis AND request saturation of inner emulation
	 */
	public void analyseCode() {

		if (ignoreFurtherInvestigation )
			return;
		try {
			if (types != null) {
				for (int i = 0, count = types.length; i < count; i++) {
					types[i].analyseCode(scope);
				}
			}

			this.scope.temporaryAnalysisIndex=0;
			int maxVars=this.scope.localIndex;
			for (Iterator iter = this.scope.externalCompilationUnits.iterator(); iter.hasNext();) {
				CompilationUnitScope externalScope = (CompilationUnitScope) iter.next();
				externalScope.temporaryAnalysisIndex=maxVars;
				maxVars+=externalScope.localIndex;
			}
			FlowInfo flowInfo=FlowInfo.initial(maxVars);
			FlowContext flowContext = new FlowContext(null, this);

			if (statements != null) {
				List functions = null;
				for (int i = 0, length = this.statements.length; i < length; i++) {
					// if this is not a function then analyse it
					if(!(this.statements[i] instanceof AbstractMethodDeclaration)) {
						flowInfo=((Statement)statements[i]).analyseCode(scope,flowContext,flowInfo);
					} else {
						// if this is a function then store it until all non functions are finished
						if(functions == null)
							functions = new ArrayList();
						functions.add(statements[i]);
					}
				}
				if(functions != null) {
					for(int f = 0; f < functions.size(); f++) {
						((Statement)functions.get(f)).analyseCode(this.scope, null, flowInfo.copy());
					}
				}
				
//				for (int i = 0, count = statements.length; i < count; i++) {
//					if (statements[i] instanceof  AbstractMethodDeclaration)
//					{
//						((AbstractMethodDeclaration)statements[i]).analyseCode(this.scope, null, flowInfo.copy());
//					}
//					else
//					flowInfo=((Statement)statements[i]).analyseCode(scope,flowContext,flowInfo);
//				}
			}
		} catch (AbortCompilationUnit e) {
			this.ignoreFurtherInvestigation = true;
			return;
		}
	}

	/*
	 * When unit result is about to be accepted, removed back pointers
	 * to compiler structures.
	 */
	public void cleanUp() {
		if (this.compilationUnitBinding!=null)

			this.compilationUnitBinding.cleanup();
		if (this.types != null) {
			for (int i = 0, max = this.types.length; i < max; i++) {
				cleanUp(this.types[i]);
			}
			for (int i = 0, max = this.localTypeCount; i < max; i++) {
			    LocalTypeBinding localType = localTypes[i];
				// null out the type's scope backpointers
				localType.scope = null; // local members are already in the list
				localType.enclosingCase = null;
			}
		}

		for (int i = 0; i < this.numberInferredTypes; i++) {
			SourceTypeBinding binding = this.inferredTypes[i].binding;
			if (binding!=null)
				binding.cleanup();
		}
		compilationResult.recoveryScannerData = null; // recovery is already done


	}
	private void cleanUp(TypeDeclaration type) {
		if (type.memberTypes != null) {
			for (int i = 0, max = type.memberTypes.length; i < max; i++){
				cleanUp(type.memberTypes[i]);
			}
		}
		if (type.binding != null) {
			// null out the type's scope backpointers
			type.binding.scope = null;
		}
	}

	public CompilationResult compilationResult() {
		return this.compilationResult;
	}

	/*
	 * Finds the matching type amoung this compilation unit types.
	 * Returns null if no type with this name is found.
	 * The type name is a compound name
	 * eg. if we're looking for X.A.B then a type name would be {X, A, B}
	 */
	public TypeDeclaration declarationOfType(char[][] typeName) {

		for (int i = 0; i < this.types.length; i++) {
			TypeDeclaration typeDecl = this.types[i].declarationOfType(typeName);
			if (typeDecl != null) {
				return typeDecl;
			}
		}
		return null;
	}


	/**
	 * <p>Find the {@link AbstractMethodDeclaration} for a given {@link MethodBinding} by searching
	 * all of the statements of this {@link CompilationUnitDeclaration} including inside of {@link MethodDeclaration}
	 * contained in this {@link CompilationUnitDeclaration}.</p>
	 * 
	 * @param methodBinding
	 * @return
	 */
	public AbstractMethodDeclaration declarationOf(MethodBinding methodBinding) {
		return declarationOf(methodBinding, this.statements);
	}
	
	/**
	 * <p>
	 * Internal helper method to find the {@link AbstractMethodDeclaration}
	 * for a given {@link MethodBinding} by searching a given set of
	 * statements.
	 * </p>
	 * 
	 * @param methodBinding
	 *            {@link MethodBinding} to find the
	 *            {@link AbstractMethodDeclaration} for
	 * @param originalStatements
	 *            statements to search for the
	 *            {@link AbstractMethodDeclaration} in
	 * 
	 * @return {@link AbstractMethodDeclaration} for the given
	 *         {@link MethodBinding} found in the given {@link ProgramElement}s,
	 *         or <code>null</code> if it could not be found
	 */
	private static AbstractMethodDeclaration declarationOf(MethodBinding methodBinding, ProgramElement[] originalStatements) {
		if (methodBinding != null && originalStatements != null) {
			List statements = new ArrayList(originalStatements.length);
			statements.addAll(Arrays.asList(originalStatements));
			
			for (int i = 0; i < statements.size(); i++) {
				IProgramElement statement = (IProgramElement)statements.get(i);
				if (statement instanceof MessageSend) {
					MessageSend msgSend = (MessageSend) statement;
					
					//search arguments of message send
					if (msgSend.arguments != null) {
						statements.addAll(Arrays.asList(msgSend.arguments));
					}
					
					/* add anonymous function message send
					 * 
					 * function() { foo = "test" }(); */
					if(msgSend.receiver instanceof IFunctionExpression) {
						statements.add(msgSend.receiver);
					}
					
					continue;
				} else if(statement instanceof ObjectLiteral) {
					ObjectLiteral objLit = (ObjectLiteral) statement;
					if(objLit.fields != null) {
						statements.addAll(Arrays.asList(objLit.fields));
					}
					continue;
				} else if(statement instanceof ObjectLiteralField) {
					ObjectLiteralField objLitField = (ObjectLiteralField) statement;
					if(objLitField.initializer != null && (objLitField.initializer instanceof ObjectLiteral || objLitField.initializer instanceof FunctionExpression)) {
						statements.add(objLitField.initializer);
						continue;
					}
				}
				
				AbstractMethodDeclaration methodDecl = AbstractMethodDeclaration.findMethodDeclaration(statement);
				
				//check statements inside of method declarations as well
				if(methodDecl != null && methodDecl.statements != null) {
					statements.addAll(Arrays.asList(methodDecl.statements));
				}
				
				//check if the found method declaration is the one that is being searched for
				if (methodDecl != null && (methodDecl.getBinding() == methodBinding || methodDecl.getBinding() == methodBinding.original())) {
					return methodDecl;
				}
			}
		}
		return null;
	}

	public char[] getFileName() {

		return compilationResult.getFileName();
	}

	public char[] getMainTypeName() {

		if (compilationResult.compilationUnit == null) {
			char[] fileName = compilationResult.getFileName();

			int start = CharOperation.lastIndexOf('/', fileName) + 1;
			if (start == 0 || start < CharOperation.lastIndexOf('\\', fileName))
				start = CharOperation.lastIndexOf('\\', fileName) + 1;

			int end = CharOperation.lastIndexOf('.', fileName);
			if (end == -1)
				end = fileName.length;

			return CharOperation.subarray(fileName, start, end);
		} else {
			return compilationResult.compilationUnit.getMainTypeName();
		}
	}

	public boolean isEmpty() {

		return (currentPackage == null) && (imports == null) && (types == null) && (statements==null);
	}

	public boolean isPackageInfo() {
		return CharOperation.equals(this.getMainTypeName(), TypeConstants.PACKAGE_INFO_NAME);
	}

	public boolean hasErrors() {
		return this.ignoreFurtherInvestigation;
	}

	public StringBuffer print(int indent, StringBuffer output) {

		if (currentPackage != null) {
			printIndent(indent, output).append("package "); //$NON-NLS-1$
			currentPackage.print(0, output, false).append(";\n"); //$NON-NLS-1$
		}
		if (imports != null)
			for (int i = 0; i < imports.length; i++) {
				if (imports[i].isInternal())
					continue;
				printIndent(indent, output).append("import "); //$NON-NLS-1$
				ImportReference currentImport = imports[i];
				currentImport.print(0, output).append(";\n"); //$NON-NLS-1$
			}

		if (types != null) {
			for (int i = 0; i < types.length; i++) {
				types[i].print(indent, output).append("\n"); //$NON-NLS-1$
			}
		}
		if (statements != null) {
			for (int i = 0; i < statements.length; i++) {
				statements[i].printStatement(indent, output).append("\n"); //$NON-NLS-1$
			}
		}
		return output;
	}

	public void recordStringLiteral(StringLiteral literal) {
		if (this.stringLiterals == null) {
			this.stringLiterals = new StringLiteral[STRING_LITERALS_INCREMENT];
			this.stringLiteralsPtr = 0;
		} else {
			int stackLength = this.stringLiterals.length;
			if (this.stringLiteralsPtr == stackLength) {
				System.arraycopy(
					this.stringLiterals,
					0,
					this.stringLiterals = new StringLiteral[stackLength + STRING_LITERALS_INCREMENT],
					0,
					stackLength);
			}
		}
		this.stringLiterals[this.stringLiteralsPtr++] = literal;
	}

	/*
	 * Keep track of all local types, so as to update their innerclass
	 * emulation later on.
	 */
	public void record(LocalTypeBinding localType) {

		if (this.localTypeCount == 0) {
			this.localTypes = new LocalTypeBinding[5];
		} else if (this.localTypeCount == this.localTypes.length) {
			System.arraycopy(this.localTypes, 0, (this.localTypes = new LocalTypeBinding[this.localTypeCount * 2]), 0, this.localTypeCount);
		}
		this.localTypes[this.localTypeCount++] = localType;
	}

	public void resolve() {
		int startingTypeIndex = 0;
		boolean isPackageInfo =  false;//isPackageInfo();
		if (this.types != null && isPackageInfo) {
            // resolve synthetic type declaration
			final TypeDeclaration syntheticTypeDeclaration = types[0];
			// set empty javadoc to avoid missing warning (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=95286)
			if (syntheticTypeDeclaration.javadoc == null) {
				syntheticTypeDeclaration.javadoc = new Javadoc(syntheticTypeDeclaration.declarationSourceStart, syntheticTypeDeclaration.declarationSourceStart);
			}
			syntheticTypeDeclaration.resolve(this.scope);
			// resolve annotations if any
//			if (this.currentPackage!= null && this.currentPackage.annotations != null) {
//				resolveAnnotations(syntheticTypeDeclaration.staticInitializerScope, this.currentPackage.annotations, this.scope.getDefaultPackage());
//			}
			// resolve javadoc package if any
			if (this.javadoc != null) {
				this.javadoc.resolve(syntheticTypeDeclaration.staticInitializerScope);
    		}
			startingTypeIndex = 1;
		} else {
			// resolve compilation unit javadoc package if any
			if (this.javadoc != null) {
				this.javadoc.resolve(this.scope);
    		}
		}

		try {
			if (types != null) {
				for (int i = startingTypeIndex, count = types.length; i < count; i++) {
					types[i].resolve(scope);
				}
			}
			if (statements != null) {
				for (int i = 0, count = statements.length; i < count; i++) {
					statements[i].resolve(scope);
				}
			}
			reportNLSProblems();
		} catch (AbortCompilationUnit e) {
			this.ignoreFurtherInvestigation = true;
			return;
		}
	}

	public void resolve(int start, int end) {
		try {
			int startingTypeIndex = 0;
			// resolve compilation unit javadoc package if any
			if (this.javadoc != null && this.javadoc.sourceStart<=start && this.javadoc.sourceEnd>= end) {
				this.javadoc.resolve(this.scope);
    		}
			if (types != null) {
				for (int i = startingTypeIndex, count = types.length; i < count; i++) {
					TypeDeclaration typeDeclaration = types[i];
					if (typeDeclaration.sourceStart<=start && typeDeclaration.sourceEnd>=end)
						typeDeclaration.resolve(scope);
				}
			}
			if (statements != null) {
				for (int i = 0, count = statements.length; i < count; i++) {
					ProgramElement programElement = statements[i];
					if (programElement.sourceStart<=start && programElement.sourceEnd>=end)
						programElement.resolve(scope);
				}
			}
			reportNLSProblems();
		} catch (AbortCompilationUnit e) {
			this.ignoreFurtherInvestigation = true;
			return;
		}
	}


	private void reportNLSProblems() {
		if (this.nlsTags != null || this.stringLiterals != null) {
			final int stringLiteralsLength = this.stringLiteralsPtr;
			final int nlsTagsLength = this.nlsTags == null ? 0 : this.nlsTags.length;
			if (stringLiteralsLength == 0) {
				if (nlsTagsLength != 0) {
					for (int i = 0; i < nlsTagsLength; i++) {
						NLSTag tag = this.nlsTags[i];
						if (tag != null) {
							scope.problemReporter().unnecessaryNLSTags(tag.start, tag.end);
						}
					}
				}
			} else if (nlsTagsLength == 0) {
				// resize string literals
				if (this.stringLiterals.length != stringLiteralsLength) {
					System.arraycopy(this.stringLiterals, 0, (stringLiterals = new StringLiteral[stringLiteralsLength]), 0, stringLiteralsLength);
				}
				Arrays.sort(this.stringLiterals, STRING_LITERAL_COMPARATOR);
				for (int i = 0; i < stringLiteralsLength; i++) {
					scope.problemReporter().nonExternalizedStringLiteral(this.stringLiterals[i]);
				}
			} else {
				// need to iterate both arrays to find non matching elements
				if (this.stringLiterals.length != stringLiteralsLength) {
					System.arraycopy(this.stringLiterals, 0, (stringLiterals = new StringLiteral[stringLiteralsLength]), 0, stringLiteralsLength);
				}
				Arrays.sort(this.stringLiterals, STRING_LITERAL_COMPARATOR);
				int indexInLine = 1;
				int lastLineNumber = -1;
				StringLiteral literal = null;
				int index = 0;
				int i = 0;
				stringLiteralsLoop: for (; i < stringLiteralsLength; i++) {
					literal = this.stringLiterals[i];
					final int literalLineNumber = literal.lineNumber;
					if (lastLineNumber != literalLineNumber) {
						indexInLine = 1;
						lastLineNumber = literalLineNumber;
					} else {
						indexInLine++;
					}
					if (index < nlsTagsLength) {
						nlsTagsLoop: for (; index < nlsTagsLength; index++) {
							NLSTag tag = this.nlsTags[index];
							if (tag == null) continue nlsTagsLoop;
							int tagLineNumber = tag.lineNumber;
							if (literalLineNumber < tagLineNumber) {
								scope.problemReporter().nonExternalizedStringLiteral(literal);
								continue stringLiteralsLoop;
							} else if (literalLineNumber == tagLineNumber) {
								if (tag.index == indexInLine) {
									this.nlsTags[index] = null;
									index++;
									continue stringLiteralsLoop;
								} else {
									nlsTagsLoop2: for (int index2 = index + 1; index2 < nlsTagsLength; index2++) {
										NLSTag tag2 = this.nlsTags[index2];
										if (tag2 == null) continue nlsTagsLoop2;
										int tagLineNumber2 = tag2.lineNumber;
										if (literalLineNumber == tagLineNumber2) {
											if (tag2.index == indexInLine) {
												this.nlsTags[index2] = null;
												continue stringLiteralsLoop;
											} else {
												continue nlsTagsLoop2;
											}
										} else {
											scope.problemReporter().nonExternalizedStringLiteral(literal);
											continue stringLiteralsLoop;
										}
									}
									scope.problemReporter().nonExternalizedStringLiteral(literal);
									continue stringLiteralsLoop;
								}
							} else {
								scope.problemReporter().unnecessaryNLSTags(tag.start, tag.end);
								continue nlsTagsLoop;
							}
						}
					}
					// all nls tags have been processed, so remaining string literals are not externalized
					break stringLiteralsLoop;
				}
				for (; i < stringLiteralsLength; i++) {
					scope.problemReporter().nonExternalizedStringLiteral(this.stringLiterals[i]);
				}
				if (index < nlsTagsLength) {
					for (; index < nlsTagsLength; index++) {
						NLSTag tag = this.nlsTags[index];
						if (tag != null) {
							scope.problemReporter().unnecessaryNLSTags(tag.start, tag.end);
						}
					}
				}
			}
		}
	}

	public void tagAsHavingErrors() {
		ignoreFurtherInvestigation = true;
	}

	
	public void traverse(org.eclipse.wst.jsdt.core.ast.ASTVisitor visitor)
	{
		this.traverse(new DelegateASTVisitor(visitor), null,false);
	}

	public void traverse(
			ASTVisitor visitor,
			CompilationUnitScope unitScope) {
		traverse(visitor, scope,false);
	}

	public void traverse(
		ASTVisitor visitor,
		CompilationUnitScope unitScope, boolean ignoreErrors) {

		if (ignoreFurtherInvestigation && !ignoreErrors)
			return;
		try {
			if (visitor.visit(this, this.scope)) {
				if (statements != null) {
					int statementsLength = statements.length;
					for (int i = 0; i < statementsLength; i++) {
						statements[i].traverse(visitor, this.scope);
					}
				}
				traverseInferredTypes(visitor,unitScope);
			}
			visitor.endVisit(this, this.scope);
		} catch (AbortCompilationUnit e) {
			// ignore
		}
	}

	public void traverseInferredTypes(ASTVisitor visitor,BlockScope unitScope) {
		boolean continueVisiting=true;
		for (int i=0;i<this.numberInferredTypes;i++) {
			InferredType inferredType = this.inferredTypes[i];
			continueVisiting=visitor.visit(inferredType, scope);
			  for (int attributeInx=0; attributeInx<inferredType.numberAttributes; attributeInx++) {
					visitor.visit(inferredType.attributes[attributeInx], scope);
				}
			if (inferredType.methods!=null)
				for (Iterator iterator = inferredType.methods.iterator();  continueVisiting && iterator
						.hasNext();) {
					InferredMethod inferredMethod = (InferredMethod) iterator.next();
					visitor.visit(inferredMethod, scope);
				}
			visitor.endVisit(inferredType, scope);
		}
	}

	public InferredType findInferredType(char [] name) {
		return (InferredType)inferredTypesHash.get(name);
	}



	public void printInferredTypes(StringBuffer sb)
	{
		for (int i=0;i<this.numberInferredTypes;i++) {
			InferredType inferredType = this.inferredTypes[i];
				if (inferredType.isDefinition())
				{
					inferredType.print(0,sb);
					sb.append("\n"); //$NON-NLS-1$
				}
		}

	}
	public int getASTType() {
		return IASTNode.SCRIPT_FILE_DECLARATION;
	
	}

	public IProgramElement[] getStatements() {
		return this.statements;
	}

	public String getInferenceID() {
		if (this.compilationResult.compilationUnit!=null)
			return this.compilationResult.compilationUnit.getInferenceID();
		return null;
	}
	
	public void addImport(char [] importName, int startPosition, int endPosition, int nameStartPosition)
	{
		ImportReference importReference=new ImportReference(importName, startPosition,  endPosition,  nameStartPosition);
		if (imports==null)
		{
			imports=new ImportReference[]{importReference};
		}
		else
		{
			ImportReference[] newImports=new ImportReference[imports.length+1];
			System.arraycopy(this.imports, 0, newImports, 0, this.imports.length);
			newImports[this.imports.length]=importReference;
			this.imports=newImports;
		}
	}

	public InferredType addType(char[] className, boolean isDefinition, String providerId) {
		InferredType type = findInferredType(className);

		if (type==null && className.length > 0)
		{
			if (numberInferredTypes == inferredTypes.length) 
			{
				System.arraycopy(
						inferredTypes,
						0,
						(inferredTypes = new InferredType[numberInferredTypes  * 2]),
						0,
						numberInferredTypes );
			}

			type=inferredTypes[numberInferredTypes ++] = new InferredType(className);
			type.inferenceProviderID = providerId;
			if (className.length > 2 && className[className.length - 2] == '[' && className[className.length - 1] == ']') {
				type.isArray = true;
			}
			
			inferredTypesHash.put(className,type);
		}
		if (isDefinition && type != null)
			type.setIsDefinition(isDefinition);
		return type;
	}
}
