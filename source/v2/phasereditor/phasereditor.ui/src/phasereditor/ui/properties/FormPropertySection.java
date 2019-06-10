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
package phasereditor.ui.properties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;

import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
public abstract class FormPropertySection<T> implements IEditorSharedImages {
	public List<T> _models;
	private String _name;
	private boolean _fillSpace;
	private List<Runnable> _updates;
	private boolean _startCollapsed;

	public FormPropertySection(String name) {
		_name = name;
		_models = new ArrayList<>();
		_fillSpace = false;
		_updates = new ArrayList<>();
	}

	public List<Runnable> getUpdates() {
		return _updates;
	}

	public void addUpdate(Runnable update) {
		_updates.add(update);
	}

	protected void addUpdate_Text(Text text, Function<T, Object> mapper) {
		text.setText(flatValues_to_String(getModels().stream().map(model -> mapper.apply(model))));
	}

	public boolean isFillSpace() {
		return _fillSpace;
	}

	public void setFillSpace(boolean fillSpace) {
		_fillSpace = fillSpace;
	}

	public String getName() {
		return _name;
	}

	public List<T> getModels() {
		return _models;
	}

	public void setModels(List<T> models) {
		_models = new ArrayList<>(models);
	}

	public void setModels(T[] models) {
		_models = new ArrayList<>();

		for (var model : models) {
			_models.add(model);
		}
	}

	protected static String flatValues_to_String(Stream<?> values) {
		var set = new HashSet<>();
		values.forEach(v -> set.add(v));

		if (set.size() == 1) {
			var value = set.toArray()[0];

			if (value == null) {
				return "";
			}

			return value.toString();
		}

		return "";
	}

	protected static Object flatValues_to_Object(Stream<?> values) {
		var set = new HashSet<>();
		values.forEach(v -> set.add(v));

		if (set.size() == 1) {
			var value = set.toArray()[0];
			return value;
		}

		return null;
	}

	@SuppressWarnings("boxing")
	protected static int flatValues_to_int(Stream<Integer> values, int def) {
		var set = new HashSet<>();
		values.forEach(v -> set.add(v));

		if (set.size() == 1) {
			var value = set.toArray()[0];
			return (int) value;
		}

		return def;
	}

	@SuppressWarnings("boxing")
	protected static float flatValues_to_float(Stream<Float> values, float def) {
		var set = new HashSet<>();
		values.forEach(v -> set.add(v));

		if (set.size() == 1) {
			var value = set.toArray()[0];
			return (float) value;
		}

		return def;
	}

	protected static Boolean flatValues_to_Boolean(Stream<Boolean> values) {
		var set = new HashSet<>();
		values.forEach(v -> set.add(v));

		if (set.size() == 1) {
			var value = set.toArray()[0];
			return (Boolean) value;
		}

		return Boolean.FALSE;
	}

	protected static boolean flatValues_to_boolean(Stream<Boolean> values) {
		Boolean value = flatValues_to_Boolean(values);
		return value != null && value.booleanValue();
	}

	protected static <T> void setValues_to_Text(Text text, List<T> models, Function<T, Object> get) {
		text.setText(flatValues_to_String(models.stream().map(model -> get.apply(model))));
	}

	protected Label label(Composite parent, String title, String helpId) {
		return label(parent, title, helpId, null);
	}

	protected Label label(Composite parent, String title, String helpId, GridData gd) {
		var label = new Label(parent, SWT.NONE);
		label.setText(title);

		if (gd != null) {
			label.setLayoutData(gd);
		}

		if (helpId != null) {
			var help = getHelp(helpId);
			if (help != null) {
				label.setToolTipText(help);
			}
		}
		return label;
	}

	@SuppressWarnings({ "static-method", "unused" })
	protected String getHelp(String helpHint) {
		return null;
	}

	@SuppressWarnings({ "unused", "static-method" })
	public boolean supportThisNumberOfModels(int number) {
		return true;
	}

	public abstract boolean canEdit(Object obj);

	public abstract Control createContent(Composite parent);

	public void user_update_UI_from_Model() {
		// nothing by default, now you can use updaters
	}

	@SuppressWarnings("unused")
	public void fillToolbar(ToolBarManager manager) {
		// nothing
	}

	public final void update_UI_from_Model() {
		if (!getModels().isEmpty()) {

			for (var update : _updates) {
				update.run();
			}

			user_update_UI_from_Model();
		}
	}

	@SuppressWarnings("unused")
	protected void visibilityChanged(boolean visible) {
		// nothing
	}

	protected static ToolBar createMenuIconToolbar(Composite parent, Consumer<IMenuManager> menuBuilder) {
		var toolbarManager = new ToolBarManager();
		toolbarManager.add(new Action("", EditorSharedImages.getImageDescriptor(IMG_BULLET_MENU)) {
			@Override
			public void run() {
				var menuManager = new MenuManager();
				menuBuilder.accept(menuManager);
				if (menuManager.isEmpty()) {
					menuManager.add(new Action("- No options available -") {
						//
					});
				}
				var menu = menuManager.createContextMenu(parent);
				menu.setVisible(true);
			}
		});
		return toolbarManager.createControl(parent);
	}

	public boolean isStartCollapsed() {
		return _startCollapsed;
	}

	public void setStartCollapsed(boolean startCollapsed) {
		_startCollapsed = startCollapsed;
	}
}
