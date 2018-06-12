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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.wb.swt.SWTResourceManager;
import org.json.JSONException;

import phasereditor.chains.core.ChainItem;
import phasereditor.chains.core.ChainsCore;
import phasereditor.chains.core.ChainsModel;
import phasereditor.chains.core.Line;
import phasereditor.chains.core.Match;
import phasereditor.chains.ui.ChainsUI;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.examples.ExampleCategoryModel;
import phasereditor.inspect.core.examples.ExampleModel;
import phasereditor.inspect.core.jsdoc.IPhaserMember;
import phasereditor.inspect.core.jsdoc.JSDocRenderer;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.editors.StringEditorInput;
import phasereditor.webrun.ui.WebRunUI;

public class ChainsView extends ViewPart {
	private Text _queryText;
	private TableViewer _chainsViewer;
	ChainsModel _chainsModel;
	private TableColumn _chainTableColumn;
	private TableViewer _examplesViewer;
	private TableColumn _examplesTableColumn;
	private Browser _docBrowser;
	private TabFolder _tabFolder;
	private SashForm _mainSash;
	private Composite _mainSashContainer;

	class ChainsLabelProvider extends StyledCellLabelProvider {

		private Font _italic;
		private Font _font;

		public ChainsLabelProvider() {
			// _hilightColor = SWTResourceManager.getColor(253, 250, 210);

		}

		@Override
		public String getToolTipText(Object element) {
			return element.toString();
		}

		@Override
		public void update(ViewerCell cell) {
			if (_font == null) {
				_font = JFaceResources.getFont(JFaceResources.TEXT_FONT);
			}
			if (_italic == null) {
				FontData fd = cell.getFont().getFontData()[0];
				_italic = SWTResourceManager.getFont(fd.getName(), fd.getHeight(), SWT.ITALIC);
			}

			Match match = (Match) cell.getElement();
			ChainItem chain = (ChainItem) match.item;

			String text = match.toString();

			StyleRange[] ranges;

			Color fgColor = ChainsUI.get_pref_Chains_highlightFgColor();
			Color bgColor = ChainsUI.get_pref_Chains_highlightBgColor();
			Color typeColor = ChainsUI.get_pref_Chains_typePartFgColor();//SWTResourceManager.getColor(154, 131, 80);

			StyleRange selRange = new StyleRange(match.start, match.length, fgColor, bgColor);
			StyleRange allRange = new StyleRange(0, text.length(), null, null);
			allRange.font = _font;
			StyleRange returnTypeRange = new StyleRange();

			{
				int index = chain.getReturnTypeIndex();
				if (index > 0) {
					int len = chain.getDisplay().length() - index;
					returnTypeRange = new StyleRange(index, len, typeColor, null);
				}
			}

			if (chain.getDepth() > 0) {
				StyleRange italicRange = new StyleRange();
				italicRange.font = _italic;
				italicRange.start = 0;
				italicRange.length = text.length();
				ranges = new StyleRange[] { allRange, italicRange, returnTypeRange, selRange };
			} else {
				ranges = new StyleRange[] { allRange, returnTypeRange , selRange};
			}

			cell.setText(text);
			cell.setStyleRanges(ranges);

			{
				String key;

				if (chain.isEnum()) {
					key = IEditorSharedImages.IMG_ENUM_OBJ;
				} else if (chain.isProperty()) {
					key = IEditorSharedImages.IMG_FIELD_PUBLIC_OBJ;
				} else if (chain.isConst()) {
					key = IEditorSharedImages.IMG_FIELD_DEFAULT_OBJ;
				} else if (chain.isMethod()) {
					key = IEditorSharedImages.IMG_METHPUB_OBJ;
				} else if (chain.isType()) {
					key = IEditorSharedImages.IMG_CLASS_OBJ;
				} else {
					key = IEditorSharedImages.IMG_PACKAGE_OBJ;
				}
				cell.setImage(EditorSharedImages.getImage(key));
			}
			super.update(cell);
		}
	}

	class ExamplesLineLabelProvider extends StyledCellLabelProvider {

		private Font _font;
		private Font _italic;

		public ExamplesLineLabelProvider() {
		}

		@Override
		public String getToolTipText(Object element) {
			return element.toString();
		}

