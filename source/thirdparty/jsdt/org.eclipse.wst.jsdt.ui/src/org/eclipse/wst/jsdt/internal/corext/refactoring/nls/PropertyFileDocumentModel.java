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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;

public class PropertyFileDocumentModel {

	private static final char[] HEX_DIGITS = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    private List fKeyValuePairs;
    private String fLineDelimiter;

    public PropertyFileDocumentModel(IDocument document) {
        parsePropertyDocument(document);
        fLineDelimiter= TextUtilities.getDefaultLineDelimiter(document);
    }
    
    public int getIndex(String key) {
    	for (int i= 0; i < fKeyValuePairs.size(); i++) {
            KeyValuePairModell keyValuePair = (KeyValuePairModell) fKeyValuePairs.get(i);
            if (keyValuePair.getKey().equals(key)) {
            	return i;
            }
    	}
    	return -1;
    }
    

    public InsertEdit insert(String key, String value) {
        return insert(new KeyValuePair(key, value));
    }

    public InsertEdit insert(KeyValuePair keyValuePair) {
        KeyValuePairModell keyValuePairModell = new KeyValuePairModell(keyValuePair); 
        int index = findInsertPosition(keyValuePairModell);
        KeyValuePairModell insertHere = (KeyValuePairModell) fKeyValuePairs.get(index);
        int offset = insertHere.fOffset - insertHere.fLeadingWhiteSpaces;
        
        String extra= ""; //$NON-NLS-1$
        if (insertHere instanceof LastKeyValuePair && ((LastKeyValuePair)insertHere).needsNewLine()) {
        	extra= fLineDelimiter;
        	((LastKeyValuePair)insertHere).resetNeedsNewLine();
        }
        return new InsertEdit(offset, extra + keyValuePairModell.getEncodedText(fLineDelimiter));
    }

    public InsertEdit[] insert(KeyValuePair[] keyValuePairs) {
        InsertEdit[] inserts = new InsertEdit[keyValuePairs.length];
        for (int i = 0; i < keyValuePairs.length; i++) {            
            inserts[i] = insert(keyValuePairs[i]);
        }
        return inserts;        
    }
    
    public DeleteEdit remove(String key) {
    	for (Iterator iter = fKeyValuePairs.iterator(); iter.hasNext();) {
            KeyValuePairModell keyValuePair = (KeyValuePairModell) iter.next();
            if (keyValuePair.fKey.equals(key)) {
            	KeyValuePairModell next = (KeyValuePairModell) iter.next();
            	return new DeleteEdit(keyValuePair.fOffset, next.fOffset - keyValuePair.fOffset);
            }            
        }
        return null;
    }
    
    public ReplaceEdit replace(KeyValuePair toReplace, KeyValuePair replaceWith) {     
        for (Iterator iter = fKeyValuePairs.iterator(); iter.hasNext();) {
            KeyValuePairModell keyValuePair = (KeyValuePairModell) iter.next();
            if (keyValuePair.fKey.equals(toReplace.getKey())) {
                String newText = new KeyValuePairModell(replaceWith).getEncodedText(fLineDelimiter);
                KeyValuePairModell next = (KeyValuePairModell) iter.next();
                int range = next.fOffset - keyValuePair.fOffset;
            	return new ReplaceEdit(keyValuePair.fOffset, range, newText);
            }            
        }
        return null;
    }
    
    private int findInsertPosition(KeyValuePairModell keyValuePair) {
        int insertIndex = 0;
        int maxMatch = Integer.MIN_VALUE;
        for (int i=0; i<fKeyValuePairs.size(); i++) {
            KeyValuePairModell element = (KeyValuePairModell) fKeyValuePairs.get(i);
            int match = element.compareTo(keyValuePair);
            if (match >= maxMatch) {
                insertIndex = i;
                maxMatch = match;
            }            
        }
        
        if (insertIndex < fKeyValuePairs.size() - 1) {
            insertIndex++;
        }
        
        return insertIndex;
    }    

    private void parsePropertyDocument(IDocument document) {
        fKeyValuePairs = new ArrayList();
      
        SimpleLineReader reader = new SimpleLineReader(document);        
        int offset = 0;
        String line = reader.readLine();
        int leadingWhiteSpaces = 0;
        while (line != null) {
            if (!SimpleLineReader.isCommentOrWhiteSpace(line)) {
                int idx = getIndexOfSeparationCharacter(line);
                if (idx != -1) {
                	String key= line.substring(0, idx);
                	String value= line.substring(idx + 1);
                    fKeyValuePairs.add(new KeyValuePairModell(key, value, offset, leadingWhiteSpaces));
                    leadingWhiteSpaces = 0;
                }
            } else {
                leadingWhiteSpaces += line.length();
            }
            offset += line.length();
            line = reader.readLine();
        }
        int lastLine= document.getNumberOfLines() - 1;
        boolean needsNewLine= false;
        try {
        	needsNewLine= !(document.getLineLength(lastLine) == 0);
		} catch (BadLocationException ignore) {
			// treat last line having no new line
		}
        LastKeyValuePair lastKeyValuePair = new LastKeyValuePair(offset, needsNewLine);
		fKeyValuePairs.add(lastKeyValuePair);
    } 
    
