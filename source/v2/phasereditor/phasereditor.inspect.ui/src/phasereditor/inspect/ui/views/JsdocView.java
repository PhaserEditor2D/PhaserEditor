package phasereditor.inspect.ui.views;

import static java.lang.System.out;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import phasereditor.inspect.core.jsdoc.IJsdocProvider;
import phasereditor.inspect.core.jsdoc.JsdocRenderer;

public class JsdocView extends ViewPart implements ISelectionListener, LocationListener {

	public static final String ID = "phasereditor.inspect.ui.jsdoc";
	private Browser _browser;
	private IJsdocProvider _currentProvider;

	public JsdocView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		_browser = new Browser(parent, SWT.NONE);
		_browser.addLocationListener(this);
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
		IJsdocProvider provider = null;

		if (obj != null) {
			provider = Adapters.adapt(obj, IJsdocProvider.class);
		}

		showFromProvider(provider);
	}

	private void showFromProvider(IJsdocProvider provider) {
		String html;
		if (provider == null) {
			html = "No available documentation.";
		} else {
			html = provider.getJsdoc();
		}
		html = JsdocRenderer.wrapDocBody(html);
		setHtml(html);

		_currentProvider = provider;
	}

	private void setHtml(String html) {
		_browser.setText(html);
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		Object obj = null;
		if (selection instanceof IStructuredSelection) {
			obj = ((IStructuredSelection) selection).getFirstElement();
		}
		showJsdocFor(obj);
	}

	@Override
	public void changing(LocationEvent event) {
		out.println("Process link: " + event.location);
		
		if (event.location.equals("about:blank")) {
			return;
		}

		if (_currentProvider != null) {
			IJsdocProvider newProvider = _currentProvider.processLink(event.location);
			if (newProvider == null) {
				event.doit = false;
			} else {
				showFromProvider(newProvider);
			}
		}
	}

	@Override
	public void changed(LocationEvent event) {
		//
	}

}
