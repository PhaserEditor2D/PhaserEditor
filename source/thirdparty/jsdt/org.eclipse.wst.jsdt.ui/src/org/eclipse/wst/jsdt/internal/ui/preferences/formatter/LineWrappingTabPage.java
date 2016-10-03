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
package org.eclipse.wst.jsdt.internal.ui.preferences.formatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;


/**
 * The line wrapping tab page.
 */
public class LineWrappingTabPage extends FormatterTabPage {
	/**
	 * Constant array for boolean selection 
	 */
	private static String[] FALSE_TRUE = {
		DefaultCodeFormatterConstants.FALSE,
		DefaultCodeFormatterConstants.TRUE
	};

    /**
     * Represents a line wrapping category. All members are final.
     */
	private final static class Category {
		public final String key;
		public final String name;
		public final String previewText;
		public final List children;
		public final List preferences;
		
		public int index;

		public Category(String _key, String _previewText, String _name) {
			this.key= _key;
			this.name= _name;
			this.previewText= _previewText != null ? createPreviewHeader(_name) + _previewText : null;
			children= new ArrayList();
			preferences= new ArrayList();
		}
		
		/**
		 * @param _name Category name
		 */
		public Category(String _name) {
		    this(null, null, _name);
		}
		
		public String toString() {
			return name;
		}

		public void addPreference(Preference specificPreference) {
			preferences.add(specificPreference);
		}

		public Preference[] getSpecificPreferences() {
			return (Preference[])preferences.toArray(new Preference[preferences.size()]);
		}
	}
	

	private final static String PREF_CATEGORY_INDEX= JavaScriptUI.ID_PLUGIN + "formatter_page.line_wrapping_tab_page.last_category_index"; //$NON-NLS-1$ 
	
	
	private final class CategoryListener implements ISelectionChangedListener, IDoubleClickListener {
		
		private final List fCategoriesList;
		
		private int fIndex= 0;
		
		public CategoryListener(List categoriesTree) {
			fCategoriesList= new ArrayList();
			flatten(fCategoriesList, categoriesTree);
		}
		
		private void flatten(List categoriesList, List categoriesTree) {
			for (final Iterator iter= categoriesTree.iterator(); iter.hasNext(); ) {
				final Category category= (Category) iter.next();
				category.index= fIndex++;
				categoriesList.add(category);
				flatten(categoriesList, category.children);
			}	
		}

		public void add(Category category) {
			category.index= fIndex++;
			fCategoriesList.add(category);
		}

		public void selectionChanged(SelectionChangedEvent event) {
		    if (event != null)
		        fSelection= (IStructuredSelection)event.getSelection();
		    
		    if (fSelection.size() == 0) {
		        disableAll();
		        return;
		    }
		    
		    if (!fOptionsGroup.isEnabled())
		        enableDefaultComponents(true);
		    
		    fSelectionState.refreshState(fSelection);
		    
			final Category category= (Category)fSelection.getFirstElement();
			fDialogSettings.put(PREF_CATEGORY_INDEX, category.index);
			
			fOptionsGroup.setText(getGroupLabel(category));
		}
		
		private String getGroupLabel(Category category) {
		    if (fSelection.size() == 1) {
			    if (fSelectionState.getElements().size() == 1)
			        return Messages.format(FormatterMessages.LineWrappingTabPage_group, category.name.toLowerCase()); 
			    return Messages.format(FormatterMessages.LineWrappingTabPage_multi_group, new String[] {category.name.toLowerCase(), Integer.toString(fSelectionState.getElements().size())}); 
		    }
			return Messages.format(FormatterMessages.LineWrappingTabPage_multiple_selections, new String[] {Integer.toString(fSelectionState.getElements().size())}); 
		}
        
        private void disableAll() {
            enableDefaultComponents(false);
            fIndentStyleCombo.setEnabled(false);       
            fForceSplit.setEnabled(false);
        }
        
        private void enableDefaultComponents(boolean enabled) {
            fOptionsGroup.setEnabled(enabled);
            fWrappingStyleCombo.setEnabled(enabled);
            fWrappingStylePolicy.setEnabled(enabled);
        }

        public void restoreSelection() {
			int index;
			try {
				index= fDialogSettings.getInt(PREF_CATEGORY_INDEX);
			} catch (NumberFormatException ex) {
				index= -1;
			}
			if (index < 0 || index > fCategoriesList.size() - 1) {
				index= 1; // In order to select a category with preview initially
			}
			final Category category= (Category)fCategoriesList.get(index);
			fCategoriesViewer.setSelection(new StructuredSelection(new Category[] {category}));
		}

