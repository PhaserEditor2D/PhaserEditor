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
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.IAccessRule;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJsGlobalScopeContainerInitializerExtension;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.util.JSDScopeUiUtil;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.ui.ISharedImages;
import org.eclipse.wst.jsdt.ui.JavaScriptElementImageDescriptor;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration;
import org.eclipse.wst.jsdt.ui.wizards.ClasspathAttributeConfiguration.ClasspathAttributeAccess;

public class CPListLabelProvider extends LabelProvider {
		
	private String fNewLabel, fClassLabel, fCreateLabel;
		
	private ImageDescriptorRegistry fRegistry;
	private ISharedImages fSharedImages;

	private ImageDescriptor fProjectImage;
	
	private ClasspathAttributeConfigurationDescriptors fAttributeDescriptors;
	
	
	public CPListLabelProvider() {
		fNewLabel= NewWizardMessages.CPListLabelProvider_new; 
		fClassLabel= NewWizardMessages.CPListLabelProvider_classcontainer; 
		fCreateLabel= NewWizardMessages.CPListLabelProvider_willbecreated; 
		fRegistry= JavaScriptPlugin.getImageDescriptorRegistry();
	
		fSharedImages= JavaScriptUI.getSharedImages();

		IWorkbench workbench= JavaScriptPlugin.getDefault().getWorkbench();
		
		fProjectImage= workbench.getSharedImages().getImageDescriptor(IDE.SharedImages.IMG_OBJ_PROJECT);
		fAttributeDescriptors= JavaScriptPlugin.getDefault().getClasspathAttributeConfigurationDescriptors();
	}
	
	public String getText(Object element) {
		if (element instanceof CPListElement) {
			return getCPListElementText((CPListElement) element);
		} else if (element instanceof CPListElementAttribute) {
			CPListElementAttribute attribute= (CPListElementAttribute) element;
			String text= getCPListElementAttributeText(attribute);
//			if (attribute.isInNonModifiableContainer()) {
//				return Messages.format(NewWizardMessages.CPListLabelProvider_non_modifiable_attribute, text); 
//			}
			return text;
		} else if (element instanceof CPUserLibraryElement) {
			return getCPUserLibraryText((CPUserLibraryElement) element);
		} else if (element instanceof IAccessRule) {
			IAccessRule rule= (IAccessRule) element;
			return Messages.format(NewWizardMessages.CPListLabelProvider_access_rules_label, new String[] { AccessRulesLabelProvider.getResolutionLabel(rule.getKind()), rule.getPattern().toString()}); 
		}
		return super.getText(element);
	}
	
	public String getCPUserLibraryText(CPUserLibraryElement element) {
		return element.getName();
	}

	public String getCPListElementAttributeText(CPListElementAttribute attrib) {
		String notAvailable= NewWizardMessages.CPListLabelProvider_none; 
		String key= attrib.getKey();
		if (key.equals(CPListElement.EXCLUSION)) {
			String arg= null;
			IPath[] patterns= (IPath[]) attrib.getValue();
			if (patterns != null && patterns.length > 0) {
				int patternsCount= 0;
				StringBuffer buf= new StringBuffer();
				for (int i= 0; i < patterns.length; i++) {
					String pattern= patterns[i].toString();
					if (pattern.length() > 0) {
						if (patternsCount > 0) {
							buf.append(NewWizardMessages.CPListLabelProvider_exclusion_filter_separator); 
						}
						buf.append(pattern);
						patternsCount++;
					}
				}
				if (patternsCount > 0) {
					arg= buf.toString();
				} else {
					arg= notAvailable;
				}
			} else {
				arg= notAvailable;
			}
			return Messages.format(NewWizardMessages.CPListLabelProvider_exclusion_filter_label, new String[] { arg }); 
		} else if (key.equals(CPListElement.INCLUSION)) {
			String arg= null;
			IPath[] patterns= (IPath[]) attrib.getValue();
			if (patterns != null && patterns.length > 0) {
				int patternsCount= 0;
				StringBuffer buf= new StringBuffer();
				for (int i= 0; i < patterns.length; i++) {
					String pattern= patterns[i].toString();
					if (pattern.length() > 0) {
						if (patternsCount > 0) {
							buf.append(NewWizardMessages.CPListLabelProvider_inclusion_filter_separator);
						}
						buf.append(pattern);
						patternsCount++;
					}					
				}
				if (patternsCount > 0) {
					arg= buf.toString();
				} else {
					arg= notAvailable;
				}
			} else {
				arg= NewWizardMessages.CPListLabelProvider_all; 
			}
			return Messages.format(NewWizardMessages.CPListLabelProvider_inclusion_filter_label, new String[] { arg });
		} else if (key.equals(CPListElement.ACCESSRULES)) {
			IAccessRule[] rules= (IAccessRule[]) attrib.getValue();
			int nRules= rules != null ? rules.length : 0;
			
			int parentKind= attrib.getParent().getEntryKind();
			if (parentKind == IIncludePathEntry.CPE_PROJECT) {
				Boolean combined= (Boolean) attrib.getParent().getAttribute(CPListElement.COMBINE_ACCESSRULES);
				if (nRules > 0) {
					if (combined.booleanValue()) {
						return Messages.format(NewWizardMessages.CPListLabelProvider_project_access_rules_combined, String.valueOf(nRules)); 
					} else {
						return Messages.format(NewWizardMessages.CPListLabelProvider_project_access_rules_not_combined, String.valueOf(nRules)); 
					}
				} else {
					return NewWizardMessages.CPListLabelProvider_project_access_rules_no_rules; 
				}
			} else if (parentKind == IIncludePathEntry.CPE_CONTAINER) {
				if (nRules > 0) {
					return Messages.format(NewWizardMessages.CPListLabelProvider_container_access_rules, String.valueOf(nRules)); 
				} else {
					return NewWizardMessages.CPListLabelProvider_container_no_access_rules; 
				}
			} else {
				if (nRules > 0) {
					return Messages.format(NewWizardMessages.CPListLabelProvider_access_rules_enabled, String.valueOf(nRules)); 
				} else {
					return NewWizardMessages.CPListLabelProvider_access_rules_disabled; 
				}
			}
		} else {
			ClasspathAttributeConfiguration config= fAttributeDescriptors.get(key);
			if (config != null) {
				ClasspathAttributeAccess access= attrib.getClasspathAttributeAccess();
				String nameLabel= config.getNameLabel(access);
				String valueLabel= config.getValueLabel(access);
				return Messages.format(NewWizardMessages.CPListLabelProvider_attribute_label, new String[] { nameLabel, valueLabel }); 
			}
			String arg= (String) attrib.getValue();
			if (arg == null) {
				arg= notAvailable; 
			}
			return Messages.format(NewWizardMessages.CPListLabelProvider_attribute_label, new String[] { key, arg }); 
		}
	}
	
