namespace phasereditor2d.scene.core {
    
    import core = colibri.core;

    export const CONTENT_TYPE_SCENE = "Scene";

    export class SceneContentTypeResolver extends core.ContentTypeResolver {

        constructor() {
            super("phasereditor2d.scene.core.SceneContentTypeResolver");
        }

        async computeContentType(file: core.io.FilePath): Promise<string> {
            
            if (file.getExtension() === "scene") {
                return CONTENT_TYPE_SCENE;
            }

            return core.CONTENT_TYPE_ANY;
        }

    }
}