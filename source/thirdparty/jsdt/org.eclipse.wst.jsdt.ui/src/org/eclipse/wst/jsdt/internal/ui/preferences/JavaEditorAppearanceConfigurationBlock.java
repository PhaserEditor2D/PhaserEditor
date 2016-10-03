/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.wst.jsdt.internal.ui.util.PixelConverter;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

/**
 * Configures Java Editor hover preferences.
 * 
 * 
 */
class JavaEditorAppearanceConfigurationBlock extends AbstractConfigurationBlock {

	private final String[][] fAppearanceColorListModel= new String[][] {
			{PreferencesMessages.JavaEditorPreferencePage_matchingBracketsHighlightColor2, PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR, null}, 
			{PreferencesMessages.JavaEditorPreferencePage_backgroundForCompletionProposals, PreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND, null }, 
			{PreferencesMessages.JavaEditorPreferencePage_foregroundForCompletionProposals, PreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND, null }, 
			{PreferencesMessages.JavaEditorPreferencePage_backgroundForMethodParameters, PreferenceConstants.CODEASSIST_PARAMETERS_BACKGROUND, null }, 
			{PreferencesMessages.JavaEditorPreferencePage_foregroundForMethodParameters, PreferenceConstants.CODEASSIST_PARAMETERS_FOREGROUND, null }, 
			{PreferencesMessages.JavaEditorPreferencePage_backgroundForCompletionReplacement, PreferenceConstants.CODEASSIST_REPLACEMENT_BACKGROUND, null }, 
			{PreferencesMessages.JavaEditorPreferencePage_foregroundForCompletionReplacement, PreferenceConstants.CODEASSIST_REPLACEMENT_FOREGROUND, null },
			{PreferencesMessages.JavaEditorPreferencePage_sourceHoverBackgroundColor, PreferenceConstants.EDITOR_SOURCE_HOVER_BACKGROUND_COLOR, PreferenceConstants.EDITOR_SOURCE_HOVER_BACKGROUND_COLOR_SYSTEM_DEFAULT},
			
		};

	private List fAppearanceColorList;
	private ColorSelector fAppearanceColorEditor;
	private Button fAppearanceColorDefault;

	private FontMetrics fFontMetrics;

	public JavaEditorAppearanceConfigurationBlock(PreferencePage mainPreferencePage, OverlayPreferenceStore store) {
		super(store, mainPreferencePage);
		getPreferenceStore().addKeys(createOverlayStoreKeys());
	}


