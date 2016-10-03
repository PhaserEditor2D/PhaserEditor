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
package org.eclipse.wst.jsdt.internal.corext.refactoring.nls;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStringStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;

public class NLSRefactoring extends Refactoring {

	public static final String BUNDLE_NAME= "BUNDLE_NAME"; //$NON-NLS-1$
	public static final String PROPERTY_FILE_EXT= ".properties"; //$NON-NLS-1$
	public static final String DEFAULT_ACCESSOR_CLASSNAME= "Messages"; //$NON-NLS-1$
	
	public static final String KEY= "${key}"; //$NON-NLS-1$
	public static final String DEFAULT_SUBST_PATTERN= "getString(" + KEY + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	
	public static final String DEFAULT_PROPERTY_FILENAME= "messages"; //$NON-NLS-1$

	//private IPath fPropertyFilePath;

	private String fAccessorClassName;
	private IPackageFragment fAccessorClassPackage;
	private String fResourceBundleName;
	private IPackageFragment fResourceBundlePackage;

	private String fSubstitutionPattern;
	private IJavaScriptUnit fCu;
	private NLSSubstitution[] fSubstitutions;
	
	private String fPrefix;
	
	/**
	 * <code>true</code> if the standard resource bundle mechanism
	 * is used and <code>false</code> NLSing is done the Eclipse way. 
	 */
	private boolean fIsEclipseNLS;

	private NLSRefactoring(IJavaScriptUnit cu) {
		Assert.isNotNull(cu);
		fCu= cu;

		JavaScriptUnit astRoot= JavaScriptPlugin.getDefault().getASTProvider().getAST(fCu, ASTProvider.WAIT_YES, null);
		NLSHint nlsHint= new NLSHint(fCu, astRoot);

		fSubstitutions= nlsHint.getSubstitutions();
		setAccessorClassName(nlsHint.getAccessorClassName());
		setAccessorClassPackage(nlsHint.getAccessorClassPackage());
		setIsEclipseNLS(detectIsEclipseNLS());
		setResourceBundleName(nlsHint.getResourceBundleName());
		setResourceBundlePackage(nlsHint.getResourceBundlePackage());
		setSubstitutionPattern(DEFAULT_SUBST_PATTERN);
		
		String cuName= fCu.getElementName();
		if (fIsEclipseNLS)
			setPrefix(cuName.substring(0, cuName.length() - 5) + "_"); // A.java -> A_ //$NON-NLS-1$
		else
			setPrefix(cuName.substring(0, cuName.length() - 4)); // A.java -> A.
	}

	public static NLSRefactoring create(IJavaScriptUnit cu) {
		if (cu == null || !cu.exists())
			return null;
		return new NLSRefactoring(cu);
	}

	/**
	 * no validation is done
	 * 
	 * @param pattern
	 *            Example: "Messages.getString(${key})". Must not be
	 *            <code>null</code>. should (but does not have to) contain
	 *            NLSRefactoring.KEY (default value is $key$) only the first
	 *            occurrence of this key will be used
	 */
	public void setSubstitutionPattern(String pattern) {
		Assert.isNotNull(pattern);
		fSubstitutionPattern= pattern;
	}

	/**
	 * to show the pattern in the UI
	 */
	public String getSubstitutionPattern() {
		if (fIsEclipseNLS)
			return KEY;
		else
			return fSubstitutionPattern;
	}

	public IJavaScriptUnit getCu() {
		return fCu;
	}

