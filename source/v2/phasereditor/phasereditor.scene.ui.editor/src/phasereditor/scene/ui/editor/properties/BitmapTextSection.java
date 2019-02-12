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

import static java.util.stream.Collectors.toList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.ui.ShowKeyInAssetPackEditorAction;
import phasereditor.scene.core.BitmapTextComponent;
import phasereditor.scene.core.BitmapTextModel;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.properties.FormPropertyPage;

/**
 * @author arian
 *
 */
public class BitmapTextSection extends ScenePropertySection {

	private Text _fontSizeText;
	private AlignAction _alignLeftAction;
	private AlignAction _alignMiddleAction;
	private AlignAction _alignRightAction;
	private Text _letterSpacingText;
	private Button _fontNameBtn;
	private FontAction _fontAction;
	private ShowKeyInAssetPackEditorAction _showFontInAssetPackEditor;
	private Action _selectAllObjectsWithSameFontAction;

	public BitmapTextSection(FormPropertyPage page) {
		super("Bitmap Text", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof BitmapTextComponent;
	}

	class AlignAction extends Action {
		private int _align;

		public AlignAction(String name, String icon, int align) {
			super(name, AS_CHECK_BOX);

			setImageDescriptor(EditorSharedImages.getImageDescriptor(icon));

			_align = align;
		}

		public int getAlign() {
			return _align;
		}

		@Override
		public void run() {

			wrapOperation(() -> {
				getModels().forEach(model -> {
					BitmapTextComponent.set_align(model, _align);
				});

			}, true);

			getEditor().setDirty(true);
			getEditor().getScene().redraw();

			updateAlignActionsState();
		}
	}

	@SuppressWarnings({ "unused" })
	@Override
	public Control createContent(Composite parent) {

		createActions();

		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));

