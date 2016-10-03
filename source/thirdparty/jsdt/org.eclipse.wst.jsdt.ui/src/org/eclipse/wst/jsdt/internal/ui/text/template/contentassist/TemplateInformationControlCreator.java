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
package org.eclipse.wst.jsdt.internal.ui.text.template.contentassist;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlCreatorExtension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.jsdt.internal.ui.text.java.hover.SourceViewerInformationControl;


final public class TemplateInformationControlCreator implements IInformationControlCreator, IInformationControlCreatorExtension {

	private SourceViewerInformationControl fControl;

	/**
	 * The orientation to be used by this hover.
	 * Allowed values are: SWT#RIGHT_TO_LEFT or SWT#LEFT_TO_RIGHT
	 * 
	 */
	private int fOrientation;

	/**
	 * @param orientation the orientation, allowed values are: SWT#RIGHT_TO_LEFT or SWT#LEFT_TO_RIGHT
	 */
	public TemplateInformationControlCreator(int orientation)  {
		Assert.isLegal(orientation == SWT.RIGHT_TO_LEFT || orientation == SWT.LEFT_TO_RIGHT);
		fOrientation= orientation;
	}

	/*
	 * @see org.eclipse.jface.text.IInformationControlCreator#createInformationControl(org.eclipse.swt.widgets.Shell)
	 */
	public IInformationControl createInformationControl(Shell parent) {
		fControl= new SourceViewerInformationControl(parent, SWT.TOOL | fOrientation, SWT.NONE);
		fControl.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				fControl= null;
			}
		});
		return fControl;
	}

	/*
	 * @see org.eclipse.jface.text.IInformationControlCreatorExtension#canReuse(org.eclipse.jface.text.IInformationControl)
	 */
	public boolean canReuse(IInformationControl control) {
		return fControl == control && fControl != null;
	}

	/*
	 * @see org.eclipse.jface.text.IInformationControlCreatorExtension#canReplace(org.eclipse.jface.text.IInformationControlCreator)
	 */
	public boolean canReplace(IInformationControlCreator creator) {
		return (creator != null && getClass() == creator.getClass());
	}
}
