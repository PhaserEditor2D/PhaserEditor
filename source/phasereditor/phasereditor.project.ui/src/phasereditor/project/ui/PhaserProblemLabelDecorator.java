package phasereditor.project.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

import phasereditor.project.core.ProjectCore;

@SuppressWarnings("restriction")
public class PhaserProblemLabelDecorator implements ILightweightLabelDecorator {
	private ListenerList<ILabelProviderListener> _listeners;

	public PhaserProblemLabelDecorator() {
		_listeners = new ListenerList<>();
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		_listeners.add(listener);
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		_listeners.add(listener);
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	@Override
	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof IFile) {
			IFile file = (IFile) element;
			try {
				if (file.exists()) {
					int severity = file.findMaxProblemSeverity(ProjectCore.PHASER_PROBLEM_MARKER_ID, true,
							IResource.DEPTH_ONE);
					if (severity != -1) {
						ImageDescriptor img = JavaPluginImages.DESC_OVR_ERROR;
						decoration.addOverlay(img);
					}
				}
			} catch (CoreException e) {
				ProjectUI.logError(e);
			}
		}
	}
}
