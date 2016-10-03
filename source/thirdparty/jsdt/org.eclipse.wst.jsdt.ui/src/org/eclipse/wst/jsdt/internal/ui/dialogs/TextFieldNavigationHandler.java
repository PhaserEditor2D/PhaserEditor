/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.commands.contexts.ContextManager;
import org.eclipse.jface.bindings.BindingManager;
import org.eclipse.jface.bindings.Scheme;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.text.JavaWordIterator;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

import com.ibm.icu.text.BreakIterator;

/**
 * Support for camelCase-aware sub-word navigation in dialog fields.
 */
public class TextFieldNavigationHandler {
	
	public static void install(Text text) {
		if (isSubWordNavigationEnabled())
			new FocusHandler(new TextNavigable(text));
	}

	public static void install(StyledText styledText) {
		if (isSubWordNavigationEnabled())
			new FocusHandler(new StyledTextNavigable(styledText));
	}
	
	public static void install(Combo combo) {
		if (isSubWordNavigationEnabled())
			new FocusHandler(new ComboNavigable(combo));
	}
	
	private static boolean isSubWordNavigationEnabled() {
		IPreferenceStore preferenceStore= JavaScriptPlugin.getDefault().getCombinedPreferenceStore();
		return preferenceStore.getBoolean(PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION);
	}
	
	private abstract static class WorkaroundNavigable extends Navigable {
		/* workarounds for:
		 * - bug 103630: Add API: Combo#getCaretPosition()
		 * - bug 106024: Text#setSelection(int, int) does not handle start > end with SWT.SINGLE
		 */ 
		Point fLastSelection;
		int fCaretPosition;
		
		void selectionChanged() {
			Point selection= getSelection();
			if (selection.equals(fLastSelection)) {
				// leave caret position
			} else if (selection.x == selection.y) { //empty range
				fCaretPosition= selection.x;
			} else if (fLastSelection.y == selection.y) {
				fCaretPosition= selection.x; //same end -> assume caret at start
			} else {
				fCaretPosition= selection.y;
			}
			fLastSelection= selection;
		}
	}
	
	private abstract static class Navigable {
		public abstract Control getControl();
		
		public abstract String getText();

		public abstract void setText(String text);
		
		public abstract Point getSelection();

		public abstract void setSelection(int start, int end);
		
		public abstract int getCaretPosition();
	}
	
	private static class TextNavigable extends WorkaroundNavigable {
		static final boolean BUG_106024_TEXT_SELECTION=
				"win32".equals(SWT.getPlatform()) //$NON-NLS-1$
				// on carbon, getCaretPosition() always returns getSelection().x
				|| "carbon".equals(SWT.getPlatform()); //$NON-NLS-1$
		
		private final Text fText;
		
		public TextNavigable(Text text) {
			fText= text;
			// workaround for bug 106024:
			if (BUG_106024_TEXT_SELECTION) {
				fLastSelection= getSelection();
				fCaretPosition= fLastSelection.y;
				fText.addKeyListener(new KeyAdapter() {
					public void keyReleased(KeyEvent e) {
						selectionChanged();
					}
				});
				fText.addMouseListener(new MouseAdapter() {
					public void mouseUp(MouseEvent e) {
						selectionChanged();
					}
				});
			}
		}
		
		public Control getControl() {
			return fText;
		}

		public String getText() {
			return fText.getText();
		}

		public void setText(String text) {
			fText.setText(text);
		}
		
		public Point getSelection() {
			return fText.getSelection();
		}

		public int getCaretPosition() {
			if (BUG_106024_TEXT_SELECTION) {
				selectionChanged();
				return fCaretPosition;
			} else {
				return fText.getCaretPosition();
			}
		}

		public void setSelection(int start, int end) {
			fText.setSelection(start, end);
		}
	}
	
	private static class StyledTextNavigable extends Navigable {
		private final StyledText fStyledText;
		
		public StyledTextNavigable(StyledText styledText) {
			fStyledText= styledText;
		}
		
		public Control getControl() {
			return fStyledText;
		}
		
		public String getText() {
			return fStyledText.getText();
		}
		
		public void setText(String text) {
			fStyledText.setText(text);
		}
		
		public Point getSelection() {
			return fStyledText.getSelection();
		}
		
		public int getCaretPosition() {
			return fStyledText.getCaretOffset();
		}
		
		public void setSelection(int start, int end) {
			fStyledText.setSelection(start, end);
		}
	}
	
	private static class ComboNavigable extends WorkaroundNavigable {
		private final Combo fCombo;
		
