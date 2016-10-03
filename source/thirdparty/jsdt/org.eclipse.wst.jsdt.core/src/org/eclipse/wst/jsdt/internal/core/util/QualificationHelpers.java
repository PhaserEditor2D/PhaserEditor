/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.util;

import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;

/**
 * <p>Methods for helping with qualified type names, both to separate them into
 * qualifier and simple name, as well as recombining qualifier with simple name.</p>
 */
public class QualificationHelpers {
	
	/**
	 * <p>The index in the array containing the qualifier of a fully qualified name
	 * separated by a method in this class.</p>
	 * 
	 * @see #seperateFullyQualifedName(char[])
	 * @see #seperateFullyQualifedTypeNames(char[])
	 */
	public static final int QULIFIERS_INDEX = 0;
	
	/**
	 * <p>The index in the array containing the simple name of a fully qualified name
	 * separated by a method in this class.</p>
	 * 
	 * @see #seperateFullyQualifedName(char[])
	 * @see #seperateFullyQualifedTypeNames(char[])
	 */
	public static final int SIMPLE_NAMES_INDEX = 1;
	
	/**
	 * <p>Given a qualification and a simple name creates a fully qualified name.</p>
	 * 
	 * @param qualification the qualification, or <code>null</code> if no qualification
	 * @param simpleName the simple name, can not be <code>null</code>
	 * 
	 * @return fully qualified name created from the given <code>simpleName</code> and
	 * the optional <code>qualification</code>
	 */
	public static char[] createFullyQualifiedName(char[] qualification, char[] simpleName) {
		char[] fullTypeName = null;
		if(simpleName != null && simpleName.length > 0) {
			if(qualification != null && qualification.length > 0) {
				fullTypeName = CharOperation.concat(qualification, simpleName, IIndexConstants.DOT);
			} else {
				fullTypeName = simpleName;
			}
		}
		
		return fullTypeName;
	}
	
	/**
	 * <p>Given a list of qualifications and a list of simple names creates a single list of
	 * fully qualified names created by matching one qualification and one simple name from
	 * their respective lists in order.</p>
	 * 
	 * @param qualifications to match with the given <code>simpleNames</code>, this can be
	 * <code>null</code> if there are not qualifications, or an array of the same size as
	 * <code>simpleNames</code> where any one of the indices maybe <code>null</code> to signify
	 * there is no qualifier for that specific simple name.
	 * @param simpleNames to match with the given <code>qualifications</code>, this array
	 * can <b>not</b> be <code>null</code> and no indices in the array can be <code>null</code> either
	 * 
	 * @return an array of fully qualified names created from the given <code>simpleNames</code>
	 * and the optional <code>qualifications</code>
	 */
	public static char[][] createFullyQualifiedNames(char[][] qualifications, char[][] simpleNames) {
		char[][] fullTypeNames = null;
		
		if(simpleNames != null) {
			fullTypeNames = new char[simpleNames.length][];
			for(int i = 0; i < fullTypeNames.length; ++i) {
				if(qualifications != null && qualifications.length > i) {
					fullTypeNames[i] = createFullyQualifiedName(qualifications[i], simpleNames[i]);
				} else {
					fullTypeNames[i] = simpleNames[i];
				}
			}
		}
		
		return fullTypeNames;
	}
	
	/**
	 * <p>Separates a fully qualified name into its qualifier and its simple name</p>
	 * 
	 * @param fullyQualifiedName fully qualified type name to separate into its qualifier and simple name
	 * 
	 * @return a multidimensional array with one dimension for the qualifier and one for the simple name
	 * 
	 * @see #QULIFIERS_INDEX
	 * @see #SIMPLE_NAMES_INDEX
	 */
	public static char[][] seperateFullyQualifedName(char[] fullyQualifiedName) {
		char[][] seperatedTypeName = new char[2][];
		
		if(fullyQualifiedName != null && fullyQualifiedName.length > 0) {
			int lastIndexOfDot = CharOperation.lastIndexOf(IIndexConstants.DOT, fullyQualifiedName);
			if(lastIndexOfDot != -1) {
				seperatedTypeName[QULIFIERS_INDEX] = CharOperation.subarray(fullyQualifiedName, 0, lastIndexOfDot);
				seperatedTypeName[SIMPLE_NAMES_INDEX] = CharOperation.subarray(fullyQualifiedName, lastIndexOfDot+1, -1);
			} else {
				seperatedTypeName[QULIFIERS_INDEX] = null;
				seperatedTypeName[SIMPLE_NAMES_INDEX] = fullyQualifiedName;
			}
		}
		
		return seperatedTypeName;
	}
	
	/**
	 * <p>Separates an array of fully qualified names into their qualifiers and their simple names</p>
	 * 
	 * @param fullyQualifiedNames fully qualified type names to separate into their qualifiers and their simple names
	 * @param minLength the minimum length of the result, padding will be with <code>null</code>
	 * 
	 * @return resulting array consists of three indices.  The first is either {@link #QULIFIERS_INDEX} or
	 * {@link #SIMPLE_NAMES_INDEX}, the second is then a list of either the qualifiers or the simple names,
	 * depending on the first index, the last index is the char[] "string" qualifier or simple name.
	 * 
	 * @see #QULIFIERS_INDEX
	 * @see #SIMPLE_NAMES_INDEX
	 */
	public static char[][][] seperateFullyQualifiednames(String[] fullyQualifiedNames, int minLength) {
		return seperateFullyQualifiedNames(stringArrayToCharArray(fullyQualifiedNames), minLength);
	}
	
