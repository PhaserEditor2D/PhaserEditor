package phasereditor.inspect.ui.views;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tm4e.ui.TMUIPlugin;
import org.eclipse.tm4e.ui.themes.ITheme;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import phasereditor.inspect.core.jsdoc.IJsdocProvider;
import phasereditor.inspect.core.jsdoc.JsdocRenderer;
import phasereditor.ui.BackNextActions;
import phasereditor.ui.SelectionProviderImpl;

public class JsdocView extends ViewPart implements ISelectionListener, LocationListener, IPreferenceChangeListener {

	public static final String ID = "phasereditor.inspect.ui.jsdoc";
	private Browser _browser;
	private IJsdocProvider _currentProvider;
	private Object _content;
	private boolean _settingContent;
	private BackNextActions _backNextAction;

	public JsdocView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		_browser = new Browser(parent, SWT.NONE);
		_browser.addLocationListener(this);

		InstanceScope.INSTANCE.getNode(TMUIPlugin.PLUGIN_ID).addPreferenceChangeListener(this);
		InstanceScope.INSTANCE.getNode("org.eclipse.e4.ui.css.swt.theme").addPreferenceChangeListener(this);

		setHtml("Select an element in the workbbench.");

		getViewSite().setSelectionProvider(new SelectionProviderImpl(true));

		initActionBars();
	}

	private void initActionBars() {
		_backNextAction = new BackNextActions() {

			@Override
			protected void display(Object obj) {
				navigateShowJsdocFor(obj);
			}
		};

		var manager = getViewSite().getActionBars().getToolBarManager();

		manager.add(_backNextAction.getBackAction());
		manager.add(_backNextAction.getNextAction());

		manager.add(new Separator());
	}

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);

		site.getWorkbenchWindow().getSelectionService().addSelectionListener(this);
	}

	@Override
	public void dispose() {

		getViewSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);

		InstanceScope.INSTANCE.getNode(TMUIPlugin.PLUGIN_ID).removePreferenceChangeListener(this);
		InstanceScope.INSTANCE.getNode("org.eclipse.e4.ui.css.swt.theme").removePreferenceChangeListener(this);

		super.dispose();
	}

	@Override
	public void setFocus() {
		_browser.setFocus();
	}

	public void showJsdocFor(Object obj) {
		if (obj != null) {

			if (obj == _content) {
				return;
			}

			_backNextAction.showNewObject(obj);
		}
	}

	protected void navigateShowJsdocFor(Object obj) {
		if (_settingContent) {
			return;
		}

		_settingContent = true;

		_content = obj;

		IJsdocProvider provider = null;

		if (obj == null) {
			getViewSite().getSelectionProvider().setSelection(StructuredSelection.EMPTY);
		} else {
			provider = Adapters.adapt(obj, IJsdocProvider.class);
			getViewSite().getSelectionProvider().setSelection(new StructuredSelection(obj));
		}

		showFromProvider(provider);

		_settingContent = false;
	}

	public Object getContent() {
		return _content;
	}

	private void showFromProvider(IJsdocProvider provider) {
		if (provider != null) {
			_currentProvider = provider;
			setHtml(provider.getJsdoc());
		}
	}

	private void setHtml(String html) {
		ITheme theme = TMUIPlugin.getThemeManager().getThemeForScope("source.js");

		// Color bg = theme.getEditorBackground();
		var bg = _browser.getParent().getBackground();
		var fg = theme.getEditorForeground();

		var html2 = JsdocRenderer.wrapDocBody(html, bg.getRGB(), fg.getRGB());

		_browser.setText(html2);
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
		if (event.location.equals("about:blank")) {
			return;
		}

		if (_currentProvider != null) {
			var newContent = _currentProvider.processLink(event.location);
			if (newContent == null) {
				event.doit = false;
			} else {
				showJsdocFor(newContent);
			}
		}
	}

	@Override
	public void changed(LocationEvent event) {
		//
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		showFromProvider(_currentProvider);
	}

}
