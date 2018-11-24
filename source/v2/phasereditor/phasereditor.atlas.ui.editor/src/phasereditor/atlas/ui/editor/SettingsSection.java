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
package phasereditor.atlas.ui.editor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class SettingsSection extends TexturePackerSection<TexturePackerEditorModel> {

	private Text _minWidthText;
	private Text _minHeightText;
	private Text _maxWidthText;
	private Text _maxHeightText;
	private Action _buildAction;
	private Button _powerOfTwoButton;
	private Text _paddingXText;
	private Text _paddingYText;
	private Button _stripWhitesapceXCheckbox;
	private Button _stripWhitesapceYCheckbox;
	private Button _useIndexesCheckbox;
	private Button _gridCheckbox;
	private Button _multiAtlasCheckbox;
	private Button _debugCheckbox;

	public SettingsSection(TexturePackerEditor editor) {
		super("Packer Settings", editor);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof TexturePackerEditorModel;
	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(5, false));

		createActions();

		// Layout

		{
			var title = new Label(comp, 0);
			title.setText("Layout");
			title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));
		}

		{
			label(comp, "Min Size", "The minimum size of output pages");

			label(comp, "Width", "The min width.");
			_minWidthText = new Text(comp, SWT.BORDER);
			_minWidthText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			label(comp, "Height", "The min height.");
			_minHeightText = new Text(comp, SWT.BORDER);
			_minHeightText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		{
			label(comp, "Max Size",
					"he maximum size of output pages.\\r\\n1024 is safe for all devices.\\r\\nExtremely old devices may have degraded performance over 512.");

			label(comp, "Width", "The max width.");
			_maxWidthText = new Text(comp, SWT.BORDER);
			_maxWidthText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			label(comp, "Height", "The max height.");
			_maxHeightText = new Text(comp, SWT.BORDER);
			_maxHeightText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		{
			_powerOfTwoButton = new Button(comp, SWT.CHECK);
			_powerOfTwoButton.setText("Power of Two");
			_powerOfTwoButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));
		}

		{
			new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL)
					.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));
		}

		// Sprites

		{
			var title = new Label(comp, 0);
			title.setText("Sprites");
			title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));
		}

		{
			label(comp, "Padding", "The number of pixels between packed images on the x and y axis.");

			label(comp, "X", "The number of pixels between packed images on the x-axis.",
					new GridData(SWT.RIGHT, SWT.CENTER, false, false));
			_paddingXText = new Text(comp, SWT.BORDER);
			_paddingXText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			label(comp, "Y", "The number of pixels between packed images on the y-axis.",
					new GridData(SWT.RIGHT, SWT.CENTER, false, false));
			_paddingYText = new Text(comp, SWT.BORDER);
			_paddingYText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		{
			_stripWhitesapceXCheckbox = createCheckboxRow(comp, "Strip Whitespace X",
					"If true, blank pixels on the left and right edges of input images\\\\r\\\\nwill be removed. Applications must take special care to draw\\\\r\\\\nthese regions properly.");
			_stripWhitesapceYCheckbox = createCheckboxRow(comp, "Strip Whitespace Y",
					"If true, blank pixels on the left and right edges of input images\\\\r\\\\nwill be removed. Applications must take special care to draw\\\\r\\\\nthese regions properly.");
		}

		{
			new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL)
					.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));
		}

		// Flags

		{
			var title = new Label(comp, 0);
			title.setText("Flags");
			title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));
		}

		{
			_useIndexesCheckbox = createCheckboxRow(comp, "Use Indexes",
					"\"If true, images are sorted by parsing the sufix of the file names\\r\\n(eg. animation_01.png, animation_02.png, ...)\"");
			_gridCheckbox = createCheckboxRow(comp, "Grid Layout", "If true, images are packed in a uniform grid, in order.");
			_multiAtlasCheckbox = createCheckboxRow(comp, "Multi-atlas",
					"If true, use the multiple atlas Phaser 3 JSON format\n(a single atlas JSON file for multiple textures).");
			_debugCheckbox = createCheckboxRow(comp, "Debug",
					"If true, lines are drawn on the output pages\nto show the packed image bounds.");
		}

		registerListeners();

		return comp;
	}

	private static Button createCheckboxRow(Composite comp, String label, String help) {

		Button btn = new Button(comp, SWT.CHECK);
		btn.setText(label);
		btn.setToolTipText(help);
		btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));

		return btn;
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		manager.add(_buildAction);
	}

	private void createActions() {
		_buildAction = new Action("Build Atlas", EditorSharedImages.getImageDescriptor(IMG_BUILD)) {
			@Override
			public void run() {
				getEditor().manuallyBuild();
			}
		};
	}

	@SuppressWarnings("boxing")
	private void registerListeners() {
		var model = getModels().get(0);

		// Layout

		listenInt(_minWidthText, value -> {
			model.getSettings().setMinWidth(value);
			getEditor().dirtify();
		});

		listenInt(_minHeightText, value -> {
			model.getSettings().setMinHeight(value);
			getEditor().dirtify();
		});

		listenInt(_maxWidthText, value -> {
			model.getSettings().setMaxWidth(value);
			getEditor().dirtify();
		});

		listenInt(_maxHeightText, value -> {
			model.getSettings().setMaxHeight(value);
			getEditor().dirtify();
		});

		listen(_powerOfTwoButton, value -> {
			model.getSettings().setPot(value);
			getEditor().dirtify();
		});

		// Sprites

		listenInt(_paddingXText, value -> {
			model.getSettings().setPaddingX(value);
			getEditor().dirtify();
		});

		listenInt(_paddingYText, value -> {
			model.getSettings().setPaddingY(value);
			getEditor().dirtify();
		});

		listen(_stripWhitesapceXCheckbox, value -> {
			model.getSettings().setStripWhitespaceX(value);
			getEditor().dirtify();
		});

		listen(_stripWhitesapceYCheckbox, value -> {
			model.getSettings().setStripWhitespaceY(value);
			getEditor().dirtify();
		});

		// Flags

		listen(_useIndexesCheckbox, value -> {
			model.getSettings().setUseIndexes(value);
			getEditor().dirtify();
		});

		listen(_gridCheckbox, value -> {
			model.getSettings().setGrid(value);
			getEditor().dirtify();
		});

		listen(_multiAtlasCheckbox, value -> {
			model.getSettings().setMultiatlas(value);
			getEditor().dirtify();
		});

		listen(_debugCheckbox, value -> {
			model.getSettings().setDebug(value);
			getEditor().dirtify();
		});

	}

	@Override
	public void update_UI_from_Model() {

		var model = getModels().get(0);

		// Layout

		_minWidthText.setText(Integer.toString(model.getSettings().getMinWidth()));
		_minHeightText.setText(Integer.toString(model.getSettings().getMinHeight()));

		_maxWidthText.setText(Integer.toString(model.getSettings().getMaxWidth()));
		_maxHeightText.setText(Integer.toString(model.getSettings().getMaxHeight()));

		_powerOfTwoButton.setSelection(model.getSettings().isPot());

		// Sprites

		_paddingXText.setText(Integer.toString(model.getSettings().getPaddingX()));
		_paddingYText.setText(Integer.toString(model.getSettings().getPaddingY()));

		_stripWhitesapceXCheckbox.setSelection(model.getSettings().isStripWhitespaceX());
		_stripWhitesapceYCheckbox.setSelection(model.getSettings().isStripWhitespaceY());

		// Flags

		_useIndexesCheckbox.setSelection(model.getSettings().isUseIndexes());
		_gridCheckbox.setSelection(model.getSettings().isGrid());
		_multiAtlasCheckbox.setSelection(model.getSettings().isMultiatlas());
		_debugCheckbox.setSelection(model.getSettings().isDebug());
	}

}