	/**
	 * <p>Separates a list of fully qualified names separated by {@link IIndexConstants#PARAMETER_SEPARATOR}
	 * into their qualifiers and their simple names</p>
	 * 
	 * @param fullyQualifiedNames a list of fully qualified type names separated by {@link IIndexConstants#PARAMETER_SEPARATOR}
	 * to separate into their qualifiers and their simple names
	 * @param minLength the minimum length of the result, padding will be with <code>null</code>
	 * 
	 * @return resulting array consists of three indices.  The first is either {@link #QULIFIERS_INDEX} or
	 * {@link #SIMPLE_NAMES_INDEX}, the second is then a list of either the qualifiers or the simple names,
	 * depending on the first index, the last index is the char[] "string" qualifier or simple name.
	 * 
	 * @see #QULIFIERS_INDEX
	 * @see #SIMPLE_NAMES_INDEX
	 */
	public static char[][][] seperateFullyQualifiedNames(char[] fullyQualifiedNames, int minLength) {
		char[][] names = CharOperation.splitOn(IIndexConstants.PARAMETER_SEPARATOR, fullyQualifiedNames);
		return seperateFullyQualifiedNames(names, minLength);
	}
	
	/**
	 * <p>Separates an array of fully qualified names into their qualifiers and their simple names</p>
	 * 
	 * @param fullyQualifiedNames fully qualified type names to separate into their qualifiers and their simple names
	 * @param minLength the minimum length of the result, padding will be with <code>null</code>
	 * 
	 * @return resulting array consists of three indices.  The first is either {@link #QULIFIERS_INDEX} or
	 * {@link #SIMPLE_NAMES_INDEX}, the second is then a list of either the qualifiers or the simple names,
	 * depending on the first index, the last index is the char[] "string" qualifier or simple name.
	 * 
	 * @see #QULIFIERS_INDEX
	 * @see #SIMPLE_NAMES_INDEX
	 */
	public static char[][][] seperateFullyQualifiedNames(char[][] fullyQualifiedNames, int minLength) {
		/* 
		 * First index is 0 or 1 for the list of qualifiers qualifier and then the list of simple names respectively
		 * Second index is a list of the qualifiers and simple names
		 * Third index is the actual 'string' qualifier or simple name
		 */
		char[][][] seperatedTypeNames = new char[2][][];
		
		if(fullyQualifiedNames.length > 0) {
			int length = minLength > fullyQualifiedNames.length ? minLength : fullyQualifiedNames.length;
			seperatedTypeNames[QULIFIERS_INDEX] = new char[length][];
			seperatedTypeNames[SIMPLE_NAMES_INDEX] = new char[length][];
			
			for(int i = 0; i < fullyQualifiedNames.length; ++i) {
				char[][] seperatedTypeName = seperateFullyQualifedName(fullyQualifiedNames[i]);
				seperatedTypeNames[QULIFIERS_INDEX][i] = seperatedTypeName[QULIFIERS_INDEX];
				seperatedTypeNames[SIMPLE_NAMES_INDEX][i] = seperatedTypeName[SIMPLE_NAMES_INDEX];
				
				//in case the qualifier is a signature, nothing happens if it is not
				if(seperatedTypeNames[QULIFIERS_INDEX][i] != null && seperatedTypeNames[QULIFIERS_INDEX][i].length > 0) {
					try {
						seperatedTypeNames[QULIFIERS_INDEX][i] = Signature.toCharArray(seperatedTypeNames[QULIFIERS_INDEX][i]);
					} catch(IllegalArgumentException e) {
						/* ignore, this will happen if a name looking like it maybe a signature gets passed in, but isn't, such as "QName"
						 * the real future fix for this should be to completely stop using signatures
						 */
					}				}
				
				//in case the simple name is a signature, nothing happens if it is not
				if(seperatedTypeNames[SIMPLE_NAMES_INDEX][i] != null && seperatedTypeNames[SIMPLE_NAMES_INDEX][i].length > 0) {
					try {
						seperatedTypeNames[SIMPLE_NAMES_INDEX][i] = Signature.toCharArray(seperatedTypeNames[SIMPLE_NAMES_INDEX][i]);
					} catch(IllegalArgumentException e) {
						/* ignore, this will happen if a name looking like it maybe a signature gets passed in, but isn't, such as "QName"
						 * the real future fix for this should be to completely stop using signatures
						 */
					}
				}
			}
			
		} else if (minLength > 0) {
			seperatedTypeNames[QULIFIERS_INDEX] = new char[minLength][];
			seperatedTypeNames[SIMPLE_NAMES_INDEX] = new char[minLength][];
		} else {
			seperatedTypeNames[QULIFIERS_INDEX] = null;
			seperatedTypeNames[SIMPLE_NAMES_INDEX] = null;
		}
		
		return seperatedTypeNames;
	}
	
	/**
	 * <p>Transform a {@link String} array into a <code>char</code> array.</p>
	 * 
	 * @param array of {@link String}s to transform into an array of <code>char</code>s
	 * 
	 * @return array of {@link String}s built from the given array of <code>char</code>s
	 */
	public static char[][] stringArrayToCharArray(String[] array) {
		char[][] results = null;
		if(array != null) {
			results = new char[array.length][];
			for(int i = 0; i < array.length; ++i) {
				results[i] = array[i].toCharArray();
			}
		}
		
		return results;
	}
}
