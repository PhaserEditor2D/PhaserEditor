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
package org.eclipse.wst.jsdt.internal.ui.refactoring.nls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabelProvider;


public class PackageBrowseAdapter implements IStringButtonAdapter {
	
    PackageSelectionDialogButtonField  fReceiver;
    private IJavaScriptUnit fCu;
    
    public PackageBrowseAdapter(IJavaScriptUnit unit) {
        fCu = unit;
    }
    
    public void setReceiver(PackageSelectionDialogButtonField  receiver) {
       fReceiver = receiver;
    }
    
	public void changeControlPressed(DialogField field) {
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(
			Display.getCurrent().getActiveShell(), new JavaScriptElementLabelProvider());
        dialog.setIgnoreCase(false);
        dialog.setTitle(NLSUIMessages.PackageBrowseAdapter_package_selection); 
        dialog.setMessage(NLSUIMessages.PackageBrowseAdapter_choose_package); 
        dialog.setElements(createPackageListInput(fCu, null));
        if (dialog.open() == Window.OK) { 
        	IPackageFragment selectedPackage= (IPackageFragment)dialog.getFirstResult();
        	if (selectedPackage != null) {
        		fReceiver.setPackage(selectedPackage);
        	}						
        }
	}
	public static Object[] createPackageListInput(IJavaScriptUnit cu, String elementNameMatch){
		try{
			IJavaScriptProject project= cu.getJavaScriptProject();
			IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
			List result= new ArrayList();
			HashMap entered =new HashMap();
			for (int i= 0; i < roots.length; i++){
				if (canAddPackageRoot(roots[i])){
					getValidPackages(roots[i], result, entered, elementNameMatch);
				}	
			}
			return result.toArray();
		} catch (JavaScriptModelException e){
			JavaScriptPlugin.log(e);
			return new Object[0];
		}
	}

    static boolean canAddPackageRoot(IPackageFragmentRoot root) throws JavaScriptModelException{
    	if (! root.exists())
    		return false;
    	if (root.isArchive())	
    		return false;
    	if (root.isExternal())
    		return false;
    	if (root.isReadOnly())		
    		return false;
    	if (! root.isStructureKnown())	
    		return false;
    	return true;	
    }
	
	static void getValidPackages(IPackageFragmentRoot root, List result, HashMap entered, String elementNameMatch) throws JavaScriptModelException {
		IJavaScriptElement[] children= null;
		try {
			children= root.getChildren();
		} catch (JavaScriptModelException e){
			return;
		}	
		for (int i= 0; i < children.length; i++){
            if (children[i] instanceof IPackageFragment) {
                IPackageFragment packageFragment = (IPackageFragment)children[i];
                String packageName = packageFragment.getElementName();
                
                if ((entered != null) && (entered.containsKey(packageName)) == true) {
                    continue;
                }
                
			    if (canAddPackage(packageFragment)) {
			        if ((elementNameMatch == null) || (elementNameMatch.equals(packageName))) {
			            result.add(packageFragment);
			            if (entered != null) {
			                entered.put(packageName, null);
			            }
			        }
			    }
            }
		}
	}

    static boolean canAddPackage(IPackageFragment p) throws JavaScriptModelException{ 
    	if (! p.exists())
    		return false;
    	if (p.isReadOnly())
    		return false;
    	if (! p.isStructureKnown())
    		return false;
    	return true;	
    }

    public static List searchAllPackages(IJavaScriptProject project, String matcher) {
		try{
			IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
			List result= new ArrayList();
			for (int i= 0; i < roots.length; i++){
				if (canAddPackageRoot(roots[i])){
					getValidPackages(roots[i], result, null, matcher);
				}	
			}
			return result;
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
			return new ArrayList(0);
		}
    }
}
