// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.scene.ui;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.statushandlers.StatusManager;

import com.subshell.snippets.jface.tooltip.tooltipsupport.ICustomInformationControlCreator;
import com.subshell.snippets.jface.tooltip.tooltipsupport.Tooltips;
import com.subshell.snippets.jface.tooltip.tooltipsupport.TreeViewerInformationProvider;

import phasereditor.assetpack.ui.preview.ExternalImageFileInformationControl;
import phasereditor.scene.core.SceneCore;
import phasereditor.scene.core.SceneFile;
import phasereditor.ui.CanvasUtilsInformationControlProvider;
import phasereditor.ui.TreeCanvasViewer;

/**
 * @author arian
 *
 */
public class SceneUI {

	private static final String PLUGIN_ID = Activator.PLUGIN_ID;
	// private static final int SCENE_SCREENSHOT_SIZE = 256;

	public static void logError(Exception e) {
		e.printStackTrace();
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	public static void logError(String msg) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, msg, null));
	}

	public static void installSceneTooltips(CommonViewer viewer) {
		Tooltips.install(viewer.getControl(), new TreeViewerInformationProvider(viewer), getSceneTooltipsCreators(),
				false);
	}

	public static void installSceneTooltips(TreeCanvasViewer viewer) {
		Tooltips.install(viewer.getControl(), new CanvasUtilsInformationControlProvider(viewer.getTree().getUtils()),
				getSceneTooltipsCreators(), false);
	}

	private static List<ICustomInformationControlCreator> getSceneTooltipsCreators() {
		List<ICustomInformationControlCreator> creators = new ArrayList<>();

		creators.add(new ICustomInformationControlCreator() {

			@Override
			public IInformationControl createInformationControl(Shell parent) {
				var control = new ExternalImageFileInformationControl(parent) {

					@Override
					public File getFileToDisplay(Object model) {
						IFile file = null;
						if (model instanceof SceneFile) {
							file = ((SceneFile) model).getFile();
						} else if (model instanceof IFile) {
							var data = SceneCore.getSceneFileDataCache().getFileData((IFile) model);
							file = data.getFile();
						}

						if (file != null) {
							Path path = SceneCore.getSceneScreenshotFile(file);
							return path.toFile();
						}

						return super.getFileToDisplay(model);
					}
				};
				return control;
			}

			@Override
			public boolean isSupported(Object info) {
				if (info instanceof SceneFile) {
					return true;
				}

				if (info instanceof IFile) {
					var data = SceneCore.getSceneFileDataCache().getFileData((IFile) info);
					if (data != null) {
						return true;
					}
				}
				return false;
			}
		});
		return creators;
	}
}
