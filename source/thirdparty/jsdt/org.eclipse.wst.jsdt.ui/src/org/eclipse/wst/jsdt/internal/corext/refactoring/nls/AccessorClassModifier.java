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
package org.eclipse.wst.jsdt.internal.corext.refactoring.nls;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaStatusConstants;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

import com.ibm.icu.text.Collator;

public class AccessorClassModifier {

	private JavaScriptUnit fRoot;
	private AST fAst;
	private ASTRewrite fASTRewrite;
	private ListRewrite fListRewrite;
	private IJavaScriptUnit fCU;
	private List fFields;

	private AccessorClassModifier(IJavaScriptUnit cu) throws CoreException {

		fCU= cu;
		
		fRoot= JavaScriptPlugin.getDefault().getASTProvider().getAST(cu, ASTProvider.WAIT_YES, null);
		fAst= fRoot.getAST();
		fASTRewrite= ASTRewrite.create(fAst);
		
		AbstractTypeDeclaration parent= null;
		if (fRoot.types().size() > 0) {
			parent= (AbstractTypeDeclaration)fRoot.types().get(0);
			fFields= new ArrayList();
			parent.accept(new GenericVisitor() {
				/**
				 * {@inheritDoc}
				 */
				public boolean visit(FieldDeclaration node) {
					int modifiers= node.getModifiers();
					if (!Modifier.isPublic(modifiers))
						return false;
					
					if (!Modifier.isStatic(modifiers))
						return false;
					
					List fragments= node.fragments();
					if (fragments.size() != 1)
						return false;
					
					VariableDeclarationFragment fragment= (VariableDeclarationFragment)fragments.get(0);
					if (fragment.getInitializer() != null)
						return false;
					
					fFields.add(node);
					return false;
				}
			});
			fListRewrite= fASTRewrite.getListRewrite(parent, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		} else {
			IStatus status= new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, IJavaStatusConstants.INTERNAL_ERROR, NLSMessages.AccessorClassModifier_missingType, null); 
			throw new CoreException(status);
		}
	}
	
	private TextEdit getTextEdit() throws CoreException {
		IDocument document= null;
		
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		IPath path= fCU.getPath();
		
		if (manager != null && path != null) {
			manager.connect(path, LocationKind.NORMALIZE, null);
			try {
				ITextFileBuffer buffer= manager.getTextFileBuffer(path, LocationKind.NORMALIZE);
				if (buffer != null)
					document= buffer.getDocument();
			} finally {
				manager.disconnect(path, LocationKind.NORMALIZE, null);
			}
		}
		
		if (document == null)
			document= new Document(fCU.getSource());
		 
		return fASTRewrite.rewriteAST(document, fCU.getJavaScriptProject().getOptions(true));
	}

