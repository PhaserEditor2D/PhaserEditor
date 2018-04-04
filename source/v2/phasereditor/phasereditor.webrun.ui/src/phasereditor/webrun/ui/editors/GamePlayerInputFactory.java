package phasereditor.webrun.ui.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import phasereditor.webrun.ui.GamePlayerEditorInput;

public class GamePlayerInputFactory implements IElementFactory {

	@Override
	public IAdaptable createElement(IMemento memento) {
		String name = memento.getString("phasereditor.webrun.ui.gameplayer.project");

		if (name == null) {
			return null;
		}

		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);

		if (project == null || !project.exists()) {
			return null;
		}

		GamePlayerEditorInput input = new GamePlayerEditorInput(project);

		String deviceName = memento.getString("phasereditor.webrun.ui.gameplayer.device.name");
		if (deviceName != null) {
			Object[] device = { deviceName, memento.getInteger("phasereditor.webrun.ui.gameplayer.device.width"),
					memento.getInteger("phasereditor.webrun.ui.gameplayer.device.height") };
			Boolean rotated = memento.getBoolean("phasereditor.webrun.ui.gameplayer.rotated");
			input.setDevice(device);
			input.setRotated(rotated.booleanValue());
		}

		return input;
	}

}
