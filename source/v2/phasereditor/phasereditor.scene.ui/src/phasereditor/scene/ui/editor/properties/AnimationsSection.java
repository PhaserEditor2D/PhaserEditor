// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.scene.ui.editor.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.animation.ui.AnimationPreviewComp;
import phasereditor.assetpack.core.AnimationsAssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.scene.core.AnimationsComponent;
import phasereditor.ui.properties.FormPropertyPage;

/**
 * @author arian
 *
 */
public class AnimationsSection extends ScenePropertySection {

	private AnimationPreviewComp _animCanvas;
	private Button _browseBtn;
	private Button _clearBtn;

	public AnimationsSection(FormPropertyPage page) {
		super("Animations", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof AnimationsComponent;
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		_animCanvas.createPlaybackToolbar(manager);
	}

	@Override
	public Control createContent(Composite parent) {

		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(2, false));

		
		_animCanvas = new AnimationPreviewComp(comp, 0);
		{
			var gd = new GridData(GridData.FILL_BOTH);
			gd.horizontalSpan = 2;
			gd.heightHint = 200;
			_animCanvas.setLayoutData(gd);
		}
		
		_browseBtn = new Button(comp, 0);
		_browseBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		_browseBtn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			this.selectAnimation();
		}));

		_clearBtn = new Button(comp, 0);
		_clearBtn.setText("Clear");
		_clearBtn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> clearAnimation()));


		return comp;
	}

	private void clearAnimation() {

		wrapOperation(() -> {
			
			getModels().forEach(model -> AnimationsComponent.set_autoPlayAnimKey(model, null));
			
		}, getModels());

		getEditor().setDirty(true);
		
		update_UI_from_Model();
	}

	private void selectAnimation() {
		var dlg = new QuickSelectAssetDialog(getEditor().getEditorSite().getShell());
		dlg.setTitle("Select Animation");

		dlg.setInput(getAnimations());

		if (dlg.open() == Window.OK) {
			var anim = (AnimationModel) dlg.getResult();

			wrapOperation(() -> {
				getModels().forEach(model -> {
					AnimationsComponent.set_autoPlayAnimKey(model, anim.getKey());
				});
			}, getModels());
				
			getEditor().setDirty(true);

			update_UI_from_Model();
		}
	}

	private List<AnimationModel> getAnimations() {
		var list = new ArrayList<AnimationModel>();

		var packs = AssetPackCore.getAssetPackModels(getEditor().getProject());
		for (var pack : packs) {
			for (var asset : pack.getAssets()) {
				if (asset instanceof AnimationsAssetModel) {
					var animAsset = (AnimationsAssetModel) asset;
					var anims = animAsset.getSubElements();
					list.addAll(anims);
				}
			}
		}

		return list;
	}

	@Override
	public void update_UI_from_Model() {
		var models = getModels();

		AnimationModel found = null;

		var key = flatValues_to_String(models.stream().map(model -> AnimationsComponent.get_autoPlayAnimKey(model)));

		if (key != null) {
			for (var anim : getAnimations()) {
				if (anim.getKey().equals(key)) {
					found = anim;
				}
			}
		}

		if (found == null) {
			_browseBtn.setText("Select Auto Play Animation");
		} else {
			_browseBtn.setText(key);
		}

		_animCanvas.setVisible(found != null);
		_animCanvas.setModel(found);

		_animCanvas.getParent().layout();
	}

}
