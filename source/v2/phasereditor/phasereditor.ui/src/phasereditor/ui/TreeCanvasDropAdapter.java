package phasereditor.ui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;

/**
 * This adapter class provides generic drag-and-drop support for a viewer.
 * <p>
 * Subclasses must implement the following methods:
 * <ul>
 * <li><code>validateDrop</code> - identifies valid drop targets in viewer</li>
 * <li><code>performDrop</code> - carries out a drop into a viewer</li>
 * </ul>
 * The <code>setFeedbackEnabled</code> method can be called to turn on and off
 * visual insertion feedback (on by default).
 * </p>
 */
public abstract class TreeCanvasDropAdapter extends DropTargetAdapter {

	/**
	 * Constant describing the position of the cursor relative to the target object.
	 * This means the mouse is positioned slightly before the target.
	 * 
	 * @see #getCurrentLocation()
	 */
	public static final int LOCATION_BEFORE = 1;

	/**
	 * Constant describing the position of the cursor relative to the target object.
	 * This means the mouse is positioned slightly after the target.
	 * 
	 * @see #getCurrentLocation()
	 */
	public static final int LOCATION_AFTER = 2;

	/**
	 * Constant describing the position of the cursor relative to the target object.
	 * This means the mouse is positioned directly on the target.
	 * 
	 * @see #getCurrentLocation()
	 */
	public static final int LOCATION_ON = 3;

	/**
	 * Constant describing the position of the cursor relative to the target object.
	 * This means the mouse is not positioned over or near any valid target.
	 * 
	 * @see #getCurrentLocation()
	 */
	public static final int LOCATION_NONE = 4;

	/**
	 * The viewer to which this drop support has been added.
	 */
	private TreeCanvas _canvas;

	/**
	 * The current operation.
	 */
	private int currentOperation = DND.DROP_NONE;

	/**
	 * The last valid operation. We need to remember the last good operation in the
	 * case where the current operation temporarily is not valid (drag over
	 * someplace you can't drop).
	 */
	private int lastValidOperation;

	/**
	 * This is used because we allow the operation to be temporarily overridden (for
	 * example a move to a copy) for a drop that happens immediately after the
	 * operation is overridden.
	 */
	private int overrideOperation = -1;

	/**
	 * The current DropTargetEvent, used only during validateDrop()
	 */
	private DropTargetEvent currentEvent;

	/**
	 * The data item currently under the mouse.
	 */
	private Object currentTarget;

	/**
	 * Information about the position of the mouse relative to the target (before,
	 * on, or after the target. Location is one of the <code>LOCATION_* </code>
	 * constants defined in this type.
	 */
	private int currentLocation;

	/**
	 * A flag that allows adapter users to turn the insertion feedback on or off.
	 * Default is <code>true</code>.
	 */
	private boolean feedbackEnabled = true;

	/**
	 * A flag that allows adapter users to turn auto scrolling on or off. Default is
	 * <code>true</code>.
	 */
	private boolean scrollEnabled = true;

	/**
	 * A flag that allows adapter users to turn auto expanding on or off. Default is
	 * <code>true</code>.
	 */
	private boolean expandEnabled = true;

	/**
	 * A flag that allows adapter users to turn selection feedback on or off.
	 * Default is <code>true</code>.
	 */
	private boolean selectFeedbackEnabled = true;

	protected TreeCanvasDropAdapter(TreeCanvasViewer viewer) {
		this(viewer.getCanvas());
	}

	/**
	 * Creates a new drop adapter for the given viewer.
	 *
	 * @param canvas
	 *            the viewer
	 */
	protected TreeCanvasDropAdapter(TreeCanvas canvas) {
		this._canvas = canvas;
	}

	/**
	 * Returns the position of the given event's coordinates relative to its target.
	 * The position is determined to be before, after, or on the item, based on some
	 * threshold value.
	 *
	 * @param event
	 *            the event
	 * @return one of the <code>LOCATION_* </code>constants defined in this class
	 */
	protected int determineLocation(DropTargetEvent event) {

		var utils = _canvas.getUtils();

		int index = utils.getDropIndex();

		if (index == -1) {
			return LOCATION_NONE;
		}

		var viewPoint = _canvas.toControl(event.x, event.y);
		var modelPoint = utils.viewToModel(viewPoint.x, viewPoint.y);

		var bounds = utils.getSelectionFrameArea(index);

		int location = LOCATION_ON;

		if ((modelPoint.y - bounds.y) < 5) {
			location = LOCATION_BEFORE;
		} else if ((bounds.y + bounds.height - modelPoint.y) < 5) {
			location = LOCATION_AFTER;
		}
		
		_canvas.redraw();

		return location;
	}

