/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matt Chapman, mpchapman@gmail.com - 89977 Make JDT .java agnostic
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.viewsupport;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaWorkbenchAdapter;
import org.eclipse.wst.jsdt.ui.JavaScriptElementImageDescriptor;

/**
 * Default strategy of the Java plugin for the construction of Java element icons.
 */
public class JavaElementImageProvider {

	/**
	 * Flags for the JavaImageLabelProvider:
	 * Generate images with overlays.
	 */
	public final static int OVERLAY_ICONS= 0x1;

	/**
	 * Generate small sized images.
	 */
	public final static int SMALL_ICONS= 0x2;
	
	/**
	 * Use the 'light' style for rendering types.
	 */	
	public final static int LIGHT_TYPE_ICONS= 0x4;	


	public static final Point SMALL_SIZE= new Point(16, 16);
	public static final Point BIG_SIZE= new Point(22, 16);

	private static ImageDescriptor DESC_OBJ_PROJECT_CLOSED;	
	private static ImageDescriptor DESC_OBJ_PROJECT;	
	{
		ISharedImages images= JavaScriptPlugin.getDefault().getWorkbench().getSharedImages(); 
		DESC_OBJ_PROJECT_CLOSED= images.getImageDescriptor(IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED);
		DESC_OBJ_PROJECT= 		 images.getImageDescriptor(IDE.SharedImages.IMG_OBJ_PROJECT);
	}
	
	private ImageDescriptorRegistry fRegistry;
		
	public JavaElementImageProvider() {
		fRegistry= null; // lazy initialization
	}	
		
	/**
	 * Returns the icon for a given element. The icon depends on the element type
	 * and element properties. If configured, overlay icons are constructed for
	 * <code>ISourceReference</code>s.
	 * @param flags Flags as defined by the JavaImageLabelProvider
	 */
	public Image getImageLabel(Object element, int flags) {
		return getImageLabel(computeDescriptor(element, flags));
	}
	
	private Image getImageLabel(ImageDescriptor descriptor){
		if (descriptor == null) 
			return null;	
		return getRegistry().get(descriptor);
	}
	
	private ImageDescriptorRegistry getRegistry() {
		if (fRegistry == null) {
			fRegistry= JavaScriptPlugin.getImageDescriptorRegistry();
		}
		return fRegistry;
	}
	

	private ImageDescriptor computeDescriptor(Object element, int flags){
		if (element instanceof IJavaScriptElement) {
			return getJavaImageDescriptor((IJavaScriptElement) element, flags);
		} else if (element instanceof IFile) {
			IFile file= (IFile) element;
			if (JavaScriptCore.isJavaScriptLikeFileName(file.getName())) {
				return getCUResourceImageDescriptor(file, flags); // image for a CU not on the build path
			}
			return getWorkbenchImageDescriptor(file, flags);
		} else if (element instanceof IAdaptable) {
			return getWorkbenchImageDescriptor((IAdaptable) element, flags);
		}
		return null;
	}
	
	private static boolean showOverlayIcons(int flags) {
		return (flags & OVERLAY_ICONS) != 0;
	}
		
	private static boolean useSmallSize(int flags) {
		return (flags & SMALL_ICONS) != 0;
	}
	
	private static boolean useLightIcons(int flags) {
		return (flags & LIGHT_TYPE_ICONS) != 0;
	}	

	/**
	 * Returns an image descriptor for a compilation unit not on the class path.
	 * The descriptor includes overlays, if specified.
	 */
	public ImageDescriptor getCUResourceImageDescriptor(IFile file, int flags) {
		Point size= useSmallSize(flags) ? SMALL_SIZE : BIG_SIZE;
		return new JavaScriptElementImageDescriptor(JavaPluginImages.DESC_OBJS_CUNIT_RESOURCE, 0, size);
	}	
		
	/**
	 * Returns an image descriptor for a java element. The descriptor includes overlays, if specified.
	 */
	public ImageDescriptor getJavaImageDescriptor(IJavaScriptElement element, int flags) {
		Point size= useSmallSize(flags) ? SMALL_SIZE : BIG_SIZE;

		ImageDescriptor baseDesc= getBaseImageDescriptor(element, flags);
		if (baseDesc != null) {
			int adornmentFlags= computeJavaAdornmentFlags(element, flags);
			return new JavaScriptElementImageDescriptor(baseDesc, adornmentFlags, size);
		}
		return new JavaScriptElementImageDescriptor(JavaPluginImages.DESC_OBJS_GHOST, 0, size);
	}

	/**
	 * Returns an image descriptor for a IAdaptable. The descriptor includes overlays, if specified (only error ticks apply).
	 * Returns <code>null</code> if no image could be found.
	 */	
	public ImageDescriptor getWorkbenchImageDescriptor(IAdaptable adaptable, int flags) {
		IWorkbenchAdapter wbAdapter= (IWorkbenchAdapter) adaptable.getAdapter(IWorkbenchAdapter.class);
		if (wbAdapter == null) {
			return null;
		}
		ImageDescriptor descriptor= wbAdapter.getImageDescriptor(adaptable);
		if (descriptor == null) {
			return null;
		}

		Point size= useSmallSize(flags) ? SMALL_SIZE : BIG_SIZE;
		return new JavaScriptElementImageDescriptor(descriptor, 0, size);
	}
	