	public String getName() {
		return Messages.format(NLSMessages.NLSRefactoring_compilation_unit, fCu.getElementName());
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {

		if (fSubstitutions.length == 0) {
			String message= Messages.format(NLSMessages.NLSRefactoring_no_strings, fCu.getElementName());
			return RefactoringStatus.createFatalErrorStatus(message);
		}
		return new RefactoringStatus();
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		checkParameters();
		try {

			pm.beginTask(NLSMessages.NLSRefactoring_checking, 5); 

			RefactoringStatus result= new RefactoringStatus();

			result.merge(checkIfAnythingToDo());
			if (result.hasFatalError()) {
				return result;
			}
			pm.worked(1);

			result.merge(validateModifiesFiles());
			if (result.hasFatalError()) {
				return result;
			}
			pm.worked(1);
			if (pm.isCanceled())
				throw new OperationCanceledException();

			result.merge(checkSubstitutionPattern());
			pm.worked(1);

			if (pm.isCanceled())
				throw new OperationCanceledException();


			result.merge(checkKeys());
			pm.worked(1);
			if (pm.isCanceled())
				throw new OperationCanceledException();

			if (!propertyFileExists() && willModifyPropertyFile()) {
				String msg= Messages.format(NLSMessages.NLSRefactoring_will_be_created, getPropertyFilePath().toString()); 
				result.addInfo(msg);
			}
			pm.worked(1);

			return result;
		} finally {
			pm.done();
		}
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			checkParameters();

			pm.beginTask("", 3); //$NON-NLS-1$

			final DynamicValidationStateChange result= new DynamicValidationStateChange(NLSMessages.NLSRefactoring_change_name);

			boolean createAccessorClass= willCreateAccessorClass();
			if (NLSSubstitution.countItems(fSubstitutions, NLSSubstitution.EXTERNALIZED) == 0) {
				createAccessorClass= false;
			}
			if (createAccessorClass) {
				result.add(AccessorClassCreator.create(fCu, fAccessorClassName, getAccessorCUPath(), fAccessorClassPackage, getPropertyFilePath(), fIsEclipseNLS, fSubstitutions, getSubstitutionPattern(), new SubProgressMonitor(pm, 1)));
			}
			pm.worked(1);

			if (willModifySource()) {
				result.add(NLSSourceModifier.create(getCu(), fSubstitutions, getSubstitutionPattern(), fAccessorClassPackage, fAccessorClassName, fIsEclipseNLS));
			}
			pm.worked(1);

			if (willModifyPropertyFile()) {
				result.add(NLSPropertyFileModifier.create(fSubstitutions, getPropertyFilePath()));
				if (isEclipseNLS() && !createAccessorClass) {
					Change change= AccessorClassModifier.create(getAccessorCu(), fSubstitutions);
					if (change != null)
						result.add(change);
				}
			}
			pm.worked(1);

			return result;
		} finally {
			pm.done();
		}
	}

	private void checkParameters() {
		Assert.isNotNull(fSubstitutions);
		Assert.isNotNull(fAccessorClassPackage);

		// these values have defaults ...
		Assert.isNotNull(fAccessorClassName);
		Assert.isNotNull(getSubstitutionPattern());
	}

	private IFile[] getAllFilesToModify() {

		List files= new ArrayList(2);
		if (willModifySource()) {
			IResource resource= fCu.getResource();
			if (resource.exists()) {
				files.add(resource);
			}
		}

		if (willModifyPropertyFile()) {
			IFile file= getPropertyFileHandle();
			if (file.exists()) {
				files.add(file);
			}
		}
		
		if (willModifyAccessorClass()) {
			IFile file= getAccessorClassFileHandle();
			if (file.exists()) {
				files.add(file);
			}
		}
		
		return (IFile[]) files.toArray(new IFile[files.size()]);
	}

	public IFile getPropertyFileHandle() {
		return ResourcesPlugin.getWorkspace().getRoot().getFile(getPropertyFilePath());
	}

	public IPath getPropertyFilePath() {
		return fResourceBundlePackage.getPath().append(fResourceBundleName);
	}
	
	public IFile getAccessorClassFileHandle() {
		return ResourcesPlugin.getWorkspace().getRoot().getFile(getAccessorClassFilePath());
	}
	
	public IPath getAccessorClassFilePath() {
		return getAccessorCUPath();
	}

	private RefactoringStatus validateModifiesFiles() {
		return Checks.validateModifiesFiles(getAllFilesToModify(), getValidationContext());
	}

