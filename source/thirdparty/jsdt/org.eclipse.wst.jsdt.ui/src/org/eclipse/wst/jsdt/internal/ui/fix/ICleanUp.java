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
import org.eclipse.wst.jsdt.internal.corext.fix.IFix;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;

/**
 * A clean up can solve several different problems in a given
 * <code>JavaScriptUnit</code>. The <code>JavaScriptUnit</code> is
 * compiled by using the compiler options returned by
 * <code>getRequiredOptions</code>.
 * 
 * 
 */
public interface ICleanUp {
	
	/**
	 * Does this clean up require an AST for the given <code>unit</code>. If
	 * true is returned an AST for unit is created by the clean up
	 * infrastructure and {@link #createFix(JavaScriptUnit)} is executed,
	 * otherwise {@link #createFix(IJavaScriptUnit)} is executed. The source
	 * from which the AST is created may be differ from the source of
	 * <code>unit</code>.
	 * <p>
	 * Implementors should return false whenever possible because creating an
	 * AST is expensive.
	 * 
	 * @param unit
	 *            the unit to create an ast for
	 * @return true if {@link #createFix(JavaScriptUnit)} must be executed,
	 *         false if {@link #createFix(IJavaScriptUnit)} must be executed
	 */
	public abstract boolean requireAST(IJavaScriptUnit unit) throws CoreException;
	
	/**
	 * Create an <code>IFix</code> which fixes all problems in
	 * <code>unit</code> or <code>null</code> if nothing to fix.
	 * <p>
	 * This is called iff {@link #requireAST(IJavaScriptUnit)} returns
	 * <code>false</code>.
	 * 
	 * @param unit
	 *            the IJavaScriptUnit to fix, not null
	 * @return the fix for the problems or <code>null</code> if nothing to fix
	 */
	public abstract IFix createFix(IJavaScriptUnit unit) throws CoreException;
	
	/**
	 * Create an <code>IFix</code> which fixes all problems in
	 * <code>compilationUnit</code> or <code>null</code> if nothing to fix.
	 * <p>
	 * This is called iff {@link #requireAST(IJavaScriptUnit)} returns
	 * <code>true</code>.
	 * 
	 * @param compilationUnit
	 *            The compilation unit to fix, may be null
	 * @return The fix or null if no fixes possible
	 * @throws CoreException
	 */
	public abstract IFix createFix(JavaScriptUnit compilationUnit) throws CoreException;
	
	/**
	 * Create a <code>IFix</code> which fixes all <code>problems</code> in
	 * <code>JavaScriptUnit</code>
	 * 
	 * @param compilationUnit
	 *            The compilation unit to fix, may be null
	 * @param problems
	 *            The locations of the problems to fix
	 * @return The fix or null if no fixes possible
	 * @throws CoreException
	 */
	public abstract IFix createFix(JavaScriptUnit compilationUnit, IProblemLocation[] problems) throws CoreException;
	
	/**
	 * Required compiler options to allow <code>createFix</code> to work
	 * correct.
	 * 
	 * @return The options as map or null
	 */
	public abstract Map getRequiredOptions();
	
	/**
	 * If true a fresh AST, containing all the changes from previous clean ups,
	 * will be created and passed to createFix.
	 * 
	 * @param compilationUnit
	 *            The current available AST
	 * @return true if the caller needs an up to date AST
	 */
	public abstract boolean needsFreshAST(JavaScriptUnit compilationUnit);
	
	/**
	 * Description for each operation this clean up will execute
	 * 
	 * @return descriptions or null
	 */
	public String[] getDescriptions();
	
	public void initialize(Map settings) throws CoreException;
	
	/**
	 * After call to checkPreConditions clients will start creating fixes for
	 * <code>compilationUnits</code> int <code>project</code> unless the
	 * result of checkPreConditions contains a fatal error
	 * 
	 * @param project
	 *            The project to clean up
	 * @param compilationUnits
	 *            The compilation Units to clean up, all member of project
	 * @param monitor
	 *            the monitor to show progress
	 * @return the result of the precondition check, not null
	 */
	public abstract RefactoringStatus checkPreConditions(IJavaScriptProject project, IJavaScriptUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException;
	
	/**
	 * Called when done cleaning up.
	 * 
	 * @param monitor
	 *            the monitor to show progress
	 * @return the result of the postcondition check, not null
	 */
	public abstract RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException;
	
	/**
	 * True if <code>problem</code> in <code>JavaScriptUnit</code> can be
	 * fixed by this CleanUp. If true
	 * <code>createFix(compilationUnit, new IProblemLocation[] {problem})</code>
	 * does not return null.
	 * 
	 * @param compilationUnit
	 *            The compilation unit to fix not null
	 * @param problem
	 *            The location of the problem to fix
	 * @return True if problem can be fixed
	 * @throws CoreException
	 */
	public boolean canFix(JavaScriptUnit compilationUnit, IProblemLocation problem) throws CoreException;
	
	/**
	 * Maximal number of problems this clean up will fix in compilation unit.
	 * There may be less then the returned number but never more.
	 * 
	 * @param compilationUnit
	 *            The compilation unit to fix, not null
	 * @return The maximal number of fixes or -1 if unknown.
	 */
	public abstract int maximalNumberOfFixes(JavaScriptUnit compilationUnit);
	
	/**
	 * A code snippet which complies to the current settings.
	 * 
	 * @return A code snippet, not null.
	 */
	public abstract String getPreview();
	
}
