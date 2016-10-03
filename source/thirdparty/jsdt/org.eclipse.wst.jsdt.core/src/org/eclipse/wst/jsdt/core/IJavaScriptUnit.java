/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     IBM Corporation - added J2SE 1.5 support
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;


/**
 * Represents an entire JavaScript file (source file with one of the
 * {@link JavaScriptCore#getJavaScriptLikeExtensions() JavaScript-like extensions}).
 * JavaScriptUnit elements need to be opened before they can be navigated or manipulated.
 * The children are of type {@link IPackageDeclaration},
 * {@link IImportContainer},{@link IFunction},{@link IField}, and {@link IType},
 * and appear in the order in which they are declared in the source.
 * If a source file cannot be parsed, its structure remains unknown.
 * Use {@link IJavaScriptElement#isStructureKnown} to determine whether this is
 * the case.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IJavaScriptUnit extends ITypeRoot, ISourceManipulation {
/**
 * Constant indicating that a reconcile operation should not return an AST.
 */
public static final int NO_AST = 0;

/**
 * Constant indicating that a reconcile operation should recompute the problems
 * even if the source hasn't changed.
 */
public static final int FORCE_PROBLEM_DETECTION = 0x01;

/**
 * Constant indicating that a reconcile operation should enable the statements recovery.
 * @see org.eclipse.wst.jsdt.core.dom.ASTParser#setStatementsRecovery(boolean)
 */
public static final int ENABLE_STATEMENTS_RECOVERY = 0x02;

/**
 * Constant indicating that a reconcile operation should enable the bindings recovery
 * @see org.eclipse.wst.jsdt.core.dom.ASTParser#setBindingsRecovery(boolean)
 * @see org.eclipse.wst.jsdt.core.dom.IBinding#isRecovered()
 */
public static final int ENABLE_BINDINGS_RECOVERY = 0x04;


/**
 * Changes this javaScript file handle into a working copy. A new {@link IBuffer} is
 * created using this javaScript file handle's owner. Uses the primary owner is none was
 * specified when this javaScript file handle was created.
 * <p>
 * When switching to working copy mode, problems are reported to given
 * {@link IProblemRequestor}. Note that once in working copy mode, the given
 * {@link IProblemRequestor} is ignored. Only the original {@link IProblemRequestor}
 * is used to report subsequent problems.
 * </p>
 * <p>
 * Once in working copy mode, changes to this javaScript file or its children are done in memory.
 * Only the new buffer is affected. Using {@link #commitWorkingCopy(boolean, IProgressMonitor)}
 * will bring the underlying resource in sync with this javaScript file.
 * </p>
 * <p>
 * If this JavaScript file was already in working copy mode, an internal counter is incremented and no
 * other action is taken on this javaScript file. To bring this javaScript file back into the original mode
 * (where it reflects the underlying resource), {@link #discardWorkingCopy} must be call as many
 * times as {@link #becomeWorkingCopy(IProblemRequestor, IProgressMonitor)}.
 * </p>
 *
 * @param problemRequestor a requestor which will get notified of problems detected during
 * 	reconciling as they are discovered. The requestor can be set to <code>null</code> indicating
 * 	that the client is not interested in problems.
 * @param monitor a progress monitor used to report progress while opening this javaScript file
 * 	or <code>null</code> if no progress should be reported
 * @throws JavaScriptModelException if this javaScript file could not become a working copy.
 * @see #discardWorkingCopy()
  *
 * @deprecated Use {@link #becomeWorkingCopy(IProgressMonitor)} instead.
 * 	Note that if this deprecated method is used, problems will be reported to the given problem requestor
 * 	as well as the problem requestor returned by the working copy owner (if not null).  While this may
 *  be desired in <b>some</b> situations, by and large it is not.
*/
void becomeWorkingCopy(IProblemRequestor problemRequestor, IProgressMonitor monitor) throws JavaScriptModelException;
/**
 * Changes this javaScript file handle into a working copy. A new {@link IBuffer} is
 * created using this javaScript file handle's owner. Uses the primary owner if none was
 * specified when this javaScript file handle was created.
 * <p>
 * When switching to working copy mode, problems are reported to the {@link IProblemRequestor
 * problem requestor} of the {@link WorkingCopyOwner working copy owner}.
 * </p><p>
 * Once in working copy mode, changes to this javaScript file or its children are done in memory.
 * Only the new buffer is affected. Using {@link #commitWorkingCopy(boolean, IProgressMonitor)}
 * will bring the underlying resource in sync with this javaScript file.
 * </p><p>
 * If this javaScript file was already in working copy mode, an internal counter is incremented and no
 * other action is taken on this javaScript file. To bring this javaScript file back into the original mode
 * (where it reflects the underlying resource), {@link #discardWorkingCopy} must be call as many
 * times as {@link #becomeWorkingCopy(IProblemRequestor, IProgressMonitor)}.
 * </p>
 *
 * @param monitor a progress monitor used to report progress while opening this javaScript file
 * 	or <code>null</code> if no progress should be reported
 * @throws JavaScriptModelException if this javaScript file could not become a working copy.
 * @see #discardWorkingCopy()
 */