		@Override
		public void update(ViewerCell cell) {
			if (_font == null) {
				_font = JFaceResources.getFont(JFaceResources.TEXT_FONT);
			}
			if (_italic == null) {
				FontData fd = _font.getFontData()[0];
				_italic = SWTResourceManager.getFont(fd.getName(), fd.getHeight(), SWT.ITALIC);
			}

			Match match = (Match) cell.getElement();
			Line line = null;
			String text;
			Font font = _font;
			if (match.item instanceof Line) {
				line = (Line) match.item;
				text = line.text;
			} else {
				text = match.item.toString();
				font = _italic;
			}
			StyleRange allRange = new StyleRange(0, text.length(), null, null);
			allRange.font = font;
			Color fgcolor = ChainsUI.get_pref_Chains_highlightFgColor();
			Color bgcolor = ChainsUI.get_pref_Chains_highlightBgColor();
			StyleRange selRange = new StyleRange(match.start, match.length, fgcolor, bgcolor);
			StyleRange[] ranges = { allRange, selRange };
			cell.setStyleRanges(ranges);
			cell.setText(text);
			cell.setImage(EditorSharedImages.getImage(IEditorSharedImages.IMG_SCRIPT_CODE));

			super.update(cell);
		}
	}

	public ChainsView() {
	}

