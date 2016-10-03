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
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.contentassist.SubjectControlContentAssistant;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.contentassist.ContentAssistHandler;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.wst.jsdt.internal.corext.refactoring.StubTypeContext;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TableTextCellEditor;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;
import org.eclipse.wst.jsdt.internal.ui.refactoring.contentassist.VariableNamesProcessor;
import org.eclipse.wst.jsdt.internal.ui.util.SWTUtil;
import org.eclipse.wst.jsdt.internal.ui.util.TableLayoutComposite;

/**
 * A special control to edit and reorder method parameters.
 */
public class ChangeParametersControl extends Composite {
	
	
	static final boolean SHOW_TYPES=false;
	public static class Mode {
		private final String fName;
		private Mode(String name) {
			fName= name;
		}
		public static final Mode EXTRACT_METHOD= new Mode("EXTRACT_METHOD"); //$NON-NLS-1$
		public static final Mode CHANGE_METHOD_SIGNATURE= new Mode("CHANGE_METHOD_SIGNATURE"); //$NON-NLS-1$
		public static final Mode INTRODUCE_PARAMETER= new Mode("INTRODUCE_PARAMETER"); //$NON-NLS-1$
		public String toString() {
			return fName;
		}
		public boolean canChangeTypes() {
			return this == CHANGE_METHOD_SIGNATURE;
		}
		public boolean canAddParameters() {
			return this == Mode.CHANGE_METHOD_SIGNATURE;
		}
		public boolean canChangeDefault() {
			return this == Mode.CHANGE_METHOD_SIGNATURE;
		}
	}
	