void becomeWorkingCopy(IProgressMonitor monitor) throws JavaScriptModelException;
/**
 * Commits the contents of this working copy to its underlying resource.
 *
 * <p>It is possible that the contents of the original resource have changed
 * since this working copy was created, in which case there is an update conflict.
 * The value of the <code>force</code> parameter effects the resolution of
 * such a conflict:<ul>
 * <li> <code>true</code> - in this case the contents of this working copy are applied to
 * 	the underlying resource even though this working copy was created before
 *		a subsequent change in the resource</li>
 * <li> <code>false</code> - in this case a {@link JavaScriptModelException} is thrown</li>
 * </ul>
 * @param force a flag to handle the cases when the contents of the original resource have changed
 * since this working copy was created
 * @param monitor the given progress monitor
 * @throws JavaScriptModelException if this working copy could not commit. Reasons include:
 * <ul>
 * <li> A {@link org.eclipse.core.runtime.CoreException} occurred while updating an underlying resource
 * <li> This element is not a working copy (INVALID_ELEMENT_TYPES)
 * <li> A update conflict (described above) (UPDATE_CONFLICT)
 * </ul>
 */
void commitWorkingCopy(boolean force, IProgressMonitor monitor) throws JavaScriptModelException;
/**
 * Creates and returns an non-static import declaration in this javaScript file
 * with the given name. This method is equivalent to
 * <code>createImport(name, Flags.AccDefault, sibling, monitor)</code>.
 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
 *
 * @param name the name of the import declaration to add
 * @param sibling the existing element which the import declaration will be inserted immediately before (if
 *	<code> null </code>, then this import will be inserted as the last import declaration.
 * @param monitor the progress monitor to notify
 * @return the newly inserted import declaration (or the previously existing one in case attempting to create a duplicate)
 *
 * @throws JavaScriptModelException if the element could not be created. Reasons include:
 * <ul>
 * <li> This JavaScript element does not exist or the specified sibling does not exist (ELEMENT_DOES_NOT_EXIST)</li>
 * <li> A {@link org.eclipse.core.runtime.CoreException} occurred while updating an underlying resource
 * <li> The specified sibling is not a child of this javaScript file (INVALID_SIBLING)
 * <li> The name is not a valid import name (INVALID_NAME)
 * </ul>
 * @see #createImport(String, IJavaScriptElement, int, IProgressMonitor)
 * 
 */
IImportDeclaration createImport(String name, IJavaScriptElement sibling, IProgressMonitor monitor) throws JavaScriptModelException;

