/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.IModelProviderDescriptor;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.ui.refactoring.model.AbstractSynchronizationCompareAdapter;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

/**
 * Java-aware synchronization compare adapter.
 * 
 * 
 */
public final class JavaSynchronizationCompareAdapter extends AbstractSynchronizationCompareAdapter {

	/** The modelProviderId name */
	private static final String MODEL_PROVIDER_ID= "modelProviderId"; //$NON-NLS-1$

	/** The modelProviders name */
	private static final String MODEL_PROVIDERS= "modelProviders"; //$NON-NLS-1$

	/** The resourcePath name */
	private static final String RESOURCE_PATH= "resourcePath"; //$NON-NLS-1$

	/** The resourceType name */
	private static final String RESOURCE_TYPE= "resourceType"; //$NON-NLS-1$

	/** The resources name */
	private static final String RESOURCES= "resources"; //$NON-NLS-1$

	/** The workingSetName name */
	private static final String WORKING_SET_NAME= "workingSetName"; //$NON-NLS-1$

	/** The workingSets name */
	private static final String WORKING_SETS= "workingSets"; //$NON-NLS-1$

	/**
	 * {@inheritDoc}
	 */
	public ICompareInput asCompareInput(final ISynchronizationContext context, final Object element) {
		if (element instanceof RefactoringDescriptorProxy)
			return super.asCompareInput(context, element);
		final IResource resource= JavaModelProvider.getResource(element);
		if (resource != null)
			return super.asCompareInput(context, resource);
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public ResourceMapping[] restore(final IMemento memento) {
		IMemento[] children= memento.getChildren(RESOURCES);
		final List result= new ArrayList();
		for (int index= 0; index < children.length; index++) {
			final Integer typeInt= children[index].getInteger(RESOURCE_TYPE);
			if (typeInt == null)
				continue;
			final String pathString= children[index].getString(RESOURCE_PATH);
			if (pathString == null)
				continue;
			IResource resource= null;
			final IPath path= new Path(pathString);
			final IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
			switch (typeInt.intValue()) {
				case IResource.ROOT:
					resource= root;
					break;
				case IResource.PROJECT:
					resource= root.getProject(path.lastSegment());
					break;
				case IResource.FILE:
					resource= root.getFile(path);
					break;
				case IResource.FOLDER:
					resource= root.getFolder(path);
					break;
			}
			if (resource != null) {
				final ResourceMapping mapping= JavaSynchronizationContentProvider.getResourceMapping(resource);
				if (mapping != null)
					result.add(mapping);
			}
		}
		children= memento.getChildren(WORKING_SETS);
		for (int index= 0; index < children.length; index++) {
			final String name= children[index].getString(WORKING_SET_NAME);
			if (name == null)
				continue;
			final IWorkingSet set= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(name);
			if (set != null) {
				final ResourceMapping mapping= JavaSynchronizationContentProvider.getResourceMapping(set);
				if (mapping != null)
					result.add(mapping);
			}
		}
		children= memento.getChildren(MODEL_PROVIDERS);
		for (int index= 0; index < children.length; index++) {
			final String id= children[index].getString(MODEL_PROVIDER_ID);
			if (id == null)
				continue;
			final IModelProviderDescriptor descriptor= ModelProvider.getModelProviderDescriptor(id);
			if (descriptor == null)
				continue;
			try {
				final ModelProvider provider= descriptor.getModelProvider();
				if (provider != null) {
					final ResourceMapping mapping= JavaSynchronizationContentProvider.getResourceMapping(provider);
					if (mapping != null)
						result.add(mapping);
				}
			} catch (CoreException event) {
				JavaScriptPlugin.log(event);
			}
		}
		return (ResourceMapping[]) result.toArray(new ResourceMapping[result.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	public void save(final ResourceMapping[] mappings, final IMemento memento) {
		for (int index= 0; index < mappings.length; index++) {
			final Object object= mappings[index].getModelObject();
			if (object instanceof IJavaScriptElement) {
				final IJavaScriptElement element= (IJavaScriptElement) object;
				final IResource resource= (IResource) element.getAdapter(IResource.class);
				if (resource != null) {
					final IMemento child= memento.createChild(RESOURCES);
					child.putInteger(RESOURCE_TYPE, resource.getType());
					child.putString(RESOURCE_PATH, resource.getFullPath().toString());
				}
			}
			if (object instanceof IResource) {
				final IResource resource= (IResource) object;
				final IMemento child= memento.createChild(RESOURCES);
				child.putInteger(RESOURCE_TYPE, resource.getType());
				child.putString(RESOURCE_PATH, resource.getFullPath().toString());
			} else if (object instanceof IWorkingSet)
				memento.createChild(WORKING_SETS).putString(WORKING_SET_NAME, ((IWorkingSet) object).getName());
			else if (object instanceof ModelProvider)
				memento.createChild(MODEL_PROVIDERS).putString(MODEL_PROVIDER_ID, ((ModelProvider) object).getId());
		}
	}
}
