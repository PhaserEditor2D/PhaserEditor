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

import java.util.Arrays;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.IExtensionActivationListener;
import org.eclipse.ui.navigator.IExtensionStateModel;
import org.eclipse.ui.navigator.INavigatorActivationService;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.navigator.IExtensionStateConstants.Values;

/**
 * Contributes the following actions to the menu on behalf of the JDT content
 * extension.
 * 
 * <ul>
 * <li>{@link CommonLayoutActionGroup}. Contributes the "Package Presentation>" submenu in the View's drop down menu (not right-click).</li>
 * </ul>
 */
public class JavaNavigatorViewActionProvider extends CommonActionProvider {

	private static final int HIERARCHICAL_LAYOUT= 0x1;

	private static final int FLAT_LAYOUT= 0x2;

	private static final String TAG_LAYOUT= "org.eclipse.wst.jsdt.internal.ui.navigator.layout"; //$NON-NLS-1$ 

	private IExtensionStateModel fStateModel;

	private CommonLayoutActionGroup fLayoutActionGroup;

	private ICommonActionExtensionSite fExtensionSite;

	private String fExtensionId;

	private IActionBars fActionBars;

	private boolean fEnabled= false;

	private IExtensionActivationListener fMenuUpdater= new IExtensionActivationListener() {

		public void onExtensionActivation(String viewerId, String[] theNavigatorExtensionIds, boolean isCurrentlyActive) {

			if (fExtensionSite != null && fActionBars != null) {

				int search= Arrays.binarySearch(theNavigatorExtensionIds, fExtensionId);
				if (search > -1) {
					if (isMyViewer(viewerId)) {
						if (wasEnabled(isCurrentlyActive))
							fLayoutActionGroup.fillActionBars(fActionBars);

						else
							if (wasDisabled(isCurrentlyActive))
								fLayoutActionGroup.unfillActionBars(fActionBars);
						// else no change 
					}
					fEnabled= isCurrentlyActive;
				}
			}

		}

		private boolean isMyViewer(String viewerId) {
			String myViewerId= fExtensionSite.getViewSite().getId();
			return myViewerId != null && myViewerId.equals(viewerId);
		}

		private boolean wasDisabled(boolean isActive) {
			return fEnabled && !isActive;
		}

		private boolean wasEnabled(boolean isActive) {
			return !fEnabled && isActive;
		}
	};


	public void fillActionBars(IActionBars actionBars) {
		fActionBars= actionBars;
		fLayoutActionGroup.fillActionBars(actionBars);
	}

	public void init(ICommonActionExtensionSite site) {

		fExtensionSite= site;

		fStateModel= fExtensionSite.getExtensionStateModel();
		fLayoutActionGroup= new CommonLayoutActionGroup(fExtensionSite.getStructuredViewer(), fStateModel);

		INavigatorActivationService activationService= fExtensionSite.getContentService().getActivationService();
		activationService.addExtensionActivationListener(fMenuUpdater);

		fExtensionId= fExtensionSite.getExtensionId();

		fEnabled= true;

	}

	public void dispose() {
		super.dispose();
		fExtensionSite.getContentService().getActivationService().removeExtensionActivationListener(fMenuUpdater);
	}

	public void setContext(ActionContext context) {
		super.setContext(context);
	}

	public void restoreState(IMemento memento) {
		boolean isCurrentLayoutFlat= false;
		Integer state= null;
		if (memento != null)
			state= memento.getInteger(TAG_LAYOUT);

		// If no memento try an restore from preference store
		if (state == null) {
			IPreferenceStore store= JavaScriptPlugin.getDefault().getPreferenceStore();
			state= Integer.valueOf(store.getInt(TAG_LAYOUT));
		}

		if (state.intValue() == FLAT_LAYOUT)
			isCurrentLayoutFlat= true;
		else
			if (state.intValue() == HIERARCHICAL_LAYOUT)
				isCurrentLayoutFlat= false;

		fStateModel.setBooleanProperty(Values.IS_LAYOUT_FLAT, isCurrentLayoutFlat);
		fLayoutActionGroup.setFlatLayout(isCurrentLayoutFlat);
	}

	public void saveState(IMemento aMemento) {
		super.saveState(aMemento);
		IPreferenceStore store= JavaScriptPlugin.getDefault().getPreferenceStore();
		if (fStateModel.getBooleanProperty(Values.IS_LAYOUT_FLAT))
			store.setValue(TAG_LAYOUT, FLAT_LAYOUT);
		else
			store.setValue(TAG_LAYOUT, HIERARCHICAL_LAYOUT);

	}
}