	// ---- Computation of base image key -------------------------------------------------
	
	/**
	 * Returns an image descriptor for a java element. This is the base image, no overlays.
	 */
	public ImageDescriptor getBaseImageDescriptor(IJavaScriptElement element, int renderFlags) {

		try {			
			switch (element.getElementType()) {	
				case IJavaScriptElement.INITIALIZER:
					return JavaPluginImages.DESC_MISC_PRIVATE; // 23479
				case IJavaScriptElement.METHOD: {
					IFunction method= (IFunction) element;
					IType declType= method.getDeclaringType();
					int flags= method.getFlags();
//					if (declType.isEnum() && isDefaultFlag(flags) && method.isConstructor())
//						return JavaPluginImages.DESC_MISC_PRIVATE;
					return getMethodImageDescriptor(false, flags);				
				}
				case IJavaScriptElement.FIELD: {
					IMember member= (IMember) element;
					IType declType= member.getDeclaringType();
					return getFieldImageDescriptor(false, member.getFlags());
				}
				case IJavaScriptElement.LOCAL_VARIABLE:
					return JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE;				
				
				case IJavaScriptElement.IMPORT_DECLARATION:
					return JavaPluginImages.DESC_OBJS_IMPDECL;
					
				case IJavaScriptElement.IMPORT_CONTAINER:
					return JavaPluginImages.DESC_OBJS_IMPCONT;
				
				case IJavaScriptElement.TYPE: {
					IType type= (IType) element;

					IType declType= type.getDeclaringType();
					boolean isInner= declType != null;
					return getTypeImageDescriptor(isInner, false, type.getFlags(), useLightIcons(renderFlags));
				}

				case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT: {
					IPackageFragmentRoot root= (IPackageFragmentRoot) element;
					if (root.isArchive()) {
						IPath attach= root.getSourceAttachmentPath();
						if (root.isExternal()) {
							if (attach == null) {
								return JavaPluginImages.DESC_OBJS_EXTJAR;
							} else {
								return JavaPluginImages.DESC_OBJS_EXTJAR_WSRC;
							}
						} else {
							if (attach == null) {
								return JavaPluginImages.DESC_OBJS_JAR;
							} else {
								return JavaPluginImages.DESC_OBJS_JAR_WSRC;
							}
						}							
					} else {
						return JavaPluginImages.DESC_OBJS_PACKFRAG_ROOT;
					}
				}
				
				case IJavaScriptElement.PACKAGE_FRAGMENT:
					return getPackageFragmentIcon(element, renderFlags);

					
				case IJavaScriptElement.JAVASCRIPT_UNIT:
					return JavaPluginImages.DESC_OBJS_CUNIT;
					
				case IJavaScriptElement.CLASS_FILE:
					/* this is too expensive for large packages
					try {
						IClassFile cfile= (IClassFile)element;
						if (cfile.isClass())
							return JavaPluginImages.IMG_OBJS_CFILECLASS;
						return JavaPluginImages.IMG_OBJS_CFILEINT;
					} catch(JavaScriptModelException e) {
						// fall through;
					}*/
					return JavaPluginImages.DESC_OBJS_CFILE;
					
				case IJavaScriptElement.JAVASCRIPT_PROJECT: 
					IJavaScriptProject jp= (IJavaScriptProject)element;
					if (jp.getProject().isOpen()) {
						IProject project= jp.getProject();
						IWorkbenchAdapter adapter= (IWorkbenchAdapter)project.getAdapter(IWorkbenchAdapter.class);
						if (adapter != null) {
							ImageDescriptor result= adapter.getImageDescriptor(project);
							if (result != null)
								return result;
						}
						return DESC_OBJ_PROJECT;
					}
					return DESC_OBJ_PROJECT_CLOSED;
					
				case IJavaScriptElement.JAVASCRIPT_MODEL:
					return JavaPluginImages.DESC_OBJS_JAVA_MODEL;
					
				default:
					// ignore. Must be a new, yet unknown Java element
					// give an advanced IWorkbenchAdapter the chance
					IWorkbenchAdapter wbAdapter= (IWorkbenchAdapter) element.getAdapter(IWorkbenchAdapter.class);
					if (wbAdapter != null && !(wbAdapter instanceof JavaWorkbenchAdapter)) { // avoid recursion
						ImageDescriptor imageDescriptor= wbAdapter.getImageDescriptor(element);
						if (imageDescriptor != null) {
							return imageDescriptor;
						}
					}
					return JavaPluginImages.DESC_OBJS_GHOST;
			}
		
		} catch (JavaScriptModelException e) {
			if (e.isDoesNotExist())
				return JavaPluginImages.DESC_OBJS_UNKNOWN;
			JavaScriptPlugin.log(e);
			return JavaPluginImages.DESC_OBJS_GHOST;
		}
	}
	
//	private static boolean isDefaultFlag(int flags) {
//		return !Flags.isPublic(flags) && !Flags.isProtected(flags) && !Flags.isPrivate(flags);
//	}
//	
	protected ImageDescriptor getPackageFragmentIcon(IJavaScriptElement element, int renderFlags) throws JavaScriptModelException {
//		IPackageFragment fragment= (IPackageFragment)element;
//		boolean containsJavaElements= false;
//		try {
//			containsJavaElements= fragment.hasChildren();
//		} catch(JavaScriptModelException e) {
//			// assuming no children;
//		}
//		if(!containsJavaElements && (fragment.getNonJavaResources().length > 0))
//			return JavaPluginImages.DESC_OBJS_EMPTY_PACKAGE_RESOURCES;
//		else if (!containsJavaElements)
//			return JavaPluginImages.DESC_OBJS_EMPTY_PACKAGE;
		return JavaPluginImages.DESC_OBJS_PACKAGE;
	}
	
