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

package org.eclipse.wst.jsdt.ui;

import java.util.regex.Pattern;

import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;

/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class CodeStyleConfiguration {
	
	private static final Pattern SEMICOLON_PATTERN= Pattern.compile(";"); //$NON-NLS-1$

	private CodeStyleConfiguration() {
		// do not instantiate and subclass
	}
	
	
	/**
	 * Returns a {@link ImportRewrite} using {@link ImportRewrite#create(IJavaScriptUnit, boolean)} and
	 * configures the rewriter with the settings as specified in the JDT UI preferences.
	 * <p>
	 * 
	 * @param cu the compilation unit to create the rewriter on
	 * @param restoreExistingImports specifies if the existing imports should be kept or removed.
	 * @return the new rewriter configured with the settings as specified in the JDT UI preferences.
	 * @throws JavaScriptModelException thrown when the compilation unit could not be accessed.
	 * 
	 * @see ImportRewrite#create(IJavaScriptUnit, boolean)
	 */
	public static ImportRewrite createImportRewrite(IJavaScriptUnit cu, boolean restoreExistingImports) throws JavaScriptModelException {
		return configureImportRewrite(ImportRewrite.create(cu, restoreExistingImports));
	}
	
	/**
	 * Returns a {@link ImportRewrite} using {@link ImportRewrite#create(JavaScriptUnit, boolean)} and
	 * configures the rewriter with the settings as specified in the JDT UI preferences.
	 * 
	 * @param astRoot the AST root to create the rewriter on
	 * @param restoreExistingImports specifies if the existing imports should be kept or removed.
	 * @return the new rewriter configured with the settings as specified in the JDT UI preferences.
	 * 
	 * @see ImportRewrite#create(JavaScriptUnit, boolean)
	 */
	public static ImportRewrite createImportRewrite(JavaScriptUnit astRoot, boolean restoreExistingImports) {
		return configureImportRewrite(ImportRewrite.create(astRoot, restoreExistingImports));
	}
	
	private static ImportRewrite configureImportRewrite(ImportRewrite rewrite) {
		IJavaScriptProject project= rewrite.getCompilationUnit().getJavaScriptProject();
		String order= PreferenceConstants.getPreference(PreferenceConstants.ORGIMPORTS_IMPORTORDER, project);
		rewrite.setImportOrder(SEMICOLON_PATTERN.split(order, 0));

		String thres= PreferenceConstants.getPreference(PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD, project);
		try {
			int num= Integer.parseInt(thres);
			if (num == 0)
				num= 1;
			rewrite.setOnDemandImportThreshold(num);
		} catch (NumberFormatException e) {
			// ignore
		}
		String thresStatic= PreferenceConstants.getPreference(PreferenceConstants.ORGIMPORTS_STATIC_ONDEMANDTHRESHOLD, project);
		try {
			int num= Integer.parseInt(thresStatic);
			if (num == 0)
				num= 1;
			rewrite.setStaticOnDemandImportThreshold(num);
		} catch (NumberFormatException e) {
			// ignore
		}
		return rewrite;
	}



}