        public void doubleClick(DoubleClickEvent event) {
            final ISelection selection= event.getSelection();
            if (selection instanceof IStructuredSelection) {
                final Category node= (Category)((IStructuredSelection)selection).getFirstElement();
                fCategoriesViewer.setExpandedState(node, !fCategoriesViewer.getExpandedState(node));
            }
        }
	}
	
	private class SelectionState {
	    private List fElements= new ArrayList();
	    private boolean fRequiresRelayout;
	    
	    public void refreshState(IStructuredSelection selection) {
	        Map wrappingStyleMap= new HashMap();
		    Map indentStyleMap= new HashMap();
		    Map forceWrappingMap= new HashMap();
		    fRequiresRelayout= false;
		    showSpecificControls(false);
	        fElements.clear();
	        evaluateElements(selection.iterator());
	        evaluateMaps(wrappingStyleMap, indentStyleMap, forceWrappingMap);
	        setPreviewText(getPreviewText(wrappingStyleMap, indentStyleMap, forceWrappingMap));
	        refreshControls(wrappingStyleMap, indentStyleMap, forceWrappingMap);
	    }
	    
		public List getElements() {
	        return fElements;
	    }
	    
	    private void evaluateElements(Iterator iterator) {
            Category category;
            String value;
            while (iterator.hasNext()) {
                category= (Category) iterator.next();
                value= (String)fWorkingValues.get(category.key);
                if (value != null) {
                    if (!fElements.contains(category))
                        fElements.add(category);
                }
                else {
                    evaluateElements(category.children.iterator());
                }
            }
        }
	    
	    private void evaluateMaps(Map wrappingStyleMap, Map indentStyleMap, Map forceWrappingMap) {
	        Iterator iterator= fElements.iterator();
            while (iterator.hasNext()) {
                insertIntoMap(wrappingStyleMap, indentStyleMap, forceWrappingMap, (Category)iterator.next());
            }
	    }
  
        private String getPreviewText(Map wrappingMap, Map indentMap, Map forceMap) {
            Iterator iterator= fElements.iterator();
            StringBuilder previewText= new StringBuilder();
            while (iterator.hasNext()) {
                Category category= (Category)iterator.next();
                previewText.append(category.previewText);
                previewText.append('\n').append('\n');
            }
            return previewText.toString();
        }
        
        private void insertIntoMap(Map wrappingMap, Map indentMap, Map forceMap, Category category) {
            final String value= (String)fWorkingValues.get(category.key);
            Integer wrappingStyle;
            Integer indentStyle;
            Boolean forceWrapping;
            
            try {
                wrappingStyle= Integer.valueOf(DefaultCodeFormatterConstants.getWrappingStyle(value));
                indentStyle= Integer.valueOf(DefaultCodeFormatterConstants.getIndentStyle(value));
                forceWrapping= new Boolean(DefaultCodeFormatterConstants.getForceWrapping(value));
            } catch (IllegalArgumentException e) {
				forceWrapping= new Boolean(false);
				indentStyle= Integer.valueOf(DefaultCodeFormatterConstants.INDENT_DEFAULT);
				wrappingStyle= Integer.valueOf(DefaultCodeFormatterConstants.WRAP_NO_SPLIT);
			} 
			
            increaseMapEntry(wrappingMap, wrappingStyle);
            increaseMapEntry(indentMap, indentStyle);
            increaseMapEntry(forceMap, forceWrapping);
        }
        
        private void increaseMapEntry(Map map, Object type) {
            Integer count= (Integer)map.get(type);
            if (count == null) // not in map yet -> count == 0
                map.put(type, Integer.valueOf(1));
            else
                map.put(type, Integer.valueOf(count.intValue() + 1));
        }
                
        private void refreshControls(Map wrappingStyleMap, Map indentStyleMap, Map forceWrappingMap) {
            updateCombos(wrappingStyleMap, indentStyleMap);
            updateButton(forceWrappingMap);
            Integer wrappingStyleMax= getWrappingStyleMax(wrappingStyleMap);
			boolean isInhomogeneous= (fElements.size() != ((Integer)wrappingStyleMap.get(wrappingStyleMax)).intValue());
			updateControlEnablement(isInhomogeneous, wrappingStyleMax.intValue());
			showSpecificControls(true);
			if (fRequiresRelayout) {
				fOptionsComposite.layout(true, true);
			}
		    doUpdatePreview();
			notifyValuesModified();
        }
        
