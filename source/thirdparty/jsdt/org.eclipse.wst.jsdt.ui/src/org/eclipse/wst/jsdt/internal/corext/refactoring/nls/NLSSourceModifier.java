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

import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

public class NLSSourceModifier {

	private final String fSubstitutionPattern;
	private final boolean fIsEclipseNLS;

	private NLSSourceModifier(String substitutionPattern, boolean isEclipseNLS) {
		fSubstitutionPattern= substitutionPattern;
		fIsEclipseNLS= isEclipseNLS;
	}

	public static Change create(IJavaScriptUnit cu, NLSSubstitution[] subs, String substitutionPattern, IPackageFragment accessorPackage, String accessorClassName, boolean isEclipseNLS) throws CoreException {

		NLSSourceModifier sourceModification= new NLSSourceModifier(substitutionPattern, isEclipseNLS);

		String message= Messages.format(NLSMessages.NLSSourceModifier_change_description, cu.getElementName()); 

		TextChange change= new CompilationUnitChange(message, cu);
		MultiTextEdit multiTextEdit= new MultiTextEdit();
		change.setEdit(multiTextEdit);

		accessorClassName= sourceModification.createImportForAccessor(multiTextEdit, accessorClassName, accessorPackage, cu);

		for (int i= 0; i < subs.length; i++) {
			NLSSubstitution substitution= subs[i];
			int newState= substitution.getState();
			if (substitution.hasStateChanged()) {
				if (newState == NLSSubstitution.EXTERNALIZED) {
					if (substitution.getInitialState() == NLSSubstitution.INTERNALIZED) {
						sourceModification.addNLS(substitution, change, accessorClassName);
					} else if (substitution.getInitialState() == NLSSubstitution.IGNORED) {
						sourceModification.addAccessor(substitution, change, accessorClassName);
					}
				} else if (newState == NLSSubstitution.INTERNALIZED) {
					if (substitution.getInitialState() == NLSSubstitution.IGNORED) {
						sourceModification.deleteTag(substitution, change);
						if (substitution.isValueRename()) {
							sourceModification.replaceValue(substitution, change);
						}
					} else if (substitution.getInitialState() == NLSSubstitution.EXTERNALIZED) {
						sourceModification.deleteAccessor(substitution, change, cu);
						if (!isEclipseNLS)
							sourceModification.deleteTag(substitution, change);
					}
				} else if (newState == NLSSubstitution.IGNORED) {
					if (substitution.getInitialState() == NLSSubstitution.INTERNALIZED) {
						sourceModification.addNLS(substitution, change, accessorClassName);
						if (substitution.isValueRename()) {
							sourceModification.replaceValue(substitution, change);
						}
					} else {
						if (substitution.getInitialState() == NLSSubstitution.EXTERNALIZED) {
							sourceModification.deleteAccessor(substitution, change, cu);
						}
					}
					}
			} else {
				if (newState == NLSSubstitution.EXTERNALIZED) {
					if (substitution.isKeyRename()) {
						sourceModification.replaceKey(substitution, change);
					}
					if (substitution.isAccessorRename()) {
						sourceModification.replaceAccessor(substitution, change);
					}
				} else {
					if (substitution.isValueRename()) {
						sourceModification.replaceValue(substitution, change);
					}
				}
			}
		}

		return change;
	}

	/**
	 * @param substitution
	 * @param change
	 */
	private void replaceAccessor(NLSSubstitution substitution, TextChange change) {
		AccessorClassReference accessorClassRef= substitution.getAccessorClassReference();
		if (accessorClassRef != null) {
			Region region= accessorClassRef.getRegion();
			int len= accessorClassRef.getName().length();
			String[] args= {accessorClassRef.getName(), substitution.getUpdatedAccessor()};
			TextChangeCompatibility.addTextEdit(change, Messages.format(NLSMessages.NLSSourceModifier_replace_accessor, args), 
					new ReplaceEdit(region.getOffset(), len, substitution.getUpdatedAccessor())); // 
		}
		
	}

