package phasereditor.canvas.core;

import static java.lang.System.out;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.osgi.framework.BundleContext;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackCore.IPacksChangeListener;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.project.core.ProjectCore;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "phasereditor.canvas.core"; //$NON-NLS-1$

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
					Set<IFile> used = new HashSet<>();
					
					IProject project = null;
					for (AssetPackModel pack : delta.getPacks()) {
						project = pack.getFile().getProject();
						break;
					}

					if (project != null) {
						for (AssetModel asset : delta.getAssets()) {
							AssetPackModel pack = asset.getPack();
							project = pack.getFile().getProject();break;
						}
					}

					if (project != null) {
						IContainer webContent = ProjectCore.getWebContentFolder(project);

						webContent.accept(r -> {
							if (r instanceof IFile && !used.contains(r)) {
								IFile file = (IFile) r;
								if (CanvasCore.isCanvasFile(file)) {
									out.println("Building canvas " + file);
									try (InputStream contents = file.getContents();) {
										JSONObject data = new JSONObject(new JSONTokener(contents));
										CanvasEditorModel model = new CanvasEditorModel(file);
										model.read(data, false);
										model.getWorld().build();
									} catch (Exception e) {
										CanvasCore.handleError(e);
									}
								}
							}
							return true;
						});

					}
				} catch (Exception e) {
					CanvasCore.handleError(e);
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
