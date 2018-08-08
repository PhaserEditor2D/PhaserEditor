package phasereditor.animation.ui.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class AnimationModelElementFactory implements IElementFactory {

	public static final String ID = "phasereditor.animation.ui.model.animationFactory";

	@Override
	public IAdaptable createElement(IMemento memento) {
		var path = memento.getString("file");
		var key = memento.getString("key");
		var dataKey = memento.getString("dataKey");

		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
		if (file.exists()) {
			try {
				var anims = new AnimationsModel_Persistable(file, dataKey);
				var anim = anims.getAnimation(key);
				return anim;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

}
