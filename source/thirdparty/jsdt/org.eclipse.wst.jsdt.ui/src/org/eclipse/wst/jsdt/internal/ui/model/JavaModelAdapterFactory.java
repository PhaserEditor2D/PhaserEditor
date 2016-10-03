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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.team.core.mapping.IResourceMappingMerger;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;

/**
 * Adaptor factory for model support.
 * 
 * 
 */
public final class JavaModelAdapterFactory implements IAdapterFactory {

	/**
	 * {@inheritDoc}
	 */
	public Object getAdapter(final Object adaptable, final Class adapter) {
		if (adaptable instanceof JavaModelProvider) {
			if (adapter == IResourceMappingMerger.class)
				return new JavaModelMerger((ModelProvider) adaptable);
			else if (adapter == ISynchronizationCompareAdapter.class)
				return new JavaSynchronizationCompareAdapter();
		} else if (adaptable instanceof RefactoringHistory) {
			if (adapter == ResourceMapping.class)
				return new JavaRefactoringHistoryResourceMapping((RefactoringHistory) adaptable);
			else if (adapter == IResource.class)
				return new JavaRefactoringHistoryResourceMapping((RefactoringHistory) adaptable).getResource();
		} else if (adaptable instanceof RefactoringDescriptorProxy) {
			if (adapter == ResourceMapping.class)
				return new JavaRefactoringDescriptorResourceMapping((RefactoringDescriptorProxy) adaptable);
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Class[] getAdapterList() {
		return new Class[] { ResourceMapping.class, ISynchronizationCompareAdapter.class, IResource.class};
	}
}