	//should stop checking if fatal error
	private RefactoringStatus checkIfAnythingToDo() throws JavaScriptModelException {
		if (NLSSubstitution.countItems(fSubstitutions, NLSSubstitution.EXTERNALIZED) != 0 && willCreateAccessorClass())
			return null;

		if (willModifyPropertyFile())
			return null;

		if (willModifySource())
			return null;

		RefactoringStatus result= new RefactoringStatus();
		result.addFatalError(NLSMessages.NLSRefactoring_nothing_to_do); 
		return result;
	}

	private boolean propertyFileExists() {
		return getPropertyFileHandle().exists();
	}

	private RefactoringStatus checkSubstitutionPattern() {
		String pattern= getSubstitutionPattern();

		RefactoringStatus result= new RefactoringStatus();
		if (pattern.trim().length() == 0) {// 
			result.addError(NLSMessages.NLSRefactoring_pattern_empty); 
		}

		if (pattern.indexOf(KEY) == -1) {
			String msg= Messages.format(NLSMessages.NLSRefactoring_pattern_does_not_contain, KEY); 
			result.addWarning(msg);
		}

		if (pattern.indexOf(KEY) != pattern.lastIndexOf(KEY)) {
			String msg= Messages.format(NLSMessages.NLSRefactoring_Only_the_first_occurrence_of, KEY);
			result.addWarning(msg);
		}

		return result;
	}

	private RefactoringStatus checkKeys() {
		RefactoringStatus result= new RefactoringStatus();
		NLSSubstitution[] subs= fSubstitutions;
		for (int i= 0; i < subs.length; i++) {
			NLSSubstitution substitution= subs[i];
			if ((substitution.getState() == NLSSubstitution.EXTERNALIZED) && substitution.hasStateChanged()) {
				result.merge(checkKey(substitution.getKey()));
			}
		}
		return result;
	}

	private static RefactoringStatus checkKey(String key) {
		RefactoringStatus result= new RefactoringStatus();

		if (key == null)
			result.addFatalError(NLSMessages.NLSRefactoring_null); 

		if (key.startsWith("!") || key.startsWith("#")) { //$NON-NLS-1$ //$NON-NLS-2$
			RefactoringStatusContext context= new JavaStringStatusContext(key, new SourceRange(0, 0));
			result.addWarning(NLSMessages.NLSRefactoring_warning, context); 
		}

		if ("".equals(key.trim())) //$NON-NLS-1$
			result.addFatalError(NLSMessages.NLSRefactoring_empty); 

		final String[] UNWANTED_STRINGS= {" ", ":", "\"", "\\", "'", "?", "="}; //$NON-NLS-7$ //$NON-NLS-6$ //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		//feature in resource bundle - does not work properly if keys have ":"
		for (int i= 0; i < UNWANTED_STRINGS.length; i++) {
			if (key.indexOf(UNWANTED_STRINGS[i]) != -1) {
				String[] args= {key, UNWANTED_STRINGS[i]};
				String msg= Messages.format(NLSMessages.NLSRefactoring_should_not_contain, args); 
				result.addError(msg);
			}
		}
		return result;
	}

	public boolean willCreateAccessorClass() throws JavaScriptModelException {

		IJavaScriptUnit compilationUnit= getAccessorCu();
		if (compilationUnit.exists()) {
			return false;
		}

		if (typeNameExistsInPackage(fAccessorClassPackage, fAccessorClassName)) {
			return false;
		}

		return (!Checks.resourceExists(getAccessorCUPath()));
	}

	private IJavaScriptUnit getAccessorCu() {
		return fAccessorClassPackage.getJavaScriptUnit(getAccessorCUName());
	}

	private boolean willModifySource() {
		NLSSubstitution[] subs= fSubstitutions;
		for (int i= 0; i < subs.length; i++) {
			if (subs[i].hasSourceChange())
				return true;
		}
		return false;
	}

	private boolean willModifyPropertyFile() {
		NLSSubstitution[] subs= fSubstitutions;
		for (int i= 0; i < subs.length; i++) {
			NLSSubstitution substitution= subs[i];
			if (substitution.hasPropertyFileChange()) {
				return true;
			}
		}
		return false;
	}
	
