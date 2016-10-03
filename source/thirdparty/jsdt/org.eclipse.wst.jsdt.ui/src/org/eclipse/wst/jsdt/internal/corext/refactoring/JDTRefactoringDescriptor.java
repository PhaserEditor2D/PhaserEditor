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
package org.eclipse.wst.jsdt.internal.corext.refactoring;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

/**
 * Descriptor object of a JDT refactoring.
 * 
 * 
 */
public class JDTRefactoringDescriptor extends JavaScriptRefactoringDescriptor {

	/**
	 * Predefined argument called <code>element&lt;Number&gt;</code>.
	 * <p>
	 * This argument should be used to describe the elements being refactored.
	 * The value of this argument does not necessarily have to uniquely identify
	 * the elements. However, it must be possible to uniquely identify the
	 * elements using the value of this argument in conjunction with the values
	 * of the other user-defined attributes.
	 * </p>
	 * <p>
	 * The element arguments are simply distinguished by appending a number to
	 * the argument name, e.g. element1. The indices of this argument are non
	 * zero-based.
	 * </p>
	 */
	public static final String ATTRIBUTE_ELEMENT= "element"; //$NON-NLS-1$

	/**
	 * Predefined argument called <code>input</code>.
	 * <p>
	 * This argument should be used to describe the element being refactored.
	 * The value of this argument does not necessarily have to uniquely identify
	 * the input element. However, it must be possible to uniquely identify the
	 * input element using the value of this argument in conjunction with the
	 * values of the other user-defined attributes.
	 * </p>
	 */
	public static final String ATTRIBUTE_INPUT= "input"; //$NON-NLS-1$

	/**
	 * Predefined argument called <code>name</code>.
	 * <p>
	 * This argument should be used to name the element being refactored. The
	 * value of this argument may be shown in the user interface.
	 * </p>
	 */
	public static final String ATTRIBUTE_NAME= "name"; //$NON-NLS-1$

	/**
	 * Predefined argument called <code>references</code>.
	 * <p>
	 * This argument should be used to describe whether references to the
	 * elements being refactored should be updated as well.
	 * </p>
	 */
	public static final String ATTRIBUTE_REFERENCES= "references"; //$NON-NLS-1$

	/**
	 * Predefined argument called <code>selection</code>.
	 * <p>
	 * This argument should be used to describe user input selections within a
	 * text file. The value of this argument has the format "offset length".
	 * </p>
	 */
	public static final String ATTRIBUTE_SELECTION= "selection"; //$NON-NLS-1$

	/**
	 * Constant describing the deprecation resolving flag.
	 * <p>
	 * Clients should set this flag to indicate that the refactoring can used to
	 * resolve deprecation problems of members. Refactorings which can run on
	 * binary targets, but require a source attachment to work correctly, should
	 * set the <code>JAR_SOURCE_ATTACHMENT</code> flag as well.
	 * </p>
	 */
	public static final int DEPRECATION_RESOLVING= 1 << 17;

	/**
	 * Converts the specified element to an input handle.
	 * 
	 * @param project
	 *            the project, or <code>null</code> for the workspace
	 * @param element
	 *            the element
	 * @return a corresponding input handle
	 */
	public static String elementToHandle(final String project, final IJavaScriptElement element) {
		final String handle= element.getHandleIdentifier();
		if (project != null && !(element instanceof IJavaScriptProject)) {
			final String id= element.getJavaScriptProject().getHandleIdentifier();
			return handle.substring(id.length());
		}
		return handle;
	}

	/**
	 * Converts an input handle back to the corresponding java element.
	 * 
	 * @param project
	 *            the project, or <code>null</code> for the workspace
	 * @param handle
	 *            the input handle
	 * @return the corresponding java element, or <code>null</code> if no such
	 *         element exists
	 */
	public static IJavaScriptElement handleToElement(final String project, final String handle) {
		return handleToElement(project, handle, true);
	}

	/**
	 * Converts an input handle back to the corresponding java element.
	 * 
	 * @param project
	 *            the project, or <code>null</code> for the workspace
	 * @param handle
	 *            the input handle
	 * @param check
	 *            <code>true</code> to check for existence of the element,
	 *            <code>false</code> otherwise
	 * @return the corresponding java element, or <code>null</code> if no such
	 *         element exists
	 */
	public static IJavaScriptElement handleToElement(final String project, final String handle, final boolean check) {
		return handleToElement(null, project, handle, check);
	}

