package phasereditor.ui.views;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.chromium.Browser;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class ChromiumBrowserView extends ViewPart {

	public static final String ID = "phasereditor.ui.chromiumBrowser";
	private Browser _browser;

	// private Browser _browser;

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		var btn = new Button(parent, SWT.BORDER);
		btn.setText("Open...");
		btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			var dlg = new InputDialog(parent.getShell(), "URL", "enter the URL:", "", new IInputValidator() {

				@Override
				public String isValid(String newText) {
					return null;
				}
			});
			if (dlg.open() == Window.OK) {
				_browser.setUrl(dlg.getValue());
			}
		}));

		btn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		_browser = new Browser(parent, SWT.BORDER);
		_browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	@Override
	public void setFocus() {
		_browser.setFocus();
	}
}