	/**
	 * Create contents of the view part.
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);

		GridLayout gl_container = new GridLayout(1, false);
		gl_container.marginWidth = 0;
		gl_container.marginHeight = 0;
		gl_container.verticalSpacing = 0;
		gl_container.horizontalSpacing = 0;
		container.setLayout(gl_container);
		{
			_tabFolder = new TabFolder(container, SWT.NONE);
			_tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
			{
				TabItem tbtmSearch = new TabItem(_tabFolder, SWT.NONE);
				tbtmSearch.setText("Search");
				{
					_mainSashContainer = new Composite(_tabFolder, SWT.NONE);
					tbtmSearch.setControl(_mainSashContainer);
					GridLayout gl_composite = new GridLayout(1, false);
					gl_composite.marginHeight = 0;
					gl_composite.marginWidth = 0;
					_mainSashContainer.setLayout(gl_composite);
					{
						_queryText = new Text(_mainSashContainer, SWT.BORDER);
						_queryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
						_queryText.setText("add.*(");
						{
							_mainSash = new SashForm(_mainSashContainer, SWT.HORIZONTAL);
							_mainSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
							_mainSash.setSize(468, 463);
							{
								_chainsViewer = new TableViewer(_mainSash, SWT.FULL_SELECTION);
								_chainsViewer.addDoubleClickListener(new IDoubleClickListener() {
									@Override
									public void doubleClick(DoubleClickEvent event) {
										showChainDoc(
												(Match) ((StructuredSelection) event.getSelection()).getFirstElement());
									}
								});
								Table chainsTable = _chainsViewer.getTable();
								chainsTable.setLinesVisible(true);
								chainsTable.setHeaderVisible(true);
								{
									TableViewerColumn tableViewerColumn = new TableViewerColumn(_chainsViewer,
											SWT.NONE);
									tableViewerColumn.setLabelProvider(new ChainsLabelProvider());
									_chainTableColumn = tableViewerColumn.getColumn();
									_chainTableColumn.setWidth(1000);
									_chainTableColumn.setText("Chains");
								}
								{
									Menu menu = new Menu(chainsTable);
									chainsTable.setMenu(menu);
									{
										MenuItem mntmShowDocumentation = new MenuItem(menu, SWT.NONE);
										mntmShowDocumentation.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent e) {
												showChainDoc(getSelectedChainItemMatch());
											}
										});
										mntmShowDocumentation.setText("Show Documentation");
									}
									{
										MenuItem mntmShowSourceCode = new MenuItem(menu, SWT.NONE);
										mntmShowSourceCode.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent e) {
												showSourceCode(getSelectedChainItemMatch());
											}
										});
										mntmShowSourceCode.setText("Show Source Code");
									}
								}
								{
									_examplesViewer = new TableViewer(_mainSash, SWT.FULL_SELECTION);
									_examplesViewer.addDoubleClickListener(new IDoubleClickListener() {
										@Override
										public void doubleClick(DoubleClickEvent event) {
											Match match = (Match) ((StructuredSelection) event.getSelection())
													.getFirstElement();
											showExample(match.item);
										}
									});
									Table examplesTable = _examplesViewer.getTable();
									examplesTable.setLinesVisible(true);
									examplesTable.setHeaderVisible(true);
									{
										TableViewerColumn tableViewerColumn = new TableViewerColumn(_examplesViewer,
												SWT.NONE);
										tableViewerColumn.setLabelProvider(new ExamplesLineLabelProvider());
										_examplesTableColumn = tableViewerColumn.getColumn();
										_examplesTableColumn.setWidth(392);
										_examplesTableColumn.setText("Examples");
									}
									{
										TableViewerColumn tableViewerColumn = new TableViewerColumn(_examplesViewer,
												SWT.NONE);
										tableViewerColumn.setLabelProvider(new ColumnLabelProvider() {
											@Override
											public String getText(Object aElement) {
												Object element = ((Match) aElement).item;
												if (element instanceof Line) {
													Line line = (Line) element;
													return line.filename + " [" + line.linenum + "]";
												}
												return element.toString();
											}
										});
										TableColumn tblclmnFile = tableViewerColumn.getColumn();
										tblclmnFile.setWidth(300);
										tblclmnFile.setText("File");
									}
									_examplesViewer.setContentProvider(new ArrayContentProvider());
								}
								_chainsViewer.setContentProvider(new ArrayContentProvider());
							}
							_mainSash.setWeights(new int[] { 1, 1 });
						}
						{
							TabItem tbtmDocumentation = new TabItem(_tabFolder, SWT.NONE);
							tbtmDocumentation.setText("JSDoc");
							{
								Composite composite_1 = new Composite(_tabFolder, SWT.NONE);
								tbtmDocumentation.setControl(composite_1);
								GridLayout gl_composite_1 = new GridLayout(1, false);
								gl_composite_1.marginWidth = 0;
								gl_composite_1.marginHeight = 0;
								composite_1.setLayout(gl_composite_1);
								{
									_docBrowser = new Browser(composite_1, SWT.NONE);
									_docBrowser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
								}
							}
						}
						_queryText.addModifyListener(new ModifyListener() {
							@Override
							public void modifyText(ModifyEvent e) {
								queryTextModified();
							}
						});
					}
				}
			}
		}

		createActions();
		initializeToolBar();
		initializeMenu();

		afterCreateWidgets();
	}

	protected void showSourceCode(Match match) {
		ChainItem item = (ChainItem) match.item;
		IPhaserMember member = item.getPhaserMember();

		PhaserJSDoc jsdoc = PhaserJSDoc.getInstance();
		Path file = jsdoc.getMemberPath(member);
		if (file != null) {
			int line = member.getLine();
			int offset = member.getOffset();
			openJSEditor(line, offset, file);
		}
	}

	protected void showExample(Object item) {
		String file;
		int linenum = -1;

		if (item instanceof String) {
			file = (String) item;
		} else {
			Line line = (Line) item;
			file = line.filename;
			linenum = line.linenum;
		}

		Path filePath = InspectCore.getBundleFile(InspectCore.RESOURCES_EXAMPLES_PLUGIN,
				"phaser3-examples/public/src/" + file);

		for (ExampleCategoryModel c : InspectCore.getExamplesModel().getExamplesCategories()) {
			for (ExampleModel e : c.getTemplates()) {
				if (e.getMainFilePath().equals(filePath)) {
					WebRunUI.openExampleInBrowser(e, linenum);

					return;
				}
			}
		}

		openJSEditor(linenum, -1, filePath);
	}

	private void openJSEditor(int linenum, int offset, Path filePath) {
		// open in editor
		try {

			String editorId = "org.eclipse.ui.genericeditor.GenericEditor";

			byte[] bytes = Files.readAllBytes(filePath);

			IWorkbenchPage activePage = getViewSite().getWorkbenchWindow().getActivePage();

			StringEditorInput input = new StringEditorInput(filePath.getFileName().toString(), new String(bytes));
			input.setTooltip(input.getName());

			// open in generic editor

			TextEditor editor = (TextEditor) activePage.openEditor(input, editorId);

			StyledText textWidget = (StyledText) editor.getAdapter(Control.class);
			textWidget.setEditable(false);

			out.println("Open " + filePath.getFileName() + " at line " + linenum);

			int index = linenum - 1;
			try {
				int offset2 = offset;
				if (offset == -1) {
					offset2 = textWidget.getOffsetAtLine(index);
				}
				textWidget.setCaretOffset(offset2);
				textWidget.setTopIndex(index);
			} catch (IllegalArgumentException e) {
				// protect from index out of bounds
				e.printStackTrace();
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected void showChainDoc(Match match) {
		ChainItem item = (ChainItem) match.item;
		try {
			// String doc = ChainsModel.getDoc(item);
			IPhaserMember member = item.getPhaserMember();
			String doc = JSDocRenderer.getInstance().render(member);
			String html = wrapBody(doc);
			_docBrowser.setText(html);
			_tabFolder.setSelection(1);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private static String wrapBody(String doc) {
		RGB rgb = SWTResourceManager.getColor(SWT.COLOR_INFO_BACKGROUND).getRGB();
		String color = "rgb(" + rgb.red + ", " + rgb.green + ", " + rgb.blue + ")";

		String html = "<html><body style='background:\"" + color + "\";'>";
		html += doc;
		html += "</body></html>";
		return html;
	}

	AtomicInteger _token = new AtomicInteger(0);

	protected void queryTextModified() {

		// this method is executed async so we should check this
		if (_queryText.isDisposed()) {
			return;
		}

		new Thread(this::searchAndUpdateTables).start();

	}

	private void afterCreateWidgets() {
		_tabFolder.setSelection(0);

		_chainsViewer.setInput(new Object[0]);
		_examplesViewer.setInput(new Object[0]);

		new Job("Building chains...") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				_chainsModel = ChainsCore.getChainsModel();

				queryTextModified();

				return Status.OK_STATUS;
			}
		}.schedule();

		_docBrowser.setText(wrapBody("<b>Double click a chain to see the JSDoc here.</b>"));

		PhaserEditorUI.refreshViewerWhenPreferencesChange(ChainsUI.getPreferenceStore(), _chainsViewer,
				_examplesViewer);
	}

	/**
	 * Create the actions.
	 */
	private void createActions() {
		// Create the actions
	}

