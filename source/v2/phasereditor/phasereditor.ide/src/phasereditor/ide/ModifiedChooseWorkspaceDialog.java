/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jan-Ove Weichel <janove.weichel@vogella.com> - Bugs 411578, 486842, 487673
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 492918
 *******************************************************************************/
package phasereditor.ide;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Geometry;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.internal.ide.ChooseWorkspaceData;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

/**
 * A dialog that prompts for a directory to use as a workspace.
 */
@SuppressWarnings("all")
public class ModifiedChooseWorkspaceDialog extends Dialog {

	private static final String DIALOG_SETTINGS_SECTION = "ChooseWorkspaceDialogSettings"; //$NON-NLS-1$

	private ChooseWorkspaceData launchData;

	private Text text;

	private boolean suppressAskAgain = false;

	private boolean centerOnMonitor = false;

	private Map<String, Link> recentWorkspacesLinks;

	private Form recentWorkspacesForm;

	/**
	 * Create a modal dialog on the arugment shell, using and updating the argument
	 * data object.
	 * 
	 * @param parentShell
	 *            the parent shell for this dialog
	 * @param launchData
	 *            the launch data from past launches
	 *
	 * @param suppressAskAgain
	 *            true means the dialog will not have a "don't ask again" button
	 * @param centerOnMonitor
	 *            indicates whether the dialog should be centered on the monitor or
	 *            according to it's parent if there is one
	 */
	public ModifiedChooseWorkspaceDialog(Shell parentShell, ChooseWorkspaceData launchData, boolean suppressAskAgain,
			boolean centerOnMonitor) {
		super(parentShell);
		this.launchData = launchData;
		this.suppressAskAgain = suppressAskAgain;
		this.centerOnMonitor = centerOnMonitor;
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(newShellStyle | SWT.NO_TRIM);
	}

	/**
	 * Show the dialog to the user (if needed). When this method finishes,
	 * #getSelection will return the workspace that should be used (whether it was
	 * just selected by the user or some previous default has been used. The
	 * parameter can be used to override the users preference. For example, this is
	 * important in cases where the default selection is already in use and the user
	 * is forced to choose a different one.
	 *
	 * @param force
	 *            true if the dialog should be opened regardless of the value of the
	 *            show dialog checkbox
	 */
	public void prompt(boolean force) {
		if (force || launchData.getShowDialog()) {
			open();

			// Bug 70576: Dialog gets dismissed via ESC and via the window's
			// close box. Make sure the launch doesn't continue with the default
			// workspace.
			if (getReturnCode() == CANCEL) {
				launchData.workspaceSelected(null);
			}

			return;
		}

		String[] recent = launchData.getRecentWorkspaces();

		// If the selection dialog was not used then the workspace to use is either the
		// most recent selection or the initialDefault (if there is no history).
		String workspace = null;
		if (recent != null && recent.length > 0) {
			workspace = recent[0];
		}
		if (workspace == null || workspace.length() == 0) {
			workspace = launchData.getInitialDefault();
		}
		launchData.workspaceSelected(TextProcessor.deprocess(workspace));
	}

	private List<Image> _images = new ArrayList<>();

	private Image loadImage(String name) {
		try {
			var img = ImageDescriptor.createFromURL(new URL("platform:/plugin/phasereditor.ide/launcher/" + name))
					.createImage();
			_images.add(img);
			return img;
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}
	}

	private void disposeResources() {
		_images.stream().forEach(Image::dispose);
	}

