package phasereditor.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * A dialog that prompts for one element out of a list of elements. Uses
 * <code>IStructuredContentProvider</code> to provide the elements and
 * <code>ILabelProvider</code> to provide their labels.
 *
 * @since 2.1
 */
public class ListCanvasDialog extends SelectionDialog {
	private ITreeContentProvider fContentProvider;

	private LabelProvider fLabelProvider;

	private Object fInput;

	private TreeCanvasViewer fTableViewer;

	private boolean fAddCancelButton = true;

	private int widthInChars = 55;

	private int heightInChars = 15;

	private FilteredTreeCanvas _tree;

	/**
	 * Create a new instance of the receiver with parent shell of parent.
	 * 
	 * @param parent
	 */
	public ListCanvasDialog(Shell parent) {
		super(parent);
	}

	/**
	 * @param input
	 *            The input for the list.
	 */
	public void setInput(Object input) {
		fInput = input;
	}

	/**
	 * @param sp
	 *            The content provider for the list.
	 */
	public void setContentProvider(ITreeContentProvider sp) {
		fContentProvider = sp;
	}

	/**
	 * @param lp
	 *            The labelProvider for the list.
	 */
	public void setLabelProvider(LabelProvider lp) {
		fLabelProvider = lp;
	}

	/**
	 * @param addCancelButton
	 *            if <code>true</code> there will be a cancel button.
	 */
	public void setAddCancelButton(boolean addCancelButton) {
		fAddCancelButton = addCancelButton;
	}

	/**
	 * @return the TableViewer for the receiver.
	 */
	public TreeCanvasViewer getTableViewer() {
		return fTableViewer;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		if (!fAddCancelButton) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		} else {
			super.createButtonsForButtonBar(parent);
		}
	}

	@Override
	protected Control createDialogArea(Composite container) {
		Composite parent = (Composite) super.createDialogArea(container);
		createMessageArea(parent);
		_tree = new FilteredTreeCanvas(parent, getTableStyle());
		fTableViewer = new TreeCanvasViewer(_tree.getTree());
		fTableViewer.setContentProvider(fContentProvider);
		fTableViewer.setLabelProvider(fLabelProvider);
		fTableViewer.setInput(fInput);
		fTableViewer.getCanvas().addMouseListener(new MouseAdapter() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if (fAddCancelButton) {
					okPressed();
				}
			}
		});
		var initialSelection = getInitialElementSelections();
		if (initialSelection != null) {
			fTableViewer.setSelection(new StructuredSelection(initialSelection));
		}
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = convertHeightInCharsToPixels(heightInChars);
		gd.widthHint = convertWidthInCharsToPixels(widthInChars);
		_tree.setLayoutData(gd);
		_tree.setFont(container.getFont());
		return parent;
	}

	/**
	 * Return the style flags for the table viewer.
	 * 
	 * @return int
	 */
	@SuppressWarnings("static-method")
	protected int getTableStyle() {
		return SWT.BORDER;
	}

	/*
	 * Overrides method from Dialog
	 */
	@Override
	protected void okPressed() {
		// Build a list of selected children.
		IStructuredSelection selection = fTableViewer.getStructuredSelection();
		setResult(selection.toList());
		super.okPressed();
	}

	/**
	 * Returns the initial height of the dialog in number of characters.
	 *
	 * @return the initial height of the dialog in number of characters
	 */
	public int getHeightInChars() {
		return heightInChars;
	}

	/**
	 * Returns the initial width of the dialog in number of characters.
	 *
	 * @return the initial width of the dialog in number of characters
	 */
	public int getWidthInChars() {
		return widthInChars;
	}

	/**
	 * Sets the initial height of the dialog in number of characters.
	 *
	 * @param heightInChars
	 *            the initialheight of the dialog in number of characters
	 */
	public void setHeightInChars(int heightInChars) {
		this.heightInChars = heightInChars;
	}

	/**
	 * Sets the initial width of the dialog in number of characters.
	 *
	 * @param widthInChars
	 *            the initial width of the dialog in number of characters
	 */
	public void setWidthInChars(int widthInChars) {
		this.widthInChars = widthInChars;
	}
}
