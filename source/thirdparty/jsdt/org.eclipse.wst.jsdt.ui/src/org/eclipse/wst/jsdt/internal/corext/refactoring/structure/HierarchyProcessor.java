/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IInitializer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IExtendedModifier;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.wst.jsdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsModel;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsSolver;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.constraints.SuperTypeRefactoringProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Partial implementation of a hierarchy refactoring processor used in pull up,
 * push down and extract supertype refactorings.
 * <p>
 * This processor provides common functionality to move members in a type
 * hierarchy, and to perform a "Use Supertype" refactoring afterwards.
 * </p>
 * 
 * 
 */
public abstract class HierarchyProcessor extends SuperTypeRefactoringProcessor {

	/**
	 * AST node visitor which performs the actual mapping.
	 */
	public static class TypeVariableMapper extends ASTVisitor {

		/** The type variable mapping to use */
		protected final TypeVariableMaplet[] fMapping;

		/** The AST rewrite to use */
		protected final ASTRewrite fRewrite;

		/**
		 * Creates a new type variable mapper.
		 * 
		 * @param rewrite
		 *            The AST rewrite to use
		 * @param mapping
		 *            The type variable mapping to use
		 */
		public TypeVariableMapper(final ASTRewrite rewrite, final TypeVariableMaplet[] mapping) {
			Assert.isNotNull(rewrite);
			Assert.isNotNull(mapping);
			fRewrite= rewrite;
			fMapping= mapping;
		}

		public final boolean visit(final SimpleName node) {
			final ITypeBinding binding= node.resolveTypeBinding();
			return true;
		}
	}

	protected static boolean areAllFragmentsDeleted(final FieldDeclaration declaration, final List declarationNodes) {
		for (final Iterator iterator= declaration.fragments().iterator(); iterator.hasNext();) {
			if (!declarationNodes.contains(iterator.next()))
				return false;
		}
		return true;
	}

