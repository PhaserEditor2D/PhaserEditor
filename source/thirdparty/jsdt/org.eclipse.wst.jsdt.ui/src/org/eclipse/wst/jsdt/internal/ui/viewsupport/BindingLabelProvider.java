/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.viewsupport;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.IPackageBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.ui.JavaScriptElementImageDescriptor;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Label provider to render bindings in viewers.
 * 
 * 
 */
public class BindingLabelProvider extends LabelProvider {

	private static int getAdornmentFlags(IBinding binding, int flags) {
		if (binding == null)
			return 0;
		
		int adornments= 0;
		if (binding instanceof IFunctionBinding && ((IFunctionBinding) binding).isConstructor())
			adornments|= JavaScriptElementImageDescriptor.CONSTRUCTOR;
		final int modifiers= binding.getModifiers();
		if (Modifier.isAbstract(modifiers))
			adornments|= JavaScriptElementImageDescriptor.ABSTRACT;
		if (Modifier.isFinal(modifiers))
			adornments|= JavaScriptElementImageDescriptor.FINAL;
		if (Modifier.isSynchronized(modifiers))
			adornments|= JavaScriptElementImageDescriptor.SYNCHRONIZED;
		if (Modifier.isStatic(modifiers))
			adornments|= JavaScriptElementImageDescriptor.STATIC;
		if (binding.isDeprecated())
			adornments|= JavaScriptElementImageDescriptor.DEPRECATED;
		if (binding instanceof IVariableBinding && ((IVariableBinding) binding).isField()) {
			if (Modifier.isTransient(modifiers))
				adornments|= JavaScriptElementImageDescriptor.TRANSIENT;
			if (Modifier.isVolatile(modifiers))
				adornments|= JavaScriptElementImageDescriptor.VOLATILE;
		}
		return adornments;
	}

	private static ImageDescriptor getBaseImageDescriptor(IBinding binding, int flags) {
		if (binding instanceof ITypeBinding) {
			ITypeBinding typeBinding= (ITypeBinding) binding;
			if (typeBinding.isArray()) {
				typeBinding= typeBinding.getElementType();
			}
			return getTypeImageDescriptor(typeBinding.getDeclaringClass() != null, typeBinding, flags);
		} else if (binding instanceof IFunctionBinding) {
			return getMethodImageDescriptor(binding.getModifiers());
		} else if (binding instanceof IVariableBinding)
			return getFieldImageDescriptor((IVariableBinding) binding);
		return JavaPluginImages.DESC_OBJS_UNKNOWN;
	}