	/**
	 * Converts an input handle back to the corresponding java element.
	 * 
	 * @param owner
	 *            the working copy owner
	 * @param project
	 *            the project, or <code>null</code> for the workspace
	 * @param handle
	 *            the input handle
	 * @param check
	 *            <code>true</code> to check for existence of the element,
	 *            <code>false</code> otherwise
	 * @return the corresponding java element, or <code>null</code> if no such
	 *         element exists
	 */
	public static IJavaScriptElement handleToElement(final WorkingCopyOwner owner, final String project, final String handle, final boolean check) {
		IJavaScriptElement element= null;
		if (owner != null)
			element= JavaScriptCore.create(handle, owner);
		else
			element= JavaScriptCore.create(handle);
		if (element == null && project != null) {
			final IJavaScriptProject javaProject= JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaScriptProject(project);
			final String identifier= javaProject.getHandleIdentifier();
			if (owner != null)
				element= JavaScriptCore.create(identifier + handle, owner);
			else
				element= JavaScriptCore.create(identifier + handle);
		}
		if (check && element instanceof IFunction) {
			final IFunction method= (IFunction) element;
			 IFunction[] methods=null;
			if (method.getDeclaringType()!=null)
				methods=method.getDeclaringType().findMethods(method);
			else
				methods=method.getJavaScriptUnit().findFunctions(method);
			if (methods != null && methods.length > 0)
				element= methods[0];
		}
		if (element != null && (!check || element.exists()))
			return element;
		return null;
	}

	/**
	 * Converts an input handle with the given prefix back to the corresponding
	 * resource.
	 * 
	 * @param project
	 *            the project, or <code>null</code> for the workspace
	 * @param handle
	 *            the input handle
	 * 
	 * @return the corresponding resource, or <code>null</code> if no such
	 *         resource exists
	 */
	public static IResource handleToResource(final String project, final String handle) {
		final IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		if ("".equals(handle)) //$NON-NLS-1$
			return null;
		final IPath path= Path.fromPortableString(handle);
		if (path == null)
			return null;
		if (project != null && !"".equals(project)) //$NON-NLS-1$
			return root.getProject(project).findMember(path);
		return root.findMember(path);
	}

	/**
	 * Converts the specified resource to an input handle.
	 * 
	 * @param project
	 *            the project, or <code>null</code> for the workspace
	 * @param resource
	 *            the resource
	 * 
	 * @return the input handle
	 */
	public static String resourceToHandle(final String project, final IResource resource) {
		if (project != null && !"".equals(project)) //$NON-NLS-1$
			return resource.getProjectRelativePath().toPortableString();
		return resource.getFullPath().toPortableString();
	}

	/**
	 * Creates a new JDT refactoring descriptor.
	 * 
	 * @param id
	 *            the unique id of the refactoring
	 * @param project
	 *            the project name, or <code>null</code>
	 * @param description
	 *            the description
	 * @param comment
	 *            the comment, or <code>null</code>
	 * @param arguments
	 *            the argument map
	 * @param flags
	 *            the flags
	 */
	public JDTRefactoringDescriptor(final String id, final String project, final String description, final String comment, final Map arguments, final int flags) {
		super(id, arguments);
		setProject(project);
		setDescription(description);
		setComment(comment);
		setFlags(flags);
	}

	/**
	 * Creates refactoring arguments for this refactoring descriptor.
	 * 
	 * @return the refactoring arguments
	 */
	public JavaRefactoringArguments createArguments() {
		final JavaRefactoringArguments arguments= new JavaRefactoringArguments(getProject());
		for (final Iterator iterator= getArguments().entrySet().iterator(); iterator.hasNext();) {
			final Map.Entry entry= (Entry) iterator.next();
			final String name= (String) entry.getKey();
			final String value= (String) entry.getValue();
			if (name != null && !"".equals(name) && value != null) //$NON-NLS-1$
				arguments.setAttribute(name, value);
		}
		return arguments;
	}

	/**
	 * {@inheritDoc}
	 */
	public Refactoring createRefactoring(final RefactoringStatus status) throws CoreException {
		Refactoring refactoring= null;
		final RefactoringContribution contribution= RefactoringCore.getRefactoringContribution(getID());
		if (contribution instanceof JDTRefactoringContribution) {
			final JDTRefactoringContribution extended= (JDTRefactoringContribution) contribution;
			refactoring= extended.createRefactoring(this);
		}
		if (refactoring != null) {
			if (refactoring instanceof IScriptableRefactoring) {
				final JavaRefactoringArguments arguments= createArguments();
				if (arguments != null)
					status.merge(((IScriptableRefactoring) refactoring).initialize(arguments));
				else
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments));
			} else
				status.merge(RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_initialization_error, getID())));
		}
		return refactoring;
	}

	/**
	 * Converts the specified element to an input handle.
	 * 
	 * @param element
	 *            the element
	 * @return a corresponding input handle
	 */
	public String elementToHandle(final IJavaScriptElement element) {
		Assert.isNotNull(element);
		return elementToHandle(getProject(), element);
	}

	/**
	 * {@inheritDoc}
	 */
	public Map getArguments() {
		return super.getArguments();
	}

	/**
	 * Converts the specified resource to an input handle.
	 * 
	 * @param resource
	 *            the resource
	 * @return a corresponding input handle
	 */
	public String resourceToHandle(final IResource resource) {
		Assert.isNotNull(resource);
		return resourceToHandle(getProject(), resource);
	}
}
