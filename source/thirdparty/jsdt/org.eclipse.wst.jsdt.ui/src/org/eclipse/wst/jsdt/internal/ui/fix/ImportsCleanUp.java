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
package org.eclipse.wst.jsdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.wst.jsdt.internal.corext.fix.IFix;
import org.eclipse.wst.jsdt.internal.corext.fix.ImportsFix;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;

public class ImportsCleanUp extends AbstractCleanUp {
	
	private CodeGenerationSettings fCodeGeneratorSettings;
	private RefactoringStatus fStatus;

	public ImportsCleanUp(Map options) {
		super(options);
    }
	
	public ImportsCleanUp() {
		super();
    }

	/**
	 * {@inheritDoc}
	 */
	public boolean requireAST(IJavaScriptUnit unit) throws CoreException {
	    return isEnabled(CleanUpConstants.ORGANIZE_IMPORTS);
	}

	/**
     * {@inheritDoc}
     */
    public IFix createFix(final JavaScriptUnit compilationUnit) throws CoreException {
		return ImportsFix.createCleanUp(compilationUnit, fCodeGeneratorSettings,
				isEnabled(CleanUpConstants.ORGANIZE_IMPORTS), fStatus);
	}

	/**
     * {@inheritDoc}
     */
    public IFix createFix(JavaScriptUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
	    return null;
    }

    /**
     * {@inheritDoc}
     */
    public RefactoringStatus checkPreConditions(IJavaScriptProject project, IJavaScriptUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		RefactoringStatus result= super.checkPreConditions(project, compilationUnits, monitor);
    	
		if (isEnabled(CleanUpConstants.ORGANIZE_IMPORTS)) {
    		fCodeGeneratorSettings= JavaPreferencesSettings.getCodeGenerationSettings(project);
    		fStatus= new RefactoringStatus();
		}
		
    	return result;
    }
    
    /**
     * {@inheritDoc}
     */
    public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
    	fCodeGeneratorSettings= null;
    	if (fStatus == null || fStatus.isOK()) {
    		return super.checkPostConditions(monitor);
    	} else {
    		return fStatus;
    	}
    }

	/**
     * {@inheritDoc}
     */
    public String[] getDescriptions() {
    	if (isEnabled(CleanUpConstants.ORGANIZE_IMPORTS))
    		return new String[] {MultiFixMessages.ImportsCleanUp_OrganizeImports_Description};
    		
	    return null;
    }

	/**
     * {@inheritDoc}
     */
    public String getPreview() {
		StringBuffer buf= new StringBuffer();
		
//		if (isEnabled(CleanUpConstants.ORGANIZE_IMPORTS)) {
//			buf.append("import org.model.Engine;\n"); //$NON-NLS-1$
//		} else {
//			buf.append("import org.model.*;\n"); //$NON-NLS-1$
//		}
		
		return buf.toString();
    }

	/**
     * {@inheritDoc}
     */
    public Map getRequiredOptions() {
	    return null;
    }

	/**
     * {@inheritDoc}
     */
    public int maximalNumberOfFixes(JavaScriptUnit compilationUnit) {
	    return -1;
    }

	/**
     * {@inheritDoc}
     */
    public boolean needsFreshAST(JavaScriptUnit compilationUnit) {
    	if (isEnabled(CleanUpConstants.ORGANIZE_IMPORTS))
	    	return true;
    	
    	return super.needsFreshAST(compilationUnit);
    }
    
	/**
     * {@inheritDoc}
     */
    public boolean canFix(JavaScriptUnit compilationUnit, IProblemLocation problem) throws CoreException {
	    return false;
    }

}
