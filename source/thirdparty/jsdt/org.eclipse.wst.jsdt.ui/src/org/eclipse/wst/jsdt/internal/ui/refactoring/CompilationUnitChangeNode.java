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
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;
import org.eclipse.ltk.ui.refactoring.LanguageElementNode;
import org.eclipse.ltk.ui.refactoring.TextEditChangeNode;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class CompilationUnitChangeNode extends TextEditChangeNode {

	static final ChildNode[] EMPTY_CHILDREN= new ChildNode[0];
	
	private static class JavaLanguageNode extends LanguageElementNode {

		private IJavaScriptElement fJavaElement;
		private static JavaElementImageProvider fgImageProvider= new JavaElementImageProvider();

		public JavaLanguageNode(TextEditChangeNode parent, IJavaScriptElement element) {
			super(parent);
			fJavaElement= element;
			Assert.isNotNull(fJavaElement);
		}
		
		public JavaLanguageNode(ChildNode parent, IJavaScriptElement element) {
			super(parent);
			fJavaElement= element;
			Assert.isNotNull(fJavaElement);
		}
		
		public String getText() {
			return JavaScriptElementLabels.getElementLabel(fJavaElement, JavaScriptElementLabels.ALL_DEFAULT);
		}
		
		public ImageDescriptor getImageDescriptor() {
			return fgImageProvider.getJavaImageDescriptor(
				fJavaElement, 
				JavaElementImageProvider.OVERLAY_ICONS | JavaElementImageProvider.SMALL_ICONS);
		}
		
		public IRegion getTextRange() throws CoreException {
			ISourceRange range= ((ISourceReference)fJavaElement).getSourceRange();
			return new Region(range.getOffset(), range.getLength());
		}	
	}	
	
	public CompilationUnitChangeNode(TextEditBasedChange change) {
		super(change);
	}
	
	protected ChildNode[] createChildNodes() {
		final TextEditBasedChange change= getTextEditBasedChange();
		IJavaScriptUnit cunit= (IJavaScriptUnit) change.getAdapter(IJavaScriptUnit.class);
		if (cunit != null) {
			List children= new ArrayList(5);
			Map map= new HashMap(20);
			TextEditBasedChangeGroup[] changes= getSortedChangeGroups(change);
			for (int i= 0; i < changes.length; i++) {
				TextEditBasedChangeGroup tec= changes[i];
				try {
					IJavaScriptElement element= getModifiedJavaElement(tec, cunit);
					if (element.equals(cunit)) {
						children.add(createTextEditGroupNode(this, tec));
					} else {
						JavaLanguageNode pjce= getChangeElement(map, element, children, this);
						pjce.addChild(createTextEditGroupNode(pjce, tec));
					}
				} catch (JavaScriptModelException e) {
					children.add(createTextEditGroupNode(this, tec));
				}
			}
			return (ChildNode[]) children.toArray(new ChildNode[children.size()]);
		} else {
			return EMPTY_CHILDREN;
		}
	}
	
	private static class OffsetComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			TextEditBasedChangeGroup c1= (TextEditBasedChangeGroup)o1;
			TextEditBasedChangeGroup c2= (TextEditBasedChangeGroup)o2;
			int p1= getOffset(c1);
			int p2= getOffset(c2);
			if (p1 < p2)
				return -1;
			if (p1 > p2)
				return 1;
			// same offset
			return 0;	
		}
		private int getOffset(TextEditBasedChangeGroup edit) {
			return edit.getRegion().getOffset();
		}
	}
	
	private TextEditBasedChangeGroup[] getSortedChangeGroups(TextEditBasedChange change) {
		TextEditBasedChangeGroup[] edits= change.getChangeGroups();
		List result= new ArrayList(edits.length);
		for (int i= 0; i < edits.length; i++) {
			if (!edits[i].getTextEditGroup().isEmpty())
				result.add(edits[i]);
		}
		Comparator comparator= new OffsetComparator();
		Collections.sort(result, comparator);
		return (TextEditBasedChangeGroup[])result.toArray(new TextEditBasedChangeGroup[result.size()]);
	}
	
	private IJavaScriptElement getModifiedJavaElement(TextEditBasedChangeGroup edit, IJavaScriptUnit cunit) throws JavaScriptModelException {
		IRegion range= edit.getRegion();
		if (range.getOffset() == 0 && range.getLength() == 0)
			return cunit;
		IJavaScriptElement result= cunit.getElementAt(range.getOffset());
		if (result == null)
			return cunit;
		
		try {
			while(true) {
				ISourceReference ref= (ISourceReference)result;
				IRegion sRange= new Region(ref.getSourceRange().getOffset(), ref.getSourceRange().getLength());
				if (result.getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT || result.getParent() == null || coveredBy(edit, sRange))
					break;
				result= result.getParent();
			}
		} catch(JavaScriptModelException e) {
			// Do nothing, use old value.
		} catch(ClassCastException e) {
			// Do nothing, use old value.
		}
		return result;
	}
	
	private JavaLanguageNode getChangeElement(Map map, IJavaScriptElement element, List children, TextEditChangeNode cunitChange) {
		JavaLanguageNode result= (JavaLanguageNode)map.get(element);
		if (result != null)
			return result;
		IJavaScriptElement parent= element.getParent();
		if (parent instanceof IJavaScriptUnit) {
			result= new JavaLanguageNode(cunitChange, element);
			children.add(result);
			map.put(element, result);
		} else {
			JavaLanguageNode parentChange= getChangeElement(map, parent, children, cunitChange);
			result= new JavaLanguageNode(parentChange, element);
			parentChange.addChild(result);
			map.put(element, result);
		}
		return result;
	}
	
	private boolean coveredBy(TextEditBasedChangeGroup group, IRegion sourceRegion) {
		int sLength= sourceRegion.getLength();
		if (sLength == 0)
			return false;
		int sOffset= sourceRegion.getOffset();
		int sEnd= sOffset + sLength - 1;
		TextEdit[] edits= group.getTextEdits();
		for (int i= 0; i < edits.length; i++) {
			TextEdit edit= edits[i];
			if (edit.isDeleted())
				return false;
			int rOffset= edit.getOffset();
			int rLength= edit.getLength();
			int rEnd= rOffset + rLength - 1;
		    if (rLength == 0) {
				if (!(sOffset < rOffset && rOffset <= sEnd))
					return false;
			} else {
				if (!(sOffset <= rOffset && rEnd <= sEnd))
					return false;
			}
		}
		return true;
	}
}
