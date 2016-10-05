package phasereditor.canvas.ui;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackCore.IPacksChangeListener;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.behaviors.UpdateBehavior;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "phasereditor.canvas.ui"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
	 * BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		AssetPackCore.addPacksChangedListener(new IPacksChangeListener() {

			@Override
			public void packsChanged(PackDelta delta) {
				try {
					Display.getDefault().asyncExec(new Runnable() {

						@Override
						public void run() {
							IEditorReference[] editors = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
									.getActivePage().getEditorReferences();
							for (IEditorReference ref : editors) {
								if (ref.getId().equals(CanvasEditor.ID)) {
									CanvasEditor editor = (CanvasEditor) ref.getEditor(false);
									if (editor != null) {
										UpdateBehavior updateBehavior = editor.getCanvas().getUpdateBehavior();
										updateBehavior.rebuild(delta);
									}
								}
							}
						}
					});
				} catch (Exception e) {
					CanvasUI.handleError(e);
				}
			}
		});

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
	 * BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
