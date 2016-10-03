/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.java;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.core.search.SearchRequestor;
import org.eclipse.wst.jsdt.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.wst.jsdt.internal.core.search.matching.FieldPattern;
import org.eclipse.wst.jsdt.internal.core.util.QualificationHelpers;
import org.eclipse.wst.jsdt.internal.corext.template.java.SignatureUtil;
import org.eclipse.wst.jsdt.internal.ui.Logger;


/**
 * Proposal info that computes the javadoc lazily when it is queried.
 *
 * 
 */
public final class FieldProposalInfo extends MemberProposalInfo {	
	/**
	 * Creates a new proposal info.
	 *
	 * @param project the java project to reference when resolving types
	 * @param proposal the proposal to generate information for
	 */
	public FieldProposalInfo(IJavaScriptProject project, CompletionProposal proposal) {
		super(project, proposal);
	}
	
	/**
	 * <p>Returns the java element that this computer corresponds to, possibly <code>null</code>.</p>
	 * 
	 * <p><b>NOTE:</b> This overrides the parent implementation so that {@link #resolveMember()} is not called
	 * because the field proposal can resolve to a none {@link IMember}.</p>
	 * 
	 * @return the java element that this computer corresponds to, possibly <code>null</code>
	 * @throws JavaScriptModelException
	 */
	public IJavaScriptElement getJavaElement() throws JavaScriptModelException {
		if (!fJavaElementResolved) {
			fJavaElementResolved= true;
			//call the internal resolve rather then #resolveMember
			fElement = resolve();
		}
		return fElement;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.MemberProposalInfo#resolveMember()
	 */
	protected IMember resolveMember() throws JavaScriptModelException {
		IMember member = null;
		IJavaScriptElement element = this.resolve();
		if(element instanceof IMember) {
			member = (IMember)element;
		}
		return member;
	}

	/**
	 * @return {@link IJavaScriptElement} that this field proposal resolves to
	 * 
	 * @throws JavaScriptModelException
	 */
	private IJavaScriptElement resolve() throws JavaScriptModelException {
		//get the type name
		char[] typeNameChars = fProposal.getDeclarationTypeName();
		String declaringTypeName = null;
		if(typeNameChars != null) {
			declaringTypeName = String.valueOf(typeNameChars);
		}
		
		/* try using the signature if type name not set
		 * NOTE: old way of doing things, should be removed at some point
		 */
		if(declaringTypeName == null) {
			char[] declarationSignature= fProposal.getDeclarationSignature();
			if(declarationSignature != null) {
				declaringTypeName = SignatureUtil.stripSignatureToFQN(String.valueOf(declarationSignature));
			}
		}
		
		//find the field
		IJavaScriptElement resolvedField = null;
		if(declaringTypeName != null) {
			String fieldName = String.valueOf(fProposal.getName());
			
			IType[] types = this.fJavaProject.findTypes(declaringTypeName);
			if(types != null && types.length > 0) {
				for(int i = 0; i < types.length && resolvedField == null; ++i) {
					IType type = types[i];
					if (type != null) {
						IField field = type.getField(fieldName);
						if (field.exists()) {
							resolvedField = field;
						}
					}
				}
			} else {
				//create the search pattern
				char[][] seperatedDeclaringTypename = QualificationHelpers.seperateFullyQualifedName(declaringTypeName.toCharArray());
//				if(!CharOperation.equals(seperatedDeclaringTypename[QualificationHelpers.SIMPLE_NAMES_INDEX], IIndexConstants.GLOBAL_SYMBOL)) {
					FieldPattern fieldPattern = new FieldPattern(true, false, false,
								fieldName.toCharArray(),
								seperatedDeclaringTypename[QualificationHelpers.QULIFIERS_INDEX],
								seperatedDeclaringTypename[QualificationHelpers.SIMPLE_NAMES_INDEX],
								SearchPattern.R_EXACT_MATCH);
					
					//search the index for a match
					SearchEngine searchEngine = new SearchEngine(DefaultWorkingCopyOwner.PRIMARY);
					IJavaScriptSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaScriptElement[] {this.fJavaProject});
					final List matches = new ArrayList();
					try {
						searchEngine.search(fieldPattern,
								new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
								scope,
								new SearchRequestor() {
									public void acceptSearchMatch(SearchMatch match) throws CoreException {
										Object element = match.getElement();
										if(element instanceof IField || element instanceof ILocalVariable) {
											matches.add(element);
										}
									}
								},
								new NullProgressMonitor());  //using a NPM here maybe a bad idea, but nothing better to do right now
					}
					catch (CoreException e) {
						Logger.logException("Failed index search for field: " + fieldName, e); //$NON-NLS-1$
					}
					
					// just use the first match found
					if(!matches.isEmpty()) {
						resolvedField = (IJavaScriptElement)matches.get(0);
					}
//				}
			}
		}
		
		return resolvedField;
	}
}
