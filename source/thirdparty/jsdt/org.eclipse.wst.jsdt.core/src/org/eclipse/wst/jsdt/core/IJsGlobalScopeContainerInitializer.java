/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 *
 */
package org.eclipse.wst.jsdt.core;

import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.compiler.libraries.LibraryLocation;

/**
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IJsGlobalScopeContainerInitializer {
	/**
	 * Binds a includepath container to a <code>IJsGlobalScopeContainer</code> for a given project,
	 * or silently fails if unable to do so.
	 * <p>
	 * A container is identified by a container path, which must be formed of two segments.
	 * The first segment is used as a unique identifier (which this initializer did register onto), and
	 * the second segment can be used as an additional hint when performing the resolution.
	 * <p>
	 * The initializer is invoked if a container path needs to be resolved for a given project, and no
	 * value for it was recorded so far. The implementation of the initializer would typically set the
	 * corresponding container using <code>JavaScriptCore#setJsGlobalScopeContainer</code>.
	 * <p>
	 * A container initialization can be indirectly performed while attempting to resolve a project
	 * includepath using <code>IJavaScriptProject#getResolvedClasspath(</code>; or directly when using
	 * <code>JavaScriptCore#getJsGlobalScopeContainer</code>. During the initialization process, any attempt
	 * to further obtain the same container will simply return <code>null</code> so as to avoid an
	 * infinite regression of initializations.
	 * <p>
	 * A container initialization may also occur indirectly when setting a project includepath, as the operation
	 * needs to resolve the includepath for validation purpose. While the operation is in progress, a referenced
	 * container initializer may be invoked. If the initializer further tries to access the referring project includepath,
	 * it will not see the new assigned includepath until the operation has completed. Note that once the JavaScript
	 * change notification occurs (at the end of the operation), the model has been updated, and the project
	 * includepath can be queried normally.
	 * <p>
	 * This method is called by the JavaScript model to give the party that defined
	 * this particular kind of includepath container the chance to install
	 * includepath container objects that will be used to convert includepath
	 * container entries into simpler includepath entries. The method is typically
	 * called exactly once for a given JavaScript project and includepath container
	 * entry. This method must not be called by other clients.
	 * <p>
	 * There are a wide variety of conditions under which this method may be
	 * invoked. To ensure that the implementation does not interfere with
	 * correct functioning of the JavaScript model, the implementation should use
	 * only the following JavaScript model APIs:
	 * <ul>
	 * <li>{@link JavaScriptCore#setJsGlobalScopeContainer(IPath, IJavaScriptProject[], IJsGlobalScopeContainer[], org.eclipse.core.runtime.IProgressMonitor)}</li>
	 * <li>{@link JavaScriptCore#getJsGlobalScopeContainer(IPath, IJavaScriptProject)}</li>
	 * <li>{@link JavaScriptCore#create(org.eclipse.core.resources.IWorkspaceRoot)}</li>
	 * <li>{@link JavaScriptCore#create(org.eclipse.core.resources.IProject)}</li>
	 * <li>{@link IJavaScriptModel#getJavaScriptProjects()}</li>
	 * <li>JavaScript element operations marked as "handle-only"</li>
	 * </ul>
	 * The effects of using other JavaScript model APIs are unspecified.
	 * </p>
	 *
	 * @param containerPath a two-segment path (ID/hint) identifying the container that needs
	 * 	to be resolved
	 * @param project the JavaScript project in which context the container is to be resolved.
	 *    This allows generic containers to be bound with project specific values.
	 * @throws CoreException if an exception occurs during the initialization
	 *
	 * @see JavaScriptCore#getJsGlobalScopeContainer(IPath, IJavaScriptProject)
	 * @see JavaScriptCore#setJsGlobalScopeContainer(IPath, IJavaScriptProject[], IJsGlobalScopeContainer[], org.eclipse.core.runtime.IProgressMonitor)
	 * @see IJsGlobalScopeContainer
	 */
	public abstract void initialize(IPath containerPath, IJavaScriptProject project) throws CoreException;

	/**
	 * Returns <code>true</code> if this container initializer can be requested to perform updates
	 * on its own container values. If so, then an update request will be performed using
	 * <code>JsGlobalScopeContainerInitializer#requestJsGlobalScopeContainerUpdate</code>/
	 * <p>
	 * @param containerPath the path of the container which requires to be updated
	 * @param project the project for which the container is to be updated
	 * @return returns <code>true</code> if the container can be updated
	 */
	public abstract boolean canUpdateJsGlobalScopeContainer(IPath containerPath, IJavaScriptProject project);

	/**
	 * Request a registered container definition to be updated according to a container suggestion. The container suggestion
	 * only acts as a place-holder to pass along the information to update the matching container definition(s) held by the
	 * container initializer. In particular, it is not expected to store the container suggestion as is, but rather adjust
	 * the actual container definition based on suggested changes.
	 * <p>
	 * IMPORTANT: In reaction to receiving an update request, a container initializer will update the corresponding
	 * container definition (after reconciling changes) at its earliest convenience, using
	 * <code>JavaScriptCore#setJsGlobalScopeContainer(IPath, IJavaScriptProject[], IJsGlobalScopeContainer[], IProgressMonitor)</code>.
	 * Until it does so, the update will not be reflected in the JavaScript Model.
	 * <p>
	 * In order to anticipate whether the container initializer allows to update its containers, the predicate
	 * <code>JavaScriptCore#canUpdateJsGlobalScopeContainer</code> should be used.
	 * <p>
	 * @param containerPath the path of the container which requires to be updated
	 * @param project the project for which the container is to be updated
	 * @param containerSuggestion a suggestion to update the corresponding container definition
	 * @throws CoreException when <code>JavaScriptCore#setJsGlobalScopeContainer</code> would throw any.
	 * @see JavaScriptCore#setJsGlobalScopeContainer(IPath, IJavaScriptProject[], IJsGlobalScopeContainer[], org.eclipse.core.runtime.IProgressMonitor)
	 * @see JsGlobalScopeContainerInitializer#canUpdateJsGlobalScopeContainer(IPath, IJavaScriptProject)
	 */
	public abstract void requestJsGlobalScopeContainerUpdate(IPath containerPath, IJavaScriptProject project, IJsGlobalScopeContainer containerSuggestion)
			throws CoreException;

	/**
	 * Returns a readable description for a container path. A readable description for a container path can be
	 * used for improving the display of references to container, without actually needing to resolve them.
	 * A good implementation should answer a description consistent with the description of the associated
	 * target container (see <code>IJsGlobalScopeContainer.getDescription()</code>).
	 *
	 * @param containerPath the path of the container which requires a readable description
	 * @param project the project from which the container is referenced
	 * @return a string description of the container
	 */
	public abstract String getDescription(IPath containerPath, IJavaScriptProject project);

	/**
	 * Returns a includepath container that is used after this initializer failed to bind a includepath container
	 * to a <code>IJsGlobalScopeContainer</code> for the given project. A non-<code>null</code>
	 * failure container indicates that there will be no more request to initialize the given container
	 * for the given project.
	 * <p>
	 * By default a non-<code>null</code> failure container with no includepath entries is returned.
	 * Clients wishing to get a chance to run the initializer again should override this method
	 * and return <code>null</code>.
	 * </p>
	 *
	 * @param containerPath the path of the container which failed to initialize
	 * @param project the project from which the container is referenced
	 * @return the default failure container, or <code>null</code> if wishing to run the initializer again
	 */
	public abstract IJsGlobalScopeContainer getFailureContainer(final IPath containerPath, IJavaScriptProject project);

	/**
	 * Returns an object which identifies a container for comparison purpose. This allows
	 * to eliminate redundant containers when accumulating includepath entries (e.g.
	 * runtime includepath computation). When requesting a container comparison ID, one
	 * should ensure using its corresponding container initializer. Indeed, a random container
	 * initializer cannot be held responsible for determining comparison IDs for arbitrary
	 * containers.
	 * <p>
	 * @param containerPath the path of the container which is being checked
	 * @param project the project for which the container is to being checked
	 * @return returns an Object identifying the container for comparison
	 */
	public abstract Object getComparisonID(IPath containerPath, IJavaScriptProject project);

	public abstract URI getHostPath(IPath path, IJavaScriptProject project);

	LibraryLocation getLibraryLocation();
	/*
	 * Returns if this library allows attachment of external JsDoc
	 */
	boolean allowAttachJsDoc();
	/**
	 * returns a String of all SuperTypes provided by this library.
	 */
	String[] containerSuperTypes();
	
	/**
	 * Get the id of the inference provider for this library
	 * @return  inference provider id
	 */
	String getInferenceID();
	
	void removeFromProject(IJavaScriptProject project);
}