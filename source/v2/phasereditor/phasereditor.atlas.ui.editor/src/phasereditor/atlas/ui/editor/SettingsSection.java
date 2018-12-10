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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.ui.properties.CheckListener;
import phasereditor.ui.properties.TextToIntListener;

/**
 * @author arian
 *
 */
public class SettingsSection extends TexturePackerSection<TexturePackerEditorModel> {

	private Text _minWidthText;
	private Text _minHeightText;
	private Text _maxWidthText;
	private Text _maxHeightText;
	private Button _powerOfTwoButton;
	private Text _paddingXText;
	private Text _paddingYText;
	private Button _stripWhitesapceXCheckbox;
	private Button _stripWhitesapceYCheckbox;
	private Button _useIndexesCheckbox;
	private Button _gridCheckbox;
	// private Button _multiAtlasCheckbox;
	private Button _debugCheckbox;

	public SettingsSection(TexturePackerPropertyPage page) {
		super("Packer Settings", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof TexturePackerEditorModel;
	}

	@Override
	protected void createActions() {

		super.createActions();

		getSettingsAction().setEnabled(false);
	}

	@SuppressWarnings({ "unused" })
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
			new TextToIntListener(_minWidthText) {

				@Override
				protected void accept(int value) {
					getModel().getSettings().setMinWidth(value);
					getEditor().dirtify();

				}
			};

			label(comp, "Height", "The min height.");
			_minHeightText = new Text(comp, SWT.BORDER);
			_minHeightText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextToIntListener(_minHeightText) {

				@Override
				protected void accept(int value) {
					getModel().getSettings().setMinHeight(value);
					getEditor().dirtify();
				}
			};
		}

		{
			label(comp, "Max Size",
					"he maximum size of output pages.\\r\\n1024 is safe for all devices.\\r\\nExtremely old devices may have degraded performance over 512.");

			label(comp, "Width", "The max width.");
			_maxWidthText = new Text(comp, SWT.BORDER);
			_maxWidthText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextToIntListener(_maxWidthText) {

				@Override
				protected void accept(int value) {
					getModel().getSettings().setMaxWidth(value);
					getEditor().dirtify();
				}
			};

			label(comp, "Height", "The max height.");
			_maxHeightText = new Text(comp, SWT.BORDER);
			_maxHeightText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			new TextToIntListener(_maxHeightText) {

				@Override
				protected void accept(int value) {
					getModel().getSettings().setMaxHeight(value);
					getEditor().dirtify();

				}
			};
		}

		{
			_powerOfTwoButton = new Button(comp, SWT.CHECK);
			_powerOfTwoButton.setText("Power of Two");
			_powerOfTwoButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));

			new CheckListener(_powerOfTwoButton) {

				@Override
				protected void accept(boolean value) {
					getModel().getSettings().setPot(value);
					getEditor().dirtify();
				}
			};
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
			new TextToIntListener(_paddingXText) {

				@Override
				protected void accept(int value) {
					getModel().getSettings().setPaddingX(value);
					getEditor().dirtify();

				}
			};

			label(comp, "Y", "The number of pixels between packed images on the y-axis.",
					new GridData(SWT.RIGHT, SWT.CENTER, false, false));
			_paddingYText = new Text(comp, SWT.BORDER);
			_paddingYText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new TextToIntListener(_paddingYText) {

				@Override
				protected void accept(int value) {
					getModel().getSettings().setPaddingY(value);
					getEditor().dirtify();

				}
			};
		}

		{
			_stripWhitesapceXCheckbox = createCheckboxRow(comp, "Strip Whitespace X",
					"If true, blank pixels on the left and right edges of input images\\\\r\\\\nwill be removed. Applications must take special care to draw\\\\r\\\\nthese regions properly.");
			new CheckListener(_stripWhitesapceXCheckbox) {

				@Override
				protected void accept(boolean value) {
					getModel().getSettings().setStripWhitespaceX(value);
					getEditor().dirtify();
				}
			};

			_stripWhitesapceYCheckbox = createCheckboxRow(comp, "Strip Whitespace Y",
					"If true, blank pixels on the left and right edges of input images\\\\r\\\\nwill be removed. Applications must take special care to draw\\\\r\\\\nthese regions properly.");
			new CheckListener(_stripWhitesapceYCheckbox) {

				@Override
				protected void accept(boolean value) {
					getModel().getSettings().setStripWhitespaceY(value);
					getEditor().dirtify();
				}
			};
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
			new CheckListener(_useIndexesCheckbox) {

				@Override
				protected void accept(boolean value) {
					getModel().getSettings().setUseIndexes(value);
					getEditor().dirtify();
				}
			};

			_gridCheckbox = createCheckboxRow(comp, "Grid Layout",
					"If true, images are packed in a uniform grid, in order.");
			new CheckListener(_gridCheckbox) {

				@Override
				protected void accept(boolean value) {
					getModel().getSettings().setGrid(value);
					getEditor().dirtify();
				}
			};

			_debugCheckbox = createCheckboxRow(comp, "Debug",
					"If true, lines are drawn on the output pages\nto show the packed image bounds.");
			new CheckListener(_debugCheckbox) {

				@Override
				protected void accept(boolean value) {
					getModel().getSettings().setDebug(value);
					getEditor().dirtify();
				}
			};
		}

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
	public void user_update_UI_from_Model() {

		var model = getModel();

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
		// _multiAtlasCheckbox.setSelection(model.getSettings().isMultiatlas());
		_debugCheckbox.setSelection(model.getSettings().isDebug());
	}

	protected TexturePackerEditorModel getModel() {
		return getModels().get(0);
	}

}
