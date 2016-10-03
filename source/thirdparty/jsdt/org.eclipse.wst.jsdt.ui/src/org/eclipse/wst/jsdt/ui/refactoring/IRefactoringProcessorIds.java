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
package org.eclipse.wst.jsdt.ui.refactoring;

import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameCompilationUnitProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameFieldProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameJavaProjectProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameMethodProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenamePackageProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameResourceProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameSourceFolderProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.JavaCopyProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.JavaDeleteProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.JavaMoveProcessor;

/**
 * Interface to define the processor IDs provided by the JDT refactoring.
 * 
 * <p>
 * This interface declares static final fields only; it is not intended to be 
 * implemented.
 * </p>
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * 
 * 
 */
public interface IRefactoringProcessorIds {

	/**
	 * Processor ID of the rename JavaScript project processor
	 * (value <code>"org.eclipse.wst.jsdt.ui.renameJavaProjectProcessor"</code>).
	 * 
	 * The rename JavaScript project processor loads the following participants:
	 * <ul>
	 *   <li>participants registered for renaming <code>IJavaScriptProject</code>.</li>
	 *   <li>participants registered for renaming <code>IProject</code>.</li>
	 * </ul>
	 */
	public static String RENAME_JAVA_PROJECT_PROCESSOR= RenameJavaProjectProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the rename source folder
	 * (value <code>"org.eclipse.wst.jsdt.ui.renameSourceFolderProcessor"</code>).
	 * 
	 * The rename package fragment root processor loads the following participants:
	 * <ul>
	 *   <li>participants registered for renaming <code>IPackageFragmentRoot</code>.</li>
	 *   <li>participants registered for renaming <code>IFolder</code>.</li>
	 * </ul>
	 */
	public static String RENAME_SOURCE_FOLDER_PROCESSOR= RenameSourceFolderProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the rename package fragment processor
	 * (value <code>"org.eclipse.wst.jsdt.ui.renamePackageProcessor"</code>).
	 * 
	 * The rename package fragment processor loads the following participants:
	 * <ul>
	 *   <li>participants registered for renaming <code>IPackageFragment</code>.</li>
	 *   <li>participants registered for moving <code>IFile</code> to participate in the
	 *       file moves caused by the package fragment rename.</li>
	 *   <li>participants registered for creating <code>IFolder</code> if the package
	 *       rename results in creating a new destination folder.</li>
	 *   <li>participants registered for deleting <code>IFolder</code> if the package
	 *       rename results in deleting the folder corresponding to the package
	 *       fragment to be renamed.</li>
	 * </ul>
	 * 
	 * <p>Since 3.3:</p>
	 * 
	 * <p>The refactoring processor moves and renames JavaScript elements and resources.
	 * Rename package fragment participants can retrieve the new location of
	 * JavaScript elements and resources through the interfaces
	 * {@link org.eclipse.wst.jsdt.core.refactoring.IJavaScriptElementMapper} and {@link org.eclipse.ltk.core.refactoring.IResourceMapper}, which can be
	 * retrieved from the processor using the getAdapter() method.</p>
	 */
	public static String RENAME_PACKAGE_FRAGMENT_PROCESSOR= RenamePackageProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the rename compilation unit processor
	 * (value <code>"org.eclipse.wst.jsdt.ui.renameCompilationUnitProcessor"</code>).
	 * 
	 * The rename compilation unit processor loads the following participants:
	 * <ul>
	 *   <li>participants registered for renaming <code>IJavaScriptUnit</code>.</li>
	 *   <li>participants registered for renaming <code>IFile</code>.</li>
	 *   <li>participants registered for renaming <code>IType</code> if the
	 *       compilation unit contains a top level type.</li>
	 * </ul>
	 */
	public static String RENAME_COMPILATION_UNIT_PROCESSOR= RenameCompilationUnitProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the rename type processor
	 * (value <code>"org.eclipse.wst.jsdt.ui.renameTypeProcessor"</code>).
	 * 
	 * The rename type processor loads the following participants:
	 * <ul>
	 *   <li>participants registered for renaming <code>IType</code>.</li>
	 *   <li>participants registered for renaming <code>IJavaScriptUnit</code> if the
	 *       type is a public top level type.</li>
	 *   <li>participants registered for renaming <code>IFile</code> if the compilation 
	 *       unit gets rename as well.</li>
	 * </ul>
	 * 
	 * <p>Since 3.2:</p>
	 * 
	 * <p>Participants that declare <pre> &lt;param name="handlesSimilarDeclarations" value="false"/&gt; </pre>
	 * in their extension contribution will not be loaded if the user selects the 
	 * "update similar declarations" feature.</p> 
	 * 
	 * <p>Rename type participants can retrieve information about similar declarations by casting the
	 * RenameArguments to RenameTypeArguments. The new signatures of similar declarations
	 * (and of other JavaScript elements or resources) are available 
	 * through the interfaces {@link org.eclipse.wst.jsdt.core.refactoring.IJavaScriptElementMapper} and {@link org.eclipse.ltk.core.refactoring.IResourceMapper}, which can be retrieved from the 
	 * processor using the getAdapter() method.</p>
	 * 
	 */
	public static String RENAME_TYPE_PROCESSOR= RenameTypeProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the rename method processor
	 * (value <code>"org.eclipse.wst.jsdt.ui.renameMethodProcessor"</code>).
	 * 
	 * The rename method processor loads the following participants:
	 * <ul>
	 *   <li>participants registered for renaming <code>IFunction</code>. Renaming
	 *       virtual methods will rename methods with the same name in the type
	 *       hierarchy of the type declaring the method to be renamed as well.
	 *       For those derived methods participants will be loaded as well.</li>
	 * </ul>
	 */
	public static String RENAME_METHOD_PROCESSOR= RenameMethodProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the rename field processor
	 * (value <code>"org.eclipse.wst.jsdt.ui.renameFieldProcessor"</code>).
	 * 
	 * The rename filed processor loads the following participants:
	 * <ul>
	 *   <li>participants registered for renaming <code>IField</code>.</li>
	 *   <li>participants registered for renaming <code>IFunction</code> if 
	 *       corresponding setter and getter methods are renamed as well.</li>
	 * </ul>
	 */
	public static String RENAME_FIELD_PROCESSOR= RenameFieldProcessor.IDENTIFIER;

//	/**
//	 * Processor ID of the rename enum constant processor
//	 * (value <code>"org.eclipse.wst.jsdt.ui.renameEnumConstProcessor"</code>).
//	 * 
//	 * The rename filed processor loads the following participants:
//	 * <ul>
//	 *   <li>participants registered for renaming <code>IField</code>.</li>
//	 * </ul>
//	 * 
//	 */
//	public static String RENAME_ENUM_CONSTANT_PROCESSOR= RenameEnumConstProcessor.IDENTIFIER;

