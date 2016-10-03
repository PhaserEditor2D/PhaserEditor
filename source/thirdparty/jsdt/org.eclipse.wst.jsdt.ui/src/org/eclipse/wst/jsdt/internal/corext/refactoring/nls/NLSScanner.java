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
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.ToolFactory;
import org.eclipse.wst.jsdt.core.compiler.IScanner;
import org.eclipse.wst.jsdt.core.compiler.ITerminalSymbols;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;

public class NLSScanner {

	//no instances	
	private NLSScanner() {
	}

	/**
	 * Returns a list of NLSLines found in the compilation unit
	 */
	public static NLSLine[] scan(IJavaScriptUnit cu) throws JavaScriptModelException, InvalidInputException {
		return scan(cu.getBuffer().getCharacters());
	}

	/**
	 * Returns a list of NLSLines found in the string
	 */	
	public static NLSLine[] scan(String s) throws InvalidInputException {
		return scan(s.toCharArray()); 
	}
	
	private static NLSLine[] scan(char[] content) throws InvalidInputException {
		List lines= new ArrayList();
		IScanner scanner= ToolFactory.createScanner(true, true, false, true);
		scanner.setSource(content);
		int token= scanner.getNextToken();
		int currentLineNr= -1;
		int previousLineNr= -1;
		NLSLine currentLine= null;
		int nlsElementIndex= 0;
		
		while (token != ITerminalSymbols.TokenNameEOF) {
			switch (token) {
				case ITerminalSymbols.TokenNameStringLiteral:
					currentLineNr= scanner.getLineNumber(scanner.getCurrentTokenStartPosition());
					if (currentLineNr != previousLineNr) {
						currentLine= new NLSLine(currentLineNr - 1);
						lines.add(currentLine);
						previousLineNr= currentLineNr;
						nlsElementIndex= 0;
					}
					String value= new String(scanner.getCurrentTokenSource());
					currentLine.add(
					        new NLSElement(
					                value, 
					                scanner.getCurrentTokenStartPosition(), 
					                scanner.getCurrentTokenEndPosition() + 1 - scanner.getCurrentTokenStartPosition(),
					                nlsElementIndex++,
					                false));
					break;
				case ITerminalSymbols.TokenNameCOMMENT_LINE:
					if (currentLineNr != scanner.getLineNumber(scanner.getCurrentTokenStartPosition()))
						break;
						
					parseTags(currentLine, scanner);
					break;
			}
			token= scanner.getNextToken();
		}
		NLSLine[] result;
		try {
			result= (NLSLine[]) lines.toArray(new NLSLine[lines.size()]);
			IDocument document= new Document(String.valueOf(scanner.getSource()));
			for (int i= 0; i < result.length; i++) {
				setTagPositions(document, result[i]);
			}
		} catch (BadLocationException exception) {
			throw new InvalidInputException();
		}
		return result;
	}
	
	private static void parseTags(NLSLine line, IScanner scanner) {
		String s= new String(scanner.getCurrentTokenSource());
		int pos= s.indexOf(NLSElement.TAG_PREFIX);
		while (pos != -1) {
			int start= pos + NLSElement.TAG_PREFIX_LENGTH; 
			int end= s.indexOf(NLSElement.TAG_POSTFIX, start);
			if (end < 0)
				return; //no error recovery
				
			String index= s.substring(start, end);
			int i= 0;
			try {
				i= Integer.parseInt(index) - 1; 	// Tags are one based not zero based.
			} catch (NumberFormatException e) {
				return; //ignore the exception - no error recovery
			}
			if (line.exists(i)) {
				NLSElement element= line.get(i);
				element.setTagPosition(scanner.getCurrentTokenStartPosition() + pos, end - pos + 1);
			} else {
				return; //no error recovery
			}
			pos= s.indexOf(NLSElement.TAG_PREFIX, start);
		}
	}
	
	private static void setTagPositions(IDocument document, NLSLine line) throws BadLocationException {
		IRegion info= document.getLineInformation(line.getLineNumber());
		int defaultValue= info.getOffset() + info.getLength();
		NLSElement[] elements= line.getElements();
		for (int i= 0; i < elements.length; i++) {
			NLSElement element= elements[i];
			if (!element.hasTag()) {
				element.setTagPosition(computeInsertOffset(elements, i, defaultValue), 0);				
			}
		}
	}
	
	private static int computeInsertOffset(NLSElement[] elements, int index, int defaultValue) {
		NLSElement previousTagged= findPreviousTagged(index, elements);
		if (previousTagged != null)
			return previousTagged.getTagPosition().getOffset() + previousTagged.getTagPosition().getLength();
		NLSElement nextTagged= findNextTagged(index, elements);
		if (nextTagged != null)
			return nextTagged.getTagPosition().getOffset();
		return defaultValue;	
	}
	
	private static NLSElement findPreviousTagged(int startIndex, NLSElement[] elements){
		int i= startIndex - 1;
		while (i >= 0){
			if (elements[i].hasTag())
				return elements[i];
			i--;
		}
		return null;
	}
    
	private static NLSElement findNextTagged(int startIndex, NLSElement[] elements){
		int i= startIndex + 1;
		while (i < elements.length){
			if (elements[i].hasTag())
				return elements[i];
			i++;
		}
		return null;
	}			
}

