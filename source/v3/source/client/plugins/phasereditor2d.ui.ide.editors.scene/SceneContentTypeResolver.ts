namespace phasereditor2d.ui.ide.editors.scene {
    
    import core = colibri.core;

    export const CONTENT_TYPE_SCENE = "Scene";

    export class SceneContentTypeResolver implements core.IContentTypeResolver {

        async computeContentType(file: core.io.FilePath): Promise<string> {
            
            if (file.getExtension() === "scene") {
                return CONTENT_TYPE_SCENE;
            }

            return core.CONTENT_TYPE_ANY;
        }

    }
}