	/**
	 * Returns the target item of the given drop event.
	 *
	 * @param event
	 *            the event
	 * @return The target of the drop, may be <code>null</code>.
	 */
	protected Object determineTarget(DropTargetEvent event) {

		_canvas.getUtils().updateDropIndex(event);

		return _canvas.getUtils().getDropObject();
	}

	private void doDropValidation(DropTargetEvent event) {
		// always remember what was previously requested, but not if it
		// was overridden
		if (event.detail != DND.DROP_NONE && overrideOperation == -1)
			lastValidOperation = event.detail;

		currentOperation = lastValidOperation;
		currentEvent = event;
		overrideOperation = -1;
		if (!validateDrop(currentTarget, currentOperation, event.currentDataType)) {
			currentOperation = DND.DROP_NONE;
		}

		// give the right feedback for the override
		if (overrideOperation != -1)
			event.detail = overrideOperation;
		else
			event.detail = currentOperation;
		currentEvent = null;
	}

	@Override
	public void dragEnter(DropTargetEvent event) {
		currentTarget = determineTarget(event);
		doDropValidation(event);
	}

	@Override
	public void dragOperationChanged(DropTargetEvent event) {
		currentTarget = determineTarget(event);
		doDropValidation(event);
	}

	@Override
	public void dragOver(DropTargetEvent event) {
		// use newly revealed item as target if scrolling occurs
		Object target = determineTarget(event);

		// set the location feedback
		int oldLocation = currentLocation;
		currentLocation = determineLocation(event);
		setFeedback(event, currentLocation);

		// see if anything has really changed before doing validation.
		if (target != currentTarget || currentLocation != oldLocation) {
			currentTarget = target;
			doDropValidation(event);
		}
	}

	@Override
	public void drop(DropTargetEvent event) {
		currentLocation = determineLocation(event);
		currentEvent = event;

		if (overrideOperation != -1)
			currentOperation = overrideOperation;

		// perform the drop behavior
		if (!performDrop(event.data)) {
			event.detail = DND.DROP_NONE;
		}

		// reset for next time
		currentOperation = DND.DROP_NONE;
		currentEvent = null;

		_canvas.getUtils().dropDone();
	}

	@Override
	public void dropAccept(DropTargetEvent event) {
		currentEvent = event;

		if (!validateDrop(currentTarget, event.detail, event.currentDataType)) {
			currentOperation = event.detail = DND.DROP_NONE;
		}

		currentEvent = null;
	}

	/**
	 * Returns a constant describing the position of the mouse relative to the
	 * target (before, on, or after the target.
	 *
	 * @return one of the <code>LOCATION_* </code> constants defined in this type
	 */
	protected int getCurrentLocation() {
		return currentLocation;
	}

	/**
	 * Returns the current operation.
	 *
	 * @return a <code>DROP_*</code> constant from class <code>DND</code>
	 *
	 * @see DND#DROP_COPY
	 * @see DND#DROP_MOVE
	 * @see DND#DROP_LINK
	 * @see DND#DROP_NONE
	 */
	protected int getCurrentOperation() {
		return currentOperation;
	}

	/**
	 * Returns the target object currently under the mouse.
	 *
	 * @return the current target object
	 */
	protected Object getCurrentTarget() {
		return currentTarget;
	}

	/**
	 * Returns the current {@link DropTargetEvent}.
	 *
	 * This may be called only inside of the
	 * {@link #validateDrop(Object, int, TransferData)} or
	 * {@link #performDrop(Object)} methods.
	 * 
	 * @return the DropTargetEvent
	 * @since 3.5
	 */
	protected DropTargetEvent getCurrentEvent() {
		Assert.isTrue(currentEvent != null);
		return currentEvent;
	}

	/**
	 * Returns whether visible insertion feedback should be presented to the user.
	 * <p>
	 * Typical insertion feedback is the horizontal insertion bars that appear
	 * between adjacent items while dragging.
	 * </p>
	 *
	 * @return <code>true</code> if visual feedback is desired, and
	 *         <code>false</code> if not
	 */
	public boolean getFeedbackEnabled() {
		return feedbackEnabled;
	}