	private void replaceKey(NLSSubstitution substitution, TextChange change) {
		Region region= substitution.getNLSElement().getPosition();
		String[] args= {substitution.getInitialKey(), substitution.getKey()};
		
		ReplaceEdit replaceEdit;
		if (fIsEclipseNLS)
			replaceEdit= new ReplaceEdit(region.getOffset(), region.getLength(), substitution.getKey());
		else
			replaceEdit= new ReplaceEdit(region.getOffset(), region.getLength(), '\"' + unwindEscapeChars(substitution.getKey()) + '\"'); // 
		
		TextChangeCompatibility.addTextEdit(change, Messages.format(NLSMessages.NLSSourceModifier_replace_key, args), replaceEdit); 
	}
	
	private void replaceValue(NLSSubstitution substitution, TextChange change) {
		Region region= substitution.getNLSElement().getPosition();
		String[] args= {substitution.getInitialValue(), substitution.getValueNonEmpty()};
		TextChangeCompatibility.addTextEdit(change, Messages.format(NLSMessages.NLSSourceModifier_replace_value, args), 
				new ReplaceEdit(region.getOffset(), region.getLength(), '\"' + unwindEscapeChars(substitution.getValueNonEmpty()) + '\"')); // 
	}

	private void deleteAccessor(NLSSubstitution substitution, TextChange change, IJavaScriptUnit cu) throws CoreException {
		AccessorClassReference accessorClassRef= substitution.getAccessorClassReference();
		if (accessorClassRef != null) {
			Region region= accessorClassRef.getRegion();
			String[] args= {substitution.getValueNonEmpty(), substitution.getKey()};
			String label= Messages.format(NLSMessages.NLSSourceModifier_remove_accessor, args);
			String replaceString= '\"' + unwindEscapeChars(substitution.getValueNonEmpty()) + '\"';
			TextChangeCompatibility.addTextEdit(change, label, new ReplaceEdit(region.getOffset(), region.getLength(), replaceString));			
			if (fIsEclipseNLS && substitution.getState() != NLSSubstitution.INTERNALIZED) {
				
				Region position= substitution.getNLSElement().getPosition();
				int lineStart= getLineStart(cu.getBuffer(), position.getOffset());
				int lineEnd= getLineEnd(cu.getBuffer(), position.getOffset());
				String cuLine= cu.getBuffer().getText(lineStart, lineEnd - lineStart);
				StringBuffer buf= new StringBuffer(cuLine);
				buf.replace(region.getOffset() - lineStart, region.getOffset() + region.getLength() - lineStart, replaceString);
				try {
					NLSLine[] allLines= NLSScanner.scan(buf.toString());
					
					NLSLine nlsLine= allLines[0];
					NLSElement element= findElement(nlsLine, position.getOffset() - lineStart - accessorClassRef.getName().length() - 1);
					if (element == null || element.hasTag())
						return;
					
					NLSElement[] elements= nlsLine.getElements();
					int indexInElementList= Arrays.asList(elements).indexOf(element);
					String editText= ' ' + NLSElement.createTagText(indexInElementList + 1); //tags are 1-based
					TextChangeCompatibility.addTextEdit(change, label, new InsertEdit(lineEnd, editText));

				} catch (InvalidInputException e) {
					
				}
			}
		}
	}

	private int getLineEnd(IBuffer buffer, int offset) {
		int pos= offset;
		int length= buffer.getLength();
		while (pos < length && !isDelemiter(buffer.getChar(pos))) {
			pos++;
		}
		return pos;
	}

	private int getLineStart(IBuffer buffer, int offset) {
		int pos= offset;
		while (pos >= 0 && !isDelemiter(buffer.getChar(pos))) {
			pos--;
		}
		return pos + 1;
	}