	/**
	 * Processor ID of the rename resource processor
	 * (value <code>"org.eclipse.wst.jsdt.ui.renameResourceProcessor"</code>).
	 * 
	 * The rename resource processor loads the following participants:
	 * <ul>
	 *   <li>participants registered for renaming <code>IResource</code>.</li>
	 * </ul>
	 */
	public static String RENAME_RESOURCE_PROCESSOR= RenameResourceProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the move resource processor
	 * (value <code>"org.eclipse.wst.jsdt.ui.MoveProcessor"</code>).
	 * 
	 * The move processor loads the following participants, depending on the type of
	 * element that gets moved:
	 * <ul>
	 *   <li><code>IPackageFragmentRoot</code>: participants registered for moving 
	 *       package fragment roots together with participants moving a <code>IFolder
	 *       </code>.</li>
	 *   <li><code>IPackageFragment</code>: participants registered for moving
	 *       package fragments. Additionally move file, create folder and delete
	 *       folder participants are loaded to reflect the resource changes
	 *       caused by a moving a package fragment.</li>
	 *   <li><code>IJavaScriptUnit</code>: participants registered for moving
	 *       compilation units and <code>IFile</code>. If the compilation unit 
	 *       contains top level types, participants for these types are loaded
	 *       as well.</li>
	 *   <li><code>IResource</code>: participants registered for moving resources.</li>
	 * </ul>
	 */
	public static String MOVE_PROCESSOR= JavaMoveProcessor.IDENTIFIER;
	
