package phasereditor.webrun.ui.editors;

import java.util.ArrayList;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.EditorActionBarContributor;

import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

@SuppressWarnings("boxing")
public class GamePlayerEditorContributor extends EditorActionBarContributor {
	private static class ViewerLabelProvider extends LabelProvider {
		public ViewerLabelProvider() {
		}

		@Override
		public String getText(Object element) {
			Object[] device = (Object[]) element;
			int w = (int) device[1];
			if (w == 0) {
				// responsive
				return (String) device[0];
			}
			return device[0] + " - " + w + "x" + device[2];
		}
	}

	static class DeviceList extends ArrayList<Object[]> {
		private static final long serialVersionUID = 1L;

		public void addDevice(String name, int w, int h) {
			add(new Object[] { name, w, h });
		}
	}

	public static final DeviceList DEVICES = new DeviceList();

	static {
		DEVICES.addDevice("Responsive", 0, 0);
		DEVICES.addDevice("BlackBerry Z30", 360, 640);
		DEVICES.addDevice("Glaxy Note II", 360, 640);
		DEVICES.addDevice("Galaxy S III", 360, 640);
		DEVICES.addDevice("Kindle Fire HDX", 1600, 2560);
		DEVICES.addDevice("LG Optimus L70", 384, 640);
		DEVICES.addDevice("Nexus 6", 412, 732);
		DEVICES.addDevice("Galazy S5", 360, 640);
		DEVICES.addDevice("Nexus 5X", 412, 732);
		DEVICES.addDevice("Nexus 6P", 412, 732);
		DEVICES.addDevice("iPhone 5", 320, 568);
		DEVICES.addDevice("iPhone 6", 375, 667);
		DEVICES.addDevice("iPhone 6 Plus", 414, 736);
		DEVICES.addDevice("iPad", 758, 1024);
		DEVICES.addDevice("iPad Pro", 1024, 1366);
	}

	private GamePlayerEditor _editor;

	public GamePlayerEditorContributor() {
	}

	@Override
	public void contributeToToolBar(IToolBarManager toolbar) {
		toolbar.add(new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				ToolItem item = new ToolItem(parent, SWT.DROP_DOWN);
				item.setImage(EditorSharedImages.getImage(IEditorSharedImages.IMG_MONITOR_EDIT));
				DropdownSelectionListener listenerOne = new DropdownSelectionListener(item);
				item.addSelectionListener(listenerOne);
			}

		});

		toolbar.add(new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				ToolItem item = new ToolItem(parent, SWT.PUSH);
				item.setImage(EditorSharedImages.getImage(IEditorSharedImages.IMG_ROTATE));
				item.addSelectionListener(new SelectionListener() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						getEditor().rotate();
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						//
					}
				});
			}

		});

	}

	public GamePlayerEditor getEditor() {
		return _editor;
	}

	class DropdownSelectionListener extends SelectionAdapter {
		private Menu menu;

		public DropdownSelectionListener(ToolItem dropdown) {
			menu = new Menu(dropdown.getParent().getShell());
			menu.addMenuListener(new MenuListener() {

				@SuppressWarnings("synthetic-access")
				@Override
				public void menuShown(MenuEvent e) {
					GamePlayerEditor editor = getEditor();
					Object[] curDevice = editor.getEditorInput().getDevice();

					MenuItem[] items = menu.getItems();

					for (int i = 0; i < items.length; i++) {
						MenuItem item = items[i];

						boolean selected = false;

						Object[] device = (Object[]) item.getData();

						if (curDevice != null && device[0].equals(curDevice[0])) {
							selected = true;
						} else if (i == 0 && (curDevice == null || (int) curDevice[1] == 0)) {
							selected = true;
						}

						item.setSelection(selected);
					}
				}

				@Override
				public void menuHidden(MenuEvent e) {
					// nothing
				}
			});

			ViewerLabelProvider label = new ViewerLabelProvider();

			for (Object[] device : DEVICES) {
				MenuItem menuItem = new MenuItem(menu, SWT.CHECK);
				menuItem.setText(label.getText(device));
				menuItem.setData(device);
				menuItem.addSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(SelectionEvent event) {
						getEditor().resizeToDevice(device);
					}
				});
			}

		}

		@Override
		public void widgetSelected(SelectionEvent event) {
			ToolItem item = (ToolItem) event.widget;
			Rectangle rect = item.getBounds();
			Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
			menu.setLocation(pt.x, pt.y + rect.height);
			menu.setVisible(true);
		}
	}

	@Override
	public void setActiveEditor(IEditorPart targetEditor) {
		_editor = (GamePlayerEditor) targetEditor;
	}
}
