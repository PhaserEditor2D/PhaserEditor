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
package org.eclipse.wst.jsdt.internal.ui.text.java;



import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.text.java.CompletionProposalLabelProvider;


/**
 * Implementation of the <code>IContextInformation</code> interface.
 */
public final class ProposalContextInformation implements IContextInformation, IContextInformationExtension {

	private final String fContextDisplayString;
	private final String fInformationDisplayString;
	private final Image fImage;
	private int fPosition;

	/**
	 * Creates a new context information.
	 */
	public ProposalContextInformation(CompletionProposal proposal) {
		// don't cache the core proposal because the ContentAssistant might
		// hang on to the context info.
		CompletionProposalLabelProvider labelProvider= new CompletionProposalLabelProvider();
		fInformationDisplayString= labelProvider.createParameterList(proposal);
		ImageDescriptor descriptor= labelProvider.createImageDescriptor(proposal);
		if (descriptor != null)
			fImage= JavaScriptPlugin.getImageDescriptorRegistry().get(descriptor);
		else
			fImage= null;
		if (proposal.getCompletion().length == 0)
			fPosition= proposal.getCompletionLocation() + 1;
		else
			fPosition= -1;
		fContextDisplayString= labelProvider.createLabel(proposal);
	}

	/*
	 * @see IContextInformation#equals
	 */
	public boolean equals(Object object) {
		if (object instanceof IContextInformation) {
			IContextInformation contextInformation= (IContextInformation) object;
			boolean equals= getInformationDisplayString().equalsIgnoreCase(contextInformation.getInformationDisplayString());
			if (getContextDisplayString() != null)
				equals= equals && getContextDisplayString().equalsIgnoreCase(contextInformation.getContextDisplayString());
			return equals;
		}
		return false;
	}

	/*
	 * @see IContextInformation#getInformationDisplayString()
	 */
	public String getInformationDisplayString() {
		return fInformationDisplayString;
	}

	/*
	 * @see IContextInformation#getImage()
	 */
	public Image getImage() {
		return fImage;
	}

	/*
	 * @see IContextInformation#getContextDisplayString()
	 */
	public String getContextDisplayString() {
		return fContextDisplayString;
	}

	/*
	 * @see IContextInformationExtension#getContextInformationPosition()
	 */
	public int getContextInformationPosition() {
		return fPosition;
	}
	
	/**
	 * Sets the context information position.
	 * 
	 * @param position the new position, or -1 for unknown.
	 * 
	 */
	public void setContextInformationPosition(int position) {
		fPosition= position;
	}
}