	public void dispose() {
	}	

	// ---- Methods to compute the adornments flags ---------------------------------
	
	private int computeJavaAdornmentFlags(IJavaScriptElement element, int renderFlags) {
		int flags= 0;
		if (showOverlayIcons(renderFlags) && element instanceof IMember) {
			try {
				IMember member= (IMember) element;
				
				if (element.getElementType() == IJavaScriptElement.METHOD && ((IFunction)element).isConstructor())
					flags |= JavaScriptElementImageDescriptor.CONSTRUCTOR;
					
				int modifiers= member.getFlags();
				if (Flags.isAbstract(modifiers) && confirmAbstract(member))
					flags |= JavaScriptElementImageDescriptor.ABSTRACT;
				if (Flags.isStatic(modifiers))
					flags |= JavaScriptElementImageDescriptor.STATIC;
				
				if (Flags.isDeprecated(modifiers))
					flags |= JavaScriptElementImageDescriptor.DEPRECATED;			
			} catch (JavaScriptModelException e) {
				// do nothing. Can't compute runnable adornment or get flags
			}
		}
		return flags;
	}
		
	private static boolean confirmAbstract(IMember element) throws JavaScriptModelException {
		// never show the abstract symbol on interfaces or members in interfaces
		if (element.getElementType() == IJavaScriptElement.TYPE) {
			return true;
		}
		return true;
	}
	
	public static ImageDescriptor getMethodImageDescriptor(boolean isInInterfaceOrAnnotation, int flags) {
		if (Flags.isPublic(flags) || isInInterfaceOrAnnotation)
			return JavaPluginImages.DESC_MISC_PUBLIC;
		if (Flags.isProtected(flags))
			return JavaPluginImages.DESC_MISC_PROTECTED;
		if (Flags.isPrivate(flags))
			return JavaPluginImages.DESC_MISC_PRIVATE;
		
		// by default return public method image
		return JavaPluginImages.DESC_MISC_PUBLIC;
	}
		
	public static ImageDescriptor getFieldImageDescriptor(boolean isInInterfaceOrAnnotation, int flags) {
		if (Flags.isPublic(flags) || isInInterfaceOrAnnotation)
			return JavaPluginImages.DESC_FIELD_PUBLIC;
		if (Flags.isProtected(flags))
			return JavaPluginImages.DESC_FIELD_PROTECTED;
		if (Flags.isPrivate(flags))
			return JavaPluginImages.DESC_FIELD_PRIVATE;

		// by default return public field image
		return JavaPluginImages.DESC_FIELD_PUBLIC;
	}		
	
	public static ImageDescriptor getTypeImageDescriptor(boolean isInner, boolean isInInterfaceOrAnnotation, int flags, boolean useLightIcons) {
		if (useLightIcons) {
			return JavaPluginImages.DESC_OBJS_CLASSALT;
		}
		if (isInner) {
			return getInnerClassImageDescriptor(isInInterfaceOrAnnotation, flags);
		}
		return getClassImageDescriptor(flags);
		
	}
	
	
	public static Image getDecoratedImage(ImageDescriptor baseImage, int adornments, Point size) {
		return JavaScriptPlugin.getImageDescriptorRegistry().get(new JavaScriptElementImageDescriptor(baseImage, adornments, size));
	}
	

	private static ImageDescriptor getClassImageDescriptor(int flags) {
		if (Flags.isPublic(flags) || Flags.isPrivate(flags))
			return JavaPluginImages.DESC_OBJS_CLASS;
		else
			return JavaPluginImages.DESC_OBJS_CLASS_DEFAULT;
	}
	
	private static ImageDescriptor getInnerClassImageDescriptor(boolean isInInterfaceOrAnnotation, int flags) {
		if (Flags.isPublic(flags) || isInInterfaceOrAnnotation)
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_PUBLIC;
		else if (Flags.isPrivate(flags))
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_PRIVATE;
		else
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_DEFAULT;
	}
}