	/**
	 * Creates and returns the contents of the upper part of this dialog (above the
	 * button bar).
	 * <p>
	 * The <code>Dialog</code> implementation of this framework method creates and
	 * returns a new <code>Composite</code> with no margins and spacing.
	 * </p>
	 *
	 * @param parent
	 *            the parent composite to contain the dialog area
	 * @return the dialog area control
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		applyStyle(parent);

		Composite composite = (Composite) super.createDialogArea(parent);
		applyStyle(composite);

		{
			var gl = (GridLayout) composite.getLayout();
			gl.marginWidth = 0;
			gl.marginHeight = 0;
			gl.verticalSpacing = 0;
			composite.addDisposeListener(e -> disposeResources());
		}

		{
			// title
			var label = new Label(composite, 0);
			var img = loadImage("background.png");
			label.setBackgroundImage(img);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.grabExcessVerticalSpace = true;
			gd.minimumHeight = img.getBounds().height;
			label.setLayoutData(gd);

		}

		// Should only create the Recent Workspaces Composite if Recent
		// workspaces exist
		boolean createRecentWorkspacesComposite = false;
		if (launchData.getRecentWorkspaces()[0] != null) {
			createRecentWorkspacesComposite = true;
		}
		createWorkspaceBrowseRow(composite);

		// if (!suppressAskAgain) {
		// createShowDialogButton(composite);
		// }

		if (createRecentWorkspacesComposite) {
			createRecentWorkspacesComposite(composite);
		}

		Dialog.applyDialogFont(composite);
		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		applyStyle(parent);

		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDEWorkbenchMessages.ChooseWorkspaceDialog_launchLabel, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

		for (Control c : parent.getChildren()) {
			applyStyle(c);
		}
	}

	Color bg = new Color(Display.getDefault(), 56, 52, 54);
	Color gray = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
	Color white = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);

	private void applyStyle(Control c) {
		// // c.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		// c.setBackground(bg);
		//
		// if (c instanceof Text) {
		// c.setForeground(gray);
		// } else if (c instanceof Link) {
		// var link = (Link) c;
		// link.setLinkForeground(white);
		// } else {
		// c.setForeground(white);
		// }
	}

	/**
	 * Returns the title that the dialog (or splash) should have.
	 *
	 * @return the window title
	 * @since 3.4
	 */
	public static String getWindowTitle() {
		String productName = null;
		IProduct product = Platform.getProduct();
		if (product != null) {
			productName = product.getName();
		}
		if (productName == null) {
			productName = IDEWorkbenchMessages.ChooseWorkspaceDialog_defaultProductName;
		}
		return productName;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(NLS.bind(IDEWorkbenchMessages.ChooseWorkspaceDialog_dialogName, getWindowTitle()));
		shell.addTraverseListener(e -> {
			// Bug 462707: [WorkbenchLauncher] dialog not closed on ESC.
			// The dialog doesn't always have a parent, so
			// Shell#traverseEscape() doesn't always close it for free.
			if (e.detail == SWT.TRAVERSE_ESCAPE) {
				e.detail = SWT.TRAVERSE_NONE;
				cancelPressed();
			}
		});
	}

	/**
	 * Notifies that the ok button of this dialog has been pressed.
	 * <p>
	 * The <code>Dialog</code> implementation of this framework method sets this
	 * dialog's return code to <code>Window.OK</code> and closes the dialog.
	 * Subclasses may override.
	 * </p>
	 */
	@Override
	protected void okPressed() {
		workspaceSelected(getWorkspaceLocation());
	}

	/**
	 * Set the selected workspace to the given String and close the dialog
	 *
	 * @param workspace
	 */
	private void workspaceSelected(String workspace) {
		launchData.workspaceSelected(TextProcessor.deprocess(workspace));
		super.okPressed();
	}

	/**
	 * Removes the workspace from RecentWorkspaces
	 *
	 * @param workspace
	 */
	private void removeWorkspaceFromLauncher(String workspace) {
		// Remove Workspace from Properties
		List<String> recentWorkpaces = new ArrayList<>(Arrays.asList(launchData.getRecentWorkspaces()));
		recentWorkpaces.remove(workspace);
		launchData.setRecentWorkspaces(recentWorkpaces.toArray(new String[0]));
		launchData.writePersistedData();
		// Remove Workspace Composite
		recentWorkspacesLinks.get(workspace).dispose();
		recentWorkspacesLinks.remove(workspace);
		if (recentWorkspacesLinks.isEmpty()) {
			recentWorkspacesForm.dispose();
		}
		getShell().layout();
		initializeBounds();
		// Remove Workspace from combobox
		if (text.getText().equals(workspace) || text.getText().isEmpty()) {
			text.setText(TextProcessor
					.process((launchData.getRecentWorkspaces().length > 0 ? launchData.getRecentWorkspaces()[0]
							: launchData.getInitialDefault())));
		}
	}

	/**
	 * Get the workspace location from the widget.
	 * 
	 * @return String
	 */
	protected String getWorkspaceLocation() {
		return text.getText();
	}

	@Override
	protected void cancelPressed() {
		launchData.workspaceSelected(null);
		super.cancelPressed();
	}

