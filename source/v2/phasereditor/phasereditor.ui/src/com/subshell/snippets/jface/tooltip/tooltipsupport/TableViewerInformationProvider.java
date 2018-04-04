package com.subshell.snippets.jface.tooltip.tooltipsupport;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TableItem;


/**
 * An {@link IInformationProvider} that provides information about
 * the {@link TableItem}s of a {@link TableViewer}.
 */
public class TableViewerInformationProvider implements IInformationProvider {

	private final TableViewer viewer;

	/**
	 * Creates a new information provider for the given table viewer.
	 * @param viewer the table viewer
	 */
	public TableViewerInformationProvider(TableViewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public Object getInformation(Point location) {
		Item item = viewer.getTable().getItem(location);
		if (item != null) {
			Object data = item.getData();
			return data;
		}
		return null;
	}

	@Override
	public Rectangle getArea(Point location) {
		TableItem item = viewer.getTable().getItem(location);
		if (item != null) {
			return item.getBounds();
		}
		return null;
	}

}
