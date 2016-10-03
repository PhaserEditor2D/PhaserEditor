/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.preferences.cleanup;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.jsdt.internal.ui.fix.ICleanUp;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.JavaPreview;
import org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ModifyDialogTabPage;

public abstract class CleanUpTabPage extends ModifyDialogTabPage {

	private final Map fValues;
	private JavaPreview fCleanUpPreview;
	private final boolean fIsSaveAction;
	private int fCount;
	private int fSelectedCount;
	
	public CleanUpTabPage(IModificationListener listener, Map values, boolean isSaveAction) {
		super(listener, values);
		fValues= values;
		fIsSaveAction= isSaveAction;
		fCount= 0;
		fSelectedCount= 0;
	}
	
	/**
	 * @return is this tab page shown in the save action dialog
	 */
	public boolean isSaveAction() {
		return fIsSaveAction;
	}
	
	public int getCleanUpCount() {
		return fCount;
	}

	public int getSelectedCleanUpCount() {
		return fSelectedCount;
	}
	
	protected abstract ICleanUp[] createPreviewCleanUps(Map values);
	
	protected JavaPreview doCreateJavaPreview(Composite parent) {
        fCleanUpPreview= new CleanUpPreview(parent, createPreviewCleanUps(fValues));
    	return fCleanUpPreview;
    }

	protected void doUpdatePreview() {
		fCleanUpPreview.setWorkingValues(fValues);
		fCleanUpPreview.update();
	}
	
	protected void initializePage() {
		fCleanUpPreview.update();
	}
	
	protected void registerPreference(final CheckboxPreference preference) {
		fCount++;
		preference.addObserver(new Observer() {
			public void update(Observable o, Object arg) {
				if (preference.getChecked()) {
					fSelectedCount++;
				} else {
					fSelectedCount--;
				}
			}
		});
		if (preference.getChecked()) {
			fSelectedCount++;
		}
	}
	
	protected void registerSlavePreference(final CheckboxPreference master, final RadioPreference[] slaves) {
		internalRegisterSlavePreference(master, slaves);
		registerPreference(master);
	}
	
	protected void registerSlavePreference(final CheckboxPreference master, final CheckboxPreference[] slaves) {
		internalRegisterSlavePreference(master, slaves);
		fCount+= slaves.length;
		
		master.addObserver(new Observer() {
			public void update(Observable o, Object arg) {
				if (master.getChecked()) {
					for (int i= 0; i < slaves.length; i++) {
						if (slaves[i].getChecked()) {
							fSelectedCount++;
						}
					}	
				} else {
					for (int i= 0; i < slaves.length; i++) {
						if (slaves[i].getChecked()) {
							fSelectedCount--;
						}
					}
				}
			}
		});
		
		for (int i= 0; i < slaves.length; i++) {
			final CheckboxPreference slave= slaves[i];
			slave.addObserver(new Observer() {
				public void update(Observable o, Object arg) {
					if (slave.getChecked()) {
						fSelectedCount++;
					} else {
						fSelectedCount--;
					}
				}
			});
		}
		
		if (master.getChecked()) {
			for (int i= 0; i < slaves.length; i++) {
				if (slaves[i].getChecked()) {
					fSelectedCount++;
				}
			}
		}
	}
	
	private void internalRegisterSlavePreference(final CheckboxPreference master, final ButtonPreference[] slaves) {
    	master.addObserver( new Observer() {
    		public void update(Observable o, Object arg) {
    			for (int i= 0; i < slaves.length; i++) {
					slaves[i].setEnabled(master.getChecked());
				}
    		}
    	});
    	
    	for (int i= 0; i < slaves.length; i++) {
			slaves[i].setEnabled(master.getChecked());
		}
	}

	protected void intent(Composite group) {
        Label l= new Label(group, SWT.NONE);
    	GridData gd= new GridData();
    	gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(4);
    	l.setLayoutData(gd);
    }

}