// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.assetpack.ui.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.MultiScriptAssetModel;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.TreeArrayContentProvider;

/**
 * @author arian
 *
 */
public class MultiScriptSection extends AssetPackEditorSection<MultiScriptAssetModel> {

	public MultiScriptSection(AssetPackEditorPropertyPage page) {
		super(page, "Multiple JavaScript Files");
		setFillSpace(true);
	}

	@Override
	public boolean supportThisNumberOfModels(int number) {
		return number == 1;
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof MultiScriptAssetModel;
	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(1, false));

		label(comp, "URLs", AssetModel.getHelp(AssetType.image, "url"));

		var viewer = new TreeViewer(comp);
		viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer.setLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				return EditorSharedImages.getImage(IMG_GENERIC_EDITOR);
			}
		});
		viewer.setContentProvider(new TreeArrayContentProvider());
		addUpdate(() -> {
			viewer.setInput(getAsset().getUrls());
		});

		var toolbar = new Composite(comp, 0);
		toolbar.setLayout(new RowLayout());

		Button delBtn;
		Button upBtn;
		Button downBtn;

		{
			var btn = new Button(toolbar, SWT.PUSH);
			btn.setToolTipText("Add more files.");
			btn.setImage(EditorSharedImages.getImage(IMG_ADD));
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				wrapOperation(() -> {
					try {
						var asset = getAsset();
						var pack = asset.getPack();

						var jsFiles = pack.discoverTextFiles("js");
						var urls = asset.getUrlsFromFiles(jsFiles);
						urls.removeAll(asset.getUrls());
						jsFiles = asset.getFilesFromUrls(urls);
						var selectedFiles = AssetPackUI.browseManyAssetFile(asset.getPack(), "scripts", jsFiles,
								getEditor().getEditorSite().getShell());
						urls = asset.getUrlsFromFiles(selectedFiles);
						asset.getUrls().addAll(urls);
					} catch (CoreException e1) {
						AssetPackUIEditor.logError(e1);
					}
				});
			}));
		}

		{
			var btn = delBtn = new Button(toolbar, SWT.PUSH);
			btn.setToolTipText("Delete selected files");
			btn.setImage(EditorSharedImages.getImage(IMG_DELETE));
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				wrapOperation(() -> {
					var urls = getAsset().getUrls();
					urls.removeAll(viewer.getStructuredSelection().toList());
				});
			}));
		}

		{
			var btn = upBtn = new Button(toolbar, SWT.PUSH);
			btn.setToolTipText("Move files up.");
			btn.setImage(EditorSharedImages.getImage(IMG_ARROW_UP));
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				wrapOperation(() -> {
					var elem = viewer.getStructuredSelection().getFirstElement();
					var urls = getAsset().getUrls();
					var i = urls.indexOf(elem);
					if (i > 0) {
						urls.set(i, urls.get(i - 1));
						urls.set(i - 1, (String) elem);
						update_UI_from_Model();
					}
				});
			}));
		}

		{
			var btn = downBtn = new Button(toolbar, SWT.PUSH);
			btn.setToolTipText("Move files down.");
			btn.setImage(EditorSharedImages.getImage(IMG_ARROW_DOWN));
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				wrapOperation(() -> {
					var elem = viewer.getStructuredSelection().getFirstElement();
					var urls = getAsset().getUrls();
					var i = urls.indexOf(elem);
					if (i < urls.size() - 1) {
						urls.set(i, urls.get(i + 1));
						urls.set(i + 1, (String) elem);
					}
				});
			}));
		}

		ISelectionChangedListener lis = e -> {
			var notEmpty = !viewer.getStructuredSelection().isEmpty();
			delBtn.setEnabled(notEmpty);
			upBtn.setEnabled(notEmpty);
			downBtn.setEnabled(notEmpty);
		};
		lis.selectionChanged(null);
		viewer.addSelectionChangedListener(lis);

		return comp;
	}

	private MultiScriptAssetModel getAsset() {
		return getModels().get(0);
	}

}