	public String getCPListElementText(CPListElement cpentry) {
		IPath path= cpentry.getPath();
		switch (cpentry.getEntryKind()) {
			case IIncludePathEntry.CPE_LIBRARY: {
				
				JsGlobalScopeContainerInitializer cpinit = cpentry.getContainerInitializer();
				if(cpinit!=null) {
					String displayText = cpinit.getDescription(cpentry.getPath(), cpentry.getJavaProject());
					if(displayText!=null)
						return displayText;
				}
				
				IResource resource= cpentry.getResource();
				if (resource instanceof IContainer) {
					StringBuffer buf= new StringBuffer(path.makeRelative().toString());
					IPath linkTarget= cpentry.getLinkTarget();
					if (linkTarget != null) {
						buf.append(JavaScriptElementLabels.CONCAT_STRING);
						buf.append(linkTarget.toOSString());
					}
					buf.append(' ');
					buf.append(fClassLabel);
					if (!resource.exists()) {
						buf.append(' ');
						if (cpentry.isMissing()) {
							buf.append(fCreateLabel);
						} else {
							buf.append(fNewLabel);
						}
					}
					return buf.toString();
				} else if (ArchiveFileFilter.isArchivePath(path) || path.getFileExtension() == null) {
					return getPathString(path, resource == null);
				}
				// should not get here
				return path.makeRelative().toString();
			}
			case IIncludePathEntry.CPE_PROJECT:
				return path.lastSegment();
			case IIncludePathEntry.CPE_CONTAINER:
				try {
					IJsGlobalScopeContainer container= JavaScriptCore.getJsGlobalScopeContainer(path, cpentry.getJavaProject());
					
					if (container != null) {
						
						
						return container.getDescription();
					}
					JsGlobalScopeContainerInitializer initializer= JavaScriptCore.getJsGlobalScopeContainerInitializer(path.segment(0));
					if (initializer != null) {
						String description= initializer.getDescription(path, cpentry.getJavaProject());
						return Messages.format(NewWizardMessages.CPListLabelProvider_unbound_library, description); 
					}
				} catch (JavaScriptModelException e) {
	
				}
				return path.toString();
			case IIncludePathEntry.CPE_SOURCE: {
				StringBuffer buf= new StringBuffer(path.makeRelative().toString());
				IPath linkTarget= cpentry.getLinkTarget();
				if (linkTarget != null) {
					buf.append(JavaScriptElementLabels.CONCAT_STRING);
					buf.append(linkTarget.toOSString());
				}
				IResource resource= cpentry.getResource();
				if (resource != null && !resource.exists()) {
					buf.append(' ');
					if (cpentry.isMissing()) {
						buf.append(fCreateLabel);
					} else {
						buf.append(fNewLabel);
					}
				} else if (cpentry.getOrginalPath() == null) {
					buf.append(' ');
					buf.append(fNewLabel);
				}
				return buf.toString();
			}
			default:
				// pass
		}
		return NewWizardMessages.CPListLabelProvider_unknown_element_label; 
	}
	
