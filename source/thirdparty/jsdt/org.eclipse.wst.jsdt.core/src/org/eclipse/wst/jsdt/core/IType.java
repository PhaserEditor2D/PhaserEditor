/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Represents  a source type in a JavaScript file (either a top-level
 * type, a member type, or a local type)
 * </p>
 * <p>
 * The children are of type <code>IMember</code>, which includes <code>IField</code>,
 * <code>IFunction</code>, <code>IInitializer</code> and <code>IType</code>.
 * The children are listed in the order in which they appear in the source file.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * <p>
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 * </p>
 *  
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IType extends IMember, IFunctionContainer {

	/**
	 * Do code completion inside a code snippet in the context of the current type.
	 *
	 * If the type can access to his source code and the insertion position is valid,
	 * then completion is performed against source. Otherwise the completion is performed
	 * against type structure and given locals variables.
	 *
	 * @param snippet the code snippet
	 * @param insertion the position with in source where the snippet
	 * is inserted. This position must not be in comments.
	 * A possible value is -1, if the position is not known.
	 * @param position the position within snippet where the user
	 * is performing code assist.
	 * @param localVariableTypeNames an array (possibly empty) of fully qualified
	 * type names of local variables visible at the current scope
	 * @param localVariableNames an array (possibly empty) of local variable names
	 * that are visible at the current scope
	 * @param localVariableModifiers an array (possible empty) of modifiers for
	 * local variables
	 * @param isStatic whether the current scope is in a static context
	 * @param requestor the completion requestor
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 */
	void codeComplete(
		char[] snippet,
		int insertion,
		int position,
		char[][] localVariableTypeNames,
		char[][] localVariableNames,
		int[] localVariableModifiers,
		boolean isStatic,
		CompletionRequestor requestor)
		throws JavaScriptModelException;

	/**
	 * Do code completion inside a code snippet in the context of the current type.
	 * It considers types in the working copies with the given owner first. In other words,
	 * the owner's working copies will take precedence over their original compilation units
	 * in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * </p><p>
	 * If the type can access to his source code and the insertion position is valid,
	 * then completion is performed against source. Otherwise the completion is performed
	 * against type structure and given locals variables.
	 * </p>
	 *
	 * @param snippet the code snippet
	 * @param insertion the position with in source where the snippet
	 * is inserted. This position must not be in comments.
	 * A possible value is -1, if the position is not known.
	 * @param position the position with in snippet where the user
	 * is performing code assist.
	 * @param localVariableTypeNames an array (possibly empty) of fully qualified
	 * type names of local variables visible at the current scope
	 * @param localVariableNames an array (possibly empty) of local variable names
	 * that are visible at the current scope
	 * @param localVariableModifiers an array (possible empty) of modifiers for
	 * local variables
	 * @param isStatic whether the current scope is in a static context
	 * @param requestor the completion requestor
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 */
	void codeComplete(
		char[] snippet,
		int insertion,
		int position,
		char[][] localVariableTypeNames,
		char[][] localVariableNames,
		int[] localVariableModifiers,
		boolean isStatic,
		CompletionRequestor requestor,
		WorkingCopyOwner owner)
		throws JavaScriptModelException;


	/**
	 * Creates and returns a field in this type with the
	 * given contents.
	 * <p>
	 * Optionally, the new element can be positioned before the specified
	 * sibling. If no sibling is specified, the element will be inserted
	 * as the last field declaration in this type.</p>
	 *
	 * <p>It is possible that a field with the same name already exists in this type.
	 * The value of the <code>force</code> parameter effects the resolution of
	 * such a conflict:<ul>
	 * <li> <code>true</code> - in this case the field is created with the new contents</li>
	 * <li> <code>false</code> - in this case a <code>JavaScriptModelException</code> is thrown</li>
	 * </ul></p>
	 *
	 * @param contents the given contents
	 * @param sibling the given sibling
	 * @param force a flag in case the same name already exists in this type
	 * @param monitor the given progress monitor
	 * @exception JavaScriptModelException if the element could not be created. Reasons include:
	 * <ul>
	 * <li> This JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> A <code>CoreException</code> occurred while updating an underlying resource
	 * <li> The specified sibling is not a child of this type (INVALID_SIBLING)
	 * <li> The contents could not be recognized as a field declaration (INVALID_CONTENTS)
	 * <li> This type is read-only (binary) (READ_ONLY)
	 * <li> There was a naming collision with an existing field (NAME_COLLISION)
	 * </ul>
	 * @return a field in this type with the given contents
	 */
	IField createField(String contents, IJavaScriptElement sibling, boolean force, IProgressMonitor monitor)
		throws JavaScriptModelException;


	/**
	 * Creates and returns a method or constructor in this type with the
	 * given contents.
	 * <p>
	 * Optionally, the new element can be positioned before the specified
	 * sibling. If no sibling is specified, the element will be appended
	 * to this type.
	 *
	 * <p>It is possible that a method with the same signature already exists in this type.
	 * The value of the <code>force</code> parameter effects the resolution of
	 * such a conflict:<ul>
	 * <li> <code>true</code> - in this case the method is created with the new contents</li>
	 * <li> <code>false</code> - in this case a <code>JavaScriptModelException</code> is thrown</li>
	 * </ul></p>
	 *
	 * @param contents the given contents
	 * @param sibling the given sibling
	 * @param force a flag in case the same name already exists in this type
	 * @param monitor the given progress monitor
	 * @exception JavaScriptModelException if the element could not be created. Reasons include:
	 * <ul>
	 * <li> This JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> A <code>CoreException</code> occurred while updating an underlying resource
	 * <li> The specified sibling is not a child of this type (INVALID_SIBLING)
	 * <li> The contents could not be recognized as a method or constructor
	 *		declaration (INVALID_CONTENTS)
	 * <li> This type is read-only (binary) (READ_ONLY)
	 * <li> There was a naming collision with an existing method (NAME_COLLISION)
	 * </ul>
	 * @return a method or constructor in this type with the given contents
	 */
	IFunction createMethod(String contents, IJavaScriptElement sibling, boolean force, IProgressMonitor monitor)
		throws JavaScriptModelException;

	/**
	 * Creates and returns a type in this type with the
	 * given contents.
	 * <p>
	 * Optionally, the new type can be positioned before the specified
	 * sibling. If no sibling is specified, the type will be appended
	 * to this type.</p>
	 *
	 * <p>It is possible that a type with the same name already exists in this type.
	 * The value of the <code>force</code> parameter effects the resolution of
	 * such a conflict:<ul>
	 * <li> <code>true</code> - in this case the type is created with the new contents</li>
	 * <li> <code>false</code> - in this case a <code>JavaScriptModelException</code> is thrown</li>
	 * </ul></p>
	 *
	 * @param contents the given contents
	 * @param sibling the given sibling
	 * @param force a flag in case the same name already exists in this type
	 * @param monitor the given progress monitor
	 * @exception JavaScriptModelException if the element could not be created. Reasons include:
	 * <ul>
	 * <li> This JavaScript element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 * <li> A <code>CoreException</code> occurred while updating an underlying resource
	 * <li> The specified sibling is not a child of this type (INVALID_SIBLING)
	 * <li> The contents could not be recognized as a type declaration (INVALID_CONTENTS)
	 * <li> This type is read-only (binary) (READ_ONLY)
	 * <li> There was a naming collision with an existing field (NAME_COLLISION)
	 * </ul>
	 * @return a type in this type with the given contents
	 */
	IType createType(String contents, IJavaScriptElement sibling, boolean force, IProgressMonitor monitor)
		throws JavaScriptModelException;

	/**
	 * Finds the methods in this type that correspond to
	 * the given method.
	 * A method m1 corresponds to another method m2 if:
	 * <ul>
	 * <li>m1 has the same element name as m2.
	 * <li>m1 has the same number of arguments as m2 and
	 *     the simple names of the argument types must be equals.
	 * <li>m1 exists.
	 * </ul>
	 * @param method the given method
	 * @return the found method or <code>null</code> if no such methods can be found.
	 *
	 */
	IFunction[] findMethods(IFunction method);

	/**
	 * Returns the children of this type that have the given category as a <code>@category</code> tag.
	 * Returns an empty array if no children with this category exist.
	 *
	 * @return the children for the given category.
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *      exception occurs while accessing its corresponding resource.
	 */
	IJavaScriptElement[] getChildrenForCategory(String category) throws JavaScriptModelException;

	/**
	 * Returns the simple name of this type, unqualified by package or enclosing type.
	 * This is a handle-only method.
	 *
	 * @return the simple name of this type
	 */
	String getElementName();

	/**
	 * Returns the field with the specified name
	 * in this type (for example, <code>"bar"</code>).
	 * This is a handle-only method.  The field may or may not exist.
	 *
	 * @param name the given name
	 * @return the field with the specified name in this type
	 */
	IField getField(String name);

	/**
	 * Returns the fields declared by this type.
	 * If this is a source type, the results are listed in the order
	 * in which they appear in the source, otherwise, the results are
	 * in no particular order.  For binary types, this includes synthetic fields.
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return the fields declared by this type
	 */
	IField[] getFields() throws JavaScriptModelException;

	/**
	 * Returns the fully qualified name of this type,
	 * including qualification for any containing types and packages.
	 * This is the name of the package, followed by <code>'.'</code>,
	 * followed by the type-qualified name.
	 * This is a handle-only method.
	 *
	 * @see IType#getTypeQualifiedName()
	 * @return the fully qualified name of this type
	 */
	String getFullyQualifiedName();

	/**
	 * Returns the fully qualified name of this type,
	 * including qualification for any containing types and packages.
	 * This is the name of the package, followed by <code>'.'</code>,
	 * followed by the type-qualified name using the <code>enclosingTypeSeparator</code>.
	 *
	 *
	 * This is a handle-only method.
	 *
	 * @param enclosingTypeSeparator the given enclosing type separator
	 * @return the fully qualified name of this type, including qualification for any containing types and packages
	 * @see IType#getTypeQualifiedName(char)
	 */
	String getFullyQualifiedName(char enclosingTypeSeparator);

	/**
	 * Returns this type's fully qualified name
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *      exception occurs while accessing its corresponding resource.
	 * @return the fully qualified parameterized representation of this type
	 */
	String getFullyQualifiedParameterizedName() throws JavaScriptModelException;

	/**
	 * Returns the initializer with the specified position relative to
	 * the order they are defined in the source.
	 * Numbering starts at 1 (thus the first occurrence is occurrence 1, not occurrence 0).
	 * This is a handle-only method.  The initializer may or may not be present.
	 *
	 * @param occurrenceCount the specified position
	 * @return the initializer with the specified position relative to the order they are defined in the source
	 */
	IInitializer getInitializer(int occurrenceCount);

	/**
	 * Returns the initializers declared by this type.
	 * For binary types this is an empty collection.
	 * If this is a source type, the results are listed in the order
	 * in which they appear in the source.
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return the initializers declared by this type
	 */
	IInitializer[] getInitializers() throws JavaScriptModelException;

	/**
	 * Returns the binding key for this type. A binding key is a key that uniquely
	 * identifies this type. It allows access to generic info for parameterized
	 * types.
	 *
	 * @return the binding key for this type
	 * @see org.eclipse.wst.jsdt.core.dom.IBinding#getKey()
	 * @see BindingKey
	 */
	String getKey();

	/**
	 * Returns the method with the specified name and parameter types
	 * in this type (for example, <code>"foo", {"I", "QString;"}</code>).
	 * To get the handle for a constructor, the name specified must be the
	 * simple name of the enclosing type.
	 * This is a handle-only method.  The method may or may not be present.
	 * <p>
	 * The type signatures may be either unresolved (for source types)
	 * or resolved (for binary types), and either basic (for basic types)
	 * or rich (for parameterized types). See {@link Signature} for details.
	 * </p>
	 *
	 * @param name the given name
	 * @param parameterTypeSignatures the given parameter types
	 * @return the method with the specified name and parameter types in this type
	 */
	IFunction getFunction(String name, String[] parameterTypeSignatures);

	/**
	 * Returns the methods and constructors declared by this type.
	 * For binary types, this may include the special <code>&lt;clinit&gt;</code>; method
	 * and synthetic methods.
	 * If this is a source type, the results are listed in the order
	 * in which they appear in the source, otherwise, the results are
	 * in no particular order.
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return the methods and constructors declared by this type
	 */
	IFunction[] getFunctions() throws JavaScriptModelException;

	/**
	 * Returns the source folder (package fragment) in which this element is defined.
	 * This is a handle-only method.
	 *
	 * @return the package fragment in which this element is defined
	 */
	IPackageFragment getPackageFragment();

	/**
	 * Returns the name of this type's superclass, or <code>null</code>
	 * for source types that do not specify a superclass.
	 * <p>
	 * For interfaces, the superclass name is always <code>"java.lang.Object"</code>.
	 * For source types, the name as declared is returned, for binary types,
	 * the resolved, qualified name is returned.
	 * For anonymous types, the superclass name is the name appearing after the 'new' keyword'.
	 * If the superclass is a parameterized type, the string
	 * may include its type arguments enclosed in "&lt;&gt;".
	 * If the returned string is needed for anything other than display
	 * purposes, use {@link #getSuperclassTypeSignature()} which returns
	 * a structured type signature string containing more precise information.
	 * </p>
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return the name of this type's superclass, or <code>null</code> for source types that do not specify a superclass
	 */
	String getSuperclassName() throws JavaScriptModelException;

	/**
	 * Returns the type signature of this type's superclass, or
	 * <code>null</code> if none.
	 * <p>
	 * The type signature may be either unresolved (for source types)
	 * or resolved (for binary types), and either basic (for basic types)
	 * or rich (for parameterized types). See {@link Signature} for details.
	 * </p>
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return the type signature of this type's superclass, or
	 * <code>null</code> if none
	 */
	String getSuperclassTypeSignature() throws JavaScriptModelException;

	/**
	 * Returns the member type declared in this type with the given simple name.
	 * This is a handle-only method. The type may or may not exist.
	 *
	 * @param name the given simple name
	 * @return the member type declared in this type with the given simple name
	 */
	IType getType(String name);

	/**
	 * Returns the type-qualified name of this type,
	 * including qualification for any enclosing types,
	 * but not including package qualification.
	 * This is a handle-only method.
	 *
	 * @return the type-qualified name of this type
	 */
	String getTypeQualifiedName();

	/**
	 * Returns the type-qualified name of this type,
	 * including qualification for any enclosing types,
	 * but not including package qualification.
	 *
	 * This is a handle-only method.
	 *
	 * @param enclosingTypeSeparator the specified enclosing type separator
	 * @return the type-qualified name of this type
	 */
	String getTypeQualifiedName(char enclosingTypeSeparator);

	/**
	 * Returns the immediate member types declared by this type.
	 * The results are listed in the order in which they appear in the source or class file.
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return the immediate member types declared by this type
	 */
	IType[] getTypes() throws JavaScriptModelException;

	/**
	 * Returns whether this type represents an anonymous type.
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return true if this type represents an anonymous type, false otherwise
	 */
	boolean isAnonymous() throws JavaScriptModelException;

	/**
	 * Returns whether this type is read-only.
	 * <p>
	 * Note that a class can neither be an interface, an enumeration class, nor an annotation type.
	 * </p>
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return true if this type represents a class, false otherwise
	 */
	boolean isClass() throws JavaScriptModelException;

	/**
	 * Returns whether this type represents a local type.
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return true if this type represents a local type, false otherwise
	 */
	boolean isLocal() throws JavaScriptModelException;

	/**
	 * Returns whether this type represents a member type.
	 *
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return true if this type represents a member type, false otherwise
	 */
	boolean isMember() throws JavaScriptModelException;
	/**
	 * Returns whether this type represents a resolved type.
	 * If a type is resolved, its key contains resolved information.
	 *
	 * @return whether this type represents a resolved type.
	 */
	boolean isResolved();
	/**
	 * Loads a previously saved ITypeHierarchy from an input stream. A type hierarchy can
	 * be stored using ITypeHierachy#store(OutputStream).
	 *
	 * Only hierarchies originally created by the following methods can be loaded:
	 * <ul>
	 * <li>IType#newSupertypeHierarchy(IProgressMonitor)</li>
	 * <li>IType#newTypeHierarchy(IJavaScriptProject, IProgressMonitor)</li>
	 * <li>IType#newTypeHierarchy(IProgressMonitor)</li>
	 * </ul>
	 *
	 * @param input stream where hierarchy will be read
	 * @param monitor the given progress monitor
	 * @return the stored hierarchy
	 * @exception JavaScriptModelException if the hierarchy could not be restored, reasons include:
	 *      - type is not the focus of the hierarchy or
	 *		- unable to read the input stream (wrong format, IOException during reading, ...)
	 * @see ITypeHierarchy#store(java.io.OutputStream, IProgressMonitor)
	 */
	ITypeHierarchy loadTypeHierachy(InputStream input, IProgressMonitor monitor) throws JavaScriptModelException;
	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type and all of its supertypes.
	 *
	 * @param monitor the given progress monitor
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return a type hierarchy for this type containing this type and all of its supertypes
	 */
	ITypeHierarchy newSupertypeHierarchy(IProgressMonitor monitor) throws JavaScriptModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type and all of its supertypes, considering types in the given
	 * working copies. In other words, the list of working copies will take
	 * precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that passing an empty working copy will be as if the original compilation
	 * unit had been deleted.
	 * </p>
	 *
	 * @param workingCopies the working copies that take precedence over their original compilation units
	 * @param monitor the given progress monitor
	 * @return a type hierarchy for this type containing this type and all of its supertypes
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 */
	ITypeHierarchy newSupertypeHierarchy(IJavaScriptUnit[] workingCopies, IProgressMonitor monitor)
		throws JavaScriptModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type and all of its supertypes, considering types in the
	 * working copies with the given owner.
	 * In other words, the owner's working copies will take
	 * precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * <p>
	 *
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @param monitor the given progress monitor
	 * @return a type hierarchy for this type containing this type and all of its supertypes
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 */
	ITypeHierarchy newSupertypeHierarchy(WorkingCopyOwner owner, IProgressMonitor monitor)
		throws JavaScriptModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes
	 * in the context of the given project.
	 *
	 * @param project the given project
	 * @param monitor the given progress monitor
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes
	 * in the context of the given project
	 */
	ITypeHierarchy newTypeHierarchy(IJavaScriptProject project, IProgressMonitor monitor) throws JavaScriptModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes
	 * in the context of the given project, considering types in the
	 * working copies with the given owner.
	 * In other words, the owner's working copies will take
	 * precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * <p>
	 *
	 * @param project the given project
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @param monitor the given progress monitor
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes
	 * in the context of the given project
	 */
	ITypeHierarchy newTypeHierarchy(IJavaScriptProject project, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaScriptModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes in the workspace.
	 *
	 * @param monitor the given progress monitor
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 * @return a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes in the workspace
	 */
	ITypeHierarchy newTypeHierarchy(IProgressMonitor monitor) throws JavaScriptModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes in the workspace,
	 * considering types in the given working copies. In other words, the list of working
	 * copies that will take precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that passing an empty working copy will be as if the original compilation
	 * unit had been deleted.
	 *
	 * @param workingCopies the working copies that take precedence over their original compilation units
	 * @param monitor the given progress monitor
	 * @return a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes in the workspace
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 */
	ITypeHierarchy newTypeHierarchy(IJavaScriptUnit[] workingCopies, IProgressMonitor monitor) throws JavaScriptModelException;

	/**
	 * Creates and returns a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes in the workspace,
	 * considering types in the working copies with the given owner.
	 * In other words, the owner's working copies will take
	 * precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * <p>
	 *
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @param monitor the given progress monitor
	 * @return a type hierarchy for this type containing
	 * this type, all of its supertypes, and all its subtypes in the workspace
	 * @exception JavaScriptModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource.
	 */
	ITypeHierarchy newTypeHierarchy(WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaScriptModelException;

	/**
	 * Resolves the given type name within the context of this type (depending on the type hierarchy
	 * and its imports).
	 * <p>
	 * Multiple answers might be found in case there are ambiguous matches.
	 * </p>
	 * <p>
	 * Each matching type name is decomposed as an array of two strings, the first denoting the package
	 * name (dot-separated) and the second being the type name. The package name is empty if it is the
	 * default package. The type name is the type qualified name using a '.' enclosing type separator.
	 * </p>
	 * <p>
	 * Returns <code>null</code> if unable to find any matching type.
	 * </p>
	 *
	 * @param typeName the given type name
	 * @exception JavaScriptModelException if code resolve could not be performed.
	 * @return the resolved type names or <code>null</code> if unable to find any matching type
	 * @see #getTypeQualifiedName(char)
	 */
	String[][] resolveType(String typeName) throws JavaScriptModelException;

	/**
	 * Resolves the given type name within the context of this type (depending on the type hierarchy
	 * and its imports) and using the given owner's working copies, considering types in the
	 * working copies with the given owner. In other words, the owner's working copies will take
	 * precedence over their original compilation units in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * </p>
	 * <p>Multiple answers might be found in case there are ambiguous matches.
	 * </p>
	 * <p>
	 * Each matching type name is decomposed as an array of two strings, the first denoting the package
	 * name (dot-separated) and the second being the type name. The package name is empty if it is the
	 * default package. The type name is the type qualified name using a '.' enclosing type separator.
	 * </p>
	 * <p>
	 * Returns <code>null</code> if unable to find any matching type.
	 *</p>
	 *
	 * @param typeName the given type name
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @exception JavaScriptModelException if code resolve could not be performed.
	 * @return the resolved type names or <code>null</code> if unable to find any matching type
	 * @see #getTypeQualifiedName(char)
	 */
	String[][] resolveType(String typeName, WorkingCopyOwner owner) throws JavaScriptModelException;
}