	private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {
		
		ArrayList overlayKeys= new ArrayList();

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_MATCHING_BRACKETS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_QUICKASSIST_LIGHTBULB));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SHOW_SEGMENTS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_OVERRIDE_INDICATORS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_PARAMETERS_BACKGROUND));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_PARAMETERS_FOREGROUND));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_REPLACEMENT_BACKGROUND));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_REPLACEMENT_FOREGROUND));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_SOURCE_HOVER_BACKGROUND_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SOURCE_HOVER_BACKGROUND_COLOR_SYSTEM_DEFAULT));

		OverlayPreferenceStore.OverlayKey[] keys= new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
		overlayKeys.toArray(keys);
		return keys;
	}

	/**
	 * Creates page for appearance preferences.
	 * 
	 * @param parent the parent composite
	 * @return the control for the preference page
	 */
	public Control createControl(Composite parent) {
		initializeDialogUnits(parent);
		
		ScrolledPageContent scrolled= new ScrolledPageContent(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		

		Composite composite= new Composite(scrolled, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);
		
		createHeader(composite);
		createAppearancePage(composite);
		
		scrolled.setContent(composite);
		final Point size= composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		scrolled.setMinSize(size.x, size.y);
		return scrolled;

	}

	private void createHeader(Composite contents) {
		final Shell shell= contents.getShell();
		String text= PreferencesMessages.JavaEditorPreferencePage_link; 
		Link link= new Link(contents, SWT.NONE);
		link.setText(text);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(shell, "org.eclipse.ui.preferencePages.GeneralTextEditor", null, null); //$NON-NLS-1$
			}
		});
		// TODO replace by link-specific tooltips when
		// bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=88866 gets fixed
		link.setToolTipText(PreferencesMessages.JavaEditorPreferencePage_link_tooltip); 
		
		
		GridData gridData= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gridData.widthHint= 150; // only expand further if anyone else requires it
		link.setLayoutData(gridData);
		
		addFiller(contents);
	}
	
	private void addFiller(Composite composite) {
		PixelConverter pixelConverter= new PixelConverter(composite);
		
		Label filler= new Label(composite, SWT.LEFT );
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.heightHint= pixelConverter.convertHeightInCharsToPixels(1) / 2;
		filler.setLayoutData(gd);
	}
	
	/**
     * Returns the number of pixels corresponding to the width of the given
     * number of characters.
     * <p>
     * This method may only be called after <code>initializeDialogUnits</code>
     * has been called.
     * </p>
     * <p>
     * Clients may call this framework method, but should not override it.
     * </p>
     * 
     * @param chars
     *            the number of characters
     * @return the number of pixels
     */
    protected int convertWidthInCharsToPixels(int chars) {
        // test for failure to initialize for backward compatibility
        if (fFontMetrics == null)
            return 0;
        return Dialog.convertWidthInCharsToPixels(fFontMetrics, chars);
    }

	/**
     * Returns the number of pixels corresponding to the height of the given
     * number of characters.
     * <p>
     * This method may only be called after <code>initializeDialogUnits</code>
     * has been called.
     * </p>
     * <p>
     * Clients may call this framework method, but should not override it.
     * </p>
     * 
     * @param chars
     *            the number of characters
     * @return the number of pixels
     */
    protected int convertHeightInCharsToPixels(int chars) {
        // test for failure to initialize for backward compatibility
        if (fFontMetrics == null)
            return 0;
        return Dialog.convertHeightInCharsToPixels(fFontMetrics, chars);
    }
    
    private Control createAppearancePage(Composite parent) {

		Composite appearanceComposite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		appearanceComposite.setLayout(layout);

		String label;
		
		label= PreferencesMessages.JavaEditorPreferencePage_subWordNavigation; 
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION, 0);

		label= PreferencesMessages.JavaEditorPreferencePage_analyseAnnotationsWhileTyping; 
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS, 0);
		
		String text= PreferencesMessages.SmartTypingConfigurationBlock_annotationReporting_link; 
		addLink(appearanceComposite, text, INDENT);
		
		Label spacer= new Label(appearanceComposite, SWT.LEFT );
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.heightHint= convertHeightInCharsToPixels(1) / 2;
		spacer.setLayoutData(gd);
		
		label= PreferencesMessages.JavaEditorPreferencePage_highlightMatchingBrackets; 
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_MATCHING_BRACKETS, 0);

		label= PreferencesMessages.JavaEditorPreferencePage_quickassist_lightbulb; 
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_QUICKASSIST_LIGHTBULB, 0);
		
		label= PreferencesMessages.JavaEditorPreferencePage_showJavaElementOnly; 
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_SHOW_SEGMENTS, 0);

		label= PreferencesMessages.JavaEditorPreferencePage_showOverrideIndicators; 
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_OVERRIDE_INDICATORS, 0);
		
		Label l= new Label(appearanceComposite, SWT.LEFT );
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.heightHint= convertHeightInCharsToPixels(1) / 2;
		l.setLayoutData(gd);
		
		l= new Label(appearanceComposite, SWT.LEFT);
		l.setText(PreferencesMessages.JavaEditorPreferencePage_appearanceOptions); 
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		l.setLayoutData(gd);

		Composite editorComposite= new Composite(appearanceComposite, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
		gd.horizontalSpan= 2;
		editorComposite.setLayoutData(gd);		

		fAppearanceColorList= new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		gd= new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		gd.heightHint= convertHeightInCharsToPixels(12);
		fAppearanceColorList.setLayoutData(gd);
						
		Composite stylesComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		l= new Label(stylesComposite, SWT.LEFT);
		l.setText(PreferencesMessages.JavaEditorPreferencePage_color); 
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		l.setLayoutData(gd);

		fAppearanceColorEditor= new ColorSelector(stylesComposite);
		Button foregroundColorButton= fAppearanceColorEditor.getButton();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		foregroundColorButton.setLayoutData(gd);

		SelectionListener colorDefaultSelectionListener= new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				boolean systemDefault= fAppearanceColorDefault.getSelection();
				fAppearanceColorEditor.getButton().setEnabled(!systemDefault);
				
				int i= fAppearanceColorList.getSelectionIndex();
				if (i == -1)
					return;

				String key= fAppearanceColorListModel[i][2];
				if (key != null)
					getPreferenceStore().setValue(key, systemDefault);
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		};
		
		fAppearanceColorDefault= new Button(stylesComposite, SWT.CHECK);
		fAppearanceColorDefault.setText(PreferencesMessages.JavaEditorPreferencePage_systemDefault); 
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.horizontalSpan= 2;
		fAppearanceColorDefault.setLayoutData(gd);
		fAppearanceColorDefault.setVisible(false);
		fAppearanceColorDefault.addSelectionListener(colorDefaultSelectionListener);
		
		fAppearanceColorList.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				handleAppearanceColorListSelection();
			}
		});
		foregroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				int i= fAppearanceColorList.getSelectionIndex();
				if (i == -1)
					return;

				String key= fAppearanceColorListModel[i][1];
				PreferenceConverter.setValue(getPreferenceStore(), key, fAppearanceColorEditor.getColorValue());
			}
		});
		return appearanceComposite;
	}


	private void addLink(Composite composite, String text, int indent) {
		GridData gd;
		final Link link= new Link(composite, SWT.NONE);
		link.setText(text);
		gd= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gd.widthHint= 300; // don't get wider initially
		gd.horizontalSpan= 2;
		gd.horizontalIndent= indent;
		link.setLayoutData(gd);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(link.getShell(), e.text, null, null); 
			}
		});
	}
    
    private void handleAppearanceColorListSelection() {	
		int i= fAppearanceColorList.getSelectionIndex();
		if (i == -1)
			return;
		String key= fAppearanceColorListModel[i][1];
		RGB rgb= PreferenceConverter.getColor(getPreferenceStore(), key);
		fAppearanceColorEditor.setColorValue(rgb);		
		updateAppearanceColorWidgets(fAppearanceColorListModel[i][2]);
	}

	private void updateAppearanceColorWidgets(String systemDefaultKey) {
		if (systemDefaultKey == null) {
			fAppearanceColorDefault.setSelection(false);
			fAppearanceColorDefault.setVisible(false);
			fAppearanceColorEditor.getButton().setEnabled(true);
		} else {
			boolean systemDefault= getPreferenceStore().getBoolean(systemDefaultKey);
			fAppearanceColorDefault.setSelection(systemDefault);
			fAppearanceColorDefault.setVisible(true);
			fAppearanceColorEditor.getButton().setEnabled(!systemDefault);
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.IPreferenceConfigurationBlock#initialize()
	 */
	public void initialize() {
		super.initialize();
		initializeDefaultColors();
		
		for (int i= 0; i < fAppearanceColorListModel.length; i++)
			fAppearanceColorList.add(fAppearanceColorListModel[i][0]);
		
		fAppearanceColorList.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (fAppearanceColorList != null && !fAppearanceColorList.isDisposed()) {
					fAppearanceColorList.select(0);
					handleAppearanceColorListSelection();
				}
			}
		});

	}

	/**
	 * Initializes the default colors.
	 * 
	 * 
	 */
	private void initializeDefaultColors() {
		if (getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SOURCE_HOVER_BACKGROUND_COLOR_SYSTEM_DEFAULT)) {
			RGB rgb= fAppearanceColorList.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND).getRGB();
			PreferenceConverter.setValue(getPreferenceStore(), PreferenceConstants.EDITOR_SOURCE_HOVER_BACKGROUND_COLOR, rgb);
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.preferences.IPreferenceConfigurationBlock#performDefaults()
	 */
	public void performDefaults() {
		super.performDefaults();
		initializeDefaultColors();
		handleAppearanceColorListSelection();
	}

	/**
     * Initializes the computation of horizontal and vertical dialog units based
     * on the size of current font.
     * <p>
     * This method must be called before any of the dialog unit based conversion
     * methods are called.
     * </p>
     * 
     * @param testControl
     *            a control from which to obtain the current font
     */
    protected void initializeDialogUnits(Control testControl) {
        // Compute and store a font metric
        GC gc = new GC(testControl);
        gc.setFont(JFaceResources.getDialogFont());
        fFontMetrics = gc.getFontMetrics();
        gc.dispose();
    }
    
}