/**
 * Creates and returns an import declaration in this javaScript file
 * with the given name.
 * <p>
 * Optionally, the new element can be positioned before the specified
 * sibling. If no sibling is specified, the element will be inserted
 * as the last import declaration in this javaScript file.
 * <p>
 * If the javaScript file already includes the specified import declaration,
 * the import is not generated (it does not generate duplicates).
 *
 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
 *
 * @param name the name of the import declaration 
 * @param sibling the existing element which the import declaration will be inserted immediately before (if
 *	<code> null </code>, then this import will be inserted as the last import declaration.
 * @param flags {@link Flags#AccStatic} for static imports, or
 * {@link Flags#AccDefault} for regular imports; other modifier flags
 * are ignored
 * @param monitor the progress monitor to notify
 * @return the newly inserted import declaration (or the previously existing one in case attempting to create a duplicate)
 *
 * @throws JavaScriptModelException if the element could not be created. Reasons include:
 * <ul>
 * <li> This JavaScript element does not exist or the specified sibling does not exist (ELEMENT_DOES_NOT_EXIST)</li>
 * <li> A {@link org.eclipse.core.runtime.CoreException} occurred while updating an underlying resource
 * <li> The specified sibling is not a child of this javaScript file (INVALID_SIBLING)
 * <li> The name is not a valid import name (INVALID_NAME)
 * </ul>
 * @see Flags
 */
IImportDeclaration createImport(String name, IJavaScriptElement sibling, int flags, IProgressMonitor monitor) throws JavaScriptModelException;

/**
 * Creates and returns a type in this javaScript file with the
 * given contents. If this javaScript file does not exist, one
 * will be created with an appropriate package declaration.
 * <p>
 * Optionally, the new type can be positioned before the specified
 * sibling. If <code>sibling</code> is <code>null</code>, the type will be appended
 * to the end of this javaScript file.
 *
 * <p>It is possible that a type with the same name already exists in this javaScript file.
 * The value of the <code>force</code> parameter effects the resolution of
 * such a conflict:<ul>
 * <li> <code>true</code> - in this case the type is created with the new contents</li>
 * <li> <code>false</code> - in this case a {@link JavaScriptModelException} is thrown</li>
 * </ul>
 *
 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
 *
 *
 * @param contents the source contents of the type declaration to add.
 * @param sibling the existing element which the type will be inserted immediately before (if
 *	<code>null</code>, then this type will be inserted as the last type declaration.
 * @param force a <code>boolean</code> flag indicating how to deal with duplicates
 * @param monitor the progress monitor to notify
 * @return the newly inserted type
 *
 * @throws JavaScriptModelException if the element could not be created. Reasons include:
 * <ul>
 * <li>The specified sibling element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
 * <li> A {@link org.eclipse.core.runtime.CoreException} occurred while updating an underlying resource
 * <li> The specified sibling is not a child of this javaScript file (INVALID_SIBLING)
 * <li> The contents could not be recognized as a type declaration (INVALID_CONTENTS)
 * <li> There was a naming collision with an existing type (NAME_COLLISION)
 * </ul>
 */
IType createType(String contents, IJavaScriptElement sibling, boolean force, IProgressMonitor monitor) throws JavaScriptModelException;

/**
 * Creates and returns a var in this javaScript file with the
 * given contents. If this javaScript file does not exist, one
 * will be created with an appropriate package declaration.
 * <p>
 * Optionally, the new var can be positioned before the specified
 * sibling. If <code>sibling</code> is <code>null</code>, the var will be appended
 * to the end of this javaScript file.
 *
 * <p>It is possible that a var with the same name already exists in this javaScript file.
 * The value of the <code>force</code> parameter effects the resolution of
 * such a conflict:<ul>
 * <li> <code>true</code> - in this case the var is created with the new contents</li>
 * <li> <code>false</code> - in this case a {@link JavaScriptModelException} is thrown</li>
 * </ul>
 *
 * @param contents the source contents of the var declaration to add.
 * @param sibling the existing element which the var will be inserted immediately before (if
 *	<code>null</code>, then this var will be inserted as the last var declaration.
 * @param force a <code>boolean</code> flag indicating how to deal with duplicates
 * @param monitor the progress monitor to notify
 * @return the newly inserted var
 *
 * @throws JavaScriptModelException if the element could not be created. Reasons include:
 * <ul>
 * <li>The specified sibling element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
 * <li> A {@link org.eclipse.core.runtime.CoreException} occurred while updating an underlying resource
 * <li> The specified sibling is not a child of this javaScript file (INVALID_SIBLING)
 * <li> The contents could not be recognized as a var declaration (INVALID_CONTENTS)
 * <li> There was a naming collision with an existing var (NAME_COLLISION)
 * </ul>
 */
