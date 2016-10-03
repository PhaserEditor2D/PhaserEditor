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
package org.eclipse.wst.jsdt.internal.ui.viewsupport;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.BindingKey;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IInitializer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.internal.ui.packageview.JsGlobalScopeContainer;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ColoredString.Style;
import org.eclipse.wst.jsdt.launching.JavaRuntime;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class ColoredJavaElementLabels {

	public static final Style QUALIFIER_STYLE= new Style(ColoredViewersManager.QUALIFIER_COLOR_NAME); 
	public static final Style COUNTER_STYLE= new Style(ColoredViewersManager.COUNTER_COLOR_NAME); 
	public static final Style DECORATIONS_STYLE= new Style(ColoredViewersManager.DECORATIONS_COLOR_NAME); 
	
	private static final Style APPENDED_TYPE_STYLE= DECORATIONS_STYLE; 
	
	public final static long COLORIZE= 1L << 55;
	
	private final static long QUALIFIER_FLAGS= JavaScriptElementLabels.P_COMPRESSED | JavaScriptElementLabels.USE_RESOLVED;
	

	private static final boolean getFlag(long flags, long flag) {
		return (flags & flag) != 0;
	}
	
	/**
	 * Returns the label of the given object. The object must be of type {@link IJavaScriptElement} or adapt to {@link org.eclipse.ui.model.IWorkbenchAdapter}. The empty string is returned
	 * if the element type is not known.
	 * @param obj Object to get the label from.
	 * @param flags The rendering flags
	 * @return Returns the label or the empty string if the object type is not supported.
	 */
	public static ColoredString getTextLabel(Object obj, long flags) {
		if (obj instanceof IJavaScriptElement) {
			return getElementLabel((IJavaScriptElement) obj, flags);
		} else if (obj instanceof IResource) {
			return new ColoredString(((IResource) obj).getName());
		} else if (obj instanceof JsGlobalScopeContainer) {
			JsGlobalScopeContainer container= (JsGlobalScopeContainer) obj;
			return getContainerEntryLabel(container.getClasspathEntry().getPath(), container.getJavaProject());
		}
		return new ColoredString(JavaScriptElementLabels.getTextLabel(obj, flags));
	}
				
	/**
	 * Returns the label for a Java element with the flags as defined by this class.
	 * @param element The element to render.
	 * @param flags The rendering flags.
	 * @return the label of the Java element
	 */
	public static ColoredString getElementLabel(IJavaScriptElement element, long flags) {
		ColoredString result= new ColoredString();
		getElementLabel(element, flags, result);
		return result;
	}
	
	/**
	 * Returns the label for a Java element with the flags as defined by this class.
	 * @param element The element to render.
	 * @param flags The rendering flags.
	 * @param result The buffer to append the resulting label to.
	 */
	public static void getElementLabel(IJavaScriptElement element, long flags, ColoredString result) {
		int type= element.getElementType();
		IPackageFragmentRoot root= null;
		
		if (type != IJavaScriptElement.JAVASCRIPT_MODEL && type != IJavaScriptElement.JAVASCRIPT_PROJECT && type != IJavaScriptElement.PACKAGE_FRAGMENT_ROOT)
			root= JavaModelUtil.getPackageFragmentRoot(element);
		if (root != null && getFlag(flags, JavaScriptElementLabels.PREPEND_ROOT_PATH)) {
			getPackageFragmentRootLabel(root, JavaScriptElementLabels.ROOT_QUALIFIED, result);
			result.append(JavaScriptElementLabels.CONCAT_STRING);
		}		
		
		switch (type) {
			case IJavaScriptElement.METHOD:
				getMethodLabel((IFunction) element, flags, result);
				break;
			case IJavaScriptElement.FIELD: 
				getFieldLabel((IField) element, flags, result);
				break;
			case IJavaScriptElement.LOCAL_VARIABLE: 
				getLocalVariableLabel((ILocalVariable) element, flags, result);
				break;
			case IJavaScriptElement.INITIALIZER:
				getInitializerLabel((IInitializer) element, flags, result);
				break;				
			case IJavaScriptElement.TYPE: 
				getTypeLabel((IType) element, flags, result);
				break;
			case IJavaScriptElement.CLASS_FILE: 
				getClassFileLabel((IClassFile) element, flags, result);
				break;					
			case IJavaScriptElement.JAVASCRIPT_UNIT: 
				getCompilationUnitLabel((IJavaScriptUnit) element, flags, result);
				break;	
			case IJavaScriptElement.PACKAGE_FRAGMENT: 
				getPackageFragmentLabel((IPackageFragment) element, flags, result);
				break;
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT: 
				getPackageFragmentRootLabel((IPackageFragmentRoot) element, flags, result);
				break;
			case IJavaScriptElement.IMPORT_CONTAINER:
			case IJavaScriptElement.IMPORT_DECLARATION:
				getDeclarationLabel(element, flags, result);
				break;
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
			case IJavaScriptElement.JAVASCRIPT_MODEL:
				result.append(element.getElementName());
				break;
			default:
				result.append(element.getElementName());
		}
		
		if (root != null && getFlag(flags, JavaScriptElementLabels.APPEND_ROOT_PATH)) {
			int offset= result.length();
			result.append(JavaScriptElementLabels.CONCAT_STRING);
			getPackageFragmentRootLabel(root, JavaScriptElementLabels.ROOT_QUALIFIED, result);
			
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
			
		}
	}

	/**
	 * Appends the label for a method to a {@link ColoredString}. Considers the M_* flags.
	 * 	@param method The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'M_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */		
	public static void getMethodLabel(IFunction method, long flags, ColoredString result) {
		try {
			BindingKey resolvedKey= getFlag(flags, JavaScriptElementLabels.USE_RESOLVED) && method.isResolved() ? new BindingKey(method.getKey()) : null;
			String resolvedSig= (resolvedKey != null) ? resolvedKey.toSignature() : null;
			
			// return type
			if (getFlag(flags, JavaScriptElementLabels.M_PRE_RETURNTYPE) && method.exists() && !method.isConstructor()) {
				String returnTypeSig= resolvedSig != null ? Signature.getReturnType(resolvedSig) : method.getReturnType();
				getTypeSignatureLabel(returnTypeSig, flags, result);
				result.append(' ');
			}
			
			// qualification
			if (getFlag(flags, JavaScriptElementLabels.M_FULLY_QUALIFIED)) {
				getTypeLabel(method.getDeclaringType(), JavaScriptElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				result.append('.');
			}
				
			result.append(method.getElementName());
			
			// parameters
			result.append('(');
			if (getFlag(flags, JavaScriptElementLabels.M_PARAMETER_TYPES | JavaScriptElementLabels.M_PARAMETER_NAMES)) {
				String[] types= null;
				int nParams= 0;
				boolean renderVarargs= false;
				if (getFlag(flags, JavaScriptElementLabels.M_PARAMETER_TYPES)) {
					if (resolvedSig != null) {
						types= Signature.getParameterTypes(resolvedSig);
					} else {
						types= method.getParameterTypes();
					}
					nParams= types.length;
					renderVarargs= method.exists() && Flags.isVarargs(method.getFlags());
				}
				String[] names= null;
				if (getFlag(flags, JavaScriptElementLabels.M_PARAMETER_NAMES) && method.exists()) {
					names= method.getParameterNames();
					if (types == null) {
						nParams= names.length;
					} else { // types != null
						if (nParams != names.length) {
							if (resolvedSig != null && types.length > names.length) {
								// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=99137
								nParams= names.length;
								String[] typesWithoutSyntheticParams= new String[nParams];
								System.arraycopy(types, types.length - nParams, typesWithoutSyntheticParams, 0, nParams);
								types= typesWithoutSyntheticParams;
							} else {
								// https://bugs.eclipse.org/bugs/show_bug.cgi?id=101029
								// JavaScriptPlugin.logErrorMessage("JavaScriptElementLabels: Number of param types(" + nParams + ") != number of names(" + names.length + "): " + method.getElementName());   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
								names= null; // no names rendered
							}
						}
					}
				}
				
				for (int i= 0; i < nParams; i++) {
					if (i > 0) {
						result.append(JavaScriptElementLabels.COMMA_STRING);
					}
					if (types != null) {
						String paramSig= types[i];
						if (renderVarargs && (i == nParams - 1)) {
							int newDim= Signature.getArrayCount(paramSig) - 1;
							getTypeSignatureLabel(Signature.getElementType(paramSig), flags, result);
							for (int k= 0; k < newDim; k++) {
								result.append('[').append(']');
							}
							result.append(JavaScriptElementLabels.ELLIPSIS_STRING);
						} else {
							getTypeSignatureLabel(paramSig, flags, result);
						}
					}
					if (names != null) {
						if (types != null) {
							result.append(' ');
						}
						result.append(names[i]);
					}
				}
			} else {
				if (method.getParameterTypes().length > 0) {
					result.append(JavaScriptElementLabels.ELLIPSIS_STRING);
				}
			}
			result.append(')');
					
			if (getFlag(flags, JavaScriptElementLabels.M_EXCEPTIONS)) {
				String[] types;
				if (resolvedKey != null) {
					//types= resolvedKey.getThrownExceptions();
					types = new String[0];
				} else {
					types= new String[0];
				}
				if (types.length > 0) {
					result.append(" throws "); //$NON-NLS-1$
					for (int i= 0; i < types.length; i++) {
						if (i > 0) {
							result.append(JavaScriptElementLabels.COMMA_STRING);
						}
						getTypeSignatureLabel(types[i], flags, result);
					}
				}
			}
			
			if (getFlag(flags, JavaScriptElementLabels.M_APP_TYPE_PARAMETERS)) {
				int offset= result.length();
				if (getFlag(flags, COLORIZE)) {
					result.colorize(offset, result.length() - offset, APPENDED_TYPE_STYLE);
				}
			}
			
			if (getFlag(flags, JavaScriptElementLabels.M_APP_RETURNTYPE) && method.exists() && !method.isConstructor()) {
				int offset= result.length();
				result.append(JavaScriptElementLabels.DECL_STRING);
				String returnTypeSig= resolvedSig != null ? Signature.getReturnType(resolvedSig) : method.getReturnType();
				getTypeSignatureLabel(returnTypeSig, flags, result);
				if (getFlag(flags, COLORIZE)) {
					result.colorize(offset, result.length() - offset, APPENDED_TYPE_STYLE);
				}
			}			

			// category
			if (getFlag(flags, JavaScriptElementLabels.M_CATEGORY) && method.exists()) 
				getCategoryLabel(method, result);
			
			// post qualification
			if (getFlag(flags, JavaScriptElementLabels.M_POST_QUALIFIED)) {
				int offset= result.length();
				result.append(JavaScriptElementLabels.CONCAT_STRING);
				if (method.getDeclaringType()!=null)
					getTypeLabel(method.getDeclaringType(), JavaScriptElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				if (getFlag(flags, COLORIZE)) {
					result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
				}
			}
			
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e); // NotExistsException will not reach this point
		}
	}

	private static void getCategoryLabel(IMember member, ColoredString result) throws JavaScriptModelException {
		String[] categories= member.getCategories();
		if (categories.length > 0) {
			ColoredString categoriesBuf= new ColoredString();
			for (int i= 0; i < categories.length; i++) {
				if (i > 0)
					categoriesBuf.append(JavaUIMessages.JavaElementLabels_category_separator_string);
				categoriesBuf.append(categories[i]);
			}
			result.append(JavaScriptElementLabels.CONCAT_STRING);
			result.append(Messages.format(JavaUIMessages.JavaElementLabels_category , categoriesBuf.toString()));
		}
	}
	
	/**
	 * Appends the label for a field to a {@link ColoredString}. Considers the F_* flags.
	 * 	@param field The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'F_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */	
	public static void getFieldLabel(IField field, long flags, ColoredString result) {
		try {
			
			if (getFlag(flags, JavaScriptElementLabels.F_PRE_TYPE_SIGNATURE) && field.exists()) {
				if (getFlag(flags, JavaScriptElementLabels.USE_RESOLVED) && field.isResolved()) {
					getTypeSignatureLabel(new BindingKey(field.getKey()).toSignature(), flags, result);
				} else {
					getTypeSignatureLabel(field.getTypeSignature(), flags, result);
				}
				result.append(' ');
			}
			
			// qualification
			if (getFlag(flags, JavaScriptElementLabels.F_FULLY_QUALIFIED)) {
				getTypeLabel(field.getDeclaringType(), JavaScriptElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				result.append('.');
			}
			result.append(field.getElementName());
			
			if (getFlag(flags, JavaScriptElementLabels.F_APP_TYPE_SIGNATURE) && field.exists()) {
				int offset= result.length();
				result.append(JavaScriptElementLabels.DECL_STRING);
				if (getFlag(flags, JavaScriptElementLabels.USE_RESOLVED) && field.isResolved()) {
					getTypeSignatureLabel(new BindingKey(field.getKey()).toSignature(), flags, result);
				} else {
					getTypeSignatureLabel(field.getTypeSignature(), flags, result);
				}
				if (getFlag(flags, COLORIZE)) {
					result.colorize(offset, result.length() - offset, APPENDED_TYPE_STYLE);
				}
			}

			// category
			if (getFlag(flags, JavaScriptElementLabels.F_CATEGORY) && field.exists())
				getCategoryLabel(field, result);

			// post qualification
			if (getFlag(flags, JavaScriptElementLabels.F_POST_QUALIFIED)) {
				int offset= result.length();
				result.append(JavaScriptElementLabels.CONCAT_STRING);
				getTypeLabel(field.getDeclaringType(), JavaScriptElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				if (getFlag(flags, COLORIZE)) {
					result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
				}
			}

		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e); // NotExistsException will not reach this point
		}			
	}
	
	/**
	 * Appends the label for a local variable to a {@link ColoredString}.
	 * 	@param localVariable The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'F_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */	
	public static void getLocalVariableLabel(ILocalVariable localVariable, long flags, ColoredString result) {
		if (getFlag(flags, JavaScriptElementLabels.F_PRE_TYPE_SIGNATURE)) {
			getTypeSignatureLabel(localVariable.getTypeSignature(), flags, result);
			result.append(' ');
		}
		
		if (getFlag(flags, JavaScriptElementLabels.F_FULLY_QUALIFIED)) {
			getElementLabel(localVariable.getParent(), JavaScriptElementLabels.M_PARAMETER_TYPES | JavaScriptElementLabels.M_FULLY_QUALIFIED | JavaScriptElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
			result.append('.');
		}
		
		result.append(localVariable.getElementName());
		
		if (getFlag(flags, JavaScriptElementLabels.F_APP_TYPE_SIGNATURE)) {
			int offset= result.length();
			result.append(JavaScriptElementLabels.DECL_STRING);
			getTypeSignatureLabel(localVariable.getTypeSignature(), flags, result);
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, APPENDED_TYPE_STYLE);
			}
		}
		
		// post qualification
		if (getFlag(flags, JavaScriptElementLabels.F_POST_QUALIFIED)) {
			result.append(JavaScriptElementLabels.CONCAT_STRING);
			getElementLabel(localVariable.getParent(), JavaScriptElementLabels.M_PARAMETER_TYPES | JavaScriptElementLabels.M_FULLY_QUALIFIED | JavaScriptElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
		}
	}
	
	/**
	 * Appends the label for a initializer to a {@link ColoredString}. Considers the I_* flags.
	 * 	@param initializer The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'I_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */	
	public static void getInitializerLabel(IInitializer initializer, long flags, ColoredString result) {
		// qualification
		if (getFlag(flags, JavaScriptElementLabels.I_FULLY_QUALIFIED)) {
			getTypeLabel(initializer.getDeclaringType(), JavaScriptElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
			result.append('.');
		}
		result.append(JavaUIMessages.JavaElementLabels_initializer); 

		// post qualification
		if (getFlag(flags, JavaScriptElementLabels.I_POST_QUALIFIED)) {
			int offset= result.length();
			result.append(JavaScriptElementLabels.CONCAT_STRING);
			getTypeLabel(initializer.getDeclaringType(), JavaScriptElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}
	
	private static void getTypeSignatureLabel(String typeSig, long flags, ColoredString result) {
		int sigKind= Signature.getTypeSignatureKind(typeSig);
		switch (sigKind) {
			case Signature.BASE_TYPE_SIGNATURE:
				result.append(Signature.toString(typeSig));
				break;
			case Signature.ARRAY_TYPE_SIGNATURE:
				getTypeSignatureLabel(Signature.getElementType(typeSig), flags, result);
				for (int dim= Signature.getArrayCount(typeSig); dim > 0; dim--) {
					result.append('[').append(']');
				}
				break;
			case Signature.CLASS_TYPE_SIGNATURE:
				String baseType= Signature.toString(typeSig);
				result.append(Signature.getSimpleName(baseType));
				
				getTypeArgumentSignaturesLabel(new String[0], flags, result);
				break;
			default:
				// unknown
		}
	}
	
	private static void getTypeArgumentSignaturesLabel(String[] typeArgsSig, long flags, ColoredString result) {
		if (typeArgsSig.length > 0) {
			result.append('<');
			for (int i = 0; i < typeArgsSig.length; i++) {
				if (i > 0) {
					result.append(JavaScriptElementLabels.COMMA_STRING);
				}
				getTypeSignatureLabel(typeArgsSig[i], flags, result);
			}
			result.append('>');
		}
	}
	
	private static void getTypeParameterSignaturesLabel(String[] typeParamSigs, long flags, ColoredString result) {
		if (typeParamSigs.length > 0) {
			result.append('<');
			for (int i = 0; i < typeParamSigs.length; i++) {
				if (i > 0) {
					result.append(JavaScriptElementLabels.COMMA_STRING);
				}
				result.append(Signature.getTypeVariable(typeParamSigs[i]));
			}
			result.append('>');
		}
	}
	

	/**
	 * Appends the label for a type to a {@link ColoredString}. Considers the T_* flags.
	 * 	@param type The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'T_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */		
	public static void getTypeLabel(IType type, long flags, ColoredString result) {
		
		if (getFlag(flags, JavaScriptElementLabels.T_FULLY_QUALIFIED)) {
			IPackageFragment pack= type.getPackageFragment();
			if (!pack.isDefaultPackage()) {
				getPackageFragmentLabel(pack, (flags & QUALIFIER_FLAGS), result);
				result.append('.');
			}
		}
		if (getFlag(flags, JavaScriptElementLabels.T_FULLY_QUALIFIED | JavaScriptElementLabels.T_CONTAINER_QUALIFIED)) {
			IType declaringType= type.getDeclaringType();
			if (declaringType != null) {
				getTypeLabel(declaringType, JavaScriptElementLabels.T_CONTAINER_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				result.append('.');
			}
			int parentType= type.getParent().getElementType();
			if (parentType == IJavaScriptElement.METHOD || parentType == IJavaScriptElement.FIELD || parentType == IJavaScriptElement.INITIALIZER) { // anonymous or local
				getElementLabel(type.getParent(), 0, result);
				result.append('.');
			}
		}
		
		String typeName= type.getElementName();
		if (typeName.length() == 0) { // anonymous
			try {
				String supertypeName= Signature.getSimpleName(type.getSuperclassName());
				
				typeName= Messages.format(JavaUIMessages.JavaElementLabels_anonym_type , supertypeName); 
				
			} catch (JavaScriptModelException e) {
				//ignore
				typeName= JavaUIMessages.JavaElementLabels_anonym; 
			}
		}
		result.append(typeName);
		if (getFlag(flags, JavaScriptElementLabels.T_TYPE_PARAMETERS)) {
			if (getFlag(flags, JavaScriptElementLabels.USE_RESOLVED) && type.isResolved()) {
				BindingKey key= new BindingKey(type.getKey());
				getTypeParameterSignaturesLabel(new String[0], flags, result);
			}
		}
		
		// category
		if (getFlag(flags, JavaScriptElementLabels.T_CATEGORY) && type.exists()) {
			try {
				getCategoryLabel(type, result);
			} catch (JavaScriptModelException e) {
				// ignore
			}
		}

		// post qualification
		if (getFlag(flags, JavaScriptElementLabels.T_POST_QUALIFIED)) {
			int offset= result.length();
			result.append(JavaScriptElementLabels.CONCAT_STRING);
			IType declaringType= type.getDeclaringType();
			if (declaringType != null) {
				getTypeLabel(declaringType, JavaScriptElementLabels.T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS), result);
				int parentType= type.getParent().getElementType();
				if (parentType == IJavaScriptElement.METHOD || parentType == IJavaScriptElement.FIELD || parentType == IJavaScriptElement.INITIALIZER) { // anonymous or local
					result.append('.');
					getElementLabel(type.getParent(), 0, result);
				}
			} else {
				getPackageFragmentLabel(type.getPackageFragment(), flags & QUALIFIER_FLAGS, result);
			}
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}

	/**
	 * Appends the label for a import container, import or package declaration to a {@link ColoredString}. Considers the D_* flags.
	 * 	@param declaration The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'D_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */	
	public static void getDeclarationLabel(IJavaScriptElement declaration, long flags, ColoredString result) {
		if (getFlag(flags, JavaScriptElementLabels.D_QUALIFIED)) {
			IJavaScriptElement openable= (IJavaScriptElement) declaration.getOpenable();
			if (openable != null) {
				result.append(getElementLabel(openable, JavaScriptElementLabels.CF_QUALIFIED | JavaScriptElementLabels.CU_QUALIFIED | (flags & QUALIFIER_FLAGS)));
				result.append('/');
			}	
		}
		if (declaration.getElementType() == IJavaScriptElement.IMPORT_CONTAINER) {
			result.append(JavaUIMessages.JavaElementLabels_import_container); 
		} else {
			result.append(declaration.getElementName());
		}
		// post qualification
		if (getFlag(flags, JavaScriptElementLabels.D_POST_QUALIFIED)) {
			int offset= result.length();
			IJavaScriptElement openable= (IJavaScriptElement) declaration.getOpenable();
			if (openable != null) {
				result.append(JavaScriptElementLabels.CONCAT_STRING);
				result.append(getElementLabel(openable, JavaScriptElementLabels.CF_QUALIFIED | JavaScriptElementLabels.CU_QUALIFIED | (flags & QUALIFIER_FLAGS)));
			}
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}	
	
	/**
	 * Appends the label for a class file to a {@link ColoredString}. Considers the CF_* flags.
	 * 	@param classFile The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'CF_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */	
	public static void getClassFileLabel(IClassFile classFile, long flags, ColoredString result) {
		if (getFlag(flags, JavaScriptElementLabels.CF_QUALIFIED)) {
			IPackageFragment pack= (IPackageFragment) classFile.getParent();
			if (!pack.isDefaultPackage()) {
				getPackageFragmentLabel(pack, (flags & QUALIFIER_FLAGS), result);
				result.append('.');
			}
		}
		result.append(classFile.getElementName());
		
		if (getFlag(flags, JavaScriptElementLabels.CF_POST_QUALIFIED)) {
			int offset= result.length();
			result.append(JavaScriptElementLabels.CONCAT_STRING);
			getPackageFragmentLabel((IPackageFragment) classFile.getParent(), flags & QUALIFIER_FLAGS, result);
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}

	/**
	 * Appends the label for a compilation unit to a {@link ColoredString}. Considers the CU_* flags.
	 * 	@param cu The element to render.
	 * @param flags The rendering flags. Flags with names starting with 'CU_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */
	public static void getCompilationUnitLabel(IJavaScriptUnit cu, long flags, ColoredString result) {
		if (getFlag(flags, JavaScriptElementLabels.CU_QUALIFIED)) {
			IPackageFragment pack= (IPackageFragment) cu.getParent();
			if (!pack.isDefaultPackage()) {
				getPackageFragmentLabel(pack, (flags & QUALIFIER_FLAGS), result);
				result.append('.');
			}
		}
		result.append(cu.getElementName());
		
		if (getFlag(flags, JavaScriptElementLabels.CU_POST_QUALIFIED)) {
			int offset= result.length();
			result.append(JavaScriptElementLabels.CONCAT_STRING);
			getPackageFragmentLabel((IPackageFragment) cu.getParent(), flags & QUALIFIER_FLAGS, result);
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}		
	}

	/**
	 * Appends the label for a package fragment to a {@link ColoredString}. Considers the P_* flags.
	 * 	@param pack The element to render.
	 * @param flags The rendering flags. Flags with names starting with P_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */	
	public static void getPackageFragmentLabel(IPackageFragment pack, long flags, ColoredString result) {
		if (getFlag(flags, JavaScriptElementLabels.P_QUALIFIED)) {
			getPackageFragmentRootLabel((IPackageFragmentRoot) pack.getParent(), JavaScriptElementLabels.ROOT_QUALIFIED, result);
			result.append('/');
		}
		if (pack.isDefaultPackage()) {
			result.append(JavaScriptElementLabels.DEFAULT_PACKAGE);
		} else if (getFlag(flags, JavaScriptElementLabels.P_COMPRESSED)) {
			StringBuffer buf= new StringBuffer();
			JavaScriptElementLabels.getPackageFragmentLabel(pack, JavaScriptElementLabels.P_COMPRESSED, buf);
			result.append(buf.toString());
		} else {
			result.append(pack.getElementName());
		}
		if (getFlag(flags, JavaScriptElementLabels.P_POST_QUALIFIED)) {
			int offset= result.length();
			result.append(JavaScriptElementLabels.CONCAT_STRING);
			getPackageFragmentRootLabel((IPackageFragmentRoot) pack.getParent(), JavaScriptElementLabels.ROOT_QUALIFIED, result);
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}

	/**
	 * Appends the label for a package fragment root to a {@link ColoredString}. Considers the ROOT_* flags.
	 * 	@param root The element to render.
	 * @param flags The rendering flags. Flags with names starting with ROOT_' are considered.
	 * @param result The buffer to append the resulting label to.
	 */	
	public static void getPackageFragmentRootLabel(IPackageFragmentRoot root, long flags, ColoredString result) {
		if (root.isArchive())
			getArchiveLabel(root, flags, result);
		else
			getFolderLabel(root, flags, result);
	}
	
	private static void getArchiveLabel(IPackageFragmentRoot root, long flags, ColoredString result) {
		// Handle variables different	
		if (getFlag(flags, JavaScriptElementLabels.ROOT_VARIABLE) && getVariableLabel(root, flags, result))
			return;
		boolean external= root.isExternal();
		if (external)
			getExternalArchiveLabel(root, flags, result);
		else
			getInternalArchiveLabel(root, flags, result);
	}
	
	private static boolean getVariableLabel(IPackageFragmentRoot root, long flags, ColoredString result) {
		try {
			IIncludePathEntry rawEntry= root.getRawIncludepathEntry();
			if (rawEntry != null && rawEntry.getEntryKind() == IIncludePathEntry.CPE_VARIABLE) {
				IPath path= rawEntry.getPath().makeRelative();
				int offset= result.length();
				if (getFlag(flags, JavaScriptElementLabels.REFERENCED_ROOT_POST_QUALIFIED)) {
					int segements= path.segmentCount();
					if (segements > 0) {
						result.append(path.segment(segements - 1));
						if (segements > 1) {
							result.append(JavaScriptElementLabels.CONCAT_STRING);
							result.append(path.removeLastSegments(1).toOSString());
						}
					} else {
						result.append(path.toString());
					}
				} else {
					result.append(path.toString());
				}
				result.append(JavaScriptElementLabels.CONCAT_STRING);
				if (root.isExternal())
					result.append(root.getPath().toOSString());
				else
					result.append(root.getPath().makeRelative().toString());
				
				if (getFlag(flags, COLORIZE)) {
					result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
				}
				return true;
			}
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e); // problems with class path
		}
		return false;
	}

	private static void getExternalArchiveLabel(IPackageFragmentRoot root, long flags, ColoredString result) {
		IPath path= root.getPath();
		if (getFlag(flags, JavaScriptElementLabels.REFERENCED_ROOT_POST_QUALIFIED)) {
			int segements= path.segmentCount();
			if (segements > 0) {
				result.append(path.segment(segements - 1));
				int offset= result.length();
				if (segements > 1 || path.getDevice() != null) {
					result.append(JavaScriptElementLabels.CONCAT_STRING);
					result.append(path.removeLastSegments(1).toOSString());
				}
				if (getFlag(flags, COLORIZE)) {
					result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
				}
			} else {
				result.append(path.toOSString());
			}
		} else {
			result.append(path.toOSString());
		}
	}

	private static void getInternalArchiveLabel(IPackageFragmentRoot root, long flags, ColoredString result) {
		IResource resource= root.getResource();
		boolean rootQualified= getFlag(flags, JavaScriptElementLabels.ROOT_QUALIFIED);
		boolean referencedQualified= getFlag(flags, JavaScriptElementLabels.REFERENCED_ROOT_POST_QUALIFIED) && isReferenced(root);
		if (rootQualified) {
			result.append(root.getPath().makeRelative().toString());
		} else {
			result.append(root.getElementName());
			int offset= result.length();
			if (referencedQualified) {
				result.append(JavaScriptElementLabels.CONCAT_STRING);
				result.append(resource.getParent().getFullPath().makeRelative().toString());
			} else if (getFlag(flags, JavaScriptElementLabels.ROOT_POST_QUALIFIED)) {
				result.append(JavaScriptElementLabels.CONCAT_STRING);
				result.append(root.getParent().getPath().makeRelative().toString());
			} else {
				return;
			}
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}

	private static void getFolderLabel(IPackageFragmentRoot root, long flags, ColoredString result) {
		IResource resource= root.getResource();
		boolean rootQualified= getFlag(flags, JavaScriptElementLabels.ROOT_QUALIFIED);
		boolean referencedQualified= getFlag(flags, JavaScriptElementLabels.REFERENCED_ROOT_POST_QUALIFIED) && isReferenced(root);
		if (rootQualified) {
			result.append(root.getPath().makeRelative().toString());
		} else {
			if (resource != null) {
				IPath projectRelativePath= resource.getProjectRelativePath();
				if (projectRelativePath.segmentCount() == 0) {
					result.append(resource.getName());
					referencedQualified= false;
				} else {
					result.append(projectRelativePath.toString());
				}
			} else
				result.append(root.getElementName());
			int offset= result.length();
			if (referencedQualified) {
				result.append(JavaScriptElementLabels.CONCAT_STRING);
				result.append(resource.getProject().getName());
			} else if (getFlag(flags, JavaScriptElementLabels.ROOT_POST_QUALIFIED)) {
				result.append(JavaScriptElementLabels.CONCAT_STRING);
				result.append(root.getParent().getElementName());
			} else {
				return;
			}
			if (getFlag(flags, COLORIZE)) {
				result.colorize(offset, result.length() - offset, QUALIFIER_STYLE);
			}
		}
	}
	
	/**
	 * @param root
	 * @return <code>true</code> if the given package fragment root is
	 * referenced. This means it is owned by a different project but is referenced
	 * by the root's parent. Returns <code>false</code> if the given root
	 * doesn't have an underlying resource.
	 */
	private static boolean isReferenced(IPackageFragmentRoot root) {
		IResource resource= root.getResource();
		if (resource != null) {
			IProject jarProject= resource.getProject();
			IProject container= root.getJavaScriptProject().getProject();
			return !container.equals(jarProject);
		}
		return false;
	}
		
	/**
	 * Returns the label of a classpath container
	 * @param containerPath The path of the container.
	 * @param project The project the container is resolved in.
	 * @return Returns the label of the classpath container
	 */
	public static ColoredString getContainerEntryLabel(IPath containerPath, IJavaScriptProject project) {
		try {
			IJsGlobalScopeContainer container= JavaScriptCore.getJsGlobalScopeContainer(containerPath, project);
			String description= null;
			if (container != null) {
				description= container.getDescription();
			}
			if (description == null) {
				JsGlobalScopeContainerInitializer initializer= JavaScriptCore.getJsGlobalScopeContainerInitializer(containerPath.segment(0));
				if (initializer != null) {
					description= initializer.getDescription(containerPath, project);
				}
			}
			if (description != null) {
				ColoredString str= new ColoredString(description);
				if (containerPath.segmentCount() > 0 && JavaRuntime.JRE_CONTAINER.equals(containerPath.segment(0))) {
					int index= description.indexOf('[');
					if (index != -1) {
						str.colorize(index, description.length() - index, DECORATIONS_STYLE); 
					}
				}
				return str;
			}
		} catch (JavaScriptModelException e) {
			// ignore
		}
		return new ColoredString(containerPath.toString());
	}

	public static ColoredString decorateColoredString(ColoredString string, String decorated, Style color) {
		String label= string.getString();
		int originalStart= decorated.indexOf(label);
		if (originalStart == -1) {
			return new ColoredString(decorated); // the decorator did something wild
		}
		if (originalStart > 0) {
			ColoredString newString= new ColoredString(decorated.substring(0, originalStart), color);
			newString.append(string);
			string= newString;
		}
		if (decorated.length() > originalStart + label.length()) { // decorator appended something
			return string.append(decorated.substring(originalStart + label.length()), color);
		}
		return string; // no change
	}
	
}
