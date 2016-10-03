/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Renaud Waldura &lt;renaud+eclipse@waldura.com&gt;
 *     IBM Corporation - updates
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.correction;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewClassCreationWizard;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.wizards.NewTypeWizardPage;

/**
 * This proposal is listed in the corrections list for a "type not found" problem.
 * It offers to create a new type by running the class/interface wizard.
 * If selected, this proposal will open a {@link NewClassCreationWizard},
 * {@link NewInterfaceCreationWizard}, {@link NewEnumCreationWizard} or {@link NewAnnotationCreationWizard}.
 *
 * @see UnresolvedElementsSubProcessor#getTypeProposals(org.eclipse.wst.jsdt.ui.text.java.IInvocationContext, org.eclipse.wst.jsdt.ui.text.java.IProblemLocation, java.util.Collection)
 */

public class NewCUCompletionUsingWizardProposal extends ChangeCorrectionProposal {

	public static final int K_CLASS= 1;
	public static final int K_INTERFACE= 2;
	public static final int K_ENUM= 3;
	public static final int K_ANNOTATION= 4;

	private Name fNode;
	private IJavaScriptUnit fCompilationUnit;
	private int fTypeKind;
	private IJavaScriptElement fTypeContainer; // IType or IPackageFragment
	private String fTypeNameWithParameters;
	private IType fCreatedType;

	private boolean fShowDialog;