        private void showSpecificControls(boolean show) {
        	if (fElements.size() != 1)
        		return;
        	
        	Preference[] preferences= ((Category)fElements.get(0)).getSpecificPreferences();
	    	if (preferences.length == 0)
	    		return;
	    	
	    	fRequiresRelayout= true;
	    	for (int i= 0; i < preferences.length; i++) {
				Preference preference= preferences[i];
				Control control= preference.getControl();
				control.setVisible(show);
				((GridData)control.getLayoutData()).exclude= !show;
			}
		}

		private Integer getWrappingStyleMax(Map wrappingStyleMap) {
            int maxCount= 0, maxStyle= 0;
            for (int i=0; i<WRAPPING_NAMES.length; i++) {
                Integer count= (Integer)wrappingStyleMap.get(Integer.valueOf(i));
                if (count == null)
                    continue;
                if (count.intValue() > maxCount) {
                    maxCount= count.intValue();
                    maxStyle= i;
                }
            }
            return Integer.valueOf(maxStyle);
        }
        
        private void updateButton(Map forceWrappingMap) {
            Integer nrOfTrue= (Integer)forceWrappingMap.get(Boolean.TRUE);
            Integer nrOfFalse= (Integer)forceWrappingMap.get(Boolean.FALSE);
            
            if (nrOfTrue == null || nrOfFalse == null)
                fForceSplit.setSelection(nrOfTrue != null);
            else
                fForceSplit.setSelection(nrOfTrue.intValue() > nrOfFalse.intValue());
            
            int max= getMax(nrOfTrue, nrOfFalse);
            String label= FormatterMessages.LineWrappingTabPage_force_split_checkbox_multi_text; 
            fForceSplit.setText(getLabelText(label, max, fElements.size())); 
        }
        
        private String getLabelText(String label, int count, int nElements) {
            if (nElements == 1 || count == 0)
                return label;
            return Messages.format(FormatterMessages.LineWrappingTabPage_occurences, new String[] {label, Integer.toString(count), Integer.toString(nElements)}); 
        }
        
        private int getMax(Integer nrOfTrue, Integer nrOfFalse) {
            if (nrOfTrue == null)
                return nrOfFalse.intValue();
            if (nrOfFalse == null)
                return nrOfTrue.intValue();
            if (nrOfTrue.compareTo(nrOfFalse) >= 0)
                return nrOfTrue.intValue();
            return nrOfFalse.intValue();
        }
        
        private void updateCombos(Map wrappingStyleMap, Map indentStyleMap) {
            updateCombo(fWrappingStyleCombo, wrappingStyleMap, WRAPPING_NAMES);
            updateCombo(fIndentStyleCombo, indentStyleMap, INDENT_NAMES);
        }
        
        private void updateCombo(Combo combo, Map map, final String[] items) {
            String[] newItems= new String[items.length];
            int maxCount= 0, maxStyle= 0;
                        
            for(int i = 0; i < items.length; i++) {
                Integer count= (Integer) map.get(Integer.valueOf(i));
                int val= (count == null) ? 0 : count.intValue();
                if (val > maxCount) {
                    maxCount= val;
                    maxStyle= i;
                }                
                newItems[i]= getLabelText(items[i], val, fElements.size()); 
            }
            combo.setItems(newItems);
            combo.setText(newItems[maxStyle]);
        }
	}
	
	protected static final String[] INDENT_NAMES = {
	    FormatterMessages.LineWrappingTabPage_indentation_default, 
	    FormatterMessages.LineWrappingTabPage_indentation_on_column, 
	    FormatterMessages.LineWrappingTabPage_indentation_by_one
	};
	
	
	protected static final String[] WRAPPING_NAMES = { 
	    FormatterMessages.LineWrappingTabPage_splitting_do_not_split, 
	    FormatterMessages.LineWrappingTabPage_splitting_wrap_when_necessary, // COMPACT_SPLIT 
	    FormatterMessages.LineWrappingTabPage_splitting_always_wrap_first_others_when_necessary, // COMPACT_FIRST_BREAK_SPLIT  
	    FormatterMessages.LineWrappingTabPage_splitting_wrap_always, // ONE_PER_LINE_SPLIT  
	    FormatterMessages.LineWrappingTabPage_splitting_wrap_always_indent_all_but_first, // NEXT_SHIFTED_SPLIT  
	    FormatterMessages.LineWrappingTabPage_splitting_wrap_always_except_first_only_if_necessary
	};
	

	private final Category fCompactIfCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_IF,
	    "function foo(int argument) {\n" + //$NON-NLS-1$
	    "  if (argument==0)\n" + //$NON-NLS-1$
	    "    return 0;\n" + //$NON-NLS-1$
	    "  if (argument==1)\n" + //$NON-NLS-1$
	    "    return 42;\n" + //$NON-NLS-1$
	    "  else\n" + //$NON-NLS-1$
	    "    return 43;\n" + //$NON-NLS-1$	
	    "}", //$NON-NLS-1$
	    FormatterMessages.LineWrappingTabPage_compact_if_else
	);
	