	protected static RefactoringStatus checkProjectCompliance(CompilationUnitRewrite sourceRewriter, IType destination, IMember[] members) {
		RefactoringStatus status= new RefactoringStatus();
		if (!JavaModelUtil.is50OrHigher(destination.getJavaScriptProject())) {
			for (int index= 0; index < members.length; index++) {
				if (members[index] instanceof IFunction) {
					final IFunction method= (IFunction) members[index];
					try {
						if (Flags.isVarargs(method.getFlags()))
							status.merge(RefactoringStatus.createErrorStatus(Messages.format(RefactoringCoreMessages.PullUpRefactoring_incompatible_language_constructs1, new String[] { JavaScriptElementLabels.getTextLabel(members[index], JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(destination, JavaScriptElementLabels.ALL_DEFAULT)}), JavaStatusContext.create(members[index])));
					} catch (JavaScriptModelException exception) {
						JavaScriptPlugin.log(exception);
					}
				}
			}
		}
		return status;
	}

	protected static void copyAnnotations(final FieldDeclaration oldField, final FieldDeclaration newField) {
		final AST ast= newField.getAST();
		for (int index= 0, n= oldField.modifiers().size(); index < n; index++) {
			final IExtendedModifier modifier= (IExtendedModifier) oldField.modifiers().get(index);
			final List modifiers= newField.modifiers();
		}
	}

	protected static void copyAnnotations(final FunctionDeclaration oldMethod, final FunctionDeclaration newMethod) {
		final AST ast= newMethod.getAST();
		for (int index= 0, n= oldMethod.modifiers().size(); index < n; index++) {
			final IExtendedModifier modifier= (IExtendedModifier) oldMethod.modifiers().get(index);
			final List modifiers= newMethod.modifiers();
		}
	}

	protected static void copyJavadocNode(final ASTRewrite rewrite, final IMember member, final BodyDeclaration oldDeclaration, final BodyDeclaration newDeclaration) throws JavaScriptModelException {
		final JSdoc predecessor= oldDeclaration.getJavadoc();
		if (predecessor != null) {
			final IDocument buffer= new Document(member.getJavaScriptUnit().getBuffer().getContents());
			try {
				final String[] lines= Strings.convertIntoLines(buffer.get(predecessor.getStartPosition(), predecessor.getLength()));
				Strings.trimIndentation(lines, member.getJavaScriptProject(), false);
				final JSdoc successor= (JSdoc) rewrite.createStringPlaceholder(Strings.concatenate(lines, TextUtilities.getDefaultLineDelimiter(buffer)), ASTNode.JSDOC);
				newDeclaration.setJavadoc(successor);
			} catch (BadLocationException exception) {
				JavaScriptPlugin.log(exception);
			}
		}
	}

	protected static void copyThrownExceptions(final FunctionDeclaration oldMethod, final FunctionDeclaration newMethod) {
		final AST ast= newMethod.getAST();
		for (int index= 0, n= oldMethod.thrownExceptions().size(); index < n; index++)
			newMethod.thrownExceptions().add(index, ASTNode.copySubtree(ast, (Name) oldMethod.thrownExceptions().get(index)));
	}

	protected static String createLabel(final IMember member) {
		if (member instanceof IType)
			return JavaScriptElementLabels.getTextLabel(member, JavaScriptElementLabels.ALL_FULLY_QUALIFIED);
		else if (member instanceof IFunction)
			return JavaScriptElementLabels.getTextLabel(member, JavaScriptElementLabels.ALL_FULLY_QUALIFIED);
		else if (member instanceof IField)
			return JavaScriptElementLabels.getTextLabel(member, JavaScriptElementLabels.ALL_FULLY_QUALIFIED);
		else if (member instanceof IInitializer)
			return RefactoringCoreMessages.HierarchyRefactoring_initializer;
		Assert.isTrue(false);
		return null;
	}

	protected static FieldDeclaration createNewFieldDeclarationNode(final ASTRewrite rewrite, final JavaScriptUnit unit, final IField field, final VariableDeclarationFragment oldFieldFragment, final TypeVariableMaplet[] mapping, final IProgressMonitor monitor, final RefactoringStatus status, final int modifiers) throws JavaScriptModelException {
		final VariableDeclarationFragment newFragment= rewrite.getAST().newVariableDeclarationFragment();
		newFragment.setExtraDimensions(oldFieldFragment.getExtraDimensions());
		if (oldFieldFragment.getInitializer() != null) {
			Expression newInitializer= null;
			if (mapping.length > 0)
				newInitializer= createPlaceholderForExpression(oldFieldFragment.getInitializer(), field.getJavaScriptUnit(), mapping, rewrite);
			else
				newInitializer= createPlaceholderForExpression(oldFieldFragment.getInitializer(), field.getJavaScriptUnit(), rewrite);
			newFragment.setInitializer(newInitializer);
		}
		newFragment.setName(((SimpleName) ASTNode.copySubtree(rewrite.getAST(), oldFieldFragment.getName())));
		final FieldDeclaration newField= rewrite.getAST().newFieldDeclaration(newFragment);
		final FieldDeclaration oldField= ASTNodeSearchUtil.getFieldDeclarationNode(field, unit);
		copyJavadocNode(rewrite, field, oldField, newField);
		copyAnnotations(oldField, newField);
		newField.modifiers().addAll(ASTNodeFactory.newModifiers(rewrite.getAST(), modifiers));
		final Type oldType= oldField.getType();
		Type newType= null;
		if (mapping.length > 0) {
			newType= createPlaceholderForType(oldType, field.getJavaScriptUnit(), mapping, rewrite);
		} else
			newType= createPlaceholderForType(oldType, field.getJavaScriptUnit(), rewrite);
		newField.setType(newType);
		return newField;
	}

	protected static Expression createPlaceholderForExpression(final Expression expression, final IJavaScriptUnit declaringCu, final ASTRewrite rewrite) throws JavaScriptModelException {
		return (Expression) rewrite.createStringPlaceholder(declaringCu.getBuffer().getText(expression.getStartPosition(), expression.getLength()), ASTNode.FUNCTION_INVOCATION);
	}

	protected static Expression createPlaceholderForExpression(final Expression expression, final IJavaScriptUnit declaringCu, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite) throws JavaScriptModelException {
		Expression result= null;
		try {
			final IDocument document= new Document(declaringCu.getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(expression.getAST());
			final ITrackedNodePosition position= rewriter.track(expression);
			expression.accept(new TypeVariableMapper(rewriter, mapping));
			rewriter.rewriteAST(document, declaringCu.getJavaScriptProject().getOptions(true)).apply(document, TextEdit.NONE);
			result= (Expression) rewrite.createStringPlaceholder(document.get(position.getStartPosition(), position.getLength()), ASTNode.FUNCTION_INVOCATION);
		} catch (MalformedTreeException exception) {
			JavaScriptPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaScriptPlugin.log(exception);
		}
		return result;
	}

	protected static BodyDeclaration createPlaceholderForProtectedTypeDeclaration(final BodyDeclaration bodyDeclaration, final JavaScriptUnit declaringCuNode, final IJavaScriptUnit declaringCu, final ASTRewrite rewrite, final boolean removeIndentation) throws JavaScriptModelException {
		String text= null;
		try {
			final ASTRewrite rewriter= ASTRewrite.create(bodyDeclaration.getAST());
			ModifierRewrite.create(rewriter, bodyDeclaration).setVisibility(Modifier.PROTECTED, null);
			final ITrackedNodePosition position= rewriter.track(bodyDeclaration);
			final IDocument document= new Document(declaringCu.getBuffer().getText(declaringCuNode.getStartPosition(), declaringCuNode.getLength()));
			rewriter.rewriteAST(document, declaringCu.getJavaScriptProject().getOptions(true)).apply(document, TextEdit.UPDATE_REGIONS);
			text= document.get(position.getStartPosition(), position.getLength());
		} catch (BadLocationException exception) {
			text= getNewText(bodyDeclaration, declaringCu, removeIndentation);
		}
		return (BodyDeclaration) rewrite.createStringPlaceholder(text, ASTNode.TYPE_DECLARATION);
	}

	protected static BodyDeclaration createPlaceholderForProtectedTypeDeclaration(final BodyDeclaration bodyDeclaration, final JavaScriptUnit declaringCuNode, final IJavaScriptUnit declaringCu, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite, final boolean removeIndentation) throws JavaScriptModelException {
		BodyDeclaration result= null;
		try {
			final IDocument document= new Document(declaringCu.getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(bodyDeclaration.getAST());
			final ITrackedNodePosition position= rewriter.track(bodyDeclaration);
			bodyDeclaration.accept(new TypeVariableMapper(rewriter, mapping) {


				public final boolean visit(final TypeDeclaration node) {
					ModifierRewrite.create(fRewrite, bodyDeclaration).setVisibility(Modifier.PROTECTED, null);
					return true;
				}
			});
			rewriter.rewriteAST(document, declaringCu.getJavaScriptProject().getOptions(true)).apply(document, TextEdit.NONE);
			result= (BodyDeclaration) rewrite.createStringPlaceholder(document.get(position.getStartPosition(), position.getLength()), ASTNode.TYPE_DECLARATION);
		} catch (MalformedTreeException exception) {
			JavaScriptPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaScriptPlugin.log(exception);
		}
		return result;
	}

	protected static SingleVariableDeclaration createPlaceholderForSingleVariableDeclaration(final SingleVariableDeclaration declaration, final IJavaScriptUnit declaringCu, final ASTRewrite rewrite) throws JavaScriptModelException {
		return (SingleVariableDeclaration) rewrite.createStringPlaceholder(declaringCu.getBuffer().getText(declaration.getStartPosition(), declaration.getLength()), ASTNode.SINGLE_VARIABLE_DECLARATION);
	}

	protected static SingleVariableDeclaration createPlaceholderForSingleVariableDeclaration(final SingleVariableDeclaration declaration, final IJavaScriptUnit declaringCu, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite) throws JavaScriptModelException {
		SingleVariableDeclaration result= null;
		try {
			final IDocument document= new Document(declaringCu.getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(declaration.getAST());
			final ITrackedNodePosition position= rewriter.track(declaration);
			declaration.accept(new TypeVariableMapper(rewriter, mapping));
			rewriter.rewriteAST(document, declaringCu.getJavaScriptProject().getOptions(true)).apply(document, TextEdit.NONE);
			result= (SingleVariableDeclaration) rewrite.createStringPlaceholder(document.get(position.getStartPosition(), position.getLength()), ASTNode.SINGLE_VARIABLE_DECLARATION);
		} catch (MalformedTreeException exception) {
			JavaScriptPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaScriptPlugin.log(exception);
		}
		return result;
	}

	protected static Type createPlaceholderForType(final Type type, final IJavaScriptUnit declaringCu, final ASTRewrite rewrite) throws JavaScriptModelException {
		return (Type) rewrite.createStringPlaceholder(declaringCu.getBuffer().getText(type.getStartPosition(), type.getLength()), ASTNode.SIMPLE_TYPE);
	}

	protected static Type createPlaceholderForType(final Type type, final IJavaScriptUnit declaringCu, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite) throws JavaScriptModelException {
		Type result= null;
		try {
			final IDocument document= new Document(declaringCu.getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(type.getAST());
			final ITrackedNodePosition position= rewriter.track(type);
			type.accept(new TypeVariableMapper(rewriter, mapping));
			rewriter.rewriteAST(document, declaringCu.getJavaScriptProject().getOptions(true)).apply(document, TextEdit.NONE);
			result= (Type) rewrite.createStringPlaceholder(document.get(position.getStartPosition(), position.getLength()), ASTNode.SIMPLE_TYPE);
		} catch (MalformedTreeException exception) {
			JavaScriptPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaScriptPlugin.log(exception);
		}
		return result;
	}

	protected static BodyDeclaration createPlaceholderForTypeDeclaration(final BodyDeclaration bodyDeclaration, final IJavaScriptUnit declaringCu, final ASTRewrite rewrite, final boolean removeIndentation) throws JavaScriptModelException {
		return (BodyDeclaration) rewrite.createStringPlaceholder(getNewText(bodyDeclaration, declaringCu, removeIndentation), ASTNode.TYPE_DECLARATION);
	}

	protected static BodyDeclaration createPlaceholderForTypeDeclaration(final BodyDeclaration bodyDeclaration, final IJavaScriptUnit declaringCu, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite, final boolean removeIndentation) throws JavaScriptModelException {
		BodyDeclaration result= null;
		try {
			final IDocument document= new Document(declaringCu.getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(bodyDeclaration.getAST());
			final ITrackedNodePosition position= rewriter.track(bodyDeclaration);
			bodyDeclaration.accept(new TypeVariableMapper(rewriter, mapping));
			rewriter.rewriteAST(document, declaringCu.getJavaScriptProject().getOptions(true)).apply(document, TextEdit.NONE);
			result= (BodyDeclaration) rewrite.createStringPlaceholder(document.get(position.getStartPosition(), position.getLength()), ASTNode.TYPE_DECLARATION);
		} catch (MalformedTreeException exception) {
			JavaScriptPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaScriptPlugin.log(exception);
		}
		return result;
	}

	protected static void deleteDeclarationNodes(final CompilationUnitRewrite sourceRewriter, final boolean sameCu, final CompilationUnitRewrite unitRewriter, final List members, final GroupCategorySet set) throws JavaScriptModelException {
		final List declarationNodes= getDeclarationNodes(unitRewriter.getRoot(), members);
		for (final Iterator iterator= declarationNodes.iterator(); iterator.hasNext();) {
			final ASTNode node= (ASTNode) iterator.next();
			final ASTRewrite rewriter= unitRewriter.getASTRewrite();
			final ImportRemover remover= unitRewriter.getImportRemover();
			if (node instanceof VariableDeclarationFragment) {
				if (node.getParent() instanceof FieldDeclaration) {
					final FieldDeclaration declaration= (FieldDeclaration) node.getParent();
					if (areAllFragmentsDeleted(declaration, declarationNodes)) {
						rewriter.remove(declaration, unitRewriter.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_remove_member, set));
						if (!sameCu)
							remover.registerRemovedNode(declaration);
					} else {
						rewriter.remove(node, unitRewriter.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_remove_member, set));
						if (!sameCu)
							remover.registerRemovedNode(node);
					}
				}
			} else {
				rewriter.remove(node, unitRewriter.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_remove_member, set));
				if (!sameCu)
					remover.registerRemovedNode(node);
			}
		}
	}

	protected static List getDeclarationNodes(final JavaScriptUnit cuNode, final List members) throws JavaScriptModelException {
		final List result= new ArrayList(members.size());
		for (final Iterator iterator= members.iterator(); iterator.hasNext();) {
			final IMember member= (IMember) iterator.next();
			ASTNode node= null;
			if (member instanceof IField) {
					node= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) member, cuNode);
			} else if (member instanceof IType)
				node= ASTNodeSearchUtil.getAbstractTypeDeclarationNode((IType) member, cuNode);
			else if (member instanceof IFunction)
				node= ASTNodeSearchUtil.getMethodDeclarationNode((IFunction) member, cuNode);
			if (node != null)
				result.add(node);
		}
		return result;
	}

	protected static String getNewText(final ASTNode node, final IJavaScriptUnit declaringCu, final boolean removeIndentation) throws JavaScriptModelException {
		final String result= declaringCu.getBuffer().getText(node.getStartPosition(), node.getLength());
		if (removeIndentation)
			return getUnindentedText(result, declaringCu);

		return result;
	}

	protected static String getUnindentedText(final String text, final IJavaScriptUnit declaringCu) throws JavaScriptModelException {
		final String[] lines= Strings.convertIntoLines(text);
		Strings.trimIndentation(lines, declaringCu.getJavaScriptProject(), false);
		return Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(declaringCu));
	}

