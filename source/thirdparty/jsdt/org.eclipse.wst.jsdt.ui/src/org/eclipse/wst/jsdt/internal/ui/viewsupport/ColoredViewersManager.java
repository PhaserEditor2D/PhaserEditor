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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.wst.jsdt.internal.ui.preferences.AppearancePreferencePage;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

public class ColoredViewersManager implements IPropertyChangeListener {
	
	public static final String QUALIFIER_COLOR_NAME= "org.eclipse.wst.jsdt.ui.ColoredLabels.qualifier"; //$NON-NLS-1$
	public static final String DECORATIONS_COLOR_NAME= "org.eclipse.wst.jsdt.ui.ColoredLabels.decorations"; //$NON-NLS-1$
	public static final String COUNTER_COLOR_NAME= "org.eclipse.wst.jsdt.ui.ColoredLabels.counter"; //$NON-NLS-1$
	public static final String INHERITED_COLOR_NAME= "org.eclipse.wst.jsdt.ui.ColoredLabels.inherited"; //$NON-NLS-1$
	
	private static ColoredViewersManager fgInstance= new ColoredViewersManager();
	
	private Map fManagedViewers;
	private ColorRegistry fColorRegisty;
	
	public ColoredViewersManager() {
		fManagedViewers= new HashMap();
		fColorRegisty= JFaceResources.getColorRegistry();
	}
	
	public void installColoredLabels(StructuredViewer viewer) {
		if (fManagedViewers.containsKey(viewer)) {
			return; // already installed
		}
		if (fManagedViewers.isEmpty()) {
			// first viewer installed
			PreferenceConstants.getPreferenceStore().addPropertyChangeListener(this);
			fColorRegisty.addListener(this);
		}
		fManagedViewers.put(viewer, new ManagedViewer(viewer));
	}
	
	
	public void uninstallColoredLabels(StructuredViewer viewer) {
		ManagedViewer mv= (ManagedViewer) fManagedViewers.remove(viewer);
		if (mv == null)
			return; // not installed
		
		if (fManagedViewers.isEmpty()) {
			PreferenceConstants.getPreferenceStore().removePropertyChangeListener(this);
			fColorRegisty.removeListener(this);
			// last viewer uninstalled
		}
	}
	
	public Color getColorForName(String symbolicName) {
		return fColorRegisty.get(symbolicName);
	}
	
		
	public void propertyChange(PropertyChangeEvent event) {
		String property= event.getProperty();
		if (property.equals(QUALIFIER_COLOR_NAME) || property.equals(COUNTER_COLOR_NAME) || property.equals(DECORATIONS_COLOR_NAME)
				|| property.equals(AppearancePreferencePage.PREF_COLORED_LABELS)) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					refreshAllViewers();
				}
			});
		}
	}
	
	protected final void refreshAllViewers() {
		for (Iterator iterator= fManagedViewers.values().iterator(); iterator.hasNext();) {
			ManagedViewer viewer= (ManagedViewer) iterator.next();
			viewer.refresh();
		}
	}
	
	private class ManagedViewer implements DisposeListener {
		
		private static final String COLORED_LABEL_KEY= "coloredlabel"; //$NON-NLS-1$
		
		private StructuredViewer fViewer;
		private OwnerDrawSupport fOwnerDrawSupport;
		
		private ManagedViewer(StructuredViewer viewer) {
			fViewer= viewer;
			fOwnerDrawSupport= null;
			fViewer.getControl().addDisposeListener(this);
			if (showColoredLabels()) {
				installOwnerDraw();
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
		 */
		public void widgetDisposed(DisposeEvent e) {
			uninstallColoredLabels(fViewer);
		}
		
		public final void refresh() {
			Control control= fViewer.getControl();
			if (!control.isDisposed()) {
				if (showColoredLabels()) {
					installOwnerDraw();
				} else {
					uninstallOwnerDraw();
				}
			}
		}
		
		protected void installOwnerDraw() {
			if (fOwnerDrawSupport == null) {
				// not yet installed
				fOwnerDrawSupport= new OwnerDrawSupport(fViewer.getControl()) { // will install itself as listeners
					public ColoredString getColoredLabel(Item item) {
						return getColoredLabelForView(item);
					}
	
					public Color getColor(String foregroundColorName, Display display) {
						return getColorForName(foregroundColorName);
					}
				};
			}
			refreshViewer();
		}
		
		protected void uninstallOwnerDraw() {
			if (fOwnerDrawSupport == null)
				return; // not installed

			fOwnerDrawSupport.dispose(); // removes itself as listener
			fOwnerDrawSupport= null;
			refreshViewer();
		}
		
		private void refreshViewer() {
			Control control= fViewer.getControl();
			if (!control.isDisposed()) {
				if (control instanceof Tree) {
					refresh(((Tree) control).getItems());
				} else if (control instanceof Table) {
					refresh(((Table) control).getItems());
				}
			}
		}

		private void refresh(Item[] items) {
			for (int i= 0; i < items.length; i++) {
				Item item= items[i];
				item.setData(COLORED_LABEL_KEY, null);
				String text= item.getText();
				item.setText(""); //$NON-NLS-1$
				item.setText(text);
				if (item instanceof TreeItem) {
					refresh(((TreeItem) item).getItems());
				}
			}
		}
		
		private ColoredString getColoredLabelForView(Item item) {
			ColoredString oldLabel= (ColoredString) item.getData(COLORED_LABEL_KEY);
			String itemText= item.getText();
			if (oldLabel != null && oldLabel.getString().equals(itemText)) {
				// avoid accesses to the label provider if possible
				return oldLabel;
			}
			ColoredString newLabel= null;
			IBaseLabelProvider labelProvider= fViewer.getLabelProvider();
			if (labelProvider instanceof IRichLabelProvider) {
				newLabel= ((IRichLabelProvider) labelProvider).getRichTextLabel(item.getData());
			}
			if (newLabel == null) {
				newLabel= new ColoredString(itemText); // fallback. Should never happen.
			} else if (!newLabel.getString().equals(itemText)) {
				// the decorator manager has already queued an new update 
				newLabel= ColoredJavaElementLabels.decorateColoredString(newLabel, itemText, ColoredJavaElementLabels.DECORATIONS_STYLE);
			}
			item.setData(COLORED_LABEL_KEY, newLabel); // cache the result
			return newLabel;
		}

	}
	
	public static boolean showColoredLabels() {
		String preference= PreferenceConstants.getPreference(AppearancePreferencePage.PREF_COLORED_LABELS, null);
		return preference != null && Boolean.valueOf(preference).booleanValue();
	}
	
	public static void install(StructuredViewer viewer) {
		fgInstance.installColoredLabels(viewer);
	}
	
	
}