	/**
	 * Returns the object currently selected by the viewer.
	 *
	 * @return the selected object or the first element in current selection, and
	 *         null if no objects are selected
	 */
	protected Object getSelectedObject() {
		ISelection selection = _canvas.getUtils().getSelection();
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			IStructuredSelection structured = (IStructuredSelection) selection;
			return structured.getFirstElement();
		}
		return null;
	}

	/**
	 * @return the viewer to which this drop support has been added.
	 */
	protected TreeCanvas getViewer() {
		return _canvas;
	}

	/**
	 * @deprecated this method should not be used. Exception handling has been
	 *             removed from DropTargetAdapter methods overridden by this class.
	 *             Handles any exception that occurs during callback, including
	 *             rethrowing behavior.
	 *             <p>
	 *             [Issue: Implementation prints stack trace and eats exception to
	 *             avoid crashing VA/J. Consider conditionalizing the implementation
	 *             to do one thing in VAJ and something more reasonable in other
	 *             operating environments. ]
	 *             </p>
	 *
	 * @param exception
	 *            the exception
	 * @param event
	 *            the event
	 */
	@SuppressWarnings("static-method")
	@Deprecated
	protected void handleException(Throwable exception, DropTargetEvent event) {
		// Currently we never rethrow because VA/Java crashes if an SWT
		// callback throws anything. Generally catching Throwable is bad, but in
		// this cases it's better than hanging the image.
		exception.printStackTrace();
		event.detail = DND.DROP_NONE;
	}

	/**
	 * Performs any work associated with the drop.
	 * <p>
	 * Subclasses must implement this method to provide drop behavior.
	 * </p>
	 *
	 * @param data
	 *            the drop data
	 * @return <code>true</code> if the drop was successful, and <code>false</code>
	 *         otherwise
	 */
	public abstract boolean performDrop(Object data);

	/**
	 * Overrides the current operation for a drop that happens immediately after the
	 * current validateDrop.
	 *
	 * This maybe called only from within a
	 * {@link #validateDrop(Object, int, TransferData)} method
	 *
	 *
	 * @param operation
	 *            the operation to be used for the drop.
	 *
	 * @see DND#DROP_COPY
	 * @see DND#DROP_MOVE
	 * @see DND#DROP_LINK
	 * @see DND#DROP_NONE
	 *
	 * @since 3.5
	 */
	protected void overrideOperation(int operation) {
		overrideOperation = operation;
	}

	private void setFeedback(DropTargetEvent event, int location) {

		if (feedbackEnabled) {
			switch (location) {
			case LOCATION_BEFORE:
				event.feedback = DND.FEEDBACK_INSERT_BEFORE;
				break;
			case LOCATION_AFTER:
				event.feedback = DND.FEEDBACK_INSERT_AFTER;
				break;
			case LOCATION_ON:
			default:
				event.feedback = DND.FEEDBACK_SELECT;
				break;
			}
		}

		// Explicitly inhibit SELECT feedback if desired
		if (!selectFeedbackEnabled) {
			event.feedback &= ~DND.FEEDBACK_SELECT;
		}

		if (expandEnabled) {
			event.feedback |= DND.FEEDBACK_EXPAND;
		}
		if (scrollEnabled) {
			event.feedback |= DND.FEEDBACK_SCROLL;
		}
	}

	/**
	 * Sets whether visible insertion feedback should be presented to the user.
	 * <p>
	 * Typical insertion feedback is the horizontal insertion bars that appear
	 * between adjacent items while dragging.
	 * </p>
	 *
	 * @param value
	 *            <code>true</code> if visual feedback is desired, and
	 *            <code>false</code> if not
	 */
	public void setFeedbackEnabled(boolean value) {
		feedbackEnabled = value;
	}

	/**
	 * Sets whether selection feedback should be provided during dragging.
	 *
	 * @param value
	 *            <code>true</code> if selection feedback is desired, and
	 *            <code>false</code> if not
	 *
	 * @since 3.2
	 */
	public void setSelectionFeedbackEnabled(boolean value) {
		selectFeedbackEnabled = value;
	}

	/**
	 * Sets whether auto scrolling and expanding should be provided during dragging.
	 *
	 * @param value
	 *            <code>true</code> if scrolling and expanding is desired, and
	 *            <code>false</code> if not
	 * @since 2.0
	 */
	public void setScrollExpandEnabled(boolean value) {
		expandEnabled = value;
		scrollEnabled = value;
	}

	/**
	 * Sets whether auto expanding should be provided during dragging.
	 *
	 * @param value
	 *            <code>true</code> if expanding is desired, and <code>false</code>
	 *            if not
	 * @since 3.4
	 */
	public void setExpandEnabled(boolean value) {
		expandEnabled = value;
	}

	/**
	 * Sets whether auto scrolling should be provided during dragging.
	 *
	 * @param value
	 *            <code>true</code> if scrolling is desired, and <code>false</code>
	 *            if not
	 * @since 3.4
	 */
	public void setScrollEnabled(boolean value) {
		scrollEnabled = value;
	}

	/**
	 * Validates dropping on the given object. This method is called whenever some
	 * aspect of the drop operation changes.
	 * <p>
	 * Subclasses must implement this method to define which drops make sense.
	 * </p>
	 *
	 * @param target
	 *            the object that the mouse is currently hovering over, or
	 *            <code>null</code> if the mouse is hovering over empty space
	 * @param operation
	 *            the current drag operation (copy, move, etc.)
	 * @param transferType
	 *            the current transfer type
	 * @return <code>true</code> if the drop is valid, and <code>false</code>
	 *         otherwise
	 */
	public abstract boolean validateDrop(Object target, int operation, TransferData transferType);
}