	/** The cached declaring type */
	protected IType fCachedDeclaringType;

	/** The cached member references */
	protected final Map fCachedMembersReferences= new HashMap(2);

	/** The cached type references */
	protected IType[] fCachedReferencedTypes;

	/** The text edit based change manager */
	protected TextEditBasedChangeManager fChangeManager;

	/** Does the refactoring use a working copy layer? */
	protected final boolean fLayer;

	/** The members to move (may be in working copies) */
	protected IMember[] fMembersToMove;

	/**
	 * Creates a new hierarchy processor.
	 * 
	 * @param members
	 *            the members, or <code>null</code> if invoked by scripting
	 * @param layer
	 *            <code>true</code> to create a working copy layer,
	 *            <code>false</code> otherwise
	 */
	protected HierarchyProcessor(final IMember[] members, final CodeGenerationSettings settings, boolean layer) {
		super(settings);
		fLayer= layer;
		if (members != null) {
			fMembersToMove= (IMember[]) SourceReferenceUtil.sortByOffset(members);
			if (layer && fMembersToMove.length > 0) {
				final IJavaScriptUnit original= fMembersToMove[0].getJavaScriptUnit();
				if (original != null) {
					try {
						final IJavaScriptUnit copy= getSharedWorkingCopy(original.getPrimary(), new NullProgressMonitor());
						if (copy != null) {
							for (int index= 0; index < fMembersToMove.length; index++) {
								final IJavaScriptElement[] elements= copy.findElements(fMembersToMove[index]);
								if (elements != null && elements.length > 0 && elements[0] instanceof IMember) {
									fMembersToMove[index]= (IMember) elements[0];
								}
							}
						}
					} catch (JavaScriptModelException exception) {
						JavaScriptPlugin.log(exception);
					}
				}
			}
		}
	}

