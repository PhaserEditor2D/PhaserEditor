// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.canvas.ui.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.json.JSONObject;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.ButtonSpriteModel;
import phasereditor.canvas.core.CanvasModelFactory;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.TextModel;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.core.WorldModel;

/**
 * @author arian
 *
 */
public class NewWizard_Sprite extends NewWizard_Base {

	private NewPage_SpriteSettings _settingsPage;

	public NewWizard_Sprite() {
		super(CanvasType.SPRITE);
	}

	@Override
	protected boolean isCanvasFileDesired() {
		return _settingsPage.isGenerateCanvasFile();
	}

	@Override
	public void addPages() {
		super.addPages();
		_settingsPage = new NewPage_SpriteSettings();
		_settingsPage.setSettings(getModel().getSettings());
		_settingsPage.setProjectProvider(() -> {
			IPath path = getFilePage().getContainerFullPath();
			path = path.append(getFilePage().getFileName());
			if (path == null) {
				return null;
			}
			try {
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				return root.getFile(path).getProject();
			} catch (Exception e) {
				return null;
			}
		});
		addPage(_settingsPage);
		_settingsPage.setPageComplete(false);
	}

	@Override
	protected JSONObject createFinalModelJSON(IFile file) {
		WorldModel world = getModel().getWorld();

		if (_settingsPage.isGenerateCanvasFile()) {
			BaseSpriteModel spriteModel;
			Object asset = _settingsPage.getSelectedAsset();
			switch (_settingsPage.getSettings().getBaseClass()) {
			case "Phaser.Button":
				spriteModel = new ButtonSpriteModel(world, (IAssetFrameModel) asset);
				break;
			case "Phaser.TileSprite":
				spriteModel = new TileSpriteModel(world, (IAssetKey) asset);
				break;
			case "Phaser.Text":
				spriteModel = new TextModel(world, "This is a text");
				break;
			default:
				spriteModel = (BaseSpriteModel) CanvasModelFactory.createModel(world, asset);
				break;
			}

			world.addChild(spriteModel);
		}

		return super.createFinalModelJSON(file);
	}

	@Override
	public boolean canFinish() {
		return _settingsPage.isPageComplete();
	}
}
