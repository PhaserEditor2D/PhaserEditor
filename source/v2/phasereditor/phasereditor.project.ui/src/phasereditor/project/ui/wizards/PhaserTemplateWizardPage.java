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
package phasereditor.project.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.wb.swt.ResourceManager;

import phasereditor.inspect.core.IProjectTemplate;
import phasereditor.inspect.core.IProjectTemplateCategory;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.ProjectTemplateInfo;
import phasereditor.inspect.core.examples.PhaserExamplesRepoModel;
import phasereditor.inspect.core.examples.PhaserExampleCategoryModel;
import phasereditor.inspect.core.examples.PhaserExampleModel;
import phasereditor.inspect.ui.TemplateContentProvider;
import phasereditor.inspect.ui.TemplateLabelProvider;
import phasereditor.ui.FilteredTree2;
import phasereditor.ui.PatternFilter2;
import phasereditor.webrun.ui.WebRunUI;

public class PhaserTemplateWizardPage extends WizardPage {

	public static final String ROOT = "root";
	public static final String PHASER_EXAMPLES = "Phaser Examples";

	private class WizardTemplateContentProvider extends TemplateContentProvider {

		public WizardTemplateContentProvider() {
		}

		@Override
		public Object[] getChildren(Object parent) {
			if (parent == ROOT) {
				List<Object> list = new ArrayList<>();
				list.addAll(InspectCore.getGeneralTemplates().getCategories());
				list.add(PHASER_EXAMPLES);
				return list.toArray();
			}

			if (parent == PHASER_EXAMPLES) {
				return _examples.getExamplesCategories().toArray();
			}

			return super.getChildren(parent);
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof PhaserExampleCategoryModel) {
				return PHASER_EXAMPLES;
			}

			return super.getParent(element);
		}

		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}
	}

	TreeViewer _treeViewer;
	PhaserExamplesRepoModel _examples;
	private IProjectTemplate _template;
	private Browser _infoText;
	private FilteredTree2 _filteredTree;
	private ToolItem _playItem;

	public PhaserTemplateWizardPage() {
		super("Phaser");
		setTitle("New Phaser Project");
		setDescription("Select a project template.");
		_template = InspectCore.getGeneralTemplates().findById("phasereditor.demos.friend_of_cuco");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		GridLayout gl_container = new GridLayout(2, false);
		gl_container.marginWidth = 0;
		gl_container.marginHeight = 0;
		container.setLayout(gl_container);

		SashForm sashForm = new SashForm(container, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		Composite bottomComp = new Composite(sashForm, SWT.NONE);
		GridLayout gl_bottomComp = new GridLayout(1, false);
		gl_bottomComp.marginWidth = 0;
		gl_bottomComp.marginHeight = 0;
		bottomComp.setLayout(gl_bottomComp);

		ToolBar toolBar = new ToolBar(bottomComp, SWT.FLAT | SWT.RIGHT);
		toolBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));

		_playItem = new ToolItem(toolBar, SWT.NONE);
		_playItem.setDisabledImage(ResourceManager.getPluginImage("phasereditor.ui", "icons/d/world.png"));
		_playItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				playSelectedTemplate();
			}
		});
		_playItem.setToolTipText("Play in a browser");
		_playItem.setImage(ResourceManager.getPluginImage("phasereditor.ui", "icons/world.png"));

		_filteredTree = new FilteredTree2(bottomComp, SWT.BORDER, new PatternFilter2(), 1);
		_treeViewer = _filteredTree.getViewer();
		_treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateFromSelection();
			}
		});
		_treeViewer.setContentProvider(new WizardTemplateContentProvider());
		_treeViewer.setLabelProvider(new TemplateLabelProvider());

		_infoText = new Browser(sashForm, SWT.BORDER);
		sashForm.setWeights(new int[] { 2, 1 });

		afterCreateWidgets();
	}

	protected void playSelectedTemplate() {
		if (_template != null && _template instanceof PhaserExampleModel) {
			PhaserExampleModel example = (PhaserExampleModel) _template;
			WebRunUI.openExampleInBrowser(example);
		}
	}

	public void setFocus() {
		_treeViewer.getTree().setFocus();
	}

	private void afterCreateWidgets() {
		_examples = InspectCore.getPhaserExamplesRepoModel();

		_treeViewer.setInput(ROOT);

		_treeViewer.expandToLevel(_template.getCategory(), 2);
		_treeViewer.setSelection(new StructuredSelection(_template), true);
	}

	protected void updateFromSelection() {
		_playItem.setEnabled(false);

		String err = "No template selected.";
		ISelection sel = _treeViewer.getSelection();
		String info = "";
		if (sel.isEmpty()) {
			setErrorMessage(err);
		} else {
			Object elem = ((IStructuredSelection) _treeViewer.getSelection()).getFirstElement();
			if (elem instanceof IProjectTemplate) {
				setErrorMessage(null);
				_template = (IProjectTemplate) elem;
				ProjectTemplateInfo templInfo = _template.getInfo();
				StringBuilder sb = new StringBuilder();

				sb.append("<b>Author:</b> " + templInfo.getAuthor() + " (" + templInfo.getEmail() + ")<br>");
				sb.append("<b>Website:</b> " + templInfo.getWebsite());
				sb.append("<br>");

				if (templInfo.getUrl() != null) {
					sb.append("<b>URL:</b> " + templInfo.getUrl() + "<br>");
				}
				sb.append("<br><b>Description</b><br><br>");
				sb.append(templInfo.getDescription());
				info = sb.toString();

				if (elem instanceof PhaserExampleModel) {
					_playItem.setEnabled(true);
				}

			} else if (elem == PHASER_EXAMPLES) {
				info = "Official Phaser Examples\n\nhttp://phaser.io/examples";
			} else {
				if (elem instanceof IProjectTemplateCategory) {
					info = ((IProjectTemplateCategory) elem).getDescription();
				}

				setErrorMessage(err);
			}
			setPageComplete(getErrorMessage() == null);
		}
		{
			Font font = getShell().getDisplay().getSystemFont();
			FontData fd = font.getFontData()[0];
			_infoText.setText(String.format("<html><body style='font-family:%s;font-size:%spt;'>%s</body></html>",
					fd.getName(), Integer.valueOf(fd.getHeight()), info));
		}
	}

	public IProjectTemplate getTemplate() {
		return _template;
	}

	public ISelectionProvider getSelectionProvider() {
		return _treeViewer;
	}
}