	public static Change create(IJavaScriptUnit cu, NLSSubstitution[] substitutions) throws CoreException {
		
		Map newKeyToSubstMap= NLSPropertyFileModifier.getNewKeyToSubstitutionMap(substitutions);
		Map oldKeyToSubstMap= NLSPropertyFileModifier.getOldKeyToSubstitutionMap(substitutions);

		AccessorClassModifier sourceModification= new AccessorClassModifier(cu);

		String message= Messages.format(NLSMessages.NLSSourceModifier_change_description, cu.getElementName()); 

		TextChange change= new CompilationUnitChange(message, cu);
		MultiTextEdit multiTextEdit= new MultiTextEdit();
		change.setEdit(multiTextEdit);
		
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution substitution= substitutions[i];
			if (NLSPropertyFileModifier.doRemove(substitution, newKeyToSubstMap, oldKeyToSubstMap)) {
				sourceModification.removeKey(substitution, change);
			}
		}
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution substitution= substitutions[i];
			if (substitution.isKeyRename() && NLSPropertyFileModifier.doReplace(substitution, newKeyToSubstMap, oldKeyToSubstMap)) {
				sourceModification.renameKey(substitution, change);
			}
		}
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution substitution= substitutions[i];
			if (NLSPropertyFileModifier.doInsert(substitution, newKeyToSubstMap, oldKeyToSubstMap)) {
				sourceModification.addKey(substitution, change);
			}
		}
		
		if (change.getChangeGroups().length == 0)
			return null;
		
		change.addEdit(sourceModification.getTextEdit());
		
		return change;
	}
	
	private void removeKey(NLSSubstitution sub, TextChange change) throws CoreException {
		ASTNode node= findField(fRoot, sub.getKey());
		if (node == null)
			return;
		
		String name= Messages.format(NLSMessages.AccessorClassModifier_remove_entry, sub.getKey()); 
		TextEditGroup editGroup= new TextEditGroup(name);
		fListRewrite.remove(node, editGroup);
		change.addTextEditGroup(editGroup);
		fFields.remove(node);
	}
	
	private void renameKey(NLSSubstitution sub, TextChange change) throws CoreException {
		ASTNode node= findField(fRoot, sub.getInitialKey());
		if (node == null)
			return;
		
		String name= Messages.format(NLSMessages.AccessorClassModifier_replace_entry, sub.getKey()); 
		TextEditGroup editGroup= new TextEditGroup(name);
		fListRewrite.remove(node, editGroup);
		fFields.remove(node);
		
		addKey(sub, change, editGroup);
		
		change.addTextEditGroup(editGroup);
	}
	
	private ASTNode findField(ASTNode astRoot, final String name) {
		
		class STOP_VISITING extends RuntimeException {
			private static final long serialVersionUID= 1L;
		}
		
		final ASTNode[] result= new ASTNode[1];
		
		try {
			astRoot.accept(new ASTVisitor() {
				
				public boolean visit(VariableDeclarationFragment node) {
					if (name.equals(node.getName().getFullyQualifiedName())) {
						result[0]= node.getParent();
						throw new STOP_VISITING();
					}
					return true;	
				}
			});
		} catch (STOP_VISITING ex) {
			// stop visiting AST
		}
		
		return result[0];
	}
	
	private void addKey(NLSSubstitution sub, TextChange change) throws CoreException {		
		String name= Messages.format(NLSMessages.AccessorClassModifier_add_entry, sub.getKey()); 
		TextEditGroup editGroup= new TextEditGroup(name);
		change.addTextEditGroup(editGroup);
		addKey(sub, change, editGroup);
	}
		
	private void addKey(NLSSubstitution sub, TextChange change, TextEditGroup editGroup) throws CoreException {	
		
		if (fListRewrite == null)
			return;
		
		String key= sub.getKey();
		FieldDeclaration fieldDeclaration= getNewFinalStringFieldDeclaration(key);

		Iterator iter= fFields.iterator();
		int insertionPosition= 0;
		if (iter.hasNext()) {
			Collator collator= Collator.getInstance();
			FieldDeclaration existingFieldDecl= (FieldDeclaration)iter.next();
			VariableDeclarationFragment fragment= (VariableDeclarationFragment)existingFieldDecl.fragments().get(0);
			String identifier= fragment.getName().getIdentifier();
			if (collator.compare(key, identifier) != 1) {
				insertionPosition= 0;
				fListRewrite.insertBefore(fieldDeclaration, existingFieldDecl, editGroup);
			} else {
				insertionPosition++;
				while (iter.hasNext()) {
					FieldDeclaration next= (FieldDeclaration)iter.next();
					fragment= (VariableDeclarationFragment)next.fragments().get(0);
					identifier= fragment.getName().getIdentifier();
					if (collator.compare(key, identifier) == -1) {
						break;
					}
					insertionPosition++;
					existingFieldDecl= next;
				}
				fListRewrite.insertAfter(fieldDeclaration, existingFieldDecl, editGroup);
			}
		} else {
			insertionPosition= 0;
			fListRewrite.insertLast(fieldDeclaration, editGroup);
		}
		fFields.add(insertionPosition, fieldDeclaration);
	}

	private FieldDeclaration getNewFinalStringFieldDeclaration(String name) {
		VariableDeclarationFragment variableDeclarationFragment= fAst.newVariableDeclarationFragment();
		variableDeclarationFragment.setName(fAst.newSimpleName(name));
		
		FieldDeclaration fieldDeclaration= fAst.newFieldDeclaration(variableDeclarationFragment);
		fieldDeclaration.setType(fAst.newSimpleType(fAst.newSimpleName("String"))); //$NON-NLS-1$
		fieldDeclaration.modifiers().add(fAst.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
		fieldDeclaration.modifiers().add(fAst.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
		
		return fieldDeclaration;
	}

}
