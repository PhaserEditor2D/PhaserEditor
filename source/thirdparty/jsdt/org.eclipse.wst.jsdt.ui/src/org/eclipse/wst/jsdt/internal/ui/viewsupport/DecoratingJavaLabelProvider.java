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
package org.eclipse.wst.jsdt.internal.ui.viewsupport;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelDecorator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.internal.ui.packageview.HierarchicalDecorationContext;
import org.eclipse.wst.jsdt.ui.ProblemsLabelDecorator;

public class DecoratingJavaLabelProvider extends DecoratingLabelProvider implements IRichLabelProvider {
	
	/**
	 * Decorating label provider for Java. Combines a JavaUILabelProvider
	 * with problem and override indicator with the workbench decorator (label
	 * decorator extension point).
	 * @param labelProvider the label provider to decorate
	 */
	public DecoratingJavaLabelProvider(JavaUILabelProvider labelProvider) {
		this(labelProvider, true);
	}

	/**
	 * Decorating label provider for Java. Combines a JavaUILabelProvider
	 * (if enabled with problem indicator) with the workbench
	 * decorator (label decorator extension point).
	 * 	@param labelProvider the label provider to decorate
	 * @param errorTick show error ticks
	 */
	public DecoratingJavaLabelProvider(JavaUILabelProvider labelProvider, boolean errorTick) {
		this(labelProvider, errorTick, true);
	}
	
	/**
	 * Decorating label provider for Java. Combines a JavaUILabelProvider
	 * (if enabled with problem indicator) with the workbench
	 * decorator (label decorator extension point).
	 * 	@param labelProvider the label provider to decorate
	 * @param errorTick show error ticks
	 * @param flatPackageMode configure flat package mode
	 */
	public DecoratingJavaLabelProvider(JavaUILabelProvider labelProvider, boolean errorTick, boolean flatPackageMode) {
		super(labelProvider, PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());
		if (errorTick) {
			labelProvider.addLabelDecorator(new ProblemsLabelDecorator(null));
		}
		setFlatPackageMode(flatPackageMode);
	}
	
	/**
	 * Tells the label decorator if the view presents packages flat or hierarchical.
	 * @param enable If set, packages are presented in flat mode.
	 */
	public void setFlatPackageMode(boolean enable) {
		if (enable) {
			setDecorationContext(DecorationContext.DEFAULT_CONTEXT);
		} else {
			setDecorationContext(HierarchicalDecorationContext.CONTEXT);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.viewsupport.IRichLabelProvider#getRichTextLabel(Object)
	 */
	public ColoredString getRichTextLabel(Object element) {
		ILabelProvider labelProvider= getLabelProvider();
		if (labelProvider instanceof IRichLabelProvider) {
			// get a rich label from the label decorator
			IRichLabelProvider richLabelProvider= (IRichLabelProvider) labelProvider;
			ColoredString richLabel= richLabelProvider.getRichTextLabel(element);
			if (richLabel != null) {
				String decorated= null;
				ILabelDecorator labelDecorator= getLabelDecorator();
				if (labelDecorator != null) {
					if (labelDecorator instanceof LabelDecorator) {
						decorated= ((LabelDecorator) labelDecorator).decorateText(richLabel.getString(), element, getDecorationContext());
					} else {
						decorated= labelDecorator.decorateText(richLabel.getString(), element);
					}
				}
				if (decorated != null) {
					return ColoredJavaElementLabels.decorateColoredString(richLabel, decorated, ColoredJavaElementLabels.DECORATIONS_STYLE);
				}
				return richLabel;
			}
		}
		return null;
	}

}