    private int getIndexOfSeparationCharacter(String line) {
        int minIndex = -1;
        int indexOfEven = line.indexOf('=');
        int indexOfColumn = line.indexOf(':');
        int indexOfBlank = line.indexOf(' ');
        
        if ((indexOfEven != -1) && (indexOfColumn != -1)) {
            minIndex = Math.min(indexOfEven, indexOfColumn);            
        } else {
            minIndex = Math.max(indexOfEven, indexOfColumn);
        }
        
        if ((minIndex != -1) && (indexOfBlank != -1)) {
            minIndex = Math.min(minIndex, indexOfBlank);            
        } else {
            minIndex = Math.max(minIndex, indexOfBlank);
        }
        
        return minIndex;        
    }  
    
    public static String unwindEscapeChars(String s){
		StringBuffer sb= new StringBuffer(s.length());
		int length= s.length();
		for (int i= 0; i < length; i++){
			char c= s.charAt(i);
			sb.append(getUnwoundString(c));
		}
		return sb.toString();
	}

	public static String unwindValue(String value) {
		return escapeLeadingWhiteSpaces(escapeCommentChars(unwindEscapeChars(value)));
	}

	private static String getUnwoundString(char c){
	        	switch(c){
	        		case '\b' :
	        			return "\\b";//$NON-NLS-1$
	        		case '\t' :
	        			return "\\t";//$NON-NLS-1$
	        		case '\n' :
	        			return "\\n";//$NON-NLS-1$
	        		case '\f' :
	        			return "\\f";//$NON-NLS-1$	
	        		case '\r' :
	        			return "\\r";//$NON-NLS-1$

//      			These can be used unescaped in properties file:
//      			case '\"' :
//      			return "\\\"";//$NON-NLS-1$
//      			case '\'' :
//      			return "\\\'";//$NON-NLS-1$
	        			
	        		case '\\' :
	        			return "\\\\";//$NON-NLS-1$
	        		
//      			This is only done when writing to the .properties file in #unwindValue(String)
//      			case '!':
//      			return "\\!";//$NON-NLS-1$
//      			case '#':
//      			return "\\#";//$NON-NLS-1$
	        			
	        		default: 
	        			if (((c < 0x0020) || (c > 0x007e))){
	        				return new StringBuffer()
							.append('\\')
							.append('u')
							.append(toHex((c >> 12) & 0xF))
							.append(toHex((c >>  8) & 0xF))
							.append(toHex((c >>  4) & 0xF))
							.append(toHex( c        & 0xF)).toString();
	        				
	        			} else
	        				return String.valueOf(c);
	        	}		
	        }

	private static char toHex(int halfByte) {
		return HEX_DIGITS[(halfByte & 0xF)];
	}

	private static String escapeCommentChars(String string) {
	    StringBuffer sb = new StringBuffer(string.length() + 5);
	    for (int i = 0; i < string.length(); i++) {
	      char c = string.charAt(i);
	      switch (c) {
	      case '!':
	        sb.append("\\!"); //$NON-NLS-1$
	        break;
	      case '#':
	        sb.append("\\#"); //$NON-NLS-1$
	        break;
	      default:
	        sb.append(c);
	      }
	    }
	    return sb.toString();
	}

	private static String escapeLeadingWhiteSpaces(String str) {
		int firstNonWhiteSpace= findFirstNonWhiteSpace(str);
		StringBuffer buf= new StringBuffer(firstNonWhiteSpace);
		for (int i = 0; i < firstNonWhiteSpace; i++) {
			buf.append('\\');
		    buf.append(str.charAt(i));
		}
		buf.append(str.substring(firstNonWhiteSpace));        
		return buf.toString();
	}

	/**
	 *  returns the length if only whitespaces
	 */
	private static int findFirstNonWhiteSpace(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isWhitespace(s.charAt(i)))
				return i;
		}
		return s.length();
	}

	private static class KeyValuePairModell extends KeyValuePair implements Comparable {        

        int fOffset;
        int fLeadingWhiteSpaces;

        public KeyValuePairModell(String key, String value, int offset, int leadingWhiteSpaces) {
            super(key, value);             
            fOffset = offset;
            fLeadingWhiteSpaces = leadingWhiteSpaces;
        }
        
        public KeyValuePairModell(KeyValuePair keyValuePair) {
            super(keyValuePair.fKey, keyValuePair.fValue);
        }

        public String getEncodedText(String lineDelimiter) {
			return PropertyFileDocumentModel.unwindEscapeChars(fKey) + '=' + PropertyFileDocumentModel.unwindValue(fValue) + lineDelimiter;
        }
        
		public int compareTo(Object o) {
            int counter = 0;
            String key = ((KeyValuePair) o).fKey;
            int minLen = Math.min(key.length(), fKey.length());
            int diffLen = Math.abs(key.length() - fKey.length());
            for (int i=0; i<minLen; i++) {
                if (key.charAt(i) == fKey.charAt(i)) {
                    counter++;                    
                } else {
                    break;
                }
            }            
            return counter - diffLen;
        }
    }

    /**
     * anchor element for a list of KeyValuePairs. (it is greater than every
     * other KeyValuePair)
     */
    private static class LastKeyValuePair extends KeyValuePairModell {

    	private boolean fNeedsNewLine;
    	
        public LastKeyValuePair(int offset, boolean needsNewLine) {
            super("last", "key", offset, 0); //$NON-NLS-1$ //$NON-NLS-2$
            fNeedsNewLine= needsNewLine;
        }
        public int compareTo(Object o) {
            return 1;
        }
        public boolean needsNewLine() {
        	return fNeedsNewLine;
        }
        public void resetNeedsNewLine() {
        	fNeedsNewLine= false;
        }
    }
}
