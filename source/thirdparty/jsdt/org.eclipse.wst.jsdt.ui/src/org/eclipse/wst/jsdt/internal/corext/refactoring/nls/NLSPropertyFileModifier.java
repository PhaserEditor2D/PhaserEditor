/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.nls;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;


public class NLSPropertyFileModifier {

	public static Change create(NLSSubstitution[] nlsSubstitutions, IPath propertyFilePath) throws CoreException {

		String name= Messages.format(NLSMessages.NLSPropertyFileModifier_change_name, propertyFilePath.toString()); 
		TextChange textChange= null;
		if (!Checks.resourceExists(propertyFilePath)) {
			IProject project= getFileHandle(propertyFilePath).getProject();
			String lineDelimiter= StubUtility.getLineDelimiterPreference(project);
			Document document= new Document();
			document.setInitialLineDelimiter(lineDelimiter);
			textChange= new DocumentChange(name, document);
			addChanges(textChange, nlsSubstitutions);
			textChange.perform(new NullProgressMonitor());
			
			String encoding= null;
			IContentType javaPropertiesContentType= Platform.getContentTypeManager().getContentType("org.eclipse.wst.jsdt.core.javaProperties"); //$NON-NLS-1$
			IContentType[] contentTypes= Platform.getContentTypeManager().findContentTypesFor(propertyFilePath.lastSegment());
			if (contentTypes.length == 0 || contentTypes.length > 1 || !contentTypes[0].equals(javaPropertiesContentType)) {
				if (javaPropertiesContentType != null)
					encoding= javaPropertiesContentType.getDefaultCharset();
				if (encoding == null)
					encoding= "ISO-8859-1"; //$NON-NLS-1$
			}
			
			return new CreateTextFileChange(propertyFilePath, textChange.getCurrentContent(new NullProgressMonitor()), encoding, "properties"); //$NON-NLS-1$
		}

		textChange= new TextFileChange(name, getPropertyFile(propertyFilePath));
		textChange.setTextType("properties"); //$NON-NLS-1$

		addChanges(textChange, nlsSubstitutions);

		return textChange;
	}

	private static IFile getPropertyFile(IPath propertyFilePath) {
		return (IFile) ResourcesPlugin.getWorkspace().getRoot().findMember(propertyFilePath);
	}
	
	private static IFile getFileHandle(IPath propertyFilePath) {
		if (propertyFilePath == null)
			return null;
		return ResourcesPlugin.getWorkspace().getRoot().getFile(propertyFilePath);
	}

	private static void addChanges(TextChange textChange, NLSSubstitution[] substitutions) throws CoreException {
		PropertyFileDocumentModel model= new PropertyFileDocumentModel(textChange.getCurrentDocument(new NullProgressMonitor()));

		Map newKeyToSubstMap= getNewKeyToSubstitutionMap(substitutions);
		Map oldKeyToSubstMap= getOldKeyToSubstitutionMap(substitutions);

		addInsertEdits(textChange, substitutions, newKeyToSubstMap, oldKeyToSubstMap, model);
		addRemoveEdits(textChange, substitutions, newKeyToSubstMap, oldKeyToSubstMap, model);
		addReplaceEdits(textChange, substitutions, newKeyToSubstMap, oldKeyToSubstMap, model);
	}

