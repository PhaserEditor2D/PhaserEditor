/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayCreation;
import org.eclipse.wst.jsdt.core.dom.ArrayInitializer;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.Corext;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.ImportReferencesCollector;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.dom.fragments.ASTFragmentFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.fragments.IExpressionFragment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.IRefactoringSearchRequestor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.wst.jsdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TightSourceRangeComputer;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class InlineConstantRefactoring extends ScriptableRefactoring {

	private static final String ATTRIBUTE_REPLACE= "replace"; //$NON-NLS-1$
	private static final String ATTRIBUTE_REMOVE= "remove";	 //$NON-NLS-1$

	private static class InlineTargetCompilationUnit {

		private static class InitializerTraversal extends HierarchicalASTVisitor {
		
			private static boolean areInSameType(ASTNode one, ASTNode other) {
				ASTNode onesContainer= getContainingTypeDeclaration(one);
				ASTNode othersContainer= getContainingTypeDeclaration(other);
		
				if (onesContainer == null || othersContainer == null)
					return false;
		
				ITypeBinding onesContainerBinding= getTypeBindingForTypeDeclaration(onesContainer);
				ITypeBinding othersContainerBinding= getTypeBindingForTypeDeclaration(othersContainer);
		
				Assert.isNotNull(onesContainerBinding);
				Assert.isNotNull(othersContainerBinding);
		
				String onesKey= onesContainerBinding.getKey();
				String othersKey= othersContainerBinding.getKey();
		
				if (onesKey == null || othersKey == null)
					return false;
		
				return onesKey.equals(othersKey);
			}
		
			private static boolean isStaticAccess(SimpleName memberName) {
				IBinding binding= memberName.resolveBinding();
				Assert.isTrue(binding instanceof IVariableBinding || binding instanceof IFunctionBinding || binding instanceof ITypeBinding);
		
				if (binding instanceof ITypeBinding)
					return true;
		
				if (binding instanceof IVariableBinding)
					return ((IVariableBinding) binding).isField();
		
				int modifiers= binding.getModifiers();
				return Modifier.isStatic(modifiers);
			}
		
			private static ASTNode getContainingTypeDeclaration(ASTNode node) {
				while (node != null && !(node instanceof AbstractTypeDeclaration) && !(node instanceof AnonymousClassDeclaration)) {
					node= node.getParent();
				}
				return node;
			}
		
			private static ITypeBinding getTypeBindingForTypeDeclaration(ASTNode declaration) {
				if (declaration instanceof AnonymousClassDeclaration)
					return ((AnonymousClassDeclaration) declaration).resolveBinding();
		
				if (declaration instanceof AbstractTypeDeclaration)
					return ((AbstractTypeDeclaration) declaration).resolveBinding();
		
				Assert.isTrue(false);
				return null;
			}
		
			private final Expression fInitializer;
			private ASTRewrite fInitializerRewrite;
			private final HashSet fStaticImportsInInitializer2;
		
			// cache:
			private Set fNamesDeclaredLocallyAtNewLocation;
		
			private final Expression fNewLocation;
			private final HashSet fStaticImportsInReference;
			private final CompilationUnitRewrite fNewLocationCuRewrite;
		
			public InitializerTraversal(Expression initializer, HashSet staticImportsInInitializer, Expression newLocation, HashSet staticImportsInReference, CompilationUnitRewrite newLocationCuRewrite) {
				fInitializer= initializer;
				fInitializerRewrite= ASTRewrite.create(initializer.getAST());
				fStaticImportsInInitializer2= staticImportsInInitializer;
				
				fNewLocation= newLocation;
				fStaticImportsInReference= staticImportsInReference;
				fNewLocationCuRewrite= newLocationCuRewrite;
		
				perform(initializer);
			}
		
			/**
			 * @param scope not a TypeDeclaration
			 * @return Set containing Strings representing simple names
			 */
			private Set getLocallyDeclaredNames(BodyDeclaration scope) {
				Assert.isTrue(!(scope instanceof AbstractTypeDeclaration));
		
				final Set result= new HashSet();
		
				if (scope instanceof FieldDeclaration)
					return result;
		
				scope.accept(new HierarchicalASTVisitor() {
		
					public boolean visit(AbstractTypeDeclaration node) {
						Assert.isTrue(node.getParent() instanceof TypeDeclarationStatement);
		
						result.add(node.getName().getIdentifier());
						return false;
					}
		
					public boolean visit(AnonymousClassDeclaration anonDecl) {
						return false;
					}
		
					public boolean visit(VariableDeclaration varDecl) {
						result.add(varDecl.getName().getIdentifier());
						return false;
					}
				});
				return result;
			}
		
			public ASTRewrite getInitializerRewrite() {
				return fInitializerRewrite;
			}
		
			private void perform(Expression initializer) {
				initializer.accept(this);
				if (initializer instanceof FunctionInvocation || initializer instanceof SuperMethodInvocation) {
					addExplicitTypeArgumentsIfNecessary(initializer);
				}
			}
			
			private void addExplicitTypeArgumentsIfNecessary(Expression invocation) {
				if (Invocations.isResolvedTypeInferredFromExpectedType(invocation)) {
					ASTNode referenceContext= fNewLocation.getParent();
					if (! (referenceContext instanceof VariableDeclarationFragment
							|| referenceContext instanceof SingleVariableDeclaration
							|| referenceContext instanceof Assignment)) {
						Invocations.resolveBinding(invocation);
					}
				}
			}
			
			public boolean visit(FieldAccess fieldAccess) {
				fieldAccess.getExpression().accept(this);
				return false;
			}
		
			public boolean visit(FunctionInvocation invocation) {
				if (invocation.getExpression() == null)
					qualifyUnqualifiedMemberNameIfNecessary(invocation.getName());
				else
					invocation.getExpression().accept(this);
		
				for (Iterator it= invocation.arguments().iterator(); it.hasNext();)
					((Expression) it.next()).accept(this);
		
				return false;
			}
		
			public boolean visit(Name name) {
				SimpleName leftmost= getLeftmost(name);
		
				IBinding leftmostBinding= leftmost.resolveBinding();
				if (leftmostBinding instanceof IVariableBinding || leftmostBinding instanceof IFunctionBinding || leftmostBinding instanceof ITypeBinding) {
					if (shouldUnqualify(leftmost))
						unqualifyMemberName(leftmost);
					else
						qualifyUnqualifiedMemberNameIfNecessary(leftmost);
				}
		
				if (leftmostBinding instanceof ITypeBinding) {
					String addedImport= fNewLocationCuRewrite.getImportRewrite().addImport((ITypeBinding) leftmostBinding);
					fNewLocationCuRewrite.getImportRemover().registerAddedImport(addedImport);
				}
				
				return false;
			}
			
			private void qualifyUnqualifiedMemberNameIfNecessary(SimpleName memberName) {
				if (shouldQualify(memberName))
					qualifyMemberName(memberName);
			}
		
			private boolean shouldUnqualify(SimpleName memberName) {
				if (areInSameType(memberName, fNewLocation))
					return ! mayBeShadowedByLocalDeclaration(memberName);
				
				return false;
			}
		
			private void unqualifyMemberName(SimpleName memberName) {
				if (doesParentQualify(memberName))
					fInitializerRewrite.replace(memberName.getParent(), memberName, null);
			}

			private boolean shouldQualify(SimpleName memberName) {
				if (! areInSameType(fInitializer, fNewLocation))
					return true;
		
				return mayBeShadowedByLocalDeclaration(memberName);
			}
			
			private boolean mayBeShadowedByLocalDeclaration(SimpleName memberName) {
				return getNamesDeclaredLocallyAtNewLocation().contains(memberName.getIdentifier());
			}
		
			private Set getNamesDeclaredLocallyAtNewLocation() {
				if (fNamesDeclaredLocallyAtNewLocation != null)
					return fNamesDeclaredLocallyAtNewLocation;
		
				BodyDeclaration enclosingBodyDecl= (BodyDeclaration) ASTNodes.getParent(fNewLocation, BodyDeclaration.class);
				Assert.isTrue(!(enclosingBodyDecl instanceof AbstractTypeDeclaration));
		
				return fNamesDeclaredLocallyAtNewLocation= getLocallyDeclaredNames(enclosingBodyDecl);
			}
		
			private void qualifyMemberName(SimpleName memberName) {
				if (isStaticAccess(memberName)) {
					IBinding memberBinding= memberName.resolveBinding();
					
					if (memberBinding instanceof IVariableBinding || memberBinding instanceof IFunctionBinding) {
						if (fStaticImportsInReference.contains(fNewLocation)) { // use static import if reference location used static import
							importStatically(memberName, memberBinding);
							return;
						} else if (fStaticImportsInInitializer2.contains(memberName)) { // use static import if already imported statically in initializer
							importStatically(memberName, memberBinding);
							return;
						}
					}
					qualifyToTopLevelClass(memberName); //otherwise: qualify and import non-static
				}
			}
		
			private void importStatically(SimpleName toImport, IBinding binding) {
				String newName= fNewLocationCuRewrite.getImportRewrite().addStaticImport(binding);
				fNewLocationCuRewrite.getImportRemover().registerAddedStaticImport(binding);
				
				Name newReference= ASTNodeFactory.newName(fInitializerRewrite.getAST(), newName);
				fInitializerRewrite.replace(toImport, newReference, null);
			}
			
			private void qualifyToTopLevelClass(SimpleName toQualify) {
				ITypeBinding declaringClass= getDeclaringClassBinding(toQualify);
				if (declaringClass == null)
					return;
				
				Type newQualification= fNewLocationCuRewrite.getImportRewrite().addImport(declaringClass, fInitializerRewrite.getAST());
				fNewLocationCuRewrite.getImportRemover().registerAddedImports(newQualification);
				
				SimpleName newToQualify= (SimpleName) fInitializerRewrite.createMoveTarget(toQualify);
				Type newType= fInitializerRewrite.getAST().newQualifiedType(newQualification, newToQualify);
				fInitializerRewrite.replace(toQualify, newType, null);
			}
			
			private static ITypeBinding getDeclaringClassBinding(SimpleName memberName) {
		
				IBinding binding= memberName.resolveBinding();
				if (binding instanceof IFunctionBinding)
					return ((IFunctionBinding) binding).getDeclaringClass();
		
				if (binding instanceof IVariableBinding)
					return ((IVariableBinding) binding).getDeclaringClass();
		
				if (binding instanceof ITypeBinding)
					return ((ITypeBinding) binding).getDeclaringClass();
		
				Assert.isTrue(false);
				return null;
		
			}
		
		}
		
		private final Expression fInitializer;
		private final IJavaScriptUnit fInitializerUnit;
		private final VariableDeclarationFragment fOriginalDeclaration; 
		
		/** The references in this compilation unit, represented as AST Nodes in the parsed representation of the compilation unit */
		private final Expression[] fReferences;
		private final VariableDeclarationFragment fDeclarationToRemove;
		private final CompilationUnitRewrite fCuRewrite;
		private final TightSourceRangeComputer fSourceRangeComputer;
		private final HashSet fStaticImportsInInitializer;
		private final boolean fIs15;
		
		private InlineTargetCompilationUnit(CompilationUnitRewrite cuRewrite, Name[] references, InlineConstantRefactoring refactoring, HashSet staticImportsInInitializer) throws JavaScriptModelException {
			fInitializer= refactoring.getInitializer();
			fInitializerUnit= refactoring.getDeclaringCompilationUnit();
			
			fCuRewrite= cuRewrite;
			fSourceRangeComputer= new TightSourceRangeComputer();
			fCuRewrite.getASTRewrite().setTargetSourceRangeComputer(fSourceRangeComputer);
			if (refactoring.getRemoveDeclaration() && refactoring.getReplaceAllReferences() && cuRewrite.getCu().equals(fInitializerUnit))
				fDeclarationToRemove= refactoring.getDeclaration();
			else
				fDeclarationToRemove= null;
			
			fOriginalDeclaration= refactoring.getDeclaration();
			
			fReferences= new Expression[references.length];
			for (int i= 0; i < references.length; i++)
				fReferences[i]= getQualifiedReference(references[i]);
			
			fIs15= JavaModelUtil.is50OrHigher(cuRewrite.getCu().getJavaScriptProject());
			fStaticImportsInInitializer= fIs15 ? staticImportsInInitializer : new HashSet(0);
		}

		private static Expression getQualifiedReference(Name fieldName) {
			if (doesParentQualify(fieldName))
				return (Expression) fieldName.getParent();

			return fieldName;
		}

		private static boolean doesParentQualify(Name fieldName) {
			ASTNode parent= fieldName.getParent();
			Assert.isNotNull(parent);

			if (parent instanceof FieldAccess && ((FieldAccess) parent).getName() == fieldName)
				return true;

			if (parent instanceof QualifiedName && ((QualifiedName) parent).getName() == fieldName)
				return true;

			if (parent instanceof FunctionInvocation && ((FunctionInvocation) parent).getName() == fieldName)
				return true;

			return false;
		}

		public CompilationUnitChange getChange() throws CoreException {
			for (int i= 0; i < fReferences.length; i++)
				inlineReference(fReferences[i]);
			
			removeConstantDeclarationIfNecessary();
			
			return fCuRewrite.createChange();
		}

		private void inlineReference(Expression reference) throws CoreException {
			ASTNode importDecl= ASTNodes.getParent(reference, ImportDeclaration.class);
			if (importDecl != null)
				return; // don't inline into static imports

			String modifiedInitializer= prepareInitializerForLocation(reference);
			if (modifiedInitializer == null)
				return;

			TextEditGroup msg= fCuRewrite.createGroupDescription(RefactoringCoreMessages.InlineConstantRefactoring_Inline); 
			Expression newReference= (Expression) fCuRewrite.getASTRewrite().createStringPlaceholder(modifiedInitializer, reference.getNodeType());

			if (fInitializer instanceof ArrayInitializer) {
				ArrayCreation arrayCreation= fCuRewrite.getAST().newArrayCreation();
				ArrayType arrayType= (ArrayType) ASTNodeFactory.newType(fCuRewrite.getAST(), fOriginalDeclaration);
				arrayCreation.setType(arrayType);

				ArrayInitializer newArrayInitializer= (ArrayInitializer) fCuRewrite.getASTRewrite().createStringPlaceholder(modifiedInitializer,
						ASTNode.ARRAY_INITIALIZER);
				arrayCreation.setInitializer(newArrayInitializer);
				newReference= arrayCreation;

				ITypeBinding typeToAddToImport= ASTNodes.getType(fOriginalDeclaration).resolveBinding();
				fCuRewrite.getImportRewrite().addImport(typeToAddToImport);
				fCuRewrite.getImportRemover().registerAddedImport(typeToAddToImport.getName());
			}

			if (shouldParenthesizeSubstitute(fInitializer, reference)) {
				ParenthesizedExpression parenthesized= fCuRewrite.getAST().newParenthesizedExpression();
				parenthesized.setExpression(newReference);
				newReference= parenthesized;
			}
			fCuRewrite.getASTRewrite().replace(reference, newReference, msg);
			fSourceRangeComputer.addTightSourceNode(reference);
			fCuRewrite.getImportRemover().registerRemovedNode(reference);
		}

		private String prepareInitializerForLocation(Expression location) throws CoreException {
			HashSet staticImportsInReference= new HashSet();
			final IJavaScriptProject project= fCuRewrite.getCu().getJavaScriptProject();
			if (fIs15)
				location.accept(new ImportReferencesCollector(project, null, new ArrayList(), staticImportsInReference));
			InitializerTraversal traversal= new InitializerTraversal(fInitializer, fStaticImportsInInitializer, location, staticImportsInReference, fCuRewrite);
			ASTRewrite initializerRewrite= traversal.getInitializerRewrite();
			IDocument document= new Document(fInitializerUnit.getBuffer().getContents()); // could reuse document when generating and applying undo edits
			
			final RangeMarker marker= new RangeMarker(fInitializer.getStartPosition(), fInitializer.getLength());
			TextEdit[] rewriteEdits= initializerRewrite.rewriteAST(document, fInitializerUnit.getJavaScriptProject().getOptions(true)).removeChildren();
			marker.addChildren(rewriteEdits);
			try {
				marker.apply(document, TextEdit.UPDATE_REGIONS);
				String rewrittenInitializer= document.get(marker.getOffset(), marker.getLength());
				IRegion region= document.getLineInformation(document.getLineOfOffset(marker.getOffset()));
				int oldIndent= Strings.computeIndentUnits(document.get(region.getOffset(), region.getLength()), project);
				return Strings.changeIndent(rewrittenInitializer, oldIndent, project, "", TextUtilities.getDefaultLineDelimiter(document)); //$NON-NLS-1$
			} catch (MalformedTreeException e) {
				JavaScriptPlugin.log(e);
			} catch (BadLocationException e) {
				JavaScriptPlugin.log(e);
			}
			return fInitializerUnit.getBuffer().getText(fInitializer.getStartPosition(), fInitializer.getLength());
		}

		private static boolean shouldParenthesizeSubstitute(Expression substitute, Expression location) {
			if (substitute instanceof Assignment) // for esthetic reasons
				return true;
			else
				return ASTNodes.substituteMustBeParenthesized(substitute, location);
		}
		
		private void removeConstantDeclarationIfNecessary() throws CoreException {
			if (fDeclarationToRemove == null)
				return;
			
			FieldDeclaration parentDeclaration= (FieldDeclaration) fDeclarationToRemove.getParent();
			ASTNode toRemove;
			if (parentDeclaration.fragments().size() == 1)
				toRemove= parentDeclaration;
			else
				toRemove= fDeclarationToRemove;

			TextEditGroup msg= fCuRewrite.createGroupDescription(RefactoringCoreMessages.InlineConstantRefactoring_remove_declaration); 
			fCuRewrite.getASTRewrite().remove(toRemove, msg);
			fCuRewrite.getImportRemover().registerRemovedNode(toRemove);
		}
	}

	// ---- End InlineTargetCompilationUnit ----------------------------------------------------------------------------------------------

	private static SimpleName getLeftmost(Name name) {
		if (name instanceof SimpleName)
			return (SimpleName) name;

		return getLeftmost(((QualifiedName) name).getQualifier());
	}

	private int fSelectionStart;
	private int fSelectionLength;
	
	private IJavaScriptUnit fSelectionCu;
	private CompilationUnitRewrite fSelectionCuRewrite;
	private Name fSelectedConstantName;
	
	private IField fField;
	private CompilationUnitRewrite fDeclarationCuRewrite;
	private VariableDeclarationFragment fDeclaration;
	private boolean fDeclarationSelected;
	private boolean fDeclarationSelectedChecked= false;
	private boolean fInitializerAllStaticFinal;
	private boolean fInitializerChecked= false;

	private boolean fRemoveDeclaration= false;
	private boolean fReplaceAllReferences= true;

	private CompilationUnitChange[] fChanges;
	
	/**
	 * Creates a new inline constant refactoring.
	 * <p>
	 * This constructor is only used by <code>DelegateCreator</code>.
	 * </p>
	 * 
	 * @param field the field to inline
	 */
	public InlineConstantRefactoring(IField field) {
		Assert.isNotNull(field);
		Assert.isTrue(!field.isBinary());
		fField= field;
	}

	/**
	 * Creates a new inline constant refactoring.
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param node the compilation unit node, or <code>null</code> if invoked by scripting
	 * @param selectionStart
	 * @param selectionLength
	 */
	public InlineConstantRefactoring(IJavaScriptUnit unit, JavaScriptUnit node, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		fSelectionCu= unit;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		if (unit != null)
			initialize(unit, node);
	}

	private void initialize(IJavaScriptUnit cu, JavaScriptUnit node) {
		fSelectionCuRewrite= new CompilationUnitRewrite(cu, node);
		fSelectedConstantName= findConstantNameNode();
	}

	private Name findConstantNameNode() {
		ASTNode node= NodeFinder.perform(fSelectionCuRewrite.getRoot(), fSelectionStart, fSelectionLength);
		if (node == null)
			return null;
		if (node instanceof FieldAccess)
			node= ((FieldAccess) node).getName();
		if (!(node instanceof Name))
			return null;
		Name name= (Name) node;
		IBinding binding= name.resolveBinding();
		if (!(binding instanceof IVariableBinding))
			return null;
		IVariableBinding variableBinding= (IVariableBinding) binding;
		if (!variableBinding.isField())
			return null;
		int modifiers= binding.getModifiers();
		if (! (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)))
			return null;

		return name;
	}

	public RefactoringStatus checkStaticFinalConstantNameSelected() {
		if (fSelectedConstantName == null)
			return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.InlineConstantRefactoring_static_final_field, null, Corext.getPluginId(), RefactoringStatusCodes.NOT_STATIC_FINAL_SELECTED, null); 

		return new RefactoringStatus();
	}

	public String getName() {
		return RefactoringCoreMessages.InlineConstantRefactoring_name; 
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 3); //$NON-NLS-1$

			if (!fSelectionCu.isStructureKnown())
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.InlineConstantRefactoring_syntax_errors, null, Corext.getPluginId(), RefactoringStatusCodes.SYNTAX_ERRORS, null); 

			RefactoringStatus result= checkStaticFinalConstantNameSelected();
			if (result.hasFatalError())
				return result;
			
			result.merge(findField());
			if (result.hasFatalError())
				return result;
			pm.worked(1);

			result.merge(findDeclaration());
			if (result.hasFatalError())
				return result;
			pm.worked(1);

			result.merge(checkInitializer());
			if (result.hasFatalError())
				return result;
			pm.worked(1);

			return result;
			
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus findField() throws JavaScriptModelException {
		fField= (IField) ((IVariableBinding) fSelectedConstantName.resolveBinding()).getJavaElement();
		if (fField != null && ! fField.exists())
			return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.InlineConstantRefactoring_local_anonymous_unsupported, null, Corext.getPluginId(), RefactoringStatusCodes.LOCAL_AND_ANONYMOUS_NOT_SUPPORTED, null); 
		
		return null;
	}
	private RefactoringStatus findDeclaration() throws JavaScriptModelException {
		fDeclarationSelectedChecked= true;
		fDeclarationSelected= false;
		ASTNode parent= fSelectedConstantName.getParent();
		if (parent instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment parentDeclaration= (VariableDeclarationFragment) parent;
			if (parentDeclaration.getName() == fSelectedConstantName) {
				fDeclarationSelected= true;
				fDeclarationCuRewrite= fSelectionCuRewrite;
				fDeclaration= (VariableDeclarationFragment) fSelectedConstantName.getParent();
				return null;
			}
		}
		
		VariableDeclarationFragment declaration= (VariableDeclarationFragment) fSelectionCuRewrite.getRoot().findDeclaringNode(fSelectedConstantName.resolveBinding());
		if (declaration != null) {
			fDeclarationCuRewrite= fSelectionCuRewrite;
			fDeclaration= declaration;
			return null;
		}

		if (fField.getJavaScriptUnit() == null)
			return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.InlineConstantRefactoring_binary_file, null, Corext.getPluginId(), RefactoringStatusCodes.DECLARED_IN_CLASSFILE, null); 
		
		fDeclarationCuRewrite= new CompilationUnitRewrite(fField.getJavaScriptUnit());
		fDeclaration= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(fField, fDeclarationCuRewrite.getRoot());
		return null;
	}

	private RefactoringStatus checkInitializer() {
		Expression initializer= getInitializer();
		if (initializer == null)
			return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.InlineConstantRefactoring_blank_finals, null, Corext.getPluginId(), RefactoringStatusCodes.CANNOT_INLINE_BLANK_FINAL, null); 

		fInitializerAllStaticFinal= ConstantChecks.isStaticFinalConstant((IExpressionFragment) ASTFragmentFactory.createFragmentForFullSubtree(initializer));
		fInitializerChecked= true;
		return new RefactoringStatus();
	}

	private VariableDeclarationFragment getDeclaration() throws JavaScriptModelException {
		return fDeclaration;
	}

	private Expression getInitializer() {
		return fDeclaration.getInitializer();
	}
	
	private IJavaScriptUnit getDeclaringCompilationUnit() throws JavaScriptModelException {
		return fField.getJavaScriptUnit();
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", 3); //$NON-NLS-1$
		
		try {
			List/*<CompilationUnitChange>*/changes= new ArrayList();
			HashSet staticImportsInInitializer= new HashSet();
			ImportReferencesCollector importReferencesCollector= new ImportReferencesCollector(fField.getJavaScriptProject(), null, new ArrayList(), staticImportsInInitializer);
			getInitializer().accept(importReferencesCollector);
			
			if (getReplaceAllReferences()) {
				SearchResultGroup[] searchResultGroups= findReferences(pm, result);
				for (int i= 0; i < searchResultGroups.length; i++) {
					if (pm.isCanceled())
						throw new OperationCanceledException();
					SearchResultGroup group= searchResultGroups[i];
					IJavaScriptUnit cu= group.getCompilationUnit();

					CompilationUnitRewrite cuRewrite= getCuRewrite(cu);
					Name[] references= extractReferenceNodes(group.getSearchResults(), cuRewrite.getRoot());
					InlineTargetCompilationUnit targetCompilationUnit= new InlineTargetCompilationUnit(
							cuRewrite, references, this, staticImportsInInitializer);
					changes.add(targetCompilationUnit.getChange());
				}

			} else {
				Assert.isTrue(! isDeclarationSelected());
				InlineTargetCompilationUnit targetForOnlySelectedReference= new InlineTargetCompilationUnit(
						fSelectionCuRewrite, new Name[] { fSelectedConstantName }, this, staticImportsInInitializer);
				changes.add(targetForOnlySelectedReference.getChange());
			}

			if (result.hasFatalError())
				return result;

			if (getRemoveDeclaration() && getReplaceAllReferences()) {
				boolean declarationRemoved= false;
				for (Iterator iter= changes.iterator(); iter.hasNext();) {
					CompilationUnitChange change= (CompilationUnitChange) iter.next();
					if (change.getCompilationUnit().equals(fDeclarationCuRewrite.getCu())) {
						declarationRemoved= true;
						break;
					}
				}
				if (! declarationRemoved) {
					InlineTargetCompilationUnit targetForDeclaration= new InlineTargetCompilationUnit(fDeclarationCuRewrite, new Name[0], this, staticImportsInInitializer);
					CompilationUnitChange change= targetForDeclaration.getChange();
					if (change != null)
						changes.add(change);
				}
			}

			IJavaScriptUnit[] cus= new IJavaScriptUnit[changes.size()];
			for (int i= 0; i < changes.size(); i++) {
				CompilationUnitChange change= (CompilationUnitChange) changes.get(i);
				cus[i]= change.getCompilationUnit();
			}
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(cus), getValidationContext()));

			pm.worked(1);

			fChanges= (CompilationUnitChange[]) changes.toArray(new CompilationUnitChange[changes.size()]);

			return result;
			
		} finally {
			fSelectionCuRewrite= null;
			fSelectedConstantName= null;
			fDeclarationCuRewrite= null;
			fDeclaration= null;
			pm.done();
		}
	}

	private Name[] extractReferenceNodes(SearchMatch[] searchResults, JavaScriptUnit cuNode) {
		Name[] references= new Name[searchResults.length];
		for (int i= 0; i < searchResults.length; i++)
			references[i]= (Name) NodeFinder.perform(cuNode, searchResults[i].getOffset(), searchResults[i].getLength());
		return references;
	}

	private CompilationUnitRewrite getCuRewrite(IJavaScriptUnit cu) {
		CompilationUnitRewrite cuRewrite;
		if (cu.equals(fSelectionCu)) {
			cuRewrite= fSelectionCuRewrite;
		} else if (cu.equals(fField.getJavaScriptUnit())) {
			cuRewrite= fDeclarationCuRewrite;
		} else {
			cuRewrite= new CompilationUnitRewrite(cu);
		}
		return cuRewrite;
	}

	private SearchResultGroup[] findReferences(IProgressMonitor pm, RefactoringStatus status) throws JavaScriptModelException {
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(fField, IJavaScriptSearchConstants.REFERENCES));
		engine.setFiltering(true, true);
		engine.setScope(RefactoringScopeFactory.create(fField));
		engine.setStatus(status);
		engine.setRequestor(new IRefactoringSearchRequestor() {
			public SearchMatch acceptSearchMatch(SearchMatch match) {
				return match.isInsideDocComment() ? null : match;
			}
		});
		engine.searchPattern(new SubProgressMonitor(pm, 1));
		return (SearchResultGroup[]) engine.getResults();
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.InlineConstantRefactoring_preview, 2);
			final Map arguments= new HashMap();
			String project= null;
			IJavaScriptProject javaProject= fSelectionCu.getJavaScriptProject();
			if (javaProject != null)
				project= javaProject.getElementName();
			int flags= RefactoringDescriptor.STRUCTURAL_CHANGE | JavaScriptRefactoringDescriptor.JAR_REFACTORING | JavaScriptRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
			try {
				if (!Flags.isPrivate(fField.getFlags()))
					flags|= RefactoringDescriptor.MULTI_CHANGE;
			} catch (JavaScriptModelException exception) {
				JavaScriptPlugin.log(exception);
			}
			final String description= Messages.format(RefactoringCoreMessages.InlineConstantRefactoring_descriptor_description_short, fField.getElementName());
			final String header= Messages.format(RefactoringCoreMessages.InlineConstantRefactoring_descriptor_description, new String[] { JavaScriptElementLabels.getElementLabel(fField, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getElementLabel(fField.getParent(), JavaScriptElementLabels.ALL_FULLY_QUALIFIED)});
			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
			comment.addSetting(Messages.format(RefactoringCoreMessages.InlineConstantRefactoring_original_pattern, JavaScriptElementLabels.getElementLabel(fField, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)));
			if (fRemoveDeclaration)
				comment.addSetting(RefactoringCoreMessages.InlineConstantRefactoring_remove_declaration);
			if (fReplaceAllReferences)
				comment.addSetting(RefactoringCoreMessages.InlineConstantRefactoring_replace_references);
			final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(IJavaScriptRefactorings.INLINE_CONSTANT, project, description, comment.asString(), arguments, flags);
			arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fSelectionCu));
			arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_SELECTION, Integer.valueOf(fSelectionStart).toString() + " " + Integer.valueOf(fSelectionLength).toString()); //$NON-NLS-1$
			arguments.put(ATTRIBUTE_REMOVE, Boolean.valueOf(fRemoveDeclaration).toString());
			arguments.put(ATTRIBUTE_REPLACE, Boolean.valueOf(fReplaceAllReferences).toString());
			return new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.InlineConstantRefactoring_inline, fChanges);
		} finally {
			pm.done();
			fChanges= null;
		}
	}

	private void checkInvariant() {
		if (isDeclarationSelected())
			Assert.isTrue(fReplaceAllReferences);
	}

	public boolean getRemoveDeclaration() {
		return fRemoveDeclaration;
	}

	public boolean getReplaceAllReferences() {
		checkInvariant();
		return fReplaceAllReferences;
	}

	public boolean isDeclarationSelected() {
		Assert.isTrue(fDeclarationSelectedChecked);
		return fDeclarationSelected;
	}

	public boolean isInitializerAllStaticFinal() {
		Assert.isTrue(fInitializerChecked);
		return fInitializerAllStaticFinal;
	}

	public void setRemoveDeclaration(boolean removeDeclaration) {
		fRemoveDeclaration= removeDeclaration;
	}

	public void setReplaceAllReferences(boolean replaceAllReferences) {
		fReplaceAllReferences= replaceAllReferences;
		checkInvariant();
	}

	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String selection= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_SELECTION);
			if (selection != null) {
				int offset= -1;
				int length= -1;
				final StringTokenizer tokenizer= new StringTokenizer(selection);
				if (tokenizer.hasMoreTokens())
					offset= Integer.valueOf(tokenizer.nextToken()).intValue();
				if (tokenizer.hasMoreTokens())
					length= Integer.valueOf(tokenizer.nextToken()).intValue();
				if (offset >= 0 && length >= 0) {
					fSelectionStart= offset;
					fSelectionLength= length;
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new Object[] { selection, JDTRefactoringDescriptor.ATTRIBUTE_SELECTION}));
			}
			final String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists())
					return createInputFatalStatus(element, IJavaScriptRefactorings.INLINE_CONSTANT);
				else {
					if (element instanceof IJavaScriptUnit) {
						fSelectionCu= (IJavaScriptUnit) element;
						if (selection == null)
							return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_SELECTION));
					} else if (element instanceof IField) {
						final IField field= (IField) element;
						try {
							final ISourceRange range= field.getNameRange();
							if (range != null) {
								fSelectionStart= range.getOffset();
								fSelectionLength= range.getLength();
							} else
								return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, IJavaScriptRefactorings.INLINE_CONSTANT));
						} catch (JavaScriptModelException exception) {
							return createInputFatalStatus(element, IJavaScriptRefactorings.INLINE_CONSTANT);
						}
						fSelectionCu= field.getJavaScriptUnit();
					} else
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new Object[] { handle, JDTRefactoringDescriptor.ATTRIBUTE_INPUT}));
					final ASTParser parser= ASTParser.newParser(AST.JLS3);
					parser.setResolveBindings(true);
					parser.setSource(fSelectionCu);
					final JavaScriptUnit unit= (JavaScriptUnit) parser.createAST(null);
					initialize(fSelectionCu, unit);
					if (checkStaticFinalConstantNameSelected().hasFatalError())
						return createInputFatalStatus(element, IJavaScriptRefactorings.INLINE_CONSTANT);
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String replace= extended.getAttribute(ATTRIBUTE_REPLACE);
			if (replace != null) {
				fReplaceAllReferences= Boolean.valueOf(replace).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_REPLACE));
			final String remove= extended.getAttribute(ATTRIBUTE_REMOVE);
			if (remove != null)
				fRemoveDeclaration= Boolean.valueOf(remove).booleanValue();
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_REMOVE));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}
}
