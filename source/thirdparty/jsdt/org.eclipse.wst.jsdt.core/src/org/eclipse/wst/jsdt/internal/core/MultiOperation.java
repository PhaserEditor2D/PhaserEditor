/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

/**
 * This class is used to perform operations on multiple <code>IJavaScriptElement</code>.
 * It is responible for running each operation in turn, collecting
 * the errors and merging the corresponding <code>JavaElementDelta</code>s.
 * <p>
 * If several errors occured, they are collected in a multi-status
 * <code>JavaModelStatus</code>. Otherwise, a simple <code>JavaModelStatus</code>
 * is thrown.
 */
public abstract class MultiOperation extends JavaModelOperation {
	/**
	 * Table specifying insertion positions for elements being
	 * copied/moved/renamed. Keyed by elements being processed, and
	 * values are the corresponding insertion point.
	 * @see #processElements()
	 */
	protected Map insertBeforeElements = new HashMap(1);
	/**
	 * Table specifying the new parent for elements being
	 * copied/moved/renamed.
	 * Keyed by elements being processed, and
	 * values are the corresponding destination parent.
	 */
	protected Map newParents;
	/**
	 * This table presents the data in <code>fRenamingList</code> in a more
	 * convenient way.
	 */
	protected Map renamings;
	/**
	 * The list of renamings supplied to the operation
	 */
	protected String[] renamingsList = null;
	/**
	 * Creates a new <code>MultiOperation</code> on <code>elementsToProcess</code>.
	 */
	protected MultiOperation(IJavaScriptElement[] elementsToProcess, boolean force) {
		super(elementsToProcess, force);
	}
	/**
	 * Creates a new <code>MultiOperation</code>.
	 */
	protected MultiOperation(IJavaScriptElement[] elementsToProcess, IJavaScriptElement[] parentElements, boolean force) {
		super(elementsToProcess, parentElements, force);
		this.newParents = new HashMap(elementsToProcess.length);
		if (elementsToProcess.length == parentElements.length) {
			for (int i = 0; i < elementsToProcess.length; i++) {
				this.newParents.put(elementsToProcess[i], parentElements[i]);
			}
		} else { //same destination for all elements to be moved/copied/renamed
			for (int i = 0; i < elementsToProcess.length; i++) {
				this.newParents.put(elementsToProcess[i], parentElements[0]);
			}
		}

	}
	/**
	 * Convenience method to create a <code>JavaScriptModelException</code>
	 * embending a <code>JavaModelStatus</code>.
	 */
	protected void error(int code, IJavaScriptElement element) throws JavaScriptModelException {
		throw new JavaScriptModelException(new JavaModelStatus(code, element));
	}
	/**
	 * Executes the operation.
	 *
	 * @exception JavaScriptModelException if one or several errors occured during the operation.
	 * If multiple errors occured, the corresponding <code>JavaModelStatus</code> is a
	 * multi-status. Otherwise, it is a simple one.
	 */
	protected void executeOperation() throws JavaScriptModelException {
		processElements();
	}
	/**
	 * Returns the parent of the element being copied/moved/renamed.
	 */
	protected IJavaScriptElement getDestinationParent(IJavaScriptElement child) {
		return (IJavaScriptElement)this.newParents.get(child);
	}
	/**
	 * Returns the name to be used by the progress monitor.
	 */
	protected abstract String getMainTaskName();
	/**
	 * Returns the new name for <code>element</code>, or <code>null</code>
	 * if there are no renamings specified.
	 */
	protected String getNewNameFor(IJavaScriptElement element) throws JavaScriptModelException {
		String newName = null;
		if (this.renamings != null)
			newName = (String) this.renamings.get(element);
		if (newName == null && element instanceof IFunction && ((IFunction) element).isConstructor())
			newName = getDestinationParent(element).getElementName();
		return newName;
	}
	/**
	 * Sets up the renamings hashtable - keys are the elements and
	 * values are the new name.
	 */
	private void initializeRenamings() {
		if (this.renamingsList != null && this.renamingsList.length == this.elementsToProcess.length) {
			this.renamings = new HashMap(this.renamingsList.length);
			for (int i = 0; i < this.renamingsList.length; i++) {
				if (this.renamingsList[i] != null) {
					this.renamings.put(this.elementsToProcess[i], this.renamingsList[i]);
				}
			}
		}
	}
	/**
	 * Returns <code>true</code> if this operation represents a move or rename, <code>false</code>
	 * if this operation represents a copy.<br>
	 * Note: a rename is just a move within the same parent with a name change.
	 */
	protected boolean isMove() {
		return false;
	}
	/**
	 * Returns <code>true</code> if this operation represents a rename, <code>false</code>
	 * if this operation represents a copy or move.
	 */
	protected boolean isRename() {
		return false;
	}