	/**
	 * Initialize the toolbar.
	 */
	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();

		toolbarManager.add(
				new Action("Split", EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_SPLIT_HORIZONTAL)) {
					private boolean _horiz = true;

					@SuppressWarnings("synthetic-access")
					@Override
					public void run() {
						_horiz = !_horiz;
						setImageDescriptor(
								EditorSharedImages.getImageDescriptor(_horiz ? IEditorSharedImages.IMG_SPLIT_HORIZONTAL
										: IEditorSharedImages.IMG_SPLIT_VERTICAL));
						int style = _horiz ? SWT.HORIZONTAL : SWT.VERTICAL;
						SashForm old = _mainSash;
						_mainSash = new SashForm(_mainSashContainer, style);
						_mainSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
						_chainsViewer.getTable().setParent(_mainSash);
						_examplesViewer.getTable().setParent(_mainSash);
						_mainSash.setWeights(new int[] { 1, 1 });
						_mainSashContainer.layout();
						_mainSashContainer.update();
						old.dispose();
					}
				});
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
		return (Match) ((IStructuredSelection) _chainsViewer.getSelection()).getFirstElement();
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

	private void searchAndUpdateTables() {
		int token = _token.incrementAndGet();

		String[] query = new String[1];

		getSite().getShell().getDisplay().syncExec(() -> {
			query[0] = _queryText.getText();
		});

		int chainsLimit = 100;

		if (_chainsModel == null) {
			return;
		}

		List<Match> list1 = _chainsModel.searchChains(query[0], chainsLimit);
		List<Match> list2 = _chainsModel.searchExamples(query[0], chainsLimit);

		if (_token.get() != token) {
			return;
		}

		swtRun(() -> {
			_chainsViewer.setInput(list1);

			_examplesViewer.setInput(list2);

			int chainsSize = list1.size();
			int examplesSize = list2.size();

			_chainTableColumn.setText(
					"Chains (" + (chainsSize == chainsLimit ? chainsSize + "+" : Integer.valueOf(chainsSize)) + ")");

			_examplesTableColumn.setText("Examples ("
					+ (examplesSize == chainsLimit ? examplesSize + "+" : Integer.valueOf(examplesSize)) + ")");

		});
	}
}
