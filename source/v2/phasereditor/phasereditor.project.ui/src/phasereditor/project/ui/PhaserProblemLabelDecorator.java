package phasereditor.project.ui;

import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import phasereditor.project.core.ProjectCore;

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
		//
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

					ImageDescriptor img;

					switch (severity) {
					case IMarker.SEVERITY_ERROR:
						img = ImageDescriptor.createFromURL(new URL("platform:/plugin/org.eclipse.ui/icons/full/ovr16/error_ovr.png"));
						break;
					case IMarker.SEVERITY_WARNING:
						img = ImageDescriptor.createFromURL(new URL("platform:/plugin/org.eclipse.ui/icons/full/ovr16/warning_ovr.png"));
						break;
					default:
						img = null;
						break;
					}

					if (img != null) {
						decoration.addOverlay(img);
					}
				}
			} catch (Exception e) {
				ProjectUI.logError(e);
			}
		}
	}
}
