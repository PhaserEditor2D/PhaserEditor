/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;

import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.parser.Scanner;
import org.eclipse.wst.jsdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.wst.jsdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.wst.jsdt.internal.core.ClasspathEntry;
import org.eclipse.wst.jsdt.internal.core.JavaModelStatus;
import org.eclipse.wst.jsdt.internal.core.util.Messages;

/**
 * Provides methods for checking JavaScript-specific conventions such as name syntax.
 * <p>
 * This class provides static methods and constants only; it is not intended to be
 * instantiated or subclassed by clients.
 * </p>
  *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
*/
public final class JavaScriptConventions {

	private static final char DOT= '.';
	private static final Scanner SCANNER = new Scanner(false /*comment*/, true /*whitespace*/, false /*nls*/, ClassFileConstants.JDK1_3 /*sourceLevel*/, null/*taskTag*/, null/*taskPriorities*/, true /*taskCaseSensitive*/);

	private JavaScriptConventions() {
		// Not instantiable
	}

	/*
	 * Returns the current identifier extracted by the scanner (without unicode
	 * escapes) from the given id and for the given source and compliance levels.
	 * Returns <code>null</code> if the id was not valid
	 */
	private static synchronized char[] scannedIdentifier(String id, String sourceLevel, String complianceLevel) {
		if (id == null) {
			return null;
		}
		// Set scanner for given source and compliance levels
		SCANNER.sourceLevel = sourceLevel == null ? ClassFileConstants.JDK1_3 : CompilerOptions.versionToJdkLevel(sourceLevel);
		SCANNER.complianceLevel = complianceLevel == null ? ClassFileConstants.JDK1_3 : CompilerOptions.versionToJdkLevel(complianceLevel);

		try {
			SCANNER.setSource(id.toCharArray());
			int token = SCANNER.scanIdentifier();
			if (token != TerminalTokens.TokenNameIdentifier) return null;
			if (SCANNER.currentPosition == SCANNER.eofPosition) { // to handle case where we had an ArrayIndexOutOfBoundsException
				try {
					return SCANNER.getCurrentIdentifierSource();
				} catch (ArrayIndexOutOfBoundsException e) {
					return null;
				}
			} else {
				return null;
			}
		}
		catch (InvalidInputException e) {
			return null;
		}
	}

	/**
	 * Validate the given javaScript unit name.
	 * <p>
	 * A javaScript unit name must obey the following rules:
	 * <ul>
	 * <li> it must not be null
	 * <li> it must be suffixed by a dot ('.') followed by one of the
	 *       {@link JavaScriptCore#getJavaScriptLikeExtensions() JavaScript-like extensions}
	 * <li> its prefix must be a valid identifier
	 * <li> it must not contain any characters or substrings that are not valid
	 *		   on the file system on which workspace root is located.
	 * </ul>
	 * </p>
	 * @param name the name of a javaScript unit
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as a javaScript unit name, otherwise a status
	 *		object indicating what is wrong with the name
	 * @deprecated Use {@link #validateCompilationUnitName(String id, String sourceLevel, String complianceLevel)} instead
	 */
	public static IStatus validateCompilationUnitName(String name) {
		return validateCompilationUnitName(name,CompilerOptions.VERSION_1_3,CompilerOptions.VERSION_1_3);
	}

	/**
	 * Validate the given javaScript unit name for the given source and compliance levels.
	 * <p>
	 * A javaScript unit name must obey the following rules:
	 * <ul>
	 * <li> it must not be null
	 * <li> it must be suffixed by a dot ('.') followed by one of the
	 *       {@link JavaScriptCore#getJavaScriptLikeExtensions() JavaScript-like extensions}
	 * <li> its prefix must be a valid identifier
	 * <li> it must not contain any characters or substrings that are not valid
	 *		   on the file system on which workspace root is located.
	 * </ul>
	 * </p>
	 * @param name the name of a javaScript unit
	 * @param sourceLevel the source level
	 * @param complianceLevel the compliance level
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as a javaScript unit name, otherwise a status
	 *		object indicating what is wrong with the name
	 */
	public static IStatus validateCompilationUnitName(String name, String sourceLevel, String complianceLevel) {
		if (name == null) {
			return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_unit_nullName, null);
		}
		if (!org.eclipse.wst.jsdt.internal.core.util.Util.isJavaLikeFileName(name)) {
			return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_unit_notJavaName, null);
		}
