/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.changes;

import org.eclipse.core.resources.IResource;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

public final class CreateCompilationUnitChange extends CreateTextFileChange {

	private final IJavaScriptUnit fUnit;

	public CreateCompilationUnitChange(IJavaScriptUnit unit, String source, String encoding) {
		super(unit.getResource().getFullPath(), source, encoding, "java"); //$NON-NLS-1$
		fUnit= unit;
	}

	public String getName() {
		return Messages.format(RefactoringCoreMessages.CompilationUnitChange_label, new String[] { fUnit.getElementName(), getPath(fUnit.getResource()) });
	}

	private String getPath(IResource resource) {
		final StringBuffer buffer= new StringBuffer(resource.getProject().getName());
		final String path= resource.getParent().getProjectRelativePath().toString();
		if (path.length() > 0) {
			buffer.append('/');
			buffer.append(path);
		}
		return buffer.toString();
	}
}
