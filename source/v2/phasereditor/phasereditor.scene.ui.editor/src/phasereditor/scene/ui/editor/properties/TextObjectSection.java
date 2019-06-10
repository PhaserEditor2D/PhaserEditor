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

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import phasereditor.scene.core.ComponentGetter;
import phasereditor.scene.core.ComponentSetter;
import phasereditor.scene.core.TextComponent;
import phasereditor.scene.core.TextComponent.Align;
import phasereditor.ui.ColorButtonSupport;
import phasereditor.ui.Colors;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.properties.FormPropertyPage;

/**
 * @author arian
 *
 */
public class TextObjectSection extends ScenePropertySection {

	private List<AlignAction> _alignActions;

	public TextObjectSection(FormPropertyPage page) {
		super("Text Object", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof TextComponent;
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		super.fillToolbar(manager);

		_alignActions.forEach(a -> manager.add(a));
	}

	private static Composite comp_cols(Composite parent, int cols) {
		var comp = new Composite(parent, 0);
		var layout = new GridLayout(cols, false);
		layout.marginWidth = layout.marginHeight = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		return comp;
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {

		createActions();

		var comp = new Composite(parent, 0);

		comp.setLayout(new GridLayout(5, false));

		{
			label(comp, "Fixed Size", "Phaser.GameObjects.Text.setFixedSize");

			{
				label(comp, "Width", "Phaser.GameObjects.TextStyle.fixedWidth");
				var text = new Text(comp, SWT.BORDER);
				text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				new SceneTextToInt(text) {

					@Override
					protected void accept2(int value) {
						getModels().forEach(m -> TextComponent.set_fixedWidth(m, value));
						getEditor().setDirty(true);
					}
				};
				addUpdate(() -> {
					text.setText(flatValues_to_String(getModels().stream().map(TextComponent::get_fixedWidth)));
				});
			}

			{
				label(comp, "Height", "Phaser.GameObjects.TextStyle.fixedHeight");
				var text = new Text(comp, SWT.BORDER);
				text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				new SceneTextToInt(text) {

					@Override
					protected void accept2(int value) {
						getModels().forEach(m -> TextComponent.set_fixedHeight(m, value));
						getEditor().setDirty(true);
					}
				};
				addUpdate(() -> {
					text.setText(flatValues_to_String(getModels().stream().map(TextComponent::get_fixedHeight)));
				});
			}
		}

		{
			// wordWrap
			label(comp, "Word Wrap", "*");
			{
				// width
				label(comp, "Width", "Phaser.GameObjects.Text.wordWrapWidth");
				var text = new Text(comp, SWT.BORDER);
				text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				new SceneTextToInt(text) {

					@Override
					protected void accept2(int value) {
						getModels().forEach(model -> TextComponent.set_wordWrapWidth(model, value));
						getEditor().setDirty(true);
					}
				};

				addUpdate_Text(text, TextComponent::get_wordWrapWidth);
			}
			{
				label(comp, "Use Advanced", "Phaser.GameObjects.Text.wordWrapUseAdvanced");
				var btn = new Button(comp, SWT.CHECK);
				btn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				new SceneCheckListener(btn) {

					@Override
					protected void accept2(boolean value) {
						getModels().forEach(model -> TextComponent.set_wordWrapUseAdvanced(model, value));
						getEditor().setDirty(value);
					}
				};
				addUpdate(() -> {
					btn.setSelection(
							flatValues_to_boolean(getModels().stream().map(TextComponent::get_wordWrapUseAdvanced)));
				});
			}
		}

		// padding
		{
			label(comp, "Padding", "Phaser.GameObjects.Text.setPadding");

			var comp1 = comp_cols(comp, 2);
			comp1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

			var comp2 = comp_cols(comp, 2);
			comp2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

			class MakeLabel {
				public MakeLabel(Composite comp3, String text) {
					var label = new Label(comp3, 0);
					label.setAlignment(SWT.CENTER);
					label.setText(text);
					label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				}
			}

			new MakeLabel(comp1, "Left");
			new MakeLabel(comp1, "Top");
			new MakeLabel(comp2, "Right");
			new MakeLabel(comp2, "Bottom");

			@SuppressWarnings("boxing")
			class MakeText {
				@SuppressWarnings("synthetic-access")
				MakeText(Composite comp3, ComponentGetter<Float> getter, ComponentSetter<Float> setter) {
					var text = new Text(comp3, SWT.BORDER);
					text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
					new SceneTextToFloat(text) {

						@Override
						protected void accept2(float value) {
							getModels().forEach(model -> setter.set(model, value));
						}
					};
					addUpdate(() -> {
						text.setText(flatValues_to_String(getModels().stream().map(model -> getter.get(model))));
						getEditor().setDirty(true);
					});
				}
			}

			new Label(comp, 0);

			comp1 = comp_cols(comp, 2);
			comp1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

			comp2 = comp_cols(comp, 2);
			comp2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

			new MakeText(comp1, TextComponent::get_paddingLeft, TextComponent::set_paddingLeft);
			new MakeText(comp1, TextComponent::get_paddingTop, TextComponent::set_paddingTop);
			new MakeText(comp2, TextComponent::get_paddingRight, TextComponent::set_paddingRight);
			new MakeText(comp2, TextComponent::get_paddingBottom, TextComponent::set_paddingBottom);

		}

		// lineSpacing
		{
			label(comp, "Line Spacing", "Phaser.GameObjects.Text.lineSpacing");
			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
			new SceneTextToFloat(text) {

				@Override
				protected void accept2(float value) {
					getModels().forEach(model -> TextComponent.set_lineSpacing(model, value));
					getEditor().setDirty(true);
				}
			};
			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream().map(TextComponent::get_lineSpacing)));
			});
		}

		// align
		{
			label(comp, "Align", "Phaser.GameObjects.Text.setAlign");

			var manager = new ToolBarManager();

			_alignActions.forEach(a -> manager.add(a));

			manager.createControl(comp)

					.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));

			addUpdate(() -> {
				_alignActions.forEach(a -> a.update());
			});
		}

		// fontFamily
		{
			label(comp, "Font Family", "Phaser.GameObjects.Text.setFontFamily");

			var comp2 = comp_cols(comp, 2);

			var text = new Text(comp2, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new SceneText(text) {
				@Override
				protected void accept2(String value) {
					getModels().forEach(model -> TextComponent.set_fontFamily(model, value));
					getEditor().setDirty(true);
				}
			};
			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream().map(TextComponent::get_fontFamily)));
			});

			createMenuIconToolbar(comp2, menu -> {
				getDisplayList()

						.treeStream()

						.filter(TextComponent::is)

						.map(TextComponent::get_fontFamily)

						.distinct()

						.forEach(family -> {
							menu.add(new Action(family) {

								@Override
								public void run() {
									wrapOperation(() -> {
										getModels().forEach(model -> TextComponent.set_fontFamily(model, family));
									});

									getEditor().setDirty(true);

									update_UI_from_Model();
								}
							});
						});
			});
		}

		// fontSize
		{
			label(comp, "Font Size", "Phaser.GameObjects.Text.setFontSize");
			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
			new SceneText(text) {

				@Override
				protected void accept2(String value) {
					getModels().forEach(model -> TextComponent.set_fontSize(model, value));
					getEditor().setDirty(true);
				}
			};
			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream().map(TextComponent::get_fontSize)));
			});
		}

		// fontStyle
		{
			label(comp, "Font Style", "Phaser.GameObjects.Text.setFontStyle");

			var btn = new Button(comp, SWT.PUSH);
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				var menu = new MenuManager();
				for (var style : TextComponent.FontStyle.values()) {
					menu.add(new Action(style.name().toUpperCase()) {
						@Override
						public void run() {
							wrapOperation(() -> {
								getModels().forEach(m -> TextComponent.set_fontStyle(m, style));
							});
							getEditor().setDirty(true);
							update_UI_from_Model();
						}
					});
					menu.createContextMenu(btn).setVisible(true);
				}
			}));

			addUpdate(() -> {
				var style = flatValues_to_Object(getModels().stream().map(TextComponent::get_fontStyle));
				btn.setText(style == null ? "" : style.toString().toUpperCase());
			});
		}

		// color
		createWebColorField(comp, "Color", TextComponent::get_color, TextComponent::set_color,
				"Phaser.GameObjects.Text.setColor");

		// stroke
		createWebColorField(comp, "Stroke", TextComponent::get_stroke, TextComponent::set_stroke,
				"Phaser.GameObjects.Text.setStroke");

		// strokeThickness
		{
			label(comp, "Stroke Thickness", "Phaser.GameObjects.TextStyle.strokeThickness");
			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
			new SceneTextToFloat(text) {

				@Override
				protected void accept2(float value) {
					getModels().forEach(m -> TextComponent.set_strokeThickness(m, value));
					getEditor().setDirty(true);
				}
			};
			addUpdate(() -> {
				text.setText(
						Float.toString(flatValues_to_float(getModels().stream().map(TextComponent::get_strokeThickness),
								TextComponent.strokeThickness_default)));
			});
		}

		// backgroundColor
		createWebColorField(comp, "Background Color", TextComponent::get_backgroundColor,
				TextComponent::set_backgroundColor, "Phaser.GameObjects.Text.setBackgroundColor");

		// shadowOffset
		{

			label(comp, "Shadow Offset", "Phaser.GameObjects.Text.setShadowOffset");

			{
				label(comp, "X", "Phaser.GameObjects.TextStyle.shadowOffsetX");
				var text = new Text(comp, SWT.BORDER);
				text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				new SceneTextToInt(text) {

					@Override
					protected void accept2(int value) {
						getModels().forEach(m -> TextComponent.set_shadowOffsetX(m, value));
						getEditor().setDirty(true);
					}
				};
				addUpdate(() -> {
					text.setText(flatValues_to_String(getModels().stream().map(TextComponent::get_shadowOffsetX)));
				});
			}
			{
				label(comp, "Y", "Phaser.GameObjects.TextStyle.shadowOffsetY");
				var text = new Text(comp, SWT.BORDER);
				text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				new SceneTextToInt(text) {

					@Override
					protected void accept2(int value) {
						getModels().forEach(m -> TextComponent.set_shadowOffsetY(m, value));
						getEditor().setDirty(true);
					}
				};
				addUpdate(() -> {
					text.setText(flatValues_to_String(getModels().stream().map(TextComponent::get_shadowOffsetY)));
				});
			}
		}

		{ // shadow
			label(comp, "Shadow", "*");

			// stroke
			{
				label(comp, "Stroke", "Phaser.GameObjects.TextStyle.shadowStroke");
				var btn = new Button(comp, SWT.CHECK);
				btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
				new SceneCheckListener(btn) {

					@Override
					protected void accept2(boolean value) {
						getModels().forEach(m -> TextComponent.set_shadowStroke(m, value));
						getEditor().setDirty(true);
					}
				};
				addUpdate(() -> {
					btn.setSelection(flatValues_to_boolean(getModels().stream().map(TextComponent::get_shadowStroke)));
				});
			}

			// fill
			{
				label(comp, "Fill", "Phaser.GameObjects.TextStyle.shadowFill");
				var btn = new Button(comp, SWT.CHECK);
				btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				new SceneCheckListener(btn) {

					@Override
					protected void accept2(boolean value) {
						getModels().forEach(m -> TextComponent.set_shadowFill(m, value));
						getEditor().setDirty(true);
					}
				};
				addUpdate(() -> {
					btn.setSelection(flatValues_to_boolean(getModels().stream().map(TextComponent::get_shadowFill)));
				});
			}
		}

		// shadowColor
		createWebColorField(comp, "Shadow Color", TextComponent::get_shadowColor, TextComponent::set_shadowColor,
				"Phaser.GameObjects.TextStyle.shadowColor");

		// shadowBlur
		{
			label(comp, "Shadow Blur", "Phaser.GameObjects.TextStyle.shadowBlur");
			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
			new SceneTextToInt(text) {

				@Override
				protected void accept2(int value) {
					getModels().forEach(m -> TextComponent.set_shadowBlur(m, value));
					getEditor().setDirty(true);
				}

			};
			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream().map(TextComponent::get_shadowBlur)));
			});

		}

		// baseline
		{
			label(comp, "Baseline", "*");

			{
				label(comp, "X", "Phaser.GameObjects.TextStyle.baselineX");
				var text = new Text(comp, SWT.BORDER);
				text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				new SceneTextToInt(text) {

					@Override
					protected void accept2(int value) {
						getModels().forEach(m -> TextComponent.set_baselineX(m, value));
						getEditor().setDirty(true);
					}
				};
				addUpdate(() -> {
					text.setText(flatValues_to_String(getModels().stream().map(TextComponent::get_baselineX)));
				});
			}

			{
				label(comp, "Height", "Phaser.GameObjects.TextStyle.baselineY");
				var text = new Text(comp, SWT.BORDER);
				text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				new SceneTextToInt(text) {

					@Override
					protected void accept2(int value) {
						getModels().forEach(m -> TextComponent.set_baselineY(m, value));
						getEditor().setDirty(true);
					}
				};
				addUpdate(() -> {
					text.setText(flatValues_to_String(getModels().stream().map(TextComponent::get_baselineY)));
				});
			}
		}

		// maxLines
		{
			label(comp, "Max Lines", "Phaser.GameObjects.TextStyle.maxLines");

			var text = new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
			new SceneTextToInt(text) {

				@Override
				protected void accept2(int value) {
					getModels().forEach(model -> TextComponent.set_maxLines(model, value));
					getEditor().setDirty(true);
				}
			};
			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream().map(TextComponent::get_maxLines)));
			});
		}

		return comp;
	}

	@SuppressWarnings("unused")
	private void createWebColorField(Composite comp, String name, ComponentGetter<String> getter,
			ComponentSetter<String> set, String helpId) {
		label(comp, name, helpId);

		var comp2 = comp_cols(comp, 2);

		{
			var text = new Text(comp2, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			new SceneText(text) {

				@Override
				protected void accept2(String value) {
					getModels().forEach(model -> set.set(model, value));
					update_UI_from_Model();
				}
			};

			addUpdate(() -> {
				text.setText(flatValues_to_String(getModels().stream().map(getter::get)));
			});
		}

		{

			var btn = new Button(comp2, SWT.PUSH);
			var support = ColorButtonSupport.createDefault(btn, rgb -> {
				var color = "#" + Colors.hexColor(rgb);
				wrapOperation(() -> {
					getModels().forEach(model -> {
						set.set(model, color);
					});
				});
				getEditor().setDirty(true);
				update_UI_from_Model();
			});
			support.setShowText(false);

			addUpdate(() -> {
				var color = flatValues_to_String(getModels().stream().map(getter::get));
				if (color.length() == 0) {
					support.setColor(null);
					support.updateProvider();
				} else {
					support.setColor(Colors.rgb(color).rgb);
					support.updateProvider();
				}
			});
		}

	}

	private void createActions() {
		_alignActions = List.of(

				new AlignAction(Align.left, IMG_TEXT_ALIGN_LEFT),

				new AlignAction(Align.center, IMG_TEXT_ALIGN_CENTER),

				new AlignAction(Align.right, IMG_TEXT_ALIGN_RIGHT),

				new AlignAction(Align.justified, IMG_TEXT_ALIGN_JUSTIFY)

		);
	}

	class AlignAction extends Action {
		private TextComponent.Align _align;

		public AlignAction(Align align, String icon) {
			super("", AS_CHECK_BOX);
			_align = align;
			setImageDescriptor(EditorSharedImages.getImageDescriptor(icon));
			setToolTipText("Align to '" + _align.name() + "'");
		}

		@SuppressWarnings("synthetic-access")
		public void update() {
			var align = flatValues_to_Object(getModels().stream().map(TextComponent::get_align));
			setChecked(_align == align);
		}

		@Override
		public void run() {
			wrapOperation(() -> {
				getModels().forEach(model -> TextComponent.set_align(model, _align));
			});

			getEditor().setDirty(true);

			update_UI_from_Model();
		}

	}
}