IField createField(String contents, IJavaScriptElement sibling, boolean force, IProgressMonitor monitor)
throws JavaScriptModelException;


/**
 * Creates and returns a function in this javaScript file with the
 * given contents. If this javaScript file does not exist, one
 * will be created with an appropriate package declaration.
 * <p>
 * Optionally, the new function can be positioned before the specified
 * sibling. If <code>sibling</code> is <code>null</code>, the function will be appended
 * to the end of this javaScript file.
 *
 * <p>It is possible that a function with the same name already exists in this javaScript file.
 * The value of the <code>force</code> parameter effects the resolution of
 * such a conflict:<ul>
 * <li> <code>true</code> - in this case the function is created with the new contents</li>
 * <li> <code>false</code> - in this case a {@link JavaScriptModelException} is thrown</li>
 * </ul>
 *
 * @param contents the source contents of the function declaration to add.
 * @param sibling the existing element which the function will be inserted immediately before (if
 *	<code>null</code>, then this function will be inserted as the last function declaration.
 * @param force a <code>boolean</code> flag indicating how to deal with duplicates
 * @param monitor the progress monitor to notify
 * @return the newly inserted function
 *
 * @throws JavaScriptModelException if the element could not be created. Reasons include:
 * <ul>
 * <li>The specified sibling element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
 * <li> A {@link org.eclipse.core.runtime.CoreException} occurred while updating an underlying resource
 * <li> The specified sibling is not a child of this javaScript file (INVALID_SIBLING)
 * <li> The contents could not be recognized as a function declaration (INVALID_CONTENTS)
 * <li> There was a naming collision with an existing function (NAME_COLLISION)
 * </ul>
 */
IFunction createMethod(String contents, IJavaScriptElement sibling, boolean force, IProgressMonitor monitor)
throws JavaScriptModelException;

/**
 * Changes this javaScript file in working copy mode back to its original mode.
 * <p>
 * This has no effect if this javaScript file was not in working copy mode.
 * </p>
 * <p>
 * If {@link #becomeWorkingCopy} was called several times on this
 * javaScript file, {@link #discardWorkingCopy} must be called as
 * many times before it switches back to the original mode.
 * </p>
 *
 * @throws JavaScriptModelException if this working copy could not return in its original mode.
 * @see #becomeWorkingCopy(IProblemRequestor, IProgressMonitor)
 */
void discardWorkingCopy() throws JavaScriptModelException;
/**
 * Finds the elements in this javaScript file that correspond to
 * the given element.
 * An element A corresponds to an element B if:
 * <ul>
 * <li>A has the same element name as B.
 * <li>If A is a method, A must have the same number of arguments as
 *     B and the simple names of the argument types must be equals.
 * <li>The parent of A corresponds to the parent of B recursively up to
 *     their respective javaScript files.
 * <li>A exists.
 * </ul>
 * Returns <code>null</code> if no such javaScript elements can be found
 * or if the given element is not included in a javaScript file.
 *
 * @param element the given element
 * @return the found elements in this javaScript file that correspond to the given element
 */
IJavaScriptElement[] findElements(IJavaScriptElement element);
/**
 * Finds the working copy for this javaScript file, given a {@link WorkingCopyOwner}.
 * If no working copy has been created for this javaScript file associated with this
 * working copy owner, returns <code>null</code>.
 * <p>
 * Users of this method must not destroy the resulting working copy.
 *
 * @param owner the given {@link WorkingCopyOwner}
 * @return the found working copy for this javaScript file, <code>null</code> if none
 * @see WorkingCopyOwner
 */
IJavaScriptUnit findWorkingCopy(WorkingCopyOwner owner);
/**
 * Returns all types declared in this javaScript file in the order
 * in which they appear in the source.
 * This includes all top-level types and nested member types.
 * It does NOT include local types (types defined in methods).
 *
 * @return the array of top-level and member types defined in a javaScript file, in declaration order.
 * @throws JavaScriptModelException if this element does not exist or if an
 *		exception occurs while accessing its corresponding resource
 */
