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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.ASTRequestor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SuperFieldAccess;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.TypeDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsSolver;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints2.ISourceConstraintVariable;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints2.ITypeConstraintVariable;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.wst.jsdt.ui.CodeGeneration;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Refactoring processor for the pull up refactoring.
 * 
 * 
 */
public class PullUpRefactoringProcessor extends HierarchyProcessor {

	/**
	 * AST node visitor which performs the actual mapping.
	 */
	private static class PullUpAstNodeMapper extends TypeVariableMapper {

		/** Are we in an anonymous class declaration? */
		private boolean fAnonymousClassDeclaration= false;

		/** The source compilation unit rewrite to use */
		private final CompilationUnitRewrite fSourceRewriter;

		/** The super reference type */
		private final IType fSuperReferenceType;

		/** The target compilation unit rewrite to use */
		private final CompilationUnitRewrite fTargetRewriter;

		/** Are we in a type declaration statement? */
		private boolean fTypeDeclarationStatement= false;

		/** The binding of the enclosing method */
		private final IFunctionBinding fEnclosingMethod;

		/**
		 * Creates a new pull up ast node mapper.
		 * 
		 * @param sourceRewriter
		 *            the source compilation unit rewrite to use
		 * @param targetRewriter
		 *            the target compilation unit rewrite to use
		 * @param rewrite
		 *            the AST rewrite to use
		 * @param type
		 *            the super reference type
		 * @param mapping
		 *            the type variable mapping
		 * @param enclosing the binding of the enclosing method
		 */
		public PullUpAstNodeMapper(final CompilationUnitRewrite sourceRewriter, final CompilationUnitRewrite targetRewriter, final ASTRewrite rewrite, final IType type, final TypeVariableMaplet[] mapping, final IFunctionBinding enclosing) {
			super(rewrite, mapping);
			Assert.isNotNull(rewrite);
			Assert.isNotNull(type);
			fSourceRewriter= sourceRewriter;
			fTargetRewriter= targetRewriter;
			fSuperReferenceType= type;
			fEnclosingMethod= enclosing;
		}

		public final void endVisit(final AnonymousClassDeclaration node) {
			fAnonymousClassDeclaration= false;
			super.endVisit(node);
		}

		public final void endVisit(final TypeDeclarationStatement node) {
			fTypeDeclarationStatement= false;
			super.endVisit(node);
		}

		public final boolean visit(final AnonymousClassDeclaration node) {
			fAnonymousClassDeclaration= true;
			return super.visit(node);
		}

		public final boolean visit(final SuperFieldAccess node) {
			if (!fAnonymousClassDeclaration && !fTypeDeclarationStatement) {
				final AST ast= node.getAST();
				final FieldAccess access= ast.newFieldAccess();
				access.setExpression(ast.newThisExpression());
				access.setName(ast.newSimpleName(node.getName().getIdentifier()));
				fRewrite.replace(node, access, null);
				if (!fSourceRewriter.getCu().equals(fTargetRewriter.getCu()))
					fSourceRewriter.getImportRemover().registerRemovedNode(node);
				return true;
			}
			return false;
		}

		public final boolean visit(final SuperMethodInvocation node) {
			if (!fAnonymousClassDeclaration && !fTypeDeclarationStatement) {
				final IBinding superBinding= node.getName().resolveBinding();
				if (superBinding instanceof IFunctionBinding) {
					final IFunctionBinding extended= (IFunctionBinding) superBinding;
					if (fEnclosingMethod != null && fEnclosingMethod.overrides(extended))
						return true;
					final ITypeBinding declaringBinding= extended.getDeclaringClass();
					if (declaringBinding != null) {
						final IType type= (IType) declaringBinding.getJavaElement();
						if (!fSuperReferenceType.equals(type))
							return true;
					}
				}
				final AST ast= node.getAST();
				final ThisExpression expression= ast.newThisExpression();
				final FunctionInvocation invocation= ast.newFunctionInvocation();
				final SimpleName simple= ast.newSimpleName(node.getName().getIdentifier());
				invocation.setName(simple);
				invocation.setExpression(expression);
				final List arguments= (List) node.getStructuralProperty(SuperMethodInvocation.ARGUMENTS_PROPERTY);
				if (arguments != null && arguments.size() > 0) {
					final ListRewrite rewriter= fRewrite.getListRewrite(invocation, FunctionInvocation.ARGUMENTS_PROPERTY);
					rewriter.insertLast(rewriter.createCopyTarget((ASTNode) arguments.get(0), (ASTNode) arguments.get(arguments.size() - 1)), null);
				}
				fRewrite.replace(node, invocation, null);
				if (!fSourceRewriter.getCu().equals(fTargetRewriter.getCu()))
					fSourceRewriter.getImportRemover().registerRemovedNode(node);
				return true;
			}
			return false;
		}

		public final boolean visit(final TypeDeclarationStatement node) {
			fTypeDeclarationStatement= true;
			return super.visit(node);
		}
	}

	protected static final String ATTRIBUTE_ABSTRACT= "abstract"; //$NON-NLS-1$

	protected static final String ATTRIBUTE_DELETE= "delete"; //$NON-NLS-1$

	protected static final String ATTRIBUTE_PULL= "pull"; //$NON-NLS-1$

	protected static final String ATTRIBUTE_STUBS= "stubs"; //$NON-NLS-1$

	private static final String IDENTIFIER= "org.eclipse.wst.jsdt.ui.pullUpProcessor"; //$NON-NLS-1$

	/** The pull up group category set */
	private static final GroupCategorySet SET_PULL_UP= new GroupCategorySet(new GroupCategory("org.eclipse.wst.jsdt.internal.corext.pullUp", //$NON-NLS-1$
			RefactoringCoreMessages.PullUpRefactoring_category_name, RefactoringCoreMessages.PullUpRefactoring_category_description));

	private static void addMatchingMember(final Map mapping, final IMember key, final IMember matchingMember) {
		Set matchingSet;
		if (mapping.containsKey(key)) {
			matchingSet= (Set) mapping.get(key);
		} else {
			matchingSet= new HashSet();
			mapping.put(key, matchingSet);
		}
		Assert.isTrue(!matchingSet.contains(matchingMember));
		matchingSet.add(matchingMember);
	}

	private static Block createMethodStub(final FunctionDeclaration method, final AST ast) {
		final Block body= ast.newBlock();
		final Expression expression= ASTNodeFactory.newDefaultExpression(ast, method.getReturnType2(), method.getExtraDimensions());
		if (expression != null) {
			final ReturnStatement returnStatement= ast.newReturnStatement();
			returnStatement.setExpression(expression);
			body.statements().add(returnStatement);
		}
		return body;
	}

	private static Set getEffectedSubTypes(final ITypeHierarchy hierarchy, final IType type) throws JavaScriptModelException {
		IType[] types= null;
		types= hierarchy.getSubclasses(type);
		final Set result= new HashSet();
		for (int index= 0; index < types.length; index++) {
			if (JdtFlags.isAbstract(types[index]))
				result.addAll(getEffectedSubTypes(hierarchy, types[index]));
			else
				result.add(types[index]);
		}
		return result;
	}

	private static IMember[] getMembers(final IMember[] members, final int type) {
		final List list= Arrays.asList(JavaElementUtil.getElementsOfType(members, type));
		return (IMember[]) list.toArray(new IMember[list.size()]);
	}

	private static void mergeMaps(final Map result, final Map map) {
		for (final Iterator iter= result.keySet().iterator(); iter.hasNext();) {
			final IMember key= (IMember) iter.next();
			if (map.containsKey(key)) {
				final Set resultSet= (Set) result.get(key);
				final Set mapSet= (Set) map.get(key);
				resultSet.addAll(mapSet);
			}
		}
	}

	private static void upgradeMap(final Map result, final Map map) {
		for (final Iterator iter= map.keySet().iterator(); iter.hasNext();) {
			final IMember key= (IMember) iter.next();
			if (!result.containsKey(key)) {
				final Set mapSet= (Set) map.get(key);
				final Set resultSet= new HashSet(mapSet);
				result.put(key, resultSet);
			}
		}
	}

	/** The methods to be declared abstract */
	protected IFunction[] fAbstractMethods= new IFunction[0];

	/** The cached supertype hierarchy of the declaring type */
	private ITypeHierarchy fCachedDeclaringSuperTypeHierarchy;

	/** The cached type hierarchy of the destination type */
	private ITypeHierarchy fCachedDestinationTypeHierarchy;

	/** The cached set of skipped supertypes */
	private Set fCachedSkippedSuperTypes;

	/** The map of compilation units to compilation unit rewrites */
	protected Map fCompilationUnitRewrites;

	/** Should method stubs be generated in subtypes? */
	protected boolean fCreateMethodStubs= true;

	/** The methods to be deleted in subtypes */
	protected IFunction[] fDeletedMethods= new IFunction[0];

	/** The destination type */
	protected IType fDestinationType;

	/**
	 * Creates a new pull up refactoring processor.
	 * 
	 * @param members
	 *            the members to pull up, or <code>null</code> if invoked by
	 *            scripting
	 * @param settings
	 *            the code generation settings, or <code>null</code> if
	 *            invoked by scripting
	 */
	public PullUpRefactoringProcessor(final IMember[] members, final CodeGenerationSettings settings) {
		this(members, settings, false);
	}

	/**
	 * Creates a new pull up refactoring processor.
	 * 
	 * @param members
	 *            the members to pull up, or <code>null</code> if invoked by
	 *            scripting
	 * @param settings
	 *            the code generation settings, or <code>null</code> if
	 *            invoked by scripting
	 * @param layer
	 *            <code>true</code> to create a working copy layer,
	 *            <code>false</code> otherwise
	 */
	protected PullUpRefactoringProcessor(final IMember[] members, final CodeGenerationSettings settings, final boolean layer) {
		super(members, settings, layer);
		if (members != null) {
			final IType type= RefactoringAvailabilityTester.getTopLevelType(fMembersToMove);
			try {
				if (type != null && RefactoringAvailabilityTester.getPullUpMembers(type).length != 0) {
					fCachedDeclaringType= RefactoringAvailabilityTester.getTopLevelType(fMembersToMove);
					fMembersToMove= new IMember[0];
				}
			} catch (JavaScriptModelException exception) {
				JavaScriptPlugin.log(exception);
			}
		}
	}

