/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     John Kaplan, johnkaplantech@gmail.com - 108071 [code templates] template for body of newly created class
 *******************************************************************************/
package org.eclipse.wst.jsdt.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.contentassist.SubjectControlContentAssistant;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.contentassist.ContentAssistHandler;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.ToolFactory;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.compiler.IScanner;
import org.eclipse.wst.jsdt.core.compiler.ITerminalSymbols;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.formatter.CodeFormatter;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.AddUnimplementedConstructorsOperation;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.AddUnimplementedMethodsOperation;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.TokenScanner;
import org.eclipse.wst.jsdt.internal.corext.refactoring.StubTypeContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.TypeContextChecker;
import org.eclipse.wst.jsdt.internal.corext.template.java.JavaContext;
import org.eclipse.wst.jsdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.Resources;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.dialogs.FilteredTypesSelectionDialog;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TableTextCellEditor;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.preferences.CodeTemplatePreferencePage;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.CompletionContextRequestor;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.JavaPackageCompletionProcessor;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;
import org.eclipse.wst.jsdt.internal.ui.util.SWTUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringButtonStatusDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.wst.jsdt.ui.CodeGeneration;
import org.eclipse.wst.jsdt.ui.CodeStyleConfiguration;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;

/**
 * The class <code>NewTypeWizardPage</code> contains controls and validation routines 
 * for a 'New Type WizardPage'. Implementors decide which components to add and to enable. 
 * Implementors can also customize the validation code. <code>NewTypeWizardPage</code> 
 * is intended to serve as base class of all wizards that create types like applets, servlets, classes, 
 * interfaces, etc.
 * <p>
 * See {@link NewClassWizardPage} or {@link NewInterfaceWizardPage} for an
 * example usage of the <code>NewTypeWizardPage</code>.
 * </p>
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * 
 * @see org.eclipse.wst.jsdt.ui.wizards.NewClassWizardPage
 * @see org.eclipse.wst.jsdt.ui.wizards.NewInterfaceWizardPage
 * 
 */
public abstract class NewTypeWizardPage extends NewContainerWizardPage {

	/**
	 * Class used in stub creation routines to add needed imports to a 
	 * compilation unit.
	 */
	public static class ImportsManager {

		private ImportRewrite fImportsRewrite;
				
		/* package */ ImportsManager(JavaScriptUnit astRoot) throws CoreException {
			fImportsRewrite= CodeStyleConfiguration.createImportRewrite(astRoot, true);
		}

		/* package */ IJavaScriptUnit getCompilationUnit() {
			return fImportsRewrite.getCompilationUnit();
		}
						
		/**
		 * Adds a new import declaration that is sorted in the existing imports.
		 * If an import already exists or the import would conflict with an import
		 * of an other type with the same simple name, the import is not added.
		 * 
		 * @param qualifiedTypeName The fully qualified name of the type to import
		 * (dot separated).
		 * @return Returns the simple type name that can be used in the code or the
		 * fully qualified type name if an import conflict prevented the import.
		 */				
		public String addImport(String qualifiedTypeName) {
			return fImportsRewrite.addImport(qualifiedTypeName);
		}
				
		/**
		 * Adds a new import declaration that is sorted in the existing imports.
		 * If an import already exists or the import would conflict with an import
		 * of an other type with the same simple name, the import is not added.
		 * 
		 * @param typeBinding the binding of the type to import
		 * 
		 * @return Returns the simple type name that can be used in the code or the
		 * fully qualified type name if an import conflict prevented the import.
		 */				
		public String addImport(ITypeBinding typeBinding) {
			return fImportsRewrite.addImport(typeBinding);
		}
		
		/**
		 * Adds a new import declaration for a static type that is sorted in the existing imports.
		 * If an import already exists or the import would conflict with an import
		 * of an other static import with the same simple name, the import is not added.
		 * 
		 * @param declaringTypeName The qualified name of the static's member declaring type
		 * @param simpleName the simple name of the member; either a field or a method name.
		 * @param isField <code>true</code> specifies that the member is a field, <code>false</code> if it is a
		 * method.
		 * @return returns either the simple member name if the import was successful or else the qualified name if
		 * an import conflict prevented the import.
		 * 
		 * 
		 */				
		public String addStaticImport(String declaringTypeName, String simpleName, boolean isField) {
			return fImportsRewrite.addStaticImport(declaringTypeName, simpleName, isField);
		}
				
		/* package */ void create(boolean needsSave, IProgressMonitor monitor) throws CoreException {
			TextEdit edit= fImportsRewrite.rewriteImports(monitor);
			JavaModelUtil.applyEdit(fImportsRewrite.getCompilationUnit(), edit, needsSave, null);
		}
		
		/* package */ void removeImport(String qualifiedName) {
			fImportsRewrite.removeImport(qualifiedName);
		}
		
		/* package */ void removeStaticImport(String qualifiedName) {
			fImportsRewrite.removeStaticImport(qualifiedName);
		}
	}
		
	
	/** Public access flag. See The JavaScript Virtual Machine Specification for more details. */
	public int F_PUBLIC = Flags.AccPublic;
	/** Private access flag. See The JavaScript Virtual Machine Specification for more details. */
	public int F_PRIVATE = Flags.AccPrivate;
	/** Static access flag. See The JavaScript Virtual Machine Specification for more details. */
	public int F_STATIC = Flags.AccStatic;
	/** Abstract property flag. See The JavaScript Virtual Machine Specification for more details. */
	public int F_ABSTRACT = Flags.AccAbstract;

	private final static String PAGE_NAME= "NewTypeWizardPage"; //$NON-NLS-1$
		
	/** Field ID of the package input field. */
	protected final static String PACKAGE= PAGE_NAME + ".package";	 //$NON-NLS-1$
	/** Field ID of the enclosing type input field. */
	protected final static String ENCLOSING= PAGE_NAME + ".enclosing"; //$NON-NLS-1$
	/** Field ID of the enclosing type checkbox. */
	protected final static String ENCLOSINGSELECTION= ENCLOSING + ".selection"; //$NON-NLS-1$
	/** Field ID of the type name input field. */	
	protected final static String TYPENAME= PAGE_NAME + ".typename"; //$NON-NLS-1$
	/** Field ID of the super type input field. */
	protected final static String SUPER= PAGE_NAME + ".superclass"; //$NON-NLS-1$
	/** Field ID of the super interfaces input field. */
	protected final static String INTERFACES= PAGE_NAME + ".interfaces"; //$NON-NLS-1$
	/** Field ID of the modifier check boxes. */
	protected final static String MODIFIERS= PAGE_NAME + ".modifiers"; //$NON-NLS-1$
	/** Field ID of the method stubs check boxes. */
	protected final static String METHODS= PAGE_NAME + ".methods"; //$NON-NLS-1$

	private static class InterfaceWrapper {
		public String interfaceName;

		public InterfaceWrapper(String interfaceName) {
			this.interfaceName= interfaceName;
		}

		public int hashCode() {
			return interfaceName.hashCode();
		}

		public boolean equals(Object obj) {
			return obj != null && getClass().equals(obj.getClass()) && ((InterfaceWrapper) obj).interfaceName.equals(interfaceName);
		}
	}
	
	private static class InterfacesListLabelProvider extends LabelProvider {
		private Image fInterfaceImage;
		
		public InterfacesListLabelProvider() {
			fInterfaceImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_INTERFACE);
		}
		
		public String getText(Object element) {
			return ((InterfaceWrapper) element).interfaceName;
		}
		
