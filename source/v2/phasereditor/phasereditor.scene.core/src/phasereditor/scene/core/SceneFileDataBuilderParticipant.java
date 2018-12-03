package phasereditor.scene.core;

import phasereditor.project.core.FileDataCache;
import phasereditor.project.core.FileDataCacheBuilderParticipant;

public class SceneFileDataBuilderParticipant extends FileDataCacheBuilderParticipant<SceneFile> {

	@Override
	public FileDataCache<SceneFile> getFileDataCache() {
		return SceneCore.getSceneFileDataCache();
	}

}