//		String identifier;
		int index;
		index = name.lastIndexOf('.');
		if (index == -1) {
			return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_unit_notJavaName, null);
		}
//		identifier = name.substring(0, index);
		// JSR-175 metadata strongly recommends "package-info.js" as the
		// file in which to store package annotations and
		// the package-level spec (replaces package.html)
//		if (!identifier.equals(PACKAGE_INFO)) {
//			IStatus status = validateIdentifier(identifier, sourceLevel, complianceLevel);
//			if (!status.isOK()) {
//				return status;
//			}
//		}
//		IStatus status = ResourcesPlugin.getWorkspace().validateName(name, IResource.FILE);
//		if (!status.isOK()) {
//			return status;
//		}
		return JavaModelStatus.VERIFIED_OK;
	}

	/*
	 * Validate the given .class file name for the given source and compliance levels.
	 * <p>
	 * A .class file name must obey the following rules:
	 * <ul>
	 * <li> it must not be null
	 * <li> it must include the <code>".class"</code> suffix
	 * <li> its prefix must be a valid identifier
	 * <li> it must not contain any characters or substrings that are not valid
	 *		   on the file system on which workspace root is located.
	 * </ul>
	 * </p>
	 * @param name the name of a .class file
	 * @param sourceLevel the source level
	 * @param complianceLevel the compliance level
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as a .class file name, otherwise a status
	 *		object indicating what is wrong with the name
	 */
	public static IStatus validateClassFileName(String name, String sourceLevel, String complianceLevel) {
		if (name == null) {
			return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_classFile_nullName, null);		}
		if (!org.eclipse.wst.jsdt.internal.compiler.util.Util.isClassFileName(name)) {
			return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_classFile_notClassFileName, null);
		}
//		String identifier;
		int index;
		index = name.lastIndexOf('.');
		if (index == -1) {
			return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_classFile_notClassFileName, null);
		}
//		identifier = name.substring(0, index);
		// JSR-175 metadata strongly recommends "package-info.js" as the
		// file in which to store package annotations and
		// the package-level spec (replaces package.html)