	/**
	 * The Recent Workspaces area of the dialog is only shown if Recent Workspaces
	 * are defined. It provides a faster way to launch a specific Workspace
	 */
	private void createRecentWorkspacesComposite(final Composite composite) {
		FormToolkit toolkit = new FormToolkit(composite.getDisplay());
		composite.addDisposeListener(c -> toolkit.dispose());
		recentWorkspacesForm = toolkit.createForm(composite);
		recentWorkspacesForm.setBackground(composite.getBackground());
		applyStyle(recentWorkspacesForm);
		applyStyle(recentWorkspacesForm.getBody());

		recentWorkspacesForm.getBody().setLayout(new GridLayout());
		ExpandableComposite recentWorkspacesExpandable = toolkit
				.createExpandableComposite(recentWorkspacesForm.getBody(), ExpandableComposite.NO_TITLE);
		applyStyle(recentWorkspacesExpandable);
		recentWorkspacesForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		recentWorkspacesExpandable.setBackground(composite.getBackground());
		// recentWorkspacesExpandable.setText(IDEWorkbenchMessages.ChooseWorkspaceDialog_recentWorkspaces);
		recentWorkspacesExpandable.setExpanded(launchData.isShowRecentWorkspaces() || true);
		recentWorkspacesExpandable.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				launchData.setShowRecentWorkspaces(((ExpandableComposite) e.getSource()).isExpanded());
				Point size = getInitialSize();
				Shell shell = getShell();
				shell.setBounds(getConstrainedShellBounds(
						new Rectangle(shell.getLocation().x, shell.getLocation().y, size.x, size.y)));
			}
		});

		Composite panel = new Composite(recentWorkspacesExpandable, SWT.NONE);
		applyStyle(panel);
		recentWorkspacesExpandable.setClient(panel);
		RowLayout layout = new RowLayout(SWT.VERTICAL);
		layout.marginLeft = 14;
		layout.spacing = 6;
		panel.setLayout(layout);
		recentWorkspacesLinks = new HashMap<>(launchData.getRecentWorkspaces().length);
		Map<String, String> uniqueWorkspaceNames = createUniqueWorkspaceNameMap();

		List<String> recentWorkspacesList = Arrays.asList(launchData.getRecentWorkspaces()).stream()
				.filter(s -> s != null && !s.isEmpty()).collect(Collectors.toList());
		List<Entry<String, String>> sortedList = uniqueWorkspaceNames.entrySet().stream().sorted((e1, e2) -> Integer
				.compare(recentWorkspacesList.indexOf(e1.getValue()), recentWorkspacesList.indexOf(e2.getValue())))
				.collect(Collectors.toList());

		for (Entry<String, String> uniqueWorkspaceEntry : sortedList) {
			final String recentWorkspace = uniqueWorkspaceEntry.getValue();

			var row = new Composite(panel, 0);
			applyStyle(row);
			row.setLayoutData(new RowData());
			row.setLayout(new RowLayout(SWT.HORIZONTAL));

			var icon = new Label(row, 0);
			icon.setImage(loadImage("bullet-go.png"));

			Link link = new Link(row, SWT.WRAP);

			String name;
			{
				var path = new Path(recentWorkspace);
				var list = path.segments();
				name = list[list.length - 1];
				if (list.length > 1) {
					name = list[list.length - 2] + "/" + name;
				}
			}

			link.setLayoutData(new RowData(SWT.DEFAULT, SWT.DEFAULT));
			link.setText("<a>" + name + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			applyStyle(link);
			link.setToolTipText(recentWorkspace);
			link.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					workspaceSelected(recentWorkspace);
				}
			});

			recentWorkspacesLinks.put(recentWorkspace, link);

			Menu menu = new Menu(link);
			MenuItem forgetItem = new MenuItem(menu, SWT.PUSH);
			forgetItem.setText(IDEWorkbenchMessages.ChooseWorkspaceDialog_removeWorkspaceSelection);
			forgetItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					removeWorkspaceFromLauncher(recentWorkspace);
				}
			});
			link.setMenu(menu);
		}
	}

	/**
	 * Creates a map with unique WorkspaceNames for the RecentWorkspacesComposite.
	 * The values are full absolute paths of recently used workspaces, the keys are
	 * unique segments somehow made from the values.
	 */
	private Map<String, String> createUniqueWorkspaceNameMap() {
		final String fileSeparator = File.separator;
		Map<String, String> uniqueWorkspaceNameMap = new HashMap<>();

		// Convert workspace paths to arrays of single path segments
		List<String[]> splittedWorkspaceNames = Arrays.asList(launchData.getRecentWorkspaces()).stream()
				.filter(s -> s != null && !s.isEmpty()).map(s -> s.split(Pattern.quote(fileSeparator)))
				.collect(Collectors.toList());

		// create and collect unique workspace keys produced from arrays,
		// try to generate unique keys starting with the last segment of the
		// workspace path, increasing number of segments if no unique names
		// could be generated,
		// loop until all array values are removed from array list
		for (int i = 1; !splittedWorkspaceNames.isEmpty(); i++) {
			final int c = i;

			// Function which flattens arrays to (hopefully unique) keys
			Function<String[], String> stringArraytoName = s -> String.join(fileSeparator,
					s.length < c ? s : Arrays.copyOfRange(s, s.length - c, s.length));

			// list of found unique keys
			List<String> uniqueNames = splittedWorkspaceNames.stream().map(stringArraytoName)
					.collect(Collectors.groupingBy(s -> s, Collectors.counting())).entrySet().stream()
					.filter(e -> e.getValue() == 1).map(e -> e.getKey()).collect(Collectors.toList());

			// remove paths for which we have found unique keys
			splittedWorkspaceNames.removeIf(a -> {
				String joined = stringArraytoName.apply(a);
				if (uniqueNames.contains(joined)) {
					uniqueWorkspaceNameMap.put(joined, String.join(fileSeparator, a));
					return true;
				}
				return false;
			});
		}
		return uniqueWorkspaceNameMap;
	}

	/**
	 * The main area of the dialog is just a row with the current selection
	 * information and a drop-down of the most recently used workspaces.
	 */
	private void createWorkspaceBrowseRow(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		applyStyle(panel);

		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		panel.setLayout(layout);
		panel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		panel.setFont(parent.getFont());

		Label label = new Label(panel, SWT.NONE);
		label.setText(IDEWorkbenchMessages.ChooseWorkspaceDialog_workspaceEntryLabel);
		applyStyle(label);

		text = new Text(panel, SWT.BORDER);
		applyStyle(text);
		text.setFocus();
		text.setLayoutData(new GridData(400, SWT.DEFAULT));
		text.addModifyListener(e -> {
			Button okButton = getButton(Window.OK);
			if (okButton != null && !okButton.isDisposed()) {
				boolean nonWhitespaceFound = false;
				String characters = getWorkspaceLocation();
				for (int i = 0; !nonWhitespaceFound && i < characters.length(); i++) {
					if (!Character.isWhitespace(characters.charAt(i))) {
						nonWhitespaceFound = true;
					}
				}
				okButton.setEnabled(nonWhitespaceFound);
			}
		});
		setInitialTextValues(text);
		text.setSelection(text.getText().length());

		Button browseButton = new Button(panel, SWT.PUSH);
		applyStyle(browseButton);
		browseButton.setImage(loadImage("folder-open.png"));
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SHEET);
				dialog.setText(IDEWorkbenchMessages.ChooseWorkspaceDialog_directoryBrowserTitle);
				dialog.setMessage(IDEWorkbenchMessages.ChooseWorkspaceDialog_directoryBrowserMessage);
				dialog.setFilterPath(getInitialBrowsePath());
				String dir = dialog.open();
				if (dir != null) {
					text.setText(TextProcessor.process(dir));
				}
			}
		});
	}

	/**
	 * Return a string containing the path that is closest to the current selection
	 * in the text widget. This starts with the current value and works toward the
	 * root until there is a directory for which File.exists returns true. Return
	 * the current working dir if the text box does not contain a valid path.
	 *
	 * @return closest parent that exists or an empty string
	 */
	private String getInitialBrowsePath() {
		File dir = new File(getWorkspaceLocation());
		while (dir != null && !dir.exists()) {
			dir = dir.getParentFile();
		}

		return dir != null ? dir.getAbsolutePath() : System.getProperty("user.dir"); //$NON-NLS-1$
	}

	/*
	 * see org.eclipse.jface.Window.getInitialLocation()
	 */
	@Override
	protected Point getInitialLocation(Point initialSize) {
		Composite parent = getShell().getParent();

		if (!centerOnMonitor || parent == null) {
			return super.getInitialLocation(initialSize);
		}

		Monitor monitor = parent.getMonitor();
		Rectangle monitorBounds = monitor.getClientArea();
		Point centerPoint = Geometry.centerPoint(monitorBounds);

		return new Point(centerPoint.x - (initialSize.x / 2), Math.max(monitorBounds.y, Math
				.min(centerPoint.y - (initialSize.y * 2 / 3), monitorBounds.y + monitorBounds.height - initialSize.y)));
	}

	/**
	 * The show dialog button allows the user to choose to neven be nagged again.
	 */
	private void createShowDialogButton(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setFont(parent.getFont());

		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		panel.setLayout(layout);

		GridData data = new GridData(GridData.FILL_BOTH);
		data.verticalAlignment = GridData.BEGINNING;
		panel.setLayoutData(data);

		Button button = new Button(panel, SWT.CHECK);
		button.setText(IDEWorkbenchMessages.ChooseWorkspaceDialog_useDefaultMessage);
		button.setSelection(!launchData.getShowDialog());
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				launchData.toggleShowDialog();
			}
		});
	}

	private void setInitialTextValues(Text text) {
		// for (String recentWorkspace : launchData.getRecentWorkspaces()) {
		// if (recentWorkspace != null) {
		// text.add(recentWorkspace);
		// }
		// }

		text.setText(
				TextProcessor.process((launchData.getRecentWorkspaces().length > 0 ? launchData.getRecentWorkspaces()[0]
						: launchData.getInitialDefault())));
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		// If we were explicitly instructed to center on the monitor, then
		// do not provide any settings for retrieving a different location or, worse,
		// saving the centered location.
		if (centerOnMonitor) {
			return null;
		}

		IDialogSettings settings = IDEWorkbenchPlugin.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
		if (section == null) {
			section = settings.addNewSection(DIALOG_SETTINGS_SECTION);
		}
		return section;
	}

	public Text getCombo() {
		return text;
	}
}