	public NewCUCompletionUsingWizardProposal(IJavaScriptUnit cu, Name node, int typeKind, IJavaScriptElement typeContainer, int severity) {
		super("", null, severity, null); //$NON-NLS-1$

		fCompilationUnit= cu;
		fNode= node;
		fTypeKind= typeKind;
		fTypeContainer= typeContainer;
		fTypeNameWithParameters= getTypeName(typeKind, node);
		
		fCreatedType= null;
		
		String containerName= ASTNodes.getQualifier(node);
		String typeName= fTypeNameWithParameters;
		boolean isInnerType= typeContainer instanceof IType;
		switch (typeKind) {
		case K_CLASS:
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CLASS));
			if (isInnerType) {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerclass_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerclass_intype_description, new String[] { typeName, containerName }));
				}
			} else {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createclass_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createclass_inpackage_description, new String[] { typeName, containerName }));
				}
			}
			break;
		case K_INTERFACE:
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_INTERFACE));
			if (isInnerType) {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerinterface_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerinterface_intype_description, new String[] { typeName, containerName }));
				}
			} else {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinterface_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinterface_inpackage_description, new String[] { typeName, containerName }));
				}
			}
			break;
		case K_ENUM:
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ENUM));
			if (isInnerType) {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerenum_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerenum_intype_description, new String[] { typeName, containerName }));
				}
			} else {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createenum_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createenum_inpackage_description, new String[] { typeName, containerName }));
				}
			}
			break;
		case K_ANNOTATION:
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ANNOTATION));
			if (isInnerType) {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerannotation_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinnerannotation_intype_description, new String[] { typeName, containerName }));
				}
			} else {
				if (containerName.length() == 0) {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createannotation_description, typeName));
				} else {
					setDisplayName(Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createannotation_inpackage_description, new String[] { typeName, containerName }));
				}
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown type kind"); //$NON-NLS-1$
		}
		fShowDialog= true;
	}
	
	private static String getTypeName(int typeKind, Name node) {
		String name= ASTNodes.getSimpleNameIdentifier(node);
		return name;
	}
	

	public void apply(IDocument document) {
//		NewElementWizard wizard= null;
//		wizard.init(JavaScriptPlugin.getDefault().getWorkbench(), new StructuredSelection(fCompilationUnit));
//
//		IType createdType= null;
//		
//		if (fShowDialog) {
//			Shell shell= JavaScriptPlugin.getActiveWorkbenchShell();
//			WizardDialog dialog= new WizardDialog(shell, wizard);
//			PixelConverter converter= new PixelConverter(JFaceResources.getDialogFont());
//			dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70), converter.convertHeightInCharsToPixels(20));
//			dialog.create();
//			dialog.getShell().setText(CorrectionMessages.NewCUCompletionUsingWizardProposal_dialogtitle);
//
//			configureWizardPage(wizard);
//			if (dialog.open() == Window.OK) {
//				createdType= (IType) wizard.getCreatedElement();
//			}
//		} else {
//			wizard.addPages();
//			try {
//				NewTypeWizardPage page= configureWizardPage(wizard);
//				page.createType(null);
//				createdType= page.getCreatedType();
//			} catch (CoreException e) {
//				JavaScriptPlugin.log(e);
//			} catch (InterruptedException e) {
//			}
//		}
//		
//		if (createdType != null) {
//			IJavaScriptElement container= createdType.getParent();
//			if (container instanceof IJavaScriptUnit) {
//				container= container.getParent();
//			}
//			if (!container.equals(fTypeContainer)) {
//				// add import
//				try {
//					ImportRewrite rewrite= StubUtility.createImportRewrite(fCompilationUnit, true);
//					rewrite.addImport(createdType.getFullyQualifiedName('.'));
//					JavaModelUtil.applyEdit(fCompilationUnit, rewrite.rewriteImports(null), false, null);
//				} catch (CoreException e) {
//				}
//			}
//			fCreatedType= createdType;
//		}
		
	}

	private NewTypeWizardPage configureWizardPage(NewElementWizard wizard) {
		IWizardPage[] pages= wizard.getPages();
		Assert.isTrue(pages.length > 0 && pages[0] instanceof NewTypeWizardPage);

		NewTypeWizardPage page= (NewTypeWizardPage) pages[0];
		fillInWizardPageName(page);
		fillInWizardPageSuperTypes(page);
		return page;
	}

	/**
	 * Fill-in the "Package" and "Name" fields.
	 * @param page the wizard page.
	 */
	private void fillInWizardPageName(NewTypeWizardPage page) {
		// allow to edit when there are type parameters
		page.setTypeName(fTypeNameWithParameters, fTypeNameWithParameters.indexOf('<') != -1);

		boolean isInEnclosingType= fTypeContainer instanceof IType;
		if (isInEnclosingType) {
			page.setEnclosingType((IType) fTypeContainer, true);
		} else {
			page.setPackageFragment((IPackageFragment) fTypeContainer, true);
		}
		page.setEnclosingTypeSelection(isInEnclosingType, true);
	}

	/**
	 * Fill-in the "Super Class" and "Super Interfaces" fields.
	 * @param page the wizard page.
	 */
	private void fillInWizardPageSuperTypes(NewTypeWizardPage page) {
		ITypeBinding type= getPossibleSuperTypeBinding(fNode);
		type= Bindings.normalizeTypeBinding(type);
		if (type != null) {
			if (type.isArray()) {
				type= type.getElementType();
			}
			if (type.isTopLevel() || type.isMember()) {
				if (type.isClass() && (fTypeKind == K_CLASS)) {
					page.setSuperClass(type.getQualifiedName(), true);
				}
			}
		}
	}

	private ITypeBinding getPossibleSuperTypeBinding(ASTNode node) {
		 if (fTypeKind == K_ANNOTATION) {
		 	return null;
		 }

		AST ast= node.getAST();
		node= ASTNodes.getNormalizedNode(node);
		ASTNode parent= node.getParent();
		switch (parent.getNodeType()) {
			case ASTNode.FUNCTION_DECLARATION:
				if (node.getLocationInParent() == FunctionDeclaration.THROWN_EXCEPTIONS_PROPERTY) {
					return ast.resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$
				}
				break;
			case ASTNode.THROW_STATEMENT :
				return ast.resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$
			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				if (parent.getLocationInParent() == CatchClause.EXCEPTION_PROPERTY) {
					return ast.resolveWellKnownType("java.lang.Exception"); //$NON-NLS-1$
				}
				break;
			case ASTNode.VARIABLE_DECLARATION_STATEMENT:
			case ASTNode.FIELD_DECLARATION:
				return null; // no guessing for LHS types, cannot be a supertype of a known type
		}
		return ASTResolving.guessBindingForTypeReference(node);
	}



	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		StringBuffer buf= new StringBuffer();
		switch (fTypeKind) {
			case K_CLASS:
				buf.append(CorrectionMessages.NewCUCompletionUsingWizardProposal_createclass_info);
				break;
			case K_INTERFACE:
				buf.append(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinterface_info);
				break;
			case K_ENUM:
				buf.append(CorrectionMessages.NewCUCompletionUsingWizardProposal_createenum_info);
				break;
			case K_ANNOTATION:
				buf.append(CorrectionMessages.NewCUCompletionUsingWizardProposal_createannotation_info);
				break;
		}
		buf.append("<br>"); //$NON-NLS-1$
		buf.append("<br>"); //$NON-NLS-1$
		if (fTypeContainer instanceof IType) {
			buf.append(CorrectionMessages.NewCUCompletionUsingWizardProposal_tooltip_enclosingtype);
		} else {
			buf.append(CorrectionMessages.NewCUCompletionUsingWizardProposal_tooltip_package);
		}
		buf.append(" <b>"); //$NON-NLS-1$
		buf.append(JavaScriptElementLabels.getElementLabel(fTypeContainer, JavaScriptElementLabels.T_FULLY_QUALIFIED));
		buf.append("</b><br>"); //$NON-NLS-1$
		buf.append("public "); //$NON-NLS-1$


		switch (fTypeKind) {
			case K_CLASS:
				buf.append("class <b>"); //$NON-NLS-1$
				break;
			case K_INTERFACE:
				buf.append("interface <b>"); //$NON-NLS-1$
				break;
			case K_ENUM:
				buf.append("enum <b>"); //$NON-NLS-1$
				break;
			case K_ANNOTATION:
				buf.append("@interface <b>"); //$NON-NLS-1$
				break;
		}
		nameToHTML(fTypeNameWithParameters, buf);

		ITypeBinding superclass= getPossibleSuperTypeBinding(fNode);
		if (superclass != null) {
			if (superclass.isClass()) {
				if (fTypeKind == K_CLASS) {
					buf.append("</b> extends <b>"); //$NON-NLS-1$
					nameToHTML(BindingLabelProvider.getBindingLabel(superclass, BindingLabelProvider.DEFAULT_TEXTFLAGS), buf);
				}
			} else {
				if (fTypeKind == K_INTERFACE) {
					buf.append("</b> extends <b>"); //$NON-NLS-1$
				} else {
					buf.append("</b> implements <b>"); //$NON-NLS-1$
				}
				nameToHTML(BindingLabelProvider.getBindingLabel(superclass, BindingLabelProvider.DEFAULT_TEXTFLAGS), buf);
			}
		}
		buf.append("</b> {<br>}<br>"); //$NON-NLS-1$
		return buf.toString();
	}
	
	private void nameToHTML(String name, StringBuffer buf) {
		for (int i= 0; i < name.length(); i++) {
			char ch= name.charAt(i);
			if (ch == '>') {
				buf.append("&gt;"); //$NON-NLS-1$
			} else if (ch == '<') {
				buf.append("&lt;"); //$NON-NLS-1$
			} else {
				buf.append(ch);
			}
		}
	}

	/**
	 * Returns the showDialog.
	 * @return boolean
	 */
	public boolean isShowDialog() {
		return fShowDialog;
	}

	/**
	 * Sets the showDialog.
	 * @param showDialog The showDialog to set
	 */
	public void setShowDialog(boolean showDialog) {
		fShowDialog= showDialog;
	}

	public IType getCreatedType() {
		return  fCreatedType;
	}


	public int getTypeKind() {
		return fTypeKind;
	}

}