	/**
	 * Subclasses must implement this method to process a given <code>IJavaScriptElement</code>.
	 */
	protected abstract void processElement(IJavaScriptElement element) throws JavaScriptModelException;
	/**
	 * Processes all the <code>IJavaScriptElement</code>s in turn, collecting errors
	 * and updating the progress monitor.
	 *
	 * @exception JavaScriptModelException if one or several operation(s) was unable to
	 * be completed.
	 */
	protected void processElements() throws JavaScriptModelException {
		try {
			beginTask(getMainTaskName(), this.elementsToProcess.length);
			IJavaScriptModelStatus[] errors = new IJavaScriptModelStatus[3];
			int errorsCounter = 0;
			for (int i = 0; i < this.elementsToProcess.length; i++) {
				try {
					verify(this.elementsToProcess[i]);
					processElement(this.elementsToProcess[i]);
				} catch (JavaScriptModelException jme) {
					if (errorsCounter == errors.length) {
						// resize
						System.arraycopy(errors, 0, (errors = new IJavaScriptModelStatus[errorsCounter*2]), 0, errorsCounter);
					}
					errors[errorsCounter++] = jme.getJavaScriptModelStatus();
				} finally {
					worked(1);
				}
			}
			if (errorsCounter == 1) {
				throw new JavaScriptModelException(errors[0]);
			} else if (errorsCounter > 1) {
				if (errorsCounter != errors.length) {
					// resize
					System.arraycopy(errors, 0, (errors = new IJavaScriptModelStatus[errorsCounter]), 0, errorsCounter);
				}
				throw new JavaScriptModelException(JavaModelStatus.newMultiStatus(errors));
			}
		} finally {
			done();
		}
	}
	/**
	 * Sets the insertion position in the new container for the modified element. The element
	 * being modified will be inserted before the specified new sibling. The given sibling
	 * must be a child of the destination container specified for the modified element.
	 * The default is <code>null</code>, which indicates that the element is to be
	 * inserted at the end of the container.
	 */
	public void setInsertBefore(IJavaScriptElement modifiedElement, IJavaScriptElement newSibling) {
		this.insertBeforeElements.put(modifiedElement, newSibling);
	}
	/**
	 * Sets the new names to use for each element being copied. The renamings
	 * correspond to the elements being processed, and the number of
	 * renamings must match the number of elements being processed.
	 * A <code>null</code> entry in the list indicates that an element
	 * is not to be renamed.
	 *
	 * <p>Note that some renamings may not be used.  If both a parent
	 * and a child have been selected for copy/move, only the parent
	 * is changed.  Therefore, if a new name is specified for the child,
	 * the child's name will not be changed.
	 */
	public void setRenamings(String[] renamingsList) {
		this.renamingsList = renamingsList;
		initializeRenamings();
	}
	/**
	 * This method is called for each <code>IJavaScriptElement</code> before
	 * <code>processElement</code>. It should check that this <code>element</code>
	 * can be processed.
	 */
	protected abstract void verify(IJavaScriptElement element) throws JavaScriptModelException;
	/**
	 * Verifies that the <code>destination</code> specified for the <code>element</code> is valid for the types of the
	 * <code>element</code> and <code>destination</code>.
	 */
	protected void verifyDestination(IJavaScriptElement element, IJavaScriptElement destination) throws JavaScriptModelException {
		if (destination == null || !destination.exists())
			error(IJavaScriptModelStatusConstants.ELEMENT_DOES_NOT_EXIST, destination);

		int destType = destination.getElementType();
		switch (element.getElementType()) {
			case IJavaScriptElement.IMPORT_DECLARATION :
				if (destType != IJavaScriptElement.JAVASCRIPT_UNIT)
					error(IJavaScriptModelStatusConstants.INVALID_DESTINATION, element);
				break;
			case IJavaScriptElement.TYPE :
				if (destType != IJavaScriptElement.JAVASCRIPT_UNIT && destType != IJavaScriptElement.TYPE)
					error(IJavaScriptModelStatusConstants.INVALID_DESTINATION, element);
				break;
			case IJavaScriptElement.METHOD :
			case IJavaScriptElement.FIELD :
			case IJavaScriptElement.INITIALIZER :
				if (!(destType == IJavaScriptElement.TYPE ||destType == IJavaScriptElement.JAVASCRIPT_UNIT) || destination instanceof BinaryType)
					error(IJavaScriptModelStatusConstants.INVALID_DESTINATION, element);
				break;
			case IJavaScriptElement.JAVASCRIPT_UNIT :
				if (destType != IJavaScriptElement.PACKAGE_FRAGMENT)
					error(IJavaScriptModelStatusConstants.INVALID_DESTINATION, element);
				else {
					CompilationUnit cu = (CompilationUnit)element;
					if (isMove() && cu.isWorkingCopy() && !cu.isPrimary())
						error(IJavaScriptModelStatusConstants.INVALID_ELEMENT_TYPES, element);
				}
				break;
			case IJavaScriptElement.PACKAGE_FRAGMENT :
				IPackageFragment fragment = (IPackageFragment) element;
				IJavaScriptElement parent = fragment.getParent();
				if (parent.isReadOnly())
					error(IJavaScriptModelStatusConstants.READ_ONLY, element);
				else if (destType != IJavaScriptElement.PACKAGE_FRAGMENT_ROOT)
					error(IJavaScriptModelStatusConstants.INVALID_DESTINATION, element);
				break;
			default :
				error(IJavaScriptModelStatusConstants.INVALID_ELEMENT_TYPES, element);
		}
	}
	/**
	 * Verify that the new name specified for <code>element</code> is
	 * valid for that type of Java element.
	 */
	protected void verifyRenaming(IJavaScriptElement element) throws JavaScriptModelException {
		String newName = getNewNameFor(element);
		boolean isValid = true;
	    IJavaScriptProject project = element.getJavaScriptProject();
	    String sourceLevel = project.getOption(JavaScriptCore.COMPILER_SOURCE, true);
	    String complianceLevel = project.getOption(JavaScriptCore.COMPILER_COMPLIANCE, true);
		switch (element.getElementType()) {
			case IJavaScriptElement.PACKAGE_FRAGMENT :
				if (((IPackageFragment) element).isDefaultPackage()) {
					// don't allow renaming of default package (see PR #1G47GUM)
					throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.NAME_COLLISION, element));
				}
				isValid = JavaScriptConventions.validatePackageName(newName, sourceLevel, complianceLevel).getSeverity() != IStatus.ERROR;
				break;
			case IJavaScriptElement.JAVASCRIPT_UNIT :
				isValid = JavaScriptConventions.validateCompilationUnitName(newName,sourceLevel, complianceLevel).getSeverity() != IStatus.ERROR;
				break;
			case IJavaScriptElement.INITIALIZER :
				isValid = false; //cannot rename initializers
				break;
			default :
				isValid = JavaScriptConventions.validateIdentifier(newName, sourceLevel, complianceLevel).getSeverity() != IStatus.ERROR;
				break;
		}

		if (!isValid) {
			throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_NAME, element, newName));
		}
	}
	/**
	 * Verifies that the positioning sibling specified for the <code>element</code> is exists and
	 * its parent is the destination container of this <code>element</code>.
	 */
	protected void verifySibling(IJavaScriptElement element, IJavaScriptElement destination) throws JavaScriptModelException {
		IJavaScriptElement insertBeforeElement = (IJavaScriptElement) this.insertBeforeElements.get(element);
		if (insertBeforeElement != null) {
			if (!insertBeforeElement.exists() || !insertBeforeElement.getParent().equals(destination)) {
				error(IJavaScriptModelStatusConstants.INVALID_SIBLING, insertBeforeElement);
			}
		}
	}
}