		public Image getImage(Object element) {
			return fInterfaceImage;
		}
	}	

	private StringButtonStatusDialogField fPackageDialogField;
	
	private SelectionButtonDialogField fEnclosingTypeSelection;
	private StringButtonDialogField fEnclosingTypeDialogField;
		
	private boolean fCanModifyPackage;
	private boolean fCanModifyEnclosingType;
	
	private IPackageFragment fCurrPackage;
	
	private IType fCurrEnclosingType;
	/**
	 * a handle to the type to be created (does usually not exist, can be null)
	 */
	private IType fCurrType;
	private StringDialogField fTypeNameDialogField;
	
	private StringButtonDialogField fSuperClassDialogField;
	private ListDialogField fSuperInterfacesDialogField;
	
	private SelectionButtonDialogFieldGroup fAccMdfButtons;
	private SelectionButtonDialogFieldGroup fOtherMdfButtons;
	
	private SelectionButtonDialogField fAddCommentButton;
	private boolean fUseAddCommentButtonValue; // used for compatibility: Wizards that don't show the comment button control
	// will use the preferences settings
	
	private IType fCreatedType;
	
	private JavaPackageCompletionProcessor fCurrPackageCompletionProcessor;
	private JavaTypeCompletionProcessor fEnclosingTypeCompletionProcessor;
	private StubTypeContext fSuperClassStubTypeContext;
	private StubTypeContext fSuperInterfaceStubTypeContext;
	
	protected IStatus fEnclosingTypeStatus;
	protected IStatus fPackageStatus;
	protected IStatus fTypeNameStatus;
	protected IStatus fSuperClassStatus;
	protected IStatus fModifierStatus;
	protected IStatus fSuperInterfacesStatus;	
	
	private final int PUBLIC_INDEX= 0, DEFAULT_INDEX= 1, PRIVATE_INDEX= 2, PROTECTED_INDEX= 3;
	private final int ABSTRACT_INDEX= 0, STATIC_INDEX= 2, ENUM_ANNOT_STATIC_INDEX= 1;
	
	private int fTypeKind;
	
	/**
	 * Constant to signal that the created type is a class.
	 * 
	 */
	public static final int CLASS_TYPE = 1;
	
	/**
	 * Constant to signal that the created type is a interface.
	 * 
	 */
	public static final int INTERFACE_TYPE = 2;
	
	/**
	 * Constant to signal that the created type is an enum.
	 * 
	 */
	public static final int ENUM_TYPE = 3;
	
	/**
	 * Constant to signal that the created type is an annotation.
	 * 
	 */
	public static final int ANNOTATION_TYPE = 4;

	/**
	 * Creates a new <code>NewTypeWizardPage</code>.
	 * 
	 * @param isClass <code>true</code> if a new class is to be created; otherwise
	 * an interface is to be created
	 * @param pageName the wizard page's name
	 */
	public NewTypeWizardPage(boolean isClass, String pageName) {
		this(isClass ? CLASS_TYPE : INTERFACE_TYPE, pageName);
	}
	
	/**
	 * Creates a new <code>NewTypeWizardPage</code>.
	 * 
	 * @param typeKind Signals the kind of the type to be created. Valid kinds are
	 * {@link #CLASS_TYPE}, {@link #INTERFACE_TYPE}, {@link #ENUM_TYPE} and {@link #ANNOTATION_TYPE}
	 * @param pageName the wizard page's name
	 * 
	 */
	public NewTypeWizardPage(int typeKind, String pageName) {
	    super(pageName);
	    fTypeKind= typeKind;

	    fCreatedType= null;
		
		TypeFieldsAdapter adapter= new TypeFieldsAdapter();
		
		fPackageDialogField= new StringButtonStatusDialogField(adapter);
		fPackageDialogField.setDialogFieldListener(adapter);
		fPackageDialogField.setLabelText(getPackageLabel()); 
		fPackageDialogField.setButtonLabel(NewWizardMessages.NewTypeWizardPage_package_button); 
		fPackageDialogField.setStatusWidthHint(NewWizardMessages.NewTypeWizardPage_default); 
				
		fEnclosingTypeSelection= new SelectionButtonDialogField(SWT.CHECK);
		fEnclosingTypeSelection.setDialogFieldListener(adapter);
		fEnclosingTypeSelection.setLabelText(getEnclosingTypeLabel()); 
		
		fEnclosingTypeDialogField= new StringButtonDialogField(adapter);
		fEnclosingTypeDialogField.setDialogFieldListener(adapter);
		fEnclosingTypeDialogField.setButtonLabel(NewWizardMessages.NewTypeWizardPage_enclosing_button); 
		
		fTypeNameDialogField= new StringDialogField();
		fTypeNameDialogField.setDialogFieldListener(adapter);
		fTypeNameDialogField.setLabelText(getTypeNameLabel()); 
		
		fSuperClassDialogField= new StringButtonDialogField(adapter);
		fSuperClassDialogField.setDialogFieldListener(adapter);
		fSuperClassDialogField.setLabelText(getSuperClassLabel()); 
		fSuperClassDialogField.setButtonLabel(NewWizardMessages.NewTypeWizardPage_superclass_button); 
		
		String[] addButtons= new String[] {
			NewWizardMessages.NewTypeWizardPage_interfaces_add, 
			/* 1 */ null,
			NewWizardMessages.NewTypeWizardPage_interfaces_remove
		}; 
		fSuperInterfacesDialogField= new ListDialogField(adapter, addButtons, new InterfacesListLabelProvider());
		fSuperInterfacesDialogField.setDialogFieldListener(adapter);
		fSuperInterfacesDialogField.setTableColumns(new ListDialogField.ColumnsDescription(1, false));
		fSuperInterfacesDialogField.setLabelText(getSuperInterfacesLabel());
		fSuperInterfacesDialogField.setRemoveButtonIndex(2);
	
		String[] buttonNames1= new String[] {
			NewWizardMessages.NewTypeWizardPage_modifiers_public, 
			NewWizardMessages.NewTypeWizardPage_modifiers_default, 
			NewWizardMessages.NewTypeWizardPage_modifiers_private, 
			NewWizardMessages.NewTypeWizardPage_modifiers_protected
		};
		fAccMdfButtons= new SelectionButtonDialogFieldGroup(SWT.RADIO, buttonNames1, 4);
		fAccMdfButtons.setDialogFieldListener(adapter);
		fAccMdfButtons.setLabelText(getModifiersLabel());		 
		fAccMdfButtons.setSelection(0, true);
		
		String[] buttonNames2;
		if (fTypeKind == CLASS_TYPE) {
			buttonNames2= new String[] {
				NewWizardMessages.NewTypeWizardPage_modifiers_abstract, 
				NewWizardMessages.NewTypeWizardPage_modifiers_final, 
				NewWizardMessages.NewTypeWizardPage_modifiers_static
			};
		} else {
		    if (fTypeKind == ENUM_TYPE || fTypeKind == ANNOTATION_TYPE) {
		        buttonNames2= new String[] {
					NewWizardMessages.NewTypeWizardPage_modifiers_abstract, 
					NewWizardMessages.NewTypeWizardPage_modifiers_static
		        };
		    }
		    else
		        buttonNames2= new String[] {};
		}

		fOtherMdfButtons= new SelectionButtonDialogFieldGroup(SWT.CHECK, buttonNames2, 4);
		fOtherMdfButtons.setDialogFieldListener(adapter);
		
		fAccMdfButtons.enableSelectionButton(PRIVATE_INDEX, false);
		fAccMdfButtons.enableSelectionButton(PROTECTED_INDEX, false);
		fOtherMdfButtons.enableSelectionButton(STATIC_INDEX, false);
		
		if (fTypeKind == ENUM_TYPE || fTypeKind == ANNOTATION_TYPE) {
		    fOtherMdfButtons.enableSelectionButton(ABSTRACT_INDEX, false);
		    fOtherMdfButtons.enableSelectionButton(ENUM_ANNOT_STATIC_INDEX, false);
		}
		
		fAddCommentButton= new SelectionButtonDialogField(SWT.CHECK);
		fAddCommentButton.setLabelText(NewWizardMessages.NewTypeWizardPage_addcomment_label); 
		
		fUseAddCommentButtonValue= false; // only used when enabled
		
		fCurrPackageCompletionProcessor= new JavaPackageCompletionProcessor();
		fEnclosingTypeCompletionProcessor= new JavaTypeCompletionProcessor(false, false, true);
		
		fPackageStatus= new StatusInfo();
		fEnclosingTypeStatus= new StatusInfo();
		
		fCanModifyPackage= true;
		fCanModifyEnclosingType= true;
		updateEnableState();
					
		fTypeNameStatus= new StatusInfo();
		fSuperClassStatus= new StatusInfo();
		fSuperInterfacesStatus= new StatusInfo();
		fModifierStatus= new StatusInfo();
	}
	
	/**
	 * Initializes all fields provided by the page with a given selection.
	 * 
	 * @param elem the selection used to initialize this page or <code>
	 * null</code> if no selection was available
	 */
	protected void initTypePage(IJavaScriptElement elem) {
		String initSuperclass= "java.lang.Object"; //$NON-NLS-1$
		ArrayList initSuperinterfaces= new ArrayList(5);

		IJavaScriptProject project= null;
		IPackageFragment pack= null;
		IType enclosingType= null;
				
		if (elem != null) {
			// evaluate the enclosing type
			project= elem.getJavaScriptProject();
			pack= (IPackageFragment) elem.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT);
			IType typeInCU= (IType) elem.getAncestor(IJavaScriptElement.TYPE);
			if (typeInCU != null) {
				if (typeInCU.getJavaScriptUnit() != null) {
					enclosingType= typeInCU;
				}
			} else {
				IJavaScriptUnit cu= (IJavaScriptUnit) elem.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
				if (cu != null) {
					enclosingType= cu.findPrimaryType();
				}
			}
			
			IType type= null;
			if (elem.getElementType() == IJavaScriptElement.TYPE) {
				type= (IType)elem;
				if (type.exists()) {
					String superName= JavaModelUtil.getFullyQualifiedName(type);
					initSuperclass= superName;
				}
			}			
		}
		
		String typeName= ""; //$NON-NLS-1$
		
		ITextSelection selection= getCurrentTextSelection();
		if (selection != null) {
			String text= selection.getText();
			if (text != null && validateJavaTypeName(text, project).isOK()) {
				typeName= text;
			}
		}

		setPackageFragment(pack, true);
		setEnclosingType(enclosingType, true);
		setEnclosingTypeSelection(false, true);
	
		setTypeName(typeName, true);
		setSuperClass(initSuperclass, true);
		setSuperInterfaces(initSuperinterfaces, true);
		
		setAddComments(StubUtility.doAddComments(project), true); // from project or workspace
	}
	
	private static IStatus validateJavaTypeName(String text, IJavaScriptProject project) {
		if (project == null || !project.exists()) {
			return JavaScriptConventions.validateJavaScriptTypeName(text, JavaScriptCore.VERSION_1_3, JavaScriptCore.VERSION_1_3);
		}
		String sourceLevel= project.getOption(JavaScriptCore.COMPILER_SOURCE, true);
		String compliance= project.getOption(JavaScriptCore.COMPILER_COMPLIANCE, true);
		return JavaScriptConventions.validateJavaScriptTypeName(text, sourceLevel, compliance);
	}
	
	private static IStatus validatePackageName(String text, IJavaScriptProject project) {
		if (project == null || !project.exists()) {
			return JavaScriptConventions.validatePackageName(text, JavaScriptCore.VERSION_1_3, JavaScriptCore.VERSION_1_3);
		}
		String sourceLevel= project.getOption(JavaScriptCore.COMPILER_SOURCE, true);
		String compliance= project.getOption(JavaScriptCore.COMPILER_COMPLIANCE, true);
		return JavaScriptConventions.validatePackageName(text, sourceLevel, compliance);
	}
	
	// -------- UI Creation ---------
	
	/**
	 * Returns the label that is used for the package input field.
	 * 
	 * @return the label that is used for the package input field.
	 * 
	 */
	protected String getPackageLabel() {
		return NewWizardMessages.NewTypeWizardPage_package_label;
	}

	/**
	 * Returns the label that is used for the enclosing type input field.
	 * 
	 * @return the label that is used for the enclosing type input field.
	 * 
	 */
	protected String getEnclosingTypeLabel() {
		return NewWizardMessages.NewTypeWizardPage_enclosing_selection_label;
	}
	
	/**
	 * Returns the label that is used for the type name input field.
	 * 
	 * @return the label that is used for the type name input field.
	 * 
	 */
	protected String getTypeNameLabel() {
		return NewWizardMessages.NewTypeWizardPage_typename_label;
	}

	/**
	 * Returns the label that is used for the modifiers input field.
	 * 
	 * @return the label that is used for the modifiers input field
	 * 
	 */
	protected String getModifiersLabel() {
		return NewWizardMessages.NewTypeWizardPage_modifiers_acc_label;
	}

	/**
	 * Returns the label that is used for the super class input field.
	 * 
	 * @return the label that is used for the super class input field.
	 * 
	 */
	protected String getSuperClassLabel() {
		return NewWizardMessages.NewTypeWizardPage_superclass_label;
	}

	/**
	 * Returns the label that is used for the super interfaces input field.
	 * 
	 * @return the label that is used for the super interfaces input field.
	 * 
	 */
	protected String getSuperInterfacesLabel() {
	    if (fTypeKind != INTERFACE_TYPE)
	        return NewWizardMessages.NewTypeWizardPage_interfaces_class_label; 
	    return NewWizardMessages.NewTypeWizardPage_interfaces_ifc_label; 
	}
	
	/**
	 * Creates a separator line. Expects a <code>GridLayout</code> with at least 1 column.
	 * 
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */
	protected void createSeparator(Composite composite, int nColumns) {
		(new Separator(SWT.SEPARATOR | SWT.HORIZONTAL)).doFillIntoGrid(composite, nColumns, convertHeightInCharsToPixels(1));		
	}

	/**
	 * Creates the controls for the package name field. Expects a <code>GridLayout</code> with at 
	 * least 4 columns.
	 * 
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */	
	protected void createPackageControls(Composite composite, int nColumns) {
		fPackageDialogField.doFillIntoGrid(composite, nColumns);
		Text text= fPackageDialogField.getTextControl(null);
		LayoutUtil.setWidthHint(text, getMaxFieldWidth());	
		LayoutUtil.setHorizontalGrabbing(text);
		ControlContentAssistHelper.createTextContentAssistant(text, fCurrPackageCompletionProcessor);
		TextFieldNavigationHandler.install(text);
	}

	/**
	 * Creates the controls for the enclosing type name field. Expects a <code>GridLayout</code> with at 
	 * least 4 columns.
	 * 
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */		
	protected void createEnclosingTypeControls(Composite composite, int nColumns) {
		// #6891
		Composite tabGroup= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
 		tabGroup.setLayout(layout);

		fEnclosingTypeSelection.doFillIntoGrid(tabGroup, 1);

		Text text= fEnclosingTypeDialogField.getTextControl(composite);
		text.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			public void getName(AccessibleEvent e) {
				e.result= NewWizardMessages.NewTypeWizardPage_enclosing_field_description;
			}
		});
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= getMaxFieldWidth();
		gd.horizontalSpan= 2;
		text.setLayoutData(gd);
		
		Button button= fEnclosingTypeDialogField.getChangeControl(composite);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.widthHint = SWTUtil.getButtonWidthHint(button);
		button.setLayoutData(gd);
		ControlContentAssistHelper.createTextContentAssistant(text, fEnclosingTypeCompletionProcessor);
		TextFieldNavigationHandler.install(text);
	}	

	/**
	 * Creates the controls for the type name field. Expects a <code>GridLayout</code> with at 
	 * least 2 columns.
	 * 
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */		
	protected void createTypeNameControls(Composite composite, int nColumns) {
		fTypeNameDialogField.doFillIntoGrid(composite, nColumns - 1);
		DialogField.createEmptySpace(composite);
		
		Text text= fTypeNameDialogField.getTextControl(null);
		LayoutUtil.setWidthHint(text, getMaxFieldWidth());
		TextFieldNavigationHandler.install(text);
	}

	/**
	 * Creates the controls for the modifiers radio/checkbox buttons. Expects a 
	 * <code>GridLayout</code> with at least 3 columns.
	 * 
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */		
	protected void createModifierControls(Composite composite, int nColumns) {
		LayoutUtil.setHorizontalSpan(fAccMdfButtons.getLabelControl(composite), 1);
		
		Control control= fAccMdfButtons.getSelectionButtonsGroup(composite);
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= nColumns - 2;
		control.setLayoutData(gd);
		
		DialogField.createEmptySpace(composite);
		
		if (fTypeKind == CLASS_TYPE) {
			DialogField.createEmptySpace(composite);
			
			control= fOtherMdfButtons.getSelectionButtonsGroup(composite);
			gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			gd.horizontalSpan= nColumns - 2;
			control.setLayoutData(gd);		
	
			DialogField.createEmptySpace(composite);
		}
	}

	/**
	 * Creates the controls for the superclass name field. Expects a <code>GridLayout</code> 
	 * with at least 3 columns.
	 * 
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */		
	protected void createSuperClassControls(Composite composite, int nColumns) {
		fSuperClassDialogField.doFillIntoGrid(composite, nColumns);
		Text text= fSuperClassDialogField.getTextControl(null);
		LayoutUtil.setWidthHint(text, getMaxFieldWidth());
		
		JavaTypeCompletionProcessor superClassCompletionProcessor= new JavaTypeCompletionProcessor(false, false, true);
		superClassCompletionProcessor.setCompletionContextRequestor(new CompletionContextRequestor() {
			public StubTypeContext getStubTypeContext() {
				return getSuperClassStubTypeContext();
			}
		});

		ControlContentAssistHelper.createTextContentAssistant(text, superClassCompletionProcessor);
		TextFieldNavigationHandler.install(text);
	}

	/**
	 * Creates the controls for the superclass name field. Expects a <code>GridLayout</code> with 
	 * at least 3 columns.
	 * 
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 */			
	protected void createSuperInterfacesControls(Composite composite, int nColumns) {
		final String INTERFACE= "interface"; //$NON-NLS-1$
		fSuperInterfacesDialogField.doFillIntoGrid(composite, nColumns);
		final TableViewer tableViewer= fSuperInterfacesDialogField.getTableViewer();
		tableViewer.setColumnProperties(new String[] {INTERFACE});
		
		TableTextCellEditor cellEditor= new TableTextCellEditor(tableViewer, 0) {
		    protected void doSetFocus() {
		        if (text != null) {
		            text.setFocus();
		            text.setSelection(text.getText().length());
		            checkSelection();
		            checkDeleteable();
		            checkSelectable();
		        }
		    }
		};
		JavaTypeCompletionProcessor superInterfaceCompletionProcessor= new JavaTypeCompletionProcessor(false, false, true);
		superInterfaceCompletionProcessor.setCompletionContextRequestor(new CompletionContextRequestor() {
			public StubTypeContext getStubTypeContext() {
				return getSuperInterfacesStubTypeContext();
			}
		});
		SubjectControlContentAssistant contentAssistant= ControlContentAssistHelper.createJavaContentAssistant(superInterfaceCompletionProcessor);
		Text cellEditorText= cellEditor.getText();
		ContentAssistHandler.createHandlerForText(cellEditorText, contentAssistant);
		TextFieldNavigationHandler.install(cellEditorText);
		cellEditor.setContentAssistant(contentAssistant);
		
		tableViewer.setCellEditors(new CellEditor[] { cellEditor });
		tableViewer.setCellModifier(new ICellModifier() {
			public void modify(Object element, String property, Object value) {
				if (element instanceof Item)
					element = ((Item) element).getData();
				
				((InterfaceWrapper) element).interfaceName= (String) value;
				fSuperInterfacesDialogField.elementChanged(element);
			}
			public Object getValue(Object element, String property) {
				return ((InterfaceWrapper) element).interfaceName;
			}
			public boolean canModify(Object element, String property) {
				return true;
			}
		});
		tableViewer.getTable().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.keyCode == SWT.F2 && event.stateMask == 0) {
					ISelection selection= tableViewer.getSelection();
					if (! (selection instanceof IStructuredSelection))
						return;
					IStructuredSelection structuredSelection= (IStructuredSelection) selection;
					tableViewer.editElement(structuredSelection.getFirstElement(), 0);
				} 
			}
		});
		GridData gd= (GridData) fSuperInterfacesDialogField.getListControl(null).getLayoutData();
		if (fTypeKind == CLASS_TYPE) {
			gd.heightHint= convertHeightInCharsToPixels(3);
		} else {
			gd.heightHint= convertHeightInCharsToPixels(6);
		}
		gd.grabExcessVerticalSpace= false;
		gd.widthHint= getMaxFieldWidth();
	}
	
	/**
	 * Creates the controls for the preference page links. Expects a <code>GridLayout</code> with 
	 * at least 3 columns.
	 * 
	 * @param composite the parent composite
	 * @param nColumns number of columns to span
	 * 
	 * 
	 */			
	protected void createCommentControls(Composite composite, int nColumns) {
    	Link link= new Link(composite, SWT.NONE);
    	link.setText(NewWizardMessages.NewTypeWizardPage_addcomment_description);
    	link.addSelectionListener(new TypeFieldsAdapter());
    	link.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, nColumns, 1));
		DialogField.createEmptySpace(composite);
		fAddCommentButton.doFillIntoGrid(composite, nColumns - 1);
	}


	
	/**
	 * Sets the focus on the type name input field.
	 */		
	protected void setFocus() {
		fTypeNameDialogField.setFocus();
	}
				
	// -------- TypeFieldsAdapter --------

	private class TypeFieldsAdapter implements IStringButtonAdapter, IDialogFieldListener, IListAdapter, SelectionListener {
		
		// -------- IStringButtonAdapter
		public void changeControlPressed(DialogField field) {
			typePageChangeControlPressed(field);
		}
		
		// -------- IListAdapter
		public void customButtonPressed(ListDialogField field, int index) {
			typePageCustomButtonPressed(field, index);
		}
		
		public void selectionChanged(ListDialogField field) {}
		
		// -------- IDialogFieldListener
		public void dialogFieldChanged(DialogField field) {
			typePageDialogFieldChanged(field);
		}
		
		public void doubleClicked(ListDialogField field) {
		}


		public void widgetSelected(SelectionEvent e) {
			typePageLinkActivated(e);
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			typePageLinkActivated(e);
		}
	}
	
	private void typePageLinkActivated(SelectionEvent e) {
		IJavaScriptProject project= getJavaProject();
		if (project != null) {
			PreferenceDialog dialog= PreferencesUtil.createPropertyDialogOn(getShell(), project.getProject(), CodeTemplatePreferencePage.PROP_ID, null, null);
			dialog.open();
		} else {
			String title= NewWizardMessages.NewTypeWizardPage_configure_templates_title; 
			String message= NewWizardMessages.NewTypeWizardPage_configure_templates_message; 
			MessageDialog.openInformation(getShell(), title, message);
		}
	}
	
	private void typePageChangeControlPressed(DialogField field) {
		if (field == fPackageDialogField) {
			IPackageFragment pack= choosePackage();	
			if (pack != null) {
				fPackageDialogField.setText(pack.getElementName());
			}
		} else if (field == fEnclosingTypeDialogField) {
			IType type= chooseEnclosingType();
			if (type != null) {
				fEnclosingTypeDialogField.setText(JavaModelUtil.getFullyQualifiedName(type));
			}
		} else if (field == fSuperClassDialogField) {
			IType type= chooseSuperClass();
			if (type != null) {
				fSuperClassDialogField.setText(JavaModelUtil.getFullyQualifiedName(type));
			}
		}
	}
	
	private void typePageCustomButtonPressed(DialogField field, int index) {		
		if (field == fSuperInterfacesDialogField) {
			List interfaces= fSuperInterfacesDialogField.getElements();
			if (!interfaces.isEmpty()) {
				Object element= interfaces.get(interfaces.size() - 1);
				fSuperInterfacesDialogField.editElement(element);
			}
		}
	}
	
	/*
	 * A field on the type has changed. The fields' status and all dependent
	 * status are updated.
	 */
	private void typePageDialogFieldChanged(DialogField field) {
		String fieldName= null;
		if (field == fPackageDialogField) {
			fPackageStatus= packageChanged();
			updatePackageStatusLabel();
			fTypeNameStatus= typeNameChanged();
			fSuperClassStatus= superClassChanged();			
			fieldName= PACKAGE;
		} else if (field == fEnclosingTypeDialogField) {
			fEnclosingTypeStatus= enclosingTypeChanged();
			fTypeNameStatus= typeNameChanged();
			fSuperClassStatus= superClassChanged();				
			fieldName= ENCLOSING;
		} else if (field == fEnclosingTypeSelection) {
			updateEnableState();
			boolean isEnclosedType= isEnclosingTypeSelected();
			if (!isEnclosedType) {
				if (fAccMdfButtons.isSelected(PRIVATE_INDEX) || fAccMdfButtons.isSelected(PROTECTED_INDEX)) {
					fAccMdfButtons.setSelection(PRIVATE_INDEX, false);
					fAccMdfButtons.setSelection(PROTECTED_INDEX, false); 
					fAccMdfButtons.setSelection(PUBLIC_INDEX, true);
				}
				if (fOtherMdfButtons.isSelected(STATIC_INDEX)) {
					fOtherMdfButtons.setSelection(STATIC_INDEX, false);
				}
			}
			fAccMdfButtons.enableSelectionButton(PRIVATE_INDEX, isEnclosedType);
			fAccMdfButtons.enableSelectionButton(PROTECTED_INDEX, isEnclosedType);
			fOtherMdfButtons.enableSelectionButton(STATIC_INDEX, isEnclosedType);
			fTypeNameStatus= typeNameChanged();
			fSuperClassStatus= superClassChanged();
			fieldName= ENCLOSINGSELECTION;
		} else if (field == fTypeNameDialogField) {
			fTypeNameStatus= typeNameChanged();
			fieldName= TYPENAME;
		} else if (field == fSuperClassDialogField) {
			fSuperClassStatus= superClassChanged();
			fieldName= SUPER;
		} else if (field == fSuperInterfacesDialogField) {
			fSuperInterfacesStatus= superInterfacesChanged();
			fieldName= INTERFACES;
		} else if (field == fOtherMdfButtons || field == fAccMdfButtons) {
			fModifierStatus= modifiersChanged();
			fieldName= MODIFIERS;
		} else {
			fieldName= METHODS;
		}
		// tell all others
		handleFieldChanged(fieldName);
	}		
	
	// -------- update message ----------------		
	
	/*
	 * @see org.eclipse.wst.jsdt.ui.wizards.NewContainerWizardPage#handleFieldChanged(String)
	 */
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (fieldName == CONTAINER) {
			fPackageStatus= packageChanged();
			fEnclosingTypeStatus= enclosingTypeChanged();			
			fTypeNameStatus= typeNameChanged();
			fSuperClassStatus= superClassChanged();
			fSuperInterfacesStatus= superInterfacesChanged();
		}
	}
	
	// ---- set / get ----------------
	
	/**
	 * Returns the text of the package input field.
	 * 
	 * @return the text of the package input field
	 */
	public String getPackageText() {
		return fPackageDialogField.getText();
	}

	/**
	 * Returns the text of the enclosing type input field.
	 * 
	 * @return the text of the enclosing type input field
	 */	
	public String getEnclosingTypeText() {
		return fEnclosingTypeDialogField.getText();
	}	
	
	
	/**
	 * Returns the package fragment corresponding to the current input.
	 * 
	 * @return a package fragment or <code>null</code> if the input 
	 * could not be resolved.
	 */
	public IPackageFragment getPackageFragment() {
		if (!isEnclosingTypeSelected()) {
			return fCurrPackage;
		} else {
			if (fCurrEnclosingType != null) {
				return fCurrEnclosingType.getPackageFragment();
			}
		}
		return null;
	}
	
	/**
	 * Sets the package fragment to the given value. The method updates the model 
	 * and the text of the control.
	 * 
	 * @param pack the package fragment to be set
	 * @param canBeModified if <code>true</code> the package fragment is
	 * editable; otherwise it is read-only.
	 */
	public void setPackageFragment(IPackageFragment pack, boolean canBeModified) {
		fCurrPackage= pack;
		fCanModifyPackage= canBeModified;
		String str= (pack == null) ? "" : pack.getElementName(); //$NON-NLS-1$
		fPackageDialogField.setText(str);
		updateEnableState();
	}	

	/**
	 * Returns the enclosing type corresponding to the current input.
	 * 
	 * @return the enclosing type or <code>null</code> if the enclosing type is 
	 * not selected or the input could not be resolved
	 */
	public IType getEnclosingType() {
		if (isEnclosingTypeSelected()) {
			return fCurrEnclosingType;
		}
		return null;
	}

	/**
	 * Sets the enclosing type. The method updates the underlying model 
	 * and the text of the control.
	 * 
	 * @param type the enclosing type
	 * @param canBeModified if <code>true</code> the enclosing type field is
	 * editable; otherwise it is read-only.
	 */	
	public void setEnclosingType(IType type, boolean canBeModified) {
		fCurrEnclosingType= type;
		fCanModifyEnclosingType= canBeModified;
		String str= (type == null) ? "" : JavaModelUtil.getFullyQualifiedName(type); //$NON-NLS-1$
		fEnclosingTypeDialogField.setText(str);
		updateEnableState();
	}
	
	/**
	 * Returns the selection state of the enclosing type checkbox.
	 * 
	 * @return the selection state of the enclosing type checkbox
	 */
	public boolean isEnclosingTypeSelected() {
		return fEnclosingTypeSelection.isSelected();
	}

	/**
	 * Sets the enclosing type checkbox's selection state.
	 * 
	 * @param isSelected the checkbox's selection state
	 * @param canBeModified if <code>true</code> the enclosing type checkbox is
	 * modifiable; otherwise it is read-only.
	 */
	public void setEnclosingTypeSelection(boolean isSelected, boolean canBeModified) {
		fEnclosingTypeSelection.setSelection(isSelected);
		fEnclosingTypeSelection.setEnabled(canBeModified);
		updateEnableState();
	}
	
	/**
	 * Returns the type name entered into the type input field.
	 * 
	 * @return the type name
	 */
	public String getTypeName() {
		return fTypeNameDialogField.getText();
	}

	/**
	 * Sets the type name input field's text to the given value. Method doesn't update
	 * the model.
	 * 
	 * @param name the new type name
	 * @param canBeModified if <code>true</code> the type name field is
	 * editable; otherwise it is read-only.
	 */	
	public void setTypeName(String name, boolean canBeModified) {
		fTypeNameDialogField.setText(name);
		fTypeNameDialogField.setEnabled(canBeModified);
	}	
	
	/**
	 * Returns the selected modifiers.
	 * 
	 * @return the selected modifiers
	 * @see Flags 
	 */	
	public int getModifiers() {
		int mdf= 0;
		if (fAccMdfButtons.isSelected(PUBLIC_INDEX)) {
			mdf+= F_PUBLIC;
		} else if (fAccMdfButtons.isSelected(PRIVATE_INDEX)) {
			mdf+= F_PRIVATE;
		}
		if (fOtherMdfButtons.isSelected(ABSTRACT_INDEX)) {	
			mdf+= F_ABSTRACT;
		}
		if (fOtherMdfButtons.isSelected(STATIC_INDEX)) {	
			mdf+= F_STATIC;
		}
		return mdf;
	}

	/**
	 * Sets the modifiers.
	 * 
	 * @param modifiers <code>F_PUBLIC</code>, <code>F_PRIVATE</code>, 
	 * <code>F_PROTECTED</code>, <code>F_ABSTRACT</code>, <code>F_FINAL</code>
	 * or <code>F_STATIC</code> or a valid combination.
	 * @param canBeModified if <code>true</code> the modifier fields are
	 * editable; otherwise they are read-only
	 * @see Flags 
	 */		
	public void setModifiers(int modifiers, boolean canBeModified) {
		if (Flags.isPublic(modifiers)) {
			fAccMdfButtons.setSelection(PUBLIC_INDEX, true);
		} else if (Flags.isPrivate(modifiers)) {
			fAccMdfButtons.setSelection(PRIVATE_INDEX, true);
		} else {
			fAccMdfButtons.setSelection(DEFAULT_INDEX, true);
		}
		if (Flags.isAbstract(modifiers)) {
			fOtherMdfButtons.setSelection(ABSTRACT_INDEX, true);
		}
		if (Flags.isStatic(modifiers)) {
			fOtherMdfButtons.setSelection(STATIC_INDEX, true);
		}
		
		fAccMdfButtons.setEnabled(canBeModified);
		fOtherMdfButtons.setEnabled(canBeModified);
	}
		
	/**
	 * Returns the content of the superclass input field.
	 * 
	 * @return the superclass name
	 */
	public String getSuperClass() {
		return fSuperClassDialogField.getText();
	}

	/**
	 * Sets the super class name.
	 * 
	 * @param name the new superclass name
	 * @param canBeModified  if <code>true</code> the superclass name field is
	 * editable; otherwise it is read-only.
	 */		
	public void setSuperClass(String name, boolean canBeModified) {
		fSuperClassDialogField.setText(name);
		fSuperClassDialogField.setEnabled(canBeModified);
	}	
	
	/**
	 * Returns the chosen super interfaces.
	 * 
	 * @return a list of chosen super interfaces. The list's elements
	 * are of type <code>String</code>
	 */
	public List getSuperInterfaces() {
		List interfaces= fSuperInterfacesDialogField.getElements();
		ArrayList result= new ArrayList(interfaces.size());
		for (Iterator iter= interfaces.iterator(); iter.hasNext();) {
			InterfaceWrapper wrapper= (InterfaceWrapper) iter.next();
			result.add(wrapper.interfaceName);
		}
		return result;
	}

	/**
	 * Sets the super interfaces.
	 * 
	 * @param interfacesNames a list of super interface. The method requires that
	 * the list's elements are of type <code>String</code>
	 * @param canBeModified if <code>true</code> the super interface field is
	 * editable; otherwise it is read-only.
	 */	
	public void setSuperInterfaces(List interfacesNames, boolean canBeModified) {
		ArrayList interfaces= new ArrayList(interfacesNames.size());
		for (Iterator iter= interfacesNames.iterator(); iter.hasNext();) {
			interfaces.add(new InterfaceWrapper((String) iter.next()));
		}
		fSuperInterfacesDialogField.setElements(interfaces);
		fSuperInterfacesDialogField.setEnabled(canBeModified);
	}
	
	/**
	 * Adds a super interface to the end of the list and selects it if it is not in the list yet.
	 * 
	 * @param superInterface the fully qualified type name of the interface.
	 * @return returns <code>true</code>if the interfaces has been added, <code>false</code>
	 * if the interface already is in the list.
	 * 
	 */
	public boolean addSuperInterface(String superInterface) {
		return fSuperInterfacesDialogField.addElement(new InterfaceWrapper(superInterface));
	}
	
	
	/**
	 * Sets 'Add comment' checkbox. The value set will only be used when creating source when
	 * the comment control is enabled (see {@link #enableCommentControl(boolean)}
	 * 
	 * @param doAddComments if <code>true</code>, comments are added.
	 * @param canBeModified if <code>true</code> check box is
	 * editable; otherwise it is read-only.
	 * 	
	 */	
	public void setAddComments(boolean doAddComments, boolean canBeModified) {
		fAddCommentButton.setSelection(doAddComments);
		fAddCommentButton.setEnabled(canBeModified);
	}
	
	/**
	 * Sets to use the 'Add comment' checkbox value. Clients that use the 'Add comment' checkbox
	 * additionally have to enable the control. This has been added for backwards compatibility.
	 * 
	 * @param useAddCommentValue if <code>true</code>, 
	 * 	
	 */	
	public void enableCommentControl(boolean useAddCommentValue) {
		fUseAddCommentButtonValue= useAddCommentValue;
	}
	
	
	/**
	 * Returns if comments are added. This method can be overridden by clients.
	 * The selection of the comment control is taken if enabled (see {@link #enableCommentControl(boolean)}, otherwise
	 * the settings as specified in the preferences is used.
	 * 
	 * @return Returns <code>true</code> if comments can be added
	 * 
	 */	
	public boolean isAddComments() {
		if (fUseAddCommentButtonValue) {
			return fAddCommentButton.isSelected();
		}
		return StubUtility.doAddComments(getJavaProject()); 
	}
			
	/**
	 * Returns the resource handle that corresponds to the compilation unit to was or
	 * will be created or modified.
	 * @return A resource or null if the page contains illegal values.
	 * 
	 */
	public IResource getModifiedResource() {
		IType enclosing= getEnclosingType();
		if (enclosing != null) {
			return enclosing.getResource();
		}
		IPackageFragment pack= getPackageFragment();
		if (pack != null) {
			String cuName= getCompilationUnitName(getTypeNameWithoutParameters());
			return pack.getJavaScriptUnit(cuName).getResource();
		}
		return null;
	}
			
	// ----------- validation ----------
			
	/*
	 * @see org.eclipse.wst.jsdt.ui.wizards.NewContainerWizardPage#containerChanged()
	 */
	protected IStatus containerChanged() {
		IStatus status= super.containerChanged();
	    IPackageFragmentRoot root= getPackageFragmentRoot();
		if ((fTypeKind == ANNOTATION_TYPE || fTypeKind == ENUM_TYPE) && !status.matches(IStatus.ERROR)) {
	    	if (root != null && !JavaModelUtil.is50OrHigher(root.getJavaScriptProject())) {
	    		// error as createType will fail otherwise (bug 96928)
				return new StatusInfo(IStatus.ERROR, Messages.format(NewWizardMessages.NewTypeWizardPage_warning_NotJDKCompliant, root.getJavaScriptProject().getElementName()));  
	    	}
	    	if (fTypeKind == ENUM_TYPE) {
		    	try {
		    	    // if findType(...) == null then Enum is unavailable
		    	    if (findType(root.getJavaScriptProject(), "java.lang.Enum") == null) //$NON-NLS-1$
		    	        return new StatusInfo(IStatus.WARNING, NewWizardMessages.NewTypeWizardPage_warning_EnumClassNotFound);  
		    	} catch (JavaScriptModelException e) {
		    	    JavaScriptPlugin.log(e);
		    	}
	    	}
	    }
		
		fCurrPackageCompletionProcessor.setPackageFragmentRoot(root);
		if (root != null) {
			fEnclosingTypeCompletionProcessor.setPackageFragment(root.getPackageFragment("")); //$NON-NLS-1$
		}
		return status;
	}
	
	/**
	 * A hook method that gets called when the package field has changed. The method 
	 * validates the package name and returns the status of the validation. The validation
	 * also updates the package fragment model.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 * 
	 * @return the status of the validation
	 */
	protected IStatus packageChanged() {
		StatusInfo status= new StatusInfo();
		IPackageFragmentRoot root= getPackageFragmentRoot();
		fPackageDialogField.enableButton(root != null);
		
		IJavaScriptProject project= root != null ? root.getJavaScriptProject() : null;
		
		String packName= getPackageText();
		if (packName.length() > 0) {
			IStatus val= validatePackageName(packName, project);
			if (val.getSeverity() == IStatus.ERROR) {
				status.setError(Messages.format(NewWizardMessages.NewTypeWizardPage_error_InvalidPackageName, val.getMessage())); 
				return status;
			} else if (val.getSeverity() == IStatus.WARNING) {
				status.setWarning(Messages.format(NewWizardMessages.NewTypeWizardPage_warning_DiscouragedPackageName, val.getMessage())); 
				// continue
			}
		} else {
			status.setWarning(NewWizardMessages.NewTypeWizardPage_warning_DefaultPackageDiscouraged); 
		}
		
		if (project != null) {
			fCurrPackage= root.getPackageFragment(packName);
		} else {
			status.setError(""); //$NON-NLS-1$
		}
		return status;
	}

	/*
	 * Updates the 'default' label next to the package field.
	 */	
	private void updatePackageStatusLabel() {
		String packName= getPackageText();
		
		if (packName.length() == 0) {
			fPackageDialogField.setStatus(NewWizardMessages.NewTypeWizardPage_default); 
		} else {
			fPackageDialogField.setStatus(""); //$NON-NLS-1$
		}
	}
	
	/*
	 * Updates the enable state of buttons related to the enclosing type selection checkbox.
	 */
	private void updateEnableState() {
		boolean enclosing= isEnclosingTypeSelected();
		fPackageDialogField.setEnabled(fCanModifyPackage && !enclosing);
		fEnclosingTypeDialogField.setEnabled(fCanModifyEnclosingType && enclosing);
		if (fTypeKind == ENUM_TYPE || fTypeKind == ANNOTATION_TYPE) {
		    fOtherMdfButtons.enableSelectionButton(ABSTRACT_INDEX, enclosing);
		    fOtherMdfButtons.enableSelectionButton(ENUM_ANNOT_STATIC_INDEX, enclosing);
		}
	}	

	/**
	 * Hook method that gets called when the enclosing type name has changed. The method 
	 * validates the enclosing type and returns the status of the validation. It also updates the 
	 * enclosing type model.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 * 
	 * @return the status of the validation
	 */
	protected IStatus enclosingTypeChanged() {
		StatusInfo status= new StatusInfo();
		fCurrEnclosingType= null;
		
		IPackageFragmentRoot root= getPackageFragmentRoot();
		
		fEnclosingTypeDialogField.enableButton(root != null);
		if (root == null) {
			status.setError(""); //$NON-NLS-1$
			return status;
		}
		
		String enclName= getEnclosingTypeText();
		if (enclName.length() == 0) {
			status.setError(NewWizardMessages.NewTypeWizardPage_error_EnclosingTypeEnterName); 
			return status;
		}
		try {
			IType type= findType(root.getJavaScriptProject(), enclName);
			if (type == null) {
				status.setError(NewWizardMessages.NewTypeWizardPage_error_EnclosingTypeNotExists); 
				return status;
			}

			if (type.getJavaScriptUnit() == null) {
				status.setError(NewWizardMessages.NewTypeWizardPage_error_EnclosingNotInCU); 
				return status;
			}
			if (!JavaModelUtil.isEditable(type.getJavaScriptUnit())) {
				status.setError(NewWizardMessages.NewTypeWizardPage_error_EnclosingNotEditable); 
				return status;			
			}
			
			fCurrEnclosingType= type;
			IPackageFragmentRoot enclosingRoot= JavaModelUtil.getPackageFragmentRoot(type);
			if (!enclosingRoot.equals(root)) {
				status.setWarning(NewWizardMessages.NewTypeWizardPage_warning_EnclosingNotInSourceFolder); 
			}
			return status;
		} catch (JavaScriptModelException e) {
			status.setError(NewWizardMessages.NewTypeWizardPage_error_EnclosingTypeNotExists); 
			JavaScriptPlugin.log(e);
			return status;
		}
	}
	
	private IType findType(IJavaScriptProject project, String typeName) throws JavaScriptModelException {
		if (project.exists()) {
			return project.findType(typeName);
		}
		return null;
	}
	
	private String getTypeNameWithoutParameters() {
		String typeNameWithParameters= getTypeName();
		int angleBracketOffset= typeNameWithParameters.indexOf('<');
		if (angleBracketOffset == -1) {
			return typeNameWithParameters;
		} else {
			return typeNameWithParameters.substring(0, angleBracketOffset);
		}
	}
	
	/**
	 * Hook method that is called when evaluating the name of the compilation unit to create. By default, a file extension
	 * <code>java</code> is added to the given type name, but implementors can override this behavior.
	 * 
	 * @param typeName the name of the type to create the compilation unit for.
	 * @return the name of the compilation unit to be created for the given name
	 * 
	 * 
	 */
	protected String getCompilationUnitName(String typeName) {
		return typeName + JavaModelUtil.DEFAULT_CU_SUFFIX;
	}
	
	
	/**
	 * Hook method that gets called when the type name has changed. The method validates the 
	 * type name and returns the status of the validation.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 * 
	 * @return the status of the validation
	 */
	protected IStatus typeNameChanged() {
		StatusInfo status= new StatusInfo();
		fCurrType= null;
		String typeNameWithParameters= getTypeName();
		// must not be empty
		if (typeNameWithParameters.length() == 0) {
			status.setError(NewWizardMessages.NewTypeWizardPage_error_EnterTypeName); 
			return status;
		}
		
		String typeName= getTypeNameWithoutParameters();
		if (typeName.indexOf('.') != -1) {
			status.setError(NewWizardMessages.NewTypeWizardPage_error_QualifiedName); 
			return status;
		}
		
		IJavaScriptProject project= getJavaProject();	
		IStatus val= validateJavaTypeName(typeName, project);
		if (val.getSeverity() == IStatus.ERROR) {
			status.setError(Messages.format(NewWizardMessages.NewTypeWizardPage_error_InvalidTypeName, val.getMessage())); 
			return status;
		} else if (val.getSeverity() == IStatus.WARNING) {
			status.setWarning(Messages.format(NewWizardMessages.NewTypeWizardPage_warning_TypeNameDiscouraged, val.getMessage())); 
			// continue checking
		}		

		// must not exist
		if (!isEnclosingTypeSelected()) {
			IPackageFragment pack= getPackageFragment();
			if (pack != null) {
				IJavaScriptUnit cu= pack.getJavaScriptUnit(getCompilationUnitName(typeName));
				fCurrType= cu.getType(typeName);
				IResource resource= cu.getResource();

				if (resource.exists()) {
					status.setError(NewWizardMessages.NewTypeWizardPage_error_TypeNameExists); 
					return status;
				}
				URI location= resource.getLocationURI();
				if (location != null) {
					try {
						IFileStore store= EFS.getStore(location);
						if (store.fetchInfo().exists()) {
							status.setError(NewWizardMessages.NewTypeWizardPage_error_TypeNameExistsDifferentCase); 
							return status;
						}
					} catch (CoreException e) {
						status.setError(Messages.format(
							NewWizardMessages.NewTypeWizardPage_error_uri_location_unkown, 
							Resources.getLocationString(resource)));
					}
				}
			}
		} else {
			IType type= getEnclosingType();
			if (type != null) {
				fCurrType= type.getType(typeName);
				if (fCurrType.exists()) {
					status.setError(NewWizardMessages.NewTypeWizardPage_error_TypeNameExists); 
					return status;
				}
			}
		}
		
		if (!typeNameWithParameters.equals(typeName) && project != null) {
			if (!JavaModelUtil.is50OrHigher(project)) {
				status.setError(NewWizardMessages.NewTypeWizardPage_error_TypeParameters); 
				return status;
			}
			String typeDeclaration= "class " + typeNameWithParameters + " {}"; //$NON-NLS-1$//$NON-NLS-2$
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setSource(typeDeclaration.toCharArray());
			parser.setProject(project);
			JavaScriptUnit compilationUnit= (JavaScriptUnit) parser.createAST(null);
			IProblem[] problems= compilationUnit.getProblems();
			if (problems.length > 0) {
				status.setError(Messages.format(NewWizardMessages.NewTypeWizardPage_error_InvalidTypeName, problems[0].getMessage())); 
				return status;
			}
		}
		return status;
	}
	
	/**
	 * Hook method that gets called when the superclass name has changed. The method 
	 * validates the superclass name and returns the status of the validation.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 * 
	 * @return the status of the validation
	 */
	protected IStatus superClassChanged() {
		StatusInfo status= new StatusInfo();
		IPackageFragmentRoot root= getPackageFragmentRoot();
		fSuperClassDialogField.enableButton(root != null);
		
		fSuperClassStubTypeContext= null;
		
		String sclassName= getSuperClass();
		if (sclassName.length() == 0) {
			// accept the empty field (stands for java.lang.Object)
			return status;
		}
		
		if (root != null) {
			Type type= TypeContextChecker.parseSuperClass(sclassName);
			if (type == null) {
				status.setError(NewWizardMessages.NewTypeWizardPage_error_InvalidSuperClassName); 
				return status;
			}
		} else {
			status.setError(""); //$NON-NLS-1$
		}
		return status;
	}

	private StubTypeContext getSuperClassStubTypeContext() {
		if (fSuperClassStubTypeContext == null) {
			String typeName;
			if (fCurrType != null) {
				typeName= getTypeName();
			} else {
				typeName= JavaTypeCompletionProcessor.DUMMY_CLASS_NAME;
			}
			fSuperClassStubTypeContext= TypeContextChecker.createSuperClassStubTypeContext(typeName, getEnclosingType(), getPackageFragment());
		}
		return fSuperClassStubTypeContext;
	}

	/**
	 * Hook method that gets called when the list of super interface has changed. The method 
	 * validates the super interfaces and returns the status of the validation.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 * 
	 * @return the status of the validation
	 */
	protected IStatus superInterfacesChanged() {
		StatusInfo status= new StatusInfo();
		
		IPackageFragmentRoot root= getPackageFragmentRoot();
		fSuperInterfacesDialogField.enableButton(0, root != null);
						
		if (root != null) {
			List elements= fSuperInterfacesDialogField.getElements();
			int nElements= elements.size();
			for (int i= 0; i < nElements; i++) {
				String intfname= ((InterfaceWrapper) elements.get(i)).interfaceName;
				Type type= TypeContextChecker.parseSuperInterface(intfname);
				if (type == null) {
					status.setError(Messages.format(NewWizardMessages.NewTypeWizardPage_error_InvalidSuperInterfaceName, intfname)); 
					return status;
				}
			}				
		}
		return status;
	}

	private StubTypeContext getSuperInterfacesStubTypeContext() {
		if (fSuperInterfaceStubTypeContext == null) {
			String typeName;
			if (fCurrType != null) {
				typeName= getTypeName();
			} else {
				typeName= JavaTypeCompletionProcessor.DUMMY_CLASS_NAME;
			}
			fSuperInterfaceStubTypeContext= TypeContextChecker.createSuperInterfaceStubTypeContext(typeName, getEnclosingType(), getPackageFragment());
		}
		return fSuperInterfaceStubTypeContext;
	}
	
	/**
	 * Hook method that gets called when the modifiers have changed. The method validates 
	 * the modifiers and returns the status of the validation.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 * 
	 * @return the status of the validation
	 */
	protected IStatus modifiersChanged() {
		StatusInfo status= new StatusInfo();
		return status;
	}
	
	// selection dialogs

	/**
	 * Opens a selection dialog that allows to select a package. 
	 * 
	 * @return returns the selected package or <code>null</code> if the dialog has been canceled.
	 * The caller typically sets the result to the package input field.
	 * <p>
	 * Clients can override this method if they want to offer a different dialog.
	 * </p>
	 * 
	 * 
	 */
	protected IPackageFragment choosePackage() {
		IPackageFragmentRoot froot= getPackageFragmentRoot();
		IJavaScriptElement[] packages= null;
		try {
			if (froot != null && froot.exists()) {
				packages= froot.getChildren();
			}
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
		}
		if (packages == null) {
			packages= new IJavaScriptElement[0];
		}
		
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaScriptElementLabelProvider(JavaScriptElementLabelProvider.SHOW_DEFAULT));
		dialog.setIgnoreCase(false);
		dialog.setTitle(NewWizardMessages.NewTypeWizardPage_ChoosePackageDialog_title); 
		dialog.setMessage(NewWizardMessages.NewTypeWizardPage_ChoosePackageDialog_description); 
		dialog.setEmptyListMessage(NewWizardMessages.NewTypeWizardPage_ChoosePackageDialog_empty); 
		dialog.setElements(packages);
		dialog.setHelpAvailable(false);
		
		IPackageFragment pack= getPackageFragment();
		if (pack != null) {
			dialog.setInitialSelections(new Object[] { pack });
		}

		if (dialog.open() == Window.OK) {
			return (IPackageFragment) dialog.getFirstResult();
		}
		return null;
	}
	
	/**
	 * Opens a selection dialog that allows to select an enclosing type. 
	 * 
	 * @return returns the selected type or <code>null</code> if the dialog has been canceled.
	 * The caller typically sets the result to the enclosing type input field.
	 * <p>
	 * Clients can override this method if they want to offer a different dialog.
	 * </p>
	 * 
	 * 
	 */
	protected IType chooseEnclosingType() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null) {
			return null;
		}
		
		IJavaScriptSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaScriptElement[] { root });
	
		FilteredTypesSelectionDialog dialog= new FilteredTypesSelectionDialog(getShell(), 
			false, getWizard().getContainer(), scope, IJavaScriptSearchConstants.TYPE);
		dialog.setTitle(NewWizardMessages.NewTypeWizardPage_ChooseEnclosingTypeDialog_title); 
		dialog.setMessage(NewWizardMessages.NewTypeWizardPage_ChooseEnclosingTypeDialog_description); 
		dialog.setInitialPattern(Signature.getSimpleName(getEnclosingTypeText()));
		
		if (dialog.open() == Window.OK) {	
			return (IType) dialog.getFirstResult();
		}
		return null;
	}	
	
	/**
	 * Opens a selection dialog that allows to select a super class. 
	 * 
	 * @return returns the selected type or <code>null</code> if the dialog has been canceled.
	 * The caller typically sets the result to the super class input field.
	 * 	<p>
	 * Clients can override this method if they want to offer a different dialog.
	 * </p>
	 * 
	 * 
	 */
	protected IType chooseSuperClass() {
		IJavaScriptProject project= getJavaProject();
		if (project == null) {
			return null;
		}
		
		IJavaScriptElement[] elements= new IJavaScriptElement[] { project };
		IJavaScriptSearchScope scope= SearchEngine.createJavaSearchScope(elements);

		FilteredTypesSelectionDialog dialog= new FilteredTypesSelectionDialog(getShell(), false,
			getWizard().getContainer(), scope, IJavaScriptSearchConstants.CLASS);
		dialog.setTitle(NewWizardMessages.NewTypeWizardPage_SuperClassDialog_title); 
		dialog.setMessage(NewWizardMessages.NewTypeWizardPage_SuperClassDialog_message); 
		dialog.setInitialPattern(getSuperClass());

		if (dialog.open() == Window.OK) {
			return (IType) dialog.getFirstResult();
		}
		return null;
	}
		
	// ---- creation ----------------

	/**
	 * Creates the new type using the entered field values.
	 * 
	 * @param monitor a progress monitor to report progress.
	 * @throws CoreException Thrown when the creation failed.
	 * @throws InterruptedException Thrown when the operation was canceled.
	 */
	public void createType(IProgressMonitor monitor) throws CoreException, InterruptedException {		
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}

		monitor.beginTask(NewWizardMessages.NewTypeWizardPage_operationdesc, 8); 
		
		IPackageFragmentRoot root= getPackageFragmentRoot();
		IPackageFragment pack= getPackageFragment();
		if (pack == null) {
			pack= root.getPackageFragment(""); //$NON-NLS-1$
		}
		
		if (!pack.exists()) {
			String packName= pack.getElementName();
			pack= root.createPackageFragment(packName, true, new SubProgressMonitor(monitor, 1));
		} else {
			monitor.worked(1);
		}
		
		boolean needsSave;
		IJavaScriptUnit connectedCU= null;
		
		try {	
			String typeName= getTypeNameWithoutParameters();
			
			boolean isInnerClass= isEnclosingTypeSelected();
		
			IType createdType;
			ImportsManager imports;
			int indent= 0;

			Set /* String (import names) */ existingImports;
			
			String lineDelimiter= null;	
			if (!isInnerClass) {
				lineDelimiter= StubUtility.getLineDelimiterUsed(pack.getJavaScriptProject());
				
				String cuName= getCompilationUnitName(typeName);
				IJavaScriptUnit parentCU= pack.createCompilationUnit(cuName, "", false, new SubProgressMonitor(monitor, 2)); //$NON-NLS-1$
				// create a working copy with a new owner
				
				needsSave= true;
				parentCU.becomeWorkingCopy(new SubProgressMonitor(monitor, 1)); // cu is now a (primary) working copy
				connectedCU= parentCU;
				
				IBuffer buffer= parentCU.getBuffer();
				
				String simpleTypeStub= constructSimpleTypeStub();
				String cuContent= constructCUContent(parentCU, simpleTypeStub, lineDelimiter);
				buffer.setContents(cuContent);
				
				JavaScriptUnit astRoot= createASTForImports(parentCU);
				existingImports= getExistingImports(astRoot);
							
				imports= new ImportsManager(astRoot);
				// add an import that will be removed again. Having this import solves 14661
				imports.addImport(JavaModelUtil.concatenateName(pack.getElementName(), typeName));
				
				String typeContent= constructTypeStub(parentCU, imports, lineDelimiter);
				
				int index= cuContent.lastIndexOf(simpleTypeStub);
				if (index == -1) {
					AbstractTypeDeclaration typeNode= (AbstractTypeDeclaration) astRoot.types().get(0);
					int start= ((ASTNode) typeNode.modifiers().get(0)).getStartPosition();
					int end= typeNode.getStartPosition() + typeNode.getLength();
					buffer.replace(start, end - start, typeContent);
				} else {
					buffer.replace(index, simpleTypeStub.length(), typeContent);
				}
				
				createdType= parentCU.getType(typeName);
			} else {
				IType enclosingType= getEnclosingType();
				
				IJavaScriptUnit parentCU= enclosingType.getJavaScriptUnit();
				
				needsSave= !parentCU.isWorkingCopy();
				parentCU.becomeWorkingCopy(new SubProgressMonitor(monitor, 1)); // cu is now for sure (primary) a working copy
				connectedCU= parentCU;
				
				JavaScriptUnit astRoot= createASTForImports(parentCU);
				imports= new ImportsManager(astRoot);
				existingImports= getExistingImports(astRoot);

	
				// add imports that will be removed again. Having the imports solves 14661
				IType[] topLevelTypes= parentCU.getTypes();
				for (int i= 0; i < topLevelTypes.length; i++) {
					imports.addImport(topLevelTypes[i].getFullyQualifiedName('.'));
				}
				
				lineDelimiter= StubUtility.getLineDelimiterUsed(enclosingType);
				StringBuffer content= new StringBuffer();
				
				String comment= getTypeComment(parentCU, lineDelimiter);
				if (comment != null) {
					content.append(comment);
					content.append(lineDelimiter);
				}

				content.append(constructTypeStub(parentCU, imports, lineDelimiter));
				IJavaScriptElement sibling= null;
				
				IJavaScriptElement[] elems= enclosingType.getChildren();
				sibling = elems.length > 0 ? elems[0] : null;
				
				createdType= enclosingType.createType(content.toString(), sibling, false, new SubProgressMonitor(monitor, 2));
			
				indent= StubUtility.getIndentUsed(enclosingType) + 1;
			}
			if (monitor.isCanceled()) {
				throw new InterruptedException();
			}
			
			// add imports for superclass/interfaces, so types can be resolved correctly
			
			IJavaScriptUnit cu= createdType.getJavaScriptUnit();	
			
			imports.create(false, new SubProgressMonitor(monitor, 1));
				
			JavaModelUtil.reconcile(cu);

			if (monitor.isCanceled()) {
				throw new InterruptedException();
			}
			
			// set up again
			JavaScriptUnit astRoot= createASTForImports(imports.getCompilationUnit());
			imports= new ImportsManager(astRoot);
			
			createTypeMembers(createdType, imports, new SubProgressMonitor(monitor, 1));
	
			// add imports
			imports.create(false, new SubProgressMonitor(monitor, 1));
			
			removeUnusedImports(cu, existingImports, false);
			
			JavaModelUtil.reconcile(cu);
			
			ISourceRange range= createdType.getSourceRange();
			
			IBuffer buf= cu.getBuffer();
			String originalContent= buf.getText(range.getOffset(), range.getLength());
			
			String formattedContent= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, originalContent, indent, null, lineDelimiter, pack.getJavaScriptProject());
			formattedContent= Strings.trimLeadingTabsAndSpaces(formattedContent);
			buf.replace(range.getOffset(), range.getLength(), formattedContent);
			if (!isInnerClass) {
				String fileComment= getFileComment(cu);
				if (fileComment != null && fileComment.length() > 0) {
					buf.replace(0, 0, fileComment + lineDelimiter);
				}
			}
			fCreatedType= createdType;

			if (needsSave) {
				cu.commitWorkingCopy(true, new SubProgressMonitor(monitor, 1));
			} else {
				monitor.worked(1);
			}
			
		} finally {
			if (connectedCU != null) {
				connectedCU.discardWorkingCopy();
			}
			monitor.done();
		}
	}	
	
	private JavaScriptUnit createASTForImports(IJavaScriptUnit cu) {
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setSource(cu);
		parser.setResolveBindings(false);
		parser.setFocalPosition(0);
		return (JavaScriptUnit) parser.createAST(null);
	}
	
	
	private Set /* String */ getExistingImports(JavaScriptUnit root) {
		List imports= root.imports();
		Set res= new HashSet(imports.size());
		for (int i= 0; i < imports.size(); i++) {
			res.add(ASTNodes.asString((ImportDeclaration) imports.get(i)));
		}
		return res;
	}

	private void removeUnusedImports(IJavaScriptUnit cu, Set existingImports, boolean needsSave) throws CoreException {
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setSource(cu);
		parser.setResolveBindings(true);
		
		JavaScriptUnit root= (JavaScriptUnit) parser.createAST(null);
		if (root.getProblems().length == 0) {
			return;
		}
		
		List importsDecls= root.imports();
		if (importsDecls.isEmpty()) {
			return;
		}
		ImportsManager imports= new ImportsManager(root);
		
		int importsEnd= ASTNodes.getExclusiveEnd((ASTNode) importsDecls.get(importsDecls.size() - 1));
		IProblem[] problems= root.getProblems();
		for (int i= 0; i < problems.length; i++) {
			IProblem curr= problems[i];
			if (curr.getSourceEnd() < importsEnd) {
				int id= curr.getID();
				if( id == IProblem.NotVisibleType) { // not visible problems hide unused -> remove both
					int pos= curr.getSourceStart();
					for (int k= 0; k < importsDecls.size(); k++) {
						ImportDeclaration decl= (ImportDeclaration) importsDecls.get(k);
						if (decl.getStartPosition() <= pos && pos < decl.getStartPosition() + decl.getLength()) {
							if (existingImports.isEmpty() || !existingImports.contains(ASTNodes.asString(decl))) {
								String name= decl.getName().getFullyQualifiedName();
								if (decl.isOnDemand()) {
									name += ".*"; //$NON-NLS-1$
								}
								if (decl.isStatic()) {
									imports.removeStaticImport(name);
								} else {
									imports.removeImport(name);
								}
							}
							break;
						}
					}
				}
			}
		}
		imports.create(needsSave, null);
	}

	/**
	 * Uses the New JavaScript file template from the code template page to generate a
	 * compilation unit with the given type content.
	 * @param cu The new created compilation unit
	 * @param typeContent The content of the type, including signature and type
	 * body.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return String Returns the result of evaluating the new file template
	 * with the given type content.
	 * @throws CoreException
	 * 
	 */
	protected String constructCUContent(IJavaScriptUnit cu, String typeContent, String lineDelimiter) throws CoreException {
		String fileComment= getFileComment(cu, lineDelimiter);
		String typeComment= getTypeComment(cu, lineDelimiter);
		IPackageFragment pack= (IPackageFragment) cu.getParent();
		String content= CodeGeneration.getCompilationUnitContent(cu, fileComment, typeComment, typeContent, lineDelimiter);
		if (content != null) {
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setProject(cu.getJavaScriptProject());
			parser.setSource(content.toCharArray());
			JavaScriptUnit unit= (JavaScriptUnit) parser.createAST(null);
			if ((pack.isDefaultPackage() || unit.getPackage() != null) && !unit.types().isEmpty()) {
				return content;
			}
		}
		StringBuffer buf= new StringBuffer();
		if (!pack.isDefaultPackage()) {
			buf.append("package ").append(pack.getElementName()).append(';'); //$NON-NLS-1$
		}
		buf.append(lineDelimiter).append(lineDelimiter);
		if (typeComment != null) {
			buf.append(typeComment).append(lineDelimiter);
		}
		buf.append(typeContent);
		return buf.toString();
	}
	

	/**
	 * Returns the created type or <code>null</code> is the type has not been created yet. The method
	 * only returns a valid type after <code>createType</code> has been called.
	 * 
	 * @return the created type
	 * @see #createType(IProgressMonitor)
	 */			
	public IType getCreatedType() {
		return fCreatedType;
	}
	
	// ---- construct CU body----------------
		
	private void writeSuperClass(StringBuffer buf, ImportsManager imports) {
		String superclass= getSuperClass();
		if (fTypeKind == CLASS_TYPE && superclass.length() > 0 && !"java.lang.Object".equals(superclass)) { //$NON-NLS-1$
			buf.append(" extends "); //$NON-NLS-1$
			
			ITypeBinding binding= null;
			if (fCurrType != null) {
				binding= TypeContextChecker.resolveSuperClass(superclass, fCurrType, getSuperClassStubTypeContext());
			}
			if (binding != null) {
				buf.append(imports.addImport(binding));
			} else {
				buf.append(imports.addImport(superclass));
			}
		}
	}
	
	private void writeSuperInterfaces(StringBuffer buf, ImportsManager imports) {
		List interfaces= getSuperInterfaces();
		int last= interfaces.size() - 1;
		if (last >= 0) {
		    if (fTypeKind != INTERFACE_TYPE) {
				buf.append(" implements "); //$NON-NLS-1$
			} else {
				buf.append(" extends "); //$NON-NLS-1$
			}
			String[] intfs= (String[]) interfaces.toArray(new String[interfaces.size()]);
			ITypeBinding[] bindings;
			if (fCurrType != null) {
				bindings= TypeContextChecker.resolveSuperInterfaces(intfs, fCurrType, getSuperInterfacesStubTypeContext());
			} else {
				bindings= new ITypeBinding[intfs.length];
			}
			for (int i= 0; i <= last; i++) {
				ITypeBinding binding= bindings[i];
				if (binding != null) {
					buf.append(imports.addImport(binding));
				} else {
					buf.append(imports.addImport(intfs[i]));
				}
				if (i < last) {
					buf.append(',');
				}
			}
		}
	}
	
	
	private String constructSimpleTypeStub() {
		StringBuffer buf= new StringBuffer("public class "); //$NON-NLS-1$
		buf.append(getTypeName());
		buf.append("{ }"); //$NON-NLS-1$
		return buf.toString();
	}
	
	/*
	 * Called from createType to construct the source for this type
	 */		
	private String constructTypeStub(IJavaScriptUnit parentCU, ImportsManager imports, String lineDelimiter) throws CoreException {
		StringBuffer buf= new StringBuffer();
		
		int modifiers= getModifiers();
		buf.append(Flags.toString(modifiers));
		if (modifiers != 0) {
			buf.append(' ');
		}
		String type= ""; //$NON-NLS-1$
		String templateID= ""; //$NON-NLS-1$
		switch (fTypeKind) {
			case CLASS_TYPE: 
				type= "class ";  //$NON-NLS-1$
				templateID= CodeGeneration.CLASS_BODY_TEMPLATE_ID;
				break;
		}
		buf.append(type);
		buf.append(getTypeName());
		writeSuperClass(buf, imports);
		writeSuperInterfaces(buf, imports);	

		buf.append(" {").append(lineDelimiter); //$NON-NLS-1$
		String typeBody= CodeGeneration.getTypeBody(templateID, parentCU, getTypeName(), lineDelimiter);
		if (typeBody != null) {
			buf.append(typeBody);
		} else {
			buf.append(lineDelimiter);
		}
		buf.append('}').append(lineDelimiter);
		return buf.toString();
	}
	
	/**
	 * Hook method that gets called from <code>createType</code> to support adding of 
	 * unanticipated methods, fields, and inner types to the created type.
	 * <p>
	 * Implementers can use any methods defined on <code>IType</code> to manipulate the
	 * new type.
	 * </p>
	 * <p>
	 * The source code of the new type will be formatted using the platform's formatter. Needed 
	 * imports are added by the wizard at the end of the type creation process using the given 
	 * import manager.
	 * </p>
	 * 
	 * @param newType the new type created via <code>createType</code>
	 * @param imports an import manager which can be used to add new imports
	 * @param monitor a progress monitor to report progress. Must not be <code>null</code>
	 * @throws CoreException thrown when creation of the type members failed
	 * 
	 * @see #createType(IProgressMonitor)
	 */		
	protected void createTypeMembers(IType newType, final ImportsManager imports, IProgressMonitor monitor) throws CoreException {
		// default implementation does nothing
		// example would be
		// String mainMathod= "public void foo(Vector vec) {}"
		// createdType.createMethod(main, null, false, null);
		// imports.addImport("java.lang.Vector");
	}
	
		
	/**
	 * @param parentCU the current compilation unit
	 * @return returns the file template or <code>null</code>
	 * @deprecated Instead of file templates, the new type code template
	 * specifies the stub for a compilation unit.
	 */		
	protected String getFileComment(IJavaScriptUnit parentCU) {
		return null;
	}
	
	/**
	 * Hook method that gets called from <code>createType</code> to retrieve 
	 * a file comment. This default implementation returns the content of the 
	 * 'file comment' template or <code>null</code> if no comment should be created.
	 * 
	 * @param parentCU the parent compilation unit
	 * @param lineDelimiter the line delimiter to use
	 * @return the file comment or <code>null</code> if a file comment 
	 * is not desired
	 * @throws CoreException 
     *
     * 
	 */		
	protected String getFileComment(IJavaScriptUnit parentCU, String lineDelimiter) throws CoreException {
		if (isAddComments()) {
			return CodeGeneration.getFileComment(parentCU, lineDelimiter);
		}
		return null;
		
	}
	
	private boolean isValidComment(String template) {
		IScanner scanner= ToolFactory.createScanner(true, false, false, false);
		scanner.setSource(template.toCharArray());
		try {
			int next= scanner.getNextToken();
			while (TokenScanner.isComment(next)) {
				next= scanner.getNextToken();
			}
			return next == ITerminalSymbols.TokenNameEOF;
		} catch (InvalidInputException e) {
		}
		return false;
	}
	
	/**
	 * Hook method that gets called from <code>createType</code> to retrieve 
	 * a type comment. This default implementation returns the content of the 
	 * 'type comment' template.
	 * 
	 * @param parentCU the parent compilation unit
	 * @param lineDelimiter the line delimiter to use
	 * @return the type comment or <code>null</code> if a type comment 
	 * is not desired
     *
     * 
	 */		
	protected String getTypeComment(IJavaScriptUnit parentCU, String lineDelimiter) {
		if (isAddComments()) {
			try {
				StringBuffer typeName= new StringBuffer();
				if (isEnclosingTypeSelected()) {
					typeName.append(JavaModelUtil.getTypeQualifiedName(getEnclosingType())).append('.');
				}
				typeName.append(getTypeNameWithoutParameters());
				String comment= CodeGeneration.getTypeComment(parentCU, typeName.toString(), lineDelimiter);
				if (comment != null && isValidComment(comment)) {
					return comment;
				}
			} catch (CoreException e) {
				JavaScriptPlugin.log(e);
			}
		}
		return null;
	}

	/**
	 * Returns the string resulting from evaluation the given template in
	 * the context of the given compilation unit. This accesses the normal
	 * template page, not the code templates. To use code templates use
	 * <code>constructCUContent</code> to construct a compilation unit stub or
	 * getTypeComment for the comment of the type.
	 * 
	 * @param name the template to be evaluated
	 * @param parentCU the templates evaluation context
	 * @param pos a source offset into the parent compilation unit. The
	 * template is evaluated at the given source offset
	 * @return return the template with the given name or <code>null</code> if the template could not be found.
	 */
	protected String getTemplate(String name, IJavaScriptUnit parentCU, int pos) {
		try {
			Template template= JavaScriptPlugin.getDefault().getTemplateStore().findTemplate(name);
			if (template != null) {
				return JavaContext.evaluateTemplate(template, parentCU, pos);
			}
		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
		} catch (BadLocationException e) {
			JavaScriptPlugin.log(e);
		} catch (TemplateException e) {
			JavaScriptPlugin.log(e);
		}
		return null;
	}	
	

	/**
	 * Creates the bodies of all unimplemented methods and constructors and adds them to the type.
	 * Method is typically called by implementers of <code>NewTypeWizardPage</code> to add
	 * needed method and constructors.
	 * 
	 * @param type the type for which the new methods and constructor are to be created
	 * @param doConstructors if <code>true</code> unimplemented constructors are created
	 * @param doUnimplementedMethods if <code>true</code> unimplemented methods are created
	 * @param imports an import manager to add all needed import statements
	 * @param monitor a progress monitor to report progress
	 * @return the created methods.
	 * @throws CoreException thrown when the creation fails.
	 */
	protected IFunction[] createInheritedMethods(IType type, boolean doConstructors, boolean doUnimplementedMethods, ImportsManager imports, IProgressMonitor monitor) throws CoreException {
		final IJavaScriptUnit cu= type.getJavaScriptUnit();
		JavaModelUtil.reconcile(cu);
		IFunction[] typeMethods= type.getFunctions();
		Set handleIds= new HashSet(typeMethods.length);
		for (int index= 0; index < typeMethods.length; index++)
			handleIds.add(typeMethods[index].getHandleIdentifier());
		ArrayList newMethods= new ArrayList();
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(type.getJavaScriptProject());
		settings.createComments= isAddComments();
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setResolveBindings(true);
		parser.setSource(cu);
		JavaScriptUnit unit= (JavaScriptUnit) parser.createAST(new SubProgressMonitor(monitor, 1));
		final ITypeBinding binding= ASTNodes.getTypeBinding(unit, type);
		if (binding != null) {
			if (doUnimplementedMethods) {
				AddUnimplementedMethodsOperation operation= new AddUnimplementedMethodsOperation(unit, binding, null, -1, false, true, false);
				operation.setCreateComments(isAddComments());
				operation.run(monitor);
				createImports(imports, operation.getCreatedImports());
			}
			if (doConstructors) {
				AddUnimplementedConstructorsOperation operation= new AddUnimplementedConstructorsOperation(unit, binding, null, -1, false, true, false);
				operation.setOmitSuper(true);
				operation.setCreateComments(isAddComments());
				operation.run(monitor);
				createImports(imports, operation.getCreatedImports());
			}
		}
		JavaModelUtil.reconcile(cu);
		typeMethods= type.getFunctions();
		for (int index= 0; index < typeMethods.length; index++)
			if (!handleIds.contains(typeMethods[index].getHandleIdentifier()))
				newMethods.add(typeMethods[index]);
		IFunction[] methods= new IFunction[newMethods.size()];
		newMethods.toArray(methods);
		return methods;
	}

	private void createImports(ImportsManager imports, String[] createdImports) {
		for (int index= 0; index < createdImports.length; index++)
			imports.addImport(createdImports[index]);
	}

	
	// ---- creation ----------------

	/**
	 * Returns the runnable that creates the type using the current settings.
	 * The returned runnable must be executed in the UI thread.
	 * 
	 * @return the runnable to create the new type
	 */		
	public IRunnableWithProgress getRunnable() {				
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					if (monitor == null) {
						monitor= new NullProgressMonitor();
					}
					createType(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} 				
			}
		};
	}
}
