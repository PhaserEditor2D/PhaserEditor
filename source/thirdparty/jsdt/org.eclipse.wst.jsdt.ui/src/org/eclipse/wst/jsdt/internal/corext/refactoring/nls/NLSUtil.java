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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.core.formatter.IndentManipulation;


public class NLSUtil {

	//no instances
	private NLSUtil() {
	}

	/**
	 * Returns null if an error occurred.
	 * closes the stream 
	 */
	public static String readString(InputStream is, String encoding) {
		if (is == null)
			return null;
		BufferedReader reader= null;
		try {
			StringBuffer buffer= new StringBuffer();
			char[] part= new char[2048];
			int read= 0;
			reader= new BufferedReader(new InputStreamReader(is, encoding));

			while ((read= reader.read(part)) != -1)
				buffer.append(part, 0, read);

			return buffer.toString();

		} catch (IOException ex) {
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ex) {
				}
			}
		}
		return null;
	}

	/**
	 * Creates and returns an NLS tag edit for a string that is at the specified position in 
	 * a compilation unit. Returns <code>null</code> if the string is already NLSed 
	 * or the edit could not be created for some other reason.
	 * @throws CoreException 
	 */
	public static TextEdit createNLSEdit(IJavaScriptUnit cu, int position) throws CoreException {
		NLSLine nlsLine= scanCurrentLine(cu, position);
		if (nlsLine == null)
			return null;
		NLSElement element= findElement(nlsLine, position);
		if (element.hasTag())
			return null;
		NLSElement[] elements= nlsLine.getElements();
		int indexInElementList= Arrays.asList(elements).indexOf(element);
		int editOffset= computeInsertOffset(elements, indexInElementList, cu);
		String editText= ' ' + NLSElement.createTagText(indexInElementList + 1); //tags are 1-based
		return new InsertEdit(editOffset, editText);
	}
	
	/**
	 * Creates and returns NLS tag edits for strings that are at the specified positions in 
	 * a compilation unit. Returns <code>null</code> if all the strings are already NLSed 
	 * or the edits could not be created for some other reason.
	 * @throws CoreException 
	 */
	public static TextEdit[] createNLSEdits(IJavaScriptUnit cu, int[] positions) throws CoreException {
		List result= new ArrayList();
		try {
			NLSLine[] allLines= NLSScanner.scan(cu);
			for (int i= 0; i < allLines.length; i++) {
				NLSLine line= allLines[i];
				NLSElement[] elements= line.getElements();
				for (int j= 0; j < elements.length; j++) {
					NLSElement element= elements[j];
					if (!element.hasTag()) {
						for (int k= 0; k < positions.length; k++) {
							if (isPositionInElement(element, positions[k])) {
								int editOffset;
								if (j==0) {
									if (elements.length > j+1) {
										editOffset= elements[j+1].getTagPosition().getOffset();
									} else {
										editOffset= findLineEnd(cu, element.getPosition().getOffset());
									}
								} else {
									Region previousPosition= elements[j-1].getTagPosition();
									editOffset=  previousPosition.getOffset() + previousPosition.getLength();
								}
								String editText= ' ' + NLSElement.createTagText(j + 1); //tags are 1-based
								result.add(new InsertEdit(editOffset, editText));
							}
						}
					}
				}
			}
		} catch (InvalidInputException e) {
			return null;
		}
		if (result.isEmpty())
			return null;
		
		return (TextEdit[])result.toArray(new TextEdit[result.size()]);
	}

	private static NLSLine scanCurrentLine(IJavaScriptUnit cu, int position) throws JavaScriptModelException {
		try {
			Assert.isTrue(position >= 0 && position <= cu.getBuffer().getLength());
			NLSLine[] allLines= NLSScanner.scan(cu);
			for (int i= 0; i < allLines.length; i++) {
				NLSLine line= allLines[i];
				if (findElement(line, position) != null)
					return line;
			}
			return null;
		} catch (InvalidInputException e) {
			return null;
		}
	}

	private static boolean isPositionInElement(NLSElement element, int position) {
		Region elementPosition= element.getPosition();
		return (elementPosition.getOffset() <= position && position <= elementPosition.getOffset() + elementPosition.getLength());
	}

	private static NLSElement findElement(NLSLine line, int position) {
		NLSElement[] elements= line.getElements();
		for (int i= 0; i < elements.length; i++) {
			NLSElement element= elements[i];
			if (isPositionInElement(element, position))
				return element;
		}
		return null;
	}

	//we try to find a good place to put the nls tag
	//first, try to find the previous nlsed-string and try putting after its tag
	//if no such string exists, try finding the next nlsed-string try putting before its tag
	//otherwise, find the line end and put the tag there
	private static int computeInsertOffset(NLSElement[] elements, int index, IJavaScriptUnit cu) throws CoreException {
		NLSElement previousTagged= findPreviousTagged(index, elements);
		if (previousTagged != null)
			return previousTagged.getTagPosition().getOffset() + previousTagged.getTagPosition().getLength();
		NLSElement nextTagged= findNextTagged(index, elements);
		if (nextTagged != null)
			return nextTagged.getTagPosition().getOffset();
		return findLineEnd(cu, elements[index].getPosition().getOffset());
	}

	private static NLSElement findPreviousTagged(int startIndex, NLSElement[] elements) {
		int i= startIndex - 1;
		while (i >= 0) {
			if (elements[i].hasTag())
				return elements[i];
			i--;
		}
		return null;
	}

	private static NLSElement findNextTagged(int startIndex, NLSElement[] elements) {
		int i= startIndex + 1;
		while (i < elements.length) {
			if (elements[i].hasTag())
				return elements[i];
			i++;
		}
		return null;
	}

	private static int findLineEnd(IJavaScriptUnit cu, int position) throws JavaScriptModelException {
		IBuffer buffer= cu.getBuffer();
		int length= buffer.getLength();
		for (int i= position; i < length; i++) {
			if (IndentManipulation.isLineDelimiterChar(buffer.getChar(i))) {
				return i;
			}
		}
		return length;
	}
}
