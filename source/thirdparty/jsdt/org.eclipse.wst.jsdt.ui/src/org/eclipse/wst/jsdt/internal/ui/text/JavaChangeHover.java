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
package org.eclipse.wst.jsdt.internal.ui.text;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineChangeHover;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;

/**
 * A line change hover for Java source code. Adds a custom information control creator returning a
 * source viewer with syntax coloring.
 *
 * 
 */
public class JavaChangeHover extends LineChangeHover  {

	/** The last computed partition type. */
	private String fPartition;
	/** The last created information control. */
	private ChangeHoverInformationControl fInformationControl;
	/** The document partitioning to be used by this hover. */
	private String fPartitioning;
	/** The last created information control. */
	private int fLastScrollIndex= 0;
	
	/**
	 * The orientation to be used by this hover.
	 * Allowed values are: SWT#RIGHT_TO_LEFT or SWT#LEFT_TO_RIGHT
	 * 
	 */
	private int fOrientation;

	/**
	 * Creates a new change hover for the given document partitioning.
	 *
	 * @param partitioning the document partitioning
	 * @param orientation the orientation, allowed values are: SWT#RIGHT_TO_LEFT or SWT#LEFT_TO_RIGHT
	 */
	public JavaChangeHover(String partitioning, int orientation) {
		Assert.isLegal(orientation == SWT.RIGHT_TO_LEFT || orientation == SWT.LEFT_TO_RIGHT);
		fPartitioning= partitioning;
		fOrientation= orientation;
	}

	/*
	 * @see org.eclipse.ui.internal.editors.text.LineChangeHover#formatSource(java.lang.String)
	 */
	protected String formatSource(String content) {
		return content;
	}

	/*
	 * @see org.eclipse.jface.text.source.IAnnotationHoverExtension#getHoverControlCreator()
	 */
	public IInformationControlCreator getHoverControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				int shellStyle= SWT.TOOL | SWT.NO_TRIM | fOrientation;
				fInformationControl= new ChangeHoverInformationControl(parent, shellStyle, SWT.NONE, fPartition, EditorsUI.getTooltipAffordanceString());
				fInformationControl.setHorizontalScrollPixel(fLastScrollIndex);
				return fInformationControl;
			}
		};
	}

	/*
	 * @see org.eclipse.jface.text.information.IInformationProviderExtension2#getInformationPresenterControlCreator()
	 * 
	 */
	public IInformationControlCreator getInformationPresenterControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				int shellStyle= SWT.RESIZE | SWT.TOOL | fOrientation;
				int style= SWT.V_SCROLL | SWT.H_SCROLL;
				fInformationControl= new ChangeHoverInformationControl(parent, shellStyle, style, fPartition, null);
				fInformationControl.setHorizontalScrollPixel(fLastScrollIndex);
				return fInformationControl;
			}
		};
	}
	
	/*
	 * @see org.eclipse.jface.text.source.LineChangeHover#computeLineRange(org.eclipse.jface.text.source.ISourceViewer, int, int, int)
	 */
	protected Point computeLineRange(ISourceViewer viewer, int line, int first, int number) {
		Point lineRange= super.computeLineRange(viewer, line, first, number);
		if (lineRange != null) {
			fPartition= getPartition(viewer, lineRange.x);
		} else {
			fPartition= IDocument.DEFAULT_CONTENT_TYPE;
		}
		fLastScrollIndex= viewer.getTextWidget().getHorizontalPixel();
		if (fInformationControl != null) {
			fInformationControl.setStartingPartitionType(fPartition);
			fInformationControl.setHorizontalScrollPixel(fLastScrollIndex);
		}
		return lineRange;
	}

	/**
	 * Returns the partition type of the document displayed in <code>viewer</code> at <code>startLine</code>.

	 * @param viewer the viewer
	 * @param startLine the line in the viewer
	 * @return the partition type at the start of <code>startLine</code>, or <code>IDocument.DEFAULT_CONTENT_TYPE</code> if none can be detected
	 */
	private String getPartition(ISourceViewer viewer, int startLine) {
		if (viewer == null)
			return null;
		IDocument doc= viewer.getDocument();
		if (doc == null)
			return null;
		if (startLine <= 0)
			return IDocument.DEFAULT_CONTENT_TYPE;
		try {
			ITypedRegion region= TextUtilities.getPartition(doc, fPartitioning, doc.getLineOffset(startLine) - 1, true);
			return region.getType();
		} catch (BadLocationException e) {
		}
		return IDocument.DEFAULT_CONTENT_TYPE;
	}


	/*
	 * @see org.eclipse.jface.text.source.LineChangeHover#getTabReplacement()
	 */
	protected String getTabReplacement() {
		return Character.toString('\t');
	}
}