//		if (!identifier.equals(PACKAGE_INFO)) {
//			IStatus status = validateIdentifier(identifier, sourceLevel, complianceLevel);
//			if (!status.isOK()) {
//				return status;
//			}
//		}
		IStatus status = ResourcesPlugin.getWorkspace().validateName(name, IResource.FILE);
		if (!status.isOK()) {
			return status;
		}
		return JavaModelStatus.VERIFIED_OK;
	}

	/**
	 * Validate the given var or field  name.
	 * <p>
	 * Syntax of a field name corresponds to VariableDeclaratorId (JLS2 8.3).
	 * For example, <code>"x"</code>.
	 *
	 * @param name the name of a field
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as a field name, otherwise a status
	 *		object indicating what is wrong with the name
	 * @deprecated Use {@link #validateFieldName(String id, String sourceLevel, String complianceLevel)} instead
	 */
	public static IStatus validateFieldName(String name) {
		return validateIdentifier(name, CompilerOptions.VERSION_1_3,CompilerOptions.VERSION_1_3);
	}

	/**
	 * Validate the given var or field name for the given source and compliance levels.
	 * <p>
	 * Syntax of a field name corresponds to VariableDeclaratorId (JLS2 8.3).
	 * For example, <code>"x"</code>.
	 *
	 * @param name the name of a field
	 * @param sourceLevel the source level
	 * @param complianceLevel the compliance level
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as a field name, otherwise a status
	 *		object indicating what is wrong with the name
	 */
	public static IStatus validateFieldName(String name, String sourceLevel, String complianceLevel) {
		return validateIdentifier(name, sourceLevel, complianceLevel);
	}

	/**
	 * Validate the given JavaScript identifier.
	 * The identifier must not have the same spelling as a JavaScript keyword,
	 * boolean literal (<code>"true"</code>, <code>"false"</code>), or null literal (<code>"null"</code>).
	 * A valid identifier can act as a simple type name, method name or field name.
	 *
	 * @param id the JavaScript identifier
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given identifier is a valid JavaScript identifier, otherwise a status
	 *		object indicating what is wrong with the identifier
	 * @deprecated Use {@link #validateIdentifier(String id, String sourceLevel, String complianceLevel)} instead
	 */
	public static IStatus validateIdentifier(String id) {
		return validateIdentifier(id,CompilerOptions.VERSION_1_3,CompilerOptions.VERSION_1_3);
	}

	/**
	 * Validate the given JavaScript identifier for the given source and compliance levels
	 * The identifier must not have the same spelling as a JavaScript keyword,
	 * boolean literal (<code>"true"</code>, <code>"false"</code>), or null literal (<code>"null"</code>).
	 * A valid identifier can act as a simple type name, method name or field name.
	 *
	 * @param id the JavaScript identifier
	 * @param sourceLevel the source level
	 * @param complianceLevel the compliance level
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given identifier is a valid JavaScript identifier, otherwise a status
	 *		object indicating what is wrong with the identifier
	 */
	public static IStatus validateIdentifier(String id, String sourceLevel, String complianceLevel) {
		if (scannedIdentifier(id, sourceLevel, complianceLevel) != null) {
			return JavaModelStatus.VERIFIED_OK;
		} else {
			return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.bind(Messages.convention_illegalIdentifier, id), null);
		}
	}

	/**
	 * Validate the given import declaration name for the given source and compliance levels.
	 * <p>
	 * The name of an import corresponds to a fully qualified type name.
	 *
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @param name the import declaration
	 * @param sourceLevel the source level
	 * @param complianceLevel the compliance level
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as an import declaration, otherwise a status
	 *		object indicating what is wrong with the name
	 */
	public static IStatus validateImportDeclaration(String name, String sourceLevel, String complianceLevel) {
		if (name == null || name.length() == 0) {
			return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_import_nullImport, null);
		}
		if (name.charAt(name.length() - 1) == '*') {
			if (name.charAt(name.length() - 2) == '.') {
				return validatePackageName(name.substring(0, name.length() - 2), sourceLevel, complianceLevel);
			} else {
				return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_import_unqualifiedImport, null);
			}
		}
		return validatePackageName(name, sourceLevel, complianceLevel);
	}

	/**
	 * Validate the given JavaScript type name, either simple or qualified.
	 * <p>
	 *
	 * @param name the name of a type
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as a JavaScript type name,
	 *      a status with code <code>IStatus.WARNING</code>
	 *		indicating why the given name is discouraged,
	 *      otherwise a status object indicating what is wrong with
	 *      the name
	 * @deprecated Use {@link #validateJavaScriptTypeName(String id, String sourceLevel, String complianceLevel)} instead
	 */
	public static IStatus validateJavaScriptTypeName(String name) {
		return validateJavaScriptTypeName(name, CompilerOptions.VERSION_1_3,CompilerOptions.VERSION_1_3);
	}

	/**
	 * Validate the given JavaScript type name, either simple or qualified, for the given source and compliance levels.
	 * <p>
	 *
	 * @param name the name of a type
	 * @param sourceLevel the source level
	 * @param complianceLevel the compliance level
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as a JavaScript type name,
	 *      a status with code <code>IStatus.WARNING</code>
	 *		indicating why the given name is discouraged,
	 *      otherwise a status object indicating what is wrong with
	 *      the name
	 */
	public static IStatus validateJavaScriptTypeName(String name, String sourceLevel, String complianceLevel) {
		if (name == null) {
			return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_type_nullName, null);
		}
		String trimmed = name.trim();
		if (!name.equals(trimmed)) {
			return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_type_nameWithBlanks, null);
		}
		int index = name.lastIndexOf('.');
		char[] scannedID;
		if (index == -1) {
			// simple name
			scannedID = scannedIdentifier(name, sourceLevel, complianceLevel);
		} else {
			// qualified name
			String pkg = name.substring(0, index).trim();
			IStatus status = validatePackageName(pkg, sourceLevel, complianceLevel);
			if (!status.isOK()) {
				return status;
			}
			String type = name.substring(index + 1).trim();
			scannedID = scannedIdentifier(type, sourceLevel, complianceLevel);
		}

		if (scannedID != null) {
			IStatus status = ResourcesPlugin.getWorkspace().validateName(new String(scannedID), IResource.FILE);
			if (!status.isOK()) {
				return status;
			}
			if ((scannedID.length > 0 && ScannerHelper.isLowerCase(scannedID[0]))) {
				return new Status(IStatus.WARNING, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_type_lowercaseName, null);
			}
			return JavaModelStatus.VERIFIED_OK;
		} else {
			return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.bind(Messages.convention_type_invalidName, name), null);
		}
	}

	/**
	 * Validate the given function name.
	 * <p>
	 *
	 * @param name the name of a method
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as a method name, otherwise a status
	 *		object indicating what is wrong with the name
	 * @deprecated Use {@link #validateFunctionName(String id, String sourceLevel, String complianceLevel)} instead
	 */
	public static IStatus validateFunctionName(String name) {
		return validateFunctionName(name, CompilerOptions.VERSION_1_3,CompilerOptions.VERSION_1_3);
	}


	/**
	 * Validate the given function name for the given source and compliance levels.
	 * <p>
	 *
	 * @param name the name of a method
	 * @param sourceLevel the source level
	 * @param complianceLevel the compliance level
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as a method name, otherwise a status
	 *		object indicating what is wrong with the name
	 */
	public static IStatus validateFunctionName(String name, String sourceLevel, String complianceLevel) {
		return validateIdentifier(name, sourceLevel,complianceLevel);
	}

	/**
	 * Validate the given package name.
	 * <p>
	 * The syntax of a package name corresponds to PackageName as
	 * defined by PackageDeclaration.
	 * <p>
	 * Note that the given name must be a non-empty package name (that is, attempting to
	 * validate the default package will return an error status.)
	 * Also it must not contain any characters or substrings that are not valid
	 * on the file system on which workspace root is located.
	 *
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @param name the name of a package
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as a package name, otherwise a status
	 *		object indicating what is wrong with the name
	 * @deprecated Use {@link #validatePackageName(String id, String sourceLevel, String complianceLevel)} instead
	 */
	public static IStatus validatePackageName(String name) {
		return validatePackageName(name, CompilerOptions.VERSION_1_3,CompilerOptions.VERSION_1_3);
	}

	/**
	 * Validate the given package name for the given source and compliance levels.
	 * <p>
	 * The syntax of a package name corresponds to PackageName as
	 * defined by PackageDeclaration.
	 * <p>
	 * Note that the given name must be a non-empty package name (that is, attempting to
	 * validate the default package will return an error status.)
	 * Also it must not contain any characters or substrings that are not valid
	 * on the file system on which workspace root is located.
	 *
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @param name the name of a package
	 * @param sourceLevel the source level
	 * @param complianceLevel the compliance level
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as a package name, otherwise a status
	 *		object indicating what is wrong with the name
	 */
	public static IStatus validatePackageName(String name, String sourceLevel, String complianceLevel) {

		if (name == null) {
			return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_package_nullName, null);
		}
		int length;
		if ((length = name.length()) == 0) {
			return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_package_emptyName, null);
		}
		if (name.charAt(0) == DOT || name.charAt(length-1) == DOT) {
			return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_package_dotName, null);
		}
		if (CharOperation.isWhitespace(name.charAt(0)) || CharOperation.isWhitespace(name.charAt(name.length() - 1))) {
			return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_package_nameWithBlanks, null);
		}
		int dot = 0;
		while (dot != -1 && dot < length-1) {
			if ((dot = name.indexOf(DOT, dot+1)) != -1 && dot < length-1 && name.charAt(dot+1) == DOT) {
				return new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_package_consecutiveDotsName, null);
				}
		}
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IStatus status = workspace.validateName(new String(name), IResource.FOLDER);
		if (!status.isOK()) {
			return status;
		}
		StringTokenizer st = new StringTokenizer(name, "."); //$NON-NLS-1$
		boolean firstToken = true;
		IStatus warningStatus = null;
		while (st.hasMoreTokens()) {
			String typeName = st.nextToken();
			typeName = typeName.trim(); // grammar allows spaces
			char[] scannedID = scannedIdentifier(typeName, sourceLevel, complianceLevel);
			if (scannedID == null) {
				return new Status(IStatus.WARNING, JavaScriptCore.PLUGIN_ID, -1, Messages.bind(Messages.convention_illegalIdentifier, typeName), null);
			}
			status = workspace.validateName(new String(name), IResource.FOLDER);
			if (!status.isOK()) {
				return status;
			}
			if (firstToken && scannedID.length > 0 && ScannerHelper.isUpperCase(scannedID[0])) {
				if (warningStatus == null) {
					warningStatus = new Status(IStatus.WARNING, JavaScriptCore.PLUGIN_ID, -1, Messages.convention_package_uppercaseName, null);
				}
			}
			firstToken = false;
		}
		if (warningStatus != null) {
			return warningStatus;
		}
		return JavaModelStatus.VERIFIED_OK;
	}

	/**
	 * Validate a given includepath location for a project, using the following rules:
	 * <ul>
	 *   <li> Includepath entries cannot collide with each other; that is, all entry paths must be unique.
	 *   <li> A project entry cannot refer to itself directly (that is, a project cannot prerequisite itself).
     *   <li> Includepath entries cannot coincidate or be nested in each other, except for the following scenarii listed below:
	 *              <li> A source/library folder can be nested in any source folder as long as the nested folder is excluded from the enclosing one. </li>
	 *      </ul>
	 * </ul>
	 *
	 *  Note that the includepath entries are not validated automatically. Only bound variables or containers are considered
	 *  in the checking process (this allows to perform a consistency check on a includepath which has references to
	 *  yet non existing projects, folders, ...).
	 *  <p>
	 *  This validation is intended to anticipate includepath issues prior to assigning it to a project. In particular, it will automatically
	 *  be performed during the includepath setting operation (if validation fails, the includepath setting will not complete).
	 *  <p>
	 * @param javaProject the given javaScript project
	 * @param rawClasspath the given includepath
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given includepath are compatible, otherwise a status
	 *		object indicating what is wrong with the includepath
	 */
	public static IJavaScriptModelStatus validateClasspath(IJavaScriptProject javaProject, IIncludePathEntry[] rawClasspath) {

		return ClasspathEntry.validateClasspath(javaProject, rawClasspath);
	}

	/**
	 * Returns a JavaScript model status describing the problem related to this includepath entry if any,
	 * a status object with code <code>IStatus.OK</code> if the entry is fine (that is, if the
	 * given includepath entry denotes a valid element to be referenced onto a includepath).
	 *
	 * @param project the given javaScript project
	 * @param entry the given includepath entry
	 * @param checkSourceAttachment a flag to determine if source attachement should be checked
	 * @return a javaScript model status describing the problem related to this includepath entry if any, a status object with code <code>IStatus.OK</code> if the entry is fine
	 */
	public static IJavaScriptModelStatus validateClasspathEntry(IJavaScriptProject project, IIncludePathEntry entry, boolean checkSourceAttachment){
		IJavaScriptModelStatus status = ClasspathEntry.validateClasspathEntry(project, entry, checkSourceAttachment, true/*recurse in container*/);
		if (status.getCode() == IJavaScriptModelStatusConstants.INVALID_INCLUDEPATH && ((ClasspathEntry) entry).isOptional())
			return JavaModelStatus.VERIFIED_OK;
		return status;
	}

	/**
	 * Validate the given type variable name.
	 * <p>
	 *
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @param name the name of a type variable
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as a type variable name, otherwise a status
	 *		object indicating what is wrong with the name
	 * @deprecated Use {@link #validateTypeVariableName(String id, String sourceLevel, String complianceLevel)} instead
	 */
	public static IStatus validateTypeVariableName(String name) {
		return validateIdentifier(name, CompilerOptions.VERSION_1_3,CompilerOptions.VERSION_1_3);
	}

	/**
	 * Validate the given type variable name for the given source and compliance levels.
	 * <p>
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @param name the name of a type variable
	 * @param sourceLevel the source level
	 * @param complianceLevel the compliance level
	 * @return a status object with code <code>IStatus.OK</code> if
	 *		the given name is valid as a type variable name, otherwise a status
	 *		object indicating what is wrong with the name
	 */
	public static IStatus validateTypeVariableName(String name, String sourceLevel, String complianceLevel) {
		return validateIdentifier(name, sourceLevel, complianceLevel);
	}

	/*
	 * Validate that all compiler options of the given project match keys and values
	 * described in {@link JavaScriptCore#getDefaultOptions()} method.
	 *
	 * @param javaProject the given javaScript project
	 * @param inheritJavaCoreOptions inherit project options from JavaScriptCore or not.
	 * @return a status object with code <code>IStatus.OK</code> if all project
	 *		compiler options are valid, otherwise a status object indicating what is wrong
	 *		with the keys and their value.
	 */
	/*
	public static IStatus validateCompilerOptions(IJavaScriptProject javaProject, boolean inheritJavaCoreOptions)	  {
		return validateCompilerOptions(javaProject.getOptions(inheritJavaCoreOptions));
	}
	*/

	/*
	 * Validate that all compiler options of the given project match keys and values
	 * described in {@link JavaScriptCore#getDefaultOptions()} method.
	 *
	 * @param compilerOptions Map of options
	 * @return a status object with code <code>IStatus.OK</code> if all
	 *		compiler options are valid, otherwise a status object indicating what is wrong
	 *		with the keys and their value.
	 */
	/*
	public static IStatus validateCompilerOptions(Map compilerOptions)	  {

		// Get current options
		String compliance = (String) compilerOptions.get(JavaScriptCore.COMPILER_COMPLIANCE);
		String source = (String) compilerOptions.get(JavaScriptCore.COMPILER_SOURCE);
		String target = (String) compilerOptions.get(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM);
		if (compliance == null && source == null && target == null) {
			return JavaModelStatus.VERIFIED_OK; // default is OK
		}

		// Initialize multi-status
		List errors = new ArrayList();

		// Set default for compliance if necessary (not set on project and not inherited...)
		if (compliance == null) {
			compliance = JavaScriptCore.getOption(JavaScriptCore.COMPILER_COMPLIANCE);
		}

		// Verify compliance level value and set source and target default if necessary
		long complianceLevel = 0;
		long sourceLevel = 0;
		long targetLevel = 0;
		if (JavaScriptCore.VERSION_1_3.equals(compliance)) {
			complianceLevel = ClassFileConstants.JDK1_3;
			if (source == null) {
				source = JavaScriptCore.VERSION_1_3;
				sourceLevel = ClassFileConstants.JDK1_3;
			}
			if (target == null) {
				target = JavaScriptCore.VERSION_1_1;
				targetLevel = ClassFileConstants.JDK1_1;
			}
		} else if (JavaScriptCore.VERSION_1_4.equals(compliance)) {
			complianceLevel = ClassFileConstants.JDK1_4;
			if (source == null) {
				source = JavaScriptCore.VERSION_1_3;
				sourceLevel = ClassFileConstants.JDK1_3;
			}
			if (target == null) {
				target = JavaScriptCore.VERSION_1_2;
				targetLevel = ClassFileConstants.JDK1_2;
			}
		} else if (JavaScriptCore.VERSION_1_5.equals(compliance)) {
			complianceLevel = ClassFileConstants.JDK1_5;
			if (source == null) {
				source = JavaScriptCore.VERSION_1_5;
				sourceLevel = ClassFileConstants.JDK1_5;
			}
			if (target == null) {
				target = JavaScriptCore.VERSION_1_5;
				targetLevel = ClassFileConstants.JDK1_5;
			}
		} else {
			// compliance is not valid
			errors.add(new JavaModelStatus(IStatus.ERROR, Util.bind("convention.compiler.invalidCompilerOption", compliance==null?"":compliance, JavaScriptCore.COMPILER_COMPLIANCE))); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// Verify source value and set default for target if necessary
		 if (JavaScriptCore.VERSION_1_4.equals(source)) {
			sourceLevel = ClassFileConstants.JDK1_4;
			if (target == null) {
				target = JavaScriptCore.VERSION_1_4;
				targetLevel = ClassFileConstants.JDK1_4;
			}
		} else if (JavaScriptCore.VERSION_1_5.equals(source)) {
			sourceLevel = ClassFileConstants.JDK1_5;
			if (target == null) {
				target = JavaScriptCore.VERSION_1_5;
				targetLevel = ClassFileConstants.JDK1_5;
			}
		} else if (JavaScriptCore.VERSION_1_3.equals(source)) {
			sourceLevel = ClassFileConstants.JDK1_3;
		} else {
			// source is not valid
			errors.add(new JavaModelStatus(IStatus.ERROR, Util.bind("convention.compiler.invalidCompilerOption", source==null?"":source, JavaScriptCore.COMPILER_SOURCE))); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// Verify target value
		 if (targetLevel == 0) {
			 targetLevel = CompilerOptions.versionToJdkLevel(target);
			 if (targetLevel == 0) {
				// target is not valid
				errors.add(new JavaModelStatus(IStatus.ERROR, Util.bind("convention.compiler.invalidCompilerOption", target==null?"":target, JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM))); //$NON-NLS-1$ //$NON-NLS-2$
			 }
		}

		// Check and set compliance/source/target compatibilities (only if they have valid values)
		if (complianceLevel != 0 && sourceLevel != 0 && targetLevel != 0) {
			// target must be 1.5 if source is 1.5
			if (sourceLevel >= ClassFileConstants.JDK1_5 && targetLevel < ClassFileConstants.JDK1_5) {
				errors.add(new JavaModelStatus(IStatus.ERROR, Util.bind("convention.compiler.incompatibleTargetForSource", target, JavaScriptCore.VERSION_1_5))); //$NON-NLS-1$
			}
	   		else
		   		// target must be 1.4 if source is 1.4
	   			if (sourceLevel >= ClassFileConstants.JDK1_4 && targetLevel < ClassFileConstants.JDK1_4) {
					errors.add(new JavaModelStatus(IStatus.ERROR, Util.bind("convention.compiler.incompatibleTargetForSource", target, JavaScriptCore.VERSION_1_4))); //$NON-NLS-1$
	   		}
			// target cannot be greater than compliance level
			if (complianceLevel < targetLevel){
				errors.add(new JavaModelStatus(IStatus.ERROR, Util.bind("convention.compiler.incompatibleComplianceForTarget", compliance, JavaScriptCore.VERSION_1_4))); //$NON-NLS-1$
			}
			// compliance must be 1.5 if source is 1.5
			if (source.equals(JavaScriptCore.VERSION_1_5) && complianceLevel < ClassFileConstants.JDK1_5) {
				errors.add(new JavaModelStatus(IStatus.ERROR, Util.bind("convention.compiler.incompatibleComplianceForSource", compliance, JavaScriptCore.VERSION_1_5))); //$NON-NLS-1$
			} else
				// compliance must be 1.4 if source is 1.4
				if (source.equals(JavaScriptCore.VERSION_1_4) && complianceLevel < ClassFileConstants.JDK1_4) {
					errors.add(new JavaModelStatus(IStatus.ERROR, Util.bind("convention.compiler.incompatibleComplianceForSource", compliance, JavaScriptCore.VERSION_1_4))); //$NON-NLS-1$
			}
		}

		// Return status
		int size = errors.size();
		switch (size) {
			case 0:
				return JavaModelStatus.VERIFIED_OK;
			case 1:
				return (IStatus) errors.get(0);
			default:
				IJavaScriptModelStatus[] allStatus = new IJavaScriptModelStatus[size];
				errors.toArray(allStatus);
				return JavaModelStatus.newMultiStatus(allStatus);
		}
	}
	*/
}