//	private final Category fTypeDeclarationSuperclassCategory= new Category(
//	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERCLASS_IN_TYPE_DECLARATION,
//	    "class Example extends OtherClass {}", //$NON-NLS-1$
//	    FormatterMessages.LineWrappingTabPage_extends_clause
//	);
//	
//
//	private final Category fTypeDeclarationSuperinterfacesCategory= new Category(
//	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_TYPE_DECLARATION,
//	    "class Example implements I1, I2, I3 {}", //$NON-NLS-1$
//	    FormatterMessages.LineWrappingTabPage_implements_clause
//	);
	
	
//	private final Category fConstructorDeclarationsParametersCategory= new Category(
//	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION,
//	    "class Example {Example(int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) { this();}" + //$NON-NLS-1$
//	    "Example() {}}", //$NON-NLS-1$
//	    FormatterMessages.LineWrappingTabPage_parameters
//	); 

	private final Category fMethodDeclarationsParametersCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION,
	    "function foo(arg1, arg2, arg3, arg4, arg5, arg6) { var x = 0; return x;}", //$NON-NLS-1$
	    FormatterMessages.LineWrappingTabPage_parameters
	); 
	
	private final Category fMessageSendArgumentsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION,
	    "function foo() {Other.bar( 100, 200, 300, 400, 500, 600, 700, 800, 900 );}", //$NON-NLS-1$
	    FormatterMessages.LineWrappingTabPage_arguments
	); 

//	private final Category fMessageSendSelectorCategory= new Category(
//	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION,
//	    "function foofoofoofoofoo(aLongParmName) {return aLongParmName.getFirstSomething();}", //$NON-NLS-1$
//	    FormatterMessages.LineWrappingTabPage_qualified_invocations
//	);
	
//	private final Category fMethodThrowsClauseCategory= new Category(
//	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_METHOD_DECLARATION, 
//	    "class Example {" + //$NON-NLS-1$
//	    "int foo() throws FirstException, SecondException, ThirdException {" + //$NON-NLS-1$
//	    "  return Other.doSomething();}}", //$NON-NLS-1$
//	    FormatterMessages.LineWrappingTabPage_throws_clause
//	);

//	private final Category fConstructorThrowsClauseCategory= new Category(
//	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_CONSTRUCTOR_DECLARATION, 
//	    "class Example {" + //$NON-NLS-1$
//	    "Example() throws FirstException, SecondException, ThirdException {" + //$NON-NLS-1$
//	    "  return Other.doSomething();}}", //$NON-NLS-1$
//	    FormatterMessages.LineWrappingTabPage_throws_clause
//	);

	
	private final Category fAllocationExpressionArgumentsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ALLOCATION_EXPRESSION,
	    "foo( new SomeClass(100, 200, 300, 400, 500, 600, 700, 800, 900 ) );", //$NON-NLS-1$
	    FormatterMessages.LineWrappingTabPage_object_allocation
	);
	
//	private final Category fQualifiedAllocationExpressionCategory= new Category (
//	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_QUALIFIED_ALLOCATION_EXPRESSION,
//	    "class Example {SomeClass foo() {return SomeOtherClass.new SomeClass(100, 200, 300, 400, 500 );}}", //$NON-NLS-1$
//		FormatterMessages.LineWrappingTabPage_qualified_object_allocation
//	);
	
	private final Category fArrayInitializerExpressionsCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_ARRAY_INITIALIZER,
	    "fArray= [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];", //$NON-NLS-1$
	    FormatterMessages.LineWrappingTabPage_array_init
	);
	
	private final Category fObjectInitializerExpressionsCategory= new Category(
		    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_ARRAY_INITIALIZER,
		    "anObject = { color:'red', wheels:4, engine:{ cylinders:4, size:2.2 } };", //$NON-NLS-1$
		    FormatterMessages.LineWrappingTabPage_object_init
		);
	
