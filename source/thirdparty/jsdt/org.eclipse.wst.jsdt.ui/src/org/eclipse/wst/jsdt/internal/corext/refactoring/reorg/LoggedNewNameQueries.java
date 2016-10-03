/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.reorg;

import org.eclipse.core.resources.IResource;
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;

/**
 * Logged implementation of new name queries.
 * 
 * 
 */
public final class LoggedNewNameQueries implements INewNameQueries {

	/** Default implementation of a new name query */
	private final class NewNameQuery implements INewNameQuery {

		/** The name */
		private final String fName;

		/** The object */
		private final Object fObject;

		/**
		 * Creates a new new name query.
		 * 
		 * @param object
		 *            the object
		 * @param name
		 *            the initial suggested name
		 */
		public NewNameQuery(final Object object, String name) {
			fObject= object;
			fName= name;
		}

		/**
		 * Returns the new name of the compilation unit, without any extension.
		 * 
		 * @return the new name, or <code>null</code>
		 */
		private String getCompilationUnitName() {
			String name= fLog.getNewName(fObject);
			if (name != null) {
				int index= name.lastIndexOf('.');
				if (index > 0)
					name= name.substring(0, index);
			}
			return name;
		}

		/**
		 * {@inheritDoc}
		 */
		public String getNewName() {
			String name= null;
			if (fObject instanceof IJavaScriptUnit)
				name= getCompilationUnitName();
			else
				name= fLog.getNewName(fObject);
			if (name == null)
				name= fName;
			return fName;
		}
	}

	/** Null implementation of new name query */
	private static final class NullNewNameQuery implements INewNameQuery {

		/**
		 * {@inheritDoc}
		 */
		public String getNewName() {
			return "null"; //$NON-NLS-1$
		}
	}

	/** The reorg execution log */
	private final ReorgExecutionLog fLog;

	/**
	 * Creates a new logged new name queries.
	 * 
	 * @param log
	 *            the reorg execution log
	 */
	public LoggedNewNameQueries(final ReorgExecutionLog log) {
		fLog= log;
	}

	/**
	 * {@inheritDoc}
	 */
	public INewNameQuery createNewCompilationUnitNameQuery(final IJavaScriptUnit unit, final String initialSuggestedName) {
		return new NewNameQuery(unit, initialSuggestedName);
	}

	/**
	 * {@inheritDoc}
	 */
	public INewNameQuery createNewPackageFragmentRootNameQuery(final IPackageFragmentRoot root, final String initialSuggestedName) {
		return new NewNameQuery(root, initialSuggestedName);
	}

	/**
	 * {@inheritDoc}
	 */
	public INewNameQuery createNewPackageNameQuery(final IPackageFragment fragment, final String initialSuggestedName) {
		return new NewNameQuery(fragment, initialSuggestedName);
	}

	/**
	 * {@inheritDoc}
	 */
	public INewNameQuery createNewResourceNameQuery(final IResource resource, final String initialSuggestedName) {
		return new NewNameQuery(resource, initialSuggestedName);
	}

	/**
	 * {@inheritDoc}
	 */
	public INewNameQuery createNullQuery() {
		return new NullNewNameQuery();
	}

	/**
	 * {@inheritDoc}
	 */
	public INewNameQuery createStaticQuery(final String name) {
		return new INewNameQuery() {

			public String getNewName() {
				return name;
			}
		};
	}
}
