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
package org.eclipse.wst.jsdt.core.refactoring;


/**
 * Interface for refactoring ids offered by the JDT tooling.
 * <p>
 * This interface provides refactoring ids for refactorings offered by the JDT
 * tooling. Refactoring instances corresponding to such an id may be
 * instantiated by the refactoring framework using
 * {@link org.eclipse.ltk.core.refactoring.RefactoringCore#getRefactoringContribution(String)}. The resulting
 * refactoring instance may be executed on the workspace with a
 * {@link org.eclipse.ltk.core.refactoring.PerformRefactoringOperation}.
 * <p>
 * Clients may obtain customizable refactoring descriptors for a certain
 * refactoring by calling
 * {@link org.eclipse.ltk.core.refactoring.RefactoringCore#getRefactoringContribution(String)} with the
 * appropriate refactoring id and then calling
 * {@link org.eclipse.ltk.core.refactoring.RefactoringContribution#createDescriptor()} to obtain a customizable
 * refactoring descriptor. The concrete subtype of refactoring descriptors is
 * dependent from the <code>id</code> argument.
 * </p>
 * <p>
 * Note: this interface is not intended to be implemented by clients.
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IJavaScriptRefactorings {

	/**
	 * Refactoring id of the 'Change Method Signature' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.change.method.signature</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.ChangeMethodSignatureDescriptor}.
	 * </p>
	 */
	public static final String CHANGE_METHOD_SIGNATURE= "org.eclipse.wst.jsdt.ui.change.method.signature"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Convert Anonymous To Nested' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.convert.anonymous</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.ConvertAnonymousDescriptor}.
	 * </p>
	 */
	public static final String CONVERT_ANONYMOUS= "org.eclipse.wst.jsdt.ui.convert.anonymous"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Convert Local Variable to Field' refactoring
	 * (value: <code>org.eclipse.wst.jsdt.ui.promote.temp</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.ConvertLocalVariableDescriptor}.
	 * </p>
	 */
	public static final String CONVERT_LOCAL_VARIABLE= "org.eclipse.wst.jsdt.ui.promote.temp"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Convert Member Type to Top Level' refactoring
	 * (value: <code>org.eclipse.wst.jsdt.ui.move.inner</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.ConvertMemberTypeDescriptor}.
	 * </p>
	 */
	public static final String CONVERT_MEMBER_TYPE= "org.eclipse.wst.jsdt.ui.move.inner"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Copy' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.copy</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.CopyDescriptor}.
	 * </p>
	 */
	public static final String COPY= "org.eclipse.wst.jsdt.ui.copy"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Delete' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.delete</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.DeleteDescriptor}.
	 * </p>
	 */
	public static final String DELETE= "org.eclipse.wst.jsdt.ui.delete"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Encapsulate Field' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.self.encapsulate</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.EncapsulateFieldDescriptor}.
	 * </p>
	 */
	public static final String ENCAPSULATE_FIELD= "org.eclipse.wst.jsdt.ui.self.encapsulate"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Extract Constant' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.extract.constant</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.ExtractConstantDescriptor}.
	 * </p>
	 */
	public static final String EXTRACT_CONSTANT= "org.eclipse.wst.jsdt.ui.extract.constant"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Extract Interface' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.extract.interface</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.ExtractInterfaceDescriptor}.
	 * </p>
	 */
	public static final String EXTRACT_INTERFACE= "org.eclipse.wst.jsdt.ui.extract.interface"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Extract Local Variable' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.extract.temp</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.ExtractLocalDescriptor}.
	 * </p>
	 */
	public static final String EXTRACT_LOCAL_VARIABLE= "org.eclipse.wst.jsdt.ui.extract.temp"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Extract Method' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.extract.method</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.ExtractMethodDescriptor}.
	 * </p>
	 */
	public static final String EXTRACT_METHOD= "org.eclipse.wst.jsdt.ui.extract.method"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Extract Superclass' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.extract.superclass</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.ExtractSuperclassDescriptor}.
	 * </p>
	 */
	public static final String EXTRACT_SUPERCLASS= "org.eclipse.wst.jsdt.ui.extract.superclass"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Generalize Declared Type' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.change.type</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.GeneralizeTypeDescriptor}.
	 * </p>
	 */
	public static final String GENERALIZE_TYPE= "org.eclipse.wst.jsdt.ui.change.type"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Infer Type Arguments' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.infer.typearguments</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.InferTypeArgumentsDescriptor}.
	 * </p>
	 */
	public static final String INFER_TYPE_ARGUMENTS= "org.eclipse.wst.jsdt.ui.infer.typearguments"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Inline Constant' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.inline.constant</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.InlineConstantDescriptor}.
	 * </p>
	 */
	public static final String INLINE_CONSTANT= "org.eclipse.wst.jsdt.ui.inline.constant"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Inline Local Variable' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.inline.temp</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.InlineLocalVariableDescriptor}.
	 * </p>
	 */
	public static final String INLINE_LOCAL_VARIABLE= "org.eclipse.wst.jsdt.ui.inline.temp"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Inline Method' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.inline.method</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.InlineMethodDescriptor}.
	 * </p>
	 */
	public static final String INLINE_METHOD= "org.eclipse.wst.jsdt.ui.inline.method"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Introduce Factory' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.introduce.factory</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link Iorg.eclipse.wst.jsdt.core.refactoring.descriptors.ntroduceFactoryDescriptor}.
	 * </p>
	 */
	public static final String INTRODUCE_FACTORY= "org.eclipse.wst.jsdt.ui.introduce.factory"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Introduce Indirection' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.introduce.indirection</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.IntroduceIndirectionDescriptor}.
	 * </p>
	 */
	public static final String INTRODUCE_INDIRECTION= "org.eclipse.wst.jsdt.ui.introduce.indirection"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Introduce Parameter' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.introduce.parameter</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.IntroduceParameterDescriptor}.
	 * </p>
	 */
	public static final String INTRODUCE_PARAMETER= "org.eclipse.wst.jsdt.ui.introduce.parameter"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Move' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.move</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.MoveDescriptor}.
	 * </p>
	 */
	public static final String MOVE= "org.eclipse.wst.jsdt.ui.move"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Move Method' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.move.method</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.MoveMethodDescriptor}.
	 * </p>
	 */
	public static final String MOVE_METHOD= "org.eclipse.wst.jsdt.ui.move.method"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Move Static Members' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.move.static</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.MoveStaticMembersDescriptor}.
	 * </p>
	 */
	public static final String MOVE_STATIC_MEMBERS= "org.eclipse.wst.jsdt.ui.move.static"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Pull Up' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.pull.up</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.PullUpDescriptor}.
	 * </p>
	 */
	public static final String PULL_UP= "org.eclipse.wst.jsdt.ui.pull.up"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Push Down' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.push.down</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.PushDownDescriptor}.
	 * </p>
	 */
	public static final String PUSH_DOWN= "org.eclipse.wst.jsdt.ui.push.down"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Rename Compilation Unit' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.rename.compilationunit</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameJavaScriptElementDescriptor}.
	 * </p>
	 */
	public static final String RENAME_JAVASCRIPT_UNIT= "org.eclipse.wst.jsdt.ui.rename.compilationunit"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Rename Enum Constant' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.rename.enum.constant</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameJavaScriptElementDescriptor}.
	 * </p>
	 */
	public static final String RENAME_ENUM_CONSTANT= "org.eclipse.wst.jsdt.ui.rename.enum.constant"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Rename Field' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.rename.field</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameJavaScriptElementDescriptor}.
	 * </p>
	 */
	public static final String RENAME_FIELD= "org.eclipse.wst.jsdt.ui.rename.field"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Rename Java Project' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.rename.java.project</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameJavaScriptElementDescriptor}.
	 * </p>
	 */
	public static final String RENAME_JAVA_PROJECT= "org.eclipse.wst.jsdt.ui.rename.java.project"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Rename Local Variable' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.rename.local.variable</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameJavaScriptElementDescriptor}.
	 * </p>
	 */
	public static final String RENAME_LOCAL_VARIABLE= "org.eclipse.wst.jsdt.ui.rename.local.variable"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Rename Method' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.rename.method</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameJavaScriptElementDescriptor}.
	 * </p>
	 */
	public static final String RENAME_METHOD= "org.eclipse.wst.jsdt.ui.rename.method"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Rename Package' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.rename.package</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameJavaScriptElementDescriptor}.
	 * </p>
	 */
	public static final String RENAME_PACKAGE= "org.eclipse.wst.jsdt.ui.rename.package"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Rename Resource' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.rename.resource</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameResourceDescriptor}.
	 * </p>
	 */
	public static final String RENAME_RESOURCE= "org.eclipse.wst.jsdt.ui.rename.resource"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Rename Source Folder' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.rename.source.folder</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameJavaScriptElementDescriptor}.
	 * </p>
	 */
	public static final String RENAME_SOURCE_FOLDER= "org.eclipse.wst.jsdt.ui.rename.source.folder"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Rename Type' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.rename.type</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameJavaScriptElementDescriptor}.
	 * </p>
	 */
	public static final String RENAME_TYPE= "org.eclipse.wst.jsdt.ui.rename.type"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Rename Type Parameter' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.rename.type.parameter</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameJavaScriptElementDescriptor}.
	 * </p>
	 */
	public static final String RENAME_TYPE_PARAMETER= "org.eclipse.wst.jsdt.ui.rename.type.parameter"; //$NON-NLS-1$

	/**
	 * Refactoring id of the 'Use Supertype Where Possible' refactoring (value:
	 * <code>org.eclipse.wst.jsdt.ui.use.supertype</code>).
	 * <p>
	 * Clients may safely cast the obtained refactoring descriptor to
	 * {@link org.eclipse.wst.jsdt.core.refactoring.descriptors.UseSupertypeDescriptor}.
	 * </p>
	 */
	public static final String USE_SUPER_TYPE= "org.eclipse.wst.jsdt.ui.use.supertype"; //$NON-NLS-1$
}