IType[] getAllTypes() throws JavaScriptModelException;
/**
 * Returns the first import declaration in this javaScript file with the given name.
 * This is a handle-only method. The import declaration may or may not exist. This
 * is a convenience method - imports can also be accessed from a javaScript file's
 * import container.
 *
 *
 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
 *
 * @param name the name of the import to find
 * @return a handle onto the corresponding import declaration. The import declaration may or may not exist.
 */
IImportDeclaration getImport(String name) ;
/**
 * Returns the import container for this javaScript file.
 * This is a handle-only method. The import container may or
 * may not exist. The import container can used to access the
 * imports.
 *
 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
 *
 * @return a handle onto the corresponding import container. The
 *		import contain may or may not exist.
 */
IImportContainer getImportContainer();
/**
 * Returns the import declarations in this javaScript file
 * in the order in which they appear in the source. This is
 * a convenience method - import declarations can also be
 * accessed from a javaScript file's import container.
 *
 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
 *
 * @return the import declarations in this javaScript file
 * @throws JavaScriptModelException if this element does not exist or if an
 *		exception occurs while accessing its corresponding resource
 */
IImportDeclaration[] getImports() throws JavaScriptModelException;
/**
 * Returns the primary javaScript file (whose owner is the primary owner)
 * this working copy was created from, or this javaScript file if this a primary
 * javaScript file.
 * <p>
 * Note that the returned primary javaScript file can be in working copy mode.
 * </p>
 *
 * @return the primary javaScript file this working copy was created from,
 * or this javaScript file if it is primary
 */
IJavaScriptUnit getPrimary();
/**
 * Returns the working copy owner of this working copy.
 * Returns null if it is not a working copy or if it has no owner.
 *
 * @return WorkingCopyOwner the owner of this working copy or <code>null</code>
 */
WorkingCopyOwner getOwner();
/**
 * Returns the top-level types declared in this javaScript file
 * in the order in which they appear in the source.
 *
 *
 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
 *
 * @return the top-level types declared in this javaScript file
 * @throws JavaScriptModelException if this element does not exist or if an
 *		exception occurs while accessing its corresponding resource
 */
IType[] getTypes() throws JavaScriptModelException;
/**
 * Returns a new working copy of this javaScript file if it is a primary javaScript file,
 * or this javaScript file if it is already a non-primary working copy.
 * <p>
 * Note: if intending to share a working copy amongst several clients, then
 * {@link #getWorkingCopy(WorkingCopyOwner, IProblemRequestor, IProgressMonitor)}
 * should be used instead.
 * </p><p>
 * When the working copy instance is created, an ADDED IJavaScriptElementDelta is
 * reported on this working copy.
 * </p><p>
 * Once done with the working copy, users of this method must discard it using
 * {@link #discardWorkingCopy()}.
 * </p><p>
 * Since 2.1, a working copy can be created on a not-yet existing compilation
 * unit. In particular, such a working copy can then be committed in order to create
 * the corresponding javaScript file.
 * </p>
 * @param monitor a progress monitor used to report progress while opening this javaScript file
 *                 or <code>null</code> if no progress should be reported
 * @throws JavaScriptModelException if the contents of this element can
 *   not be determined.
 * @return a new working copy of this element if this element is not
 * a working copy, or this element if this element is already a working copy
 */