	private static ImageDescriptor getClassImageDescriptor(int modifiers) {
		if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) || Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_OBJS_CLASS;
		else
			return JavaPluginImages.DESC_OBJS_CLASS_DEFAULT;
	}

	private static ImageDescriptor getFieldImageDescriptor(IVariableBinding binding) {
		final int modifiers= binding.getModifiers();
		if (Modifier.isPublic(modifiers))
			return JavaPluginImages.DESC_FIELD_PUBLIC;
		if (Modifier.isProtected(modifiers))
			return JavaPluginImages.DESC_FIELD_PROTECTED;
		if (Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_FIELD_PRIVATE;

		return JavaPluginImages.DESC_FIELD_DEFAULT;
	}

	private static void getFieldLabel(IVariableBinding binding, long flags, StringBuffer buffer) {
		if (((flags & JavaScriptElementLabels.F_PRE_TYPE_SIGNATURE) != 0)) {
			getTypeLabel(binding.getType(), (flags & JavaScriptElementLabels.T_TYPE_PARAMETERS), buffer);
			buffer.append(' ');
		}
		// qualification

		if ((flags & JavaScriptElementLabels.F_FULLY_QUALIFIED) != 0) {
			ITypeBinding declaringClass= binding.getDeclaringClass();
			if (declaringClass != null) { // test for array.length
				getTypeLabel(declaringClass, JavaScriptElementLabels.T_FULLY_QUALIFIED | (flags & JavaScriptElementLabels.P_COMPRESSED), buffer);
				buffer.append('.');
			}
		}
		buffer.append(binding.getName());
		if (((flags & JavaScriptElementLabels.F_APP_TYPE_SIGNATURE) != 0)) {
			buffer.append(JavaScriptElementLabels.DECL_STRING);
			getTypeLabel(binding.getType(), (flags & JavaScriptElementLabels.T_TYPE_PARAMETERS), buffer);
		}
		// post qualification
		if ((flags & JavaScriptElementLabels.F_POST_QUALIFIED) != 0) {
			ITypeBinding declaringClass= binding.getDeclaringClass();
			if (declaringClass != null) { // test for array.length
				buffer.append(JavaScriptElementLabels.CONCAT_STRING);
				getTypeLabel(declaringClass, JavaScriptElementLabels.T_FULLY_QUALIFIED | (flags & JavaScriptElementLabels.P_COMPRESSED), buffer);
			}
		}
	}

	private static void getLocalVariableLabel(IVariableBinding binding, long flags, StringBuffer buffer) {
		if (((flags & JavaScriptElementLabels.F_PRE_TYPE_SIGNATURE) != 0)) {
			getTypeLabel(binding.getType(), (flags & JavaScriptElementLabels.T_TYPE_PARAMETERS), buffer);
			buffer.append(' ');
		}
		if (((flags & JavaScriptElementLabels.F_FULLY_QUALIFIED) != 0)) {
			IFunctionBinding declaringMethod= binding.getDeclaringMethod();
			if (declaringMethod != null) {
				getMethodLabel(declaringMethod, flags, buffer);
				buffer.append('.');
			}
		}
		buffer.append(binding.getName());
		if (((flags & JavaScriptElementLabels.F_APP_TYPE_SIGNATURE) != 0)) {
			buffer.append(JavaScriptElementLabels.DECL_STRING);
			getTypeLabel(binding.getType(), (flags & JavaScriptElementLabels.T_TYPE_PARAMETERS), buffer);
		}
	}

	private static ImageDescriptor getInnerClassImageDescriptor(int modifiers) {
		if (Modifier.isPublic(modifiers))
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_PUBLIC;
		else if (Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_PRIVATE;
		else if (Modifier.isProtected(modifiers))
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_PROTECTED;
		else
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_DEFAULT;
	}

//	private static ImageDescriptor getInnerInterfaceImageDescriptor(int modifiers) {
//		if (Modifier.isPublic(modifiers))
//			return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PUBLIC;
//		else if (Modifier.isPrivate(modifiers))
//			return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PRIVATE;
//		else if (Modifier.isProtected(modifiers))
//			return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PROTECTED;
//		else
//			return JavaPluginImages.DESC_OBJS_INTERFACE_DEFAULT;
//	}
//
//	private static ImageDescriptor getInterfaceImageDescriptor(int modifiers) {
//		if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) || Modifier.isPrivate(modifiers))
//			return JavaPluginImages.DESC_OBJS_INTERFACE;
//		else
//			return JavaPluginImages.DESC_OBJS_INTERFACE_DEFAULT;
//	}

	private static ImageDescriptor getMethodImageDescriptor(int modifiers) {
		if (Modifier.isPublic(modifiers))
			return JavaPluginImages.DESC_MISC_PUBLIC;
		if (Modifier.isProtected(modifiers))
			return JavaPluginImages.DESC_MISC_PROTECTED;
		if (Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_MISC_PRIVATE;

		return JavaPluginImages.DESC_MISC_DEFAULT;
	}
	
	private static void appendDimensions(int dim, StringBuffer buffer) {
		for (int i=0 ; i < dim; i++) {
			buffer.append('[').append(']');
		}
	}
	

	private static void getMethodLabel(IFunctionBinding binding, long flags, StringBuffer buffer) {
		// return type
		if (((flags & JavaScriptElementLabels.M_PRE_RETURNTYPE) != 0) && !binding.isConstructor()) {
			getTypeLabel(binding.getReturnType(), (flags & JavaScriptElementLabels.T_TYPE_PARAMETERS), buffer);
			buffer.append(' ');
		}
		// qualification
		if ((flags & JavaScriptElementLabels.M_FULLY_QUALIFIED) != 0) {
			getTypeLabel(binding.getDeclaringClass(), JavaScriptElementLabels.T_FULLY_QUALIFIED | (flags & JavaScriptElementLabels.P_COMPRESSED), buffer);
			buffer.append('.');
		}
		buffer.append(binding.getName());
		
		// parameters
		buffer.append('(');
		if ((flags & JavaScriptElementLabels.M_PARAMETER_TYPES | JavaScriptElementLabels.M_PARAMETER_NAMES) != 0) {
			ITypeBinding[] parameters= ((flags & JavaScriptElementLabels.M_PARAMETER_TYPES) != 0) ? binding.getParameterTypes() : null;
			if (parameters != null) {
				for (int index= 0; index < parameters.length; index++) {
					if (index > 0) {
						buffer.append(JavaScriptElementLabels.COMMA_STRING); 
					}
					ITypeBinding paramType= parameters[index];
					if (binding.isVarargs() && (index == parameters.length - 1)) {
						getTypeLabel(paramType.getElementType(), (flags & JavaScriptElementLabels.T_TYPE_PARAMETERS), buffer);
						appendDimensions(paramType.getDimensions() - 1, buffer);
						buffer.append(JavaScriptElementLabels.ELLIPSIS_STRING);
					} else {
						getTypeLabel(paramType, (flags & JavaScriptElementLabels.T_TYPE_PARAMETERS), buffer);
					}
				}
			}
		} else {
			if (binding.getParameterTypes().length > 0) {
				buffer.append(JavaScriptElementLabels.ELLIPSIS_STRING);
			}
		}
		buffer.append(')');
		
		if (((flags & JavaScriptElementLabels.M_APP_RETURNTYPE) != 0) && !binding.isConstructor()) {
			buffer.append(JavaScriptElementLabels.DECL_STRING);
			getTypeLabel(binding.getReturnType(), (flags & JavaScriptElementLabels.T_TYPE_PARAMETERS), buffer);
		}
		// post qualification
		if ((flags & JavaScriptElementLabels.M_POST_QUALIFIED) != 0) {
			buffer.append(JavaScriptElementLabels.CONCAT_STRING);
			getTypeLabel(binding.getDeclaringClass(), JavaScriptElementLabels.T_FULLY_QUALIFIED | (flags & JavaScriptElementLabels.P_COMPRESSED), buffer);
		}
	}

	private static ImageDescriptor getTypeImageDescriptor(boolean inner, ITypeBinding binding, int flags) {
		if (binding.isClass()) {
			if ((flags & JavaElementImageProvider.LIGHT_TYPE_ICONS) != 0)
				return JavaPluginImages.DESC_OBJS_CLASSALT;
			if (inner)
				return getInnerClassImageDescriptor(binding.getModifiers());
			return getClassImageDescriptor(binding.getModifiers());
		}
		// primitive type, wildcard
		return null;
	}
		

	private static void getTypeLabel(ITypeBinding binding, long flags, StringBuffer buffer) {
		if ((flags & JavaScriptElementLabels.T_FULLY_QUALIFIED) != 0) {
			final IPackageBinding pack= binding.getPackage();
			if (pack != null && !pack.isUnnamed()) {
				buffer.append(pack.getName());
				buffer.append('.');
			}
		}
		if ((flags & (JavaScriptElementLabels.T_FULLY_QUALIFIED | JavaScriptElementLabels.T_CONTAINER_QUALIFIED)) != 0) {
			final ITypeBinding declaring= binding.getDeclaringClass();
			if (declaring != null) {
				getTypeLabel(declaring, JavaScriptElementLabels.T_CONTAINER_QUALIFIED | (flags & JavaScriptElementLabels.P_COMPRESSED), buffer);
				buffer.append('.');
			}
			final IFunctionBinding declaringMethod= binding.getDeclaringMethod();
			if (declaringMethod != null) {
				getMethodLabel(declaringMethod, 0, buffer);
				buffer.append('.');
			}
		}
		
		if (binding.isArray()) {
			getTypeLabel(binding.getElementType(), flags & JavaScriptElementLabels.T_TYPE_PARAMETERS, buffer);
			appendDimensions(binding.getDimensions(), buffer);
		} else { // type variables, primitive, reftype
			String name= binding.getTypeDeclaration().getName();
			if (name.length() == 0) {
				if (binding.isAnonymous()) {
					ITypeBinding baseType= binding.getSuperclass();
					
					if (baseType != null) {
						StringBuffer anonymBaseType= new StringBuffer();
						getTypeLabel(baseType, flags & JavaScriptElementLabels.T_TYPE_PARAMETERS, anonymBaseType);
						buffer.append(Messages.format(JavaUIMessages.JavaElementLabels_anonym_type, anonymBaseType.toString()));
					} else {
						buffer.append(JavaUIMessages.JavaElementLabels_anonym);
					}
				} else {
					buffer.append("UNKNOWN"); //$NON-NLS-1$
				}
			} else {
				buffer.append(name);
			}
		}


		if ((flags & JavaScriptElementLabels.T_POST_QUALIFIED) != 0) {
			final IFunctionBinding declaringMethod= binding.getDeclaringMethod();
			final ITypeBinding declaringType= binding.getDeclaringClass();
			if (declaringMethod != null) {
				buffer.append(JavaScriptElementLabels.CONCAT_STRING);
				getMethodLabel(declaringMethod, JavaScriptElementLabels.T_FULLY_QUALIFIED | (flags & JavaScriptElementLabels.P_COMPRESSED), buffer);
			} else if (declaringType != null) {
				buffer.append(JavaScriptElementLabels.CONCAT_STRING);
				getTypeLabel(declaringType, JavaScriptElementLabels.T_FULLY_QUALIFIED | (flags & JavaScriptElementLabels.P_COMPRESSED), buffer);
			} else {
				final IPackageBinding pack= binding.getPackage();
				if (pack != null && !pack.isUnnamed()) {
					buffer.append(JavaScriptElementLabels.CONCAT_STRING);
					buffer.append(pack.getName());
				}
			}
		}
	}
	
//	private static void getTypeArgumentsLabel(ITypeBinding[] typeArgs, long flags, StringBuffer buf) {
//		if (typeArgs.length > 0) {
//			buf.append('<');
//			for (int i = 0; i < typeArgs.length; i++) {
//				if (i > 0) {
//					buf.append(JavaScriptElementLabels.COMMA_STRING);
//				}
//				getTypeLabel(typeArgs[i], flags & JavaScriptElementLabels.T_TYPE_PARAMETERS, buf);
//			}
//			buf.append('>');
//		}
//	}

	/**
	 * Returns the label for a Java element with the flags as defined by {@link JavaScriptElementLabels}.
	 * @param binding The binding to render.
	 * @param flags The text flags as defined in {@link JavaScriptElementLabels}
	 * @return the label of the binding
	 */
	public static String getBindingLabel(IBinding binding, long flags) {
		StringBuffer buffer= new StringBuffer(60);
		if (binding instanceof ITypeBinding) {
			getTypeLabel(((ITypeBinding) binding), flags, buffer);
		} else if (binding instanceof IFunctionBinding) {
			getMethodLabel(((IFunctionBinding) binding), flags, buffer);
		} else if (binding instanceof IVariableBinding) {
			final IVariableBinding variable= (IVariableBinding) binding;
			if (variable.isField())
				getFieldLabel(variable, flags, buffer);
			else
				getLocalVariableLabel(variable, flags, buffer);
		}
		return buffer.toString();
	}
	
	/**
	 * Returns the image descriptor for a binding with the flags as defined by {@link JavaElementImageProvider}.
	 * @param binding The binding to get the image for.
	 * @param imageFlags The image flags as defined in {@link JavaElementImageProvider}.
	 * @return the image of the binding or null if there is no image
	 */
	public static ImageDescriptor getBindingImageDescriptor(IBinding binding, int imageFlags) {
		ImageDescriptor baseImage= getBaseImageDescriptor(binding, imageFlags);
		if (baseImage != null) {
			int adornmentFlags= getAdornmentFlags(binding, imageFlags);
			Point size= ((imageFlags & JavaElementImageProvider.SMALL_ICONS) != 0) ? JavaElementImageProvider.SMALL_SIZE : JavaElementImageProvider.BIG_SIZE;
			return new JavaScriptElementImageDescriptor(baseImage, adornmentFlags, size);
		}
		return null;
	}
	

	public static final long DEFAULT_TEXTFLAGS= JavaScriptElementLabels.ALL_DEFAULT;
	public static final int DEFAULT_IMAGEFLAGS= JavaElementImageProvider.OVERLAY_ICONS;
	

	final private long fTextFlags;
	final private int fImageFlags;

	private ImageDescriptorRegistry fRegistry;
	
	/**
	 * Creates a new binding label provider with default text and image flags
	 */
	public BindingLabelProvider() {
		this(DEFAULT_TEXTFLAGS, DEFAULT_IMAGEFLAGS);
	}

	/**
	 * @param textFlags Flags defined in {@link JavaScriptElementLabels}.
	 * @param imageFlags Flags defined in {@link JavaElementImageProvider}.
	 */
	public BindingLabelProvider(final long textFlags, final int imageFlags) {
		fImageFlags= imageFlags;
		fTextFlags= textFlags;
		fRegistry= null;
	}

	/*
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof IBinding) {
			ImageDescriptor baseImage= getBindingImageDescriptor((IBinding) element, fImageFlags);
			if (baseImage != null) {
				return getRegistry().get(baseImage);
			}
		}
		return super.getImage(element);
	}

	private ImageDescriptorRegistry getRegistry() {
		if (fRegistry == null)
			fRegistry= JavaScriptPlugin.getImageDescriptorRegistry();
		return fRegistry;
	}

	/*
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof IBinding) {
			return getBindingLabel((IBinding) element, fTextFlags);
		}
		return super.getText(element);
	}
}
