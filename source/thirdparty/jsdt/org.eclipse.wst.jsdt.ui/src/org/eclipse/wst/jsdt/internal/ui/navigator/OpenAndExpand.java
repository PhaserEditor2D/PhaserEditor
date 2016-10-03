/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.navigator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.wst.jsdt.ui.actions.OpenAction;
import org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction;

public class OpenAndExpand extends SelectionDispatchAction implements IAction {

	private OpenAction fOpenAction;
	private TreeViewer fViewer;

	public OpenAndExpand(IWorkbenchSite site, OpenAction openAction, TreeViewer viewer) {
		super(site);
		fOpenAction = openAction;
		fViewer = viewer;
	}

	public void run() {
		fOpenAction.run();
		if(getSelection() != null && getSelection() instanceof IStructuredSelection)
			expand(((IStructuredSelection)getSelection()).getFirstElement());
		
	}

	public void run(ISelection selection) {
		fOpenAction.run(selection);
		if(selection != null && selection instanceof IStructuredSelection)
			expand(((IStructuredSelection)selection).getFirstElement());
	}

	public void run(IStructuredSelection selection) {
		fOpenAction.run(selection);
		if(selection != null)
			expand(selection.getFirstElement());
	}

	public void run(ITextSelection selection) {
		fOpenAction.run(selection);
	}

	public void run(JavaTextSelection selection) {
		fOpenAction.run(selection);
	}

	public void run(Object[] elements) {
		fOpenAction.run(elements);		
	}

	public void runWithEvent(Event event) {
		fOpenAction.runWithEvent(event);
	} 
	
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		fOpenAction.addPropertyChangeListener(listener);
	}

	public boolean equals(Object obj) {
		return fOpenAction.equals(obj);
	}

	public int getAccelerator() {
		return fOpenAction.getAccelerator();
	}

	public String getActionDefinitionId() {
		return fOpenAction.getActionDefinitionId();
	}

	public String getDescription() {
		return fOpenAction.getDescription();
	}

	public ImageDescriptor getDisabledImageDescriptor() {
		return fOpenAction.getDisabledImageDescriptor();
	}

	public Object getElementToOpen(Object object) throws JavaScriptModelException {
		return fOpenAction.getElementToOpen(object);
	}

	public HelpListener getHelpListener() {
		return fOpenAction.getHelpListener();
	}

	public ImageDescriptor getHoverImageDescriptor() {
		return fOpenAction.getHoverImageDescriptor();
	}

	public String getId() {
		return fOpenAction.getId();
	}

	public ImageDescriptor getImageDescriptor() {
		return fOpenAction.getImageDescriptor();
	}

	public IMenuCreator getMenuCreator() {
		return fOpenAction.getMenuCreator();
	}

	public ISelection getSelection() {
		return fOpenAction.getSelection();
	}

	public ISelectionProvider getSelectionProvider() {
		return fOpenAction.getSelectionProvider();
	}

	public Shell getShell() {
		return fOpenAction.getShell();
	}

	public IWorkbenchSite getSite() {
		return fOpenAction.getSite();
	}

	public int getStyle() {
		return fOpenAction.getStyle();
	}

	public String getText() {
		return fOpenAction.getText();
	}

	public String getToolTipText() {
		return fOpenAction.getToolTipText();
	}

	public int hashCode() {
		return fOpenAction.hashCode();
	}

	public boolean isChecked() {
		return fOpenAction.isChecked();
	}

	public boolean isEnabled() {
		return fOpenAction.isEnabled();
	}

	public boolean isHandled() {
		return fOpenAction.isHandled();
	}

	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		fOpenAction.removePropertyChangeListener(listener);
	}

	public void selectionChanged(ISelection selection) {
		fOpenAction.selectionChanged(selection);
	}

	public void selectionChanged(IStructuredSelection selection) {
		fOpenAction.selectionChanged(selection);
	}

	public void selectionChanged(ITextSelection selection) {
		fOpenAction.selectionChanged(selection);
	}

	public void selectionChanged(JavaTextSelection selection) {
		fOpenAction.selectionChanged(selection);
	}

	public void selectionChanged(SelectionChangedEvent event) {
		fOpenAction.selectionChanged(event);
	}

	public void setAccelerator(int keycode) {
		fOpenAction.setAccelerator(keycode);
	}

	public void setActionDefinitionId(String id) {
		fOpenAction.setActionDefinitionId(id);
	}

	public void setChecked(boolean checked) {
		fOpenAction.setChecked(checked);
	}

	public void setDescription(String text) {
		fOpenAction.setDescription(text);
	}

	public void setDisabledImageDescriptor(ImageDescriptor newImage) {
		fOpenAction.setDisabledImageDescriptor(newImage);
	}

	public void setEnabled(boolean enabled) {
		fOpenAction.setEnabled(enabled);
	}

	public void setHelpListener(HelpListener listener) {
		fOpenAction.setHelpListener(listener);
	}

	public void setHoverImageDescriptor(ImageDescriptor newImage) {
		fOpenAction.setHoverImageDescriptor(newImage);
	}

	public void setId(String id) {
		fOpenAction.setId(id);
	}

	public void setImageDescriptor(ImageDescriptor newImage) {
		fOpenAction.setImageDescriptor(newImage);
	}

	public void setMenuCreator(IMenuCreator creator) {
		fOpenAction.setMenuCreator(creator);
	}

	public void setSpecialSelectionProvider(ISelectionProvider provider) {
		fOpenAction.setSpecialSelectionProvider(provider);
	}

	public void setText(String text) {
		fOpenAction.setText(text);
	}

	public void setToolTipText(String toolTipText) {
		fOpenAction.setToolTipText(toolTipText);
	}

	public String toString() {
		return fOpenAction.toString();
	}

	public void update(ISelection selection) {
		fOpenAction.update(selection);
	}

	private void expand(Object target) {
		if (! fOpenAction.isEnabled())
			fViewer.setExpandedState(target, !fViewer.getExpandedState(target));
	}

	 
}