//	private final Category fExplicitConstructorArgumentsCategory= new Category(
//	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_EXPLICIT_CONSTRUCTOR_CALL,
//	    "class Example extends AnotherClass {Example() {super(100, 200, 300, 400, 500, 600, 700);}}", //$NON-NLS-1$
//	    FormatterMessages.LineWrappingTabPage_explicit_constructor_invocations
//	);

	private final Category fConditionalExpressionCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION,
	    "function exampleFunction( anArgument) {return anArgument ? 100000 : 200000;}", //$NON-NLS-1$
	    FormatterMessages.LineWrappingTabPage_conditionals
	);

	private final Category fBinaryExpressionCategory= new Category(
	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION,
	    "function foo() {" + //$NON-NLS-1$
	    "  var sum= 100 + 200 + 300 + 400 + 500 + 600 + 700 + 800;" + //$NON-NLS-1$
	    "  var product= 1 * 2 * 3 * 4 * 5 * 6 * 7 * 8 * 9 * 10;" + //$NON-NLS-1$
	    "  var val= true && false && true && false && true;" +  //$NON-NLS-1$
	    "  return product / sum;}", //$NON-NLS-1$
	    FormatterMessages.LineWrappingTabPage_binary_exprs
	);
	
//	private final Category fEnumConstArgumentsCategory= new Category(
//	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ENUM_CONSTANT,
//	    "enum Example {" + //$NON-NLS-1$
//	    "GREEN(0, 255, 0), RED(255, 0, 0)  }", //$NON-NLS-1$
//	    FormatterMessages.LineWrappingTabPage_enum_constant_arguments
//	);
	