		public ComboNavigable(Combo combo) {
			fCombo= combo;
			// workaround for bug 103630:
			fLastSelection= getSelection();
			fCaretPosition= fLastSelection.y;
			fCombo.addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent e) {
					selectionChanged();
				}
			});
			fCombo.addMouseListener(new MouseAdapter() {
				public void mouseUp(MouseEvent e) {
					selectionChanged();
				}
			});
		}
		
		public Control getControl() {
			return fCombo;
		}

		public String getText() {
			return fCombo.getText();
		}
		
		public void setText(String text) {
			fCombo.setText(text);
		}
		
		public Point getSelection() {
			return fCombo.getSelection();
		}
		
		public int getCaretPosition() {
			selectionChanged();
			return fCaretPosition;
//			return fCombo.getCaretPosition(); // not available: bug 103630
		}
		
		public void setSelection(int start, int end) {
			fCombo.setSelection(new Point(start, end));
		}
	}
	
	private static class FocusHandler implements FocusListener {
		
		private static final String EMPTY_TEXT= ""; //$NON-NLS-1$
		
		private final JavaWordIterator fIterator;
		private final Navigable fNavigable;
		private KeyAdapter fKeyListener;
		
		private FocusHandler(Navigable navigable) {
			fIterator= new JavaWordIterator();
			fNavigable= navigable;
			
			Control control= navigable.getControl();
			control.addFocusListener(this);
			if (control.isFocusControl())
				activate();
			control.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					deactivate();
				}
			});
		}

		public void focusGained(FocusEvent e) {
			activate();
		}

		public void focusLost(FocusEvent e) {
			deactivate();
		}

		private void activate() {
			fNavigable.getControl().addKeyListener(getKeyListener());
		}
		
		private void deactivate() {
			if (fKeyListener != null) {
				Control control= fNavigable.getControl();
				if (! control.isDisposed())
					control.removeKeyListener(fKeyListener);
				fKeyListener= null;
			}
		}
		
		private KeyAdapter getKeyListener() {
			if (fKeyListener == null) {
				fKeyListener= new KeyAdapter() {
					private final boolean IS_WORKAROUND= (fNavigable instanceof ComboNavigable)
							|| (fNavigable instanceof TextNavigable && TextNavigable.BUG_106024_TEXT_SELECTION);
					private List/*<Submission>*/ fSubmissions;

					public void keyPressed(KeyEvent e) {
						if (IS_WORKAROUND) {
							if (e.keyCode == SWT.ARROW_LEFT && e.stateMask == SWT.MOD2) {
								int caretPosition= fNavigable.getCaretPosition();
								if (caretPosition != 0) {
									Point selection= fNavigable.getSelection();
									if (caretPosition == selection.x)
										fNavigable.setSelection(selection.y, caretPosition - 1);
									else
										fNavigable.setSelection(selection.x, caretPosition - 1);
								}
								e.doit= false;
								return;
								
							} else if (e.keyCode == SWT.ARROW_RIGHT && e.stateMask == SWT.MOD2) {
								String text= fNavigable.getText();
								int caretPosition= fNavigable.getCaretPosition();
								if (caretPosition != text.length()) {
									Point selection= fNavigable.getSelection();
									if (caretPosition == selection.y)
										fNavigable.setSelection(selection.x, caretPosition + 1);
									else
										fNavigable.setSelection(selection.y, caretPosition + 1);
								}
								e.doit= false;
								return;
							}
						}
						int accelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(e);
						KeySequence keySequence = KeySequence.getInstance(SWTKeySupport.convertAcceleratorToKeyStroke(accelerator));
						getSubmissions();
						for (Iterator iter= getSubmissions().iterator(); iter.hasNext();) {
							Submission submission= (Submission) iter.next();
							TriggerSequence[] triggerSequences= submission.getTriggerSequences();
							for (int i= 0; i < triggerSequences.length; i++) {
								if (triggerSequences[i].equals(keySequence)) { // XXX does not work for multi-stroke bindings
									e.doit= false;
									submission.execute();
									return;
								}
							}
						}
					}
					
					private List/*<Submission>*/ getSubmissions() {
						if (fSubmissions != null)
							return fSubmissions;
						
						fSubmissions= new ArrayList();
						
						ICommandService commandService= (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
						IBindingService bindingService= (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
						if (commandService == null || bindingService == null)
							return fSubmissions;
						
						// Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=184502 ,
						// similar to CodeAssistAdvancedConfigurationBlock.getKeyboardShortcut(..):
						BindingManager localBindingManager= new BindingManager(new ContextManager(), new CommandManager());
						final Scheme[] definedSchemes= bindingService.getDefinedSchemes();
						if (definedSchemes != null) {
							try {
								for (int i = 0; i < definedSchemes.length; i++) {
									Scheme scheme= definedSchemes[i];
									Scheme localSchemeCopy= localBindingManager.getScheme(scheme.getId());
									localSchemeCopy.define(scheme.getName(), scheme.getDescription(), scheme.getParentId());
								}
							} catch (final NotDefinedException e) {
								JavaScriptPlugin.log(e);
							}
						}
						localBindingManager.setLocale(bindingService.getLocale());
						localBindingManager.setPlatform(bindingService.getPlatform());
						
						localBindingManager.setBindings(bindingService.getBindings());
						try {
							Scheme activeScheme= bindingService.getActiveScheme();
							if (activeScheme != null)
								localBindingManager.setActiveScheme(activeScheme);
						} catch (NotDefinedException e) {
							JavaScriptPlugin.log(e);
						}
						
						fSubmissions.add(new Submission(getKeyBindings(localBindingManager, commandService, ITextEditorActionDefinitionIds.SELECT_WORD_NEXT)) {
							public void execute() {
								fIterator.setText(fNavigable.getText());
								int caretPosition= fNavigable.getCaretPosition();
								int newCaret= fIterator.following(caretPosition);
								if (newCaret != BreakIterator.DONE) {
									Point selection= fNavigable.getSelection();
									if (caretPosition == selection.y)
										fNavigable.setSelection(selection.x, newCaret);
									else
										fNavigable.setSelection(selection.y, newCaret);
								}
								fIterator.setText(EMPTY_TEXT);
							}
						});
						fSubmissions.add(new Submission(getKeyBindings(localBindingManager, commandService, ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS)) {
							public void execute() {
								fIterator.setText(fNavigable.getText());
								int caretPosition= fNavigable.getCaretPosition();
								int newCaret= fIterator.preceding(caretPosition);
								if (newCaret != BreakIterator.DONE) {
									Point selection= fNavigable.getSelection();
									if (caretPosition == selection.x)
										fNavigable.setSelection(selection.y, newCaret);
									else
										fNavigable.setSelection(selection.x, newCaret);
								}
								fIterator.setText(EMPTY_TEXT);
							}
						});
						fSubmissions.add(new Submission(getKeyBindings(localBindingManager, commandService, ITextEditorActionDefinitionIds.WORD_NEXT)) {
							public void execute() {
								fIterator.setText(fNavigable.getText());
								int caretPosition= fNavigable.getCaretPosition();
								int newCaret= fIterator.following(caretPosition);
								if (newCaret != BreakIterator.DONE)
									fNavigable.setSelection(newCaret, newCaret);
								fIterator.setText(EMPTY_TEXT);
							}
						});
						fSubmissions.add(new Submission(getKeyBindings(localBindingManager, commandService, ITextEditorActionDefinitionIds.WORD_PREVIOUS)) {
							public void execute() {
								fIterator.setText(fNavigable.getText());
								int caretPosition= fNavigable.getCaretPosition();
								int newCaret= fIterator.preceding(caretPosition);
								if (newCaret != BreakIterator.DONE)
									fNavigable.setSelection(newCaret, newCaret);
								fIterator.setText(EMPTY_TEXT);
							}
						});
						fSubmissions.add(new Submission(getKeyBindings(localBindingManager, commandService, ITextEditorActionDefinitionIds.DELETE_NEXT_WORD)) {
							public void execute() {
								Point selection= fNavigable.getSelection();
								String text= fNavigable.getText();
								int start;
								int end;
								if (selection.x != selection.y) {
									start= selection.x;
									end= selection.y;
								} else {
									fIterator.setText(text);
									start= fNavigable.getCaretPosition();
									end= fIterator.following(start);
									fIterator.setText(EMPTY_TEXT);
									if (end == BreakIterator.DONE)
										return;
								}
								fNavigable.setText(text.substring(0, start) + text.substring(end));
								fNavigable.setSelection(start, start);
							}
						});
						fSubmissions.add(new Submission(getKeyBindings(localBindingManager, commandService, ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD)) {
							public void execute() {
								Point selection= fNavigable.getSelection();
								String text= fNavigable.getText();
								int start;
								int end;
								if (selection.x != selection.y) {
									start= selection.x;
									end= selection.y;
								} else {
									fIterator.setText(text);
									end= fNavigable.getCaretPosition();
									start= fIterator.preceding(end);
									fIterator.setText(EMPTY_TEXT);
									if (start == BreakIterator.DONE)
										return;
								}
								fNavigable.setText(text.substring(0, start) + text.substring(end));
								fNavigable.setSelection(start, start);
							}
						});
						
						return fSubmissions;
					}

					private TriggerSequence[] getKeyBindings(BindingManager localBindingManager, ICommandService commandService, String commandID) {
						Command command= commandService.getCommand(commandID);
						ParameterizedCommand pCmd= new ParameterizedCommand(command, null);
						return localBindingManager.getActiveBindingsDisregardingContextFor(pCmd);
					}
					
				};
			}
			return fKeyListener;
		}
	}
	
	private abstract static class Submission {
		private TriggerSequence[] fTriggerSequences;
		
		public Submission(TriggerSequence[] triggerSequences) {
			fTriggerSequences= triggerSequences;
		}
		
		public TriggerSequence[] getTriggerSequences() {
			return fTriggerSequences;
		}
		
		public abstract void execute();
	}
	
}
