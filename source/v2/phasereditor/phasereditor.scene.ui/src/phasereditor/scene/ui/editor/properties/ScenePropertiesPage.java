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
import static phasereditor.ui.IEditorSharedImages.IMG_BULLET_COLLAPSE;
import static phasereditor.ui.IEditorSharedImages.IMG_BULLET_EXPAND;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.scene.core.EditorComponent;
import phasereditor.scene.core.FlipComponent;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.SpriteModel;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.core.VisibleComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
public class ScenePropertiesPage extends Page implements IPropertySheetPage {

	Composite _sectionsContainer;
	private SceneEditor _editor;

	public ScenePropertiesPage(SceneEditor editor) {
		super();
		_editor = editor;
	}

	public SceneEditor getEditor() {
		return _editor;
	}

	@SuppressWarnings("unused")
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {

		var models = ((IStructuredSelection) selection).toArray();

		// create all candidate sections
		var allSections = new ArrayList<PropertySection>();
		{
			var clsMap = new HashMap<Object, PropertySection>();
			for (var model : models) {
				var objSections = createSections(model);

				for (var section : objSections) {
					if (!clsMap.containsKey(section.getClass())) {
						clsMap.put(section.getClass(), section);
						allSections.add(section);
					}
				}
			}
		}

		// pick only unique sections

		var uniqueSections = new ArrayList<PropertySection>();
		var sectionMap = new HashMap<>();

		for (var section : allSections) {
			var accept = true;
			for (var model : models) {
				if (!section.canEdit(model)) {
					accept = false;
					break;
				}
			}
			if (accept) {
				uniqueSections.add(section);
				sectionMap.put(section.getClass(), section);
			}
		}

		// dispose all rows with irrelevant sections

		for (var control : _sectionsContainer.getChildren()) {
			var row = (RowComp) control;

			var newSection = sectionMap.get(row.getSection().getClass());

			if (newSection == null) {
				row.dispose();
			}
		}

		// create the missing rows, or update current rows

		for (var section : uniqueSections) {
			var createNew = true;

			for (var control : _sectionsContainer.getChildren()) {
				var row = (RowComp) control;

				var oldSection = row.getSection();

				if (oldSection.getClass() == section.getClass()) {
					oldSection.setModels(models);
					oldSection.update_UI_from_Model();
					createNew = false;
					break;
				}
			}

			if (createNew) {
				section.setModels(models);
				new RowComp(_sectionsContainer, section);
			}

		}

		// sort the rows, following the new sections order

		var clsList = uniqueSections.stream().map(section -> section.getClass()).collect(toList());

		var rowList = _sectionsContainer.getChildren();

		Arrays.sort(rowList, (a, b) -> {
			return Integer.compare(clsList.indexOf(a.getClass()), clsList.indexOf(b.getClass()));
		});

		_sectionsContainer.layout(rowList);
	}

	public class RowComp extends Composite {

		private PropertySection _section;

		public RowComp(Composite parent, PropertySection section) {
			super(parent, SWT.NONE);

			_section = section;

			{
				var gl = new GridLayout(1, false);
				gl.marginWidth = 0;
				gl.marginHeight = 0;
				setLayout(gl);
				setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			}

			var sectionId = section.getClass().getSimpleName();

			var collapsed = _collapsedSectionsIds.contains(sectionId);

			var header = new Composite(this, SWT.NONE);
			header.setLayout(new RowLayout());

			var collapseBtn = new Label(header, SWT.NONE);
			collapseBtn.setImage(EditorSharedImages.getImage(collapsed ? IMG_BULLET_EXPAND : IMG_BULLET_COLLAPSE));

			var title = new Label(header, SWT.NONE);
			title.setText(section.getName());
			title.setFont(SWTResourceManager.getBoldFont(title.getFont()));

			var control = section.createContent(this);
			control.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

			if (collapsed) {
				((GridData) control.getLayoutData()).heightHint = 0;
				control.setVisible(false);
			}

			var sep = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
			sep.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

			var expandListener = new MouseAdapter() {
				boolean _collapsed = false;

				@Override
				public void mouseUp(MouseEvent e) {
					_collapsed = !_collapsed;

					var img = EditorSharedImages.getImage(_collapsed ? IEditorSharedImages.IMG_BULLET_EXPAND
							: IEditorSharedImages.IMG_BULLET_COLLAPSE);

					collapseBtn.setImage(img);

					control.setVisible(!_collapsed);

					var gd = (GridData) control.getLayoutData();
					gd.heightHint = _collapsed ? 0 : SWT.DEFAULT;

					control.requestLayout();

					if (_collapsed) {
						_collapsedSectionsIds.add(sectionId);
					} else {
						_collapsedSectionsIds.remove(sectionId);
					}

				}
			};

			title.addMouseListener(expandListener);
			collapseBtn.addMouseListener(expandListener);
		}

		public PropertySection getSection() {
			return _section;
		}

	}

	static Set<String> _collapsedSectionsIds = new HashSet<>();

	private List<PropertySection> createSections(Object obj) {
		List<PropertySection> list = new ArrayList<>();

		if (obj instanceof EditorComponent) {
			list.add(new EditorSection(this));
		}

		if (obj instanceof TransformComponent) {
			list.add(new TransformSection(this));
		}

		if (obj instanceof OriginComponent) {
			list.add(new OriginSection(this));
		}

		if (obj instanceof FlipComponent) {
			list.add(new FlipSection(this));
		}

		if (obj instanceof VisibleComponent) {
			list.add(new VisibleSection(this));
		}

		if (obj instanceof TextureComponent) {
			list.add(new TextureSection(this));
		}

		if (obj instanceof SpriteModel) {
			list.add(new EmptyBodySection(this));
		}

		return list;
	}

	@Override
	public void createControl(Composite parent) {
		_sectionsContainer = new Composite(parent, SWT.NONE);
		_sectionsContainer.setLayout(new GridLayout(1, false));
	}

	@Override
	public Control getControl() {
		return _sectionsContainer;
	}

	@Override
	public void setFocus() {
		_sectionsContainer.setFocus();
	}

	@Override
	public void dispose() {

		_editor.removePropertyPage(this);

		super.dispose();
	}

}
