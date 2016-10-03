/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     IBM Corporation - added the following constants:
 *                                 COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE
 *                                 COMPILER_PB_STATIC_ACCESS_RECEIVER
 *                                 COMPILER_TASK_TAGS
 *                                 CORE_CIRCULAR_CLASSPATH
 *                                 CORE_INCOMPLETE_CLASSPATH
 *     IBM Corporation - added run(IWorkspaceRunnable, IProgressMonitor)
 *     IBM Corporation - added exclusion patterns to source includepath entries
 *     IBM Corporation - added specific output location to source includepath entries
 *     IBM Corporation - added the following constants:
 *                                 CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER
 *                                 CORE_JAVA_BUILD_RECREATE_MODIFIED_CLASS_FILES_IN_OUTPUT_FOLDER
 *                                 CLEAN
 *     IBM Corporation - added getJsGlobalScopeContainerInitializer(String)
 *     IBM Corporation - added the following constants:
 *                                 CODEASSIST_ARGUMENT_PREFIXES
 *                                 CODEASSIST_ARGUMENT_SUFFIXES
 *                                 CODEASSIST_FIELD_PREFIXES
 *                                 CODEASSIST_FIELD_SUFFIXES
 *                                 CODEASSIST_LOCAL_PREFIXES
 *                                 CODEASSIST_LOCAL_SUFFIXES
 *                                 CODEASSIST_STATIC_FIELD_PREFIXES
 *                                 CODEASSIST_STATIC_FIELD_SUFFIXES
 *                                 COMPILER_PB_CHAR_ARRAY_IN_STRING_CONCATENATION
 *     IBM Corporation - added the following constants:
 *                                 COMPILER_PB_LOCAL_VARIABLE_HIDING
 *                                 COMPILER_PB_SPECIAL_PARAMETER_HIDING_FIELD
 *                                 COMPILER_PB_FIELD_HIDING
 *                                 COMPILER_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT
 *                                 CORE_INCOMPATIBLE_JDK_LEVEL
 *                                 VERSION_1_5
 *                                 COMPILER_PB_EMPTY_STATEMENT
 *     IBM Corporation - added the following constants:
 *                                 COMPILER_PB_INDIRECT_STATIC_ACCESS
 *                                 COMPILER_PB_BOOLEAN_METHOD_THROWING_EXCEPTION
 *                                 COMPILER_PB_UNNECESSARY_CAST
 *     IBM Corporation - added the following constants:
 *                                 COMPILER_PB_INVALID_JAVADOC
 *                                 COMPILER_PB_INVALID_JAVADOC_TAGS
 *                                 COMPILER_PB_INVALID_JAVADOC_TAGS_VISIBILITY
 *                                 COMPILER_PB_MISSING_JAVADOC_TAGS
 *                                 COMPILER_PB_MISSING_JAVADOC_TAGS_VISIBILITY
 *                                 COMPILER_PB_MISSING_JAVADOC_TAGS_OVERRIDING
 *                                 COMPILER_PB_MISSING_JAVADOC_COMMENTS
 *                                 COMPILER_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY
 *                                 COMPILER_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING
 *                                 COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD
 *                                 COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING
 *     IBM Corporation - added the following constants:
 *                                 TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC
 *     IBM Corporation - added the following constants:
 *                                 COMPILER_PB_FALLTHROUGH_CASE
 *                                 COMPILER_PB_PARAMETER_ASSIGNMENT
 *                                 COMPILER_PB_NULL_REFERENCE
 *     IBM Corporation - added the following constants:
 *                                 CODEASSIST_DEPRECATION_CHECK
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.core.search.TypeNameRequestor;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.util.SuffixConstants;
import org.eclipse.wst.jsdt.internal.core.BatchOperation;
import org.eclipse.wst.jsdt.internal.core.ClasspathAccessRule;
import org.eclipse.wst.jsdt.internal.core.ClasspathAttribute;
import org.eclipse.wst.jsdt.internal.core.ClasspathEntry;
import org.eclipse.wst.jsdt.internal.core.CreateTypeHierarchyOperation;
import org.eclipse.wst.jsdt.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.wst.jsdt.internal.core.JavaModel;
import org.eclipse.wst.jsdt.internal.core.JavaModelManager;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.Region;
import org.eclipse.wst.jsdt.internal.core.SetContainerOperation;
import org.eclipse.wst.jsdt.internal.core.SetVariablesOperation;
import org.eclipse.wst.jsdt.internal.core.UserLibraryManager;
import org.eclipse.wst.jsdt.internal.core.builder.JavaBuilder;
import org.eclipse.wst.jsdt.internal.core.builder.State;
import org.eclipse.wst.jsdt.internal.core.util.MementoTokenizer;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.core.util.Util;
import org.osgi.framework.BundleContext;

