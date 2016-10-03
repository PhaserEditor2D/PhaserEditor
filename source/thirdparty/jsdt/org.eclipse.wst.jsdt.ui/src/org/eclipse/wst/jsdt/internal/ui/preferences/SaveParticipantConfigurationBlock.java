/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.saveparticipant.ISaveParticipantPreferenceConfiguration;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.saveparticipant.SaveParticipantDescriptor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.saveparticipant.SaveParticipantRegistry;

import com.ibm.icu.text.Collator;

/**
 * Configures Java Editor save participants.
 * 
 * 
 */
class SaveParticipantConfigurationBlock implements IPreferenceAndPropertyConfigurationBlock {
	
	private interface IDelegateOperation {
		public void run(ISaveParticipantPreferenceConfiguration block);
	}
	
	private final PreferencePage fPreferencePage;
	private final IScopeContext fContext;
	private final ArrayList fConfigurations;
	
	public SaveParticipantConfigurationBlock(IScopeContext context, PreferencePage preferencePage) {
		Assert.isNotNull(context);
		Assert.isNotNull(preferencePage);
		
		fContext= context;
		fPreferencePage= preferencePage;
		fConfigurations= new ArrayList();
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.IPreferenceConfigurationBlock#createControl(org.eclipse.swt.widgets.Composite)
	 * 
	 */
	public Control createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    	GridLayout gridLayout= new GridLayout(2, false);
    	gridLayout.marginHeight= 0;
    	gridLayout.marginWidth= 0;
		composite.setLayout(gridLayout);
		
		SaveParticipantRegistry registry= JavaScriptPlugin.getDefault().getSaveParticipantRegistry();
		SaveParticipantDescriptor[] descriptors= registry.getSaveParticipantDescriptors();
		
		if (descriptors.length == 0)
			return composite;
		
		Arrays.sort(descriptors, new Comparator() {
			public int compare(Object o1, Object o2) {
				SaveParticipantDescriptor d1= (SaveParticipantDescriptor)o1;
				SaveParticipantDescriptor d2= (SaveParticipantDescriptor)o2;
				return Collator.getInstance().compare(d1.getPostSaveListener().getName(), d2.getPostSaveListener().getName());
			}
		});
		
		IPreferencePageContainer container= fPreferencePage.getContainer();
		for (int i= 0; i < descriptors.length; i++) {
			final SaveParticipantDescriptor descriptor= descriptors[i];
			ISaveParticipantPreferenceConfiguration configuration= descriptor.createPreferenceConfiguration();
			configuration.createControl(composite, container);
			fConfigurations.add(configuration);
		}
		
		return composite;
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.IPreferenceConfigurationBlock#dispose()
	 */
	public void dispose() {
		delegateToPreferenceConfiguration(new IDelegateOperation() {
			public void run(ISaveParticipantPreferenceConfiguration block) {
				block.dispose();
			}
		});
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.IPreferenceConfigurationBlock#initialize()
	 */
	public void initialize() {
		delegateToPreferenceConfiguration(new IDelegateOperation() {
			public void run(ISaveParticipantPreferenceConfiguration block) {
				IAdaptable element= null;
				if (fPreferencePage instanceof PropertyAndPreferencePage) {
					element= ((PropertyAndPreferencePage)fPreferencePage).getElement();
				}
				block.initialize(fContext, element);
			}
		});
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.IPreferenceConfigurationBlock#performDefaults()
	 */
	public void performDefaults() {
		delegateToPreferenceConfiguration(new IDelegateOperation() {
			public void run(ISaveParticipantPreferenceConfiguration block) {
				block.performDefaults();
			}
		});
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.IPreferenceConfigurationBlock#performOk()
	 */
	public void performOk() {
		delegateToPreferenceConfiguration(new IDelegateOperation() {
			public void run(ISaveParticipantPreferenceConfiguration block) {
				block.performOk();
			}
		});
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void enableProjectSettings() {
		delegateToPreferenceConfiguration(new IDelegateOperation() {
			public void run(ISaveParticipantPreferenceConfiguration block) {
				block.enableProjectSettings();
			}
		});
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void disableProjectSettings() {
		delegateToPreferenceConfiguration(new IDelegateOperation() {
			public void run(ISaveParticipantPreferenceConfiguration block) {
				block.disableProjectSettings();
			}
		});
	}
	
	private void delegateToPreferenceConfiguration(IDelegateOperation op) {
		for (int i= 0; i < fConfigurations.size(); i++) {
	        ISaveParticipantPreferenceConfiguration block= (ISaveParticipantPreferenceConfiguration)fConfigurations.get(i);
	        op.run(block);
        }
	}
}
