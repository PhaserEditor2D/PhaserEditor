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
package org.eclipse.wst.jsdt.internal.corext.refactoring.structure.constraints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.BindingKey;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.ASTRequestor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.formatter.CodeFormatter;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.wst.jsdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.ICommentProvider;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.types.TypeEnvironment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.CodeGeneration;

/**
 * Partial implementation of a refactoring processor solving supertype
 * constraint models.
 * 
 * 
 */
public abstract class SuperTypeRefactoringProcessor extends RefactoringProcessor implements IScriptableRefactoring, ICommentProvider {

	// TODO: remove
	protected static final String ATTRIBUTE_INSTANCEOF= "instanceof"; //$NON-NLS-1$

	// TODO: remove
	protected static final String ATTRIBUTE_REPLACE= "replace"; //$NON-NLS-1$

	/** The super type group category set */
	protected static final GroupCategorySet SET_SUPER_TYPE= new GroupCategorySet(new GroupCategory("org.eclipse.wst.jsdt.internal.corext.superType", //$NON-NLS-1$
			RefactoringCoreMessages.SuperTypeRefactoringProcessor_category_name, RefactoringCoreMessages.SuperTypeRefactoringProcessor_category_description));

	/** Number of compilation units to parse at once */
	private static final int SIZE_BATCH= 500;

	/**
	 * Returns a new ast node corresponding to the given type.
	 * 
	 * @param rewrite
	 *            the compilation unit rewrite to use
	 * @param type
	 *            the specified type
	 * @return A corresponding ast node
	 */
	protected static ASTNode createCorrespondingNode(final CompilationUnitRewrite rewrite, final TType type) {
		return rewrite.getImportRewrite().addImportFromSignature(new BindingKey(type.getBindingKey()).toSignature(), rewrite.getAST());
	}

	/** The comment */
	protected String fComment;

	/** Should type occurrences on instanceof's also be rewritten? */
	protected boolean fInstanceOf= false;

	/**
	 * The obsolete casts (element type:
	 * <code>&ltICompilationUnit, Collection&ltCastVariable2&gt&gt</code>)
	 */
	protected Map fObsoleteCasts= null;

	/** The working copy owner */
	protected final WorkingCopyOwner fOwner= new WorkingCopyOwner() {
	};

	/** Should occurrences of the type be replaced by the supertype? */
	protected boolean fReplace= false;

	/** The code generation settings, or <code>null</code> */
	protected CodeGenerationSettings fSettings;

	/** The static bindings to import */
	protected final Set fStaticBindings= new HashSet();

	/** The type bindings to import */
	protected final Set fTypeBindings= new HashSet();

	/**
	 * The type occurrences (element type:
	 * <code>&ltICompilationUnit, Collection&ltIDeclaredConstraintVariable&gt&gt</code>)
	 */
	protected Map fTypeOccurrences= null;

	/**
	 * Creates a new supertype refactoring processor.
	 * 
	 * @param settings
	 *            the code generation settings, or <code>null</code>
	 */
	protected SuperTypeRefactoringProcessor(final CodeGenerationSettings settings) {
		fSettings= settings;
	}

