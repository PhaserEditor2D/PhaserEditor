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
package org.eclipse.wst.jsdt.ui.actions;

import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * Defines the definition IDs for the JavaScript editor actions.
 * 
 * <p>
 * This interface is not intended to be implemented or extended.
 * </p>.
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public interface IJavaEditorActionDefinitionIds extends ITextEditorActionDefinitionIds {

	// edit

	/**
	 * Action definition ID of the edit -> smart typing action
	 * (value <code>"org.eclipse.wst.jsdt.smartTyping.toggle"</code>).
	 *
	 * 
	 */
	public static final String TOGGLE_SMART_TYPING= "org.eclipse.wst.jsdt.smartTyping.toggle"; //$NON-NLS-1$

	/**
	 * Action definition ID of the edit -> go to matching bracket action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.goto.matching.bracket"</code>).
	 *
	 * 
	 */
	public static final String GOTO_MATCHING_BRACKET= "org.eclipse.wst.jsdt.ui.edit.text.java.goto.matching.bracket"; //$NON-NLS-1$

	/**
	 * Action definition ID of the edit -> go to next member action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.goto.next.member"</code>).
	 *
	 * 
	 */
	public static final String GOTO_NEXT_MEMBER= "org.eclipse.wst.jsdt.ui.edit.text.java.goto.next.member"; //$NON-NLS-1$

	/**
	 * Action definition ID of the edit -> go to previous member action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.goto.previous.member"</code>).
	 *
	 * 
	 */
	public static final String GOTO_PREVIOUS_MEMBER= "org.eclipse.wst.jsdt.ui.edit.text.java.goto.previous.member"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> select enclosing action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.select.enclosing"</code>).
	 */
	public static final String SELECT_ENCLOSING= "org.eclipse.wst.jsdt.ui.edit.text.java.select.enclosing"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> select next action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.select.next"</code>).
	 */
	public static final String SELECT_NEXT= "org.eclipse.wst.jsdt.ui.edit.text.java.select.next"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> select previous action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.select.previous"</code>).
	 */
	public static final String SELECT_PREVIOUS= "org.eclipse.wst.jsdt.ui.edit.text.java.select.previous"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the edit -> select restore last action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.select.last"</code>).
	 */
	public static final String SELECT_LAST= "org.eclipse.wst.jsdt.ui.edit.text.java.select.last"; //$NON-NLS-1$
	
		

	/**
	 * Action definition ID of the edit -> content assist complete prefix action
	 * (value: <code>"org.eclipse.wst.jsdt.ui.edit.text.java.complete.prefix"</code>).
	 * 
	 */
	public static final String CONTENT_ASSIST_COMPLETE_PREFIX= "org.eclipse.wst.jsdt.ui.edit.text.java.complete.prefix"; //$NON-NLS-1$

	/**
	 * Action definition ID of the navigate -> Show Outline action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.show.outline"</code>).
	 * 
	 * 
	 */
	public static final String SHOW_OUTLINE= "org.eclipse.wst.jsdt.ui.edit.text.java.show.outline"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the navigate -> Show Hierarchy action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.open.hierarchy"</code>).
	 * 
	 * 
	 */
	public static final String OPEN_HIERARCHY= "org.eclipse.wst.jsdt.ui.edit.text.java.open.hierarchy"; //$NON-NLS-1$

	
	/**
	 * Action definition ID of the Navigate -> Open Structure action
	 * (value <code>"org.eclipse.wst.jsdt.ui.navigate.java.open.structure"</code>).
	 * 
	 * 
	 */
	public static final String OPEN_STRUCTURE= "org.eclipse.wst.jsdt.ui.navigate.java.open.structure"; //$NON-NLS-1$

	// source
	
	/**
	 * Action definition ID of the source -> comment action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.comment"</code>).
	 */
	public static final String COMMENT= "org.eclipse.wst.jsdt.ui.edit.text.java.comment"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> uncomment action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.uncomment"</code>).
	 */
	public static final String UNCOMMENT= "org.eclipse.wst.jsdt.ui.edit.text.java.uncomment"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> toggle comment action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.toggle.comment"</code>).
	 * 
	 */
	public static final String TOGGLE_COMMENT= "org.eclipse.wst.jsdt.ui.edit.text.java.toggle.comment"; //$NON-NLS-1$
	

	/**
	 * Action definition ID of the source -> add block comment action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.add.block.comment"</code>).
	 * 
	 */
	public static final String ADD_BLOCK_COMMENT= "org.eclipse.wst.jsdt.ui.edit.text.java.add.block.comment"; //$NON-NLS-1$

	/**
	 * Action definition ID of the source -> remove block comment action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.remove.block.comment"</code>).
	 * 
	 */
	public static final String REMOVE_BLOCK_COMMENT= "org.eclipse.wst.jsdt.ui.edit.text.java.remove.block.comment"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> indent action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.indent"</code>).
	 */
	public static final String INDENT= "org.eclipse.wst.jsdt.ui.edit.text.java.indent"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> format action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.format"</code>).
	 */
	public static final String FORMAT= "org.eclipse.wst.jsdt.ui.edit.text.java.format"; //$NON-NLS-1$
	
	/**
	 * Action definition id of the JavaScript quick format action
	 * (value: <code>"org.eclipse.wst.jsdt.ui.edit.text.java.quick.format"</code>).
	 * 
	 */
	public static final String QUICK_FORMAT= "org.eclipse.wst.jsdt.ui.edit.text.java.quick.format"; //$NON-NLS-1$

	/**
	 * Action definition ID of the source -> add import action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.add.import"</code>).
	 */
	public static final String ADD_IMPORT= "org.eclipse.wst.jsdt.ui.edit.text.java.add.import"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> organize imports action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.organize.imports"</code>).
	 */
	public static final String ORGANIZE_IMPORTS= "org.eclipse.wst.jsdt.ui.edit.text.java.organize.imports"; //$NON-NLS-1$

	/**
	 * Action definition ID of the source -> sort order action (value
	 * <code>"org.eclipse.wst.jsdt.ui.edit.text.java.sort.members"</code>).
	 * 
	 */
	public static final String SORT_MEMBERS= "org.eclipse.wst.jsdt.ui.edit.text.java.sort.members"; //$NON-NLS-1$

	/**
	 * Action definition ID of the source -> add javadoc comment action (value
	 * <code>"org.eclipse.wst.jsdt.ui.edit.text.java.add.javadoc.comment"</code>).
	 * 
	 */		
	public static final String ADD_JAVADOC_COMMENT= "org.eclipse.wst.jsdt.ui.edit.text.java.add.javadoc.comment"; //$NON-NLS-1$

	/**
	 * Action definition ID of the source -> surround with try/catch action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.surround.with.try.catch"</code>).
	 */
	public static final String SURROUND_WITH_TRY_CATCH= "org.eclipse.wst.jsdt.ui.edit.text.java.surround.with.try.catch"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> override methods action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.override.methods"</code>).
	 */
	public static final String OVERRIDE_METHODS= "org.eclipse.wst.jsdt.ui.edit.text.java.override.methods"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> add unimplemented constructors action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.add.unimplemented.constructors"</code>).
	 */
	public static final String ADD_UNIMPLEMENTED_CONTRUCTORS= "org.eclipse.wst.jsdt.ui.edit.text.java.add.unimplemented.constructors"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source ->generate constructor using fields action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.generate.constructor.using.fields"</code>).
	 */
	public static final String GENERATE_CONSTRUCTOR_USING_FIELDS= "org.eclipse.wst.jsdt.ui.edit.text.java.generate.constructor.using.fields"; //$NON-NLS-1$

	/**
	 * Action definition ID of the source ->generate hashcode() and equals() action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.generate.hashcode.equals"</code>).
	 * 
	 */
	public static final String GENERATE_HASHCODE_EQUALS= "org.eclipse.wst.jsdt.ui.edit.text.java.generate.hashcode.equals"; //$NON-NLS-1$

	/**
	 * Action definition ID of the source -> generate setter/getter action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.create.getter.setter"</code>).
	 */
	public static final String CREATE_GETTER_SETTER= "org.eclipse.wst.jsdt.ui.edit.text.java.create.getter.setter"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the source -> generate delegates action (value
	 * <code>"org.eclipse.wst.jsdt.ui.edit.text.java.create.delegate.methods"</code>).
	 * 
	 */
	public static final String CREATE_DELEGATE_METHODS= "org.eclipse.wst.jsdt.ui.edit.text.java.create.delegate.methods"; //$NON-NLS-1$
	
	
	/**
	 * Action definition ID of the source -> externalize strings action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.externalize.strings"</code>).
	 */
	public static final String EXTERNALIZE_STRINGS= "org.eclipse.wst.jsdt.ui.edit.text.java.externalize.strings"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> pull up action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.pull.up"</code>).
	 */
	public static final String PULL_UP= "org.eclipse.wst.jsdt.ui.edit.text.java.pull.up"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> push down action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.push.down"</code>).
	 * 
	 * 
	 */
	public static final String PUSH_DOWN= "org.eclipse.wst.jsdt.ui.edit.text.java.push.down"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> rename element action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.rename.element"</code>).
	 */
	public static final String RENAME_ELEMENT= "org.eclipse.wst.jsdt.ui.edit.text.java.rename.element"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> modify method parameters action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.modify.method.parameters"</code>).
	 */
	public static final String MODIFY_METHOD_PARAMETERS= "org.eclipse.wst.jsdt.ui.edit.text.java.modify.method.parameters"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> move element action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.move.element"</code>).
	 */
	public static final String MOVE_ELEMENT= "org.eclipse.wst.jsdt.ui.edit.text.java.move.element"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> extract local variable action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.extract.local.variable"</code>).
	 */
	public static final String EXTRACT_LOCAL_VARIABLE= "org.eclipse.wst.jsdt.ui.edit.text.java.extract.local.variable"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> extract constant action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.extract.constant"</code>).
	 * 
	 * 
	 */
	public static final String EXTRACT_CONSTANT= "org.eclipse.wst.jsdt.ui.edit.text.java.extract.constant"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> introduce parameter action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.introduce.parameter"</code>).
	 * 
	 * 
	 */
	public static final String INTRODUCE_PARAMETER= "org.eclipse.wst.jsdt.ui.edit.text.java.introduce.parameter"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> introduce factory action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.introduce.factory"</code>).
	 * 
	 * 
	 */
	public static final String INTRODUCE_FACTORY= "org.eclipse.wst.jsdt.ui.edit.text.java.introduce.factory"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> self encapsulate field action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.self.encapsulate.field"</code>).
	 */
	public static final String SELF_ENCAPSULATE_FIELD= "org.eclipse.wst.jsdt.ui.edit.text.java.self.encapsulate.field"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> extract method action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.extract.method"</code>).
	 */
	public static final String EXTRACT_METHOD= "org.eclipse.wst.jsdt.ui.edit.text.java.extract.method"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> inline action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.inline"</code>).
	 *
	 * 
	 */
	public static final String INLINE= "org.eclipse.wst.jsdt.ui.edit.text.java.inline"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> replace invocations action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.replace.invocations"</code>).
	 *
	 * 
	 */
	public static final String REPLACE_INVOCATIONS= "org.eclipse.wst.jsdt.ui.edit.text.java.replace.invocations"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the refactor -> introduce indirection action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.create.indirection"</code>).
	 *
	 * 
	 */
	public static final String INTRODUCE_INDIRECTION= "org.eclipse.wst.jsdt.ui.edit.text.java.introduce.indirection"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> extract interface action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.extract.interface"</code>).
	 * 
	 * 
	 */
	public static final String EXTRACT_INTERFACE= "org.eclipse.wst.jsdt.ui.edit.text.java.extract.interface"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> change type action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.change.type"</code>).
	 * 
	 * 
	 */
	public static final String CHANGE_TYPE= "org.eclipse.wst.jsdt.ui.edit.text.java.change.type"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> move inner type to top level action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.move.inner.to.top.level"</code>).
	 * 
	 * 
	 */
	public static final String MOVE_INNER_TO_TOP= "org.eclipse.wst.jsdt.ui.edit.text.java.move.inner.to.top.level"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> use supertype action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.use.supertype"</code>).
	 * 
	 * 
	 */
	public static final String USE_SUPERTYPE= "org.eclipse.wst.jsdt.ui.edit.text.java.use.supertype"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> infer generic type arguments action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.infer.type.arguments"</code>).
	 * 
	 * 
	 */
	public static final String INFER_TYPE_ARGUMENTS_ACTION= "org.eclipse.wst.jsdt.ui.edit.text.java.infer.type.arguments"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> promote local variable action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.promote.local.variable"</code>).
	 * 
	 * 
	 */
	public static final String PROMOTE_LOCAL_VARIABLE= "org.eclipse.wst.jsdt.ui.edit.text.java.promote.local.variable"; //$NON-NLS-1$

	/**
	 * Action definition ID of the refactor -> convert anonymous to nested action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.convert.anonymous.to.nested"</code>).
	 * 
	 * 
	 */
	public static final String CONVERT_ANONYMOUS_TO_NESTED= "org.eclipse.wst.jsdt.ui.edit.text.java.convert.anonymous.to.nested"; //$NON-NLS-1$

	// navigate
	
	/**
	 * Action definition ID of the navigate -> open action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.open.editor"</code>).
	 */
	public static final String OPEN_EDITOR= "org.eclipse.wst.jsdt.ui.edit.text.java.open.editor"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the navigate -> open super implementation action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.open.super.implementation"</code>).
	 */
	public static final String OPEN_SUPER_IMPLEMENTATION= "org.eclipse.wst.jsdt.ui.edit.text.java.open.super.implementation"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the navigate -> open external javadoc action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.open.external.javadoc"</code>).
	 */
	public static final String OPEN_EXTERNAL_JAVADOC= "org.eclipse.wst.jsdt.ui.edit.text.java.open.external.javadoc"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the navigate -> open type hierarchy action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.org.eclipse.wst.jsdt.ui.edit.text.java.open.type.hierarchy"</code>).
	 */
	public static final String OPEN_TYPE_HIERARCHY= "org.eclipse.wst.jsdt.ui.edit.text.java.open.type.hierarchy"; //$NON-NLS-1$
	
    /**
     * Action definition ID of the navigate -> open call hierarchy action
     * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.org.eclipse.wst.jsdt.ui.edit.text.java.open.call.hierarchy"</code>).
     * 
     */
    public static final String OPEN_CALL_HIERARCHY= "org.eclipse.wst.jsdt.ui.edit.text.java.open.call.hierarchy"; //$NON-NLS-1$
    
	/**
	 * Action definition ID of the navigate -> show in package explorer action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.show.in.package.view"</code>).
	 */
	public static final String SHOW_IN_PACKAGE_VIEW= "org.eclipse.wst.jsdt.ui.edit.text.java.show.in.package.view"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the navigate -> show in navigator action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.show.in.navigator.view"</code>).
	 */
	public static final String SHOW_IN_NAVIGATOR_VIEW= "org.eclipse.wst.jsdt.ui.edit.text.java.show.in.navigator.view"; //$NON-NLS-1$

	// search
	
	/**
	 * Action definition ID of the search -> references in workspace action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.references.in.workspace"</code>).
	 */
	public static final String SEARCH_REFERENCES_IN_WORKSPACE= "org.eclipse.wst.jsdt.ui.edit.text.java.search.references.in.workspace"; //$NON-NLS-1$

	/**
	 * Action definition ID of the search -> references in project action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.references.in.project"</code>).
	 */
	public static final String SEARCH_REFERENCES_IN_PROJECT= "org.eclipse.wst.jsdt.ui.edit.text.java.search.references.in.project"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> references in hierarchy action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.references.in.hierarchy"</code>).
	 */
	public static final String SEARCH_REFERENCES_IN_HIERARCHY= "org.eclipse.wst.jsdt.ui.edit.text.java.search.references.in.hierarchy"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> references in working set action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.references.in.working.set"</code>).
	 */
	public static final String SEARCH_REFERENCES_IN_WORKING_SET= "org.eclipse.wst.jsdt.ui.edit.text.java.search.references.in.working.set"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> read access in workspace action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.read.access.in.workspace"</code>).
	 */
	public static final String SEARCH_READ_ACCESS_IN_WORKSPACE= "org.eclipse.wst.jsdt.ui.edit.text.java.search.read.access.in.workspace"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> read access in project action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.read.access.in.project"</code>).
	 */
	public static final String SEARCH_READ_ACCESS_IN_PROJECT= "org.eclipse.wst.jsdt.ui.edit.text.java.search.read.access.in.project"; //$NON-NLS-1$

	/**
	 * Action definition ID of the search -> read access in hierarchy action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.read.access.in.hierarchy"</code>).
	 */
	public static final String SEARCH_READ_ACCESS_IN_HIERARCHY= "org.eclipse.wst.jsdt.ui.edit.text.java.search.read.access.in.hierarchy"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> read access in working set action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.read.access.in.working.set"</code>).
	 */
	public static final String SEARCH_READ_ACCESS_IN_WORKING_SET= "org.eclipse.wst.jsdt.ui.edit.text.java.search.read.access.in.working.set"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> write access in workspace action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.write.access.in.workspace"</code>).
	 */
	public static final String SEARCH_WRITE_ACCESS_IN_WORKSPACE= "org.eclipse.wst.jsdt.ui.edit.text.java.search.write.access.in.workspace"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> write access in project action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.write.access.in.project"</code>).
	 */
	public static final String SEARCH_WRITE_ACCESS_IN_PROJECT= "org.eclipse.wst.jsdt.ui.edit.text.java.search.write.access.in.project"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> write access in hierarchy action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.write.access.in.hierarchy"</code>).
	 */
	public static final String SEARCH_WRITE_ACCESS_IN_HIERARCHY= "org.eclipse.wst.jsdt.ui.edit.text.java.search.write.access.in.hierarchy"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> write access in working set action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.write.access.in.working.set"</code>).
	 */
	public static final String SEARCH_WRITE_ACCESS_IN_WORKING_SET= "org.eclipse.wst.jsdt.ui.edit.text.java.search.write.access.in.working.set"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> declarations in workspace action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.declarations.in.workspace"</code>).
	 */
	public static final String SEARCH_DECLARATIONS_IN_WORKSPACE= "org.eclipse.wst.jsdt.ui.edit.text.java.search.declarations.in.workspace"; //$NON-NLS-1$
	/**
	 * Action definition ID of the search -> declarations in project action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.declarations.in.project"</code>).
	 */
	public static final String SEARCH_DECLARATIONS_IN_PROJECTS= "org.eclipse.wst.jsdt.ui.edit.text.java.search.declarations.in.project"; //$NON-NLS-1$
	/**
	 * Action definition ID of the search -> declarations in hierarchy action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.declarations.in.hierarchy"</code>).
	 */
	public static final String SEARCH_DECLARATIONS_IN_HIERARCHY= "org.eclipse.wst.jsdt.ui.edit.text.java.search.declarations.in.hierarchy"; //$NON-NLS-1$
	/**
	 * Action definition ID of the search -> declarations in working set action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.declarations.in.working.set"</code>).
	 */
	public static final String SEARCH_DECLARATIONS_IN_WORKING_SET= "org.eclipse.wst.jsdt.ui.edit.text.java.search.declarations.in.working.set"; //$NON-NLS-1$
	/**
	 * Action definition ID of the search -> implementors in workspace action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.implementors.in.workspace"</code>).
	 */
	public static final String SEARCH_IMPLEMENTORS_IN_WORKSPACE= "org.eclipse.wst.jsdt.ui.edit.text.java.search.implementors.in.workspace"; //$NON-NLS-1$
	/**
	 * Action definition ID of the search -> implementors in working set action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.implementors.in.working.set"</code>).
	 */
	public static final String SEARCH_IMPLEMENTORS_IN_WORKING_SET= "org.eclipse.wst.jsdt.ui.edit.text.java.search.implementors.in.working.set"; //$NON-NLS-1$

	/**
	 * Action definition ID of the search -> implementors in project action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.implementors.in.project"</code>).
	 * 
	 */
	public static final String SEARCH_IMPLEMENTORS_IN_PROJECT= "org.eclipse.wst.jsdt.ui.edit.text.java.search.implementors.in.project"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> occurrences in file quick menu action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.occurrences.in.file.quickMenu"</code>).
	 * 
	 */
	public static final String SEARCH_OCCURRENCES_IN_FILE_QUICK_MENU= "org.eclipse.wst.jsdt.ui.edit.text.java.search.occurrences.in.file.quickMenu"; //$NON-NLS-1$
	
	/**
	 * Action definition ID of the search -> occurrences in file > elements action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.occurrences.in.file"</code>).
	 * 
	 */
	public static final String SEARCH_OCCURRENCES_IN_FILE= "org.eclipse.wst.jsdt.ui.edit.text.java.search.occurrences.in.file"; //$NON-NLS-1$

	/**
	 * Action definition ID of the search -> occurrences in file > exceptions action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.exception.occurrences"</code>).
	 * 
	 */
	public static final String SEARCH_EXCEPTION_OCCURRENCES_IN_FILE= "org.eclipse.wst.jsdt.ui.edit.text.java.search.exception.occurrences"; //$NON-NLS-1$

	/**
	 * Action definition ID of the search -> occurrences in file > implements action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.search.implement.occurrences"</code>).
	 * 
	 */
	public static final String SEARCH_IMPLEMENT_OCCURRENCES_IN_FILE= "org.eclipse.wst.jsdt.ui.edit.text.java.search.implement.occurrences"; //$NON-NLS-1$

	// miscellaneous
	
	/**
	 * Action definition ID of the toggle text hover tool bar button action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.java.toggle.text.hover"</code>).
	 */
	public static final String TOGGLE_TEXT_HOVER= "org.eclipse.wst.jsdt.ui.edit.text.java.toggle.text.hover"; //$NON-NLS-1$

	/**
	 * Action definition ID of the remove occurrence annotations action
	 * (value <code>"org.eclipse.wst.jsdt.ui.edit.text.remove.occurrence.annotations"</code>).
	 * 
	 */
	public static final String REMOVE_OCCURRENCE_ANNOTATIONS= "org.eclipse.wst.jsdt.ui.edit.text.remove.occurrence.annotations";  //$NON-NLS-1$

	/**
	 * Action definition id of toggle mark occurrences action
	 * (value: <code>"org.eclipse.wst.jsdt.ui.edit.text.java.toggleMarkOccurrences"</code>).
	 * 
	 */
	public static final String TOGGLE_MARK_OCCURRENCES= "org.eclipse.wst.jsdt.ui.edit.text.java.toggleMarkOccurrences"; //$NON-NLS-1$

	/**
	 * Action definition id of the collapse members action
	 * (value: <code>"org.eclipse.wst.jsdt.ui.edit.text.java.folding.collapseMembers"</code>).
	 * 
	 */
	public static final String FOLDING_COLLAPSE_MEMBERS= "org.eclipse.wst.jsdt.ui.edit.text.java.folding.collapseMembers"; //$NON-NLS-1$

	/**
	 * Action definition id of the collapse comments action
	 * (value: <code>"org.eclipse.wst.jsdt.ui.edit.text.java.folding.collapseComments"</code>).
	 * 
	 */
	public static final String FOLDING_COLLAPSE_COMMENTS= "org.eclipse.wst.jsdt.ui.edit.text.java.folding.collapseComments"; //$NON-NLS-1$

	/**
	 * Action definition id of the code clean up action
	 * (value: <code>"org.eclipse.wst.jsdt.ui.edit.text.java.clean.up"</code>).
	 * 
	 */
	public static final String CLEAN_UP= "org.eclipse.wst.jsdt.ui.edit.text.java.clean.up"; //$NON-NLS-1$
}
