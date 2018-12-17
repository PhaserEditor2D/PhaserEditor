// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.chains.ui.views;

import static java.lang.System.out;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.core.grammar.IToken;
import org.eclipse.tm4e.core.grammar.ITokenizeLineResult;
import org.eclipse.tm4e.registry.TMEclipseRegistryPlugin;
import org.eclipse.tm4e.ui.TMUIPlugin;
import org.eclipse.tm4e.ui.internal.themes.ThemeManager;
import org.eclipse.tm4e.ui.themes.ITheme;
import org.eclipse.ui.part.ViewPart;

import phasereditor.chains.core.ChainItem;
import phasereditor.chains.core.ChainsCore;
import phasereditor.chains.core.ChainsModel;
import phasereditor.chains.core.Line;
import phasereditor.chains.core.Match;
import phasereditor.inspect.core.examples.PhaserExampleModel;
import phasereditor.inspect.core.jsdoc.JsdocRenderer;
import phasereditor.inspect.ui.handlers.RunPhaserExampleHandler;
import phasereditor.inspect.ui.handlers.ShowPhaserJsdocHandler;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.SwtRM;

public class ChainsView extends ViewPart implements IPropertyChangeListener {
	public static final String ID = "phasereditor.chains.ui.views.chains";
	private Text _queryText;
	TableViewer _viewer;
	ChainsModel _chainsModel;
	private ToolBar _filterToolbar;
	private ChainsStyledCellLabelProvider _cellLabelProvider;

	protected Font _italic;
	protected Font _font;
	protected Font _codeFont;
	protected Color _decorationsColor;
	protected ITheme _theme;
	protected IGrammar _grammar;
	protected Color _matchBorderColor;
	private Color _qualifierColor;

	class ChainsStyledCellLabelProvider extends StyledCellLabelProvider {

		public ChainsStyledCellLabelProvider() {
			updateStyleValues();
		}

		@Override
		public String getToolTipText(Object element) {
			return element.toString();
		}

		private void updateExampleLabel(ViewerCell cell) {
			Match match = (Match) cell.getElement();
			Line line = null;
			String text;
			if (match.item instanceof Line) {
				line = (Line) match.item;
				text = line.text + " @ " + line.example.getFullName() + " [" + line.linenum + "]";
			} else {
				text = ((PhaserExampleModel) match.item).getFullName();
			}

			// cell.setBackground(_theme.getEditorBackground());
			cell.setStyleRanges(match.styles);
			cell.setText(text);
			cell.setImage(EditorSharedImages.getImage(IEditorSharedImages.IMG_GAME_CONTROLLER));
		}

		@Override
		public void update(ViewerCell cell) {
			Match match = (Match) cell.getElement();
			if (match.item instanceof ChainItem) {
				updateChainLabel(cell);
			} else {
				updateExampleLabel(cell);
			}
			super.update(cell);
		}

		private void updateChainLabel(ViewerCell cell) {
			Match match = (Match) cell.getElement();

			ChainItem chain = (ChainItem) match.item;

			// cell.setBackground(_theme.getEditorBackground());
			cell.setForeground(_theme.getEditorForeground());
			cell.setText(match.toString());
			cell.setStyleRanges(match.styles);
			cell.setImage(JsdocRenderer.getInstance().getImage(chain.getPhaserMember()));
		}
	}

	public ChainsView() {
	}

	protected void updateStyleValues() {
		_font = JFaceResources.getFont(JFaceResources.DEFAULT_FONT);

		FontData fd = _font.getFontData()[0];
		_italic = SwtRM.getFont(fd.getName(), fd.getHeight(), SWT.ITALIC);

		_codeFont = JFaceResources.getFont(JFaceResources.TEXT_FONT);

		_decorationsColor = JFaceResources.getColorRegistry().get(JFacePreferences.DECORATIONS_COLOR);
		_qualifierColor = JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR);
		_matchBorderColor = JFaceResources.getColorRegistry().get(JFacePreferences.COUNTER_COLOR);

		out.println("Is dark Eclipse theme: " + ThemeManager.getInstance().isDarkEclipseTheme());

