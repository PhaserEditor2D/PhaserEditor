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
package phasereditor.scene.ui.editor.properties;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.scene.core.ComponentGetter;
import phasereditor.scene.core.ComponentSetter;
import phasereditor.scene.core.TintComponent;
import phasereditor.ui.ColorButtonSupport;
import phasereditor.ui.Colors;
import phasereditor.ui.properties.FormPropertyPage;

/**
 * @author arian
 *
 */
public class TintSection extends ScenePropertySection {
	private static final int tintDefault = TintComponent.tintTopLeft_default;

	public TintSection(FormPropertyPage page) {
		super("Tint", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof TintComponent;
	}

	@SuppressWarnings({ "unused", "boxing" })
	@Override
	public Control createContent(Composite parent) {

		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(2, false));

		{
			label(comp, "Tinted", "Phaser.GameObjects.Components.Tint.isTinted");
			var btn = new Button(comp, SWT.CHECK);
			new SceneCheckListener(btn) {

				@Override
				protected void accept2(boolean value) {
					getModels().forEach(model -> TintComponent.set_isTinted(model, value));
				}
			};

			addUpdate(() -> {
				var value = flatValues_to_boolean(getModels().stream().map(model -> TintComponent.get_isTinted(model)));
				btn.setSelection(value);
			});
		}

		{
			label(comp, "Tint Fill", "Phaser.GameObjects.Components.Tint.tintFill");
			var btn = new Button(comp, SWT.CHECK);
			new SceneCheckListener(btn) {

				@Override
				protected void accept2(boolean value) {
					getModels().forEach(model -> TintComponent.set_tintFill(model, value));
				}
			};
			addUpdate(() -> {
				btn.setSelection(flatValues_to_boolean(getModels().stream().map(TintComponent::get_tintFill)));
			});
		}

		{
			label(comp, "Tint", "Phaser.GameObjects.Components.Tint.tint");
			var btn = new Button(comp, SWT.PUSH);
			var support = ColorButtonSupport.createDefault(btn, rgb -> {
				wrapOperation(() -> {
					int color = Colors.intColor(rgb);
					getModels().forEach(model -> {
						TintComponent.set_tintTopLeft(model, color);
						TintComponent.set_tintTopRight(model, color);
						TintComponent.set_tintBottomLeft(model, color);
						TintComponent.set_tintBottomRight(model, color);
					});
				});
				getEditor().setDirty(true);
			});
			btn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			addUpdate(() -> {
				var color = flatValues_to_int(getModels()

						.stream()

						.flatMap(model -> Arrays.stream(new Integer[] {

								TintComponent.get_tintTopLeft(model),

								TintComponent.get_tintTopRight(model),

								TintComponent.get_tintBottomLeft(model),

								TintComponent.get_tintBottomRight(model),

						})),

						tintDefault);
				var rgb = Colors.color(color);
				support.setColor(rgb);
				support.updateProvider();
			});
		}

		{
			label(comp, "Tint Top", "*");

			var row = new Composite(comp, 0);
			row.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			var layout = new GridLayout(2, true);
			layout.marginWidth = layout.marginHeight = 0;
			row.setLayout(layout);

			createTintButton(row, TintComponent::get_tintTopLeft, TintComponent::set_tintTopLeft);
			createTintButton(row, TintComponent::get_tintTopRight, TintComponent::set_tintTopRight);
		}

		{
			label(comp, "Tint Bottom", "*");

			var row = new Composite(comp, 0);
			row.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			var layout = new GridLayout(2, true);
			layout.marginWidth = layout.marginHeight = 0;
			row.setLayout(layout);

			createTintButton(row, TintComponent::get_tintBottomLeft, TintComponent::set_tintBottomLeft);
			createTintButton(row, TintComponent::get_tintBottomRight, TintComponent::set_tintBottomRight);
		}

		{
			var btn = new Button(comp, SWT.PUSH);
			btn.setText("Clear Tint");
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				wrapOperation(() -> {
					getModels().forEach(model -> {
						TintComponent.set_tintTopLeft(model, tintDefault);
						TintComponent.set_tintTopRight(model, tintDefault);
						TintComponent.set_tintBottomLeft(model, tintDefault);
						TintComponent.set_tintBottomRight(model, tintDefault);
					});
				});
				update_UI_from_Model();
				getEditor().setDirty(true);
			}));
		}

		return comp;
	}

	@SuppressWarnings("boxing")
	private void createTintButton(Composite row, ComponentGetter<Integer> get, ComponentSetter<Integer> set) {
		{ // top-left
			var btn = new Button(row, SWT.PUSH);
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			var support = ColorButtonSupport.createDefault(btn, rgb -> {
				wrapOperation(() -> {
					int color = Colors.intColor(rgb);
					getModels().forEach(model -> set.set(model, color));
				});
				getEditor().setDirty(true);
			});

			addUpdate(() -> {
				var color = flatValues_to_int(getModels().stream().map(model -> get.get(model)), tintDefault);
				var rgb = Colors.color(color);
				support.setColor(rgb);
				support.updateProvider();
			});
		}
	}

}
