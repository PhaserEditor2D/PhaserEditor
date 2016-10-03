/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.util;

import java.text.MessageFormat;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.wst.jsdt.internal.core.util.messages";//$NON-NLS-1$

	private Messages() {
		// Do not instantiate
	}

	public static String hierarchy_nullProject;
	public static String hierarchy_nullRegion;
	public static String hierarchy_nullFocusType;
	public static String hierarchy_creating;
	public static String hierarchy_creatingOnType;
	public static String element_doesNotExist;
	public static String element_notOnClasspath;
	public static String element_invalidClassFileName;
	public static String element_reconciling;
	public static String element_attachingSource;
	public static String element_invalidResourceForProject;
	public static String element_nullName;
	public static String element_nullType;
	public static String element_illegalParent;
	public static String javamodel_initialization;
	public static String javamodel_building_after_upgrade;
	public static String javamodel_configuring;
	public static String javamodel_configuring_classpath_containers;
	public static String javamodel_configuring_searchengine;
	public static String javamodel_getting_build_state_number;
	public static String javamodel_refreshing_external_jars;
	public static String operation_needElements;
	public static String operation_needName;
	public static String operation_needPath;
	public static String operation_needAbsolutePath;
	public static String operation_needString;
	public static String operation_notSupported;
	public static String operation_cancelled;
	public static String operation_nullContainer;
	public static String operation_nullName;
	public static String operation_copyElementProgress;
	public static String operation_moveElementProgress;
	public static String operation_renameElementProgress;
	public static String operation_copyResourceProgress;
	public static String operation_moveResourceProgress;
	public static String operation_renameResourceProgress;
	public static String operation_createUnitProgress;
	public static String operation_createFieldProgress;
	public static String operation_createImportsProgress;
	public static String operation_createMethodProgress;
	public static String operation_createPackageProgress;
	public static String operation_createPackageFragmentProgress;
	public static String operation_createTypeProgress;
	public static String operation_deleteElementProgress;
	public static String operation_deleteResourceProgress;
	public static String operation_cannotRenameDefaultPackage;
	public static String operation_pathOutsideProject;
	public static String operation_sortelements;
	public static String workingCopy_commit;
	public static String build_preparingBuild;
	public static String build_readStateProgress;
	public static String build_saveStateProgress;
	public static String build_saveStateComplete;
	public static String build_readingDelta;
	public static String build_analyzingDeltas;
	public static String build_analyzingSources;
	public static String build_compiling;
	public static String build_foundHeader;
	public static String build_fixedHeader;
	public static String build_oneError;
	public static String build_oneWarning;
	public static String build_multipleErrors;
	public static String build_multipleWarnings;
	public static String build_done;
	public static String build_wrongFileFormat;
	public static String build_cannotSaveState;
	public static String build_cannotSaveStates;
	public static String build_initializationError;
	public static String build_serializationError;
