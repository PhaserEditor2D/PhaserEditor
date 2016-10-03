/*******************************************************************************
 * Copyright (c) 2002, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.formatter;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.ast.AND_AND_Expression;
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
import org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Block;
import org.eclipse.wst.jsdt.internal.compiler.ast.BreakStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.CaseStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.wst.jsdt.internal.compiler.ast.Clinit;
import org.eclipse.wst.jsdt.internal.compiler.ast.CombinedBinaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ContinueStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.DoStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.DoubleLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.EmptyStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.EqualExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ForInStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ForStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.FunctionExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.IfStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Initializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.IntLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ListExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.NullLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.OR_OR_Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteralField;
import org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds;
import org.eclipse.wst.jsdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedThisReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.RegExLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Statement;
import org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteralConcatenation;
import org.eclipse.wst.jsdt.internal.compiler.ast.SuperReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ThisReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.TryStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.UndefinedLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.WhileStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.WithStatement;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.parser.Scanner;
import org.eclipse.wst.jsdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.wst.jsdt.internal.core.util.CodeSnippetParsingUtil;
import org.eclipse.wst.jsdt.internal.formatter.align.Alignment;
import org.eclipse.wst.jsdt.internal.formatter.align.AlignmentException;
import org.eclipse.wst.jsdt.internal.formatter.comment.CommentRegion;

/**
 * This class is responsible for formatting a valid java source code.
 * @since 2.1
 */
/*
   <extension
         id="org.eclipse.wst.jsdt.core.newformatter.codeformatter"
         name="org.eclipse.wst.jsdt.core.newformatter.codeformatter"
         point="org.eclipse.wst.jsdt.core.codeFormatter">
      <codeFormatter
            class="org.eclipse.wst.jsdt.internal.formatter.CodeFormatterVisitor">
      </codeFormatter>
   </extension>
*/
public class CodeFormatterVisitor extends ASTVisitor {

	public static class MultiFieldDeclaration extends FieldDeclaration {

		FieldDeclaration[] declarations;

		MultiFieldDeclaration(FieldDeclaration[] declarations){
			this.declarations = declarations;
			this.modifiers = declarations[0].modifiers;
		}
	}

	public final static boolean DEBUG = false;
	private static final int NO_MODIFIERS = 0;
	/*
	 * Set of expected tokens type for a single type reference.
	 * This array needs to be SORTED.
	 */
	private static final int[] SINGLETYPEREFERENCE_EXPECTEDTOKENS = new int[] {
		TerminalTokens.TokenNameIdentifier,
		TerminalTokens.TokenNameboolean,
		TerminalTokens.TokenNamebyte,
		TerminalTokens.TokenNamechar,
		TerminalTokens.TokenNamedouble,
		TerminalTokens.TokenNamefloat,
		TerminalTokens.TokenNameint,
		TerminalTokens.TokenNamelong,
		TerminalTokens.TokenNameshort,
		TerminalTokens.TokenNamevoid
	};
	private static final int[] CLOSING_GENERICS_EXPECTEDTOKENS = new int[] {
		TerminalTokens.TokenNameRIGHT_SHIFT,
		TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT,
		TerminalTokens.TokenNameGREATER
	};
	private int chunkKind;
	public int lastLocalDeclarationSourceStart;
	private Scanner localScanner;
	public DefaultCodeFormatterOptions preferences;
	public Scribe scribe;