	private String getPathString(IPath path, boolean isExternal) {
		if (ArchiveFileFilter.isArchivePath(path)) {
			IPath appendedPath= path.removeLastSegments(1);
			String appended= isExternal ? appendedPath.toOSString() : appendedPath.makeRelative().toString();
			return Messages.format(NewWizardMessages.CPListLabelProvider_twopart, new String[] { path.lastSegment(), appended }); 
		} else {
			return isExternal ? path.toOSString() : path.makeRelative().toString();
		}
	}
	
	private ImageDescriptor getCPListElementBaseImage(CPListElement cpentry) {
		
		IJsGlobalScopeContainerInitializerExtension init = JSDScopeUiUtil.getContainerUiInitializer(cpentry.getPath());
		if(init!=null ) {
			IPath entPath = cpentry.getPath();
			ImageDescriptor image = init.getImage(entPath, cpentry.toString(), cpentry.getJavaProject());
			if(image!=null) return image;
		}
		
		switch (cpentry.getEntryKind()) {
			case IIncludePathEntry.CPE_SOURCE:
				if (cpentry.getPath().segmentCount() == 1) {
					return fProjectImage;
				} else {
					return fSharedImages.getImageDescriptor(ISharedImages.IMG_OBJS_PACKFRAG_ROOT);
				}
			case IIncludePathEntry.CPE_LIBRARY:
				IResource res= cpentry.getResource();
				if (res == null) {
					return fSharedImages.getImageDescriptor(ISharedImages.IMG_OBJS_EXTERNAL_ARCHIVE_WITH_SOURCE);
				} else if (res instanceof IFile) {
					return fSharedImages.getImageDescriptor(ISharedImages.IMG_OBJS_JAR_WITH_SOURCE);
				} else {
					return fSharedImages.getImageDescriptor(ISharedImages.IMG_OBJS_PACKFRAG_ROOT);
				}
			case IIncludePathEntry.CPE_PROJECT:
				return fProjectImage;
			case IIncludePathEntry.CPE_VARIABLE:
				ImageDescriptor variableImage= fSharedImages.getImageDescriptor(ISharedImages.IMG_OBJS_CLASSPATH_VAR_ENTRY);
				if (cpentry.isDeprecated()) {
					return new JavaScriptElementImageDescriptor(variableImage, JavaScriptElementImageDescriptor.DEPRECATED, JavaElementImageProvider.SMALL_SIZE);
				}
				return variableImage;
			case IIncludePathEntry.CPE_CONTAINER:
				return fSharedImages.getImageDescriptor(ISharedImages.IMG_OBJS_LIBRARY);
			default:
				return null;
		}
	}			
		
	public Image getImage(Object element) {
		if (element instanceof CPListElement) {
			CPListElement cpentry= (CPListElement) element;
			ImageDescriptor imageDescriptor= getCPListElementBaseImage(cpentry);
			if (imageDescriptor != null) {
				if (cpentry.isMissing()) {
					imageDescriptor= new JavaScriptElementImageDescriptor(imageDescriptor, JavaScriptElementImageDescriptor.WARNING, JavaElementImageProvider.SMALL_SIZE);
				}
				return fRegistry.get(imageDescriptor);
			}
		} else if (element instanceof CPListElementAttribute) {
			CPListElementAttribute attribute= (CPListElementAttribute) element;
			String key= (attribute).getKey();
			if (key.equals(CPListElement.EXCLUSION)) {
				return fRegistry.get(JavaPluginImages.DESC_OBJS_EXCLUSION_FILTER_ATTRIB);
			} else if (key.equals(CPListElement.INCLUSION)) {
				return fRegistry.get(JavaPluginImages.DESC_OBJS_INCLUSION_FILTER_ATTRIB);
			} else if (key.equals(CPListElement.ACCESSRULES)) {
				return fRegistry.get(JavaPluginImages.DESC_OBJS_ACCESSRULES_ATTRIB);
			} else {
				ClasspathAttributeConfiguration config= fAttributeDescriptors.get(key);
				if (config != null) {
					return fRegistry.get(config.getImageDescriptor(attribute.getClasspathAttributeAccess()));
				}
			}
			return  fSharedImages.getImage(ISharedImages.IMG_OBJS_CLASSPATH_VAR_ENTRY);
		} else if (element instanceof CPUserLibraryElement) {
			return  fSharedImages.getImage(ISharedImages.IMG_OBJS_LIBRARY);
		} else if (element instanceof IAccessRule) {
			IAccessRule rule= (IAccessRule) element;
			return AccessRulesLabelProvider.getResolutionImage(rule.getKind());
		}
		return null;
	}


}	
