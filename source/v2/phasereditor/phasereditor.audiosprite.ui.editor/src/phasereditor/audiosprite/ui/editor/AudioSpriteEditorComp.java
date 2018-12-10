// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.audiosprite.ui.editor;

import static phasereditor.ui.PhaserEditorUI.pickFileWithoutExtension;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;

import phasereditor.audio.ui.GdxMusicControl;
import phasereditor.audiosprite.core.AudioSprite;
import phasereditor.audiosprite.core.AudioSpriteCore;
import phasereditor.audiosprite.core.AudioSpritesModel;
import phasereditor.audiosprite.ui.UpdateAudioSpritesJob;
import phasereditor.ui.RM;

public class AudioSpriteEditorComp extends Composite {
	public static final String PROP_DIRTY = "dirty";

	private static class Sorter extends ViewerComparator {
		public Sorter() {
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			AudioSprite item1 = (AudioSprite) e1;
			AudioSprite item2 = (AudioSprite) e2;
			return item1.compareTo(item2);
		}
	}

	protected GdxMusicControl _musicControl;
	TableViewer _spritesViewer;
	private List<AudioSprite> _sprites;
	private AudioSpritesModel _model;
	private Composite _buttonsComp;
	private AudioSprite _lastSelectedSprite;
	private Label _filesLabel;
	private Action _addAction;
	private Action _removeAction;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public AudioSpriteEditorComp(Composite parent, int style) {
		super(parent, style);

		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = 5;
		setLayout(gridLayout);

		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

		Label lblFiles = new Label(composite, SWT.NONE);
		lblFiles.setText("files: ");

		_filesLabel = new Label(composite, SWT.WRAP | SWT.HORIZONTAL);
		GridData gd_filesLabel = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_filesLabel.widthHint = 50;
		_filesLabel.setLayoutData(gd_filesLabel);
		_filesLabel.setText("file.ogg...");

		SashForm sashForm_1 = new SashForm(this, SWT.VERTICAL);
		sashForm_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_musicControl = new GdxMusicControl(sashForm_1, SWT.NONE);
		_musicControl.getCanvas().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				handleKey(e);
			}
		});
		GridLayout gridLayout_1 = (GridLayout) _musicControl.getLayout();
		gridLayout_1.marginBottom = 5;
		gridLayout_1.marginHeight = 0;
		gridLayout_1.marginWidth = 0;

		Composite composite_2 = new Composite(sashForm_1, SWT.NONE);
		GridLayout gl_composite_2 = new GridLayout(1, false);
		gl_composite_2.marginWidth = 0;
		gl_composite_2.marginHeight = 0;
		composite_2.setLayout(gl_composite_2);

		_spritesViewer = new TableViewer(composite_2, SWT.BORDER | SWT.FULL_SELECTION);
		_spritesViewer.setComparator(new Sorter());
		_spritesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				spriteSelected();
			}
		});
		Table spritesTable = _spritesViewer.getTable();
		spritesTable.setLinesVisible(true);
		spritesTable.setHeaderVisible(true);
		spritesTable.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				handleKey(e);
			}
		});
		spritesTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		TableViewerColumn _tableViewerColumn = new TableViewerColumn(_spritesViewer, SWT.NONE);
		_tableViewerColumn.setEditingSupport(new EditingSupport(_spritesViewer) {

			@Override
			protected boolean canEdit(Object element) {
				return canEdit2(element);
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(spritesTable);
			}

			@Override
			protected Object getValue(Object element) {
				return ((AudioSprite) element).getName();
			}

			@Override
			protected void setValue(Object element, Object value) {
				((AudioSprite) element).setName((String) value);
				_spritesViewer.refresh();
				updateMusicControlWithSpritesChange();
				firePropertyChange(PROP_DIRTY);
			}
		});
		_tableViewerColumn.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				AudioSprite sprite = (AudioSprite) element;
				return sprite.getName();
			}
		});
		TableColumn _tableColumn = _tableViewerColumn.getColumn();
		_tableColumn.setWidth(224);
		_tableColumn.setText("Sprite Name");

		TableViewerColumn _tableViewerColumn_1 = new TableViewerColumn(_spritesViewer, SWT.NONE);
		_tableViewerColumn_1.setEditingSupport(new EditingSupport(_spritesViewer) {
			@Override
			protected boolean canEdit(Object element) {
				return canEdit2(element);
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(spritesTable);
			}

			@Override
			protected Object getValue(Object element) {
				return Double.toString(((AudioSprite) element).getStart());
			}

			@Override
			protected void setValue(Object element, Object value) {
				double time = Double.parseDouble((String) value);
				AudioSprite sprite = (AudioSprite) element;
				if (time < sprite.getEnd()) {
					((AudioSprite) element).setStart(time);
					_spritesViewer.refresh();
					updateMusicControlWithSpritesChange();
					firePropertyChange(PROP_DIRTY);
				}
			}
		});
		_tableViewerColumn_1.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				AudioSprite sprite = (AudioSprite) element;
				return Double.toString(sprite.getStart());
			}
		});
		TableColumn _tableColumn_1 = _tableViewerColumn_1.getColumn();
		_tableColumn_1.setWidth(158);
		_tableColumn_1.setText("Start (secs)");

		TableViewerColumn _tableViewerColumn_2 = new TableViewerColumn(_spritesViewer, SWT.NONE);
		_tableViewerColumn_2.setEditingSupport(new EditingSupport(_spritesViewer) {
			@Override
			protected boolean canEdit(Object element) {
				return canEdit2(element);
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(spritesTable);
			}

			@Override
			protected Object getValue(Object element) {
				return Double.toString(((AudioSprite) element).getEnd());
			}

			@Override
			protected void setValue(Object element, Object value) {
				double time = Double.parseDouble((String) value);
				AudioSprite sprite = (AudioSprite) element;
				if (time > sprite.getStart()) {
					sprite.setEnd(time);
					_spritesViewer.refresh();
					updateMusicControlWithSpritesChange();
					firePropertyChange(PROP_DIRTY);
				}
			}
		});
		_tableViewerColumn_2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				AudioSprite sprite = (AudioSprite) element;
				return Double.toString(sprite.getEnd());
			}
		});
		TableColumn _tableColumn_2 = _tableViewerColumn_2.getColumn();
		_tableColumn_2.setWidth(209);
		_tableColumn_2.setText("End (secs)");

		_spritesToolbar = new ToolBar(composite_2, SWT.FLAT | SWT.RIGHT);

		_buttonsComp = new Composite(composite_2, SWT.NONE);
		GridLayout gl_composite_1 = new GridLayout(1, false);
		gl_composite_1.marginHeight = 0;
		gl_composite_1.marginWidth = 0;
		_buttonsComp.setLayout(gl_composite_1);
		_spritesViewer.setContentProvider(new ArrayContentProvider());
		sashForm_1.setWeights(new int[] { 1, 1 });

		createActions();

		afterCreateWidgets();
	}

	protected void handleKey(KeyEvent e) {
		switch (e.character) {
		case '+':
			addSprite();
			e.doit = true;
			break;
		case '-':
		case SWT.DEL:
			deleteSprite();
			e.doit = true;
			break;
		default:
			break;
		}
	}

	private void createActions() {
		_addAction = new Action("add", RM.getPluginImageDescriptor("phasereditor.ui", "icons/add.png")) {
			@Override
			public void run() {
				addSprite();
			}
		};

		_removeAction = new Action("remove",
				RM.getPluginImageDescriptor("phasereditor.ui", "icons/delete.png")) {
			@Override
			public void run() {
				deleteSprite();
			}
		};
	}

	private void afterCreateWidgets() {
		_musicControl.setPaintTimeCursor(true);

		ToolBarManager manager = new ToolBarManager(_spritesToolbar);
		manager.add(_addAction);
		manager.add(_removeAction);
		manager.update(true);

		{
			int options = DND.DROP_MOVE | DND.DROP_DEFAULT;
			DropTarget target = new DropTarget(this, options);
			Transfer[] types = { LocalSelectionTransfer.getTransfer() };
			target.setTransfer(types);
			target.addDropListener(new DropTargetAdapter() {
				@Override
				public void drop(DropTargetEvent event) {
					if (event.data instanceof ISelection) {
						selectionDropped((ISelection) event.data);
					}
				}
			});
		}
	}

	protected void selectionDropped(ISelection data) {
		if (data instanceof IStructuredSelection) {
			Object[] list = ((IStructuredSelection) data).toArray();
			List<IFile> files = new ArrayList<>();
			for (Object obj : list) {
				if (obj instanceof IFile) {
					files.add((IFile) obj);
				}
			}

			UpdateAudioSpritesJob job = new UpdateAudioSpritesJob(_model, files);
			job.addJobChangeListener(new JobChangeAdapter() {

				@Override
				public void done(IJobChangeEvent event) {
					boolean result = event.getResult().isOK();
					if (result && !AudioSpriteEditorComp.this.isDisposed()) {
						swtRun(new Runnable() {

							@Override
							public void run() {
								MessageDialog.openInformation(getShell(), "Update Audio Sprite",
										"The audio sprite was updated");
								refreshContent();
							}
						});

					}
				}
			});
			job.schedule();
		}
	}

	protected void refreshContent() {
		if (isDisposed()) {
			return;
		}

		setModel(_model);
	}

	protected boolean canEdit2(Object element) {
		return _lastSelectedSprite == element;
	}

	protected void updateMusicControlWithSpritesChange() {
		_musicControl.setTimePartition(AudioSpriteCore.createTimePartition(_sprites));
	}

	protected void updateButtonsFromSelectionChanged() {
		_removeAction.setEnabled(!_spritesViewer.getSelection().isEmpty());
	}

	protected void deleteSprite() {
		IStructuredSelection selection = (IStructuredSelection) _spritesViewer.getSelection();

		if (selection.isEmpty()) {
			return;
		}

		List<AudioSprite> sprites = _model.getSprites();

		sprites.removeAll(Arrays.asList(selection.toArray()));

		_musicControl.setTimePartitionSelection(-1);
		updateMusicControlWithSpritesChange();

		_spritesViewer.refresh();

		firePropertyChange(PROP_DIRTY);
	}

	protected void addSprite() {
		AudioSprite sprite = new AudioSprite();
		List<AudioSprite> sprites = _model.getSprites();
		int count = sprites.size();
		sprite.setName("sprite" + (count + 1));

		if (count > 0) {
			sprite.setStart(sprites.get(count - 1).getEnd() + 0.1);
		}

		sprite.setEnd(_musicControl.getDuration());

		sprites.add(sprite);

		_musicControl.setTimePartition(AudioSpriteCore.createTimePartition(sprites));

		_spritesViewer.refresh();

		_spritesViewer.setSelection(new StructuredSelection(sprite));

		firePropertyChange(PROP_DIRTY);
	}

	protected void spriteSelected() {
		IStructuredSelection sel = (IStructuredSelection) _spritesViewer.getSelection();

		if (!sel.isEmpty()) {
			AudioSprite sprite = (AudioSprite) sel.getFirstElement();
			_lastSelectedSprite = sprite;
			if (sprite.isValid()) {
				updateSprite(sprite);
			} else {
				updateSprite(null);
			}
		}

		updateButtonsFromSelectionChanged();
	}

	private void updateSprite(AudioSprite sprite) {
		_musicControl.stop();
		_musicControl.setTimePartitionSelection(_model.getSprites().indexOf(sprite));
	}

	public void setModel(AudioSpritesModel model) {
		_model = model;
		_sprites = model.getSprites();

		_spritesViewer.setInput(_sprites);
		// FF will be set after select the file
		List<IFile> files = _model.getResources();

		IFile playFile = pickFileWithoutExtension(files, "ogg", "mp3");
		try {
			_musicControl.load(playFile);
		} catch (Exception e) {
			e.printStackTrace();
			MessageDialog.openError(getShell(), "Audio Sprite Editor",
					"Error loading file '" + playFile.getFullPath().toPortableString() + "'.\n\n" + e.getMessage());
		}

		_musicControl.setTimePartition(AudioSpriteCore.createTimePartition(_sprites));
		{
			String label = files.isEmpty() ? "(empty)" : "";

			for (IFile file : files) {
				if (label.length() > 0) {
					label += ", ";
				}
				label += file.getName();
			}
			_filesLabel.setText(label);
		}
	}

	public AudioSpritesModel getModel() {
		return _model;
	}

	private transient final PropertyChangeSupport support = new PropertyChangeSupport(this);
	private ToolBar _spritesToolbar;

	public void addPropertyChangeListener(PropertyChangeListener l) {
		support.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		support.removePropertyChangeListener(l);
	}

	public void addPropertyChangeListener(String property, PropertyChangeListener l) {
		support.addPropertyChangeListener(property, l);
	}

	public void removePropertyChangeListener(String property, PropertyChangeListener l) {
		support.removePropertyChangeListener(property, l);
	}

	public void firePropertyChange(String property) {
		support.firePropertyChange(property, true, false);
	}
}