	public CodeFormatterVisitor(DefaultCodeFormatterOptions preferences, Map settings, int offset, int length, CodeSnippetParsingUtil codeSnippetParsingUtil) {
		long sourceLevel = settings == null
			? ClassFileConstants.JDK1_3
			: CompilerOptions.versionToJdkLevel(settings.get(JavaScriptCore.COMPILER_SOURCE));
		this.localScanner = new Scanner(true, false, false/*nls*/, sourceLevel/*sourceLevel*/, null/*taskTags*/, null/*taskPriorities*/, true/*taskCaseSensitive*/);

		this.preferences = preferences;
		this.scribe = new Scribe(this, sourceLevel, offset, length, codeSnippetParsingUtil);
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#acceptProblem(org.eclipse.wst.jsdt.core.compiler.IProblem)
	 */
	public void acceptProblem(IProblem problem) {
		super.acceptProblem(problem);
	}

	private BinaryExpressionFragmentBuilder buildFragments(BinaryExpression binaryExpression, BlockScope scope) {
		BinaryExpressionFragmentBuilder builder = new BinaryExpressionFragmentBuilder();

		if (binaryExpression instanceof CombinedBinaryExpression) {
			binaryExpression.traverse(builder, scope);
			return builder;
		}
		switch((binaryExpression.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT) {
			case OperatorIds.MULTIPLY :
				binaryExpression.left.traverse(builder, scope);
				builder.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameMULTIPLY));
				binaryExpression.right.traverse(builder, scope);
				break;
			case OperatorIds.PLUS :
				binaryExpression.left.traverse(builder, scope);
				builder.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNamePLUS));
				binaryExpression.right.traverse(builder, scope);
				break;
			case OperatorIds.DIVIDE :
				binaryExpression.left.traverse(builder, scope);
				builder.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameDIVIDE));
				binaryExpression.right.traverse(builder, scope);
				break;
			case OperatorIds.REMAINDER :
				binaryExpression.left.traverse(builder, scope);
				builder.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameREMAINDER));
				binaryExpression.right.traverse(builder, scope);
				break;
			case OperatorIds.XOR :
				binaryExpression.left.traverse(builder, scope);
				builder.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameXOR));
				binaryExpression.right.traverse(builder, scope);
				break;
			case OperatorIds.MINUS :
				binaryExpression.left.traverse(builder, scope);
				builder.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameMINUS));
				binaryExpression.right.traverse(builder, scope);
				break;
			case OperatorIds.OR :
				binaryExpression.left.traverse(builder, scope);
				builder.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameOR));
				binaryExpression.right.traverse(builder, scope);
				break;
			case OperatorIds.AND :
				binaryExpression.left.traverse(builder, scope);
				builder.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameAND));
				binaryExpression.right.traverse(builder, scope);
				break;
			case OperatorIds.AND_AND :
				binaryExpression.left.traverse(builder, scope);
				builder.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameAND_AND));
				binaryExpression.right.traverse(builder, scope);
				break;
			case OperatorIds.OR_OR :
				binaryExpression.left.traverse(builder, scope);
				builder.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameOR_OR));
				binaryExpression.right.traverse(builder, scope);
				break;
		}

		return builder;
	}

	private CascadingMethodInvocationFragmentBuilder buildFragments(MessageSend messageSend, BlockScope scope) {
		CascadingMethodInvocationFragmentBuilder builder = new CascadingMethodInvocationFragmentBuilder();

		messageSend.traverse(builder, scope);
		return builder;
	}
	public boolean checkChunkStart(int kind) {
		if (this.chunkKind != kind) {
			this.chunkKind = kind;
			return true;
		}
		return false;
	}

	private boolean commentStartsBlock(int start, int end) {
		this.localScanner.resetTo(start, end);
		try {
			if (this.localScanner.getNextToken() ==  TerminalTokens.TokenNameLBRACE) {
				switch(this.localScanner.getNextToken()) {
					case TerminalTokens.TokenNameCOMMENT_BLOCK :
					case TerminalTokens.TokenNameCOMMENT_JAVADOC :
					case TerminalTokens.TokenNameCOMMENT_LINE :
						return true;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return false;
	}

	private ASTNode[] computeMergedMemberDeclarations(ASTNode[] nodes){
		ArrayList mergedNodes = new ArrayList();
		for (int i = 0, max = nodes.length; i < max; i++) {
			ASTNode currentNode = nodes[i];
			if (currentNode instanceof FieldDeclaration) {
				FieldDeclaration currentField = (FieldDeclaration) currentNode;
				if (mergedNodes.size() == 0) {
					// first node
					mergedNodes.add(currentNode);
				} else {
					// we need to check if the previous merged node is a field declaration
					ASTNode previousMergedNode = (ASTNode) mergedNodes.get(mergedNodes.size() - 1);
					if (previousMergedNode instanceof MultiFieldDeclaration) {
						// we merge the current node
						MultiFieldDeclaration multiFieldDeclaration = (MultiFieldDeclaration) previousMergedNode;
						int length = multiFieldDeclaration.declarations.length;
						System.arraycopy(multiFieldDeclaration.declarations, 0, multiFieldDeclaration.declarations= new FieldDeclaration[length+1], 0, length);
						multiFieldDeclaration.declarations[length] = currentField;
					} else if (previousMergedNode instanceof FieldDeclaration) {
						// need to check we need to create a multiple field declaration
						if (currentField.declarationSourceStart == ((FieldDeclaration) previousMergedNode).declarationSourceStart) {
							// we create a multi field declaration
							mergedNodes.set(mergedNodes.size() - 1, new MultiFieldDeclaration(new FieldDeclaration[]{ (FieldDeclaration)previousMergedNode, currentField}));
						} else {
							mergedNodes.add(currentNode);
						}
					} else {
						mergedNodes.add(currentNode);
					}
				}
			} else {
				mergedNodes.add(currentNode);
			}
		}
		if (mergedNodes.size() != nodes.length) {
			ASTNode[] result = new ASTNode[mergedNodes.size()];
			mergedNodes.toArray(result);
			return result;
		} else {
			return nodes;
		}
	}

	private ASTNode[] computeMergedMemberDeclarations(TypeDeclaration typeDeclaration){

		int fieldIndex = 0, fieldCount = (typeDeclaration.fields == null) ? 0 : typeDeclaration.fields.length;
		FieldDeclaration field = fieldCount == 0 ? null : typeDeclaration.fields[fieldIndex];
		int fieldStart = field == null ? Integer.MAX_VALUE : field.declarationSourceStart;

		int methodIndex = 0, methodCount = (typeDeclaration.methods == null) ? 0 : typeDeclaration.methods.length;
		AbstractMethodDeclaration method = methodCount == 0 ? null : typeDeclaration.methods[methodIndex];
		int methodStart = method == null ? Integer.MAX_VALUE : method.declarationSourceStart;

		int typeIndex = 0, typeCount = (typeDeclaration.memberTypes == null) ? 0 : typeDeclaration.memberTypes.length;
		TypeDeclaration type = typeCount == 0 ? null : typeDeclaration.memberTypes[typeIndex];
		int typeStart = type == null ? Integer.MAX_VALUE : type.declarationSourceStart;

		final int memberLength = fieldCount+methodCount+typeCount;
		ASTNode[] members = new ASTNode[memberLength];
		if (memberLength != 0) {
			int index = 0;
			int previousFieldStart = -1;
			do {
				if (fieldStart < methodStart && fieldStart < typeStart) {
					// next member is a field
					if (fieldStart == previousFieldStart){
						ASTNode previousMember = members[index - 1];
						if (previousMember instanceof MultiFieldDeclaration) {
							MultiFieldDeclaration multiField = (MultiFieldDeclaration) previousMember;
							int length = multiField.declarations.length;
							System.arraycopy(multiField.declarations, 0, multiField.declarations=new FieldDeclaration[length+1], 0, length);
							multiField.declarations[length] = field;
						} else {
							members[index - 1] = new MultiFieldDeclaration(new FieldDeclaration[]{ (FieldDeclaration)previousMember, field});
						}
					} else {
						members[index++] = field;
					}
					previousFieldStart = fieldStart;
					if (++fieldIndex < fieldCount) { // find next field if any
						fieldStart = (field = typeDeclaration.fields[fieldIndex]).declarationSourceStart;
					} else {
						fieldStart = Integer.MAX_VALUE;
					}
				} else if (methodStart < fieldStart && methodStart < typeStart) {
					// next member is a method
					if (!method.isDefaultConstructor() && !method.isClinit()) {
						members[index++] = method;
					}
					if (++methodIndex < methodCount) { // find next method if any
						methodStart = (method = typeDeclaration.methods[methodIndex]).declarationSourceStart;
					} else {
						methodStart = Integer.MAX_VALUE;
					}
				} else {
					// next member is a type
					members[index++] = type;
					if (++typeIndex < typeCount) { // find next type if any
						typeStart = (type = typeDeclaration.memberTypes[typeIndex]).declarationSourceStart;
					} else {
						typeStart = Integer.MAX_VALUE;
					}
				}
			} while ((fieldIndex < fieldCount) || (typeIndex < typeCount) || (methodIndex < methodCount));

			if (members.length != index) {
				System.arraycopy(members, 0, members=new ASTNode[index], 0, index);
			}
		}
		return members;
	}

	private boolean dumpBinaryExpression(
		BinaryExpression binaryExpression,
		int operator,
		BlockScope scope) {

//		boolean addSpace= operator==TerminalTokens.TokenNamein || operator==TerminalTokens.TokenNameinstanceof;
		final int numberOfParens = (binaryExpression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;

		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(binaryExpression, numberOfParens);
		}
		BinaryExpressionFragmentBuilder builder = buildFragments(binaryExpression, scope);
		final int fragmentsSize = builder.size();

		if ((builder.realFragmentsSize() > 1 || builder.size() > 4) && numberOfParens == 0) {
			this.scribe.printComment();
			Alignment binaryExpressionAlignment = this.scribe.createAlignment("binaryExpressionAlignment", this.preferences.alignment_for_binary_expression, Alignment.R_OUTERMOST, fragmentsSize, this.scribe.scanner.currentPosition); //$NON-NLS-1$
			this.scribe.enterAlignment(binaryExpressionAlignment);
			boolean ok = false;
			ASTNode[] fragments = builder.fragments();
			int[] operators = builder.operators();
			do {
				try {
					for (int i = 0; i < fragmentsSize - 1; i++) {
						ASTNode fragment = fragments[i];
						fragment.traverse(this, scope);
						this.scribe.printTrailingComment();
						if (this.scribe.lastNumberOfNewLines == 1) {
							// a new line has been inserted by printTrailingComment()
							this.scribe.indentationLevel = binaryExpressionAlignment.breakIndentationLevel;
						}
						if (this.preferences.wrap_before_binary_operator) {
							this.scribe.alignFragment(binaryExpressionAlignment, i);
							this.scribe.printNextToken(operators[i], this.preferences.insert_space_before_binary_operator);
						} else {
							this.scribe.printNextToken(operators[i], this.preferences.insert_space_before_binary_operator);
							this.scribe.alignFragment(binaryExpressionAlignment, i);
						}
						if (operators[i] == TerminalTokens.TokenNameMINUS && isNextToken(TerminalTokens.TokenNameMINUS)) {
							// the next character is a minus (unary operator)
							this.scribe.space();
						}
						if (this.preferences.insert_space_after_binary_operator) {
							this.scribe.space();
						}
					}
					fragments[fragmentsSize - 1].traverse(this, scope);
					this.scribe.printTrailingComment();
					ok = true;
				} catch(AlignmentException e){
					this.scribe.redoAlignment(e);
				}
			} while (!ok);
			this.scribe.exitAlignment(binaryExpressionAlignment, true);
		} else {
			binaryExpression.left.traverse(this, scope);
			this.scribe.printNextToken(operator, this.preferences.insert_space_before_binary_operator);
			if (operator == TerminalTokens.TokenNameMINUS && isNextToken(TerminalTokens.TokenNameMINUS)) {
				// the next character is a minus (unary operator)
				this.scribe.space();
			}
			if (this.preferences.insert_space_after_binary_operator) {
				this.scribe.space();
			}
			binaryExpression.right.traverse(this, scope);
		}
		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(binaryExpression, numberOfParens);
		}
		return false;
	}

	private boolean dumpEqualityExpression(
		BinaryExpression binaryExpression,
		int operator,
		BlockScope scope) {

		final int numberOfParens = (binaryExpression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;

		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(binaryExpression, numberOfParens);
		}
		binaryExpression.left.traverse(this, scope);
		this.scribe.printNextToken(operator, this.preferences.insert_space_before_binary_operator);
		if (this.preferences.insert_space_after_binary_operator) {
			this.scribe.space();
		}
		binaryExpression.right.traverse(this, scope);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(binaryExpression, numberOfParens);
		}
		return false;
	}

	private final TextEdit failedToFormat() {
		if (DEBUG) {
			System.out.println("COULD NOT FORMAT \n" + this.scribe.scanner); //$NON-NLS-1$
			System.out.println(this.scribe);
		}
		return null;
	}

	private void format(
		AbstractMethodDeclaration methodDeclaration,
		ClassScope scope,
		boolean isChunkStart,
		boolean isFirstClassBodyDeclaration) {

		if (isFirstClassBodyDeclaration) {
			int newLinesBeforeFirstClassBodyDeclaration = this.preferences.blank_lines_before_first_class_body_declaration;
			if (newLinesBeforeFirstClassBodyDeclaration > 0) {
				this.scribe.printEmptyLines(newLinesBeforeFirstClassBodyDeclaration);
			}
		} else {
			final int newLineBeforeChunk = isChunkStart ? this.preferences.blank_lines_before_new_chunk : 0;
			if (newLineBeforeChunk > 0) {
				this.scribe.printEmptyLines(newLineBeforeChunk);
			}
		}
		final int newLinesBeforeMethod = this.preferences.blank_lines_before_method;
		if (newLinesBeforeMethod > 0 && !isFirstClassBodyDeclaration) {
			this.scribe.printEmptyLines(newLinesBeforeMethod);
		} else if (this.scribe.line != 0 || this.scribe.column != 1) {
			this.scribe.printNewLine();
		}
		methodDeclaration.traverse(this, scope);
	}

	private void format(AbstractVariableDeclaration fieldDeclaration, ASTVisitor visitor, MethodScope scope, boolean isChunkStart, boolean isFirstClassBodyDeclaration) {

		if (isFirstClassBodyDeclaration) {
			int newLinesBeforeFirstClassBodyDeclaration = this.preferences.blank_lines_before_first_class_body_declaration;
			if (newLinesBeforeFirstClassBodyDeclaration > 0) {
				this.scribe.printEmptyLines(newLinesBeforeFirstClassBodyDeclaration);
			}
		} else {
			int newLineBeforeChunk = isChunkStart ? this.preferences.blank_lines_before_new_chunk : 0;
			if (newLineBeforeChunk > 0) {
				this.scribe.printEmptyLines(newLineBeforeChunk);
			}
			final int newLinesBeforeField = this.preferences.blank_lines_before_field;
			if (newLinesBeforeField > 0) {
				this.scribe.printEmptyLines(newLinesBeforeField);
			}
		}
		Alignment memberAlignment = this.scribe.getMemberAlignment();

        this.scribe.printComment();
		this.scribe.printModifiers(this);
		this.scribe.space();

		this.scribe.printNextToken(TerminalTokens.TokenNamevar, true);


		/*
		 * Field type
		 */
//		fieldDeclaration.type.traverse(this, scope);

		/*
		 * Field name
		 */
		this.scribe.alignFragment(memberAlignment, 0);

		this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier, true);

		/*
		 * Check for extra dimensions
		 */
		int extraDimensions = getDimensions();
		if (extraDimensions != 0) {
			 for (int i = 0; i < extraDimensions; i++) {
			 	this.scribe.printNextToken(TerminalTokens.TokenNameLBRACKET);
			 	this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET);
			 }
		}

		/*
		 * Field initialization
		 */
		final Expression initialization = fieldDeclaration.initialization;
		if (initialization != null) {
			this.scribe.alignFragment(memberAlignment, 1);
			this.scribe.printNextToken(TerminalTokens.TokenNameEQUAL, this.preferences.insert_space_before_assignment_operator);
			if (this.preferences.insert_space_after_assignment_operator) {
				this.scribe.space();
			}
			Alignment assignmentAlignment = this.scribe.createAlignment("fieldDeclarationAssignmentAlignment", this.preferences.alignment_for_assignment, Alignment.R_OUTERMOST, 1, this.scribe.scanner.currentPosition); //$NON-NLS-1$
			this.scribe.enterAlignment(assignmentAlignment);
			boolean ok = false;
			do {
				try {
					this.scribe.alignFragment(assignmentAlignment, 0);
					initialization.traverse(this, scope);
					ok = true;
				} catch(AlignmentException e){
					this.scribe.redoAlignment(e);
				}
			} while (!ok);
			this.scribe.exitAlignment(assignmentAlignment, true);
		}

		this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);

		if (memberAlignment != null) {
			this.scribe.alignFragment(memberAlignment, 2);
			this.scribe.printTrailingComment();
		} else {
			this.scribe.space();
			this.scribe.printTrailingComment();
		}
	}

	private void format(ImportReference importRef, boolean isLast) {
		if (importRef.isInternal())
			return;
		this.scribe.printNextToken(TerminalTokens.TokenNameimport);
		this.preferences.number_of_empty_lines_to_preserve = this.preferences.blank_lines_between_import_groups;
		this.scribe.space();
		if ((importRef.bits & ASTNode.OnDemand) != 0) {
			this.scribe.printQualifiedReference(importRef.sourceEnd);
			this.scribe.printNextToken(TerminalTokens.TokenNameDOT);
			this.scribe.printNextToken(TerminalTokens.TokenNameMULTIPLY);
			this.scribe.printNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
		} else {
			this.scribe.printQualifiedReference(importRef.sourceEnd);
			this.scribe.printNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
		}
		if (isLast) {
			this.scribe.printTrailingComment();
		} else {
			this.scribe.printTrailingComment(this.preferences.blank_lines_between_import_groups);
		}
		this.scribe.printNewLine();
	}


	private void format(MultiFieldDeclaration multiFieldDeclaration, ASTVisitor visitor, MethodScope scope, boolean isChunkStart, boolean isFirstClassBodyDeclaration) {

		if (isFirstClassBodyDeclaration) {
			int newLinesBeforeFirstClassBodyDeclaration = this.preferences.blank_lines_before_first_class_body_declaration;
			if (newLinesBeforeFirstClassBodyDeclaration > 0) {
				this.scribe.printEmptyLines(newLinesBeforeFirstClassBodyDeclaration);
			}
		} else {
			int newLineBeforeChunk = isChunkStart ? this.preferences.blank_lines_before_new_chunk : 0;
			if (newLineBeforeChunk > 0) {
				this.scribe.printEmptyLines(newLineBeforeChunk);
			}
			final int newLinesBeforeField = this.preferences.blank_lines_before_field;
			if (newLinesBeforeField > 0) {
				this.scribe.printEmptyLines(newLinesBeforeField);
			}
		}
		Alignment fieldAlignment = this.scribe.getMemberAlignment();

        this.scribe.printComment();
		this.scribe.printModifiers(this);
		this.scribe.space();

		multiFieldDeclaration.declarations[0].type.traverse(this, scope);

		final int multipleFieldDeclarationsLength = multiFieldDeclaration.declarations.length;

		Alignment multiFieldDeclarationsAlignment =this.scribe.createAlignment(
				"multiple_field",//$NON-NLS-1$
				this.preferences.alignment_for_multiple_fields,
				multipleFieldDeclarationsLength - 1,
				this.scribe.scanner.currentPosition);
		this.scribe.enterAlignment(multiFieldDeclarationsAlignment);

		boolean ok = false;
		do {
			try {
				for (int i = 0, length = multipleFieldDeclarationsLength; i < length; i++) {
					FieldDeclaration fieldDeclaration = multiFieldDeclaration.declarations[i];
					/*
					 * Field name
					 */
					if (i == 0) {
						this.scribe.alignFragment(fieldAlignment, 0);
						this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier, true);
					} else {
						this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier, false);
					}

					/*
					 * Check for extra dimensions
					 */
					int extraDimensions = getDimensions();
					if (extraDimensions != 0) {
						 for (int index = 0; index < extraDimensions; index++) {
						 	this.scribe.printNextToken(TerminalTokens.TokenNameLBRACKET);
						 	this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET);
						 }
					}

					/*
					 * Field initialization
					 */
					final Expression initialization = fieldDeclaration.initialization;
					if (initialization != null) {
						if (i == 0) {
							this.scribe.alignFragment(fieldAlignment, 1);
						}
						this.scribe.printNextToken(TerminalTokens.TokenNameEQUAL, this.preferences.insert_space_before_assignment_operator);
						if (this.preferences.insert_space_after_assignment_operator) {
							this.scribe.space();
						}
						initialization.traverse(this, scope);
					}

					if (i != length - 1) {
						this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_multiple_field_declarations);
						this.scribe.printTrailingComment();
						this.scribe.alignFragment(multiFieldDeclarationsAlignment, i);

						if (this.preferences.insert_space_after_comma_in_multiple_field_declarations) {
							this.scribe.space();
						}
					} else {
						this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
						this.scribe.alignFragment(fieldAlignment, 2);
						this.scribe.printTrailingComment();
					}
				}
				ok = true;
			} catch (AlignmentException e) {
				this.scribe.redoAlignment(e);
			}
		} while (!ok);
		this.scribe.exitAlignment(multiFieldDeclarationsAlignment, true);
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.formatter.CodeFormatter#format(int, String, int, int, int, String)
	 */
	public TextEdit format(String string, ASTNode[] nodes) {
		// reset the scribe
		this.scribe.reset();

		long startTime = System.currentTimeMillis();

		final char[] compilationUnitSource = string.toCharArray();

		this.localScanner.setSource(compilationUnitSource);
		this.scribe.initializeScanner(compilationUnitSource);

		if (nodes == null) {
			return null;
		}

		this.lastLocalDeclarationSourceStart = -1;
		try {
			formatClassBodyDeclarations(nodes);
		} catch(AbortFormatting e){
			return failedToFormat();
		}
		if (DEBUG){
			System.out.println("Formatting time: " + (System.currentTimeMillis() - startTime));  //$NON-NLS-1$
		}
		return this.scribe.getRootEdit();
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.formatter.CodeFormatter#format(int, String, int, int, int, String)
	 */
	public TextEdit format(String string, CompilationUnitDeclaration compilationUnitDeclaration) {
		// reset the scribe
		this.scribe.reset();

		if (compilationUnitDeclaration == null || compilationUnitDeclaration.ignoreFurtherInvestigation) {
			return failedToFormat();
		}

		long startTime = System.currentTimeMillis();

		final char[] compilationUnitSource = string.toCharArray();

		this.localScanner.setSource(compilationUnitSource);
		this.scribe.initializeScanner(compilationUnitSource);

		this.lastLocalDeclarationSourceStart = -1;
		try {
			compilationUnitDeclaration.traverse(this, compilationUnitDeclaration.scope);
		} catch(AbortFormatting e){
			return failedToFormat();
		}
		if (DEBUG){
			System.out.println("Formatting time: " + (System.currentTimeMillis() - startTime));  //$NON-NLS-1$
		}
		return this.scribe.getRootEdit();
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.formatter.CodeFormatter#format(int, String, int, int, int, String)
	 */
	public TextEdit format(String string, ConstructorDeclaration constructorDeclaration) {
		// reset the scribe
		this.scribe.reset();

		long startTime = System.currentTimeMillis();

		final char[] compilationUnitSource = string.toCharArray();

		this.localScanner.setSource(compilationUnitSource);
		this.scribe.initializeScanner(compilationUnitSource);

		if (constructorDeclaration == null) {
			return null;
		}

		this.lastLocalDeclarationSourceStart = -1;
		try {
			ExplicitConstructorCall explicitConstructorCall = constructorDeclaration.constructorCall;
			if (explicitConstructorCall != null && !explicitConstructorCall.isImplicitSuper()) {
				explicitConstructorCall.traverse(this, null);
			}
			Statement[] statements = constructorDeclaration.statements;
			if (statements != null) {
				formatStatements(null, statements, false);
			}
			if (hasComments()) {
				this.scribe.printNewLine();
			}
			this.scribe.printComment();
		} catch(AbortFormatting e){
			return failedToFormat();
		}
		if (DEBUG){
			System.out.println("Formatting time: " + (System.currentTimeMillis() - startTime));  //$NON-NLS-1$
		}
		return this.scribe.getRootEdit();
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.formatter.CodeFormatter#format(int, String, int, int, int, String)
	 */
	public TextEdit format(String string, Expression expression) {
		// reset the scribe
		this.scribe.reset();

		long startTime = System.currentTimeMillis();

		final char[] compilationUnitSource = string.toCharArray();

		this.localScanner.setSource(compilationUnitSource);
		this.scribe.initializeScanner(compilationUnitSource);

		if (expression == null) {
			return null;
		}

		this.lastLocalDeclarationSourceStart = -1;
		try {
			expression.traverse(this, (BlockScope) null);
			this.scribe.printComment();
		} catch(AbortFormatting e){
			return failedToFormat();
		}
		if (DEBUG){
			System.out.println("Formatting time: " + (System.currentTimeMillis() - startTime));  //$NON-NLS-1$
		}
		return this.scribe.getRootEdit();
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.formatter.CodeFormatter#format(int, String, int, int, int, String)
	 */
	public TextEdit format(String string, CommentRegion region) {
		// reset the scribe
		this.scribe.reset();

		if (region == null) {
			return failedToFormat();
		}

		long startTime = 0;
		if (DEBUG){
			startTime = System.currentTimeMillis();
		}

		final char[] compilationUnitSource = string.toCharArray();

		this.scribe.initializeScanner(compilationUnitSource);

		TextEdit result = null;
		try {
			result = region.format(this.preferences.initial_indentation_level, true);
		} catch(AbortFormatting e){
			return failedToFormat();
		}
		if (DEBUG){
			System.out.println("Formatting time: " + (System.currentTimeMillis() - startTime));  //$NON-NLS-1$
		}
		return result;
	}

	private void format(TypeDeclaration typeDeclaration){
        /*
         * Print comments to get proper line number
         */
        this.scribe.printComment();
        int line = this.scribe.line;

		this.scribe.printModifiers(this);

		if (this.scribe.line > line) {
        	// annotations introduced new line, but this is not a line wrapping
			// see 158267
			line = this.scribe.line;
		}

        /*
		 * Type name
		 */
        switch(TypeDeclaration.kind(typeDeclaration.modifiers)) {
        	case TypeDeclaration.CLASS_DECL :
				this.scribe.printNextToken(TerminalTokens.TokenNameclass, true);
        		break;
        }
		this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier, true);

		/*
		 * Superclass
		 */
		final TypeReference superclass = typeDeclaration.superclass;
		if (superclass != null) {
			Alignment superclassAlignment =this.scribe.createAlignment(
					"superclass", //$NON-NLS-1$
					this.preferences.alignment_for_superclass_in_type_declaration,
					2,
					this.scribe.scanner.currentPosition);
			this.scribe.enterAlignment(superclassAlignment);
			boolean ok = false;
			do {
				try {
					this.scribe.alignFragment(superclassAlignment, 0);
					this.scribe.printNextToken(TerminalTokens.TokenNameextends, true);
					this.scribe.alignFragment(superclassAlignment, 1);
					this.scribe.space();
					superclass.traverse(this, typeDeclaration.scope);
					ok = true;
				} catch (AlignmentException e) {
					this.scribe.redoAlignment(e);
				}
			} while (!ok);
			this.scribe.exitAlignment(superclassAlignment, true);
		}
		
		/*
		 * Type body
		 */
		String class_declaration_brace;
		boolean space_before_opening_brace;
		class_declaration_brace = this.preferences.brace_position_for_type_declaration;
		space_before_opening_brace = this.preferences.insert_space_before_opening_brace_in_type_declaration;

        formatLeftCurlyBrace(line, class_declaration_brace);
		formatTypeOpeningBrace(class_declaration_brace, space_before_opening_brace, typeDeclaration);

		boolean indent_body_declarations_compare_to_header;

		indent_body_declarations_compare_to_header = this.preferences.indent_body_declarations_compare_to_type_header;

		if (indent_body_declarations_compare_to_header) {
			this.scribe.indent();
		}
		
		formatTypeMembers(typeDeclaration);

		if (indent_body_declarations_compare_to_header) {
			this.scribe.unIndent();
		}

		if (this.preferences.insert_new_line_in_empty_type_declaration) {
			this.scribe.printNewLine();
		}
		
		this.scribe.printNextToken(TerminalTokens.TokenNameRBRACE);
		this.scribe.printTrailingComment();
		if (class_declaration_brace.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
			this.scribe.unIndent();
		}
		if (hasComments()) {
			this.scribe.printNewLine();
		}
	}

	private void format(
		TypeDeclaration memberTypeDeclaration,
		ClassScope scope,
		boolean isChunkStart,
		boolean isFirstClassBodyDeclaration) {

		if (isFirstClassBodyDeclaration) {
			int newLinesBeforeFirstClassBodyDeclaration = this.preferences.blank_lines_before_first_class_body_declaration;
			if (newLinesBeforeFirstClassBodyDeclaration > 0) {
				this.scribe.printEmptyLines(newLinesBeforeFirstClassBodyDeclaration);
			}
		} else {
			int newLineBeforeChunk = isChunkStart ? this.preferences.blank_lines_before_new_chunk : 0;
			if (newLineBeforeChunk > 0) {
				this.scribe.printEmptyLines(newLineBeforeChunk);
			}
			final int newLinesBeforeMember = this.preferences.blank_lines_before_member_type;
			if (newLinesBeforeMember > 0) {
				this.scribe.printEmptyLines(newLinesBeforeMember);
			}
		}
		memberTypeDeclaration.traverse(this, scope);
	}

	private void formatAnonymousTypeDeclaration(TypeDeclaration typeDeclaration) {
		/*
		 * Type body
		 */
		String anonymous_type_declaration_brace_position = this.preferences.brace_position_for_anonymous_type_declaration;

		formatTypeOpeningBrace(anonymous_type_declaration_brace_position, this.preferences.insert_space_before_opening_brace_in_anonymous_type_declaration, typeDeclaration);

		this.scribe.indent();

		formatTypeMembers(typeDeclaration);

		this.scribe.unIndent();
		if (this.preferences.insert_new_line_in_empty_anonymous_type_declaration) {
			this.scribe.printNewLine();
		}
		this.scribe.printNextToken(TerminalTokens.TokenNameRBRACE);
		if (anonymous_type_declaration_brace_position.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
			this.scribe.unIndent();
		}
	}

	/**
	 * @param block
	 * @param scope
	 * @param block_brace_position
	 */
	private void formatBlock(Block block, BlockScope scope, String block_brace_position, boolean insertSpaceBeforeOpeningBrace) {
		formatOpeningBrace(block_brace_position, insertSpaceBeforeOpeningBrace);
		final Statement[] statements = block.statements;
		if (statements != null) {
			this.scribe.printNewLine();
			if (this.preferences.indent_statements_compare_to_block) {
				this.scribe.indent();
			}
			formatStatements(scope, statements, true);
			this.scribe.printComment();

			if (this.preferences.indent_statements_compare_to_block) {
				this.scribe.unIndent();
			}
		} else if (this.preferences.insert_new_line_in_empty_block) {
			this.scribe.printNewLine();
			if (this.preferences.indent_statements_compare_to_block) {
				this.scribe.indent();
			}
			this.scribe.printComment();

			if (this.preferences.indent_statements_compare_to_block) {
				this.scribe.unIndent();
			}
		} else {
			if (this.preferences.indent_statements_compare_to_block) {
				this.scribe.indent();
			}
			this.scribe.printComment();

			if (this.preferences.indent_statements_compare_to_block) {
				this.scribe.unIndent();
			}
		}
		this.scribe.printNextToken(TerminalTokens.TokenNameRBRACE);
		this.scribe.printTrailingComment();
		if (DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED.equals(block_brace_position)) {
			this.scribe.unIndent();
		}
	}

	private void formatCascadingMessageSends(CascadingMethodInvocationFragmentBuilder builder, BlockScope scope) {
		int size = builder.size();
		MessageSend[] fragments = builder.fragments();
		Expression fragment = fragments[0].receiver;
		int startingPositionInCascade = 1;
		if (fragment!=null &&!fragment.isImplicitThis()) {
			fragment.traverse(this, scope);
		} else {
			MessageSend currentMessageSend = fragments[1];
			final int numberOfParens = (currentMessageSend.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
			if (numberOfParens > 0) {
				manageOpeningParenthesizedExpression(currentMessageSend, numberOfParens);
			}
			ASTNode[] arguments = currentMessageSend.arguments;
			this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier); // selector
			this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, this.preferences.insert_space_before_opening_paren_in_method_invocation);
			if (arguments != null) {
				if (this.preferences.insert_space_after_opening_paren_in_method_invocation) {
					this.scribe.space();
				}
				int argumentLength = arguments.length;
				Alignment argumentsAlignment = this.scribe.createAlignment(
						"messageArguments", //$NON-NLS-1$
						this.preferences.alignment_for_arguments_in_method_invocation,
						Alignment.R_OUTERMOST,
						argumentLength,
						this.scribe.scanner.currentPosition);
				this.scribe.enterAlignment(argumentsAlignment);
				boolean okForArguments = false;
				do {
					try {
						for (int j = 0; j < argumentLength; j++) {
							if (j > 0) {
								this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_method_invocation_arguments);
								this.scribe.printTrailingComment();
							}
							this.scribe.alignFragment(argumentsAlignment, j);
							if (j > 0 && this.preferences.insert_space_after_comma_in_method_invocation_arguments) {
								this.scribe.space();
							}
							arguments[j].traverse(this, scope);
						}
						okForArguments = true;
					} catch (AlignmentException e) {
						this.scribe.redoAlignment(e);
					}
				} while (!okForArguments);
				this.scribe.exitAlignment(argumentsAlignment, true);
				this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_before_closing_paren_in_method_invocation);
			} else {
				this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_between_empty_parens_in_method_invocation);
			}
			if (numberOfParens > 0) {
				manageClosingParenthesizedExpression(currentMessageSend, numberOfParens);
			}
			startingPositionInCascade = 2;
		}
		Alignment cascadingMessageSendAlignment =
			this.scribe.createAlignment(
				"cascadingMessageSendAlignment", //$NON-NLS-1$
				this.preferences.alignment_for_selector_in_method_invocation,
				Alignment.R_INNERMOST,
				size,
				this.scribe.scanner.currentPosition);
		this.scribe.enterAlignment(cascadingMessageSendAlignment);
		boolean ok = false;
		do {
			try {
				this.scribe.alignFragment(cascadingMessageSendAlignment, 0);
				//this.scribe.printNextToken(TerminalTokens.TokenNameDOT);
				// Fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=221018
				// functions can return functions in javascript so there won't always be a dot
				this.scribe.printOptionalNextToken(TerminalTokens.TokenNameDOT, false);
				for (int i = startingPositionInCascade; i < size; i++) {
					MessageSend currentMessageSend = fragments[i];
					final int numberOfParens = (currentMessageSend.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
					if (numberOfParens > 0) {
						manageOpeningParenthesizedExpression(currentMessageSend, numberOfParens);
					}
					ASTNode[] arguments = currentMessageSend.arguments;
					// Fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=221018
					// functions can return functions in javascript so there won't always be another identifier name
					this.scribe.printOptionalNextToken(TerminalTokens.TokenNameIdentifier, false); // selector
					this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, this.preferences.insert_space_before_opening_paren_in_method_invocation);
					if (arguments != null) {
						if (this.preferences.insert_space_after_opening_paren_in_method_invocation) {
							this.scribe.space();
						}
						int argumentLength = arguments.length;
						Alignment argumentsAlignment = this.scribe.createAlignment(
								"messageArguments", //$NON-NLS-1$
								this.preferences.alignment_for_arguments_in_method_invocation,
								Alignment.R_OUTERMOST,
								argumentLength,
								this.scribe.scanner.currentPosition);
						this.scribe.enterAlignment(argumentsAlignment);
						boolean okForArguments = false;
						do {
							try {
								for (int j = 0; j < argumentLength; j++) {
									if (j > 0) {
										this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_method_invocation_arguments);
										this.scribe.printTrailingComment();
									}
									this.scribe.alignFragment(argumentsAlignment, j);
									if (j > 0 && this.preferences.insert_space_after_comma_in_method_invocation_arguments) {
										this.scribe.space();
									}
									arguments[j].traverse(this, scope);
								}
								okForArguments = true;
							} catch (AlignmentException e) {
								this.scribe.redoAlignment(e);
							}
						} while (!okForArguments);
						this.scribe.exitAlignment(argumentsAlignment, true);
						this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_before_closing_paren_in_method_invocation);
					} else {
						this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_between_empty_parens_in_method_invocation);
					}
					if (numberOfParens > 0) {
						manageClosingParenthesizedExpression(currentMessageSend, numberOfParens);
					}
					if (i < size - 1) {
						this.scribe.alignFragment(cascadingMessageSendAlignment, i);
						this.scribe.printOptionalNextToken(TerminalTokens.TokenNameDOT, false);
					}
				}
				ok = true;
			} catch(AlignmentException e){
				this.scribe.redoAlignment(e);
			}
		} while (!ok);
		this.scribe.exitAlignment(cascadingMessageSendAlignment, true);
	}

	/*
	 * Merged traversal of member (types, fields, methods)
	 */
	private void formatClassBodyDeclarations(ASTNode[] nodes) {
		final int FIELD = 1, METHOD = 2;//, TYPE = 3;
		this.scribe.lastNumberOfNewLines = 1;
		ASTNode[] mergedNodes = computeMergedMemberDeclarations(nodes);
		Alignment memberAlignment = this.scribe.createMemberAlignment("typeMembers", this.preferences.align_type_members_on_columns ? Alignment.M_MULTICOLUMN : Alignment.M_NO_ALIGNMENT, 4, this.scribe.scanner.currentPosition); //$NON-NLS-1$
		this.scribe.enterMemberAlignment(memberAlignment);
		boolean isChunkStart = false;
		boolean ok = false;
		int startIndex = 0;
		do {
			try {
				for (int i = startIndex, max = mergedNodes.length; i < max; i++) {
					ASTNode member = mergedNodes[i];
					if (member instanceof FieldDeclaration) {
						isChunkStart = memberAlignment.checkChunkStart(FIELD, i, this.scribe.scanner.currentPosition);
						if (member instanceof MultiFieldDeclaration){
							MultiFieldDeclaration multiField = (MultiFieldDeclaration) member;
							format(multiField, this, null, isChunkStart, i == 0);
						} else if (member instanceof Initializer) {
							int newLineBeforeChunk = isChunkStart ? this.preferences.blank_lines_before_new_chunk : 0;
							if (newLineBeforeChunk > 0 && i != 0) {
								this.scribe.printEmptyLines(newLineBeforeChunk);
							} else if (i == 0) {
								int newLinesBeforeFirstClassBodyDeclaration = this.preferences.blank_lines_before_first_class_body_declaration;
								if (newLinesBeforeFirstClassBodyDeclaration > 0) {
									this.scribe.printEmptyLines(newLinesBeforeFirstClassBodyDeclaration);
								}
							}
							Initializer initializer = (Initializer) member;
							initializer.traverse(this, null);
						} else {
							FieldDeclaration field = (FieldDeclaration) member;
							format(field, this, null, isChunkStart, i == 0);
						}
					} else if (member instanceof LocalDeclaration) {
						LocalDeclaration field = (LocalDeclaration) member;
						format(field, this, null, isChunkStart, i == 0);
					} else if (member instanceof AbstractMethodDeclaration) {
						isChunkStart = memberAlignment.checkChunkStart(METHOD, i, this.scribe.scanner.currentPosition);
						format((AbstractMethodDeclaration) member, null, isChunkStart, i == 0);
					} else {
//						isChunkStart = memberAlignment.checkChunkStart(TYPE, i, this.scribe.scanner.currentPosition);
//						format((TypeDeclaration)member, null, isChunkStart, i == 0);
						member.traverse(this, null);
					}
					if (isNextToken(TerminalTokens.TokenNameSEMICOLON)) {
						this.scribe.printNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
						this.scribe.printTrailingComment();
					}
					if (i != max - 1) {
						this.scribe.printNewLine();
					}
				}
				ok = true;
			} catch(AlignmentException e){
				startIndex = memberAlignment.chunkStartIndex;
				this.scribe.redoMemberAlignment(e);
			}
		} while (!ok);
		this.scribe.exitMemberAlignment(memberAlignment);
		if (hasComments()) {
			this.scribe.printNewLine();
		}
		this.scribe.printComment();
	}

	private void formatEmptyTypeDeclaration(boolean isFirst) {
		boolean hasSemiColon = isNextToken(TerminalTokens.TokenNameSEMICOLON);
		while(isNextToken(TerminalTokens.TokenNameSEMICOLON)) {
			this.scribe.printComment();
			this.scribe.printNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
			this.scribe.printTrailingComment();
		}
		if (hasSemiColon && isFirst) {
			this.scribe.printNewLine();
		}
	}

	private void formatGuardClauseBlock(Block block, BlockScope scope) {

		this.scribe.printNextToken(TerminalTokens.TokenNameLBRACE, this.preferences.insert_space_before_opening_brace_in_block);
		this.scribe.space();

		final Statement[] statements = block.statements;
		statements[0].traverse(this, scope);
		this.scribe.printNextToken(TerminalTokens.TokenNameRBRACE, true);
		this.scribe.printTrailingComment();
	}

    private void formatLeftCurlyBrace(final int line, final String bracePosition) {
        /*
         * deal with (quite unexpected) comments right before lcurly
         */
        this.scribe.printComment();
        if (DefaultCodeFormatterConstants.NEXT_LINE_ON_WRAP.equals(bracePosition)
                && (this.scribe.line > line || this.scribe.column >= this.preferences.page_width))
        {
            this.scribe.printNewLine();
        }
    }

	private void formatLocalDeclaration(LocalDeclaration localDeclaration, BlockScope scope, boolean insertSpaceBeforeComma, boolean insertSpaceAfterComma, boolean insertSpaceBeforeVar) {

		if (!isMultipleLocalDeclaration(localDeclaration)) {
			if (localDeclaration.modifiers != NO_MODIFIERS) {
		        this.scribe.printComment();
				this.scribe.printModifiers(this);
				this.scribe.space();
			}

			this.scribe.printNextToken(TerminalTokens.TokenNamevar, insertSpaceBeforeVar);

			/*
			 * Argument type
			 */
			if (localDeclaration.type != null) {
				localDeclaration.type.traverse(this, scope);
			}
			/*
			 * Print the argument name
		 	*/
			this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier, true);
		} else {
			/*
			 * Print the argument name
		 	*/
			this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier, false);
		}
		/*
		 * Check for extra dimensions
		 */
		int extraDimensions = getDimensions();
		if (extraDimensions != 0) {
			 for (int index = 0; index < extraDimensions; index++) {
			 	this.scribe.printNextToken(TerminalTokens.TokenNameLBRACKET);
			 	this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET);
			 }
		}

		final Expression initialization = localDeclaration.initialization;
		if (initialization != null) {
			/*
			 * Print the method name
			 */
			this.scribe.printNextToken(TerminalTokens.TokenNameEQUAL, this.preferences.insert_space_before_assignment_operator);
			if (this.preferences.insert_space_after_assignment_operator) {
				this.scribe.space();
			}
			Alignment assignmentAlignment = this.scribe.createAlignment("localDeclarationAssignmentAlignment", this.preferences.alignment_for_assignment, Alignment.R_OUTERMOST, 1, this.scribe.scanner.currentPosition); //$NON-NLS-1$
			this.scribe.enterAlignment(assignmentAlignment);
			boolean ok = false;
			do {
				try {
					this.scribe.alignFragment(assignmentAlignment, 0);
					initialization.traverse(this, scope);
					ok = true;
				} catch(AlignmentException e){
					this.scribe.redoAlignment(e);
				}
			} while (!ok);
			this.scribe.exitAlignment(assignmentAlignment, true);
		}

		if (isPartOfMultipleLocalDeclaration()) {
			this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, insertSpaceBeforeComma);
			if (insertSpaceAfterComma) {
				this.scribe.space();
			}
			this.scribe.printTrailingComment();
		}
	}

	private void formatMessageSend(
		MessageSend messageSend,
		BlockScope scope,
		Alignment messageAlignment) {

		if (messageAlignment != null) {
			this.scribe.alignFragment(messageAlignment, 0);
			if (messageSend.selector!=null)
				this.scribe.printNextToken(TerminalTokens.TokenNameDOT);
		}
		if (messageSend.selector!=null)
			this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier); // selector
		this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, this.preferences.insert_space_before_opening_paren_in_method_invocation);

		final Expression[] arguments = messageSend.arguments;
		if (arguments != null) {
			if (this.preferences.insert_space_after_opening_paren_in_method_invocation) {
				this.scribe.space();
			}
			int argumentsLength = arguments.length;
			if (argumentsLength > 1) {
				Alignment argumentsAlignment = this.scribe.createAlignment(
						"messageArguments", //$NON-NLS-1$
						this.preferences.alignment_for_arguments_in_method_invocation,
						argumentsLength,
						this.scribe.scanner.currentPosition);
				this.scribe.enterAlignment(argumentsAlignment);
				boolean ok = false;
				do {
					try {
						for (int i = 0; i < argumentsLength; i++) {
							if (i > 0) {
								this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_method_invocation_arguments);
								this.scribe.printTrailingComment();
							}
							this.scribe.alignFragment(argumentsAlignment, i);
							if (i > 0 && this.preferences.insert_space_after_comma_in_method_invocation_arguments) {
								this.scribe.space();
							}
							arguments[i].traverse(this, scope);
						}
						ok = true;
					} catch (AlignmentException e) {
						this.scribe.redoAlignment(e);
					}
				} while (!ok);
				this.scribe.exitAlignment(argumentsAlignment, true);
			} else {
				for (int i = 0; i < argumentsLength; i++) {
					if (i > 0) {
						this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_method_invocation_arguments);
						this.scribe.printTrailingComment();
					}
					if (i > 0 && this.preferences.insert_space_after_comma_in_method_invocation_arguments) {
						this.scribe.space();
					}
					arguments[i].traverse(this, scope);
				}
			}
			this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_before_closing_paren_in_method_invocation);
		} else {
			this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_between_empty_parens_in_method_invocation);
		}
	}

	private void formatMethodArguments(
			AbstractMethodDeclaration methodDeclaration,
			boolean spaceBeforeOpenParen,
			boolean spaceBetweenEmptyParameters,
			boolean spaceBeforeClosingParen,
			boolean spaceBeforeFirstParameter,
			boolean spaceBeforeComma,
			boolean spaceAfterComma,
			int methodDeclarationParametersAlignment) {

		this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, spaceBeforeOpenParen);

		final Argument[] arguments = methodDeclaration.arguments;
		if (arguments != null) {
			if (spaceBeforeFirstParameter) {
				this.scribe.space();
			}
			int argumentLength = arguments.length;
			Alignment argumentsAlignment = this.scribe.createAlignment(
					"methodArguments",//$NON-NLS-1$
					methodDeclarationParametersAlignment,
					argumentLength,
					this.scribe.scanner.currentPosition);
			this.scribe.enterAlignment(argumentsAlignment);
			boolean ok = false;
			do {
				try {
					for (int i = 0; i < argumentLength; i++) {
						if (i > 0) {
							this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, spaceBeforeComma);
							this.scribe.printTrailingComment();
						}
						this.scribe.alignFragment(argumentsAlignment, i);
						if (i > 0 && spaceAfterComma) {
							this.scribe.space();
						}
						arguments[i].traverse(this, methodDeclaration.getScope());
					}
					ok = true;
				} catch (AlignmentException e) {
					this.scribe.redoAlignment(e);
				}
			} while (!ok);
			this.scribe.exitAlignment(argumentsAlignment, true);

			this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, spaceBeforeClosingParen);
		} else {
			this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, spaceBetweenEmptyParameters);
		}
	}

	private void formatEnumConstantArguments(
			FieldDeclaration enumConstant,
			boolean spaceBeforeOpenParen,
			boolean spaceBetweenEmptyParameters,
			boolean spaceBeforeClosingParen,
			boolean spaceBeforeFirstParameter,
			boolean spaceBeforeComma,
			boolean spaceAfterComma,
			int methodDeclarationParametersAlignment) {

		if (!isNextToken(TerminalTokens.TokenNameLPAREN)) {
			return;
		}

		this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, spaceBeforeOpenParen);
		final Expression[] arguments = ((AllocationExpression) enumConstant.initialization).arguments;
		if (arguments != null) {
			int argumentLength = arguments.length;
			Alignment argumentsAlignment = this.scribe.createAlignment(
					"enumConstantArguments",//$NON-NLS-1$
					methodDeclarationParametersAlignment,
					argumentLength,
					this.scribe.scanner.currentPosition);
			this.scribe.enterAlignment(argumentsAlignment);
			boolean ok = false;
			do {
				try {
					if (spaceBeforeFirstParameter) {
						this.scribe.space();
					}
					for (int i = 0; i < argumentLength; i++) {
						if (i > 0) {
							this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, spaceBeforeComma);
							this.scribe.printTrailingComment();
						}
						this.scribe.alignFragment(argumentsAlignment, i);
						if (i > 0 && spaceAfterComma) {
							this.scribe.space();
						}
						arguments[i].traverse(this, (BlockScope) null);
					}
					ok = true;
				} catch (AlignmentException e) {
					this.scribe.redoAlignment(e);
				}
			} while (!ok);
			this.scribe.exitAlignment(argumentsAlignment, true);

			this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, spaceBeforeClosingParen);
		} else {
			this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, spaceBetweenEmptyParameters);
		}
	}

	private void formatNecessaryEmptyStatement() {
		if (this.preferences.put_empty_statement_on_new_line) {
			this.scribe.printNewLine();
			this.scribe.indent();
			this.scribe.printNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
			this.scribe.printTrailingComment();
			this.scribe.unIndent();
		} else {
			this.scribe.printNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
			this.scribe.printTrailingComment();
		}
	}

	private void formatOpeningBrace(String bracePosition, boolean insertSpaceBeforeBrace) {

		if (DefaultCodeFormatterConstants.NEXT_LINE.equals(bracePosition)) {
			this.scribe.printNewLine();
		} else if (DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED.equals(bracePosition)) {
			this.scribe.printNewLine();
			this.scribe.indent();
		}
		this.scribe.printNextToken(TerminalTokens.TokenNameLBRACE, insertSpaceBeforeBrace);

		this.scribe.printTrailingComment();
	}


	private void formatOpeningBracket(String bracePosition, boolean insertSpaceBeforeBrace) {

		if (DefaultCodeFormatterConstants.NEXT_LINE.equals(bracePosition)) {
			this.scribe.printNewLine();
		} else if (DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED.equals(bracePosition)) {
			this.scribe.printNewLine();
			this.scribe.indent();
		}
		this.scribe.printNextToken(TerminalTokens.TokenNameLBRACKET, insertSpaceBeforeBrace);

		this.scribe.printTrailingComment();
	}
	private void formatStatements(BlockScope scope, final Statement[] statements, boolean insertNewLineAfterLastStatement) {
		int statementsLength = statements.length;
		for (int i = 0; i < statementsLength; i++) {
			final Statement statement = statements[i];
			if (i > 0 && (statements[i - 1] instanceof EmptyStatement) && !(statement instanceof EmptyStatement)) {
				this.scribe.printNewLine();
			}
			statement.traverse(this, scope);
			if (statement instanceof Expression) {
				this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
				this.scribe.printTrailingComment();
				if (i != statementsLength - 1) {
					if (!(statement instanceof EmptyStatement) && !(statements[i + 1] instanceof EmptyStatement)) {
						this.scribe.printNewLine();
					}
				} else if (i == statementsLength - 1 && insertNewLineAfterLastStatement) {
					this.scribe.printNewLine();
				}
			} else if (statement instanceof LocalDeclaration) {
				LocalDeclaration currentLocal = (LocalDeclaration) statement;
				if (i < (statementsLength - 1)) {
					/*
					 * We need to check that the next statement is a local declaration
					 */
					if (statements[i + 1] instanceof LocalDeclaration) {
						LocalDeclaration nextLocal = (LocalDeclaration) statements[i + 1];
						if (currentLocal.declarationSourceStart != nextLocal.declarationSourceStart) {
							this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
							this.scribe.printTrailingComment();
							if (i != statementsLength - 1) {
								if (!(statement instanceof EmptyStatement) && !(statements[i + 1] instanceof EmptyStatement)) {
									this.scribe.printNewLine();
								}
							} else if (i == statementsLength - 1 && insertNewLineAfterLastStatement) {
								this.scribe.printNewLine();
							}
						}
					} else {
						this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
						this.scribe.printTrailingComment();
						if (i != statementsLength - 1) {
							if (!(statement instanceof EmptyStatement) && !(statements[i + 1] instanceof EmptyStatement)) {
								this.scribe.printNewLine();
							}
						} else if (i == statementsLength - 1 && insertNewLineAfterLastStatement) {
							this.scribe.printNewLine();
						}
					}
				} else {
					this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
					this.scribe.printTrailingComment();
					if (i != statementsLength - 1) {
						if (!(statement instanceof EmptyStatement) && !(statements[i + 1] instanceof EmptyStatement)) {
							this.scribe.printNewLine();
						}
					} else if (i == statementsLength - 1 && insertNewLineAfterLastStatement) {
						this.scribe.printNewLine();
					}
				}
			} else if (i != statementsLength - 1) {
				if (!(statement instanceof EmptyStatement) && !(statements[i + 1] instanceof EmptyStatement)) {
					this.scribe.printNewLine();
				}
			} else if (i == statementsLength - 1 && insertNewLineAfterLastStatement) {
				this.scribe.printNewLine();
			}
		}
	}

	/*
	 * Merged traversal of member (types, fields, methods)
	 */
	private void formatTypeMembers(TypeDeclaration typeDeclaration) {
		Alignment memberAlignment = this.scribe.createMemberAlignment("typeMembers", this.preferences.align_type_members_on_columns ? Alignment.M_MULTICOLUMN : Alignment.M_NO_ALIGNMENT, 3, this.scribe.scanner.currentPosition); //$NON-NLS-1$
		this.scribe.enterMemberAlignment(memberAlignment);
		ASTNode[] members = computeMergedMemberDeclarations(typeDeclaration);
		boolean isChunkStart = false;
		boolean ok = false;
		int startIndex = 0;
		do {
			try {
				for (int i = startIndex, max = members.length; i < max; i++) {
					ASTNode member = members[i];
					if (member instanceof FieldDeclaration) {
						isChunkStart = memberAlignment.checkChunkStart(Alignment.CHUNK_FIELD, i, this.scribe.scanner.currentPosition);
						if (member instanceof MultiFieldDeclaration) {
							MultiFieldDeclaration multiField = (MultiFieldDeclaration) member;

							if (multiField.isStatic()) {
								format(multiField, this, typeDeclaration.staticInitializerScope, isChunkStart, i == 0);
							} else {
								format(multiField, this, typeDeclaration.initializerScope, isChunkStart, i == 0);
							}
						} else if (member instanceof Initializer) {
							int newLineBeforeChunk = isChunkStart ? this.preferences.blank_lines_before_new_chunk : 0;
							if (newLineBeforeChunk > 0 && i != 0) {
								this.scribe.printEmptyLines(newLineBeforeChunk);
							} else if (i == 0) {
								int newLinesBeforeFirstClassBodyDeclaration = this.preferences.blank_lines_before_first_class_body_declaration;
								if (newLinesBeforeFirstClassBodyDeclaration > 0) {
									this.scribe.printEmptyLines(newLinesBeforeFirstClassBodyDeclaration);
								}
							}
							Initializer initializer = (Initializer) member;
							if (initializer.isStatic()) {
								initializer.traverse(this, typeDeclaration.staticInitializerScope);
							} else {
								initializer.traverse(this, typeDeclaration.initializerScope);
							}
						} else {
							FieldDeclaration field = (FieldDeclaration) member;
							if (field.isStatic()) {
								format(field, this, typeDeclaration.staticInitializerScope, isChunkStart, i == 0);
							} else {
								format(field, this, typeDeclaration.initializerScope, isChunkStart, i == 0);
							}
						}
					} else if (member instanceof AbstractMethodDeclaration) {
						isChunkStart = memberAlignment.checkChunkStart(Alignment.CHUNK_METHOD, i, this.scribe.scanner.currentPosition);
						format((AbstractMethodDeclaration) member, typeDeclaration.scope, isChunkStart, i == 0);
					} else if (member instanceof TypeDeclaration) {
						isChunkStart = memberAlignment.checkChunkStart(Alignment.CHUNK_TYPE, i, this.scribe.scanner.currentPosition);
						format((TypeDeclaration)member, typeDeclaration.scope, isChunkStart, i == 0);
					}
					if (isNextToken(TerminalTokens.TokenNameSEMICOLON)) {
						this.scribe.printNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
						this.scribe.printTrailingComment();
					}
					this.scribe.printNewLine();
					// realign to the proper value
					if (this.scribe.memberAlignment != null) {
						// select the last alignment
						this.scribe.indentationLevel = this.scribe.memberAlignment.originalIndentationLevel;
					}
				}
				ok = true;
			} catch(AlignmentException e){
				startIndex = memberAlignment.chunkStartIndex;
				this.scribe.redoMemberAlignment(e);
			}
		} while (!ok);
		this.scribe.printComment();
		this.scribe.exitMemberAlignment(memberAlignment);
	}

	private void formatTypeOpeningBraceForEnumConstant(String bracePosition, boolean insertSpaceBeforeBrace, TypeDeclaration typeDeclaration) {
		int fieldCount = (typeDeclaration.fields == null) ? 0 : typeDeclaration.fields.length;
		int methodCount = (typeDeclaration.methods == null) ? 0 : typeDeclaration.methods.length;
		int typeCount = (typeDeclaration.memberTypes == null) ? 0 : typeDeclaration.memberTypes.length;

		if (methodCount <= 2) {
			for (int i = 0, max = methodCount; i < max; i++) {
				final AbstractMethodDeclaration abstractMethodDeclaration = typeDeclaration.methods[i];
				if (abstractMethodDeclaration.isDefaultConstructor()) {
					methodCount--;
				} else if (abstractMethodDeclaration.isClinit()) {
					methodCount--;
				}
			}
		}
		final int memberLength = fieldCount + methodCount+typeCount;

		boolean insertNewLine = memberLength > 0;

		if (!insertNewLine) {
			if ((typeDeclaration.bits & ASTNode.IsAnonymousType) != 0) {
				insertNewLine = this.preferences.insert_new_line_in_empty_enum_constant;
			}
		}

		formatOpeningBrace(bracePosition, insertSpaceBeforeBrace);

		if (insertNewLine) {
			this.scribe.printNewLine();
		}
	}
	private void formatTypeOpeningBrace(String bracePosition, boolean insertSpaceBeforeBrace, TypeDeclaration typeDeclaration) {
		int fieldCount = (typeDeclaration.fields == null) ? 0 : typeDeclaration.fields.length;
		int methodCount = (typeDeclaration.methods == null) ? 0 : typeDeclaration.methods.length;
		int typeCount = (typeDeclaration.memberTypes == null) ? 0 : typeDeclaration.memberTypes.length;

		if (methodCount <= 2) {
			for (int i = 0, max = methodCount; i < max; i++) {
				final AbstractMethodDeclaration abstractMethodDeclaration = typeDeclaration.methods[i];
				if (abstractMethodDeclaration.isDefaultConstructor()) {
					methodCount--;
				} else if (abstractMethodDeclaration.isClinit()) {
					methodCount--;
				}
			}
		}
		final int memberLength = fieldCount + methodCount + typeCount;

		boolean insertNewLine = memberLength > 0;

		if (!insertNewLine) {
			if ((typeDeclaration.bits & ASTNode.IsAnonymousType) != 0) {
				insertNewLine = this.preferences.insert_new_line_in_empty_anonymous_type_declaration;
			} else {
				insertNewLine = this.preferences.insert_new_line_in_empty_type_declaration;
			}
		}

		formatOpeningBrace(bracePosition, insertSpaceBeforeBrace);

		if (insertNewLine) {
			this.scribe.printNewLine();
		}
	}
	private int getDimensions() {

		this.localScanner.resetTo(this.scribe.scanner.currentPosition, this.scribe.scannerEndPosition - 1);
		int dimensions = 0;
		int balance = 0;
		try {
			int token;
			loop: while ((token = this.localScanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameRBRACKET:
						dimensions++;
						balance--;
						break;
					case TerminalTokens.TokenNameCOMMENT_BLOCK :
					case TerminalTokens.TokenNameCOMMENT_JAVADOC :
					case TerminalTokens.TokenNameCOMMENT_LINE :
						break;
					case TerminalTokens.TokenNameLBRACKET :
						balance++;
						break;
					default:
						break loop;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		if (balance == 0) {
			return dimensions;
		}
		return 0;
	}

	private boolean hasComments() {

		this.localScanner.resetTo(this.scribe.scanner.startPosition, this.scribe.scannerEndPosition - 1);
		try {
			switch(this.localScanner.getNextToken()) {
				case TerminalTokens.TokenNameCOMMENT_BLOCK :
				case TerminalTokens.TokenNameCOMMENT_JAVADOC :
				case TerminalTokens.TokenNameCOMMENT_LINE :
					return true;
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return false;
	}

	private boolean isNextToken(int tokenName) {
		this.localScanner.resetTo(this.scribe.scanner.currentPosition, this.scribe.scannerEndPosition - 1);
		try {
			int token = this.localScanner.getNextToken();
			loop: while(true) {
				switch(token) {
					case TerminalTokens.TokenNameCOMMENT_BLOCK :
					case TerminalTokens.TokenNameCOMMENT_JAVADOC :
					case TerminalTokens.TokenNameCOMMENT_LINE :
						token = this.localScanner.getNextToken();
						continue loop;
					default:
						break loop;
				}
			}
			return  token == tokenName;
		} catch(InvalidInputException e) {
			// ignore
		}
		return false;
	}

	private boolean isClosingGenericToken() {
		this.localScanner.resetTo(this.scribe.scanner.currentPosition, this.scribe.scannerEndPosition - 1);
		try {
			int token = this.localScanner.getNextToken();
			loop: while(true) {
				switch(token) {
					case TerminalTokens.TokenNameCOMMENT_BLOCK :
					case TerminalTokens.TokenNameCOMMENT_JAVADOC :
					case TerminalTokens.TokenNameCOMMENT_LINE :
						token = this.localScanner.getNextToken();
						continue loop;
					default:
						break loop;
				}
			}
			switch(token) {
				case TerminalTokens.TokenNameGREATER :
				case TerminalTokens.TokenNameRIGHT_SHIFT :
				case TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT :
					return true;
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return false;
	}

	private boolean isGuardClause(Block block) {
		return !commentStartsBlock(block.sourceStart, block.sourceEnd)
				&& block.statements != null
				&& block.statements.length == 1
				&& (block.statements[0] instanceof ReturnStatement || block.statements[0] instanceof ThrowStatement);
	}

	private boolean isMultipleLocalDeclaration(LocalDeclaration localDeclaration) {

		if (localDeclaration.declarationSourceStart == this.lastLocalDeclarationSourceStart) return true;
		this.lastLocalDeclarationSourceStart = localDeclaration.declarationSourceStart;
		return false;
	}

	private boolean isPartOfMultipleLocalDeclaration() {
		this.localScanner.resetTo(this.scribe.scanner.currentPosition, this.scribe.scannerEndPosition - 1);
		try {
			int token;
			while ((token = this.localScanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameCOMMA ://90
						return true;
					case TerminalTokens.TokenNameCOMMENT_BLOCK :
					case TerminalTokens.TokenNameCOMMENT_JAVADOC :
					case TerminalTokens.TokenNameCOMMENT_LINE :
						break;
					default:
						return false;
				}
			}
		} catch(InvalidInputException e) {
			// ignore
		}
		return false;
	}

	private void manageClosingParenthesizedExpression(Expression expression, int numberOfParens) {
		for (int i = 0; i < numberOfParens; i++) {
			this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_before_closing_paren_in_parenthesized_expression);
		}
	}

	private void manageOpeningParenthesizedExpression(Expression expression, int numberOfParens) {
		for (int i = 0; i < numberOfParens; i++) {
			this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, this.preferences.insert_space_before_opening_paren_in_parenthesized_expression);
			if (this.preferences.insert_space_after_opening_paren_in_parenthesized_expression) {
				this.scribe.space();
			}
		}
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
		AllocationExpression allocationExpression,
		BlockScope scope) {
		// 'new' ClassType '(' ArgumentListopt ')' ClassBodyopt

		final int numberOfParens = (allocationExpression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(allocationExpression, numberOfParens);
		}
		this.scribe.printNextToken(TerminalTokens.TokenNamenew);
		this.scribe.space();
		
		if (allocationExpression.type!=null)
			allocationExpression.type.traverse(this, scope);
			else
				allocationExpression.member.traverse(this, scope);

		if (!allocationExpression.isShort) {
			this.scribe
					.printNextToken(
							TerminalTokens.TokenNameLPAREN,
							this.preferences.insert_space_before_opening_paren_in_method_invocation);
			final Expression[] arguments = allocationExpression.arguments;
			if (arguments != null) {
				if (this.preferences.insert_space_after_opening_paren_in_method_invocation) {
					this.scribe.space();
				}
				int argumentLength = arguments.length;
				Alignment argumentsAlignment = this.scribe
						.createAlignment(
								"allocation",//$NON-NLS-1$
								this.preferences.alignment_for_arguments_in_allocation_expression,
								argumentLength,
								this.scribe.scanner.currentPosition);
				this.scribe.enterAlignment(argumentsAlignment);
				boolean ok = false;
				do {
					try {
						for (int i = 0; i < argumentLength; i++) {
							if (i > 0) {
								this.scribe
										.printNextToken(
												TerminalTokens.TokenNameCOMMA,
												this.preferences.insert_space_before_comma_in_allocation_expression);
								this.scribe.printTrailingComment();
							}
							this.scribe.alignFragment(argumentsAlignment, i);
							if (i > 0
									&& this.preferences.insert_space_after_comma_in_allocation_expression) {
								this.scribe.space();
							}
							arguments[i].traverse(this, scope);
						}
						ok = true;
					} catch (AlignmentException e) {
						this.scribe.redoAlignment(e);
					}
				} while (!ok);
				this.scribe.exitAlignment(argumentsAlignment, true);
				this.scribe
						.printNextToken(
								TerminalTokens.TokenNameRPAREN,
								this.preferences.insert_space_before_closing_paren_in_method_invocation);
			} else {
				this.scribe
						.printNextToken(
								TerminalTokens.TokenNameRPAREN,
								this.preferences.insert_space_between_empty_parens_in_method_invocation);
			}
		}		
		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(allocationExpression, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.AND_AND_Expression, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
		AND_AND_Expression and_and_Expression,
		BlockScope scope) {

		return dumpBinaryExpression(and_and_Expression, TerminalTokens.TokenNameAND_AND, scope);
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.Argument, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(Argument argument, BlockScope scope) {

		if (argument.modifiers != NO_MODIFIERS) {
	        this.scribe.printComment();
			this.scribe.printModifiers(this);
			this.scribe.space();
		}

		/*
		 * Argument type
		 */
		if (argument.type != null) {
			argument.type.traverse(this, scope);
		}

//		if (argument.isVarArgs()) {
//			this.scribe.printNextToken(TerminalTokens.TokenNameELLIPSIS, this.preferences.insert_space_before_ellipsis);
//			if (this.preferences.insert_space_after_ellipsis) {
//				this.scribe.space();
//			}
//			this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier, false);
//		} else {
			/*
			 * Print the argument name
			 */
			this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier, false);
//		}


		/*
		 * Check for extra dimensions
		 */
		int extraDimensions = getDimensions();
		if (extraDimensions != 0) {
			 for (int i = 0; i < extraDimensions; i++) {
			 	this.scribe.printNextToken(TerminalTokens.TokenNameLBRACKET);
			 	this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET);
			 }
		}

		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ArrayAllocationExpression, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
		ArrayAllocationExpression arrayAllocationExpression,
		BlockScope scope) {

			final int numberOfParens = (arrayAllocationExpression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
			if (numberOfParens > 0) {
				manageOpeningParenthesizedExpression(arrayAllocationExpression, numberOfParens);
			}
			this.scribe.printNextToken(TerminalTokens.TokenNamenew);
			this.scribe.space();
			arrayAllocationExpression.type.traverse(this, scope);

			final Expression[] dimensions = arrayAllocationExpression.dimensions;
			int dimensionsLength = dimensions.length;
			for (int i = 0; i < dimensionsLength; i++) {
				if (this.preferences.insert_space_before_opening_bracket_in_array_allocation_expression) {
					this.scribe.space();
				}
				this.scribe.printNextToken(TerminalTokens.TokenNameLBRACKET, false);
				if (dimensions[i] != null) {
					if (this.preferences.insert_space_after_opening_bracket_in_array_allocation_expression) {
						this.scribe.space();
					}
					dimensions[i].traverse(this, scope);
					this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET, this.preferences.insert_space_before_closing_bracket_in_array_allocation_expression);
				} else {
					this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET, this.preferences.insert_space_between_empty_brackets_in_array_allocation_expression);
				}
			}
			final ArrayInitializer initializer = arrayAllocationExpression.initializer;
			if (initializer != null) {
				initializer.traverse(this, scope);
			}

			if (numberOfParens > 0) {
				manageClosingParenthesizedExpression(arrayAllocationExpression, numberOfParens);
			}
			return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ArrayInitializer, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {		final int numberOfParens = (arrayInitializer.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(arrayInitializer, numberOfParens);
		}

		final Expression[] expressions = arrayInitializer.expressions;
		if (expressions != null) {
			String array_initializer_brace_position = this.preferences.brace_position_for_array_initializer;
			formatOpeningBracket(array_initializer_brace_position, false);

			int expressionsLength = expressions.length;
			final boolean insert_new_line_after_opening_brace = this.preferences.insert_new_line_after_opening_brace_in_array_initializer;
			if (expressionsLength > 1) {
				if (insert_new_line_after_opening_brace) {
					this.scribe.printNewLine();
				}
				Alignment arrayInitializerAlignment =this.scribe.createAlignment(
						"array_initializer",//$NON-NLS-1$
						this.preferences.alignment_for_expressions_in_array_initializer,
						Alignment.R_OUTERMOST,
						expressionsLength,
						this.scribe.scanner.currentPosition,
						this.preferences.continuation_indentation_for_array_initializer,
						true);

				if (insert_new_line_after_opening_brace) {
				    arrayInitializerAlignment.fragmentIndentations[0] = arrayInitializerAlignment.breakIndentationLevel;
				}

				this.scribe.enterAlignment(arrayInitializerAlignment);
				boolean ok = false;
				do {
					try {
						this.scribe.alignFragment(arrayInitializerAlignment, 0);
						if (this.preferences.insert_space_after_opening_brace_in_array_initializer) {
							this.scribe.space();
						}
						expressions[0].traverse(this, scope);
						for (int i = 1; i < expressionsLength; i++) {
							this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_array_initializer);
							this.scribe.printTrailingComment();
							this.scribe.alignFragment(arrayInitializerAlignment, i);
							if (this.preferences.insert_space_after_comma_in_array_initializer) {
								this.scribe.space();
							}
							expressions[i].traverse(this, scope);
							if (i == expressionsLength - 1) {
								if (isNextToken(TerminalTokens.TokenNameCOMMA)) {
									this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_array_initializer);
									this.scribe.printTrailingComment();
								}
							}
						}
						ok = true;
					} catch (AlignmentException e) {
						this.scribe.redoAlignment(e);
					}
				} while (!ok);
				this.scribe.exitAlignment(arrayInitializerAlignment, true);
			} else {
				if (insert_new_line_after_opening_brace) {
					this.scribe.printNewLine();
					this.scribe.indent();
				}
				// we don't need to use an alignment
				if (this.preferences.insert_space_after_opening_brace_in_array_initializer) {
					this.scribe.space();
				} else {
					this.scribe.needSpace = false;
				}
				expressions[0].traverse(this, scope);
				if (isNextToken(TerminalTokens.TokenNameCOMMA)) {
					this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_array_initializer);
					this.scribe.printTrailingComment();
				}
				if (insert_new_line_after_opening_brace) {
					this.scribe.unIndent();
				}
			}
			if (this.preferences.insert_new_line_before_closing_brace_in_array_initializer) {
				this.scribe.printNewLine();
			} else if (this.preferences.insert_space_before_closing_brace_in_array_initializer) {
				this.scribe.space();
			}
			this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET, false);
			if (array_initializer_brace_position.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
				this.scribe.unIndent();
			}
		} else {
			boolean keepEmptyArrayInitializerOnTheSameLine = this.preferences.keep_empty_array_initializer_on_one_line;
			
			if (keepEmptyArrayInitializerOnTheSameLine) {
				this.scribe.printNextToken(TerminalTokens.TokenNameLBRACKET, false);
				this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET, this.preferences.insert_space_between_empty_braces_in_array_initializer);
			} else {
				String array_initializer_brace_position = this.preferences.brace_position_for_array_initializer;
				formatOpeningBracket(array_initializer_brace_position, false);
				this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET, this.preferences.insert_space_between_empty_braces_in_array_initializer);
				if (array_initializer_brace_position.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
					this.scribe.unIndent();
				}
			}
		}

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(arrayInitializer, numberOfParens);
		}
		return false;
	}

	public boolean visit(ObjectLiteral objectLiteral, BlockScope scope) {
	final int numberOfParens = (objectLiteral.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
	if (numberOfParens > 0) {
		manageOpeningParenthesizedExpression(objectLiteral, numberOfParens);
	}

	final ObjectLiteralField[] expressions = objectLiteral.fields;
	if (expressions != null) {
		String objlit_initializer_brace_position = this.preferences.brace_position_for_objlit_initializer;
		formatOpeningBrace(objlit_initializer_brace_position, false);

		int expressionsLength = expressions.length;
		final boolean insert_new_line_after_opening_brace = this.preferences.insert_new_line_after_opening_brace_in_objlit_initializer;
		if (expressionsLength > 1) {
			if (insert_new_line_after_opening_brace) {
				this.scribe.printNewLine();
			}
			Alignment objlitInitializerAlignment =this.scribe.createAlignment(
					"objlit_initializer",//$NON-NLS-1$
					this.preferences.alignment_for_expressions_in_array_initializer,
					Alignment.R_OUTERMOST,
					expressionsLength,
					this.scribe.scanner.currentPosition,
					this.preferences.continuation_indentation_for_objlit_initializer,
					true);

			if (insert_new_line_after_opening_brace) {
			    objlitInitializerAlignment.fragmentIndentations[0] = objlitInitializerAlignment.breakIndentationLevel;
			}

			this.scribe.enterAlignment(objlitInitializerAlignment);
			boolean ok = false;
			do {
				try {
					this.scribe.alignFragment(objlitInitializerAlignment, 0);
					if (this.preferences.insert_space_after_opening_brace_in_array_initializer) {
						this.scribe.space();
					}
					expressions[0].traverse(this, scope);
					for (int i = 1; i < expressionsLength; i++) {
						this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_array_initializer);
						this.scribe.printTrailingComment();
						this.scribe.alignFragment(objlitInitializerAlignment, i);
						if (this.preferences.insert_space_after_comma_in_array_initializer) {
							this.scribe.space();
						}

						if (this.preferences.insert_new_line_after_comma_in_objlit_initializer) {
							this.scribe.printNewLine();
						}


						expressions[i].traverse(this, scope);

						if (i == expressionsLength - 1) {
							if (isNextToken(TerminalTokens.TokenNameCOMMA)) {
								this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_array_initializer);
								this.scribe.printTrailingComment();
							}
						}
					}
					ok = true;
				} catch (AlignmentException e) {
					this.scribe.redoAlignment(e);
				}
			} while (!ok);
			this.scribe.exitAlignment(objlitInitializerAlignment, true);
		} else {
			if (insert_new_line_after_opening_brace) {
				this.scribe.printNewLine();
				this.scribe.indent();
			}
			// we don't need to use an alignment
			if (this.preferences.insert_space_after_opening_brace_in_array_initializer) {
				this.scribe.space();
			} else {
				this.scribe.needSpace = false;
			}
			expressions[0].traverse(this, scope);
			if (isNextToken(TerminalTokens.TokenNameCOMMA)) {
				this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_array_initializer);
				this.scribe.printTrailingComment();
			}
			if (insert_new_line_after_opening_brace) {
				this.scribe.unIndent();
			}
		}
		if (this.preferences.insert_new_line_before_closing_brace_in_objlit_initializer) {
			this.scribe.printNewLine();
		} else if (this.preferences.insert_space_before_closing_brace_in_array_initializer) {
			this.scribe.space();
		}
		this.scribe.printNextToken(TerminalTokens.TokenNameRBRACE, false);
		if (objlit_initializer_brace_position.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
			this.scribe.unIndent();
		}
	} else {
		boolean keepEmptyObjLitInitializerOnTheSameLine = this.preferences.keep_empty_objlit_initializer_on_one_line;
		String objlit_initializer_brace_position = this.preferences.brace_position_for_objlit_initializer;
		if (keepEmptyObjLitInitializerOnTheSameLine) {
			this.scribe.printNextToken(TerminalTokens.TokenNameLBRACE, false);
			this.scribe.printNextToken(TerminalTokens.TokenNameRBRACE, this.preferences.insert_space_between_empty_braces_in_array_initializer);
		} else {
			formatOpeningBrace(objlit_initializer_brace_position, false);
			this.scribe.printNextToken(TerminalTokens.TokenNameRBRACE, false);
			if (objlit_initializer_brace_position.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
				this.scribe.unIndent();
			}
		}
	}

	if (numberOfParens > 0) {
		manageClosingParenthesizedExpression(objectLiteral, numberOfParens);
	}
	return false;
}

	public boolean visit(ObjectLiteralField literalField,BlockScope scope)
	{
		literalField.fieldName.traverse(this, scope);
		this.scribe.printNextToken(TerminalTokens.TokenNameCOLON, this.preferences.insert_space_before_colon_in_object_initializer);
		if(this.preferences.insert_space_after_colon_in_object_initializer) {
			this.scribe.space();
		}
		literalField.initializer.traverse(this, scope);
		return false;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ArrayQualifiedTypeReference, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
		ArrayQualifiedTypeReference arrayQualifiedTypeReference,
		BlockScope scope) {

			final int numberOfParens = (arrayQualifiedTypeReference.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
			if (numberOfParens > 0) {
				manageOpeningParenthesizedExpression(arrayQualifiedTypeReference, numberOfParens);
			}
			this.scribe.printArrayQualifiedReference(arrayQualifiedTypeReference.tokens.length, arrayQualifiedTypeReference.sourceEnd);
			int dimensions = getDimensions();
			if (dimensions != 0) {
				for (int i = 0; i < dimensions; i++) {
					this.scribe.printNextToken(TerminalTokens.TokenNameLBRACKET);
					this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET);
				}
			}
			if (numberOfParens > 0) {
				manageClosingParenthesizedExpression(arrayQualifiedTypeReference, numberOfParens);
			}
			return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ArrayQualifiedTypeReference, org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope)
	 */
	public boolean visit(
		ArrayQualifiedTypeReference arrayQualifiedTypeReference,
		ClassScope scope) {

			final int numberOfParens = (arrayQualifiedTypeReference.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
			if (numberOfParens > 0) {
				manageOpeningParenthesizedExpression(arrayQualifiedTypeReference, numberOfParens);
			}
			this.scribe.printArrayQualifiedReference(arrayQualifiedTypeReference.tokens.length, arrayQualifiedTypeReference.sourceEnd);
			int dimensions = getDimensions();
			if (dimensions != 0) {
				for (int i = 0; i < dimensions; i++) {
					this.scribe.printNextToken(TerminalTokens.TokenNameLBRACKET);
					this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET);
				}
			}
			if (numberOfParens > 0) {
				manageClosingParenthesizedExpression(arrayQualifiedTypeReference, numberOfParens);
			}
			return false;
	}


	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ArrayReference, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(ArrayReference arrayReference, BlockScope scope) {

		final int numberOfParens = (arrayReference.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(arrayReference, numberOfParens);
		}
		arrayReference.receiver.traverse(this, scope);
		this.scribe.printNextToken(TerminalTokens.TokenNameLBRACKET, this.preferences.insert_space_before_opening_bracket_in_array_reference);
		if (this.preferences.insert_space_after_opening_bracket_in_array_reference) {
			this.scribe.space();
		}
		arrayReference.position.traverse(this, scope);
		this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET, this.preferences.insert_space_before_closing_bracket_in_array_reference);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(arrayReference, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ArrayTypeReference, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
		ArrayTypeReference arrayTypeReference,
		BlockScope scope) {

		final int numberOfParens = (arrayTypeReference.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(arrayTypeReference, numberOfParens);
		}
		this.scribe.printNextToken(SINGLETYPEREFERENCE_EXPECTEDTOKENS);

		int dimensions = getDimensions();
		if (dimensions != 0) {
			if (this.preferences.insert_space_before_opening_bracket_in_array_type_reference) {
				this.scribe.space();
			}
			for (int i = 0; i < dimensions; i++) {
				this.scribe.printNextToken(TerminalTokens.TokenNameLBRACKET);
				if (this.preferences.insert_space_between_brackets_in_array_type_reference) {
					this.scribe.space();
				}
				this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET);
			}
		}
		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(arrayTypeReference, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ArrayTypeReference, org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope)
	 */
	public boolean visit(
		ArrayTypeReference arrayTypeReference,
		ClassScope scope) {

		final int numberOfParens = (arrayTypeReference.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(arrayTypeReference, numberOfParens);
		}
		this.scribe.printNextToken(SINGLETYPEREFERENCE_EXPECTEDTOKENS);
		int dimensions = getDimensions();
		if (dimensions != 0) {
			if (this.preferences.insert_space_before_opening_bracket_in_array_type_reference) {
				this.scribe.space();
			}
			for (int i = 0; i < dimensions; i++) {
				this.scribe.printNextToken(TerminalTokens.TokenNameLBRACKET);
				if (this.preferences.insert_space_between_brackets_in_array_type_reference) {
					this.scribe.space();
				}
				this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET);
			}
		}
		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(arrayTypeReference, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.AssertStatement, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
//	public boolean visit(AssertStatement assertStatement, BlockScope scope) {
//
//		this.scribe.printNextToken(TerminalTokens.TokenNameassert);
//		this.scribe.space();
//		assertStatement.assertExpression.traverse(this, scope);
//
//		if (assertStatement.exceptionArgument != null) {
//			this.scribe.printNextToken(TerminalTokens.TokenNameCOLON, this.preferences.insert_space_before_colon_in_assert);
//			if (this.preferences.insert_space_after_colon_in_assert) {
//				this.scribe.space();
//			}
//			assertStatement.exceptionArgument.traverse(this, scope);
//		}
//		this.scribe.printNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
//		this.scribe.printTrailingComment();
//		return false;
//	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.Assignment, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(Assignment assignment, BlockScope scope) {

		final int numberOfParens = (assignment.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(assignment, numberOfParens);
		}
		assignment.lhs.traverse(this, scope);
		this.scribe.printNextToken(TerminalTokens.TokenNameEQUAL, this.preferences.insert_space_before_assignment_operator);
		if (this.preferences.insert_space_after_assignment_operator) {
			this.scribe.space();
		}

		Alignment assignmentAlignment = this.scribe.createAlignment("assignmentAlignment", this.preferences.alignment_for_assignment, Alignment.R_OUTERMOST, 1, this.scribe.scanner.currentPosition); //$NON-NLS-1$
		this.scribe.enterAlignment(assignmentAlignment);
		boolean ok = false;
		do {
			try {
				this.scribe.alignFragment(assignmentAlignment, 0);
				assignment.expression.traverse(this, scope);
				ok = true;
			} catch(AlignmentException e){
				this.scribe.redoAlignment(e);
			}
		} while (!ok);
		this.scribe.exitAlignment(assignmentAlignment, true);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(assignment, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {

		switch((binaryExpression.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT) {
			case OperatorIds.AND :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNameAND, scope);
			case OperatorIds.DIVIDE :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNameDIVIDE, scope);
			case OperatorIds.GREATER :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNameGREATER, scope);
			case OperatorIds.GREATER_EQUAL :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNameGREATER_EQUAL, scope);
			case OperatorIds.LEFT_SHIFT :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNameLEFT_SHIFT, scope);
			case OperatorIds.LESS :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNameLESS, scope);
			case OperatorIds.LESS_EQUAL :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNameLESS_EQUAL, scope);
			case OperatorIds.MINUS :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNameMINUS, scope);
			case OperatorIds.MULTIPLY :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNameMULTIPLY, scope);
			case OperatorIds.OR :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNameOR, scope);
			case OperatorIds.PLUS :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNamePLUS, scope);
			case OperatorIds.REMAINDER :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNameREMAINDER, scope);
			case OperatorIds.RIGHT_SHIFT :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNameRIGHT_SHIFT, scope);
			case OperatorIds.UNSIGNED_RIGHT_SHIFT :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT, scope);
			case OperatorIds.XOR :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNameXOR, scope);
			case OperatorIds.INSTANCEOF :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNameinstanceof, scope);
			case OperatorIds.IN :
				return dumpBinaryExpression(binaryExpression, TerminalTokens.TokenNamein, scope);
			default:
				throw new IllegalStateException();
		}
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.Block, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(Block block, BlockScope scope) {
		formatBlock(block, scope, this.preferences.brace_position_for_block, this.preferences.insert_space_before_opening_brace_in_block);
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.BreakStatement, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(BreakStatement breakStatement, BlockScope scope) {

		this.scribe.printNextToken(TerminalTokens.TokenNamebreak);
		if (breakStatement.label != null) {
			this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier, true);
		}
		this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
		this.scribe.printTrailingComment();
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.CaseStatement, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(CaseStatement caseStatement, BlockScope scope) {
		if (caseStatement.constantExpression == null) {
			this.scribe.printNextToken(TerminalTokens.TokenNamedefault);
			this.scribe.printNextToken(TerminalTokens.TokenNameCOLON, this.preferences.insert_space_before_colon_in_default);
		} else {
			this.scribe.printNextToken(TerminalTokens.TokenNamecase);
			this.scribe.space();
			caseStatement.constantExpression.traverse(this, scope);
			this.scribe.printNextToken(TerminalTokens.TokenNameCOLON, this.preferences.insert_space_before_colon_in_case);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ClassLiteralAccess, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {

		final int numberOfParens = (classLiteral.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(classLiteral, numberOfParens);
		}
		classLiteral.type.traverse(this, scope);
		this.scribe.printNextToken(TerminalTokens.TokenNameDOT);
		this.scribe.printNextToken(TerminalTokens.TokenNameclass);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(classLiteral, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.Clinit, org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope)
	 */
	public boolean visit(Clinit clinit, ClassScope scope) {

		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration, org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope)
	 */
	public boolean visit(
		CompilationUnitDeclaration compilationUnitDeclaration,
		CompilationUnitScope scope) {

		// fake new line to handle empty lines before package declaration or import declarations
		this.scribe.lastNumberOfNewLines = 1;
		/*
		 * Package declaration
		 */
		final boolean hasPackage = compilationUnitDeclaration.currentPackage != null;
		if (hasPackage) {
			if (hasComments()) {
				this.scribe.printComment();
			}
			int blankLinesBeforePackage = this.preferences.blank_lines_before_package;
			if (blankLinesBeforePackage > 0) {
				this.scribe.printEmptyLines(blankLinesBeforePackage);
			}

			this.scribe.space();
			// dump the package keyword
			this.scribe.printNextToken(TerminalTokens.TokenNamepackage);
			this.scribe.space();
			this.scribe.printQualifiedReference(compilationUnitDeclaration.currentPackage.sourceEnd);
			this.scribe.printNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
			this.scribe.printTrailingComment();
			int blankLinesAfterPackage = this.preferences.blank_lines_after_package;
			if (blankLinesAfterPackage > 0) {
				this.scribe.printEmptyLines(blankLinesAfterPackage);
			} else {
				this.scribe.printNewLine();
			}
		} else {
			this.scribe.printComment();
		}

		/*
		 * Import statements
		 */
		final ImportReference[] imports = compilationUnitDeclaration.imports;
		if (imports != null) {
			if (hasPackage) {
				int blankLinesBeforeImports = this.preferences.blank_lines_before_imports;
				if (blankLinesBeforeImports > 0) {
					this.scribe.printEmptyLines(blankLinesBeforeImports);
				}
			}
			int importLength = imports.length;
			int savedNumberOfLineToPreserve = this.preferences.number_of_empty_lines_to_preserve;
			if (importLength != 1) {
				format(imports[0], false);
    			for (int i = 1; i < importLength - 1; i++) {
    				format(imports[i], false);
    			}
    			format(imports[importLength - 1], true);
    			this.preferences.number_of_empty_lines_to_preserve = savedNumberOfLineToPreserve;
			} else {
				format(imports[0], true);
			}
			this.preferences.number_of_empty_lines_to_preserve = savedNumberOfLineToPreserve;

			int blankLinesAfterImports = this.preferences.blank_lines_after_imports;
			if (blankLinesAfterImports > 0) {
				this.scribe.printEmptyLines(blankLinesAfterImports);
			}
		}

		formatEmptyTypeDeclaration(true);

		int blankLineBetweenTypeDeclarations = this.preferences.blank_lines_between_type_declarations;
//		/*
//		 * Type declarations
//		 */
//		final TypeDeclaration[] types = compilationUnitDeclaration.types;
//		if (types != null) {
//			int typesLength = types.length;
//			for (int i = 0; i < typesLength - 1; i++) {
//				types[i].traverse(this, scope);
//				formatEmptyTypeDeclaration(false);
//				if (blankLineBetweenTypeDeclarations != 0) {
//					this.scribe.printEmptyLines(blankLineBetweenTypeDeclarations);
//				} else {
//					this.scribe.printNewLine();
//				}
//			}
//			types[typesLength - 1].traverse(this, scope);
//		}
//
		final ProgramElement[] statements = compilationUnitDeclaration.statements;
		if (statements != null && statements.length>0) {
			int statementsLength = statements.length;
			for (int i = 0; i < statementsLength - 1; i++) {
				statements[i].traverse(this, scope);
				this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
				this.scribe.printTrailingComment();
				if (blankLineBetweenTypeDeclarations != 0) {
					this.scribe.printEmptyLines(blankLineBetweenTypeDeclarations);
				} else {
					this.scribe.printNewLine();
				}
			}
			statements[statementsLength - 1].traverse(this, scope);
		}


		this.scribe.printEndOfCompilationUnit();
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.CompoundAssignment, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
		CompoundAssignment compoundAssignment,
		BlockScope scope) {

		final int numberOfParens = (compoundAssignment.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(compoundAssignment, numberOfParens);
		}
		compoundAssignment.lhs.traverse(this, scope);

		/*
		 * Print the operator
		 */
		int operator;
		switch(compoundAssignment.operator) {
			case OperatorIds.PLUS :
				operator = TerminalTokens.TokenNamePLUS_EQUAL;
				break;
			case OperatorIds.MINUS :
				operator = TerminalTokens.TokenNameMINUS_EQUAL;
				break;
			case OperatorIds.MULTIPLY :
				operator = TerminalTokens.TokenNameMULTIPLY_EQUAL;
				break;
			case OperatorIds.DIVIDE :
				operator = TerminalTokens.TokenNameDIVIDE_EQUAL;
				break;
			case OperatorIds.AND :
				operator = TerminalTokens.TokenNameAND_EQUAL;
				break;
			case OperatorIds.OR :
				operator = TerminalTokens.TokenNameOR_EQUAL;
				break;
			case OperatorIds.XOR :
				operator = TerminalTokens.TokenNameXOR_EQUAL;
				break;
			case OperatorIds.REMAINDER :
				operator = TerminalTokens.TokenNameREMAINDER_EQUAL;
				break;
			case OperatorIds.LEFT_SHIFT :
				operator = TerminalTokens.TokenNameLEFT_SHIFT_EQUAL;
				break;
			case OperatorIds.RIGHT_SHIFT :
				operator = TerminalTokens.TokenNameRIGHT_SHIFT_EQUAL;
				break;
			default: // OperatorIds.UNSIGNED_RIGHT_SHIFT :
				operator = TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL;
		}

		this.scribe.printNextToken(operator, this.preferences.insert_space_before_assignment_operator);
		if (this.preferences.insert_space_after_assignment_operator) {
			this.scribe.space();
		}
		Alignment assignmentAlignment = this.scribe.createAlignment("compoundAssignmentAlignment", this.preferences.alignment_for_assignment, Alignment.R_OUTERMOST, 1, this.scribe.scanner.currentPosition); //$NON-NLS-1$
		this.scribe.enterAlignment(assignmentAlignment);
		boolean ok = false;
		do {
			try {
				this.scribe.alignFragment(assignmentAlignment, 0);
				compoundAssignment.expression.traverse(this, scope);
				ok = true;
			} catch(AlignmentException e){
				this.scribe.redoAlignment(e);
			}
		} while (!ok);
		this.scribe.exitAlignment(assignmentAlignment, true);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(compoundAssignment, numberOfParens);
		}
		return false;
	}

	/**
     * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ConditionalExpression, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
     */
    public boolean visit(
    	ConditionalExpression conditionalExpression,
    	BlockScope scope) {

    	final int numberOfParens = (conditionalExpression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
    	if (numberOfParens > 0) {
    		manageOpeningParenthesizedExpression(conditionalExpression, numberOfParens);
    	}
    	conditionalExpression.condition.traverse(this, scope);

    	Alignment conditionalExpressionAlignment =this.scribe.createAlignment(
    			"conditionalExpression", //$NON-NLS-1$
    			this.preferences.alignment_for_conditional_expression,
    			2,
    			this.scribe.scanner.currentPosition);

    	this.scribe.enterAlignment(conditionalExpressionAlignment);
    	boolean ok = false;
    	do {
    		try {
    			this.scribe.alignFragment(conditionalExpressionAlignment, 0);
    			this.scribe.printNextToken(TerminalTokens.TokenNameQUESTION, this.preferences.insert_space_before_question_in_conditional);

    			if (this.preferences.insert_space_after_question_in_conditional) {
    				this.scribe.space();
    			}
    			conditionalExpression.valueIfTrue.traverse(this, scope);
    			this.scribe.printTrailingComment();
    			this.scribe.alignFragment(conditionalExpressionAlignment, 1);
    			this.scribe.printNextToken(TerminalTokens.TokenNameCOLON, this.preferences.insert_space_before_colon_in_conditional);

    			if (this.preferences.insert_space_after_colon_in_conditional) {
    				this.scribe.space();
    			}
    			conditionalExpression.valueIfFalse.traverse(this, scope);

    			ok = true;
    		} catch (AlignmentException e) {
    			this.scribe.redoAlignment(e);
    		}
    	} while (!ok);
    	this.scribe.exitAlignment(conditionalExpressionAlignment, true);

    	if (numberOfParens > 0) {
    		manageClosingParenthesizedExpression(conditionalExpression, numberOfParens);
    	}
    	return false;
    }


	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration, org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope)
	 */
	public boolean visit(
		ConstructorDeclaration constructorDeclaration,
		ClassScope scope) {

		if (constructorDeclaration.ignoreFurtherInvestigation) {
			this.scribe.printComment();
			if (this.scribe.indentationLevel != 0) {
				this.scribe.printIndentationIfNecessary();
			}
			this.scribe.scanner.resetTo(constructorDeclaration.declarationSourceEnd + 1, this.scribe.scannerEndPosition - 1);
			this.scribe.printTrailingComment();
			switch(this.scribe.scanner.source[this.scribe.scanner.currentPosition]) {
				case '\n' :
					this.scribe.scanner.currentPosition++;
					this.scribe.lastNumberOfNewLines = 1;
					break;
				case '\r' :
					this.scribe.scanner.currentPosition++;
					if (this.scribe.scanner.source[this.scribe.scanner.currentPosition] == '\n') {
						this.scribe.scanner.currentPosition++;
					}
					this.scribe.lastNumberOfNewLines = 1;
			}
			return false;
		}
        /*
         * Print comments to get proper line number
         */
        this.scribe.printComment();
        int line = this.scribe.line;
		this.scribe.printModifiers(this);
		if (this.scribe.line > line) {
        	// annotations introduced new line, but this is not a line wrapping
			// see 158267
			line = this.scribe.line;
		}
		this.scribe.space();
		
		/*
		 * Print the method name
		 */
		this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier, true);

		formatMethodArguments(
			constructorDeclaration,
			this.preferences.insert_space_before_opening_paren_in_constructor_declaration,
			this.preferences.insert_space_between_empty_parens_in_constructor_declaration,
			this.preferences.insert_space_before_closing_paren_in_constructor_declaration,
			this.preferences.insert_space_after_opening_paren_in_constructor_declaration,
			this.preferences.insert_space_before_comma_in_constructor_declaration_parameters,
			this.preferences.insert_space_after_comma_in_constructor_declaration_parameters,
			this.preferences.alignment_for_parameters_in_constructor_declaration);

		if (!constructorDeclaration.isAbstract()) {
			/*
			 * Method body
			 */
			String constructor_declaration_brace = this.preferences.brace_position_for_constructor_declaration;
			formatLeftCurlyBrace(line, constructor_declaration_brace);
			formatOpeningBrace(constructor_declaration_brace, this.preferences.insert_space_before_opening_brace_in_constructor_declaration);
			final int numberOfBlankLinesAtBeginningOfMethodBody = this.preferences.blank_lines_at_beginning_of_method_body;
			if (numberOfBlankLinesAtBeginningOfMethodBody > 0) {
				this.scribe.printEmptyLines(numberOfBlankLinesAtBeginningOfMethodBody);
			}
			if (constructorDeclaration.constructorCall != null && !constructorDeclaration.constructorCall.isImplicitSuper()) {
				this.scribe.printNewLine();
				if (this.preferences.indent_statements_compare_to_body) {
					this.scribe.indent();
				}
				constructorDeclaration.constructorCall.traverse(this, constructorDeclaration.getScope());
				if (this.preferences.indent_statements_compare_to_body) {
					this.scribe.unIndent();
				}
			}
			final Statement[] statements = constructorDeclaration.statements;
			if (statements != null) {
				this.scribe.printNewLine();
				if (this.preferences.indent_statements_compare_to_body) {
					this.scribe.indent();
				}
				formatStatements(constructorDeclaration.getScope(), statements, true);
				this.scribe.printComment();
				if (this.preferences.indent_statements_compare_to_body) {
					this.scribe.unIndent();
				}
			} else if (this.preferences.insert_new_line_in_empty_method_body) {
				this.scribe.printNewLine();
				if (this.preferences.indent_statements_compare_to_body) {
					this.scribe.indent();
				}
				this.scribe.printComment();
				if (this.preferences.indent_statements_compare_to_body) {
					this.scribe.unIndent();
				}
			}
			this.scribe.printNextToken(TerminalTokens.TokenNameRBRACE);
			this.scribe.printTrailingComment();
			if (constructor_declaration_brace.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
				this.scribe.unIndent();
			}
		} else {
			// no method body
			this.scribe.printNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
			this.scribe.printTrailingComment();
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ContinueStatement, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(ContinueStatement continueStatement, BlockScope scope) {

		this.scribe.printNextToken(TerminalTokens.TokenNamecontinue);
		if (continueStatement.label != null) {
			this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier, true);
		}
		this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
		this.scribe.printTrailingComment();
		return false;
	}


	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.DoStatement, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(DoStatement doStatement, BlockScope scope) {

		this.scribe.printNextToken(TerminalTokens.TokenNamedo);
		final int line = this.scribe.line;

		final Statement action = doStatement.action;
		if (action != null) {
			if (action instanceof Block) {
				formatLeftCurlyBrace(line, this.preferences.brace_position_for_block);
				action.traverse(this, scope);
			} else if (action instanceof EmptyStatement) {
				/*
				 * This is an empty statement
				 */
				formatNecessaryEmptyStatement();
			} else {
				this.scribe.printNewLine();
				this.scribe.indent();
				action.traverse(this, scope);
				if (action instanceof Expression || action instanceof LocalDeclaration) {
					this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
					this.scribe.printTrailingComment();
				}
				this.scribe.printNewLine();
				this.scribe.unIndent();
			}
		} else {
			/*
			 * This is an empty statement
			 */
			formatNecessaryEmptyStatement();
		}

		if (this.preferences.insert_new_line_before_while_in_do_statement) {
			this.scribe.printNewLine();
		}
		this.scribe.printNextToken(TerminalTokens.TokenNamewhile, this.preferences.insert_space_after_closing_brace_in_block);
		this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, this.preferences.insert_space_before_opening_paren_in_while);

		if (this.preferences.insert_space_after_opening_paren_in_while) {
			this.scribe.space();
		}

		doStatement.condition.traverse(this, scope);

		this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_before_closing_paren_in_while);
		this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
		this.scribe.printTrailingComment();
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.DoubleLiteral, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(DoubleLiteral doubleLiteral, BlockScope scope) {

		final int numberOfParens = (doubleLiteral.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(doubleLiteral, numberOfParens);
		}
		Constant constant = doubleLiteral.constant;
		if (constant != null && constant.doubleValue() < 0) {
			this.scribe.printNextToken(TerminalTokens.TokenNameMINUS);
		}
		this.scribe.printNextToken(TerminalTokens.TokenNameDoubleLiteral);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(doubleLiteral, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.EmptyStatement, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(EmptyStatement statement, BlockScope scope) {
		if (this.preferences.put_empty_statement_on_new_line) {
			this.scribe.printNewLine();
		}
		this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
		this.scribe.printTrailingComment();
		return false;
	}
	// field is an enum constant
	public boolean visit(FieldDeclaration enumConstant, MethodScope scope) {
        /*
         * Print comments to get proper line number
         */
        this.scribe.printComment();
        final int line = this.scribe.line;

        this.scribe.printModifiers(this);
		this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier, false);
		formatEnumConstantArguments(
			enumConstant,
			this.preferences.insert_space_before_opening_paren_in_enum_constant,
			this.preferences.insert_space_between_empty_parens_in_enum_constant,
			this.preferences.insert_space_before_closing_paren_in_enum_constant,
			this.preferences.insert_space_after_opening_paren_in_enum_constant,
			this.preferences.insert_space_before_comma_in_enum_constant_arguments,
			this.preferences.insert_space_after_comma_in_enum_constant_arguments,
			this.preferences.alignment_for_arguments_in_enum_constant);

		Expression initialization = enumConstant.initialization;
		if (initialization instanceof QualifiedAllocationExpression) {
			TypeDeclaration typeDeclaration = ((QualifiedAllocationExpression) initialization).anonymousType;
			int fieldsCount = typeDeclaration.fields == null ? 0 : typeDeclaration.fields.length;
			int methodsCount = typeDeclaration.methods == null ? 0 : typeDeclaration.methods.length;
			int membersCount = typeDeclaration.memberTypes == null ? 0 : typeDeclaration.memberTypes.length;

			/*
			 * Type body
			 */
			String enum_constant_brace = this.preferences.brace_position_for_enum_constant;

	        formatLeftCurlyBrace(line, enum_constant_brace);
			formatTypeOpeningBraceForEnumConstant(enum_constant_brace, this.preferences.insert_space_before_opening_brace_in_enum_constant, typeDeclaration);

			if (this.preferences.indent_body_declarations_compare_to_enum_constant_header) {
				this.scribe.indent();
			}

			if (fieldsCount != 0 || methodsCount != 0 || membersCount != 0) {
				formatTypeMembers(typeDeclaration);
			}

			if (this.preferences.indent_body_declarations_compare_to_enum_constant_header) {
				this.scribe.unIndent();
			}

			if (this.preferences.insert_new_line_in_empty_enum_constant) {
				this.scribe.printNewLine();
			}
			this.scribe.printNextToken(TerminalTokens.TokenNameRBRACE);
			this.scribe.printTrailingComment();
			if (enum_constant_brace.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
				this.scribe.unIndent();
			}
			if (hasComments()) {
				this.scribe.printNewLine();
			}
		}
		return false;
	}
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.EqualExpression, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(EqualExpression equalExpression, BlockScope scope) {

		int operator = (equalExpression.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT;
		if (operator == OperatorIds.EQUAL_EQUAL) {
			return dumpEqualityExpression(equalExpression, TerminalTokens.TokenNameEQUAL_EQUAL, scope);
		}	if (operator == OperatorIds.NOT_EQUAL) {
			return dumpEqualityExpression(equalExpression, TerminalTokens.TokenNameNOT_EQUAL, scope);
		}	if (operator == OperatorIds.EQUAL_EQUAL_EQUAL) {
			return dumpEqualityExpression(equalExpression, TerminalTokens.TokenNameEQUAL_EQUAL_EQUAL, scope);

		}else {
			return dumpEqualityExpression(equalExpression, TerminalTokens.TokenNameNOT_EQUAL_EQUAL, scope);
		}
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ExplicitConstructorCall, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
		ExplicitConstructorCall explicitConstructor,
		BlockScope scope) {

		if (explicitConstructor.isImplicitSuper()) {
			return false;
		}
		final Expression qualification = explicitConstructor.qualification;
		if (qualification != null) {
			qualification.traverse(this, scope);
			this.scribe.printNextToken(TerminalTokens.TokenNameDOT);
		}

		TypeReference[] typeArguments = explicitConstructor.typeArguments;
		if (typeArguments != null) {
				this.scribe.printNextToken(TerminalTokens.TokenNameLESS, this.preferences.insert_space_before_opening_angle_bracket_in_type_arguments);
				if (this.preferences.insert_space_after_opening_angle_bracket_in_type_arguments) {
					this.scribe.space();
				}
				int length = typeArguments.length;
				for (int i = 0; i < length - 1; i++) {
					typeArguments[i].traverse(this, scope);
					this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_type_arguments);
					if (this.preferences.insert_space_after_comma_in_type_arguments) {
						this.scribe.space();
					}
				}
				typeArguments[length - 1].traverse(this, scope);
				if (isClosingGenericToken()) {
					this.scribe.printNextToken(CLOSING_GENERICS_EXPECTEDTOKENS, this.preferences.insert_space_before_closing_angle_bracket_in_type_arguments);
				}
				if (this.preferences.insert_space_after_closing_angle_bracket_in_type_arguments) {
					this.scribe.space();
				}
		}

		if (explicitConstructor.isSuperAccess()) {
			this.scribe.printNextToken(TerminalTokens.TokenNamesuper);
		} else {
			this.scribe.printNextToken(TerminalTokens.TokenNamethis);
		}
		this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, this.preferences.insert_space_before_opening_paren_in_method_invocation);

		final Expression[] arguments = explicitConstructor.arguments;
		if (arguments != null) {
			if (this.preferences.insert_space_after_opening_paren_in_method_invocation) {
				this.scribe.space();
			}
			int argumentLength = arguments.length;
			Alignment argumentsAlignment =this.scribe.createAlignment(
					"explicit_constructor_call",//$NON-NLS-1$
					this.preferences.alignment_for_arguments_in_explicit_constructor_call,
					argumentLength,
					this.scribe.scanner.currentPosition);
			this.scribe.enterAlignment(argumentsAlignment);
			boolean ok = false;
			do {
				try {
					for (int i = 0; i < argumentLength; i++) {
						if (i > 0) {
							this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_explicit_constructor_call_arguments);
							this.scribe.printTrailingComment();
						}
						this.scribe.alignFragment(argumentsAlignment, i);
						if (i > 0 && this.preferences.insert_space_after_comma_in_explicit_constructor_call_arguments) {
							this.scribe.space();
						}
						arguments[i].traverse(this, scope);
					}
					ok = true;
				} catch (AlignmentException e) {
					this.scribe.redoAlignment(e);
				}
			} while (!ok);
			this.scribe.exitAlignment(argumentsAlignment, true);
			this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_before_closing_paren_in_method_invocation);
		} else {
			this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_between_empty_parens_in_method_invocation);
		}
		this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
		this.scribe.printTrailingComment();
		return false;
	}
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.FalseLiteral, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(FalseLiteral falseLiteral, BlockScope scope) {

		final int numberOfParens = (falseLiteral.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(falseLiteral, numberOfParens);
		}
		this.scribe.printNextToken(TerminalTokens.TokenNamefalse);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(falseLiteral, numberOfParens);
		}
		return false;
	}
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(FieldReference fieldReference, BlockScope scope) {

		final int numberOfParens = (fieldReference.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(fieldReference, numberOfParens);
		}
		fieldReference.receiver.traverse(this, scope);
		this.scribe.printNextToken(TerminalTokens.TokenNameDOT);
		this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(fieldReference, numberOfParens);
		}
		return false;
	}

	public boolean visit(ForeachStatement forStatement, BlockScope scope) {
		this.scribe.printNextToken(TerminalTokens.TokenNamefor);
	    final int line = this.scribe.line;
	    this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, this.preferences.insert_space_before_opening_paren_in_for);

		if (this.preferences.insert_space_after_opening_paren_in_for) {
			this.scribe.space();
		}
		formatLocalDeclaration(forStatement.elementVariable, 
				scope, 
				false, 
				false,
				this.preferences.insert_space_after_opening_paren_in_for);

		this.scribe.printNextToken(TerminalTokens.TokenNameCOLON, this.preferences.insert_space_before_colon_in_for);
		if (this.preferences.insert_space_after_colon_in_for) {
			this.scribe.space();
		}
		forStatement.collection.traverse(this, scope);

		this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_before_closing_paren_in_for);

		final Statement action = forStatement.action;
		if (action != null) {
			if (action instanceof Block) {
	            formatLeftCurlyBrace(line, this.preferences.brace_position_for_block);
				action.traverse(this, scope);
			} else if (action instanceof EmptyStatement) {
				/*
				 * This is an empty statement
				 */
				formatNecessaryEmptyStatement();
			} else {
				this.scribe.indent();
				this.scribe.printNewLine();
				action.traverse(this, scope);
				this.scribe.unIndent();
			}
			if (action instanceof Expression || action instanceof LocalDeclaration) {
				this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
				this.scribe.printTrailingComment();
			}
		} else {
			/*
			 * This is an empty statement
			 */
			formatNecessaryEmptyStatement();
		}
		return false;
	}

	public boolean visit(ForInStatement forStatement, BlockScope scope) {
		this.scribe.printNextToken(TerminalTokens.TokenNamefor);
	    final int line = this.scribe.line;
	    this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, this.preferences.insert_space_before_opening_paren_in_for);

		if (this.preferences.insert_space_after_opening_paren_in_for) {
			this.scribe.space();
		}
		forStatement.iterationVariable.traverse(this, scope);

		this.scribe.printNextToken(TerminalTokens.TokenNamein, this.preferences.insert_space_before_colon_in_for);
		if (this.preferences.insert_space_after_colon_in_for) {
			this.scribe.space();
		}
		forStatement.collection.traverse(this, scope);

		this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_before_closing_paren_in_for);

		final Statement action = forStatement.action;
		if (action != null) {
			if (action instanceof Block) {
	            formatLeftCurlyBrace(line, this.preferences.brace_position_for_block);
				action.traverse(this, scope);
			} else if (action instanceof EmptyStatement) {
				/*
				 * This is an empty statement
				 */
				formatNecessaryEmptyStatement();
			} else {
				this.scribe.indent();
				this.scribe.printNewLine();
				action.traverse(this, scope);
				this.scribe.unIndent();
			}
			if (action instanceof Expression || action instanceof LocalDeclaration) {
				this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
				this.scribe.printTrailingComment();
			}
		} else {
			/*
			 * This is an empty statement
			 */
			formatNecessaryEmptyStatement();
		}
		return false;
	}



	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ForStatement, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(ForStatement forStatement, BlockScope scope) {

		this.scribe.printNextToken(TerminalTokens.TokenNamefor);
	    final int line = this.scribe.line;
	    this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, this.preferences.insert_space_before_opening_paren_in_for);

		if (this.preferences.insert_space_after_opening_paren_in_for) {
			this.scribe.space();
		}
		final Statement[] initializations = forStatement.initializations;
		if (initializations != null) {
			int length = initializations.length;
			for (int i = 0; i < length; i++) {
				if (initializations[i] instanceof LocalDeclaration) {
					formatLocalDeclaration(
							(LocalDeclaration) initializations[i], 
							scope, 
							this.preferences.insert_space_before_comma_in_for_inits, 
							this.preferences.insert_space_after_comma_in_for_inits,
							this.preferences.insert_space_after_opening_paren_in_for);
				} else {
					initializations[i].traverse(this, scope);
					if (i >= 0 && (i < length - 1)) {
						this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_for_inits);
						if (this.preferences.insert_space_after_comma_in_for_inits) {
							this.scribe.space();
						}
						this.scribe.printTrailingComment();
					}
				}
			}
		}
		this.scribe.printNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon_in_for);
		final Expression condition = forStatement.condition;
		if (condition != null) {
			if (this.preferences.insert_space_after_semicolon_in_for) {
				this.scribe.space();
			}
			condition.traverse(this, scope);
		}
		this.scribe.printNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon_in_for);
		final Statement[] increments = forStatement.increments;
		if (increments != null) {
			if (this.preferences.insert_space_after_semicolon_in_for) {
				this.scribe.space();
			}
			for (int i = 0, length = increments.length; i < length; i++) {
				increments[i].traverse(this, scope);
				if (i != length - 1) {
					this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_for_increments);
					if (this.preferences.insert_space_after_comma_in_for_increments) {
						this.scribe.space();
					}
					this.scribe.printTrailingComment();
				}
			}
		}
		this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_before_closing_paren_in_for);

		final Statement action = forStatement.action;
		if (action != null) {
			if (action instanceof Block) {
	            formatLeftCurlyBrace(line, this.preferences.brace_position_for_block);
				action.traverse(this, scope);
			} else if (action instanceof EmptyStatement) {
				/*
				 * This is an empty statement
				 */
				formatNecessaryEmptyStatement();
			} else {
				this.scribe.indent();
				this.scribe.printNewLine();
				action.traverse(this, scope);
				this.scribe.unIndent();
			}
			if (action instanceof Expression || action instanceof LocalDeclaration) {
				this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
				this.scribe.printTrailingComment();
			}
		} else {
			/*
			 * This is an empty statement
			 */
			formatNecessaryEmptyStatement();
		}
		return false;
	}

	public boolean visit(FunctionExpression functionExpression, BlockScope scope) {

		final int numberOfParens = (functionExpression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(functionExpression, numberOfParens);
		}
		visit(functionExpression.methodDeclaration,scope);
		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(functionExpression, numberOfParens);
		}
		return false;
	}


	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.IfStatement, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(IfStatement ifStatement, BlockScope scope) {

		this.scribe.printNextToken(TerminalTokens.TokenNameif);
        final int line = this.scribe.line;
        this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, this.preferences.insert_space_before_opening_paren_in_if);
		if (this.preferences.insert_space_after_opening_paren_in_if) {
			this.scribe.space();
		}
		ifStatement.condition.traverse(this, scope);
		this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_before_closing_paren_in_if);

		final Statement thenStatement = ifStatement.thenStatement;
		final Statement elseStatement = ifStatement.elseStatement;

		boolean thenStatementIsBlock = false;
		if (thenStatement != null) {
			if (thenStatement instanceof Block) {
				thenStatementIsBlock = true;
				if (isGuardClause((Block)thenStatement) && elseStatement == null && this.preferences.keep_guardian_clause_on_one_line) {
					/*
					 * Need a specific formatting for guard clauses
					 * guard clauses are block with a single return or throw
					 * statement
					 */
					 formatGuardClauseBlock((Block) thenStatement, scope);
				} else {
					formatLeftCurlyBrace(line, this.preferences.brace_position_for_block);
					thenStatement.traverse(this, scope);
					if (elseStatement != null && (this.preferences.insert_new_line_before_else_in_if_statement)) {
						this.scribe.printNewLine();
					}
				}
			} else if (elseStatement == null && this.preferences.keep_simple_if_on_one_line) {
				Alignment compactIfAlignment = this.scribe.createAlignment(
						"compactIf", //$NON-NLS-1$
						this.preferences.alignment_for_compact_if,
						Alignment.R_OUTERMOST,
						1,
						this.scribe.scanner.currentPosition,
						1,
						false);
				this.scribe.enterAlignment(compactIfAlignment);
				boolean ok = false;
				do {
					try {
						this.scribe.alignFragment(compactIfAlignment, 0);
						this.scribe.space();
						thenStatement.traverse(this, scope);
						if (thenStatement instanceof Expression || thenStatement instanceof LocalDeclaration) {
							this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
							this.scribe.printTrailingComment();
						}
						ok = true;
					} catch (AlignmentException e) {
						this.scribe.redoAlignment(e);
					}
				} while (!ok);
				this.scribe.exitAlignment(compactIfAlignment, true);
			} else if (this.preferences.keep_then_statement_on_same_line) {
				this.scribe.space();
				thenStatement.traverse(this, scope);
				if (thenStatement instanceof Expression || thenStatement instanceof LocalDeclaration) {
					this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
					this.scribe.printTrailingComment();
				}
				if (elseStatement != null) {
					this.scribe.printNewLine();
				}
			} else {
				this.scribe.printTrailingComment();
				this.scribe.printNewLine();
				this.scribe.indent();
				thenStatement.traverse(this, scope);
				if (thenStatement instanceof Expression || thenStatement instanceof LocalDeclaration) {
					this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
					this.scribe.printTrailingComment();
				}
				if (elseStatement != null) {
					this.scribe.printNewLine();
				}
				this.scribe.unIndent();
			}
		}

		if (elseStatement != null) {
			if (thenStatementIsBlock) {
				this.scribe.printNextToken(TerminalTokens.TokenNameelse, this.preferences.insert_space_after_closing_brace_in_block);
			} else {
				this.scribe.printNextToken(TerminalTokens.TokenNameelse, true);
			}
			if (elseStatement instanceof Block) {
				elseStatement.traverse(this, scope);
			} else if (elseStatement instanceof IfStatement) {
				if (!this.preferences.compact_else_if) {
					this.scribe.printNewLine();
					this.scribe.indent();
				}
				this.scribe.space();
				elseStatement.traverse(this, scope);
				if (!this.preferences.compact_else_if) {
					this.scribe.unIndent();
				}
			} else if (this.preferences.keep_else_statement_on_same_line) {
				this.scribe.space();
				elseStatement.traverse(this, scope);
				if (elseStatement instanceof Expression || elseStatement instanceof LocalDeclaration) {
					this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
					this.scribe.printTrailingComment();
				}
			} else {
				this.scribe.printNewLine();
				this.scribe.indent();
				elseStatement.traverse(this, scope);
				if (elseStatement instanceof Expression || elseStatement instanceof LocalDeclaration) {
					this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
					this.scribe.printTrailingComment();
				}
				this.scribe.unIndent();
			}
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.Initializer, org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope)
	 */
	public boolean visit(Initializer initializer, MethodScope scope) {

		if (initializer.isStatic()) {
			this.scribe.printNextToken(TerminalTokens.TokenNamestatic);
		}
		initializer.block.traverse(this, scope);
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.InstanceOfExpression, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
		InstanceOfExpression instanceOfExpression,
		BlockScope scope) {

		final int numberOfParens = (instanceOfExpression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(instanceOfExpression, numberOfParens);
		}
		instanceOfExpression.expression.traverse(this, scope);
		this.scribe.printNextToken(TerminalTokens.TokenNameinstanceof, true);
		this.scribe.space();
		instanceOfExpression.type.traverse(this, scope);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(instanceOfExpression, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.IntLiteral, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(IntLiteral intLiteral, BlockScope scope) {

		final int numberOfParens = (intLiteral.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(intLiteral, numberOfParens);
		}
		Constant constant = intLiteral.constant;
		if (constant != null && constant.intValue() < 0) {
			this.scribe.printNextToken(TerminalTokens.TokenNameMINUS);
		}
		this.scribe.printNextToken(TerminalTokens.TokenNameIntegerLiteral);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(intLiteral, numberOfParens);
		}
		return false;
	}


	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.LabeledStatement, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(LabeledStatement labeledStatement, BlockScope scope) {

		this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier);
		this.scribe.printNextToken(TerminalTokens.TokenNameCOLON, this.preferences.insert_space_before_colon_in_labeled_statement);
		if (this.preferences.insert_space_after_colon_in_labeled_statement) {
			this.scribe.space();
		}
		final Statement statement = labeledStatement.statement;
		statement.traverse(this, scope);
		if (statement instanceof Expression || statement instanceof LocalDeclaration) {
			this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
			this.scribe.printTrailingComment();
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		formatLocalDeclaration(
				localDeclaration, 
				scope, 
				this.preferences.insert_space_before_comma_in_multiple_local_declarations, 
				this.preferences.insert_space_after_comma_in_multiple_local_declarations,
				true);
		return false;
	}
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(MessageSend messageSend, BlockScope scope) {

		final int numberOfParens = (messageSend.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(messageSend, numberOfParens);
		}
		CascadingMethodInvocationFragmentBuilder builder = buildFragments(messageSend, scope);

		if (builder.size() >= 3 && numberOfParens == 0) {
			formatCascadingMessageSends(builder, scope);
		} else {
			Alignment messageAlignment = null;
			if (messageSend.receiver!=null && !messageSend.receiver.isImplicitThis()) {
				messageSend.receiver.traverse(this, scope);
				messageAlignment = this.scribe.createAlignment(
						"messageAlignment", //$NON-NLS-1$
						this.preferences.alignment_for_selector_in_method_invocation,
						1,
						this.scribe.scanner.currentPosition);
				this.scribe.enterAlignment(messageAlignment);
				boolean ok = false;
				do {
					try {
						formatMessageSend(messageSend, scope, messageAlignment);
						ok = true;
					} catch (AlignmentException e) {
						this.scribe.redoAlignment(e);
					}
				} while (!ok);
				this.scribe.exitAlignment(messageAlignment, true);
			} else {
				formatMessageSend(messageSend, scope, null);
			}
		}
		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(messageSend, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration, org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope)
	 */
	public boolean visit(
		MethodDeclaration methodDeclaration,
		Scope scope) {

		if (methodDeclaration.ignoreFurtherInvestigation) {
			this.scribe.printComment();
			if (this.scribe.indentationLevel != 0) {
				this.scribe.printIndentationIfNecessary();
			}
			this.scribe.scanner.resetTo(methodDeclaration.declarationSourceEnd + 1, this.scribe.scannerEndPosition - 1);
			this.scribe.printTrailingComment();
			switch(this.scribe.scanner.source[this.scribe.scanner.currentPosition]) {
				case '\n' :
					this.scribe.scanner.currentPosition++;
					this.scribe.lastNumberOfNewLines = 1;
					break;
				case '\r' :
					this.scribe.scanner.currentPosition++;
					if (this.scribe.scanner.source[this.scribe.scanner.currentPosition] == '\n') {
						this.scribe.scanner.currentPosition++;
					}
					this.scribe.lastNumberOfNewLines = 1;
			}
			return false;
		}

        /*
         * Print comments to get proper line number
         */
        this.scribe.printComment();
        int line = this.scribe.line;

        this.scribe.printModifiers(this);

		if (this.scribe.line > line) {
        	// annotations introduced new line, but this is not a line wrapping
			// see 158267
			line = this.scribe.line;
		}
		// fix for  258956
		//this.scribe.space();

		/*
		 * Print the method name
		 */
		this.scribe.printNextToken(TerminalTokens.TokenNamefunction);

		if (methodDeclaration.getName()!=null)
			this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier, true);

		formatMethodArguments(
			methodDeclaration,
			this.preferences.insert_space_before_opening_paren_in_method_declaration,
			this.preferences.insert_space_between_empty_parens_in_method_declaration,
			this.preferences.insert_space_before_closing_paren_in_method_declaration,
			this.preferences.insert_space_after_opening_paren_in_method_declaration,
			this.preferences.insert_space_before_comma_in_method_declaration_parameters,
			this.preferences.insert_space_after_comma_in_method_declaration_parameters,
			this.preferences.alignment_for_parameters_in_method_declaration);

		/*
		 * Check for extra dimensions
		 */
		int extraDimensions = getDimensions();
		if (extraDimensions != 0) {
			 for (int i = 0; i < extraDimensions; i++) {
			 	this.scribe.printNextToken(TerminalTokens.TokenNameLBRACKET);
			 	this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET);
			 }
		}

		final MethodScope methodDeclarationScope = methodDeclaration.getScope();
		
		if (!methodDeclaration.isAbstract() && ((methodDeclaration.modifiers & ExtraCompilerModifiers.AccSemicolonBody) == 0)) {
			/*
			 * Method body
			 */
			String method_declaration_brace = this.preferences.brace_position_for_method_declaration;
            formatLeftCurlyBrace(line, method_declaration_brace);
			formatOpeningBrace(method_declaration_brace, this.preferences.insert_space_before_opening_brace_in_method_declaration);
			final int numberOfBlankLinesAtBeginningOfMethodBody = this.preferences.blank_lines_at_beginning_of_method_body;
			if (numberOfBlankLinesAtBeginningOfMethodBody > 0) {
				this.scribe.printEmptyLines(numberOfBlankLinesAtBeginningOfMethodBody);
			}
			final Statement[] statements = methodDeclaration.statements;
			if (statements != null) {
				this.scribe.printNewLine();
				if (this.preferences.indent_statements_compare_to_body) {
					this.scribe.indent();
				}
				formatStatements(methodDeclarationScope, statements, true);
				this.scribe.printComment();
				if (this.preferences.indent_statements_compare_to_body) {
					this.scribe.unIndent();
				}
			} else if (this.preferences.insert_new_line_in_empty_method_body) {
				this.scribe.printNewLine();
				if (this.preferences.indent_statements_compare_to_body) {
					this.scribe.indent();
				}
				this.scribe.printComment();
				if (this.preferences.indent_statements_compare_to_body) {
					this.scribe.unIndent();
				}
			}
			this.scribe.printNextToken(TerminalTokens.TokenNameRBRACE);
			this.scribe.printTrailingComment();
			if (method_declaration_brace.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
				this.scribe.unIndent();
			}
		} else {
			// no method body
			this.scribe.printNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
			this.scribe.printTrailingComment();
		}
		return false;
	}
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.NullLiteral, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(NullLiteral nullLiteral, BlockScope scope) {

		final int numberOfParens = (nullLiteral.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(nullLiteral, numberOfParens);
		}
		this.scribe.printNextToken(TerminalTokens.TokenNamenull);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(nullLiteral, numberOfParens);
		}
		return false;
	}


	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.OR_OR_Expression, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		return dumpBinaryExpression(or_or_Expression, TerminalTokens.TokenNameOR_OR, scope);
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.PostfixExpression, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
		PostfixExpression postfixExpression,
		BlockScope scope) {

		final int numberOfParens = (postfixExpression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(postfixExpression, numberOfParens);
		}
		postfixExpression.lhs.traverse(this, scope);
		int operator = postfixExpression.operator == OperatorIds.PLUS
			? TerminalTokens.TokenNamePLUS_PLUS : TerminalTokens.TokenNameMINUS_MINUS;
		this.scribe.printNextToken(operator, this.preferences.insert_space_before_postfix_operator);
		if (this.preferences.insert_space_after_postfix_operator) {
			this.scribe.space();
		}
		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(postfixExpression, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.PrefixExpression, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {

		final int numberOfParens = (prefixExpression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(prefixExpression, numberOfParens);
		}
		int operator = prefixExpression.operator == OperatorIds.PLUS
			? TerminalTokens.TokenNamePLUS_PLUS : TerminalTokens.TokenNameMINUS_MINUS;
		this.scribe.printNextToken(operator, this.preferences.insert_space_before_prefix_operator);
		if (this.preferences.insert_space_after_prefix_operator) {
			this.scribe.space();
		}
		prefixExpression.lhs.traverse(this, scope);
		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(prefixExpression, numberOfParens);
		}
		return false;
	}

	public boolean visit(RegExLiteral regexLiteral, BlockScope scope) {

		final int numberOfParens = (regexLiteral.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(regexLiteral, numberOfParens);
		}
		this.scribe.printNextToken(TerminalTokens.TokenNameRegExLiteral);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(regexLiteral, numberOfParens);
		}
		return false;
	}


	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedAllocationExpression, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
		QualifiedAllocationExpression qualifiedAllocationExpression,
		BlockScope scope) {

		final int numberOfParens = (qualifiedAllocationExpression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(qualifiedAllocationExpression, numberOfParens);
		}
		final Expression enclosingInstance = qualifiedAllocationExpression.enclosingInstance;
		if (enclosingInstance != null) {
			enclosingInstance.traverse(this, scope);
			this.scribe.printNextToken(TerminalTokens.TokenNameDOT);
		}

		this.scribe.printNextToken(TerminalTokens.TokenNamenew);
		this.scribe.space();

		final int line = this.scribe.line;
		qualifiedAllocationExpression.type.traverse(this, scope);

		this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, this.preferences.insert_space_before_opening_paren_in_method_invocation);

		final Expression[] arguments = qualifiedAllocationExpression.arguments;
		if (arguments != null) {
			if (this.preferences.insert_space_after_opening_paren_in_method_invocation) {
				this.scribe.space();
			}
			int argumentLength = arguments.length;
			Alignment argumentsAlignment =this.scribe.createAlignment(
					"allocation",//$NON-NLS-1$
					this.preferences.alignment_for_arguments_in_qualified_allocation_expression,
					argumentLength,
					this.scribe.scanner.currentPosition);
			this.scribe.enterAlignment(argumentsAlignment);
			boolean ok = false;
			do {
				try {
					for (int i = 0; i < argumentLength; i++) {
						if (i > 0) {
							this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_allocation_expression);
							this.scribe.printTrailingComment();
						}
						this.scribe.alignFragment(argumentsAlignment, i);
						if (i > 0 && this.preferences.insert_space_after_comma_in_allocation_expression) {
							this.scribe.space();
						}
						arguments[i].traverse(this, scope);
					}
					ok = true;
				} catch (AlignmentException e) {
					this.scribe.redoAlignment(e);
				}
			} while (!ok);
			this.scribe.exitAlignment(argumentsAlignment, true);
			this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_before_closing_paren_in_method_invocation);
		} else {
			this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_between_empty_parens_in_method_invocation);
		}
		final TypeDeclaration anonymousType = qualifiedAllocationExpression.anonymousType;
		if (anonymousType != null) {
			formatLeftCurlyBrace(line, this.preferences.brace_position_for_anonymous_type_declaration);
			formatAnonymousTypeDeclaration(anonymousType);
		}
		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(qualifiedAllocationExpression, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
		QualifiedNameReference qualifiedNameReference,
		BlockScope scope) {

		final int numberOfParens = (qualifiedNameReference.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(qualifiedNameReference, numberOfParens);
		}
		this.scribe.printQualifiedReference(qualifiedNameReference.sourceEnd);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(qualifiedNameReference, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedThisReference, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
		QualifiedThisReference qualifiedThisReference,
		BlockScope scope) {

		final int numberOfParens = (qualifiedThisReference.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(qualifiedThisReference, numberOfParens);
		}
		qualifiedThisReference.qualification.traverse(this, scope);
		this.scribe.printNextToken(TerminalTokens.TokenNameDOT);
		this.scribe.printNextToken(TerminalTokens.TokenNamethis);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(qualifiedThisReference, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
		QualifiedTypeReference qualifiedTypeReference,
		BlockScope scope) {

		final int numberOfParens = (qualifiedTypeReference.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(qualifiedTypeReference, numberOfParens);
		}
		this.scribe.printQualifiedReference(qualifiedTypeReference.sourceEnd);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(qualifiedTypeReference, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference, org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope)
	 */
	public boolean visit(
		QualifiedTypeReference qualifiedTypeReference,
		ClassScope scope) {

			final int numberOfParens = (qualifiedTypeReference.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
			if (numberOfParens > 0) {
				manageOpeningParenthesizedExpression(qualifiedTypeReference, numberOfParens);
			}
			this.scribe.printQualifiedReference(qualifiedTypeReference.sourceEnd);

			if (numberOfParens > 0) {
				manageClosingParenthesizedExpression(qualifiedTypeReference, numberOfParens);
			}
			return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ReturnStatement, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(ReturnStatement returnStatement, BlockScope scope) {

		this.scribe.printNextToken(TerminalTokens.TokenNamereturn);
		final Expression expression = returnStatement.expression;
		if (expression != null) {
			final int numberOfParens = (expression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
			if ((numberOfParens != 0 && this.preferences.insert_space_before_parenthesized_expression_in_return)
					|| numberOfParens == 0) {
				this.scribe.space();
			}
			expression.traverse(this, scope);
		}
		/*
		 * Print the semi-colon
		 */
		this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
		this.scribe.printTrailingComment();
		return false;
	}
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(SingleNameReference singleNameReference, BlockScope scope) {

		final int numberOfParens = (singleNameReference.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(singleNameReference, numberOfParens);
		}
		this.scribe.printNextToken(SINGLETYPEREFERENCE_EXPECTEDTOKENS);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(singleNameReference, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.SingleTypeReference, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
		SingleTypeReference singleTypeReference,
		BlockScope scope) {

		final int numberOfParens = (singleTypeReference.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(singleTypeReference, numberOfParens);
		}
		this.scribe.printNextToken(SINGLETYPEREFERENCE_EXPECTEDTOKENS);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(singleTypeReference, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.SingleTypeReference, org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope)
	 */
	public boolean visit(
		SingleTypeReference singleTypeReference,
		ClassScope scope) {

		final int numberOfParens = (singleTypeReference.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(singleTypeReference, numberOfParens);
		}
		this.scribe.printNextToken(SINGLETYPEREFERENCE_EXPECTEDTOKENS);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(singleTypeReference, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteral, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(StringLiteral stringLiteral, BlockScope scope) {
		final int numberOfParens = (stringLiteral.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(stringLiteral, numberOfParens);
		}
		this.scribe.checkNLSTag(stringLiteral.sourceStart);
		this.scribe.printNextToken(new int[]{TerminalTokens.TokenNameCharacterLiteral, TerminalTokens.TokenNameStringLiteral});
		this.scribe.printTrailingComment();
		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(stringLiteral, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.NullLiteral, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(StringLiteralConcatenation stringLiteral, BlockScope scope) {
		final int numberOfParens = (stringLiteral.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(stringLiteral, numberOfParens);
		}

		this.scribe.printComment();
		ASTNode[] fragments = stringLiteral.literals;
		int fragmentsSize = stringLiteral.counter;
		Alignment binaryExpressionAlignment = this.scribe.createAlignment("binaryExpressionAlignment", this.preferences.alignment_for_binary_expression, Alignment.R_OUTERMOST, fragmentsSize, this.scribe.scanner.currentPosition); //$NON-NLS-1$
		this.scribe.enterAlignment(binaryExpressionAlignment);
		boolean ok = false;
		do {
			try {
				for (int i = 0; i < fragmentsSize - 1; i++) {
					ASTNode fragment = fragments[i];
					fragment.traverse(this, scope);
					this.scribe.printTrailingComment();
					if (this.scribe.lastNumberOfNewLines == 1) {
						// a new line has been inserted by printTrailingComment()
						this.scribe.indentationLevel = binaryExpressionAlignment.breakIndentationLevel;
					}
					this.scribe.alignFragment(binaryExpressionAlignment, i);
					this.scribe.printNextToken(TerminalTokens.TokenNamePLUS, this.preferences.insert_space_before_binary_operator);
					if (this.preferences.insert_space_after_binary_operator) {
						this.scribe.space();
					}
				}
				fragments[fragmentsSize - 1].traverse(this, scope);
				this.scribe.printTrailingComment();
				ok = true;
			} catch(AlignmentException e){
				this.scribe.redoAlignment(e);
			}
		} while (!ok);
		this.scribe.exitAlignment(binaryExpressionAlignment, true);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(stringLiteral, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.SuperReference, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(SuperReference superReference, BlockScope scope) {

		final int numberOfParens = (superReference.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(superReference, numberOfParens);
		}
		this.scribe.printNextToken(TerminalTokens.TokenNamesuper);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(superReference, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.SwitchStatement, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(SwitchStatement switchStatement, BlockScope scope) {
		this.scribe.printNextToken(TerminalTokens.TokenNameswitch);
		this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, this.preferences.insert_space_before_opening_paren_in_switch);

		if (this.preferences.insert_space_after_opening_paren_in_switch) {
			this.scribe.space();
		}

		switchStatement.expression.traverse(this, scope);
		this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_before_closing_paren_in_switch);
		/*
		 * Type body
		 */
		String switch_brace = this.preferences.brace_position_for_switch;
		formatOpeningBrace(switch_brace, this.preferences.insert_space_before_opening_brace_in_switch);
		this.scribe.printNewLine();

		if (this.preferences.indent_switchstatements_compare_to_switch) {
			this.scribe.indent();
		}
		final Statement[] statements = switchStatement.statements;
		boolean wasACase = false;
		boolean wasAStatement = false;
		if (statements != null) {
			int statementsLength = statements.length;
			for (int i = 0; i < statementsLength; i++) {
				final Statement statement = statements[i];
				if (statement instanceof CaseStatement) {
					if (wasACase) {
						this.scribe.printNewLine();
					}
					if ((wasACase && this.preferences.indent_switchstatements_compare_to_cases)
						|| (wasAStatement && this.preferences.indent_switchstatements_compare_to_cases)) {
						this.scribe.unIndent();
					}
					statement.traverse(this, scope);
					this.scribe.printTrailingComment();
					wasACase = true;
					wasAStatement = false;
					if (this.preferences.indent_switchstatements_compare_to_cases) {
						this.scribe.indent();
					}
				} else if (statement instanceof BreakStatement) {
					if (this.preferences.indent_breaks_compare_to_cases) {
						if (wasAStatement && !this.preferences.indent_switchstatements_compare_to_cases) {
							this.scribe.indent();
						}
					} else {
						if (wasAStatement) {
							if (this.preferences.indent_switchstatements_compare_to_cases) {
								this.scribe.unIndent();
							}
						}
						if (wasACase && this.preferences.indent_switchstatements_compare_to_cases) {
							this.scribe.unIndent();
						}
					}
					if (wasACase) {
						this.scribe.printNewLine();
					}
					statement.traverse(this, scope);
					if (this.preferences.indent_breaks_compare_to_cases) {
						this.scribe.unIndent();
					}
					wasACase = false;
					wasAStatement = false;
				} else if (statement instanceof Block) {
					String bracePosition;
					if (wasACase) {
						if (this.preferences.indent_switchstatements_compare_to_cases) {
							this.scribe.unIndent();
						}
						bracePosition =	this.preferences.brace_position_for_block_in_case;
						formatBlock((Block) statement, scope, bracePosition, this.preferences.insert_space_after_colon_in_case);
						if (this.preferences.indent_switchstatements_compare_to_cases) {
							this.scribe.indent();
						}
					} else {
						bracePosition =	this.preferences.brace_position_for_block;
						formatBlock((Block) statement, scope, bracePosition, this.preferences.insert_space_before_opening_brace_in_block);
					}
					wasAStatement = true;
					wasACase = false;
				} else {
					this.scribe.printNewLine();
					statement.traverse(this, scope);
					wasAStatement = true;
					wasACase = false;
				}
				if (statement instanceof Expression || statement instanceof LocalDeclaration) {
					/*
					 * Print the semi-colon
					 */
					this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
					this.scribe.printTrailingComment();
					this.scribe.printNewLine();
				} else if (statement instanceof LocalDeclaration) {
					LocalDeclaration currentLocal = (LocalDeclaration) statement;
					if (i < (statementsLength - 1)) {
						/*
						 * We need to check that the next statement is a local declaration
						 */
						if (statements[i + 1] instanceof LocalDeclaration) {
							LocalDeclaration nextLocal = (LocalDeclaration) statements[i + 1];
							if (currentLocal.declarationSourceStart != nextLocal.declarationSourceStart) {
								/*
								 * Print the semi-colon
								 */
								this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
								this.scribe.printTrailingComment();
								this.scribe.printNewLine();
							}
						} else {
							/*
							 * Print the semi-colon
							 */
							this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
							this.scribe.printTrailingComment();
							this.scribe.printNewLine();
						}
					} else {
						/*
						 * Print the semi-colon
						 */
						this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
						this.scribe.printTrailingComment();
						this.scribe.printNewLine();
					}
				} else if (!wasACase) {
					this.scribe.printNewLine();
				}
				this.scribe.printComment();
			}
		}

		if ((wasACase || wasAStatement) && this.preferences.indent_switchstatements_compare_to_cases) {
			this.scribe.unIndent();
		}
		if (this.preferences.indent_switchstatements_compare_to_switch) {
			this.scribe.unIndent();
		}
		this.scribe.printNewLine();
		this.scribe.printNextToken(TerminalTokens.TokenNameRBRACE);
		this.scribe.printTrailingComment();
		if (switch_brace.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
			this.scribe.unIndent();
		}
		return false;
	}


	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ThisReference, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(ThisReference thisReference, BlockScope scope) {

		if (!thisReference.isImplicitThis()) {
			final int numberOfParens = (thisReference.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
			if (numberOfParens > 0) {
				manageOpeningParenthesizedExpression(thisReference, numberOfParens);
			}
			this.scribe.printNextToken(TerminalTokens.TokenNamethis);

			if (numberOfParens > 0) {
				manageClosingParenthesizedExpression(thisReference, numberOfParens);
			}
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.ThrowStatement, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(ThrowStatement throwStatement, BlockScope scope) {

		this.scribe.printNextToken(TerminalTokens.TokenNamethrow);
		Expression expression = throwStatement.exception;
		final int numberOfParens = (expression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if ((numberOfParens > 0 && this.preferences.insert_space_before_parenthesized_expression_in_throw)
				|| numberOfParens == 0) {
			this.scribe.space();
		}
		expression.traverse(this, scope);
		/*
		 * Print the semi-colon
		 */
		this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
		this.scribe.printTrailingComment();
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.TrueLiteral, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(TrueLiteral trueLiteral, BlockScope scope) {

		final int numberOfParens = (trueLiteral.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(trueLiteral, numberOfParens);
		}
		this.scribe.printNextToken(TerminalTokens.TokenNametrue);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(trueLiteral, numberOfParens);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.TryStatement, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(TryStatement tryStatement, BlockScope scope) {

		this.scribe.printNextToken(TerminalTokens.TokenNametry);
		tryStatement.tryBlock.traverse(this, scope);
		if (tryStatement.catchArguments != null) {
			for (int i = 0, max = tryStatement.catchBlocks.length; i < max; i++) {
				if (this.preferences.insert_new_line_before_catch_in_try_statement) {
					this.scribe.printNewLine();
				}
				this.scribe.printNextToken(TerminalTokens.TokenNamecatch, this.preferences.insert_space_after_closing_brace_in_block);
				final int line = this.scribe.line;
				this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, this.preferences.insert_space_before_opening_paren_in_catch);

				if (this.preferences.insert_space_after_opening_paren_in_catch) {
					this.scribe.space();
				}

				tryStatement.catchArguments[i].traverse(this, scope);

				this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_before_closing_paren_in_catch);

				formatLeftCurlyBrace(line, this.preferences.brace_position_for_block);
				tryStatement.catchBlocks[i].traverse(this, scope);
			}
		}
		if (tryStatement.finallyBlock != null) {
			if (this.preferences.insert_new_line_before_finally_in_try_statement) {
				this.scribe.printNewLine();
			}
			this.scribe.printNextToken(TerminalTokens.TokenNamefinally, this.preferences.insert_space_after_closing_brace_in_block);
			tryStatement.finallyBlock.traverse(this, scope);
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
		TypeDeclaration localTypeDeclaration,
		BlockScope scope) {

			format(localTypeDeclaration);
			return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration, org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope)
	 */
	public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
		format(memberTypeDeclaration);
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration, org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope)
	 */
	public boolean visit(
		TypeDeclaration typeDeclaration,
		CompilationUnitScope scope) {

		format(typeDeclaration);
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.UnaryExpression, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(UnaryExpression unaryExpression, BlockScope scope) {

		final int numberOfParens = (unaryExpression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(unaryExpression, numberOfParens);
		}

		/*
		 * Print the operator
		 */
		int operator;
		boolean addSpace=false;
		int operatorValue = (unaryExpression.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT;
		switch(operatorValue) {
			case OperatorIds.PLUS:
				operator = TerminalTokens.TokenNamePLUS;
				break;
			case OperatorIds.MINUS:
				operator = TerminalTokens.TokenNameMINUS;
				break;
			case OperatorIds.TWIDDLE:
				operator = TerminalTokens.TokenNameTWIDDLE;
				break;
			case OperatorIds.DELETE:
				operator = TerminalTokens.TokenNamedelete;
				addSpace=true;
				break;
			case OperatorIds.VOID:
				operator = TerminalTokens.TokenNamevoid;
				addSpace=true;
				break;
			case OperatorIds.TYPEOF:
				operator = TerminalTokens.TokenNametypeof;
				addSpace=true;
				break;
			default:
				operator = TerminalTokens.TokenNameNOT;
		}

		this.scribe.printNextToken(operator, this.preferences.insert_space_before_unary_operator);
		if (this.preferences.insert_space_after_unary_operator || addSpace) {
			this.scribe.space();
		}
		Expression expression = unaryExpression.expression;

		if (expression instanceof PrefixExpression) {
			PrefixExpression prefixExpression = (PrefixExpression) expression;
			final int numberOfParensForExpression = (prefixExpression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
			if (numberOfParensForExpression == 0) {
				switch(operatorValue) {
					case OperatorIds.PLUS:
						if (prefixExpression.operator == OperatorIds.PLUS) {
							this.scribe.space();
						}
						break;
					case OperatorIds.MINUS:
						if (prefixExpression.operator == OperatorIds.MINUS) {
							this.scribe.space();
						}
						break;
				}
			}
			expression.traverse(this, scope);
		} else {
			expression.traverse(this, scope);
		}

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(unaryExpression, numberOfParens);
		}
		return false;
	}

	public boolean visit(UndefinedLiteral undefinedLiteral, BlockScope scope) {

		final int numberOfParens = (undefinedLiteral.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			manageOpeningParenthesizedExpression(undefinedLiteral, numberOfParens);
		}
		this.scribe.printNextToken(TerminalTokens.TokenNameundefined);

		if (numberOfParens > 0) {
			manageClosingParenthesizedExpression(undefinedLiteral, numberOfParens);
		}
		return false;
	}



	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.WhileStatement, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(WhileStatement whileStatement, BlockScope scope) {

		this.scribe.printNextToken(TerminalTokens.TokenNamewhile);
		final int line = this.scribe.line;
		this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, this.preferences.insert_space_before_opening_paren_in_while);

		if (this.preferences.insert_space_after_opening_paren_in_while) {
			this.scribe.space();
		}
		whileStatement.condition.traverse(this, scope);

		this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_before_closing_paren_in_while);

		final Statement action = whileStatement.action;
		if (action != null) {
			if (action instanceof Block) {
                formatLeftCurlyBrace(line, this.preferences.brace_position_for_block);
				action.traverse(this, scope);
			} else if (action instanceof EmptyStatement) {
				/*
				 * This is an empty statement
				 */
				formatNecessaryEmptyStatement();
			} else {
				this.scribe.printNewLine();
				this.scribe.indent();
				action.traverse(this, scope);
				if (action instanceof Expression || action instanceof LocalDeclaration) {
					this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
					this.scribe.printTrailingComment();
				}
				this.scribe.unIndent();
			}
		} else {
			/*
			 * This is an empty statement
			 */
			formatNecessaryEmptyStatement();
		}
		return false;
	}


	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.WhileStatement, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(WithStatement withStatement, BlockScope scope) {

		this.scribe.printNextToken(TerminalTokens.TokenNamewith);
		final int line = this.scribe.line;
		this.scribe.printNextToken(TerminalTokens.TokenNameLPAREN, this.preferences.insert_space_before_opening_paren_in_while);

		if (this.preferences.insert_space_after_opening_paren_in_while) {
			this.scribe.space();
		}
		withStatement.condition.traverse(this, scope);

		this.scribe.printNextToken(TerminalTokens.TokenNameRPAREN, this.preferences.insert_space_before_closing_paren_in_while);

		final Statement action = withStatement.action;
		if (action != null) {
			if (action instanceof Block) {
                formatLeftCurlyBrace(line, this.preferences.brace_position_for_block);
				action.traverse(this, scope);
			} else if (action instanceof EmptyStatement) {
				/*
				 * This is an empty statement
				 */
				formatNecessaryEmptyStatement();
			} else {
				this.scribe.printNewLine();
				this.scribe.indent();
				action.traverse(this, scope);
				if (action instanceof Expression|| action instanceof LocalDeclaration) {
					this.scribe.printOptionalNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);
					this.scribe.printTrailingComment();
				}
				this.scribe.unIndent();
			}
		} else {
			/*
			 * This is an empty statement
			 */
			formatNecessaryEmptyStatement();
		}
		return false;
	}

	public boolean visit(ListExpression listExpression, BlockScope scope) {
		int expressionsLength=listExpression.expressions.length;
		Alignment argumentsAlignment = this.scribe.createAlignment(
				"messageArguments", //$NON-NLS-1$
				this.preferences.alignment_for_arguments_in_method_invocation,
				expressionsLength,
				this.scribe.scanner.currentPosition);
		this.scribe.enterAlignment(argumentsAlignment);
		boolean ok = false;
		do {
			try {
				for (int i = 0; i < expressionsLength; i++) {
					if (i > 0) {
						this.scribe.printNextToken(TerminalTokens.TokenNameCOMMA, this.preferences.insert_space_before_comma_in_method_invocation_arguments);
						this.scribe.printTrailingComment();
					}
					this.scribe.alignFragment(argumentsAlignment, i);
					if (i > 0 && this.preferences.insert_space_after_comma_in_method_invocation_arguments) {
						this.scribe.space();
					}
					listExpression.expressions[i].traverse(this, scope);
				}
				ok = true;
			} catch (AlignmentException e) {
				this.scribe.redoAlignment(e);
			}
		} while (!ok);
		this.scribe.exitAlignment(argumentsAlignment, true);
		return false;
	}
}
