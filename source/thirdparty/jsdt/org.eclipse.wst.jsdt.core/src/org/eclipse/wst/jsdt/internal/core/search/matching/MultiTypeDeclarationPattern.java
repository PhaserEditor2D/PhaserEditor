/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search.matching;

import java.io.IOException;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.core.index.EntryResult;
import org.eclipse.wst.jsdt.internal.core.index.Index;

/**
 * <p>Pattern used to search for multiple types simultaneously.</p>
 */
public class MultiTypeDeclarationPattern extends TypeDeclarationPattern {

	/**
	 * <p>List of type simple names to match on.</p>
	 */
	private char[][] fSimpleNames;
	
	/**
	 * <p><b>Optional</b></p>
	 * 
	 * <p>List of qualifications to match on.  If specified should be the 
	 * same length as {@link #fSimpleNames} matching one to one the qualifications
	 * to the simple names.</p>
	 */
	private char[][] fQualifications;

	/**
	 * <p>Internal constructor for creating plank patterns</p>
	 *
	 * @param matchRule match rule used when comparing this pattern to search results
	 */
	MultiTypeDeclarationPattern(int matchRule) {
		super(matchRule);
	}
	
	/**
	 * <p>Constructor used to search for multiple types simultaneously that may or may not have
	 * qualifications defined.</p>
	 *
	 * @param qualifications Optional list of qualifications to go with the simple type names that are being searched for
	 * @param simpleNames List of simple type names being searched for
	 * @param matchRule match rule used when comparing this pattern to search results
	 */
	public MultiTypeDeclarationPattern(char[][] qualifications,
			char[][] simpleNames, int matchRule) {

		this(matchRule);

		if (isCaseSensitive() || qualifications == null) {
			this.fQualifications = qualifications;
		} else {
			int length = qualifications.length;
			this.fQualifications = new char[length][];
			for (int i = 0; i < length; i++)
				this.fQualifications[i] = CharOperation
						.toLowerCase(qualifications[i]);
		}
		// null simple names are allowed (should return all names)
		if (simpleNames != null) {
			if ((isCaseSensitive() || isCamelCase())) {
				this.fSimpleNames = simpleNames;
			} else {
				int length = simpleNames.length;
				this.fSimpleNames = new char[length][];
				for (int i = 0; i < length; i++)
					this.fSimpleNames[i] = CharOperation
							.toLowerCase(simpleNames[i]);
			}
		}
	}

	/**
	 * <p>Iterates over all of the type names to match on for this pattern and then uses {@link TypeDeclarationPattern#matchesDecodedKey(SearchPattern)}
	 * to actually do the match checking.</p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.TypeDeclarationPattern#matchesDecodedKey(org.eclipse.wst.jsdt.core.search.SearchPattern)
	 */
	public boolean matchesDecodedKey(SearchPattern decodedPattern) {
		boolean foundMatch = false;
		
		//loop each type
		int typesLength = this.getTypesLength();
		for(int i = 0; i < typesLength && !foundMatch; ++i) {
			//set the simple name
			if(this.fSimpleNames != null && this.fSimpleNames.length > i) {
				this.simpleName = this.fSimpleNames[i];
			} else {
				this.simpleName = null;
			}
			
			//set the qualification
			if(this.fQualifications != null && this.fQualifications.length > i) {
				this.qualification = this.fQualifications[i];
			} else {
				this.qualification = null;
			}
			
			//check if match
			foundMatch = super.matchesDecodedKey(decodedPattern);
		}
		
		//reset simple name and qualification
		this.simpleName = null;
		this.qualification = null;
		
		return foundMatch;
	}

	/**
	 * <p>Iterates over all of the types names to match on for this pattern and then uses {@link TypeDeclarationPattern#queryIn(Index)}
	 * to actually do the querying.</p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.TypeDeclarationPattern#queryIn(org.eclipse.wst.jsdt.internal.core.index.Index)
	 */
	EntryResult[] queryIn(Index index) throws IOException {
		
		EntryResult[] results = null;
		
		//loop each type
		int typesLength = this.getTypesLength();
		for(int i = 0; i < typesLength; ++i) {
			//set the simple name
			if(this.fSimpleNames != null && this.fSimpleNames.length > i) {
				this.simpleName = this.fSimpleNames[i];
			} else {
				this.simpleName = null;
			}
			
			//set the qualification
			if(this.fQualifications != null && this.fQualifications.length > i) {
				this.qualification = this.fQualifications[i];
			} else {
				this.qualification = null;
			}
			
			//run query using parent function now that one simple name and one qualification have been set
			EntryResult[] additionalResults = super.queryIn(index);
			
			//collect results
			if(additionalResults != null && additionalResults.length > 0) {
				if(results == null) {
					results = additionalResults;
				} else {
					EntryResult[] existingResults = results;
					
					results = new EntryResult[existingResults.length + additionalResults.length];
					
					System.arraycopy(existingResults, 0, results, 0, existingResults.length);
					System.arraycopy(additionalResults, 0, results, existingResults.length, additionalResults.length);
				}
			}
		}
		
		//reset simple name and qualification
		this.simpleName = null;
		this.qualification = null;
		
		return results;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.TypeDeclarationPattern#print(java.lang.StringBuffer)
	 */
	protected StringBuffer print(StringBuffer output) {
		output.append("MultiTypeDeclarationPattern: "); //$NON-NLS-1$
		if (fQualifications != null) {
			output.append("qualifications: <"); //$NON-NLS-1$
			for (int i = 0; i < fQualifications.length; i++) {
				output.append(fQualifications[i]);
				if (i < fQualifications.length - 1)
					output.append(", "); //$NON-NLS-1$
			}
			output.append("> "); //$NON-NLS-1$
		}
		if (fSimpleNames != null) {
			output.append("simpleNames: <"); //$NON-NLS-1$
			for (int i = 0; i < fSimpleNames.length; i++) {
				output.append(fSimpleNames[i]);
				if (i < fSimpleNames.length - 1)
					output.append(", "); //$NON-NLS-1$
			}
			output.append(">"); //$NON-NLS-1$
		}
		return super.print(output);
	}
	

	/**
	 * @return length of {@link #fSimpleNames} or {@link #fQualifications}, whichever is longer
	 */
	private int getTypesLength() {
		int length = 0;
		if(this.fSimpleNames != null && this.fQualifications != null) {
			length = (this.fSimpleNames.length > this.fQualifications.length) ?
					this.fSimpleNames.length : this.fQualifications.length;
		} else if(this.fSimpleNames != null) {
			length = this.fSimpleNames.length;
		} else if(this.fQualifications != null) {
			length = this.fQualifications.length;
		}
		
		return length;
	}
}