	private void addAllRequiredPullableMembers(final List queue, final IMember member, final IProgressMonitor monitor) throws JavaScriptModelException {
		Assert.isNotNull(queue);
		Assert.isNotNull(member);
		Assert.isNotNull(monitor);
		SubProgressMonitor sub= null;
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_calculating_required, 3);
			final IFunction[] requiredMethods= ReferenceFinderUtil.getMethodsReferencedIn(new IJavaScriptElement[] { member}, fOwner, new SubProgressMonitor(monitor, 1));
			sub= new SubProgressMonitor(monitor, 1);
			boolean isStatic= false;
			try {
				sub.beginTask(RefactoringCoreMessages.PullUpRefactoring_calculating_required, requiredMethods.length);
				isStatic= JdtFlags.isStatic(member);
				for (int index= 0; index < requiredMethods.length; index++) {
					final IFunction requiredMethod= requiredMethods[index];
					if (isStatic && !JdtFlags.isStatic(requiredMethod))
						continue;
					if (isRequiredPullableMember(queue, requiredMethod) && !(MethodChecks.isVirtual(requiredMethod) && isAvailableInDestination(requiredMethod, new SubProgressMonitor(sub, 1))))
						queue.add(requiredMethod);
				}
			} finally {
				sub.done();
			}
			final IField[] requiredFields= ReferenceFinderUtil.getFieldsReferencedIn(new IJavaScriptElement[] { member}, fOwner, new SubProgressMonitor(monitor, 1));
			sub= new SubProgressMonitor(monitor, 1);
			try {
				sub.beginTask(RefactoringCoreMessages.PullUpRefactoring_calculating_required, requiredFields.length);
				isStatic= JdtFlags.isStatic(member);
				for (int index= 0; index < requiredFields.length; index++) {
					final IField requiredField= requiredFields[index];
					if (isStatic && !JdtFlags.isStatic(requiredField))
						continue;
					if (isRequiredPullableMember(queue, requiredField))
						queue.add(requiredField);
				}
			} finally {
				sub.done();
			}
			final IType[] requiredTypes= ReferenceFinderUtil.getTypesReferencedIn(new IJavaScriptElement[] { member}, fOwner, new SubProgressMonitor(monitor, 1));
			sub= new SubProgressMonitor(monitor, 1);
			try {
				sub.beginTask(RefactoringCoreMessages.PullUpRefactoring_calculating_required, requiredMethods.length);
				isStatic= JdtFlags.isStatic(member);
				for (int index= 0; index < requiredTypes.length; index++) {
					final IType requiredType= requiredTypes[index];
					if (isStatic && !JdtFlags.isStatic(requiredType))
						continue;
					if (isRequiredPullableMember(queue, requiredType))
						queue.add(requiredType);
				}
			} finally {
				sub.done();
			}
		} finally {
			monitor.done();
		}
	}

	private void addMethodStubForAbstractMethod(final IFunction sourceMethod, final JavaScriptUnit declaringCuNode, final AbstractTypeDeclaration typeToCreateStubIn, final IJavaScriptUnit newCu, final CompilationUnitRewrite rewriter, final Map adjustments, final IProgressMonitor monitor, final RefactoringStatus status) throws CoreException {
		final FunctionDeclaration methodToCreateStubFor= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		final AST ast= rewriter.getRoot().getAST();
		final FunctionDeclaration newMethod= ast.newFunctionDeclaration();
		newMethod.setBody(createMethodStub(methodToCreateStubFor, ast));
		newMethod.setConstructor(false);
		newMethod.setExtraDimensions(methodToCreateStubFor.getExtraDimensions());
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, getModifiersWithUpdatedVisibility(sourceMethod, JdtFlags.clearFlag(Modifier.NATIVE | Modifier.ABSTRACT, methodToCreateStubFor.getModifiers()), adjustments, new SubProgressMonitor(monitor, 1), false, status)));
		newMethod.setName(((SimpleName) ASTNode.copySubtree(ast, methodToCreateStubFor.getName())));
		copyReturnType(rewriter.getASTRewrite(), getDeclaringType().getJavaScriptUnit(), methodToCreateStubFor, newMethod, null);
		copyParameters(rewriter.getASTRewrite(), getDeclaringType().getJavaScriptUnit(), methodToCreateStubFor, newMethod, null);
		copyThrownExceptions(methodToCreateStubFor, newMethod);
		newMethod.setJavadoc(createJavadocForStub(typeToCreateStubIn.getName().getIdentifier(), methodToCreateStubFor, newMethod, newCu, rewriter.getASTRewrite()));
		ImportRewriteUtil.addImports(rewriter, newMethod, new HashMap(), new HashMap(), false);
		rewriter.getASTRewrite().getListRewrite(typeToCreateStubIn, typeToCreateStubIn.getBodyDeclarationsProperty()).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, typeToCreateStubIn.bodyDeclarations()), rewriter.createCategorizedGroupDescription(RefactoringCoreMessages.PullUpRefactoring_add_method_stub, SET_PULL_UP));
	}

	private void addNecessaryMethodStubs(final List effected, final JavaScriptUnit root, final CompilationUnitRewrite unitRewriter, final Map adjustments, final IProgressMonitor monitor, final RefactoringStatus status) throws CoreException {
		final IType declaringType= getDeclaringType();
		final IFunction[] methods= getAbstractMethods();
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, effected.size());
			for (final Iterator iter= effected.iterator(); iter.hasNext();) {
				final IType type= (IType) iter.next();
				if (type.equals(declaringType))
					continue;
				final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(type, unitRewriter.getRoot());
				final IJavaScriptUnit unit= type.getJavaScriptUnit();
				final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 1);
				try {
					subMonitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, methods.length);
					for (int j= 0; j < methods.length; j++) {
						final IFunction method= methods[j];
						if (null == JavaModelUtil.findMethod(method.getElementName(), method.getParameterTypes(), method.isConstructor(), type)) {
							addMethodStubForAbstractMethod(method, root, declaration, unit, unitRewriter, adjustments, new SubProgressMonitor(subMonitor, 1), status);
						}
					}
					subMonitor.done();
				} finally {
					subMonitor.done();
				}
			}
		} finally {
			monitor.done();
		}
	}

	protected boolean canBeAccessedFrom(final IMember member, final IType target, final ITypeHierarchy hierarchy) throws JavaScriptModelException {
		if (super.canBeAccessedFrom(member, target, hierarchy)) {
			if (target.equals(member.getDeclaringType()))
				return true;
			if (target.equals(member))
				return true;
			if (member instanceof IFunction) {
				final IFunction method= (IFunction) member;
				final IFunction stub= target.getFunction(method.getElementName(), method.getParameterTypes());
				if (stub.exists())
					return true;
			}
			if (member.getDeclaringType() == null) {
				if (!(member instanceof IType))
					return false;
				if (JdtFlags.isPublic(member))
					return true;
				if (!JdtFlags.isPackageVisible(member))
					return false;
				if (JavaModelUtil.isSamePackage(((IType) member).getPackageFragment(), target.getPackageFragment()))
					return true;
				final IType type= member.getDeclaringType();
				if (type != null)
					return hierarchy.contains(type);
				return false;
			}
			final IType declaringType= member.getDeclaringType();
			if (!canBeAccessedFrom(declaringType, target, hierarchy))
				return false;
			if (declaringType.equals(getDeclaringType()))
				return false;
			return true;
		}
		return false;
	}

	private RefactoringStatus checkAccessedFields(final IProgressMonitor monitor, final ITypeHierarchy hierarchy) throws JavaScriptModelException {
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking_referenced_elements, 2);
		final RefactoringStatus result= new RefactoringStatus();

		final List pulledUpList= Arrays.asList(fMembersToMove);
		final List deletedList= Arrays.asList(getMembersToDelete(new SubProgressMonitor(monitor, 1)));
		final IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(fMembersToMove, fOwner, new SubProgressMonitor(monitor, 1));

		final IType destination= getDestinationType();
		for (int i= 0; i < accessedFields.length; i++) {
			final IField field= accessedFields[i];
			if (!field.exists())
				continue;

			boolean isAccessible= pulledUpList.contains(field) || deletedList.contains(field) || canBeAccessedFrom(field, destination, hierarchy);
			if (!isAccessible) {
				final String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_field_not_accessible, new String[] { JavaScriptElementLabels.getTextLabel(field, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(destination, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)});
				result.addError(message, JavaStatusContext.create(field));
			} else if (getSkippedSuperTypes(new SubProgressMonitor(monitor, 1)).contains(field.getDeclaringType())) {
				final String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_field_cannot_be_accessed, new String[] { JavaScriptElementLabels.getTextLabel(field, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(destination, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)});
				result.addError(message, JavaStatusContext.create(field));
			}
		}
		monitor.done();
		return result;
	}

	private RefactoringStatus checkAccessedMethods(final IProgressMonitor monitor, final ITypeHierarchy hierarchy) throws JavaScriptModelException {
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking_referenced_elements, 2);
		final RefactoringStatus result= new RefactoringStatus();

		final List pulledUpList= Arrays.asList(fMembersToMove);
		final List declaredAbstractList= Arrays.asList(fAbstractMethods);
		final List deletedList= Arrays.asList(getMembersToDelete(new SubProgressMonitor(monitor, 1)));
		final IFunction[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(fMembersToMove, fOwner, new SubProgressMonitor(monitor, 1));

		final IType destination= getDestinationType();
		for (int index= 0; index < accessedMethods.length; index++) {
			final IFunction method= accessedMethods[index];
			if (!method.exists())
				continue;
			boolean isAccessible= pulledUpList.contains(method) || deletedList.contains(method) || declaredAbstractList.contains(method) || canBeAccessedFrom(method, destination, hierarchy);
			if (!isAccessible) {
				final String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_method_not_accessible, new String[] { JavaScriptElementLabels.getTextLabel(method, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(destination, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)});
				result.addError(message, JavaStatusContext.create(method));
			} else if (getSkippedSuperTypes(new SubProgressMonitor(monitor, 1)).contains(method.getDeclaringType())) {
				final String[] keys= { JavaScriptElementLabels.getTextLabel(method, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(destination, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)};
				final String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_method_cannot_be_accessed, keys);
				result.addError(message, JavaStatusContext.create(method));
			}
		}
		monitor.done();
		return result;
	}

	private RefactoringStatus checkAccessedTypes(final IProgressMonitor monitor, final ITypeHierarchy hierarchy) throws JavaScriptModelException {
		final RefactoringStatus result= new RefactoringStatus();
		final IType[] accessedTypes= getTypesReferencedInMovedMembers(monitor);
		final IType destination= getDestinationType();
		final List pulledUpList= Arrays.asList(fMembersToMove);
		for (int index= 0; index < accessedTypes.length; index++) {
			final IType type= accessedTypes[index];
			if (!type.exists())
				continue;

			if (!canBeAccessedFrom(type, destination, hierarchy) && !pulledUpList.contains(type)) {
				final String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_type_not_accessible, new String[] { JavaScriptElementLabels.getTextLabel(type, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(destination, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)});
				result.addError(message, JavaStatusContext.create(type));
			}
		}
		monitor.done();
		return result;
	}

	private RefactoringStatus checkAccesses(final IProgressMonitor monitor) throws JavaScriptModelException {
		final RefactoringStatus result= new RefactoringStatus();
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking_referenced_elements, 4);
			final ITypeHierarchy hierarchy= getDestinationType().newSupertypeHierarchy(fOwner, new SubProgressMonitor(monitor, 1));
			result.merge(checkAccessedTypes(new SubProgressMonitor(monitor, 1), hierarchy));
			result.merge(checkAccessedFields(new SubProgressMonitor(monitor, 1), hierarchy));
			result.merge(checkAccessedMethods(new SubProgressMonitor(monitor, 1), hierarchy));
		} finally {
			monitor.done();
		}
		return result;
	}

	private void checkAccessModifiers(final RefactoringStatus result, final Set notDeletedMembersInSubtypes) throws JavaScriptModelException {
		final List toDeclareAbstract= Arrays.asList(fAbstractMethods);
		for (final Iterator iter= notDeletedMembersInSubtypes.iterator(); iter.hasNext();) {
			final IMember member= (IMember) iter.next();
			if (member.getElementType() == IJavaScriptElement.METHOD && !toDeclareAbstract.contains(member)) {
				final IFunction method= ((IFunction) member);
				if (method.getDeclaringType().getPackageFragment().equals(fDestinationType.getPackageFragment())) {
					if (JdtFlags.isPrivate(method))
						result.addError(Messages.format(RefactoringCoreMessages.PullUpRefactoring_lower_default_visibility, new String[] { JavaScriptElementLabels.getTextLabel(method, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(method.getDeclaringType(), JavaScriptElementLabels.ALL_FULLY_QUALIFIED)}), JavaStatusContext.create(method));
				} else if (!JdtFlags.isPublic(method))
					result.addError(Messages.format(RefactoringCoreMessages.PullUpRefactoring_lower_protected_visibility, new String[] { JavaScriptElementLabels.getTextLabel(method, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(method.getDeclaringType(), JavaScriptElementLabels.ALL_FULLY_QUALIFIED)}), JavaStatusContext.create(method));
			}
		}
	}

	protected RefactoringStatus checkDeclaringSuperTypes(final IProgressMonitor monitor) throws JavaScriptModelException {
		final RefactoringStatus result= new RefactoringStatus();
		if (getCandidateTypes(result, monitor).length == 0 && !result.hasFatalError()) {
			final String msg= Messages.format(RefactoringCoreMessages.PullUpRefactoring_not_this_type, new String[] { JavaScriptElementLabels.getTextLabel(getDeclaringType(), JavaScriptElementLabels.ALL_FULLY_QUALIFIED)});
			return RefactoringStatus.createFatalErrorStatus(msg);
		}
		return result;
	}

	protected RefactoringStatus checkDeclaringType(final IProgressMonitor monitor) throws JavaScriptModelException {
		final RefactoringStatus status= super.checkDeclaringType(monitor);
		if (JavaModelUtil.getFullyQualifiedName(getDeclaringType()).equals("java.lang.Object")) //$NON-NLS-1$
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.PullUpRefactoring_no_java_lang_Object));
		status.merge(checkDeclaringSuperTypes(monitor));
		return status;
	}

	private void checkFieldTypes(final IProgressMonitor monitor, final RefactoringStatus status) throws JavaScriptModelException {
		final Map mapping= getMatchingMembers(getDestinationTypeHierarchy(monitor), getDestinationType(), true);
		for (int i= 0; i < fMembersToMove.length; i++) {
			if (fMembersToMove[i].getElementType() != IJavaScriptElement.FIELD)
				continue;
			final IField field= (IField) fMembersToMove[i];
			final String type= Signature.toString(field.getTypeSignature());
			Assert.isTrue(mapping.containsKey(field));
			for (final Iterator iter= ((Set) mapping.get(field)).iterator(); iter.hasNext();) {
				final IField matchingField= (IField) iter.next();
				if (field.equals(matchingField))
					continue;
				if (type.equals(Signature.toString(matchingField.getTypeSignature())))
					continue;
				final String[] keys= { JavaScriptElementLabels.getTextLabel(matchingField, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(matchingField.getDeclaringType(), JavaScriptElementLabels.ALL_FULLY_QUALIFIED)};
				final String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_different_field_type, keys);
				final RefactoringStatusContext context= JavaStatusContext.create(matchingField.getJavaScriptUnit(), matchingField.getSourceRange());
				status.addError(message, context);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context) throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 12);
			clearCaches();

			final RefactoringStatus result= new RefactoringStatus();
			result.merge(createWorkingCopyLayer(new SubProgressMonitor(monitor, 4)));
			if (result.hasFatalError())
				return result;
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			result.merge(checkGenericDeclaringType(new SubProgressMonitor(monitor, 1)));
			result.merge(checkFinalFields(new SubProgressMonitor(monitor, 1)));
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			result.merge(checkAccesses(new SubProgressMonitor(monitor, 1)));
			result.merge(checkMembersInTypeAndAllSubtypes(new SubProgressMonitor(monitor, 2)));
			result.merge(checkIfSkippingOverElements(new SubProgressMonitor(monitor, 1)));
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			if (!JdtFlags.isAbstract(getDestinationType()) && getAbstractMethods().length > 0)
				result.merge(checkConstructorCalls(getDestinationType(), new SubProgressMonitor(monitor, 1)));
			else
				monitor.worked(1);
			if (result.hasFatalError())
				return result;
			fCompilationUnitRewrites= new HashMap(3);
			result.merge(checkProjectCompliance(getCompilationUnitRewrite(fCompilationUnitRewrites, getDeclaringType().getJavaScriptUnit()), getDestinationType(), fMembersToMove));
			fChangeManager= createChangeManager(new SubProgressMonitor(monitor, 1), result);
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits()), getRefactoring().getValidationContext()));
			return result;
		} finally {
			monitor.done();
		}
	}

	private RefactoringStatus checkFinalFields(final IProgressMonitor monitor) throws JavaScriptModelException {
		final RefactoringStatus result= new RefactoringStatus();
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, fMembersToMove.length);
		for (int index= 0; index < fMembersToMove.length; index++) {
			final IMember member= fMembersToMove[index];
			if (member.getElementType() == IJavaScriptElement.FIELD) {
				if (!JdtFlags.isStatic(member)) {
					if (JdtFlags.isFinal(member)) {
						final RefactoringStatusContext context= JavaStatusContext.create(member);
						result.addWarning(RefactoringCoreMessages.PullUpRefactoring_final_fields, context);
					}
				}
			}
			monitor.worked(1);
			if (monitor.isCanceled())
				throw new OperationCanceledException();
		}
		monitor.done();
		return result;
	}

	private RefactoringStatus checkGenericDeclaringType(final SubProgressMonitor monitor) throws JavaScriptModelException {
		Assert.isNotNull(monitor);

		final RefactoringStatus status= new RefactoringStatus();
		try {
			final IMember[] pullables= getMembersToMove();
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, pullables.length);
		} finally {
			monitor.done();
		}
		return status;
	}

	private RefactoringStatus checkIfDeclaredIn(final IMember element, final IType type) throws JavaScriptModelException {
		if (element instanceof IFunction)
			return checkIfMethodDeclaredIn((IFunction) element, type);
		else if (element instanceof IField)
			return checkIfFieldDeclaredIn((IField) element, type);
		else if (element instanceof IType)
			return checkIfTypeDeclaredIn((IType) element, type);
		Assert.isTrue(false);
		return null;
	}

	private RefactoringStatus checkIfFieldDeclaredIn(final IField iField, final IType type) {
		final IField fieldInType= type.getField(iField.getElementName());
		if (!fieldInType.exists())
			return null;
		final String[] keys= { JavaScriptElementLabels.getTextLabel(fieldInType, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(type, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)};
		final String msg= Messages.format(RefactoringCoreMessages.PullUpRefactoring_Field_declared_in_class, keys);
		final RefactoringStatusContext context= JavaStatusContext.create(fieldInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}

	private RefactoringStatus checkIfMethodDeclaredIn(final IFunction iMethod, final IType type) throws JavaScriptModelException {
		final IFunction methodInType= JavaModelUtil.findMethod(iMethod.getElementName(), iMethod.getParameterTypes(), iMethod.isConstructor(), type);
		if (methodInType == null || !methodInType.exists())
			return null;
		final String[] keys= { JavaScriptElementLabels.getTextLabel(methodInType, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(type, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)};
		final String msg= Messages.format(RefactoringCoreMessages.PullUpRefactoring_Method_declared_in_class, keys);
		final RefactoringStatusContext context= JavaStatusContext.create(methodInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}

	private RefactoringStatus checkIfSkippingOverElements(final IProgressMonitor monitor) throws JavaScriptModelException {
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 1);
		try {
			final Set skippedTypes= getSkippedSuperTypes(new SubProgressMonitor(monitor, 1));
			final IType[] skipped= (IType[]) skippedTypes.toArray(new IType[skippedTypes.size()]);
			final RefactoringStatus result= new RefactoringStatus();
			for (int i= 0; i < fMembersToMove.length; i++) {
				final IMember element= fMembersToMove[i];
				for (int j= 0; j < skipped.length; j++) {
					result.merge(checkIfDeclaredIn(element, skipped[j]));
				}
			}
			return result;
		} finally {
			monitor.done();
		}
	}

	private RefactoringStatus checkIfTypeDeclaredIn(final IType iType, final IType type) {
		final IType typeInType= type.getType(iType.getElementName());
		if (!typeInType.exists())
			return null;
		final String[] keys= { JavaScriptElementLabels.getTextLabel(typeInType, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(type, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)};
		final String msg= Messages.format(RefactoringCoreMessages.PullUpRefactoring_Type_declared_in_class, keys);
		final RefactoringStatusContext context= JavaStatusContext.create(typeInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkInitialConditions(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 1);
			final RefactoringStatus status= new RefactoringStatus();
			status.merge(checkDeclaringType(new SubProgressMonitor(monitor, 1)));
			if (status.hasFatalError())
				return status;
			status.merge(checkIfMembersExist());
			if (status.hasFatalError())
				return status;
			return status;
		} finally {
			monitor.done();
		}
	}

	private void checkMembersInDestinationType(final RefactoringStatus status, final Set set) throws JavaScriptModelException {
		final IMember[] destinationMembers= getCreatedDestinationMembers();
		final List list= new ArrayList(destinationMembers.length);
		list.addAll(Arrays.asList(destinationMembers));
		list.addAll(set);
		list.removeAll(Arrays.asList(fDeletedMethods));
		final IMember[] members= (IMember[]) list.toArray(new IMember[list.size()]);
		status.merge(MemberCheckUtil.checkMembersInDestinationType(members, getDestinationType()));
	}

	private RefactoringStatus checkMembersInTypeAndAllSubtypes(final IProgressMonitor monitor) throws JavaScriptModelException {
		final RefactoringStatus result= new RefactoringStatus();
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 3);
		final Set notDeletedMembers= getNotDeletedMembers(new SubProgressMonitor(monitor, 1));
		final Set notDeletedMembersInTargetType= new HashSet();
		final Set notDeletedMembersInSubtypes= new HashSet();
		for (final Iterator iter= notDeletedMembers.iterator(); iter.hasNext();) {
			final IMember member= (IMember) iter.next();
			if (getDestinationType().equals(member.getDeclaringType()))
				notDeletedMembersInTargetType.add(member);
			else
				notDeletedMembersInSubtypes.add(member);
		}
		checkMembersInDestinationType(result, notDeletedMembersInTargetType);
		checkAccessModifiers(result, notDeletedMembersInSubtypes);
		checkMethodReturnTypes(new SubProgressMonitor(monitor, 1), result, notDeletedMembersInSubtypes);
		checkFieldTypes(new SubProgressMonitor(monitor, 1), result);
		monitor.done();
		return result;
	}

	private void checkMethodReturnTypes(final IProgressMonitor monitor, final RefactoringStatus status, final Set notDeletedMembersInSubtypes) throws JavaScriptModelException {
		final Map mapping= getMatchingMembers(getDestinationTypeHierarchy(monitor), getDestinationType(), true);
		final IMember[] members= getCreatedDestinationMembers();
		for (int i= 0; i < members.length; i++) {
			if (members[i].getElementType() != IJavaScriptElement.METHOD)
				continue;
			final IFunction method= (IFunction) members[i];
			if (mapping.containsKey(method)) {
				final Set set= (Set) mapping.get(method);
				if (set != null) {
					final String returnType= Signature.toString(Signature.getReturnType(method.getSignature()).toString());
					for (final Iterator iter= set.iterator(); iter.hasNext();) {
						final IFunction matchingMethod= (IFunction) iter.next();
						if (method.equals(matchingMethod))
							continue;
						if (!notDeletedMembersInSubtypes.contains(matchingMethod))
							continue;
						if (returnType.equals(Signature.toString(Signature.getReturnType(matchingMethod.getSignature()).toString())))
							continue;
						final String[] keys= { JavaScriptElementLabels.getTextLabel(matchingMethod, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(matchingMethod.getDeclaringType(), JavaScriptElementLabels.ALL_FULLY_QUALIFIED)};
						final String message= Messages.format(RefactoringCoreMessages.PullUpRefactoring_different_method_return_type, keys);
						final RefactoringStatusContext context= JavaStatusContext.create(matchingMethod.getJavaScriptUnit(), matchingMethod.getNameRange());
						status.addError(message, context);
					}
				}
			}
		}
	}

	protected void clearCaches() {
		super.clearCaches();
		fCachedMembersReferences.clear();
		fCachedDestinationTypeHierarchy= null;
		fCachedDeclaringSuperTypeHierarchy= null;
	}

	private void copyBodyOfPulledUpMethod(final CompilationUnitRewrite sourceRewrite, final CompilationUnitRewrite targetRewrite, final IFunction method, final FunctionDeclaration oldMethod, final FunctionDeclaration newMethod, final TypeVariableMaplet[] mapping, final IProgressMonitor monitor) throws JavaScriptModelException {
		final Block body= oldMethod.getBody();
		if (body == null) {
			newMethod.setBody(null);
			return;
		}
		try {
			final IDocument document= new Document(method.getJavaScriptUnit().getBuffer().getContents());
			final ASTRewrite rewrite= ASTRewrite.create(body.getAST());
			final ITrackedNodePosition position= rewrite.track(body);
			body.accept(new PullUpAstNodeMapper(sourceRewrite, targetRewrite, rewrite, getDeclaringSuperTypeHierarchy(monitor).getSuperclass(getDeclaringType()), mapping, oldMethod.resolveBinding()));
			rewrite.rewriteAST(document, method.getJavaScriptProject().getOptions(true)).apply(document, TextEdit.NONE);
			String content= document.get(position.getStartPosition(), position.getLength());
			final String[] lines= Strings.convertIntoLines(content);
			Strings.trimIndentation(lines, method.getJavaScriptProject(), false);
			content= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(method));
			newMethod.setBody((Block) targetRewrite.getASTRewrite().createStringPlaceholder(content, ASTNode.BLOCK));
		} catch (MalformedTreeException exception) {
			JavaScriptPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaScriptPlugin.log(exception);
		}
	}

	private void createAbstractMethod(final IFunction sourceMethod, final CompilationUnitRewrite sourceRewriter, final JavaScriptUnit declaringCuNode, final AbstractTypeDeclaration destination, final TypeVariableMaplet[] mapping, final CompilationUnitRewrite targetRewrite, final Map adjustments, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaScriptModelException {
		final FunctionDeclaration oldMethod= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		final FunctionDeclaration newMethod= targetRewrite.getAST().newFunctionDeclaration();
		newMethod.setBody(null);
		newMethod.setConstructor(false);
		newMethod.setExtraDimensions(oldMethod.getExtraDimensions());
		newMethod.setJavadoc(null);
		int modifiers= getModifiersWithUpdatedVisibility(sourceMethod, Modifier.ABSTRACT | JdtFlags.clearFlag(Modifier.NATIVE | Modifier.FINAL, sourceMethod.getFlags()), adjustments, monitor, false, status);
		if (oldMethod.isVarargs())
			modifiers&= ~Flags.AccVarargs;
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(targetRewrite.getAST(), modifiers));
		newMethod.setName(((SimpleName) ASTNode.copySubtree(targetRewrite.getAST(), oldMethod.getName())));
		copyReturnType(targetRewrite.getASTRewrite(), getDeclaringType().getJavaScriptUnit(), oldMethod, newMethod, mapping);
		copyParameters(targetRewrite.getASTRewrite(), getDeclaringType().getJavaScriptUnit(), oldMethod, newMethod, mapping);
		copyThrownExceptions(oldMethod, newMethod);
		ImportRewriteUtil.addImports(targetRewrite, newMethod, new HashMap(), new HashMap(), false);
		targetRewrite.getASTRewrite().getListRewrite(destination, destination.getBodyDeclarationsProperty()).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, destination.bodyDeclarations()), targetRewrite.createCategorizedGroupDescription(RefactoringCoreMessages.PullUpRefactoring_add_abstract_method, SET_PULL_UP));
	}

	/**
	 * {@inheritDoc}
	 */
	public Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {
			final Map arguments= new HashMap();
			String project= null;
			final IType declaring= getDeclaringType();
			final IJavaScriptProject javaProject= declaring.getJavaScriptProject();
			if (javaProject != null)
				project= javaProject.getElementName();
			int flags= JavaScriptRefactoringDescriptor.JAR_MIGRATION | JavaScriptRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
			try {
				if (declaring.isLocal() || declaring.isAnonymous())
					flags|= JavaScriptRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
			} catch (JavaScriptModelException exception) {
				JavaScriptPlugin.log(exception);
			}
			final String description= fMembersToMove.length == 1 ? Messages.format(RefactoringCoreMessages.PullUpRefactoring_descriptor_description_short, new String[] { fMembersToMove[0].getElementName(), fDestinationType.getElementName()}) : Messages.format(RefactoringCoreMessages.PullUpRefactoring_descriptor_description_short_multiple, fDestinationType.getElementName());
			final String header= fMembersToMove.length == 1 ? Messages.format(RefactoringCoreMessages.PullUpRefactoring_descriptor_description_full, new String[] { JavaScriptElementLabels.getElementLabel(fMembersToMove[0], JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getElementLabel(declaring, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getElementLabel(fDestinationType, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)}) : Messages.format(RefactoringCoreMessages.PullUpRefactoring_descriptor_description, new String[] { JavaScriptElementLabels.getElementLabel(declaring, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getElementLabel(fDestinationType, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)});
			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
			comment.addSetting(Messages.format(RefactoringCoreMessages.MoveStaticMembersProcessor_target_element_pattern, JavaScriptElementLabels.getElementLabel(fDestinationType, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)));
			addSuperTypeSettings(comment, true);
			final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(IJavaScriptRefactorings.PULL_UP, project, description, comment.asString(), arguments, flags);
			arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fDestinationType));
			arguments.put(ATTRIBUTE_REPLACE, Boolean.valueOf(fReplace).toString());
			arguments.put(ATTRIBUTE_INSTANCEOF, Boolean.valueOf(fInstanceOf).toString());
			arguments.put(ATTRIBUTE_STUBS, Boolean.valueOf(fCreateMethodStubs).toString());
			arguments.put(ATTRIBUTE_PULL, Integer.valueOf(fMembersToMove.length).toString());
			for (int offset= 0; offset < fMembersToMove.length; offset++)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + 1), descriptor.elementToHandle(fMembersToMove[offset]));
			arguments.put(ATTRIBUTE_DELETE, Integer.valueOf(fDeletedMethods.length).toString());
			for (int offset= 0; offset < fDeletedMethods.length; offset++)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + fMembersToMove.length + 1), descriptor.elementToHandle(fDeletedMethods[offset]));
			arguments.put(ATTRIBUTE_ABSTRACT, Integer.valueOf(fAbstractMethods.length).toString());
			for (int offset= 0; offset < fAbstractMethods.length; offset++)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + fMembersToMove.length + fDeletedMethods.length + 1), descriptor.elementToHandle(fAbstractMethods[offset]));
			return new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.PullUpRefactoring_Pull_Up, fChangeManager.getAllChanges());
		} finally {
			monitor.done();
			clearCaches();
		}
	}

	private TextEditBasedChangeManager createChangeManager(final IProgressMonitor monitor, final RefactoringStatus status) throws CoreException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 24);
			final IJavaScriptUnit source= getDeclaringType().getJavaScriptUnit();
			final IType destination= getDestinationType();
			final IJavaScriptUnit target= destination.getJavaScriptUnit();
			final CompilationUnitRewrite sourceRewriter= getCompilationUnitRewrite(fCompilationUnitRewrites, source);
			final CompilationUnitRewrite targetRewriter= getCompilationUnitRewrite(fCompilationUnitRewrites, target);
			final Map deleteMap= createMembersToDeleteMap(new SubProgressMonitor(monitor, 1));
			final Map effectedMap= createEffectedTypesMap(new SubProgressMonitor(monitor, 1));
			final IJavaScriptUnit[] units= getAffectedCompilationUnits(new SubProgressMonitor(monitor, 1));
			IJavaScriptUnit unit= null;
			CompilationUnitRewrite rewrite= null;
			final Map adjustments= new HashMap();
			MemberVisibilityAdjustor adjustor= null;
			final IProgressMonitor sub= new SubProgressMonitor(monitor, 1);
			try {
				sub.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, units.length * 11);
				for (int index= 0; index < units.length; index++) {
					unit= units[index];
					if (!(source.equals(unit) || target.equals(unit) || deleteMap.containsKey(unit) || effectedMap.containsKey(unit))) {
						sub.worked(10);
						continue;
					}
					rewrite= getCompilationUnitRewrite(fCompilationUnitRewrites, unit);
					if (deleteMap.containsKey(unit)) {
						LinkedList list= new LinkedList((List) deleteMap.get(unit));
						deleteDeclarationNodes(sourceRewriter, sourceRewriter.getCu().equals(targetRewriter.getCu()), rewrite, list, SET_PULL_UP);
					}
					final JavaScriptUnit root= sourceRewriter.getRoot();
					if (unit.equals(target)) {
						final ASTRewrite rewriter= rewrite.getASTRewrite();
						if (!JdtFlags.isAbstract(destination) && getAbstractMethods().length > 0) {
							final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(destination, rewrite.getRoot());
							ModifierRewrite.create(rewriter, declaration).setModifiers(declaration.getModifiers() | Modifier.ABSTRACT, rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.PullUpRefactoring_make_target_abstract, SET_PULL_UP));
						}
						final IProgressMonitor subsub= new SubProgressMonitor(sub, 1);
						final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(destination, rewrite.getRoot());
						fMembersToMove= JavaElementUtil.sortByOffset(fMembersToMove);
						subsub.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, fMembersToMove.length);
						IMember member= null;
						for (int offset= fMembersToMove.length - 1; offset >= 0; offset--) {
							member= fMembersToMove[offset];
							adjustor= new MemberVisibilityAdjustor(destination, member);
							adjustor.setRewrite(sourceRewriter.getASTRewrite(), root);

							// TW: set to error if bug 78387 is fixed
							adjustor.setFailureSeverity(RefactoringStatus.WARNING);

							adjustor.setOwner(fOwner);
							adjustor.setRewrites(fCompilationUnitRewrites);
							adjustor.setStatus(status);
							adjustor.setAdjustments(adjustments);
							adjustor.adjustVisibility(new SubProgressMonitor(subsub, 1));
							adjustments.remove(member);
							if (member instanceof IField) {
								final VariableDeclarationFragment oldField= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) member, root);
								if (oldField != null) {
									int flags= getModifiersWithUpdatedVisibility(member, member.getFlags(), adjustments, new SubProgressMonitor(subsub, 1), true, status);
									final FieldDeclaration newField= createNewFieldDeclarationNode(rewriter, root, (IField) member, oldField, null, new SubProgressMonitor(subsub, 1), status, flags);
									rewriter.getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newField, ASTNodes.getInsertionIndex(newField, declaration.bodyDeclarations()), rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_add_member, SET_PULL_UP));
									ImportRewriteUtil.addImports(rewrite, oldField.getParent(), new HashMap(), new HashMap(), false);
								}
							} else if (member instanceof IFunction) {
								final FunctionDeclaration oldMethod= ASTNodeSearchUtil.getMethodDeclarationNode((IFunction) member, root);
								if (oldMethod != null) {
									final FunctionDeclaration newMethod= createNewMethodDeclarationNode(sourceRewriter, rewrite, ((IFunction) member), oldMethod, root, null, adjustments, new SubProgressMonitor(subsub, 1), status);
									rewriter.getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, declaration.bodyDeclarations()), rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_add_member, SET_PULL_UP));
									ImportRewriteUtil.addImports(rewrite, oldMethod, new HashMap(), new HashMap(), false);
								}
							} else if (member instanceof IType) {
								final AbstractTypeDeclaration oldType= ASTNodeSearchUtil.getAbstractTypeDeclarationNode((IType) member, root);
								if (oldType != null) {
									final BodyDeclaration newType= createNewTypeDeclarationNode(((IType) member), oldType, root, null, rewriter);
									rewriter.getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newType, ASTNodes.getInsertionIndex(newType, declaration.bodyDeclarations()), rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_add_member, SET_PULL_UP));
									ImportRewriteUtil.addImports(rewrite, oldType, new HashMap(), new HashMap(), false);
								}
							} else
								Assert.isTrue(false);
							subsub.worked(1);
						}
						subsub.done();
						for (int offset= 0; offset < fAbstractMethods.length; offset++)
							createAbstractMethod(fAbstractMethods[offset], sourceRewriter, root, declaration, null, rewrite, adjustments, new SubProgressMonitor(sub, 1), status);
					} else
						sub.worked(2);
					if (unit.equals(sourceRewriter.getCu())) {
						final IProgressMonitor subsub= new SubProgressMonitor(sub, 1);
						subsub.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, fAbstractMethods.length * 2);
						IFunction method= null;
						for (int offset= 0; offset < fAbstractMethods.length; offset++) {
							method= fAbstractMethods[offset];
							adjustor= new MemberVisibilityAdjustor(destination, method);
							adjustor.setRewrite(sourceRewriter.getASTRewrite(), root);
							adjustor.setRewrites(fCompilationUnitRewrites);

							// TW: set to error if bug 78387 is fixed
							adjustor.setFailureSeverity(RefactoringStatus.WARNING);

							adjustor.setOwner(fOwner);
							adjustor.setStatus(status);
							adjustor.setAdjustments(adjustments);
							if (needsVisibilityAdjustment(method, false, new SubProgressMonitor(subsub, 1), status))
								adjustments.put(method, new MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment(method, Modifier.ModifierKeyword.PROTECTED_KEYWORD, RefactoringStatus.createWarningStatus(Messages.format(RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_method_warning, new String[] { MemberVisibilityAdjustor.getLabel(method), RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_protected}), JavaStatusContext.create(method))));
						}
					} else
						sub.worked(2);
					if (effectedMap.containsKey(unit))
						addNecessaryMethodStubs((List) effectedMap.get(unit), root, rewrite, adjustments, new SubProgressMonitor(sub, 2), status);
					if (sub.isCanceled())
						throw new OperationCanceledException();
				}
			} finally {
				sub.done();
			}
			if (adjustor != null && !adjustments.isEmpty())
				adjustor.rewriteVisibility(new SubProgressMonitor(monitor, 1));
			final TextEditBasedChangeManager manager= new TextEditBasedChangeManager();
			if (fReplace) {
				final Set set= fCompilationUnitRewrites.keySet();
				for (final Iterator iterator= set.iterator(); iterator.hasNext();) {
					unit= (IJavaScriptUnit) iterator.next();
					rewrite= (CompilationUnitRewrite) fCompilationUnitRewrites.get(unit);
					if (rewrite != null) {
						final CompilationUnitChange change= rewrite.createChange(false, null);
						if (change != null)
							manager.manage(unit, change);
					}
				}
				TextEdit edit= null;
				TextEditBasedChange change= null;
				final Map workingcopies= new HashMap();
				final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 1);
				try {
					subMonitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, set.size());
					for (final Iterator iterator= set.iterator(); iterator.hasNext();) {
						unit= (IJavaScriptUnit) iterator.next();
						change= manager.get(unit);
						if (change instanceof TextChange) {
							edit= ((TextChange) change).getEdit();
							if (edit != null) {
								final IJavaScriptUnit copy= createWorkingCopy(unit, edit, status, new SubProgressMonitor(monitor, 1));
								if (copy != null)
									workingcopies.put(unit, copy);
							}
						}
					}
					final IJavaScriptUnit current= (IJavaScriptUnit) workingcopies.get(sourceRewriter.getCu());
					if (current != null)
						rewriteTypeOccurrences(manager, sourceRewriter, current, new HashSet(), status, new SubProgressMonitor(monitor, 16));
				} finally {
					subMonitor.done();
					IJavaScriptUnit[] cus= manager.getAllCompilationUnits();
					for (int index= 0; index < cus.length; index++) {
						CompilationUnitChange current= (CompilationUnitChange) manager.get(cus[index]);
						if (change != null && current.getEdit() == null)
							manager.remove(cus[index]);
					}
				}
			}
			registerChanges(manager);
			return manager;
		} finally {
			fCompilationUnitRewrites.clear();
			monitor.done();
		}
	}

	private Map createEffectedTypesMap(final IProgressMonitor monitor) throws JavaScriptModelException {
		if (!(fCreateMethodStubs && getAbstractMethods().length > 0))
			return new HashMap(0);
		final Set effected= getEffectedSubTypes(getDestinationTypeHierarchy(monitor), getDestinationType());
		final Map result= new HashMap();
		for (final Iterator iterator= effected.iterator(); iterator.hasNext();) {
			final IType type= (IType) iterator.next();
			final IJavaScriptUnit unit= type.getJavaScriptUnit();
			if (!result.containsKey(unit))
				result.put(unit, new ArrayList(1));
			((List) result.get(unit)).add(type);
		}
		return result;
	}

	private JSdoc createJavadocForStub(final String enclosingTypeName, final FunctionDeclaration oldMethod, final FunctionDeclaration newMethodNode, final IJavaScriptUnit cu, final ASTRewrite rewrite) throws CoreException {
		if (fSettings.createComments) {
			final IFunctionBinding binding= oldMethod.resolveBinding();
			if (binding != null) {
				final ITypeBinding[] params= binding.getParameterTypes();
				final String fullTypeName= JavaModelUtil.getFullyQualifiedName(getDestinationType());
				final String[] fullParamNames= new String[params.length];
				for (int i= 0; i < fullParamNames.length; i++) {
					fullParamNames[i]= Bindings.getFullyQualifiedName(params[i]);
				}
				final String comment= CodeGeneration.getMethodComment(cu, enclosingTypeName, newMethodNode, false, binding.getName(), fullTypeName, fullParamNames, StubUtility.getLineDelimiterUsed(cu));
				return (JSdoc) rewrite.createStringPlaceholder(comment, ASTNode.JSDOC);
			}
		}
		return null;
	}

	private Map createMembersToDeleteMap(final IProgressMonitor monitor) throws JavaScriptModelException {
		final IMember[] membersToDelete= getMembersToDelete(monitor);
		final Map result= new HashMap();
		for (int i= 0; i < membersToDelete.length; i++) {
			final IMember member= membersToDelete[i];
			final IJavaScriptUnit cu= member.getJavaScriptUnit();
			if (!result.containsKey(cu))
				result.put(cu, new ArrayList(1));
			((List) result.get(cu)).add(member);
		}
		return result;
	}

	private FunctionDeclaration createNewMethodDeclarationNode(final CompilationUnitRewrite sourceRewrite, final CompilationUnitRewrite targetRewrite, final IFunction sourceMethod, final FunctionDeclaration oldMethod, final JavaScriptUnit declaringCuNode, final TypeVariableMaplet[] mapping, final Map adjustments, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaScriptModelException {
		final ASTRewrite rewrite= targetRewrite.getASTRewrite();
		final AST ast= rewrite.getAST();
		final FunctionDeclaration newMethod= ast.newFunctionDeclaration();
		copyBodyOfPulledUpMethod(sourceRewrite, targetRewrite, sourceMethod, oldMethod, newMethod, mapping, monitor);
		newMethod.setConstructor(oldMethod.isConstructor());
		newMethod.setExtraDimensions(oldMethod.getExtraDimensions());
		copyJavadocNode(rewrite, sourceMethod, oldMethod, newMethod);
		int modifiers= getModifiersWithUpdatedVisibility(sourceMethod, sourceMethod.getFlags(), adjustments, monitor, true, status);
		if (oldMethod.isVarargs())
			modifiers&= ~Flags.AccVarargs;
		copyAnnotations(oldMethod, newMethod);
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, modifiers));
		newMethod.setName(((SimpleName) ASTNode.copySubtree(ast, oldMethod.getName())));
		copyReturnType(rewrite, getDeclaringType().getJavaScriptUnit(), oldMethod, newMethod, mapping);
		copyParameters(rewrite, getDeclaringType().getJavaScriptUnit(), oldMethod, newMethod, mapping);
		copyThrownExceptions(oldMethod, newMethod);
		return newMethod;
	}

	private BodyDeclaration createNewTypeDeclarationNode(final IType type, final AbstractTypeDeclaration oldType, final JavaScriptUnit declaringCuNode, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite) throws JavaScriptModelException {
		final IJavaScriptUnit declaringCu= getDeclaringType().getJavaScriptUnit();
		if (!JdtFlags.isPublic(type)) {
			if (mapping.length > 0)
				return createPlaceholderForTypeDeclaration(oldType, declaringCu, mapping, rewrite, true);

			return createPlaceholderForProtectedTypeDeclaration(oldType, declaringCuNode, declaringCu, rewrite, true);
		}
		if (mapping.length > 0)
			return createPlaceholderForTypeDeclaration(oldType, declaringCu, mapping, rewrite, true);

		return createPlaceholderForTypeDeclaration(oldType, declaringCu, rewrite, true);
	}

	private IJavaScriptUnit createWorkingCopy(final IJavaScriptUnit unit, final TextEdit edit, final RefactoringStatus status, final IProgressMonitor monitor) {
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 1);
			final IJavaScriptUnit copy= getSharedWorkingCopy(unit.getPrimary(), new SubProgressMonitor(monitor, 1));
			final IDocument document= new Document(unit.getBuffer().getContents());
			edit.apply(document, TextEdit.UPDATE_REGIONS);
			copy.getBuffer().setContents(document.get());
			JavaModelUtil.reconcile(copy);
			return copy;
		} catch (JavaScriptModelException exception) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
		} catch (MalformedTreeException exception) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
		} catch (BadLocationException exception) {
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
		} finally {
			monitor.done();
		}
		return null;
	}

	/**
	 * Creates a working copy layer if necessary.
	 * 
	 * @param monitor
	 *            the progress monitor to use
	 * @return a status describing the outcome of the operation
	 */
	protected RefactoringStatus createWorkingCopyLayer(IProgressMonitor monitor) {
		try {
			monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 1);
			IJavaScriptUnit unit= getDeclaringType().getJavaScriptUnit();
			if (fLayer)
				unit= unit.findWorkingCopy(fOwner);
			resetWorkingCopies(unit);
			return new RefactoringStatus();
		} finally {
			monitor.done();
		}
	}

	private IFunction[] getAbstractMethods() throws JavaScriptModelException {
		final IFunction[] toDeclareAbstract= fAbstractMethods;
		final IFunction[] abstractPulledUp= getAbstractMethodsToPullUp();
		final Set result= new LinkedHashSet(toDeclareAbstract.length + abstractPulledUp.length + fMembersToMove.length);

		result.addAll(Arrays.asList(toDeclareAbstract));
		result.addAll(Arrays.asList(abstractPulledUp));
		return (IFunction[]) result.toArray(new IFunction[result.size()]);
	}

	private IFunction[] getAbstractMethodsToPullUp() throws JavaScriptModelException {
		final List result= new ArrayList(fMembersToMove.length);
		for (int i= 0; i < fMembersToMove.length; i++) {
			final IMember member= fMembersToMove[i];
			if (member instanceof IFunction && JdtFlags.isAbstract(member))
				result.add(member);
		}
		return (IFunction[]) result.toArray(new IFunction[result.size()]);
	}

	public IMember[] getAdditionalRequiredMembersToPullUp(final IProgressMonitor monitor) throws JavaScriptModelException {
		final IMember[] members= getCreatedDestinationMembers();
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_calculating_required, members.length);// not
		final List queue= new ArrayList(members.length);
		queue.addAll(Arrays.asList(members));
		if (queue.isEmpty())
			return new IMember[0];
		int i= 0;
		IMember current;
		do {
			current= (IMember) queue.get(i);
			addAllRequiredPullableMembers(queue, current, new SubProgressMonitor(monitor, 1));
			i++;
			if (queue.size() == i)
				current= null;
		} while (current != null);
		queue.removeAll(Arrays.asList(members));// report only additional
		return (IMember[]) queue.toArray(new IMember[queue.size()]);
	}

	private IJavaScriptUnit[] getAffectedCompilationUnits(final IProgressMonitor monitor) throws JavaScriptModelException {
		final IType[] allSubtypes= getDestinationTypeHierarchy(monitor).getAllSubtypes(getDestinationType());
		final Set result= new HashSet(allSubtypes.length);
		for (int i= 0; i < allSubtypes.length; i++) {
			result.add(allSubtypes[i].getJavaScriptUnit());
		}
		result.add(getDestinationType().getJavaScriptUnit());
		return (IJavaScriptUnit[]) result.toArray(new IJavaScriptUnit[result.size()]);
	}

	public IType[] getCandidateTypes(final RefactoringStatus status, final IProgressMonitor monitor) throws JavaScriptModelException {
		final IType declaring= getDeclaringType();
		final IType[] superTypes= declaring.newSupertypeHierarchy(fOwner, monitor).getAllSuperclasses(declaring);
		final List list= new ArrayList(superTypes.length);
		int binary= 0;
		for (int index= 0; index < superTypes.length; index++) {
			final IType type= superTypes[index];
			if (type != null && type.exists() && !type.isReadOnly() && !type.isBinary() && !"java.lang.Object".equals(type.getFullyQualifiedName())) { //$NON-NLS-1$
				list.add(type);
			} else {
				if (type != null && type.isBinary()) {
					binary++;
				}
			}
		}
		if (superTypes.length == 1 && superTypes[0].getFullyQualifiedName().equals("java.lang.Object")) //$NON-NLS-1$
			status.addFatalError(RefactoringCoreMessages.PullUPRefactoring_not_java_lang_object);
		else if (superTypes.length == binary)
			status.addFatalError(RefactoringCoreMessages.PullUPRefactoring_no_all_binary);

		Collections.reverse(list);
		return (IType[]) list.toArray(new IType[list.size()]);
	}

	protected CompilationUnitRewrite getCompilationUnitRewrite(final Map rewrites, final IJavaScriptUnit unit) {
		Assert.isNotNull(rewrites);
		Assert.isNotNull(unit);
		CompilationUnitRewrite rewrite= (CompilationUnitRewrite) rewrites.get(unit);
		if (rewrite == null) {
			rewrite= new CompilationUnitRewrite(fOwner, unit);
			rewrites.put(unit, rewrite);
		}
		return rewrite;
	}

	private IMember[] getCreatedDestinationMembers() {
		final List result= new ArrayList(fMembersToMove.length + fAbstractMethods.length);
		result.addAll(Arrays.asList(fMembersToMove));
		result.addAll(Arrays.asList(fAbstractMethods));
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	public boolean getCreateMethodStubs() {
		return fCreateMethodStubs;
	}

	public ITypeHierarchy getDeclaringSuperTypeHierarchy(final IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			if (fCachedDeclaringSuperTypeHierarchy != null)
				return fCachedDeclaringSuperTypeHierarchy;
			fCachedDeclaringSuperTypeHierarchy= getDeclaringType().newSupertypeHierarchy(fOwner, monitor);
			return fCachedDeclaringSuperTypeHierarchy;
		} finally {
			monitor.done();
		}
	}

	public IType getDestinationType() {
		return fDestinationType;
	}

	public ITypeHierarchy getDestinationTypeHierarchy(final IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			if (fCachedDestinationTypeHierarchy != null && fCachedDestinationTypeHierarchy.getType().equals(getDestinationType()))
				return fCachedDestinationTypeHierarchy;
			fCachedDestinationTypeHierarchy= getDestinationType().newTypeHierarchy(fOwner, monitor);
			return fCachedDestinationTypeHierarchy;
		} finally {
			monitor.done();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Object[] getElements() {
		return fMembersToMove;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIdentifier() {
		return IDENTIFIER;
	}

	public IMember[] getMatchingElements(final IProgressMonitor monitor, final boolean includeAbstract) throws JavaScriptModelException {
		try {
			final Set result= new HashSet();
			final IType destination= getDestinationType();
			final Map matching= getMatchingMembers(getDestinationTypeHierarchy(monitor), getDestinationType(), includeAbstract);
			for (final Iterator iterator= matching.keySet().iterator(); iterator.hasNext();) {
				final IMember key= (IMember) iterator.next();
				Assert.isTrue(!key.getDeclaringType().equals(destination));
				result.addAll((Set) matching.get(key));
			}
			return (IMember[]) result.toArray(new IMember[result.size()]);
		} finally {
			monitor.done();
		}
	}

	private Map getMatchingMembers(final ITypeHierarchy hierarchy, final IType type, final boolean includeAbstract) throws JavaScriptModelException {
		final Map result= new HashMap();
		result.putAll(getMatchingMembersMapping(type));
		final IType[] subTypes= hierarchy.getAllSubtypes(type);
		for (int i= 0; i < subTypes.length; i++) {
			final Map map= getMatchingMembersMapping(subTypes[i]);
			mergeMaps(result, map);
			upgradeMap(result, map);
		}
		if (includeAbstract)
			return result;

		for (int i= 0; i < fAbstractMethods.length; i++) {
			if (result.containsKey(fAbstractMethods[i]))
				result.remove(fAbstractMethods[i]);
		}
		return result;
	}

	private Map getMatchingMembersMapping(final IType initial) throws JavaScriptModelException {
		final Map result= new HashMap();
		final IMember[] members= getCreatedDestinationMembers();
		for (int i= 0; i < members.length; i++) {
			final IMember member= members[i];
			if (member instanceof IFunction) {
				final IFunction method= (IFunction) member;
				final IFunction found= MemberCheckUtil.findMethod(method, initial.getFunctions());
				if (found != null)
					addMatchingMember(result, method, found);
			} else if (member instanceof IField) {
				final IField field= (IField) member;
				final IField found= initial.getField(field.getElementName());
				if (found.exists())
					addMatchingMember(result, field, found);
			} else if (member instanceof IType) {
				final IType type= (IType) member;
				final IType found= initial.getType(type.getElementName());
				if (found.exists())
					addMatchingMember(result, type, found);
			} else
				Assert.isTrue(false);
		}

		return result;
	}

	private IMember[] getMembersToDelete(final IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			final IMember[] typesToDelete= getMembers(fMembersToMove, IJavaScriptElement.TYPE);
			final IMember[] matchingElements= getMatchingElements(monitor, false);
			final IMember[] matchingFields= getMembers(matchingElements, IJavaScriptElement.FIELD);
			return JavaElementUtil.merge(JavaElementUtil.merge(matchingFields, typesToDelete), fDeletedMethods);
		} finally {
			monitor.done();
		}
	}

	private int getModifiersWithUpdatedVisibility(final IMember member, final int modifiers, final Map adjustments, final IProgressMonitor monitor, final boolean considerReferences, final RefactoringStatus status) throws JavaScriptModelException {
		if (needsVisibilityAdjustment(member, considerReferences, monitor, status)) {
			final MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment adjustment= new MemberVisibilityAdjustor.OutgoingMemberVisibilityAdjustment(member, Modifier.ModifierKeyword.PROTECTED_KEYWORD, RefactoringStatus.createWarningStatus(Messages.format(MemberVisibilityAdjustor.getMessage(member), new String[] { MemberVisibilityAdjustor.getLabel(member), MemberVisibilityAdjustor.getLabel(Modifier.ModifierKeyword.PROTECTED_KEYWORD)})));
			adjustment.setNeedsRewriting(false);
			adjustments.put(member, adjustment);
			return JdtFlags.clearAccessModifiers(modifiers) | Modifier.PROTECTED;
		}
		return modifiers;
	}

	private Set getNotDeletedMembers(final IProgressMonitor monitor) throws JavaScriptModelException {
		final Set matchingSet= new HashSet();
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 2);
		matchingSet.addAll(Arrays.asList(getMatchingElements(new SubProgressMonitor(monitor, 1), true)));
		matchingSet.removeAll(Arrays.asList(getMembersToDelete(new SubProgressMonitor(monitor, 1))));
		monitor.done();
		return matchingSet;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getProcessorName() {
		return RefactoringCoreMessages.PullUpRefactoring_Pull_Up;
	}

	public IMember[] getPullableMembersOfDeclaringType() {
		try {
			return RefactoringAvailabilityTester.getPullUpMembers(getDeclaringType());
		} catch (JavaScriptModelException e) {
			return new IMember[0];
		}
	}

	// skipped super classes are those declared in the hierarchy between the
	// declaring type of the selected members
	// and the target type
	private Set getSkippedSuperTypes(final IProgressMonitor monitor) throws JavaScriptModelException {
		monitor.beginTask(RefactoringCoreMessages.PullUpRefactoring_checking, 1);
		try {
			if (fCachedSkippedSuperTypes != null && getDestinationTypeHierarchy(new SubProgressMonitor(monitor, 1)).getType().equals(getDestinationType()))
				return fCachedSkippedSuperTypes;
			final ITypeHierarchy hierarchy= getDestinationTypeHierarchy(new SubProgressMonitor(monitor, 1));
			fCachedSkippedSuperTypes= new HashSet(2);
			IType current= hierarchy.getSuperclass(getDeclaringType());
			while (current != null && !current.equals(getDestinationType())) {
				fCachedSkippedSuperTypes.add(current);
				current= hierarchy.getSuperclass(current);
			}
			return fCachedSkippedSuperTypes;
		} finally {
			monitor.done();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.TYPE)
					return ScriptableRefactoring.createInputFatalStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.PULL_UP);
				else
					fDestinationType= (IType) element;
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String stubs= extended.getAttribute(ATTRIBUTE_STUBS);
			if (stubs != null) {
				fCreateMethodStubs= Boolean.valueOf(stubs).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_STUBS));
			final String instance= extended.getAttribute(ATTRIBUTE_INSTANCEOF);
			if (instance != null) {
				fInstanceOf= Boolean.valueOf(instance).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_INSTANCEOF));
			final String replace= extended.getAttribute(ATTRIBUTE_REPLACE);
			if (replace != null) {
				fReplace= Boolean.valueOf(replace).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_REPLACE));
			int pullCount= 0;
			int abstractCount= 0;
			int deleteCount= 0;
			String value= extended.getAttribute(ATTRIBUTE_ABSTRACT);
			if (value != null && !"".equals(value)) {//$NON-NLS-1$
				try {
					abstractCount= Integer.parseInt(value);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_ABSTRACT));
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_ABSTRACT));
			value= extended.getAttribute(ATTRIBUTE_DELETE);
			if (value != null && !"".equals(value)) {//$NON-NLS-1$
				try {
					deleteCount= Integer.parseInt(value);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DELETE));
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DELETE));
			value= extended.getAttribute(ATTRIBUTE_PULL);
			if (value != null && !"".equals(value)) {//$NON-NLS-1$
				try {
					pullCount= Integer.parseInt(value);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_PULL));
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_PULL));
			final RefactoringStatus status= new RefactoringStatus();
			List elements= new ArrayList();
			for (int index= 0; index < pullCount; index++) {
				final String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (index + 1);
				handle= extended.getAttribute(attribute);
				if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
					final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
					if (element == null || !element.exists())
						status.merge(ScriptableRefactoring.createInputWarningStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.PULL_UP));
					else
						elements.add(element);
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
			}
			fMembersToMove= (IMember[]) elements.toArray(new IMember[elements.size()]);
			elements= new ArrayList();
			for (int index= 0; index < deleteCount; index++) {
				final String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (pullCount + index + 1);
				handle= extended.getAttribute(attribute);
				if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
					final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
					if (element == null || !element.exists())
						status.merge(ScriptableRefactoring.createInputWarningStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.PULL_UP));
					else
						elements.add(element);
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
			}
			fDeletedMethods= (IFunction[]) elements.toArray(new IFunction[elements.size()]);
			elements= new ArrayList();
			for (int index= 0; index < abstractCount; index++) {
				final String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (pullCount + abstractCount + index + 1);
				handle= extended.getAttribute(attribute);
				if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
					final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
					if (element == null || !element.exists())
						status.merge(ScriptableRefactoring.createInputWarningStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.PULL_UP));
					else
						elements.add(element);
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
			}
			fAbstractMethods= (IFunction[]) elements.toArray(new IFunction[elements.size()]);
			IJavaScriptProject project= null;
			if (fMembersToMove.length > 0)
				project= fMembersToMove[0].getJavaScriptProject();
			fSettings= JavaPreferencesSettings.getCodeGenerationSettings(project);
			if (!status.isOK())
				return status;
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isPullUpAvailable(fMembersToMove);
	}

	private boolean isAvailableInDestination(final IFunction method, final IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			final IType destination= getDestinationType();
			final IFunction first= JavaModelUtil.findMethod(method.getElementName(), method.getParameterTypes(), false, destination);
			if (first != null && MethodChecks.isVirtual(first))
				return true;
			final ITypeHierarchy hierarchy= getDestinationTypeHierarchy(monitor);
			final IFunction found= JavaModelUtil.findMethodInHierarchy(hierarchy, destination, method.getElementName(), method.getParameterTypes(), false);
			return found != null && MethodChecks.isVirtual(found);
		} finally {
			monitor.done();
		}
	}

	private boolean isRequiredPullableMember(final List queue, final IMember member) throws JavaScriptModelException {
		final IType declaring= member.getDeclaringType();
		if (declaring == null) // not a member
			return false;
		return declaring.equals(getDeclaringType()) && !queue.contains(member) && RefactoringAvailabilityTester.isPullUpAvailable(member);
	}

	protected void registerChanges(final TextEditBasedChangeManager manager) throws CoreException {
		IJavaScriptUnit unit= null;
		CompilationUnitRewrite rewrite= null;
		for (final Iterator iterator= fCompilationUnitRewrites.keySet().iterator(); iterator.hasNext();) {
			unit= (IJavaScriptUnit) iterator.next();
			rewrite= (CompilationUnitRewrite) fCompilationUnitRewrites.get(unit);
			if (rewrite != null) {
				final CompilationUnitChange change= rewrite.createChange();
				if (change != null)
					manager.manage(unit, change);
			}
		}
	}

	/**
	 * Resets the environment before the first wizard page becomes visible.
	 */
	public void resetEnvironment() {
		IJavaScriptUnit unit= getDeclaringType().getJavaScriptUnit();
		if (fLayer)
			unit= unit.findWorkingCopy(fOwner);
		resetWorkingCopies(unit);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void rewriteTypeOccurrences(final TextEditBasedChangeManager manager, final ASTRequestor requestor, final CompilationUnitRewrite rewrite, final IJavaScriptUnit unit, final JavaScriptUnit node, final Set replacements, final IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 100); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
			CompilationUnitRewrite currentRewrite= null;
			final CompilationUnitRewrite existingRewrite= (CompilationUnitRewrite) fCompilationUnitRewrites.get(unit.getPrimary());
			final boolean isTouched= existingRewrite != null;
			if (isTouched)
				currentRewrite= existingRewrite;
			else
				currentRewrite= new CompilationUnitRewrite(unit, node);
			final Collection collection= (Collection) fTypeOccurrences.get(unit);
			if (collection != null && !collection.isEmpty()) {
				final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 100);
				try {
					subMonitor.beginTask("", collection.size() * 10); //$NON-NLS-1$
					subMonitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
					TType estimate= null;
					ISourceConstraintVariable variable= null;
					ITypeConstraintVariable constraint= null;
					for (final Iterator iterator= collection.iterator(); iterator.hasNext();) {
						variable= (ISourceConstraintVariable) iterator.next();
						if (variable instanceof ITypeConstraintVariable) {
							constraint= (ITypeConstraintVariable) variable;
							estimate= (TType) constraint.getData(SuperTypeConstraintsSolver.DATA_TYPE_ESTIMATE);
							if (estimate != null) {
								final CompilationUnitRange range= constraint.getRange();
								if (isTouched)
									rewriteTypeOccurrence(range, estimate, requestor, currentRewrite, node, replacements, currentRewrite.createCategorizedGroupDescription(RefactoringCoreMessages.SuperTypeRefactoringProcessor_update_type_occurrence, SET_SUPER_TYPE));
								else {
									final ASTNode result= NodeFinder.perform(node, range.getSourceRange());
									if (result != null)
										rewriteTypeOccurrence(estimate, currentRewrite, result, currentRewrite.createCategorizedGroupDescription(RefactoringCoreMessages.SuperTypeRefactoringProcessor_update_type_occurrence, SET_SUPER_TYPE));
								}
								subMonitor.worked(10);
							}
						}
					}
				} finally {
					subMonitor.done();
				}
			}
			if (!isTouched) {
				final TextChange change= currentRewrite.createChange();
				if (change != null)
					manager.manage(unit, change);
			}
		} finally {
			monitor.done();
		}
	}

	protected void rewriteTypeOccurrences(final TextEditBasedChangeManager manager, final CompilationUnitRewrite sourceRewrite, final IJavaScriptUnit copy, final Set replacements, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 100); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.PullUpRefactoring_checking);
			final IType declaring= getDeclaringType();
			final IJavaScriptProject project= declaring.getJavaScriptProject();
			final ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setWorkingCopyOwner(fOwner);
			parser.setResolveBindings(true);
			parser.setProject(project);
			parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
			parser.createASTs(new IJavaScriptUnit[] { copy}, new String[0], new ASTRequestor() {

				public final void acceptAST(final IJavaScriptUnit unit, final JavaScriptUnit node) {
					try {
						final IType subType= (IType) JavaModelUtil.findInCompilationUnit(unit, declaring);
						final AbstractTypeDeclaration subDeclaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(subType, node);
						if (subDeclaration != null) {
							final ITypeBinding subBinding= subDeclaration.resolveBinding();
							if (subBinding != null) {
								String name= null;
								ITypeBinding superBinding= null;
								final ITypeBinding[] superBindings= Bindings.getAllSuperTypes(subBinding);
								for (int index= 0; index < superBindings.length; index++) {
									name= superBindings[index].getName();
									if (name.startsWith(fDestinationType.getElementName()))
										superBinding= superBindings[index];
								}
								if (superBinding != null) {
									solveSuperTypeConstraints(unit, node, subType, subBinding, superBinding, new SubProgressMonitor(monitor, 80), status);
									if (!status.hasFatalError())
										rewriteTypeOccurrences(manager, this, sourceRewrite, unit, node, replacements, status, new SubProgressMonitor(monitor, 120));
								}
							}
						}
					} catch (JavaScriptModelException exception) {
						JavaScriptPlugin.log(exception);
						status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
					}
				}

				public final void acceptBinding(final String key, final IBinding binding) {
					// Do nothing
				}
			}, new NullProgressMonitor());
		} finally {
			monitor.done();
		}
	}

	/**
	 * Sets the methods to declare abstract.
	 * 
	 * @param methods
	 *            the methods to declare abstract
	 */
	public void setAbstractMethods(final IFunction[] methods) {
		Assert.isNotNull(methods);
		fAbstractMethods= methods;
	}

	/**
	 * Determines whether to create method stubs for non-implemented abstract
	 * methods.
	 * 
	 * @param create
	 *            <code>true</code> to create stubs, <code>false</code>
	 *            otherwise
	 */
	public void setCreateMethodStubs(final boolean create) {
		fCreateMethodStubs= create;
	}

	/**
	 * Sets the methods to delete
	 * 
	 * @param methods
	 *            the methods to delete
	 */
	public void setDeletedMethods(final IFunction[] methods) {
		Assert.isNotNull(methods);
		fDeletedMethods= methods;
	}

	/**
	 * Sets the destination type.
	 * 
	 * @param type
	 *            the destination type
	 */
	public void setDestinationType(final IType type) {
		Assert.isNotNull(type);
		if (!type.equals(fDestinationType))
			fCachedDestinationTypeHierarchy= null;
		fDestinationType= type;
	}

	/**
	 * Sets the members to move.
	 * 
	 * @param members
	 *            the members to move
	 */
	public void setMembersToMove(final IMember[] members) {
		Assert.isNotNull(members);
		fMembersToMove= (IMember[]) SourceReferenceUtil.sortByOffset(members);
	}
}