	private static class ParameterInfoContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object inputElement) {
			return removeMarkedAsDeleted((List) inputElement);
		}
		private ParameterInfo[] removeMarkedAsDeleted(List paramInfos){
			List result= new ArrayList(paramInfos.size());
			for (Iterator iter= paramInfos.iterator(); iter.hasNext();) {
				ParameterInfo info= (ParameterInfo) iter.next();
				if (! info.isDeleted())
					result.add(info);
			}
			return (ParameterInfo[]) result.toArray(new ParameterInfo[result.size()]);
		}
		public void dispose() {
			// do nothing
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// do nothing
		}
	}

	private static class ParameterInfoLabelProvider extends LabelProvider implements ITableLabelProvider, ITableFontProvider {
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
		public String getColumnText(Object element, int columnIndex) {
			ParameterInfo info= (ParameterInfo) element;
			switch (columnIndex) {
				case TYPE_PROP:
					return info.getNewTypeName();
				case NEWNAME_PROP:
					return info.getNewName();
				case DEFAULT_PROP:
				    if (info.isAdded())
				        return info.getDefaultValue();
				    else
				        return "-"; //$NON-NLS-1$
				default:
					throw new IllegalArgumentException(columnIndex + ": " + element); //$NON-NLS-1$
			}
		}
		public Font getFont(Object element, int columnIndex) {
			ParameterInfo info= (ParameterInfo) element;
			if (info.isAdded())
				return JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
			else
				return null;
		}		
	}

	private class ParametersCellModifier implements ICellModifier {
		public boolean canModify(Object element, String property) {
			Assert.isTrue(element instanceof ParameterInfo);
			if (JavaScriptCore.IS_ECMASCRIPT4 && property.equals(PROPERTIES[TYPE_PROP]))
				return fMode.canChangeTypes();
			else if (property.equals(PROPERTIES[NEWNAME_PROP]))
				return true;
			else if (property.equals(PROPERTIES[DEFAULT_PROP]))
				return (((ParameterInfo)element).isAdded());
			Assert.isTrue(false);
			return false;
		}
		public Object getValue(Object element, String property) {
			Assert.isTrue(element instanceof ParameterInfo);
			if (JavaScriptCore.IS_ECMASCRIPT4 && property.equals(PROPERTIES[TYPE_PROP]))
				return ((ParameterInfo) element).getNewTypeName();
			else if (property.equals(PROPERTIES[NEWNAME_PROP]))
				return ((ParameterInfo) element).getNewName();
			else if (property.equals(PROPERTIES[DEFAULT_PROP]))
				return ((ParameterInfo) element).getDefaultValue();
			Assert.isTrue(false);
			return null;
		}
		public void modify(Object element, String property, Object value) {
			if (element instanceof TableItem)
				element= ((TableItem) element).getData();
			if (!(element instanceof ParameterInfo))
				return;
			boolean unchanged;
			ParameterInfo parameterInfo= (ParameterInfo) element;
			if (property.equals(PROPERTIES[NEWNAME_PROP])) {
				unchanged= parameterInfo.getNewName().equals(value);
				parameterInfo.setNewName((String) value);
			} else if (property.equals(PROPERTIES[DEFAULT_PROP])) {
				unchanged= parameterInfo.getDefaultValue().equals(value);
				parameterInfo.setDefaultValue((String) value);
			} else if (property.equals(PROPERTIES[TYPE_PROP])) {
				unchanged= parameterInfo.getNewTypeName().equals(value);
				parameterInfo.setNewTypeName((String) value);
			} else {
				throw new IllegalStateException();
			}
			if (! unchanged) {
				ChangeParametersControl.this.fListener.parameterChanged(parameterInfo);
				ChangeParametersControl.this.fTableViewer.update(parameterInfo, new String[] { property });
			}
		}
	}

	private static final String[] PROPERTIES_NO_RETURN = new String[] {  RefactoringMessages.ChangeParametersControl_new, RefactoringMessages.ChangeParametersControl_default }; 
	private static final String[] PROPERTIES_WITH_RETURN = new String[] { RefactoringMessages.ChangeParametersControl_type, RefactoringMessages.ChangeParametersControl_new, RefactoringMessages.ChangeParametersControl_default }; 
	
	private static final String[] PROPERTIES= JavaScriptCore.IS_ECMASCRIPT4?PROPERTIES_WITH_RETURN:PROPERTIES_NO_RETURN;
	
	
	
	
	private static final int TYPE_PROP= JavaScriptCore.IS_ECMASCRIPT4?0:-1;
	private static final int NEWNAME_PROP= JavaScriptCore.IS_ECMASCRIPT4?1:0;
	private static final int DEFAULT_PROP= JavaScriptCore.IS_ECMASCRIPT4?2:1;

	private static final int ROW_COUNT= 7;

	private final Mode fMode;
	private final IParameterListChangeListener fListener;
	private List fParameterInfos;
	private final StubTypeContext fTypeContext;
	private final String[] fParamNameProposals;
	private ContentAssistHandler fNameContentAssistHandler;

	private TableViewer fTableViewer;
	private Button fUpButton;
	private Button fDownButton;
	private Button fEditButton;
	private Button fAddButton;
	private Button fRemoveButton;

	public ChangeParametersControl(Composite parent, int style, String label, IParameterListChangeListener listener, Mode mode, StubTypeContext typeContext) {
		this(parent, style, label, listener, mode, typeContext, new String[0]);
	}
	
	public ChangeParametersControl(Composite parent, int style, String label, IParameterListChangeListener listener, Mode mode) {
		this(parent, style, label, listener, mode, null, new String[0]);
	}
	
	public ChangeParametersControl(Composite parent, int style, String label, IParameterListChangeListener listener, Mode mode, String[] paramNameProposals) {
		this(parent, style, label, listener, mode, null, paramNameProposals);
	}
	
	/**
	 * @param label the label before the table or <code>null</code>
	 * @param typeContext the package in which to complete types
	 */
	private ChangeParametersControl(Composite parent, int style, String label, IParameterListChangeListener listener, Mode mode, StubTypeContext typeContext, String[] paramNameProposals) {
		super(parent, style);
		Assert.isNotNull(listener);
		fListener= listener;
		fMode= mode;
		fTypeContext= typeContext;
		fParamNameProposals= paramNameProposals;

		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		setLayout(layout);

		if (label != null) {
			Label tableLabel= new Label(this, SWT.NONE);
			GridData labelGd= new GridData();
			labelGd.horizontalSpan= 2;
			tableLabel.setLayoutData(labelGd);
			tableLabel.setText(label);
		}

		createParameterList(this);
		createButtonComposite(this);
	}


	public void setInput(List parameterInfos) {
		Assert.isNotNull(parameterInfos);
		fParameterInfos= parameterInfos;
		fTableViewer.setInput(fParameterInfos);
		if (fParameterInfos.size() > 0)
			fTableViewer.setSelection(new StructuredSelection(fParameterInfos.get(0)));
	}
	
	public void editParameter(ParameterInfo info) {
		fTableViewer.getControl().setFocus();
		if (! info.isDeleted()) {
			fTableViewer.setSelection(new StructuredSelection(info), true);
			updateButtonsEnabledState();
			editColumnOrNextPossible(NEWNAME_PROP);
			return;	
		}
	}

	// ---- Parameter table -----------------------------------------------------------------------------------

	private void createParameterList(Composite parent) {
		TableLayoutComposite layouter= new TableLayoutComposite(parent, SWT.NONE);
		addColumnLayoutData(layouter);
		
		final Table table= new Table(layouter, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		TableColumn tc;
		
//		if (SHOW_TYPES)
//		{
		if(JavaScriptCore.IS_ECMASCRIPT4) {
			tc= new TableColumn(table, SWT.NONE, TYPE_PROP);
			tc.setResizable(true);
			tc.setText(SHOW_TYPES?RefactoringMessages.ChangeParametersControl_table_type:""); //$NON-NLS-1$
//		
			}
		
		int index= NEWNAME_PROP;
//		if (!SHOW_TYPES)
//			index--;
		tc= new TableColumn(table, SWT.NONE,index);
		tc.setResizable(true);
		tc.setText(RefactoringMessages.ChangeParametersControl_table_name); 

		if (fMode.canChangeDefault()){
			 index= DEFAULT_PROP;
//			if (!SHOW_TYPES)
//				index--;
			tc= new TableColumn(table, SWT.NONE, index);
			tc.setResizable(true);
			tc.setText(RefactoringMessages.ChangeParametersControl_table_defaultValue); 
		}	
		
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= SWTUtil.getTableHeightHint(table, ROW_COUNT);
		gd.widthHint= 40;
		layouter.setLayoutData(gd);

		fTableViewer= new TableViewer(table);
		fTableViewer.setUseHashlookup(true);
		fTableViewer.setContentProvider(new ParameterInfoContentProvider());
		fTableViewer.setLabelProvider(new ParameterInfoLabelProvider());
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtonsEnabledState();
			}
		});

		table.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN && e.stateMask == SWT.NONE) {
					editColumnOrNextPossible(0);
					e.detail= SWT.TRAVERSE_NONE;
				}
			}
		});
		table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.F2 && e.stateMask == SWT.NONE) {
					editColumnOrNextPossible(0);
					e.doit= false;
				}
			}
		});

		addCellEditors();
	}

	private void editColumnOrNextPossible(int column){
		ParameterInfo[]	selected= getSelectedElements();
		if (selected.length != 1)
			return;
		int nextColumn= column;
		do {
			fTableViewer.editElement(selected[0], nextColumn);
			if (fTableViewer.isCellEditorActive())
				return;
			nextColumn= nextColumn(nextColumn);
		} while (nextColumn != column);
	}
	
	private void editColumnOrPrevPossible(int column){
		ParameterInfo[]	selected= getSelectedElements();
		if (selected.length != 1)
			return;
		int prevColumn= column;
		do {
			fTableViewer.editElement(selected[0], prevColumn);
			if (fTableViewer.isCellEditorActive())
			    return;
			prevColumn= prevColumn(prevColumn);
		} while (prevColumn != column);
	}
	
	private int nextColumn(int column) {
		return (column >= getTable().getColumnCount() - 1) ? 0 : column + 1;
	}
	
	private int prevColumn(int column) {
		return (column <= 0) ? getTable().getColumnCount() - 1 : column - 1;
	}
	
	private void addColumnLayoutData(TableLayoutComposite layouter) {
		if (fMode.canChangeDefault()){
		//	layouter.addColumnData(new ColumnWeightData(33, true));
			layouter.addColumnData(new ColumnWeightData(33, true));
			layouter.addColumnData(new ColumnWeightData(34, true));
		} else if (SHOW_TYPES){
			layouter.addColumnData(new ColumnWeightData(50, true));
			layouter.addColumnData(new ColumnWeightData(50, true));
		}	
		else
		{
			layouter.addColumnData(new ColumnWeightData(1, true));
			layouter.addColumnData(new ColumnWeightData(99, true));
		}
	}

	private ParameterInfo[] getSelectedElements() {
		ISelection selection= fTableViewer.getSelection();
		if (selection == null)
			return new ParameterInfo[0];

		if (!(selection instanceof IStructuredSelection))
			return new ParameterInfo[0];

		List selected= ((IStructuredSelection) selection).toList();
		return (ParameterInfo[]) selected.toArray(new ParameterInfo[selected.size()]);
	}

	// ---- Button bar --------------------------------------------------------------------------------------

	private void createButtonComposite(Composite parent) {
		Composite buttonComposite= new Composite(parent, SWT.NONE);
		buttonComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		GridLayout gl= new GridLayout();
		gl.marginHeight= 0;
		gl.marginWidth= 0;
		buttonComposite.setLayout(gl);

		if (fMode.canAddParameters())
			fAddButton= createAddButton(buttonComposite);	

		fEditButton= createEditButton(buttonComposite);
		
		if (fMode.canAddParameters())
			fRemoveButton= createRemoveButton(buttonComposite);	
		
		if (buttonComposite.getChildren().length != 0)
			addSpacer(buttonComposite);

		fUpButton= createButton(buttonComposite, RefactoringMessages.ChangeParametersControl_buttons_move_up, true); 
		fDownButton= createButton(buttonComposite, RefactoringMessages.ChangeParametersControl_buttons_move_down, false); 

		updateButtonsEnabledState();
	}

	private void addSpacer(Composite parent) {
		Label label= new Label(parent, SWT.NONE);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint= 5;
		label.setLayoutData(gd);
	}

	private void updateButtonsEnabledState() {
		fUpButton.setEnabled(canMove(true));
		fDownButton.setEnabled(canMove(false));
		if (fEditButton != null)
			fEditButton.setEnabled(getTableSelectionCount() == 1);
		if (fAddButton != null)
			fAddButton.setEnabled(true);	
		if (fRemoveButton != null)
			fRemoveButton.setEnabled(getTableSelectionCount() != 0);
	}

	private int getTableSelectionCount() {
		return getTable().getSelectionCount();
	}

	private int getTableItemCount() {
		return getTable().getItemCount();
	}

	private Table getTable() {
		return fTableViewer.getTable();
	}
	
	private Button createEditButton(Composite buttonComposite) {
		Button button= new Button(buttonComposite, SWT.PUSH);
		button.setText(RefactoringMessages.ChangeParametersControl_buttons_edit); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					ParameterInfo[] selected= getSelectedElements();
					Assert.isTrue(selected.length == 1);
					ParameterInfo parameterInfo= selected[0];
					ParameterEditDialog dialog= new ParameterEditDialog(getShell(), parameterInfo, fMode.canChangeTypes(), fMode.canChangeDefault(), fTypeContext);
					dialog.open();
					fListener.parameterChanged(parameterInfo);
					fTableViewer.update(parameterInfo, PROPERTIES);
				} finally {
					fTableViewer.getControl().setFocus();
				}
			}
		});
		return button;
	}
	
	private Button createAddButton(Composite buttonComposite) {
		Button button= new Button(buttonComposite, SWT.PUSH);
		button.setText(RefactoringMessages.ChangeParametersControl_buttons_add); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String[] excludedParamNames= new String[fParameterInfos.size()];
				for (int i= 0; i < fParameterInfos.size(); i++) {
					ParameterInfo info= (ParameterInfo) fParameterInfos.get(i);
					excludedParamNames[i]= info.getNewName();
				}
				IJavaScriptProject javaProject= fTypeContext.getCuHandle().getJavaScriptProject();
				String newParamName= StubUtility.suggestArgumentName(javaProject, RefactoringMessages.ChangeParametersControl_new_parameter_default_name, excludedParamNames);
				ParameterInfo newInfo= ParameterInfo.createInfoForAddedParameter("Object", newParamName, "null"); //$NON-NLS-1$ //$NON-NLS-2$
				int insertIndex= fParameterInfos.size();
				for (int i= fParameterInfos.size() - 1;  i >= 0; i--) {
					ParameterInfo info= (ParameterInfo) fParameterInfos.get(i);
					if (info.isNewVarargs()) {
						insertIndex= i;
						break;
					}
				}
				fParameterInfos.add(insertIndex, newInfo);
				fListener.parameterAdded(newInfo);
				fTableViewer.refresh();
				fTableViewer.getControl().setFocus();
				fTableViewer.setSelection(new StructuredSelection(newInfo), true);
				updateButtonsEnabledState();
				editColumnOrNextPossible(0);
			}
		});	
		return button;
	}

	private Button createRemoveButton(Composite buttonComposite) {
		final Button button= new Button(buttonComposite, SWT.PUSH);
		button.setText(RefactoringMessages.ChangeParametersControl_buttons_remove); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index= getTable().getSelectionIndices()[0];
				ParameterInfo[] selected= getSelectedElements();
				for (int i= 0; i < selected.length; i++) {
					if (selected[i].isAdded())
						fParameterInfos.remove(selected[i]);
					else
						selected[i].markAsDeleted();	
				}
				restoreSelection(index);
			}
			private void restoreSelection(int index) {
				fTableViewer.refresh();
				fTableViewer.getControl().setFocus();
				int itemCount= getTableItemCount();
				if (itemCount != 0 && index >= itemCount) {
					index= itemCount - 1;
					getTable().setSelection(index);
				}
				fListener.parameterListChanged();
				updateButtonsEnabledState();
			}
		});	
		return button;
	}

	private Button createButton(Composite buttonComposite, String text, final boolean up) {
		Button button= new Button(buttonComposite, SWT.PUSH);
		button.setText(text);
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ISelection savedSelection= fTableViewer.getSelection();
				if (savedSelection == null)
					return;
				ParameterInfo[] selection= getSelectedElements();
				if (selection.length == 0)
					return;
					
				if (up) {
					moveUp(selection);
				} else {
					moveDown(selection);
				}
				fTableViewer.refresh();
				fTableViewer.setSelection(savedSelection);
				fListener.parameterListChanged();
				fTableViewer.getControl().setFocus();
			}
		});
		return button;
	}
	
	//---- editing -----------------------------------------------------------------------------------------------

	private void addCellEditors() {
		fTableViewer.setColumnProperties(PROPERTIES);
		
		final TableTextCellEditor editors[]= new TableTextCellEditor[PROPERTIES.length];

		if(JavaScriptCore.IS_ECMASCRIPT4)	editors[TYPE_PROP]= new TableTextCellEditor(fTableViewer, TYPE_PROP);
		editors[NEWNAME_PROP]= new TableTextCellEditor(fTableViewer, NEWNAME_PROP);
		editors[DEFAULT_PROP]= new TableTextCellEditor(fTableViewer, DEFAULT_PROP);
		
		if (fMode.canChangeTypes() && (JavaScriptCore.IS_ECMASCRIPT4)	) {
			SubjectControlContentAssistant assistant= installParameterTypeContentAssist(editors[TYPE_PROP].getText());
			editors[TYPE_PROP].setContentAssistant(assistant);
		}
		if (fParamNameProposals.length > 0 ) {
			SubjectControlContentAssistant assistant= installParameterNameContentAssist(editors[NEWNAME_PROP].getText());
			editors[NEWNAME_PROP].setContentAssistant(assistant);
		}
		
		for (int i = 0; i < editors.length; i++) {
			final int editorColumn= i;
			final TableTextCellEditor editor = editors[i];
			// support tabbing between columns while editing:
			editor.getText().addTraverseListener(new TraverseListener() {
				public void keyTraversed(TraverseEvent e) {
					switch (e.detail) {
						case SWT.TRAVERSE_TAB_NEXT :
							editColumnOrNextPossible(nextColumn(editorColumn));
							e.detail= SWT.TRAVERSE_NONE;
							break;

						case SWT.TRAVERSE_TAB_PREVIOUS :
							editColumnOrPrevPossible(prevColumn(editorColumn));
							e.detail= SWT.TRAVERSE_NONE;
							break;
						
						default :
							break;
					}
				}
			});
			TextFieldNavigationHandler.install(editor.getText());
		}
		
		editors[NEWNAME_PROP].setActivationListener(new TableTextCellEditor.IActivationListener(){
			public void activate() {
				ParameterInfo[] selected= getSelectedElements();
				if (selected.length == 1 && fNameContentAssistHandler != null) {
					fNameContentAssistHandler.setEnabled(selected[0].isAdded());
				}
			}
		});
		
		fTableViewer.setCellEditors(editors);
		fTableViewer.setCellModifier(new ParametersCellModifier());
	}

	private SubjectControlContentAssistant installParameterTypeContentAssist(Text text) {
		JavaTypeCompletionProcessor processor= new JavaTypeCompletionProcessor(true, false);
		if (fTypeContext == null)
			processor.setCompletionContext(null, null, null);
		else
			processor.setCompletionContext(fTypeContext.getCuHandle(), fTypeContext.getBeforeString(), fTypeContext.getAfterString());
		SubjectControlContentAssistant contentAssistant= ControlContentAssistHelper.createJavaContentAssistant(processor);
		ContentAssistHandler.createHandlerForText(text, contentAssistant);
		return contentAssistant;
	}

	private SubjectControlContentAssistant installParameterNameContentAssist(Text text) {
		VariableNamesProcessor processor= new VariableNamesProcessor(fParamNameProposals);
		SubjectControlContentAssistant contentAssistant= ControlContentAssistHelper.createJavaContentAssistant(processor);
		fNameContentAssistHandler= ContentAssistHandler.createHandlerForText(text, contentAssistant);
		return contentAssistant;
	}

	//---- change order ----------------------------------------------------------------------------------------

	private void moveUp(ParameterInfo[] selection) {
		moveUp(fParameterInfos, Arrays.asList(selection));
	}

	private void moveDown(ParameterInfo[] selection) {
		Collections.reverse(fParameterInfos);
		moveUp(fParameterInfos, Arrays.asList(selection));
		Collections.reverse(fParameterInfos);
	}

	private static void moveUp(List elements, List move) {
		List res= new ArrayList(elements.size());
		List deleted= new ArrayList();
		Object floating= null;
		for (Iterator iter= elements.iterator(); iter.hasNext();) {
			Object curr= iter.next();
			if (move.contains(curr)) {
				res.add(curr);
			} else if (((ParameterInfo) curr).isDeleted()) {
				deleted.add(curr);
			} else {
				if (floating != null)
					res.add(floating);
				floating= curr;
			}
		}
		if (floating != null) {
			res.add(floating);
		}
		res.addAll(deleted);
		elements.clear();
		for (Iterator iter= res.iterator(); iter.hasNext();) {
			elements.add(iter.next());
		}
	}

	private boolean canMove(boolean up) {
		int notDeletedInfosCount= getNotDeletedInfosCount();
		if (notDeletedInfosCount == 0)
			return false;
		int[] indc= getTable().getSelectionIndices();
		if (indc.length == 0)
			return false;
		int invalid= up ? 0 : notDeletedInfosCount - 1;
		for (int i= 0; i < indc.length; i++) {
			if (indc[i] == invalid)
				return false;
		}
		return true;
	}
	
	private int getNotDeletedInfosCount(){
		if (fParameterInfos == null) // during initialization
			return 0;
		int result= 0;
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (! info.isDeleted())
				result++;
		}
		return result;
	}
}