		_grammar = TMEclipseRegistryPlugin.getGrammarRegistryManager().getGrammarForScope("source.js");
		_theme = TMUIPlugin.getThemeManager().getThemeForScope("source.js");
		out.println("TM4 theme : " + _theme.getId() + " " + _theme.getName());
		
		queryTextModified();
	}

	/**
	 * Create contents of the view part.
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gl_composite = new GridLayout(1, false);
		gl_composite.marginHeight = 0;
		gl_composite.marginWidth = 0;
		container.setLayout(gl_composite);
		{

			Composite topComposite = new Composite(container, SWT.NONE);
			topComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			GridLayout gl_layout = new GridLayout(2, false);
			gl_layout.marginWidth = 0;
			gl_layout.marginHeight = 0;
			topComposite.setLayout(gl_layout);

			_queryText = new Text(topComposite, SWT.BORDER);
			_queryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			_queryText.setText("add.*(");

			_filterToolbar = new ToolBar(topComposite, SWT.NONE);
			_filterToolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));

			{
				{
					_viewer = new TableViewer(container, SWT.FULL_SELECTION);
					_viewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
					_viewer.addDoubleClickListener(new IDoubleClickListener() {
						@Override
						public void doubleClick(DoubleClickEvent event) {
							IStructuredSelection selection = (IStructuredSelection) event.getSelection();

							Match match = (Match) ((StructuredSelection) event.getSelection()).getFirstElement();
							if (match.item instanceof ChainItem) {
								ShowPhaserJsdocHandler.run(selection);
							} else {
								RunPhaserExampleHandler.run(selection);
							}
						}
					});
					Table chainsTable = _viewer.getTable();
					chainsTable.setLinesVisible(true);
					chainsTable.setHeaderVisible(false);
					{
						TableViewerColumn tableViewerColumn = new TableViewerColumn(_viewer, SWT.NONE);
						_cellLabelProvider = new ChainsStyledCellLabelProvider();
						tableViewerColumn.setLabelProvider(_cellLabelProvider);
						TableColumn column = tableViewerColumn.getColumn();
						column.setWidth(1000);
					}
					_viewer.setContentProvider(new ArrayContentProvider());
				}
			}

			_queryText.addModifyListener(new ModifyListener() {

				@Override
				public void modifyText(ModifyEvent e) {
					queryTextModified();
				}

			});
		}

		createActions();
		initializeToolBar();
		initializeMenu();

		afterCreateWidgets();
	}

	AtomicInteger _token = new AtomicInteger(0);
	private Action _showChainsAction;
	private Action _showExamplesAction;
	private IPreferenceChangeListener _tmPrefsListener;
	private IPropertyChangeListener _jfaceListener;

	protected void queryTextModified() {

		// this method is executed async so we should check this
		if (_queryText.isDisposed()) {
			return;
		}

		new Thread(this::searchAndUpdateTables).start();

	}

	private void afterCreateWidgets() {
		_viewer.setInput(new Object[0]);

		new Job("Building chains...") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				_chainsModel = ChainsCore.getChainsModel();

				queryTextModified();

				return Status.OK_STATUS;
			}
		}.schedule();

		// PhaserEditorUI.refreshViewerWhenPreferencesChange(ChainsUI.getPreferenceStore(),
		// _viewer);
		// InspectUI.installJsdocTooltips(_chainsViewer);

		getViewSite().setSelectionProvider(_viewer);

		ToolBarManager manager = new ToolBarManager(_filterToolbar);
		manager.add(_showChainsAction);
		manager.add(_showExamplesAction);
		manager.update(true);

		_tmPrefsListener = new IPreferenceChangeListener() {

			@Override
			public void preferenceChange(PreferenceChangeEvent event) {
				updateFromPreferencesChange();
			}
		};

		_jfaceListener = new IPropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent event) {
				switch (event.getProperty()) {
				case JFacePreferences.DECORATIONS_COLOR:
				case JFacePreferences.QUALIFIER_COLOR:
				case JFacePreferences.COUNTER_COLOR:
					updateFromPreferencesChange();
					break;
				default:
					break;
				}
			}
		};

		getTMPreferencesNode().addPreferenceChangeListener(_tmPrefsListener);
		getE4CSSThemePreferencesNode().addPreferenceChangeListener(_tmPrefsListener);
		JFaceResources.getColorRegistry().addListener(_jfaceListener);
	}

	private static IEclipsePreferences getTMPreferencesNode() {
		return InstanceScope.INSTANCE.getNode(TMUIPlugin.PLUGIN_ID);
	}

	private static IEclipsePreferences getE4CSSThemePreferencesNode() {
		return InstanceScope.INSTANCE.getNode("org.eclipse.e4.ui.css.swt.theme");
	}

	@Override
	public void dispose() {

		getTMPreferencesNode().removePreferenceChangeListener(_tmPrefsListener);
		getE4CSSThemePreferencesNode().removePreferenceChangeListener(_tmPrefsListener);
		JFaceResources.getColorRegistry().removeListener(_jfaceListener);

		super.dispose();
	}

	/**
	 * Create the actions.
	 */
	private void createActions() {
		_showChainsAction = new Action("Show Chains", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_METHPUB_OBJ));
				setChecked(true);
			}

			@Override
			public void run() {
				searchAndUpdateTables();
			}
		};

		_showExamplesAction = new Action("Show Examples", IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_GAME_CONTROLLER));
				setChecked(true);
			}

			@Override
			public void run() {
				searchAndUpdateTables();
			}

		};
	}

	/**
	 * Initialize the toolbar.
	 */
	private void initializeToolBar() {
		@SuppressWarnings("unused")
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
	}

	/**
	 * Initialize the menu.
	 */
	private void initializeMenu() {
		@SuppressWarnings("unused")
		IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
	}

	@Override
	public void setFocus() {
		_queryText.setFocus();
	}

	protected Match getSelectedChainItemMatch() {
		return (Match) ((IStructuredSelection) _viewer.getSelection()).getFirstElement();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IContextProvider.class)) {
			return new IContextProvider() {

				@Override
				public String getSearchExpression(Object target) {
					return null;
				}

				@Override
				public int getContextChangeMask() {
					return NONE;
				}

				@Override
				public IContext getContext(Object target) {
					IContext context = HelpSystem.getContext("phasereditor.help.chains");
					return context;
				}
			};
		}
		return super.getAdapter(adapter);
	}

	void searchAndUpdateTables() {
		int token = _token.incrementAndGet();

		String[] query = new String[1];

		getSite().getShell().getDisplay().syncExec(() -> {
			query[0] = _queryText.getText();
		});

		int chainsLimit = 100;

		if (_chainsModel == null) {
			return;
		}

		List<Match> list1 = new ArrayList<>();
		List<Match> list2 = new ArrayList<>();

		if (_showChainsAction.isChecked()) {
			list1 = _chainsModel.searchChains(query[0], chainsLimit);
			prepareChainsMatches(list1);
		}

		if (_showExamplesAction.isChecked()) {
			list2 = _chainsModel.searchExamples(query[0], chainsLimit);
			prepareExamplesMatches(list2);
		}

		List<Match> matches = new ArrayList<>();

		matches.addAll(list1);
		matches.addAll(list2);

		if (_token.get() != token) {
			return;
		}

		swtRun(() -> {
			_viewer.setInput(matches);
		});
	}

	/**
	 * @param list1
	 */
	private void prepareChainsMatches(List<Match> matches) {
		for (Match match : matches) {
			ChainItem chain = (ChainItem) match.item;

			String text = match.toString();

			StyleRange[] ranges;

			StyleRange selRange = new StyleRange(match.start, match.length, null, null);
			selRange.font = _font;
			selRange.borderColor = _matchBorderColor;
			selRange.borderStyle = SWT.BORDER_SOLID;

			StyleRange allRange = new StyleRange(0, text.length(), null, null);
			allRange.font = _font;
			StyleRange returnTypeRange = new StyleRange();
			StyleRange declTypeRange = new StyleRange();

			{
				int index = chain.getReturnTypeIndex();
				if (index > 0) {
					int len = chain.getDisplay().length() - index;
					if (chain.getDeclTypeIndex() > 0) {
						len = chain.getDeclTypeIndex() - index;
					}
					returnTypeRange = new StyleRange(index, len, _decorationsColor, null);
				}
			}

			{
				int index = chain.getDeclTypeIndex();
				if (index > 0) {
					int len = chain.getDisplay().length() - index;
					declTypeRange = new StyleRange(index, len, _qualifierColor, null);
				}
			}

			if (chain.getDepth() > 0) {
				StyleRange italicRange = new StyleRange();
				italicRange.font = _italic;
				italicRange.start = 0;
				italicRange.length = text.length();
				ranges = new StyleRange[] { allRange, italicRange, returnTypeRange, declTypeRange, selRange };
			} else {
				ranges = new StyleRange[] { allRange, returnTypeRange, declTypeRange, selRange };
			}

			match.styles = ranges;
		}
	}

	private void prepareExamplesMatches(List<Match> matches) {
		for (Match match : matches) {
			Line line = null;
			String text;
			String code = null;
			int secondaryColorIndex;
			if (match.item instanceof Line) {
				line = (Line) match.item;
				code = line.text;
				text = line.text + " @ " + line.example.getFullName() + " [" + line.linenum + "]";
				secondaryColorIndex = line.text.length();
			} else {
				secondaryColorIndex = 0;
				text = ((PhaserExampleModel) match.item).getFullName();
			}

			StyleRange secondaryRange = new StyleRange(secondaryColorIndex, text.length() - secondaryColorIndex,
					_qualifierColor, null);

			List<StyleRange> ranges = new ArrayList<>();

			ranges.add(secondaryRange);

			if (code != null) {

				List<StyleRange> syntaxRanges = new ArrayList<>();

				parseCode(code, syntaxRanges);

				List<StyleRange> list = new ArrayList<>();

				for (int i = 0; i < code.length(); i++) {
					StyleRange mergeRange = new StyleRange();
					mergeRange.font = _codeFont;
					mergeRange.start = i;
					mergeRange.length = 1;
					mergeRange.foreground = _theme.getEditorForeground();

					if (i >= match.start && i < match.start + match.length) {
						mergeRange.borderColor = _matchBorderColor;
						mergeRange.borderStyle = SWT.BORDER_SOLID;
					}

					for (StyleRange syntaxRange : syntaxRanges) {
						if (i >= syntaxRange.start && i < syntaxRange.start + syntaxRange.length) {
							mergeRange.foreground = syntaxRange.foreground;
						}
					}

					list.add(mergeRange);
				}

				ranges.addAll(list);
			}

			match.styles = ranges.toArray(new StyleRange[ranges.size()]);
		}

	}

	private void parseCode(String line, List<StyleRange> ranges) {
		try {
			ITokenizeLineResult result;

			// synchronized (_grammar) {
			result = _grammar.tokenizeLine(line);
			// }

			for (IToken token : result.getTokens()) {
				for (String scope : token.getScopes()) {
					org.eclipse.jface.text.rules.IToken themeToken = _theme.getToken(scope);
					if (themeToken != null) {
						Object data = themeToken.getData();
						if (data != null && data instanceof TextAttribute) {
							TextAttribute attr = (TextAttribute) data;
							if (attr.getForeground() != null || attr.getBackground() != null) {
								StyleRange range = new StyleRange(token.getStartIndex(),
										token.getEndIndex() - token.getStartIndex(), attr.getForeground(),
										attr.getBackground());
								range.font = _codeFont;
								ranges.add(range);
							}
						}
					}
				}

			}
		} catch (Exception e) {
			// TODO: there are problems with parallel parsing, we should test it in Photon.
			// e.printStackTrace();
		}
	}

	private void updateFromPreferencesChange() {
		updateStyleValues();
		_viewer.refresh();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		out.println(event.getProperty());
		updateFromPreferencesChange();
	}
}