IJavaScriptUnit getWorkingCopy(IProgressMonitor monitor) throws JavaScriptModelException;
/**
 * Returns a shared working copy on this javaScript file using the given working copy owner to create
 * the buffer, or this javaScript file if it is already a non-primary working copy.
 * This API can only answer an already existing working copy if it is based on the same
 * original javaScript file AND was using the same working copy owner (that is, as defined by {@link Object#equals}).
 * <p>
 * The life time of a shared working copy is as follows:
 * <ul>
 * <li>The first call to {@link #getWorkingCopy(WorkingCopyOwner, IProblemRequestor, IProgressMonitor)}
 * 	creates a new working copy for this element</li>
 * <li>Subsequent calls increment an internal counter.</li>
 * <li>A call to {@link #discardWorkingCopy()} decrements the internal counter.</li>
 * <li>When this counter is 0, the working copy is discarded.
 * </ul>
 * So users of this method must discard exactly once the working copy.
 * <p>
 * Note that the working copy owner will be used for the life time of this working copy, that is if the
 * working copy is closed then reopened, this owner will be used.
 * The buffer will be automatically initialized with the original's javaScript file content
 * upon creation.
 * <p>
 * When the shared working copy instance is created, an ADDED IJavaScriptElementDelta is reported on this
 * working copy.
 * </p><p>
 * Since 2.1, a working copy can be created on a not-yet existing compilation
 * unit. In particular, such a working copy can then be committed in order to create
 * the corresponding javaScript file.
 * </p>
 * @param owner the working copy owner that creates a buffer that is used to get the content
 * 				of the working copy
 * @param problemRequestor a requestor which will get notified of problems detected during
 * 	reconciling as they are discovered. The requestor can be set to <code>null</code> indicating
 * 	that the client is not interested in problems.
 * @param monitor a progress monitor used to report progress while opening this javaScript file
 *                 or <code>null</code> if no progress should be reported
 * @throws JavaScriptModelException if the contents of this element can
 *   not be determined.
 * @return a new working copy of this element using the given factory to create
 * the buffer, or this element if this element is already a working copy
  * @deprecated Use {@link ITypeRoot#getWorkingCopy(WorkingCopyOwner, IProgressMonitor)} instead.
 * 	Note that if this deprecated method is used, problems will be reported on the passed problem requester
 * 	as well as on the problem requestor returned by the working copy owner (if not null).
*/
IJavaScriptUnit getWorkingCopy(WorkingCopyOwner owner, IProblemRequestor problemRequestor, IProgressMonitor monitor) throws JavaScriptModelException;
/**
 * Returns whether the resource of this working copy has changed since the
 * inception of this working copy.
 * Returns <code>false</code> if this javaScript file is not in working copy mode.
 *
 * @return whether the resource has changed
 */
public boolean hasResourceChanged();
/**
 * Returns whether this element is a working copy.
 *
 * @return true if this element is a working copy, false otherwise
 */
boolean isWorkingCopy();

/**
 * Reconciles the contents of this working copy, sends out a JavaScript delta
 * notification indicating the nature of the change of the working copy since
 * the last time it was either reconciled or made consistent
 * ({@link IOpenable#makeConsistent(IProgressMonitor)}), and returns a
 * javaScript file AST if requested.
 * <p>
 * It performs the reconciliation by locally caching the contents of
 * the working copy, updating the contents, then creating a delta
 * over the cached contents and the new contents, and finally firing
 * this delta.
 * <p>
 * The boolean argument allows to force problem detection even if the
 * working copy is already consistent.
 * </p>
 * <p>
 * This functionality allows to specify a working copy owner which is used
 * during problem detection. All references contained in the working copy are
 * resolved against other units; for which corresponding owned working copies
 * are going to take precedence over their original javaScript files. If
 * <code>null</code> is passed in, then the primary working copy owner is used.
 * </p>
 * <p>
 * Compilation problems found in the new contents are notified through the
 * {@link IProblemRequestor} interface which was passed at
 * creation, and no longer as transient markers.
 * </p>
 * <p>
 * Note: Since 3.0, added/removed/changed inner types generate change deltas.
 * </p>
 * <p>
 * If requested, a DOM AST representing the javaScript file is returned.
 * Its bindings are computed only if the problem requestor is active, or if the
 * problem detection is forced. This method returns <code>null</code> if the
 * creation of the DOM AST was not requested, or if the requested level of AST
 * API is not supported, or if the working copy was already consistent.
 * </p>
 *
 * <p>
 * This method doesn't perform statements recovery. To recover statements with syntax
 * errors, {@link #reconcile(int, boolean, boolean, WorkingCopyOwner, IProgressMonitor)} must be use.
 * </p>
 *
 * @param astLevel either {@link #NO_AST} if no AST is wanted,
 * or the {@linkplain AST#newAST(int) AST API level} of the AST if one is wanted
 * @param forceProblemDetection boolean indicating whether problem should be
 *   recomputed even if the source hasn't changed
 * @param owner the owner of working copies that take precedence over the
 *   original javaScript files, or <code>null</code> if the primary working
 *   copy owner should be used
 * @param monitor a progress monitor
 * @return the javaScript file AST or <code>null</code> if not requested,
 *    or if the requested level of AST API is not supported,
 *    or if the working copy was consistent
 * @throws JavaScriptModelException if the contents of the original element
 *		cannot be accessed. Reasons include:
 * <ul>
 * <li> The original JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
 * </ul>
 */
