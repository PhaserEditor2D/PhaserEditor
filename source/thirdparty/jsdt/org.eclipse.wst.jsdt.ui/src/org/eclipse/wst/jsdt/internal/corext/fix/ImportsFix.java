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
package org.eclipse.wst.jsdt.internal.corext.fix;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.search.TypeNameMatch;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.OrganizeImportsOperation;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.OrganizeImportsOperation.IChooseImportQuery;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;

public class ImportsFix extends AbstractFix {
	
	private static final class AmbiguousImportException extends RuntimeException {
		private static final long serialVersionUID= 1L;
	}

	public static IFix createCleanUp(final JavaScriptUnit cu, CodeGenerationSettings settings, boolean organizeImports, RefactoringStatus status) throws CoreException {
		if (!organizeImports)
			return null;
		
		IChooseImportQuery query= new IChooseImportQuery() {
			public TypeNameMatch[] chooseImports(TypeNameMatch[][] openChoices, ISourceRange[] ranges) {
				throw new AmbiguousImportException();
			}
		};
		OrganizeImportsOperation op= new OrganizeImportsOperation((IJavaScriptUnit)cu.getJavaElement(), cu, settings.importIgnoreLowercase, false, false, query);
		final TextEdit edit;
		try {
			edit= op.createTextEdit(null);
		} catch (AmbiguousImportException e) {
			status.addInfo(Messages.format(ActionMessages.OrganizeImportsAction_multi_error_unresolvable, getLocationString(cu)));
			return null;
		}
		
		if (op.getParseError() != null) {
			status.addInfo(Messages.format(ActionMessages.OrganizeImportsAction_multi_error_parse, getLocationString(cu)));
			return null;
		}
		
		if (edit == null)
			return null;
		
		if (op.getNumberOfImportsAdded() == 0 && op.getNumberOfImportsRemoved() == 0)
			return null;

		return new IFix() {
			
			public TextChange createChange() throws CoreException {
				CompilationUnitChange result= new CompilationUnitChange(getDescription(), getCompilationUnit());
				result.setEdit(edit);
				String label= getDescription();
				result.addTextEditGroup(new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label))));
	            return result;
            }

			public IJavaScriptUnit getCompilationUnit() {
	            return (IJavaScriptUnit)cu.getJavaElement();
            }

			public String getDescription() {
	            return FixMessages.ImportsFix_OrganizeImports_Description;
            }

			public IStatus getStatus() {
	            return StatusInfo.OK_STATUS;
         	  }
    	};
    }

	private static String getLocationString(final JavaScriptUnit cu) {
		return cu.getJavaElement().getPath().makeRelative().toString();
	}
	
	protected ImportsFix(String name, JavaScriptUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
	    super(name, compilationUnit, fixRewriteOperations);
    }
}