	private boolean willModifyAccessorClass() {
		if (!isEclipseNLS())
			return false;

		NLSSubstitution[] subs= fSubstitutions;
		for (int i= 0; i < subs.length; i++) {
			NLSSubstitution substitution= subs[i];
			if (substitution.hasAccessorClassChange()) {
				return true;
			}
		}
		return false;
	}

	private boolean typeNameExistsInPackage(IPackageFragment pack, String name) throws JavaScriptModelException {
		return Checks.findTypeInPackage(pack, name) != null;
	}

	private String getAccessorCUName() {
		return getAccessorClassName() + JavaModelUtil.DEFAULT_CU_SUFFIX;
	}

	private IPath getAccessorCUPath() {
		return fAccessorClassPackage.getPath().append(getAccessorCUName());
	}

	public NLSSubstitution[] getSubstitutions() {
		return fSubstitutions;
	}

	public String getPrefix() {
		return fPrefix;
	}
	
	public void setPrefix(String prefix) {
		fPrefix= prefix;
		if (fSubstitutions != null) {
			for (int i= 0; i < fSubstitutions.length; i++)
				fSubstitutions[i].setPrefix(prefix);
		}
	}

	public void setAccessorClassName(String name) {
		Assert.isNotNull(name);
		fAccessorClassName= name;
	}

	public void setAccessorClassPackage(IPackageFragment packageFragment) {
		Assert.isNotNull(packageFragment);
		fAccessorClassPackage= packageFragment;
	}
	
	/**
	 * Sets whether the Eclipse NLSing mechanism or
	 * standard resource bundle mechanism is used.
	 * 
	 * @param isEclipseNLS	<code>true</code> if NLSing is done the Eclipse way
	 * 						and <code>false</code> if the standard resource bundle mechanism is used
	 *  
	 */
	public void setIsEclipseNLS(boolean isEclipseNLS) {
		fIsEclipseNLS= isEclipseNLS;
	}

	public void setResourceBundlePackage(IPackageFragment resourceBundlePackage) {
		Assert.isNotNull(resourceBundlePackage);
		fResourceBundlePackage= resourceBundlePackage;
	}

	public void setResourceBundleName(String resourceBundleName) {
		Assert.isNotNull(resourceBundleName);
		fResourceBundleName= resourceBundleName;
	}

	public IPackageFragment getAccessorClassPackage() {
		return fAccessorClassPackage;
	}
	
	/**
	 * Computes whether the Eclipse NLSing mechanism is used.
	 * 
	 * @return		<code>true</code> if NLSing is done the Eclipse way
	 * 				and <code>false</code> if the standard resource bundle mechanism is used
	 *  
	 */
	public boolean detectIsEclipseNLS() {
		if (getAccessorClassPackage() != null) {
			IJavaScriptUnit accessorCU= getAccessorClassPackage().getJavaScriptUnit(getAccessorCUName());
			IType type= accessorCU.getType(getAccessorClassName());
			if (type.exists()) {
				try {
					String superclassName= type.getSuperclassName();
					if (!"NLS".equals(superclassName) && !NLS.class.getName().equals(superclassName)) //$NON-NLS-1$
						return false;
					IType superclass= type.newSupertypeHierarchy(null).getSuperclass(type);
					return superclass != null && NLS.class.getName().equals(superclass.getFullyQualifiedName());
				} catch (JavaScriptModelException e) {
					return false;
				}
			}
		}
		return fIsEclipseNLS;
	}
	
	/**
	 * Returns whether the Eclipse NLSing mechanism or
	 * the standard resource bundle mechanism is used.
	 * 
	 * @return		<code>true</code> if NLSing is done the Eclipse way
	 * 				and <code>false</code> if the standard resource bundle mechanism is used
	 *  
	 */
	public boolean isEclipseNLS() {
		return fIsEclipseNLS;
	}

	public IPackageFragment getResourceBundlePackage() {
		return fResourceBundlePackage;
	}

	public String getAccessorClassName() {
		return fAccessorClassName;
	}

	public String getResourceBundleName() {
		return fResourceBundleName;
	}
}
