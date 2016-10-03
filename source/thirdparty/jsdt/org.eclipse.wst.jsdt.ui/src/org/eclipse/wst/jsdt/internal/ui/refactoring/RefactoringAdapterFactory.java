/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 package org.eclipse.wst.jsdt.internal.ui.refactoring;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.ui.refactoring.TextEditChangeNode;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.MultiStateCompilationUnitChange;

public class RefactoringAdapterFactory implements IAdapterFactory {

	private static final Class[] ADAPTER_LIST= new Class[] {
		TextEditChangeNode.class
	};

	public Class[] getAdapterList() {
		return ADAPTER_LIST;
	}

	public Object getAdapter(Object object, Class key) {
		if (!TextEditChangeNode.class.equals(key))
			return null;
		if (!(object instanceof CompilationUnitChange) && !(object instanceof MultiStateCompilationUnitChange))
			return null;
		return new CompilationUnitChangeNode((TextEditBasedChange)object);
	}
}