	/**
	 * Maps the new keys to a substitutions. If a substitution is not in the map then it is a duplicate.
	 */
	static HashMap getNewKeyToSubstitutionMap(NLSSubstitution[] substitutions) {
		HashMap keyToSubstMap= new HashMap(substitutions.length);
		// find all duplicates
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution curr= substitutions[i];
			if (curr.getState() == NLSSubstitution.EXTERNALIZED) {
				NLSSubstitution val= (NLSSubstitution) keyToSubstMap.get(curr.getKey());
				if (val == null || (val.hasPropertyFileChange() && !curr.hasPropertyFileChange())) {
					keyToSubstMap.put(curr.getKey(), curr); // store if first or if stored in new and we are existing
				}
			}
		}
		return keyToSubstMap;
	}
	
	/**
	 * Maps the old keys to a substitutions. If a substitution is not in the map then it is a duplicate.
	 */
	static HashMap getOldKeyToSubstitutionMap(NLSSubstitution[] substitutions) {
		HashMap keyToSubstMap= new HashMap(substitutions.length);
		// find all duplicates
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution curr= substitutions[i];
			if (curr.getInitialState() == NLSSubstitution.EXTERNALIZED) {
				String key= curr.getInitialKey();
				if (key != null) {
					NLSSubstitution fav= (NLSSubstitution) keyToSubstMap.get(key);
					if (fav == null || (fav.hasStateChanged() && !curr.hasStateChanged())) {
						keyToSubstMap.put(key, curr); // store if first or if stored will not be externalized anymore
					}
				}
			}
		}
		return keyToSubstMap;
	}

	static boolean doReplace(NLSSubstitution substitution, Map newKeyToSubstMap, Map oldKeyToSubstMap) {
		if (substitution.getState() != NLSSubstitution.EXTERNALIZED || substitution.hasStateChanged() || substitution.getInitialValue() == null) {
			return false; // was not in property file before
		}
		if (oldKeyToSubstMap.get(substitution.getInitialKey()) != substitution) {
			return false; // not the owner of this key
		}
		if (substitution.isKeyRename() || substitution.isValueRename()) {
			if (newKeyToSubstMap.get(substitution.getKey()) == substitution) { // only rename if we're not a duplicate. duplicates will be removed
				return true;
			}
		}
		return false;
	}
	
	private static void addReplaceEdits(TextChange textChange, NLSSubstitution[] substitutions, Map newKeyToSubstMap, Map oldKeyToSubstMap, PropertyFileDocumentModel model) {
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution substitution= substitutions[i];
			if (doReplace(substitution, newKeyToSubstMap, oldKeyToSubstMap)) {
				KeyValuePair initialPair= new KeyValuePair(substitution.getInitialKey(), substitution.getInitialValue());
				KeyValuePair newPair= new KeyValuePair(substitution.getKey(), substitution.getValueNonEmpty());
				TextEdit edit= model.replace(initialPair, newPair);
				if (edit != null) {
					TextChangeCompatibility.addTextEdit(textChange, Messages.format(NLSMessages.NLSPropertyFileModifier_replace_entry, substitution.getKey()), edit); 
				}
			}
		}
	}
	
	static boolean doInsert(NLSSubstitution substitution, Map newKeyToSubstMap, Map oldKeyToSubstMap) {
		if (substitution.getState() != NLSSubstitution.EXTERNALIZED) {
			return false; // does not go into the property file
		}
		if (!substitution.hasStateChanged() && substitution.getInitialValue() != null) {
			if (!substitution.isKeyRename() || oldKeyToSubstMap.get(substitution.getInitialKey()) == substitution) {
				return false; // no key rename and was not a duplicate
			}
		}
		if (newKeyToSubstMap.get(substitution.getKey()) == substitution) { // only insert if we're not a duplicate
			return true;
		}
		return false;
	}

	private static void addInsertEdits(TextChange textChange, NLSSubstitution[] substitutions, Map newKeyToSubstMap, Map oldKeyToSubstMap, PropertyFileDocumentModel model) {
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution substitution= substitutions[i];
			if (doInsert(substitution, newKeyToSubstMap, oldKeyToSubstMap)) {
				String value= substitution.getValueNonEmpty();
				KeyValuePair curr= new KeyValuePair(substitution.getKey(), value);
				
				InsertEdit insert= model.insert(curr);
				String message= Messages.format(NLSMessages.NLSPropertyFileModifier_add_entry, curr.getKey()); 
				TextChangeCompatibility.addTextEdit(textChange, message, insert);
			}
		}
	}
	
	static boolean doRemove(NLSSubstitution substitution, Map newKeyToSubstMap, Map oldKeyToSubstMap) {
		if (substitution.getInitialState() != NLSSubstitution.EXTERNALIZED || substitution.getInitialKey() == null) {
			return false; // was not in property file before
		}
		if (oldKeyToSubstMap.get(substitution.getInitialKey()) != substitution) {
			return false; // not the owner of this key
		}
		if (substitution.hasStateChanged()) {
			return true; // was externalized, but not anymore
		} else {
			if (substitution.hasPropertyFileChange() && newKeyToSubstMap.get(substitution.getKey()) != substitution) {
				return true; // has been changed to an already existing
			}
		}
		return false;
	}
	
	private static void addRemoveEdits(TextChange textChange, NLSSubstitution[] substitutions, Map newKeyToSubstMap, Map oldKeyToSubstMap, PropertyFileDocumentModel model) {
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution substitution= substitutions[i];
			if (doRemove(substitution, newKeyToSubstMap, oldKeyToSubstMap)) {
				TextEdit edit= model.remove(substitution.getInitialKey());
				if (edit != null) {
					TextChangeCompatibility.addTextEdit(textChange, Messages.format(NLSMessages.NLSPropertyFileModifier_remove_entry, substitution.getInitialKey()), edit); 
				}
			}
		}
	}

}
