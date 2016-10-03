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
package org.eclipse.wst.jsdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class LinkedProposalPositionGroup {
	
	/**
	 * {@link LinkedProposalPositionGroup.PositionInformation} describes a position
	 * insinde a position group. The information provided must be accurate
	 * after the document change to the proposal has been performed, but doesn't
	 * need to reflect the changed done by the linking mode.
	 */
	public static abstract class PositionInformation {
		public abstract int getOffset();
		public abstract int getLength();
		public abstract int getSequenceRank();
	}
	
	public static class Proposal {
		
		private String fDisplayString;
		private Image fImage;
		private int fRelevance;

		public Proposal(String displayString, Image image, int relevance) {
			fDisplayString= displayString;
			fImage= image;
			fRelevance= relevance;
		}
		
		public String getDisplayString() {
			return fDisplayString;
		}
		
		public Image getImage() {
			return fImage;
		}
		
		public int getRelevance() {
			return fRelevance;
		}
				
		public void setImage(Image image) {
			fImage= image;
		}
		
		public String getAdditionalProposalInfo() {
			return null;
		}
				
		public TextEdit computeEdits(int offset, LinkedPosition position, char trigger, int stateMask, LinkedModeModel model) throws CoreException {
			return new ReplaceEdit(position.getOffset(), position.getLength(), fDisplayString);
		}
	}	
	
	public static PositionInformation createPositionInformation(ITrackedNodePosition pos, boolean isFirst) {
		return new TrackedNodePosition(pos, isFirst);
	}
	
	private static class TrackedNodePosition extends PositionInformation {
		
		private final ITrackedNodePosition fPos;
		private final boolean fIsFirst;
		
		public TrackedNodePosition(ITrackedNodePosition pos, boolean isFirst) {
			fPos= pos;
			fIsFirst= isFirst;
		}

		public int getOffset() {
			return fPos.getStartPosition();
		}
		
		public int getLength() {
			return fPos.getLength();
		}
		
		public int getSequenceRank() {
			return fIsFirst ? 0 : 1;
		}
	}
	
	private static final class JavaLinkedModeProposal extends Proposal {
		private final ITypeBinding fTypeProposal;
		private final IJavaScriptUnit fCompilationUnit;

		public JavaLinkedModeProposal(IJavaScriptUnit unit, ITypeBinding typeProposal, int relevance) {
			super(BindingLabelProvider.getBindingLabel(typeProposal, JavaScriptElementLabels.ALL_DEFAULT | JavaScriptElementLabels.ALL_POST_QUALIFIED), null, relevance);
			fTypeProposal= typeProposal;
			fCompilationUnit= unit;
			ImageDescriptor desc= BindingLabelProvider.getBindingImageDescriptor(fTypeProposal, BindingLabelProvider.DEFAULT_IMAGEFLAGS);
			if (desc != null) {
				setImage(JavaScriptPlugin.getImageDescriptorRegistry().get(desc));
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.corext.fix.PositionGroup.Proposal#computeEdits(int, org.eclipse.jface.text.link.LinkedPosition, char, int, org.eclipse.jface.text.link.LinkedModeModel)
		 */
		public TextEdit computeEdits(int offset, LinkedPosition position, char trigger, int stateMask, LinkedModeModel model) throws CoreException {
			ImportRewrite impRewrite= StubUtility.createImportRewrite(fCompilationUnit, true);
			String replaceString= impRewrite.addImport(fTypeProposal);
				
			MultiTextEdit composedEdit= new MultiTextEdit();
			composedEdit.addChild(new ReplaceEdit(position.getOffset(), position.getLength(), replaceString));
			composedEdit.addChild(impRewrite.rewriteImports(null));
			return composedEdit;
		}
	}
		

	private final String fGroupId;
	private final List/*<Position>*/ fPositions;
	private final List/*<Proposal>*/ fProposals;
	

	public LinkedProposalPositionGroup(String groupID) {
		fGroupId= groupID;
		fPositions= new ArrayList();
		fProposals= new ArrayList();
	}
	
	public void addPosition(PositionInformation position) {
		fPositions.add(position);
	}
	
	public void addProposal(Proposal proposal) {
		fProposals.add(proposal);
	}
	
	
	public void addPosition(ITrackedNodePosition position, boolean isFirst) {
		addPosition(createPositionInformation(position, isFirst));
	}
	
	public void addProposal(String displayString, Image image, int relevance) {
		addProposal(new Proposal(displayString, image, relevance));
	}
	
	public void addProposal(ITypeBinding type, IJavaScriptUnit cu, int relevance) {
		addProposal(new JavaLinkedModeProposal(cu, type, relevance));
	}
	
	public String getGroupId() {
		return fGroupId;
	}

	public PositionInformation[] getPositions() {
		return (PositionInformation[])fPositions.toArray(new PositionInformation[fPositions.size()]);
	}

	public Proposal[] getProposals() {
		return (Proposal[])fProposals.toArray(new Proposal[fProposals.size()]);
	}
	
}
