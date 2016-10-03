/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.wst.jsdt.core.ElementChangedEvent;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IElementChangedListener;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.buildpath.BuildpathDelta;
import org.eclipse.wst.jsdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement;

//TODO: Use global history
/**
 * On the source page of the JavaScript Settings wizard page this action is run
 */
public class ResetAllAction extends BuildpathModifierAction {
	
	private final HintTextGroup fProvider;
	private final IRunnableContext fContext;
	private IJavaScriptProject fJavaScriptProject;
	private List fEntries;

	public ResetAllAction(HintTextGroup provider, IRunnableContext context, ISetSelectionTarget selectionTarget) {
		super(null, selectionTarget, BuildpathModifierAction.RESET_ALL);
		
		fProvider= provider;
		fContext= context;
		
		setImageDescriptor(JavaPluginImages.DESC_ELCL_CLEAR);
		setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_CLEAR);
		setText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_ClearAll_label);
		setToolTipText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_ClearAll_tooltip);
		setEnabled(false);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getDetailedDescription() {
		return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_ResetAll;
	}

	public void setBreakPoint(IJavaScriptProject javaProject) {
		fJavaScriptProject= javaProject;
		if (fJavaScriptProject.exists()) {
			try {
	            fEntries= ClasspathModifier.getExistingEntries(fJavaScriptProject);
            } catch (JavaScriptModelException e) {
	            JavaScriptPlugin.log(e);
	            return;
            }
			setEnabled(true);
		} else {
			JavaScriptCore.addElementChangedListener(new IElementChangedListener() {
		
				public void elementChanged(ElementChangedEvent event) {
					if (fJavaScriptProject.exists()) {
						try {
	                        fEntries= ClasspathModifier.getExistingEntries(fJavaScriptProject);
                        } catch (JavaScriptModelException e) {
                        	JavaScriptPlugin.log(e);
                        	return;
                        } finally {
							JavaScriptCore.removeElementChangedListener(this);
                        }
						setEnabled(true);
					}					
		        }
				
			}, ElementChangedEvent.POST_CHANGE);
		}
    }
	
	/**
	 * {@inheritDoc}
	 */
	public void run() {

		try {
	        final IRunnableWithProgress runnable= new IRunnableWithProgress() {
	        	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

        			monitor.beginTask("", 3); //$NON-NLS-1$
	        		try {
	        			if (!hasChange(fJavaScriptProject))
	        				return;
	        			
	        			BuildpathDelta delta= new BuildpathDelta(getToolTipText());
	        			
	        			ClasspathModifier.commitClassPath(fEntries, fJavaScriptProject, monitor);
        				delta.setNewEntries((CPListElement[])fEntries.toArray(new CPListElement[fEntries.size()]));
	        			
	        			for (Iterator iterator= fProvider.getCreatedResources().iterator(); iterator.hasNext();) {
	                        IResource resource= (IResource)iterator.next();
	                        resource.delete(false, null);
	                        delta.addDeletedResource(resource);
                        }
	        			
	        			fProvider.resetCreatedResources();
	                    
	                    informListeners(delta);
	                    
	            		selectAndReveal(new StructuredSelection(fJavaScriptProject));
	                } catch (JavaScriptModelException e) {
	                    showExceptionDialog(e, NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_ClearAll_tooltip);
	                } catch (CoreException e) {
	                    showExceptionDialog(e, NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_ClearAll_tooltip);
                    } finally {
	                	monitor.done();
	                }
	        	}
	        };
	        fContext.run(false, false, runnable);
        } catch (InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				showExceptionDialog((CoreException)e.getCause(), NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_ClearAll_tooltip);
			} else {
				JavaScriptPlugin.log(e);
			}
        } catch (InterruptedException e) {
        }
	}
	

	/**
     * {@inheritDoc}
     */
    protected boolean canHandle(IStructuredSelection elements) {
    	if (fJavaScriptProject == null)
    		return false;
    	
	    return true;
    }

	
	//TODO: Remove, action should be disabled if not hasChange
	private boolean hasChange(IJavaScriptProject project) throws JavaScriptModelException {
		IIncludePathEntry[] currentEntries= project.getRawIncludepath();
        if (currentEntries.length != fEntries.size())
            return true;
        
        int i= 0;
        for (Iterator iterator= fEntries.iterator(); iterator.hasNext();) {
	        CPListElement oldEntrie= (CPListElement)iterator.next();
	        if (!oldEntrie.getClasspathEntry().equals(currentEntries[i]))
	        	return true;
	        i++;
        }
        return false;
	}
}