	private boolean isDelemiter(char ch) {
		String[] delem= TextUtilities.DELIMITERS;
		for (int i= 0; i < delem.length; i++) {
			if (delem[i].length() == 1 && ch == delem[i].charAt(0))
				return true;
		}
		return false;
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

	// TODO: not dry
	private String unwindEscapeChars(String s) {
		StringBuffer sb= new StringBuffer(s.length());
		int length= s.length();
		for (int i= 0; i < length; i++) {
			char c= s.charAt(i);
			sb.append(getUnwoundString(c));
		}
		return sb.toString();
	}

	private String getUnwoundString(char c) {
		switch (c) {
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
			case '\\' :
				return "\\\\";//$NON-NLS-1$
		}
		return String.valueOf(c);
	}

	private void deleteTag(NLSSubstitution substitution, TextChange change) {
		Region textRegion= substitution.getNLSElement().getTagPosition();

		TextChangeCompatibility.addTextEdit(change, NLSMessages.NLSSourceModifier_remove_tag, 
				new DeleteEdit(textRegion.getOffset(), textRegion.getLength()));
	}

	private String createImportForAccessor(MultiTextEdit parent, String accessorClassName, IPackageFragment accessorPackage, IJavaScriptUnit cu) throws CoreException {
		IType type= accessorPackage.getJavaScriptUnit(accessorClassName + JavaModelUtil.DEFAULT_CU_SUFFIX).getType(accessorClassName);
		String fullyQualifiedName= type.getFullyQualifiedName();

		ImportRewrite importRewrite= StubUtility.createImportRewrite(cu, true);
		String nameToUse= importRewrite.addImport(fullyQualifiedName);
		TextEdit edit= importRewrite.rewriteImports(null);
		parent.addChild(edit);

		return nameToUse;
	}

	private void addNLS(NLSSubstitution sub, TextChange change, String accessorName) {
		if (sub.getState() == NLSSubstitution.INTERNALIZED)
			return;
		
		NLSElement element= sub.getNLSElement();

		addAccessor(sub, change, accessorName);
		
		if (!fIsEclipseNLS || sub.getState() == NLSSubstitution.IGNORED) {
			// Add $NON-NLS-n tag
			String arg= sub.getState() == NLSSubstitution.EXTERNALIZED ? sub.getKey() : sub.getValueNonEmpty();		
			String name= Messages.format(NLSMessages.NLSSourceModifier_add_tag, arg); 
			TextChangeCompatibility.addTextEdit(change, name, createAddTagChange(element));
		}
	}

	private void addAccessor(NLSSubstitution sub, TextChange change, String accessorName) {
		if (sub.getState() == NLSSubstitution.EXTERNALIZED) {
			NLSElement element= sub.getNLSElement();
			Region position= element.getPosition();
			String[] args= {sub.getValueNonEmpty(), sub.getKey()};
			String text= Messages.format(NLSMessages.NLSSourceModifier_externalize, args); 

			String resourceGetter= createResourceGetter(sub.getKey(), accessorName);
			
			TextEdit edit= new ReplaceEdit(position.getOffset(), position.getLength(), resourceGetter);
			if (fIsEclipseNLS && element.getTagPosition() != null) {
				MultiTextEdit multiEdit= new MultiTextEdit();
				multiEdit.addChild(edit);
				Region tagPosition= element.getTagPosition();
				multiEdit.addChild(new DeleteEdit(tagPosition.getOffset(), tagPosition.getLength()));
				edit= multiEdit;
			}
			TextChangeCompatibility.addTextEdit(change, text, edit);
		}
	}

	private TextEdit createAddTagChange(NLSElement element) {
		int offset= element.getTagPosition().getOffset(); //to be changed
		String text= ' ' + element.getTagText(); 
		return new InsertEdit(offset, text);
	}

	private String createResourceGetter(String key, String accessorName) {
		StringBuffer buf= new StringBuffer();
		buf.append(accessorName);
		buf.append('.');
		
		if (fIsEclipseNLS)
			buf.append(key);
		else {
			//we just replace the first occurrence of KEY in the pattern
			int i= fSubstitutionPattern.indexOf(NLSRefactoring.KEY);
			if (i != -1) {
				buf.append(fSubstitutionPattern.substring(0, i));
				buf.append('"').append(key).append('"');
				buf.append(fSubstitutionPattern.substring(i + NLSRefactoring.KEY.length()));
			}
		}
		return buf.toString();
	}
}
