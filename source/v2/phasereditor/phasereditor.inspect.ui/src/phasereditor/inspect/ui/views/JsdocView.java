package phasereditor.inspect.ui.views;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.inspect.core.jsdoc.IPhaserMember;
import phasereditor.inspect.core.jsdoc.JSDocRenderer;

public class JsdocView extends ViewPart implements ISelectionListener {

	private Browser _browser;

	public JsdocView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		_browser = new Browser(parent, SWT.NONE);
	}

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);

		site.getWorkbenchWindow().getSelectionService().addSelectionListener(this);
	}

	@Override
	public void dispose() {

		getViewSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);

		super.dispose();
	}

	@Override
	public void setFocus() {
		_browser.setFocus();
	}

	public void showJsdocFor(Object obj) {
		IPhaserMember member;

		if (obj == null) {
			member = null;
		} else {
			member = Adapters.adapt(obj, IPhaserMember.class);
		}

		String html;
		if (member == null) {
			html = "No available documentation.";
		} else {
			JSDocRenderer renderer = JSDocRenderer.getInstance();
			html = renderer.render(member);
		}
		_browser.setText(wrapBody(html));
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		Object obj = null;
		if (selection instanceof IStructuredSelection) {
			obj = ((IStructuredSelection) selection).getFirstElement();
		}
		showJsdocFor(obj);
	}

	private static String wrapBody(String doc) {
		RGB rgb = SWTResourceManager.getColor(SWT.COLOR_INFO_BACKGROUND).getRGB();
		String color = "rgb(" + rgb.red + ", " + rgb.green + ", " + rgb.blue + ")";

		String html = "<html><body style='background:\"" + color + "\";'>";
		html += doc;
		html += "</body></html>";
		return html;
	}

}
