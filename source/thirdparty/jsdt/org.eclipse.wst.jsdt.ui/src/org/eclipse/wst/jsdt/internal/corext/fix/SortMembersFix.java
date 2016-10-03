/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt - https://bugs.eclipse.org/bugs/show_bug.cgi?id=168954
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.fix;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.util.JavaScriptUnitSorter;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.SortMembersOperation.DefaultJavaElementComparator;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;

public class SortMembersFix implements IFix {
	
	public static IFix createCleanUp(JavaScriptUnit compilationUnit, boolean sortMembers, boolean sortFields) throws CoreException {
		if (!sortMembers && !sortFields)
			return null;
		
		IJavaScriptUnit cu= (IJavaScriptUnit)compilationUnit.getJavaElement();
		
		String label= FixMessages.SortMembersFix_Change_description;
		CategorizedTextEditGroup group= new CategorizedTextEditGroup(label, new GroupCategorySet(new GroupCategory(label, label, label)));
		
		TextEdit edit= JavaScriptUnitSorter.sort(compilationUnit, new DefaultJavaElementComparator(!sortFields), 0, group, null);
		if (edit == null)
			return null;
		
		TextChange change= new CompilationUnitChange(label, cu);
		change.setEdit(edit);
		change.addTextEditGroup(group);
		
		return new SortMembersFix(change, cu);
	}
	
	private final IJavaScriptUnit fCompilationUnit;
	private final TextChange fChange;
	
	public SortMembersFix(TextChange change, IJavaScriptUnit compilationUnit) {
		fChange= change;
		fCompilationUnit= compilationUnit;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public TextChange createChange() throws CoreException {
		return fChange;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IJavaScriptUnit getCompilationUnit() {
		return fCompilationUnit;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getDescription() {
		return FixMessages.SortMembersFix_Fix_description;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IStatus getStatus() {
		return StatusInfo.OK_STATUS;
	}
}
