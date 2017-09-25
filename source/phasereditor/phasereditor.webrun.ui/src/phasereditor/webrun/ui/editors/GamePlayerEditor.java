package phasereditor.webrun.ui.editors;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import phasereditor.ui.PhaserEditorUI;
import phasereditor.webrun.ui.GamePlayerEditorInput;

@SuppressWarnings({ "boxing", "synthetic-access" })
public class GamePlayerEditor extends EditorPart {

	public static final String ID = "phasereditor.webrun.ui.gameplayer";

	static class DeviceList extends ArrayList<Object[]> {
		private static final long serialVersionUID = 1L;

		public void addDevice(String name, int w, int h) {
			add(new Object[] { name, w, h });
		}
	}

	private Browser _browser;
	private Composite _parentComposite;

	public GamePlayerEditor() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		//
	}

	@Override
	public void doSaveAs() {
		//
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public GamePlayerEditorInput getEditorInput() {
		return (GamePlayerEditorInput) super.getEditorInput();
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout gl_parentComposite = new GridLayout(1, false);
		gl_parentComposite.marginHeight = 0;
		gl_parentComposite.horizontalSpacing = 0;
		gl_parentComposite.marginWidth = 0;
		gl_parentComposite.verticalSpacing = 0;
		parent.setLayout(gl_parentComposite);

		_browser = new Browser(parent, SWT.NONE);
		_browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_parentComposite = parent;

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {

		PhaserEditorUI.openedInternalBrowser();
		_browser.addDisposeListener(e -> PhaserEditorUI.closedInternalBrowser());

		Display.getDefault().asyncExec(() -> {
			_browser.setUrl(getEditorInput().getUrl());
			GamePlayerEditorInput input = getEditorInput();
			Object[] device = input.getDevice();
			resizeToDevice(device);
		});

		_parentComposite.addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent e) {
				GC gc = e.gc;

				GamePlayerEditorInput input = getEditorInput();
				Object[] device = input.getDevice();

				if (device == null) {
					return;
				}

				Rectangle b = _parentComposite.getBounds();
				b.x = 0;
				b.y = 0;

				gc.setAntialias(SWT.ON);
				
				Color bg = gc.getBackground();
				
				gc.setBackground(e.display.getSystemColor(SWT.COLOR_WHITE));
				Point start = _browser.getLocation();
				Point size = _browser.getSize();

				gc.setForeground(e.display.getSystemColor(SWT.COLOR_DARK_GRAY));
				gc.setLineWidth(2);

				if (input.isRotated()) {
					gc.fillRoundRectangle(start.x - 60, start.y - 15, size.x + 120, size.y + 30, 10, 10);
					gc.drawRoundRectangle(start.x - 60, start.y - 15, size.x + 120, size.y + 30, 10, 10);
				} else {
					gc.fillRoundRectangle(start.x - 15, start.y - 60, size.x + 30, size.y + 120, 10, 10);
					gc.drawRoundRectangle(start.x - 15, start.y - 60, size.x + 30, size.y + 120, 10, 10);
				}

				String str = "This is an experimental feature";
				size = gc.textExtent(str);
				gc.setBackground(bg);
				gc.setForeground(e.display.getSystemColor(SWT.COLOR_RED));
				gc.drawText(str, e.width - size.x - 2, e.height - size.y - 2);
			}
		});
	}

	public void resizeToDevice(Object[] device) {
		GamePlayerEditorInput input = getEditorInput();

		input.setDevice(device);

		boolean rotated = input.isRotated();

		if (device == null) {
			_parentComposite.layout();
			_parentComposite.redraw();
			return;
		}

		int w = (int) device[1];
		int h = (int) device[2];

		GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1);

		if (w == 0) {
			// responsive
			gd.horizontalAlignment = SWT.FILL;
			gd.verticalAlignment = SWT.FILL;
		} else {

			if (rotated) {
				int t = w;
				w = h;
				h = t;
			}

			gd.widthHint = w;
			gd.heightHint = h;
			gd.minimumWidth = w;
			gd.minimumHeight = h;
		}

		_browser.setLayoutData(gd);
		_parentComposite.layout();
		_parentComposite.redraw();
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		setPartName(getEditorInput().getProjectName() + " (experimental)");
	}

	@Override
	public void setFocus() {
		_browser.setFocus();
	}

	public void rotate() {
		GamePlayerEditorInput input = getEditorInput();
		input.setRotated(!input.isRotated());
		resizeToDevice(input.getDevice());
	}

}