	protected boolean canBeAccessedFrom(final IMember member, final IType target, final ITypeHierarchy hierarchy) throws JavaScriptModelException {
		Assert.isTrue(!(member instanceof IInitializer));
		return member.exists();
	}

	protected RefactoringStatus checkConstructorCalls(final IType type, final IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 2);
			final RefactoringStatus result= new RefactoringStatus();
			final SearchResultGroup[] groups= ConstructorReferenceFinder.getConstructorReferences(type, fOwner, new SubProgressMonitor(monitor, 1), result);
			final String message= Messages.format(RefactoringCoreMessages.HierarchyRefactoring_gets_instantiated, new Object[] { JavaScriptElementLabels.getTextLabel(type, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)});

			IJavaScriptUnit unit= null;
			for (int index= 0; index < groups.length; index++) {
				unit= groups[index].getCompilationUnit();
				if (unit != null) {
					final JavaScriptUnit cuNode= RefactoringASTParser.parseWithASTProvider(unit, false, new SubProgressMonitor(monitor, 1));
					final ASTNode[] references= ASTNodeSearchUtil.getAstNodes(groups[index].getSearchResults(), cuNode);
					ASTNode node= null;
					for (int offset= 0; offset < references.length; offset++) {
						node= references[offset];
						if ((node instanceof ClassInstanceCreation) || ConstructorReferenceFinder.isImplicitConstructorReferenceNodeInClassCreations(node)) {
							final RefactoringStatusContext context= JavaStatusContext.create(unit, node);
							result.addError(message, context);
						}
					}
				}
			}
			return result;
		} finally {
			monitor.done();
		}
	}

	protected RefactoringStatus checkDeclaringType(final IProgressMonitor monitor) throws JavaScriptModelException {
		final IType type= getDeclaringType();
		if (type.isBinary())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.HierarchyRefactoring_members_of_binary);
		if (type.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.HierarchyRefactoring_members_of_read_only);
		return new RefactoringStatus();
	}

	protected RefactoringStatus checkIfMembersExist() {
		final RefactoringStatus result= new RefactoringStatus();
		IMember member= null;
		for (int index= 0; index < fMembersToMove.length; index++) {
			member= fMembersToMove[index];
			if (member == null || !member.exists())
				result.addFatalError(RefactoringCoreMessages.HierarchyRefactoring_does_not_exist);
		}
		return result;
	}

	protected void clearCaches() {
		fCachedReferencedTypes= null;
	}

	protected void copyParameters(final ASTRewrite rewrite, final IJavaScriptUnit unit, final FunctionDeclaration oldMethod, final FunctionDeclaration newMethod, final TypeVariableMaplet[] mapping) throws JavaScriptModelException {
		SingleVariableDeclaration newDeclaration= null;
		for (int index= 0, size= oldMethod.parameters().size(); index < size; index++) {
			final SingleVariableDeclaration oldDeclaration= (SingleVariableDeclaration) oldMethod.parameters().get(index);
			if (mapping.length > 0)
				newDeclaration= createPlaceholderForSingleVariableDeclaration(oldDeclaration, unit, mapping, rewrite);
			else
				newDeclaration= createPlaceholderForSingleVariableDeclaration(oldDeclaration, unit, rewrite);
			newMethod.parameters().add(index, newDeclaration);
		}
	}

	protected void copyReturnType(final ASTRewrite rewrite, final IJavaScriptUnit unit, final FunctionDeclaration oldMethod, final FunctionDeclaration newMethod, final TypeVariableMaplet[] mapping) throws JavaScriptModelException {
		Type newReturnType= null;
		if (mapping.length > 0)
			newReturnType= createPlaceholderForType(oldMethod.getReturnType2(), unit, mapping, rewrite);
		else
			newReturnType= createPlaceholderForType(oldMethod.getReturnType2(), unit, rewrite);
		newMethod.setReturnType2(newReturnType);
	}

	protected SuperTypeConstraintsSolver createContraintSolver(final SuperTypeConstraintsModel model) {
		return new SuperTypeConstraintsSolver(model);
	}

	public IType getDeclaringType() {
		if (fCachedDeclaringType != null)
			return fCachedDeclaringType;
		fCachedDeclaringType= RefactoringAvailabilityTester.getTopLevelType(fMembersToMove);
		if (fCachedDeclaringType == null)
			fCachedDeclaringType= fMembersToMove[0].getDeclaringType();
		return fCachedDeclaringType;
	}

	public IMember[] getMembersToMove() {
		return fMembersToMove;
	}

	protected IType[] getTypesReferencedInMovedMembers(final IProgressMonitor monitor) throws JavaScriptModelException {
		if (fCachedReferencedTypes == null) {
			final IType[] types= ReferenceFinderUtil.getTypesReferencedIn(fMembersToMove, fOwner, monitor);
			final List result= new ArrayList(types.length);
			final List members= Arrays.asList(fMembersToMove);
			for (int index= 0; index < types.length; index++) {
				if (!members.contains(types[index]) && !types[index].equals(getDeclaringType()))
					result.add(types[index]);
			}
			fCachedReferencedTypes= new IType[result.size()];
			result.toArray(fCachedReferencedTypes);
		}
		return fCachedReferencedTypes;
	}

	protected boolean hasNonMovedReferences(final IMember member, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaScriptModelException {
		if (!fCachedMembersReferences.containsKey(member)) {
			final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(member, IJavaScriptSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE));
			engine.setFiltering(true, true);
			engine.setStatus(status);
			engine.setOwner(fOwner);
			engine.setScope(RefactoringScopeFactory.create(member));
			engine.searchPattern(new SubProgressMonitor(monitor, 1));
			fCachedMembersReferences.put(member, engine.getResults());
		}
		final SearchResultGroup[] groups= (SearchResultGroup[]) fCachedMembersReferences.get(member);
		if (groups.length == 0)
			return false;
		else if (groups.length > 1)
			return true;
		final IJavaScriptUnit unit= groups[0].getCompilationUnit();
		if (!getDeclaringType().getJavaScriptUnit().equals(unit))
			return true;
		final SearchMatch[] matches= groups[0].getSearchResults();
		for (int index= 0; index < matches.length; index++) {
			if (!isMovedReference(matches[index]))
				return true;
		}
		return false;
	}

	protected boolean isMovedReference(final SearchMatch match) throws JavaScriptModelException {
		ISourceRange range= null;
		for (int index= 0; index < fMembersToMove.length; index++) {
			range= fMembersToMove[index].getSourceRange();
			if (range.getOffset() <= match.getOffset() && range.getOffset() + range.getLength() >= match.getOffset())
				return true;
		}
		return false;
	}

	public RefactoringParticipant[] loadParticipants(final RefactoringStatus status, final SharableParticipants sharedParticipants) throws CoreException {
		return new RefactoringParticipant[0];
	}

	protected boolean needsVisibilityAdjustment(final IMember member, final boolean references, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaScriptModelException {
		if (JdtFlags.isPublic(member))
			return false;
		if (!references)
			return true;
		return hasNonMovedReferences(member, monitor, status);
	}
}