		{
			label(comp, "Align", "Phaser.GameObjects.BitmapText.align");

			var manager = new ToolBarManager();

			manager.add(_alignLeftAction);
			manager.add(_alignMiddleAction);
			manager.add(_alignRightAction);

			var toolbar = manager.createControl(comp);
			toolbar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		{
			label(comp, "Font", "Phaser.GameObjects.BitmapText.font");

			_fontNameBtn = new Button(comp, SWT.LEFT);
			_fontNameBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			_fontNameBtn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> _fontAction.run()));
		}

		{
			label(comp, "Font Size", "Phaser.GameObjects.BitmapText.fontSize");

			_fontSizeText = new Text(comp, SWT.BORDER);
			_fontSizeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new SceneTextToInt(_fontSizeText) {
				{
					dirtyModels = true;
				}

				@Override
				protected void accept2(int value) {
					getModels().stream().forEach(model -> {
						BitmapTextComponent.set_fontSize(model, value);
					});

					getEditor().setDirty(true);
				}
			};
		}

		{
			label(comp, "Leter Spacing", "Phaser.GameObjects.BitmapText.letterSpacing");

			_letterSpacingText = new Text(comp, SWT.BORDER);
			_letterSpacingText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new SceneTextToFloat(_letterSpacingText) {
				{
					dirtyModels = true;
				}

				@Override
				protected void accept2(float value) {
					getModels().stream().forEach(model -> {
						BitmapTextComponent.set_letterSpacing(model, value);
					});

					getEditor().setDirty(true);

				}
			};
		}

		return comp;
	}

	class FontAction extends Action {
		public FontAction() {
			super(getHelp("Phaser.GameObjects.BitmapText.font"), EditorSharedImages.getImageDescriptor(IMG_FONT));
		}

		@Override
		public void run() {
			var dlg = new QuickSelectAssetDialog(getEditor().getSite().getShell());

			var editor = getEditor();

			var project = editor.getEditorInput().getFile().getProject();

			var packs = AssetPackCore.getAssetPackModels(project);

			dlg.setTitle("Select BitmapFont");
			dlg.setInput(packs.stream().flatMap(pack -> pack.getAssets().stream())
					.filter(asset -> asset instanceof BitmapFontAssetModel).toArray());

			if (dlg.open() == Window.OK) {

				wrapOperation(() -> {

					var asset = (BitmapFontAssetModel) dlg.getSingleResult();

					for (var obj : getModels()) {
						BitmapTextComponent.utils_setFont(obj, asset);
					}

				}, true);

				editor.setDirty(true);
			}
		}
	}

	class FontSizeAction extends Action {
		private boolean _plus;

		public FontSizeAction(boolean plus) {
			super(plus ? "+ Font Size" : "- Font Size",
					EditorSharedImages.getImageDescriptor(plus ? IMG_FONT_PLUS : IMG_FONT_MINUS));
			_plus = plus;
		}

		@Override
		public void run() {

			wrapOperation(() -> {
				for (var model : getModels()) {
					var size = BitmapTextComponent.get_fontSize(model);
					BitmapTextComponent.set_fontSize(model, size + (_plus ? 2 : -2));
				}

				user_update_UI_from_Model();

				getEditor().setDirty(true);

			}, true);

		}
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {

		manager.add(_alignLeftAction);
		manager.add(_alignMiddleAction);
		manager.add(_alignRightAction);

		manager.add(new Separator());

		manager.add(_fontAction);

		manager.add(new Separator());

		manager.add(new FontSizeAction(true));
		manager.add(new FontSizeAction(false));

		manager.add(new Separator());

		manager.add(_selectAllObjectsWithSameFontAction);
		manager.add(_showFontInAssetPackEditor);
	}

	private void createActions() {
		_alignLeftAction = new AlignAction("ALIGN_LEFT", IMG_TEXT_ALIGN_LEFT, BitmapTextComponent.ALIGN_LEFT);
		_alignMiddleAction = new AlignAction("ALIGN_MIDDLE", IMG_TEXT_ALIGN_CENTER, BitmapTextComponent.ALIGN_MIDDLE);
		_alignRightAction = new AlignAction("ALIGN_RIGHT", IMG_TEXT_ALIGN_RIGHT, BitmapTextComponent.ALIGN_RIGHT);

		_fontAction = new FontAction();

		_selectAllObjectsWithSameFontAction = new Action("Select all objects with the same font.",
				EditorSharedImages.getImageDescriptor(IMG_SELECT_OBJECTS)) {
			@Override
			public void run() {

				var key = getAssetKey();

				if (key == null) {
					return;
				}

				var list = getSceneModel().getDisplayList().stream()

						.filter(BitmapTextComponent::is)

						.filter(model -> key.equals(BitmapTextComponent.get_fontAssetKey(model)))

						.collect(toList());

				getEditor().setSelection(list);
				getEditor().updatePropertyPagesContentWithSelection();
			}
		};

		_showFontInAssetPackEditor = new ShowKeyInAssetPackEditorAction(
				"Show the bitmap font key in the Asset Pack editor.") {

			@Override
			protected IAssetKey getKey() {
				var key = getAssetKey();
				var asset = getAssetFinder().findAssetKey(key);
				return asset;
			}
		};
	}

	@SuppressWarnings("boxing")
	@Override
	public void user_update_UI_from_Model() {
		var models = getModels();

		getModels().forEach(model -> ((BitmapTextModel) model).updateSizeFromBitmapFont(getAssetFinder()));

		String flatValues_to_String = flatValues_to_String(
				models.stream().map(model -> BitmapTextComponent.get_fontSize(model)));

		_fontSizeText.setText(flatValues_to_String);

		_letterSpacingText.setText(
				flatValues_to_String(models.stream().map(model -> BitmapTextComponent.get_letterSpacing(model))));

		_fontNameBtn.setText(flatValues_to_String(models.stream().map(model -> {

			var asset = BitmapTextComponent.utils_getFont(model, getAssetFinder());

			return asset == null ? "<null>" : asset.getKey();
		})));

		updateAlignActionsState();

	}

	@SuppressWarnings("boxing")
	void updateAlignActionsState() {
		for (var action : new AlignAction[] { _alignLeftAction, _alignMiddleAction, _alignRightAction }) {
			action.setChecked(flatValues_to_boolean(
					getModels().stream().map(model -> BitmapTextComponent.get_align(model) == action.getAlign())));
		}
	}

	private String getAssetKey() {
		return flatValues_to_String(getModels().stream().map(model -> BitmapTextComponent.get_fontAssetKey(model)));
	}

}
