/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt - alex_blewitt@yahoo.com https://bugs.eclipse.org/bugs/show_bug.cgi?id=171066
 *******************************************************************************/

package org.eclipse.wst.jsdt.core.util;

import java.util.Comparator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.core.SortElementsOperation;

/**
 * Operation for sorting members within a javascript unit .
 * <p>
 * This class provides all functionality via static members; it is not
 * intended to be instantiated or subclassed.
 * </p>
 *
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public final class JavaScriptUnitSorter {

 	/**
 	 * Private constructor to prevent instantiation.
 	 */
	private JavaScriptUnitSorter() {
		// Not instantiable
	}

 
    private static void checkASTLevel(int level) {
        switch (level) {
        case AST.JLS2 :
        case AST.JLS3 :
            break;
        default :
            throw new IllegalArgumentException();
        }
    }

	/**
	 * Name of auxillary property whose value can be used to determine the
	 * original relative order of two body declarations. This allows a
	 * comparator to preserve the relative positions of certain kinds of
	 * body declarations when required.
	 * <p>
	 * All body declarations passed to the comparator's <code>compare</code>
	 * method by <code>JavaScriptUnitSorter.sort</code> carry an
	 * Integer-valued property. The body declaration with the lower value
	 * comes before the one with the higher value. The exact numeric value
	 * of these properties is unspecified.
	 * </p>
	 * <p>
	 * Example usage:
	 * <pre>
	 * BodyDeclaration b1 = (BodyDeclaration) object1;
	 * BodyDeclaration b2 = (BodyDeclaration) object2;
	 * Integer i1 = (Integer) b1.getProperty(RELATIVE_ORDER);
	 * Integer i2 = (Integer) b2.getProperty(RELATIVE_ORDER);
	 * return i1.intValue() - i2.intValue(); // preserve original order
	 * </pre>
	 * </p>
	 *
	 * @see org.eclipse.wst.jsdt.core.dom.BodyDeclaration
	 */
	public static final String RELATIVE_ORDER = "relativeOrder"; //$NON-NLS-1$

	/**
     * Reorders the declarations in the given javascript unit  according to
     * the specified AST level. The caller is responsible for arranging in
     * advance that the given javascript unit  is a working copy, and for
     * saving the changes afterwards.
     * <p>
     * <b>Note:</b> Reordering the members within a type declaration might be
     * more than a cosmetic change and could have potentially serious
     * repercussions. Firstly, the order in which the fields of a type are
     * initialized is significant in the JavaScript language; reordering fields
     * and initializers may result in compilation errors or change the execution
     * behavior of the code. Secondly, reordering a class's members may affect
     * how its instances are serialized. This operation should therefore be used
     * with caution and due concern for potential negative side effects.
     * </p>
     * <p>
     * The optional <code>positions</code> array contains a non-decreasing
     * ordered list of character-based source positions within the compilation
     * unit's source code string. Upon return from this method, the positions in
     * the array reflect the corresponding new locations in the modified source
     * code string. Note that this operation modifies the given array in place.
     * </p>
     * <p>
     * The <code>compare</code> method of the given comparator is passed pairs
     * of body declarations (subclasses of <code>BodyDeclaration</code>)
     * representing body declarations at the same level. The nodes are from an
     * AST of the specified level
     * ({@link org.eclipse.wst.jsdt.core.dom.ASTParser#newParser(int)}. Clients
     * will generally specify AST.JLS3 since that will cover all constructs found
     * in JavaScript 1.0, 1.1, 1.2, 1.3, 1.4, and 1.5 source code.
     * The comparator is called on body declarations of nested classes, including
     * anonymous and local classes, but always at the same level. Clients need to provide
     * a comparator implementation (there is no standard comparator). The
     * <code>RELATIVE_ORDER</code> property attached to these AST nodes afforts
     * the comparator a way to preserve the original relative order.
     * </p>
     * <p>
     * The body declarations passed as parameters to the comparator
     * always carry at least the following minimal signature information:
     * <br>
     * <table border="1" width="80%" cellpadding="5">
     *    <tr>
     *      <td width="20%"><code>TypeDeclaration</code></td>
     *      <td width="50%"><code>modifiers, isInterface, name, superclass,
     *        superInterfaces, typeParameters<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     *    <tr>
     *      <td width="20%"><code>FieldDeclaration</code></td>
     *      <td width="50%"><code>modifiers, type, fragments
     *        (VariableDeclarationFragments
     *        with name only)<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     *    <tr>
     *      <td width="20%"><code>FunctionDeclaration</code></td>
     *      <td width="50%"><code>modifiers, isConstructor, returnType, name,
     *        typeParameters, parameters
     *        (SingleVariableDeclarations with name, type, and modifiers only),
     *        thrownExceptions<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     *    <tr>
     *      <td width="20%"><code>Initializer</code></td>
     *      <td width="50%"><code>modifiers<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     *    <tr>
     *      <td width="20%"><code>AnnotationTypeDeclaration</code></td>
     *      <td width="50%"><code>modifiers, name<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     *    <tr>
     *      <td width="20%"><code>AnnotationTypeMemberDeclaration</code></td>
     *      <td width="50%"><code>modifiers, name, type, default<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     *    <tr>
     *      <td width="20%"><code>EnumDeclaration</code></td>
     *      <td width="50%"><code>modifiers, name, superInterfaces<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     *    <tr>
     *      <td width="20%"><code>EnumConstantDeclaration</code></td>
     *      <td width="50%"><code>modifiers, name, arguments<br>
     *        RELATIVE_ORDER property</code></td>
     *    </tr>
     * </table>
     * Clients should not rely on the AST nodes being properly parented or on
     * having source range information. (Future releases may provide options
     * for requesting additional information like source positions, full ASTs,
     * non-recursive sorting, etc.)
     * </p>
     *
     * @param level the AST level; one of the AST LEVEL constants
     * @param compilationUnit the given javascript unit , which must be a
     * working copy
     * @param positions an array of source positions to map, or
     * <code>null</code> if none. If supplied, the positions must
     * character-based source positions within the original source code for
     * the given javascript unit , arranged in non-decreasing order.
     * The array is updated in place when this method returns to reflect the
     * corresponding source positions in the permuted source code string
     * (but not necessarily any longer in non-decreasing order).
     * @param comparator the comparator capable of ordering
     *   <code>BodyDeclaration</code>s; this comparator is passed AST nodes
     *   from an AST of the specified AST level
     * @param options bitwise-or of option flags; <code>0</code> for default
     * behavior (reserved for future growth)
     * @param monitor the progress monitor to notify, or <code>null</code> if
     * none
     * @exception JavaScriptModelException if the javascript unit  could not be
     * sorted. Reasons include:
     * <ul>
     * <li> The given javascript unit  does not exist (ELEMENT_DOES_NOT_EXIST)</li>
     * <li> The given javascript unit  is not a working copy (INVALID_ELEMENT_TYPES)</li>
     * <li> A <code>CoreException</code> occurred while accessing the underlying
     * resource
     * </ul>
     * @exception IllegalArgumentException if the given javascript unit  is null
     * or if the given comparator is null, or if <code>level</code> is not one of
     * the AST JLS level constants.
     * @see org.eclipse.wst.jsdt.core.dom.BodyDeclaration
     * @see #RELATIVE_ORDER
     *  
     */
    public static void sort(int level, IJavaScriptUnit compilationUnit,
            int[] positions,
            Comparator comparator,
            int options,
            IProgressMonitor monitor) throws JavaScriptModelException {
        if (compilationUnit == null || comparator == null) {
            throw new IllegalArgumentException();
        }
        checkASTLevel(level);
        IJavaScriptUnit[] compilationUnits = new IJavaScriptUnit[] { compilationUnit };
        SortElementsOperation operation = new SortElementsOperation(level, compilationUnits, positions, comparator);
        operation.runOperation(monitor);
    }

	/**
	 * Reorders the declarations in the given javascript unit  according to the
	 * specified comparator. The caller is responsible for arranging in advance
	 * that the given javascript unit  is a working copy, and for applying the
	 * returned TextEdit afterwards.
	 * <p>
	 * <b>Note:</b> Reordering the members within a type declaration might be
	 * more than a cosmetic change and could have potentially serious
	 * repercussions. Firstly, the order in which the fields of a type are
	 * initialized is significant in the JavaScript language; reordering fields and
	 * initializers may result in compilation errors or change the execution
	 * behavior of the code. Secondly, reordering a class's members may affect
	 * how its instances are serialized. This operation should therefore be used
	 * with caution and due concern for potential negative side effects.
	 * </p>
	 * <p>
	 * The <code>compare</code> method of the given comparator is passed pairs
	 * of body declarations (subclasses of <code>BodyDeclaration</code>)
	 * representing body declarations at the same level.
	 * The comparator is called on body declarations of nested classes,
	 * including anonymous and local classes, but always at the same level.
	 * Clients need to provide a comparator implementation (there is no standard
	 * comparator). The <code>RELATIVE_ORDER</code> property attached to these
	 * AST nodes affords the comparator a way to preserve the original relative
	 * order.
	 * </p>
	 * <p>
	 * The body declarations passed as parameters to the comparator always carry
	 * at least the following minimal signature information: <br>
	 * <table border="1" width="80%" cellpadding="5">
	 * <tr>
	 * <td width="20%"><code>TypeDeclaration</code></td>
	 * <td width="50%"><code>modifiers, isInterface, name, superclass,
	 *        superInterfaces, typeParameters<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * <tr>
	 * <td width="20%"><code>FieldDeclaration</code></td>
	 * <td width="50%"><code>modifiers, type, fragments
	 *        (VariableDeclarationFragments
	 *        with name only)<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * <tr>
	 * <td width="20%"><code>FunctionDeclaration</code></td>
	 * <td width="50%"><code>modifiers, isConstructor, returnType, name,
	 *        typeParameters, parameters
	 *        (SingleVariableDeclarations with name, type, and modifiers only),
	 *        thrownExceptions<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * <tr>
	 * <td width="20%"><code>Initializer</code></td>
	 * <td width="50%"><code>modifiers<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * <tr>
	 * <td width="20%"><code>AnnotationTypeDeclaration</code></td>
	 * <td width="50%"><code>modifiers, name<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * <tr>
	 * <td width="20%"><code>AnnotationTypeMemberDeclaration</code></td>
	 * <td width="50%"><code>modifiers, name, type, default<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * <tr>
	 * <td width="20%"><code>EnumDeclaration</code></td>
	 * <td width="50%"><code>modifiers, name, superInterfaces<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * <tr>
	 * <td width="20%"><code>EnumConstantDeclaration</code></td>
	 * <td width="50%"><code>modifiers, name, arguments<br>
	 *        RELATIVE_ORDER property</code></td>
	 * </tr>
	 * </table>
	 * </p>
	 *
	 * @param unit
	 *            the JavaScriptUnit to sort
	 * @param comparator
	 *            the comparator capable of ordering
	 *            <code>BodyDeclaration</code>s; this comparator is passed
	 *            AST nodes from an AST of the specified AST level
	 * @param options
	 *            bitwise-or of option flags; <code>0</code> for default
	 *            behavior (reserved for future growth)
	 * @param group
	 *            the text edit group to use when generating text edits, or <code>null</code>
	 * @param monitor
	 *            the progress monitor to notify, or <code>null</code> if none
	 * @return a TextEdit describing the required edits to do the sort, or <code>null</code>
	 *            if sorting is not required
	 * @exception JavaScriptModelException
	 *                if the javascript unit  could not be sorted. Reasons
	 *                include:
	 *                <ul>
	 *                <li> The given unit was not created from a IJavaScriptUnit (INVALID_ELEMENT_TYPES)</li>
	 *                </ul>
	 * @exception IllegalArgumentException
	 *                if the given javascript unit  is null or if the given
	 *                comparator is null, or if <code>options</code> is not one
	 *                of the supported levels.
	 * @see org.eclipse.wst.jsdt.core.dom.BodyDeclaration
	 * @see #RELATIVE_ORDER
	 *  
	 */
	public static TextEdit sort(JavaScriptUnit unit,
			Comparator comparator,
			int options,
			TextEditGroup group,
			IProgressMonitor monitor) throws JavaScriptModelException {
		if (unit == null || comparator == null) {
			throw new IllegalArgumentException();
		}
		SortElementsOperation operation = new SortElementsOperation(AST.JLS3, new IJavaScriptElement[] { unit.getJavaElement() }, null, comparator);
		return operation.calculateEdit(unit, group);
	}
}
