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
package phasereditor.assetpack.ui.widgets;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import javafx.embed.swt.FXCanvas;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

public class VideoResourceDialog extends Dialog {
	CheckboxTableViewer _filesViewer;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public VideoResourceDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@SuppressWarnings("unused")
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));

		Composite composite_2 = new Composite(container, SWT.NONE);
		composite_2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		composite_2.setLayout(new GridLayout(1, false));

		Label lblMessage = new Label(composite_2, SWT.WRAP);
		GridData gd_lblMessage = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_lblMessage.widthHint = 100;
		gd_lblMessage.verticalIndent = 10;
		lblMessage.setLayoutData(gd_lblMessage);
		lblMessage.setText("Check the video files. Those in bold are not yet used in this pack.");

		SashForm sashForm = new SashForm(container, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_filesViewer = CheckboxTableViewer.newCheckList(sashForm, SWT.BORDER | SWT.FULL_SELECTION);
		_filesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				_btnOpenInSystem.setEnabled(!event.getSelection().isEmpty());
				setPlayerFile(event.getSelection());
			}
		});
		Table _table = _filesViewer.getTable();

		Composite composite = new Composite(sashForm, SWT.NONE);
		GridLayout gl_composite = new GridLayout(1, false);
		gl_composite.marginWidth = 0;
		gl_composite.marginHeight = 0;
		gl_composite.verticalSpacing = 0;
		composite.setLayout(gl_composite);

		_videoCanvas = new FXCanvas(composite, SWT.NONE);
		_videoCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		Composite composite_1 = new Composite(composite, SWT.NONE);
		composite_1.setSize(64, 64);
		GridLayout gl_composite_1 = new GridLayout(1, false);
		gl_composite_1.marginWidth = 0;
		composite_1.setLayout(gl_composite_1);

		_btnOpenInSystem = new Button(composite_1, SWT.NONE);
		_btnOpenInSystem.setEnabled(false);
		_btnOpenInSystem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openInSystem();
			}
		});
		_btnOpenInSystem.setText("Open in System Player");
		_filesViewer.setContentProvider(new ArrayContentProvider());
		_filesViewer.setLabelProvider(new LabelProvider());
		sashForm.setWeights(new int[] { 1, 1 });

		afterCreateWidgets();

		return container;
	}

 
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Video Selector");
	}
	
	/**
	 * 
	 */
	protected void openInSystem() {
		ISelection selection = _filesViewer.getSelection();
		if (!selection.isEmpty()) {
			IFile file = (IFile) ((IStructuredSelection) selection).getFirstElement();
			try {
				Desktop.getDesktop().open(new File(file.getLocation().toPortableString()));
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	protected void setPlayerFile(ISelection selection) {
		IFile file = null;
		if (!selection.isEmpty()) {
			file = (IFile) ((IStructuredSelection) selection).getFirstElement();
			try {

				MediaPlayer player = _mediaView.getMediaPlayer();
				if (player != null) {
					player.stop();
					player.dispose();
				}

				String source = file.getLocationURI().toURL().toString();
				Media media = new Media(source);
				player = new MediaPlayer(media);
				player.setAutoPlay(true);
				player.setCycleCount(MediaPlayer.INDEFINITE);
				_mediaView.setMediaPlayer(player);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	private List<IFile> _allFiles;
	private LabelProvider _labelProvider;
	private FXCanvas _videoCanvas;
	private List<IFile> _initialFiles;
	private List<IFile> _selection;
	private MediaView _mediaView;
	Button _btnOpenInSystem;

	public void setInput(List<IFile> files) {
		_allFiles = files;
	}

	public void setLabelProvider(LabelProvider labelProvider) {
		_labelProvider = labelProvider;
	}

	private void afterCreateWidgets() {
		_filesViewer.setLabelProvider(_labelProvider);
		_filesViewer.setInput(_allFiles);
		_filesViewer.setCheckedElements(_initialFiles.toArray());
		_filesViewer.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				updateSelectedFiles();
			}
		});
		_selection = Collections.emptyList();

		// video player
		_mediaView = new MediaView();
		BorderPane pane = new BorderPane(_mediaView);
		pane.setStyle("-fx-background-color:black");
		_mediaView.setPreserveRatio(true);
		Scene scene = new Scene(pane);
		_videoCanvas.setScene(scene);
		_mediaView.fitWidthProperty().bind(scene.widthProperty());
		_mediaView.fitHeightProperty().bind(scene.heightProperty());

	}

	protected void updateSelectedFiles() {
		Object[] elems = _filesViewer.getCheckedElements();
		List<IFile> list = new ArrayList<>();
		for (Object elem : elems) {
			list.add((IFile) elem);
		}
		_selection = list;
	}

	public List<IFile> getSelection() {
		return _selection;
	}

	public void setInitialFiles(List<IFile> selectedFiles) {
		_initialFiles = selectedFiles;
	}

	@Override
	protected void cancelPressed() {
		_initialFiles = null;
		super.cancelPressed();
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(457, 481);
	}
}
