package phasereditor.ui;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.internal.IWorkbenchHelpContextIds;
import org.eclipse.ui.internal.WorkbenchMessages;

/**
 * A standard dialog which solicits a list of selections from the user. This
 * class is configured with an arbitrary data model represented by content and
 * label provider objects. The <code>getResult</code> method returns the
 * selected elements.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * <p>
 * Example:
 * 
 * <pre>
 * ListSelectionDialog dlg = new ListSelectionDialog(getShell(), input, new BaseWorkbenchContentProvider(),
 * 		new WorkbenchLabelProvider(), "Select the resources to save:");
 * dlg.setInitialSelections(dirtyEditors);
 * dlg.setTitle("Save Resources");
 * dlg.open();
 * </pre>
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@SuppressWarnings("all")
public class ListSelectionDialog2 extends SelectionDialog {
	// the root element to populate the viewer with
	private Object inputElement;

	// providers for populating this dialog
	private ILabelProvider labelProvider;

	private IStructuredContentProvider contentProvider;

	// the visual selection widget group
	TableViewer listViewer;

	// sizing constants
	private final static int SIZING_SELECTION_WIDGET_HEIGHT = 250;

	private final static int SIZING_SELECTION_WIDGET_WIDTH = 300;

	/**
	 * Creates a list selection dialog.
	 *
	 * @param parentShell
	 *            the parent shell
	 * @param input
	 *            the root element to populate this dialog with
	 * @param contentProvider
	 *            the content provider for navigating the model
	 * @param labelProvider
	 *            the label provider for displaying model elements
	 * @param message
	 *            the message to be displayed at the top of this dialog, or
	 *            <code>null</code> to display a default message
	 */
	public ListSelectionDialog2(Shell parentShell, Object input, IStructuredContentProvider contentProvider,
			ILabelProvider labelProvider, String message) {
		super(parentShell);
		setTitle(WorkbenchMessages.ListSelection_title);
		inputElement = input;
		this.contentProvider = contentProvider;
		this.labelProvider = labelProvider;
		if (message != null) {
			setMessage(message);
		} else {
			setMessage(WorkbenchMessages.ListSelection_message);
		}
	}

	/**
	 * Add the selection and deselection buttons to the dialog.
	 * 
	 * @param composite
	 *            org.eclipse.swt.widgets.Composite
	 */
	private void addSelectionButtons(Composite composite) {
		Composite buttonComposite = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(new GridData(SWT.END, SWT.TOP, true, false));

		Button selectButton = createButton(buttonComposite, IDialogConstants.SELECT_ALL_ID,
				WorkbenchMessages.SelectionDialog_selectLabel, false);

		SelectionListener listener = widgetSelectedAdapter(e -> listViewer.getTable().selectAll());
		selectButton.addSelectionListener(listener);

		Button deselectButton = createButton(buttonComposite, IDialogConstants.DESELECT_ALL_ID,
				WorkbenchMessages.SelectionDialog_deselectLabel, false);

		listener = widgetSelectedAdapter(e -> listViewer.getTable().selectAll());
		deselectButton.addSelectionListener(listener);
	}

	/**
	 * Visually checks the previously-specified elements in this dialog's list
	 * viewer.
	 */
	private void checkInitialSelections() {
		listViewer.setSelection(new StructuredSelection(getInitialElementSelections()));
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IWorkbenchHelpContextIds.LIST_SELECTION_DIALOG);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		// page group
		Composite composite = (Composite) super.createDialogArea(parent);

		initializeDialogUnits(composite);

		createMessageArea(composite);

		listViewer = new TableViewer(composite, SWT.BORDER | SWT.MULTI);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = SIZING_SELECTION_WIDGET_HEIGHT;
		data.widthHint = SIZING_SELECTION_WIDGET_WIDTH;
		listViewer.getTable().setLayoutData(data);

		listViewer.setLabelProvider(labelProvider);
		listViewer.setContentProvider(contentProvider);

		addSelectionButtons(composite);

		initializeViewer();

		// initialize page
		if (!getInitialElementSelections().isEmpty()) {
			checkInitialSelections();
		}

		Dialog.applyDialogFont(composite);

		return composite;
	}

	/**
	 * Returns the viewer used to show the list.
	 *
	 * @return the viewer, or <code>null</code> if not yet created
	 */
	protected TableViewer getViewer() {
		return listViewer;
	}

	/**
	 * Initializes this dialog's viewer after it has been laid out.
	 */
	private void initializeViewer() {
		listViewer.setInput(inputElement);
	}

	/**
	 * The <code>ListSelectionDialog</code> implementation of this
	 * <code>Dialog</code> method builds a list of the selected elements for later
	 * retrieval by the client and closes this dialog.
	 */
	@Override
	protected void okPressed() {

		// Get the input children.

		setResult(listViewer.getStructuredSelection().toList());

		super.okPressed();
	}
}