//	public static String build_classFileCollision;
//	public static String build_duplicateClassFile;
//	public static String build_duplicateResource;
//	public static String build_inconsistentClassFile;
	public static String build_inconsistentProject;
	public static String build_incompleteClassPath;
	public static String build_missingSourceFile;
	public static String build_prereqProjectHasClasspathProblems;
	public static String build_prereqProjectMustBeRebuilt;
	public static String build_abortDueToClasspathProblems;
	public static String status_cannot_retrieve_attached_javadoc;
	public static String status_cannotUseDeviceOnPath;
	public static String status_coreException;
	public static String status_defaultPackageReadOnly;
	public static String status_evaluationError;
	public static String status_JDOMError;
	public static String status_IOException;
	public static String status_indexOutOfBounds;
	public static String status_invalidContents;
	public static String status_invalidDestination;
	public static String status_invalidName;
	public static String status_invalidPackage;
	public static String status_invalidPath;
	public static String status_invalidProject;
	public static String status_invalidResource;
	public static String status_invalidResourceType;
	public static String status_invalidSibling;
	public static String status_nameCollision;
	public static String status_noLocalContents;
	public static String status_OK;
	public static String status_readOnly;
	public static String status_targetException;
	public static String status_unknown_javadoc_format;
	public static String status_updateConflict;
	public static String classpath_buildPath;
	public static String classpath_cannotNestEntryInEntry;
	public static String classpath_cannotNestEntryInEntryNoExclusion;
	public static String classpath_cannotNestEntryInLibrary;
	public static String classpath_cannotReadClasspathFile;
	public static String classpath_cannotReferToItself;
	public static String classpath_closedProject;
	public static String classpath_cycle;
	public static String classpath_duplicateEntryPath;
	public static String classpath_illegalContainerPath;
	public static String classpath_illegalEntryInClasspathFile;
	public static String classpath_illegalLibraryPath;
	public static String classpath_illegalLibraryArchive;
	public static String classpath_illegalExternalFolder;
	public static String classpath_illegalProjectPath;
	public static String classpath_illegalSourceFolderPath;
	public static String classpath_illegalVariablePath;
	public static String classpath_invalidContainer;
	public static String classpath_mustEndWithSlash;
	public static String classpath_unboundContainerPath;
	public static String classpath_unboundLibrary;
	public static String classpath_unboundProject;
	public static String classpath_unboundSourceAttachment;
	public static String classpath_unboundSourceFolder;
	public static String classpath_unboundVariablePath;
	public static String classpath_unknownKind;
	public static String classpath_xmlFormatError;
	public static String classpath_disabledInclusionExclusionPatterns;
	public static String classpath_duplicateEntryExtraAttribute;
	public static String classpath_deprecated_variable;
	public static String file_notFound;
	public static String file_badFormat;
	public static String path_nullPath;
	public static String path_mustBeAbsolute;
	public static String cache_invalidLoadFactor;
	public static String savedState_jobName;
	public static String restrictedAccess_project;
	public static String restrictedAccess_library;
	public static String restrictedAccess_constructor_project;
	public static String restrictedAccess_constructor_library;
	public static String restrictedAccess_field_project;
	public static String restrictedAccess_field_library;
	public static String restrictedAccess_method_project;
	public static String restrictedAccess_method_library;
	public static String convention_unit_nullName;
	public static String convention_unit_notJavaName;
	public static String convention_classFile_nullName;
	public static String convention_classFile_notClassFileName;
	public static String convention_illegalIdentifier;
	public static String convention_import_nullImport;
	public static String convention_import_unqualifiedImport;
	public static String convention_type_nullName;
	public static String convention_type_nameWithBlanks;
	public static String convention_type_dollarName;
	public static String convention_type_lowercaseName;
	public static String convention_type_invalidName;
	public static String convention_package_nullName;
	public static String convention_package_emptyName;
	public static String convention_package_dotName;
	public static String convention_package_nameWithBlanks;
	public static String convention_package_consecutiveDotsName;
	public static String convention_package_uppercaseName;
	public static String dom_cannotDetail;
	public static String dom_nullTypeParameter;
	public static String dom_nullNameParameter;
	public static String dom_nullReturnType;
	public static String dom_nullExceptionType;
	public static String dom_mismatchArgNamesAndTypes;
	public static String dom_addNullChild;
	public static String dom_addIncompatibleChild;
	public static String dom_addChildWithParent;
	public static String dom_unableAddChild;
	public static String dom_addAncestorAsChild;
	public static String dom_addNullSibling;
	public static String dom_addSiblingBeforeRoot;
	public static String dom_addIncompatibleSibling;
	public static String dom_addSiblingWithParent;
	public static String dom_addAncestorAsSibling;
	public static String dom_addNullInterface;
	public static String dom_nullInterfaces;
	public static String importRewrite_processDescription;
	public static String correction_nullRequestor;
	public static String correction_nullUnit;
	public static String engine_searching;
	public static String engine_searching_indexing;
	public static String engine_searching_matching;
	public static String exception_wrongFormat;
	public static String process_name;
	public static String manager_filesToIndex;
	public static String manager_indexingInProgress;
	public static String converter_ConfiguringForJavaScript;
	public static String converter_ConfiguringForBrowser;
	
	public static String PostponedRunnablesManager_job_title;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	/**
	 * Bind the given message's substitution locations with the given string values.
	 *
	 * @param message the message to be manipulated
	 * @return the manipulated String
	 */
	public static String bind(String message) {
		return bind(message, null);
	}

	/**
	 * Bind the given message's substitution locations with the given string values.
	 *
	 * @param message the message to be manipulated
	 * @param binding the object to be inserted into the message
	 * @return the manipulated String
	 */
	public static String bind(String message, Object binding) {
		return bind(message, new Object[] {binding});
	}

	/**
	 * Bind the given message's substitution locations with the given string values.
	 *
	 * @param message the message to be manipulated
	 * @param binding1 An object to be inserted into the message
	 * @param binding2 A second object to be inserted into the message
	 * @return the manipulated String
	 */
	public static String bind(String message, Object binding1, Object binding2) {
		return bind(message, new Object[] {binding1, binding2});
	}

	/**
	 * Bind the given message's substitution locations with the given string values.
	 *
	 * @param message the message to be manipulated
	 * @param bindings An array of objects to be inserted into the message
	 * @return the manipulated String
	 */
	public static String bind(String message, Object[] bindings) {
		return MessageFormat.format(message, bindings);
	}
}