/**
 * The plug-in runtime class for the JavaScript model plug-in containing the core
 * (UI-free) support for JavaScript projects.
 * <p>
 * Like all plug-in runtime classes (subclasses of <code>Plugin</code>), this
 * class is automatically instantiated by the platform when the plug-in gets
 * activated. Clients must not attempt to instantiate plug-in runtime classes
 * directly.
 * </p>
 * <p>
 * The single instance of this class can be accessed from any plug-in declaring
 * the JavaScript model plug-in as a prerequisite via
 * <code>JavaScriptCore.getJavaCore()</code>. The JavaScript model plug-in will be activated
 * automatically if not already active.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public final class JavaScriptCore extends Plugin {

	public static final boolean IS_ECMASCRIPT4=false;

	private static final IResource[] NO_GENERATED_RESOURCES = new IResource[0];


	private static Plugin JAVA_CORE_PLUGIN = null;
	/**
	 * The plug-in identifier of the JavaScript core support
	 * (value <code>"org.eclipse.wst.jsdt.core"</code>).
	 */
	public static final String PLUGIN_ID = "org.eclipse.wst.jsdt.core" ; //$NON-NLS-1$

	/**
	 * The identifier for the JavaScript validator
	 * (value <code>"org.eclipse.wst.jsdt.core.javascriptValidator"</code>).
	 */
	public static final String BUILDER_ID = PLUGIN_ID + ".javascriptValidator" ; //$NON-NLS-1$

	/**
	 * The identifier for the JavaScript model
	 * (value <code>"org.eclipse.wst.jsdt.core.jsmodel"</code>).
	 */
	public static final String MODEL_ID = PLUGIN_ID + ".jsmodel" ; //$NON-NLS-1$

	/**
	 * The identifier for the JavaScript nature
	 * (value <code>"org.eclipse.wst.jsdt.core.jsnature"</code>).
	 * The presence of this nature on a project indicates that it is
	 * JavaScript-capable.
	 *
	 * @see org.eclipse.core.resources.IProject#hasNature(java.lang.String)
	 */
	public static final String NATURE_ID = PLUGIN_ID + ".jsNature" ; //$NON-NLS-1$

	/**
	 * Name of the handle id attribute in a JavaScript marker.
	 */
	protected static final String ATT_HANDLE_ID =
		"org.eclipse.wst.jsdt.internal.core.JavaModelManager.handleId" ; //$NON-NLS-1$

	/**
	 * Name of the User Library Container id.
	 */
	public static final String USER_LIBRARY_CONTAINER_ID= "org.eclipse.wst.jsdt.USER_LIBRARY"; //$NON-NLS-1$

	// *************** Possible IDs for configurable options. ********************

	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_LOCAL_VARIABLE_ATTR = PLUGIN_ID + ".compiler.debug.localVariable"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_LINE_NUMBER_ATTR = PLUGIN_ID + ".compiler.debug.lineNumber"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_SOURCE_FILE_ATTR = PLUGIN_ID + ".compiler.debug.sourceFile"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_CODEGEN_UNUSED_LOCAL = PLUGIN_ID + ".compiler.codegen.unusedLocal"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_CODEGEN_TARGET_PLATFORM = PLUGIN_ID + ".compiler.codegen.targetPlatform"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_CODEGEN_INLINE_JSR_BYTECODE = PLUGIN_ID + ".compiler.codegen.inlineJsrBytecode"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_DOC_COMMENT_SUPPORT = PLUGIN_ID + ".compiler.doc.comment.support"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 * @deprecated - discontinued since turning off would violate language specs
	 */
	public static final String COMPILER_PB_UNREACHABLE_CODE = PLUGIN_ID + ".compiler.problem.unreachableCode"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 * @deprecated - discontinued since turning off would violate language specs
	 */
	public static final String COMPILER_PB_INVALID_IMPORT = PLUGIN_ID + ".compiler.problem.invalidImport"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNDEFINED_FIELD = PLUGIN_ID + ".compiler.problem.undefinedField"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_METHOD_WITH_CONSTRUCTOR_NAME = PLUGIN_ID + ".compiler.problem.methodWithConstructorName"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_DEPRECATION = PLUGIN_ID + ".compiler.problem.deprecation"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE = PLUGIN_ID + ".compiler.problem.deprecationInDeprecatedCode"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD = "org.eclipse.wst.jsdt.core.compiler.problem.deprecationWhenOverridingDeprecatedMethod"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_HIDDEN_CATCH_BLOCK = PLUGIN_ID + ".compiler.problem.hiddenCatchBlock"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNUSED_LOCAL = PLUGIN_ID + ".compiler.problem.unusedLocal"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNUSED_PARAMETER = PLUGIN_ID + ".compiler.problem.unusedParameter"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNUSED_PARAMETER_WHEN_IMPLEMENTING_ABSTRACT = PLUGIN_ID + ".compiler.problem.unusedParameterWhenImplementingAbstract"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNUSED_PARAMETER_WHEN_OVERRIDING_CONCRETE = PLUGIN_ID + ".compiler.problem.unusedParameterWhenOverridingConcrete"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE = PLUGIN_ID + ".compiler.problem.unusedParameterIncludeDocCommentReference"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNUSED_IMPORT = PLUGIN_ID + ".compiler.problem.unusedImport"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_NON_NLS_STRING_LITERAL = PLUGIN_ID + ".compiler.problem.nonExternalizedStringLiteral"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_ASSERT_IDENTIFIER = PLUGIN_ID + ".compiler.problem.assertIdentifier"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_STATIC_ACCESS_RECEIVER = PLUGIN_ID + ".compiler.problem.staticAccessReceiver"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_INDIRECT_STATIC_ACCESS = PLUGIN_ID + ".compiler.problem.indirectStaticAccess"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_NO_EFFECT_ASSIGNMENT = PLUGIN_ID + ".compiler.problem.noEffectAssignment"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNUSED_PRIVATE_MEMBER = PLUGIN_ID + ".compiler.problem.unusedPrivateMember"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_LOCAL_VARIABLE_HIDING = PLUGIN_ID + ".compiler.problem.localVariableHiding"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_SPECIAL_PARAMETER_HIDING_FIELD = PLUGIN_ID + ".compiler.problem.specialParameterHidingField"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_FIELD_HIDING = PLUGIN_ID + ".compiler.problem.fieldHiding"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_TYPE_PARAMETER_HIDING = PLUGIN_ID + ".compiler.problem.typeParameterHiding"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT = PLUGIN_ID + ".compiler.problem.possibleAccidentalBooleanAssignment"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_FALLTHROUGH_CASE = PLUGIN_ID + ".compiler.problem.fallthroughCase"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_EMPTY_STATEMENT = PLUGIN_ID + ".compiler.problem.emptyStatement"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_BOOLEAN_METHOD_THROWING_EXCEPTION = PLUGIN_ID + ".compiler.problem.booleanMethodThrowingException"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNNECESSARY_TYPE_CHECK = PLUGIN_ID + ".compiler.problem.unnecessaryTypeCheck"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNNECESSARY_ELSE = PLUGIN_ID + ".compiler.problem.unnecessaryElse"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNDOCUMENTED_EMPTY_BLOCK = PLUGIN_ID + ".compiler.problem.undocumentedEmptyBlock"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_FINALLY_BLOCK_NOT_COMPLETING = PLUGIN_ID + ".compiler.problem.finallyBlockNotCompletingNormally"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION = PLUGIN_ID + ".compiler.problem.unusedDeclaredThrownException"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING = PLUGIN_ID + ".compiler.problem.unusedDeclaredThrownExceptionWhenOverriding"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNQUALIFIED_FIELD_ACCESS = PLUGIN_ID + ".compiler.problem.unqualifiedFieldAccess"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 * @deprecated - got renamed into {@link #COMPILER_PB_UNCHECKED_TYPE_OPERATION}
	 */
	public static final String COMPILER_PB_UNSAFE_TYPE_OPERATION = PLUGIN_ID + ".compiler.problem.uncheckedTypeOperation"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNCHECKED_TYPE_OPERATION = PLUGIN_ID + ".compiler.problem.uncheckedTypeOperation"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_RAW_TYPE_REFERENCE = PLUGIN_ID + ".compiler.problem.rawTypeReference"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_FINAL_PARAMETER_BOUND = PLUGIN_ID + ".compiler.problem.finalParameterBound"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_VARARGS_ARGUMENT_NEED_CAST = PLUGIN_ID + ".compiler.problem.varargsArgumentNeedCast"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_MISSING_OVERRIDE_ANNOTATION = PLUGIN_ID + ".compiler.problem.missingOverrideAnnotation"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_MISSING_DEPRECATED_ANNOTATION = PLUGIN_ID + ".compiler.problem.missingDeprecatedAnnotation"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNUSED_LABEL = PLUGIN_ID + ".compiler.problem.unusedLabel"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_INVALID_JAVADOC = PLUGIN_ID + ".compiler.problem.invalidJavadoc"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_INVALID_JAVADOC_TAGS = PLUGIN_ID + ".compiler.problem.invalidJavadocTags"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_INVALID_JAVADOC_TAGS__DEPRECATED_REF = PLUGIN_ID + ".compiler.problem.invalidJavadocTagsDeprecatedRef"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_INVALID_JAVADOC_TAGS__NOT_VISIBLE_REF = PLUGIN_ID + ".compiler.problem.invalidJavadocTagsNotVisibleRef"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_INVALID_JAVADOC_TAGS_VISIBILITY = PLUGIN_ID + ".compiler.problem.invalidJavadocTagsVisibility"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_TAGS = PLUGIN_ID + ".compiler.problem.missingJavadocTags"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_TAGS_VISIBILITY = PLUGIN_ID + ".compiler.problem.missingJavadocTagsVisibility"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_TAGS_OVERRIDING = PLUGIN_ID + ".compiler.problem.missingJavadocTagsOverriding"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_COMMENTS = PLUGIN_ID + ".compiler.problem.missingJavadocComments"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY = PLUGIN_ID + ".compiler.problem.missingJavadocCommentsVisibility"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING = PLUGIN_ID + ".compiler.problem.missingJavadocCommentsOverriding"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_MAX_PER_UNIT = PLUGIN_ID + ".compiler.maxProblemPerUnit"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_FATAL_OPTIONAL_ERROR = PLUGIN_ID + ".compiler.problem.fatalOptionalError"; //$NON-NLS-1$
	/**
	 * Possible configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_PARAMETER_ASSIGNMENT = PLUGIN_ID + ".compiler.problem.parameterAssignment"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_SEMANTIC_VALIDATION = "semanticValidation"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_STRICT_ON_KEYWORD_USAGE = "strictOnKeywordUsage"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_SOURCE = PLUGIN_ID + ".compiler.source"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_COMPLIANCE = PLUGIN_ID + ".compiler.compliance"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_TASK_PRIORITIES = PLUGIN_ID + ".compiler.taskPriorities"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value for COMPILER_TASK_PRIORITIES.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_TASK_PRIORITY_HIGH = "HIGH"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value for COMPILER_TASK_PRIORITIES.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_TASK_PRIORITY_LOW = "LOW"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value for COMPILER_TASK_PRIORITIES.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_TASK_PRIORITY_NORMAL = "NORMAL"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_TASK_TAGS = PLUGIN_ID + ".compiler.taskTags"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_TASK_CASE_SENSITIVE = PLUGIN_ID + ".compiler.taskCaseSensitive"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_FORBIDDEN_REFERENCE = PLUGIN_ID + ".compiler.problem.forbiddenReference"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_DISCOURAGED_REFERENCE = PLUGIN_ID + ".compiler.problem.discouragedReference"; //$NON-NLS-1$

	/* START -------------------------------- Bug 203292 Type/Method/Filed resolution error configuration --------------------- */
	public static final String UNRESOLVED_TYPE_REFERENCE = PLUGIN_ID + ".compiler.problem.unresolvedTypeReference"; //$NON-NLS-1$
	public static final String UNRESOLVED_FIELD_REFERENCE = PLUGIN_ID + ".compiler.problem.unresolvedFieldReference"; //$NON-NLS-1$
	public static final String UNRESOLVED_METHOD_REFERENCE = PLUGIN_ID + ".compiler.problem.unresolvedMethodReference"; //$NON-NLS-1$
	/* END -------------------------------- Bug 203292 Type/Method/Filed resolution error configuration --------------------- */

	/* START -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */
	public static final String LOOSE_VAR_DECL = PLUGIN_ID + ".compiler.problem.looseVarDecleration"; //$NON-NLS-1$
	public static final String OPTIONAL_SEMICOLON = PLUGIN_ID + ".compiler.problem.optionalSemicolon"; //$NON-NLS-1$
	/* END   -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */


	/**

	 *
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_SUPPRESS_WARNINGS = PLUGIN_ID + ".compiler.problem.suppressWarnings"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNHANDLED_WARNING_TOKEN = PLUGIN_ID + ".compiler.problem.unhandledWarningToken"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_NULL_REFERENCE = PLUGIN_ID + ".compiler.problem.nullReference"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_POTENTIAL_NULL_REFERENCE = PLUGIN_ID + ".compiler.problem.potentialNullReference"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_DUPLICATE_LOCAL_VARIABLES = PLUGIN_ID + ".compiler.problem.duplicateLocalVariables"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_REDUNDANT_NULL_CHECK = PLUGIN_ID + ".compiler.problem.redundantNullCheck"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNINITIALIZED_LOCAL_VARIABLE = PLUGIN_ID + ".compiler.problem.uninitializedLocalVariable"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_UNINITIALIZED_GLOBAL_VARIABLE = PLUGIN_ID + ".compiler.problem.uninitializedGlobalVariable"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPILER_PB_OVERRIDING_METHOD_WITHOUT_SUPER_INVOCATION = PLUGIN_ID + ".compiler.problem.overridingMethodWithoutSuperInvocation"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CORE_JAVA_BUILD_ORDER = PLUGIN_ID + ".computeJavaBuildOrder"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CORE_JAVA_BUILD_RESOURCE_COPY_FILTER = PLUGIN_ID + ".builder.resourceCopyExclusionFilter"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CORE_JAVA_BUILD_DUPLICATE_RESOURCE = PLUGIN_ID + ".builder.duplicateResourceTask"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER = PLUGIN_ID + ".builder.cleanOutputFolder"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CORE_JAVA_BUILD_RECREATE_MODIFIED_CLASS_FILES_IN_OUTPUT_FOLDER = PLUGIN_ID + ".builder.recreateModifiedClassFileInOutputFolder"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CORE_INCOMPLETE_CLASSPATH = PLUGIN_ID + ".incompleteClasspath"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CORE_CIRCULAR_CLASSPATH = PLUGIN_ID + ".circularClasspath"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CORE_INCOMPATIBLE_JDK_LEVEL = PLUGIN_ID + ".incompatibleJDKLevel"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CORE_JAVA_BUILD_INVALID_CLASSPATH = PLUGIN_ID + ".builder.invalidClasspath"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CORE_ENCODING = PLUGIN_ID + ".encoding"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CORE_DEFAULT_CLASSPATH_EXCLUSION_PATTERNS = PLUGIN_ID + ".classpath.exclusionPatterns.default"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CORE_ENABLE_CLASSPATH_EXCLUSION_PATTERNS = PLUGIN_ID + ".classpath.exclusionPatterns"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CORE_ENABLE_CLASSPATH_MULTIPLE_OUTPUT_LOCATIONS = PLUGIN_ID + ".classpath.multipleOutputLocations"; //$NON-NLS-1$
	/**
	 * Default task tag
	 * @deprecated Use {@link #DEFAULT_TASK_TAGS} instead
	 */
	public static final String DEFAULT_TASK_TAG = "TODO"; //$NON-NLS-1$
	/**
	 * Default task priority
	 * @deprecated Use {@link #DEFAULT_TASK_PRIORITIES} instead
	 */
	public static final String DEFAULT_TASK_PRIORITY = "NORMAL"; //$NON-NLS-1$
	/**
	 * Default task tag
	 */
	public static final String DEFAULT_TASK_TAGS = "TODO,FIXME,XXX"; //$NON-NLS-1$
	/**
	 * Default task priority
	 */
	public static final String DEFAULT_TASK_PRIORITIES = "NORMAL,HIGH,NORMAL"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CODEASSIST_VISIBILITY_CHECK = PLUGIN_ID + ".codeComplete.visibilityCheck"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CODEASSIST_DEPRECATION_CHECK = PLUGIN_ID + ".codeComplete.deprecationCheck"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CODEASSIST_CAMEL_CASE_MATCH = PLUGIN_ID + ".codeComplete.camelCaseMatch"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CODEASSIST_IMPLICIT_QUALIFICATION = PLUGIN_ID + ".codeComplete.forceImplicitQualification"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CODEASSIST_FIELD_PREFIXES = PLUGIN_ID + ".codeComplete.fieldPrefixes"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CODEASSIST_STATIC_FIELD_PREFIXES = PLUGIN_ID + ".codeComplete.staticFieldPrefixes"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CODEASSIST_LOCAL_PREFIXES = PLUGIN_ID + ".codeComplete.localPrefixes"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CODEASSIST_ARGUMENT_PREFIXES = PLUGIN_ID + ".codeComplete.argumentPrefixes"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CODEASSIST_FIELD_SUFFIXES = PLUGIN_ID + ".codeComplete.fieldSuffixes"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CODEASSIST_STATIC_FIELD_SUFFIXES = PLUGIN_ID + ".codeComplete.staticFieldSuffixes"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CODEASSIST_LOCAL_SUFFIXES = PLUGIN_ID + ".codeComplete.localSuffixes"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CODEASSIST_ARGUMENT_SUFFIXES = PLUGIN_ID + ".codeComplete.argumentSuffixes"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CODEASSIST_FORBIDDEN_REFERENCE_CHECK= PLUGIN_ID + ".codeComplete.forbiddenReferenceCheck"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CODEASSIST_DISCOURAGED_REFERENCE_CHECK= PLUGIN_ID + ".codeComplete.discouragedReferenceCheck"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String CODEASSIST_SUGGEST_STATIC_IMPORTS= PLUGIN_ID + ".codeComplete.suggestStaticImports"; //$NON-NLS-1$
	/**
	 * Possible  configurable option ID.
	 * @see #getDefaultOptions()
	 */
	public static final String TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC = PLUGIN_ID + ".timeoutForParameterNameFromAttachedJavadoc"; //$NON-NLS-1$

	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String GENERATE = "generate"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String DO_NOT_GENERATE = "do not generate"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String PRESERVE = "preserve"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String OPTIMIZE_OUT = "optimize out"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 * @since 1.3.401
	 */
	public static final String VERSION_0_0 = "0.0"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String VERSION_1_1 = "1.1"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String VERSION_1_2 = "1.2"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String VERSION_1_3 = "1.3"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String VERSION_1_4 = "1.4"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String VERSION_1_5 = "1.5"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String VERSION_1_6 = "1.6"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String VERSION_1_7 = "1.7"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String ABORT = "abort"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String ERROR = "error"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String WARNING = "warning"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String IGNORE = "ignore"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPUTE = "compute"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String INSERT = "insert"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String DO_NOT_INSERT = "do not insert"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String PRESERVE_ONE = "preserve one"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String CLEAR_ALL = "clear all"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String NORMAL = "normal"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String COMPACT = "compact"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String TAB = "tab"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String SPACE = "space"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String ENABLED = "enabled"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String DISABLED = "disabled"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String CLEAN = "clean"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String PUBLIC = "public"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String PROTECTED = "protected"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String DEFAULT = "default"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String PRIVATE = "private"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String NEVER = "never"; //$NON-NLS-1$
	/**
	 * Possible  configurable option value.
	 * @see #getDefaultOptions()
	 */
	public static final String DEFAULT_EXCLUSION_PATTERNS = "**/*.min.js,**/node_modules/*,**/bower_components/*";	 //$NON-NLS-1$

	/**
	 * Value of the content-type for JavaScript source files. Use this value to retrieve the JavaScript content type
	 * from the content type manager, and to add new JavaScript-like extensions to this content type.
	 *
	 * @see org.eclipse.core.runtime.content.IContentTypeManager#getContentType(String)
	 * @see #getJavaScriptLikeExtensions()
	 */
	public static final String JAVA_SOURCE_CONTENT_TYPE = JavaScriptCore.PLUGIN_ID+".jsSource" ; //$NON-NLS-1$

	
	public static final String READ_ONLY_SOURCE_PROPERTY =  "readOnlyResource" ; //$NON-NLS-1$

	/**
	 * Creates the JavaScript core plug-in.
	 * <p>
	 * The plug-in instance is created automatically by the
	 * Eclipse platform. Clients must not call.
	 * </p>
	 *
	 */
	public JavaScriptCore() {
		super();
		JAVA_CORE_PLUGIN = this;
	}

	/**
	 * Adds the given listener for changes to JavaScript elements.
	 * Has no effect if an identical listener is already registered.
	 *
	 * This listener will only be notified during the POST_CHANGE resource change notification
	 * and any reconcile operation (POST_RECONCILE).
	 * For finer control of the notification, use <code>addElementChangedListener(IElementChangedListener,int)</code>,
	 * which allows to specify a different eventMask.
	 *
	 * @param listener the listener
	 * @see ElementChangedEvent
	 */
	public static void addElementChangedListener(IElementChangedListener listener) {
		addElementChangedListener(listener, ElementChangedEvent.POST_CHANGE | ElementChangedEvent.POST_RECONCILE);
	}

	/**
	 * Adds the given listener for changes to JavaScript elements.
	 * Has no effect if an identical listener is already registered.
	 * After completion of this method, the given listener will be registered for exactly
	 * the specified events.  If they were previously registered for other events, they
	 * will be deregistered.
	 * <p>
	 * Once registered, a listener starts receiving notification of changes to
	 * javaScript elements in the model. The listener continues to receive
	 * notifications until it is replaced or removed.
	 * </p>
	 * <p>
	 * Listeners can listen for several types of event as defined in <code>ElementChangeEvent</code>.
	 * Clients are free to register for any number of event types however if they register
	 * for more than one, it is their responsibility to ensure they correctly handle the
	 * case where the same javaScript element change shows up in multiple notifications.
	 * Clients are guaranteed to receive only the events for which they are registered.
	 * </p>
	 *
	 * @param listener the listener
	 * @param eventMask the bit-wise OR of all event types of interest to the listener
	 * @see IElementChangedListener
	 * @see ElementChangedEvent
	 * @see #removeElementChangedListener(IElementChangedListener)
	 */
	public static void addElementChangedListener(IElementChangedListener listener, int eventMask) {
		JavaModelManager.getJavaModelManager().deltaState.addElementChangedListener(listener, eventMask);
	}

	/**
	 * Configures the given marker attribute map for the given JavaScript element.
	 * Used for markers, which denote a JavaScript element rather than a resource.
	 *
	 * @param attributes the mutable marker attribute map (key type: <code>String</code>,
	 *   value type: <code>String</code>)
	 * @param element the JavaScript element for which the marker needs to be configured
	 */
	public static void addJavaScriptElementMarkerAttributes(
		Map attributes,
		IJavaScriptElement element) {
		if (element instanceof IMember)
			element = ((IMember) element).getClassFile();
		if (attributes != null && element != null)
			attributes.put(ATT_HANDLE_ID, element.getHandleIdentifier());
	}

	/**
	 * Adds the given listener for resource change events of the given types to the JavaScript core.
	 * The listener is guaranteed to be notified of the resource change event before
	 * the JavaScript core starts processing the resource change event itself.
	 * <p>
	 * If an identical listener is already registered, the given event types are added to the event types
	 * of interest to the listener.
	 * </p>
	 * <p>
	 * Supported event types are:
	 * <ul>
	 * <li>{@link IResourceChangeEvent#PRE_BUILD}</li>
	 * <li>{@link IResourceChangeEvent#POST_BUILD}</li>
	 * <li>{@link IResourceChangeEvent#POST_CHANGE}</li>
	 * <li>{@link IResourceChangeEvent#PRE_DELETE}</li>
	 * <li>{@link IResourceChangeEvent#PRE_CLOSE}</li>
	 * </ul>
	 * This list may increase in the future.
	 * </p>
	 *
	 * @param listener the listener
	 * @param eventMask the bit-wise OR of all event types of interest to the
	 * listener
	 * @see #removePreProcessingResourceChangedListener(IResourceChangeListener)
	 * @see IResourceChangeEvent
	 */
	public static void addPreProcessingResourceChangedListener(IResourceChangeListener listener, int eventMask) {
		JavaModelManager.getJavaModelManager().deltaState.addPreResourceChangedListener(listener, eventMask);
	}

	/**
	 * Configures the given marker for the given JavaScript element.
	 * Used for markers, which denote a JavaScript element rather than a resource.
	 *
	 * @param marker the marker to be configured
	 * @param element the JavaScript element for which the marker needs to be configured
	 * @exception CoreException if the <code>IMarker.setAttribute</code> on the marker fails
	 */
	public void configureJavaScriptElementMarker(IMarker marker, IJavaScriptElement element)
		throws CoreException {
		if (element instanceof IMember)
			element = ((IMember) element).getClassFile();
		if (marker != null && element != null)
			marker.setAttribute(ATT_HANDLE_ID, element.getHandleIdentifier());
	}

	/**
	 * Returns the JavaScript model element corresponding to the given handle identifier
	 * generated by <code>IJavaScriptElement.getHandleIdentifier()</code>, or
	 * <code>null</code> if unable to create the associated element.
	 *
	 * @param handleIdentifier the given handle identifier
	 * @return the JavaScript element corresponding to the handle identifier
	 */
	public static IJavaScriptElement create(String handleIdentifier) {
		return create(handleIdentifier, DefaultWorkingCopyOwner.PRIMARY);
	}

	/**
	 * Returns the JavaScript model element corresponding to the given handle identifier
	 * generated by <code>IJavaScriptElement.getHandleIdentifier()</code>, or
	 * <code>null</code> if unable to create the associated element.
	 * If the returned JavaScript element is an <code>IJavaScriptUnit</code>, its owner
	 * is the given owner if such a working copy exists, otherwise the javaScript unit
	 * is a primary javaScript unit.
	 *
	 * @param handleIdentifier the given handle identifier
	 * @param owner the owner of the returned javaScript unit, ignored if the returned
	 *   element is not a javaScript unit
	 * @return the JavaScript element corresponding to the handle identifier
	 */
	public static IJavaScriptElement create(String handleIdentifier, WorkingCopyOwner owner) {
		if (handleIdentifier == null) {
			return null;
		}
		MementoTokenizer memento = new MementoTokenizer(handleIdentifier);
		JavaModel model = JavaModelManager.getJavaModelManager().getJavaModel();
		return model.getHandleFromMemento(memento, owner);
	}

	/**
	 * Returns the JavaScript element corresponding to the given file, or
	 * <code>null</code> if unable to associate the given file
	 * with a JavaScript element.
	 *
	 * <p>The file must be one of:<ul>
	 *	<li>a file with one of the {@link JavaScriptCore#getJavaScriptLikeExtensions()
	 *      JavaScript-like extensions} - the element returned is the corresponding <code>IJavaScriptUnit</code></li>
	 *	<li>a <code>.class</code> file - the element returned is the corresponding <code>IClassFile</code></li>
	 *	<li>a <code>.jar</code> file - the element returned is the corresponding <code>IPackageFragmentRoot</code></li>
	 *	</ul>
	 * <p>
	 * Creating a JavaScript element has the side effect of creating and opening all of the
	 * element's parents if they are not yet open.
	 *
	 * @param file the given file
	 * @return the JavaScript element corresponding to the given file, or
	 * <code>null</code> if unable to associate the given file
	 * with a JavaScript element
	 */
	public static IJavaScriptElement create(IFile file) {
		return JavaModelManager.create(file, null/*unknown javaScript project*/);
	}
	/**
	 * Returns the source folder (package fragment or package fragment root) corresponding to the given folder, or
	 * <code>null</code> if unable to associate the given folder with a JavaScript element.
	 * <p>
	 * Note that a package fragment root is returned rather than a default package.
	 * <p>
	 * Creating a JavaScript element has the side effect of creating and opening all of the
	 * element's parents if they are not yet open.
	 *
	 * @param folder the given folder
	 * @return the package fragment or package fragment root corresponding to the given folder, or
	 * <code>null</code> if unable to associate the given folder with a JavaScript element
	 */
	public static IJavaScriptElement create(IFolder folder) {
		return JavaModelManager.create(folder, null/*unknown javaScript project*/);
	}
	/**
	 * Returns the JavaScript project corresponding to the given project.
	 * <p>
	 * Creating a JavaScript Project has the side effect of creating and opening all of the
	 * project's parents if they are not yet open.
	 * <p>
	 * Note that no check is done at this time on the existence or the javaScript nature of this project.
	 *
	 * @param project the given project
	 * @return the JavaScript project corresponding to the given project, null if the given project is null
	 */
	public static IJavaScriptProject create(IProject project) {
		if (project == null) {
			return null;
		}
		JavaModel javaModel = JavaModelManager.getJavaModelManager().getJavaModel();
		return javaModel.getJavaProject(project);
	}
	/**
	 * Returns the JavaScript element corresponding to the given resource, or
	 * <code>null</code> if unable to associate the given resource
	 * with a JavaScript element.
	 * <p>
	 * The resource must be one of:<ul>
	 *	<li>a project - the element returned is the corresponding <code>IJavaScriptProject</code></li>
	 *	<li>a file with one of the {@link JavaScriptCore#getJavaScriptLikeExtensions()
	 *      JavaScript-like extensions} - the element returned is the corresponding <code>IJavaScriptUnit</code></li>
	 *  <li>a folder - the element returned is the corresponding <code>IPackageFragmentRoot</code>
	 *    	or <code>IPackageFragment</code></li>
	 *  <li>the workspace root resource - the element returned is the <code>IJavaScriptModel</code></li>
	 *	</ul>
	 * <p>
	 * Creating a JavaScript element has the side effect of creating and opening all of the
	 * element's parents if they are not yet open.
	 *
	 * @param resource the given resource
	 * @return the JavaScript element corresponding to the given resource, or
	 * <code>null</code> if unable to associate the given resource
	 * with a JavaScript element
	 */
	public static IJavaScriptElement create(IResource resource) {
		return JavaModelManager.create(resource, null/*unknown javaScript project*/);
	}
	/**
	 * Returns the JavaScript element corresponding to the given file, its project being the given
	 * project. Returns <code>null</code> if unable to associate the given resource
	 * with a JavaScript element.
	 *<p>
	 * The resource must be one of:<ul>
	 *	<li>a project - the element returned is the corresponding <code>IJavaScriptProject</code></li>
	 *	<li>a file with one of the {@link JavaScriptCore#getJavaScriptLikeExtensions()
	 *      JavaScript-like extensions} - the element returned is the corresponding <code>IJavaScriptUnit</code></li>
	 *  <li>a folder - the element returned is the corresponding <code>IPackageFragmentRoot</code>
	 *    	or <code>IPackageFragment</code></li>
	 *  <li>the workspace root resource - the element returned is the <code>IJavaScriptModel</code></li>
	 *	</ul>
	 * <p>
	 * Creating a JavaScript element has the side effect of creating and opening all of the
	 * element's parents if they are not yet open.
	 *
	 * @param resource the given resource
	 * @return the JavaScript element corresponding to the given file, or
	 * <code>null</code> if unable to associate the given file
	 * with a JavaScript element
	 */
	public static IJavaScriptElement create(IResource resource, IJavaScriptProject project) {
		return JavaModelManager.create(resource, project);
	}
	/**
	 * Returns the JavaScript model.
	 *
	 * @param root the given root
	 * @return the JavaScript model, or <code>null</code> if the root is null
	 */
	public static IJavaScriptModel create(IWorkspaceRoot root) {
		if (root == null) {
			return null;
		}
		return JavaModelManager.getJavaModelManager().getJavaModel();
	}
	/*
	 * Creates and returns a class file element for
	 * the given <code>.class</code> file. Returns <code>null</code> if unable
	 * to recognize the class file.
	 *
	 * @param file the given <code>.class</code> file
	 * @return a class file element for the given <code>.class</code> file, or <code>null</code> if unable
	 * to recognize the class file
	 */
	public static IClassFile createClassFileFrom(IFile file) {
		return JavaModelManager.createClassFileFrom(file, null);
	}
	/**
	 * Creates and returns a javaScript unit element for
	 * the given source file (i.e. a file with one of the {@link JavaScriptCore#getJavaScriptLikeExtensions()
	 * JavaScript-like extensions}). Returns <code>null</code> if unable
	 * to recognize the javaScript unit.
	 *
	 * @param file the given source file
	 * @return a javaScript unit element for the given source file, or <code>null</code> if unable
	 * to recognize the javaScript unit
	 */
	public static IJavaScriptUnit createCompilationUnitFrom(IFile file) {
		return JavaModelManager.createCompilationUnitFrom(file, null/*unknown javaScript project*/);
	}
	/*
	 * Creates and returns a handle for the given JAR file.
	 * The JavaScript model associated with the JAR's project may be
	 * created as a side effect.
	 *
	 * @param file the given JAR file
	 * @return a handle for the given JAR file, or <code>null</code> if unable to create a JAR package fragment root.
	 * (for example, if the JAR file represents a non-JavaScript resource)
	 */
	public static IPackageFragmentRoot createJarPackageFragmentRootFrom(IFile file) {
		return JavaModelManager.createJarPackageFragmentRootFrom(file, null/*unknown javaScript project*/);
	}

	/**
	 * Answers the project specific value for a given includepath container.
	 * In case this container path could not be resolved, then will answer <code>null</code>.
	 * Both the container path and the project context are supposed to be non-null.
	 * <p>
	 * The containerPath is a formed by a first ID segment followed with extra segments, which can be
	 * used as additional hints for resolution. If no container was ever recorded for this container path
	 * onto this project (using <code>setJsGlobalScopeContainer</code>, then a
	 * <code>JsGlobalScopeContainerInitializer</code> will be activated if any was registered for this container
	 * ID onto the extension point "org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer".
	 * <p>
	 * There is no assumption that the returned container must answer the exact same containerPath
	 * when requested <code>IJsGlobalScopeContainer#getPath</code>.
	 * Indeed, the containerPath is just an indication for resolving it to an actual container object.
	 * <p>
	 * Includepath container values are persisted locally to the workspace, but
	 * are not preserved from a session to another. It is thus highly recommended to register a
	 * <code>JsGlobalScopeContainerInitializer</code> for each referenced container
	 * (through the extension point "org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer").
	 * <p>
	 * @param containerPath the name of the container, which needs to be resolved
	 * @param project a specific project in which the container is being resolved
	 * @return the corresponding includepath container or <code>null</code> if unable to find one.
	 *
	 * @exception JavaScriptModelException if an exception occurred while resolving the container, or if the resolved container
	 *   contains illegal entries (contains CPE_CONTAINER entries or null entries).
	 *
	 * @see JsGlobalScopeContainerInitializer
	 * @see IJsGlobalScopeContainer
	 * @see #setJsGlobalScopeContainer(IPath, IJavaScriptProject[], IJsGlobalScopeContainer[], IProgressMonitor)
	 */
	public static IJsGlobalScopeContainer getJsGlobalScopeContainer(IPath containerPath, IJavaScriptProject project) throws JavaScriptModelException {

	    JavaModelManager manager = JavaModelManager.getJavaModelManager();
		IJsGlobalScopeContainer container = manager.getJsGlobalScopeContainer(containerPath, project);
		if (container == JavaModelManager.CONTAINER_INITIALIZATION_IN_PROGRESS) {
		    return manager.getPreviousSessionContainer(containerPath, project);
		}
		return container;
	}

	/**
	 * Helper method finding the includepath container initializer registered for a given includepath container ID
	 * or <code>null</code> if none was found while iterating over the contributions to extension point to
	 * the extension point "org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer".
	 * <p>
	 * A containerID is the first segment of any container path, used to identify the registered container initializer.
	 * <p>
	 * @param containerID - a containerID identifying a registered initializer
	 * @return JsGlobalScopeContainerInitializer - the registered includepath container initializer or <code>null</code> if
	 * none was found.
	 */
	public static JsGlobalScopeContainerInitializer getJsGlobalScopeContainerInitializer(String containerID) {
		HashMap containerInitializersCache = JavaModelManager.getJavaModelManager().containerInitializersCache;
		JsGlobalScopeContainerInitializer initializer = (JsGlobalScopeContainerInitializer) containerInitializersCache.get(containerID);
		if (initializer == null) {
			initializer = computeJsGlobalScopeContainerInitializer(containerID);
			if (initializer == null)
				return null;
			containerInitializersCache.put(containerID, initializer);
		}
		return initializer;
	}

	private static JsGlobalScopeContainerInitializer computeJsGlobalScopeContainerInitializer(String containerID) {
		Plugin jdtCorePlugin = JavaScriptCore.getPlugin();
		if (jdtCorePlugin == null) return null;

		IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(JavaScriptCore.PLUGIN_ID, JavaModelManager.CPCONTAINER_INITIALIZER_EXTPOINT_ID);
		if (extension != null) {
			IExtension[] extensions =  extension.getExtensions();
			for(int i = 0; i < extensions.length; i++){
				IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
				for(int j = 0; j < configElements.length; j++){
					IConfigurationElement configurationElement = configElements[j];
					String initializerID = configurationElement.getAttribute("id"); //$NON-NLS-1$
					if (initializerID != null && initializerID.equals(containerID)){
						if (JavaModelManager.CP_RESOLVE_VERBOSE_ADVANCED)
							verbose_found_container_initializer(containerID, configurationElement);
						try {
							Object execExt = configurationElement.createExecutableExtension("class"); //$NON-NLS-1$
							if (execExt instanceof JsGlobalScopeContainerInitializer){
								return (JsGlobalScopeContainerInitializer)execExt;
							}
						} catch(CoreException e) {
							// executable extension could not be created: ignore this initializer
							if (JavaModelManager.CP_RESOLVE_VERBOSE) {
								verbose_failed_to_instanciate_container_initializer(containerID, configurationElement);
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		return null;
	}

	private static void verbose_failed_to_instanciate_container_initializer(String containerID, IConfigurationElement configurationElement) {
		Util.verbose(
			"CPContainer INIT - failed to instanciate initializer\n" + //$NON-NLS-1$
			"	container ID: " + containerID + '\n' + //$NON-NLS-1$
			"	class: " + configurationElement.getAttribute("class"), //$NON-NLS-1$ //$NON-NLS-2$
			System.err);
	}

	private static void verbose_found_container_initializer(String containerID, IConfigurationElement configurationElement) {
		Util.verbose(
			"CPContainer INIT - found initializer\n" + //$NON-NLS-1$
			"	container ID: " + containerID + '\n' + //$NON-NLS-1$
			"	class: " + configurationElement.getAttribute("class")); //$NON-NLS-1$ //$NON-NLS-2$
	}


	/**
	 * Returns the path held in the given includepath variable.
	 * Returns <code>null</code> if unable to bind.
	 * <p>
	 * Includepath variable values are persisted locally to the workspace, and
	 * are preserved from session to session.
	 * <p>
	 * Note that includepath variables can be contributed registered initializers for,
	 * using the extension point "org.eclipse.wst.jsdt.core.JsGlobalScopeVariableInitializer".
	 * If an initializer is registered for a variable, its persisted value will be ignored:
	 * its initializer will thus get the opportunity to rebind the variable differently on
	 * each session.
	 *
	 * @param variableName the name of the includepath variable
	 * @return the path, or <code>null</code> if none
	 * @see #setClasspathVariable(String, IPath)
	 */
	/**
	 * Returns the path held in the given includepath variable.
	 * Returns <code>null</code> if unable to bind.
	 * <p>
	 * Includepath variable values are persisted locally to the workspace, and
	 * are preserved from session to session.
	 * <p>
	 * Note that includepath variables can be contributed registered initializers for,
	 * using the extension point "org.eclipse.wst.jsdt.core.JsGlobalScopeVariableInitializer".
	 * If an initializer is registered for a variable, its persisted value will be ignored:
	 * its initializer will thus get the opportunity to rebind the variable differently on
	 * each session.
	 *
	 * @param variableName the name of the includepath variable
	 * @return the path, or <code>null</code> if none
	 * @see #setIncludepathVariable(String, IPath)
	 */
	public static IPath getIncludepathVariable(final String variableName) {

	    JavaModelManager manager = JavaModelManager.getJavaModelManager();
		IPath variablePath = manager.variableGet(variableName);
		if (variablePath == JavaModelManager.VARIABLE_INITIALIZATION_IN_PROGRESS){
		    return manager.getPreviousSessionVariable(variableName);
		}

		if (variablePath != null) {
			if (variablePath == JavaModelManager.CP_ENTRY_IGNORE_PATH)
				return null;
			return variablePath;
		}

		// even if persisted value exists, initializer is given priority, only if no initializer is found the persisted value is reused
		final JsGlobalScopeVariableInitializer initializer = JavaScriptCore.getJsGlobalScopeVariableInitializer(variableName);
		if (initializer != null){
			if (JavaModelManager.CP_RESOLVE_VERBOSE)
				verbose_triggering_variable_initialization(variableName, initializer);
			if (JavaModelManager.CP_RESOLVE_VERBOSE_ADVANCED)
				verbose_triggering_variable_initialization_invocation_trace();
			manager.variablePut(variableName, JavaModelManager.VARIABLE_INITIALIZATION_IN_PROGRESS); // avoid initialization cycles
			boolean ok = false;
			try {
				// let OperationCanceledException go through
				// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=59363)
				initializer.initialize(variableName);

				variablePath = manager.variableGet(variableName); // initializer should have performed side-effect
				if (variablePath == JavaModelManager.VARIABLE_INITIALIZATION_IN_PROGRESS) return null; // break cycle (initializer did not init or reentering call)
				if (JavaModelManager.CP_RESOLVE_VERBOSE_ADVANCED)
					verbose_variable_value_after_initialization(variableName, variablePath);
				manager.variablesWithInitializer.add(variableName);
				ok = true;
			} catch (RuntimeException e) {
				if (JavaModelManager.CP_RESOLVE_VERBOSE)
					e.printStackTrace();
				throw e;
			} catch (Error e) {
				if (JavaModelManager.CP_RESOLVE_VERBOSE)
					e.printStackTrace();
				throw e;
			} finally {
				if (!ok) JavaModelManager.getJavaModelManager().variablePut(variableName, null); // flush cache
			}
		} else {
			if (JavaModelManager.CP_RESOLVE_VERBOSE_ADVANCED)
				verbose_no_variable_initializer_found(variableName);
		}
		return variablePath;
	}

	private static void verbose_no_variable_initializer_found(String variableName) {
		Util.verbose(
			"CPVariable INIT - no initializer found\n" + //$NON-NLS-1$
			"	variable: " + variableName); //$NON-NLS-1$
	}

	private static void verbose_variable_value_after_initialization(String variableName, IPath variablePath) {
		Util.verbose(
			"CPVariable INIT - after initialization\n" + //$NON-NLS-1$
			"	variable: " + variableName +'\n' + //$NON-NLS-1$
			"	variable path: " + variablePath); //$NON-NLS-1$
	}

	private static void verbose_triggering_variable_initialization(String variableName, JsGlobalScopeVariableInitializer initializer) {
		Util.verbose(
			"CPVariable INIT - triggering initialization\n" + //$NON-NLS-1$
			"	variable: " + variableName + '\n' + //$NON-NLS-1$
			"	initializer: " + initializer); //$NON-NLS-1$
	}

	private static void verbose_triggering_variable_initialization_invocation_trace() {
		Util.verbose(
			"CPVariable INIT - triggering initialization\n" + //$NON-NLS-1$
			"	invocation trace:"); //$NON-NLS-1$
		new Exception("<Fake exception>").printStackTrace(System.out); //$NON-NLS-1$
	}



	/**
	 * Returns deprecation message of a given includepath variable.
	 *
	 * @param variableName
	 * @return A string if the includepath variable is deprecated, <code>null</code> otherwise.
	 */
	public static String getIncludepathVariableDeprecationMessage(String variableName) {
	    return (String) JavaModelManager.getJavaModelManager().deprecatedVariables.get(variableName);
	}

	/**
	 * Helper method finding the includepath variable initializer registered for a given includepath variable name
	 * or <code>null</code> if none was found while iterating over the contributions to extension point to
	 * the extension point "org.eclipse.wst.jsdt.core.JsGlobalScopeVariableInitializer".
	 * <p>
 	 * @param variable the given variable
 	 * @return JsGlobalScopeVariableInitializer - the registered includepath variable initializer or <code>null</code> if
	 * none was found.
 	 */
	public static JsGlobalScopeVariableInitializer getJsGlobalScopeVariableInitializer(String variable){

		Plugin jdtCorePlugin = JavaScriptCore.getPlugin();
		if (jdtCorePlugin == null) return null;

		IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(JavaScriptCore.PLUGIN_ID, JavaModelManager.CPVARIABLE_INITIALIZER_EXTPOINT_ID);
		if (extension != null) {
			IExtension[] extensions =  extension.getExtensions();
			for(int i = 0; i < extensions.length; i++){
				IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
				for(int j = 0; j < configElements.length; j++){
					IConfigurationElement configElement = configElements[j];
					try {
						String varAttribute = configElement.getAttribute("variable"); //$NON-NLS-1$
						if (variable.equals(varAttribute)) {
							if (JavaModelManager.CP_RESOLVE_VERBOSE_ADVANCED)
								verbose_found_variable_initializer(variable, configElement);
							Object execExt = configElement.createExecutableExtension("class"); //$NON-NLS-1$
							if (execExt instanceof JsGlobalScopeVariableInitializer){
								JsGlobalScopeVariableInitializer initializer = (JsGlobalScopeVariableInitializer)execExt;
								String deprecatedAttribute = configElement.getAttribute("deprecated"); //$NON-NLS-1$
								if (deprecatedAttribute != null) {
									JavaModelManager.getJavaModelManager().deprecatedVariables.put(variable, deprecatedAttribute);
								}
								String readOnlyAttribute = configElement.getAttribute("readOnly"); //$NON-NLS-1$
								if (JavaModelManager.TRUE.equals(readOnlyAttribute)) {
									JavaModelManager.getJavaModelManager().readOnlyVariables.add(variable);
								}
								return initializer;
							}
						}
					} catch(CoreException e){
						// executable extension could not be created: ignore this initializer
						if (JavaModelManager.CP_RESOLVE_VERBOSE) {
							verbose_failed_to_instanciate_variable_initializer(variable, configElement);
							e.printStackTrace();
						}
					}
				}
			}
		}
		return null;
	}

	private static void verbose_failed_to_instanciate_variable_initializer(String variable, IConfigurationElement configElement) {
		Util.verbose(
			"CPContainer INIT - failed to instanciate initializer\n" + //$NON-NLS-1$
			"	variable: " + variable + '\n' + //$NON-NLS-1$
			"	class: " + configElement.getAttribute("class"), //$NON-NLS-1$ //$NON-NLS-2$
			System.err);
	}

	private static void verbose_found_variable_initializer(String variable, IConfigurationElement configElement) {
		Util.verbose(
			"CPVariable INIT - found initializer\n" + //$NON-NLS-1$
			"	variable: " + variable + '\n' + //$NON-NLS-1$
			"	class: " + configElement.getAttribute("class")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Returns the names of all known includepath variables.
	 * <p>
	 * Includepath variable values are persisted locally to the workspace, and
	 * are preserved from session to session.
	 * <p>
	 *
	 * @return the list of includepath variable names
	 * @see #setIncludepathVariable(String, IPath)
	 */
	public static String[] getIncludepathVariableNames() {
		return JavaModelManager.getJavaModelManager().variableNames();
	}

	/**
	 * Returns a table of all known configurable options with their default values.
	 * These options allow to configure the behaviour of the underlying components.
	 * The client may safely use the result as a template that they can modify and
	 * then pass to <code>setOptions</code>.
	 *
	 * Helper constants have been defined on JavaScriptCore for each of the option ID and
	 * their possible constant values.
	 *
	 * Note: more options might be added in further releases.
	 * <pre>
	 * RECOGNIZED OPTIONS:
	 *
	 * VALIDATOR / Setting Compliance Level
	 *    Select the compliance level for the validator. In "1.3" mode, source and target settings
	 *    should not go beyond "1.3" level.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.compliance"
	 *     - possible values:   { "1.3", "1.4", "1.5", "1.6", "1.7" }
	 *     - default:           "1.4"
	 *
	 * VALIDATOR / Setting Source Compatibility Mode
	 *    Specify whether which source level compatibility is used. From 1.4 on, 'assert' is a keyword
	 *    reserved for assertion support. Also note, than when toggling to 1.4 mode, the target VM
	 *   level should be set to "1.4" and the compliance mode should be "1.4".
	 *   Source level 1.5 is necessary to enable generics, autoboxing, covariance, annotations, enumerations
	 *   enhanced for loop, static imports and varargs. Once toggled, the target VM level should be set to "1.5"
	 *   and the compliance mode should be "1.5".
	 *   Source level 1.6 is necessary to enable the computation of stack map tables. Once toggled, the target
	 *   VM level should be set to "1.6" and the compliance mode should be "1.6".
	 *   Once the source level 1.7 is toggled, the target VM level should be set to "1.7" and the compliance mode
	 *   should be "1.7".
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.source"
	 *     - possible values:   { "1.3", "1.4", "1.5", "1.6", "1.7" }
	 *     - default:           "1.3"
	 *
	 *
	 * VALIDATOR / JSdoc Comment Support
	 *    When this support is disabled, the validator will ignore all jsdoc problems options settings
	 *    and will not report any jsdoc problem. It will also not find any reference in jsdoc comment and
	 *    DOM AST JSdoc node will be only a flat text instead of having structured tag elements.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.doc.comment.support"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "enabled"
	 *
	 * VALIDATOR / Reporting Deprecation
	 *    When enabled, the validator will signal use of deprecated API either as an
	 *    error or a warning.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.deprecation"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "warning"
	 *
	 * VALIDATOR / Reporting Deprecation Inside Deprecated Code
	 *    When enabled, the validator will signal use of deprecated API inside deprecated code.
	 *    The severity of the problem is controlled with option "org.eclipse.wst.jsdt.core.compiler.problem.deprecation".
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.deprecationInDeprecatedCode"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "disabled"
	 *
	 * VALIDATOR / Reporting Deprecation When Overriding Deprecated Method
	 *    When enabled, the validator will signal the declaration of a method overriding a deprecated one.
	 *    The severity of the problem is controlled with option "org.eclipse.wst.jsdt.core.compiler.problem.deprecation".
	 *     - option id:        "org.eclipse.wst.jsdt.core.compiler.problem.deprecationWhenOverridingDeprecatedMethod"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "disabled"
	 *
	 * VALIDATOR / Reporting Unused Local
	 *    When enabled, the validator will issue an error or a warning for unused local
	 *    variables (that is, variables never read from)
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.unusedLocal"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Unused Parameter
	 *    When enabled, the validator will issue an error or a warning for unused method
	 *    parameters (that is, parameters never read from)
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.unusedParameter"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Unused Parameter if Implementing Abstract Method
	 *    When enabled, the validator will signal unused parameters in abstract method implementations.
	 *    The severity of the problem is controlled with option "org.eclipse.wst.jsdt.core.compiler.problem.unusedParameter".
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.unusedParameterWhenImplementingAbstract"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "disabled"
	 *
	 * VALIDATOR / Reporting Unused Parameter if Overriding Concrete Method
	 *    When enabled, the validator will signal unused parameters in methods overriding concrete ones.
	 *    The severity of the problem is controlled with option "org.eclipse.wst.jsdt.core.compiler.problem.unusedParameter".
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.unusedParameterWhenOverridingConcrete"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "disabled"
	 *
	 * VALIDATOR / Consider Reference in Doc Comment for Unused Parameter Check
	 *    When enabled, the validator will consider doc comment references to parameters (i.e. @param clauses) for the unused
	 *    parameter check. Thus, documented parameters will be considered as mandated as per doc contract.
	 *    The severity of the unused parameter problem is controlled with option "org.eclipse.wst.jsdt.core.compiler.problem.unusedParameter".
	 *    Note: this option has no effect until the doc comment support is enabled according to the
	 *    option "org.eclipse.wst.jsdt.core.compiler.doc.comment.support".
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.unusedParameterIncludeDocReference"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "enabled"
	 *
	 * VALIDATOR / Reporting Unused Import
	 *    When enabled, the validator will issue an error or a warning for unused import
	 *    reference
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.unusedImport"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "warning"
	 *
	 * VALIDATOR / Reporting Unused Private Members
	 *    When enabled, the validator will issue an error or a warning whenever a private
	 *    method or field is declared but never used within the same unit.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.unusedPrivateMember"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Assignment with no Effect
	 *    When enabled, the validator will issue an error or a warning whenever an assignment
	 *    has no effect (e.g 'x = x').
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.noEffectAssignment"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "warning"
	 *
	 * VALIDATOR / Reporting Empty Statements and Unnecessary Semicolons
	 *    When enabled, the validator will issue an error or a warning if an empty statement or a
	 *    unnecessary semicolon is encountered.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.emptyStatement"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Unnecessary Type Check
	 *    When enabled, the validator will issue an error or a warning when a cast or an instanceof operation
	 *    is unnecessary.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.unnecessaryTypeCheck"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Unnecessary Else
	 *    When enabled, the validator will issue an error or a warning when a statement is unnecessarily
	 *    nested within an else clause (in situation where then clause is not completing normally).
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.unnecessaryElse"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Non-Externalized String Literal
	 *    When enabled, the validator will issue an error or a warning for non externalized
	 *    String literal (that is, not tagged with //$NON-NLS-&lt;n&gt;$).
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.nonExternalizedStringLiteral"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Non-Static Reference to a Static Member
	 *    When enabled, the validator will issue an error or a warning whenever a static field
	 *    or method is accessed with an expression receiver. A reference to a static member should
	 *    be qualified with a type name.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.staticAccessReceiver"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "warning"
	 *
	 * VALIDATOR / Reporting Indirect Reference to a Static Member
	 *    When enabled, the validator will issue an error or a warning whenever a static field
	 *    or method is accessed in an indirect way. A reference to a static member should
	 *    preferably be qualified with its declaring type name.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.indirectStaticAccess"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Local Variable Declaration Hiding another Variable
	 *    When enabled, the validator will issue an error or a warning whenever a local variable
	 *    declaration is hiding some field or local variable (either locally, inherited or defined in enclosing type).
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.localVariableHiding"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Field Declaration Hiding another Variable
	 *    When enabled, the validator will issue an error or a warning whenever a field
	 *    declaration is hiding some field or local variable (either locally, inherited or defined in enclosing type).
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.fieldHiding"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Type Declaration Hiding another Type
	 *    When enabled, the validator will issue an error or a warning in situations where a type parameter
	 *    declaration is hiding some type, when a nested type is hiding some type parameter, or when
	 *    a nested type is hiding another nested type defined in same unit.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.typeParameterHiding"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "warning"
	 *
	 * VALIDATOR / Reporting Possible Accidental Boolean Assignment
	 *    When enabled, the validator will issue an error or a warning if a boolean assignment is acting as the condition
	 *    of a control statement  (where it probably was meant to be a boolean comparison).
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.possibleAccidentalBooleanAssignment"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Undocumented Empty Block
	 *    When enabled, the validator will issue an error or a warning when an empty block is detected and it is not
	 *    documented with any comment.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.undocumentedEmptyBlock"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Finally Blocks Not Completing Normally
	 *    When enabled, the validator will issue an error or a warning when a finally block does not complete normally.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.finallyBlockNotCompletingNormally"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "warning"
	 *
	 * VALIDATOR / Reporting Unused Declared Thrown Exception
	 *    When enabled, the validator will issue an error or a warning when a method or a constructor is declaring a
	 *    thrown checked exception, but never actually raises it in its body.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.unusedDeclaredThrownException"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Unused Declared Thrown Exception in Overridind Method
	 *    When disabled, the validator will not include overriding methods in its diagnosis for unused declared
	 *    thrown exceptions.
	 *    <br>
	 *    The severity of the problem is controlled with option "org.eclipse.wst.jsdt.core.compiler.problem.unusedDeclaredThrownException".
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.unusedDeclaredThrownExceptionWhenOverriding"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "disabled"
	 *
	 * VALIDATOR / Reporting Unqualified Access to Field
	 *    When enabled, the validator will issue an error or a warning when a field is access without any qualification.
	 *    In order to improve code readability, it should be qualified, e.g. 'x' should rather be written 'this.x'.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.unqualifiedFieldAccess"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Null Dereference
	 *    When enabled, the validator will issue an error or a warning whenever a
	 *    variable that is statically known to hold a null value is used to
	 *    access a field or method.
	 *
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.nullReference"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Potential Null Dereference
	 *    When enabled, the validator will issue an error or a warning whenever a
	 *    variable that has formerly been tested against null but is not (no more)
	 *    statically known to hold a non-null value is used to access a field or
	 *    method.
	 *
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.potentialNullReference"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 *	  VALIDATOR / Reporting Duplicate Local Variables
	 *    When enabled, the validator will issue an error or a warning whenever a
	 *    two local variables with the same name have been declared.
	 *
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.duplicateLocalVariables"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Redundant Null Check
	 *    When enabled, the validator will issue an error or a warning whenever a
	 *    variable that is statically known to hold a null or a non-null value
	 *    is tested against null.
	 *
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.redundantNullCheck"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Use of Annotation Type as Super Interface
	 *    When enabled, the validator will issue an error or a warning whenever an annotation type is used
	 *    as a super-interface. Though legal, this is discouraged.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.annotationSuperInterface"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "warning"
	 *
	 * VALIDATOR / Reporting Invalid Jsdoc Comment
	 *    This is the generic control for the severity of JSdoc problems.
	 *    When enabled, the validator will issue an error or a warning for a problem in JSdoc.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.invalidJavadoc"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Visibility Level For Invalid JSdoc Tags
	 *    Set the minimum visibility level for JSdoc tag problems. Below this level problems will be ignored.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.invalidJavadocTagsVisibility"
	 *     - possible values:   { "public", "protected", "default", "private" }
	 *     - default:           "public"
	 *
	 * VALIDATOR / Reporting Invalid JSdoc Tags
	 *    When enabled, the validator will signal unbound or unexpected reference tags in JSdoc.
	 *    A 'throws' tag referencing an undeclared exception would be considered as unexpected.
	 *    <br>Note that this diagnosis can be enabled based on the visibility of the construct associated with the JSDoc;
	 *    also see the setting "org.eclipse.wst.jsdt.core.compiler.problem.invalidJavadocTagsVisibility".
	 *    <br>
	 *    The severity of the problem is controlled with option "org.eclipse.wst.jsdt.core.compiler.problem.invalidJavadoc".
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.invalidJavadocTags"
	 *     - possible values:   { "disabled", "enabled" }
	 *     - default:           "disabled"
	 *
	 * VALIDATOR / Reporting Invalid JSdoc Tags with Deprecated References
	 *    Specify whether the validator will report deprecated references used in JSdoc tags.
	 *    <br>Note that this diagnosis can be enabled based on the visibility of the construct associated with the JSDoc;
	 *    also see the setting "org.eclipse.wst.jsdt.core.compiler.problem.invalidJavadocTagsVisibility".
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.invalidJavadocTagsDeprecatedRef"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "disabled"
	 *
	 * VALIDATOR / Reporting Invalid JSdoc Tags with Not Visible References
	 *    Specify whether the validator will report non-visible references used in JSDoc tags.
	 *    <br>Note that this diagnosis can be enabled based on the visibility of the construct associated with the JSDoc;
	 *    also see the setting "org.eclipse.wst.jsdt.core.compiler.problem.invalidJavadocTagsVisibility".
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.invalidJavadocTagsNotVisibleRef"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "disabled"
	 *
	 * VALIDATOR / Reporting Missing JSDoc Tags
	 *    This is the generic control for the severity of JSDoc missing tag problems.
	 *    When enabled, the validator will issue an error or a warning when tags are missing in JSDoc comments.
	 *    <br>Note that this diagnosis can be enabled based on the visibility of the construct associated with the JSDoc;
	 *    also see the setting "org.eclipse.wst.jsdt.core.compiler.problem.missingJavadocTagsVisibility".
	 *    <br>
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.missingJavadocTags"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Visibility Level For Missing JSDoc Tags
	 *    Set the minimum visibility level for JSDoc missing tag problems. Below this level problems will be ignored.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.missingJavadocTagsVisibility"
	 *     - possible values:   { "public", "protected", "default", "private" }
	 *     - default:           "public"
	 *
	 * VALIDATOR / Reporting Missing JSDoc Tags on Overriding Methods
	 *    Specify whether the validator will verify overriding methods in order to report JSDoc missing tag problems.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.missingJavadocTagsOverriding"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "disabled"
	 *
	 * VALIDATOR / Reporting Missing JSDoc Comments
	 *    This is the generic control for the severity of missing JSDoc comment problems.
	 *    When enabled, the validator will issue an error or a warning when JSDoc comments are missing.
	 *    <br>Note that this diagnosis can be enabled based on the visibility of the construct associated with the expected JSDoc;
	 *    also see the setting "org.eclipse.wst.jsdt.core.compiler.problem.missingJavadocCommentsVisibility".
	 *    <br>
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.missingJavadocComments"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Visibility Level For Missing JSDoc Comments
	 *    Set the minimum visibility level for missing JSDoc problems. Below this level problems will be ignored.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.missingJavadocCommentsVisibility"
	 *     - possible values:   { "public", "protected", "default", "private" }
	 *     - default:           "public"
	 *
	 * VALIDATOR / Reporting Missing JSDoc Comments on Overriding Methods
	 *    Specify whether the validator will verify overriding methods in order to report missing JSDoc comment problems.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.missingJavadocCommentsOverriding"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "disabled"
	 *
	 * VALIDATOR / Maximum Number of Problems Reported per JavaScript Unit
	 *    Specify the maximum number of problems reported on each javaScript unit.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.maxProblemPerUnit"
	 *     - possible values:	"&lt;n&gt;" where &lt;n&gt; is zero or a positive integer (if zero then all problems are reported).
	 *     - default:           "100"
	 *
	 * VALIDATOR / Treating Optional Error as Fatal
	 *    When enabled, optional errors (i.e. optional problems which severity is set to "error") will be treated as standard
	 *    validator errors, yielding problem methods/types preventing from running offending code until the issue got resolved.
	 *    When disabled, optional errors are only considered as warnings, still carrying an error indication to make them more
	 *    severe. Note that by default, errors are fatal, whether they are optional or not.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.fatalOptionalError"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "enabled"
	 *
	 * VALIDATOR / Defining the Automatic Task Tags
	 *    When the tag list is not empty, the validator will issue a task marker whenever it encounters
	 *    one of the corresponding tags inside any comment in JavaScript source code.
	 *    Generated task messages will start with the tag, and range until the next line separator,
	 *    comment ending, or tag.
	 *    When a given line of code bears multiple tags, each tag will be reported separately.
	 *    Moreover, a tag immediately followed by another tag will be reported using the contents of the
	 *    next non-empty tag of the line, if any.
	 *    Note that tasks messages are trimmed. If a tag is starting with a letter or digit, then it cannot be leaded by
	 *    another letter or digit to be recognized ("fooToDo" will not be recognized as a task for tag "ToDo", but "foo#ToDo"
	 *    will be detected for either tag "ToDo" or "#ToDo"). Respectively, a tag ending with a letter or digit cannot be followed
	 *    by a letter or digit to be recognized ("ToDofoo" will not be recognized as a task for tag "ToDo", but "ToDo:foo" will
	 *    be detected either for tag "ToDo" or "ToDo:").
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.taskTags"
	 *     - possible values:   { "&lt;tag&gt;[,&lt;tag&gt;]*" } where &lt;tag&gt; is a String without any wild-card or leading/trailing spaces
	 *     - default:           "TODO,FIXME,XXX"
	 *
	 * VALIDATOR / Defining the Automatic Task Priorities
	 *    In parallel with the Automatic Task Tags, this list defines the priorities (high, normal or low)
	 *    of the task markers issued by the validator.
	 *    If the default is specified, the priority of each task marker is "NORMAL".
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.taskPriorities"
	 *     - possible values:   { "&lt;priority&gt;[,&lt;priority&gt;]*" } where &lt;priority&gt; is one of "HIGH", "NORMAL" or "LOW"
	 *     - default:           "NORMAL,HIGH,NORMAL"
	 *
	 * VALIDATOR / Determining whether task tags are case-sensitive
	 *    When enabled, task tags are considered in a case-sensitive way.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.taskCaseSensitive"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "enabled"
	 *
	 * VALIDATOR / Reporting Discouraged Reference to Type with Restricted Access
	 *    When enabled, the validator will issue an error or a warning when referring to a type with discouraged access, as defined according
	 *    to the access rule specifications.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.discouragedReference"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "warning"
	 *
	 * VALIDATOR / Reporting Unreferenced Label
	 *    When enabled, the validator will issue an error or a warning when encountering a labeled statement which label
	 *    is never explicitly referenced. A label is considered to be referenced if its name explicitly appears behind a break
	 *    or continue statement; for instance the following label would be considered unreferenced;   LABEL: { break; }
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.unusedLabel"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "warning"
	 *
	 * VALIDATOR / Reporting Parameter Assignment
	 *    When enabled, the validator will issue an error or a warning if a parameter is
	 *    assigned to.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.parameterAssignment"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Switch Fall-Through Case
	 *    When enabled, the validator will issue an error or a warning if a case may be
	 *    entered by falling through previous case. Empty cases are allowed.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.fallthroughCase"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * VALIDATOR / Reporting Overriding method that doesn't call the super method invocation
	 *    When enabled, the validator will issue an error or a warning if a method is overriding a method without calling
	 *    the super invocation.
	 *     - option id:         "org.eclipse.wst.jsdt.core.compiler.problem.overridingMethodWithoutSuperInvocation"
	 *     - possible values:   { "error", "warning", "ignore" }
	 *     - default:           "ignore"
	 *
	 * BUILDER / Specifying Filters for Resource Copying Control
	 *    Allow to specify some filters to control the resource copy process.
	 *     - option id:         "org.eclipse.wst.jsdt.core.builder.resourceCopyExclusionFilter"
	 *     - possible values:   { "&lt;name&gt;[,&lt;name&gt;]* } where &lt;name&gt; is a file name pattern (* and ? wild-cards allowed)
	 *       or the name of a folder which ends with '/'
	 *     - default:           ""
	 *
	 * BUILDER / Abort if Invalid Includepath
	 *    Allow to toggle the builder to abort if the includepath is invalid
	 *     - option id:         "org.eclipse.wst.jsdt.core.builder.invalidClasspath"
	 *     - possible values:   { "abort", "ignore" }
	 *     - default:           "abort"
	 *
	 * BUILDER / Reporting Duplicate Resources
	 *    Indicate the severity of the problem reported when more than one occurrence
	 *    of a resource is to be copied into the output location.
	 *     - option id:         "org.eclipse.wst.jsdt.core.builder.duplicateResourceTask"
	 *     - possible values:   { "error", "warning" }
	 *     - default:           "warning"
	 *
	 * JAVACORE / Computing Project Build Order
	 *    Indicate whether JavaScriptCore should enforce the project build order to be based on
	 *    the includepath prerequisite chain. When requesting to compute, this takes over
	 *    the platform default order (based on project references).
	 *     - option id:         "org.eclipse.wst.jsdt.core.computeJavaBuildOrder"
	 *     - possible values:   { "compute", "ignore" }
	 *     - default:           "ignore"
	 *
	 * JAVACORE / Default Source Encoding Format
	 *    Get the default encoding format of source files. This value is
	 *    immutable and preset to the result of ResourcesPlugin.getEncoding().
	 *    It is offered as a convenience shortcut only.
	 *     - option id:         "org.eclipse.wst.jsdt.core.encoding"
	 *     - value:           &lt;immutable, platform default value&gt;
	 *
	 * JAVACORE / Reporting Incomplete Includepath
	 *    Indicate the severity of the problem reported when an entry on the includepath does not exist,
	 *    is not legite or is not visible (for example, a referenced project is closed).
	 *     - option id:         "org.eclipse.wst.jsdt.core.incompleteClasspath"
	 *     - possible values:   { "error", "warning"}
	 *     - default:           "error"
	 *
	 * JAVACORE / Reporting Includepath Cycle
	 *    Indicate the severity of the problem reported when a project is involved in a cycle.
	 *     - option id:         "org.eclipse.wst.jsdt.core.circularClasspath"
	 *     - possible values:   { "error", "warning" }
	 *     - default:           "error"
	 * JAVACORE / Enabling Usage of Includepath Exclusion Patterns
	 *    When disabled, no entry on a project includepath can be associated with
	 *    an exclusion pattern.
	 *     - option id:         "org.eclipse.wst.jsdt.core.includepath.exclusionPatterns"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "enabled"
	 *
	 * JAVACORE / Enabling Usage of Includepath Multiple Output Locations
	 *    When disabled, no entry on a project includepath can be associated with
	 *    a specific output location, preventing thus usage of multiple output locations.
	 *     - option id:         "org.eclipse.wst.jsdt.core.includepath.multipleOutputLocations"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "enabled"
	 *
	 * JAVACORE / Set the timeout value for retrieving the method's parameter names from jsdoc
	 *    Timeout in milliseconds to retrieve the method's parameter names from jsdoc.
	 *    If the value is 0, the parameter names are not fetched and the raw names are returned.
	 *     - option id:         "org.eclipse.wst.jsdt.core.timeoutForParameterNameFromAttachedJavadoc"
	 *     - possible values:	"&lt;n&gt;", where n is an integer greater than or equal to 0
	 *     - default:           "50"
	 *
	 * DEPRECATED SEE DefaultCodeFormatterOptions: FORMATTER / Inserting New Line Before Opening Brace
	 *    When Insert, a new line is inserted before an opening brace, otherwise nothing
	 *    is inserted
	 *     - option id:         "org.eclipse.wst.jsdt.core.formatter.newline.openingBrace"
	 *     - possible values:   { "insert", "do not insert" }
	 *     - default:           "do not insert"
	 *
	 * DEPRECATED SEE DefaultCodeFormatterOptions: FORMATTER / Inserting New Line Inside Control Statement
	 *    When Insert, a new line is inserted between } and following else, catch, finally
	 *     - option id:         "org.eclipse.wst.jsdt.core.formatter.newline.controlStatement"
	 *     - possible values:   { "insert", "do not insert" }
	 *     - default:           "do not insert"
	 *
	 * DEPRECATED SEE DefaultCodeFormatterOptions: Clearing Blank Lines
	 *    When Clear all, all blank lines are removed. When Preserve one, only one is kept
	 *    and all others removed.
	 *     - option id:         "org.eclipse.wst.jsdt.core.formatter.newline.clearAll"
	 *     - possible values:   { "clear all", "preserve one" }
	 *     - default:           "preserve one"
	 *
	 * DEPRECATED SEE DefaultCodeFormatterOptions: Inserting New Line Between Else/If
	 *    When Insert, a blank line is inserted between an else and an if when they are
	 *    contiguous. When choosing to not insert, else-if will be kept on the same
	 *    line when possible.
	 *     - option id:         "org.eclipse.wst.jsdt.core.formatter.newline.elseIf"
	 *     - possible values:   { "insert", "do not insert" }
	 *     - default:           "do not insert"
	 *
	 * DEPRECATED SEE DefaultCodeFormatterOptions: Inserting New Line In Empty Block
	 *    When insert, a line break is inserted between contiguous { and }, if } is not followed
	 *    by a keyword.
	 *     - option id:         "org.eclipse.wst.jsdt.core.formatter.newline.emptyBlock"
	 *     - possible values:   { "insert", "do not insert" }
	 *     - default:           "insert"
	 *
	 * DEPRECATED SEE DefaultCodeFormatterOptions: Splitting Lines Exceeding Length
	 *    Enable splitting of long lines (exceeding the configurable length). Length of 0 will
	 *    disable line splitting
	 *     - option id:         "org.eclipse.wst.jsdt.core.formatter.lineSplit"
	 *     - possible values:	"&lt;n&gt;", where n is zero or a positive integer
	 *     - default:           "80"
	 *
	 * DEPRECATED SEE DefaultCodeFormatterOptions: Compacting Assignment
	 *    Assignments can be formatted asymmetrically, for example 'int x= 2;', when Normal, a space
	 *    is inserted before the assignment operator
	 *     - option id:         "org.eclipse.wst.jsdt.core.formatter.style.assignment"
	 *     - possible values:   { "compact", "normal" }
	 *     - default:           "normal"
	 *
	 * DEPRECATED SEE DefaultCodeFormatterOptions: Defining Indentation Character
	 *    Either choose to indent with tab characters or spaces
	 *     - option id:         "org.eclipse.wst.jsdt.core.formatter.tabulation.char"
	 *     - possible values:   { "tab", "space" }
	 *     - default:           "tab"
	 *
	 * DEPRECATED SEE DefaultCodeFormatterOptions: Defining Space Indentation Length
	 *    When using spaces, set the amount of space characters to use for each
	 *    indentation mark.
	 *     - option id:         "org.eclipse.wst.jsdt.core.formatter.tabulation.size"
	 *     - possible values:	"&lt;n&gt;", where n is a positive integer
	 *     - default:           "4"
	 *
	 * DEPRECATED SEE DefaultCodeFormatterOptions: Inserting space in cast expression
	 *    When Insert, a space is added between the type and the expression in a cast expression.
	 *     - option id:         "org.eclipse.wst.jsdt.core.formatter.space.castexpression"
	 *     - possible values:   { "insert", "do not insert" }
	 *     - default:           "insert"
	 *
	 * CODEASSIST / Activate Visibility Sensitive Completion
	 *    When active, completion doesn't show that you can not see
	 *    (for example, you can not see private methods of a super class).
	 *     - option id:         "org.eclipse.wst.jsdt.core.codeComplete.visibilityCheck"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "disabled"
	 *
	 * CODEASSIST / Activate Deprecation Sensitive Completion
	 *    When enabled, completion doesn't propose deprecated members and types.
	 *     - option id:         "org.eclipse.wst.jsdt.core.codeComplete.deprecationCheck"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "disabled"
	 *
	 * CODEASSIST / Automatic Qualification of Implicit Members
	 *    When active, completion automatically qualifies completion on implicit
	 *    field references and message expressions.
	 *     - option id:         "org.eclipse.wst.jsdt.core.codeComplete.forceImplicitQualification"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "disabled"
	 *
	 * CODEASSIST / Define the Prefixes for Field Name
	 *    When the prefixes is non empty, completion for field name will begin with
	 *    one of the proposed prefixes.
	 *     - option id:         "org.eclipse.wst.jsdt.core.codeComplete.fieldPrefixes"
	 *     - possible values:   { "&lt;prefix&gt;[,&lt;prefix&gt;]*" } where &lt;prefix&gt; is a String without any wild-card
	 *     - default:           ""
	 *
	 * CODEASSIST / Define the Prefixes for Static Field Name
	 *    When the prefixes is non empty, completion for static field name will begin with
	 *    one of the proposed prefixes.
	 *     - option id:         "org.eclipse.wst.jsdt.core.codeComplete.staticFieldPrefixes"
	 *     - possible values:   { "&lt;prefix&gt;[,&lt;prefix&gt;]*" } where &lt;prefix&gt; is a String without any wild-card
	 *     - default:           ""
	 *
	 * CODEASSIST / Define the Prefixes for Local Variable Name
	 *    When the prefixes is non empty, completion for local variable name will begin with
	 *    one of the proposed prefixes.
	 *     - option id:         "org.eclipse.wst.jsdt.core.codeComplete.localPrefixes"
	 *     - possible values:   { "&lt;prefix&gt;[,&lt;prefix&gt;]*" } where &lt;prefix&gt; is a String without any wild-card
	 *     - default:           ""
	 *
	 * CODEASSIST / Define the Prefixes for Argument Name
	 *    When the prefixes is non empty, completion for argument name will begin with
	 *    one of the proposed prefixes.
	 *     - option id:         "org.eclipse.wst.jsdt.core.codeComplete.argumentPrefixes"
	 *     - possible values:   { "&lt;prefix&gt;[,&lt;prefix&gt;]*" } where &lt;prefix&gt; is a String without any wild-card
	 *     - default:           ""
	 *
	 * CODEASSIST / Define the Suffixes for Field Name
	 *    When the suffixes is non empty, completion for field name will end with
	 *    one of the proposed suffixes.
	 *     - option id:         "org.eclipse.wst.jsdt.core.codeComplete.fieldSuffixes"
	 *     - possible values:   { "&lt;suffix&gt;[,&lt;suffix&gt;]*" } where &lt;suffix&gt; is a String without any wild-card
	 *     - default:           ""
	 *
	 * CODEASSIST / Define the Suffixes for Static Field Name
	 *    When the suffixes is non empty, completion for static field name will end with
	 *    one of the proposed suffixes.
	 *     - option id:         "org.eclipse.wst.jsdt.core.codeComplete.staticFieldSuffixes"
	 *     - possible values:   { "&lt;suffix&gt;[,&lt;suffix&gt;]*" } where &lt;suffix&gt; is a String without any wild-card
	 *     - default:           ""
	 *
	 * CODEASSIST / Define the Suffixes for Local Variable Name
	 *    When the suffixes is non empty, completion for local variable name will end with
	 *    one of the proposed suffixes.
	 *     - option id:         "org.eclipse.wst.jsdt.core.codeComplete.localSuffixes"
	 *     - possible values:   { "&lt;suffix&gt;[,&lt;suffix&gt;]*" } where &lt;suffix&gt; is a String without any wild-card
	 *     - default:           ""
	 *
	 * CODEASSIST / Define the Suffixes for Argument Name
	 *    When the suffixes is non empty, completion for argument name will end with
	 *    one of the proposed suffixes.
	 *     - option id:         "org.eclipse.wst.jsdt.core.codeComplete.argumentSuffixes"
	 *     - possible values:   { "&lt;suffix&gt;[,&lt;suffix&gt;]*" } where &lt;suffix&gt; is a String without any wild-card
	 *     - default:           ""
	 *
	 * CODEASSIST / Activate Forbidden Reference Sensitive Completion
	 *    When enabled, completion doesn't propose elements which match a
  	 *    forbidden reference rule.
	 *     - option id:         "org.eclipse.wst.jsdt.core.codeComplete.forbiddenReferenceCheck"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "enabled"
	 *
	 * CODEASSIST / Activate Discouraged Reference Sensitive Completion
	 *    When enabled, completion doesn't propose elements which match a
  	 *    discouraged reference rule.
	 *     - option id:         "org.eclipse.wst.jsdt.core.codeComplete.discouragedReferenceCheck"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "disabled"
	 *
	 * CODEASSIST / Activate Camel Case Sensitive Completion
	 *    When enabled, completion shows proposals whose name match the CamelCase
	 *    pattern.
	 *     - option id:         "org.eclipse.wst.jsdt.core.codeComplete.camelCaseMatch"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "enabled"
	 *
	 * CODEASSIST / Activate Suggestion of Static Import
	 *    When enabled, completion proposals can contain static import
	 *    pattern.
	 *     - option id:         "org.eclipse.wst.jsdt.core.codeComplete.suggestStaticImports"
	 *     - possible values:   { "enabled", "disabled" }
	 *     - default:           "enabled"
	 * </pre>
	 */
 	public static Hashtable getDefaultOptions(){
 		return JavaModelManager.getJavaModelManager().getDefaultOptions();
	}

	/**
	 * Returns the workspace root default charset encoding.
	 *
	 * @return the name of the default charset encoding for workspace root.
	 * @see IContainer#getDefaultCharset()
	 * @see ResourcesPlugin#getEncoding()
	 */
	public static String getEncoding() {
		// Verify that workspace is not shutting down (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=60687)
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		if (workspace != null) {
			try {
				return workspace.getRoot().getDefaultCharset();
			} catch (CoreException e) {
				// fails silently and return plugin global encoding if core exception occurs
			}
		}
		return ResourcesPlugin.getEncoding();
	}

	/**
	 * Returns an array that contains the resources generated by the JavaScript builder when building the
	 * javaScript units contained in the given region.
	 * <p>The contents of the array is accurate only if the elements of the given region have been built.</p>
	 * <p>The given region can contain instances of:</p>
	 * <ul>
	 * <li><code>org.eclipse.wst.jsdt.core.IJavaScriptUnit</code></li>
	 * <li><code>org.eclipse.wst.jsdt.core.IPackageFragment</code></li>
	 * <li><code>org.eclipse.wst.jsdt.core.IPackageFragmentRoot</code></li>
	 * <li><code>org.eclipse.wst.jsdt.core.IJavaScriptProject</code></li>
	 * </ul>
	 * <p>All other types of <code>org.eclipse.wst.jsdt.core.IJavaScriptElement</code> are ignored.</p>
	 *
	 * @param region the given region
	 * @param includesNonJavaResources a flag that indicates if non-javaScript resources should be included
	 *
	 * @return an array that contains the resources generated by the JavaScript builder when building the
	 * javaScript units contained in the given region, an empty array if none
	 * @exception IllegalArgumentException if the given region is <code>null</code>
	 */
	public static IResource[] getGeneratedResources(IRegion region, boolean includesNonJavaResources) {
		if (region == null) throw new IllegalArgumentException("region cannot be null"); //$NON-NLS-1$
		IJavaScriptElement[] elements = region.getElements();
		HashMap projectsStates = new HashMap();
		ArrayList collector = new ArrayList();
		for (int i = 0, max = elements.length; i < max; i++) {
			// collect all the javaScript project
			IJavaScriptElement element = elements[i];
			IJavaScriptProject javaProject = element.getJavaScriptProject();
			IProject project = javaProject.getProject();
			State state = null;
			State currentState = (State) projectsStates.get(project);
			if (currentState != null) {
				state = currentState;
			} else {
				state = (State) JavaModelManager.getJavaModelManager().getLastBuiltState(project, null);
				if (state != null) {
					projectsStates.put(project, state);
				}
			}
			if (state == null) continue;
			if (element.getElementType() == IJavaScriptElement.JAVASCRIPT_PROJECT) {
				IPackageFragmentRoot[] roots = null;
				try {
					roots = javaProject.getPackageFragmentRoots();
				} catch (JavaScriptModelException e) {
					// ignore
				}
				if (roots == null) continue;
				IRegion region2 = JavaScriptCore.newRegion();
				for (int j = 0; j < roots.length; j++) {
					region2.add(roots[j]);
				}
				IResource[] res = getGeneratedResources(region2, includesNonJavaResources);
				for (int j = 0, max2 = res.length; j < max2; j++) {
					collector.add(res[j]);
				}
				continue;
			}
			
			IJavaScriptElement root = element;
			while (root != null && root.getElementType() != IJavaScriptElement.PACKAGE_FRAGMENT_ROOT) {
				root = root.getParent();
			}
			if (root == null) continue;
		}
		int size = collector.size();
		if (size != 0) {
			IResource[] result = new IResource[size];
			collector.toArray(result);
			return result;
		}
		return NO_GENERATED_RESOURCES;
	}

	/**
	 * Returns the single instance of the JavaScript core plug-in runtime class.
	 * Equivalent to <code>(JavaScriptCore) getPlugin()</code>.
	 *
	 * @return the single instance of the JavaScript core plug-in runtime class
	 */
	public static JavaScriptCore getJavaScriptCore() {
		return (JavaScriptCore) getPlugin();
	}

	/**
	 * Returns the list of known JavaScript-like extensions.
	 * JavaScript like extension are defined in the {@link org.eclipse.core.runtime.Platform#getContentTypeManager()
	 * content type manager} for the {@link #JAVA_SOURCE_CONTENT_TYPE}.
	 * Note that a JavaScript-like extension doesn't include the leading dot ('.').
	 * Also note that the "js" extension is always defined as a JavaScript-like extension.
	 *
	 * @return the list of known JavaScript-like extensions.
	 */
	public static String[] getJavaScriptLikeExtensions() {
		return CharOperation.toStrings(Util.getJavaLikeExtensions());
	}

	/**
	 * Helper method for returning one option value only. Equivalent to <code>(String)JavaScriptCore.getOptions().get(optionName)</code>
	 * Note that it may answer <code>null</code> if this option does not exist.
	 * <p>
	 * For a complete description of the configurable options, see <code>getDefaultOptions</code>.
	 * </p>
	 *
	 * @param optionName the name of an option
	 * @return the String value of a given option
	 * @see JavaScriptCore#getDefaultOptions()
	 * @see org.eclipse.wst.jsdt.internal.core.JavaCorePreferenceInitializer for changing default settings
	 */
	public static String getOption(String optionName) {
		return JavaModelManager.getJavaModelManager().getOption(optionName);
	}

	/**
	 * Returns the table of the current options. Initially, all options have their default values,
	 * and this method returns a table that includes all known options.
	 * <p>For a complete description of the configurable options, see <code>getDefaultOptions</code>.</p>
	 * <p>Returns a default set of options even if the platform is not running.</p>
	 *
	 * @return table of current settings of all options
	 *   (key type: <code>String</code>; value type: <code>String</code>)
	 * @see #getDefaultOptions()
	 * @see org.eclipse.wst.jsdt.internal.core.JavaCorePreferenceInitializer for changing default settings
	 */
	public static Hashtable getOptions() {
		return JavaModelManager.getJavaModelManager().getOptions();
	}

	/**
	 * Returns the single instance of the JavaScript core plug-in runtime class.
	 *
	 * @return the single instance of the JavaScript core plug-in runtime class
	 */
	public static Plugin getPlugin() {
		return JAVA_CORE_PLUGIN;
	}

	/**
	 * This is a helper method, which returns the resolved includepath entry denoted
	 * by a given entry (if it is a variable entry). It is obtained by resolving the variable
	 * reference in the first segment. Returns <code>null</code> if unable to resolve using
	 * the following algorithm:
	 * <ul>
	 * <li> if variable segment cannot be resolved, returns <code>null</code></li>
	 * <li> finds a project, JAR or binary folder in the workspace at the resolved path location</li>
	 * <li> if none finds an external JAR file or folder outside the workspace at the resolved path location </li>
	 * <li> if none returns <code>null</code></li>
	 * </ul>
	 * <p>
	 * Variable source attachment path and root path are also resolved and recorded in the resulting includepath entry.
	 * <p>
	 * NOTE: This helper method does not handle includepath containers, for which should rather be used
	 * <code>JavaScriptCore#getJsGlobalScopeContainer(IPath, IJavaScriptProject)</code>.
	 * <p>
	 *
	 * @param entry the given variable entry
	 * @return the resolved library or project includepath entry, or <code>null</code>
	 *   if the given variable entry could not be resolved to a valid includepath entry
	 */
	public static IIncludePathEntry getResolvedIncludepathEntry(IIncludePathEntry entry) {

		if (entry.getEntryKind() != IIncludePathEntry.CPE_VARIABLE)
			return entry;

		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IPath resolvedPath = JavaScriptCore.getResolvedVariablePath(entry.getPath());
		if (resolvedPath == null)
			return null;

		Object target = JavaModel.getTarget(workspaceRoot, resolvedPath, false);
		if (target == null)
			return null;

		// inside the workspace
		if (target instanceof IResource) {
			IResource resolvedResource = (IResource) target;
			switch (resolvedResource.getType()) {

				case IResource.PROJECT :
					// internal project
					return JavaScriptCore.newProjectEntry(
							resolvedPath,
							entry.getAccessRules(),
							entry.combineAccessRules(),
							entry.getExtraAttributes(),
							entry.isExported());
				case IResource.FILE :
					if (org.eclipse.wst.jsdt.internal.compiler.util.Util.isArchiveFileName(resolvedResource.getName())) {
						// internal binary archive
						return JavaScriptCore.newLibraryEntry(
								resolvedPath,
								getResolvedVariablePath(entry.getSourceAttachmentPath()),
								getResolvedVariablePath(entry.getSourceAttachmentRootPath()),
								entry.getAccessRules(),
								entry.getExtraAttributes(),
								entry.isExported());
					}
					break;

				case IResource.FOLDER :
					// internal binary folder
					return JavaScriptCore.newLibraryEntry(
							resolvedPath,
							getResolvedVariablePath(entry.getSourceAttachmentPath()),
							getResolvedVariablePath(entry.getSourceAttachmentRootPath()),
							entry.getAccessRules(),
							entry.getExtraAttributes(),
							entry.isExported());
			}
		}
		// outside the workspace
		if (target instanceof File) {
			File externalFile = JavaModel.getFile(target);
			if (externalFile != null) {
				String fileName = externalFile.getName().toLowerCase();
				if (fileName.endsWith(SuffixConstants.SUFFIX_STRING_java)
						|| fileName.endsWith(SuffixConstants.SUFFIX_STRING_zip)
						) {
					// external binary archive
					return JavaScriptCore.newLibraryEntry(
							resolvedPath,
							getResolvedVariablePath(entry.getSourceAttachmentPath()),
							getResolvedVariablePath(entry.getSourceAttachmentRootPath()),
							entry.getAccessRules(),
							entry.getExtraAttributes(),
							entry.isExported());
				}
			} else { // external binary folder
				if (resolvedPath.isAbsolute()){
					return JavaScriptCore.newLibraryEntry(
							resolvedPath,
							getResolvedVariablePath(entry.getSourceAttachmentPath()),
							getResolvedVariablePath(entry.getSourceAttachmentRootPath()),
							entry.getAccessRules(),
							entry.getExtraAttributes(),
							entry.isExported());
				}
			}
		}
		return null;
	}


	/**
	 * Resolve a variable path (helper method).
	 *
	 * @param variablePath the given variable path
	 * @return the resolved variable path or <code>null</code> if none
	 */
	public static IPath getResolvedVariablePath(IPath variablePath) {

		if (variablePath == null)
			return null;
		int count = variablePath.segmentCount();
		if (count == 0)
			return null;

		// lookup variable
		String variableName = variablePath.segment(0);
		IPath resolvedPath = JavaScriptCore.getIncludepathVariable(variableName);
		if (resolvedPath == null)
			return null;

		// append path suffix
		if (count > 1) {
			resolvedPath = resolvedPath.append(variablePath.removeFirstSegments(1));
		}
		return resolvedPath;
	}

	/**
	 * Returns the names of all defined user libraries. The corresponding includepath container path
	 * is the name appended to the USER_LIBRARY_CONTAINER_ID.
	 * @return Return an array containing the names of all known user defined.
	 */
	public static String[] getUserLibraryNames() {
		 return UserLibraryManager.getUserLibraryNames();
	}

	/**
	 * Returns the working copies that have the given owner.
	 * Only javaScript units in working copy mode are returned.
	 * If the owner is <code>null</code>, primary working copies are returned.
	 *
	 * @param owner the given working copy owner or <code>null</code> for primary working copy owner
	 * @return the list of working copies for a given owner
	 */
	public static IJavaScriptUnit[] getWorkingCopies(WorkingCopyOwner owner){

		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		if (owner == null) owner = DefaultWorkingCopyOwner.PRIMARY;
		IJavaScriptUnit[] result = manager.getWorkingCopies(owner, false/*don't add primary WCs*/);
		if (result == null) return JavaModelManager.NO_WORKING_COPY;
		return result;
	}

	/**
	 * Initializes JavaScriptCore internal structures to allow subsequent operations (such
	 * as the ones that need a resolved classpath) to run full speed. A client may
	 * choose to call this method in a background thread early after the workspace
	 * has started so that the initialization is transparent to the user.
	 * <p>
	 * However calling this method is optional. Services will lazily perform
	 * initialization when invoked. This is only a way to reduce initialization
	 * overhead on user actions, if it can be performed before at some
	 * appropriate moment.
	 * </p><p>
	 * This initialization runs accross all JavaScript projects in the workspace. Thus the
	 * workspace root scheduling rule is used during this operation.
	 * </p><p>
	 * This method may return before the initialization is complete. The
	 * initialization will then continue in a background thread.
	 * </p><p>
	 * This method can be called concurrently.
	 * </p>
	 *
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @exception CoreException if the initialization fails,
	 * 		the status of the exception indicates the reason of the failure
	 */
	public static void initializeAfterLoad(IProgressMonitor monitor) throws CoreException {
		try {
			if (monitor != null) 	monitor.beginTask(Messages.javamodel_initialization, 100);

			// initialize all containers and variables
			JavaModelManager manager = JavaModelManager.getJavaModelManager();
			try {
				if (monitor != null) {
					monitor.subTask(Messages.javamodel_configuring_classpath_containers);
					manager.batchContainerInitializationsProgress.set(new SubProgressMonitor(monitor, 50)); // 50% of the time is spent in initializing containers and variables
				}

				// all classpaths in the workspace are going to be resolved, ensure that containers are initialized in one batch
				manager.batchContainerInitializations = true;

				// avoid leaking source attachment properties (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=183413)
				IJavaScriptProject[] projects = manager.getJavaModel().getJavaScriptProjects();
				for (int i = 0, length = projects.length; i < length; i++) {
					IIncludePathEntry[] classpath;
					try {
						classpath = ((JavaProject) projects[i]).getResolvedClasspath();
					} catch (JavaScriptModelException e) {
						// project no longer exist: ignore
						continue;
					}
					if (classpath != null) {
						for (int j = 0, length2 = classpath.length; j < length2; j++) {
							IIncludePathEntry entry = classpath[j];
							if (entry.getSourceAttachmentPath() != null)
								Util.setSourceAttachmentProperty(entry.getPath(), null);
							// else source might have been attached by IPackageFragmentRoot#attachSource(...), we keep it
						}
					}
				}

				// initialize delta state
				manager.deltaState.rootsAreStale = true; // in case it was already initialized before we cleaned up the source attachment proprties
				manager.deltaState.initializeRoots();
			} finally {
				manager.batchContainerInitializationsProgress.set(null);
			}

			// dummy query for waiting until the indexes are ready
			SearchEngine engine = new SearchEngine();
			IJavaScriptSearchScope scope = SearchEngine.createWorkspaceScope();
			try {
				if (monitor != null)
					monitor.subTask(Messages.javamodel_configuring_searchengine);
				engine.searchAllTypeNames(
					null,
					SearchPattern.R_EXACT_MATCH,
					"!@$#!@".toCharArray(), //$NON-NLS-1$
					SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE,
					IJavaScriptSearchConstants.CLASS,
					scope,
					new TypeNameRequestor() {
						public void acceptType(
							int modifiers,
							char[] packageName,
							char[] simpleTypeName,
							char[][] enclosingTypeNames,
							String path) {
							// no type to accept
						}
					},
					// will not activate index query caches if indexes are not ready, since it would take to long
					// to wait until indexes are fully rebuild
					IJavaScriptSearchConstants.CANCEL_IF_NOT_READY_TO_SEARCH,
					monitor == null ? null : new SubProgressMonitor(monitor, 49) // 49% of the time is spent in the dummy search
				);
			} catch (JavaScriptModelException e) {
				// /search failed: ignore
			} catch (OperationCanceledException e) {
				if (monitor != null && monitor.isCanceled())
					throw e;
				// else indexes were not ready: catch the exception so that jars are still refreshed
			}

			// check if the build state version number has changed since last session
			// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=98969)
			if (monitor != null)
				monitor.subTask(Messages.javamodel_getting_build_state_number);
			QualifiedName qName = new QualifiedName(JavaScriptCore.PLUGIN_ID, "stateVersionNumber"); //$NON-NLS-1$
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			String versionNumber = null;
			try {
				versionNumber = root.getPersistentProperty(qName);
			} catch (CoreException e) {
				// could not read version number: consider it is new
			}
			final JavaModel model = manager.getJavaModel();
			String newVersionNumber = Byte.toString(State.VERSION);
			if (!newVersionNumber.equals(versionNumber)) {
				// build state version number has changed: touch every projects to force a rebuild
				if (JavaBuilder.DEBUG)
					System.out.println("Build state version number has changed"); //$NON-NLS-1$
				IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
					public void run(IProgressMonitor progressMonitor2) throws CoreException {
						IJavaScriptProject[] projects = null;
						try {
							projects = model.getJavaScriptProjects();
						} catch (JavaScriptModelException e) {
							// could not get JavaScript projects: ignore
						}
						if (projects != null) {
							for (int i = 0, length = projects.length; i < length; i++) {
								IJavaScriptProject project = projects[i];
								try {
									if (JavaBuilder.DEBUG)
										System.out.println("Touching " + project.getElementName()); //$NON-NLS-1$
									project.getProject().touch(progressMonitor2);
								} catch (CoreException e) {
									// could not touch this project: ignore
								}
							}
						}
					}
				};
				if (monitor != null)
					monitor.subTask(Messages.javamodel_building_after_upgrade);
				try {
					ResourcesPlugin.getWorkspace().run(runnable, monitor);
				} catch (CoreException e) {
					// could not touch all projects
				}
				try {
					root.setPersistentProperty(qName, newVersionNumber);
				} catch (CoreException e) {
					Util.log(e, "Could not persist build state version number"); //$NON-NLS-1$
				}
			}

			// ensure external jars are refreshed (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=93668)
			try {
				if (monitor != null)
					monitor.subTask(Messages.javamodel_refreshing_external_jars);
				model.refreshExternalArchives(
					null/*refresh all projects*/,
					monitor == null ? null : new SubProgressMonitor(monitor, 1) // 1% of the time is spent in jar refresh
				);
			} catch (JavaScriptModelException e) {
				// refreshing failed: ignore
			}

		} finally {
			if (monitor != null) monitor.done();
		}
	}
	/**
	 * Returns whether a given includepath variable is read-only or not.
	 *
	 * @param variableName
	 * @return <code>true</code> if the includepath variable is read-only,
	 * 	<code>false</code> otherwise.
	 */
	public static boolean isIncludepathVariableReadOnly(String variableName) {
	    return JavaModelManager.getJavaModelManager().readOnlyVariables.contains(variableName);
	}

	/**
	 * Returns whether the given file name's extension is a JavaScript-like extension.
	 *
	 * @return whether the given file name's extension is a JavaScript-like extension
	 * @see #getJavaScriptLikeExtensions()
	 */
	public static boolean isJavaScriptLikeFileName(String fileName) {
		if(fileName==null) return false;
		return Util.isJavaLikeFileName(fileName);
	}

	/**
	 * Returns whether the given marker references the given JavaScript element.
	 * Used for markers, which denote a JavaScript element rather than a resource.
	 *
	 * @param element the element
	 * @param marker the marker
	 * @return <code>true</code> if the marker references the element, false otherwise
	 * @exception CoreException if the <code>IMarker.getAttribute</code> on the marker fails
	 */
	public static boolean isReferencedBy(IJavaScriptElement element, IMarker marker) throws CoreException {

		// only match units or classfiles
		if (element instanceof IMember){
			IMember member = (IMember) element;
			if (member.isBinary()){
				element = member.getClassFile();
			} else {
				element = member.getJavaScriptUnit();
			}
		}
		if (element == null) return false;
		if (marker == null) return false;

		String markerHandleId = (String)marker.getAttribute(ATT_HANDLE_ID);
		if (markerHandleId == null) return false;

		IJavaScriptElement markerElement = JavaScriptCore.create(markerHandleId);
		while (true){
			if (element.equals(markerElement)) return true; // external elements may still be equal with different handleIDs.

			// cycle through enclosing types in case marker is associated with a classfile (15568)
			if (markerElement instanceof IClassFile){
				IType enclosingType = ((IClassFile)markerElement).getType().getDeclaringType();
				if (enclosingType != null){
					markerElement = enclosingType.getClassFile(); // retry with immediate enclosing classfile
					continue;
				}
			}
			break;
		}
		return false;
	}

	/**
	 * Returns whether the given marker delta references the given JavaScript element.
	 * Used for markers deltas, which denote a JavaScript element rather than a resource.
	 *
	 * @param element the element
	 * @param markerDelta the marker delta
	 * @return <code>true</code> if the marker delta references the element
	 * @exception CoreException if the  <code>IMarkerDelta.getAttribute</code> on the marker delta fails
	 */
	public static boolean isReferencedBy(IJavaScriptElement element, IMarkerDelta markerDelta) throws CoreException {

		// only match units or classfiles
		if (element instanceof IMember){
			IMember member = (IMember) element;
			if (member.isBinary()){
				element = member.getClassFile();
			} else {
				element = member.getJavaScriptUnit();
			}
		}
		if (element == null) return false;
		if (markerDelta == null) return false;

		String markerDeltarHandleId = (String)markerDelta.getAttribute(ATT_HANDLE_ID);
		if (markerDeltarHandleId == null) return false;

		IJavaScriptElement markerElement = JavaScriptCore.create(markerDeltarHandleId);
		while (true){
			if (element.equals(markerElement)) return true; // external elements may still be equal with different handleIDs.

			// cycle through enclosing types in case marker is associated with a classfile (15568)
			if (markerElement instanceof IClassFile){
				IType enclosingType = ((IClassFile)markerElement).getType().getDeclaringType();
				if (enclosingType != null){
					markerElement = enclosingType.getClassFile(); // retry with immediate enclosing classfile
					continue;
				}
			}
			break;
		}
		return false;
	}

	/**
	 * Creates and returns a new access rule with the given file pattern and kind.
	 * <p>
	 * The rule kind is one of {@link IAccessRule#K_ACCESSIBLE}, {@link IAccessRule#K_DISCOURAGED},
	 * or {@link IAccessRule#K_NON_ACCESSIBLE}, optionally combined with {@link IAccessRule#IGNORE_IF_BETTER},
	 * e..g. <code>IAccessRule.K_NON_ACCESSIBLE | IAccessRule.IGNORE_IF_BETTER</code>.
	 * </p>
	 *
	 * @param filePattern the file pattern this access rule should match
	 * @param kind one of {@link IAccessRule#K_ACCESSIBLE}, {@link IAccessRule#K_DISCOURAGED},
	 *                     or {@link IAccessRule#K_NON_ACCESSIBLE}, optionally combined with
	 *                     {@link IAccessRule#IGNORE_IF_BETTER}
	 * @return a new access rule
	 */
	public static IAccessRule newAccessRule(IPath filePattern, int kind) {
		return new ClasspathAccessRule(filePattern, kind);
	}

	/**
	 * Creates and returns a new includepath attribute with the given name and the given value.
	 *
	 * @return a new includepath attribute
	 */
	public static IIncludePathAttribute newIncludepathAttribute(String name, String value) {
		return new ClasspathAttribute(name, value);
	}

	/**
	 * Creates and returns a new includepath entry of kind <code>CPE_CONTAINER</code>
	 * for the given path. This method is fully equivalent to calling
	 * {@link #newContainerEntry(IPath, IAccessRule[], IIncludePathAttribute[], boolean)
	 * newContainerEntry(containerPath, new IAccessRule[0], new IIncludePathAttribute[0], false)}.
	 * <p>
	 * @param containerPath the path identifying the container, it must be formed of at least two
	 * 	segments
	 * @return a new container includepath entry
	 *
	 * @see JavaScriptCore#getJsGlobalScopeContainer(IPath, IJavaScriptProject)
	 */
	public static IIncludePathEntry newContainerEntry(IPath containerPath) {
		return newContainerEntry(
		containerPath,
		ClasspathEntry.NO_ACCESS_RULES,
		ClasspathEntry.NO_EXTRA_ATTRIBUTES,
		false/*not exported*/);
	}

	/**
	 * Creates and returns a new includepath entry of kind <code>CPE_CONTAINER</code>
	 * for the given path. This method is fully equivalent to calling
	 * {@link #newContainerEntry(IPath, IAccessRule[], IIncludePathAttribute[], boolean)
	 * newContainerEntry(containerPath, new IAccessRule[0], new IIncludePathAttribute[0], isExported)}.
	 *
	 * @param containerPath the path identifying the container, it must be formed of at least
	 * 	one segment (ID+hints)
	 * @param isExported a boolean indicating whether this entry is contributed to dependent
	 *    projects in addition to the output location
	 * @return a new container includepath entry
	 *
	 * @see JavaScriptCore#getJsGlobalScopeContainer(IPath, IJavaScriptProject)
	 * @see JavaScriptCore#setJsGlobalScopeContainer(IPath, IJavaScriptProject[], IJsGlobalScopeContainer[], IProgressMonitor)
	 */
	public static IIncludePathEntry newContainerEntry(IPath containerPath, boolean isExported) {
		return newContainerEntry(
			containerPath,
			ClasspathEntry.NO_ACCESS_RULES,
			ClasspathEntry.NO_EXTRA_ATTRIBUTES,
			isExported);
	}

	/**
	 * Creates and returns a new includepath entry of kind <code>CPE_CONTAINER</code>
	 * for the given path. The path of the container will be used during resolution so as to map this
	 * container entry to a set of other includepath entries the container is acting for.
	 * <p>
	 * A container entry allows to express indirect references to a set of libraries, projects and variable entries,
	 * which can be interpreted differently for each JavaScript project where it is used.
	 * A includepath container entry can be resolved using <code>JavaScriptCore.getResolvedJsGlobalScopeContainer</code>,
	 * and updated with <code>JavaScriptCore.JsGlobalScopeContainerChanged</code>
	 * <p>
	 * A container is exclusively resolved by a <code>JsGlobalScopeContainerInitializer</code> registered onto the
	 * extension point "org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer".
	 * <p>
	 * A container path must be formed of at least one segment, where: <ul>
	 * <li> the first segment is a unique ID identifying the target container, there must be a container initializer registered
	 * 	onto this ID through the extension point  "org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer". </li>
	 * <li> the remaining segments will be passed onto the initializer, and can be used as additional
	 * 	hints during the initialization phase. </li>
	 * </ul>
	 * <p>
	 * Example of an JsGlobalScopeContainerInitializer for a includepath container denoting a default JDK container:
	 * <pre>
	 * containerEntry = JavaScriptCore.newContainerEntry(new Path("MyProvidedJDK/default"));
	 *
	 * &lt;extension
	 *    point="org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer"&gt;
	 *    &lt;containerInitializer
	 *       id="MyProvidedJDK"
	 *       class="com.example.MyInitializer"/&gt;
	 * </pre>
	 * <p>
	 * The access rules determine the set of accessible source and class files
	 * in the container. If the list of access rules is empty, then all files
	 * in this container are accessible.
	 * See {@link IAccessRule} for a detailed description of access
	 * rules. Note that if an entry defined by the container defines access rules,
	 * then these access rules are combined with the given access rules.
	 * The given access rules are considered first, then the entry's access rules are
	 * considered.
	 * </p>
	 * <p>
	 * The <code>extraAttributes</code> list contains name/value pairs that must be persisted with
	 * this entry. If no extra attributes are provided, an empty array must be passed in.<br>
	 * Note that this list should not contain any duplicate name.
	 * </p>
	 * <p>
	 * The <code>isExported</code> flag indicates whether this entry is contributed to dependent
	 * projects. If not exported, dependent projects will not see any of the classes from this entry.
	 * If exported, dependent projects will concatenate the accessible files patterns of this entry with the
	 * accessible files patterns of the projects, and they will concatenate the non accessible files patterns of this entry
	 * with the non accessible files patterns of the project.
	 * </p>
	 * <p>
	 * Note that this operation does not attempt to validate includepath containers
	 * or access the resources at the given paths.
	 * </p>
	 *
	 * @param containerPath the path identifying the container, it must be formed of at least
	 * 	one segment (ID+hints)
	 * @param accessRules the possibly empty list of access rules for this entry
	 * @param extraAttributes the possibly empty list of extra attributes to persist with this entry
	 * @param isExported a boolean indicating whether this entry is contributed to dependent
	 *    projects in addition to the output location
	 * @return a new container includepath entry
	 *
	 * @see JavaScriptCore#getJsGlobalScopeContainer(IPath, IJavaScriptProject)
	 * @see JavaScriptCore#setJsGlobalScopeContainer(IPath, IJavaScriptProject[], IJsGlobalScopeContainer[], IProgressMonitor)
	 * @see JavaScriptCore#newContainerEntry(IPath, boolean)
	 * @see JavaScriptCore#newAccessRule(IPath, int)
	 */
	public static IIncludePathEntry newContainerEntry(
			IPath containerPath,
			IAccessRule[] accessRules,
			IIncludePathAttribute[] extraAttributes,
			boolean isExported) {

		if (containerPath == null) {
			Assert.isTrue(false, "Container path cannot be null"); //$NON-NLS-1$
		} else if (containerPath.segmentCount() < 1) {
			Assert.isTrue(
				false,
				"Illegal classpath container path: \'" + containerPath.makeRelative().toString() + "\', must have at least one segment (containerID+hints)"); //$NON-NLS-1$//$NON-NLS-2$
		}
		return new ClasspathEntry(
			IPackageFragmentRoot.K_SOURCE,
			IIncludePathEntry.CPE_CONTAINER,
			containerPath,
			ClasspathEntry.INCLUDE_ALL, // inclusion patterns
			ClasspathEntry.EXCLUDE_NONE, // exclusion patterns
			null, // source attachment
			null, // source attachment root
			null, // specific output folder
			isExported,
			accessRules,
			true, // combine access rules
			extraAttributes);
	}

	/**
	 * Creates and returns a type hierarchy for all types in the given
	 * region, considering subtypes within that region and considering types in the
	 * working copies with the given owner.
	 * In other words, the owner's working copies will take
	 * precedence over their original javaScript units in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * <p>
	 *
	 * @param monitor the given progress monitor
	 * @param region the given region
	 * @param owner the owner of working copies that take precedence over their original javaScript units,
	 *   or <code>null</code> if the primary working copy owner should be used
	 * @exception JavaScriptModelException if an element in the region does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 * @exception IllegalArgumentException if region is <code>null</code>
	 * @return a type hierarchy for all types in the given
	 * region, considering subtypes within that region
	 */
	public static ITypeHierarchy newTypeHierarchy(IRegion region, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaScriptModelException {
		if (region == null) {
			throw new IllegalArgumentException(Messages.hierarchy_nullRegion);
		}
		IJavaScriptUnit[] workingCopies = JavaModelManager.getJavaModelManager().getWorkingCopies(owner, true/*add primary working copies*/);
		CreateTypeHierarchyOperation op =
			new CreateTypeHierarchyOperation(region, workingCopies, null, true/*compute subtypes*/);
		op.runOperation(monitor);
		return op.getResult();
	}

	/**
	 * Creates and returns a new non-exported includepath entry of kind <code>CPE_LIBRARY</code> for the
	 * JAR or folder identified by the given absolute path. This specifies that all package fragments
	 * within the root will have children of type <code>IClassFile</code>.
	 * This method is fully equivalent to calling
	 * {@link #newLibraryEntry(IPath, IPath, IPath, IAccessRule[], IIncludePathAttribute[], boolean)
	 * newLibraryEntry(path, sourceAttachmentPath, sourceAttachmentRootPath, new IAccessRule[0], new IIncludePathAttribute[0], false)}.
	 *
	 * @param path the absolute path of the binary archive
	 * @param sourceAttachmentPath the absolute path of the corresponding source archive or folder,
	 *    or <code>null</code> if none. Note, since 3.0, an empty path is allowed to denote no source attachment.
	 *   and will be automatically converted to <code>null</code>.
	 * @param sourceAttachmentRootPath the location of the root of the source files within the source archive or folder
	 *    or <code>null</code> if this location should be automatically detected.
	 * @return a new library includepath entry
	 */
	public static IIncludePathEntry newLibraryEntry(
		IPath path,
		IPath sourceAttachmentPath,
		IPath sourceAttachmentRootPath) {

		return newLibraryEntry(
			path,
			sourceAttachmentPath,
			sourceAttachmentRootPath,
			ClasspathEntry.NO_ACCESS_RULES,
			ClasspathEntry.NO_EXTRA_ATTRIBUTES,
			false/*not exported*/);
	}

	/**
	 * Creates and returns a new includepath entry of kind <code>CPE_LIBRARY</code> for the JAR or folder
	 * identified by the given absolute path. This specifies that all package fragments within the root
	 * will have children of type <code>IClassFile</code>.
	 * This method is fully equivalent to calling
	 * {@link #newLibraryEntry(IPath, IPath, IPath, IAccessRule[], IIncludePathAttribute[], boolean)
	 * newLibraryEntry(path, sourceAttachmentPath, sourceAttachmentRootPath, new IAccessRule[0], new IIncludePathAttribute[0], isExported)}.
	 *
	 * @param path the absolute path of the binary archive
	 * @param sourceAttachmentPath the absolute path of the corresponding source archive or folder,
	 *    or <code>null</code> if none. Note, since 3.0, an empty path is allowed to denote no source attachment.
	 *   and will be automatically converted to <code>null</code>.
	 * @param sourceAttachmentRootPath the location of the root of the source files within the source archive or folder
	 *    or <code>null</code> if this location should be automatically detected.
	 * @param isExported indicates whether this entry is contributed to dependent
	 * 	  projects in addition to the output location
	 * @return a new library includepath entry
	 */
	public static IIncludePathEntry newLibraryEntry(
		IPath path,
		IPath sourceAttachmentPath,
		IPath sourceAttachmentRootPath,
		boolean isExported) {

		return newLibraryEntry(
			path,
			sourceAttachmentPath,
			sourceAttachmentRootPath,
			ClasspathEntry.NO_ACCESS_RULES,
			ClasspathEntry.NO_EXTRA_ATTRIBUTES,
			isExported);
	}

	/**
	 * Creates and returns a new includepath entry of kind <code>CPE_LIBRARY</code> for the JAR or folder
	 * identified by the given absolute path. This specifies that all package fragments within the root
	 * will have children of type <code>IClassFile</code>.
	 * <p>
	 * A library entry is used to denote a prerequisite JAR or root folder containing binaries.
	 * The target JAR can either be defined internally to the workspace (absolute path relative
	 * to the workspace root) or externally to the workspace (absolute path in the file system).
	 * The target root folder can only be defined internally to the workspace (absolute path relative
	 * to the workspace root). To use a binary folder external to the workspace, it must first be
	 * linked (see IFolder#createLink(...)).
	 * <p>
	 * <p>
	 * The <code>extraAttributes</code> list contains name/value pairs that must be persisted with
	 * this entry. If no extra attributes are provided, an empty array must be passed in.<br>
	 * Note that this list should not contain any duplicate name.
	 * </p>
	 * <p>
	 * The <code>isExported</code> flag indicates whether this entry is contributed to dependent
	 * projects. If not exported, dependent projects will not see any of the classes from this entry.
	 * If exported, dependent projects will concatenate the accessible files patterns of this entry with the
	 * accessible files patterns of the projects, and they will concatenate the non accessible files patterns of this entry
	 * with the non accessible files patterns of the project.
	 * </p>
	 *
	 * @param path the absolute path of the binary archive
	 * @param sourceAttachmentPath the absolute path of the corresponding source archive or folder,
	 *    or <code>null</code> if none. Note, since 3.0, an empty path is allowed to denote no source attachment.
	 *   and will be automatically converted to <code>null</code>.
	 * @param sourceAttachmentRootPath the location of the root of the source files within the source archive or folder
	 *    or <code>null</code> if this location should be automatically detected.
	 * @param accessRules the possibly empty list of access rules for this entry
	 * @param extraAttributes the possibly empty list of extra attributes to persist with this entry
	 * @param isExported indicates whether this entry is contributed to dependent
	 * 	  projects in addition to the output location
	 * @return a new library includepath entry
	 */
	public static IIncludePathEntry newLibraryEntry(
			IPath path,
			IPath sourceAttachmentPath,
			IPath sourceAttachmentRootPath,
			IAccessRule[] accessRules,
			IIncludePathAttribute[] extraAttributes,
			boolean isExported) {

		if (path == null) Assert.isTrue(false, "Library path cannot be null"); //$NON-NLS-1$
		if (!path.isAbsolute()) Assert.isTrue(false, "Path for IIncludePathEntry must be absolute: " + path.toString()); //$NON-NLS-1$
		if (sourceAttachmentPath != null) {
			if (sourceAttachmentPath.isEmpty()) {
				sourceAttachmentPath = null; // treat empty path as none
			} else if (!sourceAttachmentPath.isAbsolute()) {
				Assert.isTrue(false, "Source attachment path '" //$NON-NLS-1$
						+ sourceAttachmentPath
						+ "' for IIncludePathEntry must be absolute"); //$NON-NLS-1$
			}
		}
		return new ClasspathEntry(
			IPackageFragmentRoot.K_BINARY,
			IIncludePathEntry.CPE_LIBRARY,
			JavaProject.canonicalizedPath(path),
			ClasspathEntry.INCLUDE_ALL, // inclusion patterns
			ClasspathEntry.EXCLUDE_NONE, // exclusion patterns
			sourceAttachmentPath,
			sourceAttachmentRootPath,
			null, // specific output folder
			isExported,
			accessRules,
			false, // no access rules to combine
			extraAttributes);
	}
	
	/**
	 * Creates and returns a new includepath entry of kind <code>CPE_LIBRARY</code> for the JAR or folder
	 * identified by the given absolute path. This specifies that all package fragments within the root
	 * will have children of type <code>IClassFile</code>.
	 * <p>
	 * A library entry is used to denote a prerequisite JAR or root folder containing binaries.
	 * The target JAR can either be defined internally to the workspace (absolute path relative
	 * to the workspace root) or externally to the workspace (absolute path in the file system).
	 * The target root folder can only be defined internally to the workspace (absolute path relative
	 * to the workspace root). To use a binary folder external to the workspace, it must first be
	 * linked (see IFolder#createLink(...)).
	 * <p>
	 * <p>
	 * The <code>extraAttributes</code> list contains name/value pairs that must be persisted with
	 * this entry. If no extra attributes are provided, an empty array must be passed in.<br>
	 * Note that this list should not contain any duplicate name.
	 * </p>
	 * <p>
	 * The <code>isExported</code> flag indicates whether this entry is contributed to dependent
	 * projects. If not exported, dependent projects will not see any of the classes from this entry.
	 * If exported, dependent projects will concatenate the accessible files patterns of this entry with the
	 * accessible files patterns of the projects, and they will concatenate the non accessible files patterns of this entry
	 * with the non accessible files patterns of the project.
	 * </p>
	 *
	 * @param path the absolute path of the binary archive
	 * @param sourceAttachmentPath the absolute path of the corresponding source archive or folder,
	 *    or <code>null</code> if none. Note, since 3.0, an empty path is allowed to denote no source attachment.
	 *   and will be automatically converted to <code>null</code>.
	 * @param sourceAttachmentRootPath the location of the root of the source files within the source archive or folder
	 *    or <code>null</code> if this location should be automatically detected.
	 * @param accessRules the possibly empty list of access rules for this entry
     * @param exclusionPatterns the possibly empty list of exclusion patterns
	 *    represented as relative paths
	 * @param extraAttributes the possibly empty list of extra attributes to persist with this entry
	 * @param isExported indicates whether this entry is contributed to dependent
	 * 	  projects in addition to the output location
	 * @return a new library includepath entry
	 */
	public static IIncludePathEntry newLibraryEntry(
			IPath path,
			IPath sourceAttachmentPath,
			IPath sourceAttachmentRootPath,
			IAccessRule[] accessRules,
			IIncludePathAttribute[] extraAttributes,
			IPath[] exclusionPatterns,
			boolean isExported) {

		if (path == null) Assert.isTrue(false, "Library path cannot be null"); //$NON-NLS-1$
		if (!path.isAbsolute()) Assert.isTrue(false, "Path for IIncludePathEntry must be absolute"); //$NON-NLS-1$
		if (sourceAttachmentPath != null) {
			if (sourceAttachmentPath.isEmpty()) {
				sourceAttachmentPath = null; // treat empty path as none
			} else if (!sourceAttachmentPath.isAbsolute()) {
				Assert.isTrue(false, "Source attachment path '" //$NON-NLS-1$
						+ sourceAttachmentPath
						+ "' for IIncludePathEntry must be absolute"); //$NON-NLS-1$
			}
		}
		return new ClasspathEntry(
			IPackageFragmentRoot.K_BINARY,
			IIncludePathEntry.CPE_LIBRARY,
			JavaProject.canonicalizedPath(path),
			ClasspathEntry.INCLUDE_ALL, // inclusion patterns
			exclusionPatterns, // exclusion patterns
			sourceAttachmentPath,
			sourceAttachmentRootPath,
			null, // specific output folder
			isExported,
			accessRules,
			false, // no access rules to combine
			extraAttributes);
	}

	/**
	 * Creates and returns a new non-exported includepath entry of kind <code>CPE_PROJECT</code>
	 * for the project identified by the given absolute path.
	 * This method is fully equivalent to calling
	 * {@link #newProjectEntry(IPath, IAccessRule[], boolean, IIncludePathAttribute[], boolean)
	 * newProjectEntry(path, new IAccessRule[0], true, new IIncludePathAttribute[0], false)}.
	 *
	 * @param path the absolute path of the binary archive
	 * @return a new project includepath entry
	 */
	public static IIncludePathEntry newProjectEntry(IPath path) {
		return newProjectEntry(path, false);
	}

	/**
	 * Creates and returns a new includepath entry of kind <code>CPE_PROJECT</code>
	 * for the project identified by the given absolute path.
	 * This method is fully equivalent to calling
	 * {@link #newProjectEntry(IPath, IAccessRule[], boolean, IIncludePathAttribute[], boolean)
	 * newProjectEntry(path, new IAccessRule[0], true, new IIncludePathAttribute[0], isExported)}.
	 *
	 * @param path the absolute path of the prerequisite project
	 * @param isExported indicates whether this entry is contributed to dependent
	 * 	  projects in addition to the output location
	 * @return a new project includepath entry
	 */
	public static IIncludePathEntry newProjectEntry(IPath path, boolean isExported) {

		if (!path.isAbsolute()) Assert.isTrue(false, "Path for IIncludePathEntry must be absolute"); //$NON-NLS-1$

		return newProjectEntry(
			path,
			ClasspathEntry.NO_ACCESS_RULES,
			true,
			ClasspathEntry.NO_EXTRA_ATTRIBUTES,
			isExported);
	}

	/**
	 * Creates and returns a new includepath entry of kind <code>CPE_PROJECT</code>
	 * for the project identified by the given absolute path.
	 * <p>
	 * A project entry is used to denote a prerequisite project on a includepath.
	 * The referenced project will be contributed as a whole, either as sources (in the JavaScript Model, it
	 * contributes all its package fragment roots) or as binaries (when building, it contributes its
	 * whole output location).
	 * </p>
	 * <p>
	 * A project reference allows to indirect through another project, independently from its internal layout.
	 * </p><p>
	 * The prerequisite project is referred to using an absolute path relative to the workspace root.
	 * </p>
	 * <p>
	 * The access rules determine the set of accessible class files
	 * in the project. If the list of access rules is empty then all files
	 * in this project are accessible.
	 * See {@link IAccessRule} for a detailed description of access rules.
	 * </p>
	 * <p>
	 * The <code>combineAccessRules</code> flag indicates whether access rules of one (or more)
	 * exported entry of the project should be combined with the given access rules. If they should
	 * be combined, the given access rules are considered first, then the entry's access rules are
	 * considered.
	 * </p>
	 * <p>
	 * The <code>extraAttributes</code> list contains name/value pairs that must be persisted with
	 * this entry. If no extra attributes are provided, an empty array must be passed in.<br>
	 * Note that this list should not contain any duplicate name.
	 * </p>
	 * <p>
	 * The <code>isExported</code> flag indicates whether this entry is contributed to dependent
	 * projects. If not exported, dependent projects will not see any of the classes from this entry.
	 * If exported, dependent projects will concatenate the accessible files patterns of this entry with the
	 * accessible files patterns of the projects, and they will concatenate the non accessible files patterns of this entry
	 * with the non accessible files patterns of the project.
	 * </p>
	 *
	 * @param path the absolute path of the prerequisite project
	 * @param accessRules the possibly empty list of access rules for this entry
	 * @param combineAccessRules whether the access rules of the project's exported entries should be combined with the given access rules
	 * @param extraAttributes the possibly empty list of extra attributes to persist with this entry
	 * @param isExported indicates whether this entry is contributed to dependent
	 * 	  projects in addition to the output location
	 * @return a new project includepath entry
	 */
	public static IIncludePathEntry newProjectEntry(
			IPath path,
			IAccessRule[] accessRules,
			boolean combineAccessRules,
			IIncludePathAttribute[] extraAttributes,
			boolean isExported) {

		if (!path.isAbsolute()) Assert.isTrue(false, "Path for IIncludePathEntry must be absolute"); //$NON-NLS-1$

		return new ClasspathEntry(
			IPackageFragmentRoot.K_SOURCE,
			IIncludePathEntry.CPE_PROJECT,
			path,
			ClasspathEntry.INCLUDE_ALL, // inclusion patterns
			ClasspathEntry.EXCLUDE_NONE, // exclusion patterns
			null, // source attachment
			null, // source attachment root
			null, // specific output folder
			isExported,
			accessRules,
			combineAccessRules,
			extraAttributes);
	}

	/**
	 * Returns a new empty region.
	 *
	 * @return a new empty region
	 */
	public static IRegion newRegion() {
		return new Region();
	}

	/**
	 * Creates and returns a new includepath entry of kind <code>CPE_SOURCE</code>
	 * for all files in the project's source folder identified by the given
	 * absolute workspace-relative path.
	 * <p>
	 * The convenience method is fully equivalent to:
	 * <pre>
	 * newSourceEntry(path, new IPath[] {}, new IPath[] {}, null);
	 * </pre>
	 * </p>
	 *
	 * @param path the absolute workspace-relative path of a source folder
	 * @return a new source includepath entry
	 * @see #newSourceEntry(IPath, IPath[], IPath[], IPath)
	 */
	public static IIncludePathEntry newSourceEntry(IPath path) {

		return newSourceEntry(path, ClasspathEntry.INCLUDE_ALL, ClasspathEntry.EXCLUDE_NONE, null /*output location*/);
	}

	/**
	 * Creates and returns a new includepath entry of kind <code>CPE_SOURCE</code>
	 * for the project's source folder identified by the given absolute
	 * workspace-relative path but excluding all source files with paths
	 * matching any of the given patterns.
	 * <p>
	 * The convenience method is fully equivalent to:
	 * <pre>
	 * newSourceEntry(path, new IPath[] {}, exclusionPatterns, null);
	 * </pre>
	 * </p>
	 *
	 * @param path the absolute workspace-relative path of a source folder
	 * @param exclusionPatterns the possibly empty list of exclusion patterns
	 *    represented as relative paths
	 * @return a new source includepath entry
	 * @see #newSourceEntry(IPath, IPath[], IPath[], IPath)
	 */
	public static IIncludePathEntry newSourceEntry(IPath path, IPath[] exclusionPatterns) {

		return newSourceEntry(path, ClasspathEntry.INCLUDE_ALL, exclusionPatterns, null /*output location*/);
	}

	/**
	 * Creates and returns a new includepath entry of kind <code>CPE_SOURCE</code>
	 * for the project's source folder identified by the given absolute
	 * workspace-relative path but excluding all source files with paths
	 * matching any of the given patterns, and associated with a specific output location
	 * (that is, ".class" files are not going to the project default output location).
	 * <p>
	 * The convenience method is fully equivalent to:
	 * <pre>
	 * newSourceEntry(path, new IPath[] {}, exclusionPatterns, specificOutputLocation);
	 * </pre>
	 * </p>
	 *
	 * @param path the absolute workspace-relative path of a source folder
	 * @param exclusionPatterns the possibly empty list of exclusion patterns
	 *    represented as relative paths
	 * @param specificOutputLocation the specific output location for this source entry (<code>null</code> if using project default ouput location)
	 * @return a new source includepath entry
	 * @see #newSourceEntry(IPath, IPath[], IPath[], IPath)
	 */
	public static IIncludePathEntry newSourceEntry(IPath path, IPath[] exclusionPatterns, IPath specificOutputLocation) {

	    return newSourceEntry(path, ClasspathEntry.INCLUDE_ALL, exclusionPatterns, specificOutputLocation);
	}

	/**
	 * Creates and returns a new includepath entry of kind <code>CPE_SOURCE</code>
	 * for the project's source folder identified by the given absolute
	 * workspace-relative path but excluding all source files with paths
	 * matching any of the given patterns, and associated with a specific output location
	 * (that is, ".class" files are not going to the project default output location).
	 * <p>
	 * The convenience method is fully equivalent to:
	 * <pre>
	 * newSourceEntry(path, new IPath[] {}, exclusionPatterns, specificOutputLocation, new IIncludePathAttribute[] {});
	 * </pre>
	 * </p>
	 *
	 * @param path the absolute workspace-relative path of a source folder
	 * @param inclusionPatterns the possibly empty list of inclusion patterns
	 *    represented as relative paths
	 * @param exclusionPatterns the possibly empty list of exclusion patterns
	 *    represented as relative paths
	 * @param specificOutputLocation the specific output location for this source entry (<code>null</code> if using project default ouput location)
	 * @return a new source includepath entry
	 * @see #newSourceEntry(IPath, IPath[], IPath[], IPath, IIncludePathAttribute[])
	 */
	public static IIncludePathEntry newSourceEntry(IPath path, IPath[] inclusionPatterns, IPath[] exclusionPatterns, IPath specificOutputLocation) {
		return newSourceEntry(path, inclusionPatterns, exclusionPatterns, specificOutputLocation, ClasspathEntry.NO_EXTRA_ATTRIBUTES);
	}

	/**
	 * Creates and returns a new includepath entry of kind <code>CPE_SOURCE</code>
	 * for the project's source folder identified by the given absolute
	 * workspace-relative path using the given inclusion and exclusion patterns
	 * to determine which source files are included, and the given output path
	 * to control the output location of generated files.
	 * <p>
	 * The source folder is referred to using an absolute path relative to the
	 * workspace root, e.g. <code>/Project/src</code>. A project's source
	 * folders are located with that project. That is, a source includepath
	 * entry specifying the path <code>/P1/src</code> is only usable for
	 * project <code>P1</code>.
	 * </p>
	 * <p>
	 * The inclusion patterns determines the initial set of source files that
	 * are to be included; the exclusion patterns are then used to reduce this
	 * set. When no inclusion patterns are specified, the initial file set
	 * includes all relevent files in the resource tree rooted at the source
	 * entry's path. On the other hand, specifying one or more inclusion
	 * patterns means that all <b>and only</b> files matching at least one of
	 * the specified patterns are to be included. If exclusion patterns are
	 * specified, the initial set of files is then reduced by eliminating files
	 * matched by at least one of the exclusion patterns. Inclusion and
	 * exclusion patterns look like relative file paths with wildcards and are
	 * interpreted relative to the source entry's path. File patterns are
	 * case-sensitive can contain '**', '*' or '?' wildcards (see
	 * {@link IIncludePathEntry#getExclusionPatterns()} for the full description
	 * of their syntax and semantics). The resulting set of files are included
	 * in the corresponding package fragment root; all package fragments within
	 * the root will have children of type <code>IJavaScriptUnit</code>.
	 * </p>
	 * <p>
	 * Also note that all sources/binaries inside a project are contributed as
	 * a whole through a project entry
	 * (see <code>JavaScriptCore.newProjectEntry</code>). Particular source entries
	 * cannot be selectively exported.
	 * </p>
	 * <p>
	 * The <code>extraAttributes</code> list contains name/value pairs that must be persisted with
	 * this entry. If no extra attributes are provided, an empty array must be passed in.<br>
	 * Note that this list should not contain any duplicate name.
	 * </p>
	 *
	 * @param path the absolute workspace-relative path of a source folder
	 * @param inclusionPatterns the possibly empty list of inclusion patterns
	 *    represented as relative paths
	 * @param exclusionPatterns the possibly empty list of exclusion patterns
	 *    represented as relative paths
	 * @param specificOutputLocation the specific output location for this source entry (<code>null</code> if using project default ouput location)
	 * @param extraAttributes the possibly empty list of extra attributes to persist with this entry
	 * @return a new source includepath entry with the given exclusion patterns
	 * @see IIncludePathEntry#getInclusionPatterns()
	 * @see IIncludePathEntry#getExclusionPatterns()
	 * @see IIncludePathEntry#getOutputLocation()
	 */
	public static IIncludePathEntry newSourceEntry(IPath path, IPath[] inclusionPatterns, IPath[] exclusionPatterns, IPath specificOutputLocation, IIncludePathAttribute[] extraAttributes) {

		if (path == null) Assert.isTrue(false, "Source path cannot be null"); //$NON-NLS-1$
		if (!path.isAbsolute()) Assert.isTrue(false, "Path for IIncludePathEntry must be absolute"); //$NON-NLS-1$
		if (exclusionPatterns == null) Assert.isTrue(false, "Exclusion pattern set cannot be null"); //$NON-NLS-1$
		if (inclusionPatterns == null) Assert.isTrue(false, "Inclusion pattern set cannot be null"); //$NON-NLS-1$

		return new ClasspathEntry(
			IPackageFragmentRoot.K_SOURCE,
			IIncludePathEntry.CPE_SOURCE,
			path,
			inclusionPatterns,
			exclusionPatterns,
			null, // source attachment
			null, // source attachment root
			specificOutputLocation, // custom output location
			false,
			null,
			false, // no access rules to combine
			extraAttributes);
	}

	/**
	 * Creates and returns a new non-exported includepath entry of kind <code>CPE_VARIABLE</code>
	 * for the given path. This method is fully equivalent to calling
	 * {@link #newVariableEntry(IPath, IPath, IPath, IAccessRule[], IIncludePathAttribute[], boolean)
	 * newVariableEntry(variablePath, variableSourceAttachmentPath, sourceAttachmentRootPath, new IAccessRule[0], new IIncludePathAttribute[0], false)}.
	 *
	 * @param variablePath the path of the binary archive; first segment is the
	 *   name of a includepath variable
	 * @param variableSourceAttachmentPath the path of the corresponding source archive,
	 *    or <code>null</code> if none; if present, the first segment is the
	 *    name of a includepath variable (not necessarily the same variable
	 *    as the one that begins <code>variablePath</code>)
	 * @param sourceAttachmentRootPath the location of the root of the source files within the source archive
	 *    or <code>null</code> if <code>variableSourceAttachmentPath</code> is also <code>null</code>
	 * @return a new library includepath entry
	 */
	public static IIncludePathEntry newVariableEntry(
		IPath variablePath,
		IPath variableSourceAttachmentPath,
		IPath sourceAttachmentRootPath) {

		return newVariableEntry(variablePath, variableSourceAttachmentPath, sourceAttachmentRootPath, false);
	}

	/**
	 * Creates and returns a new includepath entry of kind <code>CPE_VARIABLE</code>
	 * for the given path. This method is fully equivalent to calling
	 * {@link #newVariableEntry(IPath, IPath, IPath, IAccessRule[], IIncludePathAttribute[], boolean)
	 * newVariableEntry(variablePath, variableSourceAttachmentPath, sourceAttachmentRootPath, new IAccessRule[0], new IIncludePathAttribute[0], isExported)}.
	 *
	 * @param variablePath the path of the binary archive; first segment is the
	 *   name of a includepath variable
	 * @param variableSourceAttachmentPath the path of the corresponding source archive,
	 *    or <code>null</code> if none; if present, the first segment is the
	 *    name of a includepath variable (not necessarily the same variable
	 *    as the one that begins <code>variablePath</code>)
	 * @param variableSourceAttachmentRootPath the location of the root of the source files within the source archive
	 *    or <code>null</code> if <code>variableSourceAttachmentPath</code> is also <code>null</code>
	 * @param isExported indicates whether this entry is contributed to dependent
	 * 	  projects in addition to the output location
	 * @return a new variable includepath entry
	 */
	public static IIncludePathEntry newVariableEntry(
			IPath variablePath,
			IPath variableSourceAttachmentPath,
			IPath variableSourceAttachmentRootPath,
			boolean isExported) {

		return newVariableEntry(
			variablePath,
			variableSourceAttachmentPath,
			variableSourceAttachmentRootPath,
			ClasspathEntry.NO_ACCESS_RULES,
			ClasspathEntry.NO_EXTRA_ATTRIBUTES,
			isExported);
	}

	/**
	 * Creates and returns a new includepath entry of kind <code>CPE_VARIABLE</code>
	 * for the given path. The first segment of the path is the name of a includepath variable.
	 * The trailing segments of the path will be appended to resolved variable path.
	 * <p>
	 * A variable entry allows to express indirect references on a includepath to other projects or libraries,
	 * depending on what the includepath variable is referring.
	 * <p>
	 *	It is possible to register an automatic initializer (<code>JsGlobalScopeVariableInitializer</code>),
	 * which will be invoked through the extension point "org.eclipse.wst.jsdt.core.JsGlobalScopeVariableInitializer".
	 * After resolution, a includepath variable entry may either correspond to a project or a library entry.
	 * <p>
	 * The <code>extraAttributes</code> list contains name/value pairs that must be persisted with
	 * this entry. If no extra attributes are provided, an empty array must be passed in.<br>
	 * Note that this list should not contain any duplicate name.
	 * </p>
	 * <p>
	 * The <code>isExported</code> flag indicates whether this entry is contributed to dependent
	 * projects. If not exported, dependent projects will not see any of the classes from this entry.
	 * If exported, dependent projects will concatenate the accessible files patterns of this entry with the
	 * accessible files patterns of the projects, and they will concatenate the non accessible files patterns of this entry
	 * with the non accessible files patterns of the project.
	 * </p>
	 * <p>
	 * Note that this operation does not attempt to validate includepath variables
	 * or access the resources at the given paths.
	 * </p>
	 *
	 * @param variablePath the path of the binary archive; first segment is the
	 *   name of a includepath variable
	 * @param variableSourceAttachmentPath the path of the corresponding source archive,
	 *    or <code>null</code> if none; if present, the first segment is the
	 *    name of a includepath variable (not necessarily the same variable
	 *    as the one that begins <code>variablePath</code>)
	 * @param variableSourceAttachmentRootPath the location of the root of the source files within the source archive
	 *    or <code>null</code> if <code>variableSourceAttachmentPath</code> is also <code>null</code>
	 * @param accessRules the possibly empty list of access rules for this entry
	 * @param extraAttributes the possibly empty list of extra attributes to persist with this entry
	 * @param isExported indicates whether this entry is contributed to dependent
	 * 	  projects in addition to the output location
	 * @return a new variable includepath entry
	 */
	public static IIncludePathEntry newVariableEntry(
			IPath variablePath,
			IPath variableSourceAttachmentPath,
			IPath variableSourceAttachmentRootPath,
			IAccessRule[] accessRules,
			IIncludePathAttribute[] extraAttributes,
			boolean isExported) {

		if (variablePath == null) Assert.isTrue(false, "Variable path cannot be null"); //$NON-NLS-1$
		if (variablePath.segmentCount() < 1) {
			Assert.isTrue(
				false,
				"Illegal includepath variable path: \'" + variablePath.makeRelative().toString() + "\', must have at least one segment"); //$NON-NLS-1$//$NON-NLS-2$
		}

		return new ClasspathEntry(
			IPackageFragmentRoot.K_SOURCE,
			IIncludePathEntry.CPE_VARIABLE,
			variablePath,
			ClasspathEntry.INCLUDE_ALL, // inclusion patterns
			ClasspathEntry.EXCLUDE_NONE, // exclusion patterns
			variableSourceAttachmentPath, // source attachment
			variableSourceAttachmentRootPath, // source attachment root
			null, // specific output folder
			isExported,
			accessRules,
			false, // no access rules to combine
			extraAttributes);
	}

	/**
	 * Removed the given includepath variable. Does nothing if no value was
	 * set for this includepath variable.
	 * <p>
	 * This functionality cannot be used while the resource tree is locked.
	 * <p>
	 * Includepath variable values are persisted locally to the workspace, and
	 * are preserved from session to session.
	 * <p>
	 *
	 * @param variableName the name of the includepath variable
	 * @param monitor the progress monitor to report progress
	 * @see #setIncludepathVariable(String, IPath)
	 */
	public static void removeIncludepathVariable(String variableName, IProgressMonitor monitor) {
		try {
			SetVariablesOperation operation = new SetVariablesOperation(new String[]{ variableName}, new IPath[]{ null }, true/*update preferences*/);
			operation.runOperation(monitor);
		} catch (JavaScriptModelException e) {
			Util.log(e, "Exception while removing variable " + variableName); //$NON-NLS-1$
		}
	}

	/**
	 * Removes the given element changed listener.
	 * Has no affect if an identical listener is not registered.
	 *
	 * @param listener the listener
	 */
	public static void removeElementChangedListener(IElementChangedListener listener) {
		JavaModelManager.getJavaModelManager().deltaState.removeElementChangedListener(listener);
	}

	/**
	 * Removes the file extension from the given file name, if it has a JavaScript-like file
	 * extension. Otherwise the file name itself is returned.
	 * Note this removes the dot ('.') before the extension as well.
	 *
	 * @param fileName the name of a file
	 * @return the fileName without the JavaScript-like extension
	 */
	public static String removeJavaScriptLikeExtension(String fileName) {
		return Util.getNameWithoutJavaLikeExtension(fileName);
	}

	/**
	 * Removes the given pre-processing resource changed listener.
	 * <p>
	 * Has no affect if an identical listener is not registered.
	 *
	 * @param listener the listener
	 */
	public static void removePreProcessingResourceChangedListener(IResourceChangeListener listener) {
		JavaModelManager.getJavaModelManager().deltaState.removePreResourceChangedListener(listener);
	}



	/**
	 * Runs the given action as an atomic JavaScript model operation.
	 * <p>
	 * After running a method that modifies javaScript elements,
	 * registered listeners receive after-the-fact notification of
	 * what just transpired, in the form of a element changed event.
	 * This method allows clients to call a number of
	 * methods that modify javaScript elements and only have element
	 * changed event notifications reported at the end of the entire
	 * batch.
	 * </p>
	 * <p>
	 * If this method is called outside the dynamic scope of another such
	 * call, this method runs the action and then reports a single
	 * element changed event describing the net effect of all changes
	 * done to javaScript elements by the action.
	 * </p>
	 * <p>
	 * If this method is called in the dynamic scope of another such
	 * call, this method simply runs the action.
	 * </p>
	 *
	 * @param action the action to perform
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @exception CoreException if the operation failed.
	 */
	public static void run(IWorkspaceRunnable action, IProgressMonitor monitor) throws CoreException {
		run(action, ResourcesPlugin.getWorkspace().getRoot(), monitor);
	}
	/**
	 * Runs the given action as an atomic JavaScript model operation.
	 * <p>
	 * After running a method that modifies javaScript elements,
	 * registered listeners receive after-the-fact notification of
	 * what just transpired, in the form of a element changed event.
	 * This method allows clients to call a number of
	 * methods that modify javaScript elements and only have element
	 * changed event notifications reported at the end of the entire
	 * batch.
	 * </p>
	 * <p>
	 * If this method is called outside the dynamic scope of another such
	 * call, this method runs the action and then reports a single
	 * element changed event describing the net effect of all changes
	 * done to javaScript elements by the action.
	 * </p>
	 * <p>
	 * If this method is called in the dynamic scope of another such
	 * call, this method simply runs the action.
	 * </p>
	 * <p>
 	 * The supplied scheduling rule is used to determine whether this operation can be
	 * run simultaneously with workspace changes in other threads. See
	 * <code>IWorkspace.run(...)</code> for more details.
 	 * </p>
	 *
	 * @param action the action to perform
	 * @param rule the scheduling rule to use when running this operation, or
	 * <code>null</code> if there are no scheduling restrictions for this operation.
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @exception CoreException if the operation failed.
	 */
	public static void run(IWorkspaceRunnable action, ISchedulingRule rule, IProgressMonitor monitor) throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		if (workspace.isTreeLocked()) {
			new BatchOperation(action).run(monitor);
		} else {
			// use IWorkspace.run(...) to ensure that a build will be done in autobuild mode
			workspace.run(new BatchOperation(action), rule, IWorkspace.AVOID_UPDATE, monitor);
		}
	}
	/**
	 * Bind a container reference path to some actual containers (<code>IJsGlobalScopeContainer</code>).
	 * This API must be invoked whenever changes in container need to be reflected onto the JavaModel.
	 * Containers can have distinct values in different projects, therefore this API considers a
	 * set of projects with their respective containers.
	 * <p>
	 * <code>containerPath</code> is the path under which these values can be referenced through
	 * container includepath entries (<code>IIncludePathEntry#CPE_CONTAINER</code>). A container path
	 * is formed by a first ID segment followed with extra segments, which can be used as additional hints
	 * for the resolution. The container ID is used to identify a <code>JsGlobalScopeContainerInitializer</code>
	 * registered on the extension point "org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer".
	 * <p>
	 * There is no assumption that each individual container value passed in argument
	 * (<code>respectiveContainers</code>) must answer the exact same path when requested
	 * <code>IJsGlobalScopeContainer#getPath</code>.
	 * Indeed, the containerPath is just an indication for resolving it to an actual container object. It can be
	 * delegated to a <code>JsGlobalScopeContainerInitializer</code>, which can be activated through the extension
	 * point "org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer").
	 * <p>
	 * In reaction to changing container values, the JavaModel will be updated to reflect the new
	 * state of the updated container. A combined JavaScript element delta will be notified to describe the corresponding
	 * includepath changes resulting from the container update. This operation is batched, and automatically eliminates
	 * unnecessary updates (new container is same as old one). This operation acquires a lock on the workspace's root.
	 * <p>
	 * This functionality cannot be used while the workspace is locked, since
	 * it may create/remove some resource markers.
	 * <p>
	 * Includepath container values are persisted locally to the workspace, but
	 * are not preserved from a session to another. It is thus highly recommended to register a
	 * <code>JsGlobalScopeContainerInitializer</code> for each referenced container
	 * (through the extension point "org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer").
	 * <p>
	 * Note: setting a container to <code>null</code> will cause it to be lazily resolved again whenever
	 * its value is required. In particular, this will cause a registered initializer to be invoked
	 * again.
	 * <p>
	 * @param containerPath - the name of the container reference, which is being updated
	 * @param affectedProjects - the set of projects for which this container is being bound
	 * @param respectiveContainers - the set of respective containers for the affected projects
	 * @param monitor a monitor to report progress
	 * @throws JavaScriptModelException
	 * @see JsGlobalScopeContainerInitializer
	 * @see #getJsGlobalScopeContainer(IPath, IJavaScriptProject)
	 * @see IJsGlobalScopeContainer
	 */
	public static void setJsGlobalScopeContainer(IPath containerPath, IJavaScriptProject[] affectedProjects, IJsGlobalScopeContainer[] respectiveContainers, IProgressMonitor monitor) throws JavaScriptModelException {
		if (affectedProjects.length != respectiveContainers.length)
			Assert.isTrue(false, "Projects and containers collections should have the same size"); //$NON-NLS-1$
		SetContainerOperation operation = new SetContainerOperation(containerPath, affectedProjects, respectiveContainers);
//		operation.runOperation(monitor);
		
		operation.progressMonitor=monitor;
		
		operation.execute();
		
		
		
	}

	/**
	 * Sets the value of the given includepath variable.
	 * The path must have at least one segment.
	 * <p>
	 * This functionality cannot be used while the resource tree is locked.
	 * <p>
	 * Includepath variable values are persisted locally to the workspace, and
	 * are preserved from session to session.
	 * <p>
	 *
	 * @param variableName the name of the includepath variable
	 * @param path the path
	 * @throws JavaScriptModelException
	 * @see #getIncludepathVariable(String)
	 *
	 * @deprecated Use {@link #setIncludepathVariable(String, IPath, IProgressMonitor)} instead
	 */
	public static void setIncludepathVariable(String variableName, IPath path)
		throws JavaScriptModelException {

		setIncludepathVariable(variableName, path, null);
	}

	/**
	 * Sets the value of the given includepath variable.
	 * The path must not be null.
	 * <p>
	 * This functionality cannot be used while the resource tree is locked.
	 * <p>
	 * Includepath variable values are persisted locally to the workspace, and
	 * are preserved from session to session.
	 * <p>
	 * Updating a variable with the same value has no effect.
	 *
	 * @param variableName the name of the includepath variable
	 * @param path the path
	 * @param monitor a monitor to report progress
	 * @throws JavaScriptModelException
	 * @see #getIncludepathVariable(String)
	 */
	public static void setIncludepathVariable(
		String variableName,
		IPath path,
		IProgressMonitor monitor)
		throws JavaScriptModelException {

		if (path == null) Assert.isTrue(false, "Variable path cannot be null"); //$NON-NLS-1$
		setIncludepathVariables(new String[]{variableName}, new IPath[]{ path }, monitor);
	}

	/**
	 * Sets the values of all the given includepath variables at once.
	 * Null paths can be used to request corresponding variable removal.
	 * <p>
	 * A combined JavaScript element delta will be notified to describe the corresponding
	 * includepath changes resulting from the variables update. This operation is batched,
	 * and automatically eliminates unnecessary updates (new variable is same as old one).
	 * This operation acquires a lock on the workspace's root.
	 * <p>
	 * This functionality cannot be used while the workspace is locked, since
	 * it may create/remove some resource markers.
	 * <p>
	 * Includepath variable values are persisted locally to the workspace, and
	 * are preserved from session to session.
	 * <p>
	 * Updating a variable with the same value has no effect.
	 *
	 * @param variableNames an array of names for the updated includepath variables
	 * @param paths an array of path updates for the modified includepath variables (null
	 *       meaning that the corresponding value will be removed
	 * @param monitor a monitor to report progress
	 * @throws JavaScriptModelException
	 * @see #getIncludepathVariable(String)
	 */
	public static void setIncludepathVariables(
		String[] variableNames,
		IPath[] paths,
		IProgressMonitor monitor)
		throws JavaScriptModelException {

		if (variableNames.length != paths.length)	Assert.isTrue(false, "Variable names and paths collections should have the same size"); //$NON-NLS-1$
		SetVariablesOperation operation = new SetVariablesOperation(variableNames, paths, true/*update preferences*/);
		operation.runOperation(monitor);
	}

	/**
	 * Sets the default's validator options inside the given options map according
	 * to the given compliance.
	 *
	 * <p>The given compliance must be one of the compliance supported by the validator.
	 * See {@link #getDefaultOptions()} for a list of compliance values.</p>
	 *
	 * <p>The list of modified options is:</p>
	 * <ul>
	 * <li>{@link #COMPILER_CODEGEN_TARGET_PLATFORM}</li>
	 * <li>{@link #COMPILER_SOURCE}</li>
	 * <li>{@link #COMPILER_COMPLIANCE}</li>
	 * <li>{@link #COMPILER_PB_ASSERT_IDENTIFIER}</li>
	 * <li>{@link #COMPILER_PB_ENUM_IDENTIFIER}</li>
	 * </ul>
	 *
	 * <p>If the given compliance is unknown, the given map is unmodified.</p>
	 *
	 * @param compliance the given compliance
	 * @param options the given options map
	 */
	public static void setComplianceOptions(String compliance, Map options) {
		switch((int) (CompilerOptions.versionToJdkLevel(compliance) >>> 16)) {
			case ClassFileConstants.MAJOR_VERSION_1_3:
				options.put(JavaScriptCore.COMPILER_COMPLIANCE, JavaScriptCore.VERSION_1_3);
				options.put(JavaScriptCore.COMPILER_SOURCE, JavaScriptCore.VERSION_1_3);
				options.put(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaScriptCore.VERSION_1_1);
				options.put(JavaScriptCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaScriptCore.IGNORE);
				break;
			case ClassFileConstants.MAJOR_VERSION_1_4:
				options.put(JavaScriptCore.COMPILER_COMPLIANCE, JavaScriptCore.VERSION_1_4);
				options.put(JavaScriptCore.COMPILER_SOURCE, JavaScriptCore.VERSION_1_3);
				options.put(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaScriptCore.VERSION_1_2);
				options.put(JavaScriptCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaScriptCore.WARNING);
				break;
			case ClassFileConstants.MAJOR_VERSION_1_5:
				options.put(JavaScriptCore.COMPILER_COMPLIANCE, JavaScriptCore.VERSION_1_5);
				options.put(JavaScriptCore.COMPILER_SOURCE, JavaScriptCore.VERSION_1_5);
				options.put(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaScriptCore.VERSION_1_5);
				options.put(JavaScriptCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaScriptCore.ERROR);
				break;
			case ClassFileConstants.MAJOR_VERSION_1_6:
				options.put(JavaScriptCore.COMPILER_COMPLIANCE, JavaScriptCore.VERSION_1_6);
				options.put(JavaScriptCore.COMPILER_SOURCE, JavaScriptCore.VERSION_1_6);
				options.put(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaScriptCore.VERSION_1_6);
				options.put(JavaScriptCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaScriptCore.ERROR);
				break;
			case ClassFileConstants.MAJOR_VERSION_1_7:
				options.put(JavaScriptCore.COMPILER_COMPLIANCE, JavaScriptCore.VERSION_1_7);
				options.put(JavaScriptCore.COMPILER_SOURCE, JavaScriptCore.VERSION_1_7);
				options.put(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaScriptCore.VERSION_1_7);
				options.put(JavaScriptCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaScriptCore.ERROR);
		}
	}

	/**
	 * Sets the current table of options. All and only the options explicitly
	 * included in the given table are remembered; all previous option settings
	 * are forgotten, including ones not explicitly mentioned.
	 * <p>
	 * For a complete description of the configurable options, see
	 * <code>getDefaultOptions</code>.
	 * </p>
	 *
	 * @param newOptions
	 *            the new options (key type: <code>String</code>; value type:
	 *            <code>String</code>), or <code>null</code> to reset all
	 *            options to their default values
	 * @see JavaScriptCore#getDefaultOptions()
	 * @see org.eclipse.wst.jsdt.internal.core.JavaCorePreferenceInitializer for changing default settings
	 */
	public static void setOptions(Hashtable newOptions) {
		JavaModelManager.getJavaModelManager().setOptions(newOptions);
	}

	/* (non-Javadoc)
	 * Shutdown the JavaScriptCore plug-in.
	 * <p>
	 * De-registers the JavaModelManager as a resource changed listener and save participant.
	 * <p>
	 * @see org.eclipse.core.runtime.Plugin#stop(BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		try {
			JavaModelManager.getJavaModelManager().shutdown();
		} finally {
			// ensure we call super.stop as the last thing
			super.stop(context);
		}
	}

	/* (non-Javadoc)
	 * Startup the JavaScriptCore plug-in.
	 * <p>
	 * Registers the JavaModelManager as a resource changed listener and save participant.
	 * Starts the background indexing, and restore saved includepath variable values.
	 * <p>
	 * @throws Exception
	 * @see org.eclipse.core.runtime.Plugin#start(BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		JavaModelManager.getJavaModelManager().startup();
	}
	public static String getSystemPath()
	{
		URL url=FileLocator.find(getJavaScriptCore().getBundle(),new Path("libraries"),null); //$NON-NLS-1$
		return url.getFile();
	}


	public static boolean isReadOnly(IResource resource)
	{
		QualifiedName qn=new QualifiedName(JavaScriptCore.PLUGIN_ID,JavaScriptCore.READ_ONLY_SOURCE_PROPERTY);
		try {
			String persistentProperty = resource.getPersistentProperty(qn);
			return "true".equals(persistentProperty);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	/** 
	 * Returns default inclusion patterns
	 * 
	 * return IPath[] array of IPath elements to exclude
	 */
	public IPath[] getDefaultClasspathExclusionPatterns() {
		String defaultExclusions = JavaModelManager.getJavaModelManager().getOption(JavaScriptCore.CORE_DEFAULT_CLASSPATH_EXCLUSION_PATTERNS);
		if (defaultExclusions == null || defaultExclusions.trim().length() == 0)
			return ClasspathEntry.EXCLUDE_NONE;

		Set<IPath> result = new HashSet<IPath>();
		String[] exclusions = defaultExclusions.split(","); //$NON-NLS-1$
		for (int i = 0; exclusions != null && i < exclusions.length; i++) {
			exclusions[i] = exclusions[i] == null ? null : exclusions[i].trim();
			Path exclusion = exclusions[i] == null || exclusions[i].length() == 0 ? 
						null : new Path(exclusions[i]);
			if (exclusion != null && !result.contains(exclusion))
				result.add(exclusion);
		}

		return result.size() == 0 ? ClasspathEntry.EXCLUDE_NONE :
					result.toArray(new IPath[result.size()]);
	}

}