	/**
	 * Adds the refactoring settings to the specified comment.
	 * 
	 * @param comment
	 *            the java refactoring descriptor comment
	 * @param addUseSupertype
	 *            <code>true</code> to add the use supertype setting,
	 *            <code>false</code> otherwise
	 */
	protected void addSuperTypeSettings(final JDTRefactoringDescriptorComment comment, final boolean addUseSupertype) {
		Assert.isNotNull(comment);
		if (fReplace) {
			if (addUseSupertype)
				comment.addSetting(RefactoringCoreMessages.SuperTypeRefactoringProcessor_user_supertype_setting);
			if (fInstanceOf)
				comment.addSetting(RefactoringCoreMessages.SuperTypeRefactoringProcessor_use_in_instanceof_setting);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canEnableComment() {
		return true;
	}

	/**
	 * Creates the super type constraint solver to solve the model.
	 * 
	 * @param model
	 *            the model to create a solver for
	 * @return The created super type constraint solver
	 */
	protected abstract SuperTypeConstraintsSolver createContraintSolver(SuperTypeConstraintsModel model);

	/**
	 * Creates the declarations of the new supertype members.
	 * 
	 * @param sourceRewrite
	 *            the source compilation unit rewrite
	 * @param targetRewrite
	 *            the target rewrite
	 * @param targetDeclaration
	 *            the target type declaration
	 * @throws CoreException
	 *             if a buffer could not be retrieved
	 */
	protected void createMemberDeclarations(CompilationUnitRewrite sourceRewrite, ASTRewrite targetRewrite, AbstractTypeDeclaration targetDeclaration) throws CoreException {
		// Do nothing
	}

	/**
	 * Creates the declaration of the new supertype, excluding any comments or
	 * package declaration.
	 * 
	 * @param sourceRewrite
	 *            the source compilation unit rewrite
	 * @param subType
	 *            the subtype
	 * @param superName
	 *            the name of the supertype
	 * @param sourceDeclaration
	 *            the type declaration of the source type
	 * @param buffer
	 *            the string buffer containing the declaration
	 * @param isInterface
	 *            <code>true</code> if the type declaration is an interface,
	 *            <code>false</code> otherwise
	 * @param status
	 *            the refactoring status
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if an error occurs
	 */
	protected final void createTypeDeclaration(final CompilationUnitRewrite sourceRewrite, final IType subType, final String superName, final AbstractTypeDeclaration sourceDeclaration, final StringBuffer buffer, boolean isInterface, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(subType);
		Assert.isNotNull(superName);
		Assert.isNotNull(sourceDeclaration);
		Assert.isNotNull(buffer);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 100); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
			final String delimiter= StubUtility.getLineDelimiterUsed(subType.getJavaScriptProject());
			if (JdtFlags.isPublic(subType)) {
				buffer.append(JdtFlags.VISIBILITY_STRING_PUBLIC);
				buffer.append(" "); //$NON-NLS-1$
			}
			if (isInterface)
				buffer.append("interface "); //$NON-NLS-1$
			else
				buffer.append("class "); //$NON-NLS-1$
			buffer.append(superName);
			buffer.append(" {"); //$NON-NLS-1$
			buffer.append(delimiter);
			buffer.append(delimiter);
			buffer.append('}');
			final IDocument document= new Document(buffer.toString());
			final ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setSource(document.get().toCharArray());
			final JavaScriptUnit unit= (JavaScriptUnit) parser.createAST(new SubProgressMonitor(monitor, 100));
			final ASTRewrite targetRewrite= ASTRewrite.create(unit.getAST());
			final AbstractTypeDeclaration targetDeclaration= (AbstractTypeDeclaration) unit.types().get(0);
			createMemberDeclarations(sourceRewrite, targetRewrite, targetDeclaration);
			final TextEdit edit= targetRewrite.rewriteAST(document, subType.getJavaScriptProject().getOptions(true));
			try {
				edit.apply(document, TextEdit.UPDATE_REGIONS);
			} catch (MalformedTreeException exception) {
				JavaScriptPlugin.log(exception);
			} catch (BadLocationException exception) {
				JavaScriptPlugin.log(exception);
			}
			buffer.setLength(0);
			buffer.append(document.get());
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the necessary imports for the extracted supertype.
	 * 
	 * @param unit
	 *            the working copy of the new supertype
	 * @param monitor
	 *            the progress monitor to use
	 * @return the generated import declaration
	 * @throws CoreException
	 *             if the imports could not be generated
	 */
	protected final String createTypeImports(final IJavaScriptUnit unit, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(unit);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 100); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
			final ImportRewrite rewrite= StubUtility.createImportRewrite(unit, true);
			ITypeBinding type= null;
			for (final Iterator iterator= fTypeBindings.iterator(); iterator.hasNext();) {
				type= (ITypeBinding) iterator.next();
				rewrite.addImport(type);
			}
			IBinding binding= null;
			for (final Iterator iterator= fStaticBindings.iterator(); iterator.hasNext();) {
				binding= (IBinding) iterator.next();
				rewrite.addStaticImport(binding);
			}
			final IDocument document= new Document();
			try {
				rewrite.rewriteImports(new SubProgressMonitor(monitor, 100)).apply(document);
			} catch (MalformedTreeException exception) {
				JavaScriptPlugin.log(exception);
			} catch (BadLocationException exception) {
				JavaScriptPlugin.log(exception);
			} catch (CoreException exception) {
				JavaScriptPlugin.log(exception);
			}
			fTypeBindings.clear();
			fStaticBindings.clear();
			return document.get();
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the source for the new compilation unit containing the supertype.
	 * 
	 * @param copy
	 *            the working copy of the new supertype
	 * @param subType
	 *            the subtype
	 * @param superName
	 *            the name of the supertype
	 * @param sourceRewrite
	 *            the source compilation unit rewrite
	 * @param declaration
	 *            the type declaration
	 * @param status
	 *            the refactoring status
	 * @param monitor
	 *            the progress monitor to display progress
	 * @return the source of the new compilation unit, or <code>null</code>
	 * @throws CoreException
	 *             if an error occurs
	 */
	protected final String createTypeSource(final IJavaScriptUnit copy, final IType subType, final String superName, final CompilationUnitRewrite sourceRewrite, final AbstractTypeDeclaration declaration, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(copy);
		Assert.isNotNull(subType);
		Assert.isNotNull(superName);
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(declaration);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		String source= null;
		try {
			monitor.beginTask("", 100); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
			final String delimiter= StubUtility.getLineDelimiterUsed(subType.getJavaScriptProject());
			String typeComment= null;
			String fileComment= null;
			if (fSettings.createComments) {
				typeComment= CodeGeneration.getTypeComment(copy, superName, delimiter);
				fileComment= CodeGeneration.getFileComment(copy, delimiter);
			}
			final StringBuffer buffer= new StringBuffer(64);
			createTypeDeclaration(sourceRewrite, subType, superName, declaration, buffer, true, status, new SubProgressMonitor(monitor, 40));
			final String imports= createTypeImports(copy, new SubProgressMonitor(monitor, 60));
			source= createTypeTemplate(copy, imports, fileComment, typeComment, buffer.toString());
			if (source == null) {
				if (!subType.getPackageFragment().isDefaultPackage()) {
					if (imports.length() > 0)
						buffer.insert(0, imports);
					buffer.insert(0, "package " + subType.getPackageFragment().getElementName() + ";"); //$NON-NLS-1$//$NON-NLS-2$
				}
				source= buffer.toString();
			}
			final IDocument document= new Document(source);
			final TextEdit edit= CodeFormatterUtil.format2(CodeFormatter.K_JAVASCRIPT_UNIT, source, 0, delimiter, copy.getJavaScriptProject().getOptions(true));
			if (edit != null) {
				try {
					edit.apply(document, TextEdit.UPDATE_REGIONS);
				} catch (MalformedTreeException exception) {
					JavaScriptPlugin.log(exception);
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
				} catch (BadLocationException exception) {
					JavaScriptPlugin.log(exception);
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
				}
				source= document.get();
			}
		} finally {
			monitor.done();
		}
		return source;
	}

	/**
	 * Creates the type template based on the code generation settings.
	 * 
	 * @param unit
	 *            the working copy for the new supertype
	 * @param imports
	 *            the generated imports declaration
	 * @param fileComment
	 *            the file comment
	 * @param comment
	 *            the type comment
	 * @param content
	 *            the type content
	 * @return a template for the supertype, or <code>null</code>
	 * @throws CoreException
	 *             if the template could not be evaluated
	 */
	protected final String createTypeTemplate(final IJavaScriptUnit unit, final String imports, String fileComment, final String comment, final String content) throws CoreException {
		Assert.isNotNull(unit);
		Assert.isNotNull(imports);
		Assert.isNotNull(content);
		final IPackageFragment fragment= (IPackageFragment) unit.getParent();
		final StringBuffer buffer= new StringBuffer();
		final String delimiter= StubUtility.getLineDelimiterUsed(unit.getJavaScriptProject());
		if (!fragment.isDefaultPackage()) {
			buffer.append("package " + fragment.getElementName() + ";"); //$NON-NLS-1$ //$NON-NLS-2$
			buffer.append(delimiter);
			buffer.append(delimiter);
		}
		if (imports.length() > 0)
			buffer.append(imports);

		return StubUtility.getCompilationUnitContent(unit, buffer.toString(), fileComment, comment, content, delimiter);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void finalize() throws Throwable {
		resetWorkingCopies();
	}

	/**
	 * {@inheritDoc}
	 */
	public final String getComment() {
		return fComment;
	}

	/**
	 * Returns the field which corresponds to the specified variable declaration
	 * fragment
	 * 
	 * @param fragment
	 *            the variable declaration fragment
	 * @return the corresponding field
	 * @throws JavaScriptModelException
	 *             if an error occurs
	 */
	protected final IField getCorrespondingField(final VariableDeclarationFragment fragment) throws JavaScriptModelException {
		final IBinding binding= fragment.getName().resolveBinding();
		if (binding instanceof IVariableBinding) {
			final IVariableBinding variable= (IVariableBinding) binding;
			if (variable.isField()) {
				final IJavaScriptUnit unit= RefactoringASTParser.getCompilationUnit(fragment);
				final IJavaScriptElement element= unit.getElementAt(fragment.getStartPosition());
				if (element instanceof IField)
					return (IField) element;
			}
		}
		return null;
	}

	/**
	 * Computes the compilation units of fields referencing the specified type
	 * occurrences.
	 * 
	 * @param units
	 *            the compilation unit map (element type:
	 *            <code>&ltIJavaProject, Set&ltICompilationUnit&gt&gt</code>)
	 * @param nodes
	 *            the ast nodes representing the type occurrences
	 * @throws JavaScriptModelException
	 *             if an error occurs
	 */
	protected final void getFieldReferencingCompilationUnits(final Map units, final ASTNode[] nodes) throws JavaScriptModelException {
		ASTNode node= null;
		IField field= null;
		IJavaScriptProject project= null;
		for (int index= 0; index < nodes.length; index++) {
			node= nodes[index];
			project= RefactoringASTParser.getCompilationUnit(node).getJavaScriptProject();
			if (project != null) {
				final List fields= getReferencingFields(node, project);
				for (int offset= 0; offset < fields.size(); offset++) {
					field= (IField) fields.get(offset);
					Set set= (Set) units.get(project);
					if (set == null) {
						set= new HashSet();
						units.put(project, set);
					}
					final IJavaScriptUnit unit= field.getJavaScriptUnit();
					if (unit != null)
						set.add(unit);
				}
			}
		}
	}

	/**
	 * Computes the compilation units of methods referencing the specified type
	 * occurrences.
	 * 
	 * @param units
	 *            the compilation unit map (element type:
	 *            <code>&ltIJavaProject, Set&ltICompilationUnit&gt&gt</code>)
	 * @param nodes
	 *            the ast nodes representing the type occurrences
	 * @throws JavaScriptModelException
	 *             if an error occurs
	 */
	protected final void getMethodReferencingCompilationUnits(final Map units, final ASTNode[] nodes) throws JavaScriptModelException {
		ASTNode node= null;
		IFunction method= null;
		IJavaScriptProject project= null;
		for (int index= 0; index < nodes.length; index++) {
			node= nodes[index];
			project= RefactoringASTParser.getCompilationUnit(node).getJavaScriptProject();
			if (project != null) {
				method= getReferencingMethod(node);
				if (method != null) {
					Set set= (Set) units.get(project);
					if (set == null) {
						set= new HashSet();
						units.put(project, set);
					}
					final IJavaScriptUnit unit= method.getJavaScriptUnit();
					if (unit != null)
						set.add(unit);
				}
			}
		}
	}

	/**
	 * Computes the compilation units referencing the subtype to replace.
	 * 
	 * @param type
	 *            the subtype
	 * @param monitor
	 *            the progress monitor to use
	 * @param status
	 *            the refactoring status
	 * @return the referenced compilation units (element type:
	 *         <code>&ltIJavaProject, Collection&ltSearchResultGroup&gt&gt</code>)
	 * @throws JavaScriptModelException
	 *             if an error occurs
	 */
	protected final Map getReferencingCompilationUnits(final IType type, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaScriptModelException {
		try {
			monitor.beginTask("", 100); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
			final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2();
			engine.setOwner(fOwner);
			engine.setFiltering(true, true);
			engine.setStatus(status);
			engine.setScope(RefactoringScopeFactory.create(type));
			engine.setPattern(SearchPattern.createPattern(type, IJavaScriptSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE));
			engine.searchPattern(new SubProgressMonitor(monitor, 100));
			return engine.getAffectedProjects();
		} finally {
			monitor.done();
		}
	}

	/**
	 * Returns the fields which reference the specified ast node.
	 * 
	 * @param node
	 *            the ast node
	 * @param project
	 *            the java project
	 * @return the referencing fields
	 * @throws JavaScriptModelException
	 *             if an error occurs
	 */
	protected final List getReferencingFields(final ASTNode node, final IJavaScriptProject project) throws JavaScriptModelException {
		List result= Collections.EMPTY_LIST;
		if (node instanceof Type) {
			final BodyDeclaration parent= (BodyDeclaration) ASTNodes.getParent(node, BodyDeclaration.class);
			if (parent instanceof FieldDeclaration) {
				final List fragments= ((FieldDeclaration) parent).fragments();
				result= new ArrayList(fragments.size());
				VariableDeclarationFragment fragment= null;
				for (final Iterator iterator= fragments.iterator(); iterator.hasNext();) {
					fragment= (VariableDeclarationFragment) iterator.next();
					final IField field= getCorrespondingField(fragment);
					if (field != null)
						result.add(field);
				}
			}
		}
		return result;
	}

	/**
	 * Returns the method which references the specified ast node.
	 * 
	 * @param node
	 *            the ast node
	 * @return the referencing method
	 * @throws JavaScriptModelException
	 *             if an error occurs
	 */
	protected final IFunction getReferencingMethod(final ASTNode node) throws JavaScriptModelException {
		if (node instanceof Type) {
			final BodyDeclaration parent= (BodyDeclaration) ASTNodes.getParent(node, BodyDeclaration.class);
			if (parent instanceof FunctionDeclaration) {
				final IFunctionBinding binding= ((FunctionDeclaration) parent).resolveBinding();
				if (binding != null) {
					final IJavaScriptUnit unit= RefactoringASTParser.getCompilationUnit(node);
					final IJavaScriptElement element= unit.getElementAt(node.getStartPosition());
					if (element instanceof IFunction)
						return (IFunction) element;
				}
			}
		}
		return null;
	}

	protected IJavaScriptUnit getSharedWorkingCopy(final IJavaScriptUnit unit, final IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			IJavaScriptUnit copy= unit.findWorkingCopy(fOwner);
			if (copy == null)
				copy= unit.getWorkingCopy(fOwner, monitor);
			return copy;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Returns whether type occurrences in instanceof's should be rewritten.
	 * 
	 * @return <code>true</code> if they are rewritten, <code>false</code>
	 *         otherwise
	 */
	public final boolean isInstanceOf() {
		return fInstanceOf;
	}

	/**
	 * Should occurrences of the subtype be replaced by the supertype?
	 * 
	 * @return <code>true</code> if the subtype should be replaced,
	 *         <code>false</code> otherwise
	 */
	public final boolean isReplace() {
		return fReplace;
	}

	/**
	 * Performs the first pass of processing the affected compilation units.
	 * 
	 * @param creator
	 *            the constraints creator to use
	 * @param units
	 *            the compilation unit map (element type:
	 *            <code>&ltIJavaProject, Set&ltICompilationUnit&gt&gt</code>)
	 * @param groups
	 *            the search result group map (element type:
	 *            <code>&ltICompilationUnit, SearchResultGroup&gt</code>)
	 * @param unit
	 *            the compilation unit of the subtype
	 * @param node
	 *            the compilation unit node of the subtype
	 * @param monitor
	 *            the progress monitor to use
	 */
	protected final void performFirstPass(final SuperTypeConstraintsCreator creator, final Map units, final Map groups, final IJavaScriptUnit unit, final JavaScriptUnit node, final IProgressMonitor monitor) {
		try {
			monitor.beginTask("", 100); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
			node.accept(creator);
			monitor.worked(20);
			final SearchResultGroup group= (SearchResultGroup) groups.get(unit);
			if (group != null) {
				final ASTNode[] nodes= ASTNodeSearchUtil.getAstNodes(group.getSearchResults(), node);
				try {
					getMethodReferencingCompilationUnits(units, nodes);
					monitor.worked(40);
					getFieldReferencingCompilationUnits(units, nodes);
					monitor.worked(40);
				} catch (JavaScriptModelException exception) {
					JavaScriptPlugin.log(exception);
				}
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Performs the second pass of processing the affected compilation units.
	 * 
	 * @param creator
	 *            the constraints creator to use
	 * @param unit
	 *            the compilation unit of the subtype
	 * @param node
	 *            the compilation unit node of the subtype
	 * @param monitor
	 *            the progress monitor to use
	 */
	protected final void performSecondPass(final SuperTypeConstraintsCreator creator, final IJavaScriptUnit unit, final JavaScriptUnit node, final IProgressMonitor monitor) {
		try {
			monitor.beginTask("", 20); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
			node.accept(creator);
			monitor.worked(20);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Resets the working copies.
	 */
	protected void resetWorkingCopies() {
		final IJavaScriptUnit[] units= JavaScriptCore.getWorkingCopies(fOwner);
		for (int index= 0; index < units.length; index++) {
			final IJavaScriptUnit unit= units[index];
			try {
				unit.discardWorkingCopy();
			} catch (Exception exception) {
				// Do nothing
			}
		}
	}

	/**
	 * Resets the working copies.
	 * 
	 * @param unit
	 *            the compilation unit to discard
	 */
	protected void resetWorkingCopies(final IJavaScriptUnit unit) {
		final IJavaScriptUnit[] units= JavaScriptCore.getWorkingCopies(fOwner);
		for (int index= 0; index < units.length; index++) {
			if (!units[index].equals(unit)) {
				try {
					units[index].discardWorkingCopy();
				} catch (Exception exception) {
					// Do nothing
				}
			} else {
				try {
					units[index].getBuffer().setContents(unit.getPrimary().getBuffer().getContents());
					JavaModelUtil.reconcile(units[index]);
				} catch (JavaScriptModelException exception) {
					JavaScriptPlugin.log(exception);
				}
			}
		}
	}

	/**
	 * Creates the necessary text edits to replace the subtype occurrence by a
	 * supertype.
	 * 
	 * @param range
	 *            the compilation unit range
	 * @param estimate
	 *            the type estimate
	 * @param requestor
	 *            the ast requestor to use
	 * @param rewrite
	 *            the compilation unit rewrite to use
	 * @param copy
	 *            the compilation unit node of the working copy ast
	 * @param replacements
	 *            the set of variable binding keys of formal parameters which
	 *            must be replaced
	 * @param group
	 *            the text edit group to use
	 */
	protected final void rewriteTypeOccurrence(final CompilationUnitRange range, final TType estimate, final ASTRequestor requestor, final CompilationUnitRewrite rewrite, final JavaScriptUnit copy, final Set replacements, final TextEditGroup group) {
		ASTNode node= null;
		IBinding binding= null;
		final JavaScriptUnit target= rewrite.getRoot();
		node= NodeFinder.perform(copy, range.getSourceRange());
		if (node != null) {
			node= ASTNodes.getNormalizedNode(node).getParent();
			if (node instanceof VariableDeclaration) {
				binding= ((VariableDeclaration) node).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof SingleVariableDeclaration) {
					rewriteTypeOccurrence(estimate, rewrite, ((SingleVariableDeclaration) node).getType(), group);
					if (node.getParent() instanceof FunctionDeclaration) {
						binding= ((VariableDeclaration) node).resolveBinding();
						if (binding != null)
							replacements.add(binding.getKey());
					}
				}
			} else if (node instanceof VariableDeclarationStatement) {
				binding= ((VariableDeclaration) ((VariableDeclarationStatement) node).fragments().get(0)).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof VariableDeclarationFragment)
					rewriteTypeOccurrence(estimate, rewrite, ((VariableDeclarationStatement) ((VariableDeclarationFragment) node).getParent()).getType(), group);
			} else if (node instanceof FunctionDeclaration) {
				binding= ((FunctionDeclaration) node).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof FunctionDeclaration)
					rewriteTypeOccurrence(estimate, rewrite, ((FunctionDeclaration) node).getReturnType2(), group);
			} else if (node instanceof FieldDeclaration) {
				binding= ((VariableDeclaration) ((FieldDeclaration) node).fragments().get(0)).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof VariableDeclarationFragment) {
					node= node.getParent();
					if (node instanceof FieldDeclaration)
						rewriteTypeOccurrence(estimate, rewrite, ((FieldDeclaration) node).getType(), group);
				}
			} else if (node instanceof ArrayType) {
				final ASTNode type= node;
				while (node != null && !(node instanceof FunctionDeclaration) && !(node instanceof VariableDeclarationFragment))
					node= node.getParent();
				if (node != null) {
					final int delta= node.getStartPosition() + node.getLength() - type.getStartPosition();
					if (node instanceof FunctionDeclaration)
						binding= ((FunctionDeclaration) node).resolveBinding();
					else if (node instanceof VariableDeclarationFragment)
						binding= ((VariableDeclarationFragment) node).resolveBinding();
					if (binding != null) {
						node= target.findDeclaringNode(binding.getKey());
						if (node instanceof FunctionDeclaration || node instanceof VariableDeclarationFragment) {
							node= NodeFinder.perform(target, node.getStartPosition() + node.getLength() - delta, 0);
							if (node instanceof SimpleName)
								rewriteTypeOccurrence(estimate, rewrite, node, group);
						}
					}
				}
			} else if (node instanceof QualifiedName) {
				final ASTNode name= node;
				while (node != null && !(node instanceof FunctionDeclaration) && !(node instanceof VariableDeclarationFragment))
					node= node.getParent();
				if (node != null) {
					final int delta= node.getStartPosition() + node.getLength() - name.getStartPosition();
					if (node instanceof FunctionDeclaration)
						binding= ((FunctionDeclaration) node).resolveBinding();
					else if (node instanceof VariableDeclarationFragment)
						binding= ((VariableDeclarationFragment) node).resolveBinding();
					if (binding != null) {
						node= target.findDeclaringNode(binding.getKey());
						if (node instanceof SimpleName || node instanceof FunctionDeclaration || node instanceof VariableDeclarationFragment) {
							node= NodeFinder.perform(target, node.getStartPosition() + node.getLength() - delta, 0);
							if (node instanceof SimpleName)
								rewriteTypeOccurrence(estimate, rewrite, node, group);
						}
					}
				}
			}
		}
	}

	/**
	 * Creates the necessary text edits to replace the subtype occurrence by a
	 * supertype.
	 * 
	 * @param estimate
	 *            the type estimate
	 * @param rewrite
	 *            the ast rewrite to use
	 * @param node
	 *            the ast node to rewrite
	 * @param group
	 *            the text edit group to use
	 */
	protected final void rewriteTypeOccurrence(final TType estimate, final CompilationUnitRewrite rewrite, final ASTNode node, final TextEditGroup group) {
		rewrite.getImportRemover().registerRemovedNode(node);
		rewrite.getASTRewrite().replace(node, createCorrespondingNode(rewrite, estimate), group);
	}

	/**
	 * Creates the necessary text edits to replace the subtype occurrence by a
	 * supertype.
	 * 
	 * @param manager
	 *            the text change manager to use
	 * @param requestor
	 *            the ast requestor to use
	 * @param rewrite
	 *            the compilation unit rewrite of the subtype (not in working
	 *            copy mode)
	 * @param unit
	 *            the compilation unit
	 * @param node
	 *            the compilation unit node
	 * @param replacements
	 *            the set of variable binding keys of formal parameters which
	 *            must be replaced
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if the change could not be generated
	 */
	protected abstract void rewriteTypeOccurrences(TextEditBasedChangeManager manager, ASTRequestor requestor, CompilationUnitRewrite rewrite, IJavaScriptUnit unit, JavaScriptUnit node, Set replacements, IProgressMonitor monitor) throws CoreException;

	/**
	 * Creates the necessary text edits to replace the subtype occurrences by a
	 * supertype.
	 * 
	 * @param manager
	 *            the text change manager to use
	 * @param sourceRewrite
	 *            the compilation unit rewrite of the subtype (not in working
	 *            copy mode)
	 * @param sourceRequestor
	 *            the ast requestor of the subtype, or <code>null</code>
	 * @param subUnit
	 *            the compilation unit of the subtype, or <code>null</code>
	 * @param subNode
	 *            the compilation unit node of the subtype, or <code>null</code>
	 * @param replacements
	 *            the set of variable binding keys of formal parameters which
	 *            must be replaced
	 * @param status
	 *            the refactoring status
	 * @param monitor
	 *            the progress monitor to use
	 */
	protected final void rewriteTypeOccurrences(final TextEditBasedChangeManager manager, final ASTRequestor sourceRequestor, final CompilationUnitRewrite sourceRewrite, final IJavaScriptUnit subUnit, final JavaScriptUnit subNode, final Set replacements, final RefactoringStatus status, final IProgressMonitor monitor) {
		try {
			monitor.beginTask("", 300); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
			if (fTypeOccurrences != null) {
				final Set units= new HashSet(fTypeOccurrences.keySet());
				if (subUnit != null)
					units.remove(subUnit);
				final Map projects= new HashMap();
				Collection collection= null;
				IJavaScriptProject project= null;
				IJavaScriptUnit current= null;
				for (final Iterator iterator= units.iterator(); iterator.hasNext();) {
					current= (IJavaScriptUnit) iterator.next();
					project= current.getJavaScriptProject();
					collection= (Collection) projects.get(project);
					if (collection == null) {
						collection= new ArrayList();
						projects.put(project, collection);
					}
					collection.add(current);
				}
				final ASTParser parser= ASTParser.newParser(AST.JLS3);
				final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 320);
				try {
					final Set keySet= projects.keySet();
					subMonitor.beginTask("", keySet.size() * 100); //$NON-NLS-1$
					subMonitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
					for (final Iterator iterator= keySet.iterator(); iterator.hasNext();) {
						project= (IJavaScriptProject) iterator.next();
						collection= (Collection) projects.get(project);
						parser.setWorkingCopyOwner(fOwner);
						parser.setResolveBindings(true);
						parser.setProject(project);
						parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
						final IProgressMonitor subsubMonitor= new SubProgressMonitor(subMonitor, 100);
						try {
							subsubMonitor.beginTask("", collection.size() * 100 + 200); //$NON-NLS-1$
							subsubMonitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
							parser.createASTs((IJavaScriptUnit[]) collection.toArray(new IJavaScriptUnit[collection.size()]), new String[0], new ASTRequestor() {

								public final void acceptAST(final IJavaScriptUnit unit, final JavaScriptUnit node) {
									final IProgressMonitor subsubsubMonitor= new SubProgressMonitor(subsubMonitor, 100);
									try {
										subsubsubMonitor.beginTask("", 100); //$NON-NLS-1$
										subsubsubMonitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
										if (sourceRewrite != null)
											rewriteTypeOccurrences(manager, this, sourceRewrite, unit, node, replacements, new SubProgressMonitor(subsubsubMonitor, 100));
									} catch (CoreException exception) {
										status.merge(RefactoringStatus.createFatalErrorStatus(exception.getLocalizedMessage()));
									} finally {
										subsubsubMonitor.done();
									}
								}

								public final void acceptBinding(final String key, final IBinding binding) {
									// Do nothing
								}
							}, new SubProgressMonitor(subsubMonitor, 200));
						} finally {
							subsubMonitor.done();
						}
					}
					try {
						if (subUnit != null && subNode != null && sourceRewrite != null && sourceRequestor != null)
							rewriteTypeOccurrences(manager, sourceRequestor, sourceRewrite, subUnit, subNode, replacements, new SubProgressMonitor(subMonitor, 20));
					} catch (CoreException exception) {
						status.merge(RefactoringStatus.createFatalErrorStatus(exception.getLocalizedMessage()));
					}
				} finally {
					subMonitor.done();
				}
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final void setComment(final String comment) {
		fComment= comment;
	}

	/**
	 * Determines whether type occurrences in instanceof's should be rewritten.
	 * 
	 * @param rewrite
	 *            <code>true</code> to rewrite them, <code>false</code>
	 *            otherwise
	 */
	public final void setInstanceOf(final boolean rewrite) {
		fInstanceOf= rewrite;
	}

	/**
	 * Determines whether occurrences of the subtype should be replaced by the
	 * supertype.
	 * 
	 * @param replace
	 *            <code>true</code> to replace occurrences where possible,
	 *            <code>false</code> otherwise
	 */
	public final void setReplace(final boolean replace) {
		fReplace= replace;
	}

	/**
	 * Solves the supertype constraints to replace subtype by a supertype.
	 * 
	 * @param subUnit
	 *            the compilation unit of the subtype, or <code>null</code>
	 * @param subNode
	 *            the compilation unit node of the subtype, or <code>null</code>
	 * @param subType
	 *            the java element of the subtype
	 * @param subBinding
	 *            the type binding of the subtype to replace
	 * @param superBinding
	 *            the type binding of the supertype to use as replacement
	 * @param monitor
	 *            the progress monitor to use
	 * @param status
	 *            the refactoring status
	 * @throws JavaScriptModelException
	 *             if an error occurs
	 */
	protected final void solveSuperTypeConstraints(final IJavaScriptUnit subUnit, final JavaScriptUnit subNode, final IType subType, final ITypeBinding subBinding, final ITypeBinding superBinding, final IProgressMonitor monitor, final RefactoringStatus status) throws JavaScriptModelException {
		Assert.isNotNull(subType);
		Assert.isNotNull(subBinding);
		Assert.isNotNull(superBinding);
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		int level= 3;
		TypeEnvironment environment= new TypeEnvironment();
		final SuperTypeConstraintsModel model= new SuperTypeConstraintsModel(environment, environment.create(subBinding), environment.create(superBinding));
		final SuperTypeConstraintsCreator creator= new SuperTypeConstraintsCreator(model, fInstanceOf);
		try {
			monitor.beginTask("", 300); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
			final Map firstPass= getReferencingCompilationUnits(subType, new SubProgressMonitor(monitor, 100), status);
			final Map secondPass= new HashMap();
			IJavaScriptProject project= null;
			Collection collection= null;
			try {
				final ASTParser parser= ASTParser.newParser(AST.JLS3);
				Object element= null;
				IJavaScriptUnit current= null;
				SearchResultGroup group= null;
				SearchMatch[] matches= null;
				final Map groups= new HashMap();
				for (final Iterator outer= firstPass.keySet().iterator(); outer.hasNext();) {
					project= (IJavaScriptProject) outer.next();
					if (level == 3 && !JavaModelUtil.is50OrHigher(project))
						level= 2;
					collection= (Collection) firstPass.get(project);
					if (collection != null) {
						for (final Iterator inner= collection.iterator(); inner.hasNext();) {
							group= (SearchResultGroup) inner.next();
							matches= group.getSearchResults();
							for (int index= 0; index < matches.length; index++) {
								element= matches[index].getElement();
								if (element instanceof IMember) {
									current= ((IMember) element).getJavaScriptUnit();
									if (current != null)
										groups.put(current, group);
								}
							}
						}
					}
				}
				Set units= null;
				final Set processed= new HashSet();
				if (subUnit != null)
					processed.add(subUnit);
				model.beginCreation();
				IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 120);
				try {
					final Set keySet= firstPass.keySet();
					subMonitor.beginTask("", keySet.size() * 100); //$NON-NLS-1$
					subMonitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
					for (final Iterator outer= keySet.iterator(); outer.hasNext();) {
						project= (IJavaScriptProject) outer.next();
						collection= (Collection) firstPass.get(project);
						if (collection != null) {
							units= new HashSet(collection.size());
							for (final Iterator inner= collection.iterator(); inner.hasNext();) {
								group= (SearchResultGroup) inner.next();
								matches= group.getSearchResults();
								for (int index= 0; index < matches.length; index++) {
									element= matches[index].getElement();
									if (element instanceof IMember) {
										current= ((IMember) element).getJavaScriptUnit();
										if (current != null)
											units.add(current);
									}
								}
							}
							final List batches= new ArrayList(units);
							final int size= batches.size();
							final int iterations= ((size - 1) / SIZE_BATCH) + 1;
							final IProgressMonitor subsubMonitor= new SubProgressMonitor(subMonitor, 100);
							try {
								subsubMonitor.beginTask("", iterations * 100); //$NON-NLS-1$
								subsubMonitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
								final Map options= RefactoringASTParser.getCompilerOptions(project);
								for (int index= 0; index < iterations; index++) {
									final List iteration= batches.subList(index * SIZE_BATCH, Math.min(size, (index + 1) * SIZE_BATCH));
									parser.setWorkingCopyOwner(fOwner);
									parser.setResolveBindings(true);
									parser.setProject(project);
									parser.setCompilerOptions(options);
									final IProgressMonitor subsubsubMonitor= new SubProgressMonitor(subsubMonitor, 100);
									try {
										final int count= iteration.size();
										subsubsubMonitor.beginTask("", count * 100); //$NON-NLS-1$
										subsubsubMonitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
										parser.createASTs((IJavaScriptUnit[]) iteration.toArray(new IJavaScriptUnit[count]), new String[0], new ASTRequestor() {

											public final void acceptAST(final IJavaScriptUnit unit, final JavaScriptUnit node) {
												if (!processed.contains(unit)) {
													performFirstPass(creator, secondPass, groups, unit, node, new SubProgressMonitor(subsubsubMonitor, 100));
													processed.add(unit);
												} else
													subsubsubMonitor.worked(100);
											}

											public final void acceptBinding(final String key, final IBinding binding) {
												// Do nothing
											}
										}, new NullProgressMonitor());
									} finally {
										subsubsubMonitor.done();
									}
								}
							} finally {
								subsubMonitor.done();
							}
						}
					}
				} finally {
					firstPass.clear();
					subMonitor.done();
				}
				if (subUnit != null && subNode != null)
					performFirstPass(creator, secondPass, groups, subUnit, subNode, new SubProgressMonitor(subMonitor, 20));
				subMonitor= new SubProgressMonitor(monitor, 100);
				try {
					final Set keySet= secondPass.keySet();
					subMonitor.beginTask("", keySet.size() * 100); //$NON-NLS-1$
					subMonitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
					for (final Iterator iterator= keySet.iterator(); iterator.hasNext();) {
						project= (IJavaScriptProject) iterator.next();
						if (level == 3 && !JavaModelUtil.is50OrHigher(project))
							level= 2;
						collection= (Collection) secondPass.get(project);
						if (collection != null) {
							parser.setWorkingCopyOwner(fOwner);
							parser.setResolveBindings(true);
							parser.setProject(project);
							parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
							final IProgressMonitor subsubMonitor= new SubProgressMonitor(subMonitor, 100);
							try {
								subsubMonitor.beginTask("", collection.size() * 100); //$NON-NLS-1$
								subsubMonitor.setTaskName(RefactoringCoreMessages.SuperTypeRefactoringProcessor_creating);
								parser.createASTs((IJavaScriptUnit[]) collection.toArray(new IJavaScriptUnit[collection.size()]), new String[0], new ASTRequestor() {

									public final void acceptAST(final IJavaScriptUnit unit, final JavaScriptUnit node) {
										if (!processed.contains(unit))
											performSecondPass(creator, unit, node, new SubProgressMonitor(subsubMonitor, 100));
										else
											subsubMonitor.worked(100);
									}

									public final void acceptBinding(final String key, final IBinding binding) {
										// Do nothing
									}
								}, new NullProgressMonitor());
							} finally {
								subsubMonitor.done();
							}
						}
					}
				} finally {
					secondPass.clear();
					subMonitor.done();
				}
			} finally {
				model.endCreation();
				model.setCompliance(level);
			}
			final SuperTypeConstraintsSolver solver= createContraintSolver(model);
			solver.solveConstraints();
			fTypeOccurrences= solver.getTypeOccurrences();
			fObsoleteCasts= solver.getObsoleteCasts();
		} finally {
			monitor.done();
		}
	}
}