	/**
	 * Processor ID of the move static member processor
	 * (value <code>"org.eclipse.wst.jsdt.ui.MoveStaticMemberProcessor"</code>).
	 * 
	 * The move static members processor loads participants registered for the
	 * static JavaScript element that gets moved. No support is available to participate
	 * in non static member moves.
	 */
	public static String MOVE_STATIC_MEMBERS_PROCESSOR= "org.eclipse.wst.jsdt.ui.MoveStaticMemberProcessor"; //$NON-NLS-1$
	
	/**
	 * Processor ID of the delete resource processor
	 * (value <code>"org.eclipse.wst.jsdt.ui.DeleteProcessor"</code>).
	 * 
	 * The delete processor loads the following participants, depending on the type of
	 * element that gets deleted:
	 * <ul>
	 *   <li><code>IJavaScriptProject</code>: participants registered for deleting <code>IJavaScriptProject
	 *       </code> and <code>IProject</code></li>.
	 *   <li><code>IPackageFragmentRoot</code>: participants registered for deleting 
	 *       <code>IPackageFragmentRoot</code> and <code>IFolder</code>.
	 *   <li><code>IPackageFragment</code>: participants registered for deleting 
	 *       <code>IPackageFragment</code>. Additionally delete file and  delete folder
	 *       participants are loaded to reflect the resource changes caused by 
	 *       deleting a package fragment.</li>
	 *   <li><code>IJavaScriptUnit</code>: participants registered for deleting compilation
	 *       units and files. Additionally type delete participants are loaded to reflect the
	 *       deletion of the top level types declared in the compilation unit.</li>
	 *   <li><code>IType</code>: participants registered for deleting types. Additional 
	 *       compilation unit and file delete participants are loaded if the type to be deleted 
	 *       is the only top level type of a compilation unit.</li>
	 *   <li><code>IMember</code>: participants registered for deleting members.</li>
	 *   <li><code>IResource</code>: participants registered for deleting resources.</li>
	 * </ul>
	 */
	public static String DELETE_PROCESSOR= JavaDeleteProcessor.IDENTIFIER;	

	/**
	 * Processor ID of the copy processor (value <code>"org.eclipse.wst.jsdt.ui.CopyProcessor"</code>).
	 * 
	 * The copy processor is used when copying elements via drag and drop or when pasting
	 * elements from the clipboard. The copy processor loads the following participants,
	 * depending on the type of the element that gets copied:
	 * <ul>
	 *   <li><code>IJavaScriptProject</code>: no participants are loaded.</li>
	 *   <li><code>IPackageFragmentRoot</code>: participants registered for copying 
	 *       <code>IPackageFragmentRoot</code> and <code>org.eclipse.core.resources.mapping.ResourceMapping</code>.</li>
	 *   <li><code>IPackageFragment</code>: participants registered for copying 
	 *       <code>IPackageFragment</code> and <code>org.eclipse.core.resources.mapping.ResourceMapping</code>.</li>
	 *   <li><code>IJavaScriptUnit</code>: participants registered for copying 
	 *       <code>IJavaScriptUnit</code> and <code>org.eclipse.core.resources.mapping.ResourceMapping</code>.</li>
	 *   <li><code>IType</code>: like IJavaScriptUnit if the primary top level type is copied.
	 *       Otherwise no participants are loaded.</li>
	 *   <li><code>IMember</code>: no participants are loaded.</li>
	 *   <li><code>IFolder</code>: participants registered for copying folders.</li>
	 *   <li><code>IFile</code>: participants registered for copying files.</li>
	 * </ul>
	 * <p>
	 * Use the method {@link org.eclipse.core.resources.mapping.ResourceMapping#accept(org.eclipse.core.resources.mapping.ResourceMappingContext context, org.eclipse.core.resources.IResourceVisitor visitor, org.eclipse.core.runtime.IProgressMonitor monitor)} 
	 * to enumerate the resources which form the JavaScript element. <code>org.eclipse.core.resources.mapping.ResourceMappingContext.LOCAL_CONTEXT</code> 
	 * should be use as the <code>org.eclipse.core.resources.mapping.ResourceMappingContext</code> passed to the accept method.
	 * </p>
	 * @see org.eclipse.core.resources.mapping.ResourceMapping
	 * 
	 */
	public static String COPY_PROCESSOR= JavaCopyProcessor.IDENTIFIER;
}