JavaScriptUnit reconcile(int astLevel, boolean forceProblemDetection, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaScriptModelException;

/**
 * Reconciles the contents of this working copy, sends out a JavaScript delta
 * notification indicating the nature of the change of the working copy since
 * the last time it was either reconciled or made consistent
 * ({@link IOpenable#makeConsistent(IProgressMonitor)}), and returns a
 * javaScript file AST if requested.
 * <p>
 * It performs the reconciliation by locally caching the contents of
 * the working copy, updating the contents, then creating a delta
 * over the cached contents and the new contents, and finally firing
 * this delta.
 * <p>
 * The boolean argument allows to force problem detection even if the
 * working copy is already consistent.
 * </p>
 * <p>
 * This functionality allows to specify a working copy owner which is used
 * during problem detection. All references contained in the working copy are
 * resolved against other units; for which corresponding owned working copies
 * are going to take precedence over their original javaScript files. If
 * <code>null</code> is passed in, then the primary working copy owner is used.
 * </p>
 * <p>
 * Compilation problems found in the new contents are notified through the
 * {@link IProblemRequestor} interface which was passed at
 * creation, and no longer as transient markers.
 * </p>
 * <p>
 * Note: Since 3.0, added/removed/changed inner types generate change deltas.
 * </p>
 * <p>
 * If requested, a DOM AST representing the javaScript file is returned.
 * Its bindings are computed only if the problem requestor is active, or if the
 * problem detection is forced. This method returns <code>null</code> if the
 * creation of the DOM AST was not requested, or if the requested level of AST
 * API is not supported, or if the working copy was already consistent.
 * </p>
 *
 * <p>
 * If statements recovery is enabled then this method tries to rebuild statements
 * with syntax error. Otherwise statements with syntax error won't be present in
 * the returning DOM AST.
 * </p>
 *
 * @param astLevel either {@link #NO_AST} if no AST is wanted,
 * or the {@linkplain org.eclipse.wst.jsdt.core.dom.AST#newAST(int) AST API level} of the AST if one is wanted
 * @param forceProblemDetection boolean indicating whether problem should be
 *   recomputed even if the source hasn't changed
 * @param enableStatementsRecovery if <code>true</code> statements recovery is enabled.
 * @param owner the owner of working copies that take precedence over the
 *   original javaScript files, or <code>null</code> if the primary working
 *   copy owner should be used
 * @param monitor a progress monitor
 * @return the javaScript file AST or <code>null</code> if not requested,
 *    or if the requested level of AST API is not supported,
 *    or if the working copy was consistent
 * @throws JavaScriptModelException if the contents of the original element
 *		cannot be accessed. Reasons include:
 * <ul>
 * <li> The original JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
 * </ul>
 */
JavaScriptUnit reconcile(int astLevel, boolean forceProblemDetection, boolean enableStatementsRecovery, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaScriptModelException;

/**
 * Reconciles the contents of this working copy, sends out a JavaScript delta
 * notification indicating the nature of the change of the working copy since
 * the last time it was either reconciled or made consistent
 * ({@link IOpenable#makeConsistent(IProgressMonitor)}), and returns a
 * javaScript file AST if requested.
 *
 * <p>
 * If the problem detection is forced by passing the {@link #FORCE_PROBLEM_DETECTION} bit in the given reconcile flag,
 * problem detection is run even if the working copy is already consistent.
 * </p>
 *
 * <p>
 * It performs the reconciliation by locally caching the contents of
 * the working copy, updating the contents, then creating a delta
 * over the cached contents and the new contents, and finally firing
 * this delta.</p>
 *
 * <p>
 * This functionality allows to specify a working copy owner which is used
 * during problem detection. All references contained in the working copy are
 * resolved against other units; for which corresponding owned working copies
 * are going to take precedence over their original javaScript files. If
 * <code>null</code> is passed in, then the primary working copy owner is used.
 * </p>
 * <p>
 * Compilation problems found in the new contents are notified through the
 * {@link IProblemRequestor} interface which was passed at
 * creation, and no longer as transient markers.
 * </p>
 * <p>
 * Note: Since 3.0, added/removed/changed inner types generate change deltas.
 * </p>
 * <p>
 * If requested, a DOM AST representing the javaScript file is returned.
 * Its bindings are computed only if the problem requestor is active, or if the
 * problem detection is forced. This method returns <code>null</code> if the
 * creation of the DOM AST was not requested, or if the requested level of AST
 * API is not supported, or if the working copy was already consistent.
 * </p>
 *
 * <p>
 * If statements recovery is enabled by passing the {@link #ENABLE_STATEMENTS_RECOVERY} bit in the given reconcile flag
 * then this method tries to rebuild statements with syntax error. Otherwise statements with syntax error won't be
 * present in the returning DOM AST.</p>
 * <p>
 * If bindings recovery is enabled by passing the {@link #ENABLE_BINDINGS_RECOVERY} bit in the given reconcile flag
 * then this method tries to resolve bindings even if the type resolution contains errors.</p>
 * <p>
 * The given reconcile flags is a bit-mask of the different constants ({@link #ENABLE_BINDINGS_RECOVERY},
 * {@link #ENABLE_STATEMENTS_RECOVERY}, {@link #FORCE_PROBLEM_DETECTION}). Unspecified values are left for future use.
 * </p>
 *
 * @param astLevel either {@link #NO_AST} if no AST is wanted,
 * or the {@linkplain org.eclipse.wst.jsdt.core.dom.AST#newAST(int) AST API level} of the AST if one is wanted
 * @param reconcileFlags the given reconcile flags
 * @param owner the owner of working copies that take precedence over the
 *   original javaScript files, or <code>null</code> if the primary working
 *   copy owner should be used
 * @param monitor a progress monitor
 * @return the javaScript file AST or <code>null</code> if not requested,
 *    or if the requested level of AST API is not supported,
 *    or if the working copy was consistent
 * @throws JavaScriptModelException if the contents of the original element
 *		cannot be accessed. Reasons include:
 * <ul>
 * <li> The original JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
 * </ul>
 * @see #FORCE_PROBLEM_DETECTION
 * @see #ENABLE_BINDINGS_RECOVERY
 * @see #ENABLE_STATEMENTS_RECOVERY
 */
JavaScriptUnit reconcile(int astLevel, int reconcileFlags, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaScriptModelException;

/**
 * Restores the contents of this working copy to the current contents of
 * this working copy's original element. Has no effect if this element
 * is not a working copy.
 *
 * <p>Note: This is the inverse of committing the content of the
 * working copy to the original element with {@link #commitWorkingCopy(boolean, IProgressMonitor)}.
 *
 * @throws JavaScriptModelException if the contents of the original element
 *		cannot be accessed.  Reasons include:
 * <ul>
 * <li> The original JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
 * </ul>
 */
void restore() throws JavaScriptModelException;

/**
 * Finds the function in this javaScript file that correspond to
 * the given function.
 * Returns <code>null</code> if no such function can be found
 * or if the given element is not included in a javaScript file.
 *
 * @param function the given function
 * @return the found functions in this javaScript file that correspond to the given function
 */
IFunction[] findFunctions(IFunction function);




}