//	private final Category fEnumDeclInterfacesCategory= new Category(
//	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_ENUM_DECLARATION,
//	    "enum Example implements A, B, C {" + //$NON-NLS-1$
//	    "}", //$NON-NLS-1$
//	    FormatterMessages.LineWrappingTabPage_enum_superinterfaces
//	);
//	
//	private final Category fEnumConstantsCategory= new Category(
//	    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ENUM_CONSTANTS,
//	    "enum Example {" + //$NON-NLS-1$
//	    "CANCELLED, RUNNING, WAITING, FINISHED }" + //$NON-NLS-1$
//	    "enum Example {" + //$NON-NLS-1$
//	    "GREEN(0, 255, 0), RED(255, 0, 0)  }", //$NON-NLS-1$
//	    FormatterMessages.LineWrappingTabPage_enum_constants
//	);
	
	private final Category fAssignmentCategory= new Category(
		    DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ASSIGNMENT,
		    "var string = \"TextTextText\";\n" + //$NON-NLS-1$
		    "function foo() {\n" + //$NON-NLS-1$
		    "    for (int i = 0; i < 10; i++) {\n" + //$NON-NLS-1$
		    "    }\n" + //$NON-NLS-1$
		    "    var s;\n" + //$NON-NLS-1$
		    "    s = \"TextTextText\";\n" + //$NON-NLS-1$
		    "}\n", //$NON-NLS-1$
		    FormatterMessages.LineWrappingTabPage_assignment_alignment
		);
	
	/**
	 * The default preview line width.
	 */
	private static int DEFAULT_PREVIEW_WINDOW_LINE_WIDTH= 40;
	
	/**
	 * The key to save the user's preview window width in the dialog settings.
	 */
	private static final String PREF_PREVIEW_LINE_WIDTH= JavaScriptUI.ID_PLUGIN + ".codeformatter.line_wrapping_tab_page.preview_line_width"; //$NON-NLS-1$
	
	/**
	 * The dialog settings.
	 */
	protected final IDialogSettings fDialogSettings;	
	
	protected TreeViewer fCategoriesViewer;
	protected Label fWrappingStylePolicy;
	protected Combo fWrappingStyleCombo;
	protected Label fIndentStylePolicy;
	protected Combo fIndentStyleCombo;
	protected Button fForceSplit;

	protected CompilationUnitPreview fPreview;

	protected Group fOptionsGroup;

	/**
	 * A collection containing the categories tree. This is used as model for the tree viewer.
	 * @see TreeViewer
	 */
	private final List fCategories;
	
	/**
	 * The category listener which makes the selection persistent.
	 */
	protected final CategoryListener fCategoryListener;
	
	/**
	 * The current selection of elements. 
	 */
	protected IStructuredSelection fSelection;
	
	/**
	 * An object containing the state for the UI.
	 */
	SelectionState fSelectionState;
	
	/**
	 * A special options store wherein the preview line width is kept.
	 */
	protected final Map fPreviewPreferences;
	
	/**
	 * The key for the preview line width.
	 */
	private final String LINE_SPLIT= DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT;


	private Composite fOptionsComposite;
	
	/**
	 * Create a new line wrapping tab page.
	 * @param modifyDialog
	 * @param workingValues
	 */
	public LineWrappingTabPage(ModifyDialog modifyDialog, Map workingValues) {
		super(modifyDialog, workingValues);

		fDialogSettings= JavaScriptPlugin.getDefault().getDialogSettings();
		
		final String previewLineWidth= fDialogSettings.get(PREF_PREVIEW_LINE_WIDTH);
		
		fPreviewPreferences= new HashMap();
		fPreviewPreferences.put(LINE_SPLIT, previewLineWidth != null ? previewLineWidth : Integer.toString(DEFAULT_PREVIEW_WINDOW_LINE_WIDTH));
		
		fCategories= createCategories();
		fCategoryListener= new CategoryListener(fCategories);
	}
	
	/**
	 * @return Create the categories tree.
	 */
	protected List createCategories() {

//		final Category classDeclarations= new Category(FormatterMessages.LineWrappingTabPage_class_decls); 
//		classDeclarations.children.add(fTypeDeclarationSuperclassCategory);
//		classDeclarations.children.add(fTypeDeclarationSuperinterfacesCategory);
		
//		final Category constructorDeclarations= new Category(null, null, FormatterMessages.LineWrappingTabPage_constructor_decls); 
//		constructorDeclarations.children.add(fConstructorDeclarationsParametersCategory);
//		constructorDeclarations.children.add(fConstructorThrowsClauseCategory);

		final Category methodDeclarations= new Category(null, null, FormatterMessages.LineWrappingTabPage_method_decls); 
		methodDeclarations.children.add(fMethodDeclarationsParametersCategory);
//		methodDeclarations.children.add(fMethodThrowsClauseCategory);

//		final Category enumDeclarations= new Category(FormatterMessages.LineWrappingTabPage_enum_decls); 
//		enumDeclarations.children.add(fEnumConstantsCategory);
//		enumDeclarations.children.add(fEnumDeclInterfacesCategory);
//		enumDeclarations.children.add(fEnumConstArgumentsCategory);
		
		final Category functionCalls= new Category(FormatterMessages.LineWrappingTabPage_function_calls); 
		functionCalls.children.add(fMessageSendArgumentsCategory);
//		functionCalls.children.add(fMessageSendSelectorCategory);
//		functionCalls.children.add(fExplicitConstructorArgumentsCategory);
		functionCalls.children.add(fAllocationExpressionArgumentsCategory);
//		functionCalls.children.add(fQualifiedAllocationExpressionCategory);
		
		final Category expressions= new Category(FormatterMessages.LineWrappingTabPage_expressions); 
		expressions.children.add(fBinaryExpressionCategory);
		expressions.children.add(fConditionalExpressionCategory);
		expressions.children.add(fArrayInitializerExpressionsCategory);
		expressions.children.add(fObjectInitializerExpressionsCategory);
		expressions.children.add(fAssignmentCategory);
		
		final Category statements= new Category(FormatterMessages.LineWrappingTabPage_statements); 
		statements.children.add(fCompactIfCategory);
		
		final List root= new ArrayList();
//		root.add(classDeclarations);
//		root.add(constructorDeclarations);
		root.add(methodDeclarations);
//		root.add(enumDeclarations);
		root.add(functionCalls);
		root.add(expressions);
		root.add(statements);
		
		return root;
	}
	
	protected void doCreatePreferences(Composite composite, int numColumns) {
	
		fOptionsComposite= composite;
		
		final Group lineWidthGroup= createGroup(numColumns, composite, FormatterMessages.LineWrappingTabPage_width_indent); 

		createNumberPref(lineWidthGroup, numColumns, FormatterMessages.LineWrappingTabPage_width_indent_option_max_line_width, DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, 0, 9999); 
		createNumberPref(lineWidthGroup, numColumns, FormatterMessages.LineWrappingTabPage_width_indent_option_default_indent_wrapped, DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION, 0, 9999);
		createNumberPref(lineWidthGroup, numColumns, FormatterMessages.LineWrappingTabPage_width_indent_option_default_indent_array, DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION_FOR_ARRAY_INITIALIZER, 0, 9999);
		createNumberPref(lineWidthGroup, numColumns, FormatterMessages.LineWrappingTabPage_width_indent_option_default_indent_objlit, DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION_FOR_OBJLIT_INITIALIZER, 0, 9999);
		
		fCategoriesViewer= new TreeViewer(composite /*categoryGroup*/, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL );
		fCategoriesViewer.setContentProvider(new ITreeContentProvider() {
			public Object[] getElements(Object inputElement) {
				return ((Collection)inputElement).toArray();
			}
			public Object[] getChildren(Object parentElement) {
				return ((Category)parentElement).children.toArray();
			}
			public Object getParent(Object element) { return null; }
			public boolean hasChildren(Object element) {
				return !((Category)element).children.isEmpty();
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
			public void dispose() {}
		});
		fCategoriesViewer.setLabelProvider(new LabelProvider());
		fCategoriesViewer.setInput(fCategories);
		
		fCategoriesViewer.setExpandedElements(fCategories.toArray());

		final GridData gd= createGridData(numColumns, GridData.FILL_BOTH, SWT.DEFAULT);
		fCategoriesViewer.getControl().setLayoutData(gd);

		fOptionsGroup = createGroup(numColumns, composite, "");  //$NON-NLS-1$
		
		// label "Select split style:"
		fWrappingStylePolicy= createLabel(numColumns, fOptionsGroup, FormatterMessages.LineWrappingTabPage_wrapping_policy_label_text); 
	
		// combo SplitStyleCombo
		fWrappingStyleCombo= new Combo(fOptionsGroup, SWT.SINGLE | SWT.READ_ONLY);
		fWrappingStyleCombo.setItems(WRAPPING_NAMES);
		fWrappingStyleCombo.setLayoutData(createGridData(numColumns, GridData.HORIZONTAL_ALIGN_FILL, 0));
		
		// label "Select indentation style:"
		fIndentStylePolicy= createLabel(numColumns, fOptionsGroup, FormatterMessages.LineWrappingTabPage_indentation_policy_label_text); 
		
		// combo SplitStyleCombo
		fIndentStyleCombo= new Combo(fOptionsGroup, SWT.SINGLE | SWT.READ_ONLY);
		fIndentStyleCombo.setItems(INDENT_NAMES);
		fIndentStyleCombo.setLayoutData(createGridData(numColumns, GridData.HORIZONTAL_ALIGN_FILL, 0));
		
		// button "Force split"
		fForceSplit= new Button(fOptionsGroup, SWT.CHECK);
		fForceSplit.setLayoutData(createGridData(numColumns - 1, GridData.HORIZONTAL_ALIGN_BEGINNING, SWT.DEFAULT));
		fForceSplit.setText(FormatterMessages.LineWrappingTabPage_force_split_checkbox_text); 
		
		Preference expressionWrapPositionPreference= createCheckboxPref(fOptionsGroup, 1, FormatterMessages.LineWrappingTabPage_binary_expression_wrap_operator, DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_BINARY_OPERATOR, FALSE_TRUE);
		Control control= expressionWrapPositionPreference.getControl();
		control.setVisible(false);
		((GridData)control.getLayoutData()).exclude= true;
		fBinaryExpressionCategory.addPreference(expressionWrapPositionPreference);
		
		// selection state object
		fSelectionState= new SelectionState();
		
	}
	
		
	protected Composite doCreatePreviewPane(Composite composite, int numColumns) {
		
		super.doCreatePreviewPane(composite, numColumns);
		
		final NumberPreference previewLineWidth= new NumberPreference(composite, numColumns / 2, fPreviewPreferences, LINE_SPLIT,
		    0, 9999, FormatterMessages.LineWrappingTabPage_line_width_for_preview_label_text); 
		fDefaultFocusManager.add(previewLineWidth);
		previewLineWidth.addObserver(fUpdater);
		previewLineWidth.addObserver(new Observer() {
			public void update(Observable o, Object arg) {
				fDialogSettings.put(PREF_PREVIEW_LINE_WIDTH, (String)fPreviewPreferences.get(LINE_SPLIT));
			}
		});
		
		return composite;
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.wst.jsdt.internal.ui.preferences.formatter.ModifyDialogTabPage#doCreateJavaPreview(org.eclipse.swt.widgets.Composite)
     */
    protected JavaPreview doCreateJavaPreview(Composite parent) {
        fPreview= new CompilationUnitPreview(fWorkingValues, parent);
        return fPreview;
    }

	
	protected void initializePage() {
		
		fCategoriesViewer.addSelectionChangedListener(fCategoryListener);
		fCategoriesViewer.addDoubleClickListener(fCategoryListener);
		
		fForceSplit.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				forceSplitChanged(fForceSplit.getSelection());
			}
		});
		fIndentStyleCombo.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				indentStyleChanged(((Combo)e.widget).getSelectionIndex());
			}
		});
		fWrappingStyleCombo.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				wrappingStyleChanged(((Combo)e.widget).getSelectionIndex());
			}
		});
		
		fCategoryListener.restoreSelection();
		
		fDefaultFocusManager.add(fCategoriesViewer.getControl());
		fDefaultFocusManager.add(fWrappingStyleCombo);
		fDefaultFocusManager.add(fIndentStyleCombo);
		fDefaultFocusManager.add(fForceSplit);
	}
	
	protected void doUpdatePreview() {
		super.doUpdatePreview();
		final Object normalSetting= fWorkingValues.get(LINE_SPLIT);
		fWorkingValues.put(LINE_SPLIT, fPreviewPreferences.get(LINE_SPLIT));
		fPreview.update();
		fWorkingValues.put(LINE_SPLIT, normalSetting);
	}
	
	protected void setPreviewText(String text) {
		final Object normalSetting= fWorkingValues.get(LINE_SPLIT);
		fWorkingValues.put(LINE_SPLIT, fPreviewPreferences.get(LINE_SPLIT));
		fPreview.setPreviewText(text);
		fWorkingValues.put(LINE_SPLIT, normalSetting);
	}
	
	protected void forceSplitChanged(boolean forceSplit) {
	    Iterator iterator= fSelectionState.fElements.iterator();
	    String currentKey;
        while (iterator.hasNext()) {
            currentKey= ((Category)iterator.next()).key;
            try {
                changeForceSplit(currentKey, forceSplit);
            } catch (IllegalArgumentException e) {
    			fWorkingValues.put(currentKey, DefaultCodeFormatterConstants.createAlignmentValue(forceSplit, DefaultCodeFormatterConstants.WRAP_NO_SPLIT, DefaultCodeFormatterConstants.INDENT_DEFAULT));
    			JavaScriptPlugin.log(new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.OK, 
    			        Messages.format(FormatterMessages.LineWrappingTabPage_error_invalid_value, currentKey), e)); 
    		}
        }
        fSelectionState.refreshState(fSelection);
	}
	
	private void changeForceSplit(String currentKey, boolean forceSplit) throws IllegalArgumentException{
		String value= (String)fWorkingValues.get(currentKey);
		value= DefaultCodeFormatterConstants.setForceWrapping(value, forceSplit);
		if (value == null)
		    throw new IllegalArgumentException();
		fWorkingValues.put(currentKey, value);
	}
	
	protected void wrappingStyleChanged(int wrappingStyle) {
	       Iterator iterator= fSelectionState.fElements.iterator();
	       String currentKey;
	        while (iterator.hasNext()) {
	        	currentKey= ((Category)iterator.next()).key;
	        	try {
	        	    changeWrappingStyle(currentKey, wrappingStyle);
	        	} catch (IllegalArgumentException e) {
	    			fWorkingValues.put(currentKey, DefaultCodeFormatterConstants.createAlignmentValue(false, wrappingStyle, DefaultCodeFormatterConstants.INDENT_DEFAULT));
	    			JavaScriptPlugin.log(new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.OK, 
	    			        Messages.format(FormatterMessages.LineWrappingTabPage_error_invalid_value, currentKey), e)); 
	        	}
	        }
	        fSelectionState.refreshState(fSelection);
	}
	
	private void changeWrappingStyle(String currentKey, int wrappingStyle) throws IllegalArgumentException {
	    String value= (String)fWorkingValues.get(currentKey);
		value= DefaultCodeFormatterConstants.setWrappingStyle(value, wrappingStyle);
		if (value == null)
		    throw new IllegalArgumentException();
		fWorkingValues.put(currentKey, value);
	}
	
	protected void indentStyleChanged(int indentStyle) {
	    Iterator iterator= fSelectionState.fElements.iterator();
	    String currentKey;
        while (iterator.hasNext()) {
            currentKey= ((Category)iterator.next()).key;
        	try {
            	changeIndentStyle(currentKey, indentStyle);
        	} catch (IllegalArgumentException e) {
    			fWorkingValues.put(currentKey, DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_NO_SPLIT, indentStyle));
    			JavaScriptPlugin.log(new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.OK, 
    			        Messages.format(FormatterMessages.LineWrappingTabPage_error_invalid_value, currentKey), e)); 
    		}
        }
        fSelectionState.refreshState(fSelection);
	}
	
	private void changeIndentStyle(String currentKey, int indentStyle) throws IllegalArgumentException{
		String value= (String)fWorkingValues.get(currentKey);
		value= DefaultCodeFormatterConstants.setIndentStyle(value, indentStyle);
		if (value == null)
		    throw new IllegalArgumentException();
		fWorkingValues.put(currentKey, value);
	}
    
    protected void updateControlEnablement(boolean inhomogenous, int wrappingStyle) {
	    boolean doSplit= wrappingStyle != DefaultCodeFormatterConstants.WRAP_NO_SPLIT;
	    fIndentStylePolicy.setEnabled(true);
	    fIndentStyleCombo.setEnabled(inhomogenous || doSplit);
	    fForceSplit.setEnabled(inhomogenous || doSplit);
	}
}
