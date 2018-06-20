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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
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
import phasereditor.inspect.ui.InspectUI;
import phasereditor.inspect.ui.views.JsdocView;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.editors.StringEditorInput;
import phasereditor.webrun.ui.WebRunUI;

public class ChainsView extends ViewPart {
	private Text _queryText;
	private TableViewer _chainsViewer;
	ChainsModel _chainsModel;

	class ChainsLabelProvider extends StyledCellLabelProvider {

		private Font _italic;
		private Font _font;
		private Font _bold;
		private Font _codeFont;
		private Color _secondaryColor;

		public ChainsLabelProvider() {
			_font = JFaceResources.getFont(JFaceResources.DEFAULT_FONT);

			FontData fd = _font.getFontData()[0];
			_italic = SWTResourceManager.getFont(fd.getName(), fd.getHeight(), SWT.ITALIC);
			_bold = SWTResourceManager.getFont(fd.getName(), fd.getHeight(), SWT.BOLD);

			_codeFont = JFaceResources.getFont(JFaceResources.TEXT_FONT);

			RGB rgb = new RGB(154, 131, 80);
			_secondaryColor = SWTResourceManager.getColor(rgb);

		}

		@Override
		public String getToolTipText(Object element) {
			return element.toString();
		}

		private void updateExampleLabel(ViewerCell cell) {

			Match match = (Match) cell.getElement();
			Line line = null;
			String text;
			int secondaryColorIndex;
			if (match.item instanceof Line) {
				line = (Line) match.item;
				text = line.text + " - " + line.filename + " [" + line.linenum + "]";
				secondaryColorIndex = line.text.length();
			} else {
				secondaryColorIndex = 0;
				text = match.item.toString();
			}

			StyleRange allRange = new StyleRange(0, text.length(), null, null);
			allRange.font = _codeFont;

			StyleRange selRange = new StyleRange(match.start, match.length, null, null);
			selRange.font = _bold;
			StyleRange secondaryRange = new StyleRange(secondaryColorIndex, text.length() - secondaryColorIndex,
					_secondaryColor, null);

			StyleRange[] ranges = { allRange, secondaryRange, selRange };
			cell.setStyleRanges(ranges);
			cell.setText(text);
			cell.setImage(EditorSharedImages.getImage(IEditorSharedImages.IMG_SCRIPT_CODE));
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

			String text = match.toString();

			StyleRange[] ranges;

			StyleRange selRange = new StyleRange(match.start, match.length, null, null);
			selRange = new StyleRange(match.start, match.length, null, null);
			selRange.font = _bold;
			StyleRange allRange = new StyleRange(0, text.length(), null, null);
			allRange.font = _font;
			StyleRange returnTypeRange = new StyleRange();

			{
				int index = chain.getReturnTypeIndex();
				if (index > 0) {
					int len = chain.getDisplay().length() - index;
					returnTypeRange = new StyleRange(index, len, _secondaryColor, null);
				}
			}

			if (chain.getDepth() > 0) {
				StyleRange italicRange = new StyleRange();
				italicRange.font = _italic;
				italicRange.start = 0;
				italicRange.length = text.length();
				ranges = new StyleRange[] { allRange, italicRange, returnTypeRange, selRange };
			} else {
				ranges = new StyleRange[] { allRange, returnTypeRange, selRange };
			}

			cell.setText(text);
			cell.setStyleRanges(ranges);
			cell.setImage(JSDocRenderer.getInstance().getImage(chain.getPhaserMember()));
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
		GridLayout gl_composite = new GridLayout(1, false);
		gl_composite.marginHeight = 0;
		gl_composite.marginWidth = 0;
		container.setLayout(gl_composite);
		{
			_queryText = new Text(container, SWT.BORDER);
			_queryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			_queryText.setText("add.*(");
			{
				{
					_chainsViewer = new TableViewer(container, SWT.FULL_SELECTION);
					_chainsViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
					_chainsViewer.addDoubleClickListener(new IDoubleClickListener() {
						@Override
						public void doubleClick(DoubleClickEvent event) {
							Match match = (Match) ((StructuredSelection) event.getSelection()).getFirstElement();
							if (match.item instanceof ChainItem) {
								showChainDoc(match);
							} else {
								showExample(match.item);
							}
						}
					});
					Table chainsTable = _chainsViewer.getTable();
					chainsTable.setLinesVisible(true);
					chainsTable.setHeaderVisible(false);
					{
						TableViewerColumn tableViewerColumn = new TableViewerColumn(_chainsViewer, SWT.NONE);
						tableViewerColumn.setLabelProvider(new ChainsLabelProvider());
						TableColumn column = tableViewerColumn.getColumn();
						column.setWidth(1000);
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
					_chainsViewer.setContentProvider(new ArrayContentProvider());
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

	protected void showSourceCode(Match match) {
		if (match.item instanceof ChainItem) {
			showChainSource(match);
		} else {
			showExample(match.item);
		}
	}

	/**
	 * @param match
	 */
	private void showChainSource(Match match) {
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
		Path filePath;
		int linenum = -1;

		if (item instanceof String) {
			String file = (String) item;
			filePath = InspectCore.getBundleFile(InspectCore.RESOURCES_EXAMPLES_PLUGIN,
					"phaser3-examples/public/src/" + file);
		} else {
			Line line = (Line) item;
			linenum = line.linenum;
			filePath = line.example.getMainFilePath();
		}

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
		if (!(match.item instanceof ChainItem)) {
			return;
		}

		ChainItem item = (ChainItem) match.item;
		try {
			try {
				JsdocView view = (JsdocView) getViewSite().getPage().showView(InspectUI.JSDOC_VIEW_ID, null,
						IWorkbenchPage.VIEW_CREATE);
				view.showJsdocFor(item.getPhaserMember());
				getViewSite().getPage().activate(view);
			} catch (PartInitException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
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
		_chainsViewer.setInput(new Object[0]);

		new Job("Building chains...") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				_chainsModel = ChainsCore.getChainsModel();

				queryTextModified();

				return Status.OK_STATUS;
			}
		}.schedule();

		PhaserEditorUI.refreshViewerWhenPreferencesChange(ChainsUI.getPreferenceStore(), _chainsViewer);

		getViewSite().setSelectionProvider(_chainsViewer);
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

		List<Match> matches = new ArrayList<>();

		matches.addAll(list1);
		matches.addAll(list2);

		if (_token.get() != token) {
			return;
		}

		swtRun(() -> {
			_chainsViewer.setInput(matches);
		});
	}
}
